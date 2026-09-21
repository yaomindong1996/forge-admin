<script setup>
import { ChevronDownOutline, ChevronUpOutline, EllipsisHorizontalOutline } from '@vicons/ionicons5'
import { NDropdown, NIcon } from 'naive-ui'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { newPrintId } from './commands'
import { cloneDocument } from './history'
import { renewStaticTableIds } from './staticTable'

const store = usePrintDesignerStore()
function rowOptions(section) {
  return [
    { label: '复制区块', key: 'duplicate', disabled: section.kind === 'PAGE_BREAK' },
    { type: 'divider', key: 'divider' },
    { label: '删除区块', key: 'remove' },
  ]
}

function move(id, target) {
  store.execute((doc) => {
    const index = doc.body.findIndex(s => s.id === id)
    if (index < 0 || target < 0 || target >= doc.body.length)
      return
    const [section] = doc.body.splice(index, 1)
    doc.body.splice(target, 0, section)
  })
}

function duplicate(section) {
  const copy = cloneDocument(section)
  copy.id = newPrintId()
  copy.elements = (copy.elements || []).map(element => renewStaticTableIds({ ...element, id: newPrintId() }))
  for (const column of copy.columns || []) column.id = newPrintId()
  if (store.execute(doc => doc.body.splice(doc.body.findIndex(s => s.id === section.id) + 1, 0, copy)))
    store.selectSurface(copy.id)
}

function remove(id) {
  store.execute((doc) => {
    doc.body = doc.body.filter(s => s.id !== id)
  })
}

function handleRowAction(key, section) {
  if (key === 'duplicate')
    duplicate(section)
  else if (key === 'remove')
    remove(section.id)
}
</script>

<template>
  <section class="designer-group section-list">
    <h3>页面结构</h3>
    <button type="button" class="fixed-row" :class="{ active: store.surfaceId === 'header' }" @click="store.selectSurface('header')">
      <span class="row-badge">H</span>
      <span class="band-copy">
        <strong>页眉</strong>
        <small>{{ store.document.header.heightMm }}mm{{ store.document.header.repeat ? ' · 每页' : ' · 首页' }}</small>
      </span>
    </button>
    <div
      v-for="(section, index) in store.document.body"
      :key="section.id"
      class="section-row"
      :class="{ active: store.surfaceId === `section:${section.id}` }"
      draggable="true"
      @dragstart="$event.dataTransfer.setData('application/x-forge-print-section', section.id)"
      @dragover.prevent
      @drop.prevent="move($event.dataTransfer.getData('application/x-forge-print-section'), index)"
    >
      <button type="button" class="section-name" @click="store.selectSurface(`section:${section.id}`)">
        <span class="drag-handle">⠿</span>
        <span class="section-index">{{ index + 1 }}</span>
        <span class="section-label">{{ { FIXED: '自由画布', TEXT: '流式长文', TABLE: '明细表格', PAGE_BREAK: '手动分页' }[section.kind] }}</span>
      </button>
      <div class="section-actions">
        <button type="button" class="row-action" title="上移区块" aria-label="上移区块" :disabled="index === 0" @click="move(section.id, index - 1)">
          <NIcon :component="ChevronUpOutline" />
        </button>
        <button type="button" class="row-action" title="下移区块" aria-label="下移区块" :disabled="index === store.document.body.length - 1" @click="move(section.id, index + 1)">
          <NIcon :component="ChevronDownOutline" />
        </button>
        <NDropdown trigger="click" :options="rowOptions(section)" @select="handleRowAction($event, section)">
          <button type="button" class="row-action" title="更多区块操作" aria-label="更多区块操作">
            <NIcon :component="EllipsisHorizontalOutline" />
          </button>
        </NDropdown>
      </div>
    </div>
    <button type="button" class="fixed-row" :class="{ active: store.surfaceId === 'footer' }" @click="store.selectSurface('footer')">
      <span class="row-badge">F</span>
      <span class="band-copy">
        <strong>页脚</strong>
        <small>{{ store.document.footer.heightMm }}mm{{ store.document.footer.repeat ? ' · 每页' : ' · 末页' }}</small>
      </span>
    </button>
  </section>
</template>

<style scoped>
.section-list {
  padding-bottom: 6px !important;
}
.section-row,
.fixed-row {
  width: 100%;
  min-height: 28px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  margin: 2px 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: inherit;
  background: transparent;
}
.fixed-row {
  gap: 6px;
  padding: 0 4px;
  cursor: pointer;
  font-size: 11px;
}
.section-row:hover,
.fixed-row:hover {
  background: var(--gray-100);
}
.section-row.active,
.fixed-row.active {
  border-color: color-mix(in srgb, var(--primary-color) 28%, transparent);
  background: color-mix(in srgb, var(--primary-color) 9%, transparent);
}
.row-badge,
.section-index {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 3px;
  color: var(--text-tertiary, #64748b);
  background: color-mix(in srgb, var(--text-tertiary, #64748b) 9%, transparent);
  font-size: 9px;
}
.band-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 0;
  line-height: 1.15;
}
.band-copy strong {
  font-size: 11px;
  font-weight: 600;
}
.band-copy small {
  color: var(--text-tertiary, #64748b);
  font-size: 9px;
}
.section-name {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 1px 3px 3px;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: grab;
}
.drag-handle {
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
}
.section-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}
.section-actions {
  display: flex;
  align-items: center;
  padding-right: 1px;
  opacity: 0;
  transition: opacity 0.12s ease;
}
.section-row:hover .section-actions,
.section-row.active .section-actions,
.section-actions:focus-within {
  opacity: 1;
}
.row-action {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 3px;
  color: var(--text-tertiary, #64748b);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
}
.row-action:hover:not(:disabled) {
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 10%, transparent);
}
.row-action:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}
</style>
