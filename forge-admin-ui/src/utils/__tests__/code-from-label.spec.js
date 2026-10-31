import { describe, expect, it } from 'vitest'
import { createAutoCodeSync, generateCodeFromLabel } from '../code-from-label'

describe('generateCodeFromLabel', () => {
  it('角色权限字符使用全拼下划线', () => {
    expect(generateCodeFromLabel('系统管理员', 'roleKey')).toBe('xi_tong_guan_li_yuan')
    expect(generateCodeFromLabel('Admin', 'roleKey')).toBe('admin')
  })

  it('权限标识优先英文语义，并结合父级前缀', () => {
    expect(generateCodeFromLabel('用户新增', 'perms')).toBe('system:user:add')
    expect(generateCodeFromLabel('新增', 'perms', { parentPerms: 'system:user:list' })).toBe('system:user:add')
    expect(generateCodeFromLabel('导出', 'perms', { parentPerms: 'system:role:list' })).toBe('system:role:export')
  })

  it('岗位 / 字典 / 配置 / 分类按场景格式化', () => {
    expect(generateCodeFromLabel('研发工程师', 'postCode')).toMatch(/yan_fa/)
    expect(generateCodeFromLabel('请假审批', 'categoryCode')).toMatch(/qing_jia/)
    expect(generateCodeFromLabel('用户性别', 'dictType')).toBe('sys_user_sex')
    expect(generateCodeFromLabel('登录超时', 'configKey')).toBe('sys.login.timeout')
  })
})

describe('createAutoCodeSync', () => {
  it('名称变化时自动回填，用户改过编码后不再覆盖', () => {
    const sync = createAutoCodeSync({ scene: 'roleKey', targetField: 'roleKey' })
    const formData = {}
    sync.onSourceChange({ value: '测试角色', formData })
    expect(formData.roleKey).toBe('ce_shi_jue_se')
    const auto = formData.roleKey
    sync.onSourceChange({ value: '新角色', formData })
    expect(formData.roleKey).toBe('xin_jue_se')
    formData.roleKey = 'custom_key'
    sync.onTargetChange({ value: 'custom_key' })
    sync.onSourceChange({ value: '又一个角色', formData })
    expect(formData.roleKey).toBe('custom_key')
    expect(auto).toBe('ce_shi_jue_se')
  })

  it('编辑态已有编码时不覆盖', () => {
    const sync = createAutoCodeSync({ scene: 'roleKey', targetField: 'roleKey' })
    const formData = { id: 1, roleKey: 'admin' }
    sync.onSourceChange({ value: '系统管理员', formData })
    expect(formData.roleKey).toBe('admin')
  })
})
