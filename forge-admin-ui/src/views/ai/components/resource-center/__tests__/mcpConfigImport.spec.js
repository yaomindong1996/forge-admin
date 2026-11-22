import { describe, expect, it } from 'vitest'
import { mcpOriginOf, parseMcpServersConfig } from '../mcpConfigImport'

describe('mcpConfigImport', () => {
  it('parses Cursor mcpServers http config with Bearer token', () => {
    const entries = parseMcpServersConfig(JSON.stringify({
      mcpServers: {
        'forge-docs': {
          type: 'http',
          url: 'http://www.dlforgelab.com:8084/api/docs-assistant/mcp',
          headers: {
            Authorization: 'Bearer fdmcp_v1.demo-token',
          },
        },
      },
    }))
    expect(entries).toEqual([{
      connectionCode: 'forge-docs',
      connectionName: 'forge-docs',
      endpoint: 'http://www.dlforgelab.com:8084/api/docs-assistant/mcp',
      token: 'fdmcp_v1.demo-token',
      timeoutSeconds: 15,
      enabled: 1,
    }])
    expect(mcpOriginOf(entries[0].endpoint)).toBe('http://www.dlforgelab.com:8084')
  })

  it('rejects empty or non-http configs', () => {
    expect(() => parseMcpServersConfig('')).toThrow(/粘贴/)
    expect(() => parseMcpServersConfig('{')).toThrow(/JSON/)
    expect(() => parseMcpServersConfig(JSON.stringify({
      mcpServers: { local: { command: 'npx', args: ['-y', 'demo'] } },
    }))).toThrow(/没有可导入/)
  })
})
