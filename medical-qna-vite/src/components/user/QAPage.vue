<template>
  <div class="qa-wrapper">
    <div class="qa-container">
      <!-- 左侧历史记录 -->
      <HistoryPage 
      :historyList="historyList"
      @delete="deleteHistory"
      @select="handleSelectHistory"
      />

      <!-- 右侧问答部分 -->
      <div class="qa-main">

        <!-- 标题栏 -->
        <header class="title-bar">
          <img :src="logo" alt="Logo" class="logo">
          <span>神也吃拼好饭医药问答系统</span>
          <button @click="logout" class="logout-btn">退出登录</button>
        </header>

        <!-- 提问卡片 -->
        <div class="qa-card">
          <h2>提问窗口</h2>
          <textarea v-model="question" placeholder="请输入您的问题..." class="input-area"></textarea>
          <button @click="submitQuestion" class="submit-btn">提交</button>
        </div>

        <!-- 回答展示 -->
        <div v-if="answer" class="answer-card">
          <h3>系统回答</h3>
          <p>{{ answer }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// QAPage.vue 的 <script> 部分

import HistoryPage from './HistoryPage.vue';
import logo from '../../assets/logo.png';

export default {
  components: {
    HistoryPage
  },
  name: "QAPage",
  data() {
    return {
      logo: logo,
      question: "",
      answer: "",
      historyList: [],
      isLoading: false // 添加一个加载状态，防止重复提交
    };
  },
  methods: {
    deleteHistory(index) {
      this.historyList.splice(index, 1);
    },
    handleSelectHistory(item) {
      this.question = item.title;
      this.answer = item.content;
    },
    async submitQuestion() { // 【改动1】: 将方法改为异步 async
      if (!this.question.trim()) {
        alert("请输入问题！");
        return;
      }
      if (this.isLoading) return; // 如果正在加载，则不执行

      this.isLoading = true;
      this.answer = ""; // 清空之前的回答
      const currentQuestion = this.question; // 保存当前问题
      this.question = ""; // 清空输入框
      
      try {
        // 【改动2】: 使用fetch API调用后端
        const response = await fetch('http://127.0.0.1:8000/ask', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ question: currentQuestion }),
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        // 【改动3】: 处理流式响应
        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        while (true) {
          const { done, value } = await reader.read();
          if (done) {
            break;
          }
          const chunk = decoder.decode(value);
          this.answer += chunk; // 将收到的每个文字块追加到回答中
        }
        
        // 流结束后，将完整的问答存入历史记录
        this.historyList.push({
          title: currentQuestion,
          content: this.answer
        });

      } catch (error) {
        console.error("Fetch error:", error);
        this.answer = "抱歉，连接后端服务时发生错误。";
      } finally {
        this.isLoading = false; // 结束加载状态
      }
    }
  }
};
</script>

<style scoped>
/* 背景统一渐变 */
.qa-wrapper {
  height: 100vh;
  background: linear-gradient(135deg, #6EC6CA, #7C82E7);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 页面主体卡片 */
.qa-container {
  width: 90%;
  height: 85%;
  background: #fff;
  display: flex;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 10px 20px rgba(0,0,0,0.15);
  font-family: 'Segoe UI', sans-serif;
}

/* 左侧历史记录 */

.history-panel h3 {
  margin-bottom: 15px;
  color: #333;
}

.history-panel ul {
  list-style: none;
  padding: 0;
}

.history-panel li {
  padding: 10px;
  background: #fff;
  margin-bottom: 10px;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  transition: 0.3s;
}

.history-panel li:hover {
  background: #e0f7fa;
}

/* 右侧问答主区域 */
.qa-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 30px;
  position: relative;
}

/* 顶部标题 */
.title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between; 
  font-size: 24px;
  font-weight: bold;
  color: #4a4a4a;
  margin-bottom: 20px;
}

.logo {
  width: 40px;
  margin-right: 10px;
}

.logout-btn {
  background: #ff6b6b;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: 0.3s;
}

.logout-btn:hover {
  background: #ff5252;
}

/* 提问卡片 */
.qa-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.qa-card h2 {
  margin-bottom: 15px;
}

.input-area {
  width: 100%;
  height: 100px;
  border-radius: 8px;
  border: 1px solid #ddd;
  padding: 10px;
  resize: none;
  font-size: 16px;
  margin-bottom: 15px;
}

.submit-btn {
  background: linear-gradient(90deg, #6EC6CA, #7C82E7);
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 16px;
  transition: 0.3s;
}

.submit-btn:hover {
  opacity: 0.85;
}

/* 回答部分 */
.answer-card {
  background: #fff;
  border-left: 4px solid #7C82E7;
  padding: 15px;
  border-radius: 10px;
  box-shadow: 0 3px 8px rgba(0,0,0,0.1);
}

</style>
