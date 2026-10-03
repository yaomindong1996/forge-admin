<template>
  <n-config-provider :theme-overrides="loginThemeOverrides">
    <div class="login-container">
      <div class="login-bg-animated" />

      <div class="login-card">
        <!-- 品牌轮播与真实系统标识 -->
        <LoginBrandPanel :logo="brandLogoUrl" :system-name="brandSystemName" @logo-error="handleBrandLogoError" />

        <!-- Right side - Login form -->
        <div class="login-form-wrapper">
          <button class="login-support-link" type="button" @click="supportVisible = true">
            遇到问题？<span>请联系系统管理员</span>
          </button>
          <div class="mobile-brand">
            <img :src="brandLogoUrl" :alt="brandSystemName" @error="handleBrandLogoError">
            <strong>{{ brandSystemName }}</strong>
          </div>
          <div class="login-form">
            <div class="form-header">
              <h2 class="form-title">
                {{ showResetForm ? '找回密码' : '欢迎登录' }}
              </h2>
              <p class="form-subtitle">
                {{ showResetForm ? resetSubtitle : loginSubtitle }}
              </p>
            </div>

            <div v-if="!showResetForm" class="form-body">
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
              <!-- Tab 切换（如果启用群二维码引流） -->
              <div v-if="captchaEnabled && groupQrcodeEnabled" class="captcha-tab-switch">
                <button
                  type="button"
                  class="captcha-tab-btn"
                  :class="{ active: activeCaptchaTab === 'default' }"
                  @click="activeCaptchaTab = 'default'"
                >
                  图形验证码
                </button>
                <button
                  type="button"
                  class="captcha-tab-btn"
                  :class="{ active: activeCaptchaTab === 'group' }"
                  @click="activeCaptchaTab = 'group'"
                >
                  群二维码
                </button>
              </div>

              <!-- 默认验证码（图形/滑块/短信） -->
              <div v-if="captchaEnabled && activeCaptchaTab === 'default'" class="form-group">
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

              <!-- 群二维码验证码：独立于验证码总开关，点“获取验证码”在下方浮出二维码 -->
              <div v-if="groupQrcodeEnabled && activeCaptchaTab === 'group'" class="form-group group-qrcode-verify">
                <label for="groupCaptchaCode" class="form-label">验证码</label>
                <div class="captcha-wrapper">
                  <div class="input-wrapper flex-1">
                    <n-input
                      id="groupCaptchaCode"
                      v-model:value="loginInfo.code"
                      class="modern-input"
                      placeholder="请输入验证码"
                      :maxlength="10"
                      size="large"
                      @keydown.enter="handleLogin()"
                    >
                      <template #prefix>
                        <i class="input-icon ai-icon:key" />
                      </template>
                    </n-input>
                  </div>
                  <button
                    type="button"
                    class="qrcode-trigger-btn"
                    :class="{ active: qrcodePopoverVisible }"
                    :aria-expanded="qrcodePopoverVisible"
                    title="点击展示群二维码"
                    @click="toggleQrcodePopover"
                  >
                    <i class="i-material-symbols:qr-code-2-outline" />
                    <span>{{ qrcodePopoverVisible ? '收起' : '获取验证码' }}</span>
                  </button>
                </div>
                <Transition name="qrcode-pop">
                  <div v-if="qrcodePopoverVisible" class="qrcode-popover">
                    <div
                      class="qrcode-container"
                      :class="{ zoomable: !!groupQrcodeImage }"
                      :role="groupQrcodeImage ? 'button' : undefined"
                      :tabindex="groupQrcodeImage ? 0 : undefined"
                      title="点击放大二维码"
                      @click="groupQrcodeImage && (qrcodePreviewVisible = true)"
                      @keydown.enter="groupQrcodeImage && (qrcodePreviewVisible = true)"
                    >
                      <img
                        v-if="groupQrcodeImage"
                        :src="groupQrcodeImage"
                        :alt="groupQrcodeName"
                        class="qrcode-image"
                      >
                      <div v-else class="qrcode-placeholder">
                        <i class="ai-icon:image" />
                        <span>群二维码未配置</span>
                      </div>
                      <i v-if="groupQrcodeImage" class="qrcode-zoom-hint ai-icon:zoom-in" />
                    </div>
                    <div class="qrcode-info">
                      <h4 class="qrcode-title">
                        {{ groupQrcodeName }}
                      </h4>
                      <p class="qrcode-hint">
                        {{ groupQrcodeHint }}
                      </p>
                    </div>
                  </div>
                </Transition>
              </div>

              <!-- Remember me -->
              <div class="form-options">
                <n-checkbox
                  :checked="isRemember"
                  :on-update:checked="(val) => (isRemember = val)"
                >
                  <span class="checkbox-label">记住我</span>
                </n-checkbox>
                <button
                  v-if="canResetPassword"
                  type="button"
                  class="forgot-link"
                  @click="openResetForm"
                >
                  忘记密码
                </button>
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

              <div class="login-trust-strip">
                <span><i class="ai-icon:shield" />安全登录</span>
                <span><i class="ai-icon:layers" />租户隔离</span>
                <span><i class="ai-icon:check-circle" />操作审计</span>
              </div>

              <!-- Social login buttons -->
              <div v-if="displaySocialPlatforms.length > 0" class="social-login-section">
                <div class="social-divider">
                  <span class="divider-text">其他登录方式</span>
                </div>
                <!-- Gitee 社区登录：突出展示 -->
                <button
                  v-if="giteeCommunity.enabled"
                  class="social-button-gitee"
                  @click="handleSocialLogin('GITEE')"
                >
                  <i class="gitee-icon i-simple-icons:gitee" aria-hidden="true" />
                  <span class="gitee-label">Gitee 免密登录</span>
                </button>
                <!-- 其他三方平台 -->
                <div v-if="nonGiteePlatforms.length > 0" class="social-buttons">
                  <button
                    v-for="platform in nonGiteePlatforms"
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
                    <span v-else class="social-icon-letter">
                      {{ (platform.platformName || platform.platform).charAt(0) }}
                    </span>
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="form-body">
              <div v-if="resetPasswordChannels.length > 1" class="form-group">
                <label class="form-label">验证方式</label>
                <n-radio-group v-model:value="resetForm.channel" name="resetChannel">
                  <n-radio v-if="resetPasswordChannels.includes('sms')" value="sms">
                    手机号
                  </n-radio>
                  <n-radio v-if="resetPasswordChannels.includes('email')" value="email">
                    邮箱
                  </n-radio>
                </n-radio-group>
              </div>
              <div class="form-group">
                <label class="form-label">{{ resetAccountLabel }}</label>
                <div class="input-wrapper">
                  <n-input
                    v-model:value="resetForm.account"
                    class="modern-input"
                    :placeholder="resetAccountPlaceholder"
                    size="large"
                  >
                    <template #prefix>
                      <i class="input-icon" :class="resetForm.channel === 'email' ? 'ai-icon:mail' : 'ai-icon:phone'" />
                    </template>
                  </n-input>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">验证码</label>
                <div class="captcha-wrapper">
                  <div class="input-wrapper flex-1">
                    <n-input
                      v-model:value="resetForm.code"
                      class="modern-input"
                      placeholder="请输入验证码"
                      :maxlength="6"
                      size="large"
                    >
                      <template #prefix>
                        <i class="input-icon ai-icon:key" />
                      </template>
                    </n-input>
                  </div>
                  <n-button
                    :disabled="resetCountdown > 0 || resetSending || !resetAccountValid"
                    class="sms-button"
                    size="large"
                    :loading="resetSending"
                    @click="sendResetCode"
                  >
                    {{ resetCountdown > 0 ? `${resetCountdown}s后重发` : '获取验证码' }}
                  </n-button>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">新密码</label>
                <div class="input-wrapper">
                  <n-input
                    v-model:value="resetForm.newPassword"
                    class="modern-input"
                    type="password"
                    show-password-on="click"
                    placeholder="请输入新密码"
                    :maxlength="20"
                    size="large"
                  >
                    <template #prefix>
                      <i class="input-icon ai-icon:lock" />
                    </template>
                  </n-input>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">确认密码</label>
                <div class="input-wrapper">
                  <n-input
                    v-model:value="resetForm.confirmPassword"
                    class="modern-input"
                    type="password"
                    show-password-on="click"
                    placeholder="请再次输入新密码"
                    :maxlength="20"
                    size="large"
                  >
                    <template #prefix>
                      <i class="input-icon ai-icon:lock" />
                    </template>
                  </n-input>
                </div>
              </div>
              <n-button
                class="login-button"
                type="primary"
                size="large"
                :loading="resetSubmitting"
                block
                @click="submitResetPassword"
              >
                <span class="button-text">重置密码</span>
              </n-button>
              <button type="button" class="back-login-link" @click="closeResetForm">
                返回登录
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ICP备案号 -->
      <CommunitySupportDialog
        v-model:show="supportVisible"
        :group-image="groupQrcodeImage"
        :group-name="groupQrcodeName"
      />

      <div class="icp-record">
        <span v-if="copyrightInfo">{{ copyrightInfo }}</span>
        <a v-else href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">蒙ICP备2026004895号</a>
      </div>
    </div>

    <!-- 群二维码放大预览浮层 -->
    <Transition name="modal">
      <div
        v-if="qrcodePreviewVisible"
        class="slider-modal-overlay qrcode-preview-overlay"
        role="dialog"
        aria-label="群二维码放大预览"
        @click.self="qrcodePreviewVisible = false"
      >
        <div class="qrcode-preview-modal">
          <button type="button" class="slider-modal-close" aria-label="关闭" @click="qrcodePreviewVisible = false">
            <i class="ai-icon:x" />
          </button>
          <div class="qrcode-preview-header">
            <h3>{{ groupQrcodeName }}</h3>
            <p>{{ groupQrcodeHint }}</p>
          </div>
          <img :src="groupQrcodeImage" :alt="groupQrcodeName" class="qrcode-preview-image" @click.stop>
        </div>
      </div>
    </Transition>

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
            <!-- slideVerifyRef 由认证 composable 使用，保留滑块校验实例 -->
            <SlideVerify
              :ref="value => slideVerifyRef = value"
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

    <Transition name="modal">
      <div
        v-if="showWorkspaceModal"
        class="slider-modal-overlay"
        @click.self="closeWorkspaceModal"
      >
        <div
          class="workspace-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="workspace-modal-title"
        >
          <button type="button" class="slider-modal-close" aria-label="关闭" @click="closeWorkspaceModal">
            <i class="ai-icon:x" />
          </button>
          <div class="workspace-modal-header">
            <h3 id="workspace-modal-title">
              选择工作区
            </h3>
            <p>该账号可进入多个工作区，请选择后继续登录</p>
          </div>
          <div class="workspace-modal-list">
            <button
              v-for="item in tenantOptions"
              :key="item.value"
              type="button"
              class="workspace-option"
              :class="{ 'is-current': String(item.value) === String(lastUsedTenantId) }"
              :disabled="loading"
              @click="confirmWorkspace(item)"
            >
              <span class="workspace-option-copy">
                <strong>{{ item.label }}</strong>
                <small v-if="item.systemName && item.systemName !== item.label">{{ item.systemName }}</small>
              </span>
              <em v-if="String(item.value) === String(lastUsedTenantId)">上次使用</em>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </n-config-provider>
</template>

<script>
import { useLoginPage } from './composables/useLoginPage'
import { loginPageLocalComponents } from './loginPageLocalComponents'

export default {
  name: 'LoginPage',
  components: {
    ...loginPageLocalComponents,
  },
  setup() {
    return useLoginPage()
  },
}
</script>

<style scoped src="./loginPage.css"></style>
