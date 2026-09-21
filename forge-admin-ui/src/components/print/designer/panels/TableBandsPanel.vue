<script setup>
import { NButton, NColorPicker, NFormItem, NInput, NSelect } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../../protocol/printColor'
import { findSurface } from '../commands'
import { groupedFieldSelectOptions } from '../fieldGroups'
import PrintFieldPicker from '../PrintFieldPicker.vue'

const props = defineProps({
  mode: { type: String, default: 'header' }, // header | footer | subtotal
})

const store = usePrintDesignerStore()
const isElementTable = computed(() => store.activeElement?.type === 'DATA_TABLE')
const table = computed(() => (isElementTable.value ? store.activeElement : store.activeSurface))
const masterFieldOptions = computed(() => groupedFieldSelectOptions(store.catalog))
const formats = [
  { label: '文本', value: 'TEXT' },
  { label: '金额（分转元）', value: 'MONEY' },
  { label: '金额大写', value: 'MONEY_UPPER' },
  { label: '数字', value: 'NUMBER' },
]

/** Selected band cell: { kind: 'header'|'footer'|'subtotal', row: number (-1 footer), cell: number } */
const selection = ref(null)
const textDraft = ref('')

watch(
  () => `${table.value?.id || ''}|${props.mode}|${table.value?.headerRows?.length || 0}|${table.value?.footer ? 1 : 0}|${table.value?.subtotal ? 1 : 0}`,
  () => {
    selection.value = null
    textDraft.value = ''
  },
)

const columns = computed(() => table.value?.columns || [])
const headerRows = computed(() => table.value?.headerRows || [])
const bandKey = computed(() => props.mode === 'subtotal' ? 'subtotal' : 'footer')
const bandRow = computed(() => table.value?.[bandKey.value] || null)
const bandTitle = computed(() => props.mode === 'subtotal' ? '本页小计' : '合计')

const selectedCell = computed(() => {
  const sel = selection.value
  if (!sel || !table.value)
    return null
  if (sel.kind === 'footer' || sel.kind === 'subtotal')
    return table.value[sel.kind]?.cells?.[sel.cell] || null
  return headerRows.value[sel.row]?.cells?.[sel.cell] || null
})

watch(selectedCell, (cell) => {
  if (!cell || selection.value?.kind !== 'header') {
    textDraft.value = ''
    return
  }
  textDraft.value = cell.text || ''
}, { immediate: true })

function commitHeaderText() {
  if (selection.value?.kind !== 'header' || !selectedCell.value)
    return
  const next = textDraft.value
  if ((selectedCell.value.text || '') === next)
    return
  patchSelected({ text: next })
}

function change(action) {
  if (isElementTable.value) {
    const elementId = store.activeElement.id
    return store.execute((doc) => {
      action(findSurface(doc, store.surfaceId).elements.find(item => item.id === elementId))
    })
  }
  return store.execute(doc => action(findSurface(doc, store.surfaceId)))
}

function rowOf(item, sel = selection.value) {
  if (!sel)
    return null
  if (sel.kind === 'header')
    return item.headerRows[sel.row]
  return item[sel.kind]
}

function selectHeader(row, cell) {
  selection.value = { kind: 'header', row, cell }
}
function selectFooter(cell) {
  selection.value = { kind: bandKey.value, row: -1, cell }
}

function addHeader() {
  change((item) => {
    item.headerRows ||= [{ cells: item.columns.map(c => ({ text: c.title, span: 1 })) }]
    // Full-span top row first; user splits / merges to build a tree header.
    item.headerRows.unshift({ cells: [{ text: '分组标题', span: item.columns.length }] })
  })
  selection.value = { kind: 'header', row: 0, cell: 0 }
}

function removeHeader(index) {
  change((item) => {
    item.headerRows.splice(index, 1)
    if (!item.headerRows.length)
      delete item.headerRows
  })
  selection.value = null
}

function addFooter() {
  change((item) => {
    const kind = bandKey.value
    const label = kind === 'subtotal' ? '本页小计' : '合计'
    item[kind] = {
      cells: item.columns.map((c, i) => ({
        span: 1,
        binding: i === 0
          ? { source: 'CONSTANT', value: label }
          : { source: 'EXPRESSION', expression: `SUM(${c.field})` },
      })),
    }
  })
  selection.value = { kind: bandKey.value, row: -1, cell: 0 }
}

function removeFooter() {
  change((item) => {
    delete item[bandKey.value]
  })
  selection.value = null
}

function patchSelected(patch) {
  const sel = selection.value
  if (!sel)
    return
  change(item => Object.assign(rowOf(item).cells[sel.cell], patch))
}

function patchSelectedStyle(key, value) {
  if (value === null || !selection.value)
    return
  let next = value
  if (typeof next === 'string' && ['color', 'backgroundColor'].includes(key))
    next = toPrintColor(next)
  const sel = selection.value
  change((item) => {
    const target = rowOf(item).cells[sel.cell]
    target.style = { ...target.style, [key]: next }
  })
}

function mergeSelected() {
  const sel = selection.value
  if (!sel)
    return
  change((item) => {
    const cells = rowOf(item).cells
    if (sel.cell >= cells.length - 1)
      return
    cells[sel.cell].span += cells[sel.cell + 1].span
    cells.splice(sel.cell + 1, 1)
  })
}

function splitSelected() {
  const sel = selection.value
  if (!sel)
    return
  change((item) => {
    const cells = rowOf(item).cells
    const cell = cells[sel.cell]
    if (!cell || cell.span <= 1)
      return
    cell.span--
    cells.splice(sel.cell + 1, 0, sel.kind === 'header'
      ? { span: 1, text: '' }
      : { span: 1, binding: { source: 'CONSTANT', value: '' } })
  })
}

function cellLabel(cell, kind) {
  if (cell.contentType === 'IMAGE')
    return '（图片）'
  if (kind === 'footer' || kind === 'subtotal') {
    if (cell.binding?.source === 'FIELD')
      return cell.binding.path?.split('.').at(-1) || '字段'
    if (cell.binding?.source === 'EXPRESSION')
      return cell.binding.expression || '表达式'
    return String(cell.binding?.value ?? '') || '（空）'
  }
  return cell.text || '（空）'
}

function setBandContentType(type) {
  const sel = selection.value
  if (!sel)
    return
  change((item) => {
    const cell = rowOf(item).cells[sel.cell]
    if (type === 'IMAGE') {
      cell.contentType = 'IMAGE'
      cell.binding = { source: 'CONSTANT', value: '' }
      delete cell.text
      delete cell.format
      return
    }
    delete cell.contentType
    if (sel.kind === 'header') {
      cell.text = textDraft.value || ''
      delete cell.binding
      delete cell.format
    }
    else {
      cell.binding = { source: 'CONSTANT', value: '' }
    }
  })
}

function onBandImageUpload(value) {
  const fileId = String((Array.isArray(value) ? value[0] : value) || '')
  if (!fileId)
    return
  const sel = selection.value
  if (!sel)
    return
  change((item) => {
    const cell = rowOf(item).cells[sel.cell]
    cell.contentType = 'IMAGE'
    cell.binding = { source: 'CONSTANT', value: fileId }
    delete cell.text
    delete cell.format
  })
}

const bandImageFileId = computed(() => (
  selectedCell.value?.contentType === 'IMAGE' && selectedCell.value?.binding?.source === 'CONSTANT'
    ? String(selectedCell.value.binding.value || '')
    : ''
))
const bandContentType = computed(() => selectedCell.value?.contentType === 'IMAGE' ? 'IMAGE' : 'TEXT')

const canMerge = computed(() => {
  const sel = selection.value
  if (!sel || !selectedCell.value)
    return false
  const cells = sel.kind === 'header' ? headerRows.value[sel.row]?.cells : bandRow.value?.cells
  return !!cells && sel.cell < cells.length - 1
})
const canSplit = computed(() => (selectedCell.value?.span || 1) > 1)
</script>

<template>
  <section v-if="table?.columns" class="table-bands">
    <!-- ===== 复杂表头 ===== -->
    <template v-if="mode === 'header'">
      <p class="muted tip">
        多级表头：上层用「合并右侧」跨多列形成树形分组。整表表头颜色在「样式」页设置；这里的色板只覆盖当前格。
      </p>
      <div class="band-toolbar">
        <NButton size="tiny" :disabled="headerRows.length >= 10" @click="addHeader">
          添加上层表头
        </NButton>
        <NButton v-if="headerRows.length" size="tiny" quaternary type="error" @click="change((item) => { delete item.headerRows }); selection.value = null">
          清除多级表头
        </NButton>
      </div>

      <div v-if="!headerRows.length" class="empty-hint">
        当前为单行列标题。需要「物料信息 | 金额信息」这类分组时，点上方「添加上层表头」。
      </div>

      <div v-else class="band-sketch" role="grid" aria-label="多级表头结构">
        <div
          v-for="(row, rowIndex) in headerRows"
          :key="`h-${rowIndex}`"
          class="band-sketch-row"
        >
          <div class="row-meta">
            <span>第 {{ rowIndex + 1 }} 层</span>
            <button type="button" class="link danger" @click="removeHeader(rowIndex)">
              删层
            </button>
          </div>
          <div class="band-sketch-cells">
            <button
              v-for="(cell, cellIndex) in row.cells"
              :key="cellIndex"
              type="button"
              class="band-cell-chip"
              :class="{ active: selection?.kind === 'header' && selection.row === rowIndex && selection.cell === cellIndex }"
              :style="{ flex: cell.span, background: cell.style?.backgroundColor || '#f1f5f9', color: cell.style?.color || '#0f172a' }"
              :title="`跨 ${cell.span} 列`"
              @click="selectHeader(rowIndex, cellIndex)"
            >
              <span class="chip-text">{{ cellLabel(cell, 'header') }}</span>
              <small>×{{ cell.span }}</small>
            </button>
          </div>
        </div>
        <div class="band-sketch-row leaf">
          <div class="row-meta">
            <span>列标题</span>
          </div>
          <div class="band-sketch-cells">
            <span
              v-for="col in columns"
              :key="col.id"
              class="band-cell-chip leaf-chip"
              :style="{ flex: 1 }"
            >
              {{ col.title || col.field }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 合计 / 小计 ===== -->
    <template v-else>
      <p class="muted tip">
        {{ mode === 'subtotal' ? '本页小计按当前页明细聚合，常用 SUM(字段)。' : '合计按全部明细聚合；也可绑主表汇总字段或写固定文字。' }}
      </p>
      <div class="band-toolbar">
        <NButton v-if="!bandRow" size="tiny" @click="addFooter">
          添加{{ bandTitle }}行
        </NButton>
        <NButton v-else size="tiny" quaternary type="error" @click="removeFooter">
          删除{{ bandTitle }}行
        </NButton>
      </div>
      <div v-if="bandRow" class="band-sketch" :aria-label="`${bandTitle}行结构`">
        <div class="band-sketch-row">
          <div class="row-meta">
            <span>{{ bandTitle }}</span>
          </div>
          <div class="band-sketch-cells">
            <button
              v-for="(cell, cellIndex) in bandRow.cells"
              :key="cellIndex"
              type="button"
              class="band-cell-chip"
              :class="{ active: selection?.kind === bandKey && selection.cell === cellIndex }"
              :style="{ flex: cell.span, background: cell.style?.backgroundColor || '#fff', color: cell.style?.color || '#0f172a' }"
              @click="selectFooter(cellIndex)"
            >
              <span class="chip-text">{{ cellLabel(cell, bandKey) }}</span>
              <small>×{{ cell.span }}</small>
            </button>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint">
        尚未添加{{ bandTitle }}行。
      </div>
    </template>

    <!-- ===== 选中单元格编辑（一次只改一个） ===== -->
    <div v-if="selectedCell" class="cell-editor">
      <div class="cell-editor-head">
        <strong>
          {{ selection.kind === 'header' ? `表头 · 第 ${selection.row + 1} 层` : `${bandTitle}单元格` }}
          · 跨 {{ selectedCell.span }} 列
        </strong>
        <div class="cell-actions">
          <NButton size="tiny" :disabled="!canMerge" @click="mergeSelected">
            合并右侧
          </NButton>
          <NButton size="tiny" :disabled="!canSplit" @click="splitSelected">
            拆分一列
          </NButton>
        </div>
      </div>

      <NFormItem label="内容类型" size="small">
        <NSelect
          :value="bandContentType"
          :options="[{ label: '文字', value: 'TEXT' }, { label: '图片', value: 'IMAGE' }]"
          size="small"
          @update:value="setBandContentType"
        />
      </NFormItem>

      <template v-if="bandContentType === 'IMAGE'">
        <NFormItem label="上传图片" size="small">
          <FileUpload
            :model-value="bandImageFileId"
            :limit="1"
            :multiple="false"
            :show-download="false"
            :file-type="['png', 'jpg', 'jpeg', 'webp']"
            business-type="print"
            upload-button-text="选择图片"
            @update:model-value="onBandImageUpload"
          />
        </NFormItem>
      </template>
      <template v-else-if="selection.kind === 'header'">
        <NFormItem label="表头文字" size="small">
          <NInput
            v-model:value="textDraft"
            size="small"
            placeholder="分组名称"
            @blur="commitHeaderText"
            @keydown.enter.prevent="commitHeaderText"
          />
        </NFormItem>
      </template>
      <template v-else>
        <NFormItem label="内容来源" size="small">
          <NSelect
            :value="selectedCell.binding.source"
            :options="[
              { label: '固定文字', value: 'CONSTANT' },
              { label: '表达式（求和/运算）', value: 'EXPRESSION' },
              { label: '主表/流程字段', value: 'FIELD', disabled: !masterFieldOptions.length },
            ]"
            @update:value="patchSelected({
              binding: $event === 'CONSTANT'
                ? { source: 'CONSTANT', value: '' }
                : $event === 'EXPRESSION'
                  ? { source: 'EXPRESSION', expression: selectedCell.binding?.expression || 'SUM(qty)' }
                  : { source: 'FIELD', path: masterFieldOptions[0]?.children?.[0]?.value || '' },
            })"
          />
        </NFormItem>
        <NFormItem v-if="selectedCell.binding.source === 'CONSTANT'" label="固定文字" size="small">
          <NInput
            :value="String(selectedCell.binding.value ?? '')"
            size="small"
            @update:value="patchSelected({ binding: { source: 'CONSTANT', value: $event } })"
          />
        </NFormItem>
        <NFormItem v-else-if="selectedCell.binding.source === 'EXPRESSION'" label="表达式" size="small">
          <NInput
            :value="String(selectedCell.binding.expression ?? '')"
            type="textarea"
            size="small"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="SUM(amount) 或 qty * price"
            @update:value="patchSelected({ binding: { source: 'EXPRESSION', expression: $event } })"
          />
        </NFormItem>
        <NFormItem v-else label="汇总字段" size="small">
          <PrintFieldPicker
            :value="selectedCell.binding.path"
            :catalog="store.catalog"
            placeholder="选择汇总字段"
            @update:value="patchSelected({ binding: { source: 'FIELD', path: $event } })"
          />
        </NFormItem>
        <NFormItem label="格式" size="small">
          <NSelect
            :value="selectedCell.format?.type || 'TEXT'"
            :options="formats"
            size="small"
            @update:value="patchSelected({ format: { type: $event } })"
          />
        </NFormItem>
      </template>

      <div class="panel-grid">
        <NFormItem label="背景" size="small">
          <NColorPicker
            class="swatch-only"
            :value="selectedCell.style?.backgroundColor || (selection.kind === 'header' ? '#f1f5f9' : '#ffffff')"
            :show-alpha="false"
            :modes="['hex']"
            @update:value="patchSelectedStyle('backgroundColor', $event)"
          />
        </NFormItem>
        <NFormItem label="文字色" size="small">
          <NColorPicker
            class="swatch-only"
            :value="selectedCell.style?.color || '#000000'"
            :show-alpha="false"
            :modes="['hex']"
            @update:value="patchSelectedStyle('color', $event)"
          />
        </NFormItem>
      </div>
    </div>
    <p v-else-if="(mode === 'header' && headerRows.length) || (mode !== 'header' && bandRow)" class="muted tip pick-hint">
      在上方示意图中点击一个单元格，再设置文字、合并或样式。
    </p>
  </section>
</template>

<style scoped>
.tip {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.45;
}
.band-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.empty-hint {
  padding: 10px 8px;
  border: 1px dashed var(--border-light, #cbd5e1);
  border-radius: 6px;
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  line-height: 1.5;
}
.band-sketch {
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.band-sketch-row {
  border-bottom: 1px solid var(--border-light, #e2e8f0);
}
.band-sketch-row:last-child {
  border-bottom: 0;
}
.row-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 3px 8px;
  background: var(--gray-100, #f8fafc);
  color: var(--text-tertiary, #64748b);
  font-size: 10px;
  font-weight: 700;
}
.link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--primary-color);
  cursor: pointer;
  font-size: 10px;
}
.link.danger {
  color: var(--error-color, #d03050);
}
.band-sketch-cells {
  display: flex;
  min-height: 32px;
}
.band-cell-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  min-width: 0;
  margin: 0;
  padding: 4px 2px;
  border: 0;
  border-right: 1px solid #cbd5e1;
  background: #f1f5f9;
  color: #0f172a;
  cursor: pointer;
  font: inherit;
}
.band-cell-chip:last-child {
  border-right: 0;
}
.band-cell-chip.active {
  outline: 2px solid var(--primary-color);
  outline-offset: -2px;
  z-index: 1;
}
.band-cell-chip.leaf-chip,
.leaf .band-cell-chip {
  cursor: default;
  background: #fff;
  color: var(--text-secondary, #475569);
  font-size: 10px;
}
.chip-text {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 700;
}
.band-cell-chip small {
  color: var(--text-tertiary, #94a3b8);
  font-size: 9px;
}
.cell-editor {
  margin-top: 10px;
  padding: 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 25%, #e2e8f0);
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color) 4%, #fff);
}
.cell-editor-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 8px;
}
.cell-editor-head strong {
  font-size: 12px;
}
.cell-actions {
  display: flex;
  gap: 4px;
}
.panel-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 4px 8px;
}
.pick-hint {
  margin-top: 8px;
}
:deep(.swatch-only .n-color-picker-trigger__value) {
  display: none !important;
}
</style>
