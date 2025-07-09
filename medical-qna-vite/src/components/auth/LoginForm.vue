<template>
  <div class="login-container">
    <form class="login-form" @submit.prevent="handleLogin">
      <div class="form-header">
        <div class="icon">🏥</div>
        <h3>欢迎回来</h3>
        <p>登录您的医疗问答账号</p>
      </div>

      <!-- 错误/成功消息 -->
      <transition name="fade">
        <div v-if="message.text" :class="['message', message.type]">
          <span class="message-icon">
            {{ message.type === 'error' ? '❌' : '✅' }}
          </span>
          {{ message.text }}
        </div>
      </transition>

      <!-- 用户名字段 -->
      <div class="form-group">
        <label for="username">
          <span class="label-text">用户名</span>
          <span class="required">*</span>
        </label>
        <div class="input-wrapper">
          <input 
            type="text" 
            id="username" 
            v-model="formData.username" 
            placeholder="请输入用户名"
            :class="{ 'error': errors.username, 'success': isValidField('username') }"
            :disabled="loading"
            @blur="validateField('username')"
            @input="clearFieldError('username')"
            @keyup.enter="handleLogin"
          />
          <span class="input-icon">👤</span>
        </div>
        <transition name="slide">
          <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
        </transition>
      </div>

      <!-- 密码字段 -->
      <div class="form-group">
        <label for="password">
          <span class="label-text">密码</span>
          <span class="required">*</span>
        </label>
        <div class="input-wrapper">
          <input 
            :type="showPassword ? 'text' : 'password'" 
            id="password" 
            v-model="formData.password" 
            placeholder="请输入密码"
            :class="{ 'error': errors.password, 'success': isValidField('password') }"
            :disabled="loading"
            @blur="validateField('password')"
            @input="clearFieldError('password')"
            @keyup.enter="handleLogin"
          />
          <button 
            type="button" 
            class="password-toggle"
            @click="showPassword = !showPassword"
            :disabled="loading"
            tabindex="-1"
          >
            {{ showPassword ? '🙈' : '👁️' }}
          </button>
        </div>
        <transition name="slide">
          <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
        </transition>
      </div>

      <!-- 记住我和忘记密码 -->
      <div class="form-options">
        <label class="checkbox-label">
          <input 
            type="checkbox" 
            v-model="formData.rememberMe" 
            :disabled="loading"
          />
          <span class="checkmark"></span>
          <span class="checkbox-text">记住我</span>
        </label>
        
        <a href="#" @click.prevent="handleForgotPassword" class="forgot-link">
          忘记密码？
        </a>
      </div>

      <!-- 提交按钮 -->
      <button 
        type="submit" 
        class="submit-btn"
        :disabled="loading || !isFormValid"
        :class="{ 'loading': loading }"
      >
        <span v-if="loading" class="spinner"></span>
        <span>{{ loading ? '登录中...' : '登录' }}</span>
      </button>

      <!-- 快速登录选项 -->
      <div class="divider">
        <span>或者使用</span>
      </div>

      <div class="quick-login">
        <button 
          type="button" 
          class="quick-btn guest-btn"
          @click="quickLogin('guest')"
          :disabled="loading"
        >
          <span class="quick-icon">👤</span>
          游客模式
        </button>
      </div>

      <!-- 注册链接 -->
      <div class="form-footer">
        <span>还没有账号？</span>
        <a href="#" @click.prevent="$emit('switch-to-register')" class="switch-link">
          立即注册
        </a>
      </div>
    </form>
  </div>
</template>

<script>
import api, { authUtils } from '@/utils/api'

export default {
  name: 'LoginForm',
  emits: ['switch-to-register', 'login-success'],
  
  data() {
    return {
      formData: {
        username: '',
        password: '',
        rememberMe: false
      },
      errors: {},
      loading: false,
      showPassword: false,
      message: {
        text: '',
        type: 'error'
      }
    }
  },

  computed: {
    isFormValid() {
      return this.formData.username.length >= 3 &&
             this.formData.password.length >= 6 &&
             Object.keys(this.errors).length === 0
    }
  },

  mounted() {
    this.loadRememberedUser()
    this.checkTokenStatus()
  },

  methods: {
    // 检查本地 Token 状态
    checkTokenStatus() {
      try {
        console.log('检查本地 Token 状态...')
        
        if (authUtils.isLoggedIn()) {
          const token = authUtils.getToken()
          console.log('发现本地 Token:', token ? 'exists' : 'missing')
          
          // 检查 Token 是否过期
          if (authUtils.isTokenExpired()) {
            console.log('Token 已过期，清除本地状态')
            authUtils.clearToken()
          } else {
            // 根据用户角色跳转到不同页面
            const userInfo = authUtils.getUserInfo()
            if (userInfo && userInfo.role === 'ADMIN') {
              console.log('检测到管理员角色，跳转到管理后台')
              this.$router.push('/admin')
            } else {
              console.log('Token 有效，直接跳转到问答页面')
              this.$router.push('/qna')
            }
          }
        } else {
          console.log('无 Token，停留在登录页面')
        }
      } catch (error) {
        console.error('检查 Token 状态失败:', error)
        authUtils.clearToken()
      }
    },

    showMessage(text, type = 'error') {
      this.message = { text, type }
      setTimeout(() => {
        this.message.text = ''
      }, 5000)
    },

    validateField(fieldName) {
      const value = this.formData[fieldName]
      
      switch (fieldName) {
        case 'username':
          if (!value) {
            this.errors.username = '用户名不能为空'
          } else if (value.length < 3) {
            this.errors.username = '用户名至少3个字符'
          } else {
            delete this.errors.username
          }
          break

        case 'password':
          if (!value) {
            this.errors.password = '密码不能为空'
          } else if (value.length < 6) {
            this.errors.password = '密码至少6个字符'
          } else {
            delete this.errors.password
          }
          break
      }
    },

    clearFieldError(fieldName) {
      if (this.errors[fieldName]) {
        delete this.errors[fieldName]
      }
    },

    isValidField(fieldName) {
      const value = this.formData[fieldName]
      return value && !this.errors[fieldName]
    },

    validateAllFields() {
      ['username', 'password'].forEach(field => {
        this.validateField(field)
      })
    },

    async handleLogin() {
      console.log('开始基于 Token 的登录流程...')
      this.validateAllFields()
      
      if (!this.isFormValid) {
        this.showMessage('请检查用户名和密码')
        return
      }

      this.loading = true
      this.message.text = ''

      try {
        console.log('发送登录请求...', { username: this.formData.username })
        
        const result = await api.auth.login({
          username: this.formData.username,
          password: this.formData.password
        })

        console.log('登录 API 响应:', result)

        if (result.success && result.data.token) {
          console.log('登录成功！Token 已保存到本地存储')
          this.showMessage('登录成功！', 'success')
          
          // 保存用户信息到本地存储
          if (result.data.user) {
            authUtils.saveUserInfo(result.data.user)
          }
          
          // 处理记住我功能
          if (this.formData.rememberMe) {
            this.saveRememberedUser()
          } else {
            this.clearRememberedUser()
          }
          
          // 发送登录成功事件
          this.$emit('login-success', result.data)
          
          // 基于 Token 的直接跳转（无需服务器验证）
          console.log('Token 认证成功，执行页面跳转...')
          // 根据用户角色跳转

          const role = result.data.user?.role
        if (role === 'ADMIN') {
          this.$router.replace({ path: '/admin' })
        } else {
          this.$router.replace({ path: '/qna' })
        }
          
          // 短暂延迟确保状态更新
          setTimeout(() => {
            this.performNavigation(result.data.user?.role)
          }, 500)
          
        } else {
          throw new Error(result.message || '登录失败：未收到有效 Token')
        }

      } catch (error) {
        console.error('登录过程出错:', error)
        let errorMessage = '登录失败'
        
        if (error.message) {
          errorMessage = error.message
        } else if (error.response?.data?.message) {
          errorMessage = error.response.data.message
        }
        
        this.showMessage(errorMessage)
      } finally {
        this.loading = false
      }
    },

    // 执行页面跳转（根据角色）
    performNavigation(userRole) {
      try {
        // 根据用户角色决定跳转路径
        let targetPath = '/qna'; // 默认路径
        
        if (userRole === 'ADMIN') {
          targetPath = '/admin';
          console.log('检测到管理员角色，跳转到管理后台');
        }
        
        console.log(`开始执行页面跳转到 ${targetPath}`);
        console.log('当前路由:', this.$route.path);
        console.log('Token 状态:', authUtils.isLoggedIn());
        
        // 使用多种跳转策略
        const navigationStrategies = [
          // 策略1: Vue Router replace
          () => {
            console.log('尝试 Vue Router replace');
            return this.$router.replace({ path: targetPath });
          },
          
          // 策略2: Vue Router push  
          () => {
            console.log('尝试 Vue Router push');
            return this.$router.push({ path: targetPath });
          },
          
          // 策略3: 直接修改 URL
          () => {
            console.log('尝试直接修改 URL');
            const newUrl = `${window.location.origin}${window.location.pathname}#${targetPath}`;
            window.location.href = newUrl;
            return Promise.resolve();
          }
        ];
        
        // 依次尝试跳转策略
        const tryNavigation = async (index = 0) => {
          if (index >= navigationStrategies.length) {
            throw new Error('所有跳转策略都失败了');
          }
          
          try {
            await navigationStrategies[index]();
            console.log(`跳转策略 ${index + 1} 成功`);
          } catch (error) {
            console.log(`跳转策略 ${index + 1} 失败:`, error);
            await tryNavigation(index + 1);
          }
        };
        
        tryNavigation();
        
      } catch (error) {
        console.error('页面跳转完全失败:', error);
        this.showMessage('页面跳转失败，请手动刷新页面');
      }
    },

    async quickLogin(type) {
      let credentials = null;
      
      if (type === 'guest') {
        credentials = { username: 'guest', password: 'guest123' };
        this.showMessage('正在以游客身份登录...', 'success');
      } 
      if (credentials) {
        this.formData.username = credentials.username;
        this.formData.password = credentials.password;
        
        setTimeout(() => {
          this.handleLogin();
        }, 500);
      }
    },

    handleForgotPassword() {
      this.showMessage('忘记密码功能正在开发中，请联系管理员重置密码', 'error');
    },

    saveRememberedUser() {
      try {
        localStorage.setItem('rememberedUser', JSON.stringify({
          username: this.formData.username,
          timestamp: Date.now()
        }));
        console.log('已保存记住的用户信息');
      } catch (error) {
        console.warn('无法保存记住的用户信息:', error);
      }
    },

    loadRememberedUser() {
      try {
        const remembered = localStorage.getItem('rememberedUser');
        if (remembered) {
          const data = JSON.parse(remembered);
          // 7天有效期
          if (Date.now() - data.timestamp < 7 * 24 * 60 * 60 * 1000) {
            this.formData.username = data.username;
            this.formData.rememberMe = true;
            console.log('已加载记住的用户信息:', data.username);
          } else {
            console.log('记住的用户信息已过期');
            this.clearRememberedUser();
          }
        }
      } catch (error) {
        console.warn('无法加载记住的用户信息:', error);
      }
    },

    clearRememberedUser() {
      try {
        localStorage.removeItem('rememberedUser');
        console.log('已清除记住的用户信息');
      } catch (error) {
        console.warn('无法清除记住的用户信息:', error);
      }
    }
  }
}
</script>
<style scoped>
.login-container {
  width: 100%;
}

.login-form {
  background: transparent;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
  border: none;
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.form-header h3 {
  font-size: 24px;
  font-weight: 600;
  color: #fffefe;
  margin-bottom: 8px;
}

.form-header p {
  color: #ffffff;
  font-size: 14px;
}

.message {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.message.error {
  background: #fee;
  color: #d63384;
  border-left: 4px solid #d63384;
}

.message.success {
  background: #d4edda;
  color: #155724;
  border-left: 4px solid #28a745;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #ffffff;
}

.required {
  color: #e74c3c;
  margin-left: 2px;
}

.input-wrapper {
  position: relative;
}

.input-wrapper input {
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 2px solid #e1e5e9;
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: #fff;
  box-sizing: border-box;
}

.input-wrapper input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-wrapper input.error {
  border-color: #e74c3c;
}

.input-wrapper input.success {
  border-color: #27ae60;
}

.input-wrapper input:disabled {
  background: #f8f9fa;
  color: #6c757d;
  cursor: not-allowed;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: #999;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #999;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.password-toggle:hover {
  background: #f0f0f0;
}

.password-toggle:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.error-text {
  color: #e74c3c;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  display: none;
}

.checkmark {
  width: 16px;
  height: 16px;
  border: 2px solid #e1e5e9;
  border-radius: 3px;
  position: relative;
  transition: all 0.3s ease;
}

.checkbox-label input:checked + .checkmark {
  background: #667eea;
  border-color: #667eea;
}

.checkbox-label input:checked + .checkmark::after {
  content: '✓';
  position: absolute;
  color: white;
  font-size: 10px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.checkbox-text {
  font-size: 14px;
  color: #666;
}

.forgot-link {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
}

.forgot-link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 24px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.submit-btn.loading {
  pointer-events: none;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  margin-right: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.divider {
  text-align: center;
  position: relative;
  margin-bottom: 20px;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e1e5e9;
}

.divider span {
  background: rgba(255, 255, 255, 0.95);
  padding: 0 16px;
  color: #999;
  font-size: 12px;
}

.quick-login {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.quick-btn {
  padding: 12px;
  border: 2px solid #e1e5e9;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.quick-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
}

.quick-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quick-icon {
  font-size: 16px;
}

.guest-btn:hover:not(:disabled) {
  border-color: #95a5a6;
  color: #95a5a6;
}

.form-footer {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #e1e5e9;
  color: #ffffff;
  font-size: 14px;
}

.switch-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  margin-left: 8px;
}

.switch-link:hover {
  text-decoration: underline;
}

/* 动画效果 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .form-options {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .quick-login {
    grid-template-columns: 1fr;
  }
}
</style>