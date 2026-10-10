<template>
  <div
    v-if="normalizedChildren.length"
    class="child-table-editor"
    :class="{ 'is-single-child': normalizedChildren.length === 1 }"
  >
    <n-tabs type="line">
      <n-tab-pane
        v-for="child in normalizedChildren"
        :key="resolveChildKey(child)"
        :name="resolveChildKey(child)"
        :tab="resolveChildPanelTitle(child)"
      >
        <div class="child-table-panel">
          <div class="child-table-head">
            <!-- 多子表时页签已展示名称；单子表时表单分区标题已展示，避免「页签+面板标题」叠两行 -->
            <div
              v-if="normalizedChildren.length === 1"
              class="child-table-title child-table-title--sr-only"
            >
              {{ resolveChildPanelTitle(child) }}
            </div>
            <div class="child-table-title-spacer" aria-hidden="true" />
            <n-space
              v-if="!props.readonly || visibleToolbarActions(child).length"
              class="child-table-head-actions"
              size="small"
            >
              <n-button v-if="hasRecordSelector(child) && canCreateRows(child) && child.allowSelectExisting !== false" size="small" secondary @click="openRecordSelector(child)">
                {{ resolveSelectorButtonText(child) }}
              </n-button>
              <n-button
                v-for="action in visibleToolbarActions(child)"
                :key="action.key || action.actionCode || action.label"
                size="small"
                :type="resolveActionButtonType(action)"
                :loading="isToolbarActionLoading(action, child)"
                secondary
                @click="executeToolbarAction(action, child)"
              >
                {{ action.label || action.actionName || action.actionCode }}
              </n-button>
              <n-button
                v-if="canCreateRows(child) && child.showInCreate !== false && child.inlineCreateEnabled !== false"
                size="small"
                type="primary"
                secondary
                @click="addRow(child)"
              >
                {{ resolveAddButtonText(child) }}
              </n-button>
            </n-space>
          </div>

          <div class="child-table-scroll" :class="{ 'card-scroll': isCardMode(child) }">
            <table class="child-edit-table" :class="{ 'card-mode': isCardMode(child) }" :style="resolveTableStyle(child)">
              <thead>
                <tr>
                  <th
                    v-for="field in child.fields"
                    :key="field.field"
                    :style="{ width: resolveColumnWidth(field) }"
                  >
                    <span>{{ field.label || field.field }}</span>
                    <em v-if="field.required">*</em>
                  </th>
                  <th v-if="hasActionColumn(child)" class="action-col">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="{ row, rowIndex } in visibleRowsFor(child)"
                  :key="row.__rowKey"
                >
                  <td v-for="field in child.fields" :key="field.field" :data-label="field.label || field.field">
                    <div v-if="useRuntimeCell(field, child)" class="child-runtime-cell">
                      <AiFormItem
                        :field="toRuntimeCellField(field, child, row)"
                        :value="resolveRowFieldValue(row, field)"
                        :form-data="row"
                        :context="buildRuntimeCellContext(child, rowIndex)"
                        @update:value="updateCell(child, rowIndex, field, $event)"
                      />
                    </div>
                    <n-input
                      v-else-if="resolveFieldControlType(field) === 'textarea'"
                      type="textarea"
                      v-bind="resolveInputProps(field)"
                      :value="resolveInputValue(row[field.field])"
                      :placeholder="field.props?.placeholder || `请输入${field.label || field.field}`"
                      :disabled="isCellReadonly(child, row, field)"
                      :autosize="{ minRows: 1, maxRows: 3 }"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-input-number
                      v-else-if="isNumberLikeField(field)"
                      v-bind="resolveInputProps(field)"
                      :value="coerceNumberFieldValue(row[field.field])"
                      :placeholder="field.props?.placeholder || `请输入${field.label || field.field}`"
                      :disabled="isCellReadonly(child, row, field)"
                      :precision="field.props?.precision ?? field.precision ?? (resolveFieldControlType(field) === 'number' && String(field.componentKey || '').toLowerCase() === 'money' ? 2 : undefined)"
                      style="width: 100%"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-select
                      v-else-if="resolveFieldControlType(field) === 'select'"
                      v-bind="resolveInputProps(field)"
                      :value="resolveSelectCellValue(row[field.field], field)"
                      :placeholder="field.props?.placeholder || `请选择${field.label || field.field}`"
                      :disabled="isCellReadonly(child, row, field)"
                      :options="field.props?.options || field.options || resolveCachedChildQueryOptions(field)"
                      clearable
                      filterable
                      :multiple="field.multiple === true || field.props?.multiple === true"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <UserSelectPicker
                      v-else-if="isUserSelectLikeField(field)"
                      v-bind="resolveInputProps(field)"
                      :model-value="resolveRowFieldValue(row, field)"
                      :label-value="resolveUserLabel(row, field)"
                      :placeholder="field.props?.placeholder || `请选择${field.label || field.field}`"
                      :disabled="isCellReadonly(child, row, field)"
                      :multiple="field.multiple === true || field.props?.multiple === true"
                      :clearable="field.clearable !== false && field.props?.clearable !== false"
                      @update:model-value="updateCell(child, rowIndex, field, $event)"
                      @update:label-value="updateCellLabel(child, rowIndex, field, $event)"
                    />
                    <n-date-picker
                      v-else-if="['date', 'datetime'].includes(resolveFieldControlType(field))"
                      v-bind="resolveInputProps(field)"
                      :value="row[field.field]"
                      :type="resolveFieldControlType(field) === 'datetime' ? 'datetime' : 'date'"
                      :placeholder="field.props?.placeholder || `请选择${field.label || field.field}`"
                      :disabled="isCellReadonly(child, row, field)"
                      style="width: 100%"
                      :format="field.props?.format || (resolveFieldControlType(field) === 'datetime' ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd')"
                      :value-format="field.props?.valueFormat || (resolveFieldControlType(field) === 'datetime' ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd')"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-switch
                      v-else-if="resolveFieldControlType(field) === 'switch'"
                      v-bind="resolveInputProps(field)"
                      :value="row[field.field]"
                      :disabled="isCellReadonly(child, row, field)"
                      :checked-value="resolveSwitchCheckedValue(field)"
                      :unchecked-value="resolveSwitchUncheckedValue(field)"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-input
                      v-else
                      v-bind="resolveInputProps(field)"
                      :value="resolveInputValue(row[field.field])"
                      :placeholder="field.props?.placeholder || `请输入${field.label || field.field}`"
                      :disabled="isCellReadonly(child, row, field)"
                      clearable
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                  </td>
                  <td v-if="hasActionColumn(child)" class="action-col">
                    <n-space size="small" :wrap="false">
                      <n-button
                        v-for="action in visibleRowActions(child, row)"
                        :key="action.key || action.actionCode || action.label"
                        text
                        size="small"
                        :type="resolveActionButtonType(action)"
                        :disabled="!childActionContext(child, row).persisted || isRowActionLoading(action, child, row)"
                        :loading="isRowActionLoading(action, child, row)"
                        :title="childActionTitle(action, child, row)"
                        @click="executeRowAction(action, child, row)"
                      >
                        {{ action.label || action.actionName || action.actionCode }}
                      </n-button>
                      <n-button v-if="canDeleteRows(child)" text type="error" size="small" @click="removeRow(child, rowIndex)">
                        删除
                      </n-button>
                    </n-space>
                  </td>
                </tr>
                <tr v-if="!visibleRowsFor(child).length">
                  <td :colspan="child.fields.length + (hasActionColumn(child) ? 1 : 0)" class="empty-cell">
                    <n-empty size="small" description="暂无明细" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>

    <AiRecordSelectorModal
      v-model:show="selectorVisible"
      :title="activeSelectorTitle"
      :suite-code="activeSelectorConfig.suiteCode"
      :object-code="activeSelectorConfig.objectCode"
      :business-object-code="activeSelectorConfig.businessObjectCode"
      :target-object-code="activeSelectorConfig.targetObjectCode"
      :target-entity-code="activeSelectorConfig.targetEntityCode"
      :candidate-object-code="activeSelectorConfig.candidateObjectCode"
      :reference-object-code="activeSelectorConfig.referenceObjectCode"
      :ref-object-code="activeSelectorConfig.refObjectCode"
      :source-object-code="activeSelectorConfig.sourceObjectCode"
      :target-code="activeSelectorConfig.targetCode"
      :multiple="resolveSelectorMultiple(activeSelectorChild)"
      :display-fields="activeSelectorConfig.displayFields"
      :keyword-fields="activeSelectorConfig.keywordFields"
      :field-mappings="activeSelectorConfig.fieldMappings"
      :search-params="activeSelectorConfig.searchParams"
      :filter-fields="activeSelectorConfig.filterFields"
      :query-source-type="activeSelectorConfig.querySourceType"
      :query-source-key="activeSelectorConfig.querySourceKey"
      :keyword-param="activeSelectorConfig.keywordParam"
      :runtime-context="activeSelectorRuntimeContext"
      @confirm="handleSelectorConfirm"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { executeLowcodeQuerySource } from '@/api/lowcode-query-source'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import AiRecordSelectorModal from '@/components/ai-form/AiRecordSelectorModal.vue'
import { buildChildRowActionContext } from '@/components/ai-form/business-action-runtime'
import { resolveControlProps } from '@/components/ai-form/control-props'
import { createFieldEventRuntime } from '@/components/ai-form/field-event-runtime'
import {
  coerceNumberFieldValue,
  isNumberLikeField,
} from '@/components/ai-form/field-type-utils'
import {
  isWeakControlType,
  resolveFieldControlType,
} from '@/components/ai-form/control-type-utils'
import {
  buildQuerySourceDisplayFields,
  collectFieldMappingSourceFields,
  readOptionRowField,
} from '@/components/ai-form/option-source-runtime'
import { applyRecordFieldMappings, extractSelectorRawRecord, normalizeRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import { isFieldMultiple, parseSelectionValues, serializeSelectionValues } from '@/components/ai-form/selection-multi-value'
import {
  isOrgSelectLikeField,
  isUserSelectLikeField,
  readDataFieldValue,
  resolveSelectionLabelFields,
} from '@/components/ai-form/selection-label-fields'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { hasRuntimeVisibilityRules, resolveRuntimeControl } from '@/components/lowcode-builder/shared/runtime-rules'
import { childTableKeysAlias } from '@/utils/flow-field-permissions'
import { scan as scanCollaborationCode } from '@/utils/collaboration-runtime'

// 模块一加载就打点：不依赖是否打开弹窗。控制台没有这行 = 浏览器还在跑旧包。
console.info(
  '%c[forge-child-select] module imported v4 ' + new Date().toISOString(),
  'color:#fff;background:#16a34a;padding:2px 6px;border-radius:4px',
)

const props = defineProps({
  value: {
    type: Object,
    default: () => ({}),
  },
  childrenConfig: {
    type: Array,
    default: () => [],
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  parentFormData: {
    type: Object,
    default: () => ({}),
  },
  context: {
    type: Object,
    default: () => ({}),
  },
  rowActionVisible: {
    type: Function,
    default: () => true,
  },
  rowActionLoading: {
    type: Function,
    default: () => false,
  },
})

const emit = defineEmits(['update:value', 'rowAction', 'toolbarAction'])

const route = useRoute()
const localValue = ref({})
const selectorVisible = ref(false)
const activeSelectorChild = ref(null)
const rowEventRuntimes = new Map()
const rowEventStates = reactive({})
/** 子表 QUERY_SOURCE / BUSINESS_OBJECT 选项预加载缓存，避免单元格内 AiFormItem 单独拉数失败时下拉无数据 */
const childQueryOptionCache = reactive({})
const childQueryOptionLoading = reactive({})
/** 预加载版本号：cache 写入后递增，强制表格单元格重渲染 */
const childQueryOptionVersion = ref(0)

console.info(
  '%c[forge-child-select] ChildTableEditor setup v4',
  'color:#fff;background:#2563eb;padding:2px 6px;border-radius:4px',
)

const normalizedChildren = computed(() => {
  // 依赖 version，预加载完成后重算 fields（把 options 写进字段）
  void childQueryOptionVersion.value
  return (props.childrenConfig || [])
    .map(child => ({
      ...child,
      fields: (child.fields || [])
        .map(field => mergeChildFieldRuntimeProps(field))
        .map(field => attachCachedChildQueryOptions(field))
        .filter(field => field && field.field && isChildEditorFieldVisible(field, child)),
    }))
    .filter(child => child.fields.length)
})

function resolveChildPanelTitle(child = {}) {
  return child.tabTitle || child.relationName || child.modelName || child.modelCode || child.tableName || '子表明细'
}

watch(
  () => {
    const children = props.childrenConfig || []
    return children.map(child => ({
      key: child?.modelCode || child?.relationKey || child?.key || child?.tableName,
      fields: (child?.fields || []).map(field => ({
        field: field?.field || field?.fieldCode || field?.sourceField,
        type: field?.type || field?.componentType,
        optionSource: field?.optionSource || field?.props?.optionSource || field?.basicProps?.optionSource,
        fieldMappings: field?.fieldMappings || field?.props?.fieldMappings || field?.basicProps?.fieldMappings,
        propsKeys: field?.props && typeof field.props === 'object' ? Object.keys(field.props) : [],
      })),
    }))
  },
  (snapshot) => {
    console.info('[forge-child-select] childrenConfig snapshot', JSON.parse(JSON.stringify(snapshot)))
    snapshot.forEach((child) => {
      ;(child.fields || []).forEach((field) => {
        preloadChildQuerySourceOptions(field)
      })
    })
  },
  { immediate: true, deep: true },
)

watch(
  () => [props.value, normalizedChildren.value.map(child => resolveChildKey(child)).join('|')],
  () => {
    const next = normalizeInputValue(props.value)
    if (isSameEditorValue(localValue.value, next))
      return
    localValue.value = next
  },
  { immediate: true, deep: true },
)

onBeforeUnmount(() => {
  rowEventRuntimes.forEach(runtime => runtime.dispose())
  rowEventRuntimes.clear()
})

function resolveChildKey(child) {
  return child.modelCode || child.relationKey || child.key || child.tableName || 'children'
}

/** 设计器“数据展示”配置：卡片/抽屉形态用卡片布局呈现 */
function isCardMode(child) {
  const mode = String(child?.displayMode || '').toLowerCase()
  return mode === 'card_list' || mode === 'bottom_sheet'
}

function resolveAddButtonText(child) {
  const title = child.tabTitle || child.relationName || child.modelName || '关联数据'
  return `新增${title}`
}

const activeSelectorConfig = computed(() => normalizeRecordSelectorConfig(activeSelectorChild.value))
const activeSelectorTitle = computed(() => activeSelectorConfig.value.title || `选择${activeSelectorChild.value?.modelName || activeSelectorChild.value?.tabTitle || '记录'}`)
const activeSelectorRuntimeContext = computed(() => ({
  ...(props.context || {}),
  formData: props.parentFormData || {},
  form: props.parentFormData || {},
  record: props.context?.record || props.parentFormData || {},
  row: props.context?.row || props.parentFormData || {},
  query: route.query || {},
  params: route.params || {},
  route: {
    query: route.query || {},
    params: route.params || {},
    path: route.path,
    fullPath: route.fullPath,
    name: route.name,
  },
}))

function hasRecordSelector(child) {
  const config = normalizeRecordSelectorConfig(child)
  return Boolean(config.objectCode || config.querySourceKey)
}

function resolveSelectorButtonText(child) {
  return normalizeRecordSelectorConfig(child).buttonText || '选择记录'
}

function configuredRowActions(child = {}) {
  return Array.isArray(child.rowActions) ? child.rowActions : []
}

function visibleRowActions(child, row) {
  return configuredRowActions(child).filter(action => action?.visible !== false
    && props.rowActionVisible(action, child, row))
}

function configuredToolbarActions(child = {}) {
  return Array.isArray(child.toolbarActions) ? child.toolbarActions : []
}

function visibleToolbarActions(child) {
  return configuredToolbarActions(child).filter(action => action?.visible !== false)
}

function executeToolbarAction(action, child) {
  emit('toolbarAction', { action, child })
}

function isToolbarActionLoading(action, child) {
  return props.rowActionLoading(action, child, null)
}

function hasActionColumn(child) {
  return canDeleteRows(child) || configuredRowActions(child).length > 0
}

function canCreateRows(child = {}) {
  return !props.readonly && child.allowCreate !== false
}

function canUpdateRows(child = {}) {
  return !props.readonly && child.allowUpdate !== false
}

function canDeleteRows(child = {}) {
  return !props.readonly && child.allowDelete !== false
}

function isCellReadonly(child, row, field = {}) {
  if (props.readonly)
    return true
  if (field.writable === true)
    return false
  if (field.writable === false || field.readonly === true || field.disabled === true)
    return true
  return hasPersistedRowId(row) ? !canUpdateRows(child) : !canCreateRows(child)
}

function childActionContext(child, row) {
  return buildChildRowActionContext({
    child,
    parentRecord: props.parentFormData,
    childRecord: row,
  })
}

function executeRowAction(action, child, row) {
  const executionContext = childActionContext(child, row)
  if (!executionContext.persisted)
    return
  emit('rowAction', { action, child, row, executionContext })
}

function isRowActionLoading(action, child, row) {
  return props.rowActionLoading(action, child, row)
}

function childActionTitle(action, child, row) {
  if (!childActionContext(child, row).persisted)
    return '请先保存主记录和子表行'
  if (isRowActionLoading(action, child, row))
    return action.loadingReason || '操作执行中，请稍候'
  return action.label || action.actionName || action.actionCode || ''
}

function resolveActionButtonType(action = {}) {
  const type = String(action.buttonType || action.type || '').toLowerCase()
  return ['primary', 'info', 'success', 'warning', 'error'].includes(type) ? type : 'primary'
}

function isChildEditorFieldVisible(field = {}, child = {}) {
  const hasVisibilityRules = hasRuntimeVisibilityRules(field)
  if (!hasVisibilityRules && (
    field.hidden === true || field.visible === false || field.formVisible === false
    || field.props?.hidden === true || field.props?.visible === false || field.props?.formVisible === false
    || field.basicProps?.hidden === true || field.basicProps?.visible === false || field.basicProps?.formVisible === false
  )) {
    return false
  }
  const explicitChildVisible = readOptionalBoolean(
    field.showInChildEditor,
    field.props?.showInChildEditor,
    field.basicProps?.showInChildEditor,
  )
  if (explicitChildVisible !== null)
    return explicitChildVisible
  if (isInternalIdField(field, child))
    return false
  return true
}

function readOptionalBoolean(...values) {
  for (const value of values) {
    if (value === true || value === 'true')
      return true
    if (value === false || value === 'false')
      return false
  }
  return null
}

/**
 * 只隐藏主键、系统字段和指向主表的外键。不能按「xxxId / 标题以 ID 结尾」判断：
 * 人员、部门、引用、「指标ID」这类业务列都会这样命名，用户在设计器选了却看不到。
 */
function isInternalIdField(field = {}, child = {}) {
  const keys = [field.field, field.fieldCode, field.sourceField, field.columnName]
    .map(value => normalizeFieldKey(value))
    .filter(Boolean)
  if (!keys.length)
    return false
  if (keys.includes('id') || field.systemField === true || field.primaryKey === true)
    return true
  const foreignKeys = [child.sourceField, child.foreignKey, child.foreignKeyField, child.relationField]
    .map(value => normalizeFieldKey(value))
    .filter(Boolean)
  return foreignKeys.some(key => keys.includes(key))
}

function normalizeFieldKey(value) {
  return String(value ?? '').trim().replace(/_/g, '').toLowerCase()
}

function rowsFor(child) {
  const key = resolveChildKey(child)
  return Array.isArray(localValue.value[key]) ? localValue.value[key] : []
}

function visibleRowsFor(child) {
  return rowsFor(child)
    .map((row, rowIndex) => ({ row, rowIndex }))
    .filter(item => !isDeletedRow(item.row))
}

function addRow(child) {
  const key = resolveChildKey(child)
  localValue.value = {
    ...localValue.value,
    [key]: [...rowsFor(child), createEmptyRow(child)],
  }
  commit()
}

function openRecordSelector(child) {
  activeSelectorChild.value = child
  selectorVisible.value = true
}

function handleSelectorConfirm({ rows = [], mappings = {} } = {}) {
  const child = activeSelectorChild.value
  if (!child || !rows.length)
    return
  const key = resolveChildKey(child)
  const hasMappings = mappings && Object.keys(mappings).length
  const nextRows = rows.map(row => ({
    ...createEmptyRow(child),
    ...normalizeMappedRow(child, hasMappings
      ? applyRecordFieldMappings(row, mappings)
      : autoMapSelectedRow(child, row)),
  }))
  localValue.value = {
    ...localValue.value,
    [key]: [...rowsFor(child), ...nextRows],
  }
  commit()
}

/**
 * 未配置字段映射时按子表字段名自动匹配选中记录：
 * 先同名取值，再尝试 snake_case 列名，兼容不同接口返回的键风格。
 */
function autoMapSelectedRow(child, row) {
  const source = extractSelectorRawRecord(row)
  const patch = {}
  ;(child.fields || []).forEach((field) => {
    const key = field.field || field.sourceField
    if (!key)
      return
    let value = source[key]
    if (value === undefined)
      value = source[key.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()]
    if (value === undefined)
      value = source[key.replace(/_([a-z])/g, (_, ch) => ch.toUpperCase())]
    if (value !== undefined && value !== null)
      patch[key] = value
  })
  return patch
}

/** 选择器单/多选：recordSelector.multiple，默认多选 */
function resolveSelectorMultiple(child) {
  const selector = child?.recordSelector
  if (selector && typeof selector === 'object' && 'multiple' in selector)
    return selector.multiple !== false
  return true
}

function removeRow(child, rowIndex) {
  const key = resolveChildKey(child)
  const rows = rowsFor(child)
  const row = rows[rowIndex]
  if (isMergeSaveMode(child) && hasPersistedRowId(row)) {
    localValue.value = {
      ...localValue.value,
      [key]: rows.map((item, index) => index === rowIndex ? { ...item, _deleted: true } : item),
    }
    commit()
    return
  }
  localValue.value = {
    ...localValue.value,
    [key]: rows.filter((_row, index) => index !== rowIndex),
  }
  commit()
}

function updateCell(child, rowIndex, field, value) {
  if (isCellReadonly(child, rowsFor(child)[rowIndex], field))
    return
  updateRow(child, rowIndex, { [field.field]: normalizeCellValueForType(field, value) })
}

function updateCellLabel(child, rowIndex, field, value) {
  if (isCellReadonly(child, rowsFor(child)[rowIndex], field))
    return
  const labelField = resolveUserLabelField(field)
  if (!labelField)
    return
  updateRow(child, rowIndex, {
    [labelField]: Array.isArray(value) ? value.join(',') : value || undefined,
  })
}

function updateRow(child, rowIndex, patch) {
  const key = resolveChildKey(child)
  const rows = rowsFor(child).map((row, index) => {
    if (index !== rowIndex)
      return row
    return applyRowPatch(row, patch)
  })
  localValue.value = {
    ...localValue.value,
    [key]: rows,
  }
  commit()
}

function useRuntimeCell(field = {}, child = {}) {
  const runtimeRules = Array.isArray(field.runtimeRules)
    ? field.runtimeRules
    : Array.isArray(field.props?.runtimeRules)
      ? field.props.runtimeRules
      : Array.isArray(field.basicProps?.runtimeRules) ? field.basicProps.runtimeRules : []
  const optionSource = field.optionSource || field.props?.optionSource || field.basicProps?.optionSource
  const fieldType = String(field.type || field.componentType || field.componentKey || '').trim()
  const controlType = resolveFieldControlType(field)
  const hasCurrentChildrenSource = optionSource && ['CURRENT_CHILDREN', 'current_children', 'currentChildren']
    .includes(String(optionSource.type || ''))
  const relationKey = resolveChildKey(child)
  const fieldEvents = Array.isArray(child.fieldEvents)
    ? child.fieldEvents
    : (props.context?.childFieldEvents?.[relationKey] || props.context?.fieldEvents || [])
  const hasFieldEvents = Array.isArray(fieldEvents) && fieldEvents.some(rule => rule?.enabled !== false
    && ['FORM_LOAD', 'CHANGE', 'BLUR', 'MANUAL', 'SCAN_COMPLETE'].includes(String(rule.trigger || '').toUpperCase())
    && (!rule.sourceField || rule.sourceField === field.field))
  if (fieldType === 'barcodeScanner' || controlType === 'barcodeScanner' || runtimeRules.length || hasCurrentChildrenSource || hasFieldEvents)
    return true
  // 人员/组织别名（forgeUserSelect / deptSelect 等）必须走 AiFormItem，否则会落到普通输入框
  if (isUserSelectLikeField(field) || isOrgSelectLikeField(field))
    return true
  // type 弱但 componentKey 强：开关/日期/上传/字典等不能落到默认 n-input
  if (isWeakControlType(field.type) && !isWeakControlType(controlType)
    && !['number', 'textarea', 'input'].includes(controlType))
    return true
  // 只要配了动态选项源，一律走 AiFormItem，避免退回空 options 的原生 n-select
  if (optionSource)
    return true
  // select 一律走 AiFormItem：原生 n-select 只认静态 options，子表常丢 optionSource 后会变成空下拉
  if (fieldType === 'select' || controlType === 'select'
    || fieldType === 'customSelect' || controlType === 'customSelect') {
    return true
  }
  return [
    'dictSelect',
    'regionTreeSelect',
    'objectReference',
    'recordSelector',
    'fileUpload',
    'imageUpload',
    'cascader',
    'treeSelect',
    'customSelect',
    'radio',
    'checkbox',
    'switch',
    'daterange',
    'datetimerange',
    'month',
    'year',
    'time',
    'timerange',
  ].includes(controlType || fieldType)
}

/**
 * 交给 AiFormItem 的 field 必须保留完整 props（optionSource / cascade / 引用 / 开关值）。
 * 禁止在这里套 resolveControlProps：那会剥掉运行态元数据，子表下拉/人员/部门/联动全部失效。
 */
function toRuntimeCellField(field = {}, child = {}, row = {}) {
  const readonly = isCellReadonly(child, row, field)
  const merged = mergeChildFieldRuntimeProps(field)
  const nextProps = { ...(merged.props || {}) }
  if (!readonly) {
    delete nextProps.readonly
    delete nextProps.disabled
  }
  else {
    nextProps.readonly = true
    nextProps.disabled = true
  }
  if (!nextProps.size)
    nextProps.size = field.size || 'small'
  const cachedOptions = resolveCachedChildQueryOptions(merged)
  if (cachedOptions.length) {
    nextProps.options = cachedOptions
    merged.options = cachedOptions
  }
  // 统一别名类型，避免 AiFormItem / 权限逻辑漏匹配 forgeUserSelect 等
  let type = resolveFieldControlType(merged) || merged.type || merged.componentType || merged.componentKey
  if (isUserSelectLikeField(merged))
    type = 'userSelect'
  else if (isOrgSelectLikeField(merged))
    type = 'orgTreeSelect'
  else if (isNumberLikeField(merged))
    type = 'number'
  return {
    ...merged,
    type,
    componentType: type,
    disabled: readonly,
    readonly,
    showLabel: false,
    showFeedback: false,
    size: field.size || 'small',
    props: nextProps,
  }
}

/** 把 basicProps 里的选项源/联动/开关值补进 props，兼容发布快照只落在 basicProps 的情况 */
function mergeChildFieldRuntimeProps(field = {}) {
  const basicProps = field.basicProps && typeof field.basicProps === 'object' ? field.basicProps : {}
  const props = { ...basicProps, ...(field.props && typeof field.props === 'object' ? field.props : {}) }
  const next = { ...field, props }
  if (!next.optionSource && props.optionSource)
    next.optionSource = props.optionSource
  if (!next.dictType && (props.dictType || basicProps.dictType))
    next.dictType = props.dictType || basicProps.dictType
  if (!next.referenceObjectCode && (props.referenceObjectCode || basicProps.referenceObjectCode))
    next.referenceObjectCode = props.referenceObjectCode || basicProps.referenceObjectCode
  if (next.type === 'switch' || next.componentType === 'switch') {
    if (props.checkedValue === undefined)
      props.checkedValue = basicProps.checkedValue !== undefined ? basicProps.checkedValue : 1
    if (props.uncheckedValue === undefined)
      props.uncheckedValue = basicProps.uncheckedValue !== undefined ? basicProps.uncheckedValue : 0
  }
  next.props = props
  return next
}

function attachCachedChildQueryOptions(field = {}) {
  const cached = resolveCachedChildQueryOptions(field)
  if (!cached.length)
    return field
  const props = { ...(field.props || {}), options: cached }
  return {
    ...field,
    options: cached,
    props,
  }
}

function resolveChildOptionSource(field = {}) {
  const source = field.optionSource || field.props?.optionSource || field.basicProps?.optionSource
  if (!source || typeof source !== 'object')
    return null
  const type = String(source.type || '').toUpperCase()
  const sourceType = String(source.sourceType || '').toUpperCase()
  const sourceKey = String(source.sourceKey || '').trim()
  if (!sourceKey)
    return null
  if (type === 'QUERY_SOURCE' || sourceType === 'BUSINESS_OBJECT' || sourceType === 'DATASET' || sourceType === 'EXTERNAL_API') {
    return {
      ...source,
      type: type || 'QUERY_SOURCE',
      sourceType: sourceType || source.sourceType,
      sourceKey,
    }
  }
  return null
}

function resolveChildOptionCacheKey(source = {}) {
  return [
    String(source.sourceType || 'BUSINESS_OBJECT'),
    String(source.sourceKey || ''),
    String(source.valueField || 'id'),
    String(source.labelField || 'name'),
    String(source.pageSize || 50),
  ].join('::')
}

function resolveCachedChildQueryOptions(field = {}) {
  const source = resolveChildOptionSource(field)
  if (!source)
    return []
  const cached = childQueryOptionCache[resolveChildOptionCacheKey(source)]
  return Array.isArray(cached) ? cached : []
}

async function preloadChildQuerySourceOptions(field = {}) {
  const source = resolveChildOptionSource(field)
  if (!source) {
    console.info('[forge-child-select] skip preload (no QUERY_SOURCE)', {
      field: field.field || field.fieldCode,
      type: field.type,
      optionSource: field.optionSource || field.props?.optionSource || field.basicProps?.optionSource || null,
    })
    return
  }
  const cacheKey = resolveChildOptionCacheKey(source)
  if (Array.isArray(childQueryOptionCache[cacheKey]) || childQueryOptionLoading[cacheKey]) {
    console.info('[forge-child-select] preload skip (cached/loading)', cacheKey, {
      cached: Array.isArray(childQueryOptionCache[cacheKey]) ? childQueryOptionCache[cacheKey].length : null,
      loading: !!childQueryOptionLoading[cacheKey],
    })
    return
  }
  childQueryOptionLoading[cacheKey] = true
  console.info('[forge-child-select] preload start', cacheKey, source)
  try {
    const mappings = field.fieldMappings || field.props?.fieldMappings || source.fieldMappings || source.mappings
    const fields = buildQuerySourceDisplayFields(source, collectFieldMappingSourceFields(mappings))
    const payload = {
      sourceType: source.sourceType || 'BUSINESS_OBJECT',
      sourceKey: source.sourceKey,
      params: {},
      fields: fields.length ? fields : undefined,
      pageNum: source.pageNum || 1,
      pageSize: source.pageSize || 50,
    }
    console.info('[forge-child-select] preload request', payload)
    const res = await executeLowcodeQuerySource(payload)
    console.info('[forge-child-select] preload response', {
      code: res?.code,
      message: res?.message,
      total: res?.data?.total,
      dataType: Array.isArray(res?.data?.data) ? `array(${res.data.data.length})` : typeof res?.data?.data,
      rawKeys: res?.data && typeof res.data === 'object' ? Object.keys(res.data) : [],
      sample: Array.isArray(res?.data?.data) ? res.data.data[0] : res?.data,
    })
    const result = res?.data || {}
    const rows = Array.isArray(result.data)
      ? result.data
      : Array.isArray(result.records)
        ? result.records
        : Array.isArray(result.list)
          ? result.list
          : []
    const valueField = source.valueField || 'id'
    const labelField = source.labelField || 'name'
    const options = rows.map((row) => {
      if (!row || typeof row !== 'object')
        return null
      const value = readOptionRowField(row, valueField) ?? row.value ?? row.id
      if (value === null || value === undefined || value === '')
        return null
      const label = readOptionRowField(row, labelField) ?? row.label ?? row.name ?? value
      return {
        ...row,
        value,
        key: row.key ?? value,
        label: String(label),
      }
    }).filter(Boolean)
    childQueryOptionCache[cacheKey] = options
    childQueryOptionVersion.value += 1
    console.info('[forge-child-select] preload ok', cacheKey, options.length, options.slice(0, 3))
  }
  catch (error) {
    console.warn('[forge-child-select] preload FAIL', cacheKey, {
      message: error?.message || error,
      code: error?.code,
      detail: error?.detail || error?.error,
      stack: error?.stack,
    })
    childQueryOptionCache[cacheKey] = childQueryOptionCache[cacheKey] || []
    childQueryOptionVersion.value += 1
  }
  finally {
    childQueryOptionLoading[cacheKey] = false
  }
}

function buildRuntimeCellContext(child, rowIndex) {
  const row = rowsFor(child)[rowIndex] || {}
  const relationKey = resolveChildKey(child)
  const rowKey = row.__rowKey || `${relationKey}:${rowIndex}`
  const runtime = getRowEventRuntime(child, rowIndex, row)
  return {
    ...(props.context || {}),
    // 子表单元格必须允许拉远程选项；避免父级误带 designer-preview 时被 AiFormItem 静默跳过
    allowOptionSourceFetch: true,
    schema: child.fields || [],
    allSchema: child.fields || [],
    parentFormData: props.parentFormData || {},
    form: props.parentFormData || {},
    record: props.context?.record || props.parentFormData || {},
    row,
    currentRow: row,
    query: route.query || {},
    params: route.params || {},
    route: {
      query: route.query || {},
      params: route.params || {},
      path: route.path,
      fullPath: route.fullPath,
      name: route.name,
    },
    patchFormData: patch => updateRow(child, rowIndex, patch),
    childCollections: Object.fromEntries(normalizedChildren.value.map(item => [
      resolveChildKey(item),
      rowsFor(item).filter(rowItem => !isDeletedRow(rowItem)),
    ])),
    relationKey,
    childRowIndex: rowIndex,
    scanField: typeof props.context?.scanField === 'function' ? props.context.scanField : scanChildField,
    hasFieldEvent: (trigger, field) => runtime?.hasRule(trigger, field) === true,
    getFieldEventState: field => rowEventStates[`${rowKey}:${field}`] || { status: 'idle', loading: false, message: '' },
    getFieldEventRules: (trigger, field) => runtime?.getRules(trigger, field) || [],
    dispatchFieldEvent: (trigger, field, eventRuntime = {}) => runtime?.dispatch(trigger, field, eventRuntime) || Promise.resolve([]),
  }
}

function scanChildField(field = {}) {
  const scanOptions = props.context?.scanOptions && typeof props.context.scanOptions === 'object'
    ? props.context.scanOptions
    : {}
  const fieldScanOptions = field.props && typeof field.props === 'object'
    ? {
        timeoutMs: field.props.timeoutMs,
        formats: field.props.formats,
      }
    : {}
  return scanCollaborationCode({
    ...scanOptions,
    ...fieldScanOptions,
    scanner: typeof props.context?.scanScanner === 'function' ? props.context.scanScanner : scanOptions.scanner,
    field,
  })
}

function getRowEventRuntime(child, rowIndex, row) {
  const relationKey = resolveChildKey(child)
  const rowKey = row.__rowKey || `${relationKey}:${rowIndex}`
  const runtimeKey = `${relationKey}:${rowKey}`
  if (rowEventRuntimes.has(runtimeKey))
    return rowEventRuntimes.get(runtimeKey)
  const rules = Array.isArray(child.fieldEvents)
    ? child.fieldEvents
    : (props.context?.childFieldEvents?.[relationKey] || props.context?.fieldEvents || [])
  if (!Array.isArray(rules) || !rules.length)
    return null
  const fields = (child.fields || []).map(field => field.field).filter(Boolean)
  const runtime = createFieldEventRuntime({
    rules,
    fields,
    execute: (payload, config) => executeLowcodeQuerySource(payload, config),
    getFormData: () => rowsFor(child)[rowIndex] || {},
    getContext: () => ({
      ...(props.context || {}),
      parentFormData: props.parentFormData || {},
      formData: rowsFor(child)[rowIndex] || {},
      record: props.parentFormData || {},
      row: rowsFor(child)[rowIndex] || {},
      relationKey,
      childRowIndex: rowIndex,
    }),
    getRouteQuery: () => route.query || {},
    applyPatch: patch => updateRow(child, rowIndex, patch),
    onStateChange: (state) => {
      rowEventStates[`${rowKey}:${state.field}`] = state
    },
    onNotify: props.context?.onNotify,
  })
  rowEventRuntimes.set(runtimeKey, runtime)
  return runtime
}

function applyRowPatch(row, patch) {
  const next = { ...row }
  Object.entries(patch || {}).forEach(([key, value]) => {
    if (value === undefined)
      delete next[key]
    else
      next[key] = value
  })
  return next
}

function resolveUserLabel(row, field) {
  const candidates = resolveSelectionLabelFields(field, 'user')
  for (const candidate of candidates) {
    const value = readDataFieldValue(row || {}, candidate)
    if (value !== null && value !== undefined && String(value).trim() !== '')
      return value
  }
  return ''
}

function resolveUserLabelField(field) {
  const candidates = resolveSelectionLabelFields(field, 'user')
  return field?.props?.labelValueField || field?.props?.targetField || field?.targetField || candidates[0] || `${field?.field || ''}Name`
}

function resolveRowFieldValue(row, field = {}) {
  const keys = [field.field, field.fieldCode, field.sourceField, field.columnName]
    .map(value => String(value || '').trim())
    .filter((value, index, all) => value && all.indexOf(value) === index)
  for (const key of keys) {
    const value = readDataFieldValue(row || {}, key)
    if (value !== undefined)
      return value
  }
  return undefined
}

function createEmptyRow(child) {
  const row = {
    __rowKey: `row_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  }
  ;(child.fields || []).forEach((field) => {
    row[field.field] = normalizeDefaultCellValue(field)
  })
  return row
}

function normalizeDefaultCellValue(field = {}) {
  if (field.defaultValue !== undefined && field.defaultValue !== null)
    return normalizeCellValueForType(field, field.defaultValue)
  if (field.props?.defaultValue !== undefined && field.props.defaultValue !== null)
    return normalizeCellValueForType(field, field.props.defaultValue)
  return null
}

function normalizeCellValueForType(field = {}, value) {
  if (value === undefined || value === null)
    return null
  const type = String(field.type || field.componentType || field.componentKey || '').toLowerCase()
  if (['input', 'textarea', 'text'].includes(type))
    return typeof value === 'string' ? value : String(value)
  if (isNumberLikeField(field) || ['number', 'input-number', 'inputnumber', 'integer', 'money', 'decimal'].includes(type)) {
    return coerceNumberFieldValue(value)
  }
  if (isFieldMultiple(field))
    return serializeSelectionValues(value, true) || null
  return value
}

function normalizeMappedRow(child, patch = {}) {
  const fieldMap = new Map((child.fields || []).map(field => [field.field, field]))
  return Object.entries(patch || {}).reduce((result, [key, value]) => {
    const field = fieldMap.get(key)
    result[key] = field ? normalizeCellValueForType(field, value) : value
    return result
  }, {})
}

function resolveInputValue(value) {
  if (value === undefined || value === null)
    return null
  return typeof value === 'string' ? value : String(value)
}

function resolveSelectCellValue(value, field = {}) {
  if (!isFieldMultiple(field))
    return value
  return parseSelectionValues(value, true)
}

function resolveInputProps(field = {}) {
  return resolveControlProps(field.props)
}

function normalizeInputValue(value) {
  const source = value && typeof value === 'object' ? value : {}
  const result = {}
  const usedSourceKeys = new Set()
  normalizedChildren.value.forEach((child) => {
    const key = resolveChildKey(child)
    const previousRows = Array.isArray(localValue.value?.[key]) ? localValue.value[key] : []
    const rows = resolveSourceChildRows(source, child, usedSourceKeys)
    result[key] = rows.map((row, index) => ({
      ...row,
      __rowKey: row.__rowKey || previousRows[index]?.__rowKey || `row_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    }))
  })
  return result
}

/** 兼容 modelCode / relationKey / tableName 别名，避免子表数据键与配置键不一致时整表空白 */
function resolveSourceChildRows(source = {}, child = {}, usedSourceKeys = new Set()) {
  const candidates = [child.modelCode, child.relationKey, child.key, child.tableName, resolveChildKey(child)]
    .map(value => String(value || '').trim())
    .filter(Boolean)
  for (const candidate of candidates) {
    if (usedSourceKeys.has(candidate))
      continue
    if (Array.isArray(source[candidate])) {
      usedSourceKeys.add(candidate)
      return source[candidate]
    }
  }
  let bestKey = ''
  let bestDelta = Number.POSITIVE_INFINITY
  Object.keys(source || {}).forEach((dataKey) => {
    if (usedSourceKeys.has(dataKey) || !Array.isArray(source[dataKey]))
      return
    const matched = candidates.some(candidate => childTableKeysAlias(candidate, dataKey))
    if (!matched)
      return
    const delta = Math.min(...candidates.map(candidate => Math.abs(candidate.length - dataKey.length)))
    if (delta < bestDelta) {
      bestDelta = delta
      bestKey = dataKey
    }
  })
  if (bestKey) {
    usedSourceKeys.add(bestKey)
    return source[bestKey]
  }
  return []
}

function isSameEditorValue(left, right) {
  try {
    return JSON.stringify(left || {}) === JSON.stringify(right || {})
  }
  catch {
    return false
  }
}

function commit() {
  emit('update:value', getEditorValue())
}

function getValue() {
  const result = {}
  normalizedChildren.value.forEach((child) => {
    const key = resolveChildKey(child)
    result[key] = rowsFor(child)
      .filter(row => isDeletedRow(row) || !isEmptyRow(row, child.fields))
      .map(row => stripInternalFields(row))
  })
  return result
}

function getEditorValue() {
  const result = {}
  normalizedChildren.value.forEach((child) => {
    const key = resolveChildKey(child)
    result[key] = rowsFor(child).map(row => ({ ...row }))
  })
  return result
}

function stripInternalFields(row) {
  const result = {}
  Object.entries(row || {}).forEach(([key, value]) => {
    if (key !== '__rowKey')
      result[key] = value
  })
  return result
}

function isEmptyRow(row, fields) {
  if (isDeletedRow(row))
    return false
  return !(fields || []).some(field => !isEmptyValue(row?.[field.field]))
}

function isMergeSaveMode(child) {
  return String(child?.saveMode || '').toLowerCase() === 'merge'
}

function hasPersistedRowId(row) {
  const id = row?.id ?? row?.ID
  return id !== null && id !== undefined && String(id).trim() !== ''
}

function isDeletedRow(row) {
  const value = row?._deleted ?? row?.__deleted
  if (typeof value === 'boolean')
    return value
  return ['true', '1', 'yes', 'y'].includes(String(value || '').trim().toLowerCase())
}

function isEmptyValue(value) {
  if (value === null || value === undefined)
    return true
  if (typeof value === 'string')
    return value.trim() === ''
  if (Array.isArray(value))
    return value.length === 0
  return false
}

function validate() {
  for (const child of normalizedChildren.value) {
    const rows = visibleRowsFor(child)
    for (let index = 0; index < rows.length; index++) {
      const { row } = rows[index]
      if (isEmptyRow(row, child.fields))
        continue
      for (const field of child.fields) {
        const control = resolveRuntimeControl(field, {
          ...(props.context || {}),
          parentFormData: props.parentFormData || {},
          record: props.parentFormData || {},
          row,
          formData: row,
          data: row,
        })
        if (control.visible === false)
          continue
        const required = control.required === true || (control.required === undefined && field.required === true)
        if (required && isEmptyValue(row[field.field])) {
          throw new Error(`${child.modelName || '子表'}第${index + 1}行请填写${field.label || field.field}`)
        }
      }
    }
  }
}

function resolveTableStyle(child) {
  const contentWidth = (child.fields || [])
    .reduce((total, field) => total + Number.parseInt(resolveColumnWidth(field), 10), hasActionColumn(child) ? 150 : 0)
  const minWidth = Math.max(contentWidth, Number(child.minWidth || child.tableMinWidth || 720))
  return {
    minWidth: `${minWidth}px`,
  }
}

function resolveColumnWidth(field) {
  const configuredWidth = Number(field.width || field.props?.width || 0)
  const minWidth = Math.max(Number(field.minWidth || field.props?.minWidth || 0), resolveDefaultColumnMinWidth(field))
  return `${Math.max(configuredWidth, minWidth)}px`
}

function resolveDefaultColumnMinWidth(field = {}) {
  const type = String(field.type || field.componentType || '').toLowerCase()
  const label = String(field.label || '')
  const fieldName = String(field.field || '')
  if (type === 'textarea')
    return 260
  if (['select', 'dictselect', 'objectreference', 'recordselector', 'userselect', 'orgtreeselect', 'departmenttreeselect', 'treeselect', 'cascader', 'customselect'].includes(type)
    || isUserSelectLikeField(field)
    || isOrgSelectLikeField(field))
    return isUserSelectLikeField(field) || isOrgSelectLikeField(field) ? 280 : 200
  if (['date', 'datetime', 'daterange', 'datetimerange'].includes(type))
    return type.includes('time') ? 190 : 150
  if (['number', 'input-number', 'inputnumber', 'money'].includes(type))
    return /金额|单价|价格|报价|库存|数量|分/.test(label) || /amount|price|quantity|stock/i.test(fieldName) ? 150 : 130
  if (/单位/.test(label) || fieldName === 'unit')
    return 90
  return 140
}

function resolveSwitchCheckedValue(field = {}) {
  if (field.props?.checkedValue !== undefined)
    return field.props.checkedValue
  if (field.checkedValue !== undefined)
    return field.checkedValue
  if (field.basicProps?.checkedValue !== undefined)
    return field.basicProps.checkedValue
  return 1
}

function resolveSwitchUncheckedValue(field = {}) {
  if (field.props?.uncheckedValue !== undefined)
    return field.props.uncheckedValue
  if (field.uncheckedValue !== undefined)
    return field.uncheckedValue
  if (field.basicProps?.uncheckedValue !== undefined)
    return field.basicProps.uncheckedValue
  return 0
}

defineExpose({
  validate,
  getValue,
})
</script>

<style scoped>
.child-table-editor {
  margin-top: 18px;
  border-top: 1px solid #e5e7eb;
  padding-top: 14px;
}

/* 仅一个子表时隐藏页签栏，避免与表单分区标题再叠一层同名页签 */
.child-table-editor.is-single-child :deep(.n-tabs-nav) {
  display: none;
}

.child-table-editor.is-single-child :deep(.n-tab-pane) {
  padding-top: 0;
}

.child-table-panel {
  display: grid;
  gap: 10px;
}

.child-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.child-table-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.child-table-title--sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.child-table-title-spacer {
  flex: 1;
  min-width: 0;
}

.child-table-head-actions {
  flex: none;
  margin-left: auto;
}

.child-table-scroll {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.child-table-scroll.card-scroll {
  border: 0;
  border-radius: 0;
  overflow-x: visible;
}

.child-edit-table.card-mode,
.child-edit-table.card-mode tbody,
.child-edit-table.card-mode tr,
.child-edit-table.card-mode td {
  display: block;
  width: 100%;
}

.child-edit-table.card-mode {
  min-width: 0 !important;
}

.child-edit-table.card-mode thead {
  display: none;
}

.child-edit-table.card-mode tr {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 10px;
  padding: 4px 14px 12px;
  background: #fff;
}

.child-edit-table.card-mode td {
  border-bottom: 0;
  padding: 7px 0 0;
}

.child-edit-table.card-mode td::before {
  content: attr(data-label);
  display: block;
  margin-bottom: 2px;
  color: #86909c;
  font-size: 11px;
  line-height: 1.4;
}

.child-edit-table.card-mode td.action-col {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px dashed #eef2f7;
  text-align: right;
  width: auto;
}

.child-edit-table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  background: #fff;
}

.child-edit-table th {
  height: 38px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 0 10px;
  text-align: left;
  white-space: nowrap;
}

.child-edit-table th em {
  margin-left: 3px;
  color: #dc2626;
  font-style: normal;
}

.child-edit-table td {
  border-bottom: 1px solid #eef2f7;
  padding: 8px 10px;
  vertical-align: top;
}

.child-runtime-cell {
  position: relative;
  z-index: 1;
  width: 100%;
  min-width: 0;
  pointer-events: auto;
}

.child-runtime-cell :deep(.n-form-item) {
  margin: 0;
}

.child-runtime-cell :deep(.n-base-selection),
.child-runtime-cell :deep(.n-input),
.child-runtime-cell :deep(.n-input-number),
.child-runtime-cell :deep(.n-date-picker),
.child-runtime-cell :deep(.n-tree-select) {
  width: 100%;
  min-width: 160px;
}

/* 人员选择：输入框不要强行 min-width，否则会把右侧清空/选择按钮挤出单元格被盖住 */
.child-runtime-cell :deep(.user-select-picker .n-input) {
  min-width: 0;
}

.child-runtime-cell :deep(.user-select-picker__group) {
  width: 100%;
  min-width: 0;
}

.child-runtime-cell :deep(.user-select-picker__button) {
  flex-shrink: 0;
}

.child-runtime-cell :deep(.n-form-item-feedback-wrapper) {
  display: none;
  min-height: 0;
}

.child-edit-table tr:last-child td {
  border-bottom: 0;
}

.action-col {
  width: 76px;
  text-align: center;
  white-space: nowrap;
}

.empty-cell {
  padding: 26px 0 !important;
}
</style>
