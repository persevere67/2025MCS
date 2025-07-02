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
import HistoryPage from './HistoryPage.vue';
import logo from '../../assets/logo.png';
import { useAuthStore } from '../../stores/auth';
import { useRouter } from 'vue-router';
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
      historyList: []
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
    submitQuestion() {
      if (!this.question.trim()) {
        alert("请输入问题！");
        return;
      }
      this.answer = "这是针对您的问题的智能回答：" + this.question;
      this.historyList.push(
        {
          title: this.question,
          content: this.answer
        }
      );
      this.question = "";
    },
    async logout() {
      const authStore = useAuthStore();
      const router = useRouter();
      try {
        await authStore.logout();
        this.$message.success("退出登录成功！");
        router.push("/");
      }
      catch (error) {
        this.$message.error("退出登录失败！");
      }
    }
  }
}
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
