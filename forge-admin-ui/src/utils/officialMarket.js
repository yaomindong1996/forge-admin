/** 部署地址来自公开环境配置；不把登录 Token 或项目身份拼进外站 URL。 */
export function safeExternalHttpLink(value) {
  try {
    const url = new URL(String(value || '').trim())
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password)
      return ''
    return url.href
  }
  catch {
    return ''
  }
}

export const DEFAULT_OFFICIAL_PORTAL_BASE = 'http://www.dlforgelab.com:8084/forge-official-ui'

export const OFFICIAL_MARKET_KINDS = {
  plugin: { path: '/plugins', query: {}, openLabel: '打开插件市场', title: '插件市场' },
  app: { path: '/templates', query: { kind: 'app' }, openLabel: '打开应用市场', title: '应用市场' },
  print: { path: '/templates', query: { kind: 'print' }, openLabel: '打开模板市场', title: '打印模板市场' },
  form: { path: '/templates', query: { kind: 'form' }, openLabel: '打开模板市场', title: '表单模板市场' },
  'print-component': { path: '/templates', query: { kind: 'print-component' }, openLabel: '打开组件市场', title: '打印组件市场' },
}

/** 官网门户根路径。可从门户地址或插件市场地址推导，去掉 /plugins、/templates。 */
export function resolveOfficialPortalBase(value) {
  const href = safeExternalHttpLink(String(value || '').trim() || DEFAULT_OFFICIAL_PORTAL_BASE)
  if (!href)
    return ''
  const url = new URL(href)
  let pathname = url.pathname.replace(/\/+$/, '')
  if (pathname.endsWith('/plugins'))
    pathname = pathname.slice(0, -'/plugins'.length)
  if (pathname.endsWith('/templates'))
    pathname = pathname.slice(0, -'/templates'.length)
  pathname = pathname.replace(/\/+$/, '')
  return `${url.origin}${pathname}`
}

function withSearchQuery(href, q) {
  const query = String(q || '').trim()
  if (!href || !query)
    return href || ''
  const url = new URL(href)
  url.searchParams.set('q', query)
  return url.href
}

export function resolveOfficialMarketUrl(kind, options = {}) {
  const spec = OFFICIAL_MARKET_KINDS[kind]
  if (!spec && !options.explicit)
    return ''
  const explicit = safeExternalHttpLink(options.explicit)
  if (explicit)
    return withSearchQuery(explicit, options.q)
  const base = resolveOfficialPortalBase(options.portalBase || options.pluginMarket)
  if (!base)
    return ''
  const url = new URL(`${base.replace(/\/+$/, '')}${spec.path}`)
  Object.entries(spec.query).forEach(([key, value]) => url.searchParams.set(key, value))
  return withSearchQuery(url.href, options.q)
}

const KIND_ENV = {
  plugin: 'VITE_PLUGIN_MARKET_URL',
  app: 'VITE_APP_TEMPLATE_MARKET_URL',
  print: 'VITE_PRINT_TEMPLATE_MARKET_URL',
  form: 'VITE_FORM_TEMPLATE_MARKET_URL',
  'print-component': 'VITE_PRINT_COMPONENT_MARKET_URL',
}

/** 运行时入口：优先完整覆盖地址，否则拼官网对应市场页，可带搜索词。 */
export function officialMarketUrl(kind, extras = {}) {
  const env = import.meta.env || {}
  return resolveOfficialMarketUrl(kind, {
    explicit: env[KIND_ENV[kind]],
    portalBase: env.VITE_OFFICIAL_MARKET_BASE_URL,
    pluginMarket: env.VITE_PLUGIN_MARKET_URL,
    q: extras.q,
  })
}

export function officialMarketOpenLabel(kind) {
  return OFFICIAL_MARKET_KINDS[kind]?.openLabel || '打开市场'
}

export function officialMarketTitle(kind) {
  return OFFICIAL_MARKET_KINDS[kind]?.title || '市场'
}
