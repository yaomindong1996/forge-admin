<script setup>
import { NButton, NColorPicker, NInput, NSelect, NSwitch } from 'naive-ui'
import { computed } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../../protocol/printColor'
import { findSurface, newPrintId } from '../commands'
import { descriptionBlockHeightMm } from '../descriptions'
import { groupedFieldSelectOptions } from '../fieldGroups'
import { printFontSizeOptions } from '../printFonts'

const store = usePrintDesignerStore()
const block = computed(() => store.activeElement?.descriptions || {})
const fieldOptions = computed(() => groupedFieldSelectOptions(store.catalog))
const column = computed(() => Math.max(1, Math.min(4, Number(block.value.column) || 2)))
const showSeparator = computed(() => block.value.labelPlacement === 'left' && block.value.bordered !== true)
const alignOptions = [
  { label: '左', value: 'left' },
  { label: '中', value: 'center' },
  { label: '右', value: 'right' },
]
const placementOptions = [
  { label: '左侧', value: 'left' },
  { label: '上方', value: 'top' },
]
const sizeOptions = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
const columnOptions = [1, 2, 3, 4].map(value => ({ label: `${value} 列`, value }))
const spanOptions = computed(() => Array.from({ length: column.value }, (_, index) => ({ label: `${index + 1} 列`, value: index + 1 })))

function patchBlock(partial, { fitHeight = false } = {}) {
  const elementId = store.activeElement?.id
  if (!elementId)
    return
  const next = { ...block.value, ...partial }
  store.execute((document) => {
    const surface = findSurface(document, store.surfaceId)
    const element = surface?.elements?.find(item => item.id === elementId)
    if (!element)
      return
    element.descriptions = next
    if (fitHeight) {
      element.heightMm = descriptionBlockHeightMm(next)
      if (Number.isFinite(surface.heightMm))
        surface.heightMm = Math.max(surface.heightMm, Number((element.yMm + element.heightMm).toFixed(3)))
    }
  })
}

function setColumn(value) {
  const nextColumn = Number(value) || 1
  patchBlock({
    column: nextColumn,
    items: (block.value.items || []).map(item => ({ ...item, span: Math.min(item.span || 1, nextColumn) })),
  }, { fitHeight: true })
}

function setItem(index, partial, fitHeight = false) {
  patchBlock({
    items: (block.value.items || []).map((item, itemIndex) => itemIndex === index ? { ...item, ...partial } : item),
  }, { fitHeight })
}

function setItemField(index, path) {
  const field = store.catalog.find(item => item.path === path)
  setItem(index, {
    binding: { source: 'FIELD', path },
    label: String(field?.label || path.split('.').at(-1) || '字段').slice(0, 40),
  })
}

function removeItem(index) {
  patchBlock({
    items: (block.value.items || []).filter((_, itemIndex) => itemIndex !== index),
  }, { fitHeight: true })
}

function addItem() {
  const first = fieldOptions.value.find(group => group.children?.length)?.children?.[0]
  patchBlock({
    items: [...(block.value.items || []), {
      id: newPrintId(),
      label: String(first?.label || '字段').slice(0, 40),
      span: 1,
      binding: first ? { source: 'FIELD', path: first.value } : { source: 'CONSTANT', value: '' },
    }],
  }, { fitHeight: true })
}

function paint(key, value) {
  const color = toPrintColor(value)
  patchBlock({ [key]: color && color !== 'transparent' ? color : (key === 'labelBackground' ? '#d9e3f0' : '#111827') })
}
</script>

<template>
  <section class="designer-group detail-panel">
    <h3>布局</h3>
    <div class="pair">
      <label>列数<NSelect size="small" :value="column" :options="columnOptions" :consistent-menu-width="false" @update:value="setColumn" /></label>
      <label>尺寸<NSelect size="small" :value="block.size || 'medium'" :options="sizeOptions" :consistent-menu-width="false" @update:value="patchBlock({ size: $event }, { fitHeight: true })" /></label>
    </div>
    <div class="pair">
      <label>标签位置<NSelect size="small" :value="block.labelPlacement || 'left'" :options="placementOptions" :consistent-menu-width="false" @update:value="patchBlock({ labelPlacement: $event })" /></label>
      <label>标签对齐<NSelect size="small" :value="block.labelAlign || 'left'" :options="alignOptions" :consistent-menu-width="false" @update:value="patchBlock({ labelAlign: $event })" /></label>
    </div>
    <div class="switch-line">
      <span>边框</span>
      <NSwitch size="small" :value="block.bordered === true" @update:value="patchBlock({ bordered: $event })" />
    </div>
    <label v-if="showSeparator" class="stack">
      分隔符
      <NInput size="small" :value="block.separator ?? '：'" maxlength="8" @update:value="patchBlock({ separator: $event })" />
    </label>

    <h3>标题</h3>
    <NInput size="small" :value="block.title || ''" maxlength="80" placeholder="可留空" @update:value="patchBlock({ title: $event }, { fitHeight: true })" />
    <label class="stack">
      颜色
      <NColorPicker :value="block.titleColor || '#111827'" size="small" :show-alpha="false" :modes="['hex']" @update:value="paint('titleColor', $event)" />
    </label>
    <div class="pair">
      <label>字号<NSelect size="small" :value="block.titleFontSizePt || 12" :options="printFontSizeOptions(block.titleFontSizePt || 12)" :consistent-menu-width="false" @update:value="patchBlock({ titleFontSizePt: $event }, { fitHeight: true })" /></label>
      <label>对齐<NSelect size="small" :value="block.titleAlign || 'left'" :options="alignOptions" :consistent-menu-width="false" @update:value="patchBlock({ titleAlign: $event })" /></label>
    </div>
    <div class="switch-line">
      <span>加粗</span>
      <NSwitch size="small" :value="block.titleBold !== false" @update:value="patchBlock({ titleBold: $event })" />
    </div>

    <h3>标签</h3>
    <label class="stack">
      背景色
      <NColorPicker :value="block.labelBackground || '#d9e3f0'" size="small" :show-alpha="false" :modes="['hex']" @update:value="paint('labelBackground', $event)" />
    </label>

    <div class="items-head">
      <h3>字段</h3>
      <NButton size="tiny" @click="addItem">
        添加
      </NButton>
    </div>
    <p v-if="!(block.items || []).length" class="empty">
      还没有字段
    </p>
    <article v-for="(item, index) in block.items || []" :key="item.id" class="item-card">
      <header>
        <strong>{{ item.label || '未命名' }}</strong>
        <button type="button" @click="removeItem(index)">
          删除
        </button>
      </header>
      <label class="stack">
        标签
        <NInput size="small" :value="item.label" maxlength="40" @update:value="setItem(index, { label: $event })" />
      </label>
      <label class="stack">
        字段
        <NSelect
          size="small"
          :value="item.binding?.source === 'FIELD' ? item.binding.path : null"
          :options="fieldOptions"
          filterable
          :consistent-menu-width="false"
          placeholder="选择字段"
          @update:value="setItemField(index, $event)"
        />
      </label>
      <label class="stack">
        占列
        <NSelect size="small" :value="Math.min(item.span || 1, column)" :options="spanOptions" :consistent-menu-width="false" @update:value="setItem(index, { span: $event }, true)" />
      </label>
    </article>
  </section>
</template>

<style scoped>
.detail-panel h3 {
  margin: 12px 0 6px;
}
.detail-panel h3:first-child {
  margin-top: 0;
}
.pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 8px;
}
.stack,
.pair label {
  display: grid;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
}
.stack {
  margin-bottom: 8px;
}
.switch-line,
.items-head,
.item-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.switch-line {
  margin-bottom: 8px;
  color: #334155;
  font-size: 12px;
}
.items-head h3 {
  margin: 4px 0;
}
.item-card {
  margin-bottom: 8px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}
.item-card header {
  margin-bottom: 6px;
}
.item-card strong {
  min-width: 0;
  overflow: hidden;
  color: #1e293b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-card header button {
  flex: none;
  padding: 0;
  border: 0;
  background: transparent;
  color: #dc2626;
  font-size: 12px;
  cursor: pointer;
}
.empty {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
