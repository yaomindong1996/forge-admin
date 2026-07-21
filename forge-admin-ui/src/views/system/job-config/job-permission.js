export const JOB_PERMISSIONS = Object.freeze({
  configList: 'system:jobConfig:list',
  configAdd: 'system:jobConfig:add',
  configEdit: 'system:jobConfig:edit',
  configRemove: 'system:jobConfig:remove',
  configStart: 'system:jobConfig:start',
  configStop: 'system:jobConfig:stop',
  configTrigger: 'system:jobConfig:trigger',
  configSync: 'system:jobConfig:sync',
  configDangerous: 'system:jobConfig:dangerous',
  logList: 'system:jobLog:list',
  logDetail: 'system:jobLog:detail',
  logExport: 'system:jobLog:export',
  logClean: 'system:jobLog:clean',
  apiTokenList: 'system:jobApiToken:list',
  apiTokenAdd: 'system:jobApiToken:add',
  apiTokenRevoke: 'system:jobApiToken:revoke',
  apiTokenRotate: 'system:jobApiToken:rotate',
})

export function hasJobPermission(userStore, route, permission) {
  if (userStore?.isAdmin)
    return true
  const grants = new Set([
    ...(Array.isArray(userStore?.permissions) ? userStore.permissions : []),
    ...resolveRouteButtonCodes(route),
  ])
  return grants.has(permission) || grants.has('**') || grants.has('*:*:*')
}

function resolveRouteButtonCodes(route) {
  const buttons = route?.meta?.btns
  if (!Array.isArray(buttons))
    return []
  return buttons
    .map(item => typeof item === 'string' ? item : item?.code)
    .filter(Boolean)
}
