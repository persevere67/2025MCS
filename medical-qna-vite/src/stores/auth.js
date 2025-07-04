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
    const res = await api.auth.login(credentials)
    if(res.sucsess){
      user.value = res.data
      isAuthenticated.value = true
      localStorage.setItem('authToken', res.token)
      localStorage.setItem('userData', JSON.stringify(res.data))
    }
    else {
        console.log('登录失败')
    }
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