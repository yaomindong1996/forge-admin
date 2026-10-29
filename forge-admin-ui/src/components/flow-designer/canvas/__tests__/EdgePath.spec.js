import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BranchHeader from '../BranchHeader.vue'
import EdgeLayer from '../EdgeLayer.vue'
import EdgePath from '../EdgePath.vue'

const DOLLAR = '$'

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

  it('isDefault 边有条件时仍只渲染默认标签', () => {
    const wrapper = mount(EdgePath, {
      props: {
        edge: { id: 'F1', source: 'A', target: 'B', condition: `${DOLLAR}{amount <= 1000}`, isDefault: true },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
      },
    })
    expect(wrapper.text()).toContain('默认')
    expect(wrapper.text()).not.toContain(`${DOLLAR}{amount`)
  })

  it('普通 condition 边渲染简洁条件摘要，不直接展示表达式', () => {
    const wrapper = mount(EdgePath, {
      props: {
        edge: { id: 'F1', source: 'A', target: 'B', condition: `${DOLLAR}{amount > 1000}`, isDefault: false },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
      },
    })
    expect(wrapper.text()).toContain('条件已设')
    expect(wrapper.text()).not.toContain(`${DOLLAR}{amount`)
  })

  it('网关分支边不在 SVG 层重复渲染标签', () => {
    const wrapper = mount(EdgePath, {
      props: {
        edge: {
          id: 'F1',
          source: 'GW',
          target: 'A',
          branchId: 'b1',
          condition: `${DOLLAR}{amount > 1000}`,
          isDefault: false,
        },
        path: { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' },
      },
    })
    expect(wrapper.text()).toBe('')
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
    expect(wrapper.findAll('marker').length).toBe(6)
    expect(wrapper.findAllComponents(EdgePath).length).toBe(2)
    const svg = wrapper.find('svg')
    expect(Number(svg.attributes('width'))).toBeGreaterThan(0)
  })

  it('空 edges 数组不报错', () => {
    const wrapper = mount(EdgeLayer, { props: { edges: [], paths: new Map() } })
    expect(wrapper.findAllComponents(EdgePath).length).toBe(0)
  })

  it('showLabels=false 时不在 SVG 连线层渲染条件标签', () => {
    const edges = [
      { id: 'F1', source: 'GW', target: 'A', condition: `${DOLLAR}{amount > 1000}`, isDefault: false },
    ]
    const paths = new Map([
      ['F1', { points: [{ x: 0, y: 0 }, { x: 100, y: 100 }], type: 'straight' }],
    ])
    const wrapper = mount(EdgeLayer, {
      props: {
        edges,
        paths,
        canvasBounds: { minX: 0, minY: 0, maxX: 100, maxY: 100 },
        showLabels: false,
      },
    })
    expect(wrapper.text()).not.toContain('条件已设')
  })
})

describe('branchHeader - 条件摘要', () => {
  it('配置条件后只展示摘要，不直接展示 SpEL 原文', () => {
    const wrapper = mount(BranchHeader, {
      props: {
        edge: { id: 'F1', condition: `${DOLLAR}{amount > 1000}`, isDefault: false },
        position: { x: 100, y: 80 },
      },
    })
    expect(wrapper.text()).toContain('条件已设')
    expect(wrapper.text()).not.toContain(`${DOLLAR}{amount`)
    expect(wrapper.attributes('title')).toBe(`${DOLLAR}{amount > 1000}`)
  })

  it('多条规则展示规则数量', () => {
    const wrapper = mount(BranchHeader, {
      props: {
        edge: {
          id: 'F1',
          condition: `${DOLLAR}{amount > 1000 && jtpNo == 'A'}`,
          conditionRules: [{ field: 'amount' }, { field: 'jtpNo' }],
          isDefault: false,
        },
        position: { x: 100, y: 80 },
      },
    })
    expect(wrapper.text()).toContain('2 条条件')
  })

  it('并行分支卡使用紧凑样式', () => {
    const wrapper = mount(BranchHeader, {
      props: {
        edge: { id: 'F1', isDefault: false },
        position: { x: 100, y: 80 },
        card: true,
        compact: true,
        index: 2,
        gatewayType: 'parallel',
      },
    })
    expect(wrapper.classes()).toContain('is-compact')
    expect(wrapper.text()).toContain('分支 2')
    expect(wrapper.text()).toContain('同时执行')
    expect(wrapper.find('.branch-header-arrow').exists()).toBe(false)
  })
})
