/** monitor.vue setup part 1. */
import * as echarts from 'echarts'
import { NButton, NDropdown, NSpace, NTag } from 'naive-ui'
import { computed, h, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import flowApi from '@/api/flow'
import UserAvatar from '@/components/common/UserAvatar.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import DictTag from '@/components/DictTag.vue'
import DingFlowViewer from '@/components/flow-designer/viewer/DingFlowViewer.vue'
import FlowReadonlyFormPanel from '@/components/flow/FlowReadonlyFormPanel.vue'
import FlowTaskDetailShell from '@/components/flow/FlowTaskDetailShell.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import {
  buildCurrentTaskOptions,
  buildMonitorFormQuery,
  buildRollbackActivityOptions,
  canInterveneInstance,
  canMutateRunningInstance,
  hasMonitorPermission,
  isSuspendedInstance,
} from '../utils/monitorAdmin'
export function applyFlowMonitorPart1() {
  const __impl = {}
  const mut = {
    taskChart: null,
    processChart: null,
  }
  const confirmReassign = (...args) => __impl.confirmReassign(...args)
  const confirmRollback = (...args) => __impl.confirmRollback(...args)
  const confirmTerminate = (...args) => __impl.confirmTerminate(...args)
  const filterByTimeout = (...args) => __impl.filterByTimeout(...args)
  const handleResize = (...args) => __impl.handleResize(...args)
  const initCharts = (...args) => __impl.initCharts(...args)
  const refreshCharts = (...args) => __impl.refreshCharts(...args)
  
  const route = useRoute()
  const userStore = useUserStore()
  const message = window.$message
  const { dict } = useDict('flow_instance_status', 'flow_error_log_status')
  const canManage = computed(() => hasMonitorPermission(userStore, route, 'flow:monitor:manage'))
  const canCleanup = computed(() => hasMonitorPermission(userStore, route, 'flow:monitor:cleanup'))

  // 统计数据
  const statistics = reactive({
    runningInstances: 0,
    pendingTasks: 0,
    todayCompleted: 0,
    timeoutTasks: 0,
    degraded: false,
  })

  // 搜索表单
  const searchForm = reactive({
    processName: '',
    initiator: '',
    status: null,
    dateRange: null,
    modelKey: null,
    overdue: false,
  })

  // 状态选项
  const statusOptions = computed(() => dict.value.flow_instance_status || [])

  // 表格数据
  const tableData = ref([])
  const loading = ref(false)
  const dataError = ref(false)
  const cleanupLoading = ref(false)
  const deletingProcessId = ref(null)
  const isDeletingProcess = computed(() => cleanupLoading.value || Boolean(deletingProcessId.value))
  const cleanupLoadingMessageKey = 'flow-monitor-cleanup-loading'
  const deleteLoadingMessageKey = 'flow-monitor-delete-loading'
  const pagination = reactive({
    page: 1,
    pageSize: 10,
    itemCount: 0,
    showSizePicker: true,
    pageSizes: [10, 20, 50],
  })

  // 表格列定义
  const columns = [
    {
      title: '流程名称',
      key: 'processName',
      width: 160,
      ellipsis: { tooltip: true },
    },
    {
      title: '发起人',
      key: 'initiatorName',
      width: 80,
    },
    {
      title: '当前节点',
      key: 'currentNode',
      width: 110,
      ellipsis: { tooltip: true },
    },
    {
      title: '处理人',
      key: 'currentAssignee',
      width: 80,
    },
    {
      title: '状态',
      key: 'status',
      width: 80,
      render(row) {
        return h(DictTag, { dictType: 'flow_instance_status', value: row.status })
      },
    },
    {
      title: '发起时间',
      key: 'startTime',
      width: 150,
    },
    {
      title: '运行时长',
      key: 'duration',
      width: 80,
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      fixed: 'right',
      render(row) {
        const options = [
          { label: '流程图', key: 'diagram' },
          { label: '变量', key: 'variables' },
          { label: '错误日志', key: 'errors' },
        ]
        if (canManage.value && canInterveneInstance(row.status)) {
          options.push({ type: 'divider', key: 'd1' })
          options.push({ label: '管理', key: 'admin' })
        }
        if (canCleanup.value) {
          options.push({ type: 'divider', key: 'd2' })
          options.push({
            label: () => h('span', { style: { color: '#d03050' } }, '删除流程数据'),
            key: 'delete',
            disabled: isDeletingProcess.value,
          })
        }
        return h('div', { class: 'monitor-row-actions' }, [
          h('button', {
            type: 'button',
            class: 'monitor-row-link-action',
            disabled: isDeletingProcess.value,
            onClick: (event) => {
              event?.stopPropagation?.()
              showDetail(row)
            },
          }, [
            h('span', '详情'),
            h('i', { class: 'i-material-symbols:chevron-right' }),
          ]),
          h(NDropdown, {
            options,
            disabled: isDeletingProcess.value,
            onSelect: (key) => {
              const map = {
                diagram: () => showDiagram(row),
                variables: () => showVariables(row),
                errors: () => showInstanceErrorLogs(row),
                admin: () => showAdminActions(row),
                delete: () => handleDeleteInstance(row),
              }
              map[key]?.()
            },
          }, {
            default: () => h('button', {
              'type': 'button',
              'class': 'monitor-row-link-action',
              'disabled': isDeletingProcess.value,
              'aria-label': '更多操作',
              'onClick': event => event?.stopPropagation?.(),
            }, [
              h('span', '更多'),
              h('i', { class: 'i-material-symbols:expand-more' }),
            ]),
          }),
        ])
      },
    },
  ]

  const detailDrawerVisible = ref(false)
  const currentInstance = ref({})
  const approvalHistory = ref([])
  const monitorFormRow = computed(() => buildMonitorFormQuery(currentInstance.value))
  const canMutateCurrent = computed(() => canMutateRunningInstance(currentInstance.value?.status))
  const isSuspendedCurrent = computed(() => isSuspendedInstance(currentInstance.value?.status))

  const diagramModalVisible = ref(false)
  const currentDiagramInstanceId = ref(null)

  const variablesModalVisible = ref(false)
  const variablesLoading = ref(false)
  const processVariables = ref({})
  const adminActionsModalVisible = ref(false)
  const adminMutation = ref(null)
  const isAdminMutationBusy = computed(() => Boolean(adminMutation.value))

  function isAdminMutating(action) {
    return adminMutation.value === action
  }

  function beginAdminMutation(action) {
    if (adminMutation.value)
      return false
    adminMutation.value = action
    return true
  }

  function endAdminMutation(action) {
    if (adminMutation.value === action)
      adminMutation.value = null
  }

  const terminateReason = ref('')
  const rollbackTargetActivity = ref(null)
  const rollbackReason = ref('')
  const reassignUserId = ref('')
  const reassignUserName = ref('')
  const reassignReason = ref('')
  const activityOptions = ref([])
  const activitiesLoading = ref(false)
  const currentTaskId = ref(null)
  const currentTaskOptions = ref([])
  const userSelectModalVisible = ref(false)

  const adminTaskContext = ref([])
  const adminTaskTree = ref([])
  const adminTaskTreeTruncated = ref(false)
  const adminTaskContextLoading = ref(false)
  const adminTaskContextError = ref(false)
  const adminTaskPagination = reactive({
    page: 1,
    pageSize: 5,
    itemCount: 0,
    showSizePicker: true,
    pageSizes: [5, 10, 20],
  })
  const adminTaskColumns = [
    { title: '任务节点', key: 'taskName', minWidth: 150, ellipsis: { tooltip: true } },
    { title: '处理人', key: 'assigneeName', width: 100, render: row => row.assigneeName || row.assignee || '待签收' },
    { title: '状态', key: 'status', width: 80, render: row => row.completeTime ? '已完成' : '处理中' },
    { title: '创建时间', key: 'createTime', width: 150 },
    { title: '完成时间', key: 'completeTime', width: 150, render: row => row.completeTime || '-' },
  ]

  // 图表引用
  const taskChartRef = ref(null)
  const processChartRef = ref(null)

  // 图表周期（天数）
  const chartPeriod = ref(7)

  // 流程实例错误日志抽屉
  const instanceErrorDrawerVisible = ref(false)
  const currentErrorInstanceId = ref(null)
  const instanceErrorLogData = ref([])
  const instanceErrorLogLoading = ref(false)
  const instanceErrorStatusFilter = ref(null)
  const instanceErrorPagination = reactive({
    page: 1,
    pageSize: 10,
    itemCount: 0,
    showSizePicker: true,
    pageSizes: [10, 20, 50],
  })
  const errorStatusOptions = computed(() => toNumberOptions(dict.value.flow_error_log_status))
  const errorDetailVisible = ref(false)
  const currentErrorLog = ref(null)
  const retryModalVisible = ref(false)
  const retryReason = ref('')
  const retrying = ref(false)

  const errorColumns = [
    {
      title: '流程实例ID',
      key: 'processInstanceId',
      width: 180,
      ellipsis: { tooltip: true },
    },
    {
      title: '节点名称',
      key: 'activityName',
      width: 120,
    },
    {
      title: '错误类型',
      key: 'errorType',
      width: 120,
      ellipsis: { tooltip: true },
      render(row) {
        return getErrorTypeText(row.errorType)
      },
    },
    {
      title: '错误信息',
      key: 'errorMessage',
      width: 200,
      ellipsis: { tooltip: true },
    },
    {
      title: '错误环节',
      key: 'errorStage',
      width: 100,
      render(row) {
        return getErrorStageText(row.errorStage)
      },
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render(row) {
        return h(DictTag, { dictType: 'flow_error_log_status', value: row.status })
      },
    },
    {
      title: '创建时间',
      key: 'createTime',
      width: 160,
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      fixed: 'right',
      render(row) {
        return h(NSpace, null, {
          default: () => [
            h(NButton, { text: true, type: 'primary', onClick: () => showErrorDetail(row) }, { default: () => '详情' }),
            canManage.value && (row.status === 0 || row.status === 3) && h(NButton, { text: true, type: 'info', onClick: () => showRetryDialog(row) }, { default: () => '重试' }),
            canManage.value && row.status === 0 && h(NButton, { text: true, type: 'warning', onClick: () => handleResolveErrorLog(row) }, { default: () => '解决' }),
          ].filter(Boolean),
        })
      },
    },
  ]

  function toNumberOptions(options = []) {
    return options.map(item => ({
      ...item,
      value: Number(item.value),
    }))
  }

  function getErrorStageText(stage) {
    const map = {
      TASK_APPROVE: '审批通过',
      TASK_REJECT: '审批驳回',
      TASK_DELEGATE: '任务转办',
      TASK_RETURN: '任务退回',
      TASK_TERMINATE: '流程终结',
      TASK_WITHDRAW: '流程撤回',
      PROCESS_START: '流程启动',
      PROCESS_TERMINATE: '流程终止',
      TASK_REASSIGN: '任务转派',
      EVENT_TASK_CREATED: '任务创建事件',
      EVENT_TASK_COMPLETED: '任务完成事件',
      EVENT_TASK_ASSIGNED: '任务分配事件',
      EVENT_TASK_DELETED: '任务删除事件',
      EVENT_PROCESS_COMPLETED: '流程完成事件',
      EVENT_PROCESS_CANCELLED: '流程取消事件',
      NODE_RETRY: '节点重试',
      TASK_ASSIGNEE_MISSING: '审批人缺失',
      EVENT_DISPATCH: '事件分发',
    }
    return map[stage] || stage || '-'
  }

  function getErrorTypeText(errorType) {
    if (!errorType)
      return '-'
    const simpleName = errorType.replace(/^.*\./, '')
    const map = {
      RuntimeException: '运行时异常',
      NullPointerException: '空指针异常',
      IllegalArgumentException: '参数异常',
      IllegalStateException: '状态异常',
      FlowableException: '流程引擎异常',
      FlowableObjectNotFoundException: '流程对象未找到',
      FlowableTaskNotFoundException: '任务未找到',
      FlowableIllegalArgumentException: '流程参数异常',
      ClassCastException: '类型转换异常',
      IOException: 'IO异常',
      SQLException: '数据库异常',
      TimeoutException: '超时异常',
      ConcurrentModificationException: '并发修改异常',
      IndexOutOfBoundsException: '索引越界异常',
      NoSuchElementException: '元素不存在异常',
      UnsupportedOperationException: '不支持的操作',
      SecurityException: '安全异常',
      ArithmeticException: '算术异常',
      NumberFormatException: '数字格式异常',
      ParseException: '解析异常',
      FLOW_RUNTIME_ERROR: '流程运行异常',
      DataAccessException: '数据访问异常',
      MyBatisSystemException: 'MyBatis异常',
      DuplicateKeyException: '主键冲突',
      DataIntegrityViolationException: '数据完整性异常',
      HttpMessageNotReadableException: '请求参数解析异常',
      MethodArgumentNotValidException: '参数校验异常',
      MissingServletRequestParameterException: '缺少请求参数',
      HttpRequestMethodNotSupportedException: '请求方法不支持',
      AccessDeniedException: '权限不足',
      AuthenticationException: '认证异常',
      FeignException: '远程调用异常',
      RedisConnectionFailureException: 'Redis连接异常',
      AmqpException: '消息队列异常',
    }
    return map[simpleName] || simpleName
  }

  function showInstanceErrorLogs(row) {
    currentErrorInstanceId.value = row.id
    instanceErrorStatusFilter.value = null
    instanceErrorPagination.page = 1
    instanceErrorDrawerVisible.value = true
    loadInstanceErrorLogs()
  }

  async function loadInstanceErrorLogs() {
    if (!currentErrorInstanceId.value)
      return
    instanceErrorLogLoading.value = true
    try {
      const params = {
        page: instanceErrorPagination.page,
        pageSize: instanceErrorPagination.pageSize,
        processInstanceId: currentErrorInstanceId.value,
        status: instanceErrorStatusFilter.value,
      }
      const res = await request.get('/api/flow/monitor/error-logs', { params })
      if (res.code === 200) {
        instanceErrorLogData.value = res.data.list || []
        instanceErrorPagination.itemCount = res.data.total || 0
      }
    }
    catch (error) {
      console.error('加载实例错误日志失败', error)
    }
    finally {
      instanceErrorLogLoading.value = false
    }
  }

  function handleInstanceErrorPageChange(page) {
    instanceErrorPagination.page = page
    loadInstanceErrorLogs()
  }

  function handleInstanceErrorPageSizeChange(pageSize) {
    instanceErrorPagination.pageSize = pageSize
    instanceErrorPagination.page = 1
    loadInstanceErrorLogs()
  }

  function showErrorDetail(row) {
    currentErrorLog.value = row
    errorDetailVisible.value = true
  }

  function showRetryDialog(row) {
    currentErrorLog.value = row
    retryReason.value = ''
    retryModalVisible.value = true
  }

  async function handleResolveErrorLog(row) {
    const mutation = `resolve-error:${row.id}`
    if (!beginAdminMutation(mutation))
      return
    try {
      const res = await request.put(`/api/flow/monitor/error-logs/${row.id}/resolve`)
      if (res.code === 200) {
        message.success('已标记为已解决')
        await loadInstanceErrorLogs()
      }
      else {
        showMutationResponseError(res, '标记错误日志失败')
      }
    }
    catch (error) {
      console.error('解决错误日志失败', error)
      showMutationError(error, '标记错误日志失败')
    }
    finally {
      endAdminMutation(mutation)
    }
  }

  async function handleRetryError() {
    retryModalVisible.value = true
  }

  async function confirmRetry() {
    if (!retryReason.value.trim()) {
      message.warning('请输入重试原因')
      return
    }
    const errorLog = currentErrorLog.value
    if (!errorLog || !errorLog.id) {
      message.error('无法获取错误日志信息')
      return
    }

    if (!beginAdminMutation('retry-error'))
      return
    retrying.value = true
    try {
      const res = await request.post(`/api/flow/monitor/error-logs/${errorLog.id}/retry`, {
        processInstanceId: errorLog.processInstanceId,
        activityId: errorLog.activityId,
        reason: retryReason.value,
      })
      if (res.code === 200) {
        message.success('节点重试成功')
        retryModalVisible.value = false
        errorDetailVisible.value = false
        await Promise.all([loadInstanceErrorLogs(), loadData()])
      }
      else {
        showMutationResponseError(res, '重试失败')
      }
    }
    catch (error) {
      console.error('重试节点失败', error)
      showMutationError(error, '重试失败')
    }
    finally {
      retrying.value = false
      endAdminMutation('retry-error')
    }
  }

  // 加载统计数据
  async function loadStatistics() {
    statistics.degraded = false
    try {
      const res = await request.get('/api/flow/monitor/statistics')
      if (res.code === 200) {
        Object.assign(statistics, res.data)
      }
      else {
        statistics.degraded = true
      }
    }
    catch (error) {
      console.error('加载统计数据失败', error)
      statistics.degraded = true
    }
  }

  // 加载表格数据
  async function loadData() {
    loading.value = true
    dataError.value = false
    try {
      const params = {
        pageNum: pagination.page,
        pageSize: pagination.pageSize,
        ...buildSearchParams(),
      }
      const res = await request.get('/api/flow/monitor/instances', { params })
      if (res.code === 200) {
        tableData.value = res.data.list || []
        pagination.itemCount = res.data.total || 0
        dataError.value = res.data.degraded === true
      }
      else {
        dataError.value = true
      }
    }
    catch (error) {
      console.error('加载数据失败', error)
      dataError.value = true
    }
    finally {
      loading.value = false
    }
  }

  function buildSearchParams() {
    const params = {
      processName: searchForm.processName,
      initiator: searchForm.initiator,
      status: searchForm.status,
      modelKey: searchForm.modelKey,
      overdue: searchForm.overdue || undefined,
    }
    if (Array.isArray(searchForm.dateRange) && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    return params
  }

  // 加载任务趋势数据
  async function loadTaskTrend(days = 7) {
    try {
      const res = await request.get('/api/flow/monitor/taskTrend', { params: { days } })
      if (res.code === 200 && res.data) {
        return res.data
      }
    }
    catch (error) {
      console.error('加载任务趋势数据失败', error)
    }
    return { dates: [], created: [], completed: [] }
  }

  // 加载流程分布数据
  async function loadProcessDistribution() {
    try {
      const res = await request.get('/api/flow/monitor/processDistribution')
      if (res.code === 200 && res.data) {
        return res.data
      }
    }
    catch (error) {
      console.error('加载流程分布数据失败', error)
    }
    return []
  }

  // 搜索
  function handleSearch() {
    pagination.page = 1
    loadData()
  }

  // 重置
  function handleReset() {
    Object.assign(searchForm, {
      processName: '',
      initiator: '',
      status: null,
      dateRange: null,
      modelKey: null,
      overdue: false,
    })
    handleSearch()
  }

  function hasCleanupFilter() {
    return Boolean(
      searchForm.processName
      || searchForm.initiator
      || searchForm.status
      || searchForm.modelKey
      || searchForm.overdue
      || (Array.isArray(searchForm.dateRange) && searchForm.dateRange.length === 2),
    )
  }

  function getErrorMessage(error, fallback) {
    return error?.message || error?.response?.data?.message || error?.error?.message || fallback
  }

  function getRequestId(error) {
    return error?.response?.headers?.['x-request-id']
      || error?.response?.headers?.['X-Request-Id']
      || error?.requestId
      || error?.response?.data?.requestId
  }

  function showMutationError(error, fallback) {
    const requestId = getRequestId(error)
    const suffix = requestId ? `（请求编号：${requestId}）` : ''
    message.error(`${getErrorMessage(error, fallback)}${suffix}`)
  }

  function showMutationResponseError(response, fallback) {
    const requestId = response?.requestId || response?.data?.requestId
    const suffix = requestId ? `（请求编号：${requestId}）` : ''
    message.error(`${response?.message || response?.msg || fallback}${suffix}`)
  }

  function refreshMonitorData() {
    loadData()
    loadStatistics()
    refreshCharts()
  }

  function showDeleteLoading(key, content) {
    message.loading(content, { key, duration: 600000 })
  }

  function closeDeleteLoading(key) {
    message.destroy?.(key, 0)
  }

  function handleCleanupCurrentFilter() {
    if (isDeletingProcess.value) {
      return
    }
    const hasFilter = hasCleanupFilter()
    const totalText = pagination.itemCount > 0 ? `当前匹配 ${pagination.itemCount} 条流程记录。` : '当前没有匹配记录。'
    const scopeText = hasFilter
      ? '将删除当前筛选条件下的流程运行时、历史实例和业务关联记录。'
      : '当前未设置筛选条件，将删除监控列表中的全部流程记录。'

    let cleanupDialog = null
    cleanupDialog = window.$dialog?.error({
      title: '确认删除流程数据',
      content: `${scopeText}${totalText}删除后不可恢复，确认继续？`,
      positiveText: '确认删除',
      negativeText: '取消',
      onPositiveClick: async () => {
        showDeleteLoading(cleanupLoadingMessageKey, '正在删除当前筛选流程数据，请稍候...')
        cleanupLoading.value = true
        try {
          const res = await request.post('/api/flow/monitor/instances/cleanup', {
            ...buildSearchParams(),
            confirmText: '确认删除流程数据',
            reason: '管理员批量删除流程数据',
          })
          if (res.code === 200) {
            const count = res.data?.deletedCount ?? 0
            message.success(`已删除 ${count} 条流程数据`)
            refreshMonitorData()
          }
          else {
            message.error(res.message || '删除流程数据失败')
          }
        }
        catch (error) {
          console.error('批量删除流程数据失败', error)
          showMutationError(error, '删除流程数据失败')
        }
        finally {
          cleanupLoading.value = false
          closeDeleteLoading(cleanupLoadingMessageKey)
          cleanupDialog?.destroy?.()
        }
        return false
      },
    })
  }

  function handleDeleteInstance(row) {
    if (isDeletingProcess.value) {
      return
    }
    if (!row.id) {
      message.warning('无法获取流程实例ID')
      return
    }

    let deleteDialog = null
    deleteDialog = window.$dialog?.error({
      title: '确认删除流程数据',
      content: `将删除「${row.processName || row.id}」的流程运行时、历史实例和业务关联记录，删除后不可恢复。`,
      positiveText: '确认删除',
      negativeText: '取消',
      onPositiveClick: async () => {
        showDeleteLoading(deleteLoadingMessageKey, '正在删除流程数据，请稍候...')
        deletingProcessId.value = row.id
        try {
          const res = await request.post(`/api/flow/monitor/instance/${row.id}/delete`, {
            reason: '管理员删除流程数据',
          })
          if (res.code === 200) {
            message.success('流程数据已删除')
            detailDrawerVisible.value = false
            adminActionsModalVisible.value = false
            refreshMonitorData()
          }
          else {
            message.error(res.message || '删除流程数据失败')
          }
        }
        catch (error) {
          console.error('删除流程数据失败', error)
          showMutationError(error, '删除流程数据失败')
        }
        finally {
          deletingProcessId.value = null
          closeDeleteLoading(deleteLoadingMessageKey)
          deleteDialog?.destroy?.()
        }
        return false
      },
    })
  }

  // 分页变化
  function handlePageChange(page) {
    pagination.page = page
    loadData()
  }

  // 每页条数变化
  function handlePageSizeChange(pageSize) {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  }

  function getInstanceStatusText(status) {
    return dict.value.flow_instance_status?.find(item => String(item.value) === String(status))?.label || status || '-'
  }

  function getInstanceStatusClass(status) {
    const map = {
      running: 'info',
      active: 'info',
      suspended: 'warning',
      completed: 'success',
      approved: 'success',
      rejected: 'error',
      terminated: 'error',
      canceled: 'default',
    }
    return map[String(status || '').toLowerCase()] || 'default'
  }

  function getInstanceStatusIcon(status) {
    const map = {
      running: 'i-material-symbols:play-circle',
      active: 'i-material-symbols:play-circle',
      suspended: 'i-material-symbols:pause-circle',
      completed: 'i-material-symbols:check-circle',
      approved: 'i-material-symbols:check-circle',
      rejected: 'i-material-symbols:cancel',
      terminated: 'i-material-symbols:stop-circle',
      canceled: 'i-material-symbols:block',
    }
    return map[String(status || '').toLowerCase()] || 'i-material-symbols:info'
  }

  function resetAdminActionForm() {
    terminateReason.value = ''
    rollbackTargetActivity.value = null
    rollbackReason.value = ''
    reassignUserId.value = ''
    reassignUserName.value = ''
    reassignReason.value = ''
    activityOptions.value = []
    currentTaskId.value = null
    currentTaskOptions.value = []
  }

  async function loadAdminActionContext(processInstanceId) {
    if (!canManage.value || !canInterveneInstance(currentInstance.value?.status) || !processInstanceId)
      return
    await Promise.all([
      loadActivities(processInstanceId),
      loadCurrentTasks(processInstanceId),
    ])
  }

  async function showDetail(row) {
    currentInstance.value = row
    approvalHistory.value = []
    adminTaskContext.value = []
    adminTaskTree.value = []
    adminTaskTreeTruncated.value = false
    adminTaskContextError.value = false
    adminTaskPagination.page = 1
    detailDrawerVisible.value = true

    if (!row.id)
      return
    await Promise.all([loadApprovalHistory(row.id), loadAdminInstanceTasks()])
  }

  async function loadApprovalHistory(processInstanceId = currentInstance.value?.id) {
    if (!processInstanceId)
      return
    try {
      const res = await request.get(`/api/flow/task/history/${processInstanceId}`)
      if (res.code === 200)
        approvalHistory.value = res.data || []
    }
    catch (error) {
      console.error('加载审批历史失败', error)
    }
  }

  async function loadAdminInstanceTasks() {
    const processInstanceId = currentInstance.value?.id
    if (!processInstanceId)
      return
    adminTaskContextLoading.value = true
    adminTaskContextError.value = false
    try {
      const res = await flowApi.getMonitorInstanceTasks(processInstanceId, {
        pageNum: adminTaskPagination.page,
        pageSize: adminTaskPagination.pageSize,
      })
      if (res.code === 200) {
        adminTaskContext.value = res.data?.list || []
        adminTaskPagination.itemCount = res.data?.total || 0
        adminTaskTree.value = res.data?.taskTree || []
        adminTaskTreeTruncated.value = res.data?.taskTreeTruncated === true
      }
      else {
        adminTaskContextError.value = true
      }
    }
    catch (error) {
      console.error('加载审批任务上下文失败', error)
      adminTaskContextError.value = true
    }
    finally {
      adminTaskContextLoading.value = false
    }
  }

  function handleAdminTaskPageChange(page) {
    adminTaskPagination.page = page
    loadAdminInstanceTasks()
  }

  function handleAdminTaskPageSizeChange(pageSize) {
    adminTaskPagination.pageSize = pageSize
    adminTaskPagination.page = 1
    loadAdminInstanceTasks()
  }

  async function showAdminActions(row) {
    if (!canManage.value) {
      message.warning('没有流程管理权限')
      return
    }
    if (!canInterveneInstance(row.status)) {
      message.warning('只能干预运行中或已挂起的流程')
      return
    }
    currentInstance.value = row
    resetAdminActionForm()
    adminActionsModalVisible.value = true
    await loadAdminActionContext(row.id)
  }

  async function openRoutedInstance(processInstanceId) {
    try {
      const res = await request.get(
        `/api/flow/monitor/instance/${encodeURIComponent(processInstanceId)}`,
      )
      if (res.code !== 200 || !res.data?.id) {
        message.warning('未找到对应的流程实例')
        return
      }
      await showDetail(res.data)
    }
    catch (error) {
      console.error('加载指定流程实例失败', error)
      message.error('流程实例加载失败')
    }
  }

  // 显示流程图
  function showDiagram(row) {
    if (!row.id) {
      message.warning('无法获取流程实例')
      return
    }
    currentDiagramInstanceId.value = row.id
    diagramModalVisible.value = true
  }

  // 挂起流程
  async function handleSuspend() {
    // 验证流程状态
    if (!canMutateRunningInstance(currentInstance.value.status)) {
      message.warning('只能挂起运行中的流程')
      return
    }

    if (!currentInstance.value.id) {
      message.warning('无法获取流程实例ID')
      return
    }

    if (!beginAdminMutation('suspend'))
      return
    try {
      const res = await request.post(`/api/flow/monitor/suspend/${currentInstance.value.id}`)
      if (res.code === 200) {
        message.success('流程已挂起')
        currentInstance.value = { ...currentInstance.value, status: 'suspended' }
        await Promise.all([loadData(), loadStatistics(), loadAdminActionContext(currentInstance.value.id)])
      }
      else {
        showMutationResponseError(res, '挂起流程失败')
      }
    }
    catch (error) {
      console.error('挂起流程失败:', error)
      showMutationError(error, '挂起流程失败')
    }
    finally {
      endAdminMutation('suspend')
    }
  }

  // 激活流程
  async function handleActivate() {
    // 验证流程状态
    if (!isSuspendedInstance(currentInstance.value.status)) {
      message.warning('只能激活已挂起的流程')
      return
    }

    if (!currentInstance.value.id) {
      message.warning('无法获取流程实例ID')
      return
    }

    if (!beginAdminMutation('activate'))
      return
    try {
      const res = await request.post(`/api/flow/monitor/activate/${currentInstance.value.id}`)
      if (res.code === 200) {
        message.success('流程已激活')
        currentInstance.value = { ...currentInstance.value, status: 'running' }
        await loadAdminActionContext(currentInstance.value.id)
        await Promise.all([loadData(), loadStatistics()])
      }
      else {
        showMutationResponseError(res, '激活流程失败')
      }
    }
    catch (error) {
      console.error('激活流程失败:', error)
      showMutationError(error, '激活流程失败')
    }
    finally {
      endAdminMutation('activate')
    }
  }

  // 显示流程变量
  async function showVariables(row) {
    variablesModalVisible.value = true
    variablesLoading.value = true
    processVariables.value = {}

    try {
      const res = await request.get(`/api/flow/monitor/variables/${row.id}`)
      if (res.code === 200) {
        processVariables.value = res.data || {}
      }
    }
    catch (error) {
      console.error('加载流程变量失败', error)
      message.error('加载流程变量失败')
    }
    finally {
      variablesLoading.value = false
    }
  }

  // 格式化变量值
  function formatVariableValue(value) {
    if (value === null || value === undefined) {
      return 'null'
    }
    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2)
    }
    return String(value)
  }

  async function loadCurrentTasks(processInstanceId) {
    currentTaskOptions.value = []
    currentTaskId.value = null
    try {
      const res = await request.get(`/api/flow/monitor/current-tasks/${processInstanceId}`)
      if (res.code === 200) {
        currentTaskOptions.value = buildCurrentTaskOptions(res.data || [])
        if (currentTaskOptions.value.length === 1)
          currentTaskId.value = currentTaskOptions.value[0].value
      }
    }
    catch (error) {
      console.error('获取当前任务失败', error)
    }
  }

  // 用户选择回调
  function openReassignUserSelect() {
    if (!canMutateCurrent.value)
      return
    userSelectModalVisible.value = true
  }

  function handleUserSelect(user) {
    if (user) {
      reassignUserId.value = String(user.id)
      reassignUserName.value = user.name || user.realName || user.username
    }
  }

  // 加载历史活动节点
  async function loadActivities(processInstanceId) {
    activitiesLoading.value = true
    try {
      const res = await request.get(`/api/flow/monitor/activities/${processInstanceId}`)
      if (res.code === 200 && res.data) {
        activityOptions.value = buildRollbackActivityOptions(res.data)
      }
    }
    catch (error) {
      console.error('加载历史节点失败', error)
      message.error('加载历史节点失败')
    }
    finally {
      activitiesLoading.value = false
    }
  }

  __impl.isAdminMutating = isAdminMutating
  __impl.beginAdminMutation = beginAdminMutation
  __impl.endAdminMutation = endAdminMutation
  __impl.toNumberOptions = toNumberOptions
  __impl.getErrorStageText = getErrorStageText
  __impl.getErrorTypeText = getErrorTypeText
  __impl.showInstanceErrorLogs = showInstanceErrorLogs
  __impl.loadInstanceErrorLogs = loadInstanceErrorLogs
  __impl.handleInstanceErrorPageChange = handleInstanceErrorPageChange
  __impl.handleInstanceErrorPageSizeChange = handleInstanceErrorPageSizeChange
  __impl.showErrorDetail = showErrorDetail
  __impl.showRetryDialog = showRetryDialog
  __impl.handleResolveErrorLog = handleResolveErrorLog
  __impl.handleRetryError = handleRetryError
  __impl.confirmRetry = confirmRetry
  __impl.loadStatistics = loadStatistics
  __impl.loadData = loadData
  __impl.buildSearchParams = buildSearchParams
  __impl.loadTaskTrend = loadTaskTrend
  __impl.loadProcessDistribution = loadProcessDistribution
  __impl.handleSearch = handleSearch
  __impl.handleReset = handleReset
  __impl.hasCleanupFilter = hasCleanupFilter
  __impl.getErrorMessage = getErrorMessage
  __impl.getRequestId = getRequestId
  __impl.showMutationError = showMutationError
  __impl.showMutationResponseError = showMutationResponseError
  __impl.refreshMonitorData = refreshMonitorData
  __impl.showDeleteLoading = showDeleteLoading
  __impl.closeDeleteLoading = closeDeleteLoading
  __impl.handleCleanupCurrentFilter = handleCleanupCurrentFilter
  __impl.handleDeleteInstance = handleDeleteInstance
  __impl.handlePageChange = handlePageChange
  __impl.handlePageSizeChange = handlePageSizeChange
  __impl.getInstanceStatusText = getInstanceStatusText
  __impl.getInstanceStatusClass = getInstanceStatusClass
  __impl.getInstanceStatusIcon = getInstanceStatusIcon
  __impl.resetAdminActionForm = resetAdminActionForm
  __impl.loadAdminActionContext = loadAdminActionContext
  __impl.showDetail = showDetail
  __impl.loadApprovalHistory = loadApprovalHistory
  __impl.loadAdminInstanceTasks = loadAdminInstanceTasks
  __impl.handleAdminTaskPageChange = handleAdminTaskPageChange
  __impl.handleAdminTaskPageSizeChange = handleAdminTaskPageSizeChange
  __impl.showAdminActions = showAdminActions
  __impl.openRoutedInstance = openRoutedInstance
  __impl.showDiagram = showDiagram
  __impl.handleSuspend = handleSuspend
  __impl.handleActivate = handleActivate
  __impl.showVariables = showVariables
  __impl.formatVariableValue = formatVariableValue
  __impl.loadCurrentTasks = loadCurrentTasks
  __impl.openReassignUserSelect = openReassignUserSelect
  __impl.handleUserSelect = handleUserSelect
  __impl.loadActivities = loadActivities

  return {
    __impl, mut, beginAdminMutation, buildSearchParams, closeDeleteLoading, confirmReassign, confirmRetry, confirmRollback,
    confirmTerminate, endAdminMutation, filterByTimeout, formatVariableValue, getErrorMessage, getErrorStageText, getErrorTypeText, getInstanceStatusClass,
    getInstanceStatusIcon, getInstanceStatusText, getRequestId, handleActivate, handleAdminTaskPageChange, handleAdminTaskPageSizeChange, handleCleanupCurrentFilter, handleDeleteInstance,
    handleInstanceErrorPageChange, handleInstanceErrorPageSizeChange, handlePageChange, handlePageSizeChange, handleReset, handleResize, handleResolveErrorLog, handleRetryError,
    handleSearch, handleSuspend, handleUserSelect, hasCleanupFilter, initCharts, isAdminMutating, loadActivities, loadAdminActionContext,
    loadAdminInstanceTasks, loadApprovalHistory, loadCurrentTasks, loadData, loadInstanceErrorLogs, loadProcessDistribution, loadStatistics, loadTaskTrend,
    openReassignUserSelect, openRoutedInstance, refreshCharts, refreshMonitorData, resetAdminActionForm, showAdminActions, showDeleteLoading, showDetail,
    showDiagram, showErrorDetail, showInstanceErrorLogs, showMutationError, showMutationResponseError, showRetryDialog, showVariables, toNumberOptions,
    route, userStore, message, canManage, canCleanup, statistics, searchForm, statusOptions,
    tableData, loading, dataError, cleanupLoading, deletingProcessId, isDeletingProcess, cleanupLoadingMessageKey, deleteLoadingMessageKey,
    pagination, columns, detailDrawerVisible, currentInstance, approvalHistory, monitorFormRow, canMutateCurrent, isSuspendedCurrent,
    diagramModalVisible, currentDiagramInstanceId, variablesModalVisible, variablesLoading, processVariables, adminActionsModalVisible, adminMutation, isAdminMutationBusy,
    terminateReason, rollbackTargetActivity, rollbackReason, reassignUserId, reassignUserName, reassignReason, activityOptions, activitiesLoading,
    currentTaskId, currentTaskOptions, userSelectModalVisible, adminTaskContext, adminTaskTree, adminTaskTreeTruncated, adminTaskContextLoading, adminTaskContextError,
    adminTaskPagination, adminTaskColumns, taskChartRef, processChartRef, chartPeriod, instanceErrorDrawerVisible, currentErrorInstanceId, instanceErrorLogData,
    instanceErrorLogLoading, instanceErrorStatusFilter, instanceErrorPagination, errorStatusOptions, errorDetailVisible, currentErrorLog, retryModalVisible, retryReason,
    retrying, errorColumns,
  }
}
