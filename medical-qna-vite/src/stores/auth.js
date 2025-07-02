import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const isAuthenticated = ref(false)
  const router = useRouter()

  // 模拟登录API
  const login = async (credentials) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (credentials.username && credentials.password) {
          user.value = {
            username: credentials.username,
            token: 'mock-token'
          }
          isAuthenticated.value = true
          // 添加本地存储
          localStorage.setItem('authToken', 'mock-token')
          localStorage.setItem('userData', JSON.stringify(user.value))
          resolve()
        } else {
          reject(new Error('用户名或密码错误'))
        }
      }, 500)
    })
  }

  const logout = async() => {
    try {
      await api.post('/api/auth/logout')
    } 
    catch (error) {
      console.log(error)
    }
    finally {
      user.value = null
      isAuthenticated.value = false
      localStorage.removeItem('authToken')
      localStorage.removeItem('userData')
      router.push('/auth')
    }
  }
  // 初始化方法
  const initialize = () => {
    const token = localStorage.getItem('authToken')
    const userData = localStorage.getItem('userData')
    if (token && userData) {
      user.value = JSON.parse(userData)
      isAuthenticated.value = true
    }
  }

  return {
    user,
    isAuthenticated,
    login,
    logout,
    initialize // 确保导出 initialize 方法
  }
})