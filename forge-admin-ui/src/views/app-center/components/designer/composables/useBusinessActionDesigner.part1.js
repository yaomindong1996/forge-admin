/** BusinessActionDesigner.vue setup part 1. */
import { NButton, NDropdown, NFormItemGi, NGrid, NInput, NInputNumber, NSelect, NTag } from 'naive-ui'
import { computed, defineComponent, h, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList } from '@/api/business-app'
import {
  BUSINESS_ACTION_EXECUTION_MODE,
  canUseBusinessActionStep,
  createCallApiBusinessActionStep,
  createDefaultBusinessActionConfig,
  createLocalBusinessActionStep,
  LOCAL_TRANSACTION_STEP_TYPES,
  resolveBusinessActionExecutionMode,
} from '../business-action-designer-protocol'
import CallApiStepConfigPanel from '../CallApiStepConfigPanel.vue'
export function applyBusinessActionDesignerPart1(props, emit) {
  const __impl = {}
  const mut = {}

  // part2 延迟实现：setup 结束后经 __impl 转发，避免拆分后跨 part 裸引用报错
  function businessFieldLabel(...args) {
    return __impl.businessFieldLabel(...args)
  }
  function cloneValue(...args) {
    return __impl.cloneValue(...args)
  }
  function collectMainFields(...args) {
    return __impl.collectMainFields(...args)
  }
  function ensureActionConfig(...args) {
    return __impl.ensureActionConfig(...args)
  }
  function ensureParams(...args) {
    return __impl.ensureParams(...args)
  }
  function ensureStepConfig(...args) {
    return __impl.ensureStepConfig(...args)
  }
  function getPathValue(...args) {
    return __impl.getPathValue(...args)
  }
  function isInactiveField(...args) {
    return __impl.isInactiveField(...args)
  }
  function mergeSelectedFieldOptions(...args) {
    return __impl.mergeSelectedFieldOptions(...args)
  }
  function normalizeStringList(...args) {
    return __impl.normalizeStringList(...args)
  }
  function parseRelationConfig(...args) {
    return __impl.parseRelationConfig(...args)
  }
  function resolveStep(...args) {
    return __impl.resolveStep(...args)
  }
  function stringValue(...args) {
    return __impl.stringValue(...args)
  }
  function stringifyJson(...args) {
    return __impl.stringifyJson(...args)
  }
  function toPageField(...args) {
    return __impl.toPageField(...args)
  }
  function unwrapExpression(...args) {
    return __impl.unwrapExpression(...args)
  }
  function wrapExpression(...args) {
    return __impl.wrapExpression(...args)
  }
  const INTERNAL_STEP = {
    FOREACH: 'FOREACH',
    DOMAIN_ACTION: 'DOMAIN_ACTION',
    START_FLOW: 'START_FLOW',
    CALL_API: 'CALL_API',
  }
  const INTERNAL_ACTION = {
    QUANTITY: 'QUANTITY',
  }
  const EXECUTION_MODE = BUSINESS_ACTION_EXECUTION_MODE
  const LOCAL_STEP_TYPES = LOCAL_TRANSACTION_STEP_TYPES

  const selectedActionIndex = ref(0)
  const actionConfigText = ref('')
  const jsonError = ref('')
  const businessObjects = ref([])
  const targetFieldsMap = ref({})
  const targetFieldLoadingMap = ref({})

  const sceneOptions = [
    { label: '审批通过后', value: 'FLOW_APPROVED' },
    { label: '审批驳回后', value: 'FLOW_REJECTED' },
    { label: '手动点击按钮', value: 'MANUAL' },
    { label: '触发器调用', value: 'TRIGGER' },
  ]
  const manualActionPositionOptions = [
    { label: '主记录详情按钮', value: 'DETAIL' },
    { label: '子表行按钮', value: 'CHILD_ROW' },
  ]
  const successBehaviorOptions = [
    { label: '刷新列表', value: 'refreshList' },
    { label: '无操作', value: 'none' },
  ]
  const permissionStrategyOptions = [
    { label: '隐藏按钮', value: 'hide' },
    { label: '禁用按钮', value: 'disable' },
  ]
  const executionModeOptions = [
    { label: '本地事务（可整体回滚）', value: EXECUTION_MODE.LOCAL_TRANSACTION },
    { label: '编排模式（幂等 + 补偿）', value: EXECUTION_MODE.ORCHESTRATION },
  ]
  const inputTypeOptions = [
    { label: '文本', value: 'text' },
    { label: '数字', value: 'number' },
    { label: '整数', value: 'integer' },
    { label: '金额（元输入，服务端存分）', value: 'money' },
    { label: '布尔', value: 'boolean' },
    { label: '日期', value: 'date' },
    { label: '日期时间', value: 'datetime' },
    { label: '下拉选项', value: 'select' },
  ]
  const localStepTypeOptions = [
    { label: '创建记录', value: 'CREATE_RECORD' },
    { label: '更新字段', value: 'UPDATE_FIELD' },
    { label: '调整数值', value: 'ADJUST_NUMBER' },
    { label: '变更状态', value: 'TRANSITION_STATUS' },
    { label: '状态门禁', value: 'ASSERT_RECORD' },
  ]
  const localStepMenuOptions = localStepTypeOptions.map(item => ({ label: item.label, key: item.value }))
  const sourceTypeOptions = [
    { label: '当前记录', value: 'record' },
    { label: '父记录', value: 'parent' },
    { label: '动作输入', value: 'form' },
    { label: '页面路由参数', value: 'context' },
    { label: '系统身份', value: 'system' },
    { label: '固定值', value: 'static' },
  ]
  const adjustmentOperatorOptions = [
    { label: '增加（ADD）', value: 'ADD' },
    { label: '扣减（SUBTRACT）', value: 'SUBTRACT' },
  ]
  const quantityOperationOptions = [
    { label: '增加数量', value: 'INBOUND' },
    { label: '扣减数量', value: 'OUTBOUND' },
    { label: '锁定数量', value: 'LOCK' },
    { label: '释放锁定', value: 'RELEASE' },
    { label: '转移数量', value: 'TRANSFER' },
  ]
  const paramLabels = {
    accountCode: '归属字段',
    itemCode: '对象字段',
    dimensionKey: '维度',
    quantity: '数量字段',
    sourceDetailId: '明细记录',
    remark: '备注',
    targetAccountCode: '目标归属字段',
    targetItemCode: '目标对象字段',
    targetDimensionKey: '目标维度',
  }

  const actionList = computed(() => Array.isArray(props.actions) ? props.actions : [])
  const approvalEntryActions = computed(() => actionList.value.filter(action => containsInternalStartFlow(action)))
  const automationActions = computed(() => actionList.value
    .map((action, originalIndex) => ({ action, originalIndex }))
    .filter(item => isAutomationAction(item.action)))
  const pageInteractionActions = computed(() => actionList.value
    .filter(action => !containsInternalStartFlow(action) && !isAutomationAction(action)))
  const pageInteractionActionNames = computed(() => pageInteractionActions.value
    .slice(0, 4)
    .map(action => action.actionName || action.actionCode || '未命名操作')
    .join('、') + (pageInteractionActions.value.length > 4 ? '等' : ''))
  const selectedAction = computed(() => actionList.value[selectedActionIndex.value] || automationActions.value[0]?.action || null)
  const rootSteps = computed(() => flattenRootSteps(selectedAction.value?.actionConfig || {}))
  const resolvedExecutionMode = computed(() => resolveExecutionMode(selectedAction.value))
  const isLocalTransaction = computed(() => resolvedExecutionMode.value === EXECUTION_MODE.LOCAL_TRANSACTION)
  const localStepViews = computed(() => rootSteps.value.filter(step => LOCAL_STEP_TYPES.includes(String(step.raw.stepType || '').toUpperCase())))
  const nonLocalStepCount = computed(() => flattenAllSteps(selectedAction.value?.actionConfig || {})
    .filter((step) => {
      const type = String(step.raw.stepType || '').toUpperCase()
      return type && !canUseBusinessActionStep(EXECUTION_MODE.LOCAL_TRANSACTION, type)
    })
    .length)
  const inputSchemaRows = computed(() => Array.isArray(selectedAction.value?.actionConfig?.inputSchema)
    ? selectedAction.value.actionConfig.inputSchema
    : [])
  const callApiRecordFieldOptions = computed(() => collectMainFields(props.fields, props.modelSchema)
    .map(toPageField)
    .filter(field => !isInactiveField(field))
    .map((field) => {
      const code = field.sourceField || field.field || field.fieldCode
      return code
        ? { label: `${businessFieldLabel(field)}（${code}）`, value: `record.main.${code}` }
        : null
    })
    .filter(Boolean))
  const callApiFormFieldOptions = computed(() => [
    ...inputSchemaRows.value
      .filter(field => field?.name)
      .map(field => ({ label: `${field.label || field.name}（${field.name}）`, value: field.name })),
    ...callApiRecordFieldOptions.value,
  ])
  const targetConfigOptions = computed(() => buildTargetConfigOptions())
  const actionRelations = computed(() => buildActionRelations(props.modelSchema, props.relations))
  const collectionPathOptions = computed(() => buildCollectionPathOptions(actionRelations.value))
  const childRelationOptions = computed(() => {
    const detailRelations = actionRelations.value.filter(relation => isDetailRelation(relation))
    const options = detailRelations.map((relation) => {
      const isKeyName = relation.relationName && relation.relationName === relation.collectionKey
      return {
        label: (!isKeyName && relation.relationName)
          || relation.detailTabTitle
          || relation.targetObjectName
          || relation.modelName
          || relation.relationName
          || relation.collectionKey,
        value: relation.collectionKey,
      }
    })
    const currentValue = String(selectedAction.value?.actionConfig?.relationKey || '').trim()
    if (currentValue && !options.some(opt => opt.value === currentValue)) {
      const matched = detailRelations[0]
      const fallbackLabel = matched
        ? (matched.relationName || matched.targetObjectName || matched.modelName || currentValue)
        : currentValue
      options.push({ label: fallbackLabel, value: currentValue })
    }
    return options
  })
  const selectedManualActionPosition = computed(() => {
    const position = String(selectedAction.value?.actionPosition || 'DETAIL')
      .replace('-', '_')
      .toUpperCase()
    return position === 'CHILD_ROW' ? 'CHILD_ROW' : 'DETAIL'
  })

  watch(() => props.suiteCode, () => {
    loadBusinessObjects()
  }, { immediate: true })

  watch([() => props.relations, () => props.modelSchema, businessObjects], () => {
    preloadRelationFields()
  }, { immediate: true, deep: true })

  watch(automationActions, (items) => {
    if (!items.length) {
      selectedActionIndex.value = 0
      return
    }
    if (!items.some(item => item.originalIndex === selectedActionIndex.value))
      selectedActionIndex.value = items[0].originalIndex
  }, { immediate: true })

  watch(selectedAction, (action) => {
    actionConfigText.value = stringifyJson(action?.actionConfig || {})
    jsonError.value = ''
  }, { immediate: true })

  const BusinessQuantityStepCard = defineComponent({
    name: 'BusinessQuantityStepCard',
    props: {
      step: {
        type: Object,
        required: true,
      },
      fieldOptions: {
        type: Array,
        default: () => [],
      },
    },
    emits: ['patchStep', 'patchConfig', 'patchParam', 'patchFallback', 'remove'],
    setup(cardProps, { emit: cardEmit }) {
      const paramValue = key => cardProps.step.config?.params?.[key] ?? ''
      const fieldSelect = (key, placeholder = '选择字段') => h(NSelect, {
        'filterable': true,
        'options': mergeSelectedFieldOptions(cardProps.fieldOptions, [unwrapExpression(paramValue(key))]),
        'value': unwrapExpression(paramValue(key)),
        placeholder,
        'onUpdate:value': value => cardEmit('patchParam', { key, value: wrapExpression(value) }),
      })
      const staticInput = (key, placeholder = '固定值') => h(NInput, {
        'value': stringValue(paramValue(key)),
        placeholder,
        'onUpdate:value': value => cardEmit('patchParam', { key, value }),
      })
      const fallbackSelect = key => h(NSelect, {
        'multiple': true,
        'filterable': true,
        'clearable': true,
        'options': mergeSelectedFieldOptions(cardProps.fieldOptions, normalizeStringList(cardProps.step.config?.[`${key}FallbackFields`])),
        'value': normalizeStringList(cardProps.step.config?.[`${key}FallbackFields`]),
        'placeholder': '主字段为空时按顺序尝试其他字段',
        'onUpdate:value': value => cardEmit('patchFallback', { key, value }),
      })
      return () => h('div', { class: 'quantity-card' }, [
        h('div', { class: 'quantity-card-head' }, [
          h('div', null, [
            h('strong', null, cardProps.step.raw.stepName || '数量处理'),
            h('span', null, '更新数量台账或库存余额'),
          ]),
          h(NButton, { size: 'tiny', quaternary: true, type: 'error', onClick: () => cardEmit('remove') }, { default: () => '删除' }),
        ]),
        h(NGrid, { cols: 3, xGap: 12, yGap: 8, responsive: 'screen' }, {
          default: () => [
            h(NFormItemGi, { label: '处理方式' }, {
              default: () => h(NSelect, {
                'options': quantityOperationOptions,
                'value': cardProps.step.config.operationType || cardProps.step.config.operation || 'INBOUND',
                'onUpdate:value': value => cardEmit('patchConfig', { operationType: value }),
              }),
            }),
            h(NFormItemGi, { label: paramLabels.accountCode }, { default: () => fieldSelect('accountCode') }),
            h(NFormItemGi, { label: paramLabels.quantity }, { default: () => fieldSelect('quantity') }),
            h(NFormItemGi, { label: paramLabels.itemCode }, { default: () => fieldSelect('itemCode') }),
            h(NFormItemGi, { label: '备用识别字段' }, { default: () => fallbackSelect('itemCode') }),
            h(NFormItemGi, { label: paramLabels.sourceDetailId }, { default: () => fieldSelect('sourceDetailId') }),
            h(NFormItemGi, { label: paramLabels.dimensionKey }, { default: () => staticInput('dimensionKey', '留空表示默认维度') }),
            h(NFormItemGi, { label: paramLabels.remark, span: 2 }, { default: () => staticInput('remark', '备注') }),
          ],
        }),
      ])
    },
  })

  function addAutomationAction() {
    const actions = cloneValue(actionList.value)
    const index = actions.length + 1
    actions.push({
      actionCode: `automation_${Date.now()}`,
      actionName: `自动化 ${index}`,
      actionPosition: 'DETAIL',
      actionType: 'COMMAND',
      status: 1,
      sortOrder: index * 10,
      actionConfig: createDefaultBusinessActionConfig({
        triggerScene: 'FLOW_APPROVED',
        successBehavior: 'refreshList',
      }),
    })
    emitActions(actions)
    selectedActionIndex.value = actions.length - 1
  }

  function resolveExecutionMode(action = {}) {
    return resolveBusinessActionExecutionMode(action.actionConfig || {})
  }

  function updateExecutionMode(mode) {
    const normalized = String(mode || '').toUpperCase()
    if (normalized === EXECUTION_MODE.LOCAL_TRANSACTION && nonLocalStepCount.value) {
      window.$message?.warning('当前动作包含流程、消息或领域步骤，请先移除这些步骤后再切换本地事务')
      return
    }
    patchActionConfig({ executionMode: normalized === EXECUTION_MODE.ORCHESTRATION
      ? EXECUTION_MODE.ORCHESTRATION
      : EXECUTION_MODE.LOCAL_TRANSACTION })
  }

  function addInputSchemaField() {
    const rows = cloneValue(inputSchemaRows.value)
    rows.push({ name: `input_${rows.length + 1}`, label: `输入字段 ${rows.length + 1}`, type: 'text', required: false })
    patchActionConfig({ inputSchema: rows })
  }

  function patchInputSchemaField(index, patch = {}) {
    const rows = cloneValue(inputSchemaRows.value)
    if (!rows[index])
      return
    rows[index] = { ...rows[index], ...patch }
    if (rows[index].type === 'select' && !Array.isArray(rows[index].options))
      rows[index].options = []
    patchActionConfig({ inputSchema: rows })
  }

  function removeInputSchemaField(index) {
    const rows = cloneValue(inputSchemaRows.value)
    rows.splice(index, 1)
    patchActionConfig({ inputSchema: rows })
  }

  function inputOptionsText(field = {}) {
    return (Array.isArray(field.options) ? field.options : [])
      .map(option => `${option?.label ?? option?.value ?? ''}=${option?.value ?? option?.label ?? ''}`)
      .join(', ')
  }

  function updateInputOptions(index, value) {
    const options = String(value || '').split(',').map(item => item.trim()).filter(Boolean).map((item) => {
      const [label, ...valueParts] = item.split('=')
      const optionValue = valueParts.length ? valueParts.join('=').trim() : label
      return { label: label.trim(), value: optionValue }
    })
    patchInputSchemaField(index, { options })
  }

  function addLocalStep(stepType = 'CREATE_RECORD') {
    const type = LOCAL_STEP_TYPES.includes(String(stepType).toUpperCase()) ? String(stepType).toUpperCase() : 'CREATE_RECORD'
    const actions = cloneValue(actionList.value)
    const action = actions[selectedActionIndex.value]
    if (!action)
      return
    const config = ensureActionConfig(action)
    if (!Array.isArray(config.steps))
      config.steps = []
    const step = createLocalStep(type, config.steps.length + 1)
    step.stepConfig.targetConfigKey = props.configKey || props.modelSchema?.configKey || props.objectCode || ''
    config.steps.push(step)
    config.executionMode = EXECUTION_MODE.LOCAL_TRANSACTION
    emitActions(actions)
  }

  function createLocalStep(stepType, index) {
    return createLocalBusinessActionStep(stepType, index)
  }

  function updateLocalStepType(step, stepType) {
    const nextType = LOCAL_STEP_TYPES.includes(String(stepType || '').toUpperCase()) ? String(stepType).toUpperCase() : 'CREATE_RECORD'
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, step)
    if (!cloned)
      return
    const previousConfig = ensureStepConfig(cloned)
    const nextConfig = {
      targetConfigKey: previousConfig.targetConfigKey || '',
      rollbackOnFailure: true,
    }
    if (nextType !== 'CREATE_RECORD')
      nextConfig.targetRecordIdField = previousConfig.targetRecordIdField || 'record.id'
    if (nextType === 'CREATE_RECORD' || nextType === 'UPDATE_FIELD')
      nextConfig.fieldMappings = Array.isArray(previousConfig.fieldMappings) ? previousConfig.fieldMappings : []
    if (nextType === 'ADJUST_NUMBER')
      nextConfig.adjustments = Array.isArray(previousConfig.adjustments) ? previousConfig.adjustments : []
    if (nextType === 'TRANSITION_STATUS') {
      nextConfig.statusField = previousConfig.statusField || ''
      nextConfig.fromValue = previousConfig.fromValue ?? ''
      nextConfig.toValue = previousConfig.toValue ?? ''
    }
    if (nextType === 'ASSERT_RECORD') {
      nextConfig.expectedFieldMappings = Array.isArray(previousConfig.expectedFieldMappings)
        ? previousConfig.expectedFieldMappings
        : []
    }
    cloned.stepType = nextType
    cloned.stepName = localStepTypeOptions.find(item => item.value === nextType)?.label || nextType
    cloned.stepConfig = nextConfig
    emitActions(actions)
  }

  function stepTypeNeedsRecordId(stepType) {
    return String(stepType || '').toUpperCase() !== 'CREATE_RECORD'
  }

  function mappingSourcePlaceholder(sourceType) {
    const placeholders = {
      form: '动作输入字段名，如 quantity',
      context: '路由参数，如 routeQuery.scene',
      system: '系统字段，如 userId',
      record: '当前记录字段，如 amount',
      parent: '父记录字段，如 status',
    }
    return placeholders[sourceType] || placeholders.record
  }

  function stepFieldMappings(step) {
    return Array.isArray(step.config?.fieldMappings) ? step.config.fieldMappings : []
  }

  function addFieldMapping(step) {
    const mappings = [...stepFieldMappings(step), { targetField: '', sourceType: 'form', sourceField: '' }]
    patchStepConfig(step, { fieldMappings: mappings })
  }

  function patchFieldMapping(step, index, patch = {}) {
    const mappings = cloneValue(stepFieldMappings(step))
    if (!mappings[index])
      return
    mappings[index] = { ...mappings[index], ...patch }
    if (patch.sourceType === 'static') {
      delete mappings[index].sourceField
    }
    else if (patch.sourceType) {
      delete mappings[index].value
      delete mappings[index].staticValue
    }
    patchStepConfig(step, { fieldMappings: mappings })
  }

  function removeFieldMapping(step, index) {
    const mappings = cloneValue(stepFieldMappings(step))
    mappings.splice(index, 1)
    patchStepConfig(step, { fieldMappings: mappings })
  }

  function stepExpectedFieldMappings(step) {
    return Array.isArray(step.config?.expectedFieldMappings) ? step.config.expectedFieldMappings : []
  }

  function addExpectedFieldMapping(step) {
    patchStepConfig(step, {
      expectedFieldMappings: [
        ...stepExpectedFieldMappings(step),
        { targetField: '', value: '' },
      ],
    })
  }

  function patchExpectedFieldMapping(step, index, patch = {}) {
    const mappings = cloneValue(stepExpectedFieldMappings(step))
    if (!mappings[index])
      return
    mappings[index] = { ...mappings[index], ...patch }
    patchStepConfig(step, { expectedFieldMappings: mappings })
  }

  function removeExpectedFieldMapping(step, index) {
    patchStepConfig(step, {
      expectedFieldMappings: stepExpectedFieldMappings(step)
        .filter((_item, itemIndex) => itemIndex !== index),
    })
  }

  function stepAdjustments(step) {
    return Array.isArray(step.config?.adjustments) ? step.config.adjustments : []
  }

  function addNumberAdjustment(step) {
    patchStepConfig(step, {
      adjustments: [...stepAdjustments(step), { targetField: '', sourceType: 'form', sourceField: '', operator: 'ADD' }],
    })
  }

  function patchAdjustment(step, index, patch = {}) {
    const adjustments = cloneValue(stepAdjustments(step))
    if (!adjustments[index])
      return
    adjustments[index] = { ...adjustments[index], ...patch }
    patchStepConfig(step, { adjustments })
  }

  function removeNumberAdjustment(step, index) {
    const adjustments = cloneValue(stepAdjustments(step))
    adjustments.splice(index, 1)
    patchStepConfig(step, { adjustments })
  }

  function buildTargetConfigOptions() {
    const options = []
    const seen = new Set()
    const add = (value, label) => {
      const code = String(value || '').trim()
      if (!code || seen.has(code))
        return
      seen.add(code)
      options.push({ label: `${label || code}（${code}）`, value: code })
    }
    add(props.configKey || props.modelSchema?.configKey || props.objectCode, props.modelSchema?.objectName || props.modelSchema?.object?.name || '当前对象')
    actionRelations.value.forEach(relation => add(
      relation.targetConfigKey || relation.targetObjectCode || relation.objectCode,
      relation.relationName || relation.modelName || '关联对象',
    ))
    businessObjects.value.forEach(item => add(
      item.configKey || item.objectCode,
      item.objectName || item.name || item.objectCode,
    ))
    return options
  }

  function targetFieldOptions(step = {}) {
    const targetCode = String(step.config?.targetConfigKey || '').trim()
    const currentCode = String(props.configKey || props.modelSchema?.configKey || props.objectCode || props.modelSchema?.objectCode || '').trim()
    const fields = targetCode && targetCode !== currentCode
      ? (targetFieldsMap.value[targetCode] || [])
      : collectMainFields(props.fields, props.modelSchema).map(toPageField)
    const options = fields.filter(field => !isInactiveField(field)).map(field => ({
      label: businessFieldLabel(field),
      value: field.sourceField || field.field || field.fieldCode,
    })).filter(item => item.value)
    const selected = [
      ...stepFieldMappings(step).map(item => item.targetField),
      ...stepAdjustments(step).map(item => item.targetField),
    ]
    return mergeSelectedFieldOptions(options, selected)
  }

  function patchSelectedAction(patch = {}) {
    const actions = cloneValue(actionList.value)
    if (!actions[selectedActionIndex.value])
      return
    actions[selectedActionIndex.value] = {
      ...actions[selectedActionIndex.value],
      ...patch,
    }
    emitActions(actions)
  }

  function patchActionConfig(patch = {}) {
    const actions = cloneValue(actionList.value)
    const action = actions[selectedActionIndex.value]
    if (!action)
      return
    action.actionConfig = {
      ...(action.actionConfig || {}),
      ...patch,
    }
    emitActions(actions)
  }

  function updateActionScene(scene) {
    const actions = cloneValue(actionList.value)
    const action = actions[selectedActionIndex.value]
    if (!action)
      return
    const config = ensureActionConfig(action)
    config.triggerScene = scene
    if (scene !== 'MANUAL' && String(action.actionPosition || '').toUpperCase() === 'CHILD_ROW') {
      action.actionPosition = 'DETAIL'
      delete config.relationKey
    }
    emitActions(actions)
  }

  function updateManualActionPosition(position) {
    const actions = cloneValue(actionList.value)
    const action = actions[selectedActionIndex.value]
    if (!action)
      return
    const config = ensureActionConfig(action)
    action.actionPosition = position === 'CHILD_ROW' ? 'CHILD_ROW' : 'DETAIL'
    config.triggerScene = 'MANUAL'
    if (action.actionPosition === 'CHILD_ROW')
      config.relationKey = config.relationKey || childRelationOptions.value[0]?.value || ''
    else
      delete config.relationKey
    emitActions(actions)
  }

  function updateChildActionRelation(relationKey) {
    patchActionConfig({ relationKey: relationKey || '' })
  }

  function addDetailQuantityFlow() {
    const actions = cloneValue(actionList.value)
    const action = actions[selectedActionIndex.value]
    if (!action)
      return
    const config = ensureActionConfig(action)
    if (!Array.isArray(config.steps))
      config.steps = []
    const collectionPath = collectionPathOptions.value[0]?.value || ''
    config.steps.push({
      stepCode: `detail_loop_${Date.now()}`,
      stepName: '逐行处理明细',
      stepType: INTERNAL_STEP.FOREACH,
      rollbackOnFailure: true,
      stepConfig: {
        collectionPath,
        itemAlias: 'item',
        indexAlias: 'index',
        steps: [createQuantityStep()],
      },
    })
    emitActions(actions)
  }

  function addCallApiStep() {
    const actions = cloneValue(actionList.value)
    const action = actions[selectedActionIndex.value]
    if (!action)
      return
    const config = ensureActionConfig(action)
    if (!Array.isArray(config.steps))
      config.steps = []
    config.steps.push(createCallApiBusinessActionStep(config.steps.length + 1))
    config.executionMode = EXECUTION_MODE.ORCHESTRATION
    emitActions(actions)
  }

  function updateCallApiStepConfig(step, value = {}) {
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, step)
    if (!cloned)
      return
    const nextConfig = { ...(value || {}) }
    const failureStrategy = String(nextConfig.failureStrategy || 'THROW').toUpperCase()
    cloned.stepConfig = nextConfig
    cloned.rollbackOnFailure = failureStrategy !== 'LOG_AND_CONTINUE'
    ensureActionConfig(actions[selectedActionIndex.value]).executionMode = EXECUTION_MODE.ORCHESTRATION
    emitActions(actions)
  }

  function addQuantityStep(parentStep) {
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, parentStep)
    if (!cloned)
      return
    const config = ensureStepConfig(cloned)
    if (!Array.isArray(config.steps))
      config.steps = []
    config.steps.push(createQuantityStep())
    emitActions(actions)
  }

  function patchStep(step, patch = {}) {
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, step)
    if (!cloned)
      return
    Object.assign(cloned, patch)
    emitActions(actions)
  }

  function patchStepConfig(step, patch = {}) {
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, step)
    if (!cloned)
      return
    Object.assign(ensureStepConfig(cloned), patch)
    emitActions(actions)
  }

  function updateStepCollection(step, collectionPath) {
    const relation = relationByCollectionPath(collectionPath)
    patchStepConfig(step, {
      collectionPath,
      itemAlias: step.config?.itemAlias || 'item',
      indexAlias: step.config?.indexAlias || 'index',
      relationKey: relation?.collectionKey || '',
      relationName: relation?.relationName || relation?.modelName || '',
      targetObjectCode: relation?.targetObjectCode || '',
    })
  }

  function updateStepParam(step, key, value) {
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, step)
    if (!cloned)
      return
    const params = ensureParams(ensureStepConfig(cloned))
    params[key] = value
    emitActions(actions)
  }

  function updateFallbackFields(step, key, value) {
    const actions = cloneValue(actionList.value)
    const cloned = resolveStep(actions, step)
    if (!cloned)
      return
    ensureStepConfig(cloned)[`${key}FallbackFields`] = normalizeStringList(value)
    emitActions(actions)
  }

  function removeStep(step) {
    const actions = cloneValue(actionList.value)
    const parentSteps = getPathValue(actions[selectedActionIndex.value]?.actionConfig, step.parentPath)
    if (!Array.isArray(parentSteps))
      return
    parentSteps.splice(step.index, 1)
    emitActions(actions)
  }

  function removeAction(originalIndex) {
    const actions = cloneValue(actionList.value)
    if (originalIndex < 0 || originalIndex >= actions.length)
      return
    actions.splice(originalIndex, 1)
    emitActions(actions)
    if (selectedActionIndex.value >= actions.length)
      selectedActionIndex.value = Math.max(0, actions.length - 1)
    else if (selectedActionIndex.value > originalIndex)
      selectedActionIndex.value--
  }

  function applyActionConfigText() {
    jsonError.value = ''
    let parsed
    try {
      parsed = actionConfigText.value?.trim() ? JSON.parse(actionConfigText.value) : {}
    }
    catch (error) {
      jsonError.value = error?.message || 'JSON 格式不正确'
      return
    }
    patchSelectedAction({
      actionConfig: parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {},
    })
  }

  function emitActions(actions) {
    emit('update:actions', actions)
    emit('dirtyChange', true)
  }

  function containsInternalStartFlow(action = {}) {
    return normalizeActionType(action) === INTERNAL_STEP.START_FLOW
      || flattenAllSteps(action.actionConfig || {}).some(step => isInternalStepType(step.raw, INTERNAL_STEP.START_FLOW))
  }

  function isAutomationAction(action = {}) {
    if (containsInternalStartFlow(action))
      return false
    const type = normalizeActionType(action)
    return type === 'COMMAND'
      || type === 'TRIGGER'
      || hasConfiguredSteps(action.actionConfig)
  }

  function hasConfiguredSteps(actionConfig = {}) {
    return (Array.isArray(actionConfig?.steps) && actionConfig.steps.length > 0)
      || (Array.isArray(actionConfig?.stepList) && actionConfig.stepList.length > 0)
  }

  function normalizeActionType(action = {}) {
    return String(action.actionType || '')
      .replace(/([a-z])([A-Z])/g, '$1_$2')
      .replace(/-/g, '_')
      .trim()
      .toUpperCase()
  }

  function flattenRootSteps(actionConfig = {}) {
    const steps = Array.isArray(actionConfig.steps) ? actionConfig.steps : []
    return steps.map((step, index) => buildStepVM(step, ['steps', index], ['steps'], index, []))
  }

  function childBusinessSteps(step) {
    const children = Array.isArray(step.config.steps) ? step.config.steps : []
    const childParentPath = [...step.configPath, 'steps']
    const aliases = [
      ...step.aliases,
      {
        alias: step.config.itemAlias || 'item',
        collectionPath: step.config.collectionPath || '',
      },
    ]
    return children.map((child, index) => buildStepVM(child, [...childParentPath, index], childParentPath, index, aliases))
  }

  function flattenAllSteps(actionConfig = {}) {
    const result = []
    function visit(steps, path, parentPath, aliases) {
      if (!Array.isArray(steps))
        return
      steps.forEach((step, index) => {
        const vm = buildStepVM(step, [...path, index], parentPath.length ? parentPath : path, index, aliases)
        result.push(vm)
        visit(vm.config.steps, [...vm.configPath, 'steps'], [...vm.configPath, 'steps'], vm.aliases)
      })
    }
    visit(actionConfig.steps, ['steps'], ['steps'], [])
    return result
  }

  function buildStepVM(raw, path, parentPath, index, aliases) {
    const config = raw?.stepConfig && typeof raw.stepConfig === 'object' ? raw.stepConfig : raw || {}
    const configPath = raw?.stepConfig && typeof raw.stepConfig === 'object' ? [...path, 'stepConfig'] : path
    return {
      raw: raw || {},
      index,
      path,
      parentPath,
      config,
      configPath,
      key: path.join('.'),
      aliases,
    }
  }

  function createQuantityStep() {
    return {
      stepCode: `quantity_${Date.now()}`,
      stepName: '数量处理',
      stepType: INTERNAL_STEP.DOMAIN_ACTION,
      rollbackOnFailure: true,
      stepConfig: {
        actionType: INTERNAL_ACTION.QUANTITY,
        operationType: 'INBOUND',
        params: {
          accountCode: '',
          itemCode: '',
          quantity: '',
          sourceDetailId: '',
          dimensionKey: '',
          remark: '',
        },
      },
    }
  }

  function isQuantityStep(step) {
    const config = step?.config || step?.stepConfig || {}
    return String(config.actionType || '').toUpperCase() === INTERNAL_ACTION.QUANTITY
  }

  function isInternalStepType(step, type) {
    return String(step?.stepType || '').toUpperCase() === type
  }

  function resolveActionScene(action = {}) {
    if (action.actionConfig?.triggerScene)
      return action.actionConfig.triggerScene
    const code = action.actionCode || action.key
    const callbackMap = collectCallbackActionMap(props.documentConfig)
    return callbackMap.get(code) || 'MANUAL'
  }

  function actionSceneLabel(action = {}) {
    const value = resolveActionScene(action)
    return sceneOptions.find(item => item.value === value)?.label || '业务自动化'
  }

  function collectCallbackActionMap(documentConfig = {}) {
    const result = new Map()
    const callbackActions = documentConfig.callbackActions
      || documentConfig.mainFlowSummary?.callbackActions
      || documentConfig.mainFlow?.callbackActions
      || documentConfig.options?.callbackActions
      || {}
    Object.entries(callbackActions).forEach(([key, value]) => {
      if (!value)
        return
      const normalized = String(key).toUpperCase()
      if (normalized.includes('APPROVED') || normalized === 'APPROVED')
        result.set(value, 'FLOW_APPROVED')
      if (normalized.includes('REJECTED') || normalized === 'REJECTED')
        result.set(value, 'FLOW_REJECTED')
    })
    if (callbackActions.approvedActionCode)
      result.set(callbackActions.approvedActionCode, 'FLOW_APPROVED')
    if (callbackActions.rejectedActionCode)
      result.set(callbackActions.rejectedActionCode, 'FLOW_REJECTED')
    return result
  }

  async function loadBusinessObjects() {
    try {
      const res = await businessObjectList({
        suiteCode: props.suiteCode || undefined,
      })
      businessObjects.value = Array.isArray(res.data) ? res.data : []
      await preloadRelationFields()
    }
    catch {
      businessObjects.value = []
    }
  }

  async function preloadRelationFields() {
    const objectCodes = Array.from(new Set(actionRelations.value
      .map(relation => relation.targetObjectCode)
      .filter(Boolean)))
    await Promise.all(objectCodes.map(objectCode => loadTargetFields(objectCode)))
  }

  async function loadTargetFields(objectCode) {
    const code = String(objectCode || '').trim()
    if (!code || targetFieldsMap.value[code] || targetFieldLoadingMap.value[code])
      return
    targetFieldLoadingMap.value = {
      ...targetFieldLoadingMap.value,
      [code]: true,
    }
    try {
      let targetObject = businessObjects.value.find(item => item.configKey === code || item.objectCode === code)
      if (!targetObject?.id) {
        const byConfig = await businessObjectList({ configKey: code })
        targetObject = (byConfig.data || [])[0]
      }
      if (!targetObject?.id) {
        const byObject = await businessObjectList({ objectCode: code })
        targetObject = (byObject.data || [])[0]
      }
      if (!targetObject?.id) {
        targetFieldsMap.value = {
          ...targetFieldsMap.value,
          [code]: [],
        }
        return
      }
      const res = await businessObjectDesigner(targetObject.id)
      const fields = res.data?.fields || res.data?.modelSchema?.fields || []
      targetFieldsMap.value = {
        ...targetFieldsMap.value,
        [code]: fields.map(toPageField),
      }
    }
    catch {
      targetFieldsMap.value = {
        ...targetFieldsMap.value,
        [code]: [],
      }
    }
    finally {
      targetFieldLoadingMap.value = {
        ...targetFieldLoadingMap.value,
        [code]: false,
      }
    }
  }

  function buildCollectionPathOptions(relations = []) {
    return relations
      .filter(relation => isDetailRelation(relation))
      .map((child) => {
        const key = child.collectionKey || child.key || child.modelCode || child.tableName || child.relationName
        const value = `record.children.${key}`
        const isKeyName = child.relationName && child.relationName === key
        return {
          label: (!isKeyName && child.relationName)
            || child.detailTabTitle
            || child.targetObjectName
            || child.modelName
            || child.label
            || '明细关系',
          value,
        }
      })
  }

  function collectionOptionsForStep(step = {}) {
    const options = [...collectionPathOptions.value]
    const current = String(step.config?.collectionPath || '').trim()
    if (current && !options.some(item => item.value === current)) {
      options.unshift({
        label: resolveCollectionPathLabel(current),
        value: current,
      })
    }
    return options
  }

  function resolveCollectionPathLabel(collectionPath = '') {
    const relation = relationByCollectionPath(collectionPath)
    if (relation)
      return relation.relationName || relation.detailTabTitle || relation.targetObjectName || relation.modelName || '明细关系'
    return '未识别明细关系（请在关系与级联中维护）'
  }

  function relationByCollectionPath(collectionPath = '') {
    const path = String(collectionPath || '')
    return actionRelations.value.find((child) => {
      const keys = collectionKeyCandidates(child)
      return keys.some(key => path.endsWith(String(key)))
    }) || null
  }

  function buildActionRelations(modelSchema = {}, relations = []) {
    const schemaChildren = collectSchemaChildren(modelSchema)
    const result = []
    const usedSchema = new Set()
    ;(Array.isArray(relations) ? relations : []).forEach((relation) => {
      const matchedIndex = schemaChildren.findIndex(child => isSameRelation(child, relation))
      const schemaChild = matchedIndex >= 0 ? schemaChildren[matchedIndex] : {}
      if (matchedIndex >= 0)
        usedSchema.add(matchedIndex)
      result.push(normalizeActionRelation({
        ...schemaChild,
        ...relation,
        fields: mergeRelationFields(schemaChild, relation),
      }))
    })
    schemaChildren.forEach((child, index) => {
      if (!usedSchema.has(index))
        result.push(normalizeActionRelation(child))
    })
    return result
  }

  function normalizeActionRelation(relation = {}) {
    const targetObjectCode = relation.targetObjectCode || relation.objectCode || relation.modelCode || ''
    const parsedConfig = parseRelationConfig(relation.relationConfig)
    const collectionKey = parsedConfig.relationKey
      || relation.key
      || relation.modelCode
      || relation.tableName
      || lowerSnake(targetObjectCode)
      || relation.relationName
    const isKeyName = relation.relationName && relation.relationName === collectionKey
    return {
      ...relation,
      targetObjectCode,
      collectionKey,
      relationType: relation.relationType || relation.type || 'DETAIL',
      relationName: (!isKeyName && relation.relationName)
        || relation.detailTabTitle
        || relation.targetObjectName
        || parsedConfig.detailTabTitle
        || relation.modelName
        || relation.label
        || relation.relationName
        || '',
      fields: relationFields({
        ...relation,
        targetObjectCode,
      }),
    }
  }

  function mergeRelationFields(schemaChild = {}, relation = {}) {
    return [
      ...normalizeFields(schemaChild.fields),
      ...normalizeFields(relation.fields),
    ]
  }

  function relationFields(relation = {}) {
    const fields = [
      ...normalizeFields(relation.fields),
      ...normalizeFields(targetFieldsMap.value[relation.targetObjectCode]),
    ]
    const seen = new Set()
    return fields.filter((field) => {
      const code = field.sourceField || field.field || field.fieldCode
      if (!code || seen.has(code) || isInactiveField(field))
        return false
      seen.add(code)
      return true
    })
  }

  function normalizeFields(fields = []) {
    return Array.isArray(fields) ? fields.map(toPageField) : []
  }

  function isSameRelation(left = {}, right = {}) {
    const leftCodes = collectionKeyCandidates(left)
    const rightCodes = collectionKeyCandidates(right)
    return leftCodes.some(code => rightCodes.includes(code))
  }

  function collectionKeyCandidates(relation = {}) {
    return [
      relation.collectionKey,
      relation.key,
      relation.modelCode,
      relation.tableName,
      relation.targetObjectCode,
      lowerSnake(relation.targetObjectCode),
      relation.relationName,
    ].filter(Boolean).map(String)
  }

  function isDetailRelation(relation = {}) {
    const type = String(relation.relationType || relation.type || '').toUpperCase()
    return !type || ['DETAIL', 'CHILD_LIST', 'ONE_TO_MANY'].includes(type)
  }

  function collectSchemaChildren(modelSchema = {}) {
    if (Array.isArray(modelSchema.children))
      return modelSchema.children
    if (Array.isArray(modelSchema.childrenConfig))
      return modelSchema.childrenConfig
    if (Array.isArray(modelSchema.relations))
      return modelSchema.relations
    return []
  }

  function lowerSnake(value = '') {
    return String(value || '')
      .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
      .replace(/\W+/g, '_')
      .replace(/_+/g, '_')
      .replace(/^_|_$/g, '')
      .toLowerCase()
  }

  __impl.addAutomationAction = addAutomationAction
  __impl.resolveExecutionMode = resolveExecutionMode
  __impl.updateExecutionMode = updateExecutionMode
  __impl.addInputSchemaField = addInputSchemaField
  __impl.patchInputSchemaField = patchInputSchemaField
  __impl.removeInputSchemaField = removeInputSchemaField
  __impl.inputOptionsText = inputOptionsText
  __impl.updateInputOptions = updateInputOptions
  __impl.addLocalStep = addLocalStep
  __impl.createLocalStep = createLocalStep
  __impl.updateLocalStepType = updateLocalStepType
  __impl.stepTypeNeedsRecordId = stepTypeNeedsRecordId
  __impl.mappingSourcePlaceholder = mappingSourcePlaceholder
  __impl.stepFieldMappings = stepFieldMappings
  __impl.addFieldMapping = addFieldMapping
  __impl.patchFieldMapping = patchFieldMapping
  __impl.removeFieldMapping = removeFieldMapping
  __impl.stepExpectedFieldMappings = stepExpectedFieldMappings
  __impl.addExpectedFieldMapping = addExpectedFieldMapping
  __impl.patchExpectedFieldMapping = patchExpectedFieldMapping
  __impl.removeExpectedFieldMapping = removeExpectedFieldMapping
  __impl.stepAdjustments = stepAdjustments
  __impl.addNumberAdjustment = addNumberAdjustment
  __impl.patchAdjustment = patchAdjustment
  __impl.removeNumberAdjustment = removeNumberAdjustment
  __impl.buildTargetConfigOptions = buildTargetConfigOptions
  __impl.targetFieldOptions = targetFieldOptions
  __impl.patchSelectedAction = patchSelectedAction
  __impl.patchActionConfig = patchActionConfig
  __impl.updateActionScene = updateActionScene
  __impl.updateManualActionPosition = updateManualActionPosition
  __impl.updateChildActionRelation = updateChildActionRelation
  __impl.addDetailQuantityFlow = addDetailQuantityFlow
  __impl.addCallApiStep = addCallApiStep
  __impl.updateCallApiStepConfig = updateCallApiStepConfig
  __impl.addQuantityStep = addQuantityStep
  __impl.patchStep = patchStep
  __impl.patchStepConfig = patchStepConfig
  __impl.updateStepCollection = updateStepCollection
  __impl.updateStepParam = updateStepParam
  __impl.updateFallbackFields = updateFallbackFields
  __impl.removeStep = removeStep
  __impl.removeAction = removeAction
  __impl.applyActionConfigText = applyActionConfigText
  __impl.emitActions = emitActions
  __impl.containsInternalStartFlow = containsInternalStartFlow
  __impl.isAutomationAction = isAutomationAction
  __impl.hasConfiguredSteps = hasConfiguredSteps
  __impl.normalizeActionType = normalizeActionType
  __impl.flattenRootSteps = flattenRootSteps
  __impl.childBusinessSteps = childBusinessSteps
  __impl.flattenAllSteps = flattenAllSteps
  __impl.buildStepVM = buildStepVM
  __impl.createQuantityStep = createQuantityStep
  __impl.isQuantityStep = isQuantityStep
  __impl.isInternalStepType = isInternalStepType
  __impl.resolveActionScene = resolveActionScene
  __impl.actionSceneLabel = actionSceneLabel
  __impl.collectCallbackActionMap = collectCallbackActionMap
  __impl.loadBusinessObjects = loadBusinessObjects
  __impl.preloadRelationFields = preloadRelationFields
  __impl.loadTargetFields = loadTargetFields
  __impl.buildCollectionPathOptions = buildCollectionPathOptions
  __impl.collectionOptionsForStep = collectionOptionsForStep
  __impl.resolveCollectionPathLabel = resolveCollectionPathLabel
  __impl.relationByCollectionPath = relationByCollectionPath
  __impl.buildActionRelations = buildActionRelations
  __impl.normalizeActionRelation = normalizeActionRelation
  __impl.mergeRelationFields = mergeRelationFields
  __impl.relationFields = relationFields
  __impl.normalizeFields = normalizeFields
  __impl.isSameRelation = isSameRelation
  __impl.collectionKeyCandidates = collectionKeyCandidates
  __impl.isDetailRelation = isDetailRelation
  __impl.collectSchemaChildren = collectSchemaChildren
  __impl.lowerSnake = lowerSnake

  return {
    props, emit, __impl, mut, actionSceneLabel, addAutomationAction, addCallApiStep, addDetailQuantityFlow,
    addExpectedFieldMapping, addFieldMapping, addInputSchemaField, addLocalStep, addNumberAdjustment, addQuantityStep,
    applyActionConfigText, buildActionRelations, buildCollectionPathOptions, buildStepVM, buildTargetConfigOptions,
    childBusinessSteps, collectCallbackActionMap, collectSchemaChildren, collectionKeyCandidates,
    collectionOptionsForStep, containsInternalStartFlow, createLocalStep, createQuantityStep, emitActions,
    flattenAllSteps, flattenRootSteps, hasConfiguredSteps, inputOptionsText, isAutomationAction, isDetailRelation,
    isInternalStepType, isQuantityStep, isSameRelation, loadBusinessObjects, loadTargetFields, lowerSnake,
    mappingSourcePlaceholder, mergeRelationFields, normalizeActionRelation, normalizeActionType, normalizeFields,
    patchActionConfig, patchAdjustment, patchExpectedFieldMapping, patchFieldMapping, patchInputSchemaField,
    patchSelectedAction, patchStep, patchStepConfig, preloadRelationFields, relationByCollectionPath, relationFields,
    removeAction, removeExpectedFieldMapping, removeFieldMapping, removeInputSchemaField, removeNumberAdjustment,
    removeStep, resolveActionScene, resolveCollectionPathLabel, resolveExecutionMode, stepAdjustments,
    stepExpectedFieldMappings, stepFieldMappings, stepTypeNeedsRecordId, targetFieldOptions, updateActionScene,
    updateCallApiStepConfig, updateChildActionRelation, updateExecutionMode, updateFallbackFields, updateInputOptions,
    updateLocalStepType, updateManualActionPosition, updateStepCollection, updateStepParam, INTERNAL_STEP, INTERNAL_ACTION,
    EXECUTION_MODE, LOCAL_STEP_TYPES, selectedActionIndex, actionConfigText, jsonError, businessObjects,
    targetFieldsMap, targetFieldLoadingMap, sceneOptions, manualActionPositionOptions, successBehaviorOptions,
    permissionStrategyOptions, executionModeOptions, inputTypeOptions, localStepTypeOptions, localStepMenuOptions,
    sourceTypeOptions, adjustmentOperatorOptions, quantityOperationOptions, paramLabels, actionList,
    approvalEntryActions, automationActions, pageInteractionActions, pageInteractionActionNames, selectedAction,
    rootSteps, resolvedExecutionMode, isLocalTransaction, localStepViews, nonLocalStepCount, inputSchemaRows,
    callApiRecordFieldOptions, callApiFormFieldOptions, targetConfigOptions, actionRelations, collectionPathOptions,
    childRelationOptions, selectedManualActionPosition, BusinessQuantityStepCard,
  }
}
