/** role.vue setup part 1. */
import { NTag } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref, watch } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import PremiumTree from '@/components/common/PremiumTree.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import UserSelectPanel from '@/components/UserSelectPanel.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import RolePermissionSettings from '../components/RolePermissionSettings.vue'
export function applyRolePagePart1() {
  const __impl = {}
  const mut = {}

  // part2 延迟实现：setup 结束后经 __impl 转发，避免拆分后跨 part 裸引用报错
  function createFallbackDataScopeSettings(...args) {
    return __impl.createFallbackDataScopeSettings(...args)
  }
  async function loadAuthClientResources(...args) {
    return __impl.loadAuthClientResources(...args)
  }
  async function loadClientList(...args) {
    return __impl.loadClientList(...args)
  }
  async function loadRoleDataScopes(...args) {
    return __impl.loadRoleDataScopes(...args)
  }

  const USER_STATUS_DICT = 'sys_user_status'
  const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'
  const ROLE_TYPE_DICT = 'sys_role_type'
  const NORMAL_DISABLE_DICT = 'sys_normal_disable'
  const YES_NO_DICT = 'sys_yes_no'

  const crudRef = ref(null)
  const roleUserCrudRef = ref(null)
  const userStore = useUserStore()
  const roleList = ref([])
  const roleListLoading = ref(false)
  const roleKeyword = ref('')
  const activeRoleType = ref(null)
  const ROLE_ORG_SCOPE_GLOBAL = 1
  const ROLE_ORG_SCOPE_CUSTOM = 2

  const authModalVisible = ref(false)
  const authLoading = ref(false)
  const authLoadFailed = ref(false)
  const authSubmitLoading = ref(false)
  const resourceTreeData = ref([])
  const checkedResourceKeys = ref([])
  const dataScopeLoading = ref(false)
  const dataScopeLoadFailed = ref(false)
  const dataScopeSettings = ref({ defaultDataScope: 5, modules: [] })
  const clientList = ref([])
  const currentAuthClientCode = ref('pc')

  const currentRole = ref({})
  const addUserModalVisible = ref(false)
  const addUserLoading = ref(false)
  const assignedUserIds = ref([])
  const roleUserOrgId = ref(null)
  const roleUserKeyword = ref('')
  const roleApplicableOrgIds = ref([])
  const roleOrgTreeData = ref([])
  const roleUserTotal = ref(0)
  const roleUserCountMap = ref({})
  const userSearchParams = ref({
    userStatus: null,
  })

  const roleOrgModalVisible = ref(false)
  const roleOrgLoading = ref(false)
  const roleOrgSubmitLoading = ref(false)
  const roleScopeMode = ref('global')
  const checkedRoleOrgKeys = ref([])
  const roleOrgExpandedKeys = ref([])
  const roleOrgTreeExpandAll = ref(true)

  const { dict } = useDict(USER_STATUS_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT, YES_NO_DICT)

  const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))
  const dataScopeOptions = computed(() => toNumberOptions(dict.value[ROLE_DATA_SCOPE_DICT]))
  const manageableDataScopeOptions = computed(() => {
    if (userStore.isAdmin)
      return dataScopeOptions.value
    const deniedScopes = Number(userStore.userType) === 2 ? [1, 2] : [1]
    return dataScopeOptions.value.filter(item => !deniedScopes.includes(Number(item.value)))
  })
  const roleTypeOptions = computed(() => toNumberOptions(dict.value[ROLE_TYPE_DICT]))
  const roleStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
  const yesNoOptions = computed(() => toNumberOptions(dict.value[YES_NO_DICT]))
  const roleTypeTabs = computed(() => {
    const options = roleTypeOptions.value || []
    if (options.length > 0)
      return options.map(item => ({ label: resolveRoleTypeShortLabel(item.label), value: item.value }))
    return [{ label: '角色', value: null }]
  })
  const isCurrentRoleGlobalScope = computed(() =>
    Number(currentRole.value?.orgScopeType ?? ROLE_ORG_SCOPE_GLOBAL) === ROLE_ORG_SCOPE_GLOBAL,
  )
  const roleUserOrgOptions = computed(() => {
    const scopedOrgIds = new Set(normalizeNumberList(roleApplicableOrgIds.value))
    return flattenOrgNodes(roleOrgTreeData.value)
      .filter(item => isCurrentRoleGlobalScope.value || scopedOrgIds.has(normalizeSingleNumber(item.id)))
      .map(item => ({
        label: item.orgName,
        value: normalizeSingleNumber(item.id),
      }))
      .filter(item => item.value !== null)
  })
  const roleUserOrgTreeOptions = computed(() => {
    const scopedOrgIds = new Set(normalizeNumberList(roleApplicableOrgIds.value))
    return buildRoleUserOrgTreeOptions(roleOrgTreeData.value, scopedOrgIds, isCurrentRoleGlobalScope.value)
  })
  const allRoleOrgIds = computed(() => flattenOrgNodes(roleOrgTreeData.value)
    .map(item => normalizeSingleNumber(item.id))
    .filter(item => item !== null))
  const currentRoleScopeLabel = computed(() => {
    if (!currentRole.value?.id)
      return ''
    if (isCurrentRoleGlobalScope.value)
      return '租户全局'
    if (roleApplicableOrgIds.value.length > 0)
      return `${roleApplicableOrgIds.value.length} 个组织`
    return '未设置范围'
  })
  const currentRoleScopeTagType = computed(() => {
    if (isCurrentRoleGlobalScope.value)
      return 'success'
    return roleApplicableOrgIds.value.length > 0 ? 'info' : 'warning'
  })
  const currentRoleDataScopeLabel = computed(() => {
    if (!currentRole.value?.id)
      return ''
    return resolveRoleDataScopeLabel(currentRole.value)
  })
  const canAddUserToCurrentRole = computed(() => {
    if (!currentRole.value?.id || roleUserOrgOptions.value.length === 0)
      return false
    return !!roleUserOrgId.value
  })
  const addUserButtonText = computed(() => {
    if (!currentRole.value?.id)
      return '添加用户'
    if (roleUserOrgOptions.value.length === 0)
      return '无授权组织'
    return roleUserOrgId.value ? '添加用户' : '先选组织'
  })
  const roleUserApiConfig = computed(() => ({
    list: currentRole.value?.id
      ? `get@/system/role/${currentRole.value.id}/users`
      : '',
    detail: 'post@/system/user/getById',
  }))
  const roleUserTableColumns = computed(() => [
    {
      prop: 'username',
      label: '成员信息',
      minWidth: 190,
      render: row => h(SystemTableCell, {
        title: resolveUserDisplayName(row),
        subtitle: resolveUserAccountLabel(row),
        interactive: true,
        avatar: true,
        tooltip: `查看用户详情：${row.username || '-'}`,
        onActivate: () => roleUserCrudRef.value?.showDetail?.(row),
      }),
    },
    {
      prop: 'orgName',
      label: '所属组织',
      width: 180,
      render: row => h('span', { class: 'role-member-plain', title: resolveUserOrgLabel(row) }, resolveUserOrgLabel(row)),
    },
    {
      prop: 'phone',
      label: '联系方式',
      width: 180,
      render: row => h('span', { class: 'role-member-plain role-member-phone', title: row.phone || '' }, row.phone || '未填写手机号'),
    },
    {
      prop: 'userStatus',
      label: '状态',
      width: 110,
      render: row => h('span', {
        class: ['role-member-status', { 'is-disabled': !isUserEnabled(row) }],
      }, [
        h('span', { class: 'role-member-status-dot' }),
        h('span', { class: 'role-member-status-text' }, resolveUserStatusLabel(row)),
      ]),
    },
    {
      prop: 'actions',
      label: '操作',
      width: 90,
      fixed: 'right',
      actions: [
        {
          label: '移除',
          key: 'remove',
          type: 'primary',
          onClick: row => handleRemoveUserRole(row),
        },
      ],
    },
  ])

  function getRoleActionOptions(role = {}) {
    const options = [
      {
        label: '编辑角色',
        key: 'edit',
        icon: () => h('i', { class: 'i-material-symbols:edit-outline-rounded' }),
      },
      {
        label: '适用组织',
        key: 'org-scope',
        icon: () => h('i', { class: 'i-material-symbols:account-tree-rounded' }),
      },
      {
        label: '权限授权',
        key: 'auth',
        icon: () => h('i', { class: 'i-material-symbols:admin-panel-settings-outline-rounded' }),
      },
    ]
    if (role?.id && Number(role.id) !== 1) {
      options.push(
        { type: 'divider', key: 'delete-divider' },
        {
          label: () => h('span', { class: 'role-action-danger' }, '删除角色'),
          key: 'delete',
          icon: () => h('i', { class: 'i-material-symbols:delete-outline-rounded role-action-danger' }),
        },
      )
    }
    return options
  }

  function getRoleDropdownMenuProps() {
    return {
      class: 'role-action-dropdown-menu',
    }
  }

  async function handleRoleCardAction(key, role) {
    if (!role?.id)
      return
    if (key === 'edit') {
      handleEdit(role)
      return
    }
    if (key === 'org-scope') {
      await handleRoleOrgScope(role)
      return
    }
    if (key === 'auth') {
      await handleAuth(role)
      return
    }
    if (key === 'delete') {
      handleDelete(role)
    }
  }

  const roleOrgScopeSummary = computed(() => {
    if (allRoleOrgIds.value.length === 0)
      return '暂无组织'
    if (roleScopeMode.value === 'global')
      return `${allRoleOrgIds.value.length} 个组织`
    if (checkedRoleOrgKeys.value.length === 0)
      return '未设置'
    return `${checkedRoleOrgKeys.value.length} 个组织`
  })
  const roleOrgScopeTagType = computed(() => {
    if (allRoleOrgIds.value.length === 0 || (roleScopeMode.value === 'custom' && checkedRoleOrgKeys.value.length === 0))
      return 'warning'
    return roleScopeMode.value === 'global' ? 'success' : 'info'
  })

  const authClientTabs = computed(() => {
    if (clientList.value.length > 0)
      return clientList.value
    return [{ clientCode: 'pc', clientName: 'PC端' }]
  })
  const currentAuthClientName = computed(() => {
    const client = authClientTabs.value.find(item => item.clientCode === currentAuthClientCode.value)
    return client?.clientName || currentAuthClientCode.value || '-'
  })

  watch(roleUserOrgOptions, (options) => {
    if (roleUserOrgId.value && options.some(item => item.value === roleUserOrgId.value))
      return
    if (options.length === 1) {
      roleUserOrgId.value = options[0].value
      return
    }
    roleUserOrgId.value = null
  })

  watch(roleTypeTabs, (tabs) => {
    if (activeRoleType.value !== null || tabs.length === 0)
      return
    activeRoleType.value = tabs[0].value
    loadRoleList()
  }, { immediate: true })

  // 搜索表单配置
  const searchSchema = computed(() => [
    {
      field: 'roleName',
      label: '角色名称',
      type: 'input',
      props: {
        placeholder: '请输入角色名称',
      },
    },
    {
      field: 'roleKey',
      label: '权限字符',
      type: 'input',
      props: {
        placeholder: '请输入权限字符',
      },
    },
    {
      field: 'roleType',
      label: '角色类型',
      type: 'select',
      props: {
        placeholder: '请选择角色类型',
        options: roleTypeOptions.value,
      },
    },
    {
      field: 'roleStatus',
      label: '状态',
      type: 'select',
      props: {
        placeholder: '请选择状态',
        options: roleStatusOptions.value,
      },
    },
  ])

  // 表格列配置
  const tableColumns = computed(() => [
    {
      prop: 'roleName',
      label: '角色名称',
      width: 150,
    },
    {
      prop: 'roleKey',
      label: '权限字符',
      width: 150,
    },
    {
      prop: 'roleType',
      label: '角色类型',
      width: 120,
      render: (row) => {
        return h(DictTag, { dictType: ROLE_TYPE_DICT, value: row.roleType, size: 'small', forceTag: true })
      },
    },
    {
      prop: 'dataScope',
      label: '数据范围',
      width: 150,
      render: (row) => {
        return h(DictTag, { dictType: ROLE_DATA_SCOPE_DICT, value: row.dataScope, size: 'small', forceTag: true })
      },
    },
    {
      prop: 'sort',
      label: '排序',
      width: 80,
    },
    {
      prop: 'roleStatus',
      label: '状态',
      width: 80,
      render: (row) => {
        return h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.roleStatus, size: 'small', forceTag: true })
      },
    },
    {
      prop: 'isSystem',
      label: '系统角色',
      width: 100,
      render: (row) => {
        return h(DictTag, { dictType: YES_NO_DICT, value: row.isSystem, size: 'small', forceTag: true })
      },
    },
    {
      prop: 'remark',
      label: '备注',
      minWidth: 150,
    },
    {
      prop: 'action',
      label: '操作',
      width: 260,
      fixed: 'right',
      actions: [
        { label: '编辑', key: 'edit', onClick: handleEdit },
        { label: '适用组织', key: 'orgScope', type: 'info', onClick: handleRoleOrgScope },
        { label: '查看用户', key: 'viewUsers', onClick: handleViewUsers },
        { label: '添加用户', key: 'addUsers', type: 'success', onClick: handleAddUserFromList },
        { label: '授权', key: 'auth', onClick: handleAuth },
        { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => row.id !== 1 },
      ],
    },
  ])

  // 编辑表单配置
  const editSchema = computed(() => [
    {
      field: 'roleName',
      label: '角色名称',
      type: 'input',
      rules: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
      props: {
        placeholder: '请输入角色名称',
      },
    },
    {
      field: 'roleKey',
      label: '权限字符',
      type: 'input',
      rules: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
      props: {
        placeholder: '请输入权限字符，如：admin',
      },
    },
    {
      field: 'roleType',
      label: '角色类型',
      type: 'select',
      defaultValue: 1,
      rules: [{ required: true, type: 'number', message: '请选择角色类型', trigger: 'change' }],
      props: {
        placeholder: '请选择角色类型',
        options: roleTypeOptions.value,
      },
    },
    {
      field: 'dataScope',
      label: '数据范围',
      type: 'select',
      defaultValue: 2,
      rules: [{ required: true, type: 'number', message: '请选择数据范围', trigger: 'change' }],
      props: {
        placeholder: '请选择数据范围',
        options: manageableDataScopeOptions.value,
      },
    },
    {
      field: 'sort',
      label: '排序',
      type: 'number',
      defaultValue: 0,
      props: {
        placeholder: '排序值',
        min: 0,
      },
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
      field: 'roleStatus',
      label: '角色状态',
      type: 'radio',
      defaultValue: 1,
      props: {
        options: roleStatusOptions.value,
      },
    },
    {
      field: 'isSystem',
      label: '系统角色',
      type: 'radio',
      defaultValue: 0,
      props: {
        options: yesNoOptions.value,
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

  function toNumberOptions(options = []) {
    return options.map(item => ({
      ...item,
      value: Number(item.value),
    }))
  }

  function normalizeSingleNumber(value, fallback = null) {
    if (Array.isArray(value)) {
      const first = value.find(item => item !== null && item !== undefined && item !== '')
      return normalizeSingleNumber(first, fallback)
    }
    if (value === null || value === undefined || value === '')
      return fallback
    const numberValue = Number(value)
    return Number.isNaN(numberValue) ? fallback : numberValue
  }

  function normalizeNumberList(value) {
    const list = Array.isArray(value) ? value : (value === null || value === undefined || value === '' ? [] : [value])
    return Array.from(new Set(list
      .map(item => normalizeSingleNumber(item))
      .filter(item => item !== null)))
  }

  function resolveOptionLabel(options = [], value, fallback = '') {
    const matched = options.find(option => String(option?.value) === String(value))
    return matched?.label || fallback
  }

  function resolveRoleTypeShortLabel(label = '') {
    const text = String(label || '').trim()
    return text.replace(/角色/g, '') || text || '角色'
  }

  function resolveRoleDictValue(row = {}, field) {
    const aliasMap = {
      roleType: ['roleType', 'role_type', 'type'],
      dataScope: ['dataScope', 'data_scope'],
      roleStatus: ['roleStatus', 'role_status', 'status'],
    }
    const keys = aliasMap[field] || [field]
    const value = keys.map(key => row[key]).find(item => item !== null && item !== undefined && item !== '')
    return normalizeSingleNumber(value, value ?? '')
  }

  function resolveRoleDataScopeLabel(row = {}) {
    const value = resolveRoleDictValue(row, 'dataScope')
    return resolveOptionLabel(dataScopeOptions.value, value, row.dataScope || '-')
  }

  function resolveRoleStatusLabel(row = {}) {
    const value = resolveRoleDictValue(row, 'roleStatus')
    return Number(value) === 1 ? '正常' : '停用'
  }

  function isRoleDisabled(row = {}) {
    return Number(resolveRoleDictValue(row, 'roleStatus')) !== 1
  }

  function resolveRoleMemberCount(row = {}) {
    const count = row.userCount ?? roleUserCountMap.value[row.id] ?? row.memberCount ?? row.users
    if (count === null || count === undefined || count === '')
      return null
    const numericCount = Number(count)
    return Number.isNaN(numericCount) ? null : numericCount
  }

  function resolveUserDisplayName(row = {}) {
    return row.realName || row.name || row.nickname || row.username || `用户${row.id}`
  }

  function resolveUserAccountLabel(row = {}) {
    const username = String(row.username || '').trim()
    return username && username !== resolveUserDisplayName(row) ? `@${username}` : ''
  }

  function resolveUserOrgLabel(row = {}) {
    return row.orgName || row.org || row.deptName || row.departmentName || '-'
  }

  function resolveUserStatusLabel(row = {}) {
    return Number(row.userStatus) === 1 ? '正常' : '停用'
  }

  function isUserEnabled(row = {}) {
    return Number(row.userStatus) === 1
  }

  function flattenOrgNodes(list = []) {
    return (list || []).flatMap((item) => {
      const current = [item]
      const children = flattenOrgNodes(item.children || [])
      return [...current, ...children]
    })
  }

  function buildRoleUserOrgTreeOptions(list = [], scopedOrgIds = new Set(), globalScope = false) {
    return (list || [])
      .map((item) => {
        const value = normalizeSingleNumber(item.id)
        const children = buildRoleUserOrgTreeOptions(item.children || [], scopedOrgIds, globalScope)
        const selectable = value !== null && (globalScope || scopedOrgIds.has(value))
        if (!selectable && children.length === 0)
          return null
        return {
          label: item.orgName || item.label || '-',
          value,
          disabled: !selectable,
          children,
        }
      })
      .filter(Boolean)
  }

  function getOrgNodeIcon(node = {}) {
    return node.children?.length
      ? 'i-material-symbols:account-tree-rounded'
      : 'i-material-symbols:domain-rounded'
  }

  function getOrgNodeTone(node = {}) {
    if (!node.parentId || Number(node.parentId) === 0)
      return 'folder'
    return node.children?.length ? 'folder' : 'menu'
  }

  function buildRoleTenantParams() {
    const resolvedTenantId = userStore.userInfo?.tenantId
    return resolvedTenantId ? { tenantId: resolvedTenantId } : {}
  }

  async function loadRoleOrgTree(tenantId = currentRole.value?.tenantId) {
    const res = await request.get('/system/org/tree', {
      params: buildRoleTenantParams(tenantId),
    })
    if (res.code === 200) {
      roleOrgTreeData.value = res.data || []
      if (roleOrgTreeExpandAll.value)
        roleOrgExpandedKeys.value = getAllKeys(roleOrgTreeData.value)
    }
  }

  async function loadRoleApplicableOrgIds(roleId = currentRole.value?.id) {
    if (!roleId) {
      roleApplicableOrgIds.value = []
      return []
    }
    const res = await request.get(`/system/role/${roleId}/orgs`)
    if (res.code === 200) {
      roleApplicableOrgIds.value = normalizeNumberList(res.data || [])
      return roleApplicableOrgIds.value
    }
    roleApplicableOrgIds.value = []
    return []
  }

  // 表单提交前处理
  function beforeSubmit(formData) {
    if (!formData.id && formData.orgScopeType == null)
      formData.orgScopeType = ROLE_ORG_SCOPE_GLOBAL
    formData.tenantId = userStore.userInfo?.tenantId
    if (!userStore.isAdmin) {
      if (Number(userStore.userType) === 2 && [1, 2].includes(Number(formData.dataScope)))
        formData.dataScope = 5
      else if (Number(formData.dataScope) === 1)
        formData.dataScope = 2
    }
    return formData
  }

  async function loadRoleList() {
    try {
      roleListLoading.value = true
      const params = {
        pageNum: 1,
        pageSize: 200,
        roleName: roleKeyword.value || undefined,
        roleType: activeRoleType.value === null ? undefined : activeRoleType.value,
      }
      const res = await request.get('/system/role/page', { params })
      if (res.code === 200) {
        roleList.value = res.data?.records || res.data?.list || []
        if (!roleList.value.some(item => item.id === currentRole.value?.id)) {
          const firstRole = roleList.value[0]
          if (firstRole) {
            await handleSelectRole(firstRole)
          }
          else {
            currentRole.value = {}
            roleUserTotal.value = 0
          }
        }
      }
    }
    catch (error) {
      console.error('加载角色列表失败:', error)
      window.$message.error('加载角色列表失败')
    }
    finally {
      roleListLoading.value = false
    }
  }

  function handleRoleTypeChange(value) {
    activeRoleType.value = value
    roleKeyword.value = ''
    loadRoleList()
  }

  function handleRoleSearch() {
    loadRoleList()
  }

  function handleAddRole() {
    crudRef.value?.showAdd()
  }

  async function handleSelectRole(row) {
    if (!row?.id)
      return
    currentRole.value = row
    roleUserKeyword.value = ''
    userSearchParams.value = {
      userStatus: null,
    }
    roleUserOrgId.value = null
    roleUserTotal.value = resolveRoleMemberCount(row) ?? 0
    try {
      await Promise.all([
        loadRoleOrgTree(row.tenantId),
        loadRoleApplicableOrgIds(row.id),
      ])
      await searchRoleUsers()
    }
    catch (error) {
      console.error('加载角色用户上下文失败:', error)
      window.$message.error('加载角色用户失败')
    }
  }

  async function handleRoleMutationSuccess() {
    await loadRoleList()
    crudRef.value?.refresh()
  }

  // 编辑
  function handleEdit(row) {
    crudRef.value?.showEdit(row)
  }

  // 删除
  function handleDelete(row) {
    window.$dialog.warning({
      title: '确认删除',
      content: `确定要删除角色"${row.roleName}"吗？删除后将无法恢复！`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const res = await request.post('/system/role/remove', null, { params: { id: row.id } })
          if (res.code === 200) {
            window.$message.success('删除成功')
            await handleRoleMutationSuccess()
          }
        }
        catch {
          window.$message.error('删除失败')
        }
      },
    })
  }

  async function handleRoleOrgScope(row) {
    if (!row?.id)
      return
    if (currentRole.value?.id !== row.id)
      await handleSelectRole(row)
    roleOrgModalVisible.value = true
    checkedRoleOrgKeys.value = []
    roleOrgExpandedKeys.value = []
    try {
      roleOrgLoading.value = true
      checkedRoleOrgKeys.value = normalizeNumberList(roleApplicableOrgIds.value)
      roleScopeMode.value = isCurrentRoleGlobalScope.value ? 'global' : 'custom'
    }
    catch (error) {
      console.error('加载角色适用组织失败:', error)
      window.$message.error('加载角色适用组织失败')
    }
    finally {
      roleOrgLoading.value = false
    }
  }

  function toggleRoleOrgExpandAll() {
    roleOrgTreeExpandAll.value = !roleOrgTreeExpandAll.value
    roleOrgExpandedKeys.value = roleOrgTreeExpandAll.value ? getAllKeys(roleOrgTreeData.value) : []
  }

  function handleRoleOrgExpandedKeysChange(keys) {
    roleOrgExpandedKeys.value = keys
  }

  function handleRoleOrgCheckedKeysChange(keys) {
    checkedRoleOrgKeys.value = normalizeNumberList(keys)
  }

  async function handleSubmitRoleOrgs() {
    const nextOrgIds = roleScopeMode.value === 'global'
      ? []
      : normalizeNumberList(checkedRoleOrgKeys.value)
    if (roleScopeMode.value === 'custom' && nextOrgIds.length === 0) {
      window.$message.warning('请至少选择一个适用组织')
      return
    }
    try {
      roleOrgSubmitLoading.value = true
      const res = await request.post(`/system/role/${currentRole.value.id}/orgs`, nextOrgIds)
      if (res.code === 200) {
        window.$message.success('适用组织保存成功')
        roleOrgModalVisible.value = false
        roleApplicableOrgIds.value = nextOrgIds
        currentRole.value.orgScopeType = roleScopeMode.value === 'global'
          ? ROLE_ORG_SCOPE_GLOBAL
          : ROLE_ORG_SCOPE_CUSTOM
        const listRole = roleList.value.find(item => Number(item.id) === Number(currentRole.value.id))
        if (listRole)
          listRole.orgScopeType = currentRole.value.orgScopeType
        if (!isCurrentRoleGlobalScope.value && roleUserOrgId.value && !roleApplicableOrgIds.value.includes(roleUserOrgId.value)) {
          roleUserOrgId.value = null
        }
        await searchRoleUsers()
      }
    }
    catch (error) {
      console.error('保存适用组织失败:', error)
      window.$message.error('保存适用组织失败')
    }
    finally {
      roleOrgSubmitLoading.value = false
    }
  }

  function handleRoleScopeModeChange(value) {
    roleScopeMode.value = value
    if (value === 'global')
      checkedRoleOrgKeys.value = normalizeNumberList(allRoleOrgIds.value)
  }

  // 查看角色用户
  async function handleViewUsers(row) {
    await handleSelectRole(row)
  }

  function beforeLoadRoleUserList(params = {}) {
    const keyword = String(roleUserKeyword.value || '').trim()
    const nextParams = {
      ...params,
      username: keyword || undefined,
      userStatus: userSearchParams.value.userStatus,
      orgId: roleUserOrgId.value || undefined,
    }
    Object.keys(nextParams).forEach((key) => {
      if (nextParams[key] === '' || nextParams[key] === null || nextParams[key] === undefined)
        delete nextParams[key]
    })
    return nextParams
  }

  function handleRoleUserLoadSuccess({ total = 0 } = {}) {
    roleUserTotal.value = total
    if (currentRole.value?.id) {
      roleUserCountMap.value = {
        ...roleUserCountMap.value,
        [currentRole.value.id]: total,
      }
      currentRole.value.userCount = total
      const listRole = roleList.value.find(item => Number(item.id) === Number(currentRole.value.id))
      if (listRole)
        listRole.userCount = total
    }
  }

  async function refreshRoleUsers() {
    if (!currentRole.value?.id) {
      roleUserTotal.value = 0
      return
    }
    await nextTick()
    roleUserCrudRef.value?.refresh?.()
  }

  async function searchRoleUsers() {
    if (!currentRole.value?.id) {
      roleUserTotal.value = 0
      return
    }
    await nextTick()
    roleUserCrudRef.value?.search?.({})
  }

  // 用户搜索
  function handleUserSearch() {
    searchRoleUsers()
  }

  function handleRoleUserOrgChange() {
    searchRoleUsers()
  }

  // 用户搜索重置
  function handleUserSearchReset() {
    roleUserKeyword.value = ''
    userSearchParams.value = {
      userStatus: null,
    }
    searchRoleUsers()
  }

  // 移除角色用户
  async function handleRemoveUserRole(user) {
    const orgId = normalizeSingleNumber(roleUserOrgId.value)
    window.$dialog.warning({
      title: '确认移除',
      content: orgId === null
        ? `确定移除用户“${user.username}”在全部授权组织下的角色“${currentRole.value.roleName}”吗？`
        : `确定移除用户“${user.username}”在当前授权组织下的角色“${currentRole.value.roleName}”吗？`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          if (orgId === null) {
            const res = await request.post('/system/role/removeUserRole', null, {
              params: {
                roleId: currentRole.value.id,
                userId: user.id,
              },
            })
            if (res.code === 200) {
              window.$message.success('移除成功')
              await refreshRoleUsers()
            }
            return
          }
          const currentRes = await request.get(`/system/user/${user.id}/org-roles`, {
            params: {
              tenantId: currentRole.value.tenantId,
              orgId,
            },
          })
          const nextRoleIds = normalizeNumberList(currentRes.code === 200 ? currentRes.data : [])
            .filter(roleId => Number(roleId) !== Number(currentRole.value.id))
          const res = await request.post(`/system/user/${user.id}/org-roles`, {
            tenantId: currentRole.value.tenantId,
            orgId,
            roleIds: nextRoleIds,
          })
          if (res.code === 200) {
            window.$message.success('移除成功')
            await refreshRoleUsers()
          }
        }
        catch (error) {
          console.error('移除用户失败:', error)
          window.$message.error('移除用户失败')
        }
      },
    })
  }

  // 加载角色已授权用户ID列表
  async function loadAssignedUserIds() {
    const orgId = normalizeSingleNumber(roleUserOrgId.value)
    if (orgId === null) {
      assignedUserIds.value = []
      return
    }
    try {
      const res = await request.get(`/system/role/${currentRole.value.id}/users`, {
        params: { pageNum: 1, pageSize: 9999, orgId },
      })
      if (res.code === 200 && res.data) {
        assignedUserIds.value = (res.data.records || []).map(u => u.id)
      }
    }
    catch {
      assignedUserIds.value = []
    }
  }

  // 打开添加用户弹窗
  async function handleAddUser() {
    if (!canAddUserToCurrentRole.value) {
      window.$message.warning('请先选择一个授权组织')
      return
    }
    await loadAssignedUserIds()
    addUserModalVisible.value = true
  }

  // 从角色列表直接添加用户
  async function handleAddUserFromList(row) {
    await handleSelectRole(row)
    if (!canAddUserToCurrentRole.value) {
      window.$message.warning('请先选择一个授权组织')
      return
    }
    await loadAssignedUserIds()
    addUserModalVisible.value = true
  }

  // 确认添加用户到角色
  async function handleConfirmAddUsers(userIds) {
    if (!userIds || userIds.length === 0)
      return
    const orgId = normalizeSingleNumber(roleUserOrgId.value)
    if (orgId === null) {
      window.$message.warning('请选择授权组织')
      return
    }
    try {
      addUserLoading.value = true
      for (const userId of userIds) {
        const currentRes = await request.get(`/system/user/${userId}/org-roles`, {
          params: {
            tenantId: currentRole.value.tenantId,
            orgId,
          },
        })
        const roleIds = Array.from(new Set([
          ...normalizeNumberList(currentRes.code === 200 ? currentRes.data : []),
          normalizeSingleNumber(currentRole.value.id),
        ]))
        const res = await request.post(`/system/user/${userId}/org-roles`, {
          tenantId: currentRole.value.tenantId,
          orgId,
          roleIds,
        })
        if (res.code !== 200) {
          throw new Error(`用户 ${userId} 添加失败`)
        }
      }
      window.$message.success(`成功添加 ${userIds.length} 个用户`)
      addUserModalVisible.value = false
      await refreshRoleUsers()
    }
    catch (error) {
      console.error('添加用户失败:', error)
      window.$message.error('添加用户失败')
    }
    finally {
      addUserLoading.value = false
    }
  }

  // 授权
  async function handleAuth(row) {
    if (!row?.id)
      return
    if (currentRole.value?.id !== row.id)
      await handleSelectRole(row)
    authModalVisible.value = true
    authLoadFailed.value = false
    dataScopeLoadFailed.value = false
    dataScopeSettings.value = createFallbackDataScopeSettings()

    await Promise.all([
      loadClientList(),
      loadRoleDataScopes(),
    ])
    if (!authClientTabs.value.some(item => item.clientCode === currentAuthClientCode.value)) {
      currentAuthClientCode.value = authClientTabs.value[0]?.clientCode || 'pc'
    }
    await loadAuthClientResources()
  }

  // 获取所有节点的 key（用于展开/收起）
  function getAllKeys(list, keys = []) {
    list.forEach((item) => {
      keys.push(item.id)
      if (item.children && item.children.length > 0) {
        getAllKeys(item.children, keys)
      }
    })
    return keys
  }

  // 加载资源树
  async function loadResourceTree() {
    try {
      const res = await request.get('/system/resource/assignable-tree', {
        params: { clientCode: currentAuthClientCode.value },
      })
      if (res.code === 200) {
        resourceTreeData.value = res.data || []
        return
      }
      throw new Error(res.message || '资源树响应异常')
    }
    catch (error) {
      authLoadFailed.value = true
      console.error('加载资源树失败:', error)
      window.$message.error('加载资源树失败')
    }
  }

  // 加载角色已有的资源
  __impl.getRoleDropdownMenuProps = getRoleDropdownMenuProps
  __impl.handleRoleCardAction = handleRoleCardAction
  __impl.toNumberOptions = toNumberOptions
  __impl.normalizeSingleNumber = normalizeSingleNumber
  __impl.normalizeNumberList = normalizeNumberList
  __impl.resolveOptionLabel = resolveOptionLabel
  __impl.resolveRoleTypeShortLabel = resolveRoleTypeShortLabel
  __impl.resolveRoleDictValue = resolveRoleDictValue
  __impl.resolveRoleDataScopeLabel = resolveRoleDataScopeLabel
  __impl.resolveRoleStatusLabel = resolveRoleStatusLabel
  __impl.isRoleDisabled = isRoleDisabled
  __impl.resolveRoleMemberCount = resolveRoleMemberCount
  __impl.resolveUserDisplayName = resolveUserDisplayName
  __impl.resolveUserAccountLabel = resolveUserAccountLabel
  __impl.resolveUserOrgLabel = resolveUserOrgLabel
  __impl.resolveUserStatusLabel = resolveUserStatusLabel
  __impl.isUserEnabled = isUserEnabled
  __impl.flattenOrgNodes = flattenOrgNodes
  __impl.buildRoleUserOrgTreeOptions = buildRoleUserOrgTreeOptions
  __impl.getOrgNodeIcon = getOrgNodeIcon
  __impl.getOrgNodeTone = getOrgNodeTone
  __impl.buildRoleTenantParams = buildRoleTenantParams
  __impl.loadRoleOrgTree = loadRoleOrgTree
  __impl.loadRoleApplicableOrgIds = loadRoleApplicableOrgIds
  __impl.beforeSubmit = beforeSubmit
  __impl.loadRoleList = loadRoleList
  __impl.handleRoleTypeChange = handleRoleTypeChange
  __impl.handleRoleSearch = handleRoleSearch
  __impl.handleAddRole = handleAddRole
  __impl.handleSelectRole = handleSelectRole
  __impl.handleRoleMutationSuccess = handleRoleMutationSuccess
  __impl.handleEdit = handleEdit
  __impl.handleDelete = handleDelete
  __impl.handleRoleOrgScope = handleRoleOrgScope
  __impl.toggleRoleOrgExpandAll = toggleRoleOrgExpandAll
  __impl.handleRoleOrgExpandedKeysChange = handleRoleOrgExpandedKeysChange
  __impl.handleRoleOrgCheckedKeysChange = handleRoleOrgCheckedKeysChange
  __impl.handleSubmitRoleOrgs = handleSubmitRoleOrgs
  __impl.handleRoleScopeModeChange = handleRoleScopeModeChange
  __impl.handleViewUsers = handleViewUsers
  __impl.beforeLoadRoleUserList = beforeLoadRoleUserList
  __impl.handleRoleUserLoadSuccess = handleRoleUserLoadSuccess
  __impl.refreshRoleUsers = refreshRoleUsers
  __impl.searchRoleUsers = searchRoleUsers
  __impl.handleUserSearch = handleUserSearch
  __impl.handleRoleUserOrgChange = handleRoleUserOrgChange
  __impl.handleUserSearchReset = handleUserSearchReset
  __impl.handleRemoveUserRole = handleRemoveUserRole
  __impl.loadAssignedUserIds = loadAssignedUserIds
  __impl.handleAddUser = handleAddUser
  __impl.handleAddUserFromList = handleAddUserFromList
  __impl.handleConfirmAddUsers = handleConfirmAddUsers
  __impl.handleAuth = handleAuth
  __impl.getAllKeys = getAllKeys
  __impl.loadResourceTree = loadResourceTree

  return {
    __impl, mut, beforeLoadRoleUserList, beforeSubmit, buildRoleTenantParams, buildRoleUserOrgTreeOptions,
    flattenOrgNodes, getAllKeys, getOrgNodeIcon, getOrgNodeTone, getRoleActionOptions, getRoleDropdownMenuProps,
    handleAddRole, handleAddUser, handleAddUserFromList, handleAuth, handleConfirmAddUsers, handleDelete, handleEdit,
    handleRemoveUserRole, handleRoleCardAction, handleRoleMutationSuccess, handleRoleOrgCheckedKeysChange,
    handleRoleOrgExpandedKeysChange, handleRoleOrgScope, handleRoleScopeModeChange, handleRoleSearch,
    handleRoleTypeChange, handleRoleUserLoadSuccess, handleRoleUserOrgChange, handleSelectRole, handleSubmitRoleOrgs,
    handleUserSearch, handleUserSearchReset, handleViewUsers, isRoleDisabled, isUserEnabled, loadAssignedUserIds,
    loadResourceTree, loadRoleApplicableOrgIds, loadRoleList, loadRoleOrgTree, normalizeNumberList,
    normalizeSingleNumber, refreshRoleUsers, resolveOptionLabel, resolveRoleDataScopeLabel, resolveRoleDictValue,
    resolveRoleMemberCount, resolveRoleStatusLabel, resolveRoleTypeShortLabel, resolveUserAccountLabel,
    resolveUserDisplayName, resolveUserOrgLabel, resolveUserStatusLabel, searchRoleUsers, toNumberOptions,
    toggleRoleOrgExpandAll, USER_STATUS_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT, YES_NO_DICT,
    crudRef, roleUserCrudRef, userStore, roleList, roleListLoading, roleKeyword, activeRoleType, ROLE_ORG_SCOPE_GLOBAL,
    ROLE_ORG_SCOPE_CUSTOM, authModalVisible, authLoading, authLoadFailed, authSubmitLoading, resourceTreeData,
    checkedResourceKeys, dataScopeLoading, dataScopeLoadFailed, dataScopeSettings, clientList, currentAuthClientCode,
    currentRole, addUserModalVisible, addUserLoading, assignedUserIds, roleUserOrgId, roleUserKeyword,
    roleApplicableOrgIds, roleOrgTreeData, roleUserTotal, roleUserCountMap, userSearchParams, roleOrgModalVisible,
    roleOrgLoading, roleOrgSubmitLoading, roleScopeMode, checkedRoleOrgKeys, roleOrgExpandedKeys, roleOrgTreeExpandAll,
    userStatusOptions, dataScopeOptions, manageableDataScopeOptions, roleTypeOptions, roleStatusOptions, yesNoOptions,
    roleTypeTabs, isCurrentRoleGlobalScope, roleUserOrgOptions, roleUserOrgTreeOptions, allRoleOrgIds,
    currentRoleScopeLabel, currentRoleScopeTagType, currentRoleDataScopeLabel, canAddUserToCurrentRole,
    addUserButtonText, roleUserApiConfig, roleUserTableColumns, roleOrgScopeSummary, roleOrgScopeTagType,
    authClientTabs, currentAuthClientName, searchSchema, tableColumns, editSchema,
  }
}
