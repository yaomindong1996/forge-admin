import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/api/print'
import { usePrintCenterStore } from '@/stores/print/printCenterStore'

vi.mock('@/api/print', () => ({
  printSources: vi.fn(),
  printSourceDetail: vi.fn(),
  createPrintSource: vi.fn(),
  updatePrintSource: vi.fn(),
  changePrintSourceStatus: vi.fn(),
  deletePrintSource: vi.fn(),
}))

const sources = [
  { id: '9007199254740995', sourceName: '采购单', sourceCode: 'purchase_order' },
  { id: '9007199254740997', sourceName: '销售单', sourceCode: 'sales_order' },
]

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
  api.printSources.mockResolvedValue({ data: { records: sources, total: 2 } })
  api.printSourceDetail.mockImplementation(async id => ({
    data: {
      ...sources.find(item => String(item.id) === String(id)),
      sourceType: 'SERVICE',
      objectCode: 'purchase_order',
      sourceRevision: 1,
      status: 0,
    },
  }))
})

describe('print center store', () => {
  it('keeps long identifiers and selects the requested source', async () => {
    const store = usePrintCenterStore()
    await store.load('9007199254740997')
    expect(store.selectedId).toBe('9007199254740997')
    expect(store.selected.sourceName).toBe('销售单')
    expect(store.sourceIdentity.businessSourceId).toBe('9007199254740997')
  })

  it('keeps request generations monotonic after clearing the workspace', async () => {
    const store = usePrintCenterStore()
    await store.load()
    const generation = store.generation
    store.clear()
    expect(store.generation).toBe(generation + 1)
    expect(store.sources).toEqual([])
  })

  it('updates a source with revision control instead of resending immutable identity', async () => {
    const store = usePrintCenterStore()
    await store.load()
    api.updatePrintSource.mockResolvedValue({ data: { id: sources[0].id } })
    await store.save({
      id: sources[0].id,
      sourceRevision: 1,
      sourceName: '采购单打印',
      sourceCode: 'ignored_immutable_code',
      sourceType: 'SERVICE',
      providerCode: 'purchase-provider',
      objectCode: 'purchase_order',
      parameterSchemaJson: '{}',
      mappingJson: null,
    })
    expect(api.updatePrintSource).toHaveBeenCalledWith(sources[0].id, {
      expectedRevision: 1,
      sourceName: '采购单打印',
      providerCode: 'purchase-provider',
      datasetId: null,
      objectCode: 'purchase_order',
      parameterSchemaJson: '{}',
      mappingJson: null,
    })
  })
})
