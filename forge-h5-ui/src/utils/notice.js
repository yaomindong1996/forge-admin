export const NOTICE_PAGE_SIZE = 20

export function normalizeNoticePage(data) {
  const records = Array.isArray(data) ? data : data?.records || data?.list || data?.rows || []
  return {
    records: records.filter(item => item && item.noticeId != null),
    total: Number(data?.total ?? records.length) || 0,
  }
}

// isRead 只有明确为 1 才算已读；本次会话里刚读过的公告由 readIds 补齐。
export function isNoticeUnread(notice, readIds = []) {
  if (!notice) return false
  if (readIds.includes(String(notice.noticeId))) return false
  return Number(notice.isRead) !== 1
}

export function isNoticeTop(notice) {
  return Number(notice?.isTop) === 1
}

export function filterNotices(records, tab, readIds = []) {
  const list = Array.isArray(records) ? records : []
  return tab === 'unread' ? list.filter(item => isNoticeUnread(item, readIds)) : list
}

export function formatFileSize(bytes) {
  const size = Number(bytes)
  if (!Number.isFinite(size) || size <= 0) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export function buildNoticeDetailUrl(noticeId) {
  return `/pages/notice/detail?noticeId=${encodeURIComponent(String(noticeId ?? ''))}`
}
