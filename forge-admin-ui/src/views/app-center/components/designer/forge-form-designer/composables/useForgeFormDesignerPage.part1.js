/** ForgeFormDesigner.vue setup part 1. */
import { ArrowRedoOutline, ArrowUndoOutline, ChevronBackOutline, ChevronForwardOutline, EllipsisHorizontalOutline, TrashOutline, WarningOutline } from '@vicons/ionicons5'
import { NSpace } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import AiForm from '@/components/ai-form/AiForm.vue'
import AiFormGroupTitle from '@/components/ai-form/AiFormGroupTitle.vue'
import AiFormSectionTitle from '@/components/ai-form/AiFormSectionTitle.vue'
import { normalizeRecordSelectorConfig as normalizeRuntimeRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { buildLegacyLinkageSchema } from '../../form-first/field-linkage-config'
import { repairFormDesignerFieldRefs } from '../../form-first/fieldReferenceUtils'
import { extractForgeSchemaFieldRefs } from '../../form-first/forgeToFormCreate'
import {
  applyGridColumnsToFormDesignerSchema,
  createComponentFromField,
  createDefaultFormDesignerSchema,
  insertDesignerComponent,
  normalizeFormDesignerSchema,
  normalizeFormDesignerSchemaForSave,
  resolveAppendShelfTarget,
} from '../../form-first/formDesignerSchema'
import { createForgeFieldTemplateComponent, createForgeLayoutComponent } from '../designerLayoutFactory'
import BottomBarEditor from '../BottomBarEditor.vue'
import ForgeFieldShelf from '../ForgeFieldShelf.vue'
import ForgeFormCanvas from '../ForgeFormCanvas.vue'
import ForgePropertyPanel from '../ForgePropertyPanel.vue'
import { resolveDesignerCanvasPreviewValue } from '../field-default-value'
import { derivePageSectionsFromLayout } from '../pageSectionDerivation'
import PageSectionEditor from '../PageSectionEditor.vue'
export function applyForgeFormDesignerPagePart1(props, emit, expose) {
  const __impl = {}
  const mut = {}
  const slots = useSlots()

  const selectedId = ref('')
  const leftCollapsed = ref(false)
  // 首屏先出画布，组件架 idle 后再挂，避免 59 个物料同步拖慢进页
  const shelfHydrated = ref(false)
  const rightOpen = ref(true)
  const canvasFocusMode = ref(false)
  const formTabsMenuVisible = ref(false)
  const formTabsMenuX = ref(0)
  const formTabsMenuY = ref(0)
  const undoStack = ref([])
  const redoStack = ref([])
  const HISTORY_LIMIT = 50
  const previewMode = ref('create')
  const clearDialogVisible = ref(false)
  const clearScope = ref('current')
  const bottomBarDialogVisible = ref(false)
  const canvasView = ref(!props.enableSectionsView && props.initialCanvasView === 'sections' ? 'layout' : props.initialCanvasView)
  const previewModeOptions = [
    { label: '新增', value: 'create' },
    { label: '编辑', value: 'edit' },
    { label: '详情', value: 'detail' },
  ]
  const previewRuntimeContext = {
    mode: 'designer-preview',
    designerPreview: true,
    source: 'form-designer',
    // 预览弹窗允许拉业务对象选项，否则「选中后回填」无选项行可映射
    allowOptionSourceFetch: true,
  }
  const baseDesignerMoreOptions = [
    { label: '按字段生成', key: 'resetFromFields' },
    { label: '清理失效字段', key: 'repairRefs' },
    { label: '底部操作栏', key: 'configureBottomBar' },
    { label: '打印表单', key: 'printDesign' },
  ]

  const normalizedSchema = computed(() => normalizeFormDesignerSchema(props.modelValue || createDefaultFormDesignerSchema({
    objectCode: props.objectCode,
    objectName: props.objectName,
    fields: props.fields,
  })))
  const previewLayout = computed(() => normalizedSchema.value.layout || {})
  // 预览列数与设计器画布（ForgeFormCanvasNode.rootColumns）完全同源：默认 2 列、clamp 1-24，
  // 避免「设计默认 2 列 / 预览兕底 1 列」导致设计/预览不一致
  const previewGridCols = computed(() => {
    const raw = Number(previewLayout.value.gridCols || previewLayout.value.gridColumns) || 2
    return Math.max(1, Math.min(24, raw))
  })
  const previewSchema = computed(() => buildRuntimeFormSchema(normalizedSchema.value, previewMode.value))
  const previewValue = computed(() => buildRuntimeFormValue(normalizedSchema.value, previewMode.value))
  // 预览表单用本地可写状态，否则受控 :value 始终是默认快照会把「选中后回填」冲掉
  const previewFormValue = ref({})
  const usedFieldSet = computed(() => new Set(extractForgeSchemaFieldRefs(normalizedSchema.value || {})))
  const formAssets = computed(() => Array.isArray(normalizedSchema.value.settings?.formAssets) ? normalizedSchema.value.settings.formAssets : [])
  const designerMoreOptions = computed(() => [
    ...baseDesignerMoreOptions,
    ...(props.extraMoreOptions || []),
  ])
  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)
  const canClearCanvas = computed(() => {
    if (containsLockedField(normalizedSchema.value.components)
      || formAssets.value.some(asset => containsLockedField(asset?.schema?.components))) {
      return false
    }
    if (normalizedSchema.value.components?.length)
      return true
    return formAssets.value.some(asset => asset?.schema?.components?.length)
  })
  const componentCount = computed(() => countComponents(normalizedSchema.value.components))
  const hasDetailSettings = computed(() => Boolean(slots['detail-settings']))
  const pageSectionProtocol = computed(() => ({
    pageSections: normalizedSchema.value.pageSections || [],
    bottomBar: normalizedSchema.value.bottomBar || {},
  }))
  const canvasViewTitle = computed(() => ({
    layout: '表单页设计',
    sections: '页面分区',
    detail: '详情设置',
  }[canvasView.value] || '表单页设计'))
  const canvasViewIcon = computed(() => ({ layout: 'F', sections: 'S', detail: 'D' }[canvasView.value] || 'F'))
  const canvasViewDescription = computed(() => {
    if (canvasView.value === 'sections')
      return `${pageSectionProtocol.value.pageSections.length} 个分区 · 与表单草稿同步保存`
    if (canvasView.value === 'detail')
      return '关系页签、日志和数量区块'
    return `${normalizedSchema.value.formName || '当前表单'} · ${componentCount.value} 个组件`
  })
  const previewModeTitle = computed(() => previewModeOptions.find(item => item.value === previewMode.value)?.label || '新增')
  const previewModeDescription = computed(() => {
    const modeMap = {
      create: '空表单状态，按默认值初始化。',
      edit: '模拟详情接口返回后进入编辑。',
      detail: '模拟详情接口返回，并按只读态展示。',
    }
    return modeMap[previewMode.value] || ''
  })
  const formTabsMenuOptions = [
    { label: '＋ 新建空白表单', key: 'create' },
    { label: '⧉ 复制当前表单', key: 'duplicate' },
  ]
  const canvasMetaText = computed(() => {
    const fieldCount = usedFieldSet.value.size
    return `${fieldCount} 个业务字段 · ${componentCount.value} 个画布节点 · ${normalizedSchema.value.layout?.gridColumns || 2} 列`
  })

  function updateSchema(schema, options = {}) {
    let nextSchema = normalizeFormDesignerSchema(schema || {})
    // 分区由布局承载时，pageSections 不再独立编辑，随画布结构派生写回。
    if (props.deriveSectionsFromLayout)
      nextSchema = { ...nextSchema, pageSections: derivePageSectionsFromLayout(nextSchema.components, nextSchema.pageSections || []) }
    const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
    if (isSameDesignerSchema(nextSchema, currentSchema))
      return
    if (options.recordHistory !== false) {
      pushHistorySnapshot(currentSchema)
      redoStack.value = []
    }
    emit('update:modelValue', nextSchema)
    if (Array.isArray(nextSchema.settings?.governance?.fieldLinkages)) {
      emit('update:linkageSchema', buildLegacyLinkageSchema(nextSchema, props.linkageSchema || {}))
    }
    emit('dirtyChange', true)
  }

  function pushHistorySnapshot(schema) {
    undoStack.value = [...undoStack.value, cloneDesignerSchema(schema)].slice(-HISTORY_LIMIT)
  }

  function undoSchema() {
    if (!canUndo.value)
      return
    const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
    const previousSchema = undoStack.value[undoStack.value.length - 1]
    undoStack.value = undoStack.value.slice(0, -1)
    redoStack.value = [cloneDesignerSchema(currentSchema), ...redoStack.value].slice(0, HISTORY_LIMIT)
    selectedId.value = ''
    updateSchema(previousSchema, { recordHistory: false })
  }

  function redoSchema() {
    if (!canRedo.value)
      return
    const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value || {})
    const nextSchema = redoStack.value[0]
    redoStack.value = redoStack.value.slice(1)
    pushHistorySnapshot(currentSchema)
    selectedId.value = ''
    updateSchema(nextSchema, { recordHistory: false })
  }

  function handleDesignerShortcut(event) {
    const isUndoKey = (event.metaKey || event.ctrlKey) && !event.shiftKey && event.key?.toLowerCase?.() === 'z'
    const isRedoKey = (event.metaKey || event.ctrlKey) && ((event.shiftKey && event.key?.toLowerCase?.() === 'z') || event.key?.toLowerCase?.() === 'y')
    if (!isUndoKey && !isRedoKey)
      return
    const target = event.target
    if (target?.closest?.('input, textarea, [contenteditable="true"]'))
      return
    event.preventDefault()
    if (isRedoKey)
      redoSchema()
    else
      undoSchema()
  }

  function cloneDesignerSchema(value) {
    return JSON.parse(JSON.stringify(value || {}))
  }

  function isSameDesignerSchema(left, right) {
    return JSON.stringify(left || {}) === JSON.stringify(right || {})
  }

  function appendField(field = {}) {
    const fieldCode = field.fieldCode || field.field
    if (!fieldCode || usedFieldSet.value.has(fieldCode))
      return
    const schema = normalizeFormDesignerSchema(normalizedSchema.value)
    const component = createComponentFromField(field, schema.components.length)
    schema.components.push(component)
    selectedId.value = component.id
    rightOpen.value = true
    updateSchema(applyGridColumnsToFormDesignerSchema(schema, schema.layout?.gridColumns || 2))
  }

  /** 左侧组件库点击添加：优先落入当前选中容器，否则追加到画布根部 */
  function appendShelfItem(item = {}) {
    const componentKey = item.componentKey || 'input'
    const schema = normalizeFormDesignerSchema(normalizedSchema.value)
    const component = item.kind === 'layout'
      ? createForgeLayoutComponent(componentKey, schema)
      : createForgeFieldTemplateComponent({ componentKey, label: item.label }, schema)
    const target = resolveAppendShelfTarget(schema, selectedId.value, component)
    const nextSchema = insertDesignerComponent(schema, target, component)
    selectedId.value = component.id
    rightOpen.value = true
    updateSchema(applyGridColumnsToFormDesignerSchema(nextSchema, nextSchema.layout?.gridColumns || 2))
  }

  function resetFromFields() {
    const nextSchema = createDefaultFormDesignerSchema({
      objectCode: props.objectCode,
      objectName: props.objectName,
      fields: props.fields,
      gridColumns: normalizedSchema.value.layout?.gridColumns || 2,
    })
    selectedId.value = ''
    updateSchema({
      ...nextSchema,
      formKey: normalizedSchema.value.formKey,
      formName: normalizedSchema.value.formName,
      settings: normalizedSchema.value.settings,
      layout: {
        ...(nextSchema.layout || {}),
        ...(normalizedSchema.value.layout || {}),
      },
    })
  }

  function openClearCanvasDialog() {
    clearScope.value = 'current'
    clearDialogVisible.value = true
  }

  function updatePageSectionProtocol(protocol) {
    if (!protocol || typeof protocol !== 'object')
      return
    updateSchema({
      ...normalizedSchema.value,
      pageSections: protocol.pageSections,
      bottomBar: protocol.bottomBar,
    })
  }

  // 底部操作栏弹窗即时写回：与分区编辑一致走 updateSchema，保留撤销历史与脏标记。
  function updateBottomBarFromDialog(bottomBar) {
    updateSchema({
      ...normalizedSchema.value,
      bottomBar,
    })
  }

  function handleDesignerMoreSelect(key = '') {
    if (key === 'configureBottomBar') {
      bottomBarDialogVisible.value = true
      return
    }
    if (key === 'resetFromFields') {
      resetFromFields()
      return
    }
    if (key === 'printDesign') {
      openPrintPreview()
      return
    }
    if (key === 'repairRefs')
      repairRefs()
    else
      emit('moreSelect', key)
  }

  /** 打开预览并触发浏览器打印，像 Word 一样输出当前表单设计稿 */
  function openPrintPreview() {
    previewDialogVisible.value = true
    nextTick(() => {
      setTimeout(() => window.print(), 400)
    })
  }

  function confirmClearCanvas() {
    const schema = normalizeFormDesignerSchema(normalizedSchema.value)
    selectedId.value = ''
    clearDialogVisible.value = false

    if (clearScope.value === 'all') {
      updateSchema({
        ...schema,
        components: [],
        settings: {
          ...(schema.settings || {}),
          formAssets: formAssets.value.map(asset => ({
            ...asset,
            schema: asset.schema
              ? {
                  ...asset.schema,
                  components: [],
                }
              : asset.schema,
          })),
        },
      })
      return
    }

    updateSchema({
      ...schema,
      components: [],
    })
  }

  function repairRefs() {
    const schema = repairFormDesignerFieldRefs(normalizedSchema.value, props.fields, 'mark')
    updateSchema(schema)
  }

  function switchFormAsset(formKey = '') {
    const asset = formAssets.value.find(item => item.formKey === formKey)
    if (!asset?.schema)
      return
    const currentAsset = {
      formKey: normalizedSchema.value.formKey,
      formName: normalizedSchema.value.formName,
      schema: {
        ...normalizeFormDesignerSchema(normalizedSchema.value),
        settings: {
          ...(normalizedSchema.value.settings || {}),
          formAssets: [],
        },
      },
    }
    const nextAssets = formAssets.value
      .filter(item => item.formKey !== formKey && item.formKey !== currentAsset.formKey)
      .concat(currentAsset)
    selectedId.value = ''
    updateSchema(normalizeFormDesignerSchema({
      ...asset.schema,
      settings: {
        ...(asset.schema.settings || {}),
        formAssets: nextAssets,
      },
    }))
  }

  function openFormTabsMenu(event) {
    formTabsMenuX.value = event.clientX
    formTabsMenuY.value = event.clientY
    formTabsMenuVisible.value = true
  }

  function handleFormTabsMenuSelect(key) {
    formTabsMenuVisible.value = false
    if (key === 'create') {
      createBlankFormAsset()
      return
    }
    if (key === 'duplicate')
      duplicateCurrentFormAsset()
  }

  function createBlankFormAsset() {
    const nextAssetIndex = formAssets.value.length + 2
    const nextAssetKey = `${normalizedSchema.value.formKey || 'form'}_form_${Date.now()}`
    const assetSchema = normalizeFormDesignerSchema({
      ...normalizedSchema.value,
      formKey: nextAssetKey,
      formName: `表单 ${nextAssetIndex}`,
      components: [],
      settings: {
        ...(normalizedSchema.value.settings || {}),
        formAssets: [],
      },
    })
    updateSchema({
      ...normalizedSchema.value,
      settings: {
        ...(normalizedSchema.value.settings || {}),
        formAssets: [
          {
            formKey: nextAssetKey,
            formName: assetSchema.formName,
            schema: assetSchema,
          },
          ...formAssets.value,
        ],
      },
    })
  }

  function duplicateCurrentFormAsset() {
    const nextAssetIndex = formAssets.value.length + 2
    const nextAssetKey = `${normalizedSchema.value.formKey || 'form'}_copy_${Date.now()}`
    const assetSchema = normalizeFormDesignerSchema({
      ...normalizedSchema.value,
      formKey: nextAssetKey,
      formName: `${normalizedSchema.value.formName || '表单'} 副本 ${nextAssetIndex}`,
      settings: {
        ...(normalizedSchema.value.settings || {}),
        formAssets: [],
      },
    })
    updateSchema({
      ...normalizedSchema.value,
      settings: {
        ...(normalizedSchema.value.settings || {}),
        formAssets: [
          {
            formKey: nextAssetKey,
            formName: assetSchema.formName,
            schema: assetSchema,
          },
          ...formAssets.value,
        ],
      },
    })
  }

  function openPropertyPanel(componentId = '') {
    if (componentId)
      selectedId.value = componentId
    rightOpen.value = true
  }

  function openSourcePanel() {
    rightOpen.value = true
    nextTick(() => {
      window.dispatchEvent(new CustomEvent('forge-form-designer:open-source-panel'))
    })
  }

  function toggleCanvasFocus() {
    canvasFocusMode.value = !canvasFocusMode.value
    leftCollapsed.value = canvasFocusMode.value
    rightOpen.value = false
  }

  function handleCanvasSelectedIdChange(componentId = '') {
    selectedId.value = componentId
    if (componentId)
      rightOpen.value = true
  }

  function handleCanvasDragStart() {
    rightOpen.value = false
  }

  function flushDesigner() {
    const currentSchema = normalizeFormDesignerSchema(normalizedSchema.value)
    const sourceSchema = props.modelValue && typeof props.modelValue === 'object' ? props.modelValue : {}
    return normalizeFormDesignerSchemaForSave({
      ...sourceSchema,
      ...currentSchema,
      defaultFormKey: sourceSchema.defaultFormKey || sourceSchema.settings?.defaultFormKey || currentSchema.settings?.defaultFormKey || currentSchema.formKey,
      settings: {
        ...(sourceSchema.settings || {}),
        ...(currentSchema.settings || {}),
        formAssets: currentSchema.settings?.formAssets || sourceSchema.settings?.formAssets || [],
      },
    })
  }

  function openPageSections() {
    // 分区由布局承载的宿主没有 sections 视图，向导确认等入口改跳画布查看承载容器。
    canvasView.value = props.enableSectionsView ? 'sections' : 'layout'
  }

  function countComponents(components = []) {
    return (Array.isArray(components) ? components : []).reduce((total, component) => {
      return total + 1 + countComponents(component.children || [])
    }, 0)
  }

  function containsLockedField(value = []) {
    if (Array.isArray(value))
      return value.some(item => containsLockedField(item))
    if (!value || typeof value !== 'object')
      return false
    if (value.fieldBinding?.locked === true)
      return true
    return Object.values(value).some(item => containsLockedField(item))
  }

  expose?.({
    flushDesigner,
    openPageSections,
    resetFromFields,
    repairRefs,
    appendField,
    appendShelfItem,
    selectComponent: openPropertyPanel,
  })
  const renameDialogVisible = ref(false)
  const renameFormName = ref('')
  const previewDialogVisible = ref(false)

  watch(
    [previewDialogVisible, previewMode],
    ([visible]) => {
      if (visible)
        previewFormValue.value = { ...(previewValue.value || {}) }
    },
  )
  // Runtime component aliases removed — preview now delegates to AiForm + AiFormLayoutNodes

  const componentTypeAlias = {
    text: 'input',
    input: 'input',
    textarea: 'textarea',
    number: 'number',
    inputNumber: 'number',
    select: 'select',
    radio: 'radio',
    radioGroup: 'radio',
    checkbox: 'checkbox',
    checkboxGroup: 'checkbox',
    switch: 'switch',
    rate: 'rate',
    slider: 'slider',
    date: 'date',
    datePicker: 'date',
    datetime: 'datetime',
    time: 'time',
    timePicker: 'time',
    upload: 'upload',
    cascader: 'cascader',
    treeSelect: 'treeSelect',
    colorPicker: 'colorPicker',
    button: 'button',
    row: 'row',
    fcRow: 'row',
    col: 'col',
    table: 'table',
    tableGrid: 'tableGrid',
    divider: 'AiFormSectionTitle',
    elDivider: 'AiFormSectionTitle',
    title: 'groupTitle',
    fcTitle: 'groupTitle',
    sectionTitle: 'groupTitle',
    groupTitle: 'groupTitle',
    formSectionTitle: 'AiFormSectionTitle',
    FormSectionTitle: 'AiFormSectionTitle',
    groupHeader: 'groupTitle',
    GroupHeader: 'groupTitle',
    titleBlock: 'groupTitle',
    section: 'groupTitle',
    AiFormSectionTitle: 'AiFormSectionTitle',
    aiFormSectionTitle: 'AiFormSectionTitle',
    card: 'card',
    tabs: 'tabs',
    collapse: 'collapse',
    crud: 'crud',
    crudBlock: 'crud',
    AiCrudPage: 'crud',
    aiCrudPage: 'crud',
  }

  function getRuntimeFieldCode(component) {
    return (
      component?.fieldBinding?.fieldCode
      || component?.fieldBinding?.columnName
      || component?.field
      || component?.prop
      || component?.key
      || component?.id
    )
  }

  function normalizeRuntimeComponent(component, mode = 'create') {
    if (!component)
      return component

    const componentKey = component.componentKey || component.type || component.component || 'input'
    const runtimeType = componentTypeAlias[componentKey] || componentKey
    const nodeType = resolveRuntimeNodeType(componentKey)
    const fieldCode = getRuntimeFieldCode(component)
    const rules = [...(component.validation?.rules || [])]
    const runtimeProps = resolveRuntimeFieldProps(component, componentKey, fieldCode)
    const readonly = mode === 'detail' || Boolean(component.visibility?.readonly)
    const disabled = readonly || Boolean(runtimeProps.disabled)

    if (component.validation?.required && !rules.some(rule => rule?.required)) {
      rules.unshift({
        required: true,
        message: component.validation.requiredMessage || `请输入${component.label || ''}`,
        trigger: ['blur', 'change'],
      })
    }

    const normalized = {
      ...component,
      ...runtimeProps,
      type: runtimeType,
      component: runtimeType,
      componentKey,
      field: fieldCode,
      prop: fieldCode,
      path: fieldCode,
      name: fieldCode,
      label: component.label,
      required: Boolean(component.validation?.required),
      rules,
      hidden: Boolean(component.visibility?.hidden),
      readonly,
      disabled,
      span: component.layout?.span ?? component.span,
      children: Array.isArray(component.children)
        ? component.children.map(child => normalizeRuntimeComponent(child, mode)).filter(Boolean)
        : component.children,
      props: {
        ...runtimeProps,
        disabled,
        readonly,
      },
    }

    if (nodeType) {
      normalized.nodeType = nodeType
      if (nodeType !== 'field') {
        delete normalized.field
        delete normalized.prop
        delete normalized.path
        delete normalized.name
      }
    }

    return normalized
  }

  function resolveRuntimeProps(props = {}, componentKey = '') {
    const nextProps = { ...(props || {}) }
    if (nextProps.dictType && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader'].includes(componentKey))
      delete nextProps.options
    if (hasDynamicPreviewOptionSource(nextProps.optionSource))
      delete nextProps.options
    return nextProps
  }

  function resolveRuntimeFieldProps(component = {}, componentKey = '', fieldCode = '') {
    const runtimeProps = resolveRuntimeProps(component.props || {}, componentKey)
    const fieldAsset = findFieldAsset(fieldCode)
    return mergeRelationRuntimeProps(runtimeProps, component, fieldAsset)
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

  function mergeRelationRuntimeProps(runtimeProps = {}, component = {}, fieldAsset = null) {
    const next = { ...(runtimeProps || {}) }
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

    const recordSelector = normalizeRuntimeRecordSelectorConfig({
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

  function resolveRuntimeNodeType(componentKey = '') {
    if (['row', 'fcRow'].includes(componentKey))
      return 'row'
    if (componentKey === 'col')
      return 'col'
    if (['card', 'elCard'].includes(componentKey))
      return 'card'
    if (['tabs', 'elTabs'].includes(componentKey))
      return 'tabs'
    if (['tabPane', 'elTabPane'].includes(componentKey))
      return 'tabPane'
    if (['collapse', 'elCollapse'].includes(componentKey))
      return 'collapse'
    if (['collapseItem', 'elCollapseItem'].includes(componentKey))
      return 'collapseItem'
    if (isFormDividerRuntimeComponent(componentKey))
      return 'divider'
    if (isGroupTitleRuntimeComponent(componentKey))
      return 'groupTitle'
    if (['button', 'table', 'tableGrid'].includes(componentKey))
      return componentKey
    if (isCrudRuntimeComponent(componentKey))
      return 'AiCrudPage'
    return ''
  }

  function buildRuntimeFormSchema(schema, mode = 'create') {
    const components = Array.isArray(schema) ? schema : schema?.components || []
    return components.map(component => preparePreviewNode(component)).filter(Boolean)
  }

  /**
   * 预览专用：为 AiForm + AiFormLayoutNodes 补齐 nodeType，不做运行时属性注入。
   * 避免 normalizeRuntimeComponent 添加的 hidden/visibility/disabled/readonly/required/rules
   * 干扰 AiForm 内部的 filterVisibleNodes 和 applyRuntimeControl 逻辑。
   */
  function preparePreviewNode(component) {
    if (!component || typeof component !== 'object')
      return null
    const componentKey = component.componentKey || component.type || 'input'
    const nodeType = resolveRuntimeNodeType(componentKey)
    const children = Array.isArray(component.children)
      ? component.children.map(child => preparePreviewNode(child)).filter(Boolean)
      : []
    const isWidget = isPageWidgetComponentKey(componentKey)
    const fieldCode = component.fieldBinding?.fieldCode || component.field || component.prop || ''
    const props = { ...(component.props || {}) }
    // 动态选项源预览时丢掉静态「选项一/选项二」，交给 AiFormItem 拉业务对象
    if (hasDynamicPreviewOptionSource(props.optionSource))
      delete props.options
    const result = {
      ...component,
      props,
      componentKey,
      // 预览必须带上 AiFormItem 识别的 type/field，否则下拉会落成默认 input，选中回填不会触发
      type: component.type || componentKey,
      ...(fieldCode
        ? {
            field: fieldCode,
            prop: fieldCode,
          }
        : {}),
      children,
      ...(isWidget ? { fieldBinding: { ...(component.fieldBinding || {}), mode: component.fieldBinding?.mode || 'virtual' } } : {}),
    }
    if (nodeType) {
      result.nodeType = nodeType
      if (nodeType !== 'field') {
        delete result.field
        delete result.prop
        delete result.path
        delete result.name
      }
    }
    return result
  }

  function hasDynamicPreviewOptionSource(source = null) {
    if (!source || typeof source !== 'object')
      return false
    const type = String(source.type || '').toUpperCase()
    if (['CURRENT_CHILDREN', 'QUERY_SOURCE', 'REMOTE', 'BUSINESS_OBJECT'].includes(type))
      return true
    return Boolean(String(source.api || source.url || source.sourceKey || '').trim())
  }

  function getDefaultRuntimeValue(component, mode = 'create') {
    if (mode === 'edit' || mode === 'detail')
      return getMockRuntimeValue(component)
    return resolveDesignerCanvasPreviewValue(component)
  }

  function getMockRuntimeValue(component) {
    const componentKey = component?.componentKey || component?.type
    const label = component?.label || component?.props?.placeholder || '字段'
    const options = Array.isArray(component?.props?.options) ? component.props.options : []
    const firstOption = options[0]?.value ?? options[0]?.label ?? '1'

    if (['number', 'inputNumber', 'rate', 'slider'].includes(componentKey))
      return 100
    if (['select', 'radio', 'radioGroup'].includes(componentKey))
      return firstOption
    if (['checkbox', 'checkboxGroup'].includes(componentKey))
      return [firstOption]
    if (componentKey === 'switch')
      return true
    if (componentKey === 'date')
      return '2026-06-17'
    if (componentKey === 'datetime')
      return '2026-06-17 09:30:00'
    if (componentKey === 'month')
      return '2026-06'
    if (componentKey === 'year')
      return '2026'
    if (componentKey === 'time')
      return '09:30:00'
    if (componentKey === 'daterange')
      return ['2026-06-17', '2026-06-18']
    if (componentKey === 'datetimerange')
      return ['2026-06-17 09:30:00', '2026-06-18 18:00:00']
    if (componentKey === 'timerange')
      return ['09:30:00', '18:00:00']
    if (componentKey === 'textarea')
      return `${label}的模拟详情内容`
    if (['upload', 'imageUpload'].includes(componentKey))
      return []
    return `${label}示例`
  }

  function buildRuntimeFormValue(schema, mode = 'create') {
    const components = Array.isArray(schema) ? schema : schema?.components || []
    const model = {}
    const walk = (nodes = []) => {
      ;(Array.isArray(nodes) ? nodes : []).forEach((component) => {
        if (!component || typeof component !== 'object')
          return
        const fieldCode = getRuntimeFieldCode(component)
        if (fieldCode)
          model[fieldCode] = getDefaultRuntimeValue(component, mode)
        if (Array.isArray(component.children) && component.children.length)
          walk(component.children)
      })
    }
    walk(components)
    return model
  }

  function isCrudRuntimeComponent(componentKey) {
    return ['crud', 'crudBlock', 'AiCrudPage', 'aiCrudPage'].includes(componentKey)
  }

  function isGroupTitleRuntimeComponent(componentKey) {
    return [
      'title',
      'fcTitle',
      'sectionTitle',
      'groupTitle',
      'groupHeader',
      'GroupHeader',
      'titleBlock',
      'section',
    ].includes(componentKey)
  }

  function isFormDividerRuntimeComponent(componentKey) {
    return [
      'AiFormSectionTitle',
      'aiFormSectionTitle',
      'formSectionTitle',
      'FormSectionTitle',
      'divider',
      'elDivider',
    ].includes(componentKey)
  }

  // ─── Preview section-split helpers removed ───
  // Preview now passes the full normalized schema to AiForm + AiFormLayoutNodes,
  // which handles tabs / card / collapse / divider / crud natively.

  if (typeof window !== 'undefined') {
    window.addEventListener('forge-form-designer:preview-current-form', () => {
      previewDialogVisible.value = true
    })
  }

  function openRenameCurrentForm() {
    renameFormName.value = props.modelValue?.formName || '未命名表单'
    renameDialogVisible.value = true
  }

  function confirmRenameCurrentForm() {
    const activeSchema = cloneDesignerSchema(normalizedSchema.value)
    const nextName = renameFormName.value.trim()
    if (!activeSchema || !nextName)
      return

    activeSchema.formName = nextName
    const currentKey = activeSchema.formKey
    const assets = activeSchema.settings?.formAssets || []
    const currentAsset = assets.find(asset => asset?.formKey === currentKey)
    if (currentAsset) {
      currentAsset.formName = nextName
      if (currentAsset.schema) {
        currentAsset.schema.formName = nextName
      }
    }

    renameDialogVisible.value = false
    updateSchema({ ...activeSchema })
  }

  onMounted(() => {
    window.addEventListener('keydown', handleDesignerShortcut)
    window.addEventListener('forge-form-designer:canvas-drag-start', handleCanvasDragStart)
    const hydrateShelf = () => {
      shelfHydrated.value = true
    }
    if (typeof window.requestIdleCallback === 'function')
      window.requestIdleCallback(hydrateShelf, { timeout: 400 })
    else
      window.setTimeout(hydrateShelf, 0)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleDesignerShortcut)
    window.removeEventListener('forge-form-designer:canvas-drag-start', handleCanvasDragStart)
  })
  __impl.updateSchema = updateSchema
  __impl.pushHistorySnapshot = pushHistorySnapshot
  __impl.undoSchema = undoSchema
  __impl.redoSchema = redoSchema
  __impl.handleDesignerShortcut = handleDesignerShortcut
  __impl.cloneDesignerSchema = cloneDesignerSchema
  __impl.isSameDesignerSchema = isSameDesignerSchema
  __impl.appendField = appendField
  __impl.appendShelfItem = appendShelfItem
  __impl.resetFromFields = resetFromFields
  __impl.openClearCanvasDialog = openClearCanvasDialog
  __impl.updatePageSectionProtocol = updatePageSectionProtocol
  __impl.updateBottomBarFromDialog = updateBottomBarFromDialog
  __impl.handleDesignerMoreSelect = handleDesignerMoreSelect
  __impl.openPrintPreview = openPrintPreview
  __impl.confirmClearCanvas = confirmClearCanvas
  __impl.repairRefs = repairRefs
  __impl.switchFormAsset = switchFormAsset
  __impl.openFormTabsMenu = openFormTabsMenu
  __impl.handleFormTabsMenuSelect = handleFormTabsMenuSelect
  __impl.createBlankFormAsset = createBlankFormAsset
  __impl.duplicateCurrentFormAsset = duplicateCurrentFormAsset
  __impl.openPropertyPanel = openPropertyPanel
  __impl.openSourcePanel = openSourcePanel
  __impl.toggleCanvasFocus = toggleCanvasFocus
  __impl.handleCanvasSelectedIdChange = handleCanvasSelectedIdChange
  __impl.handleCanvasDragStart = handleCanvasDragStart
  __impl.flushDesigner = flushDesigner
  __impl.openPageSections = openPageSections
  __impl.countComponents = countComponents
  __impl.containsLockedField = containsLockedField
  __impl.getRuntimeFieldCode = getRuntimeFieldCode
  __impl.normalizeRuntimeComponent = normalizeRuntimeComponent
  __impl.resolveRuntimeProps = resolveRuntimeProps
  __impl.resolveRuntimeFieldProps = resolveRuntimeFieldProps
  __impl.findFieldAsset = findFieldAsset
  __impl.mergeRelationRuntimeProps = mergeRelationRuntimeProps
  __impl.firstText = firstText
  __impl.resolveRuntimeNodeType = resolveRuntimeNodeType
  __impl.buildRuntimeFormSchema = buildRuntimeFormSchema
  __impl.preparePreviewNode = preparePreviewNode
  __impl.hasDynamicPreviewOptionSource = hasDynamicPreviewOptionSource
  __impl.getDefaultRuntimeValue = getDefaultRuntimeValue
  __impl.getMockRuntimeValue = getMockRuntimeValue
  __impl.buildRuntimeFormValue = buildRuntimeFormValue
  __impl.isCrudRuntimeComponent = isCrudRuntimeComponent
  __impl.isGroupTitleRuntimeComponent = isGroupTitleRuntimeComponent
  __impl.isFormDividerRuntimeComponent = isFormDividerRuntimeComponent
  __impl.openRenameCurrentForm = openRenameCurrentForm
  __impl.confirmRenameCurrentForm = confirmRenameCurrentForm

  return {
    props, emit, __impl, mut, appendField, appendShelfItem, buildRuntimeFormSchema, buildRuntimeFormValue, cloneDesignerSchema,
    confirmClearCanvas, confirmRenameCurrentForm, containsLockedField, countComponents, createBlankFormAsset, duplicateCurrentFormAsset, findFieldAsset, firstText,
    flushDesigner, getDefaultRuntimeValue, getMockRuntimeValue, getRuntimeFieldCode, handleCanvasDragStart, handleCanvasSelectedIdChange, handleDesignerMoreSelect, handleDesignerShortcut,
    handleFormTabsMenuSelect, hasDynamicPreviewOptionSource, isCrudRuntimeComponent, isFormDividerRuntimeComponent, isGroupTitleRuntimeComponent, isSameDesignerSchema, mergeRelationRuntimeProps, normalizeRuntimeComponent,
    openClearCanvasDialog, openFormTabsMenu, openPageSections, openPrintPreview, openPropertyPanel, openRenameCurrentForm, openSourcePanel, preparePreviewNode,
    pushHistorySnapshot, redoSchema, repairRefs, resetFromFields, resolveRuntimeFieldProps, resolveRuntimeNodeType, resolveRuntimeProps, switchFormAsset,
    toggleCanvasFocus, undoSchema, updateBottomBarFromDialog, updatePageSectionProtocol, updateSchema, slots, selectedId, leftCollapsed, shelfHydrated,
    rightOpen, canvasFocusMode, formTabsMenuVisible, formTabsMenuX, formTabsMenuY, undoStack, redoStack, HISTORY_LIMIT,
    previewMode, clearDialogVisible, clearScope, bottomBarDialogVisible, canvasView, previewModeOptions, previewRuntimeContext, baseDesignerMoreOptions,
    normalizedSchema, previewLayout, previewGridCols, previewSchema, previewValue, previewFormValue, usedFieldSet, formAssets,
    designerMoreOptions, canUndo, canRedo, canClearCanvas, componentCount, hasDetailSettings, pageSectionProtocol, canvasViewTitle,
    canvasViewIcon, canvasViewDescription, previewModeTitle, previewModeDescription, formTabsMenuOptions, canvasMetaText, renameDialogVisible, renameFormName,
    previewDialogVisible, componentTypeAlias,
  }
}
