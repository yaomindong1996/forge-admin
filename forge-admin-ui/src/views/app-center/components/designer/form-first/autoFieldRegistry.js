import {
  camelToSnake,
  isFieldComponent,
  normalizeFormDesignerSchema,
} from './formDesignerSchema'

const FIELD_DEFAULTS = {
  input: { fieldType: 'TEXT', dataType: 'varchar', componentType: 'input', length: 128, precision: 2, queryType: 'like' },
  textarea: { fieldType: 'MULTILINE', dataType: 'text', componentType: 'textarea', length: null, precision: 2, queryType: 'like' },
  number: { fieldType: 'NUMBER', dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
  integer: { fieldType: 'NUMBER', dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
  money: { fieldType: 'MONEY', dataType: 'decimal', componentType: 'number', length: 18, precision: 2, queryType: 'eq' },
  date: { fieldType: 'DATE', dataType: 'date', componentType: 'date', length: null, precision: null, queryType: 'eq' },
  datetime: { fieldType: 'DATETIME', dataType: 'datetime', componentType: 'datetime', length: null, precision: null, queryType: 'eq' },
  time: { fieldType: 'TEXT', dataType: 'varchar', componentType: 'time', length: 32, precision: 2, queryType: 'eq' },
  switch: { fieldType: 'SWITCH', dataType: 'tinyint', componentType: 'switch', length: 1, precision: 0, queryType: 'eq' },
  select: { fieldType: 'DICT', dataType: 'varchar', componentType: 'select', length: 64, precision: 2, queryType: 'eq' },
  radio: { fieldType: 'RADIO', dataType: 'varchar', componentType: 'radio', length: 64, precision: 2, queryType: 'eq' },
  checkbox: { fieldType: 'CHECKBOX', dataType: 'varchar', componentType: 'checkbox', length: 255, precision: 2, queryType: 'in' },
  dictSelect: { fieldType: 'DICT', dataType: 'varchar', componentType: 'dictSelect', length: 64, precision: 2, queryType: 'eq' },
  cascader: { fieldType: 'DICT', dataType: 'varchar', componentType: 'cascader', length: 128, precision: 2, queryType: 'eq' },
  regionTreeSelect: { fieldType: 'REGION', dataType: 'varchar', componentType: 'regionTreeSelect', length: 32, precision: 2, queryType: 'eq' },
  orgTreeSelect: { fieldType: 'DEPT', dataType: 'bigint', componentType: 'orgTreeSelect', length: null, precision: null, queryType: 'eq' },
  userSelect: { fieldType: 'USER', dataType: 'bigint', componentType: 'userSelect', length: null, precision: null, queryType: 'eq' },
  fileUpload: { fieldType: 'FILE', dataType: 'varchar', componentType: 'fileUpload', length: 512, precision: 2, queryType: 'eq' },
  imageUpload: { fieldType: 'IMAGE', dataType: 'varchar', componentType: 'imageUpload', length: 512, precision: 2, queryType: 'eq' },
  objectReference: { fieldType: 'REFERENCE', dataType: 'bigint', componentType: 'objectReference', length: null, precision: null, queryType: 'eq' },
}

export function buildAutoFieldAssets(schema = {}, existingFields = []) {
  const normalized = normalizeFormDesignerSchema(schema)
  const existing = cloneFields(existingFields)
  const existingCodes = new Set(existing.map(field => field.fieldCode || field.field).filter(Boolean))
  const createdFields = []

  walkComponents(normalized.components, (component, index) => {
    if (!shouldCreateField(component, existingCodes))
      return
    const field = createFieldFromComponent(component, index)
    existing.push(field)
    existingCodes.add(field.fieldCode)
    createdFields.push(field)
  })

  return {
    fields: existing,
    createdFields,
  }
}

export function createFieldFromComponent(component = {}, index = 0) {
  const binding = component.fieldBinding || {}
  const fieldCode = binding.fieldCode || ''
  const defaults = FIELD_DEFAULTS[component.componentKey] || FIELD_DEFAULTS.input
  const props = component.props || {}
  const basicProps = {
    ...props,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName: binding.columnName || camelToSnake(fieldCode),
      createIfMissing: true,
      source: 'designer',
      locked: false,
      ...(binding || {}),
    },
  }
  if (props.placeholder)
    basicProps.placeholder = props.placeholder

  return {
    fieldName: component.label || fieldCode || '字段',
    fieldCode,
    columnName: binding.columnName || camelToSnake(fieldCode),
    fieldType: defaults.fieldType,
    dataType: defaults.dataType,
    length: defaults.length,
    precision: defaults.precision,
    required: Boolean(component.validation?.required),
    defaultValue: props.defaultValue ?? null,
    searchable: false,
    listVisible: true,
    formVisible: component.visibility?.hidden !== true,
    importable: true,
    exportable: true,
    componentType: defaults.componentType,
    queryType: defaults.queryType,
    dictType: props.dictType || '',
    sensitiveType: '',
    encryptAlgorithm: '',
    sortable: false,
    systemField: false,
    readonly: Boolean(component.visibility?.readonly),
    fieldStatus: 'ENABLED',
    referenceObjectCode: props.referenceObjectCode || '',
    referenceDisplayField: props.referenceDisplayField || '',
    placeholder: props.placeholder || '',
    remark: component.label || '',
    sortOrder: Number(component.props?.sortOrder ?? component.layout?.order ?? index + 1),
    fieldBinding: basicProps.fieldBinding,
    basicProps,
    advancedProps: {},
  }
}

function shouldCreateField(component = {}, existingCodes) {
  if (!isFieldComponent(component))
    return false
  const binding = component.fieldBinding || {}
  if (binding.mode === 'virtual' || !binding.fieldCode)
    return false
  return binding.createIfMissing !== false && !existingCodes.has(binding.fieldCode)
}

function walkComponents(components = [], visitor) {
  ;(Array.isArray(components) ? components : []).forEach((component, index) => {
    if (!component || typeof component !== 'object')
      return
    visitor(component, index)
    if (Array.isArray(component.children))
      walkComponents(component.children, visitor)
  })
}

function cloneFields(fields = []) {
  return JSON.parse(JSON.stringify(Array.isArray(fields) ? fields : []))
}
