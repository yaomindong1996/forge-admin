<template>
  <div class="job-config-page" :style="pageThemeStyle">
    <section
      class="monitor-overview"
      :class="{ 'is-loading': monitorLoading }"
      :aria-busy="monitorLoading"
      aria-label="近 24 小时任务运行概览"
    >
      <div class="monitor-heading-block">
        <div class="monitor-heading-main">
          <span class="monitor-heading-icon" aria-hidden="true">
            <i class="i-lucide:activity" />
          </span>
          <div class="monitor-heading-copy">
            <strong>运行概览</strong>
            <span>近 24 小时</span>
          </div>
        </div>
      </div>

      <div class="monitor-metric">
        <span>执行次数</span>
        <div><strong>{{ monitorSummary.totalCount }}</strong><small>次</small></div>
        <small>滚动统计窗口</small>
      </div>

      <div class="monitor-metric is-success">
        <span><i />执行成功</span>
        <div><strong>{{ monitorSummary.successCount }}</strong><small>次</small></div>
        <small>成功率 {{ monitorSummary.successRate.toFixed(2) }}%</small>
      </div>

      <div class="monitor-metric" :class="{ 'is-error': monitorSummary.failedCount > 0 }">
        <span><i />执行失败</span>
        <div><strong>{{ monitorSummary.failedCount }}</strong><small>次</small></div>
        <small>失败率 {{ monitorSummary.failureRate.toFixed(2) }}%</small>
      </div>

      <div class="monitor-metric">
        <span>当前窗口</span>
        <div>
          <strong :class="{ 'is-running': monitorSummary.runningCount > 0 }">{{ monitorSummary.runningCount }}</strong>
          <small>运行中</small>
        </div>
        <small>接收 {{ monitorSummary.acceptedCount }} · 跳过 {{ monitorSummary.skippedCount }}</small>
      </div>

      <NTooltip v-if="monitorSummary.consecutiveFailureTaskCount > 0">
        <template #trigger>
          <div class="monitor-metric risk-metric is-error is-clickable">
            <span><i />连续失败</span>
            <div>
              <strong>{{ monitorSummary.consecutiveFailureTaskCount }}</strong>
              <small>个任务</small>
            </div>
            <small>悬停查看任务清单</small>
          </div>
        </template>
        <div class="failure-task-tooltip">
          <div v-for="task in monitorSummary.failureTasks" :key="task.jobConfigId">
            {{ task.jobName }} · {{ task.consecutiveFailures }} 次
          </div>
        </div>
      </NTooltip>
      <div v-else class="monitor-metric risk-metric is-healthy">
        <span><i />连续失败</span>
        <div><strong>0</strong><small>个任务</small></div>
        <small>当前运行正常</small>
      </div>
    </section>

    <main class="job-content">
      <div class="job-toolbar">
        <NButton v-if="canAdd" type="primary" data-assistant-guide="job-add" @click="handleCreate">
          <template #icon>
            <i class="i-material-symbols:add-rounded" />
          </template>
          新建任务
        </NButton>
        <NTooltip>
          <template #trigger>
            <NButton secondary circle aria-label="刷新任务列表" @click="handleRefresh">
              <template #icon>
                <i class="i-material-symbols:refresh-rounded" />
              </template>
            </NButton>
          </template>
          刷新任务列表
        </NTooltip>
        <NDropdown
          v-if="hasManagementActions"
          trigger="click"
          :options="pageManagementOptions"
          @select="handlePageManagement"
        >
          <NButton secondary>
            <template #icon>
              <i class="i-material-symbols:settings-outline-rounded" />
            </template>
            管理
          </NButton>
        </NDropdown>
      </div>
      <AiCrudPage
        ref="crudRef"
        :api-config="{ list: 'get@/job/config/page' }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        row-key="id"
        :hide-add="true"
        :hide-toolbar="true"
        max-height="var(--job-table-max-height)"
        :search-y-gap="8"
      />
    </main>

    <NModal
      v-model:show="logModalVisible"
      title="运行日志"
      preset="card"
      class="job-log-modal"
      :mask-closable="false"
    >
      <JobLogList
        ref="logListRef"
        :job-config-id="currentJob.id"
        :job-name="currentJob.jobName"
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="logModalVisible = false">
            关闭
          </NButton>
          <NButton type="primary" @click="handleRefreshLog">
            <template #icon>
              <i class="i-material-symbols:refresh-rounded" />
            </template>
            刷新
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { NButton, NDropdown, NEllipsis, NModal, NSpace, NTag, NTooltip, useThemeVars } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getJobMonitorSummary } from '@/api/system/job'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import { hasJobPermission, JOB_PERMISSIONS } from './job-config/job-permission'
import JobLogList from './job-log-list.vue'
import { normalizeJobMonitorSummary } from './job-log-query'
import { resolveJobExecutionMode } from './job-view-contract'

defineOptions({ name: 'JobConfig' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeVars = useThemeVars()
const { dict } = useDict(
  'sys_job_status',
  'sys_job_invoke_mode',
  'sys_job_run_mode',
  'sys_job_sync_status',
  'sys_job_schedule_type',
  'sys_job_log_status',
)
const jobStatusOptions = computed(() => dict.value.sys_job_status || [])
const jobInvokeModeOptions = computed(() => dict.value.sys_job_invoke_mode || [])
const jobRunModeOptions = computed(() => dict.value.sys_job_run_mode || [])
const jobSyncStatusOptions = computed(() => dict.value.sys_job_sync_status || [])
const jobScheduleTypeOptions = computed(() => dict.value.sys_job_schedule_type || [])
const jobLogStatusOptions = computed(() => dict.value.sys_job_log_status || [])
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

const crudRef = ref(null)
const logModalVisible = ref(false)
const logListRef = ref(null)
const currentJob = ref({})
const monitorLoading = ref(false)
const monitorSummary = ref(normalizeJobMonitorSummary())

const canAdd = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configAdd))
const canEdit = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configEdit))
const canRemove = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configRemove))
const canStart = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configStart))
const canStop = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configStop))
const canTrigger = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configTrigger))
const canSync = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.configSync))
const canViewLogs = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.logList))
const canCleanLogs = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.logClean))
const canManageApiTokens = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.apiTokenList))
const hasManagementActions = computed(() => canManageApiTokens.value || canCleanLogs.value)

const pageManagementOptions = computed(() => {
  const options = []
  if (canManageApiTokens.value) {
    options.push({ label: '开放 API 凭证', key: 'api-tokens' })
  }
  if (canCleanLogs.value) {
    if (options.length)
      options.push({ type: 'divider', key: 'divider' })
    options.push(
      { label: '清理 7 天前日志', key: 'clean-7' },
      { label: '清空全部日志', key: 'clean-all' },
    )
  }
  return options
})

const searchSchema = computed(() => [
  {
    field: 'jobName',
    label: '任务',
    type: 'input',
    props: { placeholder: '搜索任务名称' },
  },
  {
    field: 'jobGroup',
    label: '分组',
    type: 'input',
    props: { placeholder: '输入任务分组' },
  },
  {
    field: 'scheduleType',
    label: '调度方式',
    type: 'select',
    props: {
      placeholder: '全部调度方式',
      options: jobScheduleTypeOptions.value,
      clearable: true,
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '全部状态',
      options: jobStatusOptions.value,
      clearable: true,
    },
  },
  {
    field: 'executeMode',
    label: '执行方式',
    type: 'select',
    props: {
      placeholder: '全部方式',
      options: jobRunModeOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'jobName',
    label: '任务',
    minWidth: 180,
    render: row => h(SystemTableCell, {
      title: row.jobName,
      subtitle: [row.jobGroup, row.description].filter(Boolean).join(' · '),
      interactive: canEdit.value,
      tooltip: canEdit.value ? `编辑任务：${row.jobName || '-'}` : undefined,
      onActivate: canEdit.value ? () => handleEdit(row) : undefined,
    }),
  },
  {
    prop: 'executionSummary',
    label: '执行内容',
    minWidth: 180,
    render: (row) => {
      const executionMode = resolveJobExecutionMode(
        row,
        jobRunModeOptions.value,
        jobInvokeModeOptions.value,
      )
      return h('div', { class: 'execution-cell' }, [
        h('span', { class: 'cell-main' }, row.executionSummary || resolveExecutionFallback(row)),
        h(DictTag, {
          options: executionMode.options,
          value: executionMode.value,
          size: 'small',
          forceTag: true,
        }),
      ])
    },
  },
  {
    prop: 'scheduleSummary',
    label: '执行计划',
    minWidth: 220,
    render: row => h('div', { class: 'schedule-cell' }, [
      h('div', { class: 'schedule-heading' }, [
        h(DictTag, {
          options: jobScheduleTypeOptions.value,
          value: row.scheduleType,
          size: 'small',
          forceTag: true,
        }),
        h(NEllipsis, { class: 'cell-main', tooltip: { width: 320 } }, {
          default: () => resolveScheduleSummary(row),
        }),
      ]),
      h('span', { class: 'cell-secondary' }, resolveScheduleSubline(row)),
    ]),
  },
  {
    prop: 'status',
    label: '当前状态',
    width: 150,
    render: row => h('div', { class: 'status-cell' }, [
      h(DictTag, {
        options: jobStatusOptions.value,
        value: String(row.status),
        size: 'small',
      }),
      h(DictTag, {
        options: jobSyncStatusOptions.value,
        value: row.syncStatus,
        size: 'small',
        forceTag: true,
      }),
      row.syncError
        ? h(NEllipsis, { class: 'sync-error', tooltip: { width: 420 } }, {
            default: () => row.syncError,
          })
        : null,
    ]),
  },
  {
    prop: 'nextFireTime',
    label: '下次执行',
    width: 140,
    render: row => h('div', { class: 'time-cell' }, [
      h('span', { class: 'cell-main' }, resolveNextFireTime(row)),
      row.nextFireTime && row.status !== 2
        ? h('span', { class: 'cell-secondary' }, dayjs(row.nextFireTime).format('YYYY-MM-DD'))
        : null,
    ]),
  },
  {
    prop: 'lastExecutionStatus',
    label: '最近结果',
    width: 120,
    render: row => h('div', { class: 'result-cell' }, [
      row.lastExecutionStatus == null
        ? h(NTag, { size: 'small', bordered: false }, { default: () => '尚未执行' })
        : h(DictTag, {
            options: jobLogStatusOptions.value,
            value: String(row.lastExecutionStatus),
            size: 'small',
            forceTag: true,
          }),
      row.lastExecutionTime
        ? h('span', { class: 'cell-secondary' }, dayjs(row.lastExecutionTime).format('MM-DD HH:mm'))
        : null,
      row.consecutiveFailures > 0
        ? h('span', { class: 'failure-count' }, `连续失败 ${row.consecutiveFailures} 次`)
        : null,
    ]),
  },
  {
    prop: 'action',
    label: '操作',
    width: 250,
    fixed: 'right',
    maxActionButtons: 3,
    actions: [
      {
        label: '重新同步',
        loadingLabel: '同步中...',
        failureMessage: '重新同步失败，配置已保留，请查看失败原因',
        key: 'sync',
        type: 'warning',
        onClick: handleRetrySynchronization,
        visible: row => canSync.value && shouldRetrySynchronization(row),
      },
      {
        label: '重建调度',
        loadingLabel: '重建中...',
        failureMessage: '调度重建失败，请检查调度服务状态后再试',
        key: 'rebuild',
        type: 'warning',
        onClick: handleRebuild,
        visible: row => canSync.value && !isDeletePending(row),
      },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit, visible: row => canEdit.value && !isDeletePending(row) },
      { label: '立即运行', key: 'trigger', type: 'success', onClick: handleTrigger, visible: row => canTrigger.value && canRunOnce(row) },
      { label: '查看日志', key: 'log', type: 'info', onClick: handleViewLog, visible: () => canViewLogs.value },
      { label: '启用', key: 'start', type: 'success', onClick: handleStart, visible: row => canStart.value && !isDeletePending(row) && row.status === 0 },
      { label: '停用', key: 'stop', type: 'warning', onClick: handleStop, visible: row => canStop.value && !isDeletePending(row) && row.status === 1 },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => canRemove.value && !isDeletePending(row) },
    ],
  },
])

function resolveExecutionFallback(row) {
  if (row.invokeMode === 'FLOW')
    return [row.flowModelKey, row.flowModelVersion].filter(Boolean).join(' · ') || '未绑定流程模型'
  if (row.executeMode === 'HANDLER')
    return row.executorHandler || '未配置任务处理器'
  if (row.executeMode === 'RPC')
    return [row.executorService, row.executorHandler].filter(Boolean).join(' · ') || '未配置远程服务'
  return [row.executorBean, row.executorMethod].filter(Boolean).join(' · ') || '未配置本地服务方法'
}

function resolveScheduleSummary(row) {
  if (row.scheduleType === 'ONCE') {
    const option = jobScheduleTypeOptions.value
      .find(item => String(item.value) === String(row.scheduleType))
    const label = option?.label || row.scheduleType
    return row.fireOnceTime
      ? `${label} · ${dayjs(row.fireOnceTime).format('YYYY-MM-DD HH:mm')}`
      : label
  }
  return row.scheduleSummary || row.cronExpression || '自定义执行计划'
}

function resolveScheduleSubline(row) {
  const timezone = row.timezone || '未设置时区'
  if (row.status === 2)
    return `计划已结束 · ${timezone}`
  if (row.status === 0)
    return `当前已停用 · ${timezone}`
  return `任务时区 · ${timezone}`
}

function resolveNextFireTime(row) {
  if (row.status === 2)
    return '计划已结束'
  if (row.syncStatus !== 'SYNCED')
    return '等待同步'
  if (row.status === 0)
    return '已停用'
  return row.nextFireTime ? dayjs(row.nextFireTime).format('HH:mm') : '--'
}

function shouldRetrySynchronization(row) {
  return ['FAILED', 'DELETE_PENDING'].includes(row.syncStatus)
}

function isDeletePending(row) {
  return row.syncStatus === 'DELETE_PENDING'
}

function canRunOnce(row) {
  return row.syncStatus === 'SYNCED'
}

function handleCreate() {
  if (!canAdd.value)
    return
  router.push('/system/job-config/editor')
}

function handleEdit(row) {
  if (!canEdit.value)
    return
  router.push(`/system/job-config/editor/${row.id}`)
}

function handleRefresh() {
  crudRef.value?.refresh()
  loadMonitorSummary()
}

function handleOpenApiTokens() {
  if (!canManageApiTokens.value)
    return
  router.push('/system/job-api-token')
}

function handleDelete(row) {
  if (!canRemove.value)
    return
  window.$dialog.warning({
    title: '删除任务',
    content: `确定删除“${row.jobName}”吗？任务配置删除后不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/job/config/${row.id}`)
        window.$message.success('任务已删除')
      }
      finally {
        handleRefresh()
      }
    },
  })
}

async function handleStart(row) {
  if (!canStart.value)
    return
  await request.post(`/job/config/${row.id}/start`)
  window.$message.success('任务已启用并同步到调度服务')
  handleRefresh()
}

function handleStop(row) {
  if (!canStop.value)
    return
  window.$dialog.warning({
    title: '停用任务',
    content: `停用“${row.jobName}”后将不再按计划执行，是否继续？`,
    positiveText: '停用',
    negativeText: '取消',
    onPositiveClick: async () => {
      await request.post(`/job/config/${row.id}/stop`)
      window.$message.success('任务已停用')
      handleRefresh()
    },
  })
}

function handleTrigger(row) {
  if (!canTrigger.value)
    return
  window.$dialog.info({
    title: '立即运行任务',
    content: `将立即运行“${row.jobName}”。当前计划为“${row.scheduleSummary || '自定义执行计划'}”，启停状态不会改变。`,
    positiveText: '立即运行',
    negativeText: '取消',
    onPositiveClick: async () => {
      window.$message.loading('正在提交任务...', { key: 'trigger', duration: 0 })
      try {
        await request.post(`/job/config/${row.id}/trigger`)
        window.$message.success('任务已提交，可在运行日志中查看结果', { key: 'trigger' })
      }
      catch (error) {
        window.$message.error('任务提交失败', { key: 'trigger' })
        throw error
      }
    },
  })
}

async function handleRetrySynchronization(row) {
  if (!canSync.value)
    return
  await request.post(`/job/config/${row.id}/sync`)
  window.$message.success('调度同步已恢复')
  handleRefresh()
}

async function handleRebuild(row) {
  if (!canSync.value || isDeletePending(row))
    return
  await request.post(`/job/config/${row.id}/rebuild`)
  window.$message.success('Quartz 调度已重建')
  handleRefresh()
}

function handleViewLog(row) {
  if (!canViewLogs.value)
    return
  currentJob.value = row
  logModalVisible.value = true
}

function handleRefreshLog() {
  logListRef.value?.refresh()
  loadMonitorSummary()
}

async function loadMonitorSummary() {
  monitorLoading.value = true
  try {
    const response = await getJobMonitorSummary()
    monitorSummary.value = normalizeJobMonitorSummary(response.data)
  }
  catch (error) {
    console.error('加载任务监控摘要失败:', error)
    window.$message.error('加载任务监控摘要失败')
  }
  finally {
    monitorLoading.value = false
  }
}

function handlePageManagement(key) {
  if (key === 'api-tokens')
    handleOpenApiTokens()
  else if (key === 'clean-7')
    handleCleanLogs(7)
  else if (key === 'clean-all')
    handleCleanLogs(0)
}

function handleCleanLogs(days) {
  if (!canCleanLogs.value)
    return
  const cleanAll = days === 0
  window.$dialog.warning({
    title: cleanAll ? '清空所有日志' : `清理 ${days} 天前日志`,
    content: cleanAll
      ? '确定清空全部任务运行日志吗？此操作不可恢复。'
      : `确定清理 ${days} 天前的任务运行日志吗？`,
    positiveText: cleanAll ? '清空' : '清理',
    negativeText: '取消',
    onPositiveClick: async () => {
      const response = await request.delete('/job/log/clean', { params: { days } })
      window.$message.success(`已清理 ${response.data || 0} 条日志`)
      handleRefresh()
    },
  })
}

onMounted(loadMonitorSummary)
</script>

<style scoped>
.job-config-page {
  --job-table-max-height: calc(100vh - 298px);
  --job-overview-bg: color-mix(in srgb, #ffffff 78%, transparent);
  --job-overview-border: color-mix(in srgb, #ffffff 70%, #dbe4f5);
  height: 100%;
  min-height: 0;
  padding: 12px 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  background:
    radial-gradient(42% 36% at 6% 0%, rgba(147, 197, 253, 0.42), transparent 62%),
    radial-gradient(36% 32% at 96% 4%, rgba(196, 181, 253, 0.28), transparent 58%),
    linear-gradient(180deg, #eef4ff 0%, #f5f7fb 46%, #f7f8fb 100%);
  border-radius: 16px;
}

.monitor-overview {
  flex: 0 0 auto;
  min-height: 92px;
  display: grid;
  grid-template-columns: minmax(140px, 0.85fr) repeat(5, minmax(120px, 1fr));
  align-items: stretch;
  overflow: hidden;
  border: 1px solid var(--job-overview-border);
  border-radius: 18px;
  background: var(--job-overview-bg);
  box-shadow: 0 10px 28px color-mix(in srgb, #1e3a8a 8%, transparent);
  backdrop-filter: blur(16px);
  transition: opacity 0.2s ease;
}

.monitor-overview.is-loading {
  opacity: 0.58;
}

.monitor-total-block,
.monitor-panel {
  min-width: 0;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.monitor-panel {
  gap: 12px;
  border-left: 1px solid var(--divider-color);
}

.monitor-section-title,
.monitor-panel-heading,
.result-label,
.risk-summary,
.risk-summary > div {
  display: flex;
  align-items: center;
}

.monitor-section-title {
  gap: 7px;
  color: var(--text-color-2);
  font-size: 12px;
  font-weight: 600;
}

.monitor-section-title i {
  color: var(--primary-color);
  font-size: 17px;
}

.monitor-total-value {
  margin-top: 5px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.monitor-total-value strong {
  color: var(--text-color-1);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.monitor-total-value span {
  color: var(--text-color-2);
  font-size: 12px;
}

.monitor-total-block > small,
.monitor-panel-heading small,
.result-breakdown small,
.risk-hint {
  color: var(--text-color-3);
  font-size: 11px;
}

.monitor-total-block > small {
  margin-top: 2px;
}

.monitor-panel-heading {
  justify-content: space-between;
  gap: 10px;
}

.monitor-panel-heading > span {
  color: var(--text-color-2);
  font-size: 12px;
  font-weight: 600;
}

.result-meter {
  width: 100%;
  height: 5px;
  display: flex;
  overflow: hidden;
  background: var(--divider-color);
  border-radius: 3px;
}

.result-meter span {
  height: 100%;
  transition: width 0.2s ease;
}

.result-meter span.is-success {
  background: var(--success-color);
}

.result-meter span.is-error {
  background: var(--error-color);
}

.result-breakdown {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.result-breakdown > div {
  min-width: 0;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: baseline;
  gap: 1px 8px;
}

.result-breakdown strong {
  color: var(--text-color-1);
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.result-label {
  gap: 6px;
  color: var(--text-color-2);
  font-size: 12px;
}

.result-label i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.result-label.is-success i {
  background: var(--success-color);
}

.result-label.is-error i {
  background: var(--error-color);
}

.result-breakdown small {
  grid-column: 1 / -1;
  padding-left: 12px;
}

.window-breakdown {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.window-breakdown > div {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 1px;
}

.window-breakdown > div + div {
  border-left: 1px solid var(--divider-color);
}

.window-breakdown strong {
  color: var(--text-color-1);
  font-size: 19px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.window-breakdown strong.is-running {
  color: var(--primary-color);
}

.window-breakdown span {
  color: var(--text-color-3);
  font-size: 11px;
}

.risk-panel {
  gap: 7px;
}

.risk-panel.is-clickable {
  cursor: help;
}

.risk-summary {
  gap: 9px;
}

.risk-summary > i {
  color: var(--error-color);
  font-size: 23px;
}

.risk-summary.is-healthy > i {
  color: var(--success-color);
}

.risk-summary > div {
  align-items: baseline;
  gap: 5px;
}

.risk-summary strong {
  color: var(--text-color-1);
  font-size: 19px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.risk-summary span {
  color: var(--text-color-2);
  font-size: 12px;
}

.risk-panel.is-alert {
  box-shadow: inset 3px 0 0 var(--error-color);
}

.monitor-heading-block,
.monitor-metric {
  min-width: 0;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.monitor-heading-block {
  position: relative;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.1), transparent 78%);
}

.monitor-heading-main,
.monitor-heading-copy,
.monitor-metric > span,
.monitor-metric > div {
  display: flex;
  align-items: center;
  min-width: 0;
}

.monitor-heading-main {
  gap: 10px;
}

.monitor-heading-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(145deg, #4f8cff, #2563eb);
  color: #fff;
  font-size: 16px;
  box-shadow: 0 6px 14px color-mix(in srgb, #2563eb 26%, transparent);
}

.monitor-heading-copy {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.monitor-heading-copy strong {
  color: var(--text-primary, var(--text-color-1));
  font-size: 14px;
  font-weight: 700;
  line-height: 1.25;
}

.monitor-heading-copy span {
  color: var(--text-tertiary, var(--text-color-3));
  font-size: 11px;
  line-height: 1.3;
}

.monitor-metric {
  gap: 4px;
  border-left: 1px solid color-mix(in srgb, var(--border-light, var(--divider-color)) 78%, transparent);
}

.monitor-metric > span {
  gap: 6px;
  color: var(--text-tertiary, var(--text-color-3));
  font-size: 11px;
  line-height: 1.3;
}

.monitor-metric > span i {
  width: 7px;
  height: 7px;
  flex: none;
  background: var(--text-tertiary, var(--text-color-3));
  border-radius: 50%;
}

.monitor-metric > div {
  align-items: baseline;
  gap: 5px;
  flex-wrap: wrap;
}

.monitor-metric > div strong {
  color: var(--text-primary, var(--text-color-1));
  font-size: 22px;
  font-weight: 700;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}

.monitor-metric > div small,
.monitor-metric > small {
  color: var(--text-tertiary, var(--text-color-3));
  font-size: 11px;
  line-height: 1.3;
}

.monitor-metric.is-success > span i,
.monitor-metric.is-healthy > span i {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.16);
}

.monitor-metric.is-error > span i {
  background: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.16);
}

.monitor-metric.is-error > div strong {
  color: #dc2626;
}

.monitor-metric > div strong.is-running {
  color: var(--primary-color);
}

.monitor-metric.is-clickable {
  cursor: help;
}

.failure-task-tooltip {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 12px;
}

.job-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  background: color-mix(in srgb, #ffffff 82%, transparent);
  border: 1px solid var(--job-overview-border);
  border-radius: 16px;
  box-shadow: 0 8px 24px color-mix(in srgb, #1e3a8a 6%, transparent);
  backdrop-filter: blur(14px);
}

.job-toolbar {
  flex: 0 0 auto;
  min-height: 42px;
  padding: 0 4px 9px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--divider-color);
}

.job-content :deep(.ai-crud-page),
.job-content :deep(.ai-crud-main) {
  min-height: 0;
}

.job-content :deep(.ai-crud-page) {
  flex: 1;
  height: auto;
}

.job-content :deep(.ai-crud-main) {
  height: 100%;
}

.job-content :deep(.ai-search-box) {
  padding: 10px 12px 4px;
}

.job-content :deep(.execution-cell),
.job-content :deep(.schedule-cell),
.job-content :deep(.status-cell),
.job-content :deep(.time-cell),
.job-content :deep(.result-cell) {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 5px;
}

.job-content :deep(.status-cell) {
  align-items: flex-start;
}

.job-content :deep(.cell-main) {
  max-width: 100%;
  color: var(--text-color-1);
  font-size: 13px;
  font-weight: 600;
}

.job-content :deep(.schedule-heading) {
  min-width: 0;
  max-width: 100%;
  display: flex;
  align-items: center;
  gap: 7px;
}

.job-content :deep(.schedule-heading .cell-main) {
  min-width: 0;
}

.job-content :deep(.cell-secondary),
.job-content :deep(.sync-error) {
  max-width: 100%;
  color: var(--text-color-3);
  font-size: 12px;
}

.job-content :deep(.sync-error) {
  color: var(--error-color);
}

.job-content :deep(.failure-count) {
  color: var(--error-color);
  font-size: 12px;
}

:global(.job-log-modal) {
  width: min(1120px, 94vw);
}

:global(.dark) .job-config-page {
  --job-overview-bg: color-mix(in srgb, var(--bg-primary) 82%, transparent);
  --job-overview-border: color-mix(in srgb, var(--border-light) 80%, transparent);
  background:
    radial-gradient(42% 36% at 6% 0%, color-mix(in srgb, var(--primary-color, #2563eb) 24%, transparent), transparent 62%),
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color, #2563eb) 10%, var(--bg-secondary)) 0%, var(--bg-secondary) 48%);
}

:global(.dark) .job-content {
  background: color-mix(in srgb, var(--bg-primary) 86%, transparent);
}

@media (max-width: 768px) {
  .job-config-page {
    --job-table-max-height: 520px;
    height: auto;
    min-height: 100%;
    padding: 10px;
    gap: 10px;
    overflow-y: auto;
  }

  .monitor-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .monitor-heading-block {
    grid-column: 1 / -1;
    padding: 12px 14px;
  }

  .monitor-metric {
    padding: 12px 14px;
    border-top: 1px solid color-mix(in srgb, var(--border-light, var(--divider-color)) 78%, transparent);
  }

  .monitor-metric:nth-child(even) {
    border-left: 0;
  }

  .monitor-metric:nth-child(odd) {
    border-left: 1px solid color-mix(in srgb, var(--border-light, var(--divider-color)) 78%, transparent);
  }

  .job-content {
    padding: 8px;
  }

  .job-toolbar {
    overflow-x: auto;
  }
}

@media (min-width: 769px) and (max-width: 1120px) {
  .job-config-page {
    --job-table-max-height: calc(100vh - 410px);
  }

  .monitor-overview {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .monitor-heading-block {
    grid-column: 1 / -1;
    border-bottom: 1px solid color-mix(in srgb, var(--border-light, var(--divider-color)) 78%, transparent);
  }

  .monitor-heading-main {
    width: 100%;
    justify-content: flex-start;
  }

  .monitor-metric:nth-child(2) {
    border-left: 0;
  }
}
</style>
