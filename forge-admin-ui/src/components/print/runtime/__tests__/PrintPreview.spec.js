import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPrintDocument } from '../../protocol/types'
import { createBrowserPrintSession } from '../browserPrint'
import { exportPrintPdf } from '../exportPrintPdf'
import PrintPreview from '../PrintPreview.vue'

const disposeResources = vi.hoisted(() => vi.fn())
vi.mock('../../engine/resources', () => ({ preparePrintResources: vi.fn(async () => ({ images: new Map(), dispose: disposeResources })) }))
vi.mock('../../engine/measure', () => ({ createBrowserMeasurer: () => ({ dispose() {} }) }))
vi.mock('../exportPrintPdf', () => ({ exportPrintPdf: vi.fn(async () => ({ pageCount: 1, filename: '打印.pdf' })) }))
vi.mock('../browserPrint', () => ({ createBrowserPrintSession: vi.fn() }))

describe('preview ownership', () => {
  beforeEach(() => vi.clearAllMocks())

  it('hides the print action when showing a design-only preview', async () => {
    const wrapper = mount(PrintPreview, {
      props: { template: createPrintDocument(), context: {}, catalog: [], allowPrint: false, dataLabel: '模板预览' },
      global: { stubs: { NSelect: true, NButton: { template: '<button><slot /></button>' } } },
    })
    await flushPromises()
    expect(wrapper.findAll('button').some(button => button.text().trim() === '打印')).toBe(false)
    expect(wrapper.text()).toContain('模板预览')
    expect(wrapper.find('[aria-label="适合宽度"]').exists()).toBe(true)
    expect(wrapper.find('.preview-page-shell').exists()).toBe(true)
    expect(wrapper.text()).toContain('第 1 页')
    expect(createBrowserPrintSession).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('disposes a pending print session when the preview unmounts and never prints stale content', async () => {
    const wrapper = mount(PrintPreview, {
      props: { template: createPrintDocument(), context: {}, catalog: [] },
      global: { stubs: { NSelect: true, NButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' } } },
    })
    await flushPromises()
    let complete
    const print = vi.fn()
    const dispose = vi.fn()
    createBrowserPrintSession.mockImplementation(() => new Promise((resolve) => {
      complete = resolve
    }))
    await wrapper.findAll('button').find(button => button.text().trim() === '打印').trigger('click')
    wrapper.unmount()
    complete({ print, dispose })
    await flushPromises()
    expect(print).not.toHaveBeenCalled()
    expect(dispose).toHaveBeenCalledOnce()
    expect(disposeResources).toHaveBeenCalledOnce()
  })

  it('downloads a PDF file without opening the print dialog', async () => {
    const wrapper = mount(PrintPreview, {
      props: { template: createPrintDocument(), context: {}, catalog: [] },
      global: { stubs: { NSelect: true, NButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' } } },
    })
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text().trim() === 'PDF').trigger('click')
    await flushPromises()
    expect(exportPrintPdf).toHaveBeenCalledOnce()
    expect(exportPrintPdf.mock.calls[0][1].filename).toMatch(/\.pdf$/)
    expect(exportPrintPdf.mock.calls[0][1].filename).toMatch(/\d{14}\.pdf$/)
    expect(exportPrintPdf.mock.calls[0][1].sources?.length).toBeGreaterThan(0)
    expect(createBrowserPrintSession).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('shows a paper skeleton instead of a spinner while resources load', async () => {
    const { preparePrintResources } = await import('../../engine/resources')
    let finish
    preparePrintResources.mockImplementationOnce(() => new Promise((resolve) => {
      finish = () => resolve({ images: new Map(), dispose: disposeResources })
    }))
    const wrapper = mount(PrintPreview, {
      props: { template: createPrintDocument(), context: {}, catalog: [] },
      global: { stubs: { NSelect: true, NButton: { template: '<button><slot /></button>' } } },
    })
    await Promise.resolve()
    expect(wrapper.find('.print-preview-skeleton').exists()).toBe(true)
    expect(wrapper.find('.n-spin').exists()).toBe(false)
    finish()
    await flushPromises()
    expect(wrapper.find('.print-preview-skeleton').exists()).toBe(false)
    wrapper.unmount()
  })
})
