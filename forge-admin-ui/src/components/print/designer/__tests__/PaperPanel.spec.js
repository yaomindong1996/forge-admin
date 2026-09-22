import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import PaperPanel from '../panels/PaperPanel.vue'

describe('paper panel property layout', () => {
  it('uses field picks instead of watermark expressions and filename tokens', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = usePrintDesignerStore()
    store.load(createPrintDocument(), [{ path: 'main.name', label: '名称', type: 'TEXT' }])
    const wrapper = mount(PaperPanel, {
      global: {
        plugins: [pinia],
        stubs: { FileUpload: true },
      },
    })
    const text = wrapper.text()
    expect(text).toContain('拼接字段')
    expect(text).toContain('疏密')
    expect(text).toContain('带上模板名称')
    expect(text).toContain('带上单据字段')
    expect(text).toContain('每页重复')
    expect(text).not.toContain('水印表达式')
    expect(text).not.toContain('{timestamp}')
    wrapper.unmount()
  })
})
