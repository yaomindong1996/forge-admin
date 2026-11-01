/**
 * 角色新增/编辑表单 schema 单源：改此处即同步正式新增页与快捷新增。
 */
import { useDictStore } from '@/stores/system/dictStore'
import { createAutoCodeSync } from '@/utils'
import { buildSystemEditCards } from './systemEditLayout'

export const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'
export const ROLE_TYPE_DICT = 'sys_role_type'
export const ROLE_NORMAL_DISABLE_DICT = 'sys_normal_disable'
export const ROLE_YES_NO_DICT = 'sys_yes_no'
export const SYSTEM_ROLE_EDIT_DICT_TYPES = [
  ROLE_TYPE_DICT,
  ROLE_DATA_SCOPE_DICT,
  ROLE_NORMAL_DISABLE_DICT,
  ROLE_YES_NO_DICT,
]

export function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

export function getDictNumberOptionsFromCache(dictType) {
  try {
    const cached = useDictStore().dictCache.get(dictType) || []
    return toNumberOptions(cached)
  }
  catch {
    return []
  }
}

export function resolveManageableDataScopeOptions(dataScopeOptions = [], userStore) {
  if (userStore?.isAdmin)
    return dataScopeOptions
  const deniedScopes = Number(userStore?.userType) === 2 ? [1, 2] : [1]
  return dataScopeOptions.filter(item => !deniedScopes.includes(Number(item.value)))
}

export function createRoleKeyAutoCode() {
  return createAutoCodeSync({
    scene: 'roleKey',
    targetField: 'roleKey',
  })
}

/**
 * @param {object} options
 * @param {boolean} [options.withFieldQuickCreate=false] 正式页可开字段级快捷新增；QC 弹窗内必须 false
 */
export function buildSystemRoleEditSchema({
  roleKeyAutoCode,
  roleTypeOptions,
  manageableDataScopeOptions,
  roleStatusOptions,
  yesNoOptions,
  withFieldQuickCreate = false,
} = {}) {
  const dictQc = (dictType, linkLabel) => (withFieldQuickCreate
    ? { quickCreate: { preset: 'dictData', dictType, linkLabel } }
    : {})

  return buildSystemEditCards([
    {
      label: '基础信息',
      icon: 'forge:module',
      fields: [
        {
          field: 'roleName',
          label: '角色名称',
          type: 'input',
          rules: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
          props: { placeholder: '请输入角色名称' },
          onChange: roleKeyAutoCode.onSourceChange,
        },
        {
          field: 'roleKey',
          label: '权限字符',
          type: 'input',
          rules: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
          labelTip: '根据角色名称自动生成全拼，可手动修改',
          props: { placeholder: '自动生成，如：xi_tong_guan_li_yuan' },
          onChange: roleKeyAutoCode.onTargetChange,
        },
        {
          field: 'roleType',
          label: '角色类型',
          type: 'select',
          defaultValue: 1,
          rules: [{ required: true, type: 'number', message: '请选择角色类型', trigger: 'change' }],
          ...dictQc(ROLE_TYPE_DICT, '新增类型'),
          props: {
            placeholder: '请选择角色类型',
            options: roleTypeOptions,
          },
        },
        {
          field: 'dataScope',
          label: '数据范围',
          type: 'select',
          defaultValue: 2,
          rules: [{ required: true, type: 'number', message: '请选择数据范围', trigger: 'change' }],
          props: {
            placeholder: '请选择数据范围',
            options: manageableDataScopeOptions,
          },
        },
        {
          field: 'sort',
          label: '排序',
          type: 'number',
          defaultValue: 0,
          props: { placeholder: '排序值', min: 0 },
        },
      ],
    },
    {
      label: '状态配置',
      icon: 'forge:actions',
      fields: [
        {
          field: 'roleStatus',
          label: '角色状态',
          type: 'radio',
          defaultValue: 1,
          props: { options: roleStatusOptions },
        },
        {
          field: 'isSystem',
          label: '系统角色',
          type: 'radio',
          defaultValue: 0,
          props: { options: yesNoOptions },
        },
        {
          field: 'remark',
          label: '备注',
          type: 'textarea',
          props: { placeholder: '请输入备注', rows: 3 },
        },
      ],
    },
  ])
}

export function resolveRoleEditSchemaOptions(userStore, dictValue) {
  const cached = {
    roleTypeOptions: getDictNumberOptionsFromCache(ROLE_TYPE_DICT),
    dataScopeOptions: getDictNumberOptionsFromCache(ROLE_DATA_SCOPE_DICT),
    roleStatusOptions: getDictNumberOptionsFromCache(ROLE_NORMAL_DISABLE_DICT),
    yesNoOptions: getDictNumberOptionsFromCache(ROLE_YES_NO_DICT),
  }
  const buildResult = (roleTypeOptions, dataScopeOptions, roleStatusOptions, yesNoOptions) => ({
    roleTypeOptions,
    manageableDataScopeOptions: resolveManageableDataScopeOptions(dataScopeOptions, userStore),
    roleStatusOptions,
    yesNoOptions,
  })

  if (!dictValue)
    return buildResult(cached.roleTypeOptions, cached.dataScopeOptions, cached.roleStatusOptions, cached.yesNoOptions)

  const roleTypeOptions = toNumberOptions(dictValue[ROLE_TYPE_DICT] || [])
  const dataScopeOptions = toNumberOptions(dictValue[ROLE_DATA_SCOPE_DICT] || [])
  const roleStatusOptions = toNumberOptions(dictValue[ROLE_NORMAL_DISABLE_DICT] || [])
  const yesNoOptions = toNumberOptions(dictValue[ROLE_YES_NO_DICT] || [])

  return buildResult(
    roleTypeOptions.length ? roleTypeOptions : cached.roleTypeOptions,
    dataScopeOptions.length ? dataScopeOptions : cached.dataScopeOptions,
    roleStatusOptions.length ? roleStatusOptions : cached.roleStatusOptions,
    yesNoOptions.length ? yesNoOptions : cached.yesNoOptions,
  )
}
