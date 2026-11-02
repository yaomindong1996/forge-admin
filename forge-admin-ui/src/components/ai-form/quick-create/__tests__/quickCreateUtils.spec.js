import { describe, expect, it } from 'vitest'
import { hasQuickCreatePermission } from '../permissions'
import { resolveDefaultsFromForm, resolveUserFormOrgId } from '../resolveDefaults'
import { mergeQuickCreateSelection } from '../selectionBackfill'
import { isPlatformDictQuickCreateBlocked, isPlatformDictType } from '../presets/dictData'
import { buildSystemRoleEditSchema, createRoleKeyAutoCode } from '@/views/system/schemas/systemRoleEditSchema'
import { resolveQuickCreateDefinition } from '../registry'

describe('quickCreate permissions', () => {
  it('未命中权限时不展示入口', () => {
    const store = { isAdmin: false, permissions: ['system:user:list'], apiPermissions: [] }
    expect(hasQuickCreatePermission(store, 'system:role:add')).toBe(false)
    expect(hasQuickCreatePermission(store, 'system:dict:add')).toBe(false)
  })

  it('apiPermissions URL 不能冒充业务 add 权限码', () => {
    const store = {
      isAdmin: false,
      permissions: [],
      apiPermissions: ['/**', 'POST /system/dict/data/add'],
    }
    expect(hasQuickCreatePermission(store, 'system:dict:add')).toBe(false)
  })

  it('支持通配符与 admin', () => {
    expect(hasQuickCreatePermission({ isAdmin: true, permissions: [] }, 'system:role:add')).toBe(true)
    expect(hasQuickCreatePermission({ isAdmin: true, permissions: [] }, 'system:dict:add')).toBe(true)
    expect(hasQuickCreatePermission({ permissions: ['system:role:*'] }, 'system:role:add')).toBe(true)
    expect(hasQuickCreatePermission({ permissions: ['system:dict:add'] }, 'system:dict:add')).toBe(true)
    expect(hasQuickCreatePermission({ permissions: ['system:dict:edit'] }, [
      'system:dict:add',
      'system:dict:edit',
      'system:dict:remove',
    ])).toBe(true)
    expect(hasQuickCreatePermission({ permissions: ['system:user:add'] }, [
      'system:dict:add',
      'system:dict:edit',
    ])).toBe(false)
    expect(hasQuickCreatePermission({ permissions: ['*:*:*'] }, 'system:post:add')).toBe(true)
  })
})

describe('quickCreate defaults', () => {
  it('解析 $form 引用', () => {
    expect(resolveDefaultsFromForm({ orgId: '$form.mainOrgId' }, { mainOrgId: 9 })).toEqual({ orgId: 9 })
  })

  it('组织预填 mainOrgId 优先', () => {
    expect(resolveUserFormOrgId({ mainOrgId: 3, orgIds: [8, 9] })).toBe(3)
    expect(resolveUserFormOrgId({ orgIds: [8, 9] })).toBe(8)
    expect(resolveUserFormOrgId({})).toBe(null)
  })
})

describe('quickCreate selection backfill', () => {
  it('多选 merge 去重', () => {
    expect(mergeQuickCreateSelection('1,2', 3, true)).toBe('1,2,3')
    expect(mergeQuickCreateSelection('1,2', 2, true)).toBe('1,2')
  })
})

describe('quickCreate registry', () => {
  it('解析 systemRole 预设', () => {
    const def = resolveQuickCreateDefinition({ quickCreate: { preset: 'systemRole' } })
    expect(def?.permission).toBe('system:role:add')
    expect(def?.linkLabel).toBe('新增角色')
    const schema = def?.buildSchema?.({ userStore: { isAdmin: true } }) || []
    expect(schema.some(item => item.field === 'isSystem')).toBe(true)
    expect(schema.some(item => item.field === 'roleName')).toBe(true)
    expect(def?.formLayout?.labelPlacement).toBe('left')
  })

  it('岗位快捷 schema 岗位类型含字典选项', () => {
    const def = resolveQuickCreateDefinition({ quickCreate: { preset: 'systemPost' } })
    const schema = def?.buildSchema?.({
      dict: {
        sys_post_type: [{ label: '管理', value: '1' }, { label: '业务', value: '2' }],
        sys_normal_disable: [{ label: '正常', value: '1' }],
      },
    }) || []
    const postType = schema.find(item => item.field === 'postType')
    expect(postType?.type).toBe('radio')
    expect(postType?.props?.options?.length).toBeGreaterThan(0)
    expect(def?.editSchemaDictTypes).toContain('sys_post_type')
  })

  it('角色快捷 schema 与页面工厂字段一致', () => {
    const roleKeyAutoCode = createRoleKeyAutoCode()
    const schema = buildSystemRoleEditSchema({
      roleKeyAutoCode,
      roleTypeOptions: [{ label: '普通', value: 1 }],
      manageableDataScopeOptions: [{ label: '本部门', value: 2 }],
      roleStatusOptions: [{ label: '正常', value: 1 }],
      yesNoOptions: [{ label: '否', value: 0 }],
    })
    const fields = schema.filter(item => item.field).map(item => item.field)
    expect(fields).toContain('dataScope')
    expect(fields).toContain('isSystem')
    expect(schema.find(item => item.field === 'roleStatus')?.type).toBe('radio')
  })

  it('角色正式页 withFieldQuickCreate 仅白名单字典字段有入口', () => {
    const roleKeyAutoCode = createRoleKeyAutoCode()
    const schema = buildSystemRoleEditSchema({
      roleKeyAutoCode,
      roleTypeOptions: [{ label: '普通', value: 1 }],
      manageableDataScopeOptions: [{ label: '本部门', value: 2 }],
      roleStatusOptions: [{ label: '正常', value: 1 }],
      yesNoOptions: [{ label: '否', value: 0 }],
      withFieldQuickCreate: true,
    })
    expect(schema.find(item => item.field === 'roleType')?.quickCreate).toEqual({
      preset: 'dictData',
      dictType: 'sys_role_type',
      linkLabel: '新增类型',
    })
    expect(schema.find(item => item.field === 'dataScope')?.quickCreate).toBeUndefined()
    expect(schema.find(item => item.field === 'roleStatus')?.quickCreate).toBeUndefined()
    expect(schema.find(item => item.field === 'isSystem')?.quickCreate).toBeUndefined()
    expect(resolveQuickCreateDefinition(schema.find(item => item.field === 'roleType'))?.permission)
      .toEqual(['system:dict:add', 'system:dict:edit', 'system:dict:remove'])
  })

  it('解析 dictData 预设并继承 dictType', () => {
    const def = resolveQuickCreateDefinition({
      dictType: 'crm_customer_level',
      quickCreate: { preset: 'dictData' },
    })
    expect(def?.preset).toBe('dictData')
    expect(def?.dictType).toBe('crm_customer_level')
    expect(def?.valueField).toBe('dictValue')
    expect(def?.manageMode).toBe(true)
    expect(def?.permission).toEqual(['system:dict:add', 'system:dict:edit', 'system:dict:remove'])
    expect(def?.linkLabel).toBe('管理字典项')
  })

  it('平台字典未入白名单时 resolve 为 null', () => {
    expect(resolveQuickCreateDefinition({
      quickCreate: { preset: 'dictData', dictType: 'sys_yes_no' },
    })).toBeNull()
    expect(resolveQuickCreateDefinition({
      quickCreate: { preset: 'dictData', dictType: 'sys_role_data_scope' },
    })).toBeNull()
  })

  it('平台 sys_ 字典标记与快捷新增白名单', () => {
    expect(isPlatformDictType('sys_user_type')).toBe(true)
    expect(isPlatformDictType('crm_customer_level')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_user_type')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_user_status')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_org_type')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_post_type')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_notice_type')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_role_type')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_req_method')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_user_sex')).toBe(false)
    expect(isPlatformDictQuickCreateBlocked('sys_enable_disable')).toBe(true)
    expect(isPlatformDictQuickCreateBlocked('sys_yes_no')).toBe(true)
    expect(isPlatformDictQuickCreateBlocked('sys_normal_disable')).toBe(true)
  })

  it('解析 systemOrg / systemClient 预设', () => {
    const org = resolveQuickCreateDefinition({ quickCreate: { preset: 'systemOrg' } })
    expect(org?.permission).toBe('system:org:add')
    expect(org?.linkLabel).toBe('新增组织')
    expect(org?.editSchemaDictTypes).toContain('sys_org_type')
    const orgSchema = org?.buildSchema?.({ dict: { sys_org_type: [{ label: '部门', value: '2' }], sys_normal_disable: [{ label: '正常', value: '1' }] } }) || []
    expect(orgSchema.some(item => item.field === 'orgName')).toBe(true)
    expect(orgSchema.find(item => item.field === 'orgType')?.quickCreate).toBeUndefined()

    const client = resolveQuickCreateDefinition({ quickCreate: { preset: 'systemClient' } })
    expect(client?.permission).toBe('system:client:add')
    expect(client?.valueField).toBe('clientCode')
    const gate = client?.beforeOpen?.({ userStore: { isAdmin: false } })
    expect(gate?.ok).toBe(false)
  })
})
