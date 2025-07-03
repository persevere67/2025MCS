<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>问答系统管理后台</h2>
      <p class="admin-subtitle">管理问答数据记录</p>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section card">
      <div class="search-input-group">
        <input 
          v-model="searchKeyword" 
          placeholder="输入关键词搜索问题或答案..." 
          @input="filterList"
          class="search-input"
        />
        <button @click="resetSearch" class="btn btn-secondary">
          <i class="icon-reset"></i> 重置
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container card">
      <table class="qa-table">
        <thead>
          <tr>
            <th width="80">ID</th>
            <th>问题</th>
            <th>答案</th>
            <th width="180">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredList" :key="item.id">
            <td>{{ item.id }}</td>
            <td>
              <input 
                v-if="editId === item.id" 
                v-model="editQuestion" 
                class="edit-input"
              />
              <span v-else>{{ item.question }}</span>
            </td>
            <td>
              <input 
                v-if="editId === item.id" 
                v-model="editAnswer" 
                class="edit-input"
              />
              <span v-else>{{ item.answer }}</span>
            </td>
            <td class="actions">
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
      
      <!-- 空状态 -->
      <div v-if="filteredList.length === 0" class="empty-state">
        <i class="icon-empty"></i>
        <p>暂无数据</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 模拟数据 - 实际应用中应该从API获取
const historyList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const filteredList = ref([])

const editId = ref(null)
const editQuestion = ref('')
const editAnswer = ref('')

// 初始化加载数据
onMounted(async () => {
  await fetchData()
})

// 从API获取数据
const fetchData = async () => {
  try {
    loading.value = true
    // 这里替换为实际的API调用
    // const response = await fetch('/api/qa-records')
    // const data = await response.json()
    // historyList.value = data
    // filteredList.value = data
    
    // 模拟API延迟
    await new Promise(resolve => setTimeout(resolve, 800))
    
    // 模拟数据
    historyList.value = [
      { id: 1, question: '什么是Vue3？', answer: 'Vue3是前端框架。' },
      { id: 2, question: 'JavaScript用途？', answer: '前端、后端、全栈开发。' },
      { id: 3, question: 'React和Vue的区别？', answer: 'React使用JSX，Vue使用模板语法。' },
      { id: 4, question: '什么是响应式编程？', answer: '数据变化自动更新UI的编程范式。' }
    ]
    filteredList.value = [...historyList.value]
  } catch (error) {
    console.error('获取数据失败:', error)
    // 这里可以添加错误提示
  } finally {
    loading.value = false
  }
}

// 过滤功能
const filterList = () => {
  if (!searchKeyword.value.trim()) {
    filteredList.value = [...historyList.value]
  } else {
    const keyword = searchKeyword.value.toLowerCase()
    filteredList.value = historyList.value.filter(
      (item) =>
        item.question.toLowerCase().includes(keyword) ||
        item.answer.toLowerCase().includes(keyword)
    )
  }
}

const resetSearch = () => {
  searchKeyword.value = ''
  filterList()
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
    // 这里替换为实际的API调用
    // const response = await fetch(`/api/qa-records/${item.id}`, {
    //   method: 'PUT',
    //   headers: {
    //     'Content-Type': 'application/json'
    //   },
    //   body: JSON.stringify({
    //     question: editQuestion.value,
    //     answer: editAnswer.value
    //   })
    // })
    
    // 模拟API延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const index = historyList.value.findIndex((i) => i.id === item.id)
    if (index !== -1) {
      historyList.value[index].question = editQuestion.value
      historyList.value[index].answer = editAnswer.value
    }
    editId.value = null
    filterList()
  } catch (error) {
    console.error('保存失败:', error)
    // 这里可以添加错误提示
  } finally {
    loading.value = false
  }
}

// 删除记录
const deleteRecord = async (id) => {
  if (!confirm('确定要删除这条记录吗？')) return
  
  try {
    loading.value = true
    // 这里替换为实际的API调用
    // await fetch(`/api/qa-records/${id}`, {
    //   method: 'DELETE'
    // })
    
    // 模拟API延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    
    historyList.value = historyList.value.filter((item) => item.id !== id)
    filterList()
  } catch (error) {
    console.error('删除失败:', error)
    // 这里可以添加错误提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #333;
}

.admin-header {
  margin-bottom: 30px;
  text-align: center;
}

.admin-header h2 {
  color: #2c3e50;
  font-size: 28px;
  margin-bottom: 8px;
}

.admin-subtitle {
  color: #7f8c8d;
  font-size: 16px;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
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
  border-collapse: collapse;
}

.qa-table th {
  background-color: #f8f9fa;
  color: #495057;
  font-weight: 600;
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
  background-color: #f8f9fa;
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

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 图标样式 - 可以使用实际图标库如Font Awesome */
.icon-reset::before { content: "↻"; }
.icon-edit::before { content: "✎"; }
.icon-save::before { content: "✓"; }
.icon-delete::before { content: "✕"; }
.icon-empty::before { content: "☹"; }
</style>