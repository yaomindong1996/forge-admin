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

  it('readonly + allowNavigation 时仍可缩放浏览', () => {
    const wrapper = mount(FlowCanvas, { props: { readonly: true, allowNavigation: true } })
    const buttons = wrapper.findAll('button')
    for (const b of buttons)
      expect(b.attributes('disabled')).toBeUndefined()
    wrapper.vm.zoomOut()
    expect(wrapper.vm.viewport.scale.value).toBeCloseTo(0.9, 5)
  })

  it('zoomOut 后 scale 减少', () => {
    const wrapper = mount(FlowCanvas)
    const exposed = wrapper.vm
    exposed.zoomOut()
    expect(exposed.viewport.scale.value).toBeCloseTo(0.9, 5)
  })

  it('适应画布按真实内容范围居中，而非重置到原点', async () => {
    const w = mount(FlowCanvas, {
      props: { contentBounds: { minX: 100, minY: 200, maxX: 500, maxY: 600 } },
    })
    Object.defineProperty(w.element, 'clientWidth', { value: 1000 })
    Object.defineProperty(w.element, 'clientHeight', { value: 800 })
    w.vm.viewport.pan(-500, -500)
    await w.find('[aria-label="适应画布"]').trigger('click')
    expect(w.vm.viewport.translateX.value).toBe(200)
    expect(w.vm.viewport.translateY.value).toBe(0)
    expect(w.vm.viewport.scale.value).toBe(1)
    w.unmount()
  })

  it('表单输入空格不会启动画布平移，窗口失焦清理拖动状态', async () => {
    const w = mount(FlowCanvas, { slots: { nodes: '<input aria-label="节点名称" />' }, attachTo: document.body })
    const input = w.find('input')
    await input.trigger('keydown', { code: 'Space' })
    expect(w.classes()).not.toContain('cursor-grab')
    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'Space' }))
    await w.vm.$nextTick()
    expect(w.classes()).toContain('cursor-grab')
    window.dispatchEvent(new Event('blur'))
    await w.vm.$nextTick()
    expect(w.classes()).not.toContain('cursor-grab')
    w.unmount()
  })
})
