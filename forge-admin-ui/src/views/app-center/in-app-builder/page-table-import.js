import { mapColumnDefaultToFieldDefault } from '@/views/app-center/components/designer/forge-form-designer/field-default-value'

export const PAGE_DATA_SOURCE_CREATE = 'CREATE'
export const PAGE_DATA_SOURCE_EXISTING_TABLE = 'EXISTING_TABLE'
export const PAGE_OBJECT_CREATE_BLANK = 'BLANK'
export const PAGE_OBJECT_CREATE_DB_IMPORT = 'DB_IMPORT'

const SYSTEM_COLUMNS = new Set([
  'id',
  'tenant_id',
  'create_by',
  'create_time',
  'create_dept',
  'update_by',
  'update_time',
  'del_flag',
  'deleted',
])

export function isPageExistingTableImport(selection = {}) {
  return String(selection.dataSourceMode || selection.createMode || '').toUpperCase() === PAGE_DATA_SOURCE_EXISTING_TABLE
    || String(selection.createMode || '').toUpperCase() === PAGE_OBJECT_CREATE_DB_IMPORT
}

export function resolvePageCreateMode(selection = {}) {
  return isPageExistingTableImport(selection) ? PAGE_OBJECT_CREATE_DB_IMPORT : PAGE_OBJECT_CREATE_BLANK
}

export function inferFormFieldsFromColumns(columns = []) {
  return (Array.isArray(columns) ? columns : [])
    .map((column, index) => inferFormFieldFromColumn(column, index))
    .filter(Boolean)
}

export function inferFormFieldFromColumn(column = {}, index = 0) {
  const columnName = String(column.columnName || column.column_name || '').trim()
  if (!columnName || SYSTEM_COLUMNS.has(columnName.toLowerCase()))
    return null
  const isPrimaryKey = Number(column.isPk ?? column.is_pk) === 1
  if (isPrimaryKey)
    return null

  const fieldCode = normalizeImportedFieldCode(column.javaField || column.java_field || columnName)
  if (!fieldCode)
    return null

  const dataType = mapColumnDataType(column.columnType || column.column_type, column.javaType || column.java_type)
  const label = String(column.columnComment || column.column_comment || fieldCode).trim() || fieldCode
  const componentType = inferImportedComponent(fieldCode, columnName, label, dataType, column.dictType || column.dict_type)
  const fieldType = inferImportedFieldType(dataType, componentType)
  const length = resolveImportedLength(column.columnType || column.column_type, dataType)
  const precision = resolveImportedPrecision(column.columnType || column.column_type, dataType)
  const required = Number(column.isRequired ?? column.is_required) === 1
  const defaultValue = mapColumnDefaultToFieldDefault(
    column.columnDefault ?? column.column_default,
    { dataType, componentType, fieldType },
  )

  return {
    fieldName: label,
    fieldCode,
    columnName,
    fieldType,
    dataType,
    length,
    precision,
    required,
    defaultValue,
    searchable: isImportedSearchField(fieldCode, columnName),
    listVisible: true,
    formVisible: true,
    importable: true,
    exportable: true,
    componentType,
    queryType: dataType === 'varchar' || dataType === 'text' || dataType === 'longtext' ? 'like' : 'eq',
    dictType: String(column.dictType || column.dict_type || '').trim(),
    sensitiveType: inferImportedSensitiveType(fieldCode, columnName),
    encryptAlgorithm: '',
    sortable: /time$/i.test(fieldCode) || fieldCode === 'id',
    systemField: false,
    readonly: false,
    fieldStatus: 'ENABLED',
    referenceObjectCode: '',
    referenceDisplayField: '',
    placeholder: '',
    remark: label,
    sortOrder: Number(column.sort ?? index) + 1,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName,
      createIfMissing: false,
      source: 'db_import',
      locked: false,
    },
  }
}

export function normalizeImportedFieldCode(value = '') {
  const normalized = String(value || '')
    .trim()
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
  if (!normalized)
    return ''
  if (!normalized.includes('_'))
    return normalized.charAt(0).toLowerCase() + normalized.slice(1)
  const parts = normalized.toLowerCase().split('_').filter(Boolean)
  return parts.map((part, index) => (
    index === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)
  )).join('')
}

export function mapColumnDataType(columnType = '', javaType = '') {
  const type = String(columnType || '').toLowerCase()
  if (type.startsWith('bigint')) {
    return 'bigint'
  }
  if (type.startsWith('int') || type.startsWith('smallint') || type.startsWith('mediumint')) {
    return 'int'
  }
  if (type.startsWith('tinyint')) {
    return 'tinyint'
  }
  if (type.startsWith('char')) {
    return 'char'
  }
  if (type.startsWith('varchar') || type.startsWith('nvarchar') || type.startsWith('varchar2')) {
    return 'varchar'
  }
  if (type.startsWith('decimal') || type.startsWith('numeric') || type.startsWith('number')
    || type.startsWith('double') || type.startsWith('float')) {
    return 'decimal'
  }
  if (type.startsWith('datetime') || type.startsWith('timestamp')) {
    return 'datetime'
  }
  if (type.startsWith('date')) {
    return 'date'
  }
  if (type.startsWith('time')) {
    return 'time'
  }
  if (type.includes('longtext')) {
    return 'longtext'
  }
  if (type.includes('text')) {
    return 'text'
  }
  if (javaType === 'Long') {
    return 'bigint'
  }
  if (javaType === 'Integer') {
    return 'int'
  }
  return 'varchar'
}

function inferImportedFieldType(dataType, componentType) {
  if (componentType === 'dictSelect')
    return 'DICT'
  if (componentType === 'imageUpload' || componentType === 'fileUpload')
    return 'TEXT'
  switch (dataType) {
    case 'decimal':
      return 'MONEY'
    case 'int':
    case 'bigint':
      return 'NUMBER'
    case 'tinyint':
      return 'SWITCH'
    case 'date':
      return 'DATE'
    case 'datetime':
    case 'time':
      return 'DATETIME'
    case 'text':
    case 'longtext':
      return 'MULTILINE'
    default:
      return 'TEXT'
  }
}

function inferImportedComponent(fieldCode, columnName, comment, dataType, dictType) {
  if (String(dictType || '').trim())
    return 'dictSelect'
  const text = `${fieldCode} ${columnName} ${comment}`.toLowerCase()
  if (text.includes('图片') || text.includes('头像') || text.includes('avatar') || text.includes('image'))
    return 'imageUpload'
  if (text.includes('附件') || text.includes('文件') || text.includes('file') || text.includes('attachment'))
    return 'fileUpload'
  if (dataType === 'date')
    return 'date'
  if (dataType === 'datetime')
    return 'datetime'
  if (dataType === 'int' || dataType === 'bigint' || dataType === 'decimal')
    return 'number'
  if (dataType === 'tinyint')
    return 'switch'
  if (dataType === 'text' || dataType === 'longtext' || text.includes('备注') || text.includes('描述'))
    return 'textarea'
  return 'input'
}

function isImportedSearchField(fieldCode, columnName) {
  const name = `${fieldCode} ${columnName}`.toLowerCase()
  return name.includes('name') || name.includes('code') || name.includes('status') || name.includes('type')
}

function inferImportedSensitiveType(fieldCode, columnName) {
  const text = `${fieldCode} ${columnName}`.toLowerCase()
  if (text.includes('phone') || text.includes('mobile'))
    return 'PHONE'
  if (text.includes('email'))
    return 'EMAIL'
  if (text.includes('id_card') || text.includes('idcard'))
    return 'ID_CARD'
  return 'NONE'
}

function resolveImportedLength(columnType, dataType) {
  const match = String(columnType || '').match(/\((\d+)/)
  if (match)
    return Number(match[1])
  if (dataType === 'varchar')
    return 128
  if (dataType === 'decimal')
    return 18
  return null
}

function resolveImportedPrecision(columnType, dataType) {
  if (dataType !== 'decimal')
    return null
  const match = String(columnType || '').match(/\(\d+\s*,\s*(\d+)\)/)
  return match ? Number(match[1]) : 2
}
