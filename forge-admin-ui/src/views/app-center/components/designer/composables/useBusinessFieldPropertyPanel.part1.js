/** BusinessFieldPropertyPanel.vue setup part 1. */
import { ArrowUndoOutline, SaveOutline } from '@vicons/ionicons5'
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList } from '@/api/business-app'
import { previewFormula, validateFormula } from '@/api/formula'
import DictTypeSelect from '@/components/lowcode-builder/shared/DictTypeSelect.vue'
import FieldTypeSelect from '@/components/lowcode-builder/shared/FieldTypeSelect.vue'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'
import { COMMON_VALIDATION_PRESETS, getValidationPreset } from '@/utils/validation-presets'
import { camelToSnake } from '../form-first/namingUtils'
import FormulaConfigPanel from '../formula/FormulaConfigPanel.vue'
import FormulaDebuggerPanel from '../formula/FormulaDebuggerPanel.vue'
import FormulaDependencyGraph from '../formula/FormulaDependencyGraph.vue'
import FormulaExecutionLogDrawer from '../formula/FormulaExecutionLogDrawer.vue'
import FieldDefaultValueEditor from '../forge-form-designer/panels/FieldDefaultValueEditor.vue'
export function applyBusinessFieldPropertyPanelPart1(props, emit) {
  const __impl = {}
  const mut = {
    baseline: '',
    resetting: false,
    formulaPreviewTimer: null,
    formulaPreviewInitializing: false,
    businessObjectLoadPromise: null,
    partsReady: false,
  }
  const appendExpressionVariables = (...args) => __impl.appendExpressionVariables(...args)
  const appendFormulaDependsOn = (...args) => __impl.appendFormulaDependsOn(...args)
  const appendFormulaFunctionRefs = (...args) => __impl.appendFormulaFunctionRefs(...args)
  const appendFormulaVariable = (...args) => __impl.appendFormulaVariable(...args)
  const applyFieldTypeDefaults = (...args) => __impl.applyFieldTypeDefaults(...args)
  const autoInferRelationFields = (...args) => __impl.autoInferRelationFields(...args)
  const buildDependFieldLabel = (...args) => __impl.buildDependFieldLabel(...args)
  const buildFormulaPreviewPayload = (...args) => __impl.buildFormulaPreviewPayload(...args)
  const buildRecordSelectorConfig = (...args) => __impl.buildRecordSelectorConfig(...args)
  const clearPreviewTimer = (...args) => __impl.clearPreviewTimer(...args)
  const collectExpressionFieldCodes = (...args) => __impl.collectExpressionFieldCodes(...args)
  const createDefaultCascade = (...args) => __impl.createDefaultCascade(...args)
  const createDefaultRecordSelector = (...args) => __impl.createDefaultRecordSelector(...args)
  const extractFormulaVariables = (...args) => __impl.extractFormulaVariables(...args)
  const fillPreviewSampleValues = (...args) => __impl.fillPreviewSampleValues(...args)
  const formatPreviewValue = (...args) => __impl.formatPreviewValue(...args)
  const guessFormulaSampleValue = (...args) => __impl.guessFormulaSampleValue(...args)
  const handlePreviewFormula = (...args) => __impl.handlePreviewFormula(...args)
  const handleUnifiedDisplayFieldChange = (...args) => __impl.handleUnifiedDisplayFieldChange(...args)
  const handleUnifiedObjectCodeChange = (...args) => __impl.handleUnifiedObjectCodeChange(...args)
  const handleUnifiedValueFieldChange = (...args) => __impl.handleUnifiedValueFieldChange(...args)
  const hasPreviewInputValue = (...args) => __impl.hasPreviewInputValue(...args)
  const includesAny = (...args) => __impl.includesAny(...args)
  const initializePreviewForm = (...args) => __impl.initializePreviewForm(...args)
  const isFormulaReservedToken = (...args) => __impl.isFormulaReservedToken(...args)
  const isManagedFormulaFunctionName = (...args) => __impl.isManagedFormulaFunctionName(...args)
  const isSameStringArray = (...args) => __impl.isSameStringArray(...args)
  const loadBusinessObjectOptions = (...args) => __impl.loadBusinessObjectOptions(...args)
  const loadReferenceTargetFields = (...args) => __impl.loadReferenceTargetFields(...args)
  const mappingsToLines = (...args) => __impl.mappingsToLines(...args)
  const normalizeCascade = (...args) => __impl.normalizeCascade(...args)
  const normalizeMappingList = (...args) => __impl.normalizeMappingList(...args)
  const normalizeSearchParams = (...args) => __impl.normalizeSearchParams(...args)
  const normalizeTextList = (...args) => __impl.normalizeTextList(...args)
  const parseMappingLines = (...args) => __impl.parseMappingLines(...args)
  const parseSearchParamLines = (...args) => __impl.parseSearchParamLines(...args)
  const parseTextList = (...args) => __impl.parseTextList(...args)
  const pruneRecordSelectorConfig = (...args) => __impl.pruneRecordSelectorConfig(...args)
  const resetPreviewForm = (...args) => __impl.resetPreviewForm(...args)
  const resolveDependFieldMeta = (...args) => __impl.resolveDependFieldMeta(...args)
  const resolvePreviewInputType = (...args) => __impl.resolvePreviewInputType(...args)
  const schedulePreviewCalculation = (...args) => __impl.schedulePreviewCalculation(...args)
  const searchParamsToLines = (...args) => __impl.searchParamsToLines(...args)
  const stripFormulaStringLiterals = (...args) => __impl.stripFormulaStringLiterals(...args)
  const switchRelationSelectionMode = (...args) => __impl.switchRelationSelectionMode(...args)
  const syncFormulaDependsOnFromExpression = (...args) => __impl.syncFormulaDependsOnFromExpression(...args)
  const updateCascadeEnabled = (...args) => __impl.updateCascadeEnabled(...args)
  const updateCascadeIncludeChildren = (...args) => __impl.updateCascadeIncludeChildren(...args)

  // createFieldForm / normalizePayload / watch(changed) 在 part1 顶层即跑；
  // 下列纯函数须先于 form 与 watch 挂到 __impl，不能等 part2 再注册。
  function bootstrapParseTextList(value) {
    return String(value || '')
      .split(/[\n,，]/)
      .map(item => item.trim())
      .filter(Boolean)
  }
  function bootstrapNormalizeTextList(value) {
    if (Array.isArray(value))
      return value.map(item => String(item || '').trim()).filter(Boolean)
    if (typeof value === 'string')
      return bootstrapParseTextList(value)
    return []
  }
  function bootstrapNormalizeMappingList(value) {
    if (Array.isArray(value)) {
      return value
        .map(item => ({
          sourceField: String(item?.sourceField || item?.source || '').trim(),
          targetField: String(item?.targetField || item?.target || '').trim(),
        }))
        .filter(item => item.sourceField && item.targetField)
    }
    if (value && typeof value === 'object') {
      return Object.entries(value)
        .map(([sourceField, targetField]) => ({
          sourceField: String(sourceField || '').trim(),
          targetField: String(targetField || '').trim(),
        }))
        .filter(item => item.sourceField && item.targetField)
    }
    return []
  }
  function bootstrapNormalizeSearchParams(value) {
    if (!value)
      return {}
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return Object.entries(value).reduce((result, [key, item]) => {
        const paramKey = String(key || '').trim()
        if (paramKey && item !== undefined && item !== null && item !== '')
          result[paramKey] = item
        return result
      }, {})
    }
    return {}
  }
  function bootstrapCreateDefaultCascade(source = {}) {
    return {
      enabled: !!source.enabled,
      sourceField: source.sourceField || '',
      mode: source.mode || source.matchMode || 'linkedDict',
      paramName: source.paramName || '',
      includeChildren: source.includeChildren !== false,
      clearOnParentChange: source.clearOnParentChange !== false,
    }
  }
  function bootstrapCreateDefaultRecordSelector(source = {}) {
    const config = source && typeof source === 'object' ? source : {}
    return {
      suiteCode: config.suiteCode || '',
      objectCode: config.objectCode || '',
      valueField: config.valueField || 'id',
      labelField: config.labelField || config.labelSourceField || '',
      targetLabelField: config.targetLabelField || config.labelTargetField || '',
      displayFields: bootstrapNormalizeTextList(config.displayFields),
      keywordFields: bootstrapNormalizeTextList(config.keywordFields),
      fieldMappings: bootstrapNormalizeMappingList(config.fieldMappings || config.mappings),
      searchParams: bootstrapNormalizeSearchParams(config.searchParams),
    }
  }
  function bootstrapMappingsToLines(value) {
    return bootstrapNormalizeMappingList(value)
      .map(item => `${item.sourceField}=${item.targetField}`)
      .join('\n')
  }
  function bootstrapSearchParamsToLines(value) {
    return Object.entries(bootstrapNormalizeSearchParams(value))
      .map(([key, item]) => `${key}=${item}`)
      .join('\n')
  }
  function bootstrapNormalizeCascade(source = {}) {
    const cascade = bootstrapCreateDefaultCascade(source)
    if (!cascade.enabled || !cascade.sourceField)
      return { ...cascade, enabled: false }
    if (cascade.mode === 'orgFilter') {
      cascade.paramName = ''
      return cascade
    }
    if (cascade.mode !== 'remoteParam')
      cascade.paramName = ''
    return cascade
  }
  function bootstrapParseMappingLines(value) {
    return String(value || '')
      .split(/\n/)
      .map(item => item.trim())
      .filter(Boolean)
      .map((item) => {
        const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
        return {
          sourceField: String(parts[0] || '').trim(),
          targetField: String(parts[1] || '').trim(),
        }
      })
      .filter(item => item.sourceField && item.targetField)
  }
  function bootstrapParseSearchParamLines(value) {
    const text = String(value || '').trim()
    if (!text)
      return {}
    if (text.startsWith('{')) {
      try {
        return bootstrapNormalizeSearchParams(JSON.parse(text))
      }
      catch {
        return {}
      }
    }
    return text
      .split(/\n/)
      .map(item => item.trim())
      .filter(Boolean)
      .reduce((result, item) => {
        const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
        const key = String(parts[0] || '').trim()
        const paramValue = String(parts[1] || '').trim()
        if (key && paramValue)
          result[key] = paramValue
        return result
      }, {})
  }
  function bootstrapPruneRecordSelectorConfig(config = {}) {
    const result = {}
    ;['suiteCode', 'objectCode', 'valueField', 'labelField', 'targetLabelField'].forEach((key) => {
      if (config[key])
        result[key] = config[key]
    })
    if (config.displayFields?.length)
      result.displayFields = config.displayFields
    if (config.keywordFields?.length)
      result.keywordFields = config.keywordFields
    if (config.fieldMappings?.length)
      result.fieldMappings = config.fieldMappings
    if (config.searchParams && Object.keys(config.searchParams).length)
      result.searchParams = config.searchParams
    return result
  }
  function bootstrapBuildRecordSelectorConfig(source) {
    const isRecordSelector = source.fieldType === 'RECORD_SELECTOR' || source.componentType === 'recordSelector'
    if (!isRecordSelector && source.fieldType !== 'REFERENCE' && source.componentType !== 'objectReference')
      return null
    const config = bootstrapCreateDefaultRecordSelector({
      suiteCode: source.recordSelectorSuiteCode,
      objectCode: source.recordSelectorObjectCode,
      valueField: source.recordSelectorValueField,
      labelField: source.recordSelectorLabelField,
      targetLabelField: source.recordSelectorTargetLabelField,
      displayFields: source.recordSelectorDisplayFields,
      keywordFields: source.recordSelectorKeywordFields,
      fieldMappings: bootstrapParseMappingLines(source.recordSelectorMappingsText),
      searchParams: bootstrapParseSearchParamLines(source.recordSelectorSearchParamsText),
    })
    if (isRecordSelector && !config.objectCode)
      return null
    const pruned = bootstrapPruneRecordSelectorConfig(config)
    return Object.keys(pruned).length ? pruned : null
  }
  // resetForm / watch(changed) 可能在 part2 注册前触发；预览相关先挂空实现
  function bootstrapResetPreviewForm() {}
  function bootstrapSyncFormulaDependsOnFromExpression() {}
  __impl.parseTextList = bootstrapParseTextList
  __impl.normalizeTextList = bootstrapNormalizeTextList
  __impl.normalizeMappingList = bootstrapNormalizeMappingList
  __impl.normalizeSearchParams = bootstrapNormalizeSearchParams
  __impl.mappingsToLines = bootstrapMappingsToLines
  __impl.searchParamsToLines = bootstrapSearchParamsToLines
  __impl.createDefaultCascade = bootstrapCreateDefaultCascade
  __impl.createDefaultRecordSelector = bootstrapCreateDefaultRecordSelector
  __impl.normalizeCascade = bootstrapNormalizeCascade
  __impl.parseMappingLines = bootstrapParseMappingLines
  __impl.parseSearchParamLines = bootstrapParseSearchParamLines
  __impl.pruneRecordSelectorConfig = bootstrapPruneRecordSelectorConfig
  __impl.buildRecordSelectorConfig = bootstrapBuildRecordSelectorConfig
  __impl.resetPreviewForm = bootstrapResetPreviewForm
  __impl.syncFormulaDependsOnFromExpression = bootstrapSyncFormulaDependsOnFromExpression

  const activeTab = ref(props.defaultActiveTab || 'basic')
  const defaultVisibleTabs = ['basic', 'display', 'advanced']
  const enabledTabs = computed(() => {
    const tabs = (props.visibleTabs || []).filter(Boolean)
    return tabs.length ? tabs : defaultVisibleTabs
  })
  const formulaOnly = computed(() => props.mode === 'formula')
  const form = reactive(createFieldForm())

  const fieldTypeOptions = [
    { label: '文本', value: 'TEXT' },
    { label: '多行文本', value: 'MULTILINE' },
    { label: '数字', value: 'NUMBER' },
    { label: '金额', value: 'MONEY' },
    { label: '日期', value: 'DATE' },
    { label: '日期时间', value: 'DATETIME' },
    { label: '下拉', value: 'DICT' },
    { label: '树形/外键选择', value: 'SELECT' },
    { label: '单选', value: 'RADIO' },
    { label: '多选', value: 'CHECKBOX' },
    { label: '开关', value: 'SWITCH' },
    { label: '附件', value: 'FILE' },
    { label: '图片', value: 'IMAGE' },
    { label: '人员', value: 'USER' },
    { label: '部门', value: 'DEPT' },
    { label: '地区', value: 'REGION' },
    { label: '引用对象', value: 'REFERENCE' },
    { label: '记录选择器', value: 'RECORD_SELECTOR' },
  ]

  const componentOptions = [
    { label: '输入框', value: 'input' },
    { label: '多行文本', value: 'textarea' },
    { label: '数字输入', value: 'number' },
    { label: '下拉选择', value: 'select' },
    { label: '树形选择', value: 'treeSelect' },
    { label: '单选框', value: 'radio' },
    { label: '多选框', value: 'checkbox' },
    { label: '字典选择', value: 'dictSelect' },
    { label: '日期', value: 'date' },
    { label: '日期时间', value: 'datetime' },
    { label: '开关', value: 'switch' },
    { label: '文件上传', value: 'fileUpload' },
    { label: '图片上传', value: 'imageUpload' },
    { label: '人员选择', value: 'userSelect' },
    { label: '部门树', value: 'orgTreeSelect' },
    { label: '地区树', value: 'regionTreeSelect' },
    { label: '引用对象', value: 'objectReference' },
    { label: '记录选择器', value: 'recordSelector' },
  ]

  const queryTypeOptions = [
    { label: '包含', value: 'like' },
    { label: '等于', value: 'eq' },
    { label: '大于等于', value: 'ge' },
    { label: '小于等于', value: 'le' },
    { label: '区间', value: 'between' },
    { label: '多值', value: 'in' },
  ]

  const switchDefaultOptions = [
    { label: '关闭（0）', value: 0 },
    { label: '开启（1）', value: 1 },
  ]

  const statusOptions = [
    { label: '启用', value: 'ENABLED' },
    { label: '停用', value: 'DISABLED' },
    { label: '隐藏', value: 'HIDDEN' },
  ]

  const sensitiveOptions = [
    { label: '手机号', value: 'PHONE' },
    { label: '身份证', value: 'ID_CARD' },
    { label: '银行卡', value: 'BANK_CARD' },
    { label: '邮箱', value: 'EMAIL' },
  ]
  const commonValidationOptions = COMMON_VALIDATION_PRESETS.map(item => ({
    label: item.label,
    value: item.value,
  }))

  // 公式校验/预览状态
  const formulaValidating = ref(false)
  const formulaPreviewing = ref(false)
  const formulaValidateResult = ref(null)
  const formulaPreviewResult = ref(null)
  const formulaPreviewDialogVisible = ref(false)
  const formulaDebuggerVisible = ref(false)
  const formulaLogVisible = ref(false)
  const formulaGraphVisible = ref(false)
  const formulaConditionRuleValidation = ref(null)
  const formulaPreviewForm = reactive({})
  const automaticRequiredDefault = ref(null)

  const encryptOptions = [
    { label: 'AES', value: 'AES' },
    { label: 'SM4', value: 'SM4' },
  ]
  const cascadeModeOptions = [
    { label: '接口加载 —— 选了上级后按参数请求接口', value: 'remoteParam' },
    { label: '本地过滤 —— 已有选项按父级编码筛选', value: 'parentDictCode' },
    { label: '字典行级联 —— 按关联字典值筛选', value: 'linkedDict' },
  ]

  const payload = computed(() => normalizePayload(form))
  const selectedFormulaField = computed(() => ({
    ...(props.field || {}),
    ...payload.value,
  }))
  const formulaToolFields = computed(() => {
    const current = selectedFormulaField.value
    const currentCode = current.fieldCode || current.field
    if (!currentCode)
      return props.allFields || []
    let matched = false
    const merged = (props.allFields || []).map((item) => {
      const code = item?.fieldCode || item?.field
      if (code !== currentCode)
        return item
      matched = true
      return {
        ...item,
        ...current,
      }
    })
    return matched ? merged : [current, ...merged]
  })
  const canOpenFormulaDebugger = computed(() => Boolean(selectedFormulaField.value.formulaConfig?.type))
  const hasFormulaToolFields = computed(() => formulaToolFields.value.some(item => item?.formulaConfig?.type))
  const changed = computed(() => JSON.stringify(payload.value) !== mut.baseline)
  const needsDict = computed(() => ['DICT', 'RADIO', 'CHECKBOX'].includes(form.fieldType) || ['select', 'radio', 'checkbox', 'dictSelect'].includes(form.componentType))
  // 组织→人员级联：人员组件的级联配置按组织过滤语义处理，与字典级联区分
  const isUserSelectCascadeField = computed(() => form.componentType === 'userSelect')
  const showCascadeConfig = computed(() => needsDict.value || isUserSelectCascadeField.value)
  const isRecordSelectorField = computed(() => form.fieldType === 'RECORD_SELECTOR' || form.componentType === 'recordSelector')
  const isObjectReferenceField = computed(() => form.fieldType === 'REFERENCE' || form.componentType === 'objectReference')
  const businessObjectOptions = ref([])
  const businessObjectLoading = ref(false)

  const referenceTargetFieldsMap = ref({})
  const referenceTargetFieldLoadPromises = {}

  // ─── Unified Relation Field Computeds ────────────────────────────────────────
  const isRelationField = computed(() => isRecordSelectorField.value || isObjectReferenceField.value)

  const relationSelectionMode = computed(() => {
    if (isRecordSelectorField.value)
      return 'popup'
    return 'dropdown'
  })

  const unifiedRelationObjectCode = computed(() => {
    if (isRecordSelectorField.value)
      return form.recordSelectorObjectCode || ''
    if (isObjectReferenceField.value)
      return form.referenceObjectCode || ''
    return ''
  })

  const unifiedDisplayField = computed(() => {
    if (isRecordSelectorField.value)
      return form.recordSelectorLabelField || ''
    if (isObjectReferenceField.value)
      return form.referenceDisplayField || ''
    return ''
  })

  const unifiedValueField = computed(() => {
    if (isRecordSelectorField.value)
      return form.recordSelectorValueField || ''
    if (isObjectReferenceField.value)
      return form.referenceValueField || ''
    return ''
  })

  const unifiedTargetFieldOptions = computed(() => {
    const objectCode = unifiedRelationObjectCode.value
    if (!objectCode)
      return []
    return referenceTargetFieldsMap.value[objectCode]?.options || []
  })
  const normalizedDataType = computed(() => String(form.dataType || '').toLowerCase())
  const supportsTextLength = computed(() => ['varchar', 'char'].includes(normalizedDataType.value))
  const isDecimalType = computed(() => normalizedDataType.value === 'decimal')
  const isNumericDataType = computed(() => ['tinyint', 'int', 'integer', 'bigint', 'decimal'].includes(normalizedDataType.value))
  const formulaEnabled = computed(() => !!form.formulaType)
  const formulaFeedbackLines = computed(() => {
    const result = formulaValidateResult.value
    if (!result || result.valid)
      return []
    if (Array.isArray(result.errors) && result.errors.length) {
      return result.errors
        .map(item => item?.message || item?.errorMessage || String(item || ''))
        .filter(Boolean)
    }
    return [result.errorMessage || '表达式校验失败']
  })
  const previewTargetLabel = computed(() => `${form.fieldName || form.fieldCode || '目标字段'}（${form.fieldCode || '-'}）`)
  const previewVariableCodes = computed(() => collectFormulaVariableCodes())
  const previewVariableFields = computed(() => {
    return previewVariableCodes.value.map((fieldCode) => {
      const meta = resolveDependFieldMeta(fieldCode)
      return {
        field: fieldCode,
        label: buildDependFieldLabel(meta, fieldCode),
        inputType: resolvePreviewInputType(meta, fieldCode),
      }
    })
  })
  const canPreviewCalculate = computed(() => {
    if (!form.formulaExpression || props.field?.systemField)
      return false
    return previewVariableFields.value.every(item => hasPreviewInputValue(formulaPreviewForm[item.field]))
  })
  const canOpenFormulaPreview = computed(() => {
    return Boolean(form.formulaExpression)
      && !props.field?.systemField
      && form.formulaType !== 'LOOKUP'
      && !form.formulaCrossObjectEnabled
  })
  const lengthMax = computed(() => {
    if (normalizedDataType.value === 'char')
      return 255
    return 2048
  })
  const decimalPrecisionMax = computed(() => Math.max(0, Number(form.length || 1) - 1))
  const numericStorageRange = computed(() => resolveNumericStorageRange(form.dataType, form.length, form.precision))
  const numericMinimumPlaceholder = computed(() => numericStorageRange.value?.minimum || '不额外限制')
  const numericMaximumPlaceholder = computed(() => numericStorageRange.value?.maximum || '不额外限制')
  const numericStorageHint = computed(() => {
    const range = numericStorageRange.value
    if (!range)
      return '当前字段类型没有数值范围。'
    const custom = form.minValue !== null || form.maxValue !== null
    const suffix = custom ? '；你设置的最小值、最大值会在该范围内进一步收紧。' : '；留空表示使用数据库范围。'
    return `${range.description}：${range.minimum} ～ ${range.maximum}${suffix}`
  })
  const hasSafeRequiredDefault = computed(() => hasDefaultValue(form.defaultValue))
  const requiredDefaultHint = computed(() => {
    if (!form.required)
      return ''
    if (hasSafeRequiredDefault.value)
      return `必填字段已配置默认值：${formatDefaultValue(form.defaultValue)}`
    if (!supportsAutomaticRequiredDefault(form.fieldType))
      return '当前类型无法推断合法业务值，请手动选择有效默认值，避免写入无效字典值或关联 ID。'
    return '开启必填后会按字段类型自动生成默认值，你仍可以手动修改。'
  })

  const dependFieldOptions = computed(() => {
    if (!props.allFields)
      return []
    return props.allFields
      .filter(item => item && item.fieldCode !== form.fieldCode && item.fieldStatus !== 'HIDDEN')
      .map(item => ({
        label: `${item.fieldName || item.label || item.fieldCode}（${item.fieldCode || item.field}）`,
        value: item.fieldCode || item.field,
      }))
  })

  const cascadeSourceFieldOptions = computed(() => {
    const candidates = props.allFields
      .filter(item => item && item.fieldCode !== form.fieldCode && item.fieldStatus !== 'HIDDEN')
    // 人员组件级联时优先引导选组织组件字段；无组织字段时回退全部字段，避免空选项死锁
    if (isUserSelectCascadeField.value) {
      const orgFields = candidates.filter(item => item.componentType === 'orgTreeSelect')
      if (orgFields.length) {
        return orgFields.map(item => ({
          label: `${item.fieldName || item.label || item.fieldCode}（${item.fieldCode || item.field}）`,
          value: item.fieldCode || item.field,
        }))
      }
    }
    return candidates.map(item => ({
      label: `${item.fieldName || item.label || item.fieldCode}（${item.fieldCode || item.field}）`,
      value: item.fieldCode || item.field,
    }))
  })
  const hasOrgFieldForCascade = computed(() => props.allFields.some(item => item && item.componentType === 'orgTreeSelect' && item.fieldCode !== form.fieldCode && item.fieldStatus !== 'HIDDEN'))

  // 首次 resetForm 延后到 orchestrator 在 part2 注册完成后再调用，避免 __impl 尚未就绪
  watch(
    () => props.field,
    () => {
      if (!mut.partsReady)
        return
      resetForm()
    },
    { deep: true },
  )

  watch(
    () => props.defaultActiveTab,
    (value) => {
      activeTab.value = normalizeActiveTab(value || 'basic')
    },
  )

  watch(
    enabledTabs,
    () => {
      activeTab.value = normalizeActiveTab(activeTab.value)
    },
    { immediate: true },
  )

  watch(
    () => form.fieldType,
    (value, oldValue) => {
      if (mut.resetting || !oldValue || value === oldValue || props.field?.systemField)
        return
      applyFieldTypeDefaults(value)
      if (form.required)
        applyRequiredDefault(value)
    },
  )

  watch(
    () => form.fieldCode,
    (value, oldValue) => {
      if (mut.resetting || props.field?.systemField || !oldValue || value === oldValue)
        return
      const previousColumn = camelToSnake(oldValue)
      if (!form.columnName || form.columnName === previousColumn)
        form.columnName = camelToSnake(value)
    },
  )

  watch(changed, (value) => {
    if (!mut.resetting)
      emit('dirtyChange', value)
  })

  watch(() => form.formulaExpression, () => {
    syncFormulaDependsOnFromExpression()
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
    if (formulaPreviewDialogVisible.value) {
      initializePreviewForm()
      schedulePreviewCalculation()
    }
  })

  watch(() => form.formulaConditionExpression, () => {
    syncFormulaDependsOnFromExpression()
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
  })

  watch(() => form.formulaDependsOn, () => {
    formulaPreviewResult.value = null
    if (formulaPreviewDialogVisible.value)
      initializePreviewForm()
  }, { deep: true })

  watch(() => props.allFields, () => {
    syncFormulaDependsOnFromExpression()
  }, { deep: true })

  watch(formulaPreviewForm, () => {
    if (!formulaPreviewDialogVisible.value || mut.formulaPreviewInitializing)
      return
    schedulePreviewCalculation()
  }, { deep: true })

  watch(formulaPreviewDialogVisible, (visible) => {
    if (!visible)
      clearPreviewTimer()
  })

  onBeforeUnmount(() => {
    clearPreviewTimer()
  })

  function resetForm() {
    mut.resetting = true
    automaticRequiredDefault.value = null
    Object.assign(form, createFieldForm(props.field))
    mut.baseline = JSON.stringify(normalizePayload(form))
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
    formulaConditionRuleValidation.value = null
    formulaPreviewDialogVisible.value = false
    resetPreviewForm()
    emit('dirtyChange', false)
    nextTick(() => {
      mut.resetting = false
      syncFormulaDependsOnFromExpression()
    })
  }

  function createFieldForm(field) {
    const currentField = field || {}
    const fieldType = currentField.fieldType || 'TEXT'
    const basicProps = { ...(currentField.basicProps || {}) }
    const validation = currentField.validation || basicProps.validation || currentField.advancedProps?.validation || {}
    const recordSelector = createDefaultRecordSelector(currentField.recordSelector || basicProps.recordSelector || currentField.props?.recordSelector)
    return {
      fieldName: currentField.fieldName || '',
      fieldCode: currentField.fieldCode || '',
      columnName: currentField.columnName || '',
      fieldType,
      dataType: currentField.dataType || 'varchar',
      length: currentField.length ?? 255,
      precision: currentField.precision ?? 0,
      minValue: normalizeNullableNumber(basicProps.min ?? basicProps.minimum),
      maxValue: normalizeNullableNumber(basicProps.max ?? basicProps.maximum),
      required: !!currentField.required,
      defaultValue: normalizeDefaultValueForEditor(currentField.defaultValue, fieldType),
      searchable: !!currentField.searchable,
      listVisible: currentField.listVisible !== false,
      formVisible: currentField.formVisible !== false,
      importable: currentField.importable !== false,
      exportable: currentField.exportable !== false,
      componentType: currentField.componentType || '',
      queryType: currentField.queryType || '',
      dictType: currentField.dictType || '',
      sensitiveType: currentField.sensitiveType || '',
      encryptAlgorithm: currentField.encryptAlgorithm || '',
      sortable: !!currentField.sortable,
      systemField: !!currentField.systemField,
      readonly: !!currentField.readonly,
      fieldStatus: currentField.fieldStatus || 'ENABLED',
      referenceObjectCode: currentField.referenceObjectCode || '',
      referenceDisplayField: currentField.referenceDisplayField || '',
      referenceValueField: currentField.referenceValueField || '',
      placeholder: currentField.basicProps?.placeholder || currentField.placeholder || '',
      remark: currentField.remark || '',
      sortOrder: currentField.sortOrder ?? 0,
      basicProps: {
        ...basicProps,
        cascade: createDefaultCascade(basicProps.cascade || currentField.cascade || currentField.props?.cascade),
      },
      advancedProps: { ...(currentField.advancedProps || {}) },
      formulaName: currentField.formulaConfig
        ? (currentField.formulaConfig?.name || currentField.formulaConfig?.formulaName || currentField.fieldName || '')
        : '',
      formulaType: currentField.formulaConfig?.type || '',
      formulaMode: currentField.formulaConfig?.mode || 'STORED',
      formulaTriggerMode: currentField.formulaConfig?.triggerMode || modeToTriggerMode(currentField.formulaConfig?.mode),
      formulaExpression: currentField.formulaConfig?.expression || '',
      formulaDependsOn: currentField.formulaConfig?.dependsOn || [],
      formulaFunctionRefs: currentField.formulaConfig?.functionRefs || [],
      formulaAggregateFunction: currentField.formulaConfig?.aggregate?.function || '',
      formulaAggregateRelationCode: currentField.formulaConfig?.aggregate?.relationCode
        ? String(currentField.formulaConfig.aggregate.relationCode)
        : '',
      formulaAggregateTargetField: currentField.formulaConfig?.aggregate?.targetField || '',
      formulaAggregateFilter: currentField.formulaConfig?.aggregate?.filter || '',
      formulaConditionMode: currentField.formulaConfig?.rule ? 'RULE' : 'EXPRESSION',
      formulaConditionRule: cloneConditionRule(currentField.formulaConfig?.rule),
      formulaConditionExpression: currentField.formulaConfig?.condition?.expression || currentField.formulaConfig?.expression || '',
      formulaConditionTrueValue: currentField.formulaConfig?.condition?.trueValue ?? '',
      formulaConditionFalseValue: currentField.formulaConfig?.condition?.falseValue ?? '',
      formulaLookupRelationCode: currentField.formulaConfig?.lookup?.relationCode || '',
      formulaLookupTargetObjectCode: currentField.formulaConfig?.lookup?.targetObjectCode || '',
      formulaLookupSourceField: currentField.formulaConfig?.lookup?.sourceField || '',
      formulaLookupTargetField: currentField.formulaConfig?.lookup?.targetField || '',
      formulaLookupReturnField: currentField.formulaConfig?.lookup?.returnField || '',
      formulaLookupNotFoundValue: currentField.formulaConfig?.lookup?.notFoundValue ?? '',
      formulaCrossObjectEnabled: Boolean(currentField.formulaConfig?.crossObject),
      formulaCrossObjectPath: currentField.formulaConfig?.crossObject?.path || '',
      formulaCrossObjectRelationCode: currentField.formulaConfig?.crossObject?.relationCode || '',
      formulaCrossObjectTargetObjectCode: currentField.formulaConfig?.crossObject?.targetObjectCode || '',
      formulaCrossObjectReturnField: currentField.formulaConfig?.crossObject?.returnField || '',
      formulaCrossObjectRecomputeMode: currentField.formulaConfig?.crossObject?.recomputeMode || 'ASYNC',
      validationPreset: validation.preset || validation.presetCode || validation.commonRule || '',
      validationPattern: validation.pattern || '',
      validationMessage: validation.message || validation.patternMessage || '',
      recordSelectorSuiteCode: recordSelector.suiteCode,
      recordSelectorObjectCode: recordSelector.objectCode,
      recordSelectorValueField: recordSelector.valueField,
      recordSelectorLabelField: recordSelector.labelField,
      recordSelectorTargetLabelField: recordSelector.targetLabelField,
      recordSelectorDisplayFields: normalizeTextList(recordSelector.displayFields),
      recordSelectorKeywordFields: normalizeTextList(recordSelector.keywordFields),
      recordSelectorMappingsText: mappingsToLines(recordSelector.fieldMappings),
      recordSelectorSearchParamsText: searchParamsToLines(recordSelector.searchParams),
    }
  }

  function isTabVisible(tab) {
    return enabledTabs.value.includes(tab)
  }

  function normalizeActiveTab(tab = '') {
    const candidate = tab || props.defaultActiveTab || 'basic'
    if (isTabVisible(candidate))
      return candidate
    return enabledTabs.value[0] || 'basic'
  }

  function cloneConditionRule(rule) {
    if (!rule || typeof rule !== 'object')
      return createDefaultConditionRule()
    try {
      return JSON.parse(JSON.stringify(rule))
    }
    catch {
      return createDefaultConditionRule()
    }
  }

  function createDefaultConditionRule() {
    const firstField = (props.allFields || [])
      .find(item => item && item.fieldStatus !== 'HIDDEN' && (item.fieldCode || item.field))
    return {
      operator: 'AND',
      children: [
        {
          field: firstField?.fieldCode || firstField?.field || '',
          op: 'EQ',
          value: '',
        },
      ],
    }
  }

  function normalizePayload(source) {
    // 人员组件级联固定按组织过滤，兼容存量/手改配置，避免落到字典联动语义
    const cascade = normalizeCascade({
      ...(source.basicProps?.cascade || {}),
      ...(source.componentType === 'userSelect' && source.basicProps?.cascade?.enabled
        ? { mode: 'orgFilter' }
        : {}),
    })
    const recordSelector = buildRecordSelectorConfig(source)
    const validation = buildValidationConfig(source)
    const basicProps = {
      ...(source.basicProps || {}),
      placeholder: source.placeholder || '',
    }
    if (cascade.enabled)
      basicProps.cascade = cascade
    else
      delete basicProps.cascade
    if (recordSelector)
      basicProps.recordSelector = recordSelector
    else
      delete basicProps.recordSelector
    if (validation)
      basicProps.validation = validation
    else
      delete basicProps.validation
    delete basicProps.minimum
    delete basicProps.maximum
    if (isNumericStorageType(source.dataType) && source.minValue !== null && source.minValue !== undefined)
      basicProps.min = source.minValue
    else
      delete basicProps.min
    if (isNumericStorageType(source.dataType) && source.maxValue !== null && source.maxValue !== undefined)
      basicProps.max = source.maxValue
    else
      delete basicProps.max
    return {
      fieldName: source.fieldName,
      fieldCode: source.fieldCode,
      columnName: source.columnName,
      fieldType: source.fieldType,
      dataType: source.dataType,
      length: source.length,
      precision: source.precision,
      required: source.required,
      defaultValue: source.defaultValue,
      searchable: source.searchable,
      listVisible: source.listVisible,
      formVisible: source.formVisible,
      importable: source.importable,
      exportable: source.exportable,
      componentType: source.componentType,
      queryType: source.queryType,
      dictType: source.dictType,
      sensitiveType: source.sensitiveType,
      encryptAlgorithm: source.encryptAlgorithm,
      sortable: source.sortable,
      systemField: source.systemField,
      readonly: source.readonly,
      fieldStatus: source.fieldStatus,
      referenceObjectCode: source.referenceObjectCode,
      referenceDisplayField: source.referenceDisplayField,
      referenceValueField: source.referenceValueField,
      placeholder: source.placeholder,
      remark: source.remark,
      sortOrder: source.sortOrder,
      basicProps,
      advancedProps: { ...(source.advancedProps || {}) },
      validation,
      formulaConfig: buildFormulaConfigPayload(source),
    }
  }

  function buildValidationConfig(source) {
    const validation = {}
    if (source.validationPreset)
      validation.preset = source.validationPreset
    if (source.validationPattern)
      validation.pattern = source.validationPattern
    if (source.validationMessage)
      validation.message = source.validationMessage
    return Object.keys(validation).length ? validation : null
  }

  function onValidationPresetChange(value) {
    const preset = getValidationPreset(value)
    if (!value) {
      form.validationPattern = ''
      form.validationMessage = ''
      return
    }
    if (!preset)
      return
    form.validationPattern = preset.pattern || ''
    form.validationMessage = preset.message || ''
    if (value === 'PHONE' && !form.sensitiveType)
      form.sensitiveType = 'PHONE'
    if (value === 'EMAIL' && !form.sensitiveType)
      form.sensitiveType = 'EMAIL'
    if (value === 'ID_CARD' && !form.sensitiveType)
      form.sensitiveType = 'ID_CARD'
    if (value === 'BANK_CARD' && !form.sensitiveType)
      form.sensitiveType = 'BANK_CARD'
  }

  function updateRequired(value) {
    form.required = !!value
    if (form.required) {
      applyRequiredDefault(form.fieldType)
      return
    }
    if (isCurrentAutomaticDefault())
      form.defaultValue = emptyDefaultValue(form.fieldType)
    automaticRequiredDefault.value = null
  }

  function applyRequiredDefault(fieldType) {
    const currentIsAutomatic = isCurrentAutomaticDefault()
    if (hasDefaultValue(form.defaultValue) && !currentIsAutomatic)
      return false
    const suggestion = resolveAutomaticRequiredDefault(fieldType)
    if (!suggestion.supported) {
      if (currentIsAutomatic)
        form.defaultValue = emptyDefaultValue(fieldType)
      automaticRequiredDefault.value = null
      return false
    }
    form.defaultValue = suggestion.value
    automaticRequiredDefault.value = suggestion.value
    return true
  }

  function supportsAutomaticRequiredDefault(fieldType) {
    return resolveAutomaticRequiredDefault(fieldType).supported
  }

  function resolveAutomaticRequiredDefault(fieldType) {
    const type = String(fieldType || '').toUpperCase()
    if (['NUMBER', 'MONEY', 'SWITCH'].includes(type))
      return { supported: true, value: 0 }
    if (type === 'DATE')
      return { supported: true, value: formatLocalDate(new Date()) }
    if (type === 'DATETIME')
      return { supported: true, value: formatLocalDateTime(new Date()) }
    if (['CHECKBOX', 'FILE', 'IMAGE'].includes(type))
      return { supported: true, value: '[]' }
    if (['TEXT', 'MULTILINE'].includes(type))
      return { supported: true, value: '-' }
    return { supported: false, value: '' }
  }

  function normalizeDefaultValueForEditor(value, fieldType) {
    const type = String(fieldType || '').toUpperCase()
    if (value === null || value === undefined || value === '')
      return ['NUMBER', 'MONEY'].includes(type) ? null : ''
    if (['NUMBER', 'MONEY', 'SWITCH'].includes(type)) {
      const numeric = Number(value)
      return Number.isFinite(numeric) ? numeric : value
    }
    return value
  }

  function normalizeNullableNumber(value) {
    if (value === null || value === undefined || value === '')
      return null
    const numeric = Number(value)
    return Number.isFinite(numeric) ? numeric : null
  }

  function isNumericStorageType(dataType) {
    return ['tinyint', 'int', 'integer', 'bigint', 'decimal'].includes(String(dataType || '').toLowerCase())
  }

  function resolveNumericStorageRange(dataType, length, precision) {
    const type = String(dataType || '').toLowerCase()
    if (type === 'tinyint')
      return { description: 'tinyint 固定整数范围', minimum: '-128', maximum: '127' }
    if (type === 'int' || type === 'integer')
      return { description: 'int 固定整数范围', minimum: '-2147483648', maximum: '2147483647' }
    if (type === 'bigint')
      return { description: 'bigint 固定整数范围', minimum: '-9223372036854775808', maximum: '9223372036854775807' }
    if (type !== 'decimal')
      return null
    const totalDigits = Math.min(65, Math.max(1, Number(length || 18)))
    const scale = Math.min(totalDigits - 1, Math.max(0, Number(precision ?? 2)))
    const integerDigits = totalDigits - scale
    const integerPart = '9'.repeat(integerDigits)
    const fractionPart = scale ? `.${'9'.repeat(scale)}` : ''
    const maximum = `${integerPart}${fractionPart}`
    return {
      description: `decimal(${totalDigits}, ${scale})，共 ${totalDigits} 位，其中 ${scale} 位小数`,
      minimum: `-${maximum}`,
      maximum,
    }
  }

  function onDecimalTotalDigitsChange(value) {
    form.length = value
    if (form.precision >= value)
      form.precision = Math.max(0, Number(value || 1) - 1)
  }

  function emptyDefaultValue(fieldType) {
    return ['NUMBER', 'MONEY'].includes(String(fieldType || '').toUpperCase()) ? null : ''
  }

  function isCurrentAutomaticDefault() {
    if (automaticRequiredDefault.value === null)
      return false
    return JSON.stringify(form.defaultValue) === JSON.stringify(automaticRequiredDefault.value)
  }

  function hasDefaultValue(value) {
    if (Array.isArray(value))
      return value.length > 0
    return value !== null && value !== undefined && String(value).trim() !== ''
  }

  function formatDefaultValue(value) {
    if (Array.isArray(value))
      return JSON.stringify(value)
    return String(value)
  }

  function formatLocalDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  function formatLocalDateTime(date) {
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${formatLocalDate(date)} ${hours}:${minutes}:${seconds}`
  }

  function buildFormulaConfigPayload(source) {
    if (!source.formulaType)
      return null
    const dependsOn = normalizeFormulaDependsOn(source)
    const expression = source.formulaType === 'LOOKUP'
      ? ''
      : (source.formulaCrossObjectEnabled ? source.formulaCrossObjectPath : source.formulaExpression) || ''
    const config = {
      name: source.formulaName || '',
      type: source.formulaType,
      mode: source.formulaMode || 'STORED',
      triggerMode: source.formulaTriggerMode || modeToTriggerMode(source.formulaMode),
      expression,
      dependsOn,
      functionRefs: normalizeFormulaFunctionRefs(source),
    }
    if (source.formulaType === 'LOOKUP') {
      config.lookup = {
        relationCode: source.formulaLookupRelationCode || '',
        targetObjectCode: source.formulaLookupTargetObjectCode || '',
        sourceField: source.formulaLookupSourceField || '',
        targetField: source.formulaLookupTargetField || '',
        returnField: source.formulaLookupReturnField || '',
        notFoundValue: source.formulaLookupNotFoundValue ?? null,
      }
    }
    if (source.formulaType === 'AGGREGATE' && source.formulaAggregateFunction) {
      config.aggregate = {
        function: source.formulaAggregateFunction,
        relationCode: source.formulaAggregateRelationCode || '',
        targetField: source.formulaAggregateTargetField || '',
        filter: source.formulaAggregateFilter || '',
      }
    }
    if (source.formulaType === 'CONDITIONAL' && source.formulaConditionExpression) {
      config.condition = {
        expression: source.formulaConditionExpression,
        trueValue: source.formulaConditionTrueValue ?? '',
        falseValue: source.formulaConditionFalseValue ?? '',
      }
      if (source.formulaConditionMode === 'RULE') {
        config.rule = cloneConditionRule(source.formulaConditionRule)
        config.ruleMode = 'RULE'
      }
    }
    if (source.formulaType !== 'LOOKUP' && source.formulaCrossObjectEnabled) {
      config.crossObject = {
        path: source.formulaCrossObjectPath || '',
        relationCode: source.formulaCrossObjectRelationCode || '',
        targetObjectCode: source.formulaCrossObjectTargetObjectCode || '',
        returnField: source.formulaCrossObjectReturnField || '',
        recomputeMode: source.formulaCrossObjectRecomputeMode || 'ASYNC',
      }
    }

    return config
  }

  function onConditionExpressionChange(value) {
    // 条件表达式也作为校验/预览时的执行表达式。
    form.formulaExpression = value
    formulaConditionRuleValidation.value = null
  }

  function onConditionModeChange(value) {
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
    if (value === 'RULE') {
      form.formulaConditionRule = cloneConditionRule(form.formulaConditionRule)
      return
    }
    formulaConditionRuleValidation.value = null
  }

  function onConditionRuleCompiled(expression) {
    form.formulaConditionExpression = expression
    form.formulaExpression = expression
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
  }

  function onConditionRuleValidation(result) {
    formulaConditionRuleValidation.value = result
    if (result && !result.valid)
      formulaValidateResult.value = result
  }

  function onFormulaTypeChange(value) {
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
    if (!value) {
      clearFormulaForm()
      return
    }
    if (!form.formulaMode)
      setFormulaTriggerMode('ON_SAVE')
    if (value === 'LOOKUP') {
      form.formulaExpression = ''
      form.formulaDependsOn = []
      form.formulaCrossObjectEnabled = false
      setFormulaTriggerMode(form.formulaTriggerMode || 'REALTIME')
    }
    if (value === 'CONDITIONAL') {
      form.formulaConditionMode = form.formulaConditionMode || 'EXPRESSION'
      form.formulaConditionRule = cloneConditionRule(form.formulaConditionRule)
    }
    if (value === 'AGGREGATE')
      clearCrossObjectForm()
  }

  function updateFormulaEnabled(value) {
    if (value) {
      form.formulaType = form.formulaType || 'CALC'
      form.formulaName = form.formulaName || form.fieldName || '金额计算'
      setFormulaTriggerMode(form.formulaTriggerMode || 'REALTIME')
      return
    }
    clearFormulaForm()
    formulaValidateResult.value = null
    formulaPreviewResult.value = null
  }

  function clearFormulaForm() {
    form.formulaName = ''
    form.formulaType = ''
    form.formulaMode = 'STORED'
    form.formulaTriggerMode = 'ON_SAVE'
    form.formulaExpression = ''
    form.formulaDependsOn = []
    form.formulaFunctionRefs = []
    form.formulaAggregateFunction = ''
    form.formulaAggregateRelationCode = ''
    form.formulaAggregateTargetField = ''
    form.formulaAggregateFilter = ''
    form.formulaConditionMode = 'EXPRESSION'
    form.formulaConditionRule = createDefaultConditionRule()
    form.formulaConditionExpression = ''
    form.formulaConditionTrueValue = ''
    form.formulaConditionFalseValue = ''
    form.formulaLookupRelationCode = ''
    form.formulaLookupTargetObjectCode = ''
    form.formulaLookupSourceField = ''
    form.formulaLookupTargetField = ''
    form.formulaLookupReturnField = ''
    form.formulaLookupNotFoundValue = ''
    clearCrossObjectForm()
  }

  function clearCrossObjectForm() {
    form.formulaCrossObjectEnabled = false
    form.formulaCrossObjectPath = ''
    form.formulaCrossObjectRelationCode = ''
    form.formulaCrossObjectTargetObjectCode = ''
    form.formulaCrossObjectReturnField = ''
    form.formulaCrossObjectRecomputeMode = 'ASYNC'
  }

  function setFormulaTriggerMode(value) {
    form.formulaTriggerMode = value
    form.formulaMode = value === 'REALTIME' ? 'VIRTUAL' : 'STORED'
  }

  function modeToTriggerMode(mode) {
    return mode === 'VIRTUAL' ? 'REALTIME' : 'ON_SAVE'
  }

  function insertFormulaToken(value) {
    if (!value || props.field?.systemField) {
      return
    }
    const token = normalizeExpressionToken(value)
    const current = form.formulaExpression || ''
    const joiner = current && !/\s$/.test(current) ? ' ' : ''
    form.formulaExpression = `${current}${joiner}${token}`
  }

  function insertStringToken() {
    insertFormulaToken('\'\'')
  }

  function normalizeExpressionToken(value) {
    if (value === '字段') {
      return dependFieldOptions.value[0]?.value || ''
    }
    return String(value)
  }

  function validateFormulaConfigLocal() {
    const errors = []
    if (!form.formulaType)
      errors.push('请选择公式类型')

    if (form.formulaType === 'LOOKUP') {
      if (!form.formulaLookupRelationCode)
        errors.push('请选择 LOOKUP 对象关系')
      if (!form.formulaLookupTargetObjectCode)
        errors.push('LOOKUP 目标对象不能为空')
      if (!form.formulaLookupSourceField)
        errors.push('请选择当前对象字段')
      if (!form.formulaLookupTargetField)
        errors.push('请选择目标匹配字段')
      if (!form.formulaLookupReturnField)
        errors.push('请选择返回字段')
    }
    else if (form.formulaCrossObjectEnabled) {
      if (!form.formulaCrossObjectRelationCode)
        errors.push('请选择跨对象关系')
      if (!form.formulaCrossObjectTargetObjectCode)
        errors.push('跨对象目标对象不能为空')
      if (!form.formulaCrossObjectReturnField)
        errors.push('请选择跨对象返回字段')
      if (!isOneHopPath(form.formulaCrossObjectPath))
        errors.push('跨对象路径必须是一跳 relation.field')
    }
    else if (form.formulaType === 'AGGREGATE') {
      if (!form.formulaAggregateFunction)
        errors.push('请选择聚合函数')
      if (!form.formulaAggregateRelationCode)
        errors.push('请选择聚合关联对象')
      if (form.formulaAggregateFunction !== 'COUNT' && !form.formulaAggregateTargetField)
        errors.push('请选择聚合目标字段')
    }
    else if (!form.formulaExpression) {
      errors.push('表达式不能为空')
    }

    if (form.formulaType === 'CONDITIONAL' && !form.formulaConditionExpression)
      errors.push('条件表达式不能为空')
    if (form.formulaType === 'CONDITIONAL' && form.formulaConditionMode === 'RULE') {
      if (!form.formulaConditionRule)
        errors.push('条件规则不能为空')
      if (formulaConditionRuleValidation.value && !formulaConditionRuleValidation.value.valid) {
        formulaConditionRuleValidation.value.errors?.forEach((item) => {
          errors.push(item?.message || item?.errorMessage || String(item || '条件规则校验失败'))
        })
      }
    }

    return {
      valid: errors.length === 0,
      errors: errors.map(message => ({ message })),
      variables: normalizeFormulaDependsOn(form),
    }
  }

  function isOneHopPath(path) {
    const value = String(path || '').trim()
    const firstDot = value.indexOf('.')
    return firstDot > 0 && firstDot === value.lastIndexOf('.') && firstDot < value.length - 1
  }

  async function handleValidateFormula() {
    const localResult = validateFormulaConfigLocal()
    if (!localResult.valid) {
      formulaValidateResult.value = localResult
      return
    }
    if (form.formulaType === 'LOOKUP' || form.formulaCrossObjectEnabled) {
      formulaValidateResult.value = localResult
      return
    }
    formulaValidating.value = true
    formulaValidateResult.value = null
    try {
      const res = await validateFormula({
        expression: form.formulaExpression,
        type: form.formulaType || 'CALC',
        dependsOn: normalizeFormulaDependsOn(form),
      })
      formulaValidateResult.value = res?.data ?? res
    }
    catch (e) {
      formulaValidateResult.value = { valid: false, errorMessage: e?.message || '验证请求失败' }
    }
    finally {
      formulaValidating.value = false
    }
  }

  async function openFormulaPreview() {
    if (!canOpenFormulaPreview.value)
      return
    formulaPreviewDialogVisible.value = true
    initializePreviewForm()
    await nextTick()
    schedulePreviewCalculation(0)
  }

  function buildPreviewSampleValues() {
    const values = {}
    previewVariableCodes.value.forEach((fieldCode, index) => {
      values[fieldCode] = guessFormulaSampleValue(resolveDependFieldMeta(fieldCode), index, fieldCode)
    })
    return values
  }

  function collectFormulaVariableCodes() {
    const result = []
    if (form.formulaType === 'LOOKUP') {
      appendFormulaVariable(result, form.formulaLookupSourceField)
      return result
    }
    if (form.formulaCrossObjectEnabled)
      return result
    appendFormulaDependsOn(result, form.formulaDependsOn)
    appendExpressionVariables(result, form.formulaExpression)
    if (form.formulaType === 'CONDITIONAL')
      appendExpressionVariables(result, form.formulaConditionExpression)
    return result
  }

  function normalizeFormulaDependsOn(source) {
    const result = []
    if (source.formulaType === 'LOOKUP') {
      appendFormulaVariable(result, source.formulaLookupSourceField)
      return result.filter(fieldCode => fieldCode !== source.fieldCode)
    }
    if (source.formulaCrossObjectEnabled)
      return []
    appendFormulaDependsOn(result, source.formulaDependsOn)
    appendExpressionVariables(result, source.formulaExpression)
    if (source.formulaType === 'CONDITIONAL')
      appendExpressionVariables(result, source.formulaConditionExpression)
    return result.filter(fieldCode => fieldCode !== source.fieldCode)
  }

  function normalizeFormulaFunctionRefs(source) {
    const result = []
    if (source.formulaType === 'LOOKUP')
      return result
    appendFormulaFunctionRefs(result, source.formulaExpression)
    if (source.formulaType === 'CONDITIONAL')
      appendFormulaFunctionRefs(result, source.formulaConditionExpression)
    return result
  }

  __impl.resetForm = resetForm
  __impl.createFieldForm = createFieldForm
  __impl.isTabVisible = isTabVisible
  __impl.normalizeActiveTab = normalizeActiveTab
  __impl.cloneConditionRule = cloneConditionRule
  __impl.createDefaultConditionRule = createDefaultConditionRule
  __impl.normalizePayload = normalizePayload
  __impl.buildValidationConfig = buildValidationConfig
  __impl.onValidationPresetChange = onValidationPresetChange
  __impl.updateRequired = updateRequired
  __impl.applyRequiredDefault = applyRequiredDefault
  __impl.supportsAutomaticRequiredDefault = supportsAutomaticRequiredDefault
  __impl.resolveAutomaticRequiredDefault = resolveAutomaticRequiredDefault
  __impl.normalizeDefaultValueForEditor = normalizeDefaultValueForEditor
  __impl.normalizeNullableNumber = normalizeNullableNumber
  __impl.isNumericStorageType = isNumericStorageType
  __impl.resolveNumericStorageRange = resolveNumericStorageRange
  __impl.onDecimalTotalDigitsChange = onDecimalTotalDigitsChange
  __impl.emptyDefaultValue = emptyDefaultValue
  __impl.isCurrentAutomaticDefault = isCurrentAutomaticDefault
  __impl.hasDefaultValue = hasDefaultValue
  __impl.formatDefaultValue = formatDefaultValue
  __impl.formatLocalDate = formatLocalDate
  __impl.formatLocalDateTime = formatLocalDateTime
  __impl.buildFormulaConfigPayload = buildFormulaConfigPayload
  __impl.onConditionExpressionChange = onConditionExpressionChange
  __impl.onConditionModeChange = onConditionModeChange
  __impl.onConditionRuleCompiled = onConditionRuleCompiled
  __impl.onConditionRuleValidation = onConditionRuleValidation
  __impl.onFormulaTypeChange = onFormulaTypeChange
  __impl.updateFormulaEnabled = updateFormulaEnabled
  __impl.clearFormulaForm = clearFormulaForm
  __impl.clearCrossObjectForm = clearCrossObjectForm
  __impl.setFormulaTriggerMode = setFormulaTriggerMode
  __impl.modeToTriggerMode = modeToTriggerMode
  __impl.insertFormulaToken = insertFormulaToken
  __impl.insertStringToken = insertStringToken
  __impl.normalizeExpressionToken = normalizeExpressionToken
  __impl.validateFormulaConfigLocal = validateFormulaConfigLocal
  __impl.isOneHopPath = isOneHopPath
  __impl.handleValidateFormula = handleValidateFormula
  __impl.openFormulaPreview = openFormulaPreview
  __impl.buildPreviewSampleValues = buildPreviewSampleValues
  __impl.collectFormulaVariableCodes = collectFormulaVariableCodes
  __impl.normalizeFormulaDependsOn = normalizeFormulaDependsOn
  __impl.normalizeFormulaFunctionRefs = normalizeFormulaFunctionRefs

  return {
    props, emit, __impl, mut, appendExpressionVariables, appendFormulaDependsOn, appendFormulaFunctionRefs, appendFormulaVariable,
    applyFieldTypeDefaults, applyRequiredDefault, autoInferRelationFields, buildDependFieldLabel, buildFormulaConfigPayload, buildFormulaPreviewPayload, buildPreviewSampleValues, buildRecordSelectorConfig,
    buildValidationConfig, clearCrossObjectForm, clearFormulaForm, clearPreviewTimer, cloneConditionRule, collectExpressionFieldCodes, collectFormulaVariableCodes, createDefaultCascade,
    createDefaultConditionRule, createDefaultRecordSelector, createFieldForm, emptyDefaultValue, extractFormulaVariables, fillPreviewSampleValues, formatDefaultValue, formatLocalDate,
    formatLocalDateTime, formatPreviewValue, guessFormulaSampleValue, handlePreviewFormula, handleUnifiedDisplayFieldChange, handleUnifiedObjectCodeChange, handleUnifiedValueFieldChange, handleValidateFormula,
    hasDefaultValue, hasPreviewInputValue, includesAny, initializePreviewForm, insertFormulaToken, insertStringToken, isCurrentAutomaticDefault, isFormulaReservedToken,
    isManagedFormulaFunctionName, isNumericStorageType, isOneHopPath, isSameStringArray, isTabVisible, loadBusinessObjectOptions, loadReferenceTargetFields, mappingsToLines,
    modeToTriggerMode, normalizeActiveTab, normalizeCascade, normalizeDefaultValueForEditor, normalizeExpressionToken, normalizeFormulaDependsOn, normalizeFormulaFunctionRefs, normalizeMappingList,
    normalizeNullableNumber, normalizePayload, normalizeSearchParams, normalizeTextList, onConditionExpressionChange, onConditionModeChange, onConditionRuleCompiled, onConditionRuleValidation,
    onDecimalTotalDigitsChange, onFormulaTypeChange, onValidationPresetChange, openFormulaPreview, parseMappingLines, parseSearchParamLines, parseTextList, pruneRecordSelectorConfig,
    resetForm, resetPreviewForm, resolveAutomaticRequiredDefault, resolveDependFieldMeta, resolveNumericStorageRange, resolvePreviewInputType, schedulePreviewCalculation, searchParamsToLines,
    setFormulaTriggerMode, stripFormulaStringLiterals, supportsAutomaticRequiredDefault, switchRelationSelectionMode, syncFormulaDependsOnFromExpression, updateCascadeEnabled, updateCascadeIncludeChildren, updateFormulaEnabled,
    updateRequired, validateFormulaConfigLocal, activeTab, defaultVisibleTabs, enabledTabs, formulaOnly, form, fieldTypeOptions,
    componentOptions, queryTypeOptions, switchDefaultOptions, statusOptions, sensitiveOptions, commonValidationOptions, formulaValidating, formulaPreviewing,
    formulaValidateResult, formulaPreviewResult, formulaPreviewDialogVisible, formulaDebuggerVisible, formulaLogVisible, formulaGraphVisible, formulaConditionRuleValidation, formulaPreviewForm,
    automaticRequiredDefault, encryptOptions, cascadeModeOptions, payload, selectedFormulaField, formulaToolFields, canOpenFormulaDebugger, hasFormulaToolFields,
    changed, needsDict, isUserSelectCascadeField, showCascadeConfig, isRecordSelectorField, isObjectReferenceField, businessObjectOptions, businessObjectLoading,
    referenceTargetFieldsMap, referenceTargetFieldLoadPromises, isRelationField, relationSelectionMode, unifiedRelationObjectCode, unifiedDisplayField, unifiedValueField, unifiedTargetFieldOptions,
    normalizedDataType, supportsTextLength, isDecimalType, isNumericDataType, formulaEnabled, formulaFeedbackLines, previewTargetLabel, previewVariableCodes,
    previewVariableFields, canPreviewCalculate, canOpenFormulaPreview, lengthMax, decimalPrecisionMax, numericStorageRange, numericMinimumPlaceholder, numericMaximumPlaceholder,
    numericStorageHint, hasSafeRequiredDefault, requiredDefaultHint, dependFieldOptions, cascadeSourceFieldOptions, hasOrgFieldForCascade,
  }
}
