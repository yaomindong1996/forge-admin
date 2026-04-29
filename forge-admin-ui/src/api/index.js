import { request } from '@/utils'

const useMock = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true'
export default {
  // 获取用户信息 - 使用新的认证接口
  getUser: () => request.get('/auth/userInfo'),

  // 登出 - 使用新的认证接口
  logout: () => request.post('/auth/logout', {}, { needTip: false }),

  // 获取菜单数据 - 使用新的系统资源接口
  getMenu: async () => {
    if (useMock) {
      const { mockMenuApi } = await import('@/api/mock/menu')
      return mockMenuApi.getMenu()
    }
    else {
      return request.get('/auth/current/menu')
    }
  },

  // 获取租户配置
  getTenantConfig: tenantId => request.post('/system/tenant/userTenantConfig', { id: tenantId }),
}
