import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, ref } from 'vue'
import { useDatasetPage } from '../composables/useDatasetPage'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/utils', () => ({ request: { get: vi.fn(async () => ({ code: 200, data: [] })) } }))
vi.mock('@/composables/useDict', () => ({
  useDict: () => ({ dict: ref({}) }),
  getDictData: vi.fn(async () => []),
}))
vi.mock('@/components/ai-form', () => ({ AiCrudPage: {} }))
vi.mock('@/api/data/connection', () => ({
  getDataConnectionList: vi.fn(async () => ({ code: 200, data: [] })),
  getDataConnectionFields: vi.fn(async () => ({ code: 200, data: [] })),
  getDataConnectionTables: vi.fn(async () => ({ code: 200, data: [] })),
}))
vi.mock('@/api/data/dimension', () => ({ getDataDimensionList: vi.fn(async () => ({ code: 200, data: [] })) }))
vi.mock('@/api/data/dataset', () => ({
  getDataDatasetCategoryTree: vi.fn(async () => ({ code: 200, data: [] })),
  getDataDatasetById: vi.fn(),
  getDashboardDatasetImpact: vi.fn(),
  deleteDataDataset: vi.fn(),
  offlineDataDataset: vi.fn(),
  publishDataDataset: vi.fn(),
  saveDataDatasetFields: vi.fn(),
  syncDataDatasetFields: vi.fn(),
}))

let scope
let api
beforeEach(() => {
  setActivePinia(createPinia())
  scope = effectScope()
  api = scope.run(() => useDatasetPage())
})
afterEach(() => scope.stop())

describe('dataset UI keeps original business contracts', () => {
  it('keeps all actions, with two direct actions and a compact calculated table width', () => {
    const columns = api.tableColumns.value
    const action = columns.find(column => column.prop === 'action')
    expect(action.maxActionButtons).toBe(2)
    expect(action.actions.map(item => item.key)).toEqual(['edit', 'view', 'publish', 'offline', 'fields', 'sync', 'delete'])
    expect(columns.reduce((total, column) => total + column.width, 0)).toBeLessThan(1400)
    const time = columns.find(column => column.prop === 'updateTime')
    expect(time.render({ updateTime: '2026-10-03T09:30:00' })).toBe('2026-10-03 09:30:00')
  })

  it('passes the original query protocol and clears existing criteria via reset', () => {
    const search = vi.fn()
    api.crudRef.value = { search }
    api.queryForm.datasetName = '销售'
    api.queryForm.datasetType = 'SQL'
    api.queryForm.publishStatus = 0
    api.applySearch()
    expect(search.mock.calls[0][0]).toMatchObject({ datasetName: '销售', datasetType: 'SQL', publishStatus: 0 })
    api.handleResetFilters()
    expect(search).toHaveBeenLastCalledWith({})
  })

  it('keeps panel field changes on the shared slot model and blocks writes for published records', () => {
    const data = api.prepareDatasetFormData({ id: 'sample-dataset', connectionId: '1', tableName: 'sample_table' })
    const updateValue = vi.fn()
    api.updateDatasetFormField(data, 'paramSchemaJson', [], updateValue)
    expect(updateValue).toHaveBeenCalled()
    expect(api.beforeSubmit(data)).toMatchObject({ connectionId: '1', tableName: 'sample_table', accessMode: 'PUBLIC' })
    api.currentEditingDataset.value = { publishStatus: 1 }
    expect(api.isFormReadOnly.value).toBe(true)
    expect(api.beforeSubmit({ ...data })).toBe(false)
  })
})
