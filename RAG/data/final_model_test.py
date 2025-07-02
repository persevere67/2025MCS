# final_model_test.py (Corrected paths based on your screenshot)

import json
import os
import faiss
import numpy as np
import torch
from sentence_transformers import SentenceTransformer
import pickle
import time

# --- 【最终路径修正】: 根据您的项目结构图动态构建路径 ---
# 获取当前脚本文件所在的 'data' 目录的绝对路径
# e.g., F:\RAG\data
script_dir = os.path.dirname(os.path.abspath(__file__))

# 获取 'data' 目录的上级目录，也就是项目根目录 'RAG'
# e.g., F:\RAG
project_root = os.path.dirname(script_dir)

print(f"检测到项目根目录: {project_root}")
print(f"检测到数据/脚本目录: {script_dir}")


# --- 1. 配置区域 ---
CONFIG = {
    # 模型路径 = 项目根目录 + 'epoch_3'
    "model_path": os.path.join(project_root, "epoch_3"),

    # 数据和索引文件路径 = 脚本所在目录 (data) + 文件名
    "qa_pairs_path": os.path.join(script_dir, "training_qa_pairs_final.json"),
    "faiss_index_path": os.path.join(script_dir, "final_medical_knowledge.faiss"),
    "sentences_path": os.path.join(script_dir, "final_medical_sentences.pkl")
}


def build_index_from_qa_pairs(model, qa_path, index_path, sentences_path):
    """
    从生成的问答对文件中，提取“答案”作为知识库，构建并保存FAISS索引。
    """
    print("开始加载并处理问答对数据...")
    try:
        with open(qa_path, 'r', encoding='utf-8') as f:
            qa_pairs = json.load(f)
    except FileNotFoundError:
        print(f"错误: 找不到问答对文件 {qa_path}")
        return False

    # 我们只对“答案”部分进行索引，因为它们是知识的载体
    knowledge_base_sentences = [pair[1] for pair in qa_pairs if len(pair) == 2]

    if not knowledge_base_sentences:
        print("错误: 未能从文件中提取任何有效的答案句子。")
        return False

    print(f"已提取 {len(knowledge_base_sentences)} 条知识用于构建索引。")
    print("开始使用最终模型对知识库进行编码（这可能需要几分钟）...")

    # 使用您的最终模型进行编码
    embeddings = model.encode(knowledge_base_sentences, convert_to_tensor=True, show_progress_bar=True)
    embeddings_np = embeddings.cpu().numpy()

    # 构建并保存FAISS索引
    d = embeddings_np.shape[1]
    index = faiss.IndexFlatL2(d)
    index.add(embeddings_np)

    faiss.write_index(index, index_path)
    print(f"FAISS索引已保存到: {index_path}")

    # 保存句子列表，以便后续根据索引查找原文
    with open(sentences_path, "wb") as f:
        pickle.dump(knowledge_base_sentences, f)
    print(f"知识库句子列表已保存到: {sentences_path}")
    return True


class SemanticSearcher:
    def __init__(self, model, index_path, sentences_path):
        print("初始化语义搜索器...")
        self.model = model
        self.index = faiss.read_index(index_path)
        with open(sentences_path, "rb") as f:
            self.sentences = pickle.load(f)
        print("语义搜索器准备就绪。")

    def search(self, query, k=5):
        print(f"\n==================================================")
        print(f"正在搜索: '{query}'")
        print(f"==================================================")
        start_time = time.time()

        query_embedding = self.model.encode(query, convert_to_tensor=True).cpu().numpy().reshape(1, -1)
        distances, indices = self.index.search(query_embedding, k)

        end_time = time.time()
        print(f"搜索完成，耗时: {end_time - start_time:.4f} 秒\n")

        print(f"找到 Top {k} 个最相关的结果：")
        for i, idx in enumerate(indices[0]):
            if idx != -1:
                score = 1 / (1 + np.sqrt(distances[0][i]))
                print(f"  {i + 1}. [相关度: {score:.4f}] {self.sentences[idx]}")
        print("-" * 50)


if __name__ == "__main__":
    # 加载您微调好的SBERT模型
    try:
        sbert_model = SentenceTransformer(CONFIG["model_path"])
    except Exception as e:
        print(f"错误: 加载模型失败，请确保路径 '{CONFIG['model_path']}' 正确无误。错误信息: {e}")
        exit()

    # 如果索引不存在，则创建它
    if not os.path.exists(CONFIG["faiss_index_path"]):
        print("未找到本地知识库索引，开始执行一次性构建流程...")
        success = build_index_from_qa_pairs(sbert_model, CONFIG["qa_pairs_path"], CONFIG["faiss_index_path"],
                                            CONFIG["sentences_path"])
        if not success:
            exit()

    # 创建搜索器实例并进行查询
    searcher = SemanticSearcher(sbert_model, CONFIG["faiss_index_path"], CONFIG["sentences_path"])

    # --- 在这里输入任何您想问的问题！ ---
    queries_to_test = [
        "枸橼酸西地那非有什么用？",
        "高血压的症状有哪些？"
    ]

    for q in queries_to_test:
        searcher.search(q)