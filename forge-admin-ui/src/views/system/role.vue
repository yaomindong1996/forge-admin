<template>
  <div class="system-role-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/role"
      :api-config="{
        list: 'get@/system/role/page',
        detail: 'post@/system/role/getById',
        add: 'post@/system/role/add',
        update: 'post@/system/role/edit',
        delete: 'post@/system/role/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      add-button-text="新增角色"
    />

    <!-- 授权弹窗 -->
    <n-modal
      v-model:show="authModalVisible"
      :title="`角色授权 - ${currentRole.roleName || ''}`"
      preset="card"
      style="width: 900px"
      :mask-closable="false"
    >
      <div class="auth-modal-content">
        <!-- 操作按钮 -->
        <div class="auth-toolbar">
          <n-space size="small">
            <n-button size="small" @click="toggleExpandAll">
              <template #icon>
                <i :class="treeExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
              </template>
              {{ treeExpandAll ? '折叠全部' : '展开全部' }}
            </n-button>
            <n-button size="small" @click="handleCheckAll">
              <template #icon>
                <i class="i-material-symbols:check-box-outline" />
              </template>
              全选
            </n-button>
            <n-button size="small" @click="handleUncheckAll">
              <template #icon>
                <i class="i-material-symbols:check-box-outline-blank" />
              </template>
              全不选
            </n-button>
            <n-checkbox v-model:checked="checkStrictly" @update:checked="handleCheckStrictlyChange">
              父子联动
            </n-checkbox>
          </n-space>
        </div>

        <!-- 资源类型标签页 -->
        <n-tabs v-model:value="activeResourceTab" type="line" animated class="auth-tabs">
          <n-tab-pane name="all" tab="全部资源">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <n-tree
                  v-if="resourceTreeData.length > 0"
                  ref="treeRef"
                  :data="resourceTreeData"
                  checkable
                  :cascade="!checkStrictly"
                  :check-strategy="checkStrictly ? 'all' : 'child'"
                  :default-expand-all="treeExpandAll"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :render-label="renderTreeLabel"
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无资源数据" />
              </n-spin>
            </div>
          </n-tab-pane>

          <n-tab-pane name="menu" tab="菜单">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <n-tree
                  v-if="menuTreeData.length > 0"
                  :data="menuTreeData"
                  checkable
                  :cascade="!checkStrictly"
                  :check-strategy="checkStrictly ? 'all' : 'child'"
                  :default-expand-all="treeExpandAll"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :render-label="renderTreeLabel"
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无菜单数据" />
              </n-spin>
            </div>
          </n-tab-pane>

          <n-tab-pane name="button" tab="按钮">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <n-tree
                  v-if="buttonTreeData.length > 0"
                  :data="buttonTreeData"
                  checkable
                  :cascade="!checkStrictly"
                  :check-strategy="checkStrictly ? 'all' : 'child'"
                  :default-expand-all="treeExpandAll"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :render-label="renderTreeLabel"
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无按钮数据" />
              </n-spin>
            </div>
          </n-tab-pane>

          <n-tab-pane name="api" tab="API接口">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <n-tree
                  v-if="apiTreeData.length > 0"
                  :data="apiTreeData"
                  checkable
                  :cascade="!checkStrictly"
                  :check-strategy="checkStrictly ? 'all' : 'child'"
                  :default-expand-all="treeExpandAll"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :render-label="renderTreeLabel"
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无API数据" />
              </n-spin>
            </div>
          </n-tab-pane>
        </n-tabs>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="authModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="authSubmitLoading"
            @click="handleSubmitAuth"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 用户列表弹窗 -->
    <n-modal
      v-model:show="usersModalVisible"
      :title="`角色用户 - ${currentRole.roleName || ''}`"
      preset="card"
      style="width: 900px"
      :mask-closable="false"
    >
      <div class="users-modal-content">
        <!-- 搜索表单 -->
        <div class="users-search-form">
          <n-space>
            <n-input
              v-model:value="userSearchParams.username"
              placeholder="用户名"
              clearable
              size="small"
              style="width: 150px"
              @clear="handleUserSearch"
              @keyup.enter="handleUserSearch"
            />
            <n-input
              v-model:value="userSearchParams.realName"
              placeholder="真实姓名"
              clearable
              size="small"
              style="width: 150px"
              @clear="handleUserSearch"
              @keyup.enter="handleUserSearch"
            />
            <n-input
              v-model:value="userSearchParams.phone"
              placeholder="手机号"
              clearable
              size="small"
              style="width: 150px"
              @clear="handleUserSearch"
              @keyup.enter="handleUserSearch"
            />
            <n-select
              v-model:value="userSearchParams.userStatus"
              placeholder="用户状态"
              clearable
              size="small"
              style="width: 120px"
              :options="userStatusOptions"
            />
            <n-button size="small" type="primary" @click="handleUserSearch">
              <template #icon>
                <i class="i-material-symbols:search" />
              </template>
              查询
            </n-button>
            <n-button size="small" @click="handleUserSearchReset">
              重置
            </n-button>
          </n-space>
        </div>

        <!-- 统计和刷新 -->
        <div class="users-toolbar">
          <n-space justify="space-between">
            <div class="user-count-info">
              <NTag type="info" size="small">
                共 {{ userPagination.itemCount }} 个用户
              </NTag>
            </div>
            <n-button size="small" @click="loadRoleUsers">
              <template #icon>
                <i class="i-material-symbols:refresh" />
              </template>
              刷新
            </n-button>
          </n-space>
        </div>

        <!-- 用户列表表格 -->
        <div class="users-table-container">
          <n-spin :show="usersLoading">
            <n-data-table
              v-if="roleUsers.length > 0 || !usersLoading"
              :columns="userTableColumns"
              :data="roleUsers"
              :pagination="userPaginationConfig"
              :row-key="row => row.id"
              striped
              size="small"
              @update:page="handleUserPageChange"
              @update:page-size="handleUserPageSizeChange"
            />
            <n-empty v-if="roleUsers.length === 0 && !usersLoading" description="该角色暂无用户" size="small" />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="usersModalVisible = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemRole' })

const crudRef = ref(null)
const treeRef = ref(null)

// 授权相关
const authModalVisible = ref(false)
const authLoading = ref(false)
const authSubmitLoading = ref(false)
const resourceTreeData = ref([])
const checkedResourceKeys = ref([])
const treeExpandAll = ref(true)
const treeExpandedKeys = ref([])
const checkStrictly = ref(false) // 父子联动开关，false表示联动
const activeResourceTab = ref('all') // 当前选中的资源类型标签

// 用户列表相关
const usersModalVisible = ref(false)
const usersLoading = ref(false)
const roleUsers = ref([]) // 角色下的用户列表
const currentRole = ref({})
const userSearchParams = ref({
  username: '',
  realName: '',
  phone: '',
  userStatus: null,
})
const userPagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})

// 用户状态选项
const userStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
  { label: '锁定', value: 2 },
]

// 计算分页配置
const userPaginationConfig = computed(() => ({
  page: userPagination.value.page,
  pageSize: userPagination.value.pageSize,
  itemCount: userPagination.value.itemCount,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
}))

// 资源类型映射
const resourceTypeMap = {
  1: { text: '目录', type: 'info', icon: 'i-material-symbols:folder-outline' },
  2: { text: '菜单', type: 'success', icon: 'i-material-symbols:menu' },
  3: { text: '按钮', type: 'warning', icon: 'i-material-symbols:smart-button-outline' },
  4: { text: 'API', type: 'error', icon: 'i-material-symbols:api' },
}

// 计算属性：过滤不同类型的资源
const menuTreeData = computed(() => {
  return filterResourceByType(resourceTreeData.value, [1, 2])
})

const buttonTreeData = computed(() => {
  return filterResourceByType(resourceTreeData.value, [3])
})

const apiTreeData = computed(() => {
  return filterResourceByType(resourceTreeData.value, [4])
})

// 过滤指定类型的资源
function filterResourceByType(data, types) {
  if (!data || !Array.isArray(data))
    return []

  return data.reduce((result, item) => {
    // 如果当前节点类型匹配
    if (types.includes(item.resourceType)) {
      const newItem = { ...item }
      // 递归处理子节点
      if (item.children && item.children.length > 0) {
        newItem.children = filterResourceByType(item.children, types)
      }
      result.push(newItem)
    }
    else {
      // 当前节点类型不匹配，但需要检查子节点
      if (item.children && item.children.length > 0) {
        const filteredChildren = filterResourceByType(item.children, types)
        if (filteredChildren.length > 0) {
          result.push({
            ...item,
            children: filteredChildren,
          })
        }
      }
    }
    return result
  }, [])
}

// 自定义树节点渲染
function renderTreeLabel({ option }) {
  const typeConfig = resourceTypeMap[option.resourceType] || { text: '未知', type: 'default', icon: '' }

  return h('div', { class: 'flex items-center gap-2' }, [
    // 图标
    typeConfig.icon && h('i', { class: `${typeConfig.icon} text-16` }),
    // 资源名称
    h('span', { class: 'font-medium' }, option.resourceName),
    // 类型标签
    h(NTag, {
      type: typeConfig.type,
      size: 'small',
      round: true,
      class: 'ml-2',
    }, { default: () => typeConfig.text }),
    // 权限标识（如果有）
    option.perms && h(NTag, {
      type: 'default',
      size: 'small',
      bordered: false,
      class: 'ml-2',
    }, { default: () => option.perms }),
    // API 方法和地址（如果是API类型）
    option.resourceType === 4 && option.apiMethod && h(NTag, {
      type: 'info',
      size: 'small',
      bordered: false,
      class: 'ml-2',
    }, { default: () => `${option.apiMethod} ${option.apiUrl || ''}` }),
  ])
}

// 数据范围选项
const dataScopeOptions = [
  { label: '全部数据', value: 1 },
  { label: '本租户数据', value: 2 },
  { label: '本组织数据', value: 3 },
  { label: '本组织及子组织', value: 4 },
  { label: '个人数据', value: 5 },
]

// 角色状态选项
const roleStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

// 搜索表单配置
const searchSchema = [
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
    field: 'roleStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: roleStatusOptions,
    },
  },
]

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
    prop: 'dataScope',
    label: '数据范围',
    width: 150,
    render: (row) => {
      const option = dataScopeOptions.find(opt => opt.value === row.dataScope)
      return option ? option.label : '-'
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
      return h(NTag, { type: row.roleStatus === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.roleStatus === 1 ? '正常' : '禁用' },
      )
    },
  },
  {
    prop: 'isSystem',
    label: '系统角色',
    width: 100,
    render: (row) => {
      return h(NTag, { type: row.isSystem === 1 ? 'warning' : 'default', size: 'small' }, { default: () => row.isSystem === 1 ? '是' : '否' },
      )
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
    width: 150,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '查看用户', key: 'viewUsers', onClick: handleViewUsers },
      { label: '授权', key: 'auth', onClick: handleAuth },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => row.id !== 1 },
    ],
  },
])

// 编辑表单配置
const editSchema = [
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
    field: 'dataScope',
    label: '数据范围',
    type: 'select',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择数据范围', trigger: 'change' }],
    props: {
      placeholder: '请选择数据范围',
      options: dataScopeOptions,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
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
      options: roleStatusOptions,
    },
  },
  {
    field: 'isSystem',
    label: '系统角色',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 },
      ],
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
]

// 用户表格列配置（用于角色用户列表）
const userTableColumns = [
  {
    title: '用户名',
    key: 'username',
    width: 150,
  },
  {
    title: '真实姓名',
    key: 'realName',
    width: 120,
  },
  {
    title: '手机号',
    key: 'phone',
    width: 130,
  },
  {
    title: '邮箱',
    key: 'email',
    width: 180,
  },
  {
    title: '用户类型',
    key: 'userType',
    width: 120,
    render: (row) => {
      const typeMap = {
        0: '系统管理员',
        1: '租户管理员',
        2: '普通用户',
      }
      return h(NTag, { type: 'info', size: 'small' }, { default: () => typeMap[row.userType] || '未知' })
    },
  },
  {
    title: '状态',
    key: 'userStatus',
    width: 80,
    render: (row) => {
      const statusMap = {
        0: { text: '禁用', type: 'error' },
        1: { text: '正常', type: 'success' },
        2: { text: '锁定', type: 'warning' },
      }
      const config = statusMap[row.userStatus] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right',
    render: (row) => {
      return h('a', {
        class: 'text-error cursor-pointer hover:text-error-hover',
        onClick: () => handleRemoveUserRole(row),
      }, '移除')
    },
  },
]

// 表单提交前处理
function beforeSubmit(formData) {
  console.log('提交的表单数据:', formData)
  return formData
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
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('删除失败')
      }
    },
  })
}

// 查看角色用户
async function handleViewUsers(row) {
  currentRole.value = row
  usersModalVisible.value = true
  // 重置搜索条件和分页
  userSearchParams.value = {
    username: '',
    realName: '',
    phone: '',
    userStatus: null,
  }
  userPagination.value.page = 1
  await loadRoleUsers()
}

// 加载角色用户列表
async function loadRoleUsers() {
  try {
    usersLoading.value = true
    const params = {
      ...userSearchParams.value,
      pageNum: userPagination.value.page,
      pageSize: userPagination.value.pageSize,
    }
    // 过滤空值
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })

    const res = await request.get(`/system/role/${currentRole.value.id}/users`, { params })
    if (res.code === 200) {
      roleUsers.value = res.data.records || []
      userPagination.value.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    console.error('加载角色用户失败:', error)
    window.$message.error('加载角色用户失败')
  }
  finally {
    usersLoading.value = false
  }
}

// 用户搜索
function handleUserSearch() {
  userPagination.value.page = 1
  loadRoleUsers()
}

// 用户搜索重置
function handleUserSearchReset() {
  userSearchParams.value = {
    username: '',
    realName: '',
    phone: '',
    userStatus: null,
  }
  userPagination.value.page = 1
  loadRoleUsers()
}

// 用户分页变化
function handleUserPageChange(page) {
  userPagination.value.page = page
  loadRoleUsers()
}

// 用户分页大小变化
function handleUserPageSizeChange(pageSize) {
  userPagination.value.pageSize = pageSize
  userPagination.value.page = 1
  loadRoleUsers()
}

// 移除角色用户
async function handleRemoveUserRole(user) {
  window.$dialog.warning({
    title: '确认移除',
    content: `确定要从角色"${currentRole.value.roleName}"中移除用户"${user.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/role/removeUserRole', null, {
          params: {
            roleId: currentRole.value.id,
            userId: user.id,
          },
        })
        if (res.code === 200) {
          window.$message.success('移除成功')
          await loadRoleUsers()
        }
      }
      catch (error) {
        console.error('移除用户失败:', error)
        window.$message.error('移除用户失败')
      }
    },
  })
}

// 授权
async function handleAuth(row) {
  currentRole.value = row
  authModalVisible.value = true

  // 加载资源树和已选中的资源
  await loadResourceTree()
  await loadRoleResources(row.id)
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
    authLoading.value = true
    const res = await request.get('/system/resource/tree')
    if (res.code === 200) {
      resourceTreeData.value = res.data || []

      // 如果默认展开，收集所有节点的 key
      if (treeExpandAll.value) {
        treeExpandedKeys.value = getAllKeys(resourceTreeData.value)
      }
    }
  }
  catch (error) {
    console.error('加载资源树失败:', error)
    window.$message.error('加载资源树失败')
  }
  finally {
    authLoading.value = false
  }
}

// 加载角色已有的资源
async function loadRoleResources(roleId) {
  try {
    authLoading.value = true
    const res = await request.get(`/system/role/${roleId}/resources`)
    if (res.code === 200) {
      checkedResourceKeys.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载角色资源失败:', error)
    window.$message.error('加载角色资源失败')
  }
  finally {
    authLoading.value = false
  }
}

// 展开的节点变化
function handleExpandedKeysChange(keys) {
  treeExpandedKeys.value = keys
}

// 选中的资源变化
function handleCheckedKeysChange(keys) {
  checkedResourceKeys.value = keys
}

// 展开/折叠所有
function toggleExpandAll() {
  treeExpandAll.value = !treeExpandAll.value

  if (treeExpandAll.value) {
    // 展开所有：获取所有节点的 key
    treeExpandedKeys.value = getAllKeys(resourceTreeData.value)
  }
  else {
    // 收起所有：清空展开的 key
    treeExpandedKeys.value = []
  }
}

// 全选
function handleCheckAll() {
  const allKeys = getAllKeys(resourceTreeData.value)
  checkedResourceKeys.value = allKeys
}

// 全不选
function handleUncheckAll() {
  checkedResourceKeys.value = []
}

// 父子联动开关变化
function handleCheckStrictlyChange(value) {
  // 当切换父子联动时，保持当前选中状态
  // checkStrictly 为 false 时表示联动
  console.log('父子联动:', !value)
}

// 提交授权
async function handleSubmitAuth() {
  try {
    authSubmitLoading.value = true
    const res = await request.post(
      `/system/role/${currentRole.value.id}/resources`,
      checkedResourceKeys.value,
    )
    if (res.code === 200) {
      window.$message.success('授权成功')
      authModalVisible.value = false
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
</script>

<style scoped>
.system-role-page {
  height: 100%;
}

/* 授权弹窗样式 */
.auth-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 600px;
}

.auth-toolbar {
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.auth-toolbar :deep(.n-button) {
  color: white;
  border-color: rgba(255, 255, 255, 0.3);
}

.auth-toolbar :deep(.n-button:hover) {
  background-color: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.auth-toolbar :deep(.n-checkbox) {
  color: white;
}

.auth-toolbar :deep(.n-checkbox .n-checkbox__label) {
  color: white;
}

/* 标签页样式 */
.auth-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.auth-tabs :deep(.n-tabs-nav) {
  padding: 0 12px;
  background-color: #f8f9fa;
  border-radius: 8px 8px 0 0;
}

.auth-tabs :deep(.n-tabs-tab) {
  font-weight: 500;
  padding: 12px 20px;
}

.auth-tabs :deep(.n-tabs-tab.n-tabs-tab--active) {
  color: #667eea;
}

.auth-tabs :deep(.n-tabs-pane-wrapper) {
  flex: 1;
  min-height: 0;
}

.auth-tree-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  padding: 16px;
  min-height: 300px;
  max-height: 450px;
  background-color: var(--n-color);
}

/* 优化树形组件样式 */
.auth-tree-container :deep(.n-tree) {
  font-size: 14px;
}

.auth-tree-container :deep(.n-tree-node) {
  padding: 2px 0;
}

.auth-tree-container :deep(.n-tree-node-content) {
  padding: 6px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.auth-tree-container :deep(.n-tree-node-content:hover) {
  background-color: var(--n-color-hover);
}

.auth-tree-container :deep(.n-tree-node--selected .n-tree-node-content) {
  background-color: var(--n-color-target);
}

/* 树节点标签样式增强 */
.auth-tree-container :deep(.n-tree-node-content .flex) {
  align-items: center;
}

.auth-tree-container :deep(.n-tag) {
  font-size: 12px;
  line-height: 1.5;
}

/* 滚动条样式 */
.auth-tree-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.auth-tree-container::-webkit-scrollbar-track {
  background: var(--n-scrollbar-color);
  border-radius: 4px;
}

.auth-tree-container::-webkit-scrollbar-thumb {
  background: var(--n-scrollbar-color-hover);
  border-radius: 4px;
  transition: background 0.2s ease;
}

.auth-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}

/* 弹窗底部按钮样式 */
:deep(.n-card__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--n-border-color);
  background-color: #f8f9fa;
}

/* 用户列表弹窗样式 */
.users-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 600px;
}

.users-search-form {
  padding: 12px 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.users-toolbar {
  padding: 8px 16px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(79, 172, 254, 0.15);
}

.users-toolbar :deep(.n-button) {
  color: white;
  border-color: rgba(255, 255, 255, 0.3);
}

.users-toolbar :deep(.n-button:hover) {
  background-color: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.users-toolbar :deep(.n-tag) {
  color: white;
  background-color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.user-count-info {
  display: flex;
  align-items: center;
}

.users-table-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  min-height: 300px;
  max-height: 450px;
  background-color: var(--n-color);
}

.users-table-container :deep(.n-data-table) {
  font-size: 14px;
}

.users-table-container :deep(.n-data-table-th) {
  font-weight: 600;
  background-color: #f8f9fa;
}

.users-table-container :deep(.n-data-table-td) {
  padding: 12px 16px;
}

.users-table-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.users-table-container::-webkit-scrollbar-track {
  background: var(--n-scrollbar-color);
  border-radius: 4px;
}

.users-table-container::-webkit-scrollbar-thumb {
  background: var(--n-scrollbar-color-hover);
  border-radius: 4px;
  transition: background 0.2s ease;
}

.users-table-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}
</style>
