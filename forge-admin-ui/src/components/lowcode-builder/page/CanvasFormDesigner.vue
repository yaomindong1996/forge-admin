<template>
  <div class="canvas-designer">
    <div class="canvas-toolbar">
      <div class="canvas-toolbar-head">
        <div>
          <div class="canvas-title">
            {{ zoneTitle }}
          </div>
          <div class="canvas-subtitle">
            Leafer Canvas · {{ canvasItems.length }} 个组件 · {{ canvas.width }} × {{ canvas.height }}
          </div>
        </div>
        <div class="enable-switch">
          <span>启用</span>
          <NSwitch
            :value="zone?.enabled !== false"
            size="small"
            @update:value="handleEnabledChange"
          />
        </div>
      </div>
      <div class="canvas-toolbar-actions">
        <div v-if="showLayoutModes" class="toolbar-group">
          <span class="toolbar-label">布局</span>
          <div class="layout-mode-group">
            <NButton
              v-for="cols in [1, 2, 3]"
              :key="cols"
              size="small"
              :type="activeLayoutCols === cols ? 'primary' : 'default'"
              :secondary="activeLayoutCols === cols"
              @click="applyColumnLayout(cols)"
            >
              {{ cols }}列
            </NButton>
          </div>
          <NButton size="small" @click="autoArrange">
            自动排列
          </NButton>
        </div>
        <div class="toolbar-group">
          <span class="toolbar-label">画布</span>
          <NInputNumber
            :value="canvas.width"
            :min="720"
            :max="2000"
            :step="40"
            :show-button="false"
            size="small"
            class="size-input"
            @update:value="updateCanvasSize('width', $event)"
          />
          <span class="size-divider">×</span>
          <NInputNumber
            :value="canvas.height"
            :min="360"
            :max="1800"
            :step="40"
            :show-button="false"
            size="small"
            class="size-input"
            @update:value="updateCanvasSize('height', $event)"
          />
        </div>
        <NButton v-if="!showLayoutModes" size="small" @click="autoArrange">
          自动排列
        </NButton>
      </div>
    </div>

    <div
      ref="scrollRef"
      class="canvas-scroll"
      @dragover.prevent
      @drop="handleDrop"
    >
      <div
        ref="boardRef"
        class="canvas-board"
        :style="boardStyle"
      >
        <div class="canvas-grid" />
        <div ref="leaferViewRef" class="leafer-layer" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { NButton, NInputNumber, NSwitch } from 'naive-ui'
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getDictData } from '@/composables/useDict'
import {
  createCanvasItem,
  patchZoneCanvas,
  resolveCanvasComponent,
  resolveComponentTypeFromComponentKey,
  resolveZoneTitle,
} from './page-schema'

const props = defineProps({
  zone: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selectedItemId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:zone', 'selectItem'])

const scrollRef = ref(null)
const boardRef = ref(null)
const leaferViewRef = ref(null)
const liveLayouts = ref({})
const dictOptionsMap = ref({})
const shapeMap = new Map()
let leaferApp = null
let leaferApi = null
let resizeObserver
let transformTimer = null
let destroyed = false

const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
const zoneTitle = computed(() => resolveZoneTitle(props.zone?.zoneKey))
const canvas = computed(() => props.zone?.props?.canvas || { width: 1040, height: 460, snap: 8, items: [] })
const canvasItems = computed(() => canvas.value.items || [])
const showLayoutModes = computed(() => ['edit', 'detail'].includes(props.zone?.zoneKey))
const activeLayoutCols = computed(() => Math.max(1, Math.min(3, resolveCanvasColumnCount(canvasItems.value))))
const overlayItems = computed(() => {
  return canvasItems.value
    .map(item => ({
      ...item,
      ...(liveLayouts.value[item.id] || {}),
    }))
    .sort((a, b) => Number(a.zIndex || 0) - Number(b.zIndex || 0))
})
const boardStyle = computed(() => ({
  width: `${canvas.value.width || 1040}px`,
  height: `${canvas.value.height || 460}px`,
}))
const usedFields = computed(() => {
  const refs = new Set()
  canvasItems.value.forEach((item) => {
    if (item.fieldRef)
      refs.add(item.fieldRef)
    ;(item.fieldRefs || item.props?.fieldRefs || []).forEach(ref => refs.add(ref))
  })
  return Array.from(refs).map(ref => fieldMap.value.get(ref)).filter(Boolean)
})
const dictTypes = computed(() => Array.from(new Set(usedFields.value.map(field => field.dictType).filter(Boolean))))

watch(
  () => props.zone?.props?.canvas,
  () => nextTick(renderLeaferItems),
  { deep: true },
)

watch(
  () => props.selectedItemId,
  () => nextTick(syncEditorSelection),
)

watch(dictTypes, loadDictOptions, { immediate: true })

onMounted(async () => {
  await initLeafer()
  resizeObserver = new ResizeObserver(() => resizeLeafer())
  if (boardRef.value)
    resizeObserver.observe(boardRef.value)
  window.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  destroyed = true
  if (transformTimer)
    window.clearTimeout(transformTimer)
  window.removeEventListener('keydown', handleGlobalKeydown)
  resizeObserver?.disconnect()
  leaferApp?.destroy?.()
  leaferApp = null
  leaferApi = null
  shapeMap.clear()
})

async function initLeafer() {
  if (destroyed || !leaferViewRef.value || leaferApp)
    return
  const api = await import('leafer-editor')
  if (destroyed || !leaferViewRef.value || leaferApp)
    return
  leaferApi = markRaw(api)
  const app = markRaw(new api.App({
    view: leaferViewRef.value,
    width: canvas.value.width,
    height: canvas.value.height,
    editor: {
      select: 'tap',
      moveable: true,
      resizeable: true,
      rotateable: false,
      skewable: false,
      lockRatio: false,
      stroke: '#2563eb',
      pointFill: '#ffffff',
      pointSize: 7,
      hover: false,
    },
  }))

  leaferApp = app
  app.editor?.on?.(api.EditorEvent.SELECT, handleEditorSelect)
  app.editor?.on?.(api.EditorMoveEvent.MOVE, handleEditorTransform)
  app.editor?.on?.(api.EditorScaleEvent.SCALE, handleEditorTransform)
  renderLeaferItems()
}

function renderLeaferItems() {
  const app = leaferApp
  const api = leaferApi
  if (!app || !api)
    return

  const layer = app.tree || app
  layer.clear?.()
  shapeMap.clear()

  overlayItems.value.forEach((item) => {
    const group = new api.Group({
      id: item.id,
      x: Number(item.x || 0),
      y: Number(item.y || 0),
      width: Number(item.w || 120),
      height: Number(item.h || 48),
      draggable: !item.locked,
      editable: !item.locked,
      cursor: item.locked ? 'default' : 'move',
      hitChildren: false,
      data: { itemId: item.id },
    })
    drawCanvasItem(api, group, item)
    group.on(api.PointerEvent.TAP, () => {
      emit('selectItem', item.id)
      app.editor?.select?.(group)
    })
    group.on(api.DragEvent.DRAG, () => {
      liveLayouts.value = {
        ...liveLayouts.value,
        [item.id]: readShapeLayout(group, item),
      }
    })
    group.on(api.DragEvent.END, () => {
      commitShapeLayout(group, item)
    })
    shapeMap.set(item.id, group)
    layer.add(group)
  })

  resizeLeafer()
  syncEditorSelection()
}

function drawCanvasItem(api, group, item) {
  const width = Number(item.w || 120)
  const height = Number(item.h || 48)
  const selected = item.id === props.selectedItemId
  const background = new api.Rect({
    x: 0,
    y: 0,
    width,
    height,
    fill: selected ? '#eff6ff' : item.style?.fill || '#ffffff',
    stroke: selected ? '#2563eb' : item.style?.stroke || '#cbd5e1',
    strokeWidth: selected ? 2 : 1,
    cornerRadius: Number(item.style?.radius || 6),
    editable: false,
    data: { itemId: item.id },
  })
  group.add(background)

  if (isButtonComponent(item.componentKey)) {
    drawButtonPreview(api, group, item, width, height)
    return
  }
  if (item.componentKey === 'query-set') {
    drawQuerySetPreview(api, group, item, width)
    return
  }
  if (item.componentKey === 'data-table') {
    drawTablePreview(api, group, item, width, height)
    return
  }
  if (props.zone?.zoneKey === 'detail' || item.componentKey === 'detail-field') {
    drawDetailPreview(api, group, item, width, height)
    return
  }
  drawFormFieldPreview(api, group, item, width, height)
}

function drawButtonPreview(api, group, item, width, height) {
  const isPrimary = item.componentKey === 'save-button' || item.componentKey === 'add-button'
  const buttonWidth = Math.min(width - 20, 104)
  const buttonHeight = Math.min(height - 16, 32)
  group.add(new api.Rect({
    x: 10,
    y: Math.max(8, (height - buttonHeight) / 2),
    width: buttonWidth,
    height: buttonHeight,
    fill: isPrimary ? '#2563eb' : '#ffffff',
    stroke: isPrimary ? '#2563eb' : '#cbd5e1',
    cornerRadius: 5,
    editable: false,
    data: { itemId: item.id },
  }))
  addText(api, group, item.label || resolveComponentTitle(item.componentKey), {
    x: 10,
    y: Math.max(8, (height - buttonHeight) / 2) + 8,
    width: buttonWidth,
    fill: isPrimary ? '#ffffff' : '#334155',
    textAlign: 'center',
    fontWeight: '600',
  })
}

function drawQuerySetPreview(api, group, item, width) {
  addItemHeader(api, group, item, width)
  const fields = resolveItemFields(item)
  addText(api, group, `${fields.length} 个查询字段`, {
    x: width - 116,
    y: 14,
    width: 96,
    fill: '#64748b',
    textAlign: 'right',
  })
  const chipWidth = Math.max(80, Math.floor((width - 52) / 4))
  fields.slice(0, 8).forEach((field, index) => {
    const col = index % 4
    const row = Math.floor(index / 4)
    const x = 18 + col * (chipWidth + 8)
    const y = 48 + row * 30
    group.add(new api.Rect({
      x,
      y,
      width: chipWidth,
      height: 22,
      fill: '#f8fafc',
      stroke: '#dbe3ee',
      cornerRadius: 11,
      editable: false,
      data: { itemId: item.id },
    }))
    addText(api, group, field.label || field.field, {
      x: x + 8,
      y: y + 5,
      width: chipWidth - 16,
      fill: '#475569',
    })
  })
  if (fields.length > 8) {
    addText(api, group, `+${fields.length - 8}`, {
      x: width - 50,
      y: 78,
      width: 32,
      fill: '#64748b',
      textAlign: 'right',
    })
  }
}

function drawTablePreview(api, group, item, width, height) {
  addItemHeader(api, group, item, width)
  const fields = resolveItemFields(item).slice(0, 5)
  const tableX = 16
  const tableY = 50
  const tableWidth = width - 32
  const colWidth = tableWidth / Math.max(fields.length, 1)
  const rowHeight = 32
  fields.forEach((field, index) => {
    const x = tableX + index * colWidth
    group.add(new api.Rect({
      x,
      y: tableY,
      width: colWidth,
      height: rowHeight,
      fill: '#f8fafc',
      stroke: '#e5e7eb',
      editable: false,
      data: { itemId: item.id },
    }))
    group.add(new api.Rect({
      x,
      y: tableY + rowHeight,
      width: colWidth,
      height: rowHeight,
      fill: '#ffffff',
      stroke: '#e5e7eb',
      editable: false,
      data: { itemId: item.id },
    }))
    addText(api, group, field.label || field.field, {
      x: x + 8,
      y: tableY + 9,
      width: colWidth - 16,
      fill: '#334155',
      fontWeight: '600',
    })
    addText(api, group, resolveSampleValue(field), {
      x: x + 8,
      y: tableY + rowHeight + 9,
      width: colWidth - 16,
      fill: '#64748b',
    })
  })
  addText(api, group, `共 ${resolveItemFields(item).length} 列`, {
    x: 16,
    y: Math.min(height - 28, tableY + rowHeight * 2 + 16),
    width: width - 32,
    fill: '#64748b',
  })
}

function drawDetailPreview(api, group, item, width, height) {
  const field = resolveItemField(item)
  const labelWidth = Math.max(80, Math.min(150, Number(item.style?.labelWidth || 104)))
  const contentHeight = Math.max(30, height - 20)
  addItemModelBadge(api, group, item, field, width, 12)
  group.add(new api.Rect({
    x: 10,
    y: 10,
    width: labelWidth,
    height: contentHeight,
    fill: '#f8fafc',
    stroke: '#e5e7eb',
    cornerRadius: [5, 0, 0, 5],
    editable: false,
    data: { itemId: item.id },
  }))
  group.add(new api.Rect({
    x: 10 + labelWidth,
    y: 10,
    width: Math.max(40, width - labelWidth - 20),
    height: contentHeight,
    fill: '#ffffff',
    stroke: '#e5e7eb',
    cornerRadius: [0, 5, 5, 0],
    editable: false,
    data: { itemId: item.id },
  }))
  addText(api, group, item.label || '字段', {
    x: 20,
    y: 10 + contentHeight / 2 - 6,
    width: labelWidth - 20,
    fill: '#64748b',
  })
  addText(api, group, resolveSampleValue(resolveItemField(item)), {
    x: 22 + labelWidth,
    y: 10 + contentHeight / 2 - 6,
    width: Math.max(24, width - labelWidth - 42),
    fill: '#0f172a',
  })
}

function drawFormFieldPreview(api, group, item, width, height) {
  const labelWidth = Math.max(60, Math.min(width - 110, Number(item.style?.labelWidth || 86)))
  addItemHeader(api, group, item, width)
  addText(api, group, item.label || '字段', {
    x: 14,
    y: Math.max(40, height / 2 + 4),
    width: labelWidth,
    fill: '#475569',
  })
  drawControlSketch(api, group, item, 22 + labelWidth, 40, Math.max(72, width - labelWidth - 36), Math.max(28, height - 52))
}

function drawControlSketch(api, group, item, x, y, width, height) {
  const field = resolveItemField(item)
  const componentType = field.componentType
  const componentKey = item.componentKey
  const isSwitch = componentKey === 'field-switch' || componentType === 'switch'
  const isUpload = componentKey === 'field-upload' || componentType === 'fileUpload' || componentType === 'imageUpload'
  const isTextarea = componentKey === 'field-textarea' || componentType === 'textarea'
  const controlHeight = isTextarea ? Math.max(44, height) : Math.min(34, height)

  if (isSwitch) {
    group.add(new api.Rect({
      x,
      y: y + 4,
      width: 46,
      height: 24,
      fill: '#22c55e',
      cornerRadius: 12,
      editable: false,
      data: { itemId: item.id },
    }))
    group.add(new api.Ellipse({
      x: x + 24,
      y: y + 7,
      width: 18,
      height: 18,
      fill: '#ffffff',
      editable: false,
      data: { itemId: item.id },
    }))
    return
  }

  group.add(new api.Rect({
    x,
    y,
    width,
    height: controlHeight,
    fill: '#ffffff',
    stroke: '#dbe3ee',
    cornerRadius: 5,
    editable: false,
    data: { itemId: item.id },
  }))
  addText(api, group, resolveControlText(item, field, isUpload), {
    x: x + 10,
    y: y + 9,
    width: width - 20,
    fill: isUpload ? '#1d4ed8' : '#94a3b8',
  })
  if (['field-select', 'field-dict-select', 'field-tree-select', 'field-org-tree-select', 'field-user-select', 'field-region-tree-select', 'field-cascader'].includes(componentKey)
    || field.dictType || ['select', 'radio', 'checkbox', 'dictSelect', 'treeSelect', 'orgTreeSelect', 'userSelect', 'regionTreeSelect', 'cascader'].includes(componentType)) {
    addText(api, group, 'v', {
      x: x + width - 24,
      y: y + 8,
      width: 14,
      fill: '#94a3b8',
      textAlign: 'center',
      fontWeight: '600',
    })
  }
}

function addItemHeader(api, group, item, width) {
  const field = resolveItemField(item)
  const badgeWidth = addItemModelBadge(api, group, item, field, width, 12)
  addText(api, group, item.label || resolveComponentTitle(item.componentKey), {
    x: 14,
    y: 13,
    width: Math.max(40, width - 110 - badgeWidth),
    fill: '#0f172a',
    fontWeight: '700',
  })
  addText(api, group, resolveComponentTitle(item.componentKey), {
    x: Math.max(14, width - 98),
    y: 14,
    width: 84,
    fill: '#94a3b8',
    textAlign: 'right',
    fontSize: 10,
  })
}

function addItemModelBadge(api, group, item, field, width, y) {
  if (!item?.fieldRef)
    return 0
  const label = resolveItemModelLabel(item, field)
  if (!label)
    return 0
  const palette = resolveModelBadgePalette(field)
  const badgeWidth = Math.min(88, Math.max(48, label.length * 12 + 14))
  const x = Math.max(112, width - 106 - badgeWidth)
  group.add(new api.Rect({
    x,
    y,
    width: badgeWidth,
    height: 18,
    fill: palette.fill,
    cornerRadius: 9,
    editable: false,
    data: { itemId: item.id },
  }))
  addText(api, group, label, {
    x: x + 6,
    y: y + 3,
    width: badgeWidth - 12,
    fill: palette.text,
    fontSize: 10,
    textAlign: 'center',
    fontWeight: '600',
    maxChars: 10,
  })
  return badgeWidth + 10
}

function resolveItemModelLabel(item, field = {}) {
  const modelName = item?.modelName || field?.modelName || field?.sourceLabel || ''
  if (!item?.fieldRef || !modelName)
    return ''
  return isPrimaryItemField(field) ? '主模型' : modelName
}

function resolveModelBadgePalette(field = {}) {
  if (isPrimaryItemField(field)) {
    return {
      fill: '#dbeafe',
      text: '#1d4ed8',
    }
  }
  return {
    fill: '#dcfce7',
    text: '#047857',
  }
}

function isPrimaryItemField(field = {}) {
  const sourceField = field?.sourceField || field?.field
  return !field?.modelCode || field?.field === sourceField
}

function addText(api, group, text, attrs = {}) {
  group.add(new api.Text({
    text: truncateText(text, attrs.maxChars || 24),
    x: attrs.x || 0,
    y: attrs.y || 0,
    width: attrs.width || 120,
    height: attrs.height || 16,
    fill: attrs.fill || '#334155',
    fontSize: attrs.fontSize || 12,
    fontWeight: attrs.fontWeight || '400',
    textAlign: attrs.textAlign || 'left',
    textOverflow: 'ellipsis',
    editable: false,
  }))
}

function handleEditorSelect(event) {
  const target = Array.isArray(event.value) ? event.value[0] : event.value
  const itemId = target?.data?.itemId || target?.id
  if (itemId)
    emit('selectItem', itemId)
}

function handleEditorTransform(event) {
  const target = event.target || leaferApp?.editor?.getItem?.()
  const itemId = target?.data?.itemId || target?.id
  const item = canvasItems.value.find(canvasItem => canvasItem.id === itemId)
  if (!target || !item)
    return
  liveLayouts.value = {
    ...liveLayouts.value,
    [item.id]: readShapeLayout(target, item),
  }
  if (transformTimer)
    window.clearTimeout(transformTimer)
  transformTimer = window.setTimeout(() => commitShapeLayout(target, item), 120)
}

function syncEditorSelection() {
  const app = leaferApp
  if (!app?.editor)
    return
  const shape = props.selectedItemId ? shapeMap.get(props.selectedItemId) : null
  if (shape)
    app.editor.select(shape)
  else
    app.editor.cancel?.()
}

function resizeLeafer() {
  leaferApp?.resize?.({
    width: canvas.value.width || 1040,
    height: canvas.value.height || 460,
  })
}

function readShapeLayout(shape, item) {
  const scaleX = Number(shape.scaleX || 1)
  const scaleY = Number(shape.scaleY || 1)
  return {
    x: Math.max(0, snapValue(shape.x || 0)),
    y: Math.max(0, snapValue(shape.y || 0)),
    w: Math.max(48, snapValue((shape.width || item.w || 120) * scaleX)),
    h: Math.max(32, snapValue((shape.height || item.h || 48) * scaleY)),
  }
}

function commitShapeLayout(shape, item) {
  const layout = readShapeLayout(shape, item)
  liveLayouts.value = {
    ...liveLayouts.value,
    [item.id]: layout,
  }
  updateCanvasItems(canvasItems.value.map(canvasItem => canvasItem.id === item.id
    ? { ...canvasItem, ...layout }
    : canvasItem))
  nextTick(() => {
    const nextLayouts = { ...liveLayouts.value }
    delete nextLayouts[item.id]
    liveLayouts.value = nextLayouts
  })
}

function handleDrop(event) {
  const raw = event.dataTransfer.getData('application/x-lowcode-component')
  if (!raw || !props.zone)
    return
  const payload = JSON.parse(raw)
  const rect = boardRef.value.getBoundingClientRect()
  const scrollEl = scrollRef.value
  const scrollLeft = scrollEl ? scrollEl.scrollLeft : 0
  const scrollTop = scrollEl ? scrollEl.scrollTop : 0
  const x = snapValue(event.clientX - rect.left + scrollLeft)
  const y = snapValue(event.clientY - rect.top + scrollTop)
  const item = createCanvasItem(payload, {
    zoneKey: props.zone.zoneKey,
    fields: props.fields,
    x: Math.max(0, x),
    y: Math.max(0, y),
    zIndex: resolveNextZIndex(),
  })
  updateCanvasItems([...canvasItems.value, item])
  emit('selectItem', item.id)
}

function updateCanvasSize(key, value) {
  if (!props.zone || !value)
    return
  emit('update:zone', patchZoneCanvas(props.zone, {
    ...canvas.value,
    [key]: Number(value),
  }))
}

function updateCanvasItems(items) {
  if (!props.zone)
    return
  emit('update:zone', patchZoneCanvas(props.zone, {
    ...canvas.value,
    items,
  }))
}

function handleEnabledChange(value) {
  if (!props.zone)
    return
  emit('update:zone', {
    ...props.zone,
    enabled: value,
  })
}

function autoArrange() {
  if (showLayoutModes.value) {
    applyColumnLayout(activeLayoutCols.value)
    return
  }
  const nextItems = canvasItems.value.map((item, index) => {
    if (isButtonComponent(item.componentKey)) {
      const buttonIndex = canvasItems.value.slice(0, index).filter(prev => isButtonComponent(prev.componentKey)).length
      return {
        ...item,
        x: 32 + buttonIndex * 116,
        y: 28,
        zIndex: index + 1,
      }
    }
    const fieldIndex = canvasItems.value.slice(0, index).filter(prev => !isButtonComponent(prev.componentKey)).length
    return {
      ...item,
      x: 32 + (fieldIndex % 3) * 308,
      y: 88 + Math.floor(fieldIndex / 3) * 86,
      zIndex: index + 1,
    }
  })
  updateCanvasItems(nextItems)
}

function applyColumnLayout(cols) {
  if (!props.zone)
    return
  const columnCount = Math.max(1, Math.min(3, Number(cols || 1)))
  const paddingX = 32
  const gapX = 18
  const startY = 32
  const rowGap = props.zone.zoneKey === 'detail' ? 10 : 12
  const availableWidth = Number(canvas.value.width || 1040) - paddingX * 2 - gapX * (columnCount - 1)
  const columnWidth = Math.max(220, Math.min(340, Math.floor(availableWidth / columnCount)))
  const fieldItems = sortCanvasItemsByPosition(canvasItems.value.filter(item => item.fieldRef))
  const fieldOrderMap = new Map(fieldItems.map((item, index) => [item.id, index]))
  const fieldHeights = fieldItems
    .map(item => Number(item.h || (item.componentKey === 'field-textarea' ? 98 : item.componentKey === 'field-upload' || item.componentKey === 'field-image-upload' ? 88 : 64)))
  const rowHeights = []
  fieldHeights.forEach((height, index) => {
    const row = Math.floor(index / columnCount)
    rowHeights[row] = Math.max(rowHeights[row] || 0, height)
  })
  const rowOffsets = rowHeights.reduce((offsets, height, index) => {
    offsets[index] = index === 0 ? startY : offsets[index - 1] + rowHeights[index - 1] + rowGap
    return offsets
  }, [])
  let maxBottom = startY
  const nextItems = canvasItems.value.map((item, index) => {
    if (!item.fieldRef)
      return { ...item, zIndex: index + 1 }
    const fieldIndex = fieldOrderMap.get(item.id) ?? index
    const row = Math.floor(fieldIndex / columnCount)
    const col = fieldIndex % columnCount
    const height = Number(item.h || (item.componentKey === 'field-textarea' ? 98 : item.componentKey === 'field-upload' || item.componentKey === 'field-image-upload' ? 88 : 64))
    const y = rowOffsets[row] || startY
    maxBottom = Math.max(maxBottom, y + height)
    return {
      ...item,
      x: paddingX + col * (columnWidth + gapX),
      y,
      w: columnWidth,
      h: height,
      zIndex: index + 1,
    }
  })
  emit('update:zone', patchZoneCanvas(props.zone, {
    ...canvas.value,
    height: Math.max(420, maxBottom + 48),
    items: nextItems,
  }))
}

function handleGlobalKeydown(event) {
  if (!['Delete', 'Backspace'].includes(event.key) || !props.selectedItemId)
    return
  if (isTypingTarget(event.target))
    return
  const selectedItem = canvasItems.value.find(item => item.id === props.selectedItemId)
  if (!selectedItem)
    return
  event.preventDefault()
  updateCanvasItems(canvasItems.value.filter(item => item.id !== props.selectedItemId))
  emit('selectItem', '')
}

function resolveNextZIndex() {
  return Math.max(0, ...canvasItems.value.map(item => Number(item.zIndex || 0))) + 1
}

function resolveComponentTitle(componentKey) {
  return resolveCanvasComponent(componentKey)?.title || componentKey
}

function resolveItemField(item) {
  return fieldMap.value.get(item.fieldRef) || {
    field: item.fieldRef || item.id,
    label: item.label,
    componentType: resolveComponentTypeFromComponentKey(item.componentKey, 'input'),
  }
}

function resolveItemFields(item) {
  const refs = item.fieldRefs?.length ? item.fieldRefs : item.props?.fieldRefs || []
  if (refs.length)
    return refs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
  if (item.fieldRef && fieldMap.value.get(item.fieldRef))
    return [fieldMap.value.get(item.fieldRef)]
  return []
}

function isButtonComponent(componentKey) {
  return ['import-button', 'export-button', 'custom-query', 'add-button', 'save-button', 'reset-button'].includes(componentKey)
}

function resolveCanvasColumnCount(items = []) {
  const columns = []
  ;(items || [])
    .filter(item => item.fieldRef)
    .sort((a, b) => Number(a.x || 0) - Number(b.x || 0))
    .forEach((item) => {
      const x = Number(item.x || 0)
      if (!columns.some(colX => Math.abs(colX - x) < 80))
        columns.push(x)
    })
  return columns.length || 1
}

function sortCanvasItemsByPosition(items = []) {
  return [...items].sort((a, b) => {
    const rowA = Math.round(Number(a.y || 0) / 16)
    const rowB = Math.round(Number(b.y || 0) / 16)
    if (rowA !== rowB)
      return rowA - rowB
    const xDiff = Number(a.x || 0) - Number(b.x || 0)
    if (xDiff !== 0)
      return xDiff
    return Number(a.zIndex || 0) - Number(b.zIndex || 0)
  })
}

function isTypingTarget(target) {
  const tagName = target?.tagName?.toLowerCase()
  return ['input', 'textarea', 'select'].includes(tagName)
    || target?.isContentEditable
    || Boolean(target?.closest?.('[contenteditable="true"], .n-input, .n-input-number'))
}

async function loadDictOptions(types) {
  const nextMap = { ...dictOptionsMap.value }
  for (const type of types || []) {
    if (!nextMap[type])
      nextMap[type] = await getDictData(type)
  }
  dictOptionsMap.value = nextMap
}

function resolveOptions(field) {
  if (field?.dictType && dictOptionsMap.value[field.dictType]?.length)
    return dictOptionsMap.value[field.dictType]
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function resolveSampleValue(field) {
  if (!field)
    return '示例值'
  if (field.dictType)
    return resolveOptions(field)[0]?.label || '字典值'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return '是'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? '128.00' : '128'
  if (field.componentType === 'date' || field.dataType === 'date')
    return '2026-05-20'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return '2026-05-20 09:30:00'
  return field.label ? `${field.label}示例` : '示例数据'
}

function snapValue(value) {
  const snap = Number(canvas.value.snap || 8)
  return Math.round(Number(value || 0) / snap) * snap
}

function resolveControlText(item, field, isUpload) {
  if (isUpload)
    return field.componentType === 'imageUpload' ? '选择图片' : '选择文件'
  if (item.componentKey === 'field-date' || field.componentType === 'date')
    return '选择日期'
  if (item.componentKey === 'field-datetime' || field.componentType === 'datetime')
    return '选择日期时间'
  if (item.componentKey === 'field-number' || field.componentType === 'number')
    return '数字输入'
  if (item.componentKey === 'field-select' || item.componentKey === 'field-dict-select' || field.dictType)
    return item.props?.placeholder || `请选择${item.label || field.label || '选项'}`
  return item.props?.placeholder || `请输入${item.label || field.label || '内容'}`
}

function truncateText(value, maxChars = 24) {
  const text = String(value || '')
  if (text.length <= maxChars)
    return text
  return `${text.slice(0, maxChars - 3)}...`
}
</script>

<style scoped>
.canvas-designer {
  height: 640px;
  min-height: 640px;
  display: grid;
  grid-template-rows: 92px minmax(0, 1fr);
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}

.canvas-toolbar {
  display: grid;
  grid-template-rows: 40px 42px;
  gap: 4px;
  padding: 6px 14px 8px;
  background: #ffffff;
  border-bottom: 1px solid #dbe3ee;
}

.canvas-toolbar-head,
.canvas-toolbar-actions,
.toolbar-group,
.enable-switch {
  display: flex;
  align-items: center;
}

.canvas-toolbar-head {
  justify-content: space-between;
  gap: 16px;
}

.canvas-toolbar-actions {
  justify-content: space-between;
  gap: 10px;
  overflow-x: auto;
  scrollbar-width: none;
}

.canvas-toolbar-actions::-webkit-scrollbar {
  display: none;
}

.toolbar-group {
  flex: 0 0 auto;
  gap: 8px;
  min-width: 0;
}

.toolbar-label,
.enable-switch span {
  flex: 0 0 auto;
  font-size: 12px;
  color: #64748b;
}

.enable-switch {
  gap: 8px;
}

.canvas-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.canvas-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: #64748b;
}

.size-input {
  width: 96px;
  min-width: 96px;
}

.size-input :deep(.n-input__input-el) {
  text-align: center;
}

.size-divider {
  color: #94a3b8;
}

.layout-mode-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
}

.canvas-scroll {
  min-height: 0;
  overflow: auto;
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.8), rgba(241, 245, 249, 0.95)), #f1f5f9;
}

.canvas-board {
  position: relative;
  margin: 0 auto;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.canvas-grid,
.leafer-layer {
  position: absolute;
  inset: 0;
}

.canvas-grid {
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.18) 1px, transparent 1px);
  background-size: 16px 16px;
  pointer-events: none;
}

.leafer-layer {
  z-index: 1;
}
</style>
