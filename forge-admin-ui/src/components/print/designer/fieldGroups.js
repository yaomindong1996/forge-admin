/** Group print field catalog into 主表 / 子表 / 流程 for designer pickers. */

export function collectionPaths(catalog = []) {
  return catalog.filter(field => field.type === 'COLLECTION').map(field => field.path)
}

/** True when path is a scalar under a COLLECTION (detail / nested row field). */
export function isCollectionChild(path, collections = []) {
  return collections.some(prefix => path.startsWith(`${prefix}.`))
}

export function fieldGroupKey(path, collections = []) {
  if (path.startsWith('flow.') || path === 'flow')
    return 'flow'
  const parent = collections
    .filter(prefix => path === prefix || path.startsWith(`${prefix}.`))
    .sort((a, b) => b.length - a.length)[0]
  if (parent)
    return parent
  if (path.startsWith('main.') || path === 'main')
    return 'main'
  return 'other'
}

export function fieldGroupTitle(key, catalog = []) {
  if (key === 'main')
    return '主表'
  if (key === 'flow')
    return '流程'
  if (key === 'other')
    return '其它'
  const collection = catalog.find(field => field.path === key && field.type === 'COLLECTION')
  return `子表 · ${collection?.label || key.split('.').at(-1)}`
}

/**
 * Build hierarchical groups for the left field tree.
 * Each group: { key, title, kind: 'main'|'collection'|'flow'|'other', collection?, fields: [...] }
 */
export function groupPrintFields(catalog = [], query = '') {
  const q = String(query || '').trim().toLowerCase()
  const collections = collectionPaths(catalog)
  const matched = catalog.filter((field) => {
    if (!q)
      return true
    return `${field.label || ''} ${field.path}`.toLowerCase().includes(q)
  })

  const buckets = new Map()
  for (const field of matched) {
    const key = fieldGroupKey(field.path, collections)
    if (!buckets.has(key))
      buckets.set(key, [])
    buckets.get(key).push(field)
  }

  const order = ['main', ...collections.filter(path => buckets.has(path)), 'flow', 'other']
  const seen = new Set()
  const groups = []
  for (const key of order) {
    if (seen.has(key) || !buckets.has(key))
      continue
    seen.add(key)
    const fields = buckets.get(key)
    const kind = key === 'main' ? 'main' : key === 'flow' ? 'flow' : key === 'other' ? 'other' : 'collection'
    groups.push({
      key,
      title: fieldGroupTitle(key, catalog),
      kind,
      collection: kind === 'collection' ? catalog.find(field => field.path === key) : null,
      fields,
    })
  }
  for (const [key, fields] of buckets) {
    if (seen.has(key))
      continue
    groups.push({
      key,
      title: fieldGroupTitle(key, catalog),
      kind: 'other',
      collection: null,
      fields,
    })
  }
  return groups
}

/** Naive UI select options with type:group for 主表 / 子表. */
export function groupedFieldSelectOptions(catalog = [], { includeCollections = false, onlyUnder = null } = {}) {
  const collections = collectionPaths(catalog)
  let fields = catalog.filter((field) => {
    if (field.type === 'COLLECTION')
      return includeCollections
    if (onlyUnder)
      return field.path.startsWith(`${onlyUnder}.`)
    return true
  })
  if (!includeCollections && onlyUnder == null) {
    // Default binding pickers: master (+ flow) scalars only — not detail columns.
    fields = fields.filter(field => !isCollectionChild(field.path, collections))
  }

  const groups = groupPrintFields(fields)
  return groups.flatMap((group) => {
    const options = group.fields.map((field) => {
      const leaf = field.label || field.path.split('.').at(-1)
      const relative = onlyUnder && field.path.startsWith(`${onlyUnder}.`)
        ? field.path.slice(onlyUnder.length + 1)
        : null
      return {
        label: relative ? `${leaf}（${relative}）` : `${leaf}`,
        value: onlyUnder ? (relative || field.path) : field.path,
        type: field.type,
      }
    })
    if (!options.length)
      return []
    return [{ type: 'group', label: group.title, key: group.key, children: options }]
  })
}

export function describeFieldPath(catalog = [], path = '') {
  if (!path)
    return ''
  const collections = collectionPaths(catalog)
  const field = catalog.find(item => item.path === path)
  const title = fieldGroupTitle(fieldGroupKey(path, collections), catalog)
  const leaf = field?.label || path.split('.').at(-1)
  return `${title} / ${leaf}`
}
