import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { businessTaskFormReadonlyContext } from '@/api/business-app'
import flowApi from '@/api/flow'
import { resolveFlowPrintRecord, useFlowPrintContextStore } from '@/stores/print/flowPrintContextStore'

vi.mock('@/api/business-app', () => ({
  businessTaskFormContext: vi.fn(),
  businessTaskFormReadonlyContext: vi.fn(),
}))

vi.mock('@/api/flow', () => ({
  default: {
    getTaskFormInfo: vi.fn(),
    getProcessFormInfo: vi.fn(),
  },
}))

function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { promise, resolve }
}

const codeContext = {
  configured: true,
  formType: 'business-code',
  applicationId: '7',
  objectCode: 'sample_purchase_order',
  recordId: '99',
  formKey: 'sample_purchase_order_approval_form',
  processInstanceId: 'process-1',
  taskId: 'task-1',
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
})

describe('flowPrintContextStore', () => {
  it('代码业务待办只生成稳定身份，不保留业务正文', () => {
    const record = resolveFlowPrintRecord({
      row: { taskId: 'task-1', processInstanceId: 'process-1' },
      scene: 'FLOW_TODO',
      formInfo: { formMode: 'BUSINESS_CODE_FORM', formKey: 'sample_purchase_order_approval_form' },
      businessContext: { ...codeContext, recordData: { secret: '不得进入打印请求' } },
    })

    expect(record).toEqual({
      source: {
        applicationId: '7',
        sourceType: 'CODE',
        pageId: null,
        formKey: 'sample_purchase_order_approval_form',
        objectCode: 'sample_purchase_order',
      },
      recordId: '99',
      scene: 'FLOW_TODO',
      taskId: 'task-1',
      processInstanceId: 'process-1',
    })
    expect(JSON.stringify(record)).not.toContain('不得进入打印请求')
  })

  it('记录标识缺失时只从匹配对象的业务键提取记录段', () => {
    const record = resolveFlowPrintRecord({
      row: { taskId: 'task-1', processInstanceId: 'process-1' },
      scene: 'FLOW_TODO',
      formInfo: { formMode: 'BUSINESS_CODE_FORM', formKey: 'sample_purchase_order_approval_form' },
      businessContext: {
        ...codeContext,
        recordId: null,
        businessKey: 'sample_purchase_order:9007199254740993:R2',
      },
    })

    expect(record.recordId).toBe('9007199254740993')
  })

  it('低代码我发起场景携带固定页面和运行版本且不发送 taskId', () => {
    const record = resolveFlowPrintRecord({
      row: { processInstanceId: 'process-2' },
      scene: 'FLOW_STARTED',
      formInfo: { variables: { processRunId: '81' } },
      businessContext: {
        configured: true,
        formType: 'business-object',
        applicationId: '8',
        pageId: 'page_invoice',
        objectCode: 'invoice',
        recordId: '9007199254740993',
        processInstanceId: 'process-2',
      },
    })

    expect(record).toMatchObject({
      source: { applicationId: '8', sourceType: 'LOWCODE', pageId: 'page_invoice', formKey: null, objectCode: 'invoice' },
      recordId: '9007199254740993',
      scene: 'FLOW_STARTED',
      processInstanceId: 'process-2',
      processRunId: '81',
    })
    expect(record).not.toHaveProperty('taskId')
  })

  it('低代码流程缺少运行版本时拒绝准备', () => {
    expect(() => resolveFlowPrintRecord({
      row: { taskId: 'task-2', processInstanceId: 'process-2' },
      scene: 'FLOW_DONE',
      formInfo: {},
      businessContext: {
        configured: true,
        formType: 'business-object',
        applicationId: '8',
        pageId: 'page_invoice',
        objectCode: 'invoice',
        recordId: '2',
        processInstanceId: 'process-2',
        taskId: 'task-2',
      },
    })).toThrow('运行版本身份')
  })

  it('切换任务后丢弃旧请求回包', async () => {
    const oldContext = deferred()
    flowApi.getProcessFormInfo.mockResolvedValueOnce({ code: 200, data: { formMode: 'BUSINESS_CODE_FORM', formKey: codeContext.formKey } })
    businessTaskFormReadonlyContext.mockReturnValueOnce(oldContext.promise)
    const store = useFlowPrintContextStore()
    const old = store.sync({ row: { taskId: 'task-old', processInstanceId: 'process-old' }, scene: 'FLOW_DONE' })

    const current = await store.sync({
      row: { taskId: 'task-1', processInstanceId: 'process-1' },
      scene: 'FLOW_DONE',
      formInfo: { formMode: 'BUSINESS_CODE_FORM', formKey: codeContext.formKey },
      businessContext: codeContext,
    })
    oldContext.resolve({ code: 200, data: { ...codeContext, taskId: 'task-old', processInstanceId: 'process-old' } })
    await old

    expect(current.taskId).toBe('task-1')
    expect(store.record.taskId).toBe('task-1')
    expect(store.identityKey).toContain('task-1')
  })
})
