/**
 * 设计态选中描边误写入 props.style 后，预览/运行会当成真实边框。
 * 识别主色选中对（Naive #2080f0 / Tailwind #2563eb / 主题蓝 + 浅蓝底），渲染时剥离并支持写回清理。
 */

function normalizeCssColor(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')
}

const PRIMARY_BORDER = new Set([
  '#2080f0',
  '#2563eb',
  '#165dff',
  '#3370ff',
  'rgb(32,128,240)',
  'rgba(32,128,240,1)',
  'rgb(37,99,235)',
  'rgba(37,99,235,1)',
  'rgb(22,93,255)',
  'rgba(22,93,255,1)',
])

const PRIMARY_SUPPL_BG = new Set([
  '#f3f8ff',
  '#fafdff',
  '#e8edff',
  'rgb(243,248,255)',
  'rgba(243,248,255,1)',
  'rgb(250,253,255)',
  'rgba(250,253,255,1)',
])

function isThemePrimaryBorder(border) {
  return border.includes('var(--primary-color')
    || border.includes('var(--n-primary-color')
}

export function isAccidentalSelectionChrome(style = {}) {
  const border = normalizeCssColor(style.borderColor)
  const bg = normalizeCssColor(style.backgroundColor)
  const width = Number.parseFloat(style.borderWidth)
  if (!(Number.isFinite(width) && width >= 2))
    return false
  if (!PRIMARY_BORDER.has(border) && !isThemePrimaryBorder(border))
    return false
  // 浅蓝底或透明底 + 主色 ≥2px 描边，视为选中态残留
  return !bg || bg === 'transparent' || PRIMARY_SUPPL_BG.has(bg)
}

export function stripAccidentalSelectionChrome(style = {}) {
  if (!isAccidentalSelectionChrome(style))
    return style
  const bg = normalizeCssColor(style.backgroundColor)
  return {
    ...style,
    borderColor: 'transparent',
    borderWidth: 0,
    borderStyle: 'none',
    backgroundColor: PRIMARY_SUPPL_BG.has(bg) ? 'transparent' : style.backgroundColor,
  }
}

export function selectionChromeClearPatch() {
  return {
    borderColor: 'transparent',
    borderWidth: 0,
    borderStyle: 'none',
    backgroundColor: 'transparent',
  }
}
