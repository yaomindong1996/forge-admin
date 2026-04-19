<template>
  <div class="login-container">
    <!-- Animated background -->
    <div class="login-bg-animated">
      <div class="gradient-orb orb-1" />
      <div class="gradient-orb orb-2" />
      <div class="gradient-orb orb-3" />
      <div class="grid-pattern" />
    </div>

    <!-- Login card with glassmorphism -->
    <div class="login-card">
      <!-- Left side - Branding -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="logo-wrapper">
            <img src="@/assets/images/logo.png" class="logo-img" alt="Logo">
          </div>
          <h1 class="brand-title">
            {{ title }}
          </h1>
          <p class="brand-subtitle">
            企业级中后台基础框架
          </p>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <i class="ai-icon:shield" />
              </div>
              <div class="feature-text">
                <span class="feature-title">安全可靠</span>
                <span class="feature-desc">企业级安全架构</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <i class="ai-icon:zap" />
              </div>
              <div class="feature-text">
                <span class="feature-title">高效便捷</span>
                <span class="feature-desc">快速开发部署</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <i class="ai-icon:layers" />
              </div>
              <div class="feature-text">
                <span class="feature-title">功能强大</span>
                <span class="feature-desc">插件化架构设计</span>
              </div>
            </div>
          </div>
        </div>
        <!-- Decorative elements -->
        <div class="brand-decoration">
          <div class="deco-circle deco-1" />
          <div class="deco-circle deco-2" />
          <div class="deco-circle deco-3" />
        </div>
      </div>

      <!-- Right side - Login form -->
      <div class="login-form-wrapper">
        <div class="login-form">
          <div class="form-header">
            <h2 class="form-title">
              欢迎回来
            </h2>
            <p class="form-subtitle">
              请登录您的账户
            </p>
          </div>

          <div class="form-body">
            <!-- Username -->
            <div class="form-group">
              <label for="username" class="form-label">用户名</label>
              <div class="input-wrapper">
                <n-input
                  id="username"
                  v-model:value="loginInfo.username"
                  autofocus
                  class="modern-input"
                  placeholder="请输入用户名"
                  :maxlength="20"
                  size="large"
                >
                  <template #prefix>
                    <i class="input-icon ai-icon:user" />
                  </template>
                </n-input>
              </div>
            </div>

            <!-- Password -->
            <div class="form-group">
              <label for="password" class="form-label">密码</label>
              <div class="input-wrapper">
                <n-input
                  id="password"
                  v-model:value="loginInfo.password"
                  class="modern-input"
                  type="password"
                  show-password-on="click"
                  placeholder="请输入密码"
                  :maxlength="20"
                  size="large"
                  @keydown.enter="handleLogin()"
                >
                  <template #prefix>
                    <i class="input-icon ai-icon:lock" />
                  </template>
                </n-input>
              </div>
            </div>

            <!-- 验证码区域 - 根据配置显示不同类型的验证码 -->
            <div class="form-group">
              <!-- 图形验证码 -->
              <template v-if="captchaType === 'graphical'">
                <label for="captcha" class="form-label">验证码</label>
                <div class="captcha-wrapper">
                  <div class="input-wrapper flex-1">
                    <n-input
                      id="captcha"
                      v-model:value="loginInfo.code"
                      class="modern-input"
                      placeholder="请输入验证码"
                      :maxlength="6"
                      size="large"
                      @keydown.enter="handleLogin()"
                    >
                      <template #prefix>
                        <i class="input-icon ai-icon:key" />
                      </template>
                    </n-input>
                  </div>
                  <div
                    class="captcha-image"
                    title="点击刷新验证码"
                    role="button"
                    tabindex="0"
                    @click="refreshCaptcha"
                    @keydown.enter="refreshCaptcha"
                  >
                    <img
                      v-if="captchaImage"
                      :src="captchaImage"
                      alt="验证码"
                      class="captcha-img"
                    >
                    <div v-else class="captcha-loading">
                      <i class="ai-icon:loader animate-spin" />
                    </div>
                  </div>
                </div>
              </template>

              <!-- 滑块验证码 - 已改为浮层弹出形式，点击登录按钮触发 -->
              <template v-if="captchaType === 'slider'">
                <div class="slider-verify-trigger" :class="{ verified: sliderSuccess }" @click="!sliderSuccess && openSliderModal()">
                  <div class="trigger-icon">
                    <i v-if="sliderSuccess" class="ai-icon:check-circle" style="color:#22C55E" />
                    <i v-else class="ai-icon:shield" />
                  </div>
                  <span>{{ sliderSuccess ? '安全验证已通过' : '点击进行安全验证' }}</span>
                  <i v-if="!sliderSuccess" class="trigger-arrow ai-icon:chevron-right" />
                </div>
              </template>

              <!-- 短信验证码 -->
              <template v-if="captchaType === 'sms'">
                <label for="phone" class="form-label">手机号</label>
                <div class="input-wrapper mb-3">
                  <n-input
                    id="phone"
                    v-model:value="loginInfo.phone"
                    class="modern-input"
                    placeholder="请输入手机号"
                    :maxlength="11"
                    size="large"
                  >
                    <template #prefix>
                      <i class="input-icon ai-icon:phone" />
                    </template>
                  </n-input>
                </div>
                <label for="smsCode" class="form-label">短信验证码</label>
                <div class="captcha-wrapper">
                  <div class="input-wrapper flex-1">
                    <n-input
                      id="smsCode"
                      v-model:value="loginInfo.code"
                      class="modern-input"
                      placeholder="请输入短信验证码"
                      :maxlength="6"
                      size="large"
                      @keydown.enter="handleLogin()"
                    >
                      <template #prefix>
                        <i class="input-icon ai-icon:key" />
                      </template>
                    </n-input>
                  </div>
                  <n-button
                    :disabled="smsCountdown > 0 || !isValidPhone"
                    class="sms-button"
                    size="large"
                    @click="sendSmsCode"
                  >
                    {{ smsCountdown > 0 ? `${smsCountdown}s后重发` : '获取验证码' }}
                  </n-button>
                </div>
              </template>
            </div>

            <!-- Remember me -->
            <div class="form-options">
              <n-checkbox
                :checked="isRemember"
                :on-update:checked="(val) => (isRemember = val)"
              >
                <span class="checkbox-label">记住我</span>
              </n-checkbox>
            </div>

            <!-- Submit button -->
            <n-button
              class="login-button"
              type="primary"
              size="large"
              :loading="loading"
              block
              @click="onLoginClick()"
            >
              <span class="button-text">登录</span>
              <i v-if="!loading" class="button-icon ai-icon:arrow-right" />
            </n-button>

            <!-- Social login buttons -->
            <div v-if="socialPlatforms.length > 0" class="social-login-section">
              <div class="social-divider">
                <span class="divider-text">其他登录方式</span>
              </div>
              <div class="social-buttons">
                <button
                  v-for="platform in socialPlatforms"
                  :key="platform.platform"
                  class="social-button"
                  :title="platform.platformName"
                  @click="handleSocialLogin(platform.platform)"
                >
                  <img
                    v-if="platform.platformLogoBase64"
                    :src="`data:image/png;base64,${platform.platformLogoBase64}`"
                    :alt="platform.platformName"
                    class="social-icon"
                  >
                  <i v-else class="social-icon-fallback ai-icon:link" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 滑块验证浮层 -->
  <Transition name="modal">
    <div v-if="showSliderModal" class="slider-modal-overlay" @click.self="closeSliderModal">
      <div class="slider-modal">
        <!-- 关闭按钮 -->
        <button class="slider-modal-close" @click="closeSliderModal">
          <i class="ai-icon:x" />
        </button>

        <!-- 标题区 -->
        <div class="slider-modal-header">
          <div class="slider-modal-icon">
            <i class="ai-icon:shield" />
          </div>
          <h3 class="slider-modal-title">
            安全验证
          </h3>
          <p class="slider-modal-desc">
            请拖动滑块到正确位置，完成拼图
          </p>
        </div>

        <!-- 滑块验证组件 -->
        <div class="slider-modal-body">
          <SlideVerify
            ref="slideVerifyRef"
            :w="340"
            :h="170"
            :slider-l="42"
            :slider-r="8"
            :accuracy="8"
            :imgs="slideImages"
            :show-refresh="true"
            refresh-text="刷新"
            text="拖动滑块完成拼图"
            success-text="验证成功！"
            fail-text="验证失败，请重试"
            @success="onSlideSuccess"
            @fail="onSlideFail"
            @refresh="onSlideRefresh"
          />
        </div>

        <!-- 底部辅助文字 -->
        <p class="slider-modal-tip">
          <i class="ai-icon:info" />
          如果拖动困难，可点击刷新重试
        </p>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { useStorage } from '@vueuse/core'
import { nextTick } from 'vue'
import SlideVerify from 'vue3-slide-verify'
import mainApi from '@/api'
import { useAuthStore, usePermissionStore, useUserStore } from '@/store'
import { lStorage } from '@/utils'
import { encryptPassword, initKeyExchange } from '@/utils/crypto/key-exchange'
import { request } from '@/utils/http'
import api from './api'
import 'vue3-slide-verify/dist/style.css'

const authStore = useAuthStore()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const title = import.meta.env.VITE_TITLE

const loginInfo = ref({
  username: '',
  password: '',
  code: '', // 验证码
  codeKey: '', // 验证码key
  phone: '', // 手机号（短信验证码使用）
})

const captchaImage = ref('') // 验证码图片（Base64）
const captchaExpires = ref(0) // 验证码过期时间

// 验证码类型：graphical(图形验证码), slider(滑块验证码), sms(短信验证码)
const captchaType = ref('graphical')
const loginConfig = ref(null)

// 滑块验证码相关 (vue3-slide-verify)
const slideVerifyRef = ref(null)
const slideImages = ref([]) // 滑块验证码背景图片列表
const sliderSuccess = ref(false)
const sliderFail = ref(false)
const showSliderModal = ref(false) // 滑块浮层显示状态

// 短信验证码相关
const smsCountdown = ref(0)
const smsTimer = ref(null)

const localLoginInfo = lStorage.get('loginInfo')
if (localLoginInfo) {
  loginInfo.value.username = localLoginInfo.username || ''
  loginInfo.value.password = localLoginInfo.password || ''
}

const isRemember = useStorage('isRemember', true)
const loading = ref(false)

// 三方登录平台列表
const socialPlatforms = ref([])
const socialLoading = ref(false)

// 手机号验证
const isValidPhone = computed(() => {
  const phone = loginInfo.value.phone
  return phone && /^1[3-9]\d{9}$/.test(phone)
})

// 获取登录配置
async function loadLoginConfig() {
  try {
    const res = await api.getLoginConfig()
    if (res.code === 200 && res.data) {
      loginConfig.value = res.data
      captchaType.value = res.data.captchaType || 'graphical'

      // 根据验证码类型加载对应的验证码
      await loadCaptchaByType()
    }
  }
  catch (error) {
    console.error('获取登录配置失败:', error)
    // 使用默认配置
    captchaType.value = 'graphical'
    await refreshCaptcha()
  }
}

// 获取已启用的三方登录平台
async function loadSocialPlatforms() {
  try {
    socialLoading.value = true
    const res = await api.getSocialPlatforms()
    if (res.code === 200 && res.data) {
      socialPlatforms.value = res.data.filter(p => p.enabled)
    }
  }
  catch (error) {
    console.error('获取三方登录平台失败:', error)
  }
  finally {
    socialLoading.value = false
  }
}

// 处理三方登录
async function handleSocialLogin(platform) {
  try {
    const res = await api.getSocialAuthUrl(platform)
    if (res.code === 200 && res.data) {
      // 打开授权窗口
      const width = 600
      const height = 500
      const left = (window.innerWidth - width) / 2
      const top = (window.innerHeight - height) / 2

      const authWindow = window.open(
        res.data.authUrl,
        'social_auth',
        `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no,resizable=yes`,
      )

      // 监听授权窗口关闭
      const checkClosed = setInterval(() => {
        if (authWindow.closed) {
          clearInterval(checkClosed)
        }
      }, 500)
    }
    else {
      $message.error(res.msg || '获取授权链接失败')
    }
  }
  catch (error) {
    console.error('获取三方授权链接失败:', error)
    $message.error('获取授权链接失败')
  }
}

// 根据验证码类型加载验证码
async function loadCaptchaByType() {
  switch (captchaType.value) {
    case 'slider':
      // 加载滑块验证码图片
      await loadSlideImages()
      // 重置滑块状态
      sliderSuccess.value = false
      sliderFail.value = false
      loginInfo.value.code = ''
      // 注意：组件实例在模板渲染后才可用，不在此处调用reset
      break
    case 'sms':
      // 短信验证码不需要预加载，用户点击发送时才获取
      break
    case 'graphical':
    default:
      await refreshCaptcha()
      break
  }
}

// 获取图形验证码
async function refreshCaptcha() {
  try {
    const res = await api.getCaptcha()
    if (res.code === 200 && res.data) {
      loginInfo.value.codeKey = res.data.codeKey // 验证码key
      captchaImage.value = res.data.image || '' // 验证码图片（Base64）
      captchaExpires.value = res.data.expiresIn || 300 // 过期时间

      // 清空验证码输入框
      loginInfo.value.code = ''

      // 开发环境在控制台提示验证码
      if (import.meta.env.DEV && res.data.code) {
        console.log('【开发提示】验证码:', res.data.code)
      }
    }
  }
  catch (error) {
    console.error('获取验证码失败:', error)
    $message.error('获取验证码失败')
  }
}

// 加载滑块验证码图片列表
async function loadSlideImages() {
  // 使用可靠的图片源，避免加载失败
  // 实际项目中可以配置自己的图片或使用后端提供的图片
  slideImages.value = [
    'https://picsum.photos/id/10/320/160',
    'https://picsum.photos/id/11/320/160',
    'https://picsum.photos/id/12/320/160',
    'https://picsum.photos/id/13/320/160',
    'https://picsum.photos/id/14/320/160',
  ]
}

// 打开滑块验证浮层
function openSliderModal() {
  showSliderModal.value = true
  // 浮层打开后重置验证状态
  nextTick(() => {
    if (slideVerifyRef.value && typeof slideVerifyRef.value.refresh === 'function') {
      slideVerifyRef.value.refresh()
    }
  })
}

// 关闭滑块验证浮层
function closeSliderModal() {
  showSliderModal.value = false
}

// 登录按钮点击处理
function onLoginClick() {
  if (captchaType.value === 'slider' && !sliderSuccess.value) {
    openSliderModal()
    return
  }
  handleLogin()
}

// 滑块验证成功回调
function onSlideSuccess() {
  sliderSuccess.value = true
  sliderFail.value = false
  loginInfo.value.code = 'verified'
  console.log('滑块验证成功')
  // 延迟关闭浮层再登录，让用户看到成功动画
  setTimeout(() => {
    closeSliderModal()
    handleLogin()
  }, 800)
}

// 滑块验证失败回调
function onSlideFail() {
  sliderFail.value = true
  sliderSuccess.value = false
  loginInfo.value.code = ''
  console.log('滑块验证失败')
}

// 刷新滑块验证码
function onSlideRefresh() {
  sliderSuccess.value = false
  sliderFail.value = false
  loginInfo.value.code = ''
  // 组件会自动刷新，这里可以添加额外的逻辑
}

// 处理登录失败
async function handleLoginFailure() {
  // 根据验证码类型处理
  if (captchaType.value === 'slider') {
    // 滑块验证码需要重置组件
    sliderSuccess.value = false
    sliderFail.value = false
    loginInfo.value.code = ''
    // 等待 DOM 更新后调用组件刷新方法
    await nextTick()
    if (slideVerifyRef.value && typeof slideVerifyRef.value.refresh === 'function') {
      slideVerifyRef.value.refresh()
    }
  }
  else if (captchaType.value === 'graphical') {
    // 图形验证码刷新
    await refreshCaptcha()
  }
  // 短信验证码不需要刷新，保持原样
}

// 发送短信验证码
async function sendSmsCode() {
  if (!isValidPhone.value) {
    $message.warning('请输入正确的手机号')
    return
  }

  try {
    const res = await api.sendSmsCaptcha(loginInfo.value.phone)
    if (res.code === 200 && res.data) {
      if (res.data.status === 'success') {
        $message.success('验证码发送成功')

        // 开发环境在控制台提示验证码
        if (import.meta.env.DEV && res.data.code) {
          console.log('【开发提示】短信验证码:', res.data.code)
        }

        // 开始倒计时
        smsCountdown.value = res.data.interval || 60
        startSmsCountdown()
      }
      else {
        $message.error(res.data.message || '验证码发送失败')
      }
    }
  }
  catch (error) {
    console.error('发送短信验证码失败:', error)
    $message.error('发送短信验证码失败')
  }
}

// 短信验证码倒计时
function startSmsCountdown() {
  if (smsTimer.value) {
    clearInterval(smsTimer.value)
  }

  smsTimer.value = setInterval(() => {
    if (smsCountdown.value > 0) {
      smsCountdown.value--
    }
    else {
      clearInterval(smsTimer.value)
    }
  }, 1000)
}

async function handleLogin() {
  const { username, password, code, codeKey, phone } = loginInfo.value

  // 基础验证
  if (!username || !password)
    return $message.warning('请输入用户名和密码')

  // 根据验证码类型进行验证
  if (captchaType.value === 'slider') {
    if (!sliderSuccess.value) {
      return $message.warning('请完成滑块验证')
    }
    // 滑块验证码使用组件自带的验证，这里只需要确认已通过
    // 为了安全，可以添加一个临时的token或codeKey
  }
  else if (captchaType.value === 'sms') {
    if (!phone)
      return $message.warning('请输入手机号')
    if (!code)
      return $message.warning('请输入短信验证码')
  }
  else {
    if (!code)
      return $message.warning('请输入验证码')
  }

  try {
    loading.value = true
    $message.loading('正在验证，请稍后...', { key: 'login' })

    // 对密码进行 RSA 加密
    const encryptedPassword = await encryptPassword(password, request)

    // 构造登录参数 - 使用新的后端接口格式
    const params = {
      username,
      password: encryptedPassword, // 使用加密后的密码
      code,
      codeKey,
      phone, // 短信验证码时需要
      authType: 'password_captcha', // 使用用户名密码+验证码登录方式
      encrypted: true, // 标记密码已加密
      userClient: 'pc', // 客户端类型：pc端
      appId: import.meta.env.VITE_APP_ID || 'forge_pc_001', // 客户端AppId
      appSecret: import.meta.env.VITE_APP_SECRET || undefined, // 客户端密钥（可选）
    }

    const res = await api.login(params)

    if (res.code === 200) {
      if (isRemember.value) {
        lStorage.set('loginInfo', { username, password })
      }
      else {
        lStorage.remove('loginInfo')
      }
      onLoginSuccess(res.data)
    }
    else {
      // 登录失败后刷新验证码
      await handleLoginFailure()
    }
  }
  catch (error) {
    $message.destroy('login')
    console.error(error)
    // 登录失败后刷新验证码
    await handleLoginFailure()
  }
  loading.value = false
}

async function onLoginSuccess(data = {}) {
  // 设置认证信息 - LoginResult 结构
  if (data.accessToken) {
    authStore.setToken({
      accessToken: data.accessToken,
      tokenType: data.tokenType || 'Bearer',
      expiresIn: data.expiresIn,
    })

    // 密钥交换 - 使用 token 作为会话标识
    try {
      await initKeyExchange(request, data.accessToken)
    }
    catch (error) {
      console.warn('密钥交换失败，将使用降级方案:', error)
    }
  }

  // 如果返回了用户信息，设置到用户存储中
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

    // 同时存储到localStorage用于持久化
    lStorage.set('userInfo', loginUser)
  }

  $message.loading('登录中...', { key: 'login' })
  try {
    // 先获取菜单数据，再跳转
    await loadAndSetMenuData()

    $message.success('登录成功', { key: 'login' })
    // 使用环境变量中的默认跳转路径
    const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'

    // 处理重定向
    const redirectPath = route.query.redirect
    if (redirectPath && redirectPath !== '/login') {
      // 如果 redirect 不是登录页，则跳转到 redirect
      delete route.query.redirect
      router.push({ path: redirectPath, query: route.query })
    }
    else {
      // 否则跳转到首页
      router.push(defaultRedirectPath)
    }
  }
  catch (error) {
    console.error(error)
    $message.destroy('login')
  }
}

// 监听三方登录子窗口的消息
async function handleSocialLoginMessage(event) {
  if (event.data?.type === 'SOCIAL_LOGIN_SUCCESS') {
    const { data } = event.data

    // 设置 token
    if (data?.accessToken) {
      authStore.setToken({
        accessToken: data.accessToken,
        tokenType: data.tokenType || 'Bearer',
        expiresIn: data.expiresIn,
      })
    }

    $message.success('登录成功')

    // 使用 window.location.href 强制刷新页面跳转
    const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'
    window.location.href = defaultRedirectPath
  }
  else if (event.data?.type === 'SOCIAL_LOGIN_FAILED') {
    $message.error('三方登录失败，请重试')
  }
}

// 页面加载时获取登录配置和验证码
onMounted(() => {
  loadLoginConfig()
  loadSocialPlatforms()
  // 监听三方登录消息
  window.addEventListener('message', handleSocialLoginMessage)
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (smsTimer.value) {
    clearInterval(smsTimer.value)
  }
  // 移除消息监听
  window.removeEventListener('message', handleSocialLoginMessage)
})

// 获取并设置菜单数据
async function loadAndSetMenuData() {
  try {
    const permissionStore = usePermissionStore()

    // 获取菜单数据
    const res = await mainApi.getMenu(1)
    if (res.code === 200 && res.data) {
      // 设置菜单数据到store
      permissionStore.setMenuData(res.data)
    }
    else {
      console.error('菜单数据格式不正确:', res)
    }

    // 等待菜单数据加载完成（最多等待5秒）
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
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap');

/* Container */
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  font-family: 'Inter', sans-serif;
}

/* Animated background */
.login-bg-animated {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 50%, #bae6fd 100%);
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float 25s ease-in-out infinite;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  top: -20%;
  left: -10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
  bottom: -15%;
  right: -10%;
  animation-delay: 8s;
}

.orb-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #06b6d4, #22d3ee);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 16s;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  z-index: 1;
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(40px, -60px) scale(1.05);
  }
  50% {
    transform: translate(-30px, 30px) scale(0.95);
  }
  75% {
    transform: translate(20px, 40px) scale(1.02);
  }
}

/* Login card */
.login-card {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr;
  max-width: 1100px;
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.02),
    0 4px 6px -1px rgba(0, 0, 0, 0.02),
    0 12px 24px -4px rgba(0, 0, 0, 0.04),
    0 24px 48px -8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  animation: slideUp 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (min-width: 768px) {
  .login-card {
    grid-template-columns: 42% 58%;
  }
}

/* Left side - Branding */
.login-brand {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);
  padding: 48px 40px;
  display: none;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

@media (min-width: 768px) {
  .login-brand {
    display: flex;
  }
}

.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.deco-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -80px;
  animation: pulse 8s ease-in-out infinite;
}

.deco-2 {
  width: 200px;
  height: 200px;
  bottom: -60px;
  left: -40px;
  animation: pulse 6s ease-in-out infinite 2s;
}

.deco-3 {
  width: 150px;
  height: 150px;
  top: 40%;
  right: 20%;
  background: rgba(255, 255, 255, 0.05);
  animation: pulse 7s ease-in-out infinite 4s;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.logo-wrapper {
  margin-bottom: 28px;
  animation: fadeIn 0.8s ease-out 0.2s both;
  display: flex;
  justify-content: center;
}

.logo-img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.25));
  transition: transform 0.3s ease;
}

.logo-img:hover {
  transform: scale(1.05);
}

.brand-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
  animation: fadeIn 0.8s ease-out 0.3s both;
  letter-spacing: -0.02em;
}

.brand-subtitle {
  font-size: 0.9375rem;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 40px;
  animation: fadeIn 0.8s ease-out 0.4s both;
  font-weight: 400;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: fadeIn 0.8s ease-out 0.5s both;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  text-align: left;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  cursor: default;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(4px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: white;
}

.feature-desc {
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.7);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Right side - Form */
.login-form-wrapper {
  padding: 48px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.5);
}

@media (max-width: 767px) {
  .login-form-wrapper {
    padding: 32px 24px;
  }
}

.login-form {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
  animation: fadeIn 0.6s ease-out 0.2s both;
  text-align: center;
}

.form-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 1.625rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.form-subtitle {
  font-size: 0.9375rem;
  color: #64748b;
  font-weight: 400;
}

.form-body {
  animation: fadeIn 0.6s ease-out 0.4s both;
}

/* Form groups */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
  letter-spacing: 0.01em;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.modern-input {
  width: 100%;
}

/* Override Naive UI input styles */
:deep(.modern-input.n-input) {
  border-radius: 10px;
}

:deep(.modern-input .n-input__border),
:deep(.modern-input .n-input__state-border) {
  border-radius: 10px;
}

:deep(.modern-input.n-input:not(.n-input--disabled):hover) {
  border-color: #3b82f6;
}

:deep(.modern-input.n-input:not(.n-input--disabled):focus-within) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-icon {
  color: #94a3b8;
  font-size: 16px;
}

/* Captcha */
.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-image {
  width: 120px;
  height: 48px;
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}

.captcha-image:hover {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.captcha-image:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-loading {
  color: #94a3b8;
  font-size: 1.5rem;
}

/* Slider verify trigger */
.slider-verify-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 44px;
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
  user-select: none;
}

.slider-verify-trigger:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #2563eb;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.slider-verify-trigger.verified {
  border-color: #22c55e;
  background: #f0fdf4;
  color: #16a34a;
  cursor: default;
}

.slider-verify-trigger.verified:hover {
  box-shadow: none;
}

.trigger-icon {
  display: flex;
  align-items: center;
  font-size: 1rem;
}

.trigger-arrow {
  margin-left: auto;
  font-size: 0.875rem;
  opacity: 0.5;
  transition: transform 0.2s;
}

.slider-verify-trigger:hover .trigger-arrow {
  transform: translateX(3px);
  opacity: 0.8;
}

/* Slider modal overlay */
.slider-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* Slider modal card */
.slider-modal {
  position: relative;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 2rem 1.75rem 1.5rem;
  width: 100%;
  max-width: 400px;
  box-shadow:
    0 24px 48px rgba(15, 23, 42, 0.15),
    0 8px 16px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.slider-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f1f5f9;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  padding: 0;
}

.slider-modal-close:hover {
  background: #e2e8f0;
  color: #475569;
}

.slider-modal-header {
  text-align: center;
  margin-bottom: 1.25rem;
}

.slider-modal-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.375rem;
  color: #3b82f6;
  margin: 0 auto 12px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.slider-modal-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px;
}

.slider-modal-desc {
  font-size: 0.8125rem;
  color: #94a3b8;
  margin: 0;
}

.slider-modal-body {
  display: flex;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
}

.slider-modal-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 0.75rem;
  color: #cbd5e1;
}

/* Modal transitions */
.modal-enter-active {
  animation: modalIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-leave-active {
  animation: modalOut 0.25s ease-in forwards;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes modalOut {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
}

/* SMS button */
.sms-button {
  min-width: 120px;
  white-space: nowrap;
  border-radius: 10px;
}

.mb-3 {
  margin-bottom: 12px;
}

/* Form options */
.form-options {
  margin-bottom: 24px;
}

.checkbox-label {
  font-size: 0.875rem;
  color: #64748b;
}

/* Login button */
.login-button {
  margin-top: 8px;
  border-radius: 10px;
  font-size: 0.9375rem;
  font-weight: 600;
  height: 46px;
  transition: all 0.25s ease;
  cursor: pointer;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.25);
}

.login-button:active {
  transform: translateY(0);
}

.login-button :deep(.n-button__content) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.button-text {
  font-family: 'Plus Jakarta Sans', sans-serif;
  letter-spacing: 0.01em;
}

.button-icon {
  font-size: 1.125rem;
  transition: transform 0.25s ease;
}

.login-button:hover .button-icon {
  transform: translateX(4px);
}

/* Social login section */
.social-login-section {
  margin-top: 28px;
}

.social-divider {
  position: relative;
  text-align: center;
  margin-bottom: 20px;
}

.social-divider::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
}

.divider-text {
  position: relative;
  display: inline-block;
  padding: 0 16px;
  background: #fff;
  font-size: 0.8125rem;
  color: #94a3b8;
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.social-button {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  padding: 0;
}

.social-button:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.15);
}

.social-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.social-icon-fallback {
  font-size: 20px;
  color: #64748b;
}

.social-button:hover .social-icon-fallback {
  color: #3b82f6;
}

/* Dark mode */
.dark .login-bg-animated {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
}

.dark .login-card {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .login-form-wrapper {
  background: transparent;
}

.dark .form-title {
  color: #f1f5f9;
}

.dark .form-subtitle {
  color: #94a3b8;
}

.dark .form-label {
  color: #cbd5e1;
}

.dark .social-divider::before {
  background: linear-gradient(90deg, transparent, #334155 20%, #334155 80%, transparent);
}

.dark .divider-text {
  background: transparent;
}

.dark .slider-modal {
  background: rgba(30, 41, 59, 0.98);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .slider-modal-title {
  color: #f1f5f9;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .gradient-orb,
  .deco-circle {
    animation: none;
  }
}

/* Responsive */
@media (max-width: 767px) {
  .form-title {
    font-size: 1.375rem;
  }

  .login-card {
    border-radius: 20px;
  }
}
</style>
