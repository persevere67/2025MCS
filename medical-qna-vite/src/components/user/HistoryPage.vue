<template>
  <div class="history-wrapper">
    <h3>历史记录</h3>
    <ul>
      <li v-for="(item, index) in historyList" :key="index" class="history-item">
        <span @click="$emit('select', item)">{{ item.title }}</span>
        <button class="delete-btn" @click="$emit('delete', index)">×</button>
      </li>
    </ul>
  </div>
</template>


<script>
export default {
  name: 'HistoryPage',
  props: {
    historyList: {
      type: Array,
      required: true
    }
  }
}
</script>

<style scoped>
.history-wrapper {
  display: flex;
  width: 300px;
  border-right: 1px solid #ddd;
  padding: 10px;
  overflow-y: auto;
}
.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  cursor: pointer;
}
.delete-btn {
  background: none;
  border: none;
  color: red;
  cursor: pointer;
  font-size: 16px;
}
</style>
