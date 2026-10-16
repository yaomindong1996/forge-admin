/** ForgeFormCanvasNode.vue setup part 1. */
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
export function applyForgeFormCanvasNodePart1(props, emit) {
  const __impl = {}
  const mut = {
    activeDragImage: null,
    activeDragImageOffset: { x: 0, y: 0 },
    activeDragImageScale: 1,
    activeDragImageScaleY: 1,
    activeDragImageFitScale: 1,
    activeDragImageBaseWidth: 0,
    activeDragImageBaseHeight: 0,
    activeDragImageGrabRatio: { x: 0.5, y: 0.5 },
    activePointerDropTarget: null,
    activePointerHandle: null,
    activePointerId: null,
    pointerDragStarted: false,
    activeResizeHandle: null,
    activeResizePointerId: null,
    activeResizeState: null,
    resizeObserver: null,
  }
  const applyDragImageFitScale = (...args) => __impl.applyDragImageFitScale(...args)
  const buildCrudApiConfig = (...args) => __impl.buildCrudApiConfig(...args)
  const buildFallbackCrudField = (...args) => __impl.buildFallbackCrudField(...args)
  const buildFormDividerProps = (...args) => __impl.buildFormDividerProps(...args)
  const buildGroupTitleProps = (...args) => __impl.buildGroupTitleProps(...args)
  const buildPreviewOptions = (...args) => __impl.buildPreviewOptions(...args)
  const buildPreviewPlaceholder = (...args) => __impl.buildPreviewPlaceholder(...args)
  const clamp = (...args) => __impl.clamp(...args)
  const clampDragOffset = (...args) => __impl.clampDragOffset(...args)
  const clampDragRatio = (...args) => __impl.clampDragRatio(...args)
  const collectDesignerFields = (...args) => __impl.collectDesignerFields(...args)
  const duplicateNode = (...args) => __impl.duplicateNode(...args)
  const findComponentById = (...args) => __impl.findComponentById(...args)
  const findFieldAsset = (...args) => __impl.findFieldAsset(...args)
  const firstText = (...args) => __impl.firstText(...args)
  const handleNodeMenuSelect = (...args) => __impl.handleNodeMenuSelect(...args)
  const handlePageWidgetPropsUpdate = (...args) => __impl.handlePageWidgetPropsUpdate(...args)
  const hasDynamicPreviewOptionSource = (...args) => __impl.hasDynamicPreviewOptionSource(...args)
  const isCrudRoleEnabled = (...args) => __impl.isCrudRoleEnabled(...args)
  const isFormDividerComponent = (...args) => __impl.isFormDividerComponent(...args)
  const mergeRelationPreviewProps = (...args) => __impl.mergeRelationPreviewProps(...args)
  const mountPointerDragImage = (...args) => __impl.mountPointerDragImage(...args)
  const normalizePreviewFieldType = (...args) => __impl.normalizePreviewFieldType(...args)
  const parsePayload = (...args) => __impl.parsePayload(...args)
  const removeDragImage = (...args) => __impl.removeDragImage(...args)
  const removeGridColumn = (...args) => __impl.removeGridColumn(...args)
  const removeNode = (...args) => __impl.removeNode(...args)
  const resolveBackgroundStyle = (...args) => __impl.resolveBackgroundStyle(...args)
  const resolveBorderStyle = (...args) => __impl.resolveBorderStyle(...args)
  const resolveGap = (...args) => __impl.resolveGap(...args)
  const resolvePreviewFieldProps = (...args) => __impl.resolvePreviewFieldProps(...args)
  const resolvePreviewOptions = (...args) => __impl.resolvePreviewOptions(...args)
  const resolveRowColumns = (...args) => __impl.resolveRowColumns(...args)
  const resolveRuntimeOptionProps = (...args) => __impl.resolveRuntimeOptionProps(...args)
  const resolveTableColumns = (...args) => __impl.resolveTableColumns(...args)
  const shouldUseDictOptions = (...args) => __impl.shouldUseDictOptions(...args)
  const syncDragImageFitScale = (...args) => __impl.syncDragImageFitScale(...args)
  const toCrudFormField = (...args) => __impl.toCrudFormField(...args)
  const toggleRequired = (...args) => __impl.toggleRequired(...args)
  const updateDragImagePosition = (...args) => __impl.updateDragImagePosition(...args)
  const updateNodeDesignerStyle = (...args) => __impl.updateNodeDesignerStyle(...args)
  
  const DRAG_COMPONENT_MIME = 'application/x-forge-form-component'
  const DRAG_FIELD_MIME = 'application/x-forge-form-field'
  const DRAG_LAYOUT_MIME = 'application/x-forge-form-layout'
  const DRAG_TEMPLATE_MIME = 'application/x-forge-form-template'
  const FORGE_DRAG_TYPES = [DRAG_COMPONENT_MIME, DRAG_FIELD_MIME, DRAG_LAYOUT_MIME, DRAG_TEMPLATE_MIME]
  const MAX_FORM_GRID_COLUMNS = 24
  const resizeDirections = ['n', 'e', 's', 'w', 'ne', 'nw', 'se', 'sw']
  // 拖拽跟随影子状态：影子宽度除全局上限外，还会按悬停目标（如栅格格子）宽度
  // 动态等比钳制，避免宽组件拖入窄格子时影子视觉超出栅格

  const activeDropPosition = ref('')
  const dragging = ref(false)
  const resizing = ref(false)
  const nodeRef = ref(null)
  const nodeHeight = ref(72)
  const previewValue = ref(null)

  const isSelected = computed(() => props.selectedId === props.component.id)
  const isField = computed(() => isFieldComponent(props.component))
  const isLayout = computed(() => isLayoutComponent(props.component))
  const isTitle = computed(() => ['title', 'fcTitle'].includes(props.component.componentKey))
  const isSubTableComponent = computed(() => props.component.componentKey === 'subTable')

  watch(
    () => ({
      componentKey: props.component?.componentKey,
      defaultValue: props.component?.props?.defaultValue,
      checkedValue: props.component?.props?.checkedValue,
      uncheckedValue: props.component?.props?.uncheckedValue,
      optionsSignature: JSON.stringify(props.component?.props?.options || null),
    }),
    () => {
      if (!isField.value)
        return
      previewValue.value = resolveDesignerCanvasPreviewValue(props.component)
    },
    { immediate: true },
  )
  const subTableDisplayModeLabel = computed(() => {
    const labels = { inline_grid: '行内表格', card_list: '卡片列表', bottom_sheet: '底部抽屉' }
    return labels[props.component.props?.displayMode] || '行内表格'
  })
  const subTableColumns = computed(() => {
    const cols = props.component.props?.columns
    return Array.isArray(cols) ? cols : []
  })
  const subTableConfigured = computed(() => {
    const p = props.component.props
    return Boolean(p?.relationKey || p?.modelCode)
  })
  const isButton = computed(() => ['button', 'elButton'].includes(props.component.componentKey))
  const isCrudBlock = computed(() => ['AiCrudPage', 'crudBlock'].includes(props.component.componentKey))
  const isGridRow = computed(() => ['row', 'fcRow'].includes(props.component.componentKey))
  const isGridColumn = computed(() => props.component.componentKey === 'col')
  // 栅格至少保留一个格子，否则行失去栅格语义
  const columnRemovalLocked = computed(() => {
    if (!isGridColumn.value)
      return true
    const parent = props.parentId ? getDesignerComponent(props.schema, props.parentId) : null
    if (!parent || !['row', 'fcRow'].includes(parent.componentKey))
      return true
    return (parent.children || []).filter(child => child?.componentKey === 'col').length <= 1
  })
  // 删除格子的按钮提示：带内容时明确告知内部组件会一并删除（字段资产仍可从左侧重新拖入）
  const columnDeleteTitle = computed(() => {
    if (columnRemovalLocked.value)
      return '至少保留一个格子'
    const childCount = (props.component.children || []).length
    return childCount > 0 ? `删除该格子（内部 ${childCount} 个组件一并删除，字段可从左侧重新拖入）` : '删除该格子'
  })
  const isTableLayout = computed(() => ['table', 'fcTable'].includes(props.component.componentKey))
  const isCardLayout = computed(() => ['card', 'elCard'].includes(props.component.componentKey))
  const isTabsLayout = computed(() => ['tabs', 'elTabs'].includes(props.component.componentKey))
  const isTabPaneLayout = computed(() => ['tabPane', 'elTabPane'].includes(props.component.componentKey))
  const isCollapseLayout = computed(() => ['collapse', 'elCollapse'].includes(props.component.componentKey))
  const isPageWidget = computed(() => isPageWidgetComponentKey(props.component.componentKey))
  const isStructuralSlot = computed(() => ['col', 'tableGrid', 'fcTableGrid', 'tabPane', 'elTabPane', 'collapseItem', 'elCollapseItem'].includes(props.component.componentKey))
  const selectedDesignerStyle = computed(() => props.component.props?.__designerStyle || {})
  const designerBorderHidden = computed(() => selectedDesignerStyle.value.hideInnerBorder || selectedDesignerStyle.value.borderStyle === 'none')
  const beforeDropKey = computed(() => `${props.component.id}:before`)
  const afterDropKey = computed(() => `${props.component.id}:after`)
  const insideDropKey = computed(() => `${props.component.id}:inside`)
  const beforeActive = computed(() => designerDropKey.value === beforeDropKey.value)
  const afterActive = computed(() => designerDropKey.value === afterDropKey.value)
  const activeInside = computed(() => designerDropKey.value === insideDropKey.value)
  const displayLabel = computed(() => props.component.label || props.component.props?.header || props.component.props?.title || props.component.componentKey)
  const previewContext = computed(() => ({
    mode: 'designer-preview',
    disableRuntimeRules: true,
    // 画布单字段预览不拉远程选项，避免每个控件都打查询源；完整回填请用顶部「预览」
    allowOptionSourceFetch: false,
  }))
  const previewFormData = computed(() => ({
    [props.component.fieldBinding?.fieldCode || props.component.id]: previewValue.value,
  }))

  function selectNode() {
    // A tab pane is structural content of Tabs. Selecting it made the property
    // panel show generic pane properties instead of the Tabs pane manager.
    emit('select', isTabPaneLayout.value && props.parentId ? props.parentId : props.component.id)
  }

  const rootColumns = computed(() => Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number(props.schema.layout?.gridColumns || 2))))
  const parentGridColumns = computed(() => {
    if (props.component.componentKey !== 'col')
      return rootColumns.value
    const parent = props.parentId ? getDesignerComponent(props.schema, props.parentId) : null
    if (!['row', 'fcRow'].includes(parent?.componentKey))
      return rootColumns.value
    const columns = Number(parent.props?.columns || MAX_FORM_GRID_COLUMNS)
    return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, Number.isFinite(columns) ? columns : MAX_FORM_GRID_COLUMNS))
  })
  const nodeSpan = computed(() => Math.max(1, Math.min(parentGridColumns.value, Number(props.component.layout?.span || props.component.props?.span || 1))))
  const nodeWrapStyle = computed(() => ({
    gridColumn: props.depth === 0 || props.component.componentKey === 'col' ? `span ${nodeSpan.value}` : undefined,
  }))
  const dropIndicatorStyle = computed(() => ({
    '--drop-placeholder-height': `${Math.max(nodeHeight.value, 44)}px`,
  }))
  const nodeCustomStyle = computed(() => {
    const designerStyle = props.component.props?.__designerStyle || {}
    const minHeight = designerStyle.minHeight || undefined
    const backgroundColor = designerStyle.backgroundColor || undefined
    const borderRadius = designerStyle.borderRadius || undefined
    const borderColor = designerBorderHidden.value ? 'transparent' : designerStyle.borderColor || undefined
    const borderWidth = designerBorderHidden.value
      ? '0px'
      : (designerStyle.borderWidth || undefined)
    const style = {
      ...(designerStyle.customStyle || {}),
      width: designerStyle.width || undefined,
      height: designerStyle.height || undefined,
      minHeight: isStructuralSlot.value ? undefined : minHeight,
      backgroundColor,
      borderColor,
      borderStyle: designerStyle.borderStyle || undefined,
      borderWidth,
      borderRadius,
      boxShadow: designerStyle.boxShadow || undefined,
      opacity: designerStyle.opacity || undefined,
    }
    if (minHeight)
      style['--forge-slot-min-height'] = minHeight
    if (backgroundColor)
      style['--forge-node-background'] = backgroundColor
    if (borderRadius)
      style['--forge-node-radius'] = borderRadius
    if (borderWidth)
      style['--forge-node-border-width'] = borderWidth
    return style
  })
  const nodeMenuOptions = computed(() => [
    { label: '配置', key: 'config' },
    { label: props.component.validation?.required ? '取消必填' : '设为必填', key: 'required', disabled: !isField.value },
    { label: '复制', key: 'copy' },
    {
      label: '更换背景色',
      key: 'background',
      children: [
        { label: '恢复默认', key: 'bg-default' },
        { label: '浅灰', key: 'bg-gray' },
        { label: '浅蓝', key: 'bg-blue' },
        { label: '浅绿', key: 'bg-green' },
        { label: '浅黄', key: 'bg-yellow' },
      ],
    },
    {
      label: '背景描边',
      key: 'border',
      children: [
        { label: '恢复默认', key: 'border-default' },
        { label: '灰色虚线', key: 'border-dashed' },
        { label: '蓝色实线', key: 'border-blue' },
        { label: '隐藏边框', key: 'border-none' },
      ],
    },
    { label: '移入', key: 'move-into', disabled: true },
    { type: 'divider', key: 'divider' },
    { label: '删除', key: 'delete' },
  ])
  const childrenGridStyle = computed(() => {
    if (!isGridRow.value && !isTableLayout.value)
      return {}
    const count = isTableLayout.value ? resolveTableColumns(props.component) : resolveRowColumns(props.component)
    return {
      gridTemplateColumns: `repeat(${count}, minmax(0, 1fr))`,
      columnGap: `${resolveGap(props.component.props?.gutter, 12)}px`,
      rowGap: `${resolveGap(props.component.props?.rowGap, 8)}px`,
    }
  })

  /** 将栅格行子节点（col）归一化为 DesignerGridRenderer cells 格式 */
  const gridRowCells = computed(() => {
    if (!isGridRow.value)
      return []
    return (props.component.children || []).map((child, index) => ({
      key: child.id,
      span: Number(child.props?.span || child.layout?.span || 1),
      children: child.children || [],
      _component: child,
      _index: index,
    }))
  })
  const previewField = computed(() => {
    const componentKey = props.component.componentKey || 'input'
    const fieldCode = props.component.fieldBinding?.fieldCode || props.component.id || componentKey
    const rawProps = resolvePreviewFieldProps(props.component, componentKey, fieldCode)
    delete rawProps.disabled
    delete rawProps.readonly
    // 画布上也要反映「只读」，否则属性面板勾选后看起来完全不生效
    const readonly = Boolean(props.component.visibility?.readonly)
    const disabled = readonly || Boolean(props.component.props?.disabled)
    return {
      field: fieldCode,
      label: displayLabel.value,
      type: normalizePreviewFieldType(componentKey),
      span: nodeSpan.value,
      labelWidth: props.component.layout?.labelWidth || props.schema.layout?.labelWidth || 100,
      placeholder: rawProps.placeholder || buildPreviewPlaceholder(componentKey, displayLabel.value),
      required: Boolean(props.component.validation?.required),
      clearable: rawProps.clearable !== false,
      disabled,
      readonly,
      visibility: {
        ...(props.component.visibility || {}),
        readonly,
      },
      multiple: rawProps.multiple === true || rawProps.recordSelector?.multiple === true,
      dictType: rawProps.dictType,
      defaultValue: rawProps.defaultValue,
      checkedValue: rawProps.checkedValue,
      uncheckedValue: rawProps.uncheckedValue,
      checkedText: rawProps.checkedText,
      uncheckedText: rawProps.uncheckedText,
      size: rawProps.size || props.schema.layout?.size || 'medium',
      options: resolvePreviewOptions(rawProps, componentKey),
      props: {
        ...rawProps,
        runtimeRules: [],
        disabled,
        readonly,
      },
      showFeedback: false,
    }
  })
  const crudDesignerFields = computed(() => collectDesignerFields(props.component.children || []))
  const crudSearchFields = computed(() => {
    const maxVisible = Math.max(1, Math.min(6, Number(props.component.props?.crudOptions?.searchMaxVisibleFields || 3)))
    return crudDesignerFields.value.filter((field, index) => isCrudRoleEnabled(field, 'search', index, maxVisible))
  })
  const crudTableFields = computed(() => crudDesignerFields.value.filter((field, index) => isCrudRoleEnabled(field, 'table', index)))
  const crudEditFields = computed(() => crudDesignerFields.value.filter((field, index) => isCrudRoleEnabled(field, 'edit', index)))
  const crudSearchSchema = computed(() => {
    const fields = crudSearchFields.value
    return fields.length ? fields.map(field => toCrudFormField(field, 'search')) : [buildFallbackCrudField('keyword', '关键词', 'input')]
  })
  const crudEditSchema = computed(() => {
    const fields = crudEditFields.value.length
      ? crudEditFields.value
      : [
          buildFallbackCrudField('name', '名称', 'input'),
          buildFallbackCrudField('status', '状态', 'select'),
        ]
    return fields.map(field => toCrudFormField(field, 'edit'))
  })
  const crudPreviewColumns = computed(() => {
    const fields = crudTableFields.value.length
      ? crudTableFields.value
      : [
          buildFallbackCrudField('name', '名称', 'input'),
          buildFallbackCrudField('status', '状态', 'select'),
          buildFallbackCrudField('createTime', '创建时间', 'datetime'),
        ]
    return [
      ...fields.slice(0, 6).map(field => ({
        title: field.props?.__crudConfig?.table?.title || field.label || field.fieldBinding?.fieldCode || field.componentKey,
        key: field.fieldBinding?.fieldCode || field.id,
        width: field.props?.__crudConfig?.table?.width || undefined,
        minWidth: field.props?.__crudConfig?.table?.minWidth || (field.componentKey === 'textarea' ? 180 : 120),
        align: field.props?.__crudConfig?.table?.align || undefined,
        fixed: field.props?.__crudConfig?.table?.fixed || undefined,
        sorter: field.props?.__crudConfig?.table?.sorter || undefined,
        ellipsis: field.props?.__crudConfig?.table?.ellipsis === false ? false : { tooltip: true },
      })),
      {
        title: '操作',
        key: 'actions',
        width: 140,
        fixed: 'right',
        actions: [
          { label: '编辑', key: 'edit', type: 'primary' },
          { label: '删除', key: 'delete', type: 'error' },
        ],
      },
    ]
  })
  const crudApiConfig = computed(() => props.component.props?.apiConfig || buildCrudApiConfig(props.component.props?.apiBase))
  const crudPreviewOptions = computed(() => ({
    showSearch: true,
    showPagination: true,
    searchGridCols: 3,
    searchLabelWidth: 'auto',
    searchEnableCollapse: false,
    searchMaxVisibleFields: 3,
    searchYGap: 10,
    editGridCols: 2,
    editLabelWidth: 'auto',
    editLabelPlacement: 'left',
    editLabelAlign: 'right',
    editSize: 'small',
    editShowFeedback: false,
    editXGap: 12,
    editYGap: 8,
    tableSize: 'medium',
    renderMode: 'table',
    showRenderModeSwitch: false,
    hideToolbar: false,
    hideAdd: false,
    hideBatchDelete: false,
    hideSelection: false,
    striped: false,
    bordered: false,
    showImport: false,
    showExport: false,
    showExportTasks: false,
    loadDetailOnEdit: false,
    pageSize: 5,
    ...(props.component.props?.crudOptions || {}),
  }))

  onMounted(() => {
    syncNodeHeight()
    if (typeof ResizeObserver === 'undefined' || !nodeRef.value)
      return
    mut.resizeObserver = new ResizeObserver(syncNodeHeight)
    mut.resizeObserver.observe(nodeRef.value)
  })

  onBeforeUnmount(() => {
    mut.resizeObserver?.disconnect()
    mut.resizeObserver = null
    cancelPointerDrag()
    cancelNodeResize()
    removeDragImage()
  })

  function syncNodeHeight() {
    if (!nodeRef.value)
      return
    nodeHeight.value = Math.max(44, Math.round(nodeRef.value.offsetHeight || 72))
  }

  function startPointerDrag(event) {
    if (event.button !== 0)
      return
    event.preventDefault()
    mut.activePointerHandle = event.currentTarget
    mut.activePointerId = event.pointerId
    mut.activePointerHandle?.setPointerCapture?.(event.pointerId)
    emit('select', props.component.id)
    dragging.value = true
    mut.pointerDragStarted = true
    mut.activePointerDropTarget = null
    document.documentElement.style.setProperty('--forge-designer-drag-height', `${Math.max(nodeHeight.value, 44)}px`)
    document.documentElement.dataset.forgeDesignerDraggingId = props.component.id
    document.body.classList.add('forge-pointer-dragging')
    setDesignerDragSource(props.component.id)
    clearDesignerDropKey()
    window.dispatchEvent(new CustomEvent('forge-form-designer:canvas-drag-start', {
      detail: { componentId: props.component.id },
    }))
    mountPointerDragImage(event)
    window.addEventListener('pointermove', handlePointerMove, { capture: true, passive: false })
    window.addEventListener('pointerup', finishPointerDrag, true)
    window.addEventListener('pointercancel', cancelPointerDrag, true)
  }

  function cleanupPointerDrag() {
    dragging.value = false
    mut.pointerDragStarted = false
    mut.activePointerDropTarget = null
    activeDropPosition.value = ''
    document.documentElement.style.removeProperty('--forge-designer-drag-height')
    delete document.documentElement.dataset.forgeDesignerDraggingId
    document.body.classList.remove('forge-pointer-dragging')
    try {
      mut.activePointerHandle?.releasePointerCapture?.(mut.activePointerId)
    }
    catch {
      // Pointer capture can already be released by the browser after cancel/up.
    }
    mut.activePointerHandle = null
    mut.activePointerId = null
    clearDesignerDragSource()
    clearDesignerDropKey()
    removeDragImage()
    window.removeEventListener('pointermove', handlePointerMove, true)
    window.removeEventListener('pointerup', finishPointerDrag, true)
    window.removeEventListener('pointercancel', cancelPointerDrag, true)
  }

  function handlePointerMove(event) {
    if (!mut.pointerDragStarted)
      return
    event.preventDefault()
    updateDragImagePosition(event)
    updatePointerDropPreview(event)
  }

  function finishPointerDrag(event) {
    if (!mut.pointerDragStarted)
      return
    event.preventDefault()
    const target = mut.activePointerDropTarget
    cleanupPointerDrag()
    if (!target)
      return
    emit('select', props.component.id)
    emit('update:schema', moveDesignerComponent(props.schema, props.component.id, target))
  }

  function cancelPointerDrag() {
    cleanupPointerDrag()
  }

  function startNodeResize(direction, event) {
    if (event.button !== 0 || !nodeRef.value)
      return
    event.preventDefault()
    emit('select', props.component.id)
    const rect = nodeRef.value.getBoundingClientRect()
    mut.activeResizeHandle = event.currentTarget
    mut.activeResizePointerId = event.pointerId
    mut.activeResizeHandle?.setPointerCapture?.(event.pointerId)
    mut.activeResizeState = {
      direction,
      startX: event.clientX,
      startY: event.clientY,
      startWidth: rect.width,
      startHeight: rect.height,
      startSpan: nodeSpan.value,
      columnWidth: Math.max(24, rect.width / Math.max(1, nodeSpan.value)),
      designerStyle: { ...(props.component.props?.__designerStyle || {}) },
    }
    resizing.value = true
    window.addEventListener('pointermove', handleNodeResizeMove, { capture: true, passive: false })
    window.addEventListener('pointerup', finishNodeResize, true)
    window.addEventListener('pointercancel', cancelNodeResize, true)
  }

  function handleNodeResizeMove(event) {
    // 必须读 mut.activeResizeState（拆 part 后局部 let 已迁入 mut）；
    // HMR 若仍报 activeResizeState is not defined，请硬刷新丢弃旧 pointermove 监听。
    const resizeState = mut.activeResizeState
    if (!resizeState)
      return
    event.preventDefault()
    const deltaX = event.clientX - resizeState.startX
    const deltaY = event.clientY - resizeState.startY
    const direction = resizeState.direction || ''
    const patch = {}
    const nextDesignerStyle = { ...(resizeState.designerStyle || {}) }

    if (direction.includes('e') || direction.includes('w')) {
      const horizontalDelta = deltaX * (direction.includes('w') ? -1 : 1)
      if (isSpanResizable()) {
        const nextSpan = clamp(
          resizeState.startSpan + Math.round(horizontalDelta / resizeState.columnWidth),
          1,
          parentGridColumns.value,
        )
        patch.layout = { span: nextSpan }
        if (props.component.componentKey === 'col')
          patch.props = { span: nextSpan }
      }
      else {
        nextDesignerStyle.width = `${Math.round(Math.max(96, resizeState.startWidth + horizontalDelta))}px`
        nextDesignerStyle.widthMode = 'fixed'
      }
    }

    if (direction.includes('s') || direction.includes('n')) {
      const verticalDelta = deltaY * (direction.includes('n') ? -1 : 1)
      nextDesignerStyle.minHeight = `${Math.round(Math.max(44, resizeState.startHeight + verticalDelta))}px`
      nextDesignerStyle.heightMode = 'custom'
    }

    patch.props = {
      ...(patch.props || {}),
      __designerStyle: nextDesignerStyle,
    }
    emit('update:schema', updateDesignerComponent(props.schema, props.component.id, patch))
  }

  function finishNodeResize() {
    cleanupNodeResize()
  }

  function cancelNodeResize() {
    cleanupNodeResize()
  }

  function cleanupNodeResize() {
    resizing.value = false
    mut.activeResizeState = null
    try {
      mut.activeResizeHandle?.releasePointerCapture?.(mut.activeResizePointerId)
    }
    catch {
      // Pointer capture can already be released by the browser after cancel/up.
    }
    mut.activeResizeHandle = null
    mut.activeResizePointerId = null
    window.removeEventListener('pointermove', handleNodeResizeMove, true)
    window.removeEventListener('pointerup', finishNodeResize, true)
    window.removeEventListener('pointercancel', cancelNodeResize, true)
  }

  function isSpanResizable() {
    return props.depth === 0 || props.component.componentKey === 'col'
  }

  function resizeAnchorTitle(direction) {
    if (direction === 'n' || direction === 's')
      return '调整高度'
    if (direction === 'e' || direction === 'w')
      return '调整宽度'
    return '调整宽高'
  }

  function handleBeforeDrop(event) {
    clearDropPreview()
    emit('dropBefore', event)
  }

  function handleAfterDrop(event) {
    clearDropPreview()
    emit('dropAfter', event)
  }

  function handleNodeDragOver(event) {
    if (!hasForgeDragType(event))
      return
    const target = event.target
    const insideDropArea = target?.closest?.('.layout-children, .crud-empty-drop, .tab-pane-drop-zone')
    if (insideDropArea && nodeRef.value?.contains?.(insideDropArea)) {
      handleInsideDragOver(event)
      return
    }
    if (designerDragSourceId.value === props.component.id || document.documentElement.dataset.forgeDesignerDraggingId === props.component.id) {
      clearDropPreview()
      showInvalidDrop('不能拖入组件自身')
      return
    }
    const rect = nodeRef.value?.getBoundingClientRect()
    if (!rect)
      return
    const ratio = rect.height > 0 ? (event.clientY - rect.top) / rect.height : 0.5
    const canDropInside = canPreviewDropIntoCurrentNode(event)

    if (canDropInside && ratio > 0.28 && ratio < 0.72) {
      clearDesignerDropError()
      activeDropPosition.value = 'inside'
      setDesignerDropKey(insideDropKey.value)
      return
    }

    clearDesignerDropError()
    const nextPosition = resolveDropPosition(ratio)
    activeDropPosition.value = nextPosition
    setDesignerDropKey(nextPosition === 'before' ? beforeDropKey.value : afterDropKey.value)
  }

  function handleInsideDragOver(event) {
    if (!hasForgeDragType(event))
      return
    if (designerDragSourceId.value === props.component.id || document.documentElement.dataset.forgeDesignerDraggingId === props.component.id) {
      clearDropPreview()
      showInvalidDrop('不能拖入组件自身')
      return
    }
    const slotTarget = resolveContainerSlotTarget(event)
    if (slotTarget) {
      clearDesignerDropError()
      activeDropPosition.value = 'inside'
      event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'copy' ? 'copy' : 'move'
      setDesignerDropKey(slotTarget.dropKey)
      return
    }
    if (!canDropIntoCurrentNode(event)) {
      showInvalidDrop(resolveInvalidDropMessage(event))
      return
    }
    clearDesignerDropError()
    activeDropPosition.value = 'inside'
    event.dataTransfer.dropEffect = event.dataTransfer.effectAllowed === 'copy' ? 'copy' : 'move'
    setDesignerDropKey(insideDropKey.value)
  }

  function handleNodeDragLeave() {
    // Drag events fire leave/enter repeatedly for form controls inside the node.
    // Keep the latest preview until the next dragover/drop/dragend to avoid flicker.
  }

  function handleNodeDrop(event) {
    if (beforeActive.value) {
      handleBeforeDrop(event)
      return
    }
    if (afterActive.value) {
      handleAfterDrop(event)
      return
    }
    handleInsideDrop(event)
  }

  function handleInsideDrop(event) {
    const slotTarget = resolveContainerSlotTarget(event)
    if (slotTarget) {
      handleDropToTarget(slotTarget, event)
      return
    }
    if (isGridRow.value) {
      handleRowDropToColumn(0, event)
      return
    }
    if (isTableLayout.value) {
      handleTableDropToCell(0, event)
      return
    }
    if (isTabsLayout.value) {
      showInvalidDrop('请拖入具体标签页内容区')
      return
    }
    if (!canDropIntoCurrentNode(event)) {
      showInvalidDrop(resolveInvalidDropMessage(event))
      return
    }
    handleDropToChildren((props.component.children || []).length, event)
  }

  function handleChildDrop(index, event) {
    if (isGridRow.value) {
      handleRowDropToColumn(index, event)
      return
    }
    if (isTableLayout.value) {
      const child = resolveDraggedComponent(event)
      if (!['tableGrid', 'fcTableGrid'].includes(child?.componentKey)) {
        handleTableDropToCell(index, event)
        return
      }
    }
    handleDropToChildren(index, event)
  }

  function handleDropToChildren(index, event) {
    handleDropToTarget({ parentId: props.component.id, index }, event)
  }

  function handleDropToTarget(target, event) {
    clearDropPreview()
    clearDesignerDropError()
    const sourceId = event.dataTransfer.getData(DRAG_COMPONENT_MIME)
    if (sourceId) {
      emit('select', sourceId)
      emit('update:schema', moveDesignerComponent(props.schema, sourceId, target))
      return
    }

    const fieldText = event.dataTransfer.getData(DRAG_FIELD_MIME)
    if (fieldText) {
      const field = parsePayload(fieldText)
      const component = createComponentFromField(field, target.index)
      emit('select', component.id)
      emit('update:schema', insertDesignerComponent(props.schema, target, component))
      return
    }

    const layoutText = event.dataTransfer.getData(DRAG_LAYOUT_MIME)
    if (layoutText) {
      const layout = parsePayload(layoutText)
      const component = createForgeLayoutComponent(layout.componentKey || 'card', props.schema)
      emit('select', component.id)
      emit('update:schema', insertDesignerComponent(props.schema, target, component))
      return
    }

    const templateText = event.dataTransfer.getData(DRAG_TEMPLATE_MIME)
    if (templateText) {
      const template = parsePayload(templateText)
      const component = createForgeFieldTemplateComponent(template, props.schema)
      emit('select', component.id)
      emit('update:schema', insertDesignerComponent(props.schema, target, component))
    }
  }

  function handleRowDropToColumn(index, event) {
    const child = resolveDraggedComponent(event)
    if (child?.componentKey === 'col') {
      handleDropToChildren(index, event)
      return
    }
    const columns = (props.component.children || []).filter(item => item?.componentKey === 'col')
    const targetColumn = columns[Math.max(0, Math.min(columns.length - 1, Number(index) || 0))]
    if (!targetColumn) {
      showInvalidDrop('请先添加栅格列')
      return
    }
    if (!canAcceptDesignerChild(targetColumn, child)) {
      showInvalidDrop('该格子不支持放入这个组件')
      return
    }
    handleDropToTarget({
      parentId: targetColumn.id,
      index: targetColumn.children?.length || 0,
    }, event)
  }

  function handleTableDropToCell(index, event) {
    const child = resolveDraggedComponent(event)
    if (['tableGrid', 'fcTableGrid'].includes(child?.componentKey)) {
      handleDropToChildren(index, event)
      return
    }
    const cells = (props.component.children || []).filter(item => ['tableGrid', 'fcTableGrid'].includes(item?.componentKey))
    const targetCell = cells[Math.max(0, Math.min(cells.length - 1, Number(index) || 0))]
    if (!targetCell) {
      showInvalidDrop('请先添加表格单元格')
      return
    }
    if (!canAcceptDesignerChild(targetCell, child)) {
      showInvalidDrop('该单元格不支持放入这个组件')
      return
    }
    handleDropToTarget({
      parentId: targetCell.id,
      index: targetCell.children?.length || 0,
    }, event)
  }

  function canDropIntoCurrentNode(event) {
    if (isField.value || isTitle.value)
      return false
    const child = resolveDraggedComponent(event)
    if (isGridRow.value)
      return child?.componentKey === 'col' || resolveContainerSlotTarget(event, child)?.parentId
    if (isTableLayout.value)
      return ['tableGrid', 'fcTableGrid'].includes(child?.componentKey) || Boolean(resolveContainerSlotTarget(event, child)?.parentId)
    if (isTabsLayout.value) {
      return Boolean((props.component.children || []).length)
        && canAcceptDesignerChild((props.component.children || [])[0], child)
    }
    return canAcceptDesignerChild(props.component, child)
  }

  function canPreviewDropIntoCurrentNode(event) {
    if (!hasForgeDragType(event) || isField.value || isTitle.value || isTableLayout.value)
      return false
    if (isGridRow.value) {
      const child = resolveDraggedComponent(event)
      return child?.componentKey !== 'col' && (props.component.children || []).some(item => item?.componentKey === 'col')
    }
    const parentKey = props.component.componentKey || ''
    if (['tabs', 'elTabs', 'collapse', 'elCollapse'].includes(parentKey))
      return false
    return true
  }

  function resolveDraggedComponent(event) {
    const sourceId = event.dataTransfer.getData(DRAG_COMPONENT_MIME)
    if (sourceId)
      return getDesignerComponent(props.schema, sourceId)

    if (designerDragPreviewComponent.value)
      return designerDragPreviewComponent.value

    const fieldText = event.dataTransfer.getData(DRAG_FIELD_MIME)
    if (fieldText)
      return createComponentFromField(parsePayload(fieldText), 0)

    const layoutText = event.dataTransfer.getData(DRAG_LAYOUT_MIME)
    if (layoutText) {
      const layout = parsePayload(layoutText)
      return createForgeLayoutComponent(layout.componentKey || 'card', props.schema)
    }

    const templateText = event.dataTransfer.getData(DRAG_TEMPLATE_MIME)
    if (templateText)
      return createForgeFieldTemplateComponent(parsePayload(templateText), props.schema)
    return null
  }

  function resolveContainerSlotTarget(event, draggedComponent = resolveDraggedComponent(event), containerComponent = props.component) {
    if (!draggedComponent)
      return null
    const containerKey = containerComponent?.componentKey || ''
    const childKey = draggedComponent.componentKey || ''
    if (['row', 'fcRow'].includes(containerKey) && childKey === 'col') {
      const index = resolveLayoutSlotIndex(event, containerComponent.children || [])
      return {
        parentId: containerComponent.id,
        index,
        dropKey: `${containerComponent.id}:inside`,
      }
    }
    if (['table', 'fcTable'].includes(containerKey) && ['tableGrid', 'fcTableGrid'].includes(childKey)) {
      const index = resolveLayoutSlotIndex(event, containerComponent.children || [])
      return {
        parentId: containerComponent.id,
        index,
        dropKey: `${containerComponent.id}:inside`,
      }
    }
    let slotKeys = []
    if (['row', 'fcRow'].includes(containerKey))
      slotKeys = ['col']
    else if (['table', 'fcTable'].includes(containerKey))
      slotKeys = ['tableGrid', 'fcTableGrid']
    if (!slotKeys.length)
      return null
    const slots = (containerComponent.children || []).filter(item => slotKeys.includes(item?.componentKey))
    const targetSlot = resolveLayoutSlotFromEventTarget(event, slots) || resolveLayoutSlotAtPoint(event, slots)
    if (!targetSlot || !canAcceptDesignerChild(targetSlot, draggedComponent))
      return null
    return {
      parentId: targetSlot.id,
      index: targetSlot.children?.length || 0,
      dropKey: `${targetSlot.id}:inside`,
    }
  }

  function resolveLayoutSlotFromEventTarget(event, slots = []) {
    if (!slots.length)
      return null
    const slotIds = new Set(slots.map(slot => slot?.id).filter(Boolean))
    let current = event.target?.closest?.('[data-forge-node-id]')
    while (current && nodeRef.value?.contains?.(current)) {
      const nodeId = current.dataset?.forgeNodeId
      if (slotIds.has(nodeId))
        return slots.find(slot => slot?.id === nodeId) || null
      current = current.parentElement?.closest?.('[data-forge-node-id]')
    }
    return null
  }

  function resolveLayoutSlotAtPoint(event, slots = []) {
    if (!slots.length)
      return null
    const rects = slots
      .map((slot, index) => {
        const rect = findNodeWrapRect(slot.id)
        return rect ? { slot, index, rect } : null
      })
      .filter(Boolean)
    if (!rects.length)
      return slots[0]
    const matched = rects.find(({ rect }) =>
      event.clientX >= rect.left
      && event.clientX <= rect.right
      && event.clientY >= rect.top
      && event.clientY <= rect.bottom)
    if (matched)
      return matched.slot
    const sortedRects = rects
      .map((entry) => {
        const centerX = entry.rect.left + entry.rect.width / 2
        const centerY = entry.rect.top + entry.rect.height / 2
        return {
          ...entry,
          distance: Math.abs(event.clientX - centerX) + Math.abs(event.clientY - centerY),
        }
      })
      .sort((a, b) => a.distance - b.distance)
    const nearest = sortedRects[0]
    return nearest?.slot || slots[0]
  }

  function resolveLayoutSlotIndex(event, slots = []) {
    const targetSlot = resolveLayoutSlotAtPoint(event, slots)
    const index = slots.findIndex(item => item?.id === targetSlot?.id)
    return index === -1 ? slots.length : index + 1
  }

  function findNodeWrapRect(componentId = '') {
    if (!componentId)
      return null
    const wraps = Array.from(document.querySelectorAll('[data-forge-node-id]'))
    const wrap = wraps.find(item => item?.dataset?.forgeNodeId === componentId)
    return wrap?.getBoundingClientRect?.() || null
  }

  function hasForgeDragType(event) {
    const types = Array.from(event.dataTransfer?.types || [])
    return FORGE_DRAG_TYPES.some(type => types.includes(type))
  }

  function clearDropPreview() {
    clearDesignerDropKey()
    activeDropPosition.value = ''
  }

  function resolveDropPosition(ratio) {
    return ratio <= 0.5 ? 'before' : 'after'
  }

  function updatePointerDropPreview(event) {
    const hoveredElement = document.elementFromPoint(event.clientX, event.clientY)
    if (!hoveredElement) {
      clearPointerDropPreview(event)
      return
    }

    const rootZone = hoveredElement.closest?.('.root-drop-zone')
    if (rootZone) {
      activeDropPosition.value = 'before'
      mut.activePointerDropTarget = { parentId: '', index: 0 }
      setDesignerDropKey('root:before')
      syncDragImageFitScale(rootZone.getBoundingClientRect?.()?.width || 0, event)
      return
    }

    const targetWrap = hoveredElement.closest?.('[data-forge-node-id]')
    if (!targetWrap) {
      clearPointerDropPreview(event)
      return
    }

    const targetId = targetWrap.dataset.forgeNodeId || ''
    if (!targetId || targetId === props.component.id) {
      showInvalidDrop('不能拖入组件自身')
      clearPointerDropPreview(event)
      return
    }

    if (isDescendantNodeWrap(targetWrap)) {
      showInvalidDrop('不能拖入自己的子组件')
      clearPointerDropPreview(event)
      return
    }

    const targetNode = targetWrap.querySelector?.('.canvas-node')
    const rect = targetNode?.getBoundingClientRect?.()
    if (!rect) {
      clearPointerDropPreview(event)
      return
    }

    const targetComponent = getDesignerComponent(props.schema, targetId)
    const ratio = rect.height > 0 ? (event.clientY - rect.top) / rect.height : 0.5
    const slotTarget = resolveContainerSlotTarget(event, props.component, targetComponent)
    if (slotTarget) {
      clearDesignerDropError()
      activeDropPosition.value = 'inside'
      mut.activePointerDropTarget = {
        parentId: slotTarget.parentId,
        index: slotTarget.index,
      }
      setDesignerDropKey(slotTarget.dropKey)
      // 放入容器槽位（如栅格格子）：影子贴合槽位容器宽度，避免视觉超出栅格
      syncDragImageFitScale(findNodeWrapRect(slotTarget.parentId)?.width || rect.width, event)
      return
    }
    if (canPointerDropInside(targetComponent, ratio)) {
      clearDesignerDropError()
      activeDropPosition.value = 'inside'
      mut.activePointerDropTarget = { parentId: targetId, index: targetComponent?.children?.length || 0 }
      setDesignerDropKey(`${targetId}:inside`)
      syncDragImageFitScale(rect.width, event)
      return
    }

    const targetParentId = targetWrap.dataset.forgeParentId || ''
    const parentComponent = targetParentId ? getDesignerComponent(props.schema, targetParentId) : null
    const targetIndex = Number(targetWrap.dataset.forgeIndex || 0)
    const rowColumnTarget = resolvePointerRowColumnTarget(parentComponent, targetIndex)
    if (rowColumnTarget) {
      clearDesignerDropError()
      activeDropPosition.value = 'inside'
      mut.activePointerDropTarget = rowColumnTarget
      setDesignerDropKey(`${rowColumnTarget.parentId}:inside`)
      // 悬停在栅格行上：影子贴合将落入的格子宽度
      syncDragImageFitScale(findNodeWrapRect(rowColumnTarget.parentId)?.width || rect.width, event)
      return
    }
    if (!canAcceptDesignerChild(parentComponent, props.component)) {
      showInvalidDrop(resolveInvalidPointerDropMessage(parentComponent))
      clearPointerDropPreview(event)
      return
    }

    const position = resolveDropPosition(ratio)
    activeDropPosition.value = position
    mut.activePointerDropTarget = {
      parentId: targetParentId,
      index: targetIndex + (position === 'after' ? 1 : 0),
    }
    clearDesignerDropError()
    setDesignerDropKey(`${targetId}:${position}`)
    // 与悬停目标同级插入：影子贴合目标宽度；目标在栅格内时即贴合格子内容宽度
    syncDragImageFitScale(rect.width, event)
  }

  function clearPointerDropPreview(event = null) {
    mut.activePointerDropTarget = null
    clearDropPreview()
    // 清除放置预览时影子不再贴合任何目标，恢复为仅受全局上限约束
    syncDragImageFitScale(0, event)
  }

  function isDescendantNodeWrap(targetWrap) {
    let current = targetWrap.parentElement
    while (current) {
      if (current.dataset?.forgeNodeId === props.component.id)
        return true
      current = current.parentElement
    }
    return false
  }

  function canPointerDropInside(targetComponent, ratio) {
    if (!targetComponent || ratio <= 0.28 || ratio >= 0.72)
      return false
    const targetKey = targetComponent.componentKey || ''
    if (isFieldComponent(targetComponent) || ['title', 'fcTitle', 'divider', 'elDivider'].includes(targetKey))
      return false
    if (['row', 'fcRow', 'table', 'fcTable', 'tabs', 'elTabs', 'collapse', 'elCollapse'].includes(targetKey))
      return false
    return canAcceptDesignerChild(targetComponent, props.component)
  }

  function resolvePointerRowColumnTarget(parentComponent, targetIndex = 0) {
    if (!['row', 'fcRow'].includes(parentComponent?.componentKey))
      return null
    if (props.component.componentKey === 'col')
      return null
    const columns = (parentComponent.children || []).filter(item => item?.componentKey === 'col')
    const targetColumn = columns[Math.max(0, Math.min(columns.length - 1, Number(targetIndex) || 0))]
    if (!targetColumn || !canAcceptDesignerChild(targetColumn, props.component))
      return null
    return {
      parentId: targetColumn.id,
      index: targetColumn.children?.length || 0,
    }
  }

  function showInvalidDrop(message = '当前位置不能放置该组件') {
    setDesignerDropError(message)
  }

  function resolveInvalidDropMessage(event) {
    if (isField.value)
      return '字段组件不能作为容器'
    if (isTitle.value)
      return '标题组件不能作为容器'
    if (isGridRow.value)
      return '请拖到具体格子里'
    const child = resolveDraggedComponent(event)
    if (!canAcceptDesignerChild(props.component, child))
      return '该容器不支持放入这个组件'
    return '当前位置不能放置该组件'
  }

  function resolveInvalidPointerDropMessage(parentComponent) {
    if (!parentComponent)
      return '当前位置不能放置该组件'
    const key = parentComponent.componentKey || ''
    if (['row', 'fcRow'].includes(key))
      return '请拖到具体格子里'
    if (['table', 'fcTable'].includes(key))
      return '表格布局只能拖入表格单元格'
    return '该容器不支持放入这个组件'
  }

  __impl.selectNode = selectNode
  __impl.syncNodeHeight = syncNodeHeight
  __impl.startPointerDrag = startPointerDrag
  __impl.cleanupPointerDrag = cleanupPointerDrag
  __impl.handlePointerMove = handlePointerMove
  __impl.finishPointerDrag = finishPointerDrag
  __impl.cancelPointerDrag = cancelPointerDrag
  __impl.startNodeResize = startNodeResize
  __impl.handleNodeResizeMove = handleNodeResizeMove
  __impl.finishNodeResize = finishNodeResize
  __impl.cancelNodeResize = cancelNodeResize
  __impl.cleanupNodeResize = cleanupNodeResize
  __impl.isSpanResizable = isSpanResizable
  __impl.resizeAnchorTitle = resizeAnchorTitle
  __impl.handleBeforeDrop = handleBeforeDrop
  __impl.handleAfterDrop = handleAfterDrop
  __impl.handleNodeDragOver = handleNodeDragOver
  __impl.handleInsideDragOver = handleInsideDragOver
  __impl.handleNodeDragLeave = handleNodeDragLeave
  __impl.handleNodeDrop = handleNodeDrop
  __impl.handleInsideDrop = handleInsideDrop
  __impl.handleChildDrop = handleChildDrop
  __impl.handleDropToChildren = handleDropToChildren
  __impl.handleDropToTarget = handleDropToTarget
  __impl.handleRowDropToColumn = handleRowDropToColumn
  __impl.handleTableDropToCell = handleTableDropToCell
  __impl.canDropIntoCurrentNode = canDropIntoCurrentNode
  __impl.canPreviewDropIntoCurrentNode = canPreviewDropIntoCurrentNode
  __impl.resolveDraggedComponent = resolveDraggedComponent
  __impl.resolveContainerSlotTarget = resolveContainerSlotTarget
  __impl.resolveLayoutSlotFromEventTarget = resolveLayoutSlotFromEventTarget
  __impl.resolveLayoutSlotAtPoint = resolveLayoutSlotAtPoint
  __impl.resolveLayoutSlotIndex = resolveLayoutSlotIndex
  __impl.findNodeWrapRect = findNodeWrapRect
  __impl.hasForgeDragType = hasForgeDragType
  __impl.clearDropPreview = clearDropPreview
  __impl.resolveDropPosition = resolveDropPosition
  __impl.updatePointerDropPreview = updatePointerDropPreview
  __impl.clearPointerDropPreview = clearPointerDropPreview
  __impl.isDescendantNodeWrap = isDescendantNodeWrap
  __impl.canPointerDropInside = canPointerDropInside
  __impl.resolvePointerRowColumnTarget = resolvePointerRowColumnTarget
  __impl.showInvalidDrop = showInvalidDrop
  __impl.resolveInvalidDropMessage = resolveInvalidDropMessage
  __impl.resolveInvalidPointerDropMessage = resolveInvalidPointerDropMessage

  return {
    props, emit, __impl, mut, applyDragImageFitScale, buildCrudApiConfig, buildFallbackCrudField, buildFormDividerProps,
    buildGroupTitleProps, buildPreviewOptions, buildPreviewPlaceholder, canDropIntoCurrentNode, canPointerDropInside, canPreviewDropIntoCurrentNode, cancelNodeResize, cancelPointerDrag,
    clamp, clampDragOffset, clampDragRatio, cleanupNodeResize, cleanupPointerDrag, clearDropPreview, clearPointerDropPreview, collectDesignerFields,
    duplicateNode, findComponentById, findFieldAsset, findNodeWrapRect, finishNodeResize, finishPointerDrag, firstText, handleAfterDrop,
    handleBeforeDrop, handleChildDrop, handleDropToChildren, handleDropToTarget, handleInsideDragOver, handleInsideDrop, handleNodeDragLeave, handleNodeDragOver,
    handleNodeDrop, handleNodeMenuSelect, handleNodeResizeMove, handlePageWidgetPropsUpdate, handlePointerMove, handleRowDropToColumn, handleTableDropToCell, hasDynamicPreviewOptionSource,
    hasForgeDragType, isCrudRoleEnabled, isDescendantNodeWrap, isFormDividerComponent, isSpanResizable, mergeRelationPreviewProps, mountPointerDragImage, normalizePreviewFieldType,
    parsePayload, removeDragImage, removeGridColumn, removeNode, resizeAnchorTitle, resolveBackgroundStyle, resolveBorderStyle, resolveContainerSlotTarget,
    resolveDraggedComponent, resolveDropPosition, resolveGap, resolveInvalidDropMessage, resolveInvalidPointerDropMessage, resolveLayoutSlotAtPoint, resolveLayoutSlotFromEventTarget, resolveLayoutSlotIndex,
    resolvePointerRowColumnTarget, resolvePreviewFieldProps, resolvePreviewOptions, resolveRowColumns, resolveRuntimeOptionProps, resolveTableColumns, selectNode, shouldUseDictOptions,
    showInvalidDrop, startNodeResize, startPointerDrag, syncDragImageFitScale, syncNodeHeight, toCrudFormField, toggleRequired, updateDragImagePosition,
    updateNodeDesignerStyle, updatePointerDropPreview, DRAG_COMPONENT_MIME, DRAG_FIELD_MIME, DRAG_LAYOUT_MIME, DRAG_TEMPLATE_MIME, FORGE_DRAG_TYPES, MAX_FORM_GRID_COLUMNS,
    resizeDirections, activeDropPosition, dragging, resizing, nodeRef, nodeHeight, previewValue, isSelected,
    isField, isLayout, isTitle, isSubTableComponent, subTableDisplayModeLabel, subTableColumns, subTableConfigured, isButton,
    isCrudBlock, isGridRow, isGridColumn, columnRemovalLocked, columnDeleteTitle, isTableLayout, isCardLayout, isTabsLayout,
    isTabPaneLayout, isCollapseLayout, isPageWidget, isStructuralSlot, selectedDesignerStyle, designerBorderHidden, beforeDropKey, afterDropKey,
    insideDropKey, beforeActive, afterActive, activeInside, displayLabel, previewContext, previewFormData, rootColumns,
    parentGridColumns, nodeSpan, nodeWrapStyle, dropIndicatorStyle, nodeCustomStyle, nodeMenuOptions, childrenGridStyle, gridRowCells,
    previewField, crudDesignerFields, crudSearchFields, crudTableFields, crudEditFields, crudSearchSchema, crudEditSchema, crudPreviewColumns,
    crudApiConfig, crudPreviewOptions,
  }
}
