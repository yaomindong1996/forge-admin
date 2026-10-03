import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, reactive, ref } from 'vue'
import { useDatasetWorkspaceContext, useDatasetWorkspaceStore } from '../datasetWorkspaceStore'

beforeEach(() => setActivePinia(createPinia()))

describe('dataset shared panel context', () => {
  it('keeps refs, computed values, form fields and action callbacks linked to the original API', () => {
    const currentStep = ref(1)
    const queryForm = reactive({ datasetName: '' })
    const save = vi.fn()
    const api = { currentStep, queryForm, readonly: computed(() => currentStep.value === 4), save }
    const store = useDatasetWorkspaceStore()
    store.bind(api)
    const panel = useDatasetWorkspaceContext()
    panel.currentStep.value = 4
    panel.queryForm.value.datasetName = '更新名称'
    panel.save.value()
    expect(currentStep.value).toBe(4)
    expect(panel.readonly.value).toBe(true)
    expect(queryForm.datasetName).toBe('更新名称')
    expect(save).toHaveBeenCalledOnce()
  })

  it('does not let an old page clear a newly bound page', () => {
    const store = useDatasetWorkspaceStore()
    const oldApi = { currentStep: ref(1) }
    const nextApi = { currentStep: ref(2) }
    store.bind(oldApi)
    store.bind(nextApi)
    store.release(oldApi)
    expect(store.context.currentStep).toBe(2)
    store.release(nextApi)
    expect(store.context).toBeNull()
    expect(() => useDatasetWorkspaceContext()).toThrow('尚未初始化')
  })
})
