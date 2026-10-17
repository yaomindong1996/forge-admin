import dayjs from 'dayjs'

export const RECENT_DOWNLOADS_LIMIT = 500

export function downloadScope(tenantId, userId) {
  return tenantId != null && userId != null ? JSON.stringify([String(tenantId), String(userId)]) : ''
}

export function downloadDay(value) {
  if (!value)
    return '更早'
  const date = dayjs(value)
  return date.isValid() && date.isSame(dayjs(), 'day') ? '今天' : '更早'
}

export function downloadTime(value) {
  const date = value ? dayjs(value) : null
  if (!date?.isValid())
    return '时间未知'
  return date.isSame(dayjs(), 'day') ? date.format('HH:mm') : date.format('MM-DD HH:mm')
}

export function downloadFileType(name = '') {
  const text = String(name)
  const extension = text.includes('.') ? text.split('.').pop().toLowerCase() : ''
  if (['xls', 'xlsx', 'csv'].includes(extension))
    return { icon: 'i-lucide:sheet', tone: 'sheet', extension }
  if (extension === 'pdf')
    return { icon: 'i-lucide:file-text', tone: 'pdf', extension }
  if (['zip', 'rar', '7z', 'gz', 'tar'].includes(extension))
    return { icon: 'i-lucide:file-archive', tone: 'archive', extension }
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'].includes(extension))
    return { icon: 'i-lucide:image', tone: 'image', extension }
  return { icon: 'i-lucide:file-text', tone: 'document', extension: extension?.slice(0, 5) || 'FILE' }
}

export function progressPercentage(task) {
  if (!(task.total > 0) || !(task.loaded >= 0))
    return null
  return Math.min(100, Math.max(0, Math.floor(task.loaded / task.total * 100)))
}

const HIDDEN_LIMIT = RECENT_DOWNLOADS_LIMIT
const namespace = import.meta.env.VITE_TENANT || import.meta.env.BASE_URL || 'default'
const storageKey = scope => `forge-recent-downloads-hidden:${namespace}:${scope}`

export function readHiddenDownloads(scope) {
  if (!scope)
    return []
  try {
    const value = JSON.parse(localStorage.getItem(storageKey(scope)) || '[]')
    return Array.isArray(value) ? value.filter(id => typeof id === 'string').slice(-HIDDEN_LIMIT) : []
  }
  catch {
    return []
  }
}

export function writeHiddenDownloads(scope, ids) {
  const hidden = [...new Set(ids.map(String))].slice(-HIDDEN_LIMIT)
  try {
    localStorage.setItem(storageKey(scope), JSON.stringify(hidden))
  }
  catch {
    // 私密模式或存储配额不足时，只在当前内存生效，不能中断文件下载。
  }
  return hidden
}

export function hasDownloadDock(layout, { narrow, collapsed, showSidebar }) {
  if (layout === 'bento')
    return true
  if (narrow)
    return false
  if (layout === 'side-flyout')
    return !collapsed
  if (layout === 'nexus')
    return !collapsed
  if (['normal', 'simple'].includes(layout))
    return true
  return ['full', 'top-side-menu'].includes(layout) && showSidebar
}
