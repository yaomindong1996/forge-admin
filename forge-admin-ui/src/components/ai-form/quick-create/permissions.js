/**
 * 表单内快捷新增：权限判断。
 *
 * 菜单按钮码（如 system:dict:add / system:role:add）优先看 userStore.permissions；
 * apiPermissions 中的 URL 模式不参与冒号权限码匹配，避免把接口通配误当成业务 add 权限。
 * 超管 isAdmin / * / ** / *:*:* 放行。提交仍由后端 API 鉴权兜底。
 */

function hasWildcard(grants = []) {
  const set = new Set(grants)
  return set.has('*') || set.has('**') || set.has('*:*:*')
}

function matchesPermission(grants = [], target = '') {
  if (!target)
    return false
  const set = new Set(grants)
  if (set.has(target))
    return true
  let splitIndex = target.lastIndexOf(':')
  while (splitIndex > 0) {
    const wildcard = `${target.slice(0, splitIndex)}:*`
    if (set.has(wildcard))
      return true
    splitIndex = target.lastIndexOf(':', splitIndex - 1)
  }
  return false
}

/** 仅收集菜单/按钮权限码；不含 API URL，防绕过 */
export function collectUserPermissionCodes(userStore) {
  return Array.isArray(userStore?.permissions) ? userStore.permissions : []
}

/** @deprecated 使用 collectUserPermissionCodes；保留别名以免旧测试引用 */
export function collectUserGrants(userStore) {
  return collectUserPermissionCodes(userStore)
}

export function hasQuickCreatePermission(userStore, permission) {
  if (!permission)
    return false
  if (userStore?.isAdmin)
    return true
  const grants = collectUserPermissionCodes(userStore)
  if (hasWildcard(grants))
    return true
  return matchesPermission(grants, permission)
}
