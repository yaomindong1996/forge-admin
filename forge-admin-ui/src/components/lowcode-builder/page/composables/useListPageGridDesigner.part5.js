/** Auto-split part 5 of ListPageGridDesigner setup. */
import { nextTick } from 'vue'
import { enabledApiConfigs } from '@/api/business-app'
import { resolveWidgetRenderMode } from '@/components/lowcode-builder/shared/widget-binding-slots'
import { resolveSelectedFieldRefs } from '../fieldDrawerConfig'
import {
  isListFieldSelectable,
  resolveListFieldTitle,
} from '../page-schema'
import {
  actionBehaviorOptions,
  apiMethodOptions,
} from '../listDesignerOptions'
import {
  BitableAttachmentIcon,
  BitableCalendarIcon,
  BitableLookupIcon,
  BitableMailIcon,
  BitableMemberIcon,
  BitableNumberIcon,
  BitablePhoneIcon,
  BitableSelectIcon,
  BitableStyleIcon,
  BitableTodoIcon,
} from '../bitableIcons'
export function useListPageGridDesignerPart5(props, emit, deps = {}, expose = undefined) {
  const {
    __impl,
    designerStore,
    canvasScrollRef,
    customActionModalOpen,
    activeActionIndex,
    propertyPanelTab,
    propertyPanelRef,
    propertyKeyword,
    propertySearchTabIndex,
    propertySearchKeywordRegistry,
    canvasFocusMode,
    activeTabKey,
    selectedBlock,
    externalCustomActionsEnabled,
    customActionList,
    activeAction,
    apiConfigs,
    apiConfigLoading,
    apiConfigLoaded,
    systemMenuPages,
    systemMenuPageLoading,
    systemMenuPageLoaded,
    systemMenuPageTargetOptions,
    crudTablePanelFields,
    patchBlock,
    patchBlockProps,
    localLayout,
    flushDeferredLayoutEmit,
  } = deps
function handleFieldDrawerPatchProps({ blockId, patch } = {}) {
  if (!blockId)
    return
  patchBlockProps(blockId, patch)
}

function handleFieldDrawerPatchBlock({ blockId, patch } = {}) {
  if (!blockId)
    return
  patchBlock(blockId, patch)
}
function resolveBlockFieldCount(block = selectedBlock.value, zoneKey = 'table') {
  if (block?.blockType === 'AiCrudPage' && zoneKey === 'table') {
    const refs = new Set(resolveSelectedFieldRefs(block, 'table', props.fields))
    return props.fields.filter(field => refs.has(field.field) && isCrudTableFieldVisible(field.field, block)).length
  }
  return resolveSelectedFieldRefs(block, zoneKey, props.fields).length
}
function handleCrudTableFieldReorder(rows = []) {
  if (!selectedBlock.value?.id)
    return
  const nextRefs = resolveCrudTablePanelFieldRefs(rows)
  patchBlock(selectedBlock.value.id, {
    fieldRefs: nextRefs,
    props: {
      ...(selectedBlock.value.props || {}),
      fieldSettings: buildCrudTableVisibilitySettings(nextRefs),
    },
  })
}

function toggleCrudTableField(fieldName = '', visible = true) {
  if (!selectedBlock.value?.id || !fieldName)
    return
  const nextRefs = resolveCrudTablePanelFieldRefs()
  patchBlock(selectedBlock.value.id, {
    fieldRefs: nextRefs,
    props: {
      ...(selectedBlock.value.props || {}),
      fieldSettings: buildCrudTableVisibilitySettings(nextRefs, { [fieldName]: visible === true }),
    },
  })
}

function resolveCrudFieldInlineTitle(field = {}) {
  const setting = selectedBlock.value?.props?.fieldSettings?.[field.field] || {}
  return resolveListFieldTitle(field, setting, field.field)
}

function resolveCrudTablePanelFieldRefs(rows = crudTablePanelFields.value) {
  return Array.from(new Set((rows || [])
    .map(row => row?.field)
    .filter(Boolean)))
}

function isCrudTableFieldVisible(fieldName = '', block = selectedBlock.value) {
  if (!fieldName || !block)
    return false
  const refs = resolveSelectedFieldRefs(block, 'table', props.fields)
  const setting = block.props?.fieldSettings?.[fieldName] || {}
  return refs.includes(fieldName) && setting.visible !== false
}

function buildCrudTableVisibilitySettings(nextRefs = [], overrides = {}) {
  const block = selectedBlock.value || {}
  const previousRefs = new Set(resolveSelectedFieldRefs(block, 'table', props.fields))
  const currentSettings = block.props?.fieldSettings || {}
  const nextSettings = { ...currentSettings }
  nextRefs.forEach((fieldName) => {
    const previous = currentSettings[fieldName] || {}
    const nextVisible = Object.prototype.hasOwnProperty.call(overrides, fieldName)
      ? overrides[fieldName] === true
      : previous.visible === false
        ? false
        : previousRefs.has(fieldName)
    nextSettings[fieldName] = {
      ...previous,
      visible: nextVisible,
    }
  })
  return nextSettings
}

function resolveCrudFieldIconComponent(field = {}) {
  const componentType = String(field.componentType || field.type || '').toLowerCase()
  const dataType = String(field.dataType || '').toLowerCase()
  const fieldName = String(field.field || '').toLowerCase()
  const text = `${componentType} ${dataType} ${fieldName}`
  if (field.dictType || ['select', 'dictselect', 'radio', 'radiogroup', 'switch', 'selector'].some(type => text.includes(type)))
    return BitableSelectIcon
  if (['checkbox', 'checkboxgroup', 'todo', 'boolean'].some(type => text.includes(type)))
    return BitableTodoIcon
  if (['number', 'inputnumber', 'counter', 'int', 'long', 'bigint', 'decimal', 'double', 'float'].some(type => text.includes(type)))
    return BitableNumberIcon
  if (['date', 'time', 'calendar', 'localdatetime', 'timestamp'].some(type => text.includes(type)))
    return BitableCalendarIcon
  if (['phone', 'mobile', 'tel'].some(type => text.includes(type)))
    return BitablePhoneIcon
  if (['email', 'mail'].some(type => text.includes(type)))
    return BitableMailIcon
  if (['file', 'image', 'upload', 'attachment'].some(type => text.includes(type)))
    return BitableAttachmentIcon
  if (['user', 'member', 'owner', 'creator', 'createby', 'updateby'].some(type => text.includes(type)))
    return BitableMemberIcon
  if (['lookup', 'relation', 'reference', 'org', 'dept', 'region', 'tree'].some(type => text.includes(type)))
    return BitableLookupIcon
  return BitableStyleIcon
}

function applySelectedTableGlobalAlign(value) {
  if (!selectedBlock.value || !['data-table', 'AiCrudPage', 'AiTable'].includes(selectedBlock.value.blockType))
    return
  const align = ['left', 'center', 'right'].includes(value) ? value : 'left'
  const nextSettings = { ...(selectedBlock.value.props?.fieldSettings || {}) }
  const tableRefs = resolveSelectedFieldRefs(selectedBlock.value, 'table', props.fields)
  const fieldRefs = tableRefs.length
    ? tableRefs
    : props.fields.filter(field => isListFieldSelectable(field, 'table')).map(field => field.field)
  fieldRefs.forEach((fieldName) => {
    nextSettings[fieldName] = {
      ...(nextSettings[fieldName] || {}),
      align,
    }
  })
  patchBlockProps(selectedBlock.value.id, {
    globalAlign: align,
    fieldSettings: nextSettings,
  })
}

function normalizeTableRowGap(value) {
  return Math.max(0, Math.min(32, Number(value ?? 8)))
}

// Metric editing
function updateMetric(idx, patch) {
  const list = [...(selectedBlock.value?.props?.metrics || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}
function addMetric() {
  const list = [...(selectedBlock.value?.props?.metrics || []), { label: '指标', value: '0', trend: '' }]
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}
function removeMetric(idx) {
  const list = [...(selectedBlock.value?.props?.metrics || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}

const quickEntryToneOptions = [
  { label: '主色', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '信息', value: 'info' },
]

function isQuickEntryListMode(block = selectedBlock.value) {
  const binding = block?.props?.dataBinding || {}
  return resolveWidgetRenderMode(binding, 'quick-entry') === 'list'
}

function isChartListMode(block = selectedBlock.value) {
  const blockType = block?.blockType
  if (!['bar-chart', 'line-chart', 'area-chart', 'pie-chart'].includes(blockType))
    return false
  const binding = block?.props?.dataBinding || {}
  return resolveWidgetRenderMode(binding, blockType) === 'list'
}

function updateQuickEntryItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.items || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { items: list })
}
function addQuickEntryItem() {
  const list = [...(selectedBlock.value?.props?.items || []), {
    key: `entry_${Date.now()}`,
    title: '新入口',
    description: '',
    icon: 'GridOutline',
    tone: 'primary',
    targetPageId: '',
  }]
  patchBlockProps(selectedBlock.value.id, { items: list })
}
function removeQuickEntryItem(idx) {
  const list = [...(selectedBlock.value?.props?.items || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { items: list })
}

function updateChartCategories(text) {
  const categories = String(text || '')
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
  patchBlockProps(selectedBlock.value.id, { categories })
}

function updateChartSeriesItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.series || [])]
  const next = { ...list[idx] }
  if (Object.prototype.hasOwnProperty.call(patch, 'dataText')) {
    next.data = String(patch.dataText || '')
      .split(/[,，]/)
      .map(item => item.trim())
      .filter(item => item !== '')
      .map(item => Number(item) || 0)
  }
  if (Object.prototype.hasOwnProperty.call(patch, 'name'))
    next.name = patch.name
  if (Object.prototype.hasOwnProperty.call(patch, 'data') && Array.isArray(patch.data))
    next.data = patch.data
  list[idx] = next
  patchBlockProps(selectedBlock.value.id, { series: list })
}
function addChartSeriesItem() {
  const categories = selectedBlock.value?.props?.categories || []
  const list = [...(selectedBlock.value?.props?.series || []), {
    name: `系列${(selectedBlock.value?.props?.series || []).length + 1}`,
    data: categories.map(() => 0),
  }]
  patchBlockProps(selectedBlock.value.id, { series: list })
}
function removeChartSeriesItem(idx) {
  const list = [...(selectedBlock.value?.props?.series || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { series: list })
}

function updateButtonGroupItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.buttons || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { buttons: list })
}
function addButtonGroupItem() {
  const list = [...(selectedBlock.value?.props?.buttons || []), { key: `btn_${Date.now()}`, text: '按钮', type: 'default' }]
  patchBlockProps(selectedBlock.value.id, { buttons: list })
}
function removeButtonGroupItem(idx) {
  const list = [...(selectedBlock.value?.props?.buttons || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { buttons: list })
}

function updateTagItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.tags || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { tags: list })
}
function addTagItem() {
  const list = [...(selectedBlock.value?.props?.tags || []), { label: '标签', type: 'default' }]
  patchBlockProps(selectedBlock.value.id, { tags: list })
}
function removeTagItem(idx) {
  const list = [...(selectedBlock.value?.props?.tags || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { tags: list })
}

function updateStepItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.steps || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { steps: list })
}
function addStepItem() {
  const list = [...(selectedBlock.value?.props?.steps || []), { title: '步骤', description: '' }]
  patchBlockProps(selectedBlock.value.id, { steps: list })
}
function removeStepItem(idx) {
  const list = [...(selectedBlock.value?.props?.steps || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { steps: list })
}

function updateTimelineItem(idx, patch) {
  const list = [...(selectedBlock.value?.props?.items || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { items: list })
}
function addTimelineItem() {
  const list = [...(selectedBlock.value?.props?.items || []), { title: '节点', time: '', content: '' }]
  patchBlockProps(selectedBlock.value.id, { items: list })
}
function removeTimelineItem(idx) {
  const list = [...(selectedBlock.value?.props?.items || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { items: list })
}

function updateOptionItem(propName = 'items', idx, patch) {
  const list = [...(selectedBlock.value?.props?.[propName] || [])]
  list[idx] = { ...(list[idx] || {}), ...patch }
  patchBlockProps(selectedBlock.value.id, { [propName]: list })
}

function updateOptionSource(patch = {}) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    optionSource: {
      ...(selectedBlock.value.props?.optionSource || {}),
      ...patch,
    },
  })
}

function updateWidgetDataBinding(patch = {}) {
  if (!selectedBlock.value)
    return
  const next = {
    ...(selectedBlock.value.props?.dataBinding || {}),
    ...patch,
  }
  if (next.sourceType === 'static')
    next.enabled = false
  else
    next.enabled = true
  patchBlockProps(selectedBlock.value.id, { dataBinding: next })
}

function isWidgetBindingActive(block = null) {
  const binding = block?.props?.dataBinding || {}
  return binding.enabled === true && binding.sourceType && binding.sourceType !== 'static'
}

function isStatsStripListMode(block = null) {
  const binding = block?.props?.dataBinding || {}
  if (!isWidgetBindingActive(block))
    return false
  return resolveWidgetRenderMode(binding, 'stats-strip') === 'list'
}

function isTimelineBound(block = null) {
  return isWidgetBindingActive(block)
}

function addOptionItem(propName = 'items') {
  const fallback = propName === 'options'
    ? { label: '新选项', value: `option_${Date.now()}` }
    : { label: '标签', value: '内容' }
  const list = [...(selectedBlock.value?.props?.[propName] || []), fallback]
  patchBlockProps(selectedBlock.value.id, { [propName]: list })
}

function removeOptionItem(propName = 'items', idx) {
  const list = [...(selectedBlock.value?.props?.[propName] || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { [propName]: list })
}

function resolveBooleanKeys(source = {}, keys = []) {
  return keys.filter(key => source?.[key] === true)
}

function updateLinkLikeTitle(value = '') {
  if (!selectedBlock.value)
    return
  const key = selectedBlock.value.blockType === 'link' ? 'text' : 'title'
  patchBlockProps(selectedBlock.value.id, { [key]: value })
}

function updateLinkLikeUrl(value = '') {
  if (!selectedBlock.value)
    return
  const key = selectedBlock.value.blockType === 'link' ? 'href' : 'src'
  patchBlockProps(selectedBlock.value.id, { [key]: value })
}

function updateCodeColor(value = '') {
  if (!selectedBlock.value)
    return
  if (selectedBlock.value.blockType === 'barcode') {
    patchBlockProps(selectedBlock.value.id, { lineColor: value || '#0f172a' })
    return
  }
  patchBlockProps(selectedBlock.value.id, { foreground: value || '#0f172a' })
}

function clonePlainValue(value) {
  return JSON.parse(JSON.stringify(value || {}))
}

function createCustomActionClientKey(prefix = 'custom_action') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function getCustomActionIdentity(action = {}) {
  if (action && typeof action === 'object')
    return action.clientKey || action.key || ''
  return String(action || '')
}

function addCustomAction(position = 'row') {
  if (!props.customActionsEditable)
    return
  const list = [...customActionList.value]
  const normalizedPosition = ['toolbar', 'row', 'detail'].includes(position) ? position : 'row'
  const clientKey = createCustomActionClientKey()
  list.push({
    clientKey,
    key: clientKey,
    label: '自定义按钮',
    position: normalizedPosition,
    type: 'primary',
    actionType: 'page',
    routePath: '',
    targetPageKey: '',
    targetFormKey: '',
    openTarget: '_self',
    permissionCode: '',
    confirmText: '',
    displayCondition: '',
    successBehavior: 'none',
    successMessage: '',
    failureMessage: '',
    actionConfig: normalizeCustomApiConfig({}),
    params: [],
  })
  persistCustomActionList(list)
  activeActionIndex.value = list.length - 1
  if (!['toolbar', 'row'].includes(position))
    customActionModalOpen.value = true
}

function openCustomActionManager(actionOrIdentity = '') {
  if (!props.customActionsEditable)
    return
  if (!customActionList.value.length) {
    addCustomAction('toolbar')
    activeActionIndex.value = 0
    customActionModalOpen.value = true
    return
  }
  const index = actionOrIdentity ? findCustomActionIndexByIdentity(actionOrIdentity) : -1
  activeActionIndex.value = index >= 0 ? index : 0
  if (isApiCustomAction(customActionList.value[activeActionIndex.value]))
    loadEnabledApiConfigs()
  customActionModalOpen.value = true
}

function createAndEditCustomAction(position = 'toolbar') {
  if (!props.customActionsEditable)
    return
  addCustomAction(position)
  customActionModalOpen.value = true
}

function updateCustomAction(idx, patch) {
  const list = [...customActionList.value]
  list[idx] = { ...list[idx], ...patch }
  persistCustomActionList(list)
}

function findCustomActionIndexByIdentity(actionOrIdentity = '') {
  const identity = getCustomActionIdentity(actionOrIdentity)
  if (!identity)
    return -1
  return customActionList.value.findIndex(action => getCustomActionIdentity(action) === identity)
}

function updateCustomActionByIdentity(actionOrIdentity = '', patch = {}) {
  const index = findCustomActionIndexByIdentity(actionOrIdentity)
  if (index < 0)
    return
  updateCustomAction(index, patch)
}

function toggleCustomActionVisible(action = {}) {
  updateCustomActionByIdentity(action, { visible: action.visible === false })
}

function openCustomActionEditor(actionOrIdentity = '') {
  const index = findCustomActionIndexByIdentity(actionOrIdentity)
  activeActionIndex.value = index
  if (isApiCustomAction(customActionList.value[index]))
    loadEnabledApiConfigs()
  customActionModalOpen.value = index >= 0
}

expose?.({
  openCustomActionManager,
  createAndEditCustomAction,
  flushDeferredLayoutEmit,
  get localLayout() {
    return localLayout?.value
  },
  captureLayoutSnapshot() {
    if (typeof flushDeferredLayoutEmit === 'function')
      flushDeferredLayoutEmit()
    const layout = localLayout?.value
    if (!layout || typeof layout !== 'object' || !Array.isArray(layout.items))
      return null
    return JSON.parse(JSON.stringify(layout))
  },
})

function handleCustomActionGroupReorder(position = 'toolbar', rows = []) {
  const targetPosition = ['toolbar', 'row', 'detail'].includes(position) ? position : 'toolbar'
  const rowKeys = new Set(rows.map(action => getCustomActionIdentity(action)))
  const normalizedRows = rows.map(action => ({ ...action, position: targetPosition }))
  const next = []
  let inserted = false
  customActionList.value.forEach((action) => {
    if ((action.position || 'toolbar') === targetPosition) {
      if (!inserted) {
        next.push(...normalizedRows)
        inserted = true
      }
      return
    }
    if (!rowKeys.has(getCustomActionIdentity(action)))
      next.push(action)
  })
  if (!inserted)
    next.push(...normalizedRows)
  persistCustomActionList(next)
}

function customActionMoreOptions(action = {}) {
  const currentPosition = action.position || 'toolbar'
  return [
    { label: '设置', key: 'configure' },
    {
      label: currentPosition === 'toolbar' ? '移到行按钮' : '移到工具栏',
      key: currentPosition === 'toolbar' ? 'move-row' : 'move-toolbar',
    },
    { label: '复制', key: 'copy' },
    { label: '删除', key: 'delete' },
  ]
}

function handleCustomActionMoreSelect(key = '', action = {}) {
  const index = findCustomActionIndexByIdentity(action)
  if (index < 0)
    return
  if (key === 'configure') {
    openCustomActionEditor(action)
    return
  }
  if (key === 'move-row') {
    updateCustomAction(index, { position: 'row' })
    return
  }
  if (key === 'move-toolbar') {
    updateCustomAction(index, { position: 'toolbar' })
    return
  }
  if (key === 'copy') {
    const list = [...customActionList.value]
    const clientKey = createCustomActionClientKey('custom_action_copy')
    const copied = {
      ...clonePlainValue(list[index]),
      clientKey,
      key: clientKey,
      label: `${list[index].label || '自定义按钮'} 副本`,
    }
    list.splice(index + 1, 0, copied)
    persistCustomActionList(list)
    activeActionIndex.value = index + 1
    return
  }
  if (key === 'delete')
    removeCustomActionByIdentity(action)
}

function removeCustomAction(idx) {
  const list = [...customActionList.value]
  if (idx < 0 || idx >= list.length)
    return
  list.splice(idx, 1)
  persistCustomActionList(list)
  activeActionIndex.value = list.length ? Math.min(idx, list.length - 1) : 0
}

function removeCustomActionByIdentity(actionOrIdentity = '') {
  const identity = getCustomActionIdentity(actionOrIdentity)
  if (!identity)
    return
  const list = customActionList.value.filter(action => getCustomActionIdentity(action) !== identity)
  if (list.length === customActionList.value.length)
    return
  persistCustomActionList(list)
  activeActionIndex.value = list.length ? Math.min(activeActionIndex.value, list.length - 1) : 0
}

function persistCustomActionList(list = []) {
  const normalized = normalizeCustomActionList(list)
  if (externalCustomActionsEnabled.value) {
    emit('update:customActions', normalized)
  }
  if (selectedBlock.value && ['AiCrudPage', 'toolbar', 'data-table', 'AiTable'].includes(selectedBlock.value.blockType)) {
    patchBlockProps(selectedBlock.value.id, { customActions: normalized })
  }
}

function updateActiveCustomAction(patch) {
  if (activeActionIndex.value < 0)
    return
  updateCustomAction(activeActionIndex.value, patch)
}

function updateActiveCustomActionType(value) {
  const actionType = normalizeCustomActionType(value)
  const patch = { actionType }
  if (actionType === 'CALL_API') {
    const nextConfig = normalizeCustomApiConfig(activeAction.value?.actionConfig)
    if (!nextConfig.url && activeAction.value?.routePath)
      nextConfig.url = activeAction.value.routePath
    patch.actionConfig = nextConfig
    patch.openTarget = '_self'
    loadEnabledApiConfigs()
  }
  else if (actionType === 'START_FLOW') {
    patch.actionConfig = { ...(activeAction.value?.actionConfig || {}), useMainFlow: true }
    patch.routePath = ''
    patch.targetPageKey = ''
    patch.openTarget = '_self'
  }
  else if (actionType === 'TRIGGER') {
    patch.actionConfig = { ...(activeAction.value?.actionConfig || {}), triggerCode: activeAction.value?.routePath || '' }
    patch.openTarget = '_self'
  }
  else if (actionType === 'page') {
    patch.routePath = ''
    patch.openTarget = activeAction.value?.openTarget || '_self'
    patch.actionConfig = {
      ...(activeAction.value?.actionConfig || {}),
      navigationMode: 'page',
      targetPath: '',
    }
  }
  else if (actionType === 'route') {
    patch.targetPageKey = ''
    patch.actionConfig = {
      ...(activeAction.value?.actionConfig || {}),
      navigationMode: 'route',
      targetPageKey: '',
    }
  }
  updateActiveCustomAction(patch)
}

function updateActiveActionConfig(patch = {}) {
  const current = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  updateActiveCustomAction({
    actionType: 'CALL_API',
    actionConfig: {
      ...current,
      ...patch,
    },
  })
}

function updateActiveGenericActionConfig(patch = {}) {
  updateActiveCustomAction({
    actionConfig: {
      ...(activeAction.value?.actionConfig || {}),
      ...patch,
    },
    ...(Object.prototype.hasOwnProperty.call(patch, 'triggerCode') ? { routePath: patch.triggerCode || '' } : {}),
  })
}

async function loadEnabledApiConfigs() {
  if (apiConfigLoaded.value || apiConfigLoading.value)
    return
  apiConfigLoading.value = true
  try {
    const res = await enabledApiConfigs()
    apiConfigs.value = Array.isArray(res.data) ? res.data : []
    apiConfigLoaded.value = true
  }
  catch (error) {
    apiConfigs.value = []
    apiConfigLoaded.value = true
    console.warn('[ListPageGridDesigner] API配置不可用，已切换为手工输入模式', error?.message || error)
  }
  finally {
    apiConfigLoading.value = false
  }
}

async function loadSystemMenuPages() {
  if (systemMenuPageLoaded.value || systemMenuPageLoading.value)
    return
  systemMenuPageLoading.value = true
  try {
    const res = await request.get('/system/resource/tree')
    systemMenuPages.value = normalizeResourceTreeResponse(res)
    systemMenuPageLoaded.value = true
  }
  catch (error) {
    systemMenuPages.value = []
    systemMenuPageLoaded.value = true
    console.warn('[ListPageGridDesigner] 系统菜单资源不可用，已保留手工输入模式', error?.message || error)
  }
  finally {
    systemMenuPageLoading.value = false
  }
}

function applyCustomActionApiConfig(value) {
  const configId = value === undefined || value === null ? '' : String(value)
  const selected = apiConfigs.value.find(item => String(item.id || item.apiCode || item.urlPath) === configId)
  const patch = { apiConfigId: configId || null }
  if (selected) {
    patch.apiCode = selected.apiCode || ''
    patch.apiName = selected.apiName || ''
    patch.method = selected.needEncrypt && String(selected.reqMethod || '').toUpperCase() === 'POST'
      ? 'POST_ENCRYPT'
      : normalizeCustomApiMethod(selected.reqMethod || 'POST')
    patch.url = selected.urlPath || ''
  }
  updateActiveActionConfig(patch)
}

function applySystemMenuPageTarget(value) {
  const routePath = value === undefined || value === null ? '' : String(value)
  const selected = systemMenuPageTargetOptions.value.find(item => item.value === routePath)
  updateActiveCustomAction({
    routePath,
    actionConfig: {
      ...(activeAction.value?.actionConfig || {}),
      targetPath: routePath,
      targetMenuId: selected?.raw?.id || '',
      targetMenuName: selected?.raw?.resourceName || '',
    },
  })
}

function addApiActionParam() {
  const config = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  const target = normalizeCustomApiMethod(config.method) === 'GET' ? 'query' : 'body'
  updateActiveActionConfig({
    params: [
      ...config.params,
      normalizeCustomApiParam({
        target,
        sourceType: 'rowField',
      }),
    ],
  })
}

function updateApiActionParam(paramIdx, patch) {
  const config = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  const params = [...config.params]
  params[paramIdx] = normalizeCustomApiParam({ ...(params[paramIdx] || {}), ...patch })
  updateActiveActionConfig({ params })
}

function removeApiActionParam(paramIdx) {
  const config = normalizeCustomApiConfig(activeAction.value?.actionConfig)
  const params = [...config.params]
  params.splice(paramIdx, 1)
  updateActiveActionConfig({ params })
}

function addActionParam() {
  const params = [...(activeAction.value?.params || []), createActionParam()]
  updateActiveCustomAction({ params })
}

function updateActionParam(idx, patch) {
  const params = [...(activeAction.value?.params || [])]
  params[idx] = { ...params[idx], ...patch }
  updateActiveCustomAction({ params })
}

function removeActionParam(idx) {
  const params = [...(activeAction.value?.params || [])]
  params.splice(idx, 1)
  updateActiveCustomAction({ params })
}

function createActionParam() {
  return {
    name: '',
    sourceType: 'static',
    sourceField: '',
    value: '',
  }
}
__impl.createActionParam = createActionParam

function normalizeParamSourcePatch(sourceType = 'static', param = {}) {
  return {
    sourceType,
    sourceField: '',
    value: sourceType === 'static' ? (param.value || '') : '',
  }
}

function buildParamValuePatch(param = {}, sourceField = '') {
  const sourceType = param.sourceType || 'static'
  return {
    sourceField: sourceField || '',
    value: resolveParamTemplateValue(sourceType, sourceField),
  }
}

function resolveParamTemplateValue(sourceType = 'static', sourceField = '') {
  if (!sourceField)
    return ''
  if (sourceType === 'rowField')
    return `:${sourceField}`
  if (sourceType === 'routeQuery')
    return `\${route.${sourceField}}`
  if (sourceType === 'system')
    return `\${system.${sourceField}}`
  return ''
}

function normalizeApiParamSourcePatch(sourceType = 'rowField', param = {}) {
  return {
    sourceType,
    sourceField: '',
    value: sourceType === 'static' ? (param.value || '') : '',
  }
}

function resolveSystemMenuPageTargetValue(action = {}) {
  const routePath = String(action?.routePath || action?.actionConfig?.targetPath || '').trim()
  if (!routePath)
    return null
  return systemMenuPageTargetOptions.value.some(item => item.value === routePath) ? routePath : null
}

function normalizeCustomActionType(value) {
  const normalized = String(value || 'route')
    .replace(/[-\s]+/g, '_')
  const upper = normalized.toUpperCase()
  if (['CALL_API', 'REQUEST'].includes(upper))
    return 'CALL_API'
  if (['START_FLOW', 'START_APPROVAL'].includes(upper))
    return 'START_FLOW'
  if (upper === 'TRIGGER')
    return 'TRIGGER'
  if (upper === 'EXTERNAL')
    return 'external'
  if (upper === 'REFRESH')
    return 'refresh'
  if (upper === 'PAGE' || upper === 'OPEN_PAGE')
    return 'page'
  return 'route'
}

function resolveActionBehaviorValue(value) {
  return normalizeCustomActionType(value)
}

function resolveActionBehaviorLabel(action = {}) {
  return actionBehaviorOptions.find(item => item.value === resolveActionBehaviorValue(action.actionType))?.label || '站内跳转'
}

function normalizeCustomActionList(list = []) {
  return (Array.isArray(list) ? list : [])
    .filter(action => action && typeof action === 'object')
    .map((action, index) => {
      const actionType = normalizeCustomActionType(action.actionType)
      const actionConfig = actionType === 'CALL_API'
        ? normalizeCustomApiConfig(action.actionConfig)
        : normalizeGenericActionConfig(actionType, action.actionConfig)
      const actionParams = actionType === 'CALL_API'
        ? actionConfig.params || []
        : action.params?.length ? action.params : actionConfig.params || []
      const clientKey = action.clientKey || action.key || `custom_${index + 1}`
      return {
        ...action,
        clientKey,
        key: normalizeActionKey(action.key || action.label) || `custom_${index + 1}`,
        label: action.label || '自定义按钮',
        position: ['toolbar', 'row', 'detail'].includes(action.position) ? action.position : 'row',
        type: action.type || defaultCustomActionButtonType(actionType),
        actionType,
        routePath: action.routePath || resolveCustomActionRoutePath(actionType, actionConfig),
        targetPageKey: action.targetPageKey || actionConfig.targetPageKey || '',
        targetFormKey: action.targetFormKey || actionConfig.targetFormKey || '',
        openTarget: action.openTarget || actionConfig.openTarget || (actionType === 'external' ? '_blank' : '_self'),
        permissionCode: action.permissionCode || action.permission || '',
        confirmText: action.confirmText || '',
        displayCondition: action.displayCondition || actionConfig.displayCondition || '',
        successBehavior: action.successBehavior || actionConfig.successBehavior || 'none',
        successMessage: action.successMessage || actionConfig.successMessage || '',
        failureMessage: action.failureMessage || actionConfig.failureMessage || '',
        params: normalizeCustomActionParams(actionParams),
        actionConfig,
      }
    })
}
__impl.normalizeCustomActionList = normalizeCustomActionList

function normalizeGenericActionConfig(actionType = 'route', config = {}) {
  const source = config && typeof config === 'object' && !Array.isArray(config) ? config : parseJsonObject(config)
  if (actionType === 'START_FLOW') {
    return {
      ...source,
      useMainFlow: true,
    }
  }
  return {
    ...source,
    navigationMode: actionType === 'page' || source.navigationMode === 'page' ? 'page' : (source.navigationMode || 'route'),
    targetPageKey: String(source.targetPageKey || '').trim(),
    params: normalizeCustomActionParams(source.params || []),
  }
}

function normalizeCustomActionParams(params = []) {
  return (Array.isArray(params) ? params : [])
    .filter(param => param && typeof param === 'object')
    .map((param, index) => ({
      clientKey: param.clientKey || `param_${Date.now()}_${index}`,
      name: String(param.name || '').trim(),
      sourceType: ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'static',
      sourceField: String(param.sourceField || '').trim(),
      value: param.value === undefined || param.value === null ? '' : String(param.value),
      target: ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : '',
    }))
}

function defaultCustomActionButtonType(actionType = 'route') {
  if (actionType === 'START_FLOW')
    return 'success'
  if (actionType === 'CALL_API')
    return 'primary'
  if (actionType === 'TRIGGER')
    return 'warning'
  return 'default'
}

function resolveCustomActionRoutePath(actionType = 'route', config = {}) {
  if (actionType === 'external')
    return config.url || ''
  if (actionType === 'CALL_API')
    return config.url || ''
  if (actionType === 'TRIGGER')
    return config.triggerCode || ''
  return config.targetPath || ''
}

function isApiCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'CALL_API'
}

function isRouteCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'route'
}

function isPageCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'page'
}

function isStartFlowCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'START_FLOW'
}

function isTriggerCustomAction(action = {}) {
  return normalizeCustomActionType(action?.actionType) === 'TRIGGER'
}

function isParamConfigurableAction(action = {}) {
  const actionType = normalizeCustomActionType(action?.actionType)
  return ['route', 'external', 'page'].includes(actionType)
}

function normalizeCustomApiConfig(config = {}) {
  const source = typeof config === 'string' ? parseJsonObject(config) : config && typeof config === 'object' ? config : {}
  const params = Array.isArray(source.params)
    ? source.params
    : Array.isArray(source.paramMappings)
      ? source.paramMappings
      : []
  return {
    ...source,
    apiConfigId: source.apiConfigId === undefined || source.apiConfigId === null ? null : String(source.apiConfigId),
    apiCode: String(source.apiCode || '').trim(),
    apiName: String(source.apiName || '').trim(),
    method: normalizeCustomApiMethod(source.method || source.reqMethod || source.apiMethod || 'POST'),
    url: String(source.url || source.apiUrl || source.urlPath || source.path || '').trim(),
    capabilityCode: String(source.capabilityCode || '').trim(),
    params: params.map(normalizeCustomApiParam).filter(Boolean),
  }
}

function resolveCustomApiUrl(action = {}) {
  const config = action.actionConfig && typeof action.actionConfig === 'object' ? action.actionConfig : {}
  return config.url || config.apiUrl || config.urlPath || config.path || action.routePath || ''
}

function normalizeCustomApiMethod(value) {
  const method = String(value || 'POST')
    .replace('-', '_')
    .toUpperCase()
  return apiMethodOptions.some(item => item.value === method) ? method : 'POST'
}

function normalizeCustomApiParam(param = {}) {
  const sourceType = ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'rowField'
  const target = ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : ''
  return {
    clientKey: param.clientKey || `param_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    name: String(param.name || '').trim(),
    target,
    sourceType,
    sourceField: String(param.sourceField || '').trim(),
    value: param.value === undefined || param.value === null ? '' : String(param.value),
  }
}

function normalizeResourceTreeResponse(res) {
  if (Array.isArray(res))
    return res
  if (Array.isArray(res?.data))
    return res.data
  if (Array.isArray(res?.data?.records))
    return res.data.records
  if (Array.isArray(res?.data?.list))
    return res.data.list
  if (Array.isArray(res?.data?.children))
    return res.data.children
  return []
}

function buildSystemMenuPageTargetOptions(resources = []) {
  const result = []
  const seen = new Set()
  const walk = (items = [], parents = []) => {
    ;(Array.isArray(items) ? items : []).forEach((item) => {
      if (!item || typeof item !== 'object')
        return
      const name = String(item.resourceName || item.menuName || item.name || item.title || '').trim()
      const nextParents = name ? [...parents, name] : parents
      const routePath = normalizeSystemMenuRoutePath(item)
      if (Number(item.resourceType) === 2 && routePath && !seen.has(routePath)) {
        seen.add(routePath)
        result.push({
          label: `${nextParents.join(' / ')} · ${routePath}`,
          value: routePath,
          raw: item,
        })
      }
      if (Array.isArray(item.children) && item.children.length)
        walk(item.children, nextParents)
    })
  }
  walk(resources)
  return result
}
__impl.buildSystemMenuPageTargetOptions = buildSystemMenuPageTargetOptions

function normalizeSystemMenuRoutePath(resource = {}) {
  return String(resource.path || resource.routePath || resource.component || '').trim()
}

function parseJsonObject(value) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function normalizeActionKey(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
}

function actionPathPlaceholder(action = {}) {
  const actionType = resolveActionBehaviorValue(action.actionType)
  if (actionType === 'external')
    return 'https://example.com/:id'
  if (actionType === 'refresh')
    return '刷新列表无需地址'
  if (actionType === 'CALL_API')
    return '/business/customer/audit/:id'
  if (actionType === 'page')
    return '选择应用内页面即可'
  return '/ai/xxx/:id'
}

// Tab keys bind nested content and remain system-managed when titles change.
function updateTab(idx, patch) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  if (!list[idx])
    return
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
function addTab() {
  const key = `tab_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
  const list = [...(selectedBlock.value?.props?.tabs || []), { key, title: `标签 ${selectedBlock.value?.props?.tabs?.length + 1 || 1}`, children: [] }]
  patchBlockProps(selectedBlock.value.id, { tabs: list })
  activeTabKey.value = key
}
function moveTab(idx, offset) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  const targetIndex = idx + offset
  if (!list[idx] || targetIndex < 0 || targetIndex >= list.length)
    return
  const [tab] = list.splice(idx, 1)
  list.splice(targetIndex, 0, tab)
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
function removeTab(idx) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  if (list.length <= 1)
    return
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { tabs: list })
  if (!list.some(tab => tab.key === activeTabKey.value))
    activeTabKey.value = list[Math.max(0, idx - 1)]?.key || list[0]?.key || ''
}

function propertySectionVisible(keywords = []) {
  const keyword = String(propertyKeyword.value || '').trim().toLowerCase()
  if (!keyword)
    return true
  const sectionMatched = keywords.some(item => String(item || '').toLowerCase().includes(keyword))
  if (sectionMatched)
    return true
  const blockText = [
    selectedBlock.value?.label,
    selectedBlock.value?.blockType,
    selectedBlock.value?.id,
    JSON.stringify(selectedBlock.value?.props || {}),
    JSON.stringify(selectedBlock.value?.fieldRefs || []),
  ].join(' ').toLowerCase()
  if (blockText.includes(keyword))
    return true
  const knownSectionMatched = propertySearchKeywordRegistry.some(item => item.includes(keyword))
  return !knownSectionMatched
}

function resolvePropertySearchTab(keyword = '') {
  const text = String(keyword || '').trim().toLowerCase()
  if (!text)
    return propertyPanelTab.value
  if (isPropertySearchTabMatched('props', text))
    return 'props'
  if (isPropertySearchTabMatched('style', text))
    return 'style'
  if (isPropertySearchTabMatched('interaction', text))
    return 'interaction'
  return 'props'
}
__impl.resolvePropertySearchTab = resolvePropertySearchTab

function isPropertySearchTabMatched(tab, keyword) {
  return (propertySearchTabIndex[tab] || []).some((item) => {
    const text = String(item || '').toLowerCase()
    return text === keyword || text.includes(keyword) || keyword.includes(text)
  })
}

function scrollPropertySearchTarget(keyword = '') {
  const panel = propertyPanelRef.value
  const text = String(keyword || '').trim().toLowerCase()
  if (!panel || !text)
    return
  const anchors = Array.from(panel.querySelectorAll('[data-property-search]'))
    .filter(anchor => anchor.offsetParent !== null)
  const target = anchors.find(anchor => isPropertyAnchorMatched(anchor, text)) || anchors[0]
  if (!target)
    return
  const panelRect = panel.getBoundingClientRect()
  const targetRect = target.getBoundingClientRect()
  panel.scrollTo({
    top: Math.max(0, panel.scrollTop + targetRect.top - panelRect.top - 8),
    behavior: 'smooth',
  })
}
__impl.scrollPropertySearchTarget = scrollPropertySearchTarget

function isPropertyAnchorMatched(anchor, keyword = '') {
  const text = String(anchor?.dataset?.propertySearch || '').toLowerCase()
  return text === keyword || text.includes(keyword) || keyword.includes(text)
}

function toggleCanvasFocus() {
  const nextFocused = !canvasFocusMode.value
  canvasFocusMode.value = nextFocused
  if (nextFocused)
    centerCanvasViewport()
}

function centerCanvasViewport() {
  nextTick(() => {
    const scrollEl = canvasScrollRef.value
    if (!scrollEl)
      return
    scrollEl.scrollTo({
      left: Math.max(0, (scrollEl.scrollWidth - scrollEl.clientWidth) / 2),
      top: scrollEl.scrollTop,
      behavior: 'smooth',
    })
  })
}

function selectPropertyPanelTab(tab) {
  designerStore.setPropertyTab(tab)
}
function resolveCrudFieldKey(field = {}) {
  return String(field.field || field.fieldCode || field.code || field.prop || field.key || field.id || '').trim()
}
__impl.resolveCrudFieldKey = resolveCrudFieldKey

function resolveCrudFieldLabel(field = {}) {
  return field.label || field.fieldName || field.name || field.title || resolveCrudFieldKey(field)
}
__impl.resolveCrudFieldLabel = resolveCrudFieldLabel

  return {
    ...deps,
    designerStore,
    canvasScrollRef,
    customActionModalOpen,
    activeActionIndex,
    propertyPanelTab,
    propertyPanelRef,
    propertyKeyword,
    propertySearchTabIndex,
    propertySearchKeywordRegistry,
    canvasFocusMode,
    activeTabKey,
    selectedBlock,
    externalCustomActionsEnabled,
    customActionList,
    activeAction,
    apiConfigs,
    apiConfigLoading,
    apiConfigLoaded,
    systemMenuPages,
    systemMenuPageLoading,
    systemMenuPageLoaded,
    systemMenuPageTargetOptions,
    crudTablePanelFields,
    patchBlock,
    patchBlockProps,
    handleFieldDrawerPatchProps,
    handleFieldDrawerPatchBlock,
    resolveBlockFieldCount,
    handleCrudTableFieldReorder,
    toggleCrudTableField,
    resolveCrudFieldInlineTitle,
    resolveCrudTablePanelFieldRefs,
    isCrudTableFieldVisible,
    buildCrudTableVisibilitySettings,
    resolveCrudFieldIconComponent,
    applySelectedTableGlobalAlign,
    normalizeTableRowGap,
    updateMetric,
    addMetric,
    removeMetric,
    quickEntryToneOptions,
    isQuickEntryListMode,
    isChartListMode,
    updateQuickEntryItem,
    addQuickEntryItem,
    removeQuickEntryItem,
    updateChartCategories,
    updateChartSeriesItem,
    addChartSeriesItem,
    removeChartSeriesItem,
    updateButtonGroupItem,
    addButtonGroupItem,
    removeButtonGroupItem,
    updateTagItem,
    addTagItem,
    removeTagItem,
    updateStepItem,
    addStepItem,
    removeStepItem,
    updateTimelineItem,
    addTimelineItem,
    removeTimelineItem,
    updateOptionItem,
    updateOptionSource,
    updateWidgetDataBinding,
    isWidgetBindingActive,
    isStatsStripListMode,
    isTimelineBound,
    addOptionItem,
    removeOptionItem,
    resolveBooleanKeys,
    updateLinkLikeTitle,
    updateLinkLikeUrl,
    updateCodeColor,
    clonePlainValue,
    createCustomActionClientKey,
    getCustomActionIdentity,
    addCustomAction,
    openCustomActionManager,
    createAndEditCustomAction,
    updateCustomAction,
    findCustomActionIndexByIdentity,
    updateCustomActionByIdentity,
    toggleCustomActionVisible,
    openCustomActionEditor,
    handleCustomActionGroupReorder,
    customActionMoreOptions,
    handleCustomActionMoreSelect,
    removeCustomAction,
    removeCustomActionByIdentity,
    persistCustomActionList,
    updateActiveCustomAction,
    updateActiveCustomActionType,
    updateActiveActionConfig,
    updateActiveGenericActionConfig,
    loadEnabledApiConfigs,
    loadSystemMenuPages,
    applyCustomActionApiConfig,
    applySystemMenuPageTarget,
    addApiActionParam,
    updateApiActionParam,
    removeApiActionParam,
    addActionParam,
    updateActionParam,
    removeActionParam,
    createActionParam,
    normalizeParamSourcePatch,
    buildParamValuePatch,
    resolveParamTemplateValue,
    normalizeApiParamSourcePatch,
    resolveSystemMenuPageTargetValue,
    normalizeCustomActionType,
    resolveActionBehaviorValue,
    resolveActionBehaviorLabel,
    normalizeCustomActionList,
    normalizeGenericActionConfig,
    normalizeCustomActionParams,
    defaultCustomActionButtonType,
    resolveCustomActionRoutePath,
    isApiCustomAction,
    isRouteCustomAction,
    isPageCustomAction,
    isStartFlowCustomAction,
    isTriggerCustomAction,
    isParamConfigurableAction,
    normalizeCustomApiConfig,
    resolveCustomApiUrl,
    normalizeCustomApiMethod,
    normalizeCustomApiParam,
    normalizeResourceTreeResponse,
    buildSystemMenuPageTargetOptions,
    normalizeSystemMenuRoutePath,
    parseJsonObject,
    normalizeActionKey,
    actionPathPlaceholder,
    updateTab,
    addTab,
    moveTab,
    removeTab,
    propertySectionVisible,
    resolvePropertySearchTab,
    isPropertySearchTabMatched,
    scrollPropertySearchTarget,
    isPropertyAnchorMatched,
    toggleCanvasFocus,
    centerCanvasViewport,
    selectPropertyPanelTab,
    resolveCrudFieldKey,
    resolveCrudFieldLabel,
  }
}
