<template>
  <div class="qa-wrapper">
    <div class="qa-container">
      <!-- 历史记录侧边栏组件 -->
      <HistoryPage
        :historyList="historyList"
        :isLoading="historyLoading"
        @delete="deleteHistory"
        @select="handleSelectHistory"
        @clear-all="clearAllHistory"
        @refresh="loadHistory"
      />

      <div class="qa-main">
        <!-- 顶部标题栏和操作区 -->
        <header class="title-bar">
          <div class="title-left">
            <img :src="logo" alt="Logo" class="logo" />
            <span class="title-text">神也吃拼好饭医药问答系统</span>
          </div>
          <div class="header-actions">
            <!-- 连接状态指示器 -->
            <div :class="['status-indicator', connectionStatus]">
              <span class="status-dot"></span>
              <span class="status-text">{{ connectionStatusText }}</span>
            </div>
            <!-- 用户统计信息 -->
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
          </div>
        </header>

        <!-- 问题输入卡片 -->
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

        <!-- 答案显示区域 -->
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

        <!-- 新增：猜你想问区域 -->
        <div v-if="recommendedQuestions.length > 0 && !isLoading" class="recommended-questions">
          <h4>🤔 猜你想问</h4>
          <div class="recommendation-grid">
            <button v-for="(rec, index) in recommendedQuestions" :key="index" @click="loadRecommendedQuestion(rec)" class="rec-btn">
              {{ rec }}
            </button>
          </div>
        </div>

        <!-- 错误信息卡片 -->
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

        <!-- 快捷操作按钮区 -->
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

    <!-- 全局消息提示 -->
    <div v-if="globalMessage" class="global-message" :class="globalMessageType">
      <span>{{ globalMessage }}</span>
      <button @click="globalMessage = ''" class="close-btn">✕</button>
    </div>
  </div>
</template>

<script>
import HistoryPage from './HistoryPage.vue';
import logo from '../../assets/logo.png';
// 假设 '@/utils/api' 包含了 api 和 authUtils 对象
// api 负责与 Spring Boot 后端交互，authUtils 处理用户认证
import api, { authUtils } from '@/utils/api'; 

export default {
  name: "QAPage",
  components: {
    HistoryPage
  },
  data() {
    return {
      logo: logo,
      question: "", // 用户输入的问题
      answer: "", // AI回答的答案
      currentQuestion: "", // 当前正在处理的问题（用户提交后清空输入框，但保留此问题以显示）
      lastQuestion: "", // 上一个提交的问题，用于重试
      historyList: [], // 历史记录列表，用于侧边栏显示
      chatHistory: [], // 对话历史，用于多轮对话传递给 RAG 服务
      userStats: null, // 用户统计信息
      isLoading: false, // 问题提交和回答加载状态
      historyLoading: false, // 历史记录加载状态
      errorMessage: "", // 错误信息
      globalMessage: "", // 全局消息提示
      globalMessageType: "info", // 全局消息类型 (info, success, warning, error)
      loadingText: "AI助手正在思考您的问题...", // 加载提示文本
      answerTime: "", // 答案生成时间
      recommendedQuestions: [], // 推荐问题列表 (现在将通过单独的接口获取)
      
      // 【主RAG服务URL】: 仍然指向您的主 RAG 服务 (例如 FastAPI 或 Spring Boot 的 SSE 端点)
      backendAskUrl: "http://localhost:8000/api/qa/ask", 
      // 【主RAG服务健康检查URL】: 指向主 RAG 服务的健康检查
      backendHealthUrl: "http://localhost:8000/health",
      // 【新加：推荐服务URL】: 指向您的 Flask app.py 的 /process_query 接口
      backendRecommendationUrl: "http://localhost:5000/process_query", 

      // Python RAG 服务期望的 SECRET_TOKEN (请确保与 main.py 中的一致)
      // !!! 警告: 生产环境不应硬编码敏感令牌，应通过环境变量或更安全的方式管理 !!!
      ragSecretToken: "my-super-secret-token-for-med-qa", 

      tokenCheckInterval: null, // Token 监控定时器
      loadingTexts: [ // 循环显示的加载提示文本
        "AI助手正在分析您的问题...",
        "正在查询医疗知识库...",
        "正在生成专业建议...",
        "即将为您呈现答案..."
      ],
      loadingTextIndex: 0, // 当前加载文本索引
      loadingInterval: null, // 加载文本切换定时器
      supportsSpeech: 'speechSynthesis' in window, // 检查浏览器是否支持语音合成
      isRefreshing: false, // 刷新连接状态
      connectionStatus: 'unknown', // 'connected', 'disconnected', 'unknown'
      connectionStatusText: '连接中...',
    };
  },
  computed: {
    // 判断是否可以提交问题
    canSubmit() {
      return this.question.trim() &&
             !this.isLoading &&
             this.question.length <= 1500 &&
             authUtils.isLoggedIn(); // 仍然需要用户登录才能提交问题
    },
    // 判断是否显示答案区域
    showAnswerArea() {
      return this.answer || this.isLoading || this.currentQuestion;
    }
  },
  mounted() {
    console.log('QAPage component mounted');
    this.initializePage(); // 页面挂载时初始化
    this.checkBackendHealth(); // 页面挂载时检查主 RAG 后端健康状态
    // 可以在这里也检查推荐服务的健康状态，如果需要
    // this.checkRecommendationServiceHealth(); 
  },
  beforeUnmount() {
    // 组件卸载前清除所有定时器
    this.clearLoadingInterval();
    this.stopTokenMonitoring();
  },
  methods: {
    // --- 认证和初始化 ---
    checkAuth() {
      try {
        // 检查用户是否登录
        if (!authUtils.isLoggedIn()) {
          this.showGlobalMessage('登录已过期，请重新登录', 'warning');
          this.$router.push('/'); // 未登录则跳转到登录页
          return false;
        }
        const userInfo = authUtils.getUserInfo();
        if (userInfo) {
          // 设置用户统计信息
          this.userStats = {
            username: userInfo.username,
            userId: userInfo.userId,
            role: userInfo.role,
            totalQuestions: 0 // 初始问题数为0，后续加载历史记录时更新
          };
          return true;
        } else {
          // 如果用户信息获取失败，清除token并跳转登录
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
        this.startTokenMonitoring(); // 开始监控 token 有效性
        await this.loadHistory(); // 加载历史记录
        await this.loadUserStats(); // 加载用户统计
        this.showGlobalMessage('页面加载完成', 'success');
      } catch (error) {
        console.error('Page initialization failed:', error);
        this.showGlobalMessage('页面初始化失败: ' + error.message, 'error');
      }
    },

    // --- 主 RAG 后端健康检查 (指向 localhost:8000) ---
    async checkBackendHealth() {
      this.isRefreshing = true;
      this.connectionStatus = 'unknown';
      this.connectionStatusText = '连接中...';
      try {
        // 健康检查URL指向 主 RAG 服务 (8000端口，路径为 /health)
        const response = await fetch(this.backendHealthUrl); 
        if (response.ok) {
          this.connectionStatus = 'connected';
          this.connectionStatusText = '主RAG服务正常';
          this.showGlobalMessage('主RAG服务连接正常', 'success');
        } else {
          this.connectionStatus = 'disconnected';
          this.connectionStatusText = '主RAG服务异常';
          this.showError(`主RAG服务异常: ${response.status} ${response.statusText}`);
          this.showGlobalMessage('主RAG服务连接异常', 'error');
        }
      } catch (error) {
        console.error('Failed to check main RAG health:', error);
        this.connectionStatus = 'disconnected';
        this.connectionStatusText = '主RAG服务离线';
        this.showError('无法连接到主RAG服务，请检查网络或联系管理员。');
        this.showGlobalMessage('无法连接到主RAG服务', 'error');
      } finally {
        this.isRefreshing = false;
      }
    },

    async refreshConnection() {
      await this.checkBackendHealth();
    },

    // --- 历史记录管理 (这些调用仍然指向 Spring Boot 后端，因为历史记录由Spring Boot管理) ---
    async loadHistory() {
      try {
        this.historyLoading = true;
        // 调用 Spring Boot 后端 API 获取历史记录
        const result = await api.question.getHistory(); 
        if (result.success) {
          this.historyList = result.data.map(item => ({
            id: item.id,
            title: item.question,
            content: item.answer,
            createTime: item.createAt 
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
        // 调用 Spring Boot 后端 API 获取用户统计信息
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

    async deleteHistory(id) { 
      try {
        // 调用 Spring Boot 后端 API 删除历史记录
        const result = await api.question.deleteHistory(id); 
        if (result.success) {
          this.historyList = this.historyList.filter(item => item.id !== id); 
          this.showGlobalMessage('删除成功', 'success');
          await this.loadUserStats(); // 更新用户统计
        } else {
          this.showError('删除失败: ' + result.message);
        }
      } catch (error) {
        console.error('Failed to delete history record:', error);
        this.showError('删除失败');
      }
    },

    async clearAllHistory() {
      // 【重要修正】: 替换 window.confirm 为自定义模态框提示
      // 由于 Canvas 环境中 alert/confirm 不可见，需要使用自定义 UI 替代
      // 这是一个示例，您需要自己实现一个模态框组件来显示确认信息
      this.showGlobalMessage('确定要清空所有历史记录吗？此操作无法撤销。', 'warning');
      // 实际项目中，这里会弹出一个自定义确认模态框，用户点击确认后才执行后续逻辑
      // 例如：this.$refs.confirmModal.show('确定要清空所有历史记录吗？').then(confirmed => { if (confirmed) { ... } });
      const confirmed = window.confirm('确定要清空所有历史记录吗？此操作无法撤销。'); // 临时使用，请务必替换
      if (!confirmed) {
        this.showGlobalMessage('已取消清空操作', 'info');
        return;
      }

      try {
        // 调用 Spring Boot 后端 API 清空历史记录
        const result = await api.question.clearHistory(); 
        if (result.success) {
          this.historyList = []; // 清空本地历史列表
          this.chatHistory = []; // 清空对话历史
          this.showGlobalMessage('历史记录已清空', 'success');
          await this.loadUserStats(); // 更新用户统计
        } else {
          this.showError('清空失败: ' + result.message);
        }
      } catch (error) {
        console.error('Failed to clear history records:', error);
        this.showError('清空失败');
      }
    },

    handleSelectHistory(item) {
      // 从历史记录中选择问题，填充到输入框和答案区
      this.question = item.title;
      this.answer = item.content;
      this.currentQuestion = item.title;
      this.answerTime = new Date(item.createTime).toLocaleString(); 
      this.clearError();
      this.recommendedQuestions = []; // 选择历史记录时清空推荐问题
    },

    // --- 问题提交和答案处理 ---
    async submitQuestion() {
      if (!this.canSubmit) return;

      if (!authUtils.isLoggedIn()) {
        this.showError('登录已过期，请重新登录');
        this.$router.push('/');
        return;
      }

      this.isLoading = true;
      this.answer = ''; // 清空旧答案
      this.errorMessage = ''; // 清空错误信息
      this.recommendedQuestions = []; // 提交新问题时清空推荐问题
      this.currentQuestion = this.question.trim(); // 保存当前问题
      this.lastQuestion = this.currentQuestion; // 保存上一个问题用于重试
      this.question = ''; // 清空输入框

      this.startLoadingAnimation(); // 启动加载动画

      try {
        // 【主RAG服务调用】: 调用主 RAG 服务获取答案 (期望 SSE 流)
        await this.callRAGService(this.currentQuestion); 
      } catch (error) {
        console.error('提交问题到主 RAG 服务失败:', error);
        this.showError('提交问题到主 RAG 服务失败: ' + error.message);
        this.answer = `抱歉，请求主RAG服务出错。请稍后重试。\n错误详情: ${error.message}`;
      } finally {
        this.isLoading = false;
        this.clearLoadingInterval(); // 停止加载动画
        // 【重要】: 即使主 RAG 服务调用失败，也尝试获取推荐问题
        // 这取决于您的业务逻辑，如果推荐服务不依赖于主 RAG 答案，可以独立调用
        if (!this.errorMessage) { // 只有在没有主 RAG 错误的情况下才尝试获取推荐问题
             await this.fetchRecommendedQuestions(this.currentQuestion);
        } else {
            // 如果主 RAG 服务失败，清空推荐问题
            this.recommendedQuestions = [];
        }
      }
    },

    // 【主RAG服务调用】: 处理主 RAG 服务的 SSE 流
    async callRAGService(question) {
      console.log('开始调用主 RAG 服务 (SSE)...');
      let sseStreamEnded = false; // 标志，指示 SSE 流是否已收到 'end' 事件

      try {
        const response = await fetch(this.backendAskUrl, { // backendAskUrl 现在指向主 RAG 服务
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.ragSecretToken}` // 发送主 RAG 服务期望的 SECRET_TOKEN
          },
          body: JSON.stringify({
            question: question,
            history: this.chatHistory.slice(-2) // 传递最近的两轮对话历史
          }),
        });

        if (!response.ok) {
          const errorText = await response.text(); // 尝试获取原始错误文本
          let errorMessage = `主RAG服务返回错误: ${response.status}`;
          try {
            // 尝试解析 JSON 错误信息
            const errorJson = JSON.parse(errorText);
            errorMessage += ` - ${errorJson.detail || errorJson.message || errorText}`;
          } catch (e) {
            // 如果不是 JSON，直接使用原始文本
            errorMessage += ` - ${errorText}`;
          }
          throw new Error(errorMessage);
        }

        // SSE流处理
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = ''; // 接收到的数据缓冲区
        let answerText = ''; // 累积的答案文本
        let isAnswerStarted = false; // 标志，指示是否已开始接收答案 token

        console.log('开始读取主 RAG SSE流...');
        while (true) {
          const { done, value } = await reader.read();
          if (done) {
            console.log('主 RAG SSE流读取完成。');
            break; // 如果流已结束，跳出外层循环
          }
          buffer += decoder.decode(value, { stream: true }); // 将新数据添加到缓冲区

          let lineEndIndex;
          let shouldBreakOuterLoop = false; // 新增标志，用于在收到 'end' 事件时跳出外层循环
          // 循环处理缓冲区中的所有完整行
          while ((lineEndIndex = buffer.indexOf('\n')) !== -1) {
            const fullLine = buffer.substring(0, lineEndIndex).trim(); // 获取完整行并去除首尾空格
            buffer = buffer.substring(lineEndIndex + 1); // 从缓冲区中移除已处理的行

            if (fullLine === '') { // 空行，继续处理下一行
              continue;
            }

            if (fullLine.startsWith('event:')) {
              const eventType = fullLine.substring(6).trim();
              if (eventType === 'end') {
                console.log('Received SSE end event. Setting flag to terminate stream processing.');
                sseStreamEnded = true; // 设置 SSE 流结束标志
                shouldBreakOuterLoop = true; // 设置跳出外层循环的标志
                break; // 跳出内层循环
              } else if (eventType === 'retrieval_complete') {
                console.log('Received retrieval_complete event.');
                // 如果需要，可以在这里处理与检索完成相关的数据
              }
            } else if (fullLine.startsWith('data:')) {
              let jsonString = fullLine.substring(5).trim(); // 提取潜在的 JSON 字符串

              // 增加防御性检查：如果 jsonString 仍然以 "data:" 开头，再次移除
              if (jsonString.startsWith('data:')) {
                console.warn('Detected duplicated "data:" prefix, removing again:', jsonString);
                jsonString = jsonString.substring(5).trim();
              }

              // 忽略特殊或空的数据块
              if (jsonString === '[DONE]' || jsonString === '{}' || jsonString === '') {
                console.log('Skipping empty or DONE data chunk:', jsonString);
                continue;
              }
              
              // 确保有内容可以解析
              if (jsonString.length === 0) {
                  console.warn("Empty JSON string after stripping 'data:' prefix:", fullLine);
                  continue;
              }

              console.log('Attempting to parse SSE JSON string:', jsonString);

              try {
                const obj = JSON.parse(jsonString);
                // 【注意】: 这里只处理主 RAG 服务的答案 token。推荐问题将通过单独的接口获取。
                if (obj.token !== undefined) {
                  isAnswerStarted = true;
                  answerText += obj.token; // 累积答案 token
                  this.answer = answerText; // 更新界面显示
                } 
                // 移除此处对 recommendedQuestions 的处理，因为现在由 fetchRecommendedQuestions 处理
                // else if (obj.recommendedQuestions !== undefined) {
                //   this.recommendedQuestions = obj.recommendedQuestions; 
                // }
              } catch (e) {
                console.warn("Failed to parse SSE data chunk as JSON:", jsonString, e);
                // 如果解析失败，不追加到答案，只记录警告
              }
            } else {
              // 记录非 event: 也非 data: 的行，这可能是异常情况
              console.warn("Unexpected SSE line format (neither event: nor data:):", fullLine);
            }
          }
          // 【修正点】: 如果内层循环因为 'event: end' 而中断，则也跳出外层循环
          if (shouldBreakOuterLoop) { 
              break;
          }
        }
        this.answer = answerText; // 确保最终答案完整显示
        // 如果没有收到任何答案 token，给出默认提示
        if (!isAnswerStarted && answerText === '') {
          this.answer = '未收到AI回答内容，可能主RAG服务异常或知识库无匹配。';
        }
        this.onAnswerComplete(); // 答案处理完成后的操作
      } catch (error) {
        console.error('Failed to submit question to main RAG service:', error);
        throw error; // 重新抛出错误，以便 submitQuestion 的 finally 块捕获
      }
    },

    // 【新加方法】: 调用 Flask app.py 获取推荐问题
    async fetchRecommendedQuestions(queryText) {
      console.log('开始调用推荐服务获取推荐问题...');
      try {
        const response = await fetch(this.backendRecommendationUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            // 注意：您的 app.py 的 /process_query 接口目前没有认证，所以这里不需要 Authorization header
            // 如果未来需要，请在此处添加
          },
          body: JSON.stringify({
            queryText: queryText, // 匹配 app.py 期望的字段
            // sessionId: 'some_session_id', // 如果 app.py 需要，可以添加
            // userId: 'some_user_id', // 如果 app.py 需要，可以添加
          }),
        });

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ message: response.statusText }));
          throw new Error(`推荐服务返回错误: ${response.status} - ${errorData.message || response.statusText}`);
        }

        const data = await response.json();
        if (data.recommendedQuestions && Array.isArray(data.recommendedQuestions)) {
          // 提取 questionText 并更新 recommendedQuestions 数组
          this.recommendedQuestions = data.recommendedQuestions.map(q => q.questionText);
          console.log('成功获取推荐问题:', this.recommendedQuestions);
        } else {
          console.warn('推荐服务响应中未找到 recommendedQuestions 数组或格式不正确:', data);
          this.recommendedQuestions = [];
        }
      } catch (error) {
        console.error('获取推荐问题失败:', error);
        this.showGlobalMessage('获取推荐问题失败: ' + error.message, 'error');
        this.recommendedQuestions = []; // 出错时清空推荐问题
      }
    },

    onAnswerComplete() {
      this.answerTime = new Date().toLocaleString();
      // 将当前问答添加到历史列表（用于侧边栏显示）
      this.historyList.push({
        title: this.currentQuestion,
        content: this.answer,
        createTime: this.answerTime
      });

      // 将当前问答添加到 chatHistory（用于多轮对话传递给主RAG服务）
      this.chatHistory.push({
        "question": this.currentQuestion,
        "answer": this.answer
      });

      // 调用Spring Boot保存历史记录
      this.saveAnswerToBackend(this.currentQuestion, this.answer);

      this.loadUserStats(); // 更新用户统计
      this.showGlobalMessage('问答完成', 'success');
      
      // 【重要】: 在主 RAG 答案完成后，调用推荐服务获取推荐问题
      // 这里的 this.currentQuestion 包含了用户提交的原始问题
      this.fetchRecommendedQuestions(this.currentQuestion);
    },

    // 调用 Spring Boot 保存问答记录
    async saveAnswerToBackend(question, answer) {
        try {
            const token = authUtils.getToken();
            if (!token) {
                console.warn('未登录，无法保存问答记录到后端。');
                return;
            }
            const response = await fetch('http://localhost:8080/api/qa/history', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    question: question,
                    answer: answer // 答案也一起保存
                })
            });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: response.statusText }));
                console.error('保存问答记录到后端失败:', errorData.message || response.statusText);
            } else {
                console.log('问答记录已保存到后端。');
                // 重新加载历史记录以获取最新ID和更新统计
                await this.loadHistory(); 
            }
        } catch (error) {
            console.error('保存问答记录到后端时发生网络错误:', error);
        }
    },

    // --- UI/动画辅助方法 ---
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

    // --- Token 监控 ---
    startTokenMonitoring() {
      this.tokenCheckInterval = setInterval(() => {
        if (!authUtils.isLoggedIn()) {
          this.showGlobalMessage('登录已过期，即将跳转到登录页', 'warning');
          setTimeout(() => {
            this.$router.push('/');
          }, 2000);
          this.stopTokenMonitoring();
        }
      }, 60000); // 每分钟检查一次
    },

    stopTokenMonitoring() {
      if (this.tokenCheckInterval) {
        clearInterval(this.tokenCheckInterval);
        this.tokenCheckInterval = null;
      }
    },

    // --- 实用工具方法 ---
    copyAnswer() {
      if (this.answer) {
        // 使用 document.execCommand('copy') 复制文本到剪贴板
        // 在某些现代浏览器中可能已被弃用，但 iframe 环境下可能仍是唯一可用方式
        const el = document.createElement('textarea');
        el.value = this.answer;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
        this.showGlobalMessage('已复制到剪贴板', 'success');
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
      this.chatHistory = []; // 清空对话历史，开始新的对话
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

    // 加载推荐问题并提交
    loadRecommendedQuestion(recQuestion) {
      this.question = recQuestion;
      this.submitQuestion(); 
    },

    showError(message) {
      this.errorMessage = message;
      setTimeout(() => {
        if (this.errorMessage === message) {
          this.errorMessage = "";
        }
      }, 10000); // 10秒后自动消失
    },

    showGlobalMessage(message, type = 'info') {
      this.globalMessage = message;
      this.globalMessageType = type;
      setTimeout(() => {
        if (this.globalMessage === message) {
          this.globalMessage = "";
        }
      }, 3000); // 3秒后自动消失
    },

    async logout() {
      try {
        this.stopTokenMonitoring(); // 停止 token 监控
        try {
          // 调用 Spring Boot 的注销接口
          await api.auth.logout(); 
        } catch (logoutError) {
          console.warn('Server logout failed, but local state cleared:', logoutError);
        }
        this.showGlobalMessage('已退出登录', 'success');
        setTimeout(() => {
          this.$router.push('/'); // 跳转到登录页
        }, 1000);
      } catch (error) {
        console.error('Failed to log out:', error);
        authUtils.clearToken(); // 确保本地 token 清除
        this.$router.push('/');
      }
    }
  },

  // --- 路由守卫 ---
  beforeRouteEnter(to, from, next) {
    // 进入路由前检查是否登录
    if (!authUtils.isLoggedIn()) {
      console.log('QAPage: User not logged in, redirecting to homepage');
      next('/');
    } else {
      console.log('QAPage: User logged in, access granted');
      next();
    }
  },

  beforeRouteLeave(to, from, next) {
    // 离开路由前清除定时器
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
  flex-grow: 1; /* 答案卡片可以增长 */
  display: flex;
  flex-direction: column;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f3f5;
}

.answer-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-avatar {
  font-size: 24px;
}

.typing-indicator {
  display: inline-flex;
  align-items: center;
  margin-left: 10px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background-color: #667eea;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -1.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: -1.0s;
}

.answer-actions .action-btn {
  background: #007bff;
  color: white;
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 13px;
}

.answer-actions .action-btn:hover {
  background: #0056b3;
}

.answer-content {
  flex-grow: 1; /* 答案内容区可以增长 */
  display: flex;
  flex-direction: column;
  justify-content: center; /* 垂直居中加载动画 */
  align-items: center; /* 水平居中加载动画 */
  min-height: 100px; /* 确保有最小高度显示加载状态 */
}

.answer-text {
  width: 100%;
  color: #343a40;
  font-size: 16px;
  line-height: 1.6;
  white-space: pre-wrap; /* 保留换行符和空格 */
  word-break: break-word; /* 单词过长时自动换行 */
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.answer-body {
  margin-bottom: 16px;
}

.answer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  font-size: 12px;
  color: #6c757d;
  margin-top: auto; /* 推到底部 */
  padding-top: 8px;
  border-top: 1px dashed #e9ecef;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.loading-animation {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
}

.loading-animation .dot {
  width: 12px;
  height: 12px;
  background-color: #667eea;
  border-radius: 50%;
  animation: dot-bounce 1.2s infinite ease-in-out;
}

.loading-animation .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-animation .dot:nth-child(3) {
  animation-delay: 0.4s;
}

.loading-text {
  font-size: 16px;
  color: #5a6268;
}

/* Recommended Questions */
.recommended-questions {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e9ecef;
}

.recommended-questions h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 18px;
}

.recommendation-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.rec-btn {
  background: #f0f8ff;
  color: #007bff;
  border: 1px solid #a0cfff;
  padding: 10px 15px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.rec-btn:hover {
  background: #e0f0ff;
  border-color: #007bff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,123,255,0.1);
}

/* Error Card */
.error-card {
  background: #ffebeb;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #ff9999;
}

.error-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.error-header h4 {
  margin: 0;
  color: #dc3545;
  font-size: 18px;
}

.error-card .close-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: #dc3545;
  cursor: pointer;
}

.error-text {
  color: #721c24;
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.error-actions {
  display: flex;
  gap: 10px;
}

.error-actions .retry-btn {
  background: #007bff;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.error-actions .retry-btn:hover {
  background: #0056b3;
}

.error-actions .dismiss-btn {
  background: #6c757d;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.error-actions .dismiss-btn:hover {
  background: #5a6268;
}

/* Quick Actions */
.quick-actions {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e9ecef;
}

.quick-actions h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 18px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.quick-btn {
  background: linear-gradient(45deg, #a0cfff, #cceeff);
  color: #007bff;
  border: 1px solid #007bff;
  padding: 15px 20px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.quick-btn:hover {
  background: linear-gradient(45deg, #007bff, #0056b3);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0,123,255,0.2);
}

/* Global Message */
.global-message {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 20px;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 500;
  z-index: 1000;
  animation: fade-in-out 3.5s forwards;
}

.global-message.info {
  background-color: #e7f3ff;
  color: #007bff;
  border: 1px solid #a0cfff;
}

.global-message.success {
  background-color: #d4edda;
  color: #28a745;
  border: 1px solid #28a745;
}

.global-message.warning {
  background-color: #fff3cd;
  color: #ffc107;
  border: 1px solid #ffc107;
}

.global-message.error {
  background-color: #f8d7da;
  color: #dc3545;
  border: 1px solid #dc3545;
}

.global-message .close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: inherit;
  cursor: pointer;
  opacity: 0.7;
}

.global-message .close-btn:hover {
  opacity: 1;
}

/* Keyframe Animations */
@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes pulse-glow {
  0% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7); }
  70% { box-shadow: 0 0 0 10px rgba(102, 126, 234, 0); }
  100% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0); }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
}

@keyframes fade-in-out {
  0% { opacity: 0; transform: translateX(-50%) translateY(20px); }
  10% { opacity: 1; transform: translateX(-50%) translateY(0); }
  90% { opacity: 1; transform: translateX(-50%) translateY(0); }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .qa-container {
    flex-direction: column;
    height: 95%;
    width: 98%;
  }

  .qa-main {
    padding: 15px;
  }

  .title-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-actions {
    flex-wrap: wrap;
    justify-content: flex-start;
    width: 100%;
  }

  .qa-card, .answer-card, .recommended-questions, .error-card, .quick-actions {
    padding: 15px;
  }

  .input-footer {
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
  }

  .input-info {
    width: 100%;
    justify-content: space-between;
  }

  .submit-section {
    width: 100%;
    justify-content: flex-end;
  }

  .action-grid {
    grid-template-columns: 1fr; /* On small screens, stack quick actions */
  }
}

@media (max-width: 480px) {
  .title-text {
    font-size: 20px;
  }

  .logo {
    width: 40px;
    height: 40px;
  }

  .action-btn, .logout-btn, .clear-btn, .submit-btn, .rec-btn, .quick-btn {
    padding: 8px 12px;
    font-size: 13px;
  }

  .global-message {
    width: 90%;
    font-size: 13px;
    padding: 10px 15px;
  }
} /* 响应式媒体查询结束 */
</style>
