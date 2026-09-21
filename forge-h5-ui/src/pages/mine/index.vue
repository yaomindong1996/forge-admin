<template>
  <view class="mine-page">
    <view class="mine-content">
      <view class="mine-header">
        <text class="mine-title">账户</text>
        <text class="mine-subtitle">账号、租户与安全设置</text>
      </view>

      <view class="profile-card">

        <AiImageUpload
          :model-value="rawAvatarUrl"
          :fallback="DEFAULT_AVATAR_URL"
          business-type="avatar"
          @success="handleAvatarUploadSuccess"
          @upload-start="avatarUploading = true"
          @upload-end="avatarUploading = false"
        >
          <view class="avatar-frame">
            <view class="avatar-inner">
              <AiAuthImage class="avatar-image" :src="rawAvatarUrl" :fallback="DEFAULT_AVATAR_URL" mode="aspectFill" />
            </view>
            <view class="avatar-edit">
              <AiIcon icon="/static/icons/ai-icon/camera.svg" color="#ffffff" size="sm" />
            </view>
            <view v-if="avatarUploading" class="avatar-loading-mask">
              <view class="avatar-loading-spinner" />
            </view>
          </view>
        </AiImageUpload>

        <view class="profile-copy">
          <text class="profile-name">{{ authStore.displayName }}</text>
          <view class="member-badge">
            <AiIcon icon="/static/icons/ai-icon/shield.svg" color="#1f5fbf" size="sm" />
            <text>{{ authStore.roleText }}</text>
          </view>
        </view>

      </view>

      <view class="menu-groups">
        <view
          v-for="(group, groupIndex) in menuGroups"
          :key="groupIndex"
          class="menu-group"
        >
          <view
            v-for="item in group.items"
            :key="item.key"
            class="menu-row"
            :class="{ 'menu-row--danger': item.danger }"
            @click="handleMenu(item)"
          >
            <view class="menu-icon" :class="{ 'menu-icon--danger': item.danger }">
              <AiIcon :icon="item.icon" :color="item.color" size="md" />
            </view>
            <view class="menu-main">
              <text class="menu-label">{{ item.label }}</text>
              <text class="menu-desc">{{ item.desc }}</text>
            </view>
            <AiIcon
              v-if="!item.danger"
              icon="/static/icons/ai-icon/chevron-right.svg"
              color="#94a3b8"
              size="sm"
            />
          </view>
        </view>
      </view>
    </view>

    <AiPopupSheet
      v-model="profileSheetVisible"
      title="个人资料"
      description="同步到当前登录账号资料"
      max-height="88vh"
      body-max-height="calc(88vh - 188rpx - env(safe-area-inset-bottom))"
    >
      <view class="sheet-form">
        <AiImageUpload
          class="avatar-large-upload"
          :model-value="rawAvatarUrl"
          :fallback="DEFAULT_AVATAR_URL"
          business-type="avatar"
          @success="handleAvatarUploadSuccess"
          @upload-start="avatarUploading = true"
          @upload-end="avatarUploading = false"
        >
          <view class="avatar-large">
            <AiAuthImage class="avatar-large-image" :src="rawAvatarUrl" :fallback="DEFAULT_AVATAR_URL" mode="aspectFill" />
            <view class="avatar-large-action">
              <AiIcon icon="/static/icons/ai-icon/camera.svg" color="#ffffff" size="sm" />
              <text>{{ avatarUploading ? '上传中' : '更换头像' }}</text>
            </view>
            <view v-if="avatarUploading" class="avatar-loading-mask avatar-loading-mask--large">
              <view class="avatar-loading-spinner" />
            </view>
          </view>
        </AiImageUpload>
        <AiField v-model="profileForm.username" label="账号" placeholder="请输入账号" clearable />
        <AiField v-model="profileForm.realName" label="姓名" placeholder="请输入姓名" clearable />
        <AiField v-model="profileForm.phone" label="手机号" type="number" placeholder="请输入手机号" clearable />
        <AiField v-model="profileForm.email" label="邮箱" type="text" placeholder="请输入邮箱" clearable />
      </view>
      <template #footer>
        <AiButton block :loading="profileSaving" @click="submitProfile">
          保存资料
        </AiButton>
      </template>
    </AiPopupSheet>

    <AiPopupSheet
      v-model="passwordSheetVisible"
      title="修改密码"
      description="修改后会退出当前登录状态"
      max-height="78vh"
      body-max-height="calc(78vh - 188rpx - env(safe-area-inset-bottom))"
    >
      <view class="sheet-form">
        <AiField v-model="passwordForm.oldPassword" label="当前密码" type="password" placeholder="请输入当前密码" clearable />
        <AiField v-model="passwordForm.newPassword" label="新密码" type="password" placeholder="至少 6 位字符" clearable />
        <AiField v-model="passwordForm.confirmPassword" label="确认密码" type="password" placeholder="再次输入新密码" clearable />
      </view>
      <template #footer>
        <AiButton block :loading="passwordSaving" @click="submitPassword">
          确认修改
        </AiButton>
      </template>
    </AiPopupSheet>

    <AiPopupSheet
      v-model="securitySheetVisible"
      title="安全中心"
      description="查看当前账号和绑定信息"
      max-height="74vh"
      body-max-height="calc(74vh - 172rpx - env(safe-area-inset-bottom))"
    >
      <view class="info-list">
        <view v-for="item in securityItems" :key="item.label" class="info-row">
          <view class="info-icon">
            <AiIcon :icon="item.icon" :color="item.color" size="sm" />
          </view>
          <view class="info-main">
            <text class="info-label">{{ item.label }}</text>
            <text class="info-value">{{ item.value }}</text>
          </view>
        </view>
      </view>
      <template #footer>
        <AiButton block variant="secondary" @click="openPasswordSheet">
          修改登录密码
        </AiButton>
      </template>
    </AiPopupSheet>

    <AiPopupSheet
      v-model="tenantSheetVisible"
      title="切换租户"
      :description="`当前租户：${currentTenantName}`"
      max-height="76vh"
      body-max-height="calc(76vh - 172rpx - env(safe-area-inset-bottom))"
    >
      <view class="tenant-panel">
        <view class="tenant-current-card">
          <view class="tenant-current-icon">
            <AiIcon icon="/static/icons/ai-icon/briefcase.svg" color="#ffffff" size="md" />
          </view>
          <view class="tenant-current-copy">
            <text class="tenant-current-label">当前工作空间</text>
            <text class="tenant-current-name">{{ currentTenantName }}</text>
          </view>
        </view>

        <AiListSkeleton v-if="tenantLoading" :rows="3" compact />

        <view v-else class="tenant-list">
          <view
            v-for="tenant in displayTenantOptions"
            :key="tenant.tenantId"
            class="tenant-row"
            :class="{
              active: isCurrentTenant(tenant),
              switching: switchingTenantId === tenant.tenantId,
            }"
            @click="handleTenantSwitch(tenant)"
          >
            <view class="tenant-row-icon">
              <AiIcon icon="/static/icons/ai-icon/layers.svg" :color="isCurrentTenant(tenant) ? '#1f5fbf' : '#64748b'" size="sm" />
            </view>
            <view class="tenant-row-main">
              <text class="tenant-row-name">{{ tenant.tenantName }}</text>
              <text class="tenant-row-desc">{{ isCurrentTenant(tenant) ? '正在使用' : '切换到此租户' }}</text>
            </view>
            <view v-if="switchingTenantId === tenant.tenantId" class="tenant-row-loading" />
            <AiIcon
              v-else-if="isCurrentTenant(tenant)"
              icon="/static/icons/ai-icon/check-circle.svg"
              color="#1f5fbf"
              size="sm"
            />
          </view>
        </view>
      </view>
      <template #footer>
        <AiButton block variant="secondary" :loading="tenantLoading" @click="loadTenantOptions">
          刷新租户列表
        </AiButton>
      </template>
    </AiPopupSheet>

    <AiPopupSheet
      v-model="settingsSheetVisible"
      title="通用设置"
      description="本机偏好，不影响其他设备"
      max-height="72vh"
      body-max-height="calc(72vh - 172rpx - env(safe-area-inset-bottom))"
    >
      <view class="setting-list">
        <view class="setting-row">
          <view class="setting-copy">
            <text class="setting-title">消息免打扰</text>
            <text class="setting-desc">开启后保留消息红点，不做本机提醒</text>
          </view>
          <switch :checked="messageQuietMode" color="#1f5fbf" @change="toggleQuietMode" />
        </view>
        <view class="setting-row setting-row-button" @click="clearLocalCache">
          <view class="setting-copy">
            <text class="setting-title">清理安全会话缓存</text>
            <text class="setting-desc">清理接口加密会话，不退出登录</text>
          </view>
          <AiIcon icon="/static/icons/ai-icon/trash.svg" color="#ef4444" size="sm" />
        </view>
      </view>
    </AiPopupSheet>

    <AiPopupSheet
      v-model="aboutSheetVisible"
      title="帮助与支持"
      description="移动端常用入口"
      max-height="70vh"
      body-max-height="calc(70vh - 172rpx - env(safe-area-inset-bottom))"
    >
      <view class="support-card">
        <view class="support-icon">
          <AiIcon icon="/static/icons/ai-icon/info.svg" color="#1f5fbf" size="lg" />
        </view>
        <text class="support-title">Forge 移动端</text>
        <text class="support-desc">支持移动端菜单、消息中心、流程待办和账号自助维护。遇到权限或页面打不开时，请先在首页刷新信息。</text>
        <view class="support-actions">
          <AiButton variant="secondary" size="sm" @click="refreshUser">
            刷新信息
          </AiButton>
          <AiButton size="sm" @click="goMessages">
            消息中心
          </AiButton>
        </view>
      </view>
    </AiPopupSheet>

    <AiTabBar active="mine" />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiButton from '@/components/AiButton.vue'
import AiField from '@/components/AiField.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiImageUpload from '@/components/AiImageUpload.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { resetKeyExchange } from '@/utils/crypto/key-exchange'
import { showConfirmDialog } from '@/utils/dialog'
import { DEFAULT_AVATAR_URL } from '@/utils/file'
import { toast } from '@/utils/notify'

const authStore = useAuthStore()
const userInfo = computed(() => authStore.userInfo || {})

const rawAvatarUrl = computed(() => userInfo.value.avatar || '')

const profileSheetVisible = ref(false)
const passwordSheetVisible = ref(false)
const securitySheetVisible = ref(false)
const tenantSheetVisible = ref(false)
const settingsSheetVisible = ref(false)
const aboutSheetVisible = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)
const avatarUploading = ref(false)
const tenantLoading = ref(false)
const switchingTenantId = ref(null)
const messageQuietMode = ref(false)
const tenantOptions = ref([])

const profileForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const maskedPhone = computed(() => maskPhone(userInfo.value.phone || userInfo.value.mobile))
const maskedEmail = computed(() => maskEmail(userInfo.value.email))
const currentTenantId = computed(() => userInfo.value.tenantId)
const currentTenantName = computed(() => {
  const current = tenantOptions.value.find(item => Number(item.tenantId) === Number(currentTenantId.value))
  return current?.tenantName || userInfo.value.tenantName || '当前租户'
})
const displayTenantOptions = computed(() => {
  if (tenantOptions.value.length) {
    return tenantOptions.value
  }
  if (!currentTenantId.value) {
    return []
  }
  return [{
    tenantId: currentTenantId.value,
    tenantName: userInfo.value.tenantName || '当前租户',
  }]
})
const switchableTenantCount = computed(() => {
  const ids = Array.isArray(userInfo.value.tenantIds) ? userInfo.value.tenantIds : []
  return Math.max(displayTenantOptions.value.length, ids.length)
})
const showTenantSwitch = computed(() => switchableTenantCount.value > 1)
const securityItems = computed(() => {
  const items = []
  if (userInfo.value.username) {
    items.push({
      label: '登录账号',
      value: userInfo.value.username,
      icon: '/static/icons/ai-icon/user.svg',
      color: '#1f5fbf',
    })
  }
  if (maskedPhone.value) {
    items.push({
      label: '绑定手机',
      value: maskedPhone.value,
      icon: '/static/icons/ai-icon/phone.svg',
      color: '#1f5fbf',
    })
  }
  if (maskedEmail.value) {
    items.push({
      label: '绑定邮箱',
      value: maskedEmail.value,
      icon: '/static/icons/ai-icon/mail.svg',
      color: '#1f5fbf',
    })
  }
  if (authStore.roleText) {
    items.push({
      label: '角色',
      value: authStore.roleText,
      icon: '/static/icons/ai-icon/shield.svg',
      color: '#1f5fbf',
    })
  }
  return items
})

const menuGroups = computed(() => [
  {
    items: [
      {
        key: 'profile',
        icon: '/static/icons/ai-icon/user.svg',
        label: '个人信息',
        desc: '姓名、手机、邮箱和头像',
        color: '#1f5fbf',
        bgClass: 'bg-blue',
      },
      {
        key: 'password',
        icon: '/static/icons/ai-icon/key.svg',
        label: '修改密码',
        desc: '更新当前登录密码',
        color: '#1f5fbf',
        bgClass: 'bg-purple',
      },
      {
        key: 'security',
        icon: '/static/icons/ai-icon/shield.svg',
        label: '安全中心',
        desc: '账号绑定和角色信息',
        color: '#1f5fbf',
        bgClass: 'bg-indigo',
      },
      ...(showTenantSwitch.value
        ? [{
            key: 'tenant',
            icon: '/static/icons/ai-icon/briefcase.svg',
            label: '切换租户',
            desc: currentTenantName.value,
            color: '#1f5fbf',
            bgClass: 'bg-blue',
          }]
        : []),
      {
        key: 'messages',
        icon: '/static/icons/ai-icon/bell.svg',
        label: '消息中心',
        desc: '站内消息和流程提醒',
        color: '#1f5fbf',
        bgClass: 'bg-cyan',
      },
    ],
  },
  {
    items: [
      {
        key: 'help',
        icon: '/static/icons/ai-icon/help-circle.svg',
        label: '帮助与支持',
        desc: '移动端能力说明',
        color: '#1f5fbf',
        bgClass: 'bg-emerald',
      },
      {
        key: 'settings',
        icon: '/static/icons/ai-icon/settings.svg',
        label: '通用设置',
        desc: '消息提醒和本机缓存',
        color: '#64748b',
        bgClass: 'bg-slate',
      },
      {
        key: 'logout',
        icon: '/static/icons/ai-icon/log-out.svg',
        label: '退出登录',
        desc: '清除当前登录态',
        color: '#ef4444',
        bgClass: 'bg-rose',
        danger: true,
      },
    ],
  },
])

onShow(async () => {
  hideNativeTabBar()
  messageQuietMode.value = uni.getStorageSync('forge_h5_quiet_mode') === '1'
  if (!authStore.menus.length && !authStore.permissions.length) {
    await authStore.fetchAccessSnapshot()
  }
  loadTenantOptions({ silent: true })
})

function hideNativeTabBar() {
  if (typeof uni === 'undefined' || typeof uni.hideTabBar !== 'function') {
    return
  }
  uni.hideTabBar({
    animation: false,
    fail: () => {},
  })
}

function handleMenu(item) {
  const actionMap = {
    profile: openProfileSheet,
    password: openPasswordSheet,
    security: () => { securitySheetVisible.value = true },
    tenant: openTenantSheet,
    messages: goMessages,
    help: () => { aboutSheetVisible.value = true },
    settings: () => { settingsSheetVisible.value = true },
    logout: handleLogout,
  }
  actionMap[item.key]?.()
}

function goMessages() {
  uni.navigateTo({ url: '/pages/message/index' })
}

function openProfileSheet() {
  syncProfileForm()
  profileSheetVisible.value = true
}

function openPasswordSheet() {
  securitySheetVisible.value = false
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordSheetVisible.value = true
}

async function openTenantSheet() {
  tenantSheetVisible.value = true
  await loadTenantOptions()
}

function syncProfileForm() {
  profileForm.username = userInfo.value.username || ''
  profileForm.realName = userInfo.value.realName || userInfo.value.nickName || ''
  profileForm.phone = userInfo.value.phone || userInfo.value.mobile || ''
  profileForm.email = userInfo.value.email || ''
}

async function submitProfile() {
  if (!profileForm.username.trim()) {
    toast('请输入账号', { type: 'warning' })
    return
  }
  profileSaving.value = true
  try {
    await saveProfile({
      username: profileForm.username.trim(),
      realName: profileForm.realName.trim(),
      phone: profileForm.phone.trim(),
      email: profileForm.email.trim(),
      avatar: userInfo.value.avatar || '',
    })
    profileSheetVisible.value = false
    toast('资料已更新', { type: 'success' })
  }
  catch (error) {
    console.error('保存资料失败:', error)
  }
  finally {
    profileSaving.value = false
  }
}

async function saveProfile(payload) {
  const nextProfile = {
    username: payload.username ?? userInfo.value.username,
    realName: payload.realName ?? userInfo.value.realName,
    phone: payload.phone ?? userInfo.value.phone,
    email: payload.email ?? userInfo.value.email,
    avatar: payload.avatar ?? userInfo.value.avatar,
  }
  await api.updateUserProfile(nextProfile)
  authStore.patchUserInfo(nextProfile)
  await authStore.fetchUserInfo()
}

async function handleAvatarUploadSuccess(fileData) {
  const avatar = fileData?.fileId || fileData?.id || fileData?.filePath || fileData?.url || fileData
  if (!avatar) {
    toast('头像上传结果为空', { type: 'error' })
    return
  }
  try {
    await saveProfile({ avatar })
    toast('头像已更新', { type: 'success' })
  }
  catch (error) {
    console.error('保存头像失败:', error)
  }
}

async function submitPassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    toast('请输入当前密码和新密码', { type: 'warning' })
    return
  }
  if (passwordForm.newPassword.length < 6) {
    toast('新密码至少 6 位', { type: 'warning' })
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast('两次输入的新密码不一致', { type: 'warning' })
    return
  }
  passwordSaving.value = true
  try {
    await api.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    toast('密码已修改，请重新登录', { type: 'success' })
    await authStore.logout()
    uni.reLaunch({ url: '/pages/login/index' })
  }
  catch (error) {
    console.error('修改密码失败:', error)
  }
  finally {
    passwordSaving.value = false
  }
}

async function refreshUser() {
  try {
    await authStore.fetchUserInfo()
    await authStore.fetchAccessSnapshot()
    toast('已刷新', { type: 'success' })
  }
  catch (error) {
    console.error('刷新用户信息失败:', error)
  }
}

async function loadTenantOptions(options = {}) {
  if (!authStore.accessToken) {
    return
  }
  tenantLoading.value = true
  try {
    const res = await api.getCurrentTenantOptions()
    tenantOptions.value = normalizeTenantOptions(res?.data)
  }
  catch (error) {
    if (Number(error?.code) === 401 && !authStore.isLogin) {
      return
    }
    if (!options.silent) {
      toast('租户列表加载失败', { type: 'error' })
    }
    console.error('加载租户列表失败:', error)
  }
  finally {
    tenantLoading.value = false
  }
}

async function handleTenantSwitch(tenant) {
  const tenantId = Number(tenant?.tenantId)
  if (!tenantId || tenantId === Number(currentTenantId.value) || switchingTenantId.value) {
    return
  }
  switchingTenantId.value = tenantId
  try {
    const res = await api.switchTenant(tenantId)
    if (res?.code === 200 || res?.data !== undefined) {
      authStore.setMenus([])
      authStore.setPermissions([])
      await authStore.fetchUserInfo()
      await authStore.fetchAccessSnapshot()
      tenantSheetVisible.value = false
      await loadTenantOptions({ silent: true })
      toast(`已切换到${tenant.tenantName || '新租户'}`, { type: 'success' })
      return
    }
    toast('租户切换失败', { type: 'error' })
  }
  catch (error) {
    toast('租户切换失败', { type: 'error' })
    console.error('切换租户失败:', error)
  }
  finally {
    switchingTenantId.value = null
  }
}

function normalizeTenantOptions(list = []) {
  if (!Array.isArray(list)) {
    return []
  }
  return list
    .map((item) => {
      const tenantId = item.tenantId || item.id
      return {
        tenantId,
        tenantName: item.tenantName || item.name || `租户 ${tenantId}`,
      }
    })
    .filter(item => item.tenantId)
}

function isCurrentTenant(tenant) {
  return Number(tenant?.tenantId) === Number(currentTenantId.value)
}

function toggleQuietMode(event) {
  messageQuietMode.value = Boolean(event.detail.value)
  uni.setStorageSync('forge_h5_quiet_mode', messageQuietMode.value ? '1' : '0')
  toast(messageQuietMode.value ? '已开启免打扰' : '已关闭免打扰', { type: 'success' })
}

function clearLocalCache() {
  resetKeyExchange()
  toast('安全会话缓存已清理', { type: 'success' })
}

async function handleLogout() {
  const confirmed = await showConfirmDialog({
    title: '退出登录',
    description: '确认退出当前账号？退出后需要重新登录。',
    icon: 'warning',
    confirmText: '退出登录',
    cancelText: '取消',
    isDestructive: true,
  })
  if (!confirmed) {
    return
  }
  await authStore.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}

function maskPhone(value) {
  const phone = String(value || '').trim()
  if (!phone) {
    return ''
  }
  if (phone.length < 7) {
    return phone
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

function maskEmail(value) {
  const email = String(value || '').trim()
  if (!email) {
    return ''
  }
  const [name, domain] = email.split('@')
  if (!domain) {
    return email
  }
  return `${name.slice(0, 2)}***@${domain}`
}
</script>

<style lang="scss" scoped src="../styles/mine.scss"></style>
