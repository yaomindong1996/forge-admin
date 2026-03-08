import { defineStore } from 'pinia'
import { usePermissionStore, useRouterStore, useTabStore, useUserStore } from '@/store'
import { resetKeyExchange } from '@/utils/crypto'
import { disconnectWebSocketClient } from '@/utils/websocket'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: undefined,
    userInfo: null,
    staffInfo: null,
  }),
  getters: {
    // 获取认证header
    getAuthHeaders: (state) => {
      const headers = {
      }

      // 如果有access token，则添加到Authorization header
      if (state.accessToken) {
        headers['Authorization'] = `Bearer ${state.accessToken}`
      }

      return headers
    }
  },
  actions: {
    // 设置token和用户信息（适配新的返回结构）
    setToken(data) {
      if (data ) {
        this.accessToken = data.accessToken
      }
      // 兼容旧的结
    },
    resetToken() {
      this.$reset()
    },
    toLogin() {
      const { router, route } = useRouterStore()
      router.replace({
        path: '/login',
        query: {...route.query,redirect:route.path},
      })
    },
    async switchCurrentRole(data) {
      this.resetLoginState()
      await nextTick()
      this.setToken(data)
    },
    resetLoginState() {
      const { resetUser } = useUserStore()
      const { resetRouter } = useRouterStore()
      const { resetPermission, accessRoutes } = usePermissionStore()
      const { resetTabs } = useTabStore()
      // 重置路由
      resetRouter(accessRoutes)
      // 重置用户
      resetUser()
      // 重置权限
      resetPermission()
      // 重置Tabs
      resetTabs()
      // 重置WebSocket连接
      disconnectWebSocketClient()
      // 重置token
      this.resetToken()
      // 重置密钥交换状态
      resetKeyExchange()
    },
    async logout() {
      this.resetLoginState()
      this.toLogin()
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_auth`,
  },
})
