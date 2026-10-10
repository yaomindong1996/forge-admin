export const SIGN_MODE_PARALLEL = 'PARALLEL'
export const SIGN_RELATION_ACTIVE = 1

// 后端只允许当前办理人或任务所有者加签/减签，发起人不行。
export function isTaskActor(task, userId) {
  const id = String(userId || '')
  if (!id || !task) return false
  return [task.assignee, task.owner].some(value => value != null && String(value) === id)
}

export function activeSignRelations(relations) {
  return (Array.isArray(relations) ? relations : []).filter(item => Number(item?.status) === SIGN_RELATION_ACTIVE)
}

// 节点未配置 allowAddSign 时视为允许；减签不受节点策略限制，保证已加入的人员始终可以移除。
export function canAddSign({ task, userId, policy = {}, readonly = false } = {}) {
  return !readonly && policy?.allowAddSign !== false && isTaskActor(task, userId)
}

export function canReduceSign({ task, userId, relations, readonly = false } = {}) {
  return !readonly && isTaskActor(task, userId) && activeSignRelations(relations).length > 0
}

export function signUserName(relation = {}) {
  return relation?.targetUserName || relation?.targetUserId || '未知人员'
}

export function signStatusText(relation = {}) {
  return Number(relation?.status) === SIGN_RELATION_ACTIVE ? '有效' : '已撤回'
}

export function buildSignExcludeIds({ userId, task, relations } = {}) {
  const ids = [userId, task?.assignee, ...activeSignRelations(relations).map(item => item.targetUserId)]
  return [...new Set(ids.filter(id => id != null && id !== '').map(String))]
}
