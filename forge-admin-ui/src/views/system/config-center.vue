<template>
  <div class="config-center-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon">
            <i class="i-material-symbols:settings-suggest" />
          </div>
          <h2 class="page-title">
            统一配置中心
          </h2>
        </div>
        <div class="header-desc">
          系统核心配置集中管理，支持实时更新与生效
        </div>
      </div>
      <div class="header-right">
        <NButton class="refresh-btn" @click="handleRefreshConfig">
          <i class="i-material-symbols:refresh mr-2" />
          刷新配置
        </NButton>
        <NButton type="primary" class="save-all-btn" :loading="saveAllLoading" @click="saveAllConfig">
          <i class="i-material-symbols:save mr-2" />
          保存全部
        </NButton>
      </div>
    </div>

    <!-- 配置面板 -->
    <div class="config-panel">
      <n-tabs v-model:value="activeTab" type="line" animated>
        <!-- 登录配置 -->
        <n-tab-pane name="login">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:login tab-icon" />
              <span>登录配置</span>
            </div>
          </template>
          <div class="config-section">
            <div class="section-header">
              <div class="section-title">
                <i class="i-material-symbols:login section-icon" />
                登录配置
              </div>
              <div class="section-desc">
                控制登录验证、记住我等登录相关功能
              </div>
            </div>
            <div class="config-grid">
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:verified-user-outline" />
                  启用验证码
                </div>
                <n-switch v-model:value="configForms.login.enableCaptcha" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:passkey-outline" />
                  验证码类型
                </div>
                <DictSelect v-model:value="configForms.login.captchaType" dict-type="captcha_type" class="config-select" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:remember-me" />
                  启用记住我
                </div>
                <n-switch v-model:value="configForms.login.enableRememberMe" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:schedule-outline" />
                  记住我有效天数
                </div>
                <n-input-number v-model:value="configForms.login.rememberMeDays" :min="1" :max="365" class="config-input" />
              </div>
            </div>
            <div class="section-footer">
              <NButton type="primary" :loading="saving.login" @click="saveConfig('login')">
                <i class="i-material-symbols:save mr-2" />
                保存登录配置
              </NButton>
            </div>
          </div>
        </n-tab-pane>

        <!-- 水印配置 -->
        <n-tab-pane name="watermark">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:watermark tab-icon" />
              <span>水印配置</span>
              <span class="tab-status" :class="[configForms.watermark.enable ? 'active' : 'inactive']">
                {{ configForms.watermark.enable ? '启用' : '禁用' }}
              </span>
            </div>
          </template>
          <div class="config-section">
            <div class="section-header">
              <div class="section-title">
                <i class="i-material-symbols:watermark section-icon" />
                水印配置
              </div>
              <div class="section-desc">
                配置系统水印显示效果，支持透明度、字体、旋转等参数
              </div>
            </div>
            <div class="config-grid">
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:toggle-on-outline" />
                  启用水印
                </div>
                <n-switch v-model:value="configForms.watermark.enable" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:text-fields-outline" />
                  水印内容
                </div>
                <DictSelect v-model:value="configForms.watermark.content" dict-type="water_marker_content" class="config-select" />
              </div>
              <div class="config-item full-width">
                <div class="config-label">
                  <i class="i-material-symbols:opacity-outline" />
                  水印透明度
                </div>
                <div class="config-slider-wrapper">
                  <n-slider v-model:value="configForms.watermark.opacity" :min="0.1" :max="1" :step="0.1" />
                  <span class="slider-value">{{ Math.round(configForms.watermark.opacity * 100) }}%</span>
                </div>
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:format-size-outline" />
                  字体大小
                </div>
                <n-input-number v-model:value="configForms.watermark.fontSize" :min="10" :max="50" class="config-input" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:palette-outline" />
                  字体颜色
                </div>
                <n-color-picker v-model:value="configForms.watermark.fontColor" :modes="['hex']" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:rotate-right-outline" />
                  旋转角度
                </div>
                <n-input-number v-model:value="configForms.watermark.rotate" :min="-180" :max="180" class="config-input" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:horizontal-rule-outline" />
                  X轴间距
                </div>
                <n-input-number v-model:value="configForms.watermark.gapX" :min="0" :max="200" class="config-input" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:vertical-rule-outline" />
                  Y轴间距
                </div>
                <n-input-number v-model:value="configForms.watermark.gapY" :min="0" :max="200" class="config-input" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:schedule-outline" />
                  显示时间戳
                </div>
                <n-switch v-model:value="configForms.watermark.showTimestamp" />
              </div>
              <div v-if="configForms.watermark.showTimestamp" class="config-item full-width">
                <div class="config-label">
                  <i class="i-material-symbols:date-range-outline" />
                  时间戳格式
                </div>
                <n-input v-model:value="configForms.watermark.timestampFormat" placeholder="yyyy-MM-dd HH:mm:ss" class="config-input-full" />
              </div>
            </div>
            <div class="section-footer">
              <NButton type="primary" :loading="saving.watermark" @click="saveConfig('watermark')">
                <i class="i-material-symbols:save mr-2" />
                保存水印配置
              </NButton>
            </div>
          </div>
        </n-tab-pane>

        <!-- 安全配置 -->
        <n-tab-pane name="security">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:security tab-icon" />
              <span>安全配置</span>
            </div>
          </template>
          <div class="config-section">
            <div class="section-header">
              <div class="section-title">
                <i class="i-material-symbols:security section-icon" />
                安全配置
              </div>
              <div class="section-desc">
                Sa-Token 会话管理与密码策略配置
              </div>
            </div>

            <!-- Sa-Token 配置 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:token-outline" />
                Sa-Token 配置
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:timer-outline" />
                    Token超时时间(秒)
                  </div>
                  <n-input-number v-model:value="configForms.security.saToken.timeout" :min="1" :max="2592000" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:timer-off-outline" />
                    活动超时时间(秒)
                  </div>
                  <n-input-number v-model:value="configForms.security.saToken.activityTimeout" :min="-1" :max="2592000" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:group-outline" />
                    允许并发登录
                  </div>
                  <n-switch v-model:value="configForms.security.saToken.isConcurrent" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:share-outline" />
                    共享Token
                  </div>
                  <n-switch v-model:value="configForms.security.saToken.isShare" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:body-outline" />
                    读取请求体Token
                  </div>
                  <n-switch v-model:value="configForms.security.saToken.isReadBody" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:header-outline" />
                    读取Header Token
                  </div>
                  <n-switch v-model:value="configForms.security.saToken.isReadHeader" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:cookie-outline" />
                    读取Cookie Token
                  </div>
                  <n-switch v-model:value="configForms.security.saToken.isReadCookie" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:tag-outline" />
                    Token前缀
                  </div>
                  <n-input v-model:value="configForms.security.saToken.tokenPrefix" class="config-input-full" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:label-outline" />
                    Token名称
                  </div>
                  <n-input v-model:value="configForms.security.saToken.tokenName" class="config-input-full" />
                </div>
              </div>
            </div>

            <!-- 密码策略 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:password-outline" />
                密码策略
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:straighten-outline" />
                    最小密码长度
                  </div>
                  <n-input-number v-model:value="configForms.security.passwordPolicy.minLength" :min="6" :max="30" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:uppercase-outline" />
                    需包含大写字母
                  </div>
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireUppercase" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:lowercase-outline" />
                    需包含小写字母
                  </div>
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireLowercase" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:123-outline" />
                    需包含数字
                  </div>
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireNumbers" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:symbol-outline" />
                    需包含特殊字符
                  </div>
                  <n-switch v-model:value="configForms.security.passwordPolicy.requireSpecialChars" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:calendar-month-outline" />
                    密码过期天数
                  </div>
                  <n-input-number v-model:value="configForms.security.passwordPolicy.expireDays" :min="1" :max="365" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:history-outline" />
                    密码历史记录
                  </div>
                  <n-input-number v-model:value="configForms.security.passwordPolicy.historyCount" :min="1" :max="20" class="config-input" />
                </div>
              </div>
            </div>

            <div class="section-footer">
              <NButton type="primary" :loading="saving.security" @click="saveConfig('security')">
                <i class="i-material-symbols:save mr-2" />
                保存安全配置
              </NButton>
            </div>
          </div>
        </n-tab-pane>

        <!-- 加解密配置 -->
        <n-tab-pane name="crypto">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:lock tab-icon" />
              <span>加解密配置</span>
              <span class="tab-status" :class="[configForms.crypto.enabled ? 'active' : 'inactive']">
                {{ configForms.crypto.enabled ? '启用' : '禁用' }}
              </span>
            </div>
          </template>
          <div class="config-section">
            <div class="section-header">
              <div class="section-title">
                <i class="i-material-symbols:lock section-icon" />
                加解密配置
              </div>
              <div class="section-desc">
                API 加解密、动态密钥协商与防重放攻击配置
              </div>
            </div>

            <!-- 基础配置 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:encrypted-outline" />
                加密基础配置
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:toggle-on-outline" />
                    启用加密
                  </div>
                  <n-switch v-model:value="configForms.crypto.enabled" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:functions-outline" />
                    加密算法
                  </div>
                  <n-select v-model:value="configForms.crypto.algorithm" :options="[{ label: 'SM4', value: 'SM4' }, { label: 'AES', value: 'AES' }]" class="config-select" />
                </div>
                <div class="config-item full-width">
                  <div class="config-label">
                    <i class="i-material-symbols:key-outline" />
                    对称加密密钥
                  </div>
                  <n-input v-model:value="configForms.crypto.secretKey" type="password" show-password-on="click" placeholder="Base64编码" class="config-input-full" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:dynamic-form-outline" />
                    动态密钥协商
                  </div>
                  <n-switch v-model:value="configForms.crypto.enableDynamicKey" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:timer-outline" />
                    会话密钥过期(秒)
                  </div>
                  <n-input-number v-model:value="configForms.crypto.sessionKeyExpire" :min="60" :max="86400" class="config-input" />
                </div>
              </div>
            </div>

            <!-- RSA 密钥 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:vpn-key-outline" />
                RSA 密钥配置
              </div>
              <div class="config-grid">
                <div class="config-item full-width">
                  <div class="config-label">
                    <i class="i-material-symbols:public-outline" />
                    RSA公钥
                  </div>
                  <n-input v-model:value="configForms.crypto.rsaPublicKey" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" placeholder="Base64编码" class="config-input-full" />
                </div>
                <div class="config-item full-width">
                  <div class="config-label">
                    <i class="i-material-symbols:private-outline" />
                    RSA私钥
                  </div>
                  <n-input v-model:value="configForms.crypto.rsaPrivateKey" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" placeholder="Base64编码" class="config-input-full" />
                </div>
              </div>
            </div>

            <!-- API 加密 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:api-outline" />
                API 加密配置
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:api-outline" />
                    启用API级加密
                  </div>
                  <n-switch v-model:value="configForms.crypto.enableApiCrypto" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:table-outline" />
                    启用字段级加密
                  </div>
                  <n-switch v-model:value="configForms.crypto.enableFieldCrypto" />
                </div>
                <div class="config-item full-width">
                  <div class="config-label">
                    <i class="i-material-symbols:block-outline" />
                    API加密排除路径
                  </div>
                  <n-dynamic-tags v-model:value="configForms.crypto.excludePaths" input-placeholder="输入排除路径" />
                </div>
              </div>
            </div>

            <!-- 防重放 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:shield-outline" />
                防重放攻击配置
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:shield-check-outline" />
                    启用防重放
                  </div>
                  <n-switch v-model:value="configForms.crypto.enableReplayProtection" />
                </div>
                <div v-if="configForms.crypto.enableReplayProtection" class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:timer-outline" />
                    防重放时间窗口(秒)
                  </div>
                  <n-input-number v-model:value="configForms.crypto.replayTimeWindow" :min="60" :max="3600" class="config-input" />
                </div>
                <div class="config-item full-width">
                  <div class="config-label">
                    <i class="i-material-symbols:block-outline" />
                    防重放排除路径
                  </div>
                  <n-dynamic-tags v-model:value="configForms.crypto.replayExcludePaths" input-placeholder="输入排除路径" />
                </div>
              </div>
            </div>

            <div class="section-footer">
              <NButton type="primary" :loading="saving.crypto" @click="saveConfig('crypto')">
                <i class="i-material-symbols:save mr-2" />
                保存加解密配置
              </NButton>
            </div>
          </div>
        </n-tab-pane>

        <!-- 认证配置 -->
        <n-tab-pane name="auth">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:key tab-icon" />
              <span>认证配置</span>
            </div>
          </template>
          <div class="config-section">
            <div class="section-header">
              <div class="section-title">
                <i class="i-material-symbols:key section-icon" />
                认证配置
              </div>
              <div class="section-desc">
                API权限校验、登录锁定与在线用户管理
              </div>
            </div>
            <div class="config-grid">
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:api-outline" />
                  API权限校验
                </div>
                <n-switch v-model:value="configForms.auth.enableApiPermission" />
              </div>
              <div class="config-item full-width">
                <div class="config-label">
                  <i class="i-material-symbols:block-outline" />
                  API权限排除路径
                </div>
                <n-dynamic-tags v-model:value="configForms.auth.apiPermissionExcludePaths" input-placeholder="输入排除路径" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:lock-outline" />
                  启用登录锁定
                </div>
                <n-switch v-model:value="configForms.auth.enableLoginLock" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:exclamation-outline" />
                  最大登录失败次数
                </div>
                <n-input-number v-model:value="configForms.auth.maxLoginAttempts" :min="1" :max="10" class="config-input" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:timer-outline" />
                  账号锁定时长(分钟)
                </div>
                <n-input-number v-model:value="configForms.auth.lockDuration" :min="1" :max="1440" class="config-input" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:history-outline" />
                  失败记录保留(分钟)
                </div>
                <n-input-number v-model:value="configForms.auth.failRecordExpire" :min="1" :max="1440" class="config-input" />
              </div>
              <div class="config-item full-width">
                <div class="config-label">
                  <i class="i-material-symbols:person-outline" />
                  同账号登录策略
                </div>
                <n-select v-model:value="configForms.auth.sameAccountLoginStrategy" :options="[{ label: '允许并发登录', value: 'allow_concurrent' }, { label: '新登录踢出旧登录', value: 'replace_old' }, { label: '拒绝新登录', value: 'reject_new' }]" class="config-select-full" />
              </div>
              <div class="config-item">
                <div class="config-label">
                  <i class="i-material-symbols:group-outline" />
                  在线用户管理
                </div>
                <n-switch v-model:value="configForms.auth.enableOnlineUserManagement" />
              </div>
            </div>
            <div class="section-footer">
              <NButton type="primary" :loading="saving.auth" @click="saveConfig('auth')">
                <i class="i-material-symbols:save mr-2" />
                保存认证配置
              </NButton>
            </div>
          </div>
        </n-tab-pane>

        <!-- 日志配置 -->
        <n-tab-pane name="log">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:logs tab-icon" />
              <span>日志配置</span>
            </div>
          </template>
          <div class="config-section">
            <div class="section-header">
              <div class="section-title">
                <i class="i-material-symbols:logs section-icon" />
                日志配置
              </div>
              <div class="section-desc">
                操作日志、登录日志与异步线程池配置
              </div>
            </div>

            <!-- 日志开关 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:toggle-on-outline" />
                日志开关
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:article-outline" />
                    启用操作日志
                  </div>
                  <n-switch v-model:value="configForms.log.enableOperationLog" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:login-outline" />
                    启用登录日志
                  </div>
                  <n-switch v-model:value="configForms.log.enableLoginLog" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:print-outline" />
                    打印操作日志
                  </div>
                  <n-switch v-model:value="configForms.log.printOperationLog" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:print-outline" />
                    打印登录日志
                  </div>
                  <n-switch v-model:value="configForms.log.printLoginLog" />
                </div>
              </div>
            </div>

            <!-- 日志参数 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:settings-outline" />
                日志参数
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:straighten-outline" />
                    请求参数最大长度
                  </div>
                  <n-input-number v-model:value="configForms.log.requestParamsMaxLength" :min="100" :max="10000" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:straighten-outline" />
                    响应结果最大长度
                  </div>
                  <n-input-number v-model:value="configForms.log.responseResultMaxLength" :min="100" :max="10000" class="config-input" />
                </div>
                <div class="config-item full-width">
                  <div class="config-label">
                    <i class="i-material-symbols:block-outline" />
                    排除URL路径
                  </div>
                  <n-dynamic-tags v-model:value="configForms.log.excludePaths" input-placeholder="输入排除路径" />
                </div>
              </div>
            </div>

            <!-- 线程池 -->
            <div class="config-group">
              <div class="group-title">
                <i class="i-material-symbols:thread-outline" />
                异步线程池
              </div>
              <div class="config-grid">
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:grid-view-outline" />
                    核心线程数
                  </div>
                  <n-input-number v-model:value="configForms.log.threadPoolCoreSize" :min="1" :max="20" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:expand-outline" />
                    最大线程数
                  </div>
                  <n-input-number v-model:value="configForms.log.threadPoolMaxSize" :min="1" :max="50" class="config-input" />
                </div>
                <div class="config-item">
                  <div class="config-label">
                    <i class="i-material-symbols:storage-outline" />
                    队列容量
                  </div>
                  <n-input-number v-model:value="configForms.log.threadPoolQueueCapacity" :min="100" :max="2000" class="config-input" />
                </div>
              </div>
            </div>

            <div class="section-footer">
              <NButton type="primary" :loading="saving.log" @click="saveConfig('log')">
                <i class="i-material-symbols:save mr-2" />
                保存日志配置
              </NButton>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  getAuthConfig,
  getConfigByGroup,
  getCryptoConfig,
  getLogConfig,
  getLoginConfig,
  getSecurityConfig,
  getWatermarkConfig,
  refreshConfig,
  updateAuthConfig,
  updateConfigByGroup,
  updateCryptoConfig,
  updateLogConfig,
  updateLoginConfig,
  updateSecurityConfig,
  updateWatermarkConfig,
} from '@/api/config'
import { DictSelect } from '@/components'

const activeTab = ref('login')
const saveAllLoading = ref(false)

const saving = reactive({
  login: false,
  watermark: false,
  security: false,
  crypto: false,
  auth: false,
  log: false,
})

const configForms = ref({
  login: {
    enableCaptcha: true,
    captchaType: 'char',
    enableRememberMe: true,
    rememberMeDays: 30,
  },
  watermark: {
    enable: true,
    content: '系统水印',
    opacity: 0.5,
    fontSize: 16,
    fontColor: '#ffffff',
    rotate: -20,
    gapX: 200,
    gapY: 100,
    showTimestamp: false,
    timestampFormat: 'yyyy-MM-dd HH:mm:ss',
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
      tokenName: 'Authorization',
    },
    passwordPolicy: {
      minLength: 8,
      requireUppercase: true,
      requireLowercase: true,
      requireNumbers: true,
      requireSpecialChars: false,
      expireDays: 90,
      historyCount: 5,
    },
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
    excludePaths: [],
    replayExcludePaths: [],
  },
  auth: {
    enableApiPermission: true,
    apiPermissionExcludePaths: ['/auth/**'],
    enableLoginLock: true,
    maxLoginAttempts: 4,
    lockDuration: 30,
    failRecordExpire: 15,
    sameAccountLoginStrategy: 'replace_old',
    enableOnlineUserManagement: true,
  },
  log: {
    enableOperationLog: true,
    enableLoginLog: true,
    requestParamsMaxLength: 2000,
    responseResultMaxLength: 2000,
    excludePaths: ['/auth/captcha', '/actuator/**'],
    printOperationLog: true,
    printLoginLog: true,
    threadPoolCoreSize: 2,
    threadPoolMaxSize: 5,
    threadPoolQueueCapacity: 500,
  },
})

async function getConfig(groupCode) {
  try {
    let res
    switch (groupCode) {
      case 'login':
        res = await getLoginConfig()
        break
      case 'watermark':
        res = await getWatermarkConfig()
        break
      case 'security':
        res = await getSecurityConfig()
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
  }
  catch {
    console.error(`获取${groupCode}配置失败`)
  }
}

async function saveConfig(groupCode) {
  saving[groupCode] = true
  try {
    const configData = configForms.value[groupCode]
    let res
    switch (groupCode) {
      case 'login':
        res = await updateLoginConfig(configData)
        break
      case 'watermark':
        res = await updateWatermarkConfig(configData)
        break
      case 'security':
        res = await updateSecurityConfig(configData)
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
    }
    else {
      window.$message.error(`${getGroupName(groupCode)}配置保存失败`)
    }
  }
  catch {
    window.$message.error(`${getGroupName(groupCode)}配置保存失败`)
  }
  finally {
    saving[groupCode] = false
  }
}

async function saveAllConfig() {
  saveAllLoading.value = true
  const groups = ['login', 'watermark', 'security', 'crypto', 'auth', 'log']
  for (const group of groups) {
    await saveConfig(group)
  }
  saveAllLoading.value = false
}

async function handleRefreshConfig() {
  try {
    const res = await refreshConfig()
    if (res.code === 200) {
      window.$message.success('配置刷新成功')
      for (const group of ['login', 'watermark', 'security', 'crypto', 'auth', 'log']) {
        await getConfig(group)
      }
    }
    else {
      window.$message.error('配置刷新失败')
    }
  }
  catch {
    window.$message.error('配置刷新失败')
  }
}

function getGroupName(groupCode) {
  const names = { login: '登录', watermark: '水印', security: '安全', crypto: '加解密', auth: '认证', log: '日志' }
  return names[groupCode] || groupCode
}

onMounted(async () => {
  for (const group of ['login', 'watermark', 'security', 'crypto', 'auth', 'log']) {
    await getConfig(group)
  }
})
</script>

<style scoped>
.config-center-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-desc {
  font-size: 12px;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  overflow-y: auto;
}

.tab-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-icon {
  font-size: 16px;
  color: #64748b;
}

.tab-status {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.tab-status.active {
  background: #dcfce7;
  color: #15803d;
}

.tab-status.inactive {
  background: #fee2e2;
  color: #b91c1c;
}

.config-section {
  padding: 20px 0;
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.section-icon {
  font-size: 20px;
  color: #4f46e5;
}

.section-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.config-group {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}

.config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.config-item:hover {
  border-color: #cbd5e1;
}

.config-item.full-width {
  grid-column: 1 / -1;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.config-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.config-input {
  width: 160px;
}

.config-input-full {
  width: 100%;
}

.config-select {
  width: 160px;
}

.config-select-full {
  width: 100%;
}

.config-slider-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.config-slider-wrapper :deep(.n-slider) {
  flex: 1;
}

.slider-value {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  min-width: 40px;
}

.section-footer {
  display: flex;
  justify-content: end;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  margin-top: 16px;
}
</style>
