import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { imageGenerate, imageGenerateGetResult } from '@/api/ai/media'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'
import { useAiImageJobsStore } from '../imageJobsStore'

vi.mock('@/api/ai/media', () => ({ imageGenerate: vi.fn(), imageGenerateGetResult: vi.fn() }))
vi.mock('@/store/modules/auth', async () => {
  const { defineStore } = await import('pinia')
  return { useAuthStore: defineStore('test-auth', { state: () => ({ accessToken: undefined }) }) }
})

let pinia
let store
let user
const response = (status = 'pending', extra = {}) => ({ code: 200, data: { id: '31', status, ...extra } })
function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { resolve, promise }
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.resetAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().accessToken = 'synthetic-session-only'
  user = useUserStore()
  user.userInfo = { id: '41', tenantId: '7', permissions: ['ai:image:generate'] }
  store = useAiImageJobsStore()
  imageGenerateGetResult.mockResolvedValue(response())
})
afterEach(() => {
  pinia._s.forEach(instance => instance.$dispose())
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('共享图片轮询', () => {
  it('串行 pending / generating / success，终态不再查询', async () => {
    imageGenerateGetResult.mockResolvedValueOnce(response())
      .mockResolvedValueOnce(response('generating'))
      .mockResolvedValueOnce(response('success', { resultFileId: '9007199254740993' }))
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(4000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(3)
    expect(store.jobs.get('31').result.resultFileId).toBe('9007199254740993')
    await vi.advanceTimersByTimeAsync(10000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(3)
    expect(imageGenerateGetResult.mock.calls[0][1]).toMatchObject({ timeout: 10000, needTip: false })
  })

  it('同 ID 两订阅共享慢请求，不能重叠；只最后一个离开才取消', async () => {
    const slow = deferred()
    imageGenerateGetResult.mockReturnValueOnce(slow.promise)
    const first = store.subscribe('31')
    const last = store.subscribe('31')
    await vi.advanceTimersByTimeAsync(8000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(1)
    const signal = imageGenerateGetResult.mock.calls[0][1].signal
    first()
    expect(signal.aborted).toBe(false)
    last()
    expect(signal.aborted).toBe(true)
    slow.resolve(response('success', { resultFileId: '51' }))
    await vi.advanceTimersByTimeAsync(0)
    expect(store.jobs.get('31').result.status).toBe('pending')
  })

  it('旧查询迟到不能覆盖重新订阅的结果', async () => {
    const old = deferred()
    imageGenerateGetResult.mockReturnValueOnce(old.promise)
      .mockResolvedValueOnce(response('success', { resultFileId: '52' }))
    const leave = store.subscribe('31')
    await vi.advanceTimersByTimeAsync(0)
    leave()
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(0)
    old.resolve(response('failed', { errorMsg: '旧任务响应' }))
    await vi.advanceTimersByTimeAsync(0)
    expect(store.jobs.get('31').result).toMatchObject({ status: 'success', resultFileId: '52' })
  })

  it('失败只暂停查询，手动恢复不会 POST 重新生成', async () => {
    imageGenerateGetResult.mockRejectedValueOnce(new Error('synthetic query failure'))
      .mockResolvedValueOnce(response('success', { resultFileId: '51' }))
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(10000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(1)
    expect(store.jobs.get('31')).toMatchObject({ paused: true, result: { status: 'pending' } })
    store.resume('31')
    await vi.advanceTimersByTimeAsync(0)
    expect(store.jobs.get('31')).toMatchObject({ paused: false, result: { status: 'success' } })
    expect(imageGenerate).not.toHaveBeenCalled()
  })

  it('五分钟后停止查询而非无限轮询，恢复后只查询', async () => {
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(300000)
    expect(store.jobs.get('31').paused).toBe(true)
    const calls = imageGenerateGetResult.mock.calls.length
    await vi.advanceTimersByTimeAsync(10000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(calls)
    store.resume('31')
    await vi.advanceTimersByTimeAsync(0)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(calls + 1)
    expect(imageGenerate).not.toHaveBeenCalled()
  })

  it.each([
    { code: 500 },
    { code: 200, data: null },
    response('unexpected'),
    response('success', { resultFileId: null }),
    { code: 200, data: { id: '32', status: 'success', resultFileId: '51' } },
  ])('错误/未知响应不能伪造成功：%j', async (result) => {
    imageGenerateGetResult.mockResolvedValueOnce(result)
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(0)
    expect(store.jobs.get('31')).toMatchObject({ paused: true, result: { status: 'pending' } })
  })

  it.each(['account', 'tenant', 'logout', 'permission'])('身份/权限变化立即清除并拒绝旧响应：%s', async (change) => {
    const old = deferred()
    imageGenerateGetResult.mockReturnValueOnce(old.promise)
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(0)
    const signal = imageGenerateGetResult.mock.calls[0][1].signal
    if (change === 'account') {
      user.userInfo.id = '42'
    }
    if (change === 'tenant') {
      user.userInfo.tenantId = '8'
    }
    if (change === 'logout') {
      useAuthStore().accessToken = undefined
    }
    if (change === 'permission') {
      user.userInfo.permissions = []
    }
    expect(signal.aborted).toBe(true)
    expect(store.jobs.size).toBe(0)
    old.resolve(response('success', { resultFileId: '51' }))
    await vi.advanceTimersByTimeAsync(0)
    expect(store.jobs.size).toBe(0)
  })

  it('无权限、无身份和不合法 ID 均不发请求；图片权限不推断历史许可', async () => {
    store.subscribe('invalid')
    user.userInfo.permissions = []
    store.subscribe('31')
    await vi.advanceTimersByTimeAsync(0)
    expect(store.jobs.size).toBe(0)
    expect(imageGenerateGetResult).not.toHaveBeenCalled()
  })

  it('不无限缓存任务：64 活跃任务后拒绝，释放的旧项可回收', () => {
    const leave = store.subscribe('1')
    for (let id = 2; id <= 64; id += 1) {
      store.subscribe(String(id))
    }
    expect(() => store.subscribe('65')).toThrow('查询任务过多')
    leave()
    store.subscribe('65')
    expect(store.jobs.size).toBe(64)
    expect(store.jobs.has('1')).toBe(false)
  })
})
