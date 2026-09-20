import {
  applyEventMappings,
  buildEventClearPatch,
  buildEventParams,
  normalizeScanContext,
  safeEventRules,
  shouldSkipFieldEvent,
} from '@/utils/lowcode-runtime'

export function useLowcodeFieldEvents({ api, routeQuery, getContext, getRowScope, notify }) {
  const timers = new Map()
  const controllers = new Map()
  const sequences = new Map()

  async function dispatchFieldEvent({ trigger, field = {}, data = {}, fields, rules, scan, child }) {
    const normalizedTrigger = String(trigger || '').toUpperCase()
    const sourceField = String(field.field || field.fieldCode || field.actionCode || field.key || field.id || '')
    const normalizedRules = safeEventRules(rules, fields).filter((rule) => {
      const ruleTrigger = String(rule.trigger || '').toUpperCase()
      return ruleTrigger === normalizedTrigger
        && (ruleTrigger === 'FORM_LOAD' || String(rule.sourceField || '') === sourceField)
    })
    const scope = child ? `${child.modelCode}:${getRowScope(data)}` : 'main'
    await Promise.all(normalizedRules.map(rule => schedule(rule, data, scan, scope, normalizedTrigger)))
  }

  function schedule(rule, data, scan, scope, trigger) {
    const key = `${scope}:${rule.id || rule.sourceKey}:${rule.sourceField || 'form'}`
    clearTimer(key)
    if (shouldSkipFieldEvent(rule, data)) {
      cancelRequest(key)
      Object.assign(data, buildEventClearPatch(rule))
      return Promise.resolve({ status: 'skipped' })
    }
    const delay = trigger === 'CHANGE' ? Math.max(0, Math.min(5000, Number(rule.debounceMs) || 0)) : 0
    if (!delay) return execute(rule, data, scan, key)
    cancelRequest(key)
    return new Promise((resolve) => {
      const timer = setTimeout(async () => {
        timers.delete(key)
        resolve(await execute(rule, data, scan, key))
      }, delay)
      timers.set(key, { timer, resolve })
    })
  }

  async function execute(rule, data, scan, key) {
    cancelRequest(key)
    const sequence = (sequences.get(key) || 0) + 1
    sequences.set(key, sequence)
    if (shouldSkipFieldEvent(rule, data)) {
      Object.assign(data, buildEventClearPatch(rule))
      return { status: 'skipped' }
    }
    if (rule.clearTargetsOnTrigger === true) Object.assign(data, buildEventClearPatch(rule))

    const controller = typeof AbortController === 'undefined' ? null : new AbortController()
    if (controller) controllers.set(key, controller)
    try {
      const normalizedScan = normalizeScanContext(scan)
      const runtimeContext = getContext()
      const context = normalizedScan ? { ...runtimeContext, scan: normalizedScan } : runtimeContext
      const params = buildEventParams(rule, data, context, routeQuery)
      const response = await api.executeLowcodeQuerySource(
        { sourceType: rule.sourceType, sourceKey: rule.sourceKey, params },
        controller ? { signal: controller.signal } : {},
      )
      if (sequences.get(key) !== sequence) return { status: 'stale' }
      const mapped = applyEventMappings(rule, unwrapQueryResult(response), data)
      if (mapped.found || Object.keys(mapped.patch).length) Object.assign(data, mapped.patch)
      if (!mapped.found && rule.notFoundMessage && String(rule.errorMode || 'MESSAGE').toUpperCase() !== 'SILENT')
        notify(rule.notFoundMessage, { type: 'warning' })
      return { status: mapped.found ? 'success' : 'not_found' }
    }
    catch (error) {
      if (sequences.get(key) !== sequence || controller?.signal.aborted) return { status: 'cancelled' }
      if (rule.errorMessage && String(rule.errorMode || 'MESSAGE').toUpperCase() !== 'SILENT')
        notify(rule.errorMessage, { type: 'error' })
      console.warn('[lowcode h5] field event failed', error)
      return { status: 'error' }
    }
    finally {
      if (controllers.get(key) === controller) controllers.delete(key)
    }
  }

  function clearTimer(key) {
    const pending = timers.get(key)
    if (!pending) return
    clearTimeout(pending.timer)
    timers.delete(key)
    pending.resolve({ status: 'cancelled' })
  }

  function cancelRequest(key) {
    const controller = controllers.get(key)
    if (controller && !controller.signal.aborted) controller.abort()
    controllers.delete(key)
    sequences.set(key, (sequences.get(key) || 0) + 1)
  }

  function cancelFieldEvents() {
    for (const key of [...timers.keys()]) clearTimer(key)
    for (const key of [...controllers.keys()]) cancelRequest(key)
  }

  return { dispatchFieldEvent, cancelFieldEvents }
}

function unwrapQueryResult(response) {
  const value = response?.data
  return value?.data !== undefined ? value.data : value
}
