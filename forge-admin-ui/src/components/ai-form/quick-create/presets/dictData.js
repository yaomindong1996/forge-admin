import { clearDictCache } from '@/composables/useDict'
import { resolveCreatedDictDataRecord } from '../resolveCreatedRecord'
import { submitQuickCreateApi } from '../submitApi'

/** 平台内置字典（sys_ 前缀）默认不做表单内快捷新增 */
export function isPlatformDictType(dictType = '') {
  const normalized = String(dictType || '').trim()
  if (!normalized)
    return true
  return normalized.startsWith('sys_')
}

/**
 * 允许「新增字典数据项」的平台字典（sys_ 前缀）。
 * 仅收录可配置扩展的类型/枚举类字段；启停、是否、数据范围等框架态禁止。
 */
export const PLATFORM_DICT_DATA_QUICK_CREATE_ALLOWLIST = new Set([
  'sys_user_type',
  'sys_user_status',
  'sys_user_sex',
  'sys_org_type',
  'sys_post_type',
  'sys_role_type',
  'sys_notice_type',
  'sys_req_method',
  'sys_link_open_target',
  'sys_resource_type',
  'sys_client_auth_method',
  'sys_client_login_auth_type',
  'sys_client_captcha_type',
  'sys_file_storage_type',
  'sys_file_group_type',
  'sys_code_rule_category',
  'sys_code_rule_scene',
  'sys_excel_config_type',
  'sys_message_type',
  'sys_message_channel',
  'sys_message_send_scope',
  'sys_crypto_algorithm',
  'sys_same_account_login_strategy',
  'sys_print_scene',
  'sys_print_source_type',
  'sys_plugin_origin',
  'sys_operation_type',
  'sys_login_type',
  'sys_client_type',
  'sys_job_trigger_type',
  'sys_job_api_scope',
  'sys_flow_overdue_repeat_mode',
])

export function isPlatformDictQuickCreateBlocked(dictType = '', config = {}) {
  if (config?.allowPlatformDict === true)
    return false
  if (!isPlatformDictType(dictType))
    return false
  return !PLATFORM_DICT_DATA_QUICK_CREATE_ALLOWLIST.has(String(dictType).trim())
}

export function resolveDictTypeFromContext(ctx = {}) {
  const { config, field } = ctx
  return config?.dictType
    || field?.quickCreate?.dictType
    || field?.dictType
    || field?.props?.dictType
    || ''
}

export function createDictDataQuickCreatePreset() {
  return {
    key: 'dictData',
    permission: 'system:dict:add',
    linkLabel: '新增字典项',
    modalTitle: '新增字典项',
    submitApi: 'post@/system/dict/data/add',
    valueField: 'dictValue',
    labelField: 'dictLabel',
    formLayout: {
      gridCols: 1,
      labelPlacement: 'left',
      labelWidth: 120,
      labelAlign: 'right',
      modalWidth: '560px',
    },
    buildSchema(ctx = {}) {
      const dictType = resolveDictTypeFromContext(ctx)
      return [
        {
          field: 'dictType',
          label: '字典类型',
          type: 'input',
          rules: [{ required: true, message: '字典类型不能为空', trigger: 'blur' }],
          defaultValue: dictType,
          props: { disabled: true, placeholder: dictType },
        },
        {
          field: 'dictLabel',
          label: '字典标签',
          type: 'input',
          rules: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
          props: { placeholder: '请输入字典标签' },
        },
        {
          field: 'dictValue',
          label: '字典键值',
          type: 'input',
          rules: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
          props: { placeholder: '请输入字典键值' },
        },
        {
          field: 'dictSort',
          label: '排序',
          type: 'number',
          defaultValue: 0,
          props: { placeholder: '排序值', min: 0 },
        },
        {
          field: 'dictStatus',
          label: '状态',
          type: 'radio',
          defaultValue: 1,
          rules: [{ required: true, type: 'number', message: '请选择状态', trigger: 'change' }],
          props: {
            options: [
              { label: '正常', value: 1 },
              { label: '禁用', value: 0 },
            ],
          },
        },
        {
          field: 'remark',
          label: '备注',
          type: 'textarea',
          props: { placeholder: '请输入备注', rows: 2 },
        },
      ]
    },
    buildInitialValues(ctx = {}) {
      const dictType = resolveDictTypeFromContext(ctx)
      return {
        dictType,
        dictSort: 0,
        dictStatus: 1,
        isDefault: 'N',
        listClass: 'default',
        parentDictCode: 0,
      }
    },
    beforeOpen(ctx = {}) {
      const dictType = resolveDictTypeFromContext(ctx)
      if (!dictType)
        return { ok: false, message: '未配置字典类型，无法快捷新增' }
      if (isPlatformDictQuickCreateBlocked(dictType, ctx.config))
        return { ok: false, message: '该平台字典不支持表单内快捷新增' }
      return { ok: true }
    },
    async submit(payload, ctx = {}) {
      const dictType = resolveDictTypeFromContext(ctx)
      const body = {
        dictType,
        dictSort: 0,
        dictStatus: 1,
        isDefault: 'N',
        listClass: 'default',
        parentDictCode: 0,
        ...payload,
      }
      await submitQuickCreateApi('post@/system/dict/data/add', body)
      clearDictCache(dictType)
      const record = await resolveCreatedDictDataRecord(body)
      return { submitted: body, record }
    },
  }
}
