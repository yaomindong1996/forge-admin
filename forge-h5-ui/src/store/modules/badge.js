import { defineStore } from 'pinia'

export function toBadgeCount(value) {
  const count = Number(value)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

export function formatBadgeCount(value) {
  const count = toBadgeCount(value)
  if (!count) return ''
  return count > 99 ? '99+' : String(count)
}

export function normalizeUnreadCount(data) {
  if (typeof data === 'number') return data
  return Number(data?.totalCount || data?.unreadCount || data?.count || 0)
}

// 动态加载接口层，避免 Node 单测引入 '@/api' 的路径别名与 uni 全局对象。
async function defaultBadgeLoader() {
  const [{ default: api }, { useAuthStore }] = await Promise.all([import('@/api'), import('./auth')])
  const user = useAuthStore().userInfo || {}
  const userId = user.id || user.userId || user.user_id
  const [todo, unread, notice] = await Promise.allSettled([
    userId ? api.getTodoTasks({ pageNum: 1, pageSize: 1, userId }) : Promise.resolve({ data: { total: 0 } }),
    api.getUnreadMessageCount(),
    api.getNoticeUnreadCount(),
  ])
  return {
    todoCount: todo.status === 'fulfilled' ? todo.value?.data?.total : undefined,
    unreadCount: unread.status === 'fulfilled' ? normalizeUnreadCount(unread.value?.data) : undefined,
    noticeUnreadCount: notice.status === 'fulfilled' ? normalizeUnreadCount(notice.value?.data) : undefined,
  }
}

let pendingRefresh = null

export const useBadgeStore = defineStore('badge', {
  state: () => ({
    todoCount: 0,
    unreadCount: 0,
    noticeUnreadCount: 0,
  }),
  getters: {
    todoText: state => formatBadgeCount(state.todoCount),
    unreadText: state => formatBadgeCount(state.unreadCount),
    // "消息"页签角标 = 站内消息未读 + 公告未读；工作台概览只展示站内消息数。
    messageTabText: state => formatBadgeCount(state.unreadCount + state.noticeUnreadCount),
  },
  actions: {
    setTodoCount(value) {
      this.todoCount = toBadgeCount(value)
    },
    setUnreadCount(value) {
      this.unreadCount = toBadgeCount(value)
    },
    setNoticeUnreadCount(value) {
      this.noticeUnreadCount = toBadgeCount(value)
    },
    // 五个页签页 onShow 都会刷新；切页签时合并为一次请求，单项失败保留上次的值。
    refresh(loader = defaultBadgeLoader) {
      if (pendingRefresh) return pendingRefresh
      pendingRefresh = Promise.resolve()
        .then(loader)
        .then((result = {}) => {
          if (result.todoCount !== undefined) this.setTodoCount(result.todoCount)
          if (result.unreadCount !== undefined) this.setUnreadCount(result.unreadCount)
          if (result.noticeUnreadCount !== undefined) this.setNoticeUnreadCount(result.noticeUnreadCount)
        })
        .catch((error) => {
          console.error('刷新角标失败:', error)
        })
        .finally(() => {
          pendingRefresh = null
        })
      return pendingRefresh
    },
  },
})
