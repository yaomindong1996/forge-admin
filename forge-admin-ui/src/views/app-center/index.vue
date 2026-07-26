<template>
  <div class="app-center-page">
    <section class="app-center-layout">
      <aside class="suite-nav">
        <div class="suite-nav-head">
          <div>
            <strong>业务域</strong>
            <span>{{ suites.length }} 个目录</span>
          </div>
          <n-button quaternary circle size="small" aria-label="新建业务域" @click="openSuiteEditor(null)">
            <template #icon>
              <NIcon><AddOutline /></NIcon>
            </template>
          </n-button>
        </div>

        <div class="suite-list" :class="{ refreshing: loadingSuites }">
          <button
            type="button"
            class="suite-item all-suite"
            :class="{ active: !suiteCode }"
            @click="selectSuite(null)"
          >
            <span class="suite-icon all">
              <NIcon><AppsOutline /></NIcon>
            </span>
            <span class="suite-copy">
              <strong>全部应用</strong>
              <small>{{ allApplicationTotal }} 个应用</small>
            </span>
          </button>

          <n-skeleton v-if="loadingSuites && !suites.length" text :repeat="6" />

          <div
            v-for="row in suiteTreeRows"
            :key="row.suite.id || row.suite.suiteCode"
            class="suite-row"
            :style="{ '--suite-indent': `${row.level * 15}px` }"
          >
            <button
              v-if="row.hasChildren"
              type="button"
              class="suite-toggle"
              :aria-label="isSuiteExpanded(row.suite) ? '收起子业务域' : '展开子业务域'"
              @click="toggleSuiteExpanded(row.suite)"
            >
              <NIcon>
                <ChevronDownOutline v-if="isSuiteExpanded(row.suite)" />
                <ChevronForwardOutline v-else />
              </NIcon>
            </button>
            <span v-else class="suite-toggle-placeholder" />

            <button
              type="button"
              class="suite-item"
              :class="{ active: suiteCode === row.suite.suiteCode }"
              @click="selectSuite(row.suite)"
            >
              <span class="suite-icon">
                <IconRenderer v-if="row.suite.icon" :icon="row.suite.icon" :size="17" />
                <template v-else>{{ suiteInitial(row.suite) }}</template>
              </span>
              <span class="suite-copy">
                <strong>{{ row.suite.suiteName || row.suite.suiteCode }}</strong>
                <small>{{ suiteMetaText(row.suite) }}</small>
              </span>
            </button>

            <n-dropdown
              trigger="click"
              :options="suiteActionOptions(row.suite)"
              @select="key => handleSuiteAction(key, row.suite)"
            >
              <n-button quaternary circle size="tiny" class="suite-more" aria-label="业务域操作">
                <template #icon>
                  <NIcon><EllipsisVertical /></NIcon>
                </template>
              </n-button>
            </n-dropdown>
          </div>
        </div>

        <div class="suite-nav-foot">
          <span>对象和访问入口已移入应用上下文</span>
        </div>
      </aside>

      <main class="application-workspace">
        <section class="application-panel">
          <div class="panel-toolbar">
            <ApplicationFilterBar
              class="toolbar-filters"
              v-model:keyword="keyword"
              v-model:design-status="designStatus"
              v-model:status="status"
              :loading="loadingApplications"
              @search="applyKeywordFilter"
              @refresh="loadApplications"
            />
            <div class="toolbar-actions">
              <span class="result-summary">{{ resultRangeText }}</span>
              <n-dropdown trigger="click" :options="newApplicationModeOptions" @select="handleNewApplicationModeSelect">
                <n-button type="primary" aria-label="新建应用" title="新建应用">
                  <template #icon>
                    <NIcon><AddOutline /></NIcon>
                  </template>
                  <span class="new-application-button-label">新建应用 <NIcon><ChevronDownOutline /></NIcon></span>
                </n-button>
              </n-dropdown>
            </div>
          </div>

          <div class="application-table-region">
            <div
              v-if="applications.length"
              class="table-scroll"
              tabindex="0"
              aria-label="业务应用卡片列表，可纵向滚动"
            >
              <ApplicationTable
                :applications="applications"
                @enter="openApplication"
                @edit="openApplicationEditor"
                @code="openApplicationCode"
                @publish="openApplicationPublish"
                @toggle="toggleApplication"
                @delete="removeApplication"
              />
            </div>

            <n-empty
              v-else-if="!loadingApplications"
              class="application-empty"
              description="当前条件下还没有应用"
            >
              <template #extra>
                <n-space>
                  <n-button type="primary" @click="openApplicationEditor(null, 'BLANK')">
                    新建页面应用
                  </n-button>
                  <n-button secondary @click="openApplicationEditor(null, 'TEMPLATE_SINGLE_CRUD')">
                    传统业务对象应用
                  </n-button>
                </n-space>
              </template>
            </n-empty>

            <div v-if="loadingApplications" class="application-loading-mask" aria-live="polite">
              <n-spin size="small" description="正在加载应用" />
            </div>
          </div>

          <footer class="panel-pagination">
            <div class="pagination-controls">
              <n-pagination
                :page="pageNum"
                :page-size="pageSize"
                :item-count="total"
                :page-slot="5"
                @update:page="changePage"
              />
              <n-select
                class="page-size-select"
                size="small"
                :value="pageSize"
                :options="pageSizeOptions"
                aria-label="每页应用数量"
                @update:value="changePageSize"
              />
            </div>
          </footer>
        </section>
      </main>
    </section>

    <ApplicationEditorDrawer
      v-model:show="applicationEditorVisible"
      :application="editingApplication"
      :suites="suites"
      :default-suite-code="suiteCode"
      :default-initialize-mode="editorInitializeMode"
      @saved="handleApplicationSaved"
    />

    <SuiteEditorDrawer
      v-model:show="suiteEditorVisible"
      :suite="editingSuite"
      :suites="suites"
      @saved="handleSuiteSaved"
    />

    <AppCodePanel
      v-model:show="applicationCodeVisible"
      scope="APPLICATION"
      :app="codeApplication"
    />
  </div>
</template>

<script setup>
import {
  AddOutline,
  AppsOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  CubeOutline,
  EllipsisVertical,
  GridOutline,
  ServerOutline,
} from '@vicons/ionicons5'
import { NIcon, useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessSuiteSummary,
  deleteBusinessSuite,
  updateBusinessSuiteStatus,
} from '@/api/business-app'
import {
  businessApplicationDetail,
  businessApplicationPage,
  deleteBusinessApplication,
  updateBusinessApplicationStatus,
} from '@/api/business-application'
import IconRenderer from '@/components/IconRenderer.vue'
import ApplicationFilterBar from './components/ApplicationFilterBar.vue'
import ApplicationTable from './components/ApplicationTable.vue'

const ApplicationEditorDrawer = defineAsyncComponent(() => import('./components/ApplicationEditorDrawer.vue'))
const AppCodePanel = defineAsyncComponent(() => import('./components/AppCodePanel.vue'))
const SuiteEditorDrawer = defineAsyncComponent(() => import('./components/SuiteEditorDrawer.vue'))

const route = useRoute()
const router = useRouter()
const message = useMessage()

const keyword = ref(queryText(route.query.keyword))
const suiteCode = ref(queryText(route.query.suiteCode) || null)
const designStatus = ref(queryText(route.query.designStatus) || null)
const status = ref(queryStatus(route.query.status))
const pageNum = ref(queryPositiveInt(route.query.pageNum, 1))
const pageSize = ref(queryPositiveInt(route.query.pageSize, 20))
const total = ref(0)
const suites = ref([])
const applications = ref([])
const loadingSuites = ref(false)
const loadingApplications = ref(false)
const collapsedSuiteIds = ref(new Set())
const applicationEditorVisible = ref(false)
const editingApplication = ref(null)
const applicationCodeVisible = ref(false)
const codeApplication = ref(null)
const editorInitializeMode = ref('BLANK')
const suiteEditorVisible = ref(false)
const editingSuite = ref(null)
let applicationRequestVersion = 0

const pageSizeOptions = [
  { label: '10 条/页', value: 10 },
  { label: '20 条/页', value: 20 },
  { label: '50 条/页', value: 50 },
]

function renderNewApplicationModeIcon(icon) {
  return () => h(NIcon, { size: 16 }, { default: () => h(icon) })
}

const newApplicationModeOptions = [
  {
    label: '页面应用（直接设计页面）',
    key: 'BLANK',
    icon: renderNewApplicationModeIcon(AppsOutline),
  },
  {
    label: '传统业务对象（从模板生成）',
    key: 'TEMPLATE_SINGLE_CRUD',
    icon: renderNewApplicationModeIcon(GridOutline),
  },
  { type: 'divider', key: 'mode-divider' },
  {
    label: '挂接已有业务对象（不新建字段）',
    key: 'EXISTING_OBJECT',
    icon: renderNewApplicationModeIcon(CubeOutline),
  },
  {
    label: '从数据库表导入业务对象',
    key: 'DATABASE_TABLE',
    icon: renderNewApplicationModeIcon(ServerOutline),
  },
]

const allApplicationTotal = computed(() => suites.value.reduce(
  (sum, suite) => sum + Number(suite.applicationCount || 0),
  0,
))
const suiteById = computed(() => {
  const result = new Map()
  suites.value.forEach((suite) => {
    if (suite?.id != null)
      result.set(String(suite.id), suite)
  })
  return result
})
const suiteChildrenMap = computed(() => {
  const result = new Map()
  ;[...suites.value].sort(compareSuites).forEach((suite) => {
    const parentKey = normalizeSuiteParentKey(suite)
    if (!result.has(parentKey))
      result.set(parentKey, [])
    result.get(parentKey).push(suite)
  })
  return result
})
const suiteTreeRows = computed(() => flattenSuiteRows('__root__', 0, new Set()))
const suiteApplicationTotals = computed(() => {
  const totals = new Map()
  const calculate = (suite, visited = new Set()) => {
    if (suite?.id == null)
      return Number(suite?.applicationCount || 0)
    const suiteKey = String(suite.id)
    if (visited.has(suiteKey))
      return 0
    const nextVisited = new Set(visited)
    nextVisited.add(suiteKey)
    const childTotal = (suiteChildrenMap.value.get(suiteKey) || [])
      .reduce((sum, child) => sum + calculate(child, nextVisited), 0)
    const total = Number(suite.applicationCount || 0) + childTotal
    totals.set(suiteKey, total)
    return total
  }
  suites.value.forEach(suite => calculate(suite))
  return totals
})
const resultRangeText = computed(() => {
  if (!total.value)
    return '0 个应用'
  const start = (pageNum.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, pageNum.value * pageSize.value)
  return `${start}-${end} / ${total.value} 个应用`
})

watch([designStatus, status], () => {
  pageNum.value = 1
  syncRouteQuery()
  loadApplications()
})

onMounted(async () => {
  await Promise.all([loadSuites(), loadApplications()])
})

async function loadSuites() {
  loadingSuites.value = true
  try {
    const response = await businessSuiteSummary()
    suites.value = response.data || []
  }
  finally {
    loadingSuites.value = false
  }
}

async function loadApplications() {
  const requestVersion = ++applicationRequestVersion
  loadingApplications.value = true
  try {
    const response = await businessApplicationPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: trimToUndefined(keyword.value),
      suiteCode: suiteCode.value || undefined,
      designStatus: designStatus.value || undefined,
      status: status.value === null ? undefined : Number(status.value),
    })
    if (requestVersion !== applicationRequestVersion)
      return
    applications.value = response.data?.records || []
    total.value = Number(response.data?.total || 0)
  }
  finally {
    if (requestVersion === applicationRequestVersion)
      loadingApplications.value = false
  }
}

function applyKeywordFilter() {
  pageNum.value = 1
  syncRouteQuery()
  loadApplications()
}

function selectSuite(suite) {
  suiteCode.value = suite?.suiteCode || null
  pageNum.value = 1
  syncRouteQuery()
  loadApplications()
}

function changePage(value) {
  pageNum.value = value
  syncRouteQuery()
  loadApplications()
}

function changePageSize(value) {
  pageSize.value = value
  pageNum.value = 1
  syncRouteQuery()
  loadApplications()
}

function syncRouteQuery() {
  router.replace({
    query: {
      keyword: trimToUndefined(keyword.value),
      suiteCode: suiteCode.value || undefined,
      designStatus: designStatus.value || undefined,
      status: status.value === null ? undefined : status.value,
      pageNum: pageNum.value > 1 ? pageNum.value : undefined,
      pageSize: pageSize.value !== 20 ? pageSize.value : undefined,
    },
  })
}

async function openApplicationEditor(application, initializeMode = 'BLANK') {
  editorInitializeMode.value = initializeMode
  if (application?.id) {
    try {
      const response = await businessApplicationDetail(application.id)
      editingApplication.value = { ...application, ...(response.data || {}) }
    }
    catch {
      editingApplication.value = { ...application }
    }
  }
  else {
    editingApplication.value = null
  }
  applicationEditorVisible.value = true
}

function handleNewApplicationModeSelect(mode) {
  openApplicationEditor(null, mode)
}

async function handleApplicationSaved(result) {
  await Promise.all([loadSuites(), loadApplications()])
  if (result?.created && !result.initializationWarning && result.application?.applicationCode) {
    const location = {
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: result.application.applicationCode },
      query: { edit: '1', fresh: '1' },
    }
    const target = router.resolve(location)
    window.open(target.href, '_blank', 'noopener,noreferrer')
  }
}

function openApplication(application, newTab = true, initializeMode = null) {
  if (!application?.applicationCode)
    return
  const location = {
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: application.applicationCode },
    query: initializeMode && initializeMode !== 'BLANK'
      ? { section: 'objects' }
      : undefined,
  }
  if (!newTab) {
    router.push(location)
    return
  }
  const target = router.resolve(location)
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openApplicationPublish(application) {
  if (!application?.applicationCode)
    return
  const target = router.resolve({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: application.applicationCode },
    query: { section: 'releases', publish: '1' },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openApplicationCode(application) {
  if (!application?.id)
    return
  codeApplication.value = { ...application }
  applicationCodeVisible.value = true
}

function toggleApplication(application) {
  const nextStatus = Number(application.status) === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  confirmAction({
    title: `${action}应用`,
    content: `确定${action}“${application.applicationName || application.applicationCode}”吗？停用不会覆盖设计和发布状态。`,
    positiveText: action,
    async onConfirm() {
      await updateBusinessApplicationStatus(application.id, nextStatus)
      message.success(`应用已${action}`)
      await Promise.all([loadSuites(), loadApplications()])
    },
  })
}

function removeApplication(application) {
  confirmAction({
    title: '删除应用',
    content: `确定删除“${application.applicationName || application.applicationCode}”吗？业务对象不会被删除；存在启用入口时后端会阻止操作。`,
    positiveText: '删除',
    async onConfirm() {
      await deleteBusinessApplication(application.id)
      message.success('应用已删除')
      if (applications.value.length === 1 && pageNum.value > 1)
        pageNum.value -= 1
      await Promise.all([loadSuites(), loadApplications()])
    },
  })
}

function suiteActionOptions(suite) {
  return [
    { label: '新增子业务域', key: 'create-child' },
    { label: '编辑业务域', key: 'edit' },
    { label: Number(suite.status) === 1 ? '停用业务域' : '启用业务域', key: 'toggle' },
    { type: 'divider', key: 'divider' },
    { label: '删除业务域', key: 'delete' },
  ]
}

function handleSuiteAction(key, suite) {
  if (key === 'create-child')
    openSuiteEditor({ parentId: suite.id })
  else if (key === 'edit')
    openSuiteEditor(suite)
  else if (key === 'toggle')
    toggleSuite(suite)
  else if (key === 'delete')
    removeSuite(suite)
}

function openSuiteEditor(suite) {
  editingSuite.value = suite ? { ...suite } : null
  suiteEditorVisible.value = true
}

async function handleSuiteSaved(payload) {
  suiteCode.value = payload?.suiteCode || suiteCode.value
  pageNum.value = 1
  syncRouteQuery()
  await Promise.all([loadSuites(), loadApplications()])
}

function toggleSuite(suite) {
  const nextStatus = Number(suite.status) === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  confirmAction({
    title: `${action}业务域`,
    content: `确定${action}“${suite.suiteName || suite.suiteCode}”吗？现有应用和入口数据不会被删除。`,
    positiveText: action,
    async onConfirm() {
      await updateBusinessSuiteStatus(suite.id, nextStatus)
      message.success(`业务域已${action}`)
      await loadSuites()
    },
  })
}

function removeSuite(suite) {
  confirmAction({
    title: '删除业务域',
    content: `确定删除“${suite.suiteName || suite.suiteCode}”吗？存在子域、应用、对象或入口时后端会阻止操作。`,
    positiveText: '删除',
    async onConfirm() {
      await deleteBusinessSuite(suite.id)
      if (suiteCode.value === suite.suiteCode)
        suiteCode.value = null
      message.success('业务域已删除')
      pageNum.value = 1
      syncRouteQuery()
      await Promise.all([loadSuites(), loadApplications()])
    },
  })
}

function confirmAction({ title, content, positiveText, onConfirm }) {
  if (!window.$dialog?.warning) {
    onConfirm()
    return
  }
  window.$dialog.warning({
    title,
    content,
    positiveText,
    negativeText: '取消',
    onPositiveClick: onConfirm,
  })
}

function normalizeSuiteParentKey(suite) {
  if (!suite?.parentId)
    return '__root__'
  const parentKey = String(suite.parentId)
  return suiteById.value.has(parentKey) ? parentKey : '__root__'
}

function flattenSuiteRows(parentKey, level, visited) {
  return (suiteChildrenMap.value.get(parentKey) || []).flatMap((suite) => {
    if (suite?.id == null)
      return []
    const suiteKey = String(suite.id)
    if (visited.has(suiteKey))
      return []
    const nextVisited = new Set(visited)
    nextVisited.add(suiteKey)
    const hasChildren = (suiteChildrenMap.value.get(suiteKey) || []).length > 0
    const childRows = hasChildren && isSuiteExpanded(suite)
      ? flattenSuiteRows(suiteKey, level + 1, nextVisited)
      : []
    return [{ suite, level, hasChildren }, ...childRows]
  })
}

function isSuiteExpanded(suite) {
  return suite?.id == null || !collapsedSuiteIds.value.has(String(suite.id))
}

function toggleSuiteExpanded(suite) {
  if (suite?.id == null)
    return
  const suiteId = String(suite.id)
  const next = new Set(collapsedSuiteIds.value)
  if (next.has(suiteId))
    next.delete(suiteId)
  else
    next.add(suiteId)
  collapsedSuiteIds.value = next
}

function suiteMetaText(suite) {
  const applicationCount = suite?.id == null
    ? Number(suite?.applicationCount || 0)
    : Number(suiteApplicationTotals.value.get(String(suite.id)) || 0)
  const childCount = (suiteChildrenMap.value.get(String(suite.id)) || []).length
  return childCount ? `${applicationCount} 应用 · ${childCount} 子域` : `${applicationCount} 个应用`
}

function suiteInitial(suite) {
  return String(suite?.suiteName || suite?.suiteCode || '域').trim().slice(0, 1).toUpperCase()
}

function compareSuites(left, right) {
  const sortCompare = Number(left?.sortOrder || 0) - Number(right?.sortOrder || 0)
  if (sortCompare !== 0)
    return sortCompare
  return String(left?.suiteName || left?.suiteCode || '')
    .localeCompare(String(right?.suiteName || right?.suiteCode || ''), 'zh-CN')
}

function queryText(value) {
  return Array.isArray(value) ? String(value[0] || '') : String(value || '')
}

function queryStatus(value) {
  const text = queryText(value)
  return text === '0' || text === '1' ? Number(text) : null
}

function queryPositiveInt(value, fallback) {
  const number = Number(queryText(value))
  return Number.isInteger(number) && number > 0 ? number : fallback
}

function trimToUndefined(value) {
  const text = String(value || '').trim()
  return text || undefined
}
</script>

<style scoped>
.app-center-page {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  background: var(--n-color, #fff);
  color: var(--n-text-color, #111827);
}

.app-center-layout {
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: var(--n-color, #fff);
}

.suite-nav {
  --suite-panel-bg: var(--n-color, var(--bg-primary, #fff));
  --suite-panel-head: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
  --suite-panel-border: var(--n-border-color, var(--border-light, #e5e6eb));
  --suite-item-hover: color-mix(in srgb, var(--suite-accent) 4%, var(--suite-panel-bg));
  --suite-item-active: color-mix(
    in srgb,
    var(--n-primary-color, var(--primary-color, #165dff)) 6%,
    var(--suite-panel-bg)
  );
  --suite-accent: var(--n-primary-color, var(--primary-color, #165dff));
  --suite-accent-strong: var(--n-primary-color-hover, var(--primary-color-hover, #0e42d2));
  --suite-muted: var(--n-text-color-3, var(--text-tertiary, #86909c));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-right: 1px solid var(--suite-panel-border);
  background: var(--suite-panel-bg);
}

.suite-nav-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 60px;
  padding: 12px 14px 12px 18px;
  border-bottom: 1px solid var(--suite-panel-border);
  background: var(--suite-panel-head);
}

.suite-nav-head > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.suite-nav-head strong {
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 13px;
  font-weight: 650;
}

.suite-nav-head span,
.suite-nav-foot {
  color: var(--suite-muted);
  font-size: 11px;
}

.suite-nav-head :deep(.n-button) {
  color: var(--suite-accent);
}

.suite-list {
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px 8px 16px;
  scrollbar-color: color-mix(in srgb, var(--suite-accent) 42%, transparent) var(--suite-panel-head);
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  transition: opacity 0.16s ease;
  overscroll-behavior: contain;
}

.suite-list::-webkit-scrollbar {
  width: 8px;
}

.suite-list::-webkit-scrollbar-track {
  background: var(--suite-panel-head);
}

.suite-list::-webkit-scrollbar-thumb {
  border: 2px solid var(--suite-panel-head);
  border-radius: 999px;
  background: color-mix(in srgb, var(--suite-accent) 42%, transparent);
}

.suite-list.refreshing {
  opacity: 0.74;
}

.suite-row {
  position: relative;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) 24px;
  align-items: center;
  padding-left: var(--suite-indent);
}

.suite-item {
  position: relative;
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 38px;
  padding: 4px 7px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.all-suite {
  margin-bottom: 8px;
}

.suite-item:hover,
.suite-item.active {
  background: var(--suite-item-hover);
}

.suite-item.active {
  color: var(--suite-accent-strong);
  background: var(--suite-item-active);
}

.suite-icon {
  display: inline-flex;
  flex: 0 0 20px;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 4px;
  color: color-mix(in srgb, var(--suite-accent) 76%, var(--suite-muted));
  background: transparent;
  font-size: 11px;
  font-weight: 650;
}

.suite-icon.all {
  color: var(--suite-accent-strong);
}

.suite-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
}

.suite-copy strong,
.suite-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suite-copy strong {
  font-size: 12px;
  font-weight: 500;
}

.suite-item.active .suite-copy strong {
  font-weight: 650;
}

.suite-copy small {
  color: var(--suite-muted);
  font-size: 10px;
}

.suite-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 24px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--suite-muted);
  cursor: pointer;
}

.suite-toggle-placeholder {
  width: 18px;
}

.suite-more {
  opacity: 0;
}

.suite-row:hover .suite-more,
.suite-more:focus-visible {
  opacity: 1;
}

.suite-nav-foot {
  padding: 12px 16px;
  border-top: 1px solid var(--suite-panel-border);
  background: var(--suite-panel-head);
  line-height: 1.5;
}

.application-workspace {
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--n-color, #fff);
}

.application-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 0;
  margin: 0;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: var(--n-color, #fff);
}

.panel-toolbar,
.panel-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
}

.panel-toolbar {
  flex-wrap: wrap;
  border-bottom: 1px solid var(--n-border-color, #e5e7eb);
}

.toolbar-filters {
  flex: 1 1 680px;
}

.toolbar-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.new-application-button-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.new-application-button-label :deep(.n-icon) {
  font-size: 13px;
}

.result-summary {
  flex: 0 0 auto;
  color: var(--n-text-color-3, #6b7280);
  font-size: 11px;
}

.application-table-region {
  position: relative;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.table-scroll {
  position: relative;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: stable;
  scrollbar-color: color-mix(in srgb, var(--n-primary-color, var(--primary-color, #165dff)) 45%, transparent)
    var(--n-color-embedded, #f2f3f5);
  scrollbar-width: thin;
  overscroll-behavior: contain;
}

.table-scroll::-webkit-scrollbar {
  width: 8px;
}

.table-scroll::-webkit-scrollbar-track {
  background: var(--n-color-embedded, #f2f3f5);
}

.table-scroll::-webkit-scrollbar-thumb {
  border: 2px solid var(--n-color-embedded, #f2f3f5);
  border-radius: 999px;
  background: color-mix(in srgb, var(--n-primary-color, var(--primary-color, #165dff)) 45%, transparent);
}

.application-empty {
  width: 100%;
  height: 100%;
  justify-content: center;
}

.application-loading-mask {
  position: absolute;
  z-index: 8;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(255 255 255 / 66%);
  backdrop-filter: blur(1px);
}

.panel-pagination {
  flex-wrap: wrap;
  justify-content: flex-end;
  min-height: 56px;
  border-top: 1px solid var(--n-border-color, #e5e7eb);
}

.pagination-controls {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.page-size-select {
  width: 108px;
}

:global(.dark) .application-loading-mask {
  background: rgb(24 24 28 / 72%);
}

@media (max-width: 980px) {
  .app-center-layout {
    grid-template-columns: 230px minmax(0, 1fr);
  }

  .panel-toolbar,
  .panel-pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-controls {
    width: 100%;
    justify-content: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 720px) {
  .app-center-page {
    height: auto;
    min-height: 100%;
    padding: 8px;
  }

  .app-center-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .suite-nav {
    max-height: 300px;
    border-right: 0;
    border-bottom: 1px solid var(--suite-panel-border);
  }

  .application-panel {
    min-height: 600px;
    margin: 0;
  }
}
</style>
