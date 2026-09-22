/**
 * 同一个应用运行页的路由状态只加载一次。
 *
 * Vue Router 在首次进入、query 归一化或 KeepAlive 激活时可能连续通知相同状态。
 * coordinator 会合并相同 key，并在加载期间只保留最后一个不同 key，避免旧请求覆盖新路由。
 */
export function createApplicationRuntimeLoadCoordinator(loadFn) {
  let activePromise = null
  let queuedKey = ''
  let loadedKey = ''

  async function drain() {
    let executed = false
    while (queuedKey && queuedKey !== loadedKey) {
      const nextKey = queuedKey
      queuedKey = ''
      await loadFn(nextKey)
      loadedKey = nextKey
      executed = true
    }
    return executed
  }

  function run(key) {
    const normalizedKey = String(key || '')
    if (!normalizedKey || normalizedKey === loadedKey)
      return Promise.resolve(false)

    queuedKey = normalizedKey
    if (activePromise)
      return activePromise

    const task = drain()
    const wrapped = task.finally(() => {
      if (activePromise === wrapped)
        activePromise = null
    })
    activePromise = wrapped
    return activePromise
  }

  function invalidate() {
    loadedKey = ''
  }

  return { run, invalidate }
}

export function resolveApplicationRuntimeLoadKey(route = {}, canEditApplication = false) {
  const params = route.params || {}
  const query = route.query || {}
  return JSON.stringify({
    applicationCode: String(params.applicationCode || ''),
    edit: query.edit === '1',
    draft: query.draft === '1',
    // 有编辑权限时页面管理也读草稿；key 需区分，避免权限晚到时仍停留在已发布快照
    workspace: shouldUseApplicationWorkspaceLoad(route, canEditApplication),
  })
}

/**
 * 编辑态 / 草稿预览 / 有编辑权限的页面管理：读工作台草稿（含未发布页面）。
 * 普通运行用户：读已发布快照。
 */
export function shouldUseApplicationWorkspaceLoad(route = {}, canEditApplication = false) {
  const query = route.query || {}
  if (query.edit === '1' || query.draft === '1')
    return true
  return Boolean(canEditApplication)
}
