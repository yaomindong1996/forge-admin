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
            新增集成入口
          </n-button>
        </n-space>
      </div>
      <h1>集成中心</h1>
      <p>统一登记开放接口、Webhook 和第三方平台入口，入口打开仍由后端校验状态和权限。</p>
    </header>

    <section class="integration-summary">
      <div v-for="item in summaryCards" :key="item.name" class="summary-card">
        <n-icon :component="item.icon" />
        <strong>{{ item.count }}</strong>
        <span>{{ item.name }}</span>
      </div>
    </section>

    <section class="entry-panel">
      <div class="panel-head">
        <div>
          <strong>集成应用定义</strong>
          <p>{{ total }} 个入口 · {{ enabledCount }} 个启用</p>
        </div>
        <n-select
          v-model:value="platformType"
          clearable
          class="mode-select"
          placeholder="集成类型"
          :options="platformOptions"
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
        <n-empty v-else-if="!loading" description="暂无集成应用入口" />
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
import { CloudUploadOutline, GitNetworkOutline, LinkOutline, PaperPlaneOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  businessAppDetail,
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
const platformType = ref(null)
const total = ref(0)
const pagination = ref({
  pageNum: 1,
  pageSize: 6,
})

const enabledCount = computed(() => apps.value.filter(item => item.status === 1).length)
const summaryCards = computed(() => [
  { name: '标准接口', count: countByPlatform('api'), icon: LinkOutline },
  { name: 'Webhook', count: countByPlatform('webhook'), icon: CloudUploadOutline },
  { name: '协同平台', count: countByPlatform('collaboration'), icon: PaperPlaneOutline },
  { name: '外部系统', count: countByPlatform('external'), icon: GitNetworkOutline },
])
const platformOptions = [
  { label: '标准接口', value: 'api' },
  { label: 'Webhook', value: 'webhook' },
  { label: '企微/飞书/钉钉', value: 'collaboration' },
  { label: '外部系统', value: 'external' },
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
      appType: 'INTEGRATION',
    })
    const records = res.data?.records || []
    apps.value = platformType.value
      ? records.filter(item => parseOptions(item.options).platformType === platformType.value)
      : records
    total.value = platformType.value ? apps.value.length : Number(res.data?.total || 0)
  }
  finally {
    loading.value = false
  }
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
    editingApp.value = {
      appType: 'INTEGRATION',
      entryMode: 'API',
      entryUrl: '/app-center/integration',
      appCode: '',
      appName: '',
      status: 1,
    }
  }
  editorVisible.value = true
}

async function openApp(app) {
  const res = await businessAppOpenInfo(app.id)
  const info = res.data || {}
  if (!info.canOpen) {
    message.warning(info.message || '集成入口暂不可打开')
    return
  }
  if (info.openType === 'EXTERNAL' || /^https?:\/\//i.test(info.targetUrl || '')) {
    window.open(info.targetUrl, '_blank', 'noopener,noreferrer')
    return
  }
  router.push(info.targetUrl || '/app-center/integration')
}

async function toggleApp(app) {
  await updateBusinessAppStatus(app.id, app.status === 1 ? 0 : 1)
  message.success(app.status === 1 ? '集成入口已停用' : '集成入口已启用')
  await loadApps()
}

function deleteApp(app) {
  window.$dialog?.warning({
    title: '删除集成入口',
    content: `确定删除“${app.appName || app.appCode}”吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessApp(app.id)
      message.success('集成入口已删除')
      await loadAll()
    },
  })
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.pageNum = 1
  loadApps()
}

function countByPlatform(platform) {
  return apps.value.filter(item => parseOptions(item.options).platformType === platform).length
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

.integration-summary {
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
  width: 190px;
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
  .integration-summary,
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

  .integration-summary,
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .mode-select {
    width: 100%;
  }
}
</style>
