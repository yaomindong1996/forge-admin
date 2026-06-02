<template>
  <div class="center-shell">
    <header>
      <div class="center-head-row">
        <n-button text @click="router.push(route.query.suiteCode ? `/app-center/suite/${route.query.suiteCode}` : '/app-center')">
          返回应用中心
        </n-button>
        <n-button secondary :disabled="!selectedConfig" @click="loadData">
          刷新
        </n-button>
      </div>
      <h1>业务报表看板</h1>
      <p>按业务对象展示数量、阶段、金额、流程结果和新增趋势。</p>
    </header>

    <section class="stats-toolbar">
      <n-space :wrap="true" align="center">
        <n-select
          v-model:value="selectedConfig"
          style="width: 280px"
          placeholder="选择业务应用"
          :options="appOptions"
          filterable
          @update:value="handleConfigChange"
        />
        <n-select
          v-model:value="trendPeriod"
          style="width: 120px"
          :options="periodOptions"
          @update:value="loadData"
        />
        <n-tag v-if="selectedObject" :bordered="false">
          {{ selectedObject.objectName || selectedObject.objectCode }}
        </n-tag>
      </n-space>
    </section>

    <n-spin :show="loading">
      <BusinessMetricPanel v-if="selectedConfig" :metrics="metrics" />
      <n-empty v-else description="请选择一个业务应用查看统计" />
    </n-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessAppList,
  businessObjectFields,
  businessObjectList,
  businessStatsMetrics,
} from '@/api/business-app'
import BusinessMetricPanel from './components/BusinessMetricPanel.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const apps = ref([])
const objects = ref([])
const fields = ref([])
const metrics = ref([])
const selectedConfig = ref(route.query.configKey || null)
const selectedObjectCode = ref(route.query.objectCode || null)
const trendPeriod = ref('day')

const periodOptions = [
  { label: '按天', value: 'day' },
  { label: '按周', value: 'week' },
  { label: '按月', value: 'month' },
]

const appOptions = computed(() => apps.value
  .filter(app => app.configKey)
  .map(app => ({
    label: `${app.appName || app.objectName || app.configKey}`,
    value: app.configKey,
    objectCode: app.objectCode,
  })))
const selectedApp = computed(() => apps.value.find(app => app.configKey === selectedConfig.value) || null)
const selectedObject = computed(() => {
  const objectCode = selectedObjectCode.value || selectedApp.value?.objectCode
  return objects.value.find(item => item.objectCode === objectCode) || null
})

onMounted(loadInitialData)

watch(() => route.query.configKey, (value) => {
  if (value && value !== selectedConfig.value) {
    selectedConfig.value = value
    handleConfigChange(value)
  }
})

async function loadInitialData() {
  await Promise.all([loadApps(), loadObjects()])
  resolveInitialSelection()
  await loadData()
}

async function loadApps() {
  const res = await businessAppList({
    status: 1,
    suiteCode: route.query.suiteCode || undefined,
  })
  apps.value = res.data || []
}

async function loadObjects() {
  const res = await businessObjectList({
    suiteCode: route.query.suiteCode || undefined,
    objectCode: route.query.objectCode || undefined,
  })
  objects.value = res.data || []
}

function resolveInitialSelection() {
  if (selectedConfig.value)
    return
  if (route.query.objectCode) {
    const app = apps.value.find(item => item.objectCode === route.query.objectCode && item.configKey)
    selectedConfig.value = app?.configKey || null
    selectedObjectCode.value = route.query.objectCode
    return
  }
  selectedConfig.value = appOptions.value[0]?.value || null
  selectedObjectCode.value = appOptions.value[0]?.objectCode || null
}

async function handleConfigChange(configKey) {
  selectedObjectCode.value = apps.value.find(app => app.configKey === configKey)?.objectCode || null
  await loadData()
}

async function loadData() {
  if (!selectedConfig.value) {
    metrics.value = []
    return
  }
  loading.value = true
  try {
    await loadSelectedFields()
    const res = await businessStatsMetrics(selectedConfig.value, buildMetricQuery())
    metrics.value = res.data || []
  }
  catch {
    metrics.value = []
  }
  finally {
    loading.value = false
  }
}

async function loadSelectedFields() {
  if (!selectedObject.value?.id) {
    fields.value = []
    return
  }
  const res = await businessObjectFields(selectedObject.value.id)
  fields.value = res.data || []
}

function buildMetricQuery() {
  const statusField = pickField(['documentStatus', 'approvalStatus', 'status', 'state'], ['状态'])
  const stageField = pickField(['stage', 'opportunityStage', 'salesStage'], ['阶段'])
  const amountField = pickField(['expectedAmount', 'amount', 'totalAmount', 'contractAmount'], ['金额', '费用', '价格'])
  return {
    metricTypes: ['OVERVIEW', 'TREND', 'FLOW_RESULT', ...(amountField ? ['SUM'] : [])],
    statusField,
    stageField,
    amountField,
    includeFlowResult: true,
    period: trendPeriod.value,
    days: 30,
  }
}

function pickField(codeCandidates = [], labelCandidates = []) {
  const list = fields.value || []
  const byCode = list.find(field => codeCandidates.some(code => normalize(field.fieldCode || field.field) === normalize(code)))
  if (byCode)
    return byCode.fieldCode || byCode.field
  const byLabel = list.find(field => labelCandidates.some(label => String(field.fieldName || field.label || '').includes(label)))
  return byLabel ? byLabel.fieldCode || byLabel.field : undefined
}

function normalize(value) {
  return String(value || '').replace(/_/g, '').toLowerCase()
}
</script>

<style scoped>
@import './shared-center.css';

.stats-toolbar {
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}
</style>
