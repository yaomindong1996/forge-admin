import { describe, expect, it, vi } from 'vitest'
import { leavePrintDesigner, leavePrintPreview, printPageSettingsLocation, printPreviewFallbackLocation, printRecordFromQuery, printSourceFromQuery, printSourcePayload } from '../printRouteContext'

const source = { applicationId: '9007199254740993', sourceType: 'LOWCODE', pageId: 'page_purchase', objectCode: 'purchase' }
describe('打印工作台页面身份', () => {
  it('保留页面标识和大整数应用 ID，不转为数字', () => {
    expect(printSourceFromQuery(source)).toEqual({ ...source, formKey: null })
    expect(printSourcePayload(source)).toEqual(source)
    expect(printSourcePayload({ ...source, formKey: null })).not.toHaveProperty('formKey')
    expect(printSourceFromQuery({ ...source, pageId: '9007199254740993' }).pageId).toBe('9007199254740993')
    expect(printRecordFromQuery({ ...source, recordId: 'saved_record', scene: 'DETAIL' }).source.pageId).toBe('page_purchase')
  })
  it.each(['', ' ', 'a/b', 'a\nb', 'a'.repeat(129), ['page_purchase']])('拒绝无效页面标识 %s', (pageId) => {
    expect(printSourceFromQuery({ ...source, pageId })).toBeNull()
  })
  it('代码表单身份保持独立', () => {
    expect(printSourceFromQuery({ ...source, sourceType: 'CODE', formKey: 'purchase_form' })).toEqual({ ...source, sourceType: 'CODE', pageId: null, formKey: 'purchase_form' })
  })
  it('独立业务来源保持长整型标识且不混入应用字段', () => {
    const standalone = {
      businessSourceId: '9007199254740995',
      sourceCode: 'purchase_order',
      sourceType: 'SERVICE',
      objectCode: 'purchase_order',
    }
    expect(printSourcePayload(standalone)).toEqual(standalone)
    expect(printSourceFromQuery(standalone)).toEqual({
      ...standalone,
      applicationId: null,
      pageId: null,
      formKey: null,
    })
    expect(printPreviewFallbackLocation({ source: standalone, scene: 'DETAIL' })).toEqual({
      path: '/print',
      query: { businessSourceId: standalone.businessSourceId },
    })
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

describe('打印设计器返回', () => {
  const source = { applicationId: '2', sourceType: 'LOWCODE', pageId: 'page_purchase', objectCode: 'purchase' }

  it('优先回到来源页面，而不是独立打印列表', () => {
    const router = { back: vi.fn(), replace: vi.fn() }
    expect(leavePrintDesigner({
      router,
      route: { query: { from: '/app-center/application/buy/runtime?edit=1&pageId=page_purchase&designTab=settings&settingsSection=printing' } },
      source,
    })).toBe('from')
    expect(router.replace).toHaveBeenCalledWith('/app-center/application/buy/runtime?edit=1&pageId=page_purchase&designTab=settings&settingsSection=printing')
    expect(router.back).not.toHaveBeenCalled()
  })

  it('没有来源页时回到当前页面的打印设置', () => {
    expect(printPageSettingsLocation({ applicationCode: 'BuySale', pageId: 'page_purchase' })).toEqual({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: 'BuySale' },
      query: {
        edit: '1',
        pageId: 'page_purchase',
        designTab: 'settings',
        settingsSection: 'printing',
      },
    })
    const router = { back: vi.fn(), replace: vi.fn() }
    expect(leavePrintDesigner({
      router,
      route: { query: { applicationCode: 'BuySale', templateId: '2' } },
      source,
    })).toBe('fallback')
    expect(router.replace).toHaveBeenCalledWith(printPageSettingsLocation({ applicationCode: 'BuySale', pageId: 'page_purchase' }))
    expect(router.back).not.toHaveBeenCalled()
  })

  it('独立打印列表和旧应用设置都不是合法返回地址', () => {
    const router = { back: vi.fn(), replace: vi.fn() }
    expect(leavePrintDesigner({
      router,
      route: { query: { from: '/print?applicationId=2', applicationCode: 'BuySale', templateId: '2' } },
      source,
    })).toBe('fallback')
    expect(leavePrintDesigner({
      router,
      route: { query: { from: '/app-center/application/buy/runtime?view=settings&settingsSection=printing', applicationCode: 'BuySale' } },
      source,
    })).toBe('fallback')
    expect(router.back).not.toHaveBeenCalled()
    expect(router.replace).toHaveBeenCalledWith(printPageSettingsLocation({ applicationCode: 'BuySale', pageId: 'page_purchase' }))
  })

  it('独立来源设计完成后回到打印中心当前来源', () => {
    const router = { replace: vi.fn() }
    expect(leavePrintDesigner({
      router,
      route: { query: {} },
      source: {
        businessSourceId: '9007199254740995',
        sourceCode: 'purchase_order',
        sourceType: 'SERVICE',
        objectCode: 'purchase_order',
      },
    })).toBe('source')
    expect(router.replace).toHaveBeenCalledWith({
      path: '/print',
      query: { businessSourceId: '9007199254740995' },
    })
  })
})
