import { toPng } from 'html-to-image'
import { jsPDF } from 'jspdf'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { exportPrintPdf } from '../exportPrintPdf'

vi.mock('html-to-image', () => ({
  toPng: vi.fn(async () => 'data:image/png;base64,AA'),
}))
vi.mock('jspdf', () => ({
  jsPDF: vi.fn(function () {
    this.addPage = vi.fn()
    this.addImage = vi.fn()
    this.save = vi.fn()
  }),
}))

function page(number = 1) {
  return { number, header: { elements: [] }, footer: { elements: [] }, fragments: [] }
}

function sourcePage() {
  const source = document.createElement('article')
  source.dataset.printPage = '1'
  Object.defineProperty(source, 'offsetWidth', { value: 794 })
  Object.defineProperty(source, 'offsetHeight', { value: 1123 })
  return source
}

describe('client pdf download', () => {
  beforeEach(() => vi.clearAllMocks())

  it('saves a PDF file without opening the print dialog', async () => {
    const event = vi.fn()
    const source = sourcePage()
    const result = { geometry: { widthMm: 210, heightMm: 297 }, pages: [page()] }
    await exportPrintPdf(result, { sources: [source], filename: '单据.pdf', onEvent: event })
    expect(toPng).toHaveBeenCalledOnce()
    const capture = toPng.mock.calls[0][1]
    expect(capture.width).toBeGreaterThan(0)
    expect(capture.height).toBeGreaterThan(0)
    expect(capture.style.transform).toBe('none')
    expect(jsPDF).toHaveBeenCalledOnce()
    const pdf = jsPDF.mock.results[0].value
    expect(pdf.save).toHaveBeenCalledWith('单据.pdf')
    expect(pdf.addImage).toHaveBeenCalledOnce()
    expect(pdf.addImage.mock.calls[0][1]).toBe('PNG')
    expect(pdf.addPage).not.toHaveBeenCalled()
    expect(event).toHaveBeenCalledWith({ result: 'PDF_DOWNLOADED', pageCount: 1, filename: '单据.pdf' })
    expect(document.querySelector('iframe[data-forge-print]')).toBeNull()
  })

  it('captures each sheet once when labels are tiled', async () => {
    const result = {
      geometry: { widthMm: 100, heightMm: 60 },
      pages: [{
        number: 1,
        header: { elements: [] },
        footer: { elements: [] },
        fragments: [{
          id: 'tile-1-0',
          kind: 'TILE',
          xMm: 0,
          yMm: 0,
          widthMm: 50,
          heightMm: 30,
          geometry: { widthMm: 50, heightMm: 30 },
          page: page(1),
        }, {
          id: 'tile-1-1',
          kind: 'TILE',
          xMm: 50,
          yMm: 0,
          widthMm: 50,
          heightMm: 30,
          geometry: { widthMm: 50, heightMm: 30 },
          page: page(1),
        }],
      }],
    }
    await exportPrintPdf(result, { sources: [sourcePage()], filename: '标签.pdf' })
    expect(toPng).toHaveBeenCalledOnce()
    expect(jsPDF.mock.results[0].value.save).toHaveBeenCalledWith('标签.pdf')
  })

  it('prefers already rendered preview pages over a hidden print session', async () => {
    const source = sourcePage()
    document.body.append(source)
    try {
      await exportPrintPdf({ geometry: { widthMm: 210, heightMm: 297 }, pages: [page()] }, { sources: [source], filename: '预览.pdf' })
      expect(toPng).toHaveBeenCalledWith(source, expect.objectContaining({ width: expect.any(Number) }))
      expect(document.querySelector('iframe[data-forge-print]')).toBeNull()
    }
    finally {
      source.remove()
    }
  })
})
