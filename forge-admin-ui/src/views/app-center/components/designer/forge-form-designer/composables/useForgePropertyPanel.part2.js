/** Auto-split part 2 of ForgePropertyPanel setup. */
import { NTag } from 'naive-ui'
import { computed, h, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList, codeRuleList, previewCodeRule } from '@/api/business-app'
import { serializeSelectionValues } from '@/components/ai-form/selection-multi-value'
import { resolveStorageTypeFromOptionValueMeta } from '@/components/ai-form/option-source-runtime'
import IconRenderer from '@/components/IconRenderer.vue'
import { getValidationPreset } from '@/utils/validation-presets'
import { FIELD_COMPONENT_DEFAULTS as componentFieldDefaults } from '../../form-first/fieldComponentCatalog'
import { camelToSnake } from '../../form-first/namingUtils'
import { resolveChildRelationTargetObjectCode } from '../option-source-picker'

export function useForgePropertyPanelPart2(props, emit, deps = {}) {
  const {
    __impl,
    mut,
    designerStore,
    message,
    fieldFormulaPanelVisible,
    propertyActiveTab,
    formPropertyActiveTab,
    selectedBasicExpandedNames,
    formBasicExpandedNames,
    formStyleExpandedNames,
    allSelectedBasicExpandNames,
    allFormBasicExpandNames,
    allFormStyleExpandNames,
    propertySearchHit,
    codeRules,
    codeRuleLoading,
    codeRulePreview,
    codeRulePreviewing,
    selectedComponent,
    isField,
    fieldStructureLocked,
    formGovernanceSettings,
    formPermissionConfig,
    formFieldRuleRows,
    formEventRows,
    formOfflineDraftConfig,
    selectedFieldCode,
    selectedFieldAsset,
    switchableComponentGroups,
    formEventHookOptions,
    formEventActionOptions,
    childTableRelationOptions,
    childTableTargetObjectCode,
    propertySearchIndex,
    isTreeOptionField,
    defaultOptionPageSize,
    selectedOptionSourceType,
    querySourceMetaFields,
    loadQuerySourceMeta,
    selectedGenerationConfig,
    selectedGenerationRuleCode,
    isObjectReferenceField,
    isRecordSelectorField,
    referenceObjectCode,
    referenceDisplayField,
    referenceValueField,
    referenceFieldMappings,
    recordSelectorConfigObj,
    recordSelectorObjectCode,
    businessObjectOptions,
    businessObjectLoading,
    referenceTargetFieldsMap,
    recordSelectorTargetFieldOptions,
  } = deps
const addFirstCrudExpandDescriptionField = (...args) => __impl.addFirstCrudExpandDescriptionField(...args)
const addInteractionPreset = (...args) => __impl.addInteractionPreset(...args)
const addInteractionRule = (...args) => __impl.addInteractionRule(...args)
const addLayoutChild = (...args) => __impl.addLayoutChild(...args)
const addOption = (...args) => __impl.addOption(...args)
const applySchemaCode = (...args) => __impl.applySchemaCode(...args)
const applySelectedCode = (...args) => __impl.applySelectedCode(...args)
const applySourceModalCode = (...args) => __impl.applySourceModalCode(...args)
const applyStyleValue = (...args) => __impl.applyStyleValue(...args)
const buildDefaultCrudApiConfig = (...args) => __impl.buildDefaultCrudApiConfig(...args)
const cancelSourceModalEdit = (...args) => __impl.cancelSourceModalEdit(...args)
const collectBoundFieldOptions = (...args) => __impl.collectBoundFieldOptions(...args)
const collectComponentTargetOptions = (...args) => __impl.collectComponentTargetOptions(...args)
const collectCrudConfigFields = (...args) => __impl.collectCrudConfigFields(...args)
const collectDictTypeFields = (...args) => __impl.collectDictTypeFields(...args)
const collectDrivenRuntimeRules = (...args) => __impl.collectDrivenRuntimeRules(...args)
const colorToHexInput = (...args) => __impl.colorToHexInput(...args)
const createColumn = (...args) => __impl.createColumn(...args)
const createDefaultCrudExpandPanel = (...args) => __impl.createDefaultCrudExpandPanel(...args)
const formatDefaultValueOptionLabel = (...args) => __impl.formatDefaultValueOptionLabel(...args)
const formatTranslateStyle = (...args) => __impl.formatTranslateStyle(...args)
const handleCrudExpandDescriptionPanelReorder = (...args) => __impl.handleCrudExpandDescriptionPanelReorder(...args)
const handleOpenSourcePanel = (...args) => __impl.handleOpenSourcePanel(...args)
const hasAncestorComponent = (...args) => __impl.hasAncestorComponent(...args)
const hexInputToColor = (...args) => __impl.hexInputToColor(...args)
const loadDefaultValueDictOptions = (...args) => __impl.loadDefaultValueDictOptions(...args)
const moveLayoutChild = (...args) => __impl.moveLayoutChild(...args)
const normalizeApiBase = (...args) => __impl.normalizeApiBase(...args)
const normalizeAppearanceColor = (...args) => __impl.normalizeAppearanceColor(...args)
const normalizeCrudExpandPanelPatch = (...args) => __impl.normalizeCrudExpandPanelPatch(...args)
const normalizeDefaultValueOptions = (...args) => __impl.normalizeDefaultValueOptions(...args)
const normalizeOpacityPercent = (...args) => __impl.normalizeOpacityPercent(...args)
const normalizePositionNumber = (...args) => __impl.normalizePositionNumber(...args)
const openCrudFieldDrawer = (...args) => __impl.openCrudFieldDrawer(...args)
const parseJsonObjectProp = (...args) => __impl.parseJsonObjectProp(...args)
const parseStyleText = (...args) => __impl.parseStyleText(...args)
const parseTranslateStyle = (...args) => __impl.parseTranslateStyle(...args)
const removeFirstCrudExpandDescriptionField = (...args) => __impl.removeFirstCrudExpandDescriptionField(...args)
const removeInteractionRule = (...args) => __impl.removeInteractionRule(...args)
const removeLayoutChild = (...args) => __impl.removeLayoutChild(...args)
const removeOption = (...args) => __impl.removeOption(...args)
const replaceComponentInSchema = (...args) => __impl.replaceComponentInSchema(...args)
const resetSchemaCodeDraft = (...args) => __impl.resetSchemaCodeDraft(...args)
const resetSelectedCodeDraft = (...args) => __impl.resetSelectedCodeDraft(...args)
const resolveActionLabel = (...args) => __impl.resolveActionLabel(...args)
const resolveActionValueMeta = (...args) => __impl.resolveActionValueMeta(...args)
const resolveCrudApiMethodClass = (...args) => __impl.resolveCrudApiMethodClass(...args)
const resolveCrudApiMethodLabel = (...args) => __impl.resolveCrudApiMethodLabel(...args)
const resolveCrudBitableFieldIconComponent = (...args) => __impl.resolveCrudBitableFieldIconComponent(...args)
const resolveCrudExpandDescriptionFieldKeys = (...args) => __impl.resolveCrudExpandDescriptionFieldKeys(...args)
const resolveCrudExpandDescriptionPanelFields = (...args) => __impl.resolveCrudExpandDescriptionPanelFields(...args)
const resolveCrudExpandDescriptionSelectedFields = (...args) => __impl.resolveCrudExpandDescriptionSelectedFields(...args)
const resolveCrudFieldLabel = (...args) => __impl.resolveCrudFieldLabel(...args)
const resolveCrudFieldRoles = (...args) => __impl.resolveCrudFieldRoles(...args)
const resolveCrudSwitchValue = (...args) => __impl.resolveCrudSwitchValue(...args)
const resolveDefaultCrudDescriptionFields = (...args) => __impl.resolveDefaultCrudDescriptionFields(...args)
const resolvePxNumber = (...args) => __impl.resolvePxNumber(...args)
const resolveTriggerLabel = (...args) => __impl.resolveTriggerLabel(...args)
const runtimeRuleDependsOnField = (...args) => __impl.runtimeRuleDependsOnField(...args)
const stringifyJsonProp = (...args) => __impl.stringifyJsonProp(...args)
const stringifyStyle = (...args) => __impl.stringifyStyle(...args)
const summarizeDrivenRuntimeRule = (...args) => __impl.summarizeDrivenRuntimeRule(...args)
const toCamelCase = (...args) => __impl.toCamelCase(...args)
const toKebabCase = (...args) => __impl.toKebabCase(...args)
const toggleFirstCrudExpandDescriptionField = (...args) => __impl.toggleFirstCrudExpandDescriptionField(...args)
const toggleOptionDisabled = (...args) => __impl.toggleOptionDisabled(...args)
const updateComponentStyleText = (...args) => __impl.updateComponentStyleText(...args)
const updateCrudApiBase = (...args) => __impl.updateCrudApiBase(...args)
const updateCrudApiConfig = (...args) => __impl.updateCrudApiConfig(...args)
const updateCrudExpandConfig = (...args) => __impl.updateCrudExpandConfig(...args)
const updateCrudExpandEnabled = (...args) => __impl.updateCrudExpandEnabled(...args)
const updateCrudFieldConfig = (...args) => __impl.updateCrudFieldConfig(...args)
const updateCrudFieldConfigById = (...args) => __impl.updateCrudFieldConfigById(...args)
const updateCrudFieldRole = (...args) => __impl.updateCrudFieldRole(...args)
const updateCrudOption = (...args) => __impl.updateCrudOption(...args)
const updateDefaultValue = (...args) => __impl.updateDefaultValue(...args)
const updateDesignerBorderColor = (...args) => __impl.updateDesignerBorderColor(...args)
const updateDesignerBorderStyle = (...args) => __impl.updateDesignerBorderStyle(...args)
const updateDesignerCustomStyle = (...args) => __impl.updateDesignerCustomStyle(...args)
const updateDesignerSpacing = (...args) => __impl.updateDesignerSpacing(...args)
const updateDesignerStyle = (...args) => __impl.updateDesignerStyle(...args)
const updateDesignerTranslate = (...args) => __impl.updateDesignerTranslate(...args)
const updateEditingCrudFieldConfig = (...args) => __impl.updateEditingCrudFieldConfig(...args)
const updateFirstCrudExpandDataSource = (...args) => __impl.updateFirstCrudExpandDataSource(...args)
const updateFirstCrudExpandDescriptionFields = (...args) => __impl.updateFirstCrudExpandDescriptionFields(...args)
const updateFirstCrudExpandPanel = (...args) => __impl.updateFirstCrudExpandPanel(...args)
const updateFirstCrudExpandParamsMap = (...args) => __impl.updateFirstCrudExpandParamsMap(...args)
const updateFormAppearanceBackground = (...args) => __impl.updateFormAppearanceBackground(...args)
const updateFormAppearanceBorder = (...args) => __impl.updateFormAppearanceBorder(...args)
const updateFormAppearanceOpacity = (...args) => __impl.updateFormAppearanceOpacity(...args)
const updateFormFieldLinkages = (...args) => __impl.updateFormFieldLinkages(...args)
const updateFormLayout = (...args) => __impl.updateFormLayout(...args)
const updateFormSpacing = (...args) => __impl.updateFormSpacing(...args)
const updateFormStyle = (...args) => __impl.updateFormStyle(...args)
const updateFormStyleText = (...args) => __impl.updateFormStyleText(...args)
const updateFormTranslate = (...args) => __impl.updateFormTranslate(...args)
const updateHeightMode = (...args) => __impl.updateHeightMode(...args)
const updateInteractionRule = (...args) => __impl.updateInteractionRule(...args)
const updateInteractionRuleJson = (...args) => __impl.updateInteractionRuleJson(...args)
const updateLabel = (...args) => __impl.updateLabel(...args)
const updateLayoutChild = (...args) => __impl.updateLayoutChild(...args)
const updateOption = (...args) => __impl.updateOption(...args)
const updateOptionJsonProps = (...args) => __impl.updateOptionJsonProps(...args)
const updateOptionLabel = (...args) => __impl.updateOptionLabel(...args)
const updateRowCellCount = (...args) => __impl.updateRowCellCount(...args)
const updateRowColumnSpan = (...args) => __impl.updateRowColumnSpan(...args)
const updateRowTotalColumns = (...args) => __impl.updateRowTotalColumns(...args)
const updateSchemaCodeDraft = (...args) => __impl.updateSchemaCodeDraft(...args)
const updateSelectedAppearanceBackground = (...args) => __impl.updateSelectedAppearanceBackground(...args)
const updateSelectedAppearanceBorder = (...args) => __impl.updateSelectedAppearanceBorder(...args)
const updateSelectedAppearanceOpacity = (...args) => __impl.updateSelectedAppearanceOpacity(...args)
const updateSelectedCodeDraft = (...args) => __impl.updateSelectedCodeDraft(...args)
const updateWidthMode = (...args) => __impl.updateWidthMode(...args)
const valueToPx = (...args) => __impl.valueToPx(...args)
function updateComponent(patch) {
  // 必须基于 store 最新 schema 合并。若仍用尚未回传的 props.schema 连续 emit，
  // 后一次写入会覆盖前一次（样式字号/颜色/边距连改会丢 customStyle）。
  const componentId = designerStore.selectedId || props.selectedId
  if (!componentId)
    return
  designerStore.updateComponent(componentId, patch)
}
__impl.updateComponent = updateComponent

function pickSwitchableCommonProps(sourceProps = {}, newKey = '') {
  const commonKeys = ['defaultValue', 'placeholder', 'disabled', 'clearable', 'required', 'dictType', 'multiple']
  const nextProps = {}
  commonKeys.forEach((key) => {
    if (sourceProps[key] !== undefined)
      nextProps[key] = sourceProps[key]
  })
  if (['input', 'textarea'].includes(newKey)) {
    if (sourceProps.maxlength !== undefined)
      nextProps.maxlength = sourceProps.maxlength
    if (sourceProps.showCount !== undefined)
      nextProps.showCount = sourceProps.showCount
  }
  else {
    nextProps.maxlength = undefined
    nextProps.showCount = undefined
  }
  return nextProps
}
__impl.pickSwitchableCommonProps = pickSwitchableCommonProps

function handleSwitchComponentType(newKey) {
  const component = selectedComponent.value
  if (fieldStructureLocked.value || !component || !newKey || newKey === component.componentKey)
    return
  const group = switchableComponentGroups.find(item => item.includes(component.componentKey))
  if (!group?.includes(newKey))
    return
  const defaults = componentFieldDefaults[newKey] || componentFieldDefaults.input
  const nextProps = pickSwitchableCommonProps(component.props || {}, newKey)
  const nextFieldBinding = {
    ...(component.fieldBinding || {}),
    fieldType: defaults.fieldType,
    dataType: defaults.dataType,
    componentType: defaults.componentType || newKey,
  }
  updateComponent({
    componentKey: newKey,
    props: nextProps,
    fieldBinding: nextFieldBinding,
  })
}
__impl.handleSwitchComponentType = handleSwitchComponentType

function updateComponentHidden(value) {
  const hidden = value === true
  updateComponent({
    visibility: { hidden },
    ...(hidden
      ? {
          validation: {
            required: false,
            requiredMessage: '',
          },
        }
      : {}),
  })
  if (selectedGenerationConfig.value?.enabled === true)
    emitFieldGenerationUpdate(selectedGenerationConfig.value, { hideOnForm: hidden })
}
__impl.updateComponentHidden = updateComponentHidden

function updateValidationPreset(value) {
  const preset = getValidationPreset(value)
  const nextValidation = {
    ...(selectedComponent.value?.validation || {}),
    preset: value || '',
    pattern: value ? preset?.pattern || '' : undefined,
    message: value ? preset?.message || '' : undefined,
  }
  if (!value) {
    nextValidation.pattern = ''
    nextValidation.message = ''
  }
  updateComponent({ validation: nextValidation })
}
__impl.updateValidationPreset = updateValidationPreset

function normalizePositiveInteger(value) {
  const number = Number(value)
  if (!Number.isFinite(number) || number <= 0)
    return null
  return Math.floor(number)
}
__impl.normalizePositiveInteger = normalizePositiveInteger

function updateFieldMaxLength(value) {
  const maxLength = normalizePositiveInteger(value)
  updateComponent({
    props: {
      maxlength: maxLength || undefined,
      showCount: maxLength ? true : undefined,
    },
  })
}
__impl.updateFieldMaxLength = updateFieldMaxLength

function openFieldFormulaPanel() {
  if (!selectedFieldAsset.value)
    return
  fieldFormulaPanelVisible.value = true
}
__impl.openFieldFormulaPanel = openFieldFormulaPanel

function handleFieldAssetSave(payload = {}) {
  const fieldCode = payload.fieldCode || selectedFieldCode.value
  if (!fieldCode)
    return
  const formulaConfig = payload.formulaConfig ?? null
  updateComponent(buildFieldAssetComponentPatch(payload, formulaConfig, fieldCode))
  emit('fieldAssetUpdated', {
    ...payload,
    fieldCode,
    formulaConfig,
  })
  fieldFormulaPanelVisible.value = false
}
__impl.handleFieldAssetSave = handleFieldAssetSave

function buildFieldAssetComponentPatch(payload = {}, formulaConfig = null, fieldCode = '') {
  const propsPatch = {
    formulaConfig,
    ...buildFieldAssetPlaceholderPatch(selectedComponent.value || {}, payload),
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'defaultValue'))
    propsPatch.defaultValue = payload.defaultValue
  if (Object.prototype.hasOwnProperty.call(payload, 'dictType'))
    propsPatch.dictType = payload.dictType || ''
  if (Object.prototype.hasOwnProperty.call(payload, 'referenceObjectCode'))
    propsPatch.referenceObjectCode = payload.referenceObjectCode || ''
  if (Object.prototype.hasOwnProperty.call(payload, 'referenceDisplayField'))
    propsPatch.referenceDisplayField = payload.referenceDisplayField || ''
  if (payload.basicProps && Object.prototype.hasOwnProperty.call(payload.basicProps, 'recordSelector'))
    propsPatch.recordSelector = payload.basicProps.recordSelector || undefined

  const payloadValidation = payload.validation && typeof payload.validation === 'object'
    ? { ...payload.validation }
    : null
  const patch = {
    label: payload.fieldName || selectedComponent.value?.label || fieldCode,
    props: propsPatch,
    advancedProps: {
      ...(selectedComponent.value?.advancedProps || {}),
      ...(payload.advancedProps || {}),
      formulaConfig,
    },
  }
  if (payloadValidation) {
    patch.validation = {
      ...(selectedComponent.value?.validation || {}),
      ...payloadValidation,
    }
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'required')) {
    patch.validation = {
      ...(patch.validation || selectedComponent.value?.validation || {}),
      required: Boolean(payload.required),
      requiredMessage: payload.required ? `${payload.fieldName || selectedComponent.value?.label || fieldCode}不能为空` : '',
    }
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'formVisible') || Object.prototype.hasOwnProperty.call(payload, 'readonly')) {
    patch.visibility = {
      ...(Object.prototype.hasOwnProperty.call(payload, 'formVisible') ? { hidden: payload.formVisible === false } : {}),
      ...(Object.prototype.hasOwnProperty.call(payload, 'readonly') ? { readonly: Boolean(payload.readonly) } : {}),
    }
  }
  return patch
}
__impl.buildFieldAssetComponentPatch = buildFieldAssetComponentPatch

function normalizeSelectedFieldAsset(source = {}) {
  const component = selectedComponent.value || {}
  const fieldCode = source.fieldCode || source.field || selectedFieldCode.value
  const formulaConfig = source.formulaConfig ?? component.props?.formulaConfig ?? component.advancedProps?.formulaConfig ?? null
  return {
    ...source,
    field: source.field || fieldCode,
    fieldCode,
    fieldName: source.fieldName || source.label || component.label || fieldCode,
    columnName: source.columnName || component.fieldBinding?.columnName || camelToSnake(fieldCode),
    validation: source.validation || component.validation || {},
    formulaConfig,
  }
}
__impl.normalizeSelectedFieldAsset = normalizeSelectedFieldAsset

function createFieldAssetFromSelectedComponent() {
  const component = selectedComponent.value || {}
  const fieldCode = selectedFieldCode.value
  const defaults = componentFieldDefaults[component.componentKey] || componentFieldDefaults.input
  const fieldBinding = {
    mode: 'field',
    fieldCode,
    columnName: component.fieldBinding?.columnName || camelToSnake(fieldCode),
    createIfMissing: true,
    source: 'designer',
    locked: false,
    ...(component.fieldBinding || {}),
  }
  return {
    field: fieldCode,
    fieldName: component.label || fieldCode,
    fieldCode,
    columnName: fieldBinding.columnName,
    fieldType: defaults.fieldType,
    dataType: defaults.dataType,
    length: defaults.length,
    precision: defaults.precision,
    required: Boolean(component.validation?.required),
    defaultValue: component.props?.defaultValue ?? '',
    searchable: false,
    listVisible: true,
    formVisible: component.visibility?.hidden !== true,
    importable: true,
    exportable: true,
    componentType: defaults.componentType,
    queryType: defaults.queryType,
    dictType: component.props?.dictType || '',
    sortable: false,
    systemField: false,
    readonly: Boolean(component.visibility?.readonly),
    fieldStatus: 'ENABLED',
    referenceObjectCode: component.props?.referenceObjectCode || '',
    referenceDisplayField: component.props?.referenceDisplayField || '',
    validation: component.validation || {},
    placeholder: component.props?.placeholder || '',
    remark: component.label || '',
    sortOrder: Number(component.props?.sortOrder ?? component.layout?.order ?? 0),
    fieldBinding,
    basicProps: {
      ...(component.props || {}),
      fieldBinding,
    },
    advancedProps: {
      ...(component.advancedProps || {}),
    },
    formulaConfig: component.props?.formulaConfig ?? component.advancedProps?.formulaConfig ?? null,
  }
}
__impl.createFieldAssetFromSelectedComponent = createFieldAssetFromSelectedComponent

function resolveSelectedFieldCode() {
  const component = selectedComponent.value || {}
  return String(
    component.fieldBinding?.fieldCode
    || component.field
    || component.props?.fieldCode
    || component.props?.field
    || '',
  ).trim()
}
__impl.resolveSelectedFieldCode = resolveSelectedFieldCode

function resolveSelectionLabelValueField(fieldCode = resolveSelectedFieldCode()) {
  const explicit = String(selectedComponent.value?.props?.labelValueField || '').trim()
  if (explicit)
    return explicit
  return fieldCode ? `${fieldCode}Name` : ''
}
__impl.resolveSelectionLabelValueField = resolveSelectionLabelValueField

function withTreeOptionSourceDefaults(patch = {}) {
  if (!isTreeOptionField.value)
    return patch
  const previous = selectedComponent.value?.props?.optionSource || {}
  return {
    ...createDefaultTreeOptionSourcePatch(previous),
    ...patch,
    pageSize: patch.pageSize ?? previous.pageSize ?? DEFAULT_TREE_OPTION_PAGE_SIZE,
  }
}
__impl.withTreeOptionSourceDefaults = withTreeOptionSourceDefaults

function updateTreeOptionSourceMapping(patch = {}) {
  updatePageWidgetOptionSource(withTreeOptionSourceDefaults({
    ...patch,
    structure: 'tree',
  }))
}
__impl.updateTreeOptionSourceMapping = updateTreeOptionSourceMapping

function updatePageWidgetOptionSource(patch = {}) {
  const optionSource = {
    ...(selectedComponent.value?.props?.optionSource || {}),
    ...patch,
  }
  const propsPatch = { optionSource }
  const type = String(optionSource.type || '').toUpperCase()
  const dynamic = type && type !== 'STATIC'
  if (dynamic) {
    const labelValueField = resolveSelectionLabelValueField()
    if (labelValueField)
      propsPatch.labelValueField = labelValueField
    // 切到动态源时清掉静态「选项1/选项2」，避免运行态/预览仍优先渲染残留 options
    propsPatch.options = []
  }
  else if (Object.prototype.hasOwnProperty.call(patch, 'type') && type === 'STATIC') {
    // 切回静态选项时清掉伴随字段绑定，避免残留误导
    propsPatch.labelValueField = ''
  }
  updateComponent({ props: propsPatch })
  if (Object.prototype.hasOwnProperty.call(patch, 'valueField')
    || Object.prototype.hasOwnProperty.call(patch, 'sourceKey')) {
    syncFieldStorageFromOptionValueField(optionSource.valueField || 'id')
  }
}
__impl.updatePageWidgetOptionSource = updatePageWidgetOptionSource

/**
 * 动态选项的值字段决定本表存什么类型：必须与目标对象/查询源的值字段类型一致。
 * 已有业务数据锁结构时不改。
 */
function resolveOptionValueFieldMeta(valueField = '') {
  const field = String(valueField || '').trim()
  if (!field)
    return null
  const fromQueryMeta = (querySourceMetaFields.value || []).find(item => String(item?.value || '').trim() === field)
  if (fromQueryMeta?.dataType || fromQueryMeta?.type)
    return fromQueryMeta

  // 业务对象：优先用设计器字段资产上的真实 dataType（比查询源元数据更完整）
  const objectCode = selectedComponent.value?.props?.optionSource?.sourceKey
    || (selectedOptionSourceType.value === 'CURRENT_CHILDREN' ? childTableTargetObjectCode.value : '')
  if (objectCode) {
    const options = referenceTargetFieldsMap.value[objectCode]?.options || []
    const matched = options.find(item => String(item?.value || '').trim() === field)
    const sourceField = matched?.field
    if (sourceField && (sourceField.dataType || sourceField.type)) {
      return {
        field,
        value: field,
        dataType: sourceField.dataType || sourceField.type,
        length: sourceField.length,
        precision: sourceField.precision,
      }
    }
  }
  return fromQueryMeta || { field, value: field }
}
__impl.resolveOptionValueFieldMeta = resolveOptionValueFieldMeta

function syncFieldStorageFromOptionValueField(valueField = '') {
  if (fieldStructureLocked.value || !isField.value || !selectedFieldCode.value)
    return
  const field = String(valueField || '').trim()
  if (!field)
    return
  const meta = resolveOptionValueFieldMeta(field)
  const storage = resolveStorageTypeFromOptionValueMeta(meta || {}, field)
  if (!storage?.dataType)
    return
  const current = selectedFieldAsset.value || {}
  const sameType = String(current.dataType || '').toLowerCase() === storage.dataType
  const sameLength = (current.length ?? null) === (storage.length ?? null)
  const samePrecision = (current.precision ?? null) === (storage.precision ?? null)
  if (sameType && sameLength && samePrecision)
    return
  emit('fieldAssetUpdated', {
    ...current,
    fieldCode: selectedFieldCode.value,
    field: selectedFieldCode.value,
    fieldType: current.fieldType || (isTreeOptionField.value ? 'SELECT' : current.fieldType),
    dataType: storage.dataType,
    length: storage.length,
    precision: storage.precision,
    componentType: selectedComponent.value?.componentKey || current.componentType,
  })
}
__impl.syncFieldStorageFromOptionValueField = syncFieldStorageFromOptionValueField

function updateOptionSourceType(type = 'STATIC') {
  if (type === 'CURRENT_CHILDREN') {
    updatePageWidgetOptionSource(withTreeOptionSourceDefaults({
      type: 'CURRENT_CHILDREN',
      api: undefined,
      url: undefined,
      sourceType: undefined,
      sourceKey: undefined,
      relationKey: selectedComponent.value?.props?.optionSource?.relationKey || '',
      valueField: selectedComponent.value?.props?.optionSource?.valueField || 'id',
      labelField: selectedComponent.value?.props?.optionSource?.labelField || 'label',
      persistedOnly: selectedComponent.value?.props?.optionSource?.persistedOnly !== false,
    }))
    return
  }
  if (type === 'BUSINESS_OBJECT') {
    const previous = selectedComponent.value?.props?.optionSource || {}
    const keep = String(previous.type || '') === 'QUERY_SOURCE'
      && String(previous.sourceType || '').toUpperCase() === 'BUSINESS_OBJECT'
    updatePageWidgetOptionSource(withTreeOptionSourceDefaults({
      type: 'QUERY_SOURCE',
      api: undefined,
      url: undefined,
      relationKey: undefined,
      sourceType: 'BUSINESS_OBJECT',
      sourceKey: keep ? previous.sourceKey : '',
      valueField: keep ? (previous.valueField || 'id') : 'id',
      labelField: keep ? (previous.labelField || 'name') : 'name',
      paramsText: keep ? (previous.paramsText || '{}') : '{}',
      pageSize: keep
        ? (previous.pageSize || defaultOptionPageSize.value)
        : defaultOptionPageSize.value,
    }))
    ensureOptionSourceFieldCatalog()
    return
  }
  if (type === 'REMOTE') {
    updatePageWidgetOptionSource(withTreeOptionSourceDefaults({
      type: 'REMOTE',
      api: selectedComponent.value?.props?.optionSource?.api || '',
      pageSize: selectedComponent.value?.props?.optionSource?.pageSize || defaultOptionPageSize.value,
    }))
    return
  }
  if (type === 'QUERY_SOURCE') {
    const previous = selectedComponent.value?.props?.optionSource || {}
    const wasQuerySource = String(previous.type || '') === 'QUERY_SOURCE'
      && String(previous.sourceType || '').toUpperCase() !== 'BUSINESS_OBJECT'
    updatePageWidgetOptionSource(withTreeOptionSourceDefaults({
      type: 'QUERY_SOURCE',
      api: undefined,
      url: undefined,
      relationKey: undefined,
      sourceType: wasQuerySource ? previous.sourceType : '',
      sourceKey: wasQuerySource ? previous.sourceKey : '',
      valueField: wasQuerySource ? previous.valueField : 'id',
      labelField: wasQuerySource ? previous.labelField : 'name',
      paramsText: wasQuerySource ? previous.paramsText : '{}',
      pageSize: wasQuerySource
        ? (previous.pageSize || defaultOptionPageSize.value)
        : defaultOptionPageSize.value,
    }))
    return
  }
  updateComponent({ props: { optionSource: undefined, labelValueField: '' } })
}
__impl.updateOptionSourceType = updateOptionSourceType

function updateCurrentChildrenRelationKey(value) {
  updatePageWidgetOptionSource({ relationKey: value || '' })
  const objectCode = resolveChildRelationTargetObjectCode(value, childTableRelationOptions.value)
  if (!objectCode)
    return
  loadBusinessObjectOptions().then(() => loadReferenceTargetFields(objectCode))
}
__impl.updateCurrentChildrenRelationKey = updateCurrentChildrenRelationKey

async function updateBusinessObjectOptionSource(value) {
  const objectCode = value || ''
  if (objectCode && !businessObjectOptions.value.length)
    await loadBusinessObjectOptions()
  const target = businessObjectOptions.value.find(item => item.value === objectCode)
  if (objectCode && target && !target.runtimePublished)
    message.warning(`「${target.label}」尚未发布运行配置：设计预览可正常选数据，正式运行前请先发布其所在应用`)
  updatePageWidgetOptionSource({
    type: 'QUERY_SOURCE',
    sourceType: 'BUSINESS_OBJECT',
    sourceKey: objectCode,
    valueField: selectedComponent.value?.props?.optionSource?.valueField || 'id',
    labelField: selectedComponent.value?.props?.optionSource?.labelField || 'name',
  })
  if (objectCode) {
    await Promise.all([
      loadQuerySourceMeta('BUSINESS_OBJECT', objectCode),
      loadReferenceTargetFields(objectCode, true),
    ])
    syncFieldStorageFromOptionValueField(
      selectedComponent.value?.props?.optionSource?.valueField || 'id',
    )
  }
}
__impl.updateBusinessObjectOptionSource = updateBusinessObjectOptionSource

async function ensureOptionSourceFieldCatalog() {
  const type = selectedOptionSourceType.value
  if (type === 'BUSINESS_OBJECT') {
    await loadBusinessObjectOptions()
    const objectCode = selectedComponent.value?.props?.optionSource?.sourceKey
    if (objectCode) {
      await Promise.all([
        loadQuerySourceMeta('BUSINESS_OBJECT', objectCode),
        loadReferenceTargetFields(objectCode),
      ])
      syncFieldStorageFromOptionValueField(
        selectedComponent.value?.props?.optionSource?.valueField || 'id',
      )
    }
    return
  }
  if (type === 'CURRENT_CHILDREN') {
    const objectCode = childTableTargetObjectCode.value
    if (!objectCode)
      return
    await loadBusinessObjectOptions()
    await loadReferenceTargetFields(objectCode)
    syncFieldStorageFromOptionValueField(
      selectedComponent.value?.props?.optionSource?.valueField || 'id',
    )
  }
}
__impl.ensureOptionSourceFieldCatalog = ensureOptionSourceFieldCatalog

function updatePageWidgetDataBinding(patch = {}) {
  const next = {
    ...(selectedComponent.value?.props?.dataBinding || {}),
    ...patch,
  }
  next.enabled = next.sourceType !== 'static'
  updateComponent({
    props: {
      dataBinding: next,
    },
  })
}
__impl.updatePageWidgetDataBinding = updatePageWidgetDataBinding

const widgetParamRefName = ref('')
const widgetParamRefField = ref('')

// 把「参数名 = 某字段的当前值」合并进请求参数 JSON：值写 ${字段} 占位，运行时取表单当前值并随值变化自动重查
function addWidgetParamRef() {
  const paramName = String(widgetParamRefName.value || '').trim()
  const fieldName = String(widgetParamRefField.value || '').trim()
  if (!paramName || !fieldName)
    return
  let current = {}
  try {
    const parsed = JSON.parse(selectedComponent.value?.props?.dataBinding?.paramsText || '{}')
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed))
      current = parsed
  }
  catch {
    current = {}
  }
  current[paramName] = `\${${fieldName}}`
  updatePageWidgetDataBinding({ paramsText: JSON.stringify(current, null, 2) })
  widgetParamRefName.value = ''
  widgetParamRefField.value = ''
}
__impl.addWidgetParamRef = addWidgetParamRef

function resolveBooleanKeys(source = {}, keys = []) {
  return keys.filter(key => source?.[key] === true)
}
__impl.resolveBooleanKeys = resolveBooleanKeys

function updateCodeColor(value = '') {
  if (selectedComponent.value?.componentKey === 'barcode') {
    updateComponent({ props: { lineColor: value || '#0f172a' } })
    return
  }
  updateComponent({ props: { foreground: value || '#0f172a' } })
}
__impl.updateCodeColor = updateCodeColor

function updateDictType(value = '') {
  loadDefaultValueDictOptions(value)
  updateComponent({
    props: {
      dictType: value || '',
      options: [],
      defaultValue: undefined,
    },
  })
}
__impl.updateDictType = updateDictType

async function loadCodeRuleOptions() {
  if (codeRules.value.length || codeRuleLoading.value)
    return
  const requestVersion = ++mut.codeRuleRequestVersion
  const requestedObjectCode = props.objectCode || ''
  codeRuleLoading.value = true
  try {
    const res = await codeRuleList({ scene: 'COMMON', objectCode: requestedObjectCode || undefined })
    if (requestVersion === mut.codeRuleRequestVersion && requestedObjectCode === (props.objectCode || ''))
      codeRules.value = Array.isArray(res.data) ? res.data : []
  }
  finally {
    if (requestVersion === mut.codeRuleRequestVersion)
      codeRuleLoading.value = false
  }
}
__impl.loadCodeRuleOptions = loadCodeRuleOptions

async function loadBusinessObjectOptions() {
  if (businessObjectOptions.value.length || businessObjectLoading.value)
    return
  businessObjectLoading.value = true
  try {
    const res = await businessObjectList({})
    const list = Array.isArray(res.data) ? res.data : []
    const seen = new Set()
    businessObjectOptions.value = list
      .filter((item) => {
        if (!item.objectCode || seen.has(item.objectCode))
          return false
        seen.add(item.objectCode)
        return true
      })
      .map(item => ({
        label: item.objectName || item.objectCode,
        value: item.objectCode,
        icon: item.icon || '',
        // 无运行配置的对象任何场景都查不到数据，直接禁选；
        // 已生成配置但未发布的仅设计预览可用（预览请求会带 designPreview 放行草稿配置）
        disabled: !item.configKey,
        runtimePublished: Boolean(item.lastPublishTime),
        object: item,
      }))
  }
  catch {
    businessObjectOptions.value = []
  }
  finally {
    businessObjectLoading.value = false
  }
}
__impl.loadBusinessObjectOptions = loadBusinessObjectOptions

/**
 * 引用对象下拉选项：图标 + 名称 + 发布状态标签，未生成运行配置的禁选。
 * 下拉菜单经 Teleport 渲染，scoped 样式无法命中，这里用内联样式。
 */
function renderReferenceObjectLabel(option) {
  const tag = option.disabled
    ? { type: 'default', label: '未生成配置' }
    : option.runtimePublished
      ? { type: 'success', label: '已发布' }
      : { type: 'warning', label: '未发布' }
  return h('div', {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: '6px',
      minWidth: 0,
    },
  }, [
    h(IconRenderer, { icon: option.icon, size: 15 }),
    h('span', {
      style: {
        flex: '1',
        minWidth: '0',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      },
    }, option.label),
    h(NTag, { size: 'small', bordered: false, type: tag.type, style: { flexShrink: '0' } }, { default: () => tag.label }),
  ])
}
__impl.renderReferenceObjectLabel = renderReferenceObjectLabel

async function loadReferenceTargetFields(objectCode, force = false) {
  if (!objectCode)
    return
  if (!force && referenceTargetFieldsMap.value[objectCode])
    return
  const target = businessObjectOptions.value.find(item => item.value === objectCode)?.object
  if (!target?.id) {
    referenceTargetFieldsMap.value = {
      ...referenceTargetFieldsMap.value,
      [objectCode]: { loading: false, options: [] },
    }
    return
  }
  referenceTargetFieldsMap.value = {
    ...referenceTargetFieldsMap.value,
    [objectCode]: { loading: true, options: referenceTargetFieldsMap.value[objectCode]?.options || [] },
  }
  try {
    const res = await businessObjectDesigner(target.id)
    const fields = res.data?.fields || res.data?.modelSchema?.fields || []
    const options = fields
      .filter(field => !['tenantId', 'tenant_id', 'createBy', 'create_by', 'createTime', 'create_time', 'updateBy', 'update_by', 'updateTime', 'update_time', 'delFlag', 'del_flag'].includes(field.fieldCode || field.field))
      .map(field => ({
        label: field.fieldName || field.label || field.fieldCode || field.field,
        value: field.fieldCode || field.field,
        field,
      }))
    referenceTargetFieldsMap.value = {
      ...referenceTargetFieldsMap.value,
      [objectCode]: { loading: false, options },
    }
  }
  catch {
    referenceTargetFieldsMap.value = {
      ...referenceTargetFieldsMap.value,
      [objectCode]: { loading: false, options: [] },
    }
  }
}
__impl.loadReferenceTargetFields = loadReferenceTargetFields

watch(selectedOptionSourceType, (type) => {
  if (type === 'BUSINESS_OBJECT' || type === 'CURRENT_CHILDREN')
    ensureOptionSourceFieldCatalog()
})

watch(childTableTargetObjectCode, (objectCode) => {
  if (objectCode && selectedOptionSourceType.value === 'CURRENT_CHILDREN')
    ensureOptionSourceFieldCatalog()
})

async function updateReferenceObjectCode(value) {
  const target = businessObjectOptions.value.find(item => item.value === value)
  if (value && target && !target.runtimePublished)
    message.warning(`「${target.label}」尚未发布运行配置：设计预览可正常选数据，正式运行前请先发布其所在应用`)
  updateComponent({
    props: {
      referenceObjectCode: value || '',
      referenceDisplayField: '',
      referenceValueField: '',
    },
  })
  if (value) {
    if (!businessObjectOptions.value.length)
      await loadBusinessObjectOptions()
    await loadReferenceTargetFields(value, true)
  }
}
__impl.updateReferenceObjectCode = updateReferenceObjectCode

function updateReferenceDisplayField(value) {
  updateComponent({ props: { referenceDisplayField: value || '' } })
}
__impl.updateReferenceDisplayField = updateReferenceDisplayField

function updateReferenceValueField(value) {
  updateComponent({ props: { referenceValueField: value || '' } })
}
__impl.updateReferenceValueField = updateReferenceValueField

function addReferenceFieldMapping() {
  const next = [...referenceFieldMappings.value, { sourceField: '', targetField: '' }]
  persistSelectFieldMappings(next)
}
__impl.addReferenceFieldMapping = addReferenceFieldMapping

function updateReferenceFieldMapping(index, patch = {}) {
  const next = referenceFieldMappings.value.map((item, i) => (
    i === index ? { ...item, ...patch } : item
  ))
  persistSelectFieldMappings(next)
}
__impl.updateReferenceFieldMapping = updateReferenceFieldMapping

function removeReferenceFieldMapping(index) {
  const next = referenceFieldMappings.value.filter((_, i) => i !== index)
  persistSelectFieldMappings(next)
}
__impl.removeReferenceFieldMapping = removeReferenceFieldMapping

/** 下拉选中回填：同时写入 props.fieldMappings，并镜像到 optionSource 供运行态投影源字段 */
function persistSelectFieldMappings(mappings = []) {
  const optionSource = { ...(selectedComponent.value?.props?.optionSource || {}) }
  const cleaned = (Array.isArray(mappings) ? mappings : [])
    .map(item => ({
      sourceField: String(item?.sourceField || item?.source || '').trim(),
      targetField: String(item?.targetField || item?.target || '').trim(),
    }))
  const filled = cleaned.filter(item => item.sourceField && item.targetField)
  const sourceFields = filled.map(item => item.sourceField)
  const labelField = String(optionSource.labelField || '').trim()
  const displayFields = [...new Set([
    ...(labelField ? [labelField] : []),
    ...sourceFields,
  ])]
  updateComponent({
    props: {
      fieldMappings: cleaned,
      optionSource: {
        ...optionSource,
        fieldMappings: filled,
        ...(displayFields.length ? { displayFields } : {}),
      },
    },
  })
}
__impl.persistSelectFieldMappings = persistSelectFieldMappings

// recordSelector 更新函数
function updateRecordSelectorObjectCode(value) {
  const target = businessObjectOptions.value.find(item => item.value === value)
  if (value && target && !target.runtimePublished)
    message.warning(`「${target.label}」尚未发布运行配置：设计预览可正常选数据，正式运行前请先发布其所在应用`)
  const current = { ...(selectedComponent.value?.props?.recordSelector || {}) }
  current.objectCode = value || ''
  updateComponent({ props: { recordSelector: current } })
  if (value) {
    if (!businessObjectOptions.value.length)
      loadBusinessObjectOptions()
    loadReferenceTargetFields(value, true)
  }
}
__impl.updateRecordSelectorObjectCode = updateRecordSelectorObjectCode
function updateRecordSelectorValueField(value) {
  const current = { ...(selectedComponent.value?.props?.recordSelector || {}) }
  current.valueField = value || ''
  updateComponent({ props: { recordSelector: current } })
}
__impl.updateRecordSelectorValueField = updateRecordSelectorValueField

// ── recordSelector 高级配置弹窗 ────────────────────────
const rsConfigDialogShow = ref(false)

/** 将 filterFields（优先）或 searchParams（兼容旧配置）转换为弹窗所需的 filter 数组 */
const rsDialogFilters = computed(() => {
  const filterFields = recordSelectorConfigObj.value.filterFields
  if (Array.isArray(filterFields) && filterFields.length)
    return filterFields.map(f => ({ fieldCode: f.fieldCode || '', fieldLabel: f.fieldLabel || '', defaultType: f.defaultType || 'none', formField: f.formField || '', defaultValue: f.defaultValue || '' }))
  // 兼容旧配置：从 searchParams 还原
  const params = recordSelectorConfigObj.value.searchParams || {}
  return Object.entries(params).map(([key, val]) => {
    const strVal = String(val || '')
    const formMatch = strVal.match(/^\$\{formData\.(.+?)\}$/)
    if (formMatch)
      return { fieldCode: key, fieldLabel: '', defaultType: 'form', formField: formMatch[1], defaultValue: '' }
    if (strVal)
      return { fieldCode: key, fieldLabel: '', defaultType: 'fixed', defaultValue: strVal, formField: '' }
    return { fieldCode: key, fieldLabel: '', defaultType: 'none', defaultValue: '', formField: '' }
  })
})

/** 将 fieldMappings 数组直接传给弹窗（兼容 source/sourceField 两种键名） */
const rsDialogMappings = computed(() => {
  const mappings = recordSelectorConfigObj.value.fieldMappings || []
  return mappings.map(m => ({ source: m.sourceField || m.source || '', target: m.targetField || m.target || '' }))
})

/** 选择器设置摘要 */
const rsConfigSummary = computed(() => {
  const cfg = recordSelectorConfigObj.value
  const mode = cfg.multiple === true ? '多选' : '单选'
  const keywordCount = (cfg.keywordFields || []).length
  const displayCount = (cfg.displayFields || []).length
  const filterFields = cfg.filterFields
  const filterCount = Array.isArray(filterFields) ? filterFields.length : Object.keys(cfg.searchParams || {}).length
  const mappingCount = (cfg.fieldMappings || []).length
  const parts = [mode, `展示 ${displayCount} 列`]
  if (keywordCount)
    parts.push(`搜索 ${keywordCount} 字段`)
  if (filterCount)
    parts.push(`筛选 ${filterCount} 条`)
  if (mappingCount)
    parts.push(`映射 ${mappingCount} 条`)
  return parts.join(' · ')
})

/** 弹窗确认回调：将结构化配置写入 recordSelector */
function handleRsConfigConfirm(config) {
  const current = { ...(selectedComponent.value?.props?.recordSelector || {}) }
  // 选择方式
  current.multiple = config.multiple === true
  // 搜索字段
  current.keywordFields = config.keywordFields || []
  // 展示列
  current.displayFields = config.displayFields || []
  // 从字段选项构建查找表，用于补全 fieldLabel
  const fieldLabelMap = {}
  recordSelectorTargetFieldOptions.value.forEach((opt) => {
    if (opt.value)
      fieldLabelMap[opt.value] = opt.label || opt.value
  })
  // 过滤参数：存储为 filterFields（运行时在弹窗中显示为可编辑筛选 UI）
  current.filterFields = (config.filters || []).filter(f => f.fieldCode).map(f => ({
    fieldCode: f.fieldCode,
    fieldLabel: f.fieldLabel || fieldLabelMap[f.fieldCode] || '',
    defaultType: f.defaultType || 'none',
    formField: f.formField || '',
    defaultValue: f.defaultValue || '',
    // 字段元数据，供运行时搜索表单类型感知渲染
    dictType: f.dictType || '',
    componentType: f.componentType || '',
    fieldType: f.fieldType || '',
  }))
  // 同时生成 searchParams 作为无 UI 的预过滤条件（兼容未读取 filterFields 的场景）
  const searchParams = {}
  current.filterFields.forEach((f) => {
    if (f.defaultType === 'form' && f.formField)
      searchParams[f.fieldCode] = `\${formData.${f.formField}}`
    else if (f.defaultType === 'fixed' && f.defaultValue !== undefined && f.defaultValue !== null && String(f.defaultValue).trim() !== '')
      searchParams[f.fieldCode] = f.defaultValue
  })
  current.searchParams = searchParams
  // 字段映射（后端 DTO 期望 sourceField / targetField 键名）
  current.fieldMappings = (config.mappings || []).filter(m => m.source && m.target).map(m => ({ sourceField: m.source, targetField: m.target }))
  updateComponent({ props: { recordSelector: current, multiple: current.multiple === true } })
}
__impl.handleRsConfigConfirm = handleRsConfigConfirm

function updateMultipleSelect(enabled) {
  const next = enabled === true
  const currentDefault = selectedComponent.value?.props?.defaultValue
  const patch = {
    multiple: next,
    defaultValue: next
      ? (serializeSelectionValues(currentDefault, true) || undefined)
      : normalizeSingleDefaultValue(currentDefault),
  }
  if (isRecordSelectorField.value) {
    patch.recordSelector = {
      ...(selectedComponent.value?.props?.recordSelector || {}),
      multiple: next,
    }
  }
  updateComponent({ props: patch })
}
__impl.updateMultipleSelect = updateMultipleSelect

function normalizeSingleDefaultValue(value) {
  if (Array.isArray(value))
    return value.length ? value[0] : undefined
  if (typeof value === 'string' && value.includes(','))
    return value.split(',')[0].trim() || undefined
  return value === null || value === '' ? undefined : value
}
__impl.normalizeSingleDefaultValue = normalizeSingleDefaultValue

watch(
  () => selectedComponent.value?.componentKey,
  async (key) => {
    if (key === 'objectReference' || key === 'recordSelector') {
      await loadBusinessObjectOptions()
      const objCode = key === 'objectReference' ? referenceObjectCode.value : recordSelectorObjectCode.value
      if (objCode)
        await loadReferenceTargetFields(objCode)
    }
  },
  { immediate: true },
)

watch(
  businessObjectOptions,
  () => {
    if (isObjectReferenceField.value && referenceObjectCode.value && !referenceTargetFieldsMap.value[referenceObjectCode.value])
      loadReferenceTargetFields(referenceObjectCode.value)
    if (isRecordSelectorField.value && recordSelectorObjectCode.value && !referenceTargetFieldsMap.value[recordSelectorObjectCode.value])
      loadReferenceTargetFields(recordSelectorObjectCode.value)
  },
)

function normalizeGenerationConfig(patch = {}) {
  return {
    enabled: true,
    type: 'CODE_RULE',
    mode: 'CODE_RULE',
    ruleCode: selectedGenerationRuleCode.value || codeRules.value[0]?.ruleCode || '',
    trigger: 'ON_CREATE',
    fillPolicy: 'EMPTY_ONLY',
    readonly: true,
    ...(selectedGenerationConfig.value || {}),
    ...(patch || {}),
  }
}
__impl.normalizeGenerationConfig = normalizeGenerationConfig

function emitFieldGenerationUpdate(generation = {}, options = {}) {
  const fieldCode = selectedComponent.value?.fieldBinding?.fieldCode
    || selectedComponent.value?.field
    || ''
  if (!fieldCode || typeof emit !== 'function')
    return
  emit('updateFieldGeneration', {
    fieldCode,
    generation,
    hideOnForm: options.hideOnForm === true
      || (options.hideOnForm !== false && !!selectedComponent.value?.visibility?.hidden),
  })
}
__impl.emitFieldGenerationUpdate = emitFieldGenerationUpdate

async function handleGenerationEnabled(value) {
  if (value) {
    await loadCodeRuleOptions()
    const next = normalizeGenerationConfig({
      enabled: true,
      ruleCode: selectedGenerationRuleCode.value || codeRules.value[0]?.ruleCode || '',
      readonly: true,
    })
    const currentPlaceholder = String(selectedComponent.value?.props?.placeholder || '').trim()
    const placeholderLooksManual = !currentPlaceholder
      || currentPlaceholder.startsWith('请填写')
      || currentPlaceholder.startsWith('请输入')
      || currentPlaceholder.startsWith('占位')
    updateComponent({
      props: {
        generation: next,
        defaultValue: undefined,
        // 运行态只读空值时，避免仍显示「请输入xxx」让客户误以为坏了
        ...(placeholderLooksManual ? { placeholder: '保存后自动生成' } : {}),
      },
      validation: {
        required: false,
        requiredMessage: '',
      },
      // 编码规则字段应只读可见，禁止顺带 hidden（否则运行态整字段被滤掉，看起来像「组件没了」）
      visibility: {
        readonly: true,
        hidden: false,
      },
    })
    emitFieldGenerationUpdate(next, { hideOnForm: false })
    if (next.ruleCode)
      previewSelectedGenerationRule(next.ruleCode)
    return
  }
  const disabled = {
    ...(selectedGenerationConfig.value || {}),
    enabled: false,
  }
  updateComponent({
    props: {
      generation: disabled,
    },
  })
  emitFieldGenerationUpdate(disabled, { hideOnForm: false })
  codeRulePreview.value = null
}
__impl.handleGenerationEnabled = handleGenerationEnabled

function updateGenerationConfig(patch = {}) {
  const next = normalizeGenerationConfig(patch)
  updateComponent({
    props: { generation: next },
    ...(next.readonly !== false ? { visibility: { readonly: true } } : {}),
  })
  emitFieldGenerationUpdate(next, {
    hideOnForm: !!selectedComponent.value?.visibility?.hidden,
  })
}
__impl.updateGenerationConfig = updateGenerationConfig

function updateGenerationRule(ruleCode = '') {
  updateGenerationConfig({ ruleCode: ruleCode || '' })
  if (ruleCode)
    previewSelectedGenerationRule(ruleCode)
}
__impl.updateGenerationRule = updateGenerationRule

function updateGenerationReadonly(value) {
  updateGenerationConfig({ readonly: value !== false })
  updateComponent({ visibility: { readonly: value !== false } })
}
__impl.updateGenerationReadonly = updateGenerationReadonly

async function previewSelectedGenerationRule(ruleCode = selectedGenerationRuleCode.value) {
  // @click 直接绑定时会把 MouseEvent 当作首参，需忽略非字符串
  const code = typeof ruleCode === 'string' ? ruleCode : selectedGenerationRuleCode.value
  if (!code)
    return
  await loadCodeRuleOptions()
  codeRulePreviewing.value = true
  try {
    const fieldCode = selectedComponent.value?.fieldBinding?.fieldCode || selectedComponent.value?.field || 'code'
    const res = await previewCodeRule({
      ruleCode: code,
      sequence: 1,
      fields: {
        suiteCode: props.schema?.suiteCode || props.schema?.settings?.suiteCode || 'SUITE',
        objectCode: props.schema?.objectCode || props.schema?.settings?.objectCode || 'OBJECT',
        fieldCode,
        [fieldCode]: selectedComponent.value?.label || fieldCode,
      },
    })
    codeRulePreview.value = res.data || null
  }
  finally {
    codeRulePreviewing.value = false
  }
}
__impl.previewSelectedGenerationRule = previewSelectedGenerationRule

function updateFieldBindingCode(value = '') {
  const fieldCode = String(value || '').trim()
  if (fieldStructureLocked.value || !selectedComponent.value || !isField.value)
    return
  updateComponent({
    fieldBinding: {
      ...(selectedComponent.value.fieldBinding || {}),
      mode: selectedComponent.value.fieldBinding?.mode || 'field',
      fieldCode,
      columnName: camelToSnake(fieldCode),
    },
  })
}
__impl.updateFieldBindingCode = updateFieldBindingCode

function updateUniqueValidation(value) {
  updateComponent({
    advancedProps: {
      ...(selectedComponent.value?.advancedProps || {}),
      unique: Boolean(value),
    },
  })
}
__impl.updateUniqueValidation = updateUniqueValidation

function handlePropertySearch(value = '') {
  const keyword = String(value || '').trim().toLowerCase()
  if (!keyword) {
    propertySearchHit.value = ''
    return
  }

  const hit = propertySearchIndex.find(item => item.keys.some(key => String(key).toLowerCase().includes(keyword) || keyword.includes(String(key).toLowerCase())))
  if (!hit) {
    expandAllSearchableSections()
    propertySearchHit.value = '已展开当前面板全部配置'
    return
  }

  propertySearchHit.value = hit.label
  if (selectedComponent.value) {
    if (!hit.selectedTab && hit.formTab) {
      emit('update:selectedId', '')
      formPropertyActiveTab.value = hit.formTab
      if (hit.formTab === 'style' && hit.formExpand?.length)
        formStyleExpandedNames.value = mergeExpandNames(formStyleExpandedNames.value, hit.formExpand)
      else if (hit.formExpand?.length)
        formBasicExpandedNames.value = mergeExpandNames(formBasicExpandedNames.value, hit.formExpand)
      return
    }
    propertyActiveTab.value = hit.selectedTab || hit.formTab || 'basic'
    if (hit.selectedExpand?.length)
      selectedBasicExpandedNames.value = mergeExpandNames(selectedBasicExpandedNames.value, hit.selectedExpand)
    return
  }

  formPropertyActiveTab.value = hit.formTab || hit.selectedTab || 'basic'
  if (hit.formPropertyTab === 'style' || formPropertyActiveTab.value === 'style') {
    if (hit.formExpand?.length)
      formStyleExpandedNames.value = mergeExpandNames(formStyleExpandedNames.value, hit.formExpand)
    return
  }
  if (hit.formExpand?.length)
    formBasicExpandedNames.value = mergeExpandNames(formBasicExpandedNames.value, hit.formExpand)
}
__impl.handlePropertySearch = handlePropertySearch

function mergeExpandNames(current = [], names = []) {
  return Array.from(new Set([...(Array.isArray(current) ? current : []), ...names]))
}
__impl.mergeExpandNames = mergeExpandNames

function expandAllSearchableSections() {
  if (selectedComponent.value) {
    propertyActiveTab.value = 'basic'
    selectedBasicExpandedNames.value = mergeExpandNames(selectedBasicExpandedNames.value, allSelectedBasicExpandNames)
    return
  }
  formPropertyActiveTab.value = 'basic'
  formBasicExpandedNames.value = mergeExpandNames(formBasicExpandedNames.value, allFormBasicExpandNames)
  formStyleExpandedNames.value = mergeExpandNames(formStyleExpandedNames.value, allFormStyleExpandNames)
}
__impl.expandAllSearchableSections = expandAllSearchableSections

function updateFormGovernance(patch = {}) {
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      governance: {
        ...formGovernanceSettings.value,
        ...patch,
      },
    },
  })
}
__impl.updateFormGovernance = updateFormGovernance

function updateFormPermission(patch = {}) {
  updateFormGovernance({
    permission: {
      ...formPermissionConfig.value,
      ...patch,
    },
  })
}
__impl.updateFormPermission = updateFormPermission

function updateFormOfflineDraft(patch = {}) {
  updateFormGovernance({
    offlineDraft: {
      ...formOfflineDraftConfig.value,
      ...patch,
      formCode: patch.formCode || formOfflineDraftConfig.value.formCode || props.schema.formKey || 'default',
      recordVersionField: patch.recordVersionField
        || formOfflineDraftConfig.value.recordVersionField
        || 'updateTime',
    },
  })
}
__impl.updateFormOfflineDraft = updateFormOfflineDraft

function addFormFieldRule() {
  updateFormGovernance({
    fieldRules: [
      ...formFieldRuleRows.value,
      {
        id: `rule_${Date.now()}`,
        field: '',
        required: false,
        readonly: false,
        hidden: false,
        defaultValue: '',
      },
    ],
  })
}
__impl.addFormFieldRule = addFormFieldRule

function updateFormFieldRule(index, patch = {}) {
  const list = [...formFieldRuleRows.value]
  list[index] = { ...(list[index] || {}), ...patch }
  updateFormGovernance({ fieldRules: list })
}
__impl.updateFormFieldRule = updateFormFieldRule

function removeFormFieldRule(index) {
  const list = [...formFieldRuleRows.value]
  list.splice(index, 1)
  updateFormGovernance({ fieldRules: list })
}
__impl.removeFormFieldRule = removeFormFieldRule

function addFormEvent() {
  updateFormGovernance({
    events: [
      ...formEventRows.value,
      {
        id: `event_${Date.now()}`,
        hook: 'afterLoad',
        action: 'request',
        handler: '',
        resultMapping: '',
      },
    ],
  })
}
__impl.addFormEvent = addFormEvent

function updateFormEvent(index, patch = {}) {
  const list = [...formEventRows.value]
  list[index] = { ...(list[index] || {}), ...patch }
  updateFormGovernance({ events: list })
}
__impl.updateFormEvent = updateFormEvent

// 切换动作类型时重置 handler/resultMapping：不同动作的 handler 语义不同（脚本名 / 字段=值 / 接口地址）
function handleFormEventActionChange(index, action = 'customScript') {
  updateFormEvent(index, { action: action || 'customScript', handler: '', resultMapping: '' })
}
__impl.handleFormEventActionChange = handleFormEventActionChange

// request 的 handler 存储为 "method@url"（兼容存量纯 URL，视为 GET），UI 上拆成请求方式 + 接口地址两个控件
function parseRequestHandler(handler = '') {
  const text = String(handler || '').trim()
  if (!text.includes('@'))
    return { method: 'get', url: text }
  const [method = 'get', ...urlParts] = text.split('@')
  return { method: String(method || 'get').toLowerCase(), url: urlParts.join('@').trim() }
}
__impl.parseRequestHandler = parseRequestHandler

function composeRequestHandler(method = 'get', url = '') {
  const address = String(url || '').trim()
  if (!address)
    return ''
  return `${String(method || 'get').toLowerCase()}@${address}`
}
__impl.composeRequestHandler = composeRequestHandler

function formEventSummaryLabel(eventItem = {}) {
  const hookLabel = formEventHookOptions.find(option => option.value === eventItem.hook)?.label || '未选时机'
  const actionLabel = formEventActionOptions.find(option => option.value === eventItem.action)?.label || '未选动作'
  return `${hookLabel} · ${actionLabel}`
}
__impl.formEventSummaryLabel = formEventSummaryLabel

// setFieldValue 的 handler 格式为 "field=value"，UI 上拆成字段下拉 + 值输入两个控件
function parseSetFieldValueHandler(handler = '') {
  const [field = '', ...valueParts] = String(handler || '').split('=')
  return { field: field.trim(), value: valueParts.join('=') }
}
__impl.parseSetFieldValueHandler = parseSetFieldValueHandler

function composeSetFieldValueHandler(field = '', value = '') {
  const fieldName = String(field || '').trim()
  if (!fieldName)
    return ''
  return `${fieldName}=${String(value ?? '')}`
}
__impl.composeSetFieldValueHandler = composeSetFieldValueHandler

// resultMapping 存储 "from->to,from2->to2" 字符串，UI 上拆成一行行的可视化映射（运行时会跳过未填完整的行）
function parseResultMappingRows(resultMapping = '') {
  return String(resultMapping || '').split(',').map(item => item.trim()).filter(Boolean).map((item) => {
    const [from = '', ...toParts] = item.split('->')
    return { from: from.trim(), to: toParts.join('->').trim() }
  })
}
__impl.parseResultMappingRows = parseResultMappingRows

function composeResultMappingRows(rows = []) {
  return rows
    .map(row => ({ from: String(row.from || '').trim(), to: String(row.to || '').trim() }))
    .map(row => `${row.from}->${row.to}`)
    .join(',')
}
__impl.composeResultMappingRows = composeResultMappingRows

function updateResultMappingRow(index, rowIndex, patch = {}) {
  const rows = parseResultMappingRows(formEventRows.value[index]?.resultMapping)
  rows[rowIndex] = { ...(rows[rowIndex] || {}), ...patch }
  updateFormEvent(index, { resultMapping: composeResultMappingRows(rows) })
}
__impl.updateResultMappingRow = updateResultMappingRow

function addResultMappingRow(index) {
  const rows = parseResultMappingRows(formEventRows.value[index]?.resultMapping)
  rows.push({ from: '', to: '' })
  updateFormEvent(index, { resultMapping: composeResultMappingRows(rows) })
}
__impl.addResultMappingRow = addResultMappingRow

function removeResultMappingRow(index, rowIndex) {
  const rows = parseResultMappingRows(formEventRows.value[index]?.resultMapping)
  rows.splice(rowIndex, 1)
  updateFormEvent(index, { resultMapping: composeResultMappingRows(rows) })
}
__impl.removeResultMappingRow = removeResultMappingRow

function removeFormEvent(index) {
  const list = [...formEventRows.value]
  list.splice(index, 1)
  updateFormGovernance({ events: list })
}
__impl.removeFormEvent = removeFormEvent

function updateFormFieldEvents(fieldEvents = []) {
  updateFormGovernance({ fieldEvents: Array.isArray(fieldEvents) ? fieldEvents : [] })
}
__impl.updateFormFieldEvents = updateFormFieldEvents


  return {
    ...deps,
    __impl,
    mut,
    designerStore,
    message,
    fieldFormulaPanelVisible,
    propertyActiveTab,
    formPropertyActiveTab,
    selectedBasicExpandedNames,
    formBasicExpandedNames,
    formStyleExpandedNames,
    allSelectedBasicExpandNames,
    allFormBasicExpandNames,
    allFormStyleExpandNames,
    propertySearchHit,
    codeRules,
    codeRuleLoading,
    codeRulePreview,
    codeRulePreviewing,
    selectedComponent,
    isField,
    fieldStructureLocked,
    formGovernanceSettings,
    formPermissionConfig,
    formFieldRuleRows,
    formEventRows,
    formOfflineDraftConfig,
    selectedFieldCode,
    selectedFieldAsset,
    switchableComponentGroups,
    formEventHookOptions,
    formEventActionOptions,
    childTableRelationOptions,
    childTableTargetObjectCode,
    propertySearchIndex,
    isTreeOptionField,
    defaultOptionPageSize,
    selectedOptionSourceType,
    querySourceMetaFields,
    loadQuerySourceMeta,
    selectedGenerationConfig,
    selectedGenerationRuleCode,
    isObjectReferenceField,
    isRecordSelectorField,
    referenceObjectCode,
    referenceDisplayField,
    referenceValueField,
    referenceFieldMappings,
    recordSelectorConfigObj,
    recordSelectorObjectCode,
    businessObjectOptions,
    businessObjectLoading,
    referenceTargetFieldsMap,
    recordSelectorTargetFieldOptions,
    updateComponent,
    pickSwitchableCommonProps,
    handleSwitchComponentType,
    updateComponentHidden,
    updateValidationPreset,
    normalizePositiveInteger,
    updateFieldMaxLength,
    openFieldFormulaPanel,
    handleFieldAssetSave,
    buildFieldAssetComponentPatch,
    normalizeSelectedFieldAsset,
    createFieldAssetFromSelectedComponent,
    resolveSelectedFieldCode,
    resolveSelectionLabelValueField,
    withTreeOptionSourceDefaults,
    updateTreeOptionSourceMapping,
    updatePageWidgetOptionSource,
    resolveOptionValueFieldMeta,
    syncFieldStorageFromOptionValueField,
    updateOptionSourceType,
    updateCurrentChildrenRelationKey,
    updateBusinessObjectOptionSource,
    ensureOptionSourceFieldCatalog,
    updatePageWidgetDataBinding,
    widgetParamRefName,
    widgetParamRefField,
    addWidgetParamRef,
    resolveBooleanKeys,
    updateCodeColor,
    updateDictType,
    loadCodeRuleOptions,
    loadBusinessObjectOptions,
    renderReferenceObjectLabel,
    loadReferenceTargetFields,
    updateReferenceObjectCode,
    updateReferenceDisplayField,
    updateReferenceValueField,
    addReferenceFieldMapping,
    updateReferenceFieldMapping,
    removeReferenceFieldMapping,
    persistSelectFieldMappings,
    updateRecordSelectorObjectCode,
    updateRecordSelectorValueField,
    rsConfigDialogShow,
    rsDialogFilters,
    rsDialogMappings,
    rsConfigSummary,
    handleRsConfigConfirm,
    updateMultipleSelect,
    normalizeSingleDefaultValue,
    normalizeGenerationConfig,
    handleGenerationEnabled,
    updateGenerationConfig,
    updateGenerationRule,
    updateGenerationReadonly,
    previewSelectedGenerationRule,
    updateFieldBindingCode,
    updateUniqueValidation,
    handlePropertySearch,
    mergeExpandNames,
    expandAllSearchableSections,
    updateFormGovernance,
    updateFormPermission,
    updateFormOfflineDraft,
    addFormFieldRule,
    updateFormFieldRule,
    removeFormFieldRule,
    addFormEvent,
    updateFormEvent,
    handleFormEventActionChange,
    parseRequestHandler,
    composeRequestHandler,
    formEventSummaryLabel,
    parseSetFieldValueHandler,
    composeSetFieldValueHandler,
    parseResultMappingRows,
    composeResultMappingRows,
    updateResultMappingRow,
    addResultMappingRow,
    removeResultMappingRow,
    removeFormEvent,
    updateFormFieldEvents,
  }
}
