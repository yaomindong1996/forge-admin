<template>
  <div class="dimension-studio">
    <section class="dimension-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          Dimension Registry
        </p>
        <h1 class="hero-title">
          数据维度管理台
        </h1>
        <p class="hero-description">
          把状态、地区、组织、渠道等编码类字段统一维护为可复用维度。手动维度适合稳定字典，SQL 维度适合随业务库自动同步。
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

    <section class="dimension-panel">
      <div class="panel-toolbar">
        <div>
          <p class="panel-kicker">
            Translation Assets
          </p>
          <h3>维度清单</h3>
        </div>
        <div class="toolbar-actions">
          <NInput
            v-model:value="queryForm.dimensionName"
            clearable
            placeholder="搜索维度名称或编码"
            @keydown.enter="applySearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </NInput>
          <NSelect
            v-model:value="queryForm.sourceType"
            clearable
            placeholder="录入方式"
            :options="sourceTypeOptions"
          />
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
          <NButton type="primary" secondary @click="handleOpenAddDimension">
            新增维度
          </NButton>
        </div>
      </div>

      <AiCrudPage
        ref="crudRef"
        class="dimension-crud"
        :api-config="{
          list: 'get@/data/dimension/page',
          detail: 'get@/data/dimension/:id',
          add: 'post@/data/dimension',
          update: 'put@/data/dimension',
          delete: 'delete@/data/dimension/:id',
        }"
        :show-search="false"
        :hide-toolbar="true"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-form="beforeRenderForm"
        :before-render-detail="beforeRenderDetail"
        :before-submit="beforeSubmit"
        row-key="id"
        :bordered="false"
        :striped="false"
        :scroll-x="1460"
        max-height="calc(100vh - 250px)"
        :edit-grid-cols="12"
        edit-label-placement="top"
        edit-form-class="data-dimension-edit-form"
        modal-width="min(1120px, calc(100vw - 32px))"
        add-button-text="新增维度"
        @load-list-success="handleDimensionLoadSuccess"
      >
        <template #form-dimensionGuide="{ formData }">
          <div class="dimension-guide">
            <div class="dimension-guide__main">
              <div class="dimension-guide__eyebrow">
                {{ formData.sourceType === 'SQL' ? 'SQL Sync' : 'Manual Dictionary' }}
              </div>
              <div class="dimension-guide__title">
                {{ formData.sourceType === 'SQL' ? '从数据源同步编码和值名称' : '维护一份稳定、可复用的翻译字典' }}
              </div>
              <div class="dimension-guide__desc">
                {{ getSourceGuide(formData.sourceType) }}
              </div>
            </div>
            <div class="dimension-guide__facts">
              <div class="guide-fact">
                <span>编码</span>
                <strong>{{ formData.dimensionCode || '待填写' }}</strong>
              </div>
              <div class="guide-fact">
                <span>录入方式</span>
                <strong>{{ getSourceTypeLabel(formData.sourceType) }}</strong>
              </div>
              <div class="guide-fact">
                <span>绑定数据源</span>
                <strong>{{ formData.connectionId ? getConnectionName(formData.connectionId) : '无需绑定' }}</strong>
              </div>
            </div>
          </div>
        </template>

        <template #form-sqlText="{ value, updateValue }">
          <SqlEditor
            class="dimension-sql-editor"
            :value="value"
            placeholder="SELECT code AS value, name AS label FROM sys_dict WHERE status = 1"
            @update:value="updateValue"
          />
        </template>

        <template #form-sqlPreviewAction="{ formData }">
          <div class="dimension-sql-preview-bar">
            <div>
              <div class="dimension-sql-preview-bar__title">
                先预览再保存
              </div>
              <div class="dimension-sql-preview-bar__desc">
                按当前 SQL 读取前 20 条，并用值字段 / 显示字段生成维度翻译预览。
              </div>
            </div>
            <NButton
              type="primary"
              secondary
              :loading="sqlPreviewLoading"
              @click="handlePreviewDimensionSql(formData)"
            >
              预览维度内容
            </NButton>
          </div>
        </template>
      </AiCrudPage>
    </section>

    <n-modal
      v-model:show="itemModalVisible"
      preset="card"
      :title="itemModalTitle"
      :style="{ width: 'min(1180px, calc(100vw - 32px))' }"
      :segmented="{ content: 'soft' }"
      :mask-closable="false"
    >
      <div class="item-modal">
        <div class="item-modal__toolbar">
          <div>
            <div class="item-modal__title">
              {{ selectedDimension?.dimensionName || '维度值' }}
            </div>
            <div class="item-modal__desc">
              {{ itemModalEditable ? '手动维护编码和值名称，保存后可被数据集字段绑定使用。' : 'SQL 来源维度值由同步任务生成，这里只做查看和手动触发同步。' }}
            </div>
          </div>
          <div class="item-modal__actions">
            <NButton v-if="itemModalEditable" @click="handleAddItemRow">
              新增值
            </NButton>
            <NButton
              v-if="selectedDimension?.sourceType === 'SQL'"
              type="primary"
              secondary
              :loading="itemLoading"
              @click="handleSyncSelectedDimension"
            >
              同步维度值
            </NButton>
          </div>
        </div>

        <n-data-table
          :columns="itemColumns"
          :data="dimensionItems"
          :loading="itemLoading"
          :pagination="{ pageSize: 12 }"
          :scroll-x="980"
          size="small"
          striped
        />
      </div>

      <template #footer>
        <div class="modal-footer">
          <NButton @click="itemModalVisible = false">
            关闭
          </NButton>
          <NButton
            v-if="itemModalEditable"
            type="primary"
            :loading="itemSaving"
            @click="handleSaveItems"
          >
            保存维度值
          </NButton>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="sqlPreviewVisible"
      preset="card"
      title="维度SQL预览"
      :style="{ width: 'min(1180px, calc(100vw - 32px))' }"
      :segmented="{ content: 'soft' }"
    >
      <div class="dimension-preview-summary">
        <div>
          <span>值字段</span>
          <strong>{{ sqlPreviewValueColumn || '-' }}</strong>
        </div>
        <div>
          <span>显示字段</span>
          <strong>{{ sqlPreviewLabelColumn || '-' }}</strong>
        </div>
        <div>
          <span>预览条数</span>
          <strong>{{ sqlPreviewRows.length }}</strong>
        </div>
      </div>
      <n-data-table
        :columns="sqlPreviewColumns"
        :data="sqlPreviewRows"
        :loading="sqlPreviewLoading"
        :pagination="{ pageSize: 10 }"
        :scroll-x="sqlPreviewScrollX"
        size="small"
        striped
      />
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NInput, NInputNumber, NSelect } from 'naive-ui'
import { computed, h, reactive, ref } from 'vue'
import { getDataConnectionList } from '@/api/data/connection'
import {
  deleteDataDimension,
  getDataDimensionItems,
  saveDataDimensionItems,
  syncDataDimensionItems,
} from '@/api/data/dimension'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import SqlEditor from '@/components/SqlEditor.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'DataDimension' })

const { dict } = useDict('data_dimension_source_type', 'sys_enable_disable')

const crudRef = ref(null)
const connectionOptions = ref([])
const itemModalVisible = ref(false)
const itemLoading = ref(false)
const itemSaving = ref(false)
const selectedDimension = ref(null)
const dimensionItems = ref([])
const sqlPreviewVisible = ref(false)
const sqlPreviewLoading = ref(false)
const sqlPreviewColumns = ref([])
const sqlPreviewRows = ref([])
const sqlPreviewScrollX = ref(0)
const sqlPreviewValueColumn = ref('')
const sqlPreviewLabelColumn = ref('')

const queryForm = reactive({
  dimensionName: '',
  sourceType: null,
  status: null,
})

const dimensionStats = reactive({
  total: 0,
  manual: 0,
  sql: 0,
  active: 0,
})

const sourceTypeOptions = computed(() => dict.value.data_dimension_source_type || [])

const statusOptions = computed(() => dict.value.sys_enable_disable || [])

const statCards = computed(() => [
  {
    key: 'total',
    label: '筛选结果',
    value: dimensionStats.total,
    note: '匹配当前筛选条件的维度资产总数',
  },
  {
    key: 'manual',
    label: '当前页手动维度',
    value: dimensionStats.manual,
    note: '适合状态、枚举和稳定字典类翻译',
  },
  {
    key: 'sql',
    label: '当前页 SQL 维度',
    value: dimensionStats.sql,
    note: '可从业务库同步读取最新维度值',
  },
  {
    key: 'active',
    label: '当前页启用',
    value: dimensionStats.active,
    note: '可被数据集字段绑定并参与翻译',
  },
])

const tableColumns = computed(() => [
  {
    prop: 'dimensionName',
    label: '维度资产',
    width: 300,
    render: row => h('div', { class: 'dimension-name-card' }, [
      h('div', { class: 'dimension-name-row' }, [
        h('div', { class: 'dimension-name' }, row.dimensionName),
        h(DictTag, {
          dictType: 'data_dimension_source_type',
          value: row.sourceType,
          size: 'small',
        }),
      ]),
      h('div', { class: 'dimension-code' }, row.dimensionCode),
      h('div', { class: 'dimension-desc' }, row.description || '暂无描述'),
    ]),
  },
  {
    prop: 'sourceType',
    label: '录入方式',
    width: 150,
    render: row => h('div', { class: 'source-type-cell' }, [
      h('strong', getSourceTypeLabel(row.sourceType)),
      h('span', row.sourceType === 'SQL' ? '数据源同步' : '后台维护'),
    ]),
  },
  {
    prop: 'connectionName',
    label: '同步来源',
    width: 260,
    render: row => h('div', { class: 'source-cell' }, [
      h('div', { class: 'source-cell__name' }, row.sourceType === 'SQL' ? row.connectionName || getConnectionName(row.connectionId) : '手动维护'),
      h('div', { class: 'source-cell__desc' }, row.sourceType === 'SQL'
        ? `值列：${row.valueColumn || '第1列'}，显示列：${row.labelColumn || '第2列'}`
        : '直接编辑维度值，无需数据源'),
    ]),
  },
  {
    prop: 'itemCount',
    label: '维度值',
    width: 100,
    render: row => h('span', { class: 'item-count' }, row.itemCount ?? 0),
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    render: row => h(DictTag, {
      dictType: 'sys_enable_disable',
      value: String(row.status),
      size: 'small',
    }),
  },
  { prop: 'lastSyncTime', label: '最近同步', width: 170, render: row => row.lastSyncTime || '-' },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  {
    prop: 'action',
    label: '操作',
    width: 320,
    fixed: 'right',
    maxActionButtons: 3,
    actions: [
      { label: '维度值', key: 'items', type: 'info', onClick: handleOpenItems },
      { label: '同步', key: 'sync', type: 'warning', visible: row => row.sourceType === 'SQL', onClick: handleSyncDimension },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEditDimension },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDeleteDimension },
    ],
  },
])

const editSchema = computed(() => [
  {
    field: 'dimensionGuide',
    label: '',
    type: 'slot',
    slotName: 'dimensionGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'dimensionCode',
    label: '维度编码',
    type: 'input',
    span: 4,
    rules: [{ required: true, message: '请输入维度编码', trigger: 'blur' }],
    props: { placeholder: '如 order_status' },
  },
  {
    field: 'dimensionName',
    label: '维度名称',
    type: 'input',
    span: 4,
    rules: [{ required: true, message: '请输入维度名称', trigger: 'blur' }],
    props: { placeholder: '如订单状态' },
  },
  {
    field: 'sourceType',
    label: '录入方式',
    type: 'radio',
    span: 4,
    defaultValue: 'MANUAL',
    props: { options: sourceTypeOptions },
    onChange: ({ value, formData }) => handleSourceTypeChange(value, formData),
  },
  {
    field: 'connectionId',
    label: '数据连接',
    type: 'select',
    span: 4,
    props: { placeholder: '请选择数据连接', options: connectionOptions.value, filterable: true, clearable: true },
    vIf: formData => formData.sourceType === 'SQL',
  },
  {
    field: 'valueColumn',
    label: '值字段列名',
    type: 'input',
    span: 4,
    props: { placeholder: '不填默认取第1列' },
    vIf: formData => formData.sourceType === 'SQL',
  },
  {
    field: 'labelColumn',
    label: '显示字段列名',
    type: 'input',
    span: 4,
    props: { placeholder: '不填默认取第2列' },
    vIf: formData => formData.sourceType === 'SQL',
  },
  {
    field: 'sqlText',
    label: '同步SQL',
    type: 'slot',
    slotName: 'sqlText',
    span: 12,
    rules: [{ required: true, message: '请输入同步SQL', trigger: 'blur' }],
    vIf: formData => formData.sourceType === 'SQL',
  },
  {
    field: 'sqlPreviewAction',
    label: '',
    type: 'slot',
    slotName: 'sqlPreviewAction',
    span: 12,
    showFeedback: false,
    vIf: formData => formData.sourceType === 'SQL',
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    span: 4,
    defaultValue: 1,
    props: { options: statusOptions },
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 8,
    props: { placeholder: '描述维度口径、使用范围或同步来源', rows: 3 },
  },
])

const itemModalTitle = computed(() => {
  if (!selectedDimension.value) {
    return '维度值'
  }
  return `维度值 - ${selectedDimension.value.dimensionName}`
})
const itemModalEditable = computed(() => selectedDimension.value?.sourceType === 'MANUAL')

const itemColumns = computed(() => [
  {
    title: '维度值',
    key: 'itemValue',
    width: 220,
    render: row => renderItemInput(row, 'itemValue', '如 1 / ACTIVE / CN'),
  },
  {
    title: '显示名称',
    key: 'itemLabel',
    width: 240,
    render: row => renderItemInput(row, 'itemLabel', '如 启用 / 中国'),
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: row => itemModalEditable.value
      ? h(NSelect, {
          value: String(row.status ?? 1),
          options: statusOptions.value,
          size: 'small',
          onUpdateValue: value => row.status = Number(value),
        })
      : h(DictTag, {
          dictType: 'sys_enable_disable',
          value: String(row.status),
          size: 'small',
        }),
  },
  {
    title: '排序',
    key: 'sort',
    width: 120,
    render: row => itemModalEditable.value
      ? h(NInputNumber, {
          value: row.sort ?? 0,
          min: 0,
          size: 'small',
          style: { width: '100%' },
          onUpdateValue: value => row.sort = value ?? 0,
        })
      : row.sort ?? 0,
  },
  {
    title: '扩展JSON',
    key: 'extraJson',
    width: 260,
    render: row => renderItemInput(row, 'extraJson', '{"color":"green"}'),
  },
  {
    title: '操作',
    key: 'action',
    width: 90,
    fixed: 'right',
    render: row => itemModalEditable.value
      ? h(NButton, {
          size: 'small',
          type: 'error',
          text: true,
          onClick: () => handleRemoveItemRow(row),
        }, { default: () => '删除' })
      : '-',
  },
])

loadConnectionOptions()

async function loadConnectionOptions() {
  try {
    const res = await getDataConnectionList()
    if (res.code === 200 && Array.isArray(res.data)) {
      connectionOptions.value = res.data.map(item => ({
        label: item.connectionName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('Failed to load connections', error)
  }
}

function applySearch() {
  crudRef.value?.search({
    dimensionName: queryForm.dimensionName?.trim() || undefined,
    sourceType: queryForm.sourceType || undefined,
    status: queryForm.status ?? undefined,
  })
}

function handleReset() {
  queryForm.dimensionName = ''
  queryForm.sourceType = null
  queryForm.status = null
  crudRef.value?.search({})
}

function handleOpenAddDimension() {
  crudRef.value?.showAdd()
}

function handleEditDimension(row) {
  crudRef.value?.showEdit({ ...row, __modalTitle: '编辑维度' })
}

function handleDimensionLoadSuccess({ list, total }) {
  const rows = list || []
  dimensionStats.total = total || 0
  dimensionStats.manual = rows.filter(item => item.sourceType === 'MANUAL').length
  dimensionStats.sql = rows.filter(item => item.sourceType === 'SQL').length
  dimensionStats.active = rows.filter(item => item.status === 1).length
}

function beforeRenderForm(formData) {
  return normalizeDimensionForm(formData || {})
}

function beforeRenderDetail(detailData) {
  return normalizeDimensionForm(detailData || {})
}

function normalizeDimensionForm(sourceData) {
  return {
    sourceType: 'MANUAL',
    status: 1,
    ...sourceData,
  }
}

function beforeSubmit(formData) {
  delete formData.dimensionGuide
  delete formData.sqlPreviewAction

  if (!formData.dimensionCode) {
    window.$message?.error('请输入维度编码')
    return false
  }
  if (!formData.dimensionName) {
    window.$message?.error('请输入维度名称')
    return false
  }

  formData.sourceType = formData.sourceType || 'MANUAL'
  if (formData.sourceType === 'SQL') {
    if (!formData.connectionId) {
      window.$message?.error('请选择数据连接')
      return false
    }
    if (!formData.sqlText) {
      window.$message?.error('请输入同步SQL')
      return false
    }
  }
  else {
    formData.connectionId = null
    formData.sqlText = null
    formData.valueColumn = null
    formData.labelColumn = null
  }
  return formData
}

function handleSourceTypeChange(sourceType, formData) {
  if (sourceType === 'MANUAL') {
    formData.connectionId = null
    formData.sqlText = null
    formData.valueColumn = null
    formData.labelColumn = null
  }
}

function getSourceGuide(sourceType) {
  if (sourceType === 'SQL') {
    return 'SQL 需要返回值列和显示列。未填写列名时，系统默认取结果集第 1 列作为值、第 2 列作为显示名称。'
  }
  return '手动维度适合低频变化的枚举字典，可直接维护编码、显示名称、排序和扩展信息。'
}

function getSourceTypeLabel(sourceType) {
  const item = dict.value.data_dimension_source_type?.find(d => d.value === sourceType)
  return item?.label || '手动维护'
}

function getConnectionName(connectionId) {
  return connectionOptions.value.find(item => item.value === connectionId)?.label || connectionId || '-'
}

async function handlePreviewDimensionSql(formData) {
  if (!formData.connectionId) {
    window.$message?.warning('请选择数据连接')
    return
  }
  if (!formData.sqlText) {
    window.$message?.warning('请输入同步SQL')
    return
  }

  sqlPreviewVisible.value = true
  sqlPreviewLoading.value = true
  sqlPreviewColumns.value = []
  sqlPreviewRows.value = []
  sqlPreviewScrollX.value = 0
  sqlPreviewValueColumn.value = ''
  sqlPreviewLabelColumn.value = ''

  try {
    const res = await request.post('/data/dataset/preview-sql', {
      connectionId: formData.connectionId,
      sqlText: formData.sqlText,
      maxRows: 20,
    })
    if (res.code !== 200) {
      window.$message?.error(res.msg || '维度SQL预览失败')
      return
    }

    const columns = res.data?.columns || []
    const valueColumn = formData.valueColumn || columns[0]
    const labelColumn = formData.labelColumn || columns[1] || columns[0]
    sqlPreviewValueColumn.value = valueColumn || ''
    sqlPreviewLabelColumn.value = labelColumn || ''
    sqlPreviewRows.value = (res.data?.rows || []).map(row => ({
      __dimensionValue: valueColumn ? row[valueColumn] : '',
      __dimensionLabel: labelColumn ? row[labelColumn] : '',
      ...row,
    }))
    sqlPreviewColumns.value = [
      {
        title: '维度值',
        key: '__dimensionValue',
        width: 180,
        fixed: 'left',
        render: row => row.__dimensionValue ?? '',
      },
      {
        title: '显示名称',
        key: '__dimensionLabel',
        width: 220,
        fixed: 'left',
        render: row => row.__dimensionLabel ?? '',
      },
      ...columns.map(column => ({
        title: column,
        key: column,
        width: 160,
        ellipsis: { tooltip: true },
        render: row => row[column] ?? '',
      })),
    ]
    sqlPreviewScrollX.value = Math.max(columns.length * 160 + 400, 920)
    window.$message?.success(`预览成功，共 ${sqlPreviewRows.value.length} 条`)
  }
  catch (error) {
    window.$message?.error(error?.message || '维度SQL预览失败')
  }
  finally {
    sqlPreviewLoading.value = false
  }
}

async function handleOpenItems(row) {
  selectedDimension.value = row
  itemModalVisible.value = true
  await loadDimensionItems(row)
}

async function loadDimensionItems(row) {
  if (!row?.id) {
    return
  }
  itemLoading.value = true
  dimensionItems.value = []
  try {
    const res = await getDataDimensionItems(row.id)
    if (res.code === 200 && Array.isArray(res.data)) {
      dimensionItems.value = res.data.map((item, index) => ({
        ...item,
        sort: item.sort ?? index,
        status: item.status ?? 1,
      }))
    }
    else {
      window.$message?.error(res.msg || '加载维度值失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '加载维度值失败')
  }
  finally {
    itemLoading.value = false
  }
}

function handleAddItemRow() {
  dimensionItems.value.push({
    itemValue: '',
    itemLabel: '',
    sort: dimensionItems.value.length,
    status: 1,
    extraJson: '',
  })
}

function handleRemoveItemRow(row) {
  const index = dimensionItems.value.indexOf(row)
  if (index >= 0) {
    dimensionItems.value.splice(index, 1)
  }
}

function renderItemInput(row, key, placeholder) {
  if (!itemModalEditable.value) {
    return row[key] || '-'
  }
  return h(NInput, {
    value: row[key],
    size: 'small',
    placeholder,
    onUpdateValue: value => row[key] = value,
  })
}

async function handleSaveItems() {
  if (!selectedDimension.value?.id) {
    return
  }

  const normalizedItems = []
  const values = new Set()
  for (const [index, row] of dimensionItems.value.entries()) {
    const itemValue = typeof row.itemValue === 'string' ? row.itemValue.trim() : ''
    const itemLabel = typeof row.itemLabel === 'string' ? row.itemLabel.trim() : ''
    const isEmpty = !itemValue && !itemLabel && !row.extraJson
    if (isEmpty) {
      continue
    }
    if (!itemValue) {
      window.$message?.error(`第${index + 1}行缺少维度值`)
      return
    }
    if (!itemLabel) {
      window.$message?.error(`第${index + 1}行缺少显示名称`)
      return
    }
    if (values.has(itemValue)) {
      window.$message?.error(`维度值重复：${itemValue}`)
      return
    }
    values.add(itemValue)
    normalizedItems.push({
      itemValue,
      itemLabel,
      sort: row.sort ?? index,
      status: row.status ?? 1,
      extraJson: row.extraJson || null,
    })
  }

  itemSaving.value = true
  try {
    const res = await saveDataDimensionItems(selectedDimension.value.id, normalizedItems)
    if (res.code === 200) {
      window.$message?.success('维度值已保存')
      await loadDimensionItems(selectedDimension.value)
      crudRef.value?.refresh()
    }
    else {
      window.$message?.error(res.msg || '保存维度值失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '保存维度值失败')
  }
  finally {
    itemSaving.value = false
  }
}

async function handleSyncDimension(row) {
  selectedDimension.value = row
  await syncDimension(row, false)
}

async function handleSyncSelectedDimension() {
  if (selectedDimension.value) {
    await syncDimension(selectedDimension.value, true)
  }
}

async function syncDimension(row, reloadItems) {
  if (!row?.id) {
    return
  }
  itemLoading.value = true
  try {
    const res = await syncDataDimensionItems(row.id)
    if (res.code === 200) {
      window.$message?.success(`同步完成，共 ${res.data?.length || 0} 个维度值`)
      if (reloadItems || itemModalVisible.value) {
        dimensionItems.value = (res.data || []).map((item, index) => ({
          ...item,
          sort: item.sort ?? index,
          status: item.status ?? 1,
        }))
      }
      crudRef.value?.refresh()
    }
    else {
      window.$message?.error(res.msg || '同步维度值失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '同步维度值失败')
  }
  finally {
    itemLoading.value = false
  }
}

function handleDeleteDimension(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定删除维度“${row.dimensionName}”吗？已被数据集字段引用的维度无法删除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataDimension(row.id)
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
.dimension-studio {
  background: #f8fafc;
  min-height: 100%;
  padding: 10px;
}

.dimension-hero {
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

.dimension-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
}

.hero-main {
  min-width: 0;
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
  max-width: 760px;
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

.dimension-panel {
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
  grid-template-columns: 200px 130px 110px auto auto auto;
  gap: 6px;
  align-items: center;
}

.dimension-crud {
  border-radius: 12px;
  background: #fff;
}

:deep(.dimension-crud .ai-crud-main) {
  background: transparent;
}

:deep(.dimension-crud .ai-crud-table) {
  overflow: hidden;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 16px rgb(15 23 42 / 3%);
}

:deep(.dimension-crud .n-data-table-th) {
  padding: 9px 12px;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  background: #f8fafc;
  border-bottom: 1px solid #e8ecf1;
}

:deep(.dimension-crud .n-data-table-tr:nth-child(even) td) {
  background: rgb(248 250 252 / 50%);
}

:deep(.dimension-crud .n-data-table-tr:hover td) {
  background: rgb(241 245 249 / 80%);
}

:global(.data-dimension-edit-form .n-form-item:has(.dimension-guide)),
:global(.data-dimension-edit-form .n-form-item:has(.dimension-sql-editor)),
:global(.data-dimension-edit-form .n-form-item:has(.dimension-sql-preview-bar)) {
  grid-column: 1 / -1 !important;
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dimension-edit-form .n-form-item:has(.dimension-guide) .n-form-item-blank),
:global(.data-dimension-edit-form .n-form-item:has(.dimension-sql-editor) .n-form-item-blank),
:global(.data-dimension-edit-form .n-form-item:has(.dimension-sql-preview-bar) .n-form-item-blank) {
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dimension-edit-form .dimension-sql-editor),
:global(.data-dimension-edit-form .dimension-sql-editor .sql-editor) {
  width: 100% !important;
  max-width: none !important;
}

.dimension-name-card {
  min-width: 0;
}

.dimension-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dimension-name {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dimension-code,
.dimension-desc,
.source-cell__desc,
.source-type-cell span {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
}

.dimension-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-type-cell,
.source-cell {
  display: flex;
  flex-direction: column;
}

.source-type-cell strong,
.source-cell__name {
  color: #0f172a;
  font-weight: 600;
  font-size: 13px;
}

.item-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 24px;
  border-radius: 6px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 12px;
  font-weight: 600;
}

.dimension-guide {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid rgb(100 116 139 / 16%);
  border-radius: 14px;
  background: linear-gradient(135deg, rgb(15 23 42 / 3%), transparent), #f8fafc;
}

.dimension-guide__eyebrow {
  color: #0f766e;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.dimension-guide__title {
  margin-top: 4px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.dimension-guide__desc {
  margin-top: 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.dimension-guide__facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.guide-fact {
  min-width: 0;
  padding: 10px;
  border-radius: 10px;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgb(148 163 184 / 16%);
}

.guide-fact span {
  display: block;
  color: #64748b;
  font-size: 11px;
}

.guide-fact strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #0f172a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dimension-sql-preview-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: linear-gradient(135deg, rgb(15 23 42 / 4%), transparent), #f8fafc;
}

.dimension-sql-preview-bar__title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.dimension-sql-preview-bar__desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}

.dimension-preview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.dimension-preview-summary div {
  padding: 10px 12px;
  border: 1px solid rgb(148 163 184 / 18%);
  border-radius: 10px;
  background: #f8fafc;
}

.dimension-preview-summary span {
  display: block;
  color: #64748b;
  font-size: 11px;
}

.dimension-preview-summary strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #0f172a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-modal__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.item-modal__title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.item-modal__desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.item-modal__actions,
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1200px) {
  .dimension-hero,
  .dimension-guide {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .toolbar-actions {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
  }

  .panel-toolbar {
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .dimension-studio {
    padding: 12px;
  }

  .toolbar-actions,
  .dimension-guide__facts,
  .dimension-preview-summary {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-description {
    white-space: normal;
  }

  .dimension-sql-preview-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
