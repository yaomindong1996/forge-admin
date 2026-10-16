import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { FORM_FIELD_COMPONENT_KEYS as FIELD_COMPONENT_KEYS } from './fieldComponentCatalog'
import {
  camelToSnake,
  generateFieldCode,
} from './namingUtils'

export const FORM_DESIGNER_SCHEMA_VERSION = 'form-first-v1'
export const FORM_DESIGNER_MULTI_SCHEMA_VERSION = 'form-first-v2'
export const FORM_DESIGNER_SCHEMA_KEY = 'formDesignerSchema'
export const MAX_FORM_GRID_COLUMNS = 24

export {
  camelToSnake,
  generateFieldCode,
}

export const LAYOUT_COMPONENT_KEYS = new Set([
  'fcRow',
  'row',
  'col',
  'elCard',
  'card',
  'elTabs',
  'tabs',
  'elTabPane',
  'tabPane',
  'elCollapse',
  'collapse',
  'elCollapseItem',
  'collapseItem',
  'fcTable',
  'table',
  'fcTableGrid',
  'tableGrid',
  'AiCrudPage',
  'crudBlock',
])

export const VIRTUAL_COMPONENT_KEYS = new Set([
  ...LAYOUT_COMPONENT_KEYS,
  'elDivider',
  'divider',
  'AiFormSectionTitle',
  'fcTitle',
  'title',
  'text',
  'html',
  'space',
  'elAlert',
  'alert',
  'elButton',
  'button',
  'elTag',
  'tag',
  'elImage',
  'image',
  'rich-text',
  'watermark',
  'vue-component',
  'html-tag',
  'markdown',
  'barcode',
  'barcodeScanner',
  'qrcode',
  'calendar',
  'code',
  'countdown',
  'descriptions',
  'announcement',
  'list',
  'log',
  'number-animation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
])

const GENERIC_COMPONENT_ID_SUFFIXES = new Set([
  'input',
  'textarea',
  'number',
  'inputnumber',
  'integer',
  'money',
  'date',
  'datetime',
  'daterange',
  'datetimerange',
  'month',
  'year',
  'time',
  'timerange',
  'switch',
  'select',
  'radio',
  'radiobutton',
  'checkbox',
  'slider',
  'rate',
  'color',
  'dictselect',
  'cascader',
  'treeselect',
  'regiontreeselect',
  'orgtreeselect',
  'orgselect',
  'departmentselect',
  'departmenttreeselect',
  'deptselect',
  'depttreeselect',
  'eltreeselect',
  'orgname',
  'deptname',
  'userselect',
  'userpicker',
  'username',
  'fileupload',
  'imageupload',
  'upload',
  'objectreference',
  'recordselector',
  'customselect',
  'text',
  'fcrow',
  'row',
  'col',
  'elcard',
  'card',
  'eltabs',
  'tabs',
  'eltabpane',
  'tabpane',
  'elcollapse',
  'collapse',
  'elcollapseitem',
  'collapseitem',
  'fctable',
  'table',
  'fctablegrid',
  'tablegrid',
  'crudblock',
  'eldivider',
  'divider',
  'fctitle',
  'title',
  'text',
  'html',
  'space',
  'elalert',
  'alert',
  'elbutton',
  'button',
  'eltag',
  'tag',
  'elimage',
  'image',
  'richtext',
  'watermark',
  'vuecomponent',
  'htmltag',
  'markdown',
  'barcode',
  'barcodeScanner',
  'qrcode',
  'calendar',
  'code',
  'countdown',
  'descriptions',
  'announcement',
  'list',
  'log',
  'numberanimation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
])

const FULL_ROW_COMPONENT_KEYS = new Set([
  'textarea',
  'fileUpload',
  'imageUpload',
  'upload',
  'subTable',
  'elDivider',
  'divider',
  'fcTitle',
  'title',
  'fcTable',
  'table',
  'fcTableGrid',
  'tableGrid',
  'AiCrudPage',
  'crudBlock',
  'rich-text',
  'watermark',
  'vue-component',
  'html-tag',
  'markdown',
  'barcode',
  'barcodeScanner',
  'qrcode',
  'calendar',
  'code',
  'countdown',
  'descriptions',
  'announcement',
  'list',
  'log',
  'number-animation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
])

const FULL_ROW_LAYOUT_COMPONENT_KEYS = new Set([
  'fcRow',
  'row',
  'elCard',
  'card',
  'elTabs',
  'tabs',
  'elTabPane',
  'tabPane',
  'elCollapse',
  'collapse',
  'elCollapseItem',
  'collapseItem',
])

const LAYOUT_COMPONENT_LABELS = Object.assign({
  fcRow: '栅格布局',
  row: '栅格布局',
  col: '栅格列',
  elCard: '卡片',
  card: '卡片',
  elTabs: '标签页',
  tabs: '标签页',
  elTabPane: '标签面板',
  tabPane: '标签面板',
  elCollapse: '折叠面板',
  collapse: '折叠面板',
  elCollapseItem: '折叠项',
  collapseItem: '折叠项',
  elDivider: '分割线',
  divider: '分割线',
  AiFormSectionTitle: '表单分隔线',
  fcTitle: '分组标题',
  title: '分组标题',
  fcTable: '表格布局',
  table: '表格布局',
  fcTableGrid: '表格单元格',
  tableGrid: '表格单元格',
  AiCrudPage: 'CRUD区块',
  crudBlock: 'CRUD区块',
  watermark: '水印',
  markdown: 'Markdown',
  barcode: '条形码',
  barcodeScanner: '扫码输入',
  qrcode: '二维码',
  calendar: '日历',
  code: '代码',
  countdown: '倒计时',
  descriptions: '描述',
  announcement: '公示',
  list: '列表',
  log: '日志',
  breadcrumb: '面包屑',
  menu: '菜单',
  pagination: '分页',
  split: '面板分隔',
}, {
  'rich-text': '富文本框',
  'vue-component': 'Vue组件',
  'html-tag': 'HTML标签',
  'number-animation': '数值动画',
})

const RAW_LAYOUT_LABELS = new Set([
  '',
  'layout',
  'row',
  'Row',
  'fcRow',
  'FcRow',
  'col',
  'Col',
  'Grid',
  'elCard',
  'ElCard',
  'card',
  'Card',
  'elTabs',
  'ElTabs',
  'tabs',
  'Tabs',
  'elTabPane',
  'ElTabPane',
  'tabPane',
  'TabPane',
  'Tab Pane',
  'elCollapse',
  'ElCollapse',
  'collapse',
  'Collapse',
  'elCollapseItem',
  'ElCollapseItem',
  'collapseItem',
  'CollapseItem',
  'Collapse Item',
  'elDivider',
  'ElDivider',
  'divider',
  'Divider',
  'fcTitle',
  'FcTitle',
  'title',
  'Title',
  'fcTable',
  'FcTable',
  'table',
  'Table',
  'fcTableGrid',
  'FcTableGrid',
  'tableGrid',
  'TableGrid',
  'Table Grid',
  'AiCrudPage',
  'AI CRUD Page',
  'crudBlock',
  'CrudBlock',
  'CRUD Block',
  'CRUD区块',
  'div',
  '布局',
])

export function createDefaultFormDesignerSchema(options = {}) {
  const formKey = options.formKey || buildFormKey(options.objectCode)
  const fields = Array.isArray(options.fields) ? options.fields : []
  const gridColumns = clampGridColumns(options.gridColumns, 2)
  return applyGridColumnsToFormDesignerSchema({
    schemaVersion: FORM_DESIGNER_SCHEMA_VERSION,
    formKey,
    formName: options.formName || options.objectName || '业务表单',
    layout: {
      labelPlacement: 'left',
      labelAlign: 'right',
      labelWidth: 'auto',
      size: 'medium',
      modalType: options.modalType || 'modal',
      formOpenMode: normalizeFormOpenMode(options.formOpenMode || options.modalType),
      showFeedback: true,
      gridColumns,
      rowGap: 16,
      columnGap: 16,
    },
    components: fields
      .filter(field => field && (field.formVisible !== false || hasFieldRuntimeVisibilityRules(field)) && !field.systemField && (options.includeReadonlyFields || !field.readonly))
      .map((field, index) => createComponentFromField(field, index)),
  }, gridColumns)
}

export function normalizeFormDesignerSchema(source = {}) {
  const schema = isPlainObject(source) ? cloneValue(source) : {}
  const components = Array.isArray(schema.components) ? schema.components : []
  const usedIds = new Set()
  const normalizedComponents = components
    .map((component, index) => normalizeComponent(component, index, usedIds))
    .filter(Boolean)
  return {
    schemaVersion: schema.schemaVersion || FORM_DESIGNER_SCHEMA_VERSION,
    formKey: schema.formKey || buildFormKey(schema.objectCode),
    formName: schema.formName || '业务表单',
    layout: normalizeLayout(schema.layout),
    components: migrateLegacyInteractionRules(normalizedComponents),
    pageSections: normalizePageSections(schema.pageSections),
    bottomBar: normalizeBottomBar(schema.bottomBar),
    settings: isPlainObject(schema.settings) ? schema.settings : {},
  }
}

/**
 * 画布只读根级 components。保存后的多表单会把字段放在 forms[].schema 或 settings.formAssets 里，
 * 根级 components 为空时要抬出默认表单，否则回到表单设计看到的是空白画布。
 */
export function presentFormDesignerSchema(source = {}) {
  const root = normalizeFormDesignerSchema(source)
  if (root.components?.length)
    return root
  const multi = normalizeMultiFormDesignerSchema(source)
  const active = multi.forms.find(form => form.formKey === (multi.defaultFormKey || root.formKey))
    || multi.forms.find(form => form.schema?.components?.length)
  if (!active?.schema?.components?.length)
    return root
  return normalizeFormDesignerSchema({
    ...active.schema,
    formKey: active.formKey || root.formKey,
    formName: active.formName || active.schema.formName || root.formName,
    settings: {
      ...(active.schema.settings || {}),
      ...(root.settings || {}),
      formAssets: multi.settings?.formAssets || root.settings?.formAssets || [],
    },
  })
}

export function normalizePageSections(source = []) {
  const usedIds = new Set()
  return (Array.isArray(source) ? source : [])
    .filter(isPlainObject)
    .map((section, index) => {
      const sectionType = ['card', 'child_table'].includes(section.sectionType) ? section.sectionType : 'card'
      const sectionId = reserveProtocolId(section.sectionId, `section_${index + 1}`, usedIds)
      const visibleInModes = uniqueStrings(section.visibleInModes)
      const normalized = {
        ...section,
        sectionId,
        sectionType,
        title: String(section.title || '').trim() || `分区 ${index + 1}`,
        ...(Object.prototype.hasOwnProperty.call(section, 'visibleInModes') ? { visibleInModes } : {}),
      }
      if (sectionType === 'child_table') {
        normalized.relationKey = String(section.relationKey || '').trim()
        normalized.displayMode = ['inline_grid', 'card_list', 'bottom_sheet'].includes(section.displayMode)
          ? section.displayMode
          : 'inline_grid'
        delete normalized.fields
        delete normalized.fieldOverrides
        delete normalized.collapsible
        delete normalized.collapsedByDefault
        return normalized
      }
      normalized.fields = uniqueStrings(section.fields)
      if (isPlainObject(section.fieldOverrides))
        normalized.fieldOverrides = section.fieldOverrides
      normalized.collapsible = section.collapsible === true
      if (Object.prototype.hasOwnProperty.call(section, 'collapsedByDefault'))
        normalized.collapsedByDefault = normalized.collapsible && section.collapsedByDefault === true
      delete normalized.relationKey
      delete normalized.displayMode
      return normalized
    })
}

export function normalizeBottomBar(source = {}) {
  const bottomBar = isPlainObject(source) ? source : {}
  const usedIds = new Set()
  return {
    ...bottomBar,
    actions: (Array.isArray(bottomBar.actions) ? bottomBar.actions : [])
      .filter(isPlainObject)
      .map((action, index) => {
        const actionId = String(action.actionId || '').trim()
        return {
          ...action,
          ...(actionId ? { actionId: reserveProtocolId(actionId, `bottom_action_${index + 1}`, usedIds) } : {}),
          type: String(action.type || 'save').trim().toLowerCase(),
          label: String(action.label || '').trim(),
          ...(Object.prototype.hasOwnProperty.call(action, 'visibleInModes') ? { visibleInModes: uniqueStrings(action.visibleInModes) } : {}),
          ...(action.actionCode ? { actionCode: String(action.actionCode).trim() } : {}),
          ...(action.displayCondition ? { displayCondition: String(action.displayCondition).trim() } : {}),
          ...(action.confirmText ? { confirmText: String(action.confirmText).trim() } : {}),
          ...(action.successMessage ? { successMessage: String(action.successMessage).trim() } : {}),
        }
      }),
  }
}

function reserveProtocolId(sourceId, fallbackId, usedIds) {
  const base = String(sourceId || fallbackId).trim() || fallbackId
  if (!usedIds.has(base)) {
    usedIds.add(base)
    return base
  }
  for (let index = 2; index < 1000; index += 1) {
    const candidate = `${base}_${index}`
    if (!usedIds.has(candidate)) {
      usedIds.add(candidate)
      return candidate
    }
  }
  const candidate = `${base}_${Date.now()}`
  usedIds.add(candidate)
  return candidate
}

function uniqueStrings(source = []) {
  return [...new Set((Array.isArray(source) ? source : [])
    .map(value => String(value || '').trim())
    .filter(Boolean))]
}

/**
 * 兼容早期属性面板把“显示/隐藏、启用/禁用”保存到 source.props.__events 的协议。
 * 运行时规则属于目标组件，因此在规范化阶段把旧事件投影到目标组件，保证预览、发布和下载共用同一份规则。
 */
function migrateLegacyInteractionRules(components = []) {
  const byId = new Map()
  const sources = []
  const walk = (items = []) => {
    ;(Array.isArray(items) ? items : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      if (component.id)
        byId.set(component.id, component)
      sources.push(component)
      walk(component.children || [])
    })
  }
  walk(components)

  // 派生规则由旧事件重新计算；先清理旧投影，确保删除或修改来源事件后目标状态同步更新。
  sources.forEach((component) => {
    const currentRules = Array.isArray(component.props?.runtimeRules) ? component.props.runtimeRules : []
    const retainedRules = currentRules.filter(rule => !rule?.legacyEventId)
    if (retainedRules.length === currentRules.length)
      return
    component.props = {
      ...(component.props || {}),
      runtimeRules: retainedRules,
    }
  })

  sources.forEach((source) => {
    // 未绑定业务字段时运行态用 component.id 合成 formData key（见 resolve-ai-form），迁移条件字段必须对齐
    const sourceField = String(source.fieldBinding?.fieldCode || source.id || source.key || '').trim()
    if (!sourceField)
      return
    const events = Array.isArray(source.props?.__events) ? source.props.__events : []
    events.forEach((event, eventIndex) => {
      const action = String(event?.action || '').trim()
      if (!['showHide', 'enableDisable'].includes(action) || !event?.targetId)
        return
      const target = byId.get(event.targetId)
      if (!target || target === source)
        return
      const marker = `legacy:${source.id}:${event.id || eventIndex}:${action}`
      const currentRules = Array.isArray(target.props?.runtimeRules) ? target.props.runtimeRules : []
      if (currentRules.some(rule => rule?.id === marker || rule?.legacyEventId === marker))
        return

      const nextRule = {
        id: marker,
        legacyEventId: marker,
        enabled: event.enabled !== false,
        mode: 'all',
        conditions: event.whenValue === undefined || event.whenValue === ''
          ? []
          : [{ source: 'formData', field: sourceField, operator: 'eq', value: event.whenValue }],
        effect: action === 'showHide'
          ? buildLegacyVisibilityEffect(event.value)
          : buildLegacyDisabledEffect(event.value),
      }
      target.props = {
        ...(target.props || {}),
        runtimeRules: [...currentRules, nextRule],
      }
    })
  })
  return components
}

function buildLegacyVisibilityEffect(value) {
  const visible = parseLegacyBoolean(value, true)
  return visible
    ? { visible: true, whenUnmatched: 'hidden' }
    : { hidden: true, whenUnmatched: 'visible' }
}

function buildLegacyDisabledEffect(value) {
  const enabled = parseLegacyBoolean(value, true)
  return { disabled: !enabled }
}

function parseLegacyBoolean(value, fallback) {
  if (value === true || value === false)
    return value
  const normalized = String(value ?? '').trim().toLowerCase()
  if (['true', '1', 'yes', 'y', 'on'].includes(normalized))
    return true
  if (['false', '0', 'no', 'n', 'off'].includes(normalized))
    return false
  return fallback
}

function hasFieldRuntimeVisibilityRules(field = {}) {
  const candidates = [field.runtimeRules, field.props?.runtimeRules, field.basicProps?.runtimeRules]
  return candidates.some(value => Array.isArray(value) && value.some((rule) => {
    if (rule?.enabled === false)
      return false
    const effect = {
      ...(rule?.effect || {}),
      ...(rule?.actions || {}),
      ...(rule?.then || {}),
      ...rule,
    }
    return Object.prototype.hasOwnProperty.call(effect, 'visible') || effect.hidden === true
  }))
}

export function normalizeMultiFormDesignerSchema(source = {}) {
  const legacySchema = normalizeFormDesignerSchema(source)
  const defaultFormKey = source?.defaultFormKey || source?.settings?.defaultFormKey || legacySchema.formKey || buildFormKey(source?.objectCode)
  const formsByKey = new Map()

  const appendForm = (rawForm = {}, fallback = {}) => {
    const formSchema = normalizeFormDesignerSchema({
      ...rawForm,
      formKey: rawForm.formKey || fallback.formKey || defaultFormKey,
      formName: rawForm.formName || fallback.formName || '业务表单',
      settings: {
        ...(rawForm.settings || {}),
        formAssets: [],
      },
    })
    if (!formSchema.formKey || formsByKey.has(formSchema.formKey))
      return
    formsByKey.set(formSchema.formKey, {
      formKey: formSchema.formKey,
      formName: formSchema.formName,
      usage: normalizeFormUsage(rawForm.usage || fallback.usage),
      schema: formSchema,
    })
  }

  appendForm(legacySchema, {
    formKey: defaultFormKey,
    formName: legacySchema.formName || '默认表单',
    usage: ['create', 'edit'],
  })

  ;(Array.isArray(source?.forms) ? source.forms : []).forEach((form) => {
    appendForm(form?.schema || form, form)
  })

  ;(Array.isArray(legacySchema.settings?.formAssets) ? legacySchema.settings.formAssets : []).forEach((asset) => {
    appendForm(asset?.schema || asset, asset)
  })

  const forms = Array.from(formsByKey.values())
  return {
    ...legacySchema,
    schemaVersion: FORM_DESIGNER_MULTI_SCHEMA_VERSION,
    defaultFormKey,
    forms,
    settings: {
      ...(legacySchema.settings || {}),
      formAssets: forms
        .filter(form => form.formKey !== legacySchema.formKey)
        .map(form => ({
          formKey: form.formKey,
          formName: form.formName,
          usage: form.usage,
          schema: form.schema,
        })),
    },
  }
}

export function normalizeFormDesignerSchemaForSave(source = {}) {
  const multiSchema = normalizeMultiFormDesignerSchema(source)
  const activeForm = multiSchema.forms.find(form => form.formKey === multiSchema.formKey)
    || multiSchema.forms.find(form => form.formKey === multiSchema.defaultFormKey)
    || multiSchema.forms[0]
  const activeSchema = activeForm?.schema || normalizeFormDesignerSchema(multiSchema)
  return {
    ...activeSchema,
    schemaVersion: FORM_DESIGNER_MULTI_SCHEMA_VERSION,
    defaultFormKey: multiSchema.defaultFormKey || activeSchema.formKey,
    forms: multiSchema.forms,
    settings: {
      ...(activeSchema.settings || {}),
      ...(multiSchema.settings || {}),
      formAssets: multiSchema.settings?.formAssets || [],
    },
  }
}

export function validateFormDesignerSchema(source = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const errors = []
  const ids = new Set()

  if (!schema.formKey)
    errors.push({ path: 'formKey', message: '表单编码不能为空' })
  if (!schema.formName)
    errors.push({ path: 'formName', message: '表单名称不能为空' })

  schema.components.forEach((component, index) => {
    const path = `components[${index}]`
    if (!component.id)
      errors.push({ path: `${path}.id`, message: '组件 ID 不能为空' })
    if (component.id && ids.has(component.id))
      errors.push({ path: `${path}.id`, message: `组件 ID 重复：${component.id}` })
    if (component.id)
      ids.add(component.id)
    if (!component.componentKey)
      errors.push({ path: `${path}.componentKey`, message: '组件类型不能为空' })
    if (isFieldComponent(component) && !component.fieldBinding?.fieldCode) {
      errors.push({
        path: `${path}.fieldBinding.fieldCode`,
        message: `${component.label || component.componentKey} 未绑定业务字段`,
      })
    }
  })

  return {
    valid: errors.length === 0,
    errors,
    schema,
  }
}

export function createComponentFromField(field = {}, index = 0) {
  const fieldCode = field.fieldCode || field.field || ''
  const label = field.fieldName || field.label || fieldCode || '字段'
  const componentKey = resolveComponentKey(field)
  return normalizeComponent({
    id: `cmp_${fieldCode || index}`,
    componentKey,
    label,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName: field.columnName || camelToSnake(fieldCode),
      createIfMissing: false,
      source: 'field_asset',
      locked: Boolean(
        field.systemField
        || field.readonly
        || field.locked
        || field.fieldBinding?.locked,
      ),
    },
    props: buildComponentProps(field, label),
    layout: {
      span: resolveDefaultSpan(componentKey),
      align: 'left',
    },
    validation: {
      required: Boolean(field.required),
      requiredMessage: field.required ? buildRequiredMessage(componentKey, label) : '',
    },
    visibility: {
      hidden: field.formVisible === false,
      readonly: Boolean(field.readonly),
    },
  }, index)
}

export function applyGridColumnsToFormDesignerSchema(source = {}, gridColumns = 2) {
  const columns = clampGridColumns(gridColumns, 2)
  const schema = normalizeFormDesignerSchema(source)
  return normalizeFormDesignerSchema({
    ...schema,
    layout: {
      ...(schema.layout || {}),
      gridColumns: columns,
    },
    components: (schema.components || []).map(component => applyGridColumnsToComponent(component, columns)),
  })
}

export function normalizeDesignerComponentLabel(componentKey = '', label = '') {
  const fallback = LAYOUT_COMPONENT_LABELS[componentKey]
  if (!fallback)
    return label || ''
  const value = stripTemporaryDesignerRef(String(label || '').trim())
  return !value || RAW_LAYOUT_LABELS.has(value) ? fallback : value
}

export function resolveDesignerComponentDefaultLabel(componentKey = '') {
  return LAYOUT_COMPONENT_LABELS[componentKey] || ''
}

export function isTemporaryDesignerRef(value = '') {
  return /^ref_[A-Z0-9]+$/i.test(String(value || '').trim())
}

export function isGenericDesignerComponentId(value = '', componentKey = '') {
  const id = String(value || '').trim()
  if (!id.toLowerCase().startsWith('cmp_'))
    return false
  const suffix = id.slice(4).trim()
  if (!suffix)
    return true
  const normalizedSuffix = suffix.toLowerCase()
  const normalizedKey = String(componentKey || '').trim().toLowerCase()
  return normalizedSuffix === normalizedKey
    || FIELD_COMPONENT_KEYS.has(suffix)
    || LAYOUT_COMPONENT_KEYS.has(suffix)
    || VIRTUAL_COMPONENT_KEYS.has(suffix)
    || GENERIC_COMPONENT_ID_SUFFIXES.has(normalizedSuffix)
}

export function createFieldBindingFromLabel(label, options = {}) {
  const fieldCode = options.fieldCode || generateFieldCode(label)
  return {
    mode: options.mode || 'field',
    fieldCode,
    columnName: options.columnName || camelToSnake(fieldCode),
    createIfMissing: options.createIfMissing !== false,
    source: options.source || 'designer',
    locked: Boolean(options.locked),
  }
}

export function isFieldComponent(component = {}) {
  if (FIELD_COMPONENT_KEYS.has(component.componentKey) && component.fieldBinding?.mode !== 'virtual')
    return true
  if (isPageWidgetComponentKey(component.componentKey))
    return false
  return FIELD_COMPONENT_KEYS.has(component.componentKey)
}

export function isLayoutComponent(component = {}) {
  return LAYOUT_COMPONENT_KEYS.has(component.componentKey) || isPageWidgetComponentKey(component.componentKey)
}

export function isVirtualComponent(component = {}) {
  return VIRTUAL_COMPONENT_KEYS.has(component.componentKey) || !isFieldComponent(component)
}

export function extractFieldBindings(schema = {}) {
  return normalizeFormDesignerSchema(schema).components.filter(component => isFieldComponent(component) && component.fieldBinding?.mode === 'field').map(component => ({
    componentId: component.id,
    fieldCode: component.fieldBinding.fieldCode,
    createIfMissing: Boolean(component.fieldBinding.createIfMissing),
    locked: Boolean(component.fieldBinding.locked),
  }))
}

export function findDesignerComponentPath(source = {}, componentId = '') {
  if (!componentId)
    return null
  const schema = normalizeFormDesignerSchema(source)
  return findComponentPathInList(schema.components, componentId, [])
}

export function getDesignerComponent(source = {}, componentId = '') {
  const schema = normalizeFormDesignerSchema(source)
  const path = findDesignerComponentPath(schema, componentId)
  if (!path)
    return null
  return getComponentAtPath(schema.components, path)
}

export function insertDesignerComponent(source = {}, target = {}, component = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const nextComponent = normalizeComponent(component, schema.components.length, collectReservedComponentIds(schema.components))
  if (!nextComponent)
    return schema
  const parentChildren = resolveTargetChildren(schema, target.parentId)
  const index = clampInsertIndex(target.index, parentChildren.length)
  parentChildren.splice(index, 0, nextComponent)
  return normalizeFormDesignerSchema(schema)
}

export function moveDesignerComponent(source = {}, sourceId = '', target = {}) {
  if (!sourceId)
    return normalizeFormDesignerSchema(source)
  const schema = normalizeFormDesignerSchema(source)
  const sourcePath = findComponentPathInList(schema.components, sourceId, [])
  if (!sourcePath)
    return schema
  const sourceComponent = getComponentAtPath(schema.components, sourcePath)
  if (!sourceComponent || isDescendantTarget(sourceComponent, target.parentId))
    return schema
  const sourceParentId = resolveParentComponentId(schema.components, sourcePath)
  const removed = removeComponentAtPath(schema.components, sourcePath)
  if (!removed)
    return schema
  const parentChildren = resolveTargetChildren(schema, target.parentId)
  const movingDownInSameParent = sourceParentId === (target.parentId || '') && sourcePath[sourcePath.length - 1] < Number(target.index)
  const index = clampInsertIndex(movingDownInSameParent ? Number(target.index) - 1 : target.index, parentChildren.length)
  parentChildren.splice(index, 0, removed)
  return normalizeFormDesignerSchema(schema)
}

export function removeDesignerComponent(source = {}, componentId = '') {
  if (!componentId)
    return normalizeFormDesignerSchema(source)
  const schema = normalizeFormDesignerSchema(source)
  const path = findComponentPathInList(schema.components, componentId, [])
  if (!path)
    return schema
  removeComponentAtPath(schema.components, path)
  return normalizeFormDesignerSchema(schema)
}

export function duplicateDesignerComponent(source = {}, componentId = '') {
  const schema = normalizeFormDesignerSchema(source)
  const path = findComponentPathInList(schema.components, componentId, [])
  const component = path ? getComponentAtPath(schema.components, path) : null
  if (!component)
    return schema
  const copy = cloneValue(component)
  rewriteComponentIds(copy, collectReservedComponentIds(schema.components))
  const parentPath = path.slice(0, -1)
  const parentChildren = parentPath.length ? getComponentAtPath(schema.components, parentPath)?.children : schema.components
  parentChildren.splice(path[path.length - 1] + 1, 0, copy)
  return normalizeFormDesignerSchema(schema)
}

export function updateDesignerComponent(source = {}, componentId = '', patch = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const path = findComponentPathInList(schema.components, componentId, [])
  const component = path ? getComponentAtPath(schema.components, path) : null
  if (!component)
    return schema
  Object.assign(component, mergeComponentPatch(component, patch))
  return normalizeFormDesignerSchema(schema)
}

/**
 * Adds a structural child to a layout component through the schema mutation
 * layer so a panel re-render cannot lose a newly added tab pane.
 */
export function appendDesignerLayoutChild(source = {}, componentId = '', componentKey = '') {
  const schema = normalizeFormDesignerSchema(source)
  const parent = getDesignerComponent(schema, componentId)
  const parentKey = parent?.componentKey || ''
  const isTabs = ['tabs', 'elTabs'].includes(parentKey)
  const isCollapse = ['collapse', 'elCollapse'].includes(parentKey)
  const childKey = componentKey || (isTabs ? 'tabPane' : isCollapse ? 'collapseItem' : '')
  if (!parent || !childKey || !canAcceptDesignerChild(parent, { componentKey: childKey }))
    return schema

  const children = Array.isArray(parent.children) ? parent.children : []
  const childIndex = children.length + 1
  const childId = reserveComponentId(
    `${parent.id}_${isTabs ? 'pane' : 'item'}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    collectReservedComponentIds(schema.components),
  )
  const label = isTabs ? `标签 ${childIndex}` : `分组 ${childIndex}`
  return updateDesignerComponent(schema, parent.id, {
    children: [...children, {
      id: childId,
      componentKey: childKey,
      label,
      props: isTabs ? { label, name: childId } : { title: label, name: childId },
      layout: {
        span: parent.layout?.span || schema.layout?.gridColumns || 1,
        align: 'left',
      },
      children: [],
    }],
  })
}

export function updateDesignerLayout(source = {}, patch = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const next = normalizeFormDesignerSchema({
    ...schema,
    layout: {
      ...(schema.layout || {}),
      ...(isPlainObject(patch) ? patch : {}),
    },
  })
  if (Object.prototype.hasOwnProperty.call(patch || {}, 'gridColumns'))
    return reconcileDesignerGridColumns(next, next.layout.gridColumns)
  return next
}

export function canAcceptDesignerChild(parentComponent = null, childComponent = {}) {
  if (!childComponent)
    return false
  if (!parentComponent)
    return true
  const parentKey = parentComponent.componentKey || ''
  const childKey = childComponent.componentKey || ''
  if (['tabs', 'elTabs'].includes(parentKey))
    return ['tabPane', 'elTabPane'].includes(childKey)
  if (['collapse', 'elCollapse'].includes(parentKey))
    return ['collapseItem', 'elCollapseItem'].includes(childKey)
  if (['table', 'fcTable'].includes(parentKey))
    return ['tableGrid', 'fcTableGrid'].includes(childKey)
  if (['tabPane', 'elTabPane', 'collapseItem', 'elCollapseItem', 'card', 'elCard', 'crudBlock', 'row', 'fcRow', 'col', 'tableGrid', 'fcTableGrid'].includes(parentKey))
    return true
  return false
}

/**
 * 组件库点击追加时解析落点：优先嵌进当前选中容器（卡片/标签页/折叠项/栅格列等），
 * 字段选中则插到同级后方；无法承接时回退画布根部。
 */
export function resolveAppendShelfTarget(source = {}, selectedId = '', childComponent = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const rootTarget = { parentId: '', index: schema.components.length }
  if (!selectedId)
    return rootTarget

  const selected = getDesignerComponent(schema, selectedId)
  if (!selected)
    return rootTarget

  const selectedKey = selected.componentKey || ''
  const slotParent = resolveAppendSlotParent(selected, selectedKey)
  if (slotParent && canAcceptDesignerChild(slotParent, childComponent)) {
    return {
      parentId: slotParent.id,
      index: (slotParent.children || []).length,
    }
  }

  if (canAcceptDesignerChild(selected, childComponent)) {
    return {
      parentId: selected.id,
      index: (selected.children || []).length,
    }
  }

  const path = findDesignerComponentPath(schema, selectedId)
  if (!path?.length)
    return rootTarget
  const parentPath = path.slice(0, -1)
  const parent = parentPath.length ? getComponentAtPath(schema.components, parentPath) : null
  if (parent && !canAcceptDesignerChild(parent, childComponent))
    return rootTarget
  return {
    parentId: parent?.id || '',
    index: path[path.length - 1] + 1,
  }
}

function resolveAppendSlotParent(selected = {}, selectedKey = '') {
  const children = Array.isArray(selected.children) ? selected.children : []
  if (['tabs', 'elTabs'].includes(selectedKey))
    return children.find(item => ['tabPane', 'elTabPane'].includes(item?.componentKey)) || children[0] || null
  if (['collapse', 'elCollapse'].includes(selectedKey))
    return children.find(item => ['collapseItem', 'elCollapseItem'].includes(item?.componentKey)) || children[0] || null
  if (['row', 'fcRow'].includes(selectedKey))
    return children.find(item => ['col', 'fcCol'].includes(item?.componentKey)) || children[0] || null
  if (['table', 'fcTable'].includes(selectedKey))
    return children.find(item => ['tableGrid', 'fcTableGrid'].includes(item?.componentKey)) || children[0] || null
  return null
}

export function cloneValue(value) {
  if (value === undefined)
    return undefined
  return JSON.parse(JSON.stringify(value ?? null))
}

function findComponentPathInList(components = [], componentId = '', parentPath = []) {
  for (let index = 0; index < components.length; index += 1) {
    const component = components[index]
    const path = [...parentPath, index]
    if (component?.id === componentId)
      return path
    const childPath = findComponentPathInList(component?.children || [], componentId, path)
    if (childPath)
      return childPath
  }
  return null
}

function getComponentAtPath(components = [], path = []) {
  let current = null
  let children = components
  for (const index of path) {
    current = children[index]
    if (!current)
      return null
    children = current.children || []
  }
  return current
}

function resolveParentComponentId(components = [], path = []) {
  if (!path.length || path.length === 1)
    return ''
  const parent = getComponentAtPath(components, path.slice(0, -1))
  return parent?.id || ''
}

function removeComponentAtPath(components = [], path = []) {
  if (!path.length)
    return null
  const parentPath = path.slice(0, -1)
  const parent = parentPath.length ? getComponentAtPath(components, parentPath) : null
  const children = parent ? parent.children || [] : components
  const index = path[path.length - 1]
  const [removed] = children.splice(index, 1)
  return removed || null
}

function resolveTargetChildren(schema = {}, parentId = '') {
  if (!parentId)
    return schema.components
  const path = findComponentPathInList(schema.components, parentId, [])
  const parent = path ? getComponentAtPath(schema.components, path) : null
  if (!parent)
    return schema.components
  if (!Array.isArray(parent.children))
    parent.children = []
  return parent.children
}

function clampInsertIndex(index, length) {
  const number = Number(index)
  if (!Number.isFinite(number))
    return length
  return Math.max(0, Math.min(length, number))
}

function isDescendantTarget(component = {}, targetParentId = '') {
  if (!targetParentId)
    return false
  if (component.id === targetParentId)
    return true
  return (component.children || []).some(child => isDescendantTarget(child, targetParentId))
}

function rewriteComponentIds(component = {}, usedIds = new Set()) {
  component.id = reserveComponentId(`${component.id || 'cmp_component'}_copy`, usedIds)
  ;(component.children || []).forEach(child => rewriteComponentIds(child, usedIds))
}

function mergeComponentPatch(component = {}, patch = {}) {
  const source = isPlainObject(patch) ? patch : {}
  return {
    ...source,
    props: source.props ? { ...(component.props || {}), ...source.props } : component.props,
    layout: source.layout ? { ...(component.layout || {}), ...source.layout } : component.layout,
    validation: source.validation ? { ...(component.validation || {}), ...source.validation } : component.validation,
    visibility: source.visibility ? { ...(component.visibility || {}), ...source.visibility } : component.visibility,
  }
}

function normalizeLayout(layout = {}) {
  const source = isPlainObject(layout) ? layout : {}
  const normalized = {
    labelPlacement: source.labelPlacement || 'left',
    labelAlign: normalizeLabelAlign(source.labelAlign),
    labelWidth: normalizeLabelWidth(source.labelWidth),
    size: normalizeFormSize(source.size),
    modalType: ['modal', 'drawer'].includes(source.modalType) ? source.modalType : 'modal',
    formOpenMode: normalizeFormOpenMode(source.formOpenMode || source.modalType),
    showFeedback: source.showFeedback === undefined ? true : Boolean(source.showFeedback),
    hideRequiredAsterisk: Boolean(source.hideRequiredAsterisk),
    inlineFeedback: Boolean(source.inlineFeedback || source.inlineMessage),
    gridColumns: clampGridColumns(source.gridColumns ?? source.gridCols, 2),
    gridCols: clampGridColumns(source.gridCols ?? source.gridColumns, 2),
    rowGap: Number(source.rowGap ?? source.yGap ?? 16),
    columnGap: Number(source.columnGap ?? source.xGap ?? 16),
    xGap: Number(source.xGap ?? source.columnGap ?? 16),
    yGap: Number(source.yGap ?? source.rowGap ?? 16),
    modalWidth: source.modalWidth || '800px',
    detailModalWidth: source.detailModalWidth || source.modalWidth || '800px',
    drawerPlacement: ['left', 'right'].includes(source.drawerPlacement) ? source.drawerPlacement : 'right',
  }
  if (source.formStyle !== undefined)
    normalized.formStyle = cloneValue(source.formStyle)
  if (source.formStyleText !== undefined)
    normalized.formStyleText = source.formStyleText
  if (source.formClass !== undefined)
    normalized.formClass = source.formClass
  normalized.showActions = source.showActions === undefined ? true : Boolean(source.showActions)
  normalized.showSubmit = source.showSubmit === undefined ? true : Boolean(source.showSubmit)
  normalized.showReset = source.showReset === undefined ? true : Boolean(source.showReset)
  normalized.showCancel = Boolean(source.showCancel)
  normalized.submitText = source.submitText || '提交'
  normalized.resetText = source.resetText || '重置'
  normalized.cancelText = source.cancelText || '取消'
  normalized.enableCollapse = Boolean(source.enableCollapse)
  normalized.maxVisibleFields = resolveNumber(source.maxVisibleFields, 6)
  return normalized
}

function normalizeComponent(component, index, usedIds = new Set()) {
  if (!isPlainObject(component))
    return null
  const componentKey = component.componentKey || component.type || 'input'
  const fieldComponent = FIELD_COMPONENT_KEYS.has(componentKey)
  const hasExplicitLabel = Object.prototype.hasOwnProperty.call(component, 'label')
  const sourceLabel = hasExplicitLabel
    ? component.label
    : component.title || component.props?.header || component.props?.label || component.props?.title || component.fieldBinding?.fieldCode || (fieldComponent ? '字段' : '布局')
  const label = normalizeDesignerComponentLabel(componentKey, sourceLabel)
  const sourceId = String(component.id || '').trim()
  const fieldBinding = fieldComponent
    ? normalizeFieldBinding(component.fieldBinding, component.field || component.name)
    : normalizeFieldBinding({ ...(component.fieldBinding || {}), mode: 'virtual', fieldCode: '' })
  return {
    id: resolveNormalizedComponentId(sourceId, componentKey, fieldBinding, fieldComponent, index, usedIds),
    componentKey,
    label,
    fieldBinding,
    props: normalizeComponentProps(componentKey, component.props),
    layout: normalizeComponentLayout(component.layout),
    validation: normalizeValidation(component.validation),
    visibility: normalizeVisibility(component.visibility),
    advancedProps: isPlainObject(component.advancedProps) ? { ...component.advancedProps } : {},
    children: Array.isArray(component.children)
      ? component.children.map((child, childIndex) => normalizeComponent(child, childIndex, usedIds)).filter(Boolean)
      : [],
  }
}

function normalizeComponentProps(componentKey = '', props = {}) {
  const normalizedProps = isPlainObject(props) ? { ...props } : {}
  if (normalizedProps.dictType && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader'].includes(componentKey))
    delete normalizedProps.options
  return normalizedProps
}

function collectReservedComponentIds(components = [], ids = new Set()) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!isPlainObject(component))
      return
    const componentKey = component.componentKey || component.type || 'input'
    const sourceId = String(component.id || '').trim()
    if (sourceId && !isTemporaryDesignerRef(sourceId) && !isGenericDesignerComponentId(sourceId, componentKey))
      ids.add(sourceId)
    if (Array.isArray(component.children))
      collectReservedComponentIds(component.children, ids)
  })
  return ids
}

function resolveNormalizedComponentId(sourceId, componentKey, fieldBinding, fieldComponent, index, usedIds = new Set()) {
  if (sourceId && !isTemporaryDesignerRef(sourceId) && !isGenericDesignerComponentId(sourceId, componentKey)) {
    if (!usedIds.has(sourceId)) {
      usedIds.add(sourceId)
      return sourceId
    }
    return reserveComponentId(sourceId, usedIds)
  }
  const base = fieldComponent
    ? `cmp_${fieldBinding?.fieldCode || index}`
    : `cmp_${componentKey || 'layout'}_${index}`
  return reserveComponentId(base, usedIds)
}

function reserveComponentId(baseId, usedIds = new Set()) {
  const base = String(baseId || 'cmp_component').trim() || 'cmp_component'
  if (!usedIds.has(base)) {
    usedIds.add(base)
    return base
  }
  for (let index = 2; index < 1000; index += 1) {
    const candidate = `${base}_${index}`
    if (!usedIds.has(candidate)) {
      usedIds.add(candidate)
      return candidate
    }
  }
  const candidate = `${base}_${Date.now()}`
  usedIds.add(candidate)
  return candidate
}

function stripTemporaryDesignerRef(value = '') {
  return String(value || '')
    .trim()
    .replace(/^ref_[A-Z0-9]+[\s:：-]*/i, '')
    .trim()
}

export function normalizeFieldBinding(binding = {}, fallbackFieldCode = '') {
  const source = isPlainObject(binding) ? binding : {}
  const fieldCode = source.fieldCode || fallbackFieldCode || ''
  return {
    mode: source.mode || (fieldCode ? 'field' : 'virtual'),
    fieldCode,
    ...(source.columnName ? { columnName: source.columnName } : {}),
    createIfMissing: source.createIfMissing !== false && Boolean(fieldCode),
    source: source.source || 'designer',
    locked: Boolean(source.locked),
    ...(source.fieldType ? { fieldType: source.fieldType } : {}),
    ...(source.dataType ? { dataType: source.dataType } : {}),
    ...(source.componentType ? { componentType: source.componentType } : {}),
  }
}

function normalizeComponentLayout(layout = {}) {
  const source = isPlainObject(layout) ? layout : {}
  return {
    span: Number(source.span || 1),
    align: normalizeAlign(source.align),
    ...(source.width ? { width: source.width } : {}),
    ...(source.labelWidth ? { labelWidth: source.labelWidth } : {}),
  }
}

function applyGridColumnsToComponent(component = {}, gridColumns = 2, parentComponent = null) {
  const next = cloneValue(component)
  const componentKey = next.componentKey || ''
  const fieldComponent = FIELD_COMPONENT_KEYS.has(componentKey)
  const fullRow = FULL_ROW_COMPONENT_KEYS.has(componentKey) || FULL_ROW_LAYOUT_COMPONENT_KEYS.has(componentKey)
  const currentSpan = resolveNumber(next.layout?.span, fullRow ? gridColumns : 1)
  const parentGridColumns = ['row', 'fcRow'].includes(parentComponent?.componentKey)
    ? clampGridColumns(parentComponent.props?.columns, MAX_FORM_GRID_COLUMNS)
    : gridColumns
  const nextSpan = componentKey === 'col'
    ? Math.max(1, Math.min(parentGridColumns, resolveNumber(next.layout?.span ?? next.props?.span, Math.min(6, parentGridColumns))))
    : (!fieldComponent && !fullRow)
        ? 1
        : fullRow
          ? gridColumns
          : Math.max(1, Math.min(gridColumns, currentSpan))
  next.label = normalizeDesignerComponentLabel(componentKey, next.label)
  next.layout = {
    ...(next.layout || {}),
    span: nextSpan,
  }
  if (componentKey === 'col') {
    next.props = {
      ...(next.props || {}),
      span: next.layout.span,
    }
  }
  else {
    next.props = next.props || {}
  }
  next.children = Array.isArray(next.children)
    ? next.children.map(child => applyGridColumnsToComponent(child, gridColumns, next))
    : []
  return next
}

function reconcileDesignerGridColumns(source = {}, gridColumns = 2) {
  const columns = clampGridColumns(gridColumns, 2)
  return normalizeFormDesignerSchema({
    ...source,
    layout: {
      ...(source.layout || {}),
      gridColumns: columns,
    },
    components: (source.components || []).map(component => applyGridColumnsToComponent(component, columns)),
  })
}

function normalizeValidation(validation = {}) {
  const source = isPlainObject(validation) ? validation : {}
  return {
    required: Boolean(source.required),
    requiredMessage: source.requiredMessage || '',
    preset: source.preset || source.presetCode || source.commonRule || '',
    pattern: source.pattern || '',
    message: source.message || '',
    rules: Array.isArray(source.rules) ? source.rules : [],
  }
}

function normalizeVisibility(visibility = {}) {
  const source = isPlainObject(visibility) ? visibility : {}
  return {
    hidden: Boolean(source.hidden),
    readonly: Boolean(source.readonly),
  }
}

function normalizeAlign(value) {
  return ['left', 'center', 'right'].includes(value) ? value : 'left'
}

function normalizeLabelAlign(value) {
  return ['left', 'right'].includes(value) ? value : 'right'
}

function normalizeLabelWidth(value) {
  if (value === undefined || value === null || value === '')
    return 'auto'
  if (String(value).trim() === 'auto')
    return 'auto'
  return resolveNumber(value, 100)
}

function normalizeFormSize(value) {
  if (value === 'default' || value === 'medium')
    return 'medium'
  return ['small', 'large'].includes(value) ? value : 'medium'
}

function normalizeFormOpenMode(value) {
  const mode = String(value || '').trim()
  if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace')
    return 'tabWorkspace'
  const normalized = mode.toLowerCase()
  return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : 'modal'
}

function resolveNumber(value, fallback) {
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number.parseInt(value, 10)
    return Number.isFinite(parsed) ? parsed : fallback
  }
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function resolveComponentKey(field = {}) {
  const componentType = field.componentType || ''
  const businessType = String(field.fieldType || field.businessFieldType || '').toUpperCase()
  if (componentType === 'textarea' || businessType === 'MULTILINE')
    return 'textarea'
  if (componentType === 'number' || businessType === 'NUMBER')
    return 'number'
  if (businessType === 'MONEY')
    return 'money'
  if (componentType === 'datetime' || businessType === 'DATETIME')
    return 'datetime'
  if (componentType === 'daterange')
    return 'daterange'
  if (componentType === 'datetimerange')
    return 'datetimerange'
  if (componentType === 'month')
    return 'month'
  if (componentType === 'year')
    return 'year'
  if (componentType === 'date' || businessType === 'DATE')
    return 'date'
  if (componentType === 'time')
    return 'time'
  if (componentType === 'timerange')
    return 'timerange'
  if (componentType === 'switch' || businessType === 'SWITCH')
    return 'switch'
  if (componentType === 'radioButton')
    return 'radioButton'
  if (componentType === 'radio' || businessType === 'RADIO')
    return 'radio'
  if (componentType === 'checkbox' || ['CHECKBOX', 'MULTI_SELECT'].includes(businessType))
    return 'checkbox'
  if (componentType === 'slider')
    return 'slider'
  if (componentType === 'rate')
    return 'rate'
  if (componentType === 'color')
    return 'color'
  if (field.dictType || componentType === 'dictSelect')
    return 'dictSelect'
  if (componentType === 'treeSelect')
    return 'treeSelect'
  if (componentType === 'regionTreeSelect' || businessType === 'REGION')
    return 'regionTreeSelect'
  if (componentType === 'orgTreeSelect' || businessType === 'DEPT')
    return 'orgTreeSelect'
  if (componentType === 'userSelect' || businessType === 'USER')
    return 'userSelect'
  if (componentType === 'imageUpload' || businessType === 'IMAGE')
    return 'imageUpload'
  if (['fileUpload', 'upload'].includes(componentType) || ['FILE', 'ATTACHMENT'].includes(businessType))
    return 'fileUpload'
  if (componentType === 'recordSelector' || field.type === 'recordSelector' || businessType === 'RECORD_SELECTOR')
    return 'recordSelector'
  if (componentType === 'objectReference' || field.type === 'objectReference' || businessType === 'REFERENCE')
    return 'objectReference'
  if (componentType === 'customSelect')
    return 'customSelect'
  if (componentType === 'select' || businessType === 'SELECT' || businessType === 'DICT')
    return 'select'
  return 'input'
}

function buildComponentProps(field = {}, label = '') {
  const props = {
    ...(isPlainObject(field.basicProps) ? field.basicProps : {}),
  }
  delete props.fieldBinding
  if (props.clearable === undefined)
    props.clearable = true
  props.placeholder = props.placeholder || buildPlaceholder(resolveComponentKey(field), label)
  if (field.dictType)
    props.dictType = field.dictType
  if (field.length)
    props.maxlength = field.length
  const referenceObjectCode = field.referenceObjectCode || field.props?.referenceObjectCode || field.basicProps?.referenceObjectCode || ''
  const referenceDisplayField = field.referenceDisplayField || field.props?.referenceDisplayField || field.props?.displayField || field.basicProps?.referenceDisplayField || field.basicProps?.displayField || ''
  const referenceValueField = field.referenceValueField || field.props?.referenceValueField || field.props?.valueField || field.basicProps?.referenceValueField || field.basicProps?.valueField || ''
  if (referenceObjectCode)
    props.referenceObjectCode = referenceObjectCode
  if (referenceDisplayField)
    props.referenceDisplayField = referenceDisplayField
  if (referenceValueField)
    props.referenceValueField = referenceValueField
  // recordSelector: merge from multiple sources to ensure objectCode is never lost
  const resolvedRecordSelector = field.recordSelector
    || field.props?.recordSelector
    || field.basicProps?.recordSelector
    || null
  if (resolvedRecordSelector) {
    props.recordSelector = resolvedRecordSelector
  }
  else if (field.recordSelectorObjectCode || field.props?.recordSelectorObjectCode || field.basicProps?.recordSelectorObjectCode) {
    // Synthesize recordSelector from flat fields (legacy property-panel format)
    const objectCode = field.recordSelectorObjectCode || field.props?.recordSelectorObjectCode || field.basicProps?.recordSelectorObjectCode || ''
    if (objectCode) {
      props.recordSelector = {
        objectCode,
        valueField: field.recordSelectorValueField || field.props?.recordSelectorValueField || field.basicProps?.recordSelectorValueField || 'id',
        labelField: field.recordSelectorLabelField || field.props?.recordSelectorLabelField || field.basicProps?.recordSelectorLabelField || '',
        targetLabelField: field.recordSelectorTargetLabelField || field.props?.recordSelectorTargetLabelField || field.basicProps?.recordSelectorTargetLabelField || '',
        displayFields: field.recordSelectorDisplayFields || field.props?.recordSelectorDisplayFields || field.basicProps?.recordSelectorDisplayFields || [],
        keywordFields: field.recordSelectorKeywordFields || field.props?.recordSelectorKeywordFields || field.basicProps?.recordSelectorKeywordFields || [],
      }
    }
  }
  if (field.targetLabelField || field.props?.targetLabelField)
    props.targetLabelField = field.targetLabelField || field.props?.targetLabelField
  return props
}

function resolveDefaultSpan(componentKey) {
  return ['textarea', 'fileUpload', 'imageUpload', 'subTable', 'daterange', 'datetimerange', 'timerange'].includes(componentKey) ? 2 : 1
}

function clampGridColumns(value, fallback = 2) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, number))
}

function buildPlaceholder(componentKey, label) {
  if (['select', 'radio', 'radioButton', 'checkbox', 'dictSelect', 'date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange', 'regionTreeSelect', 'orgTreeSelect', 'treeSelect', 'userSelect', 'fileUpload', 'imageUpload', 'objectReference', 'recordSelector', 'customSelect', 'color'].includes(componentKey))
    return `请选择${label}`
  return `请填写${label}`
}

function buildRequiredMessage(componentKey, label) {
  return buildPlaceholder(componentKey, label)
}

function buildFormKey(objectCode = '') {
  return objectCode ? `${objectCode}_default_form` : 'default_form'
}

function normalizeFormUsage(value) {
  const usage = Array.isArray(value) ? value : []
  const normalized = usage
    .map(item => String(item || '').trim())
    .filter(Boolean)
  return normalized.length ? Array.from(new Set(normalized)) : ['create', 'edit']
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}
