/** Auto-split part 1 of ForgePropertyPanel setup. */
import { NTag, useMessage } from 'naive-ui'
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList, codeRuleList, previewCodeRule } from '@/api/business-app'
import { getLowcodeQuerySourceCatalog, getLowcodeQuerySourceMetadata } from '@/api/lowcode-query-source'
import {
  createDefaultTreeOptionSourcePatch,
  DEFAULT_OPTION_PAGE_SIZE,
  DEFAULT_TREE_OPTION_PAGE_SIZE,
  resolveStorageTypeFromOptionValueMeta,
} from '@/components/ai-form/option-source-runtime'
import { parseQuerySourceInputSchema } from '@/components/ai-form/query-source-schema'
import { supportsMultipleSelect as isMultiSelectComponent, serializeSelectionValues } from '@/components/ai-form/selection-multi-value'
import { getComponentSpec } from '@/components/lowcode-builder/designer-core'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import { getDictData } from '@/composables/useDict'
import { useFormDesignerStore } from '@/store'
import { COMMON_VALIDATION_PRESETS, getValidationPreset } from '@/utils/validation-presets'
import { FIELD_COMPONENT_DEFAULTS as componentFieldDefaults } from '../../form-first/fieldComponentCatalog'
import { appendDesignerLayoutChild, cloneValue, findDesignerComponentPath, getDesignerComponent, isFieldComponent, isLayoutComponent, normalizeFormDesignerSchema, updateDesignerComponent } from '../../form-first/formDesignerSchema'
import { camelToSnake } from '../../form-first/namingUtils'
import { GRID_COLUMN_MARKS as gridColumnMarks, MAX_FORM_GRID_COLUMNS, normalizeFormLayoutColumns, normalizeGridCount } from '../formLayoutConfig'
import {
  collectChildTableFieldOptions,
  collectChildTableRelationOptions,
  resolveChildRelationTargetObjectCode,
} from '../option-source-picker'
import { buildDefaultPlaceholder, buildFieldAssetPlaceholderPatch, shouldSyncPlaceholder } from '../placeholder-utils'
import { provideForgePropertyPanelApi } from '../forgePropertyPanelContext'
import {
  BitableAttachmentIcon,
  BitableCalendarIcon,
  BitableDragIcon,
  BitableLookupIcon,
  BitableMemberIcon,
  BitableNumberIcon,
  BitableSelectIcon,
  BitableStyleIcon,
  createBitableSvgIcon,
} from '../bitableIcons'
export function useForgePropertyPanelPart1(props, emit, deps = {}) {
  const __impl = {}
  const mut = {
    codeRuleRequestVersion: 0,
  }
  const addFirstCrudExpandDescriptionField = (...args) => __impl.addFirstCrudExpandDescriptionField(...args)
  const addFormEvent = (...args) => __impl.addFormEvent(...args)
  const addFormFieldRule = (...args) => __impl.addFormFieldRule(...args)
  const addInteractionPreset = (...args) => __impl.addInteractionPreset(...args)
  const addInteractionRule = (...args) => __impl.addInteractionRule(...args)
  const addLayoutChild = (...args) => __impl.addLayoutChild(...args)
  const addOption = (...args) => __impl.addOption(...args)
  const addReferenceFieldMapping = (...args) => __impl.addReferenceFieldMapping(...args)
  const addResultMappingRow = (...args) => __impl.addResultMappingRow(...args)
  const addWidgetParamRef = (...args) => __impl.addWidgetParamRef(...args)
  const applySchemaCode = (...args) => __impl.applySchemaCode(...args)
  const applySelectedCode = (...args) => __impl.applySelectedCode(...args)
  const applySourceModalCode = (...args) => __impl.applySourceModalCode(...args)
  const applyStyleValue = (...args) => __impl.applyStyleValue(...args)
  const buildDefaultCrudApiConfig = (...args) => __impl.buildDefaultCrudApiConfig(...args)
  const buildFieldAssetComponentPatch = (...args) => __impl.buildFieldAssetComponentPatch(...args)
  const cancelSourceModalEdit = (...args) => __impl.cancelSourceModalEdit(...args)
  const collectBoundFieldOptions = (...args) => __impl.collectBoundFieldOptions(...args)
  const collectComponentTargetOptions = (...args) => __impl.collectComponentTargetOptions(...args)
  const collectCrudConfigFields = (...args) => __impl.collectCrudConfigFields(...args)
  const collectDictTypeFields = (...args) => __impl.collectDictTypeFields(...args)
  const collectDrivenRuntimeRules = (...args) => __impl.collectDrivenRuntimeRules(...args)
  const colorToHexInput = (...args) => __impl.colorToHexInput(...args)
  const composeRequestHandler = (...args) => __impl.composeRequestHandler(...args)
  const composeResultMappingRows = (...args) => __impl.composeResultMappingRows(...args)
  const composeSetFieldValueHandler = (...args) => __impl.composeSetFieldValueHandler(...args)
  const createColumn = (...args) => __impl.createColumn(...args)
  const createDefaultCrudExpandPanel = (...args) => __impl.createDefaultCrudExpandPanel(...args)
  const createFieldAssetFromSelectedComponent = (...args) => __impl.createFieldAssetFromSelectedComponent(...args)
  const ensureOptionSourceFieldCatalog = (...args) => __impl.ensureOptionSourceFieldCatalog(...args)
  const expandAllSearchableSections = (...args) => __impl.expandAllSearchableSections(...args)
  const formEventSummaryLabel = (...args) => __impl.formEventSummaryLabel(...args)
  const formatDefaultValueOptionLabel = (...args) => __impl.formatDefaultValueOptionLabel(...args)
  const formatTranslateStyle = (...args) => __impl.formatTranslateStyle(...args)
  const handleCrudExpandDescriptionPanelReorder = (...args) => __impl.handleCrudExpandDescriptionPanelReorder(...args)
  const handleFieldAssetSave = (...args) => __impl.handleFieldAssetSave(...args)
  const handleFormEventActionChange = (...args) => __impl.handleFormEventActionChange(...args)
  const handleGenerationEnabled = (...args) => __impl.handleGenerationEnabled(...args)
  const handleOpenSourcePanel = (...args) => __impl.handleOpenSourcePanel(...args)
  const handlePropertySearch = (...args) => __impl.handlePropertySearch(...args)
  const handleRsConfigConfirm = (...args) => __impl.handleRsConfigConfirm(...args)
  const handleSwitchComponentType = (...args) => __impl.handleSwitchComponentType(...args)
  const hasAncestorComponent = (...args) => __impl.hasAncestorComponent(...args)
  const hexInputToColor = (...args) => __impl.hexInputToColor(...args)
  const loadBusinessObjectOptions = (...args) => __impl.loadBusinessObjectOptions(...args)
  const loadCodeRuleOptions = (...args) => __impl.loadCodeRuleOptions(...args)
  const loadDefaultValueDictOptions = (...args) => __impl.loadDefaultValueDictOptions(...args)
  const loadReferenceTargetFields = (...args) => __impl.loadReferenceTargetFields(...args)
  const mergeExpandNames = (...args) => __impl.mergeExpandNames(...args)
  const moveLayoutChild = (...args) => __impl.moveLayoutChild(...args)
  const normalizeApiBase = (...args) => __impl.normalizeApiBase(...args)
  const normalizeAppearanceColor = (...args) => __impl.normalizeAppearanceColor(...args)
  const normalizeCrudExpandPanelPatch = (...args) => __impl.normalizeCrudExpandPanelPatch(...args)
  const normalizeDefaultValueOptions = (...args) => __impl.normalizeDefaultValueOptions(...args)
  const normalizeGenerationConfig = (...args) => __impl.normalizeGenerationConfig(...args)
  const normalizeOpacityPercent = (...args) => __impl.normalizeOpacityPercent(...args)
  const normalizePositionNumber = (...args) => __impl.normalizePositionNumber(...args)
  const normalizePositiveInteger = (...args) => __impl.normalizePositiveInteger(...args)
  const normalizeSelectedFieldAsset = (...args) => __impl.normalizeSelectedFieldAsset(...args)
  const normalizeSingleDefaultValue = (...args) => __impl.normalizeSingleDefaultValue(...args)
  const openCrudFieldDrawer = (...args) => __impl.openCrudFieldDrawer(...args)
  const openFieldFormulaPanel = (...args) => __impl.openFieldFormulaPanel(...args)
  const parseJsonObjectProp = (...args) => __impl.parseJsonObjectProp(...args)
  const parseRequestHandler = (...args) => __impl.parseRequestHandler(...args)
  const parseResultMappingRows = (...args) => __impl.parseResultMappingRows(...args)
  const parseSetFieldValueHandler = (...args) => __impl.parseSetFieldValueHandler(...args)
  const parseStyleText = (...args) => __impl.parseStyleText(...args)
  const parseTranslateStyle = (...args) => __impl.parseTranslateStyle(...args)
  const persistSelectFieldMappings = (...args) => __impl.persistSelectFieldMappings(...args)
  const pickSwitchableCommonProps = (...args) => __impl.pickSwitchableCommonProps(...args)
  const previewSelectedGenerationRule = (...args) => __impl.previewSelectedGenerationRule(...args)
  const removeFirstCrudExpandDescriptionField = (...args) => __impl.removeFirstCrudExpandDescriptionField(...args)
  const removeFormEvent = (...args) => __impl.removeFormEvent(...args)
  const removeFormFieldRule = (...args) => __impl.removeFormFieldRule(...args)
  const removeInteractionRule = (...args) => __impl.removeInteractionRule(...args)
  const removeLayoutChild = (...args) => __impl.removeLayoutChild(...args)
  const removeOption = (...args) => __impl.removeOption(...args)
  const removeReferenceFieldMapping = (...args) => __impl.removeReferenceFieldMapping(...args)
  const removeResultMappingRow = (...args) => __impl.removeResultMappingRow(...args)
  const renderReferenceObjectLabel = (...args) => __impl.renderReferenceObjectLabel(...args)
  const replaceComponentInSchema = (...args) => __impl.replaceComponentInSchema(...args)
  const resetSchemaCodeDraft = (...args) => __impl.resetSchemaCodeDraft(...args)
  const resetSelectedCodeDraft = (...args) => __impl.resetSelectedCodeDraft(...args)
  const resolveActionLabel = (...args) => __impl.resolveActionLabel(...args)
  const resolveActionValueMeta = (...args) => __impl.resolveActionValueMeta(...args)
  const resolveBooleanKeys = (...args) => __impl.resolveBooleanKeys(...args)
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
  const resolveOptionValueFieldMeta = (...args) => __impl.resolveOptionValueFieldMeta(...args)
  const resolvePxNumber = (...args) => __impl.resolvePxNumber(...args)
  const resolveStyleBox = (...args) => __impl.resolveStyleBox(...args)
  const resolveSelectedFieldCode = (...args) => __impl.resolveSelectedFieldCode(...args)
  const resolveSelectionLabelValueField = (...args) => __impl.resolveSelectionLabelValueField(...args)
  const resolveTriggerLabel = (...args) => __impl.resolveTriggerLabel(...args)
  const runtimeRuleDependsOnField = (...args) => __impl.runtimeRuleDependsOnField(...args)
  const stringifyJsonProp = (...args) => __impl.stringifyJsonProp(...args)
  const stringifyStyle = (...args) => __impl.stringifyStyle(...args)
  const summarizeDrivenRuntimeRule = (...args) => __impl.summarizeDrivenRuntimeRule(...args)
  const syncFieldStorageFromOptionValueField = (...args) => __impl.syncFieldStorageFromOptionValueField(...args)
  const toCamelCase = (...args) => __impl.toCamelCase(...args)
  const toKebabCase = (...args) => __impl.toKebabCase(...args)
  const toggleFirstCrudExpandDescriptionField = (...args) => __impl.toggleFirstCrudExpandDescriptionField(...args)
  const toggleOptionDisabled = (...args) => __impl.toggleOptionDisabled(...args)
  const updateBusinessObjectOptionSource = (...args) => __impl.updateBusinessObjectOptionSource(...args)
  const updateCodeColor = (...args) => __impl.updateCodeColor(...args)
  const updateComponent = (...args) => __impl.updateComponent(...args)
  const updateComponentHidden = (...args) => __impl.updateComponentHidden(...args)
  const updateComponentStyleText = (...args) => __impl.updateComponentStyleText(...args)
  const updateCrudApiBase = (...args) => __impl.updateCrudApiBase(...args)
  const updateCrudApiConfig = (...args) => __impl.updateCrudApiConfig(...args)
  const updateCrudExpandConfig = (...args) => __impl.updateCrudExpandConfig(...args)
  const updateCrudExpandEnabled = (...args) => __impl.updateCrudExpandEnabled(...args)
  const updateCrudFieldConfig = (...args) => __impl.updateCrudFieldConfig(...args)
  const updateCrudFieldConfigById = (...args) => __impl.updateCrudFieldConfigById(...args)
  const updateCrudFieldRole = (...args) => __impl.updateCrudFieldRole(...args)
  const updateCrudOption = (...args) => __impl.updateCrudOption(...args)
  const updateCurrentChildrenRelationKey = (...args) => __impl.updateCurrentChildrenRelationKey(...args)
  const updateDefaultValue = (...args) => __impl.updateDefaultValue(...args)
  const updateDesignerBorderColor = (...args) => __impl.updateDesignerBorderColor(...args)
  const updateDesignerBorderStyle = (...args) => __impl.updateDesignerBorderStyle(...args)
  const updateDesignerCustomStyle = (...args) => __impl.updateDesignerCustomStyle(...args)
  const updateDesignerBoxSpacing = (...args) => __impl.updateDesignerBoxSpacing(...args)
  const updateDesignerSpacing = (...args) => __impl.updateDesignerSpacing(...args)
  const updateDesignerStyle = (...args) => __impl.updateDesignerStyle(...args)
  const updateDesignerTranslate = (...args) => __impl.updateDesignerTranslate(...args)
  const updateDictType = (...args) => __impl.updateDictType(...args)
  const updateEditingCrudFieldConfig = (...args) => __impl.updateEditingCrudFieldConfig(...args)
  const updateFieldBindingCode = (...args) => __impl.updateFieldBindingCode(...args)
  const updateFieldMaxLength = (...args) => __impl.updateFieldMaxLength(...args)
  const updateFirstCrudExpandDataSource = (...args) => __impl.updateFirstCrudExpandDataSource(...args)
  const updateFirstCrudExpandDescriptionFields = (...args) => __impl.updateFirstCrudExpandDescriptionFields(...args)
  const updateFirstCrudExpandPanel = (...args) => __impl.updateFirstCrudExpandPanel(...args)
  const updateFirstCrudExpandParamsMap = (...args) => __impl.updateFirstCrudExpandParamsMap(...args)
  const updateFormAppearanceBackground = (...args) => __impl.updateFormAppearanceBackground(...args)
  const updateFormAppearanceBorder = (...args) => __impl.updateFormAppearanceBorder(...args)
  const updateFormAppearanceOpacity = (...args) => __impl.updateFormAppearanceOpacity(...args)
  const updateFormEvent = (...args) => __impl.updateFormEvent(...args)
  const updateFormFieldEvents = (...args) => __impl.updateFormFieldEvents(...args)
  const updateFormFieldLinkages = (...args) => __impl.updateFormFieldLinkages(...args)
  const updateFormFieldRule = (...args) => __impl.updateFormFieldRule(...args)
  const updateFormGovernance = (...args) => __impl.updateFormGovernance(...args)
  const updateFormLayout = (...args) => __impl.updateFormLayout(...args)
  const updateFormOfflineDraft = (...args) => __impl.updateFormOfflineDraft(...args)
  const updateFormPermission = (...args) => __impl.updateFormPermission(...args)
  const updateFormBoxSpacing = (...args) => __impl.updateFormBoxSpacing(...args)
  const updateFormSpacing = (...args) => __impl.updateFormSpacing(...args)
  const updateFormStyle = (...args) => __impl.updateFormStyle(...args)
  const updateFormStyleText = (...args) => __impl.updateFormStyleText(...args)
  const updateFormTranslate = (...args) => __impl.updateFormTranslate(...args)
  const updateGenerationConfig = (...args) => __impl.updateGenerationConfig(...args)
  const updateGenerationReadonly = (...args) => __impl.updateGenerationReadonly(...args)
  const updateGenerationRule = (...args) => __impl.updateGenerationRule(...args)
  const updateHeightMode = (...args) => __impl.updateHeightMode(...args)
  const updateInteractionRule = (...args) => __impl.updateInteractionRule(...args)
  const updateInteractionRuleJson = (...args) => __impl.updateInteractionRuleJson(...args)
  const updateLabel = (...args) => __impl.updateLabel(...args)
  const updateLayoutChild = (...args) => __impl.updateLayoutChild(...args)
  const updateMultipleSelect = (...args) => __impl.updateMultipleSelect(...args)
  const updateOption = (...args) => __impl.updateOption(...args)
  const updateOptionJsonProps = (...args) => __impl.updateOptionJsonProps(...args)
  const updateOptionLabel = (...args) => __impl.updateOptionLabel(...args)
  const updateOptionSourceType = (...args) => __impl.updateOptionSourceType(...args)
  const updatePageWidgetDataBinding = (...args) => __impl.updatePageWidgetDataBinding(...args)
  const updatePageWidgetOptionSource = (...args) => __impl.updatePageWidgetOptionSource(...args)
  const updateRecordSelectorObjectCode = (...args) => __impl.updateRecordSelectorObjectCode(...args)
  const updateRecordSelectorValueField = (...args) => __impl.updateRecordSelectorValueField(...args)
  const updateReferenceDisplayField = (...args) => __impl.updateReferenceDisplayField(...args)
  const updateReferenceFieldMapping = (...args) => __impl.updateReferenceFieldMapping(...args)
  const updateReferenceObjectCode = (...args) => __impl.updateReferenceObjectCode(...args)
  const updateReferenceValueField = (...args) => __impl.updateReferenceValueField(...args)
  const updateResultMappingRow = (...args) => __impl.updateResultMappingRow(...args)
  const updateRowCellCount = (...args) => __impl.updateRowCellCount(...args)
  const updateRowColumnSpan = (...args) => __impl.updateRowColumnSpan(...args)
  const updateRowTotalColumns = (...args) => __impl.updateRowTotalColumns(...args)
  const updateSchemaCodeDraft = (...args) => __impl.updateSchemaCodeDraft(...args)
  const updateSelectedAppearanceBackground = (...args) => __impl.updateSelectedAppearanceBackground(...args)
  const updateSelectedAppearanceBorder = (...args) => __impl.updateSelectedAppearanceBorder(...args)
  const updateSelectedAppearanceOpacity = (...args) => __impl.updateSelectedAppearanceOpacity(...args)
  const updateSelectedCodeDraft = (...args) => __impl.updateSelectedCodeDraft(...args)
  const updateTreeOptionSourceMapping = (...args) => __impl.updateTreeOptionSourceMapping(...args)
  const updateUniqueValidation = (...args) => __impl.updateUniqueValidation(...args)
  const updateValidationPreset = (...args) => __impl.updateValidationPreset(...args)
  const updateWidthMode = (...args) => __impl.updateWidthMode(...args)
  const valueToPx = (...args) => __impl.valueToPx(...args)
  const withTreeOptionSourceDefaults = (...args) => __impl.withTreeOptionSourceDefaults(...args)
// ---------- Pinia 桥接（AGENTS.md 5.14）----------
// 面板子组件（panels/*）统一读写 useFormDesignerStore，不再 props/emit 透传；
// 本组件作为入口保留 props/emit 接口，兼容存量父组件（ForgeFormDesigner / BusinessFormDesigner / application-runtime）。
const designerStore = useFormDesignerStore()
provideForgePropertyPanelApi()
const message = useMessage()

// 子面板模板仍用父级 prop 名（schema / selectedId / …）；经 provide 代理 + 显式 return 双保险
const schema = computed(() => designerStore.schema)
const selectedId = computed(() => designerStore.selectedId)
const fields = computed(() => designerStore.fields)
const relations = computed(() => designerStore.relations)
const objectCode = computed(() => designerStore.objectCode)

watch(
  () => [props.schema, props.selectedId, props.fields, props.relations, props.objectCode],
  () => {
    designerStore.syncFromProps({
      schema: props.schema,
      selectedId: props.selectedId,
      fields: props.fields,
      relations: props.relations,
      objectCode: props.objectCode,
    })
  },
  { immediate: true },
)

// KeepAlive 缓存页切回时 props 未必变化，但 store 可能已被其它设计器实例覆盖，需强制重新同步
onActivated(() => {
  designerStore.syncFromProps({
    schema: props.schema,
    selectedId: props.selectedId,
    fields: props.fields,
    relations: props.relations,
    objectCode: props.objectCode,
  })
})

// store 变化向外广播（同引用跳过，避免与 props → store 同步形成回环）
watch(() => designerStore.schema, (next) => {
  if (next !== props.schema)
    emit('update:schema', next)
})

watch(() => designerStore.selectedId, (next) => {
  if (next !== props.selectedId)
    emit('update:selectedId', next)
})

const advancedConfigVisible = ref(false)
const componentPropsVisible = ref(false)
const crudFieldDrawerVisible = ref(false)
const fieldFormulaPanelVisible = ref(false)
const crudDescriptionFieldPanelOpen = ref(false)
const editingCrudFieldId = ref('')
const propertyActiveTab = ref('basic')
// 表单级属性 tab 收敛到 store（panels/* 子面板需要切换它），父组件以 computed 包装保持 v-model 兼容
const formPropertyActiveTab = computed({
  get: () => designerStore.formPropertyTab,
  set: tab => designerStore.setFormPropertyTab(tab),
})
// 默认只展开核心配置（标识 + 字段组件），栅格/校验等低频项收起 —— 主次分明，避免一屏全是展开的卡片
const basicExpandedNames = ['identity', 'field']
/** 是否显示"值"编辑列（宜搭式默认隐藏：值跟随选项名，需要值≠名称时勾选"自定义值"） */
const showOptionValues = ref(false)
const selectedBasicExpandedNames = ref([...basicExpandedNames])
const formBasicExpandedNames = ref(['layout', 'permissions', 'offline'])
const formStyleExpandedNames = ref(['position', 'layout', 'typography', 'appearance'])
const allSelectedBasicExpandNames = ['identity', 'gridQuick', 'field', 'button', 'crud-field', 'temporal', 'assist', 'validation']
const allFormBasicExpandNames = ['assets', 'subTables', 'layout', 'permissions', 'offline', 'validation', 'actions']
const allFormStyleExpandNames = ['position', 'spacing', 'typography', 'appearance', 'custom-style']
const commonValidationOptions = COMMON_VALIDATION_PRESETS.map(item => ({
  label: item.label,
  value: item.value,
}))
const propertySearchKeyword = ref('')
const propertySearchHit = ref('')
const selectedCodeDraft = ref('')
const selectedCodeDraftTarget = ref('')
const schemaCodeDraft = ref('')
const schemaCodeDirty = ref(false)
const sourceError = ref('')
const sourceModalVisible = ref(false)
const dictDefaultOptions = ref({})
const dictDefaultOptionsLoading = ref({})
const codeRules = ref([])
const codeRuleLoading = ref(false)
const codeRulePreview = ref(null)
const codeRulePreviewing = ref(false)
// 选中态以 store 为准（画布 selectComponent 先写 store）；props 仅作入口同步兜底
const selectedComponent = computed(() => {
  return designerStore.selectedComponent
    || getDesignerComponent(props.schema, props.selectedId)
})
const isField = computed(() => selectedComponent.value ? isFieldComponent(selectedComponent.value) : false)
const fieldStructureLocked = computed(() => isField.value && selectedComponent.value?.fieldBinding?.locked === true)
const isLayout = computed(() => selectedComponent.value ? isLayoutComponent(selectedComponent.value) : false)
const isCrudBlock = computed(() => ['AiCrudPage', 'crudBlock'].includes(selectedComponent.value?.componentKey))
const isSubTable = computed(() => selectedComponent.value?.componentKey === 'subTable')
const isRowLayout = computed(() => ['row', 'fcRow'].includes(selectedComponent.value?.componentKey))
const isColumnLayout = computed(() => selectedComponent.value?.componentKey === 'col')
const isCardLayout = computed(() => ['card', 'elCard'].includes(selectedComponent.value?.componentKey))
// 主子表配置已拆分至 panels/FormSubTablePanel.vue（读写 Pinia store，不依赖对象关系前置校验）
const isTabsLayout = computed(() => ['tabs', 'elTabs'].includes(selectedComponent.value?.componentKey))
const isCollapseLayout = computed(() => ['collapse', 'elCollapse'].includes(selectedComponent.value?.componentKey))
const isButtonComponent = computed(() => ['button', 'elButton'].includes(selectedComponent.value?.componentKey))
const isPageWidget = computed(() => pageWidgetComponentKeys.includes(selectedComponent.value?.componentKey))
const selectedLabel = computed(() => selectedComponent.value?.label || selectedComponent.value?.props?.header || selectedComponent.value?.props?.title || '未命名组件')
const componentTypeLabel = computed(() => isCrudBlock.value ? '系统 AiCrudPage 组件' : isField.value ? 'AiForm 字段组件' : isLayout.value ? '布局组件' : selectedComponent.value?.componentKey || '组件')
const panelDescription = computed(() => selectedComponent.value ? componentTypeLabel.value : 'AiForm / AiCrudPage 通用配置')
const crudApiConfig = computed(() => selectedComponent.value?.props?.apiConfig || {})
const crudOptions = computed(() => selectedComponent.value?.props?.crudOptions || {})
const selectedDesignerStyle = computed(() => selectedComponent.value?.props?.__designerStyle || {})
// 表单级样式/治理配置也必须以 store 为准，避免连续 patch 读到尚未回传的 props.schema
const formStyle = computed(() => designerStore.schema?.layout?.formStyle || {})
const formAssets = computed(() => designerStore.formAssets)
const formGovernanceSettings = computed(() => designerStore.formGovernanceSettings || {})
const formPermissionConfig = computed(() => formGovernanceSettings.value.permission || {})
const formFieldRuleRows = computed(() => Array.isArray(formGovernanceSettings.value.fieldRules) ? formGovernanceSettings.value.fieldRules : [])
const formEventRows = computed(() => Array.isArray(formGovernanceSettings.value.events) ? formGovernanceSettings.value.events : [])
const formFieldEventRows = computed(() => Array.isArray(formGovernanceSettings.value.fieldEvents) ? formGovernanceSettings.value.fieldEvents : [])
const hasFormInitConfig = computed(() => {
  const config = formGovernanceSettings.value.formInit
  return Boolean(
    config && typeof config === 'object'
    && ((Array.isArray(config.contextDefaults) && config.contextDefaults.length)
      || config.recordLoad?.enabled === true),
  )
})
const formFieldLinkageRows = computed(() => Array.isArray(formGovernanceSettings.value.fieldLinkages) ? formGovernanceSettings.value.fieldLinkages : [])
const formOfflineDraftConfig = computed(() => {
  const source = formGovernanceSettings.value.offlineDraft
  const config = source && typeof source === 'object' ? source : {}
  return {
    enabled: config.enabled === true,
    formCode: config.formCode || props.schema.formKey || 'default',
    replayActionCode: config.replayActionCode || '',
    recordVersionField: config.recordVersionField || 'updateTime',
  }
})
const formFieldOptions = computed(() => collectBoundFieldOptions(props.schema.components || []))
const formFieldCatalog = computed(() => {
  const source = Array.isArray(props.fields) && props.fields.length ? props.fields : props.schema.components || []
  return source.map(field => ({
    fieldCode: field?.fieldCode || field?.field || field?.props?.field || field?.id,
    fieldName: field?.fieldName || field?.label || field?.props?.label || field?.id,
  })).filter(field => field.fieldCode)
})
const selectedFieldCode = computed(() => String(
  selectedComponent.value?.fieldBinding?.fieldCode
  || selectedComponent.value?.field
  || selectedComponent.value?.props?.fieldCode
  || '',
).trim())
const selectedFieldAsset = computed(() => {
  if (!isField.value || !selectedFieldCode.value)
    return null
  const matched = (props.fields || []).find((field) => {
    const code = field?.fieldCode || field?.field
    return code === selectedFieldCode.value
  })
  return normalizeSelectedFieldAsset(matched || createFieldAssetFromSelectedComponent())
})
const switchableComponentGroups = [
  ['input', 'textarea', 'number', 'inputNumber', 'money'],
  ['select', 'radio', 'checkbox', 'dictSelect'],
  ['date', 'datetime'],
  ['fileUpload', 'imageUpload'],
  ['objectReference', 'recordSelector'],
]
const componentTypeLabelMap = {
  input: '单行输入',
  barcodeScanner: '扫码输入',
  textarea: '多行文本',
  number: '数字输入',
  inputNumber: '数字输入',
  money: '金额',
  select: '下拉选择',
  radio: '单选框',
  checkbox: '多选框',
  dictSelect: '字典选择',
  date: '日期',
  datetime: '日期时间',
  fileUpload: '文件上传',
  imageUpload: '图片上传',
  objectReference: '引用对象',
  recordSelector: '记录选择器',
}
const canSwitchComponentType = computed(() => {
  if (!isField.value || !selectedComponent.value)
    return false
  const key = selectedComponent.value.componentKey
  return switchableComponentGroups.some(group => group.includes(key))
})
const switchableComponentOptions = computed(() => {
  const key = selectedComponent.value?.componentKey
  const group = switchableComponentGroups.find(item => item.includes(key)) || []
  return group.map(value => ({
    label: componentTypeLabelMap[value] || value,
    value,
  }))
})
const supportsFieldMaxLength = computed(() => ['input', 'textarea', 'barcodeScanner'].includes(selectedComponent.value?.componentKey))
const supportsFieldNumberRange = computed(() => ['number', 'inputNumber', 'money'].includes(selectedComponent.value?.componentKey))
/** 单行文本 / 数字 / 金额：右侧主面板可配 Naive prefix / suffix / 图标插槽 */
const supportsFieldAffix = computed(() => ['input', 'number', 'inputNumber', 'money'].includes(selectedComponent.value?.componentKey))
const supportsFieldShowButton = computed(() => ['number', 'inputNumber', 'money'].includes(selectedComponent.value?.componentKey))
/** 下拉：菜单 header / action / empty / arrow */
const supportsFieldSelectSlots = computed(() => selectedComponent.value?.componentKey === 'select')
/** 日期类：DatePicker Slots（date-icon / footer / 导航图标 / separator） */
const supportsFieldDatePickerSlots = computed(() => (
  ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year'].includes(selectedComponent.value?.componentKey)
))
const selectedFieldMaxLength = computed(() => {
  const fromProps = normalizePositiveInteger(selectedComponent.value?.props?.maxlength)
  if (fromProps)
    return fromProps
  return normalizePositiveInteger(selectedFieldAsset.value?.length)
})
const selectedFormulaConfig = computed(() => selectedFieldAsset.value?.formulaConfig || null)
const selectedFormulaSummary = computed(() => {
  const config = selectedFormulaConfig.value
  if (!config?.type)
    return '字段资产级计算逻辑，可用于金额、状态、跨对象取值等场景。'
  if (config.type === 'LOOKUP')
    return `查找 ${config.lookup?.returnField || '目标字段'}`
  if (config.type === 'AGGREGATE')
    return `${config.aggregate?.function || '聚合'} ${config.aggregate?.targetField || ''}`.trim()
  return config.expression || config.condition?.expression || '已配置公式'
})
const formulaPanelFields = computed(() => {
  const current = selectedFieldAsset.value
  const fields = Array.isArray(props.fields) ? props.fields : []
  if (!current)
    return fields
  let matched = false
  const merged = fields.map((field) => {
    const code = field?.fieldCode || field?.field
    if (code !== current.fieldCode)
      return field
    matched = true
    return { ...field, ...current }
  })
  return matched ? merged : [current, ...merged]
})
const formEventHookOptions = [
  { label: '表单打开前', value: 'beforeLoad' },
  { label: '表单打开后', value: 'afterLoad' },
  { label: '提交前', value: 'beforeSubmit' },
  { label: '提交后', value: 'afterSubmit' },
]
const formEventActionOptions = [
  { label: '调用接口', value: 'request' },
  { label: '填入字段值', value: 'setFieldValue' },
  { label: '执行内置动作', value: 'customScript' },
]
// 运行时白名单脚本（crud-page.vue runWhitelistedFormScript），新增脚本需同步两端
const formScriptOptions = [
  { label: '填入当前日期', value: 'fillCurrentDate' },
  { label: '填入当前时间', value: 'fillCurrentTime' },
]
const formAssetOptions = computed(() => [
  { label: `${props.schema.formName || '主表单'}（当前表单）`, value: 'current' },
  ...formAssets.value.map(asset => ({
    label: `${asset.formName || asset.formKey}（${asset.formKey}）`,
    value: asset.formKey,
  })),
])
const layoutChildren = computed(() => selectedComponent.value?.children || [])
const interactionRules = computed(() => Array.isArray(selectedComponent.value?.props?.__events) ? selectedComponent.value.props.__events : [])
const defaultTrigger = computed(() => selectedComponent.value?.componentKey === 'button' ? 'click' : 'change')
const componentTargetOptions = computed(() => collectComponentTargetOptions(props.schema.components || []))
const selectedDrivenRuntimeRules = computed(() => collectDrivenRuntimeRules(
  props.schema.components || [],
  selectedComponent.value?.fieldBinding?.fieldCode || selectedComponent.value?.field || '',
  selectedComponent.value?.id || '',
))
const dictTypeFields = computed(() => collectDictTypeFields(props.schema.components || []))
const selectedComponentCodeRaw = computed(() => JSON.stringify(selectedComponent.value || {}, null, 2))
const selectedCodeText = computed(() => selectedCodeDraftTarget.value === props.selectedId ? selectedCodeDraft.value : selectedComponentCodeRaw.value)
const schemaCodeRaw = computed(() => JSON.stringify(props.schema || {}, null, 2))
const schemaCodeText = computed(() => schemaCodeDirty.value ? schemaCodeDraft.value : schemaCodeRaw.value)
const selectedOpacityPercent = computed(() => Math.round(Number(selectedDesignerStyle.value.opacity ?? 1) * 100))
const formOpacityPercent = computed(() => Math.round(Number(formStyle.value.opacity ?? 1) * 100))
const selectedDesignerTranslate = computed(() => parseTranslateStyle(selectedDesignerStyle.value.customStyle?.transform))
const formTranslate = computed(() => parseTranslateStyle(formStyle.value.transform))
const selectedPaddingBox = computed(() => resolveStyleBox(selectedDesignerStyle.value.customStyle, 'padding'))
const selectedMarginBox = computed(() => resolveStyleBox(selectedDesignerStyle.value.customStyle, 'margin'))
const formPaddingBox = computed(() => resolveStyleBox(formStyle.value, 'padding'))
const formMarginBox = computed(() => resolveStyleBox(formStyle.value, 'margin'))
const selectedAppearanceBackgroundHex = computed(() => colorToHexInput(selectedDesignerStyle.value.backgroundColor, ''))
const selectedAppearanceBorderHex = computed(() => colorToHexInput(selectedDesignerStyle.value.borderColor, 'E4E4E7'))
const selectedAppearanceBackgroundPreview = computed(() => hexInputToColor(selectedAppearanceBackgroundHex.value, 'transparent'))
const selectedAppearanceBackgroundColorInput = computed(() => hexInputToColor(selectedAppearanceBackgroundHex.value, '#ffffff'))
const selectedAppearanceBorderPreview = computed(() => {
  if (selectedDesignerStyle.value.borderStyle === 'none')
    return '#e4e4e7'
  return hexInputToColor(selectedAppearanceBorderHex.value, '#e4e4e7')
})
const formAppearanceBackgroundHex = computed(() => colorToHexInput(formStyle.value.backgroundColor, ''))
const formAppearanceBorderHex = computed(() => colorToHexInput(formStyle.value.borderColor, 'E4E4E7'))
const formAppearanceBackgroundPreview = computed(() => hexInputToColor(formAppearanceBackgroundHex.value, 'transparent'))
const formAppearanceBackgroundColorInput = computed(() => hexInputToColor(formAppearanceBackgroundHex.value, '#ffffff'))
const formAppearanceBorderPreview = computed(() => {
  if (formStyle.value.borderStyle === 'none')
    return '#e4e4e7'
  return hexInputToColor(formAppearanceBorderHex.value, '#e4e4e7')
})
const maxFormGridColumns = MAX_FORM_GRID_COLUMNS
const normalizedFormGridColumns = computed(() => normalizeFormLayoutColumns(props.schema.layout?.gridColumns || 2))
const isDatePickerField = computed(() => ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'quarter'].includes(selectedComponent.value?.componentKey))
const isTimePickerField = computed(() => ['time', 'timerange'].includes(selectedComponent.value?.componentKey))
const isTemporalField = computed(() => isDatePickerField.value || isTimePickerField.value)
const datePickerType = computed(() => {
  const key = selectedComponent.value?.componentKey
  const map = {
    date: 'date',
    datetime: 'datetime',
    daterange: 'daterange',
    datetimerange: 'datetimerange',
    month: 'month',
    year: 'year',
    quarter: 'quarter',
  }
  return map[key] || 'date'
})

watch(() => props.selectedId, () => {
  propertySearchHit.value = ''
  selectedBasicExpandedNames.value = isTabsLayout.value
    ? [...basicExpandedNames, 'tabs']
    : [...basicExpandedNames]
  // 存量选项里已有"值 ≠ 名称"时自动展开自定义值列，否则保持单列（值跟随名称）
  const options = selectedComponent.value?.props?.options || []
  showOptionValues.value = options.some(option =>
    String(option?.value ?? '') !== '' && String(option.value) !== String(option?.label ?? ''))
}, { immediate: true })

watch(() => props.objectCode, () => {
  mut.codeRuleRequestVersion += 1
  codeRules.value = []
  codeRuleLoading.value = false
  loadCodeRuleOptions()
})

watch(() => props.initialFormTab, (tab) => {
  if (!selectedComponent.value)
    formPropertyActiveTab.value = tab === 'events' ? 'events' : 'basic'
}, { immediate: true })

// 栅格列数选项/刻度与归一化函数已下沉 formLayoutConfig.js；表单项配置面板已拆分至 panels/FormLayoutPanel.vue
const componentSizeOptions = [
  { label: '默认', value: '' },
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
const tableDensityOptions = [
  { label: '紧凑', value: 'small' },
  { label: '默认', value: 'medium' },
  { label: '宽松', value: 'large' },
]
const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
]
const transferDataSourceOptions = [
  { label: '静态选项', value: 'static' },
  { label: '远程接口', value: 'remote' },
]
const widgetDataSourceOptions = [
  { label: '静态配置', value: 'static' },
  { label: '当前表单/详情数据', value: 'context' },
  { label: '远程接口', value: 'remote' },
]
const optionSourceTypeOptions = [
  { label: '静态选项', value: 'STATIC' },
  { label: '本页子表明细', value: 'CURRENT_CHILDREN' },
  { label: '其它表 / 业务对象', value: 'BUSINESS_OBJECT' },
  { label: '数据集 / 系统接口', value: 'QUERY_SOURCE' },
  { label: '手写系统接口', value: 'REMOTE' },
]
// ─── 子表关系下拉：当前画布 + 其它表单资产 + 对象明细关系，避免只能选本页已配子表 ─────
const childTableRelationOptions = computed(() => collectChildTableRelationOptions({
  subTableComponents: designerStore.subTableComponents,
  formAssets: designerStore.formAssets,
  relations: designerStore.relations,
}))
const childTableTargetObjectCode = computed(() => resolveChildRelationTargetObjectCode(
  selectedComponent.value?.props?.optionSource?.relationKey,
  childTableRelationOptions.value,
))
// childTableFieldOptions / loading 依赖 referenceTargetFieldsMap，定义见业务对象字段加载区
const dataBindablePageWidgetKeys = [
  'rich-text',
  'watermark',
  'vue-component',
  'html-tag',
  'markdown',
  'barcode',
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
]
const menuModeOptions = [
  { label: '纵向', value: 'vertical' },
  { label: '横向', value: 'horizontal' },
]
const alertTypeOptions = [
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]
const splitDirectionOptions = [
  { label: '横向', value: 'horizontal' },
  { label: '纵向', value: 'vertical' },
]
const watermarkFontStyleOptions = [
  { label: 'normal', value: 'normal' },
  { label: 'italic', value: 'italic' },
  { label: 'oblique 12deg', value: 'oblique 12deg' },
]
const watermarkTextAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const barcodeFormatOptions = [
  'CODE128',
  'CODE39',
  'EAN13',
  'EAN8',
  'UPC',
  'ITF14',
  'MSI',
  'pharmacode',
  'codabar',
].map(value => ({ label: value, value }))
const qrcodeErrorCorrectionOptions = [
  { label: 'L', value: 'L' },
  { label: 'M', value: 'M' },
  { label: 'Q', value: 'Q' },
  { label: 'H', value: 'H' },
]
const vuePreviewModeOptions = [
  { label: '安全模板预览', value: 'safe-template' },
  { label: 'Props 模板预览', value: 'live' },
  { label: '代码视图', value: 'code' },
]
const buttonTypeOptions = [
  { label: '默认 default', value: 'default' },
  { label: '主要 primary', value: 'primary' },
  { label: '信息 info', value: 'info' },
  { label: '成功 success', value: 'success' },
  { label: '警告 warning', value: 'warning' },
  { label: '错误 error', value: 'error' },
]
const propertySearchIndex = [
  { keys: ['标识', '名称', '绑定字段', 'field', '字段编码'], label: '基础配置 / 标识', selectedTab: 'basic', selectedExpand: ['identity'], formTab: 'basic', formExpand: ['assets'] },
  { keys: ['字段', '字段组件', '占位', 'placeholder', '默认', '默认值', '字典', 'dict', '组件属性', '标签', '标题', '公式', 'formula', '计算'], label: '基础配置 / 字段组件', selectedTab: 'basic', selectedExpand: ['field'] },
  { keys: ['选项', 'option', '新增选项', '静态选项', '选项来源', '选项列表', '远程接口', '业务对象', '其它表', '子表明细', '标签', '值'], label: '基础配置 / 字段组件（选项来源）', selectedTab: 'basic', selectedExpand: ['field'] },
  { keys: ['按钮', 'button', '块级', '禁用', '类型', '文案', '动作'], label: '基础配置 / 按钮组件', selectedTab: 'basic', selectedExpand: ['button'] },
  { keys: ['说明', '说明文本', '角标', '辅助', 'badge'], label: '基础配置 / 辅助展示', selectedTab: 'basic', selectedExpand: ['assist'] },
  { keys: ['日期', '时间', '格式', 'datetime', 'date', '范围', '年月日'], label: '基础配置 / 日期时间组件', selectedTab: 'basic', selectedExpand: ['temporal'] },
  { keys: ['crud字段', '查询字段', '搜索字段', '表格列字段', '编辑字段', '列标题', '列宽', '对齐', '固定', '省略', '排序'], label: '基础配置 / CRUD 字段配置', selectedTab: 'basic', selectedExpand: ['crud-field'] },
  { keys: ['crud', '查询', '搜索', '表格', '列表', '分页', '接口', 'api', '数据源', '基础路径', '行主键', '渲染模式', '表格尺寸', '编辑表单'], label: 'CRUD 配置', selectedTab: 'crud' },
  { keys: ['布局', '跨度', '栅格', '列数', '宽度', 'labelWidth', '标签宽度', '标签位置', '标签对齐', '打开方式'], label: '布局', selectedTab: 'basic', selectedExpand: ['gridQuick'], formTab: 'basic', formExpand: ['layout'] },
  { keys: ['校验', '唯一', '唯一校验', '不能重复', '必填', '只读', '隐藏', '状态', 'unique', 'required', 'readonly'], label: '可见性与校验', selectedTab: 'basic', selectedExpand: ['validation'], formTab: 'basic', formExpand: ['validation'] },
  { keys: ['事件', '交互', '联动', '弹窗事件', 'openModal', '生命周期', '加载', '提交', '字段变化', '自动执行', '回填', '接口结果'], label: '自动化 / 事件与联动', selectedTab: 'interaction', formTab: 'events' },
  { keys: ['样式', '颜色', '背景', '边框', '圆角', '阴影', '间距', 'padding', 'margin'], label: '样式配置', selectedTab: 'style', formTab: 'style', formExpand: ['appearance', 'spacing'] },
  { keys: ['表单资产', '多表单', '表单名称', '表单编码'], label: '表单属性 / 表单资产', formTab: 'basic', formExpand: ['assets'] },
  { keys: ['弹窗', '抽屉', 'modal', 'drawer', '打开方式'], label: '表单属性 / AiForm 布局', formTab: 'basic', formExpand: ['layout'] },
  { keys: ['反馈', '折叠', '最大显示字段', '行间距', '列间距', '表单列数'], label: '表单属性 / 间距与反馈', formTab: 'basic', formExpand: ['layout', 'validation'] },
  { keys: ['操作', '提交', '重置', '取消', '提交文案'], label: '表单属性 / 操作按钮', formTab: 'basic', formExpand: ['actions'] },
  { keys: ['位置', '尺寸', '最大宽度', '最小高度', '左', '上', 'x', 'y', '坐标', '填充', '适应内容'], label: '样式 / 位置与尺寸', selectedTab: 'style', formTab: 'style', selectedExpand: ['position'], formExpand: ['position'] },
  { keys: ['排版', '文字', '字号', '行高', '文字颜色', 'font', 'lineHeight'], label: '样式 / 文字排版', selectedTab: 'style', formTab: 'style', selectedExpand: ['typography'], formExpand: ['typography'] },
  { keys: ['外观', '透明度', '边框', '边框粗细', '粗细', '背景色', '圆角', '阴影', 'border', 'borderWidth', 'shadow', 'opacity'], label: '样式 / 外观与装饰', selectedTab: 'style', formTab: 'style', selectedExpand: ['appearance'], formExpand: ['appearance'] },
  { keys: ['自定义', 'class', 'css', 'style', '自定义样式'], label: '样式配置 / 自定义样式', selectedTab: 'style', formTab: 'style', formExpand: ['custom-style'] },
  { keys: ['源码', 'json', 'schema'], label: '源码入口在画布中间工具栏', selectedTab: 'basic', formTab: 'basic' },
]
const cardSizeOptions = [
  { label: '小 small', value: 'small' },
  { label: '中 medium', value: 'medium' },
  { label: '大 large', value: 'large' },
  { label: '巨大 huge', value: 'huge' },
]
const collapseArrowPlacementOptions = [
  { label: 'left', value: 'left' },
  { label: 'right', value: 'right' },
]
const collapseDisplayDirectiveOptions = [
  { label: 'if', value: 'if' },
  { label: 'show', value: 'show' },
]
const collapseTriggerAreaOptions = [
  { label: 'main', value: 'main' },
  { label: 'arrow', value: 'arrow' },
  { label: 'extra', value: 'extra' },
]
const datePickerTypeOptions = [
  { label: 'date', value: 'date' },
  { label: 'datetime', value: 'datetime' },
  { label: 'daterange', value: 'daterange' },
  { label: 'datetimerange', value: 'datetimerange' },
  { label: 'month', value: 'month' },
  { label: 'year', value: 'year' },
  { label: 'quarter', value: 'quarter' },
]
const pickerPlacementOptions = [
  { label: 'bottom-start', value: 'bottom-start' },
  { label: 'bottom', value: 'bottom' },
  { label: 'bottom-end', value: 'bottom-end' },
  { label: 'top-start', value: 'top-start' },
  { label: 'top', value: 'top' },
  { label: 'top-end', value: 'top-end' },
]
const pickerActionOptions = [
  { label: 'clear', value: 'clear' },
  { label: 'now', value: 'now' },
  { label: 'confirm', value: 'confirm' },
]
const tableAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const tableFixedOptions = [
  { label: '不固定', value: '' },
  { label: '左固定', value: 'left' },
  { label: '右固定', value: 'right' },
]
const triggerOptions = [
  { label: '点击组件时', value: 'click' },
  { label: '字段值变化时', value: 'change' },
  { label: '输入获得焦点时', value: 'focus' },
  { label: '输入失去焦点时', value: 'blur' },
  { label: '字段被清空时', value: 'clear' },
  { label: '组件初始化后', value: 'mounted' },
]
const actionOptions = [
  { label: '给目标字段赋值', value: 'setValue' },
  { label: '清空目标字段', value: 'clearValue' },
  { label: '刷新下拉选项', value: 'setOptions' },
  { label: '控制显示或隐藏', value: 'showHide' },
  { label: '控制启用或禁用', value: 'enableDisable' },
  { label: '打开弹窗表单', value: 'openModal' },
  { label: '请求后端接口', value: 'apiRequest' },
]
const expandTriggerOptions = [
  { label: '图标', value: 'icon' },
  { label: '整行', value: 'row' },
  { label: '图标和整行', value: 'both' },
]
const expandLayoutModeOptions = [
  { label: '单面板', value: 'single' },
  { label: 'Tabs', value: 'tabs' },
  { label: '上下堆叠', value: 'stack' },
]
const expandPanelTypeOptions = [
  { label: '描述信息', value: 'descriptions' },
  { label: '子表表格', value: 'table' },
  { label: '只读表单', value: 'form' },
  { label: '数量余额', value: 'quantity-balance' },
  { label: '数量流水', value: 'quantity-ledger' },
  { label: '数量锁定', value: 'quantity-lock' },
  { label: '多面板 Tabs', value: 'tabs' },
  { label: '自定义插槽', value: 'custom' },
]
const expandDataSourceTypeOptions = [
  { label: '当前行数据', value: 'row' },
  { label: '接口加载', value: 'api' },
  { label: '数量台账', value: 'quantity' },
  { label: '静态数据', value: 'static' },
]
const modalContentModeOptions = [
  { label: '复用当前表单', value: 'currentForm' },
  { label: '引用表单资产', value: 'formAsset' },
  { label: '选择画布组件', value: 'component' },
  { label: '空白弹窗，后续配置', value: 'empty' },
]
// 注：「下拉联动」已收敛到字段组件区的「级联选项」卡片（产出运行时消费的 props.cascade），
// 不再在交互规则里生成无运行时消费的 __events 配置
const interactionPresets = [
  { key: 'openModal', title: '打开弹窗', description: '按钮点击后打开表单弹窗或业务弹窗。' },
  { key: 'showHide', title: '显示隐藏', description: '根据当前值控制另一个字段是否显示。' },
  { key: 'apiRequest', title: '调用接口', description: '点击按钮或值变化后提交接口请求。' },
]
const colorSwatches = ['#ffffff', '#f8fafc', '#eff6ff', '#ecfdf5', '#fffbeb', '#fef2f2', '#dbe3ee', '#94a3b8', '#2563eb', '#16a34a', '#f59e0b', '#dc2626', 'rgba(255, 255, 255, 0)']
const shadowOptions = [
  { label: '无', value: '' },
  { label: '轻微', value: '0 4px 12px rgba(15, 23, 42, 0.06)' },
  { label: '标准', value: '0 10px 24px rgba(15, 23, 42, 0.10)' },
  { label: '强调', value: '0 16px 36px rgba(15, 23, 42, 0.16)' },
]
const spacingSides = [
  { key: 'Top', label: '上' },
  { key: 'Right', label: '右' },
  { key: 'Bottom', label: '下' },
  { key: 'Left', label: '左' },
]
const crudApiFields = [
  { key: 'list', label: '分页列表', placeholder: 'get@/employee/page' },
  { key: 'detail', label: '详情', placeholder: 'post@/employee/getById' },
  { key: 'add', label: '新增', placeholder: 'post@/employee/add' },
  { key: 'update', label: '更新', placeholder: 'post@/employee/edit' },
  { key: 'delete', label: '删除', placeholder: 'post@/employee/remove/:id' },
  { key: 'import', label: '导入', placeholder: 'post@/employee/import' },
  { key: 'export', label: '导出', placeholder: 'post@/employee/export' },
  { key: 'importTemplate', label: '导入模板', placeholder: 'get@/employee/importTemplate' },
]
const crudSwitchFields = [
  { key: 'showSearch', label: '显示查询区', defaultValue: true },
  { key: 'searchEnableCollapse', label: '查询折叠', defaultValue: true },
  { key: 'showPagination', label: '显示分页', defaultValue: true },
  { key: 'loadDetailOnEdit', label: '编辑前加载详情', defaultValue: true },
  { key: 'hideToolbar', label: '隐藏工具栏', defaultValue: false },
  { key: 'hideAdd', label: '隐藏新增', defaultValue: false },
  { key: 'hideBatchDelete', label: '隐藏批量删除', defaultValue: false },
  { key: 'hideSelection', label: '隐藏多选', defaultValue: false },
  { key: 'striped', label: '斑马纹', defaultValue: false },
  { key: 'bordered', label: '表格边框', defaultValue: false },
  { key: 'showRenderModeSwitch', label: '列表/卡片切换', defaultValue: true },
  { key: 'showImport', label: '显示导入', defaultValue: false },
  { key: 'showExport', label: '显示导出', defaultValue: false },
  { key: 'showExportTasks', label: '导出任务入口', defaultValue: true },
  { key: 'editShowFeedback', label: '编辑校验反馈', defaultValue: true },
  { key: 'hideModalFooter', label: '隐藏弹窗底部', defaultValue: false },
]
const rowTotalColumns = computed(() => normalizeGridCount(selectedComponent.value?.props?.columns || maxFormGridColumns))
const rowColumns = computed(() => (selectedComponent.value?.children || []).filter(child => child?.componentKey === 'col'))
const rowColumnCount = computed(() => rowColumns.value.length || 1)
const crudConfigFields = computed(() => collectCrudConfigFields(selectedComponent.value?.children || []))
const crudDescriptionFieldOptions = computed(() => crudConfigFields.value.map(field => ({
  label: `${field.label || field.fieldCode}（${field.fieldCode}）`,
  rawLabel: field.label || field.fieldCode,
  value: field.fieldCode,
  field: field.fieldCode,
  componentKey: field.componentKey,
})))
const firstCrudExpandPanel = computed(() => crudOptions.value?.expandConfig?.panels?.[0] || null)
const editingCrudField = computed(() => editingCrudFieldId.value ? getDesignerComponent(props.schema, editingCrudFieldId.value) : null)
const editingCrudConfig = computed(() => editingCrudField.value?.props?.__crudConfig || {})
const selectedOptions = computed(() => selectedComponent.value?.props?.options || [])
const isFieldInsideCrud = computed(() => isField.value && hasAncestorComponent(props.schema, props.selectedId, ['AiCrudPage', 'crudBlock']))
const selectedCrudFieldConfig = computed(() => isFieldInsideCrud.value ? selectedComponent.value?.props?.__crudConfig || {} : null)
const isOptionField = computed(() => ['select', 'radio', 'radioButton', 'checkbox', 'transfer', 'cascader', 'treeSelect'].includes(selectedComponent.value?.componentKey || ''))
const isTreeOptionField = computed(() => ['treeSelect', 'cascader'].includes(selectedComponent.value?.componentKey || ''))
const defaultOptionPageSize = computed(() => (
  isTreeOptionField.value ? DEFAULT_TREE_OPTION_PAGE_SIZE : DEFAULT_OPTION_PAGE_SIZE
))
// 人员组件（userSelect）也支持级联：按组织范围过滤人员
const isUserSelectCascadeField = computed(() => selectedComponent.value?.componentKey === 'userSelect')
const selectedOptionSourceType = computed(() => {
  const source = selectedComponent.value?.props?.optionSource || {}
  const type = String(source.type || '')
  if (['CURRENT_CHILDREN', 'current_children', 'currentChildren'].includes(type))
    return 'CURRENT_CHILDREN'
  if (type === 'QUERY_SOURCE' || type === 'query_source') {
    // 业务对象单独成项，避免和数据集/系统接口混在同一选择器里不好找
    if (String(source.sourceType || '').toUpperCase() === 'BUSINESS_OBJECT')
      return 'BUSINESS_OBJECT'
    return 'QUERY_SOURCE'
  }
  // 优先按 type 字段判断：切换到 REMOTE 时 api 初始为空字符串，
  // 若依赖 api 非空判断，computed 会立刻回落 STATIC，表现为"点了没反应"
  if (type === 'REMOTE' || type === 'remote')
    return 'REMOTE'
  if (source.api || source.url)
    return 'REMOTE'
  return 'STATIC'
})

// ─── 受管查询源选项：下拉选项从平台登记的查询源目录加载 ─────
const QUERY_SOURCE_TYPE_LABELS = { DATASET: '数据集', EXTERNAL_API: '系统接口', BUSINESS_OBJECT: '业务对象' }
const querySourceCatalog = ref([])
const querySourceCatalogLoading = ref(false)
const querySourceCatalogOptions = computed(() => querySourceCatalog.value
  // 业务对象已有独立入口「其它表 / 业务对象」，目录里不再重复列出
  .filter(item => item.sourceType !== 'BUSINESS_OBJECT')
  .map(item => ({
    label: `${QUERY_SOURCE_TYPE_LABELS[item.sourceType] || item.sourceType} · ${item.sourceName || item.sourceKey}`,
    value: `${item.sourceType}::${item.sourceKey}`,
  })))
const querySourceSelection = computed(() => {
  const source = selectedComponent.value?.props?.optionSource || {}
  return source.sourceType && source.sourceKey ? `${source.sourceType}::${source.sourceKey}` : ''
})

async function loadQuerySourceCatalog() {
  if (querySourceCatalogLoading.value)
    return
  querySourceCatalogLoading.value = true
  try {
    const response = await getLowcodeQuerySourceCatalog()
    querySourceCatalog.value = Array.isArray(response?.data) ? response.data : []
  }
  catch {
    querySourceCatalog.value = []
  }
  finally {
    querySourceCatalogLoading.value = false
  }
}

watch(selectedOptionSourceType, (type) => {
  if (type === 'QUERY_SOURCE' && !querySourceCatalog.value.length)
    loadQuerySourceCatalog()
}, { immediate: true })

function updateQuerySourceSelection(value) {
  const [sourceType = '', ...sourceKeyParts] = String(value || '').split('::')
  updatePageWidgetOptionSource({
    sourceType,
    sourceKey: sourceKeyParts.join('::'),
  })
  if (sourceType && sourceKeyParts.join('::'))
    loadQuerySourceMeta(sourceType, sourceKeyParts.join('::'))
}

// ─── 受管查询源元数据：选中查询源后自动加载字段列表，供值字段/显示字段下拉选择 ─────
const querySourceMetaFields = ref([])
const querySourceMetaParams = ref([])
const querySourceMetaLoading = ref(false)
const querySourceMetaError = ref('')

async function loadQuerySourceMeta(sourceType, sourceKey) {
  if (!sourceType || !sourceKey)
    return
  querySourceMetaLoading.value = true
  querySourceMetaError.value = ''
  try {
    const res = await getLowcodeQuerySourceMetadata({ sourceType, sourceKey })
    const meta = res?.data || {}
    const allFields = Array.isArray(meta.fields) ? meta.fields : []
    const visibleFields = allFields.filter(f => !f.sensitive)
    querySourceMetaFields.value = visibleFields.map(f => ({
      label: `${f.label || f.field}（${f.field}）`,
      value: f.field,
      dataType: f.dataType || f.type || '',
      length: f.length,
      precision: f.precision,
      type: f.type || f.dataType || '',
    }))
    // 空字段提示：帮用户定位问题
    if (allFields.length === 0) {
      querySourceMetaError.value = '该查询源未配置返回字段，请在数据管理 → 数据集中维护字段定义'
    }
    else if (visibleFields.length === 0) {
      querySourceMetaError.value = `共 ${allFields.length} 个字段均被标记为敏感，无法用于选项映射`
    }
    querySourceMetaParams.value = parseQuerySourceInputSchema(meta.inputSchemaJson)
    // 切换查询源后自动清理无效参数：仅保留契约声明的 key，避免运行时报“未声明字段”错误
    const allowedNames = new Set(querySourceMetaParams.value.map(p => p.name))
    const saved = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
    const cleaned = {}
    let changed = false
    for (const [key, value] of Object.entries(saved)) {
      if (allowedNames.has(key))
        cleaned[key] = value
      else
        changed = true
    }
    if (changed)
      updatePageWidgetOptionSource({ paramsText: JSON.stringify(cleaned) })
    // 元数据就绪后按当前值字段对齐本表存储类型（id/bigint → bigint，避免 varchar(64)）
    syncFieldStorageFromOptionValueField(
      selectedComponent.value?.props?.optionSource?.valueField || 'id',
    )
  }
  catch (err) {
    console.warn('[ForgePropertyPanel] 查询源元数据加载失败:', sourceType, sourceKey, err)
    querySourceMetaFields.value = []
    querySourceMetaParams.value = []
    querySourceMetaError.value = `元数据加载失败：${err?.message || '请稍后重试'}`
  }
  finally {
    querySourceMetaLoading.value = false
  }
}

watch(querySourceSelection, (val) => {
  if (!val) {
    querySourceMetaFields.value = []
    querySourceMetaParams.value = []
    querySourceMetaError.value = ''
    return
  }
  const [sourceType = '', ...rest] = String(val).split('::')
  loadQuerySourceMeta(sourceType, rest.join('::'))
}, { immediate: true })

// ─── 选项来源参数编辑器：结构化 key-value 行，替代手写 JSON ─────
function readJsonParams(text) {
  try {
    const parsed = JSON.parse(String(text || '{}'))
    return (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) ? parsed : {}
  }
  catch { return {} }
}

const optionSourceParamRows = computed(() => {
  const obj = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
  return Object.entries(obj).map(([key, value]) => ({ key, value: String(value ?? '') }))
})

function updateOptionSourceParam(key, newValue) {
  const obj = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
  obj[key] = newValue
  updatePageWidgetOptionSource({ paramsText: JSON.stringify(obj) })
}

function addOptionSourceParam() {
  const obj = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
  const n = Object.keys(obj).length
  let key = `param${n + 1}`
  while (key in obj) key = `param${Number(key.replace(/\D/g, '') || n) + 1}`
  obj[key] = ''
  updatePageWidgetOptionSource({ paramsText: JSON.stringify(obj) })
}

function removeOptionSourceParam(key) {
  const obj = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
  delete obj[key]
  updatePageWidgetOptionSource({ paramsText: JSON.stringify(obj) })
}

// 查询源参数：由 inputSchema 驱动，只展示契约声明的参数，用户只填值。
// 解析实现收敛到 @/components/ai-form/query-source-schema，与 FieldEventRulesEditor 同源。
const querySourceParamRows = computed(() => {
  const saved = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
  return querySourceMetaParams.value.map(p => ({
    name: p.name,
    label: p.label,
    type: p.type,
    required: p.required,
    value: saved[p.name] !== undefined ? String(saved[p.name]) : '',
  }))
})

function getQuerySourceParamOptions(currentValue = '') {
  const currentField = selectedFieldCode.value
  const options = formFieldOptions.value
    .filter(f => f.value !== currentField)
    .map(f => ({
      label: `${f.label}  →  \${${f.value}}`,
      value: `\${${f.value}}`,
    }))
  // 如果当前值是固定值（非 ${...} 引用），加入选项列表以便 select 正确回显
  if (currentValue && !/^\$\{.+\}$/.test(currentValue)) {
    options.unshift({ label: `固定值：${currentValue}`, value: currentValue })
  }
  return options
}

// ─── 查询参数弹窗：摘要统计 + 弹窗状态 ─────
const querySourceParamFilledCount = computed(() => querySourceParamRows.value.filter(p => p.value).length)
const querySourceParamMissingRequired = computed(() => querySourceParamRows.value.filter(p => p.required && !p.value).length)
const queryParamModalVisible = ref(false)
const queryParamModalDraft = ref({})

function openQueryParamModal() {
  const saved = readJsonParams(selectedComponent.value?.props?.optionSource?.paramsText)
  queryParamModalDraft.value = { ...saved }
  queryParamModalVisible.value = true
}

function updateQueryParamDraft(name, value) {
  queryParamModalDraft.value = { ...queryParamModalDraft.value, [name]: value ?? '' }
}

function confirmQueryParamModal() {
  updatePageWidgetOptionSource({ paramsText: JSON.stringify(queryParamModalDraft.value) })
  queryParamModalVisible.value = false
}

function cancelQueryParamModal() {
  queryParamModalVisible.value = false
}

const isManualOptionField = computed(() => isOptionField.value && !selectedComponent.value?.props?.dictType && selectedComponent.value?.props?.dataSourceType !== 'remote')

// ─── 级联选项（下拉级联）：一站式产出运行时 AiFormItem 消费的 props.cascade ─────
const optionLinkageEmptyStrategyOptions = [
  { label: '不加载选项（等选了上级再加载）', value: 'empty' },
  { label: '显示全部选项', value: 'all' },
]
const optionLinkageConfig = computed(() => {
  const raw = selectedComponent.value?.props?.cascade || {}
  return {
    enabled: raw.enabled === true,
    sourceField: raw.sourceField || '',
    mode: raw.mode === 'remoteParam' ? 'remoteParam' : 'parentDictCode',
    paramName: raw.paramName || '',
    emptyStrategy: raw.emptyStrategy || 'empty',
    clearOnParentChange: raw.clearOnParentChange !== false,
    includeChildren: raw.includeChildren !== false,
  }
})
const optionLinkageSourceFieldOptions = computed(() => collectRuntimeRuleFieldOptions(props.schema?.components || [])
  .filter(option => option.value !== selectedFieldCode.value))
// userSelect 级联：优先列出组织字段（orgTreeSelect），其它字段也允许选择
function walkComponentTree(components = [], visitor) {
  const walk = (items = []) => {
    (Array.isArray(items) ? items : []).forEach((comp) => {
      if (!comp || typeof comp !== 'object')
        return
      visitor(comp)
      walk(comp.children || [])
    })
  }
  walk(components)
}
function findComponentByFieldCode(components = [], fieldCode) {
  let found = null
  walkComponentTree(components, (comp) => {
    const field = comp.fieldBinding?.fieldCode || comp.field || comp.props?.field
    if (field === fieldCode)
      found = comp
  })
  return found
}
const hasOrgFieldInSchema = computed(() => {
  let found = false
  walkComponentTree(props.schema?.components, (comp) => {
    if (comp.componentKey === 'orgTreeSelect' && comp !== selectedComponent.value)
      found = true
  })
  return found
})
const userSelectCascadeSourceFieldOptions = computed(() => {
  const allOptions = collectRuntimeRuleFieldOptions(props.schema?.components || [])
    .filter(option => option.value !== selectedFieldCode.value)
  const orgFields = []
  const others = []
  allOptions.forEach((option) => {
    const comp = findComponentByFieldCode(props.schema?.components, option.value)
    if (comp?.componentKey === 'orgTreeSelect')
      orgFields.push({ ...option, label: `${option.label} ★组织字段` })
    else
      others.push(option)
  })
  return [...orgFields, ...others]
})
const optionLinkageApi = computed(() => String(selectedComponent.value?.props?.optionSource?.api || ''))
// 接口加载模式下选项来源的 api 输入框由级联卡片接管，避免两处输入框编辑同一个值
const optionLinkageApiManaged = computed(() => optionLinkageConfig.value.enabled && optionLinkageConfig.value.mode === 'remoteParam')
const optionLinkageSummary = computed(() => {
  const config = optionLinkageConfig.value
  if (!config.sourceField)
    return isUserSelectCascadeField.value ? '先选择①组织字段，人员将按该组织范围过滤' : '先选择①上级字段，级联才会生效'
  const sourceLabel = resolveOptionLinkageFieldLabel(config.sourceField)
  if (isUserSelectCascadeField.value) {
    const scope = config.includeChildren === false ? '直属人员' : '含子组织人员'
    return `选了【${sourceLabel}】后，人员仅展示该组织${scope}；组织重选时已选人员自动清空`
  }
  if (config.mode === 'remoteParam') {
    if (!optionLinkageApi.value)
      return `选了【${sourceLabel}】后自动请求接口刷新选项 —— 请在③中填写选项接口`
    const param = config.paramName ? `?${config.paramName}=所选值` : ''
    return `选了【${sourceLabel}】后自动请求 ${optionLinkageApi.value}${param} 并刷新选项`
  }
  return `选了【${sourceLabel}】后，只显示与所选值匹配的选项`
})
function resolveOptionLinkageFieldLabel(fieldCode) {
  const matched = optionLinkageSourceFieldOptions.value.find(option => option.value === fieldCode)
  return matched ? matched.label.replace(/（[^）]*）$/, '') : fieldCode
}
function buildOptionLinkageDefaults() {
  return { enabled: false, sourceField: '', mode: 'remoteParam', paramName: '', emptyStrategy: 'empty', clearOnParentChange: true, includeChildren: true }
}
function toggleOptionLinkage(enabled) {
  // userSelect 组件开启级联时自动锁定 orgFilter 模式
  const basePatch = { enabled }
  if (enabled && isUserSelectCascadeField.value)
    basePatch.mode = 'orgFilter'
  updateOptionLinkage(basePatch)
}
function updateOptionLinkage(patch = {}) {
  const current = selectedComponent.value?.props?.cascade || {}
  updateComponent({ props: { cascade: { ...buildOptionLinkageDefaults(), ...current, ...patch } } })
}
function updateOptionLinkageSourceField(field) {
  const patch = { sourceField: field || '' }
  // userSelect 组件级联自动锁定 orgFilter 模式，不需要 paramName
  if (isUserSelectCascadeField.value) {
    patch.mode = 'orgFilter'
    patch.paramName = ''
    updateOptionLinkage(patch)
    return
  }
  // 参数名默认跟随上级字段名（多数接口参数名与字段同名），用户已填过则不覆盖
  if (field && !optionLinkageConfig.value.paramName)
    patch.paramName = field
  updateOptionLinkage(patch)
}
function updateOptionLinkageMode(mode = 'remoteParam') {
  const nextMode = mode === 'remoteParam' ? 'remoteParam' : 'parentDictCode'
  const currentCascade = selectedComponent.value?.props?.cascade || {}
  const nextCascade = { ...buildOptionLinkageDefaults(), ...currentCascade, mode: nextMode }
  // 接口加载依赖远程选项来源：与 cascade 合并为同一次写入。
  // 若分两次 emit，第二次会基于尚未回传的旧 props.schema 操作，丢掉第一次的 cascade.mode
  if (nextMode === 'remoteParam' && selectedOptionSourceType.value !== 'REMOTE') {
    updateComponent({
      props: {
        cascade: nextCascade,
        optionSource: {
          ...(selectedComponent.value?.props?.optionSource || {}),
          type: 'REMOTE',
          api: optionLinkageApi.value,
        },
      },
    })
    return
  }
  updateComponent({ props: { cascade: nextCascade } })
}
function updateOptionLinkageApi(api = '') {
  // 联动接口直接落到选项来源，运行时按此接口动态加载选项
  updatePageWidgetOptionSource({ type: 'REMOTE', api: api || '' })
}
const supportsMultipleSelect = computed(() => isMultiSelectComponent(selectedComponent.value?.componentKey || ''))
const selectedMultipleEnabled = computed(() => {
  const propsData = selectedComponent.value?.props || {}
  return propsData.multiple === true || propsData.recordSelector?.multiple === true
})
const defaultValueSelectMultiple = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  if (key === 'checkbox')
    return true
  if (['select', 'dictSelect'].includes(key))
    return selectedMultipleEnabled.value
  return false
})
const selectedDictType = computed(() => String(selectedComponent.value?.props?.dictType || '').trim())
const defaultValueOptionsLoading = computed(() => {
  const dictType = selectedDictType.value
  return dictType ? dictDefaultOptionsLoading.value[dictType] === true : false
})
const defaultValueSelectOptions = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  if (!['select', 'dictSelect', 'radio', 'radioButton', 'checkbox'].includes(key))
    return []
  if (selectedComponent.value?.props?.dataSourceType === 'remote')
    return []
  const staticOptions = normalizeDefaultValueOptions(selectedOptions.value || [])
  if (staticOptions.length)
    return staticOptions
  const dictType = selectedDictType.value
  return dictType ? dictDefaultOptions.value[dictType] || [] : []
})
const selectedGenerationConfig = computed(() => selectedComponent.value?.props?.generation || {})
const selectedGenerationEnabled = computed(() => selectedGenerationConfig.value.enabled === true)
// 自动编号只适用于可存字符串单据号的文本类控件；已启用配置的组件仍展示，避免历史配置被藏住
const AUTO_GENERATION_COMPONENT_KEYS = new Set([
  'input', 'textarea', 'password', 'phone', 'email', 'idCard', 'url', 'barcodeScanner', 'text',
])
const supportsAutoGeneration = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  return AUTO_GENERATION_COMPONENT_KEYS.has(key) || selectedGenerationEnabled.value
})
const selectedGenerationRuleCode = computed(() => selectedGenerationConfig.value.ruleCode || '')
const codeRuleOptions = computed(() => codeRules.value.map(rule => ({
  label: `${rule.ruleName || rule.ruleCode}（${rule.ruleCode}）`,
  value: rule.ruleCode,
  rule,
})))
const selectedGenerationRule = computed(() => {
  const ruleCode = selectedGenerationRuleCode.value
  return codeRules.value.find(rule => rule.ruleCode === ruleCode) || null
})
/** 列表画布（GridBlockRenderer）专属的栅格属性：表单画布（AiFormLayoutNodes / n-grid）不消费，表单侧统一属性面板排除 */
const GRID_LIST_ONLY_PROPS = ['cellMinHeight', 'alignItems', 'justifyItems', 'showCellBorder', 'cellBackground']

/** SpecPropertyPanel 排除的属性：已由主面板或表单专用逻辑管理，避免重复编辑入口 */
const specPanelExcludedProps = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  if (key === 'button')
    // 按钮文字/类型/尺寸/块级/禁用已在"按钮组件"折叠项配置；
    // secondary/dashed/round/loading 等由"更多属性"抽屉的 spec 面板补齐
    return ['text', 'type', 'size', 'block', 'disabled']
  if (key === 'dictSelect')
    return ['dictType'] // 字典类型由表单侧字典联动逻辑管理
  // 字段组件：占位提示/组件尺寸/可清空/显示反馈已在主面板"字段组件"折叠项配置；
  // 选项类组件的选项由"选项来源"统一管理 —— 抽屉只保留主面板没有的属性，避免重复入口
  if (isField.value) {
    const excluded = ['placeholder', 'size', 'clearable', 'showFeedback']
    // 前缀/后缀/图标/加减按钮/下拉插槽已在「字段组件」主面板配置，避免更多属性里重复
    if (supportsFieldAffix.value) {
      excluded.push(
        'prefix',
        'suffix',
        'prefixIcon',
        'suffixIcon',
        'clearIcon',
        'inputType',
        'passwordVisibleIcon',
        'passwordInvisibleIcon',
        'pair',
        'separator',
      )
    }
    if (supportsFieldShowButton.value)
      excluded.push('showButton')
    if (supportsFieldSelectSlots.value)
      excluded.push('dropdownHeader', 'dropdownAction', 'emptyText', 'arrowIcon')
    if (supportsFieldDatePickerSlots.value) {
      excluded.push(
        'dateIcon',
        'dateFooter',
        'prevYearIcon',
        'prevMonthIcon',
        'nextMonthIcon',
        'nextYearIcon',
        'separator',
      )
    }
    if (supportsMultipleSelect.value)
      excluded.push('multiple')
    if (isOptionField.value)
      excluded.push('options')
    return excluded
  }
  // 栅格：columns/gutter/rowGap 已在"栅格快捷配置"内联配置，列表画布专属属性表单不消费
  if (isRowLayout.value)
    return ['columns', 'gutter', 'rowGap', ...GRID_LIST_ONLY_PROPS]
  return []
})

/** 当前组件是否有可配置的 spec 属性（驱动"更多属性"按钮显隐） */
const hasSpecPanelProps = computed(() => {
  const spec = getComponentSpec(selectedComponent.value?.componentKey || '')
  const properties = spec?.propsSchema?.properties
  if (!properties)
    return false
  const exclude = new Set(specPanelExcludedProps.value)
  return Object.keys(properties).some(key => !exclude.has(key))
})

/** SpecPropertyPanel 属性更新：写回选中组件 props */
function handleSpecPropUpdate({ key, value }) {
  if (!key)
    return
  updateComponent({ props: { [key]: value === '' ? undefined : value } })
}

/** 栅格内联属性更新：总列数变化走 updateRowTotalColumns 联动收敛各列 span，其余直接写回 props */
function handleGridPropUpdate({ key, value }) {
  if (!key)
    return
  if (key === 'columns') {
    updateRowTotalColumns(value)
    return
  }
  updateComponent({ props: { [key]: value === '' ? undefined : value } })
}
const runtimeRuleFieldOptions = computed(() => collectRuntimeRuleFieldOptions(props.schema?.components || []))
const isDictLikeField = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  return ['select', 'dictSelect', 'radio', 'checkbox', 'cascader'].includes(key)
})
const isObjectReferenceField = computed(() => selectedComponent.value?.componentKey === 'objectReference')
const isRecordSelectorField = computed(() => selectedComponent.value?.componentKey === 'recordSelector')
const isRelationField = computed(() => isObjectReferenceField.value || isRecordSelectorField.value)
const referenceObjectCode = computed(() => selectedComponent.value?.props?.referenceObjectCode || '')
const referenceDisplayField = computed(() => selectedComponent.value?.props?.referenceDisplayField || '')
const referenceValueField = computed(() => selectedComponent.value?.props?.referenceValueField || '')
const referenceFieldMappings = computed(() => {
  const mappings = selectedComponent.value?.props?.fieldMappings
  return Array.isArray(mappings) ? mappings : []
})
// recordSelector 配置（弹窗模式）
const recordSelectorConfigObj = computed(() => selectedComponent.value?.props?.recordSelector || {})
const recordSelectorObjectCode = computed(() => recordSelectorConfigObj.value.objectCode || '')
const recordSelectorDisplayFields = computed(() => recordSelectorConfigObj.value.displayFields || [])
const recordSelectorKeywordFields = computed(() => recordSelectorConfigObj.value.keywordFields || [])
const recordSelectorValueField = computed(() => recordSelectorConfigObj.value.valueField || '')
const recordSelectorMultiple = computed(() => recordSelectorConfigObj.value.multiple === true)
const businessObjectOptions = ref([])
const businessObjectLoading = ref(false)
const referenceTargetFieldsMap = ref({})
// ─── 子表字段下拉：优先 columns，无列时回落目标对象字段（须在 referenceTargetFieldsMap 之后）─────
const childTableTargetFieldsLoading = computed(() => {
  const objectCode = childTableTargetObjectCode.value
  return !!objectCode && !!referenceTargetFieldsMap.value[objectCode]?.loading
})
const childTableFieldOptions = computed(() => {
  const relationKey = selectedComponent.value?.props?.optionSource?.relationKey
  const objectCode = childTableTargetObjectCode.value
  return collectChildTableFieldOptions({
    relationKey,
    relationOptions: childTableRelationOptions.value,
    targetFieldOptions: objectCode ? (referenceTargetFieldsMap.value[objectCode]?.options || []) : [],
  })
})
const showOptionLoadLimits = computed(() => {
  if (!isOptionField.value || selectedComponent.value?.componentKey === 'transfer')
    return false
  return ['BUSINESS_OBJECT', 'QUERY_SOURCE', 'REMOTE'].includes(selectedOptionSourceType.value)
})
const treeOptionFieldSelectOptions = computed(() => {
  if (selectedOptionSourceType.value === 'CURRENT_CHILDREN')
    return childTableFieldOptions.value || []
  if (['BUSINESS_OBJECT', 'QUERY_SOURCE'].includes(selectedOptionSourceType.value))
    return querySourceMetaFields.value || []
  return []
})
const referenceTargetFieldLoading = computed(() => {
  return !!referenceTargetFieldsMap.value[referenceObjectCode.value]?.loading
})
const referenceTargetFieldOptions = computed(() => referenceTargetFieldsMap.value[referenceObjectCode.value]?.options || [])
const recordSelectorTargetFieldLoading = computed(() => {
  return !!referenceTargetFieldsMap.value[recordSelectorObjectCode.value]?.loading
})
const recordSelectorTargetFieldOptions = computed(() => referenceTargetFieldsMap.value[recordSelectorObjectCode.value]?.options || [])

const generationFillPolicyOptions = [
  { label: '为空时生成', value: 'EMPTY_ONLY' },
  { label: '总是覆盖', value: 'OVERWRITE' },
]

const generationTriggerOptions = [
  { label: '新增时', value: 'ON_CREATE' },
]


watch(() => props.selectedId, () => {
  codeRulePreview.value = null
  if (selectedGenerationEnabled.value)
    loadCodeRuleOptions()
})

watch(selectedGenerationRuleCode, () => {
  codeRulePreview.value = null
})

function collectRuntimeRuleFieldOptions(components = []) {
  const options = []
  const seen = new Set()
  const walk = (items = []) => {
    ;(Array.isArray(items) ? items : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const field = component.fieldBinding?.fieldCode || component.field || component.props?.field
      if (field && !seen.has(field)) {
        seen.add(field)
        options.push({
          label: `${component.label || component.props?.label || field}（${field}）`,
          value: field,
        })
      }
      walk(component.children || [])
    })
  }
  walk(components)
  return options
}


  return {
    __impl,
    mut,
    designerStore,
    schema,
    selectedId,
    fields,
    relations,
    objectCode,
    message,
    createBitableSvgIcon,
    BitableDragIcon,
    BitableStyleIcon,
    BitableSelectIcon,
    BitableNumberIcon,
    BitableCalendarIcon,
    BitableAttachmentIcon,
    BitableMemberIcon,
    BitableLookupIcon,
    advancedConfigVisible,
    componentPropsVisible,
    crudFieldDrawerVisible,
    fieldFormulaPanelVisible,
    crudDescriptionFieldPanelOpen,
    editingCrudFieldId,
    propertyActiveTab,
    formPropertyActiveTab,
    basicExpandedNames,
    showOptionValues,
    selectedBasicExpandedNames,
    formBasicExpandedNames,
    formStyleExpandedNames,
    allSelectedBasicExpandNames,
    allFormBasicExpandNames,
    allFormStyleExpandNames,
    commonValidationOptions,
    propertySearchKeyword,
    propertySearchHit,
    selectedCodeDraft,
    selectedCodeDraftTarget,
    schemaCodeDraft,
    schemaCodeDirty,
    sourceError,
    sourceModalVisible,
    dictDefaultOptions,
    dictDefaultOptionsLoading,
    codeRules,
    codeRuleLoading,
    codeRulePreview,
    codeRulePreviewing,
    selectedComponent,
    isField,
    fieldStructureLocked,
    isLayout,
    isCrudBlock,
    isSubTable,
    isRowLayout,
    isColumnLayout,
    isCardLayout,
    isTabsLayout,
    isCollapseLayout,
    isButtonComponent,
    isPageWidget,
    selectedLabel,
    componentTypeLabel,
    panelDescription,
    crudApiConfig,
    crudOptions,
    selectedDesignerStyle,
    formStyle,
    formAssets,
    formGovernanceSettings,
    formPermissionConfig,
    formFieldRuleRows,
    formEventRows,
    formFieldEventRows,
    hasFormInitConfig,
    formFieldLinkageRows,
    formOfflineDraftConfig,
    formFieldOptions,
    formFieldCatalog,
    selectedFieldCode,
    selectedFieldAsset,
    switchableComponentGroups,
    componentTypeLabelMap,
    canSwitchComponentType,
    switchableComponentOptions,
    supportsFieldMaxLength,
    supportsFieldNumberRange,
    supportsFieldAffix,
    supportsFieldShowButton,
    supportsFieldSelectSlots,
    supportsFieldDatePickerSlots,
    selectedFieldMaxLength,
    selectedFormulaConfig,
    selectedFormulaSummary,
    formulaPanelFields,
    formEventHookOptions,
    formEventActionOptions,
    formScriptOptions,
    formAssetOptions,
    layoutChildren,
    interactionRules,
    defaultTrigger,
    componentTargetOptions,
    selectedDrivenRuntimeRules,
    dictTypeFields,
    selectedComponentCodeRaw,
    selectedCodeText,
    schemaCodeRaw,
    schemaCodeText,
    selectedOpacityPercent,
    formOpacityPercent,
    selectedDesignerTranslate,
    formTranslate,
    selectedPaddingBox,
    selectedMarginBox,
    formPaddingBox,
    formMarginBox,
    selectedAppearanceBackgroundHex,
    selectedAppearanceBorderHex,
    selectedAppearanceBackgroundPreview,
    selectedAppearanceBackgroundColorInput,
    selectedAppearanceBorderPreview,
    formAppearanceBackgroundHex,
    formAppearanceBorderHex,
    formAppearanceBackgroundPreview,
    formAppearanceBackgroundColorInput,
    formAppearanceBorderPreview,
    maxFormGridColumns,
    normalizedFormGridColumns,
    isDatePickerField,
    isTimePickerField,
    isTemporalField,
    datePickerType,
    componentSizeOptions,
    tableDensityOptions,
    requestMethodOptions,
    transferDataSourceOptions,
    widgetDataSourceOptions,
    optionSourceTypeOptions,
    childTableRelationOptions,
    childTableTargetObjectCode,
    dataBindablePageWidgetKeys,
    menuModeOptions,
    alertTypeOptions,
    splitDirectionOptions,
    watermarkFontStyleOptions,
    watermarkTextAlignOptions,
    barcodeFormatOptions,
    qrcodeErrorCorrectionOptions,
    vuePreviewModeOptions,
    buttonTypeOptions,
    propertySearchIndex,
    cardSizeOptions,
    collapseArrowPlacementOptions,
    collapseDisplayDirectiveOptions,
    collapseTriggerAreaOptions,
    datePickerTypeOptions,
    pickerPlacementOptions,
    pickerActionOptions,
    tableAlignOptions,
    tableFixedOptions,
    triggerOptions,
    actionOptions,
    expandTriggerOptions,
    expandLayoutModeOptions,
    expandPanelTypeOptions,
    expandDataSourceTypeOptions,
    modalContentModeOptions,
    interactionPresets,
    colorSwatches,
    shadowOptions,
    spacingSides,
    crudApiFields,
    crudSwitchFields,
    rowTotalColumns,
    rowColumns,
    rowColumnCount,
    crudConfigFields,
    crudDescriptionFieldOptions,
    firstCrudExpandPanel,
    editingCrudField,
    editingCrudConfig,
    selectedOptions,
    isFieldInsideCrud,
    selectedCrudFieldConfig,
    isOptionField,
    isTreeOptionField,
    defaultOptionPageSize,
    isUserSelectCascadeField,
    selectedOptionSourceType,
    QUERY_SOURCE_TYPE_LABELS,
    querySourceCatalog,
    querySourceCatalogLoading,
    querySourceCatalogOptions,
    querySourceSelection,
    loadQuerySourceCatalog,
    updateQuerySourceSelection,
    querySourceMetaFields,
    querySourceMetaParams,
    querySourceMetaLoading,
    querySourceMetaError,
    loadQuerySourceMeta,
    readJsonParams,
    optionSourceParamRows,
    updateOptionSourceParam,
    addOptionSourceParam,
    removeOptionSourceParam,
    querySourceParamRows,
    getQuerySourceParamOptions,
    querySourceParamFilledCount,
    querySourceParamMissingRequired,
    queryParamModalVisible,
    queryParamModalDraft,
    openQueryParamModal,
    updateQueryParamDraft,
    confirmQueryParamModal,
    cancelQueryParamModal,
    isManualOptionField,
    optionLinkageEmptyStrategyOptions,
    optionLinkageConfig,
    optionLinkageSourceFieldOptions,
    walkComponentTree,
    findComponentByFieldCode,
    hasOrgFieldInSchema,
    userSelectCascadeSourceFieldOptions,
    optionLinkageApi,
    optionLinkageApiManaged,
    optionLinkageSummary,
    resolveOptionLinkageFieldLabel,
    buildOptionLinkageDefaults,
    toggleOptionLinkage,
    updateOptionLinkage,
    updateOptionLinkageSourceField,
    updateOptionLinkageMode,
    updateOptionLinkageApi,
    supportsMultipleSelect,
    selectedMultipleEnabled,
    defaultValueSelectMultiple,
    selectedDictType,
    defaultValueOptionsLoading,
    defaultValueSelectOptions,
    selectedGenerationConfig,
    selectedGenerationEnabled,
    supportsAutoGeneration,
    selectedGenerationRuleCode,
    codeRuleOptions,
    selectedGenerationRule,
    GRID_LIST_ONLY_PROPS,
    specPanelExcludedProps,
    hasSpecPanelProps,
    handleSpecPropUpdate,
    handleGridPropUpdate,
    runtimeRuleFieldOptions,
    isDictLikeField,
    isObjectReferenceField,
    isRecordSelectorField,
    isRelationField,
    referenceObjectCode,
    referenceDisplayField,
    referenceValueField,
    referenceFieldMappings,
    recordSelectorConfigObj,
    recordSelectorObjectCode,
    recordSelectorDisplayFields,
    recordSelectorKeywordFields,
    recordSelectorValueField,
    recordSelectorMultiple,
    businessObjectOptions,
    businessObjectLoading,
    referenceTargetFieldsMap,
    childTableTargetFieldsLoading,
    childTableFieldOptions,
    showOptionLoadLimits,
    treeOptionFieldSelectOptions,
    referenceTargetFieldLoading,
    referenceTargetFieldOptions,
    recordSelectorTargetFieldLoading,
    recordSelectorTargetFieldOptions,
    generationFillPolicyOptions,
    generationTriggerOptions,
    collectRuntimeRuleFieldOptions,
  }
}
