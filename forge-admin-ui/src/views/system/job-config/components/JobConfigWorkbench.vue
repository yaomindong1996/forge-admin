<template>
  <div class="job-workbench" :style="pageThemeStyle">
    <div class="workbench-toolbar">
      <div class="header-identity">
        <NTooltip>
          <template #trigger>
            <NButton quaternary circle aria-label="返回任务列表" @click="handleCancel">
              <template #icon>
                <i class="i-material-symbols:arrow-back-rounded" />
              </template>
            </NButton>
          </template>
          返回任务列表
        </NTooltip>
      </div>

      <div class="header-operations">
        <NTag size="small" :type="saveStateType" :bordered="false">
          {{ saveStateText }}
        </NTag>
        <NButton class="desktop-action" :disabled="saving" @click="handleCancel">
          取消
        </NButton>
        <NButton
          v-if="canSave"
          class="desktop-action"
          type="primary"
          :loading="saving"
          :disabled="loading || partialSaved"
          @click="handleSave"
        >
          <template #icon>
            <i class="i-material-symbols:save-outline-rounded" />
          </template>
          {{ saveActionLabel }}
        </NButton>
      </div>
    </div>

    <div v-if="loading" class="workbench-loading">
      <NSpin size="large" />
      <span>正在加载任务配置</span>
    </div>

    <div v-else class="workbench-grid">
      <nav class="section-nav" aria-label="任务配置分区">
        <button
          v-for="(item, index) in sections"
          :key="item.key"
          type="button"
          :class="{ active: activeSection === item.key }"
          @click="scrollToSection(item.key)"
        >
          <span class="nav-step">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="nav-copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
        </button>
      </nav>

      <main ref="formScrollRef" class="form-column">
        <NAlert v-if="loadError" type="error" :show-icon="true" class="page-alert">
          {{ loadError }}
        </NAlert>
        <NAlert v-if="partialSaved" type="warning" :show-icon="true" class="page-alert">
          <template #header>
            配置已保存，调度同步失败
          </template>
          请返回任务列表查看失败原因并执行“重新同步”。
          <template #action>
            <NButton size="small" type="warning" @click="returnAfterPartialSave">
              返回任务列表
            </NButton>
          </template>
        </NAlert>
        <NAlert v-if="completedSchedule" type="info" :show-icon="true" class="page-alert">
          <template #header>
            该一次性计划已结束
          </template>
          修改执行时间后保存即可重新计划；为避免误触发，保存后状态默认设为停用。
        </NAlert>

        <NForm
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-placement="top"
          require-mark-placement="right-hanging"
        >
          <section v-show="activeSection === 'basic'" id="basic" class="form-section">
            <div class="section-heading">
              <i class="i-material-symbols:badge-outline-rounded" />
              <div>
                <h3>基本信息</h3>
                <p>任务名称、分组和业务用途</p>
              </div>
            </div>
            <JobBasicSection
              :form="form"
              :editing="editing"
              :group-options="groupOptions"
              @update-field="handleUpdateField"
            />
          </section>

          <section v-show="activeSection === 'execution'" id="execution" class="form-section">
            <div class="section-heading">
              <i class="i-material-symbols:deployed-code-outline-rounded" />
              <div>
                <h3>执行内容</h3>
                <p>{{ form.invokeMode === JOB_INVOKE_FLOW ? '绑定已发布流程模型与固定版本' : '选择任务实际执行的处理器或服务' }}</p>
              </div>
            </div>
            <JobExecutionSection
              :form="form"
              :executors="executors"
              :loading="executorLoading"
              :invoke-mode-options="invokeModeOptions"
              :flow-model-options="flowModelOptions"
              :flow-version-options="flowVersionOptions"
              :flow-model-loading="flowModelLoading"
              :flow-version-loading="flowVersionLoading"
              @update-field="handleUpdateField"
              @invoke-mode-change="handleInvokeModeChange"
              @flow-model-change="handleFlowModelChange"
              @flow-version-change="handleFlowVersionChange"
            />
          </section>

          <section v-show="activeSection === 'schedule'" id="schedule" class="form-section">
            <div class="section-heading">
              <i class="i-material-symbols:event-repeat-outline-rounded" />
              <div>
                <h3>执行计划</h3>
                <p>选择周期或单次执行，并核对任务时区</p>
              </div>
            </div>
            <JobScheduleSection
              :form="form"
              :schedule-type-options="scheduleTypeOptions"
              :timezone-options="timezoneOptions"
              :timezone-loading="timezoneLoading"
              @update-field="handleUpdateField"
              @preview="handleSchedulePreview"
            />
          </section>

          <section v-show="activeSection === 'strategy'" id="strategy" class="form-section strategy-section">
            <div class="section-heading">
              <i class="i-material-symbols:shield-outline-rounded" />
              <div>
                <h3>运行策略</h3>
                <p>配置并发保护、失败处理和任务启用状态</p>
              </div>
            </div>

            <div class="strategy-group">
              <div class="strategy-group-heading">
                <i class="i-material-symbols:sync-lock-outline-rounded" />
                <div>
                  <strong>执行保护</strong>
                  <span>控制任务重叠、补偿和自动重试</span>
                </div>
              </div>
              <div class="advanced-controls">
                <NFormItem label="并发处理" path="concurrentPolicy">
                  <NSelect
                    :value="form.concurrentPolicy"
                    :options="concurrentPolicyOptions"
                    :clearable="false"
                    @update:value="handleUpdateField({ field: 'concurrentPolicy', value: $event })"
                  />
                </NFormItem>

                <NFormItem label="错过计划时" path="misfirePolicy">
                  <NSelect
                    :value="form.misfirePolicy"
                    :options="misfirePolicyOptions"
                    :clearable="false"
                    @update:value="handleUpdateField({ field: 'misfirePolicy', value: $event })"
                  />
                </NFormItem>

                <NFormItem label="失败自动重试" path="idempotentFlag">
                  <div class="status-setting">
                    <NSwitch
                      :value="form.idempotentFlag"
                      :checked-value="1"
                      :unchecked-value="0"
                      @update:value="handleIdempotentChange"
                    />
                    <div>
                      <strong>{{ form.idempotentFlag === 1 ? '允许自动重试' : '不自动重试' }}</strong>
                      <span>{{ form.idempotentFlag === 1 ? '任务已声明可安全重复执行' : '失败后保留本次执行结果' }}</span>
                    </div>
                  </div>
                </NFormItem>

                <NFormItem label="最多重试次数" path="retryCount">
                  <NInputNumber
                    :value="form.retryCount"
                    :min="0"
                    :max="5"
                    :step="1"
                    :disabled="form.idempotentFlag !== 1"
                    @update:value="handleUpdateField({ field: 'retryCount', value: $event ?? 0 })"
                  />
                </NFormItem>
              </div>
            </div>

            <div class="strategy-group">
              <div class="strategy-group-heading">
                <i class="i-material-symbols:notifications-outline-rounded" />
                <div>
                  <strong>失败通知</strong>
                  <span>所有重试结束后发送一次通知</span>
                </div>
              </div>
              <div class="alarm-controls">
                <NFormItem label="通知状态" path="alarmEnabled">
                  <div class="status-setting">
                    <NSwitch
                      :value="form.alarmEnabled"
                      :checked-value="1"
                      :unchecked-value="0"
                      @update:value="handleUpdateField({ field: 'alarmEnabled', value: $event })"
                    />
                    <div>
                      <strong>{{ form.alarmEnabled === 1 ? '最终失败时通知' : '不发送失败通知' }}</strong>
                      <span>{{ form.alarmEnabled === 1 ? '自动重试全部结束后通知接收人' : '执行结果仍会记录在运行日志中' }}</span>
                    </div>
                  </div>
                </NFormItem>

                <template v-if="form.alarmEnabled === 1">
                  <NFormItem label="通知渠道" path="alarmChannels">
                    <NCheckboxGroup
                      :value="form.alarmChannels"
                      class="alarm-channel-group"
                      @update:value="handleUpdateField({ field: 'alarmChannels', value: $event })"
                    >
                      <NCheckbox
                        v-for="option in alarmChannelOptions"
                        :key="option.value"
                        :value="option.value"
                        :label="option.label"
                      />
                    </NCheckboxGroup>
                  </NFormItem>

                  <NFormItem
                    v-if="form.alarmChannels.includes('WEB')"
                    label="平台用户"
                    path="alarmRecipientUserIds"
                  >
                    <UserSelectPicker
                      :model-value="form.alarmRecipientUserIds"
                      :label-value="alarmRecipientUserLabels"
                      multiple
                      clearable
                      placeholder="选择接收站内信的平台用户"
                      title="选择告警接收人"
                      @update:model-value="handleUpdateField({ field: 'alarmRecipientUserIds', value: $event })"
                      @update:label-value="alarmRecipientUserLabels = $event"
                    />
                  </NFormItem>

                  <NFormItem
                    v-if="form.alarmChannels.includes('EMAIL')"
                    label="邮箱地址"
                    path="alarmEmail"
                  >
                    <NInput
                      :value="form.alarmEmail"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 4 }"
                      placeholder="多个邮箱使用英文逗号分隔"
                      @update:value="handleUpdateField({ field: 'alarmEmail', value: $event })"
                    />
                  </NFormItem>
                </template>
              </div>
            </div>

            <div class="strategy-group strategy-status-group">
              <div class="strategy-group-heading">
                <i class="i-material-symbols:power-settings-new-rounded" />
                <div>
                  <strong>保存状态</strong>
                  <span>新任务默认停用，确认后可直接启用</span>
                </div>
              </div>
              <NFormItem label="任务状态" path="status">
                <div class="status-setting">
                  <NSwitch
                    :value="form.status"
                    :checked-value="1"
                    :unchecked-value="0"
                    @update:value="handleUpdateField({ field: 'status', value: $event })"
                  />
                  <div>
                    <strong>{{ form.status === 1 ? '保存后启用' : '保存后停用' }}</strong>
                    <span>{{ form.status === 1 ? '保存并同步后按计划运行' : '只保存配置，不会自动运行' }}</span>
                  </div>
                </div>
              </NFormItem>

              <NDescriptions :column="1" size="small" bordered label-placement="left">
                <NDescriptionsItem label="任务标识">
                  {{ form.jobGroup || 'DEFAULT' }}.{{ form.jobName || '待填写' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="任务时区">
                  {{ form.timezone || '待选择' }}
                </NDescriptionsItem>
                <NDescriptionsItem v-if="editing" label="调度同步">
                  {{ resolveSyncStatus(form.syncStatus) }}
                  <span v-if="form.syncError" class="sync-message">{{ form.syncError }}</span>
                </NDescriptionsItem>
              </NDescriptions>
            </div>
          </section>

          <div class="section-actions">
            <NButton v-if="currentSectionIndex > 0" @click="goToPreviousSection">
              <template #icon>
                <i class="i-material-symbols:arrow-back-rounded" />
              </template>
              上一步
            </NButton>
            <span v-else />
            <div class="section-progress">
              {{ currentSectionIndex + 1 }} / {{ sections.length }}
            </div>
            <NButton v-if="currentSectionIndex < sections.length - 1" type="primary" secondary @click="goToNextSection">
              下一步：{{ sections[currentSectionIndex + 1].label }}
              <template #icon>
                <i class="i-material-symbols:arrow-forward-rounded" />
              </template>
            </NButton>
            <NButton
              v-else-if="canSave"
              type="primary"
              :loading="saving"
              :disabled="partialSaved"
              @click="handleSave"
            >
              <template #icon>
                <i class="i-material-symbols:save-outline-rounded" />
              </template>
              {{ saveActionLabel }}
            </NButton>
          </div>
        </NForm>
      </main>

      <aside class="summary-column">
        <div class="summary-heading">
          <div>
            <i class="i-material-symbols:fact-check-outline-rounded" />
            <strong>配置摘要</strong>
          </div>
          <NTag size="small" :type="form.status === 1 ? 'success' : 'default'" :bordered="false">
            {{ form.status === 1 ? '保存后启用' : '保存后停用' }}
          </NTag>
        </div>

        <div class="summary-identity">
          <small>任务</small>
          <strong>{{ form.jobName || '待填写任务名称' }}</strong>
          <span>{{ form.jobGroup || 'DEFAULT' }}</span>
        </div>

        <div class="summary-section">
          <span class="summary-section-label">执行目标</span>
          <strong>{{ executionSummary }}</strong>
          <small>{{ invokeModeLabel }}</small>
        </div>

        <div class="summary-section schedule-summary">
          <span class="summary-section-label">执行计划</span>
          <strong :class="{ 'text-error': schedulePreview.error }">
            {{ schedulePreview.error || schedulePreview.description || '正在校验计划' }}
          </strong>
          <div class="summary-meta">
            <span>{{ scheduleTypeLabel }}</span>
            <span>{{ form.timezone || '待选择时区' }}</span>
          </div>
        </div>

        <div class="summary-section policy-summary">
          <span class="summary-section-label">运行保障</span>
          <div>
            <i class="i-material-symbols:sync-lock-outline-rounded" />
            <span>{{ concurrentPolicyLabel }}</span>
          </div>
          <div>
            <i class="i-material-symbols:restart-alt-rounded" />
            <span>{{ form.idempotentFlag === 1 ? `失败后最多重试 ${form.retryCount || 0} 次` : '失败后不自动重试' }}</span>
          </div>
          <div>
            <i class="i-material-symbols:notifications-outline-rounded" />
            <span>{{ alarmSummary }}</span>
          </div>
        </div>

        <div class="future-heading">
          <span>未来执行</span>
          <small>{{ form.timezone || '未选择时区' }}</small>
        </div>
        <ol v-if="schedulePreview.nextFireTimes.length" class="future-list">
          <li v-for="time in schedulePreview.nextFireTimes" :key="time">
            <i class="i-material-symbols:schedule-outline-rounded" />
            {{ formatDateTime(time) }}
          </li>
        </ol>
        <div v-else class="empty-future">
          暂无可预览的执行时间
        </div>
      </aside>
    </div>

    <footer class="mobile-actions">
      <NButton :disabled="saving" @click="handleCancel">
        取消
      </NButton>
      <NButton
        v-if="canSave"
        type="primary"
        :loading="saving"
        :disabled="loading || partialSaved"
        @click="handleSave"
      >
        {{ saveActionLabel }}
      </NButton>
    </footer>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import {
  NAlert,
  NButton,
  NCheckbox,
  NCheckboxGroup,
  NDescriptions,
  NDescriptionsItem,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSpin,
  NSwitch,
  NTag,
  NTooltip,
  useThemeVars,
} from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  createJobConfig,
  getJobConfig,
  getJobExecutors,
  getJobFlowModelVersions,
  getJobTimezones,
  getPublishedJobFlowModels,
  updateJobConfig,
} from '@/api/system/job'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { useDict } from '@/composables'
import { useTabStore, useUserStore } from '@/store'
import {
  buildJobConfigPayload,
  createDefaultJobForm,
  isOnceTimeInFuture,
  JOB_INVOKE_FLOW,
  JOB_INVOKE_SINGLE,
  JOB_SCHEDULE_CRON,
  JOB_SCHEDULE_ONCE,
  normalizeJobConfig,
} from '../job-config-form'
import { hasJobPermission, JOB_PERMISSIONS } from '../job-permission'
import JobBasicSection from './JobBasicSection.vue'
import JobExecutionSection from './JobExecutionSection.vue'
import JobScheduleSection from './JobScheduleSection.vue'

defineOptions({ name: 'JobConfigWorkbench' })

const route = useRoute()
const router = useRouter()
const tabStore = useTabStore()
const userStore = useUserStore()
const themeVars = useThemeVars()
const { dict } = useDict(
  'sys_job_schedule_type',
  'sys_job_invoke_mode',
  'sys_job_concurrent_policy',
  'sys_job_misfire_policy',
  'sys_job_alarm_channel',
)
const formRef = ref(null)
const formScrollRef = ref(null)
const form = reactive(createDefaultJobForm())
const pageThemeStyle = computed(() => ({
  '--action-color': themeVars.value.actionColor,
  '--body-color': themeVars.value.bodyColor,
  '--border-color': themeVars.value.borderColor,
  '--card-color': themeVars.value.cardColor,
  '--divider-color': themeVars.value.dividerColor,
  '--error-color': themeVars.value.errorColor,
  '--primary-color': themeVars.value.primaryColor,
  '--success-color': themeVars.value.successColor,
  '--text-color-1': themeVars.value.textColor1,
  '--text-color-2': themeVars.value.textColor2,
  '--text-color-3': themeVars.value.textColor3,
}))
const executors = ref([])
const flowModels = ref([])
const flowVersions = ref([])
const timezoneOptions = ref([])
const loading = ref(true)
const executorLoading = ref(true)
const flowModelLoading = ref(true)
const flowVersionLoading = ref(false)
const timezoneLoading = ref(true)
const saving = ref(false)
const loadError = ref('')
const partialSaved = ref(false)
const completedSchedule = ref(false)
const activeSection = ref('basic')
const baseline = ref('')
const schedulePreview = reactive({ description: '', nextFireTimes: [], error: '' })
const alarmRecipientUserLabels = ref([])
let flowVersionRequestId = 0

const editing = computed(() => Boolean(route.params.id))
const dirty = computed(() => Boolean(baseline.value) && baseline.value !== snapshotForm())
const scheduleTypeOptions = computed(() => dict.value.sys_job_schedule_type || [])
const invokeModeOptions = computed(() => dict.value.sys_job_invoke_mode || [])
const concurrentPolicyOptions = computed(() => dict.value.sys_job_concurrent_policy || [])
const misfirePolicyOptions = computed(() => dict.value.sys_job_misfire_policy || [])
const alarmChannelOptions = computed(() => dict.value.sys_job_alarm_channel || [])
const flowModelOptions = computed(() => {
  const options = flowModels.value
    .filter(item => item?.modelKey)
    .map(item => ({
      label: item.modelName ? `${item.modelName} · ${item.modelKey}` : item.modelKey,
      value: String(item.modelKey),
    }))
  if (form.flowModelKey && !options.some(item => item.value === form.flowModelKey)) {
    options.unshift({
      label: `${form.flowModelKey} · 当前绑定`,
      value: form.flowModelKey,
    })
  }
  return options
})
const flowVersionOptions = computed(() => {
  const options = flowVersions.value
    .filter(item => Number.isInteger(item.version) && item.version > 0 && !item.suspended)
    .map(item => ({
      label: formatFlowVersionLabel(item),
      value: item.version,
      deploymentId: item.deploymentId || '',
      processDefinitionId: item.processDefinitionId || item.id || '',
    }))
  if (form.flowModelVersion && !options.some(item => item.value === form.flowModelVersion)) {
    options.unshift({
      label: `V${form.flowModelVersion} · 当前绑定`,
      value: form.flowModelVersion,
      deploymentId: form.flowDeploymentId || '',
      processDefinitionId: form.flowProcessDefinitionId || '',
    })
  }
  return options
})
const canSave = computed(() => hasJobPermission(
  userStore,
  route,
  editing.value ? JOB_PERMISSIONS.configEdit : JOB_PERMISSIONS.configAdd,
))
const alarmSummary = computed(() => {
  if (form.alarmEnabled !== 1)
    return '未启用'
  const labels = form.alarmChannels.map((value) => {
    return alarmChannelOptions.value.find(option => String(option.value) === String(value))?.label || value
  })
  return labels.join('、') || '待配置'
})
const scheduleTypeLabel = computed(() => {
  const option = scheduleTypeOptions.value
    .find(item => String(item.value) === String(form.scheduleType))
  return option?.label || form.scheduleType || '待选择'
})
const invokeModeLabel = computed(() => {
  const option = invokeModeOptions.value
    .find(item => String(item.value) === String(form.invokeMode))
  return option?.label || form.invokeMode || '待选择'
})
const saveActionLabel = computed(() => completedSchedule.value ? '保存并重新计划' : '保存任务')
const concurrentPolicyLabel = computed(() => {
  const option = concurrentPolicyOptions.value
    .find(item => String(item.value) === String(form.concurrentPolicy))
  return option?.label || form.concurrentPolicy || '待选择'
})

const sections = [
  { key: 'basic', label: '基本信息', description: '名称与业务归属' },
  { key: 'execution', label: '执行内容', description: '处理器或流程模型' },
  { key: 'schedule', label: '执行计划', description: '频率、时间与时区' },
  { key: 'strategy', label: '运行策略', description: '保护、通知与状态' },
]
const currentSectionIndex = computed(() => Math.max(
  0,
  sections.findIndex(item => item.key === activeSection.value),
))

const groupOptions = computed(() => [...new Set([
  'DEFAULT',
  form.jobGroup,
  ...executors.value.map(item => item.group),
].filter(Boolean))].map(value => ({ label: value, value })))

const executionSummary = computed(() => {
  if (form.invokeMode === JOB_INVOKE_FLOW) {
    const model = flowModelOptions.value.find(item => item.value === form.flowModelKey)
    const modelLabel = model?.label || form.flowModelKey || '待选择流程模型'
    return form.flowModelVersion ? `${modelLabel} · V${form.flowModelVersion}` : modelLabel
  }
  if (form.executeMode === 'HANDLER') {
    const item = executors.value.find(executor => executor.executeMode === 'HANDLER'
      && executor.code === form.executorHandler)
    return item?.displayName || form.executorHandler || '待选择任务处理器'
  }
  if (form.executeMode === 'RPC')
    return [form.executorService, form.executorHandler].filter(Boolean).join(' · ') || '待填写远程服务'
  return [form.executorBean, form.executorMethod].filter(Boolean).join(' · ') || '待填写本地服务方法'
})

const saveStateText = computed(() => {
  if (saving.value)
    return '保存中'
  if (partialSaved.value)
    return '已保存，待同步'
  return dirty.value ? '未保存' : '已保存'
})

const saveStateType = computed(() => {
  if (partialSaved.value)
    return 'warning'
  return dirty.value ? 'warning' : 'success'
})

const formRules = {
  jobName: { required: true, message: '请输入任务名称', trigger: ['blur', 'input'] },
  jobGroup: { required: true, message: '请输入任务分组', trigger: ['blur', 'input'] },
  invokeMode: { required: true, message: '请选择调用方式', trigger: 'change' },
  executeMode: {
    trigger: 'change',
    validator: (_rule, value) => form.invokeMode !== JOB_INVOKE_SINGLE || value
      ? true
      : new Error('请选择执行方式'),
  },
  flowModelKey: {
    trigger: 'change',
    validator: (_rule, value) => form.invokeMode !== JOB_INVOKE_FLOW || value
      ? true
      : new Error('请选择已发布流程模型'),
  },
  flowModelVersion: {
    trigger: 'change',
    validator: (_rule, value) => form.invokeMode !== JOB_INVOKE_FLOW
      || (Number.isInteger(value) && value > 0)
      ? true
      : new Error('请选择已发布流程版本'),
  },
  scheduleType: { required: true, message: '请选择调度方式', trigger: 'change' },
  concurrentPolicy: { required: true, message: '请选择并发策略', trigger: 'change' },
  misfirePolicy: { required: true, message: '请选择错过触发策略', trigger: 'change' },
  timezone: { required: true, message: '请选择任务时区', trigger: ['blur', 'change'] },
  cronExpression: {
    trigger: ['blur', 'input'],
    validator: (_rule, value) => form.scheduleType !== JOB_SCHEDULE_CRON || value
      ? true
      : new Error('请配置周期计划'),
  },
  fireOnceTime: {
    trigger: ['blur', 'change'],
    validator: (_rule, value) => {
      if (form.scheduleType !== JOB_SCHEDULE_ONCE)
        return true
      if (!value)
        return new Error('请选择一次性执行时间')
      return isOnceTimeInFuture(value, form.timezone)
        ? true
        : new Error('一次性执行时间必须晚于当前时刻')
    },
  },
  executorHandler: {
    trigger: ['blur', 'change'],
    validator: (_rule, value) => {
      if (form.invokeMode === JOB_INVOKE_SINGLE
        && ['HANDLER', 'RPC'].includes(form.executeMode) && !value) {
        return new Error(form.executeMode === 'HANDLER' ? '请选择任务处理器' : '请输入远程处理器')
      }
      return true
    },
  },
  executorBean: {
    trigger: ['blur', 'input'],
    validator: (_rule, value) => {
      if (form.invokeMode !== JOB_INVOKE_SINGLE || form.executeMode !== 'BEAN')
        return true
      return value ? true : new Error('请输入服务 Bean')
    },
  },
  executorMethod: {
    trigger: ['blur', 'input'],
    validator: (_rule, value) => {
      if (form.invokeMode !== JOB_INVOKE_SINGLE || form.executeMode !== 'BEAN')
        return true
      return value ? true : new Error('请输入执行方法')
    },
  },
  executorService: {
    trigger: ['blur', 'input'],
    validator: (_rule, value) => {
      if (form.invokeMode !== JOB_INVOKE_SINGLE || form.executeMode !== 'RPC')
        return true
      return value ? true : new Error('请输入远程服务')
    },
  },
  jobParam: {
    trigger: ['blur', 'input'],
    validator: (_rule, value) => {
      if (!value)
        return true
      try {
        const parsed = JSON.parse(value)
        if (form.invokeMode === JOB_INVOKE_FLOW
          && (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object')) {
          return new Error('FLOW任务参数必须是 JSON 对象')
        }
        return true
      }
      catch {
        return new Error('任务参数必须是合法 JSON')
      }
    },
  },
  retryCount: {
    trigger: ['blur', 'change'],
    validator: (_rule, value) => {
      if (!Number.isInteger(value) || value < 0 || value > 5)
        return new Error('失败重试次数必须在 0 到 5 之间')
      if (value > 0 && form.idempotentFlag !== 1)
        return new Error('只有幂等任务才能配置自动重试')
      return true
    },
  },
  alarmChannels: {
    trigger: 'change',
    validator: (_rule, value) => form.alarmEnabled !== 1 || value?.length
      ? true
      : new Error('启用告警后至少选择一个通知渠道'),
  },
  alarmRecipientUserIds: {
    trigger: 'change',
    validator: (_rule, value) => form.alarmEnabled !== 1
      || !form.alarmChannels.includes('WEB')
      || value?.length
      ? true
      : new Error('请选择站内信接收用户'),
  },
  alarmEmail: {
    trigger: ['blur', 'input'],
    validator: (_rule, value) => {
      if (form.alarmEnabled !== 1 || !form.alarmChannels.includes('EMAIL'))
        return true
      const emails = String(value || '').split(',').map(item => item.trim()).filter(Boolean)
      if (!emails.length)
        return new Error('请输入告警邮箱')
      return emails.every(isEmail)
        ? true
        : new Error('告警邮箱格式不正确')
    },
  },
}

onMounted(() => {
  loadWorkbench()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  tabStore.setTabDirty(route.fullPath, false)
})

onBeforeRouteLeave(() => {
  if (tabStore.consumeDirtyNavigation(route.fullPath))
    return true
  if (!dirty.value)
    return true
  return confirmLeave()
})

watch(dirty, (value) => {
  tabStore.setTabDirty(route.fullPath, value, '当前任务配置存在未保存的更改')
}, { immediate: true })

async function loadWorkbench() {
  loading.value = true
  loadError.value = ''
  const catalogsPromise = Promise.all([
    loadExecutors(),
    loadTimezones(),
    loadPublishedFlowModels(),
  ])
  try {
    if (editing.value) {
      const response = await getJobConfig(route.params.id)
      if (!response.data)
        throw new Error('定时任务不存在或已删除')
      completedSchedule.value = Number(response.data.status) === 2
      Object.assign(form, normalizeJobConfig(response.data))
    }
    else {
      completedSchedule.value = false
      Object.assign(form, createDefaultJobForm())
    }
    await catalogsPromise
    ensureSelectedTimezoneOption()
    if (form.invokeMode === JOB_INVOKE_FLOW && form.flowModelKey)
      await loadFlowVersions(form.flowModelKey)
    await nextTick()
    baseline.value = snapshotForm()
  }
  catch (error) {
    loadError.value = resolveErrorMessage(error, '任务配置加载失败')
  }
  finally {
    loading.value = false
  }
}

async function loadPublishedFlowModels() {
  flowModelLoading.value = true
  try {
    const response = await getPublishedJobFlowModels()
    flowModels.value = Array.isArray(response.data) ? response.data : []
  }
  catch (error) {
    flowModels.value = []
    window.$message.warning(resolveErrorMessage(error, '已发布流程模型加载失败'))
  }
  finally {
    flowModelLoading.value = false
  }
}

async function loadFlowVersions(modelKey) {
  const normalizedModelKey = String(modelKey || '').trim()
  const requestId = ++flowVersionRequestId
  if (!normalizedModelKey) {
    flowVersions.value = []
    flowVersionLoading.value = false
    return
  }

  flowVersionLoading.value = true
  try {
    const response = await getJobFlowModelVersions(normalizedModelKey)
    if (requestId !== flowVersionRequestId || form.flowModelKey !== normalizedModelKey)
      return
    flowVersions.value = (Array.isArray(response.data) ? response.data : [])
      .map(item => ({
        ...item,
        version: Number(item.version),
        processDefinitionId: item.processDefinitionId || item.id || '',
      }))
  }
  catch (error) {
    if (requestId !== flowVersionRequestId)
      return
    flowVersions.value = []
    window.$message.warning(resolveErrorMessage(error, '流程版本加载失败'))
  }
  finally {
    if (requestId === flowVersionRequestId)
      flowVersionLoading.value = false
  }
}

async function loadTimezones() {
  timezoneLoading.value = true
  try {
    const response = await getJobTimezones()
    timezoneOptions.value = Array.isArray(response.data) ? response.data : []
  }
  catch (error) {
    timezoneOptions.value = []
    window.$message.warning(resolveErrorMessage(error, '时区目录加载失败'))
  }
  finally {
    timezoneLoading.value = false
  }
}

function ensureSelectedTimezoneOption() {
  if (!form.timezone || timezoneOptions.value.some(option => option.value === form.timezone))
    return
  timezoneOptions.value.unshift({ label: form.timezone, value: form.timezone, offset: '' })
}

async function loadExecutors() {
  executorLoading.value = true
  try {
    const response = await getJobExecutors()
    executors.value = Array.isArray(response.data) ? response.data : []
  }
  catch (error) {
    executors.value = []
    window.$message.warning(resolveErrorMessage(error, '任务处理器目录加载失败'))
  }
  finally {
    executorLoading.value = false
  }
}

function handleUpdateField({ field, value }) {
  form[field] = value
  partialSaved.value = false
}

function handleInvokeModeChange(value) {
  form.invokeMode = value
  if (value === JOB_INVOKE_SINGLE && !form.executeMode)
    form.executeMode = 'HANDLER'
  partialSaved.value = false
}

function handleFlowModelChange(value) {
  form.flowModelKey = value
  form.flowModelVersion = null
  form.flowDeploymentId = ''
  form.flowProcessDefinitionId = ''
  flowVersions.value = []
  partialSaved.value = false
  loadFlowVersions(value)
}

function handleFlowVersionChange(value) {
  form.flowModelVersion = value
  const selected = flowVersionOptions.value.find(item => item.value === value)
  form.flowDeploymentId = selected?.deploymentId || ''
  form.flowProcessDefinitionId = selected?.processDefinitionId || ''
  partialSaved.value = false
}

function handleIdempotentChange(value) {
  form.idempotentFlag = value
  if (value !== 1)
    form.retryCount = 0
  partialSaved.value = false
}

function handleSchedulePreview(value) {
  Object.assign(schedulePreview, value)
}

async function handleSave() {
  if (partialSaved.value)
    return
  try {
    await formRef.value?.validate()
  }
  catch (errors) {
    focusValidationError(errors)
    return
  }
  if (schedulePreview.error) {
    scrollToSection('schedule')
    window.$message.warning('请先修正执行计划')
    return
  }

  let payload
  try {
    payload = buildJobConfigPayload(form)
  }
  catch (error) {
    scrollToSection('execution')
    window.$message.error(error.message)
    return
  }

  saving.value = true
  try {
    if (editing.value)
      await updateJobConfig(payload)
    else
      await createJobConfig(payload)
    baseline.value = snapshotForm()
    tabStore.setTabDirty(route.fullPath, false)
    window.$message.success(form.status === 1
      ? '配置已保存，任务已启用并同步成功'
      : '配置已保存，任务保持停用')
    router.push('/system/job-config')
  }
  catch (error) {
    const message = resolveErrorMessage(error, '任务保存失败')
    if (message.includes('已保存，但调度同步失败')) {
      baseline.value = snapshotForm()
      partialSaved.value = true
      tabStore.setTabDirty(route.fullPath, false)
      window.$message.warning('配置已保存，但调度同步失败')
    }
    else {
      window.$message.error(message)
    }
  }
  finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push('/system/job-config')
}

function returnAfterPartialSave() {
  baseline.value = snapshotForm()
  tabStore.setTabDirty(route.fullPath, false)
  router.push('/system/job-config')
}

function scrollToSection(key) {
  activeSection.value = key
  formScrollRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

function goToPreviousSection() {
  const previous = sections[currentSectionIndex.value - 1]
  if (previous)
    scrollToSection(previous.key)
}

function goToNextSection() {
  const next = sections[currentSectionIndex.value + 1]
  if (next)
    scrollToSection(next.key)
}

function focusValidationError(errors) {
  const path = errors?.flat?.()?.[0]?.[0]?.field || errors?.[0]?.[0]?.field || ''
  const section = ['jobName', 'jobGroup', 'description'].includes(path)
    ? 'basic'
    : ['scheduleType', 'cronExpression', 'fireOnceTime', 'timezone'].includes(path)
        ? 'schedule'
        : [
            'concurrentPolicy',
            'misfirePolicy',
            'idempotentFlag',
            'retryCount',
            'status',
            'alarmEnabled',
            'alarmChannels',
            'alarmRecipientUserIds',
            'alarmEmail',
          ].includes(path)
            ? 'strategy'
            : 'execution'
  scrollToSection(section)
  window.$message.warning('请完成必填项后再保存')
}

function snapshotForm() {
  const keys = [
    'id',
    'jobName',
    'jobGroup',
    'description',
    'invokeMode',
    'executeMode',
    'executorBean',
    'executorMethod',
    'executorHandler',
    'executorService',
    'flowModelKey',
    'flowModelVersion',
    'flowDeploymentId',
    'flowProcessDefinitionId',
    'scheduleType',
    'cronExpression',
    'fireOnceTime',
    'timezone',
    'jobParam',
    'status',
    'concurrentPolicy',
    'misfirePolicy',
    'idempotentFlag',
    'retryCount',
    'alarmEnabled',
    'alarmChannels',
    'alarmRecipientUserIds',
    'alarmEmail',
    'version',
  ]
  return JSON.stringify(Object.fromEntries(keys.map(key => [key, form[key] ?? null])))
}

function handleBeforeUnload(event) {
  if (!dirty.value)
    return
  event.preventDefault()
  event.returnValue = ''
}

function confirmLeave() {
  return new Promise((resolve) => {
    window.$dialog.warning({
      title: '未保存变更',
      content: '当前任务配置有未保存的更改，确定离开吗？',
      positiveText: '离开',
      negativeText: '继续编辑',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
}

function resolveSyncStatus(value) {
  return {
    PENDING: '等待同步',
    SYNCED: '同步成功',
    FAILED: '同步失败',
    DELETE_PENDING: '等待删除',
  }[value] || '尚未同步'
}

function resolveErrorMessage(error, fallback) {
  return error?.response?.data?.msg
    || error?.response?.data?.message
    || error?.message
    || fallback
}

function isEmail(value) {
  const text = String(value || '')
  const atIndex = text.indexOf('@')
  const domain = text.slice(atIndex + 1)
  return atIndex > 0
    && atIndex === text.lastIndexOf('@')
    && domain.includes('.')
    && !text.includes(' ')
    && !text.includes('\t')
    && !text.includes('\n')
}

function formatFlowVersionLabel(item) {
  const parts = [`V${item.version}`]
  if (item.deploymentTime)
    parts.push(formatDateTime(item.deploymentTime))
  return parts.join(' · ')
}

function formatDateTime(value) {
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.job-workbench {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--body-color);
}

.workbench-toolbar {
  min-height: 48px;
  flex: 0 0 auto;
  padding: 6px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--card-color);
  border-bottom: 1px solid var(--border-color);
}

.header-identity,
.header-operations {
  display: flex;
  align-items: center;
}

.header-identity {
  min-width: 0;
}

.header-operations {
  flex: 0 0 auto;
  gap: 8px;
}

.workbench-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  color: var(--text-color-3);
  font-size: 13px;
}

.workbench-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 196px minmax(520px, 1fr) 286px;
  background: var(--card-color);
}

.section-nav {
  min-width: 0;
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
  border-right: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--body-color) 70%, var(--card-color));
}

.section-nav button {
  width: 100%;
  min-height: 58px;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-color-2);
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  text-align: left;
}

.section-nav button:hover,
.section-nav button.active {
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 9%, transparent);
}

.section-nav button.active {
  box-shadow: inset 3px 0 0 var(--primary-color);
}

.nav-step {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  color: var(--text-color-3);
  background: var(--card-color);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.section-nav button.active .nav-step {
  color: var(--primary-color);
  border-color: color-mix(in srgb, var(--primary-color) 55%, var(--border-color));
}

.nav-copy {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 1px;
}

.nav-copy strong {
  color: inherit;
  font-size: 13px;
  font-weight: 600;
}

.nav-copy small {
  overflow: hidden;
  max-width: 100%;
  color: var(--text-color-3);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-column {
  min-width: 0;
  padding: 8px clamp(24px, 4vw, 56px) 40px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.form-column > * {
  max-width: 880px;
  margin-right: auto;
  margin-left: auto;
}

.page-alert {
  margin-top: 20px;
}

.form-section {
  padding: 26px 0 24px;
  scroll-margin-top: 12px;
}

.section-heading {
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.section-heading > i {
  margin-top: 1px;
  color: var(--primary-color);
  font-size: 20px;
}

.section-heading h3,
.section-heading p {
  margin: 0;
}

.section-heading h3 {
  color: var(--text-color-1);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
}

.section-heading p {
  margin-top: 3px;
  color: var(--text-color-3);
  font-size: 12px;
}

.alarm-controls {
  max-width: 720px;
}

.alarm-channel-group {
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 22px;
}

.advanced-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 22px;
}

.advanced-controls :deep(.n-input-number) {
  width: 100%;
}

.strategy-group {
  padding: 20px 0 8px;
  border-top: 1px solid var(--divider-color);
}

.strategy-group:first-of-type {
  padding-top: 0;
  border-top: 0;
}

.strategy-group-heading {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.strategy-group-heading > i {
  margin-top: 1px;
  color: var(--text-color-3);
  font-size: 18px;
}

.strategy-group-heading > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.strategy-group-heading strong {
  color: var(--text-color-1);
  font-size: 13px;
}

.strategy-group-heading span {
  color: var(--text-color-3);
  font-size: 11px;
}

.strategy-status-group :deep(.n-descriptions) {
  margin-top: 4px;
}

.status-setting {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-setting > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-setting strong {
  color: var(--text-color-1);
  font-size: 13px;
}

.status-setting span,
.sync-message {
  color: var(--text-color-3);
  font-size: 12px;
}

.sync-message {
  margin-left: 8px;
  color: var(--error-color);
}

.section-actions {
  min-height: 52px;
  margin-top: 4px;
  padding: 10px 0 0;
  display: grid;
  grid-template-columns: minmax(140px, 1fr) auto minmax(220px, 1fr);
  align-items: center;
  gap: 16px;
  border-top: 1px solid var(--divider-color);
}

.section-actions > :last-child {
  justify-self: end;
}

.section-progress {
  color: var(--text-color-3);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.summary-column {
  min-width: 0;
  padding: 20px 18px;
  overflow-y: auto;
  border-left: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--body-color) 62%, var(--card-color));
}

.summary-heading,
.future-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-heading {
  color: var(--text-color-1);
  font-size: 14px;
}

.summary-heading > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-heading i {
  color: var(--primary-color);
  font-size: 18px;
}

.summary-identity {
  margin-top: 18px;
  padding: 14px 0 16px;
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--divider-color);
  border-bottom: 1px solid var(--divider-color);
}

.summary-identity small,
.summary-section-label {
  color: var(--text-color-3);
  font-size: 10px;
}

.summary-identity strong {
  overflow-wrap: anywhere;
  margin-top: 4px;
  color: var(--text-color-1);
  font-size: 16px;
  line-height: 1.45;
}

.summary-identity span {
  margin-top: 2px;
  color: var(--text-color-3);
  font-size: 11px;
}

.summary-section {
  padding: 14px 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  border-bottom: 1px solid var(--divider-color);
}

.summary-section > strong {
  overflow-wrap: anywhere;
  color: var(--text-color-1);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.55;
}

.summary-section > strong.text-error {
  color: var(--error-color);
}

.summary-section > small {
  color: var(--text-color-3);
  font-size: 11px;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 12px;
  color: var(--text-color-3);
  font-size: 10px;
}

.policy-summary {
  gap: 8px;
}

.policy-summary > div {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 7px;
  color: var(--text-color-2);
  font-size: 11px;
  line-height: 1.45;
}

.policy-summary i {
  flex: 0 0 auto;
  margin-top: 1px;
  color: var(--text-color-3);
  font-size: 14px;
}

.future-heading {
  margin-top: 16px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--divider-color);
}

.future-heading span {
  color: var(--text-color-2);
  font-size: 13px;
  font-weight: 600;
}

.future-heading small {
  color: var(--text-color-3);
  font-size: 11px;
}

.future-list {
  margin: 0;
  padding: 10px 0 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
  list-style: none;
}

.future-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-color-2);
  font-size: 12px;
}

.future-list i {
  color: var(--primary-color);
  font-size: 15px;
}

.empty-future {
  padding: 18px 0;
  color: var(--text-color-3);
  font-size: 12px;
  text-align: center;
}

.mobile-actions {
  display: none;
}

@media (max-width: 1180px) {
  .workbench-grid {
    grid-template-columns: 176px minmax(0, 1fr);
  }

  .summary-column {
    grid-column: 2;
    padding: 18px clamp(24px, 4vw, 56px) 28px;
    border-top: 1px solid var(--border-color);
    border-left: 0;
  }

  .summary-column {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
    gap: 0 24px;
  }

  .summary-heading,
  .summary-identity {
    grid-column: 1 / -1;
  }

  .future-heading,
  .future-list,
  .empty-future {
    grid-column: 2;
  }
}

@media (max-width: 768px) {
  .job-workbench {
    padding-bottom: 64px;
  }

  .workbench-toolbar {
    min-height: 48px;
    padding: 6px 10px;
  }

  .desktop-action {
    display: none;
  }

  .workbench-grid {
    display: block;
    overflow-y: auto;
  }

  .section-nav {
    position: sticky;
    z-index: 2;
    top: 0;
    padding: 7px 8px;
    flex-direction: row;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid var(--border-color);
  }

  .section-nav button {
    width: auto;
    min-width: max-content;
    min-height: 42px;
    padding: 6px 10px;
    box-shadow: none;
  }

  .nav-step {
    width: 24px;
    height: 24px;
  }

  .nav-copy small {
    display: none;
  }

  .form-column {
    padding: 0 14px 24px;
    overflow: visible;
  }

  .form-section {
    min-height: 0;
    padding: 22px 0 26px;
  }

  .advanced-controls {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-column {
    padding: 18px 14px 26px;
    display: block;
    border-top: 1px solid var(--border-color);
  }

  .section-actions {
    grid-template-columns: 1fr 1fr;
  }

  .section-actions .section-progress {
    display: none;
  }

  .section-actions > :last-child {
    max-width: 100%;
    justify-self: stretch;
  }

  .section-actions > :last-child :deep(.n-button__content) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-actions {
    position: fixed;
    z-index: 20;
    right: 0;
    bottom: 0;
    left: 0;
    height: 64px;
    padding: 10px 14px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    background: var(--card-color);
    border-top: 1px solid var(--border-color);
  }
}
</style>
