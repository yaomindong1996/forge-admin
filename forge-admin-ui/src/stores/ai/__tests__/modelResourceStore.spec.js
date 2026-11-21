import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useModelResourceStore } from '../modelResourceStore'

const mocks = vi.hoisted(() => ({
  providerPage: vi.fn(),
  providerGetById: vi.fn(),
  providerTemplates: vi.fn(),
  providerAdd: vi.fn(),
  providerUpdate: vi.fn(),
  providerDelete: vi.fn(),
  providerSetDefault: vi.fn(),
  providerFetchModels: vi.fn(),
  providerBatchImportModels: vi.fn(),
  modelPage: vi.fn(),
  modelGetById: vi.fn(),
  modelAdd: vi.fn(),
  modelUpdate: vi.fn(),
  modelDelete: vi.fn(),
  modelTest: vi.fn(),
  modelSetDefault: vi.fn(),
}))

vi.mock('@/api/ai', () => mocks)
vi.mock('@/composables/useDict', () => ({
  useDict: () => ({
    dict: {
      value: {
        ai_provider_type: [{ label: 'DeepSeek', value: 'deepseek' }],
        ai_provider_adapter_type: [{ label: 'OpenAI 兼容', value: 'openai_compatible' }],
        ai_model_type: [{ label: '对话模型', value: 'chat' }],
        ai_status: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }],
        ai_is_default: [{ label: '是', value: '1' }, { label: '否', value: '0' }],
      },
    },
  }),
}))

describe('modelResourceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    window.$message = {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    }
    mocks.providerTemplates.mockResolvedValue({ code: 200, data: [] })
    mocks.providerPage.mockResolvedValue({
      code: 200,
      data: {
        records: [{ id: 101, providerName: '默认 DeepSeek', adapterCode: 'openai_compatible', isDefault: '0', status: '0' }],
        total: 1,
      },
    })
    mocks.modelPage.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
  })

  it('calls providerSetDefault after creating a provider with isDefault = 1', async () => {
    mocks.providerAdd.mockResolvedValue({ code: 200 })
    mocks.providerSetDefault.mockResolvedValue({ code: 200 })

    const store = useModelResourceStore()
    store.providerFormRef = { validate: vi.fn().mockResolvedValue(undefined) }
    store.handleAddProvider()
    store.providerModal.form.providerName = '默认 DeepSeek'
    store.providerModal.form.providerType = 'deepseek'
    store.providerModal.form.apiKey = 'sk-test'
    store.providerModal.form.isDefault = '1'

    await store.handleSaveProvider()

    expect(mocks.providerAdd).toHaveBeenCalledTimes(1)
    expect(mocks.providerSetDefault).toHaveBeenCalledWith(101)
    expect(window.$message.success).toHaveBeenCalledWith('新增成功')
  })

  it('switches default provider via handleSetDefault', async () => {
    mocks.providerSetDefault.mockResolvedValue({ code: 200 })

    const store = useModelResourceStore()
    await store.handleSetDefault({ id: 101, providerName: 'DeepSeek', isDefault: '0' })

    expect(mocks.providerSetDefault).toHaveBeenCalledWith(101)
    expect(window.$message.success).toHaveBeenCalledWith('已将「DeepSeek」设为默认供应商')
  })
})
