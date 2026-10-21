import { defineStore } from 'pinia'
import { useAppStore, usePermissionStore, useRouterStore, useTabStore, useTenantStore, useUserStore } from '@/store'
import { resetKeyExchange } from '@/utils/crypto'
import { resetSessionKeepalive } from '@/utils/http/session-keepalive'
import { lStorage } from '@/utils/storage'
import { disconnectWebSocketClient } from '@/utils/websocket'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: undefined,
    userInfo: null,
    staffInfo: null,
    isLoggingOut: false,
  }),
  getters: {
    // 获取认证header
    getAuthHeaders: (state) => {
      const headers = {
      }

      // 如果有access token，则添加到Authorization header
      if (state.accessToken) {
        headers.Authorization = `Bearer ${state.accessToken}`
      }

      return headers
    },
  },
  actions: {
    // 设置token和用户信息（适配新的返回结构）
    setToken(data) {
      this.isLoggingOut = false
      const token = data?.accessToken || data?.token
      if (token) {
        this.accessToken = token
      }
      // 兼容旧的结
    },
    resetToken() {
      const keepLoggingOut = this.isLoggingOut
      this.$reset()
      this.isLoggingOut = keepLoggingOut
      resetSessionKeepalive()
    },
    beginLogout() {
      this.isLoggingOut = true
    },
    finishLogoutSilence() {
      this.isLoggingOut = false
    },
    toLogin() {
      const { router, route } = useRouterStore()
      router.replace({
        path: '/login',
        query: { ...route.query, redirect: route.path },
      })
    },
    async switchCurrentRole(data) {
      this.resetLoginState()
      await nextTick()
      this.setToken(data)
    },
    resetLoginState(options = {}) {
      const { resetAuth = true } = options
      const appStore = useAppStore()
      const { resetUser } = useUserStore()
      const { resetRouter } = useRouterStore()
      const { resetPermission, accessRoutes } = usePermissionStore()
      const { resetTabs } = useTabStore()
      const { clearTenantConfig } = useTenantStore()
      // 重置路由
      resetRouter(accessRoutes)
      // 重置用户
      resetUser()
      // 重置权限
      resetPermission()
      // 重置Tabs
      resetTabs()
      // 重置租户配置和账号相关的布局状态
      clearTenantConfig()
      // 登录成功（resetAuth=false）保留当前外观：登录页已按租户主题着色，
      // 若此处先 reset 成默认色，会在拉菜单/租户配置完成前闪一下系统蓝。
      appStore.resetAccountState({ preserveAppearance: !resetAuth })
      // 清理本地缓存的账号资料和数据权限
      lStorage.remove('userInfo')
      lStorage.remove('staffInfo')
      lStorage.remove('dataPermission')
      // 重置WebSocket连接
      disconnectWebSocketClient()
      // 退出登录时清 token；登录成功前只清账号态，避免持久化短暂写入空 token。
      if (resetAuth) {
        this.resetToken()
      }
      else {
        this.userInfo = null
        this.staffInfo = null
      }
      // 重置密钥交换状态
      resetKeyExchange()
      // 重新登录后由新账号菜单重新推导首页
      window.$homePath = import.meta.env.VITE_HOME_PATH
    },
    async logout() {
      this.beginLogout()
      this.resetLoginState()
      this.toLogin()
      window.setTimeout(() => {
        this.finishLogoutSilence()
      }, 1200)
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_auth`,
  },
})
