<template>
  <div class="system-user-page">
    <!-- 左侧组织树 + 右侧用户列表布局 -->
    <div class="user-layout">
      <!-- 左侧组织树面板 -->
      <div class="org-tree-panel">
        <div class="org-tree-header">
          <div class="header-title">
            <i class="i-material-symbols:account-tree-rounded" />
            <span>组织架构</span>
          </div>
          <n-button text size="small" @click="toggleOrgExpandAll">
            <template #icon>
              <i :class="leftOrgExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
            </template>
          </n-button>
        </div>
        <div class="org-tree-content">
          <n-spin :show="leftOrgTreeLoading">
            <div
              class="org-tree-all-node"
              :class="{ 'is-selected': isShowAllUsers }"
              @click="handleSelectAllUsers"
            >
              <i class="i-material-symbols:groups-rounded" />
              <span>全部用户</span>
            </div>
            <n-tree
              v-if="leftOrgTreeData.length > 0"
              :data="leftOrgTreeData"
              :selected-keys="selectedOrgKeys"
              :expanded-keys="leftOrgExpandedKeys"
              :default-expand-all="leftOrgExpandAll"
              block-line
              selectable
              key-field="id"
              label-field="orgName"
              children-field="children"
              @update:selected-keys="handleOrgNodeSelect"
              @update:expanded-keys="handleLeftOrgExpandedKeysChange"
            />
            <n-empty v-else description="暂无组织数据" size="small" />
          </n-spin>
        </div>
      </div>

      <!-- 右侧用户列表 -->
      <div class="user-list-panel">
        <AiCrudPage
          ref="crudRef"
          api="/system/user"
          :api-config="{
            list: 'get@/system/user/page',
            detail: 'post@/system/user/getById',
            add: 'post@/system/user/add',
            update: 'post@/system/user/edit',
            delete: 'post@/system/user/remove',
          }"
          :search-schema="searchSchema"
          :columns="tableColumns"
          :edit-schema="editSchema"
          :before-submit="beforeSubmit"
          :before-load-list="beforeLoadList"
          row-key="id"
          :edit-grid-cols="2"
          modal-width="900px"
          add-button-text="新增用户"
        >
          <!-- 自定义工具栏提示 -->
          <template #toolbar-start>
            <div v-if="selectedOrgNode && !isShowAllUsers" class="org-filter-tip">
              <NTag type="info" size="small" closable @close="handleClearOrgFilter">
                当前筛选：{{ selectedOrgNode.orgName }}
              </NTag>
            </div>
          </template>
        </AiCrudPage>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <n-modal
      v-model:show="resetPwdModalVisible"
      title="重置密码"
      preset="card"
      style="width: 400px"
    >
      <n-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-placement="left"
        label-width="80"
      >
        <n-form-item label="新密码" path="password">
          <n-input
            v-model:value="resetPwdForm.password"
            type="password"
            show-password-on="click"
            placeholder="请输入新密码"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="resetPwdModalVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="resetPwdLoading" @click="handleConfirmResetPwd">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 授权弹窗 -->
    <n-modal
      v-model:show="authModalVisible"
      :title="`用户授权 - ${currentUser.username || ''}`"
      preset="card"
      style="width: 700px"
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

        <!-- 树形区域 -->
        <div class="auth-tree-container">
          <n-spin :show="authLoading">
            <n-tree
              v-if="roleTreeData.length > 0"
              ref="treeRef"
              :data="roleTreeData"
              checkable
              :cascade="!checkStrictly"
              :check-strategy="checkStrictly ? 'all' : 'child'"
              :default-expand-all="treeExpandAll"
              :expanded-keys="treeExpandedKeys"
              :checked-keys="checkedRoleKeys"
              key-field="id"
              label-field="roleName"
              children-field="children"
              @update:expanded-keys="handleExpandedKeysChange"
              @update:checked-keys="handleCheckedKeysChange"
            />
            <n-empty v-else description="暂无角色数据" />
          </n-spin>
        </div>
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

    <!-- 组织选择弹窗 -->
    <n-modal
      v-model:show="orgModalVisible"
      :title="`用户组织 - ${currentUser.username || ''}`"
      preset="card"
      style="width: 700px"
      :mask-closable="false"
    >
      <div class="org-modal-content">
        <!-- 操作按钮 -->
        <div class="org-toolbar">
          <n-space size="small">
            <n-button size="small" @click="toggleOrgExpandAll">
              <template #icon>
                <i :class="orgTreeExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
              </template>
              {{ orgTreeExpandAll ? '折叠全部' : '展开全部' }}
            </n-button>
          </n-space>
        </div>

        <!-- 主组织选择 -->
        <div class="org-main-section">
          <n-form-item label="主组织">
            <n-tree-select
              v-model:value="mainOrgId"
              :options="orgTreeData"
              key-field="id"
              label-field="orgName"
              children-field="children"
              placeholder="请选择主组织"
              checkable
              :default-expand-all="orgTreeExpandAll"
            />
          </n-form-item>
        </div>

        <!-- 组织树形区域 -->
        <div class="org-tree-container">
          <n-spin :show="orgLoading">
            <n-tree
              v-if="orgTreeData.length > 0"
              ref="orgTreeRef"
              :data="orgTreeData"
              checkable
              :default-expand-all="orgTreeExpandAll"
              :expanded-keys="orgTreeExpandedKeys"
              :checked-keys="checkedOrgKeys"
              key-field="id"
              label-field="orgName"
              children-field="children"
              @update:expanded-keys="handleOrgExpandedKeysChange"
              @update:checked-keys="handleOrgCheckedKeysChange"
            />
            <n-empty v-else description="暂无组织数据" />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="orgModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="orgSubmitLoading"
            @click="handleSubmitOrg"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemUser' })

const crudRef = ref(null)
const treeRef = ref(null)
const orgTreeRef = ref(null)

// 左侧组织树相关
const leftOrgTreeData = ref([])
const leftOrgTreeLoading = ref(false)
const leftOrgExpandAll = ref(true)
const leftOrgExpandedKeys = ref([])
const selectedOrgKeys = ref([])
const selectedOrgNode = ref(null)
const isShowAllUsers = ref(true)

// 授权相关
const authModalVisible = ref(false)
const authLoading = ref(false)
const authSubmitLoading = ref(false)
const currentUser = ref({})
const roleTreeData = ref([])
const checkedRoleKeys = ref([])
const treeExpandAll = ref(true)
const treeExpandedKeys = ref([])
const checkStrictly = ref(false)

// 重置密码相关
const resetPwdModalVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdForm = ref({
  id: null,
  password: '',
})
const resetPwdRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码不能少于6位', trigger: 'blur' }],
}

// 组织弹窗相关（用户组织绑定）
const orgModalVisible = ref(false)
const orgLoading = ref(false)
const orgSubmitLoading = ref(false)
const orgTreeData = ref([])
const checkedOrgKeys = ref([])
const mainOrgId = ref(null)
const orgTreeExpandAll = ref(true)
const orgTreeExpandedKeys = ref([])

// 行政区划选项
const regionOptions = ref([{ label: '请选择', value: '', key: '' }])

// 用户类型选项
const userTypeOptions = [
  { label: '系统管理员', value: 0 },
  { label: '租户管理员', value: 1 },
  { label: '普通用户', value: 2 },
]

// 用户状态选项
const userStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
  { label: '锁定', value: 2 },
]

// 性别选项
const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

// 搜索表单配置
const searchSchema = [
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
      options: userStatusOptions,
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'username',
    label: '用户名',
    width: 150,
  },
  {
    prop: 'realName',
    label: '真实姓名',
    width: 120,
  },
  {
    prop: 'userType',
    label: '用户类型',
    width: 120,
    render: (row) => {
      const option = userTypeOptions.find(opt => opt.value === row.userType)
      return option ? option.label : '-'
    },
  },
  {
    prop: 'phone',
    label: '手机号',
    width: 130,
  },
  {
    prop: 'email',
    label: '邮箱',
    width: 180,
  },
  {
    prop: 'gender',
    label: '性别',
    width: 80,
    render: (row) => {
      const option = genderOptions.find(opt => opt.value === row.gender)
      return option ? option.label : '-'
    },
  },
  {
    prop: 'regionCode',
    label: '行政区划',
    width: 150,
    render: (row) => {
      return row.regionCode || '-'
    },
  },
  {
    prop: 'userStatus',
    label: '状态',
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
      { label: '授权', key: 'auth', onClick: handleAuth },
      { label: '组织', key: 'org', onClick: handleOrg },
      { label: '重置密码', key: 'resetPwd', type: 'warning', onClick: (row) => { resetPwdForm.value = { id: row.id, password: '' }; resetPwdModalVisible.value = true } },
      { label: '禁用', key: 'disable', type: 'warning', onClick: row => handleUpdateStatus(row, 0), visible: row => row.id !== 1 && row.userStatus === 1 },
      { label: '启用', key: 'enable', type: 'success', onClick: handleUntieDisable, visible: row => row.id !== 1 && row.userStatus !== 1 },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => row.id !== 1 },
    ],
  },
])

// 编辑表单配置
const editSchema = [
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
      type: 'password',
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
    props: {
      placeholder: '请选择用户类型',
      options: userTypeOptions,
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
      options: genderOptions,
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
      lazy: true,
      onLoad: loadRegionChildren,
    },
    options: () => regionOptions.value,
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
    props: {
      options: userStatusOptions,
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

// 组件挂载时加载左侧组织树
onMounted(() => {
  loadLeftOrgTree()
})

// 获取所有节点的 key
function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 加载左侧组织树
async function loadLeftOrgTree() {
  try {
    leftOrgTreeLoading.value = true
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      leftOrgTreeData.value = res.data || []
      if (leftOrgExpandAll.value) {
        leftOrgExpandedKeys.value = getAllKeys(leftOrgTreeData.value)
      }
    }
    
    // 同时加载行政区划选项
    await loadRegionOptions()
  }
  catch (error) {
    console.error('加载组织树失败:', error)
    window.$message.error('加载组织树失败')
  }
  finally {
    leftOrgTreeLoading.value = false
  }
}

// 加载行政区划选项（只加载省级）
async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/tree')
    if (res.code === 200) {
      regionOptions.value = (res.data || []).map(item => ({
        label: item.name,
        value: item.code,
        key: item.code,
        // 根据hasChildren判断是否是叶子节点
        isLeaf: !item.hasChildren,
      }))
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

// 懒加载行政区划子节点
async function loadRegionChildren(node) {
  try {
    const res = await request.get(`/system/region/childrenVO/${node.value}`)
    if (res.code === 200) {
      return (res.data || []).map(item => ({
        label: item.name,
        value: item.code,
        key: item.code,
        isLeaf: !item.hasChildren,
      }))
    }
    return []
  }
  catch (error) {
    console.error('加载行政区划子节点失败:', error)
    return []
  }
}

// 左侧组织树节点选择
function handleOrgNodeSelect(keys) {
  selectedOrgKeys.value = keys

  if (keys.length > 0) {
    isShowAllUsers.value = false
    const orgId = keys[0]
    selectedOrgNode.value = findOrgNode(leftOrgTreeData.value, orgId)
    crudRef.value?.refresh()
  }
  else {
    selectedOrgNode.value = null
    crudRef.value?.refresh()
  }
}

// 查找组织节点
function findOrgNode(treeData, orgId) {
  for (const node of treeData) {
    if (node.id === orgId) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findOrgNode(node.children, orgId)
      if (found)
        return found
    }
  }
  return null
}

// 左侧组织树展开节点变化
function handleLeftOrgExpandedKeysChange(keys) {
  leftOrgExpandedKeys.value = keys
}

// 左侧组织树展开/折叠所有
function toggleOrgExpandAll() {
  leftOrgExpandAll.value = !leftOrgExpandAll.value

  if (leftOrgExpandAll.value) {
    leftOrgExpandedKeys.value = getAllKeys(leftOrgTreeData.value)
  }
  else {
    leftOrgExpandedKeys.value = []
  }
}

// 清除组织筛选
function handleClearOrgFilter() {
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  isShowAllUsers.value = true
  crudRef.value?.refresh()
}

function handleSelectAllUsers() {
  isShowAllUsers.value = true
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  crudRef.value?.refresh()
}

// 加载列表数据前的钩子（用于添加组织ID参数）
function beforeLoadList(params) {
  if (selectedOrgNode.value && !isShowAllUsers.value) {
    params.orgId = selectedOrgNode.value.id
  }
  return params
}

// 更多操作选项
function getMoreOptions(row) {
  const options = [
    { label: '重置密码', key: 'resetPwd', icon: () => h('i', { class: 'i-material-symbols:lock-reset' }) },
  ]

  if (row.id !== 1) {
    if (row.userStatus === 1) {
      options.push({ label: '禁用', key: 'disable', icon: () => h('i', { class: 'i-material-symbols:block' }) })
    }
    else {
      options.push({ label: '启用', key: 'enable', icon: () => h('i', { class: 'i-material-symbols:check-circle-outline' }) })
    }
  }

  return options
}

// 处理更多操作
function handleMoreAction(key, row) {
  switch (key) {
    case 'resetPwd':
      resetPwdForm.value = { id: row.id, password: '' }
      resetPwdModalVisible.value = true
      break
    case 'disable':
      handleUpdateStatus(row, 0)
      break
    case 'enable':
      handleUntieDisable(row)
      break
  }
}

// 确认重置密码
async function handleConfirmResetPwd() {
  resetPwdFormRef.value?.validate(async (errors) => {
    if (!errors) {
      try {
        resetPwdLoading.value = true
        const res = await request.post('/system/user/resetPwd', null, {
          params: {
            id: resetPwdForm.value.id,
            password: resetPwdForm.value.password,
          },
        })
        if (res.code === 200) {
          window.$message.success('重置成功')
          resetPwdModalVisible.value = false
        }
      }
      catch (error) {
        window.$message.error('重置失败')
      }
      finally {
        resetPwdLoading.value = false
      }
    }
  })
}

// 更新用户状态
async function handleUpdateStatus(row, status) {
  const actionText = status === 1 ? '启用' : '禁用'
  window.$dialog.warning({
    title: '确认操作',
    content: `确定要${actionText}用户"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/user/updateStatus', null, {
          params: { id: row.id, status },
        })
        if (res.code === 200) {
          window.$message.success(`${actionText}成功`)
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error(`${actionText}失败`)
      }
    },
  })
}

// 解封用户
async function handleUntieDisable(row) {
  window.$dialog.warning({
    title: '确认操作',
    content: `确定要启用并解封用户"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/user/doUntieDisable', null, {
          params: { id: row.id },
        })
        if (res.code === 200) {
          window.$message.success(`用户已启用`)
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error(`启用失败`)
      }
    },
  })
}

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
    content: `确定要删除用户"${row.username}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/user/remove', null, { params: { id: row.id } })
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

// 授权
async function handleAuth(row) {
  currentUser.value = row
  authModalVisible.value = true

  await loadRoleList()
  await loadUserRoles(row.id)
}

// 加载角色列表
async function loadRoleList() {
  try {
    authLoading.value = true
    const res = await request.get('/system/role/page', {
      params: { pageNum: 1, pageSize: 1000 },
    })
    if (res.code === 200) {
      roleTreeData.value = (res.data.list || res.data.records || []).map(role => ({
        id: role.id,
        roleName: role.roleName,
        roleKey: role.roleKey,
      }))

      if (treeExpandAll.value) {
        treeExpandedKeys.value = getAllKeys(roleTreeData.value)
      }
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

// 加载用户已有的角色
async function loadUserRoles(userId) {
  try {
    authLoading.value = true
    const res = await request.get(`/system/user/${userId}/roles`)
    if (res.code === 200) {
      checkedRoleKeys.value = res.data || []
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

// 展开的节点变化
function handleExpandedKeysChange(keys) {
  treeExpandedKeys.value = keys
}

// 选中的角色变化
function handleCheckedKeysChange(keys) {
  checkedRoleKeys.value = keys
}

// 展开/折叠所有
function toggleExpandAll() {
  treeExpandAll.value = !treeExpandAll.value

  if (treeExpandAll.value) {
    treeExpandedKeys.value = getAllKeys(roleTreeData.value)
  }
  else {
    treeExpandedKeys.value = []
  }
}

// 全选
function handleCheckAll() {
  const allKeys = getAllKeys(roleTreeData.value)
  checkedRoleKeys.value = allKeys
}

// 全不选
function handleUncheckAll() {
  checkedRoleKeys.value = []
}

// 父子联动开关变化
function handleCheckStrictlyChange(value) {
  console.log('父子联动:', !value)
}

// 提交授权
async function handleSubmitAuth() {
  try {
    authSubmitLoading.value = true
    const res = await request.post(
      `/system/user/${currentUser.value.id}/roles`,
      checkedRoleKeys.value,
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

// 组织管理
async function handleOrg(row) {
  currentUser.value = row
  orgModalVisible.value = true
  mainOrgId.value = null
  checkedOrgKeys.value = []

  await loadOrgTree()
  await loadUserOrgs(row.id)
}

// 加载组织树
async function loadOrgTree() {
  try {
    orgLoading.value = true
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      orgTreeData.value = res.data || []
      if (orgTreeExpandAll.value) {
        orgTreeExpandedKeys.value = getAllKeys(orgTreeData.value)
      }
    }
  }
  catch (error) {
    console.error('加载组织列表失败:', error)
    window.$message.error('加载组织列表失败')
  }
  finally {
    orgLoading.value = false
  }
}

// 加载用户已绑定的组织
async function loadUserOrgs(userId) {
  try {
    orgLoading.value = true
    const res = await request.get(`/system/user/${userId}/orgs`)
    if (res.code === 200) {
      checkedOrgKeys.value = res.data || []
      mainOrgId.value = checkedOrgKeys.value.length > 0 ? checkedOrgKeys.value[0] : null
    }
  }
  catch (error) {
    console.error('加载用户组织失败:', error)
    window.$message.error('加载用户组织失败')
  }
  finally {
    orgLoading.value = false
  }
}

// 组织展开的节点变化
function handleOrgExpandedKeysChange(keys) {
  orgTreeExpandedKeys.value = keys
}

// 组织选中的变化
function handleOrgCheckedKeysChange(keys) {
  checkedOrgKeys.value = keys
  if (mainOrgId.value && !keys.includes(mainOrgId.value)) {
    mainOrgId.value = null
  }
}

// 提交组织绑定
async function handleSubmitOrg() {
  if (checkedOrgKeys.value.length === 0) {
    window.$message.warning('请至少选择一个组织')
    return
  }

  try {
    orgSubmitLoading.value = true
    const res = await request.post(
      `/system/user/${currentUser.value.id}/orgs`,
      {
        orgIds: checkedOrgKeys.value,
        mainOrgId: mainOrgId.value,
      },
    )
    if (res.code === 200) {
      window.$message.success('组织绑定成功')
      orgModalVisible.value = false
      crudRef.value?.refresh()
    }
  }
  catch (error) {
    console.error('组织绑定失败:', error)
    window.$message.error('组织绑定失败')
  }
  finally {
    orgSubmitLoading.value = false
  }
}
</script>

<style scoped>
.system-user-page {
  height: 100%;
  padding: 0;
}

/* 左右布局 */
.user-layout {
  display: flex;
  height: 100%;
  gap: 16px;
}

/* 左侧组织树面板 */
.org-tree-panel {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.org-tree-header {
  padding: 16px 16px 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.header-title i {
  font-size: 20px;
  color: #4f46e5;
}

.org-tree-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 8px;
}

.org-tree-all-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s;
}

.org-tree-all-node:hover {
  background-color: #f3f4f6;
}

.org-tree-all-node.is-selected {
  background-color: #e0e7ff !important;
  color: #4f46e5;
  font-weight: 500;
}

.org-tree-all-node i {
  font-size: 18px;
}

.org-tree-content :deep(.n-tree) {
  font-size: 14px;
}

.org-tree-content :deep(.n-tree-node-content) {
  padding: 6px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.org-tree-content :deep(.n-tree-node-content:hover) {
  background-color: #f3f4f6;
}

.org-tree-content :deep(.n-tree-node-content--selected) {
  background-color: #e0e7ff !important;
  color: #4f46e5;
}

.org-tree-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.org-tree-content::-webkit-scrollbar-track {
  background: transparent;
}

.org-tree-content::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.org-tree-content::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 右侧用户列表面板 */
.user-list-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.user-list-panel :deep(.ai-crud-page) {
  height: 100%;
}

/* 组织筛选提示 */
.org-filter-tip {
  margin-right: 12px;
}

.org-filter-tip :deep(.n-tag) {
  font-size: 13px;
}

/* 授权弹窗样式 */
.auth-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 600px;
}

.auth-toolbar {
  padding: 12px;
  background-color: #d9d7d7;
  border-radius: 4px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.auth-tree-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-radius: 4px;
  padding: 12px;
  min-height: 300px;
  max-height: 500px;
}

.auth-tree-container :deep(.n-tree) {
  font-size: 14px;
}

.auth-tree-container :deep(.n-tree-node-content) {
  padding: 4px 0;
}

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
}

.auth-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}

/* 组织弹窗样式 */
.org-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 600px;
}

.org-toolbar {
  padding: 12px;
  background-color: #d9d7d7;
  border-radius: 4px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.org-main-section {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.org-main-section :deep(.n-form-item) {
  margin-bottom: 0;
}

.org-tree-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-radius: 4px;
  padding: 12px;
  min-height: 300px;
  max-height: 400px;
}

.org-tree-container :deep(.n-tree) {
  font-size: 14px;
}

.org-tree-container :deep(.n-tree-node-content) {
  padding: 4px 0;
}

.org-tree-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.org-tree-container::-webkit-scrollbar-track {
  background: var(--n-scrollbar-color);
  border-radius: 4px;
}

.org-tree-container::-webkit-scrollbar-thumb {
  background: var(--n-scrollbar-color-hover);
  border-radius: 4px;
}

.org-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}

/* ═══════════════════════════════════════
 * 深色模式
 * ═══════════════════════════════════════ */
.dark .org-tree-panel {
  background: #0f172a !important;
  border-color: #334155 !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .org-tree-header {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark .org-tree-header .header-title span {
  color: #f1f5f9;
}

.dark .org-tree-header .header-title i {
  color: #60a5fa;
}

.dark .org-tree-content {
  background: #0f172a;
}

.dark .org-tree-all-node {
  color: #e2e8f0;
}

.dark .org-tree-all-node:hover {
  background-color: #1e293b;
}

.dark .org-tree-all-node.is-selected {
  background-color: #1e3a5f !important;
  color: #60a5fa;
}

.dark .org-tree-content .n-tree-node-content {
  color: #e2e8f0;
}

.dark .org-tree-content .n-tree-node-content:hover {
  background-color: #1e293b;
}

.dark .org-tree-content .n-tree-node-content--selected {
  background-color: #1e3a5f !important;
  color: #60a5fa;
}

.dark .org-tree-content::-webkit-scrollbar-track {
  background: #1e293b;
}

.dark .org-tree-content::-webkit-scrollbar-thumb {
  background: #475569;
}

.dark .org-tree-content::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

.dark .user-list-panel {
  background: #0f172a !important;
  border-color: #334155 !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .org-filter-tip .n-tag {
  background: #1e293b;
  border-color: #334155;
}

.dark .auth-toolbar {
  background-color: #1e293b;
}

.dark .org-toolbar {
  background-color: #1e293b;
}

.dark .auth-tree-container {
  background: #0f172a;
}

.dark .auth-tree-container .n-tree {
  color: #e2e8f0;
}

.dark .org-tree-container {
  background: #0f172a;
}

.dark .org-tree-container .n-tree {
  color: #e2e8f0;
}

.dark .empty-state {
  background: #0f172a;
}
</style>
