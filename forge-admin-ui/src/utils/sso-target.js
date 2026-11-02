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
