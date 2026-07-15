import { describe, expect, it } from 'vitest'
import { processScopedCss } from '../scoped-css'

describe('scoped extension css', () => {
  const scope = {
    applicationCode: 'crm_center',
    pageCode: 'customer_form',
  }

  it.each([
    '@import url("https://example.com/theme.css");',
    '.card { background: url(https://example.com/a.png); }',
    'html, body { margin: 0; }',
    ':root { --primary: red; }',
    '.forge-layout .sidebar { display: none; }',
    '#app .layout-sider { display: none; }',
    '.dialog-mask { position: fixed; inset: 0; }',
    '.floating { z-index: 999999; }',
  ])('rejects unsafe css: %s', (source) => {
    expect(() => processScopedCss(source, scope)).toThrow()
  })

  it('prefixes every normal selector with the application and page root', () => {
    const result = processScopedCss('.customer-card:hover, .amount-cell strong { color: #1677ff; }', scope)

    expect(result.scopeSelector).toBe('[data-forge-app="crm_center"][data-forge-page="customer_form"]')
    expect(result.css).toContain(`${result.scopeSelector} .customer-card:hover`)
    expect(result.css).toContain(`${result.scopeSelector} .amount-cell strong`)
  })

  it('scopes selectors nested in media rules without escaping through pseudo classes', () => {
    const result = processScopedCss('@media (min-width: 900px) { .panel:not(.compact) > .row { gap: 12px; } }', scope)

    expect(result.css).toContain(`${result.scopeSelector} .panel:not(.compact)>.row`)
    expect(result.selectorCount).toBe(1)
  })
})
