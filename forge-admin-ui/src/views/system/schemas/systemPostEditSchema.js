/**
 * 岗位新增/编辑表单 schema 单源：改此处即同步正式新增页与快捷新增。
 */
import { useDictStore } from '@/stores/system/dictStore'
import { createAutoCodeSync } from '@/utils'
import { buildSystemEditCards } from './systemEditLayout'
import { toNumberOptions } from './systemRoleEditSchema'

export const POST_TYPE_DICT = 'sys_post_type'
export const POST_NORMAL_DISABLE_DICT = 'sys_normal_disable'
export const SYSTEM_POST_EDIT_DICT_TYPES = [POST_TYPE_DICT, POST_NORMAL_DISABLE_DICT]

export function getPostDictNumberOptionsFromCache(dictType) {
  try {
    const cached = useDictStore().dictCache.get(dictType) || []
    return toNumberOptions(cached)
  }
  catch {
    return []
  }
}

export function createPostCodeAutoCode() {
  return createAutoCodeSync({
    scene: 'postCode',
    targetField: 'postCode',
  })
}

export function buildSystemPostEditSchema({
  postCodeAutoCode,
  postTypeOptions,
  postStatusOptions,
  withFieldQuickCreate = false,
}) {
  return buildSystemEditCards([
    {
      label: '基础信息',
      icon: 'forge:module',
      fields: [
        {
          field: 'postName',
          label: '岗位名称',
          type: 'input',
          rules: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
          props: { placeholder: '请输入岗位名称' },
          onChange: postCodeAutoCode.onSourceChange,
        },
        {
          field: 'postCode',
          label: '岗位编码',
          type: 'input',
          rules: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
          labelTip: '根据岗位名称自动生成，可手动修改',
          props: { placeholder: '自动生成，如：yan_fa_gong_cheng_shi' },
          onChange: postCodeAutoCode.onTargetChange,
        },
        {
          field: 'orgId',
          label: '所属组织',
          type: 'treeSelect',
          required: true,
          rules: [{ required: true, type: 'number', message: '请选择所属组织', trigger: 'change' }],
          ...(withFieldQuickCreate
            ? {
                quickCreate: {
                  preset: 'systemOrg',
                  defaultsFromForm: { parentId: 0 },
                },
              }
            : {}),
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
            placeholder: '请选择所属组织',
            clearable: true,
            filterable: true,
            defaultExpandAll: true,
          },
        },
        {
          field: 'postType',
          label: '岗位类型',
          type: 'radio',
          defaultValue: 2,
          rules: [{ required: true, type: 'number', message: '请选择岗位类型', trigger: 'change' }],
          ...(withFieldQuickCreate
            ? {
                quickCreate: {
                  preset: 'dictData',
                  dictType: POST_TYPE_DICT,
                  linkLabel: '新增类型',
                },
              }
            : {}),
          props: { options: postTypeOptions },
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
          field: 'postStatus',
          label: '岗位状态',
          type: 'radio',
          defaultValue: 1,
          props: { options: postStatusOptions },
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

export function resolvePostEditSchemaOptions(dictValue) {
  const cached = {
    postTypeOptions: getPostDictNumberOptionsFromCache(POST_TYPE_DICT),
    postStatusOptions: getPostDictNumberOptionsFromCache(POST_NORMAL_DISABLE_DICT),
  }
  if (!dictValue)
    return cached

  const postTypeOptions = toNumberOptions(dictValue[POST_TYPE_DICT] || [])
  const postStatusOptions = toNumberOptions(dictValue[POST_NORMAL_DISABLE_DICT] || [])
  return {
    postTypeOptions: postTypeOptions.length ? postTypeOptions : cached.postTypeOptions,
    postStatusOptions: postStatusOptions.length ? postStatusOptions : cached.postStatusOptions,
  }
}
