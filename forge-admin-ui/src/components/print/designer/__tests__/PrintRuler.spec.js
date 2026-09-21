import { mount } from '@vue/test-utils'
import { NDropdown, NSelect } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import PrintCanvas from '../PrintCanvas.vue'
import PrintDesignerToolbar from '../PrintDesignerToolbar.vue'
import PrintRuler from '../PrintRuler.vue'

describe('print designer paper workspace', () => {
  let pinia
  let store

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    store = usePrintDesignerStore()
    store.load(createPrintDocument())
  })

  it('renders five millimetre ticks and ten millimetre labels', () => {
    const wrapper = mount(PrintRuler, { props: { lengthMm: 20, orientation: 'horizontal' } })
    expect(wrapper.findAll('.ruler-mark')).toHaveLength(5)
    expect(wrapper.findAll('.ruler-mark.major')).toHaveLength(3)
    expect(wrapper.findAll('.ruler-mark small').map(item => item.text())).toEqual(['0', '10', '20'])
  })

  it('shows both rulers, grid and physical paper guides', () => {
    const wrapper = mount(PrintCanvas, { global: { plugins: [pinia] } })
    expect(wrapper.find('.print-ruler.horizontal').exists()).toBe(true)
    expect(wrapper.find('.print-ruler.vertical').exists()).toBe(true)
    expect(wrapper.find('.design-paper.paper-grid').exists()).toBe(true)
    expect(wrapper.find('.margin-guide').attributes('style')).toContain('10mm')
    expect(wrapper.find('.header-guide').exists()).toBe(true)
    expect(wrapper.find('.footer-guide').exists()).toBe(true)
    expect(wrapper.find('.alignment-guide').exists()).toBe(false)
  })

  it('changes paper, orientation, grid and zoom from the toolbar', async () => {
    const wrapper = mount(PrintDesignerToolbar, { props: { local: false }, global: { plugins: [pinia] } })
    await wrapper.findAllComponents(NSelect)[0].vm.$emit('update:value', 'A5')
    expect(store.document.paper).toMatchObject({ widthMm: 148, heightMm: 210 })
    await wrapper.get('[aria-label="转为横向"]').trigger('click')
    expect(store.document.paper.orientation).toBe('LANDSCAPE')
    await wrapper.get('[aria-label="显示或隐藏毫米网格"]').trigger('click')
    expect(store.showGrid).toBe(false)
    await wrapper.get('[aria-label="放大画布"]').trigger('click')
    expect(store.zoom).toBe(1)
  })

  it('uses compact labelled commands and toggles both workspace panels', async () => {
    const wrapper = mount(PrintDesignerToolbar, { props: { local: false }, global: { plugins: [pinia] } })
    expect(wrapper.get('[aria-label="撤销"]').text()).toBe('')
    expect(wrapper.get('[aria-label="预览打印结果"]').text()).toBe('')
    await wrapper.get('[aria-label="显示或隐藏组件面板"]').trigger('click')
    await wrapper.get('[aria-label="显示或隐藏属性面板"]').trigger('click')
    expect(store.leftPanelOpen).toBe(false)
    expect(store.rightPanelOpen).toBe(false)
  })

  it('places movable ruler guides from the horizontal and vertical rulers', async () => {
    const wrapper = mount(PrintCanvas, { global: { plugins: [pinia] } })
    const horizontal = wrapper.find('.print-ruler.horizontal')
    await horizontal.trigger('pointermove', { clientX: 120, clientY: 10, button: 0 })
    expect(store.guidePreview).toMatchObject({ axis: 'x' })
    await horizontal.trigger('pointerdown', { clientX: 120, clientY: 10, button: 0 })
    expect(store.userGuides).toHaveLength(1)
    expect(store.userGuides[0].axis).toBe('x')
    store.moveUserGuide(store.userGuides[0].id, 42)
    expect(store.userGuides[0].positionMm).toBe(42)
    store.removeUserGuide(store.userGuides[0].id)
    expect(store.userGuides).toHaveLength(0)
  })
})
