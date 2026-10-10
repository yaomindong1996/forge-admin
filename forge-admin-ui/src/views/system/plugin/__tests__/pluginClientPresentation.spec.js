import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { pluginLicenseView } from '../pluginLicenseView'
import { resolvePluginMarketUrl, safePluginLink } from '../pluginLinks'
import { builtinPresentations, pluginPresentation } from '../pluginPresentation'

describe('客户端插件展示边界', () => {
  it('内置插件有独立插画和说明，外部同名不冒用，未知记录不伪造内容', () => {
    const entries = Object.entries(builtinPresentations)
    expect(entries.length).toBe(15)
    expect(new Set(entries.map(([, item]) => item.artwork)).size).toBe(entries.length)
    for (const [id, item] of entries) {
      expect(pluginPresentation({ id, origin: 'builtin' })).toBe(item)
      expect(item.summary.length).toBeGreaterThan(5)
      expect(item.highlights).toHaveLength(3)
    }
    expect(pluginPresentation({ id: 'plugin-system', origin: 'external' }).artwork).toBe(15)
    expect(pluginPresentation({ id: '__proto__', origin: 'builtin' }).highlights).toEqual([])
    expect(pluginPresentation(null).introduction).toContain('尚未提供')
  })

  it('外站入口只允许无凭证 HTTP(S)，不接受脚本或相对跳转', () => {
    expect(safePluginLink('https://example.com/plugins')).toBe('https://example.com/plugins')
    for (const value of ['javascript:alert(1)', 'data:text/html,test', '//example.com', '/plugins', 'https://name:secret@example.com/plugins', undefined]) {
      expect(safePluginLink(value)).toBe('')
    }
  })

  it('开发和生产使用同一正式市场缺省值，允许合法覆盖且不掩盖错误配置', () => {
    for (const value of [undefined, null, '', '  ']) {
      expect(resolvePluginMarketUrl(value))
        .toBe('http://www.dlforgelab.com:8084/forge-official-ui/plugins')
    }
    expect(resolvePluginMarketUrl(' https://customer.example/plugins '))
      .toBe('https://customer.example/plugins')
    expect(resolvePluginMarketUrl('http://localhost:5174/plugins')).toBe('http://localhost:5174/plugins')
    expect(resolvePluginMarketUrl('javascript:alert(1)')).toBe('')
    expect(resolvePluginMarketUrl('https://name:secret@example.com/plugins')).toBe('')
  })

  it('授权严格匹配插件 ID，只展示当前插件声明内的功能', () => {
    const plugin = { id: 'sample', features: [{ code: 'sample.read' }] }
    const entries = [
      { position: 1, state: 'valid', scope: { pluginIds: ['sample-other'], featureCodes: ['other.read'] } },
      { position: 2, state: 'expired', scope: { pluginIds: ['sample'], featureCodes: ['sample.read', 'other.read'] } },
      { position: 3, state: 'invalid', scope: null },
    ]
    const view = pluginLicenseView({ report: { entries } }, plugin)
    expect(view.entries).toEqual([{ position: 2, state: 'expired', terms: undefined, features: ['sample.read'] }])
    expect(view.hasUnattributedIssue).toBe(true)
    expect(pluginLicenseView({ mode: 'community' }, plugin).entries).toEqual([])
    expect(pluginLicenseView({ report: { entries } }, null).entries).toEqual([])
  })

  it('客户端页面不引入管理工作台，仍复用统一主从组件', () => {
    const relativePath = '../../plugin.vue'
    const page = readFileSync(new URL(relativePath, import.meta.url), 'utf8')
    for (const component of ['PluginWorkbench', 'PluginBuildComparison', 'RuntimeLicenseStatus', 'PluginDeliveryWorkbench']) {
      expect(page).not.toContain(component)
    }
    expect(page).toContain('MasterDetailWorkspace')
    expect(page).toContain('PluginDetail')
  })
})
