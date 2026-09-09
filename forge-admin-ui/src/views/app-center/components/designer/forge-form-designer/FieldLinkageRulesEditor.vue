<template>
  <section class="field-linkage-editor">
    <div class="field-linkage-editor__head">
      <div>
        <strong>字段联动</strong>
        <p>一个字段变化时自动控制另一个字段：级联字典选项、过滤引用数据、清空内容。</p>
      </div>
      <n-button size="tiny" type="primary" secondary @click="openCreate">
        新增联动
      </n-button>
    </div>

    <div v-if="rules.length" class="field-linkage-list">
      <article
        v-for="(rule, index) in rules"
        :key="rule.ruleId || index"
        class="field-linkage-card"
        :class="{ 'field-linkage-card--disabled': rule.enabled === false }"
      >
        <div class="field-linkage-card__main">
          <div class="field-linkage-card__title">
            <span class="field-linkage-card__field">{{ fieldLabel(rule.sourceField) }}</span>
            <span class="field-linkage-card__type">{{ ruleLabel(rule) }}</span>
            <span class="field-linkage-card__arrow">→</span>
            <span class="field-linkage-card__field">{{ fieldLabel(rule.targetField) }}</span>
          </div>
          <p>{{ ruleSummary(rule) }}</p>
        </div>
        <div class="field-linkage-card__actions">
          <n-switch
            size="small"
            :value="rule.enabled !== false"
            @update:value="updateRule(index, { enabled: $event })"
          />
          <n-button text size="tiny" type="primary" :disabled="rule.type === 'remoteParam'" @click="openEdit(index)">
            编辑
          </n-button>
          <n-dropdown
            trigger="click"
            :options="ruleActionOptions(index)"
            @select="handleRuleAction"
          >
            <n-button text size="tiny">
              更多
            </n-button>
          </n-dropdown>
        </div>
      </article>
    </div>
    <n-empty v-else size="small" description="暂无字段联动" />

    <p class="field-linkage-tip">
      下拉选项的级联（选了上级后自动刷新下级选项）在字段属性的「级联选项」中配置。
    </p>

    <n-modal
      v-model:show="modalVisible"
      preset="card"
      :title="editingIndex < 0 ? '新增字段联动' : '编辑字段联动'"
      class="field-linkage-modal"
      style="width: min(680px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <div class="field-linkage-form">
        <section class="field-linkage-form__section">
          <h4>联动关系</h4>
          <div class="field-linkage-form__grid">
            <n-form-item label="控制字段（谁变化）" required>
              <n-select
                v-model:value="draft.sourceField"
                :options="fieldOptions"
                filterable
                clearable
                placeholder="选择源字段"
              />
            </n-form-item>
            <n-form-item label="联动方式" required>
              <n-select
                v-model:value="draft.type"
                :options="linkageTypeOptions"
                @update:value="handleTypeChange"
              />
            </n-form-item>
            <n-form-item label="目标字段（控制谁）" required>
              <n-select
                v-model:value="draft.targetField"
                :options="targetFieldOptions"
                filterable
                clearable
                placeholder="选择目标字段"
              />
            </n-form-item>
          </div>
        </section>

        <section v-if="draft.type === 'linkedDict'" class="field-linkage-form__section">
          <h4>字典级联参数</h4>
          <div class="field-linkage-form__grid">
            <n-form-item label="源字典类型" required>
              <n-input v-model:value="draft.dictConfig.sourceDictType" placeholder="例如 sys_province" />
            </n-form-item>
            <n-form-item label="目标字典类型" required>
              <n-input v-model:value="draft.dictConfig.targetDictType" placeholder="例如 sys_city" />
            </n-form-item>
            <n-form-item label="关联字典类型字段">
              <n-input v-model:value="draft.dictConfig.linkedDictType" placeholder="可选" />
            </n-form-item>
          </div>
        </section>

        <section v-else-if="draft.type === 'objectReference'" class="field-linkage-form__section">
          <h4>对象引用过滤参数</h4>
          <div class="field-linkage-form__grid">
            <n-form-item label="目标对象" required>
              <n-select
                v-model:value="draft.objectConfig.targetObjectCode"
                :options="objectOptions"
                filterable
                clearable
                placeholder="选择被过滤对象"
              />
            </n-form-item>
            <n-form-item label="显示字段">
              <n-input v-model:value="draft.objectConfig.displayField" placeholder="例如 customerName" />
            </n-form-item>
            <n-form-item label="请求参数名">
              <n-input v-model:value="draft.remoteConfig.paramName" :placeholder="draft.sourceField || '默认使用控制字段编码'" />
            </n-form-item>
          </div>
        </section>

        <section class="field-linkage-form__section">
          <h4>执行细节</h4>
          <div class="field-linkage-form__footer">
            <n-form-item label="源值为空时">
              <n-select v-model:value="draft.emptyStrategy" :options="emptyStrategyOptions" />
            </n-form-item>
            <label class="field-linkage-switch">
              <span>源字段变化后清空目标</span>
              <n-switch v-model:value="draft.clearOnSourceChange" size="small" />
            </label>
            <label class="field-linkage-switch">
              <span>启用这条联动</span>
              <n-switch v-model:value="draft.enabled" size="small" />
            </label>
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
            保存联动
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { normalizeLinkageSchema } from '../form-first/linkageSchema'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  fields: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])
const modalVisible = ref(false)
const editingIndex = ref(-1)
const draft = ref(createDraft())
const validationMessage = ref('')

const linkageTypeOptions = [
  { label: '字典级联', value: 'linkedDict' },
  { label: '对象引用过滤', value: 'objectReference' },
  { label: '仅清空目标', value: 'clear' },
]
const emptyStrategyOptions = [
  { label: '清空目标', value: 'empty' },
  { label: '保留全部选项', value: 'all' },
  { label: '禁用目标字段', value: 'disabled' },
]
const rules = computed(() => normalizeLinkageSchema({ rules: props.modelValue }).rules)
const fieldOptions = computed(() => props.fields.map((field) => {
  const value = String(field?.fieldCode || field?.field || '').trim()
  return value ? { label: `${field?.fieldName || field?.label || value}（${value}）`, value } : null
}).filter(Boolean))
const objectOptions = computed(() => props.relations.map((relation) => {
  const value = String(relation?.targetObjectCode || relation?.modelCode || '').trim()
  return value ? { label: `${relation?.targetObjectName || relation?.modelName || value}（${value}）`, value } : null
}).filter(Boolean))
// 目标字段候选排除当前控制字段，避免配置成自己联动自己
const targetFieldOptions = computed(() => fieldOptions.value.filter(option => option.value !== draft.value.sourceField))

function createDraft(source = {}) {
  return {
    ruleId: source.ruleId || `linkage_${Date.now()}`,
    type: source.type || 'clear',
    sourceField: source.sourceField || '',
    targetField: source.targetField || '',
    dataSourceType: source.dataSourceType || 'none',
    dictConfig: { sourceDictType: '', targetDictType: '', linkedDictType: '', ...(source.dictConfig || {}) },
    objectConfig: { ...(source.objectConfig || {}) },
    remoteConfig: { ...(source.remoteConfig || {}) },
    emptyStrategy: source.emptyStrategy || 'empty',
    clearOnSourceChange: source.clearOnSourceChange !== false,
    enabled: source.enabled !== false,
  }
}

function openCreate() {
  editingIndex.value = -1
  draft.value = createDraft()
  validationMessage.value = ''
  modalVisible.value = true
}

function openEdit(index) {
  editingIndex.value = index
  draft.value = createDraft(rules.value[index] || {})
  validationMessage.value = ''
  modalVisible.value = true
}

function handleTypeChange(type) {
  draft.value.dataSourceType = type === 'linkedDict'
    ? 'dict'
    : type === 'objectReference'
      ? 'object'
      : type === 'clear'
        ? 'none'
        : 'remote'
  if (type === 'clear')
    draft.value.emptyStrategy = 'empty'
}

function saveDraft() {
  validationMessage.value = validateDraft()
  if (validationMessage.value)
    return
  const list = clone(rules.value)
  if (editingIndex.value < 0)
    list.push(clone(draft.value))
  else
    list.splice(editingIndex.value, 1, clone(draft.value))
  emitRules(list)
  modalVisible.value = false
}

function validateDraft() {
  if (!draft.value.sourceField)
    return '请选择控制字段'
  if (!draft.value.targetField)
    return '请选择目标字段'
  if (draft.value.type === 'linkedDict' && (!draft.value.dictConfig.sourceDictType || !draft.value.dictConfig.targetDictType))
    return '字典级联需要填写源字典类型和目标字典类型'
  if (draft.value.type === 'objectReference' && !draft.value.objectConfig.targetObjectCode)
    return '对象引用过滤需要选择目标对象'
  return ''
}

function ruleActionOptions(index) {
  return [
    { label: rules.value[index]?.enabled === false ? '启用' : '停用', key: `toggle:${index}` },
    { label: '复制', key: `copy:${index}` },
    { label: '上移', key: `up:${index}`, disabled: index === 0 },
    { label: '下移', key: `down:${index}`, disabled: index === rules.value.length - 1 },
    { label: '删除', key: `delete:${index}` },
  ]
}

function handleRuleAction(key) {
  const [action, rawIndex] = String(key || '').split(':')
  const index = Number(rawIndex)
  const list = clone(rules.value)
  if (!Number.isInteger(index) || !list[index])
    return
  if (action === 'toggle')
    list[index].enabled = list[index].enabled === false
  else if (action === 'copy')
    list.splice(index + 1, 0, { ...list[index], ruleId: `linkage_${Date.now()}` })
  else if (action === 'delete')
    list.splice(index, 1)
  else if (action === 'up' && index > 0)
    [list[index - 1], list[index]] = [list[index], list[index - 1]]
  else if (action === 'down' && index < list.length - 1)
    [list[index + 1], list[index]] = [list[index], list[index + 1]]
  emitRules(list)
}

function updateRule(index, patch = {}) {
  emitRules(rules.value.map((rule, ruleIndex) => ruleIndex === index ? { ...rule, ...patch } : rule))
}

function emitRules(nextRules) {
  emit('update:modelValue', normalizeLinkageSchema({ rules: nextRules }).rules)
}

function fieldLabel(field) {
  if (!field)
    return '未选择字段'
  const matched = fieldOptions.value.find(option => option.value === field)
  return matched ? matched.label.split('（')[0] : field
}

function ruleLabel(rule) {
  if (rule?.type === 'remoteParam')
    return '远程选项过滤（已迁移）'
  return linkageTypeOptions.find(option => option.value === rule?.type)?.label || '字段联动'
}

function ruleSummary(rule = {}) {
  if (rule.type === 'remoteParam')
    return '该类型已下线，请删除本条并改用字段属性 → 级联选项。'
  if (rule.type === 'linkedDict') {
    const source = rule.dictConfig?.sourceDictType || '未填源字典'
    const target = rule.dictConfig?.targetDictType || '未填目标字典'
    return `字典 ${source} → ${target}；${emptyStrategyLabel(rule.emptyStrategy)}`
  }
  if (rule.type === 'objectReference') {
    const object = objectOptions.value.find(option => option.value === rule.objectConfig?.targetObjectCode)
    const objectName = object ? object.label : (rule.objectConfig?.targetObjectCode || '未选对象')
    return `按 ${rule.remoteConfig?.paramName || rule.sourceField || '源字段'} 过滤 ${objectName}`
  }
  return `源值变化时清空目标；${emptyStrategyLabel(rule.emptyStrategy)}`
}

function emptyStrategyLabel(strategy) {
  return emptyStrategyOptions.find(option => option.value === strategy)?.label || '清空目标'
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}
</script>

<style scoped>
.field-linkage-editor {
  display: grid;
  gap: 10px;
}

.field-linkage-editor__head,
.field-linkage-card,
.field-linkage-card__title,
.field-linkage-card__actions {
  display: flex;
  align-items: center;
}

.field-linkage-editor__head,
.field-linkage-card {
  justify-content: space-between;
  gap: 12px;
}

.field-linkage-editor__head strong {
  color: var(--n-text-color, #3f3f46);
  font-size: 13px;
  font-weight: 600;
}

.field-linkage-editor__head p {
  margin: 3px 0 0;
  color: var(--n-text-color-3, #71717a);
  font-size: 11px;
  line-height: 16px;
}

.field-linkage-list {
  display: grid;
  gap: 8px;
}

.field-linkage-card {
  padding: 10px;
  border: 1px solid var(--n-border-color, #e5e6eb);
  border-radius: 6px;
  background: var(--n-color, #fff);
}

.field-linkage-card--disabled {
  opacity: 0.62;
}

.field-linkage-card__main {
  overflow: hidden;
  min-width: 0;
}

.field-linkage-card__title {
  gap: 6px;
}

.field-linkage-card__field {
  overflow: hidden;
  max-width: 150px;
  color: #3f3f46;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-linkage-card__type {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 3px;
  background: #f4f4f5;
  color: #52525b;
  font-size: 11px;
}

.field-linkage-card__arrow {
  color: #a1a1aa;
  font-size: 12px;
}

.field-linkage-card__main > p {
  margin: 4px 0 0;
  overflow: hidden;
  color: #71717a;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-linkage-card__actions {
  flex: 0 0 auto;
  gap: 8px;
}

.field-linkage-tip {
  margin: 0;
  color: #a1a1aa;
  font-size: 11px;
  line-height: 16px;
}

.field-linkage-form {
  display: grid;
  gap: 12px;
  max-height: min(640px, calc(100vh - 190px));
  overflow-y: auto;
  padding-right: 4px;
}

.field-linkage-form__section {
  padding: 14px;
  border: 1px solid var(--n-border-color, #e5e6eb);
  border-radius: 8px;
  background: #fff;
}

.field-linkage-form h4 {
  margin: 0 0 12px;
  color: var(--n-text-color, #3f3f46);
  font-size: 13px;
  font-weight: 600;
}

.field-linkage-form__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 14px;
}

.field-linkage-form__footer {
  display: grid;
  grid-template-columns: minmax(180px, 240px) minmax(220px, 1fr) auto;
  align-items: end;
  gap: 16px;
}

.field-linkage-switch {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 7px;
  color: #4e5969;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .field-linkage-form__grid {
    grid-template-columns: 1fr;
  }

  .field-linkage-form__footer {
    grid-template-columns: 1fr;
  }
}
</style>
