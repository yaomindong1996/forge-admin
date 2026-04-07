import { defineStore } from 'pinia'
import api from '@/api'

export const useTenantStore = defineStore('tenant', {
  state: () => ({
    config: null, // 租户配置
  }),

  getters: {
    // 系统名称
    systemName: (state) => state.config?.systemName || null,

    // 系统Logo
    systemLogo: (state) => state.config?.systemLogo || null,

    // 浏览器标题
    browserTitle: (state) => state.config?.browserTitle || null,

    // 浏览器图标
    browserIcon: (state) => state.config?.browserIcon || null,

    // 系统布局
    systemLayout: (state) => state.config?.systemLayout || null,

    // 系统主题
    systemTheme: (state) => state.config?.systemTheme || null,

    // 系统介绍
    systemIntro: (state) => state.config?.systemIntro || null,

    // 版权信息
    copyrightInfo: (state) => state.config?.copyrightInfo || null,

    // 主题配置（JSON对象）
    themeConfig: (state) => {
      if (!state.config?.themeConfig) return null
      try {
        return typeof state.config.themeConfig === 'string'
          ? JSON.parse(state.config.themeConfig)
          : state.config.themeConfig
      } catch (error) {
        console.error('解析主题配置失败:', error)
        return null
      }
    },
  },

  actions: {
    // 加载租户配置
    async loadTenantConfig(tenantId) {
      try {
        // 如果没有传入租户ID，尝试从用户信息中获取
        if (!tenantId) {
          const { useUserStore } = await import('@/store')
          const userStore = useUserStore()
          tenantId = userStore.userInfo?.tenantId
        }

        const res = await api.getTenantConfig(tenantId)
        if (res.code === 200 && res.data) {
          this.config = res.data
          return res.data
        }
        return null
      } catch (error) {
        console.error('加载租户配置失败:', error)
        return null
      }
    },

    // 清空租户配置
    clearTenantConfig() {
      this.config = null
    },
  },

  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_tenant`,
    pick: ['config'],
    storage: sessionStorage,
  },
})
