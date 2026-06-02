<template>
  <div class="app-entry-page">
    <n-result :status="resultStatus" :title="resultTitle" :description="resultDescription">
      <template #footer>
        <n-space justify="center">
          <n-button secondary @click="router.push('/app-center')">
            返回应用中心
          </n-button>
          <n-button v-if="externalUrl" type="primary" @click="openExternal">
            打开入口
          </n-button>
        </n-space>
      </template>
    </n-result>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessAppOpenInfo } from '@/api/business-app'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const message = ref('正在解析应用入口')
const externalUrl = ref('')

const resultStatus = computed(() => loading.value ? 'info' : externalUrl.value ? 'success' : 'warning')
const resultTitle = computed(() => loading.value ? '正在打开应用入口' : externalUrl.value ? '应用入口已在新窗口打开' : '应用入口暂不可打开')
const resultDescription = computed(() => message.value)

onMounted(openAppEntry)

async function openAppEntry() {
  const appId = resolveAppId()
  if (!appId) {
    loading.value = false
    message.value = '缺少应用入口 ID'
    return
  }
  const res = await businessAppOpenInfo(appId)
  const info = res.data || {}
  if (!info.canOpen) {
    loading.value = false
    message.value = info.message || '应用入口暂不可打开'
    return
  }
  if (info.openType === 'EXTERNAL' || info.openType === 'H5') {
    externalUrl.value = info.targetUrl
    openExternal()
    loading.value = false
    message.value = '浏览器已尝试打开新窗口。如未打开，请使用下方按钮。'
    return
  }
  if (info.openType === 'IFRAME') {
    router.replace({
      path: '/iframe',
      query: { page: info.targetUrl },
    })
    return
  }
  if (info.openType === 'API') {
    loading.value = false
    message.value = 'API 类型入口已保留为接口能力，不再跳转独立集成中心'
    return
  }
  router.replace(info.targetUrl)
}

function openExternal() {
  if (externalUrl.value)
    window.open(externalUrl.value, '_blank', 'noopener,noreferrer')
}

function resolveAppId() {
  if (route.params.appId)
    return route.params.appId
  const match = String(route.path || '').match(/\/app-center\/app\/([^/]+)$/)
  return match?.[1] || null
}
</script>

<style scoped>
.app-entry-page {
  display: grid;
  min-height: 100%;
  place-items: center;
  background: #f6f8fb;
  padding: 24px;
}
</style>
