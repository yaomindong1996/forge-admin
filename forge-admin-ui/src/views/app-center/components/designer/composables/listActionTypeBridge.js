/**
 * 列表自定义按钮 actionType ↔ 设计器 actionType 双向映射。
 * 独立模块便于单测，避免 refresh/page 等类型在往返中被写成 route。
 */

export function normalizeListActionType(value = '') {
  const raw = String(value || 'route')
  const upper = raw.replace(/[-\s]+/g, '_').toUpperCase()
  if (['CALL_API', 'REQUEST'].includes(upper))
    return 'CALL_API'
  if (['START_FLOW', 'START_APPROVAL'].includes(upper))
    return 'START_FLOW'
  if (upper === 'TRIGGER')
    return 'TRIGGER'
  if (upper === 'COMMAND')
    return 'COMMAND'
  if (upper === 'EXTERNAL')
    return 'external'
  if (upper === 'REFRESH')
    return 'refresh'
  if (upper === 'PAGE' || upper === 'OPEN_PAGE')
    return 'page'
  return 'route'
}

export function normalizeDesignerActionType(value = '') {
  const normalized = String(value || 'OPEN_PAGE')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace(/[-\s]+/g, '_')
    .toUpperCase()
  if (['CALL_API', 'REQUEST'].includes(normalized))
    return 'CALL_API'
  if (['START_FLOW', 'START_APPROVAL'].includes(normalized))
    return 'START_FLOW'
  if (normalized === 'OPEN_EXTERNAL')
    return 'OPEN_EXTERNAL'
  if (normalized === 'TRIGGER')
    return 'TRIGGER'
  if (normalized === 'COMMAND')
    return 'COMMAND'
  if (normalized === 'REFRESH')
    return 'REFRESH'
  return 'OPEN_PAGE'
}

export function designerActionTypeToListType(actionType = 'OPEN_PAGE') {
  if (actionType === 'CALL_API')
    return 'CALL_API'
  if (actionType === 'START_FLOW')
    return 'START_FLOW'
  if (actionType === 'TRIGGER')
    return 'TRIGGER'
  if (actionType === 'COMMAND')
    return 'COMMAND'
  if (actionType === 'OPEN_EXTERNAL')
    return 'external'
  if (actionType === 'REFRESH')
    return 'refresh'
  // OPEN_PAGE 可能是「应用内页面」或「站内路由」，由调用方结合 targetPageKey 再细分
  return 'route'
}

export function listActionTypeToDesignerType(actionType = 'route') {
  const normalized = normalizeListActionType(actionType)
  if (normalized === 'CALL_API')
    return 'CALL_API'
  if (normalized === 'START_FLOW')
    return 'START_FLOW'
  if (normalized === 'TRIGGER')
    return 'TRIGGER'
  if (normalized === 'COMMAND')
    return 'COMMAND'
  if (normalized === 'external')
    return 'OPEN_EXTERNAL'
  if (normalized === 'refresh')
    return 'REFRESH'
  return 'OPEN_PAGE'
}

export function resolveListActionTypeFromDesigner(actionType = 'OPEN_PAGE', action = {}, config = {}) {
  const mapped = designerActionTypeToListType(actionType)
  if (mapped !== 'route')
    return mapped
  // 未选目标页时也要保留「应用内页面」，否则会立刻被回写成 route，选择框看起来没反应
  if (config.navigationMode === 'page')
    return 'page'
  const targetPageKey = String(config.targetPageKey || action.targetPageKey || '').trim()
  const routePath = String(config.targetPath || action.routePath || '').trim()
  if (targetPageKey && !routePath)
    return 'page'
  return 'route'
}
