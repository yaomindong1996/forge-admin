<script setup>
import { NButton, NFormItem, NInput, NSelect } from 'naive-ui'
import { computed } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { describeFieldPath, groupedFieldSelectOptions } from '../fieldGroups'
import PrintFieldPicker from '../PrintFieldPicker.vue'
import { PRINT_MM_PRESETS, printMmOptions } from '../printMeasures'

const store = usePrintDesignerStore()
const visible = computed(() => store.activeElement?.type === 'STATIC_TABLE')
const cell = computed(() => store.activeTableCell)
const fieldOptions = computed(() => groupedFieldSelectOptions(store.catalog))
const imageFieldOptions = computed(() => {
  const imagePaths = new Set(store.catalog.filter(field => field.type === 'IMAGE').map(field => field.path))
  return groupedFieldSelectOptions(store.catalog)
    .map(group => ({
      ...group,
      children: (group.children || []).filter(option => imagePaths.has(option.value)),
    }))
    .filter(group => group.children?.length)
})
const firstFieldPath = computed(() => fieldOptions.value.find(group => group.children?.length)?.children?.[0]?.value || '')
const firstImageFieldPath = computed(() => imageFieldOptions.value.find(group => group.children?.length)?.children?.[0]?.value || '')
const bindingSource = computed(() => cell.value?.binding?.source || 'CONSTANT')
const contentType = computed(() => cell.value?.contentType === 'IMAGE' ? 'IMAGE' : 'TEXT')
const imageFileId = computed(() => (
  contentType.value === 'IMAGE' && bindingSource.value === 'CONSTANT'
    ? String(cell.value?.binding?.value || '')
    : ''
))
const fieldSummary = computed(() => {
  if (bindingSource.value !== 'FIELD' || !cell.value?.binding?.path)
    return ''
  return describeFieldPath(store.catalog, cell.value.binding.path)
})
const merged = computed(() => cell.value && (cell.value.rowSpan > 1 || cell.value.colSpan > 1))
const selectionCount = computed(() => store.selectedTableCells.length)
const canMerge = computed(() => selectionCount.value >= 2)
const alignments = [
  { label: '左', value: 'left' },
  { label: '中', value: 'center' },
  { label: '右', value: 'right' },
]
const verticalAlignments = [
  { label: '上', value: 'top' },
  { label: '中', value: 'middle' },
  { label: '下', value: 'bottom' },
]

function setContentType(type) {
  if (type === 'IMAGE') {
    store.patchSelectedTableCells({
      contentType: 'IMAGE',
      binding: bindingSource.value === 'FIELD'
        ? { source: 'FIELD', path: cell.value?.binding?.path || firstImageFieldPath.value }
        : { source: 'CONSTANT', value: imageFileId.value || '' },
    })
    return
  }
  store.patchSelectedTableCells({
    contentType: 'TEXT',
    binding: bindingSource.value === 'FIELD'
      ? { source: 'FIELD', path: cell.value?.binding?.path || firstFieldPath.value }
      : { source: 'CONSTANT', value: '' },
  })
}
function setSource(source) {
  if (contentType.value === 'IMAGE') {
    store.patchSelectedTableCells({
      contentType: 'IMAGE',
      binding: source === 'CONSTANT'
        ? { source, value: '' }
        : { source, path: firstImageFieldPath.value || firstFieldPath.value },
    })
    return
  }
  store.patchSelectedTableCells({ binding: source === 'CONSTANT' ? { source, value: '' } : { source, path: firstFieldPath.value } })
}
function setBindingValue(value) {
  store.patchSelectedTableCells({
    contentType: contentType.value,
    binding: { source: 'CONSTANT', value: value ?? '' },
  })
}
function setBindingPath(path) {
  const field = store.catalog.find(item => item.path === path)
  if (field?.type === 'COLLECTION') {
    store.notice = '子表/数组请从左侧「数据源」拖入「明细表」组件，不要绑到单个单元格'
    return
  }
  if (field?.type === 'IMAGE') {
    store.patchSelectedTableCells({
      contentType: 'IMAGE',
      binding: { source: 'FIELD', path },
    })
    return
  }
  store.patchSelectedTableCells({
    contentType: contentType.value === 'IMAGE' ? 'TEXT' : contentType.value,
    binding: { source: 'FIELD', path },
  })
}
function onImageUpload(value) {
  const fileId = String((Array.isArray(value) ? value[0] : value) || '')
  if (!fileId || fileId === imageFileId.value)
    return
  store.setStaticTableCellsImage(store.tableCellIds, fileId)
}
</script>

<template>
  <section v-if="visible" class="designer-group static-table-panel">
    <h3>空白表格</h3>
    <p class="muted tip">
      点选格子绑定字段；图片字段会直接出图。插行/列请用右键菜单。子表请从左侧拖「明细表」。表头/表体颜色在「样式」页分开设置，插入时不再自带表头。
    </p>

    <div class="table-toolbar" role="group" aria-label="表格操作">
      <NButton size="tiny" @click="store.addStaticTableRow()">
        末尾加行
      </NButton>
      <NButton size="tiny" @click="store.addStaticTableColumn()">
        末尾加列
      </NButton>
      <NButton size="tiny" :disabled="!selectionCount || store.activeElement.table.rows.length <= 1" @click="store.deleteStaticTableRow()">
        删行
      </NButton>
      <NButton size="tiny" :disabled="!selectionCount || store.activeElement.table.columns.length <= 1" @click="store.deleteStaticTableColumn()">
        删列
      </NButton>
      <NButton size="tiny" type="primary" :disabled="!canMerge" @click="store.mergeStaticTableSelection()">
        合并
      </NButton>
      <NButton size="tiny" :disabled="!merged" @click="store.splitStaticTableSelection()">
        拆分
      </NButton>
      <NButton size="tiny" secondary :disabled="!store.activeElement?.table?.cells?.length" title="把第一行标记为表头外观，不写入「表头」文字" @click="store.applyStaticTableHeaderStyle()">
        设为表头
      </NButton>
    </div>

    <p v-if="!selectionCount" class="muted tip">
      尚未选中单元格。点选一格后可绑定字段、改内容与样式。
    </p>
    <template v-else>
      <div class="cell-binding-card">
        <div class="cell-binding-head">
          <strong>单元格绑定</strong>
          <span>已选 {{ selectionCount }} 格 · 每格可单独设字段</span>
        </div>
        <NFormItem label="内容类型" size="small">
          <NSelect
            :value="contentType"
            :options="[{ label: '文字', value: 'TEXT' }, { label: '图片', value: 'IMAGE' }]"
            @update:value="setContentType"
          />
        </NFormItem>
        <NFormItem label="内容来源" size="small">
          <NSelect :value="bindingSource" :options="[{ label: '固定内容', value: 'CONSTANT' }, { label: '业务字段', value: 'FIELD' }]" @update:value="setSource" />
        </NFormItem>
        <template v-if="contentType === 'IMAGE'">
          <NFormItem v-if="bindingSource === 'CONSTANT'" label="上传图片" size="small" class="upload-item">
            <FileUpload
              :model-value="imageFileId"
              :limit="1"
              :multiple="false"
              :show-download="false"
              :file-type="['png', 'jpg', 'jpeg', 'webp']"
              business-type="print"
              upload-button-text="选择图片"
              @update:model-value="onImageUpload"
            />
          </NFormItem>
          <NFormItem v-else label="图片字段" size="small">
            <PrintFieldPicker
              :value="cell.binding?.path"
              :catalog="store.catalog"
              :types="['IMAGE']"
              placeholder="点击选择图片字段"
              @update:value="setBindingPath"
            />
          </NFormItem>
          <NButton v-if="contentType === 'IMAGE'" size="tiny" quaternary type="error" block @click="store.clearStaticTableCellsImage(store.tableCellIds)">
            清除图片
          </NButton>
        </template>
        <template v-else>
          <NFormItem v-if="bindingSource === 'CONSTANT'" label="固定内容" size="small">
            <NInput :value="String(cell.binding?.value ?? '')" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" @update:value="setBindingValue" />
          </NFormItem>
          <NFormItem v-else label="绑定字段" size="small" class="field-bind-item">
            <PrintFieldPicker
              :value="cell.binding?.path"
              :catalog="store.catalog"
              placeholder="点击选择字段（常用）"
              @update:value="setBindingPath"
            />
          </NFormItem>
          <p v-if="fieldSummary" class="binding-summary">
            当前：{{ fieldSummary }}
          </p>
        </template>
      </div>

      <h4>单元格尺寸</h4>
      <NFormItem label="水平对齐" size="small">
        <NSelect :value="cell?.style?.textAlign || 'left'" :options="alignments" @update:value="store.patchSelectedTableCellStyle({ textAlign: $event })" />
      </NFormItem>
      <NFormItem label="垂直对齐" size="small">
        <NSelect :value="cell?.style?.verticalAlign || 'middle'" :options="verticalAlignments" @update:value="store.patchSelectedTableCellStyle({ verticalAlign: $event })" />
      </NFormItem>
      <template v-if="cell">
        <NFormItem label="列宽 mm" size="small">
          <NSelect :value="store.activeElement.table.columns[cell.column].widthMm" :options="printMmOptions(store.activeElement.table.columns[cell.column].widthMm, PRINT_MM_PRESETS.track)" :filterable="false" :consistent-menu-width="false" @update:value="store.patchStaticTableTrack('column', cell.column, $event)" />
        </NFormItem>
        <NFormItem label="行高 mm" size="small">
          <NSelect :value="store.activeElement.table.rows[cell.row].heightMm" :options="printMmOptions(store.activeElement.table.rows[cell.row].heightMm, PRINT_MM_PRESETS.track)" :filterable="false" :consistent-menu-width="false" @update:value="store.patchStaticTableTrack('row', cell.row, $event)" />
        </NFormItem>
      </template>
    </template>
  </section>
</template>

<style scoped>
.tip {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.45;
}
.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 10px;
}
.static-table-panel h4 {
  margin: 10px 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
}
.cell-binding-card {
  margin: 8px 0 4px;
  padding: 10px 10px 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #356cde) 28%, #e2e8f0);
  border-radius: 8px;
  background: color-mix(in srgb, var(--primary-color, #356cde) 6%, #fff);
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: visible;
  position: relative;
  z-index: 2;
}
.cell-binding-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 6px;
}
.cell-binding-head strong {
  font-size: 13px;
  color: var(--text-primary, #0f172a);
}
.cell-binding-head span {
  font-size: 11px;
  color: var(--text-tertiary, #64748b);
}
.field-bind-item :deep(.picker-trigger) {
  min-height: 34px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #356cde) 35%, #cbd5e1);
  background: #fff;
  font-weight: 600;
}
.binding-summary {
  margin: 0 0 4px;
  color: var(--text-secondary, #475569);
  font-size: 11px;
  line-height: 1.4;
  word-break: break-all;
}
.upload-item :deep(.file-upload-wrapper) {
  width: 100%;
  max-width: 100%;
}
.upload-item :deep(.upload-dropzone) {
  min-height: 44px;
  padding: 8px;
}
.static-table-panel :deep(.n-form-item) {
  width: 100%;
  margin-bottom: 4px;
}
.static-table-panel :deep(.n-form-item-blank) {
  width: 100%;
}
</style>
