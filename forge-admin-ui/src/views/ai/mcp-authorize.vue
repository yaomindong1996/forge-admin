<template>
  <div class="authorize-page">
    <main class="authorize-panel" aria-labelledby="authorize-title">
      <div class="panel-heading">
        <div class="security-mark" aria-hidden="true">
          <NIcon :size="24">
            <ShieldCheckmarkOutline />
          </NIcon>
        </div>
        <div>
          <h1 id="authorize-title">
            确认访问授权
          </h1>
          <p>允许下方客户端以你的身份访问 Forge MCP</p>
        </div>
      </div>

      <NSpin :show="loading">
        <NAlert v-if="errorMessage" type="error" :show-icon="true" class="error-alert">
          {{ errorMessage }}
        </NAlert>

        <template v-else-if="requestInfo">
          <section class="client-section">
            <span class="section-label">请求客户端</span>
            <strong>{{ requestInfo.clientName }}</strong>
            <span class="client-id">客户端 ID：{{ requestInfo.clientId }}</span>
          </section>

          <section class="detail-section">
            <div class="detail-row">
              <span>当前身份</span>
              <strong>{{ identityText }}</strong>
            </div>
            <div class="detail-row">
              <span>有效时间</span>
              <strong>{{ expiryText }}</strong>
            </div>
          </section>

          <section class="scope-section">
            <span class="section-label">将获得以下权限</span>
            <ul>
              <li v-for="scope in requestInfo.scopes" :key="scope">
                <NIcon :size="17" aria-hidden="true">
                  <CheckmarkCircleOutline />
                </NIcon>
                <span>{{ scopeLabel(scope) }}</span>
              </li>
            </ul>
          </section>

          <p class="security-note">
            授权仅对本次短期令牌生效。令牌到期自动失效，也可以在 Forge 中提前撤销。
          </p>

          <div class="panel-actions">
            <NButton :disabled="submitting" @click="submitDecision(false)">
              拒绝
            </NButton>
            <NButton type="primary" :loading="submitting" @click="submitDecision(true)">
              同意授权
            </NButton>
          </div>
        </template>
      </NSpin>
    </main>
  </div>
</template>

<script setup>
import { CheckmarkCircleOutline, ShieldCheckmarkOutline } from '@vicons/ionicons5'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { decideMcpAuthorization, getMcpAuthorizationRequest } from '@/api/ai/capability-oauth'

const route = useRoute()
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const requestInfo = ref(null)

const oauthParams = computed(() => ({
  client_id: queryValue('client_id'),
  response_type: queryValue('response_type'),
  redirect_uri: queryValue('redirect_uri'),
  resource: queryValue('resource'),
  scope: queryValue('scope'),
  code_challenge: queryValue('code_challenge'),
  code_challenge_method: queryValue('code_challenge_method'),
  state: queryValue('state'),
}))

const identityText = computed(() => {
  if (!requestInfo.value)
    return ''
  const tenant = requestInfo.value.tenantName || `租户 ${requestInfo.value.tenantId}`
  const org = requestInfo.value.activeOrgName || `组织 ${requestInfo.value.activeOrgId}`
  return `${tenant} / ${org}`
})

const expiryText = computed(() => {
  const seconds = Number(requestInfo.value?.expiresInSeconds || 0)
  return seconds > 0 ? `${Math.ceil(seconds / 60)} 分钟` : '短期有效'
})

function queryValue(name) {
  const value = route.query[name]
  if (Array.isArray(value))
    return value[0] || ''
  return value == null ? '' : String(value)
}

function scopeLabel(scope) {
  const labels = {
    'capability:discover:capability.ping': '查看已授权的 MCP 能力',
    'capability:invoke:capability.ping': '调用已授权的 MCP 能力',
  }
  return labels[scope] || scope
}

function validateRequiredParams() {
  const required = [
    'client_id',
    'response_type',
    'redirect_uri',
    'resource',
    'scope',
    'code_challenge',
    'code_challenge_method',
  ]
  return required.every(name => oauthParams.value[name])
}

async function loadAuthorizationRequest() {
  if (!validateRequiredParams()) {
    errorMessage.value = '授权请求参数不完整，请返回客户端重新发起。'
    loading.value = false
    return
  }
  try {
    const response = await getMcpAuthorizationRequest(oauthParams.value)
    requestInfo.value = response.data
  }
  catch (error) {
    errorMessage.value = error?.message || '授权请求无效或已经失效，请返回客户端重试。'
  }
  finally {
    loading.value = false
  }
}

async function submitDecision(approved) {
  if (submitting.value)
    return
  submitting.value = true
  errorMessage.value = ''
  try {
    const response = await decideMcpAuthorization({
      clientId: oauthParams.value.client_id,
      responseType: oauthParams.value.response_type,
      redirectUri: oauthParams.value.redirect_uri,
      resource: oauthParams.value.resource,
      scope: oauthParams.value.scope,
      codeChallenge: oauthParams.value.code_challenge,
      codeChallengeMethod: oauthParams.value.code_challenge_method,
      state: oauthParams.value.state || null,
      approved,
    })
    const redirectUri = response.data?.redirectUri
    if (!redirectUri)
      throw new Error('服务端未返回安全回调地址')
    window.location.assign(redirectUri)
  }
  catch (error) {
    errorMessage.value = error?.message || '授权处理失败，请返回客户端重试。'
    submitting.value = false
  }
}

onMounted(loadAuthorizationRequest)
</script>

<style scoped>
.authorize-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f3f4f6;
}

.authorize-panel {
  width: min(100%, 560px);
  padding: 28px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 10px;
  background: var(--n-color, #fff);
  box-shadow: 0 8px 24px rgb(15 23 42 / 6%);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--n-border-color, #e5e7eb);
}

.security-mark {
  width: 42px;
  height: 42px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
}

h1 {
  margin: 0;
  color: var(--n-text-color, #111827);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
}

.panel-heading p {
  margin: 4px 0 0;
  color: var(--n-text-color-3, #6b7280);
  font-size: 14px;
}

.error-alert {
  margin-top: 22px;
}

.client-section,
.scope-section {
  padding: 22px 0;
}

.client-section {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.section-label,
.detail-row > span {
  color: var(--n-text-color-3, #6b7280);
  font-size: 13px;
}

.client-section strong {
  margin-top: 2px;
  color: var(--n-text-color, #111827);
  font-size: 17px;
}

.client-id {
  color: var(--n-text-color-3, #6b7280);
  font-size: 12px;
}

.detail-section {
  padding: 4px 0;
  border-top: 1px solid var(--n-border-color, #e5e7eb);
  border-bottom: 1px solid var(--n-border-color, #e5e7eb);
}

.detail-row {
  min-height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.detail-row strong {
  color: var(--n-text-color, #111827);
  font-size: 14px;
  font-weight: 500;
  text-align: right;
}

.scope-section ul {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.scope-section li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--n-text-color-2, #374151);
  font-size: 14px;
}

.scope-section li .n-icon {
  color: #16a34a;
}

.security-note {
  margin: 0;
  padding: 12px 14px;
  border-radius: 6px;
  color: var(--n-text-color-3, #6b7280);
  background: var(--n-color-embedded, #f9fafb);
  font-size: 12px;
  line-height: 1.6;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .authorize-page {
    align-items: start;
    padding: 16px;
  }

  .authorize-panel {
    margin-top: 32px;
    padding: 22px 18px;
  }

  .detail-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
    padding: 10px 0;
  }

  .detail-row strong {
    text-align: left;
  }
}
</style>
