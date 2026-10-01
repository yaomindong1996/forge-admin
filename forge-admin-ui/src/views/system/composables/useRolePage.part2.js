/** role.vue setup part 2. */
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
export function applyRolePagePart2(deps = {}) {
  const {
    __impl, mut, beforeLoadRoleUserList, beforeSubmit, buildRoleTenantParams, buildRoleUserOrgTreeOptions, flattenOrgNodes, getAllKeys,
    getOrgNodeIcon, getOrgNodeTone, getRoleActionOptions, getRoleDropdownMenuProps, handleAddRole, handleAddUser, handleAddUserFromList, handleAuth,
    handleConfirmAddUsers, handleDelete, handleEdit, handleRemoveUserRole, handleRoleCardAction, handleRoleMutationSuccess, handleRoleOrgCheckedKeysChange, handleRoleOrgExpandedKeysChange,
    handleRoleOrgScope, handleRoleScopeModeChange, handleRoleSearch, handleRoleTypeChange, handleRoleUserLoadSuccess, handleRoleUserOrgChange, handleSelectRole, handleSubmitRoleOrgs,
    handleUserSearch, handleUserSearchReset, handleViewUsers, isRoleDisabled, isUserEnabled, loadAssignedUserIds, loadResourceTree, loadRoleApplicableOrgIds,
    loadRoleList, loadRoleOrgTree, normalizeNumberList, normalizeSingleNumber, refreshRoleUsers, resolveOptionLabel, resolveRoleDataScopeLabel, resolveRoleDictValue,
    resolveRoleMemberCount, resolveRoleStatusLabel, resolveRoleTypeShortLabel, resolveUserAccountLabel, resolveUserDisplayName, resolveUserOrgLabel, resolveUserStatusLabel, searchRoleUsers,
    toNumberOptions, toggleRoleOrgExpandAll, USER_STATUS_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT, YES_NO_DICT, crudRef,
    roleUserCrudRef, userStore, roleList, roleListLoading, roleKeyword, activeRoleType, ROLE_ORG_SCOPE_GLOBAL, ROLE_ORG_SCOPE_CUSTOM,
    authModalVisible, authLoading, authLoadFailed, authSubmitLoading, resourceTreeData, checkedResourceKeys, dataScopeLoading, dataScopeLoadFailed,
    dataScopeSettings, clientList, currentAuthClientCode, currentRole, addUserModalVisible, addUserLoading, assignedUserIds, roleUserOrgId,
    roleUserKeyword, roleApplicableOrgIds, roleOrgTreeData, roleUserTotal, roleUserCountMap, userSearchParams, roleOrgModalVisible, roleOrgLoading,
    roleOrgSubmitLoading, roleScopeMode, checkedRoleOrgKeys, roleOrgExpandedKeys, roleOrgTreeExpandAll, userStatusOptions, dataScopeOptions, manageableDataScopeOptions,
    roleTypeOptions, roleStatusOptions, yesNoOptions, roleTypeTabs, isCurrentRoleGlobalScope, roleUserOrgOptions, roleUserOrgTreeOptions, allRoleOrgIds,
    currentRoleScopeLabel, currentRoleScopeTagType, currentRoleDataScopeLabel, canAddUserToCurrentRole, addUserButtonText, roleUserApiConfig, roleUserTableColumns, roleOrgScopeSummary,
    roleOrgScopeTagType, authClientTabs, currentAuthClientName, searchSchema, tableColumns, editSchema,
  } = deps
  async function loadRoleResources(roleId) {
    try {
      const res = await request.get(`/system/role/${roleId}/resources`, {
        params: { clientCode: currentAuthClientCode.value, includeParents: true },
      })
      if (res.code === 200) {
        checkedResourceKeys.value = filterAssignableCheckedKeys(res.data, resourceTreeData.value)
        return
      }
      throw new Error(res.message || '角色资源响应异常')
    }
    catch (error) {
      authLoadFailed.value = true
      console.error('加载角色资源失败:', error)
      window.$message.error('加载角色资源失败')
    }
  }

  /**
   * 回填角色已授权资源时只保留授权树中真实存在的非目录资源：
   * 目录由后端按已选页面自动补齐，若把历史绑定中的“孤儿目录”（其下页面已全部取消）
   * 回填并原样全量提交，会让角色反复出现点开为空的菜单入口。
   * 资源树不可用时回退为原始列表，避免误清空回填数据。
   */
  function filterAssignableCheckedKeys(rawIds, tree) {
    const list = Array.isArray(rawIds) ? rawIds : []
    if (!Array.isArray(tree) || tree.length === 0)
      return list
    const assignableIds = new Set()
    const walk = (nodes) => {
      for (const node of nodes || []) {
        if (Number(node.resourceType) !== 1)
          assignableIds.add(String(node.id))
        if (node.children && node.children.length > 0)
          walk(node.children)
      }
    }
    walk(tree)
    return list.filter(id => assignableIds.has(String(id)))
  }

  async function loadClientList() {
    try {
      const res = await request.get('/system/client/list')
      if (res.code === 200) {
        clientList.value = res.data || []
      }
    }
    catch (error) {
      console.error('加载客户端列表失败:', error)
    }
  }

  async function loadAuthClientResources() {
    authLoading.value = true
    authLoadFailed.value = false
    checkedResourceKeys.value = []
    resourceTreeData.value = []
    try {
      // 顺序执行：回填勾选依赖资源树剔除目录类资源
      await loadResourceTree()
      await loadRoleResources(currentRole.value.id)
    }
    finally {
      authLoading.value = false
    }
  }

  async function loadRoleDataScopes() {
    dataScopeLoading.value = true
    dataScopeLoadFailed.value = false
    try {
      const res = await request.get(`/system/role/${currentRole.value.id}/dataScopes`)
      if (res.code === 200) {
        applyRoleDataScopeSettings(res.data)
        return
      }
      dataScopeLoadFailed.value = true
      dataScopeSettings.value = createFallbackDataScopeSettings()
    }
    catch (error) {
      dataScopeLoadFailed.value = true
      dataScopeSettings.value = createFallbackDataScopeSettings()
      console.warn('当前后端未提供角色数据权限明细接口，已按角色数据范围降级展示:', error)
    }
    finally {
      dataScopeLoading.value = false
    }
  }

  async function handleAuthClientChange(clientCode) {
    currentAuthClientCode.value = clientCode
    await loadAuthClientResources()
  }

  function createFallbackDataScopeSettings() {
    return {
      defaultDataScope: Number(currentRole.value?.dataScope) || 5,
      modules: [],
    }
  }

  function normalizeRoleDataScopeSettings(settings = {}) {
    const defaultDataScope = Number(settings.defaultDataScope) || Number(currentRole.value?.dataScope) || 5
    return {
      defaultDataScope,
      modules: (settings.modules || []).map(module => ({
        ...module,
        dataScope: module.dataScope == null ? null : Number(module.dataScope),
        effectiveDataScope: Number(module.effectiveDataScope ?? defaultDataScope) || defaultDataScope,
      })),
    }
  }

  function applyRoleDataScopeSettings(settings = {}) {
    const normalized = normalizeRoleDataScopeSettings(settings)
    dataScopeSettings.value = normalized
    if (currentRole.value)
      currentRole.value.dataScope = normalized.defaultDataScope
  }

  // 提交授权
  async function handleSubmitAuth() {
    if (authLoading.value || dataScopeLoading.value || authLoadFailed.value || dataScopeLoadFailed.value) {
      window.$message.error('权限配置尚未完整加载，请关闭弹窗后重试')
      return
    }

    try {
      authSubmitLoading.value = true
      const res = await request.post(
        `/system/role/${currentRole.value.id}/resources`,
        checkedResourceKeys.value,
        { params: { clientCode: currentAuthClientCode.value } },
      )
      if (res.code === 200) {
        await saveRoleDataScopesIfSupported()
        window.$message.success('角色权限配置已保存')
        authModalVisible.value = false
        crudRef.value?.refresh()
      }
      else {
        throw new Error(res.message || '功能权限保存失败')
      }
    }
    catch (error) {
      console.error('角色权限配置保存失败:', error)
      window.$message.error(error?.message || '保存失败')
    }
    finally {
      authSubmitLoading.value = false
    }
  }

  async function saveRoleDataScopesIfSupported() {
    const dataScopeRes = await request.post(`/system/role/${currentRole.value.id}/dataScopes`, {
      defaultDataScope: dataScopeSettings.value.defaultDataScope,
      moduleScopes: dataScopeSettings.value.modules.map(module => ({
        moduleCode: module.moduleCode,
        dataScope: module.dataScope,
      })),
    })
    if (dataScopeRes.code !== 200)
      throw new Error(dataScopeRes.message || '数据权限保存失败')
    applyRoleDataScopeSettings(dataScopeRes.data)
  }

  onMounted(() => {
    loadRoleList()
  })
  __impl.loadRoleResources = loadRoleResources
  __impl.filterAssignableCheckedKeys = filterAssignableCheckedKeys
  __impl.loadClientList = loadClientList
  __impl.loadAuthClientResources = loadAuthClientResources
  __impl.loadRoleDataScopes = loadRoleDataScopes
  __impl.handleAuthClientChange = handleAuthClientChange
  __impl.createFallbackDataScopeSettings = createFallbackDataScopeSettings
  __impl.normalizeRoleDataScopeSettings = normalizeRoleDataScopeSettings
  __impl.applyRoleDataScopeSettings = applyRoleDataScopeSettings
  __impl.handleSubmitAuth = handleSubmitAuth
  __impl.saveRoleDataScopesIfSupported = saveRoleDataScopesIfSupported

  return {
    ...deps, applyRoleDataScopeSettings, createFallbackDataScopeSettings, filterAssignableCheckedKeys,
    handleAuthClientChange, handleSubmitAuth, loadAuthClientResources, loadClientList, loadRoleDataScopes,
    loadRoleResources, normalizeRoleDataScopeSettings, saveRoleDataScopesIfSupported,
  }
}
