<template>
  <div class="extensions-panel" :class="{ 'is-editing': editorVisible }">
    <template v-if="!editorVisible">
      <header v-if="!embedded" class="panel-heading">
        <div>
          <h2>动作与增强</h2>
          <p>配置业务规则、页面脚本、样式与 Java 服务；保存并测试通过后启用。</p>
        </div>
      </header>

      <div class="list-toolbar">
        <div class="list-toolbar__filters">
          <n-input
            v-if="!showCreateEmpty"
            v-model:value="filters.keyword"
            clearable
            size="small"
            placeholder="搜索名称或编码"
          />
          <DictSelect
            v-if="!showCreateEmpty"
            v-model:value="filters.extensionType"
            dict-type="ai_business_extension_type"
            placeholder="类型"
            clearable
            size="small"
          />
          <DictSelect
            v-if="!showCreateEmpty"
            v-model:value="filters.hookCode"
            dict-type="ai_business_extension_hook"
            placeholder="钩子"
            clearable
            size="small"
          />
          <DictSelect
            v-if="!showCreateEmpty"
            v-model:value="filters.status"
            dict-type="ai_business_extension_status"
            placeholder="状态"
            clearable
            size="small"
          />
          <n-checkbox
            v-if="contextPageId && !showCreateEmpty"
            v-model:checked="onlyCurrentPage"
            size="small"
          >
            仅当前页
          </n-checkbox>
          <span v-if="embedded && contextPageLabel" class="context-hint">
            {{ contextPageLabel }}
          </span>
        </div>
        <n-space :size="8">
          <n-dropdown
            v-if="objects.length"
            :options="objectActionOptions"
            @select="openObjectActions"
          >
            <n-button secondary size="small">
              配置对象动作
            </n-button>
          </n-dropdown>
          <n-button type="primary" size="small" :loading="editorDependencyLoading" @click="openCreate()">
            新建增强
          </n-button>
        </n-space>
      </div>

      <div v-if="showCreateEmpty" class="extensions-empty">
        <div class="extensions-empty__copy">
          <strong>还没有增强</strong>
          <span>选一种能力开始配置，测试通过后启用；预览刷新可见，正式环境需重新发布。</span>
        </div>
        <div class="empty-type-grid">
          <button
            v-for="item in createTypeChoices"
            :key="item.value"
            type="button"
            class="empty-type-card"
            @click="openCreate(item.value)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </button>
        </div>
      </div>

      <template v-else>
        <n-spin :show="loading" class="list-spin">
          <n-empty
            v-if="showFilterEmpty"
            class="extensions-filter-empty"
            size="small"
            :description="filterEmptyDescription"
          >
            <template #extra>
              <n-space>
                <n-button
                  v-if="onlyCurrentPage && contextPageId && extensions.length"
                  size="small"
                  secondary
                  @click="onlyCurrentPage = false"
                >
                  查看全部增强
                </n-button>
                <n-button
                  v-if="hasActiveFilters"
                  size="small"
                  secondary
                  @click="clearFilters"
                >
                  清除筛选
                </n-button>
                <n-button type="primary" size="small" @click="openCreate()">
                  新建增强
                </n-button>
              </n-space>
            </template>
          </n-empty>

          <div v-else class="extension-table">
            <div class="extension-row extension-row-head">
              <span>增强 <em v-if="displayedExtensions.length">{{ displayedExtensions.length }}</em></span>
              <span>类型</span>
              <span>作用范围</span>
              <span>状态</span>
              <span>操作</span>
            </div>
            <n-tooltip
              v-for="item in displayedExtensions"
              :key="item.id"
              placement="top"
              :delay="400"
            >
              <template #trigger>
                <div class="extension-row" @dblclick="openEdit(item)">
                  <div class="extension-name">
                    <strong class="extension-title" @click="openEdit(item)">{{ item.extensionName }}</strong>
                    <code>{{ item.extensionCode }}</code>
                  </div>
                  <div class="extension-type">
                    <DictTag dict-type="ai_business_extension_type" :value="item.extensionType" :bordered="false" />
                    <small>{{ hookLabel(item.hookCode) }}</small>
                  </div>
                  <span class="extension-scope">{{ scopeText(item) }}</span>
                  <div class="version-state">
                    <n-tag size="small" :type="lifecycleTone(item)" :bordered="false">
                      {{ lifecycleLabel(item) }}
                    </n-tag>
                    <small>{{ lifecycleHint(item) }}</small>
                  </div>
                  <div class="extension-actions" @click.stop>
                    <a class="cursor-pointer text-primary" @click="openEdit(item)">编辑</a>
                    <a
                      v-if="item.status !== 'ENABLED'"
                      class="cursor-pointer text-success"
                      @click="handleMoreAction('enable', item)"
                    >启用</a>
                    <a
                      v-else
                      class="cursor-pointer text-warning"
                      @click="handleMoreAction('disable', item)"
                    >停用</a>
                    <n-dropdown :options="actionOptions()" @select="key => handleMoreAction(key, item)">
                      <a class="cursor-pointer text-info">更多</a>
                    </n-dropdown>
                  </div>
                </div>
              </template>
              {{ rowTooltip(item) }}
            </n-tooltip>
          </div>
        </n-spin>
      </template>
    </template>

    <ExtensionEditorWorkspace
      :show="editorVisible"
      :application="application"
      :extension="editingExtension"
      :create-defaults="createDefaults"
      :objects="objects"
      :entries="entries"
      :pages="pages"
      :handlers="handlers"
      :start-with-test="startWithTest"
      @update:show="handleEditorShow"
      @saved="handleSaved"
      @closed="handleEditorClosed"
    />

    <ExtensionVersionDrawer
      v-model:show="versionVisible"
      :extension="versionExtension"
      @changed="handleSaved"
    />
  </div>
</template>

<script setup>
import { useDialog, useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessAppList } from '@/api/business-app'
import { businessApplicationObjects } from '@/api/business-application'
import {
  acquireBusinessExtensionLock,
  businessExtensionDetail,
  businessExtensionPage,
  businessExtensionServerHandlers,
  deleteBusinessExtension,
  releaseBusinessExtensionLock,
  updateBusinessExtensionStatus,
  validateBusinessExtension,
} from '@/api/business-extension'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import {
  extensionMatchesPage,
  resolveExtensionPageContext,
} from './extension-visual-rule'
import ExtensionEditorWorkspace from './ExtensionEditorDrawer.vue'
import ExtensionVersionDrawer from './ExtensionVersionDrawer.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialExtensions: {
    type: Array,
    default: null,
  },
  initialObjects: {
    type: Array,
    default: null,
  },
  initialEntries: {
    type: Array,
    default: null,
  },
  initialPages: {
    type: Array,
    default: null,
  },
  contextPageId: {
    type: String,
    default: '',
  },
  embedded: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['changed', 'openDesigner'])
const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const extensions = ref([])
const objects = ref([])
const entries = ref([])
const pages = ref([])
const handlers = ref([])
const editorDependencyLoading = ref(false)
const editorDependenciesLoaded = ref(false)
let editorDependencyPromise = null
let editorDependencyApplicationId = null
const editorVisible = ref(false)
const editingExtension = ref(null)
const createDefaults = ref(null)
const startWithTest = ref(false)
const versionVisible = ref(false)
const versionExtension = ref(null)
const onlyCurrentPage = ref(Boolean(props.contextPageId))
let filterReloadTimer = null
const filters = reactive({
  keyword: '',
  extensionType: null,
  hookCode: null,
  status: null,
})

const createTypeChoices = [
  { value: 'VISUAL_RULE', title: '业务规则', description: '条件校验、赋值与提示，无需写代码' },
  { value: 'CLIENT_JS', title: '页面 JS', description: '字段联动、提示与受控页面动作' },
  { value: 'SCOPED_CSS', title: '页面 CSS', description: '限定在指定页面内的样式调整' },
  { value: 'SERVER_BINDING', title: 'Java 服务', description: '调用已注册的后端业务处理器' },
]

const objectActionOptions = computed(() => objects.value.map(item => ({
  label: item.objectName || item.objectCode,
  key: String(item.objectId),
})))

const pageContext = computed(() => resolveExtensionPageContext(props.contextPageId, {
  pages: pages.value,
  entries: entries.value,
  objects: objects.value,
}))

const contextPageLabel = computed(() => {
  if (!props.contextPageId)
    return ''
  return pageContext.value?.pageTitle || props.contextPageId
})

const hasActiveFilters = computed(() => Boolean(
  filters.keyword?.trim()
  || filters.extensionType
  || filters.hookCode
  || filters.status,
))

const displayedExtensions = computed(() => {
  let list = extensions.value
  if (onlyCurrentPage.value && props.contextPageId) {
    const pageId = String(props.contextPageId)
    list = list.filter(item => extensionMatchesPage(item, pageId, pageContext.value)
      || matchesCurrentPage(item, pageId))
  }
  return list
})

const showCreateEmpty = computed(() => !loading.value && !extensions.value.length && !hasActiveFilters.value)
const showFilterEmpty = computed(() => !loading.value && !displayedExtensions.value.length && !showCreateEmpty.value)

const filterEmptyDescription = computed(() => {
  if (onlyCurrentPage.value && props.contextPageId && extensions.value.length)
    return '当前页面还没有增强，可查看应用全部增强或新建一条'
  if (hasActiveFilters.value)
    return '没有符合条件的增强'
  return '当前页面还没有增强'
})

function clearFilters() {
  filters.keyword = ''
  filters.extensionType = null
  filters.hookCode = null
  filters.status = null
}

function rowTooltip(item) {
  const parts = [
    `钩子：${hookLabel(item.hookCode)}`,
    item.failurePolicy ? `失败策略：${failurePolicyLabel(item.failurePolicy)}` : '',
    item.riskLevel ? `风险：${riskLevelLabel(item.riskLevel)}` : '',
    lifecycleHint(item),
  ].filter(Boolean)
  return parts.join(' · ')
}

function failurePolicyLabel(value) {
  return {
    BLOCK: '阻断',
    CONTINUE: '继续',
    IGNORE: '忽略',
  }[value] || value
}

function riskLevelLabel(value) {
  return {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
  }[value] || value
}

watch([
  () => props.application?.id,
  () => props.initialExtensions,
], ([applicationId, initialExtensions]) => {
  if (!applicationId)
    return
  if (String(editorDependencyApplicationId || '') !== String(applicationId))
    editorDependenciesLoaded.value = false
  if (Array.isArray(initialExtensions)) {
    extensions.value = [...initialExtensions]
    return
  }
  loadExtensions()
}, { immediate: true })

watch(() => props.initialObjects, (value) => {
  if (Array.isArray(value))
    objects.value = [...value]
}, { immediate: true })

watch(() => props.initialEntries, (value) => {
  if (Array.isArray(value))
    entries.value = [...value]
}, { immediate: true })

watch(() => props.initialPages, (value) => {
  if (Array.isArray(value))
    pages.value = [...value]
}, { immediate: true })

watch(() => props.contextPageId, (pageId) => {
  onlyCurrentPage.value = Boolean(pageId)
})

watch(
  () => [filters.keyword, filters.extensionType, filters.hookCode, filters.status],
  () => {
    if (!props.application?.id)
      return
    window.clearTimeout(filterReloadTimer)
    filterReloadTimer = window.setTimeout(() => {
      loadExtensions()
    }, 280)
  },
)

async function loadExtensions() {
  if (!props.application?.id)
    return
  loading.value = true
  try {
    const response = await businessExtensionPage({
      pageNum: 1,
      pageSize: 200,
      applicationId: props.application.id,
      keyword: filters.keyword?.trim() || undefined,
      extensionType: filters.extensionType || undefined,
      hookCode: filters.hookCode || undefined,
      status: filters.status || undefined,
    })
    extensions.value = response.data?.records || []
  }
  finally {
    loading.value = false
  }
}

async function loadTargets(applicationId) {
  const [objectResponse, entryResponse] = await Promise.all([
    businessApplicationObjects(applicationId),
    businessAppList({ applicationId }),
  ])
  objects.value = objectResponse.data || []
  entries.value = entryResponse.data || []
}

async function loadHandlers() {
  try {
    const response = await businessExtensionServerHandlers()
    handlers.value = response.data || []
  }
  catch {
    handlers.value = []
  }
}

function buildCreateDefaults(extensionType = null) {
  const defaults = {}
  if (extensionType)
    defaults.extensionType = extensionType
  const context = pageContext.value
  if (context) {
    defaults.scopeKey = context.scopeKey
    if (extensionType === 'SCOPED_CSS')
      defaults.scopeType = 'PAGE'
    if (context.objectId)
      defaults.objectId = context.objectId
    if (context.entryId)
      defaults.entryId = context.entryId
  }
  return Object.keys(defaults).length ? defaults : null
}

async function openCreate(extensionType = null) {
  await ensureEditorDependencies()
  startWithTest.value = false
  editingExtension.value = null
  createDefaults.value = buildCreateDefaults(extensionType)
  editorVisible.value = true
}

async function openEdit(item, shouldTest = false) {
  const [detailResponse, lockResponse] = await Promise.all([
    businessExtensionDetail(item.id),
    acquireBusinessExtensionLock(item.id),
    ensureEditorDependencies(),
  ])
  startWithTest.value = shouldTest
  createDefaults.value = null
  editingExtension.value = {
    ...(detailResponse.data || item),
    lockToken: lockResponse.data?.lockToken,
    lockExpireTime: lockResponse.data?.expireTime,
  }
  editorVisible.value = true
}

async function ensureEditorDependencies() {
  const applicationId = props.application?.id
  if (!applicationId)
    return
  if (editorDependenciesLoaded.value
    && String(editorDependencyApplicationId) === String(applicationId)) {
    return
  }
  if (editorDependencyPromise
    && String(editorDependencyApplicationId) === String(applicationId)) {
    return editorDependencyPromise
  }

  editorDependencyApplicationId = applicationId
  editorDependencyLoading.value = true
  const pending = (async () => {
    const tasks = [loadHandlers()]
    if (!Array.isArray(props.initialObjects) || !Array.isArray(props.initialEntries))
      tasks.push(loadTargets(applicationId))
    await Promise.all(tasks)
    if (String(props.application?.id) === String(applicationId))
      editorDependenciesLoaded.value = true
  })().finally(() => {
    if (editorDependencyPromise !== pending)
      return
    editorDependencyPromise = null
    editorDependencyLoading.value = false
  })
  editorDependencyPromise = pending
  return pending
}

function openObjectActions(objectId) {
  const item = objects.value.find(object => String(object.objectId) === String(objectId))
  if (!item)
    return
  emit('openDesigner', {
    objectId: item.objectId,
    objectCode: item.objectCode,
    panel: 'actions',
  })
}

function actionOptions() {
  return [
    { label: '校验草稿', key: 'validate' },
    { label: '打开并测试', key: 'test' },
    { label: '版本与差异', key: 'versions' },
    { label: '删除', key: 'delete' },
  ]
}

async function handleMoreAction(key, item) {
  if (key === 'versions') {
    versionExtension.value = item
    versionVisible.value = true
    return
  }
  if (key === 'validate') {
    const response = await validateBusinessExtension(item.id)
    if (response.data?.passed)
      message.success('当前草稿校验通过')
    else
      message.warning(response.data?.summary || '当前草稿校验未通过')
    await loadExtensions()
    return
  }
  if (key === 'test') {
    await openEdit(item, true)
    return
  }
  if (key === 'enable' || key === 'disable') {
    await updateBusinessExtensionStatus(item.id, key === 'enable' ? 'ENABLED' : 'DISABLED')
    message.success(key === 'enable'
      ? '增强已启用：工作台预览刷新后生效，正式环境需重新发布'
      : '增强已停用')
    await handleSaved()
    return
  }
  if (key === 'delete')
    confirmDelete(item)
}

function confirmDelete(item) {
  dialog.warning({
    title: '删除增强',
    content: `确认删除“${item.extensionName}”及其设计态版本吗？已启用增强必须先停用。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessExtension(item.id)
      message.success('增强已删除')
      await handleSaved()
    },
  })
}

async function handleSaved() {
  await loadExtensions()
  emit('changed')
}

function handleEditorShow(visible) {
  editorVisible.value = visible
  if (!visible)
    createDefaults.value = null
}

async function handleEditorClosed(payload) {
  const id = payload?.id
  const lockToken = payload?.lockToken
  if (!id || !lockToken)
    return
  try {
    await releaseBusinessExtensionLock(id, lockToken)
  }
  catch {
    // 锁可能已超时或保存链路已释放，关闭不阻断用户。
  }
}

function matchesCurrentPage(item, pageId) {
  const entry = entries.value.find(entryItem => String(entryItem.id) === String(item.entryId || ''))
  if (!entry)
    return false
  return [
    entry.pageId,
    entry.targetPageId,
    entry.entryPageId,
    entry.appCode,
  ].some(value => String(value || '') === pageId)
}

function scopeText(item) {
  if (item.objectName)
    return item.objectName
  if (item.entryName)
    return item.entryName
  if (item.scopeKey) {
    const page = pages.value.find(pageItem => String(pageItem.id) === String(item.scopeKey))
    return page?.title || item.scopeKey
  }
  return {
    APPLICATION: '整个应用',
    OBJECT: '业务对象',
    ENTRY: '页面入口',
    PAGE: '指定页面',
    COMPONENT: '指定组件',
  }[item.scopeType] || '整个应用'
}

function hookLabel(hookCode) {
  return {
    PAGE_INIT: '页面打开',
    FORM_CHANGE: '字段变更',
    BEFORE_SUBMIT: '提交前',
    AFTER_SUBMIT: '提交后',
    ROW_ACTION: '行操作',
    BEFORE_SAVE: '保存前',
    AFTER_SAVE: '保存后',
  }[hookCode] || hookCode || '—'
}

function lifecycleLabel(item) {
  if (item.status === 'ENABLED')
    return '运行中'
  if (item.status === 'DISABLED')
    return '已停用'
  if (item.status === 'TESTED')
    return '已测草稿'
  return '草稿'
}

function lifecycleTone(item) {
  if (item.status === 'ENABLED')
    return 'success'
  if (item.status === 'DISABLED')
    return 'warning'
  return 'default'
}

function lifecycleHint(item) {
  if (item.status === 'ENABLED')
    return `运行 v${item.enabledVersion || item.draftVersion} · 正式需发布`
  if (item.status === 'DISABLED')
    return `草稿 v${item.draftVersion}`
  if (item.enabledVersion)
    return `草稿 v${item.draftVersion} / 曾运行 v${item.enabledVersion}`
  return `草稿 v${item.draftVersion}`
}
</script>

<style scoped>
.extensions-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  height: 100%;
}

.extensions-panel.is-editing {
  flex: 1;
}

.panel-heading {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.panel-heading h2 {
  margin: 0;
  font-size: 15px;
}

.panel-heading p {
  margin: 2px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.list-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
}

.list-toolbar__filters {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.list-toolbar__filters > .n-input {
  width: 180px;
  max-width: 100%;
}

.list-toolbar__filters > :deep(.n-select) {
  width: 112px;
}

.context-hint {
  overflow: hidden;
  max-width: 160px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-spin {
  flex: 1;
  min-height: 0;
}

.list-spin :deep(.n-spin-container),
.list-spin :deep(.n-spin-content) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.extensions-empty {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  justify-content: flex-start;
  padding: 12px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.extensions-empty__copy {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
}

.extensions-empty__copy strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.extensions-empty__copy span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.4;
}

.empty-type-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  width: 100%;
}

.empty-type-card {
  display: flex;
  min-height: 58px;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  cursor: pointer;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-secondary, #f7f8fa);
  text-align: left;
}

.empty-type-card:hover {
  border-color: var(--primary-color, #165dff);
  background: var(--bg-primary, #fff);
}

.empty-type-card strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.empty-type-card span {
  font-size: 11px;
  line-height: 1.4;
}

.extensions-filter-empty {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.extension-table {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  scrollbar-gutter: stable;
}

.extension-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.5fr) minmax(100px, 0.7fr) minmax(100px, 0.8fr) minmax(120px, 0.9fr) 120px;
  gap: 8px;
  align-items: center;
  min-width: 760px;
  min-height: 44px;
  padding: 6px 10px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.extension-row:last-child {
  border-bottom: 0;
}

.extension-row-head {
  position: sticky;
  top: 0;
  z-index: 1;
  min-height: 32px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-weight: 600;
}

.extension-row-head span em {
  margin-left: 4px;
  color: var(--text-tertiary, #86909c);
  font-style: normal;
  font-weight: 500;
}

.extension-row:hover {
  background: color-mix(in srgb, var(--primary-color, #165dff) 4%, var(--bg-primary, #fff));
}

.extension-row-head:hover {
  background: var(--bg-secondary, #f7f8fa);
}

.extension-name,
.version-state,
.extension-type {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
}

.extension-title {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.extension-title:hover {
  color: var(--primary-color, #165dff);
}

.extension-name code,
.version-state small,
.extension-type small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.extension-scope {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.extension-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

@media (max-width: 900px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .list-toolbar__filters > .n-input,
  .list-toolbar__filters > :deep(.n-select) {
    width: calc(50% - 3px);
    flex: 1 1 calc(50% - 3px);
  }

  .empty-type-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
