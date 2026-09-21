<template>
  <view class="login-page">
    <!-- 企微免登 pending 时全屏 loading，不展示登录表单 -->
    <view v-if="wecomPending" class="page-shell wecom-loading-shell">
      <view class="wecom-loading-content">
        <image class="wecom-loading-logo" :src="assetUrl('/static/logo.png')" mode="aspectFit" />
        <text class="wecom-loading-text">正在登录...</text>
        <view class="wecom-loading-dots">
          <view class="dot dot-1" />
          <view class="dot dot-2" />
          <view class="dot dot-3" />
        </view>
      </view>
    </view>

    <view v-else class="page-shell">
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

      <view class="login-panel">
        <view class="panel-head">
          <text class="panel-title">{{ title }}</text>
          <text class="panel-subtitle">请使用企业账号完成身份验证</text>
        </view>

        <view class="form-stack">
          <view class="field">
            <image class="field-icon" :src="assetUrl('/static/icons/ai-icon/user.svg')" mode="aspectFit" />
            <input
              v-model="form.username"
              class="field-input"
              placeholder="请输入用户名"
              placeholder-class="field-placeholder"
              confirm-type="next"
            />
          </view>

          <view class="field">
            <image class="field-icon" :src="assetUrl('/static/icons/ai-icon/lock.svg')" mode="aspectFit" />
            <input
              v-model="form.password"
              class="field-input"
              :password="!showPassword"
              placeholder="请输入密码"
              placeholder-class="field-placeholder"
              confirm-type="done"
              @confirm="handleLogin"
            />
            <view class="password-toggle" @click.stop="togglePassword">
              <image
                class="toggle-icon"
                :src="assetUrl(showPassword ? '/static/icons/ai-icon/eye-off.svg' : '/static/icons/ai-icon/eye.svg')"
                mode="aspectFit"
              />
            </view>
          </view>

          <view class="captcha-row">
            <view class="field captcha-input-wrap">
              <image class="field-icon" :src="assetUrl('/static/icons/ai-icon/shield.svg')" mode="aspectFit" />
              <input
                v-model="form.code"
                class="field-input"
                placeholder="请输入验证码"
                placeholder-class="field-placeholder"
                confirm-type="done"
                @confirm="handleLogin"
              />
            </view>
            <view class="captcha-image" :class="{ 'captcha-refreshing': captcha.loading }" @click="loadCaptcha">
              <image v-if="captcha.image" class="captcha-img" :src="captcha.image" mode="aspectFit" />
              <text v-else class="captcha-empty">{{ captcha.loading ? '加载中' : '刷新' }}</text>
              <view class="captcha-lines" />
              <view class="captcha-hover">
                <image class="refresh-icon" :src="assetUrl('/static/icons/ai-icon/refresh-cw.svg')" mode="aspectFit" />
              </view>
            </view>
          </view>
        </view>

        <button class="login-button" :disabled="loading" @click="handleLogin">
          <text>{{ loading ? '登录中...' : '登录' }}</text>
          <text class="button-arrow">→</text>
        </button>

      </view>

      <view class="login-foot">
        <text>© 2026 FORGE 移动端</text>
      </view>
    </view>

    <view v-if="showWorkspaceModal" class="workspace-modal-overlay" @click="closeWorkspaceModal">
      <view class="workspace-modal" @click.stop>
        <text class="workspace-modal-title">选择工作区</text>
        <text class="workspace-modal-desc">该账号可进入多个工作区，请选择后继续登录</text>
        <view class="workspace-modal-list">
          <view
            v-for="item in tenantOptions"
            :key="String(item.tenantId)"
            class="workspace-option"
            :class="{ 'is-current': String(item.tenantId) === String(lastUsedTenantId) }"
            @click="confirmWorkspace(item)"
          >
            <view class="workspace-option-copy">
              <text class="workspace-option-name">{{ item.tenantName || item.systemName || item.tenantId }}</text>
            </view>
            <text v-if="String(item.tenantId) === String(lastUsedTenantId)" class="workspace-option-tag">上次使用</text>
          </view>
        </view>
        <view class="workspace-modal-cancel" @click="closeWorkspaceModal">取消</view>
      </view>
    </view>
  </view>
</template>

<script>
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
  data() {
    return {
      title: import.meta.env.VITE_TITLE || 'Forge 移动端',
      userClient: import.meta.env.VITE_USER_CLIENT || 'app',
      requestPrefix: import.meta.env.VITE_REQUEST_PREFIX || '/',
      redirect: '/pages/index/index',
      loading: false,
      wecomPending: false,
      showPassword: false,
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
    togglePassword() {
      this.showPassword = !this.showPassword
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
