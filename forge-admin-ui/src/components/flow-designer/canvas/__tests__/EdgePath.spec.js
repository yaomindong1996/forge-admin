import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import EdgeLayer from '../EdgeLayer.vue'
import EdgePath from '../EdgePath.vue'

describe('edgePath - 渲染', () => {
  it('渲染 path d + 默认箭头 marker', () => {
    const wrapper = mount(EdgePath, {
      props: {
        edge: { id: 'F1', source: 'A', target: 'B', condition: '', isDefault: false },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
      },
    })
    const path = wrapper.find('path')
    expect(path.exists()).toBe(true)
    expect(path.attributes('d')).toContain('M 0,0')
    expect(path.attributes('marker-end')).toContain('arrow-default')
  })

  it('isDefault 边渲染 "默认" 标签', () => {
    const wrapper = mount(EdgePath, {
      props: {
        edge: { id: 'F1', source: 'A', target: 'B', condition: '', isDefault: true },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
      },
    })
    expect(wrapper.text()).toContain('默认')
    expect(wrapper.find('path').attributes('marker-end')).toContain('default-branch')
  })

  it('condition 边渲染条件文本（截断 20 字符）', () => {
    const long = '1234567890ABCDEFGHIJKLMNOP'
    const wrapper = mount(EdgePath, {
      props: {
        edge: { id: 'F1', source: 'A', target: 'B', condition: long, isDefault: false },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
      },
    })
    expect(wrapper.text()).toContain('1234567890ABCDEFGHIJ')
    expect(wrapper.text()).toContain('…')
  })

  it('status=rejected 使用红色箭头 + 虚线', () => {
    const wrapper = mount(EdgePath, {
      props: {
        edge: { id: 'F1', source: 'A', target: 'B' },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
        status: 'rejected',
      },
    })
    const path = wrapper.find('path')
    expect(path.attributes('marker-end')).toContain('arrow-rejected')
    expect(path.attributes('stroke-dasharray')).toBe('6 4')
  })
})

describe('edgeLayer - 渲染', () => {
  it('渲染 5 种 marker + 多条 edges', () => {
    const edges = [
      { id: 'F1', source: 'A', target: 'B' },
      { id: 'F2', source: 'B', target: 'C' },
    ]
    const paths = new Map([
      ['F1', { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' }],
      ['F2', { points: [{ x: 100, y: 100 }, { x: 200, y: 200 }], type: 'straight' }],
    ])
    const wrapper = mount(EdgeLayer, {
      props: { edges, paths, canvasBounds: { minX: 0, minY: 0, maxX: 200, maxY: 200 } },
    })
    expect(wrapper.findAll('marker').length).toBe(5)
    expect(wrapper.findAllComponents(EdgePath).length).toBe(2)
    const svg = wrapper.find('svg')
    expect(Number(svg.attributes('width'))).toBeGreaterThan(0)
  })

  it('空 edges 数组不报错', () => {
    const wrapper = mount(EdgeLayer, { props: { edges: [], paths: new Map() } })
    expect(wrapper.findAllComponents(EdgePath).length).toBe(0)
  })
})
