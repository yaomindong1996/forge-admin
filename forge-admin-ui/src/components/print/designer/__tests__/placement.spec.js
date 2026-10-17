import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import { addDetailTable, addElement, addSection } from '../elementCatalog'
import { contentBottomMm, resolvePlacement } from '../placement'

describe('print designer placement', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = usePrintDesignerStore()
    store.load(createPrintDocument(), [
      { path: 'main.name', label: '姓名', type: 'TEXT' },
      { path: 'items', label: '明细', type: 'COLLECTION' },
      { path: 'items.sku', label: '编码', type: 'TEXT' },
      { path: 'items.qty', label: '数量', type: 'NUMBER' },
    ])
    addSection(store, 'FIXED')
  })

  it('packs palette clicks to the right then below instead of stacking at 3,3', () => {
    expect(addElement(store, 'TEXT')).toBe(true)
    expect(addElement(store, 'TEXT')).toBe(true)
    expect(addElement(store, 'TEXT')).toBe(true)
    expect(addElement(store, 'TEXT')).toBe(true)
    expect(addElement(store, 'TEXT')).toBe(true)
    const elements = store.activeSurface.elements
    expect(elements[0]).toMatchObject({ xMm: 3, yMm: 3 })
    expect(elements[1].xMm).toBeGreaterThan(elements[0].xMm + elements[0].widthMm)
    expect(elements[1].yMm).toBe(elements[0].yMm)
    // After the first row fills, later palette clicks wrap to the next row.
    const wrapped = elements.find(item => item.yMm > elements[0].yMm)
    expect(wrapped).toBeTruthy()
    expect(wrapped.yMm).toBeGreaterThanOrEqual(contentBottomMm(elements.filter(item => item.yMm === elements[0].yMm)))
    const boxes = elements.map(item => `${item.xMm},${item.yMm}`)
    expect(new Set(boxes).size).toBe(elements.length)
  })

  it('keeps explicit drop coordinates', () => {
    expect(addElement(store, 'TEXT', undefined, { xMm: 12, yMm: 40 })).toBe(true)
    expect(store.activeElement).toMatchObject({ xMm: 12, yMm: 40 })
  })

  it('grows the FIXED band so new elements stay in bounds', () => {
    const before = store.activeSurface.heightMm
    expect(addElement(store, 'TEXT', undefined, { xMm: 3, yMm: before - 2 })).toBe(true)
    const element = store.activeElement
    expect(store.activeSurface.heightMm).toBeGreaterThanOrEqual(element.yMm + element.heightMm)
  })

  it('places detail tables below existing content on palette insert', () => {
    expect(addElement(store, 'TEXT')).toBe(true)
    const textBottom = store.activeElement.yMm + store.activeElement.heightMm
    expect(addDetailTable(store)).toBe(true)
    expect(store.activeElement.type).toBe('DATA_TABLE')
    expect(store.activeElement.yMm).toBeGreaterThanOrEqual(textBottom)
    expect(store.activeSurface.heightMm).toBeGreaterThanOrEqual(store.activeElement.yMm + store.activeElement.heightMm)
  })

  it('resolvePlacement prefers the next row when the current row is full', () => {
    const surface = {
      elements: [
        { xMm: 3, yMm: 3, widthMm: 100, heightMm: 10 },
        { xMm: 110, yMm: 3, widthMm: 80, heightMm: 10 },
      ],
    }
    expect(resolvePlacement(surface, 50, 10, 190)).toEqual({ xMm: 3, yMm: 15 })
  })
})
