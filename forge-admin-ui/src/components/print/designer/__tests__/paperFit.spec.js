import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import { paperGeometry } from '../../protocol/units'
import { assertPrintDocument } from '../../protocol/validate'
import { fitDocumentToPaper } from '../paperFit'

describe('fitDocumentToPaper', () => {
  it('shrinks overflowing free elements when paper becomes smaller', () => {
    const doc = createPrintDocument()
    doc.body = [{
      id: 'canvas',
      kind: 'FIXED',
      heightMm: 250,
      elements: [
        { id: 'wide', type: 'TEXT', xMm: 0, yMm: 0, widthMm: 190, heightMm: 20, binding: { source: 'CONSTANT', value: '标题' } },
        { id: 'tall', type: 'RECTANGLE', xMm: 10, yMm: 30, widthMm: 80, heightMm: 200 },
      ],
    }]
    doc.paper.widthMm = 148
    doc.paper.heightMm = 210
    doc.paper.orientation = 'PORTRAIT'
    fitDocumentToPaper(doc)
    const geometry = paperGeometry(doc)
    const section = doc.body[0]
    expect(section.elements[0].widthMm).toBeLessThanOrEqual(geometry.contentWidthMm + 0.001)
    expect(section.heightMm).toBeLessThanOrEqual(geometry.contentHeightMm + 0.001)
    section.elements.forEach((element) => {
      expect(element.xMm + element.widthMm).toBeLessThanOrEqual(geometry.contentWidthMm + 0.001)
      expect(element.yMm + element.heightMm).toBeLessThanOrEqual(section.heightMm + 0.001)
    })
    expect(assertPrintDocument(doc)).toEqual(doc)
  })

  it('keeps landscape rotation layout inside one page body', () => {
    const doc = createPrintDocument()
    doc.body = [{
      id: 'canvas',
      kind: 'FIXED',
      heightMm: 240,
      elements: [
        { id: 'block', type: 'TEXT', xMm: 0, yMm: 0, widthMm: 190, heightMm: 230, binding: { source: 'CONSTANT', value: '长内容' } },
      ],
    }]
    doc.paper.orientation = 'LANDSCAPE'
    fitDocumentToPaper(doc)
    expect(doc.body[0].heightMm).toBeLessThanOrEqual(paperGeometry(doc).contentHeightMm + 0.001)
    expect(assertPrintDocument(doc)).toEqual(doc)
  })
})

describe('print designer paper fit actions', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = usePrintDesignerStore()
    const doc = createPrintDocument()
    doc.body = [{
      id: 'canvas',
      kind: 'FIXED',
      heightMm: 240,
      elements: [
        { id: 'a', type: 'TEXT', xMm: 0, yMm: 10, widthMm: 190, heightMm: 220, binding: { source: 'CONSTANT', value: '内容' } },
      ],
    }]
    store.load(doc)
  })

  it('auto fits canvas when rotating or changing paper size', () => {
    expect(store.rotatePaper()).toBe(true)
    expect(store.document.body[0].heightMm).toBeLessThanOrEqual(paperGeometry(store.document).contentHeightMm + 0.001)
    expect(assertPrintDocument(store.document)).toEqual(store.document)
    expect(store.setPaperSize(148, 210)).toBe(true)
    expect(store.document.body[0].elements[0].widthMm).toBeLessThanOrEqual(paperGeometry(store.document).contentWidthMm + 0.001)
    expect(assertPrintDocument(store.document)).toEqual(store.document)
  })

  it('opens the property panel when selecting a canvas element', () => {
    store.rightPanelOpen = false
    store.selectSurface('section:canvas')
    store.selectElement('a')
    expect(store.rightPanelOpen).toBe(true)
    expect(store.selectedIds).toEqual(['a'])
  })
})
