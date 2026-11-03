/**
 * 组织新增/编辑表单 schema 单源：改此处即同步正式新增页与快捷新增。
 */
import { useDictStore } from '@/stores/system/dictStore'
import { buildSystemEditCards, SYSTEM_EDIT_FULL_SPAN } from './systemEditLayout'
import { toNumberOptions } from './systemRoleEditSchema'

export const ORG_TYPE_DICT = 'sys_org_type'
export const ORG_NORMAL_DISABLE_DICT = 'sys_normal_disable'
export const SYSTEM_ORG_EDIT_DICT_TYPES = [ORG_TYPE_DICT, ORG_NORMAL_DISABLE_DICT]

export function getOrgDictOptionsFromCache(dictType) {
  try {
    return useDictStore().dictCache.get(dictType) || []
  }
  catch {
    return []
  }
}

export function getOrgDictNumberOptionsFromCache(dictType) {
  return toNumberOptions(getOrgDictOptionsFromCache(dictType))
}

/**
 * @param {object} options
 * @param {boolean} [options.withFieldQuickCreate=false] 正式页可开字段级快捷新增；QC 弹窗内必须 false 防套娃
 */
export function buildSystemOrgEditSchema({
  orgTypeOptions,
  orgStatusOptions,
  defaultParentId = 0,
  parentOptions,
  regionOptions,
  withFieldQuickCreate = false,
} = {}) {
  const parentField = parentOptions
    ? {
        field: 'parentId',
        label: '上级组织',
        type: 'treeSelect',
        defaultValue: defaultParentId,
        props: {
          placeholder: '请选择上级组织',
          clearable: true,
          filterable: true,
          defaultExpandAll: true,
        },
        options: parentOptions,
      }
    : {
        field: 'parentId',
        label: '上级组织',
        type: 'treeSelect',
        defaultValue: defaultParentId,
        optionSource: {
          type: 'tree',
          api: 'get@/system/org/tree',
          // eslint-disable-next-line no-template-curly-in-string
          params: { tenantId: '${tenantId}' },
          valueField: 'id',
          keyField: 'id',
          labelField: 'orgName',
          childrenField: 'children',
        },
        props: {
          placeholder: '请选择上级组织（可空为顶级）',
          clearable: true,
          filterable: true,
          defaultExpandAll: true,
        },
      }

  const basicFields = [
    parentField,
    {
      field: 'orgName',
      label: '组织名称',
      type: 'input',
      rules: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
      props: { placeholder: '请输入组织名称' },
    },
    {
      field: 'orgType',
      label: '组织类型',
      type: 'select',
      defaultValue: orgTypeOptions?.[0]?.value || '2',
      rules: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
      ...(withFieldQuickCreate
        ? {
            quickCreate: {
              preset: 'dictData',
              dictType: ORG_TYPE_DICT,
              linkLabel: '新增类型',
            },
          }
        : {}),
      props: {
        placeholder: '请选择组织类型',
        options: orgTypeOptions || [],
      },
    },
    {
      field: 'sort',
      label: '排序',
      type: 'number',
      defaultValue: 0,
      props: { placeholder: '排序值', min: 0 },
    },
  ]

  const leaderFields = [
    {
      field: 'leaderId',
      label: '负责人',
      type: 'userSelect',
      labelField: 'leaderName',
      clearable: true,
      props: {
        placeholder: '请从用户列表选择负责人',
        title: '选择组织负责人',
      },
    },
    {
      field: 'phone',
      label: '联系电话',
      type: 'input',
      rules: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
      ],
      props: { placeholder: '请输入联系电话' },
    },
    {
      field: 'address',
      label: '地址',
      type: 'input',
      span: SYSTEM_EDIT_FULL_SPAN,
      props: { placeholder: '请输入组织地址' },
    },
  ]

  if (typeof regionOptions === 'function' || Array.isArray(regionOptions)) {
    leaderFields.push({
      field: 'regionCode',
      label: '行政区划',
      type: 'treeSelect',
      props: {
        placeholder: '请选择行政区划',
        clearable: true,
        filterable: true,
      },
      options: typeof regionOptions === 'function' ? regionOptions : () => regionOptions,
    })
  }

  return buildSystemEditCards([
    {
      label: '基础信息',
      icon: 'forge:module',
      fields: basicFields,
    },
    {
      label: '负责人信息',
      icon: 'forge:collaboration',
      fields: leaderFields,
    },
    {
      label: '状态配置',
      icon: 'forge:actions',
      fields: [
        {
          field: 'orgStatus',
          label: '组织状态',
          type: 'radio',
          defaultValue: 1,
          props: { options: orgStatusOptions || [] },
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

export function resolveOrgEditSchemaOptions(dictValue) {
  const cached = {
    orgTypeOptions: getOrgDictOptionsFromCache(ORG_TYPE_DICT),
    orgStatusOptions: getOrgDictNumberOptionsFromCache(ORG_NORMAL_DISABLE_DICT),
  }
  if (!dictValue)
    return cached

  const orgTypeOptions = dictValue[ORG_TYPE_DICT] || []
  const orgStatusOptions = toNumberOptions(dictValue[ORG_NORMAL_DISABLE_DICT] || [])
  return {
    orgTypeOptions: orgTypeOptions.length ? orgTypeOptions : cached.orgTypeOptions,
    orgStatusOptions: orgStatusOptions.length ? orgStatusOptions : cached.orgStatusOptions,
  }
}
