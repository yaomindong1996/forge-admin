<template>
  <div class="desensitize-config-panel">
    <div class="panel-header">
      <span>字段脱敏规则配置</span>
      <n-button size="small" dashed @click="addRule">+ 添加规则</n-button>
    </div>
    <div v-if="rules.length === 0" class="empty-tip">
      暂无脱敏规则。点击"添加规则"手动配置，或让 AI 在生成时自动识别敏感字段。
    </div>
    <div v-else class="rules-list">
      <div class="rule-row header">
        <span class="col-field">字段名</span>
        <span class="col-label">字段标签</span>
        <span class="col-type">脱敏类型</span>
        <span class="col-preview">预览效果</span>
        <span class="col-action"></span>
      </div>
      <div v-for="(rule, idx) in rules" :key="idx" class="rule-row">
        <n-input v-model:value="rule.field" size="small" class="col-field" placeholder="字段名" />
        <n-input v-model:value="rule.label" size="small" class="col-label" placeholder="显示标签" />
        <n-select
          v-model:value="rule.type"
          size="small"
          class="col-type"
          :options="desensitizeTypeOptions"
          placeholder="选择类型"
        />
        <span class="col-preview">{{ getPreview(rule) }}</span>
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
import { ref, computed, watch } from 'vue'
import { CloseOutline } from '@vicons/ionicons5'

const props = defineProps({
  value: { type: String, default: '' },
  columnsSchema: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const desensitizeTypeOptions = [
  { label: '手机号', value: 'PHONE' },
  { label: '身份证号', value: 'ID_CARD' },
  { label: '邮箱', value: 'EMAIL' },
  { label: '银行卡号', value: 'BANK_CARD' },
  { label: '姓名', value: 'NAME' },
  { label: '地址', value: 'ADDRESS' },
  { label: '密码', value: 'PASSWORD' },
  { label: '车牌号', value: 'CAR_LICENSE' },
  { label: '自定义', value: 'CUSTOM' },
]

const previewMap = {
  PHONE: '138****8888',
  ID_CARD: '110101********0011',
  EMAIL: 't***@example.com',
  BANK_CARD: '6222 **** **** 8888',
  NAME: '张**',
  ADDRESS: '北京市海淀区******',
  PASSWORD: '********',
  CAR_LICENSE: '京A****8',
  CUSTOM: '自定义规则',
}

const rules = ref([])

// 列字段缓存，用于查找中文标签
let _columnsFields = []

// 从 value 解析
function parseValue(val) {
  if (!val) return []
  try {
    const parsed = JSON.parse(val)
    const result = []
    for (const [field, config] of Object.entries(parsed)) {
      // 尝试从 columnsSchema 中查找中文标签
      const found = _columnsFields.find(f => f.field === field)
      result.push({
        field,
        label: config.label || found?.label || field,
        type: config.type || 'CUSTOM',
      })
    }
    return result
  } catch (e) {
    return []
  }
}

// 序列化到 value
function serializeRules() {
  const obj = {}
  for (const rule of rules.value) {
    if (!rule.field) continue
    obj[rule.field] = {
      type: rule.type,
      label: rule.label,
    }
  }
  return Object.keys(obj).length > 0 ? JSON.stringify(obj, null, 2) : ''
}

// 从 columnsSchema 自动提取字段
function extractFields(schemaStr) {
  if (!schemaStr) return []
  try {
    const parsed = JSON.parse(schemaStr)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter(item => {
        const field = item.dataIndex || item.key || item.prop
        return field && field !== 'actions' && field !== 'action'
      })
      .map(item => ({
        field: item.dataIndex || item.key || item.prop,
        label: item.title || item.label || item.dataIndex || item.key || item.prop,
      }))
  } catch (e) {
    return []
  }
}

// 初始化
watch(() => props.value, (val) => {
  rules.value = parseValue(val)
}, { immediate: true })

// 监听 columnsSchema 变化，自动建议敏感字段并补充中文标签
watch(() => props.columnsSchema, (schema) => {
  const fields = extractFields(schema)
  _columnsFields = fields

  // 为已有规则补充中文标签
  for (const rule of rules.value) {
    const found = fields.find(f => f.field === rule.field)
    if (found && (!rule.label || rule.label === rule.field)) {
      rule.label = found.label
    }
  }

  const sensitiveKeywords = [
    { kw: 'phone', type: 'PHONE' },
    { kw: 'mobile', type: 'PHONE' },
    { kw: 'tel', type: 'PHONE' },
    { kw: 'id_card', type: 'ID_CARD' },
    { kw: 'idcard', type: 'ID_CARD' },
    { kw: 'email', type: 'EMAIL' },
    { kw: 'mail', type: 'EMAIL' },
    { kw: 'bank_card', type: 'BANK_CARD' },
    { kw: 'password', type: 'PASSWORD' },
    { kw: 'pwd', type: 'PASSWORD' },
    { kw: 'address', type: 'ADDRESS' },
    { kw: 'name', type: 'NAME' },
    { kw: 'license', type: 'CAR_LICENSE' },
  ]

  for (const f of fields) {
    const lowerField = f.field.toLowerCase()
    const matched = sensitiveKeywords.find(sk => lowerField.includes(sk.kw))
    if (matched) {
      const exists = rules.value.some(r => r.field === f.field)
      if (!exists) {
        rules.value.push({
          field: f.field,
          label: f.label,
          type: matched.type,
        })
      }
    }
  }
  emitUpdate()
}, { immediate: false })

function emitUpdate() {
  emit('update:value', serializeRules())
}

watch(rules, emitUpdate, { deep: true })

function addRule() {
  rules.value.push({ field: '', label: '', type: 'PHONE' })
}

function removeRule(idx) {
  rules.value.splice(idx, 1)
}

function getPreview(rule) {
  return previewMap[rule.type] || '-'
}

const jsonValue = computed(() => {
  return serializeRules() || '{}'
})
</script>

<style scoped>
.desensitize-config-panel {
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
  width: 120px;
  flex-shrink: 0;
}

.col-label {
  width: 120px;
  flex-shrink: 0;
}

.col-type {
  width: 130px;
  flex-shrink: 0;
}

.col-preview {
  flex: 1;
  font-size: 12px;
  color: #666;
  font-family: monospace;
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
