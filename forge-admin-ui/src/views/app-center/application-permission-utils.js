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

export function buildApplicationPermissionModules(workspace = {}) {
  const modules = []
  const pages = (workspace.pages || []).map((page, index) => {
    const registered = Boolean(page.registered && page.resourceId != null)
    return {
      key: `application-page:${page.pageId || index}`,
      name: page.pageName || '未命名页面',
      path: page.permissionCode || '',
      accessItem: registered
        ? {
            key: `access:${page.resourceId}`,
            label: '页面入口',
            resourceIds: [String(page.resourceId)],
            permission: page.permissionCode || '',
          }
        : null,
      accessUnavailableLabel: registered ? '' : page.pendingLabel || '待发布',
      actionItems: [],
      showDataScopePanel: false,
      showFunctionPanel: false,
    }
  })

  if (pages.length) {
    modules.push({
      key: 'application:pages',
      name: '页面入口',
      path: '角色可访问的应用入口与页面',
      pages,
    })
  }

  for (const [objectIndex, object] of (workspace.objects || []).entries()) {
    const objectCode = object.objectCode || `object-${objectIndex}`
    const actionItems = (object.actions || []).map((action, actionIndex) => {
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
    modules.push({
      key: `application-object:${objectCode}`,
      name: object.objectName || objectCode,
      path: objectCode,
      pages: [{
        key: `application-object-page:${objectCode}`,
        name: `${object.objectName || objectCode}权限`,
        path: objectCode,
        accessItem: null,
        objectPermission: true,
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
      }],
    })
  }

  return modules
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
