# main.py

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

# --- 导入FastAPI相关的库 ---
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from starlette.responses import StreamingResponse

# --- 1. 配置区域 (与之前相同) ---
# 使用动态路径，确保无论从哪里运行都能找到文件
script_dir = os.path.dirname(os.path.abspath(__file__))
print(f"检测到项目/脚本目录: {script_dir}")

CONFIG = {
    "sbert_model_path": os.path.join(script_dir, "epoch_3"),
    "faiss_index_path": os.path.join(script_dir, "data", "final_medical_knowledge.faiss"),
    "sentences_path": os.path.join(script_dir, "data", "final_medical_sentences.pkl"),
    "llm_api_base": "https://api.deepseek.com/v1",
    "llm_api_key": os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987"),
    "llm_model_name": "deepseek-chat"
}

# --- 2. 核心RAG系统类 (与之前相同) ---
class RAGSystem:
    def __init__(self, config):
        print("正在初始化RAG系统...")
        self.retrieval_model = SentenceTransformer(config['sbert_model_path'])
        self.index = faiss.read_index(config['faiss_index_path'])
        with open(config['sentences_path'], "rb") as f:
            self.sentences = pickle.load(f)
        self.llm_client = OpenAI(
            api_key=config['llm_api_key'],
            base_url=config['llm_api_base']
        )
        print("RAG系统准备就绪！")

    def retrieve(self, query, k=5):
        query_embedding = self.retrieval_model.encode(query, convert_to_tensor=True)
        query_embedding_np = query_embedding.cpu().numpy().reshape(1, -1)
        _, indices = self.index.search(query_embedding_np, k)
        retrieved_facts = [self.sentences[i] for i in indices[0] if i != -1]
        return retrieved_facts

    async def generate_stream(self, query, context):
        """使用LLM以流式模式生成回答的异步版本"""
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
                content = chunk.choices[0].delta.content
                if content:
                    yield content
        except Exception as e:
            yield f"调用LLM API时发生错误: {e}"

    async def answer_stream(self, query):
        """完整的RAG流程，返回一个异步生成器"""
        retrieved_context = self.retrieve(query)
        if not retrieved_context:
            yield "抱歉，在我的知识库中找不到与您问题相关的信息。"
            return
        context_str = "\n".join(retrieved_context)
        async for chunk in self.generate_stream(query, context_str):
            yield chunk

# --- 3. FastAPI应用设置 ---

# 创建FastAPI应用实例
app = FastAPI()

# 【重要】设置CORS中间件，允许所有来源的跨域请求（在开发阶段）
# 这能防止浏览器因为安全策略而阻止前端页面访问后端API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 定义一个数据模型，用于接收前端发来的问题
class Query(BaseModel):
    question: str

# 在应用启动时，加载RAG系统模型，这样只需加载一次
@app.on_event("startup")
async def startup_event():
    global rag_system
    rag_system = RAGSystem(CONFIG)

# 创建API接口
@app.post("/ask")
async def ask_question(query: Query):
    """
    接收前端问题，并以流式响应返回答案的API接口
    """
    return StreamingResponse(rag_system.answer_stream(query.question), media_type="text/plain")