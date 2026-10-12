<template>
  <n-modal
    :show="show"
    preset="card"
    :title="isEditing ? '编辑子表分区' : '添加子表分区'"
    :bordered="false"
    class="child-table-section-wizard"
    :style="{ width: 'min(760px, calc(100vw - 32px))' }"
    @update:show="emit('update:show', $event)"
  >
    <div class="wizard-intro">
      <strong>选择关系，自动完成主子表配置</strong>
      <span>确认后会同步生成数据模型引用、级联配置和页面分区，无需手工编辑 JSON。</span>
    </div>

    <n-form label-placement="top">
      <div class="wizard-form-grid">
        <n-form-item label="关联关系" required>
          <n-select
            :value="selectedRelationKey"
            :options="relationOptions"
            :disabled="relationLocked"
            filterable
            placeholder="请选择 DETAIL 关联关系"
            @update:value="handleRelationChange"
          />
        </n-form-item>
        <n-form-item label="分区标题" required>
          <n-input
            v-model:value="sectionTitle"
            :disabled="!selectedRelation"
            placeholder="例如：订单明细"
            @update:value="titleTouched = true"
          />
        </n-form-item>
        <n-form-item label="显示模式" required>
          <n-select
            v-model:value="displayMode"
            :disabled="!selectedRelation"
            :options="displayModeOptions"
          />
        </n-form-item>
      </div>
    </n-form>

    <section class="field-selector">
      <div class="field-selector-head">
        <div>
          <strong>可见字段</strong>
          <span v-if="selectedRelation">已选择 {{ visibleFieldCodes.length }}/{{ availableFields.length }} 个</span>
          <span v-else>选择关系后自动读取子对象字段</span>
        </div>
        <n-checkbox
          v-if="availableFields.length"
          :checked="allFieldsSelected"
          :indeterminate="someFieldsSelected"
          @update:checked="toggleAllFields"
        >
          全选
        </n-checkbox>
      </div>

      <n-spin :show="loadingFields">
        <n-checkbox-group
          v-if="availableFields.length"
          v-model:value="visibleFieldCodes"
          class="field-checkbox-list"
        >
          <n-checkbox
            v-for="field in availableFields"
            :key="fieldCode(field)"
            :value="fieldCode(field)"
          >
            <span class="field-checkbox-copy">
              <strong>{{ fieldLabel(field) }}</strong>
              <small>{{ fieldCode(field) }}</small>
            </span>
          </n-checkbox>
        </n-checkbox-group>
        <n-empty
          v-else
          size="small"
          :description="selectedRelation ? '未读取到可配置字段，请保存对象字段后重试' : '请先选择关联关系'"
        />
      </n-spin>
      <div v-if="visibleFieldCodes.length" class="field-order-section">
        <div class="field-order-head">
          <strong>展示顺序</strong>
          <span>拖动字段调整子表运行时渲染顺序</span>
        </div>
        <draggable
          v-model="visibleFieldCodes"
          :item-key="item => item"
          handle=".field-drag-handle"
          class="field-order-list"
          :animation="150"
        >
          <template #item="{ element, index }">
            <div class="field-order-row">
              <span class="field-drag-handle" title="拖动排序">⋮⋮</span>
              <span class="field-order-index">{{ index + 1 }}</span>
              <span class="field-order-label">{{ resolveFieldLabelByCode(element) }}</span>
              <n-button text size="tiny" :disabled="index === 0" aria-label="上移" @click="moveVisibleField(index, -1)">
                ↑
              </n-button>
              <n-button text size="tiny" :disabled="index === visibleFieldCodes.length - 1" aria-label="下移" @click="moveVisibleField(index, 1)">
                ↓
              </n-button>
            </div>
          </template>
        </draggable>
      </div>
    </section>

    <template #footer>
      <n-space justify="end">
        <n-button @click="closeWizard">
          取消
        </n-button>
        <n-button type="primary" :disabled="loadingFields" @click="confirmWizard">
          {{ isEditing ? '保存子表分区' : '生成子表分区' }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { businessObjectFields, businessObjectList } from '@/api/business-app'
import { resolveChildTableRelationKey } from './child-table-section-config'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  relations: {
    type: Array,
    default: () => [],
  },
  modelRefs: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:show', 'confirm'])
const message = useMessage()
const selectedRelationKey = ref('')
const sectionTitle = ref('')
const displayMode = ref('inline_grid')
const availableFields = ref([])
const visibleFieldCodes = ref([])
const resolvedObject = ref(null)
const loadingFields = ref(false)
const titleTouched = ref(false)
let loadRequestId = 0

const detailRelations = computed(() => (props.relations || []).filter((relation) => {
  return String(relation?.relationType || '').toUpperCase() === 'DETAIL'
    && relation?.status !== 0
    && relation?.status !== '0'
}))
const relationOptions = computed(() => detailRelations.value.map(relation => ({
  label: `${relationTitle(relation)} · ${relation.targetObjectCode || '未配置对象'}`,
  value: resolveChildTableRelationKey(relation),
  disabled: !resolveChildTableRelationKey(relation) || !relation.targetObjectCode,
})))
const selectedRelation = computed(() => detailRelations.value.find((relation) => {
  return resolveChildTableRelationKey(relation) === selectedRelationKey.value
}) || null)
const displayModeOptions = [
  { label: '内联表格', value: 'inline_grid' },
  { label: '卡片列表', value: 'card_list' },
]
const allFieldsSelected = computed(() => {
  return Boolean(availableFields.value.length)
    && visibleFieldCodes.value.length === availableFields.value.length
})
const someFieldsSelected = computed(() => {
  return visibleFieldCodes.value.length > 0 && !allFieldsSelected.value
})
const isEditing = computed(() => Boolean(props.modelValue))
const relationLocked = computed(() => Boolean(props.modelValue?.relationKey))

watch(() => props.show, (show) => {
  if (show)
    initializeWizard()
})

async function initializeWizard() {
  resetWizard()
  const current = props.modelValue
  if (!current)
    return
  selectedRelationKey.value = current.relationKey || ''
  sectionTitle.value = current.title || ''
  displayMode.value = current.displayMode || 'inline_grid'
  titleTouched.value = true
  const relation = selectedRelation.value
  if (relation)
    await loadRelationFields(relation, current.fieldCodes || [])
}

function resetWizard() {
  selectedRelationKey.value = ''
  sectionTitle.value = ''
  displayMode.value = 'inline_grid'
  availableFields.value = []
  visibleFieldCodes.value = []
  resolvedObject.value = null
  loadingFields.value = false
  titleTouched.value = false
  loadRequestId += 1
}

async function handleRelationChange(relationKey) {
  selectedRelationKey.value = relationKey || ''
  availableFields.value = []
  visibleFieldCodes.value = []
  resolvedObject.value = null
  const relation = selectedRelation.value
  if (!relation)
    return
  if (!titleTouched.value)
    sectionTitle.value = relationTitle(relation)
  await loadRelationFields(relation)
}

async function loadRelationFields(relation, selectedFieldCodes = null) {
  const requestId = ++loadRequestId
  loadingFields.value = true
  try {
    const modelRef = findRelationModelRef(relation)
    let fields = normalizeFieldList(modelRef?.fields)
    let object = modelRef
    if (!fields.length) {
      const objectRes = await businessObjectList({ objectCode: relation.targetObjectCode })
      const objects = unwrapList(objectRes?.data)
      object = objects.find(item => item?.objectCode === relation.targetObjectCode) || objects[0] || null
      if (object?.id) {
        const fieldRes = await businessObjectFields(object.id)
        fields = normalizeFieldList(fieldRes?.data)
      }
    }
    if (requestId !== loadRequestId)
      return
    resolvedObject.value = object || modelRef || null
    availableFields.value = fields
    const availableCodes = fields.map(fieldCode).filter(Boolean)
    const selectedCodes = Array.isArray(selectedFieldCodes)
      ? [...new Set(selectedFieldCodes.map(code => String(code || '').trim()).filter(Boolean))]
      : null
    const availableSet = new Set(availableCodes)
    visibleFieldCodes.value = selectedCodes
      ? selectedCodes.filter(code => availableSet.has(code))
      : availableCodes
  }
  catch (error) {
    if (requestId !== loadRequestId)
      return
    message.error(error?.message || '子对象字段读取失败')
  }
  finally {
    if (requestId === loadRequestId)
      loadingFields.value = false
  }
}

function findRelationModelRef(relation) {
  const relationKey = resolveChildTableRelationKey(relation)
  return (props.modelRefs || []).find((ref) => {
    return !ref?.primary && (
      ref?.props?.relationKey === relationKey
      || ref?.relationKey === relationKey
      || ref?.modelCode === relation.targetObjectCode
    )
  }) || null
}

function normalizeFieldList(source) {
  return unwrapList(source)
    .filter(field => field && !field.systemField)
    .filter(field => !['DISABLED', 'HIDDEN'].includes(String(field.fieldStatus || '').toUpperCase()))
    .filter(field => Boolean(fieldCode(field)))
}

function unwrapList(value) {
  if (Array.isArray(value))
    return value
  if (Array.isArray(value?.records))
    return value.records
  if (Array.isArray(value?.list))
    return value.list
  return []
}

function toggleAllFields(checked) {
  visibleFieldCodes.value = checked ? availableFields.value.map(fieldCode).filter(Boolean) : []
}

function confirmWizard() {
  const relation = selectedRelation.value
  if (!relation) {
    message.warning('请选择关联关系')
    return
  }
  if (!sectionTitle.value.trim()) {
    message.warning('请输入分区标题')
    return
  }
  if (!visibleFieldCodes.value.length) {
    message.warning('请至少选择一个可见字段')
    return
  }
  const selectedCodes = new Set(visibleFieldCodes.value)
  const modelRef = findRelationModelRef(relation)
  const object = resolvedObject.value || modelRef || {}
  emit('confirm', {
    relation,
    relationKey: resolveChildTableRelationKey(relation),
    title: sectionTitle.value.trim(),
    displayMode: displayMode.value,
    modelCode: relation.targetObjectCode || object.modelCode || object.objectCode,
    modelName: relation.targetObjectName || object.modelName || object.objectName,
    tableName: object.tableName || modelRef?.tableName || object.configKey || object.modelCode || relation.targetObjectCode || '',
    fields: availableFields.value.filter(field => selectedCodes.has(fieldCode(field))),
  })
  closeWizard()
}

function closeWizard() {
  emit('update:show', false)
}

function relationTitle(relation = {}) {
  return relation.relationName || relation.targetObjectName || relation.targetObjectCode || '子表分区'
}

function fieldCode(field = {}) {
  return field.fieldCode || field.sourceField || field.field || ''
}

function fieldLabel(field = {}) {
  return field.fieldName || field.rawLabel || field.label || fieldCode(field)
}

function resolveFieldLabelByCode(code) {
  const field = availableFields.value.find(item => fieldCode(item) === code)
  return field ? fieldLabel(field) : code
}

function moveVisibleField(index, offset) {
  const target = index + offset
  if (target < 0 || target >= visibleFieldCodes.value.length)
    return
  const next = [...visibleFieldCodes.value]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  visibleFieldCodes.value = next
}
</script>

<style scoped>
.wizard-intro {
  display: grid;
  gap: 4px;
  margin-bottom: 18px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #f6f9ff;
  padding: 12px 14px;
}

.wizard-intro strong {
  color: #1e3a8a;
  font-size: 14px;
}

.wizard-intro span,
.field-selector-head span {
  color: var(--text-secondary);
  font-size: 12px;
}

.wizard-form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr) minmax(150px, 0.7fr);
  gap: 12px;
}

.field-selector {
  overflow: hidden;
  min-height: 190px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
}

.field-selector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
  padding: 10px 12px;
}

.field-selector-head > div {
  display: grid;
  gap: 2px;
}

.field-checkbox-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
  padding: 14px;
}

.field-order-section {
  margin-top: 12px;
}

.field-order-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--text-primary);
  font-size: 12px;
}

.field-order-head span {
  color: var(--text-tertiary);
  font-size: 11px;
}

.field-order-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 220px;
  overflow-y: auto;
  padding: 6px 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.field-order-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 4px 6px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
  background: var(--bg-primary);
}

.field-drag-handle {
  cursor: grab;
  color: var(--text-tertiary);
  letter-spacing: -2px;
}

.field-order-index {
  width: 18px;
  color: var(--text-tertiary);
  font-size: 11px;
  text-align: center;
}

.field-order-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-checkbox-copy {
  display: inline-grid;
  gap: 1px;
  margin-left: 2px;
}

.field-checkbox-copy strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
}

.field-checkbox-copy small {
  color: var(--text-tertiary);
  font-size: 11px;
}

@media (max-width: 680px) {
  .wizard-form-grid,
  .field-checkbox-grid {
    grid-template-columns: 1fr;
  }
}
</style>
