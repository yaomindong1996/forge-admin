<template>
  <div class="profile-page">
    <div class="profile-layout">
      <!-- 左侧：基本信息卡片 -->
      <div class="profile-left">
        <n-card :bordered="false" class="profile-info-card">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <div class="avatar-container" @click="triggerAvatarUpload">
              <n-avatar
                v-if="avatarSrc"
                :size="72"
                round
                :src="avatarSrc"
                class="profile-avatar"
              />
              <n-avatar
                v-else
                :size="72"
                round
                :style="{ backgroundColor: 'var(--primary-500)', fontSize: '28px' }"
                class="profile-avatar"
              >
                {{ avatarText }}
              </n-avatar>
              <n-upload
                ref="avatarUploadRef"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :data="{ businessType: 'avatar' }"
                :max="1"
                accept=".png,.jpg,.jpeg,.webp"
                :show-file-list="false"
                style="display: none"
                @finish="handleAvatarUploadFinish"
              />
              <div class="avatar-edit-btn" @click.stop="triggerAvatarUpload">
                <i class="i-material-symbols:photo-camera text-14" />
              </div>
            </div>
            <div class="user-title-area">
              <div class="display-name">
                {{ userStore.realName || userStore.username }}
                <i class="i-material-symbols:edit-outline edit-name-icon" @click="openEditProfileModal" />
              </div>
              <div class="user-id-text">
                ID {{ userStore.userId }}
              </div>
            </div>
          </div>

          <!-- 用户信息列表 -->
          <div class="info-list">
            <div class="info-row">
              <i class="i-material-symbols:person-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">用户名</span>
                <span class="info-value">{{ userStore.username || '-' }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:smartphone-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">手机</span>
                <span class="info-value">{{ maskPhone(userStore.phone) }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:mail-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">邮箱</span>
                <span class="info-value">{{ maskEmail(userStore.email) }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:account-tree-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">部门</span>
                <span class="info-value">{{ deptName || '暂无' }}</span>
              </div>
            </div>
            <div class="info-row">
              <i class="i-material-symbols:badge-outline info-row-icon" />
              <div class="info-row-content">
                <span class="info-label">角色</span>
                <span class="info-value">{{ roleNames || '暂无' }}</span>
              </div>
            </div>
          </div>

          <!-- 注册时间 -->
          <div class="register-time">
            注册于 {{ formatDate(userInfo?.createTime) }}
          </div>
        </n-card>
      </div>

      <!-- 右侧：安全设置 + 第三方账号 -->
      <div class="profile-right">
        <!-- 安全设置卡片 -->
        <n-card :bordered="false" class="settings-card">
          <template #header>
            <span class="card-title">安全设置</span>
          </template>

          <div class="security-list">
            <!-- 安全手机 -->
            <div class="security-item">
              <div class="security-item-left">
                <div class="security-icon-wrapper">
                  <i class="i-material-symbols:smartphone-outline security-icon" />
                </div>
                <div class="security-item-info">
                  <div class="security-item-header">
                    <span class="security-item-title">安全手机</span>
                    <n-tag v-if="userStore.phone" :bordered="false" size="small" type="success">
                      已绑定
                    </n-tag>
                    <n-tag v-else :bordered="false" size="small" type="warning">
                      未绑定
                    </n-tag>
                  </div>
                  <div class="security-item-desc">
                    {{ userStore.phone ? `${maskPhone(userStore.phone)} 可用于登录、身份验证、密码找回、通知接收` : '绑定手机号可提升账号安全性' }}
                  </div>
                </div>
              </div>
              <n-button text type="primary" @click="openPhoneModal">
                {{ userStore.phone ? '修改' : '绑定' }}
              </n-button>
            </div>

            <!-- 安全邮箱 -->
            <div class="security-item">
              <div class="security-item-left">
                <div class="security-icon-wrapper">
                  <i class="i-material-symbols:mail-outline security-icon" />
                </div>
                <div class="security-item-info">
                  <div class="security-item-header">
                    <span class="security-item-title">安全邮箱</span>
                    <n-tag v-if="userStore.email" :bordered="false" size="small" type="success">
                      已绑定
                    </n-tag>
                    <n-tag v-else :bordered="false" size="small" type="warning">
                      未绑定
                    </n-tag>
                  </div>
                  <div class="security-item-desc">
                    {{ userStore.email ? `${maskEmail(userStore.email)} 可用于登录、身份验证、密码找回、通知接收` : '绑定邮箱可提升账号安全性' }}
                  </div>
                </div>
              </div>
              <n-button text type="primary" @click="openEmailModal">
                {{ userStore.email ? '修改' : '绑定' }}
              </n-button>
            </div>

            <!-- 登录密码 -->
            <div class="security-item">
              <div class="security-item-left">
                <div class="security-icon-wrapper">
                  <i class="i-material-symbols:lock-outline security-icon" />
                </div>
                <div class="security-item-info">
                  <div class="security-item-header">
                    <span class="security-item-title">登录密码</span>
                    <n-tag :bordered="false" size="small" type="success">
                      已设置
                    </n-tag>
                  </div>
                  <div class="security-item-desc">
                    为了您的账号安全，建议定期修改密码
                  </div>
                </div>
              </div>
              <n-button text type="primary" @click="openPwdModal">
                修改
              </n-button>
            </div>
          </div>
        </n-card>

        <!-- 第三方账号卡片 -->
        <n-card :bordered="false" class="settings-card social-card">
          <template #header>
            <span class="card-title">第三方账号</span>
          </template>

          <div class="security-list">
            <div v-for="item in socialBindings" :key="item.platform" class="security-item">
              <div class="security-item-left">
                <div class="security-icon-wrapper">
                  <n-avatar
                    v-if="item.platformLogo"
                    :size="36"
                    :src="item.platformLogo"
                    round
                  />
                  <i v-else class="i-material-symbols:link security-icon" />
                </div>
                <div class="security-item-info">
                  <div class="security-item-header">
                    <span class="security-item-title">绑定 {{ item.platformName }}</span>
                    <n-tag v-if="item.bound" :bordered="false" size="small" type="success">
                      已绑定
                    </n-tag>
                    <n-tag v-else :bordered="false" size="small" type="warning">
                      未绑定
                    </n-tag>
                  </div>
                  <div class="security-item-desc">
                    <template v-if="item.bound">
                      {{ item.nickname || item.email || '已绑定' }} · 绑定后，可通过 {{ item.platformName }} 进行登录
                    </template>
                    <template v-else>
                      绑定后，可通过 {{ item.platformName }} 进行登录
                    </template>
                  </div>
                </div>
              </div>
              <n-button
                v-if="item.bound"
                text
                type="error"
                :loading="unbindLoading === item.platform"
                @click="handleUnbind(item)"
              >
                解绑
              </n-button>
              <n-button
                v-else
                text
                type="primary"
                @click="handleBind(item)"
              >
                去绑定
              </n-button>
            </div>

            <n-empty v-if="socialBindings.length === 0" description="暂无已启用的第三方登录平台" />
          </div>
        </n-card>
      </div>
    </div>

    <!-- 编辑基本资料弹窗 -->
    <n-modal v-model:show="showEditProfileModal" preset="card" title="编辑基本资料" style="max-width: 420px">
      <n-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-placement="left" label-width="80">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="profileForm.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="真实姓名" path="realName">
          <n-input v-model:value="profileForm.realName" placeholder="请输入真实姓名" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditProfileModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
            确认
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 修改手机号弹窗 -->
    <n-modal v-model:show="showPhoneModal" preset="card" title="修改安全手机" style="max-width: 420px">
      <n-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-placement="left" label-width="80">
        <n-form-item label="新手机号" path="phone">
          <n-input v-model:value="phoneForm.phone" placeholder="请输入新手机号" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPhoneModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="phoneLoading" @click="handleUpdatePhone">
            确认
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 修改邮箱弹窗 -->
    <n-modal v-model:show="showEmailModal" preset="card" title="修改安全邮箱" style="max-width: 420px">
      <n-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-placement="left" label-width="80">
        <n-form-item label="新邮箱" path="email">
          <n-input v-model:value="emailForm.email" placeholder="请输入新邮箱" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEmailModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="emailLoading" @click="handleUpdateEmail">
            确认
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 修改密码弹窗 -->
    <n-modal v-model:show="showPwdModal" preset="card" title="修改登录密码" style="max-width: 420px">
      <n-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-placement="left" label-width="100">
        <n-form-item label="当前密码" path="oldPassword">
          <n-input v-model:value="pwdForm.oldPassword" type="password" show-password-on="click" placeholder="请输入当前密码" />
        </n-form-item>
        <n-form-item label="新密码" path="password">
          <n-input v-model:value="pwdForm.password" type="password" show-password-on="click" placeholder="不少于6位" />
        </n-form-item>
        <n-form-item label="确认新密码" path="confirmPassword">
          <n-input v-model:value="pwdForm.confirmPassword" type="password" show-password-on="click" placeholder="请再次输入新密码" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPwdModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="pwdLoading" @click="handleUpdatePwd">
            确认
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore, useUserStore } from '@/store'
import { request } from '@/utils'
import { getFileUrl } from '@/utils/file'

defineOptions({ name: 'Profile' })

const userStore = useUserStore()
const authStore = useAuthStore()
const userInfo = ref(null)
const deptName = ref('')
const roleNames = ref('')
const socialBindings = ref([])
const unbindLoading = ref(null)

const showPhoneModal = ref(false)
const showEmailModal = ref(false)
const showPwdModal = ref(false)
const showEditProfileModal = ref(false)

const avatarSrc = ref('')
const avatarUploadRef = ref(null)
const uploadUrl = `${import.meta.env.VITE_REQUEST_PREFIX || ''}/api/file/upload`
const uploadHeaders = computed(() => ({
  Authorization: authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
}))

const avatarText = computed(() => {
  const name = userStore.realName || userStore.username
  return name ? name.charAt(0) : 'U'
})

// 基本资料表单
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = ref({ username: '', realName: '' })
const profileRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

// 手机号表单
const phoneFormRef = ref(null)
const phoneLoading = ref(false)
const phoneForm = ref({ phone: '' })
const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
}

// 邮箱表单
const emailFormRef = ref(null)
const emailLoading = ref(false)
const emailForm = ref({ email: '' })
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

// 密码表单
const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', password: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value) => value === pwdForm.value.password,
      message: '两次输入的密码不一致',
      trigger: 'blur',
    },
  ],
}

// 加载用户信息
async function loadUserInfo() {
  try {
    const res = await request.get('/auth/userInfo')
    if (res.code === 200) {
      userInfo.value = res.data.userInfo || res.data
      if (res.data) {
        userStore.setUser(res.data)
      }
      fetchExtraInfo()
      loadAvatar()
    }
  }
  catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 加载社会账号绑定列表
async function loadSocialBindings() {
  try {
    const res = await request.get('/social/user/bindings')
    if (res.code === 200) {
      socialBindings.value = res.data || []
    }
  }
  catch (error) {
    console.error('获取三方绑定信息失败:', error)
  }
}

// 获取额外的显示信息
async function fetchExtraInfo() {
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
    catch (error) {
      console.error('获取角色信息失败', error)
    }
  }

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
    catch (error) {
      console.error('获取部门信息失败', error)
    }
  }
}

// 编辑基本资料
function openEditProfileModal() {
  profileForm.value = {
    username: userStore.username || '',
    realName: userStore.realName || '',
  }
  showEditProfileModal.value = true
}

async function handleUpdateProfile() {
  profileFormRef.value?.validate(async (errors) => {
    if (errors)
      return
    try {
      profileLoading.value = true
      const res = await request.post('/system/user/updateProfile', {
        username: profileForm.value.username,
        realName: profileForm.value.realName,
        phone: userStore.phone,
        email: userStore.email,
      })
      if (res.code === 200) {
        window.$message.success('个人资料更新成功')
        userStore.setUser({
          ...userStore.userInfo,
          username: profileForm.value.username,
          realName: profileForm.value.realName,
        })
        showEditProfileModal.value = false
      }
    }
    catch (error) {
      window.$message.error(error.message || '更新失败')
    }
    finally {
      profileLoading.value = false
    }
  })
}

// 修改手机号
function openPhoneModal() {
  phoneForm.value.phone = userStore.phone || ''
  showPhoneModal.value = true
}

async function handleUpdatePhone() {
  phoneFormRef.value?.validate(async (errors) => {
    if (errors)
      return
    try {
      phoneLoading.value = true
      const res = await request.post('/system/user/updateProfile', {
        username: userStore.username,
        realName: userStore.realName,
        phone: phoneForm.value.phone,
        email: userStore.email,
      })
      if (res.code === 200) {
        window.$message.success('手机号修改成功')
        userStore.setUser({ ...userStore.userInfo, phone: phoneForm.value.phone })
        showPhoneModal.value = false
      }
    }
    catch (error) {
      window.$message.error(error.message || '手机号修改失败')
    }
    finally {
      phoneLoading.value = false
    }
  })
}

// 修改邮箱
function openEmailModal() {
  emailForm.value.email = userStore.email || ''
  showEmailModal.value = true
}

async function handleUpdateEmail() {
  emailFormRef.value?.validate(async (errors) => {
    if (errors)
      return
    try {
      emailLoading.value = true
      const res = await request.post('/system/user/updateProfile', {
        username: userStore.username,
        realName: userStore.realName,
        phone: userStore.phone,
        email: emailForm.value.email,
      })
      if (res.code === 200) {
        window.$message.success('邮箱修改成功')
        userStore.setUser({ ...userStore.userInfo, email: emailForm.value.email })
        showEmailModal.value = false
      }
    }
    catch (error) {
      window.$message.error(error.message || '邮箱修改失败')
    }
    finally {
      emailLoading.value = false
    }
  })
}

// 修改密码
function openPwdModal() {
  pwdForm.value = { oldPassword: '', password: '', confirmPassword: '' }
  showPwdModal.value = true
}

async function handleUpdatePwd() {
  pwdFormRef.value?.validate(async (errors) => {
    if (errors)
      return
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
        showPwdModal.value = false
        setTimeout(() => authStore.logout(), 1500)
      }
    }
    catch (error) {
      window.$message.error(error.message || '密码修改失败')
    }
    finally {
      pwdLoading.value = false
    }
  })
}

// 绑定三方账号
async function handleBind(item) {
  try {
    const res = await request.get(`/social/authUrl/${item.platform}`, {
      needToken: false,
      params: { action: 'bind' },
    })
    if (res.code === 200 && res.data?.authUrl) {
      window.location.href = res.data.authUrl
    }
    else {
      window.$message.error(res.msg || '获取授权链接失败')
    }
  }
  catch (error) {
    window.$message.error(error.message || '获取授权链接失败')
  }
}

// 解绑三方账号
async function handleUnbind(item) {
  window.$dialog.warning({
    title: '确认解绑',
    content: `确定要解绑 ${item.platformName} 账号吗？解绑后无法通过 ${item.platformName} 登录。`,
    positiveText: '确认解绑',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        unbindLoading.value = item.platform
        const res = await request.delete(`/social/unbind/${item.platform}`)
        if (res.code === 200) {
          window.$message.success('解绑成功')
          loadSocialBindings()
        }
        else {
          window.$message.error(res.msg || '解绑失败')
        }
      }
      catch (error) {
        window.$message.error(error.message || '解绑失败')
      }
      finally {
        unbindLoading.value = null
      }
    },
  })
}

// 头像上传
function triggerAvatarUpload() {
  const uploadEl = avatarUploadRef.value?.$el || avatarUploadRef.value
  if (uploadEl) {
    const input = uploadEl.querySelector?.('input[type="file"]')
    if (input)
      input.click()
  }
}

async function loadAvatar() {
  const avatar = userStore.avatar
  if (!avatar) {
    avatarSrc.value = ''
    return
  }
  try {
    const url = getFileUrl(avatar)
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${authStore.accessToken}` },
    })
    if (response.ok) {
      const blob = await response.blob()
      avatarSrc.value = URL.createObjectURL(blob)
    }
  }
  catch {
    avatarSrc.value = ''
  }
}

async function handleAvatarUploadFinish({ event }) {
  try {
    const res = JSON.parse(event.target.response)
    if (res.code === 200 && res.data) {
      const avatar = res.data.fileId || res.data.filePath
      const updateRes = await request.post('/system/user/updateProfile', {
        username: userStore.username,
        realName: userStore.realName,
        phone: userStore.phone,
        email: userStore.email,
        avatar,
      })
      if (updateRes.code === 200) {
        userStore.setUser({ ...userStore.userInfo, avatar })
        loadAvatar()
        window.$message.success('头像更新成功')
      }
    }
    else {
      window.$message.error(res.msg || '头像上传失败')
    }
  }
  catch (error) {
    window.$message.error(error.message || '头像上传失败')
  }
}

// 工具函数
function maskPhone(phone) {
  if (!phone)
    return '未绑定'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

function maskEmail(email) {
  if (!email)
    return '未绑定'
  const at = email.indexOf('@')
  if (at <= 1)
    return email
  return `${email.charAt(0)}****${email.slice(at)}`
}

function formatDate(dateStr) {
  if (!dateStr)
    return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  }
  catch { return dateStr }
}

onMounted(() => {
  loadUserInfo()
  loadSocialBindings()
})
</script>

<style scoped>
.profile-page {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}

.profile-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.profile-left {
  width: 320px;
  flex-shrink: 0;
}

.profile-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 左侧个人信息卡片 — 极淡蓝紫渐变 */
.profile-info-card {
  border-radius: 12px;
  text-align: center;
  background: linear-gradient(150deg, #f5f6ff 0%, #fafaff 100%) !important;
}

.profile-avatar {
  border: 3px solid var(--n-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--n-border-color);
}

.avatar-container {
  position: relative;
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

.user-title-area {
  margin-top: 12px;
}

.display-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--n-text-color);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.edit-name-icon {
  font-size: 16px;
  color: var(--n-text-color-3);
  cursor: pointer;
}

.edit-name-icon:hover {
  color: var(--primary-color);
}

.user-id-text {
  font-size: 12px;
  color: var(--n-text-color-3);
  margin-top: 4px;
}

/* 信息列表 */
.info-list {
  padding-top: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--n-border-color);
  gap: 10px;
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
}

.register-time {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--n-border-color);
  font-size: 12px;
  color: var(--n-text-color-3);
}

/* 右侧卡片 */
.settings-card {
  border-radius: 12px;
}

/* 安全设置 — 极淡蓝灰 */
.settings-card:first-child {
  background: linear-gradient(150deg, #fafbfd 0%, #fdfdfe 100%) !important;
}

/* 第三方账号 — 极淡绿灰 */
.settings-card:last-child {
  background: linear-gradient(150deg, #f8faf7 0%, #fcfdfb 100%) !important;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--n-text-color);
}

/* 安全设置列表 */
.security-list {
  display: flex;
  flex-direction: column;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid var(--n-border-color);
  gap: 12px;
}

.security-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.security-item:first-child {
  padding-top: 0;
}

.security-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.security-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--n-color-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.security-icon {
  font-size: 20px;
  color: var(--n-text-color-2);
}

.security-item-info {
  flex: 1;
  min-width: 0;
}

.security-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.security-item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--n-text-color);
}

.security-item-desc {
  font-size: 12px;
  color: var(--n-text-color-3);
  line-height: 1.5;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-page {
    padding: 12px;
  }

  .profile-layout {
    flex-direction: column;
  }

  .profile-left {
    width: 100%;
  }
}
</style>
