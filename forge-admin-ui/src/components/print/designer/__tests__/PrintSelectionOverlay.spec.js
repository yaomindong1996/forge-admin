import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import { addElement, addSection } from '../elementCatalog'
import PrintSelectionOverlay from '../PrintSelectionOverlay.vue'

describe('PrintSelectionOverlay measure guides', () => {
  let pinia
  let store

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    store = usePrintDesignerStore()
    store.load(createPrintDocument())
    addSection(store, 'FIXED')
    expect(addElement(store, 'RECTANGLE')).toBe(true)
    store.patchSelected({ xMm: 95, yMm: 20, widthMm: 40, heightMm: 24 })
  })

  it('shows edge guides and paper-absolute millimetre labels for the selection', () => {
    const wrapper = mount(PrintSelectionOverlay, {
      global: { plugins: [pinia] },
      attachTo: document.body,
    })
    expect(wrapper.find('.selection-measure-layer').exists()).toBe(true)
    expect(wrapper.findAll('.measure-guide.vertical')).toHaveLength(2)
    expect(wrapper.findAll('.measure-guide.horizontal')).toHaveLength(2)
    const labels = wrapper.findAll('.measure-label').map(item => item.text())
    // 默认左边距 10mm + 内容区坐标
    expect(labels).toEqual(['105.0 mm', '145.0 mm'])
    wrapper.unmount()
  })
})
