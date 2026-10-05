/** 品牌区和首页跳转共用，避免链到 `/` 这种只有 redirect 的路由。 */
export function getHomePath() {
  return window.$homePath || import.meta.env.VITE_HOME_PATH || '/home'
}

export function normalizeTabPathname(path) {
  const value = String(path || '').trim()
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = String(pathname || '').replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return '/'
  const withSlash = normalized.startsWith('/') ? normalized : `/${normalized}`
  return withSlash.length > 1 ? withSlash.replace(/\/$/, '') : withSlash
}

export function isHomeTabPath(path) {
  const pathname = normalizeTabPathname(path)
  return pathname === '/' || pathname === '/home' || pathname === normalizeTabPathname(getHomePath())
}
