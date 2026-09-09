<template>
  <div class="app-center-page">
    <header class="app-center-view-head">
      <div class="app-center-view-brand">
        <span class="app-center-view-brand__mark"><NIcon><AppsOutline /></NIcon></span>
        <span><strong>应用中心</strong><small>设计、发布与交付业务应用</small></span>
      </div>
      <n-tabs v-model:value="activeView" type="line" size="small" @update:value="handleViewChange">
        <n-tab name="MY_APPS">
          我的应用
        </n-tab>
        <n-tab name="MARKET">
          应用市场
        </n-tab>
      </n-tabs>
    </header>

    <section
      v-if="activeView === 'MY_APPS'"
      class="app-center-layout"
      :class="{ 'is-suite-nav-collapsed': suiteNavCollapsed }"
    >
      <aside class="suite-nav">
        <div class="suite-nav-head">
          <div class="suite-nav-heading">
            <strong>应用分组</strong>
          </div>
          <div class="suite-nav-actions">
            <n-button
              v-if="!suiteNavCollapsed"
              quaternary
              circle
              size="small"
              class="suite-create"
              aria-label="新建分组"
              title="新建分组"
              @click="openSuiteEditor(null)"
            >
              <template #icon>
                <NIcon><AddOutline /></NIcon>
              </template>
            </n-button>
            <n-button
              quaternary
              circle
              size="small"
              class="suite-collapse"
              :aria-label="suiteNavCollapsed ? '展开应用分组' : '收起应用分组'"
              :title="suiteNavCollapsed ? '展开应用分组' : '收起应用分组'"
              @click="suiteNavCollapsed = !suiteNavCollapsed"
            >
              <template #icon>
                <NIcon>
                  <ChevronForwardOutline v-if="suiteNavCollapsed" />
                  <ChevronBackOutline v-else />
                </NIcon>
              </template>
            </n-button>
          </div>
        </div>

        <div class="suite-list" :class="{ refreshing: loadingSuites }">
          <button
            type="button"
            class="suite-item all-suite"
            :class="{ active: !suiteCode }"
            title="全部应用"
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
              :aria-label="isSuiteExpanded(row.suite) ? '收起子分组' : '展开子分组'"
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
              :title="row.suite.suiteName || row.suite.suiteCode"
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
              <n-button quaternary circle size="tiny" class="suite-more" aria-label="分组操作">
                <template #icon>
                  <NIcon><EllipsisVertical /></NIcon>
                </template>
              </n-button>
            </n-dropdown>
          </div>
        </div>
      </aside>

      <main class="application-workspace">
        <section class="application-panel">
          <div class="panel-toolbar">
            <div class="toolbar-left">
              <n-radio-group v-model:value="applicationScope" size="small" @update:value="handleScopeChange">
                <n-radio-button value="CREATED">
                  我创建的
                </n-radio-button>
                <n-radio-button value="ALL">
                  我有权限的
                </n-radio-button>
                <n-radio-button value="RECENT">
                  最近使用
                </n-radio-button>
              </n-radio-group>
              <ApplicationFilterBar
                v-model:keyword="keyword"
                v-model:design-status="designStatus"
                v-model:status="status"
                class="toolbar-filters"
                :loading="loadingApplications"
                @search="applyKeywordFilter"
                @refresh="loadApplications"
              />
            </div>
            <div class="toolbar-actions">
              <span class="result-summary">{{ resultRangeText }}</span>
              <n-button type="primary" aria-label="新建应用" title="新建应用" @click="openApplicationCreate('BLANK')">
                <template #icon>
                  <NIcon><AddOutline /></NIcon>
                </template>
                新建应用
              </n-button>
            </div>
          </div>

          <div class="application-table-region">
            <div
              v-if="groupedApplications.length"
              class="table-scroll"
              tabindex="0"
              aria-label="业务应用卡片列表，可纵向滚动"
            >
              <n-collapse
                :default-expanded-names="['today', 'thisWeek', 'earlier']"
                arrow-placement="left"
                class="application-group-collapse"
              >
                <n-collapse-item
                  v-for="group in groupedApplications"
                  :key="group.key"
                  :name="group.key"
                  :title="`${group.label}（${group.count}）`"
                >
                  <ApplicationTable
                    :applications="group.items"
                    @enter="openApplication"
                    @run="openApplicationPortal"
                    @edit="openApplicationSettings"
                    @code="openApplicationCode"
                    @publish="openApplicationPublish"
                    @toggle="toggleApplication"
                    @delete="removeApplication"
                  />
                </n-collapse-item>
              </n-collapse>
            </div>

            <n-empty
              v-else-if="!loadingApplications"
              class="application-empty"
              :description="applicationEmptyDescription"
            >
              <template #extra>
                <n-button type="primary" @click="openApplicationCreate('BLANK')">
                  新建应用
                </n-button>
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

    <AppMarketPanel v-else @create-template="openTemplateCreate" />

    <AppCreateWizard
      v-model:show="createWizardVisible"
      :suites="suites"
      :default-suite-code="suiteCode || ''"
      :initial-mode="createWizardMode"
      :initial-template-key="createWizardTemplateKey"
      :initial-delivery-mode="createWizardDeliveryMode"
      @created="handleApplicationCreated"
      @draft-saved="handleApplicationDraftSaved"
    />

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
  ChevronBackOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  EllipsisVertical,
} from '@vicons/ionicons5'
import { NIcon, useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessSuiteSummary,
  deleteBusinessSuite,
  updateBusinessSuiteStatus,
} from '@/api/business-app'
import {
  businessApplicationPage,
  deleteBusinessApplication,
  updateBusinessApplicationStatus,
} from '@/api/business-application'
import IconRenderer from '@/components/IconRenderer.vue'
import AppCreateWizard from './components/AppCreateWizard.vue'
import ApplicationFilterBar from './components/ApplicationFilterBar.vue'
import ApplicationTable from './components/ApplicationTable.vue'
import AppMarketPanel from './components/AppMarketPanel.vue'

const ApplicationEditorDrawer = defineAsyncComponent(() => import('./components/ApplicationEditorDrawer.vue'))
const AppCodePanel = defineAsyncComponent(() => import('./components/AppCodePanel.vue'))
const SuiteEditorDrawer = defineAsyncComponent(() => import('./components/SuiteEditorDrawer.vue'))

const route = useRoute()
const router = useRouter()
const message = useMessage()
const RECENT_APPLICATION_STORAGE_KEY = 'forge:app-center:recent-applications'

const activeView = ref(normalizeView(route.query.view))
const applicationScope = ref(normalizeApplicationScope(route.query.scope))
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
const suiteNavCollapsed = ref(false)
const createWizardVisible = ref(false)
const createWizardMode = ref('BLANK')
const createWizardTemplateKey = ref('')
const createWizardDeliveryMode = ref('ONLINE')
const applicationEditorVisible = ref(false)
const editingApplication = ref(null)
const applicationCodeVisible = ref(false)
const codeApplication = ref(null)
const editorInitializeMode = ref('BLANK')
const suiteEditorVisible = ref(false)
const editingSuite = ref(null)
let applicationRequestVersion = 0
let applyingRouteQuery = false

const pageSizeOptions = [
  { label: '10 条/页', value: 10 },
  { label: '20 条/页', value: 20 },
  { label: '50 条/页', value: 50 },
]

const allApplicationTotal = computed(() => suites.value.reduce(
  (sum, suite) => sum + Number(suite.applicationCount || 0),
  0,
))

const groupedApplications = computed(() => {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const weekStart = new Date(todayStart)
  weekStart.setDate(weekStart.getDate() - weekStart.getDay())
  const groups = { today: [], thisWeek: [], earlier: [] }
  for (const app of applications.value) {
    const t = new Date(String(app.updateTime || '').replace(' ', 'T'))
    if (Number.isNaN(t.getTime())) {
      groups.earlier.push(app)
      continue
    }
    if (t >= todayStart)
      groups.today.push(app)
    else if (t >= weekStart)
      groups.thisWeek.push(app)
    else
      groups.earlier.push(app)
  }
  return [
    { key: 'today', label: '今天', count: groups.today.length, items: groups.today },
    { key: 'thisWeek', label: '本周', count: groups.thisWeek.length, items: groups.thisWeek },
    { key: 'earlier', label: '更早', count: groups.earlier.length, items: groups.earlier },
  ].filter(g => g.items.length > 0)
})
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
const applicationEmptyDescription = computed(() => ({
  CREATED: '你还没有创建符合当前条件的应用',
  ALL: '当前权限范围内没有符合条件的应用',
  RECENT: '还没有最近打开的应用',
})[applicationScope.value])

watch([designStatus, status], () => {
  if (applyingRouteQuery)
    return
  pageNum.value = 1
  syncRouteQuery()
  loadApplications()
})

watch(() => route.query, async (query) => {
  const nextState = {
    activeView: normalizeView(query.view),
    applicationScope: normalizeApplicationScope(query.scope),
    keyword: queryText(query.keyword),
    suiteCode: queryText(query.suiteCode) || null,
    designStatus: queryText(query.designStatus) || null,
    status: queryStatus(query.status),
    pageNum: queryPositiveInt(query.pageNum, 1),
    pageSize: queryPositiveInt(query.pageSize, 20),
  }
  const changed = activeView.value !== nextState.activeView
    || applicationScope.value !== nextState.applicationScope
    || keyword.value !== nextState.keyword
    || suiteCode.value !== nextState.suiteCode
    || designStatus.value !== nextState.designStatus
    || status.value !== nextState.status
    || pageNum.value !== nextState.pageNum
    || pageSize.value !== nextState.pageSize
  if (!changed)
    return

  applyingRouteQuery = true
  activeView.value = nextState.activeView
  applicationScope.value = nextState.applicationScope
  keyword.value = nextState.keyword
  suiteCode.value = nextState.suiteCode
  designStatus.value = nextState.designStatus
  status.value = nextState.status
  pageNum.value = nextState.pageNum
  pageSize.value = nextState.pageSize
  await nextTick()
  applyingRouteQuery = false
  if (activeView.value === 'MY_APPS')
    loadApplications()
}, { deep: true })

onMounted(async () => {
  await Promise.all([loadSuites(), loadApplications()])
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('storage', handleApplicationPublished)
  window.addEventListener('forge:application-published', handleApplicationPublished)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('storage', handleApplicationPublished)
  window.removeEventListener('forge:application-published', handleApplicationPublished)
})

function handleVisibilityChange() {
  if (!document.hidden && activeView.value === 'MY_APPS')
    loadApplications()
}

function handleApplicationPublished(event) {
  if (event?.type === 'storage' && event.key !== 'forge:app-center:application-published')
    return
  if (activeView.value === 'MY_APPS')
    loadApplications()
}

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
      scope: applicationScope.value,
      applicationIds: applicationScope.value === 'RECENT' ? readRecentApplicationIds().join(',') : undefined,
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

function handleViewChange() {
  syncRouteQuery()
  if (activeView.value === 'MY_APPS')
    loadApplications()
}

function handleScopeChange() {
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
      view: activeView.value === 'MARKET' ? 'market' : undefined,
      scope: applicationScope.value === 'CREATED' ? undefined : applicationScope.value.toLowerCase(),
      pageNum: pageNum.value > 1 ? pageNum.value : undefined,
      pageSize: pageSize.value !== 20 ? pageSize.value : undefined,
    },
  })
}

function openApplicationCreate(mode = 'BLANK', templateKey = '', deliveryMode = 'ONLINE') {
  createWizardMode.value = mode
  createWizardTemplateKey.value = templateKey
  createWizardDeliveryMode.value = deliveryMode
  createWizardVisible.value = true
}

function openTemplateCreate({ deliveryMode = 'ONLINE', template } = {}) {
  openApplicationCreate('TEMPLATE', template?.key || '', deliveryMode)
}

async function handleApplicationCreated(result) {
  await Promise.all([loadSuites(), loadApplications()])
  const application = result?.application
  if (!application?.applicationCode)
    return
  rememberApplication(application.id)
  if (result.deliveryMode === 'SOURCE') {
    openApplicationCode(application)
    return
  }
  openApplication(application, true, result.initializeMode)
}

async function handleApplicationDraftSaved() {
  await Promise.all([loadSuites(), loadApplications()])
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
  rememberApplication(application.id)
  const location = {
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: application.applicationCode },
    query: initializeMode && initializeMode !== 'BLANK'
      ? { edit: '1' }
      : undefined,
  }
  if (!newTab) {
    router.push(location)
    return
  }
  const target = router.resolve(location)
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openApplicationPortal(application) {
  if (!application?.applicationCode)
    return
  const target = router.resolve({
    name: 'ApplicationPortal',
    params: { applicationCodeOrSlug: application.portalSlug || application.applicationCode },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openApplicationSettings(application) {
  if (!application?.applicationCode)
    return
  const target = router.resolve({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: application.applicationCode },
    query: { view: 'settings' },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openApplicationPublish(application) {
  if (!application?.applicationCode)
    return
  const target = router.resolve({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: application.applicationCode },
    query: { view: 'publish' },
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
    { label: '新增子分组', key: 'create-child' },
    { label: '编辑分组', key: 'edit' },
    { label: Number(suite.status) === 1 ? '停用分组' : '启用分组', key: 'toggle' },
    { type: 'divider', key: 'divider' },
    { label: '删除分组', key: 'delete' },
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
    title: `${action}分组`,
    content: `确定${action}"${suite.suiteName || suite.suiteCode}"吗？现有应用和入口数据不会被删除。`,
    positiveText: action,
    async onConfirm() {
      await updateBusinessSuiteStatus(suite.id, nextStatus)
      message.success(`分组已${action}`)
      await loadSuites()
    },
  })
}

function removeSuite(suite) {
  const orphanEntryCount = Number(suite?.appCount || 0)
  const orphanObjectCount = Number(suite?.objectCount || 0)
  const cleanupOrphanResources = orphanEntryCount > 0 || orphanObjectCount > 0
  const cleanupTargets = [
    orphanEntryCount > 0 ? `${orphanEntryCount} 个孤立访问入口` : null,
    orphanObjectCount > 0 ? `${orphanObjectCount} 个未被应用使用的业务对象配置` : null,
  ].filter(Boolean).join('、')
  confirmAction({
    title: '删除分组',
    content: cleanupOrphanResources
      ? `确定删除"${suite.suiteName || suite.suiteCode}"吗？将同时清理该分组内的${cleanupTargets}；访问入口菜单将停用，对应业务数据表和历史版本不会被物理删除。`
      : `确定删除"${suite.suiteName || suite.suiteCode}"吗？存在子分组或业务应用时仍会阻止删除。`,
    positiveText: cleanupOrphanResources ? '删除并清理' : '删除',
    async onConfirm() {
      await deleteBusinessSuite(suite.id, cleanupOrphanResources)
      if (suiteCode.value === suite.suiteCode)
        suiteCode.value = null
      message.success('分组已删除')
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

function normalizeView(value) {
  return queryText(value).toLowerCase() === 'market' ? 'MARKET' : 'MY_APPS'
}

function normalizeApplicationScope(value) {
  const scope = queryText(value).toUpperCase()
  return ['ALL', 'RECENT'].includes(scope) ? scope : 'CREATED'
}

function readRecentApplicationIds() {
  try {
    const stored = JSON.parse(localStorage.getItem(RECENT_APPLICATION_STORAGE_KEY) || '[]')
    if (!Array.isArray(stored))
      return []
    return stored.map(value => String(value)).filter(value => /^\d+$/.test(value)).slice(0, 30)
  }
  catch {
    return []
  }
}

function rememberApplication(applicationId) {
  const id = String(applicationId || '')
  if (!/^\d+$/.test(id))
    return
  const nextIds = [id, ...readRecentApplicationIds().filter(item => item !== id)].slice(0, 30)
  localStorage.setItem(RECENT_APPLICATION_STORAGE_KEY, JSON.stringify(nextIds))
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
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  background: var(--n-color, #fff);
  color: var(--n-text-color, #111827);
}

.app-center-view-head {
  display: flex;
  min-width: 0;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  min-height: 62px;
  padding: 9px 18px 0;
  border-bottom: 1px solid var(--n-border-color, #e5e7eb);
  background: var(--n-color, #fff);
}

.app-center-view-brand,
.app-center-view-brand > span:last-child {
  display: flex;
}

.app-center-view-brand {
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
}

.app-center-view-brand > span:last-child {
  flex-direction: column;
  gap: 1px;
}

.app-center-view-brand__mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 7px;
  color: var(--n-primary-color, #165dff);
  background: color-mix(in srgb, var(--n-primary-color, #165dff) 10%, transparent);
}

.app-center-view-brand strong {
  font-size: 14px;
}

.app-center-view-brand small {
  color: var(--n-text-color-3, #6b7280);
  font-size: 10px;
}

.app-center-view-head :deep(.n-tabs) {
  width: auto;
}

.app-center-layout {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  height: auto;
  min-height: 0;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: var(--n-color, #fff);
}

.app-center-layout.is-suite-nav-collapsed {
  grid-template-columns: 52px minmax(0, 1fr);
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
  grid-template-rows: auto minmax(0, 1fr);
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
  padding: 12px 12px 12px 16px;
  border-bottom: 1px solid var(--suite-panel-border);
  background: var(--suite-panel-head);
}

.suite-nav-heading {
  min-width: 0;
}

.suite-nav-head strong {
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
}

.suite-nav-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
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
  border-radius: 6px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.all-suite {
  margin-bottom: 8px;
}

.suite-item:hover {
  background: #f2f3f5;
}

.suite-item.active {
  color: var(--suite-accent-strong);
  background: #edf2ff;
  font-weight: 500;
}

.suite-item.active .suite-copy strong {
  font-weight: 650;
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
  background: #fff;
}

.toolbar-left {
  display: flex;
  min-width: 0;
  flex: 1 1 760px;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.toolbar-left > :deep(.n-radio-group) {
  flex: 0 1 auto;
  min-width: 0;
}

.toolbar-filters {
  flex: 1 1 500px;
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
  padding-top: 8px;
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
  //scrollbar-color: color-mix(in srgb, var(--n-primary-color, var(--primary-color, #165dff)) 45%, transparent)
  //  var(--n-color-embedded, #f2f3f5);
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

.application-group-collapse {
  --n-border-color: transparent !important;
  --n-divider-color: transparent !important;
}

.application-group-collapse :deep(.n-collapse-item__header) {
  padding: 12px 16px 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--n-text-color, #1d2129);
  letter-spacing: 0;
}

.application-group-collapse :deep(.n-collapse-item__header-main) {
  font-size: 13px;
  font-weight: 500;
}

.application-group-collapse :deep(.n-collapse-item__content-inner) {
  padding: 0 !important;
}

.application-group-collapse :deep(.n-collapse-item) {
  border-bottom: 0 !important;
}

.app-center-layout.is-suite-nav-collapsed .suite-nav-head {
  justify-content: center;
  padding-inline: 6px;
}

.app-center-layout.is-suite-nav-collapsed .suite-nav-heading,
.app-center-layout.is-suite-nav-collapsed .suite-create,
.app-center-layout.is-suite-nav-collapsed .suite-copy,
.app-center-layout.is-suite-nav-collapsed .suite-toggle,
.app-center-layout.is-suite-nav-collapsed .suite-toggle-placeholder,
.app-center-layout.is-suite-nav-collapsed .suite-more {
  display: none;
}

.app-center-layout.is-suite-nav-collapsed .suite-list {
  padding: 8px 6px 16px;
}

.app-center-layout.is-suite-nav-collapsed .suite-row {
  display: block;
  padding-left: 0;
}

.app-center-layout.is-suite-nav-collapsed .suite-item {
  justify-content: center;
  padding-inline: 4px;
}

.app-center-layout.is-suite-nav-collapsed .suite-icon {
  flex-basis: 28px;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--suite-panel-head);
}

.panel-pagination {
  flex-wrap: wrap;
  justify-content: flex-end;
  min-height: 56px;
  border-top: 1px solid var(--n-border-color, #e5e7eb);
  background: #fff;
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

  .toolbar-left,
  .toolbar-filters {
    width: 100%;
    flex-basis: 100%;
  }
}

@media (max-width: 720px) {
  .app-center-page {
    min-height: 100%;
  }

  .app-center-view-head {
    align-items: stretch;
    flex-direction: column;
    gap: 0;
    padding: 9px 12px 0;
  }

  .app-center-view-head :deep(.n-tabs) {
    width: 100%;
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

  .toolbar-left {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .toolbar-left > :deep(.n-radio-group) {
    display: grid;
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .toolbar-left > :deep(.n-radio-group .n-radio-button) {
    min-width: 0;
  }

  .toolbar-left > :deep(.n-radio-group .n-radio-button__label) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .toolbar-filters {
    width: 100%;
  }

  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
    gap: 8px;
  }

  .toolbar-actions :deep(.n-button) {
    width: 100%;
  }
}
</style>
