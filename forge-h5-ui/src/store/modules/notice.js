import { defineStore } from 'pinia'
import api from '@/api'
import { normalizeNoticePage } from '@/utils/notice'
import { useBadgeStore } from './badge'

// 公告列表、详情、消息页入口和工作台公告条共用：最新一条公告 + 本次会话已读的公告 ID。
export const useNoticeStore = defineStore('notice', {
  state: () => ({
    latest: null,
    readIds: [],
  }),
  actions: {
    async loadLatest() {
      try {
        const response = await api.getNoticePage({ pageNum: 1, pageSize: 1 })
        this.latest = normalizeNoticePage(response?.data).records[0] || null
      }
      catch (error) {
        console.error('加载最新公告失败:', error)
      }
      return this.latest
    },
    async markRead(noticeId) {
      const id = String(noticeId ?? '')
      if (!id || this.readIds.includes(id)) return
      await api.markNoticeRead(id)
      this.readIds.push(id)
      const badgeStore = useBadgeStore()
      badgeStore.setNoticeUnreadCount(badgeStore.noticeUnreadCount - 1)
    },
  },
})
