<script setup>
/**
 * ObjectActionEditor — 业务动作编辑器
 *
 * 数据结构对齐：
 * - props.fields: BusinessFieldVO[] { fieldName, fieldCode, dictType, componentType }
 * - props.relations: BusinessObjectRelationVO[] { relationType, relationName, targetObjectCode, relationConfig(JSON字符串，内含 relationKey) }
 * - props.objects: 应用内对象列表 { objectId, objectCode, objectName }
 * - actions: designerOptions.actions（targetConfigKey 指向对象 configKey）
 */
import { NButton, NEmpty, NFormItemGi, NGrid, NInput, NSelect, NSpace, NSwitch, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList } from '@/api/business-app'
import { getDictData } from '@/composables/useDict'

const props = defineProps({
  actions: { type: Array, default: () => [] },
  fields: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
  objects: { type: Array, default: () => [] },
  // 应用运行时已加载的对象设计摘要（objectId → businessObjectDesigner 响应），跨对象字段优先从这里取
  designerSummaries: { type: Object, default: () => ({}) },
  objectCode: { type: String, default: '' },
  configKey: { type: String, default: '' },
})
const emit = defineEmits(['save'])

// ===== 本地草稿（手动保存，不自动触发 API） =====
const draft = ref(clone(props.actions || []))
const selectedIndex = ref(0)
const dirty = ref(false)
const saving = ref(false)

watch(() => props.actions, (val) => {
  if (!dirty.value)
    draft.value = clone(val || [])
}, { deep: true })

watch(() => draft.value.length, (len) => {
  if (selectedIndex.value >= len)
    selectedIndex.value = Math.max(0, len - 1)
})

const selectedAction = computed(() => draft.value[selectedIndex.value] || null)
const selectedSteps = computed(() => selectedAction.value?.actionConfig?.steps || [])

// ===== 当前对象字段（兼容 FieldVO 的 fieldCode/fieldName 与设计器的 field/sourceField 两种结构） =====
const mainFields = computed(() => (props.fields || [])
  .filter(f => f && (f.fieldCode || f.field || f.sourceField))
  .map(f => ({
    value: f.fieldCode || f.field || f.sourceField,
    label: f.fieldName || f.label || f.fieldCode || f.field || f.sourceField,
    dictType: f.dictType || f.props?.dictType || '',
  })))

const currentConfigKey = computed(() =>
  String(props.configKey || props.objectCode || '').trim())

// ===== 字典动态加载（getDictData 自带全局缓存） =====
const dictOptionsMap = ref({})
const dictLoadingSet = ref(new Set())

async function ensureDict(dictType) {
  const type = String(dictType || '').trim()
  if (!type || dictOptionsMap.value[type] || dictLoadingSet.value.has(type))
    return
  dictLoadingSet.value.add(type)
  const list = await getDictData(type)
  dictOptionsMap.value = {
    ...dictOptionsMap.value,
    [type]: (list || []).map(item => ({ label: item.label, value: item.value })),
  }
}

function dictSelectOptions(dictType) {
  if (!dictType)
    return []
  return dictOptionsMap.value[dictType] || []
}

// ===== 跨对象字段加载（步骤 targetConfigKey → 目标对象字段） =====
const targetFieldsMap = ref({}) // configKey → fields[]
const targetFieldLoading = ref(new Set())

function normalizeFields(fields) {
  return (fields || [])
    .filter(f => f && (f.fieldCode || f.field || f.sourceField))
    .map(f => ({
      value: f.fieldCode || f.field || f.sourceField,
      label: f.fieldName || f.label || f.fieldCode || f.field || f.sourceField,
      dictType: f.dictType || f.props?.dictType || '',
    }))
}

async function ensureTargetFields(configKey) {
  const key = String(configKey || '').trim()
  // 已有非空结果视为缓存命中；空结果（匹配失败缓存）允许摘要到达后重试
  const cached = targetFieldsMap.value[key]
  if (!key || key.toLowerCase() === currentConfigKey.value.toLowerCase() || (cached && cached.length) || targetFieldLoading.value.has(key))
    return
  targetFieldLoading.value.add(key)
  const applyFields = (fields) => {
    targetFieldsMap.value = { ...targetFieldsMap.value, [key]: normalizeFields(fields) }
  }
  try {
    // 优先复用父组件已加载的设计摘要（configKey 小写形式直接匹配），零 API 成本
    const summary = Object.values(props.designerSummaries || {}).find(item =>
      String(item?.configKey || '').toLowerCase() === key.toLowerCase()
      || String(item?.objectCode || '').toLowerCase() === key.toLowerCase())
    if (summary?.fields?.length) {
      applyFields(summary.fields)
      return
    }
    // configKey 为小写形式，objectCode 为大写形式，两种都尝试匹配
    let target = (props.objects || []).find(item =>
      item.configKey === key
      || String(item.objectCode || '').toLowerCase() === key.toLowerCase())
    if (!target?.objectId && !target?.id) {
      const res = await businessObjectList({ configKey: key })
      target = (res?.data || [])[0]
    }
    if (!target?.objectId && !target?.id) {
      // objectCode 在库中为大写形式，key 为小写 configKey，转大写再查一次
      const res = await businessObjectList({ objectCode: key.toUpperCase() })
      target = (res?.data || [])[0]
    }
    const objectId = target?.objectId || target?.id
    if (!objectId) {
      applyFields([])
      return
    }
    const designerRes = await businessObjectDesigner(objectId)
    applyFields(designerRes?.data?.fields || [])
  }
  catch {
    applyFields([])
  }
  finally {
    targetFieldLoading.value.delete(key)
  }
}

// 监听步骤中的目标对象（附带摘要就绪信号：summaries 异步到达后重试取字段），自动加载其字段
watch(() => {
  const keys = selectedSteps.value.map(s => s?.stepConfig?.targetConfigKey).filter(Boolean)
  return `${keys.join(',')}|${Object.keys(props.designerSummaries || {}).length}`
}, (val) => {
  const keys = String(val || '').split('|')[0].split(',').filter(Boolean)
  keys.forEach(ensureTargetFields)
}, { immediate: true })

// 所有涉及字典预加载（含主对象字段字典）
watch(() => [
  ...mainFields.value.map(f => f.dictType),
  ...Object.values(targetFieldsMap.value).flat().map(f => f.dictType),
].filter(Boolean).join(','), (val) => {
  String(val || '').split(',').filter(Boolean).forEach(ensureDict)
}, { immediate: true })

// ===== 步骤目标对象的字段选项 =====
function stepFields(step) {
  const key = String(step?.stepConfig?.targetConfigKey || '').trim()
  if (!key || key === currentConfigKey.value)
    return mainFields.value
  return targetFieldsMap.value[key] || []
}

function stepFieldOptions(step) {
  const fields = stepFields(step)
  return fields.map(f => ({ label: `${f.label}（${f.value}）`, value: f.value }))
}

function stepFieldDictType(step, fieldCode) {
  if (!fieldCode)
    return ''
  return stepFields(step).find(f => f.value === fieldCode)?.dictType || ''
}

// 状态值下拉（根据步骤的状态字段找字典；无字典时退化为输入框）
function statusOptionsForStep(step, fieldKey) {
  const fieldCode = fieldKey === 'fromValue' || fieldKey === 'toValue'
    ? step?.stepConfig?.statusField
    : Object.keys(step?.stepConfig?.expectedValues || {})[0]
  const dictType = stepFieldDictType(step, fieldCode)
  return dictSelectOptions(dictType)
}

// 应用内对象选项（步骤目标对象可选）
const objectOptions = computed(() => [
  { label: '当前对象', value: currentConfigKey.value },
  ...(props.objects || [])
    .filter((item) => {
      const key = item.configKey || item.objectCode
      return key && key !== currentConfigKey.value
    })
    .map(item => ({
      label: item.objectName || item.objectCode,
      value: item.configKey || item.objectCode,
    })),
])

// ===== 子表关系（解析 relationConfig JSON 字符串取 relationKey） =====
const childRelationOptions = computed(() => (props.relations || [])
  .filter(r => r && String(r.relationType || '').toUpperCase() === 'ONE_TO_MANY')
  .map((r) => {
    let relationKey = r.relationKey || ''
    let parsed = null
    try {
      parsed = r.relationConfig ? JSON.parse(r.relationConfig) : null
    }
    catch {
      parsed = null
    }
    relationKey = relationKey || parsed?.relationKey || parsed?.collectionKey || r.targetObjectCode
    return {
      label: r.relationName || parsed?.relationName || relationKey,
      value: relationKey,
    }
  })
  .filter(item => item.value))

// ===== 来源字段智能选项 =====
// 用户输入字段（inputSchema，如"提货数量"）
const inputFieldOptions = computed(() =>
  (selectedAction.value?.actionConfig?.inputSchema || [])
    .filter(f => f && f.name)
    .map(f => ({ label: f.label || f.name, value: f.name })))

// 当前对象字段选项（parent 来源、目标为当前对象时使用）
const mainObjectFieldOptions = computed(() =>
  mainFields.value.map(f => ({ label: `${f.label}（${f.value}）`, value: f.value })))

// 子表对象字段（动作作用于子表行时 record 指子表记录），按 relationKey 缓存
const childObjectFieldsMap = computed(() => {
  const map = {}
  ;(props.relations || []).forEach((r) => {
    if (!r || String(r.relationType || '').toUpperCase() !== 'ONE_TO_MANY')
      return
    let parsed = null
    try {
      parsed = r.relationConfig ? JSON.parse(r.relationConfig) : null
    }
    catch {
      parsed = null
    }
    const relationKey = parsed?.relationKey || parsed?.collectionKey || r.targetObjectCode
    const targetCode = parsed?.targetObjectCode || r.targetObjectCode
    const summary = targetCode
      ? Object.values(props.designerSummaries || {}).find(item =>
          String(item?.objectCode || '').toLowerCase() === String(targetCode).toLowerCase())
      : null
    if (summary?.fields?.length && relationKey)
      map[relationKey] = normalizeFields(summary.fields)
  })
  return map
})

// 系统取值
const SYSTEM_FIELD_OPTIONS = [
  { label: '当前用户姓名', value: 'realName' },
  { label: '当前用户ID', value: 'userId' },
  { label: '当前时间', value: 'now' },
  { label: '当前日期', value: 'today' },
]

// 按取值方式返回来源字段选项：form=用户输入、record=子表行/当前记录、parent=父记录（主单）、system=系统
function sourceFieldOptions(mapping) {
  const type = mapping?.sourceType
  if (type === 'form')
    return inputFieldOptions.value
  if (type === 'record') {
    const relationKey = selectedAction.value?.actionConfig?.relationKey
    const fields = relationKey ? childObjectFieldsMap.value[relationKey] : null
    return (fields || mainFields.value).map(f => ({ label: `${f.label}（${f.value}）`, value: f.value }))
  }
  if (type === 'parent')
    return mainObjectFieldOptions.value
  if (type === 'system')
    return SYSTEM_FIELD_OPTIONS
  return []
}

// ===== 常量 =====
const STEP_TYPE_MAP = {
  ASSERT_RECORD: '校验条件',
  TRANSITION_STATUS: '变更状态',
  CREATE_RECORD: '创建记录',
  ADJUST_NUMBER: '调整数量',
}
const STEP_TYPE_OPTIONS = Object.entries(STEP_TYPE_MAP).map(([value, label]) => ({ label, value }))
const OPERATOR_OPTIONS = [
  { label: '增加', value: 'ADD' },
  { label: '减少', value: 'SUBTRACT' },
]
const CONSTRAINT_OPERATOR_OPTIONS = [
  { label: '大于等于', value: 'gte' },
  { label: '小于等于', value: 'lte' },
  { label: '等于', value: 'eq' },
]
const POSITION_OPTIONS = [
  { label: '列表行按钮', value: 'ROW' },
  { label: '详情页按钮', value: 'DETAIL' },
  { label: '子表行按钮', value: 'CHILD_ROW' },
]
const INPUT_TYPE_OPTIONS = [
  { label: '数字', value: 'INTEGER' },
  { label: '文本', value: 'TEXT' },
]
const SOURCE_TYPE_OPTIONS = [
  { label: '固定值', value: 'static' },
  { label: '当前行字段', value: 'record' },
  { label: '主单字段', value: 'parent' },
  { label: '用户输入', value: 'form' },
  { label: '系统值', value: 'system' },
]

// ===== 编辑操作（全部修改本地 draft） =====
function mutate(fn) {
  const next = clone(draft.value)
  fn(next)
  draft.value = next
  dirty.value = true
}

function patchAction(patch) {
  mutate((next) => {
    if (next[selectedIndex.value])
      next[selectedIndex.value] = { ...next[selectedIndex.value], ...patch }
  })
}

function patchActionConfig(patch) {
  mutate((next) => {
    const action = next[selectedIndex.value]
    if (!action)
      return
    action.actionConfig = { ...(action.actionConfig || {}), ...patch }
  })
}

function patchStep(stepIndex, patch) {
  mutate((next) => {
    const steps = next[selectedIndex.value]?.actionConfig?.steps
    if (steps?.[stepIndex])
      steps[stepIndex] = { ...steps[stepIndex], ...patch }
  })
}

function patchStepConfig(stepIndex, patch) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (step)
      step.stepConfig = { ...(step.stepConfig || {}), ...patch }
  })
}

function addAction() {
  mutate((next) => {
    next.push({
      actionCode: `action_${Date.now()}`,
      actionName: '新操作',
      actionPosition: 'ROW',
      actionType: 'COMMAND',
      permission: '',
      confirmRequired: true,
      successMessage: '',
      status: 1,
      sortOrder: (next.length + 1) * 10,
      actionConfig: { triggerScene: 'MANUAL', executionMode: 'LOCAL_TRANSACTION', inputSchema: [], steps: [] },
    })
  })
  selectedIndex.value = draft.value.length - 1
}

function removeAction(index) {
  mutate(next => next.splice(index, 1))
  if (selectedIndex.value >= draft.value.length)
    selectedIndex.value = Math.max(0, draft.value.length - 1)
}

function addStep(stepType) {
  mutate((next) => {
    const action = next[selectedIndex.value]
    if (!action)
      return
    action.actionConfig = action.actionConfig || {}
    action.actionConfig.steps = action.actionConfig.steps || []
    const maxSort = action.actionConfig.steps.reduce((max, s) => Math.max(max, s.sortOrder || 0), 0)
    action.actionConfig.steps.push({
      stepCode: `step_${Date.now()}`,
      stepName: STEP_TYPE_MAP[stepType] || '新步骤',
      stepType,
      sortOrder: maxSort + 10,
      rollbackOnFailure: true,
      stepConfig: stepType === 'TRANSITION_STATUS'
        ? { targetConfigKey: currentConfigKey.value, targetRecordIdField: 'record.id', statusField: 'status', fromValue: '', toValue: '' }
        : { targetConfigKey: currentConfigKey.value },
    })
  })
}

function removeStep(stepIndex) {
  mutate((next) => {
    const steps = next[selectedIndex.value]?.actionConfig?.steps
    if (steps)
      steps.splice(stepIndex, 1)
  })
}

function moveStep(stepIndex, direction) {
  mutate((next) => {
    const steps = next[selectedIndex.value]?.actionConfig?.steps
    if (!steps)
      return
    const target = stepIndex + direction
    if (target < 0 || target >= steps.length) {
      return
    }
    const moved = steps[stepIndex]
    steps[stepIndex] = steps[target]
    steps[target] = moved
  })
}

// ASSERT_RECORD expectedValues（对象键值对）
function updateExpected(stepIndex, field, value, oldField) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    const values = { ...(step.stepConfig?.expectedValues || {}) }
    if (oldField && oldField !== field)
      delete values[oldField]
    values[field] = value
    step.stepConfig = { ...(step.stepConfig || {}), expectedValues: values }
  })
}

function removeExpected(stepIndex, field) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    const values = { ...(step.stepConfig?.expectedValues || {}) }
    delete values[field]
    step.stepConfig = { ...(step.stepConfig || {}), expectedValues: values }
  })
}

function addExpected(stepIndex) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    const values = { ...(step.stepConfig?.expectedValues || {}) }
    values[''] = ''
    step.stepConfig = { ...(step.stepConfig || {}), expectedValues: values }
  })
}

// CREATE_RECORD 字段映射
function patchFieldMapping(stepIndex, mappingIndex, patch) {
  mutate((next) => {
    const mappings = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.fieldMappings
    if (mappings?.[mappingIndex])
      mappings[mappingIndex] = { ...mappings[mappingIndex], ...patch }
  })
}

function addFieldMapping(stepIndex) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    step.stepConfig = step.stepConfig || {}
    step.stepConfig.fieldMappings = [...(step.stepConfig.fieldMappings || []), { targetField: '', sourceType: 'static', value: '' }]
  })
}

function removeFieldMapping(stepIndex, mappingIndex) {
  mutate((next) => {
    const mappings = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.fieldMappings
    if (mappings)
      mappings.splice(mappingIndex, 1)
  })
}

// CREATE_RECORD 固定值（staticValues 键值对，如 操作类型=提货）
function updateStaticValue(stepIndex, field, value, oldField) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    const values = { ...(step.stepConfig?.staticValues || {}) }
    if (oldField && oldField !== field)
      delete values[oldField]
    values[field] = value
    step.stepConfig = { ...(step.stepConfig || {}), staticValues: values }
  })
}

function removeStaticValue(stepIndex, field) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    const values = { ...(step.stepConfig?.staticValues || {}) }
    delete values[field]
    step.stepConfig = { ...(step.stepConfig || {}), staticValues: values }
  })
}

function addStaticValue(stepIndex) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    const values = { ...(step.stepConfig?.staticValues || {}) }
    values[''] = ''
    step.stepConfig = { ...(step.stepConfig || {}), staticValues: values }
  })
}

// 记录定位条件（expectedFieldMappings：目标字段 = 来源值，用于锁定要操作的那条记录）
function patchExpectedMapping(stepIndex, mappingIndex, patch) {
  mutate((next) => {
    const mappings = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.expectedFieldMappings
    if (mappings?.[mappingIndex])
      mappings[mappingIndex] = { ...mappings[mappingIndex], ...patch }
  })
}

function addExpectedMapping(stepIndex) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    step.stepConfig = step.stepConfig || {}
    step.stepConfig.expectedFieldMappings = [...(step.stepConfig.expectedFieldMappings || []), { targetField: '', sourceType: 'parent', sourceField: 'id' }]
  })
}

function removeExpectedMapping(stepIndex, mappingIndex) {
  mutate((next) => {
    const mappings = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.expectedFieldMappings
    if (mappings)
      mappings.splice(mappingIndex, 1)
  })
}

// 数值校验（numericConstraints：字段 必须 >=/<= 取值，如 待提数量 >= 提货数量）
function patchConstraint(stepIndex, constraintIndex, patch) {
  mutate((next) => {
    const list = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.numericConstraints
    if (list?.[constraintIndex])
      list[constraintIndex] = { ...list[constraintIndex], ...patch }
  })
}

function addConstraint(stepIndex) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    step.stepConfig = step.stepConfig || {}
    step.stepConfig.numericConstraints = [...(step.stepConfig.numericConstraints || []), { field: '', operator: 'gte', sourceType: 'form', sourceField: '' }]
  })
}

function removeConstraint(stepIndex, constraintIndex) {
  mutate((next) => {
    const list = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.numericConstraints
    if (list)
      list.splice(constraintIndex, 1)
  })
}

// ADJUST_NUMBER 数值调整
function patchAdjustment(stepIndex, adjIndex, patch) {
  mutate((next) => {
    const adjs = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.adjustments
    if (adjs?.[adjIndex])
      adjs[adjIndex] = { ...adjs[adjIndex], ...patch }
  })
}

function addAdjustment(stepIndex) {
  mutate((next) => {
    const step = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]
    if (!step)
      return
    step.stepConfig = step.stepConfig || {}
    step.stepConfig.adjustments = [...(step.stepConfig.adjustments || []), { targetField: '', operator: 'ADD', sourceType: 'form', sourceField: '' }]
  })
}

function removeAdjustment(stepIndex, adjIndex) {
  mutate((next) => {
    const adjs = next[selectedIndex.value]?.actionConfig?.steps?.[stepIndex]?.stepConfig?.adjustments
    if (adjs)
      adjs.splice(adjIndex, 1)
  })
}

// 输入字段
function addInputField() {
  patchActionConfig({
    inputSchema: [...(selectedAction.value?.actionConfig?.inputSchema || []), { name: '', label: '', type: 'INTEGER', required: true }],
  })
}

function patchInputField(index, patch) {
  mutate((next) => {
    const schema = next[selectedIndex.value]?.actionConfig?.inputSchema
    if (schema?.[index])
      schema[index] = { ...schema[index], ...patch }
  })
}

function removeInputField(index) {
  mutate((next) => {
    const schema = next[selectedIndex.value]?.actionConfig?.inputSchema
    if (schema)
      schema.splice(index, 1)
  })
}

// 保存 / 还原
async function handleSave() {
  saving.value = true
  try {
    emit('save', clone(draft.value))
    dirty.value = false
  }
  finally {
    saving.value = false
  }
}

function handleReset() {
  draft.value = clone(props.actions || [])
  dirty.value = false
}

function clone(val) {
  try {
    return JSON.parse(JSON.stringify(val || []))
  }
  catch {
    return []
  }
}

defineExpose({ addAction })
</script>

<template>
  <div class="object-action-editor">
    <!-- 操作 Tab + 新增 -->
    <div class="action-toolbar">
      <div class="action-tabs">
        <button
          v-for="(action, i) in draft"
          :key="i"
          type="button"
          class="action-tab"
          :class="{ active: selectedIndex === i }"
          @click="selectedIndex = i"
        >
          <span class="tab-name">{{ action.actionName || action.actionCode || `操作 ${i + 1}` }}</span>
          <NTag v-if="action.status === 0" :bordered="false" size="tiny" type="warning">
            停用
          </NTag>
        </button>
      </div>
      <div class="toolbar-actions">
        <NButton size="small" @click="removeAction(selectedIndex)">
          删除当前
        </NButton>
        <NButton size="small" type="primary" secondary @click="addAction">
          ＋ 新增操作
        </NButton>
      </div>
    </div>

    <NEmpty v-if="!draft.length" description="暂无业务操作，点击「新增操作」创建">
      <template #extra>
        <NButton type="primary" size="small" @click="addAction">
          ＋ 新增操作
        </NButton>
      </template>
    </NEmpty>

    <div v-if="selectedAction" class="action-detail">
      <!-- 基本信息 -->
      <div class="detail-card">
        <div class="card-title">
          基本信息
        </div>
        <NGrid :cols="2" :x-gap="16" :y-gap="8" responsive="screen">
          <NFormItemGi label="操作名称">
            <NInput :value="selectedAction.actionName || ''" placeholder="如：提交预售单" @update:value="patchAction({ actionName: $event })" />
          </NFormItemGi>
          <NFormItemGi label="按钮位置">
            <NSelect :value="selectedAction.actionPosition || 'ROW'" :options="POSITION_OPTIONS" @update:value="patchAction({ actionPosition: $event })" />
          </NFormItemGi>
          <NFormItemGi label="启用状态">
            <NSwitch :value="selectedAction.status !== 0" @update:value="patchAction({ status: $event ? 1 : 0 })" />
          </NFormItemGi>
          <NFormItemGi label="成功提示">
            <NInput :value="selectedAction.successMessage || ''" placeholder="操作成功后的提示语" @update:value="patchAction({ successMessage: $event })" />
          </NFormItemGi>
          <NFormItemGi v-if="selectedAction.actionPosition === 'CHILD_ROW'" label="关联子表" :span="2">
            <NSelect
              :value="selectedAction.actionConfig?.relationKey || ''"
              :options="childRelationOptions"
              placeholder="选择子表（需先在「关系与级联」中配置一对多关系）"
              @update:value="patchActionConfig({ relationKey: $event })"
            />
          </NFormItemGi>
        </NGrid>
      </div>

      <!-- 用户输入字段 -->
      <div class="detail-card">
        <div class="card-title-row">
          <span class="card-title">用户输入字段</span>
          <NButton size="tiny" secondary @click="addInputField">
            添加
          </NButton>
        </div>
        <p class="card-hint">
          执行操作时弹窗让用户填写的字段，如"提货数量"。无需输入则不添加。
        </p>
        <NEmpty v-if="!(selectedAction.actionConfig?.inputSchema || []).length" description="该操作无需用户输入" size="small" />
        <div v-for="(field, fi) in (selectedAction.actionConfig?.inputSchema || [])" :key="fi" class="inline-row">
          <NInput :value="field.label || ''" placeholder="显示名称（如：提货数量）" @update:value="patchInputField(fi, { label: $event })" />
          <NInput :value="field.name || ''" placeholder="字段标识（如：quantity）" @update:value="patchInputField(fi, { name: $event })" />
          <NSelect :value="field.type || 'INTEGER'" :options="INPUT_TYPE_OPTIONS" style="width: 110px" @update:value="patchInputField(fi, { type: $event })" />
          <NSwitch :value="field.required !== false" size="small" @update:value="patchInputField(fi, { required: $event })" />
          <NButton size="tiny" quaternary type="error" @click="removeInputField(fi)">
            删除
          </NButton>
        </div>
      </div>

      <!-- 执行步骤 -->
      <div class="detail-card">
        <div class="card-title-row">
          <span class="card-title">执行步骤</span>
          <NSpace :size="4">
            <NButton size="tiny" secondary @click="addStep('ASSERT_RECORD')">
              ＋ 校验条件
            </NButton>
            <NButton size="tiny" secondary @click="addStep('TRANSITION_STATUS')">
              ＋ 变更状态
            </NButton>
            <NButton size="tiny" secondary @click="addStep('CREATE_RECORD')">
              ＋ 创建记录
            </NButton>
            <NButton size="tiny" secondary @click="addStep('ADJUST_NUMBER')">
              ＋ 调整数量
            </NButton>
          </NSpace>
        </div>
        <p class="card-hint">
          步骤从上到下顺序执行，任一步骤失败则整体回滚。
        </p>
        <NEmpty v-if="!selectedSteps.length" description="暂无执行步骤，点击上方按钮添加" size="small" />

        <div v-for="(step, si) in selectedSteps" :key="si" class="step-card">
          <div class="step-head">
            <span class="step-number">{{ si + 1 }}</span>
            <NSelect :value="step.stepType || ''" :options="STEP_TYPE_OPTIONS" style="width: 130px" @update:value="patchStep(si, { stepType: $event })" />
            <NInput :value="step.stepName || ''" placeholder="步骤说明" class="step-name" @update:value="patchStep(si, { stepName: $event })" />
            <NSpace :size="2">
              <NButton size="tiny" quaternary :disabled="si === 0" @click="moveStep(si, -1)">
                ↑
              </NButton>
              <NButton size="tiny" quaternary :disabled="si === selectedSteps.length - 1" @click="moveStep(si, 1)">
                ↓
              </NButton>
              <NButton size="tiny" quaternary type="error" @click="removeStep(si)">
                删除
              </NButton>
            </NSpace>
          </div>

          <!-- 目标对象（非变更状态类步骤都需要） -->
          <div class="step-body">
            <div v-if="step.stepType !== 'TRANSITION_STATUS' || true" class="target-row">
              <span class="target-label">目标对象</span>
              <NSelect
                :value="step.stepConfig?.targetConfigKey || currentConfigKey"
                :options="objectOptions"
                style="max-width: 260px"
                @update:value="patchStepConfig(si, { targetConfigKey: $event })"
              />
              <span v-if="targetFieldLoading.has(step.stepConfig?.targetConfigKey)" class="loading-hint">字段加载中…</span>
            </div>

            <!-- 变更状态 -->
            <template v-if="step.stepType === 'TRANSITION_STATUS'">
              <NGrid :cols="3" :x-gap="12" :y-gap="8" responsive="screen">
                <NFormItemGi label="状态字段">
                  <NSelect
                    filterable
                    :value="step.stepConfig?.statusField || ''"
                    :options="stepFieldOptions(step)"
                    placeholder="选择状态字段"
                    @update:value="patchStepConfig(si, { statusField: $event })"
                  />
                </NFormItemGi>
                <NFormItemGi label="从状态">
                  <NSelect
                    v-if="statusOptionsForStep(step, 'fromValue').length"
                    :value="step.stepConfig?.fromValue ?? ''"
                    :options="statusOptionsForStep(step, 'fromValue')"
                    placeholder="选择来源状态"
                    @update:value="patchStepConfig(si, { fromValue: $event })"
                  />
                  <NInput v-else :value="step.stepConfig?.fromValue ?? ''" placeholder="来源状态值" @update:value="patchStepConfig(si, { fromValue: $event })" />
                </NFormItemGi>
                <NFormItemGi label="到状态">
                  <NSelect
                    v-if="statusOptionsForStep(step, 'toValue').length"
                    :value="step.stepConfig?.toValue ?? ''"
                    :options="statusOptionsForStep(step, 'toValue')"
                    placeholder="选择目标状态"
                    @update:value="patchStepConfig(si, { toValue: $event })"
                  />
                  <NInput v-else :value="step.stepConfig?.toValue ?? ''" placeholder="目标状态值" @update:value="patchStepConfig(si, { toValue: $event })" />
                </NFormItemGi>
              </NGrid>
            </template>

            <!-- 校验条件（expectedValues 键值对） -->
            <template v-else-if="step.stepType === 'ASSERT_RECORD'">
              <div class="mapping-head">
                <span>必须满足的条件（字段 = 期望值）</span>
                <NButton size="tiny" secondary @click="addExpected(si)">
                  添加条件
                </NButton>
              </div>
              <div v-for="(value, field) in (step.stepConfig?.expectedValues || {})" :key="field" class="inline-row">
                <NSelect
                  filterable
                  :value="field"
                  :options="stepFieldOptions(step)"
                  placeholder="选择字段"
                  @update:value="updateExpected(si, $event, value, field)"
                />
                <NSelect
                  v-if="dictSelectOptions(stepFieldDictType(step, field)).length"
                  :value="value"
                  :options="dictSelectOptions(stepFieldDictType(step, field))"
                  placeholder="期望值"
                  @update:value="updateExpected(si, field, $event, field)"
                />
                <NInput v-else :value="value" placeholder="期望值" @update:value="updateExpected(si, field, $event, field)" />
                <NButton size="tiny" quaternary type="error" @click="removeExpected(si, field)">
                  删除
                </NButton>
              </div>
              <NEmpty v-if="!Object.keys(step.stepConfig?.expectedValues || {}).length" description="未配置条件时仅校验记录存在" size="small" />
              <div class="mapping-head" style="margin-top: 12px">
                <span>数值校验（字段 必须 ≥/≤ 取值）</span>
                <NButton size="tiny" secondary @click="addConstraint(si)">
                  添加校验
                </NButton>
              </div>
              <div v-for="(nc, nci) in (step.stepConfig?.numericConstraints || [])" :key="nci" class="inline-row">
                <NSelect
                  filterable
                  :value="nc.field || ''"
                  :options="stepFieldOptions(step)"
                  placeholder="校验哪个字段"
                  @update:value="patchConstraint(si, nci, { field: $event })"
                />
                <NSelect :value="nc.operator || 'gte'" :options="CONSTRAINT_OPERATOR_OPTIONS" style="width: 110px" @update:value="patchConstraint(si, nci, { operator: $event })" />
                <NSelect
                  filterable
                  :value="nc.sourceField || ''"
                  :options="sourceFieldOptions(nc)"
                  placeholder="和什么比"
                  @update:value="patchConstraint(si, nci, { sourceType: nc.sourceType || 'form', sourceField: $event })"
                />
                <NButton size="tiny" quaternary type="error" @click="removeConstraint(si, nci)">
                  删除
                </NButton>
              </div>
            </template>

            <!-- 创建记录 -->
            <template v-else-if="step.stepType === 'CREATE_RECORD'">
              <div class="mapping-head">
                <span>字段赋值（往目标对象写入的数据）</span>
                <NButton size="tiny" secondary @click="addFieldMapping(si)">
                  添加字段
                </NButton>
              </div>
              <div v-for="(m, mi) in (step.stepConfig?.fieldMappings || [])" :key="mi" class="inline-row">
                <NSelect
                  filterable
                  :value="m.targetField || ''"
                  :options="stepFieldOptions(step)"
                  placeholder="目标字段（写入哪个字段）"
                  @update:value="patchFieldMapping(si, mi, { targetField: $event })"
                />
                <NSelect
                  :value="m.sourceType || 'static'"
                  :options="SOURCE_TYPE_OPTIONS"
                  style="width: 132px"
                  placeholder="取值方式"
                  @update:value="patchFieldMapping(si, mi, { sourceType: $event })"
                />
                <NInput
                  v-if="m.sourceType === 'static'"
                  :value="m.value ?? ''"
                  placeholder="输入固定值"
                  @update:value="patchFieldMapping(si, mi, { value: $event })"
                />
                <NSelect
                  v-else
                  filterable
                  :value="m.sourceField || ''"
                  :options="sourceFieldOptions(m)"
                  placeholder="选择取值来源"
                  @update:value="patchFieldMapping(si, mi, { sourceField: $event })"
                />
                <NButton size="tiny" quaternary type="error" @click="removeFieldMapping(si, mi)">
                  删除
                </NButton>
              </div>
              <NEmpty v-if="!(step.stepConfig?.fieldMappings || []).length" description="未配置字段赋值" size="small" />
              <div class="mapping-head" style="margin-top: 12px">
                <span>固定值（直接写入，无需取值来源）</span>
                <NButton size="tiny" secondary @click="addStaticValue(si)">
                  添加固定值
                </NButton>
              </div>
              <div v-for="(sv, sf) in (step.stepConfig?.staticValues || {})" :key="sf" class="inline-row">
                <NSelect
                  filterable
                  :value="sf"
                  :options="stepFieldOptions(step)"
                  placeholder="目标字段"
                  @update:value="updateStaticValue(si, $event, sv, sf)"
                />
                <NSelect
                  v-if="dictSelectOptions(stepFieldDictType(step, sf)).length"
                  :value="sv"
                  :options="dictSelectOptions(stepFieldDictType(step, sf))"
                  placeholder="选择值"
                  @update:value="updateStaticValue(si, sf, $event, sf)"
                />
                <NInput
                  v-else
                  :value="sv"
                  placeholder="固定值（如 PICKUP）"
                  @update:value="updateStaticValue(si, sf, $event, sf)"
                />
                <NButton size="tiny" quaternary type="error" @click="removeStaticValue(si, sf)">
                  删除
                </NButton>
              </div>
            </template>

            <!-- 调整数量 -->
            <template v-else-if="step.stepType === 'ADJUST_NUMBER'">
              <div class="mapping-head">
                <span>数值调整（在原值基础上增减）</span>
                <NButton size="tiny" secondary @click="addAdjustment(si)">
                  添加
                </NButton>
              </div>
              <div v-for="(adj, ai) in (step.stepConfig?.adjustments || [])" :key="ai" class="inline-row">
                <NSelect
                  filterable
                  :value="adj.targetField || ''"
                  :options="stepFieldOptions(step)"
                  placeholder="调整哪个字段"
                  @update:value="patchAdjustment(si, ai, { targetField: $event })"
                />
                <NSelect :value="adj.operator || 'ADD'" :options="OPERATOR_OPTIONS" style="width: 100px" @update:value="patchAdjustment(si, ai, { operator: $event })" />
                <NSelect
                  filterable
                  :value="adj.sourceField || ''"
                  :options="sourceFieldOptions(adj)"
                  placeholder="按什么值调整"
                  @update:value="patchAdjustment(si, ai, { sourceType: adj.sourceType || 'form', sourceField: $event })"
                />
                <NButton size="tiny" quaternary type="error" @click="removeAdjustment(si, ai)">
                  删除
                </NButton>
              </div>
              <NEmpty v-if="!(step.stepConfig?.adjustments || []).length" description="未配置调整字段" size="small" />
              <div class="mapping-head" style="margin-top: 12px">
                <span>记录定位（找到要调整的那条记录）</span>
                <NButton size="tiny" secondary @click="addExpectedMapping(si)">
                  添加条件
                </NButton>
              </div>
              <div v-for="(em, emi) in (step.stepConfig?.expectedFieldMappings || [])" :key="emi" class="inline-row">
                <NSelect
                  filterable
                  :value="em.targetField || ''"
                  :options="stepFieldOptions(step)"
                  placeholder="目标字段"
                  @update:value="patchExpectedMapping(si, emi, { targetField: $event })"
                />
                <NSelect
                  :value="em.sourceType || 'parent'"
                  :options="SOURCE_TYPE_OPTIONS"
                  style="width: 132px"
                  @update:value="patchExpectedMapping(si, emi, { sourceType: $event })"
                />
                <NSelect
                  v-if="em.sourceType !== 'static'"
                  filterable
                  :value="em.sourceField || ''"
                  :options="sourceFieldOptions(em)"
                  placeholder="等于哪个来源值"
                  @update:value="patchExpectedMapping(si, emi, { sourceField: $event })"
                />
                <NInput
                  v-else
                  :value="em.value ?? ''"
                  placeholder="固定值"
                  @update:value="patchExpectedMapping(si, emi, { value: $event })"
                />
                <NButton size="tiny" quaternary type="error" @click="removeExpectedMapping(si, emi)">
                  删除
                </NButton>
              </div>
            </template>

            <!-- 其他类型（FOREACH / CALL_API 等）只读提示 -->
            <template v-else>
              <NTag :bordered="false" type="info" size="small">
                {{ step.stepType }} — 该步骤类型为高级配置，此处仅展示
              </NTag>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部保存栏 -->
    <div v-if="draft.length" class="editor-footer">
      <NSpace>
        <NButton :loading="saving" type="primary" @click="handleSave">
          保存
        </NButton>
        <NButton v-if="dirty" @click="handleReset">
          还原
        </NButton>
      </NSpace>
      <span v-if="dirty" class="dirty-hint">有未保存的修改</span>
    </div>
  </div>
</template>

<style scoped>
.object-action-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.action-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.action-tabs {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.action-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: var(--bg-primary);
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.15s;
}
.action-tab:hover {
  border-color: #36ad6a;
  color: #36ad6a;
}
.action-tab.active {
  border-color: #36ad6a;
  background: #f0faf4;
  color: #18a058;
  font-weight: 600;
}
.tab-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toolbar-actions {
  display: flex;
  gap: 6px;
}
.detail-card {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 14px 16px;
  background: #fafbfc;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}
.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.card-title-row .card-title {
  margin-bottom: 0;
}
.card-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
.step-card {
  border: 1px solid var(--border-light);
  border-radius: 6px;
  margin-bottom: 10px;
  background: var(--bg-primary);
  overflow: hidden;
}
.step-card:last-child {
  margin-bottom: 0;
}
.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
}
.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #18a058;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.step-name {
  flex: 1;
  min-width: 0;
}
.step-body {
  padding: 12px;
}
.target-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.target-label {
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.loading-hint {
  font-size: 12px;
  color: var(--text-tertiary);
}
.mapping-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}
.inline-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.inline-row > * {
  flex: 1;
  min-width: 0;
}
.editor-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0 0;
  border-top: 1px solid var(--border-light);
}
.dirty-hint {
  font-size: 12px;
  color: #e6a23c;
}
</style>
