import { sanitizeHtml } from '@/utils/sanitize-html'

const PLACEHOLDER_RE = /\$\{([a-z_]\w*)\}|\{([a-z_]\w*)\}/gi
const HTML_TAG_RE = /<[a-z][\s\S]*>/i

export function parseNotificationDate(value) {
  if (!value)
    return null
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return new Date(year, month - 1, day, hour, minute, second)
  }
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * 列表预览：卡片模板是 HTML（gray/normal/highlight），通知中心不能用 {{ }} 原样输出。
 * 先用 templateParams 填占位符，再剥标签；残留 ${xxx} 的行丢掉，避免历史脏数据刷屏。
 */
export function formatNotificationPreview(content, params = {}) {
  const raw = String(content ?? '').trim()
  if (!raw)
    return ''

  const filled = raw.replace(PLACEHOLDER_RE, (match, dollarKey, braceKey) => {
    const key = dollarKey || braceKey
    const value = params?.[key]
    return value == null || value === '' ? match : String(value)
  })

  // 卡片模板是多个 <div> 行；先换成换行再剥标签，保留「任务 / 流程」分行预览
  let text = filled
  if (HTML_TAG_RE.test(filled)) {
    const normalized = sanitizeHtml(filled)
      .replace(/<\s*br\s*\/?>/gi, '\n')
      .replace(/<\/\s*(div|p|li|tr|h[1-6])\s*>/gi, '\n')
      .replace(/<[^>]+>/g, '')
    text = normalized
      .replace(/&nbsp;/gi, ' ')
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>')
      .replace(/&amp;/gi, '&')
      .replace(/&quot;/gi, '"')
  }

  return text
    .split(/\r?\n+/)
    .map((line) => {
      PLACEHOLDER_RE.lastIndex = 0
      // 占位符未填完整的行直接丢掉，避免留下「流程：」这类残句
      if (PLACEHOLDER_RE.test(line))
        return ''
      return line.replace(/\s+/g, ' ').trim()
    })
    .filter(Boolean)
    .join('\n')
    .trim()
}

export function mergeNotifications(messages, notices, isUnread) {
  const entries = messages.map((message) => {
    const params = message.templateParams || message.params || {}
    return {
      ...message,
      source: 'message',
      key: `message:${message.id}`,
      readFlag: Number(message.readFlag),
      preview: formatNotificationPreview(message.content, params),
    }
  })
  notices.forEach((notice) => {
    const document = new DOMParser().parseFromString(sanitizeHtml(notice.noticeContent), 'text/html')
    const content = document.body.textContent?.trim() || ''
    entries.push({
      source: 'notice', key: `notice:${notice.noticeId}`, id: notice.noticeId,
      title: notice.noticeTitle, content, preview: content,
      createTime: notice.publishTime, readFlag: isUnread(notice) ? 0 : 1,
      noticeType: notice.noticeType, isTop: notice.isTop,
    })
  })
  return entries.sort((a, b) => (parseNotificationDate(b.createTime)?.getTime() || 0) - (parseNotificationDate(a.createTime)?.getTime() || 0))
}

export function filterNotifications(entries, category, readState, rangeDays, now = Date.now()) {
  return entries.filter((entry) => {
    const kind = entry.source === 'notice' ? 'notice' : isFlowApprovalMessage(entry) ? 'approval' : 'other'
    if (category !== 'all' && category !== kind)
      return false
    if (readState !== 'all' && Number(entry.readFlag) !== Number(readState))
      return false
    return !rangeDays || (parseNotificationDate(entry.createTime)?.getTime() || 0) >= now - rangeDays * 86400000
  })
}

export function isFlowApprovalMessage(message) {
  return message?.bizType === 'FLOW_TODO'
}

export function isPendingFlowApprovalMessage(message) {
  return isFlowApprovalMessage(message) && Number(message?.readFlag) === 0
}

export function mergeMessageNavigationTarget(target, query = {}) {
  const extraQuery = Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
  if (typeof target === 'string') {
    return Object.keys(extraQuery).length
      ? { path: target, query: extraQuery }
      : target
  }
  if (!target || typeof target !== 'object')
    return target
  return {
    ...target,
    query: {
      ...(target.query || {}),
      ...extraQuery,
    },
  }
}
