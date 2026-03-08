<template>
  <div class="menu-search-wrapper">
    <n-tooltip placement="bottom" trigger="hover">
      <template #trigger>
        <div class="search-trigger" @click="handleOpen">
          <i class="i-mdi-magnify text-20" />
          <span class="search-text">搜索菜单</span>
          <span class="search-shortcut">Ctrl K</span>
        </div>
      </template>
      <span>搜索菜单 (Ctrl + K)</span>
    </n-tooltip>

    <!-- 搜索弹窗 -->
    <n-modal
      v-model:show="showModal"
      :show-icon="false"
      :mask-closable="true"
      preset="dialog"
      class="menu-search-modal"
      :style="{ width: '600px', padding: 0 }"
      :closable="false"
    >
      <div class="search-container">
        <!-- 搜索输入框 -->
        <div class="search-input-wrapper">
          <i class="i-mdi-magnify search-icon" />
          <input
            ref="inputRef"
            v-model="searchKeyword"
            type="text"
            class="search-input"
            placeholder="搜索菜单名称、路径..."
            @keydown.up.prevent="handleKeyUp"
            @keydown.down.prevent="handleKeyDown"
            @keydown.enter.prevent="handleEnter"
            @keydown.esc="handleClose"
          />
          <span class="esc-hint">ESC</span>
        </div>

        <!-- 搜索结果列表 -->
        <div v-if="filteredMenus.length > 0" class="search-results">
          <div class="results-header">
            <span class="results-count">找到 {{ filteredMenus.length }} 个结果</span>
          </div>
          <div ref="listRef" class="results-list">
            <div
              v-for="(item, index) in filteredMenus"
              :key="item.path"
              class="result-item"
              :class="{ active: selectedIndex === index }"
              @click="handleSelect(item)"
              @mouseenter="selectedIndex = index"
            >
              <div class="item-icon">
                <i :class="item.icon || 'i-mdi-file-outline'" />
              </div>
              <div class="item-content">
                <div class="item-title" v-html="highlightText(item.title)" />
                <div class="item-path">{{ item.path }}</div>
              </div>
              <div class="item-badge">
                <i class="i-mdi-arrow-top-left" />
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="searchKeyword" class="search-empty">
          <i class="i-mdi-magnify-close empty-icon" />
          <p class="empty-text">未找到匹配的菜单</p>
          <p class="empty-hint">请尝试其他关键词</p>
        </div>

        <!-- 最近访问 -->
        <div v-else class="recent-visits">
          <div class="recent-header">
            <i class="i-mdi-history" />
            <span>最近访问</span>
          </div>
          <div class="recent-list">
            <div
              v-for="item in recentMenus"
              :key="item.path"
              class="recent-item"
              @click="handleSelect(item)"
            >
              <div class="item-icon">
                <i :class="item.icon || 'i-mdi-file-outline'" />
              </div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部提示 -->
        <div class="search-footer">
          <div class="footer-item">
            <span class="key"><i class="i-mdi-arrow-up" /></span>
            <span class="key"><i class="i-mdi-arrow-down" /></span>
            <span>选择</span>
          </div>
          <div class="footer-item">
            <span class="key">↵</span>
            <span>确认</span>
          </div>
          <div class="footer-item">
            <span class="key">esc</span>
            <span>关闭</span>
          </div>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePermissionStore } from '@/store'

const router = useRouter()
const permissionStore = usePermissionStore()

// 弹窗显示状态
const showModal = ref(false)
// 搜索关键词
const searchKeyword = ref('')
// 选中的索引
const selectedIndex = ref(0)
// 输入框引用
const inputRef = ref(null)
// 列表引用
const listRef = ref(null)
// 最近访问的菜单（从 localStorage 读取）
const recentMenus = ref([])

// 扁平化所有菜单
const flatMenus = computed(() => {
  const menus = permissionStore.menus || []
  console.log('MenuSearch - 原始菜单数据:', menus)
  const result = []

  const flatten = (items, parentName = '') => {
    if (!Array.isArray(items)) return
    items.forEach(item => {
      // 获取菜单名称（兼容 name/label/title 字段）
      const menuName = item.name || item.label || item.title || item.meta?.title || ''
      const menuPath = item.path || ''
      // 只添加有路径的菜单项
      if (menuPath && menuPath !== '/') {
        result.push({
          ...item,
          title: menuName,
          path: menuPath,
          fullTitle: parentName ? `${parentName} / ${menuName}` : menuName
        })
      }
      if (item.children && item.children.length > 0) {
        flatten(item.children, menuName)
      }
    })
  }

  flatten(menus)
  console.log('MenuSearch - 扁平化后的菜单:', result)
  return result
})

// 过滤后的菜单
const filteredMenus = computed(() => {
  if (!searchKeyword.value.trim()) {
    return []
  }

  const keyword = searchKeyword.value.toLowerCase().trim()
  return flatMenus.value.filter(item => {
    const titleMatch = item.title?.toLowerCase().includes(keyword)
    const pathMatch = item.path?.toLowerCase().includes(keyword)
    return titleMatch || pathMatch
  })
})

// 高亮匹配文本
const highlightText = (text) => {
  if (!searchKeyword.value.trim() || !text) return text
  const keyword = searchKeyword.value.trim()
  const regex = new RegExp(`(${keyword})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

// 打开弹窗
const handleOpen = () => {
  showModal.value = true
  searchKeyword.value = ''
  selectedIndex.value = 0
  loadRecentMenus()
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// 关闭弹窗
const handleClose = () => {
  showModal.value = false
}

// 选择菜单
const handleSelect = (item) => {
  if (!item || !item.path) return

  // 保存到最近访问
  saveRecentMenu(item)

  // 跳转路由
  router.push(item.path)
  handleClose()
}

// 键盘向上
const handleKeyUp = () => {
  if (filteredMenus.value.length === 0) return
  selectedIndex.value = (selectedIndex.value - 1 + filteredMenus.value.length) % filteredMenus.value.length
  scrollToSelected()
}

// 键盘向下
const handleKeyDown = () => {
  if (filteredMenus.value.length === 0) return
  selectedIndex.value = (selectedIndex.value + 1) % filteredMenus.value.length
  scrollToSelected()
}

// 回车确认
const handleEnter = () => {
  if (filteredMenus.value.length > 0 && selectedIndex.value >= 0) {
    handleSelect(filteredMenus.value[selectedIndex.value])
  }
}

// 滚动到选中项
const scrollToSelected = () => {
  nextTick(() => {
    const list = listRef.value
    if (!list) return

    const selectedItem = list.children[selectedIndex.value]
    if (selectedItem) {
      selectedItem.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  })
}

// 加载最近访问
const loadRecentMenus = () => {
  try {
    const stored = localStorage.getItem('recentMenus')
    if (stored) {
      recentMenus.value = JSON.parse(stored).slice(0, 6)
    }
  } catch (e) {
    console.error('加载最近访问失败:', e)
    recentMenus.value = []
  }
}

// 保存到最近访问
const saveRecentMenu = (item) => {
  try {
    const stored = localStorage.getItem('recentMenus')
    let recent = stored ? JSON.parse(stored) : []

    // 移除已存在的相同项
    recent = recent.filter(m => m.path !== item.path)

    // 添加到开头
    recent.unshift({
      path: item.path,
      title: item.title,
      icon: item.icon
    })

    // 只保留最近10个
    recent = recent.slice(0, 10)

    localStorage.setItem('recentMenus', JSON.stringify(recent))
  } catch (e) {
    console.error('保存最近访问失败:', e)
  }
}

// 监听搜索结果变化，重置选中索引
watch(filteredMenus, () => {
  selectedIndex.value = 0
})

// 全局快捷键
const handleKeydown = (e) => {
  // Ctrl/Cmd + K 打开搜索
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    if (!showModal.value) {
      handleOpen()
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  loadRecentMenus()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.menu-search-wrapper {
  display: flex;
  align-items: center;
}

/* 搜索触发按钮 */
.search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-color-secondary);
  font-size: 14px;
}

.search-trigger:hover {
  background: rgba(0, 0, 0, 0.08);
  color: var(--text-color);
}

.search-text {
  font-size: 13px;
}

.search-shortcut {
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 11px;
  font-weight: 500;
  color: var(--text-color-secondary);
}

/* 弹窗样式 */
:deep(.menu-search-modal) {
  .n-dialog__content {
    padding: 0 !important;
  }
}

.search-container {
  display: flex;
  flex-direction: column;
  max-height: 500px;
}

/* 搜索输入框 */
.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.search-icon {
  font-size: 20px;
  color: var(--text-color-secondary);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--text-color);
}

.search-input::placeholder {
  color: var(--text-color-placeholder);
}

.esc-hint {
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 12px;
  color: var(--text-color-secondary);
}

/* 搜索结果 */
.search-results {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.results-header {
  padding: 8px 20px;
  font-size: 12px;
  color: var(--text-color-secondary);
  border-bottom: 1px solid var(--border-color-light);
}

.results-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  max-height: 300px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.result-item:hover,
.result-item.active {
  background: rgba(var(--primary-color-rgb), 0.1);
}

.result-item.active {
  background: rgba(var(--primary-color-rgb), 0.15);
}

.item-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  font-size: 18px;
  color: var(--text-color-secondary);
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color);
  margin-bottom: 2px;
}

.item-title :deep(mark) {
  background: rgba(var(--primary-color-rgb), 0.2);
  color: var(--primary-color);
  padding: 0 2px;
  border-radius: 2px;
}

.item-path {
  font-size: 12px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-badge {
  opacity: 0;
  transition: opacity 0.2s ease;
  font-size: 14px;
  color: var(--text-color-secondary);
}

.result-item:hover .item-badge,
.result-item.active .item-badge {
  opacity: 1;
}

/* 空状态 */
.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--text-color-placeholder);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: var(--text-color);
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-color-secondary);
}

/* 最近访问 */
.recent-visits {
  padding: 16px 20px;
}

.recent-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-color-secondary);
  margin-bottom: 12px;
}

.recent-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
}

.recent-item:hover {
  background: rgba(var(--primary-color-rgb), 0.1);
}

/* 底部提示 */
.search-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-color-secondary);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 11px;
  font-weight: 500;
}

/* 滚动条样式 */
.results-list::-webkit-scrollbar {
  width: 6px;
}

.results-list::-webkit-scrollbar-track {
  background: transparent;
}

.results-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.results-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

/* 暗色模式适配 */
:deep(.dark) {
  .search-trigger {
    background: rgba(255, 255, 255, 0.08);
    color: rgba(255, 255, 255, 0.6);
  }

  .search-trigger:hover {
    background: rgba(255, 255, 255, 0.12);
    color: rgba(255, 255, 255, 0.9);
  }

  .search-shortcut {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.5);
  }

  .item-icon {
    background: rgba(255, 255, 255, 0.08);
  }

  .recent-item {
    background: rgba(255, 255, 255, 0.08);
  }

  .esc-hint,
  .key {
    background: rgba(255, 255, 255, 0.1);
  }
}
</style>
