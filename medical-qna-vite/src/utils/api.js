class ApiClient {
  constructor() {
    this.baseURL = process.env.VUE_APP_API_URL || 'http://localhost:8080';
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
        message: error.message || '网络错误，请检查连接'
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
export default api;