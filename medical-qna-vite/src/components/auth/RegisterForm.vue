<template>
  <div class="register-container">
    <form class="register-form" @submit.prevent="handleRegister">
      <div class="form-header">
        <div class="icon">👤</div>
        <h3>创建账号</h3>
        <p>加入我们的医疗问答平台</p>
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
          />
          <span class="input-icon">👤</span>
        </div>
        <transition name="slide">
          <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
        </transition>
      </div>

      <!-- 邮箱字段 -->
      <div class="form-group">
        <label for="email">
          <span class="label-text">邮箱</span>
          <span class="optional">(可选)</span>
        </label>
        <div class="input-wrapper">
          <input 
            type="email" 
            id="email" 
            v-model="formData.email" 
            placeholder="请输入邮箱地址"
            :class="{ 'error': errors.email, 'success': isValidField('email') }"
            :disabled="loading"
            @blur="validateField('email')"
            @input="clearFieldError('email')"
          />
          <span class="input-icon">📧</span>
        </div>
        <transition name="slide">
          <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
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
          />
          <button 
            type="button" 
            class="password-toggle"
            @click="showPassword = !showPassword"
            :disabled="loading"
          >
            {{ showPassword ? '🙈' : '👁️' }}
          </button>
        </div>
        <transition name="slide">
          <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
        </transition>
        
        <!-- 密码强度指示器 -->
        <div v-if="formData.password" class="password-strength">
          <div class="strength-bar">
            <div 
              class="strength-fill" 
              :class="passwordStrength.class"
              :style="{ width: passwordStrength.width }"
            ></div>
          </div>
          <span class="strength-text">{{ passwordStrength.text }}</span>
        </div>
      </div>

      <!-- 确认密码字段 -->
      <div class="form-group">
        <label for="confirmPassword">
          <span class="label-text">确认密码</span>
          <span class="required">*</span>
        </label>
        <div class="input-wrapper">
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="formData.confirmPassword" 
            placeholder="请再次输入密码"
            :class="{ 'error': errors.confirmPassword, 'success': isValidField('confirmPassword') }"
            :disabled="loading"
            @blur="validateField('confirmPassword')"
            @input="clearFieldError('confirmPassword')"
          />
          <span class="input-icon">🔒</span>
        </div>
        <transition name="slide">
          <span v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</span>
        </transition>
      </div>

      <!-- 用户协议 -->
      <div class="form-group">
        <label class="checkbox-label">
          <input 
            type="checkbox" 
            v-model="formData.agreeTo" 
            :disabled="loading"
            @change="clearFieldError('agreeTo')"
          />
          <span class="checkmark"></span>
          <span class="checkbox-text">
            我已阅读并同意 
            <a href="#" @click.prevent="showTerms = true">《用户协议》</a> 
            和 
            <a href="#" @click.prevent="showPrivacy = true">《隐私政策》</a>
          </span>
        </label>
        <transition name="slide">
          <span v-if="errors.agreeTo" class="error-text">{{ errors.agreeTo }}</span>
        </transition>
      </div>

      <!-- 提交按钮 -->
      <button 
        type="submit" 
        class="submit-btn"
        :disabled="loading || !isFormValid"
        :class="{ 'loading': loading }"
      >
        <span v-if="loading" class="spinner"></span>
        <span>{{ loading ? '注册中...' : '创建账号' }}</span>
      </button>

      <!-- 登录链接 -->
      <div class="form-footer">
        <span>已有账号？</span>
        <a href="#" @click.prevent="$emit('switch-to-login')" class="switch-link">
          立即登录
        </a>
      </div>
    </form>
  </div>
</template>

<script>
export default {
  name: 'RegisterForm',
  emits: ['switch-to-login', 'register-success'],
  
  data() {
    return {
      formData: {
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        agreeTo: false
      },
      errors: {},
      loading: false,
      showPassword: false,
      showTerms: false,
      showPrivacy: false,
      message: {
        text: '',
        type: 'error'
      }
    };
  },

  computed: {
    // 表单验证状态
    isFormValid() {
      return this.formData.username.length >= 3 &&
             this.formData.password.length >= 6 &&
             this.formData.password === this.formData.confirmPassword &&
             this.formData.agreeTo &&
             Object.keys(this.errors).length === 0;
    },

    // 密码强度
    passwordStrength() {
      const password = this.formData.password;
      if (!password) return { width: '0%', class: '', text: '' };

      let score = 0;
      let feedback = [];

      if (password.length >= 8) score++;
      else feedback.push('至少8位');

      if (/[a-z]/.test(password)) score++;
      else feedback.push('包含小写字母');

      if (/[A-Z]/.test(password)) score++;
      else feedback.push('包含大写字母');

      if (/\d/.test(password)) score++;
      else feedback.push('包含数字');

      if (/[!@#$%^&*]/.test(password)) score++;
      else feedback.push('包含特殊字符');

      const strengths = [
        { width: '20%', class: 'very-weak', text: '很弱' },
        { width: '40%', class: 'weak', text: '弱' },
        { width: '60%', class: 'medium', text: '中等' },
        { width: '80%', class: 'strong', text: '强' },
        { width: '100%', class: 'very-strong', text: '很强' }
      ];

      return strengths[score] || strengths[0];
    }
  },

  methods: {
    // API基础URL
    getApiUrl() {
      return process.env.VUE_APP_API_URL || 'http://localhost:8080';
    },

    // 显示消息
    showMessage(text, type = 'error') {
      this.message = { text, type };
      setTimeout(() => {
        this.message.text = '';
      }, 5000);
    },

    // 字段验证
    validateField(fieldName) {
      const value = this.formData[fieldName];
      
      switch (fieldName) {
        case 'username':
          if (!value) {
            this.errors.username = '用户名不能为空';
          } else if (value.length < 3) {
            this.errors.username = '用户名至少3个字符';
          } else if (value.length > 20) {
            this.errors.username = '用户名不能超过20个字符';
          } else if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(value)) {
            this.errors.username = '用户名只能包含字母、数字、下划线和中文';
          } else {
            delete this.errors.username;
          }
          break;

        case 'email':
          if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            this.errors.email = '请输入有效的邮箱地址';
          } else {
            delete this.errors.email;
          }
          break;

        case 'password':
          if (!value) {
            this.errors.password = '密码不能为空';
          } else if (value.length < 6) {
            this.errors.password = '密码至少6个字符';
          } else if (value.length > 50) {
            this.errors.password = '密码不能超过50个字符';
          } else {
            delete this.errors.password;
          }
          break;

        case 'confirmPassword':
          if (!value) {
            this.errors.confirmPassword = '请确认密码';
          } else if (value !== this.formData.password) {
            this.errors.confirmPassword = '两次输入的密码不一致';
          } else {
            delete this.errors.confirmPassword;
          }
          break;

        case 'agreeTo':
          if (!this.formData.agreeTo) {
            this.errors.agreeTo = '请同意用户协议和隐私政策';
          } else {
            delete this.errors.agreeTo;
          }
          break;
      }
    },

    // 清除字段错误
    clearFieldError(fieldName) {
      if (this.errors[fieldName]) {
        delete this.errors[fieldName];
      }
    },

    // 检查字段是否有效
    isValidField(fieldName) {
      const value = this.formData[fieldName];
      return value && !this.errors[fieldName];
    },

    // 验证所有字段
    validateAllFields() {
      ['username', 'email', 'password', 'confirmPassword', 'agreeTo'].forEach(field => {
        this.validateField(field);
      });
    },

    // 处理注册
    async handleRegister() {
      this.validateAllFields();
      
      if (!this.isFormValid) {
        this.showMessage('请检查表单中的错误信息');
        return;
      }

      this.loading = true;
      this.message.text = '';

      try {
        const response = await fetch(`${this.getApiUrl()}/auth/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({
            username: this.formData.username,
            password: this.formData.password,
            email: this.formData.email || undefined
          })
        });

        if (!response.ok) {
          const errorData = await response.text();
          throw new Error(errorData || `注册失败: ${response.status}`);
        }

        const result = await response.json();
        
        this.showMessage('注册成功！请登录', 'success');
        this.$emit('register-success', result);
        
        // 清空表单
        this.resetForm();

      } catch (error) {
        console.error('注册错误:', error);
        this.showMessage(error.message || '注册失败，请重试');
      } finally {
        this.loading = false;
      }
    },

    // 重置表单
    resetForm() {
      this.formData = {
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        agreeTo: false
      };
      this.errors = {};
      this.showPassword = false;
    }
  }
};
</script>

<style scoped>
.register-container {
  width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.register-form {
  background: #ffffff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid #e1e5e9;
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
  color: #333;
  margin-bottom: 8px;
}

.form-header p {
  color: #666;
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
  font-weight: 300;
  color: #333;
}

.required {
  color: #e74c3c;
  margin-left: 2px;
}

.optional {
  color: #999;
  font-weight: normal;
  font-size: 12px;
  margin-left: 4px;
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

.password-strength {
  margin-top: 8px;
}

.strength-bar {
  height: 4px;
  background: #e1e5e9;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 2px;
}

.strength-fill.very-weak { background: #e74c3c; }
.strength-fill.weak { background: #f39c12; }
.strength-fill.medium { background: #f1c40f; }
.strength-fill.strong { background: #27ae60; }
.strength-fill.very-strong { background: #2ecc71; }

.strength-text {
  font-size: 12px;
  color: #666;
}

.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  line-height: 1.5;
}

.checkbox-label input[type="checkbox"] {
  display: none;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 4px solid #e1e5e9;
  border-radius: 4px;
  position: relative;
  flex-shrink: 0;
  margin-top: 2px;
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
  font-size: 12px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.checkbox-text {
  font-size: 14px;
  color: #666;
}

.checkbox-text a {
  color: #667eea;
  text-decoration: none;
}

.checkbox-text a:hover {
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
  position: relative;
  overflow: hidden;
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

.form-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e1e5e9;
  color: #666;
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
  .register-container {
    padding: 16px;
  }
  
  .register-form {
    padding: 24px;
  }
  
  .form-header h3 {
    font-size: 20px;
  }
  
  .icon {
    font-size: 40px;
  }
}
</style>