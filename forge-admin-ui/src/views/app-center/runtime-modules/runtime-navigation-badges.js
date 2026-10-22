/**
 * 运行页左侧系统导航角标：待办数 / 未读抄送 / 通知未读红点。
 * 失败时静默为 0，不阻断侧栏渲染。
 */
import flowApi from '@/api/flow'
import { noticeApi } from '@/api/notice'

export function createEmptyNavigationBadges() {
  return {
    todoCount: 0,
    ccCount: 0,
    messageUnread: 0,
  }
}

function readTotal(payload) {
  const data = payload?.data ?? payload
  const total = Number(data?.total)
  if (Number.isFinite(total) && total >= 0)
    return Math.floor(total)
  const list = data?.records || data?.list || data?.rows || data?.items
  return Array.isArray(list) ? list.length : 0
}

function readCount(payload) {
  const data = payload?.data ?? payload
  if (typeof data === 'number' && Number.isFinite(data))
    return Math.max(0, Math.floor(data))
  const count = Number(data?.count ?? data?.unreadCount ?? data?.total)
  return Number.isFinite(count) && count >= 0 ? Math.floor(count) : 0
}

export async function loadRuntimeNavigationBadges() {
  const next = createEmptyNavigationBadges()
  const [todoRes, ccRes, noticeRes] = await Promise.allSettled([
    flowApi.getTodoTasks({ pageNum: 1, pageSize: 1 }),
    flowApi.getUnreadCcCount(),
    noticeApi.unreadCount(),
  ])
  if (todoRes.status === 'fulfilled')
    next.todoCount = readTotal(todoRes.value)
  if (ccRes.status === 'fulfilled')
    next.ccCount = readCount(ccRes.value)
  if (noticeRes.status === 'fulfilled')
    next.messageUnread = readCount(noticeRes.value)
  return next
}

/**
 * @param {string} pageId
 * @param {{ todoCount?: number, ccCount?: number, messageUnread?: number }} badges
 * @returns {{ count: number, dot: boolean }}
 */
export function resolveSystemPageBadge(pageId, badges = {}) {
  const id = String(pageId || '')
  if (id === 'system:todo') {
    const count = Number(badges.todoCount) || 0
    return { count, dot: false }
  }
  if (id === 'system:cc') {
    const count = Number(badges.ccCount) || 0
    return { count, dot: false }
  }
  if (id === 'system:messages') {
    const count = Number(badges.messageUnread) || 0
    return { count: 0, dot: count > 0 }
  }
  return { count: 0, dot: false }
}

export function formatNavigationBadgeCount(count = 0) {
  const n = Number(count) || 0
  if (n <= 0)
    return ''
  return n > 99 ? '99+' : String(n)
}
