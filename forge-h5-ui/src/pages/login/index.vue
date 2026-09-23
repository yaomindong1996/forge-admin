<template>
  <view class="login-page">
    <AiFeedbackHost />
    <view v-if="wecomPending" class="wecom-loading-shell">
      <view class="wecom-loading-content">
        <image class="wecom-loading-logo" :src="assetUrl('/static/logo.png')" mode="aspectFit" />
        <wd-loading type="ring" color="#4266F7" :size="28" />
        <text class="wecom-loading-text">正在验证企业身份</text>
      </view>
    </view>

    <view v-else class="page-shell">
      <view class="login-masthead">
        <view class="brand-bar">
          <view class="brand-main">
            <view class="brand-mark">
              <image class="brand-logo" :src="assetUrl('/static/logo.png')" mode="aspectFit" />
            </view>
            <view class="brand-copy">
              <text class="brand-title">Forge 移动工作台</text>
              <text class="brand-subtitle">企业应用统一入口</text>
            </view>
          </view>
          <text class="client-badge">{{ userClient.toUpperCase() }}</text>
        </view>
        <view class="masthead-copy">
          <text class="masthead-kicker">统一身份认证</text>
          <text class="masthead-title">登录移动工作台</text>
          <text class="masthead-desc">访问企业应用、审批任务与消息通知</text>
        </view>
      </view>

      <view class="login-main">
        <view class="login-panel">
          <view class="panel-head">
            <text class="panel-title">企业账号登录</text>
            <text class="panel-subtitle">请输入账号信息完成身份验证</text>
          </view>

          <view class="form-stack">
            <AiField v-model="form.username" clearable placeholder="请输入用户名">
              <template #leftIcon><AiIcon name="user" color="#86909C" size="sm" /></template>
            </AiField>

            <AiField v-model="form.password" type="password" placeholder="请输入密码" @confirm="handleLogin">
              <template #leftIcon><AiIcon name="lock" color="#86909C" size="sm" /></template>
            </AiField>

            <view class="captcha-row">
              <AiField v-model="form.code" class="captcha-field" placeholder="请输入验证码" @confirm="handleLogin">
                <template #leftIcon><AiIcon name="shield" color="#86909C" size="sm" /></template>
              </AiField>
              <button class="captcha-image" :disabled="captcha.loading" @click="loadCaptcha">
                <image v-if="captcha.image" class="captcha-img" :src="captcha.image" mode="aspectFit" />
                <view v-else class="captcha-empty">
                  <wd-loading v-if="captcha.loading" type="ring" color="#4266F7" :size="18" />
                  <text v-else>获取验证码</text>
                </view>
              </button>
            </view>
          </view>

          <AiButton block size="lg" :loading="loading" @click="handleLogin">登录</AiButton>

          <view class="login-security">
            <AiIcon name="shield" color="#86909C" size="xs" />
            <text>账号信息通过安全链路传输</text>
          </view>
        </view>

        <view class="login-foot">
          <text>{{ title }} · © 2026 FORGE</text>
        </view>
      </view>
    </view>

    <AiPopupSheet
      v-model="showWorkspaceModal"
      title="选择工作区"
      description="该账号可进入多个工作区，请选择后继续登录"
      max-height="72vh"
      body-max-height="calc(72vh - 190rpx - env(safe-area-inset-bottom))"
      @close="closeWorkspaceModal"
    >
      <view class="workspace-modal-list">
        <button
          v-for="item in tenantOptions"
          :key="String(item.tenantId)"
          class="workspace-option"
          :class="{ 'is-current': String(item.tenantId) === String(lastUsedTenantId) }"
          @click="confirmWorkspace(item)"
        >
          <text class="workspace-option-name">{{ item.tenantName || item.systemName || item.tenantId }}</text>
          <text v-if="String(item.tenantId) === String(lastUsedTenantId)" class="workspace-option-tag">上次使用</text>
          <AiIcon v-else name="chevron-right" color="#86909C" size="sm" />
        </button>
      </view>
    </AiPopupSheet>
  </view>
</template>

<script>
import AiButton from '@/components/AiButton.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiField from '@/components/AiField.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import { useAuthStore } from '@/store'
import api from '@/api'
import { resolveStaticUrl } from '@/utils/assets'
import { notify, toast } from '@/utils/notify'
import { getWeComAutoLoginPromise, isWeComAutoLoginPending } from '@/utils/wecom'

const LOGIN_TENANT_SELECTION_REQUIRED = 4091

function normalizeLoginError(error) {
  const rawMessage = error?.message || error?.error?.message || error?.error?.msg || '登录失败，请稍后重试'
  return String(rawMessage)
    .replace(/^com\.[\w.$]+Exception:\s*/i, '')
    .replace(/^.*BusinessException:\s*/i, '')
    .trim() || '登录失败，请稍后重试'
}

function extractWorkspaceOptions(error) {
  if (Number(error?.code) !== LOGIN_TENANT_SELECTION_REQUIRED)
    return []
  const raw = error?.error?.data ?? error?.data
  return Array.isArray(raw) ? raw : []
}

export default {
  components: { AiButton, AiFeedbackHost, AiField, AiIcon, AiPopupSheet },
  data() {
    return {
      title: import.meta.env.VITE_TITLE || 'Forge 移动端',
      userClient: import.meta.env.VITE_USER_CLIENT || 'app',
      requestPrefix: import.meta.env.VITE_REQUEST_PREFIX || '/',
      redirect: '/pages/index/index',
      loading: false,
      wecomPending: false,
      captcha: {
        loading: false,
        image: '',
        codeKey: '',
      },
      tenantOptions: [],
      showWorkspaceModal: false,
      lastUsedTenantId: null,
      form: {
        username: '',
        password: '',
        code: '',
        tenantId: null,
      },
    }
  },
  computed: {
    showTenantSelect() {
      return this.tenantOptions.length > 1
    },
  },
  watch: {
    'form.username'() {
      if (!this.tenantOptions.length)
        return
      this.tenantOptions = []
      this.form.tenantId = null
      this.showWorkspaceModal = false
    },
  },
  onLoad(options = {}) {
    this.redirect = options.redirect ? decodeURIComponent(options.redirect) : '/pages/index/index'
    const authStore = useAuthStore()
    if (authStore.isLogin) {
      this.goTarget()
      return
    }
    // 企微客户端内正处于免登流程时，等待其结果，避免闪现账号密码登录表单
    if (isWeComAutoLoginPending()) {
      this.wecomPending = true
      this.loading = true
      const pending = getWeComAutoLoginPromise()
      if (pending) {
        pending.then((result) => {
          if (result?.status === 'logged-in' || authStore.isLogin) {
            this.goTarget()
            return
          }
          if (result?.status === 'redirecting') {
            return
          }
          // 免登跳过或失败：回退到常规账号密码登录
          this.wecomPending = false
          this.loading = false
          this.loadCaptcha()
        })
        return
      }
    }
    this.loadCaptcha()
  },
  methods: {
    assetUrl(path) {
      return resolveStaticUrl(path)
    },
    normalizeCaptchaImage(image) {
      if (!image) {
        return ''
      }
      return image.startsWith('data:image') ? image : `data:image/png;base64,${image}`
    },
    async loadCaptcha() {
      if (this.captcha.loading) {
        return
      }
      this.captcha.loading = true
      try {
        const res = await api.getCaptcha()
        const data = res.data || {}
        this.captcha.image = this.normalizeCaptchaImage(data.image)
        this.captcha.codeKey = data.codeKey || ''
        this.form.code = ''
      }
      catch (error) {
        console.error('获取验证码失败:', error)
        toast('验证码加载失败', { type: 'error' })
      }
      finally {
        this.captcha.loading = false
      }
    },
    goTarget() {
      const url = this.redirect || '/pages/index/index'
      const path = url.split('?')[0]
      if (path === '/pages/index/index' || path === '/pages/mine/index') {
        uni.switchTab({ url: path })
        return
      }
      uni.reLaunch({ url })
    },
    closeWorkspaceModal() {
      this.showWorkspaceModal = false
    },
    confirmWorkspace(option) {
      if (!option?.tenantId || this.loading)
        return
      this.form.tenantId = option.tenantId
      this.showWorkspaceModal = false
      this.handleLogin()
    },
    applyWorkspaceChallenge(error) {
      const options = extractWorkspaceOptions(error).filter(item => item?.tenantId != null)
      if (options.length <= 1)
        return false
      this.tenantOptions = options
      this.lastUsedTenantId = this.form.tenantId
      this.form.tenantId = null
      this.showWorkspaceModal = true
      return true
    },
    async handleLogin() {
      const username = this.form.username.trim()
      const password = this.form.password
      const code = this.form.code.trim()
      if (!username || !password || !code) {
        toast('请输入用户名、密码和验证码', { type: 'warning' })
        return
      }
      if (this.showTenantSelect && !this.form.tenantId) {
        this.showWorkspaceModal = true
        return
      }
      if (!this.captcha.codeKey) {
        toast('请先刷新验证码', { type: 'warning' })
        return
      }

      this.loading = true
      try {
        const authStore = useAuthStore()
        await authStore.login({
          username,
          password,
          code,
          codeKey: this.captcha.codeKey,
          tenantId: this.form.tenantId || undefined,
        })
        toast('登录成功', { type: 'success' })
        this.goTarget()
      }
      catch (error) {
        if (this.applyWorkspaceChallenge(error))
          return
        console.error('登录失败:', error)
        notify({
          title: '登录失败',
          description: normalizeLoginError(error),
          type: 'error',
          duration: 3600,
        })
        this.loadCaptcha()
      }
      finally {
        this.loading = false
      }
    },
  },
}
</script>

<style lang="scss" scoped src="../styles/login.scss"></style>
