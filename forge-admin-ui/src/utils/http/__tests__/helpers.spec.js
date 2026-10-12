import { beforeEach, describe, expect, it, vi } from 'vitest'

import { resolveResError, shouldForceReLogin } from '../helpers'

const { authStore } = vi.hoisted(() => ({
  authStore: {
    isLoggingOut: false,
    logout: vi.fn(),
    resetToken: vi.fn(),
  },
}))

vi.mock('@/store', () => ({
  useAuthStore: () => authStore,
}))

vi.mock('../error-dialog', () => ({
  showRequestErrorDialog: vi.fn(),
}))

describe('auth error logout rules', () => {
  beforeEach(() => {
    authStore.isLoggingOut = false
    authStore.logout.mockReset()
    authStore.resetToken.mockReset()
    window.$message = { error: vi.fn() }
  })

  it('force-logouts only on known session-invalid 401 messages', () => {
    expect(shouldForceReLogin(401, '登录凭证无效')).toBe(true)
    expect(shouldForceReLogin(401, '登录已过期')).toBe(true)
    expect(shouldForceReLogin(401, '您的账号已在其他地方登录')).toBe(true)
    expect(shouldForceReLogin(401, '执行器认证失败')).toBe(false)
    expect(shouldForceReLogin(401, '安全会话已过期，请重新操作')).toBe(false)
    expect(shouldForceReLogin(401, 'Request failed with status code 401')).toBe(false)
  })

  it('does not treat flow or workspace 401 as whole-app session death', () => {
    expect(shouldForceReLogin(401, '登录凭证无效', { url: '/dev-api/api/flow/task/todo' })).toBe(false)
    expect(shouldForceReLogin(401, '登录凭证无效', { url: '/api/flow/workspace/home' })).toBe(false)
  })

  it('logs out on admin session-invalid 401', () => {
    resolveResError(401, '登录凭证无效', true, { url: '/system/user/page' })
    expect(authStore.logout).toHaveBeenCalledTimes(1)
  })

  it('keeps the current session for flow 401 even when message looks invalid', () => {
    resolveResError(401, '登录凭证无效', true, { url: '/dev-api/api/flow/task/todo' })
    expect(authStore.logout).not.toHaveBeenCalled()
    expect(window.$message.error).not.toHaveBeenCalled()
  })

  it('shows a normal error for unrelated 401 without logging out', () => {
    resolveResError(401, '执行器认证失败', true, { url: '/system/job/page' })
    expect(authStore.logout).not.toHaveBeenCalled()
    expect(window.$message.error).toHaveBeenCalledWith('执行器认证失败')
  })
})
