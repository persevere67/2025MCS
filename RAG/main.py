import os
import json
import faiss
import numpy as np
from sentence_transformers import SentenceTransformer
import pickle
import time
from openai import AsyncOpenAI  
import asyncio

# --- FastAPI相关 ---
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel

# --- 配置区域 ---
script_dir = os.path.dirname(os.path.abspath(__file__))
data_dir = os.path.join(script_dir, "data")
os.makedirs(data_dir, exist_ok=True)

deepseek_api_key = os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987")

CONFIG = {
    "sbert_model_path": os.path.join(script_dir, "epoch_3"),
    "faiss_index_path": os.path.join(data_dir, "final_medical_knowledge.faiss"),
    "sentences_path": os.path.join(data_dir, "final_medical_sentences.pkl"),
    "llm_api_base": "https://api.deepseek.com/v1",
    "llm_api_key": deepseek_api_key,
    "llm_model_name": "deepseek-chat"
}

# --- 核心RAG系统 ---
class RAGSystem:
    def __init__(self, config):
        self.config = config
        self.retrieval_model = SentenceTransformer(config['sbert_model_path'])
        self.index = faiss.read_index(config['faiss_index_path'])
        
        with open(config['sentences_path'], "rb") as f:
            self.sentences = pickle.load(f)
            
        # 使用异步客户端
        self.llm_client = AsyncOpenAI(
            api_key=config['llm_api_key'],
            base_url=config['llm_api_base']
        )
        self.status = "READY"  # 系统状态

    def retrieve(self, query, k=5):
        query_embedding = self.retrieval_model.encode(query)
        _, indices = self.index.search(np.array([query_embedding]), k)
        return [self.sentences[i] for i in indices[0]]

    async def generate_stream(self, query, context):
        """流式响应核心方法 - 已修复"""
        prompt = f"""Act like 一位专业、富有同理心的医疗问答助手。你擅长将复杂的医学信息转化为患者易于理解的语言，并能够在保证科学严谨的前提下给予温暖和清晰的健康建议。
                    你的目标是：回答用户提出的医学相关问题，所有回答必须基于提供的背景知识为核心依据，必要时可补充严谨、可靠的医学资料。
                    请按以下步骤操作：步骤 1：阅读 “背景知识” 段落，提取关键信息，明确它所提供的医学内容或结论。步骤 2：阅读 “用户问题”，识别问题核心，例如：疾病解释、药物作用、副作用、处理建议等。步骤 3：仅根据背景知识内容，使用通俗、连贯的中文，结构清晰地回答用户的问题。可使用小标题、段落或要点列出答案，确保患者或非专业读者能够理解。
                    步骤 4：如果背景知识内容不足以完整回答问题，请根据权威医学网站（如 PubMed、WHO、UpToDate、国家卫健委等）检索相关资料进行补充说明。必须保持严谨，不得猜测或编造。清楚标注哪些内容来自扩展查询。步骤 5：结尾以温和、关怀的语言总结核心建议，若问题涉及个体诊疗，请建议咨询专业医生进一步确认。请注意：不要输出背景知识本身，仅用它作为回答依据。整个回答应当逻辑自洽、信息详实、语气亲切、医学上严谨。Take a deep breath and work on this problem step-by-step.
{context}

[问题]
{query}

[回答]: """
        
        # 正确使用异步客户端创建流式响应
        stream = await self.llm_client.chat.completions.create(
            model=self.config['llm_model_name'],
            messages=[{"role": "user", "content": prompt}],
            max_tokens=1024,
            temperature=0.7,
            stream=True
        )
        
        # 正确迭代异步流
        async for chunk in stream:
            if chunk.choices and chunk.choices[0].delta.content:
                yield chunk.choices[0].delta.content.encode('utf-8')

    async def answer_stream(self, query):
        """完整RAG流式流程"""
        if self.status != "READY":
            yield f"系统未准备好: {self.status}".encode('utf-8')
            return
            
        context = self.retrieve(query)
        if not context:
            yield "未找到相关背景信息".encode('utf-8')
            return
            
        context_str = "\n".join(context)
        
        # 返回流式生成器
        async for chunk in self.generate_stream(query, context_str):
            yield chunk

# --- FastAPI应用 ---
app = FastAPI(title="医疗问答RAG系统")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

rag_system = None

@app.on_event("startup")
async def startup_event():
    global rag_system
    try:
        rag_system = RAGSystem(CONFIG)
        print("RAG系统启动完成！")
    except Exception as e:
        rag_system = None
        print(f"系统启动失败: {e}")

class Query(BaseModel):
    question: str

@app.get("/")
async def root():
    return {
        "service": "Medical RAG API",
        "status": "running",
        "rag_status": rag_system.status if rag_system else "NOT INITIALIZED"
    }

@app.get("/health")
async def health_check():
    status = "healthy" if rag_system and rag_system.status == "READY" else "unhealthy"
    return {
        "status": status,
        "model": CONFIG['llm_model_name'],
        "index_loaded": rag_system and rag_system.index is not None
    }

# 问答接口 - 流式响应
@app.post("/ask")
async def ask_question(query: Query):
    return StreamingResponse(
        rag_system.answer_stream(query.question),
        media_type="text/event-stream"
    )

# 测试检索端点 - 保留
@app.get("/test-retrieve")
async def test_retrieve(query: str, k: int = 3):
    """测试检索功能"""
    if not rag_system or rag_system.status != "READY":
        return JSONResponse(
            status_code=503,
            content={"error": "检索系统未初始化"}
        )
        
    context = rag_system.retrieve(query, k)
    return {
        "query": query,
        "k": k,
        "context": context
    }

# 测试LLM端点 - 保留
@app.get("/test-llm")
async def test_llm(prompt: str = "你好，请介绍一下自己"):
    """测试LLM连接性"""
    if not rag_system or rag_system.status != "READY":
        return JSONResponse(
            status_code=503,
            content={"error": "LLM客户端未初始化"}
        )
        
    try:
        # 使用异步客户端
        response = await rag_system.llm_client.chat.completions.create(
            model=CONFIG['llm_model_name'],
            messages=[{"role": "user", "content": prompt}],
            max_tokens=200
        )
        
        return {
            "prompt": prompt,
            "response": response.choices[0].message.content
        }
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"error": str(e)}
        )

# 错误处理
@app.exception_handler(Exception)
async def exception_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={"error": str(exc)}
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)