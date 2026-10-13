import { resolveFieldComponentDefaults } from './fieldComponentCatalog'
import {
  camelToSnake,
  isFieldComponent,
  normalizeFormDesignerSchema,
} from './formDesignerSchema'
import { normalizeFieldCode } from './namingUtils'

/**
 * 表单组件绑定码与对象字段目录对齐。
 * 必须与后端 BusinessNamingService.normalizeFieldCode 等价：
 * ent_name / entName 视为同一字段，禁止再按原始字符串精确匹配造出「双胞胎」字段。
 */
export function buildAutoFieldAssets(schema = {}, existingFields = []) {
  const normalized = normalizeFormDesignerSchema(schema)
  const existing = cloneFields(existingFields)
  const lookup = buildFieldCodeLookup(existing)
  const createdFields = []

  walkBoundFieldComponents(normalized.components, (component, index) => {
    const rawCode = String(component.fieldBinding?.fieldCode || '').trim()
    let current = findFieldByCode(lookup, rawCode)
    if (!current) {
      current = createFieldFromComponent(component, index)
      existing.push(current)
      indexField(lookup, current)
      createdFields.push(current)
    }
    alignComponentFieldBinding(component, current.fieldCode || current.field, current.columnName)
    syncExistingFieldFromComponent(current, component)
  })

  return {
    fields: dedupeFieldsByCanonicalCode(existing),
    createdFields,
    formDesignerSchema: normalized,
  }
}

/**
 * 在任意表单 schema 树上就地回写绑定码（保留 multi-form / formAssets 结构）。
 * 用于设计器草稿：buildAutoFieldAssets 内部会再 clone 一份单表单视图，不能丢掉外层 forms。
 */
export function alignFormDesignerBindingsToFields(schema = {}, fields = []) {
  if (!schema || typeof schema !== 'object')
    return schema
  const lookup = buildFieldCodeLookup(fields)
  const groups = collectFormComponentGroups(schema)
  groups.forEach((components) => {
    walkBoundFieldComponents(components, (component) => {
      const rawCode = String(component.fieldBinding?.fieldCode || '').trim()
      if (!rawCode)
        return
      const matched = findFieldByCode(lookup, rawCode)
      if (matched) {
        alignComponentFieldBinding(
          component,
          matched.fieldCode || matched.field,
          matched.columnName,
        )
        return
      }
      const canonical = toCanonicalFieldCode(rawCode, component.label)
      alignComponentFieldBinding(component, canonical, camelToSnake(canonical))
    })
  })
  return schema
}

export function toCanonicalFieldCode(value = '', fallbackLabel = '') {
  return normalizeFieldCode(String(value || '').trim(), fallbackLabel)
}

export function fieldCodesEquivalent(left = '', right = '') {
  const a = String(left || '').trim()
  const b = String(right || '').trim()
  if (!a || !b)
    return false
  if (a === b)
    return true
  return toCanonicalFieldCode(a).toLowerCase() === toCanonicalFieldCode(b).toLowerCase()
}

export function buildFieldCodeLookup(fields = []) {
  const lookup = new Map()
  ;(Array.isArray(fields) ? fields : []).forEach((field) => {
    indexField(lookup, field)
  })
  return lookup
}

export function findFieldByCode(lookup, code = '') {
  const raw = String(code || '').trim()
  if (!raw || !lookup)
    return null
  return lookup.get(toCanonicalFieldCode(raw).toLowerCase()) || null
}

export function createFieldFromComponent(component = {}, index = 0) {
  const binding = component.fieldBinding || {}
  const fieldCode = toCanonicalFieldCode(binding.fieldCode || '', component.label || '')
  const defaults = resolveFieldComponentDefaults(component.componentKey)
  const props = component.props || {}
  const columnName = binding.columnName || camelToSnake(fieldCode)
  const basicProps = {
    ...props,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName,
      createIfMissing: true,
      source: 'designer',
      locked: false,
      ...(binding || {}),
      fieldCode,
      columnName,
    },
  }
  if (props.placeholder)
    basicProps.placeholder = props.placeholder

  return {
    fieldName: component.label || fieldCode || '字段',
    fieldCode,
    field: fieldCode,
    columnName,
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
    formulaConfig: props.formulaConfig ?? component.advancedProps?.formulaConfig ?? null,
    basicProps,
    advancedProps: {
      ...(component.advancedProps || {}),
    },
  }
}

function isBoundFieldComponent(component = {}) {
  if (!isFieldComponent(component))
    return false
  const binding = component.fieldBinding || {}
  return binding.mode !== 'virtual' && Boolean(binding.fieldCode)
}

function alignComponentFieldBinding(component = {}, canonicalCode = '', columnName = '') {
  const code = String(canonicalCode || '').trim()
  if (!code || !component || typeof component !== 'object')
    return
  const nextColumn = String(columnName || '').trim() || camelToSnake(code)
  if (!component.fieldBinding || typeof component.fieldBinding !== 'object')
    component.fieldBinding = { mode: 'field' }
  component.fieldBinding.fieldCode = code
  component.fieldBinding.columnName = nextColumn
  if (component.props?.fieldBinding && typeof component.props.fieldBinding === 'object') {
    component.props.fieldBinding.fieldCode = code
    component.props.fieldBinding.columnName = nextColumn
  }
}

function indexField(lookup, field = {}) {
  const raw = String(field?.fieldCode || field?.field || '').trim()
  if (!raw || !lookup)
    return
  const key = toCanonicalFieldCode(raw).toLowerCase()
  if (!lookup.has(key))
    lookup.set(key, field)
}

function dedupeFieldsByCanonicalCode(fields = []) {
  const seen = new Set()
  const result = []
  for (const field of (Array.isArray(fields) ? fields : [])) {
    const raw = String(field?.fieldCode || field?.field || '').trim()
    if (!raw)
      continue
    const canonical = toCanonicalFieldCode(raw, field.fieldName || field.label || '')
    const key = canonical.toLowerCase()
    if (seen.has(key))
      continue
    seen.add(key)
    result.push({
      ...field,
      fieldCode: canonical,
      field: canonical,
      columnName: field.columnName || camelToSnake(canonical),
    })
  }
  return result
}

function collectFormComponentGroups(schema = {}) {
  const groups = [schema.components || []]
  const assets = Array.isArray(schema?.settings?.formAssets) ? schema.settings.formAssets : []
  assets.forEach((asset) => {
    const assetSchema = asset?.schema || asset
    groups.push(assetSchema?.components || [])
  })
  ;(Array.isArray(schema?.forms) ? schema.forms : []).forEach((form) => {
    groups.push(form?.schema?.components || form?.components || [])
  })
  return groups
}

/** 把表单设计器组件上的运行态关键配置镜像到字段注册表 */
function syncExistingFieldFromComponent(field = {}, component = {}) {
  const props = component.props && typeof component.props === 'object' ? component.props : {}
  const defaults = resolveFieldComponentDefaults(component.componentKey)
  const basicProps = {
    ...(field.basicProps && typeof field.basicProps === 'object' ? field.basicProps : {}),
  }
  const syncKeys = [
    'optionSource',
    'options',
    'dictType',
    'labelValueField',
    'fieldMappings',
    'mappings',
    'cascade',
    'cascadeConfig',
    'referenceObjectCode',
    'referenceDisplayField',
    'referenceValueField',
    'recordSelector',
    'checkedValue',
    'uncheckedValue',
    'runtimeRules',
    'multiple',
    'clearable',
    'filterable',
    'placeholder',
    'defaultValue',
  ]
  for (const key of syncKeys) {
    if (props[key] !== undefined)
      basicProps[key] = props[key]
  }
  if (component.advancedProps && typeof component.advancedProps === 'object') {
    field.advancedProps = {
      ...(field.advancedProps || {}),
      ...component.advancedProps,
    }
  }
  if (defaults?.componentType)
    field.componentType = defaults.componentType
  else if (component.componentKey)
    field.componentType = component.componentKey
  if (props.dictType !== undefined)
    field.dictType = props.dictType || ''
  if (props.referenceObjectCode !== undefined)
    field.referenceObjectCode = props.referenceObjectCode || ''
  if (props.referenceDisplayField !== undefined)
    field.referenceDisplayField = props.referenceDisplayField || ''
  if (props.defaultValue !== undefined)
    field.defaultValue = props.defaultValue
  if (props.placeholder !== undefined)
    field.placeholder = props.placeholder || ''
  if (component.label)
    field.fieldName = component.label
  if (component.validation?.required !== undefined)
    field.required = Boolean(component.validation.required)
  if (component.visibility?.readonly !== undefined)
    field.readonly = Boolean(component.visibility.readonly)
  if (component.visibility?.hidden !== undefined)
    field.formVisible = component.visibility.hidden !== true
  if (props.formulaConfig !== undefined || component.advancedProps?.formulaConfig !== undefined) {
    field.formulaConfig = props.formulaConfig ?? component.advancedProps?.formulaConfig ?? null
  }
  field.basicProps = basicProps
}

function walkBoundFieldComponents(components = [], visitor) {
  ;(Array.isArray(components) ? components : []).forEach((component, index) => {
    if (!component || typeof component !== 'object')
      return
    if (isBoundFieldComponent(component))
      visitor(component, index)
    if (Array.isArray(component.children))
      walkBoundFieldComponents(component.children, visitor)
  })
}

function cloneFields(fields = []) {
  return JSON.parse(JSON.stringify(Array.isArray(fields) ? fields : []))
}
