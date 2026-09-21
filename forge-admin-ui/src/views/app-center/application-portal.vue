<template>
  <div class="application-portal" :lang="portalLanguage" :class="[`navigation-${navigationStyle}`, { 'is-collapsed': navigationCollapsed, 'is-h5': isMobileDisplay }]" :style="portalStyle">
    <ApplicationPortalSkeleton v-if="loading" />
    <template v-else>
      <PortalEmptyState
        v-if="loadState"
        :type="loadState"
        :description="loadError"
      />

      <template v-else-if="application">
        <header class="portal-header">
          <div class="portal-brand">
            <n-button
              v-if="portalConfig.navigation.collapsible && navigationStyle !== 'top'"
              quaternary
              circle
              class="portal-collapse"
              :aria-label="navigationCollapsed ? '展开导航' : '收起导航'"
              @click="navigationCollapsed = !navigationCollapsed"
            >
              <template #icon>
                <n-icon><MenuOutline /></n-icon>
              </template>
            </n-button>
            <span v-if="portalConfig.navigation.showLogo" class="portal-logo" aria-hidden="true">
              <IconRenderer v-if="application.icon" :icon="application.icon" :size="22" />
              <n-icon v-else><AppsOutline /></n-icon>
            </span>
            <strong v-if="portalConfig.navigation.showName">{{ application.applicationName }}</strong>
          </div>

          <PortalNavigation
            v-if="navigationStyle === 'top'"
            :nodes="navigationNodes"
            :current-page-id="currentPageId"
            navigation-style="top"
            @select="selectPage"
          />

          <div class="portal-header-actions">
            <n-tooltip v-if="assistantAvailable" trigger="hover">
              <template #trigger>
                <n-button quaternary circle aria-label="打开应用 AI 助理" @click="assistantVisible = true">
                  <template #icon>
                    <n-icon><SparklesOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              询问当前页面
            </n-tooltip>
            <MessageNotification
              class="portal-message-notification"
              :message-route="systemPageRoutes.messages"
              :todo-route="systemPageRoutes.todo"
              :done-route="systemPageRoutes.done"
            />
            <UserAvatar :profile-route="profileRoute" />
          </div>
        </header>

        <div class="portal-shell">
          <div class="portal-mobile-navigation">
            <PortalNavigation
              :nodes="navigationNodes"
              :current-page-id="currentPageId"
              navigation-style="side"
              @select="selectPage"
            />
          </div>
          <aside v-if="navigationStyle !== 'top'" class="portal-sidebar">
            <PortalNavigation
              :nodes="navigationNodes"
              :current-page-id="currentPageId"
              :navigation-style="navigationStyle"
              :collapsed="navigationCollapsed"
              @select="selectPage"
            />
          </aside>

          <main class="portal-main">
            <PortalEmptyState
              v-if="!pageNodes.length"
              type="forbidden"
              :show-back="false"
            />
            <PageManagementSystemView
              v-else-if="currentSystemPage"
              :key="currentPageId"
              class="portal-system-page"
              :view="currentSystemPage.view"
              :title="currentSystemPage.title"
              :navigation-routes="systemPageRoutes"
            />
            <PortalPageRenderer
              v-else
              :key="currentPageId"
              :node="currentNode"
              :page="currentPage"
              :objects="runtime.objects"
              :entries="runtime.entries"
              :extensions="runtime.extensions"
              :application-id="String(application.id || '')"
              :application-code="application.applicationCode"
              :page-id="currentPageId"
            />
            <div
              v-if="watermarkText && portalConfig.watermark.scope !== 'full'"
              class="portal-watermark is-content"
              :style="watermarkStyle"
              aria-hidden="true"
            />
          </main>
        </div>

        <n-modal v-model:show="profileVisible" preset="card" title="个人资料" class="portal-profile-modal">
          <div class="portal-profile-content">
            <n-avatar round :size="56" :style="{ backgroundColor: 'var(--portal-primary)', fontSize: '22px' }">
              {{ profileInitial }}
            </n-avatar>
            <div>
              <strong>{{ userStore.realName || userStore.staffInfo?.staffName || userStore.username || '用户' }}</strong>
              <span>{{ userStore.username || '-' }}</span>
              <small>当前应用：{{ application.applicationName || application.applicationCode }}</small>
            </div>
          </div>
        </n-modal>

        <div
          v-if="watermarkText && portalConfig.watermark.scope === 'full'"
          class="portal-watermark is-full"
          :style="watermarkStyle"
          aria-hidden="true"
        />
      </template>
    </template>
    <PortalAiAssistant
      v-if="application"
      v-model:show="assistantVisible"
      :identifier="String(route.params.applicationCodeOrSlug || '')"
      :application="application"
      :page-id="currentPageId"
      :page-title="currentNode?.title || currentNode?.name || currentPageId"
    />
  </div>
</template>

<script setup>
import { AppsOutline, MenuOutline, SparklesOutline } from '@vicons/ionicons5'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessApplicationRuntimeByCodeOrSlug } from '@/api/business-application'
import IconRenderer from '@/components/IconRenderer.vue'
import MessageNotification from '@/layouts/components/MessageNotification.vue'
import UserAvatar from '@/layouts/components/UserAvatar.vue'
import { useUserStore } from '@/store'
import ApplicationPortalSkeleton from './components/portal/ApplicationPortalSkeleton.vue'
import PageManagementSystemView from './components/portal/PageManagementSystemView.vue'
import {
  buildPortalWatermarkStyle,
  buildPortalWatermarkText,
  normalizePortalConfig,
  parseJsonObject,
} from './components/portal/portal-config'
import { buildApplicationPortalNavigationNodes } from './components/portal/portal-navigation-runtime'
import PortalAiAssistant from './components/portal/PortalAiAssistant.vue'
import PortalEmptyState from './components/portal/PortalEmptyState.vue'
import PortalNavigation from './components/portal/PortalNavigation.vue'
import PortalPageRenderer from './components/portal/PortalPageRenderer.vue'
import { normalizeInAppBuilder } from './in-app-builder/in-app-builder-schema'
import { resolvePageManagementSystemPage } from './in-app-builder/page-management'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const application = ref(null)
const builder = ref(null)
const loadState = ref('')
const loadError = ref('')
const navigationCollapsed = ref(false)
const assistantVisible = ref(false)
const runtime = reactive({ objects: [], entries: [], extensions: [], versionNo: null })

const isMobileDisplay = computed(() => route.meta.display === 'h5' || String(route.query.display || '') === 'h5')
const portalConfig = computed(() => normalizePortalConfig(application.value?.portalConfig))
const portalLanguage = computed(() => portalConfig.value.globalization.enabled
  ? portalConfig.value.globalization.defaultLanguage
  : 'zh-CN')
const navigationStyle = computed(() => {
  const value = String(portalConfig.value.navigation.style || 'side')
  return ['side', 'top', 'collapsed'].includes(value) ? value : 'side'
})
const navigationNodes = computed(() => buildApplicationPortalNavigationNodes(
  builder.value?.nodes || [],
  isMobileDisplay.value ? 'h5' : 'pc',
))
const pageNodes = computed(() => navigationNodes.value.filter(node => node.type === 'page'))
const profileVisible = computed({
  get: () => String(route.query.profile || '') === '1',
  set: (visible) => {
    router.replace({
      query: {
        ...route.query,
        profile: visible ? '1' : undefined,
      },
    })
  },
})
const profileRoute = computed(() => ({
  name: 'ApplicationPortal',
  params: { applicationCodeOrSlug: route.params.applicationCodeOrSlug },
  query: { ...route.query, profile: '1' },
}))
const profileInitial = computed(() => String(userStore.realName || userStore.staffInfo?.staffName || userStore.username || 'U').charAt(0).toUpperCase())
const systemPageRoutes = computed(() => ({
  workbench: buildSystemPageRoute('system:workbench'),
  todo: buildSystemPageRoute('system:todo'),
  done: buildSystemPageRoute('system:done'),
  sent: buildSystemPageRoute('system:sent'),
  cc: buildSystemPageRoute('system:cc'),
  messages: buildSystemPageRoute('system:messages'),
}))
const currentPageId = computed(() => {
  const requested = String(route.query.pageId || '')
  if (pageNodes.value.some(node => String(node.id) === requested))
    return requested
  const homePageId = String(builder.value?.homePageId || '')
  if (pageNodes.value.some(node => String(node.id) === homePageId))
    return homePageId
  return String(pageNodes.value[0]?.id || '')
})
const currentNode = computed(() => pageNodes.value.find(node => String(node.id) === currentPageId.value) || null)
const currentSystemPage = computed(() => resolvePageManagementSystemPage(currentPageId.value))
const currentPage = computed(() => currentNode.value ? builder.value?.pages?.[currentNode.value.id] || null : null)
const assistantConfig = computed(() => parseJsonObject(application.value?.aiAssistantConfig))
const assistantAvailable = computed(() => assistantConfig.value.enabled === true
  && Boolean(assistantConfig.value.agentCode)
  && (assistantConfig.value.pageIds || []).map(String).includes(currentPageId.value)
  && (assistantConfig.value.capabilities || []).length > 0)
const portalStyle = computed(() => ({
  '--portal-primary': normalizeThemeColor(portalConfig.value.themeColor),
  '--portal-surface': '#ffffff',
  '--portal-surface-muted': '#f5f7fa',
  '--portal-text': '#1f2329',
  '--portal-text-muted': '#646a73',
}))
const watermarkText = computed(() => buildPortalWatermarkText(
  portalConfig.value,
  userStore.realName || userStore.username,
))
const watermarkStyle = computed(() => buildPortalWatermarkStyle(watermarkText.value))

watch(() => String(route.params.applicationCodeOrSlug || ''), loadPortal, { immediate: true })
watch(navigationStyle, (style) => {
  navigationCollapsed.value = style === 'collapsed' || portalConfig.value.navigation.collapsed === true
}, { immediate: true })
watch(currentPageId, (pageId) => {
  if (pageId && String(route.query.pageId || '') !== pageId)
    router.replace({ query: { ...route.query, pageId } })
})

async function loadPortal(identifier) {
  if (!identifier) {
    loading.value = false
    return
  }
  loading.value = true
  loadState.value = ''
  loadError.value = ''
  try {
    const response = await businessApplicationRuntimeByCodeOrSlug(identifier)
    const data = response.data || {}
    application.value = data.application || null
    runtime.objects = data.objects || []
    runtime.entries = data.entries || []
    runtime.extensions = data.extensions || []
    runtime.versionNo = data.versionNo || application.value?.lastPublishVersion || null
    builder.value = normalizeInAppBuilder(application.value?.options, application.value, runtime.objects)
    if (!application.value)
      throw new Error('应用运行配置不存在')
    if (!pageNodes.value.length)
      loadState.value = 'forbidden'
  }
  catch (error) {
    application.value = null
    builder.value = null
    runtime.objects = []
    runtime.entries = []
    runtime.extensions = []
    const message = resolveErrorMessage(error)
    loadState.value = /权限|无权|forbidden/i.test(message) ? 'forbidden' : /停用|未发布|不存在|不可用/.test(message) ? 'unavailable' : 'error'
    loadError.value = message
  }
  finally {
    loading.value = false
  }
}

function selectPage(pageId) {
  if (!pageNodes.value.some(node => String(node.id) === String(pageId)))
    return
  router.replace({ query: { ...route.query, pageId: String(pageId) } })
}

function buildSystemPageRoute(pageId) {
  const query = { ...route.query, pageId }
  delete query.taskId
  delete query.source
  delete query.t
  delete query.profile
  return {
    name: 'ApplicationPortal',
    params: { applicationCodeOrSlug: route.params.applicationCodeOrSlug },
    query,
  }
}

function normalizeThemeColor(value) {
  return /^#[0-9a-f]{6}$/i.test(String(value || '')) ? String(value) : '#3370ff'
}

function resolveErrorMessage(error) {
  return String(
    error?.message
    || error?.detail?.rawMessage
    || error?.error?.message
    || '暂时无法读取应用配置，请稍后重试。',
  )
}
</script>

<style scoped>
.application-portal {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: var(--portal-surface-muted);
  color: var(--portal-text);
}

.portal-header {
  position: sticky;
  z-index: 30;
  top: 0;
  display: grid;
  height: 56px;
  grid-template-columns: minmax(180px, auto) minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  border-bottom: 1px solid #e5e6eb;
  background: color-mix(in srgb, var(--portal-surface) 96%, transparent);
  padding: 0 18px;
  backdrop-filter: blur(12px);
}

.navigation-side .portal-header,
.navigation-collapsed .portal-header {
  grid-template-columns: minmax(180px, 1fr) auto;
}

.portal-brand,
.portal-header-actions {
  display: flex;
  min-width: 0;
  align-items: center;
}

.portal-brand {
  gap: 10px;
}

.portal-brand strong {
  overflow: hidden;
  font-size: 15px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portal-logo {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 7px;
  background: color-mix(in srgb, var(--portal-primary) 12%, transparent);
  color: var(--portal-primary);
  font-size: 20px;
}

.portal-header-actions {
  justify-content: flex-end;
  gap: 8px;
}

.portal-system-top-menu {
  --top-menu-bg-color: transparent;
  --top-menu-text-color: var(--portal-text-muted);
  --top-menu-text-color-hover: var(--portal-text);
  --top-menu-text-color-active: var(--portal-primary);
}

.portal-message-notification {
  margin-right: 0;
  color: var(--portal-text-muted);
}

.portal-profile-content {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: min(320px, 80vw);
}

.portal-profile-content > div {
  display: grid;
  gap: 3px;
}

.portal-profile-content strong {
  font-size: 16px;
}

.portal-profile-content span,
.portal-profile-content small {
  color: var(--portal-text-muted);
  font-size: 12px;
}

.portal-shell {
  display: flex;
  height: calc(100vh - 56px);
  min-height: 0;
}

.portal-mobile-navigation {
  display: none;
}

.portal-sidebar {
  position: sticky;
  z-index: 20;
  top: 56px;
  height: calc(100vh - 56px);
  flex: 0 0 auto;
  overflow-y: auto;
  border-right: 1px solid #e5e6eb;
  background: var(--portal-surface);
}

.portal-main {
  position: relative;
  min-width: 0;
  min-height: 0;
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--portal-surface-muted);
}

.portal-system-page {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
}

.portal-watermark {
  position: absolute;
  z-index: 24;
  inset: 0;
  pointer-events: none;
}

.portal-watermark.is-full {
  position: fixed;
  z-index: 1001;
}

.application-portal.is-h5 .portal-header {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 0 12px;
}

.application-portal.is-h5 .portal-header > :deep(.portal-navigation),
.application-portal.is-h5 .portal-header > :deep(.portal-system-top-menu) {
  display: none;
}

.application-portal.is-h5 .portal-shell {
  flex-direction: column;
}

.application-portal.is-h5 .portal-mobile-navigation {
  position: sticky;
  z-index: 21;
  top: 56px;
  display: block;
  overflow: hidden;
  border-bottom: 1px solid #e5e6eb;
  background: var(--portal-surface);
}

.application-portal.is-h5 .portal-sidebar {
  display: none;
}

@media (max-width: 900px) {
  .portal-header {
    grid-template-columns: minmax(0, 1fr) auto;
    padding: 0 12px;
  }

  .portal-header > :deep(.portal-navigation),
  .portal-header > :deep(.portal-system-top-menu) {
    display: none;
  }

  .portal-shell {
    flex-direction: column;
  }

  .portal-mobile-navigation {
    position: sticky;
    z-index: 21;
    top: 56px;
    display: block;
    overflow: hidden;
    border-bottom: 1px solid #e5e6eb;
    background: var(--portal-surface);
  }

  .portal-sidebar {
    display: none;
  }
}

@media print {
  .application-portal,
  .portal-shell {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .portal-header,
  .portal-sidebar,
  .portal-watermark {
    display: none !important;
  }
}
</style>
