import { paperGeometry } from '../protocol/units'
import { clampElementToContent, findSurface, newPrintId } from './commands'
import { createDataTableElement, normalizeTableColumnWidths } from './dataTable'
import { createDescriptionsConfig, descriptionBlockHeightMm } from './descriptions'
import { defaultFieldFormat } from './designerSample'
import { createStaticTable, staticTableSize } from './staticTable'

// These options describe the fixed print protocol, not configurable business enums.
export const elementCatalog = [
  { key: 'title', type: 'TEXT', preset: 'TITLE', label: '标题' },
  { key: 'text', type: 'TEXT', preset: 'TEXT', label: '文本' },
  { key: 'field-text', type: 'TEXT', preset: 'FIELD', label: '数据文本' },
  { key: 'long-text', type: 'TEXT', preset: 'LONG', label: '多行文本' },
  { key: 'image', type: 'IMAGE', label: '图片' },
  { key: 'html', type: 'HTML', label: 'HTML' },
  { key: 'line-horizontal', type: 'LINE', preset: 'HORIZONTAL', label: '横线' },
  { key: 'line-vertical', type: 'LINE', preset: 'VERTICAL', label: '竖线' },
  { key: 'rectangle', type: 'RECTANGLE', label: '矩形' },
  { key: 'ellipse', type: 'ELLIPSE', label: '椭圆' },
  { key: 'circle', type: 'ELLIPSE', preset: 'CIRCLE', label: '圆形' },
  { key: 'barcode', type: 'BARCODE', label: '条形码' },
  { key: 'qrcode', type: 'QRCODE', label: '二维码' },
  { key: 'page-number', type: 'PAGE_NUMBER', label: '页码' },
  { key: 'static-table', type: 'STATIC_TABLE', label: '空白表格' },
  { key: 'descriptions', type: 'DESCRIPTIONS', label: '详情' },
]
export const PRINT_DRAG_TYPE = 'application/x-forge-print-item'

function resolveTargetSurface(doc, store, preferredId) {
  const surface = findSurface(doc, preferredId)
  if (surface?.elements)
    return { surface, targetId: preferredId.startsWith('section:') || preferredId === 'header' || preferredId === 'footer' ? preferredId : `section:${preferredId}` }
  const fixed = doc.body.find(section => section.kind === 'FIXED')
  if (fixed)
    return { surface: fixed, targetId: `section:${fixed.id}` }
  const created = { id: newPrintId(), kind: 'FIXED', heightMm: Math.max(120, paperGeometry(doc).contentHeightMm || 200), elements: [], gapAfterMm: 2 }
  doc.body.unshift(created)
  return { surface: created, targetId: `section:${created.id}` }
}

export function addElement(store, type, binding, position, preset, preferredSurfaceId = store.surfaceId, options = {}) {
  if (!elementCatalog.some(item => item.type === type))
    return false
  const id = newPrintId()
  let targetId = preferredSurfaceId
  const ok = store.execute((doc) => {
    const resolved = resolveTargetSurface(doc, store, preferredSurfaceId)
    const surface = resolved.surface
    targetId = resolved.targetId
    const tableCols = Math.max(1, Math.min(20, Number(options.cols) || 3))
    const tableRows = Math.max(1, Math.min(20, Number(options.rows) || 3))
    const presetSize = preset === 'TITLE'
      ? { widthMm: 90, heightMm: 14 }
      : preset === 'LONG'
        ? { widthMm: 90, heightMm: 28 }
        : preset === 'VERTICAL'
          ? { widthMm: 0.5, heightMm: 40 }
          : preset === 'CIRCLE'
            ? { widthMm: 24, heightMm: 24 }
            : type === 'QRCODE'
              ? { widthMm: 18, heightMm: 18 }
              : type === 'STATIC_TABLE'
                ? { widthMm: Math.min(paperGeometry(doc).contentWidthMm, Math.max(40, tableCols * 22)), heightMm: Math.max(18, tableRows * 9) }
                : type === 'ELLIPSE'
                  ? { widthMm: 32, heightMm: 20 }
                  : type === 'HTML'
                    ? { widthMm: 60, heightMm: 24 }
                    : type === 'LINE'
                      ? { widthMm: 50, heightMm: 0.5 }
                      : type === 'TEXT' || type === 'PAGE_NUMBER'
                        ? { widthMm: 45, heightMm: 10 }
                        : { widthMm: 45, heightMm: 18 }
    const widthMm = Math.min(presetSize.widthMm, paperGeometry(doc).contentWidthMm)
    const heightMm = presetSize.heightMm
    const maxX = Math.max(0, paperGeometry(doc).contentWidthMm - widthMm)
    const e = {
      id,
      type,
      xMm: Math.max(0, Math.min(position?.xMm ?? 3, maxX)),
      yMm: Math.max(0, position?.yMm ?? 3),
      widthMm,
      heightMm,
    }
    if (type === 'STATIC_TABLE') {
      e.table = createStaticTable(tableCols, tableRows, widthMm, heightMm / tableRows)
      Object.assign(e, staticTableSize(e.table))
    }
    if (['TEXT', 'IMAGE', 'BARCODE', 'QRCODE', 'HTML'].includes(type)) {
      if (preset === 'FIELD') {
        const field = store.catalog.find(item => item.type !== 'COLLECTION')
        e.binding = field ? { source: 'FIELD', path: field.path } : { source: 'CONSTANT', value: '数据字段' }
      }
      else {
        e.binding = binding || {
          source: 'CONSTANT',
          value: type === 'TEXT'
            ? (preset === 'TITLE' ? '标题文本' : preset === 'LONG' ? '多行文本内容' : '固定文本')
            : type === 'HTML'
              ? '<div style="padding:4px;border:1px dashed #94a3b8;">HTML 内容</div>'
              : type === 'IMAGE' ? '' : '123456',
        }
      }
      const field = e.binding.source === 'FIELD' ? store.catalog.find(item => item.path === e.binding.path) : null
      if (type !== 'HTML')
        e.format = defaultFieldFormat(field)
      if (type === 'BARCODE')
        e.showCodeText = true
      if (type === 'QRCODE')
        e.showCodeText = false
    }
    if (preset === 'TITLE')
      e.style = { fontSizePt: 18, fontWeight: 700, textAlign: 'center', lineHeight: 1.2 }
    if (preset === 'LONG')
      e.style = { fontSizePt: 10, lineHeight: 1.5 }
    if (['LINE', 'RECTANGLE', 'ELLIPSE'].includes(type)) {
      e.style = type === 'LINE'
        ? { borderWidthMm: 0.5, borderColor: '#000000', backgroundColor: '#000000', borderStyle: 'solid' }
        : { borderWidthMm: 0.5, borderColor: '#000000', borderStyle: 'solid' }
    }
    if (type === 'DESCRIPTIONS') {
      e.descriptions = createDescriptionsConfig(store.catalog)
      e.widthMm = paperGeometry(doc).contentWidthMm
      e.xMm = 0
      e.heightMm = descriptionBlockHeightMm(e.descriptions)
      if (Number.isFinite(surface.heightMm))
        surface.heightMm = Math.max(surface.heightMm, Number((e.yMm + e.heightMm).toFixed(3)))
    }
    clampElementToContent(doc, e)
    surface.elements.push(e)
    if (Number.isFinite(surface.heightMm))
      surface.heightMm = Math.max(surface.heightMm, Number((e.yMm + e.heightMm).toFixed(3)))
  })
  if (ok) {
    store.selectSurface(targetId)
    store.selectedIds = [id]
    if (type === 'STATIC_TABLE')
      store.tableCellIds = []
  }
  return ok
}

/** Insert a blank table with Word-like chosen rows × cols. */
export function insertStaticTable(store, rows, cols, position) {
  return addElement(store, 'STATIC_TABLE', null, position, null, store.surfaceId, { rows, cols })
}

export function addSection(store, kind, field) {
  const id = newPrintId()
  const ok = store.execute((doc) => {
    if (kind === 'PAGE_BREAK') {
      const activeId = store.surfaceId.startsWith('section:') ? store.surfaceId.slice('section:'.length) : ''
      const activeIndex = doc.body.findIndex(section => section.id === activeId)
      const candidates = doc.body
        .slice(0, -1)
        .map((section, index) => ({ section, index }))
        .filter(({ section, index }) => section.kind !== 'PAGE_BREAK' && doc.body[index + 1]?.kind !== 'PAGE_BREAK')
      const target = candidates.find(({ index }) => index === activeIndex) || candidates.at(-1)
      if (!target)
        throw new Error('至少需要两个相邻内容区块才能插入分页符')
      doc.body.splice(target.index + 1, 0, { id, kind })
      return
    }
    const section = { id, kind, gapAfterMm: 2 }
    if (kind === 'FIXED')
      Object.assign(section, { heightMm: Math.max(80, paperGeometry(doc).contentHeightMm || 120), elements: [] })
    if (kind === 'TEXT')
      section.binding = { source: 'CONSTANT', value: '流式文本，内容变长时自动换行和续页。' }
    if (kind === 'TABLE') {
      const collection = field || store.catalog.find(f => f.type === 'COLLECTION')
      const fields = store.catalog.filter(f => f.type !== 'COLLECTION' && f.path.startsWith(`${collection?.path}.`)).slice(0, 6)
      if (!collection || !fields.length)
        throw new Error('请先提供包含明细列的字段目录')
      Object.assign(section, { collectionPath: collection.path, repeatHeader: true, emptyText: '暂无明细', columns: fields.map(f => ({ id: newPrintId(), field: f.path.slice(collection.path.length + 1), title: f.label || f.path.split('.').at(-1), widthMm: 1, format: defaultFieldFormat(f) })) })
      normalizeTableColumnWidths(section.columns, paperGeometry(doc).contentWidthMm)
      // Shrink oversized empty free-canvas so the detail table can stay on page 1.
      const geometry = paperGeometry(doc)
      const tableReserveMm = 48
      doc.body.forEach((item) => {
        if (item.kind !== 'FIXED' || !Array.isArray(item.elements))
          return
        const contentBottom = item.elements.reduce((max, element) => Math.max(max, element.yMm + element.heightMm), 0)
        const fitted = Math.max(contentBottom + 2, 24)
        const room = Math.max(fitted, geometry.contentHeightMm - tableReserveMm)
        if (item.heightMm > room)
          item.heightMm = Number(Math.min(item.heightMm, room).toFixed(3))
      })
    }
    doc.body.push(section)
  })
  if (ok)
    store.selectSurface(id)
  return ok
}

export function addDetailTable(store, field, position) {
  store.ensureFreeCanvas()
  const collection = field || store.catalog.find(item => item.type === 'COLLECTION')
  const fields = store.catalog.filter(item => item.type !== 'COLLECTION' && item.path.startsWith(`${collection?.path}.`)).slice(0, 6)
  if (!collection || !fields.length) {
    store.error = '请先提供包含明细列的字段目录'
    return false
  }
  const id = newPrintId()
  let targetId = store.surfaceId
  const ok = store.execute((doc) => {
    const resolved = resolveTargetSurface(doc, store, store.surfaceId)
    const surface = resolved.surface
    targetId = resolved.targetId
    if (!surface?.elements)
      throw new Error('请先选中自由画布再添加明细表格')
    const element = createDataTableElement(doc, collection, fields, position)
    element.id = id
    surface.elements.push(element)
    if (Number.isFinite(surface.heightMm))
      surface.heightMm = Math.max(surface.heightMm, Number((element.yMm + element.heightMm).toFixed(3)))
  })
  if (ok) {
    store.selectSurface(targetId)
    store.selectedIds = [id]
  }
  return ok
}

export function addField(store, path, position) {
  const field = store.catalog.find(f => f.path === path)
  if (!field)
    return false
  if (field.type === 'COLLECTION')
    return addDetailTable(store, field, position)
  const collection = store.catalog.filter(f => f.type === 'COLLECTION' && path.startsWith(`${f.path}.`)).sort((a, b) => b.path.length - a.path.length)[0]
  if (collection) {
    const element = store.activeElement?.type === 'DATA_TABLE' && store.activeElement.collectionPath === collection.path
      ? store.activeElement
      : null
    const surface = store.activeSurface
    if (element) {
      return store.execute((doc) => {
        const table = findSurface(doc, store.surfaceId).elements.find(item => item.id === element.id)
        const relativePath = path.slice(collection.path.length + 1)
        if (table.columns.some(column => column.field === relativePath))
          return
        if (table.headerRows || table.footer)
          throw new Error('含合并表头或表尾时，请先在表格属性中清除合并配置后再添加列')
        table.columns.push({ id: newPrintId(), field: relativePath, title: field.label || relativePath, widthMm: 20, format: defaultFieldFormat(field) })
        normalizeTableColumnWidths(table.columns, table.widthMm)
      })
    }
    if (surface?.kind !== 'TABLE' || surface.collectionPath !== collection.path) {
      store.error = '请先选中此明细对应的表格，再添加明细列'
      return false
    }
    return store.execute((doc) => {
      const table = findSurface(doc, store.surfaceId)
      const relativePath = path.slice(collection.path.length + 1)
      if (table.columns.some(column => column.field === relativePath))
        return
      if (table.headerRows || table.footer)
        throw new Error('含合并表头或表尾时，请先在表格属性中清除合并配置后再添加列')
      table.columns.push({ id: newPrintId(), field: relativePath, title: field.label || relativePath, widthMm: 20, format: defaultFieldFormat(field) })
      normalizeTableColumnWidths(table.columns, paperGeometry(doc).contentWidthMm)
    })
  }
  return addElement(store, field.type === 'IMAGE' ? 'IMAGE' : 'TEXT', { source: 'FIELD', path }, position, 'FIELD')
}

export function startItemDrag(event, item) {
  const payload = JSON.stringify(item)
  event.dataTransfer.setData(PRINT_DRAG_TYPE, payload)
  event.dataTransfer.setData('text/plain', payload)
  event.dataTransfer.effectAllowed = 'copy'
}

export function readDragItem(dataTransfer) {
  const raw = dataTransfer.getData(PRINT_DRAG_TYPE) || dataTransfer.getData('text/plain')
  if (!raw)
    return null
  try {
    return JSON.parse(raw)
  }
  catch {
    return null
  }
}
