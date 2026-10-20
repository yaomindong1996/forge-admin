function uniqueIds(values) {
  return [...new Set((values || [])
    .filter(value => value !== null && value !== undefined && value !== '')
    .map(value => String(value)))]
    .sort((left, right) => left.localeCompare(right, undefined, { numeric: true }))
}

function normalizeModuleScopes(moduleScopes) {
  if (!moduleScopes || Array.isArray(moduleScopes))
    return {}
  return Object.fromEntries(Object.entries(moduleScopes)
    .filter(([moduleCode, dataScope]) => moduleCode && dataScope !== null && dataScope !== undefined)
    .map(([moduleCode, dataScope]) => [moduleCode, Number(dataScope)]))
}

export function normalizeRolePermission(grant = {}) {
  return {
    roleId: grant.roleId == null ? null : String(grant.roleId),
    roleName: grant.roleName || '',
    roleKey: grant.roleKey || '',
    defaultDataScope: grant.defaultDataScope == null ? null : Number(grant.defaultDataScope),
    resourceIds: uniqueIds(grant.resourceIds),
    moduleScopes: normalizeModuleScopes(grant.moduleScopes),
  }
}

export function cloneRolePermission(grant = {}) {
  const normalized = normalizeRolePermission(grant)
  return {
    ...normalized,
    resourceIds: [...normalized.resourceIds],
    moduleScopes: { ...normalized.moduleScopes },
  }
}

export function buildRolePermissionPayload(draft = {}) {
  const normalized = normalizeRolePermission(draft)
  return {
    resourceIds: normalized.resourceIds,
    moduleScopes: Object.entries(normalized.moduleScopes)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([moduleCode, dataScope]) => ({ moduleCode, dataScope })),
  }
}

export function rolePermissionEquals(left, right) {
  return JSON.stringify(buildRolePermissionPayload(left)) === JSON.stringify(buildRolePermissionPayload(right))
}

export function summarizeRolePermission(workspace = {}, grant = {}) {
  const resourceIds = new Set(uniqueIds(grant.resourceIds))
  const pages = (workspace.pages || []).filter(item => item.registered && resourceIds.has(String(item.resourceId))).length
  const actions = (workspace.objects || [])
    .flatMap(item => item.actions || [])
    .filter(item => item.registered && resourceIds.has(String(item.resourceId)))
    .length
  const dataScopes = Object.keys(normalizeModuleScopes(grant.moduleScopes)).length
  return { pages, actions, dataScopes }
}

function buildObjectActionItems(object = {}, objectIndex = 0) {
  const objectCode = object.objectCode || `object-${objectIndex}`
  return (object.actions || []).map((action, actionIndex) => {
    const registered = Boolean(action.registered && action.resourceId != null)
    return {
      key: `action:${objectCode}:${action.actionCode || actionIndex}`,
      actionKey: action.actionCode || String(actionIndex),
      sectionKey: 'button',
      label: action.actionName || action.actionCode || '未命名操作',
      resourceIds: registered ? [String(action.resourceId)] : [],
      permissions: action.permissionCode ? [action.permissionCode] : [],
      sources: [{
        type: 3,
        kind: registered ? 'button' : 'pending',
        label: registered ? '按钮' : '待发布',
      }],
      disabled: !registered,
    }
  })
}

function attachObjectCapabilities(pageCard, object, objectIndex = 0) {
  if (!object)
    return pageCard
  const actionItems = buildObjectActionItems(object, objectIndex)
  return {
    ...pageCard,
    actionItems,
    moduleCode: object.moduleCode || '',
    showDataScopePanel: true,
    showFunctionPanel: true,
    dataScopeUnavailableText: object.dataScopeReady
      ? ''
      : '当前对象仅启用租户隔离，完成数据范围适配后才能按角色设置范围。',
    auxActionLabel: '配置适配',
    auxActionPayload: {
      type: 'DATA_SCOPE_ADAPTER',
      objectId: object.objectId,
      objectCode: object.objectCode,
    },
  }
}

/**
 * 应用权限工作台：按页面一级配置。
 * 页面勾选 = 门户可见；绑定对象的功能权限与数据范围挂在同一页面卡片下，不再分「页面入口 / 业务对象」两栏。
 */
export function buildApplicationPermissionModules(workspace = {}) {
  const objects = workspace.objects || []
  const objectByCode = new Map(objects
    .filter(object => object?.objectCode)
    .map(object => [String(object.objectCode), object]))
  const attachedObjectCodes = new Set()

  const pages = (workspace.pages || []).map((page, index) => {
    const registered = Boolean(page.registered && page.resourceId != null)
    const boundObject = page.objectCode ? objectByCode.get(String(page.objectCode)) : null
    if (boundObject?.objectCode)
      attachedObjectCodes.add(String(boundObject.objectCode))

    let pageCard = {
      key: `application-page:${page.pageId || index}`,
      name: page.pageName || '未命名页面',
      path: page.permissionCode || '',
      accessItem: registered
        ? {
            key: `access:${page.resourceId}`,
            label: '可访问',
            resourceIds: [String(page.resourceId)],
            permission: page.permissionCode || '',
          }
        : null,
      accessUnavailableLabel: registered ? '' : page.pendingLabel || '待发布',
      hideAccessTag: true,
      actionItems: [],
      showDataScopePanel: false,
      showFunctionPanel: false,
    }
    if (boundObject)
      pageCard = attachObjectCapabilities(pageCard, boundObject, objects.indexOf(boundObject))
    return pageCard
  })

  // 未挂到任何页面的对象：仍放进同一「页面」模块，避免丢功能授权能力
  objects.forEach((object, objectIndex) => {
    const code = object?.objectCode ? String(object.objectCode) : ''
    if (!code || attachedObjectCodes.has(code))
      return
    pages.push(attachObjectCapabilities({
      key: `application-orphan-object:${code}`,
      name: object.objectName || code,
      path: code,
      accessItem: null,
      objectPermission: true,
      hideAccessTag: true,
      actionItems: [],
      showDataScopePanel: false,
      showFunctionPanel: false,
    }, object, objectIndex))
  })

  if (!pages.length)
    return []

  return [{
    key: 'application:pages',
    name: '页面',
    path: '按页面配置访问、功能权限与数据范围',
    pages,
  }]
}

export function buildApplicationDataScopeSettings(workspace = {}, grant = {}) {
  const normalized = normalizeRolePermission(grant)
  return {
    defaultDataScope: normalized.defaultDataScope,
    modules: (workspace.objects || [])
      .filter(object => object.dataScopeReady && object.moduleCode)
      .map((object) => {
        const dataScope = normalized.moduleScopes[object.moduleCode] ?? null
        return {
          moduleCode: object.moduleCode,
          moduleName: object.objectName || object.objectCode || object.moduleCode,
          dataScope,
          effectiveDataScope: dataScope ?? normalized.defaultDataScope,
          flowRelatedVisible: object.dataScopeAdapter?.flowRelatedVisible !== false,
        }
      }),
  }
}

export function applyApplicationDataScopeSettings(grant = {}, settings = {}) {
  const normalized = cloneRolePermission(grant)
  normalized.moduleScopes = Object.fromEntries((settings.modules || [])
    .filter(module => module.moduleCode && module.dataScope !== null && module.dataScope !== undefined)
    .map(module => [module.moduleCode, Number(module.dataScope)]))
  return normalized
}

export function createLatestRequestGuard() {
  let sequence = 0
  return {
    next() {
      sequence += 1
      return sequence
    },
    isCurrent(requestId) {
      return requestId === sequence
    },
  }
}
