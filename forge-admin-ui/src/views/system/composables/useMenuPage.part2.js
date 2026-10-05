/** menu.vue setup part 2. */

import { computed } from 'vue'

export function applyMenuPagePart2(deps = {}) {
  const {
    __impl,
    getSsoTargetClientOptions,
    normalizeComponentValue,
    resourceTypeOptions,
    visibleOptions,
    apiMethodOptions,
    openTargetOptions,
    minUserTypeOptions,
    yesNoOptions,
    routeOptions,
    parentResourceOptions,
    clientCodeOptions,
  } = deps
  function handleComponentPathChange(componentPath, updateValue, currentFormData) {
    const normalizedComponent = normalizeComponentValue(componentPath)
    updateValue(normalizedComponent)
    if (!currentFormData)
      return

    currentFormData.component = normalizedComponent
    const selected = routeOptions.find(option => option.component === normalizedComponent)
    if (selected?.path && !currentFormData.path)
      currentFormData.path = selected.path
  }

  const editSchema = computed(() => [
    {
      field: '__basicDivider',
      type: 'divider',
      label: '基础信息',
      props: { titlePlacement: 'left' },
      span: 2,
    },
    {
      field: 'parentId',
      label: '上级资源',
      type: 'treeSelect',
      span: 1,
      defaultValue: 0,
      props: {
        placeholder: '请选择上级资源',
        clearable: true,
        filterable: true,
        defaultExpandAll: false,
        keyField: 'value',
        labelField: 'label',
        childrenField: 'children',
      },
      options: () => parentResourceOptions.value,
    },
    {
      field: 'resourceName',
      label: '资源名称',
      type: 'input',
      span: 1,
      rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
      props: { placeholder: '请输入资源名称' },
    },
    {
      field: 'resourceType',
      label: '资源类型',
      type: 'radio',
      span: 2,
      defaultValue: 1,
      rules: [
        {
          required: true,
          message: '请选择资源类型',
          trigger: 'change',
          validator: (_rule, value) => {
            if (value === null || value === undefined || value === '')
              return new Error('请选择资源类型')
            return true
          },
        },
      ],
      props: {
        options: resourceTypeOptions.value,
      },
    },
    {
      field: 'clientCode',
      label: '客户端',
      type: 'radio',
      span: 2,
      defaultValue: 'pc',
      rules: [
        {
          required: true,
          message: '请选择客户端',
          trigger: 'change',
          validator: (_rule, value) => {
            if (!value)
              return new Error('请选择客户端')
            return true
          },
        },
      ],
      props: { options: clientCodeOptions.value },
    },
    {
      field: 'minUserType',
      label: '最低用户类型',
      type: 'select',
      span: 1,
      defaultValue: 2,
      rules: [{ required: true, type: 'number', message: '请选择最低用户类型', trigger: 'change' }],
      props: {
        placeholder: '请选择最低用户类型',
        options: minUserTypeOptions.value,
      },
    },
    {
      field: 'sort',
      label: '排序',
      type: 'inputNumber',
      span: 1,
      defaultValue: 0,
      props: { placeholder: '排序值', min: 0 },
    },
    {
      field: '__menuDivider',
      type: 'divider',
      label: '目录/菜单配置',
      props: { titlePlacement: 'left' },
      span: 2,
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
    },
    {
      field: 'icon',
      label: '图标',
      type: 'slot',
      span: 2,
      slotName: 'icon',
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
    },
    {
      field: 'path',
      label: '路由地址',
      type: 'slot',
      slotName: 'path',
      span: 2,
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
    },
    {
      field: 'component',
      label: '组件路径',
      type: 'slot',
      span: 2,
      slotName: 'component',
      vIf: currentFormData => currentFormData.resourceType === 2,
    },
    {
      field: 'redirect',
      label: '重定向地址',
      type: 'input',
      span: 1,
      props: { placeholder: '重定向地址' },
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
    },
    {
      field: 'isExternal',
      label: '是否外链',
      type: 'radio',
      span: 1,
      defaultValue: 0,
      props: { options: yesNoOptions.value },
      vIf: currentFormData => currentFormData.resourceType === 2,
    },
    {
      field: 'ssoEnabled',
      label: '启用SSO',
      type: 'switch',
      span: 1,
      defaultValue: 0,
      checkedValue: 1,
      uncheckedValue: 0,
      checkedText: '开启',
      uncheckedText: '关闭',
      vIf: currentFormData => currentFormData.resourceType === 2,
    },
    {
      field: 'ssoTargetClient',
      label: '目标子系统',
      type: 'select',
      span: 1,
      rules: [{ required: true, message: '请选择目标子系统', trigger: 'change' }],
      props: {
        placeholder: '请选择目标子系统',
        clearable: true,
      },
      options: ({ formData: currentFormData }) => getSsoTargetClientOptions(currentFormData),
      vIf: currentFormData => currentFormData.resourceType === 2 && currentFormData.ssoEnabled === 1,
    },
    {
      field: 'openTarget',
      label: '打开方式',
      type: 'radio',
      span: 1,
      defaultValue: '_self',
      props: { options: openTargetOptions.value },
      vIf: currentFormData => currentFormData.resourceType === 2 && currentFormData.ssoEnabled === 1,
    },
    {
      field: 'keepAlive',
      label: '是否缓存',
      type: 'radio',
      span: 1,
      defaultValue: 0,
      props: { options: yesNoOptions.value },
      vIf: currentFormData => currentFormData.resourceType === 2,
    },
    {
      field: 'alwaysShow',
      label: '总是显示',
      type: 'radio',
      span: 1,
      defaultValue: 0,
      props: { options: yesNoOptions.value },
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
    },
    {
      field: '__permissionDivider',
      type: 'divider',
      label: '按钮/API配置',
      props: { titlePlacement: 'left' },
      span: 2,
      vIf: currentFormData => currentFormData.resourceType === 3 || currentFormData.resourceType === 4,
    },
    {
      field: 'perms',
      label: '权限标识',
      type: 'input',
      span: 1,
      props: { placeholder: 'sys:user:add' },
      vIf: currentFormData => currentFormData.resourceType === 3 || currentFormData.resourceType === 4,
    },
    {
      field: 'apiMethod',
      label: '请求方法',
      type: 'select',
      span: 1,
      defaultValue: 'GET',
      props: { placeholder: '请求方法', options: apiMethodOptions.value },
      vIf: currentFormData => currentFormData.resourceType === 4,
    },
    {
      field: 'apiUrl',
      label: '接口地址',
      type: 'input',
      span: 1,
      props: { placeholder: '/system/user/list' },
      vIf: currentFormData => currentFormData.resourceType === 4,
    },
    {
      field: '__statusDivider',
      type: 'divider',
      label: '状态配置',
      props: { titlePlacement: 'left' },
      span: 2,
    },
    {
      field: 'visible',
      label: '显示状态',
      type: 'radio',
      span: 1,
      defaultValue: 1,
      props: { options: visibleOptions.value },
    },
    {
      field: 'menuStatus',
      label: '菜单状态',
      type: 'radio',
      span: 1,
      defaultValue: 1,
      props: { options: visibleOptions.value },
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
    },
    {
      field: 'remark',
      label: '备注',
      type: 'textarea',
      span: 2,
      props: { placeholder: '请输入备注', rows: 3 },
    },
  ])
  __impl.handleComponentPathChange = handleComponentPathChange

  return {
    ...deps,
    editSchema,
    handleComponentPathChange,
  }
}
