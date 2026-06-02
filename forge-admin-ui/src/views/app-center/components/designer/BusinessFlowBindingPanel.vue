<template>
  <div class="business-flow-binding-panel">
    <div class="flow-head">
      <div>
        <h3>流程与自动化</h3>
        <p>绑定默认流程，配置标题模板和单据字段到流程变量的映射。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary :loading="loading" @click="loadBinding">
          刷新
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveConfig">
          保存流程
        </n-button>
      </n-space>
    </div>

    <div class="flow-body">
      <main class="flow-main">
        <n-spin :show="loading">
          <section class="flow-card">
            <div class="flow-card-head">
              <div>
                <h4>默认流程</h4>
                <p>手动按钮和触发器可以复用同一套流程绑定。</p>
              </div>
              <n-tag :type="form.flowModelKey ? 'success' : 'warning'" :bordered="false">
                {{ form.flowModelKey ? '已绑定' : '待绑定' }}
              </n-tag>
            </div>
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="14" :y-gap="4" responsive="screen">
                <n-form-item-gi label="流程模型">
                  <n-select
                    v-model:value="form.flowModelKey"
                    :options="flowModelOptions"
                    :loading="flowModelsLoading"
                    clearable
                    filterable
                    placeholder="选择已发布流程模型"
                    @update:value="handleFlowChange"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="发起方式">
                  <n-select
                    v-model:value="form.startMode"
                    :options="startModeOptions"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi :span="2" label="流程标题模板">
                  <n-input
                    v-model:value="form.titleTemplate"
                    placeholder="例如：${opportunityName}-流程"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </n-form>
          </section>

          <section class="flow-card">
            <div class="flow-card-head">
              <div>
                <h4>变量映射</h4>
                <p>保存为 formField / flowVariable，发起流程时由后端转换为流程变量。</p>
              </div>
              <n-button size="small" dashed @click="addMapping">
                添加映射
              </n-button>
            </div>
            <div v-if="form.variableMapping.length" class="mapping-list">
              <div v-for="(mapping, index) in form.variableMapping" :key="mapping.clientKey" class="mapping-row">
                <n-select
                  v-model:value="mapping.formField"
                  :options="fieldOptions"
                  clearable
                  filterable
                  placeholder="单据字段"
                  @update:value="value => updateMappingLabel(mapping, value)"
                />
                <span>→</span>
                <n-input
                  v-model:value="mapping.flowVariable"
                  placeholder="流程变量名"
                  @update:value="markDirty"
                />
                <n-button quaternary circle size="small" @click="removeMapping(index)">
                  <template #icon>
                    <n-icon><TrashOutline /></n-icon>
                  </template>
                </n-button>
              </div>
            </div>
            <n-empty v-else description="暂无变量映射" />
          </section>
        </n-spin>
      </main>

      <aside class="flow-side">
        <section>
          <h4>运行摘要</h4>
          <div class="flow-facts">
            <div>
              <span>流程模型</span>
              <strong>{{ form.flowModelName || form.flowModelKey || '-' }}</strong>
            </div>
            <div>
              <span>发起方式</span>
              <strong>{{ startModeLabel }}</strong>
            </div>
            <div>
              <span>变量映射</span>
              <strong>{{ validMappingCount }} 项</strong>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessFlowBinding, saveBusinessFlowBinding } from '@/api/business-app'
import flowApi from '@/api/flow'

const props = defineProps({
  objectCode: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  initialBinding: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['dirtyChange', 'saved', 'loaded'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const flowModelsLoading = ref(false)
const flowModelOptions = ref([])
const form = reactive(createDefaultBinding())

const startModeOptions = [
  { label: '手动发起', value: 'MANUAL' },
  { label: '触发器自动发起', value: 'TRIGGER' },
  { label: '手动与触发器', value: 'BOTH' },
]

const fieldOptions = computed(() => props.fields
  .filter(field => fieldCode(field) && !isInactiveField(field))
  .map(field => ({
    label: `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）`,
    value: fieldCode(field),
  })))
const validMappingCount = computed(() => form.variableMapping.filter(item => item.formField && item.flowVariable).length)
const startModeLabel = computed(() => startModeOptions.find(item => item.value === form.startMode)?.label || '-')

watch(() => props.objectCode, () => {
  loadBinding()
}, { immediate: true })

async function loadBinding() {
  if (!props.objectCode) {
    assignBinding(props.initialBinding || createDefaultBinding())
    return
  }
  loading.value = true
  try {
    const [bindingRes] = await Promise.all([
      businessFlowBinding(props.objectCode),
      loadFlowModels(),
    ])
    assignBinding(bindingRes.data || props.initialBinding || createDefaultBinding())
    emit('loaded', { ...form })
    emit('dirtyChange', false)
  }
  finally {
    loading.value = false
  }
}

async function loadFlowModels() {
  flowModelsLoading.value = true
  try {
    const res = await flowApi.getModelList({ status: 1 })
    flowModelOptions.value = (res.data || []).map(model => ({
      label: `${model.modelName || model.name || model.modelKey || model.key}（${model.modelKey || model.key}）`,
      value: model.modelKey || model.key,
      modelName: model.modelName || model.name || model.modelKey || model.key,
    })).filter(item => item.value)
  }
  catch {
    flowModelOptions.value = []
  }
  finally {
    flowModelsLoading.value = false
  }
}

async function saveConfig() {
  if (!props.objectCode)
    return
  if (!form.flowModelKey) {
    message.warning('请选择流程模型')
    return
  }
  saving.value = true
  try {
    await saveBusinessFlowBinding(props.objectCode, buildPayload())
    message.success('流程绑定已保存')
    emit('dirtyChange', false)
    emit('saved', { ...form })
    await loadBinding()
  }
  finally {
    saving.value = false
  }
}

function buildPayload() {
  return {
    flowModelKey: form.flowModelKey || '',
    flowModelName: form.flowModelName || selectedFlowName(),
    titleTemplate: form.titleTemplate || '',
    startMode: form.startMode || 'MANUAL',
    variableMapping: form.variableMapping
      .map(item => ({
        formField: item.formField || '',
        flowVariable: item.flowVariable || '',
        label: item.label || fieldLabel(item.formField),
      }))
      .filter(item => item.formField && item.flowVariable),
    conditionFlows: form.conditionFlows || [],
    options: { ...(form.options || {}) },
  }
}

function assignBinding(value = {}) {
  Object.assign(form, {
    ...createDefaultBinding(),
    ...value,
    startMode: normalizeStartMode(value.startMode),
    variableMapping: normalizeMappings(value.variableMapping || []),
    conditionFlows: Array.isArray(value.conditionFlows) ? value.conditionFlows : [],
    options: { ...(value.options || {}) },
  })
}

function normalizeMappings(list = []) {
  return list.map(item => ({
    clientKey: `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: item.formField || item.field || null,
    flowVariable: item.flowVariable || item.variable || '',
    label: item.label || fieldLabel(item.formField || item.field),
  }))
}

function createDefaultBinding() {
  return {
    flowModelKey: '',
    flowModelName: '',
    titleTemplate: '',
    startMode: 'MANUAL',
    variableMapping: [],
    conditionFlows: [],
    options: {},
  }
}

function handleFlowChange(value) {
  form.flowModelName = flowModelOptions.value.find(item => item.value === value)?.modelName || ''
  markDirty()
}

function addMapping() {
  form.variableMapping.push({
    clientKey: `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: null,
    flowVariable: '',
    label: '',
  })
  markDirty()
}

function removeMapping(index) {
  form.variableMapping.splice(index, 1)
  markDirty()
}

function updateMappingLabel(mapping, value) {
  mapping.label = fieldLabel(value)
  markDirty()
}

function selectedFlowName() {
  return flowModelOptions.value.find(item => item.value === form.flowModelKey)?.modelName || form.flowModelName || ''
}

function fieldLabel(code) {
  if (!code)
    return ''
  const option = fieldOptions.value.find(item => item.value === code)
  return option?.label || code
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || ''
}

function normalizeStartMode(value) {
  const normalized = String(value || 'MANUAL').toUpperCase()
  return startModeOptions.some(item => item.value === normalized) ? normalized : 'MANUAL'
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveConfig,
  loadBinding,
})
</script>

<style scoped>
.business-flow-binding-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.flow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.flow-head h3,
.flow-card h4,
.flow-side h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.flow-head p,
.flow-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.flow-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.flow-main {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.flow-card,
.flow-side section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.flow-card + .flow-card {
  margin-top: 12px;
}

.flow-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.mapping-list {
  display: grid;
  gap: 8px;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 24px minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.mapping-row span,
.flow-facts span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

.flow-side {
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.flow-facts {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.flow-facts div {
  min-width: 0;
  border-radius: 6px;
  background: #f1f5f9;
  padding: 10px;
}

.flow-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .flow-body {
    grid-template-columns: 1fr;
  }

  .flow-side {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }

  .mapping-row {
    grid-template-columns: 1fr;
  }
}
</style>
