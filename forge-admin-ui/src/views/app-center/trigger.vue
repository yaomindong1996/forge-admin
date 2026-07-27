<template>
  <div class="center-shell" :class="{ 'trigger-embedded': embedded }">
    <header class="trigger-page-header">
      <div class="center-head-row">
        <NButton v-if="!embedded" text @click="router.push('/app-center/engines')">
          返回引擎中心
        </NButton>
        <NSpace :wrap="true">
          <NButton secondary @click="loadAll">
            刷新
          </NButton>
          <NButton type="primary" @click="openEditor(null)">
            新增触发器
          </NButton>
        </NSpace>
      </div>
      <div v-if="!embedded" class="trigger-title-row">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageDescription }}</p>
        </div>
        <div class="trigger-range">
          {{ pageRangeText }}
        </div>
      </div>
    </header>

    <section v-if="!embedded" class="trigger-stats">
      <div class="trigger-stat">
        <span>总触发器</span>
        <strong>{{ total }}</strong>
      </div>
      <div class="trigger-stat">
        <span>本页启用</span>
        <strong>{{ enabledCount }}</strong>
      </div>
      <div class="trigger-stat">
        <span>本页定时</span>
        <strong>{{ scheduledCount }}</strong>
      </div>
      <div class="trigger-stat">
        <span>当前对象</span>
        <strong>{{ currentObjectLabel }}</strong>
      </div>
    </section>

    <section class="trigger-panel">
      <div class="trigger-toolbar">
        <div v-if="!embedded">
          <h2>规则列表</h2>
          <p>筛选、启停和查看每条触发器的执行情况。</p>
        </div>
        <NSpace :wrap="true" align="center">
          <n-select
            v-if="!objectLocked"
            v-model:value="filterObjectCode"
            clearable
            filterable
            class="trigger-filter-select"
            placeholder="按业务单元筛选"
            :options="objectOptions"
            @update:value="handleFilterChange"
          />
          <n-select
            v-model:value="filterScenarioType"
            clearable
            class="trigger-filter-select"
            placeholder="按场景筛选"
            :options="scenarioTemplateOptions"
            @update:value="handleFilterChange"
          />
          <NButton :disabled="!hasActiveFilter" @click="resetFilters">
            重置
          </NButton>
        </NSpace>
      </div>

      <n-data-table
        :columns="columns"
        :data="triggers"
        :loading="loading"
        :pagination="false"
        :row-key="row => row.id"
        :row-class-name="rowClassName"
        :scroll-x="1040"
        size="small"
        striped
      />

      <div class="trigger-pagination">
        <span>{{ pageRangeText }}</span>
        <n-pagination
          :page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :item-count="total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </section>

    <!-- 触发器编辑弹窗 -->
    <n-modal v-model:show="editorVisible" preset="card" :title="editingTrigger ? '编辑触发器' : '新增触发器'" style="width: 680px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="触发器名称" path="triggerName">
          <n-input v-model:value="formData.triggerName" placeholder="例：商机创建时发起主流程" />
        </n-form-item>
        <n-form-item label="业务单元" path="objectCode">
          <n-select
            v-model:value="formData.objectCode"
            :options="objectOptions"
            :disabled="objectLocked"
            clearable
            filterable
            placeholder="选择业务单元"
            @update:value="handleObjectCodeChange"
          />
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
        <n-form-item v-if="isScheduledTrigger && formData.developerMode !== 1" label="定时参数">
          <div class="schedule-config-grid">
            <n-select
              :value="scheduleConfig.dueField"
              :options="fieldOptions"
              clearable
              filterable
              placeholder="到期字段"
              @update:value="value => updateScheduleField('dueField', value || '')"
            />
            <n-input-number
              :value="scheduleConfig.lookAheadDays"
              :min="0"
              :max="365"
              placeholder="提前天数"
              @update:value="value => updateScheduleField('lookAheadDays', value || 0)"
            />
            <n-input-number
              :value="scheduleConfig.lookBehindDays"
              :min="0"
              :max="30"
              placeholder="回看天数"
              @update:value="value => updateScheduleField('lookBehindDays', value || 0)"
            />
            <n-input-number
              :value="scheduleConfig.batchSize"
              :min="1"
              :max="200"
              placeholder="单批数量"
              @update:value="value => updateScheduleField('batchSize', value || 50)"
            />
            <n-input-number
              :value="scheduleConfig.minIntervalMinutes"
              :min="5"
              :max="1440"
              placeholder="最小间隔分钟"
              @update:value="value => updateScheduleField('minIntervalMinutes', value || 5)"
            />
          </div>
        </n-form-item>
        <n-form-item v-if="isScheduledTrigger && formData.developerMode !== 1" label="分层提醒">
          <div class="tier-rule-list">
            <div v-for="(rule, index) in scheduleConfig.tierRules" :key="rule.clientKey" class="tier-rule-row">
              <n-input
                :value="rule.ruleName"
                placeholder="规则名称"
                @update:value="value => updateTierRuleField(index, 'ruleName', value)"
              />
              <n-select
                :value="rule.metricField"
                :options="fieldOptions"
                clearable
                filterable
                placeholder="指标字段"
                @update:value="value => updateTierRuleField(index, 'metricField', value || '')"
              />
              <n-input-number
                :value="rule.minValue"
                placeholder="最小值"
                style="width: 100%"
                @update:value="value => updateTierRuleField(index, 'minValue', value)"
              />
              <n-input-number
                :value="rule.maxValue"
                placeholder="最大值"
                style="width: 100%"
                @update:value="value => updateTierRuleField(index, 'maxValue', value)"
              />
              <n-input-number
                :value="rule.lookAheadDays"
                :min="0"
                :max="365"
                placeholder="提前天数"
                style="width: 100%"
                @update:value="value => updateTierRuleField(index, 'lookAheadDays', value || 0)"
              />
              <n-select
                :value="rule.receiverRule"
                :options="receiverRuleOptions"
                clearable
                placeholder="接收人"
                @update:value="value => updateTierRuleField(index, 'receiverRule', value || '')"
              />
              <NButton quaternary circle size="small" @click="removeTierRule(index)">
                <template #icon>
                  <n-icon><TrashOutline /></n-icon>
                </template>
              </NButton>
            </div>
            <NButton dashed size="small" @click="addTierRule">
              添加分层规则
            </NButton>
          </div>
        </n-form-item>
        <n-form-item label="开发者模式">
          <NSwitch v-model:value="formData.developerMode" :checked-value="1" :unchecked-value="0" />
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
          :receiver-rule-options="receiverRuleOptions"
          :action-options="actionOptions"
        />

        <n-form-item label="描述">
          <n-input v-model:value="formData.triggerDesc" type="textarea" :rows="2" placeholder="触发器说明" />
        </n-form-item>
      </n-form>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="editorVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="submitting" :disabled="!formData.objectCode" @click="handleSubmit">
            保存
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 执行日志抽屉 -->
    <n-drawer v-model:show="logDrawerVisible" :width="640" placement="right">
      <n-drawer-content :title="`执行日志 - ${logTriggerName}`">
        <n-data-table
          :columns="logColumns"
          :data="logs"
          :loading="logLoading"
          :pagination="false"
          size="small"
        />
        <div class="log-pagination">
          <n-pagination
            :page="logPagination.pageNum"
            :page-size="logPagination.pageSize"
            :item-count="logPagination.total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="handleLogPageChange"
            @update:page-size="handleLogPageSizeChange"
          />
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { NButton, NPopconfirm, NSpace, NSwitch, NTag, useMessage } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessObjectActions,
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
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import TriggerActionConfigPanel from './components/TriggerActionConfigPanel.vue'
import TriggerConditionBuilder from './components/TriggerConditionBuilder.vue'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  lockObject: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()
const route = useRoute()
const message = useMessage()
const { dict } = useDict(
  'ai_business_trigger_receiver_rule',
  'ai_business_trigger_type',
  'ai_business_trigger_event_type',
  'ai_business_trigger_action_type',
  'ai_business_trigger_execute_status',
  'sys_enable_disable',
)
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
const businessObjects = ref([])
const scenarioTemplates = ref([])
const logDrawerVisible = ref(false)
const logTriggerName = ref('')
const logs = ref([])
const logLoading = ref(false)
const activeLogTriggerId = ref(null)
const logPagination = ref({ pageNum: 1, pageSize: 10, total: 0 })
const formRef = ref(null)
const formData = ref(initFormData())
const fieldOptions = ref([])
const actionOptions = ref([])
const objectFieldsCache = ref({})
const objectActionsCache = ref({})

const embedded = computed(() => props.embedded)
const fixedObjectCode = computed(() => String(props.objectCode || '').trim())
const objectLocked = computed(() => props.embedded && props.lockObject && Boolean(fixedObjectCode.value))
const pageTitle = computed(() => props.embedded ? '自动化触发器' : '触发器配置')
const pageDescription = computed(() => props.embedded
  ? '配置当前业务单元的自动化规则，支持记录事件、定时提醒、流程发起和消息推送。'
  : '基于业务事件的自动化规则：当记录创建、修改或状态变更时自动发起主流程、推送消息或更新字段。')
const enabledCount = computed(() => triggers.value.filter(t => t.status === 1).length)
const scheduledCount = computed(() => triggers.value.filter(isScheduleRow).length)
const currentObjectLabel = computed(() => {
  if (props.objectName && fixedObjectCode.value)
    return `${props.objectName}（${fixedObjectCode.value}）`
  if (!filterObjectCode.value)
    return '全部'
  return objectOptions.value.find(item => item.value === filterObjectCode.value)?.label || filterObjectCode.value
})
const hasActiveFilter = computed(() => Boolean((!objectLocked.value && filterObjectCode.value) || filterScenarioType.value))
const pageRangeText = computed(() => {
  if (!total.value)
    return '暂无数据'
  const start = (pagination.value.pageNum - 1) * pagination.value.pageSize + 1
  const end = Math.min(total.value, pagination.value.pageNum * pagination.value.pageSize)
  return `${start}-${end} / ${total.value}`
})
const isScheduledTrigger = computed(() => {
  const triggerType = String(formData.value.triggerType || '').toUpperCase()
  return ['SCHEDULE', 'SCHEDULED'].includes(triggerType) || formData.value.eventType === 'SCHEDULED_DUE'
})
const scheduleConfig = computed(() => readScheduleConfig())
const scenarioTemplateOptions = computed(() => scenarioTemplates.value.map(item => ({
  label: item.scenarioName || item.scenarioType,
  value: item.scenarioType,
})))

const formRules = {
  triggerName: { required: true, message: '请输入触发器名称' },
  objectCode: { required: true, message: '请选择业务单元' },
  triggerType: { required: true, message: '请选择触发类型' },
  eventType: { required: true, message: '请选择事件类型' },
  actionType: { required: true, message: '请选择动作类型' },
}

const receiverRuleOptions = computed(() => dict.value.ai_business_trigger_receiver_rule || [])
const triggerTypeOptions = computed(() => dict.value.ai_business_trigger_type || [])
const eventTypeOptions = computed(() => dict.value.ai_business_trigger_event_type || [])
const actionTypeOptions = computed(() => dict.value.ai_business_trigger_action_type || [])
const executeStatusOptions = computed(() => dict.value.ai_business_trigger_execute_status || [])
const statusOptions = computed(() => dict.value.sys_enable_disable || [])

const columns = [
  {
    title: '触发器',
    key: 'triggerName',
    minWidth: 230,
    render: row => h('div', { class: 'trigger-name-cell' }, [
      h('strong', row.triggerName || '-'),
      row.triggerDesc ? h('span', row.triggerDesc) : null,
    ]),
  },
  {
    title: '业务单元',
    key: 'objectCode',
    width: 140,
    render: row => h(NTag, { size: 'small', bordered: false }, { default: () => row.objectCode || '-' }),
  },
  {
    title: '触发方式',
    key: 'triggerType',
    width: 110,
    render: row => h(DictTag, { options: triggerTypeOptions.value, value: normalizeTriggerType(row.triggerType), size: 'small', bordered: false }),
  },
  { title: '事件', key: 'eventType', width: 140, render: row => h(DictTag, { options: eventTypeOptions.value, value: row.eventType, size: 'small', bordered: false }) },
  { title: '动作', key: 'actionType', width: 120, render: row => h(DictTag, { options: actionTypeOptions.value, value: row.actionType, size: 'small', bordered: false }) },
  { title: '执行次数', key: 'executeCount', width: 90, render: row => row.executeCount || 0 },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: row => h(NSpace, { align: 'center', size: 8 }, {
      default: () => [
        h(NSwitch, {
          'value': row.status === 1,
          'size': 'small',
          'onUpdate:value': () => toggleStatus(row),
        }),
        h(DictTag, {
          options: statusOptions.value,
          value: row.status,
          size: 'small',
          bordered: false,
          forceTag: true,
        }),
      ],
    }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 190,
    fixed: 'right',
    render: row => h(NSpace, { size: 10 }, {
      default: () => [
        h(NButton, { text: true, type: 'primary', size: 'small', onClick: () => openEditor(row) }, { default: () => '编辑' }),
        h(NButton, { text: true, type: 'info', size: 'small', onClick: () => openLogs(row) }, { default: () => '日志' }),
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row) }, {
          trigger: () => h(NButton, { text: true, type: 'error', size: 'small' }, { default: () => '删除' }),
          default: () => `确定删除“${row.triggerName || '该触发器'}”吗？`,
        }),
      ],
    }),
  },
]

const logColumns = [
  { title: '事件', key: 'eventType', width: 110, render: row => h(DictTag, { options: eventTypeOptions.value, value: row.eventType, size: 'small', bordered: false }) },
  { title: '记录ID', key: 'recordId', width: 100 },
  {
    title: '状态',
    key: 'executeStatus',
    width: 90,
    render: row => h(DictTag, { options: executeStatusOptions.value, value: row.executeStatus, size: 'small', bordered: false }),
  },
  { title: '耗时(ms)', key: 'durationMs', width: 80 },
  { title: '错误', key: 'errorMessage', ellipsis: { tooltip: true } },
  { title: '时间', key: 'executeTime', width: 160 },
]

onMounted(loadAll)

async function loadAll() {
  await loadObjects()
  applyRouteObjectContext()
  await loadScenarioTemplates()
  await loadTriggers()
}

async function loadObjects() {
  try {
    const res = await businessObjectList({})
    businessObjects.value = res.data || []
    objectOptions.value = businessObjects.value.map(item => ({
      label: item.objectName || item.objectCode,
      value: item.objectCode,
      objectId: item.id,
      suiteCode: item.suiteCode,
    }))
  }
  catch {
    businessObjects.value = []
    objectOptions.value = []
  }
}

function applyRouteObjectContext() {
  if (fixedObjectCode.value) {
    filterObjectCode.value = fixedObjectCode.value
    return
  }
  if (props.embedded)
    return
  const queryObjectCode = normalizeQueryValue(route.query.objectCode)
  if (!queryObjectCode)
    return
  filterObjectCode.value = queryObjectCode
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
  catch (e) {
    message.error(e.message || '加载触发器失败')
    triggers.value = []
    total.value = 0
  }
  finally {
    loading.value = false
  }
}

function handleFilterChange() {
  pagination.value.pageNum = 1
  loadTriggers()
}

function resetFilters() {
  filterObjectCode.value = objectLocked.value ? fixedObjectCode.value : null
  filterScenarioType.value = null
  handleFilterChange()
}

function handlePageChange(page) {
  pagination.value.pageNum = page
  loadTriggers()
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
    formData.value = initFormData(resolveDefaultObjectCode())
  }
  editorVisible.value = true
  if (formData.value.objectCode) {
    loadFieldOptions(formData.value.objectCode)
    loadActionOptions(formData.value.objectCode)
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (!formData.value.objectCode) {
    message.warning('请先选择业务单元')
    return
  }
  if (isScheduledTrigger.value && !scheduleConfig.value.dueField) {
    message.warning('请选择定时触发的到期字段')
    return
  }
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
    await loadTriggers()
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
  await loadTriggers()
}

async function handleDelete(row) {
  await deleteBusinessTrigger(row.id)
  message.success('已删除')
  await reloadAfterDelete()
}

async function openLogs(row) {
  activeLogTriggerId.value = row.id
  logTriggerName.value = row.triggerName
  logDrawerVisible.value = true
  logPagination.value.pageNum = 1
  await loadLogs()
}

async function loadLogs() {
  if (!activeLogTriggerId.value)
    return
  logLoading.value = true
  try {
    const res = await businessTriggerLogs({
      triggerId: activeLogTriggerId.value,
      pageNum: logPagination.value.pageNum,
      pageSize: logPagination.value.pageSize,
    })
    logs.value = res.data?.records || []
    logPagination.value.total = Number(res.data?.total || 0)
  }
  finally {
    logLoading.value = false
  }
}

function handleLogPageChange(page) {
  logPagination.value.pageNum = page
  loadLogs()
}

function handleLogPageSizeChange(size) {
  logPagination.value.pageSize = size
  logPagination.value.pageNum = 1
  loadLogs()
}

function isScheduleRow(row) {
  const triggerType = normalizeTriggerType(row?.triggerType)
  return triggerType === 'SCHEDULE' || row?.eventType === 'SCHEDULED_DUE'
}

function rowClassName(row) {
  return row.status === 1 ? '' : 'trigger-row-disabled'
}

async function reloadAfterDelete() {
  await loadTriggers()
  if (triggers.value.length === 0 && pagination.value.pageNum > 1) {
    pagination.value.pageNum -= 1
    await loadTriggers()
  }
}

function initFormData(defaultObjectCode = null) {
  return {
    triggerName: '',
    objectCode: defaultObjectCode,
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

async function loadFieldOptions(objectCode) {
  if (objectFieldsCache.value[objectCode]) {
    fieldOptions.value = objectFieldsCache.value[objectCode]
    return
  }
  try {
    const obj = businessObjects.value.find(o => o.objectCode === objectCode)
      || (await businessObjectList({ objectCode })).data?.find(o => o.objectCode === objectCode)
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

async function loadActionOptions(objectCode) {
  if (objectActionsCache.value[objectCode]) {
    actionOptions.value = objectActionsCache.value[objectCode]
    return
  }
  try {
    const obj = businessObjects.value.find(o => o.objectCode === objectCode)
      || (await businessObjectList({ objectCode })).data?.find(o => o.objectCode === objectCode)
    if (!obj)
      return
    const res = await businessObjectActions(obj.id)
    const options = (res.data || [])
      .filter(action => action && action.status !== 0 && isExecutableAction(action))
      .map(action => ({
        label: `${action.actionName || action.actionCode}（${action.actionCode}）`,
        value: action.actionCode,
      }))
      .filter(item => item.value)
    objectActionsCache.value[objectCode] = options
    actionOptions.value = options
  }
  catch {
    actionOptions.value = []
  }
}

function handleObjectCodeChange(value) {
  if (objectLocked.value) {
    formData.value.objectCode = fixedObjectCode.value
    return
  }
  formData.value.objectCode = value || null
  fieldOptions.value = []
  actionOptions.value = []
  formData.value.eventCondition = ''
  formData.value.actionConfig = ''
  if (value) {
    loadFieldOptions(value)
    loadActionOptions(value)
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
  formData.value.triggerType = template.eventType === 'SCHEDULED_DUE' ? 'SCHEDULE' : 'EVENT'
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
  else if (template.actionType === 'START_FLOW') {
    formData.value.actionConfig = JSON.stringify({
      useMainFlow: true,
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
    triggerType: normalizeTriggerType(trigger.triggerType),
    developerMode: Number(trigger.developerMode || 0),
    actionConfig: stringifyJson(trigger.actionConfig),
    eventCondition: stringifyJson(trigger.eventCondition),
  }
}

function normalizeSubmitPayload(value = {}) {
  const object = businessObjects.value.find(item => item.objectCode === value.objectCode)
  return {
    ...value,
    suiteCode: value.suiteCode || object?.suiteCode || '',
    triggerType: normalizeTriggerType(value.triggerType),
    developerMode: Number(value.developerMode || 0),
    actionConfig: value.actionConfig || '{}',
    eventCondition: value.eventCondition || '',
  }
}

function normalizeTriggerType(value) {
  const type = String(value || 'EVENT').toUpperCase()
  return type === 'SCHEDULED' ? 'SCHEDULE' : type
}

function isExecutableAction(action = {}) {
  const actionConfig = action.actionConfig || {}
  return String(action.actionType || '').toUpperCase() === 'COMMAND'
    || Array.isArray(actionConfig.steps)
    || Array.isArray(actionConfig.stepList)
}

function readScheduleConfig() {
  const condition = safeParseObject(formData.value.eventCondition)
  const schedule = condition.schedule && typeof condition.schedule === 'object'
    ? condition.schedule
    : {}
  return {
    dueField: schedule.dueField || '',
    lookAheadDays: Number(schedule.lookAheadDays || 0),
    lookBehindDays: Number(schedule.lookBehindDays || 0),
    batchSize: Number(schedule.batchSize || 50),
    minIntervalMinutes: Math.max(Number(schedule.minIntervalMinutes || 5), 5),
    tierRules: normalizeTierRules(schedule.tierRules || schedule.reminderRules || []),
  }
}

function updateScheduleField(key, value) {
  updateScheduleConfig((schedule) => {
    schedule[key] = value
  })
}

function updateScheduleConfig(updater) {
  const condition = safeParseObject(formData.value.eventCondition)
  const schedule = condition.schedule && typeof condition.schedule === 'object'
    ? { ...condition.schedule }
    : {}
  updater(schedule)
  condition.schedule = schedule
  formData.value.eventCondition = JSON.stringify(condition)
}

function addTierRule() {
  updateScheduleConfig((schedule) => {
    const tierRules = normalizeTierRules(schedule.tierRules || [])
    tierRules.push({
      clientKey: `tier_${Date.now()}`,
      ruleCode: `tier_${Date.now()}`,
      ruleName: '分层提醒',
      metricField: '',
      minValue: null,
      maxValue: null,
      lookAheadDays: Number(schedule.lookAheadDays || 0),
      receiverRule: '',
    })
    schedule.tierRules = tierRules.map(toTierRulePayload)
  })
}

function updateTierRuleField(index, key, value) {
  updateScheduleConfig((schedule) => {
    const tierRules = normalizeTierRules(schedule.tierRules || [])
    if (!tierRules[index])
      return
    tierRules[index] = {
      ...tierRules[index],
      [key]: value,
    }
    schedule.tierRules = tierRules.map(toTierRulePayload)
  })
}

function removeTierRule(index) {
  updateScheduleConfig((schedule) => {
    const tierRules = normalizeTierRules(schedule.tierRules || [])
    tierRules.splice(index, 1)
    schedule.tierRules = tierRules.map(toTierRulePayload)
  })
}

function normalizeTierRules(list = []) {
  if (!Array.isArray(list))
    return []
  return list.map((item, index) => {
    const ruleCode = String(item.ruleCode || `tier_${index + 1}`).trim()
    return {
      clientKey: ruleCode || `tier_${index + 1}`,
      ruleCode,
      ruleName: String(item.ruleName || ruleCode || '分层提醒').trim(),
      metricField: item.metricField || '',
      minValue: item.minValue === undefined || item.minValue === null || item.minValue === '' ? null : Number(item.minValue),
      maxValue: item.maxValue === undefined || item.maxValue === null || item.maxValue === '' ? null : Number(item.maxValue),
      lookAheadDays: Number(item.lookAheadDays || 0),
      receiverRule: item.receiverRule || '',
    }
  })
}

function toTierRulePayload(rule = {}) {
  return {
    ruleCode: rule.ruleCode,
    ruleName: rule.ruleName,
    metricField: rule.metricField || '',
    minValue: rule.minValue,
    maxValue: rule.maxValue,
    lookAheadDays: Number(rule.lookAheadDays || 0),
    receiverRule: rule.receiverRule || '',
  }
}

function safeParseObject(value) {
  if (!value)
    return {}
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
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
  if (val) {
    loadFieldOptions(val)
    loadActionOptions(val)
  }
  else {
    fieldOptions.value = []
    actionOptions.value = []
  }
})

watch(() => route.query.objectCode, () => {
  if (props.embedded)
    return
  applyRouteObjectContext()
  handleFilterChange()
})

watch(fixedObjectCode, () => {
  if (!props.embedded)
    return
  applyRouteObjectContext()
  handleFilterChange()
})

function resolveDefaultObjectCode() {
  return fixedObjectCode.value || normalizeQueryValue(route.query.objectCode) || filterObjectCode.value || null
}

function normalizeQueryValue(value) {
  return Array.isArray(value) ? value[0] : value
}
</script>

<style scoped>
@import './shared-center.css';

.trigger-page-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trigger-embedded {
  min-height: 100%;
  padding: 0;
}

.trigger-embedded .trigger-page-header {
  border-bottom: 1px solid #e5e9f2;
  background: #fff;
  padding: 10px 16px;
}

.trigger-embedded .center-head-row {
  align-items: center;
}

.trigger-embedded .trigger-panel {
  border: 0;
  border-radius: 0;
}

.trigger-embedded .trigger-toolbar {
  justify-content: flex-end;
  padding: 10px 16px;
}

.trigger-title-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.trigger-range {
  min-width: 118px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  line-height: 30px;
  text-align: center;
}

.trigger-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.trigger-stat {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px;
}

.trigger-stat span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.trigger-stat strong {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  color: #111827;
  font-size: 20px;
  font-weight: 650;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trigger-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.trigger-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #edf0f5;
}

.trigger-toolbar h2 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 650;
}

.trigger-toolbar p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.trigger-filter-select {
  width: 220px;
}

.trigger-name-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.trigger-name-cell strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trigger-name-cell span {
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

:deep(.trigger-row-disabled td) {
  color: #94a3b8;
}

:deep(.n-data-table .n-data-table-th) {
  font-weight: 650;
}

.trigger-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid #edf0f5;
  color: #64748b;
  font-size: 12px;
}

.log-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.schedule-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(160px, 1fr));
  gap: 8px;
  width: 100%;
}

.tier-rule-list {
  display: grid;
  gap: 8px;
  width: 100%;
}

.tier-rule-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(140px, 1.2fr) 96px 96px 96px minmax(120px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

@media (max-width: 720px) {
  .trigger-title-row,
  .trigger-toolbar,
  .trigger-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .trigger-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trigger-filter-select {
    width: 100%;
  }

  .schedule-config-grid {
    grid-template-columns: 1fr;
  }

  .tier-rule-row {
    grid-template-columns: 1fr;
  }
}
</style>
