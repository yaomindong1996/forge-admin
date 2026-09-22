import { resolveSwitchValuePair } from '@/views/app-center/components/designer/forge-form-designer/field-default-value'

/**
 * 列表开关列：判断是否应按 switch 渲染（含存量无 render.type 时从 editSchema / componentType 推断）。
 */
export function isSwitchColumnConfig(col = {}, editField = null) {
  const renderType = col?.render?.type || col?.renderType || col?.componentType
  if (String(renderType || '').trim() === 'switch')
    return true
  const fieldType = editField?.type || editField?.componentType
  return String(fieldType || '').trim() === 'switch'
}

export function resolveSwitchColumnValuePair(col = {}, editField = null) {
  const render = col?.render && typeof col.render === 'object' ? col.render : {}
  return resolveSwitchValuePair({
    checkedValue: render.checkedValue ?? col.checkedValue ?? editField?.checkedValue,
    uncheckedValue: render.uncheckedValue ?? col.uncheckedValue ?? editField?.uncheckedValue,
    props: {
      ...(editField?.props || {}),
      checkedValue: render.checkedValue ?? col.checkedValue ?? editField?.props?.checkedValue,
      uncheckedValue: render.uncheckedValue ?? col.uncheckedValue ?? editField?.props?.uncheckedValue,
      checkedText: render.checkedText ?? col.checkedText ?? editField?.checkedText ?? editField?.props?.checkedText,
      uncheckedText: render.uncheckedText ?? col.uncheckedText ?? editField?.uncheckedText ?? editField?.props?.uncheckedText,
    },
    checkedText: render.checkedText ?? col.checkedText ?? editField?.checkedText,
    uncheckedText: render.uncheckedText ?? col.uncheckedText ?? editField?.uncheckedText,
  }, editField)
}

export function normalizeSwitchCellValue(raw, checkedValue = 1, uncheckedValue = 0) {
  if (raw === checkedValue || raw === true || raw === 'true' || raw === 1 || raw === '1')
    return checkedValue
  if (raw === uncheckedValue || raw === false || raw === 'false' || raw === 0 || raw === '0')
    return uncheckedValue
  if (raw === null || raw === undefined || raw === '')
    return uncheckedValue
  return raw
}

export function resolveSwitchColumnTexts(col = {}, editField = null) {
  const render = col?.render && typeof col.render === 'object' ? col.render : {}
  return {
    checkedText: render.checkedText ?? col.checkedText ?? editField?.checkedText ?? editField?.props?.checkedText ?? '',
    uncheckedText: render.uncheckedText ?? col.uncheckedText ?? editField?.uncheckedText ?? editField?.props?.uncheckedText ?? '',
  }
}
