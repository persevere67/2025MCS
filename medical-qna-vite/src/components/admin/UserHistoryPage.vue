<template>
  <div class="user-history-page">
    <div class="admin-container">
      <!-- 返回按钮和标题 -->
      <div class="admin-header">
        <div class="header-left">
          <h1 class="admin-title">用户问答历史</h1>
          <p class="admin-subtitle">用户ID: {{ route.params.userId }}</p>
        </div>
        
        <div class="header-actions">
          <button @click="goBack" class="action-btn back-btn">
            <i class="icon-back"></i> 返回
          </button>
          <button @click="refreshData" class="action-btn refresh-btn" :disabled="loading">
            <i class="icon-refresh"></i>
            {{ loading ? '刷新中...' : '刷新数据' }}
          </button>
        </div>
      </div>

      <!-- 问答历史表格 -->
      <div class="user-question-history-section card">
        <div class="user-display">
          <span class="user-count">{{ userQuestionHistory.length }}</span>
        </div>

        <div class="table-container">
          <table class="user-table">
            <thead>
              <tr>
                <th class="id-col">ID</th>
                <th class="question-col">问题</th>
                <th class="answer-col">答案</th>
                <th class="actions-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in userQuestionHistory" :key="item.id" class="user-row">
                <td class="id-col">
                  <span class="user-id-badge">{{ item.id }}</span>
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
      <div v-if="userQuestionHistory.length === 0 && !loading" class="empty-state">
        <i class="icon-empty">☹</i>
        <p>暂无历史记录</p>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api, { authUtils } from '@/utils/api'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 数据相关
const loading = ref(false)
const userQuestionHistory = ref([])
// 编辑相关
const editId = ref(null)
const editQuestion = ref('')
const editAnswer = ref('')
// 全局消息
const globalMessage = ref('')
const globalMessageType = ref('info')

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
    const userInfo = authUtils.getUserInfo()
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
    loading.value = true;
    const userId = route.params.userId;
    if (!userId) {
      showGlobalMessage('用户ID不存在', 'error');
      return;
    }
    const response = await api.admin.getUserQuestionHistory(userId, { page: 0, size: 10 });
    if (response.success) {
      userQuestionHistory.value = response.data.content || [];
      if (userQuestionHistory.value.length === 0) {
        showGlobalMessage('该用户暂无问答记录', 'info');
      }
    } else {
      showGlobalMessage('获取历史记录失败: ' + response.message, 'error');
    }
  } catch (error) {
    console.error('获取数据失败:', error);
    showGlobalMessage('网络错误，无法加载数据', 'error');
  } finally {
    loading.value = false;
  }
};

// 刷新数据
const refreshData = async () => {
  await fetchData()
  showGlobalMessage('数据已刷新', 'success')
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
    const userId = route.params.userId
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
    const userId = route.params.userId
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

// 返回上级界面
const goBack = () => {
  router.go(-1)
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
.user-history-page {
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

.back-btn {
  background-color: #6c757d;
  color: white;
}

.back-btn:hover {
  background-color: #5a6268;
  transform: translateY(-1px);
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

.user-question-history-section {
  position: relative;
  z-index: 10;
  background-color: rgba(255, 255, 255, 0.7);
  border: 1px solid #e2e8f0;
  isolation: isolate;
}

.user-display {
  display: flex;
  flex-direction: column;
  width: 100%;
  margin-left: 12px;
  align-items: flex-start;
  gap: 10px;
}

.user-count {
  font-size: 14px;
  color: #718096;
  background: #edf2f7;
  padding: 4px 10px;
  border-radius: 12px;
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
  top: 0;
}

.user-table td {
  padding: 16px;
  border-bottom: 1px solid #edf2f7;
  vertical-align: middle;
}

.id-col { width: 120px; }
.question-col { min-width: 250px; }
.answer-col { min-width: 250px; }
.actions-col { width: 180px; }

.user-id-badge {
  display: inline-block;
  padding: 4px 8px;
  background-color: #ebf8ff;
  color: #3182ce;
  border-radius: 4px;
  font-size: 13px;
  font-family: monospace;
}

.user-row:hover {
  background-color: #f8fafc;
}

.edit-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: #fff;
}

.actions-col {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
}

.btn {
  display: inline-flex;
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

.card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  backdrop-filter: blur(5px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  padding: 20px;
  margin-bottom: 20px;
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

.icon-back::before { content: "←"; }
.icon-refresh::before { content: "🔄"; }
.icon-edit::before { content: "✎"; }
.icon-save::before { content: "✓"; }
.icon-delete::before { content: "✕"; }
.icon-empty::before { content: "☹"; }

@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .header-actions {
    flex-direction: row;
    justify-content: center;
  }

  .user-table th, .user-table td {
    padding: 12px;
  }

  .actions-col {
    flex-direction: column;
    gap: 4px;
  }
}
</style>