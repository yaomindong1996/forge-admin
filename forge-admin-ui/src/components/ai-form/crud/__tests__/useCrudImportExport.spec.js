import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, ref } from 'vue'
import { downloadFile, request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'
import { useCrudImportExport } from '../composables/useCrudImportExport'

vi.mock('@/utils', () => ({ downloadFile: vi.fn(), request: vi.fn() }))
vi.mock('@/utils/encrypt-request', () => ({ postEncrypt: vi.fn() }))
const instances = []
let downloads
function setup(overrides = {}, api = {}) {
  const props = reactive({ apiConfig: {}, showExport: true, showExportTasks: true, publicParams: {}, ...overrides })
  const searchParams = ref({ name: '查询', tenant: '被覆盖' })
  const parseApiConfig = vi.fn((key, fallback, method) => api[key] || { method, url: fallback })
  const extractApiUrl = vi.fn(value => value.includes('@') ? value.split('@')[1] : value)
  const loadList = vi.fn()
  const runtime = useCrudImportExport({ props, searchParams, parseApiConfig, extractApiUrl, loadList })
  instances.push(runtime)
  return { ...runtime, props, searchParams, parseApiConfig, loadList }
}
const excel = () => new Blob(['excel'], { type: 'application/vnd.ms-excel' })
function jsonBlob(data) {
  const blob = new Blob([JSON.stringify(data)], { type: 'application/json' })
  // jsdom 的 Blob 尚无 text()；模拟浏览器原生 API。
  blob.text = async () => JSON.stringify(data)
  return blob
}
function mockAsync(response, task = { id: 'task-1', status: 'RUNNING', progress: 25 }) {
  request.mockImplementation(async ({ url }) => {
    if (url.endsWith('/tasks'))
      return { data: { records: [], total: 1 } }
    if (url.includes('/tasks/'))
      return { data: task }
    return response
  })
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.resetAllMocks()
  downloads = []
  window.$message = { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  vi.stubGlobal('URL', class extends URL {
    static createObjectURL = vi.fn(() => 'blob:download')
    static revokeObjectURL = vi.fn()
  })
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function () {
    downloads.push(this.download)
  })
})
afterEach(() => {
  instances.splice(0).forEach(runtime => runtime.clearExportTaskPollTimer())
  vi.useRealTimers()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('导入导出运行态', () => {
  it('上传包含文件、附加字段、请求头和进度，加密方法转普通 multipart', async () => {
    const runtime = setup({ importData: { count: 0, empty: '', skip: null, absent: undefined }, importHeaders: { 'X-Test': 'yes' } }, { import: { method: 'postEncrypt', url: '/import' } })
    request.mockResolvedValue({ data: { success: true } })
    const file = new File(['data'], 'test.xlsx')
    const progress = vi.fn()
    expect(await runtime.executeImport(file, progress)).toEqual({ success: true })
    const config = request.mock.lastCall[0]
    expect(config).toMatchObject({ method: 'post', url: '/import', encrypt: false, headers: { 'X-Test': 'yes' } })
    expect([...config.data.keys()]).toEqual(['file', 'count', 'empty'])
    expect(config.data.get('file')).toBe(file)
    config.onUploadProgress({ loaded: 1, total: 3 })
    config.onUploadProgress({ loaded: 1 })
    expect(progress).toHaveBeenCalledTimes(1)
    expect(progress).toHaveBeenCalledWith(33)
    runtime.handleShowImport()
    expect(runtime.importModalVisible.value).toBe(true)
    runtime.handleImportSuccess()
    expect(runtime.loadList).toHaveBeenCalledOnce()
  })

  it.each([
    [{ 'content-disposition': 'attachment; filename*=utf-8\'\'%E6%A8%A1%E6%9D%BF.xlsx' }, '模板.xlsx'],
    [{ get: () => 'attachment; filename="normal.xlsx"' }, 'normal.xlsx'],
    [{}, '导入模板.xlsx'],
  ])('模板下载保持 Blob 参数和文件名 %s', async (headers, expected) => {
    const runtime = setup({ importTemplateUrl: '/template' })
    request.mockResolvedValue({ data: excel(), headers })
    await runtime.handleDownloadTemplate()
    expect(request).toHaveBeenCalledWith({ method: 'get', url: '/template', responseType: 'blob', rawResponse: true, encrypt: false })
    expect(downloads).toEqual([expected])
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:download')
  })

  it('无模板和无任务接口保留提示，配置变化仍响应式生效', async () => {
    const runtime = setup()
    await runtime.handleDownloadTemplate()
    await runtime.handleOpenExportTasks()
    expect(request).not.toHaveBeenCalled()
    expect(window.$message.warning).toHaveBeenCalledTimes(2)
    expect(runtime.hasImportTemplate.value).toBe(false)
    expect(runtime.showExportTaskEntry.value).toBe(false)
    runtime.props.apiConfig = { importTemplate: 'get@/template', export: 'post@/ai/crud/order/export?format=xlsx' }
    expect(runtime.hasImportTemplate.value).toBe(true)
    expect(runtime.showExportTaskEntry.value).toBe(true)
    runtime.props.showExportTasks = false
    expect(runtime.showExportTaskEntry.value).toBe(false)
  })

  it('无 export 地址时仍可用列表接口反推导出任务入口', () => {
    const runtime = setup({
      apiConfig: { list: 'get@/ai/crud/hr_business_object_bkhu/page?designPreview=1' },
    })
    expect(runtime.showExportTaskEntry.value).toBe(true)
  })

  it.each(['get', 'post', 'postEncrypt'])('同步导出 %s 保持参数合并及加密分支', async (method) => {
    const runtime = setup({ publicParams: { tenant: '公共值' } }, { export: { method, url: '/export' } })
    request.mockResolvedValue(excel())
    postEncrypt.mockResolvedValue(excel())
    runtime.searchParams.value.name = '新查询'
    await runtime.handleExport()
    const params = { name: '新查询', tenant: '公共值' }
    if (method === 'postEncrypt') {
      expect(postEncrypt).toHaveBeenCalledWith('/export', params, { responseType: 'blob', rawResponse: true })
      expect(request).not.toHaveBeenCalled()
    }
    else {
      expect(request).toHaveBeenCalledWith({ method, url: '/export', data: method === 'get' ? undefined : params, params: method === 'get' ? params : undefined, responseType: 'blob', rawResponse: true, encrypt: false })
    }
    expect(downloads).toEqual(['导出数据.xlsx'])
    expect(runtime.exportLoading.value).toBe(false)
  })

  it.each([
    () => ({ async: true, taskId: 'task-1' }),
    () => ({ data: { async: true, taskId: 'task-1' } }),
    () => jsonBlob({ async: true, taskId: 'task-1' }),
    () => ({ data: jsonBlob({ data: { async: true, taskId: 'task-1' } }) }),
  ])('异步 JSON/Blob 响应打开任务抽屉并按两秒轮询', async (response) => {
    const runtime = setup({ exportApi: '/ai/crud/order/export' })
    mockAsync(response())
    await runtime.handleExport()
    await vi.advanceTimersByTimeAsync(0)
    expect(runtime.exportTaskDrawerVisible.value).toBe(true)
    expect(runtime.activeExportTask.value).toMatchObject({ id: 'task-1', status: 'RUNNING' })
    expect(runtime.exportTaskPaginationConfig.value.itemCount).toBe(1)
    expect(downloads).toEqual([])
    const calls = request.mock.calls.length
    await vi.advanceTimersByTimeAsync(1999)
    expect(request).toHaveBeenCalledTimes(calls)
    await vi.advanceTimersByTimeAsync(1)
    expect(request).toHaveBeenCalledTimes(calls + 1)
    runtime.clearExportTaskPollTimer()
    await vi.advanceTimersByTimeAsync(5000)
    expect(request).toHaveBeenCalledTimes(calls + 1)
  })

  it.each(['SUCCESS', 'FAILED'])('%s 终态不再轮询并保留消息', async (status) => {
    const runtime = setup({ exportApi: '/ai/crud/order/export' })
    mockAsync({ async: true, taskId: 'task-1' }, { id: 'task-1', status, errorMessage: '失败原因' })
    await runtime.handleExport()
    await vi.advanceTimersByTimeAsync(10000)
    expect(request).toHaveBeenCalledTimes(3)
    expect(runtime.isExportTaskRunning(runtime.activeExportTask.value)).toBe(false)
    expect(status === 'SUCCESS' ? window.$message.success : window.$message.error).toHaveBeenLastCalledWith(status === 'SUCCESS' ? '导出任务已完成' : '失败原因')
  })

  it('任务分页、修改每页数量及文件下载保留原协议', async () => {
    const runtime = setup({ exportTaskConfigKey: 'order' })
    request.mockResolvedValue({ data: { records: [{ id: '1' }], total: 23 } })
    await runtime.handleOpenExportTasks()
    runtime.exportTaskPaginationConfig.value.onUpdatePage(2)
    await Promise.resolve()
    expect(request.mock.lastCall[0]).toMatchObject({ url: '/ai/crud/order/export/tasks', params: { pageNum: 2, pageSize: 10 } })
    runtime.exportTaskPaginationConfig.value.onUpdatePageSize(20)
    await Promise.resolve()
    expect(request.mock.lastCall[0].params).toEqual({ pageNum: 1, pageSize: 20 })
    expect(runtime.exportTaskPaginationConfig.value.itemCount).toBe(23)
    await runtime.handleDownloadExportTask({ fileId: 'long-id', fileName: '导出.xlsx' })
    expect(downloadFile).toHaveBeenCalledWith('long-id', '导出.xlsx')
    const action = runtime.exportTaskColumns.value.find(column => column.key === 'actions')
    expect(action.render({ status: 'RUNNING' }).props.disabled).toBe(true)
    expect(action.render({ status: 'SUCCESS', fileId: 'long-id' }).props.disabled).toBe(false)
  })

  it('两实例弹窗状态与轮询清理互不影响', async () => {
    const first = setup({ exportApi: '/ai/crud/first/export' })
    const second = setup({ exportApi: '/ai/crud/second/export' })
    mockAsync({ async: true, taskId: 'task-1' })
    first.handleShowImport()
    expect(second.importModalVisible.value).toBe(false)
    await first.handleExport()
    await second.handleExport()
    await vi.advanceTimersByTimeAsync(0)
    first.clearExportTaskPollTimer()
    request.mockClear()
    await vi.advanceTimersByTimeAsync(2000)
    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/ai/crud/second/export/tasks/task-1' }))
  })
})
