<template>
  <div class="notification-enhancer">
    <!-- 通知入口按钮 -->
    <n-badge :value="unreadCount" :max="99">
      <n-button
        circle
        quaternary
        @click="showNotificationPanel = true"
        class="notification-trigger"
      >
        <template #icon>
          <i class="i-material-symbols:notifications" />
        </template>
      </n-button>
    </n-badge>

    <!-- 通知面板 -->
    <n-drawer
      v-model:show="showNotificationPanel"
      placement="right"
      width="400"
      :on-after-leave="handleClose"
    >
      <n-drawer-content title="消息通知" closable>
        <div class="notification-panel">
          <!-- 筛选和操作 -->
          <div class="notification-actions">
            <n-tabs
              v-model:value="activeTab"
              type="segment"
              size="small"
            >
              <n-tab-pane name="all" tab="全部" />
              <n-tab-pane name="unread" tab="未读" />
              <n-tab-pane name="system" tab="系统" />
            </n-tabs>
            
            <div class="action-buttons">
              <n-button
                v-if="hasUnread"
                text
                size="small"
                @click="markAllRead"
              >
                <i class="i-material-symbols:mark-email-read" />
                全部已读
              </n-button>
              
              <n-button
                text
                size="small"
                @click="clearAll"
              >
                <i class="i-material-symbols:delete" />
                清空
              </n-button>
            </div>
          </div>

          <!-- 通知列表 -->
          <div class="notification-list">
            <!-- 加载状态 -->
            <div v-if="loading" class="notification-loading">
              <n-spin size="medium" />
              <span>加载中...</span>
            </div>
            
            <!-- 空状态 -->
            <div v-else-if="filteredNotifications.length === 0" class="notification-empty">
              <i class="i-material-symbols:inbox" />
              <span>暂无消息</span>
            </div>
            
            <!-- 通知项 -->
            <div v-else class="notification-items">
              <div
                v-for="notification in filteredNotifications"
                :key="notification.id"
                class="notification-item"
                :class="{
                  'item-unread': !notification.read,
                  'item-important': notification.type === 'important',
                  'item-system': notification.type === 'system'
                }"
                @click="handleNotificationClick(notification)"
              >
                <!-- 通知图标 -->
                <div class="notification-icon" :class="`icon-${notification.type}`">
                  <i :class="getNotificationIcon(notification.type)" />
                </div>
                
                <!-- 通知内容 -->
                <div class="notification-content">
                  <div class="notification-header">
                    <span class="notification-title">{{ notification.title }}</span>
                    <span class="notification-time">{{ formatTime(notification.time) }}</span>
                  </div>
                  
                  <div class="notification-body">
                    <span class="notification-message">{{ notification.message }}</span>
                  </div>
                </div>
                
                <!-- 未读标记 -->
                <div v-if="!notification.read" class="notification-dot" />
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="total > pageSize" class="notification-pagination">
            <n-pagination
              v-model:page="currentPage"
              :page-count="Math.ceil(total / pageSize)"
              size="small"
            />
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'naive-ui'

const props = defineProps({
  // 通知列表
  notifications: {
    type: Array,
    default: () => []
  },
  // 每页数量
  pageSize: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['read', 'clear', 'click'])

const showNotificationPanel = ref(false)
const activeTab = ref('all')
const currentPage = ref(1)
const loading = ref(false)

// 未读数量
const unreadCount = computed(() => {
  return props.notifications.filter(n => !n.read).length
})

// 是否有未读消息
const hasUnread = computed(() => unreadCount.value > 0)

// 过滤后的通知
const filteredNotifications = computed(() => {
  let filtered = [...props.notifications]
  
  // 按类型过滤
  if (activeTab.value === 'unread') {
    filtered = filtered.filter(n => !n.read)
  } else if (activeTab.value === 'system') {
    filtered = filtered.filter(n => n.type === 'system')
  }
  
  // 按时间排序（最新的在前）
  filtered.sort((a, b) => b.time - a.time)
  
  // 分页
  const start = (currentPage.value - 1) * props.pageSize
  const end = start + props.pageSize
  return filtered.slice(start, end)
})

// 总数
const total = computed(() => {
  if (activeTab.value === 'unread') {
    return props.notifications.filter(n => !n.read).length
  } else if (activeTab.value === 'system') {
    return props.notifications.filter(n => n.type === 'system').length
  }
  return props.notifications.length
})

// 获取通知图标
function getNotificationIcon(type) {
  const icons = {
    info: 'i-material-symbols:info',
    success: 'i-material-symbols:check-circle',
    warning: 'i-material-symbols:warning',
    error: 'i-material-symbols:error',
    important: 'i-material-symbols:priority-high',
    system: 'i-material-symbols:settings'
  }
  return icons[type] || icons.info
}

// 格式化时间
function formatTime(timestamp) {
  const diff = Date.now() - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 处理通知点击
function handleNotificationClick(notification) {
  // 标记为已读
  if (!notification.read) {
    emit('read', notification.id)
    notification.read = true
  }
  
  emit('click', notification)
}

// 标记全部已读
function markAllRead() {
  props.notifications.forEach(n => {
    if (!n.read) {
      emit('read', n.id)
      n.read = true
    }
  })
  message.success('已全部标记为已读')
}

// 清空全部
function clearAll() {
  emit('clear')
  message.success('通知已清空')
}

// 处理关闭
function handleClose() {
  currentPage.value = 1
}

// 暴露方法
defineExpose({
  addNotification: (notification) => {
    props.notifications.unshift({
      id: Date.now(),
      read: false,
      time: Date.now(),
      ...notification
    })
  },
  markAsRead: (id) => {
    const notification = props.notifications.find(n => n.id === id)
    if (notification) {
      notification.read = true
    }
  },
  clearAll,
  unreadCount
})
</script>

<style scoped>
.notification-enhancer {
  display: inline-block;
}

.notification-trigger {
  font-size: 20px;
  transition: all var(--transition);
}

.notification-trigger:hover {
  background: var(--bg-tertiary);
}

/* 通知面板 */
.notification-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.notification-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.action-buttons {
  display: flex;
  gap: var(--space-2);
}

/* 通知列表 */
.notification-list {
  flex: 1;
  overflow-y: auto;
}

.notification-loading,
.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-20) var(--space-4);
  color: var(--text-tertiary);
}

.notification-loading i,
.notification-empty i {
  font-size: 48px;
  margin-bottom: var(--space-3);
  opacity: 0.5;
}

.notification-items {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.notification-item {
  position: relative;
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
  background: var(--bg-secondary);
  border: 1px solid transparent;
}

.notification-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--border-strong);
  transform: translateX(4px);
}

.item-unread {
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--bg-secondary) 100%);
}

.item-important {
  border: 1px solid var(--warning-200);
}

.item-system {
  background: var(--bg-tertiary);
}

.notification-icon {
  width: 40px;
  height: 40px;
  min-width: 40px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.notification-icon.icon-info {
  background: var(--info-100);
  color: var(--info-600);
}

.notification-icon.icon-success {
  background: var(--success-100);
  color: var(--success-600);
}

.notification-icon.icon-warning {
  background: var(--warning-100);
  color: var(--warning-600);
}

.notification-icon.icon-error {
  background: var(--error-100);
  color: var(--error-600);
}

.notification-icon.icon-important {
  background: var(--error-100);
  color: var(--error-600);
}

.notification-icon.icon-system {
  background: var(--gray-200);
  color: var(--gray-600);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.notification-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  flex-shrink: 0;
  margin-left: var(--space-2);
}

.notification-body {
  margin-bottom: var(--space-2);
}

.notification-message {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-dot {
  position: absolute;
  top: var(--space-4);
  right: var(--space-4);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-500);
  box-shadow: 0 0 0 2px var(--bg-secondary);
}

/* 分页 */
.notification-pagination {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-default);
  display: flex;
  justify-content: center;
}

/* 暗色模式适配 */
.dark .notification-icon.icon-info {
  background: var(--info-900);
  color: var(--info-100);
}

.dark .notification-icon.icon-success {
  background: var(--success-900);
  color: var(--success-100);
}

.dark .notification-icon.icon-warning {
  background: var(--warning-900);
  color: var(--warning-100);
}

.dark .notification-icon.icon-error {
  background: var(--error-900);
  color: var(--error-100);
}

.dark .notification-icon.icon-important {
  background: var(--error-900);
  color: var(--error-100);
}

.dark .notification-icon.icon-system {
  background: var(--gray-700);
  color: var(--gray-300);
}

.dark .item-unread {
  background: linear-gradient(135deg, var(--primary-900) 0%, var(--bg-secondary) 100%);
}

.dark .item-important {
  border-color: var(--warning-700);
}
</style>