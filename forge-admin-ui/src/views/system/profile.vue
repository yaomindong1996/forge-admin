<template>
  <div class="profile-page">
    <n-grid :x-gap="16" :y-gap="16" :cols="24" item-responsive>
      <!-- 左侧：个人信息卡片 -->
      <n-gi span="24 m:8 l:6">
        <n-card :bordered="false" class="profile-info-card">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <div class="avatar-container">
              <n-avatar :size="80" round :src="userStore.avatar || defaultAvatar" class="main-avatar" />
              <div class="avatar-edit-btn" @click="handleEditAvatar">
                <i class="i-material-symbols:photo-camera text-14" />
              </div>
            </div>
            <div class="user-info-text">
              <div class="user-name">
                {{ userStore.realName || userStore.username }}
              </div>
              <n-tag :bordered="false" size="small" class="user-tag">
                {{ userStore.username }}
              </n-tag>
            </div>
          </div>

          <!-- 详细信息列表 -->
           <div class="info-list">
            <div class="info-row">
              <i class="i-material-symbols:account-tree-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">部门</span>
                <span class="info-value">{{ deptName || '暂无' }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:smartphone-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">角色</span>
                <span class="info-value">{{ roleNames || '暂无' }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:devices info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">客户端</span>
                <span class="info-value">
                  <n-tag v-if="userStore.userInfo?.userClient" :type="getClientTagType(userStore.userInfo.userClient)" size="small" :bordered="false">
                    {{ getClientName(userStore.userInfo.userClient) }}
                  </n-tag>
                  <span v-else>-</span>
                </span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:contact-phone info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">手机</span>
                <span class="info-value">{{ userStore.phone || '未绑定' }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:mail-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">邮箱</span>
                <span class="info-value">{{ userStore.email || '未绑定' }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:calendar-month-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(userInfo?.createTime) }}</span>
              </div>
            </div>
          </div>
        </n-card>
      </n-gi>

      <!-- 右侧：编辑区域 -->
      <n-gi span="24 m:16 l:18">
        <n-card :bordered="false" class="edit-card">
          <n-tabs type="line" animated size="large" :tabs-padding="16">
            <n-tab-pane name="basic">
              <template #tab>
                <span class="tab-title">基本资料</span>
              </template>
              <div class="form-section">
                <n-form
                  ref="profileFormRef"
                  :model="profileForm"
                  :rules="profileRules"
                  label-placement="left"
                  label-width="80"
                  require-mark-placement="right-hanging"
                >
                  <n-grid :cols="2" :x-gap="24" :y-gap="4">
                    <n-gi>
                      <n-form-item label="用户名" path="username">
                        <n-input
                          v-model:value="profileForm.username"
                          placeholder="请输入用户名"
                          :input-props="{ autocomplete: 'off' }"
                        />
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="真实姓名" path="realName">
                        <n-input
                          v-model:value="profileForm.realName"
                          placeholder="请输入真实姓名"
                          :input-props="{ autocomplete: 'off' }"
                        />
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="手机号" path="phone">
                        <n-input
                          v-model:value="profileForm.phone"
                          placeholder="请输入手机号"
                          :input-props="{ autocomplete: 'off' }"
                        />
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="邮箱" path="email">
                        <n-input
                          v-model:value="profileForm.email"
                          placeholder="请输入邮箱"
                          :input-props="{ autocomplete: 'off' }"
                        />
                      </n-form-item>
                    </n-gi>
                  </n-grid>

                  <div class="form-actions">
                    <n-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
                      保存修改
                    </n-button>
                    <n-button @click="resetProfileForm">
                      重置
                    </n-button>
                  </div>
                </n-form>
              </div>
            </n-tab-pane>

            <n-tab-pane name="security">
              <template #tab>
                <span class="tab-title">安全设置</span>
              </template>
              <div class="form-section">
                <n-alert type="warning" class="mb-20" :bordered="false">
                  修改密码后需要重新登录
                </n-alert>

                <n-form
                  ref="pwdFormRef"
                  :model="pwdForm"
                  :rules="pwdRules"
                  label-placement="left"
                  label-width="100"
                  require-mark-placement="right-hanging"
                >
                  <n-form-item label="当前密码" path="oldPassword">
                    <n-input
                      v-model:value="pwdForm.oldPassword"
                      type="password"
                      show-password-on="click"
                      placeholder="请输入当前密码"
                      :input-props="{ autocomplete: 'off' }"
                      style="max-width: 360px"
                    />
                  </n-form-item>

                  <n-grid :cols="2" :x-gap="24" :y-gap="4">
                    <n-gi>
                      <n-form-item label="新密码" path="password">
                        <n-input
                          v-model:value="pwdForm.password"
                          type="password"
                          show-password-on="click"
                          placeholder="不少于6位"
                          :input-props="{ autocomplete: 'new-password' }"
                        />
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="确认新密码" path="confirmPassword">
                        <n-input
                          v-model:value="pwdForm.confirmPassword"
                          type="password"
                          show-password-on="click"
                          placeholder="请再次输入新密码"
                          :input-props="{ autocomplete: 'new-password' }"
                        />
                      </n-form-item>
                    </n-gi>
                  </n-grid>

                  <div class="form-actions">
                    <n-button type="primary" :loading="pwdLoading" @click="handleUpdatePwd">
                      修改密码
                    </n-button>
                    <n-button @click="resetPwdForm">
                      取消
                    </n-button>
                  </div>
                </n-form>
              </div>
            </n-tab-pane>
          </n-tabs>
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import defaultAvatar from '@/assets/images/avatar.png'
import { useAuthStore, useUserStore } from '@/store'
import { request } from '@/utils'

defineOptions({ name: 'Profile' })

const userStore = useUserStore()
const authStore = useAuthStore()
const userInfo = ref(null)
const deptName = ref('')
const roleNames = ref('')

// 基本资料表单
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = ref({
  username: '',
  realName: '',
  phone: '',
  email: '',
})

const profileRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

// 密码表单
const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = ref({
  oldPassword: '',
  password: '',
  confirmPassword: '',
})

function validateConfirmPassword(rule, value) {
  if (value !== pwdForm.value.password) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

// 加载用户信息
async function loadUserInfo() {
  try {
    const res = await request.get('/auth/userInfo')
    if (res.code === 200) {
      userInfo.value = res.data.userInfo || res.data

      // 初始化表单
      profileForm.value = {
        username: userInfo.value.username,
        realName: userInfo.value.realName,
        phone: userInfo.value.phone,
        email: userInfo.value.email,
      }

      // 获取部门名称和角色名称
      fetchExtraInfo()
    }
  }
  catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 获取客户端标签类型
function getClientTagType(clientCode) {
  const typeMap = {
    pc: 'info',
    app: 'success',
    h5: 'warning',
    wechat: 'success',
  }
  return typeMap[clientCode] || 'default'
}

// 获取客户端名称
function getClientName(clientCode) {
  const nameMap = {
    pc: '桌面端',
    app: '移动APP',
    h5: '网页版',
    wechat: '微信端',
  }
  return nameMap[clientCode] || clientCode
}

// 获取额外的显示信息（部门、角色）
async function fetchExtraInfo() {
  // 获取角色名称
  if (userStore.roleIds && userStore.roleIds.length > 0) {
    try {
      const res = await request.get('/system/role/page', {
        params: { pageNum: 1, pageSize: 1000 },
      })
      if (res.code === 200) {
        const allRoles = res.data.list || res.data.records || []
        const userRoles = allRoles.filter(role => userStore.roleIds.includes(role.id))
        roleNames.value = userRoles.map(r => r.roleName).join(', ')
      }
    }
    catch (e) {
      console.error('获取角色信息失败')
    }
  }

  // 获取部门名称
  if (userStore.userInfo?.mainOrgId) {
    try {
      const res = await request.get('/system/org/tree')
      if (res.code === 200) {
        const findOrgName = (list, id) => {
          for (const item of list) {
            if (item.id === id)
              return item.orgName
            if (item.children) {
              const name = findOrgName(item.children, id)
              if (name)
                return name
            }
          }
          return null
        }
        deptName.value = findOrgName(res.data, userStore.userInfo.mainOrgId)
      }
    }
    catch (e) {
      console.error('获取部门信息失败')
    }
  }
}

// 更新基本资料
async function handleUpdateProfile() {
  profileFormRef.value?.validate(async (errors) => {
    if (!errors) {
      try {
        profileLoading.value = true
        const res = await request.post('/system/user/updateProfile', profileForm.value)
        if (res.code === 200) {
          window.$message.success('个人资料更新成功')
          // 更新 store 中的信息
          userStore.setUser({
            ...userStore.userInfo,
            ...profileForm.value,
          })
          loadUserInfo()
        }
      }
      catch (error) {
        window.$message.error('更新失败')
      }
      finally {
        profileLoading.value = false
      }
    }
  })
}

// 修改密码
async function handleUpdatePwd() {
  pwdFormRef.value?.validate(async (errors) => {
    if (!errors) {
      try {
        pwdLoading.value = true
        const res = await request.post('/auth/changePassword', null, {
          params: {
            oldPassword: pwdForm.value.oldPassword,
            newPassword: pwdForm.value.password,
          },
        })
        if (res.code === 200) {
          window.$message.success('密码修改成功，请重新登录')
          // 退出登录
          setTimeout(() => {
            authStore.logout()
          }, 1500)
        }
      }
      catch (error) {
        window.$message.error(error.message || '密码修改失败')
      }
      finally {
        pwdLoading.value = false
      }
    }
  })
}

// 模拟头像修改
function handleEditAvatar() {
  window.$message.info('头像修改功能即将上线')
}

// 重置基本资料表单
function resetProfileForm() {
  profileForm.value = {
    username: userInfo.value.username,
    realName: userInfo.value.realName,
    phone: userInfo.value.phone,
    email: userInfo.value.email,
  }
}

// 重置密码表单
function resetPwdForm() {
  pwdForm.value = {
    oldPassword: '',
    password: '',
    confirmPassword: '',
  }
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr)
    return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
  }
  catch {
    return dateStr
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}

/* 左侧个人信息卡片 */
.profile-info-card {
  border-radius: 12px;
}

/* 头像区域 - 横向布局 */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--n-border-color);
  margin-bottom: 8px;
}

.avatar-container {
  position: relative;
  flex-shrink: 0;
}

.main-avatar {
  border: 3px solid var(--n-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--n-color);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--n-text-color);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  border: 2px solid var(--n-color);
}

.avatar-edit-btn:hover {
  transform: scale(1.1);
}

.user-info-text {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: var(--n-text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-tag {
  background: var(--n-color-hover);
  color: var(--n-text-color-2);
}

/* 信息列表 */
.info-list {
  padding-top: 4px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--n-border-color);
  gap: 12px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row-icon {
  font-size: 18px;
  color: var(--n-text-color-3);
  flex-shrink: 0;
}

.info-row-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.info-label {
  font-size: 13px;
  color: var(--n-text-color-3);
  flex-shrink: 0;
}

.info-value {
  font-size: 13px;
  color: var(--n-text-color);
  font-weight: 500;
  word-break: break-all;
  text-align: right;
}

/* 右侧编辑卡片 */
.edit-card {
  border-radius: 12px;
}

/* Tabs 样式 */
.tab-title {
  font-size: 14px;
  font-weight: 500;
}

/* 表单区域 */
.form-section {
  padding-top: 8px;
}

:deep(.n-form-item) {
  margin-bottom: 16px;
}

:deep(.n-form-item-label) {
  font-weight: 500;
  font-size: 13px;
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--n-border-color);
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-page {
    padding: 12px;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
