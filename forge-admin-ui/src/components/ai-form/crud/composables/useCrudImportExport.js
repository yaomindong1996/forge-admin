import { NButton, NProgress, NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { downloadFile, request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'

/** 导入导出私有运行态；props 和查询 ref 保持响应式，轮询由调用方清理。 */
export function useCrudImportExport({ props, searchParams, parseApiConfig, extractApiUrl, loadList }) {
  // 导入
  const importModalVisible = ref(false)
  const hasImportTemplate = computed(() => !!(props.importTemplateUrl || props.apiConfig?.importTemplate))

  // 导出
  const exportLoading = ref(false)

  // 导出任务
  const exportTaskDrawerVisible = ref(false)
  const exportTaskLoading = ref(false)
  const exportTasks = ref([])
  const activeExportTaskId = ref(null)
  const exportTaskPollTimer = ref(null)
  const exportTaskDrawerWidth = 'min(680px, 92vw)'
  const exportTaskPagination = ref({
    page: 1,
    pageSize: 10,
    itemCount: 0,
  })

  const resolvedExportTaskConfigKey = computed(() => {
    if (props.exportTaskConfigKey) {
      return props.exportTaskConfigKey
    }
    const exportApi = props.apiConfig?.export || props.exportApi || ''
    const exportUrl = extractApiUrl(exportApi)
    const exportMatch = exportUrl.match(/\/ai\/crud\/([^/]+)\/export(?:$|\?)/)
    if (exportMatch?.[1])
      return exportMatch[1]
    // 与自定义查询一致：列表接口也能反推对象编码，避免缺 export 地址时「导出任务」开关无效
    const listApi = props.apiConfig?.list || props.listApi || props.api || ''
    const listMatch = String(listApi).match(/\/ai\/crud\/([^/]+)\/page/)
    return listMatch?.[1] || ''
  })

  const hasExportTaskApi = computed(() => {
    return !!props.apiConfig?.exportTasks || !!resolvedExportTaskConfigKey.value
  })

  const showExportTaskEntry = computed(() => {
    return props.showExport && props.showExportTasks && hasExportTaskApi.value
  })

  const activeExportTask = computed(() => {
    if (!activeExportTaskId.value) {
      return null
    }
    return exportTasks.value.find(task => String(task.id) === String(activeExportTaskId.value)) || null
  })

  const exportTaskPaginationConfig = computed(() => ({
    page: exportTaskPagination.value.page,
    pageSize: exportTaskPagination.value.pageSize,
    itemCount: exportTaskPagination.value.itemCount,
    pageSizes: [10, 20, 50],
    showSizePicker: true,
    prefix: ({ itemCount }) => `共${itemCount}个任务`,
    onUpdatePage: handleExportTaskPageChange,
    onUpdatePageSize: handleExportTaskPageSizeChange,
  }))

  const exportTaskColumns = computed(() => [
    {
      title: '文件',
      key: 'fileName',
      minWidth: 180,
      ellipsis: { tooltip: true },
      render: row => row.fileName || '-',
    },
    {
      title: '状态',
      key: 'status',
      width: 96,
      render: row => h(NTag, {
        size: 'small',
        type: resolveExportTaskTagType(row.status),
      }, { default: () => resolveExportTaskStatusText(row.status) }),
    },
    {
      title: '进度',
      key: 'progress',
      width: 150,
      render: row => h(NProgress, {
        type: 'line',
        percentage: row.progress || 0,
        processing: isExportTaskRunning(row),
        indicatorPlacement: 'inside',
        height: 10,
        status: row.status === 'FAILED' ? 'error' : undefined,
      }),
    },
    {
      title: '数据量',
      key: 'totalCount',
      width: 120,
      render: row => `${row.exportedCount || 0}/${row.totalCount || 0}`,
    },
    {
      title: '创建时间',
      key: 'createTime',
      width: 160,
      render: row => row.createTime || '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 88,
      fixed: 'right',
      render: row => h(NButton, {
        text: true,
        type: 'primary',
        size: 'small',
        disabled: row.status !== 'SUCCESS' || !row.fileId,
        onClick: () => handleDownloadExportTask(row),
      }, { default: () => '下载' }),
    },
  ])

  /**
   * 显示导入弹窗
   */
  function handleShowImport() {
    importModalVisible.value = true
  }

  /**
   * 执行导入请求，复用统一 axios 拦截器补齐认证和防重放头。
   */
  async function executeImport(file, onProgress) {
    const { method, url } = parseApiConfig('import', props.importApi, 'post')
    const requestMethod = method === 'postEncrypt' ? 'post' : method.toLowerCase()
    const formData = new FormData()
    formData.append('file', file)

    Object.entries(props.importData || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null)
        formData.append(key, value)
    })

    const response = await request({
      method: requestMethod,
      url,
      data: formData,
      headers: props.importHeaders,
      encrypt: false,
      onUploadProgress: (event) => {
        if (event.total)
          onProgress?.(Math.round((event.loaded / event.total) * 100))
      },
    })
    return response?.data || response
  }

  function handleImportSuccess() {
    loadList()
  }

  /**
   * 下载导入模板
   */
  async function handleDownloadTemplate() {
    if (!props.importTemplateUrl && !props.apiConfig.importTemplate) {
      window.$message.warning('未配置导入模板地址')
      return
    }

    try {
      const { method, url } = parseApiConfig('importTemplate', props.importTemplateUrl, 'get')
      const response = await request({
        method: method === 'postEncrypt' ? 'post' : method.toLowerCase(),
        url,
        responseType: 'blob',
        rawResponse: true,
        encrypt: false,
      })
      downloadBlobResponse(response, props.exportFileName || '导入模板.xlsx')
    }
    catch (error) {
      console.error('下载导入模板失败:', error)
      window.$message.error(error?.message || '下载导入模板失败')
    }
  }

  /**
   * 导出
   */
  async function handleExport() {
    exportLoading.value = true
    try {
      const { method, url } = parseApiConfig('export', props.exportApi || props.api, 'post')

      const params = {
        ...searchParams.value,
        ...props.publicParams,
      }

      // 确定使用哪种请求方法
      let requestMethod = method
      // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
      const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
      if (useEncrypt) {
        requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
      }
      else {
        requestMethod = method.toLowerCase()
      }

      // 发送请求
      let response
      if (useEncrypt && requestMethod === 'postEncrypt') {
        response = await postEncrypt(url, params, { responseType: 'blob', rawResponse: true })
      }
      else {
        response = await request({
          method: requestMethod,
          url,
          data: requestMethod === 'get' ? undefined : params,
          params: requestMethod === 'get' ? params : undefined,
          responseType: 'blob',
          rawResponse: true,
          encrypt: false,
        })
      }

      const asyncExportResult = await resolveAsyncExportResult(response)
      if (asyncExportResult?.async) {
        window.$message.success(asyncExportResult.message || '导出任务已提交')
        activeExportTaskId.value = asyncExportResult.taskId
        exportTaskDrawerVisible.value = true
        exportTaskPagination.value.page = 1
        await loadExportTasks()
        pollExportTask(asyncExportResult.taskId)
        return
      }

      downloadBlobResponse(response, props.exportFileName || '导出数据.xlsx')
      window.$message.success('导出成功')
    }
    catch (error) {
      console.error('导出失败:', error)
      window.$message.error('导出失败')
    }
    finally {
      exportLoading.value = false
    }
  }

  async function resolveAsyncExportResult(response) {
    if (response?.data?.async !== undefined) {
      return response.data
    }
    if (response?.async !== undefined) {
      return response
    }
    const blob = response?.data instanceof Blob ? response.data : response instanceof Blob ? response : null
    if (blob?.type?.includes('json')) {
      try {
        const text = await blob.text()
        if (!text) {
          return null
        }
        const parsed = JSON.parse(text)
        if (parsed?.data?.async !== undefined) {
          return parsed.data
        }
        if (parsed?.async !== undefined) {
          return parsed
        }
      }
      catch (error) {
        console.warn('解析异步导出响应失败:', error)
      }
    }
    return null
  }

  async function handleOpenExportTasks() {
    if (!hasExportTaskApi.value) {
      window.$message.warning('当前导出接口不支持任务查询')
      return
    }
    exportTaskDrawerVisible.value = true
    exportTaskPagination.value.page = 1
    await loadExportTasks()
  }

  async function loadExportTasks() {
    if (!hasExportTaskApi.value) {
      return
    }
    exportTaskLoading.value = true
    try {
      const fallbackUrl = `/ai/crud/${resolvedExportTaskConfigKey.value}/export/tasks`
      const { method, url } = props.apiConfig?.exportTasks
        ? parseApiConfig('exportTasks', '', 'get')
        : { method: 'get', url: fallbackUrl }
      const response = await request({
        method,
        url,
        params: {
          pageNum: exportTaskPagination.value.page,
          pageSize: exportTaskPagination.value.pageSize,
        },
      })
      const page = response?.data || {}
      exportTasks.value = page.records || []
      exportTaskPagination.value.itemCount = page.total || 0
    }
    catch (error) {
      console.error('加载导出任务失败:', error)
      window.$message.error(error?.message || '加载导出任务失败')
    }
    finally {
      exportTaskLoading.value = false
    }
  }

  async function pollExportTask(taskId) {
    clearExportTaskPollTimer()
    if (!taskId || !hasExportTaskApi.value) {
      return
    }

    const fetchTask = async () => {
      try {
        const fallbackUrl = `/ai/crud/${resolvedExportTaskConfigKey.value}/export/tasks/${taskId}`
        const { method, url } = props.apiConfig?.exportTask
          ? parseApiConfig('exportTask', '', 'get', { taskId })
          : { method: 'get', url: fallbackUrl }
        const response = await request({
          method,
          url,
          needTip: false,
        })
        const task = response?.data
        if (!task) {
          return
        }
        upsertExportTask(task)
        if (isExportTaskRunning(task)) {
          exportTaskPollTimer.value = window.setTimeout(fetchTask, 2000)
        }
        else if (task.status === 'SUCCESS') {
          window.$message.success('导出任务已完成')
        }
        else if (task.status === 'FAILED') {
          window.$message.error(task.errorMessage || '导出任务失败')
        }
      }
      catch (error) {
        console.warn('轮询导出任务失败:', error)
      }
    }

    await fetchTask()
  }

  function upsertExportTask(task) {
    const index = exportTasks.value.findIndex(item => String(item.id) === String(task.id))
    if (index >= 0) {
      exportTasks.value.splice(index, 1, task)
    }
    else {
      exportTasks.value.unshift(task)
    }
  }

  async function handleDownloadExportTask(row) {
    if (!row?.fileId) {
      window.$message.warning('导出文件还未生成')
      return
    }
    try {
      await downloadFile(row.fileId, row.fileName || '导出数据.xlsx')
    }
    catch (error) {
      console.error('下载导出文件失败:', error)
      window.$message.error(error?.message || '下载导出文件失败')
    }
  }

  function handleExportTaskPageChange(page) {
    exportTaskPagination.value.page = page
    loadExportTasks()
  }

  function handleExportTaskPageSizeChange(pageSize) {
    exportTaskPagination.value.pageSize = pageSize
    exportTaskPagination.value.page = 1
    loadExportTasks()
  }

  function clearExportTaskPollTimer() {
    if (exportTaskPollTimer.value) {
      window.clearTimeout(exportTaskPollTimer.value)
      exportTaskPollTimer.value = null
    }
  }

  function isExportTaskRunning(task) {
    return ['PENDING', 'RUNNING'].includes(task?.status)
  }

  function resolveExportTaskStatusText(status) {
    switch (status) {
      case 'PENDING':
        return '排队中'
      case 'RUNNING':
        return '导出中'
      case 'SUCCESS':
        return '已完成'
      case 'FAILED':
        return '失败'
      default:
        return '未知'
    }
  }

  function resolveExportTaskTagType(status) {
    switch (status) {
      case 'SUCCESS':
        return 'success'
      case 'FAILED':
        return 'error'
      case 'RUNNING':
        return 'info'
      case 'PENDING':
        return 'warning'
      default:
        return 'default'
    }
  }

  function downloadBlobResponse(response, fallbackName) {
    const blob = response?.data instanceof Blob ? response.data : response
    if (!(blob instanceof Blob)) {
      throw new TypeError('下载响应不是文件流')
    }

    const fileName = resolveDownloadFileName(response, fallbackName)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  function resolveDownloadFileName(response, fallbackName) {
    const disposition = response?.headers?.['content-disposition']
      || response?.headers?.get?.('content-disposition')
      || ''
    const utf8Match = disposition.match(/filename\*=utf-8''([^;]+)/i)
    if (utf8Match?.[1]) {
      return decodeURIComponent(utf8Match[1])
    }
    const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
    if (normalMatch?.[1]) {
      return decodeURIComponent(normalMatch[1])
    }
    return fallbackName
  }

  return {
    importModalVisible,
    hasImportTemplate,
    exportLoading,
    exportTaskDrawerVisible,
    exportTaskLoading,
    exportTasks,
    exportTaskDrawerWidth,
    showExportTaskEntry,
    activeExportTask,
    exportTaskPaginationConfig,
    exportTaskColumns,
    handleShowImport,
    executeImport,
    handleImportSuccess,
    handleDownloadTemplate,
    handleExport,
    handleOpenExportTasks,
    handleDownloadExportTask,
    clearExportTaskPollTimer,
    isExportTaskRunning,
    resolveExportTaskStatusText,
    resolveExportTaskTagType,
  }
}
