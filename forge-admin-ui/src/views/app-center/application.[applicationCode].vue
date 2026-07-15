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
          @preview="openApplicationPreview"
          @code="openApplicationCode"
          @primary-action="handlePrimaryAction"
          @publish="openApplicationPublish"
        />

        <div class="workspace-body">
          <aside class="workspace-sidebar">
            <ApplicationWorkspaceNav
              :sections="workspace.sections"
              :active-section="activeSection"
              @select="selectSection"
            />
          </aside>

          <main class="workspace-content" :class="{ 'is-overview': activeSection === 'overview' }">
            <KeepAlive :max="7">
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

const ApplicationObjectsPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationObjectsPanel.vue'))
const ApplicationEntriesPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationEntriesPanel.vue'))
const ApplicationAutomationPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationAutomationPanel.vue'))
const ApplicationExtensionsPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationExtensionsPanel.vue'))
const ApplicationPublishPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationPublishPanel.vue'))
const ApplicationCapabilityPanel = defineAsyncComponent(() => import('./application-workspace/ApplicationCapabilityPanel.vue'))
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

const activeSection = computed(() => {
  const section = String(route.query.section || 'overview')
  return validSections.has(section) ? section : 'overview'
})

const capabilityPanel = computed(() => {
  const configs = {
    permissions: {
      title: '权限',
      description: '汇总入口可见范围、对象动作权限、字段权限和数据权限。',
      assetCount: application.value?.objectCount || 0,
      items: [
        {
          title: '对象与字段权限',
          description: '进入业务对象设计器查看动作权限和数据权限摘要。',
          action: 'objects',
          actionLabel: '选择业务对象',
        },
        {
          title: '页面入口可见范围',
          description: '入口级角色、菜单资源和打开权限在页面入口中维护。',
          action: 'entries',
          actionLabel: '查看页面入口',
        },
      ],
    },
  }
  return configs[activeSection.value] || configs.permissions
})

const panelComponents = {
  overview: ApplicationOverviewPanel,
  objects: ApplicationObjectsPanel,
  entries: ApplicationEntriesPanel,
  automation: ApplicationAutomationPanel,
  enhancements: ApplicationExtensionsPanel,
  permissions: ApplicationCapabilityPanel,
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
  if (activeSection.value === 'automation') {
    return {
      application: application.value,
      initialObjects: workspace.value?.objects || [],
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
  return capabilityPanel.value
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
  selectSection(Number(application.value?.entryCount || 0) > 0 ? 'entries' : 'objects')
}

function openApplicationPublish() {
  publishRequestToken.value += 1
  selectSection('releases')
}

function openApplicationCode() {
  if (!application.value?.id)
    return
  applicationCodeVisible.value = true
}

function openApplicationPreview() {
  const objects = workspace.value?.objects || []
  const primary = objects.find(item => item.objectRole === 'PRIMARY' && item.configKey)
    || objects.find(item => item.configKey)
  if (!primary?.configKey) {
    message.warning(objects.length ? '主对象还没有可预览的页面配置，请先完成对象设计' : '应用还没有数据对象，请先添加对象')
    selectSection('objects')
    return
  }
  const target = router.resolve({
    name: 'AiCrudPageDynamic',
    params: { configKey: primary.configKey },
    query: {
      designPreview: '1',
      title: `${application.value?.applicationName || primary.objectName || '应用'}预览`,
    },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
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
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid var(--border-light, #e5e6eb);
  }

  .workspace-sidebar :deep(.workspace-nav) {
    min-width: 920px;
    flex-direction: row;
  }

  .workspace-content {
    padding: 10px;
  }
}
</style>
