<template>
  <div class="api-config-editor">
    <div v-for="item in apiFields" :key="item.key" class="api-row">
      <div class="api-label">
        {{ item.label }}
      </div>
      <div class="api-method">
        <n-select
          v-model:value="localConfig[`${item.key}_method`]"
          size="small"
          :options="methodOptions"
          style="width: 80px"
        />
      </div>
      <div class="api-path" style="flex: 1;">
        <n-input
          v-model:value="localConfig[`${item.key}_path`]"
          size="small"
          :placeholder="item.placeholder"
        />
      </div>
    </div>
    <div class="api-footer">
      <n-button size="small" text @click="showRaw = !showRaw">
        {{ showRaw ? '隐藏JSON' : '查看JSON' }}
      </n-button>
    </div>
    <div v-if="showRaw" class="raw-json-area">
      <textarea
        class="raw-json-textarea"
        :value="rawJson"
        spellcheck="false"
        rows="6"
        @change="handleRawChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  value: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const showRaw = ref(false)

const methodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
  { label: 'PUT', value: 'put' },
  { label: 'DELETE', value: 'delete' },
]

const apiFields = [
  { key: 'list', label: '列表接口', placeholder: '/xxx/page' },
  { key: 'detail', label: '详情接口', placeholder: '/xxx/{id}' },
  { key: 'create', label: '新增接口', placeholder: '/xxx' },
  { key: 'update', label: '编辑接口', placeholder: '/xxx' },
  { key: 'delete', label: '删除接口', placeholder: '/xxx/{id}' },
]

// 解析 "get@/xxx/page" 格式
function parseApiStr(str) {
  if (!str)
    return { method: 'get', path: '' }
  const parts = str.split('@')
  if (parts.length === 2)
    return { method: parts[0].toLowerCase(), path: parts[1] }
  return { method: 'get', path: str }
}

// 初始化 localConfig
function initLocalConfig(jsonStr) {
  const cfg = {}
  apiFields.forEach((f) => {
    cfg[`${f.key}_method`] = 'get'
    cfg[`${f.key}_path`] = ''
  })
  if (!jsonStr || !jsonStr.trim())
    return cfg
  try {
    const parsed = JSON.parse(jsonStr)
    apiFields.forEach((f) => {
      if (parsed[f.key]) {
        const { method, path } = parseApiStr(parsed[f.key])
        cfg[`${f.key}_method`] = method
        cfg[`${f.key}_path`] = path
      }
    })
  }
  catch (e) {}
  return cfg
}

const localConfig = ref(initLocalConfig(props.value))

// 外部 JSON 变化时同步
watch(() => props.value, (newVal) => {
  const newCfg = initLocalConfig(newVal)
  if (JSON.stringify(newCfg) !== JSON.stringify(localConfig.value)) {
    localConfig.value = newCfg
  }
})

// 本地变化时生成 JSON
watch(localConfig, () => {
  emit('update:value', buildJson())
}, { deep: true })

function buildJson() {
  const obj = {}
  apiFields.forEach((f) => {
    const method = localConfig.value[`${f.key}_method`] || 'get'
    const path = localConfig.value[`${f.key}_path`] || ''
    if (path)
      obj[f.key] = `${method}@${path}`
  })
  return JSON.stringify(obj, null, 2)
}

const rawJson = computed(() => buildJson())

function handleRawChange(e) {
  try {
    const parsed = JSON.parse(e.target.value)
    emit('update:value', JSON.stringify(parsed, null, 2))
    localConfig.value = initLocalConfig(JSON.stringify(parsed))
  }
  catch (err) {}
}
</script>

<style scoped>
.api-config-editor {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
}

.api-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.api-label {
  width: 70px;
  flex-shrink: 0;
  font-size: 12px;
  color: #555;
  font-weight: 500;
}

.api-method {
  flex-shrink: 0;
}

.api-footer {
  padding: 6px 10px;
  display: flex;
  justify-content: flex-end;
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
