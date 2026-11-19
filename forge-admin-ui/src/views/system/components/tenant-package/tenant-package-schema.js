import { buildSystemEditCards, SYSTEM_EDIT_FULL_SPAN } from '../../schemas/systemEditLayout'

export const BOOTSTRAP_PACKAGE_OPTIONS = [
  { label: '字典与系统配置', value: 'dictAndConfig' },
  { label: '默认根组织', value: 'rootOrg' },
  { label: '租户管理员角色与菜单（不含租户管理）', value: 'tenantAdminRole' },
  { label: '消息模板与业务类型', value: 'messageTemplates' },
  { label: '审批常用语与 SpEL 模板', value: 'flowAssist' },
  { label: '数据权限预设', value: 'dataScopePresets' },
]

export const ALL_BOOTSTRAP_CODES = BOOTSTRAP_PACKAGE_OPTIONS.map(item => item.value)

export const DEFAULT_FLAG_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 },
]

export function createTenantPackageEditSchema({ statusOptions }) {
  return buildSystemEditCards([
    {
      label: '基础信息',
      icon: 'forge:module',
      fields: [
        {
          field: 'packageCode',
          label: '套餐编码',
          type: 'input',
          rules: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
          props: { placeholder: '如 standard / enterprise' },
        },
        {
          field: 'packageName',
          label: '套餐名称',
          type: 'input',
          rules: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
          props: { placeholder: '请输入套餐名称' },
        },
        {
          field: 'packageStatus',
          label: '状态',
          type: 'radio',
          defaultValue: 1,
          props: { options: statusOptions },
        },
        {
          field: 'isDefault',
          label: '默认套餐',
          type: 'radio',
          defaultValue: 0,
          props: { options: DEFAULT_FLAG_OPTIONS },
        },
        {
          field: 'sort',
          label: '排序',
          type: 'number',
          defaultValue: 100,
          props: { placeholder: '越小越靠前', min: 0 },
        },
        {
          field: 'description',
          label: '说明',
          type: 'textarea',
          span: SYSTEM_EDIT_FULL_SPAN,
          props: { placeholder: '面向运营的开通说明', rows: 2 },
        },
        {
          field: 'remark',
          label: '备注',
          type: 'textarea',
          span: SYSTEM_EDIT_FULL_SPAN,
          props: { placeholder: '内部备注', rows: 2 },
        },
      ],
    },
    {
      label: '开通组成项',
      icon: 'forge:actions',
      fields: [
        {
          field: 'bootstrapPackages',
          label: '初始化包',
          type: 'checkbox',
          span: SYSTEM_EDIT_FULL_SPAN,
          defaultValue: [...ALL_BOOTSTRAP_CODES],
          props: { options: BOOTSTRAP_PACKAGE_OPTIONS },
        },
        {
          field: 'userLimit',
          label: '默认人员上限',
          type: 'number',
          defaultValue: 0,
          props: { placeholder: '0 表示无限制', min: 0 },
        },
        {
          field: 'resourceIdsText',
          label: '菜单资源白名单',
          type: 'textarea',
          span: SYSTEM_EDIT_FULL_SPAN,
          props: {
            placeholder: '可选：资源 ID 列表，逗号或 JSON 数组；留空则开户时从默认 admin 拷贝并排除租户管理',
            rows: 3,
          },
        },
      ],
    },
  ])
}

export function itemsToFormFields(items = []) {
  const bootstrapPackages = []
  let userLimit = 0
  let resourceIdsText = ''
  for (const item of items) {
    if (!item)
      continue
    if (String(item.itemType).toUpperCase() === 'BOOTSTRAP' && item.itemCode) {
      if (isTruthy(item.itemValue))
        bootstrapPackages.push(item.itemCode)
    }
    else if (String(item.itemType).toUpperCase() === 'QUOTA' && item.itemCode === 'userLimit') {
      const parsed = Number(item.itemValue)
      userLimit = Number.isFinite(parsed) ? parsed : 0
    }
    else if (String(item.itemType).toUpperCase() === 'RESOURCE' && item.itemCode === 'resourceIds') {
      resourceIdsText = item.itemValue || ''
    }
  }
  return { bootstrapPackages, userLimit, resourceIdsText }
}

export function formFieldsToItems(formData = {}) {
  const items = []
  const selected = Array.isArray(formData.bootstrapPackages) ? formData.bootstrapPackages : []
  ALL_BOOTSTRAP_CODES.forEach((code, index) => {
    if (selected.includes(code)) {
      items.push({
        itemType: 'BOOTSTRAP',
        itemCode: code,
        itemValue: 'true',
        sort: index + 1,
      })
    }
  })
  items.push({
    itemType: 'QUOTA',
    itemCode: 'userLimit',
    itemValue: String(formData.userLimit ?? 0),
    sort: 100,
  })
  const resourceIdsText = String(formData.resourceIdsText || '').trim()
  if (resourceIdsText) {
    items.push({
      itemType: 'RESOURCE',
      itemCode: 'resourceIds',
      itemValue: resourceIdsText,
      sort: 200,
    })
  }
  return items
}

function isTruthy(value) {
  if (value == null || value === '')
    return true
  const v = String(value).trim().toLowerCase()
  return !(v === '0' || v === 'false' || v === 'no' || v === 'off')
}
