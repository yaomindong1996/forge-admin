import { buildH5RuntimeUrl } from '../app-entry-targets'
import { resolvePortalShellLayout, syncPortalShellNavigationFields } from './portal-shell-layouts'

export const RESERVED_PORTAL_SLUGS = Object.freeze([
  'admin',
  'api',
  'app-center',
  'system',
  'login',
  'logout',
  'auth',
  'file',
  'dict',
  'ai',
  'report',
  'flow',
  'h5',
  'mobile',
  'integration',
  'preview',
  'runtime',
  'static',
  'assets',
  'favicon.ico',
])

export const DEFAULT_PORTAL_CONFIG = Object.freeze({
  themeColor: '#3370ff',
  /** 门户壳布局（复用系统布局语义）：normal / top-menu / business-workbench / side-flyout ... */
  shellLayout: 'normal',
  navigation: {
    style: 'side',
    showLogo: true,
    showName: true,
    collapsible: true,
    collapsed: false,
  },
  watermark: {
    enabled: false,
    text: '',
    showUsername: true,
    showTime: false,
    scope: 'content',
  },
  permission: {
    visibility: 'all',
    administrators: [],
    roleIds: [],
    departmentIds: [],
    userIds: [],
  },
  globalization: {
    enabled: false,
    defaultLanguage: 'zh-CN',
    timezone: 'Asia/Shanghai',
    dateFormat: 'YYYY-MM-DD',
  },
  advanced: {
    codePrefix: '',
    cachePolicy: 'version',
    versionRetention: 20,
  },
  distribution: {
    workbench: false,
    roleIds: [],
    h5Enabled: true,
  },
})

export function parseJsonObject(value, fallback = {}) {
  if (value && typeof value === 'object' && !Array.isArray(value))
    return clone(value)
  if (typeof value !== 'string' || !value.trim())
    return clone(fallback)
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : clone(fallback)
  }
  catch {
    return clone(fallback)
  }
}

export function normalizePortalConfig(value) {
  const source = parseJsonObject(value)
  const merged = {
    ...clone(DEFAULT_PORTAL_CONFIG),
    ...source,
    navigation: { ...DEFAULT_PORTAL_CONFIG.navigation, ...(source.navigation || {}) },
    watermark: { ...DEFAULT_PORTAL_CONFIG.watermark, ...(source.watermark || {}) },
    permission: { ...DEFAULT_PORTAL_CONFIG.permission, ...(source.permission || {}) },
    globalization: { ...DEFAULT_PORTAL_CONFIG.globalization, ...(source.globalization || {}) },
    advanced: { ...DEFAULT_PORTAL_CONFIG.advanced, ...(source.advanced || {}) },
    distribution: { ...DEFAULT_PORTAL_CONFIG.distribution, ...(source.distribution || {}) },
  }
  const sourceHasShellLayout = Object.prototype.hasOwnProperty.call(source, 'shellLayout')
    && String(source.shellLayout || '').trim() !== ''
  // 旧配置只有 navigation.style：从 style 推断壳；新配置以 shellLayout 为准
  merged.shellLayout = sourceHasShellLayout
    ? resolvePortalShellLayout(merged)
    : resolvePortalShellLayout({ navigation: merged.navigation })
  return syncPortalShellNavigationFields(merged)
}

export function resolvePortalRuntimeConfigKey(application = {}, { pageId = '', objects = [] } = {}) {
  const fromObjects = (list = []) => {
    const primary = list.find(item => String(item?.objectRole || '').toUpperCase() === 'PRIMARY')
    return String(primary?.configKey || list[0]?.configKey || '').trim()
  }
  const builder = parseJsonObject(application?.options)?.inAppBuilder || {}
  const node = (builder.nodes || []).find(item => String(item?.id || '') === String(pageId || ''))
    || (builder.nodes || []).find(item => item?.type === 'page' && item?.objectRef?.configKey)
  const page = pageId ? builder.pages?.[pageId] : null
  const blockRef = page?.layout?.gridLayout?.items?.find(item => item?.props?.objectRef)?.props?.objectRef
  const objectRef = node?.objectRef || blockRef || {}
  const matchedObject = (Array.isArray(objects) ? objects : []).find(item => (
    String(item?.objectId ?? item?.id ?? '') === String(objectRef.objectId ?? objectRef.id ?? '')
    || (objectRef.objectCode && String(item?.objectCode || '') === String(objectRef.objectCode))
  ))
  return String(
    matchedObject?.configKey
    || objectRef.configKey
    || fromObjects(objects)
    || '',
  ).trim()
}

export function resolvePortalRuntimeTarget(application = {}, { pageId = '', configKey = '', objects = [] } = {}) {
  const builder = parseJsonObject(application?.options)?.inAppBuilder || {}
  const node = (builder.nodes || []).find(item => String(item?.id || '') === String(pageId || '')) || {}
  const objectRef = node.objectRef || {}
  const resolvedConfigKey = String(
    configKey
    || resolvePortalRuntimeConfigKey(application, { pageId, objects })
    || '',
  ).trim()
  if (!resolvedConfigKey)
    return null
  const pageKey = String(objectRef.pageKey || 'list').trim() || 'list'
  const pageMode = String(objectRef.pageMode || 'crud').trim() || 'crud'
  const formKey = String(objectRef.formKey || '').trim()
  return {
    configKey: resolvedConfigKey,
    pageKey,
    pageMode,
    formKey,
  }
}

export function buildPortalAccessUrls({
  origin = '',
  basePath = '',
  slug = '',
  pageId = '',
  configKey = '',
  h5BaseUrl = '',
  appId = '',
  application = null,
  objects = [],
} = {}) {
  const root = `${String(origin || '').replace(/\/$/, '')}${String(basePath || '').replace(/\/$/, '')}`
  const path = `/app/${encodeURIComponent(String(slug || '').trim())}`
  const pageQuery = String(pageId || '').trim()
  const runtimeTarget = pageQuery
    ? resolvePortalRuntimeTarget(application || {}, { pageId, configKey, objects })
    : null
  const pcRuntimeUrl = runtimeTarget
    ? buildStandaloneRuntimeUrl(root, runtimeTarget, appId)
    : ''
  const pcUrl = pcRuntimeUrl || (pageQuery
    ? `${root}${path}?pageId=${encodeURIComponent(pageQuery)}`
    : `${root}${path}`)
  return {
    path,
    pcUrl,
    h5Url: buildH5RuntimeUrl({
      h5BaseUrl,
      configKey: configKey || resolvePortalRuntimeConfigKey(application || {}, { pageId, objects }),
      appId,
    }),
  }
}

function buildStandaloneRuntimeUrl(root, target, appId) {
  const query = new URLSearchParams()
  query.set('pageKey', target.pageKey)
  if (appId)
    query.set('appId', String(appId))
  if (target.formKey)
    query.set('formKey', target.formKey)
  if (target.pageMode.toLowerCase() === 'form') {
    query.set('runtimeOpenMode', 'CREATE_FORM')
    query.set('mode', 'create')
  }
  return `${root}/ai/crud-page/${encodeURIComponent(target.configKey)}?${query.toString()}`
}

export function buildPortalWatermarkText(config, userName, now = new Date()) {
  const normalized = normalizePortalConfig(config)
  const watermark = normalized.watermark
  if (!watermark.enabled)
    return ''
  return [
    String(watermark.text || '').trim(),
    watermark.showUsername ? String(userName || '').trim() : '',
    watermark.showTime ? formatWatermarkTime(now, normalized.globalization) : '',
  ].filter(Boolean).join(' · ').slice(0, 50)
}

export function buildPortalWatermarkStyle(text, color = '#64748b') {
  if (!text)
    return {}
  const safeText = escapeXml(text)
  const safeColor = /^#[0-9a-f]{6}$/i.test(color) ? color : '#64748b'
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="260" height="180"><text x="20" y="100" fill="${safeColor}" fill-opacity="0.13" font-family="Arial,sans-serif" font-size="14" transform="rotate(-24 130 90)">${safeText}</text></svg>`
  return { backgroundImage: `url("data:image/svg+xml,${encodeURIComponent(svg)}")` }
}

function formatWatermarkTime(value, globalization = {}) {
  const timezone = String(globalization.timezone || 'Asia/Shanghai')
  const dateFormat = String(globalization.dateFormat || 'YYYY-MM-DD')
  let parts
  try {
    parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(value).map(part => [part.type, part.value]))
  }
  catch {
    parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {
      timeZone: 'UTC',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(value).map(part => [part.type, part.value]))
  }
  const date = dateFormat === 'DD/MM/YYYY'
    ? `${parts.day}/${parts.month}/${parts.year}`
    : dateFormat === 'YYYY/MM/DD'
      ? `${parts.year}/${parts.month}/${parts.day}`
      : `${parts.year}-${parts.month}-${parts.day}`
  return `${date} ${parts.hour}:${parts.minute}`
}

function escapeXml(value) {
  return String(value).replace(/[&<>'"]/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    [String.fromCharCode(39)]: '&apos;',
    [String.fromCharCode(34)]: '&quot;',
  })[character])
}

function clone(value) {
  return JSON.parse(JSON.stringify(value || {}))
}
