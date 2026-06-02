<template>
  <div class="business-action-designer">
    <div class="action-designer-head">
      <div>
        <h3>自定义操作</h3>
        <p>维护工具栏、行操作和详情操作，普通模式只填写业务动作。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="loadActions">
          刷新
        </n-button>
        <n-button size="small" secondary @click="addFlowAction">
          添加发起流程
        </n-button>
        <n-button size="small" secondary @click="addAction('ROW')">
          新增操作
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveActions">
          保存操作
        </n-button>
      </n-space>
    </div>

    <div class="action-designer-body">
      <main class="action-list-pane">
        <n-spin :show="loading">
          <div v-if="localActions.length" class="action-card-list">
            <section v-for="(action, index) in localActions" :key="action.clientKey" class="action-card">
              <header class="action-card-head">
                <div>
                  <strong>{{ action.actionName || '未命名操作' }}</strong>
                  <p>{{ actionPositionLabel(action.actionPosition) }} · {{ actionTypeLabel(action.actionType) }}</p>
                </div>
                <n-space size="small">
                  <n-tag size="small" :type="action.status === 0 ? 'default' : 'success'" :bordered="false">
                    {{ action.status === 0 ? '停用' : '启用' }}
                  </n-tag>
                  <n-popconfirm @positive-click="removeAction(index)">
                    <template #trigger>
                      <n-button quaternary circle size="small">
                        <template #icon>
                          <n-icon><TrashOutline /></n-icon>
                        </template>
                      </n-button>
                    </template>
                    确认删除该操作？
                  </n-popconfirm>
                </n-space>
              </header>

              <n-form label-placement="top" :show-feedback="false" size="small" class="action-form">
                <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                  <n-form-item-gi label="操作名称">
                    <n-input v-model:value="action.actionName" placeholder="例如：发起流程" @update:value="value => updateActionName(action, value)" />
                  </n-form-item-gi>
                  <n-form-item-gi label="操作位置">
                    <n-select v-model:value="action.actionPosition" :options="positionOptions" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="操作类型">
                    <n-select v-model:value="action.actionType" :options="actionTypeOptions" @update:value="value => updateActionType(action, value)" />
                  </n-form-item-gi>
                  <n-form-item-gi label="权限标识">
                    <n-input v-model:value="action.permission" placeholder="例如：ai:businessObject:publish" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="二次确认">
                    <n-switch v-model:value="action.confirmRequired" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="启用状态">
                    <n-switch :value="action.status !== 0" @update:value="value => updateStatus(action, value)" />
                  </n-form-item-gi>
                  <n-form-item-gi label="成功提示">
                    <n-input v-model:value="action.successMessage" placeholder="操作成功" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="失败提示">
                    <n-input v-model:value="action.failureMessage" placeholder="操作失败" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="排序">
                    <n-input-number v-model:value="action.sortOrder" :min="0" style="width: 100%" @update:value="markDirty" />
                  </n-form-item-gi>

                  <n-form-item-gi v-if="action.actionType === 'OPEN_PAGE'" :span="3" label="目标页面">
                    <n-input v-model:value="action.actionConfig.targetPath" placeholder="例如：/app-center/object/CUSTOMER" @update:value="markDirty" />
                  </n-form-item-gi>
                  <template v-else-if="action.actionType === 'START_FLOW'">
                    <n-form-item-gi label="流程模型">
                      <n-select
                        v-model:value="action.actionConfig.flowModelKey"
                        :options="flowModelOptions"
                        :loading="flowModelsLoading"
                        clearable
                        filterable
                        placeholder="选择已发布流程"
                        @update:value="value => updateFlowModel(action, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi :span="2" label="流程标题模板">
                      <n-input v-model:value="action.actionConfig.titleTemplate" placeholder="例如：${name}-流程" @update:value="markDirty" />
                    </n-form-item-gi>
                    <n-form-item-gi :span="3" label="变量映射">
                      <div class="action-mapping-list">
                        <div v-for="(mapping, mappingIndex) in action.actionConfig.variableMapping" :key="mapping.clientKey" class="action-mapping-row">
                          <n-select
                            v-model:value="mapping.formField"
                            :options="fieldOptions"
                            clearable
                            filterable
                            placeholder="单据字段"
                            @update:value="value => updateMappingLabel(mapping, value)"
                          />
                          <span>→</span>
                          <n-input v-model:value="mapping.flowVariable" placeholder="流程变量名" @update:value="markDirty" />
                          <n-button quaternary circle size="small" @click="removeMapping(action, mappingIndex)">
                            <template #icon>
                              <n-icon><TrashOutline /></n-icon>
                            </template>
                          </n-button>
                        </div>
                        <n-button dashed size="small" @click="addMapping(action)">
                          添加变量映射
                        </n-button>
                      </div>
                    </n-form-item-gi>
                  </template>
                  <n-form-item-gi v-else-if="action.actionType === 'OPEN_EXTERNAL'" :span="3" label="外部链接">
                    <n-input v-model:value="action.actionConfig.url" placeholder="https://example.com" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi v-else-if="action.actionType === 'TRIGGER'" :span="3" label="触发器标识">
                    <n-input v-model:value="action.actionConfig.triggerCode" placeholder="例如：customer_notify" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi v-else :span="3" label="能力标识">
                    <n-input v-model:value="action.actionConfig.capabilityCode" placeholder="填写已封装的业务能力标识" @update:value="markDirty" />
                  </n-form-item-gi>
                </n-grid>
              </n-form>
            </section>
          </div>
          <n-empty v-else-if="!loading" description="暂无自定义操作" />
        </n-spin>
      </main>

      <aside class="action-summary-pane">
        <section>
          <h4>操作分布</h4>
          <div class="action-stat-grid">
            <div v-for="item in actionStats" :key="item.position">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </div>
          </div>
        </section>
        <section>
          <h4>发布关注</h4>
          <p>自定义操作会进入发布检查摘要；发起流程操作需要完成流程绑定和按钮权限配置。</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessObjectActions,
  saveBusinessObjectActions,
} from '@/api/business-app'
import flowApi from '@/api/flow'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['updated', 'dirtyChange'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const flowModelsLoading = ref(false)
const localActions = ref([])
const flowModelOptions = ref([])

const positionOptions = [
  { label: '工具栏', value: 'TOOLBAR' },
  { label: '行操作', value: 'ROW' },
  { label: '详情页', value: 'DETAIL' },
]

const actionTypeOptions = [
  { label: '打开页面', value: 'OPEN_PAGE' },
  { label: '调用能力', value: 'CALL_API' },
  { label: '发起流程', value: 'START_FLOW' },
  { label: '执行触发器', value: 'TRIGGER' },
  { label: '打开外部链接', value: 'OPEN_EXTERNAL' },
]

const fieldOptions = computed(() => props.fields
  .filter(field => fieldCode(field) && !isInactiveField(field))
  .map(field => ({
    label: `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）`,
    value: fieldCode(field),
  })))
const actionStats = computed(() => positionOptions.map(item => ({
  position: item.value,
  label: item.label,
  count: localActions.value.filter(action => action.actionPosition === item.value && action.status !== 0).length,
})))

onMounted(loadFlowModels)

watch(() => props.objectId, () => {
  loadActions()
}, { immediate: true })

async function loadActions() {
  if (!props.objectId) {
    localActions.value = []
    return
  }
  loading.value = true
  try {
    const res = await businessObjectActions(props.objectId)
    localActions.value = (res.data || []).map(normalizeAction)
    emit('updated', localActions.value)
  }
  finally {
    loading.value = false
  }
}

async function loadFlowModels() {
  flowModelsLoading.value = true
  try {
    const res = await flowApi.getModelList({ status: 1 })
    flowModelOptions.value = (res.data || []).map(model => ({
      label: `${model.modelName || model.name || model.modelKey || model.key}（${model.modelKey || model.key}）`,
      value: model.modelKey || model.key,
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

function addAction(position = 'ROW') {
  localActions.value.push(normalizeAction({
    actionCode: `custom_${Date.now()}`,
    actionName: '自定义操作',
    actionPosition: position,
    actionType: 'OPEN_PAGE',
    confirmRequired: false,
    status: 1,
    sortOrder: localActions.value.length * 10 + 10,
    actionConfig: {},
  }))
  emit('dirtyChange', true)
}

function addFlowAction() {
  localActions.value.push(normalizeAction({
    actionCode: `start_flow_${Date.now()}`,
    actionName: '发起流程',
    actionPosition: 'ROW',
    actionType: 'START_FLOW',
    confirmRequired: true,
    successMessage: '流程已发起',
    failureMessage: '流程发起失败',
    status: 1,
    sortOrder: localActions.value.length * 10 + 10,
    actionConfig: { variableMapping: [] },
  }))
  emit('dirtyChange', true)
}

function removeAction(index) {
  localActions.value.splice(index, 1)
  emit('dirtyChange', true)
}

function updateActionName(action, value) {
  action.actionName = value
  if (!action.actionCode || /^custom_\d+/.test(action.actionCode))
    action.actionCode = normalizeActionCode(value) || action.actionCode
  markDirty()
}

function updateStatus(action, value) {
  action.status = value ? 1 : 0
  markDirty()
}

async function saveActions() {
  if (!props.objectId)
    return
  saving.value = true
  try {
    await saveBusinessObjectActions(props.objectId, localActions.value.map(toActionPayload))
    message.success('自定义操作已保存')
    emit('dirtyChange', false)
    await loadActions()
  }
  finally {
    saving.value = false
  }
}

function normalizeAction(action = {}) {
  return {
    ...action,
    clientKey: action.actionCode || `action_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    actionCode: action.actionCode || normalizeActionCode(action.actionName) || `custom_${Date.now()}`,
    actionName: action.actionName || '自定义操作',
    actionPosition: normalizePosition(action.actionPosition),
    actionType: normalizeActionType(action.actionType),
    confirmRequired: Boolean(action.confirmRequired),
    successMessage: action.successMessage || '',
    failureMessage: action.failureMessage || '',
    status: action.status ?? 1,
    sortOrder: action.sortOrder ?? 0,
    actionConfig: normalizeActionConfig(action.actionType, action.actionConfig),
  }
}

function toActionPayload(action = {}) {
  return {
    actionCode: normalizeActionCode(action.actionCode) || normalizeActionCode(action.actionName),
    actionName: action.actionName,
    actionPosition: normalizePosition(action.actionPosition),
    actionType: normalizeActionType(action.actionType),
    permission: action.permission,
    confirmRequired: Boolean(action.confirmRequired),
    successMessage: action.successMessage,
    failureMessage: action.failureMessage,
    status: action.status ?? 1,
    sortOrder: action.sortOrder ?? 0,
    actionConfig: normalizeActionConfigForPayload(action.actionType, action.actionConfig),
  }
}

function normalizePosition(value) {
  const normalized = String(value || 'ROW').replace('-', '_').toUpperCase()
  return positionOptions.some(item => item.value === normalized) ? normalized : 'ROW'
}

function normalizeActionType(value) {
  const normalized = String(value || 'OPEN_PAGE')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace('-', '_')
    .toUpperCase()
  if (normalized === 'START_APPROVAL')
    return 'START_FLOW'
  return actionTypeOptions.some(item => item.value === normalized) ? normalized : 'OPEN_PAGE'
}

function normalizeActionConfig(actionType, config = {}) {
  const source = typeof config === 'string' ? safeParseJson(config) : { ...(config || {}) }
  if (normalizeActionType(actionType) !== 'START_FLOW')
    return source
  return {
    ...source,
    flowModelKey: source.flowModelKey || source.flowKey || '',
    flowModelName: source.flowModelName || '',
    titleTemplate: source.titleTemplate || '',
    variableMapping: normalizeVariableMapping(source.variableMapping || []),
  }
}

function normalizeActionConfigForPayload(actionType, config = {}) {
  const source = normalizeActionConfig(actionType, config)
  if (normalizeActionType(actionType) !== 'START_FLOW')
    return source
  return {
    ...source,
    variableMapping: (source.variableMapping || [])
      .map(item => ({
        formField: item.formField || '',
        flowVariable: item.flowVariable || '',
        label: item.label || fieldLabel(item.formField),
      }))
      .filter(item => item.formField && item.flowVariable),
  }
}

function updateActionType(action, value) {
  action.actionType = normalizeActionType(value)
  action.actionConfig = normalizeActionConfig(action.actionType, action.actionConfig)
  markDirty()
}

function normalizeVariableMapping(list = []) {
  return (list || []).map(item => ({
    clientKey: item.clientKey || `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: item.formField || item.field || null,
    flowVariable: item.flowVariable || item.variable || '',
    label: item.label || fieldLabel(item.formField || item.field),
  })).filter(item => item.formField || item.flowVariable)
}

function updateFlowModel(action, value) {
  const model = flowModelOptions.value.find(item => item.value === value)
  action.actionConfig.flowModelName = model?.modelName || ''
  markDirty()
}

function addMapping(action) {
  if (!Array.isArray(action.actionConfig.variableMapping))
    action.actionConfig.variableMapping = []
  action.actionConfig.variableMapping.push({
    clientKey: `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: null,
    flowVariable: '',
    label: '',
  })
  markDirty()
}

function removeMapping(action, index) {
  action.actionConfig.variableMapping.splice(index, 1)
  markDirty()
}

function updateMappingLabel(mapping, value) {
  mapping.label = fieldLabel(value)
  markDirty()
}

function fieldLabel(code) {
  if (!code)
    return ''
  return fieldOptions.value.find(item => item.value === code)?.label || code
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || ''
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function safeParseJson(value) {
  try {
    return JSON.parse(value || '{}')
  }
  catch {
    return {}
  }
}

function normalizeActionCode(value) {
  return String(value || '')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase()
    .slice(0, 64)
}

function actionPositionLabel(value) {
  return positionOptions.find(item => item.value === value)?.label || value || '-'
}

function actionTypeLabel(value) {
  return actionTypeOptions.find(item => item.value === value)?.label || value || '-'
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveActions,
  loadActions,
})
</script>

<style scoped>
.business-action-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.action-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.action-designer-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.action-designer-head p,
.action-card-head p,
.action-summary-pane p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.action-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.action-list-pane {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.action-card-list {
  display: grid;
  gap: 12px;
}

.action-card,
.action-summary-pane section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.action-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.action-card-head strong,
.action-summary-pane h4 {
  margin: 0;
  color: #111827;
  font-size: 14px;
}

.action-summary-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.action-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.action-stat-grid div {
  border-radius: 6px;
  background: #f1f5f9;
  padding: 10px;
  text-align: center;
}

.action-stat-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.action-stat-grid strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 18px;
}

.action-mapping-list {
  display: grid;
  gap: 8px;
  width: 100%;
}

.action-mapping-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 24px minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.action-mapping-row span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 1100px) {
  .action-designer-body {
    grid-template-columns: 1fr;
  }

  .action-summary-pane {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }

  .action-mapping-row {
    grid-template-columns: 1fr;
  }
}
</style>
