import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintDesignerStore } from '../../../../stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import { mmToPx } from '../../protocol/units'
import { createStaticTable } from '../staticTable'

function fixture() {
  const doc = createPrintDocument()
  doc.body.push({ id: 'section', kind: 'FIXED', heightMm: 60, elements: [
    { id: 'a', type: 'TEXT', xMm: 10, yMm: 10, widthMm: 30, heightMm: 10, binding: { source: 'CONSTANT', value: '文字' } },
    { id: 'b', type: 'TEXT', xMm: 50, yMm: 20, widthMm: 30, heightMm: 10, binding: { source: 'CONSTANT', value: '第二项' } },
  ] })
  return doc
}

describe('print designer commands and history', () => {
  let store
  beforeEach(() => {
    setActivePinia(createPinia())
    store = usePrintDesignerStore()
    store.load(fixture())
    store.selectSurface('section')
    store.selectElement('a')
  })

  it('records an entire zoomed drag as one undoable operation', () => {
    store.zoom = 0.5
    store.beginGesture()
    store.moveGesture(mmToPx(2) * 0.5, 0)
    store.moveGesture(mmToPx(9) * 0.5, mmToPx(4) * 0.5)
    store.endGesture()
    expect(store.selectedElements[0].xMm).toBeCloseTo(19)
    expect(store.history.past).toHaveLength(1)
    store.undo()
    expect(store.selectedElements[0].xMm).toBe(10)
    expect(store.dirty).toBe(false)
    store.redo()
    expect(store.selectedElements[0].yMm).toBeCloseTo(14)
  })

  it('snaps movement to element edges and clears dynamic guides after the gesture', () => {
    store.beginGesture()
    store.moveGesture(mmToPx(9.4) * store.zoom, 0)
    expect(store.selectedElements[0].xMm).toBeCloseTo(20)
    expect(store.alignmentGuides.x).toEqual([50])
    expect(store.alignmentGuides.position).toMatchObject({ xMm: 20, yMm: 10 })
    store.endGesture()
    expect(store.alignmentGuides).toEqual({ x: [], y: [], position: null })
  })

  it('does not show an alignment line outside the snap threshold and clears on cancel', () => {
    store.beginGesture()
    store.moveGesture(mmToPx(8.5) * store.zoom, mmToPx(2.5) * store.zoom)
    expect(store.alignmentGuides.x).toEqual([])
    expect(store.alignmentGuides.y).toEqual([])
    store.cancelGesture()
    expect(store.selectedElements[0]).toMatchObject({ xMm: 10, yMm: 10 })
    expect(store.alignmentGuides.position).toBeNull()
  })

  it('snaps resize handles to neighbouring element edges', () => {
    store.beginGesture()
    store.moveGesture(mmToPx(9.5) * store.zoom, 0, true)
    expect(store.selectedElements[0].widthMm).toBeCloseTo(40)
    expect(store.alignmentGuides.x).toEqual([50])
    store.endGesture()
  })

  it('cancels gestures and ignores no-op commands', () => {
    store.beginGesture()
    store.moveGesture(50, 10)
    store.cancelGesture()
    store.patchSelected({ xMm: 10 })
    expect(store.dirty).toBe(false)
    expect(store.history.past).toHaveLength(0)
  })

  it('clamps horizontal overflow and grows the fixed band when dragging downward', () => {
    store.selectElement('b', true)
    store.moveSelection(-100, 100)
    expect(store.selectedElements.map(e => [e.xMm, e.yMm])).toEqual([[0, 110], [40, 120]])
    expect(store.activeSurface.heightMm).toBeGreaterThanOrEqual(130)
  })

  it('grows header and footer when dragging elements past the current band height', () => {
    store.execute((doc) => {
      doc.header.heightMm = 12
      doc.header.elements = [{ id: 'h1', type: 'TEXT', xMm: 2, yMm: 2, widthMm: 30, heightMm: 8, binding: { source: 'CONSTANT', value: '页眉' } }]
      doc.footer.heightMm = 12
      doc.footer.elements = [{ id: 'f1', type: 'TEXT', xMm: 2, yMm: 2, widthMm: 30, heightMm: 8, binding: { source: 'CONSTANT', value: '页脚' } }]
    })

    store.selectSurface('header')
    store.selectElement('h1')
    store.beginGesture()
    store.moveGesture(0, mmToPx(10) * store.zoom)
    store.endGesture()
    expect(store.document.header.elements[0].yMm).toBeCloseTo(12, 1)
    expect(store.document.header.heightMm).toBeGreaterThanOrEqual(20)

    store.selectSurface('footer')
    store.selectElement('f1')
    store.beginGesture()
    store.moveGesture(0, mmToPx(8) * store.zoom)
    store.endGesture()
    expect(store.document.footer.elements[0].yMm).toBeCloseTo(10, 1)
    expect(store.document.footer.heightMm).toBeGreaterThanOrEqual(18)
  })

  it('does not snap the selection bottom to the band edge while dragging downward', () => {
    store.execute((doc) => {
      doc.header.heightMm = 20
      doc.header.elements = [{ id: 'h2', type: 'TEXT', xMm: 2, yMm: 10, widthMm: 30, heightMm: 8, binding: { source: 'CONSTANT', value: '近底' } }]
    })
    store.selectSurface('header')
    store.selectElement('h2')
    store.beginGesture()
    // 3mm past the band bottom — previously snapped back to heightMm and refused to grow.
    store.moveGesture(0, mmToPx(5) * store.zoom)
    store.endGesture()
    expect(store.document.header.elements[0].yMm).toBeCloseTo(15, 1)
    expect(store.document.header.heightMm).toBeGreaterThanOrEqual(23)
  })

  it('aligns selected elements on every edge and distributes three elements', () => {
    store.selectElement('b', true)
    expect(store.alignSelection('right')).toBe(true)
    expect(store.selectedElements.map(element => element.xMm + element.widthMm)).toEqual([80, 80])
    store.undo()
    expect(store.alignSelection('middle')).toBe(true)
    expect(store.selectedElements.map(element => element.yMm + element.heightMm / 2)).toEqual([20, 20])

    store.execute(document => document.body[0].elements.push({ id: 'c', type: 'TEXT', xMm: 100, yMm: 30, widthMm: 20, heightMm: 10, binding: { source: 'CONSTANT', value: '第三项' } }))
    store.selectElement('c', true)
    expect(store.distributeSelection('horizontal')).toBe(true)
    expect(store.selectedElements.map(element => element.xMm)).toEqual([10, 55, 100])
    expect(store.distributeSelection('vertical')).toBe(true)
    expect(store.selectedElements.map(element => element.yMm)).toEqual([15, 22.5, 30])
  })

  it('rejects out-of-bounds properties and invalid imports atomically', () => {
    const before = store.serialize()
    expect(store.patchSelected({ widthMm: 300 })).toBe(false)
    expect(store.error).toBeTruthy()
    expect(store.serialize()).toBe(before)
    expect(() => store.load({ protocol: 'untrusted' })).toThrow()
    expect(store.serialize()).toBe(before)
  })

  it('gives pasted elements distinct IDs and preserves them through undo/redo', () => {
    store.selectElement('b', true)
    store.copySelection()
    store.pasteSelection()
    const ids = store.selectedIds.slice()
    expect(new Set(store.activeSurface.elements.map(e => e.id)).size).toBe(4)
    store.undo()
    expect(store.activeSurface.elements).toHaveLength(2)
    store.redo()
    expect(store.activeSurface.elements.slice(2).map(e => e.id)).toEqual(ids)
  })

  it('selects all, duplicates and changes selected stacking order atomically', () => {
    store.selectAll()
    expect(store.selectedIds).toEqual(['a', 'b'])
    expect(store.duplicateSelection()).toBe(true)
    const duplicates = store.selectedIds.slice()
    expect(store.activeSurface.elements).toHaveLength(4)
    expect(store.moveSelectionLayer('back')).toBe(true)
    expect(store.activeSurface.elements.slice(0, 2).map(element => element.id)).toEqual(duplicates)
    expect(store.moveSelectionLayer('front')).toBe(true)
    expect(store.activeSurface.elements.slice(-2).map(element => element.id)).toEqual(duplicates)
    store.undo()
    expect(store.activeSurface.elements.slice(0, 2).map(element => element.id)).toEqual(duplicates)
  })

  it('toggles italic/underline, spaces fixed gaps and clears the active surface', () => {
    expect(store.toggleSelectionItalic()).toBe(true)
    expect(store.activeElement.style.fontStyle).toBe('italic')
    expect(store.toggleSelectionUnderline()).toBe(true)
    expect(store.activeElement.style.textDecoration).toBe('underline')
    expect(store.patchSelectionStyle({ color: '#112233' })).toBe(true)
    expect(store.activeElement.style.color).toBe('#112233')
    store.selectElement('b', true)
    expect(store.spaceSelection('horizontal', 5)).toBe(true)
    expect(store.selectedElements.map(element => element.xMm)).toEqual([10, 45])
    expect(store.clearActiveSurfaceElements()).toBe(true)
    expect(store.activeSurface.elements).toHaveLength(0)
  })

  it('disables snapping when the gesture opts out', () => {
    store.beginGesture()
    store.moveGesture(mmToPx(9.4) * store.zoom, 0, false, 'se', { snap: false })
    expect(store.selectedElements[0].xMm).toBeCloseTo(19.4, 1)
    expect(store.alignmentGuides.x).toEqual([])
    store.cancelGesture()
  })

  it('nudges zoom, rotates paper and toggles grid from shared view commands', () => {
    store.zoom = 0.8
    store.nudgeZoom(1)
    expect(store.zoom).toBe(1)
    store.setZoom(1.5)
    expect(store.canZoomIn).toBe(false)
    store.nudgeZoom(1)
    expect(store.zoom).toBe(1.5)
    expect(store.rotatePaper()).toBe(true)
    expect(store.document.paper.orientation).toBe('LANDSCAPE')
    store.toggleGrid(false)
    expect(store.showGrid).toBe(false)
  })

  it('selects intersecting elements by marquee box and expands collapsed bands', () => {
    expect(store.selectByBox('section:section', { x: 0, y: 0, w: 45, h: 25 })).toEqual(['a'])
    expect(store.marquee.count).toBe(1)
    store.clearMarquee()
    expect(store.marquee).toBeNull()
    expect(store.selectByBox('section:section', { x: 5, y: 5, w: 80, h: 40 }, ['a'])).toEqual(['a', 'b'])
    store.execute(doc => doc.header.heightMm = 0)
    expect(store.expandBand('header')).toBe(true)
    expect(store.document.header.heightMm).toBe(12)
    store.toggleSnap(false)
    expect(store.snapEnabled).toBe(false)
    store.toggleGuideLines(true)
    expect(store.showGuideLines).toBe(true)
  })

  it('adjusts selection typography and syncs multi-select sizes from canvas commands', () => {
    expect(store.nudgeSelectionFontSize(2)).toBe(true)
    expect(store.activeElement.style.fontSizePt).toBe(12)
    expect(store.toggleSelectionBold()).toBe(true)
    expect(store.activeElement.style.fontWeight).toBe(700)
    expect(store.setSelectionTextAlign('center')).toBe(true)
    expect(store.activeElement.style.textAlign).toBe('center')
    store.selectElement('b')
    expect(store.patchSelected({ widthMm: 20 })).toBe(true)
    store.selectElement('a')
    store.selectElement('b', true)
    expect(store.syncSelectionSize('widthMm', 'max')).toBe(true)
    expect(store.selectedElements.map(element => element.widthMm)).toEqual([30, 30])
    expect(store.cutSelection()).toBe(true)
    expect(store.activeSurface.elements).toHaveLength(0)
    expect(store.clipboard).toHaveLength(2)
  })

  it('moves selection one layer at a time', () => {
    store.selectElement('a')
    expect(store.moveSelectionLayer('forward')).toBe(true)
    expect(store.activeSurface.elements.map(element => element.id)).toEqual(['b', 'a'])
    expect(store.moveSelectionLayer('backward')).toBe(true)
    expect(store.activeSurface.elements.map(element => element.id)).toEqual(['a', 'b'])
  })

  it('rotates, mirrors and protects locked elements while keeping unlock reachable', () => {
    expect(store.rotateSelection(90)).toBe(true)
    expect(store.flipSelection('x')).toBe(true)
    expect(store.activeElement).toMatchObject({ rotationDeg: 90, flipX: true })
    expect(store.toggleSelectionLock(true)).toBe(true)
    const locked = store.serialize()
    expect(store.moveSelection(10, 10)).toBe(false)
    expect(store.moveSelectionLayer('front')).toBe(false)
    expect(store.removeSelection()).toBe(false)
    expect(store.serialize()).toBe(locked)
    store.copySelection()
    expect(store.pasteSelection()).toBe(true)
    expect(store.activeElement.locked).toBe(false)
    store.undo()
    store.selectElement('a')
    expect(store.toggleSelectionLock(false)).toBe(true)
    expect(store.activeElement.locked).toBe(false)
  })

  it('limits retained history and clears redo when editing after undo', () => {
    for (let i = 0; i < 70; i++) {
      store.patchSelected({ binding: { source: 'CONSTANT', value: String(i) } })
    }
    expect(store.history.past.length).toBeLessThanOrEqual(50)
    store.undo()
    store.patchSelected({ binding: { source: 'CONSTANT', value: 'new' } })
    expect(store.canRedo).toBe(false)
  })

  it('marks only the saved revision clean and preserves edits during async save', async () => {
    store.patchSelected({ xMm: 11 })
    let complete
    const saving = store.save(() => new Promise((resolve) => {
      complete = resolve
    }))
    store.patchSelected({ xMm: 12 })
    complete()
    await saving
    expect(store.dirty).toBe(true)
    store.undo()
    expect(store.dirty).toBe(false)
  })

  it('keeps a failed save dirty and does not mark another loaded document saved', async () => {
    store.patchSelected({ xMm: 11 })
    expect(await store.save(async () => {
      throw new Error('保存失败')
    })).toBe(false)
    expect(store.dirty).toBe(true)
    let complete
    const saving = store.save(() => new Promise((resolve) => {
      complete = resolve
    }))
    store.load(createPrintDocument())
    complete()
    await saving
    expect(store.dirty).toBe(false)
    expect(store.document.body).toHaveLength(1)
    expect(store.document.body[0].kind).toBe('FIXED')
  })
  it('keeps imported section IDs distinct from header/footer editor surfaces', () => {
    const document = fixture()
    document.body[0].id = 'header'
    store.load(document)
    expect(store.activeSurface.kind).toBe('FIXED')
    store.selectElement('a')
    store.patchSelected({ xMm: 11 })
    expect(store.document.body[0].elements[0].xMm).toBe(11)
    expect(store.document.header.elements).toHaveLength(0)
    store.selectSurface('header')
    expect(store.activeSurface).toEqual(store.document.header)
  })
  it('edits, merges and duplicates native blank-table cells with unique nested ids', () => {
    const table = createStaticTable(2, 2, 40, 8)
    store.execute(document => document.body[0].elements.push({ id: 'table', type: 'STATIC_TABLE', xMm: 0, yMm: 35, widthMm: 40, heightMm: 16, table }))
    store.selectElement('table')
    store.selectTableCell(store.activeElement.table.cells[0].id)
    store.patchSelectedTableCells({ binding: { source: 'CONSTANT', value: '合同编号' } })
    expect(store.activeTableCell.binding.value).toBe('合同编号')
    store.selectTableCell(store.activeElement.table.cells[1].id, true)
    expect(store.mergeStaticTableSelection()).toBe(true)
    expect(store.activeTableCell.colSpan).toBe(2)
    expect(store.splitStaticTableSelection()).toBe(true)
    store.duplicateSelection()
    const [original, copy] = store.activeSurface.elements.filter(element => element.type === 'STATIC_TABLE')
    expect(copy.table.cells.map(cell => cell.id)).not.toEqual(original.table.cells.map(cell => cell.id))
  })
  it('grows the last static-table tracks and can shift origin from the start edge', () => {
    const table = createStaticTable(2, 2, 40, 8)
    store.execute(document => document.body[0].elements.push({ id: 'table', type: 'STATIC_TABLE', xMm: 4, yMm: 6, widthMm: 40, heightMm: 16, table }))
    store.selectElement('table')
    expect(store.resizeStaticTableTrack('column', 1, 30)).toBe(true)
    expect(store.activeElement.widthMm).toBeCloseTo(50)
    expect(store.resizeStaticTableTrack('row', 1, 12)).toBe(true)
    expect(store.activeElement.heightMm).toBeCloseTo(20)
    expect(store.resizeStaticTableTrack('column', 0, 10, { originMm: 8 })).toBe(true)
    expect(store.activeElement.xMm).toBe(8)
    expect(store.activeElement.table.columns[0].widthMm).toBe(10)
  })
})
