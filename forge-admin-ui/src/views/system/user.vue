<template>
  <div class="system-user-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/user"
      :api-config="{
        list: 'get@/system/user/page',
        detail: 'post@/system/user/getById',
        add: 'post@/system/user/add',
        update: 'post@/system/user/edit',
        delete: 'post@/system/user/remove'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      :modal-width="'900px'"
      add-button-text="新增用户"
    >
      <!-- 自定义操作列 -->
      <template #table-action="{ row }">
        <div class="flex items-center gap-8">
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleEdit(row)"
          >
            编辑
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleAuth(row)"
          >
            授权
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleOrg(row)"
          >
            组织
          </a>
          <span class="text-gray-300">|</span>
          <n-dropdown
            trigger="click"
            :options="getMoreOptions(row)"
            @select="(key) => handleMoreAction(key, row)"
          >
            <a class="text-primary cursor-pointer hover:text-primary-hover">更多</a>
          </n-dropdown>
          <span class="text-gray-300" v-if="row.id!==1">|</span>
          <a
            v-if="row.id!==1"
            class="text-error cursor-pointer hover:text-error-hover"
            @click="handleDelete(row)"
          >
            删除
          </a>
        </div>
      </template>
    </AiCrudPage>

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
          <n-button @click="resetPwdModalVisible = false">取消</n-button>
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
              @update:expanded-keys="handleExpandedKeysChange"
              @update:checked-keys="handleCheckedKeysChange"
              key-field="id"
              label-field="roleName"
              children-field="children"
            />
            <n-empty v-else description="暂无角色数据" />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="authModalVisible = false">取消</n-button>
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
              @update:expanded-keys="handleOrgExpandedKeysChange"
              @update:checked-keys="handleOrgCheckedKeysChange"
              key-field="id"
              label-field="orgName"
              children-field="children"
            />
            <n-empty v-else description="暂无组织数据" />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="orgModalVisible = false">取消</n-button>
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
import { ref, h } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemUser' })

const crudRef = ref(null)
const treeRef = ref(null)
const orgTreeRef = ref(null)

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
  password: ''
})
const resetPwdRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码不能少于6位', trigger: 'blur' }]
}

// 组织相关
const orgModalVisible = ref(false)
const orgLoading = ref(false)
const orgSubmitLoading = ref(false)
const orgTreeData = ref([])
const checkedOrgKeys = ref([])
const mainOrgId = ref(null)
const orgTreeExpandAll = ref(true)
const orgTreeExpandedKeys = ref([])

// 用户类型选项
const userTypeOptions = [
  { label: '系统管理员', value: 0 },
  { label: '租户管理员', value: 1 },
  { label: '普通用户', value: 2 }
]

// 用户状态选项
const userStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
  { label: '锁定', value: 2 }
]

// 性别选项
const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    props: {
      placeholder: '请输入用户名'
    }
  },
  {
    field: 'realName',
    label: '真实姓名',
    type: 'input',
    props: {
      placeholder: '请输入真实姓名'
    }
  },
  {
    field: 'phone',
    label: '手机号',
    type: 'input',
    props: {
      placeholder: '请输入手机号'
    }
  },
  {
    field: 'userStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: userStatusOptions
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'username',
    label: '用户名',
    width: 150
  },
  {
    prop: 'realName',
    label: '真实姓名',
    width: 120
  },
  {
    prop: 'userType',
    label: '用户类型',
    width: 120,
    render: (row) => {
      const option = userTypeOptions.find(opt => opt.value === row.userType)
      return option ? option.label : '-'
    }
  },
  {
    prop: 'phone',
    label: '手机号',
    width: 130
  },
  {
    prop: 'email',
    label: '邮箱',
    width: 180
  },
  {
    prop: 'gender',
    label: '性别',
    width: 80,
    render: (row) => {
      const option = genderOptions.find(opt => opt.value === row.gender)
      return option ? option.label : '-'
    }
  },
  {
    prop: 'userStatus',
    label: '状态',
    width: 80,
    render: (row) => {
      const statusMap = {
        0: { text: '禁用', type: 'error' },
        1: { text: '正常', type: 'success' },
        2: { text: '锁定', type: 'warning' }
      }
      const config = statusMap[row.userStatus] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 150
  },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    _slot: 'action'
  }
]

// 编辑表单配置
const editSchema = [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    rules: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    props: {
      placeholder: '请输入用户名'
    }
  },
  {
    field: 'realName',
    label: '真实姓名',
    type: 'input',
    rules: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    props: {
      placeholder: '请输入真实姓名'
    }
  },
  {
    field: 'password',
    label: '密码',
    type: 'input',
    rules: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    props: {
      type: 'password',
      placeholder: '请输入密码'
    },
    vIf: (formData) => !formData.id // 只在新增时显示
  },
  {
    field: 'userType',
    label: '用户类型',
    type: 'select',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择用户类型', trigger: 'change' }],
    props: {
      placeholder: '请选择用户类型',
      options: userTypeOptions
    }
  },
  {
    type: 'divider',
    label: '联系信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'phone',
    label: '手机号',
    type: 'input',
    rules: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    props: {
      placeholder: '请输入手机号'
    }
  },
  {
    field: 'email',
    label: '邮箱',
    type: 'input',
    rules: [
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ],
    props: {
      placeholder: '请输入邮箱'
    }
  },
  {
    field: 'idCard',
    label: '身份证号',
    type: 'input',
    rules: [
      { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
    ],
    props: {
      placeholder: '请输入身份证号'
    }
  },
  {
    field: 'gender',
    label: '性别',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: genderOptions
    }
  },
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'userStatus',
    label: '用户状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: userStatusOptions
    }
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 3
    }
  }
]

// 更多操作选项
function getMoreOptions(row) {
  const options = [
    { label: '重置密码', key: 'resetPwd', icon: () => h('i', { class: 'i-material-symbols:lock-reset' }) }
  ]

  if (row.id !== 1) { // 超级管理员不能被禁用
    if (row.userStatus === 1) {
      options.push({ label: '禁用', key: 'disable', icon: () => h('i', { class: 'i-material-symbols:block' }) })
    } else {
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
            password: resetPwdForm.value.password
          }
        })
        if (res.code === 200) {
          window.$message.success('重置成功')
          resetPwdModalVisible.value = false
        }
      } catch (error) {
        window.$message.error('重置失败')
      } finally {
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
          params: { id: row.id, status }
        })
        if (res.code === 200) {
          window.$message.success(`${actionText}成功`)
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error(`${actionText}失败`)
      }
    }
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
          params: { id: row.id }
        })
        if (res.code === 200) {
          window.$message.success(`用户已启用`)
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error(`启用失败`)
      }
    }
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
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}

// 授权
async function handleAuth(row) {
  currentUser.value = row
  authModalVisible.value = true

  // 加载角色列表和已选中的角色
  await loadRoleList()
  await loadUserRoles(row.id)
}

// 获取所有节点的 key
function getAllKeys(list, keys = []) {
  list.forEach(item => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 加载角色列表
async function loadRoleList() {
  try {
    authLoading.value = true
    const res = await request.get('/system/role/page', {
      params: { pageNum: 1, pageSize: 1000 }
    })
    if (res.code === 200) {
      // 将角色列表转换为树形结构（这里简化处理，实际可能需要根据业务调整）
      roleTreeData.value = (res.data.list || res.data.records || []).map(role => ({
        id: role.id,
        roleName: role.roleName,
        roleKey: role.roleKey
      }))

      // 如果默认展开，收集所有节点的 key
      if (treeExpandAll.value) {
        treeExpandedKeys.value = getAllKeys(roleTreeData.value)
      }
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
    window.$message.error('加载角色列表失败')
  } finally {
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
  } catch (error) {
    console.error('加载用户角色失败:', error)
    window.$message.error('加载用户角色失败')
  } finally {
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
  } else {
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
      checkedRoleKeys.value
    )
    if (res.code === 200) {
      window.$message.success('授权成功')
      authModalVisible.value = false
    }
  } catch (error) {
    console.error('授权失败:', error)
    window.$message.error('授权失败')
  } finally {
    authSubmitLoading.value = false
  }
}

// 组织管理
async function handleOrg(row) {
  currentUser.value = row
  orgModalVisible.value = true
  mainOrgId.value = null
  checkedOrgKeys.value = []

  // 加载组织树和用户已绑定的组织
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
      // 如果默认展开，收集所有节点的 key
      if (orgTreeExpandAll.value) {
        orgTreeExpandedKeys.value = getAllKeys(orgTreeData.value)
      }
    }
  } catch (error) {
    console.error('加载组织列表失败:', error)
    window.$message.error('加载组织列表失败')
  } finally {
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
      // 查找主组织
      const orgId = res.data.find(id => {
        // 这里简化处理，实际应该后端返回主组织ID
        // 暂时将第一个选中的组织作为主组织
        return id === checkedOrgKeys.value[0]
      })
      mainOrgId.value = checkedOrgKeys.value.length > 0 ? checkedOrgKeys.value[0] : null
    }
  } catch (error) {
    console.error('加载用户组织失败:', error)
    window.$message.error('加载用户组织失败')
  } finally {
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
  // 如果主组织不在选中列表中，清空主组织
  if (mainOrgId.value && !keys.includes(mainOrgId.value)) {
    mainOrgId.value = null
  }
}

// 组织展开/折叠所有
function toggleOrgExpandAll() {
  orgTreeExpandAll.value = !orgTreeExpandAll.value
  if (orgTreeExpandAll.value) {
    orgTreeExpandedKeys.value = getAllKeys(orgTreeData.value)
  } else {
    orgTreeExpandedKeys.value = []
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
        mainOrgId: mainOrgId.value
      }
    )
    if (res.code === 200) {
      window.$message.success('组织绑定成功')
      orgModalVisible.value = false
      crudRef.value?.refresh()
    }
  } catch (error) {
    console.error('组织绑定失败:', error)
    window.$message.error('组织绑定失败')
  } finally {
    orgSubmitLoading.value = false
  }
}
</script>

<style scoped>
.system-user-page {
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
  padding: 12px;
  background-color:  #d9d7d7;
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
</style>
