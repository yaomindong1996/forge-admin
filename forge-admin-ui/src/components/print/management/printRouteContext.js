// 路由只携带来源标识；实际归属和记录权限由服务端重新核验。
const positive = value => /^[1-9]\d*$/.test(String(value || '')) ? String(value) : null
const text = value => typeof value === 'string' ? value : null
export function printSourceFromQuery(query) {
  const applicationId = positive(query.applicationId)
  const objectCode = text(query.objectCode)
  if (!applicationId || !objectCode || !['LOWCODE', 'CODE'].includes(query.sourceType))
    return null
  const source = { applicationId, objectCode, sourceType: query.sourceType, pageId: null, formKey: null }
  if (source.sourceType === 'LOWCODE')
    source.pageId = typeof query.pageId === 'string' && /^[A-Z0-9][\w.:-]{0,127}$/i.test(query.pageId) ? query.pageId : null
  else source.formKey = text(query.formKey)
  return source.pageId || source.formKey ? source : null
}

/** 发给后端的来源身份：去掉 null，避免 GET 把 formKey=null 传成空串后被当成无权访问。 */
export function printSourcePayload(source) {
  const normalized = source && typeof source === 'object'
    ? (source.applicationId && source.objectCode && source.sourceType
        ? printSourceFromQuery(source)
        : null)
    : null
  if (!normalized)
    return null
  return Object.fromEntries(Object.entries(normalized).filter(([, value]) => value != null && value !== ''))
}
export function printRecordFromQuery(query) {
  const source = printSourceFromQuery(query)
  const recordId = text(query.recordId)
  if (!source || !recordId || !['LIST', 'DETAIL', 'FLOW_TODO', 'FLOW_DONE', 'FLOW_STARTED'].includes(query.scene))
    return null
  return { source, recordId, scene: query.scene, taskId: text(query.taskId), processInstanceId: text(query.processInstanceId), processRunId: positive(query.processRunId) }
}

const FLOW_FALLBACK = {
  FLOW_TODO: '/flow/todo',
  FLOW_DONE: '/flow/done',
  FLOW_STARTED: '/flow/started',
}

function parseLocalPath(path) {
  if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//'))
    return null
  try {
    return new URL(path, 'http://local.invalid')
  }
  catch {
    return null
  }
}

/** Designer must return to the owning page, never the independent /print list or application settings. */
export function isSafePrintReturnPath(path) {
  const url = parseLocalPath(path)
  if (!url || url.pathname === '/print' || url.pathname.startsWith('/print/'))
    return false
  return /\/app-center\/application\/[^/]+\/runtime$/.test(url.pathname)
    && url.searchParams.get('edit') === '1'
}

export function printPageSettingsLocation({ applicationCode, pageId } = {}) {
  const code = String(applicationCode || '').trim()
  if (!code)
    return { path: '/app-center' }
  return {
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: code },
    query: {
      edit: '1',
      pageId: pageId || undefined,
      designTab: 'settings',
      settingsSection: 'printing',
    },
  }
}

export function printPreviewFallbackLocation(record) {
  if (FLOW_FALLBACK[record?.scene])
    return { path: FLOW_FALLBACK[record.scene] }
  const applicationId = record?.source?.applicationId
  if (applicationId)
    return { path: `/app-center/app/${applicationId}` }
  return { path: '/app-center' }
}

export function leavePrintDesigner({
  router,
  route,
  source,
  applicationCode,
} = {}) {
  if (!router)
    return 'noop'
  const from = route?.query?.from
  if (isSafePrintReturnPath(from)) {
    router.replace(from)
    return 'from'
  }
  router.replace(printPageSettingsLocation({
    applicationCode: applicationCode || route?.query?.applicationCode,
    pageId: source?.pageId || route?.query?.pageId,
  }))
  return 'fallback'
}

export function leavePrintPreview({
  router,
  record,
  historyState = typeof window === 'undefined' ? null : window.history.state,
  closeWindow = () => {
    if (typeof window === 'undefined')
      return false
    window.close()
    return window.closed
  },
} = {}) {
  if (!router)
    return 'noop'
  if (historyState?.back != null) {
    router.back()
    return 'back'
  }
  if (closeWindow())
    return 'close'
  router.replace(printPreviewFallbackLocation(record))
  return 'fallback'
}
