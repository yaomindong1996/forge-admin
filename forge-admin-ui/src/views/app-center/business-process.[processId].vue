<script setup>
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  businessFlowFormAssets,
  businessObjectActions,
  businessObjectDesigner,
  businessObjectFields,
  saveBusinessObjectActions,
} from '@/api/business-app'
import { businessApplicationObjects } from '@/api/business-application'
import {
  businessProcessDesigner,
  businessProcessFlowModels,
  businessProcessPage,
  publishBusinessProcess,
  saveBusinessProcessSchema,
  validateBusinessProcess,
} from '@/api/business-process'
import messageApi from '@/api/message'
import { businessProcessHashInput } from '@/components/business-process-designer/business-process-schema.js'
import BusinessProcessDesigner from '@/components/business-process-designer/BusinessProcessDesigner.vue'
import ObjectActionEditor from './components/designer/ObjectActionEditor.vue'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
  processId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close', 'saved'])

const FlowDesignPage = defineAsyncComponent(() => import('@/views/flow/design.vue'))

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const process = ref(null)
const draftSchema = ref(null)
const draftSchemaHash = ref('')
const serverValidation = ref(null)
const saveState = ref('idle')
const saveError = ref('')
const dirty = ref(false)
const saveQueued = ref(false)
const publishing = ref(false)
const applicationObjects = ref([])
const fields = ref([])
const flowModels = ref([])
const formAssets = ref([])
const businessActions = ref([])
const messageTemplates = ref([])
const subProcesses = ref([])
const capabilities = ref([])
const serviceActors = ref([])
const flowDesignerVisible = ref(false)
const flowDesignerModelId = ref('')
const flowDesignerContext = ref({
  modelKey: '',
  businessObjectCode: '',
  businessObjectName: '',
  applicationId: '',
  businessFormKey: '',
})
let activeSavePromise = null
let flowCatalogRequestId = 0

const processId = computed(() => props.embedded ? stringValue(props.processId) : stringValue(route.params.processId))
const subjectObject = computed(() => applicationObjects.value.find(item => (
  stringValue(item.objectId || item.id) === stringValue(process.value?.subjectObjectId)
  || item.objectCode === process.value?.subjectObjectCode
)) || null)
const pageTitle = computed(() => process.value?.processName || '业务流程设计')

watch(() => props.embedded ? props.processId : route.params.processId, loadDesigner)
onMounted(loadDesigner)

onBeforeRouteLeave(() => {
  if (props.embedded)
    return true
  if (!dirty.value)
    return true
  return confirmLeave()
})

async function loadDesigner() {
  if (!processId.value || loading.value)
    return
  loading.value = true
  loadError.value = ''
  try {
    const response = await businessProcessDesigner(processId.value)
    const data = normalizeIds(response.data || {})
    if (!data.id || !data.businessProcessJson)
      throw new Error('业务流程草稿不存在或已删除')
    process.value = data
    draftSchema.value = clone(data.businessProcessJson)
    draftSchemaHash.value = stringValue(data.draftSchemaHash)
    serverValidation.value = data.validation || null
    dirty.value = false
    saveQueued.value = false
    saveState.value = 'idle'
    saveError.value = ''
    await loadCatalogs()
  }
  catch (error) {
    process.value = null
    draftSchema.value = null
    loadError.value = errorMessage(error, '业务流程草稿加载失败')
  }
  finally {
    loading.value = false
  }
}

async function loadCatalogs() {
  const applicationId = stringValue(process.value?.applicationId)
  const objectId = stringValue(process.value?.subjectObjectId)
  const objectCode = process.value?.subjectObjectCode || draftSchema.value?.subject?.objectCode || ''

  const [objectsResult, fieldsResult, actionsResult, modelsResult, assetsResult, templatesResult, processesResult]
    = await Promise.allSettled([
      applicationId ? businessApplicationObjects(applicationId) : Promise.resolve({ data: [] }),
      objectId ? businessObjectFields(objectId) : Promise.resolve({ data: [] }),
      objectId ? businessObjectActions(objectId) : Promise.resolve({ data: [] }),
      businessProcessFlowModels(processId.value),
      objectCode
        ? businessFlowFormAssets(objectCode, {
            includeInternal: true,
            applicationId,
          })
        : Promise.resolve({ data: { formAssets: [] } }),
      messageApi.getTemplatePage({ pageNum: 1, pageSize: 200, status: 1 }),
      applicationId
        ? businessProcessPage({ applicationId, pageNum: 1, pageSize: 200, status: 1 })
        : Promise.resolve({ data: { records: [] } }),
    ])

  applicationObjects.value = settledData(objectsResult, [])
    .map(normalizeIds)
  fields.value = settledData(fieldsResult, []).map(normalizeIds)
  businessActions.value = settledData(actionsResult, [])
    .filter(item => item && Number(item.status ?? 1) !== 0)
    .map(normalizeIds)
  flowModels.value = settledData(modelsResult, [])
    .map(item => normalizeIds({
      ...item,
      modelId: item.modelId || item.id,
      deployed: item.deployed === true || Boolean(item.deploymentId),
    }))
  const assetData = settledData(assetsResult, { formAssets: [] })
  formAssets.value = (Array.isArray(assetData) ? assetData : assetData.formAssets || []).map(normalizeIds)
  const templateData = settledData(templatesResult, { records: [] })
  messageTemplates.value = (Array.isArray(templateData) ? templateData : templateData.records || [])
    .filter(item => item && Number(item.status ?? 1) !== 0)
    .map(normalizeIds)
  const processData = settledData(processesResult, { records: [] })
  subProcesses.value = (Array.isArray(processData) ? processData : processData.records || [])
    .filter(item => stringValue(item.id) !== processId.value && item.publishedVersion != null)
    .map(normalizeIds)

  // 受治理能力桥接和定时服务账号目录尚未交付时保持空目录，配置节点据此失败关闭。
  capabilities.value = []
  serviceActors.value = []
}

async function refreshFlowCatalog() {
  const requestId = ++flowCatalogRequestId
  const objectId = stringValue(process.value?.subjectObjectId)
  const objectCode = process.value?.subjectObjectCode || ''
  const [fieldsResult, modelsResult, assetsResult] = await Promise.allSettled([
    objectId ? businessObjectFields(objectId) : Promise.resolve({ data: [] }),
    businessProcessFlowModels(processId.value),
    objectCode
      ? businessFlowFormAssets(objectCode, {
          includeInternal: true,
          applicationId: stringValue(process.value?.applicationId),
        })
      : Promise.resolve({ data: { formAssets: [] } }),
  ])
  if (requestId !== flowCatalogRequestId)
    return
  fields.value = settledData(fieldsResult, fields.value).map(normalizeIds)
  flowModels.value = settledData(modelsResult, flowModels.value)
    .map(item => normalizeIds({
      ...item,
      modelId: item.modelId || item.id,
      deployed: item.deployed === true || Boolean(item.deploymentId),
    }))
  const assetData = settledData(assetsResult, { formAssets: [] })
  formAssets.value = (Array.isArray(assetData) ? assetData : assetData.formAssets || []).map(normalizeIds)
}

async function refreshBusinessObjectFields() {
  await refreshFlowCatalog()
}

function handleSchemaUpdate(value) {
  draftSchema.value = clone(value)
  if (saveState.value === 'saving')
    saveQueued.value = true
}

function handleDirtyChange(value) {
  dirty.value = Boolean(value)
}

// ===== 画布内直接编辑 / 新建业务动作（动作是流程的执行节点） =====
const actionEditorVisible = ref(false)
const actionEditorContext = ref(null) // { actionCode, isNew, nodeId }
const subjectDesigner = ref(null) // 主对象设计数据（fields/relations/configKey）

async function openActionEditor(payload = {}) {
  const objectId = stringValue(process.value?.subjectObjectId)
  if (!objectId) {
    window.$message?.warning('该流程未绑定业务对象，无法配置动作')
    return
  }
  if (!subjectDesigner.value) {
    try {
      const res = await businessObjectDesigner(objectId)
      subjectDesigner.value = res?.data || {}
    }
    catch {
      subjectDesigner.value = {}
    }
  }
  actionEditorContext.value = payload
  actionEditorVisible.value = true
}

const editingAction = computed(() => {
  const ctx = actionEditorContext.value
  if (!ctx)
    return []
  if (ctx.isNew) {
    return [{
      actionCode: `action_${Date.now()}`,
      actionName: '新操作',
      actionPosition: 'ROW',
      actionType: 'COMMAND',
      permission: '',
      confirmRequired: true,
      successMessage: '',
      status: 1,
      sortOrder: (businessActions.value.length + 1) * 10,
      actionConfig: { triggerScene: 'MANUAL', executionMode: 'LOCAL_TRANSACTION', inputSchema: [], steps: [] },
    }]
  }
  return businessActions.value.filter(item => stringValue(item?.actionCode) === stringValue(ctx.actionCode))
})

const subjectDesignerSummaries = computed(() => {
  const objectId = stringValue(process.value?.subjectObjectId)
  return objectId && subjectDesigner.value ? { [objectId]: subjectDesigner.value } : {}
})

function bindActionToNode(nodeId, actionCode) {
  const schema = draftSchema.value
  const nodes = schema?.nodes
  if (!Array.isArray(nodes))
    return
  const index = nodes.findIndex(node => node?.id === nodeId)
  if (index < 0)
    return
  const nextNodes = nodes.slice()
  nextNodes[index] = {
    ...nextNodes[index],
    config: { ...(nextNodes[index].config || {}), actionType: 'BUSINESS_ACTION', businessActionCode: actionCode },
  }
  draftSchema.value = { ...schema, nodes: nextNodes }
  dirty.value = true
}

async function handleActionSaved(actions) {
  const objectId = stringValue(process.value?.subjectObjectId)
  const saved = actions?.[0]
  if (!objectId || !saved?.actionCode)
    return
  try {
    const full = await businessObjectActions(objectId)
    const list = (full?.data || []).slice()
    const index = list.findIndex(item => stringValue(item?.actionCode) === stringValue(saved.actionCode))
    if (index >= 0)
      list.splice(index, 1, saved)
    else
      list.push(saved)
    await saveBusinessObjectActions(objectId, list)
    window.$message?.success(`业务动作「${saved.actionName || saved.actionCode}」已保存`)
    if (actionEditorContext.value?.isNew && actionEditorContext.value.nodeId)
      bindActionToNode(actionEditorContext.value.nodeId, saved.actionCode)
    actionEditorVisible.value = false
    await loadCatalogs()
  }
  catch (error) {
    window.$message?.error(error?.message || '保存业务动作失败')
  }
}

async function handleSave(schema, metadata = {}) {
  return persistSchema(schema, metadata)
}

function persistSchema(schema, metadata = {}) {
  if (activeSavePromise)
    return activeSavePromise
  const task = persistSchemaInternal(schema, metadata)
  activeSavePromise = task
  return task.finally(() => {
    if (activeSavePromise === task)
      activeSavePromise = null
  })
}

async function persistSchemaInternal(schema, metadata = {}) {
  if (!processId.value || !schema)
    return false
  if (!isServerHash(draftSchemaHash.value)) {
    saveState.value = 'error'
    saveError.value = '草稿缺少服务端并发基线，请刷新后重试。'
    return false
  }

  const submittedSchema = clone(schema)
  const submittedHashInput = businessProcessHashInput(submittedSchema)
  saveQueued.value = false
  saveState.value = 'saving'
  saveError.value = ''
  try {
    const response = await saveBusinessProcessSchema(processId.value, {
      businessProcessJson: submittedSchema,
      expectedSchemaHash: draftSchemaHash.value,
    })
    const data = normalizeIds(response.data || {})
    const nextServerHash = stringValue(data.draftSchemaHash)
    if (!isServerHash(nextServerHash))
      throw new Error('服务端未返回有效草稿摘要')
    draftSchemaHash.value = nextServerHash
    process.value = { ...process.value, ...data }
    serverValidation.value = data.validation || serverValidation.value

    const currentHashInput = businessProcessHashInput(draftSchema.value)
    if (saveQueued.value || currentHashInput !== submittedHashInput) {
      saveState.value = 'idle'
      dirty.value = true
      saveQueued.value = false
      return persistSchemaInternal(draftSchema.value, { ...metadata, reason: 'queued' })
    }

    draftSchema.value = clone(data.businessProcessJson || submittedSchema)
    dirty.value = false
    saveState.value = 'saved'
    if (metadata.reason === 'manual')
      notify('success', '业务流程草稿已保存')
    return true
  }
  catch (error) {
    dirty.value = true
    if (isConflict(error)) {
      saveState.value = 'conflict'
      saveError.value = errorMessage(error, '草稿已被其他人更新')
    }
    else {
      saveState.value = 'error'
      saveError.value = errorMessage(error, '业务流程草稿保存失败')
    }
    return false
  }
}

async function handleValidate(schema) {
  if (activeSavePromise)
    await activeSavePromise
  const currentSchema = clone(draftSchema.value || schema)
  if (dirty.value) {
    const saved = await persistSchema(currentSchema, { reason: 'validate' })
    if (!saved)
      return
  }
  try {
    const response = await validateBusinessProcess(processId.value)
    serverValidation.value = response.data || null
    if (serverValidation.value?.valid)
      notify('success', '流程检查通过，可进入应用发布检查')
    else
      notify('warning', `流程检查发现 ${serverValidation.value?.errorCount || 0} 项错误`)
  }
  catch (error) {
    notify('error', errorMessage(error, '业务流程检查失败'))
  }
}

function openFlowDesigner(payload = {}) {
  const modelId = stringValue(payload.modelId || payload.id)
  if (!modelId) {
    notify('warning', '请先选择或创建审批流程模型')
    return
  }
  const applicationId = stringValue(process.value?.applicationId)
  const objectCode = stringValue(process.value?.subjectObjectCode || draftSchema.value?.subject?.objectCode)
  const defaultFormKey = stringValue(formAssets.value.find(item => item?.formKey)?.formKey)
  if (props.embedded) {
    flowDesignerModelId.value = modelId
    flowDesignerContext.value = {
      modelKey: stringValue(payload.modelKey),
      businessObjectCode: objectCode,
      businessObjectName: stringValue(subjectObject.value?.objectName || objectCode),
      applicationId: stringValue(payload.applicationId || applicationId),
      businessFormKey: stringValue(payload.businessFormKey || defaultFormKey),
    }
    flowDesignerVisible.value = true
    return
  }
  router.push({
    path: '/flow/design',
    query: {
      id: modelId,
      businessObjectCode: objectCode || undefined,
      businessObjectName: stringValue(subjectObject.value?.objectName || objectCode) || undefined,
      applicationId: stringValue(payload.applicationId || applicationId) || undefined,
      businessFormKey: stringValue(payload.businessFormKey || defaultFormKey) || undefined,
      returnTo: route.fullPath,
    },
  })
}

function handleFlowDesignerClose() {
  flowDesignerVisible.value = false
  void refreshFlowCatalog()
}

async function handleFlowDesignerSaved(model = {}) {
  mergeFlowModelSnapshot(model)
  await refreshFlowCatalog()
}

function mergeFlowModelSnapshot(model = {}) {
  const modelId = stringValue(model.id || model.modelId || flowDesignerModelId.value)
  const modelKey = stringValue(model.modelKey || flowDesignerContext.value.modelKey)
  if (!modelId && !modelKey)
    return
  const normalized = normalizeIds({
    ...model,
    modelId,
    modelKey,
    modelName: model.modelName || model.name || modelKey,
    deployed: model.deployed === true || Boolean(model.deploymentId),
  })
  const index = flowModels.value.findIndex(item => (
    stringValue(item.modelId || item.id) === modelId
    || (modelKey && stringValue(item.modelKey) === modelKey)
  ))
  if (index < 0) {
    flowModels.value = [normalized, ...flowModels.value]
    return
  }
  const next = [...flowModels.value]
  next[index] = { ...next[index], ...normalized }
  flowModels.value = next
}

function returnToApplication() {
  if (props.embedded) {
    emit('close')
    return
  }
  const returnTo = String(route.query.returnTo || '')
  if (isLocalPath(returnTo)) {
    router.push(resolveReturnTarget(returnTo))
    return
  }
  const applicationCode = String(route.query.applicationCode || '')
  if (applicationCode) {
    router.push({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode },
      query: { designSection: 'automation' },
    })
    return
  }
  router.push('/app-center')
}

// 独立发布：只生成当前流程的不可变版本并切换运行投影，不触发应用发布。
async function openApplicationPublish() {
  if (publishing.value)
    return
  if (dirty.value) {
    notify('warning', '请先保存流程草稿，再发布流程')
    return
  }
  if (!window.$dialog)
    return
  window.$dialog.warning({
    title: '发布业务流程',
    content: '将为当前流程生成不可变流程版本并立即生效，应用内其他资产与流程不受影响。',
    positiveText: '检查并发布',
    negativeText: '取消',
    onPositiveClick: () => executePublish(),
  })
}

async function executePublish() {
  if (publishing.value)
    return
  publishing.value = true
  try {
    const validation = await validateBusinessProcess(processId.value)
    if (!validation.data?.valid) {
      const errorCount = Number(validation.data?.errorCount || 0)
      notify('warning', errorCount
        ? `流程检查发现 ${errorCount} 项错误，请修正后再发布`
        : '流程检查未通过，请修正后再发布')
      return
    }
    const response = await publishBusinessProcess(processId.value)
    const versionNo = Number(response.data?.versionNo || 0)
    notify('success', versionNo ? `业务流程已发布为 V${versionNo}` : '业务流程发布成功')
    if (props.embedded) {
      emit('saved')
      return
    }
    returnToApplication()
  }
  catch (error) {
    notify('error', errorMessage(error, '业务流程发布失败'))
  }
  finally {
    publishing.value = false
  }
}

function resolveReturnTarget(returnTo) {
  // button（按钮动作配置）与 designer（页面设计资源树）两类来源方都会监听 processRefresh 重载列表。
  if (!['button', 'designer'].includes(String(route.query.from || '')))
    return returnTo
  const target = new URL(returnTo, window.location.origin)
  target.searchParams.set('processRefresh', processId.value)
  return `${target.pathname}${target.search}${target.hash}`
}

function confirmLeave() {
  return new Promise((resolve) => {
    if (!window.$dialog) {
      resolve(false)
      return
    }
    window.$dialog.warning({
      title: '未保存变更',
      content: '当前业务流程有未保存的修改，确认离开吗？',
      positiveText: '离开',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
}

function settledData(result, fallback) {
  return result.status === 'fulfilled' ? (result.value?.data ?? fallback) : fallback
}

function normalizeIds(value) {
  if (Array.isArray(value))
    return value.map(normalizeIds)
  if (!value || typeof value !== 'object')
    return value
  const result = {}
  Object.entries(value).forEach(([key, item]) => {
    if ((key === 'id' || key.endsWith('Id')) && item != null && ['number', 'string'].includes(typeof item)) {
      result[key] = String(item)
      return
    }
    if (key.endsWith('Ids') && Array.isArray(item)) {
      result[key] = item.map(id => stringValue(id))
      return
    }
    result[key] = normalizeIds(item)
  })
  return result
}

function clone(value) {
  return value == null ? value : JSON.parse(JSON.stringify(value))
}

function stringValue(value) {
  return value == null ? '' : String(value)
}

function isServerHash(value) {
  return /^[a-f0-9]{64}$/.test(String(value || ''))
}

function isConflict(error) {
  return Number(error?.response?.status || error?.status) === 409
}

function isLocalPath(value) {
  return value.startsWith('/') && !value.startsWith('//')
}

function errorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

function notify(type, message) {
  window.$message?.[type]?.(message)
}
</script>

<template>
  <div class="process-designer-page" :class="{ 'process-designer-embedded': embedded }">
    <header class="process-page-header">
      <button
        type="button"
        class="back-button"
        data-process-action="back"
        @click="returnToApplication"
      >
        <span aria-hidden="true">←</span>
        {{ embedded ? '关闭' : '返回业务流程' }}
      </button>
      <div v-if="process" class="process-page-identity">
        <strong>{{ pageTitle }}</strong>
        <code>{{ process.processCode }}</code>
        <span>{{ subjectObject?.objectName || process.subjectObjectCode }}</span>
      </div>
      <div class="process-page-boundary">
        <span>应用画布负责业务编排，审批内部配置仍由 Flowable 管理</span>
        <n-button
          data-process-action="publish"
          size="tiny"
          type="primary"
          secondary
          :loading="publishing"
          title="生成不可变流程版本并立即生效，不影响应用内其他资产"
          @click="openApplicationPublish"
        >
          发布
        </n-button>
      </div>
    </header>

    <main class="process-page-main">
      <div v-if="loading" class="page-loading">
        <n-spin size="medium" />
        <span>正在加载业务流程草稿…</span>
      </div>

      <n-result
        v-else-if="loadError || !draftSchema"
        status="error"
        title="业务流程无法打开"
        :description="loadError || '草稿不存在或无权访问'"
      >
        <template #footer>
          <n-button @click="returnToApplication">
            返回应用工作台
          </n-button>
        </template>
      </n-result>

      <BusinessProcessDesigner
        v-else
        :schema="draftSchema"
        :process-name="process.processName"
        :save-state="saveState"
        :save-error="saveError"
        :server-validation="serverValidation"
        :object-id="stringValue(process.subjectObjectId)"
        :object-name="subjectObject?.objectName || process.subjectObjectCode"
        :objects="applicationObjects"
        :fields="fields"
        :flow-models="flowModels"
        :form-assets="formAssets"
        :business-actions="businessActions"
        :message-templates="messageTemplates"
        :capabilities="capabilities"
        :sub-processes="subProcesses"
        :service-actors="serviceActors"
        @update:schema="handleSchemaUpdate"
        @save="handleSave"
        @validate="handleValidate"
        @dirty-change="handleDirtyChange"
        @refresh-flow-model="refreshFlowCatalog"
        @refresh-fields="refreshBusinessObjectFields"
        @edit-action="openActionEditor"
        @open-flow-designer="openFlowDesigner"
        @reload="loadDesigner"
      />
    </main>

    <!-- 画布内编辑 / 新建业务动作 -->
    <n-modal
      v-model:show="actionEditorVisible"
      preset="card"
      :title="actionEditorContext?.isNew ? '新建业务动作' : '编辑业务动作'"
      class="action-editor-modal"
      :style="{ width: 'min(960px, 94vw)' }"
      @after-leave="actionEditorContext = null"
    >
      <ObjectActionEditor
        :actions="editingAction"
        :fields="subjectDesigner?.fields || []"
        :relations="subjectDesigner?.relations || []"
        :objects="applicationObjects"
        :designer-summaries="subjectDesignerSummaries"
        :object-code="process?.subjectObjectCode || ''"
        :config-key="subjectDesigner?.configKey || ''"
        @save="handleActionSaved"
      />
    </n-modal>

    <!-- 审批流程设计器的唯一内嵌容器；节点配置只发出打开事件，不再自行叠加遮罩。 -->
    <n-modal
      v-model:show="flowDesignerVisible"
      :mask-closable="false"
      :close-on-esc="false"
      :auto-focus="false"
      class="embedded-flow-designer-modal"
    >
      <div class="embedded-flow-designer-shell">
        <FlowDesignPage
          v-if="flowDesignerVisible"
          embedded
          :model-id="flowDesignerModelId"
          :business-object-code="flowDesignerContext.businessObjectCode"
          :business-object-name="flowDesignerContext.businessObjectName"
          :application-id="flowDesignerContext.applicationId"
          :business-form-key="flowDesignerContext.businessFormKey"
          @close="handleFlowDesignerClose"
          @saved="handleFlowDesignerSaved"
          @deployed="handleFlowDesignerSaved"
        />
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.process-designer-page {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  overflow: hidden;
  background: var(--body-color, #f5f6f8);
}

.process-designer-page.process-designer-embedded {
  min-height: 0;
  height: 100%;
}

.process-page-header {
  display: grid;
  min-height: 52px;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
  padding: 6px 16px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  background: var(--card-color, #fff);
  grid-template-columns: auto minmax(0, 1fr) auto;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.back-button {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  border: 1px solid var(--border-color, #d1d5db);
  border-radius: 6px;
  color: var(--text-color-2, #334155);
  background: var(--card-color, #fff);
  cursor: pointer;
  font-size: 13px;
}

.back-button:hover {
  border-color: var(--primary-color, #2563eb);
  color: var(--primary-color, #2563eb);
}

.process-page-identity {
  display: grid;
  min-width: 0;
  align-items: baseline;
  column-gap: 8px;
  grid-template-columns: auto minmax(0, 1fr);
}

.process-page-identity strong {
  overflow: hidden;
  color: var(--text-color-1, #0f172a);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-page-identity code,
.process-page-identity span,
.process-page-boundary {
  color: var(--text-color-3, #64748b);
  font-size: 11px;
}

.process-page-identity span {
  grid-column: 1 / -1;
}

.process-page-boundary {
  max-width: 360px;
  padding-left: 14px;
  border-left: 1px solid var(--border-color, #e5e7eb);
  line-height: 1.5;
  text-align: right;
}
.process-page-boundary span {
  display: block;
  margin-bottom: 6px;
}

.process-page-main {
  min-height: 0;
  flex: 1;
  padding: 12px;
}

.process-page-main > :deep(.business-process-designer) {
  height: calc(100vh - 72px);
  min-height: 620px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.process-designer-embedded .process-page-main > :deep(.business-process-designer) {
  height: calc(100% - 10px);
  min-height: 0;
}

.page-loading {
  display: flex;
  min-height: calc(100vh - 90px);
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  color: var(--text-color-3, #64748b);
  font-size: 13px;
}

.process-designer-embedded .page-loading {
  min-height: 200px;
}

@media (max-width: 900px) {
  .process-page-header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .process-page-boundary {
    display: none;
  }
}

.embedded-flow-designer-modal {
  width: 98vw;
  max-width: 1920px;
  height: 96vh;
}

.embedded-flow-designer-shell {
  height: 96vh;
  overflow: hidden;
  border-radius: 8px;
}
</style>
