/** crud-page.vue setup part 1. */
import { computed, defineAsyncComponent, h, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import { businessDocumentRuntimeBatch } from '@/api/business-app'
import catalog from '@/catalog'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { buildFormRuntimeContext } from '@/components/ai-form/form-runtime-context'
import { createOfflineSchemaHash } from '@/components/ai-form/offline-form-runtime'
import { normalizeRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import { applyCrudHookRules, CRUD_HOOK_RULE_TARGETS, normalizeCrudHookRules } from '@/components/lowcode-builder/page/crud-hook-rules'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { hasRuntimeVisibilityRules } from '@/components/lowcode-builder/shared/runtime-rules'
import { getDictData } from '@/composables/useDict'
import { useTabStore } from '@/store'
import { postEncrypt, request } from '@/utils'
import { getDefaultPageTitle } from '@/utils/page-title'
import {
  compileUiDocument,
  resolveAiFormSchemaFromUiDocument,
  UI_DOCUMENT_PROTOCOL_VERSION,
  UI_DOCUMENT_UI_TYPES,
} from '@/protocols/ui-document'
import { normalizeMultiFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
import { shouldRenderRuntimeListGrid } from '@/components/lowcode-builder/shared/runtime-list-grid'
export function applyCrudPageViewPart1(props, emit) {
  const __impl = {}
  const mut = {}

  // part2 延迟实现：setup 结束后经 __impl 转发，避免拆分后跨 part 裸引用报错
  function applyRuntimeFieldMeta(...args) {
    return __impl.applyRuntimeFieldMeta(...args)
  }
  function firstRuntimeText(...args) {
    return __impl.firstRuntimeText(...args)
  }
  function resolveRouteConfigKey(...args) {
    return __impl.resolveRouteConfigKey(...args)
  }

  const RUNTIME_ROUTE_PARAM_KEYS = new Set([
    'appId',
    'menuKey',
    'menuResourceId',
    'runtimeOpenMode',
    'pageKey',
    'formKey',
    'mode',
    'title',
    'configKey',
    'id',
    'recordId',
    'formDefaultValues',
    'submitDefaultParams',
    'designPreview',
  ])

  const route = useRoute()
  const router = useRouter()
  const tabStore = useTabStore()

  const loading = ref(false)
  const configLoaded = ref(false)
  const errorMsg = ref('')
  const renderConfig = ref(null)
  const dictCache = ref({})
  const runtimeCrudRef = ref(null)
  const lastInitialActionKey = ref('')
  const runtimeDetailRecord = ref({})
  const runtimeDetailLoading = ref(false)
  const embeddedRuntime = computed(() => Boolean(props.runtimeConfig?.configKey))
  const runtimeOpenMode = computed(() => String(route.query?.runtimeOpenMode || '').toUpperCase())
  const formOnlyRuntime = computed(() => runtimeOpenMode.value === 'CREATE_FORM')
  const designPreview = computed(() => !embeddedRuntime.value && String(route.query?.designPreview || '') === '1')

  /** 当前加载的模板组件（null 表示降级到 AiCrudPage） */
  const currentTemplate = ref(null)

  const activeRuntimePageKey = computed(() => String(route.query?.pageKey || 'list').trim() || 'list')
  const activeRuntimeFormKey = computed(() => String(route.query?.formKey || '').trim())
  const runtimePages = computed(() => {
    const pages = renderConfig.value?.pageSchema?.pages
    return Array.isArray(pages) ? pages : []
  })
  const activeRuntimePage = computed(() => {
    const key = activeRuntimePageKey.value
    return runtimePages.value.find(page => page?.pageKey === key) || null
  })
  const activeRuntimeGridLayout = computed(() => {
    const pageSchema = renderConfig.value?.pageSchema || {}
    const page = activeRuntimePage.value
    if (page?.gridLayout && Array.isArray(page.gridLayout.items))
      return page.gridLayout
    if (activeRuntimePageKey.value !== 'list')
      return null
    const layout = pageSchema.listGridLayout
    if (!layout || !Array.isArray(layout.items))
      return null
    return layout
  })
  const runtimeGridLayout = computed(() => {
    const layout = activeRuntimeGridLayout.value
    if (!layout)
      return null
    const pageSchema = renderConfig.value?.pageSchema || {}
    return {
      ...layout,
      layoutType: layout.layoutType || pageSchema.layoutType || renderConfig.value?.layoutType || 'simple-crud',
    }
  })
  // list 页有伴生块（树/标题/提示等）时走自由布局；纯 CRUD 仍渲 AiCrudPage
  const standardListRuntime = computed(() => activeRuntimePageKey.value === 'list' && !formOnlyRuntime.value)
  const shouldRenderRuntimeGrid = computed(() => {
    if (!configLoaded.value || formOnlyRuntime.value)
      return false
    return shouldRenderRuntimeListGrid({
      pageSchema: renderConfig.value?.pageSchema,
      modelSchema: renderConfig.value?.modelSchema,
      layoutType: renderConfig.value?.layoutType,
      formOnly: formOnlyRuntime.value,
      pageKey: activeRuntimePageKey.value,
    })
  })
  const runtimeEffectiveLayoutType = computed(() => (
    runtimeGridLayout.value?.layoutType
    || renderConfig.value?.pageSchema?.layoutType
    || renderConfig.value?.layoutType
    || 'simple-crud'
  ))

  const runtimeFields = computed(() => {
    const fields = renderConfig.value?.modelSchema?.fields
    return Array.isArray(fields) ? fields.map(normalizeRuntimeField).filter(field => field.field) : []
  })

  const runtimeColumnSettings = computed(() => {
    const settings = {}
    const items = activeRuntimeGridLayout.value?.items
    if (!Array.isArray(items))
      return settings
    ;['data-table', 'AiCrudPage', 'AiTable'].forEach((blockType) => {
      const block = items.find(item => item?.blockType === blockType)
      const fieldSettings = block?.props?.fieldSettings
      if (fieldSettings && typeof fieldSettings === 'object')
        Object.assign(settings, fieldSettings)
    })
    return settings
  })
  const runtimeAiCrudBlockProps = computed(() => {
    const items = activeRuntimeGridLayout.value?.items
    if (!Array.isArray(items))
      return {}
    return items.find(item => item?.blockType === 'AiCrudPage')?.props || {}
  })
  const activeRuntimeFormProfile = computed(() => buildRuntimeFormProfile(renderConfig.value, activeRuntimeFormKey.value))
  const routeEntryPublicQuery = computed(() => extractRouteEntryPublicQuery(route.query || {}))
  const routeEntryFormDefaultValues = computed(() => parseRouteRecordParam(route.query?.formDefaultValues))
  const routeEntrySubmitDefaultParams = computed(() => parseRouteRecordParam(route.query?.submitDefaultParams))

  function normalizeRuntimeField(field = {}) {
    return {
      ...field,
      field: field.field || field.fieldCode || field.columnName || '',
      label: field.label || field.fieldName || field.field || field.fieldCode || field.columnName || '',
      componentType: field.componentType || field.componentKey || field.dataType || 'input',
    }
  }

  function extractRouteEntryPublicQuery(query = {}) {
    return Object.entries(query).reduce((result, [key, value]) => {
      if (!key || RUNTIME_ROUTE_PARAM_KEYS.has(key))
        return result
      const normalizedValue = normalizeRouteParamValue(value)
      if (normalizedValue !== undefined && normalizedValue !== '')
        result[key] = normalizedValue
      return result
    }, {})
  }

  function normalizeRouteParamValue(value) {
    if (Array.isArray(value))
      return value.length > 1 ? value : value[0]
    if (value === null || value === undefined)
      return undefined
    return value
  }

  function parseRouteRecordParam(value) {
    const normalizedValue = normalizeRouteParamValue(value)
    if (!normalizedValue)
      return {}
    if (typeof normalizedValue === 'object' && !Array.isArray(normalizedValue))
      return normalizedValue
    if (typeof normalizedValue !== 'string')
      return {}
    try {
      const parsed = JSON.parse(normalizedValue)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    }
    catch {
      return {}
    }
  }

  /**
   * 转换表格列配置：将 JSON 格式的 render 对象转为 Vue render 函数
   * 如果配置了 transConfig，则使用翻译后的 xxxName 字段直接显示文本
   */
  function transformColumns(columns, transConfig, options = {}) {
    // 构建翻译映射: { field -> targetField }
    const transMap = {}
    if (transConfig && typeof transConfig === 'object') {
      for (const [field, conf] of Object.entries(transConfig)) {
        if (!isSystemRuntimeField(field))
          transMap[field] = conf.targetField || (`${field}Name`)
      }
    }

    let treeColumnApplied = false
    const isActionColumnKey = key => ['actions', 'action', 'operations', 'operation'].includes(String(key || ''))
    const canRenderTranslatedText = key => key && !isSystemRuntimeField(key)
    const result = (columns || []).map((col) => {
      // 统一提取字段名，优先级：prop > key > dataIndex
      const key = col.prop || col.key || col.dataIndex
      const columnSetting = key ? options.columnSettings?.[key] || {} : {}
      // 统一补prop字段，AiTable需要这个字段来匹配数据
      const newCol = { ...col, ...columnSetting, prop: key }
      if (isActionColumnKey(key)) {
        const mergedActions = mergeRowActions(Array.isArray(col.actions) ? col.actions : [], options.rowActions || [])
        if (mergedActions.length) {
          newCol.actions = options.includeDetailAction ? ensureDetailRowAction(mergedActions) : mergedActions
          newCol.width = Math.max(Number(col.width) || 0, newCol.actions.length * 58, 180)
        }
      }
      else if (Array.isArray(col.actions) && options.includeDetailAction) {
        newCol.actions = ensureDetailRowAction(col.actions)
        newCol.width = Math.max(Number(col.width) || 0, newCol.actions.length * 58, 180)
      }
      if (options.treeTable && !treeColumnApplied && key && !isActionColumnKey(key)) {
        newCol.tree = true
        treeColumnApplied = true
      }

      if (options.fitTableToContainer && !newCol.fixed && !isActionColumnKey(key)) {
        delete newCol.width
        delete newCol.minWidth
        delete newCol.maxWidth
      }

      const renderConfig = {
        ...(newCol.renderConfig || {}),
        ...(columnSetting.renderConfig || {}),
      }
      // dictTag 渲染
      if (col.render && typeof col.render === 'object' && col.render.type === 'dictTag') {
        renderConfig.renderType = 'dictTag'
        renderConfig.dictType = col.render.dictType
      }
      else if (canRenderTranslatedText(key) && col.render && typeof col.render === 'object' && col.render.type === 'relationName') {
        renderConfig.textField = col.render.targetField || `${key}Name`
      }
      else if (canRenderTranslatedText(key) && col.render && typeof col.render === 'object' && ['orgName', 'userName', 'regionName', 'fileUpload'].includes(col.render.type)) {
        renderConfig.textField = col.render.targetField || `${key}Name`
      }
      // 如果该字段有翻译配置，优先显示翻译后的值，没有则显示原字段值
      else if (canRenderTranslatedText(key) && transMap[key]) {
        renderConfig.textField = transMap[key]
      }
      if (Object.keys(renderConfig).length)
        newCol.renderConfig = renderConfig
      // AiCrudPage.resolveColumnRender 认 col.render.type；同时写入 relationName，避免只写 renderConfig 时列表仍显示 value
      if (renderConfig.textField && (!newCol.render || typeof newCol.render !== 'function')) {
        if (!newCol.render || typeof newCol.render !== 'object') {
          newCol.render = {
            type: 'relationName',
            targetField: renderConfig.textField,
          }
        }
        else if (!newCol.render.targetField && ['relationName', 'orgName', 'userName', 'regionName'].includes(newCol.render.type)) {
          newCol.render = {
            ...newCol.render,
            targetField: renderConfig.textField,
          }
        }
      }
      applyRuntimeColumnPresentation(newCol, newCol, key)
      return newCol
    })

    const rowActions = normalizeRuntimePageActions(options.rowActions || [], 'row')
    const hasActionColumn = result.some((col) => {
      const key = col.prop || col.key || col.dataIndex
      return isActionColumnKey(key)
    })
    if (!hasActionColumn && rowActions.length) {
      result.push({
        key: 'actions',
        title: '操作',
        dataIndex: 'actions',
        prop: 'actions',
        width: Math.max(180, rowActions.length * 58),
        fixed: 'right',
        actions: options.includeDetailAction ? ensureDetailRowAction(rowActions) : rowActions,
        maxActionButtons: 3,
      })
    }

    return result
  }

  function isSystemRuntimeField(field) {
    return [
      'id',
      'tenantId',
      'tenant_id',
      'createBy',
      'create_by',
      'createTime',
      'create_time',
      'createDept',
      'create_dept',
      'updateBy',
      'update_by',
      'updateTime',
      'update_time',
      'delFlag',
      'del_flag',
    ].includes(String(field || ''))
  }

  function applyRuntimeColumnPresentation(targetCol, sourceCol = {}, key = '') {
    if (!key)
      return
    if (['actions', 'action', 'operations', 'operation'].includes(String(key || '')))
      return
    targetCol.render = (row) => {
      return h(FieldValueRenderer, {
        value: row[key],
        row,
        field: {
          ...sourceCol,
          field: key,
          dictType: sourceCol.dictType || sourceCol.render?.dictType,
        },
        setting: {
          ...(sourceCol.render || {}),
          ...(sourceCol.renderConfig || {}),
          ...sourceCol,
        },
        context: {
          record: row,
          row,
          data: row,
          route: {
            query: route.query || {},
            params: route.params || {},
            path: route.path,
            fullPath: route.fullPath,
            name: route.name,
          },
        },
        onNavigate: ({ event }) => {
          const target = buildRuntimeColumnRoute(sourceCol, row)
          if (target)
            router.push(target)
          event?.preventDefault?.()
        },
      })
    }
  }

  function buildRuntimeColumnRoute(col = {}, row = {}) {
    const configKey = renderConfig.value?.configKey || resolveRouteConfigKey()
    if (!configKey)
      return null
    const paramName = col.targetParamName || 'id'
    const paramField = col.targetParamField || 'id'
    const paramValue = row[paramField] ?? row.id
    const query = {
      ...route.query,
      pageKey: col.targetPageKey || 'detail',
      formKey: col.targetFormKey || route.query?.formKey || undefined,
      [paramName]: paramValue,
    }
    if (!query.formKey)
      delete query.formKey
    if ((col.targetPageKey || 'detail') === 'detail') {
      query.mode = 'detail'
      query.recordId = paramValue
    }
    return {
      path: `/ai/crud-page/${encodeURIComponent(configKey)}`,
      query,
    }
  }

  function mergeRowActions(baseActions = [], extraActions = []) {
    const next = [...baseActions]
    const existingKeys = new Set(next.map(action => String(action?.key || action?.actionCode || '').toLowerCase()).filter(Boolean))
    normalizeRuntimePageActions(extraActions, 'row').forEach((action) => {
      const key = String(action.key || '').toLowerCase()
      if (!key || existingKeys.has(key))
        return
      existingKeys.add(key)
      next.push(action)
    })
    return next
  }

  function ensureDetailRowAction(actions = []) {
    if (actions.some(action => action?.key === 'detail'))
      return actions
    const next = [...actions]
    const editIndex = next.findIndex(action => action?.key === 'edit')
    const detailAction = { key: 'detail', label: '查看详情', type: 'info', position: 'row' }
    if (editIndex >= 0) {
      next.splice(editIndex + 1, 0, detailAction)
      return next
    }
    next.unshift(detailAction)
    return next
  }

  function normalizeRuntimePageActions(actions = [], position = 'row') {
    if (!Array.isArray(actions))
      return []
    return actions
      .map(action => normalizeRuntimePageAction(action, position))
      .filter(Boolean)
  }

  function normalizeRuntimePageAction(action = {}, position = 'row') {
    if (!action || action.visible === false || action.status === 0)
      return null
    const actionPosition = normalizeActionPosition(action.position || action.actionPosition || position)
    if (actionPosition !== position)
      return null
    const actionType = normalizeActionType(action.actionType || action.type)
    const config = action.actionConfig || {}
    const routePath = action.routePath
      || config.targetPath
      || config.routePath
      || config.url
      || ''
    const key = action.key || action.actionCode || action.actionName || action.label
    if (!key)
      return null
    const label = action.label || action.actionName || key
    if (position === 'toolbar' && isBuiltinCreateToolbarAction(key, label, actionType, routePath))
      return null
    return {
      ...action,
      key,
      label,
      type: resolveRuntimeButtonType(action, actionType),
      position: actionPosition,
      actionType,
      routePath,
      targetFormKey: action.targetFormKey || config.targetFormKey || '',
      openTarget: action.openTarget || config.openTarget || (actionType === 'external' ? '_blank' : '_self'),
      confirmText: action.confirmText || (action.confirmRequired ? `确认执行“${action.actionName || action.label || key}”？` : ''),
    }
  }

  function isBuiltinCreateToolbarAction(key, label, actionType, routePath) {
    if (actionType !== 'route' || routePath)
      return false
    const normalizedKey = normalizeActionIdentity(key)
    const normalizedLabel = normalizeActionIdentity(label)
    return ['add', 'create', 'new', '新增', '新建'].includes(normalizedKey)
      || ['add', 'create', 'new', '新增', '新建'].includes(normalizedLabel)
  }

  function normalizeActionIdentity(value) {
    return String(value || '')
      .trim()
      .toLowerCase()
      .replace(/[-_\s]+/g, '')
  }

  function normalizeActionPosition(value) {
    const text = String(value || 'row').toLowerCase()
    if (text === 'toolbar')
      return 'toolbar'
    if (text === 'detail')
      return 'detail'
    return 'row'
  }

  function normalizeActionType(value) {
    const text = String(value || 'route')
      .replace(/([a-z])([A-Z])/g, '$1_$2')
      .replace('-', '_')
      .toUpperCase()
    if (text === 'START_FLOW')
      return 'START_FLOW'
    if (text === 'OPEN_EXTERNAL' || text === 'EXTERNAL')
      return 'external'
    if (text === 'OPEN_PAGE' || text === 'ROUTE')
      return 'route'
    return text || 'route'
  }

  function resolveRuntimeButtonType(action = {}, actionType = '') {
    if (action.buttonType)
      return action.buttonType
    if (action.type && !['OPEN_PAGE', 'OPEN_EXTERNAL', 'START_FLOW', 'CALL_API', 'TRIGGER', 'route', 'external'].includes(action.type))
      return action.type
    if (actionType === 'START_FLOW')
      return 'success'
    if (actionType === 'external')
      return 'info'
    if (['TRIGGER', 'CALL_API'].includes(actionType))
      return 'warning'
    return 'primary'
  }

  /**
   * 转换表单字段配置：为 dictType 字段注入字典选项，为日期字段配置格式化
   */
  function transformFields(fields, fieldMetaMap = new Map()) {
    return (fields || []).map((field) => {
      const newField = { ...field }
      const fieldMeta = fieldMetaMap.get(field.field || field.fieldCode)
      applyRuntimeFieldMeta(newField, fieldMeta)
      applyRuntimeFieldValidation(newField, fieldMeta)
      applyRuntimeFieldLength(newField, fieldMeta)
      if (newField.multiple === undefined) {
        newField.multiple = newField.props?.multiple === true
          || newField.basicProps?.multiple === true
          || newField.props?.recordSelector?.multiple === true
          || newField.recordSelector?.multiple === true
      }

      if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
        const options = dictCache.value[field.dictType] || []
        newField.props = {
          ...(newField.props || {}),
          options,
        }
      }

      // recordSelector: surface field asset selector config to props for runtime resolution.
      if (isRuntimeRecordSelectorField(newField, fieldMeta)) {
        const selectorConfig = normalizeRecordSelectorConfig(buildRuntimeRelationSource(newField, fieldMeta))
        const selectorObjectCode = selectorConfig.objectCode
        if (selectorObjectCode) {
          newField.props = {
            ...(newField.props || {}),
            recordSelector: {
              ...selectorConfig,
              objectCode: selectorObjectCode,
              businessObjectCode: selectorConfig.businessObjectCode || newField.businessObjectCode || fieldMeta?.businessObjectCode || selectorObjectCode,
              targetObjectCode: selectorConfig.targetObjectCode || newField.targetObjectCode || fieldMeta?.targetObjectCode || selectorObjectCode,
            },
          }
        }
      }

      // objectReference: surface reference config to props for runtime optionSource generation
      if (isRuntimeObjectReferenceField(newField, fieldMeta)) {
        const relationSource = buildRuntimeRelationSource(newField, fieldMeta)
        const refObjectCode = firstRuntimeText(
          relationSource.referenceObjectCode,
          relationSource.basicProps?.referenceObjectCode,
          relationSource.props?.referenceObjectCode,
          relationSource.referenceConfig?.referenceObjectCode,
          relationSource.basicProps?.referenceConfig?.referenceObjectCode,
          relationSource.props?.referenceConfig?.referenceObjectCode,
          normalizeRecordSelectorConfig(relationSource).objectCode,
        )
        if (refObjectCode) {
          newField.props = {
            ...(newField.props || {}),
            referenceObjectCode: refObjectCode,
            businessObjectCode: newField.props?.businessObjectCode || refObjectCode,
            targetObjectCode: newField.props?.targetObjectCode || refObjectCode,
            referenceDisplayField: firstRuntimeText(relationSource.referenceDisplayField, relationSource.basicProps?.referenceDisplayField, relationSource.props?.referenceDisplayField, relationSource.props?.displayField),
            referenceValueField: firstRuntimeText(relationSource.referenceValueField, relationSource.basicProps?.referenceValueField, relationSource.props?.referenceValueField, relationSource.props?.valueField, 'id'),
          }
        }
      }

      // Number field type coercion
      if (['number', 'inputNumber'].includes(field.type)) {
        newField.onMounted = (vm) => {
          if (vm.field && vm.value) {
            if (typeof vm.value === 'string') {
              const num = Number.parseFloat(vm.value)
              if (!Number.isNaN(num)) {
                vm.value = num
              }
            }
          }
        }
      }

      const timeProps = resolveDateTimeProps(field.type)
      if (timeProps) {
        newField.props = {
          ...(newField.props || {}),
          ...timeProps,
        }
      }

      return newField
    })
  }

  function buildRuntimeRelationSource(field = {}, meta = {}) {
    return {
      ...(meta || {}),
      ...(field || {}),
      basicProps: {
        ...(meta?.basicProps || {}),
        ...(field?.basicProps || {}),
      },
      props: {
        ...(meta?.props || {}),
        ...(meta?.basicProps || {}),
        ...(field?.props || {}),
      },
      recordSelector: field?.props?.recordSelector
        || field?.recordSelector
        || field?.basicProps?.recordSelector
        || meta?.props?.recordSelector
        || meta?.recordSelector
        || meta?.basicProps?.recordSelector,
    }
  }

  function isRuntimeRecordSelectorField(field = {}, meta = {}) {
    return ['recordSelector', 'RECORD_SELECTOR'].includes(field.type)
      || field.componentType === 'recordSelector'
      || meta?.fieldType === 'RECORD_SELECTOR'
      || meta?.componentType === 'recordSelector'
      || Boolean(normalizeRecordSelectorConfig(buildRuntimeRelationSource(field, meta)).objectCode)
  }

  function isRuntimeObjectReferenceField(field = {}, meta = {}) {
    return ['objectReference', 'REFERENCE'].includes(field.type)
      || field.componentType === 'objectReference'
      || meta?.fieldType === 'REFERENCE'
      || meta?.componentType === 'objectReference'
  }

  function applyRuntimeFieldValidation(field = {}, meta = {}) {
    const validation = field.validation || field.props?.validation || meta?.validation || meta?.basicProps?.validation || meta?.advancedProps?.validation
    if (validation && typeof validation === 'object')
      field.validation = validation
  }

  function applyRuntimeFieldLength(field = {}, meta = {}) {
    const length = Number(field.maxlength || field.props?.maxlength || field.length || meta?.length || 0)
    if (!length || !isRuntimeTextField(field))
      return
    field.maxlength = length
    field.showCount = field.showCount ?? field.props?.showCount ?? true
    field.props = {
      ...(field.props || {}),
      maxlength: length,
      showCount: field.showCount,
    }
  }

  function isRuntimeTextField(field = {}) {
    const type = String(field.type || field.componentType || '').toLowerCase()
    return ['input', 'textarea'].includes(type)
  }

  function buildRuntimeFormProfile(cfg = {}, requestedFormKey = '') {
    const baseEditSchema = Array.isArray(cfg?.editSchema) ? cfg.editSchema : []
    const formDesignerSchema = cfg?.options?.formDesignerSchema || cfg?.formDesignerSchema
    if (!formDesignerSchema) {
      return {
        editSchema: baseEditSchema,
        editFormLayout: cfg?.options?.editFormLayout,
        formAssets: cfg?.options?.formAssets || cfg?.formAssets || [],
        governance: {},
        designerLayout: {},
        uiDocument: null,
        protocolVersion: null,
      }
    }
    const multiSchema = normalizeMultiFormDesignerSchema(formDesignerSchema)
    const selectedForm = resolveRuntimeForm(multiSchema, requestedFormKey)
    if (!selectedForm?.schema) {
      return {
        editSchema: baseEditSchema,
        editFormLayout: cfg?.options?.editFormLayout,
        formAssets: [],
        governance: {},
        designerLayout: {},
        uiDocument: null,
        protocolVersion: null,
      }
    }
    const governance = normalizeFormGovernance(selectedForm.schema.settings?.governance || selectedForm.schema.governance)
    const baseFieldMap = new Map(baseEditSchema.map(field => [field.field, field]))
    const components = flattenDesignerComponents(selectedForm.schema.components || [])
    const editSchema = components
      .map(component => buildRuntimeFieldFromDesignerComponent(component, baseFieldMap))
      .filter(Boolean)
    const governedFields = applyRuntimeFormGovernance(editSchema.length ? editSchema : baseEditSchema, governance)
    // 设计态 → 统一运行态协议（与审批后端 TaskFormUiDocumentCompiler 同构）
    const uiDocument = compileUiDocument(
      selectedForm.schema,
      selectedForm.formKey || '',
      governedFields,
      [],
      { uiType: UI_DOCUMENT_UI_TYPES.LOWCODE_FORM },
    )
    return {
      editSchema: governedFields,
      // 保留旧 layout 作无 uiDocument 时的回退；有协议时 transformEditFields 优先走 uiDocument
      editFormLayout: buildRuntimeFormLayoutFromDesignerComponents(selectedForm.schema.components || []),
      formAssets: buildRuntimeFormAssets(multiSchema, selectedForm.formKey),
      governance,
      designerLayout: selectedForm.schema.layout || {},
      uiDocument,
      protocolVersion: UI_DOCUMENT_PROTOCOL_VERSION,
    }
  }

  function normalizeFormGovernance(value = {}) {
    if (!value || typeof value !== 'object')
      return {}
    return {
      permission: value.permission && typeof value.permission === 'object' ? value.permission : {},
      fieldRules: Array.isArray(value.fieldRules) ? value.fieldRules : [],
      events: Array.isArray(value.events) ? value.events : [],
      fieldEvents: Array.isArray(value.fieldEvents) ? value.fieldEvents : [],
      formInit: value.formInit && typeof value.formInit === 'object' ? value.formInit : {},
      offlineDraft: value.offlineDraft && typeof value.offlineDraft === 'object' ? value.offlineDraft : {},
    }
  }

  function applyRuntimeFormGovernance(fields = [], governance = {}) {
    const permission = governance.permission || {}
    if (permission.visible === false)
      return []
    const ruleMap = new Map((governance.fieldRules || [])
      .filter(rule => rule?.field)
      .map(rule => [rule.field, rule]))

    return (Array.isArray(fields) ? fields : [])
      .map(field => applyRuntimeFieldGovernance(field, ruleMap.get(field?.field), permission))
      .filter(Boolean)
  }

  function applyRuntimeFieldGovernance(field = {}, rule = {}, permission = {}) {
    if (!field?.field)
      return field
    if (rule?.hidden === true)
      return null
    const next = { ...field, props: { ...(field.props || {}) } }
    if (Object.prototype.hasOwnProperty.call(rule, 'required')) {
      next.required = !!rule.required
      if (!next.required && Array.isArray(next.rules))
        next.rules = next.rules.filter(item => !item?.required)
    }
    if (Object.prototype.hasOwnProperty.call(rule, 'defaultValue'))
      next.defaultValue = rule.defaultValue
    const readonly = permission.editable === false
      ? true
      : Object.prototype.hasOwnProperty.call(rule, 'readonly')
        ? !!rule.readonly
        : next.readonly
    if (readonly) {
      next.readonly = true
      next.disabled = true
      next.props.readonly = true
      next.props.disabled = true
    }
    return next
  }

  function resolveRuntimeForm(multiSchema = {}, requestedFormKey = '') {
    const forms = Array.isArray(multiSchema.forms) ? multiSchema.forms : []
    if (!forms.length)
      return null
    return forms.find(form => form.formKey === requestedFormKey)
      || forms.find(form => form.formKey === multiSchema.defaultFormKey)
      || forms[0]
  }

  function flattenDesignerComponents(components = []) {
    const result = []
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      result.push(component)
      result.push(...flattenDesignerComponents(component.children || []))
    })
    return result
  }

  function buildRuntimeFieldFromDesignerComponent(component = {}, baseFieldMap = new Map()) {
    const fieldCode = component.fieldBinding?.fieldCode
    if (!fieldCode)
      return null
    const visibility = component.visibility || {}
    const base = baseFieldMap.get(fieldCode) || { field: fieldCode, type: 'input', label: fieldCode }
    const props = { ...(base.props || {}), ...(component.props || {}) }
    const hasVisibilityRules = hasRuntimeVisibilityRules({ ...component, props })
    if (visibility.hidden === true && !hasVisibilityRules)
      return null
    const validation = component.validation || {}
    const readonly = visibility.readonly === true || base.readonly === true
    if (readonly) {
      props.readonly = true
      props.disabled = true
    }
    return {
      ...base,
      field: fieldCode,
      label: component.label || base.label || fieldCode,
      type: normalizeDesignerRuntimeFieldType(component.componentKey || base.type),
      required: validation.required ?? base.required,
      readonly,
      disabled: readonly || base.disabled === true,
      hidden: visibility.hidden === true,
      visibility: {
        ...(base.visibility || {}),
        ...visibility,
      },
      defaultValue: props.defaultValue ?? base.defaultValue,
      dictType: props.dictType || base.dictType,
      validation,
      props,
    }
  }

  function normalizeDesignerRuntimeFieldType(componentKey = '') {
    const key = String(componentKey || '').trim()
    const map = {
      inputNumber: 'number',
      integer: 'number',
      money: 'number',
      dictSelect: 'select',
      orgTreeSelect: 'treeSelect',
      deptTreeSelect: 'treeSelect',
      departmentTreeSelect: 'treeSelect',
      regionTreeSelect: 'treeSelect',
      userSelect: 'select',
      imageUpload: 'imageUpload',
      fileUpload: 'fileUpload',
    }
    return map[key] || key || 'input'
  }

  function buildRuntimeFormLayoutFromDesignerComponents(components = []) {
    return (Array.isArray(components) ? components : [])
      .map(component => buildRuntimeFormLayoutNode(component))
      .filter(Boolean)
  }

  function buildRuntimeFormLayoutNode(component = {}) {
    if (!component || typeof component !== 'object')
      return null
    const fieldCode = component.fieldBinding?.fieldCode
    if (fieldCode) {
      return {
        nodeType: 'field',
        key: component.id || fieldCode,
        field: fieldCode,
        span: component.layout?.span,
        gridStyle: component.layout?.gridStyle,
      }
    }
    const children = buildRuntimeFormLayoutFromDesignerComponents(component.children || [])
    const node = {
      nodeType: component.componentKey || component.nodeType || 'groupTitle',
      componentKey: component.componentKey,
      key: component.id,
      label: component.label,
      props: component.props || {},
      span: component.layout?.span,
      align: component.layout?.align,
      style: component.style,
      children,
    }
    if (!children.length && !isStandaloneRuntimeLayoutNode(node))
      return null
    return node
  }

  function buildRuntimeFormAssets(multiSchema = {}, activeFormKey = '') {
    const forms = Array.isArray(multiSchema.forms) ? multiSchema.forms : []
    return forms
      .filter(form => form?.formKey && form.formKey !== activeFormKey)
      .map(form => ({
        formKey: form.formKey,
        formName: form.formName || form.formKey,
        usage: form.usage || [],
        schema: form.schema || {},
      }))
  }

  function transformEditFields(fields = [], layout = [], fieldMetaMap = new Map(), uiDocument = null) {
    const transformedFields = transformFields(fields, fieldMetaMap)
    // 统一协议优先：设计页编译出的 uiDocument 与审批端同构，保证所见即所得
    if (uiDocument && typeof uiDocument === 'object') {
      const resolved = resolveAiFormSchemaFromUiDocument({
        protocolVersion: uiDocument.version || UI_DOCUMENT_PROTOCOL_VERSION,
        uiDocument,
        fields: transformedFields,
      })
      if (Array.isArray(resolved) && resolved.length)
        return resolved
    }
    if (!Array.isArray(layout) || !layout.length)
      return transformedFields

    const fieldMap = new Map(transformedFields.map(field => [field.field, field]))
    const usedFields = new Set()
    const nodes = layout
      .map(node => hydrateRuntimeLayoutNode(node, fieldMap, usedFields))
      .filter(Boolean)

    transformedFields.forEach((field) => {
      if (field.field && !usedFields.has(field.field))
        nodes.push(field)
    })
    return nodes
  }

  function hydrateRuntimeLayoutNode(node = {}, fieldMap, usedFields) {
    if (!node || typeof node !== 'object')
      return null
    const nodeType = resolveRuntimeLayoutNodeType(node)
    if (node.nodeType === 'field') {
      const field = fieldMap.get(node.field)
      if (!field)
        return null
      usedFields.add(node.field)
      return {
        ...field,
        nodeType: 'field',
        key: node.key || field.field,
        span: node.span || field.span,
        gridStyle: node.gridStyle || field.gridStyle,
      }
    }

    const children = (node.children || [])
      .map(child => hydrateRuntimeLayoutNode(child, fieldMap, usedFields))
      .filter(Boolean)
    if (!children.length && !isStandaloneRuntimeLayoutNode({ ...node, nodeType }))
      return null
    return {
      ...node,
      nodeType,
      children,
    }
  }

  function resolveRuntimeLayoutNodeType(node = {}) {
    if (isGroupTitleRuntimeLayoutNode(node))
      return 'groupTitle'
    if (isLegacyGroupTitleRuntimeLayoutNode(node))
      return 'groupTitle'
    if (isSectionTitleRuntimeLayoutNode(node))
      return 'divider'
    if (isActionRuntimeLayoutNode(node))
      return node.componentKey || node.type || node.nodeType
    return node.nodeType
  }

  function isGroupTitleRuntimeLayoutNode(node = {}) {
    return ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
      .includes(node.componentKey || node.type || node.nodeType)
  }

  function isSectionTitleRuntimeLayoutNode(node = {}) {
    return ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
      .includes(node.componentKey || node.type || node.nodeType)
  }

  function isLegacyGroupTitleRuntimeLayoutNode(node = {}) {
    const props = node.props || {}
    return node.nodeType === 'divider'
      && !node.componentKey
      && Object.prototype.hasOwnProperty.call(props, 'description')
      && !Object.prototype.hasOwnProperty.call(props, 'title')
  }

  function isStandaloneRuntimeLayoutNode(node = {}) {
    return isSectionTitleRuntimeLayoutNode(node)
      || isGroupTitleRuntimeLayoutNode(node)
      || isActionRuntimeLayoutNode(node)
      || node.nodeType === 'widget'
      || isPageWidgetComponentKey(node.componentKey || node.type || node.nodeType)
  }

  function isActionRuntimeLayoutNode(node = {}) {
    return ['button', 'table', 'tableGrid', 'AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock']
      .includes(node.componentKey || node.type || node.nodeType)
  }

  function resolveDateTimeProps(type) {
    switch (String(type || '').toLowerCase()) {
      case 'date':
      case 'daterange':
        return { format: 'yyyy-MM-dd', valueFormat: 'yyyy-MM-dd' }
      case 'datetime':
      case 'datetimerange':
        return { format: 'yyyy-MM-dd HH:mm:ss', valueFormat: 'yyyy-MM-dd HH:mm:ss' }
      case 'time':
      case 'timerange':
        return { format: 'HH:mm:ss', valueFormat: 'HH:mm:ss' }
      default:
        return null
    }
  }

  function normalizeRuntimeWidth(value) {
    if (value === null || value === undefined)
      return ''
    const width = String(value).trim()
    if (!width || width === 'auto' || width === '100%')
      return ''
    if (/^\d+$/.test(width))
      return `${width}px`
    return width
  }

  function resolvePageSchemaEditFormStyle(pageSchema = {}) {
    const zones = Array.isArray(pageSchema.zones) ? pageSchema.zones : []
    const editZone = zones.find((zone) => {
      const key = String(zone?.zoneKey || zone?.key || zone?.type || '').toLowerCase()
      const component = String(zone?.componentKey || zone?.component || '').toLowerCase()
      return ['edit', 'edit-form', 'form'].includes(key) || component === 'edit-form'
    })
    return editZone?.props?.editFormStyle || {}
  }

  function resolveRuntimeModalWidth(options = {}, cfg = {}, formProfile = {}) {
    // 表单设计器布局里的弹窗宽度优先（后端旧版本未把 layout 平铺到 editZone.props 时仍能生效）
    const explicitWidth = normalizeRuntimeWidth(
      formProfile.designerLayout?.modalWidth || options.modalWidth || cfg.modalWidth,
    )
    if (explicitWidth)
      return explicitWidth
    const formStyle = options.editFormStyle
      || cfg.editFormStyle
      || formProfile.editFormStyle
      || resolvePageSchemaEditFormStyle(cfg.pageSchema)
      || {}
    const formStyleWidth = normalizeRuntimeWidth(formStyle.maxWidth || formStyle.width)
    if (formStyleWidth)
      return formStyleWidth
    return '800px'
  }

  __impl.extractRouteEntryPublicQuery = extractRouteEntryPublicQuery
  __impl.normalizeRouteParamValue = normalizeRouteParamValue
  __impl.parseRouteRecordParam = parseRouteRecordParam
  __impl.transformColumns = transformColumns
  __impl.isSystemRuntimeField = isSystemRuntimeField
  __impl.applyRuntimeColumnPresentation = applyRuntimeColumnPresentation
  __impl.buildRuntimeColumnRoute = buildRuntimeColumnRoute
  __impl.mergeRowActions = mergeRowActions
  __impl.ensureDetailRowAction = ensureDetailRowAction
  __impl.normalizeRuntimePageActions = normalizeRuntimePageActions
  __impl.normalizeRuntimePageAction = normalizeRuntimePageAction
  __impl.isBuiltinCreateToolbarAction = isBuiltinCreateToolbarAction
  __impl.normalizeActionIdentity = normalizeActionIdentity
  __impl.normalizeActionPosition = normalizeActionPosition
  __impl.normalizeActionType = normalizeActionType
  __impl.resolveRuntimeButtonType = resolveRuntimeButtonType
  __impl.transformFields = transformFields
  __impl.buildRuntimeRelationSource = buildRuntimeRelationSource
  __impl.isRuntimeRecordSelectorField = isRuntimeRecordSelectorField
  __impl.isRuntimeObjectReferenceField = isRuntimeObjectReferenceField
  __impl.applyRuntimeFieldValidation = applyRuntimeFieldValidation
  __impl.applyRuntimeFieldLength = applyRuntimeFieldLength
  __impl.isRuntimeTextField = isRuntimeTextField
  __impl.buildRuntimeFormProfile = buildRuntimeFormProfile
  __impl.normalizeFormGovernance = normalizeFormGovernance
  __impl.applyRuntimeFormGovernance = applyRuntimeFormGovernance
  __impl.applyRuntimeFieldGovernance = applyRuntimeFieldGovernance
  __impl.resolveRuntimeForm = resolveRuntimeForm
  __impl.flattenDesignerComponents = flattenDesignerComponents
  __impl.buildRuntimeFieldFromDesignerComponent = buildRuntimeFieldFromDesignerComponent
  __impl.normalizeDesignerRuntimeFieldType = normalizeDesignerRuntimeFieldType
  __impl.buildRuntimeFormLayoutFromDesignerComponents = buildRuntimeFormLayoutFromDesignerComponents
  __impl.buildRuntimeFormLayoutNode = buildRuntimeFormLayoutNode
  __impl.buildRuntimeFormAssets = buildRuntimeFormAssets
  __impl.transformEditFields = transformEditFields
  __impl.hydrateRuntimeLayoutNode = hydrateRuntimeLayoutNode
  __impl.resolveRuntimeLayoutNodeType = resolveRuntimeLayoutNodeType
  __impl.isGroupTitleRuntimeLayoutNode = isGroupTitleRuntimeLayoutNode
  __impl.isSectionTitleRuntimeLayoutNode = isSectionTitleRuntimeLayoutNode
  __impl.isLegacyGroupTitleRuntimeLayoutNode = isLegacyGroupTitleRuntimeLayoutNode
  __impl.isStandaloneRuntimeLayoutNode = isStandaloneRuntimeLayoutNode
  __impl.isActionRuntimeLayoutNode = isActionRuntimeLayoutNode
  __impl.resolveDateTimeProps = resolveDateTimeProps
  __impl.normalizeRuntimeWidth = normalizeRuntimeWidth
  __impl.resolvePageSchemaEditFormStyle = resolvePageSchemaEditFormStyle
  __impl.resolveRuntimeModalWidth = resolveRuntimeModalWidth

  return {
    props, emit, __impl, mut, applyRuntimeColumnPresentation, applyRuntimeFieldGovernance, applyRuntimeFieldLength,
    applyRuntimeFieldValidation, applyRuntimeFormGovernance, buildRuntimeColumnRoute,
    buildRuntimeFieldFromDesignerComponent, buildRuntimeFormAssets, buildRuntimeFormLayoutFromDesignerComponents,
    buildRuntimeFormLayoutNode, buildRuntimeFormProfile, buildRuntimeRelationSource, ensureDetailRowAction,
    extractRouteEntryPublicQuery, flattenDesignerComponents, hydrateRuntimeLayoutNode, isActionRuntimeLayoutNode,
    isBuiltinCreateToolbarAction, isGroupTitleRuntimeLayoutNode, isLegacyGroupTitleRuntimeLayoutNode,
    isRuntimeObjectReferenceField, isRuntimeRecordSelectorField, isRuntimeTextField, isSectionTitleRuntimeLayoutNode,
    isStandaloneRuntimeLayoutNode, isSystemRuntimeField, mergeRowActions, normalizeActionIdentity,
    normalizeActionPosition, normalizeActionType, normalizeDesignerRuntimeFieldType, normalizeFormGovernance,
    normalizeRouteParamValue, normalizeRuntimeField, normalizeRuntimePageAction, normalizeRuntimePageActions,
    normalizeRuntimeWidth, parseRouteRecordParam, resolveDateTimeProps, resolvePageSchemaEditFormStyle,
    resolveRuntimeButtonType, resolveRuntimeForm, resolveRuntimeLayoutNodeType, resolveRuntimeModalWidth,
    transformColumns, transformEditFields, transformFields, route, router, tabStore, loading, configLoaded, errorMsg,
    renderConfig, dictCache, runtimeCrudRef, lastInitialActionKey, runtimeDetailRecord, runtimeDetailLoading,
    embeddedRuntime, runtimeOpenMode, formOnlyRuntime, designPreview, currentTemplate, activeRuntimePageKey,
    activeRuntimeFormKey, runtimePages, activeRuntimePage, activeRuntimeGridLayout, runtimeGridLayout,
    standardListRuntime, shouldRenderRuntimeGrid, runtimeEffectiveLayoutType, runtimeFields, runtimeColumnSettings,
    runtimeAiCrudBlockProps, activeRuntimeFormProfile, routeEntryPublicQuery, routeEntryFormDefaultValues,
    routeEntrySubmitDefaultParams, RUNTIME_ROUTE_PARAM_KEYS,
  }
}
