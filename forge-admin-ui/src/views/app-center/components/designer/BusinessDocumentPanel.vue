<template>
  <div class="business-document-panel">
    <div class="document-head">
      <div>
        <h3>单据设置</h3>
        <p>维护单据基础字段、编号规则和状态生命周期，主流程统一在流程与自动化中配置。</p>
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
          <section class="document-section">
            <div class="section-head">
              <div>
                <h4>基础配置</h4>
                <p>启用后运行态会按单据生命周期处理状态、流程和权限。</p>
              </div>
              <n-switch v-model:value="form.documentEnabled" @update:value="markDirty" />
            </div>

            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="14" :y-gap="6" responsive="screen">
                <n-form-item-gi label="单据名称">
                  <n-input
                    v-model:value="form.documentName"
                    :disabled="!form.documentEnabled"
                    :placeholder="defaultDocumentName"
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
            <div class="document-help-list">
              <span><strong>状态字段</strong>保存草稿、流程中、已通过等状态值。</span>
              <span><strong>发起人/负责人</strong>用于流程发起、待办归属和消息接收。</span>
              <span><strong>主流程</strong>在“流程与自动化”维护，这里只读取摘要。</span>
            </div>
          </section>

          <section class="document-section">
            <div class="section-head">
              <div>
                <h4>编号规则</h4>
                <p>点击变量即可插入模板，预览只使用样例数据，不占用真实流水号。</p>
              </div>
            </div>
            <DocumentNoRuleEditor
              v-model="form.noRuleTemplate"
              :disabled="!form.documentEnabled"
              :suite-code="effectiveSuiteCode"
              :object-code="effectiveObjectCode"
              :field-options="fieldOptions"
              @preview="handleNoRulePreview"
              @update:model-value="markDirty"
            />
          </section>

          <section class="document-section">
            <div class="section-head">
              <div>
                <h4>状态映射</h4>
                <p>将标准单据状态映射到当前对象字段值，并控制该状态下能否编辑、删除或发起主流程。</p>
              </div>
            </div>
            <DocumentStatusMappingTable
              :rows="form.statusMappingRows"
              :disabled="!form.documentEnabled"
              :status-field="form.statusField"
              :fields="fields"
              @update:rows="updateStatusRows"
            />
          </section>
        </n-spin>
      </main>

      <aside class="document-side">
        <DocumentConfigSummary
          :form="form"
          :no-rule-preview="noRulePreview"
          @configure-flow="$emit('configureFlow')"
        />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessDocumentConfig, saveBusinessDocumentConfig } from '@/api/business-app'
import DocumentConfigSummary from './DocumentConfigSummary.vue'
import DocumentNoRuleEditor from './DocumentNoRuleEditor.vue'
import DocumentStatusMappingTable from './DocumentStatusMappingTable.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
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

const emit = defineEmits(['dirtyChange', 'saved', 'loaded', 'configureFlow'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const noRulePreview = ref(null)
const form = reactive(createDefaultConfig())

const defaultDocumentName = computed(() => `${props.objectName || '业务对象'}单据`)
const effectiveSuiteCode = computed(() => props.suiteCode || form.suiteCode || '')
const effectiveObjectCode = computed(() => props.objectCode || form.objectCode || '')
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
    const configRes = await businessDocumentConfig(props.objectId)
    assignConfig(configRes.data || props.initialConfig || createDefaultConfig())
    emit('loaded', { ...form })
    emit('dirtyChange', false)
  }
  finally {
    loading.value = false
  }
}

async function saveConfig() {
  if (!props.objectId)
    return
  if (form.documentEnabled && !form.statusField) {
    message.warning('启用单据模式后必须选择状态字段')
    return
  }
  if (noRulePreview.value?.valid === false) {
    message.warning('编号规则存在错误，请先修正预览提示')
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
  const statusMappingRows = normalizeStatusRows(form.statusMappingRows)
  return {
    documentEnabled: !!form.documentEnabled,
    documentName: form.documentName || defaultDocumentName.value,
    documentNoRule: form.noRuleTemplate || '',
    noRuleTemplate: form.noRuleTemplate || '',
    statusField: form.statusField || '',
    starterField: form.starterField || '',
    ownerField: form.ownerField || '',
    defaultFlowKey: form.defaultFlowKey || form.mainFlowSummary?.flowModelKey || '',
    statusMapping: statusMappingFromRows(statusMappingRows),
    statusMappingRows,
    statusActionPolicy: form.statusActionPolicy || {},
    options: { ...(form.options || {}) },
  }
}

function assignConfig(value = {}) {
  Object.assign(form, {
    ...createDefaultConfig(),
    ...value,
    documentEnabled: readBoolean(value.documentEnabled, false),
    noRuleTemplate: value.noRuleTemplate || value.documentNoRule || '',
    statusMappingRows: normalizeStatusRows(value.statusMappingRows || rowsFromLegacyMapping(value.statusMapping || {})),
    statusActionPolicy: { ...(value.statusActionPolicy || {}) },
    mainFlowSummary: { ...(value.mainFlowSummary || {}) },
    options: { ...(value.options || {}) },
  })
  noRulePreview.value = value.noRulePreview || null
}

function updateStatusRows(rows) {
  form.statusMappingRows = normalizeStatusRows(rows)
  markDirty()
}

function handleNoRulePreview(preview) {
  noRulePreview.value = preview
}

function normalizeStatusRows(rows = []) {
  const defaults = defaultStatusRows()
  const byKey = new Map(defaults.map(row => [row.standardStatus, { ...row }]))
  for (const row of rows || []) {
    if (!row?.standardStatus)
      continue
    const key = String(row.standardStatus).toUpperCase()
    byKey.set(key, { ...(byKey.get(key) || {}), ...row, standardStatus: key })
  }
  return Array.from(byKey.values())
}

function rowsFromLegacyMapping(mapping = {}) {
  return defaultStatusRows().map(row => ({
    ...row,
    statusValue: mapping[row.standardStatus] || row.statusValue,
  }))
}

function statusMappingFromRows(rows = []) {
  return rows.reduce((result, row) => {
    if (row.standardStatus && row.statusValue)
      result[row.standardStatus] = row.statusValue
    return result
  }, {})
}

function defaultStatusRows() {
  return [
    row('DRAFT', '草稿', 'DRAFT', '草稿', 'default', true, true, true),
    row('SUBMITTED', '已提交', 'SUBMITTED', '已提交', 'info', false, false, false),
    row('IN_PROCESS', '流程中', 'IN_PROCESS', '流程中', 'warning', false, false, false),
    row('APPROVED', '已通过', 'APPROVED', '已通过', 'success', false, false, false),
    row('REJECTED', '已驳回', 'REJECTED', '已驳回', 'error', true, false, true),
    row('CANCELED', '已撤回', 'CANCELED', '已撤回', 'default', true, false, true),
    row('CLOSED', '已关闭', 'CLOSED', '已关闭', 'default', false, false, false),
  ]
}

function row(standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow) {
  return { standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow }
}

function createDefaultConfig() {
  return {
    documentEnabled: false,
    documentName: '',
    documentNoRule: '',
    noRuleTemplate: '',
    statusField: '',
    starterField: '',
    ownerField: '',
    defaultFlowKey: '',
    statusMappingRows: defaultStatusRows(),
    statusActionPolicy: {},
    mainFlowSummary: {},
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
.document-section h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.document-head p,
.document-section p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.document-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  min-height: 0;
}

.document-main {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.document-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.document-section + .document-section {
  margin-top: 12px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.document-help-list {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: #f8fbff;
  padding: 10px 12px;
}

.document-help-list span {
  color: #475569;
  font-size: 12px;
  line-height: 1.55;
}

.document-help-list strong {
  color: #1d4ed8;
}

.document-side {
  min-width: 0;
  overflow: auto;
  border-left: 1px solid #e5e7eb;
  background: #fff;
  padding: 14px;
}

@media (max-width: 1024px) {
  .document-body {
    grid-template-columns: 1fr;
  }

  .document-side {
    border-top: 1px solid #e5e7eb;
    border-left: 0;
  }
}
</style>
