/**
 * 列表页自由布局运行态：跨区块刷新 / 过滤 / 打开表单的轻量注册表。
 * GridBlockRenderer 挂载 AiCrudPage 时注册，按钮事件按 targetBlockId 查找。
 */

const runtimeBlockApis = new Map()

/** 从组件实例上取 CRUD 能力（expose / 公共实例） */
export function pickCrudApi(instance) {
  if (!instance || typeof instance !== 'object')
    return null
  if (typeof instance.refresh === 'function'
    || typeof instance.loadList === 'function'
    || typeof instance.showAdd === 'function'
    || typeof instance.search === 'function')
    return instance
  const exposed = instance.$?.exposed || instance.exposed
  if (exposed && (typeof exposed.refresh === 'function'
    || typeof exposed.loadList === 'function'
    || typeof exposed.showAdd === 'function'
    || typeof exposed.search === 'function'))
    return exposed
  const setup = instance.$?.setupState || instance.setupState
  if (setup && (typeof setup.refresh === 'function'
    || typeof setup.loadList === 'function'
    || typeof setup.showAdd === 'function'))
    return setup
  return null
}

/** 在壳组件子树中查找嵌套 AiCrudPage（RuntimeListGridFlow 场景） */
export function findNestedAiCrudApi(host) {
  const direct = pickCrudApi(host)
  if (direct)
    return direct
  if (typeof host?.getRuntimeCrudRef === 'function') {
    const nested = pickCrudApi(host.getRuntimeCrudRef())
    if (nested)
      return nested
  }
  const root = host?.$ || host
  if (!root || typeof root !== 'object')
    return null
  let found = null
  function walk(comp, depth = 0) {
    if (!comp || found || depth > 40)
      return
    const name = comp.type?.name || comp.type?.__name || ''
    if (name === 'AiCrudPage') {
      found = pickCrudApi(comp) || pickCrudApi(comp.proxy) || pickCrudApi(comp.ctx)
      if (found)
        return
    }
    const queue = [comp.subTree]
    while (queue.length && !found) {
      const vnode = queue.shift()
      if (vnode?.component)
        walk(vnode.component, depth + 1)
      if (Array.isArray(vnode?.children))
        queue.push(...vnode.children)
    }
  }
  walk(root)
  return found
}

export function registerRuntimeBlock(blockId, api) {
  const id = String(blockId || '').trim()
  if (!id || !api)
    return () => {}
  runtimeBlockApis.set(id, api)
  return () => {
    if (runtimeBlockApis.get(id) === api)
      runtimeBlockApis.delete(id)
  }
}

export function getRuntimeBlock(blockId) {
  const id = String(blockId || '').trim()
  return id ? (runtimeBlockApis.get(id) || null) : null
}

export function refreshRuntimeBlock(blockId) {
  const api = getRuntimeBlock(blockId)
  if (!api)
    return false
  if (typeof api.refresh === 'function') {
    api.refresh()
    return true
  }
  if (typeof api.loadList === 'function') {
    api.loadList()
    return true
  }
  return false
}

export function filterRuntimeBlock(blockId, params = {}) {
  const api = getRuntimeBlock(blockId)
  if (!api)
    return false
  if (typeof api.search === 'function') {
    api.search({ ...(params || {}) })
    return true
  }
  if (typeof api.setSearchParams === 'function' && typeof api.loadList === 'function') {
    api.setSearchParams({ ...(params || {}) })
    api.loadList()
    return true
  }
  return false
}

/** 根据 resolveApi() 生成注册表条目 */
export function createRuntimeBlockApi(resolveApi) {
  return {
    refresh: () => {
      const api = resolveApi()
      return api?.refresh?.() || api?.loadList?.()
    },
    loadList: () => resolveApi()?.loadList?.(),
    search: params => resolveApi()?.search?.(params),
    setSearchParams: params => resolveApi()?.setSearchParams?.(params),
    showAdd: () => resolveApi()?.showAdd?.(),
    showEdit: row => resolveApi()?.showEdit?.(row),
    showDetail: row => resolveApi()?.showDetail?.(row),
  }
}

export function openRuntimeBlockForm(blockId, mode = 'add') {
  const api = getRuntimeBlock(blockId)
  if (!api)
    return false
  if (mode === 'edit' && typeof api.showEdit === 'function') {
    api.showEdit()
    return true
  }
  if (mode === 'detail' && typeof api.showDetail === 'function') {
    api.showDetail()
    return true
  }
  if (typeof api.showAdd === 'function') {
    api.showAdd()
    return true
  }
  return false
}
