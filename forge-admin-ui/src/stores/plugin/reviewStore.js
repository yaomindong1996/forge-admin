import { defineStore } from 'pinia'
import { ref } from 'vue'

// 表单与请求编排共享同一个冻结请求；不层层传递，也不把核查说明持久化到浏览器存储。
export const usePluginReviewStore = defineStore('plugin-review', () => {
  const pending = ref(null)
  function prepare(task, decision, form) {
    if (pending.value) {
      if (pending.value.taskId !== task.id)
        throw new Error('请先返回上一任务刷新核查未确认的请求')
      return
    }
    pending.value = Object.freeze({
      taskId: task.id,
      requestId: crypto.randomUUID(),
      revision: task.revision,
      sha256: task.sha256,
      resultSha256: task.execution?.resultSha256 || null,
      decision,
      executorStopped: form.executorStopped,
      notDeployed: form.notDeployed,
      artifactsReviewed: decision === 'approve_build' ? form.artifactsReviewed : null,
      migrationsReviewed: decision === 'approve_build' ? form.migrationsReviewed : null,
      note: form.note.trim(),
    })
  }
  function clear() {
    pending.value = null
  }
  function reconcile(task) {
    // 新版本说明任务已经流转，不能用旧页面再提交新操作；重新查看历史判断结果。
    if (pending.value && task && pending.value.taskId === task.id && task.revision > pending.value.revision)
      clear()
  }
  return { pending, prepare, clear, reconcile }
})
