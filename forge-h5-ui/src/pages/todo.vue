<template>
  <view class="todo-page">
    <AiFeedbackHost />
    <AiTabHeader title="待办" :searchable="false">
      <template #actions>
        <button class="todo-start-button" aria-label="发起审批" @click="openApprovalStart">
          <AiIcon icon="/static/icons/ai-icon/plus.svg" color="#171a1d" size="md" />
        </button>
      </template>
    </AiTabHeader>
    <view class="todo-content">
      <!-- 查询与范围切换 -->
      <view class="todo-tools">
        <view class="todo-query-row">
          <AiSearchBar v-model="keyword" :placeholder="isCcScope ? '搜索抄送标题' : '搜索标题或申请人'" @search="handleSearch" @clear="clearSearch" />
          <!-- 抄送接口只支持标题与已读筛选，已读筛选放在抄送面板内 -->
          <button v-if="!isCcScope" class="todo-filter-trigger" :class="{ 'is-active': activeFilterCount }" @click="openFilters">
            <AiIcon icon="/static/icons/ai-icon/filter.svg" :color="activeFilterCount ? '#0066ff' : '#747677'" size="sm" />
            <text>筛选{{ activeFilterCount ? ` ${activeFilterCount}` : '' }}</text>
          </button>
        </view>
        <!-- 任务范围切换 -->
        <view class="work-scope-tabs">
          <button v-for="scope in workScopes" :key="scope.value" class="work-scope-tab" :class="{ active: activeScope === scope.value }" @click="setScope(scope.value)">
            {{ scope.label }}
            <text v-if="scope.value === 'todo' && activeScope === 'todo' && total" class="scope-count">{{ total > 99 ? '99+' : total }}</text>
            <!-- 抄送未读数只显示在本页签，不计入底栏"待办"角标 -->
            <text v-if="scope.value === 'cc' && ccStore.unreadText" class="scope-count">{{ ccStore.unreadText }}</text>
          </button>
        </view>
      </view>

      <CcListPanel v-if="isCcScope" ref="ccPanelRef" :keyword="keyword" />

      <!-- 任务卡整卡直达审批；撤回、催办是独立操作。 -->
      <scroll-view v-else class="todo-list" scroll-y :show-scrollbar="false" refresher-enabled :refresher-triggered="pullRefreshing" @refresherrefresh="refreshByPull" @scrolltolower="loadMore">
        <AiListSkeleton v-if="loading && !tasks.length" :rows="6" />
        <template v-else-if="tasks.length">
          <view
            v-for="task in tasks"
            :key="taskKey(task)"
            class="task-card"
            :class="{ 'is-opening': openingTaskId === String(task.taskId || task.id || '') }"
            @click="openTask(task)"
          >
            <!-- 卡片头：申请人头像 + 标题 + 状态 -->
            <view class="task-card__head">
              <view class="task-applicant__avatar">{{ applicantInitial(task) }}</view>
              <view class="task-card__heading">
                <text class="task-card__title">{{ taskTitle(task) }}</text>
                <text class="task-time">{{ applicantName(task) }} · {{ formatFlowDateTime(task.createTime || task.startTime) }}</text>
              </view>
              <text class="status-tag" :class="statusToneClass(task)">{{ statusText(task) }}</text>
            </view>
            <view class="task-card__body">
              <view class="task-card__line">
                <text class="task-card__label">当前节点</text>
                <text class="task-card__node">{{ task.taskName || task.name || '审批节点' }}</text>
              </view>
              <view class="task-card__line">
                <text class="task-card__label">流程类型</text>
                <view class="task-card__identity">
                  <text class="task-type-tag" :class="taskTypeTone(task)">{{ taskTypeText(task) }}</text>
                  <text v-if="isUrgentTask(task)" class="priority-tag">紧急</text>
                </view>
              </view>
            </view>
            <view class="task-card__footer">
              <text class="task-card__hint">{{ activeScope === 'todo' ? '需要你审批' : scopeLabel }}</text>
              <view v-if="activeScope === 'started' && (canWithdraw(task) || showRemind(task))" class="task-card__actions">
                <button
                  v-if="showRemind(task)"
                  class="remind-button"
                  :disabled="isRemindCoolingDown(task) || isReminding(task)"
                  @click.stop="remind(task)"
                >
                  {{ isRemindCoolingDown(task) ? '已催办' : '催办' }}
                </button>
                <button v-if="canWithdraw(task)" class="claim-button" @click.stop="withdrawTask(task)">撤回</button>
              </view>
              <view v-else class="task-primary-action">
                <text>{{ openingTaskId === String(task.taskId || task.id || '') ? '正在进入' : taskActionText(task) }}</text>
                <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#0066ff" size="sm" />
              </view>
            </view>
          </view>
          <AiListSkeleton v-if="loading" :rows="2" compact />
          <view v-else class="list-foot">{{ hasMore ? '上拉加载更多' : `没有更多${scopeLabel}` }}</view>
        </template>
        <view v-else class="state-box">
          <AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#0066ff" size="lg" />
          <text class="state-title">{{ flowServiceUnavailable ? '流程服务不可用' : `暂无${scopeLabel}` }}</text>
          <text class="state-copy">{{ emptyDescription }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 多条件筛选：取消时不会修改已生效条件 -->
    <AiFilterSheet v-model="filterVisible" title="筛选任务" @reset="resetFilters" @apply="applyFilters">
      <view class="todo-filter-field">
        <text class="todo-filter-label">流程分类</text>
        <AiSelect
          v-model="draftCategoryFilter"
          :options="categoryOptions"
          title="选择流程分类"
          placeholder="全部流程"
          filterable
        />
      </view>
      <view v-if="activeScope === 'todo'" class="todo-filter-field">
        <text class="todo-filter-label">任务状态</text>
        <AiSelect
          v-model="draftStatusFilter"
          :options="statusFilters"
          title="选择任务状态"
          placeholder="全部"
        />
      </view>
    </AiFilterSheet>

    <AiTabBar active="todo" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import AiIcon from '@/components/AiIcon.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiFilterSheet from '@/components/AiFilterSheet.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiSelect from '@/components/AiSelect.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import AiTabHeader from '@/components/AiTabHeader.vue'
import CcListPanel from '@/components/flow/CcListPanel.vue'
import api from '@/api'
import { useFlowRemind } from '@/composables/flow/useFlowRemind'
import { useAuthStore, useBadgeStore, useCcStore } from '@/store'
import { showConfirmDialog } from '@/utils/dialog'
import { takeTabHandoff } from '@/utils/tab-handoff'
import { resolveApiErrorMessage } from '@/utils/flow-page'
import { toast } from '@/utils/notify'
import { formatFlowDateTime } from '@/utils/flow-display'

const authStore = useAuthStore()
const badgeStore = useBadgeStore()
const ccStore = useCcStore()
const { showRemind, isRemindCoolingDown, isReminding, remind } = useFlowRemind()
const ccPanelRef = ref(null)
const tasks = ref([])
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const keyword = ref('')
const activeScope = ref('todo')
const statusFilter = ref('')
const categoryFilter = ref('')
const draftCategoryFilter = ref('')
const draftStatusFilter = ref('')
const filterVisible = ref(false)
const categoryOptions = ref([{ label: '全部流程', value: '' }])
const loading = ref(false)
const flowServiceUnavailable = ref(false)
const openingTaskId = ref('')
const pullRefreshing = ref(false)

const statusFilters = [
  { label: '全部', value: '' },
  { label: '待签收', value: '0' },
  { label: '处理中', value: '1' },
]
const workScopes = [
  { label: '待处理', value: 'todo' },
  { label: '已处理', value: 'done' },
  { label: '我发起的', value: 'started' },
  { label: '抄送我的', value: 'cc' },
]
const isCcScope = computed(() => activeScope.value === 'cc')
const userId = computed(() => authStore.userInfo?.id || authStore.userInfo?.userId || authStore.userInfo?.user_id || '')
const hasMore = computed(() => tasks.value.length < total.value)
const scopeLabel = computed(() => workScopes.find(item => item.value === activeScope.value)?.label || '待办')
const activeFilterCount = computed(() => Number(Boolean(categoryFilter.value)) + Number(activeScope.value === 'todo' && Boolean(statusFilter.value)))
const emptyDescription = computed(() => flowServiceUnavailable.value
  ? '请确认流程服务可用后重试'
  : keyword.value ? `没有符合当前条件的${scopeLabel.value}` : `当前没有${scopeLabel.value}`)

// 必须先于加载列表的 onShow 注册：同页多个 onShow 按注册顺序执行，范围要在请求前确定
onShow(() => {
  const handoff = takeTabHandoff('todo')
  if (workScopes.some(scope => scope.value === handoff?.scope)) {
    activeScope.value = handoff.scope
    statusFilter.value = ''
  }
})

onShow(async () => {
  await loadTasks({ reset: true })
  // 抄送页签下由面板 reload 一并刷新未读数
  if (!isCcScope.value) ccStore.loadUnreadCount(api.getCcUnreadCount)
  if (categoryOptions.value.length === 1) {
    loadCategories()
  }
})

onPullDownRefresh(async () => {
  try { await refreshByPull() } finally { uni.stopPullDownRefresh() }
})

async function refreshByPull() {
  if (pullRefreshing.value) return
  pullRefreshing.value = true
  try { await Promise.all([loadTasks({ reset: true }), loadCategories()]) }
  finally { pullRefreshing.value = false }
}

onReachBottom(loadMore)

async function loadTasks({ reset = false } = {}) {
  // 抄送面板自行分页；首次切换时面板挂载后自动加载，此时 ref 尚未就绪。
  if (isCcScope.value) {
    if (reset) await ccPanelRef.value?.reload()
    return
  }
  if (loading.value || (!reset && !hasMore.value)) return
  if (reset) {
    pageNum.value = 1
    total.value = 0
    tasks.value = []
  }
  loading.value = true
  try {
    flowServiceUnavailable.value = false
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userId: userId.value || undefined,
      title: keyword.value.trim() || undefined,
      status: activeScope.value === 'todo' && statusFilter.value !== '' ? Number(statusFilter.value) : undefined,
      category: categoryFilter.value || undefined,
    }
    const res = await resolveScopeApi()(params)
    const page = normalizePage(res?.data)
    tasks.value = reset ? page.records : tasks.value.concat(page.records)
    total.value = page.total
    pageNum.value += 1
    // 无筛选的待处理总数就是底栏角标，顺手同步，审批返回后角标立即更新。
    if (activeScope.value === 'todo' && !params.title && params.status === undefined && !params.category) {
      badgeStore.setTodoCount(page.total)
    }
  }
  catch (error) {
    flowServiceUnavailable.value = isFlowServiceUnavailableError(error)
    if (!flowServiceUnavailable.value) console.error('加载待办失败:', error)
  }
  finally { loading.value = false }
}

function openApprovalStart() { uni.navigateTo({ url: '/pages/approval/start' }) }
function loadMore() { loadTasks() }
function handleSearch() { loadTasks({ reset: true }) }
function clearSearch() { keyword.value = ''; handleSearch() }
function openFilters() {
  draftCategoryFilter.value = categoryFilter.value
  draftStatusFilter.value = statusFilter.value
  filterVisible.value = true
}
function resetFilters() {
  draftCategoryFilter.value = ''
  draftStatusFilter.value = ''
}
function applyFilters() {
  categoryFilter.value = draftCategoryFilter.value === undefined || draftCategoryFilter.value === null ? '' : String(draftCategoryFilter.value)
  statusFilter.value = activeScope.value === 'todo' ? String(draftStatusFilter.value || '') : ''
  filterVisible.value = false
  loadTasks({ reset: true })
}
function setScope(value) {
  if (activeScope.value === value) return
  activeScope.value = value
  statusFilter.value = ''
  loadTasks({ reset: true })
}
async function loadCategories() {
  try {
    const res = await api.getFlowCategories()
    categoryOptions.value = [{ label: '全部流程', value: '' }, ...flattenCategories(res?.data)]
  }
  catch (error) { console.error('加载流程分类失败:', error) }
}

async function withdrawTask(task) {
  const processInstanceId = task.processInstanceId
  if (!processInstanceId) return toast('该流程缺少实例标识，无法撤回', { type: 'warning' })
  const confirmed = await showConfirmDialog({
    title: '确认撤回流程',
    description: '撤回后当前审批任务将结束，请确认业务状态允许撤回。',
    confirmText: '确认撤回',
    isDestructive: true,
  })
  if (!confirmed) return
  try {
    await api.withdrawFlowProcess({ processInstanceId: String(processInstanceId), userId: String(userId.value) })
    toast('撤回成功', { type: 'success' })
    await loadTasks({ reset: true })
  }
  catch (error) {
    console.error('撤回流程失败:', error)
    toast(resolveApiErrorMessage(error, '撤回流程失败'), { type: 'error' })
  }
}

async function openTask(task) {
  const taskId = task.taskId || task.id
  if (!taskId) return toast('待办任务缺少标识', { type: 'warning' })
  const normalizedTaskId = String(taskId)
  if (openingTaskId.value) return
  openingTaskId.value = normalizedTaskId
  try {
    let targetTask = task
    if (activeScope.value === 'todo' && isCandidateTask(task)) {
      await api.claimFlowTask(normalizedTaskId, userId.value)
      targetTask = { ...task, status: 1, assignee: String(userId.value) }
      toast('已签收，正在进入处理页', { type: 'success' })
    }
    try { uni.setStorageSync(`flow-task:${normalizedTaskId}`, targetTask) } catch (error) { console.warn('缓存流程摘要失败:', error) }
    const mode = activeScope.value === 'todo' ? 'todo' : 'readonly'
    await navigateToTask(`/pages/todo-detail?taskId=${encodeURIComponent(normalizedTaskId)}&mode=${mode}`)
  }
  catch (error) {
    console.error('进入待办处理页失败:', error)
    toast(resolveApiErrorMessage(error, '进入待办处理页失败'), { type: 'error' })
    await loadTasks({ reset: true })
  }
  finally { openingTaskId.value = '' }
}

function navigateToTask(url) {
  return new Promise((resolve, reject) => uni.navigateTo({ url, success: resolve, fail: reject }))
}

function normalizePage(data) {
  const records = data?.records || data?.list || data?.rows || []
  const safeRecords = Array.isArray(records) ? records : []
  return { records: safeRecords, total: Number(data?.total ?? data?.totalCount ?? safeRecords.length) || 0 }
}
function flattenCategories(items = [], result = []) {
  ;(Array.isArray(items) ? items : []).forEach((item) => {
    const value = item.category || item.code || item.id || item.value
    const label = item.categoryName || item.name || item.label || value
    if (value) result.push({ label: String(label), value: String(value) })
    if (item.children?.length) flattenCategories(item.children, result)
  })
  return result
}
function isCandidateTask(task = {}) { return Number(task.status) === 0 && !task.assignee }
function taskKey(task) { return task.taskId || task.id || task.processInstanceId || task.title }
function taskTitle(task = {}) { return task.title || task.businessTitle || task.processName || task.processDefinitionName || task.taskName || '审批任务' }
function taskTypeText(task = {}) { return task.categoryName || task.category || task.processName || task.processDefinitionName || '流程审批' }
function taskTypeTone(task = {}) {
  const identity = `${taskTypeText(task)} ${task.businessType || ''} ${task.objectCode || ''}`.toLowerCase()
  const semanticTones = [
    [/(采购|订单|报销|费用|财务|purchase|expense|finance)/, 'tone-orange'],
    [/(请假|考勤|人事|入职|leave|attendance|hr)/, 'tone-emerald'],
    [/(资产|物料|库存|领用|asset|inventory)/, 'tone-purple'],
    [/(客户|销售|商机|合同|customer|sales|contract)/, 'tone-cyan'],
    [/(公告|通知|消息|notice|message)/, 'tone-rose'],
  ]
  const matched = semanticTones.find(([pattern]) => pattern.test(identity))
  if (matched) return matched[1]
  const tones = ['tone-blue', 'tone-orange', 'tone-emerald', 'tone-purple', 'tone-cyan', 'tone-rose']
  const hash = [...identity].reduce((total, char) => ((total * 31) + char.charCodeAt(0)) >>> 0, 0)
  return tones[hash % tones.length]
}
function applicantName(task = {}) { return task.startUserName || task.createByName || '未知申请人' }
function applicantInitial(task = {}) { return String(applicantName(task)).trim().slice(0, 1) || '申' }
function statusText(task) {
  if (isCandidateTask(task)) return '待签收'
  if (activeScope.value === 'done') return '已处理'
  if (activeScope.value === 'started') return canWithdraw(task) ? '进行中' : '已结束'
  return Number(task.status) === 1 ? '处理中' : '待处理'
}
function statusToneClass(task) {
  if (activeScope.value === 'done') return 'status-done'
  if (activeScope.value === 'started') return canWithdraw(task) ? 'status-running' : 'status-neutral'
  return isCandidateTask(task) ? 'status-waiting' : 'status-pending'
}
function taskActionText(task) {
  if (activeScope.value === 'todo') return isCandidateTask(task) ? '签收并处理' : '立即处理'
  return '查看详情'
}
function isUrgentTask(task) { return Number(task.priority || 0) >= 3 }
function canWithdraw(task) { return Number(task.status) === 0 || Number(task.status) === 1 || ['RUNNING', 'IN_PROCESS'].includes(String(task.status || '').toUpperCase()) }
function resolveScopeApi() { return activeScope.value === 'done' ? api.getDoneFlowTasks : activeScope.value === 'started' ? api.getStartedFlowTasks : api.getTodoTasks }
function isFlowServiceUnavailableError(error) {
  const status = Number(error?.code || error?.error?.status || 0)
  return error?.code === 'NETWORK_ERROR' || status === 404 || (status === 500 && !error?.error?.data)
}
</script>

<style lang="scss" scoped src="./styles/todo.scss"></style>
