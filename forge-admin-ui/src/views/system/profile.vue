<template>
  <div class="profile-page">
    <n-grid :x-gap="20" :y-gap="20" :cols="24" item-responsive>
      <!-- 左侧：个人信息卡片 -->
      <n-gi span="24 m:8 l:7">
        <n-card :bordered="false" class="profile-info-card">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <div class="avatar-container">
              <n-avatar :size="120" round :src="userStore.avatar || defaultAvatar" class="main-avatar" />
              <div class="avatar-edit-btn" @click="handleEditAvatar">
                <i class="i-material-symbols:photo-camera text-18" />
              </div>
            </div>
            <h3 class="user-name">{{ userStore.realName || userStore.username }}</h3>
            <div class="user-role-tag">
              <n-tag :bordered="false" type="primary" size="small">
                {{ userStore.username }}
              </n-tag>
            </div>
          </div>

          <n-divider class="my-20" />

          <!-- 详细信息列表 -->
          <div class="info-list">
            <div class="info-row">
              <div class="info-icon">
                <i class="i-material-symbols:account-tree-outline" />
              </div>
              <div class="info-content">
                <div class="info-label">所属部门</div>
                <div class="info-value">{{ deptName || '暂无部门' }}</div>
              </div>
            </div>

            <div class="info-row">
              <div class="info-icon">
                <i class="i-material-symbols:shield-person-outline" />
              </div>
              <div class="info-content">
                <div class="info-label">所属角色</div>
                <div class="info-value">{{ roleNames || '暂无角色' }}</div>
              </div>
            </div>

            <div class="info-row">
              <div class="info-icon">
                <i class="i-material-symbols:smartphone-outline" />
              </div>
              <div class="info-content">
                <div class="info-label">手机号码</div>
                <div class="info-value">{{ userStore.phone || '未绑定' }}</div>
              </div>
            </div>

            <div class="info-row">
              <div class="info-icon">
                <i class="i-material-symbols:mail-outline" />
              </div>
              <div class="info-content">
                <div class="info-label">用户邮箱</div>
                <div class="info-value">{{ userStore.email || '未绑定' }}</div>
              </div>
            </div>

            <div class="info-row">
              <div class="info-icon">
                <i class="i-material-symbols:calendar-month-outline" />
              </div>
              <div class="info-content">
                <div class="info-label">创建日期</div>
                <div class="info-value">{{ formatDate(userInfo?.createTime) }}</div>
              </div>
            </div>
          </div>
        </n-card>
      </n-gi>

      <!-- 右侧：编辑区域 -->
      <n-gi span="24 m:16 l:17">
        <n-card :bordered="false" class="edit-card">
          <n-tabs type="segment" animated size="large" :tabs-padding="20">
            <n-tab-pane name="basic">
              <template #tab>
                <div class="tab-title">
                  <i class="i-material-symbols:person-outline mr-8" />
                  基本资料
                </div>
              </template>
              <div class="form-section">
                <n-form
                  ref="profileFormRef"
                  :model="profileForm"
                  :rules="profileRules"
                  label-placement="top"
                  require-mark-placement="right-hanging"
                  size="large"
                >
                  <n-grid :cols="2" :x-gap="24">
                    <n-gi>
                      <n-form-item label="用户名" path="username">
                        <n-input
                          v-model:value="profileForm.username"
                          placeholder="请输入用户名"
                          :input-props="{ autocomplete: 'off' }"
                        >
                          <template #prefix>
                            <i class="i-material-symbols:person text-16 opacity-60" />
                          </template>
                        </n-input>
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="真实姓名" path="realName">
                        <n-input
                          v-model:value="profileForm.realName"
                          placeholder="请输入真实姓名"
                          :input-props="{ autocomplete: 'off' }"
                        >
                          <template #prefix>
                            <i class="i-material-symbols:badge-outline text-16 opacity-60" />
                          </template>
                        </n-input>
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="手机号" path="phone">
                        <n-input
                          v-model:value="profileForm.phone"
                          placeholder="请输入手机号"
                          :input-props="{ autocomplete: 'off' }"
                        >
                          <template #prefix>
                            <i class="i-material-symbols:phone-iphone text-16 opacity-60" />
                          </template>
                        </n-input>
                      </n-form-item>
                    </n-gi>
                    <n-gi>
                      <n-form-item label="邮箱" path="email">
                        <n-input
                          v-model:value="profileForm.email"
                          placeholder="请输入邮箱"
                          :input-props="{ autocomplete: 'off' }"
                        >
                          <template #prefix>
                            <i class="i-material-symbols:alternate-email text-16 opacity-60" />
                          </template>
                        </n-input>
                      </n-form-item>
                    </n-gi>
                  </n-grid>

                  <div class="form-actions">
                    <n-button type="primary" size="large" :loading="profileLoading" @click="handleUpdateProfile">
                      <template #icon>
                        <i class="i-material-symbols:check-circle-outline" />
                      </template>
                      保存修改
                    </n-button>
                    <n-button size="large" @click="resetProfileForm">
                      <template #icon>
                        <i class="i-material-symbols:refresh" />
                      </template>
                      重置
                    </n-button>
                  </div>
                </n-form>
              </div>
            </n-tab-pane>

            <n-tab-pane name="security">
              <template #tab>
                <div class="tab-title">
                  <i class="i-material-symbols:lock-outline mr-8" />
                  安全设置
                </div>
              </template>
              <div class="form-section">
                <n-alert type="info" class="mb-24" :bordered="false">
                  <template #icon>
                    <i class="i-material-symbols:info-outline" />
                  </template>
                  为了您的账户安全，修改密码后需要重新登录
                </n-alert>

                <n-form
                  ref="pwdFormRef"
                  :model="pwdForm"
                  :rules="pwdRules"
                  label-placement="top"
                  require-mark-placement="right-hanging"
                  size="large"
                >
                  <n-form-item label="当前密码" path="oldPassword">
                    <n-input
                      v-model:value="pwdForm.oldPassword"
                      type="password"
                      show-password-on="click"
                      placeholder="请输入当前密码"
                      :input-props="{ autocomplete: 'off' }"
                    >
                      <template #prefix>
                        <i class="i-material-symbols:lock-outline text-16 opacity-60" />
                      </template>
                    </n-input>
                  </n-form-item>

                  <n-divider class="my-16" />

                  <n-grid :cols="2" :x-gap="24">
                    <n-gi>
                      <n-form-item label="新密码" path="password">
                        <n-input
                          v-model:value="pwdForm.password"
                          type="password"
                          show-password-on="click"
                          placeholder="请输入新密码（不少于6位）"
                          :input-props="{ autocomplete: 'new-password' }"
                        >
                          <template #prefix>
                            <i class="i-material-symbols:key-outline text-16 opacity-60" />
                          </template>
                        </n-input>
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
                        >
                          <template #prefix>
                            <i class="i-material-symbols:check-circle-outline text-16 opacity-60" />
                          </template>
                        </n-input>
                      </n-form-item>
                    </n-gi>
                  </n-grid>

                  <div class="form-actions">
                    <n-button type="primary" size="large" :loading="pwdLoading" @click="handleUpdatePwd">
                      <template #icon>
                        <i class="i-material-symbols:shield-lock-outline" />
                      </template>
                      修改密码
                    </n-button>
                    <n-button size="large" @click="resetPwdForm">
                      <template #icon>
                        <i class="i-material-symbols:cancel-outline" />
                      </template>
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
import { ref, onMounted, computed } from 'vue'
import { useAuthStore, useUserStore } from '@/store'
import { request } from '@/utils'
import defaultAvatar from '@/assets/images/avatar.png'

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
  email: ''
})

const profileRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 密码表单
const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = ref({
  oldPassword: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value) => {
  if (value !== pwdForm.value.password) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
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
        email: userInfo.value.email
      }

      // 获取部门名称和角色名称
      fetchExtraInfo()
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 获取额外的显示信息（部门、角色）
async function fetchExtraInfo() {
  // 获取角色名称
  if (userStore.roleIds && userStore.roleIds.length > 0) {
    try {
      const res = await request.get('/system/role/page', {
        params: { pageNum: 1, pageSize: 1000 }
      })
      if (res.code === 200) {
        const allRoles = res.data.list || res.data.records || []
        const userRoles = allRoles.filter(role => userStore.roleIds.includes(role.id))
        roleNames.value = userRoles.map(r => r.roleName).join(', ')
      }
    } catch (e) {
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
            if (item.id === id) return item.orgName
            if (item.children) {
              const name = findOrgName(item.children, id)
              if (name) return name
            }
          }
          return null
        }
        deptName.value = findOrgName(res.data, userStore.userInfo.mainOrgId)
      }
    } catch (e) {
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
            ...profileForm.value
          })
          loadUserInfo()
        }
      } catch (error) {
        window.$message.error('更新失败')
      } finally {
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
            newPassword: pwdForm.value.password
          }
        })
        if (res.code === 200) {
          window.$message.success('密码修改成功，请重新登录')
          // 退出登录
          setTimeout(() => {
            authStore.logout()
          }, 1500)
        }
      } catch (error) {
        window.$message.error(error.message || '密码修改失败')
      } finally {
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
    email: userInfo.value.email
  }
}

// 重置密码表单
function resetPwdForm() {
  pwdForm.value = {
    oldPassword: '',
    password: '',
    confirmPassword: ''
  }
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch {
    return dateStr
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
/* 页面容器 */
.profile-page {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 100px);
}

/* 左侧个人信息卡片 */
.profile-info-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.profile-info-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

/* 头像区域 */
.avatar-section {
  text-align: center;
  padding: 32px 24px 24px;
  background: linear-gradient(135deg, #8fd0fc 0%, #2387f8 100%);
  color: white;
  position: relative;
}

.avatar-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.3;
}

.avatar-container {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.main-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.avatar-edit-btn {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8fd0fc 0%, #2387f8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.avatar-edit-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.user-name {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.user-role-tag {
  position: relative;
  z-index: 1;
}

.user-role-tag :deep(.n-tag) {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);
}

/* 信息列表 */
.info-list {
  padding: 0 16px 16px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 16px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
  cursor: default;
}

.info-row:hover {
  background: #f8f9fa;
  transform: translateX(4px);
}

.info-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #365bf6 0%, #0535d2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  flex-shrink: 0;
  margin-right: 16px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.info-content {
  flex: 1;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
  word-break: break-all;
}

/* 右侧编辑卡片 */
.edit-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.edit-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

/* Tabs 样式 */
.tab-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 500;
}

:deep(.n-tabs-tab) {
  padding: 12px 24px;
}

:deep(.n-tabs-tab.n-tabs-tab--active) {
  font-weight: 600;
}

/* 表单区域 */
.form-section {
  padding: 24px;
  background: #fafafa;
  border-radius: 12px;
  margin-top: 16px;
}

:deep(.n-form-item) {
  margin-bottom: 20px;
}

:deep(.n-form-item-label) {
  font-weight: 500;
  color: #262626;
  font-size: 14px;
}

:deep(.n-input) {
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.n-input:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

:deep(.n-input.n-input--focus) {
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.2);
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
}

.form-actions :deep(.n-button) {
  border-radius: 8px;
  padding: 0 32px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.form-actions :deep(.n-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.form-actions :deep(.n-button--primary:hover) {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.form-actions :deep(.n-button:not(.n-button--primary)) {
  border: 1px solid #d9d9d9;
}

.form-actions :deep(.n-button:not(.n-button--primary):hover) {
  border-color: #667eea;
  color: #667eea;
}

/* Alert 样式 */
:deep(.n-alert) {
  border-radius: 8px;
  border-left: 4px solid #1890ff;
}

/* 分隔线 */
:deep(.n-divider) {
  margin: 16px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-page {
    padding: 12px;
  }

  .avatar-section {
    padding: 24px 16px 20px;
  }

  .main-avatar {
    width: 100px !important;
    height: 100px !important;
  }

  .user-name {
    font-size: 20px;
  }

  .form-section {
    padding: 16px;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions :deep(.n-button) {
    width: 100%;
  }
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.profile-info-card,
.edit-card {
  animation: fadeInUp 0.5s ease-out;
}

.edit-card {
  animation-delay: 0.1s;
}
</style>
