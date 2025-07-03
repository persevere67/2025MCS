const ip = 'http://localhost:8080';
import axios from 'axios';


class ApiClient {
  constructor() {
    this.baseURL = ip; // 设置基础URL
  }

  async request(url, options = {}) {
    const config = {
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      ...options
    };

    try {
      const response = await fetch(`${this.baseURL}${url}`, config);
      const result = await response.json();

      if (response.ok && result.code === '0000') {
        return {
          success: true,
          data: result.data,
          message: result.message
        };
      } else {
        return {
          success: false,
          message: result.message || `请求失败: ${response.status}`,
          code: result.code
        };
      }
    } catch (error) {
      console.error('API请求错误:', error);
      return {
        success: false,
        message: error.message || '网络错误,请检查连接'
      };
    }
  }

  async get(url, params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const fullUrl = queryString ? `${url}?${queryString}` : url;
    return this.request(fullUrl, { method: 'GET' });
  }

  async post(url, data = {}) {
    return this.request(url, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  auth = {
    login: (credentials) => this.post('/api/auth/login', credentials),
    register: (userData) => this.post('/api/auth/register', userData),
    logout: () => this.post('/api/auth/logout'),
    getCurrentUser: () => this.get('/api/auth/current'),
    checkSession: () => this.get('/api/auth/check')
  };

  question = {
    ask: (question) => this.post('/api/question/ask', { question }),
    getHistory: () => this.get('/api/question/history')
  };
}

const api = new ApiClient();

const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080',
  withCredentials: true // 必须设置为 true 以发送 cookies
})

// 请求拦截器
api.interceptors.request.use(config => {
  const token = localStorage.getItem('authToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

// 响应拦截器
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      // 未授权，清除认证状态并跳转到登录页
      const authStore = useAuthStore()
      authStore.logout()
    }
    return Promise.reject(error)
  }
)


export default api;