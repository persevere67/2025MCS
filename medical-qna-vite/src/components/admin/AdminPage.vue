<template>
  <div class="admin-wrapper">
    <h2>问答系统 - 管理员界面</h2>

    <!-- 查询输入框 -->
    <div class="search-section">
      <input v-model="searchKeyword" placeholder="请输入关键词搜索" @input="filterList" />
      <button @click="resetSearch">重置</button>
    </div>

    <!-- 数据表格 -->
    <table class="qa-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>问题</th>
          <th>答案</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>
            <input v-if="editId === item.id" v-model="editQuestion" />
            <span v-else>{{ item.question }}</span>
          </td>
          <td>
            <input v-if="editId === item.id" v-model="editAnswer" />
            <span v-else>{{ item.answer }}</span>
          </td>
          <td>
            <button v-if="editId !== item.id" @click="editRecord(item)">编辑</button>
            <button v-if="editId === item.id" @click="saveRecord(item)">保存</button>
            <button @click="deleteRecord(item.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// 模拟历史记录数据
const historyList = ref([
  { id: 1, question: '什么是Vue3？', answer: 'Vue3是前端框架。' },
  { id: 2, question: 'JavaScript用途？', answer: '前端、后端、全栈开发。' },
])

const searchKeyword = ref('')
const filteredList = ref([...historyList.value])

const newQuestion = ref('')
const newAnswer = ref('')

const editId = ref(null)
const editQuestion = ref('')
const editAnswer = ref('')

// 过滤功能
const filterList = () => {
  if (!searchKeyword.value.trim()) {
    filteredList.value = [...historyList.value]
  } else {
    filteredList.value = historyList.value.filter(
      (item) =>
        item.question.includes(searchKeyword.value) ||
        item.answer.includes(searchKeyword.value)
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

const saveRecord = (item) => {
  const index = historyList.value.findIndex((i) => i.id === item.id)
  if (index !== -1) {
    historyList.value[index].question = editQuestion.value
    historyList.value[index].answer = editAnswer.value
  }
  editId.value = null
  filterList()
}

// 删除功能
const deleteRecord = (id) => {
  historyList.value = historyList.value.filter((item) => item.id !== id)
  filterList()
}
</script>

<style scoped>
.admin-wrapper {
  width: 800px;
  margin: 20px auto;
  font-family: Arial, sans-serif;
}

.search-section,
.add-section {
  margin-bottom: 20px;
}

input {
  margin: 0 8px 8px 0;
  padding: 6px;
}

button {
  margin-right: 8px;
  padding: 6px 12px;
}

.qa-table {
  width: 100%;
  border-collapse: collapse;
}

.qa-table th,
.qa-table td {
  border: 1px solid #ccc;
  padding: 8px;
  text-align: center;
}
</style>
