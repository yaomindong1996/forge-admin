<template>
  <div class="entries-panel">
    <header class="panel-heading">
      <div>
        <h2>页面入口</h2>
        <p>集中管理表单、列表、详情、外部页面和移动端等访问入口。</p>
      </div>
      <n-button type="primary" @click="openCreate">
        新增访问入口
      </n-button>
    </header>

    <n-spin :show="loading">
      <n-empty v-if="!loading && !entries.length" class="entries-empty" description="当前应用还没有访问入口">
        <template #extra>
          <n-button type="primary" @click="openCreate">
            创建第一个入口
          </n-button>
        </template>
      </n-empty>
      <div v-else class="entry-table">
        <div class="entry-row entry-row-head">
          <span>入口名称</span>
          <span>类型</span>
          <span>业务对象</span>
          <span>状态</span>
          <span>最近更新</span>
          <span>操作</span>
        </div>
        <div v-for="item in entries" :key="item.id" class="entry-row">
          <div class="entry-name">
            <strong :title="entryDisplayName(item)">{{ entryDisplayName(item) }}</strong>
            <small v-if="item.description">{{ item.description }}</small>
          </div>
          <DictTag dict-type="ai_business_app_entry_mode" :value="item.entryMode" :bordered="false" />
          <span>{{ item.objectName || item.objectCode || '-' }}</span>
          <DictTag dict-type="sys_enable_disable" :value="item.status" :bordered="false" />
          <span>{{ item.updateTime || '-' }}</span>
          <div class="entry-actions">
            <a class="cursor-pointer text-primary" @click="openEntry(item)">打开</a>
            <a v-if="item.objectCode" class="cursor-pointer text-info" @click="openEntryDesigner(item)">设计页面</a>
            <a class="cursor-pointer text-primary" @click="openEdit(item)">编辑</a>
            <a
              :class="item.status === 1 ? 'text-warning' : 'text-success'"
              class="cursor-pointer"
              @click="toggleStatus(item)"
            >
              {{ item.status === 1 ? '停用' : '启用' }}
            </a>
          </div>
        </div>
      </div>
    </n-spin>

    <AppEntryWizard
      v-model:show="wizardVisible"
      :app="wizardEntry"
      :suites="workspaceSuites"
      :application-id="application?.id"
      :default-suite-code="application?.suiteCode"
      :objects="applicationObjects"
      lock-suite
      @saved="handleSaved"
    />
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { businessAppList, updateBusinessAppStatus } from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'
import AppEntryWizard from '../components/AppEntryWizard.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialEntries: {
    type: Array,
    default: null,
  },
  applicationObjects: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['changed', 'openDesigner'])
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const entries = ref([])
const wizardVisible = ref(false)
const editingEntry = ref(null)

const workspaceSuites = computed(() => props.application
  ? [{
      suiteCode: props.application.suiteCode,
      suiteName: props.application.suiteName || props.application.suiteCode,
    }]
  : [])

const wizardEntry = computed(() => editingEntry.value || {
  applicationId: props.application?.id,
  suiteCode: props.application?.suiteCode,
})

watch([
  () => props.application?.id,
  () => props.initialEntries,
], ([applicationId, initialEntries]) => {
  if (!applicationId)
    return
  if (Array.isArray(initialEntries)) {
    entries.value = [...initialEntries]
    return
  }
  loadEntries()
}, { immediate: true })

async function loadEntries() {
  if (!props.application?.id)
    return
  loading.value = true
  try {
    const response = await businessAppList({ applicationId: props.application.id })
    entries.value = response.data || []
  }
  finally {
    loading.value = false
  }
}

function openCreate() {
  editingEntry.value = null
  wizardVisible.value = true
}

function openEdit(item) {
  editingEntry.value = { ...item, applicationId: props.application?.id }
  wizardVisible.value = true
}

function openEntry(item) {
  const route = router.resolve(`/app-center/app/${item.id}`)
  window.open(route.href, '_blank', 'noopener,noreferrer')
}

function openEntryDesigner(item) {
  const runtimeOpenMode = String(item.runtimeOpenMode || '').toUpperCase()
  emit('openDesigner', {
    objectCode: item.objectCode,
    panel: runtimeOpenMode === 'LIST' ? 'list' : 'form',
    detailTab: runtimeOpenMode === 'DETAIL' ? 'detail' : 'form',
  })
}

async function toggleStatus(item) {
  await updateBusinessAppStatus(item.id, item.status === 1 ? 0 : 1)
  message.success(item.status === 1 ? '访问入口已停用' : '访问入口已启用')
  await loadEntries()
  emit('changed')
}

async function handleSaved() {
  await loadEntries()
  emit('changed')
}

function entryDisplayName(item = {}) {
  const appName = String(item.appName || '').trim()
  const appCode = String(item.appCode || '').trim()
  const technicalName = !appName || appName === appCode || /^[A-Z][A-Z0-9_]*$/.test(appName)
  if (!technicalName)
    return appName
  if (item.objectName)
    return `${item.objectName}入口`
  return '业务访问入口'
}
</script>

<style scoped>
.entries-panel {
  display: grid;
  gap: 16px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.panel-heading h2 {
  margin: 0;
  font-size: 18px;
}

.panel-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
}

.entries-empty {
  min-height: 260px;
  padding-top: 64px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.entry-table {
  overflow-x: auto;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(190px, 1.4fr) 110px minmax(140px, 1fr) 80px 160px 220px;
  gap: 12px;
  align-items: center;
  min-width: 980px;
  min-height: 58px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.entry-row:last-child {
  border-bottom: 0;
}

.entry-row-head {
  min-height: 38px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-weight: 600;
}

.entry-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.entry-name strong {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-name small {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-actions {
  display: flex;
  gap: 12px;
}
</style>
