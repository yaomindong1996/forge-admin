import { describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { useFlowModelCatalog } from '../useFlowModelCatalog'

vi.mock('@/api/flow', () => ({ default: {} }))
vi.mock('@/api/business-app', () => ({ businessFlowModelBindingsBatch: vi.fn() }))
const row = key => ({ id: `19000000000000000${key}`, modelKey: key, modelName: key })
const response = (records, total = records.length) => ({ code: 200, data: { records, total } })
function deferred() {
  let resolve
  const promise = new Promise(done => resolve = done)
  return { promise, resolve }
}
function setup() {
  const api = { getModelPage: vi.fn().mockResolvedValue(response([row('A'), row('B')])), sortModels: vi.fn() }
  const bindings = vi.fn().mockResolvedValue({ code: 200, data: [{ flowModelKey: 'B', objectName: '库存' }] })
  const scope = effectScope()
  const catalog = scope.run(() => useFlowModelCatalog({ api, queryBindings: bindings, buildModelFormBinding: () => [] }))
  return { api, bindings, scope, catalog }
}
describe('模型卡片列表查询领域', () => {
  it('一页只调用一次模型及批量绑定，按 Key 分组并保留长 ID 字符串', async () => {
    const { api, bindings, scope, catalog: c } = setup()
    await c.fetchData()
    expect(api.getModelPage).toHaveBeenCalledTimes(1)
    expect(bindings).toHaveBeenCalledTimes(1)
    expect(bindings).toHaveBeenCalledWith(['A', 'B'])
    expect(c.dataSource.value[0].businessBindings).toEqual([])
    expect(c.dataSource.value[1].businessBindings[0].objectName).toBe('库存')
    expect(typeof c.dataSource.value[0].id).toBe('string')
    scope.stop()
  })
  it('无匹配时不调用绑定，清理关键词空白，重置回到第一页', async () => {
    const { api, bindings, scope, catalog: c } = setup()
    api.getModelPage.mockResolvedValue(response([]))
    c.queryParams.modelName = '  合同  '
    c.pagination.page = 3
    await c.handleSearch()
    expect(c.hasAppliedFilters.value).toBe(true)
    expect(api.getModelPage).toHaveBeenLastCalledWith(expect.objectContaining({ modelName: '合同', pageNum: 1 }))
    expect(bindings).not.toHaveBeenCalled()
    await c.handleReset()
    expect(c.hasAppliedFilters.value).toBe(false)
    expect(c.pagination.page).toBe(1)
    scope.stop()
  })
  it('分页用已生效条件，不偷偷提交正在输入的关键词', async () => {
    const { api, scope, catalog: c } = setup()
    c.queryParams.modelName = '合同'
    await c.handleSearch()
    c.queryParams.modelName = '未确认的输入'
    c.pagination.page = 2
    await c.fetchData()
    expect(api.getModelPage).toHaveBeenLastCalledWith(expect.objectContaining({ modelName: '合同', pageNum: 2 }))
    scope.stop()
  })
  it('绑定失败保留模型并明确标记，不误报未绑定，支持重试', async () => {
    const { bindings, scope, catalog: c } = setup()
    bindings.mockRejectedValue(new Error('unavailable'))
    await c.fetchData()
    expect(c.dataSource.value).toHaveLength(2)
    expect(c.bindingLoadError.value).toBe(true)
    expect(c.loadError.value).toBe('')
    bindings.mockResolvedValue({ code: 200, data: [] })
    await c.fetchData()
    expect(c.bindingLoadError.value).toBe(false)
    scope.stop()
  })
  it('模型失败保留上次结果与分页，重试原失败页而不是伪造空结果', async () => {
    const { api, scope, catalog: c } = setup()
    await c.fetchData()
    api.getModelPage.mockRejectedValueOnce(new Error('unavailable'))
    c.pagination.page = 3
    await c.fetchData()
    expect(c.loadError.value).toContain('保留上一次')
    expect(c.pagination.page).toBe(1)
    expect(c.dataSource.value).toHaveLength(2)
    await c.retryQuery()
    expect(api.getModelPage).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 3 }))
    expect(c.loadError.value).toBe('')
    scope.stop()
  })
  it('慢模型响应不能覆盖新查询', async () => {
    const { api, scope, catalog: c } = setup()
    const old = deferred()
    api.getModelPage.mockReturnValueOnce(old.promise).mockResolvedValueOnce(response([row('new')]))
    const first = c.handleSearch()
    c.queryParams.modelName = 'new'
    await c.handleSearch()
    old.resolve(response([row('old')]))
    await first
    expect(c.dataSource.value[0].modelKey).toBe('new')
    expect(c.appliedQuery.value.modelName).toBe('new')
    scope.stop()
  })
  it('慢绑定不串页，卸载后不能写回', async () => {
    const { api, bindings, scope, catalog: c } = setup()
    const pending = deferred()
    bindings.mockReturnValueOnce(pending.promise)
    const first = c.fetchData()
    await Promise.resolve()
    api.getModelPage.mockResolvedValue(response([row('latest')]))
    await c.fetchData()
    pending.resolve({ code: 200, data: [] })
    await first
    expect(c.dataSource.value[0].modelKey).toBe('latest')
    const late = deferred()
    api.getModelPage.mockReturnValue(late.promise)
    const last = c.fetchData()
    scope.stop()
    late.resolve(response([row('disposed')]))
    await last
    expect(c.dataSource.value[0].modelKey).toBe('latest')
  })
  it('排序时不查询，失败不退出排序，保存使用字符串 ID', async () => {
    const { api, scope, catalog: c } = setup()
    await c.fetchData()
    c.sortMode.value = true
    await c.handleSearch()
    expect(api.getModelPage).toHaveBeenCalledTimes(1)
    c.handleDragStart(c.dataSource.value[0])
    c.handleDrop(c.dataSource.value[1])
    expect(c.dataSource.value[0].modelKey).toBe('B')
    api.sortModels.mockResolvedValueOnce({ code: 500 })
    await c.saveModelOrder()
    expect(c.sortMode.value).toBe(true)
    api.sortModels.mockResolvedValueOnce({ code: 200 })
    await c.saveModelOrder()
    expect(c.sortMode.value).toBe(false)
    expect(api.sortModels).toHaveBeenLastCalledWith({ items: [
      { modelId: row('B').id, sortOrder: 0 },
      { modelId: row('A').id, sortOrder: 1 },
    ] })
    scope.stop()
  })
})
