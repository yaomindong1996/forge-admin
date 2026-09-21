import { collectExpressionFieldPaths } from './expression'

/** Return field requirements with locations; table columns use collection-relative paths. */
export function collectFieldBindings(document) {
  const fields = []
  const binding = (value, location) => {
    if (value?.source === 'FIELD') {
      fields.push({ path: value.path, location, collection: false })
    }
    if (value?.source === 'EXPRESSION' && value.expression) {
      try {
        collectExpressionFieldPaths(value.expression).forEach(path => fields.push({ path, location, collection: false }))
      }
      catch {
        // Invalid expressions are reported by document validation.
      }
    }
  }
  const elements = (items, location) => items.forEach((item, i) => {
    binding(item.binding, `${location}[${i}].binding`)
    if (item.type === 'DATA_TABLE') {
      fields.push({ path: item.collectionPath, location: `${location}[${i}]`, collection: true })
      item.columns?.forEach((column, j) => fields.push({ path: `${item.collectionPath}.${column.field}`, location: `${location}[${i}].columns[${j}]`, collection: false }))
      item.footer?.cells?.forEach((cell, j) => binding(cell.binding, `${location}[${i}].footer.cells[${j}]`))
      item.subtotal?.cells?.forEach((cell, j) => binding(cell.binding, `${location}[${i}].subtotal.cells[${j}]`))
    }
  })
  elements(document.header.elements, 'header.elements')
  elements(document.footer.elements, 'footer.elements')
  document.body.forEach((section, i) => {
    const location = `body[${i}]`
    binding(section.binding, `${location}.binding`)
    elements(section.elements || [], `${location}.elements`)
    if (section.kind === 'TABLE') {
      fields.push({ path: section.collectionPath, location, collection: true })
      section.columns.forEach((column, j) => fields.push({ path: `${section.collectionPath}.${column.field}`, location: `${location}.columns[${j}]`, collection: false }))
      section.footer?.cells.forEach((cell, j) => binding(cell.binding, `${location}.footer.cells[${j}]`))
      section.subtotal?.cells.forEach((cell, j) => binding(cell.binding, `${location}.subtotal.cells[${j}]`))
    }
  })
  if (document.watermark?.expression) {
    try {
      collectExpressionFieldPaths(document.watermark.expression).forEach(path => fields.push({ path, location: 'watermark.expression', collection: false }))
    }
    catch {
      // Invalid expressions are reported by document validation.
    }
  }
  return fields
}

export function validateFieldCatalog(document, catalog) {
  const allowed = new Map(catalog.map(field => [field.path, field]))
  return collectFieldBindings(document).flatMap((field) => {
    const entry = allowed.get(field.path)
    if (!entry || (field.collection && entry.type !== 'COLLECTION') || (!field.collection && entry.type === 'COLLECTION')) {
      return [{ path: field.location, field: field.path, code: 'FIELD_NOT_ALLOWED', message: `字段不可用：${field.path}` }]
    }
    return []
  })
}
