import { ref } from 'vue'

export function createUserManagementState() {
  const selectedUserIds = ref([])
  const leftOrgTreeData = ref([])
  const leftOrgTreeLoading = ref(false)
  const leftOrgExpandAll = ref(false)
  const leftOrgExpandedKeys = ref([])
  const leftOrgPanelCollapsed = ref(false)
  const selectedOrgKeys = ref([])
  const selectedOrgNode = ref(null)
  const isShowAllUsers = ref(true)
  const relationModalVisible = ref(false)
  const relationActiveTab = ref('overview')
  const relationSubmitLoading = ref(false)
  const authLoading = ref(false)
  const authSubmitLoading = ref(false)
  const batchAuthModalVisible = ref(false)
  const batchAuthSubmitLoading = ref(false)
  const batchAuthTenantId = ref(null)
  const batchAuthOrgId = ref(null)
  const batchAuthOrgTreeData = ref([])
  const currentUser = ref({})
  const roleTableData = ref([])
  const roleSearchKeyword = ref('')
  const editingUserTenantId = ref(null)
  const checkedRoleKeys = ref([])
  const authOrgId = ref(null)
  const currentUserOrgBindings = ref([])
  const rolePermissionModalVisible = ref(false)
  const currentPermissionRole = ref({})
  const rolePagination = ref({
    page: 1,
    pageSize: 10,
    itemCount: 0,
  })
  const resetPwdModalVisible = ref(false)
  const resetPwdLoading = ref(false)
  const resetPwdForm = ref({
    id: null,
    password: '',
  })
  const orgLoading = ref(false)
  const orgSubmitLoading = ref(false)
  const orgTreeData = ref([])
  const mainOrgId = ref(null)
  const checkedOrgKeys = ref([])
  const orgTreeExpandAll = ref(true)
  const orgTreeExpandedKeys = ref([])
  const postLoading = ref(false)
  const postSubmitLoading = ref(false)
  const postList = ref([])
  const checkedPostKeys = ref([])
  const mainPostId = ref(null)
  const tenantLoading = ref(false)
  const tenantSubmitLoading = ref(false)
  const tenantOptions = ref([])
  const checkedTenantKeys = ref([])
  const defaultTenantId = ref(null)
  // 高级关系维护当前读写的操作租户（可与行上默认租户、管理员登录租户不同）
  const relationOperationTenantId = ref(null)
  const batchTenantModalVisible = ref(false)
  const batchTenantSubmitLoading = ref(false)
  const batchTenantForm = ref({
    tenantId: null,
    memberType: 2,
  })
  const searchRegionOptions = ref([])
  const editRegionOptions = ref([])
  const dict = ref({})
  return { selectedUserIds, leftOrgTreeData, leftOrgTreeLoading, leftOrgExpandAll, leftOrgExpandedKeys, leftOrgPanelCollapsed, selectedOrgKeys, selectedOrgNode, isShowAllUsers, relationModalVisible, relationActiveTab, relationSubmitLoading, authLoading, authSubmitLoading, batchAuthModalVisible, batchAuthSubmitLoading, batchAuthTenantId, batchAuthOrgId, batchAuthOrgTreeData, currentUser, roleTableData, roleSearchKeyword, editingUserTenantId, checkedRoleKeys, authOrgId, currentUserOrgBindings, rolePermissionModalVisible, currentPermissionRole, rolePagination, resetPwdModalVisible, resetPwdLoading, resetPwdForm, orgLoading, orgSubmitLoading, orgTreeData, mainOrgId, checkedOrgKeys, orgTreeExpandAll, orgTreeExpandedKeys, postLoading, postSubmitLoading, postList, checkedPostKeys, mainPostId, tenantLoading, tenantSubmitLoading, tenantOptions, checkedTenantKeys, defaultTenantId, relationOperationTenantId, batchTenantModalVisible, batchTenantSubmitLoading, batchTenantForm, searchRegionOptions, editRegionOptions, dict }
}
