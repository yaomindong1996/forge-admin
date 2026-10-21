import { ref } from 'vue'

/**
 * 导航过程中提前锁定壳层，避免仍停在系统布局时内容已卸掉。
 * beforeEach 写入，afterEach 在目标 meta 生效后清掉。
 */
export const pendingShellLayout = ref(null)

function normalizeRoutePath(path) {
  const value = String(path || '').trim()
  if (!value)
    return ''
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = String(pathname || '').replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return normalized || '/'
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

/** 与 permission-guard 同源判断，放本文件避免循环依赖 */
export function isApplicationPortalPath(path) {
  const targetPath = normalizeRoutePath(path)
  return targetPath === '/app' || (targetPath.startsWith('/app/') && !targetPath.startsWith('/app-center'))
}

/** 读当前浏览器路径（兼容 hash history），用于首屏 meta 尚未确认时判壳。 */
export function readBrowserPathname() {
  if (typeof window === 'undefined')
    return ''
  if (import.meta.env.VITE_USE_HASH === 'true') {
    const hash = window.location.hash || ''
    const raw = hash.startsWith('#') ? hash.slice(1) : hash
    return normalizeRoutePath(raw.split('?')[0] || '/')
  }
  return normalizeRoutePath(window.location.pathname || '/')
}

/**
 * 全屏空壳页：设计器 / 运行工作台 / 登录与错误页等，禁止回退到系统侧栏。
 */
export function isEmptyShellPath(path) {
  const targetPath = normalizeRoutePath(path)
  if (!targetPath || targetPath === '/')
    return false
  if ([
    '/login',
    '/login/callback',
    '/404',
    '/403',
    '/mcp-authorize',
  ].includes(targetPath)) {
    return true
  }
  if (/^\/app-center\/application\/[^/]+\/(?:runtime|publish|settings|preview)(?:\/|$)/.test(targetPath))
    return true
  // 旧入口只做 redirect → runtime，中间帧也要空壳，避免闪系统菜单
  if (/^\/app-center\/application\/[^/]+\/?$/.test(targetPath))
    return true
  if (/^\/app-center\/business-process\/[^/]+(?:\/|$)/.test(targetPath))
    return true
  if (/^\/print\/(?:designer|preview)(?:\/|$)/.test(targetPath))
    return true
  return false
}

export function isApplicationRuntimePath(path) {
  const targetPath = normalizeRoutePath(path)
  return /^\/app-center\/application\/[^/]+\/runtime(?:\/|$)/.test(targetPath)
}

export function resolvePendingShellLayout(to = {}) {
  const metaLayout = to?.meta?.layout
  if (metaLayout === 'empty' || metaLayout === 'app-portal')
    return metaLayout
  if (isApplicationPortalPath(to?.path))
    return 'app-portal'
  if (isEmptyShellPath(to?.path) || isApplicationRuntimePath(to?.path))
    return 'empty'
  return null
}

/**
 * 解析当前应使用的壳布局名。
 * 优先级：pending → route.meta.layout → URL 推断 → 用户偏好系统布局
 */
export function resolveShellLayoutName({
  routeMetaLayout,
  routePath,
  locationPath,
  pending,
  appLayout,
} = {}) {
  if (pending === 'empty' || pending === 'app-portal')
    return pending
  if (routeMetaLayout)
    return routeMetaLayout

  const candidates = [routePath, locationPath].filter(Boolean)
  for (const candidate of candidates) {
    if (isApplicationPortalPath(candidate))
      return 'app-portal'
    if (isEmptyShellPath(candidate))
      return 'empty'
  }
  return appLayout
}
