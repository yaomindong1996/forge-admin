/**
 * 列表区块 click 事件运行态执行（action-button / link / 同类触发）。
 * 从 useGridBlockRenderer 抽出，避免巨型 composable 继续膨胀。
 */
import {
  filterRuntimeBlock,
  openRuntimeBlockForm,
  refreshRuntimeBlock,
} from './runtime-block-registry'
import { confirmRuntimeAction, parseRuntimeApiConfig } from './gridBlockRendererUtils.js'
import { request } from '@/utils'

/** 优先 ctx.message（组件树 useMessage），再回退 window.$message（离散 API）。 */
function showRuntimeToast(content = '', type = 'info', ctx = {}) {
  const text = String(content || '').trim()
  if (!text)
    return
  const level = ['info', 'success', 'warning', 'error'].includes(String(type || ''))
    ? String(type)
    : 'info'
  const api = ctx.message || window.$message
  const fn = api?.[level] || api?.info
  if (typeof fn === 'function')
    fn.call(api, text)
}

export function resolveRuntimeEventMessageContent(eventItem = {}, resolveRuntimeText, resolveRuntimeParamValue) {
  const direct = [
    eventItem.message,
    eventItem.content,
    eventItem.messageContent,
    eventItem.toastText,
  ].map(item => String(resolveRuntimeText(item || '') || '').trim()).find(Boolean)
  if (direct)
    return direct
  const params = Array.isArray(eventItem.params) ? eventItem.params : []
  const contentParam = params.find((item) => {
    const key = String(item?.key || item?.name || item?.field || '').trim()
    return ['content', 'message', 'text', 'title', 'messageContent'].includes(key)
  })
  if (contentParam) {
    const fromParam = String(resolveRuntimeParamValue(contentParam) || '').trim()
    if (fromParam)
      return fromParam
  }
  // 设计器「添加参数」常见只填 value、name 为空
  const firstStatic = params.find(item => String(item?.value ?? '').trim())
  if (firstStatic) {
    const fromFirst = String(resolveRuntimeParamValue(firstStatic) || '').trim()
    if (fromFirst)
      return fromFirst
  }
  return String(resolveRuntimeText(eventItem.description || eventItem.label || '') || '').trim()
}

/**
 * 是否像应用导航页 id（切 pageId）。
 * - page_page_*：设计器自动生成
 * - page_leave_apply / page_onboard_home：市场包等人读命名
 * - system:workbench：系统页
 * 对象多画布 pageKey（list / e2e_dialog / page_<时间戳>）不要走这里。
 */
export function looksLikeApplicationPageId(target = '') {
  const next = String(target || '').trim()
  if (!next)
    return false
  if (next.startsWith('system:'))
    return true
  if (/^page_page_[a-z0-9]+$/i.test(next))
    return true
  // 排除 page_1712345678901 这类对象画布 key
  if (/^page_\d{10,}$/.test(next))
    return false
  return /^page_[a-z][a-z0-9_]*$/i.test(next)
}

/**
 * 应用运行态默认用 pageId 切应用页；但对象多画布页（list/dialog/自定义 pageKey）
 * 必须保留当前 pageId，只改 pageKey，否则会把 e2e_dialog 误写成 pageId 导致白页/串页。
 */
export function applyPageNavigationQuery(query = {}, target = '', isApplicationRuntimeRoute) {
  const next = String(target || '').trim()
  if (!next)
    return query
  const onAppRoute = typeof isApplicationRuntimeRoute === 'function'
    ? Boolean(isApplicationRuntimeRoute())
    : Boolean(isApplicationRuntimeRoute)
  const currentPageId = String(query.pageId || '').trim()
  if (onAppRoute) {
    // 应用页 ↔ 应用页：换 pageId，清掉残留 pageKey，避免列表被当成「非 list」空壳
    if (looksLikeApplicationPageId(next) || (currentPageId && next === currentPageId)) {
      query.pageId = next
      delete query.pageKey
      return query
    }
    // 仍在当前应用页内切对象画布（list / dialog / e2e_*）
    if (currentPageId) {
      query.pageKey = next
      return query
    }
    query.pageId = next
    delete query.pageKey
    return query
  }
  query.pageKey = next
  return query
}

export function navigateRuntimeEvent(eventItem = {}, ctx = {}) {
  const { route, router, isApplicationRuntimeRoute, appendRuntimeEventParams } = ctx
  if (!eventItem.targetPageKey || !router || !route)
    return false
  const query = { ...route.query }
  applyPageNavigationQuery(query, eventItem.targetPageKey, isApplicationRuntimeRoute)
  if (eventItem.targetFormKey)
    query.formKey = eventItem.targetFormKey
  else
    delete query.formKey
  appendRuntimeEventParams?.(query, eventItem.params || [])
  router.push({ path: route.path, query, hash: route.hash })
  return true
}

export function handleRuntimeEventSuccess(eventItem = {}, ctx = {}) {
  const { router, route } = ctx
  if (eventItem.successBehavior === 'goBack') {
    router?.back?.()
    return
  }
  if (eventItem.successBehavior === 'refreshList' && router && route) {
    router.replace({
      path: route.path,
      query: { ...route.query, _refresh: Date.now() },
      hash: route.hash,
    })
  }
}

async function requestRuntimeEvent(eventItem = {}, ctx = {}) {
  const { appendRuntimeEventParams, resolveRuntimeParamValue } = ctx
  const apiConfig = parseRuntimeApiConfig(eventItem.requestUrl)
  if (!apiConfig.url)
    return
  const payload = {}
  if (appendRuntimeEventParams)
    appendRuntimeEventParams(payload, eventItem.params || [])
  else {
    ;(Array.isArray(eventItem.params) ? eventItem.params : []).forEach((param) => {
      const name = String(param?.name || '').trim()
      if (!name)
        return
      const value = resolveRuntimeParamValue?.(param)
      if (value !== undefined && value !== '')
        payload[name] = value
    })
  }
  await request({
    method: apiConfig.method,
    url: apiConfig.url,
    ...(apiConfig.method === 'get' ? { params: payload } : { data: payload }),
  })
}

function buildFilterParams(eventItem = {}, resolveRuntimeParamValue) {
  const params = {}
  ;(Array.isArray(eventItem.params) ? eventItem.params : []).forEach((param) => {
    const name = String(param?.name || param?.key || '').trim()
    if (!name)
      return
    const value = resolveRuntimeParamValue?.(param)
    if (value !== undefined && value !== '')
      params[name] = value
  })
  return params
}

function runCustomScriptEvent(eventItem = {}, ctx = {}) {
  const handler = String(eventItem.handler || eventItem.script || '').trim()
  const targetId = eventItem.targetBlockId || ctx.selfBlockId || ''
  if (handler === 'refreshList' || handler === 'refreshBlock') {
    if (!refreshRuntimeBlock(targetId))
      handleRuntimeEventSuccess({ successBehavior: 'refreshList' }, ctx)
    return
  }
  if (handler === 'noop' || !handler) {
    const tip = String(ctx.resolveRuntimeText?.(eventItem.description || '') || '').trim()
    if (tip)
      showRuntimeToast(tip, 'info', ctx)
    return
  }
  console.warn(`[grid-block] 自定义脚本不在白名单内: ${handler}`)
}

/**
 * 执行区块主 click 事件；返回是否已处理（含 none）。
 */
export async function runBlockClickEvent(eventItem = {}, ctx = {}) {
  if (!eventItem || eventItem.action === 'none')
    return false
  const {
    resolveRuntimeText,
    resolveRuntimeParamValue,
    selfBlockId = '',
  } = ctx
  if (eventItem.confirmText) {
    const ok = await confirmRuntimeAction(resolveRuntimeText?.(eventItem.confirmText) || eventItem.confirmText)
    if (!ok)
      return true
  }
  const action = String(eventItem.action || '')
  if (action === 'back') {
    if (ctx.router?.options?.history && window.history.length > 1)
      ctx.router.back()
    else if (ctx.router && ctx.route) {
      const query = { ...ctx.route.query }
      ;['pageKey', 'pageId', 'formKey', 'mode', 'id', 'recordId'].forEach(key => delete query[key])
      ctx.router.replace({ path: ctx.route.path, query, hash: ctx.route.hash })
    }
    return true
  }
  if (action === 'navigate') {
    navigateRuntimeEvent(eventItem, ctx)
    return true
  }
  if (action === 'openUrl') {
    const href = String(resolveRuntimeText?.(eventItem.href || eventItem.url || '') || eventItem.href || eventItem.url || '').trim()
    if (href && typeof window !== 'undefined') {
      const safe = /^(https?:|mailto:|tel:|\/)/i.test(href) ? href : ''
      if (safe)
        window.open(safe, '_blank', 'noopener,noreferrer')
      else
        showRuntimeToast('外部链接格式无效', 'warning', ctx)
    }
    return true
  }
  if (action === 'refreshBlock') {
    const targetId = eventItem.targetBlockId || selfBlockId
    if (!refreshRuntimeBlock(targetId))
      handleRuntimeEventSuccess({ successBehavior: 'refreshList' }, ctx)
    handleRuntimeEventSuccess(eventItem, ctx)
    return true
  }
  if (action === 'filterBlock') {
    const targetId = eventItem.targetBlockId || selfBlockId
    const params = buildFilterParams(eventItem, resolveRuntimeParamValue)
    if (!filterRuntimeBlock(targetId, params))
      showRuntimeToast('未找到可过滤的目标列表区块', 'warning', ctx)
    handleRuntimeEventSuccess(eventItem, ctx)
    return true
  }
  if (action === 'openModal' || action === 'openDrawer') {
    const targetId = eventItem.targetBlockId || selfBlockId
    if (!openRuntimeBlockForm(targetId, 'add')) {
      if (eventItem.targetPageKey || eventItem.targetFormKey)
        navigateRuntimeEvent(eventItem, ctx)
      else
        showRuntimeToast('未找到可打开表单的目标区块', 'warning', ctx)
    }
    handleRuntimeEventSuccess(eventItem, ctx)
    return true
  }
  if (action === 'request') {
    await requestRuntimeEvent(eventItem, ctx)
    handleRuntimeEventSuccess(eventItem, ctx)
    return true
  }
  if (action === 'customScript') {
    runCustomScriptEvent(eventItem, ctx)
    handleRuntimeEventSuccess(eventItem, ctx)
    return true
  }
  if (action === 'message' || action === 'toast' || action === 'notify') {
    const content = resolveRuntimeEventMessageContent(eventItem, resolveRuntimeText, resolveRuntimeParamValue)
    if (content)
      showRuntimeToast(content, eventItem.messageType || 'info', ctx)
    handleRuntimeEventSuccess(eventItem, ctx)
    return true
  }
  return false
}
