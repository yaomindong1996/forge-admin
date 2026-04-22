<template>
  <div class="schema-field-editor">
    <!-- 表头 -->
    <div class="editor-header">
      <div class="header-row">
        <div v-for="col in columns" :key="col.key" :style="{ flex: col.flex || 1, minWidth: col.width || '80px' }">
          {{ col.label }}
        </div>
        <div style="width: 60px; flex-shrink: 0;">操作</div>
      </div>
    </div>
    <!-- 行列表 -->
    <div class="editor-body">
      <div v-for="(row, idx) in localRows" :key="idx" class="editor-row">
        <!-- field -->
        <div :style="{ flex: 1.5, minWidth: '100px' }">
          <n-input v-model:value="row.field" size="small" placeholder="字段名(camelCase)" />
        </div>
        <!-- label -->
        <div :style="{ flex: 1.5, minWidth: '90px' }">
          <n-input v-model:value="row.label" size="small" placeholder="中文标签" />
        </div>
        <!-- type -->
        <div :style="{ flex: 1.2, minWidth: '90px' }">
          <n-select
            v-model:value="row.type"
            size="small"
            :options="typeOptions"
            placeholder="类型"
          />
        </div>
        <!-- required (仅 editSchema) -->
        <div v-if="mode === 'edit'" style="flex: 0.6; min-width: 60px; display: flex; align-items: center; justify-content: center;">
          <n-checkbox v-model:checked="row.required" size="small" />
        </div>
        <!-- dictType (所有模式通用) -->
        <div :style="{ flex: 1.5, minWidth: '100px' }">
          <n-select
            v-model:value="row.dictType"
            size="small"
            placeholder="字典类型(选填)"
            :options="dictTypeOptions"
            filterable
            clearable
            tag
          />
        </div>
        <!-- width (columnsSchema) -->
        <div v-if="mode === 'columns'" :style="{ flex: 0.8, minWidth: '70px' }">
          <n-input-number v-model:value="row.width" size="small" placeholder="宽度" :show-button="false" />
        </div>
        <!-- 操作 -->
        <div style="width: 60px; flex-shrink: 0; display: flex; gap: 4px; justify-content: center;">
          <n-button size="tiny" text type="error" @click="removeRow(idx)">
            <n-icon><CloseOutline /></n-icon>
          </n-button>
          <n-button v-if="idx > 0" size="tiny" text @click="moveUp(idx)">↑</n-button>
          <n-button v-if="idx < localRows.length - 1" size="tiny" text @click="moveDown(idx)">↓</n-button>
        </div>
      </div>
      <div v-if="localRows.length === 0" class="empty-tip">暂无字段，点击"添加字段"按钮新增</div>
    </div>
    <!-- 底部操作 -->
    <div class="editor-footer">
      <n-button size="small" dashed @click="addRow">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        添加字段
      </n-button>
      <n-button size="small" text style="margin-left: auto;" @click="showRaw = !showRaw">
        {{ showRaw ? '隐藏JSON' : '查看JSON' }}
      </n-button>
    </div>
    <!-- 原始 JSON 折叠区 -->
    <div v-if="showRaw" class="raw-json-area">
      <textarea
        class="raw-json-textarea"
        :value="rawJson"
        @change="handleRawChange"
        spellcheck="false"
        rows="6"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { AddOutline, CloseOutline } from '@vicons/ionicons5'
import { request } from '@/utils'

const props = defineProps({
  // 'search' | 'columns' | 'edit'
  mode: { type: String, default: 'search' },
  // JSON 字符串
  value: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const showRaw = ref(false)
const dictTypeOptions = ref([])

async function loadDictTypes() {
  try {
    const res = await request.get('/system/dict/type/list', { params: { pageSize: 9999 } })
    const list = res.data || []
    dictTypeOptions.value = list
      .filter(d => d.dictType)
      .map(d => ({
        label: d.dictName ? `${d.dictName} (${d.dictType})` : d.dictType,
        value: d.dictType,
      }))
  } catch (e) {
    console.warn('[SchemaFieldEditor] 加载字典类型失败:', e)
  }
}

onMounted(loadDictTypes)

const columns = computed(() => {
  if (props.mode === 'columns') {
    return [
      { key: 'field', label: '字段名', flex: 1.5 },
      { key: 'label', label: '中文标题', flex: 1.5 },
      { key: 'type', label: '类型', flex: 1.2 },
      { key: 'dictType', label: '字典类型', flex: 1.5 },
      { key: 'width', label: '宽度', flex: 0.8 },
    ]
  }
  if (props.mode === 'edit') {
    return [
      { key: 'field', label: '字段名', flex: 1.5 },
      { key: 'label', label: '中文标题', flex: 1.5 },
      { key: 'type', label: '类型', flex: 1.2 },
      { key: 'required', label: '必填', flex: 0.6 },
      { key: 'dictType', label: '字典类型', flex: 1.5 },
    ]
  }
  // search
  return [
    { key: 'field', label: '字段名', flex: 1.5 },
    { key: 'label', label: '中文标题', flex: 1.5 },
    { key: 'type', label: '类型', flex: 1.2 },
    { key: 'dictType', label: '字典类型', flex: 1.5 },
  ]
})
const searchTypeOptions = [
  { label: '输入框', value: 'input' },
  { label: '下拉框', value: 'select' },
  { label: '日期范围', value: 'daterange' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
]

const editTypeOptions = [
  { label: '输入框', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '下拉框', value: 'select' },
  { label: '单选', value: 'radio' },
  { label: '复选框', value: 'checkbox' },
  { label: '开关', value: 'switch' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '数字', value: 'number' },
  { label: '文件上传', value: 'upload' },
  { label: '图片上传', value: 'imageUpload' },
]

const columnTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '标签(字典)', value: 'dictTag' },
  { label: '日期', value: 'date' },
  { label: '数字', value: 'number' },
]

const typeOptions = computed(() => {
  if (props.mode === 'columns') return columnTypeOptions
  if (props.mode === 'edit') return editTypeOptions
  return searchTypeOptions
})

// 把 JSON 字符串解析成行数组
function parseToRows(jsonStr) {
  if (!jsonStr || !jsonStr.trim()) return []
  try {
    const parsed = JSON.parse(jsonStr)
    if (!Array.isArray(parsed)) return []
    return parsed.map(item => {
      if (props.mode === 'columns') {
        // columnsSchema: { key, title, dataIndex, width, render: {type, dictType} }
        return {
          field: item.key || item.dataIndex || '',
          label: item.title || '',
          type: item.render?.type || 'text',
          dictType: item.render?.dictType || '',
          width: item.width || null,
        }
      }
      else if (props.mode === 'edit') {
        // editSchema: { field, label, type, required, dictType }
        return {
          field: item.field || '',
          label: item.label || '',
          type: item.type || 'input',
          required: item.required === true || item.required === 'true',
          dictType: item.dictType || '',
        }
      }
      else {
        // searchSchema: { field, label, type, dictType }
        return {
          field: item.field || '',
          label: item.label || '',
          type: item.type || 'input',
          dictType: item.dictType || '',
        }
      }
    })
  }
  catch (e) {
    return []
  }
}

// 行数组转 JSON 字符串
function rowsToJson(rows) {
  if (!rows || rows.length === 0) return '[]'
  const arr = rows
    .filter(r => r.field || r.label)
    .map(row => {
      if (props.mode === 'columns') {
        const item = {
          key: row.field,
          title: row.label,
          dataIndex: row.field,
        }
        if (row.width) item.width = row.width
        if (row.type && row.type !== 'text') {
          item.render = { type: row.type }
          if (row.dictType) item.render.dictType = row.dictType
        }
        return item
      }
      else if (props.mode === 'edit') {
        const item = {
          field: row.field,
          label: row.label,
          type: row.type || 'input',
          required: !!row.required,
        }
        if (row.dictType) item.dictType = row.dictType
        return item
      }
      else {
        const item = {
          field: row.field,
          label: row.label,
          type: row.type || 'input',
        }
        if (row.dictType) item.dictType = row.dictType
        return item
      }
    })
  return JSON.stringify(arr, null, 2)
}

const localRows = ref(parseToRows(props.value))

// 外部 JSON 变化时同步（如 AI 生成完毕）
watch(() => props.value, (newVal) => {
  const newRows = parseToRows(newVal)
  // 简单比较避免无限循环
  if (JSON.stringify(newRows) !== JSON.stringify(localRows.value)) {
    localRows.value = newRows
  }
})

// 行变化时同步到外部
watch(localRows, () => {
  emit('update:value', rowsToJson(localRows.value))
}, { deep: true })

const rawJson = computed(() => rowsToJson(localRows.value))

function handleRawChange(e) {
  try {
    const parsed = JSON.parse(e.target.value)
    if (Array.isArray(parsed)) {
      emit('update:value', JSON.stringify(parsed, null, 2))
      localRows.value = parseToRows(JSON.stringify(parsed))
    }
  }
  catch (err) {
    // 非法 JSON 不处理
  }
}

function addRow() {
  if (props.mode === 'columns') {
    localRows.value.push({ field: '', label: '', type: 'text', dictType: '', width: null })
  }
  else if (props.mode === 'edit') {
    localRows.value.push({ field: '', label: '', type: 'input', required: false, dictType: '' })
  }
  else {
    localRows.value.push({ field: '', label: '', type: 'input', dictType: '' })
  }
}

function removeRow(idx) {
  localRows.value.splice(idx, 1)
}

function moveUp(idx) {
  if (idx <= 0) return
  const tmp = localRows.value[idx - 1]
  localRows.value[idx - 1] = localRows.value[idx]
  localRows.value[idx] = tmp
}

function moveDown(idx) {
  if (idx >= localRows.value.length - 1) return
  const tmp = localRows.value[idx + 1]
  localRows.value[idx + 1] = localRows.value[idx]
  localRows.value[idx] = tmp
}
</script>

<style scoped>
.schema-field-editor {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
}

.editor-header {
  background: #fafafa;
  border-bottom: 1px solid #e0e0e0;
  padding: 0 8px;
}

.header-row {
  display: flex;
  gap: 6px;
  padding: 6px 0;
  font-weight: 500;
  color: #666;
  font-size: 12px;
}

.editor-body {
  padding: 4px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 400px;
  overflow-y: auto;
}

.editor-row {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 3px 0;
}

.empty-tip {
  padding: 16px;
  text-align: center;
  color: #aaa;
}

.editor-footer {
  padding: 6px 8px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
}

.raw-json-area {
  padding: 8px;
  border-top: 1px solid #eee;
  background: #f9f9f9;
}

.raw-json-textarea {
  width: 100%;
  resize: vertical;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  border: 1px solid #ddd;
  padding: 6px;
  border-radius: 3px;
  background: #fff;
  box-sizing: border-box;
}
</style>
