<template>
  <n-modal
    v-model:show="visible"
    :title="panelTitle"
    preset="card"
    style="width: min(1500px, calc(100vw - 48px))"
    :mask-closable="false"
  >
    <div class="app-code-workbench">
      <section class="code-toolbar">
        <div class="toolbar-meta">
          <n-tag :bordered="false" type="info">
            {{ isApplicationScope ? '应用代码包' : '下载代码' }}
          </n-tag>
          <span>{{ app?.suiteName || app?.suiteCode || '业务域' }}</span>
          <template v-if="isApplicationScope">
            <span>·</span>
            <span>已选 {{ form.objectIds.length }} 个对象</span>
          </template>
          <template v-else>
            <span>/</span>
            <span>{{ app?.objectName || app?.objectCode || '业务单元' }}</span>
          </template>
        </div>
        <n-space>
          <n-button secondary :loading="saving" @click="saveOptions">
            <template #icon>
              <n-icon><SaveOutline /></n-icon>
            </template>
            保存设置
          </n-button>
          <n-button secondary :loading="previewing" @click="previewCode">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            刷新预览
          </n-button>
          <n-button
            type="primary"
            :loading="downloading"
            :disabled="!previewReady"
            @click="downloadCode"
          >
            <template #icon>
              <n-icon><DownloadOutline /></n-icon>
            </template>
            下载当前预览
          </n-button>
        </n-space>
      </section>

      <n-collapse
        :default-expanded-names="isApplicationScope ? [] : ['settings']"
        class="settings-collapse"
      >
        <n-collapse-item title="代码包设置" name="settings">
          <n-form label-placement="top" :model="form">
            <div class="settings-section-title">
              基础信息
            </div>
            <n-grid :cols="4" :x-gap="12" :y-gap="4" responsive="screen">
              <n-form-item-gi label="来源版本">
                <n-select v-model:value="form.sourceType" :options="sourceTypeOptions" />
              </n-form-item-gi>
              <n-form-item-gi v-if="!isApplicationScope && form.sourceType === 'VERSION'" label="版本 ID">
                <n-input-number v-model:value="form.versionId" :show-button="false" placeholder="输入版本 ID" />
              </n-form-item-gi>
              <n-form-item-gi
                v-if="!isApplicationScope"
                label="业务接口前缀"
                :span="form.sourceType === 'VERSION' ? 2 : 3"
              >
                <n-input v-model:value="form.businessApiBase" placeholder="/crm/customer" />
              </n-form-item-gi>
              <n-form-item-gi label="Java 基础包名">
                <n-input v-model:value="form.domainPackage" placeholder="com.mdframe.forge" />
              </n-form-item-gi>
              <n-form-item-gi label="模块名">
                <n-input v-model:value="form.moduleName" placeholder="crm" />
              </n-form-item-gi>
              <n-form-item-gi label="作者">
                <n-input v-model:value="form.author" placeholder="Forge Generator" />
              </n-form-item-gi>
            </n-grid>

            <div class="settings-section-title">
              命名策略
            </div>
            <n-grid :cols="4" :x-gap="12" :y-gap="4" responsive="screen">
              <n-form-item-gi label="业务类名前缀">
                <n-input v-model:value="form.entityPrefix" placeholder="例如 Biz（可不填）" />
              </n-form-item-gi>
              <n-form-item-gi label="需要剥离的表前缀" :span="3">
                <n-dynamic-tags
                  v-model:value="form.stripTablePrefixes"
                  :input-props="{ placeholder: '输入前缀后回车，例如 tf_' }"
                />
              </n-form-item-gi>
            </n-grid>

            <div class="settings-section-title">
              输出目录
            </div>
            <n-grid :cols="4" :x-gap="12" :y-gap="4" responsive="screen">
              <n-form-item-gi label="后端 Java 根目录">
                <n-input v-model:value="form.backendBasePath" placeholder="backend/src/main/java" />
              </n-form-item-gi>
              <n-form-item-gi label="Mapper XML 根目录">
                <n-input v-model:value="form.mapperXmlBasePath" placeholder="backend/src/main/resources/mapper" />
              </n-form-item-gi>
              <n-form-item-gi label="前端页面根目录">
                <n-input v-model:value="form.frontendBasePath" placeholder="frontend/src/views" />
              </n-form-item-gi>
              <n-form-item-gi label="前端 API 根目录">
                <n-input v-model:value="form.frontendApiBasePath" placeholder="frontend/src/api" />
              </n-form-item-gi>
            </n-grid>

            <div class="settings-section-title">
              下载内容
            </div>
            <n-form-item class="include-options">
              <n-space :size="18" wrap>
                <n-checkbox v-model:checked="form.includeBackend">
                  后端 Java / Mapper XML
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeFrontend">
                  前端页面 / API
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeSql">
                  数据库 SQL
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeMenuSql" :disabled="!form.includeSql">
                  菜单 SQL
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeDictSql" :disabled="!form.includeSql">
                  字典 SQL
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeExcelSql" :disabled="!form.includeSql">
                  导入导出配置 SQL
                </n-checkbox>
              </n-space>
            </n-form-item>
            <n-form-item v-if="isApplicationScope" label="生成对象">
              <div class="object-selector">
                <div class="object-selector-head">
                  <span>主对象会自动带上已配置的子表或左树依赖，不重复生成冲突代码。</span>
                  <n-space size="small">
                    <n-button text type="primary" @click="selectAllObjects">
                      全选
                    </n-button>
                    <n-button text @click="form.objectIds = []">
                      清空
                    </n-button>
                  </n-space>
                </div>
                <n-checkbox-group v-model:value="form.objectIds" class="object-checkbox-grid">
                  <label
                    v-for="object in availableObjects"
                    :key="object.objectId"
                    class="object-checkbox-item"
                  >
                    <n-checkbox :value="object.objectId" :disabled="!object.configKey" />
                    <span class="object-checkbox-copy">
                      <strong>{{ object.objectName || object.objectCode }}</strong>
                      <small>{{ objectRoleLabel(object.objectRole) }} · {{ layoutLabel(object.layoutType) }}</small>
                    </span>
                  </label>
                </n-checkbox-group>
              </div>
            </n-form-item>
          </n-form>
        </n-collapse-item>
      </n-collapse>

      <n-spin :show="previewing || loadingOptions">
        <div class="preview-container">
          <aside class="file-list">
            <div class="file-list-header">
              <span>目录</span>
              <span>{{ fileCount }} 个文件<span v-if="!previewReady && fileCount"> · 设置已变更</span></span>
            </div>
            <n-tree
              block-line
              expand-on-click
              :data="fileTree"
              :expanded-keys="expandedKeys"
              :selected-keys="selectedKeys"
              :render-label="renderTreeLabel"
              @update:expanded-keys="keys => expandedKeys = keys"
              @update:selected-keys="handleTreeSelect"
            />
          </aside>

          <section class="code-viewer">
            <div class="code-header">
              <span class="file-name">{{ selectedFile || '请选择文件' }}</span>
              <n-button size="small" :disabled="!selectedFile" @click="copySelectedFile">
                <template #icon>
                  <n-icon><CopyOutline /></n-icon>
                </template>
                复制
              </n-button>
            </div>
            <div v-if="selectedFile" ref="editorContainer" class="editor-container" />
            <n-empty v-else class="preview-empty" description="暂无可预览文件" />
          </section>
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
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { sql } from '@codemirror/lang-sql'
import { xml } from '@codemirror/lang-xml'
import { oneDark } from '@codemirror/theme-one-dark'
import { CopyOutline, DownloadOutline, RefreshOutline, SaveOutline } from '@vicons/ionicons5'
import { basicSetup, EditorView } from 'codemirror'
import { useMessage } from 'naive-ui'
import { computed, h, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import {
  businessAppCodeOptions,
  businessAppCodePreview,
  businessDownloadAppCode,
  businessSaveAppCodeOptions,
} from '@/api/business-app'
import {
  businessApplicationCodeOptions,
  downloadBusinessApplicationCode,
  previewBusinessApplicationCode,
  saveBusinessApplicationCodeOptions,
} from '@/api/business-application'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  app: {
    type: Object,
    default: null,
  },
  scope: {
    type: String,
    default: 'ENTRY',
  },
})

const emit = defineEmits(['update:show'])
const message = useMessage()
const saving = ref(false)
const previewing = ref(false)
const downloading = ref(false)
const loadingOptions = ref(false)
const availableObjects = ref([])
const fileMap = ref({})
const fileTree = ref([])
const expandedKeys = ref([])
const selectedFile = ref('')
const editorContainer = ref(null)
const form = reactive(defaultForm())
const previewSignature = ref('')
let editorView = null

const isApplicationScope = computed(() => props.scope === 'APPLICATION')
const sourceTypeOptions = computed(() => [
  { label: '当前草稿', value: 'DRAFT' },
  { label: '已发布版本', value: 'PUBLISHED' },
  ...(!isApplicationScope.value ? [{ label: '指定版本', value: 'VERSION' }] : []),
])

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})
const panelTitle = computed(() => isApplicationScope.value
  ? `${props.app?.applicationName || '应用'}完整代码`
  : `${props.app?.appName || '访问入口'}功能代码`)
const fileCount = computed(() => Object.keys(fileMap.value).length)
const selectedKeys = computed(() => selectedFile.value ? [selectedFile.value] : [])
const currentSignature = computed(() => JSON.stringify(normalizePayload(buildPayload())))
const previewReady = computed(() => fileCount.value > 0 && previewSignature.value === currentSignature.value)

watch(() => props.show, async (value) => {
  if (value) {
    await openWorkbench()
    return
  }
  destroyEditor()
})

watch(() => props.app?.id, async () => {
  if (props.show)
    await openWorkbench()
})

async function openWorkbench() {
  resetWorkbench()
  await loadOptions()
  await previewCode()
}

function resetWorkbench() {
  Object.assign(form, defaultForm())
  fileMap.value = {}
  fileTree.value = []
  expandedKeys.value = []
  selectedFile.value = ''
  availableObjects.value = []
  previewSignature.value = ''
  destroyEditor()
}

async function loadOptions() {
  if (!props.app?.id)
    return
  loadingOptions.value = true
  try {
    const res = isApplicationScope.value
      ? await businessApplicationCodeOptions(props.app.id)
      : await businessAppCodeOptions(props.app.id)
    const options = res.data || {}
    availableObjects.value = Array.isArray(options.objects) ? options.objects : []
    Object.assign(form, defaultForm(), options)
    form.stripTablePrefixes = normalizeStripTablePrefixes(options.stripTablePrefixes)
    if (isApplicationScope.value) {
      form.objectIds = availableObjects.value
        .filter(object => object.configKey)
        .map(object => object.objectId)
    }
  }
  finally {
    loadingOptions.value = false
  }
}

async function saveOptions() {
  if (!props.app?.id)
    return
  saving.value = true
  try {
    if (isApplicationScope.value)
      await saveBusinessApplicationCodeOptions(props.app.id, buildPayload())
    else
      await businessSaveAppCodeOptions(props.app.id, buildPayload())
    message.success('代码包设置已保存')
    await previewCode()
  }
  finally {
    saving.value = false
  }
}

async function previewCode() {
  if (!props.app?.id)
    return
  previewSignature.value = ''
  previewing.value = true
  try {
    if (isApplicationScope.value && !form.objectIds.length) {
      message.warning('请至少选择一个需要生成代码的数据对象')
      return
    }
    const payload = buildPayload()
    const res = isApplicationScope.value
      ? await previewBusinessApplicationCode(props.app.id, payload)
      : await businessAppCodePreview(props.app.id, payload)
    const files = res.data?.files || {}
    applyPreviewFiles(files)
    previewSignature.value = JSON.stringify(normalizePayload(payload))
  }
  finally {
    previewing.value = false
  }
}

async function downloadCode() {
  if (!props.app?.id)
    return
  downloading.value = true
  try {
    if (!previewReady.value) {
      message.warning('代码包设置已变化，请重新预览后再下载')
      return
    }
    const payload = buildPayload()
    const blob = isApplicationScope.value
      ? await downloadBusinessApplicationCode(props.app.id, payload)
      : await businessDownloadAppCode(props.app.id, payload)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = isApplicationScope.value
      ? `${props.app.applicationCode || 'application'}-source.zip`
      : `${props.app.appCode || props.app.configKey || 'app'}-code.zip`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }
  finally {
    downloading.value = false
  }
}

function applyPreviewFiles(files) {
  fileMap.value = files || {}
  const keys = Object.keys(fileMap.value).sort((a, b) => a.localeCompare(b))
  const treeResult = buildFileTree(keys)
  fileTree.value = treeResult.tree
  expandedKeys.value = treeResult.expandedKeys
  selectedFile.value = keys[0] || ''
  nextTick(() => {
    if (selectedFile.value)
      initEditor(fileMap.value[selectedFile.value], selectedFile.value)
    else
      destroyEditor()
  })
}

function buildFileTree(paths) {
  const root = []
  const nodeMap = new Map()
  const directoryKeys = []
  paths.forEach((path) => {
    const parts = path.split('/').filter(Boolean)
    let currentChildren = root
    let currentPath = ''
    parts.forEach((part, index) => {
      const isFile = index === parts.length - 1
      currentPath = currentPath ? `${currentPath}/${part}` : part
      const key = isFile ? path : `dir:${currentPath}`
      let node = nodeMap.get(key)
      if (!node) {
        node = {
          key,
          label: part,
          type: isFile ? 'file' : 'directory',
          children: isFile ? undefined : [],
        }
        nodeMap.set(key, node)
        currentChildren.push(node)
        if (!isFile)
          directoryKeys.push(key)
      }
      if (!isFile)
        currentChildren = node.children
    })
  })
  sortTree(root)
  return { tree: root, expandedKeys: directoryKeys }
}

function sortTree(nodes) {
  nodes.sort((a, b) => {
    if (a.type !== b.type)
      return a.type === 'directory' ? -1 : 1
    return a.label.localeCompare(b.label)
  })
  nodes.forEach((node) => {
    if (node.children)
      sortTree(node.children)
  })
}

function renderTreeLabel({ option }) {
  return h('span', { class: ['tree-node-label', option.type === 'directory' ? 'is-directory' : 'is-file'] }, [
    h('i', {
      class: option.type === 'directory'
        ? 'i-material-symbols:folder-outline-rounded'
        : 'i-material-symbols:description-outline-rounded',
    }),
    h('span', option.label),
  ])
}

async function handleTreeSelect(keys) {
  const key = keys?.[0]
  if (!key || !fileMap.value[key])
    return
  selectedFile.value = key
  await nextTick()
  initEditor(fileMap.value[key], key)
}

function getLanguageSupport(filename) {
  if (filename.endsWith('.java'))
    return java()
  if (filename.endsWith('.xml'))
    return xml()
  if (filename.endsWith('.vue') || filename.endsWith('.js') || filename.endsWith('.ts'))
    return javascript()
  if (filename.endsWith('.sql'))
    return sql()
  return null
}

function initEditor(code, filename) {
  destroyEditor()
  if (!editorContainer.value)
    return
  const extensions = [
    basicSetup,
    oneDark,
    EditorView.editable.of(false),
  ]
  const languageSupport = getLanguageSupport(filename)
  if (languageSupport)
    extensions.splice(1, 0, languageSupport)
  editorView = new EditorView({
    doc: code || '',
    extensions,
    parent: editorContainer.value,
  })
}

function destroyEditor() {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
}

function copySelectedFile() {
  const code = fileMap.value[selectedFile.value]
  if (!code)
    return
  navigator.clipboard.writeText(code)
    .then(() => message.success('复制成功'))
    .catch(() => message.error('复制失败'))
}

function buildPayload() {
  return {
    sourceType: form.sourceType || 'DRAFT',
    versionId: form.sourceType === 'VERSION' ? form.versionId : null,
    businessApiBase: form.businessApiBase,
    groupId: form.groupId,
    domainPackage: form.domainPackage,
    moduleName: form.moduleName,
    author: form.author,
    entityPrefix: form.entityPrefix,
    stripTablePrefixes: [...form.stripTablePrefixes],
    backendBasePath: form.backendBasePath,
    mapperXmlBasePath: form.mapperXmlBasePath,
    frontendApiBasePath: form.frontendApiBasePath,
    includeBackend: form.includeBackend,
    includeFrontend: form.includeFrontend,
    includeSql: form.includeSql,
    includeMenuSql: form.includeMenuSql,
    includeDictSql: form.includeDictSql,
    includeExcelSql: form.includeExcelSql,
    frontendBasePath: form.frontendBasePath,
    objectIds: isApplicationScope.value ? [...form.objectIds] : undefined,
  }
}

function normalizePayload(payload) {
  return {
    ...payload,
    objectIds: Array.isArray(payload.objectIds)
      ? [...payload.objectIds].map(String).sort((left, right) => left.localeCompare(right))
      : undefined,
  }
}

function normalizeStripTablePrefixes(value) {
  if (Array.isArray(value))
    return value.filter(item => typeof item === 'string' && item.trim()).map(item => item.trim())
  if (typeof value === 'string')
    return value.split(',').map(item => item.trim()).filter(Boolean)
  return [...defaultForm().stripTablePrefixes]
}

function selectAllObjects() {
  form.objectIds = availableObjects.value
    .filter(object => object.configKey)
    .map(object => object.objectId)
}

function objectRoleLabel(role) {
  return {
    PRIMARY: '主对象',
    DETAIL: '明细对象',
    REFERENCE: '树形/引用对象',
    SHARED: '共享对象',
  }[role] || '应用对象'
}

function layoutLabel(layoutType) {
  return {
    'simple-crud': '单表 CRUD',
    'tree-crud': '左树右表',
    'master-detail-crud': '主子表',
  }[layoutType] || '基础页面'
}

function defaultForm() {
  return {
    sourceType: 'DRAFT',
    versionId: null,
    businessApiBase: '',
    groupId: '',
    domainPackage: '',
    moduleName: '',
    author: '',
    entityPrefix: '',
    stripTablePrefixes: ['sys_', 'ai_', 't_', 'tb_'],
    backendBasePath: 'backend/src/main/java',
    mapperXmlBasePath: 'backend/src/main/resources/mapper',
    includeSql: true,
    includeMenuSql: true,
    includeDictSql: true,
    includeExcelSql: true,
    includeBackend: true,
    includeFrontend: true,
    frontendBasePath: 'frontend/src/views',
    frontendApiBasePath: 'frontend/src/api',
    objectIds: [],
  }
}

onUnmounted(() => {
  destroyEditor()
})
</script>

<style scoped>
.app-code-workbench {
  display: grid;
  grid-template-rows: auto auto minmax(300px, 1fr);
  gap: 12px;
  height: min(84vh, 900px);
  min-height: 560px;
  overflow: hidden;
}

.code-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 10px;
}

.toolbar-meta {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.settings-collapse {
  border: 1px solid var(--n-border-color, var(--border-light, #e5e7eb));
  border-radius: 8px;
  background: var(--n-color, var(--bg-primary, #fff));
  padding: 0 12px;
}

.settings-collapse :deep(.n-collapse-item__content-inner) {
  max-height: 360px;
  overflow-y: auto;
}

.settings-section-title {
  margin: 2px 0 8px;
  color: var(--n-text-color-2, var(--text-secondary, #4e5969));
  font-size: 12px;
  font-weight: 600;
}

.settings-section-title:not(:first-child) {
  margin-top: 8px;
  border-top: 1px solid var(--n-border-color, var(--border-light, #e5e7eb));
  padding-top: 10px;
}

.include-options {
  margin-bottom: 6px;
}

.settings-collapse :deep(.n-dynamic-tags) {
  width: 100%;
}

.object-selector {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--n-border-color, var(--border-light, #e5e7eb));
  border-radius: 7px;
  background: var(--n-color, var(--bg-primary, #fff));
}

.object-selector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  padding: 5px 10px;
  border-bottom: 1px solid var(--n-border-color, var(--border-light, #e5e7eb));
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
  font-size: 11px;
}

.object-checkbox-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  max-height: 110px;
  overflow-y: auto;
  padding: 7px;
  background: var(--n-border-color, var(--border-light, #e5e7eb));
}

.object-checkbox-item {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  min-height: 42px;
  padding: 5px 8px;
  background: var(--n-color, var(--bg-primary, #fff));
  cursor: pointer;
}

.object-checkbox-item:hover {
  background: var(--n-color-hover, var(--bg-hover, #f2f3f5));
}

.object-checkbox-copy {
  display: grid;
  min-width: 0;
}

.object-checkbox-copy strong,
.object-checkbox-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.object-checkbox-copy strong {
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 12px;
  font-weight: 600;
}

.object-checkbox-copy small {
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-size: 10px;
}

.app-code-workbench :deep(.n-spin-container),
.app-code-workbench :deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
}

.preview-container {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.file-list {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: auto;
  border-right: 1px solid #e5e7eb;
  background: #fafafa;
}

.file-list-header {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  padding: 10px 12px;
}

.file-list :deep(.n-tree) {
  padding: 8px;
}

.file-list :deep(.tree-node-label) {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.file-list :deep(.tree-node-label i) {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 16px;
}

.file-list :deep(.tree-node-label span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-viewer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  background: #0f172a;
}

.code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #1e293b;
  background: #111827;
  padding: 9px 12px;
}

.file-name {
  overflow: hidden;
  color: #e5e7eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-container {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: auto;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
  min-height: 100%;
  font-size: 13px;
}

.editor-container :deep(.cm-scroller) {
  overflow: auto;
}

.preview-empty {
  align-self: center;
  justify-self: center;
}

@media (max-width: 900px) {
  .app-code-workbench {
    height: min(84vh, 820px);
  }

  .code-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .preview-container {
    grid-template-columns: 1fr;
  }

  .file-list {
    max-height: 220px;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .object-checkbox-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
