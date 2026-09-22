<template>
  <div class="application-workspace-shell">
    <div class="workspace-state">
      <section v-if="application && workspace" class="workspace-surface">
        <ApplicationWorkspaceHeader
          :application="application"
          :workspace="workspace"
          :refreshing="refreshing"
          @back="router.push('/app-center')"
          @refresh="refreshWorkspace"
          @page-design="openApplicationPageDesigner"
          @runtime="openApplicationRuntime"
          @code="openApplicationCode"
          @primary-action="handlePrimaryAction"
          @publish="openApplicationPublish"
        />

        <div class="workspace-body">
          <aside class="workspace-sidebar">
            <ApplicationWorkspaceNav
              :sections="visibleSections"
              :active-section="activeSection"
              @select="selectSection"
            />
          </aside>

          <main class="workspace-content" :class="{ 'is-overview': activeSection === 'overview' }">
            <div v-if="designerOwnedGuide" class="designer-redirect-card">
              <div class="designer-redirect-icon" aria-hidden="true">
                <n-icon><OpenOutline /></n-icon>
              </div>
              <div class="designer-redirect-copy">
                <h2>{{ designerOwnedGuide.title }}</h2>
                <p>{{ designerOwnedGuide.description }}</p>
              </div>
              <n-button type="primary" @click="openApplicationPageDesigner(designerOwnedGuide.designerSection)">
                <template #icon>
                  <n-icon><CreateOutline /></n-icon>
                </template>
                打开设计器
              </n-button>
            </div>
            <KeepAlive v-else :max="7">
              <component
                :is="activePanelComponent"
                :key="activeSection"
                v-bind="activePanelProps"
                @navigate="selectSection"
                @changed="refreshWorkspace"
                @publish-request-consumed="clearPublishRequest"
                @initial-create-opened="clearCreateHint"
                @action="handleCapabilityAction"
                @open-designer="openFullScreenDesigner"
              />
            </KeepAlive>
          </main>
        </div>
      </section>

      <n-result
        v-else-if="!initialLoading"
        status="404"
        title="应用不存在或无权访问"
        description="请返回应用总览重新选择。"
      >
        <template #footer>
          <n-button type="primary" @click="router.push('/app-center')">
            返回应用总览
          </n-button>
        </template>
      </n-result>

      <div v-if="initialLoading" class="workspace-loading" role="status" aria-label="正在加载应用工作台">
        <n-spin size="medium" />
      </div>

      <AppCodePanel
        v-model:show="applicationCodeVisible"
        scope="APPLICATION"
        :app="application"
      />
    </div>
  </div>
</template>

<script setup>
import { CreateOutline, OpenOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessApplicationWorkspace,
  businessApplicationWorkspaceByCode,
} from '@/api/business-application'
import ApplicationOverviewPanel from './application-workspace/ApplicationOverviewPanel.vue'
import ApplicationWorkspaceHeader from './application-workspace/ApplicationWorkspaceHeader.vue'
import ApplicationWorkspaceNav from './application-workspace/ApplicationWorkspaceNav.vue'
import { buildEntryOpenUrl } from './components/app-entry-targets'
import { normalizeInAppBuilder } from './in-app-builder/in-app-builder-schema'

const ApplicationObjectsPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationObjectsPanel.vue'))
const ApplicationEntriesPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationEntriesPanel.vue'))
const ApplicationExtensionsPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationExtensionsPanel.vue'))
const ApplicationPublishPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationPublishPanel.vue'))
const ApplicationPermissionsPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationPermissionsPanel.vue'))
const AppCodePanel = defineAsyncComponent(() => import('./components/AppCodePanel.vue'))
const route = useRoute()
const router = useRouter()
const message = useMessage()
const application = ref(null)
const workspace = ref(null)
const initialLoading = ref(false)
const refreshing = ref(false)
const publishRequestToken = ref(0)
const applicationCodeVisible = ref(false)

const validSections = new Set([
  'overview',
  'objects',
  'entries',
  'automation',
  'enhancements',
  'permissions',
  'releases',
])

// 这些配置能力已收敛进应用设计器：控制台导航隐藏，但旧 URL 仍可打开并落地引导卡。
const designerOwnedSectionMeta = {
  automation: {
    title: '业务流程已移入应用设计器',
    description: '业务流程的新建、编排、校验与发布统一在应用设计器的「业务流程」中管理，此处不再维护第二份入口。',
    designerSection: 'automation',
  },
  enhancements: {
    title: '动作增强已移入应用设计器',
    description: '业务规则、页面 JS、页面 CSS 和 Java 服务增强统一在应用设计器的「动作增强」中配置，此处不再维护第二份入口。',
    designerSection: 'automation-enhancements',
  },
}

const activeSection = computed(() => {
  const section = String(route.query.section || 'overview')
  return validSections.has(section) ? section : 'overview'
})

const visibleSections = computed(() => (workspace.value?.sections || [])
  .filter(section => !designerOwnedSectionMeta[section.sectionKey] && section.sectionKey !== 'printing'))

const designerOwnedGuide = computed(() => designerOwnedSectionMeta[activeSection.value] || null)

const panelComponents = {
  overview: ApplicationOverviewPanel,
  objects: ApplicationObjectsPanel,
  entries: ApplicationEntriesPanel,
  enhancements: ApplicationExtensionsPanel,
  permissions: ApplicationPermissionsPanel,
  releases: ApplicationPublishPanel,
}

const activePanelComponent = computed(() => panelComponents[activeSection.value] || ApplicationOverviewPanel)

const activePanelProps = computed(() => {
  if (activeSection.value === 'overview') {
    return {
      application: application.value,
      workspace: workspace.value,
    }
  }
  if (activeSection.value === 'objects') {
    return {
      application: application.value,
      initialObjects: workspace.value?.objects || [],
      initialCreateMode: route.query.create === 'database' ? 'DB_IMPORT' : '',
    }
  }
  if (activeSection.value === 'entries') {
    return {
      application: application.value,
      initialEntries: workspace.value?.entries || [],
      applicationObjects: workspace.value?.objects || [],
    }
  }
  if (activeSection.value === 'enhancements') {
    return {
      application: application.value,
      initialExtensions: workspace.value?.extensions || [],
      initialObjects: workspace.value?.objects || [],
      initialEntries: workspace.value?.entries || [],
    }
  }
  if (activeSection.value === 'releases') {
    return {
      application: application.value,
      publishRequestToken: publishRequestToken.value,
    }
  }
  if (activeSection.value === 'permissions') {
    return {
      application: application.value,
      initialObjects: workspace.value?.objects || [],
    }
  }
  return {}
})

watch(() => route.params.applicationCode, loadWorkspace)
onMounted(loadWorkspace)

async function loadWorkspace() {
  const applicationCode = route.params.applicationCode
  if (!applicationCode)
    return
  initialLoading.value = true
  try {
    const workspaceResponse = await businessApplicationWorkspaceByCode(applicationCode)
    workspace.value = workspaceResponse.data || null
    application.value = workspace.value?.application || null
    consumeRoutePublishRequest()
  }
  catch {
    application.value = null
    workspace.value = null
  }
  finally {
    initialLoading.value = false
  }
}

async function refreshWorkspace() {
  if (!application.value?.id || refreshing.value)
    return
  refreshing.value = true
  try {
    const response = await businessApplicationWorkspace(application.value.id)
    workspace.value = response.data || workspace.value
    application.value = workspace.value?.application || application.value
  }
  finally {
    refreshing.value = false
  }
}

function selectSection(section) {
  if (!validSections.has(section))
    return
  router.replace({
    query: {
      ...route.query,
      section: section === 'overview' ? undefined : section,
    },
  })
}

function openFullScreenDesigner(payload = {}) {
  if (payload.type === 'DATA_SCOPE_ADAPTER' || payload.panel === 'permission') {
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        section: 'permissions',
        dataScopeObjectId: payload.objectId || undefined,
      },
    })
    return
  }
  if (payload.processId) {
    router.push({
      name: 'BusinessProcessDesigner',
      params: { processId: String(payload.processId) },
      query: {
        applicationCode: application.value?.applicationCode || route.params.applicationCode,
        returnTo: route.fullPath,
      },
    })
    return
  }
  if (!payload.objectCode && !payload.objectId)
    return
  const matchedObject = (workspace.value?.objects || []).find(item => (
    (payload.objectId && String(item.objectId) === String(payload.objectId))
    || (payload.objectCode && item.objectCode === payload.objectCode)
  ))
  const objectCode = payload.objectCode || matchedObject?.objectCode
  if (!objectCode)
    return
  router.push({
    name: 'BusinessObjectDesigner',
    params: { objectCode },
    query: {
      objectId: payload.objectId || matchedObject?.objectId || undefined,
      suiteCode: application.value?.suiteCode || undefined,
      panel: payload.panel || 'fields',
      detailTab: payload.detailTab || (payload.panel === 'detail' ? 'detail' : 'form'),
      returnTo: route.fullPath,
    },
  })
}

function handlePrimaryAction() {
  selectSection('objects')
}

function openApplicationPublish() {
  publishRequestToken.value += 1
  selectSection('releases')
}

// 应用可能只配置了访问入口（对象列表/表单页），并没有自由编排页面。
// 此时运行壳的首页是空的，直接打开会显示“页面尚未配置”，因此优先落到入口。
const designedHomePageId = computed(() => {
  if (!application.value)
    return ''
  const schema = normalizeInAppBuilder(
    application.value?.options,
    application.value,
    workspace.value?.objects || [],
  )
  return String(schema.homePageId || '')
})

const runnableEntry = computed(() => {
  const entries = Array.isArray(workspace.value?.entries) ? workspace.value.entries : []
  return entries.find(item => item?.id && item.status === 1)
    || entries.find(item => item?.id)
    || null
})

function openEntryRuntime(entry) {
  const url = buildEntryOpenUrl(entry)
  if (!url) {
    message.warning('该入口尚未关联可运行的页面配置，请先发布业务单元')
    return
  }
  const fullUrl = url.startsWith('/')
    ? `${window.location.origin}${url}`
    : url
  window.open(fullUrl, '_blank', 'noopener,noreferrer')
}

function openApplicationRuntime() {
  if (!application.value?.applicationCode)
    return
  if (!designedHomePageId.value && runnableEntry.value) {
    openEntryRuntime(runnableEntry.value)
    return
  }
  if (!designedHomePageId.value) {
    message.warning('该应用还没有可运行的页面，请先配置访问入口或设计页面')
    return
  }
  const target = router.resolve({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: application.value.applicationCode },
    query: { pageId: designedHomePageId.value },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openApplicationPageDesigner(designerSection = '') {
  if (!application.value?.applicationCode)
    return
  const section = typeof designerSection === 'string' ? designerSection : ''
  const directResource = section === 'automation-enhancements'
  router.push({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: application.value.applicationCode },
    query: {
      edit: '1',
      ...(section ? directResource ? { designResource: section } : { designSection: section } : {}),
    },
  })
}

function openApplicationCode() {
  if (!application.value?.id)
    return
  applicationCodeVisible.value = true
}

function clearPublishRequest() {
  publishRequestToken.value = 0
}

function consumeRoutePublishRequest() {
  if (!application.value?.id || String(route.query.publish || '') !== '1')
    return
  publishRequestToken.value += 1
  const query = { ...route.query, section: 'releases' }
  delete query.publish
  router.replace({ query })
}

function handleCapabilityAction(action) {
  if (action === 'designer') {
    openApplicationPageDesigner('data-model')
    return
  }
  selectSection(action)
}

function clearCreateHint() {
  if (!route.query.create)
    return
  const query = { ...route.query }
  delete query.create
  router.replace({ query })
}
</script>

<style scoped>
.application-workspace-shell {
  min-height: 100%;
  padding: 0 8px 8px;
  color: var(--text-primary, #1d2129);
  background: var(--bg-secondary, #f7f8fa);
}

.workspace-state {
  position: relative;
  min-height: calc(100vh - 38px);
}

.workspace-surface {
  overflow: hidden;
  min-height: calc(100vh - 46px);
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.workspace-body {
  display: grid;
  grid-template-columns: 204px minmax(0, 1fr);
  min-height: calc(100vh - 108px);
}

.workspace-sidebar {
  border-right: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-secondary, #f7f8fa);
}

.workspace-content {
  overflow: auto;
  min-width: 0;
  padding: 12px;
  background: var(--bg-primary, #fff);
}

.workspace-content.is-overview {
  padding: 10px 12px 12px;
}

.designer-redirect-card {
  display: flex;
  align-items: center;
  gap: 18px;
  max-width: 760px;
  margin: 48px auto 0;
  padding: 28px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 8px;
  background: var(--bg-secondary, #f7f8fa);
}

.designer-redirect-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 10%, transparent);
  font-size: 22px;
}

.designer-redirect-copy {
  min-width: 0;
  flex: 1;
}

.designer-redirect-copy h2 {
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 16px;
}

.designer-redirect-copy p {
  margin: 6px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 13px;
  line-height: 20px;
}

.workspace-loading {
  position: absolute;
  z-index: 2;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-primary, #fff) 88%, transparent);
}

@media (max-width: 860px) {
  .workspace-body {
    grid-template-columns: 1fr;
  }

  .workspace-sidebar {
    overflow: hidden;
    border-right: 0;
    border-bottom: 1px solid var(--border-light, #e5e6eb);
  }

  .workspace-sidebar :deep(.workspace-nav) {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 4px;
    padding: 8px;
  }

  .workspace-sidebar :deep(.nav-item) {
    min-width: 0;
  }

  .workspace-content {
    padding: 10px;
  }
}
</style>
