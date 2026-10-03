/** index.vue setup part 1. */
import { useStorage } from '@vueuse/core'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import SlideVerify from 'vue3-slide-verify'
import mainApi from '@/api'
import defaultLogoUrl from '@/assets/images/logo.png'
import { useAppStore, useAuthStore, usePermissionStore, useTenantStore, useUserStore } from '@/store'
import { lStorage } from '@/utils'
import { loadRuntimeCryptoConfig } from '@/utils/crypto/crypto-config'
import { encryptPassword, initKeyExchange } from '@/utils/crypto/key-exchange'
import { request } from '@/utils/http'
import { normalizePageTitle } from '@/utils/page-title'
import { applyTenantConfig, resolveTenantPublicAssetUrl } from '@/utils/tenant-config'
import api from '../api'
import 'vue3-slide-verify/dist/style.css'
export function applyLoginPagePart1() {
  const __impl = {}
  const mut = {
    tenantInitCompleted: false,
  }
  const authStore = useAuthStore()
  const userStore = useUserStore()
  const appStore = useAppStore()
  const router = useRouter()
  const route = useRoute()
  const userClient = import.meta.env.VITE_USER_CLIENT || 'pc'
  const LOGIN_TENANT_STORAGE_KEY = 'login_selected_tenant_id'
  const SOCIAL_TENANT_MAP_KEY = 'login_social_tenant_map'
  const LOGIN_TENANT_SELECTION_REQUIRED = 4091

  const tenantOptions = ref([])
  const selectedTenantId = ref(null)
  const showWorkspaceModal = ref(false)
  const lastUsedTenantId = ref(null)
  const skipTenantContextRefresh = ref(false)
  const tenantConfigApplying = ref(false)
  const brandLogoUrl = ref(defaultLogoUrl)
  const loginConfig = ref(null)
  const selectedTenantOption = computed(() => tenantOptions.value.find(item => String(item.value) === String(selectedTenantId.value)) || null)
  const tenantSelectOptions = computed(() => tenantOptions.value.map(item => ({
    label: item.label,
    value: item.value,
  })))
  const showTenantSelect = computed(() => tenantSelectOptions.value.length > 1)
  const brandSystemName = computed(() => normalizePageTitle(loginConfig.value?.systemName) || 'ForgeAdmin')
  const loginSubtitle = computed(() => showTenantSelect.value && selectedTenantOption.value?.tenantName
    ? `${selectedTenantOption.value.tenantName} 工作区`
    : '请输入账号密码')
  const copyrightInfo = computed(() => normalizePageTitle(loginConfig.value?.copyrightInfo))

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
  const captchaEnabled = computed(() => loginConfig.value?.enableCaptcha !== false)

  // 群二维码引流验证码：独立于验证码总开关生效，未配置图片时回退占位符
  const groupQrcodeEnabled = computed(() => loginConfig.value?.groupQrcodeEnabled === true)
  const activeCaptchaTab = ref('default') // 'default' 或 'group'
  const groupQrcodeImage = computed(() => {
    const value = String(loginConfig.value?.groupQrcodeImage || '').trim()
    if (!value)
      return ''
    const lowerValue = value.toLowerCase()
    if (lowerValue.startsWith('http://') || lowerValue.startsWith('https://')
      || lowerValue.startsWith('data:') || lowerValue.startsWith('blob:')) {
      return value
    }
    // fileId 走登录页专用匿名图片接口，带 cache buster 避免换图后读到旧缓存
    const prefix = import.meta.env.VITE_REQUEST_PREFIX || ''
    return `${prefix}/auth/loginQrcode?v=${encodeURIComponent(value)}`
  })
  const groupQrcodeName = computed(() => loginConfig.value?.groupQrcodeName || '用户交流群')
  const groupQrcodeHint = computed(() => loginConfig.value?.groupQrcodeHint || '扫码加入用户群，获取验证码并完成登录')
  // 群二维码浮层交互：点“获取验证码”展开浮动二维码，点二维码可放大
  const qrcodePopoverVisible = ref(false)
  const qrcodePreviewVisible = ref(false)

  function toggleQrcodePopover() {
    qrcodePopoverVisible.value = !qrcodePopoverVisible.value
  }
  const resetPasswordChannels = computed(() => loginConfig.value?.resetPasswordChannels || [])
  const canResetPassword = computed(() => resetPasswordChannels.value.includes('sms') || resetPasswordChannels.value.includes('email'))
  const showResetForm = ref(false)
  const resetSending = ref(false)
  const resetSubmitting = ref(false)
  const resetCountdown = ref(0)
  const resetTimer = ref(null)
  const resetForm = ref({
    channel: 'sms',
    account: '',
    code: '',
    newPassword: '',
    confirmPassword: '',
  })
  const resetSubtitle = computed(() => (resetForm.value.channel === 'email' ? '通过邮箱验证码重置密码' : '通过手机验证码重置密码'))
  const resetAccountLabel = computed(() => (resetForm.value.channel === 'email' ? '邮箱' : '手机号'))
  const resetAccountPlaceholder = computed(() => (resetForm.value.channel === 'email' ? '请输入绑定邮箱' : '请输入绑定手机号'))
  const resetAccountValid = computed(() => {
    const account = (resetForm.value.account || '').trim()
    if (resetForm.value.channel === 'email')
      return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(account)
    return /^1[3-9]\d{9}$/.test(account)
  })

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
  const giteeCommunity = ref({
    enabled: false,
    requireStar: false,
    repoUrl: 'https://gitee.com/ForgeLab/forge-admin',
    owner: 'ForgeLab',
    repo: 'forge-admin',
  })
  const nonGiteePlatforms = computed(() =>
    (socialPlatforms.value || []).filter(item => item.enabled && item.platform !== 'GITEE'),
  )
  const displaySocialPlatforms = computed(() => {
    const list = (socialPlatforms.value || []).filter(item => item.enabled)
    if (!giteeCommunity.value.enabled)
      return list
    const withoutGitee = list.filter(item => item.platform !== 'GITEE')
    return [
      { platform: 'GITEE', platformName: 'Gitee', enabled: true },
      ...withoutGitee,
    ]
  })

  watch(resetPasswordChannels, (channels) => {
    if (!channels.includes(resetForm.value.channel))
      resetForm.value.channel = channels.includes('sms') ? 'sms' : 'email'
  }, { immediate: true })

  watch(selectedTenantId, (tenantId) => {
    if (!mut.tenantInitCompleted || tenantConfigApplying.value || skipTenantContextRefresh.value)
      return
    syncSelectedTenantToStorage(tenantId)
    tenantConfigApplying.value = true
    refreshLoginContext()
      .finally(() => {
        tenantConfigApplying.value = false
      })
  })

  // 验证码 Tab 切换时清空输入，避免混用
  watch(activeCaptchaTab, () => {
    loginInfo.value.code = ''
    // 切换验证码 tab 时收起群二维码浮层与放大预览
    qrcodePopoverVisible.value = false
    qrcodePreviewVisible.value = false
  })

  // 手机号验证
  const isValidPhone = computed(() => {
    const phone = loginInfo.value.phone
    return phone && /^1[3-9]\d{9}$/.test(phone)
  })

  function normalizeTenantId(value) {
    if (Array.isArray(value)) {
      const first = value.find(item => item !== null && item !== undefined && item !== '')
      return normalizeTenantId(first)
    }
    if (value === null || value === undefined || value === '')
      return null
    const parsed = Number(value)
    return Number.isNaN(parsed) ? null : parsed
  }

  function syncSelectedTenantToStorage(tenantId) {
    const normalizedTenantId = normalizeTenantId(tenantId)
    if (normalizedTenantId === null) {
      lStorage.remove(LOGIN_TENANT_STORAGE_KEY)
      return
    }
    lStorage.set(LOGIN_TENANT_STORAGE_KEY, normalizedTenantId)
  }

  function getSelectedTenantFromStorage() {
    return normalizeTenantId(lStorage.get(LOGIN_TENANT_STORAGE_KEY))
  }

  function getSocialTenantMap() {
    return lStorage.get(SOCIAL_TENANT_MAP_KEY) || {}
  }

  function setSocialTenantMap(map) {
    lStorage.set(SOCIAL_TENANT_MAP_KEY, map)
  }

  function rememberSocialTenant(state, tenantId) {
    const normalizedTenantId = normalizeTenantId(tenantId)
    if (!state || normalizedTenantId === null)
      return
    const map = getSocialTenantMap()
    map[state] = normalizedTenantId
    setSocialTenantMap(map)
  }

  function applyBrandLogo(config) {
    brandLogoUrl.value = resolveTenantPublicAssetUrl(config, 'logo') || defaultLogoUrl
  }

  function handleBrandLogoError() {
    if (brandLogoUrl.value !== defaultLogoUrl)
      brandLogoUrl.value = defaultLogoUrl
  }

  async function applyLoginPageConfig(config) {
    loginConfig.value = config || null
    await applyTenantConfig(config, appStore)
    applyBrandLogo(config)
  }

  function applyWorkspaceChallenge(payload) {
    const code = payload?.code
    const rawOptions = payload?.error?.data ?? payload?.data
    if (code !== LOGIN_TENANT_SELECTION_REQUIRED || !Array.isArray(rawOptions) || rawOptions.length <= 1)
      return false
    tenantOptions.value = rawOptions.map(item => ({
      ...item,
      value: normalizeTenantId(item.tenantId),
      label: item.tenantName || item.systemName || item.browserTitle || String(item.tenantId),
    })).filter(item => item.value !== null)
    lastUsedTenantId.value = getSelectedTenantFromStorage()
    selectedTenantId.value = null
    showWorkspaceModal.value = true
    return true
  }

  function closeWorkspaceModal() {
    showWorkspaceModal.value = false
  }

  async function confirmWorkspace(option) {
    const tenantId = normalizeTenantId(option?.value)
    if (tenantId === null || loading.value)
      return
    skipTenantContextRefresh.value = true
    selectedTenantId.value = tenantId
    syncSelectedTenantToStorage(tenantId)
    showWorkspaceModal.value = false
    try {
      await handleLogin()
    }
    finally {
      skipTenantContextRefresh.value = false
    }
  }

  // 获取登录配置
  async function loadLoginConfig() {
    const currentTenantId = normalizeTenantId(selectedTenantId.value)
    try {
      const res = await api.getLoginConfig(userClient, currentTenantId)
      if (res.code === 200 && res.data) {
        await applyLoginPageConfig(res.data)
        captchaType.value = res.data.captchaType || 'graphical'

        // 验证码总开关关闭但群二维码启用时，直接进入群二维码验证，不再展示默认验证码
        if (!captchaEnabled.value && groupQrcodeEnabled.value) {
          activeCaptchaTab.value = 'group'
        }

        // 根据验证码类型加载对应的验证码
        if (captchaEnabled.value) {
          await loadCaptchaByType()
        }
        else {
          loginInfo.value.code = ''
          loginInfo.value.codeKey = ''
        }
      }
    }
    catch (error) {
      console.error('获取登录配置失败:', error)
      // 使用默认配置
      captchaType.value = 'graphical'
      await applyLoginPageConfig(null)
      await refreshCaptcha()
    }
  }

  // 获取已启用的三方登录平台
  async function loadSocialPlatforms() {
    try {
      socialLoading.value = true
      const [platformRes, communityRes] = await Promise.all([
        api.getSocialPlatforms(normalizeTenantId(selectedTenantId.value)),
        api.getGiteeCommunityLogin().catch(() => null),
      ])
      if (platformRes.code === 200 && platformRes.data)
        socialPlatforms.value = platformRes.data.filter(p => p.enabled)
      if (communityRes?.code === 200 && communityRes.data) {
        giteeCommunity.value = { ...giteeCommunity.value, ...communityRes.data }
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
      const communityGitee = giteeCommunity.value.enabled && platform === 'GITEE'
      const tenantId = communityGitee ? undefined : normalizeTenantId(selectedTenantId.value)
      const res = await api.getSocialAuthUrl(platform, tenantId)
      if (res.code === 200 && res.data) {
        rememberSocialTenant(res.data.state, tenantId)
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
    // 滑块验证仅在默认验证码 tab 且类型为 slider 时触发
    if (captchaEnabled.value && activeCaptchaTab.value === 'default' && captchaType.value === 'slider' && !sliderSuccess.value) {
      openSliderModal()
      return
    }
    handleLogin()
  }

  function openResetForm() {
    showResetForm.value = true
  }

  function closeResetForm() {
    showResetForm.value = false
    resetForm.value.code = ''
    resetForm.value.newPassword = ''
    resetForm.value.confirmPassword = ''
  }

  function startResetCountdown() {
    resetCountdown.value = 60
    if (resetTimer.value)
      clearInterval(resetTimer.value)
    resetTimer.value = setInterval(() => {
      resetCountdown.value -= 1
      if (resetCountdown.value <= 0) {
        clearInterval(resetTimer.value)
        resetTimer.value = null
      }
    }, 1000)
  }

  async function sendResetCode() {
    if (!resetAccountValid.value)
      return $message.warning(`请输入正确的${resetAccountLabel.value}`)
    try {
      resetSending.value = true
      await api.sendResetPasswordCode({
        channel: resetForm.value.channel,
        account: resetForm.value.account.trim(),
        ...(selectedTenantId.value != null ? { tenantId: selectedTenantId.value } : {}),
      })
      startResetCountdown()
    }
    catch (error) {
      console.error(error)
    }
    finally {
      resetSending.value = false
    }
  }

  async function submitResetPassword() {
    if (!resetAccountValid.value)
      return $message.warning(`请输入正确的${resetAccountLabel.value}`)
    if (!resetForm.value.code)
      return $message.warning('请输入验证码')
    if (!resetForm.value.newPassword)
      return $message.warning('请输入新密码')
    if (resetForm.value.newPassword !== resetForm.value.confirmPassword)
      return $message.warning('两次输入的密码不一致')
    try {
      resetSubmitting.value = true
      await loadRuntimeCryptoConfig()
      const passwordEncryptionEnabled = loginConfig.value?.enablePasswordEncryption !== false
      const submittedPassword = await encryptPassword(resetForm.value.newPassword, request, passwordEncryptionEnabled)
      const res = await api.resetPassword({
        channel: resetForm.value.channel,
        account: resetForm.value.account.trim(),
        code: resetForm.value.code.trim(),
        newPassword: submittedPassword,
        ...(selectedTenantId.value != null ? { tenantId: selectedTenantId.value } : {}),
      })
      if (res.code === 200) {
        $message.success('密码已重置，请使用新密码登录')
        closeResetForm()
      }
    }
    catch (error) {
      console.error(error)
    }
    finally {
      resetSubmitting.value = false
    }
  }

  // 滑块验证成功回调
  function onSlideSuccess(result) {
    sliderSuccess.value = true
    sliderFail.value = false
    const moveX = Number(result?.left)
    loginInfo.value.code = Number.isFinite(moveX) ? String(Math.round(moveX)) : ''
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
    if (!captchaEnabled.value)
      return

    // 群二维码 tab：清空验证码即可，无需刷新图片
    if (activeCaptchaTab.value === 'group') {
      loginInfo.value.code = ''
      return
    }

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
    const tenantId = normalizeTenantId(selectedTenantId.value)

    // 基础验证
    if (!username || !password)
      return $message.warning('请输入用户名和密码')

    if (tenantSelectOptions.value.length > 1 && tenantId === null) {
      showWorkspaceModal.value = true
      return
    }

    // 群二维码验证码独立于验证码总开关生效
    if (activeCaptchaTab.value === 'group' && groupQrcodeEnabled.value) {
      if (!code)
        return $message.warning('请输入群验证码')
    }
    // 默认验证码 tab
    else if (captchaEnabled.value) {
      if (captchaType.value === 'slider') {
        if (!sliderSuccess.value) {
          return $message.warning('请完成滑块验证')
        }
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
    }

    try {
      loading.value = true
      $message.loading('正在验证，请稍后...', { key: 'login' })

      await loadRuntimeCryptoConfig()

      // 登录密码 RSA 与通用 API 传输加密相互独立，以服务端登录配置为准。
      const passwordEncryptionEnabled = loginConfig.value?.enablePasswordEncryption !== false
      const submittedPassword = await encryptPassword(password, request, passwordEncryptionEnabled)

      // 构造登录参数 - 使用新的后端接口格式
      const usingGroupCaptcha = activeCaptchaTab.value === 'group' && groupQrcodeEnabled.value
      const params = {
        username,
        password: submittedPassword,
        code,
        codeKey: usingGroupCaptcha ? 'group_captcha' : codeKey,
        phone, // 短信验证码时需要
        // 群二维码验证码独立生效时也必须走验证码认证策略
        authType: (captchaEnabled.value || usingGroupCaptcha) ? 'password_captcha' : 'password',
        captchaType: usingGroupCaptcha ? 'group' : undefined, // 群二维码时告知后端
        userClient,
        appId: import.meta.env.VITE_APP_ID || 'forge_pc_001', // 客户端AppId
        ...(tenantId != null ? { tenantId } : {}),
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
      else if (applyWorkspaceChallenge(res)) {
        $message.destroy('login')
      }
      else {
        // 登录接口 needTip: false 屏蔽了全局错误弹窗，失败原因必须在登录页自行提示
        $message.error(res.message || '登录失败，请重试', { key: 'login' })
        await handleLoginFailure()
      }
    }
    catch (error) {
      $message.destroy('login')
      if (!applyWorkspaceChallenge(error)) {
        console.error(error)
        $message.error(error?.message || '登录失败，请重试', { key: 'login' })
        await handleLoginFailure()
      }
    }
    loading.value = false
  }

  async function onLoginSuccess(data = {}) {
    authStore.resetLoginState({ resetAuth: false })
    const accessToken = data.accessToken || data.token

    // 设置认证信息 - LoginResult 结构
    if (accessToken) {
      authStore.setToken({
        accessToken,
        tokenType: data.tokenType || 'Bearer',
        expiresIn: data.expiresIn,
      })

      // 密钥交换 - 使用 token 作为会话标识
      try {
        await initKeyExchange(request, accessToken)
      }
      catch (error) {
        console.warn('密钥交换失败，将使用降级方案:', error)
      }
    }

    // 如果返回了用户信息，设置到用户存储中
    if (data.userInfo) {
      const loginUser = data.userInfo
      loginUser.forcePasswordChange = data.forcePasswordChange === true || loginUser.forcePasswordChange === true
      userStore.setUser({
        id: loginUser.userId,
        username: loginUser.username,
        nickName: loginUser.realName || loginUser.username,
        email: loginUser.email,
        phone: loginUser.phone,
        avatar: loginUser.avatar,
        userType: loginUser.userType,
        userStatus: loginUser.userStatus,
        forcePasswordChange: loginUser.forcePasswordChange,
        tenantId: loginUser.tenantId,
        tenantName: loginUser.tenantName,
        tenantIds: loginUser.tenantIds || [],
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

    const mustChangePassword = data.forcePasswordChange === true || data.userInfo?.forcePasswordChange === true
    if (mustChangePassword) {
      $message.warning('当前账号必须先修改初始密码', { key: 'login' })
      router.push('/profile')
      return
    }

    $message.loading('登录中...', { key: 'login' })
    try {
      // 先获取菜单数据，再跳转
      await loadAndSetMenuData(data.userInfo?.tenantId || selectedTenantId.value)

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
      const accessToken = data?.accessToken || data?.token

      authStore.resetLoginState({ resetAuth: false })

      // 设置 token
      if (accessToken) {
        authStore.setToken({
          accessToken,
          tokenType: data.tokenType || 'Bearer',
          expiresIn: data.expiresIn,
        })
      }

      $message.success('登录成功')

      // 使用 window.location.href 强制刷新页面跳转
      // resolve() 会自动拼上路由 base（生产环境为 /forge），直接赋 '/' 会跳出 SPA 落到站点根路径
      const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'
      window.location.href = router.resolve(defaultRedirectPath).href
    }
    else if (event.data?.type === 'SOCIAL_LOGIN_FAILED') {
      $message.error('三方登录失败，请重试')
    }
  }

  // 页面加载时获取登录配置和验证码
  watch(() => loginInfo.value.username, () => {
    if (tenantOptions.value.length === 0)
      return
    tenantOptions.value = []
    selectedTenantId.value = null
    showWorkspaceModal.value = false
  })

  onMounted(() => {
    ;(async () => {
      try {
        tenantConfigApplying.value = true
        await refreshLoginContext()
      }
      finally {
        mut.tenantInitCompleted = true
        tenantConfigApplying.value = false
      }
    })()
    // 监听三方登录消息
    window.addEventListener('message', handleSocialLoginMessage)
  })

  // 组件卸载时清理定时器
  onUnmounted(() => {
    if (smsTimer.value) {
      clearInterval(smsTimer.value)
    }
    if (resetTimer.value)
      clearInterval(resetTimer.value)
    // 移除消息监听
    window.removeEventListener('message', handleSocialLoginMessage)
  })

  // 获取并设置菜单数据
  async function refreshLoginContext() {
    await Promise.all([
      loadLoginConfig(),
      loadSocialPlatforms(),
    ])
  }

  async function loadAndSetMenuData(loginTenantId = selectedTenantId.value) {
    try {
      const permissionStore = usePermissionStore()
      const tenantStore = useTenantStore()
      const tenantConfig = await tenantStore.loadTenantConfig(normalizeTenantId(loginTenantId) || userStore.userInfo?.tenantId)
      await applyTenantConfig(tenantConfig, appStore)

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
  __impl.toggleQrcodePopover = toggleQrcodePopover
  __impl.normalizeTenantId = normalizeTenantId
  __impl.syncSelectedTenantToStorage = syncSelectedTenantToStorage
  __impl.getSelectedTenantFromStorage = getSelectedTenantFromStorage
  __impl.getSocialTenantMap = getSocialTenantMap
  __impl.setSocialTenantMap = setSocialTenantMap
  __impl.rememberSocialTenant = rememberSocialTenant
  __impl.applyBrandLogo = applyBrandLogo
  __impl.handleBrandLogoError = handleBrandLogoError
  __impl.applyLoginPageConfig = applyLoginPageConfig
  __impl.applyWorkspaceChallenge = applyWorkspaceChallenge
  __impl.closeWorkspaceModal = closeWorkspaceModal
  __impl.confirmWorkspace = confirmWorkspace
  __impl.loadLoginConfig = loadLoginConfig
  __impl.loadSocialPlatforms = loadSocialPlatforms
  __impl.handleSocialLogin = handleSocialLogin
  __impl.loadCaptchaByType = loadCaptchaByType
  __impl.refreshCaptcha = refreshCaptcha
  __impl.loadSlideImages = loadSlideImages
  __impl.openSliderModal = openSliderModal
  __impl.closeSliderModal = closeSliderModal
  __impl.onLoginClick = onLoginClick
  __impl.openResetForm = openResetForm
  __impl.closeResetForm = closeResetForm
  __impl.startResetCountdown = startResetCountdown
  __impl.sendResetCode = sendResetCode
  __impl.submitResetPassword = submitResetPassword
  __impl.onSlideSuccess = onSlideSuccess
  __impl.onSlideFail = onSlideFail
  __impl.onSlideRefresh = onSlideRefresh
  __impl.handleLoginFailure = handleLoginFailure
  __impl.sendSmsCode = sendSmsCode
  __impl.startSmsCountdown = startSmsCountdown
  __impl.handleLogin = handleLogin
  __impl.onLoginSuccess = onLoginSuccess
  __impl.handleSocialLoginMessage = handleSocialLoginMessage
  __impl.refreshLoginContext = refreshLoginContext
  __impl.loadAndSetMenuData = loadAndSetMenuData

  return {
    __impl, mut, applyBrandLogo, applyLoginPageConfig, applyWorkspaceChallenge, closeResetForm, closeSliderModal, closeWorkspaceModal,
    confirmWorkspace, getSelectedTenantFromStorage, getSocialTenantMap, handleBrandLogoError, handleLogin, handleLoginFailure, handleSocialLogin, handleSocialLoginMessage,
    loadAndSetMenuData, loadCaptchaByType, loadLoginConfig, loadSlideImages, loadSocialPlatforms, normalizeTenantId, onLoginClick, onLoginSuccess,
    onSlideFail, onSlideRefresh, onSlideSuccess, openResetForm, openSliderModal, refreshCaptcha, refreshLoginContext, rememberSocialTenant,
    sendResetCode, sendSmsCode, setSocialTenantMap, startResetCountdown, startSmsCountdown, submitResetPassword, syncSelectedTenantToStorage, toggleQrcodePopover,
    authStore, userStore, appStore, router, route, userClient, LOGIN_TENANT_STORAGE_KEY, SOCIAL_TENANT_MAP_KEY,
    LOGIN_TENANT_SELECTION_REQUIRED, tenantOptions, selectedTenantId, showWorkspaceModal, lastUsedTenantId, skipTenantContextRefresh, tenantConfigApplying, brandLogoUrl,
    loginConfig, selectedTenantOption, tenantSelectOptions, showTenantSelect, brandSystemName, loginSubtitle, copyrightInfo, loginInfo,
    captchaImage, captchaExpires, captchaType, captchaEnabled, groupQrcodeEnabled, activeCaptchaTab, groupQrcodeImage, groupQrcodeName,
    groupQrcodeHint, qrcodePopoverVisible, qrcodePreviewVisible, resetPasswordChannels, canResetPassword, showResetForm, resetSending, resetSubmitting,
    resetCountdown, resetTimer, resetForm, resetSubtitle, resetAccountLabel, resetAccountPlaceholder, resetAccountValid, slideVerifyRef,
    slideImages, sliderSuccess, sliderFail, showSliderModal, smsCountdown, smsTimer, localLoginInfo, isRemember,
    loading, socialPlatforms, socialLoading, giteeCommunity, nonGiteePlatforms, displaySocialPlatforms, isValidPhone,
  }
}
