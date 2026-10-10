import { defineStore } from 'pinia'
import { formatCcUnread, normalizeCcUnreadCount } from '../../utils/flow-cc.js'

// 抄送未读数只在待办页"抄送我的"页签展示，不计入底栏角标。
// 接口函数由调用方传入：避免 store 动态引入 '@/api'，Node 单测也能直接注入。
export const useCcStore = defineStore('cc', {
  state: () => ({
    unreadCount: 0,
    current: null,
    readIds: [],
  }),
  getters: {
    unreadText: state => formatCcUnread(state.unreadCount),
  },
  actions: {
    setCurrent(cc) {
      this.current = cc ? { ...cc } : null
    },
    async loadUnreadCount(request) {
      try {
        const response = await request()
        this.unreadCount = normalizeCcUnreadCount(response?.data)
      }
      catch (error) {
        console.error('加载抄送未读数失败:', error)
      }
      return this.unreadCount
    },
    async markRead(id, request) {
      const key = String(id ?? '')
      if (!key || this.readIds.includes(key)) return false
      await request(key)
      this.readIds.push(key)
      this.unreadCount = Math.max(0, this.unreadCount - 1)
      if (this.current && String(this.current.id) === key) this.current = { ...this.current, isRead: 1 }
      return true
    },
    async markAllRead(request) {
      await request()
      this.unreadCount = 0
      if (this.current) this.current = { ...this.current, isRead: 1 }
    },
  },
})
