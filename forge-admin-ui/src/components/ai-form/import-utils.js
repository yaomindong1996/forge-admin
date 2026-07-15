function hasCellValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function isNonEmptyRow(row) {
  return Array.isArray(row) && row.some(hasCellValue)
}

function normalizeHeader(value, index) {
  const text = value === null || value === undefined ? '' : String(value).trim()
  return text || `列${index + 1}`
}

function toNonNegativeNumber(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : fallback
}

function unwrapImportResult(payload) {
  const nested = payload?.data
  if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
    const resultKeys = ['success', 'totalRows', 'successRows', 'failedRows', 'errors', 'successData', 'summary']
    if (resultKeys.some(key => Object.prototype.hasOwnProperty.call(nested, key)))
      return nested
  }
  return payload && typeof payload === 'object' ? payload : {}
}

export function buildImportPreview(rows, maxRows = 20) {
  const sourceRows = Array.isArray(rows) ? rows : []
  const headerIndex = sourceRows.findIndex(isNonEmptyRow)
  if (headerIndex < 0) {
    return { headers: [], rows: [], totalRows: 0, truncated: false }
  }

  const headerRow = sourceRows[headerIndex]
  const dataRows = sourceRows
    .map((row, index) => ({ row, index }))
    .filter(item => item.index > headerIndex && isNonEmptyRow(item.row))
  const columnCount = Math.max(
    headerRow.length,
    ...dataRows.map(item => item.row.length),
    0,
  )
  const headers = Array.from({ length: columnCount }, (_, index) => normalizeHeader(headerRow[index], index))
  const previewLimit = Math.max(1, Number(maxRows) || 20)
  const previewRows = dataRows.slice(0, previewLimit).map(({ row, index }) => {
    const result = { __rowNumber: index + 1 }
    headers.forEach((_, columnIndex) => {
      result[`column_${columnIndex}`] = row[columnIndex] ?? ''
    })
    return result
  })

  return {
    headers,
    rows: previewRows,
    totalRows: dataRows.length,
    truncated: dataRows.length > previewRows.length,
  }
}

export function normalizeImportResult(payload) {
  const source = unwrapImportResult(payload)
  const rawErrors = Array.isArray(source.errors) ? source.errors : []
  const errors = rawErrors.map((error = {}, index) => ({
    __errorKey: `import-error-${index}`,
    rowNum: error.rowNum ?? error.rowNumber ?? '-',
    field: error.field || error.fieldName || '',
    label: error.columnName || error.label || error.fieldName || error.field || '字段',
    rawValue: error.rawValue ?? error.value ?? '',
    message: error.errorMessage || error.message || '数据不正确',
    suggestion: error.suggestion || '',
  }))
  const inferredSuccessRows = Array.isArray(source.successData) ? source.successData.length : 0
  const successRows = toNonNegativeNumber(source.successRows, inferredSuccessRows)
  const failedRows = toNonNegativeNumber(source.failedRows, errors.length)
  const totalRows = toNonNegativeNumber(source.totalRows, successRows + failedRows)

  return {
    success: source.success !== false && failedRows === 0,
    totalRows,
    successRows,
    failedRows,
    summary: source.summary || (failedRows > 0
      ? `共${totalRows}行，成功${successRows}行，失败${failedRows}行`
      : `导入成功，共${successRows}行`),
    errors,
  }
}
