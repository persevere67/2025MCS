<template>
  <div class="history-wrapper">
    <div class="history-header">
      <h3>历史记录</h3>
      <button v-if="historyList.length > 0" @click="clearAllHistory" class="clear-all-btn">
        清空
      </button>
    </div>
    
    <div v-if="historyList.length === 0" class="empty-state">
      <div class="empty-icon">📝</div>
      <p>暂无历史记录</p>
    </div>
    
    <ul v-else class="history-list">
      <li v-for="(item, index) in historyList" :key="item.id || index" class="history-item">
        <div class="history-content" @click="$emit('select', item)">
          <div class="history-title" :title="item.title">
            {{ item.title }}
          </div>
          <div class="history-time" v-if="item.createTime">
            {{ formatTime(item.createTime) }}
          </div>
        </div>
        <button class="delete-btn" @click="$emit('delete', index)" :title="'删除: ' + item.title">
          ×
        </button>
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
  },
  emits: ['select', 'delete', 'clear-all'],
  methods: {
    formatTime(timeString) {
      try {
        const date = new Date(timeString);
        const now = new Date();
        const diff = now - date;
        
        // 小于1分钟
        if (diff < 60000) {
          return '刚刚';
        }
        // 小于1小时
        if (diff < 3600000) {
          return Math.floor(diff / 60000) + '分钟前';
        }
        // 小于1天
        if (diff < 86400000) {
          return Math.floor(diff / 3600000) + '小时前';
        }
        // 大于1天
        if (diff < 604800000) {
          return Math.floor(diff / 86400000) + '天前';
        }
        
        // 超过一周，显示具体日期
        return date.toLocaleDateString();
      } catch (error) {
        return '';
      }
    },
    
    clearAllHistory() {
      if (confirm('确定要清空所有历史记录吗？')) {
        this.$emit('clear-all');
      }
    }
  }
}
</script>

<style scoped>
.history-wrapper {
  width: 280px;
  border-right: 1px solid #e1e5e9;
  padding: 0;
  overflow-y: auto;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.history-header {
  padding: 20px;
  border-bottom: 1px solid #e1e5e9;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.history-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.clear-all-btn {
  background: #ff6b6b;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.clear-all-btn:hover {
  background: #ff5252;
  transform: translateY(-1px);
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #999;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.history-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: flex-start;
  margin: 0;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  cursor: pointer;
  background: white;
  position: relative;
}

.history-item:hover {
  background-color: #f0f7ff;
  transform: translateX(2px);
}

.history-content {
  flex: 1;
  overflow: hidden;
  margin-right: 8px;
}

.history-title {
  font-size: 14px;
  line-height: 1.4;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.history-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.delete-btn {
  background: transparent;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
  outline: none;
  padding: 2px 4px;
  border-radius: 4px;
  flex-shrink: 0;
  opacity: 0;
  transform: scale(0.8);
}

.history-item:hover .delete-btn {
  opacity: 1;
  transform: scale(1);
}

.delete-btn:hover {
  background: #ff6b6b;
  color: white;
  transform: scale(1.1);
}

/* 滚动条样式 */
.history-wrapper::-webkit-scrollbar {
  width: 6px;
}

.history-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.history-wrapper::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.history-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .history-wrapper {
    width: 250px;
  }
  
  .history-header {
    padding: 15px;
  }
  
  .history-item {
    padding: 10px 12px;
  }
}</style>