import { defineStore } from 'pinia'
import api from '@/api'
import { useAppStore } from './app'
import { loadRuntimeCryptoConfig } from '@/utils/crypto/crypto-config'
import { rsaEncrypt } from '@/utils/crypto/rsa'
import { hasPermission } from '@/utils/permission'

function getToken(data = {}) {
  return data.accessToken || data.token
}

function getDisplayName(userInfo) {
  const nested = userInfo?.userInfo
  return userInfo?.realName || userInfo?.nickName || userInfo?.username
    || nested?.realName || nested?.nickName || nested?.username || '用户'
}

const loginConfigCache = new Map()
const loginConfigRequests = new Map()
let passwordLoginPromise = null

function loginConfigKey(tenantId) {
  return `${import.meta.env.VITE_USER_CLIENT || 'h5'}:${tenantId == null || tenantId === '' ? 'default' : String(tenantId)}`
}

async function resolveLoginConfig(tenantId, force = false) {
  const key = loginConfigKey(tenantId)
  if (!force && loginConfigCache.has(key))
    return loginConfigCache.get(key)
  if (!force && loginConfigRequests.has(key))
    return loginConfigRequests.get(key)

  const request = api.getLoginConfig({
    userClient: import.meta.env.VITE_USER_CLIENT || 'h5',
    ...(tenantId ? { tenantId } : {}),
  }).then((response) => {
    const config = response?.data || null
    loginConfigCache.set(key, config)
    useAppStore().setBrandConfig(config)
    return config
  }).finally(() => {
    if (loginConfigRequests.get(key) === request)
      loginConfigRequests.delete(key)
  })
  loginConfigRequests.set(key, request)
  return request
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: '',
    tokenType: 'Bearer',
    expiresIn: null,
    userInfo: null,
    menus: [],
    permissions: [],
  }),
  getters: {
    isLogin: state => !!state.accessToken,
    displayName: state => getDisplayName(state.userInfo),
    roleText: state => {
      const roles = state.userInfo?.roleKeys || state.userInfo?.roles
        || state.userInfo?.userInfo?.roleKeys || state.userInfo?.userInfo?.roles || []
      return Array.isArray(roles) && roles.length ? roles.join(' / ') : '移动端用户'
    },
    avatar: state => state.userInfo?.avatar || state.userInfo?.userInfo?.avatar || state.userInfo?.staffInfo?.avatar || '',
    hasPermission: state => code => hasPermission(state.permissions, code),
  },
  actions: {
    setToken(data = {}) {
      const token = getToken(data)
      if (!token) {
        return
      }
      this.accessToken = token
      this.tokenType = data.tokenType || 'Bearer'
      this.expiresIn = data.expiresIn || null
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo || null
    },
    patchUserInfo(userInfo = {}) {
      this.userInfo = {
        ...(this.userInfo || {}),
        ...userInfo,
      }
    },
    setMenus(menus) {
      this.menus = Array.isArray(menus) ? menus : []
    },
    setPermissions(permissions) {
      this.permissions = Array.isArray(permissions) ? permissions : []
    },
    resetAuth() {
      this.accessToken = ''
      this.tokenType = 'Bearer'
      this.expiresIn = null
      this.userInfo = null
      this.menus = []
      this.permissions = []
      useAppStore().setBrandConfig(null)
    },
    async loadBrand(tenantId, force = false) {
      const appStore = useAppStore()
      try {
        return await resolveLoginConfig(tenantId, force)
      }
      catch (error) { console.warn('加载租户品牌配置失败:', error) }
      return appStore.brandConfig
    },
    async encryptPassword(password, enabled) {
      if (!enabled) {
        return password
      }
      try {
        const res = await api.getPublicKey()
        const publicKey = res?.data?.publicKey
        if (!publicKey) {
          throw new Error('未获取到密码加密公钥')
        }
        return rsaEncrypt(password, publicKey)
      }
      catch (error) {
        console.error('密码 RSA 加密失败:', error)
        throw new Error('密码加密服务暂不可用，请刷新后重试')
      }
    },
    async login(form) {
      if (passwordLoginPromise)
        return passwordLoginPromise

      // 复制表单，避免 single-flight 期间响应式数据被验证码刷新或工作区切换修改。
      const credentials = { ...(form || {}) }
      const request = (async () => {
        await loadRuntimeCryptoConfig()
        const userClient = import.meta.env.VITE_USER_CLIENT || 'h5'
        // 复用登录页预加载的同租户配置，不再重复请求 loginConfig。
        const loginConfig = await resolveLoginConfig(credentials.tenantId)
        const passwordEncryptionEnabled = loginConfig?.enablePasswordEncryption !== false
        const password = await this.encryptPassword(credentials.password, passwordEncryptionEnabled)
        const payload = {
          username: credentials.username,
          password,
          code: credentials.code,
          codeKey: credentials.codeKey,
          tenantId: credentials.tenantId || undefined,
          authType: 'password_captcha',
          userClient,
          appId: import.meta.env.VITE_APP_ID || undefined,
        }
        const res = await api.login(payload)
        this.setToken(res.data || {})
        await this.fetchUserInfo()
        this.fetchAccessSnapshot()
        return res
      })()

      passwordLoginPromise = request
      try {
        return await request
      }
      finally {
        if (passwordLoginPromise === request)
          passwordLoginPromise = null
      }
    },
    async oauthLogin({ socialTicket, connectionCode, tenantId } = {}) {
      const payload = {
        socialTicket,
        connectionCode,
        tenantId: tenantId || undefined,
        authType: 'oauth2',
        userClient: import.meta.env.VITE_USER_CLIENT || 'app',
        appId: import.meta.env.VITE_APP_ID || undefined,
      }
      const res = await api.login(payload)
      this.setToken(res.data || {})
      await this.fetchUserInfo()
      this.fetchAccessSnapshot()
      return res
    },
    async fetchUserInfo() {
      if (!this.accessToken) {
        return null
      }
      const res = await api.getUserInfo()
      this.setUserInfo(res.data || null)
      const tenantId = this.userInfo?.tenantId
      if (tenantId && String(useAppStore().brandConfig?.tenantId || '') !== String(tenantId)) await this.loadBrand(tenantId)
      return this.userInfo
    },
    async fetchAccessSnapshot() {
      if (!this.accessToken) {
        return
      }
      const [menuResult, permissionResult] = await Promise.allSettled([
        api.getCurrentMenu(),
        api.getCurrentPermissions(),
      ])
      if (menuResult.status === 'fulfilled') {
        this.setMenus(menuResult.value?.data)
      }
      if (permissionResult.status === 'fulfilled') {
        this.setPermissions(permissionResult.value?.data)
      }
    },
    async logout() {
      try {
        if (this.accessToken) {
          await api.logout()
        }
      }
      finally {
        this.resetAuth()
      }
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_auth`,
    pick: ['accessToken', 'tokenType', 'expiresIn', 'userInfo', 'menus', 'permissions'],
  },
})
