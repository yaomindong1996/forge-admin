import { buildSystemEditCards, SYSTEM_EDIT_FULL_SPAN } from '../../schemas/systemEditLayout'

// 租户信息 Schema 与外观组件分别维护。
export function createTenantEditSchema({ tenantStatusOptions, businessDatasourceOptions }) {
  return buildSystemEditCards([
    {
      label: '基础信息',
      icon: 'forge:module',
      fields: [
        {
          field: 'tenantName',
          label: '租户名称',
          type: 'input',
          rules: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
          props: { placeholder: '请输入租户名称' },
        },
        {
          field: 'tenantStatus',
          label: '租户状态',
          type: 'radio',
          defaultValue: 1,
          // 默认租户（id=1）禁止在表单里改状态，与后端红线一致
          vIf: formData => Number(formData?.id) !== 1,
          props: { options: tenantStatusOptions },
        },
        {
          field: 'contactPerson',
          label: '负责人',
          type: 'input',
          rules: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
          props: { placeholder: '请输入负责人' },
        },
        {
          field: 'contactPhone',
          label: '联系电话',
          type: 'input',
          rules: [
            { required: true, message: '请输入联系电话', trigger: 'blur' },
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
          ],
          props: { placeholder: '请输入联系电话' },
        },
        {
          field: 'userLimit',
          label: '人员上限',
          type: 'number',
          defaultValue: 0,
          props: { placeholder: '0表示无限制', min: 0 },
        },
        {
          field: 'expireTime',
          label: '过期时间',
          type: 'datetime',
          props: { placeholder: '请选择过期时间', clearable: true },
        },
        {
          field: 'tenantDesc',
          label: '租户描述',
          type: 'textarea',
          props: { placeholder: '请输入租户描述', rows: 2 },
        },
      ],
    },
    {
      label: '业务数据源',
      icon: 'forge:database',
      fields: [
        {
          field: 'defaultBusinessDatasourceId',
          label: '默认业务库',
          type: 'select',
          span: SYSTEM_EDIT_FULL_SPAN,
          props: {
            placeholder: '未选择时回退主库',
            clearable: true,
            options: businessDatasourceOptions,
          },
        },
      ],
    },
    {
      label: '品牌设置',
      icon: 'forge:builder',
      fields: [
        {
          field: 'systemName',
          label: '系统名称',
          type: 'input',
          props: { placeholder: '显示在系统左上角' },
        },
        {
          field: 'browserTitle',
          label: '浏览器标签',
          type: 'input',
          props: { placeholder: '浏览器标签页显示的名称' },
        },
        {
          field: 'systemLogo',
          label: '系统Logo',
          type: 'imageUpload',
          businessType: 'tenant-logo',
          limit: 1,
          fileSize: 2,
          valueType: 'string',
          props: { showTip: true },
        },
        {
          field: 'browserIcon',
          label: '浏览器图标',
          type: 'imageUpload',
          businessType: 'tenant-icon',
          limit: 1,
          fileSize: 1,
          fileType: ['png', 'ico', 'jpg'],
          valueType: 'string',
          props: { showTip: true },
        },
        {
          field: 'systemIntro',
          label: '系统介绍',
          type: 'textarea',
          props: { placeholder: '登录页显示的系统介绍', rows: 2 },
        },
        {
          field: 'copyrightInfo',
          label: '版权信息',
          type: 'textarea',
          props: { placeholder: '页面底部显示的版权信息', rows: 2 },
        },
      ],
    },
    {
      label: '布局与外观',
      icon: 'forge:actions',
      fields: [
        {
          field: 'appearance',
          label: ' ',
          type: 'slot',
          slotName: 'appearance',
          span: SYSTEM_EDIT_FULL_SPAN,
          showFeedback: false,
        },
      ],
    },
  ])
}
