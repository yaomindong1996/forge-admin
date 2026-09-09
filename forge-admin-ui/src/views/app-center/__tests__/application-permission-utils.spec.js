import { describe, expect, it } from 'vitest'
import {
  applyApplicationDataScopeSettings,
  buildApplicationDataScopeSettings,
  buildApplicationPermissionModules,
  buildRolePermissionPayload,
  createLatestRequestGuard,
  normalizeRolePermission,
  rolePermissionEquals,
  summarizeRolePermission,
} from '../application-permission-utils'

describe('application permission utilities', () => {
  it('normalizes resource and module grants into a stable save payload', () => {
    const grant = normalizeRolePermission({
      roleId: '9',
      resourceIds: [4, '2', 4, null],
      moduleScopes: { 'ai:business:ORDER': '3', '': 5, 'ignored': null },
    })

    expect(buildRolePermissionPayload(grant)).toEqual({
      resourceIds: ['2', '4'],
      moduleScopes: [{ moduleCode: 'ai:business:ORDER', dataScope: 3 }],
    })
    expect(rolePermissionEquals(grant, {
      resourceIds: [4, 2],
      moduleScopes: { 'ai:business:ORDER': 3 },
    })).toBe(true)
  })

  it('summarizes only registered application resources', () => {
    const workspace = {
      pages: [
        { resourceId: 11, registered: true },
        { resourceId: null, registered: false },
      ],
      objects: [{ actions: [
        { resourceId: 21, registered: true },
        { resourceId: 22, registered: false },
      ] }],
    }

    expect(summarizeRolePermission(workspace, {
      resourceIds: [11, 21, 22],
      moduleScopes: { 'ai:business:ORDER': 5 },
    })).toEqual({ pages: 1, actions: 1, dataScopes: 1 })
  })

  it('rejects stale role permission requests after a role switch', () => {
    const guard = createLatestRequestGuard()
    const first = guard.next()
    const second = guard.next()

    expect(guard.isCurrent(first)).toBe(false)
    expect(guard.isCurrent(second)).toBe(true)
  })

  it('builds page entry and object permission modules for the shared role workbench', () => {
    const modules = buildApplicationPermissionModules({
      pages: [
        { pageId: 'home', pageName: '应用首页', permissionCode: 'app:page:home', resourceId: '11', registered: true },
        { pageId: 'draft', pageName: '待发布页', permissionCode: 'app:page:draft', registered: false, pendingLabel: '待发布' },
      ],
      objects: [{
        objectId: '91',
        objectCode: 'ORDER',
        objectName: '订单',
        moduleCode: 'ai:business:ORDER',
        dataScopeReady: false,
        actions: [
          { actionCode: 'list', actionName: '查看列表', permissionCode: 'ai:business:ORDER:list', resourceId: '21', registered: true },
          { actionCode: 'edit', actionName: '编辑', permissionCode: 'ai:business:ORDER:edit', registered: false },
        ],
      }],
    })

    expect(modules).toHaveLength(2)
    expect(modules[0].name).toBe('页面入口')
    expect(modules[0].pages[0].accessItem.resourceIds).toEqual(['11'])
    expect(modules[0].pages[1]).toMatchObject({ accessItem: null, accessUnavailableLabel: '待发布' })
    expect(modules[1].pages[0]).toMatchObject({
      moduleCode: 'ai:business:ORDER',
      objectPermission: true,
      showDataScopePanel: true,
      auxActionLabel: '配置适配',
      auxActionPayload: {
        type: 'DATA_SCOPE_ADAPTER',
        objectId: '91',
        objectCode: 'ORDER',
      },
    })
    expect(modules[1].pages[0].actionItems).toEqual([
      expect.objectContaining({ label: '查看列表', resourceIds: ['21'], disabled: false }),
      expect.objectContaining({ label: '编辑', resourceIds: [], disabled: true }),
    ])
  })

  it('keeps inherited object scopes out of the save payload', () => {
    const workspace = {
      objects: [
        { objectCode: 'ORDER', objectName: '订单', moduleCode: 'ai:business:ORDER', dataScopeReady: true },
        { objectCode: 'CUSTOMER', objectName: '客户', moduleCode: 'ai:business:CUSTOMER', dataScopeReady: false },
      ],
    }
    const grant = {
      defaultDataScope: 5,
      moduleScopes: { 'ai:business:ORDER': 3 },
    }
    const settings = buildApplicationDataScopeSettings(workspace, grant)

    expect(settings).toEqual({
      defaultDataScope: 5,
      modules: [{
        moduleCode: 'ai:business:ORDER',
        moduleName: '订单',
        dataScope: 3,
        effectiveDataScope: 3,
        flowRelatedVisible: true,
      }],
    })

    const nextGrant = applyApplicationDataScopeSettings(grant, {
      ...settings,
      modules: settings.modules.map(module => ({ ...module, dataScope: null })),
    })
    expect(buildRolePermissionPayload(nextGrant).moduleScopes).toEqual([])
  })
})
