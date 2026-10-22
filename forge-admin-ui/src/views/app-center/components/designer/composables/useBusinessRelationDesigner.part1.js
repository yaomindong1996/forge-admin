/** BusinessRelationDesigner.vue setup part 1. */
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  businessObjectDesigner,
  businessObjectList,
  businessObjectRelations,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import LowcodeErDiagram from '@/components/lowcode-builder/model/LowcodeErDiagram.vue'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyLinkageSchemaToFields,
  createLinkageSchemaFromFields,
  normalizeLinkageSchema,
  validateLinkageSchema,
} from '../form-first/linkageSchema'
import {
  afterBusinessObjectsLoaded,
  afterRelationsLoaded,
  onObjectSwitched,
} from './er-canvas-candidates'
export function applyBusinessRelationDesignerPart1(props, emit) {
  const __impl = {}
  const mut = {
    resettingLinkage: false,
  }
  const applySelectorDefaults = (...args) => __impl.applySelectorDefaults(...args)
  const approvalConfigSummary = (...args) => __impl.approvalConfigSummary(...args)
  const approvalConfigTagType = (...args) => __impl.approvalConfigTagType(...args)
  const buildErDiagramModels = (...args) => __impl.buildErDiagramModels(...args)
  const businessFieldLabel = (...args) => __impl.businessFieldLabel(...args)
  const businessFieldOptionLabel = (...args) => __impl.businessFieldOptionLabel(...args)
  const canInlineEdit = (...args) => __impl.canInlineEdit(...args)
  const createClientKey = (...args) => __impl.createClientKey(...args)
  const createLinkageRuleId = (...args) => __impl.createLinkageRuleId(...args)
  const displayFieldLabel = (...args) => __impl.displayFieldLabel(...args)
  const fieldLabel = (...args) => __impl.fieldLabel(...args)
  const fieldOptionsForObject = (...args) => __impl.fieldOptionsForObject(...args)
  const fillRuleFieldDefaults = (...args) => __impl.fillRuleFieldDefaults(...args)
  const findForeachQuantityStep = (...args) => __impl.findForeachQuantityStep(...args)
  const findReferenceSourceField = (...args) => __impl.findReferenceSourceField(...args)
  const findSemanticSourceField = (...args) => __impl.findSemanticSourceField(...args)
  const firstDisplayField = (...args) => __impl.firstDisplayField(...args)
  const firstSourceField = (...args) => __impl.firstSourceField(...args)
  const firstTargetField = (...args) => __impl.firstTargetField(...args)
  const inferLinkageType = (...args) => __impl.inferLinkageType(...args)
  const inferSelectorDisplayFields = (...args) => __impl.inferSelectorDisplayFields(...args)
  const inferSelectorKeywordFields = (...args) => __impl.inferSelectorKeywordFields(...args)
  const inferSelectorMappings = (...args) => __impl.inferSelectorMappings(...args)
  const inferSourceFieldByTokens = (...args) => __impl.inferSourceFieldByTokens(...args)
  const inferTargetFieldByTokens = (...args) => __impl.inferTargetFieldByTokens(...args)
  const inlineConfigSummary = (...args) => __impl.inlineConfigSummary(...args)
  const inlineConfigTagType = (...args) => __impl.inlineConfigTagType(...args)
  const isInactiveField = (...args) => __impl.isInactiveField(...args)
  const linkageRuleLabel = (...args) => __impl.linkageRuleLabel(...args)
  const linkageRuleSentence = (...args) => __impl.linkageRuleSentence(...args)
  const linkageTargetFieldOptions = (...args) => __impl.linkageTargetFieldOptions(...args)
  const loadTargetFields = (...args) => __impl.loadTargetFields(...args)
  const lowerFirst = (...args) => __impl.lowerFirst(...args)
  const lowerSnake = (...args) => __impl.lowerSnake(...args)
  const mappingConfigSummary = (...args) => __impl.mappingConfigSummary(...args)
  const mappingConfigTagType = (...args) => __impl.mappingConfigTagType(...args)
  const markDirty = (...args) => __impl.markDirty(...args)
  const markLinkageDirty = (...args) => __impl.markLinkageDirty(...args)
  const markRelationDirty = (...args) => __impl.markRelationDirty(...args)
  const normalizeDesignerRelationType = (...args) => __impl.normalizeDesignerRelationType(...args)
  const normalizeErFields = (...args) => __impl.normalizeErFields(...args)
  const normalizeErRelationType = (...args) => __impl.normalizeErRelationType(...args)
  const normalizeErStorageDataType = (...args) => __impl.normalizeErStorageDataType(...args)
  const normalizeLinkageRuleDraft = (...args) => __impl.normalizeLinkageRuleDraft(...args)
  const normalizedFieldToken = (...args) => __impl.normalizedFieldToken(...args)
  const optionMatchesToken = (...args) => __impl.optionMatchesToken(...args)
  const relationCollectionKey = (...args) => __impl.relationCollectionKey(...args)
  const relationLabel = (...args) => __impl.relationLabel(...args)
  const relationMatchSummary = (...args) => __impl.relationMatchSummary(...args)
  const relationMatchTagType = (...args) => __impl.relationMatchTagType(...args)
  const relationQuantityActionCode = (...args) => __impl.relationQuantityActionCode(...args)
  const relationSentence = (...args) => __impl.relationSentence(...args)
  const resolveLinkageDataSourceType = (...args) => __impl.resolveLinkageDataSourceType(...args)
  const selectorActiveFields = (...args) => __impl.selectorActiveFields(...args)
  const selectorCandidateFieldOptions = (...args) => __impl.selectorCandidateFieldOptions(...args)
  const selectorCandidateObjectCode = (...args) => __impl.selectorCandidateObjectCode(...args)
  const selectorConfigSummary = (...args) => __impl.selectorConfigSummary(...args)
  const selectorConfigTagType = (...args) => __impl.selectorConfigTagType(...args)
  const selectorContextFieldOptions = (...args) => __impl.selectorContextFieldOptions(...args)
  const selectorMappingCount = (...args) => __impl.selectorMappingCount(...args)
  const selectorTargetFieldOptions = (...args) => __impl.selectorTargetFieldOptions(...args)
  const sourceFieldLabel = (...args) => __impl.sourceFieldLabel(...args)
  const sourceFieldPlaceholder = (...args) => __impl.sourceFieldPlaceholder(...args)
  const syncLinkageModel = (...args) => __impl.syncLinkageModel(...args)
  const targetDisplayFieldOptions = (...args) => __impl.targetDisplayFieldOptions(...args)
  const targetFieldLabel = (...args) => __impl.targetFieldLabel(...args)
  const targetFieldOptions = (...args) => __impl.targetFieldOptions(...args)
  const targetFieldPlaceholder = (...args) => __impl.targetFieldPlaceholder(...args)
  const toErRelation = (...args) => __impl.toErRelation(...args)
  const toFieldPayload = (...args) => __impl.toFieldPayload(...args)
  const toPageField = (...args) => __impl.toPageField(...args)
  const unwrapFieldPath = (...args) => __impl.unwrapFieldPath(...args)
  const wrapExpression = (...args) => __impl.wrapExpression(...args)
  const message = useMessage()
  const loading = ref(false)
  const saving = ref(false)
  const activePanel = ref('er')
  const activeRelationKey = ref('')
  const activeLinkageRuleId = ref('')
  const erCandidateObjectCodes = ref([])
  const businessObjects = ref([])
  const localRelations = ref([])
  const localLinkage = ref(normalizeLinkageSchema())
  const targetFieldsMap = ref({})
  const targetFieldLoadingMap = ref({})
  const relationsLoaded = ref(false)
  const relationDirty = ref(false)

  const relationWizardVisible = ref(false)
  const relationWizardLoading = ref(false)
  const relationWizardForm = ref({
    relationType: 'DETAIL',
    targetObjectCode: '',
    sourceFieldCode: '',
    targetFieldCode: '',
    saveMode: 'merge',
    relationName: '',
    detailTabTitle: '',
  })

  const childSaveModeOptions = [
    { label: '全量替换', value: 'replace' },
    { label: '行级合并', value: 'merge' },
  ]

  const approvalQuantityOperationOptions = [
    { label: '增加数量', value: 'INBOUND' },
    { label: '扣减数量', value: 'OUTBOUND' },
  ]

  const selectorFilterSourceOptions = [
    { label: '固定值', value: 'static' },
    { label: '当前表单字段', value: 'formData' },
    { label: '当前记录字段', value: 'record' },
    { label: '当前行字段', value: 'row' },
    { label: '路由查询参数', value: 'query' },
    { label: '路由路径参数', value: 'params' },
  ]

  const linkageTypeOptions = [
    { label: '字典父子(parent_dict_code)', value: 'parentDictCode' },
    { label: '关联字典(linked_dict_type/value)', value: 'linkedDict' },
    { label: '远程参数过滤', value: 'remoteParam' },
    { label: '组织范围过滤', value: 'orgScope' },
    { label: '对象引用过滤', value: 'objectReference' },
  ]

  const emptyStrategyOptions = [
    { label: '显示空选项', value: 'empty' },
    { label: '显示全部选项', value: 'all' },
    { label: '禁用目标字段', value: 'disabled' },
  ]

  const methodOptions = [
    { label: 'GET', value: 'GET' },
    { label: 'POST', value: 'POST' },
  ]

  const sourceFieldOptions = computed(() => {
    const fields = (props.fields || []).map(toPageField).filter(field => field.field && !isInactiveField(field))
    return fields.map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  })

  const targetObjectOptions = computed(() => {
    const objects = (businessObjects.value || []).filter(item => item.objectCode !== props.objectCode)
    return objects.map(item => ({
      label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
      value: item.objectCode,
    }))
  })

  const objectNameMap = computed(() => new Map((businessObjects.value || []).map(item => [item.objectCode, item.objectName || item.objectCode])))
  const fieldMap = computed(() => new Map((props.fields || []).map(field => [field.fieldCode || field.field, toPageField(field)]).filter(([fieldCode]) => fieldCode)))
  const activeRelation = computed(() => {
    if (!localRelations.value.length)
      return null
    return localRelations.value.find(relation => relation.clientKey === activeRelationKey.value) || localRelations.value[0]
  })
  const erDiagramSummary = computed(() => {
    const relationCount = localRelations.value.filter(relation => relation.targetObjectCode).length
    return relationCount ? `${relationCount} 条关系` : '查看当前对象结构'
  })
  const erDiagramSubtitle = computed(() => {
    const modelCount = erDiagramModels.value.length
    const relationCount = localRelations.value.filter(relation => relation.targetObjectCode).length
    return `${props.objectName || props.objectCode || '当前对象'}：${modelCount} 个对象，${relationCount} 条关系`
  })
  const erDiagramModels = computed(() => buildErDiagramModels())

  watch(() => props.objectId, () => {
    // 切换对象时清空画布候选，避免把上一对象的无关表带到新画布
    erCandidateObjectCodes.value = onObjectSwitched()
    loadRelations()
  }, { immediate: true })

  watch(() => props.suiteCode, () => {
    loadBusinessObjects()
  }, { immediate: true })

  watch(
    () => [props.linkageSchema, props.fields],
    () => resetLinkageSchema(),
    { immediate: true, deep: true },
  )

  watch(localRelations, (relations) => {
    if (!relations.length) {
      activeRelationKey.value = ''
      return
    }
    if (!relations.some(relation => relation.clientKey === activeRelationKey.value))
      activeRelationKey.value = relations[0].clientKey
  }, { deep: false })

  onMounted(() => {
    loadBusinessObjects()
  })

  async function loadRelations() {
    if (!props.objectId) {
      localRelations.value = []
      relationsLoaded.value = true
      relationDirty.value = false
      return
    }
    loading.value = true
    relationsLoaded.value = false
    relationDirty.value = false
    try {
      const res = await businessObjectRelations(props.objectId)
      localRelations.value = (res.data || [])
        .filter(relation => !props.objectCode || !relation.sourceObjectCode || relation.sourceObjectCode === props.objectCode)
        .map(normalizeRelation)
      erCandidateObjectCodes.value = afterRelationsLoaded(
        erCandidateObjectCodes.value,
        localRelations.value.map(relation => relation.targetObjectCode),
      )
      relationsLoaded.value = true
      relationDirty.value = false
      emit('updated', localRelations.value)
      localRelations.value.forEach((relation) => {
        loadTargetFields(relation.targetObjectCode)
        loadTargetFields(selectorCandidateObjectCode(relation))
      })
    }
    catch (error) {
      message.error(error?.message || '关系配置加载失败')
    }
    finally {
      loading.value = false
    }
  }

  async function loadBusinessObjects() {
    try {
      const res = await businessObjectList({
        suiteCode: props.suiteCode || undefined,
      })
      const list = Array.isArray(res.data) ? res.data : []
      const seen = new Set()
      businessObjects.value = list.filter((item) => {
        if (!item.objectCode || seen.has(item.objectCode))
          return false
        seen.add(item.objectCode)
        return true
      })
      // 画布对象只来自：已配置关系的目标 + 用户在「画布对象」里显式勾选。
      erCandidateObjectCodes.value = afterBusinessObjectsLoaded(erCandidateObjectCodes.value)
      localRelations.value.forEach((relation) => {
        loadTargetFields(relation.targetObjectCode)
        loadTargetFields(selectorCandidateObjectCode(relation))
      })
      erCandidateObjectCodes.value.forEach(loadTargetFields)
    }
    catch {
      businessObjects.value = []
    }
  }

  function resetLinkageSchema() {
    mut.resettingLinkage = true
    try {
      const next = createLinkageSchemaFromFields(props.fields || [], props.linkageSchema || {})
      if (!isSameSchema(next, localLinkage.value))
        localLinkage.value = cloneSchema(next)
    }
    finally {
      mut.resettingLinkage = false
    }
  }

  function openRelationWizard() {
    const target = targetObjectOptions.value[0]
    if (!target) {
      message.warning('当前套件没有可关联的目标对象')
      return
    }
    relationWizardForm.value = {
      relationType: 'DETAIL',
      targetObjectCode: target.value,
      sourceFieldCode: '',
      targetFieldCode: '',
      saveMode: 'merge',
      relationName: '',
      detailTabTitle: '',
    }
    applyRelationWizardDefaults()
    relationWizardVisible.value = true
  }

  async function applyRelationWizardDefaults() {
    const { relationType, targetObjectCode } = relationWizardForm.value
    if (!targetObjectCode)
      return
    relationWizardLoading.value = true
    try {
      await loadTargetFields(targetObjectCode)
      const targetName = objectNameMap.value.get(targetObjectCode) || targetObjectCode
      relationWizardForm.value.sourceFieldCode = firstSourceField(relationType, targetObjectCode)
      relationWizardForm.value.targetFieldCode = firstTargetField(targetObjectCode, relationType)
      relationWizardForm.value.relationName = relationLabel({ relationType, targetObjectCode })
      relationWizardForm.value.detailTabTitle = targetName
    }
    finally {
      relationWizardLoading.value = false
    }
  }

  function confirmRelationWizard() {
    const form = relationWizardForm.value
    if (!form.targetObjectCode) {
      message.warning('请选择目标对象')
      return
    }
    if (!form.sourceFieldCode) {
      message.warning('请选择当前对象字段')
      return
    }
    if (!form.targetFieldCode) {
      message.warning('请选择目标对象字段')
      return
    }
    const relation = buildRelationDraft(form)
    localRelations.value.push(relation)
    activeRelationKey.value = relation.clientKey
    markRelationDirty()
    relationWizardVisible.value = false
    nextTick(() => scrollToRelationSection('basic'))
  }

  function buildRelationDraft(form = {}) {
    const relationType = normalizeDesignerRelationType(form.relationType)
    const targetName = objectNameMap.value.get(form.targetObjectCode) || form.targetObjectCode
    return {
      clientKey: createClientKey(),
      relationType,
      targetObjectCode: form.targetObjectCode,
      relationName: form.relationName || relationLabel({ relationType, targetObjectCode: form.targetObjectCode }),
      sourceFieldCode: form.sourceFieldCode,
      targetFieldCode: form.targetFieldCode,
      displayField: firstDisplayField(form.targetObjectCode, form.targetFieldCode),
      detailTabTitle: form.detailTabTitle || targetName,
      showInDetail: true,
      inlineCreateEnabled: canInlineEdit({ relationType }),
      inlineEditEnabled: canInlineEdit({ relationType }),
      saveMode: canInlineEdit({ relationType }) ? (form.saveMode || 'merge') : undefined,
      defaultFilter: '',
      selectorEnabled: false,
      selectorSuiteCode: props.suiteCode || '',
      selectorObjectCode: form.targetObjectCode,
      selectorTitle: '',
      selectorButtonText: '选择记录',
      selectorDisplayFields: [],
      selectorKeywordFields: [],
      selectorMappings: [],
      selectorSearchParams: [],
      description: '',
      status: 1,
      sortOrder: localRelations.value.length * 10 + 10,
    }
  }

  async function handleErConnect(connection = {}) {
    const currentObjectCode = props.objectCode || ''
    const sourceIsCurrent = connection.sourceModelCode === currentObjectCode
    const targetIsCurrent = connection.targetModelCode === currentObjectCode
    if (sourceIsCurrent === targetIsCurrent) {
      message.warning(sourceIsCurrent ? '不能在同一个对象内部创建自关联连线' : '请从当前对象连接到目标对象')
      return
    }

    const targetObjectCode = sourceIsCurrent ? connection.targetModelCode : connection.sourceModelCode
    const sourceFieldCode = sourceIsCurrent ? connection.sourceField : connection.targetField
    const targetFieldCode = sourceIsCurrent ? connection.targetField : connection.sourceField
    if (!targetObjectOptions.value.some(item => item.value === targetObjectCode)) {
      message.warning('目标对象不属于当前业务域，不能创建关系')
      return
    }

    await loadTargetFields(targetObjectCode)
    const existing = localRelations.value.find(relation => relation.targetObjectCode === targetObjectCode)
    if (existing) {
      existing.sourceFieldCode = sourceFieldCode
      existing.targetFieldCode = targetFieldCode
      existing.displayField = existing.displayField || firstDisplayField(targetObjectCode, targetFieldCode)
      activeRelationKey.value = existing.clientKey
      markRelationDirty()
      message.success('已更新关系字段连接')
    }
    else {
      const relation = buildRelationDraft({
        relationType: 'DETAIL',
        targetObjectCode,
        sourceFieldCode,
        targetFieldCode,
        saveMode: 'merge',
      })
      localRelations.value.push(relation)
      activeRelationKey.value = relation.clientKey
      markRelationDirty()
      message.success('关系已创建，请确认右侧配置后保存')
    }
    activePanel.value = 'relation'
    nextTick(() => scrollToRelationSection('basic'))
  }

  function addRelation() {
    openRelationWizard()
  }

  function selectRelation(clientKey) {
    activePanel.value = 'relation'
    activeRelationKey.value = clientKey || ''
  }

  function selectLinkageRule(ruleId) {
    activePanel.value = 'linkage'
    activeLinkageRuleId.value = ruleId || localLinkage.value.rules?.[0]?.ruleId || ''
  }

  function selectErDiagram() {
    activePanel.value = 'er'
    erCandidateObjectCodes.value.forEach(loadTargetFields)
  }

  function updateErCandidateObjects(values) {
    const relationTargets = localRelations.value.map(relation => relation.targetObjectCode).filter(Boolean)
    erCandidateObjectCodes.value = Array.from(new Set([...(values || []), ...relationTargets]))
    erCandidateObjectCodes.value.forEach(loadTargetFields)
  }

  function removeActiveRelation() {
    const current = activeRelation.value
    if (!current)
      return
    const index = localRelations.value.findIndex(relation => relation.clientKey === current.clientKey)
    if (index >= 0)
      removeRelation(index)
  }

  function scrollToRelationSection(key) {
    activePanel.value = key === 'linkage' ? 'linkage' : 'relation'
    nextTick(() => {
      const element = document.getElementById(`relation-section-${key}`)
      if (element)
        element.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  function addLinkageRule() {
    const source = sourceFieldOptions.value[0]
    const target = sourceFieldOptions.value.find(item => item.value !== source?.value) || sourceFieldOptions.value[1]
    if (!source || !target) {
      message.warning('至少需要两个可用字段才能配置级联')
      return
    }
    const rule = normalizeLinkageRuleDraft({
      ruleId: createLinkageRuleId(source.value, target.value),
      type: inferLinkageType(target.value),
      sourceField: source.value,
      targetField: target.value,
    })
    localLinkage.value = normalizeLinkageSchema({
      ...localLinkage.value,
      rules: [
        ...(localLinkage.value.rules || []),
        rule,
      ],
    })
    activePanel.value = 'linkage'
    activeLinkageRuleId.value = rule.ruleId
    syncLinkageModel(true)
    nextTick(() => scrollToRelationSection('linkage'))
  }

  function removeRelation(index) {
    localRelations.value.splice(index, 1)
    const nextRelation = localRelations.value[Math.max(0, index - 1)] || localRelations.value[0]
    activeRelationKey.value = nextRelation?.clientKey || ''
    markRelationDirty()
  }

  function removeLinkageRule(index) {
    const rules = [...(localLinkage.value.rules || [])]
    rules.splice(index, 1)
    localLinkage.value = normalizeLinkageSchema({
      ...localLinkage.value,
      rules,
    })
    if (!rules.some(rule => rule.ruleId === activeLinkageRuleId.value))
      activeLinkageRuleId.value = rules[0]?.ruleId || ''
    syncLinkageModel(true)
  }

  async function updateTargetObject(relation, value) {
    relation.targetObjectCode = value
    await loadTargetFields(value)
    const targetName = objectNameMap.value.get(value) || value
    relation.detailTabTitle = relation.detailTabTitle || targetName
    relation.relationName = relationLabel(relation)
    relation.sourceFieldCode = firstSourceField(relation.relationType, value, relation.sourceFieldCode)
    relation.targetFieldCode = firstTargetField(value, relation.relationType)
    relation.displayField = firstDisplayField(value, relation.targetFieldCode)
    if (!relation.selectorObjectCode)
      relation.selectorObjectCode = value || ''
    if (relation.selectorEnabled)
      await applySelectorDefaults(relation, false)
    markDirty()
  }

  function updateStatus(relation, value) {
    relation.status = value ? 1 : 0
    markDirty()
  }

  function updateShowInDetail(relation, value) {
    relation.showInDetail = !!value
    markDirty()
  }

  function updateInlineEdit(relation, value) {
    relation.inlineEditEnabled = canInlineEdit(relation) && !!value
    markDirty()
  }

  function updateInlineCreate(relation, value) {
    relation.inlineCreateEnabled = canInlineEdit(relation) && !!value
    markDirty()
  }

  function updateRelationSelectorEnabled(relation, value) {
    relation.selectorEnabled = canInlineEdit(relation) && !!value
    if (relation.selectorEnabled && !relation.selectorObjectCode)
      relation.selectorObjectCode = relation.targetObjectCode || ''
    if (relation.selectorEnabled)
      applySelectorDefaults(relation, false)
    markDirty()
  }

  function updateRelationApprovalQuantityEnabled(relation, value) {
    relation.approvalQuantityEnabled = canInlineEdit(relation) && !!value
    if (relation.approvalQuantityEnabled) {
      relation.approvalQuantityOperation = relation.approvalQuantityOperation || 'INBOUND'
      relation.approvalQuantityAccountField = relation.approvalQuantityAccountField || inferSourceFieldByTokens(['warehouse', 'store', 'depot', '仓库'])
      relation.approvalQuantityItemField = relation.approvalQuantityItemField || inferTargetFieldByTokens(relation, ['materialId', 'itemId', 'productId', '物料', '商品'])
      relation.approvalQuantityField = relation.approvalQuantityField || inferTargetFieldByTokens(relation, ['quantity', 'qty', 'num', '数量'])
      relation.approvalQuantityItemFallbackFields = normalizeTextList(relation.approvalQuantityItemFallbackFields)
    }
    markDirty()
  }

  async function updateSelectorObject(relation, value) {
    relation.selectorObjectCode = value || ''
    await loadTargetFields(selectorCandidateObjectCode(relation))
    await applySelectorDefaults(relation, false)
    markDirty()
  }

  function updateSelectorSearchParamSource(param, value) {
    param.sourceType = value || 'static'
    if (param.sourceType === 'static')
      param.sourceField = ''
    else
      param.staticValue = ''
    markDirty()
  }

  function addSelectorMapping(relation) {
    relation.selectorMappings = [
      ...(relation.selectorMappings || []),
      createSelectorMappingRow(),
    ]
    markDirty()
  }

  function removeSelectorMapping(relation, index) {
    relation.selectorMappings.splice(index, 1)
    markDirty()
  }

  function addSelectorSearchParam(relation) {
    relation.selectorSearchParams = [
      ...(relation.selectorSearchParams || []),
      createSelectorSearchParamRow(),
    ]
    markDirty()
  }

  function removeSelectorSearchParam(relation, index) {
    relation.selectorSearchParams.splice(index, 1)
    markDirty()
  }

  function updateLinkageType(rule, value) {
    const next = normalizeLinkageRuleDraft({
      ...rule,
      type: value,
      matchMode: value,
      dataSourceType: resolveLinkageDataSourceType(value),
    })
    Object.assign(rule, next)
    fillRuleFieldDefaults(rule)
    syncLinkageModel(true)
  }

  function updateLinkageSource(rule, value) {
    rule.sourceField = value || ''
    fillRuleFieldDefaults(rule)
    syncLinkageModel(true)
  }

  function updateLinkageTarget(rule, value) {
    rule.targetField = value || ''
    fillRuleFieldDefaults(rule)
    syncLinkageModel(true)
  }

  function updateRuleClearOnChange(rule, value) {
    rule.clearOnSourceChange = !!value
    syncLinkageModel(true)
  }

  function updateRuleEnabled(rule, value) {
    rule.enabled = !!value
    syncLinkageModel(true)
  }

  async function saveRelations() {
    if (!props.objectId)
      return
    const shouldSaveRelations = relationsLoaded.value
    if (shouldSaveRelations) {
      if (!relationsLoaded.value) {
        message.warning('关系配置还没有加载完成，请刷新后再保存')
        return
      }
      const invalidRelation = localRelations.value.find(relation => !relation.targetObjectCode || !relation.sourceFieldCode || !relation.targetFieldCode)
      if (invalidRelation) {
        message.warning(`请先补全「${invalidRelation.relationName || relationLabel(invalidRelation)}」的关联对象和匹配字段`)
        return
      }
      const missingDisplayRelation = localRelations.value.find(relation => relation.relationType === 'REFERENCE' && !relation.displayField)
      if (missingDisplayRelation) {
        message.warning(`请先为「${missingDisplayRelation.relationName || relationLabel(missingDisplayRelation)}」选择目标对象回显字段`)
        return
      }
      const invalidSelectorRelation = localRelations.value.find(relation => relation.selectorEnabled && !relation.selectorObjectCode)
      if (invalidSelectorRelation) {
        message.warning(`请先为「${invalidSelectorRelation.relationName || relationLabel(invalidSelectorRelation)}」选择子表选择器候选对象`)
        return
      }
    }
    saving.value = true
    try {
      const linkageSchema = normalizeLinkageSchema(localLinkage.value)
      const validation = validateLinkageSchema(linkageSchema, sourceFieldOptions.value.map(item => item.value))
      if (!validation.valid) {
        scrollToRelationSection('linkage')
        message.warning(validation.errors[0]?.message || '请先修复级联规则')
        return
      }
      const fields = applyLinkageSchemaToFields(props.fields || [], linkageSchema)
      const nextActions = buildDesignerActionsFromRelations(localRelations.value)
      const designerPayload = {
        fields: fields.map(toFieldPayload),
        linkageSchema,
        designerOptions: {
          ...(props.designerOptions || {}),
          actions: nextActions,
        },
      }
      if (shouldSaveRelations)
        designerPayload.relations = localRelations.value.map(toRelationPayload)
      emit('update:designerActions', nextActions)
      await saveBusinessObjectDesigner(props.objectId, designerPayload)
      message.success('关系与级联配置已保存')
      relationDirty.value = false
      emit('fieldsUpdated', fields)
      emit('update:linkageSchema', linkageSchema)
      emit('dirtyChange', false)
      await loadRelations()
    }
    finally {
      saving.value = false
    }
  }

  function normalizeRelation(relation = {}) {
    const config = parseRelationConfig(relation.relationConfig)
    const selector = normalizeRelationSelector(config.recordSelector || config.selector)
    const isKeyAlias = relation.relationName && relation.relationName === config.relationKey
    return {
      ...relation,
      clientKey: relation.id || createClientKey(),
      relationType: normalizeDesignerRelationType(relation.relationType),
      relationKey: config.relationKey || '',
      relationName: (!isKeyAlias && relation.relationName) || relation.targetObjectName || relationLabel(relation),
      targetObjectCode: relation.targetObjectCode || '',
      sourceFieldCode: relation.sourceFieldCode || '',
      targetFieldCode: relation.targetFieldCode || '',
      detailTabTitle: config.detailTabTitle || relation.relationName || relation.targetObjectName || relation.targetObjectCode || '',
      showInDetail: config.showInDetail !== false,
      inlineCreateEnabled: canInlineEdit(relation) && config.inlineCreateEnabled !== false && config.inlineCreateEnabled !== 'false',
      inlineEditEnabled: canInlineEdit(relation) && config.inlineEditEnabled !== false && config.inlineEditEnabled !== 'false',
      saveMode: normalizeChildSaveMode(config.saveMode),
      defaultFilter: config.defaultFilter || '',
      displayField: config.displayField || '',
      selectorEnabled: Boolean(selector.objectCode),
      selectorSuiteCode: selector.suiteCode,
      selectorObjectCode: selector.objectCode,
      selectorTitle: selector.title,
      selectorButtonText: selector.buttonText,
      selectorDisplayFields: normalizeDisplayFieldCodes(selector.displayFields),
      selectorKeywordFields: normalizeTextList(selector.keywordFields),
      selectorMappings: createSelectorMappingRows(selector.fieldMappings),
      selectorSearchParams: createSelectorSearchParamRows(selector.searchParams),
      selectorDisplayFieldsText: listToLines(selector.displayFields),
      selectorKeywordFieldsText: listToLines(selector.keywordFields),
      selectorMappingsText: mappingsToLines(selector.fieldMappings),
      selectorSearchParamsText: searchParamsToLines(selector.searchParams),
      ...resolveApprovalQuantityConfig(relation),
      status: relation.status ?? 1,
      sortOrder: relation.sortOrder ?? 0,
    }
  }

  function toRelationPayload(relation = {}) {
    return {
      id: relation.id,
      suiteCode: props.suiteCode,
      sourceObjectCode: props.objectCode,
      targetObjectCode: relation.targetObjectCode,
      relationType: normalizeDesignerRelationType(relation.relationType),
      relationName: relation.relationName || relationLabel(relation),
      sourceFieldCode: relation.sourceFieldCode,
      targetFieldCode: relation.targetFieldCode,
      relationConfig: buildRelationConfig(relation),
      description: relation.description,
      status: relation.status ?? 1,
      sortOrder: relation.sortOrder ?? 0,
    }
  }

  function buildRelationConfig(relation) {
    const config = {}
    config.relationKey = relationCollectionKey(relation)
    if (relation.detailTabTitle)
      config.detailTabTitle = relation.detailTabTitle
    config.showInDetail = relation.showInDetail !== false
    config.inlineCreateEnabled = canInlineEdit(relation) && relation.inlineCreateEnabled === true
    config.inlineEditEnabled = canInlineEdit(relation) && relation.inlineEditEnabled === true
    config.saveMode = normalizeChildSaveMode(relation.saveMode)
    if (relation.defaultFilter)
      config.defaultFilter = relation.defaultFilter
    if (relation.displayField)
      config.displayField = relation.displayField
    const selector = buildRelationSelectorConfig(relation)
    if (selector)
      config.recordSelector = selector
    return Object.keys(config).length ? JSON.stringify(config) : ''
  }

  function parseRelationConfig(value) {
    if (!value)
      return {}
    try {
      const parsed = JSON.parse(value)
      return parsed && typeof parsed === 'object' ? parsed : {}
    }
    catch {
      return {
        defaultFilter: value,
      }
    }
  }

  function normalizeRelationSelector(value) {
    const config = value && typeof value === 'object' ? value : {}
    return {
      suiteCode: String(config.suiteCode || '').trim(),
      objectCode: String(config.objectCode || '').trim(),
      title: String(config.title || config.selectorTitle || '').trim(),
      buttonText: String(config.buttonText || '').trim(),
      displayFields: normalizeTextList(config.displayFields),
      keywordFields: normalizeTextList(config.keywordFields),
      fieldMappings: normalizeMappingList(config.fieldMappings || config.mappings),
      searchParams: normalizeSearchParams(config.searchParams),
    }
  }

  function normalizeDisplayFieldCodes(value) {
    return normalizeTextList(value)
      .map(item => String(item || '').split(/\s*[:：]\s*/, 1)[0].trim())
      .filter(Boolean)
  }

  function createSelectorMappingRow(source = {}) {
    return {
      clientKey: createClientKey(),
      sourceField: String(source.sourceField || source.source || '').trim(),
      targetField: String(source.targetField || source.target || '').trim(),
    }
  }

  function createSelectorMappingRows(value) {
    return normalizeMappingList(value).map(createSelectorMappingRow)
  }

  function createSelectorSearchParamRow(source = {}) {
    const row = normalizeSelectorSearchParamRow(source)
    return {
      clientKey: createClientKey(),
      ...row,
    }
  }

  function createSelectorSearchParamRows(value) {
    return Object.entries(normalizeSearchParams(value)).map(([key, item]) => createSelectorSearchParamRow({
      paramKey: key,
      value: item,
    }))
  }

  function normalizeSelectorSearchParamRow(source = {}) {
    const paramKey = String(source.paramKey || source.key || '').trim()
    const sourceType = String(source.sourceType || '').trim()
    if (sourceType) {
      return {
        paramKey,
        sourceType,
        sourceField: String(source.sourceField || '').trim(),
        staticValue: source.staticValue ?? '',
      }
    }
    return {
      paramKey,
      ...parseSelectorParamValue(source.value ?? source.staticValue),
    }
  }

  function parseSelectorParamValue(value) {
    const textValue = String(value ?? '').trim()
    const matched = textValue.match(/^\$\{(formData|form|record|row|query|routeQuery|params)\.([^}]+)\}$/)
    if (!matched) {
      return {
        sourceType: 'static',
        sourceField: '',
        staticValue: textValue,
      }
    }
    const sourceTypeAlias = {
      form: 'formData',
      routeQuery: 'query',
    }
    return {
      sourceType: sourceTypeAlias[matched[1]] || matched[1],
      sourceField: matched[2],
      staticValue: '',
    }
  }

  function buildRelationSelectorConfig(relation = {}) {
    if (!relation.selectorEnabled)
      return null
    const selector = normalizeRelationSelector({
      suiteCode: relation.selectorSuiteCode || props.suiteCode,
      objectCode: relation.selectorObjectCode || relation.targetObjectCode,
      title: relation.selectorTitle,
      buttonText: relation.selectorButtonText,
      displayFields: buildSelectorDisplayFields(relation),
      keywordFields: normalizeTextList(relation.selectorKeywordFields?.length ? relation.selectorKeywordFields : relation.selectorKeywordFieldsText),
      fieldMappings: buildSelectorMappings(relation),
      searchParams: buildSelectorSearchParams(relation),
    })
    if (!selector.objectCode)
      return null
    const result = {
      objectCode: selector.objectCode,
    }
    if (selector.suiteCode)
      result.suiteCode = selector.suiteCode
    if (selector.title)
      result.title = selector.title
    if (selector.buttonText)
      result.buttonText = selector.buttonText
    if (selector.displayFields.length)
      result.displayFields = selector.displayFields
    if (selector.keywordFields.length)
      result.keywordFields = selector.keywordFields
    if (selector.fieldMappings.length)
      result.fieldMappings = selector.fieldMappings
    if (Object.keys(selector.searchParams).length)
      result.searchParams = selector.searchParams
    return result
  }

  function buildSelectorDisplayFields(relation = {}) {
    const codes = normalizeDisplayFieldCodes((relation.selectorDisplayFields || []).length
      ? relation.selectorDisplayFields
      : relation.selectorDisplayFieldsText)
    if (!codes.length)
      return []
    const fieldMap = new Map((targetFieldsMap.value[selectorCandidateObjectCode(relation)] || []).map(field => [field.field, field]))
    return codes.map((fieldCode) => {
      const field = fieldMap.get(fieldCode)
      const label = field ? businessFieldLabel(field) : ''
      return label && label !== fieldCode ? `${fieldCode}:${label}` : fieldCode
    })
  }

  function buildSelectorMappings(relation = {}) {
    const rows = Array.isArray(relation.selectorMappings) && relation.selectorMappings.length
      ? relation.selectorMappings
      : createSelectorMappingRows(relation.selectorMappingsText)
    return rows
      .map(row => ({
        sourceField: String(row.sourceField || '').trim(),
        targetField: String(row.targetField || '').trim(),
      }))
      .filter(row => row.sourceField && row.targetField)
  }

  function buildSelectorSearchParams(relation = {}) {
    const rows = Array.isArray(relation.selectorSearchParams) && relation.selectorSearchParams.length
      ? relation.selectorSearchParams
      : createSelectorSearchParamRows(relation.selectorSearchParamsText)
    return rows.reduce((result, row) => {
      const paramKey = String(row.paramKey || '').trim()
      if (!paramKey)
        return result
      const sourceType = row.sourceType || 'static'
      if (sourceType === 'static') {
        if (row.staticValue !== undefined && row.staticValue !== null && String(row.staticValue).trim() !== '')
          result[paramKey] = row.staticValue
        return result
      }
      const sourceField = String(row.sourceField || '').trim()
      if (sourceField)
        result[paramKey] = `\${${sourceType}.${sourceField}}`
      return result
    }, {})
  }

  function normalizeTextList(value) {
    if (Array.isArray(value))
      return value.map(item => String(item || '').trim()).filter(Boolean)
    if (typeof value === 'string')
      return parseTextList(value)
    return []
  }

  function parseTextList(value) {
    return String(value || '')
      .split(/[\n,，]/)
      .map(item => item.trim())
      .filter(Boolean)
  }

  function normalizeMappingList(value) {
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
    if (typeof value === 'string')
      return parseMappingLines(value)
    return []
  }

  function parseMappingLines(value) {
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

  function normalizeSearchParams(value) {
    if (!value)
      return {}
    if (typeof value === 'string')
      return parseSearchParamLines(value)
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

  function parseSearchParamLines(value) {
    const text = String(value || '').trim()
    if (!text)
      return {}
    if (text.startsWith('{')) {
      try {
        return normalizeSearchParams(JSON.parse(text))
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

  function listToLines(value) {
    return normalizeTextList(value).join('\n')
  }

  function mappingsToLines(value) {
    return normalizeMappingList(value)
      .map(item => `${item.sourceField}=${item.targetField}`)
      .join('\n')
  }

  function searchParamsToLines(value) {
    return Object.entries(normalizeSearchParams(value))
      .map(([key, item]) => `${key}=${item}`)
      .join('\n')
  }

  function normalizeChildSaveMode(value) {
    return String(value || '').toLowerCase() === 'merge' ? 'merge' : 'replace'
  }

  function resolveApprovalQuantityConfig(relation = {}) {
    const action = findApprovalQuantityAction(relation)
    const loopStep = action ? findForeachQuantityStep(action) : null
    const quantityStep = loopStep?.quantityStep || null
    const params = quantityStep?.stepConfig?.params || {}
    return {
      approvalQuantityEnabled: Boolean(action),
      approvalQuantityOperation: quantityStep?.stepConfig?.operationType || quantityStep?.stepConfig?.operation || 'INBOUND',
      approvalQuantityAccountField: unwrapFieldPath(params.accountCode, 'record.main.'),
      approvalQuantityItemField: unwrapFieldPath(params.itemCode, `${loopStep?.itemAlias || 'item'}.`),
      approvalQuantityField: unwrapFieldPath(params.quantity, `${loopStep?.itemAlias || 'item'}.`),
      approvalQuantityItemFallbackFields: normalizeTextList(quantityStep?.stepConfig?.itemCodeFallbackFields)
        .map(value => unwrapFieldPath(value, `${loopStep?.itemAlias || 'item'}.`))
        .filter(Boolean),
      approvalQuantityRemark: params.remark || '',
    }
  }

  function buildDesignerActionsFromRelations(relations = []) {
    const unmanagedActions = (props.designerActions || [])
      .filter(action => !isManagedApprovalQuantityAction(action))
    const managedActions = relations
      .filter(relation => relation.approvalQuantityEnabled === true)
      .map((relation, index) => createApprovalQuantityAction(relation, index))
    return [
      ...unmanagedActions,
      ...managedActions,
    ]
  }

  function createApprovalQuantityAction(relation = {}, index = 0) {
    const relationName = relation.relationName || relation.detailTabTitle || relationLabel(relation)
    const itemAlias = 'item'
    const collectionKey = relationCollectionKey(relation)
    const fallbackFields = normalizeTextList(relation.approvalQuantityItemFallbackFields)
      .map(field => `${itemAlias}.${field}`)
    return {
      actionCode: relationQuantityActionCode(relation),
      actionName: `${relationName}审批后数量更新`,
      actionPosition: 'DETAIL',
      actionType: 'COMMAND',
      status: 1,
      sortOrder: 500 + index,
      actionConfig: {
        managedBy: 'RELATION_APPROVAL_QUANTITY',
        relationKey: collectionKey,
        targetObjectCode: relation.targetObjectCode,
        triggerScene: 'FLOW_APPROVED',
        successBehavior: 'refreshList',
        steps: [
          {
            stepCode: `loop_${collectionKey}`,
            stepName: `逐行处理${relationName}`,
            stepType: 'FOREACH',
            rollbackOnFailure: true,
            stepConfig: {
              collectionPath: `record.children.${collectionKey}`,
              itemAlias,
              indexAlias: 'index',
              relationKey: collectionKey,
              relationName,
              targetObjectCode: relation.targetObjectCode,
              steps: [
                {
                  stepCode: `quantity_${collectionKey}`,
                  stepName: '更新数量',
                  stepType: 'DOMAIN_ACTION',
                  rollbackOnFailure: true,
                  stepConfig: {
                    actionType: 'QUANTITY',
                    operationType: relation.approvalQuantityOperation || 'INBOUND',
                    itemCodeFallbackFields: fallbackFields,
                    params: {
                      accountCode: wrapExpression(`record.main.${relation.approvalQuantityAccountField}`),
                      itemCode: wrapExpression(`${itemAlias}.${relation.approvalQuantityItemField}`),
                      quantity: wrapExpression(`${itemAlias}.${relation.approvalQuantityField}`),
                      sourceDetailId: wrapExpression(`${itemAlias}.id`),
                      remark: relation.approvalQuantityRemark || '',
                    },
                  },
                },
              ],
            },
          },
        ],
      },
    }
  }

  function findApprovalQuantityAction(relation = {}) {
    const relationKey = relationCollectionKey(relation)
    return (props.designerActions || []).find((action) => {
      if (!isManagedApprovalQuantityAction(action))
        return false
      const config = action.actionConfig || {}
      if (config.relationKey === relationKey || config.targetObjectCode === relation.targetObjectCode)
        return true
      const loopStep = findForeachQuantityStep(action)
      const collectionPath = loopStep?.foreachStep?.stepConfig?.collectionPath || ''
      return collectionPath.endsWith(relationKey)
    }) || null
  }

  function isManagedApprovalQuantityAction(action = {}) {
    const config = action.actionConfig || {}
    if (config.managedBy === 'RELATION_APPROVAL_QUANTITY')
      return true
    if (action.actionType !== 'COMMAND')
      return false
    if (!['FLOW_APPROVED', 'APPROVED', ''].includes(String(config.triggerScene || '').toUpperCase()))
      return false
    return Boolean(findForeachQuantityStep(action))
  }

  __impl.loadRelations = loadRelations
  __impl.loadBusinessObjects = loadBusinessObjects
  __impl.resetLinkageSchema = resetLinkageSchema
  __impl.openRelationWizard = openRelationWizard
  __impl.applyRelationWizardDefaults = applyRelationWizardDefaults
  __impl.confirmRelationWizard = confirmRelationWizard
  __impl.buildRelationDraft = buildRelationDraft
  __impl.handleErConnect = handleErConnect
  __impl.addRelation = addRelation
  __impl.selectRelation = selectRelation
  __impl.selectLinkageRule = selectLinkageRule
  __impl.selectErDiagram = selectErDiagram
  __impl.updateErCandidateObjects = updateErCandidateObjects
  __impl.removeActiveRelation = removeActiveRelation
  __impl.scrollToRelationSection = scrollToRelationSection
  __impl.addLinkageRule = addLinkageRule
  __impl.removeRelation = removeRelation
  __impl.removeLinkageRule = removeLinkageRule
  __impl.updateTargetObject = updateTargetObject
  __impl.updateStatus = updateStatus
  __impl.updateShowInDetail = updateShowInDetail
  __impl.updateInlineEdit = updateInlineEdit
  __impl.updateInlineCreate = updateInlineCreate
  __impl.updateRelationSelectorEnabled = updateRelationSelectorEnabled
  __impl.updateRelationApprovalQuantityEnabled = updateRelationApprovalQuantityEnabled
  __impl.updateSelectorObject = updateSelectorObject
  __impl.updateSelectorSearchParamSource = updateSelectorSearchParamSource
  __impl.addSelectorMapping = addSelectorMapping
  __impl.removeSelectorMapping = removeSelectorMapping
  __impl.addSelectorSearchParam = addSelectorSearchParam
  __impl.removeSelectorSearchParam = removeSelectorSearchParam
  __impl.updateLinkageType = updateLinkageType
  __impl.updateLinkageSource = updateLinkageSource
  __impl.updateLinkageTarget = updateLinkageTarget
  __impl.updateRuleClearOnChange = updateRuleClearOnChange
  __impl.updateRuleEnabled = updateRuleEnabled
  __impl.saveRelations = saveRelations
  __impl.normalizeRelation = normalizeRelation
  __impl.toRelationPayload = toRelationPayload
  __impl.buildRelationConfig = buildRelationConfig
  __impl.parseRelationConfig = parseRelationConfig
  __impl.normalizeRelationSelector = normalizeRelationSelector
  __impl.normalizeDisplayFieldCodes = normalizeDisplayFieldCodes
  __impl.createSelectorMappingRow = createSelectorMappingRow
  __impl.createSelectorMappingRows = createSelectorMappingRows
  __impl.createSelectorSearchParamRow = createSelectorSearchParamRow
  __impl.createSelectorSearchParamRows = createSelectorSearchParamRows
  __impl.normalizeSelectorSearchParamRow = normalizeSelectorSearchParamRow
  __impl.parseSelectorParamValue = parseSelectorParamValue
  __impl.buildRelationSelectorConfig = buildRelationSelectorConfig
  __impl.buildSelectorDisplayFields = buildSelectorDisplayFields
  __impl.buildSelectorMappings = buildSelectorMappings
  __impl.buildSelectorSearchParams = buildSelectorSearchParams
  __impl.normalizeTextList = normalizeTextList
  __impl.parseTextList = parseTextList
  __impl.normalizeMappingList = normalizeMappingList
  __impl.parseMappingLines = parseMappingLines
  __impl.normalizeSearchParams = normalizeSearchParams
  __impl.parseSearchParamLines = parseSearchParamLines
  __impl.listToLines = listToLines
  __impl.mappingsToLines = mappingsToLines
  __impl.searchParamsToLines = searchParamsToLines
  __impl.normalizeChildSaveMode = normalizeChildSaveMode
  __impl.resolveApprovalQuantityConfig = resolveApprovalQuantityConfig
  __impl.buildDesignerActionsFromRelations = buildDesignerActionsFromRelations
  __impl.createApprovalQuantityAction = createApprovalQuantityAction
  __impl.findApprovalQuantityAction = findApprovalQuantityAction
  __impl.isManagedApprovalQuantityAction = isManagedApprovalQuantityAction

  return {
    props, emit, __impl, mut, addLinkageRule, addRelation, addSelectorMapping, addSelectorSearchParam,
    applyRelationWizardDefaults, applySelectorDefaults, approvalConfigSummary, approvalConfigTagType, buildDesignerActionsFromRelations, buildErDiagramModels, buildRelationConfig, buildRelationDraft,
    buildRelationSelectorConfig, buildSelectorDisplayFields, buildSelectorMappings, buildSelectorSearchParams, businessFieldLabel, businessFieldOptionLabel, canInlineEdit, confirmRelationWizard,
    createApprovalQuantityAction, createClientKey, createLinkageRuleId, createSelectorMappingRow, createSelectorMappingRows, createSelectorSearchParamRow, createSelectorSearchParamRows, displayFieldLabel,
    fieldLabel, fieldOptionsForObject, fillRuleFieldDefaults, findApprovalQuantityAction, findForeachQuantityStep, findReferenceSourceField, findSemanticSourceField, firstDisplayField,
    firstSourceField, firstTargetField, handleErConnect, inferLinkageType, inferSelectorDisplayFields, inferSelectorKeywordFields, inferSelectorMappings, inferSourceFieldByTokens,
    inferTargetFieldByTokens, inlineConfigSummary, inlineConfigTagType, isInactiveField, isManagedApprovalQuantityAction, linkageRuleLabel, linkageRuleSentence, linkageTargetFieldOptions,
    listToLines, loadBusinessObjects, loadRelations, loadTargetFields, lowerFirst, lowerSnake, mappingConfigSummary, mappingConfigTagType,
    mappingsToLines, markDirty, markLinkageDirty, markRelationDirty, normalizeChildSaveMode, normalizeDesignerRelationType, normalizeDisplayFieldCodes, normalizeErFields,
    normalizeErRelationType, normalizeErStorageDataType, normalizeLinkageRuleDraft, normalizeMappingList, normalizeRelation, normalizeRelationSelector, normalizeSearchParams, normalizeSelectorSearchParamRow,
    normalizeTextList, normalizedFieldToken, openRelationWizard, optionMatchesToken, parseMappingLines, parseRelationConfig, parseSearchParamLines, parseSelectorParamValue,
    parseTextList, relationCollectionKey, relationLabel, relationMatchSummary, relationMatchTagType, relationQuantityActionCode, relationSentence, removeActiveRelation,
    removeLinkageRule, removeRelation, removeSelectorMapping, removeSelectorSearchParam, resetLinkageSchema, resolveApprovalQuantityConfig, resolveLinkageDataSourceType, saveRelations,
    scrollToRelationSection, searchParamsToLines, selectErDiagram, selectLinkageRule, selectRelation, selectorActiveFields, selectorCandidateFieldOptions, selectorCandidateObjectCode,
    selectorConfigSummary, selectorConfigTagType, selectorContextFieldOptions, selectorMappingCount, selectorTargetFieldOptions, sourceFieldLabel, sourceFieldPlaceholder, syncLinkageModel,
    targetDisplayFieldOptions, targetFieldLabel, targetFieldOptions, targetFieldPlaceholder, toErRelation, toFieldPayload, toPageField, toRelationPayload,
    unwrapFieldPath, updateErCandidateObjects, updateInlineCreate, updateInlineEdit, updateLinkageSource, updateLinkageTarget, updateLinkageType, updateRelationApprovalQuantityEnabled,
    updateRelationSelectorEnabled, updateRuleClearOnChange, updateRuleEnabled, updateSelectorObject, updateSelectorSearchParamSource, updateShowInDetail, updateStatus, updateTargetObject,
    wrapExpression, message, loading, saving, activePanel, activeRelationKey, activeLinkageRuleId, erCandidateObjectCodes,
    businessObjects, localRelations, localLinkage, targetFieldsMap, targetFieldLoadingMap, relationsLoaded, relationDirty, relationWizardVisible,
    relationWizardLoading, relationWizardForm, childSaveModeOptions, approvalQuantityOperationOptions, selectorFilterSourceOptions, linkageTypeOptions, emptyStrategyOptions, methodOptions,
    sourceFieldOptions, targetObjectOptions, objectNameMap, fieldMap, activeRelation, erDiagramSummary, erDiagramSubtitle, erDiagramModels,
  }
}
