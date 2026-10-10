import {
  createDelivery,
  listDeliveries,
  listDeliveryCandidates,
  listDeliveryTargets,
  reconcileDelivery,
} from '@/api/system/pluginDelivery'
import { createPluginRequestId, taskResponse } from '@/views/system/plugin/pluginTaskUtils'

function confirmedCreation(response, command) {
  const row = taskResponse(response)
  if (!row.id || !['targetId', 'taskId', 'releaseId', 'action'].every(key => row[key] === command[key]))
    throw new Error('交付任务响应与冻结请求不一致')
  return row
}

async function loadDeliveries(state, sequence) {
  const { targets, candidates, tasks, loading, error } = state
  const current = ++sequence.value
  loading.value = true
  try {
    const values = await Promise.all([listDeliveryTargets(), listDeliveryCandidates(), listDeliveries()])
    const data = values.map(taskResponse)
    if (current !== sequence.value)
      return
    if (!data.every(Array.isArray)) {
      throw new Error('交付列表响应不完整')
    }
    targets.value = data[0]
    candidates.value = data[1]
    tasks.value = data[2]
    error.value = ''
  }
  catch {
    if (current === sequence.value)
      error.value = '加载交付目标或记录失败，请重试'
  }
  finally {
    if (current === sequence.value)
      loading.value = false
  }
}

async function submitDelivery(state, load) {
  const { busy, pending, complete, draft, candidate, error } = state
  if (busy.value || (!pending.value && !complete.value))
    return
  // 请求只在内存中冻结；失联不能重新生成requestId或改变确认依据。
  pending.value ||= Object.freeze({
    ...draft,
    taskId: candidate.value.taskId,
    requestId: createPluginRequestId(),
    note: draft.note.trim(),
  })
  busy.value = true
  try {
    confirmedCreation(await createDelivery(pending.value), pending.value)
    pending.value = null
    draft.note = ''
    await load()
  }
  catch {
    error.value = '任务提交结果未确认；请先刷新核查，重试会保留同一请求，不要重新创建'
  }
  finally { busy.value = false }
}

async function closeDelivery(state, input, load) {
  const { busy, error } = state
  const { id, command } = input
  if (busy.value || !command.executorStopped || command.note.trim().length < 10)
    return false
  busy.value = true
  try {
    taskResponse(await reconcileDelivery(id, command))
    await load()
    return true
  }
  catch {
    error.value = '人工关闭未完成，请刷新核查任务状态'
    return false
  }
  finally { busy.value = false }
}

export function createDeliveryActions(state) {
  const sequence = { value: 0 }
  const load = () => loadDeliveries(state, sequence)
  return {
    load,
    submit: () => submitDelivery(state, load),
    reconcile: (id, command) => closeDelivery(state, { id, command }, load),
  }
}
