/**
 * 表单栅格布局共享配置与纯函数（从 ForgePropertyPanel 下沉，coding-style 7.5）
 *
 * 表单级面板（panels/FormLayoutPanel 等）与组件级栅格逻辑共用，
 * 统一在此维护，禁止在组件内复制粘贴。
 */

/** 表单栅格最大列数（24 栅格） */
export const MAX_FORM_GRID_COLUMNS = 24

/** 可选列数序列 1..24 */
export const GRID_COLUMN_OPTIONS = Array.from({ length: MAX_FORM_GRID_COLUMNS }).map((_, index) => index + 1)

/** 滑杆刻度标记（只在关键档位显示数字） */
export const GRID_COLUMN_MARKS = GRID_COLUMN_OPTIONS.reduce((marks, item) => {
  if (![1, 6, 12, 18, 24].includes(item))
    return marks
  marks[item] = `${item}`
  return marks
}, {})

/** 栅格数归一化：非法值回退 2，越界收敛到 [1, max] */
export function normalizeGridCount(value, max = MAX_FORM_GRID_COLUMNS) {
  const number = Number(value)
  return Math.max(1, Math.min(max, Number.isFinite(number) ? number : 2))
}

/** 标签宽度输入归一化：空/auto → 'auto'，纯数字 → number，其余原样返回 */
export function normalizeLabelWidthInput(value) {
  const text = String(value ?? '').trim()
  if (!text || text === 'auto')
    return 'auto'
  const number = Number(text)
  return Number.isFinite(number) ? number : text
}
