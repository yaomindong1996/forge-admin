<template>
  <div
    ref="rootRef"
    class="inline-rich-text"
    :class="{ 'is-editing': editable, 'is-focused': focused }"
    @pointerdown.capture="handleEditorPointerDown"
    @keyup.capture="queueSelectionRefresh"
    @click.stop
  >
    <WangEditor
      :key="editorModeKey"
      class="inline-rich-text-editor"
      :model-value="modelValue"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
      @on-focus="handleFocus"
      @on-blur="handleBlur"
      @update:model-value="handleValueUpdate"
    />
  </div>

  <Teleport to="body">
    <div
      v-if="editorRef"
      v-show="editable && focused && showToolbar"
      class="inline-rich-text-floating-toolbar"
      :style="toolbarStyle"
      role="toolbar"
      aria-label="文本格式"
      @pointerdown.stop
    >
      <WangToolbar
        :editor="editorRef"
        :default-config="toolbarConfig"
        mode="default"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, shallowRef } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  editable: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '输入内容',
  },
})

const emit = defineEmits(['update:modelValue', 'activate', 'blur'])

let wangEditorModulePromise
const WangEditor = defineAsyncComponent(() => loadWangEditorModule().then(module => module.Editor))
const WangToolbar = defineAsyncComponent(() => loadWangEditorModule().then(module => module.Toolbar))

function loadWangEditorModule() {
  if (!wangEditorModulePromise) {
    wangEditorModulePromise = Promise.all([
      import('@wangeditor/editor/dist/css/style.css'),
      import('@wangeditor/editor-for-vue'),
    ]).then(([, module]) => module)
  }
  return wangEditorModulePromise
}

const editorRef = shallowRef(null)
const rootRef = shallowRef(null)
const focused = shallowRef(false)
const showToolbar = shallowRef(false)
const toolbarPosition = shallowRef({ top: -9999, left: -9999 })
let selectingWithPointer = false
let selectionRefreshFrame = 0
const editorModeKey = computed(() => props.editable ? 'editable' : 'readonly')
const toolbarStyle = computed(() => ({ top: `${toolbarPosition.value.top}px`, left: `${toolbarPosition.value.left}px` }))
const toolbarConfig = computed(() => ({
  excludeKeys: ['group-video', 'insertTable', 'codeBlock', 'fullScreen'],
}))
const editorConfig = computed(() => ({
  placeholder: props.placeholder,
  readOnly: !props.editable,
  autoFocus: false,
  scroll: false,
  MENU_CONF: {
    uploadImage: { server: '' },
  },
  // 统一由外部的选区浮动工具条承载格式操作。这里必须是空对象，
  // 不能保留 text 键；WangEditor 只要发现 text 键就仍会初始化内置 hoverbar。
  hoverbarKeys: {},
}))

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown, true)
  document.addEventListener('pointerup', handleDocumentPointerUp, true)
  window.addEventListener('resize', updateToolbarPosition)
  window.addEventListener('scroll', updateToolbarPosition, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown, true)
  document.removeEventListener('pointerup', handleDocumentPointerUp, true)
  window.removeEventListener('resize', updateToolbarPosition)
  window.removeEventListener('scroll', updateToolbarPosition, true)
  if (selectionRefreshFrame)
    cancelAnimationFrame(selectionRefreshFrame)
  editorRef.value?.destroy?.()
})

function handleCreated(editor) {
  editorRef.value = editor
}

function handleFocus() {
  if (!props.editable)
    return
  focused.value = true
  emit('activate')
}

function handleEditorPointerDown() {
  if (!props.editable)
    return
  // 不能沿用上一次选区：普通点击应只落下光标，不应在首次点入时误弹格式条。
  selectingWithPointer = true
  focused.value = true
  showToolbar.value = false
  emit('activate')
}

function handleBlur() {
  emit('blur')
}

function handleValueUpdate(value) {
  if (!props.editable)
    return
  emit('update:modelValue', value || '')
}

function handleDocumentPointerDown(event) {
  if (event.target?.closest?.('.inline-rich-text, .inline-rich-text-floating-toolbar'))
    return
  focused.value = false
  showToolbar.value = false
}

function handleDocumentPointerUp() {
  if (!selectingWithPointer)
    return
  selectingWithPointer = false
  queueSelectionRefresh()
}

function queueSelectionRefresh() {
  if (!props.editable)
    return
  if (selectionRefreshFrame)
    cancelAnimationFrame(selectionRefreshFrame)
  selectionRefreshFrame = requestAnimationFrame(() => {
    selectionRefreshFrame = 0
    refreshToolbarFromSelection()
  })
}

function refreshToolbarFromSelection() {
  const selection = window.getSelection()
  if (!selection?.rangeCount || selection.isCollapsed) {
    showToolbar.value = false
    return
  }
  const anchor = selection.anchorNode
  const focus = selection.focusNode
  const isEditorSelection = Boolean(anchor && focus && rootRef.value?.contains(anchor) && rootRef.value?.contains(focus))
  focused.value = isEditorSelection || focused.value
  showToolbar.value = isEditorSelection
  if (isEditorSelection)
    nextTick(updateToolbarPosition)
}

function updateToolbarPosition() {
  if (!showToolbar.value)
    return
  const selection = window.getSelection()
  if (!selection?.rangeCount)
    return
  const rect = selection.getRangeAt(0).getBoundingClientRect()
  if (!rect.width && !rect.height)
    return
  const toolbarWidth = 420
  toolbarPosition.value = {
    top: Math.round(rect.top > 52 ? rect.top - 44 : Math.min(window.innerHeight - 40, rect.bottom + 8)),
    left: Math.round(Math.max(8, Math.min(window.innerWidth - toolbarWidth - 8, rect.left))),
  }
}
</script>

<style scoped>
.inline-rich-text {
  display: flex;
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: transparent;
}

.inline-rich-text.is-editing {
  cursor: text;
}

.inline-rich-text.is-focused {
  overflow: visible;
}

.inline-rich-text-editor {
  min-width: 0;
  min-height: 0;
  flex: 1;
  overflow: visible;
}

.inline-rich-text-editor :deep(.w-e-text-container) {
  min-height: 100%;
  height: 100% !important;
  overflow: visible !important;
  border: 0 !important;
  background: transparent;
}

.inline-rich-text-editor :deep(.w-e-scroll) {
  min-height: 100%;
  overflow: visible !important;
}

.inline-rich-text-editor :deep(.w-e-text) {
  min-height: 100%;
  padding: 8px 20px 14px;
  color: #1f2329;
  cursor: text;
}

/*
 * 部分全局 WangEditor 配置会在实例配置之后合并 hoverbarKeys，
 * 造成第一次划选文本时仍创建一个内置短工具条。标题组件只允许使用
 * 下方 Teleport 的统一工具条，因此在组件作用域内彻底隐藏该内置层。
 */
/*
 * Hoverbar 由 WangEditor 动态挂到编辑器根容器外层，不能用 :deep 限定
 * 在当前组件子树内，否则首次 hover 时仍会漏出。应用运行页的富文本只使用
 * 我们的 Teleport 工具条，因此全局屏蔽 WangEditor 原生 hoverbar。
 */
:global(.w-e-hover-bar) {
  display: none !important;
}

.inline-rich-text-editor :deep(.w-e-text-placeholder) {
  left: 20px;
  top: 8px;
}

.inline-rich-text-editor :deep(h1) {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.3;
}

.inline-rich-text-editor :deep(p) {
  margin: 4px 0 0;
  color: rgba(100, 106, 115, 0.79);
  font-size: 16px;
  line-height: 1.5;
}

.inline-rich-text-editor :deep(ul),
.inline-rich-text-editor :deep(ol) {
  margin: 6px 0;
  padding-left: 22px;
}

:global(.inline-rich-text-floating-toolbar) {
  position: fixed;
  z-index: 11000;
  max-width: calc(100vw - 16px);
  overflow: visible;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 35, 41, 0.18);
}

:global(.inline-rich-text-floating-toolbar .w-e-toolbar) {
  border: 0 !important;
  border-radius: 8px;
  background: #fff;
}
</style>
