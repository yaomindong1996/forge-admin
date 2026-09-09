<template>
  <div class="schema-tree-editor">
    <div class="ste-toolbar">
      <NButton size="tiny" dashed type="primary" :disabled="readonly" @click="addRootField">
        添加字段
      </NButton>
      <NButton size="tiny" dashed :disabled="readonly" @click="openImport">
        从 JSON 实例导入
      </NButton>
      <NButton v-if="isOutput" size="tiny" quaternary @click="showPaths = !showPaths">
        {{ showPaths ? '隐藏字段路径' : '显示字段路径' }}
      </NButton>
      <NButton size="tiny" quaternary @click="toggleViewMode">
        {{ viewMode === 'tree' ? 'JSON 模式' : '结构化模式' }}
      </NButton>
      <span class="ste-count">共 {{ fieldCount }} 个字段</span>
    </div>

    <template v-if="viewMode === 'tree' && !isOutput">
      <div class="ste-list">
        <div v-for="item in flatRows" :key="item.node._id" class="ste-card">
          <div class="ste-card__head">
            <span>{{ item.node.label || item.node.name || '未命名参数' }}</span>
            <NButton
              size="tiny"
              quaternary
              type="error"
              :disabled="readonly"
              @click="removeNode(item.node)"
            >
              删除
            </NButton>
          </div>
          <label class="ste-field">
            <span>字段名</span>
            <NInput
              v-model:value="item.node.name"
              size="small"
              placeholder="英文标识，如 keyword、userId"
              :status="nameStatus(item.node)"
              :disabled="readonly"
            />
          </label>
          <label class="ste-field">
            <span>显示名称</span>
            <NInput
              v-model:value="item.node.label"
              size="small"
              placeholder="给调用方看的名称，如 企业名称"
              :disabled="readonly"
            />
          </label>
          <div class="ste-field-row">
            <label class="ste-field">
              <span>类型</span>
              <NSelect
                v-model:value="item.node.type"
                size="small"
                :options="typeOptions"
                :disabled="readonly"
              />
            </label>
            <label class="ste-required-field">
              <NCheckbox v-model:checked="item.node.required" size="small" :disabled="readonly" />
              <span>必填</span>
            </label>
          </div>
        </div>
      </div>

      <div v-if="!flatRows.length" class="ste-empty">
        还没有参数。点击「添加字段」逐项填写，或「从 JSON 实例导入」粘贴真实请求参数自动生成。
      </div>
    </template>

    <template v-else-if="viewMode === 'tree'">
      <div class="ste-header" :style="gridStyle">
        <span />
        <span>字段名</span>
        <span>显示名称</span>
        <span>类型</span>
        <span>必填</span>
        <span v-if="showPaths">字段路径</span>
        <span />
      </div>

      <div class="ste-list">
        <div
          v-for="item in flatRows"
          :key="item.node._id"
          class="ste-row"
          :style="[gridStyle, { paddingLeft: `${item.depth * 18}px` }]"
        >
          <span
            class="ste-toggle"
            :class="{ 'is-leaf': !hasChildren(item.node) }"
            @click="toggleExpand(item.node)"
          >{{ hasChildren(item.node) ? (item.node._expanded === false ? '▸' : '▾') : '' }}</span>
          <NInput
            v-model:value="item.node.name"
            size="small"
            placeholder="英文标识，如 userName"
            :status="nameStatus(item.node)"
            :disabled="readonly"
          />
          <NInput
            v-model:value="item.node.label"
            size="small"
            placeholder="显示名称，如 用户名"
            :disabled="readonly"
          />
          <NSelect
            v-model:value="item.node.type"
            size="small"
            :options="typeOptions"
            :disabled="readonly"
          />
          <div class="ste-required">
            <NCheckbox v-model:checked="item.node.required" size="small" :disabled="readonly" title="必填" />
          </div>
          <NInput
            v-if="showPaths"
            size="small"
            :value="displayPath(item)"
            :placeholder="item.autoPath || '自动生成'"
            :status="pathStatus(item)"
            :disabled="readonly"
            @update:value="value => updatePath(item, value)"
          />
          <div class="ste-actions">
            <NButton
              v-if="canNest(item.node)"
              size="tiny"
              quaternary
              type="primary"
              title="添加子字段"
              :disabled="readonly"
              @click="addChild(item.node)"
            >
              +子
            </NButton>
            <NButton
              size="tiny"
              quaternary
              type="error"
              title="删除"
              :disabled="readonly"
              @click="removeNode(item.node)"
            >
              删
            </NButton>
          </div>
        </div>
      </div>

      <div v-if="!flatRows.length" class="ste-empty">
        还没有字段。点击「添加字段」逐项配置，或「从 JSON 实例导入」粘贴真实报文自动生成。
      </div>

      <p v-if="hint" class="ste-hint">
        {{ hint }}
      </p>
    </template>

    <template v-else>
      <NInput
        v-model:value="jsonText"
        type="textarea"
        :rows="textareaRows"
        :placeholder="jsonPlaceholder"
      />
      <div class="ste-json-actions">
        <NButton size="tiny" @click="formatJsonText">
          格式化
        </NButton>
        <NButton size="tiny" type="primary" @click="applyJsonText">
          应用并回到结构化
        </NButton>
      </div>
      <p v-if="jsonError" class="ste-error">
        {{ jsonError }}
      </p>
    </template>

    <NModal
      v-model:show="importVisible"
      preset="card"
      title="从 JSON 实例导入"
      class="ste-import-modal"
      style="width: min(640px, 92vw)"
    >
      <div class="ste-import-body">
        <NInput
          v-model:value="importText"
          type="textarea"
          :rows="8"
          :placeholder="importPlaceholder"
        />
        <div v-if="importRootNameVisible" class="ste-import-root">
          <span>顶层是数组时的包装字段名</span>
          <NInput v-model:value="importRootName" size="small" placeholder="如 records" style="width: 180px" />
        </div>
        <p v-if="importError" class="ste-error">
          {{ importError }}
        </p>
        <p v-else-if="importPreview" class="ste-hint">
          {{ importPreview }}
        </p>
      </div>
      <template #footer>
        <div class="ste-import-footer">
          <NButton size="small" :disabled="!parsedNodes.length" @click="applyImport('append')">
            追加到现有字段
          </NButton>
          <NButton size="small" type="primary" :disabled="!parsedNodes.length" @click="applyImport('replace')">
            替换全部字段
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup>
import { NButton, NCheckbox, NInput, NModal, NSelect } from 'naive-ui'
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  // input：输入参数定义（平铺行）；output：返回字段解析（支持上下级嵌套）
  mode: { type: String, default: 'output' },
  hint: { type: String, default: '' },
  textareaRows: { type: Number, default: 10 },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const jsonPlaceholder = '[{"name":"userName","label":"用户名","type":"string","required":true,"path":"Result.UserName"}]'
const importPlaceholder = '粘贴真实请求参数或响应 JSON，例如：\n{ "Result": [ { "Name": "某某公司" } ], "Status": "200" }'

// 与后端 ExternalQueryContractValidator#SAFE_NAME 保持一致
const SAFE_NAME = /^[a-z]\w{0,63}$/i
const typeOptions = [
  { label: '文本', value: 'string' },
  { label: '整数', value: 'integer' },
  { label: '小数', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '对象', value: 'object' },
  { label: '数组', value: 'array' },
]

const isOutput = computed(() => props.mode !== 'input')
const nodes = ref([])
const viewMode = ref('tree')
// 字段路径由层级结构自动推导，默认不展示；需要覆盖时再展开
const showPaths = ref(false)
const jsonText = ref('')
const jsonError = ref('')
let nodeSeq = 0

const importVisible = ref(false)
const importText = ref('')
const importRootName = ref('')
const importError = ref('')
const parsedNodes = ref([])

const gridStyle = computed(() => {
  if (!isOutput.value || !showPaths.value)
    return { gridTemplateColumns: '20px minmax(0, 1.2fr) minmax(0, 1.2fr) 92px 34px 60px' }
  return { gridTemplateColumns: '20px minmax(0, 1fr) minmax(0, 1fr) 92px 34px minmax(0, 1.2fr) 76px' }
})

// 展平渲染：附带深度、自动 path，供模板直接遍历
const flatRows = computed(() => {
  const rows = []
  const walk = (list, depth, prefix, viaArray) => {
    list.forEach((node) => {
      const autoPath = prefix
        ? `${prefix}.${viaArray ? `0.${node.name}` : node.name}`
        : (node.name || '')
      rows.push({ node, depth, autoPath })
      if (node.children?.length && node._expanded !== false)
        walk(node.children, depth + 1, autoPath, node.type === 'array')
    })
  }
  walk(nodes.value, 0, '', false)
  return rows
})

const fieldCount = computed(() => flatRows.value.filter(item => item.node.name).length)

watch(() => props.modelValue, () => {
  syncFromValue(props.modelValue)
}, { immediate: true })

watch(nodes, () => {
  const next = serializeNodes()
  if (next === normalizeIncoming(props.modelValue))
    return
  emit('update:modelValue', next)
}, { deep: true })

function nextId() {
  nodeSeq += 1
  return `node-${nodeSeq}`
}

function createNode(partial = {}) {
  return {
    _id: nextId(),
    name: partial.name || '',
    label: partial.label || '',
    type: typeOptions.some(option => option.value === partial.type) ? partial.type : 'string',
    required: Boolean(partial.required),
    path: partial.path || '',
    _pathCustom: Boolean(partial.path),
    _expanded: true,
    children: partial.children || [],
  }
}

function normalizeIncoming(value) {
  if (value == null || value === '')
    return '[]'
  if (typeof value === 'string')
    return value.trim() || '[]'
  try {
    return JSON.stringify(value)
  }
  catch {
    return '[]'
  }
}

function parseRows(value) {
  let parsed = value
  if (typeof value === 'string') {
    try {
      parsed = value.trim() ? JSON.parse(value) : []
    }
    catch {
      parsed = []
    }
  }
  const rows = Array.isArray(parsed) ? parsed : []
  if (!isOutput.value)
    return rows.map(row => createNode(row))
  // 输出模式按取值路径重建层级：数字段折叠为数组元素，缺失的父段补为 object/array 分组节点
  const root = []
  rows.forEach((row) => {
    const segments = String(row?.path || '').split('.').filter(Boolean)
    let list = root
    let parent = null
    let index = 0
    while (index < segments.length - 1) {
      const segment = segments[index]
      if (/^\d+$/.test(segment)) {
        if (parent)
          parent.type = 'array'
        index += 1
        continue
      }
      let child = list.find(node => node.name === segment)
      if (!child) {
        child = createNode({ name: segment, label: segment, type: /^\d+$/.test(segments[index + 1]) ? 'array' : 'object' })
        list.push(child)
      }
      parent = child
      list = child.children
      index += 1
    }
    list.push(createNode(row))
  })
  // 重新计算 autoPath 判断是否为用户自定义路径
  const recompute = (list, prefix, viaArray) => {
    list.forEach((node) => {
      const autoPath = prefix ? `${prefix}.${viaArray ? `0.${node.name}` : node.name}` : node.name
      if (node.path && node.path !== autoPath)
        node._pathCustom = true
      if (!node.path)
        node._pathCustom = false
      recompute(node.children, autoPath, node.type === 'array')
    })
  }
  recompute(root, '', false)
  return root
}

function serializeNodes() {
  const rows = []
  const walk = (list, prefix, viaArray) => {
    list.forEach((node) => {
      if (!node.name)
        return
      const autoPath = prefix ? `${prefix}.${viaArray ? `0.${node.name}` : node.name}` : node.name
      const row = {
        name: node.name,
        label: node.label || node.name,
        type: node.type || 'string',
      }
      if (node.required)
        row.required = true
      if (isOutput.value)
        row.path = node._pathCustom && node.path ? node.path : autoPath
      rows.push(row)
      if (node.children?.length)
        walk(node.children, autoPath, node.type === 'array')
    })
  }
  walk(nodes.value, '', false)
  return JSON.stringify(rows)
}

function syncFromValue(value) {
  if (serializeNodes() === normalizeIncoming(value))
    return
  nodes.value = parseRows(value)
}

function hasChildren(node) {
  return Boolean(node.children?.length)
}

function canNest(node) {
  return node.type === 'object' || node.type === 'array'
}

function toggleExpand(node) {
  if (!hasChildren(node))
    return
  node._expanded = node._expanded === false
}

function addRootField() {
  nodes.value.push(createNode())
}

function addChild(parent) {
  parent._expanded = true
  parent.children.push(createNode())
}

function removeNode(target) {
  const walk = list => list.filter((node) => {
    if (node === target)
      return false
    if (node.children?.length)
      node.children = walk(node.children)
    return true
  })
  nodes.value = walk(nodes.value)
}

function nameStatus(node) {
  return node.name && !SAFE_NAME.test(node.name) ? 'error' : undefined
}

function pathStatus(item) {
  if (!isOutput.value)
    return undefined
  if (!item.node.name)
    return undefined
  return displayPath(item) ? undefined : 'error'
}

function displayPath(item) {
  return item.node._pathCustom ? item.node.path : ''
}

function updatePath(item, value) {
  item.node.path = value
  item.node._pathCustom = value !== item.autoPath
}

function toggleViewMode() {
  if (viewMode.value === 'tree') {
    jsonText.value = prettyText(serializeNodes())
    jsonError.value = ''
    viewMode.value = 'json'
    return
  }
  applyJsonText()
}

function prettyText(text) {
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  }
  catch {
    return text
  }
}

function formatJsonText() {
  jsonText.value = prettyText(jsonText.value)
}

function applyJsonText() {
  try {
    const parsed = JSON.parse(jsonText.value || '[]')
    if (!Array.isArray(parsed))
      throw new Error('必须是 JSON 数组')
    jsonError.value = ''
    viewMode.value = 'tree'
    nodes.value = parseRows(JSON.stringify(parsed))
  }
  catch (exception) {
    jsonError.value = `JSON 不合法：${exception.message}`
  }
}

function openImport() {
  importText.value = ''
  importRootName.value = ''
  importError.value = ''
  parsedNodes.value = []
  importVisible.value = true
}

watch([importText, importRootName], () => {
  importError.value = ''
  parsedNodes.value = []
  const text = importText.value.trim()
  if (!text)
    return
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) {
      if (!parsed.length) {
        importError.value = '数组为空，没有可解析的字段'
        return
      }
      const rootName = importRootName.value.trim() || 'records'
      parsedNodes.value = [valueToNode(rootName, parsed)]
      return
    }
    if (parsed && typeof parsed === 'object') {
      parsedNodes.value = Object.entries(parsed).map(([key, value]) => valueToNode(key, value))
      return
    }
    importError.value = '请粘贴 JSON 对象或数组实例'
  }
  catch (exception) {
    importError.value = `JSON 不合法：${exception.message}`
  }
})

const importRootNameVisible = computed(() => {
  try {
    return Array.isArray(JSON.parse(importText.value || 'null'))
  }
  catch {
    return false
  }
})

const importPreview = computed(() => {
  if (!parsedNodes.value.length)
    return ''
  let total = 0
  let maxDepth = 0
  const walk = (list, depth) => {
    maxDepth = Math.max(maxDepth, depth)
    total += list.length
    list.forEach(node => walk(node.children || [], depth + 1))
  }
  walk(parsedNodes.value, 1)
  return `解析成功：共 ${total} 个字段，最深 ${maxDepth} 层。确认后${isOutput.value ? '按层级生成字段树' : '生成参数行'}。`
})

// 递归解析实例值：数组取第一个元素的结构作为子字段
function valueToNode(key, value) {
  const node = createNode({ name: key, label: key })
  if (value === null)
    return node
  if (Array.isArray(value)) {
    node.type = 'array'
    if (value.length && typeof value[0] === 'object' && value[0] !== null && !Array.isArray(value[0]))
      node.children = Object.entries(value[0]).map(([childKey, childValue]) => valueToNode(childKey, childValue))
    return node
  }
  switch (typeof value) {
    case 'boolean':
      node.type = 'boolean'
      break
    case 'number':
      node.type = Number.isInteger(value) ? 'integer' : 'number'
      break
    case 'object':
      node.type = 'object'
      node.children = Object.entries(value).map(([childKey, childValue]) => valueToNode(childKey, childValue))
      break
  }
  return node
}

function applyImport(strategy) {
  if (!parsedNodes.value.length)
    return
  // 输入模式保持平铺：嵌套结构只保留顶层行，子级折叠为 object/array 类型提示
  const incoming = isOutput.value
    ? parsedNodes.value
    : parsedNodes.value.map(node => ({ ...node, children: [] }))
  nodes.value = strategy === 'replace' ? incoming : [...nodes.value, ...incoming]
  importVisible.value = false
}
</script>

<style scoped>
.schema-tree-editor {
  width: 100%;
  min-width: 0;
}

.ste-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.ste-count {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-left: auto;
}

.ste-header,
.ste-row {
  display: grid;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.ste-header {
  padding-bottom: 6px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
  border-bottom: 1px solid var(--border-light);
}

.ste-list {
  display: grid;
  gap: 10px;
  padding-top: 8px;
}

.ste-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary, #f8fafc);
}

.ste-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.ste-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.ste-field > span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.ste-field-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.ste-required-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.ste-row {
  padding-right: 4px;
  padding-bottom: 2px;
  padding-top: 2px;
  border-left: 1px solid var(--border-light);
}

.ste-toggle {
  color: var(--text-tertiary);
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  text-align: center;
  user-select: none;
}

.ste-toggle.is-leaf {
  cursor: default;
}

.ste-required {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ste-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
}

.ste-empty {
  padding: 14px 12px;
  border: 1px dashed var(--border-light);
  border-radius: 6px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.ste-hint {
  margin: 8px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.ste-error {
  margin: 8px 0 0;
  color: var(--error-color);
  font-size: 12px;
  line-height: 18px;
}

.ste-json-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.ste-import-body {
  display: grid;
  gap: 10px;
}

.ste-import-root {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.ste-import-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
