import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FlowCanvas from '../FlowCanvas.vue'

describe('flowCanvas - 基础渲染', () => {
  it('mount 成功并渲染默认工具栏', () => {
    const wrapper = mount(FlowCanvas)
    expect(wrapper.find('.flow-canvas').exists()).toBe(true)
    expect(wrapper.find('.canvas-transform').exists()).toBe(true)
    expect(wrapper.find('.canvas-toolbar').exists()).toBe(true)
    expect(wrapper.text()).toContain('100%')
  })

  it('插槽 edges / nodes 内容正确渲染', () => {
    const wrapper = mount(FlowCanvas, {
      slots: {
        edges: '<svg data-testid="edges"></svg>',
        nodes: '<div data-testid="nodes">N</div>',
        toolbar: '<button data-testid="custom-toolbar">X</button>',
      },
    })
    expect(wrapper.find('[data-testid="edges"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="nodes"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="custom-toolbar"]').exists()).toBe(true)
  })

  it('暴露 viewport 与缩放方法', () => {
    const wrapper = mount(FlowCanvas)
    const exposed = wrapper.vm
    expect(typeof exposed.zoomIn).toBe('function')
    expect(typeof exposed.zoomOut).toBe('function')
    expect(typeof exposed.resetView).toBe('function')
    expect(typeof exposed.fitToScreen).toBe('function')
    expect(typeof exposed.screenToCanvas).toBe('function')
    expect(typeof exposed.canvasToScreen).toBe('function')
    expect(exposed.viewport).toBeTruthy()
  })

  it('zoomIn 后 scale 增加 0.1', () => {
    const wrapper = mount(FlowCanvas)
    const exposed = wrapper.vm
    expect(exposed.viewport.scale.value).toBe(1)
    exposed.zoomIn()
    expect(exposed.viewport.scale.value).toBeCloseTo(1.1, 5)
  })

  it('canvas-transform 含 transform style', () => {
    const wrapper = mount(FlowCanvas)
    const transform = wrapper.find('.canvas-transform')
    expect(transform.attributes('style')).toContain('translate(0px, 0px)')
    expect(transform.attributes('style')).toContain('scale(1)')
  })

  it('readonly 模式下工具栏按钮 disabled', () => {
    const wrapper = mount(FlowCanvas, { props: { readonly: true } })
    const buttons = wrapper.findAll('button')
    for (const b of buttons)
      expect(b.attributes('disabled')).toBeDefined()
  })

  it('zoomOut 后 scale 减少', () => {
    const wrapper = mount(FlowCanvas)
    const exposed = wrapper.vm
    exposed.zoomOut()
    expect(exposed.viewport.scale.value).toBeCloseTo(0.9, 5)
  })
})
