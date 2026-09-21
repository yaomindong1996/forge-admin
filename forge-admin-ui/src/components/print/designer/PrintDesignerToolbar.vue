<script setup>
import {
  AddOutline,
  ArrowBackOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  CloudUploadOutline,
  CodeSlashOutline,
  EllipsisHorizontalOutline,
  EyeOutline,
  GitBranchOutline,
  GridOutline,
  LayersOutline,
  LinkOutline,
  MapOutline,
  OptionsOutline,
  RemoveOutline,
  SaveOutline,
  ScaleOutline,
  SwapHorizontalOutline,
} from '@vicons/ionicons5'
import { NButton, NDropdown, NIcon, NInput, NSelect } from 'naive-ui'
import { computed, h } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { paperGeometry } from '../protocol/units'
import { formatPrintZoom, PRINT_ZOOM_LEVELS } from './designerView'

const props = defineProps({
  local: Boolean,
  externalDirty: Boolean,
  pageMode: Boolean,
  templateName: { type: String, default: '' },
  designStatus: { type: [String, Number], default: '' },
  draftRevision: { type: [String, Number], default: '' },
  canManage: { type: Boolean, default: true },
  canPublish: Boolean,
  publishing: Boolean,
  nameDirty: Boolean,
})
const emit = defineEmits([
  'new',
  'copy',
  'save',
  'restore',
  'protocol',
  'calibration',
  'back',
  'update:templateName',
  'bindings',
  'versions',
  'publish',
])
const store = usePrintDesignerStore()
const zooms = PRINT_ZOOM_LEVELS.map(value => ({ label: formatPrintZoom(value), value }))
const geometry = computed(() => paperGeometry(store.document))
const paperOptions = [
  { label: 'A3 · 297 × 420 mm', value: 'A3' },
  { label: 'A4 · 210 × 297 mm', value: 'A4' },
  { label: 'A5 · 148 × 210 mm', value: 'A5' },
  { label: 'B4 · 250 × 353 mm', value: 'B4' },
  { label: 'B5 · 176 × 250 mm', value: 'B5' },
  { label: '80mm 连续纸 · 80 × 297 mm', value: 'RECEIPT' },
  { label: '标签 · 50 × 30 mm', value: 'LABEL' },
]
const paperName = computed(() => {
  const { widthMm, heightMm } = store.document.paper
  if (widthMm === 297 && heightMm === 420)
    return 'A3'
  if (widthMm === 210 && heightMm === 297)
    return 'A4'
  if (widthMm === 148 && heightMm === 210)
    return 'A5'
  if (widthMm === 250 && heightMm === 353)
    return 'B4'
  if (widthMm === 176 && heightMm === 250)
    return 'B5'
  if (widthMm === 80 && heightMm === 297)
    return 'RECEIPT'
  if (widthMm === 50 && heightMm === 30)
    return 'LABEL'
  return 'CUSTOM'
})
const paperSelectOptions = computed(() => {
  if (paperName.value === 'CUSTOM')
    return [{ label: `自定义 · ${geometry.value.widthMm} × ${geometry.value.heightMm} mm`, value: 'CUSTOM', disabled: true }, ...paperOptions]
  return paperOptions
})
const moreOptions = computed(() => {
  const localOptions = props.local
    ? [
        { label: '新建模板', key: 'new' },
        { label: '复制当前模板', key: 'copy' },
        { label: '恢复本地草稿', key: 'restore' },
        { type: 'divider', key: 'local-divider' },
      ]
    : []
  return [
    ...localOptions,
    { label: '打印校准与验收', key: 'calibration', icon: () => h(NIcon, null, { default: () => h(ScaleOutline) }) },
    { label: '导入 / 导出协议', key: 'protocol', icon: () => h(NIcon, null, { default: () => h(CodeSlashOutline) }) },
  ]
})

function setPaper(widthMm, heightMm) {
  store.setPaperSize(widthMm, heightMm)
}

function preset(value) {
  const sizes = { A3: [297, 420], A4: [210, 297], A5: [148, 210], B4: [250, 353], B5: [176, 250], RECEIPT: [80, 297], LABEL: [50, 30] }
  if (sizes[value])
    setPaper(...sizes[value])
}

function handleMore(key) {
  if (['new', 'copy', 'restore', 'protocol', 'calibration'].includes(key))
    emit(key)
}
</script>

<template>
  <header class="designer-toolbar">
    <template v-if="pageMode">
      <button type="button" class="tool-button" title="返回模板列表" aria-label="返回模板列表" @click="$emit('back')">
        <NIcon :component="ArrowBackOutline" />
      </button>
      <NInput
        :value="templateName"
        :input-props="{ 'aria-label': '模板名称' }"
        :maxlength="100"
        :disabled="!canManage"
        class="template-name"
        size="small"
        placeholder="模板名称"
        @update:value="$emit('update:templateName', $event)"
      />
      <div v-if="designStatus !== '' && designStatus != null" class="template-meta">
        <DictTag dict-type="sys_print_design_status" :value="designStatus" />
        <span>修订 {{ draftRevision }}</span>
      </div>
    </template>
    <div v-else class="toolbar-identity" :title="local ? '打印模板设计 · 本地草稿' : '打印模板设计'">
      <strong>打印设计</strong>
      <i class="save-dot" :class="{ dirty: store.dirty || externalDirty }" />
      <span class="save-state">{{ (store.dirty || externalDirty) ? '未保存' : '已保存' }}</span>
    </div>

    <div class="command-group panel-commands">
      <button type="button" class="tool-button" :class="{ active: store.leftPanelOpen }" title="显示或隐藏组件面板" aria-label="显示或隐藏组件面板" @click="store.leftPanelOpen = !store.leftPanelOpen">
        <NIcon :component="LayersOutline" />
      </button>
      <button type="button" class="tool-button" :class="{ active: store.rightPanelOpen }" title="显示或隐藏属性面板" aria-label="显示或隐藏属性面板" @click="store.rightPanelOpen = !store.rightPanelOpen">
        <NIcon :component="OptionsOutline" />
      </button>
    </div>

    <div class="command-group history-commands">
      <button type="button" class="tool-button" title="撤销" aria-label="撤销" :disabled="!store.canUndo" @click="store.undo()">
        <NIcon :component="ArrowUndoOutline" />
      </button>
      <button type="button" class="tool-button" title="重做" aria-label="重做" :disabled="!store.canRedo" @click="store.redo()">
        <NIcon :component="ArrowRedoOutline" />
      </button>
    </div>

    <div class="command-group paper-commands">
      <NSelect :value="paperName" aria-label="纸张规格" :options="paperSelectOptions" size="small" class="paper-select" @update:value="preset" />
      <button type="button" class="tool-button" :title="`${store.document.paper.orientation === 'PORTRAIT' ? '转为横向' : '转为纵向'} · 当前 ${geometry.widthMm} × ${geometry.heightMm} mm`" :aria-label="store.document.paper.orientation === 'PORTRAIT' ? '转为横向' : '转为纵向'" @click="store.rotatePaper()">
        <NIcon :component="SwapHorizontalOutline" />
      </button>
      <button type="button" class="tool-button" :class="{ active: store.showGrid }" title="显示或隐藏毫米网格" aria-label="显示或隐藏毫米网格" @click="store.toggleGrid()">
        <NIcon :component="GridOutline" />
      </button>
      <button type="button" class="tool-button" :class="{ active: store.miniMapOpen }" title="显示或隐藏概览图" aria-label="显示或隐藏概览图" @click="store.toggleMiniMap()">
        <NIcon :component="MapOutline" />
      </button>
    </div>

    <div class="command-group zoom-commands">
      <button type="button" class="tool-button" title="缩小画布" aria-label="缩小画布" :disabled="!store.canZoomOut" @click="store.nudgeZoom(-1)">
        <NIcon :component="RemoveOutline" />
      </button>
      <NSelect :value="store.zoom" aria-label="画布缩放" :options="zooms" size="small" class="zoom-select" @update:value="store.setZoom" />
      <button type="button" class="tool-button" title="放大画布" aria-label="放大画布" :disabled="!store.canZoomIn" @click="store.nudgeZoom(1)">
        <NIcon :component="AddOutline" />
      </button>
    </div>

    <div class="toolbar-spacer" />
    <div class="command-group final-commands">
      <template v-if="pageMode">
        <button type="button" class="tool-button" title="场景绑定" aria-label="场景绑定" @click="$emit('bindings')">
          <NIcon :component="LinkOutline" />
        </button>
        <button type="button" class="tool-button" title="发布版本" aria-label="发布版本" @click="$emit('versions')">
          <NIcon :component="GitBranchOutline" />
        </button>
      </template>
      <button type="button" class="tool-button" title="预览打印结果" aria-label="预览打印结果" :disabled="!!store.gesture || store.fieldIssues.length > 0" @click="store.previewOpen = true">
        <NIcon :component="EyeOutline" />
      </button>
      <NDropdown trigger="click" :options="moreOptions" @select="handleMore">
        <button type="button" class="tool-button" title="更多操作" aria-label="更多操作" :disabled="store.saving">
          <NIcon :component="EllipsisHorizontalOutline" />
        </button>
      </NDropdown>
      <NButton size="small" type="primary" :loading="store.saving" :disabled="!!store.gesture" class="save-button" @click="$emit('save')">
        <template #icon>
          <NIcon :component="SaveOutline" />
        </template>
        保存
      </NButton>
      <NButton
        v-if="pageMode && canPublish"
        size="small"
        type="primary"
        secondary
        :loading="publishing"
        :disabled="nameDirty || (canManage && store.dirty) || !String(templateName || '').trim()"
        @click="$emit('publish')"
      >
        <template #icon>
          <NIcon :component="CloudUploadOutline" />
        </template>
        发布
      </NButton>
    </div>
  </header>
</template>

<style scoped>
.designer-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 40px;
  padding: 4px 8px;
  overflow-x: auto;
  border-bottom: 1px solid var(--border-light, #ddd);
  background: var(--bg-primary, #fff);
  scrollbar-width: thin;
}
.toolbar-identity,
.command-group,
.template-meta {
  display: flex;
  align-items: center;
  flex: none;
}
.toolbar-identity {
  gap: 6px;
  padding: 0 5px 0 3px;
}
.toolbar-identity strong {
  font-size: 13px;
  white-space: nowrap;
}
.template-name {
  width: min(200px, 28vw);
}
.template-meta {
  gap: 6px;
  color: #64748b;
  font-size: 11px;
  white-space: nowrap;
}
.save-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-tertiary, #94a3b8);
}
.save-dot.dirty {
  background: var(--primary-color);
}
.save-state {
  color: var(--text-tertiary, #777);
  font-size: 11px;
  white-space: nowrap;
}
.command-group {
  gap: 2px;
  padding-left: 6px;
  border-left: 1px solid var(--border-light, #ddd);
}
.tool-button {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-primary, #334155);
  background: transparent;
  cursor: pointer;
  font-size: 15px;
}
.tool-button:hover:not(:disabled),
.tool-button.active {
  color: var(--primary-color);
  border-color: color-mix(in srgb, var(--primary-color) 25%, transparent);
  background: color-mix(in srgb, var(--primary-color) 9%, transparent);
}
.tool-button:disabled {
  opacity: 0.32;
  cursor: not-allowed;
}
.paper-select {
  width: 154px;
}
.zoom-select {
  width: 96px;
}
.toolbar-spacer {
  flex: 1 1 auto;
  min-width: 6px;
}
.save-button {
  min-width: 70px;
}
@media (max-width: 840px) {
  .toolbar-identity .save-state,
  .history-commands,
  .template-meta {
    display: none;
  }
  .paper-select {
    width: 120px;
  }
  .template-name {
    width: min(140px, 36vw);
  }
}
</style>
