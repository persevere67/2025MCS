import os
import json
import faiss
import numpy as np
from sentence_transformers import SentenceTransformer
import pickle
import time
from openai import OpenAI
import asyncio

# --- 导入FastAPI相关的库 ---
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.responses import StreamingResponse

# --- 1. 配置区域 ---
# 使用动态路径，确保无论从哪里运行都能找到文件
script_dir = os.path.dirname(os.path.abspath(__file__))
print(f"检测到项目/脚本目录: {script_dir}")

# 创建必要的目录
data_dir = os.path.join(script_dir, "data")
os.makedirs(data_dir, exist_ok=True)

# 更健壮的配置处理
try:
    # 尝试从环境变量获取API密钥
    deepseek_api_key = os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987")
    
    CONFIG = {
        "sbert_model_path": os.path.join(script_dir, "epoch_3"),
        "faiss_index_path": os.path.join(data_dir, "final_medical_knowledge.faiss"),
        "sentences_path": os.path.join(data_dir, "final_medical_sentences.pkl"),
        "llm_api_base": "https://api.deepseek.com/v1",
        "llm_api_key": deepseek_api_key,
        "llm_model_name": "deepseek-chat"
    }
    
    print("配置信息:")
    print(json.dumps(CONFIG, indent=2, ensure_ascii=False))
    
except Exception as e:
    print(f"配置初始化失败: {e}")
    CONFIG = None

# --- 2. 核心RAG系统类 ---
class RAGSystem:
    def __init__(self, config):
        print("正在初始化RAG系统...")
        
        try:
            # 加载模型
            print(f"加载Sentence Transformer模型: {config['sbert_model_path']}")
            self.retrieval_model = SentenceTransformer(config['sbert_model_path'])
            
            # 加载FAISS索引
            print(f"加载FAISS索引: {config['faiss_index_path']}")
            self.index = faiss.read_index(config['faiss_index_path'])
            
            # 加载句子数据
            print(f"加载句子数据: {config['sentences_path']}")
            with open(config['sentences_path'], "rb") as f:
                self.sentences = pickle.load(f)
                
            # 初始化OpenAI客户端
            print(f"初始化OpenAI客户端，使用模型: {config['llm_model_name']}")
            self.llm_client = OpenAI(
                api_key=config['llm_api_key'],
                base_url=config['llm_api_base']
            )
            
            print("RAG系统准备就绪！")
            self.status = "READY"
            
        except Exception as e:
            print(f"RAG系统初始化失败: {e}")
            self.status = f"ERROR: {str(e)}"
            # 部分功能降级处理
            self.retrieval_model = None
            self.index = None
            self.sentences = []
            self.llm_client = None

    def retrieve(self, query, k=5):
        """检索相关文档"""
        if not self.retrieval_model or not self.index:
            return ["系统未正确初始化，无法检索"]
        
        try:
            query_embedding = self.retrieval_model.encode(query, convert_to_tensor=True)
            query_embedding_np = query_embedding.cpu().numpy().reshape(1, -1)
            _, indices = self.index.search(query_embedding_np, k)
            retrieved_facts = [self.sentences[i] for i in indices[0] if i != -1]
            return retrieved_facts
        except Exception as e:
            print(f"检索失败: {e}")
            return [f"检索失败: {str(e)}"]

    async def generate_stream(self, query, context):
        """使用LLM以流式模式生成回答"""
        if not self.llm_client:
            yield "LLM客户端未初始化，无法生成回答"
            return
            
        prompt = f"""Act like 一位专业、富有同理心的医疗问答助手。你擅长将复杂的医学信息转化为患者易于理解的语言，并能够在保证科学严谨的前提下给予温暖和清晰的健康建议。
                    你的目标是：回答用户提出的医学相关问题，所有回答必须基于提供的背景知识为核心依据，必要时可补充严谨、可靠的医学资料。
                    请按以下步骤操作：步骤 1：阅读 “背景知识” 段落，提取关键信息，明确它所提供的医学内容或结论。步骤 2：阅读 “用户问题”，识别问题核心，例如：疾病解释、药物作用、副作用、处理建议等。步骤 3：仅根据背景知识内容，使用通俗、连贯的中文，结构清晰地回答用户的问题。可使用小标题、段落或要点列出答案，确保患者或非专业读者能够理解。
步骤 4：如果背景知识内容不足以完整回答问题，请根据权威医学网站（如 PubMed、WHO、UpToDate、国家卫健委等）检索相关资料进行补充说明。必须保持严谨，不得猜测或编造。清楚标注哪些内容来自扩展查询。步骤 5：结尾以温和、关怀的语言总结核心建议，若问题涉及个体诊疗，请建议咨询专业医生进一步确认。请注意：不要输出背景知识本身，仅用它作为回答依据。整个回答应当逻辑自洽、信息详实、语气亲切、医学上严谨。Take a deep breath and work on this problem step-by-step.

[背景知识]
{context}

[问题]
{query}

[你的回答]：
"""
        try:
            stream = self.llm_client.chat.completions.create(
                model=CONFIG['llm_model_name'],
                messages=[{"role": "user", "content": prompt}],
                max_tokens=1024,
                temperature=0.7,
                stream=True,
            )
            for chunk in stream:
                if chunk.choices and chunk.choices[0].delta.content:
                    content = chunk.choices[0].delta.content
                    yield content
        except Exception as e:
            print(f"LLM生成错误: {e}")
            yield f"生成回答时出错: {str(e)}"

    async def answer_stream(self, query):
        """完整的RAG流程，返回异步生成器"""
        if self.status != "READY":
            yield f"系统未准备好: {self.status}"
            return
            
        print(f"处理查询: {query}")
        
        # 检索相关上下文
        retrieved_context = self.retrieve(query)
        if not retrieved_context:
            yield "未找到相关背景信息"
            return
            
        print(f"检索到 {len(retrieved_context)} 条相关上下文")
        context_str = "\n".join(retrieved_context)
        
        # 生成流式响应
        async for chunk in self.generate_stream(query, context_str):
            yield chunk

# --- 3. FastAPI应用设置 ---

# 创建FastAPI应用实例
app = FastAPI(
    title="医疗问答RAG系统",
    description="基于知识检索的医疗问答API",
    version="1.0.0"
)

# CORS配置 - 允许所有来源
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 定义数据模型
class Query(BaseModel):
    question: str

# 全局变量
rag_system = None

@app.on_event("startup")
async def startup_event():
    """应用启动时初始化RAG系统"""
    global rag_system
    
    print("\n" + "="*50)
    print("正在启动医疗问答RAG服务")
    print("="*50 + "\n")
    
    if not CONFIG:
        print("致命错误: 配置未初始化")
        return
        
    try:
        rag_system = RAGSystem(CONFIG)
        print("RAG系统启动完成！")
    except Exception as e:
        print(f"启动失败: {e}")

# --- API端点定义 ---

@app.get("/")
async def root():
    """根端点，用于服务基本检查"""
    return {
        "service": "Medical RAG API",
        "status": "running",
        "timestamp": time.time(),
        "version": "1.0.0",
        "rag_status": rag_system.status if rag_system else "NOT INITIALIZED"
    }

@app.get("/health")
async def health_check():
    """健康检查端点"""
    status = "healthy" if rag_system and rag_system.status == "READY" else "unhealthy"
    
    return {
        "status": status,
        "service": "Medical RAG Service",
        "timestamp": time.time(),
        "model": CONFIG['llm_model_name'] if CONFIG else "UNKNOWN",
        "index_status": "loaded" if rag_system and rag_system.index else "unavailable",
        "llm_status": "available" if rag_system and rag_system.llm_client else "unavailable"
    }

@app.post("/ask")
async def ask_question(query: Query):
    """
    问答接口 - 流式响应
    
    接收JSON格式的问题:
    {
        "question": "您的医疗问题"
    }
    """
    if not rag_system or rag_system.status != "READY":
        raise HTTPException(
            status_code=503,
            detail="RAG系统未准备好，请稍后再试"
        )
        
    return StreamingResponse(
        rag_system.answer_stream(query.question),
        media_type="text/plain"
    )

@app.get("/test-retrieve")
async def test_retrieve(query: str, k: int = 3):
    """测试检索功能"""
    if not rag_system or not rag_system.retrieval_model:
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

@app.get("/test-llm")
async def test_llm(prompt: str = "你好，请介绍一下自己"):
    """测试LLM连接性"""
    if not rag_system or not rag_system.llm_client:
        return JSONResponse(
            status_code=503,
            content={"error": "LLM客户端未初始化"}
        )
        
    try:
        # 直接调用LLM
        response = rag_system.llm_client.chat.completions.create(
            model=CONFIG['llm_model_name'],
            messages=[{"role": "user", "content": prompt}],
            max_tokens=200,
            temperature=0.7
        )
        
        return {
            "status": "success",
            "prompt": prompt,
            "response": response.choices[0].message.content
        }
        
    except Exception as e:
        return {
            "status": "error",
            "error": str(e)
        }

# --- 错误处理 ---
@app.exception_handler(Exception)
async def generic_exception_handler(request, exc):
    """全局异常处理"""
    return JSONResponse(
        status_code=500,
        content={"error": f"服务器错误: {str(exc)}"}
    )

# --- 应用入口 ---
if __name__ == "__main__":
    import uvicorn
    print("\n启动服务...")
    uvicorn.run(app, host="0.0.0.0", port=8000)