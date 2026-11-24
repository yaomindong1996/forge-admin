<template>
  <div class="flow-monitor-page">
    <n-alert v-if="statistics.degraded" type="warning" class="monitor-alert" :show-icon="true">
      流程监控统计暂时不可用，请稍后重试。
      <template #action>
        <NButton text type="primary" @click="loadStatistics">
          重试
        </NButton>
      </template>
    </n-alert>

    <header class="monitor-page-header">
      <div class="header-copy">
        <h2>流程监控中心</h2>
        <p>实时监控流程运行状态，统计任务处理情况</p>
      </div>

      <nav class="header-tabs" aria-label="监控视图切换">
        <button
          v-for="tab in viewTabs"
          :key="tab.value"
          type="button"
          class="header-tab"
          :class="{ 'is-active': activeView === tab.value }"
          @click="activeView = tab.value"
        >
          {{ tab.label }}
        </button>
      </nav>

      <div class="header-range">
        <n-select
          v-model:value="chartPeriod"
          :options="periodOptions"
          size="small"
          class="period-select"
          @update:value="refreshCharts"
        />
        <n-date-picker
          v-model:value="searchForm.dateRange"
          type="daterange"
          clearable
          size="small"
          class="header-date-range"
          @update:value="handleSearch"
        />
      </div>
    </header>

    <section class="monitor-kpi" role="group" aria-label="流程监控摘要">
      <article class="kpi-card theme-running">
        <span class="kpi-icon" aria-hidden="true">
          <i class="i-lucide:play" />
        </span>
        <div class="kpi-copy">
          <small>运行中</small>
          <strong>{{ statistics.runningInstances || 0 }}</strong>
        </div>
      </article>
      <article class="kpi-card theme-pending">
        <span class="kpi-icon" aria-hidden="true">
          <i class="i-lucide:clipboard-list" />
        </span>
        <div class="kpi-copy">
          <small>待办</small>
          <strong>{{ statistics.pendingTasks || 0 }}</strong>
        </div>
      </article>
      <article class="kpi-card theme-done">
        <span class="kpi-icon" aria-hidden="true">
          <i class="i-lucide:check" />
        </span>
        <div class="kpi-copy">
          <small>今日完成</small>
          <strong>{{ statistics.todayCompleted || 0 }}</strong>
        </div>
      </article>
      <button
        type="button"
        class="kpi-card theme-timeout is-action"
        :class="{ 'is-active': searchForm.overdue === true }"
        title="按超时任务筛选"
        @click="filterByTimeout"
      >
        <span class="kpi-icon" aria-hidden="true">
          <i class="i-lucide:alarm-clock" />
        </span>
        <div class="kpi-copy">
          <small>超时</small>
          <strong>{{ statistics.timeoutTasks || 0 }}</strong>
        </div>
      </button>
    </section>

    <div v-show="activeView === 'monitor'" class="flow-workbench">
      <div class="panel-toolbar">
        <div class="toolbar-filters">
          <n-input
            v-model:value="searchForm.processName"
            placeholder="搜索流程名称"
            clearable
            class="filter-name"
            @keydown.enter="handleSearch"
          >
            <template #prefix>
              <i class="i-lucide:search" />
            </template>
          </n-input>
          <n-input
            v-model:value="searchForm.initiator"
            placeholder="发起人"
            clearable
            class="filter-initiator"
            @keydown.enter="handleSearch"
          />
          <n-select
            v-model:value="searchForm.status"
            :options="statusOptions"
            placeholder="流程状态"
            clearable
            class="filter-status"
          />
          <n-date-picker
            v-model:value="searchForm.dateRange"
            type="daterange"
            clearable
            class="filter-range"
          />
          <NButton type="primary" @click="handleSearch">
            查询
          </NButton>
          <NButton secondary @click="handleReset">
            重置
          </NButton>
        </div>
        <div class="toolbar-actions">
          <NButton
            v-if="canCleanup"
            type="error"
            secondary
            :loading="cleanupLoading"
            :disabled="isDeletingProcess"
            @click="handleCleanupCurrentFilter"
          >
            删除当前筛选
          </NButton>
          <NButton secondary :disabled="isDeletingProcess" @click="loadData">
            刷新
          </NButton>
        </div>
      </div>

      <div class="table-region">
        <n-alert v-if="dataError" type="error" class="monitor-alert" :show-icon="true">
          流程实例列表加载失败，请重试。
          <template #action>
            <NButton text type="primary" @click="loadData">
              重试
            </NButton>
          </template>
        </n-alert>
        <n-data-table
          size="medium"
          :columns="columns"
          :data="tableData"
          :loading="loading || isDeletingProcess"
          :pagination="pagination"
          :remote="true"
          :row-key="row => row.id"
          flex-height
          class="monitor-table"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- 流程实例错误日志抽屉 -->
    <n-drawer v-model:show="instanceErrorDrawerVisible" :width="900" title="流程错误日志">
      <n-drawer-content title="流程错误日志">
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <span>流程错误日志</span>
            <n-select
              v-model:value="instanceErrorStatusFilter"
              :options="errorStatusOptions"
              placeholder="错误状态"
              clearable
              size="small"
              style="width: 130px"
              @update:value="loadInstanceErrorLogs"
            />
          </NSpace>
        </template>
        <n-data-table
          :columns="errorColumns"
          :data="instanceErrorLogData"
          :loading="instanceErrorLogLoading"
          :pagination="instanceErrorPagination"
          :remote="true"
          :row-key="row => row.id"
          :scroll-x="1000"
          @update:page="handleInstanceErrorPageChange"
          @update:page-size="handleInstanceErrorPageSizeChange"
        />
      </n-drawer-content>
    </n-drawer>

    <!-- 错误日志详情弹窗 -->
    <n-modal v-model:show="errorDetailVisible" preset="card" title="错误详情" style="width: 800px;">
      <n-descriptions v-if="currentErrorLog" :column="2" label-placement="left" bordered>
        <n-descriptions-item label="流程实例ID">
          {{ currentErrorLog.processInstanceId }}
        </n-descriptions-item>
        <n-descriptions-item label="节点名称">
          {{ currentErrorLog.activityName || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="任务名称">
          {{ currentErrorLog.taskName || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="错误环节">
          {{ getErrorStageText(currentErrorLog.errorStage) }}
        </n-descriptions-item>
        <n-descriptions-item label="错误类型">
          {{ getErrorTypeText(currentErrorLog.errorType) }}
        </n-descriptions-item>
        <n-descriptions-item label="状态">
          <DictTag dict-type="flow_error_log_status" :value="currentErrorLog.status" />
        </n-descriptions-item>
        <n-descriptions-item label="重试次数">
          {{ currentErrorLog.retryCount || 0 }}
        </n-descriptions-item>
        <n-descriptions-item label="重试时间">
          {{ currentErrorLog.lastRetryTime || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="错误信息" :span="2">
          <n-text depth="3">
            {{ currentErrorLog.errorMessage || '-' }}
          </n-text>
        </n-descriptions-item>
        <n-descriptions-item label="堆栈信息" :span="2">
          <n-code :code="currentErrorLog.stackTrace || '无'" language="text" word-wrap style="max-height: 300px; overflow: auto" />
        </n-descriptions-item>
      </n-descriptions>
      <template #footer>
        <NSpace>
          <NButton v-if="canManage && currentErrorLog && (currentErrorLog.status === 0 || currentErrorLog.status === 3)" type="primary" @click="handleRetryError">
            重试节点
          </NButton>
          <NButton v-if="canManage && currentErrorLog && currentErrorLog.status === 0" type="warning" @click="handleResolveErrorLog">
            标记已解决
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 重试确认弹窗 -->
    <n-modal v-model:show="retryModalVisible" preset="card" title="确认重试" style="width: 450px;">
      <NSpace vertical>
        <n-text>确定要重试该节点吗？请填写重试原因：</n-text>
        <n-input
          v-model:value="retryReason"
          type="textarea"
          placeholder="请输入重试原因"
          :rows="3"
        />
      </NSpace>
      <template #footer>
        <NSpace>
          <NButton @click="retryModalVisible = false">
            取消
          </NButton>
          <NButton type="primary" :disabled="!retryReason.trim()" :loading="retrying" @click="confirmRetry">
            确认重试
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <div
      class="monitor-charts"
      :class="{
        'is-stats': activeView === 'stats',
        'is-analysis': activeView === 'analysis',
      }"
    >
      <section
        class="chart-panel"
        :class="{ 'is-primary': activeView === 'analysis' }"
      >
        <header class="chart-panel-head">
          <strong>任务处理趋势</strong>
          <n-radio-group v-model:value="chartPeriod" size="small" @update:value="refreshCharts">
            <n-radio-button :value="7">
              近7天
            </n-radio-button>
            <n-radio-button :value="30">
              近30天
            </n-radio-button>
          </n-radio-group>
        </header>
        <div class="chart-container">
          <div ref="taskChartRef" />
        </div>
      </section>
      <section
        class="chart-panel"
        :class="{ 'is-primary': activeView === 'stats' }"
      >
        <header class="chart-panel-head">
          <strong>流程分布统计</strong>
        </header>
        <div class="chart-container">
          <div ref="processChartRef" />
        </div>
      </section>
    </div>

    <!-- 流程图弹窗 -->
    <n-modal v-model:show="diagramModalVisible" preset="card" title="流程图" style="width: 90%; max-width: 1200px;">
      <DingFlowViewer
        v-if="diagramModalVisible && currentDiagramInstanceId"
        :process-instance-id="currentDiagramInstanceId"
      />
    </n-modal>

    <FlowTaskDetailShell
      v-model:show="detailDrawerVisible"
      :title="currentInstance.processName || '流程实例详情'"
      :subtitle="currentInstance.currentNode ? `当前节点：${currentInstance.currentNode}` : ''"
      :status-text="getInstanceStatusText(currentInstance.status)"
      :status-class="getInstanceStatusClass(currentInstance.status)"
      :status-icon="getInstanceStatusIcon(currentInstance.status)"
      :records="approvalHistory"
      record-title="审批记录"
      fullscreen
    >
      <template v-if="currentInstance?.id">
        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:info-outline" />
            基本信息
          </div>
          <div class="approval-field-grid">
            <div class="approval-field">
              <span class="approval-label">流程名称</span>
              <span class="approval-value">{{ currentInstance.processName || '-' }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">流程状态</span>
              <span class="approval-value">
                <DictTag dict-type="flow_instance_status" :value="currentInstance.status" />
              </span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起人</span>
              <span class="approval-value approval-user-inline">
                <UserAvatar :name="currentInstance.initiatorName || '未知'" :size="24" />
                {{ currentInstance.initiatorName || '-' }}
              </span>
            </div>
            <div class="approval-field">
              <span class="approval-label">发起时间</span>
              <span class="approval-value">{{ currentInstance.startTime || '-' }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">当前节点</span>
              <span class="approval-value">{{ currentInstance.currentNode || '-' }}</span>
            </div>
            <div class="approval-field">
              <span class="approval-label">处理人</span>
              <span class="approval-value">{{ currentInstance.currentAssignee || '-' }}</span>
            </div>
          </div>
        </section>

        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:account-tree" />
            审批任务上下文
          </div>
          <n-alert v-if="adminTaskContextError" type="error" class="mb-2" :show-icon="true">
            审批任务加载失败，请重试。
            <template #action>
              <NButton text type="primary" @click="loadAdminInstanceTasks">
                重试
              </NButton>
            </template>
          </n-alert>
          <n-data-table
            :columns="adminTaskColumns"
            :data="adminTaskContext"
            :loading="adminTaskContextLoading"
            :pagination="adminTaskPagination"
            :remote="true"
            :row-key="row => row.id || row.taskId"
            :bordered="false"
            size="small"
            @update:page="handleAdminTaskPageChange"
            @update:page-size="handleAdminTaskPageSizeChange"
          />
          <div v-if="adminTaskTree.length" class="approval-task-tree mt-3">
            <div class="approval-tree-title">
              任务节点关系
            </div>
            <n-tree :data="adminTaskTree" block-line selectable :default-expand-all="true" />
            <n-alert v-if="adminTaskTreeTruncated" type="warning" class="mt-2" :show-icon="true">
              流程任务超过 500 条，关系树仅展示前 500 条，分页列表仍可继续查询。
            </n-alert>
          </div>
        </section>

        <section class="approval-detail-section">
          <div class="approval-section-header">
            <i class="i-material-symbols:fact-check" />
            表单内容
          </div>
          <FlowReadonlyFormPanel :row="monitorFormRow" source="flowMonitor" />
        </section>

        <section class="approval-detail-section">
          <n-collapse arrow-placement="right">
            <n-collapse-item title="查看流程图" name="diagram">
              <div class="approval-diagram">
                <DingFlowViewer
                  v-if="currentInstance.id"
                  :process-instance-id="currentInstance.id"
                  :compact="true"
                />
              </div>
            </n-collapse-item>
          </n-collapse>
        </section>
      </template>
    </FlowTaskDetailShell>

    <!-- 流程变量查看弹窗 -->
    <n-modal v-model:show="variablesModalVisible" preset="card" title="流程变量" style="width: 800px;">
      <n-spin :show="variablesLoading">
        <n-empty v-if="!variablesLoading && Object.keys(processVariables).length === 0" description="暂无流程变量" />
        <n-descriptions v-else :column="1" label-placement="left" bordered>
          <n-descriptions-item v-for="(value, key) in processVariables" :key="key" :label="key">
            <n-text code>
              {{ formatVariableValue(value) }}
            </n-text>
          </n-descriptions-item>
        </n-descriptions>
      </n-spin>
    </n-modal>

    <n-modal v-model:show="adminActionsModalVisible" preset="card" title="管理员干预" style="width: 600px;">
      <n-alert v-if="isSuspendedCurrent" type="warning" class="monitor-admin-alert">
        流程已挂起。可以激活或终止；回退和转派需要先激活。
      </n-alert>
      <NSpace class="monitor-admin-status-actions">
        <NButton v-if="canMutateCurrent" type="warning" :loading="isAdminMutating('suspend')" :disabled="isAdminMutationBusy" @click="handleSuspend">
          挂起流程
        </NButton>
        <NButton v-if="isSuspendedCurrent" type="primary" :loading="isAdminMutating('activate')" :disabled="isAdminMutationBusy" @click="handleActivate">
          激活流程
        </NButton>
      </NSpace>
      <NSpace vertical size="large">
        <n-card title="终止流程" size="small">
          <template #header-extra>
            <NTag type="error" size="small">
              危险操作
            </NTag>
          </template>
          <NSpace vertical>
            <n-text depth="3">
              终止后流程立即结束，无法恢复。
            </n-text>
            <n-input
              v-model:value="terminateReason"
              type="textarea"
              placeholder="请输入终止原因"
              :rows="2"
            />
            <NButton type="error" :loading="isAdminMutating('terminate')" :disabled="!terminateReason.trim() || isAdminMutationBusy" @click="confirmTerminate">
              确认终止
            </NButton>
          </NSpace>
        </n-card>

        <n-card title="节点回退" size="small">
          <NSpace vertical>
            <n-text depth="3">
              {{ canMutateCurrent ? '将流程回退到指定的历史节点。' : '请先激活流程后再回退。' }}
            </n-text>
            <n-select
              v-model:value="rollbackTargetActivity"
              :options="activityOptions"
              placeholder="请选择目标节点"
              :loading="activitiesLoading"
              :disabled="!canMutateCurrent"
            />
            <n-input
              v-model:value="rollbackReason"
              type="textarea"
              placeholder="请输入回退原因"
              :rows="2"
              :disabled="!canMutateCurrent"
            />
            <NButton type="warning" :loading="isAdminMutating('rollback')" :disabled="!canMutateCurrent || !rollbackTargetActivity || isAdminMutationBusy" @click="confirmRollback">
              确认回退
            </NButton>
          </NSpace>
        </n-card>

        <n-card title="任务转派" size="small">
          <NSpace vertical>
            <n-text depth="3">
              {{ canMutateCurrent ? '将当前任务转派给其他用户处理。' : '请先激活流程后再转派。' }}
            </n-text>
            <n-select
              v-if="currentTaskOptions.length > 1"
              v-model:value="currentTaskId"
              :options="currentTaskOptions"
              placeholder="请选择要转派的任务"
              :disabled="!canMutateCurrent"
            />
            <n-input
              v-model:value="reassignUserName"
              placeholder="请选择新处理人"
              readonly
              :disabled="!canMutateCurrent"
              @click="openReassignUserSelect"
            >
              <template #suffix>
                <i class="i-material-symbols:person-search cursor-pointer" @click="openReassignUserSelect" />
              </template>
            </n-input>
            <n-input
              v-model:value="reassignReason"
              type="textarea"
              placeholder="请输入转派原因"
              :rows="2"
              :disabled="!canMutateCurrent"
            />
            <NButton type="primary" :loading="isAdminMutating('reassign')" :disabled="!canMutateCurrent || !reassignUserId || isAdminMutationBusy" @click="confirmReassign">
              确认转派
            </NButton>
          </NSpace>
        </n-card>
      </NSpace>
    </n-modal>

    <!-- 用户选择弹窗 -->
    <UserSelectModal
      v-model:show="userSelectModalVisible"
      title="选择转派用户"
      :multiple="false"
      @confirm="handleUserSelect"
    />
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import DictTag from '@/components/DictTag.vue'
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowReadonlyFormPanel from '@/components/flow/FlowReadonlyFormPanel.vue'
import FlowTaskDetailShell from '@/components/flow/FlowTaskDetailShell.vue'
import { useFlowMonitor } from './composables/useFlowMonitor'

defineOptions({ name: 'FlowMonitor' })

const activeView = ref('monitor')
const viewTabs = [
  { label: '流程监控', value: 'monitor' },
  { label: '数据统计', value: 'stats' },
  { label: '运行分析', value: 'analysis' },
]
const periodOptions = [
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
]

const {
  adminActionsModalVisible,
  adminTaskColumns,
  adminTaskContext,
  adminTaskContextError,
  adminTaskContextLoading,
  adminTaskPagination,
  adminTaskTree,
  adminTaskTreeTruncated,
  approvalHistory,
  activitiesLoading,
  activityOptions,
  canCleanup,
  canManage,
  canMutateCurrent,
  chartPeriod,
  cleanupLoading,
  columns,
  confirmReassign,
  confirmRetry,
  confirmRollback,
  confirmTerminate,
  currentDiagramInstanceId,
  currentErrorLog,
  currentInstance,
  currentTaskId,
  currentTaskOptions,
  dataError,
  detailDrawerVisible,
  diagramModalVisible,
  errorColumns,
  errorDetailVisible,
  errorStatusOptions,
  filterByTimeout,
  formatVariableValue,
  getErrorStageText,
  getErrorTypeText,
  getInstanceStatusClass,
  getInstanceStatusIcon,
  getInstanceStatusText,
  handleActivate,
  handleAdminTaskPageChange,
  handleAdminTaskPageSizeChange,
  handleCleanupCurrentFilter,
  handleInstanceErrorPageChange,
  handleInstanceErrorPageSizeChange,
  handlePageChange,
  handlePageSizeChange,
  handleResolveErrorLog,
  handleReset,
  handleRetryError,
  handleSearch,
  handleSuspend,
  handleUserSelect,
  instanceErrorDrawerVisible,
  instanceErrorLogData,
  instanceErrorLogLoading,
  instanceErrorPagination,
  instanceErrorStatusFilter,
  isAdminMutating,
  isAdminMutationBusy,
  isDeletingProcess,
  isSuspendedCurrent,
  loadAdminInstanceTasks,
  loadData,
  loadInstanceErrorLogs,
  loadStatistics,
  loading,
  monitorFormRow,
  openReassignUserSelect,
  pagination,
  processChartRef,
  processVariables,
  reassignReason,
  reassignUserId,
  reassignUserName,
  refreshCharts,
  retryModalVisible,
  retryReason,
  retrying,
  rollbackReason,
  rollbackTargetActivity,
  searchForm,
  statistics,
  statusOptions,
  tableData,
  taskChartRef,
  terminateReason,
  userSelectModalVisible,
  variablesLoading,
  variablesModalVisible,
} = useFlowMonitor()

watch(activeView, async () => {
  await nextTick()
  window.dispatchEvent(new Event('resize'))
})
</script>

<style scoped src="./flowMonitor.css"></style>
