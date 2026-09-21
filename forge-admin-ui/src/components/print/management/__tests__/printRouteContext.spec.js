import { describe, expect, it, vi } from 'vitest'
import { leavePrintPreview, printPreviewFallbackLocation, printRecordFromQuery, printSourceFromQuery } from '../printRouteContext'

const source = { applicationId: '9007199254740993', sourceType: 'LOWCODE', pageId: 'page_purchase', objectCode: 'purchase' }
describe('打印工作台页面身份', () => {
  it('保留页面标识和大整数应用 ID，不转为数字', () => {
    expect(printSourceFromQuery(source)).toEqual({ ...source, formKey: null })
    expect(printSourceFromQuery({ ...source, pageId: '9007199254740993' }).pageId).toBe('9007199254740993')
    expect(printRecordFromQuery({ ...source, recordId: 'saved_record', scene: 'DETAIL' }).source.pageId).toBe('page_purchase')
  })
  it.each(['', ' ', 'a/b', 'a\nb', 'a'.repeat(129), ['page_purchase']])('拒绝无效页面标识 %s', (pageId) => {
    expect(printSourceFromQuery({ ...source, pageId })).toBeNull()
  })
  it('代码表单身份保持独立', () => {
    expect(printSourceFromQuery({ ...source, sourceType: 'CODE', formKey: 'purchase_form' })).toEqual({ ...source, sourceType: 'CODE', pageId: null, formKey: 'purchase_form' })
  })
})

describe('打印预览返回', () => {
  const record = printRecordFromQuery({ ...source, recordId: '5', scene: 'LIST' })

  it('有历史时后退，新标签没有历史则关掉或回到来源应用', () => {
    const router = { back: vi.fn(), replace: vi.fn() }
    expect(leavePrintPreview({ router, record, historyState: { back: '/app' }, closeWindow: () => false })).toBe('back')
    expect(router.back).toHaveBeenCalledOnce()
    expect(leavePrintPreview({ router, record, historyState: { back: null }, closeWindow: () => true })).toBe('close')
    expect(leavePrintPreview({ router, record, historyState: {}, closeWindow: () => false })).toBe('fallback')
    expect(router.replace).toHaveBeenCalledWith({ path: '/app-center/app/9007199254740993' })
  })

  it('流程场景回到对应待办页', () => {
    expect(printPreviewFallbackLocation({ scene: 'FLOW_TODO' })).toEqual({ path: '/flow/todo' })
    expect(printPreviewFallbackLocation({ scene: 'FLOW_DONE' })).toEqual({ path: '/flow/done' })
    expect(printPreviewFallbackLocation({ scene: 'FLOW_STARTED' })).toEqual({ path: '/flow/started' })
  })
})
