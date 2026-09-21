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

/** Preview is usually opened in a new tab, so history.back() has nowhere to go. */
export function printPreviewFallbackLocation(record) {
  if (FLOW_FALLBACK[record?.scene])
    return { path: FLOW_FALLBACK[record.scene] }
  const applicationId = record?.source?.applicationId
  if (applicationId)
    return { path: `/app-center/app/${applicationId}` }
  return { path: '/app-center' }
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
