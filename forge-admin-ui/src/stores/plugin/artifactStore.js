import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { parseArtifactMetadata } from '@/views/system/plugin/pluginArtifactUtils'
import { createPluginRequestId } from '@/views/system/plugin/pluginTaskUtils'

// 草稿/冻结请求只在内存；结果不确定时保持原requestId，不制造重复登记。
export const usePluginArtifactStore = defineStore('plugin-artifact', () => {
  const taskId = ref('')
  const metadata = ref(null)
  const pending = ref(null)
  const error = ref('')
  const draft = reactive({ text: '', note: '', localVerified: false, notDeployed: false })
  function clear() {
    metadata.value = null
    pending.value = null
    error.value = ''
    Object.assign(draft, { text: '', note: '', localVerified: false, notDeployed: false })
  }
  function select(task) {
    if (taskId.value !== task?.id) {
      clear()
      taskId.value = task?.id || ''
    }
    // 已收到相同请求的审计记录后才清除冻结请求，不能只靠HTTP成功或任务修订变化猜结果。
    if (pending.value && task?.artifacts?.some(row => row.requestId === pending.value.requestId))
      clear()
  }
  function preview(task) {
    try {
      metadata.value = parseArtifactMetadata(draft.text, task)
      error.value = ''
    }
    catch (cause) {
      metadata.value = null
      error.value = cause.message || '登记元数据解析失败'
    }
  }
  function prepare(task) {
    if (pending.value)
      return
    if (task?.id !== taskId.value || task.status !== 'release_ready' || task.artifacts?.length)
      return
    preview(task)
    if (!metadata.value || !draft.localVerified || !draft.notDeployed || draft.note.trim().length < 10)
      return
    pending.value = Object.freeze({
      taskId: task.id,
      requestId: createPluginRequestId(),
      metadataJson: draft.text,
      localVerified: true,
      notDeployed: true,
      note: draft.note.trim(),
    })
  }
  return { taskId, draft, metadata, pending, error, clear, select, preview, prepare }
})
