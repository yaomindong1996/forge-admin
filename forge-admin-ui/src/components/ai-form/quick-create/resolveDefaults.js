/**
 * 解析 quickCreate.defaultsFromForm / 预设默认值中的 $form.xxx 引用。
 */

function readFormPath(formData = {}, path = '') {
  const segments = String(path || '').split('.').filter(Boolean)
  let current = formData
  for (const segment of segments) {
    if (current === null || current === undefined)
      return undefined
    current = current[segment]
  }
  return current
}

export function resolveFormReference(rawValue, formData = {}) {
  if (typeof rawValue !== 'string')
    return rawValue
  const matched = rawValue.match(/^\$\{(.+)\}$/) || rawValue.match(/^\$form\.(.+)$/)
  if (!matched)
    return rawValue
  return readFormPath(formData, matched[1])
}

export function resolveDefaultsFromForm(defaults = {}, formData = {}) {
  const result = {}
  Object.entries(defaults || {}).forEach(([key, value]) => {
    result[key] = resolveFormReference(value, formData)
  })
  return result
}

/** 用户表单岗位预填：mainOrgId 优先，否则 orgIds[0] */
export function resolveUserFormOrgId(formData = {}) {
  const mainOrgId = formData?.mainOrgId
  if (mainOrgId !== null && mainOrgId !== undefined && mainOrgId !== '')
    return mainOrgId
  const orgIds = formData?.orgIds
  if (Array.isArray(orgIds) && orgIds.length)
    return orgIds[0]
  return null
}
