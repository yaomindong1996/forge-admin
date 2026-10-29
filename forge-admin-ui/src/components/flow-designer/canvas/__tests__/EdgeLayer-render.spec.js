import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import EdgeLayer from '../EdgeLayer.vue'

describe('EdgeLayer 可见描边', () => {
  it('neutral 交互态必须渲染带实色 stroke 的 path', () => {
    const edges = [{ id: 'F1', source: 'A', target: 'B' }]
    const paths = new Map([
      ['F1', { points: [{ x: 100, y: 50 }, { x: 100, y: 150 }], type: 'straight' }],
    ])
    const wrapper = mount(EdgeLayer, {
      props: {
        edges,
        paths,
        canvasBounds: { minX: 80, minY: 40, maxX: 700, maxY: 760 },
        neutral: true,
        interactive: true,
      },
    })
    const linePaths = wrapper.findAll('path').filter(p => (p.attributes('d') || '').includes('M 100'))
    expect(linePaths.length).toBeGreaterThanOrEqual(1)
    const painted = linePaths.find(p => p.attributes('stroke') !== 'transparent')
    expect(painted).toBeTruthy()
    const style = painted.attributes('style') || ''
    expect(style.includes('stroke') || painted.attributes('stroke')).toBeTruthy()
  })

  it('paths 用普通对象也能取到折线', () => {
    const wrapper = mount(EdgeLayer, {
      props: {
        edges: [{ id: 'F1', source: 'A', target: 'B' }],
        paths: {
          F1: { points: [{ x: 10, y: 10 }, { x: 10, y: 80 }], type: 'straight' },
        },
        canvasBounds: { minX: 0, minY: 0, maxX: 200, maxY: 200 },
        neutral: true,
        interactive: true,
      },
    })
    const linePaths = wrapper.findAll('path').filter(p => (p.attributes('d') || '').includes('M 10,10'))
    expect(linePaths.length).toBeGreaterThanOrEqual(1)
  })
})
