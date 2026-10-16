/**
 * 表单设计器 governance.events → AiCrudPage 生命周期钩子。
 * afterLoad/beforeLoad 对齐 beforeRenderForm（打开表单时预填）。
 */

function normalizeGovernanceHook(hook = '') {
  const value = String(hook || '')
  if (value === 'beforeLoad' || value === 'afterLoad')
    return 'beforeRenderForm'
  if (value === 'beforeSubmit')
    return 'beforeSubmit'
  if (value === 'afterSubmit')
    return 'afterSubmit'
  return ''
}

export function applySetFieldValueHandler(handler = '', payload = {}) {
  const target = payload && typeof payload === 'object' && !Array.isArray(payload)
    ? payload
    : {}
  const [field, ...valueParts] = String(handler || '').split('=')
  const fieldName = field?.trim()
  if (!fieldName)
    return target
  target[fieldName] = valueParts.join('=').trim()
  return target
}

function runWhitelistedScript(handler = '', payload = {}) {
  const name = String(handler || '').trim()
  const scripts = {
    fillCurrentDate: () => {
      payload.currentDate = new Date().toISOString().slice(0, 10)
    },
    fillCurrentTime: () => {
      payload.currentTime = new Date().toISOString()
    },
  }
  if (!name || name === 'noop')
    return
  if (!scripts[name]) {
    console.warn(`[form-governance] 表单事件脚本不在白名单内: ${name}`)
    return
  }
  scripts[name]()
}

async function runGovernanceEvent(eventItem = {}, payload = {}) {
  const action = String(eventItem.action || '')
  if (action === 'setFieldValue') {
    applySetFieldValueHandler(eventItem.handler, payload)
    return
  }
  if (action === 'customScript') {
    runWhitelistedScript(eventItem.handler, payload)
  }
}

/**
 * @param {Array} events governance.events
 * @returns {{ beforeRenderForm?: Function, beforeSubmit?: Function, afterSubmit?: Function }}
 */
export function buildFormGovernanceHookProps(events = []) {
  const grouped = (Array.isArray(events) ? events : []).reduce((map, eventItem) => {
    const hookName = normalizeGovernanceHook(eventItem?.hook)
    if (!hookName || !eventItem?.handler)
      return map
    if (!map[hookName])
      map[hookName] = []
    map[hookName].push(eventItem)
    return map
  }, {})

  return Object.entries(grouped).reduce((props, [hookName, list]) => {
    props[hookName] = async (payload) => {
      const data = payload && typeof payload === 'object' && !Array.isArray(payload)
        ? { ...payload }
        : {}
      for (const eventItem of list)
        await runGovernanceEvent(eventItem, data)
      return data
    }
    return props
  }, {})
}
