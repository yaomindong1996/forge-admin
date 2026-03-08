<template>
  <div class="config-center-container">
    <n-card title="统一配置中心" :bordered="false">
      <n-tabs type="line" animated>
        <!-- 登录配置 -->
        <n-tab-pane name="login" tab="登录配置">
          <n-form
            ref="loginFormRef"
            :model="configForms.login"
            :rules="formRules.login"
            label-placement="left"
            label-width="180"
            size="medium"
          >
            <n-grid :cols="1" :x-gap="12" :y-gap="8">
              <n-grid-item>
                <n-form-item label="启用验证码" path="enableCaptcha">
                  <n-switch v-model:value="configForms.login.enableCaptcha" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="验证码类型" path="captchaType">
                  <DictSelect
                    v-model:value="configForms.login.captchaType"
                    dict-type="captcha_type"
                  />
                </n-form-item>
              </n-grid-item>
            
              <n-grid-item>
                <n-form-item label="启用记住我" path="enableRememberMe">
                  <n-switch v-model:value="configForms.login.enableRememberMe" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="记住我有效天数" path="rememberMeDays">
                  <n-input-number
                    v-model:value="configForms.login.rememberMeDays"
                    :min="1"
                    :max="365"
                    placeholder="请输入记住我有效天数"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
            <n-space>
              <n-button type="primary" @click="saveConfig('login')">保存登录配置</n-button>
            </n-space>
          </n-form>
        </n-tab-pane>

        <!-- 水印配置 -->
        <n-tab-pane name="watermark" tab="水印配置">
          <n-form
            ref="watermarkFormRef"
            :model="configForms.watermark"
            :rules="formRules.watermark"
            label-placement="left"
            label-width="180"
            size="medium"
          >
            <n-grid :cols="1" :x-gap="12" :y-gap="8">
              <n-grid-item>
                <n-form-item label="启用水印" path="enable">
                  <n-switch v-model:value="configForms.watermark.enable" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="水印内容" path="content">
                <DictSelect
                    v-model:value="configForms.watermark.content"
                    dict-type="water_marker_content"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="水印透明度" path="opacity">
                  <n-slider
                    v-model:value="configForms.watermark.opacity"
                    :min="0.1"
                    :max="1"
                    :step="0.1"
                    :format-tooltip="(value) => `${Math.round(value * 100)}%`"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="字体大小" path="fontSize">
                  <n-input-number
                    v-model:value="configForms.watermark.fontSize"
                    :min="10"
                    :max="50"
                    placeholder="请输入字体大小"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="字体颜色" path="fontColor">
                  <n-color-picker
                    v-model:value="configForms.watermark.fontColor"
                    :modes="['hex']"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="旋转角度" path="rotate">
                  <n-input-number
                    v-model:value="configForms.watermark.rotate"
                    :min="-180"
                    :max="180"
                    placeholder="请输入旋转角度"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="X轴间距" path="gapX">
                  <n-input-number
                    v-model:value="configForms.watermark.gapX"
                    :min="0"
                    :max="200"
                    placeholder="请输入X轴间距"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="Y轴间距" path="gapY">
                  <n-input-number
                    v-model:value="configForms.watermark.gapY"
                    :min="0"
                    :max="200"
                    placeholder="请输入Y轴间距"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="显示时间戳" path="showTimestamp">
                  <n-switch v-model:value="configForms.watermark.showTimestamp" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item v-if="configForms.watermark.showTimestamp">
                <n-form-item label="时间戳格式" path="timestampFormat">
                  <n-input
                    v-model:value="configForms.watermark.timestampFormat"
                    placeholder="请输入时间戳格式，如：yyyy-MM-dd HH:mm:ss"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
            <n-space>
              <n-button type="primary" @click="saveConfig('watermark')">保存水印配置</n-button>
            </n-space>
          </n-form>
        </n-tab-pane>

        <!-- 安全配置 -->
        <n-tab-pane name="security" tab="安全配置">
          <n-form
            ref="securityFormRef"
            :model="configForms.security"
            :rules="formRules.security"
            label-placement="left"
            label-width="180"
            size="medium"
          >
            <n-grid :cols="1" :x-gap="12" :y-gap="8">
              <n-grid-item>
                <n-form-item label="Sa-Token超时时间(秒)" path="saToken.timeout">
                  <n-input-number
                    v-model:value="configForms.security.saToken.timeout"
                    :min="1"
                    :max="2592000"
                    placeholder="请输入token超时时间"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="活动超时时间(秒)" path="saToken.activityTimeout">
                  <n-input-number
                    v-model:value="configForms.security.saToken.activityTimeout"
                    :min="-1"
                    :max="2592000"
                    placeholder="请输入活动超时时间(-1表示永不超时)"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="是否允许并发登录" path="saToken.isConcurrent">
                  <n-switch v-model:value="configForms.security.saToken.isConcurrent" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="是否共享token" path="saToken.isShare">
                  <n-switch v-model:value="configForms.security.saToken.isShare" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="是否读取请求体token" path="saToken.isReadBody">
                  <n-switch v-model:value="configForms.security.saToken.isReadBody" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="是否读取Header token" path="saToken.isReadHeader">
                  <n-switch v-model:value="configForms.security.saToken.isReadHeader" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="是否读取Cookie token" path="saToken.isReadCookie">
                  <n-switch v-model:value="configForms.security.saToken.isReadCookie" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="Token前缀" path="saToken.tokenPrefix">
                  <n-input
                    v-model:value="configForms.security.saToken.tokenPrefix"
                    placeholder="请输入token前缀"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="Token名称" path="saToken.tokenName">
                  <n-input
                    v-model:value="configForms.security.saToken.tokenName"
                    placeholder="请输入token名称"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="最小密码长度" path="passwordPolicy.minLength">
                  <n-input-number
                    v-model:value="configForms.security.passwordPolicy.minLength"
                    :min="6"
                    :max="30"
                    placeholder="请输入最小密码长度"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="密码需包含大写字母" path="passwordPolicy.requireUppercase">
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireUppercase" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="密码需包含小写字母" path="passwordPolicy.requireLowercase">
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireLowercase" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="密码需包含数字" path="passwordPolicy.requireNumbers">
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireNumbers" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="密码需包含特殊字符" path="passwordPolicy.requireSpecialChars">
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireSpecialChars" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="密码过期天数" path="passwordPolicy.expireDays">
                  <n-input-number
                    v-model:value="configForms.security.passwordPolicy.expireDays"
                    :min="1"
                    :max="365"
                    placeholder="请输入密码过期天数"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="密码历史记录数量" path="passwordPolicy.historyCount">
                  <n-input-number
                    v-model:value="configForms.security.passwordPolicy.historyCount"
                    :min="1"
                    :max="20"
                    placeholder="请输入密码历史记录数量"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
            <n-space>
              <n-button type="primary" @click="saveConfig('security')">保存安全配置</n-button>
            </n-space>
          </n-form>
        </n-tab-pane>

        <!-- 加解密配置 -->
        <n-tab-pane name="crypto" tab="加解密配置">
          <n-form
            ref="cryptoFormRef"
            :model="configForms.crypto"
            :rules="formRules.crypto"
            label-placement="left"
            label-width="180"
            size="medium"
          >
            <n-grid :cols="1" :x-gap="12" :y-gap="8">
              <n-grid-item>
                <n-form-item label="启用加密" path="enabled">
                  <n-switch v-model:value="configForms.crypto.enabled" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="加密算法" path="algorithm">
                  <n-select
                    v-model:value="configForms.crypto.algorithm"
                    :options="[
                      { label: 'SM4', value: 'SM4' },
                      { label: 'AES', value: 'AES' }
                    ]"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="对称加密密钥" path="secretKey">
                  <n-input
                    v-model:value="configForms.crypto.secretKey"
                    type="password"
                    show-password-on="click"
                    placeholder="请输入对称加密密钥(Base64编码)"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用动态密钥协商" path="enableDynamicKey">
                  <n-switch v-model:value="configForms.crypto.enableDynamicKey" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="RSA公钥" path="rsaPublicKey">
                  <n-input
                    v-model:value="configForms.crypto.rsaPublicKey"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 6 }"
                    placeholder="请输入RSA公钥(Base64编码)"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="RSA私钥" path="rsaPrivateKey">
                  <n-input
                    v-model:value="configForms.crypto.rsaPrivateKey"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 6 }"
                    placeholder="请输入RSA私钥(Base64编码)"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="会话密钥过期时间" path="sessionKeyExpire">
                  <n-input-number
                    v-model:value="configForms.crypto.sessionKeyExpire"
                    :min="60"
                    :max="86400"
                    placeholder="请输入会话密钥过期时间(秒)"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用API级加解密" path="enableApiCrypto">
                  <n-switch v-model:value="configForms.crypto.enableApiCrypto" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用字段级加解密" path="enableFieldCrypto">
                  <n-switch v-model:value="configForms.crypto.enableFieldCrypto" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用防重放攻击保护" path="enableReplayProtection">
                  <n-switch v-model:value="configForms.crypto.enableReplayProtection" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item v-if="configForms.crypto.enableReplayProtection">
                <n-form-item label="防重放时间窗口" path="replayTimeWindow">
                  <n-input-number
                    v-model:value="configForms.crypto.replayTimeWindow"
                    :min="60"
                    :max="3600"
                    placeholder="请输入防重放时间窗口(秒)"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="API加解密排除路径" path="excludePaths">
                  <n-dynamic-tags
                    v-model:value="configForms.crypto.excludePaths"
                    :input-placeholder="'请输入排除路径，如：/api/public'"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="防重放排除路径" path="replayExcludePaths">
                  <n-dynamic-tags
                    v-model:value="configForms.crypto.replayExcludePaths"
                    :input-placeholder="'请输入防重放排除路径，如：/api/public'"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
            <n-space>
              <n-button type="primary" @click="saveConfig('crypto')">保存加解密配置</n-button>
            </n-space>
          </n-form>
        </n-tab-pane>

        <!-- 认证配置 -->
        <n-tab-pane name="auth" tab="认证配置">
          <n-form
            ref="authFormRef"
            :model="configForms.auth"
            :rules="formRules.auth"
            label-placement="left"
            label-width="180"
            size="medium"
          >
            <n-grid :cols="1" :x-gap="12" :y-gap="8">
              <n-grid-item>
                <n-form-item label="启用API接口权限校验" path="enableApiPermission">
                  <n-switch v-model:value="configForms.auth.enableApiPermission" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用登录失败锁定功能" path="enableLoginLock">
                  <n-switch v-model:value="configForms.auth.enableLoginLock" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="最大登录失败尝试次数" path="maxLoginAttempts">
                  <n-input-number
                    v-model:value="configForms.auth.maxLoginAttempts"
                    :min="1"
                    :max="10"
                    placeholder="请输入最大登录失败尝试次数"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="账号锁定时长(分钟)" path="lockDuration">
                  <n-input-number
                    v-model:value="configForms.auth.lockDuration"
                    :min="1"
                    :max="1440"
                    placeholder="请输入账号锁定时长"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="登录失败记录保留时长(分钟)" path="failRecordExpire">
                  <n-input-number
                    v-model:value="configForms.auth.failRecordExpire"
                    :min="1"
                    :max="1440"
                    placeholder="请输入登录失败记录保留时长"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="同一账号登录策略" path="sameAccountLoginStrategy">
                  <n-select
                    v-model:value="configForms.auth.sameAccountLoginStrategy"
                    :options="[
                      { label: '允许并发登录', value: 'allow_concurrent' },
                      { label: '新登录踢出旧登录', value: 'replace_old' },
                      { label: '拒绝新登录', value: 'reject_new' }
                    ]"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用在线用户管理" path="enableOnlineUserManagement">
                  <n-switch v-model:value="configForms.auth.enableOnlineUserManagement" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="API权限校验排除路径" path="apiPermissionExcludePaths">
                  <n-dynamic-tags
                    v-model:value="configForms.auth.apiPermissionExcludePaths"
                    :input-placeholder="'请输入排除路径，如：/auth/**'"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
            <n-space>
              <n-button type="primary" @click="saveConfig('auth')">保存认证配置</n-button>
            </n-space>
          </n-form>
        </n-tab-pane>

        <!-- 日志配置 -->
        <n-tab-pane name="log" tab="日志配置">
          <n-form
            ref="logFormRef"
            :model="configForms.log"
            :rules="formRules.log"
            label-placement="left"
            label-width="180"
            size="medium"
          >
            <n-grid :cols="1" :x-gap="12" :y-gap="8">
              <n-grid-item>
                <n-form-item label="启用操作日志" path="enableOperationLog">
                  <n-switch v-model:value="configForms.log.enableOperationLog" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="启用登录日志" path="enableLoginLog">
                  <n-switch v-model:value="configForms.log.enableLoginLog" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="请求参数最大长度" path="requestParamsMaxLength">
                  <n-input-number
                    v-model:value="configForms.log.requestParamsMaxLength"
                    :min="100"
                    :max="10000"
                    placeholder="请输入请求参数最大长度"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="响应结果最大长度" path="responseResultMaxLength">
                  <n-input-number
                    v-model:value="configForms.log.responseResultMaxLength"
                    :min="100"
                    :max="10000"
                    placeholder="请输入响应结果最大长度"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="在控制台打印操作日志" path="printOperationLog">
                  <n-switch v-model:value="configForms.log.printOperationLog" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="在控制台打印登录日志" path="printLoginLog">
                  <n-switch v-model:value="configForms.log.printLoginLog" />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="线程池核心线程数" path="threadPoolCoreSize">
                  <n-input-number
                    v-model:value="configForms.log.threadPoolCoreSize"
                    :min="1"
                    :max="20"
                    placeholder="请输入线程池核心线程数"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="线程池最大线程数" path="threadPoolMaxSize">
                  <n-input-number
                    v-model:value="configForms.log.threadPoolMaxSize"
                    :min="1"
                    :max="50"
                    placeholder="请输入线程池最大线程数"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="线程池队列容量" path="threadPoolQueueCapacity">
                  <n-input-number
                    v-model:value="configForms.log.threadPoolQueueCapacity"
                    :min="100"
                    :max="2000"
                    placeholder="请输入线程池队列容量"
                  />
                </n-form-item>
              </n-grid-item>
              <n-grid-item>
                <n-form-item label="排除的URL路径" path="excludePaths">
                  <n-dynamic-tags
                    v-model:value="configForms.log.excludePaths"
                    :input-placeholder="'请输入排除路径，如：/health'"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
            <n-space>
              <n-button type="primary" @click="saveConfig('log')">保存日志配置</n-button>
            </n-space>
          </n-form>
        </n-tab-pane>
      </n-tabs>

      <!-- 底部操作栏 -->
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleRefreshConfig">刷新配置</n-button>
          <n-button type="primary" @click="saveAllConfig">保存全部配置</n-button>
        </n-space>
      </template>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NCard, NTabs, NTabPane, NForm, NFormItem, NSwitch, NInput, NInputNumber, NSelect, NSlider, NColorPicker, NGrid, NGridItem, NSpace, NButton, NDynamicTags } from 'naive-ui'
import { getConfigByGroup, updateConfigByGroup, refreshConfig, getLoginConfig, updateLoginConfig, getWatermarkConfig, updateWatermarkConfig, getSecurityConfig, updateSecurityConfig, getSystemConfig, updateSystemConfig, getCryptoConfig, updateCryptoConfig, getAuthConfig, updateAuthConfig, getLogConfig, updateLogConfig } from '@/api/config'
import { DictSelect } from '@/components'

// 表单引用
const loginFormRef = ref(null)
const watermarkFormRef = ref(null)
const securityFormRef = ref(null)
const cryptoFormRef = ref(null)
const authFormRef = ref(null)
const logFormRef = ref(null)

// 配置表单数据
const configForms = ref({
  login: {
    enableCaptcha: true,
    captcha_type: 'char',
    maxRetryCount: 4,
    lockTimeMinutes: 30,
    enableRememberMe: true,
    rememberMeDays: 30
  },
  watermark: {
    enable: true,
    water_marker_content: '系统水印',
    opacity: 0.5,
    fontSize: 16,
    fontColor: '#ffffff',
    rotate: -20,
    gapX: 200,
    gapY: 100,
    offsetX: 0,
    offsetY: 0,
    zIndex: 9999,
    showTimestamp: false,
    timestampFormat: 'yyyy-MM-dd HH:mm:ss'
  },
  security: {
    saToken: {
      timeout: 2592000,
      activityTimeout: -1,
      isConcurrent: false,
      isShare: false,
      isReadBody: true,
      isReadHeader: true,
      isReadCookie: false,
      tokenPrefix: 'Bearer',
      tokenName: 'Authorization'
    },
    passwordPolicy: {
      minLength: 8,
      requireUppercase: true,
      requireLowercase: true,
      requireNumbers: true,
      requireSpecialChars: false,
      expireDays: 90,
      historyCount: 5
    }
  },
  crypto: {
    enabled: false,
    algorithm: 'SM4',
    secretKey: '',
    enableDynamicKey: true,
    rsaPublicKey: '',
    rsaPrivateKey: '',
    sessionKeyExpire: 7200,
    enableApiCrypto: true,
    enableFieldCrypto: true,
    enableReplayProtection: false,
    replayTimeWindow: 300,
    excludePaths: []
  },
  auth: {
    enableApiPermission: true,
    apiPermissionExcludePaths: ['/auth/**'],
    enableLoginLock: true,
    maxLoginAttempts: 4,
    lockDuration: 30,
    failRecordExpire: 15,
    sameAccountLoginStrategy: 'replace_old',
    enableOnlineUserManagement: true
  },
  log: {
    enableOperationLog: true,
    enableLoginLog: true,
    requestParamsMaxLength: 2000,
    responseResultMaxLength: 2000,
    excludePaths: ['/auth/captcha', '/actuator/**', '/swagger-ui/**', '/v3/api-docs/**'],
    printOperationLog: true,
    printLoginLog: true,
    threadPoolCoreSize: 2,
    threadPoolMaxSize: 5,
    threadPoolQueueCapacity: 500
  }
})

// 表单验证规则
const formRules = {
  login: {
    maxRetryCount: { required: true, message: '请输入最大重试次数', trigger: 'blur' },
    lockTimeMinutes: { required: true, message: '请输入锁定时间', trigger: 'blur' },
    rememberMeDays: { required: true, message: '请输入记住我有效天数', trigger: 'blur' }
  },
  watermark: {
    water_marker_content: { required: true, message: '请输入水印内容', trigger: 'blur' },
    opacity: { required: true, message: '请选择水印透明度', trigger: 'blur' },
    fontSize: { required: true, message: '请输入字体大小', trigger: 'blur' },
    fontColor: { required: true, message: '请选择字体颜色', trigger: 'blur' },
    rotate: { required: true, message: '请输入旋转角度', trigger: 'blur' },
    gapX: { required: true, message: '请输入X轴间距', trigger: 'blur' },
    gapY: { required: true, message: '请输入Y轴间距', trigger: 'blur' }
  },
  security: {
    'saToken.timeout': { required: true, message: '请输入Sa-Token超时时间', trigger: 'blur' },
    'saToken.activityTimeout': { required: true, message: '请输入活动超时时间', trigger: 'blur' },
    'saToken.tokenPrefix': { required: true, message: '请输入Token前缀', trigger: 'blur' },
    'saToken.tokenName': { required: true, message: '请输入Token名称', trigger: 'blur' },
    'passwordPolicy.minLength': { required: true, message: '请输入最小密码长度', trigger: 'blur' },
    'passwordPolicy.expireDays': { required: true, message: '请输入密码过期天数', trigger: 'blur' },
    'passwordPolicy.historyCount': { required: true, message: '请输入密码历史记录数量', trigger: 'blur' }
  },
  crypto: {
    algorithm: { required: true, message: '请选择加密算法', trigger: 'blur' },
    sessionKeyExpire: { required: true, message: '请输入会话密钥过期时间', trigger: 'blur' }
  },
  auth: {
    maxLoginAttempts: { required: true, message: '请输入最大登录失败尝试次数', trigger: 'blur' },
    lockDuration: { required: true, message: '请输入账号锁定时长', trigger: 'blur' },
    failRecordExpire: { required: true, message: '请输入登录失败记录保留时长', trigger: 'blur' },
    sameAccountLoginStrategy: { required: true, message: '请选择同一账号登录策略', trigger: 'blur' }
  },
  log: {
    requestParamsMaxLength: { required: true, message: '请输入请求参数最大长度', trigger: 'blur' },
    responseResultMaxLength: { required: true, message: '请输入响应结果最大长度', trigger: 'blur' },
    threadPoolCoreSize: { required: true, message: '请输入线程池核心线程数', trigger: 'blur' },
    threadPoolMaxSize: { required: true, message: '请输入线程池最大线程数', trigger: 'blur' },
    threadPoolQueueCapacity: { required: true, message: '请输入线程池队列容量', trigger: 'blur' }
  }
}

// 获取配置
const getConfig = async (groupCode) => {
  try {
    let res
    // 根据配置类型选择不同的API
    switch(groupCode) {
      case 'login':
        res = await getLoginConfig()
        break
      case 'watermark':
        res = await getWatermarkConfig()
        break
      case 'security':
        res = await getSecurityConfig()
        break
      case 'system':
        res = await getSystemConfig()
        break
      case 'crypto':
        res = await getCryptoConfig()
        break
      case 'auth':
        res = await getAuthConfig()
        break
      case 'log':
        res = await getLogConfig()
        break
      default:
        res = await getConfigByGroup(groupCode)
    }
    
    if (res.code === 200) {
      configForms.value[groupCode] = res.data
    }
  } catch (error) {
    console.error(`获取${groupCode}配置失败:`, error)
  }
}

// 保存配置
const saveConfig = async (groupCode) => {
  try {
    let res
    // 对配置数据进行字段名映射
    let configData = configForms.value[groupCode]
    
    // 根据配置类型选择不同的API
    switch(groupCode) {
      case 'login':
        res = await updateLoginConfig(configData)
        break
      case 'watermark':
        res = await updateWatermarkConfig(configData)
        break
      case 'security':
        res = await updateSecurityConfig(configData)
        break
      case 'system':
        res = await updateSystemConfig(configData)
        break
      case 'crypto':
        res = await updateCryptoConfig(configData)
        break
      case 'auth':
        res = await updateAuthConfig(configData)
        break
      case 'log':
        res = await updateLogConfig(configData)
        break
      default:
        res = await updateConfigByGroup(groupCode, configData)
    }
    
    if (res.code === 200) {
      window.$message.success(`${getGroupName(groupCode)}配置保存成功`)
    } else {
      window.$message.error(`${getGroupName(groupCode)}配置保存失败: ${res.message}`)
    }
  } catch (error) {
    console.error(`保存${groupCode}配置失败:`, error)
    window.$message.error(`${getGroupName(groupCode)}配置保存失败`)
  }
}

// 保存全部配置
const saveAllConfig = async () => {
  const groups = ['login', 'watermark', 'security', 'crypto', 'auth', 'log']
  for (const group of groups) {
    await saveConfig(group)
  }
  window.$message.success('全部配置保存成功')
}

// 刷新配置
const handleRefreshConfig = async () => {
  try {
    const res = await refreshConfig()
    if (res.code === 200) {
      window.$message.success('配置刷新成功')
    } else {
      window.$message.error(`配置刷新失败: ${res.message}`)
    }
  } catch (error) {
    console.error('配置刷新失败:', error)
    window.$message.error('配置刷新失败')
  }
}

// 获取分组名称
const getGroupName = (groupCode) => {
  const groupNames = {
    login: '登录',
    watermark: '水印',
    security: '安全',
    crypto: '加解密',
    auth: '认证',
    log: '日志'
  }
  return groupNames[groupCode] || groupCode
}

// 页面加载时获取所有配置
onMounted(async () => {
  const groups = ['login', 'watermark', 'security', 'crypto', 'auth', 'log']
  for (const group of groups) {
    await getConfig(group)
  }
})
</script>

<style scoped>
.config-center-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}
</style>