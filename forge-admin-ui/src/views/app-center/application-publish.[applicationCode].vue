<template>
  <div class="application-publish-page">
    <header class="publish-header">
      <div class="publish-header-left">
        <n-button quaternary circle :aria-label="runtimeReturnLabel" @click="returnWorkspace">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
        </n-button>
        <button type="button" class="publish-breadcrumb" @click="returnWorkspace">
          <span>{{ runtimeReturnLabel }}</span>
          <span aria-hidden="true">›</span>
        </button>
        <div class="publish-header-title">
          <span>应用发布</span>
          <strong>{{ application?.applicationName || '未命名应用' }}</strong>
        </div>
      </div>
      <nav class="publish-header-tabs">
        <button type="button" class="publish-header-tab" @click="goRuntime">
          {{ runtimeReturnLabel }}
        </button>
        <button type="button" class="publish-header-tab" @click="openSettings">
          应用设置
        </button>
        <button type="button" class="publish-header-tab active">
          应用发布
        </button>
      </nav>
      <div class="publish-header-actions">
        <n-button secondary :disabled="!application" @click="openPcRuntime">
          <template #icon>
            <n-icon><DesktopOutline /></n-icon>
          </template>
          电脑端运行
        </n-button>
        <n-button type="primary" :disabled="!application || application.status !== 1" @click="historyRef?.preparePublish()">
          <template #icon>
            <n-icon><RocketOutline /></n-icon>
          </template>
          立即发布
        </n-button>
      </div>
    </header>

    <n-spin :show="loading">
      <div v-if="application" class="publish-page-body">
        <AppPublishStatusCard :application="application" :toggling="toggling" @toggle="toggleStatus" />
        <div class="publish-section-layout">
          <aside class="publish-section-nav">
            <button
              v-for="item in publishSections"
              :key="item.key"
              type="button"
              :class="{ active: activePublishSection === item.key }"
              @click="activePublishSection = item.key"
            >
              <n-icon><component :is="item.icon" /></n-icon>
              <span>{{ item.label }}</span>
            </button>
          </aside>
          <main class="publish-section-content">
            <div v-show="activePublishSection === 'access'">
              <AppPublishAccess :application="application" />
            </div>
            <div v-show="activePublishSection === 'ai'">
              <AppPublishAiAssistant :application="application" :pages="pages" @changed="loadApplication" />
            </div>
            <div v-show="activePublishSection === 'distribute'">
              <AppPublishDistribute :application="application" @changed="loadApplication" />
            </div>
            <div v-show="activePublishSection === 'history'">
              <AppPublishVersionHistory ref="historyRef" :application="application" @changed="loadApplication" @navigate="handleNavigate" />
            </div>
          </main>
        </div>
      </div>
      <n-result v-else-if="!loading" status="error" title="应用发布信息加载失败" :description="loadError">
        <template #footer>
          <n-button @click="loadApplication">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            重新加载
          </n-button>
        </template>
      </n-result>
    </n-spin>
  </div>
</template>

<script setup>
import {
  ArrowBackOutline,
  DesktopOutline,
  LinkOutline,
  RefreshOutline,
  RocketOutline,
  ShareSocialOutline,
  SparklesOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessApplicationDetailByCode,
  updateBusinessApplicationStatus,
} from '@/api/business-application'
import { parseJsonObject } from './components/portal/portal-config'
import AppPublishAccess from './components/publish/AppPublishAccess.vue'
import AppPublishAiAssistant from './components/publish/AppPublishAiAssistant.vue'
import AppPublishDistribute from './components/publish/AppPublishDistribute.vue'
import AppPublishStatusCard from './components/publish/AppPublishStatusCard.vue'
import AppPublishVersionHistory from './components/publish/AppPublishVersionHistory.vue'
import { resolveDeprecatedWorkspaceLocation } from './workspace-redirect'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const application = ref(null)
const loading = ref(false)
const toggling = ref(false)
const loadError = ref('')
const historyRef = ref(null)
const activePublishSection = ref('history')
const publishSections = [
  { key: 'access', label: '访问地址', icon: LinkOutline },
  { key: 'ai', label: 'AI 助理', icon: SparklesOutline },
  { key: 'distribute', label: '应用分发', icon: ShareSocialOutline },
  { key: 'history', label: '发布历史', icon: RocketOutline },
]
const pages = computed(() => parseJsonObject(application.value?.options)?.inAppBuilder?.nodes || [])

watch(() => String(route.params.applicationCode || ''), loadApplication, { immediate: true })

async function loadApplication() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await businessApplicationDetailByCode(route.params.applicationCode)
    application.value = response.data || null
    if (!application.value)
      throw new Error('应用不存在')
  }
  catch (error) {
    application.value = null
    loadError.value = error?.message || '暂时无法读取应用发布信息。'
  }
  finally {
    loading.value = false
  }
}

async function toggleStatus() {
  if (!application.value || toggling.value)
    return
  toggling.value = true
  try {
    const nextStatus = application.value.status === 1 ? 0 : 1
    await updateBusinessApplicationStatus(application.value.id, nextStatus)
    message.success(nextStatus === 1 ? '应用已启用' : '应用已停用')
    await loadApplication()
  }
  catch (error) {
    message.error(error?.message || '更新应用状态失败')
  }
  finally {
    toggling.value = false
  }
}

const fromPageDesign = computed(() => String(route.query.fromPageDesign || '') === '1'
  || route.query.draft === '1'
  || route.query.edit === '1'
  || route.query.returnEdit === '1')

const runtimeReturnLabel = computed(() => {
  if (route.query.draft === '1')
    return '返回预览'
  if (fromPageDesign.value)
    return '返回设计'
  return '页面管理'
})

function buildRuntimeReturnQuery() {
  if (!fromPageDesign.value)
    return {}
  const query = {}
  const pageId = String(route.query.pageId || '').trim()
  if (pageId)
    query.pageId = pageId
  // 优先回到草稿预览；否则回到编辑态
  if (route.query.draft === '1') {
    query.draft = '1'
    query.returnEdit = '1'
  }
  else if (route.query.edit === '1' || route.query.returnEdit === '1') {
    query.edit = '1'
    if (route.query.returnEdit === '1')
      query.returnEdit = '1'
  }
  return query
}

function returnWorkspace() {
  router.push({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: route.params.applicationCode },
    query: buildRuntimeReturnQuery(),
  })
}

function goRuntime() {
  router.push({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: route.params.applicationCode },
    query: buildRuntimeReturnQuery(),
  })
}

/** 与应用列表「运行」一致：打开电脑端门户运行页；带 pageId 时直达当前页 */
function openPcRuntime() {
  if (!application.value?.applicationCode) {
    message.warning('应用信息未加载完成')
    return
  }
  const pageId = String(route.query.pageId || '').trim()
  const target = router.resolve({
    name: 'ApplicationPortal',
    params: {
      applicationCodeOrSlug: application.value.portalSlug || application.value.applicationCode,
    },
    query: pageId ? { pageId } : undefined,
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openSettings() {
  router.push({ name: 'BusinessApplicationSettings', params: { applicationCode: route.params.applicationCode } })
}

function handleNavigate(section) {
  router.push(resolveDeprecatedWorkspaceLocation({
    params: { applicationCode: route.params.applicationCode },
    query: { section: section || 'overview' },
  }))
}
</script>

<style scoped>
.application-publish-page {
  height: 100vh;
  overflow-y: auto;
  background: var(--bg-secondary);
  /* 覆盖全局 body overflow:hidden */
  position: fixed;
  inset: 0;
}

.publish-header {
  flex: 0 0 auto;
  height: 56px;
  display: grid;
  grid-template-columns: minmax(200px, 1fr) auto minmax(240px, 1fr);
  align-items: center;
  gap: 16px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.publish-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.publish-breadcrumb {
  cursor: pointer;
  border: 0;
  background: transparent;
  padding: 4px 6px;
  border-radius: 4px;
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 20px;
  white-space: nowrap;
}

.publish-breadcrumb:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.publish-breadcrumb span:first-child {
  color: var(--text-tertiary);
}

.publish-header-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.publish-header-title span {
  color: var(--text-tertiary);
  font-size: 13px;
}

.publish-header-title strong {
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.publish-header-tabs {
  display: flex;
  align-items: center;
  justify-self: center;
  gap: 2px;
}

.publish-header-tab {
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  background: transparent;
  padding: 5px 14px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 20px;
  white-space: nowrap;
}

.publish-header-tab:hover {
  color: var(--text-primary);
}

.publish-header-tab.active {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  font-weight: 600;
}

.publish-header-actions {
  display: flex;
  align-items: center;
  justify-self: end;
  gap: 8px;
}

.publish-page-body {
  display: grid;
  width: min(1280px, calc(100% - 48px));
  gap: 20px;
  margin: 20px auto 32px;
}

/* 左侧 tab 分区布局 */
.publish-section-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.publish-section-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-primary);
  position: sticky;
  top: 72px;
}

.publish-section-nav button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.15s,
    color 0.15s;
}

.publish-section-nav button:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.publish-section-nav button.active {
  background: #e8f1ff;
  color: #1677ff;
  font-weight: 600;
}

.publish-section-content {
  min-width: 0;
}

.publish-section-content :deep(.publish-section-card) {
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-primary);
  padding: 24px;
  box-shadow: 0 1px 3px rgb(31 35 41 / 4%);
}

.publish-section-content :deep(.publish-section-card > header) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.publish-section-content :deep(.publish-section-card > header h2) {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
}

.publish-section-content :deep(.publish-section-card > header p) {
  margin: 5px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.publish-section-content :deep(.publish-card-alert) {
  margin-bottom: 18px;
}

@media (max-width: 768px) {
  .publish-section-layout {
    grid-template-columns: 1fr;
  }
  .publish-section-nav {
    flex-direction: row;
    position: static;
    overflow-x: auto;
  }
}
</style>
