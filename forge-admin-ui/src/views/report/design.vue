<template>
  <div class="report-design-page">
    <div v-if="loading" class="state-panel">
      <n-spin size="large">
        <template #description>
          正在打开{{ pageTitle }}
        </template>
      </n-spin>
    </div>

    <div v-else-if="errorMessage" class="state-panel">
      <n-result
        status="error"
        :title="`${pageTitle}打开失败`"
        :description="errorMessage"
      >
        <template #footer>
          <n-space justify="center">
            <n-button type="primary" @click="loadFrame">
              重试
            </n-button>
            <n-button :disabled="!frameSrc" @click="openInNewTab">
              新窗口打开
            </n-button>
          </n-space>
        </template>
      </n-result>
    </div>

    <div v-else-if="displayMode === 'embed'" class="frame-shell">
      <div class="frame-toolbar">
        <div class="frame-title">
          {{ pageTitle }}
        </div>
        <n-space>
          <n-button size="small" @click="loadFrame">
            刷新登录
          </n-button>
          <n-button size="small" type="primary" @click="openInNewTab">
            新窗口打开
          </n-button>
        </n-space>
      </div>
      <iframe :src="frameSrc" frameborder="0" class="report-frame" />
    </div>

    <div v-else class="state-panel">
      <n-spin size="large">
        <template #description>
          正在跳转{{ pageTitle }}
        </template>
      </n-spin>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { createSsoTicket } from '@/api/auth-sso'
import {
  DEFAULT_SSO_TARGET_CLIENT,
  normalizeSsoRedirectPath,
  resolveSsoTargetBaseUrl,
} from '@/utils/sso-target'

const route = useRoute()

const loading = ref(true)
const errorMessage = ref('')
const frameSrc = ref('')

const targetClient = computed(() => {
  const queryTargetClient = typeof route.query.targetClient === 'string' ? route.query.targetClient.trim() : ''
  return queryTargetClient || DEFAULT_SSO_TARGET_CLIENT
})
const pageTitle = computed(() => {
  const queryTitle = typeof route.query.title === 'string' ? route.query.title.trim() : ''
  return queryTitle || '子系统'
})
const redirectPath = computed(() => normalizeSsoRedirectPath(route.query.redirect, targetClient.value))
const targetBaseUrl = computed(() => {
  const queryBaseUrl = typeof route.query.baseUrl === 'string' ? route.query.baseUrl : ''
  return resolveSsoTargetBaseUrl({
    targetClient: targetClient.value,
    preferredBaseUrl: queryBaseUrl,
  })
})
const displayMode = computed(() => route.query.display === 'redirect' ? 'redirect' : 'embed')

function buildSsoUrl(ticket) {
  return `${targetBaseUrl.value}/#/sso-login?ticket=${encodeURIComponent(ticket)}&redirect=${encodeURIComponent(redirectPath.value)}`
}

async function loadFrame() {
  loading.value = true
  errorMessage.value = ''

  try {
    if (!targetBaseUrl.value) {
      throw new Error(`未配置目标子系统地址：${targetClient.value}`)
    }

    const res = await createSsoTicket({
      targetClient: targetClient.value,
      redirectPath: redirectPath.value,
    })

    const ticket = res?.data?.ticket
    if (!ticket) {
      throw new Error(res?.message || '未获取到 SSO 票据')
    }

    frameSrc.value = buildSsoUrl(ticket)
    if (displayMode.value === 'redirect') {
      window.location.replace(frameSrc.value)
    }
  }
  catch (error) {
    errorMessage.value = error?.message || '获取单点登录地址失败'
  }
  finally {
    loading.value = false
  }
}

async function openInNewTab() {
  if (!frameSrc.value) {
    await loadFrame()
  }

  if (frameSrc.value) {
    window.open(frameSrc.value, '_blank', 'noopener,noreferrer')
  }
}

onMounted(() => {
  loadFrame()
})
</script>

<style scoped>
.report-design-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 128px);
  min-height: 640px;
  padding: 16px;
  box-sizing: border-box;
  background: #f5f7fb;
}

.state-panel {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

.frame-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-primary);
}

.frame-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #eef2f7;
  background: var(--bg-primary);
}

.frame-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.report-frame {
  flex: 1;
  width: 100%;
  min-height: 0;
  background: var(--bg-primary);
}
</style>
