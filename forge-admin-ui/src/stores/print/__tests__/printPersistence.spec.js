import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/api/print'
import { createPrintDocument } from '@/components/print/protocol/types'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { usePrintRuntimeStore } from '@/stores/print/printRuntimeStore'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

vi.mock('@/api/print', () => ({ printTemplate: vi.fn(), printCatalog: vi.fn(), updatePrintTemplate: vi.fn(), publishPrintTemplate: vi.fn(), printVersions: vi.fn(), printVersion: vi.fn(), availablePrintTemplates: vi.fn(), preparePrint: vi.fn(), recordPrintEvent: vi.fn(), printTemplates: vi.fn() }))
const source = { applicationId: '2', sourceType: 'LOWCODE', pageId: '3', objectCode: 'purchase' }
const record = { source, recordId: 'saved_record', scene: 'DETAIL' }
const row = (id = '1', revision = 1) => ({ id, source, templateName: '合成模板', draftRevision: revision, schemaJson: JSON.stringify(createPrintDocument()) })
function deferred() {
  let resolve
  let reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}
const prepared = (id = '1') => ({ executionId: id, schemaJson: row().schemaJson, context: { main: { value: 'SYNTHETIC' }, children: {}, flow: {} }, catalog: { fields: [] }, generatedAt: '2026-09-19T06:00:00', dataMode: 'CURRENT' })
beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
  api.printTemplate.mockResolvedValue({ data: row() })
  api.printCatalog.mockResolvedValue({ data: { fields: [] } })
})
describe('服务端草稿持久化', () => {
  it('保存回包不覆盖等待期间继续编辑的名称和画布', async () => {
    const store = usePrintTemplateStore()
    await store.open('1')
    const designer = usePrintDesignerStore()
    designer.load(store.document, store.catalog)
    designer.execute((doc) => {
      doc.paper.marginMm.top = 12
    })
    store.name = '发出时名称'
    const wait = deferred()
    api.updatePrintTemplate.mockReturnValue(wait.promise)
    const saving = designer.save(store.save)
    designer.execute((doc) => {
      doc.paper.marginMm.top = 16
    })
    store.name = '继续修改名称'
    wait.resolve({ data: { ...row('1', 2), templateName: '发出时名称' } })
    await saving
    expect(store.row.draftRevision).toBe(2)
    expect(store.name).toBe('继续修改名称')
    expect(store.nameDirty).toBe(true)
    expect(designer.document.paper.marginMm.top).toBe(16)
    expect(designer.dirty).toBe(true)
    expect(JSON.parse(api.updatePrintTemplate.mock.calls[0][1].schemaJson).paper.marginMm.top).toBe(12)
  })
  it('409 保留未保存内容且不伪报成功', async () => {
    const store = usePrintTemplateStore()
    await store.open('1')
    const designer = usePrintDesignerStore()
    designer.load(store.document, [])
    designer.execute((doc) => {
      doc.paper.marginMm.top = 18
    })
    api.updatePrintTemplate.mockRejectedValue({ code: 409, message: '修订冲突，请重新载入' })
    expect(await designer.save(store.save)).toBe(false)
    expect(designer.dirty).toBe(true)
    expect(store.row.draftRevision).toBe(1)
    expect(store.error).toContain('修订冲突')
  })
  it('切换模板后丢弃旧读取和旧保存回包', async () => {
    const store = usePrintTemplateStore()
    const wait = deferred()
    api.printTemplate.mockReturnValueOnce(wait.promise)
    const old = store.open('1')
    api.printTemplate.mockResolvedValueOnce({ data: row('2') })
    await store.open('2')
    wait.resolve({ data: row('1') })
    await old
    expect(store.row.id).toBe('2')
    const saveWait = deferred()
    api.updatePrintTemplate.mockReturnValue(saveWait.promise)
    const saving = store.save(store.document)
    store.clear()
    saveWait.resolve({ data: row('2', 2) })
    await saving
    expect(store.row).toBeNull()
    expect(store.document).toBeNull()
  })
  it('历史版本只作为候选返回，不改变当前修订和保存基线', async () => {
    const store = usePrintTemplateStore()
    await store.open('1')
    api.printVersion.mockResolvedValue({ data: { schemaJson: row().schemaJson } })
    const document = await store.versionDocument('22')
    expect(document.protocol).toBe('forge-print')
    expect(store.row.draftRevision).toBe(1)
  })
  it('只将已保存修订发布，失败保留版本指针', async () => {
    const store = usePrintTemplateStore()
    await store.open('1')
    api.publishPrintTemplate.mockRejectedValue(new Error('字段已删除'))
    expect(await store.publish()).toBe(false)
    expect(store.row.draftRevision).toBe(1)
    expect(api.publishPrintTemplate).toHaveBeenCalledWith('1', { expectedRevision: 1 })
  })
})
describe('运行准备隔离', () => {
  it('默认模板自动准备并保留服务端生成时刻', async () => {
    api.availablePrintTemplates.mockResolvedValue({ data: [{ id: '8', isDefault: true }] })
    api.preparePrint.mockResolvedValue({ data: prepared() })
    const store = usePrintRuntimeStore()
    await store.open(record)
    expect(api.preparePrint).toHaveBeenCalledWith(record, '8')
    expect(store.loading).toBe(false)
    expect(store.prepared.context.system.generatedAt).toBe('2026-09-19T06:00:00')
  })
  it('关闭会清空记录数据并忽略迟到的 prepare 响应', async () => {
    const wait = deferred()
    api.availablePrintTemplates.mockResolvedValue({ data: [] })
    api.preparePrint.mockReturnValue(wait.promise)
    const store = usePrintRuntimeStore()
    await store.open(record)
    const pending = store.select('1')
    store.close()
    wait.resolve({ data: prepared() })
    await pending
    expect(store.prepared).toBeNull()
    expect(store.record).toBeNull()
    expect(store.options).toEqual([])
  })
  it('重复选择时仅展示最后一次成功结果，失败无旧数据兜底', async () => {
    api.availablePrintTemplates.mockResolvedValue({ data: [] })
    const wait = deferred()
    api.preparePrint.mockReturnValueOnce(wait.promise).mockResolvedValueOnce({ data: prepared('2') })
    const store = usePrintRuntimeStore()
    await store.open(record)
    const old = store.select('1')
    await store.select('2')
    wait.resolve({ data: prepared('1') })
    await old
    expect(store.prepared.executionId).toBe('2')
    api.preparePrint.mockRejectedValue(new Error('无权限'))
    await store.select('1')
    expect(store.prepared).toBeNull()
    expect(store.error).toBe('无权限')
  })
  it('执行事件失败可以重试，错误上报只有白名单码', async () => {
    const store = usePrintRuntimeStore()
    store.prepared = prepared()
    api.recordPrintEvent.mockRejectedValueOnce(new Error('网络故障')).mockResolvedValueOnce({ data: {} })
    await store.failed(new Error('业务正文不能上报'))
    expect(api.recordPrintEvent).toHaveBeenLastCalledWith('1', { result: 'FAILED', errorCode: 'PRINT_FAILED' })
    expect(store.eventError).not.toBe('')
    await store.event(store.pendingEvent)
    expect(store.eventSent).toBe(true)
    await store.event({ result: 'DIALOG_OPENED', pageCount: 1 })
    expect(api.recordPrintEvent).toHaveBeenCalledTimes(2)
  })
})
