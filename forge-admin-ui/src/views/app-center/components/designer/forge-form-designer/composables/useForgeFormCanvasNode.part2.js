/** ForgeFormCanvasNode.vue setup part 2. */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiFormGroupTitle from '@/components/ai-form/AiFormGroupTitle.vue'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import AiFormSectionTitle from '@/components/ai-form/AiFormSectionTitle.vue'
import { normalizeRecordSelectorConfig as normalizePreviewRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import IconRenderer from '@/components/IconRenderer.vue'
import { DesignerGridRenderer, DesignerNodeOverlay } from '@/components/lowcode-builder/designer-core'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import {
  canAcceptDesignerChild,
  createComponentFromField,
  duplicateDesignerComponent,
  getDesignerComponent,
  insertDesignerComponent,
  isFieldComponent,
  isLayoutComponent,
  moveDesignerComponent,
  normalizeFormDesignerSchema,
  removeDesignerComponent,
  updateDesignerComponent,
} from '../../form-first/formDesignerSchema'
import {
  clearDesignerDragSource,
  clearDesignerDropError,
  clearDesignerDropKey,
  designerDragPreviewComponent,
  designerDragSourceId,
  designerDropKey,
  resolveDragPreviewFitScale,
  setDesignerDragSource,
  setDesignerDropError,
  setDesignerDropKey,
} from '../designerDragState'
import { createForgeFieldTemplateComponent, createForgeLayoutComponent } from '../designerLayoutFactory'
import { resolveDesignerCanvasPreviewValue } from '../field-default-value'
export function applyForgeFormCanvasNodePart2(props, emit, deps = {}) {
  const {
    __impl, mut, canDropIntoCurrentNode, canPointerDropInside, canPreviewDropIntoCurrentNode, cancelNodeResize, cancelPointerDrag, cleanupNodeResize,
    cleanupPointerDrag, clearDropPreview, clearPointerDropPreview, findNodeWrapRect, finishNodeResize, finishPointerDrag, handleAfterDrop, handleBeforeDrop,
    handleChildDrop, handleDropToChildren, handleDropToTarget, handleInsideDragOver, handleInsideDrop, handleNodeDragLeave, handleNodeDragOver, handleNodeDrop,
    handleNodeResizeMove, handlePointerMove, handleRowDropToColumn, handleTableDropToCell, hasForgeDragType, isDescendantNodeWrap, isSpanResizable, resizeAnchorTitle,
    resolveContainerSlotTarget, resolveDraggedComponent, resolveDropPosition, resolveInvalidDropMessage, resolveInvalidPointerDropMessage, resolveLayoutSlotAtPoint, resolveLayoutSlotFromEventTarget, resolveLayoutSlotIndex,
    resolvePointerRowColumnTarget, selectNode, showInvalidDrop, startNodeResize, startPointerDrag, syncNodeHeight, updatePointerDropPreview, DRAG_COMPONENT_MIME,
    DRAG_FIELD_MIME, DRAG_LAYOUT_MIME, DRAG_TEMPLATE_MIME, FORGE_DRAG_TYPES, MAX_FORM_GRID_COLUMNS, resizeDirections, activeDropPosition, dragging,
    resizing, nodeRef, nodeHeight, previewValue, isSelected, isField, isLayout, isTitle,
    isSubTableComponent, subTableDisplayModeLabel, subTableColumns, subTableConfigured, isButton, isCrudBlock, isGridRow, isGridColumn,
    columnRemovalLocked, columnDeleteTitle, isTableLayout, isCardLayout, isTabsLayout, isTabPaneLayout, isCollapseLayout, isPageWidget,
    isStructuralSlot, selectedDesignerStyle, designerBorderHidden, beforeDropKey, afterDropKey, insideDropKey, beforeActive, afterActive,
    activeInside, displayLabel, previewContext, previewFormData, rootColumns, parentGridColumns, nodeSpan, nodeWrapStyle,
    dropIndicatorStyle, nodeCustomStyle, nodeMenuOptions, childrenGridStyle, gridRowCells, previewField, crudDesignerFields, crudSearchFields,
    crudTableFields, crudEditFields, crudSearchSchema, crudEditSchema, crudPreviewColumns, crudApiConfig, crudPreviewOptions,
  } = deps
  function mountPointerDragImage(event) {
    removeDragImage()
    if (!nodeRef.value)
      return

    const rect = nodeRef.value.getBoundingClientRect()
    const layoutWidth = nodeRef.value.offsetWidth || rect.width
    const layoutHeight = nodeRef.value.offsetHeight || rect.height
    const scaleX = layoutWidth > 0 ? rect.width / layoutWidth : 1
    const scaleY = layoutHeight > 0 ? rect.height / layoutHeight : scaleX
    mut.activeDragImageScale = Math.max(0.1, Math.min(2, Number.isFinite(scaleX) ? scaleX : 1))
    mut.activeDragImageScaleY = Math.max(0.1, Math.min(2, Number.isFinite(scaleY) ? scaleY : mut.activeDragImageScale))
    mut.activeDragImageBaseWidth = Math.max(1, rect.width || layoutWidth)
    mut.activeDragImageBaseHeight = Math.max(1, rect.height || layoutHeight)
    mut.activeDragImageGrabRatio = {
      x: rect.width > 0 ? clampDragRatio((event.clientX - rect.left) / rect.width) : 0.5,
      y: rect.height > 0 ? clampDragRatio((event.clientY - rect.top) / rect.height) : 0.5,
    }
    const clone = nodeRef.value.cloneNode(true)
    clone.classList.add('drag-follow-clone')
    Object.assign(clone.style, {
      position: 'fixed',
      top: '0',
      left: '0',
      zIndex: '2147483647',
      width: `${layoutWidth}px`,
      height: `${layoutHeight}px`,
      margin: '0',
      background: props.component.props?.__designerStyle?.backgroundColor || '#fff',
      pointerEvents: 'none',
      transition: 'none',
      willChange: 'left, top, transform',
    })
    document.body.appendChild(clone)
    mut.activeDragImage = clone
    // 初始仅应用全局宽度上限；拖动过程中由 updatePointerDropPreview 按悬停目标
    // （栅格格子等）宽度动态收紧，影子视觉宽度始终贴合放置区域
    applyDragImageFitScale(resolveDragPreviewFitScale(mut.activeDragImageBaseWidth), event)
  }

  /**
   * 按缩放系数重设拖拽影子的 transform 与指针偏移：
   * - transform-origin 为 0 0，visual 尺寸 = base 尺寸 × fit；
   * - 偏移按初始抓取比例换算并重新 clamp，保证影子缩放后仍贴合指针抓取点。
   */
  function applyDragImageFitScale(fitScale, event) {
    if (!mut.activeDragImage)
      return
    mut.activeDragImageFitScale = Math.max(0.1, Math.min(1, Number.isFinite(fitScale) ? fitScale : 1))
    const visualWidth = mut.activeDragImageBaseWidth * mut.activeDragImageFitScale
    const visualHeight = mut.activeDragImageBaseHeight * mut.activeDragImageFitScale
    mut.activeDragImage.style.transform = `scale(${mut.activeDragImageScale * mut.activeDragImageFitScale}, ${mut.activeDragImageScaleY * mut.activeDragImageFitScale})`
    mut.activeDragImageOffset = {
      x: clampDragOffset(mut.activeDragImageGrabRatio.x * visualWidth, visualWidth, 20),
      y: clampDragOffset(mut.activeDragImageGrabRatio.y * visualHeight, visualHeight, 18),
    }
    if (event)
      updateDragImagePosition(event)
  }

  /**
   * 按悬停放置目标宽度同步拖拽影子缩放：
   * targetWidth 无效（如 0）时仅应用全局宽度上限，否则影子视觉宽度不超过目标宽度。
   */
  function syncDragImageFitScale(targetWidth = 0, event = null) {
    applyDragImageFitScale(resolveDragPreviewFitScale(mut.activeDragImageBaseWidth, targetWidth), event)
  }

  function removeDragImage() {
    mut.activeDragImage?.remove?.()
    mut.activeDragImage = null
    mut.activeDragImageOffset = { x: 0, y: 0 }
    mut.activeDragImageScale = 1
    mut.activeDragImageScaleY = 1
    mut.activeDragImageFitScale = 1
    mut.activeDragImageBaseWidth = 0
    mut.activeDragImageBaseHeight = 0
    mut.activeDragImageGrabRatio = { x: 0.5, y: 0.5 }
  }

  function updateDragImagePosition(event) {
    if (!mut.activeDragImage)
      return
    if (!event.clientX && !event.clientY)
      return
    const x = Math.round(event.clientX - mut.activeDragImageOffset.x)
    const y = Math.round(event.clientY - mut.activeDragImageOffset.y)
    mut.activeDragImage.style.left = `${x}px`
    mut.activeDragImage.style.top = `${y}px`
  }

  function clampDragOffset(value, size, minOffset) {
    if (!Number.isFinite(value))
      return minOffset
    if (size <= minOffset * 2)
      return Math.max(0, size / 2)
    return Math.min(Math.max(value, minOffset), size - minOffset)
  }

  function clampDragRatio(value) {
    if (!Number.isFinite(value))
      return 0.5
    return Math.min(1, Math.max(0, value))
  }

  function resolveRowColumns(component = {}) {
    const columns = Number(component.props?.columns || component.children?.length || rootColumns.value)
    return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number.isFinite(columns) ? columns : rootColumns.value))
  }

  function resolveTableColumns(component = {}) {
    const columns = Number(component.props?.columns || component.children?.length || rootColumns.value)
    return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number.isFinite(columns) ? columns : rootColumns.value))
  }

  function resolveGap(value, fallback) {
    const number = Number(value)
    return Number.isFinite(number) ? number : fallback
  }

  function collectDesignerFields(components = []) {
    return (Array.isArray(components) ? components : []).flatMap((item) => {
      if (!item)
        return []
      if (isFieldComponent(item))
        return [item]
      return collectDesignerFields(item.children || [])
    })
  }

  function isCrudRoleEnabled(field = {}, role = 'table', index = 0, maxSearchFields = 3) {
    const roles = field.props?.__crudRoles || {}
    if (Object.prototype.hasOwnProperty.call(roles, role))
      return roles[role] !== false
    if (role === 'search')
      return index < maxSearchFields
    return true
  }

  function toCrudFormField(component = {}, area = 'edit') {
    const componentKey = component.componentKey || 'input'
    const field = component.fieldBinding?.fieldCode || component.id || componentKey
    const rawProps = resolveRuntimeOptionProps({ ...(component.props || {}) }, componentKey)
    const crudConfig = rawProps.__crudConfig || {}
    delete rawProps.__designerStyle
    delete rawProps.customStyleText
    delete rawProps.__crudRoles
    delete rawProps.__crudConfig
    const areaConfig = crudConfig[area] || {}
    return {
      field,
      label: areaConfig.label || component.label || field,
      type: normalizePreviewFieldType(componentKey),
      placeholder: areaConfig.placeholder || rawProps.placeholder || buildPreviewPlaceholder(componentKey, component.label || field),
      required: Boolean(component.validation?.required),
      clearable: rawProps.clearable !== false,
      disabled: false,
      multiple: rawProps.multiple === true || rawProps.recordSelector?.multiple === true,
      readonly: area === 'edit' ? Boolean(crudConfig.edit?.readonly) : false,
      span: Math.max(1, Math.min(6, Number(areaConfig.span || component.layout?.span || 1))),
      dictType: rawProps.dictType,
      options: resolvePreviewOptions(rawProps, componentKey),
      showFeedback: false,
      props: {
        ...rawProps,
        disabled: false,
        readonly: false,
      },
    }
  }

  function buildFallbackCrudField(field, label, type) {
    return {
      id: `crud_${field}`,
      componentKey: type,
      label,
      fieldBinding: { fieldCode: field },
      props: {},
      layout: { span: 1 },
      validation: { required: false },
    }
  }

  function buildCrudApiConfig(apiBase = '/ai/crud/business_object') {
    let prefix = String(apiBase || '').trim() || '/ai/crud/business_object'
    if (!prefix.startsWith('/'))
      prefix = `/${prefix}`
    // 与 AiFormLayoutNodes.normalizeEmbeddedCrudApiBase 一致：剥 *_default_form
    const key = prefix
      .replace(/^\/ai\/crud\//i, '')
      .replace(/^\//, '')
      .replace(/-/g, '_')
      .replace(/[^a-z0-9_]/gi, '_')
      .replace(/_+/g, '_')
      .replace(/^_|_$/g, '')
      .replace(/_default_form$/i, '')
    prefix = `/ai/crud/${key || 'business_object'}`
    return {
      list: `get@${prefix}/page`,
      detail: `get@${prefix}/:id`,
      create: `post@${prefix}`,
      update: `put@${prefix}`,
      delete: `delete@${prefix}/:id`,
    }
  }

  function duplicateNode() {
    emit('update:schema', duplicateDesignerComponent(props.schema, props.component.id))
  }

  function removeNode() {
    // locked 只限制改组件/存储类型（属性面板），从画布删除组件不删字段资产，字段架子可重新拖入
    emit('update:schema', removeDesignerComponent(props.schema, props.component.id))
  }

  /**
   * 删除栅格格子：连同格子内的组件一起从画布删除（字段资产仍保留在左侧字段架子，
   * 可重新拖入）。与属性面板「格子数量」调减是刻意分离的两种语义——后者是布局重排，
   * 多余格子的组件并入最后一格；画布删除按钮是明确删除动作，不应把内容“搬家”。
   */
  function removeGridColumn() {
    if (columnRemovalLocked.value)
      return
    const schema = normalizeFormDesignerSchema(props.schema)
    // 必须在同一份 normalize 后的 schema 上定位父节点：getDesignerComponent 内部会二次
    // clone，返回的是副本引用，对它修改不会体现在 emit 的 schema 上
    const parent = findComponentById(schema.components, props.parentId)
    if (!parent || !['row', 'fcRow'].includes(parent.componentKey))
      return
    const indexInParent = (parent.children || []).findIndex(child => child.id === props.component.id)
    if (indexInParent === -1)
      return
    parent.children.splice(indexInParent, 1)
    emit('update:schema', normalizeFormDesignerSchema(schema))
  }

  function findComponentById(components = [], componentId = '') {
    for (const component of Array.isArray(components) ? components : []) {
      if (component?.id === componentId)
        return component
      const found = findComponentById(component?.children || [], componentId)
      if (found)
        return found
    }
    return null
  }

  function toggleRequired() {
    if (!isField.value)
      return
    const required = !props.component.validation?.required
    emit('update:schema', updateDesignerComponent(props.schema, props.component.id, {
      validation: {
        required,
        requiredMessage: required ? props.component.validation?.requiredMessage || `${displayLabel.value}不能为空` : '',
      },
    }))
  }

  function handleNodeMenuSelect(key) {
    if (key === 'config') {
      emit('select', props.component.id)
      emit('configure', props.component.id)
      return
    }
    if (key === 'copy') {
      duplicateNode()
      return
    }
    if (key === 'required') {
      toggleRequired()
      return
    }
    if (key === 'delete') {
      removeNode()
      return
    }
    if (key.startsWith('bg-')) {
      updateNodeDesignerStyle(resolveBackgroundStyle(key))
      return
    }
    if (key.startsWith('border-'))
      updateNodeDesignerStyle(resolveBorderStyle(key))
  }

  function updateNodeDesignerStyle(stylePatch = {}) {
    const current = props.component.props?.__designerStyle || {}
    emit('update:schema', updateDesignerComponent(props.schema, props.component.id, {
      props: {
        __designerStyle: {
          ...current,
          ...stylePatch,
        },
      },
    }))
  }

  function handlePageWidgetPropsUpdate(propsData = {}) {
    emit('update:schema', updateDesignerComponent(props.schema, props.component.id, {
      props: propsData,
    }))
  }

  function resolveBackgroundStyle(key) {
    const map = {
      'bg-default': { backgroundColor: undefined },
      'bg-gray': { backgroundColor: '#f3f4f6' },
      'bg-blue': { backgroundColor: '#eff6ff' },
      'bg-green': { backgroundColor: '#ecfdf5' },
      'bg-yellow': { backgroundColor: '#fffbeb' },
    }
    return map[key] || {}
  }

  function resolveBorderStyle(key) {
    const map = {
      'border-default': { borderColor: undefined, borderStyle: undefined, hideInnerBorder: false },
      'border-dashed': { borderColor: '#9ca3af', borderStyle: 'dashed', hideInnerBorder: false },
      'border-blue': { borderColor: '#2563eb', borderStyle: 'solid', hideInnerBorder: false },
      'border-none': { borderColor: 'transparent', borderStyle: 'none', hideInnerBorder: true },
    }
    return map[key] || {}
  }

  function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value))
  }

  function parsePayload(value) {
    try {
      return JSON.parse(value || '{}')
    }
    catch {
      return {}
    }
  }

  function normalizePreviewFieldType(componentKey = '') {
    const typeMap = {
      integer: 'number',
      money: 'number',
      orgSelect: 'orgTreeSelect',
      departmentSelect: 'orgTreeSelect',
      departmentTreeSelect: 'orgTreeSelect',
      deptSelect: 'orgTreeSelect',
      deptTreeSelect: 'orgTreeSelect',
      userPicker: 'userSelect',
      upload: 'fileUpload',
    }
    return typeMap[componentKey] || componentKey || 'input'
  }

  function buildPreviewPlaceholder(componentKey = '', label = '') {
    if (['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'transfer', 'date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange', 'userSelect', 'orgTreeSelect', 'regionTreeSelect', 'treeSelect', 'customSelect', 'color'].includes(componentKey))
      return `请选择${label}`
    return `请填写${label}`
  }

  function buildPreviewOptions(componentKey = '') {
    if (!['select', 'radio', 'radioButton', 'checkbox', 'transfer'].includes(componentKey))
      return undefined
    if (componentKey === 'transfer') {
      return [
        { label: '选项一', value: 'option1' },
        { label: '选项二', value: 'option2' },
        { label: '选项三', value: 'option3' },
      ]
    }
    return [
      { label: '选项一', value: '1' },
      { label: '选项二', value: '2' },
    ]
  }

  function resolveRuntimeOptionProps(rawProps = {}, componentKey = '') {
    const nextProps = { ...(rawProps || {}) }
    if (shouldUseDictOptions(nextProps, componentKey))
      delete nextProps.options
    // 动态源下清掉残留静态 options，避免 AiFormItem / Naive 再读到「选项一/选项二」
    if (hasDynamicPreviewOptionSource(nextProps.optionSource))
      delete nextProps.options
    return nextProps
  }

  function resolvePreviewFieldProps(component = {}, componentKey = '', fieldCode = '') {
    const rawProps = resolveRuntimeOptionProps({ ...(component.props || {}) }, componentKey)
    return mergeRelationPreviewProps(rawProps, component, findFieldAsset(fieldCode))
  }

  function findFieldAsset(fieldCode = '') {
    const code = String(fieldCode || '').trim()
    if (!code)
      return null
    return (Array.isArray(props.fields) ? props.fields : []).find((field) => {
      const candidates = [
        field?.fieldCode,
        field?.field,
        field?.columnName,
        field?.prop,
        field?.name,
      ].map(value => String(value ?? '').trim()).filter(Boolean)
      return candidates.includes(code)
    }) || null
  }

  function mergeRelationPreviewProps(rawProps = {}, component = {}, fieldAsset = null) {
    const next = { ...(rawProps || {}) }
    const fieldProps = {
      ...(fieldAsset?.basicProps && typeof fieldAsset.basicProps === 'object' ? fieldAsset.basicProps : {}),
      ...(fieldAsset?.props && typeof fieldAsset.props === 'object' ? fieldAsset.props : {}),
    }
    const componentKey = component?.componentKey || component?.type || ''
    const fieldType = String(fieldAsset?.fieldType || fieldAsset?.businessFieldType || '').trim()

    const referenceObjectCode = firstText(
      next.referenceObjectCode,
      component.props?.referenceObjectCode,
      component.referenceObjectCode,
      fieldAsset?.props?.referenceObjectCode,
      fieldAsset?.referenceObjectCode,
      fieldProps.referenceObjectCode,
    )
    const referenceDisplayField = firstText(
      next.referenceDisplayField,
      next.displayField,
      next.labelField,
      component.props?.referenceDisplayField,
      component.props?.displayField,
      component.props?.labelField,
      component.referenceDisplayField,
      fieldAsset?.props?.referenceDisplayField,
      fieldAsset?.props?.displayField,
      fieldAsset?.props?.labelField,
      fieldAsset?.referenceDisplayField,
      fieldProps.referenceDisplayField,
    )
    const referenceValueField = firstText(
      next.referenceValueField,
      next.valueField,
      component.props?.referenceValueField,
      component.props?.valueField,
      component.referenceValueField,
      fieldAsset?.props?.referenceValueField,
      fieldAsset?.props?.valueField,
      fieldAsset?.referenceValueField,
      fieldProps.referenceValueField,
      'id',
    )

    if (componentKey === 'objectReference' || fieldType === 'REFERENCE') {
      if (referenceObjectCode)
        next.referenceObjectCode = referenceObjectCode
      if (referenceDisplayField)
        next.referenceDisplayField = referenceDisplayField
      if (referenceValueField)
        next.referenceValueField = referenceValueField
    }

    const recordSelector = normalizePreviewRecordSelectorConfig({
      ...(fieldAsset || {}),
      ...(component || {}),
      ...(next || {}),
      basicProps: {
        ...(fieldAsset?.basicProps || {}),
        ...(component?.basicProps || {}),
      },
      props: {
        ...(fieldAsset?.props || {}),
        ...(fieldAsset?.basicProps || {}),
        ...(component?.props || {}),
        ...(next || {}),
      },
      recordSelector: next.recordSelector
        || component.props?.recordSelector
        || component.recordSelector
        || fieldAsset?.props?.recordSelector
        || fieldAsset?.basicProps?.recordSelector
        || fieldAsset?.recordSelector,
    })
    if ((componentKey === 'recordSelector' || fieldType === 'RECORD_SELECTOR') && recordSelector.objectCode) {
      next.recordSelector = recordSelector
      next.objectCode = recordSelector.objectCode
      next.businessObjectCode = recordSelector.businessObjectCode || recordSelector.objectCode
      next.targetObjectCode = recordSelector.targetObjectCode || recordSelector.objectCode
    }
    return next
  }

  function firstText(...values) {
    return values.map(value => String(value ?? '').trim()).find(Boolean) || ''
  }

  function resolvePreviewOptions(rawProps = {}, componentKey = '') {
    if (shouldUseDictOptions(rawProps, componentKey))
      return undefined
    // 已配置动态选项源时，不要再注入「选项一/选项二」占位，否则会盖住业务对象数据
    if (hasDynamicPreviewOptionSource(rawProps.optionSource))
      return undefined
    if (Array.isArray(rawProps.options) && rawProps.options.length)
      return rawProps.options
    return buildPreviewOptions(componentKey)
  }

  function hasDynamicPreviewOptionSource(source = null) {
    if (!source || typeof source !== 'object')
      return false
    const type = String(source.type || '').toUpperCase()
    if (['CURRENT_CHILDREN', 'QUERY_SOURCE', 'REMOTE', 'BUSINESS_OBJECT'].includes(type))
      return true
    return Boolean(String(source.api || source.url || source.sourceKey || '').trim())
  }

  function shouldUseDictOptions(rawProps = {}, componentKey = '') {
    return Boolean(rawProps.dictType) && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader'].includes(componentKey)
  }
  function buildGroupTitleProps(component) {
    return {
      ...(component?.props || {}),
      title: component?.props?.title || component?.label || '分组标题',
      label: component?.label || component?.props?.title || '分组标题',
    }
  }

  function isFormDividerComponent(component) {
    return ['AiFormSectionTitle', 'aiFormSectionTitle', 'formDivider', 'formSectionTitle'].includes(component?.componentKey)
  }

  function buildFormDividerProps(component) {
    return {
      ...(component?.props || {}),
      title: component?.props?.title || component?.label || '表单分隔线',
      label: component?.label || component?.props?.title || '表单分隔线',
    }
  }
  __impl.mountPointerDragImage = mountPointerDragImage
  __impl.applyDragImageFitScale = applyDragImageFitScale
  __impl.syncDragImageFitScale = syncDragImageFitScale
  __impl.removeDragImage = removeDragImage
  __impl.updateDragImagePosition = updateDragImagePosition
  __impl.clampDragOffset = clampDragOffset
  __impl.clampDragRatio = clampDragRatio
  __impl.resolveRowColumns = resolveRowColumns
  __impl.resolveTableColumns = resolveTableColumns
  __impl.resolveGap = resolveGap
  __impl.collectDesignerFields = collectDesignerFields
  __impl.isCrudRoleEnabled = isCrudRoleEnabled
  __impl.toCrudFormField = toCrudFormField
  __impl.buildFallbackCrudField = buildFallbackCrudField
  __impl.buildCrudApiConfig = buildCrudApiConfig
  __impl.duplicateNode = duplicateNode
  __impl.removeNode = removeNode
  __impl.removeGridColumn = removeGridColumn
  __impl.findComponentById = findComponentById
  __impl.toggleRequired = toggleRequired
  __impl.handleNodeMenuSelect = handleNodeMenuSelect
  __impl.updateNodeDesignerStyle = updateNodeDesignerStyle
  __impl.handlePageWidgetPropsUpdate = handlePageWidgetPropsUpdate
  __impl.resolveBackgroundStyle = resolveBackgroundStyle
  __impl.resolveBorderStyle = resolveBorderStyle
  __impl.clamp = clamp
  __impl.parsePayload = parsePayload
  __impl.normalizePreviewFieldType = normalizePreviewFieldType
  __impl.buildPreviewPlaceholder = buildPreviewPlaceholder
  __impl.buildPreviewOptions = buildPreviewOptions
  __impl.resolveRuntimeOptionProps = resolveRuntimeOptionProps
  __impl.resolvePreviewFieldProps = resolvePreviewFieldProps
  __impl.findFieldAsset = findFieldAsset
  __impl.mergeRelationPreviewProps = mergeRelationPreviewProps
  __impl.firstText = firstText
  __impl.resolvePreviewOptions = resolvePreviewOptions
  __impl.hasDynamicPreviewOptionSource = hasDynamicPreviewOptionSource
  __impl.shouldUseDictOptions = shouldUseDictOptions
  __impl.buildGroupTitleProps = buildGroupTitleProps
  __impl.isFormDividerComponent = isFormDividerComponent
  __impl.buildFormDividerProps = buildFormDividerProps

  return {
    ...deps,
  }
}
