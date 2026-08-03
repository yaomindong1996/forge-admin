<template>
  <section class="online-test-panel">
    <div class="panel-heading">
      <div>
        <h3>在线测试</h3>
        <p>使用所选客户端的真实认证方式调用开放网关，结果可下载后交给外围系统联调。</p>
      </div>
      <n-space>
        <n-button size="small" :disabled="!guide" @click="downloadIntegrationExample">
          <template #icon>
            <i class="i-material-symbols:download-rounded" />
          </template>
          下载接入示例
        </n-button>
        <n-button size="small" :disabled="!testReport" @click="downloadTestReport">
          <template #icon>
            <i class="i-material-symbols:receipt-long-outline-rounded" />
          </template>
          下载测试报文
        </n-button>
      </n-space>
    </div>

    <n-alert type="warning" :show-icon="true" class="security-alert">
      Secret、签名密钥和用户断言私钥只在本弹窗内存中使用，关闭或切换客户端后立即清空；下载内容会自动脱敏。
    </n-alert>

    <n-alert
      v-if="guide?.requestNotes?.length"
      type="info"
      :show-icon="true"
      class="request-note-alert"
    >
      <template #header>
        填写请求前先确认
      </template>
      <ul class="request-note-list">
        <li v-for="note in guide.requestNotes" :key="note">
          {{ note }}
        </li>
      </ul>
    </n-alert>

    <div class="test-form-grid">
      <div class="test-field">
        <span class="field-label">认证方式</span>
        <n-select
          v-model:value="authMode"
          :options="authOptions"
          placeholder="请选择认证方式"
          :disabled="!guide?.ready"
        />
      </div>
      <div class="test-field">
        <span class="field-label">{{ credentialLabel }}</span>
        <n-input
          v-model:value="credential"
          type="password"
          show-password-on="click"
          :placeholder="credentialPlaceholder"
          :disabled="!authMode"
          autocomplete="new-password"
        />
      </div>
      <div v-if="requiresSubjectToken" class="test-field test-field-wide">
        <span class="field-label">真实用户身份来源</span>
        <n-radio-group v-model:value="subjectTokenMode" type="button" size="small">
          <n-radio-button value="OIDC">
            受信 OIDC JWT
          </n-radio-button>
          <n-radio-button v-if="guide?.userAssertionEnabled" value="USER_ASSERTION">
            客户端签名用户断言
          </n-radio-button>
        </n-radio-group>
      </div>
      <div v-if="requiresSubjectToken && subjectTokenMode === 'OIDC'" class="test-field test-field-wide">
        <span class="field-label">受信 OIDC subject_token</span>
        <n-input
          v-model:value="subjectToken"
          type="password"
          show-password-on="click"
          placeholder="粘贴外围用户的受信 OIDC JWT，仅本次测试使用"
          autocomplete="off"
        />
      </div>
      <template v-if="requiresSubjectToken && subjectTokenMode === 'USER_ASSERTION'">
        <div class="test-field">
          <span class="field-label">外围用户标识（JWT sub）</span>
          <n-input
            v-model:value="userAssertionSubject"
            maxlength="512"
            placeholder="必须已在客户端页面预绑定"
            autocomplete="off"
          />
        </div>
        <div class="test-field">
          <span class="field-label">Forge 组织 ID（可选）</span>
          <n-input
            v-model:value="userAssertionOrgId"
            placeholder="不填则使用该用户默认组织"
            autocomplete="off"
          />
        </div>
        <div class="test-field test-field-wide">
          <span class="field-label">用户断言私钥（PKCS#8 PEM，仅本次测试）</span>
          <n-input
            v-model:value="userAssertionPrivateKey"
            type="textarea"
            :autosize="{ minRows: 6, maxRows: 10 }"
            placeholder="粘贴生成密钥时一次性保存的 -----BEGIN PRIVATE KEY----- PEM"
            autocomplete="off"
            class="private-key-input"
          />
          <div class="assertion-protocol-hint">
            kid {{ guide.userAssertionKeyId }} · iss {{ guide.userAssertionIssuer }} · aud {{ guide.userAssertionAudience }}
          </div>
        </div>
      </template>
      <div class="test-field test-field-wide">
        <div class="body-label-line">
          <span class="field-label">请求 Body（JSON 对象）</span>
          <n-button text type="primary" size="tiny" @click="resetBody">
            恢复示例
          </n-button>
        </div>
        <n-input
          v-model:value="requestBody"
          type="textarea"
          :autosize="{ minRows: 7, maxRows: 16 }"
          placeholder="请输入合法的 JSON 对象"
          class="json-input"
        />
      </div>
    </div>

    <div class="test-actions">
      <n-alert v-if="!guide?.ready" type="error" :show-icon="true">
        当前存在调用阻断，请先处理上方“调用前检查”中的红色项目。
      </n-alert>
      <n-button
        type="primary"
        :loading="testing"
        :disabled="!guide?.ready || !authMode"
        @click="handleTest"
      >
        <template #icon>
          <i class="i-material-symbols:play-arrow-rounded" />
        </template>
        发起真实调用
      </n-button>
    </div>

    <div v-if="testReport" class="test-result">
      <div class="result-summary">
        <div>
          <strong>测试结果</strong>
          <span>{{ testReport.startedAt }} · {{ testReport.durationMs }} ms</span>
        </div>
        <n-tag :type="testReport.success ? 'success' : 'error'" round>
          {{ testReport.success ? '调用成功' : '调用失败' }}
        </n-tag>
      </div>
      <n-alert v-if="testReport.error" type="error" :show-icon="true" class="result-error">
        {{ testReport.error }}
      </n-alert>
      <n-tabs type="line" animated>
        <n-tab-pane v-if="testReport.tokenExchange" name="token" tab="Token 报文">
          <pre class="report-panel"><code>{{ exchangeText(testReport.tokenExchange) }}</code></pre>
        </n-tab-pane>
        <n-tab-pane name="invoke" tab="能力调用报文">
          <pre class="report-panel"><code>{{ exchangeText(testReport.invocation) }}</code></pre>
        </n-tab-pane>
      </n-tabs>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  guide: {
    type: Object,
    default: null,
  },
})

const authMode = ref(null)
const credential = ref('')
const subjectTokenMode = ref('OIDC')
const subjectToken = ref('')
const userAssertionSubject = ref('')
const userAssertionOrgId = ref('')
const userAssertionPrivateKey = ref('')
const requestBody = ref('{}')
const testing = ref(false)
const testReport = ref(null)

const authOptions = computed(() => (props.guide?.availableAuthModes || []).map(mode => ({
  label: mode === 'OAUTH' ? 'OAuth 2.1' : 'AppId + HMAC-SHA256',
  value: mode,
})))

const requiresSubjectToken = computed(() => (
  authMode.value === 'OAUTH' && props.guide?.tokenExchangeRequired
))

const credentialLabel = computed(() => (
  authMode.value === 'HMAC' ? '签名密钥 Signing Key' : 'OAuth Client Secret'
))

const credentialPlaceholder = computed(() => (
  authMode.value === 'HMAC'
    ? '粘贴创建或轮换客户端时保存的签名密钥'
    : '粘贴创建或轮换客户端时保存的 Client Secret'
))

watch(() => props.guide, (guide) => {
  authMode.value = guide?.availableAuthModes?.[0] || null
  credential.value = ''
  subjectTokenMode.value = guide?.userAssertionEnabled ? 'USER_ASSERTION' : 'OIDC'
  subjectToken.value = ''
  userAssertionSubject.value = ''
  userAssertionOrgId.value = ''
  userAssertionPrivateKey.value = ''
  testReport.value = null
  resetBody()
}, { immediate: true })

watch(authMode, () => {
  credential.value = ''
  subjectTokenMode.value = props.guide?.userAssertionEnabled ? 'USER_ASSERTION' : 'OIDC'
  subjectToken.value = ''
  userAssertionSubject.value = ''
  userAssertionOrgId.value = ''
  userAssertionPrivateKey.value = ''
  testReport.value = null
})

watch(subjectTokenMode, () => {
  subjectToken.value = ''
  userAssertionSubject.value = ''
  userAssertionOrgId.value = ''
  userAssertionPrivateKey.value = ''
  testReport.value = null
})

function resetBody() {
  requestBody.value = JSON.stringify(props.guide?.requestExample || {}, null, 2)
}

function handleTest() {
  if (!validateTestInput())
    return
  if (props.guide.behavior === 'READ_ONLY') {
    executeTest()
    return
  }
  window.$dialog.warning({
    title: '确认执行有副作用的能力',
    content: '该能力可能启动流程、修改业务数据或触发外部动作。本次测试会真实执行，并自动携带一次性 Idempotency-Key。是否继续？',
    positiveText: '确认执行',
    negativeText: '取消',
    onPositiveClick: executeTest,
  })
}

function validateTestInput() {
  if (!props.guide?.ready) {
    window.$message.error('当前调用条件未就绪，请先处理阻断项')
    return false
  }
  if (!credential.value.trim()) {
    window.$message.error(`请输入${credentialLabel.value}`)
    return false
  }
  if (requiresSubjectToken.value) {
    if (subjectTokenMode.value === 'OIDC' && !subjectToken.value.trim()) {
      window.$message.error('请提供受信 OIDC subject_token')
      return false
    }
    if (subjectTokenMode.value === 'USER_ASSERTION') {
      if (!props.guide?.userAssertionEnabled || !props.guide?.userAssertionKeyId) {
        window.$message.error('当前客户端尚未启用用户断言密钥')
        return false
      }
      if (!userAssertionSubject.value.trim()) {
        window.$message.error('请输入已预绑定的外围用户标识')
        return false
      }
      if (!userAssertionPrivateKey.value.includes('-----BEGIN PRIVATE KEY-----')) {
        window.$message.error('请粘贴有效的 PKCS#8 PEM 私钥')
        return false
      }
      if (userAssertionOrgId.value.trim() && !/^[1-9]\d*$/.test(userAssertionOrgId.value.trim())) {
        window.$message.error('Forge 组织 ID 必须是正整数')
        return false
      }
    }
  }
  try {
    const payload = JSON.parse(requestBody.value)
    if (!payload || Array.isArray(payload) || typeof payload !== 'object')
      throw new Error('请求 Body 必须是 JSON 对象')
    if (props.guide?.sourceType === 'FLOW_ACTION')
      validateFlowActionPayload(payload)
  }
  catch (error) {
    window.$message.error(error?.message || '请求 Body 不是合法 JSON')
    return false
  }
  return true
}

function validateFlowActionPayload(payload) {
  if (props.guide?.actionCode === 'SUBMIT') {
    const data = payload.data
    if (!data || Array.isArray(data) || typeof data !== 'object')
      throw new Error('SUBMIT 的 data 必须是包含申请字段的 JSON 对象')
    if ('recordId' in payload)
      throw new Error('SUBMIT 会自动创建业务记录，请不要传 recordId')
    return
  }
  if (typeof payload.recordId !== 'string' || !/^[1-9]\d{0,18}$/.test(payload.recordId.trim())) {
    throw new Error('recordId 必须替换为已经保存、且当前委托用户可见的真实记录 ID')
  }
  if (props.guide?.actionCode === 'START') {
    const argumentsValue = payload.arguments
    if (!argumentsValue || Array.isArray(argumentsValue) || typeof argumentsValue !== 'object')
      throw new Error('arguments 必须是 JSON 对象')
    if (Object.keys(argumentsValue).length)
      throw new Error('START 的 arguments 必须保持为空对象 {}')
  }
}

async function executeTest() {
  testing.value = true
  testReport.value = null
  const startedAt = new Date()
  try {
    const report = authMode.value === 'HMAC'
      ? await executeHmac()
      : await executeOAuth()
    testReport.value = {
      ...report,
      authMode: authMode.value,
      userIdentityMode: requiresSubjectToken.value ? subjectTokenMode.value : null,
      capabilityCode: props.guide.capabilityCode,
      clientId: props.guide.clientId,
      startedAt: formatDate(startedAt),
      durationMs: Date.now() - startedAt.getTime(),
    }
    if (testReport.value.success)
      window.$message.success('能力调用成功，可以下载完整测试报文')
    else
      window.$message.error(testReport.value.error || '能力调用失败，请查看返回报文和 requestId')
  }
  catch (error) {
    testReport.value = {
      success: false,
      error: error?.message || '网络请求失败',
      authMode: authMode.value,
      userIdentityMode: requiresSubjectToken.value ? subjectTokenMode.value : null,
      capabilityCode: props.guide.capabilityCode,
      clientId: props.guide.clientId,
      startedAt: formatDate(startedAt),
      durationMs: Date.now() - startedAt.getTime(),
      tokenExchange: null,
      invocation: null,
    }
    window.$message.error(testReport.value.error)
  }
  finally {
    testing.value = false
  }
}

async function executeOAuth() {
  const params = new URLSearchParams()
  params.set('grant_type', props.guide.tokenExchangeRequired
    ? 'urn:ietf:params:oauth:grant-type:token-exchange'
    : 'client_credentials')
  if (props.guide.tokenExchangeRequired) {
    const clientAssertion = subjectTokenMode.value === 'USER_ASSERTION'
    params.set('subject_token', clientAssertion
      ? await createUserAssertionJwt()
      : subjectToken.value.trim())
    params.set('subject_token_type', clientAssertion
      ? props.guide.userAssertionSubjectTokenType
      : 'urn:ietf:params:oauth:token-type:jwt')
    params.set('requested_token_type', 'urn:ietf:params:oauth:token-type:access_token')
  }
  params.set('resource', props.guide.openapiResource)
  params.set('scope', `capability:invoke:${props.guide.capabilityCode}`)

  const tokenStartedAt = Date.now()
  const tokenResponse = await fetch(backendProxyUrl(props.guide.tokenUrl), {
    method: 'POST',
    credentials: 'omit',
    headers: {
      Authorization: `Basic ${basicCredentials(props.guide.clientId, credential.value)}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
  })
  const tokenText = await tokenResponse.text()
  const tokenPayload = parseBody(tokenText)
  const tokenExchange = exchangeReport(
    {
      method: 'POST',
      url: props.guide.tokenUrl,
      headers: {
        Authorization: 'Basic <REDACTED>',
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: redactTokenForm(params),
      rawBody: redactTokenFormString(params),
    },
    tokenResponse,
    redactSensitive(tokenPayload),
    Date.now() - tokenStartedAt,
  )
  const accessToken = tokenPayload?.access_token
  if (!tokenResponse.ok || !accessToken) {
    return {
      success: false,
      error: `获取访问令牌失败，HTTP ${tokenResponse.status}`,
      tokenExchange,
      invocation: null,
    }
  }

  const invocation = await invokeGateway({
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  }, {
    Authorization: 'Bearer <REDACTED>',
    'Content-Type': 'application/json',
  })
  const success = invocation.response.status >= 200 && invocation.response.status < 300
  return {
    success,
    error: success ? null : gatewayErrorMessage(invocation),
    tokenExchange,
    invocation,
  }
}

async function createUserAssertionJwt() {
  if (!globalThis.crypto?.subtle)
    throw new Error('当前浏览器环境不支持 RSA 签名，请使用 HTTPS 或 localhost')
  const issuedAt = Math.floor(Date.now() / 1000)
  const configuredTtl = Number(props.guide?.userAssertionMaxTtlSeconds || 120)
  const ttlSeconds = Math.min(120, Math.max(30, configuredTtl))
  const claims = {
    iss: props.guide.userAssertionIssuer,
    aud: props.guide.userAssertionAudience,
    client_id: String(props.guide.clientId),
    sub: userAssertionSubject.value.trim(),
    iat: issuedAt,
    exp: issuedAt + ttlSeconds,
    jti: globalThis.crypto.randomUUID?.() || fallbackNonce(),
  }
  if (userAssertionOrgId.value.trim())
    claims.forge_org_id = userAssertionOrgId.value.trim()
  const header = {
    alg: 'RS256',
    typ: 'JWT',
    kid: props.guide.userAssertionKeyId,
  }
  const signingInput = `${base64UrlText(JSON.stringify(header))}.${base64UrlText(JSON.stringify(claims))}`
  const privateKey = await globalThis.crypto.subtle.importKey(
    'pkcs8',
    pemPrivateKeyBytes(userAssertionPrivateKey.value),
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign'],
  )
  const signature = await globalThis.crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    privateKey,
    new TextEncoder().encode(signingInput),
  )
  return `${signingInput}.${base64UrlBytes(signature)}`
}

async function executeHmac() {
  if (!globalThis.crypto?.subtle)
    throw new Error('当前浏览器环境不支持 Web Crypto，请使用 HTTPS 或 localhost')
  const timestamp = String(Date.now())
  const nonce = globalThis.crypto.randomUUID?.() || fallbackNonce()
  const bodyHash = await sha256Hex(requestBody.value)
  const path = new URL(props.guide.invokeUrl).pathname
  const canonical = [
    String(props.guide.clientId), timestamp, nonce, 'POST', path, bodyHash,
  ].join('\n')
  const signature = await hmacSha256Hex(credential.value, canonical)
  const invocation = await invokeGateway({
    'X-Forge-App-Id': String(props.guide.clientId),
    'X-Forge-Timestamp': timestamp,
    'X-Forge-Nonce': nonce,
    'X-Forge-Signature': signature,
    'Content-Type': 'application/json',
  }, {
    'X-Forge-App-Id': String(props.guide.clientId),
    'X-Forge-Timestamp': timestamp,
    'X-Forge-Nonce': nonce,
    'X-Forge-Signature': '<REDACTED>',
    'Content-Type': 'application/json',
  })
  const success = invocation.response.status >= 200 && invocation.response.status < 300
  return {
    success,
    error: success ? null : gatewayErrorMessage(invocation),
    tokenExchange: null,
    invocation,
  }
}

async function invokeGateway(actualHeaders, reportHeaders) {
  const idempotencyKey = props.guide.behavior === 'READ_ONLY'
    ? null
    : (globalThis.crypto?.randomUUID?.() || fallbackNonce())
  const requestHeaders = { ...actualHeaders }
  const safeHeaders = { ...reportHeaders }
  if (idempotencyKey) {
    requestHeaders['Idempotency-Key'] = idempotencyKey
    safeHeaders['Idempotency-Key'] = idempotencyKey
  }
  const startedAt = Date.now()
  const response = await fetch(backendProxyUrl(props.guide.invokeUrl), {
    method: 'POST',
    credentials: 'omit',
    headers: requestHeaders,
    body: requestBody.value,
  })
  const responseText = await response.text()
  const report = exchangeReport({
    method: 'POST',
    url: props.guide.invokeUrl,
    headers: safeHeaders,
    body: JSON.parse(requestBody.value),
    rawBody: requestBody.value,
  }, response, redactSensitive(parseBody(responseText)), Date.now() - startedAt)
  report.success = response.ok
  return report
}

function exchangeReport(request, response, body, durationMs) {
  return {
    request,
    response: {
      status: response.status,
      statusText: response.statusText,
      headers: redactHeaders(response.headers),
      body,
      rawBody: typeof body === 'string' ? body : JSON.stringify(body),
    },
    durationMs,
  }
}

function gatewayErrorMessage(invocation) {
  const status = invocation?.response?.status
  const body = invocation?.response?.body
  const code = body && typeof body === 'object' ? body.code : null
  const message = body && typeof body === 'object' ? body.message : null
  if (message)
    return `${message}${code ? `（${code}）` : ''}`
  return `能力调用失败，HTTP ${status || '-'}`
}

function redactTokenForm(params) {
  const safe = {}
  for (const [key, value] of params.entries())
    safe[key] = key === 'subject_token' ? '<REDACTED>' : value
  return safe
}

function redactTokenFormString(params) {
  const safe = new URLSearchParams(params)
  if (safe.has('subject_token'))
    safe.set('subject_token', '<REDACTED>')
  return safe.toString()
}

function redactSensitive(value) {
  if (Array.isArray(value))
    return value.map(redactSensitive)
  if (!value || typeof value !== 'object')
    return value
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [
    key,
    /(authorization|token|secret|password|signing.?key|signature)/i.test(key)
      ? '<REDACTED>'
      : redactSensitive(item),
  ]))
}

function redactHeaders(headers) {
  const result = {}
  headers.forEach((value, key) => {
    result[key] = /(authorization|cookie|token|secret|signature)/i.test(key)
      ? '<REDACTED>'
      : value
  })
  return result
}

function parseBody(text) {
  if (!text)
    return null
  try {
    return JSON.parse(text)
  }
  catch {
    return text
  }
}

function backendProxyUrl(absoluteUrl) {
  const parsed = new URL(absoluteUrl, window.location.origin)
  const prefix = String(import.meta.env.VITE_REQUEST_PREFIX || '').replace(/\/$/, '')
  return `${prefix}${parsed.pathname}${parsed.search}`
}

function basicCredentials(clientId, secret) {
  const bytes = new TextEncoder().encode(`${clientId}:${secret}`)
  let binary = ''
  for (const byte of bytes)
    binary += String.fromCharCode(byte)
  return btoa(binary)
}

function pemPrivateKeyBytes(pem) {
  const normalized = pem
    .replace('-----BEGIN PRIVATE KEY-----', '')
    .replace('-----END PRIVATE KEY-----', '')
    .replace(/\s/g, '')
  let binary
  try {
    binary = atob(normalized)
  }
  catch {
    throw new Error('用户断言私钥不是有效的 PKCS#8 PEM')
  }
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1)
    bytes[index] = binary.charCodeAt(index)
  return bytes.buffer
}

function base64UrlText(value) {
  return base64UrlBytes(new TextEncoder().encode(value))
}

function base64UrlBytes(value) {
  const bytes = value instanceof Uint8Array ? value : new Uint8Array(value)
  let binary = ''
  for (const byte of bytes)
    binary += String.fromCharCode(byte)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function sha256Hex(value) {
  const digest = await globalThis.crypto.subtle.digest(
    'SHA-256', new TextEncoder().encode(value),
  )
  return bytesToHex(digest)
}

async function hmacSha256Hex(key, value) {
  const cryptoKey = await globalThis.crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(key),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  )
  const signature = await globalThis.crypto.subtle.sign(
    'HMAC', cryptoKey, new TextEncoder().encode(value),
  )
  return bytesToHex(signature)
}

function bytesToHex(buffer) {
  return [...new Uint8Array(buffer)]
    .map(value => value.toString(16).padStart(2, '0'))
    .join('')
}

function fallbackNonce() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function exchangeText(exchange) {
  return exchange ? JSON.stringify(exchange, null, 2) : '未发起请求'
}

function downloadTestReport() {
  if (!testReport.value)
    return
  downloadText(
    JSON.stringify({
      securityNotice: '本报文已自动脱敏，不包含 Client Secret、Signing Key、用户断言私钥或可用 Token。',
      ...testReport.value,
    }, null, 2),
    `${fileStem()}-test-report.json`,
    'application/json;charset=UTF-8',
  )
}

function downloadIntegrationExample() {
  if (!props.guide)
    return
  const guide = props.guide
  const sections = [
    `# ${guide.capabilityName} 外围系统接入示例`,
    '',
    '> 安全提示：示例和测试报文均已脱敏，请从密钥管理系统注入真实凭据。',
    '',
    '## 接口信息',
    '',
    `- 能力编码：\`${guide.capabilityCode}\``,
    `- 能力版本：\`${guide.version || '-'}\``,
    `- 调用地址：\`${guide.invokeUrl}\``,
    `- Token 地址：\`${guide.tokenUrl}\``,
    `- OAuth Resource：\`${guide.openapiResource}\``,
    `- 客户端 ID / AppId：\`${guide.clientId}\``,
    `- 用户身份方案：\`${guide.userAssertionEnabled ? '客户端 RS256 用户断言' : '受信 OIDC JWT'}\``,
    ...(guide.userAssertionEnabled ? [
      `- 用户断言 kid：\`${guide.userAssertionKeyId}\``,
      `- 用户断言 Issuer：\`${guide.userAssertionIssuer}\``,
      `- 用户断言 Audience：\`${guide.userAssertionAudience}\``,
      `- Subject Token Type：\`${guide.userAssertionSubjectTokenType}\``,
    ] : []),
    ...(guide.requestNotes?.length ? [
      '',
      '## 请求前提',
      '',
      ...guide.requestNotes.map(note => `- ${note}`),
    ] : []),
    '',
    '## 请求 Body',
    '',
    '```json',
    JSON.stringify(guide.requestExample || {}, null, 2),
    '```',
  ]
  appendCodeSection(sections, 'OAuth curl', 'bash', guide.oauthExample)
  appendCodeSection(sections, 'HMAC curl', 'bash', guide.hmacExample)
  appendCodeSection(sections, 'OAuth Java 17', 'java', guide.oauthJavaExample)
  appendCodeSection(sections, '客户端用户断言 Java 17', 'java', guide.userAssertionJavaExample)
  appendCodeSection(sections, 'HMAC Java 17', 'java', guide.hmacJavaExample)
  if (testReport.value)
    appendCodeSection(sections, '最近一次测试报文（已脱敏）', 'json', JSON.stringify(testReport.value, null, 2))
  downloadText(
    sections.join('\n'),
    `${fileStem()}-integration-example.md`,
    'text/markdown;charset=UTF-8',
  )
}

function appendCodeSection(sections, title, language, content) {
  if (!content)
    return
  sections.push('', `## ${title}`, '', `\`\`\`${language}`, content, '\`\`\`')
}

function downloadText(content, filename, type) {
  const url = URL.createObjectURL(new Blob([content], { type }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function fileStem() {
  return `${props.guide?.capabilityCode || 'capability'}-${props.guide?.version || 'latest'}`
}

function formatDate(date) {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'medium',
    hour12: false,
  }).format(date)
}
</script>

<style scoped>
.online-test-panel {
  padding: 22px 0 4px;
}

.panel-heading,
.result-summary,
.body-label-line,
.test-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-heading {
  align-items: flex-start;
  margin-bottom: 14px;
}

.panel-heading h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

.panel-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.security-alert {
  margin-bottom: 16px;
}

.request-note-alert {
  margin-bottom: 16px;
}

.request-note-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}

.test-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.test-field-wide {
  grid-column: 1 / -1;
}

.field-label {
  display: block;
  margin-bottom: 7px;
  color: var(--text-secondary);
  font-size: 12px;
}

.body-label-line .field-label {
  margin-bottom: 7px;
}

.json-input :deep(textarea),
.private-key-input :deep(textarea),
.report-panel {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
}

.assertion-protocol-hint {
  margin-top: 6px;
  color: var(--text-tertiary);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
  overflow-wrap: anywhere;
}

.test-actions {
  align-items: flex-start;
  margin-top: 16px;
}

.test-actions .n-alert {
  flex: 1;
}

.test-result {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.result-summary strong,
.result-summary span {
  display: block;
}

.result-summary span {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.result-error {
  margin-top: 12px;
}

.report-panel {
  max-height: 420px;
  overflow: auto;
  margin: 0;
  padding: 14px;
  border-radius: 6px;
  background: #111827;
  color: #e5e7eb;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 760px) {
  .panel-heading,
  .test-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .test-form-grid {
    grid-template-columns: 1fr;
  }

  .test-field-wide {
    grid-column: auto;
  }
}
</style>
