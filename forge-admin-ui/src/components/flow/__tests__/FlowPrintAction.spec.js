import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FlowPrintAction from '@/components/flow/FlowPrintAction.vue'
import { useFlowPrintContextStore } from '@/stores/print/flowPrintContextStore'

vi.mock('@/api/business-app', () => ({
  businessTaskFormContext: vi.fn(),
  businessTaskFormReadonlyContext: vi.fn(),
}))
vi.mock('@/api/flow', () => ({ default: { getTaskFormInfo: vi.fn(), getProcessFormInfo: vi.fn() } }))

const record = {
  source: { applicationId: '7', sourceType: 'CODE', pageId: null, formKey: 'purchase_form', objectCode: 'purchase' },
  recordId: '99',
  scene: 'FLOW_TODO',
  taskId: 'task-1',
  processInstanceId: 'process-1',
}

function mountAction(props = {}) {
  return mount(FlowPrintAction, {
    props: {
      row: { taskId: 'task-1', processInstanceId: 'process-1' },
      scene: 'FLOW_TODO',
      ...props,
    },
    global: {
      stubs: {
        NButton: { props: ['disabled', 'loading'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot name="icon"/><slot/></button>' },
        NModal: { props: ['show'], template: '<section v-if="show" class="print-modal"><slot/></section>' },
        NAlert: { template: '<div><slot/></div>' },
        PrintTemplatePicker: { props: ['record'], template: '<div class="print-picker">{{ record.taskId }}</div>' },
      },
    },
  })
}

beforeEach(() => {
  setActivePinia(createPinia())
  window.$message = { error: vi.fn() }
  window.$dialog = undefined
})

afterEach(() => {
  delete window.$message
  delete window.$dialog
})

describe('flowPrintAction', () => {
  it('有未保存修改时确认后才打开已保存数据打印', async () => {
    let dialogOptions
    window.$dialog = { warning: vi.fn((options) => {
      dialogOptions = options
    }) }
    const store = useFlowPrintContextStore()
    store.sync = vi.fn(async () => {
      store.record = record
      return record
    })
    const wrapper = mountAction({ dirty: true })

    await wrapper.get('button').trigger('click')
    expect(store.sync).not.toHaveBeenCalled()
    expect(dialogOptions.content).toContain('已保存记录')

    dialogOptions.onPositiveClick()
    await flushPromises()
    expect(store.sync).toHaveBeenCalledTimes(1)
    expect(wrapper.vm.visible).toBe(true)
    wrapper.unmount()
  })

  it('切换当前任务会关闭预览并清理旧上下文', async () => {
    const store = useFlowPrintContextStore()
    store.sync = vi.fn(async ({ row }) => {
      const next = { ...record, taskId: row.taskId, processInstanceId: row.processInstanceId }
      store.record = next
      return next
    })
    const wrapper = mountAction()
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.vm.visible).toBe(true)

    await wrapper.setProps({ row: { taskId: 'task-2', processInstanceId: 'process-2' } })
    await flushPromises()
    expect(wrapper.vm.visible).toBe(false)
    expect(store.record).toBeNull()
    wrapper.unmount()
  })
})
