<template>
  <div class="template-variable-editor">
    <n-input
      ref="inputRef"
      :value="modelValue"
      :placeholder="placeholder"
      @update:value="updateValue"
    />
    <div class="token-groups">
      <section v-if="fieldTokens.length">
        <span>单据字段</span>
        <div>
          <n-button
            v-for="token in fieldTokens"
            :key="token.value"
            size="tiny"
            tertiary
            @click="insertToken(token.insertText)"
          >
            {{ token.label }}
          </n-button>
        </div>
      </section>
      <section v-if="variableTokens.length">
        <span>流程变量</span>
        <div>
          <n-button
            v-for="token in variableTokens"
            :key="token.value"
            size="tiny"
            tertiary
            @click="insertToken(token.insertText)"
          >
            {{ token.label }}
          </n-button>
        </div>
      </section>
    </div>
    <div class="template-preview">
      <span>预览</span>
      <strong>{{ previewText || '-' }}</strong>
    </div>
    <n-alert v-if="unknownTokens.length" type="warning" :bordered="false">
      未识别变量：{{ unknownTokens.join('、') }}。请从下方变量中选择插入，或确认流程中已定义该变量。
    </n-alert>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  variables: {
    type: Array,
    default: () => [],
  },
  placeholder: {
    type: String,
    default: '例如：${name}-流程',
  },
})

const emit = defineEmits(['update:modelValue'])
const inputRef = ref(null)

const fieldTokens = computed(() => (props.fields || [])
  .map(option => ({
    label: option.label || option.value,
    value: option.value,
    insertText: `\${${option.value}}`,
    sample: option.sampleValue || sampleFromLabel(option.label || option.value),
  }))
  .filter(item => item.value))

const variableTokens = computed(() => (props.variables || [])
  .map(option => ({
    label: option.label || option.value,
    value: option.value,
    insertText: `\${${option.value}}`,
    sample: option.sampleValue || sampleFromVariable(option.value),
  }))
  .filter(item => item.value))

const tokenMap = computed(() => {
  const result = new Map()
  ;[...fieldTokens.value, ...variableTokens.value].forEach((token) => {
    if (!result.has(token.value))
      result.set(token.value, token.sample || token.value)
  })
  return result
})

const usedTokens = computed(() => {
  const matches = String(props.modelValue || '').matchAll(/\$\{([^}]+)}/g)
  return Array.from(matches).map(match => match[1]).filter(Boolean)
})

const unknownTokens = computed(() => usedTokens.value
  .filter(token => !tokenMap.value.has(token))
  .filter((token, index, list) => list.indexOf(token) === index))

const previewText = computed(() => {
  let text = props.modelValue || ''
  usedTokens.value.forEach((token) => {
    const replacement = tokenMap.value.get(token) || token
    text = text.replaceAll(`\${${token}}`, replacement)
  })
  return text
})

function updateValue(value) {
  emit('update:modelValue', value || '')
}

function insertToken(token) {
  const current = props.modelValue || ''
  const element = inputRef.value?.inputElRef || inputRef.value?.textareaElRef
  const start = element?.selectionStart ?? current.length
  const end = element?.selectionEnd ?? current.length
  const next = `${current.slice(0, start)}${token}${current.slice(end)}`
  emit('update:modelValue', next)
  nextTick(() => {
    element?.focus?.()
    element?.setSelectionRange?.(start + token.length, start + token.length)
  })
}

function sampleFromLabel(label) {
  const text = String(label || '').replace(/（.*?）/g, '').trim()
  return text || '样例值'
}

function sampleFromVariable(value) {
  const samples = {
    objectCode: 'OPPORTUNITY',
    recordId: '10001',
    businessKey: 'OPPORTUNITY:10001',
    initiator: 'zhangsan',
    startUserId: '1',
    deptId: '10',
    deptManager: 'lisi',
  }
  return samples[value] || value || '变量值'
}
</script>

<style scoped>
.template-variable-editor {
  display: grid;
  gap: 8px;
  width: 100%;
}

.token-groups {
  display: grid;
  gap: 8px;
}

.token-groups section {
  display: grid;
  gap: 6px;
}

.token-groups section > span,
.template-preview span {
  color: #64748b;
  font-size: 12px;
}

.token-groups section > div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.template-preview {
  display: grid;
  gap: 4px;
  border-radius: 6px;
  background: #f8fafc;
  padding: 8px 10px;
}

.template-preview strong {
  overflow-wrap: anywhere;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
}
</style>
