<template>
  <div class="profile-page">
    <div class="profile-layout">
      <!-- 左侧：基本信息卡片 -->
      <div class="profile-left">
        <n-card :bordered="false" class="profile-info-card">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <div class="avatar-container" :class="{ 'is-uploading': avatarUploading }" @click="triggerAvatarUpload">
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
              <input
                ref="avatarInputRef"
                class="avatar-file-input"
                type="file"
                accept="image/png,image/jpeg,image/webp"
                @change="handleAvatarFileChange"
              >
              <div v-if="avatarUploading" class="avatar-upload-mask">
                <n-spin size="small" />
                <span>上传中</span>
              </div>
              <div v-else class="avatar-edit-btn" @click.stop="triggerAvatarUpload">
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
                  <AuthImage
                    v-if="item.platformLogo"
                    :src="item.platformLogo"
                    :lazy="false"
                    :img-style="{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover' }"
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

    <Teleport to="body">
      <div v-if="avatarCropVisible" class="avatar-cropper-full">
        <div class="avatar-cropper-shell">
          <div class="avatar-cropper-topbar">
            <button class="cropper-icon-button" type="button" :disabled="avatarUploading || avatarCropProcessing" @click="cancelAvatarCrop">
              <i class="i-material-symbols:close-rounded" />
            </button>
            <div class="cropper-title">
              <span class="cropper-title-main">编辑头像</span>
              <span class="cropper-title-sub">拖动图片调整位置，滚轮、双指或滑块缩放</span>
            </div>
            <button class="cropper-reset-button" type="button" :disabled="avatarUploading || avatarCropProcessing" @click="resetAvatarCrop">
              重置
            </button>
          </div>

          <div
            ref="avatarCropStageRef"
            class="avatar-cropper-stage"
            @touchstart.stop="startAvatarCropTouch"
            @touchmove.stop.prevent="moveAvatarCropTouch"
            @touchend.stop="endAvatarCropTouch"
            @touchcancel.stop="endAvatarCropTouch"
            @mousedown.stop.prevent="startAvatarCropDrag"
            @wheel.stop.prevent="handleAvatarCropWheel"
          >
            <img
              v-if="avatarCropSource"
              :src="avatarCropSource"
              class="avatar-cropper-image"
              :style="avatarCropImageStyle"
              alt="待裁剪头像"
              draggable="false"
              @load="handleAvatarCropImageLoad"
            >
            <div class="avatar-cropper-frame" :class="`avatar-cropper-frame--${avatarCropShape}`" :style="avatarCropFrameStyle" />
            <div class="avatar-cropper-grid" :class="`avatar-cropper-grid--${avatarCropShape}`" :style="avatarCropFrameStyle">
              <span v-for="item in 4" :key="item" class="avatar-cropper-grid-line" />
            </div>
          </div>

          <div class="avatar-cropper-panel">
            <div class="cropper-shape-tabs">
              <button
                v-for="item in avatarCropShapeOptions"
                :key="item.value"
                class="cropper-shape-tab"
                :class="{ active: avatarCropShape === item.value }"
                type="button"
                :disabled="avatarUploading || avatarCropProcessing"
                @click="avatarCropShape = item.value"
              >
                <i :class="item.icon" />
                <span>{{ item.label }}</span>
              </button>
            </div>

            <div class="cropper-zoom-control">
              <i class="i-material-symbols:image-rounded" />
              <n-slider
                :value="avatarCropZoomPercent"
                :min="0"
                :max="100"
                :step="1"
                :disabled="avatarUploading || avatarCropProcessing"
                @update:value="handleAvatarCropZoomPercent"
              />
              <i class="i-material-symbols:fullscreen-rounded" />
            </div>

            <div class="cropper-actions">
              <button class="cropper-action cropper-action--ghost" type="button" :disabled="avatarUploading || avatarCropProcessing" @click="cancelAvatarCrop">
                取消
              </button>
              <button class="cropper-action cropper-action--primary" type="button" :disabled="avatarUploading || avatarCropProcessing" @click="confirmAvatarCrop">
                {{ avatarUploading || avatarCropProcessing ? '处理中' : '使用头像' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

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
    <n-modal
      v-model:show="showPwdModal"
      preset="card"
      :title="mustChangePassword ? '首次登录必须修改密码' : '修改登录密码'"
      :closable="!mustChangePassword"
      :mask-closable="!mustChangePassword"
      :close-on-esc="!mustChangePassword"
      style="max-width: 420px"
    >
      <n-alert v-if="mustChangePassword" type="warning" class="mb-4">
        当前账号仍在使用初始密码或管理员重置密码，修改后才能继续访问系统功能。
      </n-alert>
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
          <n-button v-if="!mustChangePassword" @click="showPwdModal = false">
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import AuthImage from '@/components/common/AuthImage.vue'
import { useAuthStore, useUserStore } from '@/store'
import { request } from '@/utils'
import { resolveRenderableFileUrl } from '@/utils/file'

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
const avatarInputRef = ref(null)
const avatarUploading = ref(false)
const avatarCropVisible = ref(false)
const avatarCropSource = ref('')
const avatarCropStageRef = ref(null)
const avatarCropZoom = ref(1)
const avatarCropProcessing = ref(false)
const avatarCropImageMeta = ref({ width: 1, height: 1 })
const avatarCropOffset = ref({ x: 0, y: 0 })
const avatarCropDragState = ref(null)
const avatarCropPinchState = ref(null)
const avatarCropSize = ref(280)
const avatarCropShape = ref('circle')
const avatarOutputSize = 512
let avatarCropObjectUrl = ''
const avatarCropShapeOptions = [
  { value: 'circle', label: '圆形', icon: 'i-material-symbols:circle-outline-rounded' },
  { value: 'round', label: '圆角', icon: 'i-material-symbols:rounded-corner-rounded' },
  { value: 'square', label: '方形', icon: 'i-material-symbols:crop-square-rounded' },
]
const uploadUrl = `${import.meta.env.VITE_REQUEST_PREFIX || ''}/api/file/upload`
const uploadHeaders = computed(() => ({
  Authorization: authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
}))
const avatarCropBaseScale = computed(() => Math.max(
  avatarCropSize.value / avatarCropImageMeta.value.width,
  avatarCropSize.value / avatarCropImageMeta.value.height,
))
const avatarCropRenderScale = computed(() => avatarCropBaseScale.value * avatarCropZoom.value)
const avatarCropRenderedSize = computed(() => ({
  width: avatarCropImageMeta.value.width * avatarCropRenderScale.value,
  height: avatarCropImageMeta.value.height * avatarCropRenderScale.value,
}))
const avatarCropZoomPercent = computed(() => Math.round((avatarCropZoom.value - 1) / 2 * 100))
const avatarCropFrameStyle = computed(() => ({
  width: `${avatarCropSize.value}px`,
  height: `${avatarCropSize.value}px`,
}))
const avatarCropImageStyle = computed(() => ({
  width: `${avatarCropRenderedSize.value.width}px`,
  height: `${avatarCropRenderedSize.value.height}px`,
  transform: `translate(calc(-50% + ${avatarCropOffset.value.x}px), calc(-50% + ${avatarCropOffset.value.y}px))`,
}))

const avatarText = computed(() => {
  const name = userStore.realName || userStore.username
  return name ? name.charAt(0) : 'U'
})

const mustChangePassword = computed(() => userStore.forcePasswordChange)

// 基本资料表单
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = ref({ username: '', realName: '' })
const profileRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

function hasAnyPermission(permissions = [], ...targets) {
  const permissionSet = new Set(permissions || [])
  if (permissionSet.has('*') || permissionSet.has('*:*:*'))
    return true
  return targets.some((target) => {
    if (!target)
      return false
    if (permissionSet.has(target))
      return true
    let splitIndex = target.lastIndexOf(':')
    while (splitIndex > 0) {
      const wildcardPermission = `${target.slice(0, splitIndex)}:*`
      if (permissionSet.has(wildcardPermission))
        return true
      splitIndex = target.lastIndexOf(':', splitIndex - 1)
    }
    return false
  })
}

function findOrgName(list = [], id) {
  for (const item of list || []) {
    if (Number(item.id) === Number(id))
      return item.orgName || item.name || ''
    const name = findOrgName(item.children || [], id)
    if (name)
      return name
  }
  return ''
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
      if (mustChangePassword.value) {
        window.$message.warning('请先修改初始密码')
        openPwdModal()
      }
      else {
        fetchExtraInfo()
        loadAvatar()
      }
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
  const roleKeys = userStore.roles || userStore.userInfo?.roleKeys || []
  roleNames.value = Array.isArray(roleKeys) ? roleKeys.join(', ') : ''
  if (userStore.roleIds && userStore.roleIds.length > 0) {
    const canReadRoles = userStore.isAdmin
      || userStore.isTenantAdmin
      || hasAnyPermission(userStore.permissions, 'system:role:list', 'system:role:query')
    if (canReadRoles) {
      try {
        const res = await request.get('/system/role/page', {
          params: { pageNum: 1, pageSize: 1000 },
          needTip: false,
        })
        if (res.code === 200) {
          const allRoles = res.data.list || res.data.records || []
          const userRoles = allRoles.filter(role => userStore.roleIds.includes(role.id))
          roleNames.value = userRoles.map(r => r.roleName || r.roleKey).filter(Boolean).join(', ') || roleNames.value
        }
      }
      catch (error) {
        console.error('获取角色信息失败', error)
      }
    }
  }

  deptName.value = userStore.activeOrgName || userStore.userInfo?.activeOrgName || userStore.userInfo?.deptName || ''
  const targetOrgId = userStore.userInfo?.mainOrgId || userStore.activeOrgId || userStore.userInfo?.activeOrgId
  if (!deptName.value && targetOrgId) {
    try {
      const res = await request.get('/system/org/current/options', { needTip: false })
      if (res.code === 200) {
        deptName.value = findOrgName(res.data, targetOrgId)
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
  if (avatarUploading.value || avatarCropProcessing.value)
    return
  avatarInputRef.value?.click()
}

function handleAvatarFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file)
    return
  if (!file.type?.startsWith('image/')) {
    window.$message.warning('请选择图片文件')
    return
  }
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
    window.$message.warning('仅支持 PNG、JPG、WEBP 格式')
    return
  }
  openAvatarCrop(URL.createObjectURL(file))
}

async function openAvatarCrop(url) {
  releaseAvatarCropObjectUrl()
  avatarCropObjectUrl = url
  avatarCropSource.value = url
  avatarCropZoom.value = 1
  avatarCropOffset.value = { x: 0, y: 0 }
  avatarCropImageMeta.value = { width: 1, height: 1 }
  avatarCropShape.value = 'circle'
  avatarCropVisible.value = true
  lockBodyScroll()
  await nextTick()
  measureAvatarCropStage()
}

function cancelAvatarCrop() {
  if (avatarUploading.value || avatarCropProcessing.value)
    return
  avatarCropVisible.value = false
  avatarCropSource.value = ''
  unlockBodyScroll()
  releaseAvatarCropObjectUrl()
}

function handleAvatarCropImageLoad(event) {
  const image = event.target
  avatarCropImageMeta.value = {
    width: image.naturalWidth || 1,
    height: image.naturalHeight || 1,
  }
  resetAvatarCrop()
}

function measureAvatarCropStage() {
  const rect = avatarCropStageRef.value?.getBoundingClientRect?.()
  const width = rect?.width || window.innerWidth || 960
  const height = rect?.height || Math.max(320, (window.innerHeight || 720) - 260)
  avatarCropSize.value = Math.round(Math.min(width * 0.78, height * 0.72, 340))
  clampAvatarCropOffset()
}

function resetAvatarCrop() {
  avatarCropZoom.value = 1
  avatarCropOffset.value = { x: 0, y: 0 }
  clampAvatarCropOffset()
}

function handleAvatarCropZoomPercent(value) {
  avatarCropZoom.value = 1 + Number(value || 0) / 100 * 2
  clampAvatarCropOffset()
}

function handleAvatarCropWheel(event) {
  const nextZoom = avatarCropZoom.value + (event.deltaY > 0 ? -0.08 : 0.08)
  avatarCropZoom.value = Math.min(3, Math.max(1, nextZoom))
  clampAvatarCropOffset()
}

function startAvatarCropTouch(event) {
  if (event.touches?.length === 2) {
    avatarCropPinchState.value = {
      distance: getAvatarCropTouchDistance(event.touches),
      zoom: avatarCropZoom.value,
    }
    avatarCropDragState.value = null
    return
  }
  const point = event.touches?.[0]
  if (point)
    startAvatarCropPoint(point)
}

function moveAvatarCropTouch(event) {
  if (event.touches?.length === 2 && avatarCropPinchState.value) {
    const ratio = getAvatarCropTouchDistance(event.touches) / Math.max(1, avatarCropPinchState.value.distance)
    avatarCropZoom.value = Math.min(3, Math.max(1, avatarCropPinchState.value.zoom * ratio))
    clampAvatarCropOffset()
    return
  }
  applyAvatarCropDrag(event.touches?.[0])
}

function endAvatarCropTouch() {
  avatarCropDragState.value = null
  avatarCropPinchState.value = null
}

function startAvatarCropDrag(event) {
  startAvatarCropPoint(event)
  window.addEventListener('mousemove', handleAvatarCropMouseMove)
  window.addEventListener('mouseup', endAvatarCropDrag)
}

function startAvatarCropPoint(point) {
  avatarCropDragState.value = {
    x: point.clientX,
    y: point.clientY,
    originX: avatarCropOffset.value.x,
    originY: avatarCropOffset.value.y,
  }
}

function handleAvatarCropMouseMove(event) {
  applyAvatarCropDrag(event)
}

function applyAvatarCropDrag(point) {
  if (!avatarCropDragState.value || !point)
    return
  avatarCropOffset.value = {
    x: avatarCropDragState.value.originX + point.clientX - avatarCropDragState.value.x,
    y: avatarCropDragState.value.originY + point.clientY - avatarCropDragState.value.y,
  }
  clampAvatarCropOffset()
}

function endAvatarCropDrag() {
  avatarCropDragState.value = null
  avatarCropPinchState.value = null
  window.removeEventListener('mousemove', handleAvatarCropMouseMove)
  window.removeEventListener('mouseup', endAvatarCropDrag)
}

function clampAvatarCropOffset() {
  const maxX = Math.max(0, (avatarCropRenderedSize.value.width - avatarCropSize.value) / 2)
  const maxY = Math.max(0, (avatarCropRenderedSize.value.height - avatarCropSize.value) / 2)
  avatarCropOffset.value = {
    x: Math.min(maxX, Math.max(-maxX, avatarCropOffset.value.x)),
    y: Math.min(maxY, Math.max(-maxY, avatarCropOffset.value.y)),
  }
}

function getAvatarCropTouchDistance(touches) {
  const [a, b] = touches
  const x = a.clientX - b.clientX
  const y = a.clientY - b.clientY
  return Math.sqrt(x * x + y * y)
}

async function confirmAvatarCrop() {
  if (avatarUploading.value || avatarCropProcessing.value || !avatarCropSource.value)
    return
  avatarCropProcessing.value = true
  try {
    const file = await cropAvatarToFile()
    await uploadAvatarFile(file)
    avatarCropVisible.value = false
    avatarCropSource.value = ''
    unlockBodyScroll()
    releaseAvatarCropObjectUrl()
  }
  catch (error) {
    window.$message.error(error.message || '头像上传失败')
  }
  finally {
    avatarCropProcessing.value = false
  }
}

function cropAvatarToFile() {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const scale = avatarCropRenderScale.value
      const sourceSize = avatarCropSize.value / scale
      const sx = Math.min(
        avatarCropImageMeta.value.width - sourceSize,
        Math.max(0, avatarCropImageMeta.value.width / 2 - sourceSize / 2 - avatarCropOffset.value.x / scale),
      )
      const sy = Math.min(
        avatarCropImageMeta.value.height - sourceSize,
        Math.max(0, avatarCropImageMeta.value.height / 2 - sourceSize / 2 - avatarCropOffset.value.y / scale),
      )
      const canvas = document.createElement('canvas')
      canvas.width = avatarOutputSize
      canvas.height = avatarOutputSize
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('图片裁剪失败'))
        return
      }
      ctx.clearRect(0, 0, avatarOutputSize, avatarOutputSize)
      applyAvatarCropClip(ctx)
      ctx.drawImage(image, sx, sy, sourceSize, sourceSize, 0, 0, avatarOutputSize, avatarOutputSize)
      const mimeType = avatarCropShape.value === 'square' ? 'image/jpeg' : 'image/png'
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('图片裁剪失败'))
          return
        }
        const ext = mimeType === 'image/png' ? 'png' : 'jpg'
        resolve(new File([blob], `avatar-${Date.now()}.${ext}`, { type: mimeType }))
      }, mimeType, 0.92)
    }
    image.onerror = () => reject(new Error('图片读取失败'))
    image.src = avatarCropSource.value
  })
}

function applyAvatarCropClip(ctx) {
  if (avatarCropShape.value === 'square')
    return
  if (avatarCropShape.value === 'circle') {
    ctx.beginPath()
    ctx.arc(avatarOutputSize / 2, avatarOutputSize / 2, avatarOutputSize / 2, 0, Math.PI * 2)
    ctx.clip()
    return
  }
  const radius = Math.round(avatarOutputSize * 0.18)
  ctx.beginPath()
  ctx.moveTo(radius, 0)
  ctx.arcTo(avatarOutputSize, 0, avatarOutputSize, avatarOutputSize, radius)
  ctx.arcTo(avatarOutputSize, avatarOutputSize, 0, avatarOutputSize, radius)
  ctx.arcTo(0, avatarOutputSize, 0, 0, radius)
  ctx.arcTo(0, 0, avatarOutputSize, 0, radius)
  ctx.closePath()
  ctx.clip()
}

function lockBodyScroll() {
  document.body.style.overflow = 'hidden'
}

function unlockBodyScroll() {
  document.body.style.overflow = ''
}

async function uploadAvatarFile(file) {
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'avatar')
    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: uploadHeaders.value,
      body: formData,
    })
    const res = await response.json()
    if (!response.ok || !(res?.code === 200 || res?.respCode === '0000') || !res?.data) {
      throw new Error(res?.msg || res?.message || '头像上传失败')
    }

    // 业务侧只持久化 fileId；accessUrl 是 COS 临时签名，不能当头像永久地址存
    const avatar = res.data.fileId || res.data.filePath || res.data.id
    if (!avatar)
      throw new Error('头像上传失败')

    const updateRes = await request.post('/system/user/updateProfile', {
      username: userStore.username,
      realName: userStore.realName,
      phone: userStore.phone,
      email: userStore.email,
      avatar,
    })
    if (updateRes.code !== 200) {
      throw new Error(updateRes.msg || '头像更新失败')
    }

    userStore.setUser({ ...userStore.userInfo, avatar })
    // 上传响应里的签名 URL 立刻可展示；再强制走 /api/file/url 刷新，避免沿用过期缓存
    if (res.data.accessUrl)
      avatarSrc.value = res.data.accessUrl
    await loadAvatar(true)
    window.$message.success('头像更新成功')
  }
  finally {
    avatarUploading.value = false
  }
}

function releaseAvatarCropObjectUrl() {
  if (avatarCropObjectUrl?.startsWith('blob:'))
    URL.revokeObjectURL(avatarCropObjectUrl)
  avatarCropObjectUrl = ''
}

async function loadAvatar(forceRefresh = false) {
  const avatar = userStore.avatar
  if (!avatar) {
    avatarSrc.value = ''
    return
  }
  try {
    avatarSrc.value = await resolveRenderableFileUrl(avatar, undefined, forceRefresh)
  }
  catch {
    // 保留上传刚写入的临时 accessUrl，避免刷新失败时立刻变空白
    if (!avatarSrc.value)
      avatarSrc.value = ''
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
  loadUserInfo().then(() => {
    if (!mustChangePassword.value) {
      loadSocialBindings()
    }
  })
})

onBeforeUnmount(() => {
  releaseAvatarCropObjectUrl()
  endAvatarCropDrag()
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
  cursor: pointer;
}

.avatar-container.is-uploading {
  cursor: wait;
}

.avatar-file-input {
  display: none;
}

.avatar-upload-mask {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.68);
  backdrop-filter: blur(2px);
}

.avatar-upload-mask :deep(.n-spin-body) {
  color: #fff;
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

.avatar-cropper-full {
  position: fixed;
  z-index: 2147483000;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
  color: #fff;
  background: rgba(2, 6, 23, 0.62);
  backdrop-filter: blur(8px);
}

.avatar-cropper-shell {
  display: flex;
  flex-direction: column;
  width: min(720px, calc(100vw - 48px));
  height: min(760px, calc(100vh - 56px));
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 18px;
  background:
    radial-gradient(circle at 18% 6%, rgba(37, 99, 235, 0.2), transparent 28%),
    linear-gradient(180deg, #020617, #0f172a);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.42);
}

.avatar-cropper-topbar {
  display: flex;
  min-height: 68px;
  align-items: center;
  gap: 14px;
  padding: 14px 20px 12px;
  box-sizing: border-box;
}

.cropper-icon-button,
.cropper-reset-button,
.cropper-shape-tab,
.cropper-action {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.cropper-icon-button:disabled,
.cropper-reset-button:disabled,
.cropper-shape-tab:disabled,
.cropper-action:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.cropper-icon-button {
  display: flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #fff;
  background: color-mix(in srgb, var(--bg-primary) 10%, transparent);
}

.cropper-icon-button i {
  font-size: 24px;
}

.cropper-title {
  min-width: 0;
  flex: 1;
}

.cropper-title-main,
.cropper-title-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cropper-title-main {
  font-size: 18px;
  font-weight: 800;
}

.cropper-title-sub {
  margin-top: 4px;
  color: rgba(226, 232, 240, 0.72);
  font-size: 13px;
  font-weight: 600;
}

.cropper-reset-button {
  height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  color: #93c5fd;
  font-size: 13px;
  font-weight: 800;
  background: transparent;
}

.cropper-reset-button:hover {
  background: color-mix(in srgb, var(--bg-primary) 8%, transparent);
}

.avatar-cropper-stage {
  position: relative;
  min-height: 320px;
  flex: 1;
  overflow: hidden;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.avatar-cropper-stage:active {
  cursor: grabbing;
}

.avatar-cropper-image {
  position: absolute;
  top: 50%;
  left: 50%;
  max-width: none;
  max-height: none;
  transform-origin: center center;
  -webkit-user-drag: none;
  user-select: none;
}

.avatar-cropper-frame,
.avatar-cropper-grid {
  position: absolute;
  top: 50%;
  left: 50%;
  overflow: hidden;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.avatar-cropper-frame {
  border: 2px solid rgba(255, 255, 255, 0.96);
  box-shadow:
    0 0 0 1px rgba(37, 99, 235, 0.52),
    0 0 0 9999px rgba(2, 6, 23, 0.66),
    0 18px 70px rgba(0, 0, 0, 0.34);
}

.avatar-cropper-frame--circle,
.avatar-cropper-grid--circle {
  border-radius: 9999px;
}

.avatar-cropper-frame--round,
.avatar-cropper-grid--round {
  border-radius: 20%;
}

.avatar-cropper-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
  opacity: 0.58;
}

.avatar-cropper-grid-line {
  border-right: 1px solid rgba(255, 255, 255, 0.32);
  border-bottom: 1px solid rgba(255, 255, 255, 0.32);
}

.avatar-cropper-panel {
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(2, 6, 23, 0.84);
  backdrop-filter: blur(24px);
}

.cropper-shape-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-width: 520px;
  margin: 0 auto;
}

.cropper-shape-tab {
  display: flex;
  height: 42px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 800;
  background: color-mix(in srgb, var(--bg-primary) 8%, transparent);
}

.cropper-shape-tab i {
  font-size: 18px;
}

.cropper-shape-tab.active {
  border-color: rgba(96, 165, 250, 0.6);
  background: linear-gradient(135deg, #2563eb, #0f766e);
}

.cropper-zoom-control {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 14px;
  max-width: 520px;
  margin: 14px auto 0;
  color: var(--text-tertiary);
}

.cropper-zoom-control i {
  font-size: 20px;
}

.cropper-zoom-control :deep(.n-slider-rail__fill) {
  background: var(--bg-primary) !important;
}

.cropper-zoom-control :deep(.n-slider-handle) {
  border-color: #fff !important;
  background: var(--bg-primary) !important;
}

.cropper-actions {
  display: grid;
  grid-template-columns: 0.84fr 1.16fr;
  gap: 12px;
  max-width: 520px;
  margin: 16px auto 0;
}

.cropper-action {
  height: 46px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 900;
}

.cropper-action--ghost {
  color: #e2e8f0;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: color-mix(in srgb, var(--bg-primary) 8%, transparent);
}

.cropper-action--primary {
  color: var(--text-primary);
  background: var(--bg-primary);
}

@media (max-width: 640px) {
  .avatar-cropper-full {
    padding: 14px;
  }

  .avatar-cropper-shell {
    width: calc(100vw - 28px);
    height: min(680px, calc(100vh - 28px));
    border-radius: 14px;
  }
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
