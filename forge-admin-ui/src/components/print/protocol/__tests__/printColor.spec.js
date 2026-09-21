import { describe, expect, it } from 'vitest'
import { isPrintColor, sanitizePrintColors, toPrintColor } from '../printColor'

describe('print color protocol', () => {
  it('turns picker values into #rrggbb or transparent', () => {
    expect(toPrintColor('transparent')).toBe('transparent')
    expect(toPrintColor('#abc')).toBe('#aabbcc')
    expect(toPrintColor('#11223344')).toBe('#112233')
    expect(toPrintColor('#11223300')).toBe('transparent')
    expect(toPrintColor('rgba(17, 34, 51, 1)')).toBe('#112233')
    expect(toPrintColor('rgba(0, 0, 0, 0)')).toBe('transparent')
    expect(isPrintColor('red')).toBe(false)
    expect(isPrintColor('transparent')).toBe(true)
  })

  it('rewrites nested cell fills before save', () => {
    const doc = { body: [{ elements: [{ table: { cells: [{ style: { backgroundColor: 'rgb(255, 0, 0)' } }] } }] }] }
    sanitizePrintColors(doc)
    expect(doc.body[0].elements[0].table.cells[0].style.backgroundColor).toBe('#ff0000')
  })
})
