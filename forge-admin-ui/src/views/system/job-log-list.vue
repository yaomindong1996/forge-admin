<template>
  <div class="job-log-list">
    <section v-if="jobConfigId" class="overview-strip">
      <NSpin :show="overviewLoading" size="small">
        <div class="overview-content">
          <div class="overview-identity">
            <span class="overview-name">{{ overview.jobName || jobName || '当前任务' }}</span>
            <span class="overview-group">{{ overview.jobGroup || '-' }}</span>
          </div>
          <div class="overview-metrics">
            <div class="overview-metric">
              <span>最近状态</span>
              <DictTag
                v-if="overview.lastExecutionStatus != null"
                :options="jobStatusOptions"
                :value="String(overview.lastExecutionStatus)"
                size="small"
              />
              <strong v-else>尚未执行</strong>
            </div>
            <div class="overview-metric">
              <span>下次执行</span>
              <strong>{{ formatDateTime(overview.nextFireTime) }}</strong>
            </div>
            <div class="overview-metric" :class="{ 'is-risk': overview.consecutiveFailures > 0 }">
              <span>连续失败</span>
              <strong>{{ overview.consecutiveFailures || 0 }}</strong>
            </div>
          </div>
          <div v-if="overview.recentExecutions?.length" class="recent-executions">
            <span>最近 5 次</span>
            <DictTag
              v-for="item in overview.recentExecutions"
              :key="item.id"
              :options="jobStatusOptions"
              :value="String(item.status)"
              size="small"
            />
          </div>
        </div>
      </NSpin>
    </section>

    <section class="records-panel">
      <header class="log-toolbar">
        <div class="filter-fields">
          <NInput
            v-if="!jobConfigId"
            v-model:value="filters.jobName"
            clearable
            placeholder="搜索任务名称"
            class="filter-control filter-task"
            @keyup.enter="handleSearch"
          />
          <NInput
            v-if="!jobConfigId"
            v-model:value="filters.jobGroup"
            clearable
            placeholder="任务分组"
            class="filter-control filter-group"
            @keyup.enter="handleSearch"
          />
          <NSelect
            v-model:value="filters.status"
            clearable
            placeholder="全部状态"
            :options="jobStatusOptions"
            class="filter-control filter-select"
          />
          <NSelect
            v-model:value="filters.triggerType"
            clearable
            placeholder="全部来源"
            :options="triggerTypeOptions"
            class="filter-control filter-select"
          />
          <NDatePicker
            v-model:value="filters.dateRange"
            type="datetimerange"
            clearable
            placeholder="执行时间范围"
            class="filter-control filter-time"
          />
        </div>
        <div class="toolbar-actions">
          <NButton quaternary @click="resetFilters">
            重置
          </NButton>
          <NButton type="primary" @click="handleSearch">
            <template #icon>
              <i class="i-material-symbols:search-rounded" />
            </template>
            查询
          </NButton>
          <NButton v-if="canExport" secondary :loading="exportLoading" @click="handleExport">
            <template #icon>
              <i class="i-material-symbols:download-rounded" />
            </template>
            导出
          </NButton>
        </div>
      </header>

      <div class="records-heading">
        <div>
          <strong>执行记录</strong>
          <span>共 {{ pagination.itemCount }} 条</span>
          <span v-if="activeFilterCount">已筛选 {{ activeFilterCount }} 项</span>
        </div>
        <span v-if="canViewDetail" class="records-hint">
          <i class="i-material-symbols:touch-app-outline-rounded" />
          点击记录查看详情
        </span>
      </div>

      <NDataTable
        :columns="columns"
        :data="logList"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        :row-props="rowProps"
        :bordered="false"
        :single-line="false"
        size="small"
        :max-height="520"
        :scroll-x="jobConfigId ? 680 : 900"
      />
    </section>

    <NDrawer
      v-model:show="detailVisible"
      width="min(680px, 100vw)"
      placement="right"
      :style="detailThemeStyle"
      :mask-closable="false"
    >
      <NDrawerContent class="job-log-detail-drawer" :native-scrollbar="false" closable>
        <template #header>
          <div class="drawer-title">
            <strong>执行详情</strong>
            <span>{{ detail?.jobName || '任务运行记录' }}</span>
          </div>
        </template>

        <NSpin :show="detailLoading">
          <template v-if="detail">
            <section class="detail-hero">
              <div class="detail-identity">
                <div>
                  <span>{{ detail.jobGroup || '未分组' }}</span>
                  <h3>{{ detail.jobName || '-' }}</h3>
                </div>
                <div class="detail-tags">
                  <DictTag
                    :options="jobStatusOptions"
                    :value="String(detail.status)"
                    size="small"
                  />
                  <DictTag
                    :options="triggerTypeOptions"
                    :value="detail.triggerType"
                    size="small"
                  />
                </div>
              </div>
              <div class="detail-metrics">
                <div>
                  <span>执行时长</span>
                  <strong>{{ formatDuration(detail.duration) }}</strong>
                </div>
                <div>
                  <span>重试次数</span>
                  <strong>{{ detail.retryCount || 0 }}</strong>
                </div>
                <div>
                  <span>开始时间</span>
                  <strong>{{ formatDateTime(resolveJobLogStartedAt(detail)) }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-heading">
                <strong>执行时间线</strong>
                <span>从计划触发到执行结束</span>
              </div>
              <div class="execution-timeline">
                <div class="timeline-item">
                  <i />
                  <span>计划时间</span>
                  <strong>{{ formatDateTime(detail.scheduledFireTime) }}</strong>
                </div>
                <div class="timeline-item">
                  <i />
                  <span>触发时间</span>
                  <strong>{{ formatDateTime(detail.triggerTime) }}</strong>
                </div>
                <div class="timeline-item">
                  <i />
                  <span>开始时间</span>
                  <strong>{{ formatDateTime(detail.startTime) }}</strong>
                </div>
                <div class="timeline-item" :class="{ 'is-pending': !detail.endTime }">
                  <i />
                  <span>结束时间</span>
                  <strong>{{ formatDateTime(detail.endTime) }}</strong>
                </div>
              </div>
            </section>

            <section v-if="detail.resultSummary || detail.exceptionSummary" class="detail-section summary-section">
              <div class="detail-section-heading">
                <strong>执行摘要</strong>
                <span>仅展示经过安全裁剪的结果</span>
              </div>
              <section v-if="detail.resultSummary" class="detail-block">
                <h4>执行结果</h4>
                <pre>{{ detail.resultSummary }}</pre>
              </section>
              <section v-if="detail.exceptionSummary" class="detail-block is-error">
                <h4>异常信息</h4>
                <pre>{{ detail.exceptionSummary }}</pre>
              </section>
            </section>
            <NEmpty v-else description="本次执行没有结果或异常摘要" class="detail-empty" />

            <NCollapse class="technical-collapse" arrow-placement="right">
              <NCollapseItem title="技术信息" name="technical">
                <dl class="technical-grid">
                  <div>
                    <dt>执行器</dt>
                    <dd>{{ detail.executorHandler || '-' }}</dd>
                  </div>
                  <div>
                    <dt>执行实例</dt>
                    <dd>{{ detail.fireInstanceId || '-' }}</dd>
                  </div>
                  <div v-if="detail.processInstanceId" class="technical-process">
                    <dt>流程实例</dt>
                    <dd>
                      <span>{{ detail.processInstanceId }}</span>
                      <NButton
                        v-if="canViewFlowHistory"
                        text
                        type="primary"
                        size="small"
                        @click="openFlowHistory"
                      >
                        查看流程
                      </NButton>
                    </dd>
                  </div>
                </dl>
              </NCollapseItem>
            </NCollapse>
          </template>
          <NEmpty v-else-if="!detailLoading" description="未获取到执行详情" class="detail-empty" />
        </NSpin>

        <template #footer>
          <div class="drawer-actions">
            <NButton
              v-if="detail?.processInstanceId && canViewFlowHistory"
              type="primary"
              secondary
              @click="openFlowHistory"
            >
              <template #icon>
                <i class="i-material-symbols:account-tree-outline-rounded" />
              </template>
              流程历史
            </NButton>
            <NButton @click="detailVisible = false">
              关闭
            </NButton>
          </div>
        </template>
      </NDrawerContent>
    </NDrawer>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NDataTable,
  NDatePicker,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NInput,
  NSelect,
  NSpin,
  useThemeVars,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { exportJobLogs, getJobLogDetail, getJobOverview } from '@/api/system/job'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { usePermissionStore, useUserStore } from '@/store'
import { request } from '@/utils'
import { hasJobPermission, JOB_PERMISSIONS } from './job-config/job-permission'
import { buildJobLogQuery, resolveJobLogStartedAt } from './job-log-query'
import { hasAccessibleRoute } from './job-view-contract'

const props = defineProps({
  jobConfigId: {
    type: [Number, String],
    default: null,
  },
  jobName: {
    type: String,
    default: '',
  },
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const themeVars = useThemeVars()
const { dict } = useDict('sys_job_log_status', 'sys_job_trigger_type')
const jobStatusOptions = computed(() => dict.value.sys_job_log_status || [])
const triggerTypeOptions = computed(() => dict.value.sys_job_trigger_type || [])
const canViewDetail = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.logDetail))
const canExport = computed(() => hasJobPermission(userStore, route, JOB_PERMISSIONS.logExport))
const canViewFlowHistory = computed(() => hasAccessibleRoute(
  permissionStore.accessRoutes,
  '/flow/monitor',
))
const detailThemeStyle = computed(() => ({
  '--border-color': themeVars.value.borderColor,
  '--card-color': themeVars.value.cardColor,
  '--divider-color': themeVars.value.dividerColor,
  '--error-color': themeVars.value.errorColor,
  '--primary-color': themeVars.value.primaryColor,
  '--table-color-hover': themeVars.value.hoverColor,
  '--text-color-1': themeVars.value.textColor1,
  '--text-color-2': themeVars.value.textColor2,
  '--text-color-3': themeVars.value.textColor3,
}))

const filters = reactive({
  jobName: '',
  jobGroup: '',
  status: null,
  triggerType: null,
  dateRange: null,
})
const loading = ref(false)
const exportLoading = ref(false)
const overviewLoading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const logList = ref([])
const detail = ref(null)
const overview = ref({ recentExecutions: [], consecutiveFailures: 0 })
const activeFilterCount = computed(() => [
  !props.jobConfigId && filters.jobName,
  !props.jobConfigId && filters.jobGroup,
  filters.status !== null && filters.status !== undefined && filters.status !== '',
  filters.triggerType,
  Array.isArray(filters.dateRange) && filters.dateRange.length === 2,
].filter(Boolean).length)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  prefix: info => `共 ${info.itemCount} 条`,
  onChange: (page) => {
    pagination.page = page
    loadLogList()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadLogList()
  },
})

const columns = computed(() => {
  const tableColumns = []
  if (!props.jobConfigId) {
    tableColumns.push({
      title: '任务',
      key: 'jobName',
      width: 220,
      render: row => h('div', { class: 'task-cell' }, [
        h('strong', row.jobName || '-'),
        h('span', row.jobGroup || '未分组'),
      ]),
    })
  }

  tableColumns.push(
    {
      title: '状态 / 来源',
      key: 'status',
      width: 170,
      render: row => h('div', { class: 'status-source-cell' }, [
        h(DictTag, {
          options: jobStatusOptions.value,
          value: String(row.status),
          size: 'small',
        }),
        h('span', { class: 'trigger-source' }, [
          h('i', { class: 'i-material-symbols:bolt-outline-rounded' }),
          getDictLabel(triggerTypeOptions.value, row.triggerType),
        ]),
      ]),
    },
    {
      title: '开始时间',
      key: 'startTime',
      width: 174,
      render: row => renderDateTimeCell(resolveJobLogStartedAt(row)),
    },
    {
      title: '执行时长',
      key: 'duration',
      width: 112,
      render: row => h('span', { class: 'duration-value' }, formatDuration(row.duration)),
    },
    {
      title: '重试',
      key: 'retryCount',
      width: 86,
      render: (row) => {
        const retryCount = Number(row.retryCount || 0)
        return h('span', {
          class: ['retry-value', { 'has-retry': retryCount > 0 }],
        }, retryCount > 0 ? `${retryCount} 次` : '—')
      },
    },
  )

  if (canViewDetail.value) {
    tableColumns.push({
      title: '操作',
      key: 'action',
      width: 92,
      fixed: 'right',
      render: row => h(NButton, {
        text: true,
        type: 'primary',
        size: 'small',
        onClick: (event) => {
          event.stopPropagation()
          handleViewDetail(row)
        },
      }, { default: () => '查看详情' }),
    })
  }
  return tableColumns
})

function getDictLabel(options, value) {
  return options.find(option => String(option.value) === String(value))?.label || (value ?? '-')
}

function renderDateTimeCell(value) {
  if (!value)
    return h('span', { class: 'empty-value' }, '—')
  const parsed = dayjs(value)
  if (!parsed.isValid())
    return h('span', { class: 'empty-value' }, '-')
  return h('time', { class: 'date-time-cell', datetime: parsed.toISOString() }, [
    h('strong', parsed.format('YYYY-MM-DD')),
    h('span', parsed.format('HH:mm:ss')),
  ])
}

function rowProps(row) {
  if (!canViewDetail.value)
    return {}
  return {
    class: 'is-detail-row',
    tabindex: 0,
    onClick: () => handleViewDetail(row),
    onKeydown: (event) => {
      if (event.target !== event.currentTarget || !['Enter', ' '].includes(event.key))
        return
      event.preventDefault()
      handleViewDetail(row)
    },
  }
}

function currentQuery() {
  return buildJobLogQuery(filters, {
    jobConfigId: props.jobConfigId,
    jobName: props.jobName,
  })
}

async function loadLogList() {
  loading.value = true
  try {
    const response = await request.get('/job/log/page', {
      params: {
        pageNum: pagination.page,
        pageSize: pagination.pageSize,
        ...currentQuery(),
      },
    })
    logList.value = response.data?.records || response.data?.list || []
    pagination.itemCount = Number(response.data?.total || 0)
  }
  catch (error) {
    console.error('加载任务日志失败:', error)
    window.$message.error('加载任务日志失败')
  }
  finally {
    loading.value = false
  }
}

async function loadOverview() {
  if (!props.jobConfigId) {
    overview.value = { recentExecutions: [], consecutiveFailures: 0 }
    return
  }
  overviewLoading.value = true
  try {
    const response = await getJobOverview(props.jobConfigId)
    overview.value = response.data || { recentExecutions: [], consecutiveFailures: 0 }
  }
  catch (error) {
    console.error('加载任务概览失败:', error)
    window.$message.error('加载任务概览失败')
  }
  finally {
    overviewLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadLogList()
}

function resetFilters() {
  filters.jobName = ''
  filters.jobGroup = ''
  filters.status = null
  filters.triggerType = null
  filters.dateRange = null
  handleSearch()
}

async function handleViewDetail(row) {
  if (!canViewDetail.value)
    return
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const response = await getJobLogDetail(row.id)
    detail.value = response.data || null
  }
  catch (error) {
    console.error('加载日志详情失败:', error)
    window.$message.error('加载日志详情失败')
  }
  finally {
    detailLoading.value = false
  }
}

function openFlowHistory() {
  if (!canViewFlowHistory.value || !detail.value?.processInstanceId)
    return
  detailVisible.value = false
  router.push({
    path: '/flow/monitor',
    query: { processInstanceId: String(detail.value.processInstanceId) },
  })
}

async function handleExport() {
  if (!canExport.value)
    return
  exportLoading.value = true
  try {
    const response = await exportJobLogs(currentQuery())
    downloadBlobResponse(response, '定时任务执行日志.xlsx')
    window.$message.success('日志导出成功')
  }
  catch (error) {
    console.error('导出任务日志失败:', error)
    window.$message.error(error?.message || '导出任务日志失败')
  }
  finally {
    exportLoading.value = false
  }
}

function downloadBlobResponse(response, fallbackName) {
  const blob = response?.data instanceof Blob ? response.data : response
  if (!(blob instanceof Blob))
    throw new TypeError('下载响应不是文件流')

  const disposition = response?.headers?.['content-disposition']
    || response?.headers?.get?.('content-disposition')
    || ''
  const utf8Match = disposition.match(/filename\*=utf-8''([^;]+)/i)
  const filename = utf8Match?.[1] ? decodeURIComponent(utf8Match[1]) : fallbackName
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function formatDateTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function formatDuration(value) {
  if (value === null || value === undefined)
    return '-'
  if (value < 1000)
    return `${value} ms`
  return `${(value / 1000).toFixed(value < 10000 ? 1 : 0)} s`
}

async function refresh() {
  await Promise.all([loadOverview(), loadLogList()])
}

watch(() => props.jobConfigId, () => {
  pagination.page = 1
  refresh()
})

onMounted(refresh)

defineExpose({ refresh })
</script>

<style scoped>
.job-log-list {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overview-strip,
.records-panel {
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--card-color);
}

.overview-strip {
  padding: 12px 14px;
  border-left: 3px solid var(--primary-color);
}

.overview-content,
.overview-metrics,
.recent-executions,
.log-toolbar,
.filter-fields {
  display: flex;
  align-items: center;
}

.overview-content {
  min-height: 44px;
  gap: 18px;
}

.overview-identity {
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.overview-name {
  color: var(--text-color-1);
  font-size: 14px;
  font-weight: 650;
}

.overview-group,
.overview-metric span,
.recent-executions > span,
:deep(.task-cell span) {
  color: var(--text-color-3);
  font-size: 12px;
}

.overview-metrics {
  gap: 0;
}

.overview-metric {
  min-width: 120px;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-left: 1px solid var(--divider-color);
}

.overview-metric strong {
  color: var(--text-color-1);
  font-size: 13px;
}

.overview-metric.is-risk strong {
  color: var(--error-color);
}

.recent-executions {
  min-width: 0;
  margin-left: auto;
  gap: 6px;
}

.log-toolbar {
  padding: 11px 12px;
  justify-content: space-between;
  gap: 10px;
  background: color-mix(in srgb, var(--card-color) 94%, var(--body-color));
  border-bottom: 1px solid var(--divider-color);
}

.filter-fields {
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-control {
  flex: 0 0 auto;
}

.filter-task {
  width: 190px;
}

.filter-group,
.filter-select {
  width: 136px;
}

.filter-time {
  width: 318px;
}

.toolbar-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.records-heading {
  min-height: 42px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--divider-color);
}

.records-heading > div,
.records-hint {
  display: flex;
  align-items: center;
  gap: 10px;
}

.records-heading strong {
  color: var(--text-color-1);
  font-size: 13px;
  font-weight: 650;
}

.records-heading span,
.records-hint {
  color: var(--text-color-3);
  font-size: 12px;
}

.records-heading > div > span + span {
  position: relative;
  padding-left: 10px;
}

.records-heading > div > span + span::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 1px;
  height: 12px;
  background: var(--divider-color);
  content: '';
  transform: translateY(-50%);
}

.records-hint {
  gap: 5px;
}

.records-hint i {
  font-size: 15px;
}

:deep(.n-data-table-th) {
  background: color-mix(in srgb, var(--card-color) 92%, var(--body-color));
  color: var(--text-color-2);
  font-size: 12px;
  font-weight: 600;
}

:deep(.n-data-table-tr.is-detail-row) {
  cursor: pointer;
}

:deep(.n-data-table-tr.is-detail-row:focus-visible td) {
  background: color-mix(in srgb, var(--primary-color) 7%, var(--card-color));
  box-shadow: inset 0 0 0 1px var(--primary-color);
  outline: none;
}

:deep(.task-cell) {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

:deep(.task-cell strong) {
  overflow: hidden;
  color: var(--text-color-1);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-source-cell,
.date-time-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.trigger-source {
  max-width: 100%;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  color: var(--text-color-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trigger-source i {
  flex: 0 0 auto;
  font-size: 13px;
}

.date-time-cell strong,
.date-time-cell span,
.duration-value,
.retry-value {
  font-variant-numeric: tabular-nums;
}

.date-time-cell strong {
  color: var(--text-color-1);
  font-size: 12px;
  font-weight: 550;
}

.date-time-cell span {
  color: var(--text-color-3);
  font-size: 11px;
}

.duration-value {
  color: var(--text-color-2);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}

.retry-value,
.empty-value {
  color: var(--text-color-3);
  font-size: 12px;
}

.retry-value.has-retry {
  color: var(--error-color);
  font-weight: 600;
}

.drawer-title {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-title strong {
  color: var(--text-color-1);
  font-size: 15px;
}

.drawer-title span {
  overflow: hidden;
  color: var(--text-color-3);
  font-size: 12px;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-log-detail-drawer :deep(.n-drawer-body-content-wrapper) {
  padding: 16px 18px 22px;
}

.detail-hero {
  overflow: hidden;
  background: color-mix(in srgb, var(--card-color) 92%, var(--table-color-hover));
  border: 1px solid var(--border-color);
  border-radius: 7px;
}

.detail-identity {
  padding: 15px 16px 13px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-identity > div:first-child {
  min-width: 0;
}

.detail-identity span {
  color: var(--text-color-3);
  font-size: 11px;
}

.detail-identity h3 {
  margin: 3px 0 0;
  overflow: hidden;
  color: var(--text-color-1);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-tags {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-metrics {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1.7fr;
  border-top: 1px solid var(--divider-color);
}

.detail-metrics > div {
  min-width: 0;
  padding: 11px 16px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.detail-metrics > div + div {
  border-left: 1px solid var(--divider-color);
}

.detail-metrics span,
.detail-section-heading span {
  color: var(--text-color-3);
  font-size: 11px;
}

.detail-metrics strong {
  overflow: hidden;
  color: var(--text-color-1);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-section {
  padding-top: 20px;
}

.detail-section-heading {
  margin-bottom: 13px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.detail-section-heading strong {
  color: var(--text-color-1);
  font-size: 13px;
  font-weight: 650;
}

.execution-timeline {
  padding: 2px 4px 0;
}

.timeline-item {
  position: relative;
  min-height: 36px;
  padding: 0 0 14px;
  display: grid;
  grid-template-columns: 14px 78px minmax(0, 1fr);
  align-items: start;
  gap: 8px;
}

.timeline-item:not(:last-child)::after {
  position: absolute;
  top: 10px;
  bottom: -2px;
  left: 5px;
  width: 1px;
  background: var(--divider-color);
  content: '';
}

.timeline-item > i {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  margin-top: 4px;
  background: var(--card-color);
  border: 2px solid var(--primary-color);
  border-radius: 50%;
}

.timeline-item.is-pending > i {
  border-color: var(--border-color);
}

.timeline-item > span {
  color: var(--text-color-3);
  font-size: 12px;
  line-height: 18px;
}

.timeline-item > strong {
  color: var(--text-color-2);
  font-size: 12px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  line-height: 18px;
  text-align: right;
}

.detail-block + .detail-block {
  margin-top: 12px;
}

.detail-block h4 {
  margin: 0 0 8px;
  color: var(--text-color-1);
  font-size: 13px;
}

.detail-block pre {
  max-height: 240px;
  margin: 0;
  padding: 11px 12px;
  overflow: auto;
  color: var(--text-color-2);
  background: var(--table-color-hover);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-block.is-error h4,
.detail-block.is-error pre {
  color: var(--error-color);
}

.detail-block.is-error pre {
  background: color-mix(in srgb, var(--error-color) 5%, var(--card-color));
  border-color: color-mix(in srgb, var(--error-color) 22%, var(--border-color));
}

.detail-empty {
  padding: 30px 0 18px;
}

.technical-collapse {
  margin-top: 18px;
  border-top: 1px solid var(--divider-color);
}

.technical-collapse :deep(.n-collapse-item__header-main) {
  color: var(--text-color-2);
  font-size: 12px;
  font-weight: 600;
}

.technical-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.technical-grid > div {
  min-width: 0;
  padding: 10px 12px;
}

.technical-grid > div:nth-child(even) {
  border-left: 1px solid var(--divider-color);
}

.technical-grid > div:nth-child(n + 3) {
  border-top: 1px solid var(--divider-color);
}

.technical-grid dt {
  margin-bottom: 4px;
  color: var(--text-color-3);
  font-size: 11px;
}

.technical-grid dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--text-color-2);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
}

.technical-grid .technical-process {
  grid-column: 1 / -1;
}

.technical-process dd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.technical-process dd > span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 900px) {
  .overview-content {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .recent-executions {
    width: 100%;
    margin-left: 0;
  }

  .log-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .job-log-list {
    gap: 10px;
  }

  .overview-strip {
    padding: 11px 12px;
  }

  .overview-identity {
    min-width: 0;
    width: 100%;
  }

  .overview-metrics {
    width: 100%;
    justify-content: space-between;
  }

  .overview-metric {
    min-width: 0;
    flex: 1;
    padding: 0 10px;
  }

  .filter-control,
  .filter-time {
    width: 100%;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions > :deep(.n-button) {
    flex: 1;
  }

  .records-heading {
    min-height: 40px;
  }

  .records-hint {
    display: none;
  }

  .job-log-detail-drawer :deep(.n-drawer-body-content-wrapper) {
    padding: 14px 14px 20px;
  }

  .detail-identity {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .detail-tags {
    align-self: flex-start;
  }

  .detail-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-metrics > div:last-child {
    grid-column: 1 / -1;
    border-top: 1px solid var(--divider-color);
    border-left: 0;
  }

  .detail-section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .technical-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .technical-grid > div:nth-child(even) {
    border-left: 0;
  }

  .technical-grid > div + div {
    border-top: 1px solid var(--divider-color);
  }
}
</style>
