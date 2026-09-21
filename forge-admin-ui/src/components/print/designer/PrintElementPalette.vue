<script setup>
import {
  BarcodeOutline,
  CheckmarkCircleOutline,
  CodeSlashOutline,
  DocumentLockOutline,
  DocumentTextOutline,
  EllipseOutline,
  ImageOutline,
  LayersOutline,
  ListOutline,
  QrCodeOutline,
  RemoveOutline,
  ReturnDownForwardOutline,
  ShieldCheckmarkOutline,
  SquareOutline,
  TextOutline,
} from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import { computed } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { addElement, addSection, elementCatalog, addDetailTable as insertDetailTable, startItemDrag } from './elementCatalog'
import { insertRegisteredPrintComponent, printComponentRegistry } from './printComponentRegistry'
import PrintTableIcon from './PrintTableIcon.vue'
import './businessPrintComponents'

const store = usePrintDesignerStore()
const elementIcons = {
  BARCODE: BarcodeOutline,
  IMAGE: ImageOutline,
  HTML: CodeSlashOutline,
  LINE: RemoveOutline,
  PAGE_NUMBER: DocumentTextOutline,
  QRCODE: QrCodeOutline,
  RECTANGLE: SquareOutline,
  ELLIPSE: EllipseOutline,
  TEXT: TextOutline,
  STATIC_TABLE: PrintTableIcon,
  DATA_TABLE: PrintTableIcon,
}
const flowSections = [
  { type: 'FIXED', label: '自由画布层', icon: LayersOutline, tip: '可自由拖放组件的图层' },
  { type: 'TEXT', label: '流式长文', icon: ListOutline, tip: '随内容自动换页' },
  { type: 'PAGE_BREAK', label: '手动分页', icon: ReturnDownForwardOutline, tip: '强制从下一页开始' },
]
const businessIcons = {
  approval: CheckmarkCircleOutline,
  contract: DocumentLockOutline,
  signature: ShieldCheckmarkOutline,
}
const businessComponents = computed(() => printComponentRegistry.list())

function addBusiness(key) {
  store.ensureFreeCanvas()
  insertRegisteredPrintComponent(store, key)
}

function addFlowSection(kind) {
  try {
    addSection(store, kind)
  }
  catch (error) {
    store.error = error?.message || '无法添加该区块'
  }
}

function addDetail() {
  try {
    insertDetailTable(store)
  }
  catch (error) {
    store.error = error?.message || '无法添加明细表格'
  }
}
</script>

<template>
  <section class="designer-group palette-group">
    <h3>基础组件</h3>
    <p class="muted tip">
      拖到中间画布即可自由摆放；明细表格与空白表格一样可拖动缩放
    </p>
    <div class="palette-grid">
      <button
        v-for="item in elementCatalog"
        :key="item.key"
        type="button"
        class="palette-item"
        draggable="true"
        :title="`拖入或点击添加${item.label}`"
        :aria-label="item.label"
        @dragstart="startItemDrag($event, { type: item.type, preset: item.preset })"
        @click="addElement(store, item.type, undefined, undefined, item.preset)"
      >
        <NIcon :component="elementIcons[item.type]" :size="16" :class="{ 'vertical-line-icon': item.preset === 'VERTICAL' }" />
        <span>{{ item.label }}</span>
      </button>
      <button
        type="button"
        class="palette-item"
        draggable="true"
        title="绑定集合字段循环行，可自由摆放"
        aria-label="明细表格"
        @dragstart="startItemDrag($event, { detailTable: true })"
        @click="addDetail"
      >
        <NIcon :component="PrintTableIcon" :size="16" />
        <span>明细表格</span>
      </button>
    </div>
    <h3>流式排版</h3>
    <p class="muted tip">
      画布层、长文与强制分页
    </p>
    <div class="palette-list">
      <button
        v-for="item in flowSections"
        :key="item.type"
        type="button"
        class="palette-row"
        draggable="true"
        :title="item.tip"
        @dragstart="startItemDrag($event, { section: item.type })"
        @click="addFlowSection(item.type)"
      >
        <NIcon :component="item.icon" :size="14" />
        <span>{{ item.label }}</span>
      </button>
    </div>
    <h3>业务组件</h3>
    <div class="palette-list">
      <button
        v-for="item in businessComponents"
        :key="item.key"
        type="button"
        class="palette-row"
        draggable="true"
        :title="`拖入或点击插入${item.label}`"
        @dragstart="startItemDrag($event, { component: item.key })"
        @click="addBusiness(item.key)"
      >
        <NIcon :component="businessIcons[item.icon] || DocumentTextOutline" :size="14" />
        <span>{{ item.label }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.palette-group h3 {
  margin: 10px 0 4px;
  font-size: 12px;
}
.palette-group h3:first-child {
  margin-top: 0;
}
.tip {
  margin: 0 0 8px;
  font-size: 11px;
}
.palette-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}
.palette-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 54px;
  padding: 8px 4px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: grab;
  font-size: 11px;
  line-height: 1.2;
  text-align: center;
}
.palette-item:hover {
  border-color: color-mix(in srgb, var(--primary-color) 45%, transparent);
  background: color-mix(in srgb, var(--primary-color) 6%, var(--bg-primary));
  color: var(--primary-color);
}
.palette-item:active {
  cursor: grabbing;
}
.vertical-line-icon {
  transform: rotate(90deg);
}
.palette-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.palette-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 8px;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  color: var(--text-primary);
  cursor: grab;
  font-size: 12px;
  text-align: left;
}
.palette-row:hover {
  border-color: var(--border-light, #e2e8f0);
  background: var(--gray-100);
}
.palette-row:active {
  cursor: grabbing;
}
</style>
