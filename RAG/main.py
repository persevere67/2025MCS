# main.py (Final Integrated Version)

import os
import json
import faiss
import numpy as np
import torch
from sentence_transformers import SentenceTransformer
import pickle
import time
from openai import OpenAI
import asyncio
from typing import List, Dict

# --- 导入FastAPI相关的库 ---
from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
# 【核心升级】: 导入 sse-starlette 用于实现标准的SSE流
from sse_starlette.sse import EventSourceResponse

# --- 1. 配置区域 ---
# 使用动态路径，确保无论从哪里运行都能找到文件
script_dir = os.path.dirname(os.path.abspath(__file__))
print(f"检测到项目/脚本目录: {script_dir}")

# 创建必要的目录
data_dir = os.path.join(script_dir, "data")
model_dir = os.path.join(script_dir, "epoch_3")
os.makedirs(data_dir, exist_ok=True)
os.makedirs(model_dir, exist_ok=True)


try:
    # 【重要】请务必使用您新生成的、未泄露的密钥！
    deepseek_api_key = os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987")
    
    CONFIG = {
        "sbert_model_path": model_dir,
        "faiss_index_path": os.path.join(data_dir, "final_medical_knowledge.faiss"),
        "sentences_path": os.path.join(data_dir, "final_medical_sentences.pkl"),
        "llm_api_base": "https://api.deepseek.com/v1",
        "llm_api_key": deepseek_api_key,
        "llm_model_name": "deepseek-chat",
        "SECRET_TOKEN": "my-super-secret-token-for-med-qa" # 简单的认证密钥
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
        self.status = "INITIALIZING"
        try:
            print(f"加载Sentence Transformer模型: {config['sbert_model_path']}")
            self.retrieval_model = SentenceTransformer(config['sbert_model_path'])
            print(f"加载FAISS索引: {config['faiss_index_path']}")
            self.index = faiss.read_index(config['faiss_index_path'])
            print(f"加载句子数据: {config['sentences_path']}")
            with open(config['sentences_path'], "rb") as f:
                self.sentences = pickle.load(f)
            print(f"初始化OpenAI客户端，使用模型: {config['llm_model_name']}")
            self.llm_client = OpenAI(api_key=config['llm_api_key'], base_url=config['llm_api_base'])
            self.status = "READY"
            print("RAG系统准备就绪！")
        except Exception as e:
            self.status = f"ERROR: {str(e)}"
            print(f"RAG系统初始化失败: {e}")

    def retrieve(self, query, k=5):
        if self.status != "READY": return ["系统未正确初始化，无法检索"]
        query_embedding = self.retrieval_model.encode(query, convert_to_tensor=True)
        query_embedding_np = query_embedding.cpu().numpy().reshape(1, -1)
        _, indices = self.index.search(query_embedding_np, k)
        return [self.sentences[i] for i in indices[0] if i != -1]

    # 【Prompt工程升级】: generate_stream 方法，升级Prompt并处理历史记录
    async def generate_stream(self, query: str, context: str, history: List[Dict[str, str]]):
        if self.status != "READY":
            yield json.dumps({"token": "LLM客户端未初始化"})
            return

        formatted_history = "\n".join([f"用户问：{h['question']}\n你答：{h['answer']}" for h in history])
            
        prompt = f"""你是一位顶级的医疗问答专家，富有同理心且语言严谨。

### 任务与规则
1.  **核心任务**: 根据“当前问题”和“背景知识”，生成一个专业且易于理解的回答。
2.  **上下文感知**: 参考“对话历史”来理解用户的追问。例如，如果用户问“它有什么副作用？”，你需要从历史中找出“它”指的是什么药品或疾病。
3.  **忠于事实**: 所有回答都必须严格基于“背景知识”，绝不允许编造。
4.  **引用来源**: 在回答中，对于引用自背景知识的信息，请在句末用`[来源x]`的格式标注出处，`x`对应背景知识的编号。
5.  **格式化输出**: 请使用Markdown语法来组织回答，可以使用标题、列表和加粗来提升可读性。
6.  **未知处理**: 如果背景知识不足以回答，请明确告知“根据现有资料无法回答此问题”，不要猜测。
7.  **标准结尾**: 最后，请以“希望以上信息对您有帮助，如有不适请及时就医。”作为结尾。

### 对话历史
{formatted_history}

### 背景知识
{context}

### 当前问题
{query}

### 你的回答 (请严格按照以上规则生成)：
"""
        try:
            stream = self.llm_client.chat.completions.create(
                model=CONFIG['llm_model_name'],
                messages=[{"role": "user", "content": prompt}],
                stream=True,
                temperature=0.5
            )
            for chunk in stream:
                content = chunk.choices[0].delta.content
                if content:
                    # 【SSE格式】: 将每个文字块包装成SSE事件格式
                    yield json.dumps({"token": content})
        except Exception as e:
            print(f"LLM生成错误: {e}")
            yield json.dumps({"token": f"\n\n[调用AI生成回答时出错: {e}]"})

    # 【Prompt工程升级】: answer_stream 方法，处理历史和带编号的上下文
    async def answer_stream(self, query: str, history: List[Dict[str, str]]):
        print(f"处理查询: '{query}' (包含 {len(history)} 条历史)")
        
        retrieved_context_list = self.retrieve(query)
        context_str = "\n".join([f"【来源{i+1}】: {fact}" for i, fact in enumerate(retrieved_context_list)])
        
        yield f"event: retrieval_complete\ndata: {json.dumps({'count': len(retrieved_context_list)})}\n\n"
        
        async for chunk in self.generate_stream(query, context_str, history):
            yield f"data: {chunk}\n\n"
            
        yield "event: end\ndata: {}\n\n"

# --- 3. FastAPI应用设置 ---
app = FastAPI(title="医疗问答RAG系统", version="1.2.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

# --- 4. 认证 (占位符实现) ---
async def verify_token(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="无效的认证格式")
    token = authorization.split(" ")[1]
    if token != CONFIG["SECRET_TOKEN"]:
        raise HTTPException(status_code=401, detail="Token无效或已过期")
    print(f"认证成功, Token: ...{token[-4:]}")
    return {"user_id": "testuser"}

# --- 5. 数据模型 (升级以支持历史记录) ---
class Query(BaseModel):
    question: str
    history: List[Dict[str, str]] = []

# --- 6. 全局变量和启动事件 ---
rag_system: RAGSystem = None
@app.on_event("startup")
def startup_event():
    global rag_system
    if CONFIG:
        rag_system = RAGSystem(CONFIG)

# --- 7. API端点定义 (升级以支持历史记录和SSE) ---
@app.post("/api/question/ask", dependencies=[Depends(verify_token)])
async def ask_question(query: Query):
    if not rag_system or rag_system.status != "READY":
        raise HTTPException(status_code=503, detail="RAG系统未准备好")
    return EventSourceResponse(rag_system.answer_stream(query.question, query.history))

# --- 其他占位符API接口 (保持不变) ---
@app.get("/api/health/check")
def health_check():
    return {"success": True, "data": {"pythonService": rag_system.status if rag_system else "NOT_INITIALIZED"}}

# ... 您可以根据需要添加/保留其他如 /history, /stats 的占位符接口 ...

# --- 应用入口 ---
if __name__ == "__main__":
    import uvicorn
    if "YOUR_NEW_API_KEY_HERE" in CONFIG["llm_api_key"]:
        print("\n\n错误：请在脚本的CONFIG部分或环境变量中设置您新的DeepSeek API密钥！")
    else:
        print("\n启动服务，请访问 http://127.0.0.1:8000")
        uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
