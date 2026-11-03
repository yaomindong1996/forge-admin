import { isExternal } from '@/utils/is'

function trimString(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeRoutePath(value, fallback = '') {
  const raw = trimString(value) || fallback
  if (!raw) {
    return ''
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

function normalizeRedirectPathValue(value, fallback = '/') {
  const raw = trimString(value)
  if (!raw || raw.includes('://') || raw.startsWith('//')) {
    return fallback
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

function parseEnvJsonMap(rawValue) {
  const value = trimString(rawValue)
  if (!value) {
    return {}
  }

  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch (error) {
    console.warn('[SSO] VITE_SSO_TARGET_BASE_URLS 解析失败:', error)
    return {}
  }
}

export function normalizeSsoBaseUrl(value) {
  return trimString(value).replace(/\/+$/, '')
}

export const SSO_BRIDGE_ROUTE = normalizeRoutePath(import.meta.env.VITE_SSO_BRIDGE_ROUTE)
export const DEFAULT_SSO_TARGET_CLIENT = trimString(import.meta.env.VITE_SSO_TARGET_CLIENT)
export const DEFAULT_REPORT_BASE_URL = normalizeSsoBaseUrl(import.meta.env.VITE_REPORT_UI_BASE_URL)
export const REPORT_HOST_FALLBACK = trimString(import.meta.env.VITE_REPORT_UI_HOST_FALLBACK)
export const REPORT_PATH_PREFIX = normalizeRoutePath(import.meta.env.VITE_REPORT_UI_PATH_PREFIX)

const envDefaultRedirectMap = parseEnvJsonMap(import.meta.env.VITE_SSO_DEFAULT_REDIRECTS)
const DEFAULT_SSO_ENTRY_MAP = Object.entries(envDefaultRedirectMap).reduce((acc, [key, value]) => {
  const redirectPath = normalizeRedirectPathValue(value, '')
  if (key && redirectPath) {
    acc[key] = redirectPath
  }
  return acc
}, {})

const reportDefaultRedirect = normalizeRedirectPathValue(import.meta.env.VITE_REPORT_UI_DEFAULT_REDIRECT, '')
if (DEFAULT_SSO_TARGET_CLIENT && reportDefaultRedirect && !DEFAULT_SSO_ENTRY_MAP[DEFAULT_SSO_TARGET_CLIENT]) {
  DEFAULT_SSO_ENTRY_MAP[DEFAULT_SSO_TARGET_CLIENT] = reportDefaultRedirect
}

export function getDefaultSsoRedirectPath(targetClient = DEFAULT_SSO_TARGET_CLIENT) {
  return DEFAULT_SSO_ENTRY_MAP[targetClient] || '/'
}

export function normalizeSsoRedirectPath(value, targetClient = DEFAULT_SSO_TARGET_CLIENT) {
  const raw = trimString(value)
  if (!raw || raw.includes('://') || raw.startsWith('//')) {
    return getDefaultSsoRedirectPath(targetClient)
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

export function parseExternalSsoTarget(rawTarget, targetClient = DEFAULT_SSO_TARGET_CLIENT) {
  const target = trimString(rawTarget)
  if (!target || !isExternal(target)) {
    return null
  }

  try {
    const targetUrl = new URL(target)
    const baseUrl = normalizeSsoBaseUrl(`${targetUrl.origin}${targetUrl.pathname}`)
    let redirectPath = ''

    if (targetUrl.hash) {
      const hashValue = targetUrl.hash.replace(/^#/, '')
      if (hashValue.startsWith('/')) {
        const [hashPath] = hashValue.split('?')
        redirectPath = hashPath
      }
    }

    return {
      baseUrl,
      redirectPath: normalizeSsoRedirectPath(redirectPath, targetClient),
    }
  }
  catch {
    return null
  }
}

/** 运行时来自「客户端配置」homeUrl，优先于构建期环境变量 */
const runtimeClientHomeUrlMap = Object.create(null)
let clientHomeUrlsLoaded = false
let clientHomeUrlLoadPromise = null

export function setRuntimeClientHomeUrls(clients = []) {
  Object.keys(runtimeClientHomeUrlMap).forEach((key) => {
    delete runtimeClientHomeUrlMap[key]
  })
  ;(Array.isArray(clients) ? clients : []).forEach((item) => {
    const clientCode = trimString(item?.clientCode)
    const homeUrl = normalizeSsoBaseUrl(item?.homeUrl)
    if (clientCode && homeUrl) {
      runtimeClientHomeUrlMap[clientCode] = homeUrl
    }
  })
  clientHomeUrlsLoaded = true
}

export function getRuntimeClientHomeUrl(targetClient) {
  const clientCode = trimString(targetClient)
  return clientCode ? (runtimeClientHomeUrlMap[clientCode] || '') : ''
}

/**
 * 拉取启用客户端列表并缓存 homeUrl，供 SSO 跳转使用。
 * @param {() => Promise<{ data?: Array }>} listClients 默认请求 /system/client/list
 */
export async function ensureClientHomeUrlsLoaded(listClients) {
  if (clientHomeUrlsLoaded) {
    return runtimeClientHomeUrlMap
  }
  if (clientHomeUrlLoadPromise) {
    return clientHomeUrlLoadPromise
  }

  clientHomeUrlLoadPromise = Promise.resolve()
    .then(async () => {
      if (typeof listClients === 'function') {
        const res = await listClients()
        setRuntimeClientHomeUrls(res?.data || res || [])
        return runtimeClientHomeUrlMap
      }
      const { request } = await import('@/utils')
      const res = await request.get('/system/client/list')
      setRuntimeClientHomeUrls(res?.data || [])
      return runtimeClientHomeUrlMap
    })
    .catch((error) => {
      console.warn('[SSO] 加载客户端入口地址失败:', error)
      return runtimeClientHomeUrlMap
    })
    .finally(() => {
      clientHomeUrlLoadPromise = null
    })

  return clientHomeUrlLoadPromise
}

/** 客户端配置变更后强制重新拉取 */
export function invalidateClientHomeUrlCache() {
  Object.keys(runtimeClientHomeUrlMap).forEach((key) => {
    delete runtimeClientHomeUrlMap[key]
  })
  clientHomeUrlsLoaded = false
  clientHomeUrlLoadPromise = null
}

export function getConfiguredSsoTargetBaseUrls() {
  const envMap = parseEnvJsonMap(import.meta.env.VITE_SSO_TARGET_BASE_URLS)
  const normalizedMap = Object.entries(envMap).reduce((acc, [key, value]) => {
    const normalizedValue = normalizeSsoBaseUrl(value)
    if (normalizedValue) {
      acc[key] = normalizedValue
    }
    return acc
  }, {})

  const legacyReportBaseUrl = DEFAULT_REPORT_BASE_URL

  if (DEFAULT_SSO_TARGET_CLIENT && legacyReportBaseUrl && !normalizedMap[DEFAULT_SSO_TARGET_CLIENT]) {
    normalizedMap[DEFAULT_SSO_TARGET_CLIENT] = legacyReportBaseUrl
  }

  return normalizedMap
}

/**
 * 解析子系统 baseUrl。
 * 优先级：菜单显式完整 URL → 客户端配置 homeUrl → 构建期环境变量兜底。
 */
export function resolveSsoTargetBaseUrl({ targetClient = DEFAULT_SSO_TARGET_CLIENT, preferredBaseUrl = '' } = {}) {
  const normalizedPreferredBaseUrl = normalizeSsoBaseUrl(preferredBaseUrl)
  if (normalizedPreferredBaseUrl) {
    return normalizedPreferredBaseUrl
  }

  const runtimeHomeUrl = getRuntimeClientHomeUrl(targetClient)
  if (runtimeHomeUrl) {
    return runtimeHomeUrl
  }

  const configuredBaseUrls = getConfiguredSsoTargetBaseUrls()
  return configuredBaseUrls[targetClient] || ''
}

/**
 * 只接受可导航的 http(s) 绝对地址。
 * 拒绝 javascript:、协议相对地址和带 userinfo 的 URL，避免票据被带到非预期来源。
 */
export function normalizeHttpSsoBaseUrl(value) {
  const trimmed = normalizeSsoBaseUrl(value)
  if (!trimmed || trimmed.startsWith('//'))
    return ''

  let url
  try {
    url = new URL(trimmed)
  }
  catch {
    return ''
  }
  if (url.username || url.password)
    return ''
  if (url.protocol !== 'http:' && url.protocol !== 'https:')
    return ''
  return normalizeSsoBaseUrl(`${url.origin}${url.pathname}`)
}

function isNestedHttpBase(candidate, allowedBase) {
  const left = normalizeHttpSsoBaseUrl(candidate)
  const right = normalizeHttpSsoBaseUrl(allowedBase)
  if (!left || !right)
    return false
  if (left === right)
    return true

  const candidateUrl = new URL(left)
  const allowedUrl = new URL(right)
  if (candidateUrl.origin !== allowedUrl.origin)
    return false
  const allowedPath = allowedUrl.pathname.endsWith('/')
    ? allowedUrl.pathname
    : `${allowedUrl.pathname}/`
  return candidateUrl.pathname.startsWith(allowedPath)
}

function matchesReportHostFallback(candidate) {
  if (!REPORT_HOST_FALLBACK)
    return false
  const normalized = normalizeHttpSsoBaseUrl(candidate)
  if (!normalized)
    return false
  return new URL(normalized).host === REPORT_HOST_FALLBACK
}

/**
 * 桥接页只能跳到已配置入口、当前用户菜单里的入口，或报表宿主本机兜底。
 * 查询参数里的任意 baseUrl 不能单独放行，否则会把 SSO 票据交给攻击者站点。
 * 显式传入但不在允许范围内时返回空串，调用方不得继续出票。
 */
export function selectTrustedSsoBaseUrl({ requestedBaseUrl = '', allowedBaseUrls = [] } = {}) {
  const allowed = (Array.isArray(allowedBaseUrls) ? allowedBaseUrls : [])
    .map(item => normalizeHttpSsoBaseUrl(item))
    .filter(Boolean)
  const requestedRaw = trimString(requestedBaseUrl)
  if (!requestedRaw)
    return allowed[0] || ''

  const requested = normalizeHttpSsoBaseUrl(requestedRaw)
  const trusted = Boolean(requested) && (
    allowed.some(base => isNestedHttpBase(requested, base))
    || matchesReportHostFallback(requested)
  )
  return trusted ? requested : ''
}
