export const CONTACT_PAGE_SIZE = 20

const CJK_PATTERN = /[\u3400-\u9FFF]/

/**
 * 头像缺省时的文字：中文姓名取后两字，其余取前两个字母
 */
export function contactInitials(name) {
  const text = String(name || '').trim()
  if (!text) return '?'
  if (CJK_PATTERN.test(text)) return text.slice(-2)
  return text.replace(/\s+/g, '').slice(0, 2).toUpperCase()
}

export function contactSubtitle(member = {}) {
  const org = Array.isArray(member.orgNames) ? member.orgNames[0] : ''
  const post = Array.isArray(member.postNames) ? member.postNames[0] : ''
  return [org, post].filter(Boolean).join(' · ')
}

/**
 * 只有完整号码才允许拨打；脱敏号码（含 *）直接拨会拨错人
 */
export function canDial(phone) {
  const value = String(phone || '').trim()
  return /^\+?[\d\s-]{3,20}$/.test(value)
}

export function normalizeContactPage(data) {
  const records = Array.isArray(data)
    ? data
    : data?.records || data?.list || data?.rows || []
  const total = Number(data?.total ?? records.length) || 0
  return { records, total }
}

/**
 * H5 会自动解码路由参数而小程序不会，二次解码含 % 的文本会抛错
 */
export function safeDecode(value) {
  const text = String(value ?? '')
  try { return decodeURIComponent(text) }
  catch { return text }
}

export function buildContactOrgUrl({ orgId, orgName } = {}) {
  const query = []
  if (orgId) query.push(`orgId=${encodeURIComponent(String(orgId))}`)
  if (orgName) query.push(`orgName=${encodeURIComponent(String(orgName))}`)
  return query.length ? `/pages/contacts/org?${query.join('&')}` : '/pages/contacts/org'
}

export function buildContactMemberUrl(userId) {
  return `/pages/contacts/member?userId=${encodeURIComponent(String(userId || ''))}`
}
