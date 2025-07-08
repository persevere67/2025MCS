# E:\2025MCS\RAG\main.py

import os
import json
import faiss
import numpy as np
import torch
import torch
from sentence_transformers import SentenceTransformer
import pickle
import time
from openai import OpenAI
from openai import OpenAI
import asyncio
from typing import List, Dict

# --- 导入FastAPI相关的库 ---
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
# 【核心升级】: Import sse-starlette for standard SSE streaming
from sse_starlette.sse import EventSourceResponse

# --- 1. Configuration Area ---
# Use dynamic paths to ensure files are found regardless of where the script is run
script_dir = os.path.dirname(os.path.abspath(__file__))
print(f"Detected project/script directory: {script_dir}")

# Create necessary directories
data_dir = os.path.join(script_dir, "data")
model_dir = os.path.join(script_dir, "epoch_3")
os.makedirs(data_dir, exist_ok=True)
os.makedirs(model_dir, exist_ok=True)

try:
    # 【Important】: Please use your newly generated, unexposed key!
    deepseek_api_key = os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987")
    CONFIG = {
        "sbert_model_path": model_dir,
        "faiss_index_path": os.path.join(data_dir, "final_medical_knowledge.faiss"),
        "sentences_path": os.path.join(data_dir, "final_medical_sentences.pkl"),
        "llm_api_base": "https://api.deepseek.com/v1",
        "llm_api_key": deepseek_api_key,
        "llm_model_name": "deepseek-chat",
        "SECRET_TOKEN": "my-super-secret-token-for-med-qa"  # Simple authentication key
    }
    print("Configuration information:")
    print(json.dumps(CONFIG, indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Configuration initialization failed: {e}")
    CONFIG = None

# --- 2. Core RAG System Class ---
class RAGSystem:
    def __init__(self, config):
        print("Initializing RAG system...")
        self.status = "INITIALIZING"
        try:
            print(f"Loading Sentence Transformer model: {config['sbert_model_path']}")
            self.retrieval_model = SentenceTransformer(config['sbert_model_path'])
            print(f"Loading FAISS index: {config['faiss_index_path']}")
            self.index = faiss.read_index(config['faiss_index_path'])
            print(f"Loading sentence data: {config['sentences_path']}")
            with open(config['sentences_path'], "rb") as f:
                self.sentences = pickle.load(f)
            print(f"Initializing OpenAI client with model: {config['llm_model_name']}")
            self.llm_client = OpenAI(api_key=config['llm_api_key'], base_url=config['llm_api_base'])
            self.status = "READY"
            print("RAG system ready!")
        except Exception as e:
            self.status = f"ERROR: {str(e)}"
            print(f"RAG system initialization failed: {e}")

    def retrieve(self, query, k=5):
        if self.status != "READY":
            return ["System not properly initialized, unable to retrieve."]
        query_embedding = self.retrieval_model.encode(query, convert_to_tensor=True)
        query_embedding_np = query_embedding.cpu().numpy().reshape(1, -1)
        _, indices = self.index.search(query_embedding_np, k)
        return [self.sentences[i] for i in indices[0] if i != -1]

    # 【Prompt Engineering Upgrade】: generate_stream method, upgraded Prompt and history handling
    async def generate_stream(self, query: str, context: str, history: List[Dict[str, str]]):
        if self.status != "READY":
            yield json.dumps({"token": "LLM client not initialized"})
            return

        formatted_history = "\n".join([f"用户问：{h['question']}\n你答：{h['answer']}" for h in history])
        prompt = f"""你是一位顶级的医疗问答专家，富有同理心且语言严谨。

### 任务与规则

1.  **核心任务**: 根据“当前问题”和“背景知识”，生成一个专业且易于理解的回答。
2.  **上下文感知**: 参考“对话历史”来理解用户的追问。例如，如果用户问“它有什么副作用？”，你需要从历史中找出“它”指的是什么药品或疾病。
3.  **忠于事实**: 所有回答都必须严格基于“背景知识”，绝不允许编造。
4.  **引用来源**: 在回答中，对于引用自背景知识的信息，请在句末用[来源x]的格式标注出处，x对应背景知识的编号。
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
                temperature=0.8
            )
            for chunk in stream:
                content = chunk.choices[0].delta.content
                if content:
                    # 【SSE Format】: Wrap each text chunk into SSE event format
                    yield json.dumps({"token": content})
        except Exception as e:
            print(f"LLM generation error: {e}")
            yield json.dumps({"token": f"\n\n[Error calling AI to generate answer: {e}]"})

    # 【Prompt Engineering Upgrade】: answer_stream method, handles history and numbered context
    async def answer_stream(self, query: str, history: List[Dict[str, str]]):
        print(f"Processing query: '{query}' (contains {len(history)} history entries)")
        retrieved_context_list = self.retrieve(query)
        context_str = "\n".join([f"【来源{i+1}】: {fact}" for i, fact in enumerate(retrieved_context_list)])
        yield f"event: retrieval_complete\ndata: {json.dumps({'count': len(retrieved_context_list)})}\n\n"
        async for chunk in self.generate_stream(query, context_str, history):
            yield f"data: {chunk}\n\n"
        yield "event: end\ndata: {}\n\n"

# --- 3. FastAPI Application Setup ---
app = FastAPI(title="Medical Q&A RAG System", version="1.2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 4. Authentication (Placeholder Implementation) ---
async def verify_token(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authentication format.")
    token = authorization.split(" ")[1]
    if token != CONFIG["SECRET_TOKEN"]:
        raise HTTPException(status_code=401, detail="Invalid or expired Token.")
    print(f"Authentication successful, Token: ...{token[-4:]}")
    return {"user_id": "testuser"}

# --- 5. Data Models (Upgraded to support history) ---
class Query(BaseModel):
    question: str
    history: List[Dict[str, str]] = []

# --- 6. Global Variables and Startup Event ---
rag_system: RAGSystem = None
@app.on_event("startup")
def startup_event():
    global rag_system
    if CONFIG:
        rag_system = RAGSystem(CONFIG)

# --- 7. API Endpoint Definition (Upgraded to support history and SSE) ---
@app.post("/api/question/ask", dependencies=[Depends(verify_token)])
async def ask_question(query: Query):
    if not rag_system or rag_system.status != "READY":
        raise HTTPException(status_code=503, detail="RAG system not ready.")
    return EventSourceResponse(rag_system.answer_stream(query.question, query.history))

# --- Other Placeholder API Endpoints (Kept unchanged) ---
@app.get("/api/health/check")
def health_check():
    return {"success": True, "data": {"pythonService": rag_system.status if rag_system else "NOT_INITIALIZED"}}

# --- Application Entry Point ---
if __name__ == "__main__":
    import uvicorn
    if "YOUR_NEW_API_KEY_HERE" in CONFIG["llm_api_key"]:
        print("\n\nError: Please set your new DeepSeek API key in the script's CONFIG section or environment variables!")
    else:
        print("\nStarting service, please visit http://127.0.0.1:8000")
        uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)