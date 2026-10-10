import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getSystemVersion } from '@/api/system/version'
import { useSystemVersionStore } from '../versionStore'

vi.mock('@/api/system/version', () => ({ getSystemVersion: vi.fn() }))
const response = { code: 200, data: { version: '1.2.0', coreVersion: '1.2.0', edition: 'community' } }

describe('系统版本状态', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.stubGlobal('__FORGE_BUILD_INFO__', { version: '1.2.0', client: 'admin-ui', notes: '<script>test</script>' })
  })
  afterEach(() => vi.unstubAllGlobals())

  it('不在创建 Store 时请求；打开后获取，重复点击不并发', async () => {
    let resolve
    getSystemVersion.mockReturnValue(new Promise((done) => {
      resolve = done
    }))
    const store = useSystemVersionStore()
    expect(getSystemVersion).not.toHaveBeenCalled()
    const pending = store.open()
    await store.open()
    await store.refresh()
    expect(getSystemVersion).toHaveBeenCalledTimes(1)
    expect(store.loading).toBe(true)
    resolve(response)
    await pending
    expect(store.backend.version).toBe('1.2.0')
    expect(store.visible).toBe(true)
    expect(store.warning).toBe('')
  })

  it('404/业务错误不伪造后端；仍可查看前端，并可重试', async () => {
    getSystemVersion.mockRejectedValueOnce(new Error('404')).mockResolvedValueOnce(response)
    const store = useSystemVersionStore()
    await store.open()
    expect(store.backend).toBeNull()
    expect(store.local.version).toBe('1.2.0')
    expect(store.error).toContain('暂时无法获取')
    expect(store.loading).toBe(false)
    await store.refresh()
    expect(store.error).toBe('')
    expect(store.backend.version).toBe('1.2.0')
    getSystemVersion.mockResolvedValueOnce({ code: 500, data: response.data })
    await store.refresh()
    expect(store.backend).toBeNull()
    expect(store.error).toBeTruthy()
  })

  it('显示不一致提示，缺失后端 build-info 不用核心版本冒充', async () => {
    getSystemVersion.mockResolvedValueOnce({ code: 200, data: { version: '1.1.0' } })
    const store = useSystemVersionStore()
    await store.open()
    expect(store.warning).toContain('不一致')
    getSystemVersion.mockResolvedValueOnce({ code: 200, data: { version: null, coreVersion: '1.2.0' } })
    await store.refresh()
    expect(store.backend.version).toBeNull()
    expect(store.warning).toBe('')
  })

  it('关闭、退出或重开后，迟到响应不能覆盖当前信息', async () => {
    let resolve
    getSystemVersion.mockReturnValueOnce(new Promise((done) => {
      resolve = done
    })).mockResolvedValueOnce(response)
    const store = useSystemVersionStore()
    const pending = store.open()
    store.close()
    expect(store.loading).toBe(false)
    await store.open()
    resolve({ code: 200, data: { version: '0.0.1' } })
    await pending
    expect(store.backend.version).toBe('1.2.0')
    store.close()
    expect(store.visible).toBe(false)
    expect(store.backend).toBeNull()
  })
})
