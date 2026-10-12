<template>
  <div class="business-flow-binding-panel" :class="{ compact, 'compact-expanded': compactExpanded }">
    <n-alert type="info" :bordered="false" class="legacy-entry-alert">
      流程绑定配置已整合到业务流程画布，建议在新入口维护。此处仅支持查看历史配置。
      <template #action>
        <n-button data-open-process-workspace text type="primary" @click="openProcessWorkspace">
          前往应用工作台业务流程
        </n-button>
      </template>
    </n-alert>

    <div class="flow-head">
      <div>
        <h3>{{ compact ? '流程绑定' : '流程与自动化' }}</h3>
        <p>{{ compact ? '选择流程模型和启动方式，节点配置会在下方自动生成。' : '绑定默认流程并进入流程设计器配置节点；业务字段由表单设计自动提供给流程使用。' }}</p>
      </div>
      <n-space size="small">
        <n-button v-if="compact" size="small" quaternary @click="compactExpanded = !compactExpanded">
          {{ compactExpanded ? '收起完整设置' : '完整绑定设置' }}
        </n-button>
        <n-button size="small" secondary :loading="loading" @click="loadBinding">
          刷新
        </n-button>
        <n-button v-if="!legacyReadOnly" size="small" type="primary" :loading="saving" @click="saveConfig">
          保存流程
        </n-button>
      </n-space>
    </div>

    <div class="flow-body" :class="{ 'is-read-only': legacyReadOnly }" aria-readonly="true">
      <main class="flow-main">
        <n-spin :show="loading">
          <section class="flow-card">
            <div class="flow-card-head">
              <div>
                <h4><span class="flow-step-index">1</span>选择流程模型</h4>
                <p>手动按钮和触发器可以复用同一套流程绑定。</p>
              </div>
              <n-tag :type="form.flowModelKey ? 'success' : 'warning'" :bordered="false">
                {{ form.flowModelKey ? '已绑定' : '待绑定' }}
              </n-tag>
            </div>
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="14" :y-gap="4" responsive="screen">
                <n-form-item-gi label="流程模型">
                  <n-select
                    v-model:value="form.flowModelKey"
                    :options="flowModelOptions"
                    :loading="flowModelsLoading"
                    clearable
                    filterable
                    placeholder="选择已发布流程模型"
                    @update:value="handleFlowChange"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="谁来发起">
                  <n-select
                    v-model:value="form.startMode"
                    :options="startModeOptions"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi v-if="!compact || compactExpanded" :span="2" label="流程标题模板">
                  <TemplateVariableEditor
                    v-model="form.titleTemplate"
                    :fields="fieldOptions"
                    :variables="variableOptions"
                    placeholder="例如：${opportunityName}-流程"
                    @update:model-value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </n-form>
          </section>

          <section class="flow-card flow-field-binding-card">
            <div class="flow-card-head">
              <div>
                <h4>业务字段绑定</h4>
                <p>业务字段会作为流程变量参与标题模板、条件分支和节点表达式，默认按字段编码同名传递。</p>
              </div>
              <n-tag :type="effectiveVariableMappingCount ? 'success' : 'warning'" :bordered="false">
                {{ effectiveVariableMappingCount ? `${effectiveVariableMappingCount} 个绑定` : '未绑定' }}
              </n-tag>
            </div>
            <div class="field-binding-toolbar">
              <span>选择流程模型后可按流程变量调整绑定；未配置时系统会自动按字段编码补齐。</span>
              <n-space size="small">
                <n-button size="tiny" secondary @click="resetVariableMappings">
                  自动补齐
                </n-button>
                <n-button size="tiny" secondary @click="addVariableMapping">
                  添加绑定
                </n-button>
              </n-space>
            </div>
            <div v-if="form.variableMapping.length" class="variable-mapping-list">
              <div
                v-for="(mapping, index) in form.variableMapping"
                :key="mapping.clientKey"
                class="variable-mapping-row"
              >
                <n-select
                  v-model:value="mapping.formField"
                  :options="fieldOptions"
                  clearable
                  filterable
                  placeholder="业务字段"
                  @update:value="value => updateVariableMappingField(mapping, value)"
                />
                <span>传给</span>
                <n-select
                  v-model:value="mapping.flowVariable"
                  :options="flowVariableSelectOptions"
                  clearable
                  filterable
                  tag
                  placeholder="流程变量"
                  @update:value="markDirty"
                />
                <n-button quaternary circle size="small" @click="removeVariableMapping(index)">
                  ×
                </n-button>
              </div>
            </div>
            <n-empty v-else size="small" description="暂无字段绑定，点击自动补齐生成默认绑定" />
          </section>

          <n-collapse class="advanced-flow-collapse" arrow-placement="right">
            <n-collapse-item name="business-binding">
              <template #header>
                <span class="advanced-flow-title">
                  高级配置：业务记录绑定
                  <n-tag size="small" :type="businessBindingTagType" :bordered="false">
                    {{ businessBindingModeLabel }}
                  </n-tag>
                </span>
              </template>

              <template v-if="!codeApp">
                <n-form label-placement="top" size="small" :show-feedback="false">
                  <n-grid :cols="3" :x-gap="14" :y-gap="4" responsive="screen">
                    <n-form-item-gi label="接入方式">
                      <n-select
                        v-model:value="form.businessBinding.mode"
                        :options="businessBindingModeOptions"
                        @update:value="handleBusinessBindingModeChange"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="业务表">
                      <n-input
                        v-model:value="form.businessBinding.tableName"
                        :disabled="businessBindingReadonly"
                        placeholder="发布后自动填充"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="主键字段">
                      <n-input
                        v-model:value="form.businessBinding.primaryKeyField"
                        :disabled="businessBindingReadonly"
                        placeholder="id"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="租户字段">
                      <n-input
                        v-model:value="form.businessBinding.tenantField"
                        :disabled="businessBindingReadonly"
                        placeholder="tenant_id"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="流程状态字段">
                      <n-select
                        v-model:value="form.businessBinding.statusField"
                        :options="fieldOptions"
                        :disabled="businessBindingAdapterMode"
                        clearable
                        filterable
                        tag
                        placeholder="选择或输入状态字段"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="标题字段">
                      <n-select
                        v-model:value="form.businessBinding.titleField"
                        :options="fieldOptions"
                        :disabled="businessBindingAdapterMode"
                        clearable
                        filterable
                        tag
                        placeholder="选择或输入标题字段"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                  </n-grid>
                </n-form>
                <n-alert
                  v-if="businessBindingMessage"
                  :type="businessBindingAdapterMode ? 'info' : 'warning'"
                  :bordered="false"
                  class="business-binding-alert"
                >
                  {{ businessBindingMessage }}
                </n-alert>
              </template>

              <div v-else class="adapter-facts">
                <div>
                  <span>业务编码</span>
                  <strong>{{ objectCode }}</strong>
                </div>
                <div>
                  <span>主键字段</span>
                  <strong>{{ form.businessBinding.primaryKeyField || 'id' }}</strong>
                </div>
                <div>
                  <span>状态回写</span>
                  <strong>业务代码处理</strong>
                </div>
              </div>
            </n-collapse-item>
          </n-collapse>

          <section class="flow-card flow-step-card">
            <div class="flow-card-head">
              <div>
                <h4><span class="flow-step-index">2</span>流程节点一览</h4>
                <p>节点字段权限在真实流程设计器中维护，这里只提供状态预览和快捷入口。</p>
              </div>
              <n-tag :type="userTasks.length ? 'info' : 'default'" :bordered="false">
                {{ userTasks.length ? `${userTasks.length} 个审批节点` : '未读取节点' }}
              </n-tag>
            </div>

            <n-spin :show="variablesLoading || formAssetsLoading">
              <div v-if="userTasks.length" class="flow-node-list">
                <button
                  v-for="task in userTasks"
                  :key="task.taskDefKey"
                  type="button"
                  class="flow-node-row"
                  :disabled="!selectedFlowModelId"
                  @click="openFlowDesigner"
                >
                  <span>
                    <strong>{{ task.taskName || task.taskDefKey }}</strong>
                    <small>{{ task.taskDefKey }}</small>
                  </span>
                  <span>{{ taskAssigneeSummary(task) }}</span>
                  <n-tag size="small" :type="taskPermissionType(task)" :bordered="false">
                    {{ taskPermissionSummary(task) }}
                  </n-tag>
                </button>
              </div>
              <n-empty v-else size="small" description="选择流程模型后自动读取审批节点" />

              <div class="flow-node-footer">
                <span>{{ formAssets.length }} 个表单资产 · {{ fieldOptions.length }} 个业务字段</span>
                <n-button
                  type="primary"
                  secondary
                  size="small"
                  :disabled="!selectedFlowModelId"
                  @click.stop="openFlowDesigner"
                >
                  打开流程设计器
                </n-button>
              </div>
            </n-spin>

            <n-alert v-if="formAssetWarnings.length" type="warning" :bordered="false" class="node-form-warning">
              {{ formAssetWarnings.join('；') }}
            </n-alert>
          </section>

          <section class="flow-card flow-step-card">
            <div class="flow-card-head">
              <div>
                <h4><span class="flow-step-index">3</span>审批结果动作</h4>
                <p>流程结束后按结果执行对象动作，动作步骤、事务和日志复用通用动作引擎。</p>
              </div>
              <n-tag :type="callbackActionCount ? 'success' : 'default'" :bordered="false">
                {{ callbackActionCount ? `${callbackActionCount} 个动作` : '未配置' }}
              </n-tag>
            </div>
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="3" :x-gap="14" :y-gap="4" responsive="screen">
                <n-form-item-gi
                  v-for="item in callbackResultOptions"
                  :key="item.value"
                  :label="item.label"
                >
                  <n-select
                    v-model:value="form.options.callbackActions[item.value]"
                    :options="callbackActionOptions"
                    :loading="actionLoading"
                    clearable
                    filterable
                    placeholder="选择对象动作"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </n-form>
          </section>
        </n-spin>
      </main>
    </div>

    <n-modal
      v-model:show="flowDesignerVisible"
      class="flow-designer-modal"
      :mask-closable="false"
      :auto-focus="false"
    >
      <section class="flow-designer-modal-shell">
        <FlowDesignPage
          v-if="flowDesignerVisible"
          embedded
          class="embedded-flow-design-page"
          :model-id="selectedFlowModelId"
          :business-object-code="objectCode"
          :business-object-name="objectName || objectCode"
          :application-id="applicationId"
          :code-app="codeApp"
          @close="handleFlowDesignerClose"
          @saved="handleFlowDesignerSaved"
          @deployed="handleFlowDesignerSaved"
        />
      </section>
    </n-modal>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessFlowBinding, businessFlowFormAssets, businessFlowVariables, businessObjectActions, saveBusinessFlowBinding } from '@/api/business-app'
import flowApi from '@/api/flow'
import TemplateVariableEditor from './TemplateVariableEditor.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  objectCode: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectName: {
    type: String,
    default: '',
  },
  initialBinding: {
    type: Object,
    default: null,
  },
  codeApp: {
    type: Boolean,
    default: false,
  },
  compact: {
    type: Boolean,
    default: false,
  },
  applicationCode: {
    type: String,
    default: '',
  },
  applicationId: {
    type: [String, Number],
    default: '',
  },
})

const emit = defineEmits(['dirtyChange', 'saved', 'loaded', 'contextChange'])

const FlowDesignPage = defineAsyncComponent(() => import('@/views/flow/design.vue'))

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const flowModelsLoading = ref(false)
const variablesLoading = ref(false)
const formAssetsLoading = ref(false)
const actionLoading = ref(false)
const flowModelOptions = ref([])
const variableOptions = ref([])
const userTasks = ref([])
const formAssets = ref([])
const flowFieldCandidates = ref([])
const formAssetWarnings = ref([])
const actionOptions = ref([])
const flowDesignerVisible = ref(false)
const compactExpanded = ref(false)
const form = reactive(createDefaultBinding())
const legacyReadOnly = true

const startModeOptions = [
  { label: '用户点击按钮', value: 'MANUAL' },
  { label: '触发器自动发起', value: 'TRIGGER' },
  { label: '按钮和触发器都可以', value: 'BOTH' },
]
const businessBindingModeOptions = computed(() => {
  if (props.codeApp) {
    return [
      { label: '代码适配器', value: 'ADAPTER' },
      { label: '简单业务表', value: 'BUSINESS_TABLE' },
    ]
  }
  return [
    { label: '低代码对象', value: 'LOWCODE_OBJECT' },
    { label: '简单业务表', value: 'BUSINESS_TABLE' },
    { label: '代码适配器', value: 'ADAPTER' },
  ]
})

const fieldOptions = computed(() => {
  const options = []
  const used = new Set()
  const append = (field) => {
    const code = fieldCode(field)
    if (!code || used.has(code) || isInactiveField(field)) {
      return
    }
    used.add(code)
    const label = field.fieldName || field.fieldLabel || field.label || code
    options.push({
      label: `${label}（${code}）`,
      value: code,
    })
  }
  props.fields.forEach(append)
  flowFieldCandidates.value.forEach(append)
  return options
})
const flowVariableSelectOptions = computed(() => {
  const options = []
  const used = new Set()
  const append = (label, value) => {
    const code = normalizeText(value)
    if (!code || used.has(code))
      return
    used.add(code)
    options.push({
      label: label || code,
      value: code,
    })
  }
  variableOptions.value.forEach(option => append(option.label, option.value))
  fieldOptions.value.forEach(option => append(`${option.value}（同名业务字段）`, option.value))
  form.variableMapping.forEach(mapping => append(mapping.flowVariable, mapping.flowVariable))
  return options
})
const selectedFlowModel = computed(() => flowModelOptions.value.find(item => item.value === form.flowModelKey) || null)
const selectedFlowModelId = computed(() => selectedFlowModel.value?.id || '')
const normalizedBusinessBindingMode = computed(() => normalizeBusinessBindingMode(form.businessBinding.mode))
const businessBindingAdapterMode = computed(() => normalizedBusinessBindingMode.value === 'ADAPTER')
const businessBindingReadonly = computed(() => normalizedBusinessBindingMode.value === 'LOWCODE_OBJECT' || businessBindingAdapterMode.value)
const businessBindingModeLabel = computed(() => businessBindingModeOptions.value.find(item => item.value === normalizedBusinessBindingMode.value)?.label || (props.codeApp ? '代码适配器' : '低代码对象'))
const businessBindingTagType = computed(() => {
  if (businessBindingAdapterMode.value)
    return 'info'
  if (form.businessBinding.statusField)
    return 'success'
  return 'warning'
})
const callbackResultOptions = [
  { label: '审批通过后', value: 'APPROVED' },
  { label: '审批驳回后', value: 'REJECTED' },
  { label: '流程取消后', value: 'CANCELED' },
]
const callbackActionOptions = computed(() => actionOptions.value)
const callbackActionCount = computed(() => Object.values(form.options?.callbackActions || {}).filter(Boolean).length)
const effectiveVariableMappingCount = computed(() => form.variableMapping.filter(item => item.formField && item.flowVariable).length)
const businessBindingMessage = computed(() => {
  if (businessBindingAdapterMode.value)
    return '代码适配器接管状态回写和节点表单权限，平台不会直接更新业务表字段。'
  if (normalizedBusinessBindingMode.value === 'BUSINESS_TABLE') {
    if (!form.businessBinding.tableName || !form.businessBinding.primaryKeyField || !form.businessBinding.statusField)
      return '简单业务表需要明确业务表、主键字段和流程状态字段。'
    return ''
  }
  if (!form.businessBinding.statusField)
    return '未选择流程状态字段时，流程仍可发起，但业务列表不会自动显示流程状态。'
  return ''
})
watch(() => props.objectCode, () => {
  loadBinding()
}, { immediate: true })

async function loadBinding() {
  if (!props.objectCode) {
    assignBinding(props.initialBinding || createDefaultBinding())
    return
  }
  loading.value = true
  try {
    const [bindingRes] = await Promise.all([
      businessFlowBinding(props.objectCode),
      loadFlowModels(),
      loadFormAssets(),
      loadActionOptions(),
    ])
    assignBinding(bindingRes.data || props.initialBinding || createDefaultBinding())
    await loadVariableCandidates()
    emit('loaded', { ...form })
    emit('dirtyChange', false)
  }
  finally {
    loading.value = false
  }
}

function openProcessWorkspace() {
  const applicationCode = normalizeText(props.applicationCode)
    || normalizeText(route.params.applicationCode)
    || normalizeText(route.query.applicationCode)
  if (!applicationCode) {
    router.push('/app-center')
    return
  }
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode },
    query: { section: 'automation' },
  })
}

async function loadActionOptions() {
  if (!props.objectId) {
    actionOptions.value = []
    return
  }
  actionLoading.value = true
  try {
    const res = await businessObjectActions(props.objectId)
    actionOptions.value = (res.data || [])
      .filter(action => action && action.status !== 0 && isExecutableAction(action))
      .map(action => ({
        label: `${action.actionName || action.actionCode}（${action.actionCode}）`,
        value: action.actionCode,
      }))
      .filter(item => item.value)
  }
  catch {
    actionOptions.value = []
  }
  finally {
    actionLoading.value = false
  }
}

async function loadFlowModels() {
  flowModelsLoading.value = true
  try {
    const res = await flowApi.getModelList({ status: 1 })
    flowModelOptions.value = (res.data || []).map(model => ({
      label: `${model.modelName || model.name || model.modelKey || model.key}（${model.modelKey || model.key}）`,
      value: model.modelKey || model.key,
      id: model.id,
      modelName: model.modelName || model.name || model.modelKey || model.key,
    })).filter(item => item.value)
  }
  catch {
    flowModelOptions.value = []
  }
  finally {
    flowModelsLoading.value = false
  }
}

async function loadFormAssets() {
  formAssetsLoading.value = true
  formAssetWarnings.value = []
  try {
    const res = await businessFlowFormAssets(props.objectCode, { includeInternal: true })
    const data = res.data || {}
    formAssets.value = Array.isArray(data.formAssets) ? data.formAssets : []
    formAssetWarnings.value = data.warnings || []
  }
  catch (e) {
    formAssets.value = []
    formAssetWarnings.value = [e.message || '业务表单资产加载失败']
  }
  finally {
    formAssetsLoading.value = false
  }
}

async function saveConfig() {
  if (!props.objectCode)
    return
  if (!validateBeforeSave())
    return
  saving.value = true
  try {
    await saveBusinessFlowBinding(props.objectCode, buildPayload())
    message.success('流程绑定已保存')
    emit('dirtyChange', false)
    emit('saved', { ...form })
    await loadBinding()
  }
  finally {
    saving.value = false
  }
}

function validateBeforeSave() {
  if (!form.flowModelKey) {
    message.warning('请选择流程模型')
    return false
  }
  return true
}

function buildPayload() {
  return {
    flowModelKey: form.flowModelKey || '',
    flowModelName: form.flowModelName || selectedFlowName(),
    titleTemplate: form.titleTemplate || '',
    startMode: form.startMode || 'MANUAL',
    variableMapping: buildVariableMappingPayload(),
    businessBinding: normalizeBusinessBinding(form.businessBinding),
    nodeForms: [],
    conditionFlows: form.conditionFlows || [],
    options: normalizeFlowOptions(form.options || {}),
  }
}

function assignBinding(value = {}) {
  Object.assign(form, {
    ...createDefaultBinding(),
    ...value,
    startMode: normalizeStartMode(value.startMode),
    variableMapping: normalizeMappings(value.variableMapping || []),
    businessBinding: normalizeBusinessBinding(value.businessBinding),
    nodeForms: normalizeNodeForms(value.nodeForms || []),
    conditionFlows: Array.isArray(value.conditionFlows) ? value.conditionFlows : [],
    options: normalizeFlowOptions(value.options || {}),
  })
}

async function applyBindingConfig(value = {}, assetCatalog = null) {
  if (assetCatalog) {
    formAssets.value = Array.isArray(assetCatalog.formAssets) ? assetCatalog.formAssets : []
    formAssetWarnings.value = assetCatalog.warnings || []
  }
  else {
    await loadFormAssets()
  }
  assignBinding(value || createDefaultBinding())
  await Promise.all([
    loadFlowModels(),
    loadVariableCandidates(),
  ])
  emit('loaded', { ...form })
  emit('dirtyChange', false)
}

function normalizeMappings(list = []) {
  return list.map(createVariableMappingRow).filter(item => item.formField || item.flowVariable)
}

function createVariableMappingRow(item = {}) {
  const formField = normalizeText(item.formField || item.field)
  const flowVariable = normalizeText(item.flowVariable || item.variable) || formField
  return {
    clientKey: item.clientKey || `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField,
    flowVariable,
    label: item.label || fieldLabel(formField),
  }
}

function createDefaultBinding() {
  return {
    flowModelKey: '',
    flowModelName: '',
    titleTemplate: '',
    startMode: 'MANUAL',
    variableMapping: [],
    businessBinding: createDefaultBusinessBinding(),
    nodeForms: [],
    conditionFlows: [],
    options: createDefaultFlowOptions(),
  }
}

function createDefaultFlowOptions() {
  return {
    callbackActions: {
      APPROVED: '',
      REJECTED: '',
      CANCELED: '',
    },
  }
}

function normalizeFlowOptions(value = {}) {
  const source = value && typeof value === 'object' ? value : {}
  const callbackActions = normalizeCallbackActions(source.callbackActions || source.flowCallbackActions || {})
  return {
    ...source,
    callbackActions,
  }
}

function normalizeCallbackActions(value = {}) {
  const source = value && typeof value === 'object' ? value : {}
  return {
    APPROVED: normalizeText(source.APPROVED || source.approved || source.approvedActionCode),
    REJECTED: normalizeText(source.REJECTED || source.rejected || source.rejectedActionCode),
    CANCELED: normalizeText(source.CANCELED || source.CANCELLED || source.canceled || source.cancelled || source.canceledActionCode),
  }
}

function isExecutableAction(action = {}) {
  const actionConfig = action.actionConfig || {}
  return String(action.actionType || '').toUpperCase() === 'COMMAND'
    || Array.isArray(actionConfig.steps)
    || Array.isArray(actionConfig.stepList)
}

function handleFlowChange(value) {
  form.flowModelName = flowModelOptions.value.find(item => item.value === value)?.modelName || ''
  form.variableMapping = []
  form.nodeForms = []
  emitFlowContext()
  loadVariableCandidates()
  markDirty()
}

function handleBusinessBindingModeChange(value) {
  form.businessBinding.mode = normalizeBusinessBindingMode(value)
  if (!form.businessBinding.primaryKeyField)
    form.businessBinding.primaryKeyField = 'id'
  if (!form.businessBinding.tenantField)
    form.businessBinding.tenantField = 'tenant_id'
  if (form.businessBinding.mode === 'LOWCODE_OBJECT')
    fillLowcodeBusinessBindingDefaults()
  markDirty()
}

async function loadVariableCandidates() {
  variableOptions.value = []
  flowFieldCandidates.value = []
  if (!form.flowModelKey) {
    userTasks.value = []
    emitFlowContext()
    return
  }
  variablesLoading.value = true
  emitFlowContext()
  let contextError = ''
  try {
    const res = await businessFlowVariables(form.flowModelKey, { objectCode: props.objectCode })
    const data = res.data || {}
    variableOptions.value = (data.flowVariables || []).map(item => ({
      label: `${item.displayName || item.variableName}（${item.variableName}）`,
      value: item.variableName,
      source: item.source,
    })).filter(item => item.value)
    flowFieldCandidates.value = normalizeFlowFieldCandidates(data.fieldCandidates || [])
    userTasks.value = normalizeUserTasks(data.userTasks || [])
    ensureVariableMappings()
  }
  catch (e) {
    flowFieldCandidates.value = []
    userTasks.value = []
    ensureVariableMappings()
    contextError = e.message || '流程节点解析失败，可暂时使用历史手动配置'
    message.warning(e.message || '流程变量候选项加载失败')
  }
  finally {
    variablesLoading.value = false
    emitFlowContext(contextError)
  }
}

function emitFlowContext(error = '') {
  emit('contextChange', {
    flowModelKey: normalizeText(form.flowModelKey),
    flowModelName: normalizeText(form.flowModelName || selectedFlowName()),
    userTasks: userTasks.value.map(task => ({ ...task })),
    loading: variablesLoading.value,
    error,
  })
}

function ensureVariableMappings() {
  if (form.variableMapping.length || !fieldOptions.value.length)
    return
  form.variableMapping = normalizeMappings(buildAutomaticVariableMapping())
}

function addVariableMapping() {
  form.variableMapping.push(createVariableMappingRow())
  markDirty()
}

function removeVariableMapping(index) {
  form.variableMapping.splice(index, 1)
  markDirty()
}

function resetVariableMappings() {
  form.variableMapping = normalizeMappings(buildAutomaticVariableMapping())
  markDirty()
}

function updateVariableMappingField(mapping, value) {
  mapping.formField = normalizeText(value)
  if (!mapping.flowVariable)
    mapping.flowVariable = mapping.formField
  mapping.label = fieldLabel(mapping.formField)
  markDirty()
}

function selectedFlowName() {
  return flowModelOptions.value.find(item => item.value === form.flowModelKey)?.modelName || form.flowModelName || ''
}

function normalizeUserTasks(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(item => ({
      taskDefKey: normalizeText(item.taskDefKey || item.id),
      taskName: normalizeText(item.taskName || item.name),
      assignee: normalizeText(item.assignee || item.assigneeName),
      candidateUsers: normalizeList(item.candidateUsers || item.userCandidates || item.users),
      candidateGroups: normalizeList(item.candidateGroups || item.roleCandidates || item.groups),
    }))
    .filter(item => item.taskDefKey)
}

function taskAssigneeSummary(task = {}) {
  if (task.assignee)
    return `审批人：${task.assignee}`
  if (task.candidateGroups?.length)
    return `角色：${task.candidateGroups.join('、')}`
  if (task.candidateUsers?.length)
    return `用户：${task.candidateUsers.join('、')}`
  return '审批人：流程设计器配置'
}

function taskPermissionSummary(task = {}) {
  const config = nodeFormForTask(task)
  if (!config)
    return '待配置'
  const total = fieldOptions.value.length
  const configured = new Set([
    ...config.visibleFields,
    ...config.writableFields,
    ...config.requiredFields,
  ]).size
  return configured ? `已配置 ${configured}/${total || configured} 字段` : '已配置'
}

function taskPermissionType(task = {}) {
  return nodeFormForTask(task) ? 'success' : 'warning'
}

function nodeFormForTask(task = {}) {
  const taskDefKey = normalizeText(task.taskDefKey)
  return form.nodeForms.find(item => item.taskDefKey === taskDefKey) || null
}

function normalizeList(value = []) {
  if (Array.isArray(value))
    return value.map(item => normalizeText(item)).filter(Boolean)
  const text = normalizeText(value)
  return text ? [text] : []
}

function normalizeFlowFieldCandidates(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(item => ({
      fieldCode: normalizeText(item.fieldCode || item.field || item.code),
      fieldLabel: normalizeText(item.fieldLabel || item.label || item.fieldName || item.title),
      dataType: normalizeText(item.dataType || item.fieldType || item.type),
      fieldStatus: normalizeText(item.fieldStatus),
    }))
    .filter(item => item.fieldCode)
}

function normalizeNodeForms(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(normalizeNodeForm)
    .filter(item => item.taskDefKey)
}

function normalizeNodeForm(value = {}) {
  const fieldPermissions = Array.isArray(value.fieldPermissions) ? value.fieldPermissions : []
  return {
    taskDefKey: normalizeText(value.taskDefKey),
    taskName: normalizeText(value.taskName),
    formMode: normalizeText(value.formMode),
    formKey: normalizeText(value.formKey),
    formName: normalizeText(value.formName),
    providerKey: normalizeText(value.providerKey),
    formUrl: normalizeText(value.formUrl),
    viewKey: normalizeText(value.viewKey) || 'default',
    editMode: normalizeText(value.editMode) || 'READONLY',
    visibleFields: normalizeFieldSelection(value.visibleFields || fieldsByPermission(fieldPermissions, 'readable')),
    writableFields: normalizeFieldSelection(value.writableFields || fieldsByPermission(fieldPermissions, 'writable')),
    requiredFields: normalizeFieldSelection(value.requiredFields || fieldsByPermission(fieldPermissions, 'required')),
  }
}

function fieldsByPermission(fieldPermissions = [], permission) {
  if (permission === 'readable') {
    const hiddenMode = fieldPermissions.some(item => item.readable === false)
    return hiddenMode
      ? fieldPermissions.filter(item => item.readable !== false).map(item => item.field)
      : []
  }
  return fieldPermissions.filter(item => item[permission] === true).map(item => item.field)
}

function normalizeFieldSelection(value = []) {
  return Array.from(new Set((Array.isArray(value) ? value : [])
    .map(item => normalizeText(item))
    .filter(Boolean)))
}

function createDefaultBusinessBinding() {
  return {
    mode: props.codeApp ? 'ADAPTER' : 'LOWCODE_OBJECT',
    tableName: '',
    primaryKeyField: 'id',
    tenantField: 'tenant_id',
    statusField: '',
    titleField: '',
    ownerField: '',
  }
}

function normalizeBusinessBinding(value = {}) {
  const defaults = createDefaultBusinessBinding()
  const next = {
    ...defaults,
    ...(value || {}),
    mode: normalizeBusinessBindingMode(value?.mode),
    tableName: normalizeText(value?.tableName),
    primaryKeyField: normalizeText(value?.primaryKeyField) || defaults.primaryKeyField,
    tenantField: normalizeText(value?.tenantField) || defaults.tenantField,
    statusField: normalizeText(value?.statusField),
    titleField: normalizeText(value?.titleField),
    ownerField: '',
  }
  if (next.mode === 'LOWCODE_OBJECT') {
    next.statusField = next.statusField || inferStatusField()
    next.titleField = next.titleField || inferTitleField()
  }
  return next
}

function fillLowcodeBusinessBindingDefaults() {
  form.businessBinding.statusField = form.businessBinding.statusField || inferStatusField()
  form.businessBinding.titleField = form.businessBinding.titleField || inferTitleField()
}

function normalizeBusinessBindingMode(value) {
  const fallback = props.codeApp ? 'ADAPTER' : 'LOWCODE_OBJECT'
  const normalized = String(value || fallback).trim().toUpperCase()
  return businessBindingModeOptions.value.some(item => item.value === normalized) ? normalized : fallback
}

function normalizeText(value) {
  return String(value || '').trim()
}

function inferStatusField() {
  return inferFieldCode([
    'documentStatus',
    'document_status',
    'approvalStatus',
    'approval_status',
    'flowStatus',
    'flow_status',
  ], /流程状态|审批状态|单据状态/)
}

function inferTitleField() {
  return inferFieldCode([
    'title',
    'name',
    'subject',
    'documentName',
    'document_name',
    'contractName',
    'contract_name',
  ], /标题|名称|主题|合同名称|单据名称/)
}

function inferFieldCode(exactCodes = [], labelPattern = null) {
  const fields = props.fields
    .filter(field => fieldCode(field) && !isInactiveField(field))
    .map(field => ({
      code: fieldCode(field),
      label: String(field.fieldName || field.label || ''),
    }))
  const exact = fields.find(field => exactCodes.includes(field.code))
  if (exact)
    return exact.code
  const byLabel = labelPattern ? fields.find(field => labelPattern.test(field.label)) : null
  return byLabel?.code || ''
}

function fieldLabel(code) {
  if (!code)
    return ''
  const option = fieldOptions.value.find(item => item.value === code)
  return option?.label || code
}

function buildAutomaticVariableMapping() {
  const used = new Set()
  return fieldOptions.value
    .map((option) => {
      const code = normalizeText(option.value)
      if (!code || used.has(code))
        return null
      used.add(code)
      return {
        formField: code,
        flowVariable: code,
        label: option.label || code,
      }
    })
    .filter(Boolean)
}

function buildVariableMappingPayload() {
  const mappings = normalizeMappings(form.variableMapping)
  const rows = mappings.length ? mappings : normalizeMappings(buildAutomaticVariableMapping())
  return rows
    .map(row => ({
      formField: row.formField,
      flowVariable: row.flowVariable || row.formField,
      label: row.label || fieldLabel(row.formField),
    }))
    .filter(row => row.formField && row.flowVariable)
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || field.code || ''
}

function openFlowDesigner() {
  if (!selectedFlowModelId.value) {
    message.warning('请选择流程模型后再打开流程设计器')
    return
  }
  flowDesignerVisible.value = true
}

async function handleFlowDesignerSaved() {
  await loadVariableCandidates()
}

async function handleFlowDesignerClose() {
  flowDesignerVisible.value = false
  await loadVariableCandidates()
}

function normalizeStartMode(value) {
  const normalized = String(value || 'MANUAL').toUpperCase()
  if (['MANUAL_AND_TRIGGER', 'MANUAL_TRIGGER'].includes(normalized))
    return 'BOTH'
  if (['AUTO', 'AUTOMATIC'].includes(normalized))
    return 'TRIGGER'
  return startModeOptions.some(item => item.value === normalized) ? normalized : 'MANUAL'
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveConfig,
  loadBinding,
  applyBindingConfig,
  assignBinding,
  buildPayload,
  validateBeforeSave,
  openNodeConfigDesigner: openFlowDesigner,
})
</script>

<style scoped>
.business-flow-binding-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-height: 100%;
}

.legacy-entry-alert {
  margin: 12px 16px 0;
}

.business-flow-binding-panel.compact {
  border: 0;
  background: var(--bg-primary);
}

.business-flow-binding-panel.compact .flow-head {
  min-height: 54px;
  padding: 10px 16px;
}

.business-flow-binding-panel.compact .flow-main {
  padding: 0;
  background: var(--bg-primary);
}

.business-flow-binding-panel.compact:not(.compact-expanded) .flow-main > :deep(.n-spin-container) {
  padding: 0;
}

.business-flow-binding-panel.compact:not(.compact-expanded) .flow-card:first-child {
  display: grid;
  grid-template-columns: minmax(150px, 0.3fr) minmax(420px, 1fr);
  align-items: center;
  gap: 20px;
  border: 0;
  border-top: 1px solid var(--border-light);
  border-radius: 0;
  padding: 10px 16px 6px;
}

.business-flow-binding-panel.compact:not(.compact-expanded) .flow-card:first-child .flow-card-head {
  align-items: center;
  margin: 0 0 4px;
}

.business-flow-binding-panel.compact:not(.compact-expanded) .flow-card:first-child .flow-card-head p,
.business-flow-binding-panel.compact:not(.compact-expanded) .flow-step-index,
.business-flow-binding-panel.compact:not(.compact-expanded) .flow-field-binding-card,
.business-flow-binding-panel.compact:not(.compact-expanded) .advanced-flow-collapse,
.business-flow-binding-panel.compact:not(.compact-expanded) .flow-step-card {
  display: none;
}

.business-flow-binding-panel.compact:not(.compact-expanded) .flow-card:first-child :deep(.n-grid) {
  grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
}

@media (max-width: 900px) {
  .business-flow-binding-panel.compact:not(.compact-expanded) .flow-card:first-child {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

.flow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light);
  padding: 14px 16px;
}

.flow-head h3,
.flow-card h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
}

.flow-head p,
.flow-card p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.flow-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
}

.flow-body.is-read-only {
  cursor: default;
  opacity: 0.82;
  pointer-events: none;
}

.flow-main {
  min-width: 0;
  overflow: visible;
  background: var(--bg-secondary);
  padding: 14px;
}

.flow-card,
.advanced-flow-collapse {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.flow-card + .flow-card,
.flow-card + .advanced-flow-collapse {
  margin-top: 12px;
}

.flow-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.flow-step-index {
  display: inline-grid;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  place-items: center;
  border-radius: 999px;
  background: #e0ecff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  vertical-align: -2px;
}

.business-binding-alert {
  margin-top: 10px;
}

.field-binding-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.field-binding-toolbar > span {
  min-width: 0;
}

.variable-mapping-list {
  display: grid;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 2px;
}

.variable-mapping-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 44px minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.variable-mapping-row > span {
  color: var(--text-secondary);
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
}

.adapter-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.adapter-facts div {
  min-width: 0;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px;
}

.adapter-facts span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 16px;
}

.adapter-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-form-warning {
  margin-top: 10px;
}

.flow-node-list {
  display: grid;
  gap: 6px;
}

.flow-node-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--border-light);
  border-radius: 7px;
  background: var(--bg-primary);
  padding: 9px 10px;
  text-align: left;
  transition:
    border-color 160ms ease,
    background 160ms ease;
}

.flow-node-row:not(:disabled) {
  cursor: pointer;
}

.flow-node-row:not(:disabled):hover,
.flow-node-row:not(:disabled):focus-visible {
  border-color: #bfdbfe;
  background: #f8fbff;
  outline: none;
}

.flow-node-row:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.flow-node-row strong,
.flow-node-row small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-node-row strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 18px;
}

.flow-node-row small,
.flow-node-row > span:nth-child(2) {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.flow-node-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.advanced-flow-collapse {
  padding: 8px 14px 12px;
}

.advanced-flow-collapse :deep(.n-collapse-item__header) {
  min-height: 44px;
  padding: 10px 0;
  align-items: center;
}

.advanced-flow-collapse :deep(.n-collapse-item__header-main) {
  min-height: 28px;
  align-items: center;
  overflow: visible;
}

.advanced-flow-title {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  min-height: 28px;
  color: var(--text-primary);
  font-weight: 700;
  line-height: 22px;
}

.flow-designer-modal-shell {
  width: min(1720px, calc(100vw - 24px));
  height: calc(100vh - 24px);
  max-height: calc(100vh - 24px);
  min-height: 0;
  display: flex;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-secondary);
  box-shadow: 0 24px 72px rgba(15, 23, 42, 0.28);
}

.embedded-flow-design-page {
  flex: 1;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

@media (max-width: 1100px) {
  .adapter-facts {
    grid-template-columns: 1fr;
  }

  .flow-node-row {
    grid-template-columns: 1fr;
  }

  .flow-node-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .field-binding-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .variable-mapping-row {
    grid-template-columns: 1fr;
  }

  .variable-mapping-row > span {
    text-align: left;
  }
}
</style>
