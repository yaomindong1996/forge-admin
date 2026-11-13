/**
 * 与管理端 views/flow/utils/priority.js 保持一致：0~3 为业务级别；
 * 其它数值按 Flowable 优先级换算，默认 50 属于普通，不能当作紧急。
 */
export function resolveFlowPriorityLevel(priority) {
  if (priority === undefined || priority === null || priority === '')
    return null
  const value = Number(priority)
  if (!Number.isFinite(value))
    return null
  if (value >= 0 && value <= 3)
    return value
  if (value >= 80)
    return 3
  if (value >= 60)
    return 2
  if (value >= 40)
    return 1
  return 0
}

export function isUrgentFlowPriority(priority) {
  return resolveFlowPriorityLevel(priority) === 3
}
