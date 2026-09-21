import { describe, expect, it } from 'vitest'
import { createPrintDocument } from '../../protocol/types'
import { estimateDesignerSectionHeight, paginateDesignerBody } from '../designerPagination'

function fixed(id, heightMm, gapAfterMm = 0) {
  return { id, kind: 'FIXED', heightMm, elements: [], gapAfterMm }
}

describe('designer paper estimation', () => {
  it('separates automatic and manual page starts without changing the document', () => {
    const doc = createPrintDocument()
    doc.paper = { widthMm: 100, heightMm: 100, orientation: 'PORTRAIT', marginMm: { top: 10, right: 10, bottom: 10, left: 10 } }
    doc.body = [fixed('a', 50, 10), fixed('b', 30), { id: 'manual', kind: 'PAGE_BREAK' }, fixed('c', 20)]
    const before = JSON.stringify(doc)
    const result = paginateDesignerBody(doc, section => section.heightMm)
    expect(result.pages).toHaveLength(3)
    expect(result.pages[1]).toMatchObject({ automaticBreakBefore: 'b', breakBefore: null })
    expect(result.pages[2].breakBefore).toMatchObject({ id: 'manual' })
    expect(JSON.stringify(doc)).toBe(before)
  })

  it('estimates sample text and table height with printable minimums', () => {
    expect(estimateDesignerSectionHeight({ kind: 'TEXT' }, { text: '短文本', contentWidthMm: 190 })).toBe(16)
    expect(estimateDesignerSectionHeight({ kind: 'TABLE' }, { tableRows: 5 })).toBe(44)
    expect(estimateDesignerSectionHeight({ kind: 'FIXED', heightMm: 33 })).toBe(33)
  })

  it('uses the oriented paper height for automatic designer pagination', () => {
    const doc = createPrintDocument()
    doc.paper = { widthMm: 100, heightMm: 200, orientation: 'PORTRAIT', marginMm: { top: 10, right: 10, bottom: 10, left: 10 } }
    doc.body = [fixed('a', 70), fixed('b', 70)]
    expect(paginateDesignerBody(doc, section => section.heightMm).pages).toHaveLength(1)
    doc.paper.orientation = 'LANDSCAPE'
    expect(paginateDesignerBody(doc, section => section.heightMm).pages).toHaveLength(2)
  })
})
