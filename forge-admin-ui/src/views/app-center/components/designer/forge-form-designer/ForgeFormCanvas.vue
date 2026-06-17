<template>
  <div class="forge-form-canvas" :class="{ 'preview-mode': previewMode }" @click="handleCanvasBlankClick" @dragover.prevent @drop="handleRootDrop">
    <div class="canvas-stage">
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
          @drop-before="handleDropAt(index, $event)"
          @drop-after="handleDropAt(index + 1, $event)"
        />
      </n-form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { createComponentFromField, insertDesignerComponent, moveDesignerComponent, normalizeFormDesignerSchema } from '../form-first/formDesignerSchema'
import { clearDesignerDropKey, designerDropKey, setDesignerDropKey } from './designerDragState'
import { createForgeFieldTemplateComponent, createForgeLayoutComponent } from './designerLayoutFactory'
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

const emit = defineEmits(['update:schema', 'update:selectedId', 'configure'])
const DRAG_COMPONENT_MIME = 'application/x-forge-form-component'
const DRAG_FIELD_MIME = 'application/x-forge-form-field'
const DRAG_LAYOUT_MIME = 'application/x-forge-form-layout'
const DRAG_TEMPLATE_MIME = 'application/x-forge-form-template'
const FORGE_DRAG_TYPES = [DRAG_COMPONENT_MIME, DRAG_FIELD_MIME, DRAG_LAYOUT_MIME, DRAG_TEMPLATE_MIME]
const previewModel = reactive({})
const rootDropActive = ref(false)
const rootTopActive = computed(() => designerDropKey.value === 'root:before')
const MAX_FORM_GRID_COLUMNS = 6
const gridColumns = computed(() => Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number(props.schema.layout?.gridColumns || 2))))
const gridStyle = computed(() => ({
  ...(isPlainObject(props.schema.layout?.formStyle) ? props.schema.layout.formStyle : {}),
  gridTemplateColumns: `repeat(${gridColumns.value}, minmax(0, 1fr))`,
  columnGap: `${resolveGap(props.schema.layout?.columnGap, 16)}px`,
  rowGap: `${resolveGap(props.schema.layout?.rowGap, 16)}px`,
}))

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

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

defineExpose({
  normalize: () => normalizeFormDesignerSchema(props.schema),
})
</script>

<style scoped>
.forge-form-canvas {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 10px;
  background:
    linear-gradient(#eef2f7 1px, transparent 1px), linear-gradient(90deg, #eef2f7 1px, transparent 1px), #f8fafc;
  background-size: 24px 24px;
}

.canvas-stage {
  max-width: 1280px;
  min-height: 100%;
  margin: 0 auto;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.06);
}

.form-grid {
  display: grid;
  gap: 10px 12px;
  align-items: start;
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
  background: #f3f4f6;
  box-shadow: 8px 8px 0 rgba(148, 163, 184, 0.36);
}

.canvas-empty strong {
  font-size: 15px;
}

.canvas-empty span {
  color: #64748b;
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
  background: #f3f4f6;
  box-shadow: none;
}

.root-drop-zone::before {
  display: none;
}

.root-drop-zone.active::before {
  display: none;
}

.preview-mode {
  background: #f8fafc;
}

.preview-mode .canvas-stage {
  max-width: 860px;
  min-height: auto;
  box-shadow: none;
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
</style>
