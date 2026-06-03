<template>
  <div class="app-center-page">
    <header class="page-head">
      <div class="page-title-block">
        <h1>应用总览</h1>
        <p>按业务套件组织对象、入口和引擎能力。</p>
      </div>
      <n-space class="head-actions" :wrap="true">
        <n-button secondary @click="router.push('/app-center/engines')">
          <template #icon>
            <n-icon><HardwareChipOutline /></n-icon>
          </template>
          底座能力
        </n-button>
        <n-button type="primary" @click="openSuiteEditor(null)">
          <template #icon>
            <n-icon><AlbumsOutline /></n-icon>
          </template>
          新建套件
        </n-button>
      </n-space>
    </header>

    <section class="app-center-layout">
      <aside class="suite-nav">
        <div class="suite-nav-head">
          <div>
            <strong>业务套件</strong>
            <span>{{ suites.length }} 个套件</span>
          </div>
          <n-space :size="4">
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button quaternary circle size="small" @click="openSuiteEditor(null)">
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              新建套件
            </n-tooltip>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button quaternary circle size="small" @click="loadAll">
                  <template #icon>
                    <n-icon><RefreshOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              刷新
            </n-tooltip>
          </n-space>
        </div>

        <n-spin :show="loadingSuites">
          <div class="suite-nav-list">
            <button
              class="suite-nav-item"
              :class="{ active: !suiteCode }"
              type="button"
              @click="selectSuite(null)"
            >
              <span class="suite-mark all">
                <n-icon><GridOutline /></n-icon>
              </span>
              <span class="suite-nav-copy">
                <strong>全部业务</strong>
                <small>{{ suiteObjectTotal }} 个对象 · {{ suiteAppTotal }} 个入口</small>
              </span>
            </button>

            <button
              v-for="suite in suites"
              :key="suite.id"
              class="suite-nav-item"
              :class="{ active: suiteCode === suite.suiteCode }"
              type="button"
              @click="selectSuite(suite)"
            >
              <span class="suite-mark" :class="{ 'has-icon': suite.icon }">
                <IconRenderer v-if="suite.icon" :icon="suite.icon" :size="22" />
                <template v-else>{{ suiteInitial(suite) }}</template>
              </span>
              <span class="suite-nav-copy">
                <strong>{{ suite.suiteName || suite.suiteCode }}</strong>
                <small>{{ suite.objectCount || 0 }} 个对象 · {{ suite.appCount || 0 }} 个入口</small>
              </span>
            </button>
          </div>
        </n-spin>
      </aside>

      <main class="workspace">
        <section class="workspace-head">
          <div class="selected-suite-title">
            <span class="suite-mark large" :class="{ 'has-icon': activeSuite?.icon }">
              <IconRenderer v-if="activeSuite?.icon" :icon="activeSuite.icon" :size="24" />
              <template v-else>{{ activeSuiteInitial }}</template>
            </span>
            <div>
              <h2>{{ activeSuiteName }}</h2>
              <p>{{ activeSuiteDescription }}</p>
            </div>
          </div>
          <n-space class="workspace-actions" :wrap="true">
            <n-button secondary :disabled="!activeSuite" @click="openSuite(activeSuite)">
              <template #icon>
                <n-icon><OpenOutline /></n-icon>
              </template>
              进入套件
            </n-button>
            <n-button secondary :disabled="!activeSuite" @click="openSuiteEditor(activeSuite)">
              <template #icon>
                <n-icon><CreateOutline /></n-icon>
              </template>
              编辑套件
            </n-button>
            <n-button secondary @click="openObjectWizard">
              <template #icon>
                <n-icon><CubeOutline /></n-icon>
              </template>
              新建业务对象
            </n-button>
            <n-button type="primary" @click="openEditor(null)">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              新增入口
            </n-button>
          </n-space>
        </section>

        <section class="metric-grid">
          <div v-for="metric in metrics" :key="metric.label" class="metric-item">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
        </section>

        <section class="workspace-toolbar">
          <AppFilterBar
            v-model:keyword="keyword"
            v-model:suite-code="suiteCode"
            v-model:app-type="appType"
            :suites="suites"
            :show-suite="false"
            @search="loadWorkspace"
            @refresh="loadWorkspace"
            @create-object="openObjectWizard"
            @create-app="openEditor(null)"
          />
        </section>

        <section class="workspace-content">
          <n-tabs v-model:value="activeView" type="segment" animated>
            <n-tab-pane name="objects" :tab="`业务对象 ${objectTotal}`">
              <n-spin :show="loadingObjects">
                <div v-if="objects.length" class="card-grid object-grid">
                  <ObjectCard
                    v-for="object in objects"
                    :key="object.id"
                    :object="object"
                    @open="openObject"
                    @design="openObjectDesigner"
                    @stats="openObjectStats"
                    @toggle="toggleObject"
                    @delete="deleteObject"
                  />
                </div>
                <n-empty v-else-if="!loadingObjects" description="当前筛选下暂无业务对象" />
              </n-spin>
              <div v-if="objectTotal > objectPagination.pageSize" class="card-pagination">
                <n-pagination
                  v-model:page="objectPagination.pageNum"
                  v-model:page-size="objectPagination.pageSize"
                  :item-count="objectTotal"
                  :page-sizes="pageSizeOptions"
                  show-size-picker
                  @update:page="loadObjects"
                  @update:page-size="handleObjectPageSizeChange"
                />
              </div>
            </n-tab-pane>
            <n-tab-pane name="apps" :tab="`应用入口 ${appTotal}`">
              <n-spin :show="loadingApps">
                <div v-if="apps.length" class="card-grid app-grid">
                  <AppCard
                    v-for="app in apps"
                    :key="app.id"
                    :app="app"
                    @open="openApp"
                    @config="openEditor"
                    @toggle="toggleApp"
                    @delete="deleteApp"
                  />
                </div>
                <n-empty v-else-if="!loadingApps" description="当前筛选下暂无应用入口" />
              </n-spin>
              <div v-if="appTotal > appPagination.pageSize" class="card-pagination">
                <n-pagination
                  v-model:page="appPagination.pageNum"
                  v-model:page-size="appPagination.pageSize"
                  :item-count="appTotal"
                  :page-sizes="pageSizeOptions"
                  show-size-picker
                  @update:page="loadApps"
                  @update:page-size="handleAppPageSizeChange"
                />
              </div>
            </n-tab-pane>
          </n-tabs>
        </section>
      </main>
    </section>

    <AppEditorDrawer
      v-model:show="editorVisible"
      :app="editingApp"
      :suites="suites"
      @saved="loadAll"
    />
    <BusinessObjectWizardDrawer
      v-model:show="objectWizardVisible"
      :suites="suites"
      :default-suite-code="suiteCode"
      @saved="handleObjectSaved"
    />
    <SuiteEditorDrawer
      v-model:show="suiteEditorVisible"
      :suite="editingSuite"
      @saved="handleSuiteSaved"
    />
    <BusinessObjectDesignerPage
      v-if="designerVisible"
      :key="designerMountKey"
      embedded
      :embedded-object-code="designingObject?.objectCode || ''"
      :embedded-object-id="designingObject?.id || null"
      :embedded-suite-code="designingObject?.suiteCode || suiteCode || ''"
      :initial-panel="designerPanel"
      @saved="loadAll"
      @close="closeObjectDesigner"
    />
  </div>
</template>

<script setup>
import {
  AddOutline,
  AlbumsOutline,
  CreateOutline,
  CubeOutline,
  GridOutline,
  HardwareChipOutline,
  OpenOutline,
  RefreshOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  businessAppDetail,
  businessAppOpenInfo,
  businessAppPage,
  businessObjectPage,
  businessSuiteSummary,
  deleteBusinessApp,
  deleteBusinessObject,
  updateBusinessAppStatus,
  updateBusinessObjectStatus,
} from '@/api/business-app'
import IconRenderer from '@/components/IconRenderer.vue'
import AppCard from './components/AppCard.vue'
import AppEditorDrawer from './components/AppEditorDrawer.vue'
import AppFilterBar from './components/AppFilterBar.vue'
import BusinessObjectWizardDrawer from './components/BusinessObjectWizardDrawer.vue'
import ObjectCard from './components/ObjectCard.vue'
import SuiteEditorDrawer from './components/SuiteEditorDrawer.vue'
import BusinessObjectDesignerPage from './object-designer.[objectCode].vue'

const router = useRouter()
const message = useMessage()

const activeView = ref('objects')
const keyword = ref('')
const suiteCode = ref(null)
const appType = ref(null)
const suites = ref([])
const objects = ref([])
const apps = ref([])
const objectTotal = ref(0)
const appTotal = ref(0)
const loadingSuites = ref(false)
const loadingObjects = ref(false)
const loadingApps = ref(false)
const editorVisible = ref(false)
const editingApp = ref(null)
const objectWizardVisible = ref(false)
const suiteEditorVisible = ref(false)
const editingSuite = ref(null)
const designerVisible = ref(false)
const designingObject = ref(null)
const designerPanel = ref('form')
const pageSizeOptions = [6, 12, 24, 48]
const objectPagination = ref({
  pageNum: 1,
  pageSize: 6,
})
const appPagination = ref({
  pageNum: 1,
  pageSize: 6,
})

const activeSuite = computed(() => suites.value.find(item => item.suiteCode === suiteCode.value) || null)
const suiteObjectTotal = computed(() => suites.value.reduce((sum, item) => sum + Number(item.objectCount || 0), 0))
const suiteAppTotal = computed(() => suites.value.reduce((sum, item) => sum + Number(item.appCount || 0), 0))
const enabledAppCount = computed(() => apps.value.filter(item => item.status === 1).length)
const activeSuiteName = computed(() => activeSuite.value?.suiteName || activeSuite.value?.suiteCode || '全部业务套件')
const activeSuiteDescription = computed(() => {
  if (activeSuite.value?.description)
    return activeSuite.value.description
  return '聚合展示当前范围内的业务对象和应用入口。'
})
const activeSuiteInitial = computed(() => activeSuite.value ? suiteInitial(activeSuite.value) : '全')
const designerMountKey = computed(() => `${designingObject.value?.objectCode || 'object'}_${designerPanel.value}`)
const metrics = computed(() => [
  { label: '业务对象', value: objectTotal.value },
  { label: '应用入口', value: appTotal.value },
  { label: '本页启用', value: enabledAppCount.value },
  { label: '套件总数', value: activeSuite.value ? 1 : suites.value.length },
])

watch([keyword, suiteCode], () => {
  objectPagination.value.pageNum = 1
  appPagination.value.pageNum = 1
  loadWorkspace()
})

watch(appType, () => {
  appPagination.value.pageNum = 1
  loadApps()
})

onMounted(loadAll)

async function loadAll() {
  await loadSuites()
  await loadWorkspace()
}

async function loadWorkspace() {
  await Promise.all([loadObjects(), loadApps()])
}

async function loadSuites() {
  loadingSuites.value = true
  try {
    const res = await businessSuiteSummary()
    suites.value = res.data || []
  }
  finally {
    loadingSuites.value = false
  }
}

async function loadObjects() {
  loadingObjects.value = true
  try {
    const res = await businessObjectPage({
      pageNum: objectPagination.value.pageNum,
      pageSize: objectPagination.value.pageSize,
      keyword: keyword.value,
      suiteCode: suiteCode.value,
    })
    objects.value = res.data?.records || []
    objectTotal.value = Number(res.data?.total || 0)
  }
  finally {
    loadingObjects.value = false
  }
}

async function loadApps() {
  loadingApps.value = true
  try {
    const res = await businessAppPage({
      pageNum: appPagination.value.pageNum,
      pageSize: appPagination.value.pageSize,
      keyword: keyword.value,
      suiteCode: suiteCode.value,
      appType: appType.value,
    })
    apps.value = res.data?.records || []
    appTotal.value = Number(res.data?.total || 0)
  }
  finally {
    loadingApps.value = false
  }
}

function suiteInitial(suite) {
  const text = String(suite?.suiteName || suite?.suiteCode || 'A').trim()
  if (/^[A-Z0-9]{2,4}$/.test(text))
    return text.slice(0, 3)
  return text.slice(0, 2).toUpperCase()
}

function selectSuite(suite) {
  suiteCode.value = suite?.suiteCode || null
}

function openSuite(suite) {
  if (!suite?.suiteCode)
    return
  router.push(`/app-center/suite/${suite.suiteCode}`)
}

function handleObjectPageSizeChange(pageSize) {
  objectPagination.value.pageSize = pageSize
  objectPagination.value.pageNum = 1
  loadObjects()
}

function handleAppPageSizeChange(pageSize) {
  appPagination.value.pageSize = pageSize
  appPagination.value.pageNum = 1
  loadApps()
}

function openObject(object) {
  if (!object?.objectCode)
    return
  router.push({
    path: `/app-center/object/${object.objectCode}`,
    query: { suiteCode: object.suiteCode },
  })
}

function openObjectDesigner(object, panel = 'form') {
  if (!object?.objectCode)
    return
  designingObject.value = {
    ...object,
    suiteCode: object.suiteCode || suiteCode.value,
  }
  designerPanel.value = panel || 'form'
  designerVisible.value = true
}

async function closeObjectDesigner() {
  designerVisible.value = false
  designingObject.value = null
  await loadAll()
}

async function openEditor(app) {
  if (app?.id) {
    try {
      const res = await businessAppDetail(app.id)
      editingApp.value = { ...app, ...(res.data || {}) }
    }
    catch {
      editingApp.value = { ...app }
    }
  }
  else {
    editingApp.value = suiteCode.value ? { suiteCode: suiteCode.value } : null
  }
  editorVisible.value = true
}

function openObjectWizard() {
  objectWizardVisible.value = true
}

function openSuiteEditor(suite) {
  editingSuite.value = suite ? { ...suite } : null
  suiteEditorVisible.value = true
}

async function handleObjectSaved(payload) {
  activeView.value = 'objects'
  suiteCode.value = payload?.suiteCode || suiteCode.value
  await loadAll()
  openObjectDesigner(payload, payload?.designerPanel || 'form')
}

async function handleSuiteSaved(payload) {
  suiteCode.value = payload?.suiteCode || suiteCode.value
  await loadAll()
}

async function openApp(app) {
  const res = await businessAppOpenInfo(app.id)
  const info = res.data || {}
  if (!info.canOpen) {
    message.warning(info.message || '应用入口暂不可打开')
    return
  }
  if (info.openType === 'EXTERNAL' || info.openType === 'H5') {
    window.open(info.targetUrl, '_blank', 'noopener,noreferrer')
    return
  }
  if (info.openType === 'API') {
    message.info('API 类型入口已保留为接口能力，不再跳转独立集成中心')
    return
  }
  router.push(info.targetUrl)
}

function openObjectStats(object) {
  const query = {
    suiteCode: object?.suiteCode || suiteCode.value || undefined,
    objectCode: object?.objectCode || undefined,
  }
  if (object?.configKey)
    query.configKey = object.configKey
  router.push({
    path: '/app-center/stats',
    query,
  })
}

async function toggleApp(app) {
  await updateBusinessAppStatus(app.id, app.status === 1 ? 0 : 1)
  message.success(app.status === 1 ? '应用入口已停用' : '应用入口已启用')
  loadApps()
}

async function toggleObject(object) {
  await updateBusinessObjectStatus(object.id, object.status === 1 ? 0 : 1)
  message.success(object.status === 1 ? '业务对象已停用' : '业务对象已启用')
  await loadObjects()
}

function deleteObject(object) {
  window.$dialog?.warning({
    title: '删除业务对象',
    content: `确定删除“${object.objectName || object.objectCode}”吗？已关联关系或应用入口的对象会被后端拦截。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessObject(object.id)
      message.success('业务对象已删除')
      await loadAll()
    },
  })
}

function deleteApp(app) {
  window.$dialog?.warning({
    title: '删除应用入口',
    content: `确定删除“${app.appName || app.appCode}”吗？删除后不会删除关联业务对象或运行配置。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessApp(app.id)
      message.success('应用入口已删除')
      await loadAll()
    },
  })
}
</script>

<style scoped>
.app-center-page {
  min-height: 100%;
  background: #f6f8fb;
  padding: 20px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.head-actions {
  justify-content: flex-end;
  max-width: 620px;
}

.page-title-block h1,
.workspace-head h2 {
  margin: 0;
  color: #111827;
  font-weight: 700;
  letter-spacing: 0;
}

.page-title-block h1 {
  font-size: 24px;
}

.page-title-block p,
.workspace-head p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.55;
}

.app-center-layout {
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.suite-nav,
.workspace-head,
.metric-grid,
.workspace-toolbar,
.workspace-content {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.suite-nav {
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 140px);
  overflow: auto;
  padding: 14px;
}

.suite-nav-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.suite-nav-head strong,
.suite-nav-head span {
  display: block;
}

.suite-nav-head strong {
  color: #111827;
  font-size: 15px;
}

.suite-nav-head span {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.suite-nav-list {
  display: grid;
  gap: 8px;
}

.suite-nav-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 11px;
  align-items: center;
  width: 100%;
  min-height: 66px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #f9fafb;
  padding: 10px;
  text-align: left;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.suite-nav-item:hover {
  border-color: #cbd5e1;
  background: #fff;
}

.suite-nav-item.active {
  border-color: #2f6feb;
  background: #f4f8ff;
  box-shadow: inset 3px 0 0 #2f6feb;
}

.suite-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 13px;
  font-weight: 700;
}

.suite-mark.has-icon {
  background: #f8fafc;
  color: #2563eb;
}

.suite-mark.all {
  background: #ecfdf5;
  color: #15803d;
  font-size: 20px;
}

.suite-mark.large {
  width: 50px;
  height: 50px;
  font-size: 14px;
}

.suite-nav-copy {
  min-width: 0;
}

.suite-nav-copy strong,
.suite-nav-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suite-nav-copy strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.35;
}

.suite-nav-copy small {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.workspace {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.workspace-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
}

.workspace-actions {
  justify-content: flex-end;
  max-width: 620px;
}

.selected-suite-title {
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-width: 0;
}

.selected-suite-title h2,
.selected-suite-title p {
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-suite-title h2 {
  white-space: nowrap;
  font-size: 20px;
}

.selected-suite-title p {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 0;
  overflow: hidden;
}

.metric-item {
  min-width: 0;
  min-height: 76px;
  border-right: 1px solid #eef2f7;
  padding: 14px 16px;
}

.metric-item:last-child {
  border-right: 0;
}

.metric-item span,
.metric-item strong {
  display: block;
}

.metric-item span {
  color: #6b7280;
  font-size: 12px;
}

.metric-item strong {
  margin-top: 6px;
  color: #111827;
  font-size: 22px;
  line-height: 1.1;
}

.workspace-toolbar {
  background: #fbfcfe;
  padding: 12px;
}

.workspace-content {
  min-width: 0;
  padding: 10px 14px 16px;
}

.workspace-content :deep(.n-spin-content) {
  display: block;
  width: 100%;
}

.workspace-content :deep(.n-tabs-nav) {
  margin-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 10px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
  align-items: stretch;
}

.card-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .app-center-layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-item:nth-child(2) {
    border-right: 0;
  }

  .metric-item:nth-child(-n + 2) {
    border-bottom: 1px solid #eef2f7;
  }
}

@media (max-width: 860px) {
  .page-head,
  .workspace-head {
    display: grid;
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .head-actions,
  .workspace-actions {
    justify-content: flex-start;
    max-width: none;
  }

  .app-center-layout {
    grid-template-columns: 1fr;
  }

  .suite-nav {
    position: static;
    max-height: none;
  }

  .suite-nav-list {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }
}

@media (max-width: 520px) {
  .app-center-page {
    padding: 12px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .metric-item,
  .metric-item:nth-child(2) {
    border-right: 0;
  }

  .metric-item:not(:last-child) {
    border-bottom: 1px solid #eef2f7;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
