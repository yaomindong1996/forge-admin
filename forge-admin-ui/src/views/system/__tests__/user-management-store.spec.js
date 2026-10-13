import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, reactive } from 'vue'
import { useUserManagementStore } from '@/stores/system/userManagementStore'

const mocks = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), login: null }))
vi.mock('@/utils', () => ({ request: { get: mocks.get, post: mocks.post } }))
vi.mock('@/store', () => ({ useUserStore: () => mocks.login }))

let store
let pinia
let list
beforeEach(() => {
  vi.clearAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
  mocks.login = reactive({ userId: 9, isAdmin: false, activeOrgId: 10, userInfo: { tenantId: 1 } })
  mocks.get.mockResolvedValue({ code: 200, data: [] })
  mocks.post.mockResolvedValue({ code: 200 })
  window.$message = { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  window.$dialog = { warning: vi.fn() }
  list = { refresh: vi.fn(), clearSelection: vi.fn(), showEdit: vi.fn(), showDetail: vi.fn(), getSearchParams: () => ({}), getSelectedKeys: () => [] }
  store = useUserManagementStore()
  store.attachList(list)
})
afterEach(() => {
  store.$dispose()
  vi.restoreAllMocks()
})

describe('用户管理拆分行为契约', () => {
  it('组织选择、全部用户与筛选清除联动列表', () => {
    store.leftOrgTreeData = [{ id: 10, orgName: '测试组织' }]
    store.handleOrgNodeSelect([10])
    expect(store.beforeLoadList({ pageNum: 1 })).toEqual({ pageNum: 1, tenantId: 1, orgId: 10 })
    store.handleClearOrgFilter()
    expect(store.selectedOrgKeys).toEqual([])
    expect(store.beforeLoadList({})).toEqual({ tenantId: 1 })
    expect(list.refresh).toHaveBeenCalledTimes(2)
  })

  it('新增默认组织与编辑回显保持数字 ID 和客户端转换', () => {
    store.selectedOrgNode = { id: '20' }
    expect(store.beforeRenderUserForm({})).toMatchObject({ tenantId: 1, orgIds: [20], mainOrgId: 20, userType: 2 })
    expect(store.beforeRenderUserForm({ id: 8, orgIds: ['30'], mainOrgId: '30', allowedClients: 'web, app' }))
      .toMatchObject({ orgIds: [30], mainOrgId: 30, allowedClients: ['web', 'app'] })
  })

  it('提交强制登录租户，普通管理员不能提升用户类型', () => {
    const data = store.beforeSubmit({ userType: 0, tenantId: 99, orgIds: ['20', 20], roleIds: ['3'], allowedClients: ['web', 'app'] })
    expect(data).toMatchObject({ userType: 2, tenantId: 1, tenantIds: [1], orgIds: [20], mainOrgId: 20, roleIds: [3], allowedClients: 'web,app' })
    expect(store.beforeSubmit({ allowedClients: [] }).allowedClients).toBeNull()
  })

  it('字典异步更新后 Schema 响应式更新', () => {
    store.setDictionaries({ sys_user_status: [{ value: '1', label: '启用' }] })
    expect(store.searchSchema.find(item => item.field === 'userStatus').props.options).toEqual([{ value: 1, label: '启用' }])
    store.setDictionaries({ sys_user_status: [{ value: '0', label: '停用' }] })
    expect(store.userStatusOptions[0].value).toBe(0)
  })

  it('非管理员用户隐藏自己的敏感操作，批量操作同样保护', async () => {
    const actions = store.tableColumns.find(column => column.prop === 'action').actions
    for (const key of ['resetPwd', 'relation', 'disable', 'enable', 'delete']) {
      expect(actions.find(action => action.key === key).visible({ id: '9', userStatus: 1 })).toBe(false)
    }
    expect(store.editSchema.find(item => item.field === 'userStatus').vIf({ id: '9' })).toBe(false)
    store.handleUserSelectionChange({ keys: ['8', '9'] })
    await store.handleSubmitBatchAuth()
    expect(mocks.post).not.toHaveBeenCalled()
    expect(window.$message.warning).toHaveBeenCalledWith('批量操作不能包含当前登录用户')
  })

  it('系统管理员可维护自己：操作可见、状态可改、批量可包含自己', async () => {
    mocks.login.isAdmin = true
    const actions = store.tableColumns.find(column => column.prop === 'action').actions
    expect(actions.find(action => action.key === 'resetPwd').visible({ id: '9', userStatus: 1 })).toBe(true)
    expect(actions.find(action => action.key === 'relation').visible({ id: '9', userStatus: 1 })).toBe(true)
    expect(actions.find(action => action.key === 'disable').visible({ id: '9', userStatus: 1 })).toBe(true)
    expect(actions.find(action => action.key === 'enable').visible({ id: '9', userStatus: 0 })).toBe(true)
    expect(actions.find(action => action.key === 'delete').visible({ id: '9', userStatus: 1 })).toBe(true)
    expect(store.editSchema.find(item => item.field === 'userStatus').vIf({ id: '9' })).toBe(true)
    // 系统管理员（userType=0）禁用/删除保护，不依赖硬编码 id
    expect(actions.find(action => action.key === 'disable').visible({ id: 1, userType: 0, userStatus: 1 })).toBe(false)
    expect(actions.find(action => action.key === 'delete').visible({ id: 1, userType: 0, userStatus: 1 })).toBe(false)
    expect(actions.find(action => action.key === 'disable').visible({ id: 99, userType: 0, userStatus: 1 })).toBe(false)
    expect(actions.find(action => action.key === 'delete').visible({ id: 99, userType: 0, userStatus: 1 })).toBe(false)
    expect(actions.find(action => action.key === 'disable').visible({ id: 9, userType: 2, userStatus: 1 })).toBe(true)
    // 批量授权允许包含自己
    store.batchAuthTenantId = 1
    store.batchAuthOrgId = 10
    store.checkedRoleKeys = [3]
    store.handleUserSelectionChange({ keys: ['9'] })
    mocks.get.mockResolvedValueOnce({ code: 200, data: [] })
    await store.handleSubmitBatchAuth()
    expect(mocks.post.mock.calls).toEqual([['/system/user/9/org-roles', { tenantId: 1, orgId: 10, roleIds: [3] }]])
  })

  it('角色排序、全选本页和清空选择保持跨页选择', () => {
    store.roleTableData = [{ id: 2 }, { id: 1 }, { id: 3 }]
    store.checkedRoleKeys = [1, 7]
    expect(store.orderedRoleTableData.map(role => role.id)).toEqual([1, 2, 3])
    store.handleCheckAll()
    expect(store.checkedRoleKeys).toEqual([1, 7, 2, 3])
    store.handleUncheckAll()
    expect(store.checkedRoleKeys).toEqual([])
  })

  it('角色查询保留租户、组织、分页和已授权用户优先参数', async () => {
    store.currentUser = { id: 8, tenantId: 2 }
    store.authOrgId = 10
    store.rolePagination = { page: 2, pageSize: 20, itemCount: 0 }
    mocks.get.mockResolvedValueOnce({ code: 200, data: { records: [{ id: 3 }], total: 21 } })
    await store.loadRoleList()
    expect(mocks.get).toHaveBeenCalledWith('/system/role/page', { params: { pageNum: 2, pageSize: 20, roleName: undefined, orgId: 10, prioritizedUserId: 8, tenantId: 1 } })
    expect(store.rolePagination.itemCount).toBe(21)
  })

  it('批量授权追加现有角色而非覆盖，成功清选择并刷新', async () => {
    store.selectedUserIds = [8, 7]
    store.batchAuthTenantId = 1
    store.batchAuthOrgId = 10
    store.checkedRoleKeys = [3, 4]
    mocks.get.mockResolvedValueOnce({ code: 200, data: ['2', '3'] }).mockResolvedValueOnce({ code: 200, data: [5] })
    await store.handleSubmitBatchAuth()
    expect(mocks.post.mock.calls).toEqual([
      ['/system/user/8/org-roles', { tenantId: 1, orgId: 10, roleIds: [2, 3, 4] }],
      ['/system/user/7/org-roles', { tenantId: 1, orgId: 10, roleIds: [5, 3, 4] }],
    ])
    expect(store.selectedUserIds).toEqual([])
    expect(list.clearSelection).toHaveBeenCalledOnce()
    expect(list.refresh).toHaveBeenCalledOnce()
    expect(store.batchAuthSubmitLoading).toBe(false)
  })

  it('单用户授权保留请求字段，失败不关闭且释放 loading', async () => {
    store.currentUser = { id: 8, tenantId: 2 }
    store.authOrgId = 10
    store.checkedRoleKeys = ['3']
    await nextTick()
    store.relationModalVisible = true
    vi.spyOn(console, 'error').mockImplementation(() => {})
    mocks.post.mockRejectedValueOnce(new Error('模拟失败'))
    await store.handleSubmitAuth()
    expect(mocks.post).toHaveBeenCalledWith('/system/user/8/org-roles', { orgId: 10, roleIds: [3], tenantId: 2 })
    expect(store.authSubmitLoading).toBe(false)
    expect(store.relationModalVisible).toBe(true)
    expect(list.refresh).not.toHaveBeenCalled()
  })

  it('主组织、主岗位和默认租户不保留已移除选项', async () => {
    store.mainOrgId = 10
    store.checkedOrgKeys = [20]
    store.mainPostId = 30
    store.checkedPostKeys = [40]
    store.defaultTenantId = 1
    store.checkedTenantKeys = [2]
    await nextTick()
    expect(store.mainOrgId).toBe(20)
    expect(store.mainPostId).toBeNull()
    expect(store.defaultTenantId).toBe(2)
  })

  it('组织为空时不提交，主组织有效后按既有协议保存', async () => {
    store.currentUser = { id: 8 }
    await store.handleSubmitOrg()
    expect(mocks.post).not.toHaveBeenCalled()
    store.handleOrgCheckedKeysChange(['10', '20'])
    await store.handleSubmitOrg()
    expect(mocks.post).toHaveBeenCalledWith('/system/user/8/orgs', { orgIds: [10, 20], mainOrgId: 10 }, { params: { tenantId: 1 } })
  })

  it('管理员租户页签与批量入口响应登录角色', async () => {
    expect(store.relationTabItems.map(item => item.key)).not.toContain('tenant')
    await store.handleOpenBatchTenant()
    expect(store.batchTenantModalVisible).toBe(false)
    mocks.login.isAdmin = true
    expect(store.relationTabItems.map(item => item.key)).toContain('tenant')
    expect(store.userBatchActionOptions.map(item => item.key)).toContain('tenant')
  })

  it('密码弹窗关闭立即清理密码，组件引用不进入 Pinia 状态', async () => {
    store.resetPwdModalVisible = true
    store.resetPwdForm = { id: 8, password: 'test-only' }
    store.resetPwdModalVisible = false
    await nextTick()
    expect(store.resetPwdForm).toEqual({ id: null, password: '' })
    expect(store.$state).not.toHaveProperty('crudRef')
    expect(store.$state).not.toHaveProperty('resetPwdFormRef')
  })

  it('卸载后重新进入创建新状态，旧请求不能污染新页面', async () => {
    let resolve
    mocks.get.mockReturnValueOnce(new Promise((done) => {
      resolve = done
    }))
    const oldStore = store
    const pending = oldStore.loadTenantOptions()
    oldStore.$dispose()
    oldStore.resetWorkspace()
    delete pinia.state.value[oldStore.$id]
    store = useUserManagementStore()
    resolve({ code: 200, data: [{ id: 7, tenantName: '旧请求' }] })
    await pending
    expect(store.tenantOptions).toEqual([])
    expect(store.currentUser).toEqual({})
    expect(store.relationModalVisible).toBe(false)
  })
})
