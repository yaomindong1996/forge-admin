<template>
  <n-modal
    v-model:show="visible"
    :title="`代码预览 - ${tableName}`"
    preset="card"
    style="width: 1400px"
    :mask-closable="false"
  >
    <div class="code-preview-modal">
      <n-spin :show="loading">
        <div class="preview-container">
          <!-- 左侧文件列表 -->
          <div class="file-list">
            <n-menu
              :options="fileOptions"
              :value="selectedFile"
              @update:value="handleFileSelect"
            />
          </div>

          <!-- 右侧代码展示 -->
          <div class="code-viewer">
            <div class="code-header">
              <span class="file-name">{{ selectedFile }}</span>
              <n-button size="small" @click="handleCopy">
                <template #icon>
                  <i class="i-material-symbols:content-copy" />
                </template>
                复制
              </n-button>
            </div>
            <div ref="editorContainer" class="editor-container"></div>
          </div>
        </div>
      </n-spin>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">关闭</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { ref, watch, onUnmounted, nextTick, computed } from 'vue'
import { request } from '@/utils'
import { EditorView, basicSetup } from 'codemirror'
import { java } from '@codemirror/lang-java'
import { xml } from '@codemirror/lang-xml'
import { javascript } from '@codemirror/lang-javascript'
import { sql } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  tableName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:show'])

const visible = ref(false)
const loading = ref(false)
const selectedFile = ref('')
const fileMap = ref({})
const editorContainer = ref(null)
let editorView = null

// 文件选项
const fileOptions = computed(() => {
  return Object.keys(fileMap.value).map(key => ({
    label: getFileName(key),
    key: key
  }))
})

// 监听 show 变化
watch(() => props.show, (val) => {
  visible.value = val
  if (val && props.tableName) {
    loadPreview()
  }
})

watch(visible, (val) => {
  emit('update:show', val)
  if (!val) {
    destroyEditor()
  }
})

// 加载预览代码
async function loadPreview() {
  try {
    loading.value = true
    const res = await request.get(`/generator/preview/${props.tableName}`)
    if (res.code === 200) {
      fileMap.value = res.data || {}
      // 自动选中第一个文件
      const keys = Object.keys(fileMap.value)
      if (keys.length > 0) {
        selectedFile.value = keys[0]
        await nextTick()
        initEditor(fileMap.value[keys[0]], keys[0])
      }
    }
  } catch (error) {
    console.error('加载预览失败:', error)
    window.$message.error('加载预览失败')
  } finally {
    loading.value = false
  }
}

// 获取文件名（从路径中提取）
function getFileName(path) {
  const parts = path.split('/')
  return parts[parts.length - 1]
}

// 获取语言支持
function getLanguageSupport(filename) {
  if (filename.endsWith('.java')) return java()
  if (filename.endsWith('.xml')) return xml()
  if (filename.endsWith('.vue') || filename.endsWith('.js') || filename.endsWith('.ts')) return javascript()
  if (filename.endsWith('.sql')) return sql()
  return []
}

// 初始化编辑器
function initEditor(code, filename) {
  destroyEditor()

  if (!editorContainer.value) return

  const languageSupport = getLanguageSupport(filename)
  const extensions = [
    basicSetup,
    oneDark,
    EditorView.editable.of(false)
    // 移除 lineWrapping 以显示水平滚动条
  ]

  if (languageSupport) {
    extensions.splice(1, 0, languageSupport)
  }

  editorView = new EditorView({
    doc: code || '',
    extensions,
    parent: editorContainer.value
  })
}

// 销毁编辑器
function destroyEditor() {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
}

// 文件选择变化
async function handleFileSelect(key) {
  selectedFile.value = key
  await nextTick()
  initEditor(fileMap.value[key], key)
}

// 复制代码
function handleCopy() {
  const code = fileMap.value[selectedFile.value]
  if (code) {
    navigator.clipboard.writeText(code).then(() => {
      window.$message.success('复制成功')
    }).catch(() => {
      window.$message.error('复制失败')
    })
  }
}

// 关闭弹窗
function handleClose() {
  visible.value = false
}

// 组件卸载时销毁编辑器
onUnmounted(() => {
  destroyEditor()
})
</script>

<style scoped>
.code-preview-modal {
  height: 600px;
  overflow: hidden;
}

.preview-container {
  display: flex;
  height: 100%;
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.file-list {
  width: 280px;
  border-right: 1px solid var(--n-border-color);
  overflow-y: auto;
  background-color: var(--n-color-modal);
  flex-shrink: 0;
}

.file-list :deep(.n-menu) {
  padding: 8px;
}

.file-list :deep(.n-menu-item-content-header) {
  font-size: 13px;
}

.code-viewer {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #282c34;
  border-bottom: 1px solid #3e4451;
  flex-shrink: 0;
}

.file-name {
  font-size: 13px;
  color: #abb2bf;
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  min-height: 0;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
  width: 100%;
}

.editor-container :deep(.cm-scroller) {
  overflow: auto !important;
  height: 100%;
  max-height: 100%;
}

.editor-container :deep(.cm-content) {
  min-height: 100%;
}

/* 确保滚动条样式可见 */
.editor-container :deep(.cm-scroller)::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.editor-container :deep(.cm-scroller)::-webkit-scrollbar-track {
  background: #282c34;
}

.editor-container :deep(.cm-scroller)::-webkit-scrollbar-thumb {
  background: #4e5561;
  border-radius: 5px;
}

.editor-container :deep(.cm-scroller)::-webkit-scrollbar-thumb:hover {
  background: #5c6370;
}
</style>
