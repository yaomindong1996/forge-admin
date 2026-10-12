<template>
  <section class="field-event-editor">
    <div class="field-event-editor__head">
      <div>
        <strong>字段自动查询</strong>
        <p>字段值变化后，自动查找匹配信息并带回到表单。</p>
      </div>
      <n-button size="tiny" type="primary" secondary @click="openCreate">
        添加查询
      </n-button>
    </div>

    <div v-if="normalizedRules.length" class="field-event-list">
      <article v-for="(rule, index) in normalizedRules" :key="rule.id || index" class="field-event-card">
        <div class="field-event-card__main">
          <div class="field-event-card__title">
            <span>{{ rule.name || '未命名查询' }}</span>
            <n-tag size="tiny" :type="rule.enabled === false ? 'default' : 'success'">
              {{ rule.enabled === false ? '停用' : triggerLabel(rule.trigger) }}
            </n-tag>
          </div>
          <p>{{ fieldLabel(rule.sourceField) }} · {{ sourceLabel(rule) }}</p>
          <span>{{ rule.paramMappings?.length || 0 }} 个参数，{{ rule.resultMappings?.length || 0 }} 个回填字段</span>
        </div>
        <div class="field-event-card__actions">
          <n-button text size="tiny" type="primary" @click="openEdit(index)">
            编辑
          </n-button>
          <n-dropdown
            trigger="click"
            :options="ruleActionOptions(index, rule)"
            @select="handleRuleAction"
          >
            <n-button text size="tiny">
              更多
            </n-button>
          </n-dropdown>
        </div>
      </article>
    </div>
    <n-empty v-else size="small" description="还没有字段查询规则" />

    <n-modal
      v-model:show="modalVisible"
      preset="card"
      :title="editingIndex < 0 ? '添加字段自动查询' : '编辑字段自动查询'"
      class="field-event-modal"
      style="width: min(920px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <div class="field-event-form">
        <section class="field-event-form__section">
          <h4>什么时候查、查什么</h4>
          <div class="field-event-form__grid">
            <n-form-item label="这条查询叫什么" required>
              <n-input v-model:value="draft.name" maxlength="80" placeholder="例如：输入手机号后带回联系人" />
            </n-form-item>
            <n-form-item label="什么时候查询" required>
              <n-select v-model:value="draft.trigger" :options="triggerOptions" @update:value="handleTriggerChange" />
            </n-form-item>
            <n-form-item v-if="draft.trigger !== 'FORM_LOAD'" label="哪个字段触发" required>
              <n-select v-model:value="draft.sourceField" :options="fieldOptions" filterable placeholder="选择表单字段" />
            </n-form-item>
            <n-form-item label="从哪里查" required>
              <div class="query-source-picker">
                <n-radio-group
                  v-model:value="sourceTypeTab"
                  class="query-source-tabs"
                  size="small"
                  @update:value="handleSourceTypeChange"
                >
                  <n-radio-button value="DATASET">
                    数据集（{{ sourceCount('DATASET') }}）
                  </n-radio-button>
                  <n-radio-button value="BUSINESS_OBJECT">
                    业务对象（{{ sourceCount('BUSINESS_OBJECT') }}）
                  </n-radio-button>
                  <n-radio-button value="EXTERNAL_API">
                    接口（{{ sourceCount('EXTERNAL_API') }}）
                  </n-radio-button>
                </n-radio-group>
                <n-select
                  :value="selectedSourceValue"
                  :options="sourceOptions"
                  :loading="catalogLoading"
                  filterable
                  placeholder="选择一个已开放的查询源"
                  @update:value="handleSourceChange"
                />
                <small class="field-event-help">数据集适合查表格数据，业务对象适合查平台内的业务单据，接口适合调用已登记的业务服务。</small>
              </div>
            </n-form-item>
          </div>
          <div v-if="isListQuerySourceType && draft.sourceKey" class="dataset-query-settings">
            <div>
              <strong>分页限制</strong>
              <span>只取前面一小页数据，避免一次返回过多记录。</span>
            </div>
            <label>
              <span>每次最多返回</span>
              <n-input-number v-model:value="draft.pageSize" :min="1" :max="100" :step="10" size="small" />
              <em>条</em>
            </label>
          </div>
          <div class="field-event-form__switches">
            <label><span>启用规则</span><n-switch v-model:value="draft.enabled" size="small" /></label>
            <label><span>空值时跳过</span><n-switch v-model:value="draft.skipWhenEmpty" size="small" /></label>
            <label><span>查询前清空旧结果</span><n-switch v-model:value="draft.clearTargetsOnTrigger" size="small" /></label>
            <label v-if="draft.trigger === 'CHANGE'">
              <span>防抖毫秒</span>
              <n-input-number v-model:value="draft.debounceMs" :min="0" :max="5000" :step="100" size="small" />
            </label>
          </div>
        </section>

        <section class="field-event-form__section">
          <div class="field-event-form__section-head">
            <div>
              <h4>查询需要哪些信息</h4>
              <p>为每个参数选择取值来源：表单字段、登录上下文，或直接输入地址参数名；必填参数取值为空时自动跳过本次查询。</p>
            </div>
            <n-button size="tiny" secondary @click="addParamMapping">
              添加参数
            </n-button>
          </div>
          <n-alert v-if="!draft.sourceKey" type="info" :bordered="false" :show-icon="false" class="param-empty-alert">
            请先在上一步选择查询源，参数列表会根据查询源定义自动生成。
          </n-alert>
          <n-spin v-else :show="metadataLoading">
            <div v-if="draft.paramMappings.length" class="mapping-list">
              <div v-for="(mapping, index) in draft.paramMappings" :key="`${mapping.param}_${index}`" class="param-mapping-card">
                <div class="param-mapping-card__head">
                  <template v-if="inputParamMeta(mapping.param)">
                    <div class="param-mapping-card__meta">
                      <span class="param-mapping-card__label">
                        <span v-if="inputParamMeta(mapping.param).required" class="param-required-star">*</span>
                        {{ inputParamMeta(mapping.param).label }}
                      </span>
                      <span class="param-mapping-card__code">{{ mapping.param }}<template v-if="inputParamMeta(mapping.param).type && inputParamMeta(mapping.param).type !== 'string'"> · {{ inputParamMeta(mapping.param).type }}</template></span>
                    </div>
                  </template>
                  <n-select
                    v-else
                    v-model:value="mapping.param"
                    :options="paramOptions"
                    filterable
                    tag
                    size="small"
                    class="param-mapping-card__param-input"
                    placeholder="输入或选择参数名"
                  />
                  <div class="param-mapping-card__flags">
                    <n-switch
                      :value="mapping.required === true"
                      size="small"
                      @update:value="mapping.required = $event"
                    >
                      <template #checked>
                        必填
                      </template>
                      <template #unchecked>
                        选填
                      </template>
                    </n-switch>
                    <n-button text size="tiny" type="error" @click="removeParamMapping(index)">
                      删除
                    </n-button>
                  </div>
                </div>
                <div class="param-mapping-card__value">
                  <n-select
                    :value="mappingValueText(mapping)"
                    :options="paramValueOptions"
                    filterable
                    tag
                    clearable
                    size="small"
                    placeholder="选择表单字段 / 登录上下文，或输入地址参数名"
                    @update:value="handleMappingValueChange(mapping, $event)"
                  />
                  <n-input
                    v-if="isCustomContextMapping(mapping)"
                    v-model:value="mapping.path"
                    size="small"
                    placeholder="填写上下文路径，例如 currentUser.staffId"
                  />
                  <small v-if="mapping.source === 'ROUTE_QUERY' && mapping.path && !isSafePath(mapping.path)" class="param-mapping-card__error">
                    地址参数名只能包含字母、数字、点号、下划线和中划线
                  </small>
                </div>
              </div>
            </div>
            <n-empty v-else size="small" :description="paramEmptyText" />
          </n-spin>
        </section>

        <section class="field-event-form__section">
          <div class="field-event-form__section-head">
            <div>
              <h4>结果回填</h4>
              <p>只会写入这里显式选择的目标字段。</p>
            </div>
            <n-button size="tiny" secondary @click="addResultMapping">
              添加回填
            </n-button>
          </div>
          <n-form-item label="结果取值方式" class="result-mode-field">
            <n-radio-group v-model:value="draft.resultMode" size="small">
              <n-radio-button value="ROOT">
                根对象
              </n-radio-button>
              <n-radio-button value="FIRST_ROW">
                列表首行
              </n-radio-button>
            </n-radio-group>
            <small class="field-event-help">数据集、业务对象返回列表，通常选「列表首行」。</small>
          </n-form-item>
          <div v-if="draft.resultMappings.length" class="mapping-list">
            <div v-for="(mapping, index) in draft.resultMappings" :key="index" class="result-mapping-card">
              <div class="result-mapping-card__cell">
                <label>返回字段</label>
                <n-select
                  v-model:value="mapping.from"
                  :options="resultFieldOptions"
                  filterable
                  tag
                  size="small"
                  placeholder="选择或输入返回字段"
                />
              </div>
              <span class="mapping-arrow">→</span>
              <div class="result-mapping-card__cell">
                <label>回填到</label>
                <n-select v-model:value="mapping.to" :options="fieldOptions" filterable size="small" placeholder="选择表单字段" />
              </div>
              <div class="result-mapping-card__cell result-mapping-card__cell--behavior">
                <label>未找到时</label>
                <n-select v-model:value="mapping.whenMissing" :options="missingOptions" size="small" />
              </div>
              <n-button text size="tiny" type="error" class="result-mapping-card__remove" @click="removeResultMapping(index)">
                删除
              </n-button>
            </div>
          </div>
          <n-empty v-else size="small" description="至少添加一个回填字段" />
        </section>

        <section class="field-event-form__section">
          <h4>用户反馈</h4>
          <div class="field-event-form__grid">
            <n-form-item label="未找到提示">
              <n-input v-model:value="draft.notFoundMessage" maxlength="200" />
            </n-form-item>
            <n-form-item label="查询失败提示">
              <n-input v-model:value="draft.errorMessage" maxlength="200" />
            </n-form-item>
          </div>
        </section>
        <n-alert v-if="validationMessage" type="warning" :show-icon="false">
          {{ validationMessage }}
        </n-alert>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">
            取消
          </n-button>
          <n-button type="primary" @click="saveDraft">
            保存规则
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getLowcodeQuerySourceCatalog, getLowcodeQuerySourceMetadata } from '@/api/lowcode-query-source'
import { isSafePath } from '@/components/ai-form/data-source-binding-runtime'
import { parseQuerySourceInputSchema } from '@/components/ai-form/query-source-schema'
import {
  CUSTOM_PATH_OPTION,
  FIELD_EVENT_CONTEXT_PATH_OPTIONS,
  withCustomPathOption,
} from '@/components/ai-form/runtime-context-options'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])
const modalVisible = ref(false)
const editingIndex = ref(-1)
const draft = ref(createDraft())
const sourceTypeTab = ref('DATASET')
const catalog = ref([])
const metadata = ref(null)
const catalogLoading = ref(false)
const metadataLoading = ref(false)
const validationMessage = ref('')

const triggerOptions = [
  { label: '表单打开后', value: 'FORM_LOAD' },
  { label: '字段值变化', value: 'CHANGE' },
  { label: '字段失去焦点', value: 'BLUR' },
  { label: '点击查询按钮', value: 'MANUAL' },
  { label: '扫码或按 Enter', value: 'SCAN_COMPLETE' },
]
const missingOptions = [
  { label: '清空旧值', value: 'CLEAR' },
  { label: '保留旧值', value: 'KEEP' },
]
// 自定义上下文路径 eg的下拉标识值（选项值加 ctx: 前缀，避免与地址参数手动输入混淆）
const CUSTOM_CTX_VALUE = `ctx:${CUSTOM_PATH_OPTION}`

const normalizedRules = computed(() => Array.isArray(props.modelValue) ? props.modelValue : [])
const sourceOptions = computed(() => catalog.value
  .filter(item => item.sourceType === sourceTypeTab.value)
  .map(item => ({
    label: `${item.sourceName || item.sourceKey}${item.sourceGroup ? ` · ${item.sourceGroup}` : ''}`,
    value: `${item.sourceType}::${item.sourceKey}`,
  })))
const selectedSourceValue = computed(() => draft.value.sourceType && draft.value.sourceKey
  ? `${draft.value.sourceType}::${draft.value.sourceKey}`
  : '')
const inputSchema = computed(() => parseQuerySourceInputSchema(metadata.value?.inputSchemaJson))
const sourceFields = computed(() => ensureBusinessObjectIdField(
  Array.isArray(metadata.value?.fields) ? metadata.value.fields : [],
  draft.value.sourceType,
))
const resultFieldOptions = computed(() => sourceFields.value.map(field => ({
  label: `${field.label || field.field} · ${field.path || field.field}`,
  value: field.path || field.field,
})))
const isListQuerySourceType = computed(() => ['DATASET', 'BUSINESS_OBJECT'].includes(draft.value.sourceType))
// 参数取值下拉：字段引用 / 登录上下文分组展示，未匹配项（地址参数名）走 tag 手动输入
const paramValueOptions = computed(() => [
  {
    type: 'group',
    label: '表单字段',
    key: 'fields',
    children: props.fieldOptions.map(option => ({
      label: `${option.label}（${option.value}）`,
      value: `field:${option.value}`,
    })),
  },
  {
    type: 'group',
    label: '登录上下文',
    key: 'context',
    children: withCustomPathOption(FIELD_EVENT_CONTEXT_PATH_OPTIONS).map(option => ({
      label: `${option.label}（${option.value}）`,
      value: `ctx:${option.value}`,
    })),
  },
])
const paramOptions = computed(() => {
  const options = inputSchema.value.map(item => ({ label: item.label || item.name, value: item.name }))
  const known = new Set(options.map(item => item.value))
  for (const field of sourceFields.value) {
    const name = field.path || field.field
    if (name && !known.has(name))
      options.push({ label: `${field.label || name} · ${name}`, value: name })
  }
  return options
})
// 参数区空态文案：区分「未选查询源」「未声明参数」两种场景，避免用户误以为功能失效
const paramEmptyText = computed(() => {
  if (!draft.value.sourceKey)
    return '请先选择查询源'
  if (!inputSchema.value.length)
    return '该查询源未声明查询参数，可点右上角「添加参数」手动添加'
  return '暂无参数'
})

onMounted(loadCatalog)

function createDraft(source = {}) {
  return {
    id: source.id || `field_query_${Date.now()}`,
    name: source.name || '',
    enabled: source.enabled !== false,
    trigger: source.trigger || 'BLUR',
    sourceField: source.sourceField || '',
    sourceType: source.sourceType || '',
    sourceKey: source.sourceKey || '',
    pageNum: Number.isInteger(Number(source.pageNum)) && Number(source.pageNum) > 0 ? Number(source.pageNum) : 1,
    pageSize: Number.isInteger(Number(source.pageSize)) && Number(source.pageSize) > 0 ? Math.min(Number(source.pageSize), 100) : 20,
    debounceMs: Number.isInteger(Number(source.debounceMs)) ? Number(source.debounceMs) : 300,
    skipWhenEmpty: source.skipWhenEmpty !== false,
    clearTargetsOnTrigger: source.clearTargetsOnTrigger === true,
    paramMappings: clone(source.paramMappings || []),
    resultMode: source.resultMode || 'ROOT',
    resultMappings: clone(source.resultMappings || []),
    notFoundMessage: source.notFoundMessage || '未匹配到数据',
    errorMessage: source.errorMessage || '查询失败，请重试',
    errorMode: source.errorMode || 'MESSAGE',
  }
}

async function loadCatalog() {
  catalogLoading.value = true
  try {
    const response = await getLowcodeQuerySourceCatalog()
    catalog.value = Array.isArray(response?.data) ? response.data : []
  }
  catch {
    catalog.value = []
  }
  finally {
    catalogLoading.value = false
  }
}

async function loadMetadata() {
  if (!draft.value.sourceType || !draft.value.sourceKey) {
    metadata.value = null
    return
  }
  metadataLoading.value = true
  try {
    const response = await getLowcodeQuerySourceMetadata({
      sourceType: draft.value.sourceType,
      sourceKey: draft.value.sourceKey,
    })
    metadata.value = response?.data || null
    syncParamMappings()
    if (draft.value.resultMode === 'ROOT' && isListQuerySourceType.value)
      draft.value.resultMode = 'FIRST_ROW'
  }
  catch {
    metadata.value = null
  }
  finally {
    metadataLoading.value = false
  }
}

function syncParamMappings() {
  const current = new Map(draft.value.paramMappings.map(item => [item.param, item]))
  const synced = inputSchema.value.map((item) => {
    const existing = current.get(item.name)
    if (existing) {
      // schema 侧标记必填但旧配置缺失时补齐，保证必填短路校验生效
      if (item.required && existing.required !== true)
        return { ...existing, required: true }
      return existing
    }
    const sameField = props.fieldOptions.find(option => option.value === item.name)
    const mapping = {
      param: item.name,
      source: 'FORM_FIELD',
      field: sameField?.value || draft.value.sourceField || '',
    }
    if (item.required)
      mapping.required = true
    return mapping
  })
  const known = new Set(inputSchema.value.map(item => item.name))
  for (const mapping of draft.value.paramMappings) {
    // 手动添加的参数（例如业务对象的自由参数）不在参数 schema 中，需保留
    if (mapping.param && !known.has(mapping.param) && !synced.some(item => item.param === mapping.param))
      synced.push(mapping)
  }
  draft.value.paramMappings = synced
}

function addParamMapping() {
  draft.value.paramMappings.push({ param: '', source: 'FORM_FIELD', field: '' })
}

function removeParamMapping(index) {
  draft.value.paramMappings.splice(index, 1)
}

function openCreate() {
  editingIndex.value = -1
  draft.value = createDraft()
  sourceTypeTab.value = preferredSourceType()
  draft.value.sourceType = sourceTypeTab.value
  metadata.value = null
  validationMessage.value = ''
  modalVisible.value = true
  if (!catalog.value.length)
    loadCatalog()
}

function openEdit(index) {
  editingIndex.value = index
  draft.value = createDraft(normalizedRules.value[index])
  sourceTypeTab.value = draft.value.sourceType || preferredSourceType()
  validationMessage.value = ''
  modalVisible.value = true
  loadMetadata()
}

function handleTriggerChange(trigger) {
  if (trigger === 'FORM_LOAD')
    draft.value.sourceField = ''
  if (trigger !== 'CHANGE')
    draft.value.debounceMs = 0
}

function handleSourceChange(value) {
  const [sourceType = '', ...sourceKeyParts] = String(value || '').split('::')
  draft.value.sourceType = sourceType
  draft.value.sourceKey = sourceKeyParts.join('::')
  sourceTypeTab.value = sourceType
  draft.value.pageNum = 1
  if (isListQuerySourceType.value && (!draft.value.pageSize || draft.value.pageSize > 100))
    draft.value.pageSize = 20
  draft.value.paramMappings = []
  draft.value.resultMappings = []
  loadMetadata()
}

function handleSourceTypeChange(sourceType) {
  if (!sourceType || sourceType === draft.value.sourceType)
    return
  draft.value.sourceType = sourceType
  draft.value.sourceKey = ''
  draft.value.paramMappings = []
  draft.value.resultMappings = []
  metadata.value = null
}

// 参数取值的统一展示值：field:xxx / ctx:xxx 为内部引用形态，其余原样展示（地址参数名）
function mappingValueText(mapping) {
  if (mapping.source === 'FORM_FIELD')
    return mapping.field ? `field:${mapping.field}` : null
  if (mapping.source === 'CONTEXT_PATH') {
    if (!mapping.path)
      return null
    return isKnownContextPath(mapping.path) ? `ctx:${mapping.path}` : CUSTOM_CTX_VALUE
  }
  return mapping.path || null
}

function handleMappingValueChange(mapping, value) {
  if (!value) {
    mapping.source = 'FORM_FIELD'
    mapping.field = ''
    mapping.path = ''
    return
  }
  if (value.startsWith('field:')) {
    mapping.source = 'FORM_FIELD'
    mapping.field = value.slice('field:'.length)
    mapping.path = ''
    return
  }
  if (value === CUSTOM_CTX_VALUE) {
    mapping.source = 'CONTEXT_PATH'
    mapping.field = ''
    // 已保存过自定义路径则保留，否则置空等待输入
    mapping.path = isSafePath(mapping.path) && !isKnownContextPath(mapping.path) ? mapping.path : ''
    return
  }
  if (value.startsWith('ctx:')) {
    mapping.source = 'CONTEXT_PATH'
    mapping.field = ''
    mapping.path = value.slice('ctx:'.length)
    return
  }
  // 手动输入：视为页面地址参数名（ROUTE_QUERY）
  mapping.source = 'ROUTE_QUERY'
  mapping.field = ''
  mapping.path = value
}

function isKnownContextPath(path) {
  return FIELD_EVENT_CONTEXT_PATH_OPTIONS.some(option => option.value === path)
}

// 上下文参数展示自定义路径输入框的条件：选了上下文来源但路径不在常用项中
function isCustomContextMapping(mapping) {
  return mapping.source === 'CONTEXT_PATH' && !isKnownContextPath(mapping.path)
}

function addResultMapping() {
  draft.value.resultMappings.push({ from: '', to: '', whenMissing: 'CLEAR' })
}

function removeResultMapping(index) {
  draft.value.resultMappings.splice(index, 1)
}

function saveDraft() {
  validationMessage.value = validateDraft()
  if (validationMessage.value)
    return
  if (isListQuerySourceType.value && draft.value.resultMode === 'ROOT')
    draft.value.resultMode = 'FIRST_ROW'
  const list = clone(normalizedRules.value)
  if (editingIndex.value < 0)
    list.push(clone(draft.value))
  else
    list.splice(editingIndex.value, 1, clone(draft.value))
  emit('update:modelValue', list)
  modalVisible.value = false
}

function validateDraft() {
  if (!draft.value.name.trim())
    return '请填写规则名称'
  if (draft.value.trigger !== 'FORM_LOAD' && !draft.value.sourceField)
    return '请选择触发字段'
  if (!draft.value.sourceType || !draft.value.sourceKey)
    return '请选择受管查询源'
  if (!draft.value.resultMappings.length)
    return '请至少添加一个结果回填字段'
  if (draft.value.paramMappings.some(item => !item.param || !item.source || (item.source === 'FORM_FIELD' ? !item.field : !item.path)))
    return '请完整配置查询参数来源'
  if (draft.value.paramMappings.some(item => item.source !== 'FORM_FIELD' && !isSafePath(item.path)))
    return '地址参数名或上下文路径格式不正确，只能包含字母、数字、点号、下划线和中划线'
  if (isListQuerySourceType.value && (!Number.isInteger(Number(draft.value.pageSize)) || Number(draft.value.pageSize) < 1 || Number(draft.value.pageSize) > 100))
    return '每次返回条数需设置为 1～100'
  if (draft.value.resultMappings.some(item => !item.to))
    return '请完整配置结果目标字段'
  const targetFields = draft.value.resultMappings.map(item => item.to)
  if (new Set(targetFields).size !== targetFields.length)
    return '同一规则不能重复回填同一个字段'
  return ''
}

function ruleActionOptions(index, rule) {
  return [
    { label: rule.enabled === false ? '启用' : '停用', key: `toggle:${index}` },
    { label: '复制', key: `copy:${index}` },
    { label: '上移', key: `up:${index}`, disabled: index === 0 },
    { label: '下移', key: `down:${index}`, disabled: index === normalizedRules.value.length - 1 },
    { label: '删除', key: `delete:${index}` },
  ]
}

function handleRuleAction(key) {
  const [action, rawIndex] = String(key || '').split(':')
  const index = Number(rawIndex)
  const list = clone(normalizedRules.value)
  if (!Number.isInteger(index) || !list[index])
    return
  if (action === 'toggle')
    list[index].enabled = list[index].enabled === false
  else if (action === 'copy')
    list.splice(index + 1, 0, { ...list[index], id: `field_query_${Date.now()}`, name: `${list[index].name || '查询规则'} 副本` })
  else if (action === 'delete')
    list.splice(index, 1)
  else if (action === 'up' && index > 0)
    [list[index - 1], list[index]] = [list[index], list[index - 1]]
  else if (action === 'down' && index < list.length - 1)
    [list[index + 1], list[index]] = [list[index], list[index + 1]]
  emit('update:modelValue', list)
}

function inputParamMeta(param) {
  return inputSchema.value.find(item => item.name === param) || null
}

function triggerLabel(trigger) {
  return triggerOptions.find(item => item.value === trigger)?.label || trigger || '未配置'
}

function fieldLabel(field) {
  if (!field)
    return '表单打开'
  return props.fieldOptions.find(item => item.value === field)?.label || field
}

const SOURCE_TYPE_LABELS = { DATASET: '数据集', BUSINESS_OBJECT: '业务对象', EXTERNAL_API: '接口' }

function sourceLabel(rule) {
  const source = catalog.value.find(item => item.sourceType === rule.sourceType && item.sourceKey === rule.sourceKey)
  const name = source?.sourceName || rule.sourceKey || '未选择查询源'
  return `${name} · ${SOURCE_TYPE_LABELS[rule.sourceType] || '接口'}`
}

function sourceCount(sourceType) {
  return catalog.value.filter(item => item.sourceType === sourceType).length
}

function ensureBusinessObjectIdField(fields, sourceType) {
  const list = Array.isArray(fields) ? [...fields] : []
  if (sourceType !== 'BUSINESS_OBJECT')
    return list
  const hasId = list.some(field => (field?.path || field?.field) === 'id')
  if (!hasId)
    list.unshift({ field: 'id', label: '主键 ID', path: 'id', type: 'bigint' })
  return list
}

function preferredSourceType() {
  if (catalog.value.some(item => item.sourceType === 'DATASET'))
    return 'DATASET'
  if (catalog.value.some(item => item.sourceType === 'BUSINESS_OBJECT'))
    return 'BUSINESS_OBJECT'
  return 'EXTERNAL_API'
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}
</script>

<style scoped>
.field-event-editor {
  display: grid;
  gap: 10px;
}

.field-event-modal {
  --n-color: var(--bg-primary);
  --n-color-modal: var(--bg-primary);
}

.field-event-editor__head,
.field-event-form__section-head,
.field-event-card,
.field-event-card__title,
.field-event-card__actions,
.field-event-form__switches {
  display: flex;
  align-items: center;
}

.field-event-editor__head,
.field-event-form__section-head,
.field-event-card {
  justify-content: space-between;
  gap: 12px;
}

.field-event-editor__head strong,
.field-event-form h4 {
  color: var(--n-text-color);
  font-size: 13px;
  font-weight: 600;
}

.field-event-editor__head p,
.field-event-form__section-head p,
.field-event-card p {
  margin: 3px 0 0;
  color: var(--n-text-color-3);
  font-size: 11px;
  line-height: 16px;
}

.field-event-list,
.field-event-form,
.mapping-list {
  display: grid;
  gap: 8px;
}

.field-event-card {
  padding: 10px;
  border: 1px solid var(--n-border-color);
  border-radius: 6px;
  background: var(--n-color);
}

.field-event-card__main {
  overflow: hidden;
  min-width: 0;
}

.field-event-card__title {
  gap: 6px;
}

.field-event-card__title > span:first-child {
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-event-card__main > span {
  color: var(--n-text-color-3);
  font-size: 11px;
}

.field-event-card__actions {
  flex: 0 0 auto;
  gap: 8px;
}

.field-event-form {
  max-height: min(680px, calc(100vh - 190px));
  overflow-y: auto;
  padding-right: 4px;
}

.field-event-form__section {
  padding: 14px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--bg-primary);
}

.field-event-form h4 {
  margin: 0 0 12px;
}

.field-event-form__section-head h4 {
  margin-bottom: 0;
}

.field-event-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.query-source-picker {
  display: grid;
  gap: 6px;
}

.query-source-tabs {
  width: fit-content;
  max-width: 100%;
}

.field-event-help {
  color: var(--n-text-color-3);
  font-size: 11px;
  line-height: 16px;
}

.dataset-query-settings {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -2px 0 12px;
  padding: 8px 10px;
  border: 1px solid var(--n-border-color);
  border-radius: 6px;
  background: var(--n-color-modal, #fff);
}

.dataset-query-settings > div {
  display: grid;
  gap: 2px;
}

.dataset-query-settings strong {
  color: var(--n-text-color-2);
  font-size: 12px;
}

.dataset-query-settings span,
.dataset-query-settings em {
  color: var(--n-text-color-3);
  font-size: 11px;
  font-style: normal;
}

.dataset-query-settings label {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 6px;
  color: var(--n-text-color-2);
  font-size: 12px;
}

.field-event-form__switches {
  flex-wrap: wrap;
  gap: 8px 18px;
}

.field-event-form__switches label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--n-text-color-2);
  font-size: 12px;
}

.param-empty-alert {
  margin-bottom: 0;
}

/* 参数映射卡片：头部展示参数定义（必填星 + 名称 + 编码/类型），右侧必填开关与删除；主体为统一取值选择 */
.param-mapping-card {
  display: grid;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.param-mapping-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.param-mapping-card__meta {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.param-mapping-card__label {
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  color: var(--n-text-color);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.param-required-star {
  margin-right: 2px;
  color: var(--n-error-color, #d03050);
}

.param-mapping-card__code {
  overflow: hidden;
  color: var(--n-text-color-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.param-mapping-card__param-input {
  max-width: 260px;
}

.param-mapping-card__flags {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 10px;
}

.param-mapping-card__value {
  display: grid;
  gap: 6px;
}

.param-mapping-card__error {
  color: var(--n-error-color, #d03050);
  font-size: 11px;
}

/* 结果回填卡片：返回字段 → 目标字段 → 缺失行为，一行对齐 */
.result-mapping-card {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) 20px minmax(160px, 1fr) minmax(110px, 0.5fr) auto;
  align-items: end;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.result-mapping-card__cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.result-mapping-card__cell label {
  font-size: 11px;
  color: var(--n-text-color-3);
}

.result-mapping-card__remove {
  margin-bottom: 2px;
}

.mapping-arrow {
  color: var(--n-text-color-3);
  font-size: 12px;
  text-align: center;
  padding-bottom: 6px;
}

.result-mode-field {
  margin-top: 10px;
}

@media (max-width: 760px) {
  .field-event-form__grid,
  .result-mapping-card {
    grid-template-columns: 1fr;
  }

  .mapping-arrow {
    display: none;
  }

  .dataset-query-settings {
    align-items: flex-start;
    flex-direction: column;
  }

  .query-source-tabs {
    width: 100%;
  }

  .query-source-tabs :deep(.n-radio-button) {
    flex: 1;
  }
}
</style>
