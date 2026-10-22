import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ensureTenantBranding,
  getDefaultFaviconUrl,
  isBrowserSafeFaviconUrl,
  resetTenantBrandingStateForTests,
  resolveTenantFaviconUrl,
  setDocumentFavicon,
} from '@/utils/tenant-config'

describe('租户浏览器图标（favicon）', () => {
  beforeEach(() => {
    resetTenantBrandingStateForTests()
    document.head.querySelectorAll('link[rel="icon"], link[rel="shortcut icon"], link[rel="apple-touch-icon"]')
      .forEach(link => link.remove())
  })

  it('rejects blob / authenticated file urls that browsers cannot load as tab icons', () => {
    expect(isBrowserSafeFaviconUrl('blob:https://local/abc')).toBe(false)
    expect(isBrowserSafeFaviconUrl('/api/file/download/file_12345678')).toBe(false)
    expect(isBrowserSafeFaviconUrl('file_12345678abcd')).toBe(false)
    expect(isBrowserSafeFaviconUrl('/auth/tenant/assets/1/icon?v=file_12345678')).toBe(true)
    expect(isBrowserSafeFaviconUrl('/favicon-32.png')).toBe(true)
    expect(isBrowserSafeFaviconUrl('https://cdn.example.com/a.png')).toBe(true)
  })

  it('resolves managed file icons via public tenant asset endpoint', () => {
    const url = resolveTenantFaviconUrl({
      id: 1,
      browserIcon: 'file_abc12345',
    })
    expect(url).toContain('/auth/tenant/assets/1/icon')
    expect(url).toContain('v=file_abc12345')
  })

  it('falls back to empty when only a raw file id is present without tenant id', () => {
    expect(resolveTenantFaviconUrl({ browserIcon: 'file_abc12345' })).toBe('')
  })

  it('setDocumentFavicon falls back to default when given unsafe url', () => {
    setDocumentFavicon('blob:https://local/broken')
    const icon = document.querySelector('link[rel="icon"]')
    expect(icon?.getAttribute('href')).toBe(getDefaultFaviconUrl())
    expect(document.querySelector('link[rel="apple-touch-icon"]')).toBeTruthy()
  })

  it('setDocumentFavicon keeps apple-touch-icon when replacing favicon', () => {
    const apple = document.createElement('link')
    apple.rel = 'apple-touch-icon'
    apple.href = '/apple-touch-icon.png'
    document.head.appendChild(apple)

    setDocumentFavicon('/auth/tenant/assets/1/icon?v=x')
    expect(document.querySelectorAll('link[rel="apple-touch-icon"]').length).toBe(1)
    expect(document.querySelector('link[rel="icon"]')?.getAttribute('href')).toContain('/auth/tenant/assets/1/icon')
  })

  it('ensureTenantBranding loads missing config then sets a browser-safe favicon', async () => {
    const loadTenantConfig = vi.fn(async () => ({
      id: 1,
      browserIcon: 'file_abc12345',
      systemName: '演示租户',
    }))
    const tenantStore = { config: null, loadTenantConfig }
    const appStore = {
      restoreTenantAppearance: vi.fn(),
      resetAccountState: vi.fn(),
    }

    await ensureTenantBranding(appStore, { tenantStore, tenantId: 1 })

    expect(loadTenantConfig).toHaveBeenCalledWith(1)
    expect(document.querySelector('link[rel="icon"]')?.getAttribute('href')).toContain('/auth/tenant/assets/1/icon')
  })

  it('ensureTenantBranding faviconOnly only updates the icon link', async () => {
    const tenantStore = {
      config: {
        id: 1,
        browserIcon: 'file_abc12345',
        systemName: '演示租户',
      },
      loadTenantConfig: vi.fn(),
    }
    const appStore = {
      restoreTenantAppearance: vi.fn(),
      resetAccountState: vi.fn(),
    }

    await ensureTenantBranding(appStore, { tenantStore, faviconOnly: true })

    expect(tenantStore.loadTenantConfig).not.toHaveBeenCalled()
    expect(appStore.restoreTenantAppearance).not.toHaveBeenCalled()
    expect(document.querySelector('link[rel="icon"]')?.getAttribute('href')).toContain('/auth/tenant/assets/1/icon')
  })

  it('ensureTenantBranding skips full theme reapply for the same config fingerprint', async () => {
    const tenantStore = {
      config: {
        id: 1,
        browserIcon: 'file_abc12345',
        systemName: '演示租户',
      },
      loadTenantConfig: vi.fn(),
    }
    const appStore = {
      restoreTenantAppearance: vi.fn(),
      resetAccountState: vi.fn(),
    }

    await ensureTenantBranding(appStore, { tenantStore })
    await ensureTenantBranding(appStore, { tenantStore })

    expect(appStore.restoreTenantAppearance).toHaveBeenCalledTimes(1)
    expect(document.querySelector('link[rel="icon"]')?.getAttribute('href')).toContain('/auth/tenant/assets/1/icon')
  })
})
