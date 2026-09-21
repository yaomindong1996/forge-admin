<script setup>
import { NAlert, NSelect, NSpin } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { printTemplates } from '@/api/print'

const props = defineProps({
  config: { type: Object, default: () => ({}) },
  formAsset: { type: Object, default: null },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])
const loading = ref(false)
const error = ref('')
const templates = ref([])
let generation = 0

const policyOptions = [
  { label: '继承应用场景绑定', value: 'INHERIT' },
  { label: '限制为指定模板', value: 'RESTRICT' },
]

const policy = computed(() => String(props.config.printTemplatePolicy || 'INHERIT').toUpperCase())
const selectedIds = computed(() => normalizeIds(props.config.printTemplateIds))
const formRef = computed(() => props.config.formRef && typeof props.config.formRef === 'object' ? props.config.formRef : {})
const applicationId = computed(() => String(props.formAsset?.applicationId || formRef.value.applicationId || '').trim())
const expectedSource = computed(() => {
  const mode = String(props.config.formMode || props.formAsset?.formMode || formRef.value.formMode || formRef.value.type || '').toUpperCase()
  return {
    sourceType: mode === 'BUSINESS_CODE_FORM' ? 'CODE' : 'LOWCODE',
    objectCode: String(props.formAsset?.objectCode || formRef.value.objectCode || '').trim(),
    pageId: String(props.formAsset?.pageId || formRef.value.pageId || '').trim(),
    formKey: String(props.config.formKey || props.formAsset?.formKey || formRef.value.formKey || '').trim(),
  }
})
const templateOptions = computed(() => {
  const options = templates.value
    .filter(matchesSource)
    .map(item => ({ label: `${item.templateName}（${item.templateCode}）`, value: String(item.id) }))
  const known = new Set(options.map(item => item.value))
  selectedIds.value.forEach((value) => {
    if (!known.has(value))
      options.push({ label: `模板 ${value}`, value })
  })
  return options
})

watch(applicationId, loadTemplates, { immediate: true })

function normalizeIds(value) {
  const values = Array.isArray(value) ? value : String(value || '').split(',')
  return [...new Set(values.map(item => String(item).trim()).filter(item => /^[1-9]\d*$/.test(item)))]
}

function matchesSource(item = {}) {
  const source = item.source || item
  if (String(source.sourceType || '').toUpperCase() !== expectedSource.value.sourceType)
    return false
  if (expectedSource.value.objectCode && String(source.objectCode || '') !== expectedSource.value.objectCode)
    return false
  if (expectedSource.value.sourceType === 'CODE')
    return !expectedSource.value.formKey || String(source.formKey || '') === expectedSource.value.formKey
  return !expectedSource.value.pageId || String(source.pageId || '') === expectedSource.value.pageId
}

async function loadTemplates(value) {
  const current = ++generation
  templates.value = []
  error.value = ''
  if (!value)
    return
  loading.value = true
  try {
    const response = await printTemplates({ applicationId: value, pageNum: 1, pageSize: 100 })
    if (current === generation)
      templates.value = response.data?.records || []
  }
  catch (reason) {
    if (current === generation)
      error.value = reason?.message || '打印模板列表加载失败'
  }
  finally {
    if (current === generation)
      loading.value = false
  }
}

function updatePolicy(value) {
  emit('update:config', {
    printTemplatePolicy: value,
    printTemplateIds: value === 'RESTRICT' ? selectedIds.value : [],
  })
}

function updateIds(value) {
  emit('update:config', { printTemplateIds: normalizeIds(value) })
}
</script>

<template>
  <div class="print-policy-config">
    <n-form-item label="模板范围">
      <NSelect
        :value="policy"
        :options="policyOptions"
        :disabled="readonly"
        @update:value="updatePolicy"
      />
    </n-form-item>
    <template v-if="policy === 'RESTRICT'">
      <n-form-item label="允许的模板">
        <NSpin :show="loading" size="small">
          <NSelect
            :value="selectedIds"
            :options="templateOptions"
            :disabled="readonly || !applicationId"
            multiple
            filterable
            placeholder="选择该节点允许使用的模板"
            @update:value="updateIds"
          />
        </NSpin>
      </n-form-item>
      <NAlert v-if="!applicationId" type="warning" :show-icon="false">
        当前节点表单缺少应用身份，请先选择已发布应用表单资产。
      </NAlert>
      <NAlert v-else-if="error" type="warning" :show-icon="false">
        {{ error }}；已有模板标识仍会保留。
      </NAlert>
      <NAlert v-else-if="selectedIds.length === 0" type="info" :show-icon="false">
        限制模式未选择模板时，该节点不提供打印模板。
      </NAlert>
    </template>
    <p class="print-policy-hint">
      继承模式使用应用在待办、已办、我发起场景的发布绑定；限制模式只收窄可用模板，不授予额外权限。
    </p>
  </div>
</template>

<style scoped>
.print-policy-config {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.print-policy-hint {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}
</style>
