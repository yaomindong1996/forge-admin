export const REMIND_COOLDOWN_MS = 10 * 60 * 1000

const RUNNING_STATUSES = ['0', '1', 'RUNNING', 'IN_PROCESS']

// 后端对未签收任务会拒绝催办，所以只有运行中且已有办理人的任务才展示入口。
export function canRemindTask(task) {
  if (!task) return false
  const status = String(task.status ?? '').toUpperCase()
  return RUNNING_STATUSES.includes(status) && Boolean(task.assignee || status === '1')
}

export function remindTaskId(task = {}) {
  return String(task?.taskId || task?.id || '')
}

export function isRemindCooling(cooldowns, taskId, now = Date.now()) {
  return Number(cooldowns?.[String(taskId)] || 0) > now
}

export function markRemindCooldown(cooldowns, taskId, now = Date.now()) {
  if (taskId) cooldowns[String(taskId)] = now + REMIND_COOLDOWN_MS
  return cooldowns
}

export function isRemindThrottledMessage(message) {
  return String(message || '').includes('已催办过')
}
