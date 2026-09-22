import { normalizePrintPageWatermark } from '@/components/print/management/pagePrintWatermark'

export const IN_APP_BUILDER_SCHEMA_VERSION = 2
export const HOME_PAGE_ID = 'page_home'

export const inAppPageTypes = [
  { label: '首页', value: 'home' },
  { label: '介绍页', value: 'intro' },
  { label: '业务数据页', value: 'object' },
  { label: '空白内容页', value: 'content' },
  { label: '访问入口页', value: 'entry' },
]

const DEFAULT_HOME_LAYOUT = {
  title: '欢迎使用应用',
  description: '从左侧选择页面，或进入编辑应用开始搭建。',
  items: [],
}

const DEFAULT_COMPONENT_PROPS = {
  'page-title': { title: '页面标题', subtitle: '补充一句简短的页面说明' },
  'intro': { title: '欢迎使用', description: '用一句话介绍这个页面可以完成什么。' },
  'metric-card': { title: '核心指标', value: '0', trend: '较昨日持平' },
  'business-list': { title: '业务数据列表', emptyText: '请先绑定业务对象' },
  'business-form': { title: '录入表单', submitText: '提交' },
  'todo': { title: '我的待办', emptyText: '暂无待办事项' },
  'chart': { title: '趋势图', emptyText: '请配置图表数据' },
  'text': { content: '在这里填写页面内容。' },
  'image': { alt: '图片说明', src: '' },
  'columns': { columns: 2, gap: 16 },
  'divider': { label: '' },
}

export function normalizeInAppBuilder(rawOptions, _application = {}, objects = []) {
  const options = parseOptions(rawOptions)
  const saved = restoreLegacyPrimaryObjectPage(
    clone(options.inAppBuilder || {}),
    options,
    _application,
    objects,
  )
  const nodes = normalizeNodes(saved.nodes)
  const homePageId = resolveHomePageId(saved.homePageId, nodes)
  const pages = normalizePages(saved.pages, nodes)
  const schema = {
    schemaVersion: IN_APP_BUILDER_SCHEMA_VERSION,
    legacyObjectPageMigrated: saved.legacyObjectPageMigrated === true,
    homePageId,
    nodes,
    pages,
    formAssets: normalizeFormAssets(saved.formAssets),
    flowInteraction: normalizeFlowInteraction(saved.flowInteraction),
  }

  // 页面是应用设计的显式产物。空应用保持为空，不能为了运行壳自动造出一个“首页”。
  // 已保存的旧首页仍会被原样保留，确保历史应用可以继续打开。
  normalizeObjectReferences(schema, objects)
  return schema
}

export function hasPendingLegacyObjectPageMigration(rawOptions, schema) {
  const options = parseOptions(rawOptions)
  const saved = options.inAppBuilder && typeof options.inAppBuilder === 'object'
    ? options.inAppBuilder
    : {}
  return saved.legacyObjectPageMigrated !== true
    && schema?.legacyObjectPageMigrated === true
    && (schema.nodes || []).some(node => node?.legacyObjectPage === true)
}

export function mergeInAppBuilderOptions(applicationOptions, schema) {
  const options = parseOptions(applicationOptions)
  return {
    ...options,
    inAppBuilder: clone({
      schemaVersion: IN_APP_BUILDER_SCHEMA_VERSION,
      legacyObjectPageMigrated: schema.legacyObjectPageMigrated === true,
      homePageId: schema.homePageId,
      nodes: schema.nodes,
      pages: schema.pages,
      formAssets: schema.formAssets,
      flowInteraction: normalizeFlowInteraction(schema.flowInteraction),
    }),
  }
}

/**
 * 改版前的对象型应用没有独立页面实体，运行页直接由主业务对象和 CRUD
 * 发布配置推导。新版页面树首次读取这类应用时，把主对象投影成一个对象页；
 * 迁移标记随后随草稿保存，用户以后主动删除全部页面时不会再次自动恢复。
 */
function restoreLegacyPrimaryObjectPage(saved, options, application, objects) {
  const builder = saved && typeof saved === 'object' ? saved : {}
  const nodes = Array.isArray(builder.nodes) ? builder.nodes : []
  const pages = builder.pages && typeof builder.pages === 'object' ? builder.pages : {}
  const primaryObjectCode = String(options.primaryObjectCode || '').trim()
  if (builder.legacyObjectPageMigrated === true
    || nodes.length > 0
    || Object.keys(pages).length > 0
    || !primaryObjectCode) {
    return builder
  }

  const primaryObject = (Array.isArray(objects) ? objects : []).find(item => (
    String(item?.objectCode || '') === primaryObjectCode
    && String(item?.objectRole || 'PRIMARY').toUpperCase() === 'PRIMARY'
  ))
  if (!primaryObject?.configKey)
    return builder

  const objectOptions = parseOptions(primaryObject.options)
  const objectCode = String(primaryObject.objectCode || primaryObjectCode).trim()
  const objectName = String(primaryObject.objectName || application?.applicationName || objectCode).trim()
  const pageId = `page_${slugify(objectCode) || 'legacy_object'}`
  const objectRef = {
    objectId: primaryObject.objectId ?? primaryObject.id ?? null,
    objectCode,
    objectName,
    configKey: String(primaryObject.configKey || '').trim(),
    pageKey: String(objectOptions.pageKey || 'list').trim() || 'list',
    pageMode: 'crud',
    hasBusinessData: true,
    valid: true,
  }
  return {
    ...builder,
    schemaVersion: IN_APP_BUILDER_SCHEMA_VERSION,
    legacyObjectPageMigrated: true,
    homePageId: pageId,
    nodes: [{
      id: pageId,
      type: 'page',
      title: objectName,
      icon: String(application?.icon || '').trim(),
      parentId: null,
      sort: 0,
      pageType: 'object',
      pageTemplate: String(primaryObject.layoutType || '').toLowerCase() === 'master-detail-crud'
        ? 'master-detail'
        : 'crud',
      objectRef,
      mountTarget: 'BOTH',
      systemMenuVisible: false,
      navigationVisible: true,
      access: { mode: 'inherit', roleIds: [] },
      legacyObjectPage: true,
    }],
    pages: {
      [pageId]: {
        title: objectName,
        description: '',
        layout: {
          items: [],
          pageTitleComponentInitialized: true,
        },
      },
    },
    formAssets: Array.isArray(builder.formAssets) ? builder.formAssets : [],
  }
}

export function normalizeFlowInteraction(source = {}) {
  const flow = source && typeof source === 'object' ? clone(source) : {}
  return {
    ...flow,
    approvalActions: (Array.isArray(flow.approvalActions) ? flow.approvalActions : [])
      .filter(action => action && typeof action === 'object')
      .map((action, index) => ({
        ...action,
        actionId: String(action.actionId || `flow_action_${index + 1}`).trim(),
        operation: ['approve', 'reject', 'return', 'delegate'].includes(action.operation) ? action.operation : 'approve',
        label: String(action.label || '').trim(),
        permissionKey: String(action.permissionKey || action.permissionCode || '').trim(),
        permissionStrategy: action.permissionStrategy === 'disable' ? 'disable' : 'hide',
        enabled: action.enabled !== false,
      })),
    timeline: {
      ...(flow.timeline && typeof flow.timeline === 'object' ? flow.timeline : {}),
      enabled: flow.timeline?.enabled === true,
      title: String(flow.timeline?.title || '审批记录').trim() || '审批记录',
    },
    nodePermissions: (Array.isArray(flow.nodePermissions) ? flow.nodePermissions : [])
      .filter(item => item && typeof item === 'object')
      .map(item => ({
        ...item,
        nodeKey: String(item.nodeKey || '').trim(),
        visibleSectionIds: uniqueStrings(item.visibleSectionIds),
        readonlySectionIds: uniqueStrings(item.readonlySectionIds),
      })),
    callbacks: flow.callbacks && typeof flow.callbacks === 'object' ? flow.callbacks : {},
  }
}

/**
 * 应用内表单是页面组件可复用的设计资产，不属于左侧发布导航。
 * 字段目录不单独持久化，始终由 formDesignerSchema 的控件绑定派生。
 */
export function createInAppFormAsset(schema, input = {}) {
  const next = clone(schema)
  const title = String(input.name || input.formName || '未命名表单').trim() || '未命名表单'
  const id = createFormAssetId(next.formAssets, title)
  const formKey = String(input.formKey || input.formDesignerSchema?.formKey || id).trim() || id
  next.formAssets = [
    ...(next.formAssets || []),
    {
      id,
      formKey,
      name: title,
      formDesignerSchema: clone(input.formDesignerSchema || {}),
    },
  ]
  return { schema: next, formAssetId: id }
}

export function updateInAppFormAsset(schema, formAssetId, patch = {}) {
  const next = clone(schema)
  const index = (next.formAssets || []).findIndex(asset => asset.id === String(formAssetId || ''))
  if (index < 0)
    throw new Error('表单不存在')
  const current = next.formAssets[index]
  next.formAssets[index] = {
    ...current,
    ...(Object.prototype.hasOwnProperty.call(patch, 'name') ? { name: String(patch.name || '').trim() || current.name } : {}),
    ...(Object.prototype.hasOwnProperty.call(patch, 'formKey') ? { formKey: String(patch.formKey || '').trim() || current.formKey } : {}),
    ...(Object.prototype.hasOwnProperty.call(patch, 'formDesignerSchema') ? { formDesignerSchema: clone(patch.formDesignerSchema || {}) } : {}),
  }
  return next
}

export function removeInAppFormAsset(schema, formAssetId) {
  const next = clone(schema)
  const id = String(formAssetId || '')
  next.formAssets = (next.formAssets || []).filter(asset => asset.id !== id)
  Object.values(next.pages || {}).forEach((page) => {
    const items = page?.layout?.gridLayout?.items
    if (!Array.isArray(items))
      return
    items.forEach((item) => {
      if (item?.props?.formAssetId === id)
        item.props.formAssetId = ''
    })
  })
  return next
}

export function createNavigationNode(schema, input = {}) {
  const next = clone(schema)
  const type = input.type === 'group' ? 'group' : 'page'
  const parentId = normalizeParentId(next, input.parentId)
  const siblingNodes = next.nodes.filter(node => node.parentId === parentId)
  const id = createNodeId(next.nodes, type, input.title)
  const node = {
    id,
    type,
    title: String(input.title || (type === 'group' ? '未命名页面组' : '未命名页面')).trim() || (type === 'group' ? '未命名页面组' : '未命名页面'),
    icon: String(input.icon || '').trim(),
    parentId,
    sort: resolveNextSort(siblingNodes),
    systemMenuVisible: input.systemMenuVisible === true,
    navigationVisible: input.navigationVisible !== false,
    access: normalizeNodeAccess(input.access),
  }
  if (type === 'page') {
    node.pageType = normalizePageType(input.pageType)
    node.pageTemplate = String(input.pageTemplate || input.templateKey || '').trim()
    node.objectRef = normalizeObjectRef(input.objectRef)
    node.entryRef = normalizeEntryRef(input.entryRef)
    node.printWatermark = normalizePrintPageWatermark(input.printWatermark)
    next.pages[id] = normalizePageLayout(input.layout, node)
    if (!next.homePageId)
      next.homePageId = id
  }
  next.nodes.push(node)
  return next
}

export function moveNavigationNode(schema, nodeId, targetParentId = null, targetIndex) {
  const next = clone(schema)
  const node = findNode(next, nodeId)
  if (!node)
    throw new Error('页面或页面组不存在')
  const normalizedParentId = normalizeParentId(next, targetParentId)
  if (node.id === normalizedParentId || isDescendant(next.nodes, normalizedParentId, node.id))
    throw new Error('页面组不能移动到自身或其子节点内')

  node.parentId = normalizedParentId
  const siblings = next.nodes
    .filter(item => item.parentId === normalizedParentId && item.id !== node.id)
    .sort(sortNodes)
  const index = Number.isInteger(targetIndex)
    ? Math.max(0, Math.min(targetIndex, siblings.length))
    : siblings.length
  siblings.splice(index, 0, node)
  siblings.forEach((item, order) => {
    item.sort = order * 10
  })
  return next
}

/**
 * 页面表单对象只属于创建它的页面。页面已从导航删除，且没有其它页面仍引用该对象时，流程里不应再选到它。
 */
export function isOrphanPageFormObject(object, schema) {
  const options = parseOptions(object?.options)
  const sourcePageId = String(options.sourcePageId || '').trim()
  if (options.managedBy !== 'PAGE_FORM' || !sourcePageId)
    return false
  const pageIds = new Set((schema?.nodes || [])
    .filter(node => node?.type === 'page')
    .map(node => node.id))
  if (pageIds.has(sourcePageId))
    return false
  return !collectReferencedObjectIds(schema).has(String(object?.objectId || object?.id || ''))
}

/**
 * 删除页面前的影响预览：可能回收页面表单对象配置，但不会物理删表。
 */
export function resolveNavigationDeleteImpact(schema, nodeId, strategy, objects = []) {
  const node = (schema?.nodes || []).find(item => item.id === nodeId)
  if (!node) {
    return {
      removedPageIds: [],
      impactedObjects: [],
      danger: false,
    }
  }

  const nodes = schema.nodes || []
  const descendants = collectDescendants(nodes, node.id)
  let removedIds
  if (node.type === 'group' && descendants.length && strategy?.type === 'move-children') {
    removedIds = new Set([node.id])
  }
  else {
    removedIds = new Set([node.id, ...descendants.map(item => item.id)])
  }

  const removedPageIds = nodes
    .filter(item => removedIds.has(item.id) && item.type === 'page')
    .map(item => item.id)
  const removedPageIdSet = new Set(removedPageIds)
  const impactedById = new Map()

  for (const object of objects || []) {
    const options = parseOptions(object?.options)
    const sourcePageId = String(options.sourcePageId || '').trim()
    if (options.managedBy !== 'PAGE_FORM' || !sourcePageId || !removedPageIdSet.has(sourcePageId))
      continue
    const key = String(object.objectId ?? object.id ?? object.objectCode ?? '')
    if (!key)
      continue
    impactedById.set(key, {
      objectId: object.objectId ?? object.id,
      objectCode: object.objectCode || '',
      objectName: object.objectName || '',
      tableName: object.tableName || '',
      reason: 'page-form',
    })
  }

  for (const pageId of removedPageIds) {
    const pageNode = nodes.find(item => item.id === pageId)
    const ref = pageNode?.objectRef
    if (!ref?.objectId && !ref?.objectCode)
      continue
    const matched = (objects || []).find(item =>
      (ref.objectId != null && String(item.objectId ?? item.id) === String(ref.objectId))
      || (ref.objectCode && item.objectCode === ref.objectCode))
    if (!matched)
      continue
    const key = String(matched.objectId ?? matched.id ?? matched.objectCode ?? '')
    if (!key || impactedById.has(key))
      continue
    impactedById.set(key, {
      objectId: matched.objectId ?? matched.id,
      objectCode: matched.objectCode || ref.objectCode || '',
      objectName: matched.objectName || ref.objectName || '',
      tableName: matched.tableName || '',
      reason: 'bound',
    })
  }

  return {
    removedPageIds,
    impactedObjects: [...impactedById.values()],
    danger: true,
  }
}

function collectReferencedObjectIds(schema) {
  const ids = new Set()
  const visit = (value) => {
    if (!value || typeof value !== 'object')
      return
    if (Array.isArray(value)) {
      value.forEach(visit)
      return
    }
    const objectId = value.objectRef?.objectId
    if (objectId != null && String(objectId).trim())
      ids.add(String(objectId))
    Object.values(value).forEach(visit)
  }
  visit(schema?.nodes)
  visit(schema?.pages)
  return ids
}

export function removeNavigationNode(schema, nodeId, strategy) {
  const next = clone(schema)
  const node = findNode(next, nodeId)
  if (!node)
    throw new Error('页面或页面组不存在')
  const descendants = collectDescendants(next.nodes, node.id)
  if (node.type === 'group' && descendants.length && !strategy?.type)
    throw new Error('请选择页面组删除后的子页面处理方式')

  if (node.type === 'group' && descendants.length && strategy?.type === 'move-children') {
    const targetParentId = normalizeParentId(next, strategy.targetParentId)
    if (targetParentId === node.id || isDescendant(next.nodes, targetParentId, node.id))
      throw new Error('不能将子页面移动到即将删除的页面组内')
    const directChildren = next.nodes.filter(item => item.parentId === node.id).sort(sortNodes)
    directChildren.forEach((child, index) => {
      child.parentId = targetParentId
      child.sort = resolveNextSort(next.nodes.filter(item => item.parentId === targetParentId && item.id !== child.id)) + index
    })
    next.nodes = next.nodes.filter(item => item.id !== node.id)
    return normalizeHomePage(normalizeSiblingSort(next))
  }

  const removedIds = new Set([node.id, ...descendants.map(item => item.id)])
  next.nodes = next.nodes.filter(item => !removedIds.has(item.id))
  removedIds.forEach((id) => {
    delete next.pages[id]
  })
  return normalizeHomePage(normalizeSiblingSort(next))
}

export function insertPageComponent(schema, pageId, component = {}, target = {}) {
  const next = clone(schema)
  const page = next.pages[pageId]
  if (!page)
    throw new Error('页面不存在或不支持组件配置')
  const items = Array.isArray(page.layout?.items) ? page.layout.items : []
  const id = createComponentId(items, component.componentKey)
  const item = {
    id,
    componentKey: String(component.componentKey || '').trim() || 'text',
    label: String(component.label || component.title || '未命名组件').trim() || '未命名组件',
    props: {
      ...clone(DEFAULT_COMPONENT_PROPS[component.componentKey] || {}),
      ...clone(component.props || {}),
    },
  }
  const targetIndex = resolveInsertIndex(items, target)
  items.splice(targetIndex, 0, item)
  next.pages[pageId] = {
    ...page,
    layout: {
      ...(page.layout || {}),
      items,
    },
  }
  return { schema: next, selectedComponentId: id }
}

export function updatePageComponent(schema, pageId, componentId, patch = {}) {
  const next = clone(schema)
  const items = next.pages?.[pageId]?.layout?.items
  const index = items?.findIndex(item => item.id === componentId) ?? -1
  if (index < 0)
    throw new Error('组件不存在')
  items[index] = {
    ...items[index],
    ...clone(patch),
    props: { ...items[index].props, ...clone(patch.props || {}) },
  }
  return next
}

export function resolveInAppBuilderNode(schema, nodeId) {
  return clone((schema?.nodes || []).find(node => node.id === nodeId) || null)
}

function parseOptions(value) {
  if (!value)
    return {}
  if (typeof value === 'string') {
    try {
      return JSON.parse(value)
    }
    catch {
      return {}
    }
  }
  return clone(value)
}

function normalizeNodes(nodes) {
  const used = new Set()
  return (Array.isArray(nodes) ? nodes : [])
    .filter((node) => {
      const id = String(node?.id || '').trim()
      if (!id || used.has(id))
        return false
      used.add(id)
      return true
    })
    .map((node, index) => ({
      id: String(node.id).trim(),
      type: isGroupNode(node) ? 'group' : 'page',
      title: String(node.title || node.name || (isGroupNode(node) ? '未命名页面组' : '未命名页面')).trim(),
      icon: String(node.icon || '').trim(),
      parentId: resolveNodeParentId(node),
      sort: Number.isFinite(Number(node.sort)) ? Number(node.sort) : index * 10,
      // Menu publishing fields are part of the page design snapshot.  Keep
      // both the current top-level shape and the older settings shape so a
      // publish/reload round-trip does not silently discard the target.
      systemMenuVisible: (node.systemMenuVisible ?? node.settings?.systemMenuVisible) === true,
      navigationVisible: (node.navigationVisible ?? node.settings?.navigationVisible) !== false,
      access: normalizeNodeAccess(node.access),
      mountTarget: String(node.mountTarget ?? node.settings?.mountTarget ?? 'ADMIN').trim().toUpperCase() || 'ADMIN',
      menuName: String(node.menuName ?? node.settings?.menuName ?? '').trim(),
      menuParentId: node.menuParentId ?? node.settings?.menuParentId ?? null,
      mobileMenuParentId: node.mobileMenuParentId ?? node.settings?.mobileMenuParentId ?? null,
      menuSort: Number.isFinite(Number(node.menuSort ?? node.settings?.menuSort))
        ? Number(node.menuSort ?? node.settings?.menuSort)
        : null,
      legacyObjectPage: node.legacyObjectPage === true,
      ...(isGroupNode(node)
        ? {}
        : {
            pageType: normalizePageType(node.pageType),
            pageTemplate: String(node.pageTemplate || node.templateKey || '').trim(),
            objectRef: normalizeObjectRef(node.objectRef),
            entryRef: normalizeEntryRef(node.entryRef),
            printWatermark: normalizePrintPageWatermark(node.printWatermark ?? node.settings?.printWatermark),
          }),
    }))
}

function isGroupNode(node = {}) {
  const type = String(node.type || node.nodeType || node.kind || '').trim().toLowerCase()
  return ['group', 'page-group', 'page_group', 'pagegroup', 'menu-group', 'menu_group', 'directory', 'folder'].includes(type)
}

function resolveNodeParentId(node = {}) {
  const parentId = node.parentId ?? node.parentNodeId ?? node.parentID ?? node.settings?.parentId
  return parentId === undefined || parentId === null || parentId === '' ? null : String(parentId)
}

function resolveHomePageId(homePageId, nodes) {
  const saved = String(homePageId || '').trim()
  if (saved && nodes.some(node => node.id === saved && node.type === 'page'))
    return saved
  return nodes.find(node => node.pageType === 'home')?.id || nodes.find(node => node.type === 'page')?.id || null
}

function normalizePages(pages, nodes) {
  const source = pages && typeof pages === 'object' ? pages : {}
  return nodes.reduce((result, node) => {
    if (node.type === 'page')
      result[node.id] = normalizePageLayout(source[node.id], node)
    return result
  }, {})
}

function normalizeFormAssets(formAssets) {
  const used = new Set()
  return (Array.isArray(formAssets) ? formAssets : [])
    .filter((asset) => {
      const id = String(asset?.id || '').trim()
      if (!id || used.has(id))
        return false
      used.add(id)
      return true
    })
    .map((asset) => {
      const id = String(asset.id).trim()
      const name = String(asset.name || asset.formName || '未命名表单').trim() || '未命名表单'
      return {
        id,
        formKey: String(asset.formKey || id).trim() || id,
        name,
        formDesignerSchema: clone(asset.formDesignerSchema || asset.schema || {}),
      }
    })
}

function normalizeObjectReferences(schema, objects) {
  const objectIds = new Set((objects || []).map(item => String(item.objectId || item.id || '')).filter(Boolean))
  const objectCodes = new Set((objects || []).map(item => String(item.objectCode || '')).filter(Boolean))
  schema.nodes.forEach((node) => {
    if (node.pageType !== 'object' || !node.objectRef)
      return
    const { objectId, objectCode } = node.objectRef
    node.objectRef = {
      ...node.objectRef,
      valid: (!objectId || objectIds.has(String(objectId))) && (!objectCode || objectCodes.has(String(objectCode))),
    }
  })
}

function normalizePageLayout(layout, node, application = {}) {
  const source = layout && typeof layout === 'object' ? clone(layout) : {}
  return {
    title: String(source.title || node.title || application.applicationName || '未命名页面'),
    description: String(source.description || (node.pageType === 'home' ? DEFAULT_HOME_LAYOUT.description : '')),
    layout: {
      items: Array.isArray(source.layout?.items)
        ? source.layout.items.map(item => clone(item)).filter(item => item?.id && item?.componentKey)
        : [],
      ...(source.layout?.gridLayout && typeof source.layout.gridLayout === 'object'
        ? { gridLayout: clone(source.layout.gridLayout) }
        : {}),
      ...(source.layout?.pageTitleComponentInitialized === true
        ? { pageTitleComponentInitialized: true }
        : {}),
    },
  }
}

function normalizePageType(value) {
  const type = String(value || '').toLowerCase()
  return inAppPageTypes.some(item => item.value === type) ? type : 'content'
}

/**
 * 页面节点访问控制：默认继承应用运行入口的授权角色；
 * roles 模式在发布时投影为系统菜单并只授权给指定角色（后端发布校验会拦截空角色）。
 */
export function normalizeNodeAccess(value) {
  const source = value && typeof value === 'object' ? value : {}
  const mode = String(source.mode || '').trim().toLowerCase() === 'roles' ? 'roles' : 'inherit'
  const roleIds = [...new Set((Array.isArray(source.roleIds) ? source.roleIds : [])
    .map((roleId) => {
      if (roleId === undefined || roleId === null || roleId === '')
        return null
      const numeric = Number(roleId)
      return Number.isFinite(numeric) ? numeric : null
    })
    .filter(roleId => roleId !== null))]
  if (mode === 'inherit')
    return { mode: 'inherit', roleIds: [] }
  return { mode: 'roles', roleIds }
}

function normalizeObjectRef(value) {
  if (!value || typeof value !== 'object')
    return null
  const objectId = value.objectId === undefined || value.objectId === null ? null : String(value.objectId)
  const objectCode = String(value.objectCode || '').trim()
  if (!objectId && !objectCode)
    return null
  return {
    objectId,
    objectCode,
    pageKey: String(value.pageKey || 'list').trim() || 'list',
    pageMode: String(value.pageMode || 'crud').trim() || 'crud',
    objectName: String(value.objectName || '').trim(),
    configKey: String(value.configKey || '').trim(),
    formKey: String(value.formKey || '').trim(),
    hasBusinessData: value.hasBusinessData === true,
    defaultParams: clone(value.defaultParams || {}),
    runtimeDatasourceId: value.runtimeDatasourceId ?? null,
    createMode: String(value.createMode || '').trim(),
    importDatasourceId: value.importDatasourceId ?? value.runtimeDatasourceId ?? null,
    importTableName: String(value.importTableName || '').trim(),
    valid: value.valid !== false,
  }
}

function normalizeHomePage(schema) {
  const exists = schema.homePageId && schema.nodes.some(node => node.id === schema.homePageId && node.type === 'page')
  if (!exists)
    schema.homePageId = schema.nodes.filter(node => node.type === 'page').sort(sortNodes)[0]?.id || null
  return schema
}

function normalizeEntryRef(value) {
  if (!value || typeof value !== 'object')
    return null
  const entryId = value.entryId === undefined || value.entryId === null ? null : String(value.entryId)
  return entryId ? { entryId } : null
}

function normalizeParentId(schema, parentId) {
  if (parentId === undefined || parentId === null || parentId === '')
    return null
  const parent = findNode(schema, parentId)
  if (!parent || parent.type !== 'group')
    throw new Error('请选择有效的页面组')
  return parent.id
}

function normalizeSiblingSort(schema) {
  const groups = new Map()
  schema.nodes.forEach((node) => {
    const key = node.parentId || '__root__'
    groups.set(key, [...(groups.get(key) || []), node])
  })
  groups.forEach(nodes => nodes.sort(sortNodes).forEach((node, index) => {
    node.sort = index * 10
  }))
  return schema
}

function findNode(schema, nodeId) {
  return schema.nodes.find(node => node.id === String(nodeId)) || null
}

function isDescendant(nodes, nodeId, parentId) {
  if (!nodeId || !parentId)
    return false
  let current = nodes.find(node => node.id === nodeId)
  const visited = new Set()
  while (current?.parentId && !visited.has(current.id)) {
    if (current.parentId === parentId)
      return true
    visited.add(current.id)
    current = nodes.find(node => node.id === current.parentId)
  }
  return false
}

function collectDescendants(nodes, parentId) {
  const result = []
  const queue = [parentId]
  while (queue.length) {
    const current = queue.shift()
    const children = nodes.filter(node => node.parentId === current)
    result.push(...children)
    queue.push(...children.map(node => node.id))
  }
  return result
}

function resolveNextSort(nodes) {
  if (!nodes.length)
    return 0
  return Math.max(...nodes.map(node => Number(node.sort) || 0)) + 10
}

function resolveInsertIndex(items, target) {
  if (target?.afterComponentId) {
    const index = items.findIndex(item => item.id === target.afterComponentId)
    if (index >= 0)
      return index + 1
  }
  if (Number.isInteger(target?.index))
    return Math.max(0, Math.min(target.index, items.length))
  return items.length
}

function createNodeId(nodes, type, title) {
  const prefix = type === 'group' ? 'group' : 'page'
  const base = slugify(title) || prefix
  return createUniqueId(nodes.map(node => node.id), `${prefix}_${base}`)
}

function createComponentId(items, componentKey) {
  const prefix = slugify(componentKey) || 'component'
  const ids = new Set(items.map(item => item.id))
  let sequence = 1
  let candidate = `component_${prefix}`
  while (ids.has(candidate))
    candidate = `component_${prefix}_${sequence++}`
  return candidate
}

function createFormAssetId(formAssets, title) {
  const base = slugify(title) || 'form'
  return createUniqueId((formAssets || []).map(asset => asset.id), `form_${base}`)
}

function createUniqueId(existingIds, prefix) {
  const ids = new Set(existingIds)
  let candidate = `${prefix}_${createUniqueSuffix()}`
  while (ids.has(candidate))
    candidate = `${prefix}_${createUniqueSuffix()}`
  return candidate
}

function createUniqueSuffix() {
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let suffix = ''
  for (let index = 0; index < 6; index += 1)
    suffix += alphabet[Math.floor(Math.random() * alphabet.length)]
  return suffix
}

function slugify(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/^_+|_+$/g, '')
}

function sortNodes(a, b) {
  return Number(a.sort || 0) - Number(b.sort || 0) || String(a.title).localeCompare(String(b.title), 'zh-CN')
}

function uniqueStrings(values) {
  return [...new Set((Array.isArray(values) ? values : [])
    .map(value => String(value || '').trim())
    .filter(Boolean))]
}

function clone(value) {
  return value === undefined ? undefined : JSON.parse(JSON.stringify(value))
}
