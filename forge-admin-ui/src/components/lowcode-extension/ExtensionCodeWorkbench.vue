<template>
  <div
    ref="workbenchRoot"
    class="extension-code-workbench"
    :class="{ 'is-expanded': expanded }"
  >
    <header class="workbench-context">
      <div class="context-main">
        <span class="language-badge">{{ modeTitle }}</span>
        <div>
          <strong>{{ hookGuide.label }}</strong>
          <p>{{ hookGuide.description }}</p>
        </div>
      </div>
      <div class="scope-summary">
        <span>增强范围</span>
        <strong>{{ scopeDescription }}</strong>
      </div>
    </header>

    <div class="workbench-body">
      <section class="editor-pane">
        <div class="editor-toolbar">
          <div>
            <strong>{{ mode === 'css' ? 'CSS 内容' : '脚本内容' }}</strong>
            <span>{{ mode === 'css' ? '保存时自动校验并添加页面作用域' : '在独立 Worker 沙箱中同步执行' }}</span>
          </div>
          <div class="editor-actions">
            <span>{{ characterCount }} 字符</span>
            <n-popconfirm
              :to="false"
              positive-text="清空"
              negative-text="取消"
              @positive-click="clearEditor"
            >
              <template #trigger>
                <n-button size="tiny" quaternary :disabled="!modelValue">
                  清空
                </n-button>
              </template>
              确定清空当前编辑器内容吗？
            </n-popconfirm>
            <n-button
              size="tiny"
              quaternary
              :title="expanded ? '退出全屏' : '全屏编辑'"
              @click="toggleExpanded"
            >
              {{ expanded ? '退出全屏' : '全屏' }}
            </n-button>
          </div>
        </div>
        <div class="editor-shell">
          <div ref="editorHost" class="code-editor-host" />
          <span v-if="!modelValue" class="editor-placeholder">
            从右侧选择一个示例，或在这里开始编写
          </span>
        </div>
      </section>

      <aside class="guide-pane">
        <n-tabs v-model:value="activeTab" type="line" size="small" animated>
          <n-tab-pane name="examples" tab="可用示例">
            <div v-if="examples.length" class="guide-list example-list">
              <article v-for="example in examples" :key="example.id" class="example-card">
                <div>
                  <strong>{{ example.title }}</strong>
                  <p>{{ example.description }}</p>
                </div>
                <n-button
                  v-if="!modelValue"
                  size="tiny"
                  secondary
                  @click="applyExample(example)"
                >
                  使用示例
                </n-button>
                <n-popconfirm
                  v-else
                  :to="false"
                  positive-text="替换内容"
                  negative-text="取消"
                  @positive-click="applyExample(example)"
                >
                  <template #trigger>
                    <n-button size="tiny" secondary>
                      使用示例
                    </n-button>
                  </template>
                  将用此示例替换当前编辑器内容，是否继续？
                </n-popconfirm>
              </article>
            </div>
            <n-empty v-else size="small" description="当前触发点暂无适用示例" />
          </n-tab-pane>

          <n-tab-pane name="capabilities" :tab="mode === 'css' ? '可增强区域' : '可用 API'">
            <div class="guide-list">
              <button
                v-for="capability in capabilities"
                :key="capability.code"
                type="button"
                class="capability-item"
                @click="insertSnippet(capability.snippet)"
              >
                <code>{{ capability.code }}</code>
                <strong>{{ capability.title }}</strong>
                <span>{{ capability.description }}</span>
                <small>点击插入编辑器</small>
              </button>
            </div>
          </n-tab-pane>

          <n-tab-pane name="boundaries" tab="能力边界">
            <div class="boundary-list">
              <div
                v-for="item in boundaries"
                :key="item.text"
                class="boundary-item"
                :class="{ forbidden: !item.allowed }"
              >
                <span>{{ item.allowed ? '可用' : '禁止' }}</span>
                <p>{{ item.text }}</p>
              </div>
            </div>
          </n-tab-pane>
        </n-tabs>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { basicSetup, EditorView } from 'codemirror'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  CSS_BOUNDARIES,
  CSS_CAPABILITIES,
  getExtensionCodeExamples,
  getExtensionHookGuide,
  JS_BOUNDARIES,
  JS_CAPABILITIES,
} from './extension-code-catalog'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  mode: {
    type: String,
    default: 'javascript',
    validator: value => ['javascript', 'css'].includes(value),
  },
  hookCode: {
    type: String,
    default: '',
  },
  applicationCode: {
    type: String,
    default: '',
  },
  pageCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'example-applied'])
const workbenchRoot = ref(null)
const editorHost = ref(null)
const activeTab = ref('examples')
const expanded = ref(false)
let editorView = null

const modeTitle = computed(() => props.mode === 'css' ? '作用域 CSS' : '页面 JS 沙箱')
const hookGuide = computed(() => getExtensionHookGuide(props.hookCode))
const examples = computed(() => getExtensionCodeExamples(props.mode, props.hookCode))
const capabilities = computed(() => props.mode === 'css' ? CSS_CAPABILITIES : JS_CAPABILITIES)
const boundaries = computed(() => props.mode === 'css' ? CSS_BOUNDARIES : JS_BOUNDARIES)
const characterCount = computed(() => String(props.modelValue || '').length)
const scopeDescription = computed(() => {
  const application = props.applicationCode || '当前应用'
  const page = props.pageCode || '当前页面'
  if (props.mode === 'javascript')
    return [application, page, '白名单字段与授权动作'].join(' / ')
  return [application, page, '页面根节点内'].join(' / ')
})

const editorTheme = EditorView.theme({
  '&': {
    height: '100%',
    color: '#e5e7eb',
    backgroundColor: '#1f2329',
    fontSize: '13px',
  },
  '.cm-scroller': {
    fontFamily: 'SFMono-Regular, Menlo, Monaco, Consolas, Liberation Mono, monospace',
    lineHeight: '1.65',
  },
  '.cm-content': {
    minHeight: '400px',
    padding: '12px 0',
    caretColor: '#dbeafe',
  },
  '.cm-gutters': {
    color: '#7f8794',
    backgroundColor: '#191c21',
    borderRight: '1px solid #303640',
  },
  '.cm-activeLine': {
    backgroundColor: 'rgba(90, 106, 126, 0.16)',
  },
  '.cm-activeLineGutter': {
    color: '#d8dee9',
    backgroundColor: 'rgba(90, 106, 126, 0.22)',
  },
  '&.cm-focused': {
    outline: 'none',
  },
}, { dark: true })

onMounted(async () => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  await nextTick()
  initEditor()
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  destroyEditor()
})

watch(() => props.modelValue, (value) => {
  setEditorValue(value || '')
})

watch(() => props.mode, async () => {
  await nextTick()
  initEditor()
})

watch(expanded, async () => {
  await nextTick()
  editorView?.requestMeasure()
  editorView?.focus()
})

function initEditor() {
  destroyEditor()
  if (!editorHost.value)
    return

  const extensions = [
    basicSetup,
    oneDark,
    editorTheme,
    EditorView.lineWrapping,
    EditorView.updateListener.of((update) => {
      if (update.docChanged)
        emit('update:modelValue', update.state.doc.toString())
    }),
  ]
  if (props.mode === 'javascript')
    extensions.splice(1, 0, javascript())

  editorView = new EditorView({
    doc: props.modelValue || '',
    extensions,
    parent: editorHost.value,
  })
}

function destroyEditor() {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
}

function setEditorValue(value) {
  if (!editorView)
    return
  const current = editorView.state.doc.toString()
  if (current === value)
    return
  editorView.dispatch({
    changes: { from: 0, to: current.length, insert: value },
  })
}

function replaceEditorContent(value) {
  const content = String(value || '')
  setEditorValue(content)
  emit('update:modelValue', content)
  nextTick(() => editorView?.focus())
}

function applyExample(example) {
  replaceEditorContent(example.code)
  emit('example-applied', example)
}

function clearEditor() {
  replaceEditorContent('')
}

function insertSnippet(snippet) {
  if (!editorView)
    return
  const selection = editorView.state.selection.main
  const prefix = selection.from > 0 ? '\n' : ''
  const content = prefix + String(snippet || '')
  editorView.dispatch({
    changes: {
      from: selection.from,
      to: selection.to,
      insert: content,
    },
    selection: {
      anchor: selection.from + content.length,
    },
  })
  editorView.focus()
}

function handleFullscreenChange() {
  expanded.value = document.fullscreenElement === workbenchRoot.value
}

async function toggleExpanded() {
  const root = workbenchRoot.value
  if (!root?.requestFullscreen) {
    expanded.value = !expanded.value
    return
  }
  try {
    if (document.fullscreenElement === root)
      await document.exitFullscreen()
    else
      await root.requestFullscreen()
  }
  catch {
    expanded.value = !expanded.value
  }
}
</script>

<style scoped>
.extension-code-workbench {
  overflow: hidden;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
  background: var(--bg-primary, #fff);
}

.extension-code-workbench.is-expanded {
  position: fixed;
  z-index: 5000;
  inset: 18px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 18px 60px rgba(15, 23, 42, 0.28);
}

.workbench-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 64px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-secondary, #f7f8fa);
}

.context-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.language-badge {
  flex: 0 0 auto;
  padding: 4px 8px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 4px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  font-size: 11px;
  font-weight: 700;
}

.context-main strong,
.scope-summary strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.context-main p {
  overflow: hidden;
  margin: 2px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-summary {
  display: flex;
  min-width: 210px;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.scope-summary span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.scope-summary strong {
  overflow: hidden;
  max-width: 320px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workbench-body {
  display: grid;
  min-height: 440px;
  grid-template-columns: minmax(0, 1fr) 286px;
}

.is-expanded .workbench-body {
  min-height: 0;
  flex: 1;
}

.editor-pane {
  min-width: 0;
  background: #1f2329;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 0 10px 0 13px;
  border-bottom: 1px solid #303640;
  color: #d8dee9;
  background: #242930;
}

.editor-toolbar > div:first-child,
.editor-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.editor-toolbar strong {
  font-size: 12px;
}

.editor-toolbar span {
  color: #9ca6b5;
  font-size: 11px;
}

.editor-actions :deep(.n-button) {
  color: #c4ccd7;
}

.editor-shell {
  position: relative;
}

.code-editor-host {
  height: 398px;
}

.is-expanded .code-editor-host {
  height: calc(100vh - 143px);
}

.editor-placeholder {
  position: absolute;
  z-index: 1;
  top: 15px;
  left: 48px;
  color: #6f7886;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  pointer-events: none;
}

.guide-pane {
  min-width: 0;
  padding: 0 12px 12px;
  border-left: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-primary, #fff);
}

.guide-pane :deep(.n-tabs-nav) {
  margin-bottom: 8px;
}

.guide-pane :deep(.n-tab-pane) {
  max-height: 378px;
  overflow-y: auto;
}

.is-expanded .guide-pane :deep(.n-tab-pane) {
  max-height: calc(100vh - 135px);
}

.guide-list,
.boundary-list {
  display: grid;
  gap: 8px;
}

.example-card {
  display: grid;
  gap: 9px;
  padding: 10px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-secondary, #f7f8fa);
}

.example-card strong,
.capability-item strong {
  display: block;
  color: var(--text-primary, #1d2129);
  font-size: 12px;
}

.example-card p {
  margin: 4px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  line-height: 1.5;
}

.example-card :deep(.n-button) {
  justify-self: start;
}

.capability-item {
  display: grid;
  width: 100%;
  gap: 4px;
  padding: 9px 10px;
  cursor: pointer;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  text-align: left;
}

.capability-item:hover {
  border-color: var(--primary-color, #165dff);
  background: var(--bg-secondary, #f7f8fa);
}

.capability-item code {
  overflow: hidden;
  color: var(--primary-color, #165dff);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.capability-item span,
.capability-item small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  line-height: 1.45;
}

.capability-item small {
  margin-top: 2px;
}

.boundary-item {
  display: grid;
  grid-template-columns: 38px 1fr;
  gap: 8px;
  align-items: start;
  padding: 9px 0;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.boundary-item > span {
  padding: 2px 5px;
  border-radius: 3px;
  color: var(--success-700, #16895a);
  background: rgba(30, 174, 117, 0.11);
  font-size: 10px;
  text-align: center;
}

.boundary-item.forbidden > span {
  color: var(--error-600, #c54747);
  background: rgba(239, 82, 82, 0.09);
}

.boundary-item p {
  margin: 0;
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
  line-height: 1.55;
}

@media (max-width: 900px) {
  .workbench-context,
  .context-main {
    align-items: flex-start;
  }

  .workbench-context {
    flex-direction: column;
  }

  .scope-summary {
    min-width: 0;
    align-items: flex-start;
  }

  .workbench-body {
    grid-template-columns: 1fr;
  }

  .guide-pane {
    border-top: 1px solid var(--border-light, #e5e6eb);
    border-left: 0;
  }

  .guide-pane :deep(.n-tab-pane) {
    max-height: 300px;
  }

  .context-main p {
    white-space: normal;
  }
}
</style>
