<template>
  <div class="system-user-page">
    <!-- 左侧组织树 + 右侧用户列表布局 -->
    <div class="user-layout">
      <!-- 左侧组织树面板 -->
      <div class="org-tree-panel" :class="{ 'is-collapsed': leftOrgPanelCollapsed }">
        <div class="org-tree-header">
          <div class="header-title">
            <div class="header-icon">
              <i class="i-material-symbols:account-tree-rounded" />
            </div>
            <div v-if="!leftOrgPanelCollapsed" class="header-copy">
              <span>组织架构</span>
              <small>{{ orgTreeSummaryText }}</small>
            </div>
          </div>
          <div class="header-actions">
            <n-button
              v-if="!leftOrgPanelCollapsed"
              quaternary
              circle
              size="small"
              title="展开或折叠树节点"
              @click="toggleOrgExpandAll"
            >
              <template #icon>
                <i :class="leftOrgExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
              </template>
            </n-button>
            <n-button
              quaternary
              circle
              size="small"
              :title="leftOrgPanelCollapsed ? '展开左侧组织树' : '收起左侧组织树'"
              @click="toggleLeftOrgPanel"
            >
              <template #icon>
                <i :class="leftOrgPanelCollapsed ? 'i-material-symbols:chevron-right-rounded' : 'i-material-symbols:left-panel-close-rounded'" />
              </template>
            </n-button>
          </div>
        </div>
        <div v-show="!leftOrgPanelCollapsed" class="org-tree-content">
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
        <div
          v-show="leftOrgPanelCollapsed"
          class="org-tree-collapsed-hint"
          :class="{ 'has-active-filter': selectedOrgNode && !isShowAllUsers }"
          @click="toggleLeftOrgPanel"
        >
          <i class="i-material-symbols:group-work-outline-rounded" />
          <span>组织筛选</span>
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
            <n-checkbox v-model:checked="checkStrictly">
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
const leftOrgPanelCollapsed = ref(false)
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

// 行政区划选项（搜索用：虚拟节点可选；编辑用：虚拟节点不可选）
const searchRegionOptions = ref([])
const editRegionOptions = ref([])

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
      if (!row.regionCode)
        return '-'
      const name = findRegionName(searchRegionOptions.value, row.regionCode)
      return name || row.regionCode
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
      {
        label: '重置密码',
        key: 'resetPwd',
        type: 'warning',
        onClick: (row) => {
          resetPwdForm.value = { id: row.id, password: '' }
          resetPwdModalVisible.value = true
        },
      },
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

const orgTreeSummaryText = computed(() => {
  const total = countTreeNodes(leftOrgTreeData.value)
  if (!total)
    return '未加载组织'
  return `${total} 个组织节点`
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

function countTreeNodes(list = []) {
  return list.reduce((total, item) => total + 1 + countTreeNodes(item.children || []), 0)
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

// 加载行政区划选项（一次性加载内蒙完整区划树，含虚拟组织）
async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/treeAll', { params: { rootCode: '150000', dataRight: true } })
    if (res.code === 200) {
      const data = res.data || []
      // 搜索场景：虚拟节点可选（代表"该区域下所有"）
      searchRegionOptions.value = convertRegionToTreeSelect(data, false)
      // 编辑场景：虚拟节点不可选（避免存入ALL后缀编码）
      editRegionOptions.value = convertRegionToTreeSelect(data, true)
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

// 将后端返回的树形数据转换为TreeSelect组件需要的格式
// virtualDisabled: true=编辑表单（虚拟节点不可选），false=搜索筛选（虚拟节点可选）
function convertRegionToTreeSelect(list, virtualDisabled = true) {
  return list.map((item) => {
    const node = {
      label: item.name,
      value: item.code,
      key: item.code,
    }
    if (virtualDisabled && item.code && item.code.endsWith('ALL')) {
      node.disabled = true
    }
    if (item.children && item.children.length > 0) {
      node.children = convertRegionToTreeSelect(item.children, virtualDisabled)
    }
    return node
  })
}

// 在区划树中根据code查找名称
function findRegionName(options, code) {
  for (const item of options) {
    if (item.value === code)
      return item.label
    if (item.children) {
      const name = findRegionName(item.children, code)
      if (name)
        return name
    }
  }
  return null
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

function toggleLeftOrgPanel() {
  leftOrgPanelCollapsed.value = !leftOrgPanelCollapsed.value
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
      catch {
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
      catch {
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
      catch {
        window.$message.error(`启用失败`)
      }
    },
  })
}

// 表单提交前处理
function beforeSubmit(formData) {
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
      catch {
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
  padding: 20px;
  display: flex;
  flex-direction: column;
}

/* 左右布局 */
.user-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 12px;
}

/* 左侧组织树面板 */
.org-tree-panel {
  width: 248px;
  min-width: 248px;
  background: linear-gradient(180deg, #fbfdff 0%, #ffffff 18%, #ffffff 100%);
  border-radius: 14px;
  border: 1px solid #dbe4f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition:
    width 0.24s ease,
    min-width 0.24s ease,
    box-shadow 0.24s ease;
}

.org-tree-panel.is-collapsed {
  width: 72px;
  min-width: 72px;
}

.org-tree-panel.is-collapsed .org-tree-header {
  flex-direction: column;
  justify-content: flex-start;
  gap: 10px;
  padding: 12px 8px;
}

.org-tree-panel.is-collapsed .header-title,
.org-tree-panel.is-collapsed .header-actions {
  width: 100%;
  justify-content: center;
}

.org-tree-header {
  padding: 14px 12px 12px;
  border-bottom: 1px solid #e8eef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  background: linear-gradient(
    135deg,
    rgba(59, 130, 246, 0.08) 0%,
    rgba(59, 130, 246, 0.02) 55%,
    rgba(255, 255, 255, 0.96) 100%
  );
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.header-icon {
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.header-icon i {
  font-size: 18px;
}

.header-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-copy span {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.header-copy small {
  font-size: 12px;
  color: #64748b;
  line-height: 1.2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.org-tree-header :deep(.n-button) {
  color: #475569;
}

.org-tree-header :deep(.n-button:hover) {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
}

.org-tree-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 8px 12px;
}

.org-tree-content :deep(.n-spin-content) {
  width: 100%;
  align-items: stretch;
}

.org-tree-collapsed-hint {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px 6px;
  color: #64748b;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.org-tree-collapsed-hint i {
  font-size: 22px;
}

.org-tree-collapsed-hint span {
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  font-size: 12px;
  font-weight: 600;
}

.org-tree-collapsed-hint:hover {
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
}

.org-tree-collapsed-hint.has-active-filter {
  color: #2563eb;
}

.org-tree-all-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.org-tree-all-node:hover {
  background-color: #f8fafc;
  border-color: #dbe4f0;
}

.org-tree-all-node.is-selected {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12) 0%, rgba(59, 130, 246, 0.08) 100%) !important;
  border-color: rgba(37, 99, 235, 0.18);
  color: #2563eb;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.05);
}

.org-tree-all-node i {
  font-size: 18px;
}

.org-tree-content :deep(.n-tree) {
  font-size: 13px;
}

.org-tree-content :deep(.n-tree-node) {
  align-items: center;
}

.org-tree-content :deep(.n-tree-node-content) {
  padding: 8px 10px;
  border-radius: 10px;
  color: #334155;
  transition: all 0.2s ease;
}

.org-tree-content :deep(.n-tree-node-content:hover) {
  background-color: #f8fafc;
}

.org-tree-content :deep(.n-tree-node-content--selected) {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12) 0%, rgba(59, 130, 246, 0.08) 100%) !important;
  color: #2563eb;
}

.org-tree-content :deep(.n-tree-node-switcher) {
  color: #64748b;
}

.org-tree-content :deep(.n-tree-node-indent) {
  width: 12px;
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
  border-radius: 14px;
  border: 1px solid #dbe4f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
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
  box-shadow: 0 12px 30px rgba(2, 6, 23, 0.35);
}

.dark .org-tree-header {
  background: linear-gradient(
    135deg,
    rgba(37, 99, 235, 0.18) 0%,
    rgba(30, 41, 59, 0.94) 58%,
    rgba(15, 23, 42, 0.96) 100%
  );
  border-bottom-color: #334155;
}

.dark .org-tree-header .header-copy span {
  color: #f1f5f9;
}

.dark .org-tree-header .header-copy small {
  color: #94a3b8;
}

.dark .header-icon {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
}

.dark .org-tree-content {
  background: #0f172a;
}

.dark .org-tree-collapsed-hint {
  color: #94a3b8;
}

.dark .org-tree-collapsed-hint:hover,
.dark .org-tree-collapsed-hint.has-active-filter {
  background: rgba(37, 99, 235, 0.14);
  color: #60a5fa;
}

.dark .org-tree-all-node {
  color: #e2e8f0;
}

.dark .org-tree-all-node:hover {
  background-color: #162033;
  border-color: #334155;
}

.dark .org-tree-all-node.is-selected {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.2) 0%, rgba(30, 64, 175, 0.12) 100%) !important;
  color: #60a5fa;
}

.dark .org-tree-content :deep(.n-tree-node-content) {
  color: #e2e8f0;
}

.dark .org-tree-content :deep(.n-tree-node-content:hover) {
  background-color: #162033;
}

.dark .org-tree-content :deep(.n-tree-node-content--selected) {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.2) 0%, rgba(30, 64, 175, 0.12) 100%) !important;
  color: #60a5fa;
}

.dark .org-tree-header :deep(.n-button) {
  color: #cbd5e1;
}

.dark .org-tree-header :deep(.n-button:hover) {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.12);
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
  box-shadow: 0 12px 30px rgba(2, 6, 23, 0.28);
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

@media (max-width: 1200px) {
  .org-tree-panel {
    width: 224px;
    min-width: 224px;
  }
}

@media (max-width: 960px) {
  .user-layout {
    flex-direction: column;
  }

  .org-tree-panel,
  .org-tree-panel.is-collapsed {
    width: 100%;
    min-width: 0;
  }

  .org-tree-collapsed-hint {
    flex-direction: row;
    padding: 12px;
  }

  .org-tree-collapsed-hint span {
    writing-mode: initial;
    letter-spacing: 0;
  }
}
</style>
