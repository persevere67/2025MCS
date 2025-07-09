import json
import os
import faiss
import numpy as np
import torch
from sentence_transformers import SentenceTransformer
import pickle
from tqdm import tqdm
from collections import defaultdict
from openai import OpenAI
import asyncio

# --- 1. 配置区域 ---
CONFIG = {
    "fine_tuned_model_path": "epoch_3",
    "base_model_path": "sbert-base-chinese-nli-local",
    "sentences_path": os.path.join("data", "final_medical_sentences.pkl"),
    "top_k": 5,
    # 配置用于“裁判”的LLM
    "judge_llm_api_key": os.environ.get("DEEPSEEK_API_KEY", "sk-e40b773f247748f1b4d5d831cb3a8987"),
    "judge_llm_api_base": "https://api.deepseek.com/v1",
    "judge_llm_model_name": "deepseek-chat"  # 可以用deepseek-chat，也可以用更强的模型如gpt-4o
}

# --- 2. 评测集 (这次我们不再需要"标准答案"了) ---
EVALUATION_QUERIES = [
    "老年性心瓣膜病有什么症状？",
    "过敏性血管炎的治疗方法有哪些？",
    "蜈蚣咬了会怎么样？",
    "颅内黑色素瘤怎么治？"
]


# --- 3. 核心评测类 ---
class RAGEvaluator:
    def __init__(self, config):
        print("初始化RAG评测器...")
        # 加载基础模型和微调模型
        self.base_model = SentenceTransformer(config["base_model_path"])
        self.fine_tuned_model = SentenceTransformer(config["fine_tuned_model_path"])

        # 加载知识库
        with open(config["sentences_path"], "rb") as f:
            self.all_sentences = pickle.load(f)

        # 为两个模型分别构建索引
        print("正在为两个模型构建FAISS索引...")
        self.base_index = self._build_faiss_index(self.base_model)
        self.fine_tuned_index = self._build_faiss_index(self.fine_tuned_model)

        # 初始化作为“裁判”的LLM
        self.judge_llm = OpenAI(api_key=config['judge_llm_api_key'], base_url=config['judge_llm_api_base'])
        print("评测器准备就绪。")

    def _build_faiss_index(self, model):
        embeddings = model.encode(self.all_sentences, convert_to_tensor=True, show_progress_bar=True, batch_size=128)
        index = faiss.IndexFlatL2(embeddings.shape[1])
        index.add(embeddings.cpu().numpy())
        return index

    def _get_relevance_score(self, query, fact):
        """调用LLM作为裁判，为单个<问题,事实>对打分"""
        prompt = f"""你是一个客观、严谨的AI评测员。你的任务是判断“检索到的事实”在多大程度上能够帮助回答“用户问题”。请根据以下标准，仅输出一个1到5的整数分数。

[评分标准]
5 - 核心答案：事实直接、完整地回答了用户问题。
4 - 重要信息：事实是回答问题所必需的核心信息之一。
3 - 相关背景：事实与问题主题高度相关，能提供有用的背景信息，但不是直接答案。
2 - 轻微相关：事实与问题中的关键词相关，但对回答问题帮助不大。
1 - 完全无关：事实与问题完全不相关。

[用户问题]: {query}
[检索到的事实]: {fact}

[你的评分 (仅一个数字)]:
"""
        try:
            response = self.judge_llm.chat.completions.create(
                model=CONFIG['judge_llm_model_name'],
                messages=[{"role": "user", "content": prompt}],
                max_tokens=5,
                temperature=0
            )
            score_text = response.choices[0].message.content.strip()
            return int(re.search(r'\d+', score_text).group())
        except Exception as e:
            print(f"LLM打分失败: {e}")
            return 0  # 打分失败则记为0分

    def calculate_ndcg_at_k(self, relevance_scores, k):
        """计算nDCG@k"""
        dcg = 0
        for i in range(min(k, len(relevance_scores))):
            dcg += relevance_scores[i] / np.log2(i + 2)

        ideal_scores = sorted(relevance_scores, reverse=True)
        idcg = 0
        for i in range(min(k, len(ideal_scores))):
            idcg += ideal_scores[i] / np.log2(i + 2)

        return dcg / idcg if idcg > 0 else 0

    async def evaluate(self, queries, k):
        results = {"base_model": [], "fine_tuned_model": []}

        for model_name, model, index in [("原始SBERT模型", self.base_model, self.base_index),
                                         ("微调后模型", self.fine_tuned_model, self.fine_tuned_index)]:
            print(f"\n--- 正在使用 '{model_name}' 进行评测 ---")

            all_query_scores = []
            all_ndcg_scores = []

            for query in tqdm(queries, desc=f"评测 {model_name}"):
                query_embedding = model.encode(query, convert_to_tensor=True).cpu().numpy().reshape(1, -1)
                _, retrieved_indices = index.search(query_embedding, k)

                relevance_scores = []
                print(f"\n查询: '{query}'")
                print("检索到的结果与LLM裁判打分:")
                for i, retrieved_id in enumerate(retrieved_indices[0]):
                    fact = self.all_sentences[retrieved_id]
                    score = self._get_relevance_score(query, fact)
                    relevance_scores.append(score)
                    print(f"  {i + 1}. [裁判打分: {score}/5] {fact}")

                all_query_scores.append(np.mean(relevance_scores))
                all_ndcg_scores.append(self.calculate_ndcg_at_k(relevance_scores, k))

            results[model_name] = {
                "Mean Relevance Score": np.mean(all_query_scores),
                f"nDCG@{k}": np.mean(all_ndcg_scores)
            }
        return results


# --- 主程序入口 ---
if __name__ == "__main__":
    import re

    if "YOUR_NEW_API_KEY_HERE" in CONFIG["judge_llm_api_key"]:
        print("错误: 请在脚本中配置您的DeepSeek或GPT的API密钥，用于AI裁判打分。")
        exit()

    evaluator = RAGEvaluator(CONFIG)

    # 使用asyncio运行异步的评测函数
    evaluation_results = asyncio.run(evaluator.evaluate(EVALUATION_QUERIES, k=CONFIG["top_k"]))

    # 打印最终对比报告
    k = CONFIG['top_k']
    base_metrics = evaluation_results["原始SBERT模型"]
    tuned_metrics = evaluation_results["微调后模型"]

    print("\n\n" + "=" * 60)
    print("           高级诊断：智能评测对比报告")
    print("=" * 60)
    print(f"| {'指标 (Metric)':<25} | {'原始模型':<15} | {'微调后模型':<15} |")
    print(f"|{'-' * 27}|{'-' * 17}|{'-' * 17}|")
    print(
        f"| {'平均相关度分数 (1-5分)':<25} | {base_metrics['Mean Relevance Score']:.4f}{'':<10} | {tuned_metrics['Mean Relevance Score']:.4f}{'':<10} |")
    print(
        f"| {'nDCG@' + str(k) + ' (排序质量)':<25} | {base_metrics[f'nDCG@{k}']:.4f}{'':<10} | {tuned_metrics[f'nDCG@{k}']:.4f}{'':<10} |")
    print("=" * 60)
    print("报告解读：")
    print("  - 平均相关度分数：代表模型找回来的信息平均有多大用处，分数越高越好。")
    print(f"  - nDCG@{k}：综合了“相关性”和“排序”的指标，越接近1.0，代表模型越能把最有用的信息排在最前面。")