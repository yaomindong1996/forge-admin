<script setup>
import { storeToRefs } from 'pinia'
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessProcessPage,
  copyBusinessProcess,
  createBusinessProcess,
  deleteBusinessProcess,
  publishBusinessProcess,
  updateBusinessProcessStatus,
  validateBusinessProcess,
} from '@/api/business-process'
import AiForm from '@/components/ai-form/AiForm.vue'
import AiSearch from '@/components/ai-form/AiSearch.vue'
import AiTable from '@/components/ai-form/AiTable.vue'
import AiModal from '@/components/ai-modal/index.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useProcessListStore } from '@/stores/business-process/processListStore'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'

const ApplicationProcessRunsPanel = defineAsyncComponent({
  delay: 120,
  loadingComponent: DesignerAsyncLoader,
  loader: () => import('./ApplicationProcessRunsPanel.vue'),
})

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialObjects: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['changed', 'navigate', 'openDesigner'])
const route = useRoute()
const router = useRouter()
const { dict } = useDict('sys_normal_disable')
const store = useProcessListStore()
const { objectOptions, activeSection } = storeToRefs(store)
const createModalRef = ref(null)
const createFormRef = ref(null)
const searchRef = ref(null)
const validating = ref(false)
const loadError = ref('')
let listRequest = 0

const loading = ref(false)
const creating = ref(false)
const actionId = ref('')
const records = ref([])
const total = ref(0)
const pageNum = ref(normalizePage(route.query.processPage))
const pageSize = ref(10)
const keyword = ref(String(route.query.processKeyword || ''))
const status = ref(normalizeOptionalStatus(route.query.processStatus))
const createForm = ref(createEmptyForm())

watch([() => props.application?.id, () => props.initialObjects], () => {
  store.syncContext(props.application, props.initialObjects)
}, { immediate: true })

const statusOptions = computed(() => (dict.value?.sys_normal_disable || [])
  .map(item => ({
    label: item.label || item.dictLabel,
    value: normalizeOptionalStatus(item.value ?? item.dictValue),
  }))
  .filter(item => item.value != null))

const applicationVersion = computed(() => (
  props.application?.publishedVersion
  || props.application?.currentVersion
  || props.application?.lastPublishVersion
  || 0
))

watch(() => props.application?.id, (applicationId, previousId) => {
  if (previousId && applicationId !== previousId) {
    pageNum.value = 1
    keyword.value = ''
    status.value = null
    records.value = []
    total.value = 0
    createModalRef.value?.close()
    syncRouteFilters()
  }
  if (!applicationId) {
    listRequest += 1
    loading.value = false
    records.value = []
    total.value = 0
    return
  }
  loadProcesses()
}, { immediate: true })

// 应用内弹窗设计器发布后由宿主派发，同步最新版本与设计状态
function handleProcessChanged() {
  if (props.application?.id)
    loadProcesses()
}
window.addEventListener('forge:business-process-changed', handleProcessChanged)

onBeforeUnmount(() => {
  listRequest += 1
  window.removeEventListener('forge:business-process-changed', handleProcessChanged)
})

// 从流程画布返回时（returnTo 携带 processRefresh），重载列表同步最新草稿状态。
watch(() => route.query.processRefresh, () => {
  if (props.application?.id)
    loadProcesses()
})

async function loadProcesses() {
  if (!props.application?.id)
    return
  const requestId = ++listRequest
  loading.value = true
  loadError.value = ''
  try {
    const response = await businessProcessPage({
      applicationId: stringValue(props.application.id),
      keyword: keyword.value || undefined,
      status: status.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (requestId !== listRequest)
      return
    const data = response.data || {}
    records.value = Array.isArray(data.records) ? data.records : (data.list || [])
    total.value = Number(data.total ?? records.value.length)
  }
  catch (error) {
    if (requestId === listRequest) {
      records.value = []
      total.value = 0
      loadError.value = errorMessage(error, '业务流程加载失败')
    }
  }
  finally {
    if (requestId === listRequest)
      loading.value = false
  }
}

function openCreate() {
  if (!objectOptions.value.length)
    return
  const primary = objectOptions.value.find(item => item.role === 'PRIMARY') || objectOptions.value[0]
  createForm.value = { ...createEmptyForm(), subjectObjectId: primary?.value || '' }
  createModalRef.value?.open({ title: '新建业务流程', width: '560px', modalStyle: { maxWidth: 'calc(100vw - 32px)' } })
}

async function confirmCreate() {
  if (creating.value || validating.value)
    return
  validating.value = true
  try {
    await createFormRef.value?.validate()
  }
  catch {
    return // 字段错误由公共表单显示。
  }
  finally {
    validating.value = false
  }
  const processName = String(createForm.value.processName || '').trim()
  if (!processName) {
    notify('warning', '请输入流程名称')
    return
  }
  if (!createForm.value.subjectObjectId) {
    notify('warning', '请选择主业务对象')
    return
  }
  creating.value = true
  try {
    const response = await createBusinessProcess({
      applicationId: stringValue(props.application.id),
      processName,
      processDescription: String(createForm.value.processDescription || '').trim(),
      subjectObjectId: stringValue(createForm.value.subjectObjectId),
      status: 1,
    })
    createModalRef.value?.close()
    notify('success', '业务流程已创建')
    emit('changed')
    const processId = stringValue(response.data?.id)
    await loadProcesses()
    if (processId)
      openDesigner(processId)
  }
  catch (error) {
    notify('error', errorMessage(error, '业务流程创建失败'))
  }
  finally {
    creating.value = false
  }
}

async function copyProcess(item) {
  const processId = stringValue(item.id)
  if (!processId || actionId.value)
    return
  actionId.value = `copy:${processId}`
  try {
    const response = await copyBusinessProcess(processId, {})
    notify('success', '流程副本已创建')
    emit('changed')
    await loadProcesses()
    const copiedId = stringValue(response.data?.id)
    if (copiedId)
      openDesigner(copiedId)
  }
  catch (error) {
    notify('error', errorMessage(error, '复制流程失败'))
  }
  finally {
    actionId.value = ''
  }
}

async function toggleStatus(item) {
  const processId = stringValue(item.id)
  if (!processId || actionId.value)
    return
  const nextStatus = Number(item.status) === 1 ? 0 : 1
  actionId.value = `status:${processId}`
  try {
    await updateBusinessProcessStatus(processId, nextStatus)
    notify('success', nextStatus === 1 ? '业务流程已启用' : '业务流程已停用')
    emit('changed')
    await loadProcesses()
  }
  catch (error) {
    notify('error', errorMessage(error, '更新流程状态失败'))
  }
  finally {
    actionId.value = ''
  }
}

function removeProcess(item) {
  const processId = stringValue(item.id)
  if (!processId || actionId.value)
    return
  const performDelete = async () => {
    actionId.value = `delete:${processId}`
    try {
      await deleteBusinessProcess(processId)
      notify('success', '业务流程已删除')
      emit('changed')
      if (records.value.length === 1 && pageNum.value > 1)
        pageNum.value -= 1
      await syncRouteFilters()
      await loadProcesses()
    }
    catch (error) {
      notify('error', errorMessage(error, '删除流程失败'))
      return false
    }
    finally {
      actionId.value = ''
    }
  }
  if (!window.$dialog)
    return
  window.$dialog.warning({
    title: '删除业务流程',
    content: `确认删除“${item.processName || item.processCode}”吗？删除后不能再触发新运行，历史版本和运行记录会保留。存在未结束的运行时不能删除。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: performDelete,
  })
}

function openDesigner(processId) {
  emit('openDesigner', { processId: stringValue(processId) })
}

function requestApplicationPublish() {
  emit('navigate', 'releases')
}

// 独立发布：只生成该流程的不可变版本并切换运行投影，不触发应用发布，也不影响其他流程。
function publishProcess(item) {
  const processId = stringValue(item.id)
  if (!processId || actionId.value)
    return
  if (!window.$dialog)
    return
  window.$dialog.warning({
    title: '发布业务流程',
    content: `将为“${item.processName || item.processCode}”生成不可变流程版本并立即生效，应用内其他资产与流程不受影响。`,
    positiveText: '检查并发布',
    negativeText: '取消',
    onPositiveClick: () => executePublish(item),
  })
}

async function executePublish(item) {
  const processId = stringValue(item.id)
  if (!processId || actionId.value)
    return
  actionId.value = `publish:${processId}`
  try {
    const validation = await validateBusinessProcess(processId)
    if (!validation.data?.valid) {
      const errorCount = Number(validation.data?.errorCount || 0)
      notify('warning', errorCount
        ? `流程检查发现 ${errorCount} 项错误，请修正后再发布`
        : '流程检查未通过，请修正后再发布')
      await loadProcesses()
      return false
    }
    const response = await publishBusinessProcess(processId)
    const versionNo = Number(response.data?.versionNo || 0)
    notify('success', versionNo ? `业务流程已发布为 V${versionNo}` : '业务流程发布成功')
    emit('changed')
    await loadProcesses()
  }
  catch (error) {
    notify('error', errorMessage(error, '业务流程发布失败'))
    return false
  }
  finally {
    actionId.value = ''
  }
}

const searchSchema = computed(() => [
  { field: 'keyword', label: '流程', type: 'input', props: { placeholder: '名称或编码', clearable: true } },
  { field: 'status', label: '启停状态', type: 'select', props: { placeholder: '全部状态', clearable: true, options: statusOptions.value } },
])
const createSchema = computed(() => [
  { field: 'processName', label: '流程名称', type: 'input', required: true, props: { 'maxlength': 128, 'placeholder': '例如：采购提交审批', 'data-process-field': 'name' } },
  { field: 'subjectObjectId', label: '主业务对象', type: 'select', required: true, props: { options: objectOptions.value, filterable: true, placeholder: '请选择主业务对象' } },
  { field: 'processDescription', label: '说明', type: 'textarea', props: { maxlength: 500, showCount: true, rows: 3, placeholder: '说明流程适用场景' } },
])
const columns = computed(() => [
  { prop: 'processName', label: '流程', minWidth: 220, slot: 'identity' },
  { prop: 'subjectObjectCode', label: '主业务对象', minWidth: 170, slot: 'subject' },
  { prop: 'designStatus', label: '设计状态', width: 120, slot: 'designStatus' },
  { prop: 'publishedVersion', label: '版本', width: 110, slot: 'version' },
  { prop: 'status', label: '启停状态', width: 100, slot: 'status' },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  { prop: 'action', label: '操作', width: 172, fixed: 'right', slot: 'actions' },
])
const pagination = computed(() => ({
  page: pageNum.value,
  pageSize: pageSize.value,
  itemCount: total.value,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  showQuickJumper: true,
  prefix: ({ itemCount }) => `共 ${itemCount} 项`,
}))
const hasFilters = computed(() => Boolean(keyword.value || status.value != null))
const emptyTitle = computed(() => loadError.value ? '业务流程加载失败' : hasFilters.value ? '没有匹配的业务流程' : '当前应用还没有业务流程')
const emptyDescription = computed(() => loadError.value || (hasFilters.value ? '请调整搜索条件或点击重置。' : (objectOptions.value.length ? '点击“新建流程”开始编排业务。' : '请先在数据对象分区加入主业务对象。')))

function moreOptions(item) {
  return [
    { label: '运行记录', key: 'runs' },
    { label: '复制流程', key: 'copy', disabled: Boolean(actionId.value) },
    { label: Number(item.status) === 1 ? '停用流程' : '启用流程', key: 'status', disabled: Boolean(actionId.value) },
    { type: 'divider', key: 'divider' },
    { label: '删除流程', key: 'delete', disabled: Boolean(actionId.value), props: { class: 'text-error' } },
  ]
}
function handleMore(key, item) {
  if (key === 'runs')
    store.viewRuns(item)
  else if (key === 'copy')
    copyProcess(item)
  else if (key === 'status')
    toggleStatus(item)
  else if (key === 'delete')
    removeProcess(item)
}

function searchProcesses(values = {}) {
  keyword.value = String(values.keyword || '').trim()
  status.value = normalizeOptionalStatus(values.status)
  return applyFilters()
}
function changePageSize(value) {
  pageSize.value = value
  return applyFilters()
}

async function applyFilters() {
  pageNum.value = 1
  await syncRouteFilters()
  await loadProcesses()
}

async function changePage(value) {
  pageNum.value = normalizePage(value)
  await syncRouteFilters()
  await loadProcesses()
}

function syncRouteFilters() {
  return router.replace({
    path: route.path,
    query: {
      ...route.query,
      processKeyword: keyword.value || undefined,
      processStatus: status.value == null ? undefined : String(status.value),
      processPage: pageNum.value > 1 ? String(pageNum.value) : undefined,
    },
  })
}

function subjectName(item) {
  const objectId = stringValue(item.subjectObjectId)
  const objectCode = item.subjectObjectCode
  return objectOptions.value.find(object => object.value === objectId || object.code === objectCode)?.label
    || objectCode
    || '未识别对象'
}

function createEmptyForm() {
  return {
    processName: '',
    processDescription: '',
    subjectObjectId: '',
  }
}

function normalizePage(value) {
  const page = Number(value)
  return Number.isInteger(page) && page > 0 ? page : 1
}

function normalizeOptionalStatus(value) {
  if (value === '' || value == null)
    return null
  const normalized = Number(value)
  return Number.isInteger(normalized) ? normalized : null
}

function stringValue(value) {
  return value == null ? '' : String(value)
}

function errorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

function notify(type, message) {
  window.$message?.[type]?.(message)
}
</script>

<template>
  <section class="process-panel" data-workspace-process>
    <header class="process-panel-header">
      <h2>业务流程</h2>
      <span v-if="applicationVersion" class="panel-meta">应用版本 V{{ applicationVersion }}</span>
    </header>
    <n-tabs v-model:value="activeSection" type="line" size="small" class="process-tabs">
      <n-tab name="list" data-process-section="list">
        流程列表
      </n-tab>
      <n-tab name="runs" data-process-section="runs">
        运行记录
      </n-tab>
    </n-tabs>

    <div v-if="activeSection === 'list'" class="process-workspace">
      <AiSearch
        :key="application?.id"
        ref="searchRef"
        :schema="searchSchema"
        :model-value="{ keyword, status }"
        :grid-cols="3"
        :label-width="80"
        :enable-collapse="false"
        :y-gap="8"
        @search="searchProcesses"
        @reset="searchProcesses()"
        @keydown.enter.prevent="searchRef?.handleSearch()"
      />
      <AiTable
        :columns="columns"
        :data-source="records"
        :row-key="row => String(row.id)"
        :loading="loading"
        :pagination="pagination"
        :scroll-x="1062"
        :empty-title="emptyTitle"
        :empty-description="emptyDescription"
        :show-render-mode-switch="false"
        :show-search-toggle="false"
        :show-fullscreen="false"
        hide-selection
        @refresh="loadProcesses"
        @page-change="changePage"
        @page-size-change="changePageSize"
      >
        <template #toolbar-left>
          <n-button data-process-action="create" size="small" type="primary" :disabled="!objectOptions.length" @click="openCreate">
            <template #icon>
              <i class="i-lucide:plus" />
            </template>
            新建流程
          </n-button>
          <n-button size="small" @click="requestApplicationPublish">
            应用发布
          </n-button>
        </template>
        <template #identity="{ row }">
          <SystemTableCell :title="row.processName || row.processCode" :subtitle="row.processCode" interactive :data-process-open="String(row.id)" @activate="openDesigner(row.id)" />
        </template>
        <template #subject="{ row }">
          <div class="entity-cell">
            <SystemTableCell :title="subjectName(row)" /><span class="panel-meta">{{ row.subjectObjectCode }}</span>
          </div>
        </template>
        <template #designStatus="{ row }">
          <DictTag dict-type="ai_business_process_design_status" :value="row.designStatus" :bordered="false" />
        </template>
        <template #version="{ row }">
          <div class="entity-cell">
            <span>{{ row.publishedVersion ? `V${row.publishedVersion}` : '未发布' }}</span><span class="panel-meta">草稿 {{ row.currentVersion || 0 }}</span>
          </div>
        </template>
        <template #status="{ row }">
          <DictTag dict-type="sys_normal_disable" :value="row.status" :bordered="false" />
        </template>
        <template #actions="{ row }">
          <div class="row-actions">
            <n-button text type="primary" size="small" @click="openDesigner(row.id)">
              设计
            </n-button>
            <n-divider vertical />
            <n-button text type="primary" size="small" :data-process-publish="String(row.id)" :disabled="Boolean(actionId) || Number(row.status) !== 1" :loading="actionId === `publish:${row.id}`" :title="Number(row.status) === 1 ? '检查并发布当前流程' : '请先启用流程再发布'" @click="publishProcess(row)">
              发布
            </n-button>
            <n-divider vertical />
            <n-dropdown trigger="click" placement="bottom-end" :options="moreOptions(row)" @select="key => handleMore(key, row)">
              <n-button quaternary size="small" aria-label="更多操作" title="更多操作" :data-process-more="String(row.id)">
                <template #icon>
                  <i class="i-lucide:more-horizontal" />
                </template>
              </n-button>
            </n-dropdown>
          </div>
        </template>
      </AiTable>
    </div>
    <ApplicationProcessRunsPanel v-else />

    <AiModal ref="createModalRef" :closable="false" :esc-closable="false" show-header-extra>
      <template #header-extra>
        <n-button quaternary circle size="small" :disabled="creating || validating" aria-label="关闭新建流程" title="关闭" @click="createModalRef?.close()">
          <template #icon>
            <i class="i-lucide:x" />
          </template>
        </n-button>
      </template>
      <AiForm ref="createFormRef" v-model:value="createForm" :schema="createSchema" :grid-cols="1" :label-width="100" :disabled="creating" :show-submit="false" :show-reset="false" />
      <p class="create-hint">
        主业务对象创建后不可切换；流程编码由系统自动生成并保持稳定。
      </p>
      <template #footer>
        <div class="modal-actions">
          <n-button :disabled="creating || validating" @click="createModalRef?.close()">
            取消
          </n-button>
          <n-button data-process-action="confirm-create" type="primary" :loading="creating || validating" @click="confirmCreate">
            创建并设计
          </n-button>
        </div>
      </template>
    </AiModal>
  </section>
</template>

<style scoped>
.process-panel {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  background: var(--bg-primary);
}
.process-panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 0;
}
.process-panel-header h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.process-tabs {
  flex: none;
  padding: 0 12px;
}
.process-workspace {
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
}
.entity-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}
.panel-meta,
.create-hint {
  color: var(--text-tertiary);
  font-size: 12px;
}
.panel-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.create-hint {
  margin: 0;
  line-height: 1.6;
}
.row-actions,
.modal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.row-actions :deep(.n-divider) {
  margin: 0;
}
.modal-actions {
  justify-content: flex-end;
}
</style>
