<template>
  <div class="callback-container">
    <div class="callback-card">
      <div class="callback-icon">
        <i v-if="loading" class="ai-icon:loader animate-spin" />
        <i v-else-if="success" class="ai-icon:check-circle" style="color: #22C55E" />
        <i v-else class="ai-icon:x-circle" style="color: #EF4444" />
      </div>
      <h2 class="callback-title">
        {{ message }}
      </h2>
      <p class="callback-desc">
        {{ detailMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import mainApi from '@/api'
import { useAuthStore, usePermissionStore, useUserStore } from '@/store'
import { lStorage } from '@/utils'
import { initKeyExchange } from '@/utils/crypto/key-exchange'
import { request } from '@/utils/http'
import api from './api'

const authStore = useAuthStore()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const loading = ref(true)
const success = ref(false)
const message = ref('正在处理授权...')
const detailMessage = ref('请稍候...')

async function handleCallback() {
  const { code, state } = route.query

  // 从 state 中解析 platform，格式：platform_randomUUID
  let platform = null
  const stateCode = state
  if (state && state.includes('_')) {
    const parts = state.split('_')
    platform = parts[0]
  }

  if (!code || !state || !platform) {
    loading.value = false
    success.value = false
    message.value = '授权参数缺失'
    detailMessage.value = '缺少必要的授权参数，请重新尝试登录'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
    return
  }

  try {
    // 1. 调用回调接口获取 AuthUser
    const callbackRes = await api.socialCallback({
      platform,
      code,
      state: stateCode,
    })

    if (callbackRes.code !== 200 || !callbackRes.data) {
      loading.value = false
      success.value = false
      message.value = '授权失败'
      detailMessage.value = callbackRes.msg || '第三方平台授权失败'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    const authUser = callbackRes.data
    message.value = '正在登录...'

    // 2. 调用登录接口完成登录
    const loginParams = {
      authType: 'oauth2',
      socialPlatform: platform,
      socialUuid: authUser.uuid,
      socialNickname: authUser.nickname,
      socialAvatar: authUser.avatar,
      socialEmail: authUser.email,
    }

    const loginRes = await api.login(loginParams)

    if (loginRes.code !== 200) {
      loading.value = false
      success.value = false
      message.value = '登录失败'
      detailMessage.value = loginRes.msg || '系统登录失败，请重新尝试'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    // 3. 处理登录成功
    await onLoginSuccess(loginRes.data)

    loading.value = false
    success.value = true
    message.value = '登录成功'
    detailMessage.value = '正在跳转到首页...'
  }
  catch (error) {
    console.error('三方登录回调处理失败:', error)
    loading.value = false
    success.value = false
    message.value = '登录异常'
    detailMessage.value = '处理登录时发生错误，请重新尝试'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
}

async function onLoginSuccess(data = {}) {
  if (data.accessToken) {
    authStore.setToken({
      accessToken: data.accessToken,
      tokenType: data.tokenType || 'Bearer',
      expiresIn: data.expiresIn,
    })

    try {
      await initKeyExchange(request, data.accessToken)
    }
    catch (error) {
      console.warn('密钥交换失败，将使用降级方案:', error)
    }
  }

  if (data.userInfo) {
    const loginUser = data.userInfo
    userStore.setUser({
      id: loginUser.userId,
      username: loginUser.username,
      nickName: loginUser.realName || loginUser.username,
      email: loginUser.email,
      phone: loginUser.phone,
      avatar: loginUser.avatar,
      userType: loginUser.userType,
      userStatus: loginUser.userStatus,
      tenantId: loginUser.tenantId,
      roleIds: loginUser.roleIds || [],
      roleKeys: loginUser.roleKeys || [],
      permissions: loginUser.permissions || [],
      apiPermissions: loginUser.apiPermissions || [],
      orgIds: loginUser.orgIds || [],
      mainOrgId: loginUser.mainOrgId,
      roles: loginUser.roleKeys ? Array.from(loginUser.roleKeys) : [],
      userInfo: loginUser,
    })
    lStorage.set('userInfo', loginUser)
  }

  try {
    await loadAndSetMenuData()

    // 通知父窗口登录成功
    if (window.opener) {
      window.opener.postMessage({
        type: 'SOCIAL_LOGIN_SUCCESS',
        data: {
          accessToken: data.accessToken,
          tokenType: data.tokenType,
          expiresIn: data.expiresIn,
        },
      }, '*')
      // 关闭子窗口
      window.close()
    }
    else {
      // 如果没有父窗口，直接跳转
      const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'
      router.push(defaultRedirectPath)
    }
  }
  catch (error) {
    console.error(error)
    if (window.opener) {
      window.opener.postMessage({
        type: 'SOCIAL_LOGIN_FAILED',
        error: error.message,
      }, '*')
      window.close()
    }
    else {
      router.push('/login')
    }
  }
}

async function loadAndSetMenuData() {
  try {
    const permissionStore = usePermissionStore()
    const res = await mainApi.getMenu(1)
    if (res.code === 200 && res.data) {
      permissionStore.setMenuData(res.data)
    }

    let waitCount = 0
    while (!permissionStore.menuDataLoaded && waitCount < 50) {
      await new Promise(resolve => setTimeout(resolve, 100))
      waitCount++
    }
  }
  catch (error) {
    console.error('获取菜单数据失败:', error)
  }
}

onMounted(() => {
  handleCallback()
})
</script>

<style scoped>
.callback-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #bfdbfe 100%);
}

.callback-card {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  padding: 2.5rem 2rem;
  text-align: center;
  max-width: 400px;
}

.callback-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  font-size: 2rem;
}

.callback-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.callback-desc {
  font-size: 0.875rem;
  color: #64748b;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
