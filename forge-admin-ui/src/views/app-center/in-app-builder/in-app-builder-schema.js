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
  const saved = clone(options.inAppBuilder || {})
  const nodes = normalizeNodes(saved.nodes)
  const homePageId = resolveHomePageId(saved.homePageId, nodes)
  const pages = normalizePages(saved.pages, nodes)
  const schema = {
    schemaVersion: IN_APP_BUILDER_SCHEMA_VERSION,
    homePageId,
    nodes,
    pages,
    formAssets: normalizeFormAssets(saved.formAssets),
  }

  // 页面是应用设计的显式产物。空应用保持为空，不能为了运行壳自动造出一个“首页”。
  // 已保存的旧首页仍会被原样保留，确保历史应用可以继续打开。
  normalizeObjectReferences(schema, objects)
  return schema
}

export function mergeInAppBuilderOptions(applicationOptions, schema) {
  const options = parseOptions(applicationOptions)
  return {
    ...options,
    inAppBuilder: clone({
      schemaVersion: IN_APP_BUILDER_SCHEMA_VERSION,
      homePageId: schema.homePageId,
      nodes: schema.nodes,
      pages: schema.pages,
      formAssets: schema.formAssets,
    }),
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
  }
  if (type === 'page') {
    node.pageType = normalizePageType(input.pageType)
    node.pageTemplate = String(input.pageTemplate || input.templateKey || '').trim()
    node.objectRef = normalizeObjectRef(input.objectRef)
    node.entryRef = normalizeEntryRef(input.entryRef)
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
      type: node.type === 'group' ? 'group' : 'page',
      title: String(node.title || (node.type === 'group' ? '未命名页面组' : '未命名页面')).trim(),
      icon: String(node.icon || '').trim(),
      parentId: node.parentId ? String(node.parentId) : null,
      sort: Number.isFinite(Number(node.sort)) ? Number(node.sort) : index * 10,
      ...(node.type === 'group'
        ? {}
        : {
            pageType: normalizePageType(node.pageType),
            pageTemplate: String(node.pageTemplate || node.templateKey || '').trim(),
            objectRef: normalizeObjectRef(node.objectRef),
            entryRef: normalizeEntryRef(node.entryRef),
          }),
    }))
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
    defaultParams: clone(value.defaultParams || {}),
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
  const ids = new Set(nodes.map(node => node.id))
  let sequence = 1
  let candidate = `${prefix}_${base}`
  while (ids.has(candidate))
    candidate = `${prefix}_${base}_${sequence++}`
  return candidate
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
  const ids = new Set((formAssets || []).map(asset => asset.id))
  const base = slugify(title) || 'form'
  let sequence = 1
  let candidate = `form_${base}`
  while (ids.has(candidate))
    candidate = `form_${base}_${sequence++}`
  return candidate
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

function clone(value) {
  return value === undefined ? undefined : JSON.parse(JSON.stringify(value))
}
