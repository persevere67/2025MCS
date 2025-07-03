# rag_app_streaming.py (Path Fixed Version)

import os
import json
import faiss
import numpy as np
import torch
from sentence_transformers import SentenceTransformer
import pickle
import time
from openai import OpenAI

# --- 【核心修正】: 动态构建健壮的文件路径 ---
# 获取当前脚本文件所在的绝对路径 (e.g., .../RAG/data/rag_app.py)
script_path = os.path.abspath(__file__)
# 获取脚本所在的目录 (e.g., .../RAG/data)
data_dir = os.path.dirname(script_path)
# 获取项目的根目录 (e.g., .../RAG)
project_root = os.path.dirname(data_dir)

print(f"检测到项目根目录: {project_root}")
print(f"检测到数据目录: {data_dir}")

# --- 1. 配置区域 ---
CONFIG = {
    # 使用动态路径，确保总能找到正确的文件
    "sbert_model_path": os.path.join(project_root, "epoch_3"),
    "faiss_index_path": os.path.join(data_dir, "final_medical_knowledge.faiss"),
    "sentences_path": os.path.join(data_dir, "final_medical_sentences.pkl"),

    "llm_api_base": "https://api.deepseek.com/v1",
    # 【重要】请务必使用您新生成的、未泄露的密钥！建议使用环境变量。
    "llm_api_key": os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987"),
    "llm_model_name": "deepseek-chat"
}


# --- 2. 核心RAG系统类 (无需改动) ---
class RAGSystem:
    def __init__(self, config):
        print("\n正在初始化RAG系统...")
        # 加载检索模型
        print(f"加载SBERT模型: {config['sbert_model_path']}")
        self.retrieval_model = SentenceTransformer(config['sbert_model_path'])

        # 加载向量索引和句子库
        print(f"加载FAISS索引: {config['faiss_index_path']}")
        self.index = faiss.read_index(config['faiss_index_path'])
        print(f"加载句子列表: {config['sentences_path']}")
        with open(config['sentences_path'], "rb") as f:
            self.sentences = pickle.load(f)

        # 初始化大型语言模型客户端
        print(f"初始化LLM客户端，目标模型: {config['llm_model_name']}")
        self.llm_client = OpenAI(
            api_key=config['llm_api_key'],
            base_url=config['llm_api_base']
        )
        print("RAG系统准备就绪！")

    def retrieve(self, query, k=5):
        """使用SBERT和FAISS进行检索"""
        query_embedding = self.retrieval_model.encode(query, convert_to_tensor=True)
        query_embedding_np = query_embedding.cpu().numpy().reshape(1, -1)
        distances, indices = self.index.search(query_embedding_np, k)
        retrieved_facts = [self.sentences[i] for i in indices[0] if i != -1]
        return retrieved_facts

    def generate(self, query, context):
        """使用LLM以流式模式生成回答"""
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
                temperature=0.9,
                stream=True,
            )
            for chunk in stream:
                content = chunk.choices[0].delta.content
                if content is not None:
                    yield content
        except Exception as e:
            yield f"调用LLM API时发生错误: {e}"

    def answer(self, query):
        """完整的RAG流程，现在它会返回一个生成器"""
        retrieved_context = self.retrieve(query)
        if not retrieved_context:
            yield "抱歉，在我的知识库中找不到与您问题相关的信息。"
            return
        context_str = "\n".join(retrieved_context)
        yield from self.generate(query, context_str)


# --- 3. 主程序入口 ---
if __name__ == "__main__":
    if "YOUR_NEW_API_KEY_HERE" in CONFIG["llm_api_key"]:
        print("\n\n错误：请在脚本的CONFIG部分填入您新的DeepSeek API密钥！")
        exit()

    try:
        rag_system = RAGSystem(CONFIG)
    except FileNotFoundError as e:
        print(f"\n初始化失败！错误: {e}")
        print("请确保您的模型、FAISS索引和句子文件都位于正确的路径下。")
        exit()
    except Exception as e:
        print(f"\n初始化时发生未知错误: {e}")
        exit()

    print("\n\n--- 医疗智能问答系统已启动 ---")
    print("您可以开始提问了（输入 '退出' 或 'quit' 来结束程序）")

    while True:
        user_query = input("\n您的问题是: ")
        if user_query.lower() in ['退出', 'quit']:
            break

        print("\n--- 回答 ---")
        start_time = time.time()
        
        full_response = ""
        for chunk in rag_system.answer(user_query):
            print(chunk, end="", flush=True)
            full_response += chunk

        end_time = time.time()
        print(f"\n(本次回答总耗时: {end_time - start_time:.2f} 秒)")