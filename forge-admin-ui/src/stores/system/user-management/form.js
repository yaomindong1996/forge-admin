import { computed, h } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import { FORM_MAIN_ORG_ID_EXPR, FORM_TENANT_ID_EXPR, isSystemAdminUser, normalizeNumberList, normalizeSingleNumber, renderDictTag, resolveOptionLabel, resolveUserAccountLabel, resolveUserDisplayName, splitTableCellValues } from './utils'

// 共享状态由 Pinia 创建；跨领域调用通过同一工作台实例协调。
export function createUserForm(state, runtime, workspace) {
  const { selectedOrgNode, editingUserTenantId, resetPwdModalVisible, resetPwdForm, searchRegionOptions, editRegionOptions } = state
  const { userStore, crudRef } = runtime
  const searchSchema = computed(() => [
    {
      field: 'username',
      label: '用户名',
      type: 'input',
      props: {
        placeholder: '请输入用户名',
      },
    },
    {
      field: 'realName',
      label: '真实姓名',
      type: 'input',
      props: {
        placeholder: '请输入真实姓名',
      },
    },
    {
      field: 'regionCode',
      label: '行政区划',
      type: 'treeSelect',
      props: {
        placeholder: '请选择行政区划',
        clearable: true,
        filterable: true,
      },
      options: () => searchRegionOptions.value,
    },
    {
      field: 'phone',
      label: '手机号',
      type: 'input',
      props: {
        placeholder: '请输入手机号',
      },
    },
    {
      field: 'userStatus',
      label: '状态',
      type: 'select',
      props: {
        placeholder: '请选择状态',
        options: workspace.userStatusOptions.value,
      },
    },
  ])

  const tableColumns = computed(() => [
    {
      prop: 'username',
      label: '用户',
      minWidth: 190,
      render: row => h(SystemTableCell, {
        title: resolveUserDisplayName(row),
        subtitle: resolveUserAccountLabel(row),
        interactive: true,
        avatar: true,
        tooltip: `查看用户详情：${row.username || '-'}`,
        onActivate: () => crudRef.value?.showDetail(row),
      }),
    },
    {
      prop: 'phone',
      label: '手机号',
      minWidth: 130,
    },
    {
      prop: 'orgName',
      label: '所属组织',
      minWidth: 160,
      render: row => h(SystemTableCell, {
        values: splitTableCellValues(row.orgName || row.orgNames),
      }),
    },
    {
      prop: 'postName',
      label: '岗位',
      minWidth: 130,
      render: row => h(SystemTableCell, {
        values: splitTableCellValues(row.postName || row.postNames),
      }),
    },
    {
      prop: 'userType',
      label: '用户类型',
      width: 100,
      render: row => renderDictTag(workspace.userTypeOptions.value, normalizeSingleNumber(row.userType)),
    },
    {
      prop: 'userStatus',
      label: '状态',
      width: 90,
      render: row => renderDictTag(workspace.userStatusOptions.value, normalizeSingleNumber(row.userStatus), 'user-status-tag'),
    },
    {
      prop: 'action',
      label: '操作',
      width: 160,
      fixed: 'right',
      actions: [
        { label: '编辑', key: 'edit', onClick: workspace.handleEdit },
        {
          label: '重置密码',
          key: 'resetPwd',
          type: 'warning',
          visible: row => !workspace.isRestrictedCurrentUser(row.id),
          onClick: (row) => {
            resetPwdForm.value = { id: row.id, password: '' }
            resetPwdModalVisible.value = true
          },
        },
        { label: '高级关系维护', key: 'relation', type: 'info', onClick: workspace.handleRelation, visible: row => !workspace.isRestrictedCurrentUser(row.id) },
        { label: '禁用', key: 'disable', type: 'warning', onClick: row => workspace.handleUpdateStatus(row, 0), visible: row => !isSystemAdminUser(row) && !workspace.isRestrictedCurrentUser(row.id) && row.userStatus === 1 },
        { label: '启用', key: 'enable', type: 'success', onClick: workspace.handleUntieDisable, visible: row => !isSystemAdminUser(row) && !workspace.isRestrictedCurrentUser(row.id) && row.userStatus !== 1 },
        { label: '删除', key: 'delete', type: 'error', onClick: workspace.handleDelete, visible: row => !isSystemAdminUser(row) && !workspace.isRestrictedCurrentUser(row.id) },
      ],
    },
  ])

  const editSchema = computed(() => [
    {
      type: 'divider',
      label: '基础信息',
      props: {
        titlePlacement: 'left',
      },
      span: 2,
    },
    {
      field: 'username',
      label: '用户名',
      type: 'input',
      rules: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      props: {
        placeholder: '请输入用户名',
      },
    },
    {
      field: 'realName',
      label: '真实姓名',
      type: 'input',
      rules: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
      props: {
        placeholder: '请输入真实姓名',
      },
    },
    {
      field: 'password',
      label: '密码',
      type: 'input',
      rules: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      props: {
        inputType: 'password',
        showPasswordOn: 'click',
        placeholder: '请输入密码',
      },
      vIf: formData => !formData.id,
    },
    {
      field: 'userType',
      label: '用户类型',
      type: 'select',
      defaultValue: 2,
      rules: [{ required: true, type: 'number', message: '请选择用户类型', trigger: 'change' }],
      labelTip: '普通用户不能绑定【本租户数据】及以上数据范围的角色；租户管理员不能绑定【全部数据】范围的角色',
      props: {
        placeholder: '请选择用户类型',
        options: workspace.userTypeOptions.value,
        disabled: !userStore.isAdmin,
      },
    },
    {
      type: 'divider',
      label: '组织与授权',
      props: {
        titlePlacement: 'left',
      },
      span: 2,
    },
    {
      field: 'orgIds',
      label: '所属组织',
      type: 'treeSelect',
      span: 2,
      required: true,
      multiple: true,
      filterable: true,
      labelTip: '首个选中组织会自动作为主组织',
      onChange: ({ value, context }) => {
        const orgIds = normalizeNumberList(value)
        context?.patchFormData?.({
          mainOrgId: orgIds[0] || null,
        })
      },
      optionSource: {
        type: 'tree',
        // 全量树：可搜索，展开可见下级；懒加载树在下拉里展开后经常空白且无法过滤
        api: 'get@/system/org/tree',
        params: {
          tenantId: FORM_TENANT_ID_EXPR,
        },
        valueField: 'id',
        keyField: 'id',
        labelField: 'orgName',
        fallbackLabelFields: ['name'],
        childrenField: 'children',
      },
      props: {
        placeholder: '请选择所属组织（可搜索）',
        multiple: true,
        clearable: true,
        filterable: true,
        cascade: false,
        checkable: true,
        showPath: false,
        defaultExpandAll: true,
      },
    },
    {
      field: 'roleIds',
      label: '角色',
      type: 'select',
      span: 1,
      labelTip: '不选组织时展示全部角色，选组织后会按主组织刷新',
      optionSource: {
        api: 'get@/system/role/page',
        params: {
          tenantId: FORM_TENANT_ID_EXPR,
          orgId: FORM_MAIN_ORG_ID_EXPR,
          pageNum: 1,
          pageSize: 200,
        },
        valueField: 'id',
        labelField: 'roleName',
        recordsField: 'data.list',
      },
      props: {
        placeholder: '请选择角色',
        multiple: true,
        clearable: true,
        filterable: true,
      },
    },
    {
      field: 'postIds',
      label: '岗位',
      type: 'select',
      span: 1,
      optionSource: {
        api: 'get@/system/post/list',
        params: {
          tenantId: FORM_TENANT_ID_EXPR,
        },
        valueField: 'id',
        labelField: 'postName',
      },
      props: {
        placeholder: '请选择岗位',
        multiple: true,
        clearable: true,
        filterable: true,
      },
    },
    {
      field: 'allowedClients',
      label: '允许客户端',
      type: 'select',
      span: 2,
      labelTip: '不选则默认所有客户端均可登录',
      optionSource: {
        api: 'get@/system/client/list',
        valueField: 'clientCode',
        labelField: 'clientName',
        recordsField: 'data',
      },
      props: {
        placeholder: '请选择允许登录的客户端（不选则不限制）',
        multiple: true,
        clearable: true,
        filterable: true,
      },
    },
    {
      type: 'divider',
      label: '联系信息',
      props: {
        titlePlacement: 'left',
      },
      span: 2,
    },
    {
      field: 'phone',
      label: '手机号',
      type: 'input',
      rules: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
      ],
      props: {
        placeholder: '请输入手机号',
      },
    },
    {
      field: 'email',
      label: '邮箱',
      type: 'input',
      rules: [
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
      ],
      props: {
        placeholder: '请输入邮箱',
      },
    },
    {
      field: 'idCard',
      label: '身份证号',
      type: 'input',
      rules: [
        { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}([\dX])$)/i, message: '请输入正确的身份证号', trigger: 'blur' },
      ],
      props: {
        placeholder: '请输入身份证号',
      },
    },
    {
      field: 'gender',
      label: '性别',
      type: 'radio',
      defaultValue: 0,
      props: {
        options: workspace.genderOptions.value,
      },
    },
    {
      type: 'divider',
      label: '行政区划',
      props: {
        titlePlacement: 'left',
      },
      span: 2,
    },
    {
      field: 'regionCode',
      label: '行政区划',
      type: 'treeSelect',
      props: {
        placeholder: '请选择行政区划',
        clearable: true,
        filterable: true,
      },
      options: () => editRegionOptions.value,
    },
    {
      type: 'divider',
      label: '状态配置',
      props: {
        titlePlacement: 'left',
      },
      span: 2,
    },
    {
      field: 'userStatus',
      label: '用户状态',
      type: 'radio',
      defaultValue: 1,
      vIf: formData => !workspace.isRestrictedCurrentUser(formData.id) && !isSystemAdminUser(formData),
      props: {
        options: workspace.userStatusOptions.value,
      },
    },
    {
      field: 'remark',
      label: '备注',
      type: 'textarea',
      span: 2,
      props: {
        placeholder: '请输入备注',
        rows: 3,
      },
    },
  ])

  function beforeRenderUserForm(row) {
    const defaultOrgId = !row?.id
      ? normalizeSingleNumber(selectedOrgNode.value?.id || userStore.activeOrgId || userStore.userInfo?.activeOrgId)
      : null
    const normalized = workspace.normalizeUserFormData({
      ...row,
      orgIds: row?.orgIds?.length
        ? row.orgIds
        : defaultOrgId !== null
          ? [defaultOrgId]
          : row?.orgIds,
      mainOrgId: row?.mainOrgId ?? defaultOrgId,
    } || {}, userStore.userInfo?.tenantId)
    editingUserTenantId.value = normalized.tenantId || null
    return normalized
  }

  async function beforeRenderUserDetail(data) {
    if (!data?.id) {
      return workspace.normalizeUserFormData(data || {}, userStore.userInfo?.tenantId)
    }
    const normalizedData = workspace.normalizeUserFormData(data, editingUserTenantId.value || userStore.userInfo?.tenantId)
    const tenantId = normalizedData.tenantId
    const next = {
      ...normalizedData,
      tenantId,
      userTypeLabel: resolveOptionLabel(workspace.userTypeOptions.value, normalizeSingleNumber(data.userType)),
      genderLabel: resolveOptionLabel(workspace.genderOptions.value, normalizeSingleNumber(data.gender)),
      userStatusLabel: resolveOptionLabel(workspace.userStatusOptions.value, normalizeSingleNumber(data.userStatus)),
    }
    if (!userStore.isAdmin && workspace.isCurrentLoginUser(data.id)) {
      return next
    }
    return next
  }

  function beforeSubmit(formData) {
    Object.assign(formData, workspace.normalizeUserFormData(formData, userStore.userInfo?.tenantId))
    formData.tenantId = userStore.userInfo?.tenantId
    formData.tenantIds = [userStore.userInfo?.tenantId].filter(Boolean)
    if (!userStore.isAdmin) {
      formData.userType = 2
    }
    if (!formData.mainOrgId && Array.isArray(formData.orgIds) && formData.orgIds.length > 0) {
      formData.mainOrgId = formData.orgIds[0]
    }
    // allowedClients: 前端数组 → 后端逗号分隔字符串，空数组表示不限制
    if (Array.isArray(formData.allowedClients)) {
      formData.allowedClients = formData.allowedClients.filter(Boolean).join(',') || null
    }
    return formData
  }

  return { searchSchema, tableColumns, editSchema, beforeRenderUserForm, beforeRenderUserDetail, beforeSubmit }
}
