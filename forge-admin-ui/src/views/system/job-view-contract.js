function normalizeRoutePath(path) {
  const value = String(path || '').trim()
  if (!value)
    return ''
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = String(pathname || '').replace(/\/+$/, '')
  if (!normalized)
    return '/'
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

export function resolveJobExecutionMode(row = {}, runModeOptions = [], invokeModeOptions = []) {
  const invokeMode = String(row.invokeMode || '').toUpperCase()
  const flowInvocation = invokeMode === 'FLOW'
  return {
    options: flowInvocation ? invokeModeOptions : runModeOptions,
    value: flowInvocation ? invokeMode : row.executeMode,
  }
}

export function hasAccessibleRoute(accessRoutes = [], targetPath) {
  const normalizedTarget = normalizeRoutePath(targetPath)
  if (!normalizedTarget || !Array.isArray(accessRoutes))
    return false
  return accessRoutes.some(route => normalizeRoutePath(route?.path) === normalizedTarget)
}
