import { normalizePageTitle, setDocumentTitle } from '@/utils/page-title'
import { readTenantAppearance } from '@/utils/tenant-appearance'

const MANAGED_FILE_ID_PATTERN = /^[\w-]{8,128}$/

function getRequestPrefix() {
  return import.meta.env.VITE_REQUEST_PREFIX || ''
}

function normalizeTenantId(value) {
  if (Array.isArray(value)) {
    const first = value.find(item => item !== null && item !== undefined && item !== '')
    return normalizeTenantId(first)
  }
  if (value === null || value === undefined || value === '')
    return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

function isDirectRenderableReference(value = '') {
  const text = String(value || '').trim().toLowerCase()
  return text.startsWith('http://')
    || text.startsWith('https://')
    || text.startsWith('data:')
    || text.startsWith('blob:')
}

function isManagedFileReference(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return false
  if (text.includes('/api/file/download/') || text.includes('/api/file/url/'))
    return true
  return !text.startsWith('/') && !text.includes('/') && !text.includes('\\') && MANAGED_FILE_ID_PATTERN.test(text)
}

function extractManagedFileId(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return ''

  const normalizedText = text.toLowerCase()
  let fileId = ''

  if (normalizedText.includes('/api/file/download/')) {
    fileId = text.split('/api/file/download/').pop() || ''
  }
  else if (normalizedText.includes('/api/file/url/')) {
    fileId = text.split('/api/file/url/').pop() || ''
  }
  else if (!text.startsWith('/') && !text.includes('/') && !text.includes('\\')) {
    fileId = text
  }

  if (!fileId)
    return ''

  const queryIndex = fileId.indexOf('?')
  if (queryIndex >= 0)
    fileId = fileId.slice(0, queryIndex)

  const hashIndex = fileId.indexOf('#')
  if (hashIndex >= 0)
    fileId = fileId.slice(0, hashIndex)

  return MANAGED_FILE_ID_PATTERN.test(fileId) ? fileId : ''
}

function resolveAssetReference(tenantConfig, assetType) {
  if (!tenantConfig)
    return ''
  if (assetType === 'logo')
    return tenantConfig.systemLogo || ''
  return tenantConfig.browserIcon || ''
}

function buildAssetCacheBuster(assetReference) {
  const fileId = extractManagedFileId(assetReference)
  return fileId ? `?v=${encodeURIComponent(fileId)}` : ''
}

export function resolveTenantPublicAssetUrl(tenantConfig, assetType = 'logo') {
  const assetReference = String(resolveAssetReference(tenantConfig, assetType) || '').trim()
  if (!assetReference)
    return ''
  if (isDirectRenderableReference(assetReference))
    return assetReference
  if (assetReference.startsWith('/') && !isManagedFileReference(assetReference))
    return assetReference

  const tenantId = normalizeTenantId(tenantConfig?.tenantId ?? tenantConfig?.id)
  if (!tenantId || !isManagedFileReference(assetReference))
    return ''

  const normalizedAssetType = assetType === 'favicon' ? 'icon' : assetType
  return `${getRequestPrefix()}/auth/tenant/assets/${tenantId}/${normalizedAssetType}${buildAssetCacheBuster(assetReference)}`
}

export function getDefaultFaviconUrl() {
  const base = import.meta.env.BASE_URL || '/'
  const prefix = base.endsWith('/') ? base : `${base}/`
  return `${prefix}favicon-32.png`
}

/**
 * 浏览器标签页图标只能用「无需鉴权头」的地址。
 * blob:/api/file/* 文件 ID 在 <link rel="icon"> 上会 401/失效，Chrome 就会显示地球图标。
 */
export function isBrowserSafeFaviconUrl(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return false
  const lower = text.toLowerCase()
  if (lower.startsWith('blob:'))
    return false
  if (lower.includes('/api/file/'))
    return false
  if (lower.startsWith('data:image/'))
    return true
  if (lower.startsWith('http://') || lower.startsWith('https://'))
    return true
  if (text.includes('/auth/tenant/assets/'))
    return true
  if ((lower.endsWith('.png') || lower.endsWith('.ico') || lower.endsWith('.svg') || lower.endsWith('.webp'))
    && (text.startsWith('/') || text.startsWith('.'))) {
    return true
  }
  return false
}

/** 解析租户浏览器图标；不可用则返回空串，由 setDocumentFavicon 回退默认图标 */
export function resolveTenantFaviconUrl(tenantConfig) {
  if (!tenantConfig?.browserIcon)
    return ''
  const publicUrl = resolveTenantPublicAssetUrl(tenantConfig, 'icon')
  if (isBrowserSafeFaviconUrl(publicUrl))
    return publicUrl
  const raw = String(tenantConfig.browserIcon || '').trim()
  if (isBrowserSafeFaviconUrl(raw))
    return raw
  return ''
}

export function setDocumentFavicon(iconUrl) {
  const head = document.head || document.getElementsByTagName('head')[0]
  if (!head)
    return

  const preferred = String(iconUrl || '').trim()
  const href = isBrowserSafeFaviconUrl(preferred) ? preferred : getDefaultFaviconUrl()

  // 只替换 favicon，保留 apple-touch-icon（旧逻辑 rel*="icon" 会误删）
  document.querySelectorAll('link[rel="icon"], link[rel="shortcut icon"]').forEach(link => link.remove())

  const icon = document.createElement('link')
  icon.rel = 'icon'
  icon.type = href.toLowerCase().includes('.ico') ? 'image/x-icon' : 'image/png'
  icon.href = href
  head.appendChild(icon)

  const shortcut = document.createElement('link')
  shortcut.rel = 'shortcut icon'
  shortcut.href = href
  head.appendChild(shortcut)

  if (!document.querySelector('link[rel="apple-touch-icon"]')) {
    const base = import.meta.env.BASE_URL || '/'
    const prefix = base.endsWith('/') ? base : `${base}/`
    const apple = document.createElement('link')
    apple.rel = 'apple-touch-icon'
    apple.href = `${prefix}apple-touch-icon.png`
    head.appendChild(apple)
  }
}

/** 同页生命周期内已完整套用的租户品牌指纹，避免路由切换反复刷主题 */
let appliedTenantBrandingKey = ''

/** @internal vitest 隔离用 */
export function resetTenantBrandingStateForTests() {
  appliedTenantBrandingKey = ''
}

function resolveTenantBrandingKey(tenantConfig = {}) {
  return [
    tenantConfig.id ?? tenantConfig.tenantId ?? '',
    tenantConfig.browserIcon || '',
    tenantConfig.browserTitle || '',
    tenantConfig.systemName || '',
  ].join('|')
}

export async function applyTenantConfig(tenantConfig, appStore) {
  // 无配置才回默认；有配置时直接替换外观，避免先闪系统默认色再套租户主题。
  if (!tenantConfig) {
    appStore.resetAccountState()
    setDocumentFavicon('')
    appliedTenantBrandingKey = ''
    return
  }

  // 登录与外观恢复共用解析器，历史 systemTheme=light 不得覆盖 JSON 内合法主色。
  appStore.restoreTenantAppearance(tenantConfig)

  const pageBaseTitle = normalizePageTitle(tenantConfig.browserTitle) || normalizePageTitle(tenantConfig.systemName)
  if (pageBaseTitle) {
    setDocumentTitle(pageBaseTitle)
  }

  // 标签页图标只用公开地址；不再走 resolveRenderableFileUrl（会生成 blob，标签页常显示地球）
  setDocumentFavicon(resolveTenantFaviconUrl(tenantConfig))
  appliedTenantBrandingKey = resolveTenantBrandingKey(tenantConfig)
}

/**
 * 已登录页面（含新开「运行」标签）补齐租户品牌。
 * 用户信息在 localStorage，租户配置若只在 sessionStorage：新标签有 user 无 tenant，
 * 门户/运行页又 defer 菜单引导，会跳过 applyTenantConfig，标签页就一直是地球图标。
 */
export async function ensureTenantBranding(appStore, options = {}) {
  const tenantId = options.tenantId
  let tenantStore = options.tenantStore
  if (!tenantStore) {
    const { useTenantStore } = await import('@/store/modules/tenant.js')
    tenantStore = useTenantStore()
  }
  let config = tenantStore.config
  if (!config)
    config = await tenantStore.loadTenantConfig(tenantId)
  if (!config) {
    setDocumentFavicon('')
    appliedTenantBrandingKey = ''
    return null
  }
  const brandingKey = resolveTenantBrandingKey(config)
  // 仅补图标：显式要求，或本页已完整套用过同一份配置
  if (options.faviconOnly || appliedTenantBrandingKey === brandingKey) {
    setDocumentFavicon(resolveTenantFaviconUrl(config))
    return config
  }
  await applyTenantConfig(config, appStore)
  appliedTenantBrandingKey = brandingKey
  return config
}
