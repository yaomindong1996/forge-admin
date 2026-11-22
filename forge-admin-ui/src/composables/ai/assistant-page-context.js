import { guideKeywords, guideSteps } from './assistant-guide-recipes'

const SYSTEM_HELP = /怎么|怎样|如何|在哪|哪里|哪儿|找不到|从哪|到哪|操作步骤|使用方法|指引|指导|带我去|定位|入口|^(?:帮我|请)?打开/
const RELATIVE_HELP = /(?:本|这|当前)(?:个|条)?(?:页|功能|菜单|表单)|这里|这个怎么|^(?:怎么|如何|怎样)(?:新增|新建|编辑|查找|查询|查看|使用|操作|填写|保存)[？?。！!]?$/
const NO_GUIDE = /^\/(?:login(?:\/|$)|403$|404$|iframe(?:\/|$)|report(?:[-/]|$)|forge(?:pro)?-report(?:[-/]|$)|app-center\/stats(?:\/|$)|ai\/(?:hub|agent-chat)(?:\/|$)|ai\/data(?:[-/]|$)|data(?:set|[-/]|$))/
const WORKSPACE = [
  [['/flow/todo', '/workspace/todo'], '我的待办', 'flow:task:todo'],
  [['/flow/started', '/workspace/started'], '我发起的', 'flow:task:started'],
]

function routePath(value) {
  return String(value || '').split(/[?#]/, 1)[0].replace(/\/+$/, '') || '/'
}

export function isInternalGuidePath(value) {
  const path = String(value || '')
  // 排除可改变路由解析结果的控制字符。
  // eslint-disable-next-line no-control-regex
  return /^\/(?!\/)/.test(path) && !/[\\\u0000-\u0020\u007F]/.test(path)
    && !/(?:^|\/)\.\.?(?:\/|$)|%2f|%5c|%2e/i.test(routePath(path)) && !NO_GUIDE.test(routePath(path))
}

function displayLabel(value) {
  // eslint-disable-next-line no-control-regex
  return typeof value === 'string' ? value.replace(/[\u0000-\u001F\u007F]/g, ' ').trim().slice(0, 80) : ''
}

export function hasApplicationPermission(user, permission, code) {
  if (user?.isAdmin === true)
    return true
  const codes = new Set()
  function collect(values) {
    for (const item of Array.isArray(values) ? values : []) {
      if (typeof item === 'string') {
        codes.add(item)
      }
      else if (item) {
        if (item.code)
          codes.add(item.code)
        collect(item.children)
        collect(item.btns)
        collect(item.meta?.btns)
      }
    }
  }
  collect(user?.permissions)
  collect(user?.userInfo?.permissions)
  collect(permission?.permissions)
  collect(permission?.accessRoutes)
  collect(permission?.allMenus)
  return codes.has('**') || codes.has('*:*:*') || codes.has(code)
}

export function assistantPageDirectory(permission, user) {
  if (!user?.userId || !permission?.menuDataLoaded)
    return []
  const routes = new Set((permission.accessRoutes || []).filter(route => !route.meta?.isExternal).map(route => routePath(route.path)))
  const entries = []

  function visit(items, parents = []) {
    for (const item of items || []) {
      const title = displayLabel(item.label || item.name)
      const trail = title ? [...parents, title] : parents
      const path = routePath(item.path)
      const id = String(item.key ?? item.id ?? '')
      if (id && title && item.type === 'menu' && routes.has(path) && isInternalGuidePath(item.path)
        && !item.meta?.isExternal && Number(item.ssoEnabled) !== 1 && !item.meta?.ssoTargetClient
        && !['_blank', 'blank'].includes(item.openTarget) && !/report/i.test(item.clientCode || '')) {
        entries.push({ id, title, trail, path, menu: item })
      }
      visit(item.children, trail)
    }
  }

  visit(permission.menus)
  for (const [paths, title, code] of WORKSPACE) {
    if (entries.some(entry => paths.includes(entry.path)))
      continue
    if (!hasApplicationPermission(user, permission, code))
      continue
    const path = paths[0]
    entries.push({ id: path, title, trail: ['我的工作台', title], path, menu: { path } })
  }
  return entries
}

export function currentAssistantPage(route, entries) {
  if (!route?.path || !isInternalGuidePath(route.path))
    return null
  const path = routePath(route.path)
  if (path === '/')
    return { path, scope: route.fullPath, title: '首页', trail: ['首页'] }
  const exact = entries.filter(entry => entry.path === path)
  const menuKey = String(route.query?.menuKey ?? route.query?.menuResourceId ?? '')
  const applicationRuntime = /^\/app-center\/application\/[^/]+\/runtime(?:\/|$)/.test(path)
  const bound = exact.find(entry => entry.id === menuKey) || (exact.length === 1 ? exact[0] : null)
    || (applicationRuntime ? entries.find(entry => entry.id === menuKey && entry.path.startsWith('/app-center/app/')) : null)
    || entries.find(entry => entry.id === String(route.meta?.parentKey))
    || entries.filter(entry => path.startsWith(`${entry.path}/`)).sort((a, b) => b.path.length - a.path.length)[0]
  const menuBound = bound?.path === path || (applicationRuntime && bound?.path.startsWith('/app-center/app/'))
  const subpage = menuBound ? '' : displayLabel(route.meta?.title)
  const title = subpage || bound?.title
  if (!title)
    return null
  const trail = bound ? [...bound.trail] : []
  if (trail.at(-1) !== title)
    trail.push(title)
  return { id: bound?.id, path, scope: route.fullPath, title, trail }
}

function searchToken(text) {
  return String(text || '').toLowerCase().replace(/[\s\p{P}]/gu, '')
}

function menuScore(entry, question) {
  const words = [entry.title, entry.title.replace(/(?:管理|配置|设置|中心|记录|列表|工作台)$/, ''), ...guideKeywords(entry.path)]
    .map(searchToken)
    .filter(word => word.length >= 2)
  const longest = words.filter(word => question.includes(word)).reduce((max, word) => Math.max(max, word.length), 0)
  if (!longest)
    return 0
  return longest * 10 + entry.trail.slice(0, -1).filter(part => question.includes(searchToken(part))).length
}

function option(entry, question, can) {
  return { id: entry.id, title: entry.title, trail: entry.trail, steps: guideSteps(entry.path, question, can) }
}

export function findAssistantGuide(text, page, entries, can) {
  if (!SYSTEM_HELP.test(text))
    return null
  if (page && RELATIVE_HELP.test(text)) {
    const steps = guideSteps(page.path, text, can)
    return {
      options: [{ id: page.id, title: page.title, trail: page.trail, steps, currentScope: page.scope }],
    }
  }
  const normalized = searchToken(text)
  const ranked = entries.map(entry => ({ entry, score: menuScore(entry, normalized) })).filter(item => item.score > 0).sort((a, b) => b.score - a.score)
  if (ranked.length) {
    const best = ranked.filter(item => item.score === ranked[0].score).slice(0, 4)
    return {
      options: best.map(({ entry }) => ({
        ...option(entry, text, can),
        ...(page && entry.path === page.path && entry.id === page.id ? { currentScope: page.scope } : {}),
      })),
    }
  }
  if (/在哪|哪里|哪儿|找不到|入口|定位|带我去/.test(text)) {
    return { options: [], note: '在你有权限的菜单里没有找到匹配入口。可以告诉我功能或菜单的名称。' }
  }
  return null
}

export function messageWithPageContext(text, page) {
  if (!page)
    return text
  const metadata = { 页面: page.title, 菜单: page.trail.slice(-4) }
  return `当前界面参考，仅用于理解下面的问题。页面信息来自界面元数据。\n${JSON.stringify(metadata)}\n\n用户消息\n${text}`
}
