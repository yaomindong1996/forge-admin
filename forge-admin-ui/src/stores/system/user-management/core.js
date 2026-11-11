import { computed } from 'vue'
import { flattenOrgNodes, isSameKey, NORMAL_DISABLE_DICT, normalizeNumberList, normalizeSingleNumber, resolveTenantIds, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, toNumberOptions, USER_SEX_DICT, USER_STATUS_DICT, USER_TYPE_DICT } from './utils'

// 共享状态由 Pinia 创建；跨领域调用通过同一工作台实例协调。
export function createUserCore(state, runtime, workspace) {
  const { selectedUserIds, currentUser, dict } = state
  const { userStore, crudRef } = runtime
  function resolvePreferredOrgIdFromTree(list = []) {
    const flat = flattenOrgNodes(list)
    const activeOrgId = normalizeSingleNumber(userStore.activeOrgId || userStore.userInfo?.activeOrgId)
    if (activeOrgId !== null && flat.some(item => isSameKey(item.id, activeOrgId))) {
      return activeOrgId
    }
    return normalizeSingleNumber(flat[0]?.id)
  }

  function resolveSingleTenantId(data = {}, fallbackTenantId = null) {
    const tenantIds = resolveTenantIds(data, fallbackTenantId)
    if (tenantIds.length === 0) {
      return normalizeSingleNumber(fallbackTenantId)
    }

    const tenantId = normalizeSingleNumber(data.tenantId)
    if (tenantId !== null && tenantIds.includes(tenantId)) {
      return tenantId
    }

    const preferredTenantId = normalizeSingleNumber(fallbackTenantId)
    if (preferredTenantId !== null && tenantIds.includes(preferredTenantId)) {
      return preferredTenantId
    }

    const currentTenantId = normalizeSingleNumber(userStore.userInfo?.tenantId)
    if (currentTenantId !== null && tenantIds.includes(currentTenantId)) {
      return currentTenantId
    }

    return tenantIds[0]
  }

  function normalizeUserFormData(data = {}, fallbackTenantId = null) {
    const next = { ...(data || {}) }
    next.tenantIds = resolveTenantIds(next, fallbackTenantId)
    next.tenantId = resolveSingleTenantId(next, fallbackTenantId)
    next.userType = normalizeSingleNumber(next.userType, 2)
    next.gender = normalizeSingleNumber(next.gender, 0)
    next.userStatus = normalizeSingleNumber(next.userStatus, 1)
    if (next.roleIds !== null && next.roleIds !== undefined) {
      next.roleIds = normalizeNumberList(next.roleIds)
    }
    if (next.postIds !== null && next.postIds !== undefined) {
      next.postIds = normalizeNumberList(next.postIds)
    }
    if (next.orgIds !== null && next.orgIds !== undefined) {
      next.orgIds = normalizeNumberList(next.orgIds)
    }
    if (next.mainOrgId !== null && next.mainOrgId !== undefined) {
      next.mainOrgId = normalizeSingleNumber(next.mainOrgId)
    }
    if (next.mainOrgId === null && next.orgIds?.length > 0) {
      next.mainOrgId = next.orgIds[0]
    }
    // allowedClients: 后端逗号分隔字符串 → 前端数组
    if (typeof next.allowedClients === 'string' && next.allowedClients.trim()) {
      next.allowedClients = next.allowedClients.split(',').map(s => s.trim()).filter(Boolean)
    }
    else if (!Array.isArray(next.allowedClients)) {
      next.allowedClients = []
    }
    return next
  }

  function isCurrentLoginUser(targetUserId) {
    return targetUserId != null && isSameKey(targetUserId, userStore.userId)
  }

  // 当前登录用户保护：防止非管理员误锁自己；系统管理员不受此限制。
  function isRestrictedCurrentUser(targetUserId) {
    return !userStore.isAdmin && isCurrentLoginUser(targetUserId)
  }

  function getSelectedBatchUserIds() {
    const ids = normalizeNumberList(selectedUserIds.value.length > 0 ? selectedUserIds.value : crudRef.value?.getSelectedKeys?.())
    if (ids.length === 0) {
      window.$message.warning('请先选择用户')
      return []
    }
    if (ids.some(id => isRestrictedCurrentUser(id))) {
      window.$message.warning('批量操作不能包含当前登录用户')
      return []
    }
    return ids
  }

  function resolveDefaultBatchTenantId() {
    const searchTenantId = normalizeSingleNumber(crudRef.value?.getSearchParams?.()?.tenantId)
    if (userStore.isAdmin && searchTenantId !== null) {
      return searchTenantId
    }

    const currentTenantId = normalizeSingleNumber(userStore.userInfo?.tenantId)
    if (currentTenantId !== null) {
      return currentTenantId
    }

    return workspace.tenantSelectOptions.value[0]?.value || null
  }

  // 必须吃入操作租户；忽略参数会导致高级关系维护始终读写管理员登录租户，
  // 在目标租户看起来「已授权」却登录无菜单，删默认租户后概览也会误显示关系全空。
  function buildTenantParams(tenantId) {
    const resolvedTenantId = tenantId ?? userStore.userInfo?.tenantId
    return resolvedTenantId ? { tenantId: resolvedTenantId } : {}
  }

  function resolveOperationTenantId(row = currentUser.value) {
    return row?.tenantId
      || (userStore.isAdmin ? crudRef.value?.getSearchParams?.()?.tenantId : null)
      || userStore.userInfo?.tenantId
  }

  const userTypeOptions = computed(() => toNumberOptions(dict.value[USER_TYPE_DICT]))

  const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))

  const genderOptions = computed(() => toNumberOptions(dict.value[USER_SEX_DICT]))

  const roleTypeOptions = computed(() => toNumberOptions(dict.value[ROLE_TYPE_DICT]))

  const roleDataScopeOptions = computed(() => toNumberOptions(dict.value[ROLE_DATA_SCOPE_DICT]))

  const normalDisableOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))

  return { resolvePreferredOrgIdFromTree, resolveSingleTenantId, normalizeUserFormData, isCurrentLoginUser, isRestrictedCurrentUser, getSelectedBatchUserIds, resolveDefaultBatchTenantId, buildTenantParams, resolveOperationTenantId, userTypeOptions, userStatusOptions, genderOptions, roleTypeOptions, roleDataScopeOptions, normalDisableOptions }
}
