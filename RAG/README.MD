测试：

```
curl -X POST http://localhost:8000/ask -H "Content-Type: application/json" -d "{\"question\": \"什么是糖尿病？\"}"
```
# 智能医疗问答RAG系统

这是一个基于 **检索增强生成（Retrieval-Augmented Generation, RAG）** 架构的智能医疗问答系统。项目旨在利用最先进的自然语言处理技术，结合结构化（知识图谱）与非结构化（文本）的医疗数据，为用户提供精准、可靠且人性化的医疗健康咨询服务。

![系统界面截图](https://i.imgur.com/your-screenshot-url.png)
*（可选）在这里替换成您前端页面的截图*

-----

## 核心技术栈

本项目的实现依赖于一系列现代化的AI与Web开发技术：

  * **后端框架**: **FastAPI+SPRINGBOOT** - 一个高性能、易于使用的Python Web框架，用于构建API服务。
  * **前端框架**: **Vue.js** - 一款流行的、渐进式JavaScript框架，用于构建用户界面。
  * **检索模型 (Retriever)**: **Sentence-Transformers** - 基于PyTorch的库，用于训练和使用最先进的句子、文本和图像嵌入模型。我们使用它来微调一个专门用于理解医疗语义的\*\*双编码器（Bi-Encoder）\*\*模型。
  * **向量数据库**: **FAISS (Facebook AI Similarity Search)** - 一个由Facebook AI开发的高性能向量相似性搜索库，用于存储和快速检索知识库向量。
  * **生成模型 (Generator)**: **DeepSeek LLM** - 通过API调用的方式，利用其强大的自然语言生成能力，将检索到的事实“润色”成流畅、人性化的回答。

-----

## 项目架构

本系统遵循经典的RAG架构，其工作流程如下：

1.  **离线处理 (Offline Processing)**:

      * **知识提取与转换**: 使用`generate_training_data.py`脚本，从多个源（如疾病JSON、药品JSON、知识图谱等）提取信息，并将其统一转换为大量的`["问题", "答案"]`格式的训练对。
      * **模型微调**: 使用`final_train.py`脚本，在高质量的训练对上对基础的Sentence-BERT模型进行微调，使其成为一个医疗领域的语义理解专家。
      * **知识库索引**: 使用`rag_app.py`（或测试脚本）的首次运行功能，将知识库中的所有“答案”文本通过微调好的模型编码成向量，并存入一个FAISS索引文件中。

2.  **在线问答 (Online Serving)**:

      * **API服务**: `main.py`启动一个FastAPI服务，加载微调好的SBERT模型和FAISS索引，随时准备接收请求。
      * **用户提问**: 前端Vue应用将用户输入的问题发送到后端`/api/question/ask`接口。
      * **检索 (Retrieve)**: 后端使用SBERT模型将用户问题编码成向量，然后在FAISS索引中进行高速语义搜索，找出最相关的几条“事实”作为上下文。
      * **生成 (Generate)**: 后端将原始问题和检索到的上下文打包成一个精心设计的Prompt，通过API发送给DeepSeek LLM。
      * **流式响应**: LLM以流式模式生成回答，后端通过SSE（服务器发送事件）协议将回答逐字实时地传回前端，实现打字机效果。

-----

## 项目文件结构

RAG/
│
├── data/
│   ├── medical.json           # 原始疾病医药数据
│   ├── generate_from_new_data #将加载好的json项转化为问答对
│   ├── training_qa_pairs_final.json # 生成的训练数据
│   ├── final_medical_knowledge.faiss # 生成的FAISS索引
│   └── final_medical_sentences.pkl # 生成的事实句子列表
│
├── epoch_3/                    # 您最终训练好的SBERT模型文件夹
│   ├── config.json
│   ├── modules.json
│   └── ... (其他模型文件)
│
├── scripts/
│   ├── generate_from_new_data.py # 用于从新数据源生成问答对的脚本
│   |── final_train.py            # 最终的模型训练脚本
|   └── rag_app.py                # 集成后的核心逻辑
|   
│
├── main.py                     # 后端FastAPI服务的入口文件
|── README.md                   # 项目说明文件 (就是本文件)
|── requirements.txt            # conda环境需要安装的Python软件包
└── .gitignore                  # Git上传忽略的部分大文件


-----

## 环境配置与安装

在运行本项目前，请确保您已安装Python（推荐3.10+）和Node.js（推荐16+）。

### 1\. 后端环境配置
模型部分建议使用Conda创建独立的虚拟环境。
后端应用部分建议配置好Springboot及其相关环境
```bash
# 1. 创建并激活Conda环境
conda create -n rag_env python=3.10
conda activate rag_env

# 2. 安装所有Python依赖
# (请确保您项目根目录下有一个 requirements.txt 文件，或者逐一安装)
# pip install torch torchvision torchaudio --index-url [https://download.pytorch.org/whl/cu121](https://download.pytorch.org/whl/cu121)
# pip install sentence-transformers faiss-cpu "fastapi[all]" sse-starlette "python-jose[cryptography]" openai
pip install -r requirements.txt # 推荐方式

# 3. 配置API密钥
# (推荐) 在终端中设置环境变量
# set DEEPSEEK_API_KEY=sk-e40b773f247748f1b4d5d831cb3a8987  (Windows CMD)
# $env:DEEPSEEK_API_KEY="sk-e40b773f247748f1b4d5d831cb3a8987" (Windows PowerShell)
# export DEEPSEEK_API_KEY=sk-e40b773f247748f1b4d5d831cb3a8987 (Linux/macOS)
# 或者，直接修改 main.py 中的 CONFIG 字典
2. 前端环境配置
Bash

# 1. 进入前端项目目录 (假设在 src 文件夹的父目录)
# cd path/to/your/vue-project

# 2. 安装Node.js依赖
npm install

# 3. (重要) 配置API代理
# 为了让前端(如 http://localhost:8080)能访问后端API([http://127.0.0.1:8000](http://127.0.0.1:8000))而不产生跨域问题，
# 请在Vue项目的根目录下创建或修改 vue.config.js 文件，添加以下内容：
module.exports = {
  devServer: {
    proxy: {
      '/api': {
        target: '[http://127.0.0.1:8000](http://127.0.0.1:8000)',
        changeOrigin: true,
        pathRewrite: { '^/api': '' } // 注意：如果后端的路径已经包含了/api，这里可能需要调整
      }
    }
  }
};
运行指南
1. 启动后端服务
在您的Conda环境(rag_env)激活的状态下，在项目根目录运行：

Bash

uvicorn main:app --reload
服务将在 http://127.0.0.1:8000 启动。您应该会看到RAG系统开始初始化并加载模型的日志。

2. 启动前端服务
打开一个新的终端，进入您的Vue项目目录，运行：

Bash

npm run serve
您的Web应用通常会在 http://localhost:8080 (或类似地址) 启动。

3. 开始使用
打开浏览器访问您的前端页面。由于系统包含认证，您可能需要先通过您自己的登录流程获取Token，或者在开发者工具中手动设置一个用于测试的Token。之后，您就可以开始与您的智能医疗问答系统对话了！

未来展望 (TODO)
[ ] 数据库集成: 为历史记录、用户统计等功能接入一个持久化数据库（如SQLite或PostgreSQL）。

[ ] 管理员系统: 开发一个后台管理界面，用于知识库管理、问答日志审计和系统配置。

[ ] 智能推荐: 实现“您可能还想问”功能，通过知识图谱或LLM为用户提供相关问题推荐。

[ ] 模型持续优化: 建立“人工反馈闭环（Human-in-the-Loop）”，利用管理员的标注数据对SBERT模型进行增量微调。