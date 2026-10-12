<template>
  <section class="bottom-bar-editor">
    <div class="workbench-heading bottom-heading">
      <div>
        <strong>底部操作栏</strong>
        <span>{{ bottomActions.length }} 个按钮</span>
      </div>
      <n-dropdown :options="bottomActionAddOptions" trigger="click" @select="addBottomAction">
        <n-button size="small" type="primary" secondary>
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          新增按钮
        </n-button>
      </n-dropdown>
    </div>

    <draggable
      :model-value="bottomActions"
      item-key="__editorKey"
      handle=".action-drag-handle"
      ghost-class="action-sort-ghost"
      chosen-class="action-sort-chosen"
      @update:model-value="emitActions"
    >
      <template #item="{ element, index }">
        <article class="bottom-action-row">
          <span class="action-drag-handle" title="拖拽排序">
            <n-icon><ReorderFourOutline /></n-icon>
          </span>
          <div class="action-main-fields">
            <n-form-item label="按钮类型">
              <n-select
                :value="element.type"
                :options="bottomActionTypeOptions"
                @update:value="updateBottomActionType(index, $event)"
              />
            </n-form-item>
            <n-form-item label="按钮名称">
              <n-input
                :value="element.label"
                placeholder="按钮名称"
                @update:value="patchBottomAction(index, { label: $event })"
              />
            </n-form-item>
            <n-form-item label="按钮行为">
              <n-button
                v-if="isConfigurableBottomAction(element)"
                secondary
                block
                @click="configureBottomAction(index, element)"
              >
                {{ bottomActionBehaviorSummary(element) }}
              </n-button>
              <span v-else class="builtin-action-summary">{{ bottomActionBehaviorSummary(element) }}</span>
            </n-form-item>
            <n-form-item label="按钮样式">
              <n-select
                :value="element.variant || 'secondary'"
                :options="variantOptions"
                @update:value="patchBottomAction(index, { variant: $event })"
              />
            </n-form-item>
          </div>

          <div class="action-condition-fields">
            <span class="inline-field-label">显示条件</span>
            <n-select
              class="condition-field"
              :value="conditionValue(element).field"
              :options="conditionFieldOptions(element)"
              filterable
              clearable
              placeholder="字段"
              @update:value="updateBottomActionCondition(index, { field: $event || '' })"
            />
            <n-select
              class="condition-operator"
              :value="conditionValue(element).operator"
              :options="conditionOperatorOptions"
              @update:value="updateBottomActionCondition(index, { operator: $event })"
            />
            <n-input
              class="condition-value"
              :value="conditionValue(element).value"
              clearable
              placeholder="值"
              @update:value="updateBottomActionCondition(index, { value: $event })"
            />
          </div>

          <n-alert
            v-if="!isSupportedDisplayCondition(element.displayCondition)"
            class="action-condition-warning"
            type="warning"
            :bordered="false"
          >
            现有显示条件“{{ element.displayCondition }}”不受支持，请重新设置。
          </n-alert>

          <div class="action-extra-fields">
            <n-form-item label="页面模式">
              <n-checkbox-group
                :value="visibleModes(element)"
                @update:value="patchBottomAction(index, { visibleInModes: updateVisibleModes(element, $event) })"
              >
                <n-space size="small">
                  <n-checkbox
                    v-for="mode in modeOptions"
                    :key="mode.value"
                    :value="mode.value"
                    :disabled="isOnlyVisibleMode(element, mode.value)"
                  >
                    {{ mode.label }}
                  </n-checkbox>
                </n-space>
              </n-checkbox-group>
            </n-form-item>
            <n-form-item label="确认提示">
              <n-input
                :value="element.confirmText || ''"
                clearable
                placeholder="可选"
                @update:value="patchBottomAction(index, { confirmText: $event || '' })"
              />
            </n-form-item>
            <n-form-item label="成功提示">
              <n-input
                :value="element.successMessage || ''"
                clearable
                placeholder="可选"
                @update:value="patchBottomAction(index, { successMessage: $event || '' })"
              />
            </n-form-item>
            <n-form-item label="按钮权限标识">
              <n-input
                :value="element.permissionKey || element.permissionCode || ''"
                clearable
                placeholder="例如：order:submit"
                @update:value="patchBottomAction(index, { permissionKey: $event || '', permissionCode: $event || '' })"
              />
            </n-form-item>
            <n-form-item label="无权限时">
              <n-select
                :value="element.permissionStrategy || 'hide'"
                :options="permissionStrategyOptions"
                @update:value="patchBottomAction(index, { permissionStrategy: $event })"
              />
            </n-form-item>
          </div>

          <n-button class="action-remove" circle quaternary type="error" title="删除按钮" @click="removeBottomAction(index)">
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
          </n-button>
        </article>
      </template>
    </draggable>
    <n-empty v-if="!bottomActions.length" size="small" description="未配置底部按钮" />
  </section>
</template>

<script setup>
import { AddOutline, ReorderFourOutline, TrashOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import draggable from 'vuedraggable'
import {
  appendMissingOptions,
  createBottomAction,
  isSupportedDisplayCondition,
  parseDisplayCondition,
  resolveVisibleModes,
  serializeDisplayCondition,
  updateVisibleModes,
} from './pageSectionEditorUtils'

const props = defineProps({
  // 受控组件：modelValue 为 schema.bottomBar（{ actions: [] }），变更即 emit 完整对象。
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
})
const emit = defineEmits(['update:modelValue', 'configureBottomAction'])

const modeOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'edit' },
  { label: '详情', value: 'detail' },
]
const bottomActionTypeOptions = [
  { label: '提交保存', value: 'save' },
  { label: '跳转页面', value: 'navigate' },
  { label: '启动业务流程', value: 'process' },
  { label: '执行自定义动作', value: 'action' },
  { label: '重置', value: 'reset' },
  { label: '取消', value: 'cancel' },
]
const bottomActionAddOptions = bottomActionTypeOptions.map(item => ({ label: item.label, key: item.value }))
const variantOptions = [
  { label: '主要按钮', value: 'primary' },
  { label: '次要按钮', value: 'secondary' },
]
const permissionStrategyOptions = [
  { label: '隐藏按钮', value: 'hide' },
  { label: '禁用按钮', value: 'disable' },
]
const conditionOperatorOptions = [
  { label: '等于', value: '==' },
  { label: '不等于', value: '!=' },
]

const bottomActions = computed(() => (Array.isArray(props.modelValue?.actions) ? props.modelValue.actions : [])
  .map((action, index) => ({
    ...action,
    __editorKey: action.actionId || `existing_action_${index}_${action.type || 'action'}`,
  })))
const baseFieldOptions = computed(() => props.fields
  .map((field) => {
    const value = String(field?.fieldCode || field?.field || field?.sourceField || '').trim()
    return value
      ? { label: `${field?.fieldName || field?.label || field?.comment || value}（${value}）`, value }
      : null
  })
  .filter(Boolean))

function emitActions(actions = []) {
  emit('update:modelValue', {
    ...(props.modelValue || {}),
    actions: actions.map(({ __editorKey, ...action }) => action),
  })
}

function addBottomAction(type) {
  const publicActions = bottomActions.value.map(({ __editorKey, ...action }) => action)
  const action = createBottomAction(type, publicActions)
  emitActions([...bottomActions.value, action])
}

function patchBottomAction(index, patch = {}) {
  emitActions(bottomActions.value.map((action, actionIndex) => actionIndex === index ? { ...action, ...patch } : action))
}

function updateBottomActionType(index, type) {
  const current = bottomActions.value[index]
  const preset = createBottomAction(type, [])
  patchBottomAction(index, {
    type,
    label: current.label || preset.label,
    variant: current.variant || preset.variant,
    ...(['navigate', 'process', 'action'].includes(type)
      ? { actionCode: current.actionCode || '' }
      : { actionCode: undefined }),
    actionType: undefined,
    targetPageKey: undefined,
    processCode: undefined,
    processId: undefined,
  })
}

function configureBottomAction(index, action) {
  emit('configureBottomAction', { index, action: { ...action } })
}

function bottomActionBehaviorSummary(action = {}) {
  if (action.type === 'navigate')
    return action.actionCode ? `跳转：${action.actionCode}` : '配置目标页面'
  if (action.type === 'process')
    return action.actionCode ? `启动：${action.actionCode}` : '配置业务流程'
  if (action.type === 'action')
    return action.actionCode ? `执行：${action.actionCode}` : '配置自定义动作'
  if (action.type === 'reset')
    return '重置当前表单'
  if (action.type === 'cancel')
    return '取消并返回'
  return '提交并保存'
}

function isConfigurableBottomAction(action = {}) {
  return ['save', 'navigate', 'process', 'action'].includes(action.type)
}

function removeBottomAction(index) {
  emitActions(bottomActions.value.filter((_, actionIndex) => actionIndex !== index))
}

function conditionValue(action) {
  return parseDisplayCondition(action?.displayCondition)
}

function updateBottomActionCondition(index, patch = {}) {
  const condition = { ...conditionValue(bottomActions.value[index]), ...patch }
  patchBottomAction(index, { displayCondition: serializeDisplayCondition(condition) })
}

function conditionFieldOptions(action) {
  return appendMissingOptions(baseFieldOptions.value, [conditionValue(action).field], '字段已失效')
}

function visibleModes(item = {}) {
  return resolveVisibleModes(item)
}

function isOnlyVisibleMode(item = {}, mode = '') {
  const modes = visibleModes(item)
  return modes.length === 1 && modes[0] === mode
}
</script>

<style scoped>
.bottom-bar-editor {
  border: 1px solid var(--border-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--bg-primary);
}

.workbench-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workbench-heading > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.workbench-heading strong {
  font-size: 14px;
  font-weight: 600;
}

.workbench-heading span {
  color: var(--text-tertiary);
  font-size: 12px;
}

.bottom-heading {
  background: var(--bg-secondary);
}

.bottom-action-row {
  position: relative;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 36px;
  gap: 10px;
  padding: 12px 10px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
}

.action-main-fields,
.action-condition-fields,
.action-extra-fields {
  grid-column: 2;
}

.action-main-fields {
  display: grid;
  grid-template-columns: 140px minmax(140px, 1fr) minmax(180px, 1.3fr) 130px;
  gap: 0 16px;
}

.builtin-action-summary {
  display: flex;
  min-height: 34px;
  align-items: center;
  color: var(--text-secondary);
  font-size: 13px;
}

.action-condition-fields {
  display: grid;
  grid-template-columns: 72px minmax(160px, 1fr) 110px minmax(140px, 1fr);
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.inline-field-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.action-extra-fields {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) minmax(180px, 1fr) minmax(180px, 1fr);
  gap: 0 16px;
}

.action-condition-warning {
  grid-column: 2;
  margin: -8px 0 18px;
}

.action-drag-handle {
  display: inline-grid;
  color: var(--text-tertiary);
  cursor: grab;
  place-items: center;
  align-self: start;
  padding-top: 8px;
}

.action-remove {
  grid-column: 3;
  grid-row: 1;
  align-self: start;
}

.action-sort-ghost {
  border: 1px dashed #3370ff;
  background: #eaf2ff;
  opacity: 0.55;
}

.action-sort-chosen {
  box-shadow: 0 6px 16px rgba(31, 35, 41, 0.12);
}

:deep(.n-form-item) {
  min-width: 0;
}

@media (max-width: 900px) {
  .action-main-fields,
  .action-extra-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
