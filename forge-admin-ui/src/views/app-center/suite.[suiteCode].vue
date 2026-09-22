<template>
  <div class="suite-detail-page">
    <header class="suite-head">
      <div class="suite-head-top">
        <n-button text @click="router.push('/app-center')">
          返回应用中心
        </n-button>
        <n-space :wrap="true" size="small">
          <n-button secondary :disabled="!suite" @click="openSuiteEditor">
            <template #icon>
              <n-icon><CreateOutline /></n-icon>
            </template>
            编辑业务域
          </n-button>
          <n-button secondary type="warning" :disabled="!suite" @click="toggleSuite">
            <template #icon>
              <n-icon><PowerOutline /></n-icon>
            </template>
            {{ suiteStatusActionText(suite) }}
          </n-button>
          <n-button secondary type="error" :disabled="!suite" @click="deleteSuite">
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
            删除业务域
          </n-button>
        </n-space>
      </div>
      <div class="suite-title">
        <span class="suite-avatar" :class="{ 'has-icon': suite?.icon }">
          <IconRenderer v-if="suite?.icon" :icon="suite.icon" :size="28" />
          <template v-else>{{ suiteInitial }}</template>
        </span>
        <div>
          <div class="suite-title-line">
            <h1>{{ suite?.suiteName || suiteCode }}</h1>
            <DictTag v-if="suite" dict-type="sys_enable_disable" :value="suite.status" :bordered="false" />
          </div>
          <p>{{ suite?.description || '业务域详情' }}</p>
        </div>
      </div>
      <div class="suite-stats">
        <div><strong>{{ objectTotal }}</strong><span>业务单元</span></div>
        <div><strong>{{ appTotal }}</strong><span>访问入口</span></div>
        <div><strong>{{ enabledAppCount }}</strong><span>本页启用</span></div>
      </div>
    </header>

    <div class="suite-content-grid">
      <main class="suite-main">
        <section class="suite-section">
          <div class="section-head">
            <div>
              <h2>业务单元</h2>
              <p>优先从对象进入关系、能力和标准业务入口。</p>
            </div>
            <n-space size="small">
              <n-button secondary @click="openSuiteStats">
                业务域看板
              </n-button>
              <n-button type="primary" @click="openObjectWizard">
                新建对象
              </n-button>
            </n-space>
          </div>
          <n-spin :show="loadingObjects">
            <div v-if="objects.length" class="object-grid">
              <ObjectCard
                v-for="object in objects"
                :key="object.id"
                :object="object"
                @open="openObject"
                @edit="openObjectEditor"
                @design="openObjectDesigner"
                @stats="openObjectStats"
                @toggle="toggleObject"
                @delete="deleteObject"
              />
            </div>
            <n-empty v-else-if="!loadingObjects" description="当前业务域暂无业务单元" />
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
        </section>

        <section class="suite-section">
          <div class="section-head">
            <div>
              <h2>场景入口</h2>
              <p>业务应用、报表看板和自动化入口按场景展示。</p>
            </div>
            <n-button type="primary" @click="openEditor(null)">
              新增入口
            </n-button>
          </div>
          <n-spin :show="loadingApps">
            <div v-if="apps.length" class="app-grid">
              <AppCard
                v-for="app in apps"
                :key="app.id"
                :app="app"
                @open="openApp"
                @code="openCodePanel"
                @config="openEditor"
                @toggle="toggleApp"
                @delete="deleteApp"
              />
            </div>
            <n-empty v-else-if="!loadingApps" description="当前业务域暂无访问入口" />
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
        </section>
      </main>

      <aside class="suite-side">
        <section class="suite-section acceptance-section">
          <div class="section-head compact">
            <div>
              <h2>交付验收</h2>
              <p>检查业务域是否达到最小交付标准。</p>
            </div>
          </div>
          <SuiteAcceptancePanel
            :suite-code="suiteCode"
            compact
            @object-click="handleAcceptanceObjectClick"
            @action="handleAcceptanceAction"
          />
        </section>
      </aside>
    </div>

    <AppEditorDrawer
      v-model:show="editorVisible"
      :app="editingApp"
      :suites="suite ? [suite] : []"
      @saved="loadAll"
    />
    <AppCodePanel
      v-model:show="codePanelVisible"
      :app="codingApp"
    />
    <BusinessObjectWizardDrawer
      v-model:show="objectWizardVisible"
      :suites="suite ? [suite] : []"
      :default-suite-code="suiteCode"
      @saved="handleObjectSaved"
    />
    <BusinessObjectEditorDrawer
      v-model:show="objectEditorVisible"
      :object="editingObject"
      :suites="objectEditorSuites"
      @saved="handleObjectEdited"
    />
    <SuiteEditorDrawer
      v-model:show="suiteEditorVisible"
      :suite="suite"
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
import { CreateOutline, PowerOutline, TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessAppDetail,
  businessAppOpenInfo,
  businessAppPage,
  businessObjectDetail,
  businessObjectPage,
  businessSuiteList,
  deleteBusinessApp,
  deleteBusinessObject,
  deleteBusinessSuite,
  updateBusinessAppStatus,
  updateBusinessObjectStatus,
  updateBusinessSuiteStatus,
} from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { useTabStore } from '@/store'
import { getDefaultPageTitle } from '@/utils/page-title'
import AppCard from './components/AppCard.vue'
import ObjectCard from './components/ObjectCard.vue'

const AppCodePanel = defineAsyncComponent(() => import('./components/AppCodePanel.vue'))
const AppEditorDrawer = defineAsyncComponent(() => import('./components/AppEditorDrawer.vue'))
const BusinessObjectEditorDrawer = defineAsyncComponent(() => import('./components/BusinessObjectEditorDrawer.vue'))
const BusinessObjectWizardDrawer = defineAsyncComponent(() => import('./components/BusinessObjectWizardDrawer.vue'))
const SuiteAcceptancePanel = defineAsyncComponent(() => import('./components/SuiteAcceptancePanel.vue'))
const SuiteEditorDrawer = defineAsyncComponent(() => import('./components/SuiteEditorDrawer.vue'))
const BusinessObjectDesignerPage = defineAsyncComponent(() => import('./object-designer.[objectCode].vue'))

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tabStore = useTabStore()
const suiteCode = computed(() => route.params.suiteCode)
const suite = ref(null)
const objects = ref([])
const apps = ref([])
const objectTotal = ref(0)
const appTotal = ref(0)
const loadingObjects = ref(false)
const loadingApps = ref(false)
const editorVisible = ref(false)
const editingApp = ref(null)
const codePanelVisible = ref(false)
const codingApp = ref(null)
const objectWizardVisible = ref(false)
const objectEditorVisible = ref(false)
const editingObject = ref(null)
const objectEditorSuites = ref([])
const suiteEditorVisible = ref(false)
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

const suiteInitial = computed(() => String(suite.value?.suiteName || suiteCode.value || 'A').slice(0, 2).toUpperCase())
const enabledAppCount = computed(() => apps.value.filter(item => item.status === 1).length)
const pageTitle = computed(() => suite.value?.suiteName || suiteCode.value || '业务域详情')
const designerMountKey = computed(() => `${designingObject.value?.objectCode || 'object'}_${designerPanel.value}`)

onMounted(loadAll)

watch(pageTitle, (title) => {
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${getDefaultPageTitle()}`
  tabStore.updateTabTitle(route.fullPath, title)
}, { immediate: true })

async function loadAll() {
  await Promise.all([loadSuite(), loadObjects(), loadApps()])
}

async function loadSuite() {
  const res = await businessSuiteList({ suiteCode: suiteCode.value })
  suite.value = (res.data || [])[0] || null
}

async function loadObjects() {
  loadingObjects.value = true
  try {
    const res = await businessObjectPage({
      pageNum: objectPagination.value.pageNum,
      pageSize: objectPagination.value.pageSize,
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
      suiteCode: suiteCode.value,
    })
    apps.value = res.data?.records || []
    appTotal.value = Number(res.data?.total || 0)
  }
  finally {
    loadingApps.value = false
  }
}

function openObject(object) {
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
    editingApp.value = { suiteCode: suiteCode.value }
  }
  editorVisible.value = true
}

function openObjectWizard() {
  objectWizardVisible.value = true
}

async function openObjectEditor(object) {
  if (!object?.id)
    return
  await loadObjectEditorSuites()
  try {
    const res = await businessObjectDetail(object.id)
    editingObject.value = { ...object, ...(res.data || {}) }
  }
  catch {
    editingObject.value = { ...object }
  }
  objectEditorVisible.value = true
}

async function loadObjectEditorSuites() {
  if (objectEditorSuites.value.length)
    return
  const res = await businessSuiteList()
  objectEditorSuites.value = res.data || []
}

function suiteStatusActionText(currentSuite) {
  return currentSuite?.status === 0 ? '启用业务域' : '停用业务域'
}

function openSuiteEditor() {
  suiteEditorVisible.value = true
}

async function handleSuiteSaved() {
  await loadAll()
}

async function handleObjectSaved(data) {
  await loadAll()
  openObjectDesigner(data, data?.designerPanel || 'form')
}

async function handleObjectEdited(data) {
  if (data?.suiteCode && data.suiteCode !== suiteCode.value) {
    await router.replace(`/app-center/suite/${data.suiteCode}`)
  }
  await loadAll()
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

async function openApp(app) {
  if (isCodeDownloadApp(app)) {
    await openCodePanel(app)
    return
  }
  const res = await businessAppOpenInfo(app.id)
  const info = res.data || {}
  if (info.openType === 'API') {
    message.info('API 类型入口已保留为接口能力，不再跳转独立集成中心')
    return
  }
  // 外部打开/未配置地址的入口：不再弹外部窗口或弹错误提示，
  // 统一内嵌到 app-entry 页面展示（无地址显示空白占位，不跳转、不报错）
  if (info.openType === 'EXTERNAL' || info.openType === 'H5' || !info.canOpen) {
    router.push(`/app-center/app/${app.id}`)
    return
  }
  router.push(info.targetUrl)
}

async function openCodePanel(app) {
  if (!app?.id)
    return
  try {
    const res = await businessAppDetail(app.id)
    codingApp.value = { ...app, ...(res.data || {}) }
  }
  catch {
    codingApp.value = { ...app }
  }
  codePanelVisible.value = true
}

function isCodeDownloadApp(app) {
  return app?.entryMode === 'RUNTIME' && app?.appMode === 'CODE_DOWNLOAD'
}

function openObjectStats(object) {
  const query = {
    suiteCode: object?.suiteCode || suiteCode.value,
    objectCode: object?.objectCode || undefined,
  }
  if (object?.configKey)
    query.configKey = object.configKey
  router.push({
    path: '/app-center/stats',
    query,
  })
}

function openSuiteStats() {
  router.push({
    path: '/app-center/stats',
    query: { suiteCode: suiteCode.value },
  })
}

async function toggleApp(app) {
  await updateBusinessAppStatus(app.id, app.status === 1 ? 0 : 1)
  message.success(app.status === 1 ? '访问入口已停用' : '访问入口已启用')
  loadApps()
}

async function applyObjectStatus(object, nextStatus) {
  await updateBusinessObjectStatus(object.id, nextStatus)
  message.success(nextStatus === 0 ? '业务单元已停用' : '业务单元已启用')
  await loadObjects()
}

async function toggleObject(object) {
  const nextStatus = object.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    window.$dialog?.warning({
      title: '停用业务单元',
      content: `确定停用“${object.objectName || object.objectCode}”吗？停用后关联配置仍保留，但用户不应继续进入该业务单元办理新业务。`,
      positiveText: '停用',
      negativeText: '取消',
      onPositiveClick: () => applyObjectStatus(object, nextStatus),
    })
    return
  }
  await applyObjectStatus(object, nextStatus)
}

async function applySuiteStatus(currentSuite, nextStatus) {
  await updateBusinessSuiteStatus(currentSuite.id, nextStatus)
  message.success(nextStatus === 0 ? '业务域已停用' : '业务域已启用')
  await loadAll()
}

async function toggleSuite() {
  if (!suite.value?.id)
    return
  const currentSuite = suite.value
  const nextStatus = currentSuite.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    window.$dialog?.warning({
      title: '停用业务域',
      content: `确定停用“${currentSuite.suiteName || currentSuite.suiteCode}”吗？停用后该业务域下的业务单元和访问入口配置仍会保留。`,
      positiveText: '停用',
      negativeText: '取消',
      onPositiveClick: () => applySuiteStatus(currentSuite, nextStatus),
    })
    return
  }
  await applySuiteStatus(currentSuite, nextStatus)
}

function deleteSuite() {
  if (!suite.value?.id)
    return
  const currentSuite = suite.value
  const orphanEntryCount = Number(currentSuite.appCount || 0)
  const orphanObjectCount = Number(currentSuite.objectCount || 0)
  const cleanupOrphanResources = orphanEntryCount > 0 || orphanObjectCount > 0
  const cleanupTargets = [
    orphanEntryCount > 0 ? `${orphanEntryCount} 个孤立访问入口` : null,
    orphanObjectCount > 0 ? `${orphanObjectCount} 个未被应用使用的业务对象配置` : null,
  ].filter(Boolean).join('、')
  window.$dialog?.warning({
    title: '删除业务域',
    content: cleanupOrphanResources
      ? `确定删除“${currentSuite.suiteName || currentSuite.suiteCode}”吗？将同时清理该业务域内的${cleanupTargets}；访问入口菜单将停用，对应业务数据表和历史版本不会被物理删除。`
      : `确定删除“${currentSuite.suiteName || currentSuite.suiteCode}”吗？存在子业务域或业务应用时仍会阻止删除。`,
    positiveText: cleanupOrphanResources ? '删除并清理' : '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessSuite(currentSuite.id, cleanupOrphanResources)
      message.success('业务域已删除')
      router.replace('/app-center')
    },
  })
}

function deleteObject(object) {
  const tableName = object.tableName || object.configKey
  const tableHint = tableName
    ? `关联物理表「${tableName}」不会被删除。`
    : '关联数据库物理表不会被删除。'
  window.$dialog?.warning({
    title: '危险操作：删除业务单元',
    content: `确定删除“${object.objectName || object.objectCode}”吗？将清理对象元数据与相关配置，删除后无法从应用侧恢复。${tableHint}已关联关系或访问入口的对象会被后端拦截。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessObject(object.id)
      message.success('业务单元已删除')
      await loadAll()
    },
  })
}

function deleteApp(app) {
  window.$dialog?.warning({
    title: '删除访问入口',
    content: `确定删除“${app.appName || app.appCode}”吗？删除后不会删除关联业务单元或运行配置。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessApp(app.id)
      message.success('访问入口已删除')
      await loadAll()
    },
  })
}

function handleAcceptanceObjectClick(obj) {
  openObjectDesigner({
    ...obj,
    suiteCode: obj?.suiteCode || suiteCode.value,
  }, obj?.runnable ? 'publish' : 'fields')
}

function handleAcceptanceAction(action, data) {
  switch (action) {
    case 'FIX_OBJECT':
      if (data?.objects) {
        const obj = data.objects.find(o => !o.runnable)
        if (obj) {
          handleAcceptanceObjectClick(obj)
        }
      }
      break
    case 'VIEW_ACCEPTANCE_REPORT':
      message.success('所有核心对象已就绪，可以交付使用。')
      break
    case 'OPEN_ENGINE_CENTER':
      router.push('/app-center/engines')
      break
    case 'OPEN_MOBILE_CENTER':
      message.info('移动能力已收敛到业务入口和运行页配置')
      break
    case 'OPEN_INTEGRATION_CENTER':
      message.info('集成能力已收敛到消息通道和触发器配置')
      break
    case 'OPEN_CHANNEL_CENTER':
      router.push('/app-center/trigger')
      break
    default:
      break
  }
}
</script>

<style scoped>
.suite-detail-page {
  min-height: 100%;
  background: #f6f8fb;
  padding: 20px;
}

.suite-head,
.suite-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.suite-head {
  margin-bottom: 16px;
}

.suite-head-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.suite-title {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  margin-top: 12px;
}

.suite-title > span {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 8px;
  background: #eef2ff;
  color: #3730a3;
  font-weight: 700;
}

.suite-title > span.has-icon {
  background: #f8fafc;
  color: #2563eb;
}

.suite-title h1,
.section-head h2 {
  margin: 0;
  color: #111827;
  letter-spacing: 0;
}

.suite-title-line {
  display: flex;
  min-width: 0;
  gap: 10px;
  align-items: center;
}

.suite-title h1 {
  min-width: 0;
  overflow: hidden;
  font-size: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suite-title p,
.section-head p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.suite-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.suite-stats div {
  border-left: 3px solid #dbeafe;
  background: #f8fafc;
  padding: 10px 12px;
}

.suite-stats strong,
.suite-stats span {
  display: block;
}

.suite-stats strong {
  color: #111827;
  font-size: 20px;
}

.suite-stats span {
  color: #6b7280;
  font-size: 12px;
}

.suite-section {
  margin-bottom: 16px;
}

.suite-section :deep(.n-spin-content) {
  display: block;
  width: 100%;
}

.suite-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 420px);
  gap: 16px;
  align-items: start;
}

.suite-main,
.suite-side {
  min-width: 0;
}

.suite-side {
  position: sticky;
  top: 16px;
}

.acceptance-section {
  border-color: #dbeafe;
  background: #f8fbff;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head.compact {
  display: block;
}

.object-grid,
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 12px;
}

.card-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .suite-content-grid {
    grid-template-columns: 1fr;
  }

  .suite-side {
    position: static;
  }
}

@media (max-width: 680px) {
  .suite-detail-page {
    padding: 12px;
  }

  .suite-head-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .suite-stats {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }

  .object-grid,
  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
