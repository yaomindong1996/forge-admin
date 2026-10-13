import { isChildListField } from '@/components/lowcode-builder/page/page-schema-fields'
import {
  alignFormDesignerBindingsToFields,
  buildAutoFieldAssets,
  toCanonicalFieldCode,
} from '@/views/app-center/components/designer/form-first/autoFieldRegistry'
import { normalizeFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'

export function buildBusinessObjectDesignerPayloadFromFormAsset(asset = {}, existingFields = []) {
  // 子表列（model__field / fieldScope=child）只服务列表选列，绝不能写进主对象字段目录；
  // 否则删了再拖子表会把同一列以「扁平编码」反复入库，保存时报字段编码重复。
  const sourceSchema = asset.formDesignerSchema || asset.schema || {}
  const previewSchema = normalizeFormDesignerSchema(sourceSchema)
  const subTableChildCodes = collectSubTableQualifiedFieldCodes(previewSchema)
  const primaryExisting = filterPrimaryObjectDesignerFields(existingFields, subTableChildCodes)
  const { fields: autoFields, formDesignerSchema } = buildAutoFieldAssets(sourceSchema, primaryExisting)
  // 回写绑定码到返回的 schema，避免 snake/camel 混用再次提交
  alignFormDesignerBindingsToFields(formDesignerSchema, autoFields)
  const fields = autoFields
    .filter(field => !isChildScopedFormFieldAsset(field))
    .map(toBusinessFieldPayload)
  return {
    fields,
    formDesignerSchema,
  }
}

/**
 * 表单设计器字段资产 / 保存载荷用的主对象字段过滤。
 * 同时丢掉历史上被错误提升进来的扁平编码（与当前子表列 normalize 后同码）。
 * @param {unknown[]} fields
 * @param {string[]} [extraChildCodes] 额外的子表合成编码（如当前画布 subTable 列），即使 fields 里已被剔掉也要参与扁平码比对
 */
export function filterPrimaryObjectDesignerFields(fields = [], extraChildCodes = []) {
  const list = Array.isArray(fields) ? fields : []
  const promotedFlatCodes = collectPromotedChildFlatCodes(list)
  for (const raw of (Array.isArray(extraChildCodes) ? extraChildCodes : [])) {
    const code = String(raw || '').trim()
    if (!code)
      continue
    promotedFlatCodes.add(code.toLowerCase())
    promotedFlatCodes.add(flattenFieldCodeLikeBackend(code).toLowerCase())
  }
  return list.filter((field) => {
    if (isChildScopedFormFieldAsset(field))
      return false
    const code = String(field?.fieldCode || field?.field || '').trim()
    if (!code)
      return false
    // 大小写不敏感：__ 扁平化会把 IndicatorId 收成 Indicatorid，脏数据里也可能保留原驼峰
    return !promotedFlatCodes.has(code.toLowerCase())
  })
}

/** 子表明细列或带 model__field 的合成字段，禁止当作主表字段资产 */
export function isChildScopedFormFieldAsset(field = {}) {
  if (!field || typeof field !== 'object')
    return false
  if (field.fieldScope === 'child' || field.scope === 'child')
    return true
  if (isChildListField(field))
    return true
  const code = String(field.field || field.fieldCode || '').trim()
  return code.includes('__')
}

/**
 * 收集子表合成字段编码，以及其按后端 normalizeFieldCode 近似扁平化后的编码，
 * 用于剔除「oa_kpi_xxx__indicatorId → oaKpiXxxIndicatorid」这类脏主表字段。
 */
function collectPromotedChildFlatCodes(fields = []) {
  const codes = new Set()
  for (const field of fields) {
    if (!isChildScopedFormFieldAsset(field))
      continue
    const raw = String(field.fieldCode || field.field || '').trim()
    if (!raw)
      continue
    codes.add(raw.toLowerCase())
    codes.add(flattenFieldCodeLikeBackend(raw).toLowerCase())
  }
  return codes
}

/** 对齐 BusinessNamingService.normalizeFieldCode（含 camelCase 拆词） */
function flattenFieldCodeLikeBackend(value = '') {
  return toCanonicalFieldCode(value)
}

/** 从画布 subTable 列收集 model__field 合成编码 */
function collectSubTableQualifiedFieldCodes(schema = {}) {
  const codes = []
  const walk = (components = []) => {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const key = String(component.componentKey || component.type || '').trim()
      if (key === 'subTable' || key === 'forgeSubTable' || key === 'childTable') {
        const props = component.props && typeof component.props === 'object' ? component.props : {}
        const relationKey = String(props.relationKey || props.modelCode || component.fieldBinding?.fieldCode || '').trim()
        const columns = Array.isArray(props.columns) ? props.columns : []
        columns.forEach((column) => {
          const childCode = String(column?.fieldCode || column?.field || column?.key || column?.value || '').trim()
          if (relationKey && childCode)
            codes.push(`${relationKey}__${childCode}`)
        })
      }
      walk(component.children)
    })
  }
  walk(schema.components)
  return codes
}

export function syncFormBoundFieldRefs({ formFieldCodes = [], searchFieldRefs = [] } = {}) {
  const fieldRefs = uniqueFieldCodes(formFieldCodes)
  const keptSearch = uniqueFieldCodes(searchFieldRefs).filter(ref => fieldRefs.includes(ref))
  return {
    fieldRefs,
    searchFieldRefs: (keptSearch.length ? keptSearch : fieldRefs).slice(0, 8),
  }
}

function uniqueFieldCodes(values = []) {
  return [...new Set((Array.isArray(values) ? values : [])
    .map(value => String(value || '').trim())
    .filter(Boolean))]
}

export function normalizeObjectDesignerFieldCatalog(fields = []) {
  return (Array.isArray(fields) ? fields : [])
    .map((field, index) => {
      const fieldCode = field?.field || field?.fieldCode || field?.fieldBinding?.fieldCode || ''
      if (!fieldCode)
        return null
      const fieldName = field.fieldName || field.label || field.comment || fieldCode || `字段 ${index + 1}`
      return {
        ...field,
        field: fieldCode,
        fieldCode,
        sourceField: field.sourceField || fieldCode,
        fieldName,
        label: fieldName,
        listVisible: field.listVisible !== false,
        formVisible: field.formVisible !== false,
        fieldStatus: field.fieldStatus || 'ENABLED',
        systemField: Boolean(field.systemField),
      }
    })
    .filter(Boolean)
}

function toBusinessFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName || field.label || field.fieldCode || field.field,
    fieldCode: field.fieldCode || field.field,
    columnName: field.columnName,
    fieldType: field.fieldType || field.businessFieldType || 'TEXT',
    dataType: field.dataType,
    length: field.length,
    precision: field.precision,
    required: Boolean(field.required),
    defaultValue: field.defaultValue,
    searchable: Boolean(field.searchable),
    listVisible: field.listVisible !== false,
    formVisible: field.formVisible !== false,
    importable: field.importable !== false,
    exportable: field.exportable !== false,
    componentType: field.componentType,
    queryType: field.queryType,
    dictType: field.dictType,
    sensitiveType: field.sensitiveType,
    encryptAlgorithm: field.encryptAlgorithm,
    sortable: Boolean(field.sortable),
    systemField: false,
    readonly: Boolean(field.readonly),
    fieldStatus: field.fieldStatus || 'ENABLED',
    referenceObjectCode: field.referenceObjectCode,
    referenceDisplayField: field.referenceDisplayField,
    placeholder: field.placeholder || field.basicProps?.placeholder || '',
    remark: field.remark || field.fieldName || field.label || '',
    sortOrder: field.sortOrder,
    formulaConfig: field.formulaConfig ?? null,
    fieldBinding: { ...(field.fieldBinding || field.basicProps?.fieldBinding || {}) },
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}
