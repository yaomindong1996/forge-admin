/**
 * 表单栅格布局共享配置与纯函数（从 ForgePropertyPanel 下沉，coding-style 7.5）
 *
 * 表单级面板（panels/FormLayoutPanel 等）与组件级栅格逻辑共用，
 * 统一在此维护，禁止在组件内复制粘贴。
 */

/**
 * 嵌套 row/col 等仍按 24 栅格算跨度；
 * 「表单列数」指根表单一行几个字段：快捷 1–4，也可自定义到 FORM_LAYOUT_COLUMN_MAX。
 */
export const MAX_FORM_GRID_COLUMNS = 24

/** 根表单字段列数上限（属性面板「表单列数」可自定义） */
export const FORM_LAYOUT_COLUMN_MAX = 12

/** 可选列数序列 1..24（字段跨度等） */
export const GRID_COLUMN_OPTIONS = Array.from({ length: MAX_FORM_GRID_COLUMNS }).map((_, index) => index + 1)

/** 表单列数快捷档位 */
export const FORM_LAYOUT_COLUMN_PRESETS = [1, 2, 3, 4]

/** 24 栅格跨度滑杆刻度（字段/格子 span，不是「表单列数」） */
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

/** 表单列数归一化（默认上限 4） */
export function normalizeFormLayoutColumns(value) {
  return normalizeGridCount(value, FORM_LAYOUT_COLUMN_MAX)
}

/** 标签宽度输入归一化：空/auto → 'auto'，纯数字 → number，其余原样返回 */
export function normalizeLabelWidthInput(value) {
  const text = String(value ?? '').trim()
  if (!text || text === 'auto')
    return 'auto'
  const number = Number(text)
  return Number.isFinite(number) ? number : text
}
