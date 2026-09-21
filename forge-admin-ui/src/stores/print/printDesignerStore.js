import { defineStore } from 'pinia'
import { applyLineGeometrySideEffects, applyLineStyleSideEffects, clampElementToContent, findSurface, newPrintId, normalizeTableColumnWidths, resizeElement, selectionBounds, snapResize, snapTranslation, translateElements } from '../../components/print/designer/commands'
import { nextPrintZoom, PRINT_ZOOM_LEVELS } from '../../components/print/designer/designerView'
import { cloneDocument, createHistory, recordChange, travelHistory } from '../../components/print/designer/history'
import { clearStaticTableBandCellStyles, deleteStaticTableColumns, deleteStaticTableRows, insertStaticTableColumns, insertStaticTableRows, mergeStaticTableCells, renewPrintElementIds, splitStaticTableCell, staticTableSize, styleStaticTableRowsAsHeader } from '../../components/print/designer/staticTable'
import { validateFieldCatalog } from '../../components/print/protocol/fieldCatalog'
import { formatPrintApiError } from '../../components/print/protocol/formatPrintError'
import { normalizeStyleColors, sanitizePrintColors } from '../../components/print/protocol/printColor'
import { createPrintDocument, PRINT_LIMITS } from '../../components/print/protocol/types'
import { paperGeometry, screenDeltaToMm } from '../../components/print/protocol/units'
import { assertPrintDocument } from '../../components/print/protocol/validate'

function ensureFreeCanvasDocument(doc) {
  const existing = doc.body.find(section => section.kind === 'FIXED')
  if (existing)
    return existing
  if (doc.body.length > 0)
    return null
  const heightMm = Math.max(120, Number((paperGeometry(doc).contentHeightMm || 200).toFixed(3)))
  const canvas = { id: newPrintId(), kind: 'FIXED', heightMm, elements: [], gapAfterMm: 2 }
  doc.body.unshift(canvas)
  return canvas
}

export const usePrintDesignerStore = defineStore('printDesigner', {
  state: () => ({
    document: createPrintDocument(),
    catalog: [],
    surfaceId: 'header',
    selectedIds: [],
    tableCellIds: [],
    tableColumnId: '',
    tableColumnIds: [],
    /** DATA_TABLE box selection: { top, bottom, left, right } over preview rows/cols */
    tableSelectionRange: null,
    /** Concrete cells for box selection: [{ kind, kindIndex, columnId }] */
    tableSelectionCells: [],
    zoom: 0.8,
    showGrid: true,
    snapEnabled: true,
    showGuideLines: false,
    /** Session-only ruler guides: { id, axis: 'x'|'y', positionMm } */
    userGuides: [],
    guidePreview: null,
    selectedGuideId: '',
    miniMapOpen: true,
    history: createHistory(),
    saved: '',
    gesture: null,
    clipboard: [],
    cellClipboard: null,
    error: '',
    notice: '',
    saving: false,
    generation: 0,
    previewOpen: false,
    leftPanelOpen: true,
    rightPanelOpen: true,
    /** Blank-table insert count for ↑↓←→ / context menu (default 1). */
    staticTableInsertCount: 1,
    alignmentGuides: { x: [], y: [], position: null },
    marquee: null,
  }),
  getters: {
    activeSurface: state => findSurface(state.document, state.surfaceId),
    selectedElements() {
      return this.activeSurface?.elements?.filter(e => this.selectedIds.includes(e.id)) || []
    },
    activeElement() {
      return this.selectedElements.length === 1 ? this.selectedElements[0] : null
    },
    selectedTableCells() {
      return this.activeElement?.type === 'STATIC_TABLE' ? this.activeElement.table.cells.filter(cell => this.tableCellIds.includes(cell.id)) : []
    },
    activeTableCell() {
      return this.selectedTableCells[0] || null
    },
    selectedTableColumns() {
      const table = this.activeElement?.type === 'DATA_TABLE'
        ? this.activeElement
        : (this.activeSurface?.kind === 'TABLE' ? this.activeSurface : null)
      if (!table?.columns?.length)
        return []
      const ids = this.tableColumnIds.length
        ? this.tableColumnIds
        : (this.tableColumnId ? [this.tableColumnId] : [])
      return table.columns.filter(column => ids.includes(column.id))
    },
    hasLockedSelection() {
      return this.selectedElements.some(element => element.locked)
    },
    dirty: state => JSON.stringify(state.document) !== state.saved,
    canUndo: state => state.history.past.length > 0,
    canRedo: state => state.history.future.length > 0,
    canZoomIn: state => state.zoom < PRINT_ZOOM_LEVELS.at(-1),
    canZoomOut: state => state.zoom > PRINT_ZOOM_LEVELS[0],
    fieldIssues: state => validateFieldCatalog(state.document, state.catalog),
  },
  actions: {
    nudgeZoom(step) {
      this.zoom = nextPrintZoom(this.zoom, step)
    },
    setZoom(value) {
      if (PRINT_ZOOM_LEVELS.includes(value))
        this.zoom = value
    },
    rotatePaper() {
      return this.execute((doc) => {
        doc.paper.orientation = doc.paper.orientation === 'PORTRAIT' ? 'LANDSCAPE' : 'PORTRAIT'
        this.clampDocumentToPaper(doc)
      })
    },
    setPaperSize(widthMm, heightMm) {
      if (!Number.isFinite(widthMm) || !Number.isFinite(heightMm) || widthMm < 20 || heightMm < 20)
        return false
      return this.execute((doc) => {
        doc.paper.widthMm = Number(widthMm.toFixed(3))
        doc.paper.heightMm = Number(heightMm.toFixed(3))
        if (widthMm !== heightMm)
          doc.paper.orientation = widthMm > heightMm ? 'LANDSCAPE' : 'PORTRAIT'
        this.clampDocumentToPaper(doc)
      })
    },
    /** Keep free elements / detail tables inside the printable content after paper changes. */
    clampDocumentToPaper(doc) {
      const contentWidth = paperGeometry(doc).contentWidthMm
      const visit = (surface) => {
        if (!surface?.elements)
          return
        surface.elements.forEach((element) => {
          clampElementToContent(doc, element)
          if (Number.isFinite(surface.heightMm) && element.yMm + element.heightMm > surface.heightMm)
            surface.heightMm = Number((element.yMm + element.heightMm).toFixed(3))
        })
      }
      visit(doc.header)
      visit(doc.footer)
      doc.body.forEach((section) => {
        if (section.kind === 'FIXED')
          visit(section)
        if (section.kind === 'TABLE' && section.columns?.length)
          normalizeTableColumnWidths(section.columns, contentWidth)
      })
    },
    toggleGrid(value) {
      this.showGrid = value ?? !this.showGrid
    },
    toggleSnap(value) {
      this.snapEnabled = value ?? !this.snapEnabled
    },
    toggleGuideLines(value) {
      this.showGuideLines = value ?? !this.showGuideLines
    },
    setGuidePreview(preview) {
      this.guidePreview = preview && Number.isFinite(preview.positionMm)
        ? { axis: preview.axis === 'y' ? 'y' : 'x', positionMm: Number(preview.positionMm.toFixed(2)) }
        : null
    },
    clearGuidePreview() {
      this.guidePreview = null
    },
    addUserGuide({ axis, positionMm }) {
      if (!['x', 'y'].includes(axis) || !Number.isFinite(positionMm))
        return null
      const id = newPrintId()
      this.userGuides = [...this.userGuides, { id, axis, positionMm: Number(positionMm.toFixed(2)) }]
      this.selectedGuideId = id
      this.guidePreview = null
      this.selectedIds = []
      this.tableCellIds = []
      this.tableColumnId = ''
      this.tableColumnIds = []
      this.tableSelectionRange = null
      this.tableSelectionCells = []
      return id
    },
    selectGuide(id) {
      this.selectedGuideId = this.userGuides.some(guide => guide.id === id) ? id : ''
      if (this.selectedGuideId) {
        this.selectedIds = []
        this.tableCellIds = []
        this.tableColumnId = ''
        this.tableColumnIds = []
        this.tableSelectionRange = null
        this.tableSelectionCells = []
      }
    },
    moveUserGuide(id, positionMm) {
      if (!Number.isFinite(positionMm))
        return
      this.userGuides = this.userGuides.map(guide => (
        guide.id === id
          ? { ...guide, positionMm: Number(Math.max(0, positionMm).toFixed(2)) }
          : guide
      ))
    },
    removeUserGuide(id) {
      this.userGuides = this.userGuides.filter(guide => guide.id !== id)
      if (this.selectedGuideId === id)
        this.selectedGuideId = ''
    },
    clearSelectedGuide() {
      this.selectedGuideId = ''
    },
    clearAllUserGuides() {
      this.userGuides = []
      this.selectedGuideId = ''
      this.guidePreview = null
    },
    toggleMiniMap(value) {
      this.miniMapOpen = value ?? !this.miniMapOpen
    },
    expandBand(band, heightMm = 12) {
      if (!['header', 'footer'].includes(band) || !Number.isFinite(heightMm) || heightMm <= 0)
        return false
      return this.execute((doc) => {
        doc[band].heightMm = Number(heightMm.toFixed(3))
      })
    },
    selectByBox(surfaceId, box, previousIds = []) {
      const surface = findSurface(this.document, surfaceId)
      if (!surface?.elements || !box)
        return []
      const ids = surface.elements
        .filter(element => element.xMm < box.x + box.w && element.xMm + element.widthMm > box.x && element.yMm < box.y + box.h && element.yMm + element.heightMm > box.y)
        .map(element => element.id)
      this.surfaceId = surfaceId.startsWith('section:') || surfaceId === 'header' || surfaceId === 'footer' ? surfaceId : `section:${surfaceId}`
      this.selectedIds = [...new Set([...previousIds, ...ids])]
      this.tableCellIds = []
      this.marquee = { ...box, id: this.surfaceId, count: this.selectedIds.length }
      return this.selectedIds
    },
    clearMarquee() {
      this.marquee = null
    },
    load(document, catalog = this.catalog) {
      assertPrintDocument(document)
      this.document = cloneDocument(document)
      ensureFreeCanvasDocument(this.document)
      this.catalog = cloneDocument(catalog)
      this.saved = this.serialize()
      this.history = createHistory()
      this.selectedIds = []
      this.tableCellIds = []
      this.tableColumnId = ''
      this.tableColumnIds = []
      this.tableSelectionRange = null
      this.tableSelectionCells = []
      const firstFixed = this.document.body.find(section => section.kind === 'FIXED')
      this.surfaceId = firstFixed ? `section:${firstFixed.id}` : (this.document.body.length ? `section:${this.document.body[0].id}` : 'header')
      this.gesture = null
      this.clipboard = []
      this.error = ''
      this.notice = ''
      this.saving = false
      this.previewOpen = false
      this.alignmentGuides = { x: [], y: [], position: null }
      this.marquee = null
      this.generation++
    },
    ensureFreeCanvas() {
      const existing = this.document.body.find(section => section.kind === 'FIXED')
      if (existing) {
        this.selectSurface(existing.id)
        return existing
      }
      let canvas = null
      this.execute((doc) => {
        const heightMm = Math.max(120, Number((paperGeometry(doc).contentHeightMm || 200).toFixed(3)))
        canvas = { id: newPrintId(), kind: 'FIXED', heightMm, elements: [], gapAfterMm: 2 }
        doc.body.unshift(canvas)
      })
      if (canvas)
        this.selectSurface(canvas.id)
      return canvas
    },
    collapseBand(band) {
      if (!['header', 'footer'].includes(band))
        return false
      return this.execute((doc) => {
        doc[band].heightMm = 0
      })
    },
    serialize() {
      return JSON.stringify(sanitizePrintColors(cloneDocument(this.document)))
    },
    selectSurface(id) {
      if (id !== 'header' && id !== 'footer' && !id.startsWith('section:')) {
        id = `section:${id}`
      }
      if (this.surfaceId !== id) {
        this.cancelGesture()
        this.surfaceId = id
        this.selectedIds = []
        this.tableCellIds = []
        this.tableColumnId = ''
        this.tableColumnIds = []
        this.tableSelectionRange = null
        this.tableSelectionCells = []
      }
    },
    selectElement(id, additive = false) {
      const sameSingle = this.selectedIds.length === 1 && this.selectedIds[0] === id
      if (additive) {
        this.selectedIds = this.selectedIds.includes(id) ? this.selectedIds.filter(value => value !== id) : [...this.selectedIds, id]
      }
      else {
        this.selectedIds = [id]
      }
      this.selectedGuideId = ''
      if (additive || !sameSingle) {
        this.tableCellIds = []
        this.tableColumnId = ''
        this.tableColumnIds = []
        this.tableSelectionRange = null
        this.tableSelectionCells = []
      }
    },
    selectTableCell(id, additive = false) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || !this.activeElement.table.cells.some(cell => cell.id === id))
        return
      this.tableColumnId = ''
      this.tableColumnIds = []
      this.tableSelectionRange = null
      this.tableSelectionCells = []
      this.tableCellIds = additive
        ? (this.tableCellIds.includes(id) ? this.tableCellIds.filter(value => value !== id) : [...this.tableCellIds, id])
        : [id]
    },
    selectTableCells(ids = []) {
      if (this.activeElement?.type !== 'STATIC_TABLE')
        return
      const allowed = new Set(this.activeElement.table.cells.map(cell => cell.id))
      this.tableColumnId = ''
      this.tableColumnIds = []
      this.tableSelectionRange = null
      this.tableSelectionCells = []
      this.tableCellIds = [...new Set(ids.filter(id => allowed.has(id)))]
    },
    selectTableColumn(id) {
      this.selectTableColumns(id ? [id] : [])
    },
    selectTableColumns(ids = [], range = null, cells = null) {
      const list = [...new Set(ids.filter(Boolean))]
      if (!list.length)
        return
      let table = this.activeElement?.type === 'DATA_TABLE'
        ? this.activeElement
        : (this.activeSurface?.kind === 'TABLE' ? this.activeSurface : null)
      if (!table && this.activeSurface?.elements) {
        const hit = this.activeSurface.elements.find(item => item.type === 'DATA_TABLE' && item.columns?.some(column => list.includes(column.id)))
        if (hit) {
          this.selectedIds = [hit.id]
          table = hit
        }
      }
      const allowed = new Set(table?.columns?.map(column => column.id) || [])
      const next = list.filter(id => allowed.has(id))
      if (!next.length)
        return
      this.tableCellIds = []
      this.tableColumnId = next[0]
      this.tableColumnIds = next
      if (range && Number.isFinite(range.top) && Number.isFinite(range.bottom) && Number.isFinite(range.left) && Number.isFinite(range.right)) {
        this.tableSelectionRange = {
          top: Math.min(range.top, range.bottom),
          bottom: Math.max(range.top, range.bottom),
          left: Math.min(range.left, range.right),
          right: Math.max(range.left, range.right),
        }
        this.tableSelectionCells = Array.isArray(cells)
          ? cells.filter(cell => cell?.columnId && allowed.has(cell.columnId) && ['header', 'data', 'footer'].includes(cell.kind) && Number.isFinite(cell.kindIndex))
          : []
      }
      else {
        this.tableSelectionRange = null
        this.tableSelectionCells = []
      }
    },
    clearTableColumn() {
      this.tableColumnId = ''
      this.tableColumnIds = []
      this.tableSelectionRange = null
      this.tableSelectionCells = []
    },
    selectAll() {
      this.selectedIds = this.activeSurface?.elements?.map(element => element.id) || []
    },
    reconcileSelection() {
      if (!this.activeSurface) {
        this.surfaceId = this.document.body.length ? `section:${this.document.body[0].id}` : 'header'
      }
      this.selectedIds = this.selectedIds.filter(id => this.activeSurface?.elements?.some(e => e.id === id))
      if (this.activeElement?.type !== 'STATIC_TABLE')
        this.tableCellIds = []
      else this.tableCellIds = this.tableCellIds.filter(id => this.activeElement.table.cells.some(cell => cell.id === id))
      const table = this.activeElement?.type === 'DATA_TABLE'
        ? this.activeElement
        : (this.activeSurface?.kind === 'TABLE' ? this.activeSurface : null)
      const allowedColumns = new Set(table?.columns?.map(column => column.id) || [])
      this.tableColumnIds = this.tableColumnIds.filter(id => allowedColumns.has(id))
      if (!this.tableColumnIds.includes(this.tableColumnId))
        this.tableColumnId = this.tableColumnIds[0] || ''
      if (this.tableColumnId && !this.tableColumnIds.includes(this.tableColumnId))
        this.tableColumnIds = [this.tableColumnId]
      if (!this.tableColumnIds.length) {
        this.tableSelectionRange = null
        this.tableSelectionCells = []
      }
    },
    execute(change) {
      this.cancelGesture()
      const candidate = cloneDocument(this.document)
      try {
        change(candidate)
        // Keep STATIC_TABLE element box in sync with track sums (avoids preview TABLE_SIZE_MISMATCH).
        const syncStatic = (elements = []) => {
          elements.forEach((element) => {
            if (element?.type === 'STATIC_TABLE' && element.table)
              Object.assign(element, staticTableSize(element.table))
          })
        }
        syncStatic(candidate.header?.elements)
        syncStatic(candidate.footer?.elements)
        candidate.body?.forEach(section => syncStatic(section.elements))
        sanitizePrintColors(candidate)
        assertPrintDocument(candidate)
        recordChange(this.history, this.document, candidate)
        this.document = candidate
        this.reconcileSelection()
        this.error = ''
        this.notice = ''
        return true
      }
      catch (error) {
        this.error = `${error.message}${error.path ? `（${error.path}）` : ''}`
        return false
      }
    },
    patchSelected(patch) {
      return this.execute((document) => {
        findSurface(document, this.surfaceId)?.elements?.filter(e => this.selectedIds.includes(e.id)).forEach((e) => {
          const next = cloneDocument(patch)
          if (e.type === 'STATIC_TABLE' && (next.widthMm !== undefined || next.heightMm !== undefined)) {
            resizeElement(document, this.surfaceId, e.id, (next.widthMm ?? e.widthMm) - e.widthMm, (next.heightMm ?? e.heightMm) - e.heightMm)
            delete next.widthMm
            delete next.heightMm
          }
          if (next.style) {
            if (e.type === 'LINE')
              applyLineStyleSideEffects(e, next.style)
            else
              e.style = { ...e.style, ...next.style }
            delete next.style
          }
          Object.assign(e, next)
          applyLineGeometrySideEffects(e, next)
        })
      })
    },
    /** 样式 → 表头/表体：写 band 并从对应行格子上拿掉同名覆盖色，避免默认白/灰底挡住表头背景. */
    patchStaticTableBand(band, stylePatch) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked || !stylePatch)
        return false
      const field = band === 'header' ? 'headerStyle' : 'style'
      const elementId = this.activeElement.id
      return this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        element[field] = { ...element[field], ...cloneDocument(stylePatch) }
        clearStaticTableBandCellStyles(element.table, band, Object.keys(stylePatch))
      })
    },
    patchSelectedTableCells(patch) {
      if (!this.tableCellIds.length || this.activeElement?.locked)
        return false
      const elementId = this.activeElement.id
      return this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        element.table.cells.filter(cell => this.tableCellIds.includes(cell.id)).forEach(cell => Object.assign(cell, cloneDocument(patch)))
      })
    },
    /** Patch one blank-table cell by id (safe while focus/selection is changing). */
    patchStaticTableCell(cellId, patch) {
      if (!cellId || this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const elementId = this.activeElement.id
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        const cell = element?.table?.cells?.find(item => item.id === cellId)
        if (!cell)
          throw new Error('单元格不存在')
        Object.assign(cell, cloneDocument(patch))
      })
      if (ok && !this.tableCellIds.includes(cellId))
        this.tableCellIds = [cellId]
      return ok
    },
    setStaticTableCellsImage(cellIds = [], fileId = '') {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const ids = [...new Set(cellIds.filter(Boolean))]
      if (!ids.length || !fileId)
        return false
      const elementId = this.activeElement.id
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        element.table.cells.filter(cell => ids.includes(cell.id)).forEach((cell) => {
          cell.contentType = 'IMAGE'
          cell.binding = { source: 'CONSTANT', value: fileId }
          delete cell.format
          // Fill the cell by default (minus padding), so preview is not a tiny 8mm strip.
          const width = element.table.columns
            .slice(cell.column, cell.column + cell.colSpan)
            .reduce((sum, col) => sum + col.widthMm, 0)
          const height = element.table.rows
            .slice(cell.row, cell.row + cell.rowSpan)
            .reduce((sum, row) => sum + row.heightMm, 0)
          cell.imageWidthMm = Number(Math.max(3, width - 2).toFixed(2))
          cell.imageHeightMm = Number(Math.max(3, height - 2).toFixed(2))
        })
        Object.assign(element, staticTableSize(element.table))
      })
      if (ok) {
        this.tableCellIds = ids
        this.notice = `已插入图片到 ${ids.length} 个单元格`
      }
      return ok
    },
    clearStaticTableCellsImage(cellIds = []) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const ids = [...new Set(cellIds.filter(Boolean))]
      if (!ids.length)
        return false
      const elementId = this.activeElement.id
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        element.table.cells.filter(cell => ids.includes(cell.id)).forEach((cell) => {
          delete cell.contentType
          cell.binding = { source: 'CONSTANT', value: '' }
        })
      })
      if (ok) {
        this.tableCellIds = ids
        this.notice = '已清除单元格图片'
      }
      return ok
    },
    patchSelectedTableCellStyle(patch) {
      if (!this.tableCellIds.length || this.activeElement?.locked)
        return false
      patch = normalizeStyleColors(patch)
      const elementId = this.activeElement.id
      return this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        element.table.cells.filter(cell => this.tableCellIds.includes(cell.id)).forEach(cell => cell.style = { ...cell.style, ...cloneDocument(patch) })
      })
    },
    /** Apply style to box-selected DATA_TABLE cells only (not whole column). */
    patchSelectedDataTableColumnStyles(patch) {
      const columns = this.selectedTableColumns
      if (!columns.length || this.activeElement?.type !== 'DATA_TABLE' || this.activeElement.locked)
        return false
      const elementId = this.activeElement.id
      const ids = columns.map(column => column.id)
      const cells = this.tableSelectionCells?.length
        ? this.tableSelectionCells
        : null
      return this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        const headerOnly = cells?.length && cells.every(item => item.kind === 'header')
        if (headerOnly) {
          const columnIds = new Set(cells.map(item => item.columnId))
          const wholeHeader = columnIds.size === element.columns.length && cells.every(item => item.kindIndex === 0)
          if (wholeHeader) {
            element.headerStyle = { ...element.headerStyle, ...cloneDocument(patch) }
            return
          }
          if (!element.cellStyles || typeof element.cellStyles !== 'object')
            element.cellStyles = {}
          cells.forEach(({ kind, kindIndex, columnId }) => {
            const key = `${kind}:${kindIndex}:${columnId}`
            element.cellStyles[key] = { ...element.cellStyles[key], ...cloneDocument(patch) }
          })
          return
        }
        // Box selection → only the concrete cells captured at select time.
        if (cells?.length) {
          if (!element.cellStyles || typeof element.cellStyles !== 'object')
            element.cellStyles = {}
          cells.forEach(({ kind, kindIndex, columnId }) => {
            const key = `${kind}:${kindIndex}:${columnId}`
            element.cellStyles[key] = { ...element.cellStyles[key], ...cloneDocument(patch) }
          })
          return
        }
        // Column list / no cell list → whole selected columns (body style).
        element.columns.filter(column => ids.includes(column.id)).forEach((column) => {
          column.style = { ...column.style, ...cloneDocument(patch) }
        })
      })
    },
    /** Live drag resize: mutate from gesture snapshot without flooding undo. */
    applyLiveDocument(mutator) {
      if (typeof mutator !== 'function')
        return false
      if (!this.gesture)
        this.beginGesture()
      if (!this.gesture)
        return false
      const candidate = cloneDocument(this.gesture.before)
      try {
        mutator(candidate)
      }
      catch {
        return false
      }
      this.document = candidate
      return true
    },
    resizeStaticTableTrack(axis, index, sizeMm, { live = false, originMm } = {}) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked || !Number.isFinite(sizeMm))
        return false
      const elementId = this.activeElement.id
      const next = Math.max(4, Number(sizeMm.toFixed(2)))
      const apply = (document) => {
        const surface = findSurface(document, this.surfaceId)
        const element = surface.elements.find(item => item.id === elementId)
        const tracks = axis === 'row' ? element.table.rows : element.table.columns
        const key = axis === 'row' ? 'heightMm' : 'widthMm'
        tracks[index][key] = next
        Object.assign(element, staticTableSize(element.table))
        if (Number.isFinite(originMm)) {
          if (axis === 'column')
            element.xMm = Math.max(0, Number(originMm.toFixed(3)))
          else
            element.yMm = Math.max(0, Number(originMm.toFixed(3)))
        }
        surface.heightMm = Math.max(surface.heightMm, Number((element.yMm + element.heightMm).toFixed(3)))
      }
      return live ? this.applyLiveDocument(apply) : this.execute(apply)
    },
    resizeDataTableColumn(index, widthMm, { live = false } = {}) {
      if (this.activeElement?.type !== 'DATA_TABLE' || this.activeElement.locked || !Number.isFinite(widthMm))
        return false
      const elementId = this.activeElement.id
      const next = Math.max(4, Number(widthMm.toFixed(2)))
      const apply = (document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        if (!element?.columns?.[index])
          throw new Error('列不存在')
        const last = index === element.columns.length - 1
        element.columns[index].widthMm = next
        if (last) {
          element.widthMm = Number(element.columns.reduce((sum, column) => sum + column.widthMm, 0).toFixed(3))
        }
        else {
          const total = element.widthMm || element.columns.reduce((sum, column) => sum + column.widthMm, 0)
          normalizeTableColumnWidths(element.columns, total)
          element.widthMm = total
        }
      }
      return live ? this.applyLiveDocument(apply) : this.execute(apply)
    },
    patchStaticTableCellImageSize(cellId, size = {}, { live = false } = {}) {
      if (!cellId || this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const elementId = this.activeElement.id
      const apply = (document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        const cell = element?.table?.cells?.find(item => item.id === cellId)
        if (!cell || cell.contentType !== 'IMAGE')
          throw new Error('当前单元格不是图片')
        if (Number.isFinite(size.imageWidthMm))
          cell.imageWidthMm = Math.max(3, Number(size.imageWidthMm.toFixed(2)))
        if (Number.isFinite(size.imageHeightMm))
          cell.imageHeightMm = Math.max(3, Number(size.imageHeightMm.toFixed(2)))
        const row = element.table.rows[cell.row]
        if (row && cell.imageHeightMm)
          row.heightMm = Math.max(row.heightMm, Number((cell.imageHeightMm + 2).toFixed(2)))
        Object.assign(element, staticTableSize(element.table))
      }
      return live ? this.applyLiveDocument(apply) : this.execute(apply)
    },
    patchStaticTableTrack(axis, index, value) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked || !['row', 'column'].includes(axis) || !Number.isFinite(value))
        return false
      const elementId = this.activeElement.id
      return this.execute((document) => {
        const surface = findSurface(document, this.surfaceId)
        const element = surface.elements.find(item => item.id === elementId)
        const tracks = axis === 'row' ? element.table.rows : element.table.columns
        const key = axis === 'row' ? 'heightMm' : 'widthMm'
        tracks[index][key] = value
        Object.assign(element, staticTableSize(element.table))
        surface.heightMm = Math.max(surface.heightMm, Number((element.yMm + element.heightMm).toFixed(3)))
      })
    },
    patchSurface(patch) {
      return this.execute(document => Object.assign(findSurface(document, this.surfaceId), cloneDocument(patch)))
    },
    moveSelection(dx, dy) {
      if (this.hasLockedSelection)
        return false
      return this.execute(document => translateElements(document, this.surfaceId, this.selectedIds, dx, dy))
    },
    beginGesture() {
      if (this.hasLockedSelection)
        return false
      this.cancelGesture()
      this.gesture = { before: cloneDocument(this.document), surfaceId: this.surfaceId, ids: [...this.selectedIds], zoom: this.zoom }
      this.alignmentGuides = { x: [], y: [], position: null }
      return true
    },
    moveGesture(dx, dy, resize = false, handle = 'se', options = {}) {
      if (!this.gesture) {
        return
      }
      const { before, surfaceId, ids, zoom } = this.gesture
      const candidate = cloneDocument(before)
      const x = screenDeltaToMm(dx, zoom)
      const y = screenDeltaToMm(dy, zoom)
      const threshold = options.snap === false || !this.snapEnabled ? 0 : 0.8
      if (resize) {
        const snapped = snapResize(before, surfaceId, ids[0], x, y, threshold, handle)
        resizeElement(candidate, surfaceId, ids[0], snapped.dx, snapped.dy, handle)
        this.alignmentGuides = snapped.guides
      }
      else {
        const snapped = snapTranslation(before, surfaceId, ids, x, y, threshold)
        translateElements(candidate, surfaceId, ids, snapped.dx, snapped.dy)
        this.alignmentGuides = snapped.guides
      }
      this.document = candidate
    },
    endGesture() {
      if (this.gesture) {
        // Sync static table boxes before recording history.
        const syncStatic = (elements = []) => {
          elements.forEach((element) => {
            if (element?.type === 'STATIC_TABLE' && element.table)
              Object.assign(element, staticTableSize(element.table))
          })
        }
        syncStatic(this.document.header?.elements)
        syncStatic(this.document.footer?.elements)
        this.document.body?.forEach(section => syncStatic(section.elements))
        recordChange(this.history, this.gesture.before, this.document)
        this.gesture = null
        this.alignmentGuides = { x: [], y: [], position: null }
      }
    },
    cancelGesture() {
      if (this.gesture) {
        this.document = this.gesture.before
        this.gesture = null
      }
      this.alignmentGuides = { x: [], y: [], position: null }
    },
    undo() {
      this.cancelGesture()
      this.document = travelHistory(this.history, this.document, 'undo')
      this.reconcileSelection()
      this.error = ''
      this.notice = ''
    },
    redo() {
      this.cancelGesture()
      this.document = travelHistory(this.history, this.document, 'redo')
      this.reconcileSelection()
      this.error = ''
      this.notice = ''
    },
    copySelection() {
      if (this.tableCellIds.length && this.activeElement?.type === 'STATIC_TABLE')
        return this.copyTableCells()
      if (this.selectedTableColumns.length && this.activeElement?.type === 'DATA_TABLE')
        return this.copyTableCells()
      const elements = this.selectedElements
      if (!elements.length) {
        this.notice = '请先选中要复制的元素'
        return false
      }
      this.clipboard = cloneDocument(elements)
      this.cellClipboard = null
      this.notice = `已复制 ${elements.length} 个元素，Ctrl+V 粘贴`
      this.error = ''
      return true
    },
    copyTableCells() {
      if (this.activeElement?.type === 'DATA_TABLE') {
        const columns = this.selectedTableColumns
        if (!columns.length) {
          this.notice = '请先选中明细表格列'
          return false
        }
        const indices = columns
          .map(column => this.activeElement.columns.findIndex(item => item.id === column.id))
          .filter(index => index >= 0)
          .sort((a, b) => a - b)
        const origin = indices[0]
        this.cellClipboard = {
          kind: 'DATA_COLUMNS',
          cells: indices.map((index) => {
            const column = this.activeElement.columns[index]
            return {
              column: index - origin,
              field: column.field,
              title: column.title,
              format: cloneDocument(column.format),
              style: cloneDocument(column.style),
            }
          }),
        }
        this.clipboard = []
        this.notice = `已复制 ${indices.length} 列内容，选中目标列后 Ctrl+V 粘贴`
        this.error = ''
        return true
      }
      const cells = this.selectedTableCells
      if (!cells.length) {
        this.notice = '请先选中单元格'
        return false
      }
      const top = Math.min(...cells.map(cell => cell.row))
      const left = Math.min(...cells.map(cell => cell.column))
      this.cellClipboard = {
        kind: 'STATIC_CELLS',
        cells: cloneDocument(cells).map((cell) => {
          const binding = cell.binding?.source
            ? cloneDocument(cell.binding)
            : { source: 'CONSTANT', value: cell.text ?? '' }
          return {
            row: cell.row - top,
            column: cell.column - left,
            binding,
            style: cell.style,
            format: cell.format,
            contentType: cell.contentType === 'IMAGE' ? 'IMAGE' : 'TEXT',
          }
        }),
      }
      this.clipboard = []
      this.notice = `已复制 ${cells.length} 个单元格，选中目标格后 Ctrl+V 粘贴`
      this.error = ''
      return true
    },
    pasteSelection() {
      if (this.cellClipboard?.kind === 'DATA_COLUMNS' && this.activeElement?.type === 'DATA_TABLE')
        return this.pasteTableCells()
      if (this.cellClipboard?.cells?.length && this.activeElement?.type === 'STATIC_TABLE'
        && (this.cellClipboard.kind === 'STATIC_CELLS' || !this.cellClipboard.kind)) {
        return this.pasteTableCells()
      }
      if (!this.activeSurface?.elements || !this.clipboard.length) {
        this.notice = this.clipboard.length ? '当前区块不支持粘贴' : '剪贴板为空，请先复制'
        return false
      }
      const copies = cloneDocument(this.clipboard).map(element => renewPrintElementIds(element, newPrintId))
      const ok = this.execute((document) => {
        findSurface(document, this.surfaceId).elements.push(...copies)
        translateElements(document, this.surfaceId, copies.map(e => e.id), 3, 3)
      })
      if (ok) {
        this.selectedIds = copies.map(e => e.id)
        this.notice = `已粘贴 ${copies.length} 个元素`
      }
      return ok
    },
    pasteTableCells() {
      if (this.cellClipboard?.kind === 'DATA_COLUMNS') {
        if (this.activeElement?.type !== 'DATA_TABLE' || this.activeElement.locked) {
          this.notice = '请先复制列内容，并选中明细表格目标列'
          return false
        }
        const anchors = this.selectedTableColumns
        if (!anchors.length) {
          this.notice = '请先选中要粘贴到的列'
          return false
        }
        const elementId = this.activeElement.id
        const origin = this.activeElement.columns.findIndex(column => column.id === anchors[0].id)
        if (origin < 0)
          return false
        const pasted = []
        const ok = this.execute((document) => {
          const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
          for (const item of this.cellClipboard.cells) {
            const column = element.columns[origin + item.column]
            if (!column)
              continue
            if (item.field !== undefined)
              column.field = item.field
            if (item.title !== undefined)
              column.title = item.title
            if (item.format !== undefined)
              column.format = cloneDocument(item.format)
            if (item.style !== undefined)
              column.style = cloneDocument(item.style)
            pasted.push(column.id)
          }
          if (!pasted.length)
            throw new Error('粘贴列超出表格范围，请换位置')
        })
        if (ok) {
          this.selectTableColumns(pasted)
          this.notice = `已粘贴 ${pasted.length} 列内容`
        }
        return ok
      }
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked || !this.cellClipboard?.cells?.length) {
        this.notice = '请先复制单元格，并选中空白表格中的目标格'
        return false
      }
      const anchor = this.activeTableCell || this.selectedTableCells[0]
      if (!anchor) {
        this.notice = '请先选中要粘贴到的单元格'
        return false
      }
      const elementId = this.activeElement.id
      const originRow = Math.min(...this.selectedTableCells.map(cell => cell.row))
      const originCol = Math.min(...this.selectedTableCells.map(cell => cell.column))
      const pasted = []
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        const table = element.table
        for (const item of this.cellClipboard.cells) {
          const row = originRow + item.row
          const column = originCol + item.column
          if (row < 0 || column < 0 || row >= table.rows.length || column >= table.columns.length)
            continue
          const cell = table.cells.find(c => c.row === row && c.column === column && c.rowSpan === 1 && c.colSpan === 1)
            || table.cells.find(c => c.row <= row && row < c.row + c.rowSpan && c.column <= column && column < c.column + c.colSpan)
          if (!cell)
            continue
          if (item.contentType === 'IMAGE') {
            cell.contentType = 'IMAGE'
            cell.binding = item.binding
              ? cloneDocument(item.binding)
              : { source: 'CONSTANT', value: item.text || '' }
            delete cell.format
          }
          else {
            delete cell.contentType
            if (item.binding !== undefined)
              cell.binding = cloneDocument(item.binding)
            else if (item.text !== undefined)
              cell.binding = { source: 'CONSTANT', value: item.text }
            if (item.format !== undefined)
              cell.format = cloneDocument(item.format)
          }
          if (item.style !== undefined)
            cell.style = cloneDocument(item.style)
          pasted.push(cell.id)
        }
        if (!pasted.length)
          throw new Error('粘贴区域超出表格或目标格已合并，请换位置')
      })
      if (ok) {
        this.tableCellIds = [...new Set(pasted)]
        this.notice = `已粘贴 ${pasted.length} 个单元格`
      }
      return ok
    },
    duplicateSelection() {
      if (!this.selectedElements.length)
        return false
      const copies = cloneDocument(this.selectedElements).map(element => renewPrintElementIds(element, newPrintId))
      const ok = this.execute((document) => {
        findSurface(document, this.surfaceId).elements.push(...copies)
        translateElements(document, this.surfaceId, copies.map(element => element.id), 3, 3)
      })
      if (ok) {
        this.selectedIds = copies.map(element => element.id)
        this.notice = `已克隆 ${copies.length} 个元素`
      }
      return ok
    },
    moveSelectionLayer(position) {
      if (!this.selectedElements.length || this.hasLockedSelection || !['front', 'back', 'forward', 'backward'].includes(position))
        return false
      return this.execute((document) => {
        const surface = findSurface(document, this.surfaceId)
        if (position === 'front' || position === 'back') {
          const selected = surface.elements.filter(element => this.selectedIds.includes(element.id))
          const rest = surface.elements.filter(element => !this.selectedIds.includes(element.id))
          surface.elements = position === 'front' ? [...rest, ...selected] : [...selected, ...rest]
          return
        }
        const selected = new Set(this.selectedIds)
        const next = surface.elements.slice()
        if (position === 'forward') {
          for (let index = next.length - 2; index >= 0; index -= 1) {
            if (selected.has(next[index].id) && !selected.has(next[index + 1].id))
              [next[index], next[index + 1]] = [next[index + 1], next[index]]
          }
        }
        else {
          for (let index = 1; index < next.length; index += 1) {
            if (selected.has(next[index].id) && !selected.has(next[index - 1].id))
              [next[index - 1], next[index]] = [next[index], next[index - 1]]
          }
        }
        surface.elements = next
      })
    },
    cutSelection() {
      if (!this.selectedIds.length || this.hasLockedSelection)
        return false
      this.copySelection()
      return this.removeSelection()
    },
    styleSelectionTargets() {
      if (this.tableCellIds.length && this.activeElement?.type === 'STATIC_TABLE')
        return this.selectedTableCells
      if (this.activeElement?.type === 'STATIC_TABLE')
        return [{ style: this.activeElement.headerStyle || {} }]
      if (this.activeElement?.type === 'DATA_TABLE' && this.tableSelectionCells?.length) {
        if (this.tableSelectionCells.every(hit => hit.kind === 'header'))
          return [{ style: this.activeElement.headerStyle || {} }]
        return this.tableSelectionCells.map((hit) => {
          const key = `${hit.kind}:${hit.kindIndex}:${hit.columnId}`
          return { style: this.activeElement.cellStyles?.[key] || {} }
        })
      }
      if (this.activeElement?.type === 'DATA_TABLE' && this.selectedTableColumns.length)
        return this.selectedTableColumns
      if (this.selectedElements.length)
        return this.selectedElements
      if (this.activeSurface?.kind === 'TEXT')
        return [this.activeSurface]
      return []
    },
    patchSelectionStyle(stylePatch) {
      if (!stylePatch || typeof stylePatch !== 'object')
        return false
      stylePatch = normalizeStyleColors(stylePatch)
      if (this.tableCellIds.length && this.activeElement?.type === 'STATIC_TABLE')
        return this.patchSelectedTableCellStyle(stylePatch)
      if (this.activeElement?.type === 'STATIC_TABLE')
        return this.patchSelected({ headerStyle: { ...this.activeElement.headerStyle, ...cloneDocument(stylePatch) } })
      if (this.activeElement?.type === 'DATA_TABLE' && this.selectedTableColumns.length)
        return this.patchSelectedDataTableColumnStyles(stylePatch)
      if (this.activeElement?.type === 'DATA_TABLE')
        return this.patchSelected({ headerStyle: { ...this.activeElement.headerStyle, ...cloneDocument(stylePatch) } })
      if (!this.selectedIds.length && this.activeSurface?.kind === 'TABLE')
        return this.patchSurface({ headerStyle: { ...this.activeSurface.headerStyle, ...cloneDocument(stylePatch) } })
      if (!this.selectedIds.length && this.activeSurface?.kind === 'TEXT')
        return this.patchSurface({ style: { ...this.activeSurface.style, ...cloneDocument(stylePatch) } })
      if (!this.selectedElements.length || this.hasLockedSelection)
        return false
      return this.execute((document) => {
        findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).forEach((element) => {
          if (element.type === 'LINE')
            applyLineStyleSideEffects(element, stylePatch)
          else
            element.style = { ...element.style, ...cloneDocument(stylePatch) }
        })
      })
    },
    nudgeSelectionFontSize(delta) {
      if (!Number.isFinite(delta))
        return false
      const targets = this.styleSelectionTargets()
      if (!targets.length || this.hasLockedSelection)
        return false
      const current = targets[0]?.style?.fontSizePt || 10
      const next = Math.min(144, Math.max(6, Number((current + delta).toFixed(1))))
      return this.patchSelectionStyle({ fontSizePt: next })
    },
    toggleSelectionBold() {
      const targets = this.styleSelectionTargets()
      if (!targets.length || this.hasLockedSelection)
        return false
      const bold = targets.every(item => item.style?.fontWeight === 700)
      return this.patchSelectionStyle({ fontWeight: bold ? 400 : 700 })
    },
    toggleSelectionItalic() {
      const targets = this.styleSelectionTargets()
      if (!targets.length || this.hasLockedSelection)
        return false
      const italic = targets.every(item => item.style?.fontStyle === 'italic')
      return this.patchSelectionStyle({ fontStyle: italic ? 'normal' : 'italic' })
    },
    toggleSelectionUnderline() {
      const targets = this.styleSelectionTargets()
      if (!targets.length || this.hasLockedSelection)
        return false
      const underlined = targets.every(item => item.style?.textDecoration === 'underline')
      return this.patchSelectionStyle({ textDecoration: underlined ? 'none' : 'underline' })
    },
    setSelectionTextAlign(align) {
      if (!['left', 'center', 'right', 'justify'].includes(align))
        return false
      return this.patchSelectionStyle({ textAlign: align })
    },
    setSelectionVerticalAlign(align) {
      if (!['top', 'middle', 'bottom'].includes(align))
        return false
      return this.patchSelectionStyle({ verticalAlign: align })
    },
    setSelectionFontFamily(fontFamily) {
      if (typeof fontFamily !== 'string' || !fontFamily.trim())
        return false
      return this.patchSelectionStyle({ fontFamily: fontFamily.trim() })
    },
    setSelectionTextDecoration(decoration) {
      if (!['none', 'underline', 'line-through', 'overline'].includes(decoration))
        return false
      return this.patchSelectionStyle({ textDecoration: decoration })
    },
    nudgeSelectionSize(axis, deltaMm) {
      if (!['widthMm', 'heightMm'].includes(axis) || !Number.isFinite(deltaMm) || !this.selectedElements.length || this.hasLockedSelection)
        return false
      return this.execute((document) => {
        findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).forEach((element) => {
          const next = Math.max(1, Number((element[axis] + deltaMm).toFixed(2)))
          element[axis] = next
          clampElementToContent(document, element)
        })
      })
    },
    spaceSelection(axis, gapMm) {
      if (!['horizontal', 'vertical'].includes(axis) || this.selectedElements.length < 2 || this.hasLockedSelection || !Number.isFinite(gapMm) || gapMm < 0)
        return false
      return this.execute((document) => {
        const horizontal = axis === 'horizontal'
        const positionKey = horizontal ? 'xMm' : 'yMm'
        const sizeKey = horizontal ? 'widthMm' : 'heightMm'
        const elements = findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).sort((a, b) => a[positionKey] - b[positionKey])
        let cursor = elements[0][positionKey]
        for (const element of elements) {
          element[positionKey] = Number(cursor.toFixed(3))
          cursor += element[sizeKey] + gapMm
        }
        if (!horizontal) {
          const surface = findSurface(document, this.surfaceId)
          const bottom = Math.max(...elements.map(element => element.yMm + element.heightMm))
          if (Number.isFinite(surface.heightMm))
            surface.heightMm = Math.max(surface.heightMm, Number(bottom.toFixed(3)))
        }
      })
    },
    clearActiveSurfaceElements() {
      if (!this.activeSurface?.elements?.length || this.hasLockedSelection)
        return false
      return this.execute((document) => {
        const surface = findSurface(document, this.surfaceId)
        if (surface?.elements)
          surface.elements = []
      })
    },
    syncSelectionSize(axis, mode) {
      if (!['widthMm', 'heightMm'].includes(axis) || this.selectedElements.length < 2 || this.hasLockedSelection)
        return false
      const values = this.selectedElements.map(element => element[axis])
      let size
      if (mode === 'min')
        size = Math.min(...values)
      else if (mode === 'max')
        size = Math.max(...values)
      else if (mode === 'avg')
        size = values.reduce((sum, value) => sum + value, 0) / values.length
      else if (Number.isFinite(mode))
        size = mode
      else
        return false
      size = Number(Math.max(0.1, size).toFixed(3))
      return this.execute((document) => {
        findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).forEach((element) => {
          if (axis === 'widthMm')
            resizeElement(document, this.surfaceId, element.id, size - element.widthMm, 0, 'e')
          else
            resizeElement(document, this.surfaceId, element.id, 0, size - element.heightMm, 's')
        })
      })
    },
    removeSelection() {
      if (this.selectedGuideId) {
        this.removeUserGuide(this.selectedGuideId)
        return true
      }
      if (this.hasLockedSelection)
        return false
      return this.execute((document) => {
        const surface = findSurface(document, this.surfaceId)
        if (surface?.elements) {
          surface.elements = surface.elements.filter(e => !this.selectedIds.includes(e.id))
        }
      })
    },
    alignSelection(alignment) {
      if (this.selectedElements.length < 2 || this.hasLockedSelection) {
        return false
      }
      return this.execute((document) => {
        const elements = findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id))
        const bounds = selectionBounds(elements)
        for (const element of elements) {
          if (alignment === 'left')
            element.xMm = bounds.xMm
          else if (alignment === 'center')
            element.xMm = Number((bounds.xMm + (bounds.widthMm - element.widthMm) / 2).toFixed(3))
          else if (alignment === 'right')
            element.xMm = Number((bounds.xMm + bounds.widthMm - element.widthMm).toFixed(3))
          else if (alignment === 'top')
            element.yMm = bounds.yMm
          else if (alignment === 'middle')
            element.yMm = Number((bounds.yMm + (bounds.heightMm - element.heightMm) / 2).toFixed(3))
          else if (alignment === 'bottom')
            element.yMm = Number((bounds.yMm + bounds.heightMm - element.heightMm).toFixed(3))
        }
      })
    },
    distributeSelection(axis) {
      if (this.selectedElements.length < 3 || this.hasLockedSelection) {
        return false
      }
      return this.execute((document) => {
        const horizontal = axis === 'horizontal'
        const positionKey = horizontal ? 'xMm' : 'yMm'
        const sizeKey = horizontal ? 'widthMm' : 'heightMm'
        const elements = findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).sort((a, b) => a[positionKey] - b[positionKey])
        const first = elements[0][positionKey]
        const lastEdge = elements.at(-1)[positionKey] + elements.at(-1)[sizeKey]
        const occupied = elements.reduce((total, element) => total + element[sizeKey], 0)
        const gap = (lastEdge - first - occupied) / (elements.length - 1)
        let cursor = first
        for (const element of elements) {
          element[positionKey] = Number(cursor.toFixed(3))
          cursor += element[sizeKey] + gap
        }
      })
    },
    rotateSelection(delta) {
      if (!this.selectedElements.length || this.hasLockedSelection || !Number.isFinite(delta))
        return false
      return this.execute((document) => {
        findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).forEach((element) => {
          const value = (element.rotationDeg || 0) + delta
          element.rotationDeg = ((value + 180) % 360 + 360) % 360 - 180
        })
      })
    },
    flipSelection(axis) {
      if (!this.selectedElements.length || this.hasLockedSelection || !['x', 'y'].includes(axis))
        return false
      return this.execute((document) => {
        const key = axis === 'x' ? 'flipX' : 'flipY'
        findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).forEach((element) => {
          element[key] = !element[key]
        })
      })
    },
    toggleSelectionLock(value) {
      if (!this.selectedElements.length)
        return false
      return this.execute((document) => {
        findSurface(document, this.surfaceId).elements.filter(element => this.selectedIds.includes(element.id)).forEach((element) => {
          element.locked = value ?? !element.locked
        })
      })
    },
    setStaticTableInsertCount(value) {
      const next = Math.floor(Number(value))
      if (!Number.isFinite(next) || next < 1) {
        this.staticTableInsertCount = 1
        return
      }
      this.staticTableInsertCount = Math.min(PRINT_LIMITS.staticTableRows, next)
    },
    addStaticTableRow(count = this.staticTableInsertCount) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const id = this.activeElement.id
      const at = this.activeTableCell ? this.activeTableCell.row + 1 : this.activeElement.table.rows.length
      const n = Math.max(1, Math.floor(Number(count) || 1))
      if (this.activeElement.table.rows.length + n > PRINT_LIMITS.staticTableRows) {
        this.notice = `空白表格最多 ${PRINT_LIMITS.staticTableRows} 行`
        return false
      }
      let created = []
      const ok = this.execute((document) => {
        const surface = findSurface(document, this.surfaceId)
        const element = surface.elements.find(item => item.id === id)
        created = insertStaticTableRows(element.table, at, n)
        Object.assign(element, staticTableSize(element.table))
        surface.heightMm = Math.max(surface.heightMm, Number((element.yMm + element.heightMm).toFixed(3)))
      })
      if (ok)
        this.tableCellIds = created
      return ok
    },
    insertStaticTableRowRelative(where = 'below', count = this.staticTableInsertCount) {
      if (!this.selectedTableCells.length || this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const id = this.activeElement.id
      const at = where === 'above'
        ? Math.min(...this.selectedTableCells.map(cell => cell.row))
        : Math.max(...this.selectedTableCells.map(cell => cell.row + (cell.rowSpan || 1) - 1)) + 1
      const n = Math.max(1, Math.floor(Number(count) || 1))
      if (this.activeElement.table.rows.length + n > PRINT_LIMITS.staticTableRows) {
        this.notice = `空白表格最多 ${PRINT_LIMITS.staticTableRows} 行`
        return false
      }
      let created = []
      const ok = this.execute((document) => {
        const surface = findSurface(document, this.surfaceId)
        const element = surface.elements.find(item => item.id === id)
        created = insertStaticTableRows(element.table, at, n)
        Object.assign(element, staticTableSize(element.table))
        surface.heightMm = Math.max(surface.heightMm, Number((element.yMm + element.heightMm).toFixed(3)))
      })
      if (ok)
        this.tableCellIds = created
      return ok
    },
    addStaticTableColumn(count = this.staticTableInsertCount) {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const id = this.activeElement.id
      const at = this.activeTableCell ? this.activeTableCell.column + 1 : this.activeElement.table.columns.length
      const n = Math.max(1, Math.floor(Number(count) || 1))
      if (this.activeElement.table.columns.length + n > PRINT_LIMITS.staticTableColumns) {
        this.notice = `空白表格最多 ${PRINT_LIMITS.staticTableColumns} 列`
        return false
      }
      let created = []
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === id)
        created = insertStaticTableColumns(element.table, at, n)
        Object.assign(element, staticTableSize(element.table))
        const available = paperGeometry(document).contentWidthMm - element.xMm
        if (element.widthMm > available) {
          const ratio = available / element.widthMm
          element.table.columns.forEach(column => column.widthMm = Number((column.widthMm * ratio).toFixed(3)))
          Object.assign(element, staticTableSize(element.table))
          element.table.columns.at(-1).widthMm = Number((element.table.columns.at(-1).widthMm + available - element.widthMm).toFixed(3))
          element.widthMm = available
        }
      })
      if (ok)
        this.tableCellIds = created
      return ok
    },
    insertStaticTableColumnRelative(where = 'right', count = this.staticTableInsertCount) {
      if (!this.selectedTableCells.length || this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const id = this.activeElement.id
      const at = where === 'left'
        ? Math.min(...this.selectedTableCells.map(cell => cell.column))
        : Math.max(...this.selectedTableCells.map(cell => cell.column + (cell.colSpan || 1) - 1)) + 1
      const n = Math.max(1, Math.floor(Number(count) || 1))
      if (this.activeElement.table.columns.length + n > PRINT_LIMITS.staticTableColumns) {
        this.notice = `空白表格最多 ${PRINT_LIMITS.staticTableColumns} 列`
        return false
      }
      let created = []
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === id)
        created = insertStaticTableColumns(element.table, at, n)
        Object.assign(element, staticTableSize(element.table))
        const available = paperGeometry(document).contentWidthMm - element.xMm
        if (element.widthMm > available) {
          const ratio = available / element.widthMm
          element.table.columns.forEach(column => column.widthMm = Number((column.widthMm * ratio).toFixed(3)))
          Object.assign(element, staticTableSize(element.table))
          element.table.columns.at(-1).widthMm = Number((element.table.columns.at(-1).widthMm + available - element.widthMm).toFixed(3))
          element.widthMm = available
        }
      })
      if (ok)
        this.tableCellIds = created
      return ok
    },
    deleteStaticTableRow() {
      if (!this.selectedTableCells.length || this.activeElement?.locked)
        return false
      const elementId = this.activeElement.id
      const rows = [...new Set(this.selectedTableCells.map(cell => cell.row))]
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        if (!deleteStaticTableRows(element.table, rows))
          throw new Error('空白表格至少保留一行')
        Object.assign(element, staticTableSize(element.table))
      })
      if (ok)
        this.tableCellIds = []
      return ok
    },
    deleteStaticTableColumn() {
      if (!this.selectedTableCells.length || this.activeElement?.locked)
        return false
      const elementId = this.activeElement.id
      const columns = [...new Set(this.selectedTableCells.map(cell => cell.column))]
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        if (!deleteStaticTableColumns(element.table, columns))
          throw new Error('空白表格至少保留一列')
        Object.assign(element, staticTableSize(element.table))
      })
      if (ok)
        this.tableCellIds = []
      return ok
    },
    mergeStaticTableSelection() {
      if (this.selectedTableCells.length < 2 || this.activeElement.locked)
        return false
      const elementId = this.activeElement.id
      let mergedId = null
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        mergedId = mergeStaticTableCells(element.table, this.tableCellIds)
        if (!mergedId)
          throw new Error('请选择连续的矩形单元格区域')
      })
      if (ok)
        this.tableCellIds = [mergedId]
      return ok
    },
    splitStaticTableSelection() {
      if (!this.activeTableCell || this.activeElement.locked)
        return false
      const elementId = this.activeElement.id
      const cellId = this.activeTableCell.id
      let created = []
      const ok = this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        created = splitStaticTableCell(element.table, cellId)
        if (!created.length)
          throw new Error('当前单元格没有合并')
      })
      if (ok)
        this.tableCellIds = created
      return ok
    },
    applyStaticTableHeaderStyle() {
      if (this.activeElement?.type !== 'STATIC_TABLE' || this.activeElement.locked)
        return false
      const elementId = this.activeElement.id
      const rows = this.selectedTableCells.length
        ? [...new Set(this.selectedTableCells.map(cell => cell.row))]
        : [0]
      return this.execute((document) => {
        const element = findSurface(document, this.surfaceId).elements.find(item => item.id === elementId)
        if (rows.includes(0)) {
          element.headerStyle = {
            ...element.headerStyle,
            fontWeight: 700,
            textAlign: 'center',
            backgroundColor: element.headerStyle?.backgroundColor || '#f1f5f9',
          }
        }
        const extra = rows.filter(row => row !== 0)
        if (extra.length)
          styleStaticTableRowsAsHeader(element.table, extra)
      })
    },
    async save(writer) {
      if (this.saving || this.gesture) {
        return false
      }
      const generation = this.generation
      const serialized = this.serialize()
      this.saving = true
      try {
        await writer(JSON.parse(serialized))
        if (this.generation === generation) {
          this.saved = serialized
          this.error = ''
          this.notice = this.dirty ? '已保存此版本，仍有新的修改未保存' : '草稿已保存'
        }
        return true
      }
      catch (error) {
        if (this.generation === generation) {
          this.error = `保存失败：${formatPrintApiError(error, error?.message || '保存失败')}`
        }
        return false
      }
      finally {
        if (this.generation === generation) {
          this.saving = false
        }
      }
    },
  },
})
