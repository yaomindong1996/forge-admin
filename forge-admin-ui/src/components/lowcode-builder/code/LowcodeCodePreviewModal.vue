<template>
  <n-modal
    v-model:show="visible"
    :title="`代码预览 - ${titleText}`"
    preset="card"
    style="width: min(1320px, calc(100vw - 32px))"
    :mask-closable="false"
  >
    <div class="code-preview">
      <div class="preview-toolbar">
        <n-select
          v-model:value="sourceType"
          size="small"
          :options="sourceOptions"
          class="source-select"
          @update:value="loadPreview"
        />
        <n-button size="small" :loading="loading" @click="loadPreview">
          刷新
        </n-button>
      </div>
      <n-spin :show="loading">
        <div class="preview-layout">
          <aside class="file-list">
            <div class="file-count">
              {{ filePaths.length }} 个文件
            </div>
            <n-tree
              v-if="fileTreeData.length"
              block-line
              :data="fileTreeData"
              :selected-keys="selectedTreeKeys"
              :expanded-keys="expandedTreeKeys"
              :render-prefix="renderTreePrefix"
              class="file-tree"
              @update:selected-keys="handleTreeSelect"
              @update:expanded-keys="handleTreeExpand"
            />
            <n-empty v-else size="small" description="暂无文件" />
          </aside>
          <main class="code-panel">
            <div class="code-head">
              <span>{{ selectedFile || '请选择文件' }}</span>
              <n-button size="small" :disabled="!selectedFile" @click="copyCode">
                复制
              </n-button>
            </div>
            <pre v-if="selectedFile" class="code-body">{{ selectedCode }}</pre>
            <n-empty v-else description="暂无可预览文件" />
          </main>
        </div>
      </n-spin>
    </div>
    <template #footer>
      <n-space justify="end">
        <n-button @click="visible = false">
          关闭
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { DocumentTextOutline, FolderOpenOutline } from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import { computed, h, ref, watch } from 'vue'
import { lowcodeAppCodePreview } from '@/api/lowcode-crud'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  appId: {
    type: [Number, String],
    default: null,
  },
  appName: {
    type: String,
    default: '',
  },
  defaultSourceType: {
    type: String,
    default: 'DRAFT',
  },
  codegenOptions: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:show'])

const visible = ref(false)
const loading = ref(false)
const files = ref({})
const selectedFile = ref('')
const sourceType = ref('DRAFT')
const expandedTreeKeys = ref([])

const sourceOptions = [
  { label: '已保存草稿', value: 'DRAFT' },
  { label: '已发布版本', value: 'PUBLISHED' },
]

const titleText = computed(() => props.appName || props.appId || '-')
const filePaths = computed(() => Object.keys(files.value).sort((a, b) => a.localeCompare(b)))
const fileTreeData = computed(() => buildFileTree(filePaths.value))
const selectedTreeKeys = computed(() => selectedFile.value ? [selectedFile.value] : [])
const selectedCode = computed(() => files.value[selectedFile.value] || '')

watch(() => props.show, async (value) => {
  visible.value = value
  if (value) {
    sourceType.value = props.defaultSourceType || 'DRAFT'
    await loadPreview()
  }
})

watch(visible, (value) => {
  emit('update:show', value)
})

watch(fileTreeData, (data) => {
  expandedTreeKeys.value = collectDirectoryKeys(data)
})

async function loadPreview() {
  if (!props.appId)
    return
  loading.value = true
  try {
    const res = await lowcodeAppCodePreview(props.appId, {
      ...compactCodegenOptions(props.codegenOptions),
      sourceType: sourceType.value,
    })
    files.value = res.data?.files || {}
    selectedFile.value = filePaths.value[0] || ''
  }
  catch (e) {
    files.value = {}
    selectedFile.value = ''
    window.$message?.error(e?.message || '代码预览加载失败')
  }
  finally {
    loading.value = false
  }
}

function handleTreeSelect(keys) {
  const key = keys?.[0]
  if (key && Object.prototype.hasOwnProperty.call(files.value, key))
    selectedFile.value = key
}

function handleTreeExpand(keys) {
  expandedTreeKeys.value = keys
}

function renderTreePrefix({ option }) {
  const icon = option.type === 'file' ? DocumentTextOutline : FolderOpenOutline
  return h(
    NIcon,
    {
      size: 15,
      class: option.type === 'file' ? 'file-node-icon' : 'folder-node-icon',
    },
    { default: () => h(icon) },
  )
}

function buildFileTree(paths) {
  const roots = []
  for (const path of paths) {
    const parts = String(path || '').split('/').filter(Boolean)
    let level = roots
    const keyParts = []
    parts.forEach((part, index) => {
      keyParts.push(part)
      const isFile = index === parts.length - 1
      const key = isFile ? path : `dir:${keyParts.join('/')}`
      let node = level.find(item => item.key === key)
      if (!node) {
        node = {
          label: part,
          key,
          type: isFile ? 'file' : 'dir',
          children: isFile ? undefined : [],
        }
        level.push(node)
      }
      if (!isFile)
        level = node.children
    })
  }
  sortTree(roots)
  return roots
}

function sortTree(nodes) {
  nodes.sort((a, b) => {
    if (a.type !== b.type)
      return a.type === 'dir' ? -1 : 1
    return String(a.label).localeCompare(String(b.label))
  })
  nodes.forEach((node) => {
    if (node.children?.length)
      sortTree(node.children)
  })
}

function collectDirectoryKeys(nodes) {
  const keys = []
  for (const node of nodes || []) {
    if (node.type === 'dir')
      keys.push(node.key)
    if (node.children?.length)
      keys.push(...collectDirectoryKeys(node.children))
  }
  return keys
}

function compactCodegenOptions(options = {}) {
  const result = {}
  Object.entries(options || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '')
      result[key] = String(value).trim()
  })
  return result
}

async function copyCode() {
  if (!selectedFile.value)
    return
  await navigator.clipboard?.writeText(selectedCode.value)
  window.$message?.success('已复制')
}
</script>

<style scoped>
.code-preview {
  display: grid;
  gap: 12px;
}

.preview-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.source-select {
  width: 140px;
}

.preview-layout {
  display: grid;
  min-height: 560px;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.file-list {
  min-width: 0;
  overflow: auto;
  border-right: 1px solid var(--border-light);
  background: var(--bg-secondary);
  padding: 10px;
}

.file-count {
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}

.file-tree {
  min-width: 0;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.file-tree :deep(.n-tree-node-content) {
  min-width: 0;
  border-radius: 6px;
}

.file-tree :deep(.n-tree-node-content__text) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-tree :deep(.n-tree-node--selected .n-tree-node-content) {
  background: #dbeafe;
  color: #1d4ed8;
}

.folder-node-icon {
  color: #2563eb;
}

.file-node-icon {
  color: var(--text-secondary);
}

.code-panel {
  display: grid;
  min-width: 0;
  grid-template-rows: auto minmax(0, 1fr);
}

.code-head {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid var(--border-light);
  padding: 10px 12px;
}

.code-head span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-body {
  min-width: 0;
  overflow: auto;
  margin: 0;
  background: #0f172a;
  color: #e2e8f0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  padding: 14px;
}

@media (max-width: 760px) {
  .preview-layout {
    grid-template-columns: 1fr;
  }

  .file-list {
    max-height: 220px;
    border-right: 0;
    border-bottom: 1px solid var(--border-light);
  }
}
</style>
