<template>
  <div class="app-entry-page">
    <!-- 内部路径 / 可内嵌地址：直接内嵌展示 -->
    <iframe v-if="frameUrl" :src="frameUrl" class="entry-frame" frameborder="0" />
    <!-- 外部打开但未配置地址：显示空白占位，不跳转、不报错 -->
    <n-empty v-else-if="blank" class="blank-holder" description="该入口未配置打开地址" />
    <!-- 其它可打开场景 -->
    <n-result v-else :status="resultStatus" :title="resultTitle" :description="resultDescription">
      <template #footer>
        <n-space justify="center">
          <n-button secondary @click="router.push('/app-center')">
            返回应用中心
          </n-button>
          <n-button v-if="externalUrl" type="primary" @click="openExternal">
            在新窗口打开
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
const message = ref('正在解析访问入口')
const externalUrl = ref('')
const frameUrl = ref('')
const blank = ref(false)

const resultStatus = computed(() => loading.value ? 'info' : externalUrl.value ? 'success' : 'warning')
const resultTitle = computed(() => loading.value ? '正在打开访问入口' : externalUrl.value ? '访问入口可打开' : '访问入口暂不可打开')
const resultDescription = computed(() => message.value)

onMounted(openAppEntry)

async function openAppEntry() {
  const appId = resolveAppId()
  if (!appId) {
    loading.value = false
    message.value = '缺少访问入口 ID'
    return
  }
  const res = await businessAppOpenInfo(appId)
  const info = res.data || {}
  if (info.openType === 'EXTERNAL' || info.openType === 'H5') {
    const url = info.targetUrl || ''
    loading.value = false
    if (!url) {
      // 外部打开但未配置地址：展示空白占位，不跳转、不报错
      blank.value = true
      return
    }
    if (url.startsWith('/')) {
      // 内部路径：内嵌展示
      frameUrl.value = url
      return
    }
    // 外部 http 地址：提供手动打开按钮，不自动弹出
    externalUrl.value = url
    message.value = '该入口为外部地址，可点击按钮在新窗口打开'
    return
  }
  if (!info.canOpen) {
    loading.value = false
    message.value = info.message || '访问入口暂不可打开'
    return
  }
  if (info.openType === 'IFRAME') {
    router.replace({
      path: '/iframe',
      query: buildContextQuery({ page: info.targetUrl }, info),
    })
    return
  }
  if (info.openType === 'API') {
    loading.value = false
    message.value = 'API 类型入口已保留为接口能力，不再跳转独立集成中心'
    return
  }
  router.replace(buildRouteLocation(info.targetRoute || info.targetUrl, info))
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

function buildRouteLocation(targetUrl, info = {}) {
  const target = String(targetUrl || '').trim()
  if (!target)
    return { path: '/app-center' }
  const [pathAndQuery, hashValue = ''] = target.split('#')
  const [path, queryString = ''] = pathAndQuery.split('?')
  const query = {}
  const params = new URLSearchParams(queryString)
  params.forEach((value, key) => {
    query[key] = value
  })
  return {
    path: path || '/app-center',
    query: buildContextQuery(query, info),
    hash: hashValue ? `#${hashValue}` : undefined,
  }
}

function buildContextQuery(query = {}, info = {}) {
  const nextQuery = { ...query }
  if (info.appId && !nextQuery.appId)
    nextQuery.appId = String(info.appId)
  const menuKey = info.activeMenuKey || info.menuResourceId
  if (menuKey && !nextQuery.menuKey)
    nextQuery.menuKey = String(menuKey)
  if (info.menuResourceId && !nextQuery.menuResourceId)
    nextQuery.menuResourceId = String(info.menuResourceId)
  if (info.appName && !nextQuery.title)
    nextQuery.title = info.appName
  if (info.runtimeOpenMode && !nextQuery.runtimeOpenMode)
    nextQuery.runtimeOpenMode = info.runtimeOpenMode
  return nextQuery
}
</script>

<style scoped>
.app-entry-page {
  display: grid;
  min-height: 100%;
  place-items: center;
  background: var(--bg-secondary);
  padding: 24px;
}
.entry-frame {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 96px);
}
.blank-holder {
  padding: 48px 0;
}
</style>