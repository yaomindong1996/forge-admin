import { defineStore } from 'pinia'
import {
  buildDefaultData,
  ensureChildRows,
  hasComposedRuntimePageSchema,
  mergeFlowActionsIntoBottomBar,
  normalizeChildrenConfig,
  normalizeDesignerComponents,
  normalizeField,
  normalizeMainFields,
  normalizeRuntimeFlowInteraction,
  normalizeRuntimeZoneCanvasNodes,
  parseRuntimeConfig,
  resolveChildRows,
  resolveFieldLinkages,
  resolveMobilePageChrome,
  resolveRuntimeFormDesignerSchema,
  resolveRuntimePageZones,
  resolveRuntimeZoneCanvasFieldRefs,
  resolveRuntimeZoneFormDesignerSchema,
  syncChildRowAliases,
} from '../../utils/lowcode-runtime.js'

export const useLowcodeRuntimeStore = defineStore('lowcode-runtime', {
  state: () => ({
    routeQuery: {},
    configKey: '',
    title: '低代码应用',
    subtitle: '移动端运行页',
    loading: true,
    saving: false,
    errorMessage: '',
    config: {},
    mode: 'list',
    currentId: '',
    records: [],
    total: 0,
    page: 1,
    pageSize: 10,
    searchExpanded: false,
    searchData: {},
    mainData: {},
    childData: {},
    dictOptions: {},
    bottomActionLoading: '',
  }),
  getters: {
    formDesignerSchema: state => resolveRuntimeFormDesignerSchema(state.config, state.mode),
    runtimePageZones(state) {
      return hasComposedRuntimePageSchema(state.config) ? resolveRuntimePageZones(state.config, state.mode) : []
    },
    hasComposedPageZones() { return this.runtimePageZones.length > 0 },
    mainFields(state) { return normalizeMainFields(state.config, this.formDesignerSchema) },
    mainNodes(state) { return normalizeDesignerComponents(state.config, this.formDesignerSchema) },
    searchFields: state => (Array.isArray(state.config.searchSchema) ? state.config.searchSchema : []).map(normalizeField).filter(field => field.field),
    visibleColumns: state => (Array.isArray(state.config.columnsSchema) ? state.config.columnsSchema : []).filter(column => column?.prop || column?.field),
    hasPageSections() { return Array.isArray(this.formDesignerSchema?.pageSections) && this.formDesignerSchema.pageSections.length > 0 },
    pageSections() { return this.formDesignerSchema?.pageSections || [] },
    flowInteraction: state => normalizeRuntimeFlowInteraction(state.config?.options?.flowInteraction),
    bottomBar() { return mergeFlowActionsIntoBottomBar(this.formDesignerSchema?.bottomBar || {}, this.flowInteraction) },
    fieldLinkages(state) { return resolveFieldLinkages(this.formDesignerSchema, state.config) },
    allChildren: state => normalizeChildrenConfig(state.config),
    visibleChildren() { return this.allChildren.filter(child => childVisibleInMode(child, this.mode)) },
    currentFlowNodeKey: state => String(
      state.routeQuery.taskDefKey || state.routeQuery.taskDefinitionKey || state.routeQuery.nodeKey
      || state.mainData.taskDefKey || state.mainData.taskDefinitionKey || '',
    ),
    currentFlowTaskId: state => String(state.routeQuery.taskId || state.mainData.taskId || ''),
    currentProcessInstanceId: state => String(
      state.routeQuery.processInstanceId || state.mainData.processInstanceId || state.mainData.flowInstanceId || '',
    ),
    hasConfiguredBottomBar() { return Array.isArray(this.bottomBar?.actions) && this.bottomBar.actions.length > 0 },
    canEdit: state => state.mode === 'detail' && String(state.mainData.status || '').toUpperCase() === 'DRAFT',
    pageChrome: state => resolveMobilePageChrome(state.config, state.routeQuery, { title: state.title, subtitle: state.subtitle }),
  },
  actions: {
    initializeRoute(query = {}) {
      clearObject(this.routeQuery)
      Object.assign(this.routeQuery, query)
      this.configKey = String(query.configKey || query.runtimeConfigKey || query.pageConfigKey || query.config || resolveConfigKey(query.path) || '').trim()
      this.title = String(query.title || '低代码应用')
      this.subtitle = String(query.subtitle || '移动端运行页')
      const mode = String(query.mode || '').toLowerCase()
      this.mode = ['create', 'detail', 'edit'].includes(mode) ? mode : 'list'
      this.currentId = String(query.recordId || query.id || '')
      this.page = 1
      this.total = 0
      this.records = []
      this.config = {}
      this.errorMessage = ''
      this.loading = true
      this.saving = false
      this.searchExpanded = false
      this.bottomActionLoading = ''
      clearObject(this.searchData)
      clearObject(this.mainData)
      clearObject(this.childData)
      clearObject(this.dictOptions)
    },
    applyConfig(payload = {}) {
      this.config = parseRuntimeConfig(payload)
      this.title = String(this.routeQuery.title || this.config.appName || this.config.objectName || this.title)
    },
    applyList(payload = {}) {
      this.records = payload.records || payload.list || payload.rows || []
      this.total = Number(payload.total || this.records.length || 0)
    },
    applyDetail(payload = {}) {
      clearObject(this.mainData)
      Object.assign(this.mainData, payload.main || payload)
      clearObject(this.childData)
      Object.assign(this.childData, payload.children || {})
      this.allChildren.forEach(child => syncChildRowAliases(child, this.childData))
    },
    initializeFormData() {
      clearObject(this.mainData)
      Object.assign(this.mainData, this.buildMainDefaultData())
      clearObject(this.childData)
      this.allChildren.forEach(child => ensureChildRows(child, this.childData))
    },
    buildMainDefaultData() {
      const defaults = buildDefaultData(this.mainFields)
      const storedFields = Array.isArray(this.config.editSchema) ? this.config.editSchema : []
      storedFields.forEach((field) => {
        const fieldCode = String(field?.field || field?.sourceField || '').trim()
        const defaultValue = field?.defaultValue ?? field?.props?.defaultValue
        if (fieldCode && defaultValue !== undefined && defaults[fieldCode] === undefined) defaults[fieldCode] = defaultValue
      })
      return defaults
    },
    setDictOptions(type, options) { this.dictOptions[type] = Array.isArray(options) ? options : [] },
    resetSearch() { clearObject(this.searchData); this.page = 1 },
    toggleSearch() { this.searchExpanded = !this.searchExpanded },
    changePage(delta) {
      const pageCount = Math.max(1, Math.ceil(Number(this.total || 0) / this.pageSize))
      this.page = Math.min(pageCount, Math.max(1, this.page + delta))
    },
    openCreate() { this.mode = 'create'; this.currentId = '' },
    openDetail(row = {}) { this.currentId = String(row[this.config.rowKey || 'id']); this.mode = 'detail' },
    openEdit() { this.mode = 'edit' },
    goList() { this.mode = 'list'; this.currentId = '' },
    childRows(child) { return resolveChildRows(child, this.childData) },
    addChildRow(child) { ensureChildRows(child, this.childData).push(buildDefaultData(child.fields)) },
    removeChildRow(child, index) { this.childRows(child).splice(index, 1) },
    buildChildrenPayload() {
      return Object.fromEntries(this.allChildren.map(child => [child.modelCode, this.childRows(child)]).filter(([key]) => key))
    },
    runtimeZoneFormSchema(zone = {}) { return resolveRuntimeZoneFormDesignerSchema(zone) || {} },
    runtimeZoneMainFields(zone = {}) {
      const fields = normalizeMainFields(this.config, this.runtimeZoneFormSchema(zone))
      const refs = resolveRuntimeZoneCanvasFieldRefs(zone)
      if (!refs.length) return fields
      const order = new Map(refs.map((field, index) => [String(field), index]))
      return fields.filter(field => order.has(String(field.field))).sort((left, right) => order.get(String(left.field)) - order.get(String(right.field)))
    },
    runtimeZoneNodes(zone = {}) {
      const designerNodes = normalizeDesignerComponents(this.config, this.runtimeZoneFormSchema(zone))
      return designerNodes.length ? designerNodes : normalizeRuntimeZoneCanvasNodes(this.config, zone)
    },
    runtimeZoneHasSections(zone = {}) {
      const schema = this.runtimeZoneFormSchema(zone)
      return Array.isArray(schema.pageSections) && schema.pageSections.length > 0
    },
    runtimeZoneBottomBar(zone = {}) {
      return mergeFlowActionsIntoBottomBar(this.runtimeZoneFormSchema(zone)?.bottomBar || this.bottomBar || {}, this.flowInteraction)
    },
    runtimeZoneFieldLinkages(zone = {}) { return resolveFieldLinkages(this.runtimeZoneFormSchema(zone), this.config) },
    runtimeZoneChildren(zone = {}) {
      const props = zone.props || {}
      const keys = [props.relationKey, props.childRelationKey, props.modelCode, ...(Array.isArray(props.relationKeys) ? props.relationKeys : [])].filter(Boolean).map(String)
      if (!keys.length) return this.visibleChildren
      return this.visibleChildren.filter(child => [child.key, child.relationKey, child.modelCode].filter(Boolean).some(key => keys.includes(String(key))))
    },
  },
})

function childVisibleInMode(child, mode) {
  if (mode === 'detail') return child.showInDetail !== false
  if (mode === 'edit') return child.showInEdit !== false
  return child.showInCreate !== false
}

function clearObject(target) {
  Object.keys(target).forEach(key => delete target[key])
}

function resolveConfigKey(path = '') {
  const match = String(path || '').match(/(?:crud-page|crud|lowcode)\/([^/?]+)/)
  return decodeURIComponent(match?.[1] || '')
}
