import { describe, expect, it } from 'vitest'
import {
  DEFAULT_OFFICIAL_PORTAL_BASE,
  resolveOfficialMarketUrl,
  resolveOfficialPortalBase,
  safeExternalHttpLink,
} from '../officialMarket'

describe('official market links', () => {
  it('only allows credential-free http(s) absolute urls', () => {
    expect(safeExternalHttpLink('https://example.com/templates')).toBe('https://example.com/templates')
    for (const value of ['javascript:alert(1)', 'data:text/html,test', '//example.com', '/templates', 'https://name:secret@example.com/t', undefined])
      expect(safeExternalHttpLink(value)).toBe('')
  })

  it('derives portal base from plugin market or templates urls', () => {
    expect(resolveOfficialPortalBase('')).toBe(DEFAULT_OFFICIAL_PORTAL_BASE)
    expect(resolveOfficialPortalBase('http://www.dlforgelab.com:8084/forge-official-ui/plugins'))
      .toBe(DEFAULT_OFFICIAL_PORTAL_BASE)
    expect(resolveOfficialPortalBase('https://customer.example/portal/plugins'))
      .toBe('https://customer.example/portal')
  })

  it('builds kind-specific template pages instead of the plugin catalog', () => {
    expect(resolveOfficialMarketUrl('print')).toBe(`${DEFAULT_OFFICIAL_PORTAL_BASE}/templates?kind=print`)
    expect(resolveOfficialMarketUrl('form')).toBe(`${DEFAULT_OFFICIAL_PORTAL_BASE}/templates?kind=form`)
    expect(resolveOfficialMarketUrl('app')).toBe(`${DEFAULT_OFFICIAL_PORTAL_BASE}/templates?kind=app`)
    expect(resolveOfficialMarketUrl('plugin')).toBe(`${DEFAULT_OFFICIAL_PORTAL_BASE}/plugins`)
    expect(resolveOfficialMarketUrl('print', { explicit: 'https://cdn.example/print.zip' }))
      .toBe('https://cdn.example/print.zip')
    expect(resolveOfficialMarketUrl('app', { q: '采购' }))
      .toBe(`${DEFAULT_OFFICIAL_PORTAL_BASE}/templates?kind=app&q=%E9%87%87%E8%B4%AD`)
    expect(resolveOfficialMarketUrl('print-component'))
      .toBe(`${DEFAULT_OFFICIAL_PORTAL_BASE}/templates?kind=print-component`)
  })
})
