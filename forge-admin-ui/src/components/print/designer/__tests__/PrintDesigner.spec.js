import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { createPrintDocument } from '../../protocol/types'
import { mmToPx } from '../../protocol/units'
import { parseDraft, readDraft, writeDraft } from '../draftStorage'
import { addElement, addField, addSection } from '../elementCatalog'
import TableBandsPanel from '../panels/TableBandsPanel.vue'
import PrintCanvas from '../PrintCanvas.vue'
import PrintDesigner from '../PrintDesigner.vue'

const catalog = [{ path: 'main.name', label: '名称', type: 'TEXT' }, { path: 'children.lines', type: 'COLLECTION' }, { path: 'children.lines.name', type: 'TEXT' }]
describe('designer editing workflow', () => {
  let pinia
  let store
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    store = usePrintDesignerStore()
    store.load(createPrintDocument(), catalog)
    localStorage.clear()
  })
  it('guards server-side name edits even when the canvas itself is clean', async () => {
    const confirmDiscard = vi.fn(() => false)
    const wrapper = mount(PrintDesigner, { props: { externalDirty: true, confirmDiscard, saveDraft: vi.fn() }, global: { plugins: [createPinia()] } })
    expect(await wrapper.vm.canLeave()).toBe(false)
    expect(confirmDiscard).toHaveBeenCalledOnce()
    wrapper.unmount()
  })

  it('creates every supported element and isolates collection fields to their table', () => {
    addSection(store, 'FIXED')
    for (const type of ['TEXT', 'IMAGE', 'HTML', 'LINE', 'RECTANGLE', 'ELLIPSE', 'BARCODE', 'QRCODE', 'PAGE_NUMBER']) {
      expect(addElement(store, type)).toBe(true)
    }
    expect(addField(store, 'unknown')).toBe(false)
    expect(addField(store, 'children.lines.name')).toBe(false)
    expect(addField(store, 'children.lines')).toBe(true)
    expect(store.activeElement).toMatchObject({ type: 'DATA_TABLE', collectionPath: 'children.lines' })
    expect(store.activeElement.columns[0].field).toBe('name')
    expect(store.fieldIssues).toEqual([])
    expect(store.patchSelectionStyle({ backgroundColor: '#112233' })).toBe(true)
    expect(store.activeElement.headerStyle.backgroundColor).toBe('#112233')
  })
  it('inserts a blank table without default header labels', () => {
    addSection(store, 'FIXED')
    expect(addElement(store, 'STATIC_TABLE')).toBe(true)
    expect(store.activeElement.table.cells.filter(cell => cell.row === 0).every(cell => !cell.binding?.value && !cell.style)).toBe(true)
    expect(store.patchSelectionStyle({ backgroundColor: '#ddeeff', color: '#aa0000' })).toBe(true)
    expect(store.activeElement.headerStyle).toMatchObject({ backgroundColor: '#ddeeff', color: '#aa0000' })
    expect(store.applyStaticTableHeaderStyle()).toBe(true)
    expect(store.activeElement.headerStyle).toMatchObject({ backgroundColor: '#ddeeff', fontWeight: 700 })
    expect(store.activeElement.table.cells[0].style).toBeUndefined()
    store.activeElement.table.cells.filter(cell => cell.row === 0).forEach((cell) => {
      cell.style = { backgroundColor: '#ffffff' }
    })
    expect(store.patchStaticTableBand('header', { backgroundColor: '#ff9900' })).toBe(true)
    expect(store.activeElement.headerStyle.backgroundColor).toBe('#ff9900')
    expect(store.activeElement.table.cells.filter(cell => cell.row === 0).every(cell => !cell.style?.backgroundColor)).toBe(true)
  })
  it('normalizes picker fills so static table cells can save', () => {
    addSection(store, 'FIXED')
    addElement(store, 'STATIC_TABLE')
    store.selectTableCell(store.activeElement.table.cells[0].id)
    expect(store.patchSelectionStyle({ backgroundColor: 'transparent' })).toBe(true)
    expect(store.activeTableCell.style.backgroundColor).toBe('transparent')
    expect(store.patchSelectionStyle({ backgroundColor: 'rgba(17, 34, 51, 1)' })).toBe(true)
    expect(store.activeTableCell.style.backgroundColor).toBe('#112233')
    const saved = JSON.parse(store.serialize())
    const table = saved.body.flatMap(section => section.elements || []).find(element => element.type === 'STATIC_TABLE')
    expect(table.table.cells.find(cell => cell.id === store.activeTableCell.id).style.backgroundColor).toBe('#112233')
  })
  it('keeps table resize handles hittable while a cell is selected', async () => {
    addSection(store, 'FIXED')
    addElement(store, 'STATIC_TABLE')
    store.selectTableCell(store.activeElement.table.cells[0].id)
    const wrapper = mount(PrintCanvas, { attachTo: document.body, global: { plugins: [pinia] } })
    await flushPromises()
    expect(document.body.querySelector('[aria-label="右下角"]')).toBeTruthy()
    expect(document.body.querySelector('[aria-label="上边"]')).toBeTruthy()
    expect(document.body.querySelector('[aria-label="左上角"]')).toBeNull()
    wrapper.unmount()
  })
  it('parks collapsed header chip in the paper margin instead of over titles', async () => {
    store.execute(doc => doc.header.heightMm = 0)
    const wrapper = mount(PrintCanvas, { attachTo: document.body, global: { plugins: [pinia] } })
    await flushPromises()
    const chip = wrapper.get('.header-collapsed')
    expect(chip.text()).toContain('页眉已折叠')
    expect(chip.attributes('style') || '').not.toMatch(/top:/)
    wrapper.unmount()
  })
  it('shows a cell image resize handle above row/column tracks', async () => {
    addSection(store, 'FIXED')
    addElement(store, 'STATIC_TABLE')
    const cellId = store.activeElement.table.cells[0].id
    store.execute((document) => {
      document.body.forEach((section) => {
        section.elements?.filter(element => element.type === 'STATIC_TABLE').forEach((element) => {
          const cell = element.table.cells.find(item => item.id === cellId)
          if (cell)
            cell.contentType = 'IMAGE'
        })
      })
    })
    store.selectTableCell(cellId)
    const wrapper = mount(PrintCanvas, { attachTo: document.body, global: { plugins: [pinia] } })
    await flushPromises()
    expect(wrapper.find('[aria-label="拖动调整图片大小"]').exists()).toBe(true)
    wrapper.unmount()
  })
  it('creates title and vertical line presets with print-ready defaults', () => {
    addSection(store, 'FIXED')
    addElement(store, 'TEXT', undefined, undefined, 'TITLE')
    expect(store.activeElement).toMatchObject({ type: 'TEXT', widthMm: 90, heightMm: 14, style: { fontSizePt: 18, fontWeight: 700, textAlign: 'center' } })
    addElement(store, 'LINE', undefined, undefined, 'VERTICAL')
    expect(store.activeElement).toMatchObject({ type: 'LINE', widthMm: 0.5, heightMm: 40, style: { borderColor: '#000000', backgroundColor: '#000000', borderWidthMm: 0.5, borderStyle: 'solid' } })
    expect(store.patchSelectionStyle({ borderWidthMm: 1 })).toBe(true)
    expect(store.activeElement).toMatchObject({ widthMm: 1, style: { borderWidthMm: 1 } })
    expect(store.patchSelectionStyle({ backgroundColor: '#d93838' })).toBe(true)
    expect(store.activeElement.style).toMatchObject({ backgroundColor: '#d93838', borderColor: '#d93838' })
    expect(store.patchSelectionStyle({ borderStyle: 'dashed' })).toBe(true)
    expect(store.activeElement.style.borderStyle).toBe('dashed')
    addElement(store, 'LINE')
    expect(store.activeElement).toMatchObject({ type: 'LINE', widthMm: 50, heightMm: 0.5, style: { borderColor: '#000000', backgroundColor: '#000000', borderWidthMm: 0.5 } })
  })
  it('inserts a manual page break between content sections and renders two papers', async () => {
    store.execute((doc) => {
      doc.body = []
      doc.footer = { heightMm: 10, repeat: true, elements: [{ id: 'designer-page-number', type: 'PAGE_NUMBER', pageNumberFormat: 'CURRENT_TOTAL', xMm: 0, yMm: 0, widthMm: 40, heightMm: 8 }] }
    })
    addSection(store, 'FIXED')
    const first = store.document.body[0].id
    addSection(store, 'FIXED')
    store.selectSurface(first)
    expect(addSection(store, 'PAGE_BREAK')).toBe(true)
    expect(store.document.body.map(section => section.kind)).toEqual(['FIXED', 'PAGE_BREAK', 'FIXED'])
    const wrapper = mount(PrintCanvas, { global: { plugins: [pinia] } })
    await flushPromises()
    expect(wrapper.findAll('.ruler-frame')).toHaveLength(2)
    expect(wrapper.text()).toContain('手动分页 · 从本页开始')
    expect(wrapper.findAll('[data-element-id="designer-page-number"]').map(node => node.text())).toEqual(['1 / 2', '2 / 2'])
    wrapper.unmount()
  })
  it('moves and resizes with pointer gestures at half scale, undoing each once', async () => {
    addSection(store, 'FIXED')
    addElement(store, 'TEXT')
    store.zoom = 0.5
    const wrapper = mount(PrintCanvas, { attachTo: document.body, global: { plugins: [pinia] } })
    const before = store.activeElement.xMm
    await wrapper.get('[data-element-id]').trigger('pointerdown', { button: 0, pointerId: 1, clientX: 100, clientY: 100 })
    const move = new Event('pointermove')
    Object.assign(move, { pointerId: 1, clientX: 100 + mmToPx(10) * 0.5, clientY: 100 })
    window.dispatchEvent(move)
    const up = new Event('pointerup')
    Object.assign(up, { pointerId: 1, clientX: move.clientX, clientY: 100 })
    window.dispatchEvent(up)
    expect(store.activeElement.xMm).toBeCloseTo(before + 10)
    store.undo()
    expect(store.activeElement.xMm).toBe(before)
    await flushPromises()
    const se = document.body.querySelector('[aria-label="右下角"]')
    expect(se).toBeTruthy()
    const down = new Event('pointerdown', { bubbles: true })
    Object.assign(down, { button: 0, pointerId: 1, clientX: 100, clientY: 100 })
    se.dispatchEvent(down)
    window.dispatchEvent(move)
    window.dispatchEvent(up)
    expect(store.activeElement.widthMm).toBeCloseTo(55)
    store.undo()
    expect(store.activeElement.widthMm).toBe(45)
    wrapper.unmount()
  })
  it('opens the element context menu with native editing actions', async () => {
    addSection(store, 'FIXED')
    addElement(store, 'TEXT')
    const wrapper = mount(PrintCanvas, { attachTo: document.body, global: { plugins: [pinia] } })
    await wrapper.get('[data-element-id]').trigger('contextmenu', { clientX: 120, clientY: 160 })
    await flushPromises()
    expect(document.body.textContent).toContain('向左旋转 90°')
    expect(document.body.textContent).toContain('水平镜像')
    expect(document.body.textContent).toContain('锁定元素')
    wrapper.unmount()
  })
  it('cancels an unfinished gesture on unmount and removes pointer listeners', async () => {
    addSection(store, 'FIXED')
    addElement(store, 'TEXT')
    const wrapper = mount(PrintCanvas, { global: { plugins: [pinia] } })
    const before = store.serialize()
    await wrapper.get('[data-element-id]').trigger('pointerdown', { button: 0, pointerId: 2, clientX: 100, clientY: 100 })
    const move = new Event('pointermove')
    Object.assign(move, { pointerId: 2, clientX: 130, clientY: 130 })
    window.dispatchEvent(move)
    expect(store.serialize()).not.toBe(before)
    wrapper.unmount()
    expect(store.serialize()).toBe(before)
    window.dispatchEvent(move)
    expect(store.serialize()).toBe(before)
  })
  it('round-trips protocol without preview data and rejects corrupt storage', () => {
    addSection(store, 'FIXED')
    addField(store, 'main.name')
    writeDraft(store.document)
    expect(readDraft()).toEqual(store.document)
    localStorage.setItem('forge:print:local-draft:v1', '{"schemaVersion":999}')
    expect(() => readDraft()).toThrow()
  })
  it('keeps a dirty template if discard is cancelled and saves through the provided adapter', async () => {
    const save = vi.fn(async () => {})
    const confirmDiscard = vi.fn(() => false)
    const wrapper = mount(PrintDesigner, { props: { template: createPrintDocument(), catalog, saveDraft: save, confirmDiscard }, global: { plugins: [pinia], stubs: { PrintCanvas: true, PrintPreview: true } } })
    addSection(store, 'FIXED')
    const dirty = store.serialize()
    const button = label => wrapper.findAll('button').find(b => b.text() === label)
    expect(button('新建')).toBeUndefined()
    expect(await wrapper.vm.canLeave()).toBe(false)
    expect(confirmDiscard).toHaveBeenCalled()
    expect(store.serialize()).toBe(dirty)
    await button('保存').trigger('click')
    await flushPromises()
    expect(save).toHaveBeenCalledOnce()
    expect(store.dirty).toBe(false)
    wrapper.unmount()
  })
  it('selects a rectangle of elements and moves them together with arrow keys', async () => {
    addSection(store, 'FIXED')
    addElement(store, 'TEXT', undefined, { xMm: 3, yMm: 3 })
    addElement(store, 'TEXT', undefined, { xMm: 60, yMm: 3 })
    store.zoom = 1
    const wrapper = mount(PrintCanvas, { global: { plugins: [pinia] } })
    const surface = wrapper.get(`[data-surface-id="${store.surfaceId}"]`)
    await surface.trigger('pointerdown', { button: 0, clientX: 0, clientY: 0 })
    const move = new Event('pointermove')
    Object.assign(move, { clientX: mmToPx(110), clientY: mmToPx(15) })
    window.dispatchEvent(move)
    window.dispatchEvent(new Event('pointerup'))
    expect(store.selectedIds).toHaveLength(2)
    await wrapper.get('.print-canvas').trigger('keydown', { key: 'ArrowRight', shiftKey: true })
    expect(store.selectedElements.map(e => e.xMm)).toEqual([8, 65])
    wrapper.unmount()
  })

  it('preserves the current template when a paper edit would clip elements', () => {
    addSection(store, 'FIXED')
    addElement(store, 'TEXT', undefined, { xMm: 140, yMm: 3 })
    const before = store.serialize()
    expect(store.execute((doc) => {
      doc.paper.widthMm = 148
    })).toBe(false)
    expect(store.serialize()).toBe(before)
  })

  it('locates invalid fields and blocks oversize imports before parsing', () => {
    addSection(store, 'FIXED')
    addField(store, 'main.name')
    store.catalog = []
    expect(store.fieldIssues[0].path).toMatch(/^body\[\d+\]\.elements\[0\]\.binding$/)
    expect(() => parseDraft(' '.repeat(1024 * 1024 + 1))).toThrow('模板超出允许大小')
  })

  it('does not report local storage quota failures as saved', async () => {
    addSection(store, 'TEXT')
    const storage = { setItem: () => {
      throw new Error('quota')
    } }
    expect(await store.save(doc => writeDraft(doc, storage))).toBe(false)
    expect(store.dirty).toBe(true)
    expect(store.error).toContain('quota')
  })
  it('edits merged table headers atomically and can undo footer creation', async () => {
    store.catalog.push({ path: 'children.lines.amount', type: 'MONEY' })
    addSection(store, 'TABLE')
    const wrapper = mount(TableBandsPanel, { props: { mode: 'header' }, global: { plugins: [pinia] } })
    const button = label => wrapper.findAll('button').find(b => b.text().includes(label))
    await button('添加上层表头').trigger('click')
    await wrapper.vm.$nextTick()
    expect(store.activeSurface.headerRows.map(row => row.cells.map(cell => cell.span))).toEqual([[2], [1, 1]])
    // Select top header cell then split / merge
    const chip = wrapper.find('.band-cell-chip')
    expect(chip.exists()).toBe(true)
    await chip.trigger('click')
    await wrapper.vm.$nextTick()
    await button('拆分一列').trigger('click')
    expect(store.activeSurface.headerRows[0].cells.map(cell => cell.span)).toEqual([1, 1])
    await wrapper.find('.band-cell-chip').trigger('click')
    await wrapper.vm.$nextTick()
    await button('合并右侧').trigger('click')
    expect(store.activeSurface.headerRows[0].cells.map(cell => cell.span)).toEqual([2])
    wrapper.unmount()

    const footerPanel = mount(TableBandsPanel, { props: { mode: 'footer' }, global: { plugins: [pinia] } })
    const footerButton = label => footerPanel.findAll('button').find(b => b.text().includes(label))
    await footerButton('添加合计行').trigger('click')
    expect(store.activeSurface.footer.cells).toHaveLength(2)
    store.undo()
    expect(store.activeSurface.footer).toBeUndefined()
    footerPanel.unmount()
  })
})
