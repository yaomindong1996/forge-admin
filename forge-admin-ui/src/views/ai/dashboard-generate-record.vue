<template>
  <div class="dashboard-record-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{ list: 'get@/ai/dashboard-generate-record/page' }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :before-search="handleBeforeSearch"
      :scroll-x="1600"
      max-height="calc(100vh - 300px)"
      @load-list-success="handleLoadSuccess"
    />

    <n-modal
      v-model:show="detailVisible"
      preset="card"
      title="大屏生成记录详情"
      style="width: min(1080px, calc(100vw - 32px))"
    >
      <n-spin :show="detailLoading">
        <div v-if="currentRecord" class="record-detail">
          <n-descriptions label-placement="left" bordered :column="2" size="small">
            <n-descriptions-item label="大屏项目">
              {{ currentRecord.projectName || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="生成用户">
              {{ getUserLabel(currentRecord) }}
            </n-descriptions-item>
            <n-descriptions-item label="业务定义">
              {{ currentRecord.businessName || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="AI模型">
              {{ [currentRecord.providerName, currentRecord.modelName].filter(Boolean).join(' / ') || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="生成状态">
              <DictTag dict-type="ai_dashboard_generate_status" :value="currentRecord.status" size="small" />
            </n-descriptions-item>
            <n-descriptions-item label="生成时间">
              {{ currentRecord.createTime || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="组件统计">
              {{ currentRecord.componentCount || 0 }} 个组件，{{ currentRecord.boundCount || 0 }} 个动态绑定
            </n-descriptions-item>
            <n-descriptions-item label="耗时">
              {{ formatElapsed(currentRecord.elapsedMs) }}
            </n-descriptions-item>
          </n-descriptions>

          <n-tabs type="line" animated class="detail-tabs">
            <n-tab-pane name="prompt" tab="提示词">
              <pre class="code-block">{{ currentRecord.prompt || '-' }}</pre>
            </n-tab-pane>
            <n-tab-pane name="summary" tab="校验摘要">
              <pre class="code-block">{{ formatJson(currentRecord.validationSummaryJson) }}</pre>
            </n-tab-pane>
            <n-tab-pane name="response" tab="生成结果">
              <pre class="code-block">{{ formatJson(currentRecord.responseJson) }}</pre>
            </n-tab-pane>
            <n-tab-pane name="request" tab="请求上下文">
              <pre class="code-block">{{ formatJson(currentRecord.requestJson) }}</pre>
            </n-tab-pane>
            <n-tab-pane v-if="currentRecord.errorMessage" name="error" tab="错误信息">
              <pre class="error-block">{{ currentRecord.errorMessage }}</pre>
            </n-tab-pane>
          </n-tabs>
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <NButton @click="detailVisible = false">
            关闭
          </NButton>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NEllipsis } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { formatDateTime, request } from '@/utils'

defineOptions({ name: 'AiDashboardGenerateRecord' })

const { dict } = useDict('ai_dashboard_generate_status')

const crudRef = ref(null)
const recordStats = ref({
  total: 0,
})
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentRecord = ref(null)

const statusOptions = computed(() => dict.value.ai_dashboard_generate_status || [])

const searchSchema = [
  {
    field: 'projectName',
    label: '大屏项目',
    type: 'input',
    props: { placeholder: '请输入大屏项目名称' },
  },
  {
    field: 'businessName',
    label: '业务定义',
    type: 'input',
    props: { placeholder: '请输入业务定义名称' },
  },
  {
    field: 'userKeyword',
    label: '生成用户',
    type: 'input',
    props: { placeholder: '用户名/姓名/用户ID' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: statusOptions,
    },
  },
  {
    field: 'modelName',
    label: '模型',
    type: 'input',
    props: { placeholder: '请输入模型名称' },
  },
  {
    field: 'timeRange',
    label: '生成时间',
    type: 'daterange',
    startPlaceholder: '开始时间',
    endPlaceholder: '结束时间',
    clearable: true,
    format: 'yyyy-MM-dd HH:mm:ss',
    valueFormat: 'yyyy-MM-dd HH:mm:ss',
    props: { type: 'datetimerange' },
  },
]

const tableColumns = computed(() => [
  {
    prop: 'projectName',
    label: '大屏项目',
    width: 190,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.projectName || '-' })
    },
  },
  {
    prop: 'generatedTitle',
    label: '生成标题',
    width: 180,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.generatedTitle || '-' })
    },
  },
  {
    prop: 'realName',
    label: '生成用户',
    width: 140,
    render(row) {
      return getUserLabel(row)
    },
  },
  {
    prop: 'businessName',
    label: '业务定义',
    width: 160,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.businessName || '-' })
    },
  },
  {
    prop: 'modelName',
    label: '模型',
    width: 180,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => [row.providerName, row.modelName].filter(Boolean).join(' / ') || '-' })
    },
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    render(row) {
      return h(DictTag, { dictType: 'ai_dashboard_generate_status', value: row.status, size: 'small' })
    },
  },
  {
    prop: 'componentCount',
    label: '组件/绑定',
    width: 110,
    render(row) {
      return `${row.componentCount || 0}/${row.boundCount || 0}`
    },
  },
  {
    prop: 'elapsedMs',
    label: '耗时',
    width: 100,
    render(row) {
      return formatElapsed(row.elapsedMs)
    },
  },
  {
    prop: 'createTime',
    label: '生成时间',
    width: 170,
  },
  {
    prop: 'prompt',
    label: '提示词摘要',
    minWidth: 220,
    render(row) {
      return h(NEllipsis, { lineClamp: 1 }, { default: () => row.prompt || '-' })
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 90,
    fixed: 'right',
    render(row) {
      return h(
        NButton,
        { size: 'small', text: true, type: 'primary', onClick: () => openDetail(row) },
        { default: () => '详情' },
      )
    },
  },
])

function handleBeforeSearch(params) {
  const result = { ...params }
  if (params.timeRange && params.timeRange.length === 2) {
    result.startTime = formatDateTime(params.timeRange[0])
    result.endTime = formatDateTime(params.timeRange[1])
    delete result.timeRange
  }
  return result
}

function handleLoadSuccess({ total }) {
  recordStats.value.total = total || 0
}

function getUserLabel(row) {
  const name = row?.realName || row?.username
  return name ? `${name}（${row.userId || '-'}）` : row?.userId || '-'
}

function formatElapsed(value) {
  if (value === null || value === undefined)
    return '-'
  if (value < 1000)
    return `${value}ms`
  return `${(value / 1000).toFixed(1)}s`
}

function formatJson(value) {
  if (!value)
    return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  }
  catch {
    return value
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  currentRecord.value = null
  try {
    const res = await request.get(`/ai/dashboard-generate-record/${row.id}`)
    currentRecord.value = res.data
  }
  catch (error) {
    window.$message.error(error?.message || '获取生成记录详情失败')
  }
  finally {
    detailLoading.value = false
  }
}
</script>

<style scoped>
.dashboard-record-page {
  min-height: 100%;
}

.record-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-tabs {
  margin-top: 4px;
}

.code-block,
.error-block {
  max-height: 460px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 8px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.error-block {
  background: #2b1113;
  color: #fecaca;
}
</style>
