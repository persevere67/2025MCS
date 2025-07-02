<template>
  <div class="history-wrapper">
    <h3>历史记录</h3>
    <ul>
      <li v-for="(item, index) in historyList" :key="index" class="history-item">
        <span class="history-title" @click="$emit('select', item)">
          {{ item.title }}
        </span>
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
  width: 200px;
  border-right: 1px solid #ddd;
  padding: 10px;
  overflow-y: auto;
  background-color: #fff;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 6px;
  border-radius: 5px;
  transition: background-color 0.3s ease;
  cursor: pointer;
}

.history-item:hover {
  background-color: #f0f0f0;
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.delete-btn {
  background: transparent;
  border: none;
  color: #000000;
  cursor: pointer;
  font-size: 18px;
  transition: transform 0.3s ease-in-out;
  outline: none;
}

.delete-btn:hover {
  transform: scale(1.3); 
  color: #000000;
}
</style>
