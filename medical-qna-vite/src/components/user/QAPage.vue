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
          <button @click="submitQuestion" :disabled="isLoading" class="submit-btn">
            {{ isLoading ? '处理中...' : '提交' }}
          </button>
        </div>

        <!-- 回答展示 -->
        <div v-if="answer" class="answer-card">
          <h3>系统回答</h3>
          <div class="answer-content">{{ answer }}</div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="isLoading" class="loading-indicator">
          <div class="spinner"></div>
          <span>正在思考中...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HistoryPage from './HistoryPage.vue';
import logo from '../../assets/logo.png';
import api from '@/utils/api';

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
      isLoading: false
    };
  },
  mounted() {
    this.loadHistory();
  },
  methods: {
    async loadHistory() {
      try {
        const response = await api.get('/api/question/history');
        if (response.data.success) {
          this.historyList = response.data.data.map(item => ({
            id: item.id,
            title: item.question,
            content: item.answer,
            createTime: item.createTime
          }));
        }
      } catch (error) {
        console.error('加载历史记录失败:', error);
      }
    },

    async deleteHistory(index) {
      try {
        const historyItem = this.historyList[index];
        if (historyItem.id) {
          const response = await api.delete(`/api/question/history/${historyItem.id}`);
          if (response.data.success) {
            this.historyList.splice(index, 1);
          } else {
            alert('删除失败: ' + response.data.message);
          }
        } else {
          // 本地记录，直接删除
          this.historyList.splice(index, 1);
        }
      } catch (error) {
        console.error('删除历史记录失败:', error);
        alert('删除失败');
      }
    },

    handleSelectHistory(item) {
      this.question = item.title;
      this.answer = item.content;
    },

    async submitQuestion() {
      if (!this.question.trim()) {
        alert("请输入问题！");
        return;
      }
      if (this.isLoading) return;

      this.isLoading = true;
      this.answer = "";
      const currentQuestion = this.question;
      this.question = "";
      
      try {
        // 使用EventSource接收流式响应
        const eventSource = new EventSource(
          `/api/question/ask`, 
          {
            withCredentials: true
          }
        );

        // 发送问题到后端
        const response = await fetch('/api/question/ask', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ question: currentQuestion }),
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        // 处理流式响应
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          
          // 处理可能的多个事件
          const events = buffer.split('\n\n');
          buffer = events.pop() || ''; // 保留不完整的事件

          for (const event of events) {
            if (event.trim()) {
              const lines = event.split('\n');
              let eventType = 'data';
              let eventData = '';

              for (const line of lines) {
                if (line.startsWith('event:')) {
                  eventType = line.substring(6).trim();
                } else if (line.startsWith('data:')) {
                  eventData = line.substring(5).trim();
                }
              }

              if (eventType === 'data' && eventData) {
                this.answer += eventData;
              } else if (eventType === 'error') {
                throw new Error(eventData);
              } else if (eventType === 'complete') {
                // 答案完成，添加到历史记录
                this.historyList.unshift({
                  title: currentQuestion,
                  content: this.answer,
                  createTime: new Date().toISOString()
                });
                break;
              }
            }
          }
        }

      } catch (error) {
        console.error("提交问题失败:", error);
        this.answer = "抱歉，连接服务时发生错误：" + error.message;
      } finally {
        this.isLoading = false;
      }
    },

    async logout() {
      try {
        await api.post('/api/auth/logout');
        this.$router.push('/');
      } catch (error) {
        console.error('退出登录失败:', error);
        // 即使退出失败也跳转到首页
        this.$router.push('/');
      }
    }
  }
}
</script>

<style scoped>
.qa-wrapper {
  height: 100vh;
  background: linear-gradient(135deg, #6EC6CA, #7C82E7);
  display: flex;
  align-items: center;
  justify-content: center;
}

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

.qa-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 30px;
  position: relative;
}

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
  box-sizing: border-box;
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

.submit-btn:hover:not(:disabled) {
  opacity: 0.85;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.answer-card {
  background: #fff;
  border-left: 4px solid #7C82E7;
  padding: 15px;
  border-radius: 10px;
  box-shadow: 0 3px 8px rgba(0,0,0,0.1);
  overflow: auto;
}

.answer-content {
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #666;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #7C82E7;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>