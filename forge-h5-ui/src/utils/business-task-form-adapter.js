/**
 * 业务待办表单上下文适配器
 * 将 BusinessTaskFormContextVO 的返回数据转换为 PageSectionRenderer / LowcodeForm 所需的格式
 */

import { parseJson, resolveChildRows, resolveChildTitle } from './lowcode-runtime.js'
import { normalizeMobileComponentType, resolveMobileComponent } from '../components/lowcode/mobile-component-registry.js'

/**
 * 将后端 context.fields 转换为 LowcodeForm 所需的 mainFields 格式
 * @param {Array} rawFields - 后端返回的 fields 数组
 * @returns {Array} mainFields - LowcodeForm 格式的字段数组
 */
export function adaptBusinessTaskFields(rawFields = [], fieldPermissions = [], options = {}) {
  const permissionMap = createPermissionMap(fieldPermissions, options.includeChildPermissions === true)
  return (Array.isArray(rawFields) ? rawFields : [])
    .map(item => {
      const field = String(item?.field || item?.fieldCode || '').trim()
      if (!field) return null
      const permission = permissionMap.get(normalizePermissionField(field))
      const readable = permission
        ? resolvePermissionFlag(permission, 'readable', 'visible', true)
        : resolvePermissionFlag(item, 'readable', 'visible', true)
      if (!readable) return null
      const dictType = String(item?.dictType || item?.props?.dictType || '').trim() || undefined
      const rawType = String(item?.type || item?.componentType || item?.componentKey || 'input')
      const type = resolveFieldType(rawType, dictType, item)
      const writable = permission
        ? resolvePermissionFlag(permission, 'writable', 'editable', false)
        : resolvePermissionFlag(item, 'writable', 'editable', false) && item?.readonly !== true
      const readonly = !writable || item?.readonly === true
      const itemPermissions = normalizeItemPermissions(permission || item)
      const props = {
        ...(item?.props || {}),
        ...(item?.optionSource !== undefined ? { optionSource: item.optionSource } : {}),
        ...(item?.recordSelector !== undefined ? { recordSelector: item.recordSelector } : {}),
        ...(item?.selectorConfig !== undefined ? { selectorConfig: item.selectorConfig } : {}),
        ...(item?.querySource !== undefined ? { querySource: item.querySource } : {}),
        dictType: dictType || undefined,
        readonly,
        disabled: !writable,
        itemPermissions,
      }
      return {
        field,
        fieldCode: field,
        label: item?.label || item?.fieldName || field,
        type,
        props,
        required: writable && (typeof permission?.required === 'boolean' ? permission.required : item?.required === true),
        readonly,
        hidden: item?.hidden === true,
        formVisible: readable,
        defaultValue: item?.defaultValue ?? item?.props?.defaultValue,
        runtimeRules: item?.runtimeRules || item?.props?.runtimeRules || [],
        options: normalizeFieldOptions(item?.options || item?.props?.options),
        ...((item?.multiple === true || item?.props?.multiple === true) ? { multiple: true } : {}),
        ...((item?.labelField || item?.props?.labelField) ? { labelField: item?.labelField || item?.props?.labelField } : {}),
        ...((item?.valueField || item?.props?.valueField) ? { valueField: item?.valueField || item?.props?.valueField } : {}),
        ...((item?.labelValueField || item?.props?.labelValueField) ? { labelValueField: item?.labelValueField || item?.props?.labelValueField } : {}),
        ...((item?.fieldMappings || item?.props?.fieldMappings) ? { fieldMappings: item?.fieldMappings || item?.props?.fieldMappings } : {}),
        itemSchema: adaptBusinessTaskFields(
          item?.itemSchema || item?.props?.itemSchema || [],
          itemPermissions,
          { includeChildPermissions: true },
        ),
        itemPermissions,
        arrayConfig: { ...(item?.arrayConfig || item?.props?.arrayConfig || {}) },
        businessType: item?.businessType || item?.props?.businessType,
        limit: item?.limit ?? item?.props?.limit,
      }
    })
    .filter(Boolean)
}

/**
 * 根据后端原始类型、dictType 推断 LowcodeField 支持的类型
 */
function resolveFieldType(rawType, dictType, item) {
  const canonicalType = normalizeMobileComponentType(rawType)
  const descriptor = resolveMobileComponent(canonicalType)
  const normalizedType = String(rawType || '').replace(/[\s_-]/g, '').toLowerCase()
  if (dictType) {
    if (String(item?.props?.displayMode || '').toLowerCase() === 'pill' || normalizedType.includes('pill')) {
      return 'pillSelect'
    }
    return 'dictSelect'
  }
  if (descriptor.kind === 'field') return canonicalType
  if (canonicalType === 'signature-pad') return canonicalType
  if (normalizedType.includes('textarea')) return 'textarea'
  if (normalizedType.includes('number') || normalizedType.includes('integer') || normalizedType.includes('money')) return 'number'
  if (normalizedType.includes('datetimerange')) return 'datetimerange'
  if (normalizedType.includes('daterange')) return 'daterange'
  if (normalizedType.includes('timerange')) return 'timerange'
  if (normalizedType === 'range' || normalizedType.includes('numberrange')) return 'numberrange'
  if (normalizedType.includes('imag') && normalizedType.includes('upload')) return 'imageUpload'
  if (normalizedType.includes('file') || normalizedType.includes('upload') || normalizedType.includes('attachment')) return 'fileUpload'
  if (normalizedType.includes('checkbox')) return 'checkbox'
  if (normalizedType.includes('radio')) return 'radio'
  if (normalizedType.includes('select') || normalizedType.includes('picker')) return 'select'
  if (normalizedType.includes('datetime')) return 'datetime'
  if (normalizedType === 'date' || normalizedType.includes('datepicker')) return 'date'
  if (normalizedType.includes('switch') || normalizedType.includes('boolean')) return 'switch'
  if (normalizedType.includes('barcode') || normalizedType.includes('scan')) return 'barcodeScanner'
  return canonicalType || normalizedType || 'unknown'
}

function createPermissionMap(permissions = [], includeChildPermissions = false) {
  return new Map(normalizePermissionList(permissions)
    .filter(item => item && (includeChildPermissions || String(item.scope || '').toLowerCase() !== 'child'))
    .map(item => [normalizePermissionField(item.field || item.fieldCode), item])
    .filter(([field]) => field))
}

function normalizePermissionList(value) {
  const parsed = parseJson(value, value)
  if (Array.isArray(parsed)) return parsed
  if (!parsed || typeof parsed !== 'object') return []
  const candidates = parsed.fields || parsed.fieldPermissions || parsed.permissions || []
  return Array.isArray(candidates) ? candidates : []
}

function resolvePermissionFlag(source, primary, legacy, fallback) {
  if (typeof source?.[primary] === 'boolean') return source[primary]
  if (typeof source?.[legacy] === 'boolean') return source[legacy]
  return fallback
}

function normalizePermissionField(field = '') {
  return String(field || '').replace(/[_-]/g, '').toLowerCase()
}

function normalizeItemPermissions(source = {}) {
  const candidates = source.itemPermissions || source.fields || source.children || source.props?.itemPermissions || []
  return normalizePermissionList(candidates)
}

function normalizeFieldOptions(raw) {
  const source = Array.isArray(raw) ? raw : Array.isArray(raw?.options) ? raw.options : []
  return source.map(item => {
    if (typeof item === 'string' || typeof item === 'number') return { label: String(item), value: item }
    return {
      label: item?.label ?? item?.name ?? item?.text ?? item?.dictLabel ?? String(item?.value ?? item?.id ?? ''),
      value: item?.value ?? item?.id ?? item?.key ?? item?.dictValue ?? '',
    }
  }).filter(option => option.value !== '' && option.value !== undefined)
}

/**
 * 构建默认 pageSections（当后端未返回分区配置时）
 * 将所有字段放入一个 card 类型的分区中
 * @param {Array} fields - mainFields 数组
 * @returns {Array} pageSections
 */
export function buildDefaultPageSections(fields = [], children = [], configuredSections = null) {
  const sections = Array.isArray(configuredSections) && configuredSections.length
    ? configuredSections.map(section => ({ ...section }))
    : []
  if (!sections.length && fields.length) {
    sections.push({
      sectionId: 'main',
      sectionType: 'card',
      title: '',
      fields: fields.map(f => f.field),
      fieldOverrides: {},
      collapsible: false,
      collapsedByDefault: false,
    })
  }
  const configuredRelations = new Set(sections.map(section => String(section?.relationKey || '')).filter(Boolean))
  ;(Array.isArray(children) ? children : []).forEach((child, index) => {
    if (!child?.relationKey) return
    if (configuredRelations.has(String(child.relationKey))) return
    sections.push({
      sectionId: `child:${child.relationKey || index}`,
      sectionType: 'child_table',
      title: resolveChildTitle(child),
      relationKey: child.relationKey,
      displayMode: 'card_list',
      collapsible: false,
      collapsedByDefault: false,
    })
  })
  return sections
}

/**
 * 尝试从 formAssets 或 formRef 中提取已有的 pageSections
 * @param {Object} context - BusinessTaskFormContextVO
 * @returns {Array|null} pageSections 或 null
 */
export function extractPageSections(context = {}) {
  // 尝试从 formAssets 中查找包含 pageSections 的 schema
  const assets = Array.isArray(context.formAssets) ? context.formAssets : []
  for (const asset of assets) {
    const schema = parseJson(asset?.schema, null)
    if (Array.isArray(schema?.pageSections) && schema.pageSections.length) {
      return schema.pageSections
    }
  }
  // 尝试从 formRef 中查找
  const formRef = context.formRef || {}
  const refSchema = parseJson(formRef.formDesignerSchema || formRef.schema, null)
  if (Array.isArray(refSchema?.pageSections) && refSchema.pageSections.length) {
    return refSchema.pageSections
  }
  return null
}

/**
 * 将后端 childrenConfig 转换为 normalizeChildrenConfig 所需的格式
 * @param {Array} rawChildren - 后端返回的 childrenConfig
 * @returns {Array} children - 标准化的子表配置
 */
export function adaptChildrenConfig(rawChildren = [], fieldPermissions = []) {
  const allPermissions = normalizePermissionList(fieldPermissions)
  return (Array.isArray(rawChildren) ? rawChildren : [])
    .map((child, index) => {
      const key = String(child?.key || child?.relationKey || child?.modelCode || `children_${index}`)
      const modelCode = String(child?.modelCode || child?.tableName || key)
      const relationKey = String(child?.relationKey || child?.key || child?.modelCode || key)
      const childPermissions = [
        ...normalizePermissionList(child?.fieldPermissions || child?.permissions || []),
        ...allPermissions.filter(permission => matchesChildPermission(permission, { key, modelCode, relationKey })),
      ]
      return {
        ...child,
        key,
        modelCode,
        relationKey,
        approvalPermissionControlled: true,
        saveMode: child?.saveMode || 'merge',
        fields: adaptBusinessTaskFields(child?.fields || [], childPermissions, { includeChildPermissions: true }),
        rowActions: Array.isArray(child?.rowActions) ? child.rowActions : [],
        toolbarActions: Array.isArray(child?.toolbarActions) ? child.toolbarActions : [],
      }
    })
}

export function hasWritableBusinessTaskForm(fields = [], children = []) {
  if ((Array.isArray(fields) ? fields : []).some(isWritableTaskField)) return true
  return (Array.isArray(children) ? children : []).some((child) => {
    const writableFields = Array.isArray(child?.fields) && child.fields.some(isWritableTaskField)
    return child?.allowCreate === true || child?.allowDelete === true || (child?.allowUpdate === true && writableFields)
  })
}

export function buildBusinessTaskFormData({ formType, fields = [], children = [], mainData = {}, childData = {} } = {}) {
  const main = pickWritableTaskFields(mainData, fields)
  if (String(formType || '').toLowerCase() !== 'business-object') return main

  const childPayload = {}
  ;(Array.isArray(children) ? children : []).forEach((child) => {
    const key = String(child?.modelCode || child?.relationKey || child?.key || '').trim()
    if (!key) return
    const writableFields = new Set((Array.isArray(child?.fields) ? child.fields : [])
      .filter(isWritableTaskField)
      .map(field => String(field.field || field.fieldCode || '').trim())
      .filter(Boolean))
    const rows = resolveChildRows(child, childData)
      .map(row => buildTaskChildRow(row, writableFields, child))
      .filter(Boolean)
    if (rows.length || child?.allowCreate === true || child?.allowUpdate === true || child?.allowDelete === true)
      childPayload[key] = rows
  })
  return {
    main,
    ...(Object.keys(childPayload).length ? { children: childPayload } : {}),
  }
}

function pickWritableTaskFields(source = {}, fields = []) {
  return (Array.isArray(fields) ? fields : []).reduce((result, field) => {
    const key = String(field?.field || field?.fieldCode || '').trim()
    if (key && isWritableTaskField(field) && Object.prototype.hasOwnProperty.call(source, key))
      result[key] = source[key]
    return result
  }, {})
}

function buildTaskChildRow(row = {}, writableFields, child = {}) {
  const id = row?.id ?? row?.ID
  const persisted = id !== undefined && id !== null && String(id).trim() !== ''
  const deleted = isDeletedTaskRow(row)
  if (deleted) {
    if (!persisted || child?.allowDelete !== true) return null
    return { id: String(id), _deleted: true }
  }
  if (persisted && child?.allowUpdate !== true) return null
  if (!persisted && child?.allowCreate !== true) return null
  const result = {}
  if (persisted) result.id = String(id)
  Object.entries(row || {}).forEach(([key, value]) => {
    if (writableFields.has(key)) result[key] = value
  })
  return result
}

function isWritableTaskField(field = {}) {
  if (field?.readonly === true || field?.disabled === true || field?.props?.readonly === true || field?.props?.disabled === true)
    return false
  if (typeof field?.writable === 'boolean') return field.writable
  return field?.readonly === false
}

function isDeletedTaskRow(row = {}) {
  const value = row?._deleted ?? row?.__deleted
  if (typeof value === 'boolean') return value
  return ['true', '1', 'yes', 'y'].includes(String(value || '').trim().toLowerCase())
}

function matchesChildPermission(permission = {}, child = {}) {
  if (String(permission.scope || '').toLowerCase() !== 'child') return false
  const permissionKey = String(permission.relationKey || permission.childKey || permission.modelCode || '').trim()
  if (!permissionKey) return true
  return [child.key, child.modelCode, child.relationKey].map(String).includes(permissionKey)
}

/**
 * 从 recordData 中提取主表数据
 * @param {Object} recordData - 后端返回的 recordData
 * @returns {Object} mainData
 */
export function extractMainData(recordData = {}) {
  if (!recordData || typeof recordData !== 'object' || Array.isArray(recordData)) return {}
  if (recordData.main && typeof recordData.main === 'object' && !Array.isArray(recordData.main)) {
    return { ...recordData.main }
  }
  const { children, ...main } = recordData
  return { ...main }
}

/**
 * 从 recordData 中提取子表数据
 * @param {Object} recordData - 后端返回的 recordData
 * @returns {Object} childData - { relationKey: [...] }
 */
export function extractChildData(recordData = {}) {
  if (!recordData || typeof recordData !== 'object' || Array.isArray(recordData)) return {}
  if (recordData.children && typeof recordData.children === 'object' && !Array.isArray(recordData.children)) {
    return { ...recordData.children }
  }
  return {}
}

/**
 * 收集所有需要加载的字典类型
 * @param {Array} fields - mainFields
 * @param {Array} children - adaptChildrenConfig 的结果
 * @returns {Set} dictTypes
 */
export function collectDictTypes(fields = [], children = []) {
  const types = new Set()
  const collect = fieldList => {
    fieldList.forEach(field => {
      const dictType = field?.dictType || field?.props?.dictType
      if (dictType && (field.type === 'dictSelect' || field.type === 'pillSelect')) {
        types.add(dictType)
      }
    })
  }
  collect(fields)
  children.forEach(child => collect(child.fields || []))
  return types
}

/**
 * 构建流程交互配置（用于 PageSectionRenderer 的 flowInteraction prop）
 * @param {Object} context - BusinessTaskFormContextVO
 * @returns {Object} flowInteraction
 */
export function buildFlowInteraction(context = {}) {
  const permissions = normalizePermissionList(context.fieldPermissions)
  return {
    approvalActions: [],
    timeline: { enabled: false, title: '审批记录' },
    nodePermissions: permissions.map(perm => ({
      nodeKey: String(perm?.nodeKey || perm?.taskDefKey || ''),
      visibleSectionIds: Array.isArray(perm?.visibleSectionIds) ? perm.visibleSectionIds : [],
      readonlySectionIds: Array.isArray(perm?.readonlySectionIds) ? perm.readonlySectionIds : [],
    })),
    callbacks: {},
  }
}

/**
 * 完整适配：将 BusinessTaskFormContextVO 转换为 PageSectionRenderer 所需的全部 props
 * @param {Object} context - BusinessTaskFormContextVO
 * @param {string} mode - 'edit' | 'detail'
 * @returns {Object} { sections, mainFields, mainData, children, childData, flowInteraction, dictTypes }
 */
export function adaptBusinessTaskFormContext(context = {}, mode = 'edit') {
  const mainFields = adaptBusinessTaskFields(context.fields || context.formRef?.fields || [], context.fieldPermissions || [])
  const children = adaptChildrenConfig(context.childrenConfig || [], context.fieldPermissions || [])
  const sections = buildDefaultPageSections(mainFields, children, extractPageSections(context))
  const mainData = extractMainData(context.recordData)
  const childData = extractChildData(context.recordData)
  const flowInteraction = buildFlowInteraction(context)
  const dictTypes = collectDictTypes(mainFields, children)
  const currentFlowNodeKey = String(context.taskDefKey || '')
  return {
    sections,
    mainFields,
    mainData,
    children,
    childData,
    flowInteraction,
    dictTypes,
    currentFlowNodeKey,
  }
}
