<template>
  <section class="call-api-config">
    <div class="call-api-config__head">
      <div>
        <strong>调用受管外部接口</strong>
        <p>接口地址、认证和重试由数据源管理员维护，动作只负责映射业务参数。</p>
      </div>
      <n-tag size="small" type="info" :bordered="false">
        EXTERNAL_API
      </n-tag>
    </div>

    <n-alert v-if="catalogError" type="warning" :bordered="false" class="call-api-alert">
      暂时无法加载可用外部接口，请确认数据源权限后重试。
    </n-alert>

    <n-grid :cols="3" :x-gap="12" :y-gap="8" responsive="screen">
      <n-form-item-gi label="受管外部接口">
        <n-select
          :value="config.sourceKey || null"
          :options="sourceOptions"
          :loading="catalogLoading"
          filterable
          clearable
          placeholder="选择已启用的外部接口"
          @update:value="handleSourceChange"
        />
      </n-form-item-gi>
      <n-form-item-gi label="结果取值方式">
        <n-select :value="config.resultMode" :options="resultModeOptions" @update:value="updateConfig({ resultMode: $event })" />
      </n-form-item-gi>
      <n-form-item-gi label="失败处理">
        <n-select
          :value="config.failureStrategy"
          :options="failureStrategyOptions"
          @update:value="handleFailureStrategyChange"
        />
      </n-form-item-gi>
    </n-grid>

    <section class="call-api-section">
      <div class="call-api-section__head">
        <div>
          <strong>请求参数</strong>
          <span>只从当前记录、动作输入、受控上下文或系统身份取值。</span>
        </div>
      </div>
      <n-spin :show="metadataLoading">
        <div v-if="config.paramMappings.length" class="call-api-mapping-list">
          <div v-for="(mapping, index) in config.paramMappings" :key="`${mapping.param}-${index}`" class="call-api-param-row">
            <div class="call-api-param-name">
              <strong>{{ inputParamLabel(mapping.param) }}</strong>
              <span>{{ mapping.param || '未命名参数' }}</span>
            </div>
            <n-select
              :value="mapping.sourceType"
              :options="paramSourceOptions"
              size="small"
              @update:value="patchParam(index, { sourceType: $event, sourceField: '' })"
            />
            <n-select
              v-if="['record', 'form'].includes(mapping.sourceType)"
              :value="mapping.sourceField || null"
              :options="mapping.sourceType === 'form' ? formFieldOptions : recordFieldOptions"
              filterable
              clearable
              size="small"
              placeholder="选择字段"
              @update:value="patchParam(index, { sourceField: $event || '' })"
            />
            <n-input
              v-else-if="mapping.sourceType === 'static'"
              :value="stringValue(mapping.value)"
              size="small"
              placeholder="固定值"
              @update:value="patchParam(index, { value: $event })"
            />
            <n-select
              v-else-if="mapping.sourceType === 'system'"
              :value="mapping.sourceField || null"
              :options="systemFieldOptions"
              size="small"
              placeholder="选择系统字段"
              @update:value="patchParam(index, { sourceField: $event || '' })"
            />
            <n-input
              v-else
              :value="mapping.sourceField || ''"
              size="small"
              placeholder="如 routeQuery.scene"
              @update:value="patchParam(index, { sourceField: $event })"
            />
          </div>
        </div>
        <n-empty v-else size="small" description="该接口没有声明输入参数" />
      </n-spin>
    </section>

    <section class="call-api-section">
      <div class="call-api-section__head">
        <div>
          <strong>结果映射</strong>
          <span>仅写入明确声明的字段；后续步骤可通过“步骤编码.目标字段”读取步骤上下文。</span>
        </div>
        <n-button size="tiny" secondary @click="addResultMapping">
          添加映射
        </n-button>
      </div>
      <div v-if="config.resultMappings.length" class="call-api-mapping-list">
        <div v-for="(mapping, index) in config.resultMappings" :key="index" class="call-api-result-row">
          <n-select
            :value="mapping.from || null"
            :options="resultFieldOptions"
            filterable
            tag
            size="small"
            placeholder="返回字段路径，留空取整个结果"
            @update:value="patchResult(index, { from: $event || '' })"
          />
          <span class="call-api-arrow">→</span>
          <n-select
            :value="mapping.target"
            :options="resultTargetOptions"
            size="small"
            @update:value="patchResult(index, { target: $event })"
          />
          <n-select
            v-if="mapping.target === 'FORM_DATA'"
            :value="mapping.to || null"
            :options="formFieldOptions"
            filterable
            size="small"
            placeholder="表单字段"
            @update:value="patchResult(index, { to: $event || '' })"
          />
          <n-input
            v-else
            :value="mapping.to || ''"
            size="small"
            placeholder="上下文字段，如 resultCode"
            @update:value="patchResult(index, { to: $event })"
          />
          <n-select
            :value="mapping.whenMissing"
            :options="missingOptions"
            size="small"
            @update:value="patchResult(index, { whenMissing: $event })"
          />
          <n-button text size="tiny" type="error" @click="removeResultMapping(index)">
            删除
          </n-button>
        </div>
      </div>
      <n-empty v-else size="small" description="无结果映射，仍会执行外部接口" />
    </section>

    <n-alert
      v-if="config.failureStrategy === 'LOG_AND_CONTINUE'"
      type="warning"
      :bordered="false"
      class="call-api-alert"
    >
      外围调用失败会记录失败结果并继续后续步骤；不能提供跨系统回滚。
    </n-alert>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getLowcodeQuerySourceCatalog, getLowcodeQuerySourceMetadata } from '@/api/lowcode-query-source'
import {
  filterExternalApiSources,
  normalizeCallApiStepConfig,
  parseCallApiInputSchema,
  syncCallApiParamMappings,
} from './call-api-step-config'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  recordFieldOptions: {
    type: Array,
    default: null,
  },
  formFieldOptions: {
    type: Array,
    default: null,
  },
  contextMode: {
    type: String,
    default: 'ACTION',
  },
})

const emit = defineEmits(['update:modelValue'])
const catalog = ref([])
const metadata = ref(null)
const catalogLoading = ref(false)
const metadataLoading = ref(false)
const catalogError = ref(false)
let metadataRequestId = 0

const config = computed(() => normalizeCallApiStepConfig(props.modelValue))
const recordFieldOptions = computed(() => Array.isArray(props.recordFieldOptions)
  ? props.recordFieldOptions
  : props.fieldOptions)
const formFieldOptions = computed(() => Array.isArray(props.formFieldOptions)
  ? props.formFieldOptions
  : props.fieldOptions)
const sourceOptions = computed(() => filterExternalApiSources(catalog.value).map(item => ({
  label: `${item.sourceName || item.sourceKey}${item.sourceGroup ? ` · ${item.sourceGroup}` : ''}`,
  value: item.sourceKey,
})))
const inputSchema = computed(() => parseCallApiInputSchema(metadata.value?.inputSchemaJson))
const resultFieldOptions = computed(() => (Array.isArray(metadata.value?.fields) ? metadata.value.fields : []).map(field => ({
  label: `${field.label || field.field} · ${field.path || field.field}`,
  value: field.path || field.field,
})))
const paramSourceOptions = computed(() => {
  const options = [
    { label: '当前记录', value: 'record' },
    { label: '动作输入', value: 'form' },
    { label: '受控上下文', value: 'context' },
    { label: '系统身份', value: 'system' },
  ]
  if (props.contextMode === 'ACTION')
    options.push({ label: '固定值', value: 'static' })
  return options
})
const systemFieldOptions = [
  { label: '租户 ID', value: 'tenantId' },
  { label: '用户 ID', value: 'userId' },
  { label: '当前组织 ID', value: 'activeOrgId' },
  { label: '记录 ID', value: 'recordId' },
  { label: '对象编码', value: 'objectCode' },
]
const resultModeOptions = [
  { label: '根对象', value: 'ROOT' },
  { label: '列表首行', value: 'FIRST_ROW' },
]
const failureStrategyOptions = [
  { label: '抛异常并终止', value: 'THROW' },
  { label: '记录错误并继续', value: 'LOG_AND_CONTINUE' },
]
const resultTargetOptions = [
  { label: '步骤上下文', value: 'STEP_CONTEXT' },
  { label: '表单字段', value: 'FORM_DATA' },
]
const missingOptions = [
  { label: '保留旧值', value: 'KEEP' },
  { label: '清空目标', value: 'CLEAR' },
]

onMounted(async () => {
  await loadCatalog()
  await loadMetadata()
})

watch(() => props.modelValue?.sourceKey, (next, previous) => {
  if (next && next !== previous)
    loadMetadata()
}, { immediate: false })

async function loadCatalog() {
  catalogLoading.value = true
  catalogError.value = false
  try {
    const response = await getLowcodeQuerySourceCatalog()
    catalog.value = Array.isArray(response?.data) ? response.data : []
  }
  catch {
    catalog.value = []
    catalogError.value = true
  }
  finally {
    catalogLoading.value = false
  }
}

async function loadMetadata(sourceKeyOverride) {
  const sourceKey = String(sourceKeyOverride || config.value.sourceKey || '').trim()
  if (!sourceKey) {
    metadata.value = null
    return
  }
  const requestId = ++metadataRequestId
  metadataLoading.value = true
  try {
    const response = await getLowcodeQuerySourceMetadata({ sourceType: 'EXTERNAL_API', sourceKey })
    if (requestId !== metadataRequestId)
      return
    metadata.value = response?.data || null
    const synced = syncCallApiParamMappings(config.value, metadata.value, recordFieldOptions.value)
    emit('update:modelValue', synced)
  }
  catch {
    if (requestId === metadataRequestId)
      metadata.value = null
  }
  finally {
    if (requestId === metadataRequestId)
      metadataLoading.value = false
  }
}

function handleSourceChange(sourceKey) {
  metadata.value = null
  updateConfig({
    sourceType: 'EXTERNAL_API',
    sourceKey: sourceKey || '',
    paramMappings: [],
    resultMappings: [],
    resultMode: 'ROOT',
  })
  if (sourceKey)
    loadMetadata(sourceKey)
}

function handleFailureStrategyChange(failureStrategy) {
  updateConfig({ failureStrategy })
}

function updateConfig(patch = {}) {
  emit('update:modelValue', { ...config.value, ...patch })
}

function patchParam(index, patch = {}) {
  const mappings = config.value.paramMappings.map((item, itemIndex) => itemIndex === index
    ? { ...item, ...patch }
    : item)
  updateConfig({ paramMappings: mappings })
}

function addResultMapping() {
  updateConfig({
    resultMappings: [...config.value.resultMappings, {
      from: '',
      to: '',
      target: 'STEP_CONTEXT',
      whenMissing: 'KEEP',
    }],
  })
}

function patchResult(index, patch = {}) {
  const resultMappings = config.value.resultMappings.map((item, itemIndex) => itemIndex === index
    ? { ...item, ...patch }
    : item)
  updateConfig({ resultMappings })
}

function removeResultMapping(index) {
  updateConfig({ resultMappings: config.value.resultMappings.filter((_item, itemIndex) => itemIndex !== index) })
}

function inputParamLabel(param) {
  return inputSchema.value.find(item => item.name === param)?.label || param
}

function stringValue(value) {
  if (value == null)
    return ''
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
</script>

<style scoped>
.call-api-config {
  display: grid;
  gap: 12px;
}

.call-api-config__head,
.call-api-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.call-api-config__head strong,
.call-api-section__head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.call-api-config__head p,
.call-api-section__head span {
  display: block;
  margin: 3px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.call-api-section {
  padding-top: 12px;
  border-top: 1px solid #e4e4e7;
}

.call-api-mapping-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.call-api-param-row,
.call-api-result-row {
  display: grid;
  grid-template-columns: minmax(140px, 0.8fr) minmax(130px, 0.8fr) minmax(180px, 1.2fr);
  gap: 8px;
  align-items: center;
}

.call-api-param-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.call-api-param-name strong {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.call-api-param-name span {
  color: var(--text-tertiary);
  font-size: 11px;
}

.call-api-result-row {
  grid-template-columns: minmax(160px, 1fr) 20px minmax(120px, 0.8fr) minmax(160px, 1fr) 110px auto;
}

.call-api-arrow {
  color: var(--text-tertiary);
  text-align: center;
}

.call-api-alert {
  font-size: 12px;
}

@media (max-width: 880px) {
  .call-api-param-row,
  .call-api-result-row {
    grid-template-columns: 1fr 1fr;
  }

  .call-api-arrow {
    display: none;
  }
}
</style>
