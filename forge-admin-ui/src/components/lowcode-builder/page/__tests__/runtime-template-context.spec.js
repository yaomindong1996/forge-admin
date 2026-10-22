import { describe, expect, it } from 'vitest'
import {
  buildRuntimeTemplateContext,
  interpolateApiUrl,
  interpolateText,
} from '../gridBlockRendererUtils'
import { resolveDefaultPageBlockHeight } from '@/views/app-center/runtime-modules/page-flow-geometry'

describe('runtime template context for detail pages', () => {
  it('exposes query/params as top-level and route.* aliases', () => {
    const ctx = buildRuntimeTemplateContext({
      query: { id: 'q-1', tab: ['a', 'b'] },
      params: { id: 'p-1' },
      record: { name: '订单' },
    })
    expect(ctx.id).toBe('p-1')
    expect(ctx.tab).toBe('a')
    expect(ctx.query.id).toBe('q-1')
    expect(ctx.params.id).toBe('p-1')
    expect(ctx.route.id).toBe('p-1')
    expect(ctx.route.params.id).toBe('p-1')
    expect(ctx.name).toBe('订单')
  })

  it('interpolates mustache and :id style detail URLs', () => {
    const ctx = buildRuntimeTemplateContext({ query: { id: '88' } })
    expect(interpolateText('get@/api/order/{{ id }}', ctx)).toBe('get@/api/order/88')
    expect(interpolateApiUrl('/ai/crud/demo/{{ id }}', ctx)).toBe('/ai/crud/demo/88')
    expect(interpolateApiUrl('/ai/crud/demo/:id', ctx)).toBe('/ai/crud/demo/88')
    expect(interpolateApiUrl('/ai/crud/demo/:id', {})).toBe('/ai/crud/demo/:id')
  })

  it('sizes AiForm and detail-info shells by field count', () => {
    const formTall = resolveDefaultPageBlockHeight({
      blockType: 'AiForm',
      fieldRefs: Array.from({ length: 12 }, (_, i) => `f${i}`),
      props: { gridCols: 2 },
    })
    const formShort = resolveDefaultPageBlockHeight({
      blockType: 'AiForm',
      fieldRefs: ['a', 'b'],
      props: { gridCols: 2 },
    })
    expect(formTall).toBeGreaterThan(formShort)
    expect(formTall).toBeGreaterThanOrEqual(280)

    const detail = resolveDefaultPageBlockHeight({
      blockType: 'detail-info',
      fieldRefs: Array.from({ length: 8 }, (_, i) => `d${i}`),
      props: { columnCount: 2 },
    })
    expect(detail).toBeGreaterThanOrEqual(220)
  })
})
