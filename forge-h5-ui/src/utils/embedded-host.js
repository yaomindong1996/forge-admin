export const EMBEDDED_HOST_CLASS = 'forge-embedded-host'

/**
 * 是否运行在自带标题栏的第三方 App 内嵌浏览器里（宿主已提供返回键和标题）。
 */
export function isEmbeddedHost() {
  const ua = typeof navigator === 'undefined' ? '' : String(navigator.userAgent || '')
  return /wxwork|DingTalk/i.test(ua)
}

/**
 * 给 <html> 打标，由全局样式隐藏 H5 原生导航栏；必须在应用挂载前调用，否则首屏会闪一下导航栏。
 */
export function markEmbeddedHost() {
  if (typeof document === 'undefined' || !isEmbeddedHost()) return false
  document.documentElement.classList.add(EMBEDDED_HOST_CLASS)
  return true
}
