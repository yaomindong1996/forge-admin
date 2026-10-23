/** Auto-split part 3 of ForgePropertyPanel setup. */
import { computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { serializeSelectionValues } from '@/components/ai-form/selection-multi-value'
import { getDictData } from '@/composables/useDict'
import { colorToHexInput, hexInputToColor } from '@/utils/designer-color'
import {
  appendDesignerLayoutChild,
  cloneValue,
  findDesignerComponentPath,
  getDesignerComponent,
  isFieldComponent,
  normalizeFormDesignerSchema,
} from '../../form-first/formDesignerSchema'
import { normalizeGridCount } from '../formLayoutConfig'
import { buildDefaultPlaceholder, shouldSyncPlaceholder } from '../placeholder-utils'

export function useForgePropertyPanelPart3(props, emit, deps = {}) {
  const {
    __impl,
    mut,
    selectedDictType,
    designerStore,
    message,
    BitableStyleIcon,
    BitableSelectIcon,
    BitableNumberIcon,
    BitableCalendarIcon,
    BitableAttachmentIcon,
    BitableMemberIcon,
    BitableLookupIcon,
    crudFieldDrawerVisible,
    editingCrudFieldId,
    selectedCodeDraft,
    selectedCodeDraftTarget,
    schemaCodeDraft,
    schemaCodeDirty,
    sourceError,
    sourceModalVisible,
    dictDefaultOptions,
    dictDefaultOptionsLoading,
    selectedComponent,
    isField,
    isLayout,
    isCrudBlock,
    crudApiConfig,
    crudOptions,
    selectedDesignerStyle,
    formStyle,
    layoutChildren,
    interactionRules,
    defaultTrigger,
    selectedComponentCodeRaw,
    selectedCodeText,
    schemaCodeRaw,
    schemaCodeText,
    selectedOpacityPercent,
    formOpacityPercent,
    selectedDesignerTranslate,
    formTranslate,
    maxFormGridColumns,
    triggerOptions,
    actionOptions,
    expandPanelTypeOptions,
    crudConfigFields,
    crudDescriptionFieldOptions,
    firstCrudExpandPanel,
    selectedOptions,
    defaultValueSelectMultiple,
    updateComponent,
    loadCodeRuleOptions,
    updateFormGovernance,
  } = deps
function updateFormFieldLinkages(fieldLinkages = []) {
  updateFormGovernance({ fieldLinkages: Array.isArray(fieldLinkages) ? fieldLinkages : [] })
}
__impl.updateFormFieldLinkages = updateFormFieldLinkages

// 多表单管理已拆分至 panels/FormAssetsPanel.vue（读写 Pinia store）
function collectBoundFieldOptions(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    const field = component?.fieldBinding?.fieldCode || component?.field || component?.props?.field
    if (field && !result.some(item => item.value === field)) {
      result.push({
        label: `${component.label || field}（${field}）`,
        value: field,
      })
    }
    collectBoundFieldOptions(component?.children || component?.props?.children || [], result)
  })
  return result
}
__impl.collectBoundFieldOptions = collectBoundFieldOptions

function updateLabel(value) {
  const component = selectedComponent.value
  const patch = { label: value }
  let propsPatch = null
  if (isCrudBlock.value) {
    propsPatch = {
      title: value,
    }
  }
  else if (isLayout.value) {
    propsPatch = {
      header: value,
      title: value,
      label: value,
    }
  }
  else if (isField.value && shouldSyncPlaceholder(component, value)) {
    propsPatch = {
      placeholder: buildDefaultPlaceholder(component?.componentKey, value),
    }
  }
  if (propsPatch)
    patch.props = propsPatch
  updateComponent(patch)
}
__impl.updateLabel = updateLabel

async function loadDefaultValueDictOptions(dictType = '') {
  const normalizedType = String(dictType || '').trim()
  if (!normalizedType || dictDefaultOptions.value[normalizedType] || dictDefaultOptionsLoading.value[normalizedType])
    return
  dictDefaultOptionsLoading.value = {
    ...dictDefaultOptionsLoading.value,
    [normalizedType]: true,
  }
  try {
    const data = await getDictData(normalizedType)
    dictDefaultOptions.value = {
      ...dictDefaultOptions.value,
      [normalizedType]: normalizeDefaultValueOptions(data),
    }
  }
  finally {
    const nextLoading = { ...dictDefaultOptionsLoading.value }
    delete nextLoading[normalizedType]
    dictDefaultOptionsLoading.value = nextLoading
  }
}
__impl.loadDefaultValueDictOptions = loadDefaultValueDictOptions

watch(
  selectedDictType,
  (dictType) => {
    loadDefaultValueDictOptions(dictType)
  },
  { immediate: true },
)


function normalizeDefaultValueOptions(options = []) {
  return (Array.isArray(options) ? options : [])
    .map((option) => {
      if (option == null)
        return null
      if (typeof option !== 'object') {
        return {
          label: String(option),
          value: option,
        }
      }
      const value = option.value ?? option.dictValue ?? option.key
      if (value === undefined || value === null)
        return null
      const label = option.label ?? option.dictLabel ?? option.title ?? String(value)
      return {
        label: formatDefaultValueOptionLabel(label, value),
        value,
        disabled: option.disabled === true,
      }
    })
    .filter(Boolean)
}
__impl.normalizeDefaultValueOptions = normalizeDefaultValueOptions

function formatDefaultValueOptionLabel(label, value) {
  const labelText = String(label ?? '')
  const valueText = String(value ?? '')
  return labelText && valueText && labelText !== valueText ? `${labelText}（${valueText}）` : labelText || valueText
}
__impl.formatDefaultValueOptionLabel = formatDefaultValueOptionLabel

function updateDefaultValue(value) {
  if (defaultValueSelectMultiple.value) {
    const serialized = serializeSelectionValues(value, true)
    updateComponent({ props: { defaultValue: serialized || undefined } })
    return
  }
  updateComponent({ props: { defaultValue: value === null || value === undefined || value === '' ? undefined : value } })
}
__impl.updateDefaultValue = updateDefaultValue

function addLayoutChild(componentKey = '') {
  if (!selectedComponent.value)
    return
  designerStore.applySchema(
    appendDesignerLayoutChild(designerStore.schema, selectedComponent.value.id, componentKey),
  )
}
__impl.addLayoutChild = addLayoutChild

function updateLayoutChild(index, patch = {}) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  if (!nextChildren[index])
    return
  nextChildren[index] = {
    ...nextChildren[index],
    ...patch,
    props: patch.props
      ? {
          ...(nextChildren[index].props || {}),
          ...patch.props,
        }
      : nextChildren[index].props,
    layout: patch.layout
      ? {
          ...(nextChildren[index].layout || {}),
          ...patch.layout,
        }
      : nextChildren[index].layout,
  }
  updateComponent({ children: nextChildren })
}
__impl.updateLayoutChild = updateLayoutChild

function moveLayoutChild(index, delta) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  const nextIndex = index + delta
  if (!nextChildren[index] || nextIndex < 0 || nextIndex >= nextChildren.length)
    return
  const [item] = nextChildren.splice(index, 1)
  nextChildren.splice(nextIndex, 0, item)
  updateComponent({ children: nextChildren })
}
__impl.moveLayoutChild = moveLayoutChild

function removeLayoutChild(index) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  if (nextChildren.length <= 1 || !nextChildren[index])
    return
  const [removed] = nextChildren.splice(index, 1)
  updateComponent({ children: nextChildren })
  if (removed?.id === props.selectedId)
    emit('update:selectedId', selectedComponent.value?.id || '')
}
__impl.removeLayoutChild = removeLayoutChild

function addInteractionRule() {
  const nextRules = cloneValue(interactionRules.value || [])
  nextRules.push({
    id: `evt_${Date.now()}`,
    trigger: defaultTrigger.value,
    action: defaultTrigger.value === 'click' ? 'openModal' : 'setValue',
    targetId: '',
    condition: '',
  })
  updateComponent({ props: { __events: nextRules } })
  nextTick(() => {
    const cards = document.querySelectorAll('.interaction-rule-card')
    const last = cards[cards.length - 1]
    last?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}
__impl.addInteractionRule = addInteractionRule

function addInteractionPreset(key = '') {
  const nextRules = cloneValue(interactionRules.value || [])
  const base = {
    id: `evt_${Date.now()}`,
    trigger: defaultTrigger.value,
    action: 'setValue',
    targetId: '',
    whenValue: '',
  }
  const map = {
    openModal: {
      trigger: 'click',
      action: 'openModal',
      modalTitle: '业务弹窗',
      modalContentMode: 'currentForm',
    },
    showHide: {
      trigger: 'change',
      action: 'showHide',
      // 默认「开关打开时显示目标」；不设 whenValue 时条件为空会永远命中，联动看起来无效
      whenValue: 'true',
      value: 'true',
    },
    enableDisable: {
      trigger: 'change',
      action: 'enableDisable',
      value: 'true',
    },
    apiRequest: {
      trigger: defaultTrigger.value,
      action: 'apiRequest',
      api: 'post@/api/action',
    },
  }
  nextRules.push({
    ...base,
    ...(map[key] || {}),
  })
  updateComponent({ props: { __events: nextRules } })
}
__impl.addInteractionPreset = addInteractionPreset

function updateInteractionRule(index, patch = {}) {
  const nextRules = cloneValue(interactionRules.value || [])
  if (!nextRules[index])
    return
  nextRules[index] = {
    ...nextRules[index],
    ...patch,
  }
  updateComponent({ props: { __events: nextRules } })
}
__impl.updateInteractionRule = updateInteractionRule

function updateInteractionRuleJson(index, key, value = '') {
  const patch = { [`${key}Json`]: value }
  try {
    patch[key] = value?.trim() ? JSON.parse(value) : undefined
  }
  catch {
    patch[key] = value
  }
  updateInteractionRule(index, patch)
}
__impl.updateInteractionRuleJson = updateInteractionRuleJson

function removeInteractionRule(index) {
  const nextRules = cloneValue(interactionRules.value || [])
  nextRules.splice(index, 1)
  updateComponent({ props: { __events: nextRules } })
}
__impl.removeInteractionRule = removeInteractionRule

function resolveTriggerLabel(value = '') {
  return triggerOptions.find(item => item.value === value)?.label || '事件规则'
}
__impl.resolveTriggerLabel = resolveTriggerLabel

function resolveActionLabel(value = '') {
  return actionOptions.find(item => item.value === value)?.label || '未选择动作'
}
__impl.resolveActionLabel = resolveActionLabel

function resolveActionValueMeta(action = '') {
  const map = {
    setValue: {
      label: '要填入目标的值',
      placeholder: '例如：已处理',
      help: '触发后会把这个值写入目标组件。',
    },
    showHide: {
      label: '是否显示目标',
      placeholder: 'true 显示，false 隐藏',
      help: '用于根据当前字段值显示或隐藏目标组件。',
    },
    enableDisable: {
      label: '是否启用目标',
      placeholder: 'true 启用，false 禁用',
      help: '用于根据当前字段值启用或禁用目标组件。',
    },
  }
  return map[action] || {
    label: '动作值',
    placeholder: '请输入',
    help: '这条动作执行时使用的值。',
  }
}
__impl.resolveActionValueMeta = resolveActionValueMeta

function openCrudFieldDrawer(componentId = '') {
  editingCrudFieldId.value = componentId
  crudFieldDrawerVisible.value = true
}
__impl.openCrudFieldDrawer = openCrudFieldDrawer

function updateEditingCrudFieldConfig(area, patch = {}) {
  if (!editingCrudFieldId.value)
    return
  updateCrudFieldConfigById(editingCrudFieldId.value, area, patch)
}
__impl.updateEditingCrudFieldConfig = updateEditingCrudFieldConfig

function collectComponentTargetOptions(components = [], result = [], depth = 0) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component?.id)
      return
    const prefix = depth ? `${'　'.repeat(depth)}` : ''
    result.push({
      label: `${prefix}${component.label || component.props?.label || component.props?.title || component.componentKey} (${component.fieldBinding?.fieldCode || component.id})`,
      value: component.id,
    })
    collectComponentTargetOptions(component.children || [], result, depth + 1)
  })
  return result
}
__impl.collectComponentTargetOptions = collectComponentTargetOptions

function collectDrivenRuntimeRules(components = [], sourceField = '', sourceId = '', result = []) {
  const normalizedSourceField = String(sourceField || '').trim()
  if (!normalizedSourceField) {
    return result
  }
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component)
      return
    const rules = Array.isArray(component.props?.runtimeRules) ? component.props.runtimeRules : []
    rules.forEach((rule, ruleIndex) => {
      if (runtimeRuleDependsOnField(rule, normalizedSourceField)) {
        result.push({
          key: `${component.id || component.fieldBinding?.fieldCode || 'target'}:${rule.id || ruleIndex}`,
          targetId: component.id,
          targetLabel: component.label || component.fieldBinding?.fieldCode || component.componentKey || '目标字段',
          summary: summarizeDrivenRuntimeRule(rule, component),
        })
      }
    })
    if (component.id !== sourceId)
      collectDrivenRuntimeRules(component.children || [], normalizedSourceField, sourceId, result)
  })
  return result
}
__impl.collectDrivenRuntimeRules = collectDrivenRuntimeRules

function runtimeRuleDependsOnField(rule = {}, sourceField = '') {
  const conditions = Array.isArray(rule.conditions) ? rule.conditions : []
  if (!conditions.length && rule.field)
    return String(rule.field) === sourceField
  return conditions.some(condition => String(condition?.field || condition?.path || condition?.key || '') === sourceField)
}
__impl.runtimeRuleDependsOnField = runtimeRuleDependsOnField

function summarizeDrivenRuntimeRule(rule = {}, component = {}) {
  const effect = { ...(rule.effect || {}), ...(rule.actions || {}), ...rule }
  const actionText = effect.visible === true
    ? '满足条件时显示'
    : effect.hidden === true
      ? '满足条件时隐藏'
      : effect.readonly === true
        ? '满足条件时只读'
        : effect.required === true
          ? '满足条件时必填'
          : '满足条件时生效'
  const conditions = Array.isArray(rule.conditions) ? rule.conditions : []
  const conditionText = conditions.length
    ? conditions.map(condition => `${condition.field || condition.path || condition.key || '字段'} ${condition.operator || condition.op || 'eq'} ${condition.value ?? condition.expected ?? ''}`.trim()).join('，')
    : '无条件'
  return `${actionText}：${conditionText}；目标 ${component.fieldBinding?.fieldCode || component.id || component.componentKey}`
}
__impl.summarizeDrivenRuntimeRule = summarizeDrivenRuntimeRule

function collectDictTypeFields(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component)
      return
    const dictType = component.props?.dictType || component.dictType || component.basicProps?.dictType || component.advancedProps?.dictType
    if (dictType)
      result.push({ dictType })
    collectDictTypeFields(component.children || [], result)
  })
  return result
}
__impl.collectDictTypeFields = collectDictTypeFields

function updateSelectedCodeDraft(value) {
  selectedCodeDraftTarget.value = props.selectedId
  selectedCodeDraft.value = value
  sourceError.value = ''
}
__impl.updateSelectedCodeDraft = updateSelectedCodeDraft

function resetSelectedCodeDraft() {
  selectedCodeDraftTarget.value = props.selectedId
  selectedCodeDraft.value = selectedComponentCodeRaw.value
  sourceError.value = ''
}
__impl.resetSelectedCodeDraft = resetSelectedCodeDraft

function applySelectedCode() {
  const componentId = designerStore.selectedId || props.selectedId
  if (!componentId)
    return false
  try {
    const parsed = JSON.parse(selectedCodeText.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('当前组件 JSON 必须是对象')
    const nextSchema = replaceComponentInSchema(designerStore.schema, componentId, parsed)
    designerStore.applySchema(normalizeFormDesignerSchema(nextSchema))
    sourceError.value = ''
    selectedCodeDraftTarget.value = ''
    selectedCodeDraft.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}
__impl.applySelectedCode = applySelectedCode

function updateSchemaCodeDraft(value) {
  schemaCodeDirty.value = true
  schemaCodeDraft.value = value
  sourceError.value = ''
}
__impl.updateSchemaCodeDraft = updateSchemaCodeDraft

function resetSchemaCodeDraft() {
  schemaCodeDirty.value = true
  schemaCodeDraft.value = schemaCodeRaw.value
  sourceError.value = ''
}
__impl.resetSchemaCodeDraft = resetSchemaCodeDraft

function applySchemaCode() {
  try {
    const parsed = JSON.parse(schemaCodeText.value || '{}')
    designerStore.applySchema(normalizeFormDesignerSchema(parsed))
    sourceError.value = ''
    schemaCodeDirty.value = false
    schemaCodeDraft.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}
__impl.applySchemaCode = applySchemaCode

function applySourceModalCode() {
  const applied = selectedComponent.value ? applySelectedCode() : applySchemaCode()
  if (applied)
    sourceModalVisible.value = false
}
__impl.applySourceModalCode = applySourceModalCode

function cancelSourceModalEdit() {
  if (selectedComponent.value)
    resetSelectedCodeDraft()
  else
    resetSchemaCodeDraft()
  sourceModalVisible.value = false
}
__impl.cancelSourceModalEdit = cancelSourceModalEdit

function replaceComponentInSchema(schema = {}, componentId = '', replacement = {}) {
  const nextSchema = cloneValue(schema)
  const path = findDesignerComponentPath(nextSchema, componentId)
  if (!path)
    return nextSchema
  let children = nextSchema.components || []
  for (let index = 0; index < path.length - 1; index += 1)
    children = children[path[index]].children || []
  children[path[path.length - 1]] = replacement
  return nextSchema
}
__impl.replaceComponentInSchema = replaceComponentInSchema

__impl.colorToHexInput = colorToHexInput
__impl.hexInputToColor = hexInputToColor

function normalizeAppearanceColor(value, fallback = '#ffffff') {
  return hexInputToColor(value, fallback)
}
__impl.normalizeAppearanceColor = normalizeAppearanceColor

function normalizeOpacityPercent(value, fallback = 100) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.min(100, Math.max(20, number))
}
__impl.normalizeOpacityPercent = normalizeOpacityPercent

function updateSelectedAppearanceBackground(value) {
  updateDesignerStyle({ backgroundColor: normalizeAppearanceColor(value, 'transparent') })
}
__impl.updateSelectedAppearanceBackground = updateSelectedAppearanceBackground

function updateSelectedAppearanceBorder(value) {
  updateDesignerBorderColor(normalizeAppearanceColor(value, '#e4e4e7'))
}
__impl.updateSelectedAppearanceBorder = updateSelectedAppearanceBorder

function updateSelectedAppearanceOpacity(value) {
  updateDesignerStyle({ opacity: normalizeOpacityPercent(value, selectedOpacityPercent.value) / 100 })
}
__impl.updateSelectedAppearanceOpacity = updateSelectedAppearanceOpacity

function updateDesignerStyle(stylePatch = {}) {
  updateComponent({
    props: {
      __designerStyle: {
        ...selectedDesignerStyle.value,
        ...stylePatch,
      },
    },
  })
}
__impl.updateDesignerStyle = updateDesignerStyle

function resolveActiveBorderWidthPx(current, fallbackPx = 1) {
  const width = resolvePxNumber(current, 0)
  return valueToPx(width > 0 ? width : fallbackPx)
}

function updateDesignerBorderStyle(value) {
  if (value === 'none') {
    updateDesignerStyle({
      borderStyle: 'none',
      borderColor: 'transparent',
      borderWidth: '0px',
      hideInnerBorder: true,
    })
    return
  }
  updateDesignerStyle({
    borderStyle: value || undefined,
    borderColor: selectedDesignerStyle.value.borderColor === 'transparent'
      ? undefined
      : selectedDesignerStyle.value.borderColor,
    borderWidth: resolveActiveBorderWidthPx(selectedDesignerStyle.value.borderWidth),
    hideInnerBorder: false,
  })
}
__impl.updateDesignerBorderStyle = updateDesignerBorderStyle

function updateDesignerBorderColor(value) {
  if (!value) {
    updateDesignerStyle({ borderColor: undefined })
    return
  }
  updateDesignerStyle({
    borderColor: value,
    borderStyle: selectedDesignerStyle.value.borderStyle === 'none'
      ? 'solid'
      : selectedDesignerStyle.value.borderStyle || 'solid',
    borderWidth: resolveActiveBorderWidthPx(selectedDesignerStyle.value.borderWidth),
    hideInnerBorder: false,
  })
}
__impl.updateDesignerBorderColor = updateDesignerBorderColor

function updateDesignerBorderWidth(value) {
  const width = Math.max(0, Math.min(32, resolvePxNumber(value, 1)))
  if (width <= 0) {
    updateDesignerStyle({
      borderWidth: '0px',
      borderStyle: 'none',
      borderColor: 'transparent',
      hideInnerBorder: true,
    })
    return
  }
  updateDesignerStyle({
    borderWidth: valueToPx(width),
    borderStyle: selectedDesignerStyle.value.borderStyle === 'none'
      ? 'solid'
      : selectedDesignerStyle.value.borderStyle || 'solid',
    hideInnerBorder: false,
  })
}
__impl.updateDesignerBorderWidth = updateDesignerBorderWidth

function updateWidthMode(value) {
  updateDesignerStyle({
    widthMode: value,
    width: value === 'fill' ? '100%' : undefined,
  })
}
__impl.updateWidthMode = updateWidthMode

function updateHeightMode(value) {
  updateDesignerStyle({
    heightMode: value,
    minHeight: value === 'fill' ? '100%' : undefined,
    height: value === 'default' ? undefined : 'auto',
  })
}
__impl.updateHeightMode = updateHeightMode

function updateComponentStyleText(value) {
  updateDesignerStyle({
    customStyleText: value,
    customStyle: parseStyleText(value),
  })
}
__impl.updateComponentStyleText = updateComponentStyleText

function updateDesignerSpacing(key, value) {
  const nextCustomStyle = applyStyleValue(selectedDesignerStyle.value.customStyle || {}, key, valueToPx(value))
  updateDesignerStyle({
    customStyle: nextCustomStyle,
    customStyleText: stringifyStyle(nextCustomStyle),
  })
}
__impl.updateDesignerSpacing = updateDesignerSpacing

/** 四向间距：与列表 SpacingBoxControl 的 { top,right,bottom,left } | number 对齐 */
function resolveStyleBox(styleSource, prefix) {
  const style = styleSource || {}
  return {
    top: resolvePxNumber(style[`${prefix}Top`], 0),
    right: resolvePxNumber(style[`${prefix}Right`], 0),
    bottom: resolvePxNumber(style[`${prefix}Bottom`], 0),
    left: resolvePxNumber(style[`${prefix}Left`], 0),
  }
}
__impl.resolveStyleBox = resolveStyleBox

function stylePatchFromBox(prefix, value) {
  const box = value && typeof value === 'object' && !Array.isArray(value)
    ? {
        top: value.top ?? 0,
        right: value.right ?? 0,
        bottom: value.bottom ?? 0,
        left: value.left ?? 0,
      }
    : {
        top: value,
        right: value,
        bottom: value,
        left: value,
      }
  return {
    [`${prefix}Top`]: valueToPx(box.top),
    [`${prefix}Right`]: valueToPx(box.right),
    [`${prefix}Bottom`]: valueToPx(box.bottom),
    [`${prefix}Left`]: valueToPx(box.left),
  }
}

function updateDesignerBoxSpacing(prefix, value) {
  const patch = stylePatchFromBox(prefix, value)
  const nextCustomStyle = Object.entries(patch).reduce(
    (style, [key, next]) => applyStyleValue(style, key, next),
    { ...(selectedDesignerStyle.value.customStyle || {}) },
  )
  updateDesignerStyle({
    customStyle: nextCustomStyle,
    customStyleText: stringifyStyle(nextCustomStyle),
  })
}
__impl.updateDesignerBoxSpacing = updateDesignerBoxSpacing

function updateDesignerCustomStyle(patch = {}) {
  const nextCustomStyle = Object.entries(patch).reduce((style, [key, value]) => applyStyleValue(style, key, value), {
    ...(selectedDesignerStyle.value.customStyle || {}),
  })
  updateDesignerStyle({
    customStyle: nextCustomStyle,
    customStyleText: stringifyStyle(nextCustomStyle),
  })
}
__impl.updateDesignerCustomStyle = updateDesignerCustomStyle

function updateDesignerTranslate(axis, value) {
  const current = selectedDesignerTranslate.value
  const next = {
    x: axis === 'x' ? normalizePositionNumber(value) : current.x,
    y: axis === 'y' ? normalizePositionNumber(value) : current.y,
  }
  updateDesignerCustomStyle({ transform: formatTranslateStyle(next) })
}
__impl.updateDesignerTranslate = updateDesignerTranslate

function updateFormStyle(stylePatch = {}) {
  const next = { ...(formStyle.value || {}) }
  const patch = stylePatch && typeof stylePatch === 'object' && !Array.isArray(stylePatch)
    ? stylePatch
    : {}
  Object.entries(patch).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '')
      delete next[key]
    else
      next[key] = value
  })
  updateFormLayout({
    formStyle: next,
    formStyleText: stringifyStyle(next),
  })
}
__impl.updateFormStyle = updateFormStyle

function updateFormAppearanceBackground(value) {
  updateFormStyle({ backgroundColor: normalizeAppearanceColor(value, 'transparent') })
}
__impl.updateFormAppearanceBackground = updateFormAppearanceBackground

function updateFormAppearanceBorder(value) {
  updateFormStyle({
    borderColor: normalizeAppearanceColor(value, '#e4e4e7'),
    borderStyle: formStyle.value.borderStyle === 'none'
      ? 'solid'
      : formStyle.value.borderStyle || 'solid',
    borderWidth: resolveActiveBorderWidthPx(formStyle.value.borderWidth),
  })
}
__impl.updateFormAppearanceBorder = updateFormAppearanceBorder

function updateFormBorderStyle(value) {
  if (value === 'none') {
    updateFormStyle({
      borderStyle: 'none',
      borderColor: 'transparent',
      borderWidth: '0px',
    })
    return
  }
  updateFormStyle({
    borderStyle: value || 'solid',
    borderColor: formStyle.value.borderColor === 'transparent'
      ? '#e4e4e7'
      : formStyle.value.borderColor,
    borderWidth: resolveActiveBorderWidthPx(formStyle.value.borderWidth),
  })
}
__impl.updateFormBorderStyle = updateFormBorderStyle

function updateFormBorderWidth(value) {
  const width = Math.max(0, Math.min(32, resolvePxNumber(value, 1)))
  if (width <= 0) {
    updateFormStyle({
      borderWidth: '0px',
      borderStyle: 'none',
      borderColor: 'transparent',
    })
    return
  }
  updateFormStyle({
    borderWidth: valueToPx(width),
    borderStyle: formStyle.value.borderStyle === 'none'
      ? 'solid'
      : formStyle.value.borderStyle || 'solid',
  })
}
__impl.updateFormBorderWidth = updateFormBorderWidth

function updateFormAppearanceOpacity(value) {
  updateFormStyle({ opacity: normalizeOpacityPercent(value, formOpacityPercent.value) / 100 })
}
__impl.updateFormAppearanceOpacity = updateFormAppearanceOpacity

function updateFormStyleText(value) {
  updateFormLayout({
    formStyleText: value,
    formStyle: parseStyleText(value),
  })
}
__impl.updateFormStyleText = updateFormStyleText

function updateFormSpacing(key, value) {
  const nextStyle = applyStyleValue(formStyle.value || {}, key, valueToPx(value))
  updateFormLayout({
    formStyle: nextStyle,
    formStyleText: stringifyStyle(nextStyle),
  })
}
__impl.updateFormSpacing = updateFormSpacing

function updateFormBoxSpacing(prefix, value) {
  const patch = stylePatchFromBox(prefix, value)
  const nextStyle = Object.entries(patch).reduce(
    (style, [key, next]) => applyStyleValue(style, key, next),
    { ...(formStyle.value || {}) },
  )
  updateFormLayout({
    formStyle: nextStyle,
    formStyleText: stringifyStyle(nextStyle),
  })
}
__impl.updateFormBoxSpacing = updateFormBoxSpacing

function updateFormTranslate(axis, value) {
  const current = formTranslate.value
  const next = {
    x: axis === 'x' ? normalizePositionNumber(value) : current.x,
    y: axis === 'y' ? normalizePositionNumber(value) : current.y,
  }
  updateFormStyle({ transform: formatTranslateStyle(next) })
}
__impl.updateFormTranslate = updateFormTranslate

function updateCrudApiBase(value) {
  const apiBase = normalizeApiBase(value)
  updateComponent({
    props: {
      apiBase,
      apiConfig: buildDefaultCrudApiConfig(apiBase),
    },
  })
}
__impl.updateCrudApiBase = updateCrudApiBase

function updateCrudApiConfig(key, value) {
  updateComponent({
    props: {
      apiConfig: {
        ...crudApiConfig.value,
        [key]: value,
      },
    },
  })
}
__impl.updateCrudApiConfig = updateCrudApiConfig

function resolveCrudApiMethodLabel(item = {}) {
  const apiValue = crudApiConfig.value?.[item.key] || item.placeholder || ''
  const method = String(apiValue).split('@')[0]?.trim()?.toUpperCase()
  if (method === 'DELETE')
    return 'DEL'
  return method || 'API'
}
__impl.resolveCrudApiMethodLabel = resolveCrudApiMethodLabel

function resolveCrudApiMethodClass(item = {}) {
  const label = resolveCrudApiMethodLabel(item).toLowerCase()
  if (label === 'del')
    return 'delete'
  return ['get', 'post', 'put'].includes(label) ? label : 'post'
}
__impl.resolveCrudApiMethodClass = resolveCrudApiMethodClass

function updateCrudOption(key, value) {
  updateComponent({
    props: {
      crudOptions: {
        ...crudOptions.value,
        [key]: value,
      },
    },
  })
}
__impl.updateCrudOption = updateCrudOption

function updateCrudExpandEnabled(enabled) {
  const current = crudOptions.value?.expandConfig || {}
  updateCrudOption('expandConfig', enabled
    ? {
        enabled: true,
        trigger: current.trigger || 'icon',
        lazy: current.lazy !== false,
        cache: current.cache !== false,
        layout: current.layout || { mode: 'single', density: 'compact', padding: 12 },
        panels: current.panels?.length ? current.panels : [createDefaultCrudExpandPanel()],
      }
    : { ...current, enabled: false })
}
__impl.updateCrudExpandEnabled = updateCrudExpandEnabled

function updateCrudExpandConfig(patch = {}) {
  const current = crudOptions.value?.expandConfig || {}
  updateCrudOption('expandConfig', {
    ...current,
    ...patch,
    enabled: current.enabled === true,
  })
}
__impl.updateCrudExpandConfig = updateCrudExpandConfig

function updateFirstCrudExpandPanel(patch = {}) {
  const current = crudOptions.value?.expandConfig || {}
  const panels = current.panels?.length ? [...current.panels] : [createDefaultCrudExpandPanel()]
  panels[0] = normalizeCrudExpandPanelPatch({ ...panels[0], ...patch })
  updateCrudOption('expandConfig', {
    ...current,
    enabled: true,
    panels,
  })
}
__impl.updateFirstCrudExpandPanel = updateFirstCrudExpandPanel

function updateFirstCrudExpandDataSource(patch = {}) {
  updateFirstCrudExpandPanel({
    dataSource: {
      ...(firstCrudExpandPanel.value?.dataSource || {}),
      ...patch,
    },
  })
}
__impl.updateFirstCrudExpandDataSource = updateFirstCrudExpandDataSource

function updateFirstCrudExpandParamsMap(value) {
  updateFirstCrudExpandDataSource({ paramsMap: parseJsonObjectProp(value) })
}
__impl.updateFirstCrudExpandParamsMap = updateFirstCrudExpandParamsMap

function updateFirstCrudExpandDescriptionFields(value) {
  const fields = Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : String(value || '').split(/\r?\n|,/).map(item => item.trim()).filter(Boolean)
  updateFirstCrudExpandPanel({
    descriptions: {
      ...(firstCrudExpandPanel.value?.descriptions || {}),
      fields: fields.map(field => ({ field, label: resolveCrudFieldLabel(field) })),
    },
  })
}
__impl.updateFirstCrudExpandDescriptionFields = updateFirstCrudExpandDescriptionFields

function resolveCrudExpandDescriptionFieldKeys(panel) {
  const configured = (panel?.descriptions?.fields || [])
    .map(field => field.field || field.key)
    .filter(Boolean)
  if (configured.length)
    return configured
  return resolveDefaultCrudDescriptionFields().map(field => field.field).filter(Boolean)
}
__impl.resolveCrudExpandDescriptionFieldKeys = resolveCrudExpandDescriptionFieldKeys

function resolveCrudExpandDescriptionSelectedFields(panel) {
  const optionMap = new Map(crudDescriptionFieldOptions.value.map(option => [option.value, option]))
  return resolveCrudExpandDescriptionFieldKeys(panel).map((field) => {
    const option = optionMap.get(field)
    return {
      field,
      label: option?.rawLabel || resolveCrudFieldLabel(field),
      componentKey: option?.componentKey,
    }
  })
}
__impl.resolveCrudExpandDescriptionSelectedFields = resolveCrudExpandDescriptionSelectedFields

function resolveCrudExpandDescriptionPanelFields(panel) {
  const selectedKeys = resolveCrudExpandDescriptionFieldKeys(panel)
  const selected = new Set(selectedKeys)
  const optionMap = new Map(crudDescriptionFieldOptions.value.map(option => [option.value, option]))
  const orderedKeys = [
    ...selectedKeys,
    ...Array.from(optionMap.keys()).filter(key => !selected.has(key)),
  ]
  return orderedKeys
    .map((field) => {
      const option = optionMap.get(field)
      if (!option && !selected.has(field))
        return null
      return {
        field,
        label: option?.rawLabel || resolveCrudFieldLabel(field),
        componentKey: option?.componentKey,
        selected: selected.has(field),
      }
    })
    .filter(Boolean)
}
__impl.resolveCrudExpandDescriptionPanelFields = resolveCrudExpandDescriptionPanelFields

function addFirstCrudExpandDescriptionField(field) {
  if (!field)
    return
  const fields = resolveCrudExpandDescriptionFieldKeys(firstCrudExpandPanel.value)
  updateFirstCrudExpandDescriptionFields(Array.from(new Set([...fields, field])))
}
__impl.addFirstCrudExpandDescriptionField = addFirstCrudExpandDescriptionField

function removeFirstCrudExpandDescriptionField(field) {
  const fields = resolveCrudExpandDescriptionFieldKeys(firstCrudExpandPanel.value)
  updateFirstCrudExpandDescriptionFields(fields.filter(item => item !== field))
}
__impl.removeFirstCrudExpandDescriptionField = removeFirstCrudExpandDescriptionField

function toggleFirstCrudExpandDescriptionField(field, visible) {
  if (visible)
    addFirstCrudExpandDescriptionField(field)
  else
    removeFirstCrudExpandDescriptionField(field)
}
__impl.toggleFirstCrudExpandDescriptionField = toggleFirstCrudExpandDescriptionField

function handleCrudExpandDescriptionPanelReorder(fields = []) {
  updateFirstCrudExpandDescriptionFields(fields.filter(field => field?.selected).map(field => field.field))
}
__impl.handleCrudExpandDescriptionPanelReorder = handleCrudExpandDescriptionPanelReorder

function resolveCrudBitableFieldIconComponent(field = {}) {
  const key = String(field.componentKey || '').toLowerCase()
  if (['number', 'inputnumber', 'input-number', 'rate', 'slider'].includes(key))
    return BitableNumberIcon
  if (['date', 'datetime', 'datepicker', 'timepicker', 'time', 'month', 'year'].includes(key))
    return BitableCalendarIcon
  if (['select', 'dictselect', 'radio', 'radiobutton', 'checkbox', 'cascader', 'treeselect', 'switch'].includes(key))
    return BitableSelectIcon
  if (['upload', 'fileupload', 'imageupload'].includes(key))
    return BitableAttachmentIcon
  if (['userselect', 'deptselect'].includes(key))
    return BitableMemberIcon
  if (['lookup', 'relation', 'reference'].includes(key))
    return BitableLookupIcon
  return BitableStyleIcon
}
__impl.resolveCrudBitableFieldIconComponent = resolveCrudBitableFieldIconComponent

function createDefaultCrudExpandPanel() {
  return {
    key: 'summary',
    title: '概览',
    type: 'descriptions',
    dataSource: { type: 'row' },
    descriptions: {
      columns: 3,
      fields: resolveDefaultCrudDescriptionFields(),
    },
  }
}
__impl.createDefaultCrudExpandPanel = createDefaultCrudExpandPanel

function normalizeCrudExpandPanelPatch(panel = {}) {
  const type = panel.type || 'descriptions'
  const quantityPanel = ['quantity-balance', 'quantity-ledger', 'quantity-lock'].includes(type)
  return {
    ...panel,
    key: panel.key || (type === 'table' ? 'detailTable' : quantityPanel ? type : 'summary'),
    title: panel.title || (type === 'table' ? '明细' : quantityPanel ? expandPanelTypeOptions.find(item => item.value === type)?.label : '概览'),
    type,
    dataSource: quantityPanel ? { ...(panel.dataSource || {}), type: 'quantity', queryType: type } : panel.dataSource || { type: 'row' },
    quantity: quantityPanel ? { ...(panel.quantity || {}), queryType: type } : panel.quantity,
    descriptions: panel.descriptions || { columns: 3, fields: resolveDefaultCrudDescriptionFields() },
    table: panel.table || { rowKey: 'id', columns: [], pagination: false, maxHeight: 320 },
  }
}
__impl.normalizeCrudExpandPanelPatch = normalizeCrudExpandPanelPatch

function resolveDefaultCrudDescriptionFields() {
  return crudConfigFields.value.slice(0, 6).map(field => ({
    field: field.fieldCode,
    label: field.label || field.fieldCode,
  }))
}
__impl.resolveDefaultCrudDescriptionFields = resolveDefaultCrudDescriptionFields

function resolveCrudFieldLabel(fieldCode) {
  const field = crudConfigFields.value.find(item => item.fieldCode === fieldCode || item.id === fieldCode)
  return field?.label || fieldCode
}
__impl.resolveCrudFieldLabel = resolveCrudFieldLabel

function parseJsonObjectProp(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  try {
    const parsed = JSON.parse(text)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}
__impl.parseJsonObjectProp = parseJsonObjectProp

// 编辑打开方式/弹窗宽度/抽屉方向逻辑已随 panels/FormLayoutPanel.vue 迁出；updateFormLayout 统一走 store

function updateCrudFieldRole(componentId, role, value) {
  if (!componentId)
    return
  const field = getDesignerComponent(designerStore.schema, componentId)
  const currentRoles = field?.props?.__crudRoles || {}
  designerStore.updateComponent(componentId, {
    props: {
      __crudRoles: {
        ...currentRoles,
        [role]: Boolean(value),
      },
    },
  })
}
__impl.updateCrudFieldRole = updateCrudFieldRole

function updateCrudFieldConfig(area, patch = {}) {
  if (!props.selectedId || !area)
    return
  const current = selectedComponent.value?.props?.__crudConfig || {}
  updateComponent({
    props: {
      __crudConfig: {
        ...current,
        [area]: {
          ...(current[area] || {}),
          ...patch,
        },
      },
    },
  })
}
__impl.updateCrudFieldConfig = updateCrudFieldConfig

function updateCrudFieldConfigById(componentId, area, patch = {}) {
  if (!componentId || !area)
    return
  const field = getDesignerComponent(designerStore.schema, componentId)
  const current = field?.props?.__crudConfig || {}
  designerStore.updateComponent(componentId, {
    props: {
      __crudConfig: {
        ...current,
        [area]: {
          ...(current[area] || {}),
          ...patch,
        },
      },
    },
  })
}
__impl.updateCrudFieldConfigById = updateCrudFieldConfigById

function updateOption(index, patch = {}) {
  const nextOptions = cloneValue(selectedOptions.value || [])
  if (!nextOptions[index])
    return
  nextOptions[index] = {
    ...nextOptions[index],
    ...patch,
  }
  updateComponent({ props: { options: nextOptions } })
}
__impl.updateOption = updateOption

/** 选项名输入：值未被单独改过时自动跟随名称，避免窄面板内同时维护两个字段 */
function updateOptionLabel(index, label = '') {
  const option = selectedOptions.value?.[index] || {}
  const valueFollowsLabel = String(option.value ?? '') === '' || String(option.value) === String(option.label ?? '')
  updateOption(index, valueFollowsLabel ? { label, value: label } : { label })
}
__impl.updateOptionLabel = updateOptionLabel

/** 行尾图标切换选项禁用（替代原行内开关，减少一行控件数） */
function toggleOptionDisabled(index) {
  updateOption(index, { disabled: !selectedOptions.value?.[index]?.disabled })
}
__impl.toggleOptionDisabled = toggleOptionDisabled

function updateOptionJsonProps(index, value = '') {
  try {
    updateOption(index, { props: value?.trim() ? JSON.parse(value) : undefined })
  }
  catch {
    updateOption(index, { props: value })
  }
}
__impl.updateOptionJsonProps = updateOptionJsonProps

function addOption() {
  const nextOptions = cloneValue(selectedOptions.value || [])
  const nextIndex = nextOptions.length + 1
  // 值跟随名称（宜搭式）：新选项 value = label，需要不同值时勾选"自定义值"单独修改
  const label = `选项${nextIndex}`
  nextOptions.push({ label, value: label })
  updateComponent({ props: { options: nextOptions } })
}
__impl.addOption = addOption

function removeOption(index) {
  const nextOptions = cloneValue(selectedOptions.value || [])
  nextOptions.splice(index, 1)
  updateComponent({ props: { options: nextOptions } })
}
__impl.removeOption = removeOption

function stringifyJsonProp(value) {
  if (value === undefined || value === null || value === '')
    return ''
  if (typeof value === 'string')
    return value
  return JSON.stringify(value, null, 2)
}
__impl.stringifyJsonProp = stringifyJsonProp

function resolveCrudSwitchValue(key, defaultValue = false) {
  const value = crudOptions.value?.[key]
  return value === undefined ? defaultValue : Boolean(value)
}
__impl.resolveCrudSwitchValue = resolveCrudSwitchValue

function collectCrudConfigFields(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component)
      return
    if (isFieldComponent(component)) {
      const index = result.length
      result.push({
        id: component.id,
        label: component.label || component.fieldBinding?.fieldCode || component.componentKey,
        fieldCode: component.fieldBinding?.fieldCode || component.id,
        componentKey: component.componentKey,
        roles: resolveCrudFieldRoles(component, index),
        config: component.props?.__crudConfig || {},
      })
      return
    }
    collectCrudConfigFields(component.children || [], result)
  })
  return result
}
__impl.collectCrudConfigFields = collectCrudConfigFields

function resolveCrudFieldRoles(component = {}, index = 0) {
  const roles = component.props?.__crudRoles || {}
  return {
    search: Object.prototype.hasOwnProperty.call(roles, 'search') ? roles.search !== false : index < 3,
    table: Object.prototype.hasOwnProperty.call(roles, 'table') ? roles.table !== false : true,
    edit: Object.prototype.hasOwnProperty.call(roles, 'edit') ? roles.edit !== false : true,
  }
}
__impl.resolveCrudFieldRoles = resolveCrudFieldRoles

function updateFormLayout(patch = {}) {
  designerStore.updateLayout(patch)
}
__impl.updateFormLayout = updateFormLayout

function updateRowTotalColumns(value) {
  const columns = normalizeGridCount(value)
  const row = selectedComponent.value
  if (!row)
    return
  const children = cloneValue(row.children || []).map(child => child?.componentKey === 'col'
    ? {
        ...child,
        props: { ...(child.props || {}), span: Math.min(columns, normalizeGridCount(child.props?.span || child.layout?.span || 1)) },
        layout: { ...(child.layout || {}), span: Math.min(columns, normalizeGridCount(child.layout?.span || child.props?.span || 1)) },
      }
    : child)
  updateComponent({
    props: {
      ...(row.props || {}),
      columns,
      gutter: row.props?.gutter ?? 12,
    },
    children,
  })
}
__impl.updateRowTotalColumns = updateRowTotalColumns

function updateRowCellCount(value) {
  const count = normalizeGridCount(value)
  const row = selectedComponent.value
  if (!row)
    return
  const totalColumns = normalizeGridCount(row.props?.columns || maxFormGridColumns)
  const defaultSpan = Math.max(1, Math.floor(totalColumns / Math.max(1, count)))

  const currentChildren = Array.isArray(row.children) ? cloneValue(row.children) : []
  const currentColumns = currentChildren.filter(child => child?.componentKey === 'col')
  const directChildren = currentChildren.filter(child => child?.componentKey !== 'col')
  if (!currentColumns.length && directChildren.length) {
    currentColumns.push(createColumn(row.id, 0))
    currentColumns[0].children = directChildren
  }
  else if (currentColumns.length && directChildren.length) {
    currentColumns[0].children = [
      ...(currentColumns[0].children || []),
      ...directChildren,
    ]
  }

  const nextColumns = currentColumns.slice(0, count)
  while (nextColumns.length < count)
    nextColumns.push(createColumn(row.id, nextColumns.length, defaultSpan))

  const overflowColumns = currentColumns.slice(count)
  if (overflowColumns.length && nextColumns.length) {
    const lastColumn = nextColumns[nextColumns.length - 1]
    lastColumn.children = [
      ...(lastColumn.children || []),
      ...overflowColumns.flatMap(column => column.children || []),
    ]
  }

  nextColumns.forEach((column, index) => {
    const span = Math.min(totalColumns, normalizeGridCount(column.layout?.span || column.props?.span || defaultSpan))
    column.label = `第 ${index + 1} 列`
    column.layout = { ...(column.layout || {}), span }
    column.props = { ...(column.props || {}), span }
  })

  updateComponent({
    label: `${count} 列栅格`,
    props: {
      // 展开原 props：列数调整是布局重排，rowGap/gutter 等其它栅格配置必须原样保留
      ...(row.props || {}),
      columns: totalColumns,
      gutter: row.props?.gutter ?? 12,
    },
    children: nextColumns,
  })
}
__impl.updateRowCellCount = updateRowCellCount

function updateRowColumnSpan(index, value) {
  const row = selectedComponent.value
  if (!row || !Array.isArray(row.children))
    return
  const span = Math.min(normalizeGridCount(row.props?.columns || maxFormGridColumns), normalizeGridCount(value))
  const children = cloneValue(row.children)
  const childIndex = children.reduce((matchedIndex, child, currentIndex) => {
    if (matchedIndex !== -1 || child?.componentKey !== 'col')
      return matchedIndex
    const currentColumnIndex = children.slice(0, currentIndex + 1).filter(item => item?.componentKey === 'col').length - 1
    return currentColumnIndex === index ? currentIndex : -1
  }, -1)
  const column = children[childIndex]
  if (!column || column.componentKey !== 'col')
    return
  children[childIndex] = {
    ...column,
    props: { ...(column.props || {}), span },
    layout: { ...(column.layout || {}), span },
  }
  updateComponent({ children })
}
__impl.updateRowColumnSpan = updateRowColumnSpan

function createColumn(rowId, index, span = 6) {
  const normalizedSpan = normalizeGridCount(span)
  return {
    id: `${rowId || 'row'}_col_${Date.now()}_${index + 1}`,
    componentKey: 'col',
    label: `第 ${index + 1} 列`,
    props: { span: normalizedSpan },
    layout: { span: normalizedSpan, align: 'left' },
    children: [],
  }
}
__impl.createColumn = createColumn

// normalizeGridCount / normalizeLabelWidthInput 已下沉 formLayoutConfig.js

function normalizeApiBase(value) {
  const text = String(value || '').trim().replace(/\/+/g, '/')
  if (!text)
    return '/ai/crud/business_object'
  const withSlash = text.startsWith('/') ? text : `/${text}`
  // 历史 kebab / formKey 路径纠正为对象 CRUD configKey（剥 *_default_form）
  const key = withSlash
    .replace(/^\/ai\/crud\//i, '')
    .replace(/^\//, '')
    .replace(/-/g, '_')
    .replace(/[^a-z0-9_]/gi, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .replace(/_default_form$/i, '')
  return `/ai/crud/${key || 'business_object'}`
}
__impl.normalizeApiBase = normalizeApiBase

function buildDefaultCrudApiConfig(apiBase = '/ai/crud/business_object') {
  const prefix = String(apiBase || '').trim() || '/ai/crud/business_object'
  return {
    list: `get@${prefix}/page`,
    detail: `get@${prefix}/:id`,
    create: `post@${prefix}`,
    update: `put@${prefix}`,
    delete: `delete@${prefix}/:id`,
  }
}
__impl.buildDefaultCrudApiConfig = buildDefaultCrudApiConfig

function hasAncestorComponent(schema = {}, componentId = '', componentKeys = []) {
  const path = findDesignerComponentPath(schema, componentId)
  if (!path || path.length <= 1)
    return false
  const wanted = new Set(componentKeys)
  let children = schema.components || []
  for (let index = 0; index < path.length - 1; index += 1) {
    const component = children[path[index]]
    if (!component)
      return false
    if (wanted.has(component.componentKey))
      return true
    children = component.children || []
  }
  return false
}
__impl.hasAncestorComponent = hasAncestorComponent

function resolvePxNumber(value, fallback = 0) {
  const match = String(value ?? '').match(/-?\d+(?:\.\d+)?/)
  if (!match)
    return fallback
  return Number(match[0])
}
__impl.resolvePxNumber = resolvePxNumber

function normalizePositionNumber(value) {
  const next = Number(value)
  return Number.isFinite(next) ? next : 0
}
__impl.normalizePositionNumber = normalizePositionNumber

function parseTranslateStyle(value = '') {
  const text = String(value || '')
  const translateMatch = text.match(/translate(?:3d)?\(\s*(-?\d+(?:\.\d+)?)px(?:\s*,\s*(-?\d+(?:\.\d+)?)px)?/)
  if (translateMatch) {
    return {
      x: Number(translateMatch[1]) || 0,
      y: Number(translateMatch[2]) || 0,
    }
  }
  const xMatch = text.match(/translateX\(\s*(-?\d+(?:\.\d+)?)px\)/)
  const yMatch = text.match(/translateY\(\s*(-?\d+(?:\.\d+)?)px\)/)
  return {
    x: xMatch ? Number(xMatch[1]) || 0 : 0,
    y: yMatch ? Number(yMatch[1]) || 0 : 0,
  }
}
__impl.parseTranslateStyle = parseTranslateStyle

function formatTranslateStyle(position = {}) {
  const x = normalizePositionNumber(position.x)
  const y = normalizePositionNumber(position.y)
  if (!x && !y)
    return undefined
  return `translate(${x}px, ${y}px)`
}
__impl.formatTranslateStyle = formatTranslateStyle

function parseStyleText(value = '') {
  return String(value || '')
    .split(';')
    .map(item => item.trim())
    .filter(Boolean)
    .reduce((style, item) => {
      const [rawKey, ...rawValue] = item.split(':')
      const key = rawKey?.trim()
      const nextValue = rawValue.join(':').trim()
      if (!key || !nextValue)
        return style
      style[toCamelCase(key)] = nextValue
      return style
    }, {})
}
__impl.parseStyleText = parseStyleText

function stringifyStyle(style = {}) {
  return Object.entries(style || {})
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${toKebabCase(key)}: ${value}`)
    .join('; ')
}
__impl.stringifyStyle = stringifyStyle

function applyStyleValue(source = {}, key, value) {
  const next = { ...(source || {}) }
  if (value === undefined || value === null || value === '')
    delete next[key]
  else
    next[key] = value
  return next
}
__impl.applyStyleValue = applyStyleValue

function valueToPx(value) {
  if (value === undefined || value === null || value === '')
    return undefined
  const number = Number(value)
  return Number.isFinite(number) ? `${number}px` : undefined
}
__impl.valueToPx = valueToPx

function toCamelCase(value = '') {
  return String(value).replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())
}
__impl.toCamelCase = toCamelCase

function toKebabCase(value = '') {
  return String(value).replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`)
}
__impl.toKebabCase = toKebabCase

function handleOpenSourcePanel() {
  sourceError.value = ''
  sourceModalVisible.value = true
}
__impl.handleOpenSourcePanel = handleOpenSourcePanel

onMounted(() => {
  window.addEventListener('forge-form-designer:open-source-panel', handleOpenSourcePanel)
  loadCodeRuleOptions()
})

onBeforeUnmount(() => {
  window.removeEventListener('forge-form-designer:open-source-panel', handleOpenSourcePanel)
})
  return {
    ...deps,
    __impl,
    mut,
    designerStore,
    message,
    BitableStyleIcon,
    BitableSelectIcon,
    BitableNumberIcon,
    BitableCalendarIcon,
    BitableAttachmentIcon,
    BitableMemberIcon,
    BitableLookupIcon,
    crudFieldDrawerVisible,
    editingCrudFieldId,
    selectedCodeDraft,
    selectedCodeDraftTarget,
    schemaCodeDraft,
    schemaCodeDirty,
    sourceError,
    sourceModalVisible,
    dictDefaultOptions,
    dictDefaultOptionsLoading,
    selectedComponent,
    isField,
    isLayout,
    isCrudBlock,
    crudApiConfig,
    crudOptions,
    selectedDesignerStyle,
    formStyle,
    layoutChildren,
    interactionRules,
    defaultTrigger,
    selectedComponentCodeRaw,
    selectedCodeText,
    schemaCodeRaw,
    schemaCodeText,
    selectedOpacityPercent,
    formOpacityPercent,
    selectedDesignerTranslate,
    formTranslate,
    maxFormGridColumns,
    triggerOptions,
    actionOptions,
    expandPanelTypeOptions,
    crudConfigFields,
    crudDescriptionFieldOptions,
    firstCrudExpandPanel,
    selectedOptions,
    defaultValueSelectMultiple,
    selectedDictType,
    updateComponent,
    loadCodeRuleOptions,
    updateFormGovernance,
    updateFormFieldLinkages,
    collectBoundFieldOptions,
    updateLabel,
    loadDefaultValueDictOptions,
    normalizeDefaultValueOptions,
    formatDefaultValueOptionLabel,
    updateDefaultValue,
    addLayoutChild,
    updateLayoutChild,
    moveLayoutChild,
    removeLayoutChild,
    addInteractionRule,
    addInteractionPreset,
    updateInteractionRule,
    updateInteractionRuleJson,
    removeInteractionRule,
    resolveTriggerLabel,
    resolveActionLabel,
    resolveActionValueMeta,
    openCrudFieldDrawer,
    updateEditingCrudFieldConfig,
    collectComponentTargetOptions,
    collectDrivenRuntimeRules,
    runtimeRuleDependsOnField,
    summarizeDrivenRuntimeRule,
    collectDictTypeFields,
    updateSelectedCodeDraft,
    resetSelectedCodeDraft,
    applySelectedCode,
    updateSchemaCodeDraft,
    resetSchemaCodeDraft,
    applySchemaCode,
    applySourceModalCode,
    cancelSourceModalEdit,
    replaceComponentInSchema,
    colorToHexInput,
    hexInputToColor,
    normalizeAppearanceColor,
    normalizeOpacityPercent,
    updateSelectedAppearanceBackground,
    updateSelectedAppearanceBorder,
    updateSelectedAppearanceOpacity,
    updateDesignerStyle,
    updateDesignerBorderStyle,
    updateDesignerBorderColor,
    updateDesignerBorderWidth,
    updateWidthMode,
    updateHeightMode,
    updateComponentStyleText,
    updateDesignerSpacing,
    updateDesignerBoxSpacing,
    updateDesignerCustomStyle,
    updateDesignerTranslate,
    updateFormStyle,
    updateFormAppearanceBackground,
    updateFormAppearanceBorder,
    updateFormBorderStyle,
    updateFormBorderWidth,
    updateFormAppearanceOpacity,
    updateFormStyleText,
    updateFormSpacing,
    updateFormBoxSpacing,
    updateFormTranslate,
    updateCrudApiBase,
    updateCrudApiConfig,
    resolveCrudApiMethodLabel,
    resolveCrudApiMethodClass,
    updateCrudOption,
    updateCrudExpandEnabled,
    updateCrudExpandConfig,
    updateFirstCrudExpandPanel,
    updateFirstCrudExpandDataSource,
    updateFirstCrudExpandParamsMap,
    updateFirstCrudExpandDescriptionFields,
    resolveCrudExpandDescriptionFieldKeys,
    resolveCrudExpandDescriptionSelectedFields,
    resolveCrudExpandDescriptionPanelFields,
    addFirstCrudExpandDescriptionField,
    removeFirstCrudExpandDescriptionField,
    toggleFirstCrudExpandDescriptionField,
    handleCrudExpandDescriptionPanelReorder,
    resolveCrudBitableFieldIconComponent,
    createDefaultCrudExpandPanel,
    normalizeCrudExpandPanelPatch,
    resolveDefaultCrudDescriptionFields,
    resolveCrudFieldLabel,
    parseJsonObjectProp,
    updateCrudFieldRole,
    updateCrudFieldConfig,
    updateCrudFieldConfigById,
    updateOption,
    updateOptionLabel,
    toggleOptionDisabled,
    updateOptionJsonProps,
    addOption,
    removeOption,
    stringifyJsonProp,
    resolveCrudSwitchValue,
    collectCrudConfigFields,
    resolveCrudFieldRoles,
    updateFormLayout,
    updateRowTotalColumns,
    updateRowCellCount,
    updateRowColumnSpan,
    createColumn,
    normalizeApiBase,
    buildDefaultCrudApiConfig,
    hasAncestorComponent,
    resolvePxNumber,
    normalizePositionNumber,
    parseTranslateStyle,
    formatTranslateStyle,
    parseStyleText,
    stringifyStyle,
    applyStyleValue,
    valueToPx,
    toCamelCase,
    toKebabCase,
    handleOpenSourcePanel,
  }
}
