<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-mesh"></div>
      <div class="bg-grid"></div>
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>
      <div class="bg-orb orb-3"></div>
      <div class="bg-particles">
        <span v-for="n in 30" :key="n" class="particle" :style="particleStyle(n)"></span>
      </div>
    </div>

    <div class="login-body">
      <div class="login-split">
        <div class="split-left">
          <div class="left-brand">
            <div class="brand-logo">
              <img class="brand-logo-img" src="@/assets/logo.png" alt="logo" />
            </div>
            <div class="brand-title">
              <span class="brand-title-main">Forge</span>
              <span class="brand-title-sub">AI 数据大屏</span>
            </div>
          </div>

          <div class="left-showcase">
            <div class="left-carousel">
              <n-carousel autoplay :interval="4000" dot-type="line" effect="fade">
                <div v-for="(item, i) in carouselImgList" :key="i" class="carousel-item">
                  <img :src="getImageUrl(item, 'login')" alt="screenshot" />
                </div>
              </n-carousel>
            </div>

            <div class="left-features">
              <div class="feature-row">
                <div class="feature-icon">
                  <n-icon size="20"><rocket-icon /></n-icon>
                </div>
                <div class="feature-text">
                  <strong>AI 智能生成</strong>
                  <span>一句话描述，自动生成大屏页面</span>
                </div>
              </div>
              <div class="feature-row">
                <div class="feature-icon">
                  <n-icon size="20"><bar-chart-icon /></n-icon>
                </div>
                <div class="feature-text">
                  <strong>50+ 图表组件</strong>
                  <span>丰富的可视化组件库，随取随用</span>
                </div>
              </div>
              <div class="feature-row">
                <div class="feature-icon">
                  <n-icon size="20"><sparkles-icon /></n-icon>
                </div>
                <div class="feature-text">
                  <strong>一键发布</strong>
                  <span>编辑完成即可发布上线</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="split-right">
          <div class="login-form">
            <div class="form-glow-ring"></div>
            <div class="form-accent"></div>

            <div class="form-header">
              <div class="form-header-badge">
                <n-icon size="14"><sparkles-icon /></n-icon>
                <span>AI 数据大屏</span>
              </div>
              <h2>欢迎登录</h2>
              <p>登录您的账户以继续使用</p>
            </div>

            <n-form
              ref="formRef"
              size="large"
              :model="formInline"
              :rules="rules"
            >
              <n-form-item path="username">
                <n-input
                  v-model:value="formInline.username"
                  type="text"
                  maxlength="16"
                  :placeholder="$t('global.form_account')"
                  class="login-input"
                  @keydown.enter="handleSubmit"
                >
                  <template #prefix>
                    <n-icon size="20"><person-outline-icon /></n-icon>
                  </template>
                </n-input>
              </n-form-item>

              <n-form-item path="password">
                <n-input
                  v-model:value="formInline.password"
                  type="password"
                  maxlength="16"
                  show-password-on="click"
                  :placeholder="$t('global.form_password')"
                  class="login-input"
                  @keydown.enter="handleSubmit"
                >
                  <template #prefix>
                    <n-icon size="20"><lock-closed-outline-icon /></n-icon>
                  </template>
                </n-input>
              </n-form-item>

              <n-form-item>
                <div class="flex justify-between items-center w-full">
                  <n-checkbox v-model:checked="autoLogin" size="small">
                    {{ $t('login.form_auto') }}
                  </n-checkbox>
                </div>
              </n-form-item>

              <n-form-item>
                <n-button
                  class="login-submit-btn"
                  type="primary"
                  size="large"
                  :loading="loading"
                  block
                  @click="handleSubmit"
                >
                  <span class="submit-text">{{ $t('login.form_button') }}</span>
                  <span class="submit-arrow">→</span>
                </n-button>
              </n-form-item>
            </n-form>

            <div class="form-footer">
              <span>默认账号 admin / 123456</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 多工作区：后端 4091 后补传 tenantId 再登录 -->
    <div
      v-if="showWorkspaceModal"
      class="workspace-modal-overlay"
      @click.self="closeWorkspaceModal"
    >
      <div class="workspace-modal" role="dialog" aria-modal="true" aria-labelledby="workspace-modal-title">
        <button type="button" class="workspace-modal-close" aria-label="关闭" @click="closeWorkspaceModal">
          ×
        </button>
        <div class="workspace-modal-header">
          <h3 id="workspace-modal-title">选择工作区</h3>
          <p>该账号可进入多个工作区，请选择后继续登录</p>
        </div>
        <div class="workspace-modal-list">
          <button
            v-for="item in tenantOptions"
            :key="String(item.tenantId)"
            type="button"
            class="workspace-option"
            :disabled="loading"
            @click="confirmWorkspace(item)"
          >
            <strong>{{ item.tenantName || item.systemName || item.browserTitle || item.tenantId }}</strong>
            <small v-if="item.systemName && item.systemName !== item.tenantName">{{ item.systemName }}</small>
          </button>
        </div>
      </div>
    </div>

    <div class="login-footer-bar">
      <layout-footer></layout-footer>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { LayoutFooter } from '@/layout/components/LayoutFooter'
import { PageEnum } from '@/enums/pageEnum'
import { icon } from '@/plugins'
import { StorageEnum } from '@/enums/storageEnum'
import { routerTurnByName, setLocalStorage } from '@/utils'
import { loginApi, type LoginTenantOption } from '@/api/auth'
import { loadUserData } from '@/utils/auth'

const { GO_ACCESS_TOKEN_STORE } = StorageEnum
const LOGIN_TENANT_SELECTION_REQUIRED = 4091

const { PersonOutlineIcon, LockClosedOutlineIcon, RocketIcon, BarChartIcon, SparklesIcon } = icon.ionicons5

const formRef = ref()
const loading = ref(false)
const autoLogin = ref(true)
const showWorkspaceModal = ref(false)
const tenantOptions = ref<LoginTenantOption[]>([])
const selectedTenantId = ref<number | string | null>(null)

const t = window['$t']

const formInline = reactive({
  username: 'admin',
  password: '123456',
})

const rules = {
  username: {
    required: true,
    message: t('global.form_account'),
    trigger: 'blur',
  },
  password: {
    required: true,
    message: t('global.form_password'),
    trigger: 'blur',
  },
}

const carouselImgList = ['one', 'two', 'three']

const getImageUrl = (name: string, folder: string) => {
  return new URL(`../../assets/images/${folder}/${name}.png`, import.meta.url).href
}

const particleStyle = (n: number) => {
  const seed = n * 137.508
  return {
    left: `${(seed * 17) % 100}%`,
    top: `${(seed * 13) % 100}%`,
    width: `${2 + (n % 3)}px`,
    height: `${2 + (n % 3)}px`,
    animationDuration: `${8 + (n % 12)}s`,
    animationDelay: `${-(n * 0.8)}s`,
    opacity: 0.15 + (n % 5) * 0.08
  }
}

function applyWorkspaceChallenge(error: any) {
  const code = error?.code ?? error?.response?.data?.code
  const rawOptions = error?.data ?? error?.response?.data?.data
  if (code !== LOGIN_TENANT_SELECTION_REQUIRED || !Array.isArray(rawOptions) || rawOptions.length <= 1) {
    return false
  }
  tenantOptions.value = rawOptions
    .map((item: LoginTenantOption) => ({
      ...item,
      tenantId: item?.tenantId,
    }))
    .filter((item: LoginTenantOption) => item.tenantId != null && item.tenantId !== '')
  selectedTenantId.value = null
  showWorkspaceModal.value = true
  return true
}

function closeWorkspaceModal() {
  showWorkspaceModal.value = false
}

async function doLogin(tenantId?: number | string | null) {
  const { username, password } = formInline
  loading.value = true
  try {
    const res = await loginApi({
      username,
      password,
      ...(tenantId != null && tenantId !== '' ? { tenantId } : {}),
    })
    if (res && res.code === 200 && res.data?.accessToken) {
      setLocalStorage(GO_ACCESS_TOKEN_STORE, res.data.accessToken)
      await loadUserData()
      window['$message'].success(`${t('login.login_success')}!`)
      routerTurnByName(PageEnum.BASE_HOME_NAME, true)
      return
    }
    window['$message'].error(res?.msg || `${t('login.login_message')}!`)
  }
  catch (error: any) {
    if (applyWorkspaceChallenge(error)) {
      return
    }
    const msg = error?.response?.data?.msg || error?.message || `${t('login.login_message')}!`
    window['$message'].error(msg)
  }
  finally {
    loading.value = false
  }
}

async function confirmWorkspace(option: LoginTenantOption) {
  if (option?.tenantId == null || loading.value) {
    return
  }
  selectedTenantId.value = option.tenantId
  showWorkspaceModal.value = false
  await doLogin(option.tenantId)
}

const handleSubmit = (e: Event) => {
  e.preventDefault()
  formRef.value.validate(async (errors: any) => {
    if (!errors) {
      selectedTenantId.value = null
      await doLogin()
    }
    else {
      window['$message'].error(`${t('login.login_message')}!`)
    }
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #05080e;

  .login-bg {
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 0;

    .bg-mesh {
      position: absolute;
      inset: 0;
      background:
        radial-gradient(ellipse at 80% 20%, rgba(var(--app-theme-rgb), 0.16), transparent 55%),
        radial-gradient(ellipse at 20% 80%, rgba(167, 139, 250, 0.13), transparent 55%),
        radial-gradient(ellipse at 50% 50%, rgba(59, 130, 246, 0.06), transparent 70%);
    }

    .bg-grid {
      position: absolute;
      inset: 0;
      background-image:
        linear-gradient(rgba(var(--app-theme-rgb), 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(var(--app-theme-rgb), 0.03) 1px, transparent 1px);
      background-size: 64px 64px;
      mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, #000 20%, transparent 70%);
    }

    .bg-orb {
      position: absolute;
      border-radius: 50%;
      filter: blur(100px);
      animation: orbDrift 25s ease-in-out infinite;
    }

    .orb-1 {
      width: 600px;
      height: 600px;
      top: -20%;
      right: -10%;
      background: rgba(var(--app-theme-rgb), 0.14);
      animation-duration: 28s;
    }

    .orb-2 {
      width: 450px;
      height: 450px;
      bottom: -15%;
      left: -8%;
      background: rgba(167, 139, 250, 0.11);
      animation-duration: 32s;
      animation-delay: -7s;
    }

    .orb-3 {
      width: 350px;
      height: 350px;
      top: 40%;
      left: 40%;
      background: rgba(59, 130, 246, 0.08);
      animation-duration: 22s;
      animation-delay: -14s;
    }

    @keyframes orbDrift {
      0%, 100% { transform: translate(0, 0) scale(1); }
      20% { transform: translate(60px, -40px) scale(1.1); }
      40% { transform: translate(-30px, 50px) scale(0.92); }
      60% { transform: translate(-50px, -30px) scale(1.06); }
      80% { transform: translate(40px, 20px) scale(0.96); }
    }

    .bg-particles {
      position: absolute;
      inset: 0;

      .particle {
        position: absolute;
        border-radius: 50%;
        background: rgba(var(--app-theme-rgb), 0.6);
        animation: particleUp linear infinite;
      }

      @keyframes particleUp {
        0% {
          transform: translateY(0) scale(1);
          opacity: 0;
        }
        10% {
          opacity: 0.8;
        }
        90% {
          opacity: 0.2;
        }
        100% {
          transform: translateY(-100vh) scale(0.4);
          opacity: 0;
        }
      }
    }
  }

  .login-body {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100vh;
    padding: 0 48px;
  }

  .login-split {
    display: flex;
    width: 100%;
    max-width: 1200px;
    gap: 60px;
    align-items: center;

    .split-left {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 20px;

      .left-brand {
        display: flex;
        align-items: center;
        gap: 12px;

        .brand-logo {
          width: 44px;
          height: 44px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 12px;
          background:
            linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.34), rgba(var(--app-theme-rgb), 0.08));
          border: 1px solid rgba(var(--app-theme-rgb), 0.22);
          box-shadow: 0 0 28px rgba(var(--app-theme-rgb), 0.16);
          overflow: hidden;

          .brand-logo-img {
            width: 38px;
            height: 38px;
            object-fit: contain;
          }
        }

        .brand-title {
          display: flex;
          flex-direction: column;
          gap: 1px;

          .brand-title-main {
            font-size: 20px;
            font-weight: 800;
            letter-spacing: 2px;
            color: transparent;
            background-clip: text;
            -webkit-background-clip: text;
            background-image: linear-gradient(135deg, var(--app-theme, #3b82f6), #a78bfa);
          }

          .brand-title-sub {
            font-size: 11px;
            letter-spacing: 3px;
            color: rgba(148, 163, 184, 0.5);
          }
        }
      }

      .left-showcase {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 18px;
        min-height: 0;
      }

      .left-carousel {
        flex: 1;
        min-height: 0;
        border-radius: 16px;
        overflow: hidden;
        border: 1px solid rgba(var(--app-theme-rgb), 0.08);
        box-shadow:
          0 0 60px rgba(var(--app-theme-rgb), 0.06),
          0 20px 60px rgba(0, 0, 0, 0.5);
        background: rgba(10, 14, 23, 0.3);

        .carousel-item {
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;

          img {
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
          }
        }

        :deep(.n-carousel) {
          height: 100%;
        }

        :deep(.n-carousel__slides) {
          height: 100%;
        }

        :deep(.n-carousel__slide) {
          height: 100%;
        }

        :deep(.n-carousel__dots) {
          bottom: 14px;

          .n-carousel__dot {
            width: 24px;
            height: 3px;
            border-radius: 2px;
            background: rgba(255, 255, 255, 0.18);

            &.n-carousel__dot--active {
              background: rgba(var(--app-theme-rgb), 0.85);
            }
          }
        }
      }

      .left-features {
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding: 6px 0;

        .feature-row {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 10px 14px;
          border-radius: 12px;
          background: rgba(15, 23, 42, 0.2);
          border: 1px solid rgba(255, 255, 255, 0.03);
          transition: all 0.22s ease;

          &:hover {
            background: rgba(var(--app-theme-rgb), 0.06);
            border-color: rgba(var(--app-theme-rgb), 0.1);
          }

          .feature-icon {
            flex-shrink: 0;
            width: 34px;
            height: 34px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 10px;
            background: rgba(var(--app-theme-rgb), 0.1);
            color: var(--app-theme, #3b82f6);
          }

          .feature-text {
            display: flex;
            flex-direction: column;
            gap: 3px;

            strong {
              font-size: 13px;
              color: rgba(226, 232, 240, 0.88);
            }

            span {
              font-size: 11px;
              color: rgba(148, 163, 184, 0.5);
              line-height: 1.4;
            }
          }
        }
      }
    }

    .split-right {
      flex: 0 0 420px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        top: -30%;
        right: -20%;
        width: 300px;
        height: 300px;
        border-radius: 50%;
        background: rgba(var(--app-theme-rgb), 0.05);
        filter: blur(100px);
        pointer-events: none;
        animation: rightOrb 8s ease-in-out infinite alternate;
      }
    }

    @keyframes rightOrb {
      0% { transform: translate(0, 0) scale(1); }
      100% { transform: translate(-40px, -20px) scale(1.3); }
    }
  }

  .login-form {
    position: relative;
    width: 100%;
    padding: 44px 40px;
    border-radius: 20px;
    background: rgba(10, 14, 23, 0.72);
    backdrop-filter: blur(40px);
    -webkit-backdrop-filter: blur(40px);
    border: 1px solid rgba(255, 255, 255, 0.06);
    box-shadow:
      0 0 100px rgba(var(--app-theme-rgb), 0.06),
      0 24px 80px rgba(0, 0, 0, 0.5);

    .form-glow-ring {
      position: absolute;
      inset: -2px;
      border-radius: 22px;
      padding: 2px;
      background: linear-gradient(
        135deg,
        rgba(var(--app-theme-rgb), 0.35),
        transparent 40%,
        transparent 60%,
        rgba(167, 139, 250, 0.25)
      );
      background-size: 300% 300%;
      animation: ringGlow 6s ease-in-out infinite;
      mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
      mask-composite: exclude;
      -webkit-mask-composite: xor;
      pointer-events: none;
      z-index: 0;
    }

    @keyframes ringGlow {
      0%, 100% { background-position: 0% 50%; }
      50% { background-position: 100% 50%; }
    }

    .form-accent {
      position: absolute;
      top: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 80px;
      height: 3px;
      border-radius: 0 0 4px 4px;
      background: linear-gradient(
        90deg,
        transparent,
        var(--app-theme, #3b82f6),
        rgba(167, 139, 250, 0.8),
        transparent
      );
      z-index: 1;
    }

    .form-header {
      position: relative;
      z-index: 1;
      margin-bottom: 32px;

      .form-header-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 14px;
        padding: 4px 12px;
        border-radius: 999px;
        font-size: 11px;
        color: rgba(var(--app-theme-rgb), 0.85);
        background: rgba(var(--app-theme-rgb), 0.08);
        border: 1px solid rgba(var(--app-theme-rgb), 0.12);
        letter-spacing: 0.5px;
      }

      h2 {
        margin: 0;
        font-size: 26px;
        font-weight: 700;
        color: rgba(226, 232, 240, 0.94);
      }

      p {
        margin: 8px 0 0;
        font-size: 13px;
        color: rgba(148, 163, 184, 0.55);
      }
    }

    :deep(.n-form-item) {
      --n-feedback-padding: 6px 0 2px;
      position: relative;
      z-index: 1;
    }

    :deep(.n-form-item-blank) {
      margin-bottom: 6px;
    }

    :deep(.login-input .n-input) {
      --n-height: 48px;
      --n-border-radius: 12px;
      --n-border: 1px solid rgba(148, 163, 184, 0.12);
      --n-border-hover: 1px solid rgba(var(--app-theme-rgb), 0.26);
      --n-border-focus: 1px solid rgba(var(--app-theme-rgb), 0.44);
      --n-box-shadow-focus: 0 0 0 3px rgba(var(--app-theme-rgb), 0.08);
      background: rgba(15, 23, 42, 0.34);
      transition: all 0.25s ease;

      &:hover {
        background: rgba(15, 23, 42, 0.48);
      }
    }

    :deep(.login-input .n-input__prefix) {
      margin-right: 10px;
      color: rgba(var(--app-theme-rgb), 0.45);
      transition: color 0.25s ease;
    }

    :deep(.login-input .n-input:focus-within .n-input__prefix) {
      color: rgba(var(--app-theme-rgb), 0.75);
    }

    :deep(.n-checkbox) {
      --n-color-checked: rgba(var(--app-theme-rgb), 0.85);
      --n-border-checked: 1px solid rgba(var(--app-theme-rgb), 0.5);
    }

    .login-submit-btn {
      position: relative;
      z-index: 1;
      --n-height: 50px;
      --n-border-radius: 12px;
      font-size: 15px;
      font-weight: 700;
      margin-top: 8px;
      background:
        linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.92), rgba(var(--app-theme-rgb), 0.64));
      border: none;
      box-shadow:
        0 4px 28px rgba(var(--app-theme-rgb), 0.3),
        inset 0 1px 0 rgba(255, 255, 255, 0.1);
      transition: all 0.3s ease;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(135deg, transparent, rgba(255, 255, 255, 0.08), transparent);
        transform: translateX(-100%);
        transition: transform 0.6s ease;
      }

      &:hover {
        box-shadow:
          0 8px 40px rgba(var(--app-theme-rgb), 0.5),
          inset 0 1px 0 rgba(255, 255, 255, 0.15);
        transform: translateY(-2px);

        &::after {
          transform: translateX(100%);
        }
      }

      &:active {
        transform: translateY(0);
      }

      .submit-text {
        position: relative;
        z-index: 1;
        letter-spacing: 6px;
      }

      .submit-arrow {
        position: relative;
        z-index: 1;
        margin-left: 4px;
        font-size: 16px;
        transition: transform 0.3s ease;
      }

      &:hover .submit-arrow {
        transform: translateX(3px);
      }
    }

    .form-footer {
      position: relative;
      z-index: 1;
      margin-top: 22px;
      text-align: center;

      span {
        font-size: 11px;
        color: rgba(148, 163, 184, 0.3);
      }
    }
  }

  .login-footer-bar {
    position: fixed;
    bottom: 0;
    width: 100%;
    z-index: 2;
  }
}

@media (max-width: 900px) {
  .login-page {
    .login-body {
      padding: 0 20px;
    }

    .login-split {
      gap: 0;

      .split-left {
        display: none;
      }

      .split-right {
        flex: 1;
        max-width: 400px;
        margin: 0 auto;
      }
    }
  }
}

.workspace-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(5, 8, 14, 0.72);
  backdrop-filter: blur(6px);
}

.workspace-modal {
  position: relative;
  width: min(420px, 100%);
  padding: 28px 24px 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.96);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.35);
  color: #e2e8f0;
}

.workspace-modal-close {
  position: absolute;
  top: 12px;
  right: 14px;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #94a3b8;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.workspace-modal-header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
}

.workspace-modal-header p {
  margin: 0 0 18px;
  color: #94a3b8;
  font-size: 13px;
}

.workspace-modal-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: min(360px, 50vh);
  overflow: auto;
}

.workspace-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: rgba(30, 41, 59, 0.72);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.workspace-option:hover:not(:disabled) {
  border-color: rgba(var(--app-theme-rgb), 0.55);
  background: rgba(var(--app-theme-rgb), 0.12);
}

.workspace-option:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.workspace-option strong {
  font-size: 14px;
  font-weight: 600;
}

.workspace-option small {
  color: #94a3b8;
  font-size: 12px;
}
</style>
