import { describe, expect, it } from 'vitest'
import { resolveOwnedMenuSsoBaseUrl } from '@/composables/useMenu'
import {
  normalizeHttpSsoBaseUrl,
  REPORT_HOST_FALLBACK,
  selectTrustedSsoBaseUrl,
} from '@/utils/sso-target'

const REPORT_BASE = 'https://reports.example/forge-report'

describe('sso 跳转地址白名单', () => {
  it('拒绝 javascript、协议相对地址和带账号的 URL', () => {
    expect(normalizeHttpSsoBaseUrl('javascript:alert(1)')).toBe('')
    expect(normalizeHttpSsoBaseUrl('//evil.example/steal')).toBe('')
    expect(normalizeHttpSsoBaseUrl('https://user:pass@evil.example/steal')).toBe('')
    expect(normalizeHttpSsoBaseUrl(REPORT_BASE)).toBe(REPORT_BASE)
  })

  it('地址栏里的任意 baseUrl 不能成为出票目标', () => {
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: 'https://evil.example/steal',
      allowedBaseUrls: [REPORT_BASE],
    })).toBe('')
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: 'https://evil.example/forge-report',
      allowedBaseUrls: [REPORT_BASE],
    })).toBe('')
  })

  it('允许已配置入口本身及其同站下级路径', () => {
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: `${REPORT_BASE}/screen`,
      allowedBaseUrls: [REPORT_BASE],
    })).toBe(`${REPORT_BASE}/screen`)
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: '',
      allowedBaseUrls: [REPORT_BASE],
    })).toBe(REPORT_BASE)
  })

  it('未传 baseUrl 时回退到菜单或客户端入口，伪造值则直接拒绝', () => {
    const menuBase = 'https://screen.example/app'
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: menuBase,
      allowedBaseUrls: [menuBase, REPORT_BASE],
    })).toBe(menuBase)
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: 'https://evil.example',
      allowedBaseUrls: [menuBase, REPORT_BASE],
    })).toBe('')
  })

  it('只把报表宿主兜底域名视为额外可信主机', () => {
    if (!REPORT_HOST_FALLBACK)
      return
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: `http://${REPORT_HOST_FALLBACK}/custom`,
      allowedBaseUrls: [REPORT_BASE],
    })).toBe(`http://${REPORT_HOST_FALLBACK}/custom`)
    expect(selectTrustedSsoBaseUrl({
      requestedBaseUrl: `http://${REPORT_HOST_FALLBACK}.evil.example/custom`,
      allowedBaseUrls: [REPORT_BASE],
    })).toBe('')
  })

  it('未知菜单不能提供入口地址', () => {
    expect(resolveOwnedMenuSsoBaseUrl([], 'missing', 'forge_report')).toBe('')
  })
})
