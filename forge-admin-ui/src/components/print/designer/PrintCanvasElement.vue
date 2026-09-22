<script setup>
import { LockClosedOutline } from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { encodePrintCode } from '../renderers/codes'
import { printRenderers } from '../renderers/registry'
import { printStyle } from '../renderers/style'
import { designerBindingText, designerBindingValue, fieldLabel } from './designerSample'
import PrintDataTableDesigner from './PrintDataTableDesigner.vue'
import PrintStaticTableDesigner from './PrintStaticTableDesigner.vue'

const props = defineProps({
  element: { type: Object, required: true },
  selected: Boolean,
  catalog: { type: Array, default: () => [] },
  context: { type: Object, default: () => ({}) },
  resolveFile: { type: Function, default: undefined },
  tableCellIds: { type: Array, default: () => [] },
  tableColumnId: { type: String, default: '' },
  tableColumnIds: { type: Array, default: () => [] },
  pageNumber: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
})
const emit = defineEmits(['tableCellSelect', 'tableCellSelectRange', 'tableCellChange', 'tableContextAction', 'move', 'selectColumn', 'selectColumns', 'changeField', 'tableResizeTrack', 'tableResizeImage', 'dataColumnResize'])
const store = usePrintDesignerStore()
const imageBlobCache = new Map()
const codeSrc = ref('')
const resolvedImageSrc = ref('')
const editing = ref(false)
const draft = ref('')
const editorRef = ref(null)

const canInlineEdit = computed(() => {
  if (props.element.locked || !['TEXT', 'HTML'].includes(props.element.type))
    return false
  const binding = props.element.binding
  return !binding || binding.source === 'CONSTANT'
})
const text = computed(() => {
  if (props.element.type === 'PAGE_NUMBER')
    return props.element.pageNumberFormat === 'CURRENT' ? `${props.pageNumber}` : `${props.pageNumber} / ${props.totalPages}`
  return designerBindingText(props.element.binding, props.element.format, props.catalog, props.context)
})
const bindingName = computed(() => props.element.binding?.source === 'FIELD' ? fieldLabel(props.catalog, props.element.binding.path) : '')
const rawImageValue = computed(() => {
  if (props.element.type !== 'IMAGE')
    return ''
  const value = designerBindingValue(props.element.binding, props.context)
  return typeof value === 'string' ? value : ''
})
const imageSrc = computed(() => {
  if (!rawImageValue.value)
    return ''
  if (rawImageValue.value.startsWith('data:image/') || /^https?:\/\//i.test(rawImageValue.value) || rawImageValue.value.startsWith('blob:'))
    return rawImageValue.value
  return resolvedImageSrc.value || imageBlobCache.get(rawImageValue.value) || ''
})
const node = computed(() => ({
  ...props.element,
  text: text.value,
  html: props.element.type === 'HTML' ? text.value : undefined,
  src: codeSrc.value || imageSrc.value,
  table: props.element.table
    ? {
        ...props.element.table,
        cells: props.element.table.cells.map((cell) => {
          if (cell.contentType === 'IMAGE') {
            return {
              ...cell,
              type: 'IMAGE',
              text: '',
              src: '', // designer uses PrintStaticTableDesigner; keep empty for fallback renderer
            }
          }
          return {
            ...cell,
            text: designerBindingText(cell.binding, cell.format, props.catalog, props.context),
          }
        }),
      }
    : undefined,
  descriptions: props.element.type === 'DESCRIPTIONS'
    ? {
        ...(props.element.descriptions || {}),
        items: (props.element.descriptions?.items || []).map(item => ({
          ...item,
          text: designerBindingText(item.binding, undefined, props.catalog, props.context),
        })),
      }
    : undefined,
}))
const renderer = computed(() => printRenderers[props.element.type])
const style = computed(() => ({
  left: `${props.element.xMm}mm`,
  top: `${props.element.yMm}mm`,
  width: `${props.element.widthMm}mm`,
  height: `${props.element.heightMm}mm`,
  opacity: props.element.style?.opacity ?? 1,
  transform: props.element.rotationDeg || props.element.flipX || props.element.flipY ? `rotate(${props.element.rotationDeg || 0}deg) scaleX(${props.element.flipX ? -1 : 1}) scaleY(${props.element.flipY ? -1 : 1})` : undefined,
  transformOrigin: 'center center',
}))

watch(() => [props.element.type, props.element.barcodeFormat, props.element.widthMm, props.element.heightMm, props.element.showCodeText, text.value], async (_, __, onCleanup) => {
  codeSrc.value = ''
  if (!['BARCODE', 'QRCODE'].includes(props.element.type) || !text.value)
    return
  const controller = new AbortController()
  onCleanup(() => controller.abort())
  try {
    codeSrc.value = await encodePrintCode({ ...props.element, text: text.value }, controller.signal)
  }
  catch {
    codeSrc.value = ''
  }
}, { immediate: true })

watch(() => [props.element.type, rawImageValue.value], async ([type, value], _, onCleanup) => {
  if (type !== 'IMAGE' || !value || value.startsWith('data:image/') || /^https?:\/\//i.test(value) || value.startsWith('blob:')) {
    resolvedImageSrc.value = ''
    return
  }
  if (!/^[\w-]{1,128}$/.test(value)) {
    resolvedImageSrc.value = ''
    return
  }
  const cached = imageBlobCache.get(value)
  if (cached) {
    resolvedImageSrc.value = cached
    return
  }
  if (!props.resolveFile) {
    resolvedImageSrc.value = ''
    return
  }
  const controller = new AbortController()
  let active = true
  onCleanup(() => {
    active = false
    controller.abort()
  })
  try {
    const blob = await props.resolveFile(value, { signal: controller.signal })
    if (!active || controller.signal.aborted || !(blob instanceof Blob))
      return
    const blobUrl = URL.createObjectURL(blob)
    const previous = imageBlobCache.get(value)
    if (previous && previous !== blobUrl)
      URL.revokeObjectURL(previous)
    imageBlobCache.set(value, blobUrl)
    resolvedImageSrc.value = blobUrl
  }
  catch {
    if (active && !controller.signal.aborted)
      resolvedImageSrc.value = ''
  }
}, { immediate: true })

async function startInlineEdit() {
  if (!canInlineEdit.value)
    return
  editing.value = true
  draft.value = props.element.binding?.value ?? text.value ?? ''
  await nextTick()
  editorRef.value?.focus?.()
  editorRef.value?.select?.()
}

function commitInlineEdit() {
  if (!editing.value)
    return
  const value = draft.value
  editing.value = false
  store.selectElement(props.element.id)
  store.patchSelected({
    binding: { source: 'CONSTANT', value },
  })
}

function cancelInlineEdit() {
  editing.value = false
}

watch(() => props.selected, (value) => {
  if (!value && editing.value)
    commitInlineEdit()
})
</script>

<template>
  <div
    :style="style"
    class="canvas-element"
    :class="[{ selected, 'locked': element.locked, editing, 'is-data-table': element.type === 'DATA_TABLE', 'is-static-table': element.type === 'STATIC_TABLE' }, element.type.toLowerCase()]"
    :data-element-id="element.id"
    role="button"
    :aria-label="`${element.type} ${text || bindingName}`"
    :title="bindingName ? `绑定字段：${bindingName}` : (canInlineEdit ? '双击编辑文字' : undefined)"
    tabindex="-1"
    @dblclick.stop="startInlineEdit"
  >
    <textarea
      v-if="editing"
      ref="editorRef"
      v-model="draft"
      class="inline-editor"
      :style="printStyle(element.style)"
      @pointerdown.stop
      @keydown.esc.stop.prevent="cancelInlineEdit"
      @blur="commitInlineEdit"
    />
    <PrintStaticTableDesigner
      v-else-if="element.type === 'STATIC_TABLE'"
      :node="element"
      :selected-ids="selected ? tableCellIds : []"
      :locked="element.locked || !selected"
      :insert-count="store.staticTableInsertCount"
      :catalog="catalog"
      :context="context"
      :resolve-file="resolveFile"
      class="element-renderer interactive"
      @update:insert-count="store.setStaticTableInsertCount($event)"
      @select="(id, additive) => emit('tableCellSelect', id, additive)"
      @select-range="(ids) => emit('tableCellSelectRange', ids)"
      @change="(id, value) => emit('tableCellChange', id, value)"
      @context-action="(payload) => emit('tableContextAction', payload)"
      @paste-image-file="(payload) => emit('tableContextAction', { key: 'image-paste-file', ...payload })"
      @resize-track="(payload) => emit('tableResizeTrack', payload)"
      @resize-image="(payload) => emit('tableResizeImage', payload)"
      @move="emit('move', $event)"
    />
    <PrintDataTableDesigner
      v-else-if="element.type === 'DATA_TABLE'"
      class="element-renderer interactive"
      :element="element"
      :catalog="catalog"
      :context="context"
      :selected="selected"
      :locked="element.locked"
      :active-column-id="tableColumnId"
      :active-column-ids="tableColumnIds"
      @move="emit('move', $event)"
      @select-column="(id, range, cells) => emit('selectColumn', id, range, cells)"
      @select-columns="(ids, range, cells) => emit('selectColumns', ids, range, cells)"
      @change-field="emit('changeField', $event)"
      @resize-column="(payload) => emit('dataColumnResize', payload)"
    />
    <component :is="renderer" v-else-if="renderer" :node="node" class="element-renderer" />
    <div v-if="element.type === 'IMAGE' && !imageSrc" class="resource-placeholder">
      <span class="placeholder-icon">▧</span>
      <span>{{ bindingName || '图片' }}</span>
    </div>
    <div v-if="['BARCODE', 'QRCODE'].includes(element.type) && !codeSrc" class="resource-placeholder code-placeholder">
      <span>{{ element.type === 'QRCODE' ? '二维码' : '条形码' }}</span>
      <small>{{ text }}</small>
    </div>
    <div v-if="element.type === 'QRCODE' && codeSrc && element.showCodeText" class="code-caption">
      {{ text }}
    </div>
    <span v-if="element.locked" class="lock-indicator" aria-label="元素已锁定" title="已锁定">
      <NIcon :component="LockClosedOutline" :size="10" />
    </span>
  </div>
</template>

<style scoped>
.canvas-element {
  position: absolute;
  box-sizing: border-box;
  overflow: visible;
  cursor: move;
  user-select: none;
  outline: 1px dashed color-mix(in srgb, var(--text-tertiary, #a8adb5) 55%, transparent);
  color: var(--text-primary, #111);
}
.canvas-element.is-data-table,
.canvas-element.is-static-table {
  cursor: default;
  overflow: visible;
}
.canvas-element:hover:not(.selected) {
  outline-color: color-mix(in srgb, var(--primary-color) 55%, transparent);
}
.canvas-element.selected {
  outline: none;
}
.canvas-element.editing {
  outline: 1px solid var(--primary-color, #356cde);
  cursor: text;
  z-index: 6;
}
.inline-editor {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  margin: 0;
  border: 0;
  resize: none;
  outline: none;
  background: #fff;
  pointer-events: auto;
}
.element-renderer {
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.element-renderer.interactive {
  pointer-events: auto;
}
.resource-placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 2px;
  color: var(--text-tertiary, #64748b);
  background: repeating-linear-gradient(
    45deg,
    var(--gray-100, #f8fafc),
    var(--gray-100, #f8fafc) 4px,
    var(--bg-primary, #fff) 4px,
    var(--bg-primary, #fff) 8px
  );
  font-size: 9pt;
  text-align: center;
  pointer-events: none;
}
.placeholder-icon {
  font-size: 14px;
  line-height: 1;
}
.code-placeholder small {
  display: block;
  max-width: 90%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 8px;
}
.lock-indicator {
  position: absolute;
  top: 1px;
  right: 1px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  height: 12px;
  border-radius: 2px;
  color: #fff;
  background: var(--text-primary, #334155);
  pointer-events: none;
}
.code-caption {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 1px 2px;
  color: #111;
  background: rgb(255 255 255 / 92%);
  font-size: 8pt;
  line-height: 1.2;
  text-align: center;
  pointer-events: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
