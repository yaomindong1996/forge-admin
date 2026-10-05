// 租户信息 Schema 与外观组件分别维护。
export function createTenantEditSchema({ tenantStatusOptions, businessDatasourceOptions }) {
  return [
  // ==================== 基础信息 ====================
    {
      type: 'divider',
      label: '基础信息',
      props: { titlePlacement: 'left', description: '租户的联系人、容量和有效期，优先保证业务信息完整。' },
      span: 2,
    },
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
      span: 2,
      props: { placeholder: '请输入租户描述', rows: 2 },
    },

    // ==================== 业务数据源 ====================
    {
      type: 'divider',
      label: '业务数据源',
      props: { titlePlacement: 'left', description: '未配置时 forge-business 模块按主库回退执行。' },
      span: 2,
    },
    {
      field: 'defaultBusinessDatasourceId',
      label: '默认业务库',
      type: 'select',
      span: 2,
      props: {
        placeholder: '未选择时回退主库',
        clearable: true,
        options: businessDatasourceOptions,
      },
    },

    // ==================== 品牌设置 ====================
    {
      type: 'divider',
      label: '品牌设置',
      props: { titlePlacement: 'left', description: '控制浏览器、登录页和系统左上角展示，建议先完成名称和图标。' },
      span: 2,
    },
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
      span: 2,
      props: { placeholder: '登录页显示的系统介绍', rows: 2 },
    },
    {
      field: 'copyrightInfo',
      label: '版权信息',
      type: 'textarea',
      span: 2,
      props: { placeholder: '页面底部显示的版权信息', rows: 2 },
    },

    {
      type: 'divider',
      label: '布局与外观',
      span: 2,
      props: { titlePlacement: 'left', description: '选择布局和三个基础色，其余状态自动适配。' },
    },
    { field: 'appearance', label: '', type: 'slot', slotName: 'appearance', span: 2 },
  ]
}
