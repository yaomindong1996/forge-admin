<template>
  <div class="no-rule-editor">
    <div class="template-row">
      <n-input
        ref="inputRef"
        :value="modelValue"
        :disabled="disabled"
        placeholder="例如：LEAVE-${yyyyMMdd}-${seq:4}"
        @update:value="updateValue"
      />
      <n-button secondary :disabled="disabled" :loading="previewing" @click="runPreview">
        预览
      </n-button>
    </div>

    <div class="token-groups">
      <section v-for="group in tokenGroups" :key="group.name">
        <strong>{{ group.name }}</strong>
        <div class="token-list">
          <button
            v-for="token in group.tokens"
            :key="token.insertText"
            type="button"
            :disabled="disabled"
            @click="insertToken(token.insertText)"
          >
            {{ token.label }}
          </button>
        </div>
      </section>
    </div>

    <div class="preview-strip" :class="{ invalid: preview && preview.valid === false }">
      <span>样例</span>
      <strong>{{ preview?.previewNo || '-' }}</strong>
    </div>
    <div v-if="preview?.errors?.length" class="issue-list error">
      <p v-for="item in preview.errors" :key="`${item.token || ''}_${item.message}`">
        {{ item.message }}<span v-if="item.suggestion">，{{ item.suggestion }}</span>
      </p>
    </div>
    <div v-else-if="preview?.warnings?.length" class="issue-list warning">
      <p v-for="item in preview.warnings" :key="`${item.token || ''}_${item.message}`">
        {{ item.message }}<span v-if="item.suggestion">，{{ item.suggestion }}</span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { businessDocumentNoRuleTokens, previewBusinessDocumentNoRule } from '@/api/business-app'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'preview'])

const inputRef = ref(null)
const tokens = ref([])
const preview = ref(null)
const previewing = ref(false)
let previewTimer = null

const tokenGroups = computed(() => {
  const groups = new Map()
  for (const token of tokens.value) {
    const groupName = token.groupName || '其他'
    if (!groups.has(groupName))
      groups.set(groupName, [])
    groups.get(groupName).push(token)
  }
  return Array.from(groups.entries()).map(([name, groupTokens]) => ({ name, tokens: groupTokens }))
})

watch(() => props.modelValue, () => schedulePreview())
watch(() => [props.suiteCode, props.objectCode], () => schedulePreview())

onMounted(async () => {
  try {
    const res = await businessDocumentNoRuleTokens()
    tokens.value = res.data || []
  }
  catch {
    tokens.value = fallbackTokens()
  }
  schedulePreview()
})

function updateValue(value) {
  emit('update:modelValue', normalizeTemplate(value))
}

function insertToken(token) {
  if (props.disabled)
    return
  const current = props.modelValue || ''
  const inputEl = inputRef.value?.inputElRef
  const start = Number.isInteger(inputEl?.selectionStart) ? inputEl.selectionStart : current.length
  const end = Number.isInteger(inputEl?.selectionEnd) ? inputEl.selectionEnd : start
  const next = `${current.slice(0, start)}${token}${current.slice(end)}`
  emit('update:modelValue', next)
  requestAnimationFrame(() => {
    inputEl?.focus?.()
    inputEl?.setSelectionRange?.(start + token.length, start + token.length)
  })
}

function schedulePreview() {
  clearTimeout(previewTimer)
  previewTimer = setTimeout(runPreview, 320)
}

async function runPreview() {
  const template = props.modelValue || ''
  if (!template) {
    preview.value = null
    emit('preview', null)
    return
  }
  previewing.value = true
  try {
    const res = await previewBusinessDocumentNoRule({
      template,
      suiteCode: props.suiteCode,
      objectCode: props.objectCode,
      sequence: 1,
      sampleData: buildSampleData(),
    })
    preview.value = res.data || null
    emit('preview', preview.value)
  }
  finally {
    previewing.value = false
  }
}

function buildSampleData() {
  return props.fieldOptions.slice(0, 8).reduce((result, field) => {
    result[field.value] = field.label?.split('（')?.[0] || field.value
    return result
  }, {})
}

function normalizeTemplate(value) {
  return String(value || '').replace(/(^|[^\$])\{([^{}]+)\}/g, (match, prefix, token) => {
    const trimmed = String(token || '').trim()
    const seqMatch = trimmed.match(/^seq(\d+)$/)
    const normalized = seqMatch ? `seq:${seqMatch[1]}` : trimmed
    return `${prefix}\${${normalized}}`
  })
}

function fallbackTokens() {
  return [
    { label: '年月日', groupName: '日期时间', insertText: '${yyyyMMdd}' },
    { label: '四位流水号', groupName: '序列', insertText: '${seq:4}' },
    { label: '对象编码', groupName: '上下文', insertText: '${objectCode}' },
    { label: '单据字段', groupName: '业务字段', insertText: '${field:<fieldCode>}' },
  ]
}
</script>

<style scoped>
.no-rule-editor {
  display: grid;
  gap: 10px;
}

.template-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.token-groups {
  display: grid;
  gap: 8px;
}

.token-groups section {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
}

.token-groups strong,
.preview-strip span {
  color: #64748b;
  font-size: 12px;
  line-height: 28px;
}

.token-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.token-list button {
  height: 28px;
  border: 1px solid #d6dde8;
  border-radius: 6px;
  background: #fff;
  color: #1f2937;
  cursor: pointer;
  font-size: 12px;
  padding: 0 8px;
}

.token-list button:disabled {
  cursor: not-allowed;
  opacity: .48;
}

.preview-strip {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
  padding: 8px 10px;
}

.preview-strip.invalid {
  border-color: #fecaca;
  background: #fef2f2;
}

.preview-strip strong {
  overflow: hidden;
  color: #111827;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issue-list p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
}

.issue-list.error p {
  color: #dc2626;
}

.issue-list.warning p {
  color: #b45309;
}

@media (max-width: 720px) {
  .template-row,
  .token-groups section {
    grid-template-columns: 1fr;
  }
}
</style>
