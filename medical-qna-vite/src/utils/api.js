// utils/api.js
const ip = 'http://localhost:8080';
import axios from 'axios';

// 设置默认配置
axios.defaults.baseURL = ip;
axios.defaults.timeout = 30000;
axios.defaults.withCredentials = true;

// 请求拦截器 - 自动添加token
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 调试日志
    console.log('API请求:', {
      url: config.url,
      method: config.method.toUpperCase(),
      hasToken: !!token,
      data: config.data
    });
    
    return config;
  },
  error => {
    console.error('请求拦截器错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器 - 处理认证错误和统一响应格式
axios.interceptors.response.use(
  response => {
    console.log('API响应:', {
      url: response.config.url,
      status: response.status,
      data: response.data
    });
    return response;
  },
  error => {
    console.error('API错误:', {
      url: error.config?.url,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data
    });
    
    // 处理401认证失败
    if (error.response?.status === 401) {
      console.warn('认证失败，清除token');
      authUtils.clearToken();
      
      // 如果不在登录页面，跳转到登录页面
      if (window.location.hash !== '#/' && window.location.hash !== '#/auth') {
        console.log('跳转到登录页面');
        window.location.href = window.location.origin + window.location.pathname + '#/';
      }
    }
    
    return Promise.reject(error);
  }
);

class ApiClient {
  
  // 统一处理响应格式
  _handleResponse(response) {
    // 适配你的后端响应格式
    if (response.data && typeof response.data === 'object') {
      // 如果有success字段，直接使用
      if ('success' in response.data) {
        return {
          success: response.data.success,
          data: response.data.data,
          message: response.data.message || ''
        };
      }
      // 如果有code字段，转换为success格式
      else if ('code' in response.data) {
        return {
          success: response.data.code === '0000',
          data: response.data.data,
          message: response.data.message || ''
        };
      }
      // 其他情况，直接返回data
      else {
        return {
          success: true,
          data: response.data,
          message: 'success'
        };
      }
    }
    
    // 默认成功响应
    return {
      success: true,
      data: response.data,
      message: 'success'
    };
  }

  // 统一处理错误
  _handleError(error) {
    let message = '网络错误';
    
    if (error.response) {
      // 服务器响应了错误状态码
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          message = '认证失败，请重新登录';
          break;
        case 403:
          message = '没有权限访问';
          break;
        case 404:
          message = '接口不存在';
          break;
        case 500:
          message = '服务器内部错误';
          break;
        default:
          message = data?.message || `请求失败 (${status})`;
      }
    } else if (error.request) {
      // 请求发出但没有收到响应
      message = '网络连接失败，请检查网络';
    } else {
      // 其他错误
      message = error.message || '未知错误';
    }
    
    return {
      success: false,
      data: null,
      message: message
    };
  }

  async post(url, data = {}) {
    try {
      const response = await axios.post(url, data);
      return this._handleResponse(response);
    } catch (error) {
      return this._handleError(error);
    }
  }

  async get(url, params = {}) {
    try {
      const response = await axios.get(url, { params });
      return this._handleResponse(response);
    } catch (error) {
      return this._handleError(error);
    }
  }

  async put(url, data = {}) {
    try {
      const response = await axios.put(url, data);
      return this._handleResponse(response);
    } catch (error) {
      return this._handleError(error);
    }
  }

  async delete(url, params = {}) {
    try {
      const response = await axios.delete(url, { params });
      return this._handleResponse(response);
    } catch (error) {
      return this._handleError(error);
    }
  }

  // 专门用于公开接口的方法（不自动添加token）
  async publicGet(url, params = {}) {
    try {
      const response = await axios.get(url, { 
        params,
        headers: {
          // 明确不发送Authorization头
        }
      });
      return this._handleResponse(response);
    } catch (error) {
      return this._handleError(error);
    }
  }

  // 认证相关接口
  auth = {
    login: async (credentials) => {
      const result = await this.post('/api/auth/login', credentials);
      
      // 如果登录成功，保存token
      if (result.success && result.data && result.data.token) {
        authUtils.saveToken(result.data.token);
        
        // 保存用户信息（如果有）
        if (result.data.user) {
          authUtils.saveUserInfo(result.data.user);
        }
      }
      
      return result;
    },
    
    logout: async () => {
      try {
        const result = await this.post('/api/auth/logout');
        return result;
      } catch (error) {
        // 即使服务器注销失败，也要清除本地状态
        return { success: true, message: '本地注销成功' };
      } finally {
        // 无论成功与否都清除本地token
        authUtils.clearToken();
      }
    },
    
    register: (userData) => this.post('/api/auth/register', userData),
    
    getCurrentUser: () => this.get('/api/auth/current'),
    
    checkSession: () => this.get('/api/auth/check')
  };

  // 问答相关接口
  question = {
    // 注意：ask接口使用SSE，不通过这个方法
    // 实际的ask请求在组件中直接使用fetch处理SSE
    getHistory: () => this.get('/api/question/history'),
    
    deleteHistory: (id) => this.delete(`/api/question/history/${id}`),
    
    clearHistory: () => this.delete('/api/question/history'),
    
    getStats: () => this.get('/api/question/stats'),
    
    // 健康检查接口（公开访问）
    springHealth: () => this.publicGet('/api/question/spring-health'),
    
    health: () => this.publicGet('/api/question/health'),
    
    testAuth: () => this.get('/api/question/test-auth'),
    
    testPython: () => this.get('/api/question/test-python')
  };

  // 用户相关接口（如果有的话）
  user = {
    getProfile: () => this.get('/api/user/profile'),
    
    updateProfile: (data) => this.put('/api/user/profile', data)
  };
}

// 创建API实例
const api = new ApiClient();

// 导出API实例
export default api;

// 也可以导出单独的方法供直接使用
export const { auth, question, user } = api;

// 增强的认证工具类
export const authUtils = {
  // Token 相关常量
  TOKEN_KEY: 'token',
  USER_INFO_KEY: 'userInfo',
  
  // 保存 Token
  saveToken(token) {
    try {
      localStorage.setItem(this.TOKEN_KEY, token);
      console.log('Token已保存:', token.substring(0, 20) + '...');
    } catch (error) {
      console.error('保存Token失败:', error);
    }
  },
  
  // 获取 Token
  getToken() {
    try {
      return localStorage.getItem(this.TOKEN_KEY);
    } catch (error) {
      console.error('获取Token失败:', error);
      return null;
    }
  },
  
  // 清除 Token 和相关数据
  clearToken() {
    try {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.USER_INFO_KEY);
      console.log('Token和用户信息已清除');
    } catch (error) {
      console.error('清除Token失败:', error);
    }
  },
  
  // 手动设置 Token
  setToken(token) {
    this.saveToken(token);
  },
  
  // 检查是否已登录（基于本地Token）
  isLoggedIn() {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    
    // 检查Token格式
    if (!this.isValidJWTFormat(token)) {
      console.warn('Token格式无效');
      this.clearToken();
      return false;
    }
    
    // 检查Token是否过期
    if (this.isTokenExpired(token)) {
      console.warn('Token已过期');
      this.clearToken();
      return false;
    }
    
    return true;
  },
  
  // 检查JWT格式
  isValidJWTFormat(token) {
    if (!token || typeof token !== 'string') {
      return false;
    }
    
    // JWT应该有三个部分，用.分隔
    const parts = token.split('.');
    return parts.length === 3;
  },
  
  // 检查Token是否过期
  isTokenExpired(token = null) {
    try {
      const currentToken = token || this.getToken();
      if (!currentToken) {
        return true;
      }
      
      // 解析JWT payload
      const payload = this.parseJWTPayload(currentToken);
      if (!payload || !payload.exp) {
        console.warn('无法解析Token过期时间');
        return true;
      }
      
      // 获取当前时间戳（秒）
      const currentTime = Math.floor(Date.now() / 1000);
      const expTime = payload.exp;
      const remainingSeconds = expTime - currentTime;
      
      console.log('Token过期检查:', {
        currentTime: currentTime,
        expTime: expTime,
        remainingSeconds: remainingSeconds,
        remainingMinutes: Math.floor(remainingSeconds / 60),
        remainingHours: Math.floor(remainingSeconds / 3600)
      });
      
      // 只有真正过期才返回true（不加缓冲时间）
      const isExpired = currentTime >= expTime;
      
      if (isExpired) {
        console.log('Token已真正过期');
      } else {
        console.log(`Token还有 ${Math.floor(remainingSeconds / 60)} 分钟过期`);
      }
      
      return isExpired;
      
    } catch (error) {
      console.error('检查Token过期状态失败:', error);
      return true;
    }
  },
  
  // 解析JWT Payload
  parseJWTPayload(token) {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }
      
      // 解码Base64URL
      const payload = parts[1];
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded);
      
    } catch (error) {
      console.error('解析JWT Payload失败:', error);
      return null;
    }
  },
  
  // 获取用户信息（从Token或本地存储）
  getUserInfo() {
    try {
      // 优先从Token中获取
      const token = this.getToken();
      if (token) {
        const payload = this.parseJWTPayload(token);
        if (payload) {
          return {
            username: payload.sub,
            userId: payload.userId,
            role: payload.role,
            exp: payload.exp
          };
        }
      }
      
      // 备选：从本地存储获取
      const userInfo = localStorage.getItem(this.USER_INFO_KEY);
      return userInfo ? JSON.parse(userInfo) : null;
      
    } catch (error) {
      console.error('获取用户信息失败:', error);
      return null;
    }
  },
  
  // 保存用户信息到本地存储
  saveUserInfo(userInfo) {
    try {
      localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(userInfo));
      console.log('用户信息已保存:', userInfo.username || userInfo.id);
    } catch (error) {
      console.error('保存用户信息失败:', error);
    }
  },
  
  // 获取用户角色
  getUserRole() {
    const userInfo = this.getUserInfo();
    return userInfo ? userInfo.role : null;
  },
  
  // 检查用户权限
  hasPermission(requiredRole) {
    const userRole = this.getUserRole();
    if (!userRole) {
      return false;
    }
    
    // 简单的角色检查
    const roleHierarchy = {
      'ADMIN': 3,
      'USER': 2,
      'GUEST': 1
    };
    
    const userLevel = roleHierarchy[userRole] || 0;
    const requiredLevel = roleHierarchy[requiredRole] || 0;
    
    return userLevel >= requiredLevel;
  },
  
  // 获取Token剩余时间（秒）
  getTokenRemainingTime() {
    try {
      const token = this.getToken();
      if (!token) {
        return 0;
      }
      
      const payload = this.parseJWTPayload(token);
      if (!payload || !payload.exp) {
        return 0;
      }
      
      const currentTime = Math.floor(Date.now() / 1000);
      const remainingTime = payload.exp - currentTime;
      
      return Math.max(0, remainingTime);
      
    } catch (error) {
      console.error('获取Token剩余时间失败:', error);
      return 0;
    }
  },
  
  // 调试Token信息
  debugToken(token = null) {
    try {
      const currentToken = token || this.getToken();
      if (!currentToken) {
        console.log('DEBUG: 没有Token');
        return null;
      }
      
      const payload = this.parseJWTPayload(currentToken);
      if (!payload) {
        console.log('DEBUG: 无法解析Token');
        return null;
      }
      
      const currentTime = Math.floor(Date.now() / 1000);
      const info = {
        username: payload.sub,
        userId: payload.userId,
        role: payload.role,
        issuedAt: payload.iat ? new Date(payload.iat * 1000).toLocaleString() : 'unknown',
        expiresAt: payload.exp ? new Date(payload.exp * 1000).toLocaleString() : 'unknown',
        currentTime: new Date(currentTime * 1000).toLocaleString(),
        remainingSeconds: payload.exp ? payload.exp - currentTime : 0,
        remainingMinutes: payload.exp ? Math.floor((payload.exp - currentTime) / 60) : 0,
        isExpired: payload.exp ? currentTime >= payload.exp : true
      };
      
      console.log('DEBUG Token 信息:', info);
      return info;
      
    } catch (error) {
      console.error('DEBUG Token 失败:', error);
      return null;
    }
  },
  
  // 格式化剩余时间
  formatRemainingTime() {
    const seconds = this.getTokenRemainingTime();
    if (seconds <= 0) {
      return '已过期';
    }
    
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (hours > 0) {
      return `${hours}小时${minutes}分钟`;
    } else if (minutes > 0) {
      return `${minutes}分钟`;
    } else {
      return `${seconds}秒`;
    }
  }
};
