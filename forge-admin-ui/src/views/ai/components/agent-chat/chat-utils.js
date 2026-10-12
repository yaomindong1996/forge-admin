/**
 * agent-chat 子组件共享纯函数（阶段四#1 拆分）。
 *
 * 这些都是无副作用的展示/格式化辅助，原先内联在 agent-chat.vue，
 * 拆分子组件后由父组件与各子组件按需导入，避免多处重复定义。
 */

// 头像取色：按 key 稳定散列到固定色板
const AVATAR_COLORS = ['#2563eb', '#7c3aed', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#14b8a6']

export function colorFor(key) {
  const s = String(key || 'agent')
  let h = 0
  for (let i = 0; i < s.length; i += 1)
    h = (h * 31 + s.charCodeAt(i)) >>> 0
  return AVATAR_COLORS[h % AVATAR_COLORS.length]
}

// 预设问题解析：兼容数组 / JSON 字符串 / 多行文本
export function parsePresetQuestions(v) {
  if (!v)
    return []
  if (Array.isArray(v))
    return v.filter(Boolean)
  if (typeof v === 'string') {
    try {
      const arr = JSON.parse(v)
      if (Array.isArray(arr))
        return arr.filter(Boolean)
      return arr ? [String(arr)] : []
    }
    catch {
      return v.split('\n').map(s => s.trim()).filter(Boolean)
    }
  }
  return []
}

export function hasVal(v) {
  return v !== '' && v !== null && v !== undefined
}

export function formatToolValue(v) {
  if (v === null || v === undefined)
    return ''
  if (typeof v === 'string')
    return v
  try {
    return JSON.stringify(v, null, 2)
  }
  catch {
    return String(v)
  }
}

export function toolStatusLabel(status) {
  switch (status) {
    case 'running': return '执行中'
    case 'done': return '完成'
    case 'success': return '完成'
    case 'error': return '失败'
    case 'waiting_confirm': return '等待确认'
    case 'aborted': return '未执行'
    default: return status || ''
  }
}

// 耗时展示：<1s 显示毫秒，≥1s 显示秒（保留 1 位小数）
export function formatDuration(ms) {
  if (ms == null)
    return ''
  if (ms < 1000)
    return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

export function getStatusLabel(status) {
  switch (status) {
    case 'streaming': return '生成中'
    case 'waiting_confirm': return '等待确认'
    case 'error': return '失败'
    case 'aborted': return '已停止'
    case 'done': return '已完成'
    default: return status || ''
  }
}

// 会话时间分组桶：今天 / 近7天 / 更早
// 返回值必须与 agent-chat.vue sessionGroups 的 group.key 一致（today/recent/older），
// 否则 groups.find(g => g.key === key) 匹配不上、会话被静默丢弃 → 列表渲染为空白。
export function formatSessionBucket(t) {
  if (!t)
    return 'older'
  const d = new Date(String(t).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime()))
    return 'older'
  const now = new Date()
  const sevenDaysAgo = new Date(now)
  sevenDaysAgo.setDate(now.getDate() - 7)
  if (d.toDateString() === now.toDateString())
    return 'today'
  if (d >= sevenDaysAgo)
    return 'recent'
  return 'older'
}

// 会话列表时间戳：今天显示 HH:mm，本年显示 MM-DD，跨年显示 YYYY-MM-DD
export function formatTime(t) {
  if (!t)
    return ''
  const d = new Date(String(t).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime()))
    return ''
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  if (d.toDateString() === now.toDateString())
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (d.getFullYear() === now.getFullYear())
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
