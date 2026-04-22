<template>
  <div class="trans-config-panel">
    <div class="panel-header">
      <span>字典翻译规则配置</span>
      <n-button size="small" dashed @click="addRule">+ 添加规则</n-button>
    </div>
    <div v-if="rules.length === 0" class="empty-tip">
      暂无翻译规则。在 schema 中为字段添加 dictType 后，此处可自动提取；也可手动添加。
    </div>
    <div v-else class="rules-list">
      <div class="rule-row header">
        <span class="col-field">原字段</span>
        <span class="col-dict">字典类型</span>
        <span class="col-target">翻译字段</span>
        <span class="col-action"></span>
      </div>
      <div v-for="(rule, idx) in rules" :key="idx" class="rule-row">
        <div class="col-field">
          <n-input v-model:value="rule.field" size="small" placeholder="原字段名" />
          <span v-if="rule.label && rule.label !== rule.field" class="field-label">{{ rule.label }}</span>
        </div>
        <n-select
          v-model:value="rule.dictType"
          size="small"
          class="col-dict"
          placeholder="字典类型"
          :options="dictTypeOptions"
          filterable
          clearable
          tag
        />
        <n-input v-model:value="rule.targetField" size="small" class="col-target" placeholder="默认: 原字段+Name" />
        <n-button size="tiny" text type="error" class="col-action" @click="removeRule(idx)">
          <n-icon><CloseOutline /></n-icon>
        </n-button>
      </div>
    </div>
    <div class="json-view">
      <n-collapse>
        <n-collapse-item title="查看 JSON">
          <pre class="json-code">{{ jsonValue }}</pre>
        </n-collapse-item>
      </n-collapse>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { CloseOutline } from '@vicons/ionicons5'
import { request } from '@/utils'

const props = defineProps({
  value: { type: String, default: '' },
  columnsSchema: { type: String, default: '' },
  editSchema: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const rules = ref([])
const dictTypeOptions = ref([])
// 列字段缓存，用于查找中文标签
let _columnsFields = []

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
    console.warn('[TransConfigPanel] 加载字典类型失败:', e)
  }
}

onMounted(loadDictTypes)

function parseValue(val) {
  if (!val) return []
  try {
    const parsed = JSON.parse(val)
    const result = []
    for (const [field, config] of Object.entries(parsed)) {
      const found = _columnsFields.find(f => f.field === field)
      result.push({
        field,
        dictType: config.dictType || '',
        targetField: config.targetField || field + 'Name',
        label: found?.label || field,
      })
    }
    return result
  } catch (e) {
    return []
  }
}

function extractDictFields(schemas) {
  const fields = []
  for (const schemaStr of schemas) {
    if (!schemaStr) continue
    try {
      const parsed = JSON.parse(schemaStr)
      if (!Array.isArray(parsed)) continue
      for (const item of parsed) {
        const dt = item.dictType || item.render?.dictType
        if (dt) {
          const field = item.field || item.dataIndex || item.key || item.prop
          const label = item.label || item.title || field
          fields.push({ field, dictType: dt, targetField: field + 'Name', label })
        }
      }
    } catch (e) {}
  }
  return fields
}

// 从 value 初始化
watch(() => props.value, (val) => {
  rules.value = parseValue(val)
}, { immediate: true })

// 从 schema 自动提取 dictType 字段并补充中文标签
watch(
  [() => props.columnsSchema, () => props.editSchema],
  ([columns, edit]) => {
    const extracted = extractDictFields([columns, edit])
    _columnsFields = extracted

    // 为已有规则补充中文标签
    for (const rule of rules.value) {
      const found = extracted.find(f => f.field === rule.field)
      if (found && (!rule.label || rule.label === rule.field)) {
        rule.label = found.label
      }
    }

    for (const ex of extracted) {
      const exists = rules.value.some(r => r.field === ex.field)
      if (!exists) {
        rules.value.push({ ...ex })
      }
    }
    emitUpdate()
  },
  { immediate: false },
)

function emitUpdate() {
  const obj = {}
  for (const rule of rules.value) {
    if (!rule.field || !rule.dictType) continue
    obj[rule.field] = {
      dictType: rule.dictType,
      targetField: rule.targetField || rule.field + 'Name',
    }
  }
  emit('update:value', Object.keys(obj).length > 0 ? JSON.stringify(obj, null, 2) : '')
}

watch(rules, emitUpdate, { deep: true })

function addRule() {
  rules.value.push({ field: '', dictType: '', targetField: '' })
}

function removeRule(idx) {
  rules.value.splice(idx, 1)
}

const jsonValue = computed(() => {
  const obj = {}
  for (const rule of rules.value) {
    if (!rule.field || !rule.dictType) continue
    obj[rule.field] = {
      dictType: rule.dictType,
      targetField: rule.targetField || rule.field + 'Name',
    }
  }
  return Object.keys(obj).length > 0 ? JSON.stringify(obj, null, 2) : '{}'
})
</script>

<style scoped>
.trans-config-panel {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: #555;
}

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 13px;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #fff;
  border-radius: 4px;
}

.rule-row.header {
  background: transparent;
  font-size: 12px;
  color: #888;
  padding: 4px 8px;
}

.col-field {
  width: 140px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-label {
  font-size: 11px;
  color: #888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.col-dict {
  width: 160px;
  flex-shrink: 0;
}

.col-target {
  width: 160px;
  flex-shrink: 0;
}

.col-action {
  width: 28px;
  flex-shrink: 0;
  text-align: center;
}

.json-view {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ddd;
}

.json-code {
  font-size: 11px;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0;
}
</style>
