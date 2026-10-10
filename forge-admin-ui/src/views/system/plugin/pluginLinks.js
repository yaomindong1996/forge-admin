/** 部署地址来自公开环境配置；不把登录 Token 或项目身份拼进外站 URL。 */
export function safePluginLink(value) {
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

export const DEFAULT_PLUGIN_MARKET_URL = 'http://www.dlforgelab.com:8084/forge-official-ui/plugins'

/** 客户端默认使用正式市场，本地开发也不依赖另起门户服务。 */
export function resolvePluginMarketUrl(value) {
  return safePluginLink(String(value || '').trim() || DEFAULT_PLUGIN_MARKET_URL)
}

export const pluginMarketUrl = resolvePluginMarketUrl(import.meta.env.VITE_PLUGIN_MARKET_URL)

export const pluginGuideUrl = safePluginLink(import.meta.env.VITE_PLUGIN_GUIDE_URL
  || 'http://www.dlforgelab.com:8084/forge-docs/guide/plugin-center')
