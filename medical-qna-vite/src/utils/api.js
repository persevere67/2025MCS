const ip = 'http://localhost:8080';
import axios from 'axios';

axios.defaults.baseURL = ip;

axios.interceptors.request.use(config => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

class ApiClient {
  async post(url, data = {}) {
    try {
      const response = await axios.post(url, data);
      if (response.data.code === '0000') {
        return {
          success: true,
          data: response.data.data,
          message: response.data.message
        };
      } else {
        return {
          success: false,
          message: response.data.message
        };
      }
    } catch (error) {
      return {
        success: false,
        message: error.message || '网络错误'
      };
    }
  }

  async get(url, params = {}) {
    try {
      const response = await axios.get(url, { params });
      if (response.data.code === '0000') {
        return {
          success: true,
          data: response.data.data,
          message: response.data.message
        };
      } else {
        return {
          success: false,
          message: response.data.message
        };
      }
    } catch (error) {
      return {
        success: false,
        message: error.message || '网络错误'
      };
    }
  }

  auth = {
    login: (credentials) => this.post('/api/auth/login', credentials),
    logout: () => this.post('/api/auth/logout'),
    register: (userData) => this.post('/api/auth/register', userData),
    getCurrentUser: () => this.get('/api/auth/current'),
    checkSession: () => this.get('/api/auth/check')
  };

  question = {
    ask: (question) => this.post('/api/question/ask', { question }),
    getHistory: () => this.get('/api/question/history')
  };
}

const api = new ApiClient();
export default api;
