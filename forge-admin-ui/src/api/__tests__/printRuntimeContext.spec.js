import { expect, it, vi } from 'vitest'
import { request } from '@/utils'
import { crudConfigRender } from '../ai/lowcode'

vi.mock('@/utils', () => ({ request: { get: vi.fn() } }))
it('正式页面上下文通过查询参数发送，不残留为 Axios 顶层参数', () => {
  const options = { applicationId: '9007199254740993', appId: '7', pageId: 'page_purchase', needTip: false }
  crudConfigRender('purchase', false, options)
  expect(request.get).toHaveBeenLastCalledWith('/ai/crud-config/render/purchase', { needTip: false, params: { applicationId: '9007199254740993', appId: '7', pageId: 'page_purchase' } })
  expect(options.pageId).toBe('page_purchase')
})
it('旧调用与草稿标志兼容', () => {
  crudConfigRender('purchase', true, { params: { custom: 'keep' } })
  expect(request.get).toHaveBeenLastCalledWith('/ai/crud-config/render/purchase', { params: { custom: 'keep', designPreview: true } })
})
