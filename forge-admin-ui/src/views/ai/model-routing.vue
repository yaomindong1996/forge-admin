<template>
  <div class="routing-page">
    <n-tabs v-model:value="activeTab" type="line" animated>
      <n-tab-pane name="policies" tab="路由策略">
        <div class="toolbar">
          <n-input v-model:value="policyQuery.keyword" clearable placeholder="搜索策略名称或编码" class="search-input" @keyup.enter="loadPolicies" />
          <NButton secondary @click="loadPolicies">
            查询
          </NButton>
          <NButton v-if="canEditPolicies" type="primary" @click="openPolicyModal()">
            新增策略
          </NButton>
        </div>
        <n-data-table :columns="policyColumns" :data="policies" :loading="policyLoading" :row-key="row => row.id" size="small" />
        <div class="pagination">
          <n-pagination v-model:page="policyQuery.pageNum" v-model:page-size="policyQuery.pageSize" :item-count="policyTotal" :page-sizes="[10, 20, 50]" show-size-picker @update:page="loadPolicies" @update:page-size="handlePolicyPageSize" />
        </div>
      </n-tab-pane>

      <n-tab-pane v-if="canViewInvocations" name="invocations" tab="调用记录">
        <div class="toolbar">
          <n-input v-model:value="invocationQuery.agentCode" clearable placeholder="Agent 编码" class="search-input" />
          <n-select v-model:value="invocationQuery.providerId" clearable filterable :options="providerOptions" placeholder="供应商" class="filter-select" />
          <n-select v-model:value="invocationQuery.modelId" clearable filterable :options="invocationModelOptions" placeholder="模型" class="model-filter-select" />
          <n-select v-model:value="invocationQuery.outcome" clearable :options="outcomeOptions" placeholder="调用结果" class="filter-select" />
          <n-date-picker v-model:value="invocationTimeRange" clearable type="datetimerange" class="time-filter" />
          <NButton secondary @click="loadInvocations">
            查询
          </NButton>
        </div>
        <div class="summary-line">
          <span>调用 {{ summary.totalCount || 0 }} 次</span>
          <span>成功 {{ summary.successCount || 0 }} 次</span>
          <span>P95 {{ summary.p95LatencyMs ?? '-' }} ms</span>
          <span>估算成本 {{ summary.estimatedCostCent ?? 0 }} 分</span>
          <span v-if="summary.costUnavailableCount">{{ summary.costUnavailableCount }} 次缺少价格</span>
        </div>
        <n-data-table :columns="invocationColumns" :data="invocations" :loading="invocationLoading" :row-key="row => row.id" :scroll-x="1200" size="small" />
        <div class="pagination">
          <n-pagination v-model:page="invocationQuery.pageNum" v-model:page-size="invocationQuery.pageSize" :item-count="invocationTotal" :page-sizes="[20, 50, 100]" show-size-picker @update:page="loadInvocations" @update:page-size="handleInvocationPageSize" />
        </div>
      </n-tab-pane>
    </n-tabs>

    <n-modal v-model:show="policyModal.show" preset="card" :title="policyModal.form.id ? '编辑路由策略' : '新增路由策略'" class="policy-modal">
      <n-form ref="policyFormRef" :model="policyModal.form" :rules="policyRules" label-placement="left" label-width="100">
        <n-form-item label="策略名称" path="policyName">
          <n-input v-model:value="policyModal.form.policyName" />
        </n-form-item>
        <n-form-item label="策略编码" path="policyCode">
          <n-input v-model:value="policyModal.form.policyCode" :disabled="!!policyModal.form.id" placeholder="小写字母、数字、下划线" />
        </n-form-item>
        <n-form-item label="所需能力" path="requiredCapabilities">
          <n-select v-model:value="policyModal.form.requiredCapabilities" multiple clearable :options="capabilityOptions" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-select v-model:value="policyModal.form.status" :options="statusOptions" />
        </n-form-item>
        <n-form-item label="候选模型" path="targets">
          <div class="target-list">
            <div v-for="(target, index) in policyModal.form.targets" :key="index" class="target-row">
              <n-select v-model:value="target.modelId" filterable :options="modelOptions" placeholder="选择模型" />
              <n-input-number v-model:value="target.priority" :min="1" :precision="0" placeholder="优先级" />
              <n-select v-model:value="target.status" :options="statusOptions" placeholder="状态" />
              <NButton text type="error" @click="removeTarget(index)">
                移除
              </NButton>
            </div>
            <NButton dashed block @click="addTarget">
              添加候选模型
            </NButton>
          </div>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="policyModal.form.remark" type="textarea" :rows="2" />
        </n-form-item>
      </n-form>
      <template #action>
        <NButton @click="policyModal.show = false">
          取消
        </NButton><NButton type="primary" :loading="policyModal.saving" @click="savePolicy">
          保存
        </NButton>
      </template>
    </n-modal>

    <n-modal v-model:show="previewModal.show" preset="card" title="预览路由" class="preview-modal">
      <n-input v-model:value="previewModal.agentCode" placeholder="输入要预览的 Agent 编码" />
      <div v-if="previewModal.result" class="preview-result">
        <strong>{{ previewModal.result.providerName }} / {{ previewModal.result.modelName || previewModal.result.providerModelId }}</strong>
        <span>{{ previewModal.result.source }} · {{ previewModal.result.reason }}</span>
        <div v-if="previewModal.result.skippedCandidates?.length" class="skipped-list">
          <span class="skipped-title">已跳过候选</span>
          <div v-for="item in previewModal.result.skippedCandidates" :key="`${item.modelId}-${item.reason}`" class="skipped-item">
            <span>{{ resolveModelLabel(item.modelId) }}</span>
            <NTag size="small" type="warning">
              {{ item.reason }}
            </NTag>
          </div>
        </div>
      </div>
      <template #action>
        <NButton @click="previewModal.show = false">
          关闭
        </NButton><NButton type="primary" :loading="previewModal.loading" @click="runPreview">
          预览
        </NButton>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { invocationPage, invocationSummary, modelPage, providerPage, routePolicyAdd, routePolicyDelete, routePolicyGet, routePolicyPage, routePolicyPreview, routePolicyUpdate } from '@/api/ai'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'

defineOptions({ name: 'AiModelRouting' })
const { dict } = useDict('ai_model_capability_type', 'ai_status', 'ai_invocation_outcome')
const route = useRoute()
const userStore = useUserStore()
const activeTab = ref('policies')
const policies = ref([])
const policyLoading = ref(false)
const policyTotal = ref(0)
const invocations = ref([])
const invocationLoading = ref(false)
const invocationTotal = ref(0)
const summary = reactive({})
const modelCatalog = ref([])
const providerCatalog = ref([])
const invocationTimeRange = ref(null)
const policyFormRef = ref(null)
const policyQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const invocationQuery = reactive({ pageNum: 1, pageSize: 20, agentCode: '', providerId: null, modelId: null, outcome: null })
const capabilityOptions = computed(() => dict.value.ai_model_capability_type || [])
const statusOptions = computed(() => dict.value.ai_status || [])
const outcomeOptions = computed(() => dict.value.ai_invocation_outcome || [])
const modelOptions = computed(() => modelCatalog.value.filter(model => model.status === '0').map((model) => {
  const provider = providerCatalog.value.find(item => String(item.id) === String(model.providerId))
  return { label: `${provider?.providerName || '供应商'} / ${model.modelName || model.modelId}`, value: String(model.id) }
}))
const providerOptions = computed(() => providerCatalog.value.map(provider => ({ label: provider.providerName, value: String(provider.id) })))
const allModelOptions = computed(() => modelCatalog.value.map((model) => {
  const provider = providerCatalog.value.find(item => String(item.id) === String(model.providerId))
  return { label: `${provider?.providerName || '供应商'} / ${model.modelName || model.modelId}`, value: String(model.id), providerId: String(model.providerId) }
}))
const invocationModelOptions = computed(() => allModelOptions.value.filter(option => !invocationQuery.providerId || option.providerId === String(invocationQuery.providerId)))
const routePermissionCodes = computed(() => (route.meta?.btns || []).map(item => item.code))
const canEditPolicies = computed(() => hasPermission('ai:model-routing:edit'))
const canPreviewPolicies = computed(() => hasPermission('ai:model-routing:preview'))
const canViewInvocations = computed(() => hasPermission('ai:model-invocation:list'))

const policyModal = reactive({ show: false, saving: false, form: createPolicyForm() })
const previewModal = reactive({ show: false, loading: false, agentCode: '', result: null })
const policyRules = {
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  policyCode: [{ required: true, message: '请输入策略编码', trigger: 'blur' }],
  targets: [{
    validator: () => {
      const modelIds = policyModal.form.targets.map(item => item.modelId)
      return modelIds.length > 0
        && modelIds.every(Boolean)
        && new Set(modelIds.map(String)).size === modelIds.length
    },
    message: '请完整选择候选模型，且不能重复',
    trigger: 'change',
  }],
}

const policyColumns = [
  { title: '策略名称', key: 'policyName', minWidth: 150 },
  { title: '策略编码', key: 'policyCode', minWidth: 160 },
  { title: '所需能力', key: 'requiredCapabilities', minWidth: 220, render: row => h('div', { class: 'tag-list' }, (row.requiredCapabilities || []).map(code => h(NTag, { size: 'small' }, { default: () => capabilityOptions.value.find(item => item.value === code)?.label || code }))) },
  { title: '候选数', key: 'targets', width: 80, render: row => row.targets?.length || 0 },
  { title: '状态', key: 'status', width: 80, render: row => statusOptions.value.find(item => item.value === row.status)?.label || row.status },
  { title: '操作', key: 'actions', width: 190, render: row => h('div', { class: 'actions' }, [canPreviewPolicies.value ? h(NButton, { text: true, class: 'text-info', onClick: () => openPreview() }, { default: () => '预览' }) : null, canEditPolicies.value ? h(NButton, { text: true, class: 'text-primary', onClick: () => openPolicyModal(row.id) }, { default: () => '编辑' }) : null, canEditPolicies.value ? h(NPopconfirm, { onPositiveClick: () => deletePolicy(row.id) }, { trigger: () => h(NButton, { text: true, class: 'text-error' }, { default: () => '删除' }), default: () => '确定删除该策略吗？' }) : null].filter(Boolean)) },
]
const invocationColumns = [
  { title: '时间', key: 'createTime', width: 165 },
  { title: 'Agent', key: 'agentCode', width: 140 },
  { title: '模型', key: 'providerModelId', width: 180 },
  { title: '路由来源', key: 'routeSource', width: 120 },
  { title: '结果', key: 'outcome', width: 100 },
  { title: '耗时(ms)', key: 'latencyMs', width: 100 },
  { title: '输入Token', key: 'promptTokens', width: 100 },
  { title: '输出Token', key: 'completionTokens', width: 100 },
  { title: 'HTTP', key: 'httpStatus', width: 80 },
  { title: '错误码', key: 'errorCode', width: 150, ellipsis: { tooltip: true } },
]

function createPolicyForm() {
  return { id: null, policyName: '', policyCode: '', requiredCapabilities: [], status: '0', remark: '', targets: [{ modelId: null, priority: 100, status: '0' }] }
}
async function loadPolicies() {
  policyLoading.value = true
  try {
    const res = await routePolicyPage(policyQuery)
    if (res.code === 200) {
      policies.value = res.data?.records || []
      policyTotal.value = Number(res.data?.total || 0)
    }
  }
  finally {
    policyLoading.value = false
  }
}
async function loadInvocations() {
  if (!canViewInvocations.value)
    return

  invocationLoading.value = true
  try {
    const params = {
      ...invocationQuery,
      startTime: formatLocalDateTime(invocationTimeRange.value?.[0]),
      endTime: formatLocalDateTime(invocationTimeRange.value?.[1]),
    }
    const [pageRes, summaryRes] = await Promise.all([invocationPage(params), invocationSummary(params)])
    if (pageRes.code === 200) {
      invocations.value = pageRes.data?.records || []
      invocationTotal.value = Number(pageRes.data?.total || 0)
    }
    if (summaryRes.code === 200)
      Object.assign(summary, summaryRes.data || {})
  }
  finally {
    invocationLoading.value = false
  }
}
async function loadCatalog() {
  const [models, providers] = await Promise.all([loadAllPages(modelPage), loadAllPages(providerPage)])
  modelCatalog.value = models
  providerCatalog.value = providers
}
async function loadAllPages(loader) {
  const pageSize = 100
  const records = []
  let pageNum = 1
  while (true) {
    const res = await loader({ pageNum, pageSize })
    if (res.code !== 200)
      break
    const pageRecords = res.data?.records || []
    records.push(...pageRecords)
    const total = Number(res.data?.total || records.length)
    if (!pageRecords.length || records.length >= total)
      break
    pageNum += 1
  }
  return records
}
async function openPolicyModal(id) {
  policyModal.form = createPolicyForm()
  if (id) {
    const res = await routePolicyGet(id)
    if (res.code === 200) {
      policyModal.form = {
        ...res.data,
        targets: (res.data.targets || []).map(item => ({ modelId: String(item.modelId), priority: item.priority, status: item.status })),
      }
    }
  }
  policyModal.show = true
}
function addTarget() {
  policyModal.form.targets.push({ modelId: null, priority: 100, status: '0' })
}
function removeTarget(index) {
  policyModal.form.targets.splice(index, 1)
}
async function savePolicy() {
  await policyFormRef.value?.validate()
  policyModal.saving = true
  try {
    const payload = { ...policyModal.form, targets: policyModal.form.targets.map(item => ({ ...item, modelId: String(item.modelId) })) }
    const res = payload.id ? await routePolicyUpdate(payload) : await routePolicyAdd(payload)
    if (res.code === 200) {
      window.$message.success('保存成功')
      policyModal.show = false
      await loadPolicies()
    }
  }
  finally {
    policyModal.saving = false
  }
}
async function deletePolicy(id) {
  const res = await routePolicyDelete(id)
  if (res.code === 200) {
    window.$message.success('删除成功')
    await loadPolicies()
  }
}
function openPreview() {
  previewModal.result = null
  previewModal.show = true
}
function resolveModelLabel(modelId) {
  return allModelOptions.value.find(item => String(item.value) === String(modelId))?.label || `模型 ${modelId}`
}
function formatLocalDateTime(timestamp) {
  if (timestamp == null)
    return undefined
  const date = new Date(timestamp)
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
function hasPermission(code) {
  if (routePermissionCodes.value.length)
    return routePermissionCodes.value.includes(code)
  const permissions = userStore.getDataPermission || []
  return !permissions.length || permissions.includes('**') || permissions.includes('*:*:*') || permissions.includes(code)
}
async function runPreview() {
  if (!previewModal.agentCode.trim()) {
    window.$message.warning('请输入 Agent 编码')
    return
  }
  previewModal.loading = true
  try {
    const res = await routePolicyPreview({ agentCode: previewModal.agentCode.trim() })
    if (res.code === 200)
      previewModal.result = res.data
  }
  finally {
    previewModal.loading = false
  }
}
function handlePolicyPageSize(value) {
  policyQuery.pageSize = value
  policyQuery.pageNum = 1
  loadPolicies()
}
function handleInvocationPageSize(value) {
  invocationQuery.pageSize = value
  invocationQuery.pageNum = 1
  loadInvocations()
}
onMounted(() => {
  loadCatalog()
  loadPolicies()
})
watch(activeTab, (value) => {
  if (value === 'invocations')
    loadInvocations()
})
watch(() => invocationQuery.providerId, () => {
  if (invocationQuery.modelId && !invocationModelOptions.value.some(option => option.value === invocationQuery.modelId))
    invocationQuery.modelId = null
})
</script>

<style scoped>
.routing-page {
  padding: 16px;
  min-height: 100%;
  background: var(--n-color, #fff);
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}
.search-input {
  width: 280px;
}
.filter-select {
  width: 160px;
}
.model-filter-select {
  width: 220px;
}
.time-filter {
  width: 360px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.summary-line {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 6px;
  color: var(--n-text-color-2);
}
.target-list {
  display: grid;
  width: 100%;
  gap: 8px;
}
.target-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 120px 110px 48px;
  gap: 8px;
  align-items: center;
}
.policy-modal {
  width: min(760px, calc(100vw - 32px));
}
.preview-modal {
  width: min(520px, calc(100vw - 32px));
}
.preview-result {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 6px;
}
.skipped-list {
  display: grid;
  gap: 8px;
  padding-top: 10px;
  margin-top: 6px;
  border-top: 1px solid var(--n-border-color);
}
.skipped-title {
  color: var(--n-text-color-2);
}
.skipped-item {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}
.tag-list,
.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
@media (max-width: 640px) {
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .search-input,
  .filter-select {
    width: 100%;
  }
  .model-filter-select,
  .time-filter {
    width: 100%;
  }
  .target-row {
    grid-template-columns: 1fr 110px;
  }
}
</style>
