<script setup>
import {
  ClipboardOutline,
  CopyOutline,
  DuplicateOutline,
  LockClosedOutline,
  LockOpenOutline,
  ReloadOutline,
  SwapHorizontalOutline,
  SwapVerticalOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NDropdown, NIcon, NModal } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, ref } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { getFileUrl } from '@/utils/file'
import { paperGeometry, screenDeltaToMm } from '../protocol/units'
import { printStyle, tableCellStyle as printTableCellStyle, tableFrameStyle } from '../renderers/style'
import { findSurface } from './commands'
import { estimateDesignerSectionHeight, paginateDesignerBody } from './designerPagination'
import { designerBindingText, designerTablePreview } from './designerSample'
import { addDetailTable, addElement, addField, addSection, PRINT_DRAG_TYPE, readDragItem } from './elementCatalog'
import { cloneDocument } from './history'
import { composeWatermarkPreview } from './paperPanelModel'
import PrintCanvasActionBar from './PrintCanvasActionBar.vue'
import PrintCanvasElement from './PrintCanvasElement.vue'
import { insertRegisteredPrintComponent } from './printComponentRegistry'
import PrintMiniMap from './PrintMiniMap.vue'
import PrintRuler, { PRINT_RULER_SIZE_MM } from './PrintRuler.vue'
import PrintSelectionOverlay from './PrintSelectionOverlay.vue'
import { usePrintDrag } from './usePrintDrag'
import { usePrintKeyboard } from './usePrintKeyboard'

const props = defineProps({
  context: { type: Object, default: () => ({}) },
  resolveFile: { type: Function, default: undefined },
})
const store = usePrintDesignerStore()
const drag = usePrintDrag(store)
const keyboard = usePrintKeyboard(store)
const showCellImageModal = ref(false)
const pendingImageCellIds = ref([])
const cellImageDraft = ref('')
const geometry = computed(() => paperGeometry(store.document))
const overlayUrl = computed(() => {
  const fileId = store.document.paper.designBackground?.fileId
  return fileId ? getFileUrl(fileId) : ''
})
const overlayStyle = computed(() => store.document.paper.designBackground || {})
const watermarkText = computed(() => composeWatermarkPreview(store.document.watermark || {}, store.catalog))
const watermarkStyle = computed(() => store.document.watermark || {})
const sectionNames = { FIXED: '自由画布', TEXT: '流式长文', TABLE: '明细表格' }
const icon = component => () => h(NIcon, null, { default: () => h(component) })
const bandPreview = ref(null)
const designerPages = computed(() => {
  const doc = store.document
  const headerHeight = bandPreview.value?.band === 'header'
    ? bandPreview.value.heightMm
    : doc.header.heightMm
  const footerHeight = bandPreview.value?.band === 'footer'
    ? bandPreview.value.heightMm
    : doc.footer.heightMm
  const plan = paginateDesignerBody(doc, section => estimateDesignerSectionHeight(section, {
    text: section.kind === 'TEXT' ? flowText(section) : '',
    tableRows: section.kind === 'TABLE' ? tableRows(section).length : 0,
    contentWidthMm: geometry.value.contentWidthMm,
  }))
  return {
    ...plan,
    pages: plan.pages.map((page) => {
      const surfaces = [
        {
          ...doc.header,
          id: 'header',
          label: doc.header.repeat || page.number === 1 ? '页眉' : '页眉（本页不重复）',
          pageRole: 'header',
          designHeightMm: headerHeight,
          readOnly: page.number !== 1,
          elements: doc.header.repeat || page.number === 1 ? doc.header.elements : [],
        },
        ...page.sections.map(({ section, heightMm }) => ({
          ...section,
          id: `section:${section.id}`,
          label: `${doc.body.indexOf(section) + 1}. ${sectionNames[section.kind] || section.kind}`,
          pageRole: 'body',
          designHeightMm: heightMm,
          readOnly: false,
        })),
        {
          ...doc.footer,
          id: 'footer',
          label: doc.footer.repeat || page.number === plan.pages.length ? '页脚' : '页脚（本页不重复）',
          pageRole: 'footer',
          designHeightMm: footerHeight,
          readOnly: doc.footer.repeat ? page.number !== 1 : page.number !== plan.pages.length,
          elements: doc.footer.repeat || page.number === plan.pages.length ? doc.footer.elements : [],
        },
      ]
      return {
        ...page,
        surfaces,
        headerSurface: surfaces[0],
        bodySurfaces: surfaces.filter(surface => surface.pageRole === 'body'),
        footerSurface: surfaces.at(-1),
      }
    }),
  }
})
const paperName = computed(() => {
  const { widthMm, heightMm } = store.document.paper
  if (widthMm === 297 && heightMm === 420)
    return 'A3'
  if (widthMm === 210 && heightMm === 297)
    return 'A4'
  if (widthMm === 148 && heightMm === 210)
    return 'A5'
  if (widthMm === 250 && heightMm === 353)
    return 'B4'
  if (widthMm === 176 && heightMm === 250)
    return 'B5'
  return '自定义'
})
const marquee = ref(null)
const contextMenu = ref({ show: false, x: 0, y: 0 })
const viewportRef = ref(null)
const activePage = ref(1)
let clearMarquee = () => {}
const MARQUEE_THRESHOLD_MM = 1.2
const guideGeometry = computed(() => {
  const paper = store.document.paper
  const contentWidth = geometry.value.contentWidthMm
  const contentHeight = geometry.value.heightMm - paper.marginMm.top - paper.marginMm.bottom
  return {
    left: paper.marginMm.left,
    top: paper.marginMm.top,
    width: contentWidth,
    height: contentHeight,
    centerX: paper.marginMm.left + contentWidth / 2,
    centerY: paper.marginMm.top + contentHeight / 2,
  }
})
const contextOptions = computed(() => {
  const selected = store.selectedIds.length
  const locked = store.hasLockedSelection
  const everyLocked = selected && store.selectedElements.every(element => element.locked)
  return [
    { label: '删除', key: 'delete', disabled: !selected || locked, icon: icon(TrashOutline) },
    { label: '复制', key: 'copy', disabled: !selected, icon: icon(CopyOutline) },
    { label: '粘贴', key: 'paste', disabled: !store.clipboard.length || !store.activeSurface?.elements, icon: icon(ClipboardOutline) },
    { label: '复制一份', key: 'duplicate', disabled: !selected, icon: icon(DuplicateOutline) },
    { type: 'divider', key: 'common-divider' },
    { label: everyLocked ? '解锁元素' : '锁定元素', key: everyLocked ? 'unlock' : 'lock', disabled: !selected, icon: icon(everyLocked ? LockOpenOutline : LockClosedOutline) },
    { label: '置于顶层', key: 'front', disabled: !selected || locked },
    { label: '置于底层', key: 'back', disabled: !selected || locked },
    { type: 'divider', key: 'transform-divider' },
    { label: '向左旋转 90°', key: 'rotate-left', disabled: !selected || locked, icon: icon(ReloadOutline) },
    { label: '向右旋转 90°', key: 'rotate-right', disabled: !selected || locked, icon: icon(ReloadOutline) },
    { label: '水平镜像', key: 'flip-x', disabled: !selected || locked, icon: icon(SwapHorizontalOutline) },
    { label: '垂直镜像', key: 'flip-y', disabled: !selected || locked, icon: icon(SwapVerticalOutline) },
    { type: 'divider', key: 'select-divider' },
    { label: '全选当前区块', key: 'select-all', disabled: !store.activeSurface?.elements?.length },
  ]
})

function flowText(surface) {
  return designerBindingText(surface.binding, surface.format, store.catalog, props.context)
}

function tableRows(surface) {
  return designerTablePreview(surface, store.catalog, props.context)
}

function tableCellStyle(cell, rowIndex, colIndex) {
  const style = printTableCellStyle(cell.style, {
    top: rowIndex === 0,
    left: (cell.colStart ?? colIndex) === 0,
  })
  return {
    ...style,
    width: `${cell.widthMm}mm`,
    flex: 'none',
    backgroundColor: cell.style?.backgroundColor || style.backgroundColor,
    color: cell.style?.color || '#334155',
    fontSize: cell.style?.fontSizePt ? `${cell.style.fontSizePt}pt` : '9pt',
    fontWeight: cell.style?.fontWeight || undefined,
  }
}

function showContextMenu(event) {
  contextMenu.value = { show: true, x: event.clientX, y: event.clientY }
}

function elementContext(event, surfaceId, id) {
  store.selectSurface(surfaceId)
  if (!store.selectedIds.includes(id))
    store.selectElement(id)
  showContextMenu(event)
}

function surfaceContext(event, surface) {
  store.selectSurface(surface.id)
  store.selectedIds = []
  showContextMenu(event)
}

function contextAction(key) {
  const actions = {
    'select-all': () => store.selectAll(),
    'copy': () => store.copySelection(),
    'paste': () => store.pasteSelection(),
    'duplicate': () => store.duplicateSelection(),
    'rotate-left': () => store.rotateSelection(-90),
    'rotate-right': () => store.rotateSelection(90),
    'flip-x': () => store.flipSelection('x'),
    'flip-y': () => store.flipSelection('y'),
    'front': () => store.moveSelectionLayer('front'),
    'back': () => store.moveSelectionLayer('back'),
    'lock': () => store.toggleSelectionLock(true),
    'unlock': () => store.toggleSelectionLock(false),
    'delete': () => store.removeSelection(),
  }
  actions[key]?.()
  contextMenu.value.show = false
}

function elementDown(event, surfaceId, id) {
  store.selectSurface(surfaceId)
  event.currentTarget.closest('.print-canvas').focus({ preventScroll: true })
  if (event.shiftKey || event.metaKey || event.ctrlKey) {
    store.selectElement(id, true)
    return
  }
  if (!store.selectedIds.includes(id))
    store.selectElement(id)
  // Detail / blank tables move only via the top-left handle (sv-print / Word style).
  const element = store.activeSurface?.elements?.find(item => item.id === id)
  if (element?.type === 'DATA_TABLE' || element?.type === 'STATIC_TABLE')
    return
  drag.start(event)
}

function elementMove(event, surfaceId, id) {
  store.selectSurface(surfaceId)
  store.selectElement(id)
  event.currentTarget?.closest?.('.print-canvas')?.focus?.({ preventScroll: true })
  drag.start(event)
}

function selectDataTableColumn(columnId, range, cells) {
  store.selectTableColumns(columnId ? [columnId] : [], range || null, cells || null)
}

function selectDataTableColumns(ids, range, cells) {
  store.selectTableColumns(ids, range || null, cells || null)
}

function changeDataTableField({ columnId, field }) {
  if (!columnId || !field || store.activeElement?.type !== 'DATA_TABLE')
    return
  const elementId = store.activeElement.id
  const collectionPath = store.activeElement.collectionPath
  store.execute((doc) => {
    const element = findSurface(doc, store.surfaceId)?.elements?.find(item => item.id === elementId)
    const column = element?.columns?.find(item => item.id === columnId)
    if (!column)
      return
    column.field = field
    const catalogField = store.catalog.find(item => item.path === `${collectionPath}.${field}`)
    if (catalogField?.label)
      column.title = catalogField.label
    else if (!column.title)
      column.title = field
  })
  store.selectTableColumn(columnId)
}

function onRulerPreview({ axis, positionMm }) {
  store.setGuidePreview({ axis, positionMm })
}

function onRulerPlace({ axis, positionMm }) {
  // Prefer last preview so the solid guide lands exactly where the dashed preview was.
  const locked = store.guidePreview?.axis === axis ? store.guidePreview.positionMm : positionMm
  store.addUserGuide({ axis, positionMm: locked })
}

function onRulerLeave() {
  store.clearGuidePreview()
}

function startUserGuideDrag(event, guide) {
  if (event.button !== 0)
    return
  event.preventDefault()
  event.stopPropagation()
  store.selectGuide(guide.id)
  const paper = event.currentTarget.closest('.design-paper')
  if (!paper)
    return
  const max = guide.axis === 'x' ? geometry.value.widthMm : geometry.value.heightMm
  const move = (next) => {
    const rect = paper.getBoundingClientRect()
    const span = guide.axis === 'x' ? rect.width : rect.height
    if (!span)
      return
    const offset = guide.axis === 'x' ? next.clientX - rect.left : next.clientY - rect.top
    const ratio = Math.min(1, Math.max(0, offset / span))
    store.moveUserGuide(guide.id, Number((ratio * max).toFixed(2)))
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function tableCellChange(id, value) {
  store.patchStaticTableCell(id, {
    contentType: 'TEXT',
    binding: { source: 'CONSTANT', value: value ?? '' },
  })
}

function pickImageForCells(cellIds = []) {
  const ids = cellIds.filter(Boolean)
  if (!ids.length)
    return
  pendingImageCellIds.value = ids
  cellImageDraft.value = ''
  nextTick(() => {
    showCellImageModal.value = true
  })
}

function onCellImageUpload(value) {
  const fileId = String((Array.isArray(value) ? value[0] : value) || '')
  cellImageDraft.value = fileId
  if (!fileId || !pendingImageCellIds.value.length)
    return
  store.setStaticTableCellsImage(pendingImageCellIds.value, fileId)
  showCellImageModal.value = false
  pendingImageCellIds.value = []
}

async function pasteImageFileIntoCell({ cellId, file }) {
  if (!cellId || !file)
    return
  try {
    const { uploadPrintCellImage } = await import('./uploadPrintImage')
    const fileId = await uploadPrintCellImage(file)
    store.setStaticTableCellsImage([cellId], fileId)
  }
  catch (error) {
    store.error = error?.message || '图片粘贴失败'
  }
}

function onTableResizeTrack(payload = {}) {
  const { phase, axis, index, sizeMm, originMm } = payload
  if (phase === 'start') {
    store.beginGesture()
    return
  }
  if (phase === 'end') {
    store.endGesture()
    return
  }
  if (phase === 'move' && Number.isFinite(sizeMm))
    store.resizeStaticTableTrack(axis, index, sizeMm, { live: true, originMm })
}

function onTableResizeImage(payload = {}) {
  const { phase, cellId, imageWidthMm, imageHeightMm } = payload
  if (phase === 'start') {
    store.beginGesture()
    return
  }
  if (phase === 'end') {
    store.endGesture()
    return
  }
  if (phase === 'move' && cellId)
    store.patchStaticTableCellImageSize(cellId, { imageWidthMm, imageHeightMm }, { live: true })
}

function onDataColumnResize(payload = {}) {
  const { phase, index, widthMm } = payload
  if (phase === 'start') {
    store.beginGesture()
    return
  }
  if (phase === 'end') {
    store.endGesture()
    return
  }
  if (phase === 'move' && Number.isFinite(widthMm))
    store.resizeDataTableColumn(index, widthMm, { live: true })
}

function tableContextAction(payload) {
  const { key, cellId, file, count } = payload || {}
  if (store.activeElement?.type !== 'STATIC_TABLE')
    return
  if (Number.isFinite(count) && count >= 1)
    store.setStaticTableInsertCount(count)
  if (key === 'merge') {
    store.mergeStaticTableSelection()
  }
  else if (key === 'split') {
    store.splitStaticTableSelection()
  }
  else if (key === 'row-above') {
    store.insertStaticTableRowRelative('above', count)
  }
  else if (key === 'row-below') {
    store.insertStaticTableRowRelative('below', count)
  }
  else if (key === 'row-delete') {
    store.deleteStaticTableRow()
  }
  else if (key === 'col-left') {
    store.insertStaticTableColumnRelative('left', count)
  }
  else if (key === 'col-right') {
    store.insertStaticTableColumnRelative('right', count)
  }
  else if (key === 'col-delete') {
    store.deleteStaticTableColumn()
  }
  else if (key === 'image-insert') {
    const ids = store.tableCellIds.length ? [...store.tableCellIds] : (cellId ? [cellId] : [])
    if (cellId && !ids.includes(cellId))
      store.selectTableCells([cellId])
    else if (ids.length)
      store.selectTableCells(ids)
    pickImageForCells(ids.length ? ids : (cellId ? [cellId] : []))
  }
  else if (key === 'image-clear') {
    store.clearStaticTableCellsImage(store.tableCellIds.length ? store.tableCellIds : (cellId ? [cellId] : []))
  }
  else if (key === 'image-paste-file') {
    pasteImageFileIntoCell({ cellId, file })
  }
}

function surfaceDown(event, surface) {
  if (event.button !== 0)
    return
  clearMarquee()
  store.clearMarquee()
  store.selectSurface(surface.id)
  event.currentTarget.closest('.print-canvas').focus({ preventScroll: true })
  const previous = event.shiftKey || event.metaKey || event.ctrlKey ? [...store.selectedIds] : []
  store.selectedIds = previous
  store.tableCellIds = []
  store.tableColumnId = ''
  store.tableColumnIds = []
  store.clearSelectedGuide()
  if (!surface.elements)
    return
  const rect = event.currentTarget.getBoundingClientRect()
  const x = screenDeltaToMm(event.clientX - rect.left, store.zoom)
  const y = screenDeltaToMm(event.clientY - rect.top, store.zoom)
  let dragged = false
  const move = (next) => {
    const dx = screenDeltaToMm(next.clientX - rect.left, store.zoom)
    const dy = screenDeltaToMm(next.clientY - rect.top, store.zoom)
    const box = { x: Math.min(x, dx), y: Math.min(y, dy), w: Math.abs(dx - x), h: Math.abs(dy - y) }
    if (box.w < MARQUEE_THRESHOLD_MM && box.h < MARQUEE_THRESHOLD_MM) {
      marquee.value = null
      store.clearMarquee()
      return
    }
    dragged = true
    const ids = store.selectByBox(surface.id, box, previous)
    marquee.value = { ...box, id: surface.id, count: ids.length }
  }
  const finish = () => {
    if (!dragged && !previous.length)
      store.selectedIds = []
    clearMarquee()
    store.clearMarquee()
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', finish)
  window.addEventListener('pointercancel', finish)
  window.addEventListener('blur', finish)
  clearMarquee = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', finish)
    window.removeEventListener('pointercancel', finish)
    window.removeEventListener('blur', finish)
    marquee.value = null
    clearMarquee = () => {}
  }
}

function headerGuideTopMm(page) {
  if (bandPreview.value?.band === 'header' && page.number === 1)
    return store.document.paper.marginMm.top + bandPreview.value.heightMm
  return store.document.paper.marginMm.top + store.document.header.heightMm
}

function footerGuideTopMm(page) {
  if (bandPreview.value?.band === 'footer' && page.number === designerPages.value.pages.length)
    return geometry.value.heightMm - store.document.paper.marginMm.bottom - bandPreview.value.heightMm
  return geometry.value.footerTopMm
}

function startBandResize(event, band) {
  if (event.button !== 0)
    return
  event.preventDefault()
  event.stopPropagation()
  const startY = event.clientY
  const startHeight = store.document[band].heightMm
  const other = band === 'header' ? store.document.footer.heightMm : store.document.header.heightMm
  const maxBand = Math.max(0, geometry.value.heightMm - store.document.paper.marginMm.top - store.document.paper.marginMm.bottom - other - 40)
  let nextHeight = startHeight
  bandPreview.value = { band, heightMm: startHeight }
  const move = (next) => {
    const dy = screenDeltaToMm(next.clientY - startY, store.zoom)
    nextHeight = band === 'header'
      ? Math.min(maxBand, Math.max(0, Number((startHeight + dy).toFixed(2))))
      : Math.min(maxBand, Math.max(0, Number((startHeight - dy).toFixed(2))))
    bandPreview.value = { band, heightMm: nextHeight }
  }
  const finish = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', finish)
    window.removeEventListener('pointercancel', finish)
    if (Math.abs(nextHeight - startHeight) < 0.05) {
      bandPreview.value = null
      return
    }
    if (nextHeight <= 0)
      store.collapseBand(band)
    else
      store.expandBand(band, nextHeight)
    bandPreview.value = null
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', finish)
  window.addEventListener('pointercancel', finish)
}

function startTableResize(event, surface, edge) {
  if (event.button !== 0 || surface.readOnly || surface.kind !== 'TABLE')
    return
  event.preventDefault()
  event.stopPropagation()
  store.selectSurface(surface.id)
  const startX = event.clientX
  const startY = event.clientY
  const before = cloneDocument(store.document)
  const columns = cloneDocument(surface.columns || [])
  const contentWidth = geometry.value.contentWidthMm
  const startWidth = Math.min(contentWidth, columns.reduce((sum, column) => sum + column.widthMm, 0))
  const startMinHeight = Number(surface.minHeightMm) || surface.designHeightMm || 24
  const applyDraft = (dx, dy) => {
    const draft = cloneDocument(before)
    const table = findSurface(draft, surface.id)
    if (!table || table.kind !== 'TABLE')
      return
    if (edge === 'e' || edge === 'se') {
      const target = Math.min(contentWidth, Math.max(40, startWidth + dx))
      const scale = target / Math.max(1, startWidth)
      table.columns.forEach((column, index) => {
        column.widthMm = Number((columns[index].widthMm * scale).toFixed(2))
      })
      const sum = table.columns.reduce((total, column) => total + column.widthMm, 0)
      const drift = Number((target - sum).toFixed(2))
      if (table.columns.length)
        table.columns.at(-1).widthMm = Number((table.columns.at(-1).widthMm + drift).toFixed(2))
    }
    if (edge === 's' || edge === 'se')
      table.minHeightMm = Number(Math.max(24, startMinHeight + dy).toFixed(2))
    store.document = draft
  }
  const move = (next) => {
    applyDraft(screenDeltaToMm(next.clientX - startX, store.zoom), screenDeltaToMm(next.clientY - startY, store.zoom))
  }
  const finish = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', finish)
    window.removeEventListener('pointercancel', finish)
    const after = cloneDocument(store.document)
    store.document = cloneDocument(before)
    store.execute((doc) => {
      const table = findSurface(doc, surface.id)
      const src = findSurface(after, surface.id)
      if (!table || !src || table.kind !== 'TABLE')
        return
      table.columns = cloneDocument(src.columns)
      if (src.minHeightMm !== undefined)
        table.minHeightMm = src.minHeightMm
      else
        delete table.minHeightMm
    })
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', finish)
  window.addEventListener('pointercancel', finish)
}

function drop(event, surface) {
  const item = readDragItem(event.dataTransfer)
  if (!item) {
    store.error = '请从左侧组件或字段区拖入内容'
    return
  }
  const targetId = surface?.elements ? surface.id : null
  if (targetId)
    store.selectSurface(targetId)
  else
    store.ensureFreeCanvas()
  try {
    const rect = event.currentTarget.getBoundingClientRect()
    const position = { xMm: screenDeltaToMm(event.clientX - rect.left, store.zoom), yMm: screenDeltaToMm(event.clientY - rect.top, store.zoom) }
    if (item.field)
      addField(store, item.field, position)
    else if (item.component)
      insertRegisteredPrintComponent(store, item.component, position)
    else if (item.detailTable)
      addDetailTable(store, undefined, position)
    else if (item.section)
      addSection(store, item.section)
    else if (item.type)
      addElement(store, item.type, undefined, position, item.preset, store.surfaceId)
    else
      store.error = '请从左侧组件或字段区拖入内容'
  }
  catch (error) {
    store.error = error?.message || '请从左侧组件或字段区拖入内容'
  }
}

function dropOnPaper(event) {
  if (![...event.dataTransfer.types].some(type => type === PRINT_DRAG_TYPE || type === 'text/plain'))
    return
  store.ensureFreeCanvas()
  drop(event, { id: store.surfaceId, elements: store.activeSurface?.elements })
}

async function jumpToPage(number) {
  activePage.value = number
  await nextTick()
  const node = viewportRef.value?.querySelector(`[data-page-number="${number}"]`)
  node?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' })
}

function onViewportScroll() {
  const root = viewportRef.value
  if (!root)
    return
  const pages = [...root.querySelectorAll('[data-page-number]')]
  if (!pages.length)
    return
  const mid = root.scrollTop + root.clientHeight / 2
  let current = Number(pages[0].dataset.pageNumber || 1)
  for (const page of pages) {
    const top = page.offsetTop
    const bottom = top + page.offsetHeight
    if (mid >= top && mid <= bottom)
      current = Number(page.dataset.pageNumber || current)
  }
  activePage.value = current
}
onBeforeUnmount(() => clearMarquee())
</script>

<template>
  <div class="print-canvas" tabindex="0" aria-label="打印编辑画布" @keydown="keyboard">
    <NModal
      v-model:show="showCellImageModal"
      preset="card"
      title="插入单元格图片"
      :style="{ width: '420px' }"
      :bordered="false"
      size="small"
      :z-index="4000"
      :mask-closable="true"
    >
      <p class="muted tip" style="margin: 0 0 8px; font-size: 12px;">
        上传后写入当前选中的 {{ pendingImageCellIds.length || 1 }} 个单元格（含合并格）。
      </p>
      <FileUpload
        :model-value="cellImageDraft"
        :limit="1"
        :multiple="false"
        :show-download="false"
        :file-type="['png', 'jpg', 'jpeg', 'webp']"
        business-type="print"
        upload-button-text="选择图片上传"
        @update:model-value="onCellImageUpload"
      />
    </NModal>
    <NDropdown
      trigger="manual"
      placement="bottom-start"
      :show="contextMenu.show"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :options="contextOptions"
      @select="contextAction"
      @clickoutside="contextMenu.show = false"
    />
    <div class="canvas-note">
      <span>{{ paperName }} · {{ geometry.widthMm }}×{{ geometry.heightMm }}mm · {{ designerPages.pages.length }}页</span>
      <PrintCanvasActionBar />
      <span class="canvas-note-actions">
        <button
          v-if="store.userGuides.length"
          type="button"
          class="clear-guides-btn"
          title="清除全部辅助线"
          @click="store.clearAllUserGuides()"
        >
          清除标线 ({{ store.userGuides.length }})
        </button>
        <span>拖组件到画布 · Shift多选 · Alt关吸附</span>
      </span>
    </div>
    <div ref="viewportRef" class="canvas-viewport" @scroll.passive="onViewportScroll">
      <div class="paper-holder" :style="{ zoom: store.zoom }">
        <div
          v-for="page in designerPages.pages"
          :key="page.number"
          class="ruler-frame"
          :data-page-number="page.number"
          :style="{
            gridTemplateColumns: `${PRINT_RULER_SIZE_MM}mm ${geometry.widthMm}mm`,
            gridTemplateRows: `${PRINT_RULER_SIZE_MM}mm minmax(${geometry.heightMm}mm, max-content)`,
          }"
        >
          <div class="ruler-corner" />
          <PrintRuler
            :length-mm="geometry.widthMm"
            orientation="horizontal"
            :zoom="store.zoom"
            :preview-mm="store.guidePreview?.axis === 'x' ? store.guidePreview.positionMm : null"
            @preview="onRulerPreview"
            @place="onRulerPlace"
            @leave="onRulerLeave"
          />
          <PrintRuler
            :length-mm="geometry.heightMm"
            orientation="vertical"
            :zoom="store.zoom"
            :preview-mm="store.guidePreview?.axis === 'y' ? store.guidePreview.positionMm : null"
            @preview="onRulerPreview"
            @place="onRulerPlace"
            @leave="onRulerLeave"
          />
          <div
            class="design-paper"
            :class="{ 'paper-grid': store.showGrid }"
            :style="{
              width: `${geometry.widthMm}mm`,
              height: `${geometry.heightMm}mm`,
              padding: `${store.document.paper.marginMm.top}mm ${store.document.paper.marginMm.right}mm ${store.document.paper.marginMm.bottom}mm ${store.document.paper.marginMm.left}mm`,
            }"
            @dragover.prevent
            @drop.prevent="dropOnPaper"
          >
            <img
              v-if="overlayUrl"
              class="design-overlay"
              :src="overlayUrl"
              alt=""
              :style="{
                opacity: overlayStyle.opacity ?? 1,
                transform: overlayStyle.rotationDeg ? `rotate(${overlayStyle.rotationDeg}deg)` : undefined,
              }"
            >
            <div
              v-if="watermarkText"
              class="design-watermark"
              :style="{
                color: watermarkStyle.color || '#94a3b8',
                fontSize: `${watermarkStyle.fontSizePt || 14}pt`,
                opacity: watermarkStyle.opacity ?? 0.12,
              }"
            >
              {{ watermarkText }}
            </div>
            <div
              class="margin-guide"
              :style="{
                top: `${store.document.paper.marginMm.top}mm`,
                right: `${store.document.paper.marginMm.right}mm`,
                bottom: `${store.document.paper.marginMm.bottom}mm`,
                left: `${store.document.paper.marginMm.left}mm`,
              }"
            />
            <div
              class="paper-guide header-guide"
              :style="{ top: `${headerGuideTopMm(page)}mm` }"
              @pointerdown.stop="page.number === 1 && startBandResize($event, 'header')"
            >
              <span>页眉线 · 拖动调整</span>
            </div>
            <div
              class="paper-guide footer-guide"
              :style="{ top: `${footerGuideTopMm(page)}mm` }"
              @pointerdown.stop="page.number === designerPages.pages.length && startBandResize($event, 'footer')"
            >
              <span>页脚线 · 拖动调整</span>
            </div>
            <template v-if="store.showGuideLines">
              <div class="reference-guide vertical" :style="{ left: `${guideGeometry.centerX}mm` }" />
              <div class="reference-guide horizontal" :style="{ top: `${guideGeometry.centerY}mm` }" />
              <div
                class="reference-box"
                :style="{
                  left: `${guideGeometry.left}mm`,
                  top: `${guideGeometry.top}mm`,
                  width: `${guideGeometry.width}mm`,
                  height: `${guideGeometry.height}mm`,
                }"
              />
            </template>
            <div
              v-if="store.guidePreview"
              class="user-guide preview"
              :class="store.guidePreview.axis === 'x' ? 'vertical' : 'horizontal'"
              :style="store.guidePreview.axis === 'x'
                ? { left: `${store.guidePreview.positionMm}mm` }
                : { top: `${store.guidePreview.positionMm}mm` }"
            />
            <div
              v-for="guide in store.userGuides"
              :key="guide.id"
              class="user-guide"
              :class="[guide.axis === 'x' ? 'vertical' : 'horizontal', { selected: store.selectedGuideId === guide.id }]"
              :style="guide.axis === 'x' ? { left: `${guide.positionMm}mm` } : { top: `${guide.positionMm}mm` }"
              :title="guide.axis === 'x' ? '竖向辅助线 · 拖动移动 · Delete 删除' : '横向辅助线 · 拖动移动 · Delete 删除'"
              @pointerdown.stop="startUserGuideDrag($event, guide)"
              @dblclick.stop="store.removeUserGuide(guide.id)"
            >
              <button
                type="button"
                class="user-guide-remove"
                title="删除辅助线"
                aria-label="删除辅助线"
                @pointerdown.stop
                @click.stop="store.removeUserGuide(guide.id)"
              >
                ×
              </button>
            </div>
            <span class="paper-page-label">第 {{ page.number }} / {{ designerPages.pages.length }} 页</span>
            <button
              v-if="page.number === 1 && store.document.header.heightMm <= 0"
              type="button"
              class="band-collapsed header-collapsed"
              @click="store.expandBand('header')"
            >
              页眉已折叠 · 点击展开为 12mm
            </button>
            <button
              v-if="page.number === designerPages.pages.length && store.document.footer.heightMm <= 0"
              type="button"
              class="band-collapsed footer-collapsed"
              @click="store.expandBand('footer')"
            >
              页脚已折叠 · 点击展开为 12mm
            </button>
            <button
              v-if="page.number === 1 && store.document.header.heightMm > 0 && !(store.document.header.elements || []).length"
              type="button"
              class="band-collapse-action header-collapse-action"
              @click="store.collapseBand('header')"
            >
              收起页眉
            </button>
            <button
              v-if="page.number === designerPages.pages.length && store.document.footer.heightMm > 0 && !(store.document.footer.elements || []).length"
              type="button"
              class="band-collapse-action footer-collapse-action"
              @click="store.collapseBand('footer')"
            >
              收起页脚
            </button>
            <button
              v-if="page.breakBefore"
              type="button"
              class="page-break-chip"
              :class="{ active: store.surfaceId === `section:${page.breakBefore.id}` }"
              :style="{ top: `${geometry.bodyTopMm}mm` }"
              @click="store.selectSurface(page.breakBefore.id)"
            >
              手动分页 · 从本页开始
            </button>
            <span v-else-if="page.automaticBreakBefore" class="automatic-break-chip" :style="{ top: `${geometry.bodyTopMm}mm` }">按示例数据自动续页</span>
            <div
              class="paper-content"
              :style="{
                '--band-header': `${page.headerSurface?.designHeightMm || 0}mm`,
                '--band-footer': `${page.footerSurface?.designHeightMm || 0}mm`,
              }"
            >
              <template v-for="surface in [page.headerSurface]" :key="`${page.number}:${surface.id}`">
                <section
                  v-if="surface.designHeightMm > 0"
                  :data-surface-id="surface.id"
                  class="design-surface surface-header"
                  :class="{ active: !surface.readOnly && store.surfaceId === surface.id, repeated: surface.readOnly }"
                  :style="{ height: `${Math.max(surface.designHeightMm, 0)}mm` }"
                  @pointerdown.self="!surface.readOnly && surfaceDown($event, surface)"
                  @contextmenu.self.prevent="!surface.readOnly && surfaceContext($event, surface)"
                  @dragover.prevent
                  @drop.prevent.stop="!surface.readOnly && drop($event, surface)"
                >
                  <span class="surface-label">{{ surface.label }}</span>
                  <div v-if="surface.designHeightMm > 0 && !(surface.elements || []).length && !surface.readOnly" class="band-empty">
                    <strong>页眉编辑区</strong>
                    <span>拖入组件 · 高度 {{ surface.designHeightMm }}mm</span>
                  </div>
                  <PrintCanvasElement
                    v-for="element in surface.elements || []"
                    :key="element.id"
                    :element="element"
                    :selected="!surface.readOnly && store.surfaceId === surface.id && store.selectedIds.includes(element.id)"
                    :catalog="store.catalog"
                    :context="context"
                    :resolve-file="resolveFile"
                    :table-cell-ids="store.tableCellIds"
                    :table-column-id="store.tableColumnId"
                    :table-column-ids="store.tableColumnIds"
                    :page-number="page.number"
                    :total-pages="designerPages.pages.length"
                    @pointerdown.stop="!surface.readOnly && elementDown($event, surface.id, element.id)"
                    @contextmenu.stop.prevent="!surface.readOnly && elementContext($event, surface.id, element.id)"
                    @table-cell-select="store.selectTableCell"
                    @table-cell-select-range="store.selectTableCells"
                    @table-cell-change="tableCellChange"
                    @table-context-action="tableContextAction"
                    @move="elementMove($event, surface.id, element.id)"
                    @select-column="selectDataTableColumn"
                    @select-columns="selectDataTableColumns"
                    @change-field="changeDataTableField"
                    @table-resize-track="onTableResizeTrack"
                    @table-resize-image="onTableResizeImage"
                    @data-column-resize="onDataColumnResize"
                  />
                  <PrintSelectionOverlay v-if="!surface.readOnly && store.surfaceId === surface.id" />
                  <div v-if="marquee?.id === surface.id" class="marquee" :style="{ left: `${marquee.x}mm`, top: `${marquee.y}mm`, width: `${marquee.w}mm`, height: `${marquee.h}mm` }">
                    <span v-if="marquee.count" class="marquee-count">已选 {{ marquee.count }}</span>
                  </div>
                </section>
              </template>
              <div class="body-stack">
                <section
                  v-for="surface in page.bodySurfaces"
                  :key="`${page.number}:${surface.id}`"
                  :data-surface-id="surface.id"
                  class="design-surface surface-body"
                  :class="{ active: store.surfaceId === surface.id }"
                  :style="{
                    height: `${surface.designHeightMm}mm`,
                    marginBottom: `${surface.gapAfterMm || 0}mm`,
                  }"
                  @pointerdown.self="surfaceDown($event, surface)"
                  @contextmenu.self.prevent="surfaceContext($event, surface)"
                  @dragover.prevent
                  @drop.prevent.stop="drop($event, surface)"
                >
                  <span class="surface-label">{{ surface.label }}</span>
                  <PrintCanvasElement
                    v-for="element in surface.elements || []"
                    :key="element.id"
                    :element="element"
                    :selected="store.surfaceId === surface.id && store.selectedIds.includes(element.id)"
                    :catalog="store.catalog"
                    :context="context"
                    :resolve-file="resolveFile"
                    :table-cell-ids="store.tableCellIds"
                    :table-column-id="store.tableColumnId"
                    :table-column-ids="store.tableColumnIds"
                    :page-number="page.number"
                    :total-pages="designerPages.pages.length"
                    @pointerdown.stop="elementDown($event, surface.id, element.id)"
                    @contextmenu.stop.prevent="elementContext($event, surface.id, element.id)"
                    @table-cell-select="store.selectTableCell"
                    @table-cell-select-range="store.selectTableCells"
                    @table-cell-change="tableCellChange"
                    @table-context-action="tableContextAction"
                    @move="elementMove($event, surface.id, element.id)"
                    @select-column="selectDataTableColumn"
                    @select-columns="selectDataTableColumns"
                    @change-field="changeDataTableField"
                    @table-resize-track="onTableResizeTrack"
                    @table-resize-image="onTableResizeImage"
                    @data-column-resize="onDataColumnResize"
                  />
                  <div v-if="surface.kind === 'TEXT'" class="flow-text" :style="printStyle(surface.style)" @pointerdown="surfaceDown($event, surface)">
                    {{ flowText(surface) }}
                  </div>
                  <div
                    v-if="surface.kind === 'TABLE'"
                    class="table-sketch"
                    :class="{ selected: store.surfaceId === surface.id }"
                    @pointerdown="surfaceDown($event, surface)"
                  >
                    <div class="table-sketch-frame" :style="tableFrameStyle(surface.style)">
                      <div v-for="(row, rowIndex) in tableRows(surface)" :key="row.key || rowIndex" class="table-sketch-row" :data-row-kind="row.kind">
                        <span v-for="(cell, colIndex) in row.cells" :key="cell.key" :style="tableCellStyle(cell, rowIndex, colIndex)">{{ cell.text }}</span>
                      </div>
                    </div>
                    <template v-if="store.surfaceId === surface.id">
                      <i class="table-resize e" title="拖动调整宽度" @pointerdown.stop="startTableResize($event, surface, 'e')" />
                      <i class="table-resize s" title="拖动调整高度" @pointerdown.stop="startTableResize($event, surface, 's')" />
                      <i class="table-resize se" title="拖动调整宽高" @pointerdown.stop="startTableResize($event, surface, 'se')" />
                    </template>
                  </div>
                  <template v-if="store.surfaceId === surface.id && store.gesture">
                    <div v-for="x in store.alignmentGuides.x" :key="`x-${x}`" class="alignment-guide vertical" :style="{ left: `${x}mm` }" />
                    <div v-for="y in store.alignmentGuides.y" :key="`y-${y}`" class="alignment-guide horizontal" :style="{ top: `${y}mm` }" />
                    <span
                      v-if="store.alignmentGuides.position"
                      class="position-chip"
                      :style="{ left: `${store.alignmentGuides.position.xMm}mm`, top: `${store.alignmentGuides.position.yMm}mm` }"
                    >
                      X {{ store.alignmentGuides.position.xMm.toFixed(1) }} · Y {{ store.alignmentGuides.position.yMm.toFixed(1) }} mm
                      <template v-if="store.alignmentGuides.position.widthMm !== undefined">
                        · W {{ store.alignmentGuides.position.widthMm.toFixed(1) }} · H {{ store.alignmentGuides.position.heightMm.toFixed(1) }}
                      </template>
                    </span>
                  </template>
                  <PrintSelectionOverlay v-if="store.surfaceId === surface.id" />
                  <div v-if="marquee?.id === surface.id" class="marquee" :style="{ left: `${marquee.x}mm`, top: `${marquee.y}mm`, width: `${marquee.w}mm`, height: `${marquee.h}mm` }">
                    <span v-if="marquee.count" class="marquee-count">已选 {{ marquee.count }}</span>
                  </div>
                </section>
              </div>
              <template v-for="surface in [page.footerSurface]" :key="`${page.number}:${surface.id}`">
                <section
                  v-if="surface.designHeightMm > 0"
                  :data-surface-id="surface.id"
                  class="design-surface surface-footer"
                  :class="{ active: !surface.readOnly && store.surfaceId === surface.id, repeated: surface.readOnly }"
                  :style="{ height: `${Math.max(surface.designHeightMm, 0)}mm` }"
                  @pointerdown.self="!surface.readOnly && surfaceDown($event, surface)"
                  @contextmenu.self.prevent="!surface.readOnly && surfaceContext($event, surface)"
                  @dragover.prevent
                  @drop.prevent.stop="!surface.readOnly && drop($event, surface)"
                >
                  <span class="surface-label">{{ surface.label }}</span>
                  <div v-if="surface.designHeightMm > 0 && !(surface.elements || []).length && !surface.readOnly" class="band-empty">
                    <strong>页脚编辑区</strong>
                    <span>拖入组件 · 高度 {{ surface.designHeightMm }}mm</span>
                  </div>
                  <PrintCanvasElement
                    v-for="element in surface.elements || []"
                    :key="element.id"
                    :element="element"
                    :selected="!surface.readOnly && store.surfaceId === surface.id && store.selectedIds.includes(element.id)"
                    :catalog="store.catalog"
                    :context="context"
                    :resolve-file="resolveFile"
                    :table-cell-ids="store.tableCellIds"
                    :table-column-id="store.tableColumnId"
                    :table-column-ids="store.tableColumnIds"
                    :page-number="page.number"
                    :total-pages="designerPages.pages.length"
                    @pointerdown.stop="!surface.readOnly && elementDown($event, surface.id, element.id)"
                    @contextmenu.stop.prevent="!surface.readOnly && elementContext($event, surface.id, element.id)"
                    @table-cell-select="store.selectTableCell"
                    @table-cell-select-range="store.selectTableCells"
                    @table-cell-change="tableCellChange"
                    @table-context-action="tableContextAction"
                    @move="elementMove($event, surface.id, element.id)"
                    @select-column="selectDataTableColumn"
                    @select-columns="selectDataTableColumns"
                    @change-field="changeDataTableField"
                    @table-resize-track="onTableResizeTrack"
                    @table-resize-image="onTableResizeImage"
                    @data-column-resize="onDataColumnResize"
                  />
                  <PrintSelectionOverlay v-if="!surface.readOnly && store.surfaceId === surface.id" />
                  <div v-if="marquee?.id === surface.id" class="marquee" :style="{ left: `${marquee.x}mm`, top: `${marquee.y}mm`, width: `${marquee.w}mm`, height: `${marquee.h}mm` }">
                    <span v-if="marquee.count" class="marquee-count">已选 {{ marquee.count }}</span>
                  </div>
                </section>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
    <nav v-if="designerPages.pages.length > 1" class="page-nav" aria-label="页面导航">
      <button
        v-for="page in designerPages.pages"
        :key="page.number"
        type="button"
        class="page-thumb"
        :class="{ active: activePage === page.number }"
        :title="`跳到第 ${page.number} 页`"
        :aria-label="`跳到第 ${page.number} 页`"
        @click="jumpToPage(page.number)"
      >
        <span class="thumb-preview" />
        <small>{{ page.number }}</small>
      </button>
    </nav>
    <PrintMiniMap
      :viewport-el="viewportRef"
      :pages="designerPages.pages"
      :paper-width-mm="geometry.widthMm"
      :paper-height-mm="geometry.heightMm"
    />
  </div>
</template>

<style scoped>
.print-canvas {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--gray-100, #f6f8fb);
  outline: none;
}

.canvas-note {
  display: flex;
  min-height: 44px;
  padding: 6px 10px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  color: var(--text-tertiary, #667085);
  background: var(--bg-primary, #fff);
  font-size: 12px;
  white-space: nowrap;
  overflow-x: auto;
  scrollbar-width: thin;
}
.canvas-note > span:first-child,
.canvas-note > span:last-child {
  flex: 0 0 auto;
  opacity: 0.9;
  font-size: 12px;
}
.canvas-note-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.clear-guides-btn {
  padding: 2px 8px;
  border: 1px solid var(--border-light, #d0d7e2);
  border-radius: 4px;
  color: var(--text-secondary, #475569);
  background: var(--bg-primary, #fff);
  font-size: 11px;
  cursor: pointer;
}
.clear-guides-btn:hover {
  color: var(--error-color, #d03050);
  border-color: var(--error-color, #d03050);
}
.canvas-note :deep(.canvas-actionbar-shell) {
  flex: 1 1 auto;
  justify-content: center;
  min-width: 0;
}
.canvas-viewport {
  flex: 1;
  min-height: 0;
  padding: 10px 14px 16px;
  overflow: auto;
}
.page-nav {
  display: flex;
  gap: 4px;
  padding: 4px 8px;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--border-light, #e5e7eb);
  background: var(--bg-primary, #fff);
  overflow-x: auto;
  scrollbar-width: thin;
}
.page-thumb {
  width: 34px;
  height: 42px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--border-light, #d0d7e2);
  border-radius: 4px;
  background: var(--bg-primary, #fff);
  color: var(--text-tertiary, #64748b);
  cursor: pointer;
}
.page-thumb.active,
.page-thumb:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
.thumb-preview {
  width: 18px;
  height: 24px;
  border: 1px solid currentColor;
  border-radius: 1px;
  background: linear-gradient(var(--bg-primary, #fff), var(--gray-100, #f6f8fb));
  opacity: 0.85;
}
.page-thumb small {
  font-size: 9px;
  line-height: 1;
}
.print-canvas:has(.page-nav) :deep(.print-minimap) {
  bottom: 52px;
}
.paper-holder {
  width: max-content;
  margin: 0 auto;
  transform-origin: top center;
  display: flex;
  flex-direction: column;
  gap: 18mm;
}
.ruler-frame {
  display: grid;
  width: max-content;
  align-items: stretch;
  filter: drop-shadow(0 1px 3px rgb(0 0 0 / 8%));
}
.ruler-corner {
  grid-column: 1;
  grid-row: 1;
  border-right: 1px solid #94a3b8;
  border-bottom: 1px solid #94a3b8;
  background: #eef2f7;
}
.print-ruler.horizontal {
  grid-column: 2;
  grid-row: 1;
}
.print-ruler.vertical {
  grid-column: 1;
  grid-row: 2;
}
.design-paper {
  position: relative;
  z-index: 0;
  grid-column: 2;
  grid-row: 2;
  box-sizing: border-box;
  color: var(--text-primary, #111827);
  background-color: #fff;
  /* Outline (not border) so mm guides share the same origin as the rulers. */
  outline: 1px solid var(--border-light, #b8c0cc);
  /* Clip spill so handles/bands don't create a second scrollbar; page scroll stays on .canvas-viewport. */
  overflow: hidden;
}
.design-overlay,
.design-watermark {
  position: absolute;
  pointer-events: none;
  z-index: 0;
}
.design-overlay {
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: fill;
}
.design-watermark {
  top: 40%;
  left: 20%;
  color: #94a3b8;
  opacity: 0.12;
  font-size: 28pt;
  font-weight: 700;
  transform: rotate(-24deg);
  white-space: nowrap;
}
.paper-content {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.body-stack {
  position: absolute;
  z-index: 2;
  top: var(--band-header, 0mm);
  right: 0;
  bottom: var(--band-footer, 0mm);
  left: 0;
  overflow: hidden;
}
.design-surface {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  flex: none;
  outline: 1px dashed color-mix(in srgb, var(--text-tertiary, #64748b) 45%, transparent);
  touch-action: none;
  overflow: visible;
  background: #fff;
}
.design-surface.surface-header,
.design-surface.surface-footer {
  position: absolute;
  right: 0;
  left: 0;
  z-index: 3;
  background: color-mix(in srgb, var(--gray-100, #f6f8fb) 70%, #fff);
}
.design-surface.surface-header {
  top: 0;
}
.design-surface.surface-footer {
  bottom: 0;
  margin-top: 0;
}
.design-surface.repeated {
  opacity: 0.72;
  pointer-events: none;
}
.design-surface.active {
  outline-color: var(--primary-color);
  outline-width: 1.5px;
  background: color-mix(in srgb, var(--primary-color) 5%, #fff);
}
.design-surface.surface-header.active,
.design-surface.surface-footer.active {
  outline-width: 1.5px;
}
.design-paper.paper-grid {
  background-image:
    linear-gradient(to right, rgb(148 163 184 / 14%) 1px, transparent 1px),
    linear-gradient(to bottom, rgb(148 163 184 / 14%) 1px, transparent 1px);
  background-size:
    5mm 5mm,
    5mm 5mm;
}
.margin-guide {
  position: absolute;
  z-index: 1;
  border: 1px dashed color-mix(in srgb, var(--primary-color) 40%, transparent);
  pointer-events: none;
}
.paper-guide {
  position: absolute;
  z-index: 8;
  right: 0;
  left: 0;
  height: 16px;
  margin-top: -8px;
  border-top: 2.5px dashed color-mix(in srgb, var(--error-color, #d03050) 85%, transparent);
  cursor: ns-resize;
  pointer-events: auto;
  touch-action: none;
}
.paper-guide span {
  position: absolute;
  top: -17px;
  right: 2px;
  padding: 1px 6px;
  color: var(--error-color, #d03050);
  background: color-mix(in srgb, var(--bg-primary, #fff) 92%, transparent);
  font-size: 11px;
  font-weight: 700;
  line-height: 14px;
  pointer-events: none;
}
.paper-guide:hover,
.paper-guide:active {
  height: 20px;
  margin-top: -10px;
  border-top-width: 4px;
  border-top-style: solid;
  background: color-mix(in srgb, var(--error-color, #d03050) 14%, transparent);
}
.band-empty {
  position: absolute;
  inset: 2mm;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 1px dashed color-mix(in srgb, var(--primary-color) 40%, transparent);
  border-radius: 4px;
  color: var(--text-tertiary, #64748b);
  background: color-mix(in srgb, var(--bg-primary, #fff) 55%, transparent);
  pointer-events: none;
  text-align: center;
}
.band-empty strong {
  color: var(--text-primary, #334155);
  font-size: 10px;
}
.band-empty span {
  font-size: 9px;
}
.band-collapsed {
  position: absolute;
  z-index: 9;
  left: 2mm;
  padding: 2px 8px;
  border: 1px dashed var(--text-tertiary, #64748b);
  border-radius: 999px;
  color: var(--text-secondary, #475569);
  background: color-mix(in srgb, var(--bg-primary, #fff) 94%, transparent);
  font-size: 9px;
  cursor: pointer;
}
.band-collapsed:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
}
.band-collapse-action {
  position: absolute;
  z-index: 9;
  left: 2mm;
  padding: 2px 8px;
  border: 1px solid var(--border-light, #cbd5e1);
  border-radius: 999px;
  color: var(--text-secondary, #475569);
  background: color-mix(in srgb, var(--bg-primary, #fff) 94%, transparent);
  font-size: 10px;
  cursor: pointer;
}
.band-collapse-action:hover {
  color: var(--error-color, #d03050);
  border-color: var(--error-color, #d03050);
}
.header-collapsed,
.header-collapse-action {
  top: 1.5mm;
}
.footer-collapsed,
.footer-collapse-action {
  top: auto;
  bottom: 1.5mm;
}
.reference-guide {
  position: absolute;
  z-index: 3;
  pointer-events: none;
}
.reference-guide.vertical {
  top: 0;
  bottom: 0;
  width: 0;
  border-left: 1.5px dashed color-mix(in srgb, var(--primary-color) 55%, transparent);
}
.reference-guide.horizontal {
  right: 0;
  left: 0;
  height: 0;
  border-top: 1.5px dashed color-mix(in srgb, var(--primary-color) 55%, transparent);
}
.user-guide {
  position: absolute;
  z-index: 7;
  touch-action: none;
}
.user-guide.vertical {
  top: 0;
  bottom: 0;
  width: 12px;
  transform: translateX(-50%);
  cursor: ew-resize;
  border: none;
  background: transparent;
}
.user-guide.horizontal {
  right: 0;
  left: 0;
  height: 12px;
  transform: translateY(-50%);
  cursor: ns-resize;
  border: none;
  background: transparent;
}
.user-guide.vertical::before,
.user-guide.horizontal::before {
  content: '';
  position: absolute;
  background: color-mix(in srgb, var(--primary-color, #356cde) 88%, #0f172a);
  pointer-events: none;
}
.user-guide.vertical::before {
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  transform: translateX(-50%);
}
.user-guide.horizontal::before {
  right: 0;
  left: 0;
  top: 50%;
  height: 1px;
  transform: translateY(-50%);
}
.user-guide.preview {
  pointer-events: none;
}
.user-guide.preview::before {
  opacity: 0.55;
  background: repeating-linear-gradient(
    to bottom,
    color-mix(in srgb, var(--primary-color, #356cde) 88%, #0f172a) 0 3px,
    transparent 3px 6px
  );
}
.user-guide.preview.horizontal::before {
  background: repeating-linear-gradient(
    to right,
    color-mix(in srgb, var(--primary-color, #356cde) 88%, #0f172a) 0 3px,
    transparent 3px 6px
  );
}
.user-guide.selected::before {
  background: var(--error-color, #d03050);
}
.user-guide-remove {
  position: absolute;
  z-index: 1;
  display: none;
  width: 16px;
  height: 16px;
  padding: 0;
  border: 1px solid #fff;
  border-radius: 50%;
  color: #fff;
  background: var(--error-color, #d03050);
  font-size: 12px;
  line-height: 14px;
  cursor: pointer;
}
.user-guide.vertical .user-guide-remove {
  top: 2px;
  left: 50%;
  transform: translateX(-50%);
}
.user-guide.horizontal .user-guide-remove {
  top: 50%;
  right: 2px;
  transform: translateY(-50%);
}
.user-guide:hover .user-guide-remove,
.user-guide.selected .user-guide-remove {
  display: block;
}
.reference-box {
  position: absolute;
  z-index: 3;
  box-sizing: border-box;
  border: 1px dotted color-mix(in srgb, var(--primary-color) 35%, transparent);
  pointer-events: none;
}
.surface-label {
  position: absolute;
  z-index: 3;
  top: 1px;
  right: 1mm;
  padding: 0 2px;
  color: var(--text-tertiary, #64748b);
  background: color-mix(in srgb, var(--bg-primary, #fff) 78%, transparent);
  font-size: 8px;
  pointer-events: none;
}
.paper-page-label {
  position: absolute;
  z-index: 6;
  top: 1.5mm;
  right: 2mm;
  padding: 1px 4px;
  border-radius: 3px;
  color: var(--text-secondary, #475569);
  background: color-mix(in srgb, var(--gray-100, #f1f5f9) 90%, transparent);
  font-size: 8px;
  pointer-events: none;
}
.page-break-chip,
.automatic-break-chip {
  position: absolute;
  z-index: 8;
  right: 3mm;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 8px;
  line-height: 14px;
  transform: translateY(2px);
}
.page-break-chip {
  border: 1px solid var(--text-tertiary, #64748b);
  color: var(--text-secondary, #475569);
  background: color-mix(in srgb, var(--bg-primary, #fff) 94%, transparent);
  cursor: pointer;
}
.page-break-chip.active {
  color: #fff;
  border-color: var(--primary-color);
  background: var(--primary-color);
}
.automatic-break-chip {
  color: var(--text-tertiary, #64748b);
  background: color-mix(in srgb, var(--gray-100, #f1f5f9) 92%, transparent);
  pointer-events: none;
}
.flow-text {
  padding: 3mm 1mm;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  cursor: pointer;
}
.table-sketch {
  position: relative;
  display: block;
  box-sizing: border-box;
  min-height: 100%;
  padding: 2mm;
  cursor: pointer;
  font:
    10pt Arial,
    sans-serif;
}
.table-sketch.selected {
  outline: 1.5px solid var(--primary-color, #356cde);
  outline-offset: -1px;
  background: color-mix(in srgb, var(--primary-color, #356cde) 4%, #fff);
}
.table-sketch-frame {
  position: relative;
  width: 100%;
}
.table-sketch-row {
  display: flex;
}
.table-sketch-row > * {
  box-sizing: border-box;
  flex-shrink: 0;
}
.table-sketch-row strong,
.table-sketch-row span {
  display: block;
  padding: 1mm;
  overflow-wrap: anywhere;
}
.table-sketch-row[data-row-kind='header'] span {
  font-weight: 700;
}
.table-resize {
  position: absolute;
  z-index: 6;
  box-sizing: border-box;
  background: #fff;
  border: 1.5px solid var(--primary-color);
  border-radius: 2px;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 25%, transparent);
  pointer-events: auto;
  touch-action: none;
}
.table-resize.e {
  top: 50%;
  right: -6px;
  width: 10px;
  height: 22px;
  margin-top: -11px;
  cursor: ew-resize;
}
.table-resize.s {
  bottom: -6px;
  left: 50%;
  width: 22px;
  height: 10px;
  margin-left: -11px;
  cursor: ns-resize;
}
.table-resize.se {
  right: -6px;
  bottom: -6px;
  width: 12px;
  height: 12px;
  cursor: nwse-resize;
}
.alignment-guide {
  position: absolute;
  z-index: 20;
  pointer-events: none;
}
.alignment-guide.vertical {
  top: 0;
  bottom: 0;
  border-left: 1px dashed var(--error-color, #d03050);
}
.alignment-guide.horizontal {
  right: 0;
  left: 0;
  border-top: 1px dashed var(--error-color, #d03050);
}
.position-chip {
  position: absolute;
  z-index: 21;
  padding: 1px 4px;
  color: #fff;
  background: var(--error-color, #d03050);
  border-radius: 2px;
  font:
    8px/13px Arial,
    sans-serif;
  transform: translate(2px, -15px);
  pointer-events: none;
  white-space: nowrap;
}
.marquee {
  position: absolute;
  z-index: 22;
  border: 1.5px solid var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 12%, transparent);
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 55%);
  pointer-events: none;
}
.marquee-count {
  position: absolute;
  top: -16px;
  left: 0;
  padding: 1px 5px;
  border-radius: 3px;
  color: #fff;
  background: var(--primary-color);
  font-size: 9px;
  line-height: 14px;
  white-space: nowrap;
}
</style>
