<script setup>
import {
  NAlert,
  NButton,
  NCollapse,
  NCollapseItem,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NSelect,
  NSpin,
  NTag,
} from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { getDataDatasetMetadata, getDataDatasetPage } from '@/api/data/dataset'
import { usePrintCenterStore } from '@/stores/print/printCenterStore'

const props = defineProps({
  show: Boolean,
  source: { type: Object, default: null },
})
const emit = defineEmits(['update:show', 'saved'])
const store = usePrintCenterStore()
const formRef = ref(null)
const datasets = ref([])
const datasetLoading = ref(false)
const datasetMeta = ref(null)
const metaLoading = ref(false)
const metaError = ref('')
const metaExpanded = ref(true)
const error = ref('')
const form = reactive(emptyForm())
const datasetMapping = reactive({
  recordIdParam: 'id',
  childrenKey: '',
  maxRows: 1000,
})
const editing = computed(() => Boolean(form.id))
const title = computed(() => (editing.value ? '编辑打印业务' : '登记可打印业务'))
const sourceTypeOptions = [
  {
    label: '代码业务（已开发好的业务模块）',
    value: 'SERVICE',
  },
  {
    label: '数据集（数据中心已发布的查询）',
    value: 'DATASET',
  },
]
/** 服务端已注册的业务打印适配器；选中后自动带上正确的业务标识。 */
const providerOptions = [
  {
    label: '示例采购单',
    value: 'sample-purchase-order',
    objectCode: 'sample_purchase_order',
  },
]
const datasetOptions = computed(() => datasets.value.map(item => ({
  label: `${item.datasetName}（${item.datasetCode}）`,
  value: String(item.id),
})))
/** 查询参数 ≠ 结果字段；打印「单据 ID 参数」必须落在参数协议里。 */
const datasetParams = computed(() => parseDatasetParams(datasetMeta.value?.paramSchemaJson))
const datasetFields = computed(() => {
  const fields = datasetMeta.value?.fields
  return Array.isArray(fields) ? fields : []
})
const recordIdParamOptions = computed(() => datasetParams.value.map(item => ({
  label: `${item.paramName}${item.dataType ? `（${item.dataType}）` : ''}`,
  value: item.paramName,
})))
const recordIdParamMatched = computed(() => {
  const name = String(datasetMapping.recordIdParam || '').trim()
  if (!name || !datasetParams.value.length)
    return false
  return datasetParams.value.some(item => item.paramName === name)
})
const rules = {
  sourceName: {
    required: true,
    message: '请填写显示名称，例如「采购单」',
    trigger: ['input', 'blur'],
  },
  sourceCode: {
    required: true,
    pattern: /^[A-Z][\w-]{0,79}$/i,
    message: '调用编码：字母开头，可用字母、数字、下划线、短横线',
    trigger: ['input', 'blur'],
  },
  objectCode: {
    required: true,
    pattern: /^[A-Z]\w{0,99}$/i,
    message: '业务标识：字母开头，可用字母、数字、下划线',
    trigger: ['input', 'blur'],
  },
}

watch(() => props.show, async (show) => {
  if (!show)
    return
  Object.assign(form, props.source ? sourceForm(props.source) : emptyForm())
  syncDatasetMappingFromJson(form.mappingJson)
  error.value = ''
  metaError.value = ''
  datasetMeta.value = null
  metaExpanded.value = true
  if (form.sourceType === 'DATASET' || !editing.value)
    await loadDatasets()
  if (form.sourceType === 'DATASET' && form.datasetId)
    await loadDatasetMeta(form.datasetId)
})

watch(() => form.providerCode, (code) => {
  if (form.sourceType !== 'SERVICE' || !code)
    return
  const matched = providerOptions.find(item => item.value === code)
  if (matched?.objectCode)
    form.objectCode = matched.objectCode
})

watch(() => form.sourceType, (type) => {
  if (type === 'DATASET') {
    form.providerCode = null
    if (!form.mappingJson) {
      datasetMapping.recordIdParam = 'id'
      datasetMapping.childrenKey = ''
      datasetMapping.maxRows = 1000
      form.mappingJson = stringifyDatasetMapping()
    }
    else {
      syncDatasetMappingFromJson(form.mappingJson)
    }
    if (form.datasetId)
      loadDatasetMeta(form.datasetId)
  }
  else {
    form.datasetId = null
    datasetMeta.value = null
    metaError.value = ''
    if (form.mappingJson === '{}')
      form.mappingJson = null
  }
})

watch(() => form.datasetId, (id) => {
  if (form.sourceType !== 'DATASET')
    return
  if (!id) {
    datasetMeta.value = null
    metaError.value = ''
    return
  }
  metaExpanded.value = true
  loadDatasetMeta(id)
})

watch(datasetMapping, () => {
  if (form.sourceType === 'DATASET')
    form.mappingJson = stringifyDatasetMapping()
}, { deep: true })

function emptyForm() {
  return {
    id: null,
    sourceRevision: null,
    sourceName: '',
    sourceCode: '',
    sourceType: 'SERVICE',
    providerCode: '',
    datasetId: null,
    objectCode: '',
    parameterSchemaJson: '{}',
    mappingJson: null,
  }
}

function sourceForm(source) {
  return {
    ...emptyForm(),
    ...source,
    datasetId: source.datasetId == null ? null : String(source.datasetId),
  }
}

function stringifyDatasetMapping() {
  const payload = {
    recordIdParam: String(datasetMapping.recordIdParam || 'id').trim() || 'id',
    maxRows: Number(datasetMapping.maxRows) > 0 ? Number(datasetMapping.maxRows) : 1000,
  }
  const childrenKey = String(datasetMapping.childrenKey || '').trim()
  if (childrenKey)
    payload.childrenKey = childrenKey
  return JSON.stringify(payload, null, 2)
}

function syncDatasetMappingFromJson(raw) {
  try {
    const parsed = raw ? JSON.parse(raw) : {}
    datasetMapping.recordIdParam = parsed.recordIdParam || 'id'
    datasetMapping.childrenKey = parsed.childrenKey || ''
    datasetMapping.maxRows = Number(parsed.maxRows) > 0 ? Number(parsed.maxRows) : 1000
  }
  catch {
    datasetMapping.recordIdParam = 'id'
    datasetMapping.childrenKey = ''
    datasetMapping.maxRows = 1000
  }
}

async function loadDatasets() {
  datasetLoading.value = true
  try {
    const { data } = await getDataDatasetPage({
      pageNum: 1,
      pageSize: 100,
      status: 1,
      publishStatus: 1,
    })
    datasets.value = data?.records || []
  }
  catch (reason) {
    error.value = reason.message || '无法读取已发布数据集'
  }
  finally {
    datasetLoading.value = false
  }
}

function parseDatasetParams(raw) {
  try {
    const parsed = raw ? JSON.parse(raw) : []
    if (!Array.isArray(parsed))
      return []
    return parsed
      .map(item => ({
        paramName: String(item?.paramName || '').trim(),
        dataType: String(item?.dataType || 'STRING').trim() || 'STRING',
      }))
      .filter(item => item.paramName)
  }
  catch {
    return []
  }
}

function fieldLabel(field) {
  const name = field?.fieldName || '-'
  const label = String(field?.fieldLabel || '').trim()
  return label && label !== name ? `${label}（${name}）` : name
}

function ensureRecordIdParam() {
  const params = datasetParams.value
  if (!params.length)
    return
  const current = String(datasetMapping.recordIdParam || '').trim()
  if (params.some(item => item.paramName === current))
    return
  const preferred = params.find(item => item.paramName === 'id') || params[0]
  datasetMapping.recordIdParam = preferred.paramName
}

async function loadDatasetMeta(datasetId) {
  const id = Number(datasetId)
  if (!Number.isFinite(id) || id <= 0) {
    datasetMeta.value = null
    metaError.value = ''
    return
  }
  metaLoading.value = true
  metaError.value = ''
  try {
    const { data } = await getDataDatasetMetadata(id)
    datasetMeta.value = data || null
    ensureRecordIdParam()
  }
  catch (reason) {
    datasetMeta.value = null
    metaError.value = reason.message || '无法读取数据集结构'
  }
  finally {
    metaLoading.value = false
  }
}

async function refreshDatasetMeta() {
  if (!form.datasetId)
    return
  metaExpanded.value = true
  await loadDatasetMeta(form.datasetId)
}

function toggleMetaExpanded() {
  metaExpanded.value = !metaExpanded.value
}

function fillObjectCodeFromSourceCode() {
  if (form.objectCode)
    return
  const matched = providerOptions.find(item => item.value === form.providerCode)
  if (matched?.objectCode) {
    form.objectCode = matched.objectCode
    return
  }
  if (form.sourceCode)
    form.objectCode = form.sourceCode.replace(/-/g, '_')
}

function validateJson(value, label, required = false) {
  if (!value && !required)
    return true
  try {
    const parsed = JSON.parse(value || '')
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object')
      throw new Error('JSON 不是对象')
    return true
  }
  catch {
    error.value = `${label}格式不对，需要是 JSON 对象`
    return false
  }
}

async function save() {
  error.value = ''
  try {
    fillObjectCodeFromSourceCode()
    await formRef.value?.validate()
    if (form.sourceType === 'SERVICE' && !String(form.providerCode || '').trim()) {
      error.value = '请选择要对接的业务模块（没有可选时请与开发确认适配器编码）'
      return false
    }
    if (form.sourceType === 'DATASET' && !form.datasetId) {
      error.value = '请选择数据中心里已发布的数据集'
      return false
    }
    if (form.sourceType === 'DATASET') {
      if (!datasetParams.value.length) {
        error.value = '所选数据集没有查询参数。请先在数据中心声明主键参数（如 id）并发布'
        return false
      }
      if (!recordIdParamMatched.value) {
        error.value = `单据 ID 参数「${datasetMapping.recordIdParam}」不在数据集参数协议中`
        return false
      }
    }
    if (form.sourceType === 'DATASET')
      form.mappingJson = stringifyDatasetMapping()
    if (!validateJson(form.parameterSchemaJson, '额外打印参数')
      || !validateJson(form.mappingJson, '数据映射', form.sourceType === 'DATASET')) {
      return false
    }
    const saved = await store.save({
      ...form,
      sourceName: form.sourceName.trim(),
      sourceCode: form.sourceCode.trim(),
      providerCode: String(form.providerCode || '').trim() || null,
      objectCode: form.objectCode.trim(),
    })
    emit('saved', saved)
    emit('update:show', false)
  }
  catch (reason) {
    error.value = reason.message || '保存失败'
  }
  return false
}
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    :title="title"
    :style="{ width: 'min(640px, 94vw)' }"
    :mask-closable="!store.saving"
    @update:show="value => !store.saving && emit('update:show', value)"
  >
    <!-- 错误 -->
    <NAlert v-if="error" type="error" class="source-form__alert">
      {{ error }}
    </NAlert>
    <!-- 新建说明 -->
    <NAlert v-if="!editing" type="info" class="source-form__alert">
      先登记「哪种单据能打印」。保存后默认停用；做好模板并绑定场景后再启用。
      代码业务只能对接系统里已经写好的取数程序（目前有「示例采购单」）。
      用户管理等系统页若没有专用程序，请改选「数据集」。说明见 docs/PRINT_CENTER.md。
    </NAlert>

    <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" :label-width="108">
      <!-- 基础信息 -->
      <NFormItem label="显示名称" path="sourceName">
        <div class="source-form__field">
          <NInput v-model:value="form.sourceName" maxlength="100" placeholder="列表里看到的名字，例如：采购单" />
          <p class="source-form__hint">
            给业务人员看的名称，随便写清楚即可。
          </p>
        </div>
      </NFormItem>
      <NFormItem label="调用编码" path="sourceCode">
        <div class="source-form__field">
          <NInput
            v-model:value="form.sourceCode"
            :disabled="editing"
            maxlength="80"
            placeholder="例如：sample_purchase_order"
            @blur="fillObjectCodeFromSourceCode"
          />
          <p class="source-form__hint">
            业务页面调用打印时用的编码，创建后不能改。示例采购单建议填 sample_purchase_order。
          </p>
        </div>
      </NFormItem>
      <NFormItem label="数据从哪来">
        <div class="source-form__field">
          <NSelect v-model:value="form.sourceType" :disabled="editing" :options="sourceTypeOptions" />
          <p class="source-form__hint">
            有现成业务代码选「代码业务」；想直接打数据集结果选「数据集」。
          </p>
        </div>
      </NFormItem>

      <!-- 代码业务 -->
      <NFormItem v-if="form.sourceType === 'SERVICE'" label="对接业务">
        <div class="source-form__field">
          <NSelect
            v-model:value="form.providerCode"
            filterable
            tag
            clearable
            :options="providerOptions"
            placeholder="选择已接入的业务，或输入适配器编码"
          />
          <p class="source-form__hint">
            「示例采购单」只能打采购单样例，不能打用户列表。
            其它业务要么开发写取数程序，要么改用上方「数据集」。
          </p>
        </div>
      </NFormItem>

      <!-- 数据集 -->
      <template v-else>
        <NFormItem label="选择数据集">
          <div class="source-form__field">
            <div class="source-form__dataset-row">
              <NSelect
                v-model:value="form.datasetId"
                filterable
                class="source-form__dataset-select"
                :loading="datasetLoading"
                :options="datasetOptions"
                placeholder="选择数据中心已发布且你有权限的数据集"
              />
              <NButton
                secondary
                circle
                :disabled="!form.datasetId"
                :loading="metaLoading"
                title="查看数据集结构"
                aria-label="查看数据集结构"
                @click="refreshDatasetMeta"
              >
                <template #icon>
                  <i class="i-lucide:search" />
                </template>
              </NButton>
            </div>
            <p class="source-form__hint">
              只列出已发布的数据集。结果里有 <code>id</code> 列不够，查询参数协议里也必须声明同名参数。
            </p>
          </div>
        </NFormItem>

        <!-- 选中后展示参数 / 字段，可展开收起 -->
        <div v-if="form.datasetId" class="source-form__meta">
          <button
            type="button"
            class="source-form__meta-toggle"
            :aria-expanded="metaExpanded"
            @click="toggleMetaExpanded"
          >
            <span class="source-form__meta-toggle-label">
              数据集结构
              <span v-if="datasetMeta" class="source-form__meta-count">
                参数 {{ datasetParams.length }} · 字段 {{ datasetFields.length }}
              </span>
            </span>
            <i
              class="source-form__meta-chevron"
              :class="metaExpanded ? 'i-lucide:chevron-up' : 'i-lucide:chevron-down'"
            />
          </button>
          <div v-show="metaExpanded" class="source-form__meta-body">
            <NSpin :show="metaLoading">
              <NAlert v-if="metaError" type="error" class="source-form__meta-alert">
                {{ metaError }}
              </NAlert>
              <template v-else-if="datasetMeta">
                <NAlert
                  v-if="!datasetParams.length"
                  type="warning"
                  class="source-form__meta-alert"
                >
                  这个数据集没有声明任何查询参数。请先到数据中心给它加主键参数（如
                  <code>id</code>），用该参数过滤一行，再发布。
                </NAlert>
                <NAlert
                  v-else-if="!recordIdParamMatched"
                  type="warning"
                  class="source-form__meta-alert"
                >
                  当前「单据 ID 参数」
                  <code>{{ datasetMapping.recordIdParam || '-' }}</code>
                  不在参数列表里，保存后打印会 409。请从下方参数中选择。
                </NAlert>

                <div class="source-form__meta-block">
                  <div class="source-form__meta-title">
                    查询参数（打印要对接的是这些，不是结果列）
                  </div>
                  <div v-if="datasetParams.length" class="source-form__meta-tags">
                    <NTag
                      v-for="item in datasetParams"
                      :key="item.paramName"
                      size="small"
                      :type="item.paramName === datasetMapping.recordIdParam ? 'primary' : 'default'"
                      :bordered="false"
                    >
                      {{ item.paramName }}
                      <span class="source-form__meta-type">{{ item.dataType }}</span>
                    </NTag>
                  </div>
                  <p v-else class="source-form__hint">
                    暂无查询参数
                  </p>
                </div>

                <div class="source-form__meta-block">
                  <div class="source-form__meta-title">
                    结果字段（模板设计可用）
                    <span class="source-form__meta-count">{{ datasetFields.length }}</span>
                  </div>
                  <div v-if="datasetFields.length" class="source-form__meta-tags">
                    <NTag
                      v-for="field in datasetFields"
                      :key="field.fieldName"
                      size="small"
                      :bordered="false"
                    >
                      {{ fieldLabel(field) }}
                    </NTag>
                  </div>
                  <p v-else class="source-form__hint">
                    暂无字段，请先在数据中心同步字段并发布
                  </p>
                </div>
              </template>
            </NSpin>
          </div>
        </div>

        <NFormItem label="单据 ID 参数">
          <div class="source-form__field">
            <NSelect
              v-if="recordIdParamOptions.length"
              v-model:value="datasetMapping.recordIdParam"
              filterable
              tag
              :options="recordIdParamOptions"
              placeholder="从数据集参数中选择"
            />
            <NInput
              v-else
              v-model:value="datasetMapping.recordIdParam"
              maxlength="80"
              placeholder="通常填 id"
            />
            <p class="source-form__hint">
              页面传来的单据主键会写入这个参数名去查数据集。必须是上方「查询参数」之一。
            </p>
          </div>
        </NFormItem>
        <NFormItem label="明细字段名">
          <div class="source-form__field">
            <NInput v-model:value="datasetMapping.childrenKey" maxlength="80" placeholder="没有明细可留空，例如 items" />
            <p class="source-form__hint">
              若数据集结果里有子表数组，填数组字段名；没有明细留空。
            </p>
          </div>
        </NFormItem>
        <NFormItem label="最多行数">
          <div class="source-form__field">
            <NInputNumber
              v-model:value="datasetMapping.maxRows"
              :min="1"
              :max="10000"
              class="source-form__number"
            />
            <p class="source-form__hint">
              防止一次拉太多行，默认 1000。
            </p>
          </div>
        </NFormItem>
      </template>

      <NFormItem label="业务标识" path="objectCode">
        <div class="source-form__field">
          <NInput
            v-model:value="form.objectCode"
            maxlength="100"
            placeholder="示例采购单必须是 sample_purchase_order"
            @focus="fillObjectCodeFromSourceCode"
          />
          <p class="source-form__hint">
            必须和业务模块约定一致。示例采购单只能填 <code>sample_purchase_order</code>，填成 purchase_order 会 403。
          </p>
        </div>
      </NFormItem>

      <!-- 高级：少数字段才需要碰 JSON -->
      <NCollapse class="source-form__advanced">
        <NCollapseItem title="高级设置（一般不用改）" name="advanced">
          <NFormItem label="额外参数">
            <div class="source-form__field">
              <NInput
                v-model:value="form.parameterSchemaJson"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 8 }"
                placeholder='默认 {} 即可。例如：{"warehouseId":{"type":"string","required":false}}'
              />
              <p class="source-form__hint">
                页面除单据 ID 外还要传别的条件时才配置。空对象 <code>{}</code> 表示不接收额外参数。
              </p>
            </div>
          </NFormItem>
          <NFormItem v-if="form.sourceType === 'SERVICE'" label="映射配置">
            <div class="source-form__field">
              <NInput
                :value="form.mappingJson || ''"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 6 }"
                placeholder="代码业务通常留空"
                @update:value="value => form.mappingJson = value || null"
              />
              <p class="source-form__hint">
                代码业务由适配器自己取数，这里一般留空。
              </p>
            </div>
          </NFormItem>
          <NFormItem v-else label="映射 JSON">
            <div class="source-form__field">
              <NInput
                v-model:value="form.mappingJson"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 8 }"
                readonly
              />
              <p class="source-form__hint">
                由上方「单据 ID 参数 / 明细字段名 / 最多行数」自动生成，仅供核对。
              </p>
            </div>
          </NFormItem>
        </NCollapseItem>
      </NCollapse>
    </NForm>

    <template #footer>
      <div class="source-form__actions">
        <NButton :disabled="store.saving" @click="emit('update:show', false)">
          取消
        </NButton>
        <NButton type="primary" :loading="store.saving" @click="save">
          保存
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.source-form__alert {
  margin-bottom: 12px;
}

.source-form__field {
  width: 100%;
}

.source-form__dataset-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.source-form__dataset-select {
  flex: 1;
  min-width: 0;
}

.source-form__meta {
  margin: 0 0 12px;
  padding: 0;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--gray-100, #f6f8fb);
  overflow: hidden;
}

.source-form__meta-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  margin: 0;
  padding: 8px 12px;
  border: 0;
  background: transparent;
  color: var(--text-primary, #1d2129);
  cursor: pointer;
  text-align: left;
}

.source-form__meta-toggle:hover {
  background: rgba(0, 0, 0, 0.02);
}

.source-form__meta-toggle-label {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.source-form__meta-chevron {
  flex-shrink: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 14px;
}

.source-form__meta-body {
  padding: 0 12px 10px;
}

.source-form__meta-alert {
  margin-bottom: 10px;
}

.source-form__meta-block + .source-form__meta-block {
  margin-top: 10px;
}

.source-form__meta-title {
  margin-bottom: 6px;
  color: var(--text-primary, #1d2129);
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.source-form__meta-count {
  margin-left: 6px;
  color: var(--text-tertiary, #86909c);
  font-weight: 400;
}

.source-form__meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.source-form__meta-type {
  margin-left: 4px;
  opacity: 0.7;
}

.source-form__hint {
  margin: 6px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 18px;
}

.source-form__hint code {
  padding: 0 4px;
  border-radius: 3px;
  background: var(--gray-100, #f2f3f5);
  font-size: 12px;
}

.source-form__number {
  width: 160px;
}

.source-form__advanced {
  margin-top: 4px;
}

.source-form__advanced :deep(.n-collapse-item__header) {
  font-size: 13px;
  color: var(--text-tertiary, #86909c);
}

.source-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
