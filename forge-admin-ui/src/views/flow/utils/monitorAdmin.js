export function normalizeInstanceStatus(status) {
  return String(status || '').trim().toLowerCase()
}

export function canInterveneInstance(status) {
  const value = normalizeInstanceStatus(status)
  return value === 'running' || value === 'active' || value === 'suspended'
}

export function canMutateRunningInstance(status) {
  const value = normalizeInstanceStatus(status)
  return value === 'running' || value === 'active'
}

export function isSuspendedInstance(status) {
  return normalizeInstanceStatus(status) === 'suspended'
}

export function compactParams(source = {}) {
  const result = {}
  Object.entries(source).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '')
      result[key] = value
  })
  return result
}

export function buildMonitorFormQuery(row = {}) {
  return compactParams({
    taskId: row.taskId,
    businessKey: row.businessKey,
    processInstanceId: row.processInstanceId || row.id,
    processDefKey: row.processDefKey || row.processDefinitionKey,
    taskDefKey: row.taskDefKey || row.taskDefinitionKey,
    objectCode: row.objectCode,
    recordId: row.recordId,
    formKey: row.formKey,
  })
}

export function buildCurrentTaskOptions(tasks = []) {
  return (Array.isArray(tasks) ? tasks : []).map(task => ({
    label: [task.name || task.taskName || '当前任务', task.assigneeName || task.assignee || '待认领']
      .filter(Boolean)
      .join(' · '),
    value: String(task.id || task.taskId || ''),
    task,
  })).filter(item => item.value)
}

export function isPlatformAdmin(userStore) {
  if (userStore?.isAdmin)
    return true
  const userType = userStore?.userType ?? userStore?.userInfo?.userType
  return Number(userType) === 0
}

export function hasMonitorPermission(userStore, route, permission) {
  if (isPlatformAdmin(userStore))
    return true
  const grants = new Set([
    ...(Array.isArray(userStore?.permissions) ? userStore.permissions : []),
    ...resolveRouteButtonCodes(route),
  ])
  return grants.has(permission) || grants.has('**') || grants.has('*:*:*')
}

function resolveRouteButtonCodes(route) {
  const buttons = route?.meta?.btns
  if (!Array.isArray(buttons))
    return []
  return buttons
    .map(item => typeof item === 'string' ? item : item?.code)
    .filter(Boolean)
}

// 回退只能回到已完成过的历史节点：正在进行的节点(endTime 为空)不可选，多次经过的节点只列一次
export function buildRollbackActivityOptions(activities = []) {
  const active = new Set()
  const options = new Map()
  for (const item of Array.isArray(activities) ? activities : []) {
    if (!item?.activityId)
      continue
    if (!item.endTime)
      active.add(item.activityId)
    else if (!options.has(item.activityId))
      options.set(item.activityId, { label: item.activityName || item.activityId, value: item.activityId })
  }
  return [...options.values()].filter(option => !active.has(option.value))
}
