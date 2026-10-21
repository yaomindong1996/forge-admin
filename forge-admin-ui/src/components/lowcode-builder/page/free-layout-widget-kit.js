/**
 * 自由布局组件共享能力：色调、跳转、图标默认值。
 * 各区块属性面板 / 运行态渲染统一走这里，方便后续扩展。
 */

export const FREE_LAYOUT_TONE_OPTIONS = Object.freeze([
  { label: '主题蓝', value: 'primary' },
  { label: '成功绿', value: 'success' },
  { label: '警告橙', value: 'warning' },
  { label: '信息青', value: 'info' },
  { label: '危险红', value: 'danger' },
  { label: '中性灰', value: 'neutral' },
  { label: '自定义色', value: 'custom' },
])

export const FREE_LAYOUT_LINK_TYPE_OPTIONS = Object.freeze([
  { label: '无跳转', value: 'none' },
  { label: '应用内页面', value: 'page' },
  { label: '外部链接', value: 'url' },
])

export const FREE_LAYOUT_TONE_COLORS = Object.freeze({
  primary: { color: '#2563eb', soft: 'rgba(37, 99, 235, 0.12)' },
  success: { color: '#16a34a', soft: 'rgba(22, 163, 74, 0.12)' },
  warning: { color: '#d97706', soft: 'rgba(217, 119, 6, 0.14)' },
  info: { color: '#0284c7', soft: 'rgba(2, 132, 199, 0.12)' },
  danger: { color: '#dc2626', soft: 'rgba(220, 38, 38, 0.12)' },
  neutral: { color: '#64748b', soft: 'rgba(100, 116, 139, 0.12)' },
})

export const FREE_LAYOUT_INFO_TYPE_OPTIONS = Object.freeze([
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
])

const HEX_RE = /^#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$/i

export function isHexColor(value) {
  return HEX_RE.test(String(value || '').trim())
}

export function resolveTonePalette(tone = 'primary', customColor = '') {
  const key = String(tone || 'primary').trim() || 'primary'
  if (key === 'custom' && isHexColor(customColor)) {
    const color = String(customColor).trim()
    return { tone: 'custom', color, soft: hexToSoft(color) }
  }
  return {
    tone: FREE_LAYOUT_TONE_COLORS[key] ? key : 'primary',
    ...(FREE_LAYOUT_TONE_COLORS[key] || FREE_LAYOUT_TONE_COLORS.primary),
  }
}

function hexToSoft(hex) {
  const raw = String(hex).replace('#', '')
  const full = raw.length === 3
    ? raw.split('').map(ch => ch + ch).join('')
    : raw.slice(0, 6)
  const r = Number.parseInt(full.slice(0, 2), 16)
  const g = Number.parseInt(full.slice(2, 4), 16)
  const b = Number.parseInt(full.slice(4, 6), 16)
  if (![r, g, b].every(Number.isFinite))
    return 'rgba(37, 99, 235, 0.12)'
  return `rgba(${r}, ${g}, ${b}, 0.12)`
}

/**
 * 归一化快捷入口项：兼容旧 targetPageId，补齐 linkType / color。
 */
export function normalizeQuickEntryItem(item = {}, index = 0) {
  const targetPageId = String(item.targetPageId || item.pageId || item.targetPageKey || '').trim()
  const href = String(item.href || item.url || '').trim()
  let linkType = String(item.linkType || '').trim()
  if (!linkType) {
    if (href)
      linkType = 'url'
    else if (targetPageId)
      linkType = 'page'
    else
      linkType = 'none'
  }
  const tone = String(item.tone || 'primary').trim() || 'primary'
  return {
    key: item.key || `entry_${index + 1}`,
    title: String(item.title || `入口${index + 1}`).trim() || `入口${index + 1}`,
    description: String(item.description || '').trim(),
    icon: String(item.icon || 'GridOutline').trim() || 'GridOutline',
    tone,
    color: isHexColor(item.color) ? String(item.color).trim() : '',
    linkType,
    targetPageId,
    href,
  }
}

export function normalizeStatMetric(item = {}, index = 0) {
  const tone = String(item.tone || 'primary').trim() || 'primary'
  return {
    key: item.key || `metric_${index + 1}`,
    label: String(item.label || `指标${index + 1}`).trim() || `指标${index + 1}`,
    value: item.value ?? '-',
    trend: String(item.trend || '').trim(),
    icon: String(item.icon || '').trim(),
    tone,
    color: isHexColor(item.color) ? String(item.color).trim() : '',
    linkType: String(item.linkType || (item.targetPageId ? 'page' : 'none')).trim() || 'none',
    targetPageId: String(item.targetPageId || '').trim(),
    href: String(item.href || '').trim(),
    labelField: item.labelField || '',
    valueField: item.valueField || '',
    metaField: item.metaField || '',
  }
}

/** 运行态：从入口/指标项解析跳转动作 */
export function resolveWidgetLinkAction(item = {}) {
  const normalized = {
    linkType: String(item.linkType || '').trim(),
    targetPageId: String(item.targetPageId || item.pageId || item.targetPageKey || '').trim(),
    href: String(item.href || item.url || '').trim(),
  }
  if (!normalized.linkType) {
    if (normalized.href)
      normalized.linkType = 'url'
    else if (normalized.targetPageId)
      normalized.linkType = 'page'
    else
      normalized.linkType = 'none'
  }
  if (normalized.linkType === 'page' && normalized.targetPageId) {
    return {
      type: 'page',
      targetPageId: normalized.targetPageId,
      action: 'navigate',
    }
  }
  if (normalized.linkType === 'url' && normalized.href) {
    return {
      type: 'url',
      href: normalized.href,
      action: 'openUrl',
    }
  }
  return { type: 'none', action: '' }
}

export function resolveToneCssVars(tone = 'primary', customColor = '') {
  const palette = resolveTonePalette(tone, customColor)
  return {
    '--fl-tone-color': palette.color,
    '--fl-tone-soft': palette.soft,
  }
}
