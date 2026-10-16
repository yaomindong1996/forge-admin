/** BusinessListDesigner.vue setup part 2. */
import {
  AddOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  CopyOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  FunnelOutline,
  GridOutline,
  LayersOutline,
  ListOutline,
  PrintOutline,
  RefreshOutline,
  SparklesOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NIcon as NaiveIcon, useMessage } from 'naive-ui'
import { computed, h, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { saveBusinessObjectActions, saveBusinessObjectDesigner, saveBusinessObjectListLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyCrudHookRules,
  CRUD_HOOK_RULE_TARGETS,
  normalizeCrudHookRules,
} from '@/components/lowcode-builder/page/crud-hook-rules'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import {
  isRichListGridLayout,
  resolveRuntimeListGridModel,
} from '@/components/lowcode-builder/shared/runtime-list-grid'
import { useListDesignerStore } from '@/store'
import {
  applyGridLayoutToZones,
  bootstrapGridLayoutFromZones,
  buildPageDesignModelSchema,
  createDefaultListGridLayout,
  createDefaultPageSchema,
  createPageModelRef,
  isPageFieldVisible,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_BASE_COL_WIDTH,
  LIST_PAGE_GRID_GAP,
  resolveDefaultTreeConfig,
  resolveListFieldTitle,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import {
  appendDesignPreviewToApiConfig,
  appendDesignPreviewToApiValue,
} from '@/components/lowcode-builder/shared/runtime-crud-props'
import { applyEmbeddedTreeTableRuntimeProps } from '@/components/lowcode-builder/shared/runtime-tree-table'
import { createViewSchemaFromPageSchema } from '../form-first/viewSchema'
import {
  designerActionTypeToListType as bridgeDesignerActionTypeToListType,
  listActionTypeToDesignerType as bridgeListActionTypeToDesignerType,
  normalizeDesignerActionType as bridgeNormalizeDesignerActionType,
  normalizeListActionType as bridgeNormalizeListActionType,
  resolveListActionTypeFromDesigner as bridgeResolveListActionTypeFromDesigner,
} from './listActionTypeBridge'
export function applyBusinessListDesignerPart2(props, emit, expose, deps = {}) {
  const {
    __impl, mut, addActivePageParam, addDesignerPage, buildCurrentViewSchema, buildDesignerActionsForSave, buildDesignerCrudHookHandlers, buildDesignerRuntimeCrudProps,
    clearActivePageLayout, clearEmbeddedTreeForLeftTreeLayout, clearTreeAddChildOnListBlocks, createCleanTemplateGridLayout, createUniquePageKey, duplicateActivePage, filterDesignerGridBlocks, handleGridLayoutUpdate,
    handleGridModelSchemaUpdate, handleListCustomActionsUpdate, handleListMoreSelect, handlePageActionSelect, isChildRowDesignerAction, isProtectedPage, mergeUnmanagedChildRowActions, normalizeDesignerActionsForList,
    normalizeDesignerPageType, normalizePageKey, normalizePageParamName, openPrintPreview, pageTypeText, patchActivePage, patchPageWatermark, removeActivePage, removeActivePageParam,
    renderMenuIcon, resetActivePageLayout, resetListSchema, resetZoneFields, resolveDesignerFormOpenMode, resolveDesignerModalType, resolveTemplateSelectValue, rewriteBlocksPageEventTargets,
    rewriteGridLayoutPageEventTargets, rewritePageEventTargets, saveLayout, switchActivePage, syncSchemaCustomActions, updateActivePageKey, updateActivePageParam, updateListTemplate,
    updateTreeLayoutEnabled, message, saving, undoStack, redoStack, listGridDesignerRef, activePageKey, pageSettingsExpanded,
    HISTORY_LIMIT, VALID_PAGE_TYPES, baseModelSchema, localSchema, pendingModelSchema, workingModelSchema, effectiveModelSchema, designFields,
    layoutModeLabel, canUndo, canRedo, listPreviewVisible, listPreviewLayout, listPreviewLayoutKey, templateSelectValue, listCustomActions, pageTypeOptions, listTemplateOptions,
    activeTemplateLabel, listMoreOptions, pageActionOptions, designerPages, actionPageOptions, activeDesignerPage, currentPageGridLayout, previewGridLayout, designerGridLayout,
    designerPreviewGridLayout, listDesignerPage, listPreviewPageName, pageWatermark, pageWatermarkContent,
    listPreviewWatermark, listPreviewWatermarkContent, visibleListCustomActions, designerRuntimeCrudProps,
  } = deps
  function designerActionToListAction(action = {}, index = 0) {
    const actionType = normalizeDesignerActionType(action.actionType)
    const config = parsePlainObject(action.actionConfig)
    const position = normalizeListActionPosition(action.actionPosition)
    const key = normalizeActionCode(action.actionCode || action.key || action.actionName) || `custom_${index + 1}`
    const routePath = actionType === 'OPEN_EXTERNAL'
      ? (config.url || action.routePath || '')
      : actionType === 'CALL_API'
        ? (config.url || action.routePath || '')
        : actionType === 'TRIGGER'
          ? (config.triggerCode || action.routePath || '')
          : actionType === 'COMMAND' || actionType === 'REFRESH'
            ? ''
            : config.targetPath || action.routePath || ''
    const listActionType = resolveListActionTypeFromDesigner(actionType, action, config)
    return {
      clientKey: action.clientKey || config.clientKey || key,
      key,
      label: action.actionName || action.label || '自定义操作',
      position,
      type: action.type || resolveDefaultActionButtonType(actionType),
      actionType: listActionType,
      visible: action.visible !== false,
      routePath,
      targetPageKey: config.targetPageKey || action.targetPageKey || '',
      targetFormKey: config.targetFormKey || action.targetFormKey || '',
      openTarget: config.openTarget || action.openTarget || (actionType === 'OPEN_EXTERNAL' ? '_blank' : '_self'),
      permissionKey: action.permissionKey || action.permissionCode || action.permission || '',
      permissionCode: action.permissionKey || action.permissionCode || action.permission || '',
      permissionStrategy: action.permissionStrategy === 'disable' ? 'disable' : 'hide',
      confirmText: action.confirmText || (action.confirmRequired ? `确认执行“${action.actionName || action.label || '该操作'}”？` : ''),
      displayCondition: action.displayCondition || config.displayCondition || '',
      successBehavior: action.successBehavior || config.successBehavior || 'none',
      successMessage: action.successMessage || config.successMessage || '',
      failureMessage: action.failureMessage || config.failureMessage || '',
      params: normalizeActionParams(config.params || action.params || []),
      actionConfig: normalizeListActionConfig(listActionType, {
        ...config,
        displayCondition: action.displayCondition || config.displayCondition || '',
        successBehavior: action.successBehavior || config.successBehavior || 'none',
      }),
      status: action.status ?? 1,
      sortOrder: action.sortOrder ?? index * 10 + 10,
    }
  }

  function listActionToDesignerAction(action = {}, index = 0, options = {}) {
    const normalized = normalizeListCustomAction(action, index)
    const actionType = listActionTypeToDesignerType(normalized.actionType)
    return {
      clientKey: normalized.clientKey || normalized.key,
      actionCode: normalizeActionCode(normalized.key || normalized.label) || `custom_${index + 1}`,
      actionName: normalized.label || '自定义操作',
      actionPosition: normalizeDesignerActionPosition(normalized.position),
      actionType,
      // 保留列表侧按钮样式，避免 designerActions 回写后 type 被默认值覆盖
      type: normalized.type || resolveDefaultListButtonType(normalized.actionType),
      visible: normalized.visible !== false,
      permissionKey: normalized.permissionKey || normalized.permissionCode || '',
      permission: normalized.permissionKey || normalized.permissionCode || '',
      permissionStrategy: normalized.permissionStrategy === 'disable' ? 'disable' : 'hide',
      confirmRequired: Boolean(normalized.confirmText),
      confirmText: normalized.confirmText || '',
      displayCondition: normalized.displayCondition || '',
      successBehavior: normalized.successBehavior || 'none',
      successMessage: normalized.successMessage || '',
      failureMessage: normalized.failureMessage || '',
      status: normalized.status ?? 1,
      sortOrder: normalized.sortOrder ?? index * 10 + 10,
      actionConfig: buildDesignerActionConfig(normalized, actionType, options),
    }
  }

  function buildDesignerActionConfig(action = {}, actionType = 'OPEN_PAGE', options = {}) {
    const config = {
      ...parsePlainObject(action.actionConfig),
      clientKey: action.clientKey || action.actionConfig?.clientKey || action.key || '',
      // 显示条件随 actionConfig 往返，避免 designerActions 回写丢失
      displayCondition: action.displayCondition || action.actionConfig?.displayCondition || '',
    }
    const sourceParams = actionType === 'CALL_API'
      ? config.params || []
      : action.params?.length ? action.params : config.params || []
    const params = options.preserveDraftParams
      ? normalizeActionParams(sourceParams).map(({ clientKey, ...param }) => param)
      : toDesignerActionParams(sourceParams)
    if (actionType === 'CALL_API') {
      return {
        ...config,
        method: normalizeActionApiMethod(config.method || 'POST'),
        url: config.url || action.routePath || '',
        capabilityCode: config.capabilityCode || '',
        successBehavior: action.successBehavior || config.successBehavior || '',
        successMessage: action.successMessage || config.successMessage || '',
        failureMessage: action.failureMessage || config.failureMessage || '',
        params,
      }
    }
    if (actionType === 'START_FLOW') {
      return {
        ...config,
        useMainFlow: true,
      }
    }
    if (actionType === 'TRIGGER') {
      return {
        ...config,
        triggerCode: config.triggerCode || action.triggerCode || action.routePath || '',
        params,
      }
    }
    if (actionType === 'COMMAND') {
      return {
        ...config,
        formSchema: Array.isArray(config.formSchema) ? config.formSchema : [],
        steps: Array.isArray(config.steps) ? config.steps : [],
        successBehavior: action.successBehavior || config.successBehavior || 'refreshList',
        params,
      }
    }
    if (actionType === 'OPEN_EXTERNAL') {
      return {
        ...config,
        url: action.routePath || config.url || '',
        openTarget: action.openTarget || config.openTarget || '_blank',
        params,
        successBehavior: action.successBehavior || config.successBehavior || '',
      }
    }
    if (actionType === 'REFRESH') {
      return {
        ...config,
        clientKey: config.clientKey || action.clientKey || action.key || '',
        displayCondition: action.displayCondition || config.displayCondition || '',
        successBehavior: action.successBehavior || config.successBehavior || 'none',
      }
    }
    return {
      ...config,
      targetPath: action.routePath || config.targetPath || '',
      targetPageKey: action.targetPageKey || config.targetPageKey || '',
      targetFormKey: action.targetFormKey || config.targetFormKey || '',
      openTarget: action.openTarget || config.openTarget || '_self',
      // 列表侧 page/route 都映射成 OPEN_PAGE，用 navigationMode 区分，避免回写丢类型
      navigationMode: normalizeListActionType(action.actionType) === 'page'
        ? 'page'
        : (config.navigationMode === 'page' ? 'page' : 'route'),
      params,
      displayCondition: action.displayCondition || config.displayCondition || '',
      successBehavior: action.successBehavior || config.successBehavior || '',
    }
  }

  function normalizeListCustomActions(actions = []) {
    if (!Array.isArray(actions))
      return []
    return actions
      .filter(action => action && typeof action === 'object')
      .map(normalizeListCustomAction)
  }

  function normalizeListCustomAction(action = {}, index = 0) {
    let actionType = normalizeListActionType(action.actionType)
    const config = normalizeListActionConfig(actionType, action.actionConfig)
    const targetPageKey = action.targetPageKey || config.targetPageKey || ''
    const routePath = action.routePath || resolveListActionRoutePath(actionType, config)
    if (actionType === 'route' && targetPageKey && !routePath)
      actionType = 'page'
    const key = normalizeActionCode(action.key || action.actionCode || action.label) || `custom_${index + 1}`
    return {
      ...action,
      clientKey: action.clientKey || key,
      key,
      label: action.label || action.actionName || '自定义操作',
      position: normalizeListActionPosition(action.position || action.actionPosition),
      type: action.type || resolveDefaultListButtonType(actionType),
      actionType,
      visible: action.visible !== false,
      routePath,
      targetPageKey,
      targetFormKey: action.targetFormKey || config.targetFormKey || '',
      openTarget: action.openTarget || config.openTarget || (actionType === 'external' ? '_blank' : '_self'),
      permissionKey: action.permissionKey || action.permissionCode || action.permission || '',
      permissionCode: action.permissionKey || action.permissionCode || action.permission || '',
      permissionStrategy: action.permissionStrategy === 'disable' ? 'disable' : 'hide',
      confirmText: action.confirmText || '',
      displayCondition: action.displayCondition || config.displayCondition || '',
      successBehavior: action.successBehavior || config.successBehavior || 'none',
      successMessage: action.successMessage || config.successMessage || '',
      failureMessage: action.failureMessage || config.failureMessage || '',
      params: normalizeActionParams(action.params?.length ? action.params : config.params || []),
      actionConfig: config,
      status: action.status ?? 1,
      sortOrder: action.sortOrder ?? index * 10 + 10,
    }
  }

  function normalizeListActionConfig(actionType = 'route', config = {}) {
    const source = parsePlainObject(config)
    if (actionType === 'CALL_API') {
      return {
        ...source,
        apiConfigId: source.apiConfigId === undefined || source.apiConfigId === null ? null : String(source.apiConfigId),
        apiCode: String(source.apiCode || '').trim(),
        apiName: String(source.apiName || '').trim(),
        method: normalizeActionApiMethod(source.method || source.reqMethod || source.apiMethod || 'POST'),
        url: String(source.url || source.apiUrl || source.urlPath || source.path || '').trim(),
        capabilityCode: String(source.capabilityCode || '').trim(),
        params: normalizeActionParams(source.params || source.paramMappings || []),
      }
    }
    if (actionType === 'START_FLOW') {
      return {
        ...source,
        useMainFlow: true,
      }
    }
    if (actionType === 'COMMAND') {
      return {
        ...source,
        formSchema: Array.isArray(source.formSchema) ? source.formSchema : [],
        steps: Array.isArray(source.steps) ? source.steps : [],
        successBehavior: String(source.successBehavior || '').trim() || 'refreshList',
        params: normalizeActionParams(source.params || []),
      }
    }
    if (actionType === 'refresh') {
      return {
        ...source,
      }
    }
    return {
      ...source,
      navigationMode: actionType === 'page' || source.navigationMode === 'page' ? 'page' : (source.navigationMode || 'route'),
      targetPageKey: String(source.targetPageKey || '').trim(),
      targetPath: String(source.targetPath || '').trim(),
      params: normalizeActionParams(source.params || []),
    }
  }

  function normalizeActionParams(params = []) {
    if (!Array.isArray(params))
      return []
    return params
      .filter(param => param && typeof param === 'object')
      .map((param, index) => ({
        clientKey: param.clientKey || `param_${Date.now()}_${index}`,
        name: String(param.name || '').trim(),
        target: ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : '',
        sourceType: ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'rowField',
        sourceField: String(param.sourceField || '').trim(),
        value: param.value === undefined || param.value === null ? '' : String(param.value),
      }))
  }

  function toDesignerActionParams(params = []) {
    return normalizeActionParams(params)
      .filter(param => param.name && (param.sourceType === 'static' ? param.value !== '' : param.sourceField))
      .map(({ clientKey, ...param }) => param)
  }

  function collectSchemaCustomActions(schema = {}) {
    const grids = [
      schema.listGridLayout,
      ...(Array.isArray(schema.pages) ? schema.pages.map(page => page?.gridLayout).filter(Boolean) : []),
    ]
    const actions = []
    grids.forEach((grid) => {
      ;(grid?.items || []).forEach((item) => {
        if (Array.isArray(item?.props?.customActions))
          actions.push(...item.props.customActions)
      })
    })
    const tableZone = (schema.zones || []).find(zone => zone.zoneKey === 'table')
    if (Array.isArray(tableZone?.props?.customActions))
      actions.push(...tableZone.props.customActions)
    return normalizeListCustomActions(deduplicateListActions(actions))
  }

  function deduplicateListActions(actions = []) {
    const seen = new Set()
    return actions.filter((action) => {
      const key = action?.key || action?.actionCode || action?.label
      if (!key)
        return true
      const normalized = String(key)
      if (seen.has(normalized))
        return false
      seen.add(normalized)
      return true
    })
  }

  function isInvalidDesignerApiAction(action = {}) {
    if (normalizeDesignerActionType(action.actionType) !== 'CALL_API')
      return false
    const config = parsePlainObject(action.actionConfig)
    return !String(config.url || config.apiUrl || config.urlPath || '').trim()
      && !String(config.capabilityCode || '').trim()
  }

  function normalizeDesignerActionType(value = '') {
    return bridgeNormalizeDesignerActionType(value)
  }

  function designerActionTypeToListType(actionType = 'OPEN_PAGE') {
    return bridgeDesignerActionTypeToListType(actionType)
  }

  function listActionTypeToDesignerType(actionType = 'route') {
    return bridgeListActionTypeToDesignerType(actionType)
  }

  function normalizeListActionType(value = '') {
    return bridgeNormalizeListActionType(value)
  }

  function resolveListActionTypeFromDesigner(actionType = 'OPEN_PAGE', action = {}, config = {}) {
    return bridgeResolveListActionTypeFromDesigner(actionType, action, config)
  }

  function normalizeListActionPosition(value = '') {
    const normalized = String(value || 'row').replace(/[-\s]+/g, '_').toUpperCase()
    if (normalized === 'TOOLBAR')
      return 'toolbar'
    if (normalized === 'DETAIL')
      return 'detail'
    return 'row'
  }

  function normalizeDesignerActionPosition(value = '') {
    const normalized = normalizeListActionPosition(value)
    if (normalized === 'toolbar')
      return 'TOOLBAR'
    if (normalized === 'detail')
      return 'DETAIL'
    return 'ROW'
  }

  function resolveDefaultActionButtonType(actionType = 'OPEN_PAGE') {
    if (actionType === 'START_FLOW')
      return 'success'
    if (actionType === 'CALL_API')
      return 'primary'
    if (actionType === 'TRIGGER')
      return 'warning'
    if (actionType === 'COMMAND')
      return 'success'
    return 'default'
  }

  function resolveDefaultListButtonType(actionType = 'route') {
    if (actionType === 'START_FLOW')
      return 'success'
    if (actionType === 'CALL_API')
      return 'primary'
    if (actionType === 'TRIGGER')
      return 'warning'
    if (actionType === 'COMMAND')
      return 'success'
    return 'default'
  }

  function resolveListActionRoutePath(actionType = 'route', config = {}) {
    if (actionType === 'external')
      return config.url || ''
    if (actionType === 'CALL_API')
      return config.url || ''
    if (actionType === 'TRIGGER')
      return config.triggerCode || ''
    if (actionType === 'COMMAND' || actionType === 'refresh')
      return ''
    return config.targetPath || ''
  }

  function normalizeActionApiMethod(value = '') {
    const method = String(value || 'POST')
      .replace('-', '_')
      .toUpperCase()
    return ['GET', 'POST', 'POST_ENCRYPT', 'PUT', 'DELETE', 'PATCH'].includes(method) ? method : 'POST'
  }

  function normalizeActionCode(value = '') {
    return String(value || '')
      .trim()
      .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
      .replace(/\W+/g, '_')
      .replace(/_+/g, '_')
      .replace(/^_+|_+$/g, '')
      .toLowerCase()
      .slice(0, 64)
  }

  function parsePlainObject(value) {
    if (!value)
      return {}
    if (typeof value === 'string') {
      try {
        const parsed = JSON.parse(value)
        return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
      }
      catch {
        return {}
      }
    }
    return typeof value === 'object' && !Array.isArray(value) ? { ...value } : {}
  }

  function resolveDesignerDefaultApiValues(schema = {}) {
    const modelSchema = effectiveModelSchema.value || {}
    const key = schema.configKey
      || modelSchema.configKey
      || modelSchema.object?.configKey
      || modelSchema.object?.code
      || modelSchema.objectCode
      || modelSchema.modelCode
      || ''
    const prefix = key ? `/ai/crud/${key}` : '/ai/crud/当前配置'
    return {
      api: prefix,
      listApi: `get@${prefix}/page`,
      detailApi: `get@${prefix}/:id`,
      createApi: `post@${prefix}`,
      updateApi: `put@${prefix}`,
      deleteApi: `delete@${prefix}/:id`,
      treeApi: `get@${prefix}/tree`,
    }
  }

  function buildDesignerSelfTreeOptionSource(schema = localSchema.value) {
    const treeApi = resolveDesignerDefaultApiValues(schema).treeApi
    if (!treeApi || treeApi.includes('当前配置'))
      return undefined
    return {
      type: 'tree',
      api: treeApi,
      keyField: 'key',
      valueField: 'targetValue',
      labelField: 'label',
      childrenField: 'children',
      params: { loadMode: 'full' },
    }
  }

  function buildDesignerFieldMap(fields = []) {
    return new Map((Array.isArray(fields) ? fields : [])
      .map(field => [field.field || field.fieldCode, field])
      .filter(([fieldCode]) => fieldCode))
  }

  function buildDesignerColumns(zone = {}, fieldMap = new Map()) {
    return resolveDesignerZoneRefs(zone, fieldMap).filter((fieldCode) => {
      const setting = zone.props?.fieldSettings?.[fieldCode] || {}
      return setting.visible !== false
    }).map((fieldCode) => {
      const field = fieldMap.get(fieldCode) || {}
      const setting = zone.props?.fieldSettings?.[fieldCode] || {}
      return {
        key: fieldCode,
        field: fieldCode,
        title: resolveListFieldTitle(field, setting, fieldCode),
        minWidth: Number(setting.width || field.width || 110),
        align: setting.align || undefined,
        ellipsis: { tooltip: true },
      }
    })
  }

  function buildDesignerSearchSchema(zone = {}, fieldMap = new Map(), schema = localSchema.value) {
    // 优先用 AiCrudPage.searchFieldRefs（列表设计器查询条件事实来源）
    const crud = (schema?.listGridLayout?.items || schema?.pages?.find(page => page?.pageKey === 'list')?.gridLayout?.items || [])
      .find(item => item?.blockType === 'AiCrudPage')
    const hasExplicitSearchRefs = Object.prototype.hasOwnProperty.call(crud?.props || {}, 'searchFieldRefs')
    const refs = hasExplicitSearchRefs
      ? (Array.isArray(crud.props.searchFieldRefs) ? crud.props.searchFieldRefs : [])
      : (Array.isArray(zone.fieldRefs) ? zone.fieldRefs : [])
    // 搜索区仅在用户显式配置了搜索字段时才生成搜索表单项，
    // 避免新建列表页时自动把对象全部字段填充为搜索输入框。
    if (!refs.length)
      return []
    const settings = hasExplicitSearchRefs
      ? (crud.props?.searchFieldSettings || zone.props?.fieldSettings || {})
      : (zone.props?.fieldSettings || {})
    return refs.filter(fieldCode => fieldMap.has(fieldCode)).map((fieldCode) => {
      const field = fieldMap.get(fieldCode) || {}
      const setting = settings[fieldCode] || {}
      return buildDesignerRuntimeField(field, setting, 'search')
    })
  }

  function buildDesignerEditSchema(zone = {}, fieldMap = new Map()) {
    const refs = resolveDesignerZoneRefs(zone, fieldMap)
    const settings = zone.props?.fieldSettings || {}
    const fields = refs.map((fieldCode) => {
      const field = fieldMap.get(fieldCode) || {}
      return buildDesignerRuntimeField(field, settings[fieldCode] || {}, 'form')
    })
    const layout = hydrateDesignerFormLayout(zone.props?.formLayout || [], new Map(fields.map(field => [field.field, field])))
    return layout.length ? layout : fields
  }

  function resolveDesignerZoneRefs(zone = {}, fieldMap = new Map()) {
    const refs = Array.isArray(zone.fieldRefs) && zone.fieldRefs.length
      ? zone.fieldRefs
      : Array.from(fieldMap.keys())
    return refs.filter(fieldCode => fieldMap.has(fieldCode))
  }

  function buildDesignerRuntimeField(field = {}, setting = {}, mode = 'form') {
    const fieldCode = field.field || field.fieldCode || ''
    const type = normalizeDesignerRuntimeFieldType(setting.componentType || field.componentType || field.dataType)
    const optionSource = setting.optionSource || field.optionSource || setting.props?.optionSource || field.props?.optionSource
      || field.basicProps?.optionSource
      // 查询区不擅自拼本表 /tree；表单预览仍可对无选项源的 treeSelect 兜底
      || (mode !== 'search' && type === 'treeSelect' ? buildDesignerSelfTreeOptionSource() : undefined)
    const runtimeField = {
      field: fieldCode,
      label: setting.label || field.label || field.fieldName || fieldCode,
      type,
      placeholder: setting.placeholder || field.placeholder || (['treeSelect', 'orgTreeSelect', 'regionTreeSelect', 'select', 'dictSelect', 'userSelect', 'cascader'].includes(type)
        ? `请选择${field.label || fieldCode}`
        : `请输入${field.label || fieldCode}`),
      span: Number(setting.span || field.span || 1),
      clearable: true,
      options: setting.options || field.options || [],
      dictType: setting.dictType || field.dictType || '',
      optionSource,
      props: {
        ...(field.props || {}),
        ...(setting.props || {}),
        ...(optionSource ? { optionSource } : {}),
      },
    }
    ;[
      'labelWidth',
      'required',
      'requiredMessage',
      'rules',
      'trigger',
      'readonly',
      'disabled',
      'defaultValue',
      'componentStyle',
      'componentClass',
      'formItemStyle',
      'formItemClass',
      'showLabel',
    ].forEach((key) => {
      if (setting[key] !== undefined)
        runtimeField[key] = setting[key]
    })
    return runtimeField
  }

  function hydrateDesignerFormLayout(layout = [], fieldMap = new Map(), usedFields = new Set()) {
    return (Array.isArray(layout) ? layout : [])
      .map(node => hydrateDesignerFormLayoutNode(node, fieldMap, usedFields))
      .filter(Boolean)
  }

  function hydrateDesignerFormLayoutNode(node = {}, fieldMap = new Map(), usedFields = new Set()) {
    if (!node || typeof node !== 'object')
      return null
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
    const children = hydrateDesignerFormLayout(node.children || [], fieldMap, usedFields)
    if (!children.length && !isStandaloneDesignerLayoutNode(node))
      return null
    return {
      ...node,
      children,
    }
  }

  function isStandaloneDesignerLayoutNode(node = {}) {
    return ['divider', 'groupTitle', 'button'].includes(node.nodeType || node.componentKey || node.type)
  }

  function normalizeDesignerRuntimeFieldType(type = '') {
    // UI 组件类型优先：treeSelect 存 bigint 时不能先被 dataType 分支吃掉
    if (['textarea', 'select', 'dictSelect', 'checkbox', 'radio', 'switch', 'treeSelect', 'userSelect', 'orgTreeSelect', 'regionTreeSelect', 'cascader'].includes(type))
      return type
    if (['int', 'bigint', 'decimal', 'number', 'inputNumber'].includes(type))
      return 'number'
    if (['datetime', 'date', 'time'].includes(type))
      return type
    return 'input'
  }

  function syncDesignerDraft() {
    const schema = normalizeSchemaForSave(resolveSchema(localSchema.value, effectiveModelSchema.value))
    const viewSchema = buildCurrentViewSchema(schema)
    setLocalSchema(schema, { external: true })
    emit('update:modelValue', cloneSchema(schema))
    emit('update:viewSchema', cloneSchema(viewSchema))
    return {
      dirty: !isSameSchema(schema, props.modelValue) || !isSameSchema(viewSchema, props.viewSchema || {}),
      pageSchema: cloneSchema(schema),
      viewSchema: cloneSchema(viewSchema),
    }
  }

  function normalizeSchemaForSave(schema = localSchema.value) {
    const resolved = ensureGridListSchema(resolveSchema(schema, effectiveModelSchema.value), effectiveModelSchema.value)
    const listPage = (resolved.pages || []).find(page => page.pageKey === 'list')
    const sourceGrid = listPage?.gridLayout || resolved.listGridLayout
    if (!sourceGrid)
      return { ...resolved, listLayoutMode: 'grid' }
    const syncedGrid = syncGridLayoutWithModel(sourceGrid, effectiveModelSchema.value, { layoutType: resolved.layoutType })
    return {
      ...resolved,
      listLayoutMode: 'grid',
      listGridLayout: syncedGrid,
      pages: updateDesignerPageGrid(resolved.pages || [], 'list', syncedGrid),
      zones: applyGridLayoutToZones(resolved.zones || [], syncedGrid, effectiveModelSchema.value),
    }
  }

  function resolveSchema(pageSchema, modelSchema) {
    const source = cloneSchema(pageSchema || createDefaultPageSchema(modelSchema))
    const layoutType = inferLayoutType(source, modelSchema)
    const schema = syncPageSchemaWithModel(
      {
        ...source,
        layoutType,
      },
      modelSchema,
    )
    const gridSchema = ensureGridListSchema({
      ...schema,
      layoutType,
    }, modelSchema)
    return {
      ...gridSchema,
      pages: ensureDesignerPages(gridSchema, modelSchema),
    }
  }

  function ensureGridListSchema(schema = {}, modelSchema = effectiveModelSchema.value) {
    const listPageGrid = (schema.pages || []).find(page => page?.pageKey === 'list')?.gridLayout
    const sourceGrid = listPageGrid || schema.listGridLayout
    const fallbackGrid = sourceGrid?.items?.length && isStandardListGridLayout(sourceGrid)
      ? sourceGrid
      : createDefaultListGridLayout(modelSchema, { layoutType: schema.layoutType })
    const syncedGrid = syncGridLayoutWithModel(fallbackGrid, modelSchema, { layoutType: schema.layoutType })
    return {
      ...schema,
      listLayoutMode: 'grid',
      listGridLayout: syncedGrid,
      pages: updateDesignerPageGrid(schema.pages || [], 'list', syncedGrid),
      zones: applyGridLayoutToZones(schema.zones || [], syncedGrid, modelSchema),
    }
  }

  function isStandardListGridLayout(gridLayout = {}) {
    return Array.isArray(gridLayout.items) && gridLayout.items.some(item => item?.blockType === 'AiCrudPage')
  }

  function ensureDesignerPages(schema, modelSchema) {
    const sourcePages = Array.isArray(schema.pages) ? schema.pages : []
    const listPage = sourcePages.find(page => page.pageKey === 'list')
    const listGridLayout = listPage?.gridLayout
      || schema.listGridLayout
      || createDefaultListGridLayout(modelSchema, { layoutType: schema.layoutType })
    const pages = [
      normalizeDesignerPage(listPage, {
        pageKey: 'list',
        pageName: '列表页',
        pageType: 'list',
        routePath: '',
        description: '主列表页面',
        params: [],
        detailMethod: 'get',
        detailApi: '',
        detailDataField: 'data',
        gridLayout: listGridLayout,
      }),
      ...sourcePages
        .filter(page => !['list', 'detail'].includes(page.pageKey))
        .map(page => normalizeDesignerPage(page, {
          pageKey: page.pageKey,
          pageName: page.pageName || '自定义页面',
          pageType: normalizeDesignerPageType(page.pageType || 'custom'),
          routePath: page.routePath || '',
          description: page.description || '',
          params: page.params || [],
          detailMethod: page.detailMethod || 'get',
          detailApi: page.detailApi || '',
          detailDataField: page.detailDataField || 'data',
          gridLayout: page.gridLayout || { cols: 12, rowHeight: 32, gap: 8, designWidth: LIST_PAGE_DESIGN_WIDTH, layoutType: schema.layoutType, items: [] },
        })),
    ]
    if (!pages.some(page => page.pageKey === activePageKey.value))
      activePageKey.value = 'list'
    return pages
  }

  function normalizeDesignerPage(page, defaults) {
    return {
      ...defaults,
      ...(page || {}),
      pageKey: page?.pageKey || defaults.pageKey,
      pageName: page?.pageName || defaults.pageName,
      pageType: normalizeDesignerPageType(page?.pageType || defaults.pageType || 'custom'),
      routePath: page?.routePath ?? defaults.routePath ?? '',
      description: page?.description ?? defaults.description ?? '',
      params: Array.isArray(page?.params) ? page.params : defaults.params || [],
      detailMethod: page?.detailMethod || defaults.detailMethod || 'get',
      detailApi: page?.detailApi ?? defaults.detailApi ?? '',
      detailDataField: page?.detailDataField || defaults.detailDataField || 'data',
      gridLayout: page?.gridLayout || defaults.gridLayout,
    }
  }

  function updateDesignerPageGrid(pages = [], pageKey = 'list', gridLayout) {
    if (!pages.some(page => page.pageKey === pageKey)) {
      return [
        ...pages,
        {
          pageKey,
          pageName: pageKey === 'list' ? '列表页' : '页面',
          pageType: pageKey === 'list' ? 'list' : 'custom',
          routePath: '',
          description: '',
          params: [],
          detailMethod: 'get',
          detailApi: '',
          detailDataField: 'data',
          gridLayout,
        },
      ]
    }
    return pages.map(page => page.pageKey === pageKey
      ? { ...page, gridLayout }
      : page)
  }

  function setLocalSchema(schema, options = {}) {
    if (isSameSchema(schema, localSchema.value))
      return
    if (!options.external && options.recordHistory !== false) {
      pushHistorySnapshot(localSchema.value)
      redoStack.value = []
    }
    mut.applyingExternalSchema = !!options.external
    localSchema.value = schema
  }

  function pushHistorySnapshot(schema) {
    undoStack.value = [...undoStack.value, cloneSchema(schema)].slice(-HISTORY_LIMIT)
  }

  function undoSchema() {
    if (!canUndo.value)
      return
    const currentSchema = cloneSchema(localSchema.value)
    const previousSchema = undoStack.value[undoStack.value.length - 1]
    undoStack.value = undoStack.value.slice(0, -1)
    redoStack.value = [currentSchema, ...redoStack.value].slice(0, HISTORY_LIMIT)
    setLocalSchema(previousSchema, { recordHistory: false })
  }

  function redoSchema() {
    if (!canRedo.value)
      return
    const currentSchema = cloneSchema(localSchema.value)
    const nextSchema = redoStack.value[0]
    redoStack.value = redoStack.value.slice(1)
    pushHistorySnapshot(currentSchema)
    setLocalSchema(nextSchema, { recordHistory: false })
  }

  function handleListDesignerShortcut(event) {
    const key = event.key?.toLowerCase?.()
    const isUndoKey = (event.metaKey || event.ctrlKey) && !event.shiftKey && key === 'z'
    const isRedoKey = (event.metaKey || event.ctrlKey) && ((event.shiftKey && key === 'z') || key === 'y')
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

  function resolveListPagePreviewLayout() {
    const pages = Array.isArray(localSchema.value?.pages) ? localSchema.value.pages : []
    const listPage = pages.find(page => page?.pageKey === 'list' || page?.pageType === 'list') || null
    const persisted = listPage?.gridLayout || localSchema.value?.listGridLayout || {}
    return Array.isArray(persisted?.items) ? JSON.parse(JSON.stringify(persisted)) : { items: [] }
  }

  async function openLocalPreview() {
    // 先把画布未发出的 layout 刷回 localSchema，再落盘，最后开预览
    const designer = listGridDesignerRef.value
    const designerStore = useListDesignerStore()
    designer?.flushDeferredLayoutEmit?.()
    if (props.objectId) {
      try {
        await saveLayout()
      }
      catch {
        return
      }
    }
    // 仅 pageKey=list 才采信画布 live；pageType=list 但 key 非 list 的异常页视为二级页
    const onListPage = activePageKey.value === 'list'
      || activeDesignerPage.value?.pageKey === 'list'
    const snapshot = onListPage && typeof designer?.captureLayoutSnapshot === 'function'
      ? designer.captureLayoutSnapshot()
      : null
    const storeLayout = onListPage ? designerStore.layout : null
    const exposedLayout = onListPage ? designer?.localLayout : null
    // 多页面时预览必须锁定「列表页」布局；禁止用页面2/弹窗页画布（否则会串出 action-button 等）
    const persisted = resolveListPagePreviewLayout()
    const liveLayout = onListPage
      ? (pickLivePreviewLayout(snapshot, storeLayout, exposedLayout)
        || (Array.isArray(persisted?.items) && persisted.items.length ? persisted : null))
      : null
    const source = (Array.isArray(liveLayout?.items) && liveLayout.items.length)
      ? liveLayout
      : persisted
    // 预览前同步并修理 AiCrudPage.fieldRefs，避免查询字段污染列表列
    const syncedSource = syncGridLayoutWithModel(
      source,
      effectiveModelSchema.value,
      { layoutType: localSchema.value.layoutType },
    )
    listPreviewLayout.value = normalizeListPreviewLayout(filterDesignerGridBlocks(syncedSource))
    listPreviewLayoutKey.value += 1
    listPreviewVisible.value = true
  }

  /** 模板回落：永远用列表页，禁止 designerPreviewGridLayout（会跟当前激活的页面2） */
  const listOnlyPreviewLayout = computed(() => {
    const snapshot = listPreviewLayout.value
    if (Array.isArray(snapshot?.items) && snapshot.items.length)
      return snapshot
    const persisted = resolveListPagePreviewLayout()
    const synced = syncGridLayoutWithModel(
      persisted,
      effectiveModelSchema.value,
      { layoutType: localSchema.value?.layoutType || 'simple-crud' },
    )
    return normalizeListPreviewLayout(filterDesignerGridBlocks(synced))
  })

  /** 有伴生块时本地预览改走 RuntimeListGridFlow，避免绝对画布裁掉 CRUD 分页 */
  const listPreviewRuntimeModel = computed(() => {
    // 回落也只能用列表页，禁止用当前激活的「页面2」画布
    const layout = listOnlyPreviewLayout.value
    if (!isRichListGridLayout(layout))
      return null
    return resolveRuntimeListGridModel(
      {
        listGridLayout: layout,
        layoutType: localSchema.value?.layoutType || 'simple-crud',
      },
      {
        modelSchema: effectiveModelSchema.value,
        layoutType: localSchema.value?.layoutType || 'simple-crud',
        pageKey: 'list',
        // 与列表画布一致：不要强制实时预览，否则查询字段/工具栏会和设计态分叉
        forceLivePreview: false,
      },
    )
  })

  function pickRichestPreviewLayout(...candidates) {
    let best = null
    let bestCount = -1
    for (const candidate of candidates) {
      if (!candidate || typeof candidate !== 'object')
        continue
      const items = candidate.items
      if (!Array.isArray(items) || !items.length)
        continue
      if (items.length > bestCount) {
        best = candidate
        bestCount = items.length
      }
    }
    return best ? JSON.parse(JSON.stringify(best)) : null
  }

  /** 按候选顺序取第一个有效布局（用于删除后预览，不能按块数最多选） */
  function pickLivePreviewLayout(...candidates) {
    for (const candidate of candidates) {
      if (!candidate || typeof candidate !== 'object')
        continue
      const items = candidate.items
      if (!Array.isArray(items) || !items.length)
        continue
      return JSON.parse(JSON.stringify(candidate))
    }
    return null
  }

  /** 预览前把 heightMode=full 压成 fixed，避免 AiCrudPage 铺满盖住其它组件 */
  function normalizeListPreviewLayout(grid = {}) {
    if (!grid || typeof grid !== 'object')
      return grid
    const items = Array.isArray(grid.items) ? grid.items : []
    return {
      ...grid,
      items: items.map((item) => {
        const style = item?.props?.style || {}
        if (style.heightMode !== 'full')
          return item
        return {
          ...item,
          props: {
            ...(item.props || {}),
            style: {
              ...style,
              heightMode: 'fixed',
            },
          },
        }
      }),
    }
  }

  function omitTreeRuntimeProps(props = {}) {
    const { treeConfig, ...rest } = props || {}
    return rest
  }

  function inferLayoutType(pageSchema, modelSchema) {
    if (isTreeLayout(pageSchema, modelSchema))
      return 'tree-crud'
    if (isRelationLayout(pageSchema, modelSchema))
      return 'master-detail-crud'
    return 'simple-crud'
  }

  function isTreeLayout(pageSchema = {}, modelSchema = {}) {
    const listPageGridLayout = Array.isArray(pageSchema.pages)
      ? pageSchema.pages.find(page => page?.pageKey === 'list')?.gridLayout
      : null
    const hasTreeGridBlock = Boolean(
      pageSchema.listGridLayout?.items?.some(item => item.blockType === 'tree-panel')
      || listPageGridLayout?.items?.some(item => item.blockType === 'tree-panel'),
    )
    // 左树右表：仅当明确选了 tree-crud 模板，或画布已有 tree-panel。
    // 嵌入式树表（treeConfig.enabled / appType=TREE）不切换布局，避免中间画布被挤向右侧。
    return pageSchema.layoutType === 'tree-crud' || hasTreeGridBlock
  }

  function isRelationLayout(pageSchema = {}, modelSchema = {}) {
    return modelSchema?.appType === 'MASTER_DETAIL'
      || (pageSchema.modelRefs || []).some(ref => ref && !ref.primary)
      || (modelSchema.pageModelRefs || []).some(ref => ref && !ref.primary)
      || (modelSchema.fields || []).some(field => field?.modelCode && field.field !== (field.sourceField || field.field))
  }

  function resolveLayoutModeLabel(layoutType) {
    if (layoutType === 'tree-crud')
      return '自由画布 · 已套用左树右表'
    if (layoutType === 'master-detail-crud')
      return '自由画布 · 已启用关联数据'
    return '自由画布'
  }

  function updateTreeZone(zones = [], enabled, modelSchema = effectiveModelSchema.value) {
    const defaultTreeConfig = resolveDefaultTreeConfig(modelSchema || {}, modelSchema?.treeConfig || {})
    return zones.map((zone) => {
      if (zone.zoneKey !== 'table')
        return zone
      const props = { ...(zone.props || {}) }
      if (enabled) {
        // 左树右表：右表是平铺列表，默认不要「添加下级」
        props.treeConfig = {
          ...defaultTreeConfig,
          ...(props.treeConfig || {}),
          enabled: true,
          enableTreeAddChild: false,
        }
        props.enableTreeAddChild = false
      }
      else {
        delete props.treeConfig
        props.enableTreeAddChild = false
      }
      return {
        ...zone,
        props,
      }
    })
  }

  function resolveDesignModelSchema(pageSchema, modelSchema) {
    const refs = mergePrimaryModelRef(pageSchema?.modelRefs || [], modelSchema || {})
    return buildPageDesignModelSchema(modelSchema || {}, refs)
  }

  function mergePrimaryModelRef(modelRefs, modelSchema) {
    if (!Array.isArray(modelRefs) || !modelRefs.length)
      return []
    const primaryRef = createPageModelRef({ modelSchema }, { primary: true })
    const refs = modelRefs.map(ref => ref?.primary
      ? {
          ...ref,
          modelCode: primaryRef.modelCode || ref.modelCode,
          modelName: primaryRef.modelName || ref.modelName,
          tableName: primaryRef.tableName || ref.tableName,
          relations: primaryRef.relations?.length ? primaryRef.relations : ref.relations,
          fields: primaryRef.fields,
        }
      : ref)
    if (!refs.some(ref => ref?.primary))
      refs.unshift(primaryRef)
    return refs
  }

  function toPageField(field) {
    return {
      ...field,
      field: field.field || field.fieldCode,
      label: field.label || field.fieldName || field.fieldCode,
      comment: field.remark || field.fieldName,
      columnName: field.columnName,
      dataType: field.dataType,
      componentType: field.componentType,
      dictType: field.dictType,
      required: field.required,
      systemField: field.systemField,
      readonly: field.readonly,
      searchable: field.searchable,
      listVisible: field.listVisible,
      formVisible: field.formVisible,
      fieldStatus: field.fieldStatus,
      basicProps: { ...(field.basicProps || {}) },
      advancedProps: { ...(field.advancedProps || {}) },
    }
  }

  expose?.({
    saveLayout,
    syncDesignerDraft,
  })

  onMounted(() => {
    window.addEventListener('keydown', handleListDesignerShortcut)
    window.addEventListener('forge-list-designer:preview-current-list', handlePreviewCurrentListEvent)
    // 初始规范化后的schema若与已保存草稿不一致（如旧草稿缺grid布局被重建），
    // 标记为脏，否则用户看到的配置会因保存按钮禁用而无法落盘。
    if (!isSameSchema(localSchema.value, props.modelValue))
      emit('dirtyChange', true)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleListDesignerShortcut)
    window.removeEventListener('forge-list-designer:preview-current-list', handlePreviewCurrentListEvent)
  })

  function handlePreviewCurrentListEvent() {
    void openLocalPreview()
  }
  __impl.designerActionToListAction = designerActionToListAction
  __impl.listActionToDesignerAction = listActionToDesignerAction
  __impl.buildDesignerActionConfig = buildDesignerActionConfig
  __impl.normalizeListCustomActions = normalizeListCustomActions
  __impl.normalizeListCustomAction = normalizeListCustomAction
  __impl.normalizeListActionConfig = normalizeListActionConfig
  __impl.normalizeActionParams = normalizeActionParams
  __impl.toDesignerActionParams = toDesignerActionParams
  __impl.collectSchemaCustomActions = collectSchemaCustomActions
  __impl.deduplicateListActions = deduplicateListActions
  __impl.isInvalidDesignerApiAction = isInvalidDesignerApiAction
  __impl.normalizeDesignerActionType = normalizeDesignerActionType
  __impl.designerActionTypeToListType = designerActionTypeToListType
  __impl.listActionTypeToDesignerType = listActionTypeToDesignerType
  __impl.normalizeListActionType = normalizeListActionType
  __impl.normalizeListActionPosition = normalizeListActionPosition
  __impl.normalizeDesignerActionPosition = normalizeDesignerActionPosition
  __impl.resolveDefaultActionButtonType = resolveDefaultActionButtonType
  __impl.resolveDefaultListButtonType = resolveDefaultListButtonType
  __impl.resolveListActionRoutePath = resolveListActionRoutePath
  __impl.normalizeActionApiMethod = normalizeActionApiMethod
  __impl.normalizeActionCode = normalizeActionCode
  __impl.parsePlainObject = parsePlainObject
  __impl.resolveDesignerDefaultApiValues = resolveDesignerDefaultApiValues
  __impl.buildDesignerSelfTreeOptionSource = buildDesignerSelfTreeOptionSource
  __impl.buildDesignerFieldMap = buildDesignerFieldMap
  __impl.buildDesignerColumns = buildDesignerColumns
  __impl.buildDesignerSearchSchema = buildDesignerSearchSchema
  __impl.buildDesignerEditSchema = buildDesignerEditSchema
  __impl.resolveDesignerZoneRefs = resolveDesignerZoneRefs
  __impl.buildDesignerRuntimeField = buildDesignerRuntimeField
  __impl.hydrateDesignerFormLayout = hydrateDesignerFormLayout
  __impl.hydrateDesignerFormLayoutNode = hydrateDesignerFormLayoutNode
  __impl.isStandaloneDesignerLayoutNode = isStandaloneDesignerLayoutNode
  __impl.normalizeDesignerRuntimeFieldType = normalizeDesignerRuntimeFieldType
  __impl.syncDesignerDraft = syncDesignerDraft
  __impl.normalizeSchemaForSave = normalizeSchemaForSave
  __impl.resolveSchema = resolveSchema
  __impl.ensureGridListSchema = ensureGridListSchema
  __impl.isStandardListGridLayout = isStandardListGridLayout
  __impl.ensureDesignerPages = ensureDesignerPages
  __impl.normalizeDesignerPage = normalizeDesignerPage
  __impl.updateDesignerPageGrid = updateDesignerPageGrid
  __impl.setLocalSchema = setLocalSchema
  __impl.pushHistorySnapshot = pushHistorySnapshot
  __impl.undoSchema = undoSchema
  __impl.redoSchema = redoSchema
  __impl.handleListDesignerShortcut = handleListDesignerShortcut
  __impl.openLocalPreview = openLocalPreview
  __impl.omitTreeRuntimeProps = omitTreeRuntimeProps
  __impl.inferLayoutType = inferLayoutType
  __impl.isTreeLayout = isTreeLayout
  __impl.isRelationLayout = isRelationLayout
  __impl.resolveLayoutModeLabel = resolveLayoutModeLabel
  __impl.updateTreeZone = updateTreeZone
  __impl.resolveDesignModelSchema = resolveDesignModelSchema
  __impl.mergePrimaryModelRef = mergePrimaryModelRef
  __impl.toPageField = toPageField

  function hydrateInitialState() {
    const next = resolveSchema(props.modelValue, resolveDesignModelSchema(props.modelValue, baseModelSchema.value))
    setLocalSchema(next, { external: true })
    const mappedActions = normalizeDesignerActionsForList(props.designerActions)
    const fallbackActions = mappedActions.length ? mappedActions : collectSchemaCustomActions(localSchema.value)
    listCustomActions.value = fallbackActions
    templateSelectValue.value = resolveTemplateSelectValue(localSchema.value.layoutType)
  }
  __impl.hydrateInitialState = hydrateInitialState

  return {
    ...deps,
    listOnlyPreviewLayout,
    listPreviewRuntimeModel,
  }
}
