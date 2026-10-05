import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAccountActions } from '../composables/useAccountActions'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  logoutApi: vi.fn(),
  beginLogout: vi.fn(),
  logout: vi.fn(),
}))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push }) }))
vi.mock('@/api', () => ({ default: { logout: mocks.logoutApi } }))
vi.mock('@/store', () => ({ useAuthStore: () => ({ beginLogout: mocks.beginLogout, logout: mocks.logout }) }))
vi.mock('@/utils/http/helpers', () => ({ isSilentAuthError: error => error?.silent === true }))

describe('共用账户动作', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.logoutApi.mockResolvedValue(undefined)
    window.$dialog = { confirm: vi.fn() }
    window.$message = { success: vi.fn() }
  })

  it.each([null, '/app/example/profile', { path: '/profile', query: { source: 'portal' } }])(
    '资料入口 %s 保留门户目标或使用系统资料',
    (target) => {
      const actions = useAccountActions(() => target)
      actions.handleAccountAction('profile')
      expect(mocks.push).toHaveBeenCalledWith(target || '/profile')
    },
  )

  it('函数资料路由响应最新目标，空目标仍回到系统资料', () => {
    let target = '/app/example/profile'
    const actions = useAccountActions(() => () => target)
    actions.openProfile()
    expect(mocks.push).toHaveBeenLastCalledWith(target)
    target = null
    actions.openProfile()
    expect(mocks.push).toHaveBeenLastCalledWith('/profile')
  })

  it('取消退出只显示确认，不调用接口或修改登录态', () => {
    useAccountActions().handleAccountAction('logout')
    expect(window.$dialog.confirm).toHaveBeenCalledOnce()
    expect(window.$dialog.confirm.mock.calls[0][0].content).toBe('确认退出？')
    expect(mocks.beginLogout).not.toHaveBeenCalled()
    expect(mocks.logoutApi).not.toHaveBeenCalled()
    expect(mocks.logout).not.toHaveBeenCalled()
  })

  it('确认退出依次冻结认证、调用注销、清理登录态，只请求一次', async () => {
    let complete
    mocks.logoutApi.mockImplementation(() => new Promise((resolve) => {
      complete = resolve
    }))
    useAccountActions().confirmLogout()
    const confirm = window.$dialog.confirm.mock.calls[0][0].confirm
    const first = confirm()
    await confirm()
    expect(mocks.beginLogout).toHaveBeenCalledOnce()
    expect(mocks.logoutApi).toHaveBeenCalledOnce()
    expect(mocks.logout).not.toHaveBeenCalled()
    complete()
    await first
    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.beginLogout.mock.invocationCallOrder[0]).toBeLessThan(mocks.logoutApi.mock.invocationCallOrder[0])
    expect(mocks.logoutApi.mock.invocationCallOrder[0]).toBeLessThan(mocks.logout.mock.invocationCallOrder[0])
    expect(window.$message.success).toHaveBeenCalledWith('已退出登录')
  })

  it.each([true, false])('注销失败仍清理本地会话，静默鉴权异常 %s 不重复报错', async (silent) => {
    const error = { silent }
    mocks.logoutApi.mockRejectedValue(error)
    const logger = vi.spyOn(console, 'error').mockImplementation(() => {})
    try {
      useAccountActions().confirmLogout()
      await window.$dialog.confirm.mock.calls[0][0].confirm()
      expect(mocks.logout).toHaveBeenCalledOnce()
      expect(logger).toHaveBeenCalledTimes(silent ? 0 : 1)
    }
    finally { logger.mockRestore() }
  })
})
