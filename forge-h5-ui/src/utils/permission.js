export const ALL_PERMISSION = '*:*:*'

export const FLOW_PERMISSIONS = Object.freeze({
  start: 'ai:businessFlow:start',
  runtime: 'ai:businessDocument:view',
  withdraw: 'ai:businessDocument:withdraw',
  remind: 'flow:task:remind',
})

// 前端判断只用于隐藏按钮，后端注解才是权限依据。
export function hasPermission(permissions, code) {
  if (!code || !Array.isArray(permissions) || !permissions.length) return false
  return permissions.includes(ALL_PERMISSION) || permissions.includes(code)
}
