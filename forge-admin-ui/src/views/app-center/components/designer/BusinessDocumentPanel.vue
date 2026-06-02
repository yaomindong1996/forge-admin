<template>
  <div class="business-document-panel">
    <div class="document-head">
      <div>
        <h3>单据设置</h3>
        <p>配置当前对象的单据生命周期、编号和责任字段。</p>
      </div>
      <n-space align="center" size="small">
        <n-tag :type="form.documentEnabled ? 'success' : 'default'" :bordered="false">
          {{ form.documentEnabled ? '已启用' : '未启用' }}
        </n-tag>
        <n-button size="small" secondary :loading="loading" @click="loadConfig">
          刷新
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveConfig">
          保存单据
        </n-button>
      </n-space>
    </div>

    <div class="document-body">
      <main class="document-main">
        <n-spin :show="loading">
          <section class="document-card">
            <div class="document-card-head">
              <div>
                <h4>单据模式</h4>
                <p>未启用时对象仍按普通 CRUD 运行。</p>
              </div>
              <n-switch v-model:value="form.documentEnabled" @update:value="markDirty" />
            </div>

            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="14" :y-gap="4" responsive="screen">
                <n-form-item-gi label="单据名称">
                  <n-input
                    v-model:value="form.documentName"
                    :disabled="!form.documentEnabled"
                    :placeholder="defaultDocumentName"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="编号规则">
                  <n-input
                    v-model:value="form.documentNoRule"
                    :disabled="!form.documentEnabled"
                    placeholder="例如：OPP-${yyyyMMdd}-${seq}"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="状态字段">
                  <n-select
                    v-model:value="form.statusField"
                    :disabled="!form.documentEnabled"
                    :options="fieldOptions"
                    clearable
                    filterable
                    placeholder="选择单据状态字段"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="默认流程">
                  <n-select
                    v-model:value="form.defaultFlowKey"
                    :disabled="!form.documentEnabled"
                    :options="flowModelOptions"
                    :loading="flowModelsLoading"
                    clearable
                    filterable
                    placeholder="选择已发布流程"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="发起人字段">
                  <n-select
                    v-model:value="form.starterField"
                    :disabled="!form.documentEnabled"
                    :options="fieldOptions"
                    clearable
                    filterable
                    placeholder="选择记录发起人字段"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="负责人字段">
                  <n-select
                    v-model:value="form.ownerField"
                    :disabled="!form.documentEnabled"
                    :options="fieldOptions"
                    clearable
                    filterable
                    placeholder="选择负责人字段"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </n-form>
          </section>

          <section class="document-card">
            <div class="document-card-head">
              <div>
                <h4>状态映射</h4>
                <p>把标准生命周期映射到当前对象字段值。</p>
              </div>
            </div>
            <div class="status-grid">
              <div v-for="status in statusDefinitions" :key="status.key" class="status-row">
                <span>{{ status.label }}</span>
                <n-input
                  v-model:value="form.statusMapping[status.key]"
                  :disabled="!form.documentEnabled"
                  :placeholder="status.defaultValue"
                  size="small"
                  @update:value="markDirty"
                />
              </div>
            </div>
          </section>
        </n-spin>
      </main>

      <aside class="document-side">
        <section>
          <h4>发布关注</h4>
          <div class="document-facts">
            <div>
              <span>状态字段</span>
              <strong>{{ form.statusField || '-' }}</strong>
            </div>
            <div>
              <span>默认流程</span>
              <strong>{{ form.defaultFlowKey || '-' }}</strong>
            </div>
            <div>
              <span>发起人</span>
              <strong>{{ form.starterField || '-' }}</strong>
            </div>
            <div>
              <span>负责人</span>
              <strong>{{ form.ownerField || '-' }}</strong>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessDocumentConfig, saveBusinessDocumentConfig } from '@/api/business-app'
import flowApi from '@/api/flow'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  objectName: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  initialConfig: {
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

const statusDefinitions = [
  { key: 'DRAFT', label: '草稿', defaultValue: 'DRAFT' },
  { key: 'SUBMITTED', label: '已提交', defaultValue: 'SUBMITTED' },
  { key: 'IN_PROCESS', label: '流程中', defaultValue: 'IN_PROCESS' },
  { key: 'APPROVED', label: '已通过', defaultValue: 'APPROVED' },
  { key: 'REJECTED', label: '已驳回', defaultValue: 'REJECTED' },
  { key: 'CANCELED', label: '已撤回', defaultValue: 'CANCELED' },
  { key: 'CLOSED', label: '已关闭', defaultValue: 'CLOSED' },
]

const form = reactive(createDefaultConfig())

const defaultDocumentName = computed(() => `${props.objectName || '业务对象'}单据`)
const fieldOptions = computed(() => props.fields
  .filter(field => fieldCode(field) && !isInactiveField(field))
  .map(field => ({
    label: `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）`,
    value: fieldCode(field),
  })))

watch(() => props.objectId, () => {
  loadConfig()
}, { immediate: true })

watch(() => props.initialConfig, (value) => {
  if (!props.objectId && value)
    assignConfig(value)
}, { deep: true })

async function loadConfig() {
  if (!props.objectId) {
    assignConfig(props.initialConfig || createDefaultConfig())
    return
  }
  loading.value = true
  try {
    const [configRes] = await Promise.all([
      businessDocumentConfig(props.objectId),
      loadFlowModels(),
    ])
    assignConfig(configRes.data || props.initialConfig || createDefaultConfig())
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
  if (!props.objectId)
    return
  if (form.documentEnabled && !form.statusField) {
    message.warning('启用单据模式后必须选择状态字段')
    return
  }
  saving.value = true
  try {
    await saveBusinessDocumentConfig(props.objectId, buildPayload())
    message.success('单据设置已保存')
    emit('dirtyChange', false)
    emit('saved', { ...form })
    await loadConfig()
  }
  finally {
    saving.value = false
  }
}

function buildPayload() {
  return {
    documentEnabled: !!form.documentEnabled,
    documentName: form.documentName || defaultDocumentName.value,
    documentNoRule: form.documentNoRule || '',
    statusField: form.statusField || '',
    starterField: form.starterField || '',
    ownerField: form.ownerField || '',
    defaultFlowKey: form.defaultFlowKey || '',
    statusMapping: normalizeStatusMapping(form.statusMapping),
    options: { ...(form.options || {}) },
  }
}

function assignConfig(value = {}) {
  Object.assign(form, {
    ...createDefaultConfig(),
    ...value,
    documentEnabled: readBoolean(value.documentEnabled, false),
    statusMapping: normalizeStatusMapping(value.statusMapping || {}),
    options: { ...(value.options || {}) },
  })
}

function normalizeStatusMapping(mapping = {}) {
  return statusDefinitions.reduce((result, item) => {
    result[item.key] = mapping[item.key] || item.defaultValue
    return result
  }, {})
}

function createDefaultConfig() {
  return {
    documentEnabled: false,
    documentName: '',
    documentNoRule: '',
    statusField: '',
    starterField: '',
    ownerField: '',
    defaultFlowKey: '',
    statusMapping: normalizeStatusMapping({}),
    options: {},
  }
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || ''
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function readBoolean(value, defaultValue = false) {
  if (value === null || value === undefined)
    return defaultValue
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  return ['true', '1', 'yes'].includes(String(value).trim().toLowerCase())
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveConfig,
  loadConfig,
})
</script>

<style scoped>
.business-document-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.document-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.document-head h3,
.document-card h4,
.document-side h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.document-head p,
.document-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.document-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.document-main {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.document-card,
.document-side section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.document-card + .document-card {
  margin-top: 12px;
}

.document-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.status-row {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.status-row span,
.document-facts span {
  color: #64748b;
  font-size: 12px;
}

.document-side {
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.document-facts {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.document-facts div {
  min-width: 0;
  border-radius: 6px;
  background: #f1f5f9;
  padding: 10px;
}

.document-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .document-body {
    grid-template-columns: 1fr;
  }

  .document-side {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>
