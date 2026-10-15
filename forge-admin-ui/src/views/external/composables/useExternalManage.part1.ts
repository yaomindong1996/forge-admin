/** manage.vue setup part 1. */
import { NButton, NSpace, NTag, NTooltip } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { clearExternalApiLogs, debugExternalApi, getExternalApiById, getExternalApiLogSummary, updateExternalApiDocument } from '@/api/external/api'
import { getExternalSystemList } from '@/api/external/system'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import FileUpload from '@/components/file-upload/index.vue'
import { useDict } from '@/composables/useDict'
import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store'
import { generateUUID, getFileDownloadUrl } from '@/utils'
import { toBooleanDictOptions, toNumberDictOptions } from '@/utils/dict-options'
import ApiConfigSection from '../components/ApiConfigSection.vue'
import ExternalApiFormSlot from '../components/ExternalApiFormSlot.vue'
import ExternalConfigEditor from '../components/ExternalConfigEditor.vue'
import PathFieldPicker from '../components/PathFieldPicker.vue'
export function applyExternalManagePart1(props, emit) {
  const __impl = {}
  const mut = {}

  // 拆分 SFC 时遗漏的 setup 状态初始化（缺这些会导致页面空白 / message is not defined）
  const message = window.$message
  const authStore = useAuthStore()
  const systemCrudRef = ref()
  const apiCrudRef = ref()
  const logCrudRef = ref()
  const selectedSystem = ref(null)
  const systemOptions = ref([])
  const systemBaseUrlMap = ref({})
  const systemCount = ref(0)
  const apiViewMode = ref(false)
  const logDrawerVisible = ref(false)
  const logApi = ref(null)
  const logSummary = ref({})
  const debugModalVisible = ref(false)
  const debugApi = ref(null)
  const debugParamsText = ref('{}')
  const debugFieldSchema = ref([])
  const debugFieldValues = ref({})
  const debugResult = ref(null)
  const debugLoading = ref(false)
  const usageModalVisible = ref(false)
  const usageApi = ref(null)
  const usageCode = ref('')
  const docModalVisible = ref(false)
  const docApi = ref(null)
  const docFiles = ref([])
  const docSaving = ref(false)
  const EXTERNAL_AUTH_ADAPTER_DICT = 'external_auth_adapter'
  const { dict, getLabel } = useDict(
    EXTERNAL_AUTH_ADAPTER_DICT,
    'external_auth_type',
    'external_api_key_position',
    'external_request_content_type',
    'external_call_status',
    'external_call_type',
    'external_api_execution_mode',
    'sys_enable_disable',
    'sys_req_method',
    'sys_yes_no',
  )

  const apiFormSlotNames = ['apiPresetGuide', 'urlPreview', 'requestHeaders', 'requestParams', 'requestBodyTemplate', 'mockResponseJson', 'paramMappings', 'successCodes', 'inputSchemaJson', 'outputSchemaJson']
  const oauth2GrantTypeOptions = [
    { label: '客户端凭证', value: 'client_credentials' },
    { label: '密码模式', value: 'password' },
    { label: '授权码', value: 'authorization_code' },
    { label: '刷新令牌', value: 'refresh_token' },
  ]
  const tokenHeaderOptions = [
    { label: 'Authorization', value: 'Authorization' },
    { label: 'X-Access-Token', value: 'X-Access-Token' },
    { label: 'X-Auth-Token', value: 'X-Auth-Token' },
  ]
  const tokenPrefixOptions = [
    { label: 'Bearer', value: 'Bearer' },
    { label: 'Token', value: 'Token' },
  ]

  const isApiStandalone = computed(() => props.initialView === 'api')
  const logDrawerWidth = 'min(1120px, 92vw)'
  const apiPageTitle = computed(() => {
    if (isApiStandalone.value || !selectedSystem.value)
      return '外部接口'
    return `接口管理 · ${selectedSystem.value.systemName || selectedSystem.value.systemCode || ''}`
  })
  const panelSubtitle = computed(() => {
    if (!isApiStandalone.value && !apiViewMode.value)
      return '维护外部系统连接与认证配置'
    return selectedSystem.value ? `${selectedSystem.value.systemName || ''} 下的接口配置` : '维护所有外部接口配置'
  })
  const logDrawerTitle = computed(() => logApi.value ? `调用日志 - ${logApi.value.apiName || logApi.value.apiCode}` : '外部接口调用日志')

  const systemApiConfig = {
    list: 'get@/external/system/page',
    detail: 'get@/external/system/:id',
    add: 'post@/external/system',
    update: 'put@/external/system',
    delete: 'delete@/external/system/:id',
  }

  const apiApiConfig = {
    list: 'get@/external/api/page',
    detail: 'get@/external/api/:id',
    add: 'post@/external/api',
    update: 'put@/external/api',
    delete: 'delete@/external/api/:id',
  }

  const logApiConfig = {
    list: 'get@/external/api/log/page',
    detail: 'get@/external/api/log/:id',
    delete: 'delete@/external/api/log/:id',
  }

  const authTypeOptions = computed(() => dict.value.external_auth_type || [])
  const apiKeyPositionOptions = computed(() => dict.value.external_api_key_position || [])
  const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))
  const methodOptions = computed(() => (dict.value.sys_req_method || []).filter(item => item.value !== 'ALL'))
  const contentTypeOptions = computed(() => dict.value.external_request_content_type || [])
  const executionModeOptions = computed(() => dict.value.external_api_execution_mode || [])
  const booleanOptions = computed(() => toBooleanDictOptions(dict.value.sys_yes_no))
  const callStatusOptions = computed(() => toNumberDictOptions(dict.value.external_call_status))
  const callTypeOptions = computed(() => toBooleanDictOptions(dict.value.external_call_type))

  const systemCountText = computed(() => systemCount.value ? `${systemCount.value} 个系统` : '暂无系统')

  const systemSearchSchema = computed(() => [
    { field: 'systemName', label: '系统名称', type: 'input', props: { placeholder: '搜索系统名称' } },
    { field: 'systemCode', label: '系统编码', type: 'input', props: { placeholder: '搜索系统编码' } },
    { field: 'systemStatus', label: '状态', type: 'select', props: { placeholder: '全部状态', options: statusOptions.value } },
  ])

  const apiSearchSchema = computed(() => [
    { field: 'apiName', label: '接口名称', type: 'input', props: { placeholder: '搜索接口名称' } },
    { field: 'apiCode', label: '接口编码', type: 'input', props: { placeholder: '搜索接口编码' } },
    { field: 'apiMethod', label: '请求方法', type: 'select', props: { placeholder: '全部方法', options: methodOptions.value } },
    { field: 'apiStatus', label: '状态', type: 'select', props: { placeholder: '全部状态', options: statusOptions.value } },
  ])

  const systemTableColumns = computed(() => [
    {
      prop: 'systemName',
      label: '系统名称',
      width: 220,
      render: row => h('div', { class: 'name-cell' }, [
        h('span', { class: 'primary-text' }, row.systemName || '-'),
        h('span', { class: 'secondary-text' }, row.systemCode || '未设置编码'),
      ]),
    },
    {
      prop: 'baseUrl',
      label: '基础URL',
      width: 300,
      render: row => renderTextWithTooltip(row.baseUrl, 'url-cell'),
    },
    {
      prop: 'authType',
      label: '认证',
      width: 120,
      render: row => h(DictTag, { options: authTypeOptions.value, value: row.authType, size: 'small' }),
    },
    {
      prop: 'apiCount',
      label: '接口数',
      width: 90,
      align: 'right',
      render: row => h('a', {
        class: 'table-link',
        onClick: (event) => {
          event.stopPropagation()
          handleOpenSystemApis(row)
        },
      }, row.apiCount ?? 0),
    },
    {
      prop: 'requestLoggingEnabled',
      label: '请求日志',
      width: 90,
      render: row => renderBooleanTag(row.requestLoggingEnabled),
    },
    {
      prop: 'trustedInternal',
      label: '内部系统',
      width: 90,
      render: row => renderBooleanTag(row.trustedInternal),
    },
    {
      prop: 'systemStatus',
      label: '状态',
      width: 80,
      render: row => renderStatusTag(row.systemStatus),
    },
    {
      prop: 'action',
      label: '操作',
      width: 170,
      fixed: 'right',
      maxActionButtons: 2,
      actions: [
        { label: '接口', key: 'apis', type: 'primary', onClick: handleOpenSystemApis },
        { label: '编辑', key: 'edit', type: 'primary', onClick: row => systemCrudRef.value?.showEdit(row) },
        { label: '日志', key: 'logs', type: 'info', onClick: handleOpenSystemLogs },
        { label: '删除', key: 'delete', type: 'error', onClick: row => systemCrudRef.value?.handleDelete(row) },
      ],
    },
  ])

  const apiTableColumns = computed(() => [
    {
      prop: 'apiName',
      label: '接口名称',
      width: 220,
      render: row => h('div', { class: 'name-cell' }, [
        h('span', { class: 'primary-text' }, row.apiName || '-'),
        h('span', { class: 'secondary-text' }, row.apiCode || '未设置编码'),
      ]),
    },
    { prop: 'systemName', label: '所属系统', width: 160 },
    {
      prop: 'apiMethod',
      label: '模式/方法',
      width: 110,
      render: row => h('div', { class: 'name-cell name-cell--compact' }, [
        h(DictTag, { options: executionModeOptions.value, value: row.executionMode || 'HTTP', size: 'small' }),
        h('span', { class: 'secondary-text' }, row.apiMethod || '-'),
      ]),
    },
    { prop: 'requestContentType', label: '请求配置', width: 180, render: row => renderApiRequestConfig(row) },
    { prop: 'apiPath', label: '接口路径', width: 280, render: row => renderTextWithTooltip(row.apiPath, 'url-cell') },
    { prop: 'responseDataPath', label: '数据路径', width: 130, render: row => renderTextWithTooltip(row.responseDataPath || '-', 'mono-cell') },
    { prop: 'successCodes', label: '成功规则', width: 160, render: row => renderApiResponseRule(row) },
    {
      prop: 'docFileId',
      label: '文档',
      width: 80,
      render: row => renderDocTag(row),
    },
    {
      prop: 'apiStatus',
      label: '状态',
      width: 80,
      render: row => renderStatusTag(row.apiStatus),
    },
    { prop: 'sortOrder', label: '排序', width: 80, align: 'right' },
    { prop: 'createTime', label: '创建时间', width: 170 },
    {
      prop: 'action',
      label: '操作',
      width: 170,
      fixed: 'right',
      maxActionButtons: 2,
      actions: [
        { label: '调试', key: 'debug', type: 'success', onClick: handleOpenDebug },
        { label: '编辑', key: 'edit', type: 'primary', onClick: row => apiCrudRef.value?.showEdit(row) },
        { label: '前端调用', key: 'usage', type: 'success', onClick: handleOpenUsage },
        { label: '日志', key: 'logs', type: 'info', onClick: handleOpenLogs },
        { label: '文档', key: 'doc', type: 'primary', onClick: handleOpenDoc },
        { label: '删除', key: 'delete', type: 'error', onClick: row => apiCrudRef.value?.handleDelete(row) },
      ],
    },
  ])

  const logSearchSchema = computed(() => [
    { field: 'callStatus', label: '调用状态', type: 'select', props: { placeholder: '全部状态', options: callStatusOptions.value } },
    { field: 'debugFlag', label: '调用类型', type: 'select', props: { placeholder: '全部类型', options: callTypeOptions.value } },
  ])

  const logTableColumns = computed(() => [
    { prop: 'apiName', label: '接口名称', width: 180 },
    { prop: 'requestMethod', label: '方法', width: 80, render: row => renderMethodTag(row.requestMethod) },
    { prop: 'requestUrl', label: '请求地址', width: 340, render: row => renderTextWithTooltip(row.requestUrl, 'url-cell') },
    {
      prop: 'callStatus',
      label: '状态',
      width: 80,
      render: row => h(DictTag, { options: callStatusOptions.value, value: row.callStatus, size: 'small' }),
    },
    { prop: 'httpStatusCode', label: 'HTTP', width: 80, align: 'right' },
    { prop: 'durationMs', label: '耗时', width: 100, align: 'right', render: row => renderDurationTag(row.durationMs) },
    {
      prop: 'debugFlag',
      label: '类型',
      width: 90,
      render: row => h(DictTag, { options: callTypeOptions.value, value: row.debugFlag, size: 'small' }),
    },
    { prop: 'createTime', label: '调用时间', width: 170 },
    { prop: 'errorMessage', label: '错误信息', width: 220, render: row => renderTextWithTooltip(row.errorMessage || '-', 'error-cell') },
    {
      prop: 'action',
      label: '操作',
      width: 120,
      fixed: 'right',
      maxActionButtons: 2,
      actions: [
        { label: '查看', key: 'edit', type: 'primary', onClick: row => logCrudRef.value?.showEdit(row) },
        { label: '删除', key: 'delete', type: 'error', onClick: row => logCrudRef.value?.handleDelete(row) },
      ],
    },
  ])

  const logDetailSchema = computed(() => [
    { field: 'apiName', label: '接口名称', type: 'text' },
    { field: 'callStatus', label: '调用状态', type: 'text', formatter: value => getLabel('external_call_status', value) },
    { field: 'requestUrl', label: '请求地址', type: 'text', span: 2 },
    { field: 'requestBody', label: '请求参数', type: 'textarea', span: 2, props: { rows: 5, readonly: true } },
    { field: 'responseBody', label: '响应内容', type: 'textarea', span: 2, props: { rows: 8, readonly: true } },
    { field: 'errorMessage', label: '错误信息', type: 'textarea', span: 2, props: { rows: 3, readonly: true } },
  ])

  function boolSwitch(field, label, defaultValue, extra = {}) {
    return {
      field,
      label,
      type: 'switch',
      defaultValue,
      checkedText: '开',
      uncheckedText: '关',
      ...extra,
    }
  }

  function toBool(value, fallback = false) {
    if (value === true || value === 1 || value === '1' || value === 'true')
      return true
    if (value === false || value === 0 || value === '0' || value === 'false')
      return false
    return fallback
  }

  const systemEditSchema = computed(() => [
    { field: '__system_base', type: 'divider', label: '基础信息', span: 2, props: { titlePlacement: 'left', description: '先填写对方系统地址，接口路径会基于该地址拼接。' } },
    { field: 'systemName', label: '系统名称', type: 'input', required: true, props: { placeholder: '如：统一用户中心' } },
    { field: 'systemCode', label: '系统编码', type: 'input', required: true, props: { placeholder: '如：user_center' } },
    { field: 'baseUrl', label: '基础URL', type: 'input', required: true, span: 2, props: { placeholder: '如：https://api.example.com' } },
    { field: 'systemStatus', label: '状态', type: 'radio', defaultValue: 1, props: { options: statusOptions.value } },
    boolSwitch('trustedInternal', '可信内部系统', false),
    { field: 'systemDesc', label: '系统描述', type: 'textarea', span: 2, props: { placeholder: '说明系统用途、负责人或调用范围', rows: 2 } },

    { field: '__system_auth', type: 'divider', label: '认证配置', span: 2, props: { titlePlacement: 'left', description: '按对方实际鉴权方式选择，密钥加密存储且不会出现在列表中。' } },
    { field: 'authType', label: '认证类型', type: 'select', required: true, props: { options: authTypeOptions.value, placeholder: '请选择认证类型' } },
    { field: 'basicUsername', label: '用户名', type: 'input', vIf: form => form.authType === 'basic', props: { placeholder: 'Basic Auth 用户名' } },
    { field: 'basicPassword', label: '密码', type: 'input', vIf: form => form.authType === 'basic', description: '留空或保持 ****** 表示沿用已保存的密码', props: { type: 'password', showPasswordOn: 'click', placeholder: 'Basic Auth 密码' } },
    { field: 'tokenHeaderName', label: 'Token Header', type: 'select', vIf: form => form.authType === 'token', props: { options: tokenHeaderOptions, filterable: true, tag: true, placeholder: '默认 Authorization' } },
    { field: 'tokenPrefix', label: 'Token前缀', type: 'select', vIf: form => form.authType === 'token', props: { options: tokenPrefixOptions, filterable: true, tag: true, placeholder: '默认 Bearer' } },
    { field: 'tokenValue', label: 'Token值', type: 'textarea', span: 2, vIf: form => form.authType === 'token', description: '留空或保持 ****** 表示沿用已保存的 Token', props: { placeholder: '请输入 Token', rows: 3 } },
    { field: 'tokenHeaderName', label: '透传Header', type: 'select', vIf: form => form.authType === 'current_token', props: { options: tokenHeaderOptions, filterable: true, tag: true, placeholder: '默认 Authorization' } },
    { field: 'tokenPrefix', label: '透传前缀', type: 'select', vIf: form => form.authType === 'current_token', props: { options: tokenPrefixOptions, filterable: true, tag: true, placeholder: '默认 Bearer' } },
    { field: 'oauth2TokenUrl', label: 'Token URL', type: 'input', span: 2, vIf: form => form.authType === 'oauth2', props: { placeholder: 'OAuth2 获取 Token 的地址' } },
    { field: 'oauth2ClientId', label: 'Client ID', type: 'input', vIf: form => form.authType === 'oauth2' },
    { field: 'oauth2ClientSecret', label: 'Client Secret', type: 'input', vIf: form => form.authType === 'oauth2', description: '留空或保持 ****** 表示沿用已保存的 Secret', props: { type: 'password', showPasswordOn: 'click' } },
    { field: 'oauth2GrantType', label: '授权类型', type: 'select', vIf: form => form.authType === 'oauth2', defaultValue: 'client_credentials', props: { options: oauth2GrantTypeOptions, filterable: true, tag: true, placeholder: '默认 client_credentials' } },
    { field: 'oauth2Scope', label: 'Scope', type: 'input', vIf: form => form.authType === 'oauth2', props: { placeholder: '可选' } },
    { field: 'apiKeyName', label: 'API Key名称', type: 'input', vIf: form => form.authType === 'api_key', props: { placeholder: '如：X-API-Key' } },
    { field: 'apiKeyPosition', label: 'API Key位置', type: 'select', vIf: form => form.authType === 'api_key', props: { options: apiKeyPositionOptions.value, placeholder: '请选择放置位置' } },
    { field: 'apiKeyValue', label: 'API Key值', type: 'textarea', span: 2, vIf: form => form.authType === 'api_key', description: '留空或保持 ****** 表示沿用已保存的 Key', props: { rows: 3, placeholder: '请输入 API Key' } },
    {
      field: 'customAuthAdapter',
      label: '认证适配器',
      type: 'select',
      required: true,
      vIf: form => form.authType === 'custom',
      props: {
        options: dict.value[EXTERNAL_AUTH_ADAPTER_DICT] || [],
        placeholder: '请选择认证适配器',
        filterable: true,
      },
    },
    {
      field: 'customAuthConfig',
      label: '认证参数',
      type: 'slot',
      slotName: 'customAuthConfig',
      span: 2,
      vIf: form => form.authType === 'custom',
    },

    { field: 'systemAdvanced', label: '高级设置', type: 'slot', slotName: 'systemAdvanced', span: 2, showLabel: false },
    { field: 'remark', label: '备注', type: 'textarea', span: 2, props: { rows: 2, placeholder: '补充说明' } },
  ])

  const apiEditSchema = computed(() => [
    { field: '__api_base', type: 'divider', label: '基础信息', span: 2, props: { titlePlacement: 'left', description: '先选系统和请求方式，路径会拼在所属系统的基础 URL 后面。' } },
    { field: 'apiPresetGuide', type: 'slot', slotName: 'apiPresetGuide', span: 2, showLabel: false },
    {
      field: 'systemNameDisplay',
      label: '所属系统',
      type: 'text',
      vIf: () => !!selectedSystem.value && !isApiStandalone.value,
      formatter: () => selectedSystem.value?.systemName || selectedSystem.value?.systemCode || '-',
    },
    {
      field: 'systemId',
      label: '所属系统',
      type: 'select',
      required: true,
      vIf: () => !selectedSystem.value || isApiStandalone.value,
      props: { options: systemOptions.value, placeholder: '请选择外部系统', filterable: true },
    },
    { field: 'apiStatus', label: '状态', type: 'radio', defaultValue: 1, props: { options: statusOptions.value } },
    { field: 'apiName', label: '接口名称', type: 'input', required: true, props: { placeholder: '如：查询用户列表' } },
    { field: 'apiCode', label: '接口编码', type: 'input', required: true, props: { placeholder: '如：query_users' } },
    { field: 'executionMode', label: '执行模式', type: 'select', defaultValue: 'HTTP', props: { options: executionModeOptions.value, placeholder: '请选择执行模式' } },
    { field: 'apiMethod', label: '请求方法', type: 'select', required: true, props: { options: methodOptions.value, placeholder: '请选择请求方法' } },
    { field: 'apiPath', label: '接口路径', type: 'input', required: true, vIf: form => (form.executionMode || 'HTTP') !== 'MOCK', props: { placeholder: '如：/api/v1/users' } },
    { field: 'urlPreview', label: '请求地址预览', type: 'slot', slotName: 'urlPreview', span: 2, showLabel: false, vIf: form => (form.executionMode || 'HTTP') !== 'MOCK' },
    { field: 'apiDesc', label: '接口描述', type: 'textarea', span: 2, props: { placeholder: '说明接口用途和调用场景', rows: 2 } },

    { field: '__api_request', type: 'divider', label: '请求配置', span: 2, props: { titlePlacement: 'left', description: '请求头、输入参数、固定参数各自独占一行，按块填写即可。' } },
    { field: 'requestContentType', label: '请求格式', type: 'select', defaultValue: 'application/json', props: { options: contentTypeOptions.value } },
    { field: 'responseContentType', label: '响应格式', type: 'select', defaultValue: 'application/json', props: { options: contentTypeOptions.value } },
    { field: 'requestHeaders', label: '额外请求头', type: 'slot', slotName: 'requestHeaders', span: 2, showLabel: false, gridClass: 'external-config-block' },
    { field: 'inputSchemaJson', label: '输入参数定义', type: 'slot', slotName: 'inputSchemaJson', span: 2, showLabel: false, gridClass: 'external-config-block' },
    { field: 'requestParams', label: '固定请求参数', type: 'slot', slotName: 'requestParams', span: 2, showLabel: false, gridClass: 'external-config-block' },
    { field: 'requestBodyTemplate', label: '请求体模板', type: 'slot', slotName: 'requestBodyTemplate', span: 2, showLabel: false, vIf: form => ['POST', 'PUT', 'PATCH'].includes(form.apiMethod) },
    boolSwitch('paramMappingEnabled', '启用参数映射', false),
    { field: 'paramMappings', label: '参数映射规则', type: 'slot', slotName: 'paramMappings', span: 2, showLabel: false, vIf: form => form.paramMappingEnabled === true },

    { field: '__api_response', type: 'divider', label: '响应配置', span: 2, props: { titlePlacement: 'left', description: '按顺序配置：先粘贴响应实例生成字段结构，再完成成功判定与数据定位。' } },
    { field: 'outputSchemaJson', label: '返回字段解析', type: 'slot', slotName: 'outputSchemaJson', span: 2, showLabel: false },
    { field: 'successCodes', label: '成功码', type: 'slot', slotName: 'successCodes', span: 2, showLabel: false, defaultValue: '0,200' },
    { field: 'responsePaths', label: '数据与错误定位', type: 'slot', slotName: 'responsePaths', span: 2, showLabel: false },
    { field: 'mockResponseJson', label: 'Mock响应', type: 'slot', slotName: 'mockResponseJson', span: 2, showLabel: false, vIf: form => (form.executionMode || 'HTTP') === 'MOCK' },

    { field: 'apiAdvanced', label: '高级设置', type: 'slot', slotName: 'apiAdvanced', span: 2, showLabel: false },
    { field: 'remark', label: '备注', type: 'textarea', span: 2, props: { rows: 2, placeholder: '补充说明' } },
  ])

  function renderStatusTag(status) {
    return h(DictTag, { options: statusOptions.value, value: status, size: 'small' })
  }

  function renderMethodTag(method) {
    return h(DictTag, { options: methodOptions.value, value: method, size: 'small' })
  }

  function renderBooleanTag(value) {
    return h(DictTag, { options: booleanOptions.value, value, size: 'small' })
  }

  function renderDocTag(row) {
    return h(NTag, { size: 'small', type: row.docFileId ? 'success' : 'default' }, { default: () => row.docFileId ? '已维护' : '未维护' })
  }

  function renderApiRequestConfig(row) {
    const hasExtraConfig = row.requestHeaders || row.requestParams || row.requestBodyTemplate || row.paramMappingEnabled
    return h('div', { class: 'name-cell' }, [
      h('span', { class: 'primary-text' }, row.requestContentType || '-'),
      h('span', { class: 'secondary-text' }, hasExtraConfig ? '已配置请求规则' : '默认透传参数'),
    ])
  }

  function renderApiResponseRule(row) {
    const ruleText = row.errorCodePath ? `错误码: ${row.errorCodePath}` : `HTTP ${row.successCodes || '2xx'}`
    return h('div', { class: 'name-cell' }, [
      h('span', { class: 'primary-text' }, ruleText),
      h('span', { class: 'secondary-text' }, row.responseTransformEnabled ? '已启用响应转换' : '未转换'),
    ])
  }

  function renderDurationTag(duration) {
    const value = Number(duration) || 0
    const type = value > 3000 ? 'error' : value > 1000 ? 'warning' : 'success'
    return h(NTag, { size: 'small', type }, { default: () => formatDuration(value) })
  }

  function renderTextWithTooltip(value, className = 'truncate-cell') {
    const text = value || '-'
    return h(NTooltip, { trigger: 'hover' }, {
      trigger: () => h('span', { class: className }, text),
      default: () => text,
    })
  }

  function beforeRenderSystemList(list) {
    mergeSystemOptions(list)
    return list
  }

  function handleSystemListLoaded({ total }) {
    systemCount.value = Number(total || 0)
  }

  function getDefaultSystemForm() {
    return {
      authType: 'none',
      systemStatus: 1,
      trustedInternal: false,
      retryEnabled: true,
      retryMaxAttempts: 3,
      retryBackoffInterval: 1000,
      connectTimeout: 5000,
      readTimeout: 10000,
      writeTimeout: 10000,
      sslVerifyEnabled: true,
      requestLoggingEnabled: true,
    }
  }

  function beforeRenderSystemForm(row) {
    // part2 写入 __impl.formatSystemForm；提交/渲染时再解析，避免拆分后闭包未绑定
    return __impl.formatSystemForm(row || getDefaultSystemForm())
  }

  function beforeRenderSystemDetail(data) {
    return __impl.formatSystemForm({
      ...getDefaultSystemForm(),
      ...data,
    })
  }

  function beforeSubmitSystem(formData) {
    const payload = __impl.cleanSystemAuthFields(formData)
    if (payload.authType === 'custom') {
      if (!payload.customAuthAdapter) {
        message.error('请选择认证适配器')
        return false
      }
      if (!validateJson(payload.customAuthConfig, '自定义认证配置')) {
        return false
      }
    }
    return __impl.trimPayload(payload)
  }

  function useQichachaPreset(formData, updateValue) {
    if (formData) {
      formData.authType = 'custom'
      formData.customAuthAdapter = 'qichacha'
    }
    updateValue(JSON.stringify({ appKey: '', secret: '' }))
    message.success('已填充企查查认证结构，请补充 AppKey 和 Secret')
  }

  function useQichachaApiPreset(formData) {
    if (!formData)
      return
    Object.assign(formData, {
      apiName: '企查查企业查询',
      apiCode: 'company-search',
      executionMode: 'HTTP',
      apiMethod: 'GET',
      apiPath: '/ECIV4/Search',
      requestContentType: 'application/json',
      responseContentType: 'application/json',
      requestParams: JSON.stringify({ key: '' }),
      responseDataPath: 'Result',
      errorCodePath: 'Status',
      errorMsgPath: 'Message',
      successCodes: '200',
      paramMappingEnabled: true,
      paramMappings: JSON.stringify({ keyword: { target: 'key' } }),
      lowcodeQueryEnabled: true,
      inputSchemaJson: JSON.stringify([{ name: 'keyword', label: '企业名称或统一社会信用代码', type: 'string', required: true }]),
      outputSchemaJson: JSON.stringify([
        { name: 'companyName', label: '企业名称', path: 'Result.0.Name', type: 'string' },
        { name: 'creditCode', label: '统一社会信用代码', path: 'Result.0.CreditCode', type: 'string' },
      ]),
    })
    message.success('企查查接口模板已填充，请核对接口文档和权限后保存')
  }

  function beforeLoadApiList(params) {
    if (selectedSystem.value?.id) {
      return { ...params, systemId: selectedSystem.value.id }
    }
    return params
  }

  function beforeLoadLogList(params) {
    const nextParams = buildLogParams(params)
    loadLogSummary(nextParams)
    return nextParams
  }

  function buildLogParams(params = {}) {
    const nextParams = { ...params }
    if (logApi.value?.scopeType === 'system') {
      nextParams.systemId = logApi.value.systemId
      return nextParams
    }
    if (logApi.value?.id) {
      nextParams.apiId = logApi.value.id
    }
    if (logApi.value?.systemId) {
      nextParams.systemId = logApi.value.systemId
    }
    return nextParams
  }

  function getDefaultApiForm() {
    const systemId = getSelectedSystemId()
    return {
      systemId,
      systemNameDisplay: selectedSystem.value?.systemName || selectedSystem.value?.systemCode || '',
      executionMode: 'HTTP',
      apiMethod: 'GET',
      requestContentType: 'application/json',
      responseContentType: 'application/json',
      paramMappingEnabled: false,
      responseTransformEnabled: false,
      successCodes: '0,200',
      rateLimitEnabled: false,
      rateLimitQps: 10,
      cacheEnabled: false,
      cacheTtl: 300,
      permissionCheckEnabled: true,
      lowcodeQueryEnabled: false,
      apiStatus: 1,
      sortOrder: 0,
    }
  }

  function formatApiForm(data) {
    return {
      ...data,
      executionMode: data?.executionMode || 'HTTP',
      paramMappingEnabled: toBool(data?.paramMappingEnabled, false),
      responseTransformEnabled: toBool(data?.responseTransformEnabled, false),
      rateLimitEnabled: toBool(data?.rateLimitEnabled, false),
      cacheEnabled: toBool(data?.cacheEnabled, false),
      permissionCheckEnabled: toBool(data?.permissionCheckEnabled, true),
      lowcodeQueryEnabled: toBool(data?.lowcodeQueryEnabled, false),
    }
  }

  function beforeRenderApiForm(row) {
    if (!row) {
      return getDefaultApiForm()
    }
    const systemId = getSelectedSystemId()
    return formatApiForm({
      ...row,
      systemId: systemId || row.systemId,
      systemNameDisplay: selectedSystem.value?.systemName || row.systemName || '',
    })
  }

  function beforeRenderApiDetail(data) {
    const systemId = getSelectedSystemId()
    return formatApiForm({
      ...getDefaultApiForm(),
      ...data,
      systemId: systemId || data?.systemId,
      systemNameDisplay: selectedSystem.value?.systemName || data?.systemName || '',
    })
  }

  function beforeSubmitApi(formData) {
    const payload = { ...formData }
    // responsePaths 是「数据与错误定位」组合卡片的虚拟插槽字段，实体值在各路径字段上
    delete payload.responsePaths
    const systemId = getSelectedSystemId()
    if (systemId && !isApiStandalone.value) {
      payload.systemId = systemId
    }
    if (!payload.systemId) {
      message.error('请选择所属系统')
      return false
    }
    payload.executionMode = payload.executionMode || 'HTTP'
    if (payload.executionMode === 'MOCK') {
      payload.apiPath = payload.apiPath || '/mock'
      if (!payload.mockResponseJson?.trim()) {
        message.error('Mock模式必须配置Mock响应JSON')
        return false
      }
      if (!validateJson(payload.mockResponseJson, 'Mock响应JSON')) {
        return false
      }
    }
    if (!validateJson(payload.inputSchemaJson, '输入参数定义')
      || !validateJson(payload.outputSchemaJson, '返回字段解析')) {
      return false
    }
    if (payload.lowcodeQueryEnabled === true) {
      if (parseSchema(payload.inputSchemaJson).length === 0) {
        message.error('开放为低代码查询源时必须配置输入参数定义，无参数可留空不开放')
        return false
      }
      if (parseSchema(payload.outputSchemaJson).length === 0) {
        message.error('开放为低代码查询源时必须配置返回字段解析')
        return false
      }
    }
    if (!__impl.validateApiSchemaFields(payload.inputSchemaJson, '输入参数定义', false)
      || !__impl.validateApiSchemaFields(payload.outputSchemaJson, '返回字段解析', true)) {
      return false
    }
    const jsonFields = [
      ['requestHeaders', '额外请求头'],
      ['requestParams', '固定请求参数'],
      ['paramMappings', '参数映射规则'],
    ]
    for (const [field, label] of jsonFields) {
      if (!validateJson(payload[field], label)) {
        return false
      }
    }
    if (payload.requestContentType === 'application/json' && !validateJson(payload.requestBodyTemplate, '请求体模板')) {
      return false
    }
    return __impl.trimPayload(payload)
  }

  function getSelectedSystemId() {
    return selectedSystem.value?.id || selectedSystem.value?.systemId || null
  }

  function handleOpenDebug(row) {
    debugApi.value = row
    debugParamsText.value = formatDebugParams(row.requestParams)
    debugFieldSchema.value = parseSchema(row.inputSchemaJson)
    debugFieldValues.value = Object.fromEntries(debugFieldSchema.value.map((field) => {
      if (field.type === 'boolean')
        return [field.name, false]
      if (field.type === 'integer' || field.type === 'number')
        return [field.name, null]
      return [field.name, '']
    }))
    debugResult.value = null
    debugModalVisible.value = true
  }

  function handleOpenUsage(row) {
    usageApi.value = row
    const method = String(row.apiMethod || 'GET').toUpperCase()
    const call = ['GET', 'HEAD'].includes(method)
      ? `callExternalApi(${row.id}, { keyword: '请输入企业名称或统一社会信用代码' })`
      : `callExternalApi(${row.id}, { keyword: '请输入查询条件' }, '${method}')`
    usageCode.value = `import { callExternalApi } from '@/api/external/api'\n\n// 由后端代理调用，浏览器无需接触企查查 AppKey/Secret\nconst response = await ${call}\nconst result = response.data\nconsole.log(result)`
    usageModalVisible.value = true
  }

  async function copyUsageCode() {
    try {
      await navigator.clipboard.writeText(usageCode.value)
      message.success('示例代码已复制')
    }
    catch {
      message.error('复制失败，请手动复制')
    }
  }

  function formatDebugParams(value) {
    if (!value || typeof value !== 'string') {
      return '{}'
    }
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    }
    catch {
      return '{}'
    }
  }

  async function handleRunDebug() {
    let params = {}
    if (debugFieldSchema.value.length) {
      params = { ...debugFieldValues.value }
      const missingField = debugFieldSchema.value.find((field) => {
        if (!field.required)
          return false
        const value = params[field.name]
        if (value === null || value === undefined)
          return true
        return typeof value === 'string' && !value.trim()
      })
      if (missingField) {
        message.error(`请填写${missingField.label || missingField.name}`)
        return
      }
      debugFieldSchema.value.forEach((field) => {
        const value = params[field.name]
        if (typeof value !== 'string')
          return
        if (field.type === 'integer' && value !== '')
          params[field.name] = Number.parseInt(value, 10)
        else if (field.type === 'number' && value !== '')
          params[field.name] = Number(value)
        else if (field.type === 'boolean')
          params[field.name] = value === 'true'
      })
    }
    else {
      try {
        params = debugParamsText.value?.trim() ? JSON.parse(debugParamsText.value) : {}
        if (!params || Array.isArray(params) || typeof params !== 'object') {
          message.error('调试参数必须是 JSON 对象')
          return
        }
      }
      catch {
        message.error('调试参数必须是合法 JSON')
        return
      }
    }
    debugLoading.value = true
    try {
      const res = await debugExternalApi(debugApi.value.id, params)
      debugResult.value = res.data
      logCrudRef.value?.refresh()
    }
    finally {
      debugLoading.value = false
    }
  }

  function parseSchema(value) {
    if (!value)
      return []
    try {
      const parsed = typeof value === 'string' ? JSON.parse(value) : value
      return Array.isArray(parsed) ? parsed.filter(item => item?.name) : []
    }
    catch {
      return []
    }
  }

  const PATH_TYPE_LABELS = { string: '文本', integer: '整数', number: '小数', boolean: '布尔', object: '对象', array: '数组' }

  // 路径选择器的候选项来自「返回字段解析」的结果；preferredType 用于把匹配类型排到前面
  function pathOptionsFor(formData, preferredType) {
    const options = parseSchema(formData?.outputSchemaJson)
      .filter(row => row.path)
      .map(row => ({
        label: `${row.path}（${PATH_TYPE_LABELS[row.type] || '文本'}）`,
        value: row.path,
        fieldType: row.type,
      }))
    if (preferredType)
      options.sort((a, b) => Number(b.fieldType === preferredType) - Number(a.fieldType === preferredType))
    return options
  }

  function handleOpenLogs(row) {
    logApi.value = row
    logDrawerVisible.value = true
    const params = buildLogParams()
    loadLogSummary(params)
    nextTick(() => logCrudRef.value?.refresh())
  }

  function handleOpenSystemLogs(row) {
    logApi.value = {
      scopeType: 'system',
      systemId: row.id,
      apiName: row.systemName || row.systemCode,
    }
    logDrawerVisible.value = true
    const params = buildLogParams()
    loadLogSummary(params)
    nextTick(() => logCrudRef.value?.refresh())
  }

  async function loadLogSummary(params = {}) {
    try {
      const res = await getExternalApiLogSummary(params)
      logSummary.value = res.data || {}
    }
    catch (error) {
      console.error('加载外部接口日志汇总失败', error)
    }
  }

  function handleClearLogs() {
    const targetName = logApi.value?.apiName || logApi.value?.apiCode
    const targetLabel = logApi.value?.scopeType === 'system' ? '系统' : '接口'
    window.$dialog.warning({
      title: '确认清空',
      content: logApi.value ? `确定清空${targetLabel}「${targetName}」的调用日志吗？` : '确定清空调用日志吗？',
      positiveText: '清空',
      negativeText: '取消',
      onPositiveClick: async () => {
        await clearExternalApiLogs({
          apiId: logApi.value?.id,
          systemId: logApi.value?.systemId,
        })
        message.success('清空成功')
        loadLogSummary(buildLogParams())
        logCrudRef.value?.refresh()
      },
    })
  }

  function handleRefreshLogs() {
    loadLogSummary(buildLogParams())
    logCrudRef.value?.refresh()
  }

  async function handleOpenDoc(row) {
    const res = await getExternalApiById(row.id)
    docApi.value = res.data || row
    docFiles.value = docApi.value?.docFileId
      ? [{
          id: docApi.value.docFileId,
          fileId: docApi.value.docFileId,
          originalName: docApi.value.docFileName || docApi.value.docFileId,
          name: docApi.value.docFileName || docApi.value.docFileId,
        }]
      : []
    docModalVisible.value = true
  }

  function handleDocUploadSuccess(fileData) {
    if (!docApi.value) {
      return
    }
    docApi.value.docFileId = fileData.fileId
    docApi.value.docFileName = fileData.originalName || fileData.fileName || fileData.fileId
  }

  function handleDocRemove() {
    if (!docApi.value) {
      return
    }
    docApi.value.docFileId = ''
    docApi.value.docFileName = ''
  }

  async function handleSaveDoc() {
    if (!docApi.value) {
      return
    }
    const currentFile = Array.isArray(docFiles.value) ? docFiles.value[0] : null
    docApi.value.docFileId = currentFile?.fileId || currentFile?.url || ''
    docApi.value.docFileName = currentFile?.originalName || currentFile?.fileName || currentFile?.name || ''
    docSaving.value = true
    try {
      await updateExternalApiDocument(docApi.value)
      message.success('接口文档保存成功')
      docModalVisible.value = false
      apiCrudRef.value?.refresh()
    }
    finally {
      docSaving.value = false
    }
  }

  async function downloadDoc() {
    if (!docApi.value?.docFileId) {
      message.warning('暂无可下载文档')
      return
    }
    try {
      const response = await managedFetch(getFileDownloadUrl(docApi.value.docFileId), {
        method: 'GET',
        headers: {
          'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
          'X-Timestamp': Date.now().toString(),
          'X-Nonce': generateUUID(),
        },
      }, {
        globalLoadingType: 'download',
        globalLoadingText: '文件下载处理中，请稍候...',
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || '下载失败')
      }

      const blob = await response.blob()
      const blobUrl = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = getDownloadFileName(response, docApi.value.docFileName || docApi.value.docFileId)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      setTimeout(() => URL.revokeObjectURL(blobUrl), 100)
    }
    catch (error) {
      message.error(`下载失败：${error.message || '未知错误'}`)
    }
  }

  function getDownloadFileName(response, fallbackName) {
    const contentDisposition = response.headers.get('Content-Disposition')
    if (!contentDisposition) {
      return fallbackName
    }
    const utf8Match = contentDisposition.match(/filename\*=utf-8''([^;]+)/i)
    if (utf8Match?.[1]) {
      return decodeURIComponent(utf8Match[1])
    }
    const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
    if (normalMatch?.[1]) {
      return decodeURIComponent(normalMatch[1])
    }
    return fallbackName
  }

  function formatPercent(value) {
    return `${Number(value || 0).toFixed(2)}%`
  }

  function formatDuration(value) {
    const duration = Number(value || 0)
    if (duration >= 1000) {
      return `${(duration / 1000).toFixed(2)}s`
    }
    return `${Math.round(duration)}ms`
  }

  function formatJson(value) {
    if (value === null || value === undefined) {
      return ''
    }
    if (typeof value === 'string') {
      try {
        return JSON.stringify(JSON.parse(value), null, 2)
      }
      catch {
        return value
      }
    }
    return JSON.stringify(value, null, 2)
  }

  function handleOpenSystemApis(row) {
    selectedSystem.value = row
    apiViewMode.value = true
    nextTick(() => {
      apiCrudRef.value?.setSearchParams({})
      apiCrudRef.value?.refresh()
    })
  }

  function showAllApis() {
    selectedSystem.value = null
    apiViewMode.value = true
    nextTick(() => apiCrudRef.value?.refresh())
  }

  function backToSystems() {
    selectedSystem.value = null
    apiViewMode.value = false
    nextTick(() => systemCrudRef.value?.refresh())
  }

  async function handleSystemSaved() {
    await loadSystemOptions()
    systemCrudRef.value?.refresh()
    apiCrudRef.value?.refresh()
  }

  async function handleSystemDeleted() {
    selectedSystem.value = null
    await loadSystemOptions()
    apiCrudRef.value?.refresh()
  }

  function handleApiSaved() {
    systemCrudRef.value?.refresh()
  }

  function handleApiDeleted() {
    systemCrudRef.value?.refresh()
  }

  async function loadSystemOptions() {
    try {
      const res = await getExternalSystemList()
      mergeSystemOptions(res.data || [])
    }
    catch (error) {
      console.error('加载外部系统列表失败', error)
    }
  }

  function mergeSystemOptions(list) {
    const optionMap = new Map()
    const urlMap = { ...systemBaseUrlMap.value }
    systemOptions.value.forEach(item => optionMap.set(item.value, item))
    list.forEach((item) => {
      if (item.id) {
        optionMap.set(item.id, {
          label: item.systemCode ? `${item.systemName}（${item.systemCode}）` : item.systemName,
          value: item.id,
        })
        urlMap[item.id] = item.baseUrl || ''
      }
    })
    systemOptions.value = Array.from(optionMap.values())
    systemBaseUrlMap.value = urlMap
  }

  function validateJson(value, label) {
    if (typeof value !== 'string' || value.trim() === '') {
      return true
    }
    try {
      JSON.parse(value)
      return true
    }
    catch {
      message.error(`${label} 必须是合法 JSON`)
      return false
    }
  }

  // 与后端 ExternalQueryContractValidator#SAFE_NAME 保持一致
  const SCHEMA_SAFE_NAME = /^[a-z]\w{0,63}$/i

  __impl.boolSwitch = boolSwitch
  __impl.toBool = toBool
  __impl.renderStatusTag = renderStatusTag
  __impl.renderMethodTag = renderMethodTag
  __impl.renderBooleanTag = renderBooleanTag
  __impl.renderDocTag = renderDocTag
  __impl.renderApiRequestConfig = renderApiRequestConfig
  __impl.renderApiResponseRule = renderApiResponseRule
  __impl.renderDurationTag = renderDurationTag
  __impl.renderTextWithTooltip = renderTextWithTooltip
  __impl.beforeRenderSystemList = beforeRenderSystemList
  __impl.handleSystemListLoaded = handleSystemListLoaded
  __impl.getDefaultSystemForm = getDefaultSystemForm
  __impl.beforeRenderSystemForm = beforeRenderSystemForm
  __impl.beforeRenderSystemDetail = beforeRenderSystemDetail
  __impl.beforeSubmitSystem = beforeSubmitSystem
  __impl.useQichachaPreset = useQichachaPreset
  __impl.useQichachaApiPreset = useQichachaApiPreset
  __impl.beforeLoadApiList = beforeLoadApiList
  __impl.beforeLoadLogList = beforeLoadLogList
  __impl.buildLogParams = buildLogParams
  __impl.getDefaultApiForm = getDefaultApiForm
  __impl.formatApiForm = formatApiForm
  __impl.beforeRenderApiForm = beforeRenderApiForm
  __impl.beforeRenderApiDetail = beforeRenderApiDetail
  __impl.beforeSubmitApi = beforeSubmitApi
  __impl.getSelectedSystemId = getSelectedSystemId
  __impl.handleOpenDebug = handleOpenDebug
  __impl.handleOpenUsage = handleOpenUsage
  __impl.copyUsageCode = copyUsageCode
  __impl.formatDebugParams = formatDebugParams
  __impl.handleRunDebug = handleRunDebug
  __impl.parseSchema = parseSchema
  __impl.pathOptionsFor = pathOptionsFor
  __impl.handleOpenLogs = handleOpenLogs
  __impl.handleOpenSystemLogs = handleOpenSystemLogs
  __impl.loadLogSummary = loadLogSummary
  __impl.handleClearLogs = handleClearLogs
  __impl.handleRefreshLogs = handleRefreshLogs
  __impl.handleOpenDoc = handleOpenDoc
  __impl.handleDocUploadSuccess = handleDocUploadSuccess
  __impl.handleDocRemove = handleDocRemove
  __impl.handleSaveDoc = handleSaveDoc
  __impl.downloadDoc = downloadDoc
  __impl.getDownloadFileName = getDownloadFileName
  __impl.formatPercent = formatPercent
  __impl.formatDuration = formatDuration
  __impl.formatJson = formatJson
  __impl.handleOpenSystemApis = handleOpenSystemApis
  __impl.showAllApis = showAllApis
  __impl.backToSystems = backToSystems
  __impl.handleSystemSaved = handleSystemSaved
  __impl.handleSystemDeleted = handleSystemDeleted
  __impl.handleApiSaved = handleApiSaved
  __impl.handleApiDeleted = handleApiDeleted
  __impl.loadSystemOptions = loadSystemOptions
  __impl.mergeSystemOptions = mergeSystemOptions
  __impl.validateJson = validateJson

  return {
    props, emit, __impl, mut, backToSystems, beforeLoadApiList, beforeLoadLogList, beforeRenderApiDetail,
    beforeRenderApiForm, beforeRenderSystemDetail, beforeRenderSystemForm, beforeRenderSystemList, beforeSubmitApi, beforeSubmitSystem, boolSwitch, buildLogParams,
    copyUsageCode, downloadDoc, formatApiForm, formatDebugParams, formatDuration, formatJson, formatPercent,
    getDefaultApiForm, getDefaultSystemForm, getDownloadFileName, getSelectedSystemId, handleApiDeleted, handleApiSaved, handleClearLogs,
    handleDocRemove, handleDocUploadSuccess, handleOpenDebug, handleOpenDoc, handleOpenLogs, handleOpenSystemApis, handleOpenSystemLogs, handleOpenUsage,
    handleRefreshLogs, handleRunDebug, handleSaveDoc, handleSystemDeleted, handleSystemListLoaded, handleSystemSaved, loadLogSummary, loadSystemOptions,
    mergeSystemOptions, parseSchema, pathOptionsFor, renderApiRequestConfig, renderApiResponseRule, renderBooleanTag, renderDocTag, renderDurationTag,
    renderMethodTag, renderStatusTag, renderTextWithTooltip, showAllApis, toBool, useQichachaApiPreset, useQichachaPreset,
    validateJson, message, authStore, systemCrudRef, apiCrudRef, logCrudRef, selectedSystem,
    systemOptions, systemBaseUrlMap, systemCount, apiViewMode, logDrawerVisible, logApi, logSummary, debugModalVisible,
    debugApi, debugParamsText, debugFieldSchema, debugFieldValues, debugResult, debugLoading, usageModalVisible, usageApi,
    usageCode, docModalVisible, docApi, docFiles, docSaving, EXTERNAL_AUTH_ADAPTER_DICT, apiFormSlotNames, oauth2GrantTypeOptions,
    tokenHeaderOptions, tokenPrefixOptions, isApiStandalone, logDrawerWidth, apiPageTitle, panelSubtitle, logDrawerTitle, systemApiConfig,
    apiApiConfig, logApiConfig, authTypeOptions, apiKeyPositionOptions, statusOptions, methodOptions, contentTypeOptions, executionModeOptions,
    booleanOptions, callStatusOptions, callTypeOptions, systemCountText, systemSearchSchema, apiSearchSchema, systemTableColumns, apiTableColumns,
    logSearchSchema, logTableColumns, logDetailSchema, systemEditSchema, apiEditSchema, PATH_TYPE_LABELS, SCHEMA_SAFE_NAME,
  }
}
