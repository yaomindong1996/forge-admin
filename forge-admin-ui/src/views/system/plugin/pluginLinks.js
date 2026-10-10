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

export const pluginMarketUrl = safePluginLink(import.meta.env.VITE_PLUGIN_MARKET_URL
  || (import.meta.env.DEV ? 'http://localhost:5174/plugins' : ''))

export const pluginGuideUrl = safePluginLink(import.meta.env.VITE_PLUGIN_GUIDE_URL
  || 'http://www.dlforgelab.com:8084/forge-docs/guide/plugin-center')
