import { defineStore } from 'pinia'
import { request } from '@/utils'

export const useDemoStore = defineStore('demo', {
  state: () => ({
    enabled: false,
    message: '',
    loaded: false,
  }),

  getters: {
    isDemo: state => state.enabled,
    demoMessage: state => state.message,
  },

  actions: {
    async loadDemoStatus() {
      if (this.loaded)
        return

      try {
        const { data } = await request.get('/system/demo/status')
        this.enabled = data.enabled || false
        this.message = data.message || ''
        this.loaded = true
      }
      catch (error) {
        console.error('获取演示环境状态失败', error)
        this.enabled = false
        this.loaded = true
      }
    },

    clearStatus() {
      this.enabled = false
      this.message = ''
      this.loaded = false
    },
  },
})
