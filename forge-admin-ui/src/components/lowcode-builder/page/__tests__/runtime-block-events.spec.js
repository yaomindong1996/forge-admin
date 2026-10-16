import { beforeEach, describe, expect, it, vi } from 'vitest'
import { filterRuntimeBlock, registerRuntimeBlock, refreshRuntimeBlock } from '../runtime-block-registry'
import {
  applyPageNavigationQuery,
  navigateRuntimeEvent,
  resolveRuntimeEventMessageContent,
  runBlockClickEvent,
} from '../runtime-block-events'

describe('runtime-block-events', () => {
  beforeEach(() => {
    window.$message = { info: vi.fn(), warning: vi.fn() }
  })

  it('resolves message content from nameless static params', () => {
    const text = resolveRuntimeEventMessageContent({
      action: 'message',
      description: 'fallback',
      params: [{ name: '', value: 'from-param', sourceType: 'static' }],
    }, v => v, param => param.value)
    expect(text).toBe('from-param')
  })

  it('prefers explicit message field over description', () => {
    const text = resolveRuntimeEventMessageContent({
      action: 'message',
      message: 'direct-message',
      description: 'fallback',
    }, v => v, () => '')
    expect(text).toBe('direct-message')
  })

  it('runs message action and shows toast', async () => {
    await runBlockClickEvent({
      action: 'message',
      description: 'hello-e2e',
    }, {
      resolveRuntimeText: v => v,
      resolveRuntimeParamValue: () => '',
    })
    expect(window.$message.info).toHaveBeenCalledWith('hello-e2e')
  })

  it('prefers ctx.message over window.$message for toasts', async () => {
    const ctxMessage = { info: vi.fn(), success: vi.fn() }
    await runBlockClickEvent({
      action: 'message',
      message: 'from-ctx',
      messageType: 'success',
    }, {
      message: ctxMessage,
      resolveRuntimeText: v => v,
      resolveRuntimeParamValue: () => '',
    })
    expect(ctxMessage.success).toHaveBeenCalledWith('from-ctx')
    expect(window.$message.info).not.toHaveBeenCalled()
  })

  it('resolves back-button style param message for click events', () => {
    const text = resolveRuntimeEventMessageContent({
      action: 'message',
      description: 'E2E_back-button_click',
      params: [{ name: '', value: 'E2E_back-button_event', sourceType: 'static' }],
    }, v => v, param => param.value)
    expect(text).toBe('E2E_back-button_event')
  })

  it('refreshes target block via registry', async () => {
    const refresh = vi.fn()
    const unregister = registerRuntimeBlock('blk_list', { refresh })
    await runBlockClickEvent({
      action: 'refreshBlock',
      targetBlockId: 'blk_list',
    }, {
      resolveRuntimeText: v => v,
      resolveRuntimeParamValue: () => '',
      selfBlockId: 'btn_1',
    })
    expect(refresh).toHaveBeenCalled()
    unregister()
  })

  it('filters target block with params', async () => {
    const search = vi.fn()
    const unregister = registerRuntimeBlock('blk_list', { search })
    await runBlockClickEvent({
      action: 'filterBlock',
      targetBlockId: 'blk_list',
      params: [{ name: 'E2E_input', value: 'abc', sourceType: 'static' }],
    }, {
      resolveRuntimeText: v => v,
      resolveRuntimeParamValue: param => param.value,
      selfBlockId: 'btn_1',
    })
    expect(search).toHaveBeenCalledWith({ E2E_input: 'abc' })
    unregister()
  })
})

describe('applyPageNavigationQuery', () => {
  it('keeps application pageId and sets object multi-page pageKey', () => {
    const query = applyPageNavigationQuery(
      { pageId: 'page_page_u6xdzs', draft: '1' },
      'e2e_dialog',
      () => true,
    )
    expect(query).toEqual({ pageId: 'page_page_u6xdzs', draft: '1', pageKey: 'e2e_dialog' })
  })

  it('switches application pages with pageId for page_page_* targets', () => {
    const query = applyPageNavigationQuery(
      { pageId: 'page_page_u6xdzs', pageKey: 'list' },
      'page_page_other',
      () => true,
    )
    expect(query.pageId).toBe('page_page_other')
    expect(query.pageKey).toBeUndefined()
  })

  it('uses pageKey on non-application routes', () => {
    const query = applyPageNavigationQuery({}, 'detail', () => false)
    expect(query).toEqual({ pageKey: 'detail' })
  })

  it('navigateRuntimeEvent writes pageKey inside application page', () => {
    const push = vi.fn()
    navigateRuntimeEvent(
      { targetPageKey: 'e2e_dialog' },
      {
        route: { path: '/app/hr', query: { pageId: 'page_page_u6xdzs' }, hash: '' },
        router: { push },
        isApplicationRuntimeRoute: () => true,
      },
    )
    expect(push).toHaveBeenCalledWith({
      path: '/app/hr',
      query: { pageId: 'page_page_u6xdzs', pageKey: 'e2e_dialog' },
      hash: '',
    })
  })
})

describe('runtime-block-registry', () => {
  it('refresh and filter helpers return false when missing', () => {
    expect(refreshRuntimeBlock('missing')).toBe(false)
    expect(filterRuntimeBlock('missing', {})).toBe(false)
  })

  it('opens form via showAdd on registered api', async () => {
    const showAdd = vi.fn()
    const unregister = registerRuntimeBlock('blk_list', { showAdd })
    await runBlockClickEvent({
      action: 'openModal',
      targetBlockId: 'blk_list',
    }, {
      resolveRuntimeText: v => v,
      resolveRuntimeParamValue: () => '',
      selfBlockId: 'btn_1',
    })
    expect(showAdd).toHaveBeenCalled()
    unregister()
  })
})
