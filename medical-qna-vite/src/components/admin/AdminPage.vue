<template>
  <div class="admin-page">
    <div class="admin-container">
      <div class="admin-header">
        <div class="header-left">
          <h1 class="admin-title">问答系统管理后台</h1>
          <p class="admin-subtitle">管理问答数据记录</p>
        </div>
        
        <!-- 右侧用户信息和操作区域 -->
        <div class="header-right">
          <div class="user-info" v-if="userInfo">
            <span class="user-avatar">👤</span>
            <div class="user-details">
              <span class="username">{{ userInfo.username }}</span>
              <span class="user-role">{{ userInfo.role || 'ADMIN' }}</span>
            </div>
          </div>
          
          <div class="header-actions">
            <button @click="refreshData" class="action-btn refresh-btn" :disabled="loading">
              <i class="icon-refresh"></i>
              {{ loading ? '刷新中...' : '刷新数据' }}
            </button>
            
            <button @click="logout" class="action-btn logout-btn">
              <i class="icon-logout"></i> 退出登录
            </button>
          </div>
        </div>
      </div>

      <!-- 搜索区域：新增双搜索框 -->
      <div class="search-section card">
        <!-- 用户搜索框 -->
        <div class="search-input-group">
          <input 
            v-model="userSearchKeyword" 
            placeholder="输入用户名或邮箱搜索用户..." 
            @input="filterUserList"
            class="search-input"
          />
          <button @click="resetUserSearch" class="btn btn-secondary">
            <i class="icon-reset"></i> 重置用户搜索
          </button>
        </div>
        
        <!-- 问答搜索框 -->
        <div class="search-input-group" style="margin-top: 10px;">
          <input 
            v-model="qaSearchKeyword" 
            placeholder="输入问题或答案关键词搜索..." 
            @input="filterQAList"
            class="search-input"
          />
          <button @click="resetQASearch" class="btn btn-secondary">
            <i class="icon-reset"></i> 重置问答搜索
          </button>
        </div>
      </div>

      <!-- 用户列表部分 -->
      <div class="user-list-section card">
        <h2><i class="icon-user">⭐</i>用户列表</h2>
        <div class="user-display">
          <span class="user-count">{{ filteredUserList.length }}</span>
          <div class="username-tags">
            <span v-for="user in userList" :key="user.id" class="username-tag">
              {{ user.username }}
            </span>
          </div>
        </div>

        <div class="table-container"></div>
          <table class="user-table">
            <thead>
              <tr>
                <th class="id-col">用户ID</th>
                <th class="username-col">用户名</th>
                <th class="email-col">邮箱</th>
                <th class="actions-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredUserList" :key="user.id" class="user-row">
                <td class="id-col">
                  <span class="user-id-badge">{{ user.id }}</span>
                </td>
                <td class="name-col">
                  <div class="username-wrapper">
                    <span class="user-avatar">{{ user.username.charAt(0).toUpperCase() }}</span>
                    <span class="username">{{ user.username }}</span>
                  </div>
                </td>
                <td class="email-col">
                  <a :href="'mailto:${user.email}'" class="email-link">{{ user.email }}</a>
                </td>
                <td class="actions-col">
                  <button @click="goToUserHistory(user.id)" class="btn btn-view">
                    <i class="icon-history"></i> 历史记录
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 用户问答历史部分 -->
      <div class="user-question-history-section card " v-if="userQuestionHistory.length > 0">
        <h2><i class="icon-history">🕒</i>用户问答历史</h2>
        
        <div class="user-display">
          <span class="user-count">{{ filteredQAList.length }}</span>
        </div>

        <div class="table-container">
          <table class="user-table">
            <thead>
              <tr>
                <th class="id-col">ID</th>
                <th class="username-col">所属用户</th>
                <th class="question-col">问题</th>
                <th class="answer-col">答案</th>
                <th class="actions-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredQAList" :key="item.id" class="user-row">
                <td class="id-col">
                  <span class="user-id-badge">{{ item.id }}</span>
                </td>
                <td class="username-col">
                  <div class="username-wrapper">
                    <span class="user-avatar">{{ getUserById(item.userId)?.username.charAt(0).toUpperCase() || '?' }}</span>
                    <span class="username">{{ getUserById(item.userId)?.username || '未知用户' }}</span>
                  </div>
                </td>
                <td class="question-col">
                  <input 
                    v-if="editId === item.id" 
                    v-model="editQuestion" 
                    class="edit-input"
                  />
                  <span v-else>{{ item.question }}</span>
                </td>
                <td class="answer-col">
                  <input 
                    v-if="editId === item.id" 
                    v-model="editAnswer" 
                    class="edit-input"
                  />
                  <span v-else>{{ item.answer }}</span>
                </td>
                <td class="actions-col">
                  <button 
                    v-if="editId !== item.id" 
                    @click="editRecord(item)" 
                    class="btn btn-edit"
                  >
                    <i class="icon-edit"></i> 编辑
                  </button>
                  <button 
                    v-if="editId === item.id" 
                    @click="saveRecord(item)" 
                    class="btn btn-save"
                  >
                    <i class="icon-save"></i> 保存
                  </button>
                  <button 
                    @click="deleteRecord(item.id)" 
                    class="btn btn-delete"
                  >
                    <i class="icon-delete"></i> 删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredUserList.length === 0 && !loading" class="empty-state">
        <i class="icon-empty">☹</i>
        <p>暂无用户数据</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
      </div>

      <!-- 全局提示组件 -->
      <div v-if="globalMessage" class="global-message" :class="globalMessageType">
        <span>{{ globalMessage }}</span>
        <button @click="globalMessage = ''" class="close-btn">✕</button>
      </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { authUtils } from '@/utils/api'
import { useRouter } from 'vue-router'

const router = useRouter()

// 数据相关
const historyList = ref([])
const loading = ref(false)
// 分离搜索关键词：用户搜索和问答搜索
const userSearchKeyword = ref('')  // 用户搜索关键词
const qaSearchKeyword = ref('')   // 问答搜索关键词

// 过滤后的用户列表（基于用户搜索关键词）
const filteredUserList = computed(() => {
  if (!userSearchKeyword.value.trim()) {
    return userList.value
  }
  const keyword = userSearchKeyword.value.toLowerCase()
  return userList.value.filter(user => 
    user.username.toLowerCase().includes(keyword) ||
    user.email.toLowerCase().includes(keyword)
  )
})

// 过滤后的问答列表（基于问答搜索关键词）
const filteredQAList = computed(() => {
  if (!qaSearchKeyword.value.trim()) {
    return userQuestionHistory.value
  }
  const keyword = qaSearchKeyword.value.toLowerCase()
  return userQuestionHistory.value.filter(
    (item) =>
      item.question.toLowerCase().includes(keyword) ||
      item.answer.toLowerCase().includes(keyword)
  )
})

const userList = ref([]) // 存储用户列表
const userQuestionHistory = ref([]) // 存储用户问答历史

// 编辑相关
const editId = ref(null)
const editQuestion = ref('')
const editAnswer = ref('')

// 用户信息
const userInfo = ref(null)

// 全局消息
const globalMessage = ref('')
const globalMessageType = ref('info')

// 根据用户ID获取用户信息
const getUserById = (userId) => {
  return userList.value.find(user => user.id === userId)
}

// 初始化加载数据
onMounted(async () => {
  await initializePage()
})

// 页面初始化
const initializePage = async () => {
  try {
    // 检查认证状态
    if (!authUtils.isLoggedIn()) {
      showGlobalMessage('请先登录', 'warning')
      router.push('/')
      return
    }

    // 获取用户信息
    userInfo.value = authUtils.getUserInfo()
    
    // 检查管理员权限
    if (!authUtils.hasPermission('ADMIN')) {
      showGlobalMessage('没有管理员权限', 'error')
      router.push('/qa')
      return
    }

    await fetchData()
    showGlobalMessage('页面加载完成', 'success')
  } catch (error) {
    console.error('页面初始化失败:', error)
    showGlobalMessage('页面初始化失败: ' + error.message, 'error')
  }
}

// 从API获取数据
const fetchData = async () => {
  try {
    loading.value = true

    // 调用管理员 API 获取所有用户信息
    const userResponse = await api.admin.getAllUsers()
    if (userResponse.success) {
      userList.value = userResponse.data
    }

    // 调用管理员 API 获取所有问答记录
    const response = await api.admin.getAllQuestionAnswers({ page: 0, size: 10 }) // 假设分页参数
    if (response.success) {
      userQuestionHistory.value = response.data.content; 
    }

  } catch (error) {
    console.error('获取数据失败:', error)
    showGlobalMessage('获取数据失败: ' + error.message, 'error')
  } finally {
    loading.value = false
  }
}

// 获取用户问答历史
const getUserQuestionHistoryData = async (userId) => {
  try {
    loading.value = true
    const response = await api.admin.getUserQuestionHistory(userId, { page: 0, size: 10 }) // 假设分页参数
    if (response.success) {
      userQuestionHistory.value = response.data.content
    }
  } catch (error) {
    console.error('获取用户问答历史失败:', error)
    showGlobalMessage('获取用户问答历史失败: ' + error.message, 'error')
  } finally {
    loading.value = false
  }
}

// 跳转到用户历史记录页面
const goToUserHistory = (userId) => {
  router.push({ name: 'UserHistory', params: { userId } });
  // 同时加载该用户的问答历史
  getUserQuestionHistoryData(userId);
};

// 用户搜索相关方法
const filterUserList = () => {
  // 由computed自动处理过滤
}

const resetUserSearch = () => {
  userSearchKeyword.value = ''
}

// 问答搜索相关方法
const filterQAList = () => {
  // 由computed自动处理过滤
}

const resetQASearch = () => {
  qaSearchKeyword.value = ''
}

// 编辑功能
const editRecord = (item) => {
  editId.value = item.id
  editQuestion.value = item.question
  editAnswer.value = item.answer
}

// 保存记录到API
const saveRecord = async (item) => {
  try {
    loading.value = true
    
    // 调用 API 更新记录
    const response = await api.admin.updateQuestionAnswer(item.id, {
      question: editQuestion.value,
      answer: editAnswer.value
    })
    if (!response.success) {
      throw new Error(response.message)
    }
    
    const index = userQuestionHistory.value.findIndex((i) => i.id === item.id)
    if (index !== -1) {
      userQuestionHistory.value[index].question = editQuestion.value
      userQuestionHistory.value[index].answer = editAnswer.value
    }
    editId.value = null
    showGlobalMessage('保存成功', 'success')
  } catch (error) {
    console.error('保存失败:', error)
    showGlobalMessage('保存失败: ' + error.message, 'error')
  } finally {
    loading.value = false
  }
}

// 删除记录
const deleteRecord = async (id) => {
  if (!confirm('确定要删除这条记录吗？')) return
  
  try {
    loading.value = true
    
    // 调用 API 删除记录
    const response = await api.admin.deleteQuestionAnswer(id)
    if (!response.success) {
      throw new Error(response.message)
    }
    
    userQuestionHistory.value = userQuestionHistory.value.filter((item) => item.id !== id)
    showGlobalMessage('删除成功', 'success')
  } catch (error) {
    console.error('删除失败:', error)
    showGlobalMessage('删除失败: ' + error.message, 'error')
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = async () => {
  await fetchData()
  showGlobalMessage('数据已刷新', 'success')
}

// 退出登录
const logout = async () => {
  try {
    if (!confirm('确定要退出登录吗？')) {
      return
    }

    loading.value = true
    showGlobalMessage('正在退出登录...', 'info')

    try {
      // 调用后端注销接口
      const result = await api.auth.logout()
      console.log('服务器注销结果:', result)
    } catch (logoutError) {
      console.warn('服务器注销失败，但本地状态已清除:', logoutError)
    }

    // 清除本地认证信息
    authUtils.clearToken()
    userInfo.value = null

    showGlobalMessage('已退出登录', 'success')

    // 延迟跳转到登录页面
    setTimeout(() => {
      router.push('/')
    }, 1000)

  } catch (error) {
    console.error('退出登录失败:', error)
    showGlobalMessage('退出登录失败: ' + error.message, 'error')
    
    // 即使出错也清除本地状态
    authUtils.clearToken()
    userInfo.value = null
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 显示全局消息
const showGlobalMessage = (message, type = 'info') => {
  globalMessage.value = message
  globalMessageType.value = type
  
  setTimeout(() => {
    if (globalMessage.value === message) {
      globalMessage.value = ''
    }
  }, 3000)
}
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 50px;
  background-image: url('@/assets/bj5.jpg');
  background-size: cover;
  background-position: relative;
  background-repeat: no-repeat;
  background-attachment: fixed;
  min-height: 100vh;
  padding: 20px;
}

.admin-container {
  position: relative;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  z-index: 100;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #333;
  overflow: hidden;
}


.admin-header {
  margin-bottom: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 2px solid #e9ecef;
}

.header-left h2 {
  color: #2c3e50;
  font-size: 28px;
  margin-bottom: 8px;
}

.admin-title {
  font-size: 2.5rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.admin-subtitle {
  background: linear-gradient(90deg, #4299e1, #3182ce);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  display: inline-block;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f7fafc, #ebf8ff);
  border-radius: 12px;
  border: 1px solid #bee3f8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.user-list-section {
  border-radius: 12px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #f8fafc;
  border-bottom: 1px solid #edf2f7;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-count {
  font-size: 14px;
  color: #718096;
  background: #edf2f7;
  padding: 4px 10px;
  border-radius: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #4299e1;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}

.username {
  font-weight: 500;
  color: #2d3748;
}

.user-display {
  display: flex;
  flex-direction: column;
  width: 100%;
  margin-left: 12px;
  align-items: flex-start;
  gap: 10px;
}

.username-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 300px;
  overflow: hidden;
}

.username-tag {
  display: inline-block;
  background-color: #e2e8f0;
  padding: 4px 8px;
  border-radius: 16px;
  font-size: 14px;
  color: #4a5568;
  white-space: nowrap;
}

.table-container {
  overflow-x: auto;
}

.user-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.user-table th {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
  background-color: #f7fafc;
  text-align: left;
  position: sticky;
  top: 0;;
}

.user-table td {
  padding: 16px;
  border-bottom: 1px solid #edf2f7;
  vertical-align: middle;
}

.id-col { width: 120px; }
.name-col { width: 180px; }
.email-col { min-width: 200px; }
.action-col { width: 140px; }

.user-id-badge {
  display: inline-block;
  padding: 4px 8px;
  background-color: #ebf8ff;
  color: #3182ce;
  border-radius: 4px;
  font-size: 13px;
  font-family: monospace;
}

.username-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}


.user-row:hover {
  background-color: #f8fafc;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.user-role {
  font-size: 12px;
  color: #6c757d;
  background: #e7f3ff;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 6px;
}

.refresh-btn {
  background-color: #17a2b8;
  color: white;
}

.refresh-btn:hover:not(:disabled) {
  background-color: #138496;
  transform: translateY(-1px);
}

.refresh-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  transform: none;
}

.logout-btn {
  background-color: #dc3545;
  color: white;
}

.logout-btn:hover {
  background-color: #c82333;
  transform: translateY(-1px);
}

.user-question-history-section {
  position: relative;
  z-index: 10;
  background-color: rgba(255, 255, 255, 0.7);
  border: 1px solid #e2e8f0;
  isolation: isolate; /* 创建新的堆叠上下文 */
}

.question-col {
  min-width: 250px;
}

.answer-col {
  min-width: 250px;
}

/* 编辑输入框样式 */
.edit-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: #fff;
}

/* 操作按钮组样式 */
.actions-col {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
}

.card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  backdrop-filter: blur(5px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  padding: 20px;
  margin-bottom: 20px;
}

.search-section {
  margin-bottom: 20px;
}

.search-input-group {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.search-input:focus {
  border-color: #3498db;
  outline: none;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.table-container {
  overflow-x: auto;
}

.qa-table {
  width: 100%;
  border-radius: 8px;
  border: 1px solid #ddd;
  border-collapse: collapse;
  overflow: auto;
}

.qa-table th {
  background-color: linear-gradient(135deg, #4299e1, #3182ce);
  color: #495057;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.5px;
  text-align: left;
  padding: 12px 15px;
  border-bottom: 2px solid #e9ecef;
}

.qa-table td {
  padding: 12px 15px;
  border-bottom: 1px solid #e9ecef;
  vertical-align: middle;
}

.qa-table tr:hover td {
  background-color: #ebf8ff;
  transform: scale(1.01);
}

.edit-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn {
  display: inline-flex;
  position: relative;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn i {
  margin-right: 6px;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.btn-edit {
  background-color: #17a2b8;
  color: white;
}

.btn-edit:hover {
  background-color: #138496;
}

.btn-save {
  background-color: #28a745;
  color: white;
}

.btn-save:hover {
  background-color: #218838;
}

.btn-delete {
  background-color: #dc3545;
  color: white;
}

.btn-delete:hover {
  background-color: #c82333;
}

.btn-view {
  background-color: #4299e1;
  color: white;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-view:hover {
  background-color: #3182ce;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 无用户状态 */
.no-users {
  padding: 40px 0;
  text-align: center;
  color: #a0aec0;
}

.no-users i {
  font-size: 48px;
  margin-bottom: 12px;
  display: block;
}

.no-users p {
  margin: 0;
  font-size: 15px;
}

/* 图标样式 */
.icon-users::before { content: "👥"; }
.icon-history::before { content: "🕒"; }
.icon-search-empty::before { content: "🔍"; }

/* 响应式调整 */
@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .user-count {
    align-self: flex-start;
  }
  
  .user-table th, .user-table td {
    padding: 12px;
  }
  
  .btn-view {
    padding: 6px 10px;
  }
}

.empty-state {
  padding: 40px 0;
  text-align: center;
  color: #6c757d;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 全局消息 */
.global-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.global-message.info { 
  background: #17a2b8; 
}

.global-message.success { 
  background: #28a745; 
}

.global-message.warning { 
  background: #ffc107; 
  color: #212529; 
}

.global-message.error { 
  background: #dc3545; 
}

.close-btn {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  margin-left: 8px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

/* 图标样式 - 可以使用实际图标库如Font Awesome */
.icon-reset::before { content: "↻"; }
.icon-edit::before { content: "✎"; }
.icon-save::before { content: "✓"; }
.icon-delete::before { content: "✕"; }
.icon-empty::before { content: "☹"; }
.icon-refresh::before { content: "🔄"; }
.icon-logout::before { content: "🚪"; }

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .header-right {
    flex-direction: column;
    gap: 12px;
  }

  .header-actions {
    flex-direction: row;
    justify-content: center;
  }

  .search-input-group {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
    gap: 4px;
  }

  .user-info {
    justify-content: center;
  }
}
</style>