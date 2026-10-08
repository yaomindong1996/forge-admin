import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, reactive } from 'vue'
import { createRuntimeLicenseState } from '@/stores/plugin/runtimeLicenseState'
import { licenseTime, licenseUsePeriod } from '../runtimeLicenseUtils'

let scope
let state
let actor
let api
const response = mode => ({ code: 200, data: { mode, edition: 'community', report: null } })
function deferred() {
  let resolve
  const promise = new Promise(done => resolve = done)
  return { promise, resolve }
}
beforeEach(() => {
  scope = effectScope()
  actor = reactive({ id: 'one', allowed: true })
  api = vi.fn()
  state = scope.run(() => createRuntimeLicenseState(api, () => actor.allowed, () => actor.id))
})
afterEach(() => scope.stop())

describe('只读运行时授权状态', () => {
  it('拒绝无权限查询，失败清除旧快照，重试后恢复', async () => {
    api.mockResolvedValueOnce(response('community'))
    api.mockResolvedValueOnce({ code: 500, message: '查询失败' })
    api.mockResolvedValueOnce(response('custom'))
    await state.load()
    expect(state.data.value.mode).toBe('community')
    await state.load()
    expect(state.data.value).toBeNull()
    expect(state.error.value).toBe('查询失败')
    await state.load()
    expect(state.error.value).toBe('')
    actor.allowed = false
    expect(state.data.value).toBeNull()
    await state.load()
    expect(api).toHaveBeenCalledTimes(3)
  })
  it('最后一次查询生效，慢请求不覆盖新数据或关闭新 loading', async () => {
    const old = deferred()
    const current = deferred()
    api.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    const first = state.load()
    const second = state.load()
    old.resolve(response('custom'))
    await first
    expect(state.loading.value).toBe(true)
    current.resolve(response('community'))
    await second
    expect(state.data.value.mode).toBe('community')
    expect(state.loading.value).toBe(false)
  })
  it('账号或权限同步 ABA 切换后，原响应仍不得回填', async () => {
    for (const property of ['id', 'allowed']) {
      const pending = deferred()
      api.mockReturnValueOnce(pending.promise)
      const request = state.load()
      const original = actor[property]
      actor[property] = property === 'id' ? 'two' : false
      actor[property] = original
      pending.resolve(response('license'))
      await request
      expect(state.data.value).toBeNull()
      expect(state.loading.value).toBe(false)
    }
  })
  it('页面关闭或 store 销毁后，不接受旧请求', async () => {
    const pending = deferred()
    api.mockReturnValue(pending.promise)
    const request = state.load()
    state.clear()
    pending.resolve(response('license'))
    await request
    expect(state.data.value).toBeNull()
    const second = deferred()
    api.mockReturnValue(second.promise)
    const other = state.load()
    scope.stop()
    second.resolve(response('license'))
    await other
    expect(state.data.value).toBeNull()
  })
  it('使用截止为 null 才是永久，UTC 和 epoch 秒正确格式化，0 不能当缺失', () => {
    expect(licenseTime(0)).not.toBe('—')
    expect(licenseTime(0)).toBe(licenseTime('1970-01-01T00:00:00Z'))
    expect(licenseTime('invalid')).toBe('—')
    expect(licenseUsePeriod(null)).toBe('—')
    expect(licenseUsePeriod({ notBefore: 0, validUntil: null })).toContain('永久使用')
    expect(licenseUsePeriod({ notBefore: 0, validUntil: 100 })).not.toContain('永久')
    expect(licenseUsePeriod({ notBefore: 0, validUntil: 100 })).not.toContain('T')
  })
})
