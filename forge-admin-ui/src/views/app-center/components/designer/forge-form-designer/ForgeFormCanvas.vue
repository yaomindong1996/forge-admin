<template>
  <div
    class="forge-form-canvas"
    :class="{ 'preview-mode': previewMode }"
    @wheel="handleCanvasWheel"
  >
    <transition name="drop-error">
      <div v-if="designerDropError" class="drop-error-toast">
        {{ designerDropError }}
      </div>
    </transition>
    <div
      class="canvas-scroll"
      @click="handleCanvasBlankClick"
      @dragover.prevent
      @drop="handleRootDrop"
    >
      <div class="canvas-stage-shell" :style="canvasStageShellStyle">
        <div class="canvas-stage" :style="canvasStageStyle">
          <div
            v-if="!schema.components?.length"
            class="canvas-empty"
            :class="{ active: rootDropActive }"
            @dragenter.prevent="rootDropActive = hasForgeDragType($event)"
            @dragleave="rootDropActive = false"
            @drop.stop="handleDropAt(0, $event)"
          >
            <strong>把字段拖到这里</strong>
            <span>也可以从左侧点击字段快速添加。</span>
          </div>
          <n-form
            v-else
            class="form-grid"
            :model="previewModel"
            :label-placement="schema.layout?.labelPlacement || 'left'"
            :label-width="schema.layout?.labelWidth || 'auto'"
            :label-align="schema.layout?.labelAlign || 'right'"
            :size="schema.layout?.size || 'medium'"
            :show-feedback="false"
            :style="gridStyle"
          >
            <div
              class="root-drop-zone"
              :class="{ active: rootTopActive }"
              @dragenter.prevent="handleRootTopDragOver"
              @dragover.prevent="handleRootTopDragOver"
              @drop.stop="handleDropAt(0, $event)"
            />
            <ForgeFormCanvasNode
              v-for="(component, index) in schema.components"
              :key="component.id"
              :component="component"
              :fields="fields"
              :schema="schema"
              :selected-id="selectedId"
              :depth="0"
              parent-id=""
              :index="index"
              @select="$emit('update:selectedId', $event)"
              @update:schema="$emit('update:schema', $event)"
              @configure="$emit('configure', $event)"
              @configure-sub-table="$emit('configureSubTable', $event)"
              @drop-before="handleDropAt(index, $event)"
              @drop-after="handleDropAt(index + 1, $event)"
            />
          </n-form>
        </div>
      </div>
    </div>
    <div v-if="!previewMode" class="canvas-viewport-dock">
      <div class="viewport-device-group" aria-label="预览设备">
        <button
          type="button"
          class="viewport-device-button"
          :class="{ active: canvasPreviewMode === 'desktop' }"
          title="Desktop"
          @click.stop="applyCanvasPreviewMode('desktop')"
        >
          <n-icon><DesktopOutline /></n-icon>
        </button>
        <button
          type="button"
          class="viewport-device-button"
          :class="{ active: canvasPreviewMode === 'narrow' }"
          title="Tablet"
          @click.stop="applyCanvasPreviewMode('narrow')"
        >
          <n-icon><TabletLandscapeOutline /></n-icon>
        </button>
        <button
          type="button"
          class="viewport-device-button"
          :class="{ active: canvasPreviewMode === 'mobile' }"
          title="Mobile"
          @click.stop="applyCanvasPreviewMode('mobile')"
        >
          <n-icon><PhonePortraitOutline /></n-icon>
        </button>
      </div>
      <n-popover trigger="click" placement="bottom" :width="282" :to="false">
        <template #trigger>
          <n-button class="viewport-icon-button" circle secondary title="预览形态和设计宽度">
            <template #icon>
              <n-icon><BrowsersOutline /></n-icon>
            </template>
          </n-button>
        </template>
        <div class="canvas-viewport-popover">
          <div class="viewport-popover-head">
            <strong>预览视口</strong>
            <span>{{ canvasPreviewModeLabel }} · {{ canvasDesignWidth }}px</span>
          </div>
          <label class="canvas-viewport-field">
            <span>预览形态</span>
            <n-select
              :value="canvasPreviewMode"
              :options="canvasPreviewModeOptions"
              size="small"
              class="canvas-preview-select"
              @update:value="applyCanvasPreviewMode"
            />
          </label>
          <label class="canvas-viewport-field">
            <span>设计宽度</span>
            <n-select
              :value="canvasDesignWidth"
              :options="canvasWidthOptions"
              size="small"
              class="canvas-width-select"
              @update:value="updateCanvasDesignWidth"
            />
          </label>
          <label class="canvas-viewport-field">
            <span>自定义宽度</span>
            <n-input-number
              :value="canvasDesignWidth"
              :min="375"
              :max="1920"
              :step="10"
              size="small"
              class="canvas-width-input"
              :show-button="false"
              @update:value="updateCanvasDesignWidth"
            />
          </label>
        </div>
      </n-popover>
      <span class="viewport-divider" />
      <n-button class="viewport-icon-button" circle secondary title="缩小" @click.stop="updateCanvasZoom(canvasZoom - 0.1)">
        <template #icon>
          <n-icon><RemoveOutline /></n-icon>
        </template>
      </n-button>
      <n-popover trigger="click" placement="bottom" :width="238" :to="false">
        <template #trigger>
          <n-button class="viewport-zoom-button" secondary title="画布缩放">
            <template #icon>
              <n-icon><ResizeOutline /></n-icon>
            </template>
            {{ canvasZoomLabel }}
          </n-button>
        </template>
        <div class="canvas-viewport-popover">
          <div class="viewport-popover-head">
            <strong>画布缩放</strong>
            <span>Ctrl/⌘ + 滚轮也可缩放</span>
          </div>
          <div class="canvas-viewport-zoom-actions">
            <n-button size="small" secondary @click.stop="updateCanvasZoom(canvasZoom - 0.1)">
              -
            </n-button>
            <n-select
              :value="canvasZoom"
              :options="canvasZoomOptions"
              size="small"
              class="canvas-zoom-select"
              @click.stop
              @update:value="updateCanvasZoom"
            />
            <n-button size="small" secondary @click.stop="updateCanvasZoom(canvasZoom + 0.1)">
              +
            </n-button>
          </div>
          <n-button size="small" quaternary block @click.stop="updateCanvasZoom(1)">
            重置为 100%
          </n-button>
        </div>
      </n-popover>
      <n-button class="viewport-icon-button" circle secondary title="放大" @click.stop="updateCanvasZoom(canvasZoom + 0.1)">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
      </n-button>
      <span class="viewport-divider" />
      <n-button class="viewport-icon-button" circle secondary title="查看源码" @click.stop="$emit('openSource')">
        <template #icon>
          <n-icon><CodeSlashOutline /></n-icon>
        </template>
      </n-button>
      <n-button class="viewport-icon-button" circle secondary title="专注画布" @click.stop="$emit('toggleFocus')">
        <template #icon>
          <n-icon><ExpandOutline /></n-icon>
        </template>
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { AddOutline, BrowsersOutline, CodeSlashOutline, DesktopOutline, ExpandOutline, PhonePortraitOutline, RemoveOutline, ResizeOutline, TabletLandscapeOutline } from '@vicons/ionicons5'
import { computed, reactive, ref } from 'vue'
import { createComponentFromField, insertDesignerComponent, moveDesignerComponent, normalizeFormDesignerSchema } from '../form-first/formDesignerSchema'
import { clearDesignerDropKey, designerDropError, designerDropKey, setDesignerDropKey } from './designerDragState'
import { createForgeFieldTemplateComponent, createForgeLayoutComponent } from './designerLayoutFactory'
import { normalizeFormLayoutColumns } from './formLayoutConfig'
import ForgeFormCanvasNode from './ForgeFormCanvasNode.vue'

const props = defineProps({
  schema: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: String,
    default: '',
  },
  previewMode: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:schema', 'update:selectedId', 'configure', 'configureSubTable', 'openSource', 'toggleFocus'])
const DRAG_COMPONENT_MIME = 'application/x-forge-form-component'
const DRAG_FIELD_MIME = 'application/x-forge-form-field'
const DRAG_LAYOUT_MIME = 'application/x-forge-form-layout'
const DRAG_TEMPLATE_MIME = 'application/x-forge-form-template'
const FORGE_DRAG_TYPES = [DRAG_COMPONENT_MIME, DRAG_FIELD_MIME, DRAG_LAYOUT_MIME, DRAG_TEMPLATE_MIME]
const previewModel = reactive({})
const rootDropActive = ref(false)
const canvasZoom = ref(1)
const canvasDesignWidth = ref(1200)
const canvasPreviewMode = ref('desktop')
const canvasWidthOptions = [
  { label: '390 移动', value: 390 },
  { label: '720 抽屉', value: 720 },
  { label: '768 窄屏', value: 768 },
  { label: '960 弹窗', value: 960 },
  { label: '1200 默认', value: 1200 },
  { label: '1366 桌面', value: 1366 },
  { label: '1440', value: 1440 },
  { label: '1920', value: 1920 },
]
const canvasZoomOptions = [
  { label: '67%', value: 0.67 },
  { label: '75%', value: 0.75 },
  { label: '90%', value: 0.9 },
  { label: '100%', value: 1 },
  { label: '110%', value: 1.1 },
  { label: '125%', value: 1.25 },
]
const canvasPreviewModeOptions = [
  { label: '桌面', value: 'desktop' },
  { label: '窄屏', value: 'narrow' },
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
  { label: '移动', value: 'mobile' },
]
const canvasZoomLabel = computed(() => `${Math.round(canvasZoom.value * 100)}%`)
const canvasPreviewModeLabel = computed(() => canvasPreviewModeOptions.find(item => item.value === canvasPreviewMode.value)?.label || '桌面')
const rootTopActive = computed(() => designerDropKey.value === 'root:before')
const gridColumns = computed(() => normalizeFormLayoutColumns(props.schema.layout?.gridColumns || 2))
const gridStyle = computed(() => {
  const formStyle = isPlainObject(props.schema.layout?.formStyle) ? props.schema.layout.formStyle : {}
  const textColor = formStyle.color || undefined
  return {
    ...formStyle,
    // 供 Naive 标签/输入继承表单级文字色
    ...(textColor
      ? {
          color: textColor,
          '--n-text-color': textColor,
          '--n-label-text-color': textColor,
          '--forge-form-color': textColor,
        }
      : {}),
    gridTemplateColumns: `repeat(${gridColumns.value}, minmax(0, 1fr))`,
    columnGap: `${resolveGap(props.schema.layout?.columnGap, 16)}px`,
    rowGap: `${resolveGap(props.schema.layout?.rowGap, 16)}px`,
  }
})
const canvasStageShellStyle = computed(() => ({
  width: `${Math.round(canvasDesignWidth.value * canvasZoom.value)}px`,
  minWidth: '100%',
}))
const canvasStageStyle = computed(() => ({
  width: `${canvasDesignWidth.value}px`,
  transform: `scale(${canvasZoom.value})`,
  transformOrigin: '50% 0',
}))

function updateCanvasDesignWidth(value) {
  canvasDesignWidth.value = clamp(Number(value) || 1200, 375, 1920)
}

function applyCanvasPreviewMode(value = 'desktop') {
  const modeMap = {
    desktop: { width: 1200, zoom: 1 },
    narrow: { width: 768, zoom: 0.9 },
    modal: { width: 960, zoom: 0.9 },
    drawer: { width: 720, zoom: 0.9 },
    mobile: { width: 390, zoom: 1 },
  }
  const next = modeMap[value] || modeMap.desktop
  canvasPreviewMode.value = value
  updateCanvasDesignWidth(next.width)
  updateCanvasZoom(next.zoom)
}

function updateCanvasZoom(value) {
  canvasZoom.value = clamp(Number(value) || 1, 0.67, 1.25)
}

function handleCanvasWheel(event) {
  if (props.previewMode || (!event.ctrlKey && !event.metaKey))
    return
  event.preventDefault()
  const delta = event.deltaY > 0 ? -0.08 : 0.08
  updateCanvasZoom(Number((canvasZoom.value + delta).toFixed(2)))
}

function handleRootDrop(event) {
  if (props.previewMode)
    return
  handleDropAt(props.schema.components?.length || 0, event)
}

function handleCanvasBlankClick(event) {
  if (props.previewMode)
    return
  if (event.target?.closest?.('.canvas-node'))
    return
  emit('update:selectedId', '')
}

function handleDropAt(index, event) {
  if (props.previewMode)
    return
  rootDropActive.value = false
  clearDesignerDropKey()
  const result = applyDropPayload(event, { parentId: '', index })
  if (result)
    emit('update:schema', result)
}

function handleRootTopDragOver(event) {
  if (props.previewMode)
    return
  if (!hasForgeDragType(event))
    return
  setDesignerDropKey('root:before')
}

function applyDropPayload(event, target) {
  const sourceId = event.dataTransfer.getData(DRAG_COMPONENT_MIME)
  if (sourceId) {
    emit('update:selectedId', sourceId)
    return moveDesignerComponent(props.schema, sourceId, target)
  }

  const fieldText = event.dataTransfer.getData(DRAG_FIELD_MIME)
  if (fieldText) {
    const field = parsePayload(fieldText)
    const component = createComponentFromField(field, props.schema.components?.length || 0)
    emit('update:selectedId', component.id)
    return insertDesignerComponent(props.schema, target, component)
  }

  const layoutText = event.dataTransfer.getData(DRAG_LAYOUT_MIME)
  if (layoutText) {
    const layout = parsePayload(layoutText)
    const component = createForgeLayoutComponent(layout.componentKey, props.schema)
    emit('update:selectedId', component.id)
    return insertDesignerComponent(props.schema, target, component)
  }

  const templateText = event.dataTransfer.getData(DRAG_TEMPLATE_MIME)
  if (templateText) {
    const template = parsePayload(templateText)
    const component = createForgeFieldTemplateComponent(template, props.schema)
    emit('update:selectedId', component.id)
    return insertDesignerComponent(props.schema, target, component)
  }
  return null
}

function hasForgeDragType(event) {
  const types = Array.from(event.dataTransfer?.types || [])
  return FORGE_DRAG_TYPES.some(type => types.includes(type))
}

function parsePayload(value) {
  try {
    return JSON.parse(value || '{}')
  }
  catch {
    return {}
  }
}

function resolveGap(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

defineExpose({
  normalize: () => normalizeFormDesignerSchema(props.schema),
})
</script>

<style scoped>
.forge-form-canvas {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: radial-gradient(circle at 1px 1px, rgba(113, 113, 122, 0.22) 1px, transparent 0), #f8f9fa;
  background-size: 20px 20px;
  overscroll-behavior: contain;
}

.canvas-scroll {
  height: 100%;
  min-height: 0;
  overflow: auto;
  /* 顶部留给悬浮工具条，上下留白大致对称 */
  padding: 40px 16px 20px;
  overscroll-behavior: contain;
}

.drop-error-toast {
  position: absolute;
  top: 10px;
  right: 18px;
  left: 18px;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  width: max-content;
  max-width: calc(100% - 36px);
  margin: 0 auto;
  padding: 6px 12px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: #fff1f2;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(185, 28, 28, 0.1);
}

.drop-error-enter-active,
.drop-error-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.drop-error-enter-from,
.drop-error-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.canvas-viewport-dock {
  position: absolute;
  z-index: 8;
  top: 8px;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: max-content;
  max-width: calc(100% - 28px);
  margin: 0;
  padding: 4px 8px;
  border: 1px solid rgba(228, 228, 231, 0.9);
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-primary) 94%, transparent);
  box-shadow:
    0 4px 12px rgba(15, 23, 42, 0.06),
    0 1px 3px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
  transform: translateX(-50%);
}

.viewport-divider {
  flex: 0 0 auto;
  width: 1px;
  height: 14px;
  margin: 0 1px;
  background: #e4e4e7;
}

.viewport-device-group {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  padding-right: 4px;
  border-right: 1px solid #e4e4e7;
}

.viewport-device-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  cursor: pointer;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 13px;
  padding: 0;
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.viewport-device-button:hover {
  background: #f4f4f5;
  color: var(--text-secondary);
}

.viewport-device-button.active {
  background: var(--primary-color, #3370ff);
  color: #fff;
  box-shadow: 0 1px 2px rgba(51, 112, 255, 0.28);
}

.canvas-viewport-popover {
  display: grid;
  gap: 10px;
}

.canvas-viewport-field {
  display: grid;
  gap: 5px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.viewport-popover-head {
  display: grid;
  gap: 2px;
}

.viewport-popover-head strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 18px;
}

.viewport-popover-head span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 16px;
  white-space: nowrap;
}

.canvas-viewport-zoom-actions {
  display: grid;
  grid-template-columns: 34px minmax(92px, 1fr) 34px;
  align-items: center;
  gap: 6px;
}

.canvas-preview-select,
.canvas-width-select,
.canvas-width-input,
.canvas-zoom-select {
  width: 100%;
}

.canvas-width-input :deep(.n-input__input-el) {
  text-align: center;
}

.viewport-icon-button {
  --n-color: var(--bg-primary) !important;
  --n-color-hover: #f4f6ff !important;
  --n-color-pressed: #e8edff !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-pressed: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #c7d2fe !important;
  --n-text-color: var(--text-secondary) !important;
  --n-text-color-hover: #3153d8 !important;
  --n-text-color-pressed: #253fb2 !important;
  --n-text-color-focus: #3153d8 !important;
  width: 24px !important;
  height: 24px !important;
  --n-height: 24px !important;
  --n-width: 24px !important;
  --n-font-size: 12px !important;
  --n-padding: 0 !important;
  font-weight: 700;
  border-radius: 999px !important;
}

.viewport-zoom-button {
  --n-color: var(--bg-primary) !important;
  --n-color-hover: #f4f6ff !important;
  --n-color-pressed: #e8edff !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #c7d2fe !important;
  --n-border-pressed: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #c7d2fe !important;
  --n-text-color: var(--text-secondary) !important;
  --n-text-color-hover: #3153d8 !important;
  --n-text-color-pressed: #253fb2 !important;
  --n-text-color-focus: #3153d8 !important;
  --n-height: 24px !important;
  --n-font-size: 11px !important;
  min-width: 56px;
  height: 24px !important;
  padding: 0 6px !important;
  font-size: 11px;
  font-weight: 700;
  border-radius: 999px !important;
}

.canvas-stage-shell {
  display: flex;
  justify-content: center;
  min-height: 100%;
  padding-top: 10px;
  padding-bottom: 56px;
  transition: width 160ms ease;
}

.canvas-stage {
  flex: none;
  min-height: 100%;
  margin: 0 auto;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
  transition:
    width 160ms ease,
    transform 160ms ease;
  will-change: transform;
}

.form-grid {
  display: grid;
  gap: 10px 12px;
  align-items: start;
}

/* 表单级文字色落到画布节点 */
.form-grid :deep(.canvas-node) {
  color: inherit;
}

.canvas-empty {
  position: relative;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  min-height: 420px;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1e40af;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.canvas-empty.active,
.canvas-empty:hover {
  border-color: #9ca3af;
  background: var(--bg-tertiary);
  box-shadow: 8px 8px 0 rgba(148, 163, 184, 0.36);
}

.canvas-empty strong {
  font-size: 15px;
}

.canvas-empty span {
  color: var(--text-secondary);
  font-size: 12px;
}

.root-drop-zone {
  grid-column: 1 / -1;
  position: relative;
  height: 8px;
  margin: -4px 0;
  overflow: visible;
  border-radius: 6px;
  transition:
    background 160ms ease,
    border-color 160ms ease;
}

.root-drop-zone.active {
  height: var(--forge-designer-drag-height, 72px);
  margin: 4px 0;
  margin-bottom: 4px;
  border: 1px dashed #9ca3af;
  background: var(--bg-tertiary);
  box-shadow: none;
}

.root-drop-zone::before {
  display: none;
}

.root-drop-zone.active::before {
  display: none;
}

.preview-mode {
  background: var(--bg-secondary);
}

.preview-mode .canvas-stage-shell {
  width: 100% !important;
  padding-top: 0;
}

.preview-mode .canvas-stage {
  max-width: 860px;
  min-height: auto;
  box-shadow: none;
  transform: none !important;
}

.preview-mode :deep(.node-overlay),
.preview-mode :deep(.drop-line),
.preview-mode :deep(.root-drop-zone) {
  display: none;
}

.preview-mode :deep(.canvas-node),
.preview-mode :deep(.canvas-node:hover),
.preview-mode :deep(.canvas-node.selected) {
  border-color: transparent;
  box-shadow: none;
  cursor: default;
}

@media (max-width: 900px) {
  .forge-form-canvas {
    padding: 0;
  }

  .canvas-scroll {
    padding: 40px 8px 12px;
  }

  .canvas-viewport-dock {
    top: 8px;
    left: 50%;
    right: auto;
  }

  .canvas-stage-shell {
    padding-top: 6px;
  }
}
</style>
