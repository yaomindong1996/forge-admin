<template>
  <div class="excel-column-config">
    <header class="column-toolbar">
      <div class="column-toolbar__identity">
        <span class="column-toolbar__icon i-lucide:columns-3" aria-hidden="true" />
        <div>
          <div class="column-toolbar__title">
            设置 Excel 中显示的列
          </div>
          <div class="column-toolbar__description">
            {{ configName || '当前方案' }} · {{ configTypeLabel }} · 共 {{ dataList.length }} 列，列表顺序就是文件中的排列顺序
          </div>
        </div>
      </div>
      <div class="column-toolbar__actions">
        <NButton size="small" :disabled="loading || saving" @click="handleAdd">
          <template #icon>
            <span class="i-lucide:plus" aria-hidden="true" />
          </template>
          添加一列
        </NButton>
        <NButton type="primary" size="small" :loading="saving" :disabled="loading || !dirty" @click="handleSave">
          <template #icon>
            <span class="i-lucide:save" aria-hidden="true" />
          </template>
          保存全部更改
        </NButton>
        <NButton size="small" quaternary :disabled="saving" @click="requestClose">
          关闭
        </NButton>
      </div>
    </header>

    <div class="column-state" :class="{ 'is-dirty': dirty }" role="status">
      <span :class="dirty ? 'i-lucide:circle-alert' : 'i-lucide:circle-check'" aria-hidden="true" />
      <span>{{ dirty ? '有未保存的更改，保存后业务页面才会使用新列设置' : '当前列设置已保存' }}</span>
    </div>

    <div class="column-table-wrap">
      <NDataTable
        :columns="columns"
        :data="dataList"
        :row-key="row => row.key"
        :pagination="false"
        :loading="loading"
        :scroll-x="760"
        :max-height="520"
        size="medium"
      >
        <template #empty>
          <NEmpty description="还没有设置文件列">
            <template #extra>
              <NButton size="small" type="primary" @click="handleAdd">
                添加第一列
              </NButton>
            </template>
          </NEmpty>
        </template>
      </NDataTable>
    </div>

    <NModal
      v-model:show="showEditModal"
      preset="card"
      :title="editingIndex >= 0 ? '编辑文件列' : '添加文件列'"
      :style="{ width: 'min(680px, 94vw)' }"
      :mask-closable="false"
      :close-on-esc="false"
    >
      <NForm ref="editFormRef" :model="editForm" label-placement="top">
        <div class="column-edit-grid">
          <NFormItem
            label="Excel 表头"
            path="columnName"
            :rule="{ required: true, message: '请输入 Excel 表头', trigger: 'blur' }"
          >
            <NInput v-model:value="editForm.columnName" placeholder="用户在文件中看到的名称，如：用户姓名" />
          </NFormItem>
          <NFormItem
            label="对应数据字段"
            path="fieldName"
            :rule="{ required: true, message: '请输入对应数据字段', trigger: 'blur' }"
          >
            <NInput v-model:value="editForm.fieldName" placeholder="由技术人员提供，如：realName" />
            <template #feedback>
              用于从业务数据中取值，通常不需要日常修改。
            </template>
          </NFormItem>
          <NFormItem label="列宽" path="width">
            <NInputNumber v-model:value="editForm.width" :min="5" :max="100" style="width: 100%" />
            <template #feedback>
              姓名等短文本建议 15–20，备注等长文本建议 25–40。
            </template>
          </NFormItem>
        </div>

        <div class="column-purpose">
          <div class="column-purpose__title">
            这列用于
          </div>
          <div class="column-purpose__options">
            <label v-if="exportEnabled" class="switch-option">
              <NSwitch v-model:value="editForm.export" aria-label="显示在导出文件中" />
              <span><strong>显示在导出文件中</strong><small>关闭后，下载文件中不包含这一列</small></span>
            </label>
            <label v-if="importEnabled" class="switch-option">
              <NSwitch v-model:value="editForm.importable" aria-label="允许从导入文件填写" />
              <span><strong>允许从导入文件填写</strong><small>关闭后，导入时忽略这一列</small></span>
            </label>
            <label v-if="importEnabled && editForm.importable" class="switch-option">
              <NSwitch v-model:value="editForm.required" aria-label="导入时必须填写" />
              <span><strong>导入时必须填写</strong><small>空值会提示用户补充后再导入</small></span>
            </label>
          </div>
        </div>

        <details class="column-advanced" :open="advancedOpen">
          <summary>格式与校验（高级）</summary>
          <p>只有日期、金额、状态翻译或导入校验有特殊要求时才需要填写。</p>
          <div class="column-edit-grid">
            <NFormItem label="日期显示格式" path="dateFormat">
              <NInput v-model:value="editForm.dateFormat" placeholder="例如：yyyy-MM-dd" />
            </NFormItem>
            <NFormItem label="数字显示格式" path="numberFormat">
              <NInput v-model:value="editForm.numberFormat" placeholder="例如：#,##0.00" />
            </NFormItem>
            <NFormItem label="状态/类型翻译" path="dictType">
              <DictTypeSelect v-model:value="editForm.dictType" />
            </NFormItem>
            <NFormItem v-if="importEnabled" label="模板示例值" path="exampleValue">
              <NInput v-model:value="editForm.exampleValue" placeholder="例如：张三" />
            </NFormItem>
            <NFormItem v-if="importEnabled" label="校验规则" path="validationRule">
              <NInput v-model:value="editForm.validationRule" placeholder="由技术人员填写正则表达式" />
            </NFormItem>
            <NFormItem v-if="importEnabled" label="校验失败提示" path="validationMessage">
              <NInput v-model:value="editForm.validationMessage" placeholder="例如：手机号格式不正确" />
            </NFormItem>
          </div>
        </details>
      </NForm>
      <template #footer>
        <div class="modal-footer">
          <NButton @click="showEditModal = false">
            取消
          </NButton>
          <NButton type="primary" @click="handleConfirmEdit">
            {{ editingIndex >= 0 ? '完成修改' : '添加到列表' }}
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup>
import { NButton, NDataTable, NEmpty, NForm, NFormItem, NInput, NInputNumber, NModal, NSwitch } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTypeSelect from '@/components/lowcode-builder/shared/DictTypeSelect.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils/request'

const props = defineProps({
  configKey: { type: String, required: true },
  configName: { type: String, default: '' },
  configType: { type: String, default: 'BOTH' },
})

const emit = defineEmits(['close'])
const CONFIG_TYPE_DICT = 'sys_excel_config_type'
const dataList = ref([])
const showEditModal = ref(false)
const editFormRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)
const advancedOpen = ref(false)
const { getLabel } = useDict(CONFIG_TYPE_DICT)
const exportEnabled = computed(() => props.configType !== 'IMPORT')
const importEnabled = computed(() => props.configType !== 'EXPORT')
const configTypeLabel = computed(() => getLabel(CONFIG_TYPE_DICT, props.configType || 'BOTH') || props.configType || '导入导出')
const editForm = ref(createEmptyColumn())
let editingIndex = -1

const columns = computed(() => {
  const columnList = [
    {
      title: '顺序',
      key: 'orderNum',
      width: 62,
      align: 'center',
      render: (_row, index) => h('span', { class: 'column-order' }, index + 1),
    },
    {
      title: '文件中的列',
      key: 'columnName',
      minWidth: 210,
      render: (row, index) => h(SystemTableCell, {
        title: row.columnName || '未命名列',
        subtitle: `对应字段：${row.fieldName || '未设置'}`,
        interactive: true,
        tooltip: `编辑“${row.columnName || row.fieldName || '此列'}”`,
        onActivate: () => handleEdit(row, index),
      }),
    },
    {
      title: '使用方式',
      key: 'purpose',
      minWidth: 150,
      render: row => h('span', { class: 'purpose-text' }, formatPurpose(row)),
    },
    {
      title: '显示与校验',
      key: 'format',
      minWidth: 150,
      render: row => h('span', { class: 'format-text' }, formatRule(row)),
    },
  ]

  if (importEnabled.value) {
    columnList.push({
      title: '模板示例',
      key: 'exampleValue',
      width: 130,
      ellipsis: { tooltip: true },
      render: row => row.exampleValue || '-',
    })
  }

  columnList.push({
    title: '操作',
    key: 'action',
    width: 188,
    align: 'center',
    fixed: 'right',
    render: (row, index) => renderActions(row, index),
  })
  return columnList
})

function createEmptyColumn(orderNum = 1) {
  return {
    key: `new_${Date.now()}_${orderNum}`,
    id: null,
    fieldName: '',
    columnName: '',
    width: 20,
    orderNum,
    export: exportEnabled.value,
    importable: importEnabled.value,
    required: false,
    dateFormat: '',
    numberFormat: '',
    dictType: '',
    exampleValue: '',
    validationRule: '',
    validationMessage: '',
  }
}

function renderActions(row, index) {
  return h('div', { class: 'column-actions' }, [
    h('button', { type: 'button', class: 'column-action-link', onClick: () => handleEdit(row, index) }, '编辑'),
    h('span', { class: 'column-action-divider' }, '|'),
    h('button', {
      'type': 'button',
      'class': ['column-order-button', index === 0 ? 'is-disabled' : ''],
      'disabled': index === 0,
      'title': '向上移动',
      'aria-label': `向上移动${row.columnName || '此列'}`,
      'onClick': () => handleMoveUp(index),
    }, [h('span', { 'class': 'i-lucide:arrow-up', 'aria-hidden': 'true' })]),
    h('button', {
      'type': 'button',
      'class': ['column-order-button', index === dataList.value.length - 1 ? 'is-disabled' : ''],
      'disabled': index === dataList.value.length - 1,
      'title': '向下移动',
      'aria-label': `向下移动${row.columnName || '此列'}`,
      'onClick': () => handleMoveDown(index),
    }, [h('span', { 'class': 'i-lucide:arrow-down', 'aria-hidden': 'true' })]),
    h('span', { class: 'column-action-divider' }, '|'),
    h('button', { type: 'button', class: 'column-action-link is-danger', onClick: () => handleDelete(index) }, '删除'),
  ])
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/system/excel/column-config/list', { params: { configKey: props.configKey } })
    if (res?.code !== 200) {
      throw new Error(res?.respMsg || res?.message || '加载失败')
    }
    dataList.value = (res.data || []).map((item, index) => ({
      ...normalizeColumnForMode(item),
      key: `${item.id || 'new'}_${index}`,
      orderNum: index + 1,
    }))
    dirty.value = false
  }
  catch (error) {
    window.$message.error(error?.message || '加载文件列失败')
  }
  finally {
    loading.value = false
  }
}

function handleAdd() {
  editForm.value = createEmptyColumn(dataList.value.length + 1)
  editingIndex = -1
  advancedOpen.value = false
  showEditModal.value = true
}

function handleEdit(row, index) {
  editForm.value = normalizeColumnForMode({ ...row })
  editingIndex = index
  advancedOpen.value = hasAdvancedSettings(row)
  showEditModal.value = true
}

async function handleConfirmEdit() {
  try {
    await editFormRef.value?.validate()
    const isEditing = editingIndex >= 0
    const nextColumn = normalizeColumnForMode({ ...editForm.value })
    if (isEditing) {
      dataList.value[editingIndex] = nextColumn
    }
    else {
      dataList.value.push(nextColumn)
    }
    renumberColumns()
    dirty.value = true
    showEditModal.value = false
    window.$message.success(isEditing ? '列信息已更新，记得保存全部更改' : '已添加到列表，记得保存全部更改')
  }
  catch (error) {
    if (error?.message !== '验证失败') {
      window.$message.error('请检查文件列信息')
    }
  }
}

function handleDelete(index) {
  const column = dataList.value[index]
  window.$dialog.warning({
    title: `删除“${column?.columnName || '此列'}”？`,
    content: '保存全部更改后，这一列将从导出文件和导入模板中移除。',
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: () => {
      dataList.value.splice(index, 1)
      renumberColumns()
      dirty.value = true
    },
  })
}

function handleMoveUp(index) {
  if (index <= 0) {
    return
  }
  ;[dataList.value[index - 1], dataList.value[index]] = [dataList.value[index], dataList.value[index - 1]]
  renumberColumns()
  dirty.value = true
}

function handleMoveDown(index) {
  if (index >= dataList.value.length - 1) {
    return
  }
  ;[dataList.value[index], dataList.value[index + 1]] = [dataList.value[index + 1], dataList.value[index]]
  renumberColumns()
  dirty.value = true
}

function renumberColumns() {
  dataList.value.forEach((item, index) => {
    item.orderNum = index + 1
  })
}

async function handleSave() {
  if (saving.value) {
    return
  }
  if (!dataList.value.length) {
    window.$message.warning('请至少添加一列再保存')
    return
  }
  saving.value = true
  try {
    const payload = dataList.value.map(item => ({
      id: item.id,
      configKey: props.configKey,
      fieldName: item.fieldName,
      columnName: item.columnName,
      width: item.width,
      orderNum: item.orderNum,
      export: item.export,
      importable: item.importable,
      required: item.required,
      dateFormat: item.dateFormat || null,
      numberFormat: item.numberFormat || null,
      dictType: item.dictType || null,
      exampleValue: item.exampleValue || null,
      validationRule: item.validationRule || null,
      validationMessage: item.validationMessage || null,
    }))
    const res = await request.post('/system/excel/column-config/batch', payload, {
      params: { configKey: props.configKey },
      needTip: false,
    })
    if (res?.code !== 200) {
      throw new Error(res?.respMsg || res?.message || '保存失败')
    }
    window.$message.success('文件列设置已保存，新下载立即生效')
    await loadData()
  }
  catch (error) {
    window.$message.error(`保存文件列失败：${error?.message || '请稍后重试'}`)
  }
  finally {
    saving.value = false
  }
}

function requestClose() {
  if (!dirty.value) {
    emit('close')
    return
  }
  window.$dialog.warning({
    title: '还有未保存的更改',
    content: '现在关闭将丢失本次对文件列的调整。',
    positiveText: '放弃更改并关闭',
    negativeText: '继续编辑',
    onPositiveClick: () => emit('close'),
  })
}

function formatPurpose(row) {
  const labels = []
  if (exportEnabled.value && row.export !== false) {
    labels.push('导出显示')
  }
  if (importEnabled.value && row.importable !== false) {
    labels.push(row.required === true ? '导入必填' : '可导入')
  }
  return labels.length ? labels.join(' · ') : '暂不使用'
}

function formatRule(row) {
  if (row.dictType) {
    return '状态/类型翻译'
  }
  if (row.dateFormat) {
    return `日期：${row.dateFormat}`
  }
  if (row.numberFormat) {
    return `数字：${row.numberFormat}`
  }
  if (row.validationRule) {
    return '已设置导入校验'
  }
  return '按原值显示'
}

function hasAdvancedSettings(row) {
  return Boolean(row?.dateFormat || row?.numberFormat || row?.dictType || row?.exampleValue || row?.validationRule || row?.validationMessage)
}

function normalizeColumnForMode(column) {
  const importable = importEnabled.value && column.importable !== false
  return {
    ...column,
    export: exportEnabled.value && column.export !== false,
    importable,
    required: importable && column.required === true,
    exampleValue: importEnabled.value ? column.exampleValue || '' : '',
    validationRule: importEnabled.value ? column.validationRule || '' : '',
    validationMessage: importEnabled.value ? column.validationMessage || '' : '',
  }
}

onMounted(loadData)
watch(() => props.configKey, loadData)
</script>

<style scoped>
.excel-column-config {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.column-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.column-toolbar__identity {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.column-toolbar__icon {
  flex: 0 0 auto;
  color: var(--primary-color);
  font-size: 21px;
}

.column-toolbar__title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.column-toolbar__description {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.column-toolbar__actions,
.modal-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.column-state {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.column-state.is-dirty {
  color: var(--warning-color, #d46b08);
}

.column-table-wrap {
  min-height: 0;
}

.column-edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.column-edit-grid > :first-child {
  grid-column: 1 / -1;
}

.column-purpose {
  margin: 2px 0 14px;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-secondary);
}

.column-purpose__title {
  margin-bottom: 10px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.column-purpose__options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.switch-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
}

.switch-option :deep(.n-switch) {
  flex: 0 0 auto;
  margin-top: 1px;
}

.switch-option strong,
.switch-option small {
  display: block;
}

.switch-option strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
}

.switch-option small {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
}

.column-advanced {
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}

.column-advanced summary {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
}

.column-advanced > p {
  margin: 6px 0 10px;
  color: var(--text-tertiary);
  font-size: 12px;
}

:deep(.column-order),
:deep(.purpose-text),
:deep(.format-text) {
  color: var(--text-secondary);
  font-size: 12px;
}

:deep(.column-actions) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

:deep(.column-action-link),
:deep(.column-order-button) {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--primary-color);
  font: inherit;
  cursor: pointer;
}

:deep(.column-action-link:hover),
:deep(.column-order-button:hover) {
  color: var(--primary-color-hover, var(--primary-color));
}

:deep(.column-action-link.is-danger) {
  color: var(--error-color);
}

:deep(.column-order-button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 3px;
}

:deep(.column-order-button.is-disabled) {
  color: var(--text-disabled);
  cursor: not-allowed;
}

:deep(.column-action-divider) {
  color: var(--border-color, var(--border-light));
}

@media (max-width: 720px) {
  .column-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .column-toolbar__actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .column-edit-grid,
  .column-purpose__options {
    grid-template-columns: 1fr;
  }
}
</style>
