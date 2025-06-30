import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const isAuthenticated = ref(false)
  const router = useRouter()

  // 模拟登录API
  const login = async (credentials) => {
    // 这里应该是调用实际的登录API
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (credentials.username && credentials.password) {
          user.value = {
            username: credentials.username,
            token: 'mock-token'
          }
          isAuthenticated.value = true
          resolve()
        } else {
          reject(new Error('用户名或密码错误'))
        }
      }, 500)
    })
  }

  const logout = () => {
    user.value = null
    isAuthenticated.value = false
    router.push('/login')
  }

  return {
    user,
    isAuthenticated,
    login,
    logout
  }
})