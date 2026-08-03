<template>
  <section class="automation-designer">
    <header class="designer-head">
      <div>
        <h2>自动化动作</h2>
        <p>配置审批结果、按钮或触发器之后自动执行的业务处理。发起审批和审批办理不在这里配置。</p>
      </div>
      <NButton size="small" type="primary" secondary @click="addAutomationAction">
        新增自动化
      </NButton>
    </header>

    <div class="boundary-strip">
      <div class="boundary-item">
        <strong>发起审批</strong>
        <span>在“单据流程”和列表按钮里配置</span>
      </div>
      <div class="boundary-item">
        <strong>同意 / 驳回</strong>
        <span>在流程设计器节点中配置</span>
      </div>
      <div class="boundary-item active">
        <strong>审批后业务处理</strong>
        <span>在本页配置字段映射和执行动作</span>
      </div>
    </div>

    <div v-if="approvalEntryActions.length || pageInteractionActions.length" class="context-notices">
      <n-alert v-if="approvalEntryActions.length" type="info" :bordered="false" class="approval-entry-note">
        已识别到 {{ approvalEntryActions.length }} 个发起审批入口。这类入口由“单据流程”和列表按钮维护，本页不展示底层流程启动参数。
      </n-alert>

      <n-alert v-if="pageInteractionActions.length" type="info" :bordered="false" class="approval-entry-note">
        已隐藏 {{ pageInteractionActions.length }} 个页面操作（{{ pageInteractionActionNames }}）。它们只负责打开新增/编辑页面或执行前端交互，
        不是服务端自动化步骤，也不能直接作为开放能力执行；页面上的原有按钮不会受影响。
      </n-alert>
    </div>

    <n-empty v-if="!automationActions.length" description="当前还没有业务自动化动作" class="empty-state" />

    <div v-else class="automation-workbench">
      <aside class="automation-list">
        <div class="pane-title">
          <strong>业务自动化</strong>
          <span>{{ automationActions.length }}</span>
        </div>
        <button
          v-for="item in automationActions"
          :key="item.originalIndex"
          type="button"
          class="automation-list-item"
          :class="{ active: item.originalIndex === selectedActionIndex }"
          @click="selectedActionIndex = item.originalIndex"
        >
          <strong>{{ item.action.actionName || '未命名自动化' }}</strong>
          <span>{{ actionSceneLabel(item.action) }}</span>
        </button>
      </aside>

      <main v-if="selectedAction" class="automation-main">
        <section class="panel-section action-summary">
          <div class="section-title">
            <h3>自动化信息</h3>
            <n-switch
              :value="selectedAction.status !== 0"
              @update:value="patchSelectedAction({ status: $event ? 1 : 0 })"
            />
          </div>
          <NGrid :cols="3" :x-gap="12" :y-gap="8" responsive="screen">
            <NFormItemGi label="自动化名称">
              <NInput
                :value="selectedAction.actionName || ''"
                placeholder="例如：审批通过后更新库存"
                @update:value="patchSelectedAction({ actionName: $event })"
              />
            </NFormItemGi>
            <NFormItemGi label="执行场景">
              <NSelect
                :value="resolveActionScene(selectedAction)"
                :options="sceneOptions"
                @update:value="updateActionScene($event)"
              />
            </NFormItemGi>
            <NFormItemGi label="成功后">
              <NSelect
                :value="selectedAction.actionConfig?.successBehavior || 'refreshList'"
                :options="successBehaviorOptions"
                @update:value="patchActionConfig({ successBehavior: $event })"
              />
            </NFormItemGi>
          </NGrid>
        </section>

        <section class="panel-section">
          <div class="section-title">
            <h3>业务处理流程</h3>
            <NButton size="tiny" secondary @click="addDetailQuantityFlow">
              添加明细数量处理
            </NButton>
          </div>

          <n-empty v-if="!rootSteps.length" description="还没有业务处理步骤" size="small" />

          <div v-else class="flow-stack">
            <article v-for="rootStep in rootSteps" :key="rootStep.key" class="flow-card">
              <template v-if="isInternalStepType(rootStep.raw, INTERNAL_STEP.FOREACH)">
                <div class="flow-card-head">
                  <span class="step-index">{{ rootStep.index + 1 }}</span>
                  <div>
                    <strong>逐行处理明细</strong>
                    <em>对选中的子表明细逐行执行业务动作</em>
                  </div>
                  <NButton size="tiny" quaternary type="error" @click="removeStep(rootStep)">
                    删除
                  </NButton>
                </div>
                <NGrid :cols="1" :x-gap="12" :y-gap="8" responsive="screen">
                  <NFormItemGi label="处理明细">
                    <NSelect
                      filterable
                      :options="collectionOptionsForStep(rootStep)"
                      :value="rootStep.config.collectionPath || ''"
                      placeholder="选择关系与级联中配置的明细"
                      @update:value="updateStepCollection(rootStep, $event)"
                    />
                  </NFormItemGi>
                </NGrid>
                <n-alert
                  v-if="!collectionPathOptions.length"
                  type="warning"
                  :bordered="false"
                  class="relation-warning"
                >
                  还没有可用于自动化的明细关系。请先到“关系与级联”配置主表和明细表的关系，自动化动作会直接复用那里的关系和字段。
                </n-alert>

                <div class="nested-actions">
                  <div class="nested-title">
                    <strong>每行执行</strong>
                    <NButton size="tiny" secondary @click="addQuantityStep(rootStep)">
                      添加数量处理
                    </NButton>
                  </div>
                  <BusinessQuantityStepCard
                    v-for="child in childBusinessSteps(rootStep)"
                    :key="child.key"
                    :step="child"
                    :field-options="fieldPathOptions(child)"
                    @patch-step="patchStep(child, $event)"
                    @patch-config="patchStepConfig(child, $event)"
                    @patch-param="updateStepParam(child, $event.key, $event.value)"
                    @patch-fallback="updateFallbackFields(child, $event.key, $event.value)"
                    @remove="removeStep(child)"
                  />
                </div>
              </template>

              <BusinessQuantityStepCard
                v-else-if="isQuantityStep(rootStep.raw)"
                :step="rootStep"
                :field-options="fieldPathOptions(rootStep)"
                @patch-step="patchStep(rootStep, $event)"
                @patch-config="patchStepConfig(rootStep, $event)"
                @patch-param="updateStepParam(rootStep, $event.key, $event.value)"
                @patch-fallback="updateFallbackFields(rootStep, $event.key, $event.value)"
                @remove="removeStep(rootStep)"
              />

              <div v-else class="unsupported-step">
                <div>
                  <strong>{{ rootStep.raw.stepName || '高级步骤' }}</strong>
                  <span>该步骤暂未提供可视化表单，可在高级 JSON 中维护。</span>
                </div>
                <NButton size="tiny" quaternary type="error" @click="removeStep(rootStep)">
                  删除
                </NButton>
              </div>
            </article>
          </div>
        </section>

        <n-collapse class="advanced-json">
          <n-collapse-item title="高级 JSON（开发者兜底）" name="json">
            <NInput
              v-model:value="actionConfigText"
              type="textarea"
              :autosize="{ minRows: 8, maxRows: 18 }"
              placeholder="动作配置 JSON"
              @blur="applyActionConfigText"
            />
            <n-alert v-if="jsonError" type="error" :bordered="false" class="json-error">
              {{ jsonError }}
            </n-alert>
          </n-collapse-item>
        </n-collapse>
      </main>
    </div>
  </section>
</template>

<script setup>
import { NButton, NFormItemGi, NGrid, NInput, NSelect } from 'naive-ui'
import { computed, defineComponent, h, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList } from '@/api/business-app'

const props = defineProps({
  actions: {
    type: Array,
    default: () => [],
  },
  fields: {
    type: Array,
    default: () => [],
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  relations: {
    type: Array,
    default: () => [],
  },
  suiteCode: {
    type: String,
    default: '',
  },
  documentConfig: {
    type: Object,
    default: () => ({}),
  },
})
const emit = defineEmits(['update:actions', 'dirtyChange'])
const INTERNAL_STEP = {
  FOREACH: 'FOREACH',
  DOMAIN_ACTION: 'DOMAIN_ACTION',
  START_FLOW: 'START_FLOW',
}
const INTERNAL_ACTION = {
  QUANTITY: 'QUANTITY',
}

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
const successBehaviorOptions = [
  { label: '刷新列表', value: 'refreshList' },
  { label: '无操作', value: 'none' },
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
const actionRelations = computed(() => buildActionRelations(props.modelSchema, props.relations))
const collectionPathOptions = computed(() => buildCollectionPathOptions(actionRelations.value))

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
    actionConfig: {
      triggerScene: 'FLOW_APPROVED',
      successBehavior: 'refreshList',
      steps: [],
    },
  })
  emitActions(actions)
  selectedActionIndex.value = actions.length - 1
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
  patchActionConfig({ triggerScene: scene })
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
    let targetObject = businessObjects.value.find(item => item.objectCode === code)
    if (!targetObject?.id) {
      const res = await businessObjectList({ objectCode: code })
      targetObject = (res.data || [])[0]
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
      return {
        label: child.relationName || child.detailTabTitle || child.modelName || child.label || '明细关系',
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
    return relation.relationName || relation.detailTabTitle || relation.modelName || '明细关系'
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
  const collectionKey = relation.key
    || relation.modelCode
    || relation.tableName
    || lowerSnake(targetObjectCode)
    || relation.relationName
  return {
    ...relation,
    targetObjectCode,
    collectionKey,
    relationType: relation.relationType || relation.type || 'DETAIL',
    relationName: relation.relationName || relation.detailTabTitle || relation.modelName || relation.label || '',
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

function toPageField(field = {}) {
  return {
    ...field,
    field: field.field || field.fieldCode || field.sourceField,
    label: field.label || field.fieldName || field.name || field.fieldCode || field.sourceField,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function businessFieldLabel(field = {}) {
  const fieldName = field.field || field.fieldCode || field.sourceField || ''
  const systemLabels = {
    id: '记录ID',
    createBy: '创建人',
    createTime: '创建时间',
    updateBy: '修改人',
    updateTime: '修改时间',
    createDept: '创建部门',
    tenantId: '租户',
  }
  return systemLabels[fieldName] || field.label || field.fieldName || field.name || '未命名字段'
}

function fieldPathOptions(step = {}) {
  const options = []
  const seen = new Set()
  const add = (value, label) => {
    const text = String(value || '').trim()
    if (!text || seen.has(text))
      return
    seen.add(text)
    options.push({ label: label || '未命名字段', value: text })
  }
  collectMainFields(props.fields, props.modelSchema).forEach((sourceField) => {
    const field = toPageField(sourceField)
    const fieldCode = field.sourceField || field.field || field.fieldCode
    if (!fieldCode)
      return
    add(`record.main.${fieldCode}`, fieldDisplayLabel(field, '单据字段'))
  })
  const aliases = step.aliases?.length ? step.aliases : [{ alias: 'item', collectionPath: '' }]
  aliases.forEach((aliasInfo) => {
    const relation = relationByCollectionPath(aliasInfo.collectionPath) || actionRelations.value[0]
    const detailLabel = detailDisplayLabel(aliasInfo.collectionPath)
    const fields = relation?.fields || []
    fields.forEach((field) => {
      const fieldCode = field.sourceField || field.field || field.fieldCode
      if (!fieldCode)
        return
      add(`${aliasInfo.alias}.${fieldCode}`, fieldDisplayLabel(field, detailLabel))
    })
    add(`${aliasInfo.alias}.id`, `${detailLabel} · ID`)
  })
  return options
}

function fieldDisplayLabel(field = {}, scopeLabel = '') {
  const label = businessFieldLabel(field)
  return scopeLabel ? `${scopeLabel} · ${label}` : label
}

function collectMainFields(fields = [], modelSchema = {}) {
  if (Array.isArray(fields) && fields.length)
    return fields
  return Array.isArray(modelSchema.fields) ? modelSchema.fields : []
}

function detailDisplayLabel(collectionPath = '') {
  const relation = relationByCollectionPath(collectionPath) || actionRelations.value[0]
  return relation?.relationName || relation?.detailTabTitle || relation?.modelName || relation?.label || '明细字段'
}

function mergeSelectedFieldOptions(options = [], values = []) {
  const result = Array.isArray(options) ? [...options] : []
  const seen = new Set(result.map(item => item.value))
  normalizeStringList(values).forEach((value) => {
    if (seen.has(value))
      return
    result.push({
      label: resolvePathDisplayLabel(value, result),
      value,
    })
    seen.add(value)
  })
  return result
}

function resolvePathDisplayLabel(value, options = []) {
  const matched = options.find(item => item.value === value)
  if (matched?.label)
    return matched.label
  const fieldCode = String(value || '').split('.').pop()
  const field = findFieldByCode(fieldCode)
  if (field)
    return fieldDisplayLabel(field, String(value || '').startsWith('record.') ? '单据字段' : '明细字段')
  return '未识别字段（请在关系与级联中维护）'
}

function findFieldByCode(fieldCode) {
  if (!fieldCode)
    return null
  const allFields = [
    ...collectMainFields(props.fields, props.modelSchema).map(toPageField),
    ...actionRelations.value.flatMap(relation => relation.fields || []),
  ].map(toPageField)
  return allFields.find((field) => {
    const codes = [field.sourceField, field.field, field.fieldCode, field.columnName].filter(Boolean)
    return codes.some(code => String(code) === String(fieldCode))
  }) || null
}

function ensureActionConfig(action) {
  if (!action.actionConfig || typeof action.actionConfig !== 'object' || Array.isArray(action.actionConfig))
    action.actionConfig = {}
  return action.actionConfig
}

function ensureStepConfig(step) {
  if (!step.stepConfig || typeof step.stepConfig !== 'object' || Array.isArray(step.stepConfig))
    step.stepConfig = {}
  return step.stepConfig
}

function ensureParams(config) {
  if (!config.params || typeof config.params !== 'object' || Array.isArray(config.params))
    config.params = {}
  return config.params
}

function resolveStep(actions, step) {
  const action = actions[selectedActionIndex.value]
  if (!action?.actionConfig)
    return null
  return getPathValue(action.actionConfig, step.path)
}

function getPathValue(root, path = []) {
  let cursor = root
  for (const key of path) {
    if (cursor == null)
      return null
    cursor = cursor[key]
  }
  return cursor
}

function wrapExpression(path) {
  const text = String(path || '').trim()
  return text ? `\${${text}}` : ''
}

function unwrapExpression(value) {
  const text = String(value || '').trim()
  const match = text.match(/^\$\{([^}]+)\}$/)
  return match ? match[1] : text
}

function stringValue(value) {
  if (value == null)
    return ''
  if (typeof value === 'object')
    return JSON.stringify(value)
  return String(value)
}

function normalizeStringList(value) {
  const list = Array.isArray(value) ? value : value ? [value] : []
  return Array.from(new Set(list.map(item => String(item || '').trim()).filter(Boolean)))
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? []))
}

function stringifyJson(value) {
  try {
    return JSON.stringify(value || {}, null, 2)
  }
  catch {
    return '{}'
  }
}
</script>

<style scoped>
.automation-designer {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  gap: 12px;
  min-height: 100%;
  padding: 14px;
  background: #f7f8fa;
}

.designer-head,
.boundary-strip,
.panel-section,
.automation-list,
.advanced-json {
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fff;
}

.designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
}

.designer-head h2,
.section-title h3 {
  margin: 0;
  color: #18181b;
  font-size: 15px;
  font-weight: 700;
}

.designer-head p {
  margin: 3px 0 0;
  color: #71717a;
  font-size: 12px;
}

.boundary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
}

.boundary-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  background: #fafafa;
}

.boundary-item.active {
  background: #eef3ff;
}

.boundary-item strong {
  color: #27272a;
  font-size: 13px;
}

.boundary-item span,
.pane-title span,
.automation-list-item span,
.flow-card-head em,
.quantity-card-head span,
.unsupported-step span {
  color: #71717a;
  font-size: 12px;
}

.approval-entry-note {
  font-size: 12px;
}

.context-notices {
  display: grid;
  gap: 8px;
}

.relation-warning {
  margin-top: 10px;
  font-size: 12px;
}

.empty-state {
  align-self: center;
}

.automation-workbench {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.automation-list {
  min-height: 0;
  overflow: auto;
  padding: 8px;
}

.pane-title,
.section-title,
.flow-card-head,
.nested-title,
.quantity-card-head,
.unsupported-step {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.pane-title {
  padding: 4px 4px 8px;
  color: #52525b;
  font-size: 12px;
}

.automation-list-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  text-align: left;
}

.automation-list-item:hover,
.automation-list-item.active {
  border-color: #bfd0ff;
  background: #eef3ff;
}

.automation-list-item strong,
.quantity-card-head strong,
.unsupported-step strong {
  color: #27272a;
  font-size: 13px;
}

.automation-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.panel-section {
  padding: 12px;
}

.action-summary {
  padding-bottom: 4px;
}

.flow-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.flow-card,
.quantity-card,
.unsupported-step {
  padding: 12px;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fafafa;
}

.flow-card-head {
  margin-bottom: 12px;
}

.flow-card-head > div,
.quantity-card-head > div,
.unsupported-step > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.step-index {
  display: grid;
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  background: #2944cc;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.nested-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e4e7;
}

.nested-title {
  margin-bottom: 10px;
}

.quantity-card + .quantity-card {
  margin-top: 10px;
}

.quantity-card {
  background: #fff;
}

.quantity-card-head {
  margin-bottom: 10px;
}

.advanced-json {
  overflow: hidden;
}

.json-error {
  margin-top: 8px;
}

@media (max-width: 1180px) {
  .boundary-strip,
  .automation-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
