<template>
  <div class="business-studio">
    <section class="business-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          业务语义资产
        </p>
        <h1 class="hero-title">
          业务定义管理台
        </h1>
        <p class="hero-description">
          把业务目标、指标口径和可用数据集维护为统一语义，让大屏生成从“描述驱动”升级为“数据资产驱动”。
        </p>
      </div>

      <div class="hero-stats">
        <div v-for="card in statCards" :key="card.key" class="hero-stat-card">
          <div class="hero-stat-label">
            {{ card.label }}
          </div>
          <div class="hero-stat-value">
            {{ card.value }}
          </div>
          <div class="hero-stat-note">
            {{ card.note }}
          </div>
        </div>
      </div>
    </section>

    <section class="business-panel">
      <div class="panel-toolbar">
        <div>
          <p class="panel-kicker">
            业务资产
          </p>
          <h3>业务定义清单</h3>
        </div>
        <div class="toolbar-actions">
          <NInput
            v-model:value="queryForm.businessName"
            clearable
            placeholder="搜索业务名称或编码"
            @keydown.enter="applySearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </NInput>
          <NSelect
            v-model:value="queryForm.status"
            clearable
            placeholder="状态"
            :options="statusOptions"
          />
          <NButton type="primary" @click="applySearch">
            搜索
          </NButton>
          <NButton @click="handleReset">
            重置
          </NButton>
          <NButton type="primary" secondary @click="handleOpenAddBusiness">
            新增业务定义
          </NButton>
        </div>
      </div>

      <AiCrudPage
        ref="crudRef"
        class="business-crud"
        :api-config="{
          list: 'get@/data/business/page',
          detail: 'get@/data/business/:id',
          add: 'post@/data/business',
          update: 'put@/data/business',
          delete: 'delete@/data/business/:id',
        }"
        :show-search="false"
        :hide-toolbar="true"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-form="beforeRenderForm"
        :before-render-detail="beforeRenderDetail"
        :before-submit="beforeSubmit"
        :load-detail-on-edit="true"
        row-key="id"
        :bordered="false"
        :striped="false"
        :scroll-x="1480"
        max-height="calc(100vh - 250px)"
        :edit-grid-cols="12"
        edit-label-placement="top"
        edit-form-class="data-business-edit-form"
        modal-width="min(1180px, calc(100vw - 32px))"
        add-button-text="新增业务定义"
        @load-list-success="handleBusinessLoadSuccess"
      >
        <template #form-businessEditor="{ formData, field }">
          <div class="business-editor">
            <section class="business-editor-section">
              <div class="business-section-title">
                基础信息
              </div>
              <div class="business-form-grid business-form-grid--base">
                <label class="business-field">
                  <span>业务编码 <em>*</em></span>
                  <NInput
                    v-model:value="formData.businessCode"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="如 sales_overview"
                  />
                </label>
                <label class="business-field">
                  <span>业务名称 <em>*</em></span>
                  <NInput
                    v-model:value="formData.businessName"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="如销售经营分析"
                  />
                </label>
                <label class="business-field">
                  <span>状态</span>
                  <div class="status-switch-row">
                    <NSwitch
                      v-model:value="formData.status"
                      :checked-value="1"
                      :unchecked-value="0"
                      :disabled="isBusinessEditorReadonly(field)"
                    />
                    <small>{{ formData.status === 0 ? '禁用' : '启用' }}</small>
                  </div>
                </label>
              </div>
            </section>

            <section class="business-editor-section">
              <div class="business-section-title">
                业务语义
              </div>
              <div class="business-form-grid business-form-grid--semantic">
                <label class="business-field business-field--full">
                  <span>业务定义描述 <em>*</em></span>
                  <NInput
                    v-model:value="formData.businessDesc"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="描述业务对象、业务边界、核心管理动作和数据使用范围"
                  />
                </label>
                <label class="business-field">
                  <span>分析目标</span>
                  <NInput
                    v-model:value="formData.analysisGoal"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="例如：监控销售额、订单量、转化率、区域贡献和异常波动"
                  />
                </label>
                <label class="business-field">
                  <span>指标口径</span>
                  <NInput
                    v-model:value="formData.metricDefinition"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="定义核心指标、单位、聚合方式和展示口径"
                  />
                </label>
                <label class="business-field">
                  <span>分析维度</span>
                  <NInput
                    v-model:value="formData.dimensionDefinition"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="例如：按时间、区域、渠道、客户、产品、组织等维度分析"
                  />
                </label>
                <label class="business-field">
                  <span>大屏使用建议</span>
                  <NInput
                    v-model:value="formData.usageGuide"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="isBusinessEditorReadonly(field)"
                    placeholder="说明哪些数据集适合做概览指标、趋势、排行、表格或地图"
                  />
                </label>
              </div>
            </section>

            <section class="business-editor-section">
              <div class="dataset-binding-toolbar">
                <div class="business-section-title">
                  绑定数据集
                </div>
                <NButton
                  v-if="!isBusinessEditorReadonly(field)"
                  type="primary"
                  secondary
                  @click="addDatasetBinding(formData.datasets, next => formData.datasets = next)"
                >
                  <template #icon>
                    <i class="i-material-symbols:add-rounded" />
                  </template>
                  添加数据集
                </NButton>
              </div>

              <n-empty
                v-if="!formData.datasets || formData.datasets.length === 0"
                description="暂未绑定数据集"
                size="small"
              />
              <n-data-table
                v-else
                :columns="datasetBindingColumns(next => formData.datasets = next, isBusinessEditorReadonly(field))"
                :data="getDatasetBindingRows(formData.datasets)"
                :pagination="false"
                :scroll-x="980"
                size="small"
                striped
              />
            </section>
          </div>
        </template>
      </AiCrudPage>
    </section>
  </div>
</template>

<script setup>
import { NButton, NInput, NInputNumber, NProgress, NSelect, NSwitch, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { deleteDataBusiness } from '@/api/data/business'
import { getDataDatasetList } from '@/api/data/dataset'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'DataBusiness' })

const { dict } = useDict('sys_enable_disable')

const crudRef = ref(null)
const datasetOptions = ref([])
const datasetOptionMap = computed(() => new Map(datasetOptions.value.map(item => [item.value, item])))

const queryForm = reactive({
  businessName: '',
  status: null,
})

const businessStats = reactive({
  total: 0,
  active: 0,
  disabled: 0,
  datasets: 0,
})

const statusOptions = computed(() => dict.value.sys_enable_disable || [])

const statCards = computed(() => [
  {
    key: 'total',
    label: '筛选结果',
    value: businessStats.total,
    note: '匹配当前筛选条件的业务定义总数',
  },
  {
    key: 'active',
    label: '当前页启用',
    value: businessStats.active,
    note: '可在大屏生成时选择使用',
  },
  {
    key: 'disabled',
    label: '当前页禁用',
    value: businessStats.disabled,
    note: '不会作为生成上下文使用',
  },
  {
    key: 'datasets',
    label: '当前页绑定',
    value: businessStats.datasets,
    note: '当前页业务定义绑定的数据集总数',
  },
])

const tableColumns = computed(() => [
  {
    prop: 'businessName',
    label: '业务定义',
    width: 340,
    render: row => h('div', { class: 'business-name-card' }, [
      h('div', { class: 'business-name-row' }, [
        h('div', { class: 'business-name' }, row.businessName),
        h(DictTag, {
          dictType: 'sys_enable_disable',
          value: String(row.status),
          size: 'small',
        }),
      ]),
      h('div', { class: 'business-code' }, row.businessCode),
      h('div', { class: 'business-desc' }, row.businessDesc || '暂无业务定义描述'),
    ]),
  },
  {
    prop: 'analysisGoal',
    label: '分析目标',
    width: 300,
    ellipsis: true,
    render: row => row.analysisGoal || '-',
  },
  {
    prop: 'datasetNames',
    label: '绑定数据集',
    width: 240,
    render: row => renderDatasetSummary(row),
  },
  {
    prop: 'aiReadiness',
    label: '配置完整度',
    width: 170,
    render: row => renderReadiness(row),
  },
  {
    prop: 'metricDefinition',
    label: '指标口径',
    width: 300,
    ellipsis: true,
    render: row => row.metricDefinition || '-',
  },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEditBusiness },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDeleteBusiness },
    ],
  },
])

const editSchema = computed(() => [
  {
    field: 'businessEditor',
    label: '',
    type: 'slot',
    slotName: 'businessEditor',
    span: 12,
    showLabel: false,
    showFeedback: false,
  },
])

onMounted(loadDatasetOptions)

async function loadDatasetOptions() {
  try {
    const res = await getDataDatasetList()
    const records = res.data || []
    datasetOptions.value = records.map(item => ({
      label: `${item.datasetName}${item.datasetCode ? ` (${item.datasetCode})` : ''}`,
      value: item.id,
      datasetName: item.datasetName,
      datasetCode: item.datasetCode,
      datasetType: item.datasetType,
    }))
  }
  catch (error) {
    window.$message?.error(error?.message || '加载数据集失败')
  }
}

function applySearch() {
  crudRef.value?.search({
    businessName: queryForm.businessName || undefined,
    status: queryForm.status ?? undefined,
  })
}

function handleReset() {
  queryForm.businessName = ''
  queryForm.status = null
  crudRef.value?.search({})
}

function handleOpenAddBusiness() {
  crudRef.value?.showAdd()
}

function hasText(value) {
  return !!String(value || '').trim()
}

function getDatasetCount(source) {
  if (Array.isArray(source?.datasets)) {
    return source.datasets.filter(item => item.datasetId).length
  }
  return source?.datasetCount || 0
}

function getBusinessReadiness(source = {}) {
  let score = 0
  const suggestions = []
  const semanticFields = [
    { key: 'businessDesc', score: 14, suggestion: '补充业务定义描述，说明业务边界和管理对象。' },
    { key: 'analysisGoal', score: 12, suggestion: '补充分析目标，说明大屏要回答什么问题。' },
    { key: 'metricDefinition', score: 16, suggestion: '补充指标口径，明确单位、聚合和同比环比口径。' },
    { key: 'dimensionDefinition', score: 12, suggestion: '补充分析维度，如时间、区域、渠道、组织等。' },
    { key: 'usageGuide', score: 11, suggestion: '补充大屏使用建议，说明数据集适合做哪些组件。' },
  ]

  semanticFields.forEach((item) => {
    if (hasText(source[item.key])) {
      score += item.score
    }
    else {
      suggestions.push(item.suggestion)
    }
  })

  const datasetCount = getDatasetCount(source)
  if (datasetCount > 0) {
    score += Math.min(24, 14 + datasetCount * 3)
  }
  else {
    suggestions.push('至少绑定一个数据集，否则大屏只能生成静态组件。')
  }

  if (Array.isArray(source.datasets) && source.datasets.some(item => hasText(item.usageRemark))) {
    score += 6
  }
  else if (datasetCount > 0) {
    suggestions.push('为数据集补充用途说明，可提升动态绑定成功率。')
  }

  if ((source.status ?? 1) === 1) {
    score += 5
  }

  const normalizedScore = Math.min(100, score)
  const level = normalizedScore >= 80 ? 'high' : normalizedScore >= 55 ? 'medium' : 'low'
  const label = level === 'high' ? '准备充分' : level === 'medium' ? '可生成' : '待完善'

  return {
    score: normalizedScore,
    level,
    label,
    suggestions: suggestions.slice(0, 3),
  }
}

function getReadinessColor(level) {
  if (level === 'high')
    return '#0f766e'
  if (level === 'medium')
    return '#d97706'
  return '#dc2626'
}

function renderReadiness(row) {
  const readiness = getBusinessReadiness(row)
  return h('div', { class: 'readiness-cell' }, [
    h('div', { class: 'readiness-cell-head' }, [
      h('strong', { style: { color: getReadinessColor(readiness.level) } }, `${readiness.score}`),
      h('span', readiness.label),
    ]),
    h(NProgress, {
      type: 'line',
      percentage: readiness.score,
      height: 6,
      borderRadius: 3,
      fillBorderRadius: 3,
      showIndicator: false,
      color: getReadinessColor(readiness.level),
    }),
  ])
}

function handleEditBusiness(row) {
  crudRef.value?.showEdit({ ...row, __modalTitle: '编辑业务定义' })
}

function handleBusinessLoadSuccess({ list, total }) {
  const rows = list || []
  businessStats.total = total || 0
  businessStats.active = rows.filter(item => item.status === 1).length
  businessStats.disabled = rows.filter(item => item.status === 0).length
  businessStats.datasets = rows.reduce((sum, item) => sum + (item.datasetCount || 0), 0)
}

function beforeRenderForm(formData) {
  return normalizeBusinessForm(formData || {})
}

function beforeRenderDetail(detailData) {
  return normalizeBusinessForm(detailData || {})
}

function normalizeBusinessForm(sourceData) {
  return {
    status: 1,
    ...sourceData,
    datasets: normalizeDatasetBindings(sourceData.datasets),
  }
}

function normalizeDatasetBindings(bindings) {
  if (!Array.isArray(bindings) || bindings.length === 0) {
    return []
  }
  return bindings.map((item, index) => ({
    __key: item.__key || `${item.datasetId || 'dataset'}-${index}-${Date.now()}`,
    id: item.id,
    datasetId: item.datasetId,
    datasetName: item.datasetName,
    datasetCode: item.datasetCode,
    datasetType: item.datasetType,
    description: item.description,
    isPrimary: item.isPrimary ?? (index === 0 ? 1 : 0),
    sort: item.sort ?? index,
    usageRemark: item.usageRemark || '',
  }))
}

function beforeSubmit(formData) {
  delete formData.businessEditor
  delete formData.businessGuide

  if (!formData.businessCode) {
    window.$message?.error('请输入业务编码')
    return false
  }
  if (!formData.businessName) {
    window.$message?.error('请输入业务名称')
    return false
  }
  if (!formData.businessDesc) {
    window.$message?.error('请输入业务定义描述')
    return false
  }
  const datasets = normalizeDatasetBindings(formData.datasets).filter(item => item.datasetId)
  if (!datasets.length) {
    window.$message?.error('请至少绑定一个数据集')
    return false
  }
  const datasetIds = new Set()
  for (const item of datasets) {
    if (datasetIds.has(item.datasetId)) {
      window.$message?.error('绑定数据集不能重复')
      return false
    }
    datasetIds.add(item.datasetId)
  }
  if (!datasets.some(item => item.isPrimary === 1)) {
    datasets[0].isPrimary = 1
  }
  formData.datasets = datasets.map((item, index) => ({
    id: item.id,
    datasetId: item.datasetId,
    isPrimary: item.isPrimary === 1 ? 1 : 0,
    sort: item.sort ?? index,
    usageRemark: item.usageRemark || null,
  }))
  formData.status = formData.status ?? 1
  return formData
}

function isBusinessEditorReadonly(field) {
  return !!(field?.disabled || field?.readonly || field?.props?.disabled || field?.props?.readonly)
}

function addDatasetBinding(value, updateValue) {
  const next = normalizeDatasetBindings(value)
  next.push({
    __key: `dataset-${Date.now()}`,
    datasetId: null,
    isPrimary: next.length === 0 ? 1 : 0,
    sort: next.length,
    usageRemark: '',
  })
  updateValue(next)
}

function getDatasetBindingRows(value) {
  const rows = normalizeDatasetBindings(value)
  rows.forEach((item, index) => {
    item.__owner = rows
    item.__index = index
  })
  return rows
}

function removeDatasetBinding(rowIndex, value, updateValue) {
  const next = normalizeDatasetBindings(value).filter((_, index) => index !== rowIndex)
  if (next.length && !next.some(item => item.isPrimary === 1)) {
    next[0].isPrimary = 1
  }
  updateValue(cleanDatasetBindings(next))
}

function updateDatasetBinding(row, key, value, updateValue) {
  row[key] = value
  updateValue(cleanDatasetBindings(row.__owner))
}

function markPrimaryDataset(row, updateValue) {
  row.__owner.forEach((item) => {
    item.isPrimary = item.__key === row.__key ? 1 : 0
  })
  updateValue(cleanDatasetBindings(row.__owner))
}

function cleanDatasetBindings(rows) {
  return rows.map(item => ({
    __key: item.__key,
    id: item.id,
    datasetId: item.datasetId,
    datasetName: item.datasetName,
    datasetCode: item.datasetCode,
    datasetType: item.datasetType,
    description: item.description,
    isPrimary: item.isPrimary,
    sort: item.sort,
    usageRemark: item.usageRemark,
  }))
}

function getDatasetSelectOptions(row) {
  if (!row?.datasetId || datasetOptionMap.value.has(row.datasetId)) {
    return datasetOptions.value
  }
  const label = `${row.datasetName || `数据集 ${row.datasetId}`}${row.datasetCode ? ` (${row.datasetCode})` : ''}`
  return [
    {
      label,
      value: row.datasetId,
      datasetName: row.datasetName,
      datasetCode: row.datasetCode,
      datasetType: row.datasetType,
    },
    ...datasetOptions.value,
  ]
}

function renderDatasetSummary(row) {
  const names = String(row.datasetNames || '')
    .split('、')
    .map(item => item.trim())
    .filter(Boolean)
  if (!names.length) {
    return h('span', { class: 'dataset-empty' }, `${row.datasetCount || 0} 个数据集`)
  }
  return h('div', { class: 'dataset-summary' }, [
    h('div', { class: 'dataset-summary-count' }, `${row.datasetCount || names.length} 个数据集`),
    h('div', { class: 'dataset-summary-tags' }, names.slice(0, 3).map(name =>
      h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => name }),
    ).concat(names.length > 3
      ? [h(NTag, { size: 'small', bordered: false }, { default: () => `+${names.length - 3}` })]
      : [])),
  ])
}

function datasetBindingColumns(updateValue, readonly = false) {
  return [
    {
      title: '数据集',
      key: 'datasetId',
      width: 280,
      render: row => h(NSelect, {
        value: row.datasetId,
        options: getDatasetSelectOptions(row),
        filterable: true,
        clearable: true,
        disabled: readonly,
        placeholder: '选择已发布数据集',
        onUpdateValue: value => updateDatasetBinding(row, 'datasetId', value, updateValue),
      }),
    },
    {
      title: '主数据集',
      key: 'isPrimary',
      width: 100,
      render: row => h(NSwitch, {
        value: row.isPrimary === 1,
        disabled: readonly,
        onUpdateValue: (checked) => {
          if (checked) {
            markPrimaryDataset(row, updateValue)
          }
        },
      }),
    },
    {
      title: '排序',
      key: 'sort',
      width: 150,
      render: row => h('div', { class: 'sort-editor' }, [
        h('span', { class: 'sort-index' }, `#${(row.sort ?? row.__index ?? 0) + 1}`),
        h(NInputNumber, {
          value: row.sort,
          min: 0,
          size: 'small',
          showButton: false,
          disabled: readonly,
          onUpdateValue: value => updateDatasetBinding(row, 'sort', value ?? 0, updateValue),
        }),
      ]),
    },
    {
      title: '用途说明',
      key: 'usageRemark',
      width: 360,
      render: row => h(NInput, {
        value: row.usageRemark,
        size: 'small',
        disabled: readonly,
        placeholder: getDatasetRemarkPlaceholder(row.datasetId),
        onUpdateValue: value => updateDatasetBinding(row, 'usageRemark', value, updateValue),
      }),
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      fixed: 'right',
      render: row => h(NButton, {
        quaternary: true,
        type: 'error',
        disabled: readonly,
        onClick: () => removeDatasetBinding(row.__index, row.__owner, updateValue),
      }, {
        default: () => '删除',
      }),
    },
  ]
}

function getDatasetRemarkPlaceholder(datasetId) {
  const option = datasetOptionMap.value.get(datasetId)
  if (!option) {
    return '例如：用于趋势、排行、概览指标或明细表格'
  }
  return `${option.datasetName} 的大屏使用场景`
}

function handleDeleteBusiness(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定删除业务定义“${row.businessName}”吗？删除后 report-ui 将不能再基于该业务定义生成大屏。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataBusiness(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          crudRef.value?.refresh()
        }
        else {
          window.$message?.error(res.msg || '删除失败')
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '删除失败')
      }
    },
  })
}
</script>

<style scoped>
.business-studio {
  background: #f8fafc;
  min-height: 100%;
  padding: 10px;
}

.business-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(460px, 0.9fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 12px 16px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
  text-transform: none;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 600;
}

.hero-description {
  overflow: hidden;
  max-width: 720px;
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.hero-stat-card {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  background: #fff;
}

.hero-stat-label {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stat-value {
  margin-top: 2px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.hero-stat-note {
  display: none;
}

.business-panel {
  position: relative;
  margin-top: 0;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
  padding: 10px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.panel-toolbar h3 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.toolbar-actions {
  display: grid;
  grid-template-columns: minmax(200px, 1fr) 130px auto auto auto;
  gap: 6px;
  align-items: center;
  min-width: min(700px, 100%);
}

.business-name-card {
  display: grid;
  gap: 3px;
}

.business-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.business-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.business-code {
  color: #0f766e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 600;
}

.business-desc {
  color: #64748b;
  font-size: 11px;
  line-height: 1.4;
}

.dataset-summary {
  display: grid;
  gap: 4px;
}

.dataset-summary-count,
.dataset-empty {
  color: #0f766e;
  font-size: 11px;
  font-weight: 600;
}

.dataset-summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.readiness-cell {
  display: grid;
  gap: 5px;
}

.readiness-cell-head {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.readiness-cell-head strong {
  font-size: 16px;
  font-weight: 700;
}

.readiness-cell-head span {
  color: #64748b;
  font-size: 11px;
}

.sort-editor {
  display: grid;
  grid-template-columns: 38px minmax(68px, 1fr);
  gap: 6px;
  align-items: center;
}

.sort-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 24px;
  border-radius: 6px;
  background: #e0f2f1;
  color: #0f766e;
  font-size: 11px;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.sort-editor :deep(.n-input-number) {
  width: 100%;
}

.sort-editor :deep(.n-input__input-el) {
  text-align: center;
  font-weight: 700;
}

.business-editor {
  display: grid;
  gap: 12px;
}

.business-editor-section {
  padding-bottom: 12px;
  border-bottom: 1px solid #edf2f7;
}

.business-editor-section:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.business-section-title {
  margin-bottom: 10px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.business-form-grid {
  display: grid;
  gap: 12px;
}

.business-form-grid--base {
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1fr) 180px;
}

.business-form-grid--semantic {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.business-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.business-field--full {
  grid-column: 1 / -1;
}

.business-field > span {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.business-field em {
  color: #dc2626;
  font-style: normal;
}

.status-switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
}

.status-switch-row small {
  color: #64748b;
  font-size: 12px;
}

.dataset-binding-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

:deep(.data-business-edit-form.ai-crud-edit-form) {
  --ai-crud-form-item-gap: 0;
}

:deep(.data-business-edit-form .n-form-item) {
  margin-bottom: 0;
}

:deep(.data-business-edit-form .n-form-item-blank) {
  min-height: 0;
}

:deep(.data-business-edit-form .n-form-item-feedback-wrapper) {
  display: none;
  min-height: 0;
}

:deep(.business-crud .ai-crud-main) {
  background: transparent;
}

:deep(.business-crud .ai-crud-table) {
  overflow: hidden;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 16px rgb(15 23 42 / 3%);
}

:deep(.business-crud .n-data-table-th) {
  padding: 9px 12px;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  background: #f8fafc;
  border-bottom: 1px solid #e8ecf1;
}

:deep(.business-crud .n-data-table-tr:nth-child(even) td) {
  background: rgb(248 250 252 / 50%);
}

:deep(.business-crud .n-data-table-tr:hover td) {
  background: rgb(241 245 249 / 80%);
}

@media (max-width: 1200px) {
  .business-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .dataset-binding-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .business-form-grid--base,
  .business-form-grid--semantic {
    grid-template-columns: 1fr;
  }

  .panel-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    grid-template-columns: 1fr 1fr;
    min-width: 0;
  }
}
</style>
