<template>
  <div class="qa-wrapper">
    <div class="qa-container">
      <HistoryPage
        :historyList="historyList"
        :isLoading="historyLoading"
        @delete="deleteHistory"
        @select="handleSelectHistory"
        @clear="clearAllHistory"
        @refresh="loadHistory"
      />

      <div class="qa-main">
        <header class="title-bar">
          <div class="title-left">
            <img :src="logo" alt="Logo" class="logo" />
            <span class="title-text">神也吃拼好饭医药问答系统</span>
          </div>
          <div class="header-actions">
            <div class="status-indicator connected">
              <span class="status-dot"></span>
              <span class="status-text">服务正常</span>
            </div>
            <div class="stats-info" v-if="userStats">
              <span class="stats-item">👤 {{ userStats.username }}</span>
              <span class="stats-item">📊 共{{ userStats.totalQuestions }}个问题</span>
            </div>
            
            <!-- 操作按钮 -->
            <button @click="refreshConnection" class="action-btn" :disabled="isRefreshing">
              <span v-if="isRefreshing">🔄</span>
              <span v-else>🔄</span>
              刷新
            </button>
            <button @click="logout" class="logout-btn">
              🚪 退出登录
            </button>
            <div class="action-buttons">
              <button @click="logout" class="logout-btn">
                🚪 退出登录
              </button>
            </div>
          </div>
        </header>

        <div class="qa-card">
          <div class="card-header">
            <h2>💬 AI医疗助手</h2>
            <div class="input-tips">
              <span class="tip">💡 提示：请详细描述您的医疗问题，AI助手将为您提供专业建议。支持多轮对话。</span>
            </div>
          </div>
          <div class="input-section">
            <textarea
              v-model="question"
              placeholder="请详细描述您的症状或医疗问题...&#10;例如：我最近经常头痛，特别是下午的时候，持续了一周了..."
              class="input-area"
              :disabled="isLoading"
              @keydown.ctrl.enter="submitQuestion"
              @input="onQuestionInput"
              rows="4"
            ></textarea>
            <div class="input-footer">
              <div class="input-info">
                <span class="char-count" :class="{ 'limit-warning': question.length > 1000 }">
                  {{ question.length }}/1500 字符
                </span>
                <span class="shortcut-tip">Ctrl + Enter 快速提交</span>
              </div>
              <div class="submit-section">
                <button
                  @click="clearInput"
                  class="clear-btn"
                  v-if="question.trim()"
                  :disabled="isLoading"
                >
                  🗑️ 清空
                </button>
                <button
                  @click="submitQuestion"
                  :disabled="!canSubmit"
                  class="submit-btn"
                  :class="{ 'pulse': question.trim() && !isLoading }"
                >
                  <span v-if="isLoading">
                    <span class="loading-spinner"></span>
                    AI思考中...
                  </span>
                  <span v-else>
                    🚀 提交问题
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="showAnswerArea" class="answer-section">
          <div v-if="currentQuestion" class="current-question">
            <h4>📝 您的问题：</h4>
            <p>{{ currentQuestion }}</p>
          </div>

          <div class="answer-card">
            <div class="answer-header">
              <h3>
                <span class="ai-avatar">🤖</span>
                AI医疗助手回答
                <span v-if="isLoading" class="typing-indicator">
                  <span></span><span></span><span></span>
                </span>
              </h3>
              <div class="answer-actions" v-if="!isLoading && answer">
                <button @click="copyAnswer" class="action-btn" title="复制回答">
                  📋 复制
                </button>
                <button @click="speakAnswer" class="action-btn" title="语音朗读" v-if="supportsSpeech">
                  🔊 朗读
                </button>
                <button @click="clearAnswer" class="action-btn" title="清除回答">
                  🗑️ 清除
                </button>
              </div>
            </div>
            <div class="answer-content">
              <div v-if="isLoading && !answer" class="loading-state">
                <div class="loading-animation">
                  <div class="dot"></div>
                  <div class="dot"></div>
                  <div class="dot"></div>
                </div>
                <p class="loading-text">{{ loadingText }}</p>
              </div>
              <div v-if="answer" class="answer-text">
                <div class="answer-body" v-html="answer"></div>
                <div class="answer-footer" v-if="!isLoading">
                  <span class="answer-time">回答于 {{ answerTime }}</span>
                  <span class="answer-length">{{ answer.length }} 字符</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="errorMessage" class="error-card">
          <div class="error-header">
            <h4>❌ 出现错误</h4>
            <button @click="clearError" class="close-btn">✕</button>
          </div>
          <p class="error-text">{{ errorMessage }}</p>
          <div class="error-actions">
            <button @click="retryLastQuestion" class="retry-btn" v-if="lastQuestion">
              🔄 重试
            </button>
            <button @click="clearError" class="dismiss-btn">
              ✓ 知道了
            </button>
          </div>
        </div>

        <div class="quick-actions" v-if="!isLoading">
          <h4>🔧 快捷操作</h4>
          <div class="action-grid">
            <button @click="loadSampleQuestion('症状咨询')" class="quick-btn">
              🩺 症状咨询
            </button>
            <button @click="loadSampleQuestion('用药指导')" class="quick-btn">
              💊 用药指导
            </button>
            <button @click="loadSampleQuestion('健康建议')" class="quick-btn">
              🏃‍♂️ 健康建议
            </button>
            <button @click="loadSampleQuestion('急救知识')" class="quick-btn">
              🚑 急救知识
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="globalMessage" class="global-message" :class="globalMessageType">
      <span>{{ globalMessage }}</span>
      <button @click="globalMessage = ''" class="close-btn">✕</button>
    </div>
  </div>
</template>

<script>
import HistoryPage from './HistoryPage.vue';
import logo from '../../assets/logo.png';
import api, { authUtils } from '@/utils/api'; // 确保导入 authUtils

export default {
  name: "QAPage",
  components: {
    HistoryPage
  },
  data() {
    return {
      logo: logo,
      question: "",
      answer: "",
      currentQuestion: "",
      lastQuestion: "",
      historyList: [],
      chatHistory: [], // We use a new array to specifically manage conversation history for the API
      userStats: null,
      isLoading: false,
      historyLoading: false,
      errorMessage: "",
      globalMessage: "",
      globalMessageType: "info",
      loadingText: "AI助手正在思考您的问题...",
      answerTime: "",
      
      // 新增：推荐问题数据
      // 确保这里的结构与后端 MedicalRecommendationResponse.RecommendedQuestion 匹配
      // 后端返回的是 { questionText: "...", questionId: "...", relevantEntities: [], relevanceScore: ... }
      recommendedQuestions: [], 

      // RAG服务配置 (现在将调用 Spring Boot 后端，后端再调用 RAG)
      backendAskUrl: "http://localhost:8080/api/qa/ask", // Spring Boot 后端问答接口
      
      // Token 监控
      // Token monitoring
      tokenCheckInterval: null,
      // Loading status text rotation
      loadingTexts: [
        "AI助手正在分析您的问题...",
        "正在查询医疗知识库...",
        "正在生成专业建议...",
        "即将为您呈现答案..."
      ],
      loadingTextIndex: 0,
      loadingInterval: null,
      
      // 语音朗读支持
      supportsSpeech: 'speechSynthesis' in window,
      isRefreshing: false, // 新增：刷新按钮状态
      connectionStatus: 'unknown', // 新增：连接状态
      connectionStatusText: '连接中...', // 新增：连接状态文本
      loadingInterval: null,
      // Speech synthesis
      supportsSpeech: 'speechSynthesis' in window,
    };
  },
  computed: {
    canSubmit() {
      return this.question.trim() &&
             !this.isLoading &&
             this.question.length <= 1500 &&
             authUtils.isLoggedIn();
    },
    showAnswerArea() {
      return this.answer || this.isLoading || this.currentQuestion;
    }
  },
  mounted() {
    console.log('QAPage component mounted');
    this.initializePage();
    this.checkBackendHealth(); // 页面挂载时检查后端健康状态
  },
  beforeUnmount() {
    this.clearLoadingInterval();
    this.stopTokenMonitoring();
  },
  methods: {
    // --- Authentication and Initialization ---
    checkAuth() {
      try {
        if (!authUtils.isLoggedIn()) {
          this.showGlobalMessage('登录已过期，请重新登录', 'warning');
          this.$router.push('/');
          return false;
        }
        const userInfo = authUtils.getUserInfo();
        if (userInfo) {
          this.userStats = {
            username: userInfo.username,
            userId: userInfo.userId,
            role: userInfo.role,
            totalQuestions: 0
          };
          return true;
        } else {
          authUtils.clearToken();
          this.$router.push('/');
          return false;
        }
      } catch (error) {
        console.error('Authentication check failed:', error);
        authUtils.clearToken();
        this.$router.push('/');
        return false;
      }
    },

    async initializePage() {
      this.showGlobalMessage('正在初始化页面...', 'info');
      try {
        if (!this.checkAuth()) {
          return;
        }
        this.startTokenMonitoring();
        await this.loadHistory();
        await this.loadUserStats();
        this.showGlobalMessage('页面加载完成', 'success');
      } catch (error) {
        console.error('Page initialization failed:', error);
        this.showGlobalMessage('页面初始化失败: ' + error.message, 'error');
      }
    },

    // --- History Management ---
    async loadHistory() {
      try {
        this.historyLoading = true;
        // 确保 api.question.getHistory() 存在且返回正确的数据结构
        const result = await api.question.getHistory(); 
        
        const result = await api.question.getHistory();
        if (result.success) {
          this.historyList = result.data.map(item => ({
            id: item.id,
            title: item.question,
            content: item.answer,
            createTime: item.createAt // 修正：使用 createAt
          }));
          if (this.userStats) {
            this.userStats.totalQuestions = this.historyList.length;
          }
        }
      } catch (error) {
        console.error('Failed to load history records:', error);
        this.showError('加载历史记录失败');
      } finally {
        this.historyLoading = false;
      }
    },

    async loadUserStats() {
      try {
        // 确保 api.question.getStats() 存在且返回正确的数据结构
        const result = await api.question.getStats(); 
        if (result.success) {
          if (this.userStats) {
            this.userStats = { ...this.userStats, ...result.data };
          }
        }
      } catch (error) {
        console.error('Failed to load user statistics:', error);
      }
    },

    async deleteHistory(id) { // 接收 id
      try {
        // 确保 api.question.deleteHistory() 存在且返回正确的数据结构
        const result = await api.question.deleteHistory(id); 
        if (result.success) {
          this.historyList = this.historyList.filter(item => item.id !== id); // 根据 id 过滤
          this.showGlobalMessage('删除成功', 'success');
          await this.loadUserStats();
        } else {
          this.showError('删除失败: ' + result.message);
        }
      } catch (error) {
        console.error('Failed to delete history record:', error);
        this.showError('删除失败');
      }
    },

    async clearAllHistory() {
      if (!confirm('确定要清空所有历史记录吗？此操作无法撤销。')) {
        return;
      }
      try {
        // 确保 api.question.clearHistory() 存在且返回正确的数据结构
        const result = await api.question.clearHistory(); 
        if (result.success) {
          this.historyList = [];
          this.chatHistory = []; // Also clear API conversation history
          this.showGlobalMessage('历史记录已清空', 'success');
          await this.loadUserStats();
        } else {
          this.showError('清空失败: ' + result.message);
        }
      } catch (error) {
        console.error('Failed to clear history records:', error);
        this.showError('清空失败');
      }
    },

    handleSelectHistory(item) {
      this.question = item.title;
      this.answer = item.content;
      this.currentQuestion = item.title;
      this.answerTime = new Date(item.createTime).toLocaleString(); // 修正：使用 createTime
      this.clearError();
      this.recommendedQuestions = []; // 清空推荐问题
    },

    // --- Question Submission and Answer Processing ---
    async submitQuestion() {
      if (!this.canSubmit) return;

      if (!authUtils.isLoggedIn()) {
        this.showError('登录已过期，请重新登录');
        this.$router.push('/');
        return;
      }

      this.isLoading = true;
      this.answer = '';
      this.errorMessage = '';
      this.recommendedQuestions = []; // 清空推荐问题
      this.currentQuestion = this.question.trim();
      this.lastQuestion = this.currentQuestion;
      this.question = '';
      const currentQuestion = this.question.trim();
      this.currentQuestion = currentQuestion;
      this.lastQuestion = currentQuestion;
      this.question = ''; // Clear input field

      this.startLoadingAnimation();

      try {
        // 调用 Spring Boot 后端获取流式回答和推荐问题
        await this.callBackendAskService(this.currentQuestion);
        
      } catch (error) {
        console.error('提交问题失败:', error);
        this.showError('提交问题失败: ' + error.message);
      } finally {
        this.isLoading = false;
        this.clearLoadingInterval();
      }
    },

    // 新增：调用后端问答接口并处理 SSE 流
    async callBackendAskService(question) {
      console.log('开始调用后端问答服务...');
      try {
        const token = authUtils.getToken();
        if (!token) {
          throw new Error('请先登录以获取认证信息');
        }

        const response = await fetch(this.backendAskUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}` // 发送 JWT Token
          },
          body: JSON.stringify({ question: question }) // 发送 JSON 请求体
      // 【Core Change】: Prepare conversation history to be sent to the backend
      // We only send the last 2 turns of conversation to prevent the prompt from getting too long
      const historyForApi = this.chatHistory.slice(-2);

      try {
        // 【Core Change】: Add history to the POST body
        const response = await fetch('/api/question/ask', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer my-super-secret-token-for-med-qa'
            },
            body: JSON.stringify({
                question: currentQuestion,
                history: historyForApi // <--- Pass history
            }),
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: response.statusText }));
            throw new Error(errorData.message || `Service returned error ${response.status}`);
        }

        // Stream processing SSE response, compatible with extreme cases where chunks are split
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let answerText = '';
        let isAnswerStarted = false;

        console.log('开始读取SSE流...');
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          // Process as long as there is 'data:' (compatible with multiple lines/splits)
          let dataIdx;
          while ((dataIdx = buffer.indexOf('data:')) !== -1) {
            // Find the next 'data:', then find the next 'data:' or the end
            let nextIdx = buffer.indexOf('data:', dataIdx + 5);
            let chunkStr = nextIdx !== -1 ? buffer.slice(dataIdx, nextIdx) : buffer;

            // If it's not a complete JSON and not the last chunk, break and wait for the next chunk
            if (!chunkStr.includes('\n\n') && nextIdx === -1) break;

            // Process chunkStr
            let lines = chunkStr.split('\n');
            for (let line of lines) {
              line = line.trim();
              if (line.startsWith('data:')) {
                const dataStr = line.replace(/^data:\s*/, '');
                if (!dataStr) continue;
                try {
                  if (dataStr === '{}' || dataStr === '[DONE]') continue;
                  if (dataStr.startsWith('{') && dataStr.endsWith('}')) {
                    const obj = JSON.parse(dataStr);
                    if (obj.token !== undefined) {
                      isAnswerStarted = true;
                      answerText += obj.token;
                      this.answer = answerText;
                    }
                  }
                } catch (e) {
                  // Not JSON, ignore
                }
              }
            }
            // Remove the processed part
            buffer = nextIdx !== -1 ? buffer.slice(nextIdx) : '';
          }
        }
        // Assign after completion
        this.answer = answerText;
        if (!isAnswerStarted) {
          this.answer = '未收到AI回答内容，可能后端服务异常或知识库无匹配。';
        }
        this.onAnswerComplete();
      } catch (error) {
        console.error('Failed to submit question:', error);
        this.showError('提交问题失败: ' + error.message);
        this.answer = `抱歉，请求出错。请稍后重试。\n错误详情: ${error.message}`;
      } finally {
        this.isLoading = false;
        this.clearLoadingInterval();
      }
    },

    onAnswerComplete() {
      this.answerTime = new Date().toLocaleString();
      // Update the historyList for display (you already have this logic)
      // Note: We manually update the list here to provide immediate feedback, rather than re-calling loadHistory()
      this.historyList.push({
        title: this.currentQuestion,
        content: this.answer,
        createTime: this.answerTime
      });

      // 【New】Update chatHistory for API calls
      this.chatHistory.push({
        "question": this.currentQuestion,
        "answer": this.answer
      });

      this.loadUserStats();
      this.showGlobalMessage('问答完成', 'success');
    },

    // --- UI/Animation Helpers ---
    startLoadingAnimation() {
      this.loadingTextIndex = 0;
      this.loadingText = this.loadingTexts[0];
      this.loadingInterval = setInterval(() => {
        this.loadingTextIndex = (this.loadingTextIndex + 1) % this.loadingTexts.length;
        this.loadingText = this.loadingTexts[this.loadingTextIndex];
      }, 2000);
    },

    clearLoadingInterval() {
      if (this.loadingInterval) {
        clearInterval(this.loadingInterval);
        this.loadingInterval = null;
      }
    },

    // --- Token Monitoring ---
    startTokenMonitoring() {
      this.tokenCheckInterval = setInterval(() => {
        if (!authUtils.isLoggedIn()) {
          this.showGlobalMessage('登录已过期，即将跳转到登录页', 'warning');
          setTimeout(() => {
            this.$router.push('/');
          }, 2000);
          this.stopTokenMonitoring();
        }
      }, 60000); // Check every minute
    },

    stopTokenMonitoring() {
      if (this.tokenCheckInterval) {
        clearInterval(this.tokenCheckInterval);
        this.tokenCheckInterval = null;
      }
    },

    // --- Utility Methods ---
    copyAnswer() {
      if (this.answer) {
        navigator.clipboard.writeText(this.answer).then(() => {
          this.showGlobalMessage('已复制到剪贴板', 'success');
        }).catch(() => {
          this.showError('复制失败');
        });
      }
    },
    speakAnswer() {
      if (!this.supportsSpeech || !this.answer) return;
      const utterance = new SpeechSynthesisUtterance(this.answer);
      speechSynthesis.speak(utterance);
    },

    clearAnswer() {
      this.answer = "";
      this.currentQuestion = "";
      this.recommendedQuestions = []; // 清空推荐问题
      this.clearError();
      // When clearing the current answer, also clear the API history to start a new conversation flow
      this.chatHistory = [];
      this.showGlobalMessage('对话已重置', 'info');
    },

    clearInput() {
      this.question = "";
    },

    clearError() {
      this.errorMessage = "";
    },

    retryLastQuestion() {
      if (this.lastQuestion) {
        this.question = this.lastQuestion;
        this.clearError();
        this.submitQuestion();
      }
    },

    onQuestionInput() {
      if (this.question.length > 1500) {
        this.question = this.question.substring(0, 1500);
        this.showGlobalMessage('问题长度已达到上限', 'warning');
      }
    },

    loadSampleQuestion(type) {
      const samples = {
        '症状咨询': '我最近经常头痛，特别是下午的时候，持续了一周，请问可能是什么原因？',
        '用药指导': '请问感冒药和消炎药可以一起服用吗？有什么需要注意的？',
        '健康建议': '我想了解如何保持心血管健康，日常生活中应该注意什么？',
        '急救知识': '如果有人突然晕倒了，我应该如何进行急救处理？'
      };
      this.question = samples[type] || '';
    },

    // 新增：加载推荐问题到输入框并提交
    loadRecommendedQuestion(recQuestion) {
      this.question = recQuestion;
      this.submitQuestion(); // 自动提交问题
    },

    showError(message) {
      this.errorMessage = message;
      setTimeout(() => {
        if (this.errorMessage === message) {
          this.errorMessage = "";
        }
      }, 10000);
    },

    showGlobalMessage(message, type = 'info') {
      this.globalMessage = message;
      this.globalMessageType = type;
      setTimeout(() => {
        if (this.globalMessage === message) {
          this.globalMessage = "";
        }
      }, 3000);
    },

    async logout() {
      try {
        this.stopTokenMonitoring();
        try {
          // 确保 api.auth.logout() 存在
          await api.auth.logout(); 
        } catch (logoutError) {
          console.warn('Server logout failed, but local state cleared:', logoutError);
        }
        this.showGlobalMessage('已退出登录', 'success');
        setTimeout(() => {
          this.$router.push('/');
        }, 1000);
      } catch (error) {
        console.error('Failed to log out:', error);
        authUtils.clearToken();
        this.$router.push('/');
      }
    }
  },

  // --- Router Guards ---
  beforeRouteEnter(to, from, next) {
    if (!authUtils.isLoggedIn()) {
      console.log('QAPage: User not logged in, redirecting to homepage');
      next('/');
    } else {
      console.log('QAPage: User logged in, access granted');
      next();
    }
  },

  beforeRouteLeave(to, from, next) {
    this.stopTokenMonitoring();
    this.clearLoadingInterval();
    next();
  }
};
</script>

<style scoped>
/* Basic Layout */
.qa-wrapper {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}

.qa-container {
  width: 95%;
  height: 90%;
  background: #ffffff;
  display: flex; /* 保持 flex 布局 */
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
}

/* 历史记录部分 */
/* HistoryPage 组件的样式可能由其自身管理，或者在这里设置 flex 属性 */
/* 假设 HistoryPage 默认宽度，或者通过 flex-basis 控制 */
/* .history-page-wrapper { flex: 0 0 250px; } */ /* 如果 HistoryPage 是一个直接的 div */

.qa-main {
  flex: 3; /* 占据中间大部分空间 */
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, #fafbfc 0%, #f8f9fa 100%);
}

/* Title Bar */
.title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 0;
  border-bottom: 2px solid #e9ecef;
}

.title-left {
  display: flex;
  align-items: center;
}

.logo {
  width: 48px;
  height: 48px;
  margin-right: 12px;
  border-radius: 8px;
}

.title-text {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  background: linear-gradient(45deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Status Indicator */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 20px;
  background: rgba(255,255,255,0.9);
  border: 1px solid #dee2e6;
  font-size: 14px;
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-indicator.connected .status-dot {
  background: #28a745;
}

.status-indicator.disconnected .status-dot {
  background: #dc3545;
}

.status-indicator.unknown .status-dot {
  background: #ffc107;
}

/* Statistics Info */
.stats-info {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #6c757d;
}

.stats-item {
  padding: 4px 8px;
  background: rgba(255,255,255,0.8);
  border-radius: 12px;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn, .logout-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 18px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-btn {
  background: #6c757d;
  color: white;
}

.action-btn:hover:not(:disabled) {
  background: #5a6268;
  transform: translateY(-1px);
}

.logout-btn {
  background: #dc3545;
  color: white;
}

.logout-btn:hover {
  background: #c82333;
  transform: translateY(-1px);
}

/* Status Card (Note: You have .error-card below, consider consolidating or clarifying purpose if both are status-related) */
.status-card {
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  border-left: 4px solid;
}

.status-card.error {
  background: #f8d7da;
  border-color: #dc3545;
  color: #721c24;
}

.retry-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 12px;
}

/* QA Card */
.qa-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e9ecef;
}

.card-header h2 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 20px;
}

.input-tips {
  margin-bottom: 16px;
}

.tip {
  font-size: 14px;
  color: #6c757d;
  background: #e7f3ff;
  padding: 8px 12px;
  border-radius: 8px;
  display: inline-block;
}

/* Input Area */
.input-section {
  /* Added for consistent spacing */
  display: flex;
  flex-direction: column;
}

.input-area {
  width: 100%;
  min-height: 120px;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  padding: 16px;
  font-size: 16px;
  line-height: 1.5;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.3s ease;
}

.input-area:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-area:disabled {
  background: #f8f9fa;
  cursor: not-allowed;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.input-info {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #6c757d;
}

.char-count.limit-warning {
  color: #dc3545;
  font-weight: 600;
}

.submit-section {
  display: flex;
  gap: 8px;
}

.clear-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn {
  background: linear-gradient(45deg, #667eea, #764ba2);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
}

.submit-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
  transform: none;
}

.submit-btn.pulse {
  animation: pulse-glow 2s infinite;
}

/* Answer Area */
.answer-section {
  margin-bottom: 24px;
  flex-grow: 1; /* 允许回答区域在 qa-main 中增长 */
  display: flex;
  flex-direction: column;
}

.current-question {
  background: #e7f3ff;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 16px;
  border-left: 4px solid #007bff;
}

.current-question h4 {
  margin: 0 0 8px 0;
  color: #007bff;
}

.answer-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e9ecef;
  flex-grow: 1; /* 让回答卡片填充剩余空间 */
  display: flex;
  flex-direction: column;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.answer-header h3 {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  color: #2c3e50;
}

.ai-avatar {
  font-size: 24px;
}

.answer-actions {
  display: flex;
  gap: 8px;
}

.answer-content {
  line-height: 1.7;
  flex-grow: 1; /* 让内容区域填充回答卡片剩余空间 */
  overflow-y: auto; /* 如果内容过多，允许滚动 */
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
}

.loading-animation {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.loading-animation .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: loading-bounce 1.4s ease-in-out infinite both;
}

.loading-animation .dot:nth-child(1) { animation-delay: -0.32s; }
.loading-animation .dot:nth-child(2) { animation-delay: -0.16s; }

.loading-text {
  color: #6c757d;
  font-style: italic;
}

.answer-text {
  color: #2c3e50;
}

.answer-body {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.answer-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #6c757d;
}

/* Error Card */
.error-card {
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.error-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.error-header h4 {
  margin: 0;
  color: #721c24;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #721c24;
}

.error-text {
  color: #721c24;
  margin-bottom: 16px;
}

.error-actions {
  display: flex;
  gap: 8px;
}

.dismiss-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
}

/* Quick Actions */
.quick-actions {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e9ecef;
  margin-bottom: 24px; /* 添加间距 */
}

.quick-actions h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.quick-btn {
  background: linear-gradient(45deg, #f8f9fa, #e9ecef);
  border: 1px solid #dee2e6;
  padding: 12px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.quick-btn:hover {
  background: linear-gradient(45deg, #667eea, #764ba2);
  color: white;
  transform: translateY(-2px);
}

/* Global Message */
.global-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.global-message.info { background: #17a2b8; }
.global-message.success { background: #28a745; }
.global-message.warning { background: #ffc107; color: #212529; }
.global-message.error { background: #dc3545; }

/* Animations */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 5px rgba(102, 126, 234, 0.5); }
  50% { box-shadow: 0 0 20px rgba(102, 126, 234, 0.8); }
}

@keyframes loading-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.typing-indicator span {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #667eea;
  margin: 0 1px;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
  .typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}

.loading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Responsive Design */
@media (max-width: 768px) {
  .qa-container {
    width: 100%;
    height: 100%;
    flex-direction: column;
    border-radius: 0;
  }
  .title-bar {
    flex-direction: column;
    gap: 12px;
  }
  .header-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
  .action-grid {
    grid-template-columns: 1fr;
  }
  .input-footer {
    flex-direction: column;
    gap: 12px;
  }
  .answer-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
