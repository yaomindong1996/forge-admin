<template>
  <div class="business-detail-designer">
    <div v-if="!settingsOnly" class="detail-designer-head">
      <div>
        <h3>详情设置</h3>
        <p>详情主信息自动复用表单设计布局，关联数据来自关系配置。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="$emit('openForm')">
          调整表单
        </n-button>
        <n-button size="small" secondary @click="$emit('openRelations')">
          配置关系
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveLayout">
          保存设置
        </n-button>
      </n-space>
    </div>

    <div class="detail-designer-body">
      <main class="detail-workspace">
        <section v-if="!settingsOnly" class="detail-section">
          <div class="detail-section-head">
            <div>
              <h4>主信息展示</h4>
              <p>查看详情按表单字段顺序只读展示，不再单独维护第二套字段分组。</p>
            </div>
            <n-tag size="small" type="info" :bordered="false">
              {{ formFieldRefs.length }} 个字段
            </n-tag>
          </div>

          <div v-if="fieldPreviewRows.length" class="field-preview-grid">
            <article v-for="field in fieldPreviewRows" :key="field.field" class="field-preview-item">
              <strong>{{ field.label || field.field }}</strong>
              <span>{{ field.field }} · {{ field.componentType || field.dataType || 'input' }}</span>
              <em v-if="field.required">必填</em>
              <em v-else-if="isReadonlySystemField(field)">系统字段</em>
            </article>
          </div>
          <n-empty v-else description="表单暂未配置字段" />
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <h4>关联数据页签</h4>
              <p>页签名称、筛选字段和是否可在编辑表单维护，统一在关系配置中设置。</p>
            </div>
            <n-tag size="small" :type="relationRows.length ? 'success' : 'default'" :bordered="false">
              {{ relationRows.length }} 个关系
            </n-tag>
          </div>

          <div v-if="relationRows.length" class="relation-preview-list">
            <article v-for="relation in relationRows" :key="relation.id || relation.clientKey" class="relation-preview-item">
              <div class="relation-preview-main">
                <strong>{{ relation.tabTitle }}</strong>
                <span>{{ relation.sentence }}</span>
              </div>
              <div class="relation-preview-meta">
                <n-tag size="small" :bordered="false">
                  {{ relationTypeLabel(relation.relationType) }}
                </n-tag>
                <n-tag v-if="relation.inlineEditEnabled" size="small" type="success" :bordered="false">
                  编辑表单可维护
                </n-tag>
                <n-tag v-if="relation.defaultFilter" size="small" type="info" :bordered="false">
                  已配置筛选
                </n-tag>
              </div>
            </article>
          </div>
          <n-empty v-else description="暂无启用的关联数据页签" />
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <h4>附加页签</h4>
              <p>操作日志和审批记录是详情页附加能力，不影响表单布局。</p>
            </div>
          </div>
          <div class="detail-toggle-list">
            <label class="detail-toggle-row">
              <span>操作日志</span>
              <n-switch v-model:value="detailOptions.showOperationLog" @update:value="markDirty" />
            </label>
            <label class="detail-toggle-row">
              <span>审批记录</span>
              <n-switch v-model:value="detailOptions.showApprovalLog" @update:value="markDirty" />
            </label>
          </div>
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <h4>数量区块</h4>
              <p>按当前记录映射查询参数，展示通用数量余额、流水或锁定记录。</p>
            </div>
            <n-space size="small">
              <n-button size="small" secondary @click="addQuantityPanel('quantity-balance')">
                添加余额
              </n-button>
              <n-button size="small" secondary @click="addQuantityPanel('quantity-ledger')">
                添加流水
              </n-button>
              <n-button size="small" secondary @click="addQuantityPanel('quantity-lock')">
                添加锁定
              </n-button>
            </n-space>
          </div>

          <div v-if="detailOptions.quantityPanels.length" class="quantity-panel-list">
            <article v-for="(panel, index) in detailOptions.quantityPanels" :key="panel.key" class="quantity-panel-item">
              <div class="quantity-panel-head">
                <strong>{{ panel.title || quantityPanelTypeLabel(panel.type) }}</strong>
                <n-button text type="error" size="small" @click="removeQuantityPanel(index)">
                  移除
                </n-button>
              </div>
              <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                <n-form-item-gi label="区块类型">
                  <n-select
                    v-model:value="panel.type"
                    :options="quantityPanelTypeOptions"
                    @update:value="value => updateQuantityPanelType(panel, value)"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="区块标题">
                  <n-input v-model:value="panel.title" placeholder="数量余额 / 数量流水" @update:value="markDirty" />
                </n-form-item-gi>
                <n-form-item-gi label="每页条数">
                  <n-input-number v-model:value="panel.pageSize" :min="1" :max="200" style="width: 100%" @update:value="markDirty" />
                </n-form-item-gi>
                <n-form-item-gi :span="3" label="查询参数映射">
                  <n-input
                    v-model:value="panel.paramsText"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 6 }"
                    placeholder="sourceRecordId=${row.id}，每行一个"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi :span="3" label="展示字段">
                  <n-input
                    v-model:value="panel.displayFieldsText"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 6 }"
                    placeholder="field:显示名，每行一个；留空使用默认列"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </article>
          </div>
          <n-empty v-else description="暂无数量区块" />
        </section>
      </main>

      <aside class="detail-summary-pane">
        <section>
          <h4>详情来源</h4>
          <div class="source-row">
            <span>主信息</span>
            <strong>表单设计</strong>
          </div>
          <div class="source-row">
            <span>关联页签</span>
            <strong>关系配置</strong>
          </div>
          <div class="source-row">
            <span>编辑关联数据</span>
            <strong>{{ inlineRelationText }}</strong>
          </div>
          <div class="source-row">
            <span>数量区块</span>
            <strong>{{ detailOptions.quantityPanels.length }} 个</strong>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { saveBusinessObjectDesigner, saveBusinessObjectDetailLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  buildPageDesignModelSchema,
  createDefaultPageSchema,
  createPageModelRef,
  isReadonlySystemField,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import { createViewSchemaFromPageSchema } from './form-first/viewSchema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  modelValue: {
    type: Object,
    default: null,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
  relations: {
    type: Array,
    default: () => [],
  },
  viewSchema: {
    type: Object,
    default: null,
  },
  settingsOnly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'update:viewSchema', 'saved', 'dirtyChange', 'openForm', 'openRelations'])

const message = useMessage()
const saving = ref(false)

const baseModelSchema = computed(() => {
  const modelFields = props.modelSchema?.fields || []
  return {
    ...(props.modelSchema || {}),
    fields: modelFields.length ? modelFields : props.fields.map(toPageField),
  }
})
const localSchema = ref(resolveSchema(props.modelValue, resolveDesignModelSchema(props.modelValue, baseModelSchema.value)))
const effectiveModelSchema = computed(() => resolveDesignModelSchema(localSchema.value, baseModelSchema.value))
const designFields = computed(() => effectiveModelSchema.value.fields || [])
const editZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'edit') || null)
const detailZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'detail') || null)
const fieldMap = computed(() => new Map(designFields.value.map(field => [field.field, field])))
const detailViewFieldRefs = computed(() => resolveDetailViewFieldRefs(props.viewSchema, fieldMap.value))
const formFieldRefs = computed(() => {
  const refs = resolveFormFieldRefs(editZone.value, fieldMap.value)
    .filter(ref => isPrimaryDetailField(fieldMap.value.get(ref)))
  return refs.length ? refs : detailViewFieldRefs.value
})
const fieldPreviewRows = computed(() => formFieldRefs.value.map(ref => fieldMap.value.get(ref)).filter(Boolean))
const relationRows = computed(() => normalizeRelationRows(props.relations || []))
const inlineRelationCount = computed(() => relationRows.value.filter(item => item.inlineEditEnabled).length)
const inlineRelationText = computed(() => inlineRelationCount.value ? `${inlineRelationCount.value} 个` : '未启用')
const quantityPanelTypeOptions = [
  { label: '数量余额', value: 'quantity-balance' },
  { label: '数量流水', value: 'quantity-ledger' },
  { label: '数量锁定', value: 'quantity-lock' },
]
const detailOptions = ref(resolveDetailOptions(detailZone.value))

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, resolveDesignModelSchema(value, baseModelSchema.value))
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
    detailOptions.value = resolveDetailOptions(next.zones?.find(zone => zone.zoneKey === 'detail') || null)
  },
  { deep: true, immediate: true },
)

watch(
  effectiveModelSchema,
  (value) => {
    const next = syncPageSchemaWithModel(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  () => props.relations,
  () => {
    syncDetailSchema()
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
      emit('update:viewSchema', cloneSchema(buildCurrentViewSchema(value)))
      emit('dirtyChange', true)
    }
  },
  { deep: true },
)

function resolveSchema(pageSchema, modelSchema) {
  return syncPageSchemaWithModel(
    cloneSchema(pageSchema || createDefaultPageSchema(modelSchema)),
    modelSchema,
  )
}

function resolveDesignModelSchema(pageSchema, modelSchema) {
  const refs = mergePrimaryModelRef(pageSchema?.modelRefs || [], modelSchema || {})
  return buildPageDesignModelSchema(modelSchema || {}, refs)
}

function mergePrimaryModelRef(modelRefs, modelSchema) {
  if (!Array.isArray(modelRefs) || !modelRefs.length)
    return []
  const primaryRef = createPageModelRef({ modelSchema }, { primary: true })
  const refs = modelRefs.map(ref => ref?.primary
    ? {
        ...ref,
        modelCode: primaryRef.modelCode || ref.modelCode,
        modelName: primaryRef.modelName || ref.modelName,
        tableName: primaryRef.tableName || ref.tableName,
        relations: primaryRef.relations?.length ? primaryRef.relations : ref.relations,
        fields: primaryRef.fields,
      }
    : ref)
  if (!refs.some(ref => ref?.primary))
    refs.unshift(primaryRef)
  return refs
}

function resolveDetailOptions(zone) {
  const zoneProps = zone?.props || {}
  return {
    showOperationLog: zoneProps.showOperationLog !== false,
    showApprovalLog: zoneProps.showApprovalLog !== false,
    quantityPanels: normalizeQuantityPanels(zoneProps.quantityPanels),
  }
}

function normalizeQuantityPanels(value) {
  return (Array.isArray(value) ? value : [])
    .map((panel, index) => normalizeQuantityPanel(panel, index))
    .filter(Boolean)
}

function normalizeQuantityPanel(panel = {}, index = 0) {
  const type = normalizeQuantityPanelType(panel.type || panel.quantity?.queryType || panel.dataSource?.queryType)
  const paramsMap = panel.quantity?.paramsMap || panel.dataSource?.paramsMap || panel.dataSource?.params || {}
  const columns = panel.table?.columns || []
  return {
    key: panel.key || `${type}_${Date.now()}_${index}`,
    type,
    title: panel.title || quantityPanelTypeLabel(type),
    pageSize: Number(panel.quantity?.pageSize || panel.dataSource?.pageSize || 20),
    paramsText: searchParamsToLines(paramsMap),
    displayFieldsText: columnsToLines(columns),
  }
}

function normalizeQuantityPanelType(type) {
  return quantityPanelTypeOptions.some(item => item.value === type) ? type : 'quantity-balance'
}

function quantityPanelTypeLabel(type) {
  return quantityPanelTypeOptions.find(item => item.value === type)?.label || '数量区块'
}

function addQuantityPanel(type = 'quantity-balance') {
  const normalizedType = normalizeQuantityPanelType(type)
  detailOptions.value.quantityPanels.push({
    key: `${normalizedType}_${Date.now()}`,
    type: normalizedType,
    title: quantityPanelTypeLabel(normalizedType),
    pageSize: 20,
    paramsText: `sourceRecordId=\${row.id}`,
    displayFieldsText: '',
  })
  markDirty()
}

function removeQuantityPanel(index) {
  detailOptions.value.quantityPanels.splice(index, 1)
  markDirty()
}

function updateQuantityPanelType(panel, type) {
  panel.type = normalizeQuantityPanelType(type)
  if (!panel.title || quantityPanelTypeOptions.some(item => item.label === panel.title))
    panel.title = quantityPanelTypeLabel(panel.type)
  markDirty()
}

function buildQuantityPanelPayload(panel = {}) {
  const type = normalizeQuantityPanelType(panel.type)
  const paramsMap = parseSearchParamLines(panel.paramsText)
  const columns = parseColumnLines(panel.displayFieldsText)
  const result = {
    key: panel.key || `${type}_${Date.now()}`,
    type,
    title: panel.title || quantityPanelTypeLabel(type),
    dataSource: {
      type: 'quantity',
      queryType: type,
      paramsMap,
      pageSize: Number(panel.pageSize || 20),
    },
    quantity: {
      queryType: type,
      paramsMap,
      pageNum: 1,
      pageSize: Number(panel.pageSize || 20),
    },
  }
  if (columns.length) {
    result.table = {
      rowKey: 'id',
      columns,
      pagination: false,
      maxHeight: 320,
    }
  }
  return result
}

function resolveFormFieldRefs(zone, fields) {
  const canvasItems = zone?.props?.canvas?.items || []
  const canvasRefs = canvasItems
    .filter(item => item.fieldRef)
    .sort(compareCanvasItem)
    .map(item => item.fieldRef)
  const refs = canvasRefs.length ? canvasRefs : (zone?.fieldRefs || [])
  return Array.from(new Set(refs.filter(ref => fields.has(ref))))
}

function resolveDetailViewFieldRefs(viewSchema = {}, fields) {
  const sections = Array.isArray(viewSchema?.detail?.sections) ? viewSchema.detail.sections : []
  const refs = sections
    .filter(section => section?.visible !== false)
    .flatMap(section => Array.isArray(section.fields) ? section.fields : [])
    .filter(item => item?.visible !== false)
    .map(item => item.fieldCode || item.field)
    .filter(ref => ref && fields.has(ref) && isPrimaryDetailField(fields.get(ref)))
  return Array.from(new Set(refs))
}

function compareCanvasItem(left, right) {
  const leftY = Number(left.y || 0)
  const rightY = Number(right.y || 0)
  if (Math.abs(leftY - rightY) > 8)
    return leftY - rightY
  const leftX = Number(left.x || 0)
  const rightX = Number(right.x || 0)
  if (leftX !== rightX)
    return leftX - rightX
  return Number(left.zIndex || 0) - Number(right.zIndex || 0)
}

function normalizeRelationRows(relations) {
  return (relations || [])
    .map((relation, index) => {
      const config = parseRelationConfig(relation.relationConfig)
      return {
        ...relation,
        clientKey: relation.id || `relation_${index}`,
        tabTitle: config.detailTabTitle || relation.detailTabTitle || relation.relationName || relation.targetObjectName || relation.targetObjectCode || `关联 ${index + 1}`,
        defaultFilter: config.defaultFilter || relation.defaultFilter || '',
        inlineEditEnabled: config.inlineEditEnabled === true || config.inlineEditEnabled === 'true',
        showInDetail: config.showInDetail !== false,
        sentence: relationSentence(relation),
      }
    })
    .filter(relation => relation.status !== 0 && relation.showInDetail)
}

function parseRelationConfig(value) {
  if (!value)
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch {
    return {
      defaultFilter: value,
    }
  }
}

function parseSearchParamLines(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  if (text.startsWith('{')) {
    try {
      const parsed = JSON.parse(text)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return text
    .split(/\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .reduce((result, item) => {
      const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
      const key = String(parts[0] || '').trim()
      const paramValue = String(parts[1] || '').trim()
      if (key && paramValue)
        result[key] = paramValue
      return result
    }, {})
}

function searchParamsToLines(value) {
  const source = value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  return Object.entries(source)
    .map(([key, item]) => `${key}=${item}`)
    .join('\n')
}

function parseColumnLines(value) {
  return String(value || '')
    .split(/\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .map((item) => {
      const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
      const field = String(parts[0] || '').trim()
      const label = String(parts[1] || field).trim()
      return field ? { prop: field, label, minWidth: 120 } : null
    })
    .filter(Boolean)
}

function columnsToLines(value) {
  return (Array.isArray(value) ? value : [])
    .map((column) => {
      const field = column.prop || column.field || column.key
      if (!field)
        return ''
      return `${field}:${column.label || column.title || field}`
    })
    .filter(Boolean)
    .join('\n')
}

function relationSentence(relation = {}) {
  const source = relation.sourceObjectName || relation.sourceObjectCode || '当前对象'
  const target = relation.targetObjectName || relation.targetObjectCode || '目标对象'
  const verbs = {
    REFERENCE: '属于 / 引用',
    CHILD_LIST: '有多个',
    DETAIL: '包含明细',
    MANY_TO_MANY: '关联多个',
  }
  return `${source}${verbs[relation.relationType] || '关联'}${target}`
}

function relationTypeLabel(value) {
  const labels = {
    REFERENCE: '引用',
    CHILD_LIST: '一对多',
    DETAIL: '明细',
    MANY_TO_MANY: '多对多',
  }
  return labels[value] || value || '关系'
}

function isPrimaryDetailField(field = {}) {
  const sourceField = field.sourceField || field.field
  return !field.modelCode || field.field === sourceField
}

function buildDetailZone(currentZone) {
  const groups = [{
    key: 'form_layout',
    title: '主信息',
    columns: resolveFormColumns(editZone.value),
    items: formFieldRefs.value.map(fieldRef => ({
      fieldRef,
      hidden: false,
      readonly: true,
    })),
  }]
  return {
    ...(currentZone || {}),
    zoneKey: 'detail',
    componentKey: 'detail-panel',
    enabled: true,
    fieldRefs: formFieldRefs.value,
    props: {
      ...(currentZone?.props || {}),
      detailSource: 'FORM_LAYOUT',
      detailGroups: groups,
      relationTabs: relationRows.value.map(relation => ({
        relationId: relation.id || null,
        relationName: relation.relationName,
        targetObjectCode: relation.targetObjectCode,
        targetObjectName: relation.targetObjectName,
        enabled: true,
        tabTitle: relation.tabTitle,
        defaultFilter: relation.defaultFilter,
        inlineEditEnabled: relation.inlineEditEnabled,
      })),
      showRelationTab: relationRows.value.length > 0,
      showOperationLog: detailOptions.value.showOperationLog,
      showApprovalLog: detailOptions.value.showApprovalLog,
      quantityPanels: detailOptions.value.quantityPanels.map(buildQuantityPanelPayload),
    },
  }
}

function resolveFormColumns(zone) {
  const cols = Number(zone?.props?.editGridCols || zone?.props?.canvas?.gridCols || 2)
  if ([1, 2, 3, 4].includes(cols))
    return cols
  return 2
}

function syncDetailSchema() {
  const zone = buildDetailZone(detailZone.value)
  let zones = localSchema.value.zones || []
  if (zones.some(item => item.zoneKey === 'detail'))
    zones = zones.map(item => item.zoneKey === 'detail' ? zone : item)
  else
    zones = [...zones, zone]
  const nextSchema = {
    ...localSchema.value,
    zones,
  }
  if (!isSameSchema(nextSchema, localSchema.value))
    localSchema.value = nextSchema
}

function markDirty() {
  syncDetailSchema()
  emit('dirtyChange', true)
}

async function saveLayout() {
  if (!props.objectId)
    return
  syncDetailSchema()
  saving.value = true
  try {
    const schema = syncPageSchemaWithModel(localSchema.value, effectiveModelSchema.value)
    const detail = schema.zones?.find(zone => zone.zoneKey === 'detail')
    await saveBusinessObjectDetailLayout(props.objectId, {
      layoutKey: 'detail',
      layoutName: '详情设置',
      layoutType: schema.layoutType,
      pageSchema: cloneSchema(schema),
      zones: detail ? [detail] : [],
      settings: detail?.props || {},
    })
    const viewSchema = buildCurrentViewSchema(schema)
    await saveBusinessObjectDesigner(props.objectId, {
      viewSchema: cloneSchema(viewSchema),
    })
    emit('update:viewSchema', cloneSchema(viewSchema))
    emit('saved', cloneSchema(schema))
    emit('dirtyChange', false)
    message.success('详情设置已保存')
  }
  finally {
    saving.value = false
  }
}

function syncDesignerDraft() {
  syncDetailSchema()
  const schema = syncPageSchemaWithModel(localSchema.value, effectiveModelSchema.value)
  const viewSchema = buildCurrentViewSchema(schema)
  localSchema.value = schema
  emit('update:modelValue', cloneSchema(schema))
  emit('update:viewSchema', cloneSchema(viewSchema))
  return {
    dirty: !isSameSchema(schema, props.modelValue) || !isSameSchema(viewSchema, props.viewSchema || {}),
    pageSchema: cloneSchema(schema),
    viewSchema: cloneSchema(viewSchema),
  }
}

function buildCurrentViewSchema(schema = localSchema.value) {
  return createViewSchemaFromPageSchema(schema, designFields.value, props.viewSchema || {})
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    comment: field.remark || field.fieldName,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    required: field.required,
    systemField: field.systemField,
    readonly: field.readonly,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

defineExpose({
  saveLayout,
  syncDesignerDraft,
})
</script>

<style scoped>
.business-detail-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.detail-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light);
  padding: 14px 16px;
}

.detail-designer-head h3,
.detail-section h4,
.detail-summary-pane h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  letter-spacing: 0;
}

.detail-designer-head p,
.detail-section p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.detail-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.detail-workspace {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  overflow: auto;
  background: var(--bg-secondary);
  padding: 14px;
}

.detail-section,
.detail-summary-pane section {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.detail-section-head,
.detail-toggle-row,
.relation-preview-item,
.source-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.field-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.field-preview-item,
.relation-preview-item,
.detail-toggle-row,
.source-row {
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fdfefe;
  padding: 10px 12px;
}

.field-preview-item {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.field-preview-item strong,
.relation-preview-main strong,
.source-row strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-preview-item span,
.field-preview-item em,
.relation-preview-main span,
.source-row span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-preview-list,
.detail-toggle-list,
.quantity-panel-list,
.detail-summary-pane {
  display: grid;
  gap: 10px;
}

.relation-preview-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.relation-preview-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.quantity-panel-item {
  display: grid;
  gap: 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fdfefe;
  padding: 12px;
}

.quantity-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.quantity-panel-head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.detail-summary-pane {
  align-content: start;
  border-left: 1px solid var(--border-light);
  background: var(--bg-secondary);
  padding: 12px;
}

.source-row {
  margin-top: 10px;
}

@media (max-width: 1100px) {
  .detail-designer-body {
    grid-template-columns: 1fr;
  }

  .detail-summary-pane {
    border-left: 0;
    border-top: 1px solid var(--border-light);
  }
}
</style>
