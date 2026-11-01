/**
 * 平台管理表单统一布局：与用户管理一致。
 * 24 栅格，常规字段 span=8（一行三项），整行字段 span=24。
 */

export const SYSTEM_EDIT_GRID_COLS = 24
export const SYSTEM_EDIT_FIELD_SPAN = 8
export const SYSTEM_EDIT_FULL_SPAN = 24

/** AiCrudPage / 快捷新增弹窗共用 */
export const SYSTEM_EDIT_FORM_LAYOUT = {
  gridCols: SYSTEM_EDIT_GRID_COLS,
  labelPlacement: 'top',
  labelAlign: 'left',
  labelWidth: 'auto',
  xGap: 16,
  yGap: 8,
  formClass: 'system-edit-form',
  modalWidth: '960px',
}

/**
 * 规范化单字段 span（兼容旧 2 列栅格的 span=1/2）
 * @param {object} field
 * @param {{ full?: boolean }} [opts]
 */
export function normalizeSystemEditField(field, opts = {}) {
  if (!field || field.type === 'divider')
    return null
  const next = { ...field }
  const forceFull = opts.full
    || next.fullSpan
    || next.type === 'textarea'
    || next.type === 'slot' && next.slotName === 'appearance'
  if (forceFull) {
    next.span = SYSTEM_EDIT_FULL_SPAN
    return next
  }
  if (next.span == null || next.span === 1)
    next.span = SYSTEM_EDIT_FIELD_SPAN
  else if (next.span === 2)
    // 旧 2 列下的整行 → 新布局整行
    next.span = SYSTEM_EDIT_FULL_SPAN
  return next
}

/**
 * 将分区字段包成 AiForm card（顶标签三列布局）
 * @param {Array<{ label: string, icon?: string, fields: object[] }>} sections
 */
export function buildSystemEditCards(sections = []) {
  return sections
    .filter(section => section && Array.isArray(section.fields) && section.fields.length)
    .map((section) => {
      const children = section.fields
        .map(field => normalizeSystemEditField(field))
        .filter(Boolean)
      return {
        type: 'card',
        nodeType: 'card',
        label: section.label || '基础信息',
        props: {
          icon: section.icon || 'forge:module',
          bordered: true,
        },
        children,
      }
    })
}
