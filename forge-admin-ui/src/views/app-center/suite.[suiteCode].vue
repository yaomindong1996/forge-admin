<template>
  <div class="suite-detail-page">
    <header class="suite-head">
      <n-button text @click="router.push('/app-center')">
        返回应用中心
      </n-button>
      <div class="suite-title">
        <span>{{ suiteInitial }}</span>
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
          <ObjectCard v-for="object in objects" :key="object.id" :object="object" @open="openObject" />
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
  updateBusinessAppStatus,
} from '@/api/business-app'
import { useTabStore } from '@/store'
import AppCard from './components/AppCard.vue'
import AppEditorDrawer from './components/AppEditorDrawer.vue'
import BusinessObjectWizardDrawer from './components/BusinessObjectWizardDrawer.vue'
import ObjectCard from './components/ObjectCard.vue'

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

function openEditor(app) {
  editingApp.value = app ? { ...app } : { suiteCode: suiteCode.value }
  editorVisible.value = true
}

function openObjectWizard() {
  objectWizardVisible.value = true
}

async function handleObjectSaved() {
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

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.object-grid,
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.card-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
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
