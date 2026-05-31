<template>
  <div class="suite-detail-page">
    <header class="suite-head">
      <n-button text @click="router.push('/app-center')">
        返回应用中心
      </n-button>
      <div class="suite-title">
        <span class="suite-avatar" :class="{ 'has-icon': suite?.icon }">
          <IconRenderer v-if="suite?.icon" :icon="suite.icon" :size="28" />
          <template v-else>{{ suiteInitial }}</template>
        </span>
        <div>
          <h1>{{ suite?.suiteName || suiteCode }}</h1>
          <p>{{ suite?.description || '业务套件详情' }}</p>
        </div>
      </div>
      <div class="suite-stats">
        <div><strong>{{ objectTotal }}</strong><span>业务对象</span></div>
        <div><strong>{{ appTotal }}</strong><span>应用入口</span></div>
        <div><strong>{{ enabledAppCount }}</strong><span>本页启用</span></div>
      </div>
    </header>

    <div class="suite-content-grid">
      <main class="suite-main">
        <section class="suite-section">
          <div class="section-head">
            <div>
              <h2>业务对象</h2>
              <p>优先从对象进入关系、能力和标准业务入口。</p>
            </div>
            <n-button type="primary" @click="openObjectWizard">
              新建对象
            </n-button>
          </div>
          <n-spin :show="loadingObjects">
            <div v-if="objects.length" class="object-grid">
              <ObjectCard
                v-for="object in objects"
                :key="object.id"
                :object="object"
                @open="openObject"
                @design="openObjectDesigner"
                @toggle="toggleObject"
                @delete="deleteObject"
              />
            </div>
            <n-empty v-else-if="!loadingObjects" description="当前套件暂无业务对象" />
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
              <p>业务应用、看板、移动端和集成入口按场景展示。</p>
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
                @config="openEditor"
                @toggle="toggleApp"
                @delete="deleteApp"
              />
            </div>
            <n-empty v-else-if="!loadingApps" description="当前套件暂无应用入口" />
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
              <p>检查业务套件是否达到最小交付标准。</p>
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
    <BusinessObjectWizardDrawer
      v-model:show="objectWizardVisible"
      :suites="suite ? [suite] : []"
      :default-suite-code="suiteCode"
      @saved="handleObjectSaved"
    />
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessAppOpenInfo,
  businessAppPage,
  businessObjectPage,
  businessSuiteList,
  deleteBusinessApp,
  deleteBusinessObject,
  updateBusinessAppStatus,
  updateBusinessObjectStatus,
} from '@/api/business-app'
import IconRenderer from '@/components/IconRenderer.vue'
import { useTabStore } from '@/store'
import AppCard from './components/AppCard.vue'
import AppEditorDrawer from './components/AppEditorDrawer.vue'
import BusinessObjectWizardDrawer from './components/BusinessObjectWizardDrawer.vue'
import ObjectCard from './components/ObjectCard.vue'
import SuiteAcceptancePanel from './components/SuiteAcceptancePanel.vue'

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
const objectWizardVisible = ref(false)
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
const pageTitle = computed(() => suite.value?.suiteName || suiteCode.value || '业务套件详情')

onMounted(loadAll)

watch(pageTitle, (title) => {
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${import.meta.env.VITE_TITLE}`
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

function openObjectDesigner(object, panel = 'fields') {
  if (!object?.objectCode)
    return
  router.push({
    path: `/app-center/object/${object.objectCode}/designer`,
    query: {
      suiteCode: object.suiteCode,
      panel,
      returnTo: route.fullPath,
    },
  })
}

function openEditor(app) {
  editingApp.value = app ? { ...app } : { suiteCode: suiteCode.value }
  editorVisible.value = true
}

function openObjectWizard() {
  objectWizardVisible.value = true
}

async function handleObjectSaved(data) {
  await loadAll()
  openObjectDesigner(data, data?.designerPanel || 'fields')
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
    router.push('/app-center/integration')
    return
  }
  router.push(info.targetUrl)
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
      router.push('/app-center/mobile')
      break
    case 'OPEN_INTEGRATION_CENTER':
      router.push('/app-center/integration')
      break
    case 'OPEN_CHANNEL_CENTER':
      router.push('/app-center/mobile')
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

.suite-title h1 {
  font-size: 24px;
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
