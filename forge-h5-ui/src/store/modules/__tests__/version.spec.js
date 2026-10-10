import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getSystemVersion } from '@/api/system-version'
import { useSystemVersionStore } from '../version'

vi.mock('@/api/system-version', () => ({ getSystemVersion: vi.fn() }))

describe('H5 版本信息', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('创建无请求，打开时查 App 服务；失败后可重试', async () => {
    const store = useSystemVersionStore()
    expect(getSystemVersion).not.toHaveBeenCalled()
    getSystemVersion.mockRejectedValueOnce(new Error('404'))
    await store.open()
    expect(store.backend).toBeNull()
    expect(store.error).toBeTruthy()
    getSystemVersion.mockResolvedValueOnce({ code: 200, data: { version: '1.2.0', coreVersion: '1.2.0' } })
    await store.refresh()
    expect(store.backend.version).toBe('1.2.0')
    expect(store.error).toBe('')
  })

  it('并发只请求一次，离开页面后不回填旧请求', async () => {
    let resolve
    getSystemVersion.mockReturnValueOnce(new Promise((done) => {
      resolve = done
    }))
    const store = useSystemVersionStore()
    const pending = store.open()
    await store.refresh()
    expect(getSystemVersion).toHaveBeenCalledTimes(1)
    store.close()
    resolve({ code: 200, data: { version: '1.2.0' } })
    await pending
    expect(store.backend).toBeNull()
    expect(store.visible).toBe(false)
    expect(store.loading).toBe(false)
  })
})
