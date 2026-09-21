const SUPER_PERMISSIONS = new Set(['**', '*:*:*'])

function includesPermission(source, permission) {
  return Array.isArray(source)
    && source.some(grant => SUPER_PERMISSIONS.has(grant) || grant === permission)
}

export function hasPrintPermission(user, permission) {
  if (!permission)
    return false
  if (user?.isAdmin)
    return true
  return includesPermission(user?.permissions, permission)
    || includesPermission(user?.getDataPermission, permission)
}
