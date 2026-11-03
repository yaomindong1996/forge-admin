import { computed, h, watch } from 'vue'
import { request } from '@/utils'
import { orderRolesWithCurrentFirst } from '@/views/system/user-role-order'
import { convertOrgToTreeSelect, formatUserOrgBindingLabel, isSameKey, normalizeNumberList, normalizeSingleNumber, renderDictTag, resolveRoleDictValue } from './utils'

// 共享状态由 Pinia 创建；跨领域调用通过同一工作台实例协调。
export function createUserRoles(state, runtime, workspace) {
  const { selectedOrgNode, relationModalVisible, authLoading, authSubmitLoading, batchAuthModalVisible, batchAuthSubmitLoading, batchAuthTenantId, batchAuthOrgId, batchAuthOrgTreeData, currentUser, roleTableData, roleSearchKeyword, checkedRoleKeys, authOrgId, currentUserOrgBindings, rolePermissionModalVisible, currentPermissionRole, rolePagination, tenantOptions } = state
  const { userStore, crudRef } = runtime
  const orderedRoleTableData = computed(() => orderRolesWithCurrentFirst(roleTableData.value, checkedRoleKeys.value))

  const authOrgOptions = computed(() => currentUserOrgBindings.value
    .map(item => ({
      label: formatUserOrgBindingLabel(item),
      value: normalizeSingleNumber(item.orgId),
    }))
    .filter(item => item.value !== null))

  const batchAuthOrgTreeOptions = computed(() => convertOrgToTreeSelect(batchAuthOrgTreeData.value))

  const rolePaginationConfig = computed(() => ({
    page: rolePagination.value.page,
    pageSize: rolePagination.value.pageSize,
    itemCount: rolePagination.value.itemCount,
    showSizePicker: true,
    pageSizes: [10, 20, 50, 100],
  }))

  const authRoleColumns = computed(() => [
    {
      type: 'selection',
      multiple: true,
      width: 48,
    },
    {
      title: '角色名称',
      key: 'roleName',
      minWidth: 150,
    },
    {
      title: '权限字符',
      key: 'roleKey',
      minWidth: 140,
    },
    {
      title: '角色类型',
      key: 'roleType',
      width: 110,
      render: row => renderDictTag(
        workspace.roleTypeOptions.value,
        resolveRoleDictValue(row, 'roleType'),
        'role-type-tag',
      ),
    },
    {
      title: '数据范围',
      key: 'dataScope',
      width: 140,
      render: row => renderDictTag(
        workspace.roleDataScopeOptions.value,
        resolveRoleDictValue(row, 'dataScope'),
        'role-data-scope-tag',
      ),
    },
    {
      title: '状态',
      key: 'roleStatus',
      width: 90,
      render: row => renderDictTag(
        workspace.normalDisableOptions.value,
        resolveRoleDictValue(row, 'roleStatus'),
        'role-status-tag',
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      // 文字链操作：避免动态 material-symbols class 未进 safelist 导致空图标
      render: row => h(
        'a',
        {
          class: 'text-primary cursor-pointer hover:text-primary-hover',
          role: 'button',
          tabindex: 0,
          onClick: (event) => {
            event.preventDefault()
            event.stopPropagation()
            handleRolePermission(row)
          },
          onKeydown: (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault()
              handleRolePermission(row)
            }
          },
        },
        '权限配置',
      ),
    },
  ])

  async function handleOpenBatchAuth() {
    const userIds = workspace.getSelectedBatchUserIds()
    if (userIds.length === 0) {
      return
    }

    if (tenantOptions.value.length === 0) {
      await workspace.loadTenantOptions()
    }

    batchAuthTenantId.value = workspace.resolveDefaultBatchTenantId()
    if (!batchAuthTenantId.value) {
      window.$message.warning('请选择授权租户')
      return
    }

    batchAuthModalVisible.value = true
    batchAuthOrgId.value = null
    batchAuthOrgTreeData.value = []
    roleSearchKeyword.value = ''
    checkedRoleKeys.value = []
    rolePagination.value.page = 1
    await loadBatchAuthOrgTree(batchAuthTenantId.value)
    batchAuthOrgId.value = selectedOrgNode.value?.id || workspace.resolvePreferredOrgIdFromTree(batchAuthOrgTreeData.value)
    if (!batchAuthOrgId.value) {
      window.$message.warning('请选择授权组织')
      return
    }
    await loadRoleList(batchAuthTenantId.value, batchAuthOrgId.value)
  }

  async function loadUserOrgBindings(userId, tenantId = workspace.resolveOperationTenantId()) {
    try {
      const res = await request.get(`/system/user/${userId}/org-bindings`, {
        params: workspace.buildTenantParams(tenantId),
      })
      if (res.code === 200) {
        currentUserOrgBindings.value = res.data || []
        return
      }
    }
    catch (error) {
      console.error('加载用户组织绑定详情失败:', error)
    }
    currentUserOrgBindings.value = []
  }

  function resolveDefaultAuthOrgId() {
    const activeOrgId = normalizeSingleNumber(userStore.activeOrgId || userStore.userInfo?.activeOrgId)
    if (activeOrgId !== null && currentUserOrgBindings.value.some(item => isSameKey(item.orgId, activeOrgId))) {
      return activeOrgId
    }

    const mainOrg = currentUserOrgBindings.value.find(item => Number(item.isMain) === 1)
    if (mainOrg) {
      return normalizeSingleNumber(mainOrg.orgId)
    }

    return normalizeSingleNumber(currentUserOrgBindings.value[0]?.orgId)
  }

  async function loadBatchAuthOrgTree(tenantId) {
    try {
      batchAuthOrgTreeData.value = await workspace.loadCompleteOrgTree(tenantId)
    }
    catch (error) {
      console.error('加载批量授权组织失败:', error)
      batchAuthOrgTreeData.value = []
    }
  }

  async function loadRoleList(tenantId = workspace.resolveOperationTenantId(), orgId = authOrgId.value) {
    try {
      authLoading.value = true
      const res = await request.get('/system/role/page', {
        params: {
          pageNum: rolePagination.value.page,
          pageSize: rolePagination.value.pageSize,
          roleName: roleSearchKeyword.value || undefined,
          orgId: normalizeSingleNumber(orgId) || undefined,
          prioritizedUserId: batchAuthModalVisible.value ? undefined : currentUser.value?.id,
          ...workspace.buildTenantParams(tenantId),
        },
      })
      if (res.code === 200) {
        roleTableData.value = res.data.list || res.data.records || []
        rolePagination.value.itemCount = Number(res.data.total || 0)
      }
    }
    catch (error) {
      console.error('加载角色列表失败:', error)
      window.$message.error('加载角色列表失败')
    }
    finally {
      authLoading.value = false
    }
  }

  async function loadUserRoles(userId, orgId = authOrgId.value) {
    const normalizedOrgId = normalizeSingleNumber(orgId)
    if (!normalizedOrgId) {
      checkedRoleKeys.value = []
      return
    }
    try {
      authLoading.value = true
      const res = await request.get(`/system/user/${userId}/org-roles`, {
        params: {
          ...workspace.buildTenantParams(workspace.resolveOperationTenantId()),
          orgId: normalizedOrgId,
        },
      })
      if (res.code === 200) {
        checkedRoleKeys.value = normalizeNumberList(res.data || [])
      }
    }
    catch (error) {
      console.error('加载用户角色失败:', error)
      window.$message.error('加载用户角色失败')
    }
    finally {
      authLoading.value = false
    }
  }

  function handleCheckedKeysChange(keys) {
    checkedRoleKeys.value = normalizeNumberList(keys || [])
  }

  function handleRolePermission(row) {
    currentPermissionRole.value = row
    rolePermissionModalVisible.value = true
  }

  function handleRoleSearch() {
    rolePagination.value.page = 1
    loadRoleList(
      batchAuthModalVisible.value ? batchAuthTenantId.value : workspace.resolveOperationTenantId(),
      batchAuthModalVisible.value ? batchAuthOrgId.value : authOrgId.value,
    )
  }

  function handleRolePageChange(page) {
    rolePagination.value.page = page
    loadRoleList(
      batchAuthModalVisible.value ? batchAuthTenantId.value : workspace.resolveOperationTenantId(),
      batchAuthModalVisible.value ? batchAuthOrgId.value : authOrgId.value,
    )
  }

  function handleRolePageSizeChange(pageSize) {
    rolePagination.value.pageSize = pageSize
    rolePagination.value.page = 1
    loadRoleList(
      batchAuthModalVisible.value ? batchAuthTenantId.value : workspace.resolveOperationTenantId(),
      batchAuthModalVisible.value ? batchAuthOrgId.value : authOrgId.value,
    )
  }

  function handleCheckAll() {
    const currentPageKeys = normalizeNumberList(roleTableData.value.map(role => role.id))
    checkedRoleKeys.value = Array.from(new Set([...checkedRoleKeys.value, ...currentPageKeys]))
  }

  function handleUncheckAll() {
    checkedRoleKeys.value = []
  }

  async function handleSubmitAuth() {
    const orgId = normalizeSingleNumber(authOrgId.value)
    if (!orgId) {
      window.$message.warning('请选择授权组织')
      return
    }
    try {
      authSubmitLoading.value = true
      const res = await request.post(
        `/system/user/${currentUser.value.id}/org-roles`,
        {
          orgId,
          roleIds: normalizeNumberList(checkedRoleKeys.value),
          tenantId: workspace.resolveOperationTenantId(),
        },
      )
      if (res.code === 200) {
        window.$message.success('角色授权成功')
        relationModalVisible.value = false
        crudRef.value?.refresh()
      }
    }
    catch (error) {
      console.error('授权失败:', error)
      window.$message.error('授权失败')
    }
    finally {
      authSubmitLoading.value = false
    }
  }

  async function handleSubmitBatchAuth() {
    const userIds = workspace.getSelectedBatchUserIds()
    if (userIds.length === 0) {
      return
    }
    const tenantId = normalizeSingleNumber(batchAuthTenantId.value)
    if (tenantId === null) {
      window.$message.warning('请选择授权租户')
      return
    }
    const orgId = normalizeSingleNumber(batchAuthOrgId.value)
    if (orgId === null) {
      window.$message.warning('请选择授权组织')
      return
    }
    const roleIds = normalizeNumberList(checkedRoleKeys.value)
    if (roleIds.length === 0) {
      window.$message.warning('请至少选择一个角色')
      return
    }

    try {
      batchAuthSubmitLoading.value = true
      for (const userId of userIds) {
        const currentRes = await request.get(`/system/user/${userId}/org-roles`, {
          params: { tenantId, orgId },
        })
        const mergedRoleIds = Array.from(new Set([
          ...normalizeNumberList(currentRes.code === 200 ? currentRes.data : []),
          ...roleIds,
        ]))
        const res = await request.post(`/system/user/${userId}/org-roles`, {
          tenantId,
          orgId,
          roleIds: mergedRoleIds,
        })
        if (res.code !== 200) {
          throw new Error(`用户 ${userId} 授权失败`)
        }
      }
      window.$message.success('批量授权成功')
      batchAuthModalVisible.value = false
      checkedRoleKeys.value = []
      workspace.clearBatchSelection()
      crudRef.value?.refresh()
    }
    catch (error) {
      console.error('批量授权失败:', error)
      window.$message.error('批量授权失败')
    }
    finally {
      batchAuthSubmitLoading.value = false
    }
  }

  watch(authOrgId, (orgId, previousOrgId) => {
    if (!relationModalVisible.value || isSameKey(orgId, previousOrgId)) {
      return
    }
    checkedRoleKeys.value = []
    rolePagination.value.page = 1
    loadRoleList(workspace.resolveOperationTenantId(), orgId)
    loadUserRoles(currentUser.value.id, orgId)
  })

  watch(batchAuthTenantId, async (tenantId, previousTenantId) => {
    if (!batchAuthModalVisible.value || isSameKey(tenantId, previousTenantId)) {
      return
    }
    checkedRoleKeys.value = []
    batchAuthOrgId.value = null
    rolePagination.value.page = 1
    await loadBatchAuthOrgTree(tenantId)
    batchAuthOrgId.value = workspace.resolvePreferredOrgIdFromTree(batchAuthOrgTreeData.value)
    if (batchAuthOrgId.value) {
      loadRoleList(tenantId, batchAuthOrgId.value)
    }
  })

  watch(batchAuthOrgId, (orgId, previousOrgId) => {
    if (!batchAuthModalVisible.value || isSameKey(orgId, previousOrgId)) {
      return
    }
    checkedRoleKeys.value = []
    rolePagination.value.page = 1
    if (orgId) {
      loadRoleList(batchAuthTenantId.value, orgId)
    }
  })

  return { orderedRoleTableData, authOrgOptions, batchAuthOrgTreeOptions, rolePaginationConfig, authRoleColumns, handleOpenBatchAuth, loadUserOrgBindings, resolveDefaultAuthOrgId, loadBatchAuthOrgTree, loadRoleList, loadUserRoles, handleCheckedKeysChange, handleRolePermission, handleRoleSearch, handleRolePageChange, handleRolePageSizeChange, handleCheckAll, handleUncheckAll, handleSubmitAuth, handleSubmitBatchAuth }
}
