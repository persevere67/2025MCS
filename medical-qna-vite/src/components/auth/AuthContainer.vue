<template>
  <div class="auth-container">
    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="decoration-item decoration-1"></div>
      <div class="decoration-item decoration-2"></div>
      <div class="decoration-item decoration-3"></div>
      <div class="decoration-item decoration-4"></div>
    </div>

    <!-- 主要内容区域 -->
    <div class="auth-content">
      <!-- 左侧信息面板 -->
      <div class="info-panel">
        <div class="brand">
          <div class="brand-icon">🏥</div>
          <h1>医疗问答系统</h1>
          <p class="brand-subtitle">智能医疗咨询平台</p>
        </div>

        <div class="features">
          <div class="feature-item" v-for="feature in features" :key="feature.id">
            <div class="feature-icon">{{ feature.icon }}</div>
            <div class="feature-content">
              <h4>{{ feature.title }}</h4>
              <p>{{ feature.description }}</p>
            </div>
          </div>
        </div>

        <div class="testimonial">
          <div class="quote">"这个平台帮助我快速获得了专业的医疗建议，非常实用！"</div>
          <div class="author">— 郭先生，普通用户</div>
        </div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="form-panel">
        <div class="form-container">
          <!-- 切换标签 -->
          <div class="auth-tabs">
            <button 
              :class="['tab-btn', { active: currentView === 'login' }]"
              @click="switchView('login')"
            >
              登录
            </button>
            <button 
              :class="['tab-btn', { active: currentView === 'register' }]"
              @click="switchView('register')"
            >
              注册
            </button>
            <div class="tab-indicator" :style="tabIndicatorStyle"></div>
          </div>

          <!-- 表单切换 -->
          <transition :name="slideDirection" mode="out-in">
            <LoginForm 
              v-if="currentView === 'login'"
              key="login"
              @switch-to-register="switchView('register')"
              @login-success="handleLoginSuccess"
            />
            <RegisterForm 
              v-else
              key="register"
              @switch-to-login="switchView('login')"
              @register-success="handleRegisterSuccess"
            />
          </transition>
        </div>

        <!-- 安全提示 -->
        <div class="security-notice">
          <div class="security-icon">🔒</div>
          <span>您的数据受到SSL加密保护</span>
        </div>
      </div>
    </div>

    <!-- 版权信息 -->
    <div class="footer">
      <div class="footer-content">
        <span>&copy; 2024 医疗问答系统. 保留所有权利.</span>
        <div class="footer-links">
          <a href="#" @click.prevent="showPrivacy">隐私政策</a>
          <a href="#" @click.prevent="showTerms">服务条款</a>
          <a href="#" @click.prevent="showHelp">帮助</a>
        </div>
      </div>
    </div>

    <!-- 模态框 -->
    <transition name="modal">
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>{{ modalTitle }}</h3>
            <button @click="closeModal" class="close-btn">×</button>
          </div>
          <div class="modal-body">
            <p>{{ modalContent }}</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'
import api from '@/utils/api'

export default {
  name: 'AuthContainer',
  components: {
    LoginForm,
    RegisterForm
  },
  
  data() {
    return {
      currentView: 'login',
      slideDirection: 'slide-left',
      showModal: false,
      modalTitle: '',
      modalContent: '',
      features: [
        {
          id: 1,
          icon: '🤖',
          title: 'AI智能诊断',
          description: '基于大数据和机器学习的智能医疗咨询'
        },
        {
          id: 2,
          icon: '🔒',
          title: '隐私保护',
          description: '严格保护用户隐私，医疗数据安全可靠'
        },
        {
          id: 3,
          icon: '⚡',
          title: '快速响应',
          description: '24小时在线服务，快速获得专业建议'
        }
      ]
    }
  },

  computed: {
    tabIndicatorStyle() {
      return {
        transform: this.currentView === 'login' ? 'translateX(0)' : 'translateX(100%)'
      }
    }
  },

  methods: {
    switchView(view) {
      const previousView = this.currentView
      this.currentView = view
      
      // 设置滑动方向
      if (previousView === 'login' && view === 'register') {
        this.slideDirection = 'slide-left'
      } else if (previousView === 'register' && view === 'login') {
        this.slideDirection = 'slide-right'
      }
    },

    handleLoginSuccess(userData) {
      console.log('登录成功:', userData)
      // 这里可以跳转到主页面或者触发父组件的事件
      this.$emit('login-success', userData)
    },

    handleRegisterSuccess(userData) {
      console.log('注册成功:', userData)
      // 注册成功后通常切换到登录页面
      this.switchView('login')
    },

    showPrivacy() {
      this.modalTitle = '隐私政策'
      this.modalContent = '我们致力于保护您的个人隐私。我们收集和使用您的信息仅用于提供更好的医疗咨询服务。您的医疗数据将严格保密，不会向第三方泄露。'
      this.showModal = true
    },

    showTerms() {
      this.modalTitle = '服务条款'
      this.modalContent = '欢迎使用我们的医疗问答平台。使用本服务即表示您同意遵守我们的服务条款。请注意，本平台提供的信息仅供参考，不能替代专业医疗诊断。'
      this.showModal = true
    },

    showHelp() {
      this.modalTitle = '帮助'
      this.modalContent = '如果您在使用过程中遇到任何问题，请联系我们的客服团队。客服热线：400-123-4567，工作时间：周一至周日 9:00-18:00。'
      this.showModal = true
    },

    closeModal() {
      this.showModal = false
    }
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.decoration-item {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.decoration-1 {
  width: 100px;
  height: 100px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.decoration-3 {
  width: 80px;
  height: 80px;
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}


@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

.auth-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

.info-panel {
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
}

.brand {
  margin-bottom: 60px;
}

.brand-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.brand h1 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 8px;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 18px;
  opacity: 0.9;
  font-weight: 300;
}

.features {
  margin-bottom: 40px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.feature-icon {
  font-size: 36px;
  flex-shrink: 0;
  margin-top: 4px;
}

.feature-content h4 {
  font-size: 30px;
  font-weight: 600;
  margin-bottom: 8px;
}

.feature-content p {
  font-size: 18px;
  opacity: 0.9;
  line-height: 1.5;
}

.testimonial {
  background: rgba(255, 255, 255, 0.1);
  padding: 24px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.quote {
  font-size: 16px;
  font-style: italic;
  margin-bottom: 12px;
  line-height: 1.6;
}

.author {
  font-size: 14px;
  opacity: 0.8;
  text-align: right;
}

.form-panel {
  display: flex;
  flex-direction: column;
  background-image: url('@/assets/bj6.jpg');
  background-size: cover;
  background-position: fixed;
  backdrop-filter: blur(10px);
  padding: 40px;
  position: relative;
}

.form-container {
  max-width: 500px;
  margin: 0 auto;
  width: 100%;
}

.auth-tabs {
  display: flex;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 32px;
  position: relative;
}

.tab-btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
}

.tab-btn.active {
  color: #667eea;
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
  z-index: 1;
}

.security-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 12px;
  color: #ffffff;
}

.security-icon {
  font-size: 14px;
}

.footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.1);
  padding: 20px 40px;
  z-index: 1;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.footer-links {
  display: flex;
  gap: 24px;
}

.footer-links a {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-links a:hover {
  color: white;
}

/* 切换动画 */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.4s ease;
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
  border-bottom: 1px solid #e1e5e9;
  margin-bottom: 24px;
}

.modal-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
}

.modal-body {
  padding: 0 24px 24px;
  line-height: 1.6;
  color: #666;
}

.modal-enter-active, .modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from, .modal-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .auth-content {
    grid-template-columns: 1fr;
  }
  
  .info-panel {
    display: none;
  }
  
  .form-panel {
    padding: 20px;
  }
  
  .brand h1 {
    font-size: 28px;
  }
  
  .footer-content {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
  
  .footer-links {
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .form-panel {
    padding: 16px;
  }
  
  .auth-tabs {
    margin-bottom: 24px;
  }
  
  .tab-btn {
    padding: 10px 16px;
    font-size: 14px;
  }
}
</style>