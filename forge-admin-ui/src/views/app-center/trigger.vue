<template>
  <div class="center-shell">
    <header>
      <div class="center-head-row">
        <n-button text @click="router.push('/app-center/engines')">
          返回引擎中心
        </n-button>
        <n-space :wrap="true">
          <n-button secondary @click="loadAll">
            刷新
          </n-button>
          <n-button type="primary" @click="openEditor(null)">
            新增触发器
          </n-button>
        </n-space>
      </div>
      <h1>触发器配置</h1>
      <p>基于业务事件的自动化规则：当记录创建、修改或状态变更时自动发起流程、推送消息或更新字段。</p>
    </header>

    <section class="trigger-filters">
      <n-space :wrap="true" align="center">
        <n-select
          v-model:value="filterObjectCode"
          clearable
          style="width: 240px"
          placeholder="按业务对象筛选"
          :options="objectOptions"
          @update:value="loadTriggers"
        />
        <n-select
          v-model:value="filterScenarioType"
          clearable
          style="width: 240px"
          placeholder="按场景筛选"
          :options="scenarioTemplateOptions"
          @update:value="loadTriggers"
        />
        <n-tag v-if="total > 0" :bordered="false">
          {{ total }} 条触发器 · {{ enabledCount }} 条启用
        </n-tag>
      </n-space>
    </section>

    <n-spin :show="loading">
      <n-data-table
        :columns="columns"
        :data="triggers"
        :pagination="false"
        :row-key="row => row.id"
        striped
      />
    </n-spin>

    <div v-if="total > pagination.pageSize" class="trigger-pagination">
      <n-pagination
        v-model:page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :item-count="total"
        :page-sizes="[10, 20, 50]"
        show-size-picker
        @update:page="loadTriggers"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <!-- 触发器编辑弹窗 -->
    <n-modal v-model:show="editorVisible" preset="card" :title="editingTrigger ? '编辑触发器' : '新增触发器'" style="width: 680px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="触发器名称" path="triggerName">
          <n-input v-model:value="formData.triggerName" placeholder="例：商机创建时发起流程" />
        </n-form-item>
        <n-form-item label="业务对象" path="objectCode">
          <n-select v-model:value="formData.objectCode" :options="objectOptions" placeholder="选择业务对象" />
        </n-form-item>
        <n-form-item label="场景模板">
          <n-select
            v-model:value="formData.scenarioType"
            :options="scenarioTemplateOptions"
            clearable
            placeholder="选择业务场景"
            @update:value="applyScenarioTemplate"
          />
        </n-form-item>
        <n-form-item label="触发类型" path="triggerType">
          <n-select v-model:value="formData.triggerType" :options="triggerTypeOptions" placeholder="选择触发类型" />
        </n-form-item>
        <n-form-item label="事件类型" path="eventType">
          <n-select v-model:value="formData.eventType" :options="eventTypeOptions" placeholder="选择事件类型" />
        </n-form-item>
        <n-form-item label="开发者模式">
          <n-switch v-model:value="formData.developerMode" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="触发条件">
          <n-input
            v-if="formData.developerMode === 1"
            v-model:value="formData.eventCondition"
            type="textarea"
            :rows="3"
            placeholder="JSON 条件表达式"
          />
          <TriggerConditionBuilder
            v-else
            v-model="formData.eventCondition"
            :field-options="fieldOptions"
          />
        </n-form-item>
        <n-form-item label="动作类型" path="actionType">
          <n-select v-model:value="formData.actionType" :options="actionTypeOptions" placeholder="选择动作类型" @update:value="handleActionTypeChange" />
        </n-form-item>

        <n-form-item v-if="formData.developerMode === 1 && formData.actionType" label="动作配置">
          <n-input
            v-model:value="formData.actionConfig"
            type="textarea"
            :rows="4"
            placeholder="JSON 动作配置"
          />
        </n-form-item>
        <TriggerActionConfigPanel
          v-else
          v-model="formData.actionConfig"
          :action-type="formData.actionType"
          :field-options="fieldOptions"
          :object-options="objectOptions"
          :flow-model-options="flowModelOptions"
          :receiver-rule-options="receiverRuleOptions"
        />

        <n-form-item label="描述">
          <n-input v-model:value="formData.triggerDesc" type="textarea" :rows="2" placeholder="触发器说明" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="editorVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">
            保存
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 执行日志抽屉 -->
    <n-drawer v-model:show="logDrawerVisible" :width="640" placement="right">
      <n-drawer-content :title="`执行日志 - ${logTriggerName}`">
        <n-data-table :columns="logColumns" :data="logs" :pagination="false" size="small" />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  businessObjectFields,
  businessObjectList,
  businessTriggerLogs,
  businessTriggerPage,
  businessTriggerScenarioTemplates,
  createBusinessTrigger,
  deleteBusinessTrigger,
  updateBusinessTrigger,
  updateBusinessTriggerStatus,
} from '@/api/business-app'
import flowApi from '@/api/flow'
import TriggerActionConfigPanel from './components/TriggerActionConfigPanel.vue'
import TriggerConditionBuilder from './components/TriggerConditionBuilder.vue'

const router = useRouter()
const message = useMessage()
const triggers = ref([])
const loading = ref(false)
const submitting = ref(false)
const editorVisible = ref(false)
const editingTrigger = ref(null)
const filterObjectCode = ref(null)
const filterScenarioType = ref(null)
const total = ref(0)
const pagination = ref({ pageNum: 1, pageSize: 10 })
const objectOptions = ref([])
const scenarioTemplates = ref([])
const logDrawerVisible = ref(false)
const logTriggerName = ref('')
const logs = ref([])
const formRef = ref(null)

const enabledCount = computed(() => triggers.value.filter(t => t.status === 1).length)
const scenarioTemplateOptions = computed(() => scenarioTemplates.value.map(item => ({
  label: item.scenarioName || item.scenarioType,
  value: item.scenarioType,
})))

const formData = ref(initFormData())
const flowModelOptions = ref([])
const flowModelsLoading = ref(false)
const fieldOptions = ref([])
const objectFieldsCache = ref({})

const formRules = {
  triggerName: { required: true, message: '请输入触发器名称' },
  objectCode: { required: true, message: '请选择业务对象' },
  triggerType: { required: true, message: '请选择触发类型' },
  eventType: { required: true, message: '请选择事件类型' },
  actionType: { required: true, message: '请选择动作类型' },
}

const receiverRuleOptions = [
  { label: '记录创建人', value: 'CREATOR' },
  { label: '记录负责人', value: 'OWNER' },
  { label: '指定用户', value: 'USERS' },
  { label: '全部用户', value: 'ALL' },
]

const triggerTypeOptions = [
  { label: '事件触发', value: 'EVENT' },
  { label: '定时触发', value: 'SCHEDULED' },
]
const eventTypeOptions = [
  { label: '记录创建', value: 'RECORD_CREATED' },
  { label: '记录更新', value: 'RECORD_UPDATED' },
  { label: '记录删除', value: 'RECORD_DELETED' },
  { label: '状态变更', value: 'STATUS_CHANGED' },
  { label: '字段变更', value: 'FIELD_CHANGED' },
  { label: '流程通过', value: 'FLOW_APPROVED' },
  { label: '流程驳回', value: 'FLOW_REJECTED' },
  { label: '到期提醒', value: 'SCHEDULED_DUE' },
]
const actionTypeOptions = [
  { label: '发起流程', value: 'START_FLOW' },
  { label: '发送消息', value: 'SEND_MESSAGE' },
  { label: '创建记录', value: 'CREATE_RECORD' },
  { label: '更新字段', value: 'UPDATE_FIELD' },
  { label: 'Webhook', value: 'WEBHOOK' },
]

const columns = [
  { title: '名称', key: 'triggerName', width: 180 },
  { title: '业务对象', key: 'objectCode', width: 120 },
  { title: '事件', key: 'eventType', width: 120, render: row => eventTypeLabel(row.eventType) },
  { title: '动作', key: 'actionType', width: 100, render: row => actionTypeLabel(row.actionType) },
  { title: '执行次数', key: 'executeCount', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row => h('a', {
      class: row.status === 1 ? 'text-success cursor-pointer' : 'text-warning cursor-pointer',
      onClick: () => toggleStatus(row),
    }, row.status === 1 ? '启用' : '禁用'),
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: row => h('div', { class: 'flex gap-3' }, [
      h('a', { class: 'text-primary cursor-pointer', onClick: () => openEditor(row) }, '编辑'),
      h('a', { class: 'text-info cursor-pointer', onClick: () => openLogs(row) }, '日志'),
      h('a', { class: 'text-error cursor-pointer', onClick: () => handleDelete(row) }, '删除'),
    ]),
  },
]

const logColumns = [
  { title: '事件', key: 'eventType', width: 100 },
  { title: '记录ID', key: 'recordId', width: 100 },
  { title: '状态', key: 'executeStatus', width: 80 },
  { title: '耗时(ms)', key: 'durationMs', width: 80 },
  { title: '错误', key: 'errorMessage', ellipsis: { tooltip: true } },
  { title: '时间', key: 'executeTime', width: 160 },
]

onMounted(loadAll)

async function loadAll() {
  await Promise.all([loadObjects(), loadScenarioTemplates(), loadFlowModels(), loadTriggers()])
}

async function loadObjects() {
  try {
    const res = await businessObjectList({})
    objectOptions.value = (res.data || []).map(item => ({
      label: item.objectName || item.objectCode,
      value: item.objectCode,
    }))
  }
  catch {
    objectOptions.value = []
  }
}

async function loadScenarioTemplates() {
  try {
    const res = await businessTriggerScenarioTemplates()
    scenarioTemplates.value = res.data || []
  }
  catch {
    scenarioTemplates.value = []
  }
}

async function loadTriggers() {
  loading.value = true
  try {
    const res = await businessTriggerPage({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      objectCode: filterObjectCode.value,
      scenarioType: filterScenarioType.value,
    })
    triggers.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  }
  finally {
    loading.value = false
  }
}

function handlePageSizeChange(size) {
  pagination.value.pageSize = size
  pagination.value.pageNum = 1
  loadTriggers()
}

function openEditor(trigger) {
  editingTrigger.value = trigger
  if (trigger) {
    formData.value = normalizeFormData(trigger)
  }
  else {
    formData.value = initFormData()
  }
  editorVisible.value = true
  loadFlowModels()
  if (formData.value.objectCode) {
    loadFieldOptions(formData.value.objectCode)
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = normalizeSubmitPayload(formData.value)
    if (editingTrigger.value) {
      await updateBusinessTrigger(payload)
      message.success('触发器已更新')
    }
    else {
      await createBusinessTrigger(payload)
      message.success('触发器已创建')
    }
    editorVisible.value = false
    loadTriggers()
  }
  catch (e) {
    message.error(e.message || '操作失败')
  }
  finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateBusinessTriggerStatus(row.id, newStatus)
  message.success(newStatus === 1 ? '已启用' : '已禁用')
  loadTriggers()
}

async function handleDelete(row) {
  await deleteBusinessTrigger(row.id)
  message.success('已删除')
  loadTriggers()
}

async function openLogs(row) {
  logTriggerName.value = row.triggerName
  logDrawerVisible.value = true
  const res = await businessTriggerLogs({ triggerId: row.id, pageNum: 1, pageSize: 50 })
  logs.value = res.data?.records || []
}

function eventTypeLabel(type) {
  return eventTypeOptions.find(o => o.value === type)?.label || type
}

function actionTypeLabel(type) {
  return actionTypeOptions.find(o => o.value === type)?.label || type
}

function initFormData() {
  return {
    triggerName: '',
    objectCode: null,
    triggerType: 'EVENT',
    scenarioType: null,
    blockingMode: 'ASYNC',
    developerMode: 0,
    eventType: null,
    eventCondition: '',
    actionType: null,
    actionConfig: '',
    triggerDesc: '',
  }
}

async function loadFlowModels() {
  flowModelsLoading.value = true
  try {
    const res = await flowApi.getModelList({ status: 1 })
    flowModelOptions.value = (res.data || []).map(m => ({
      label: `${m.modelName || m.name}（${m.modelKey || m.key}）`,
      value: m.modelKey || m.key,
    }))
  }
  catch {
    flowModelOptions.value = []
  }
  finally {
    flowModelsLoading.value = false
  }
}

async function loadFieldOptions(objectCode) {
  if (objectFieldsCache.value[objectCode]) {
    fieldOptions.value = objectFieldsCache.value[objectCode]
    return
  }
  try {
    // 通过 objectOptions 找到 objectId
    const obj = (await businessObjectList({ objectCode })).data?.find(o => o.objectCode === objectCode)
    if (!obj)
      return
    const res = await businessObjectFields(obj.id)
    const options = (res.data || []).map(f => ({
      label: `${f.fieldName}（${f.fieldCode}）`,
      value: f.fieldCode,
    }))
    objectFieldsCache.value[objectCode] = options
    fieldOptions.value = options
  }
  catch {
    fieldOptions.value = []
  }
}

function handleActionTypeChange() {
  formData.value.actionConfig = ''
}

function applyScenarioTemplate(scenarioType) {
  const template = scenarioTemplates.value.find(item => item.scenarioType === scenarioType)
  if (!template)
    return
  formData.value.eventType = template.eventType || formData.value.eventType
  formData.value.actionType = template.actionType || formData.value.actionType
  formData.value.triggerType = template.eventType === 'SCHEDULED_DUE' ? 'SCHEDULED' : 'EVENT'
  if (!formData.value.triggerName)
    formData.value.triggerName = template.scenarioName || ''
  if (!formData.value.triggerDesc)
    formData.value.triggerDesc = template.description || ''
  if (template.actionType === 'SEND_MESSAGE') {
    formData.value.actionConfig = JSON.stringify({
      receiverRule: template.receiverRule || '',
      templateCode: '',
      receiverIds: '',
    })
  }
  else {
    formData.value.actionConfig = ''
  }
}

function normalizeFormData(trigger = {}) {
  return {
    ...initFormData(),
    ...trigger,
    developerMode: Number(trigger.developerMode || 0),
    actionConfig: stringifyJson(trigger.actionConfig),
    eventCondition: stringifyJson(trigger.eventCondition),
  }
}

function normalizeSubmitPayload(value = {}) {
  return {
    ...value,
    developerMode: Number(value.developerMode || 0),
    actionConfig: value.actionConfig || '{}',
    eventCondition: value.eventCondition || '',
  }
}

function stringifyJson(value) {
  if (!value)
    return ''
  if (typeof value === 'string')
    return value
  return JSON.stringify(value)
}

// 监听 objectCode 变化加载字段
watch(() => formData.value.objectCode, (val) => {
  if (val)
    loadFieldOptions(val)
  else
    fieldOptions.value = []
})
</script>

<style scoped>
@import './shared-center.css';

.trigger-filters {
  margin-bottom: 16px;
}
.trigger-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
