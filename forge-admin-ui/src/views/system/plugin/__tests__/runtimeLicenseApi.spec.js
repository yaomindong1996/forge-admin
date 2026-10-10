import { describe, expect, it, vi } from 'vitest'
import { getRuntimeLicenseStatus } from '@/api/system/runtimeLicense'

const get = vi.hoisted(() => vi.fn())
vi.mock('@/utils/request', () => ({ request: { get } }))

describe('运行时授权只读 API', () => {
  it('复用统一请求工具，固定地址，无身份或文件参数', async () => {
    await getRuntimeLicenseStatus()
    expect(get).toHaveBeenCalledTimes(1)
    expect(get).toHaveBeenCalledWith('/system/plugin/runtime-license/status', { needTip: false })
  })
})
