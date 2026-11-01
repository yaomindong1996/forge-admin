/**
 * 客户端新增/编辑表单 schema 单源：改此处即同步正式新增页与快捷新增。
 */
import { useDictStore } from '@/stores/system/dictStore'
import { createAutoCodeSync } from '@/utils'
import { toBooleanDictOptions, toNumberDictOptions } from '@/utils/dict-options'
import { buildSystemEditCards } from './systemEditLayout'

export const CLIENT_AUTH_METHOD_DICT = 'sys_client_auth_method'
export const CLIENT_ENABLE_DISABLE_DICT = 'sys_enable_disable'
export const CLIENT_YES_NO_DICT = 'sys_yes_no'
export const CLIENT_LOGIN_AUTH_TYPE_DICT = 'sys_client_login_auth_type'
export const CLIENT_CAPTCHA_TYPE_DICT = 'sys_client_captcha_type'

export const SYSTEM_CLIENT_EDIT_DICT_TYPES = [
  CLIENT_AUTH_METHOD_DICT,
  CLIENT_ENABLE_DISABLE_DICT,
  CLIENT_YES_NO_DICT,
  CLIENT_LOGIN_AUTH_TYPE_DICT,
  CLIENT_CAPTCHA_TYPE_DICT,
]

function getCached(dictType) {
  try {
    return useDictStore().dictCache.get(dictType) || []
  }
  catch {
    return []
  }
}

export function createClientCodeAutoCode() {
  return createAutoCodeSync({
    scene: 'clientCode',
    targetField: 'clientCode',
  })
}

export function resolveClientEditSchemaOptions(dictValue) {
  const pick = (type, transform = list => list) => {
    const fromDict = dictValue?.[type]
    const list = Array.isArray(fromDict) && fromDict.length ? fromDict : getCached(type)
    return transform(list)
  }
  return {
    clientAuthMethodOptions: pick(CLIENT_AUTH_METHOD_DICT),
    statusOptions: pick(CLIENT_ENABLE_DISABLE_DICT, toNumberDictOptions),
    booleanOptions: pick(CLIENT_YES_NO_DICT, toBooleanDictOptions),
    authTypeOptions: pick(CLIENT_LOGIN_AUTH_TYPE_DICT),
    captchaTypeOptions: pick(CLIENT_CAPTCHA_TYPE_DICT),
  }
}

/**
 * @param {object} options
 * @param {ReturnType<typeof createClientCodeAutoCode>} options.clientCodeAuto
 * @param {boolean} [options.withFieldQuickCreate=false]
 */
export function buildSystemClientEditSchema({
  clientCodeAuto,
  clientAuthMethodOptions = [],
  statusOptions = [],
  booleanOptions = [],
  authTypeOptions = [],
  captchaTypeOptions = [],
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
          field: 'clientName',
          label: '客户端名称',
          type: 'input',
          rules: [{ required: true, message: '请输入客户端名称', trigger: 'blur' }],
          props: { placeholder: '请输入客户端名称' },
          onChange: clientCodeAuto?.onSourceChange,
        },
        {
          field: 'clientCode',
          label: '客户端编码',
          type: 'input',
          rules: [{ required: true, message: '请输入客户端编码', trigger: 'blur' }],
          disabled: ({ formData, context }) => Boolean((context?.isEdit || formData?.id) && formData?.builtIn),
          labelTip: '根据名称自动生成，可手动修改',
          props: { placeholder: '自动生成，如：pc、app、h5' },
          onChange: clientCodeAuto?.onTargetChange,
        },
        {
          field: 'appId',
          label: 'AppId',
          type: 'input',
          rules: [{ required: true, message: '请输入AppId', trigger: 'blur' }],
          props: { placeholder: '应用ID' },
        },
        {
          field: 'clientAuthMethod',
          label: '客户端认证方式',
          type: 'radio',
          defaultValue: 'client_secret',
          rules: [{ required: true, message: '请选择客户端认证方式', trigger: 'change' }],
          ...dictQc(CLIENT_AUTH_METHOD_DICT, '新增认证方式'),
          props: { options: clientAuthMethodOptions },
        },
        {
          field: 'appSecret',
          label: 'AppSecret',
          type: 'input',
          props: {
            type: 'password',
            placeholder: '新增时必填；编辑留空表示不轮换',
          },
          vIf: formData => formData.clientAuthMethod === 'client_secret',
        },
      ],
    },
    {
      label: 'Token配置',
      icon: 'forge:connection',
      fields: [
        {
          field: 'tokenTimeout',
          label: 'Token有效期(秒)',
          type: 'number',
          defaultValue: 7200,
          rules: [{ required: true, message: '请输入Token有效期' }],
          props: {
            placeholder: '如：86400(1天), 2592000(30天)',
            min: 60,
            step: 3600,
          },
        },
        {
          field: 'tokenActivityTimeout',
          label: 'Token活跃超时(秒)',
          type: 'number',
          defaultValue: -1,
          props: { placeholder: '-1表示不限制' },
        },
        {
          field: 'concurrentLogin',
          label: '允许并发登录',
          type: 'radio',
          defaultValue: false,
          props: { options: booleanOptions },
        },
        {
          field: 'shareToken',
          label: '共享Token',
          type: 'radio',
          defaultValue: false,
          props: { options: booleanOptions },
        },
        {
          field: 'authTypes',
          label: '支持的认证方式',
          type: 'select',
          ...dictQc(CLIENT_LOGIN_AUTH_TYPE_DICT, '新增认证类型'),
          props: {
            placeholder: '请选择支持的认证方式',
            options: authTypeOptions,
            multiple: true,
          },
        },
        {
          field: 'captchaType',
          label: '验证码覆盖',
          type: 'select',
          defaultValue: '',
          ...dictQc(CLIENT_CAPTCHA_TYPE_DICT, '新增验证码类型'),
          props: {
            placeholder: '为空时继承全局登录配置',
            options: captchaTypeOptions,
          },
        },
      ],
    },
    {
      label: '状态配置',
      icon: 'forge:actions',
      fields: [
        {
          field: 'status',
          label: '状态',
          type: 'radio',
          defaultValue: 1,
          disabled: ({ formData, context }) => Boolean((context?.isEdit || formData?.id) && formData?.builtIn),
          props: { options: statusOptions },
        },
        {
          field: 'description',
          label: '描述',
          type: 'textarea',
          props: { placeholder: '请输入客户端描述', rows: 3 },
        },
      ],
    },
  ])
}
