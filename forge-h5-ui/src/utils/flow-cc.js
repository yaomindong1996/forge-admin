export const CC_PAGE_SIZE = 20

export const CC_READ_FILTERS = Object.freeze([
  { label: '全部', value: '' },
  { label: '未读', value: '0' },
])

export function normalizeCcPage(data) {
  const records = Array.isArray(data) ? data : data?.records || data?.list || data?.rows || []
  const safeRecords = Array.isArray(records) ? records.filter(item => item && item.id != null) : []
  return { records: safeRecords, total: Number(data?.total ?? safeRecords.length) || 0 }
}

export function normalizeCcUnreadCount(data) {
  const count = Number(typeof data === 'number' ? data : data?.count)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

export function formatCcUnread(value) {
  const count = normalizeCcUnreadCount(value)
  if (!count) return ''
  return count > 99 ? '99+' : String(count)
}

// 服务端 isRead 只有明确为 1 才算已读；本次会话里刚读过的记录由 readIds 补齐，避免返回列表时再刷新一次。
export function isCcUnread(cc, readIds = []) {
  if (!cc) return false
  return Number(cc.isRead) !== 1 && !readIds.includes(String(cc.id ?? ''))
}

export function ccTitle(cc = {}) {
  return cc.title || cc.processName || cc.processDefinitionName || '流程抄送'
}

export function ccSummary(cc = {}) {
  return cc.businessSummary || cc.content || ''
}

export function ccSender(cc = {}) {
  return cc.sendUserName || '系统'
}

export function buildCcDetailUrl(cc = {}) {
  const query = [`id=${encodeURIComponent(String(cc.id ?? ''))}`]
  if (cc.processInstanceId) query.push(`processInstanceId=${encodeURIComponent(String(cc.processInstanceId))}`)
  return `/pages/flow/cc-detail?${query.join('&')}`
}
