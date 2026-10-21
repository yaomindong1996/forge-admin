/**
 * 应用卡片 / 工作台标题用的图标规范化。
 * 市场包与调试导入曾写入 i-lucide:*；这类 class 来自业务数据，UnoCSS 扫不到，
 * 卡片上会只剩色块。统一落到 IconRenderer 可稳定渲染的 ionicons5。
 */
const LUCIDE_TO_IONICONS = Object.freeze({
  'i-lucide:calendar-days': 'ionicons5:CalendarOutline',
  'i-lucide:calendar': 'ionicons5:CalendarOutline',
  'i-lucide:file-text': 'ionicons5:DocumentTextOutline',
  'i-lucide:user-plus': 'ionicons5:PersonAddOutline',
  'i-lucide:badge-check': 'ionicons5:RibbonOutline',
  'i-lucide:app-window': 'ionicons5:AppsOutline',
})

const DEFAULT_APP_ICON = 'ionicons5:AppsOutline'

export function resolveApplicationDisplayIcon(icon) {
  const raw = String(icon || '').trim()
  if (!raw)
    return DEFAULT_APP_ICON
  if (
    raw.startsWith('ionicons5:')
    || raw.startsWith('forge:')
    || raw.startsWith('forge-color:')
    || raw.startsWith('local-image:')
    || raw.startsWith('http://')
    || raw.startsWith('https://')
    || raw.startsWith('/')
    || raw.startsWith('data:')
    || /^[a-f0-9]{32}$/i.test(raw)
  ) {
    return raw
  }
  if (LUCIDE_TO_IONICONS[raw])
    return LUCIDE_TO_IONICONS[raw]
  // 其它 UnoCSS 动态图标类同样不可靠，给稳定兜底，避免空色块
  if (raw.startsWith('i-') || raw.startsWith('mdi-') || raw.startsWith('ai-'))
    return DEFAULT_APP_ICON
  return raw
}
