import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { usePluginCenter } from '../usePluginCenter'

const api = vi.hoisted(() => ({ listRuntimePlugins: vi.fn(), getRuntimePlugin: vi.fn() }))
vi.mock('@/api/system/plugin', () => api)
let scope
let center
beforeEach(() => {
  vi.clearAllMocks()
  scope = effectScope()
  center = scope.run(() => usePluginCenter())
})
afterEach(() => scope.stop())
const page = id => ({ code: 200, data: { records: [{ id }], total: 1, coreVersion: '1.2.0' } })
function deferred() {
  let resolve
  const promise = new Promise(done => resolve = done)
  return { promise, resolve }
}

describe('插件中心请求编排', () => {
  it('传递固定分页协议，空来源不提交，查询和重置回到第一页', async () => {
    api.listRuntimePlugins.mockResolvedValue(page('one'))
    await center.changePage(3)
    expect(api.listRuntimePlugins).toHaveBeenLastCalledWith({ keyword: '', pageNum: 3, pageSize: 15 })
    center.query.origin = 'external'
    center.query.keyword = 'one'
    await center.search()
    const expected = { keyword: 'one', origin: 'external', pageNum: 1, pageSize: 15 }
    expect(api.listRuntimePlugins).toHaveBeenLastCalledWith(expected)
    await center.changeSize(30)
    expect(center.query.pageSize).toBe(30)
    await center.reset()
    expect(center.query.origin).toBeNull()
    expect(center.query.keyword).toBe('')
  })

  it('慢的旧查询不能覆盖新的清单或结束新请求的 loading', async () => {
    const first = deferred()
    const second = deferred()
    api.listRuntimePlugins.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const oldRequest = center.load()
    const newRequest = center.search()
    first.resolve(page('old'))
    await oldRequest
    expect(center.loading.value).toBe(true)
    second.resolve(page('new'))
    await newRequest
    expect(center.records.value[0].id).toBe('new')
    expect(center.loading.value).toBe(false)
  })

  it('失败清除旧数据并可重试，不把 500 响应当成空清单', async () => {
    api.listRuntimePlugins.mockResolvedValueOnce(page('one'))
      .mockResolvedValueOnce({ code: 500, message: '无权访问' })
      .mockResolvedValueOnce(page('two'))
    await center.load()
    await center.load()
    expect(center.records.value).toEqual([])
    expect(center.metadata.value).toEqual({})
    expect(center.error.value).toBe('无权访问')
    await center.load()
    expect(center.error.value).toBe('')
    expect(center.records.value[0].id).toBe('two')
  })

  it('详情以新 ID 为准，关闭后的响应不能重新填充面板', async () => {
    const first = deferred()
    api.getRuntimePlugin.mockReturnValueOnce(first.promise).mockResolvedValueOnce({ code: 200, data: { id: 'two' } })
    const oldRequest = center.openDetail('one')
    await center.openDetail('two')
    first.resolve({ code: 200, data: { id: 'one' } })
    await oldRequest
    expect(center.detail.value.id).toBe('two')
    const closing = deferred()
    api.getRuntimePlugin.mockReturnValueOnce(closing.promise)
    const request = center.openDetail('three')
    center.closeDetail()
    closing.resolve({ code: 200, data: { id: 'three' } })
    await request
    expect(center.detailVisible.value).toBe(false)
    expect(center.detail.value).toBeNull()
  })

  it('详情失败不沿用上一条，重试保留选中的 ID', async () => {
    api.getRuntimePlugin.mockResolvedValueOnce({ code: 404, message: '未加载' })
      .mockResolvedValueOnce({ code: 200, data: { id: 'one' } })
    await center.openDetail('one')
    expect(center.detail.value).toBeNull()
    expect(center.detailError.value).toBe('未加载')
    await center.openDetail()
    expect(api.getRuntimePlugin).toHaveBeenLastCalledWith('one')
    expect(center.detail.value.id).toBe('one')
  })

  it('销毁作用域后，旧响应不能回填已经退出的页面', async () => {
    const pending = deferred()
    api.listRuntimePlugins.mockReturnValueOnce(pending.promise)
    const request = center.load()
    scope.stop()
    pending.resolve(page('late'))
    await request
    expect(center.records.value).toEqual([])
    expect(center.loading.value).toBe(false)
  })
})
