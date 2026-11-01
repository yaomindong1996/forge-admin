/** menu.vue setup part 2. */

import { computed } from 'vue'
import { createAutoCodeSync } from '@/utils'

export function applyMenuPagePart2(deps = {}) {
  const {
    __impl,
    formData,
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
    allResources,
  } = deps

  const permsAutoCode = createAutoCodeSync({
    scene: 'perms',
    targetField: 'perms',
    buildOptions: (current) => ({
      parentPerms: findResourcePerms(current?.parentId),
      resourceType: current?.resourceType,
    }),
  })

  function findResourcePerms(parentId) {
    if (parentId === null || parentId === undefined || Number(parentId) === 0)
      return ''
    const walk = (nodes = []) => {
      for (const node of nodes) {
        if (Number(node?.id) === Number(parentId))
          return node?.perms || ''
        const nested = walk(node?.children || [])
        if (nested)
          return nested
      }
      return ''
    }
    return walk(allResources?.value || [])
  }

  function maybeSyncPerms(payload = {}) {
    const current = payload.formData || formData?.value || {}
    const type = Number(current.resourceType)
    if (type !== 3 && type !== 4)
      return
    permsAutoCode.onSourceChange({
      ...payload,
      value: current.resourceName,
      formData: current,
    })
  }

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

  // 与用户管理一致：分区卡片 + 24 栅格 span8（不再用 divider 顶栏 Tab）
  const editSchema = computed(() => [
    {
      type: 'card',
      nodeType: 'card',
      label: '基础信息',
      props: { icon: 'forge:module', bordered: true },
      children: [
        {
          field: 'parentId',
          label: '上级资源',
          type: 'treeSelect',
          span: 8,
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
          onChange: ({ value, formData: current, context }) => {
            maybeSyncPerms({ formData: { ...current, parentId: value }, context })
          },
        },
        {
          field: 'resourceName',
          label: '资源名称',
          type: 'input',
          span: 8,
          rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
          props: { placeholder: '请输入资源名称' },
          onChange: ({ value, formData: current, context }) => {
            maybeSyncPerms({ value, formData: { ...current, resourceName: value }, context })
          },
        },
        {
          field: 'resourceType',
          label: '资源类型',
          type: 'radio',
          span: 8,
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
          quickCreate: {
            preset: 'dictData',
            dictType: 'sys_resource_type',
            linkLabel: '新增类型',
          },
          props: { options: resourceTypeOptions.value },
          onChange: ({ value, formData: current, context }) => {
            maybeSyncPerms({ formData: { ...current, resourceType: value }, context })
          },
        },
        {
          field: 'clientCode',
          label: '客户端',
          type: 'radio',
          span: 8,
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
          span: 8,
          defaultValue: 2,
          rules: [{ required: true, type: 'number', message: '请选择最低用户类型', trigger: 'change' }],
          quickCreate: {
            preset: 'dictData',
            dictType: 'sys_user_type',
            linkLabel: '新增类型',
          },
          props: {
            placeholder: '请选择最低用户类型',
            options: minUserTypeOptions.value,
          },
        },
        {
          field: 'sort',
          label: '排序',
          type: 'inputNumber',
          span: 8,
          defaultValue: 0,
          props: { placeholder: '排序值', min: 0 },
        },
      ],
    },
    {
      type: 'card',
      nodeType: 'card',
      label: '目录/菜单配置',
      props: { icon: 'forge:builder', bordered: true },
      vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
      children: [
        {
          field: 'icon',
          label: '图标',
          type: 'slot',
          span: 8,
          slotName: 'icon',
        },
        {
          field: 'isExternal',
          label: '是否外链',
          type: 'radio',
          span: 8,
          defaultValue: 0,
          props: { options: yesNoOptions.value },
          vIf: currentFormData => currentFormData.resourceType === 2,
        },
        {
          field: 'path',
          label: Number(formData?.value?.isExternal) === 1 ? '外链地址' : '路由地址',
          type: 'slot',
          slotName: 'path',
          span: 8,
        },
        {
          field: 'component',
          label: '组件路径',
          type: 'slot',
          span: 8,
          slotName: 'component',
          vIf: currentFormData => currentFormData.resourceType === 2 && Number(currentFormData.isExternal) !== 1,
        },
        {
          field: 'redirect',
          label: '重定向地址',
          type: 'input',
          span: 8,
          props: { placeholder: '目录跳转地址，可留空' },
          vIf: currentFormData => (currentFormData.resourceType === 1 || currentFormData.resourceType === 2)
            && Number(currentFormData.isExternal) !== 1,
        },
        {
          field: 'openTarget',
          label: '打开方式',
          type: 'radio',
          span: 8,
          defaultValue: '_self',
          quickCreate: {
            preset: 'dictData',
            dictType: 'sys_link_open_target',
            linkLabel: '新增方式',
          },
          props: { options: openTargetOptions.value },
          vIf: currentFormData => currentFormData.resourceType === 2
            && (Number(currentFormData.isExternal) === 1 || Number(currentFormData.ssoEnabled) === 1),
        },
        {
          field: 'ssoEnabled',
          label: '启用SSO',
          type: 'switch',
          span: 8,
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
          span: 8,
          rules: [{ required: true, message: '请选择目标子系统', trigger: 'change' }],
          props: {
            placeholder: '请选择目标子系统',
            clearable: true,
          },
          options: ({ formData: currentFormData }) => getSsoTargetClientOptions(currentFormData),
          vIf: currentFormData => currentFormData.resourceType === 2 && currentFormData.ssoEnabled === 1,
        },
        {
          field: 'keepAlive',
          label: '是否缓存',
          type: 'radio',
          span: 8,
          defaultValue: 0,
          props: { options: yesNoOptions.value },
          vIf: currentFormData => currentFormData.resourceType === 2 && Number(currentFormData.isExternal) !== 1,
        },
        {
          field: 'alwaysShow',
          label: '总是显示',
          type: 'radio',
          span: 8,
          defaultValue: 0,
          props: { options: yesNoOptions.value },
        },
      ],
    },
    {
      type: 'card',
      nodeType: 'card',
      label: '按钮/API配置',
      props: { icon: 'forge:connection', bordered: true },
      vIf: currentFormData => currentFormData.resourceType === 3 || currentFormData.resourceType === 4,
      children: [
        {
          field: 'perms',
          label: '权限标识',
          type: 'input',
          span: 8,
          labelTip: '根据名称与上级自动生成，可手动修改',
          props: { placeholder: '自动生成，如：system:user:add' },
          onChange: permsAutoCode.onTargetChange,
        },
        {
          field: 'apiMethod',
          label: '请求方法',
          type: 'select',
          span: 8,
          defaultValue: 'GET',
          quickCreate: {
            preset: 'dictData',
            dictType: 'sys_req_method',
            linkLabel: '新增方法',
          },
          props: { placeholder: '请求方法', options: apiMethodOptions.value },
          vIf: currentFormData => currentFormData.resourceType === 4,
        },
        {
          field: 'apiUrl',
          label: '接口地址',
          type: 'input',
          span: 8,
          props: { placeholder: '/system/user/list' },
          vIf: currentFormData => currentFormData.resourceType === 4,
        },
      ],
    },
    {
      type: 'card',
      nodeType: 'card',
      label: '状态配置',
      props: { icon: 'forge:actions', bordered: true },
      children: [
        {
          field: 'visible',
          label: '显示状态',
          type: 'radio',
          span: 8,
          defaultValue: 1,
          props: { options: visibleOptions.value },
        },
        {
          field: 'menuStatus',
          label: '菜单状态',
          type: 'radio',
          span: 8,
          defaultValue: 1,
          props: { options: visibleOptions.value },
          vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
        },
        {
          field: 'remark',
          label: '备注',
          type: 'textarea',
          span: 24,
          props: { placeholder: '请输入备注', rows: 3 },
        },
      ],
    },
  ])
  __impl.handleComponentPathChange = handleComponentPathChange
  __impl.resetPermsAutoCode = () => permsAutoCode.reset()

  return {
    ...deps,
    editSchema,
    handleComponentPathChange,
  }
}
