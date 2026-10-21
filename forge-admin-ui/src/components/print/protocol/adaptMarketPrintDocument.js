/**
 * 把市场/已安装打印模板里的字段路径，按当前来源字段目录重绑。
 * 市场包常写死 main.docNo 等示例路径，落到真实业务对象时必须对齐实际表结构。
 */

function leaf(path = '') {
  const parts = String(path).split('.').filter(Boolean)
  return parts.at(-1) || ''
}

function parentOf(path = '') {
  const parts = String(path).split('.').filter(Boolean)
  return parts.length > 1 ? parts.slice(0, -1).join('.') : ''
}

function normalizeKey(value = '') {
  return String(value)
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fff]/g, '')
}

function clone(value) {
  return structuredClone ? structuredClone(value) : JSON.parse(JSON.stringify(value))
}

/**
 * @param {Array<{ path: string, label?: string, type?: string }>} catalog
 * @param {string} wanted
 * @param {{ collection?: boolean }} [opts]
 * @returns {string|null}
 */
export function resolveCatalogPath(catalog = [], wanted, opts = {}) {
  const path = String(wanted || '').trim()
  if (!path)
    return null
  const wantCollection = Boolean(opts.collection)
  const fields = catalog.filter((field) => {
    const isCollection = field?.type === 'COLLECTION'
    return wantCollection ? isCollection : !isCollection
  })
  const exact = fields.find(field => field.path === path)
  if (exact)
    return exact.path

  // 仅有一张子表时，市场包里的任意集合路径都对齐到它
  if (wantCollection && fields.length === 1)
    return fields[0].path

  const wantedLeaf = normalizeKey(leaf(path))
  const wantedParent = parentOf(path)
  if (!wantedLeaf)
    return null

  const scored = fields
    .map((field) => {
      const fieldLeaf = normalizeKey(leaf(field.path))
      const fieldLabel = normalizeKey(field.label || '')
      let score = 0
      if (fieldLeaf === wantedLeaf)
        score += 100
      else if (fieldLabel === wantedLeaf)
        score += 90
      else
        return { path: field.path, score: 0 }
      if (wantedParent && parentOf(field.path) === wantedParent)
        score += 20
      else if (wantedParent && parentOf(field.path).split('.')[0] === wantedParent.split('.')[0])
        score += 8
      return { path: field.path, score }
    })
    .filter(item => item.score >= 90)
    .sort((a, b) => b.score - a.score || a.path.localeCompare(b.path))

  return scored[0]?.path || null
}

function adaptBinding(binding, catalog, stats) {
  if (!binding || typeof binding !== 'object')
    return binding
  if (binding.source === 'FIELD') {
    const next = resolveCatalogPath(catalog, binding.path, { collection: false })
    if (next) {
      if (next !== binding.path)
        stats.remapped += 1
      return { ...binding, path: next }
    }
    stats.cleared += 1
    // 无法对齐时改成空常量，避免草稿保存被后端字段目录拦住；设计器里可再绑
    return { source: 'CONSTANT', value: '' }
  }
  return binding
}

function adaptElements(elements = [], catalog, stats) {
  return elements.map((element) => {
    const next = { ...element, binding: adaptBinding(element.binding, catalog, stats) }
    if (Array.isArray(element.descriptions?.items)) {
      next.descriptions = {
        ...element.descriptions,
        items: element.descriptions.items.map(item => ({
          ...item,
          binding: adaptBinding(item.binding, catalog, stats),
        })),
      }
    }
    if (element.type === 'DATA_TABLE') {
      const oldCollection = element.collectionPath
      const newCollection = resolveCatalogPath(catalog, oldCollection, { collection: true })
      if (newCollection) {
        if (newCollection !== oldCollection)
          stats.remapped += 1
        next.collectionPath = newCollection
        next.columns = (element.columns || []).map((column) => {
          const relative = String(column.field || '')
          const full = relative.includes('.') ? relative : `${oldCollection}.${relative}`
          const resolved = resolveCatalogPath(catalog, full, { collection: false })
            || resolveCatalogPath(catalog, `${newCollection}.${leaf(relative)}`, { collection: false })
          if (resolved && resolved.startsWith(`${newCollection}.`)) {
            const nextRelative = resolved.slice(newCollection.length + 1)
            if (nextRelative !== relative)
              stats.remapped += 1
            return { ...column, field: nextRelative }
          }
          stats.cleared += 1
          return { ...column, field: '' }
        })
      }
      else {
        stats.cleared += 1
        next.collectionPath = ''
      }
      if (element.footer?.cells) {
        next.footer = {
          ...element.footer,
          cells: element.footer.cells.map(cell => ({
            ...cell,
            binding: adaptBinding(cell.binding, catalog, stats),
          })),
        }
      }
      if (element.subtotal?.cells) {
        next.subtotal = {
          ...element.subtotal,
          cells: element.subtotal.cells.map(cell => ({
            ...cell,
            binding: adaptBinding(cell.binding, catalog, stats),
          })),
        }
      }
    }
    return next
  })
}

function adaptSection(section, catalog, stats) {
  const next = {
    ...section,
    binding: adaptBinding(section.binding, catalog, stats),
    elements: adaptElements(section.elements || [], catalog, stats),
  }
  if (section.kind === 'TABLE') {
    const oldCollection = section.collectionPath
    const newCollection = resolveCatalogPath(catalog, oldCollection, { collection: true })
    if (newCollection) {
      if (newCollection !== oldCollection)
        stats.remapped += 1
      next.collectionPath = newCollection
      next.columns = (section.columns || []).map((column) => {
        const relative = String(column.field || '')
        const full = relative.includes('.') ? relative : `${oldCollection}.${relative}`
        const resolved = resolveCatalogPath(catalog, full, { collection: false })
          || resolveCatalogPath(catalog, `${newCollection}.${leaf(relative)}`, { collection: false })
        if (resolved && resolved.startsWith(`${newCollection}.`)) {
          const nextRelative = resolved.slice(newCollection.length + 1)
          if (nextRelative !== relative)
            stats.remapped += 1
          return { ...column, field: nextRelative }
        }
        stats.cleared += 1
        return { ...column, field: '' }
      })
    }
    else {
      stats.cleared += 1
      next.collectionPath = ''
    }
    if (section.footer?.cells) {
      next.footer = {
        ...section.footer,
        cells: section.footer.cells.map(cell => ({
          ...cell,
          binding: adaptBinding(cell.binding, catalog, stats),
        })),
      }
    }
    if (section.subtotal?.cells) {
      next.subtotal = {
        ...section.subtotal,
        cells: section.subtotal.cells.map(cell => ({
          ...cell,
          binding: adaptBinding(cell.binding, catalog, stats),
        })),
      }
    }
  }
  return next
}

/**
 * @returns {{ document: object, remapped: number, cleared: number }}
 */
export function adaptMarketPrintDocument(document, catalog = []) {
  const stats = { remapped: 0, cleared: 0 }
  const next = clone(document)
  if (!next || typeof next !== 'object')
    return { document: next, ...stats }

  if (next.header)
    next.header = { ...next.header, elements: adaptElements(next.header.elements || [], catalog, stats) }
  if (next.footer)
    next.footer = { ...next.footer, elements: adaptElements(next.footer.elements || [], catalog, stats) }
  if (Array.isArray(next.body))
    next.body = next.body.map(section => adaptSection(section, catalog, stats))

  return { document: next, remapped: stats.remapped, cleared: stats.cleared }
}
