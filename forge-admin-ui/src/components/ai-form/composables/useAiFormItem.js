import { CopyOutline } from '@vicons/ionicons5'
import { useClipboard } from '@vueuse/core'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { queryBusinessRecordSelector } from '@/api/business-app'
import { executeLowcodeQuerySource } from '@/api/lowcode-query-source'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DictSelect from '@/components/DictSelect.vue'
import FileUpload from '@/components/file-upload/index.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import { resolveRuntimeControl } from '@/components/lowcode-builder/shared/runtime-rules'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'
import { getDictData } from '@/composables/useDict'
import { request } from '@/utils'
import { resolveSwitchValuePair } from '@/views/app-center/components/designer/forge-form-designer/field-default-value'
import AiCustomSelect from '../AiCustomSelect.vue'
import AiFormArrayField from '../AiFormArrayField.vue'
import AiFormGroupTitle from '../AiFormGroupTitle.vue'
import AiFormSectionTitle from '../AiFormSectionTitle.vue'
import AiRecordSelectorModal from '../AiRecordSelectorModal.vue'
import { resolveControlProps } from '../control-props'
import {
  coerceNumberFieldValue,
  isInputLikeFieldType,
  isNumberLikeField,
  resolveNumberFieldType,
} from '../field-type-utils'
import {
  buildQuerySourceDisplayFields,
  buildTreeFromFlatRows,
  collectFieldMappingSourceFields,
  decorateLazyTreeNodes,
  resolveFieldCascadeConfig,
  resolveFirstFilledOptionField,
  resolveOptionLoadMode,
  resolveOptionPageSize,
  resolvePendingOptionLabel,
  rowsHaveNestedChildren,
  shouldBuildTreeOptions,
} from '../option-source-runtime'
import { applyRecordFieldMappings, extractSelectorRawRecord, normalizeRecordSelectorConfig, normalizeSelectorMappings, resolveSelectorSearchParams } from '../record-selector-utils'
import { resolveSelectionLabelFields as buildSelectionLabelFields, readDataFieldValue, ORG_SELECT_FIELD_TYPES, USER_SELECT_FIELD_TYPES } from '../selection-label-fields'
import { isFieldMultiple, parseSelectionValues, serializeSelectionLabels, serializeSelectionValues } from '../selection-multi-value'
import { expandSearchTreeSelectValue } from '@/components/lowcode-builder/shared/runtime-tree-table'
import {
  resolveSwitchCheckedValue,
  resolveSwitchUncheckedValue,
  getPlaceholder,
  resolveFieldAffix,
  resolveNumberFieldPrefix,
  resolveNumberFieldSuffix,
  resolveDesignerInlineStyle,
  cacheAsyncOptions,
  hasEffectiveOptionSource,
  buildCurrentChildrenOptions,
  hasPersistedOptionId,
  safeParseObject,
  applyTreeLoadParams,
  isSelectorQueryApi,
  parseOptionApi,
  finalizeOptionTree,
  extractOptionRows,
  normalizeOptionNode,
  resolveCascadeConfig,
  normalizeAlign,
  getNestedValue,
  flattenOptionNodes,
  isSameOptionValue,
  normalizeRuntimeFieldType,
  isOrgTreeSelectField,
  isUserSelectField,
  isObjectReferenceField,
  isRecordSelectorField,
  firstNonBlank,
  resolveSelectionLabelFields,
  normalizeLabelValue,
  normalizeDisplayText,
  resolveUserLabel,
  isFilledValue,
  resolveScanErrorMessage,
  firstDisplayFieldName,
  resolveFormattedRangeValue,
  normalizeTimestampPickerValue,
  normalizeFormattedPickerValue,
  isSupportedPickerText,
  normalizeTimestampRangePickerValue,
  normalizeFormattedRangePickerValue,
} from '../aiFormItemUtils'

export function useAiFormItem(props, emit) {

  const route = useRoute()
  const { copy } = useClipboard()
  const remoteOptions = ref([])
  const remoteLoading = ref(false)
  const dictOptions = ref([])
  const sourceDictOptions = ref([])
  const recordSelectorVisible = ref(false)
  const scanLoading = ref(false)
  const scanFeedback = ref({ status: 'idle', message: '' })
  const pickerDefaultTimestamp = Date.now()
  let remoteRequestSeq = 0

  // 人员/组织选择器类型集合与选择器 label 适配共享（见 selection-label-fields.js，勿在此另建副本）
  const ORG_TREE_SELECT_TYPES = ORG_SELECT_FIELD_TYPES
  const USER_SELECT_TYPES = USER_SELECT_FIELD_TYPES

  const controlProps = computed(() => resolveControlProps(props.field?.props))
  const numberFieldValue = computed(() => (
    isNumberLikeField(props.field) ? coerceNumberFieldValue(props.value) : props.value
  ))
  const numberFieldPrefix = computed(() => resolveNumberFieldPrefix(props.field, resolveNumberFieldType))
  const numberFieldSuffix = computed(() => resolveNumberFieldSuffix(props.field))
  const inputFieldPrefix = computed(() => resolveFieldAffix(props.field, 'prefix'))
  const inputFieldSuffix = computed(() => resolveFieldAffix(props.field, 'suffix'))
  const switchControlProps = computed(() => {
    const next = { ...controlProps.value }
    delete next.checkedValue
    delete next.uncheckedValue
    delete next.checkedText
    delete next.uncheckedText
    delete next.defaultValue
    return next
  })
  const fieldRuntimeControl = computed(() => resolveRuntimeControl(props.field || {}, {
    ...(props.context || {}),
    record: props.formData || {},
    row: props.context?.currentRow || props.context?.row || props.formData || {},
    formData: props.formData || {},
    data: props.formData || {},
    route: {
      query: route.query || {},
      params: route.params || {},
      path: route.path,
      fullPath: route.fullPath,
      name: route.name,
    },
  }))
  const fieldRuntimeVisible = computed(() => fieldRuntimeControl.value.visible !== false)

  const formItemRef = ref(null)
  const arrayFieldRef = ref(null)

  /**
   * 将字段级固定 labelWidth 显式写回 label 元素的 inline style。
   *
   * naive-ui FormItem 挂载时的 invalidateLabelWidth 为测量 label 自然宽度，会直接清空
   * label 元素的 inline width 且事后只恢复 whiteSpace；当字段配置了固定 labelWidth 且表单级
   * labelWidth 为 'auto' 时，mergedLabelWidth 恒为该固定值，Vue 后续 patch 因新旧值相同而
   * 跳过 width 写入，DOM 宽度就此丢失，label 塌缩为内容宽度导致整列不对齐。
   * 这里在挂载与 labelWidth 变化后写回，兜底该原生缺陷。
   */
  function restoreFixedLabelWidth() {
    const labelWidth = props.field?.labelWidth
    if (labelWidth === undefined || labelWidth === null || labelWidth === '' || labelWidth === 'auto')
      return
    const labelEl = formItemRef.value?.$el?.querySelector?.(':scope > .n-form-item-label')
    if (labelEl)
      labelEl.style.width = typeof labelWidth === 'number' ? `${labelWidth}px` : String(labelWidth)
  }

  onMounted(async () => {
    await nextTick()
    restoreFixedLabelWidth()
    registerArrayValidator()
  })

  onBeforeUnmount(() => {
    props.context?.unregisterFieldValidator?.(props.field?.field)
  })

  watch(() => props.field?.labelWidth, () => {
    nextTick(restoreFixedLabelWidth)
  })

  watch(() => props.field?.type, () => nextTick(registerArrayValidator))

  function registerArrayValidator() {
    const field = props.field?.field
    if (!field)
      return
    if (props.field?.type !== 'array') {
      props.context?.unregisterFieldValidator?.(field)
      return
    }
    props.context?.registerFieldValidator?.(field, () => arrayFieldRef.value?.validate?.())
  }

  /**
   * 获取占位符文本
   */

  /** 详情态 / 表单只读：整表不可编辑（与 ChildTableEditor :readonly 对齐） */
  function isFormDetailReadonly() {
    const ctx = props.context || {}
    return ctx.formReadonly === true
      || ctx.readonly === true
      || ctx.isDetail === true
      || ctx.modalStatus === 'detail'
  }

  /**
   * 处理禁用状态
   */
  function disabledHandler(field) {
    if (isFormDetailReadonly())
      return true
    if (isCascadeDisabledByEmptyParent())
      return true
    if (fieldRuntimeControl.value.readonly || fieldRuntimeControl.value.disabled)
      return true
    if (typeof field.disabled === 'boolean') {
      return field.disabled
    }
    if (typeof field.disabled === 'function') {
      return field.disabled({
        formData: props.formData,
        field,
        context: props.context,
      })
    }
    return false
  }

  const fieldAlign = computed(() => normalizeAlign(props.field?.align || props.field?.textAlign || props.field?.props?.align))
  const fieldAlignClass = computed(() => fieldAlign.value === 'left' ? '' : `ai-form-item-align-${fieldAlign.value}`)
  const formItemClass = computed(() => [
    `ai-form-item--${props.field?.type || 'input'}`,
    (isFormDetailReadonly() || props.field?.readonly || props.field?.props?.readonly) ? 'ai-form-item--readonly' : '',
    fieldAlignClass.value,
    props.field?.field ? `ai-form-item-field-${props.field.field}` : '',
    props.field?.formItemClass,
  ].filter(Boolean))
  const designerInlineStyle = computed(() => resolveDesignerInlineStyle(props.field || {}))
  const resolvedFormItemStyle = computed(() => {
    const merged = {
      ...designerInlineStyle.value,
      ...(props.field?.formItemStyle || {}),
      ...(props.field?.style || {}),
    }
    return Object.keys(merged).length ? merged : undefined
  })
  const componentControlStyle = computed(() => {
    const runtimeStyle = fieldRuntimeControl.value.style || {}
    const merged = {
      ...designerInlineStyle.value,
      ...(props.field?.componentStyle || props.field?.style || {}),
      ...runtimeStyle,
    }
    return Object.keys(merged).length ? merged : undefined
  })
  const componentControlClass = computed(() => [
    `ai-form-control--${props.field?.type || 'input'}`,
    fieldRuntimeControl.value.className,
    props.field?.componentClass,
  ].filter(Boolean))
  const recordSelectorConfig = computed(() => normalizeRecordSelectorConfig(props.field))
  const fieldMultiple = computed(() => isFieldMultiple(props.field) || recordSelectorConfig.value.multiple === true)
  const recordSelectorRuntimeContext = computed(() => ({
    ...(props.context || {}),
    formData: props.formData || {},
    form: props.formData || {},
    record: props.context?.record || props.formData || {},
    row: props.context?.currentRow || props.context?.row || props.formData || {},
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
  /** 引用字段显示名称冗余键（<field>Name）：选中时随主列一起提交，回显零关联查询。 */
  const relationLabelValueField = computed(() => {
    const field = props.field || {}
    return firstNonBlank(field.props?.labelValueField, field.labelValueField, field.field ? `${field.field}Name` : '')
  })
  const recordSelectorDisplayText = computed(() => {
    const config = recordSelectorConfig.value
    const labelField = props.field?.labelField || props.field?.props?.labelField || props.field?.props?.targetLabelField || config.targetLabelField || config.labelTargetField
    // 伴随列冗余值（<field>Name）来自详情接口，优先展示；其次用户配置的回显目标字段
    if (relationLabelValueField.value && props.formData?.[relationLabelValueField.value])
      return props.formData[relationLabelValueField.value]
    if (labelField && props.formData?.[labelField])
      return props.formData[labelField]
    return normalizeDisplayText(props.value)
  })
  const fieldDescription = computed(() => props.field?.props?.description || props.field?.description || '')
  const fieldLabelTip = computed(() => props.field?.props?.labelTip || props.field?.labelTip || '')
  const fieldBadge = computed(() => props.field?.props?.badge || props.field?.badge || '')
  const manualFieldEventEnabled = computed(() => props.context?.hasFieldEvent?.('MANUAL', props.field?.field) === true)
  const blurFieldEventEnabled = computed(() => props.context?.hasFieldEvent?.('BLUR', props.field?.field) === true
    || props.context?.hasComponentInteractionEvent?.('blur', props.field?.field) === true)
  const scanFieldEventEnabled = computed(() => props.context?.hasFieldEvent?.('SCAN_COMPLETE', props.field?.field) === true)
  const fieldEventState = computed(() => props.context?.getFieldEventState?.(props.field?.field) || {
    status: 'idle',
    loading: false,
    message: '',
  })
  const showFieldEventFeedback = computed(() => manualFieldEventEnabled.value
    || scanFieldEventEnabled.value
    || scanFeedback.value.message
    || fieldEventState.value.loading
    || Boolean(fieldEventState.value.message))
  const isSectionTitleField = computed(() => {
    return !isLegacyGroupTitleField(props.field) && ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
      .includes(props.field?.type || props.field?.componentKey || props.field?.nodeType)
  })
  const isGroupTitleField = computed(() => {
    return isLegacyGroupTitleField(props.field) || ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
      .includes(props.field?.type || props.field?.componentKey || props.field?.nodeType)
  })
  const isFieldRequired = computed(() => {
    if (Object.prototype.hasOwnProperty.call(fieldRuntimeControl.value, 'required') && fieldRuntimeControl.value.required !== undefined)
      return fieldRuntimeControl.value.required === true
    if (props.field?.required === true)
      return true
    const rules = props.field?.rules
    if (!rules)
      return false
    return (Array.isArray(rules) ? rules : [rules]).some(rule => rule?.required === true)
  })
  const fieldDictType = computed(() => props.field?.dictType || props.field?.props?.dictType || '')
  const cascadeConfig = computed(() => resolveCascadeConfig(props.field))
  const dictCascadeConfig = computed(() => {
    if (!cascadeConfig.value)
      return null
    return {
      ...cascadeConfig.value,
      sourceOptions: sourceDictOptions.value,
    }
  })
  const remoteOptionSource = computed(() => {
    if (shouldSuppressDesignerRemoteOptions(props.field))
      return null
    return resolveDynamicOptionSource(props.field)
  })

  /**
   * 远程选项源的内容签名：params 已含 ${field} 引用解析后的值与级联参数值，
   * 值不变则签名不变。remoteOptionSource 每次重算都返回新对象（引用比较恒不等），
   * 若直接 watch 对象本身，任意字段值变化都会让所有远程下拉重新请求、loading 闪烁。
   */
  const remoteOptionSourceKey = computed(() => {
    const source = remoteOptionSource.value
    if (!source)
      return ''
    return JSON.stringify([
      source.type,
      source.api,
      source.dictType,
      source.sourceType,
      source.sourceKey,
      source.valueField,
      source.labelField,
      source.parentField,
      source.childrenField,
      source.loadMode,
      source.rootParentValue,
      source.pageNum,
      source.pageSize,
      source.waitForParent === true,
      Array.isArray(source.waitForFields) ? source.waitForFields.join(',') : '',
      source.params || {},
    ])
  })

  const treeSelectLazyLoadHandler = computed(() => {
    const source = remoteOptionSource.value
    if (!source || resolveOptionLoadMode(source) !== 'lazy')
      return undefined
    return handleTreeSelectLazyLoad
  })

  const runtimePageWidgetKey = computed(() => {
    const field = props.field || {}
    const componentKey = String(field.componentKey || '').trim()
    if (componentKey && isPageWidgetComponentKey(componentKey))
      return componentKey
    const type = String(field.type || '').trim()
    return isPageWidgetComponentKey(type) ? type : ''
  })
  const isRuntimePageWidgetField = computed(() => {
    if (!runtimePageWidgetKey.value)
      return false
    const bindingMode = props.field?.fieldBinding?.mode
    // 显式 virtual 绑定 或 未配置 fieldBinding 的挂件组件（预览模式兜底）
    if (bindingMode === 'virtual' || props.field?.virtual === true || props.field?.isVirtual === true)
      return true
    return !props.field?.fieldBinding && !props.field?.field
  })
  const runtimePageWidgetProps = computed(() => {
    const field = props.field || {}
    const widgetKey = runtimePageWidgetKey.value
    const fieldProps = field.props && typeof field.props === 'object' ? field.props : {}
    const next = {
      ...fieldProps,
    }
    if (field.label && !next.title && ['rich-text', 'markdown', 'barcode', 'qrcode', 'transfer', 'audio-player', 'video-player', 'iframe'].includes(widgetKey))
      next.title = field.label
    if (isFilledValue(props.value)) {
      if (widgetKey === 'rich-text' || widgetKey === 'markdown')
        next.content = props.value
      else if (widgetKey === 'transfer')
        next.value = Array.isArray(props.value) ? props.value : String(props.value).split(',').map(item => item.trim()).filter(Boolean)
      else if (widgetKey === 'barcode' || widgetKey === 'qrcode')
        next.value = props.value
      else if (widgetKey === 'audio-player' || widgetKey === 'video-player' || widgetKey === 'iframe' || widgetKey === 'avatar')
        next.src = props.value
    }
    return next
  })

  function isLegacyGroupTitleField(field = {}) {
    const fieldProps = field?.props || {}
    return field?.nodeType === 'divider'
      && !field?.componentKey
      && Object.prototype.hasOwnProperty.call(fieldProps, 'description')
      && !Object.prototype.hasOwnProperty.call(fieldProps, 'title')
  }
  const cascadeSourceValue = computed(() => {
    const cascade = cascadeConfig.value
    return cascade?.enabled && cascade.sourceField ? props.formData?.[cascade.sourceField] : undefined
  })
  const sourceFieldConfig = computed(() => findSchemaField(cascadeConfig.value?.sourceField))
  const sourceDictType = computed(() => cascadeConfig.value?.sourceDictType || sourceFieldConfig.value?.dictType || sourceFieldConfig.value?.props?.dictType || '')

  // 组织→人员级联：源组织字段的值作为人员弹窗的过滤范围，组织重选时人员组件同步刷新
  const userSelectCascadeOrgId = computed(() => {
    const cascade = cascadeConfig.value
    if (!cascade?.enabled || !cascade.sourceField || !isUserSelectField(props.field))
      return null
    const orgId = props.formData?.[cascade.sourceField]
    return orgId === null || orgId === undefined || orgId === '' ? null : orgId
  })
  const userSelectCascadeIncludeChildren = computed(() => cascadeConfig.value?.includeChildren !== false)

  function isCascadeDisabledByEmptyParent() {
    const cascade = cascadeConfig.value
    if (!cascade?.enabled || cascade.emptyStrategy !== 'disabled' || !cascade.sourceField)
      return false
    const sourceValue = props.formData?.[cascade.sourceField]
    return sourceValue === null || sourceValue === undefined || sourceValue === ''
  }

  watch(
    remoteOptionSourceKey,
    () => {
      const source = remoteOptionSource.value
      if (!source) {
        remoteOptions.value = []
        return
      }
      // 子表预加载选项已到位时先灌进 remoteOptions，避免首屏空下拉
      const preloaded = Array.isArray(props.field?.options) && props.field.options.length
        ? props.field.options
        : (Array.isArray(props.field?.props?.options) && props.field.props.options.length
          ? props.field.props.options
          : null)
      if (preloaded && !remoteOptions.value.length) {
        remoteOptions.value = preloaded
        console.info('[forge-child-select] AiFormItem seed remoteOptions from preloaded', {
          field: props.field?.field,
          count: preloaded.length,
        })
      }
      loadRemoteOptions(source)
    },
    { immediate: true },
  )

  // 预加载异步完成后 field.props.options 才到，补种一次
  watch(
    () => [
      Array.isArray(props.field?.props?.options) ? props.field.props.options.length : 0,
      Array.isArray(props.field?.options) ? props.field.options.length : 0,
    ],
    () => {
      if (remoteOptions.value.length)
        return
      const preloaded = Array.isArray(props.field?.options) && props.field.options.length
        ? props.field.options
        : (Array.isArray(props.field?.props?.options) && props.field.props.options.length
          ? props.field.props.options
          : null)
      if (!preloaded)
        return
      remoteOptions.value = preloaded
      console.info('[forge-child-select] AiFormItem late-seed remoteOptions', {
        field: props.field?.field,
        count: preloaded.length,
      })
    },
  )

  // 远程选项就绪后回写伴随标签，下次进入可直接用 label 回显，避免先闪 id
  watch(
    [remoteOptions, () => props.value, remoteLoading],
    () => {
      if (remoteLoading.value || !remoteOptionSource.value)
        return
      if (!shouldSyncOptionLabels(props.field) || !isFilledValue(props.value))
        return
      if (!Array.isArray(remoteOptions.value) || !remoteOptions.value.length)
        return
      syncSelectionLabelFromOptions(props.field, props.value)
    },
  )

  watch(fieldDictType, loadDictOptions, { immediate: true })
  watch(sourceDictType, loadSourceDictOptions, { immediate: true })
  watch(cascadeSourceValue, (value, oldValue) => {
    if (oldValue === undefined || value === oldValue || !cascadeConfig.value?.clearOnParentChange)
      return
    clearCurrentValue()
  })

  /**
   * 获取选项数据 - 使用 computed 确保响应式
   */
  const currentOptions = computed(() => {
    const field = props.field

    // 优先使用 options 函数
    if (typeof field.options === 'function') {
      const result = field.options({
        formData: props.formData,
        field,
        context: props.context,
      })

      // 如果返回的是 Promise，需要在外部处理
      // 这里我们检查是否有缓存的选项
      if (result instanceof Promise) {
        // 如果有缓存的选项，使用缓存
        if (field._cachedOptions && Array.isArray(field._cachedOptions)) {
          return withCurrentValueOption(resolveCascadedOptions(field._cachedOptions))
        }
        // 否则返回空数组，并异步加载
        cacheAsyncOptions(field, result)
        return []
      }

      return withCurrentValueOption(resolveCascadedOptions(result))
    }

    // 动态选项源（业务对象/数据集/远程接口/当前子表）优先于设计器残留的静态 options
    // 否则切到「其它表/业务对象」后仍显示「选项1/选项2」
    const currentChildrenSource = resolveCurrentChildrenSource(field)
    if (currentChildrenSource) {
      return withCurrentValueOption(resolveCascadedOptions(
        finalizeOptionTree(
          buildCurrentChildrenOptions(currentChildrenSource, props.context),
          currentChildrenSource,
          field.type || field.componentKey,
        ),
      ))
    }

    if (fieldDictType.value) {
      const options = resolveCascadedOptions(dictOptions.value)
      if (field.type === 'cascader')
        return buildDictTreeOptions(options)
      return withCurrentValueOption(options)
    }

    if (remoteOptionSource.value) {
      const remote = withCurrentValueOption(resolveCascadedOptions(remoteOptions.value))
      if (remote.length)
        return remote
      // 子表预加载会先把 options 写进 field.props；远程还在飞或失败时优先用已有选项，避免一直「无数据」
      const staticFromField = Array.isArray(field.options) && field.options.length
        ? field.options
        : (Array.isArray(field.props?.options) && field.props.options.length ? field.props.options : null)
      if (staticFromField) {
        console.info('[forge-child-select] AiFormItem use preloaded/static options', {
          field: field.field,
          count: staticFromField.length,
          remoteLoading: remoteLoading.value,
        })
        return withCurrentValueOption(resolveCascadedOptions(staticFromField))
      }
      if (remoteLoading.value)
        return remote
    }

    // 无动态源时才使用静态 options 数组
    if (field.options && Array.isArray(field.options) && field.options.length > 0) {
      return withCurrentValueOption(resolveCascadedOptions(field.options))
    }

    // 检查 props.options（兼容旧的配置方式）
    if (field.props?.options && Array.isArray(field.props.options) && field.props.options.length > 0) {
      return withCurrentValueOption(resolveCascadedOptions(field.props.options))
    }

    // 最后处理 enumType (仅当 options 为空时)
    if (field.enumType) {
      // 这里应该根据 enumType 获取对应的枚举数据
      // 由于这是一个示例,我们返回一个空数组
      // 在实际项目中,这里应该从父组件传递的 context 中获取数据
      // 或者通过 props 传递具体的选项数据
      console.warn(`字段 ${field.field} 使用了 enumType: ${field.enumType},但未提供具体选项数据`)
      return []
    }

    return []
  })

  function withCurrentValueOption(options = []) {
    const result = Array.isArray(options) ? [...options] : []
    const field = props.field || {}
    if (props.value === null || props.value === undefined || props.value === '')
      return result
    const values = Array.isArray(props.value)
      ? props.value
      : fieldMultiple.value && typeof props.value === 'string'
        ? parseSelectionValues(props.value, true)
        : [props.value]
    const companionLabel = resolveSelectionLabelValue(field)
    const companionLabels = Array.isArray(companionLabel)
      ? companionLabel
      : String(companionLabel || '').split(',').map(item => item.trim()).filter(Boolean)
    // 远程选项未就绪时必须注入占位项，否则 n-select 会把 value（id）直接当标签显示造成闪烁
    const remotePending = Boolean(remoteOptionSource.value) && remoteLoading.value
    values.forEach((value, index) => {
      const companion = companionLabels[index] || companionLabels[0] || ''
      const existing = flattenOptionNodes(result).find(option => isSameOptionValue(option?.value ?? option?.key, value))
      if (existing) {
        // 选项已存在但 label 仍是 id/value（或空）时，优先用伴随名称回显
        if (companion && (!existing.label || String(existing.label) === String(value)))
          existing.label = companion
        return
      }
      const label = resolvePendingOptionLabel({
        companionLabel: companion,
        remotePending,
      })
      if (!label)
        return
      result.unshift({
        value,
        key: value,
        label,
        // 回显临时节点：禁止懒加载再去拉下级，避免无意义请求
        isLeaf: true,
      })
    })
    return result
  }

  const selectOptionLoading = computed(() => Boolean(props.field?.loading || (remoteOptionSource.value && remoteLoading.value)))

  async function loadDictOptions(dictType) {
    if (!dictType) {
      dictOptions.value = []
      return
    }
    dictOptions.value = await getDictData(dictType)
  }

  async function loadSourceDictOptions(dictType) {
    if (!dictType) {
      sourceDictOptions.value = []
      return
    }
    sourceDictOptions.value = await getDictData(dictType)
  }

  function resolveOptionSource(field = {}) {
    if (isUserSelectField(field))
      return null
    if (shouldSuppressDesignerRemoteOptions(field))
      return null
    const configuredSource = field.optionSource || field.props?.optionSource
    if (hasEffectiveOptionSource(configuredSource)) {
      const source = normalizeOptionSource(configuredSource)
      if (shouldSuppressDesignerRemoteOptions(field, source))
        return null
      return source
    }
    if (isObjectReferenceField(field)) {
      const generated = buildObjectReferenceOptionSource(field)
      if (generated)
        return generated
    }
    if (isOrgTreeSelectField(field)) {
      return {
        type: 'tree',
        api: 'get@/system/org/tree',
        valueField: 'id',
        keyField: 'id',
        labelField: 'orgName',
        fallbackLabelFields: ['name'],
        childrenField: 'children',
      }
    }
    return null
  }

  function shouldSuppressDesignerRemoteOptions(field = {}, source = null) {
    if (!isDesignerPreviewContext())
      return false
    if (isObjectReferenceField(field) || isRecordSelectorField(field))
      return true
    if (source?.type === 'businessRecordSelector')
      return true
    return String(source?.api || field?.optionSource?.api || field?.props?.optionSource?.api || '').includes('selector/query')
  }

  function isDesignerPreviewContext() {
    const context = props.context || {}
    const mode = String(context.mode || context.source || context.scene || '').trim()
    return context.designerPreview === true
      || context.designMode === true
      || ['designer', 'designer-preview', 'form-designer', 'design', 'canvas'].includes(mode)
  }

  function resolveCurrentChildrenSource(field = {}) {
    const source = field.optionSource || field.props?.optionSource
    if (!source || typeof source !== 'object')
      return null
    return ['CURRENT_CHILDREN', 'current_children', 'currentChildren'].includes(String(source.type || ''))
      ? source
      : null
  }

  function normalizeOptionSource(source) {
    if (typeof source === 'string')
      return { api: source }
    const next = { ...(source || {}) }
    if (!next.api && next.url)
      next.api = next.url
    // params 可能是 JSON 字符串（历史快照 / 误序列化）；必须先收成对象，
    // 否则 Object.entries("{}") 会变成 {0:'{',1:'}'}，下拉请求异常或被空参逻辑误伤。
    if (typeof next.params === 'string')
      next.params = safeParseObject(next.params)
    if ((!next.params || typeof next.params !== 'object' || Array.isArray(next.params))
      && typeof next.paramsText === 'string') {
      next.params = safeParseObject(next.paramsText)
    }
    if (!next.params || typeof next.params !== 'object' || Array.isArray(next.params))
      next.params = {}
    // 业务对象选项源统一走 QUERY_SOURCE 执行入口
    if (!next.type && String(next.sourceType || '').toUpperCase() === 'BUSINESS_OBJECT' && next.sourceKey)
      next.type = 'QUERY_SOURCE'
    // 树形组件动态源：未显式配置时补默认父级/加载策略，保证扁平表能拼树
    const componentType = props.field?.type || props.field?.componentKey || ''
    if (shouldBuildTreeOptions(next, componentType) && !String(next.parentField || '').trim()
      && String(next.type || '').toLowerCase() !== 'tree') {
      if (!next.parentField)
        next.parentField = 'parentId'
      if (!next.childrenField)
        next.childrenField = 'children'
      if (!next.loadMode)
        next.loadMode = 'full'
      if (!next.structure)
        next.structure = 'tree'
    }
    return next
  }

  function resolveDynamicOptionSource(field = {}) {
    const source = resolveOptionSource(field)
    if (!source)
      return null
    const resolvedParams = resolveDynamicParams(source.params || {})
    // 记录哪些参数来自 ${field} 引用且当前为空，QUERY_SOURCE 需要等这些字段有值后再发起请求
    const waitForFields = []
    if (source.type === 'QUERY_SOURCE') {
      for (const [key, rawValue] of Object.entries(source.params || {})) {
        if (typeof rawValue !== 'string')
          continue
        const matched = rawValue.match(/^\$\{(.+)\}$/) || rawValue.match(/^\$form\.(.+)$/)
        if (matched) {
          const resolved = resolvedParams[key]
          if (resolved === undefined || resolved === null || resolved === '')
            waitForFields.push(matched[1])
        }
      }
    }
    const next = {
      ...source,
      params: resolvedParams,
    }
    if (waitForFields.length)
      next.waitForFields = waitForFields
    ensureSelectorQueryObjectCode(next, field)
    const cascade = cascadeConfig.value
    if (cascade?.enabled && cascade.mode === 'remoteParam' && cascade.sourceField && cascade.paramName) {
      const sourceValue = props.formData?.[cascade.sourceField]
      if ((sourceValue === null || sourceValue === undefined || sourceValue === '') && cascade.emptyStrategy !== 'all') {
        next.waitForParent = true
      }
      next.params = {
        ...next.params,
        [cascade.paramName]: sourceValue,
      }
    }
    return next
  }

  function ensureSelectorQueryObjectCode(source = {}, field = {}) {
    if (!String(source.api || '').includes('selector/query'))
      return source
    const objectCode = isObjectReferenceField(field)
      ? resolveObjectReferenceConfig(field).objectCode
      : normalizeRecordSelectorConfig({
        ...field,
        ...source,
        params: source.params,
      }).objectCode
    if (!objectCode)
      return source
    source.params = {
      ...(source.params || {}),
      objectCode,
      businessObjectCode: source.params?.businessObjectCode || objectCode,
      targetObjectCode: source.params?.targetObjectCode || objectCode,
    }
    return source
  }

  function resolveDynamicParams(params = {}) {
    const result = {}
    Object.entries(params || {}).forEach(([key, value]) => {
      if (typeof value === 'string') {
        const matched = value.match(/^\$\{(.+)\}$/) || value.match(/^\$form\.(.+)$/)
        result[key] = matched ? props.formData?.[matched[1]] : value
        return
      }
      result[key] = value
    })
    return result
  }

  async function loadRemoteOptions(source, keyword = '', { parentValue, forChildren = false } = {}) {
    if (!source)
      return forChildren ? [] : undefined
    if (source.waitForParent) {
      if (!forChildren)
        remoteOptions.value = []
      return forChildren ? [] : undefined
    }
    if (source.type === 'businessRecordSelector') {
      const objectCode = normalizeRecordSelectorConfig(source).objectCode
      if (!objectCode) {
        if (!forChildren)
          remoteOptions.value = []
        return forChildren ? [] : undefined
      }
    }
    const requestSeq = forChildren ? remoteRequestSeq : ++remoteRequestSeq
    if (!forChildren)
      remoteLoading.value = true
    const componentType = props.field?.type || props.field?.componentKey || ''
    const isTree = shouldBuildTreeOptions(source, componentType)
    const loadMode = resolveOptionLoadMode(source)
    const pageSize = resolveOptionPageSize(source, { isTree })
    try {
      if (source.type === 'businessRecordSelector') {
        const objectCode = normalizeRecordSelectorConfig(source).objectCode
        const selectorPayload = {
          ...(source.params || {}),
          objectCode,
          businessObjectCode: source.businessObjectCode || source.params?.businessObjectCode || objectCode,
          targetObjectCode: source.targetObjectCode || source.params?.targetObjectCode || objectCode,
          keyword: keyword || undefined,
          keywordFields: source.keywordFields || source.params?.keywordFields || [],
          displayFields: source.displayFields || source.params?.displayFields || [],
          searchParams: source.searchParams || source.params?.searchParams || {},
        }
        applyTreeLoadParams(selectorPayload, source, { isTree, loadMode, parentValue })
        const res = await queryBusinessRecordSelector(selectorPayload, {
          pageNum: source.pageNum || 1,
          pageSize,
        })
        if (!forChildren && requestSeq !== remoteRequestSeq)
          return
        const normalized = normalizeRemoteOptions(res?.data || {}, source, componentType)
        if (forChildren)
          return normalized
        remoteOptions.value = normalized
        return
      }

      if (source.type === 'QUERY_SOURCE') {
        // 画布静态预览可跳过远程拉数；预览弹窗 / 运行页必须拉数，否则选中后回填没有源字段
        if (isDesignerPreviewContext() && props.context?.allowOptionSourceFetch !== true) {
          console.info('[forge-child-select] AiFormItem skip QUERY_SOURCE (designer preview)', {
            field: props.field?.field,
            mode: props.context?.mode,
            allowOptionSourceFetch: props.context?.allowOptionSourceFetch,
          })
          if (!forChildren)
            remoteOptions.value = []
          return forChildren ? [] : undefined
        }
        // 配置了参数但解析后全部为空时阻断请求，避免后端报必填参数错误；
        // 字段填值后 watch 自动重新触发。无参数或任一参数有值时正常发出。
        const rawParams = (source.params && typeof source.params === 'object' && !Array.isArray(source.params))
          ? source.params
          : {}
        const resolvedEntries = Object.entries(rawParams)
        const hasAnyParamValue = resolvedEntries.some(([, v]) => v !== undefined && v !== null && v !== '')
        if (resolvedEntries.length > 0 && !hasAnyParamValue && loadMode !== 'lazy') {
          console.info('[forge-child-select] AiFormItem block QUERY_SOURCE (empty params)', {
            field: props.field?.field,
            rawParams,
          })
          if (!forChildren)
            remoteOptions.value = []
          return forChildren ? [] : undefined
        }
        // 过滤空值参数：${字段名} 引用解析后为空时自动剥离
        const params = {}
        for (const [key, value] of Object.entries(rawParams)) {
          if (value !== undefined && value !== null && value !== '')
            params[key] = value
        }
        if (keyword && source.keywordParam)
          params[source.keywordParam] = keyword
        applyTreeLoadParams(params, source, { isTree, loadMode, parentValue })
        try {
          const displayFields = buildQuerySourceDisplayFields(
            source,
            collectFieldMappingSourceFields(resolveComponentFieldMappings(props.field)),
          )
          const payload = {
            sourceType: source.sourceType,
            sourceKey: source.sourceKey,
            params,
            fields: displayFields.length ? displayFields : undefined,
            pageNum: source.pageNum || 1,
            pageSize,
          }
          console.info('[forge-child-select] AiFormItem QUERY_SOURCE request', {
            field: props.field?.field,
            payload,
            staticOptions: Array.isArray(props.field?.props?.options) ? props.field.props.options.length : 0,
            contextAllowFetch: props.context?.allowOptionSourceFetch,
            contextMode: props.context?.mode,
          })
          const res = await executeLowcodeQuerySource(payload)
          if (!forChildren && requestSeq !== remoteRequestSeq)
            return
          const result = res?.data || {}
          const normalized = normalizeRemoteOptions(result, source, componentType)
          console.info('[forge-child-select] AiFormItem QUERY_SOURCE ok', {
            field: props.field?.field,
            count: normalized.length,
            sample: normalized.slice(0, 2),
          })
          if (forChildren)
            return normalized
          remoteOptions.value = normalized
        }
        catch (err) {
          if (!forChildren && requestSeq !== remoteRequestSeq)
            return
          console.warn(
            `[forge-child-select] AiFormItem QUERY_SOURCE FAIL [${source.sourceType}/${source.sourceKey}]`,
            {
              field: props.field?.field,
              params,
              message: err?.message || err,
              code: err?.code,
              detail: err?.detail || err?.error,
            },
          )
          if (forChildren)
            return []
          remoteOptions.value = []
        }
        return forChildren ? [] : undefined
      }

      if (!source.api)
        return forChildren ? [] : undefined
      if (isSelectorQueryApi(source.api)) {
        const selectorConfig = normalizeRecordSelectorConfig({
          ...props.field,
          ...source,
          ...(source.params || {}),
          params: source.params,
        })
        const objectCode = selectorConfig.objectCode
        if (!objectCode) {
          if (!forChildren)
            remoteOptions.value = []
          return forChildren ? [] : undefined
        }
        const selectorPayload = {
          ...(source.params || {}),
          objectCode,
          businessObjectCode: selectorConfig.businessObjectCode || objectCode,
          targetObjectCode: selectorConfig.targetObjectCode || objectCode,
          keyword: keyword || undefined,
          keywordFields: source.keywordFields || source.params?.keywordFields || [],
          displayFields: source.displayFields || source.params?.displayFields || [],
          searchParams: source.searchParams || source.params?.searchParams || {},
        }
        applyTreeLoadParams(selectorPayload, source, { isTree, loadMode, parentValue })
        const res = await queryBusinessRecordSelector(selectorPayload, {
          pageNum: source.pageNum || 1,
          pageSize,
        })
        if (!forChildren && requestSeq !== remoteRequestSeq)
          return
        const normalized = normalizeRemoteOptions(res?.data || {}, source, componentType)
        if (forChildren)
          return normalized
        remoteOptions.value = normalized
        return
      }
      const { method, url } = parseOptionApi(source.api)
      const params = {
        ...(source.params || {}),
      }
      if (keyword && source.keywordParam)
        params[source.keywordParam] = keyword
      applyTreeLoadParams(params, source, { isTree, loadMode, parentValue })
      // 远程分页接口常见 pageSize；树全量也带上作为服务端限流提示
      if (params.pageSize === undefined && params.pageNum === undefined)
        params.pageSize = pageSize
      const res = await request({
        method,
        url,
        params: method === 'get' ? params : undefined,
        data: method === 'get' ? undefined : params,
      })
      if (!forChildren && requestSeq !== remoteRequestSeq)
        return
      const normalized = normalizeRemoteOptions(res, source, componentType)
      if (forChildren)
        return normalized
      remoteOptions.value = normalized
    }
    catch (error) {
      console.warn(`[AiFormItem] 加载 ${props.field?.field || ''} 选项失败:`, {
        error,
        field: props.field,
        source,
        selectorConfig: normalizeRecordSelectorConfig({
          ...props.field,
          ...source,
          ...(source?.params || {}),
          params: source?.params,
        }),
        context: props.context,
      })
      if (forChildren)
        return []
      remoteOptions.value = []
    }
    finally {
      if (!forChildren && requestSeq === remoteRequestSeq)
        remoteLoading.value = false
    }
    return forChildren ? [] : undefined
  }

  async function handleTreeSelectLazyLoad(option) {
    const source = remoteOptionSource.value
    if (!source || !option || resolveOptionLoadMode(source) !== 'lazy')
      return
    const parentValue = option.value ?? option.key
    const children = await loadRemoteOptions(source, '', { parentValue, forChildren: true })
    option.children = Array.isArray(children) ? children : []
    if (!option.children.length)
      option.isLeaf = true
  }

  function normalizeRemoteOptions(data, source = {}, componentType = '') {
    const rows = extractOptionRows(data, source)
    if (!Array.isArray(rows))
      return []
    const isTree = shouldBuildTreeOptions(source, componentType || props.field?.type || props.field?.componentKey)
    const childrenField = source.childrenField || 'children'
    const nested = rowsHaveNestedChildren(rows, childrenField)
    // 已有嵌套 children，或声明为 tree 且未配 parentField：按嵌套递归；否则先扁平映射再拼树
    const includeChildren = nested || (isTree && !String(source.parentField || '').trim())
    const mapped = rows.map(row => normalizeOptionNode(row, source, includeChildren)).filter(Boolean)
    return finalizeOptionTree(mapped, source, componentType || props.field?.type || props.field?.componentKey)
  }

  function resolveCascadedOptions(options = []) {
    const cascade = cascadeConfig.value
    if (!cascade?.enabled || !cascade.sourceField)
      return options
    const sourceValue = props.formData?.[cascade.sourceField]
    if (sourceValue === null || sourceValue === undefined || sourceValue === '')
      return cascade.emptyStrategy === 'all' ? options : []
    if (cascade.mode === 'remoteParam')
      return options
    return (Array.isArray(options) ? options : []).filter(option => matchesCascade(option, sourceValue, cascade))
  }

  function matchesCascade(option, sourceValue, cascade) {
    const raw = option.raw || option
    if (cascade.mode === 'parentDictCode') {
      const parentDictCode = raw.parentDictCode ?? raw.parent_dict_code
      const sourceDictCode = resolveSourceDictCode(sourceValue)
      return isSameOptionValue(parentDictCode, sourceDictCode) || isSameOptionValue(parentDictCode, sourceValue)
    }
    if (cascade.mode === 'linkedDict') {
      const linkedType = raw.linkedDictType ?? raw.linked_dict_type
      const linkedValue = raw.linkedDictValue ?? raw.linked_dict_value
      const expectedType = cascade.linkedDictType || cascade.sourceDictType || sourceDictType.value
      const typeMatched = !expectedType || isSameOptionValue(linkedType, expectedType)
      return typeMatched && isSameOptionValue(linkedValue, sourceValue)
    }
    return true
  }

  function resolveSourceDictCode(sourceValue) {
    const matched = sourceDictOptions.value.find(option => isSameOptionValue(option.value, sourceValue))
    return matched?.dictCode ?? matched?.raw?.dictCode ?? sourceValue
  }

  function buildDictTreeOptions(options = []) {
    const nodes = (Array.isArray(options) ? options : []).map(option => ({
      ...option,
      key: option.dictCode ?? option.key ?? option.value,
      value: option.value,
      label: option.label,
      children: [],
    }))
    const byCode = new Map(nodes.map(node => [String(node.dictCode ?? node.key), node]))
    const roots = []
    nodes.forEach((node) => {
      const parentCode = node.parentDictCode ?? node.raw?.parentDictCode
      if (parentCode !== null && parentCode !== undefined && parentCode !== '' && Number(parentCode) !== 0 && byCode.has(String(parentCode))) {
        byCode.get(String(parentCode)).children.push(node)
      }
      else {
        roots.push(node)
      }
    })
    nodes.forEach((node) => {
      if (!node.children.length)
        delete node.children
    })
    return roots
  }

  function findSchemaField(fieldName) {
    if (!fieldName)
      return null
    const schemas = [
      ...(props.context?.schema || []),
      ...(props.context?.allSchema || []),
    ]
    return schemas.find(item => item?.field === fieldName) || null
  }

  function clearCurrentValue() {
    if (props.value === null || props.value === undefined || props.value === '')
      return
    emit('update:value', fieldMultiple.value ? '' : null)
    // 级联清空时同步清掉人员选中名称回显（<field>Name），避免组织重选后显示残留旧名称
    if (isUserSelectField(props.field))
      patchSelectionLabelValue(props.field, '')
  }

  function resolveOptionValue(rawValue) {
    return normalizeOptionValue(rawValue, currentOptions.value, fieldMultiple.value)
  }

  /**
   * n-slider range 模式要求 value 为二元数组；值为 null 时组件内部 (range ? value : [value]).map 会崩溃，
   * 归一化兜底：range 模式非法值回退 [min, max]
   */
  function resolveSliderValue(rawValue, field = {}) {
    const isRange = field.range === true || field.props?.range === true
    if (isRange) {
      if (Array.isArray(rawValue) && rawValue.length === 2)
        return rawValue
      return [Number(field.min ?? field.props?.min ?? 0), Number(field.max ?? field.props?.max ?? 100)]
    }
    return rawValue
  }

  function normalizeOptionValue(rawValue, options = [], multiple = false) {
    if (rawValue === null || rawValue === undefined || rawValue === '')
      return multiple ? [] : rawValue

    const values = multiple
      ? parseSelectionValues(rawValue, true)
      : rawValue

    if (!Array.isArray(options) || !options.length)
      return values

    if (Array.isArray(values))
      return values.map(item => findOptionValue(options, item)).filter(item => item !== undefined)

    return findOptionValue(options, values)
  }

  function findOptionValue(options = [], rawValue) {
    const match = flattenOptionNodes(options).find(option => isSameOptionValue(option?.value ?? option?.key, rawValue))
    if (match)
      return match.value ?? match.key
    return rawValue
  }

  /** 关联选择统一渲染：objectReference 默认下拉、recordSelector 默认弹窗，selectorMode 配置可覆盖。 */
  const isRelationSelectorField = computed(() => isObjectReferenceField(props.field) || isRecordSelectorField(props.field))

  const relationSelectorMode = computed(() => {
    const field = props.field || {}
    const mode = field.selectorMode || field.props?.selectorMode || field.basicProps?.selectorMode
    if (mode === 'dropdown' || mode === 'popup')
      return mode
    return isObjectReferenceField(field) ? 'dropdown' : 'popup'
  })

  function resolveObjectReferenceConfig(field = {}) {
    const props = field.props || {}
    const objectCode = firstNonBlank(
      field.referenceObjectCode,
      props.referenceObjectCode,
      field.basicProps?.referenceObjectCode,
      field.referenceConfig?.referenceObjectCode,
      props.referenceConfig?.referenceObjectCode,
      field.basicProps?.referenceConfig?.referenceObjectCode,
      normalizeRecordSelectorConfig(field).objectCode,
    )
    return {
      objectCode,
      valueField: firstNonBlank(
        props.referenceValueField,
        props.valueField,
        props.targetValueField,
        field.referenceValueField,
        field.valueField,
        field.targetValueField,
        'id',
      ),
      labelField: firstNonBlank(
        props.referenceDisplayField,
        props.displayField,
        props.labelField,
        props.targetLabelField,
        field.referenceDisplayField,
        field.displayField,
        field.labelField,
        field.targetLabelField,
      ),
    }
  }

  function buildObjectReferenceOptionSource(field = {}) {
    const config = resolveObjectReferenceConfig(field)
    if (!config.objectCode || !config.labelField)
      return null
    const selectorConfig = normalizeRecordSelectorConfig(field)
    const mappingSources = collectFieldMappingSourceFields(selectorConfig.fieldMappings)
    return {
      type: 'businessRecordSelector',
      objectCode: config.objectCode,
      valueField: config.valueField,
      labelField: config.labelField,
      recordsField: 'records',
      pageNum: 1,
      pageSize: 100,
      displayFields: [...new Set([`${config.labelField}:${config.labelField}`, ...mappingSources])],
      keywordFields: [config.labelField],
    }
  }

  // objectReference remote search support
  const objectReferenceRemoteEnabled = computed(() => {
    return !isDesignerPreviewContext()
      && isRelationSelectorField.value
      && relationSelectorMode.value === 'dropdown'
      && Boolean(resolveObjectReferenceConfig(props.field).objectCode)
  })
  const objectReferenceSearchKeyword = ref('')
  let objectReferenceSearchTimer = null

  function handleObjectReferenceSearch(keyword) {
    objectReferenceSearchKeyword.value = keyword || ''
    if (objectReferenceSearchTimer)
      clearTimeout(objectReferenceSearchTimer)
    objectReferenceSearchTimer = setTimeout(() => {
      reloadObjectReferenceOptions(keyword)
    }, 300)
  }

  function handleObjectReferenceUpdate(value) {
    handleUpdate(value)
    syncSelectionLabelFromOptions(props.field, value)
    applyOptionFieldMappings(value)
  }

  /** 普通下拉（业务对象/数据集选项）选中后同样执行 fieldMappings 回填 */
  function handleSelectUpdate(value) {
    const field = props.field || {}
    const fieldKey = field.field
    const multiple = fieldMultiple.value
    const normalizedValue = multiple ? serializeSelectionValues(value, true) : value
    const mappingPatch = buildOptionFieldMappingPatch(value)
    const labelPatch = buildSelectionLabelPatch(field, value)

    // 下拉值 + 伴随名称 + 选中回填同一批写入，避免受控表单二次 emit 冲掉映射结果
    if (fieldKey && typeof props.context?.patchFormData === 'function') {
      props.context.patchFormData({
        [fieldKey]: normalizedValue,
        ...labelPatch,
        ...mappingPatch,
      })
      // 仍 emit，保证 CHANGE 字段事件触发；handleFieldChange 会保留 patch 里已写入的其它键
      emit('update:value', normalizedValue)
      return
    }
    handleUpdate(value)
    applyOptionFieldMappings(value)
  }

  /**
   * 下拉选中后，按组件 fieldMappings 把所选记录字段写回表单。
   * 与「字段自动查询」是两套能力：映射来自当前选项行（零请求）；自动查询会再发一次查询源请求。
   * 同一批目标字段不要两套都配，否则后到的自动查询会覆盖映射结果。
   */
  function applyOptionFieldMappings(value) {
    const filled = buildOptionFieldMappingPatch(value)
    if (!Object.keys(filled).length)
      return
    const patchFormData = props.context?.patchFormData
    if (typeof patchFormData !== 'function')
      return
    patchFormData(filled)
  }

  /**
   * 按组件 fieldMappings 从当前选项行生成回填补丁（不含下拉自身字段）。
   */
  function buildOptionFieldMappingPatch(value) {
    if (fieldMultiple.value)
      return {}
    if (value === null || value === undefined || value === '')
      return {}
    const mappings = resolveComponentFieldMappings(props.field)
    if (!mappings || (Array.isArray(mappings) ? !mappings.length : !Object.keys(mappings).length))
      return {}
    const option = flattenOptionNodes(currentOptions.value).find(item =>
      isSameOptionValue(item?.value ?? item?.key, value),
    ) || flattenOptionNodes(remoteOptions.value).find(item =>
      isSameOptionValue(item?.value ?? item?.key, value),
    )
    if (!option)
      return {}
    const patch = applyRecordFieldMappings(option, mappings)
    // 映射源恰好是显示字段时：选项行可能只剩 label，用 label 兜底（用户配 fieldInput → fieldInput3）
    const labelField = String(
      props.field?.props?.optionSource?.labelField
      || props.field?.optionSource?.labelField
      || '',
    ).trim()
    if (labelField && option.label !== undefined && option.label !== null && String(option.label) !== '') {
      Object.entries(normalizeSelectorMappings(mappings)).forEach(([sourceField, targetField]) => {
        if (patch[targetField] !== undefined)
          return
        if (sourceField === labelField)
          patch[targetField] = option.label
      })
    }
    return Object.fromEntries(
      Object.entries(patch).filter(([, item]) => item !== undefined),
    )
  }

  /** 兼容 props.fieldMappings / recordSelector.fieldMappings / optionSource.fieldMappings */
  function resolveComponentFieldMappings(field = {}) {
    const fromProps = field.props?.fieldMappings || field.props?.mappings
    if (Array.isArray(fromProps) && fromProps.length)
      return fromProps
    if (fromProps && typeof fromProps === 'object' && !Array.isArray(fromProps) && Object.keys(fromProps).length)
      return fromProps
    const fromOptionSource = field.props?.optionSource?.fieldMappings
      || field.props?.optionSource?.mappings
      || field.optionSource?.fieldMappings
      || field.optionSource?.mappings
    if (Array.isArray(fromOptionSource) && fromOptionSource.length)
      return fromOptionSource
    if (fromOptionSource && typeof fromOptionSource === 'object' && !Array.isArray(fromOptionSource) && Object.keys(fromOptionSource).length)
      return fromOptionSource
    const fromSelector = normalizeRecordSelectorConfig(field).fieldMappings
    if (Array.isArray(fromSelector) && fromSelector.length)
      return fromSelector
    if (fromSelector && typeof fromSelector === 'object' && Object.keys(fromSelector).length)
      return fromSelector
    return field.fieldMappings || field.mappings || []
  }

  async function reloadObjectReferenceOptions(keyword = '') {
    if (isDesignerPreviewContext())
      return
    const config = resolveObjectReferenceConfig(props.field)
    if (!config.objectCode || !config.labelField)
      return
    remoteLoading.value = true
    try {
      // 过滤参数支持 ${formData.xxx} 模板，与弹窗选择器共用同一套解析逻辑；
      // 搜索字段优先用选择器配置，未配置时退回显示字段。
      const selectorConfig = recordSelectorConfig.value
      const searchParams = resolveSelectorSearchParams(
        selectorConfig.searchParams || {},
        recordSelectorRuntimeContext.value,
      )
      const keywordFields = (selectorConfig.keywordFields || []).length
        ? selectorConfig.keywordFields
        : [config.labelField]
      // 字段映射依赖选项上的源字段；把映射源和显示字段一并要回，避免选中后映射读空
      const mappingSources = collectFieldMappingSourceFields(selectorConfig.fieldMappings)
      const displayFields = [...new Set([
        `${config.labelField}:${config.labelField}`,
        ...mappingSources,
      ])]
      const res = await queryBusinessRecordSelector({
        objectCode: config.objectCode,
        keyword: keyword || undefined,
        keywordFields,
        displayFields,
        searchParams,
      }, { pageNum: 1, pageSize: 50 })
      const records = res.data?.records || []
      remoteOptions.value = records.map(record => ({
        ...record,
        label: record[config.labelField] || record.name || String(record[config.valueField] || record.id || ''),
        value: record[config.valueField] || record.id,
        _raw: extractSelectorRawRecord(record),
      }))
    }
    catch {
      // Keep existing options on error
    }
    finally {
      remoteLoading.value = false
    }
  }

  function resolveSelectionLabelValue(field = {}) {
    const remoteLabelField = String(
      field.optionSource?.labelField
      || field.props?.optionSource?.labelField
      || '',
    ).trim()
    for (const candidate of resolveSelectionLabelFields(field)) {
      // optionSource.labelField 是远端行字段名，不是表单伴随字段，不能拿来读 formData
      if (remoteLabelField && candidate === remoteLabelField)
        continue
      const value = readDataFieldValue(props.formData || {}, candidate)
      if (isFilledValue(value))
        return value
    }
    return field.labelValue ?? field.props?.labelValue ?? ''
  }

  function isFieldClearable(field = {}) {
    if (field.clearable === false || field.props?.clearable === false)
      return false
    return true
  }

  /** Naive TreeSelect 的 cascade 是多选勾选联动，不是业务级联配置对象 */
  function resolveNaiveTreeCascade(field = {}) {
    if (typeof field.cascade === 'boolean')
      return field.cascade
    if (typeof field.props?.cascade === 'boolean')
      return field.props.cascade
    // 业务级联配置对象不能传给 Naive
    if (field.cascade && typeof field.cascade === 'object')
      return true
    if (field.props?.cascade && typeof field.props.cascade === 'object')
      return true
    return field.cascade !== false
  }

  function syncSelectionLabelFromOptions(field = {}, value) {
    const patch = buildSelectionLabelPatch(field, value)
    if (!Object.keys(patch).length)
      return
    if (typeof props.context?.patchFormData !== 'function')
      return
    props.context.patchFormData(patch)
  }

  /** 从当前选项解析伴随名称补丁（供原子写入与 sync 共用） */
  function buildSelectionLabelPatch(field = {}, value) {
    if (!shouldSyncOptionLabels(field))
      return {}
    const multiple = isFieldMultiple(field) || fieldMultiple.value
    const values = parseSelectionValues(value, true)
    const labels = values
      .map(item => flattenOptionNodes(currentOptions.value).find(option => isSameOptionValue(option?.value ?? option?.key, item))?.label)
      .filter(Boolean)
    const isCleared = values.length === 0
      || values.every(item => item === null || item === undefined || item === '')
    if (!labels.length && !isCleared)
      return {}
    const labelValue = multiple ? labels : (labels[0] ?? '')
    return buildSelectionLabelValuePatch(field, labelValue)
  }

  function buildSelectionLabelValuePatch(field = {}, labelValue) {
    const candidates = resolveSelectionLabelFields(field)
    if (!candidates.length)
      return {}
    const normalizedLabel = normalizeLabelValue(labelValue)
    const explicitLabelField = firstNonBlank(field.props?.labelValueField, field.labelValueField)
    const patchTargets = explicitLabelField && candidates.includes(explicitLabelField)
      ? [explicitLabelField, ...candidates.filter(candidate => candidate !== explicitLabelField)]
      : candidates
    const patch = {}
    patchTargets.forEach((candidate, index) => {
      if (index === 0 || Object.prototype.hasOwnProperty.call(props.formData || {}, candidate))
        patch[candidate] = isFilledValue(normalizedLabel) ? normalizedLabel : undefined
    })
    return patch
  }

  function patchSelectionLabelValue(field = {}, labelValue) {
    const patch = buildSelectionLabelValuePatch(field, labelValue)
    if (!Object.keys(patch).length || typeof props.context?.patchFormData !== 'function')
      return
    props.context.patchFormData(patch)
  }

  function handleTreeSelectUpdate(field, newValue) {
    const multiple = isFieldMultiple(field) || fieldMultiple.value
    const normalizedValue = normalizeOptionValue(newValue, currentOptions.value, multiple)
    syncIncludeChildrenFlag(field, normalizedValue)
    if (isOrgTreeSelectField(field) || field?.type === 'treeSelect') {
      if (isFilledValue(normalizedValue))
        syncSelectionLabelFromOptions(field, normalizedValue)
      else
        patchSelectionLabelValue(field, '')
    }
    emit('update:value', serializeSelectionValues(normalizedValue, multiple))
  }

  function handleRegionTreeSelectUpdate(field, newValue) {
    if (!props.context?.isSearch || !field?.field) {
      emit('update:value', newValue)
      return
    }
    const includeChildrenKey = `${field.field}_includeChildren`
    if (newValue === null || newValue === undefined || newValue === '') {
      props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
      emit('update:value', newValue)
      return
    }
    const textValue = String(newValue)
    if (textValue.endsWith('ALL')) {
      props.context?.patchFormData?.({ [includeChildrenKey]: true })
      emit('update:value', textValue.replace(/ALL$/, ''))
      return
    }
    props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
    emit('update:value', newValue)
  }

  function resolveUserSelectLabel(field) {
    return resolveSelectionLabelValue(field) ?? ''
  }

  function handleUserSelectLabelUpdate(field, labelValue) {
    patchSelectionLabelValue(field, labelValue)
  }

  function handleUserSelect(field, users) {
    const selectedUsers = Array.isArray(users) ? users : users ? [users] : []
    const labels = selectedUsers.map(resolveUserLabel).filter(Boolean)
    const multiple = isFieldMultiple(field) || fieldMultiple.value
    if (labels.length || !selectedUsers.length)
      patchSelectionLabelValue(field, multiple ? labels : (labels[0] ?? ''))
    const events = getComponentEvents(field)
    if (typeof events.select === 'function')
      events.select(users)
  }

  function syncIncludeChildrenFlag(field, value) {
    if (!props.context?.isSearch || !field?.field || !(field.type === 'treeSelect' || isOrgTreeSelectField(field)))
      return
    const includeChildrenKey = `${field.field}_includeChildren`
    const expandedKey = `${field.field}__treeExpanded`
    if (Array.isArray(value) || value === null || value === undefined || value === '') {
      props.context?.patchFormData?.({
        [includeChildrenKey]: undefined,
        [expandedKey]: undefined,
      })
      return
    }
    // 查询区树形与左树一致：默认查本级+全部下级；字段显式关闭时才仅本级
    const allowIncludeChildren = field.includeChildren !== false
      && field.props?.includeChildren !== false
    if (!allowIncludeChildren) {
      props.context?.patchFormData?.({
        [includeChildrenKey]: undefined,
        [expandedKey]: undefined,
      })
      return
    }
    // 与左树相同：前端从已加载选项树展开子孙，提交时发 field=1,5
    const expanded = expandSearchTreeSelectValue(value, currentOptions.value, {
      includeChildren: true,
      childrenField: field.props?.optionSource?.childrenField
        || field.optionSource?.childrenField
        || 'children',
    })
    props.context?.patchFormData?.({
      [includeChildrenKey]: true,
      [expandedKey]: expanded.expanded.length > 1 ? expanded.expanded.join(',') : undefined,
    })
  }

  /**
   * 获取组件事件
   */
  function getComponentEvents(field) {
    if (!field.on)
      return {}

    const events = {}
    Object.keys(field.on).forEach((eventName) => {
      events[eventName] = (...args) => {
        if (typeof field.on[eventName] === 'function') {
          field.on[eventName]({
            field,
            formData: props.formData,
            context: props.context,
            args,
          })
        }
      }
    })
    return events
  }

  /**
   * 处理值更新
   */
  function handleUpdate(newValue) {
    const field = props.field || {}
    const multiple = fieldMultiple.value
    if (shouldSyncOptionLabels(field))
      syncSelectionLabelFromOptions(field, newValue)
    emit('update:value', multiple ? serializeSelectionValues(newValue, true) : newValue)
  }

  function shouldSyncOptionLabels(field = {}) {
    if (isUserSelectField(field))
      return false
    if (isRelationSelectorField.value && relationSelectorMode.value === 'popup')
      return false
    const type = normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey)
    return ['select', 'dictSelect', 'objectReference', 'recordSelector', 'orgTreeSelect', 'treeSelect', 'cascader'].includes(type)
  }

  function handleFieldFocusout(event) {
    if (!blurFieldEventEnabled.value)
      return
    if (event?.currentTarget?.contains?.(event.relatedTarget))
      return
    // 先跑设计器交互（setValue 等），再跑 fieldEvents 查询回填
    props.context?.runComponentInteractionEvents?.('blur', props.field?.field, props.value)
    props.context?.dispatchFieldEvent?.('BLUR', props.field?.field)
  }

  function handleFieldKeyup(event) {
    if (!scanFieldEventEnabled.value || event?.key !== 'Enter')
      return
    event.preventDefault?.()
    event.stopPropagation?.()
    props.context?.dispatchFieldEvent?.('SCAN_COMPLETE', props.field?.field)
  }

  async function handleScanFieldEvent() {
    if (scanLoading.value || disabledHandler(props.field))
      return
    const scanner = props.context?.scanField
    if (typeof scanner !== 'function') {
      setScanFeedback('error', '当前环境不支持扫码')
      return
    }
    scanLoading.value = true
    setScanFeedback('loading', '')
    try {
      const result = await scanner(props.field)
      if (!result?.value)
        throw Object.assign(new Error('扫码结果无效'), { code: 'SCAN_INVALID_RESULT' })
      handleUpdate(result.value)
      await nextTick()
      await props.context?.dispatchFieldEvent?.('SCAN_COMPLETE', props.field?.field, { scan: result })
      setScanFeedback('success', '')
    }
    catch (error) {
      setScanFeedback('error', resolveScanErrorMessage(error))
    }
    finally {
      scanLoading.value = false
    }
  }

  function setScanFeedback(status, message) {
    scanFeedback.value = { status, message: message || '' }
  }

  function handleManualFieldEvent() {
    if (fieldEventState.value.loading)
      return
    props.context?.dispatchFieldEvent?.('MANUAL', props.field?.field)
  }

  function openRecordSelector() {
    if (!recordSelectorConfig.value.objectCode) {
      window.$message?.warning('未配置选择器业务对象')
      return
    }
    recordSelectorVisible.value = true
  }

  function clearRecordSelectorValue() {
    const config = recordSelectorConfig.value
    const labelField = props.field?.labelField || props.field?.props?.labelField || props.field?.props?.targetLabelField || config.targetLabelField || config.labelTargetField
    const patch = { [props.field.field]: undefined }
    if (labelField)
      patch[labelField] = undefined
    if (relationLabelValueField.value)
      patch[relationLabelValueField.value] = undefined
    props.context?.patchFormData?.(patch)
    emit('update:value', fieldMultiple.value ? '' : null)
  }

  function handleRecordSelectorConfirm({ rows = [], mappings = {} } = {}) {
    const selectedRows = Array.isArray(rows) ? rows.filter(Boolean) : []
    if (!selectedRows.length)
      return
    const config = recordSelectorConfig.value
    const multiple = fieldMultiple.value || config.multiple === true
    const valueField = props.field?.valueField || props.field?.props?.valueField || config.valueField || 'id'
    const labelField = props.field?.labelField || props.field?.props?.labelField || props.field?.props?.targetLabelField || config.targetLabelField || config.labelTargetField
    const labelSourceField = props.field?.labelSourceField || props.field?.props?.labelSourceField || config.labelField || config.labelSourceField
    const labelSource = labelSourceField || firstDisplayFieldName(config.displayFields) || 'name'
    const values = []
    const labels = []
    selectedRows.forEach((selected) => {
      const rawRecord = extractSelectorRawRecord(selected)
      const value = rawRecord[valueField] ?? selected[valueField] ?? selected.id
      if (value === null || value === undefined || String(value).trim() === '')
        return
      values.push(value)
      labels.push(rawRecord[labelSource] ?? selected[labelSource] ?? selected.label ?? '')
    })
    const serializedValue = multiple ? serializeSelectionValues(values, true) : (values[0] ?? null)
    const serializedLabel = multiple ? serializeSelectionLabels(labels) : (labels[0] ?? '')
    const patch = {
      ...(multiple ? {} : applyRecordFieldMappings(selectedRows[0], mappings || config.fieldMappings)),
      [props.field.field]: serializedValue,
    }
    if (labelField)
      patch[labelField] = serializedLabel
    if (relationLabelValueField.value)
      patch[relationLabelValueField.value] = serializedLabel
    props.context?.patchFormData?.(patch)
    emit('update:value', serializedValue)
  }

  /** 弹窗展示字段配置可能是 "field:label" 冒号形式，取纯字段名。 */

  function handleRuntimePageWidgetUpdate(nextProps = {}) {
    const widgetKey = runtimePageWidgetKey.value
    if (widgetKey === 'rich-text' || widgetKey === 'markdown') {
      emit('update:value', nextProps.content || '')
      return
    }
    if (widgetKey === 'transfer') {
      emit('update:value', Array.isArray(nextProps.value) ? nextProps.value : [])
    }
  }

  function resolvePickerDefaultValue(field, range = false) {
    const configured = field?.pickerDefaultValue ?? field?.props?.pickerDefaultValue ?? field?.props?.defaultPickerValue
    const value = configured ?? pickerDefaultTimestamp
    return range ? [value, value] : value
  }

  function handleRangeUpdate(index, nextValue) {
    const next = Array.isArray(props.value) ? [...props.value] : [null, null]
    next[index] = nextValue
    emit('update:value', next)
  }

  /**
   * 处理文件上传变化
   */
  function handleUploadChange({ fileList }) {
    emit('update:value', fileList)
  }

  /**
   * 复制文本
   */
  function handleCopy(text) {
    copy(text)
    window.$message?.success('复制成功')
  }

  /**
   * 文件上传成功回调
   */
  function handleUploadSuccess(field, data) {
    if (field.onSuccess && typeof field.onSuccess === 'function') {
      field.onSuccess({
        data,
        field,
        formData: props.formData,
        context: props.context,
      })
    }
  }

  /**
   * 文件上传失败回调
   */
  function handleUploadError(field, error) {
    if (field.onError && typeof field.onError === 'function') {
      field.onError({
        error,
        field,
        formData: props.formData,
        context: props.context,
      })
    }
  }

  /**
   * 文件删除回调
   */
  function handleUploadRemove(field, file) {
    if (field.onRemove && typeof field.onRemove === 'function') {
      field.onRemove({
        file,
        field,
        formData: props.formData,
        context: props.context,
      })
    }
  }
  return {
    route, remoteOptions, remoteLoading, dictOptions, sourceDictOptions, recordSelectorVisible, scanLoading, scanFeedback,
    pickerDefaultTimestamp, remoteRequestSeq, ORG_TREE_SELECT_TYPES, USER_SELECT_TYPES, controlProps, numberFieldValue, numberFieldPrefix, numberFieldSuffix, inputFieldPrefix, inputFieldSuffix, switchControlProps, fieldRuntimeControl,
    fieldRuntimeVisible, formItemRef, arrayFieldRef, restoreFixedLabelWidth, registerArrayValidator, disabledHandler, fieldAlign, fieldAlignClass,
    formItemClass, resolvedFormItemStyle, componentControlStyle, componentControlClass, recordSelectorConfig, fieldMultiple, recordSelectorRuntimeContext, relationLabelValueField, recordSelectorDisplayText,
    fieldDescription, fieldLabelTip, fieldBadge, manualFieldEventEnabled, blurFieldEventEnabled, scanFieldEventEnabled, fieldEventState, showFieldEventFeedback,
    isSectionTitleField, isGroupTitleField, isFieldRequired, fieldDictType, cascadeConfig, dictCascadeConfig, remoteOptionSource, remoteOptionSourceKey,
    treeSelectLazyLoadHandler, runtimePageWidgetKey, isRuntimePageWidgetField, runtimePageWidgetProps, isLegacyGroupTitleField, cascadeSourceValue, sourceFieldConfig, sourceDictType,
    userSelectCascadeOrgId, userSelectCascadeIncludeChildren, isCascadeDisabledByEmptyParent, currentOptions, withCurrentValueOption, selectOptionLoading, loadDictOptions, loadSourceDictOptions,
    resolveOptionSource, shouldSuppressDesignerRemoteOptions, isDesignerPreviewContext, resolveCurrentChildrenSource, normalizeOptionSource, resolveDynamicOptionSource, ensureSelectorQueryObjectCode, resolveDynamicParams,
    loadRemoteOptions, handleTreeSelectLazyLoad, normalizeRemoteOptions, resolveCascadedOptions, matchesCascade, resolveSourceDictCode, buildDictTreeOptions, findSchemaField,
    clearCurrentValue, resolveOptionValue, resolveSliderValue, normalizeOptionValue, findOptionValue, isRelationSelectorField, relationSelectorMode, resolveObjectReferenceConfig,
    buildObjectReferenceOptionSource, objectReferenceRemoteEnabled, objectReferenceSearchKeyword, objectReferenceSearchTimer, handleObjectReferenceSearch, handleObjectReferenceUpdate, handleSelectUpdate, applyOptionFieldMappings,
    buildOptionFieldMappingPatch, resolveComponentFieldMappings, reloadObjectReferenceOptions, resolveSelectionLabelValue, isFieldClearable, resolveNaiveTreeCascade, syncSelectionLabelFromOptions, buildSelectionLabelPatch,
    buildSelectionLabelValuePatch, patchSelectionLabelValue, handleTreeSelectUpdate, handleRegionTreeSelectUpdate, resolveUserSelectLabel, handleUserSelectLabelUpdate, handleUserSelect, syncIncludeChildrenFlag,
    getComponentEvents, handleUpdate, shouldSyncOptionLabels, handleFieldFocusout, handleFieldKeyup, handleScanFieldEvent, setScanFeedback, handleManualFieldEvent,
    openRecordSelector, clearRecordSelectorValue, handleRecordSelectorConfirm, handleRuntimePageWidgetUpdate, resolvePickerDefaultValue, handleRangeUpdate, handleUploadChange, handleCopy,
    handleUploadSuccess, handleUploadError, handleUploadRemove, CopyOutline, resolveSwitchCheckedValue, resolveSwitchUncheckedValue, getPlaceholder, isOrgTreeSelectField,
    isUserSelectField, resolveFormattedRangeValue, normalizeTimestampPickerValue, normalizeFormattedPickerValue, normalizeTimestampRangePickerValue, normalizeFormattedRangePickerValue,
    isNumberLikeField, resolveNumberFieldType,
  }
}
