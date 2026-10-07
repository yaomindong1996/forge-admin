import { ref } from 'vue'
import { uploadPluginPackage } from '@/api/system/pluginTask'
import { createPluginRequestId } from './pluginTaskUtils'

/** 网络结果不确定时保留同一请求 ID，可幂等重试，不另起重复任务。 */
export function usePluginUpload(run, busy, error) {
  const pendingUpload = ref(null)
  function upload(file) {
    if (busy.value)
      return
    if (!file || !/\.zip$/i.test(file.name) || file.size > 8 * 1024 * 1024) {
      error.value = '请选择不超过 8 MiB 的 ZIP 包'
      return
    }
    pendingUpload.value = { file, id: createPluginRequestId() }
    return retryUpload()
  }
  function retryUpload() {
    const upload = pendingUpload.value
    return upload && run(() => uploadPluginPackage(upload.file, upload.id))
  }
  function clear() {
    pendingUpload.value = null
  }
  return { pendingUpload, upload, retryUpload, clear }
}
