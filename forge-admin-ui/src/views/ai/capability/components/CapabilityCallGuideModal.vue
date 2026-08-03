<template>
  <n-modal
    :show="show"
    preset="card"
    class="capability-call-guide-modal"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <template #header>
      <div class="modal-heading">
        <span class="modal-title">调用与测试</span>
        <span v-if="capability" class="modal-subtitle">
          {{ capability.capabilityName }} · {{ capability.capabilityCode }}
        </span>
      </div>
    </template>

    <div class="guide-body">
      <div class="client-bar">
        <div class="client-field">
          <span class="field-label">选择调用客户端</span>
          <n-select
            v-model:value="selectedClientId"
            :options="clientOptions"
            :loading="clientsLoading"
            placeholder="选择一个客户端后生成可执行指南"
            filterable
            clearable
            @update:value="loadGuide"
          >
            <template #empty>
              <n-empty size="small" description="暂无机器客户端，请先创建客户端" />
            </template>
          </n-select>
        </div>
        <n-button
          quaternary
          circle
          :loading="guideLoading"
          :disabled="!selectedClientId"
          aria-label="刷新调用检查"
          @click="loadGuide(selectedClientId)"
        >
          <template #icon>
            <i class="i-material-symbols:refresh-rounded" />
          </template>
        </n-button>
        <n-tag v-if="guide" :type="guide.ready ? 'success' : 'error'" round>
          {{ guide.ready ? '已具备静态调用条件' : '存在调用阻断' }}
        </n-tag>
      </div>

      <n-alert v-if="clientsError" type="error" :show-icon="true">
        {{ clientsError }}
      </n-alert>
      <n-alert v-else-if="!clientsLoading && clients.length === 0" type="warning" :show-icon="true">
        还没有可用于诊断的客户端。请先在“机器客户端”页面创建客户端，再为它授权此能力。
      </n-alert>
      <n-spin v-else-if="guideLoading" class="guide-loading" description="正在核对网关、客户端和授权状态…" />

      <template v-else-if="guide">
        <n-alert
          v-if="guide.versionUpgradeAvailable"
          type="warning"
          :show-icon="true"
          class="version-alert"
        >
          <template #header>
            这个客户端仍在使用旧能力版本
          </template>
          <div class="version-alert-content">
            <span>
              能力当前是 v{{ guide.currentVersion }}，但客户端授权按“{{ grantVersionStrategyLabel }}”
              实际调用 v{{ guide.version || guide.grantFixedVersion || '-' }}。
              发布新版本不会自动修改已有授权。
            </span>
            <n-button
              v-if="canUpdateGrant && guide.grantId"
              type="warning"
              secondary
              :loading="versionSwitching"
              @click="confirmUseCurrentVersion"
            >
              切换到当前 v{{ guide.currentVersion }}
            </n-button>
            <span v-else class="version-permission-tip">
              请让具有能力授权维护权限的管理员切换授权版本。
            </span>
          </div>
        </n-alert>

        <section class="guide-section">
          <div class="section-heading">
            <div>
              <h3>调用地址与身份</h3>
              <p>以下内容由所选客户端的实际授权版本实时生成。</p>
            </div>
          </div>
          <div class="endpoint-grid">
            <div class="endpoint-item endpoint-wide">
              <span class="endpoint-label">调用地址</span>
              <div class="copy-line">
                <code>{{ guide.invokeUrl }}</code>
                <n-button text type="primary" @click="copyValue(guide.invokeUrl, '调用地址已复制')">
                  复制
                </n-button>
              </div>
            </div>
            <div class="endpoint-item endpoint-wide">
              <span class="endpoint-label">OAuth Resource</span>
              <div class="copy-line">
                <code>{{ guide.openapiResource }}</code>
                <n-button text type="primary" @click="copyValue(guide.openapiResource, 'Resource 已复制')">
                  复制
                </n-button>
              </div>
            </div>
            <div class="endpoint-item endpoint-wide">
              <span class="endpoint-label">Token 地址</span>
              <div class="copy-line">
                <code>{{ guide.tokenUrl }}</code>
                <n-button text type="primary" @click="copyValue(guide.tokenUrl, 'Token 地址已复制')">
                  复制
                </n-button>
              </div>
            </div>
            <div class="endpoint-item">
              <span class="endpoint-label">实际调用版本</span>
              <strong>{{ guide.version ? `v${guide.version}` : '授权版本尚未解析' }}</strong>
            </div>
            <div class="endpoint-item">
              <span class="endpoint-label">当前能力版本</span>
              <strong>{{ guide.currentVersion ? `v${guide.currentVersion}` : '-' }}</strong>
            </div>
            <div class="endpoint-item">
              <span class="endpoint-label">客户端授权策略</span>
              <strong>{{ grantVersionStrategyLabel }}</strong>
            </div>
            <div class="endpoint-item">
              <span class="endpoint-label">客户端</span>
              <strong>{{ guide.clientName }}（{{ guide.clientCode }}）</strong>
            </div>
            <div class="endpoint-item">
              <span class="endpoint-label">调用主体</span>
              <strong>{{ actorTypeLabel }}</strong>
            </div>
            <div class="endpoint-item">
              <span class="endpoint-label">可用认证</span>
              <strong>{{ authModeLabel }}</strong>
            </div>
            <template v-if="guide.userAssertionEnabled">
              <div class="endpoint-item">
                <span class="endpoint-label">客户端用户断言</span>
                <strong>RS256 · kid {{ guide.userAssertionKeyId }}</strong>
              </div>
              <div class="endpoint-item">
                <span class="endpoint-label">断言 Audience</span>
                <strong>{{ guide.userAssertionAudience }}</strong>
              </div>
              <div class="endpoint-item endpoint-wide">
                <span class="endpoint-label">用户断言 Subject Token Type</span>
                <div class="copy-line">
                  <code>{{ guide.userAssertionSubjectTokenType }}</code>
                  <n-button text type="primary" @click="copyValue(guide.userAssertionSubjectTokenType, 'Token Type 已复制')">
                    复制
                  </n-button>
                </div>
              </div>
            </template>
          </div>
        </section>

        <section class="guide-section readiness-section">
          <div class="section-heading">
            <div>
              <h3>调用前检查</h3>
              <p>红色项会直接阻断请求；“运行时”项由实际用户或服务账号在调用时校验。</p>
            </div>
          </div>
          <div class="check-list">
            <div v-for="check in guide.checks" :key="check.code" class="check-row">
              <div class="check-state" :class="`state-${check.status.toLowerCase()}`">
                <i :class="checkIcon(check.status)" />
              </div>
              <div class="check-content">
                <div class="check-title">
                  <strong>{{ check.label }}</strong>
                  <n-tag :type="checkTagType(check.status)" size="small" :bordered="false">
                    {{ checkStatusLabel(check.status) }}
                  </n-tag>
                </div>
                <p>{{ check.message }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="guide-section">
          <div class="section-heading example-heading">
            <div>
              <h3>可复制调用示例</h3>
              <p>命令不会包含真实密钥。请在安全环境中替换尖括号占位符。</p>
            </div>
            <n-button
              v-if="currentExample"
              size="small"
              @click="copyValue(currentExample, '调用命令已复制')"
            >
              <template #icon>
                <i class="i-material-symbols:content-copy-outline-rounded" />
              </template>
              复制当前示例
            </n-button>
          </div>
          <n-tabs v-model:value="activeExample" type="line" animated>
            <n-tab-pane v-if="guide.oauthExample" name="OAUTH" tab="OAuth 2.1">
              <n-alert
                v-if="guide.requiredActorType === 'USER'"
                type="info"
                class="example-alert"
              >
                {{ guide.userAssertionEnabled
                  ? '该客户端默认使用独立 RSA 私钥签发用户断言，再通过 Token Exchange 换取 Forge 短期令牌。'
                  : '该能力使用受信 OIDC/JWT Token Exchange。外围用户必须已映射为 Forge 用户，权限由该用户实时校验。' }}
              </n-alert>
              <pre class="code-panel"><code>{{ guide.oauthExample }}</code></pre>
            </n-tab-pane>
            <n-tab-pane v-if="guide.hmacExample" name="HMAC" tab="AppId + HMAC">
              <n-alert type="warning" class="example-alert">
                签名密钥仅在创建或轮换时展示一次，请从密钥管理系统注入，不要写入代码仓库。
              </n-alert>
              <pre class="code-panel"><code>{{ guide.hmacExample }}</code></pre>
            </n-tab-pane>
            <n-tab-pane v-if="guide.oauthJavaExample" name="OAUTH_JAVA" tab="OAuth Java 17">
              <n-alert type="info" class="example-alert">
                受信 OIDC 方案仅使用 Java 17 标准库。配置 FORGE_CLIENT_SECRET（委托模式另配 FORGE_SUBJECT_TOKEN）后即可运行。
              </n-alert>
              <pre class="code-panel"><code>{{ guide.oauthJavaExample }}</code></pre>
            </n-tab-pane>
            <n-tab-pane
              v-if="guide.userAssertionJavaExample"
              name="USER_ASSERTION_JAVA"
              tab="用户断言 Java 17"
            >
              <n-alert type="warning" class="example-alert">
                完整示例会读取 PKCS#8 私钥文件并生成两分钟 RS256 JWT。请配置 FORGE_CLIENT_SECRET、FORGE_USER_ASSERTION_PRIVATE_KEY_FILE 和 FORGE_EXTERNAL_SUBJECT。
              </n-alert>
              <pre class="code-panel"><code>{{ guide.userAssertionJavaExample }}</code></pre>
            </n-tab-pane>
            <n-tab-pane v-if="guide.hmacJavaExample" name="HMAC_JAVA" tab="HMAC Java 17">
              <n-alert type="warning" class="example-alert">
                示例会按真实网关规范生成摘要和签名；Signing Key 从 FORGE_SIGNING_KEY 环境变量读取。
              </n-alert>
              <pre class="code-panel"><code>{{ guide.hmacJavaExample }}</code></pre>
            </n-tab-pane>
            <n-tab-pane name="BODY" tab="请求 Body">
              <pre class="code-panel"><code>{{ requestBodyText }}</code></pre>
            </n-tab-pane>
          </n-tabs>
        </section>

        <CapabilityOnlineTestPanel :guide="guide" />
      </template>

      <div v-else-if="!clientsLoading" class="guide-placeholder">
        <i class="i-material-symbols:integration-instructions-outline-rounded" />
        <strong>选择客户端，平台会告诉你能不能调用</strong>
        <span>检查结果会覆盖网关开关、能力状态、主体模式、认证方式、授权和版本。</span>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <n-space>
          <n-button :loading="markdownDownloading" :disabled="!capability?.id" @click="downloadMarkdown">
            <template #icon>
              <i class="i-material-symbols:description-outline-rounded" />
            </template>
            下载 Markdown
          </n-button>
          <n-button :loading="openApiDownloading" :disabled="!capability?.id" @click="downloadOpenApi">
            <template #icon>
              <i class="i-material-symbols:code-rounded" />
            </template>
            下载 OpenAPI JSON
          </n-button>
        </n-space>
        <n-button @click="emit('update:show', false)">
          关闭
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { useClipboard } from '@vueuse/core'
import { computed, ref, watch } from 'vue'
import {
  downloadCapabilityMarkdown,
  downloadCapabilityOpenApi,
  getCapabilityCallGuide,
  getCapabilityCallGuideClients,
  useCurrentCapabilityGrantVersion,
} from '@/api/ai/capability'
import CapabilityOnlineTestPanel from './CapabilityOnlineTestPanel.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  capability: {
    type: Object,
    default: null,
  },
  canUpdateGrant: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show'])
const { copy } = useClipboard({ legacy: true })

const clients = ref([])
const clientsLoading = ref(false)
const clientsError = ref('')
const selectedClientId = ref(null)
const guide = ref(null)
const guideLoading = ref(false)
const activeExample = ref('BODY')
const markdownDownloading = ref(false)
const openApiDownloading = ref(false)
const versionSwitching = ref(false)

const clientOptions = computed(() => clients.value.map((client) => {
  const status = client.status === 'ENABLED' ? '启用' : client.status || '未知状态'
  const expired = isExpired(client.expiresAt) ? ' · 已过期' : ''
  return {
    label: `${client.clientName}（${client.clientCode}）· ${status}${expired}`,
    value: client.id,
  }
}))

const actorTypeLabel = computed(() => ({
  USER: '实际委托用户',
  SERVICE: '客户端服务账号',
  BOTH: '用户或服务账号',
}[guide.value?.requiredActorType] || guide.value?.requiredActorType || '-'))

const authModeLabel = computed(() => {
  const modes = guide.value?.availableAuthModes || []
  return modes.length ? modes.join(' / ') : '无匹配认证方式'
})

const grantVersionStrategyLabel = computed(() => {
  const fixedVersion = guide.value?.grantFixedVersion
    ? ` v${guide.value.grantFixedVersion}`
    : ''
  if (guide.value?.grantVersionStrategy === 'PINNED')
    return `固定版本${fixedVersion}`
  if (guide.value?.grantVersionStrategy === 'FOLLOW_MAJOR')
    return `跟随主版本（基准${fixedVersion || '未设置'}）`
  return guide.value?.grantVersionStrategy || '尚未授权'
})

const requestBodyText = computed(() => JSON.stringify(guide.value?.requestExample || {}, null, 2))

const currentExample = computed(() => {
  if (activeExample.value === 'OAUTH')
    return guide.value?.oauthExample || ''
  if (activeExample.value === 'HMAC')
    return guide.value?.hmacExample || ''
  if (activeExample.value === 'OAUTH_JAVA')
    return guide.value?.oauthJavaExample || ''
  if (activeExample.value === 'USER_ASSERTION_JAVA')
    return guide.value?.userAssertionJavaExample || ''
  if (activeExample.value === 'HMAC_JAVA')
    return guide.value?.hmacJavaExample || ''
  return requestBodyText.value
})

watch(() => props.show, async (visible) => {
  if (!visible) {
    guide.value = null
    selectedClientId.value = null
    return
  }
  guide.value = null
  selectedClientId.value = null
  activeExample.value = 'BODY'
  await loadClients()
})

watch(() => props.capability?.id, () => {
  if (!props.show)
    return
  guide.value = null
  selectedClientId.value = null
  loadClients()
})

async function loadClients() {
  clientsLoading.value = true
  clientsError.value = ''
  try {
    const res = await getCapabilityCallGuideClients()
    clients.value = res.data || []
    const preferred = clients.value.find(client => client.status === 'ENABLED' && !isExpired(client.expiresAt))
      || clients.value[0]
    selectedClientId.value = preferred?.id || null
    if (selectedClientId.value)
      await loadGuide(selectedClientId.value)
  }
  catch (error) {
    clients.value = []
    clientsError.value = error?.message || '客户端列表加载失败'
  }
  finally {
    clientsLoading.value = false
  }
}

async function loadGuide(clientId) {
  guide.value = null
  if (!clientId || !props.capability?.id)
    return
  guideLoading.value = true
  try {
    const res = await getCapabilityCallGuide(props.capability.id, clientId)
    guide.value = res.data
    activeExample.value = guide.value?.oauthExample
      ? 'OAUTH'
      : guide.value?.hmacExample ? 'HMAC' : 'BODY'
  }
  catch (error) {
    window.$message.error(error?.message || '调用指南加载失败')
  }
  finally {
    guideLoading.value = false
  }
}

function confirmUseCurrentVersion() {
  if (!guide.value?.grantId || !guide.value?.currentVersion || versionSwitching.value)
    return
  window.$dialog.warning({
    title: '切换客户端授权版本',
    content: `确定把该客户端的授权基准切换到 v${guide.value.currentVersion} 吗？平台会保留原授权策略、允许操作和有效期，并重新校验新版契约。`,
    positiveText: '确认切换',
    negativeText: '取消',
    onPositiveClick: useCurrentVersion,
  })
}

async function useCurrentVersion() {
  versionSwitching.value = true
  try {
    const res = await useCurrentCapabilityGrantVersion(guide.value.grantId)
    if (res.code !== 200)
      return
    window.$message.success(`客户端授权已切换到 v${guide.value.currentVersion}`)
    await loadGuide(selectedClientId.value)
  }
  catch (error) {
    window.$message.error(error?.message || '授权版本切换失败')
  }
  finally {
    versionSwitching.value = false
  }
}

function isExpired(expiresAt) {
  if (!expiresAt)
    return false
  const timestamp = new Date(String(expiresAt).replace(' ', 'T')).getTime()
  return Number.isFinite(timestamp) && timestamp <= Date.now()
}

function checkIcon(status) {
  if (status === 'PASSED')
    return 'i-material-symbols:check-circle-rounded'
  if (status === 'FAILED')
    return 'i-material-symbols:error-rounded'
  return 'i-material-symbols:info-rounded'
}

function checkTagType(status) {
  if (status === 'PASSED')
    return 'success'
  if (status === 'FAILED')
    return 'error'
  return 'info'
}

function checkStatusLabel(status) {
  return {
    PASSED: '通过',
    FAILED: '阻断',
    RUNTIME: '运行时校验',
    INFO: '说明',
  }[status] || status
}

async function copyValue(value, successMessage) {
  if (!value)
    return
  try {
    await copy(String(value))
    window.$message.success(successMessage)
  }
  catch {
    window.$message.error('复制失败，请手动选择文本复制')
  }
}

async function downloadMarkdown() {
  if (!props.capability?.id)
    return
  markdownDownloading.value = true
  try {
    const response = await downloadCapabilityMarkdown(props.capability.id)
    saveBlob(response, `${fileStem()}.md`, 'text/markdown;charset=UTF-8')
    window.$message.success('Markdown 调用文档已下载')
  }
  catch (error) {
    window.$message.error(error?.message || 'Markdown 文档下载失败')
  }
  finally {
    markdownDownloading.value = false
  }
}

async function downloadOpenApi() {
  if (!props.capability?.id)
    return
  openApiDownloading.value = true
  try {
    const response = await downloadCapabilityOpenApi(props.capability.id)
    saveBlob(response, `${fileStem()}-openapi.json`, 'application/json')
    window.$message.success('OpenAPI 文档已下载')
  }
  catch (error) {
    window.$message.error(error?.message || 'OpenAPI 文档下载失败')
  }
  finally {
    openApiDownloading.value = false
  }
}

function fileStem() {
  return `${props.capability?.capabilityCode || 'capability'}-${props.capability?.currentVersion || 'latest'}`
}

function saveBlob(response, fallbackName, contentType) {
  const data = response?.data ?? response
  const blob = data instanceof Blob ? data : new Blob([data || ''], { type: contentType })
  const filename = responseFilename(response) || fallbackName
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function responseFilename(response) {
  const disposition = response?.headers?.['content-disposition']
    || response?.headers?.get?.('content-disposition')
  if (!disposition)
    return ''
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    }
    catch {
      return utf8Match[1]
    }
  }
  return disposition.match(/filename="?([^";]+)"?/i)?.[1] || ''
}
</script>

<style scoped>
.guide-body {
  min-height: 420px;
}

.version-alert {
  margin-top: 16px;
}

.version-alert-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.version-alert-content > span:first-child {
  line-height: 1.7;
}

.version-permission-tip {
  flex: none;
  color: var(--text-tertiary);
  font-size: 12px;
}

.modal-heading {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.modal-title {
  flex: none;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-subtitle {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.client-bar {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border-light);
}

.client-field {
  width: min(520px, 100%);
}

.field-label,
.endpoint-label {
  display: block;
  margin-bottom: 7px;
  color: var(--text-secondary);
  font-size: 12px;
}

.guide-loading {
  display: flex;
  min-height: 360px;
  align-items: center;
  justify-content: center;
}

.guide-section {
  padding: 22px 0;
  border-bottom: 1px solid var(--border-light);
}

.guide-section:last-child {
  padding-bottom: 4px;
  border-bottom: 0;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 15px;
}

.section-heading h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

.section-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.endpoint-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-top: 1px solid var(--border-light);
  border-left: 1px solid var(--border-light);
}

.endpoint-item {
  min-width: 0;
  padding: 13px 15px;
  border-right: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.endpoint-wide {
  grid-column: 1 / -1;
}

.endpoint-item strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  word-break: break-word;
}

.copy-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.copy-line code {
  overflow: hidden;
  color: var(--text-primary);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-top: 1px solid var(--border-light);
  border-left: 1px solid var(--border-light);
}

.check-row {
  display: flex;
  gap: 11px;
  min-height: 82px;
  padding: 13px 15px;
  border-right: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
}

.check-state {
  display: flex;
  width: 24px;
  height: 24px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 18px;
}

.state-passed {
  color: var(--success-color);
}

.state-failed {
  color: var(--error-color);
}

.state-runtime,
.state-info {
  color: var(--info-color);
}

.check-content {
  min-width: 0;
}

.check-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.check-title strong {
  color: var(--text-primary);
  font-size: 13px;
}

.check-content p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.example-heading {
  align-items: center;
}

.example-alert {
  margin-bottom: 12px;
}

.code-panel {
  overflow: auto;
  max-height: 360px;
  min-height: 150px;
  margin: 0;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-tertiary);
  color: var(--text-primary);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.guide-placeholder {
  display: flex;
  min-height: 350px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  text-align: center;
}

.guide-placeholder i {
  margin-bottom: 16px;
  font-size: 42px;
}

.guide-placeholder strong {
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.guide-placeholder span {
  max-width: 520px;
  font-size: 12px;
  line-height: 1.7;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

@media (max-width: 760px) {
  .version-alert-content {
    align-items: stretch;
    flex-direction: column;
  }

  .version-permission-tip {
    flex: initial;
  }

  .client-bar {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .client-field {
    width: 100%;
  }

  .endpoint-grid,
  .check-list {
    grid-template-columns: 1fr;
  }

  .endpoint-wide {
    grid-column: auto;
  }

  .modal-footer {
    align-items: stretch;
    flex-direction: column;
  }
}

:global(.capability-call-guide-modal) {
  width: min(1040px, calc(100vw - 32px));
}

:global(.capability-call-guide-modal > .n-card__content) {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
}
</style>
