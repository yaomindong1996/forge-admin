/**
 * 解析 Cursor / Claude Desktop 风格的 mcpServers JSON，转为社区版 MCP 接入表单。
 * 仅支持 type=http（或省略 type 但带 url）的远程 Streamable HTTP 服务。
 */
export function parseMcpServersConfig(raw) {
  const text = String(raw || '').trim()
  if (!text)
    throw new Error('请粘贴 MCP 配置 JSON')

  let data
  try {
    data = JSON.parse(text)
  }
  catch {
    throw new Error('JSON 格式无效，请检查是否完整复制')
  }

  const servers = data?.mcpServers && typeof data.mcpServers === 'object'
    ? data.mcpServers
    : (data && typeof data === 'object' && !Array.isArray(data) ? data : null)
  if (!servers)
    throw new Error('未找到 mcpServers 对象')

  const entries = []
  for (const [key, value] of Object.entries(servers)) {
    if (!value || typeof value !== 'object')
      continue
    const type = String(value.type || (value.url ? 'http' : '')).toLowerCase()
    if (type && type !== 'http' && type !== 'sse')
      continue
    const url = String(value.url || value.serverUrl || '').trim()
    if (!url)
      continue
    const headers = value.headers && typeof value.headers === 'object' ? value.headers : {}
    const auth = String(headers.Authorization || headers.authorization || '').trim()
    const token = auth.replace(/^Bearer\s+/i, '').trim()
    const code = String(key).trim().replace(/[^a-zA-Z0-9_-]/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '') || 'mcp-service'
    entries.push({
      connectionCode: code.slice(0, 64),
      connectionName: String(value.name || key).trim().slice(0, 80) || code,
      endpoint: url,
      token,
      timeoutSeconds: Number(value.timeoutSeconds) > 0 ? Math.min(60, Number(value.timeoutSeconds)) : 15,
      enabled: 1,
    })
  }

  if (!entries.length)
    throw new Error('没有可导入的 HTTP MCP 服务（需要 url 字段）')
  return entries
}

export function mcpOriginOf(endpoint) {
  try {
    const uri = new URL(endpoint)
    const port = uri.port || (uri.protocol === 'https:' ? '443' : '80')
    return `${uri.protocol}//${uri.hostname.toLowerCase()}:${port}`
  }
  catch {
    return ''
  }
}
