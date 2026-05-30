<template>
  <div class="center-shell">
    <header>
      <div class="center-head-row">
        <n-button text @click="router.push('/app-center')">
          返回应用中心
        </n-button>
        <n-space :wrap="true">
          <n-button secondary @click="loadAll">
            刷新
          </n-button>
          <n-button type="primary" @click="openEditor(null)">
            新增移动入口
          </n-button>
        </n-space>
      </div>
      <h1>移动端中心</h1>
      <p>登记 H5、移动待办、移动审批和移动业务入口，统一纳入业务套件和打开校验。</p>
    </header>

    <section class="mobile-summary">
      <div v-for="item in summaryCards" :key="item.name" class="summary-card">
        <n-icon :component="item.icon" />
        <strong>{{ item.count }}</strong>
        <span>{{ item.name }}</span>
      </div>
    </section>

    <section class="entry-panel">
      <div class="panel-head">
        <div>
          <strong>移动应用入口</strong>
          <p>{{ total }} 个入口 · {{ enabledCount }} 个启用</p>
        </div>
        <n-select
          v-model:value="entryMode"
          clearable
          class="mode-select"
          placeholder="入口模式"
          :options="entryModeOptions"
          @update:value="loadApps"
        />
      </div>
      <n-spin :show="loading">
        <div v-if="apps.length" class="entry-grid">
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
        <n-empty v-else-if="!loading" description="暂无移动应用入口" />
      </n-spin>
      <div v-if="total > pagination.pageSize" class="entry-pagination">
        <n-pagination
          v-model:page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :item-count="total"
          :page-sizes="[6, 12, 24]"
          show-size-picker
          @update:page="loadApps"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </section>

    <AppEditorDrawer
      v-model:show="editorVisible"
      :app="editingApp"
      :suites="suites"
      @saved="loadAll"
    />
  </div>
</template>

<script setup>
import { ClipboardOutline, ListOutline, PhonePortraitOutline, WalkOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  businessAppOpenInfo,
  businessAppPage,
  businessSuiteSummary,
  deleteBusinessApp,
  updateBusinessAppStatus,
} from '@/api/business-app'
import AppCard from './components/AppCard.vue'
import AppEditorDrawer from './components/AppEditorDrawer.vue'

const router = useRouter()
const message = useMessage()
const suites = ref([])
const apps = ref([])
const loading = ref(false)
const editorVisible = ref(false)
const editingApp = ref(null)
const entryMode = ref(null)
const total = ref(0)
const pagination = ref({
  pageNum: 1,
  pageSize: 6,
})

const enabledCount = computed(() => apps.value.filter(item => item.status === 1).length)
const summaryCards = computed(() => [
  { name: 'H5 入口', count: countByMode('H5'), icon: PhonePortraitOutline },
  { name: '移动待办', count: countByScene('todo'), icon: ListOutline },
  { name: '移动审批', count: countByScene('approval'), icon: ClipboardOutline },
  { name: '移动业务', count: countByScene('business'), icon: WalkOutline },
])
const entryModeOptions = [
  { label: 'H5', value: 'H5' },
  { label: '内部路由', value: 'ROUTE' },
  { label: '外部链接', value: 'EXTERNAL' },
]

onMounted(loadAll)

async function loadAll() {
  await Promise.all([loadSuites(), loadApps()])
}

async function loadSuites() {
  const res = await businessSuiteSummary()
  suites.value = res.data || []
}

async function loadApps() {
  loading.value = true
  try {
    const res = await businessAppPage({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      appType: 'MOBILE',
      entryMode: entryMode.value,
    })
    apps.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  }
  finally {
    loading.value = false
  }
}

function openEditor(app) {
  editingApp.value = app
    ? { ...app }
    : {
        appType: 'MOBILE',
        entryMode: 'H5',
        appCode: '',
        appName: '',
        status: 1,
      }
  editorVisible.value = true
}

async function openApp(app) {
  const res = await businessAppOpenInfo(app.id)
  const info = res.data || {}
  if (!info.canOpen) {
    message.warning(info.message || '移动入口暂不可打开')
    return
  }
  if (info.openType === 'EXTERNAL' || info.openType === 'H5') {
    window.open(info.targetUrl, '_blank', 'noopener,noreferrer')
    return
  }
  router.push(info.targetUrl)
}

async function toggleApp(app) {
  await updateBusinessAppStatus(app.id, app.status === 1 ? 0 : 1)
  message.success(app.status === 1 ? '移动入口已停用' : '移动入口已启用')
  await loadApps()
}

function deleteApp(app) {
  window.$dialog?.warning({
    title: '删除移动入口',
    content: `确定删除“${app.appName || app.appCode}”吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessApp(app.id)
      message.success('移动入口已删除')
      await loadAll()
    },
  })
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.pageNum = 1
  loadApps()
}

function countByMode(mode) {
  return apps.value.filter(item => item.entryMode === mode).length
}

function countByScene(scene) {
  return apps.value.filter((item) => {
    const options = parseOptions(item.options)
    return options.mobileScene === scene
  }).length
}

function parseOptions(value) {
  try {
    return value ? JSON.parse(value) : {}
  }
  catch {
    return {}
  }
}
</script>

<style scoped>
@import './shared-center.css';

.center-head-row,
.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.mobile-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card,
.entry-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.summary-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 86px;
  padding: 14px;
}

.summary-card .n-icon {
  grid-row: span 2;
  width: 42px;
  height: 42px;
  color: #2563eb;
  font-size: 24px;
}

.summary-card strong,
.summary-card span {
  display: block;
}

.summary-card strong {
  color: #111827;
  font-size: 22px;
  line-height: 1;
}

.summary-card span {
  color: #6b7280;
  font-size: 12px;
}

.entry-panel {
  padding: 16px;
}

.panel-head {
  margin-bottom: 14px;
}

.panel-head strong {
  color: #111827;
  font-size: 15px;
}

.panel-head p {
  margin: 4px 0 0;
}

.mode-select {
  width: 180px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.entry-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 960px) {
  .mobile-summary,
  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .center-head-row,
  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .mobile-summary,
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .mode-select {
    width: 100%;
  }
}
</style>
