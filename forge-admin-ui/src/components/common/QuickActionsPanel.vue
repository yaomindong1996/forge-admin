<template>
  <div class="quick-actions-panel">
    <!-- 快捷操作按钮 -->
    <div class="quick-actions">
      <div
        v-for="action in filteredActions"
        :key="action.key"
        class="quick-action-item"
        :class="{ 'action-disabled': action.disabled }"
        @click="handleActionClick(action)"
      >
        <div class="action-icon" :style="{ background: action.color }">
          <i :class="action.icon" />
        </div>
        <div class="action-info">
          <span class="action-title">{{ action.title }}</span>
          <span v-if="action.shortcut" class="action-shortcut">{{ action.shortcut }}</span>
        </div>
        <n-badge
          v-if="action.badge"
          :value="action.badge"
          :max="99"
          class="action-badge"
        />
      </div>
    </div>

    <!-- 搜索框 -->
    <div v-if="showSearch" class="quick-search">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索操作..."
        clearable
        size="large"
        :input-props="{ spellcheck: false }"
      >
        <template #prefix>
          <i class="i-material-symbols:search" />
        </template>
      </n-input>
    </div>

    <!-- 操作历史 -->
    <div v-if="showHistory && history.length > 0" class="action-history">
      <div class="history-header">
        <span class="history-title">最近操作</span>
        <n-button text size="small" @click="clearHistory">
          清空
        </n-button>
      </div>
      <div class="history-list">
        <div
          v-for="(item, index) in history.slice(0, 5)"
          :key="index"
          class="history-item"
          @click="handleHistoryClick(item)"
        >
          <i :class="item.icon" class="history-icon" />
          <span class="history-text">{{ item.title }}</span>
          <span class="history-time">{{ formatTime(item.time) }}</span>
        </div>
      </div>
    </div>

    <!-- 快捷键提示 -->
    <div v-if="showShortcuts" class="shortcuts-hint">
      <div class="shortcuts-title">
        快捷键
      </div>
      <div class="shortcuts-list">
        <div
          v-for="shortcut in shortcutHints"
          :key="shortcut.key"
          class="shortcut-item"
        >
          <span class="shortcut-desc">{{ shortcut.desc }}</span>
          <div class="shortcut-keys">
            <kbd
              v-for="(key, idx) in shortcut.keys"
              :key="idx"
              class="shortcut-key"
            >
              {{ key }}
            </kbd>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  // 操作列表
  actions: {
    type: Array,
    default: () => [],
  },
  // 是否显示搜索
  showSearch: {
    type: Boolean,
    default: true,
  },
  // 是否显示历史记录
  showHistory: {
    type: Boolean,
    default: true,
  },
  // 是否显示快捷键提示
  showShortcuts: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['action', 'search'])

const router = useRouter()
const searchKeyword = ref('')
const history = ref([])

// 默认快捷键提示
const shortcutHints = ref([
  { desc: '搜索', keys: ['Ctrl', 'K'] },
  { desc: '操作面板', keys: ['Ctrl', '/'] },
  { desc: '新建', keys: ['Ctrl', 'N'] },
  { desc: '刷新', keys: ['Ctrl', 'R'] },
])

// 根据搜索关键词过滤操作
const filteredActions = computed(() => {
  if (!searchKeyword.value) {
    return props.actions
  }
  const keyword = searchKeyword.value.toLowerCase()
  return props.actions.filter(action =>
    action.title.toLowerCase().includes(keyword)
    || (action.description && action.description.toLowerCase().includes(keyword)),
  )
})

// 处理操作点击
function handleActionClick(action) {
  if (action.disabled)
    return

  // 添加到历史记录
  addToHistory(action)

  // 执行操作
  if (action.handler) {
    action.handler()
  }
  else if (action.path) {
    router.push(action.path)
  }

  emit('action', action)
}

// 处理历史记录点击
function handleHistoryClick(item) {
  if (item.path) {
    router.push(item.path)
  }
}

// 添加到历史记录
function addToHistory(action) {
  const exists = history.value.findIndex(h => h.key === action.key)
  if (exists !== -1) {
    history.value.splice(exists, 1)
  }
  history.value.unshift({
    key: action.key,
    title: action.title,
    icon: action.icon,
    path: action.path,
    time: Date.now(),
  })

  // 限制历史记录数量
  if (history.value.length > 10) {
    history.value.pop()
  }

  // 保存到localStorage
  saveHistory()
}

// 清空历史记录
function clearHistory() {
  history.value = []
  localStorage.removeItem('quick-actions-history')
}

// 格式化时间
function formatTime(timestamp) {
  const diff = Date.now() - timestamp
  const minutes = Math.floor(diff / 60000)

  if (minutes < 1)
    return '刚刚'
  if (minutes < 60)
    return `${minutes}分钟前`
  if (minutes < 1440)
    return `${Math.floor(minutes / 60)}小时前`
  return `${Math.floor(minutes / 1440)}天前`
}

// 保存历史记录
function saveHistory() {
  localStorage.setItem('quick-actions-history', JSON.stringify(history.value))
}

// 加载历史记录
function loadHistory() {
  const saved = localStorage.getItem('quick-actions-history')
  if (saved) {
    try {
      history.value = JSON.parse(saved)
    }
    catch (e) {
      console.error('Failed to load history:', e)
    }
  }
}

// 键盘快捷键处理
function handleKeydown(e) {
  // Ctrl + K: 聚焦搜索框
  if (e.ctrlKey && e.key === 'k') {
    e.preventDefault()
    const searchInput = document.querySelector('.quick-search input')
    if (searchInput) {
      searchInput.focus()
    }
  }

  // Ctrl + /: 显示/隐藏面板
  if (e.ctrlKey && e.key === '/') {
    e.preventDefault()
    togglePanel()
  }
}

// 切换面板显示（需要外部实现）
function togglePanel() {
  emit('toggle')
}

onMounted(() => {
  loadHistory()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// 暴露方法
defineExpose({
  addAction: (action) => {
    props.actions.push(action)
  },
  removeAction: (key) => {
    const index = props.actions.findIndex(a => a.key === key)
    if (index !== -1) {
      props.actions.splice(index, 1)
    }
  },
  clearHistory,
  focusSearch: () => {
    const searchInput = document.querySelector('.quick-search input')
    if (searchInput) {
      searchInput.focus()
    }
  },
})
</script>

<style scoped>
.quick-actions-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--bg-primary);
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
  background: var(--bg-secondary);
  border: 1px solid transparent;
}

.quick-action-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--border-strong);
  transform: translateX(4px);
}

.quick-action-item:active {
  transform: translateX(2px);
}

.action-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-disabled:hover {
  transform: none;
  background: var(--bg-secondary);
  border-color: transparent;
}

.action-icon {
  width: 40px;
  height: 40px;
  min-width: 40px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition);
}

.quick-action-item:hover .action-icon {
  transform: scale(1.1);
}

.action-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.action-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-shortcut {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.action-badge {
  flex-shrink: 0;
}

/* 搜索框 */
.quick-search {
  padding: var(--space-2) 0;
}

/* 操作历史 */
.action-history {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.history-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.history-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition);
}

.history-item:hover {
  background: var(--bg-tertiary);
  transform: translateX(4px);
}

.history-icon {
  font-size: 16px;
  color: var(--text-tertiary);
}

.history-text {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-time {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

/* 快捷键提示 */
.shortcuts-hint {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.shortcuts-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-3);
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.shortcut-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shortcut-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.shortcut-keys {
  display: flex;
  gap: var(--space-1);
}

.shortcut-key {
  padding: var(--space-1) var(--space-2);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-family: var(--font-family-mono);
  color: var(--text-secondary);
  box-shadow: var(--shadow-xs);
}

/* 暗色模式适配 */
.dark .shortcut-key {
  background: var(--gray-800);
  border-color: var(--gray-700);
}
</style>
