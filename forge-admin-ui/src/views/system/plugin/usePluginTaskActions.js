import { onScopeDispose, ref } from 'vue'
import { cancelPluginTask, confirmPluginTask, getPluginTask } from '@/api/system/pluginTask'
import { taskResponse } from './pluginTaskUtils'
import { useLatestPluginRequest } from './useLatestPluginRequest'
import { usePluginUpload } from './usePluginUpload'

export function usePluginTaskActions(refresh) {
  const detailRequest = useLatestPluginRequest(async id => taskResponse(await getPluginTask(id)), true)
  const visible = ref(false)
  const busy = ref(false)
  const actionError = ref('')
  const uploader = usePluginUpload(run, busy, actionError)
  let selectedId = ''
  let disposed = false
  onScopeDispose(() => {
    disposed = true
  })

  function open(id = selectedId) {
    if (busy.value)
      return
    selectedId = id
    visible.value = true
    actionError.value = ''
    return detailRequest.run(id)
  }
  function close() {
    if (busy.value)
      return
    visible.value = false
    detailRequest.cancel()
  }
  async function refreshDetail() {
    await open()
    if (!disposed && visible.value && !busy.value)
      await refresh()
  }
  async function run(action) {
    if (busy.value)
      return
    busy.value = true
    actionError.value = ''
    try {
      const task = taskResponse(await action())
      if (disposed)
        return
      detailRequest.cancel()
      detailRequest.error.value = ''
      detailRequest.data.value = task
      selectedId = task.id
      visible.value = true
      uploader.clear()
      await refresh()
    }
    catch (error) {
      if (!disposed)
        actionError.value = error?.message || '操作失败；结果不确定时请刷新任务列表'
    }
    finally {
      if (!disposed)
        busy.value = false
    }
  }
  const task = detailRequest.data
  const confirm = () => run(() => confirmPluginTask(task.value))
  const cancel = () => run(() => cancelPluginTask(task.value))
  const { pendingUpload, upload, retryUpload } = uploader
  return {
    task,
    visible,
    busy,
    actionError,
    pendingUpload,
    open,
    close,
    upload,
    retryUpload,
    refreshDetail,
    confirm,
    cancel,
    detailLoading: detailRequest.loading,
    detailError: detailRequest.error,
  }
}
