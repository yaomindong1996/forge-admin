<script setup>
import { ChevronDownOutline, ChevronUpOutline, TrashOutline } from '@vicons/ionicons5'
import { NColorPicker, NFormItem, NIcon, NInput, NSelect, NSwitch, NTabPane, NTabs } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../../protocol/printColor'
import { findSurface, normalizeTableColumnWidths } from '../commands'
import { collectionPaths, fieldGroupKey, fieldGroupTitle } from '../fieldGroups'
import PrintFieldPicker from '../PrintFieldPicker.vue'
import { printFontSizeOptions } from '../printFonts'
import { PRINT_MM_PRESETS, printMmOptions } from '../printMeasures'
import TableBandsPanel from './TableBandsPanel.vue'

const store = usePrintDesignerStore()
const tab = ref('columns')
const isElementTable = computed(() => store.activeElement?.type === 'DATA_TABLE')
const table = computed(() => {
  if (isElementTable.value)
    return store.activeElement
  return store.activeSurface?.kind === 'TABLE' ? store.activeSurface : null
})
const sourceSummary = computed(() => {
  if (!table.value?.collectionPath)
    return ''
  return fieldGroupTitle(fieldGroupKey(table.value.collectionPath, collectionPaths(store.catalog)), store.catalog)
})
const formats = [
  { label: '文本', value: 'TEXT' },
  { label: '金额', value: 'MONEY' },
  { label: '数字', value: 'NUMBER' },
  { label: '日期', value: 'DATE' },
  { label: '布尔', value: 'BOOLEAN' },
]
const alignments = [
  { label: '左', value: 'left' },
  { label: '中', value: 'center' },
  { label: '右', value: 'right' },
]
const activeColumnId = computed(() => store.tableColumnId)
const activeColumn = computed(() => table.value?.columns.find(column => column.id === activeColumnId.value) || null)

watch(
  () => table.value?.id,
  (id, prev) => {
    if (id && id !== prev)
      tab.value = 'columns'
  },
)

function mutateTable(mutator) {
  if (isElementTable.value) {
    const elementId = store.activeElement.id
    return store.execute((doc) => {
      const element = findSurface(doc, store.surfaceId).elements.find(item => item.id === elementId)
      mutator(element)
    })
  }
  return store.execute((doc) => {
    mutator(findSurface(doc, store.surfaceId))
  })
}

function selectColumn(id) {
  store.selectTableColumn(id)
  tab.value = 'columns'
}
function column(id, patch) {
  mutateTable((item) => {
    Object.assign(item.columns.find(c => c.id === id), patch)
    if (patch.widthMm !== undefined)
      normalizeTableColumnWidths(item.columns, item.widthMm || item.columns.reduce((sum, c) => sum + c.widthMm, 0))
  })
}
function columnStyle(id, key, value) {
  if (value === null)
    return
  let next = value
  if (typeof next === 'string' && ['color', 'backgroundColor'].includes(key))
    next = toPrintColor(next)
  mutateTable((item) => {
    const col = item.columns.find(c => c.id === id)
    col.style = { ...col.style, [key]: next }
  })
}
function reorder(index, offset) {
  mutateTable((item) => {
    if (item.headerRows || item.footer)
      throw new Error('请先在「复杂表头 / 合计」页清除合并配置，再调整列顺序')
    const [value] = item.columns.splice(index, 1)
    item.columns.splice(index + offset, 0, value)
  })
}
function removeSelectedOrOne(id) {
  const selected = store.tableColumnIds.includes(id) && store.tableColumnIds.length > 1
    ? [...store.tableColumnIds]
    : [id]
  if (selected.length >= (table.value?.columns?.length || 0)) {
    store.notice = '明细表格至少保留一列'
    return
  }
  mutateTable((item) => {
    if (item.headerRows || item.footer)
      throw new Error('请先在「复杂表头 / 合计」页清除合并配置，再删除列')
    item.columns = item.columns.filter(c => !selected.includes(c.id))
    normalizeTableColumnWidths(item.columns, item.widthMm || item.columns.reduce((sum, c) => sum + c.widthMm, 0))
  })
  store.tableColumnIds = store.tableColumnIds.filter(value => !selected.includes(value))
  store.tableColumnId = store.tableColumnIds[0] || ''
}
function patchTable(patch) {
  if (isElementTable.value)
    return store.patchSelected(patch)
  return store.patchSurface(patch)
}
</script>

<template>
  <section v-if="table" class="designer-group table-panel">
    <h3>明细表格</h3>
    <div class="source-card">
      <span class="source-badge">子表</span>
      <div class="source-copy">
        <strong>{{ sourceSummary || '明细数据源' }}</strong>
        <small>{{ table.collectionPath }}</small>
      </div>
    </div>
    <p class="muted tip">
      {{ isElementTable ? '左上角方块拖动整表；表头按住拖拽可框选多列；Ctrl+C/V 复制粘贴列字段；右侧分栏设置列 / 多级表头 / 合计。' : '流式整宽明细区块。' }}
      单元格纵向合并请用「空白表格」组件（类 Word）；明细表按数据行循环，支持多级表头横向合并。
    </p>

    <NTabs v-model:value="tab" type="segment" size="small" class="table-tabs">
      <NTabPane name="columns" tab="列">
        <NFormItem label="续页重复表头" size="small">
          <NSwitch :value="table.repeatHeader !== false" @update:value="patchTable({ repeatHeader: $event })" />
        </NFormItem>
        <NFormItem label="无明细文案" size="small">
          <NInput :value="table.emptyText || ''" @update:value="patchTable({ emptyText: $event })" />
        </NFormItem>

        <div class="column-list-head">
          <strong>显示列</strong>
          <span>{{ table.columns.length }} 列 · 点选后编辑</span>
        </div>
        <ul class="column-list">
          <li
            v-for="(item, index) in table.columns"
            :key="item.id"
            class="column-item"
            :class="{ active: activeColumnId === item.id || store.tableColumnIds.includes(item.id) }"
            @click="selectColumn(item.id)"
          >
            <span class="column-index">{{ index + 1 }}</span>
            <div class="column-main" @click.stop>
              <NInput size="tiny" :value="item.title" placeholder="列标题" @update:value="column(item.id, { title: $event })" />
              <PrintFieldPicker
                size="tiny"
                :value="item.field"
                :catalog="store.catalog"
                :only-under="table.collectionPath"
                placeholder="选择子表字段"
                @update:value="column(item.id, { field: $event })"
              />
            </div>
            <div class="column-actions" @click.stop>
              <button type="button" title="上移" :disabled="index === 0" @click="reorder(index, -1)">
                <NIcon :component="ChevronUpOutline" :size="14" />
              </button>
              <button type="button" title="下移" :disabled="index === table.columns.length - 1" @click="reorder(index, 1)">
                <NIcon :component="ChevronDownOutline" :size="14" />
              </button>
              <button type="button" class="danger" title="删除列" :disabled="table.columns.length === 1" @click="removeSelectedOrOne(item.id)">
                <NIcon :component="TrashOutline" :size="14" />
              </button>
            </div>
          </li>
        </ul>

        <div v-if="activeColumn" class="column-detail">
          <h4>列「{{ activeColumn.title || activeColumn.field }}」</h4>
          <p class="muted tip">
            这里只改本列表体。整表表头/表体颜色在右侧「样式」页设置。
          </p>
          <div class="panel-grid">
            <NFormItem label="宽度 mm" size="small">
              <NSelect :value="activeColumn.widthMm" :options="printMmOptions(activeColumn.widthMm, PRINT_MM_PRESETS.track)" :filterable="false" :consistent-menu-width="false" @update:value="column(activeColumn.id, { widthMm: $event })" />
            </NFormItem>
            <NFormItem label="格式" size="small">
              <NSelect :value="activeColumn.format?.type || 'TEXT'" :options="formats" @update:value="column(activeColumn.id, { format: { type: $event } })" />
            </NFormItem>
            <NFormItem label="表体对齐" size="small">
              <NSelect :value="activeColumn.style?.textAlign || 'left'" :options="alignments" @update:value="columnStyle(activeColumn.id, 'textAlign', $event)" />
            </NFormItem>
            <NFormItem label="字号" size="small">
              <NSelect :value="activeColumn.style?.fontSizePt || 10" :options="printFontSizeOptions(activeColumn.style?.fontSizePt || 10)" :filterable="false" :consistent-menu-width="false" @update:value="columnStyle(activeColumn.id, 'fontSizePt', $event)" />
            </NFormItem>
            <NFormItem label="文字色" size="small">
              <NColorPicker class="swatch-only" :value="activeColumn.style?.color || '#000000'" :show-alpha="false" :modes="['hex']" @update:value="columnStyle(activeColumn.id, 'color', $event)" />
            </NFormItem>
            <NFormItem label="表体背景" size="small">
              <NColorPicker class="swatch-only" :value="activeColumn.style?.backgroundColor || '#ffffff'" :show-alpha="false" :modes="['hex']" @update:value="columnStyle(activeColumn.id, 'backgroundColor', $event)" />
            </NFormItem>
          </div>
        </div>
        <p v-else class="muted tip">
          点击上方某一列，编辑宽度与表体样式。
        </p>
      </NTabPane>

      <NTabPane name="header" :tab="table.headerRows?.length ? `表头(${table.headerRows.length})` : '表头'">
        <TableBandsPanel mode="header" />
      </NTabPane>

      <NTabPane name="footer" :tab="table.footer ? '合计✓' : '合计'">
        <TableBandsPanel mode="footer" />
      </NTabPane>
      <NTabPane name="subtotal" :tab="table.subtotal ? '小计✓' : '小计'">
        <TableBandsPanel mode="subtotal" />
      </NTabPane>
    </NTabs>
  </section>
</template>

<style scoped>
.tip {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.45;
}
.source-card {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 22%, #e2e8f0);
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color) 6%, #fff);
}
.source-badge {
  flex: none;
  padding: 2px 6px;
  border-radius: 3px;
  background: var(--primary-color);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}
.source-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.source-copy strong {
  font-size: 12px;
  color: var(--text-primary, #0f172a);
}
.source-copy small {
  color: var(--text-tertiary, #64748b);
  font-size: 10px;
  overflow-wrap: anywhere;
}
.table-tabs {
  margin-top: 4px;
}
.table-tabs :deep(.n-tabs-pane-wrapper) {
  padding-top: 8px;
}
.column-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0;
  color: var(--text-secondary, #64748b);
  font-size: 12px;
}
.column-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px;
  overflow: hidden;
}
.column-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  gap: 6px;
  align-items: start;
  padding: 6px 8px;
  border-bottom: 1px solid var(--border-light, #e2e8f0);
  cursor: pointer;
}
.column-item:last-child {
  border-bottom: 0;
}
.column-item.active {
  background: color-mix(in srgb, var(--primary-color) 8%, #fff);
}
.column-index {
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 28px;
}
.column-main {
  display: grid;
  gap: 4px;
}
.column-actions {
  display: flex;
  gap: 2px;
}
.column-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
}
.column-actions button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.column-actions button.danger {
  color: var(--error-color, #d03050);
}
.column-detail {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-light, #e2e8f0);
}
.column-detail h4,
.table-tabs h4 {
  margin: 0 0 6px;
  font-size: 12px;
}
.panel-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 4px 8px;
}
:deep(.swatch-only .n-color-picker-trigger__value) {
  display: none !important;
}
</style>
