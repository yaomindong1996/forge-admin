<template>
  <div class="flow-binding-bar">
    <n-form-item label="流程模型" label-placement="left" :show-feedback="false" class="binding-field">
      <n-select
        v-model:value="form.flowModelKey"
        :options="flowModelOptions"
        :loading="loading"
        filterable
        clearable
        placeholder="选择已发布的审批流程"
        @update:value="handleModelChange"
      />
    </n-form-item>
    <n-form-item label="启动方式" label-placement="left" :show-feedback="false" class="binding-field">
      <n-select v-model:value="form.startMode" :options="startModeOptions" :disabled="!form.flowModelKey" />
    </n-form-item>
    <div class="binding-actions">
      <n-button size="small" type="primary" secondary :loading="saving" :disabled="!dirty" @click="saveBinding">
        保存绑定
      </n-button>
      <n-button size="small" quaternary title="变量映射、业务绑定等完整流程绑定设置" @click="emit('openAdvanced')">
        完整绑定设置
      </n-button>
    </div>
  </div>
  <p v-if="bindingHint" class="binding-hint" :class="{ 'is-warning': !form.flowModelKey }">
    {{ bindingHint }}
  </p>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessFlowBinding, saveBusinessFlowBinding } from '@/api/business-app'
import flowApi from '@/api/flow'

const props = defineProps({
  objectCode: {
    type: String,
    default: '',
  },
  objectId: {
    type: [Number, String],
    default: null,
  },
})

const emit = defineEmits(['saved', 'loaded', 'openAdvanced'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const flowModelOptions = ref([])
const binding = ref({})
const form = reactive({
  flowModelKey: '',
  flowModelName: '',
  startMode: 'MANUAL',
})
const startModeOptions = [
  { label: '用户点击按钮', value: 'MANUAL' },
  { label: '触发器自动发起', value: 'TRIGGER' },
  { label: '按钮和触发器都可以', value: 'BOTH' },
]

const dirty = computed(() => form.flowModelKey !== (binding.value.flowModelKey || '')
  || form.startMode !== normalizeStartMode(binding.value.startMode))
const bindingHint = computed(() => {
  if (!form.flowModelKey)
    return '尚未绑定流程模型；绑定后节点配置将自动读取审批节点。'
  return `当前绑定：${form.flowModelName || form.flowModelKey} · ${startModeLabel(form.startMode)}`
})

watch(() => props.objectCode, (code) => {
  if (code)
    loadBinding()
}, { immediate: true })

async function loadBinding() {
  loading.value = true
  try {
    const [bindingRes, modelRes] = await Promise.all([
      businessFlowBinding(props.objectCode),
      loadFlowModels(),
    ])
    binding.value = bindingRes.data || {}
    form.flowModelKey = binding.value.flowModelKey || ''
    form.flowModelName = binding.value.flowModelName || ''
    form.startMode = normalizeStartMode(binding.value.startMode)
    emit('loaded', { ...binding.value })
  }
  catch (error) {
    binding.value = {}
    message.warning(error?.message || '流程绑定信息加载失败')
  }
  finally {
    loading.value = false
  }
}

async function loadFlowModels() {
  try {
    const res = await flowApi.getModelList({ status: 1 })
    flowModelOptions.value = (res.data || []).map(model => ({
      label: `${model.modelName || model.name || model.modelKey || model.key}（${model.modelKey || model.key}）`,
      value: model.modelKey || model.key,
    })).filter(item => item.value)
  }
  catch {
    flowModelOptions.value = []
  }
}

function handleModelChange(value) {
  form.flowModelKey = value || ''
  form.flowModelName = flowModelOptions.value.find(item => item.value === value)?.label || ''
  if (!value)
    form.startMode = 'MANUAL'
}

async function saveBinding() {
  if (!props.objectCode || saving.value)
    return
  if (!form.flowModelKey) {
    message.warning('请选择流程模型')
    return
  }
  saving.value = true
  try {
    // 与 BusinessFlowBindingPanel.buildPayload 保持同一协议：未在本面板编辑的字段原样回传。
    const payload = {
      flowModelKey: form.flowModelKey,
      flowModelName: form.flowModelName || binding.value.flowModelName || '',
      titleTemplate: binding.value.titleTemplate || '',
      startMode: form.startMode,
      variableMapping: binding.value.variableMapping || [],
      businessBinding: binding.value.businessBinding || {},
      nodeForms: [],
      conditionFlows: binding.value.conditionFlows || [],
      options: binding.value.options || {},
    }
    await saveBusinessFlowBinding(props.objectCode, payload)
    binding.value = { ...binding.value, ...payload }
    message.success('流程绑定已保存')
    emit('saved', { ...binding.value })
  }
  catch (error) {
    message.error(error?.message || '流程绑定保存失败')
  }
  finally {
    saving.value = false
  }
}

function normalizeStartMode(value) {
  return ['MANUAL', 'TRIGGER', 'BOTH'].includes(value) ? value : 'MANUAL'
}

function startModeLabel(value) {
  return startModeOptions.find(item => item.value === value)?.label || '用户点击按钮'
}
</script>

<style scoped>
.flow-binding-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 22px;
}

.binding-field {
  margin: 0;
}

.binding-field :deep(.n-select) {
  width: min(320px, 42vw);
}

.binding-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.binding-hint {
  margin: 8px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.binding-hint.is-warning {
  color: #bf8700;
}
</style>
