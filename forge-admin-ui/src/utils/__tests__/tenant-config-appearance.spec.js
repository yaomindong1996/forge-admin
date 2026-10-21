import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defaultThemeConfig } from '@/config/theme.config'
import { useAppStore } from '@/store/modules/app'
import { applyTenantConfig } from '@/utils/tenant-config'

vi.mock('@/utils/file', () => ({ resolveRenderableFileUrl: vi.fn() }))

describe('登录与恢复外观使用一致的租户配置', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it.each([
    { systemTheme: 'light', themeConfig: '{"primaryColor":"#0f766e"}' },
    { systemTheme: '#b91c1c', themeConfig: { primaryColor: '#4242F7', navigationMode: 'auto' } },
    { systemTheme: 'dark', themeConfig: 'null' },
    { systemTheme: '#2f6fed', themeConfig: '[]' },
    { systemTheme: '#334155', themeConfig: 'invalid' },
    { themeConfig: { headerDark: { backgroundColor: '#061917' }, extension: { density: 'compact' } } },
  ])('初始化与恢复解析一致：%j', async (appearance) => {
    const config = { ...appearance, systemLayout: 'simple', systemName: '测试工作台' }
    const original = JSON.stringify(config)
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    try {
      const store = useAppStore()
      store.isDark = true
      await applyTenantConfig(config, store)
      const initialized = JSON.stringify(store.themeConfig)
      expect(store.layout).toBe('simple')
      expect(store.isDark).toBe(true)
      expect(document.title).toBe('测试工作台')
      store.setThemeConfig({ primaryColor: '#111111' })
      store.restoreTenantAppearance(config)
      expect(JSON.stringify(store.themeConfig)).toBe(initialized)
      expect(JSON.stringify(config)).toBe(original)
    }
    finally { warn.mockRestore() }
  })

  it('历史主题模式不覆盖 JSON 主色，未知字段和深色配置保留', async () => {
    const store = useAppStore()
    await applyTenantConfig({
      systemTheme: 'light',
      themeConfig: JSON.stringify({
        primaryColor: '#0f766e',
        headerDark: { backgroundColor: '#061917' },
        extension: { density: 'compact' },
      }),
    }, store)
    expect(store.primaryColor).toBe('#0f766e')
    expect(store.themeConfig.navigationMode).toBe('custom')
    expect(store.themeConfig.headerDark.backgroundColor).toBe('#061917')
    expect(store.themeConfig.extension).toEqual({ density: 'compact' })
  })

  it('空配置清除上一账号的外观残留', async () => {
    const store = useAppStore()
    store.setThemeConfig({ primaryColor: '#0f766e', extension: { density: 'compact' } })
    await applyTenantConfig(null, store)
    expect(store.themeConfig).toEqual(defaultThemeConfig)
    expect(store.themeConfig).not.toHaveProperty('extension')
  })

  it('应用有效租户配置时不先重置为默认色，避免登录闪色', async () => {
    const store = useAppStore()
    await applyTenantConfig({
      systemTheme: '#0f766e',
      themeConfig: { primaryColor: '#0f766e' },
    }, store)
    expect(store.primaryColor).toBe('#0f766e')
    const resetSpy = vi.spyOn(store, 'resetAccountState')
    const seen = []
    const stop = store.$subscribe((_mutation, state) => {
      seen.push(state.primaryColor)
    })
    try {
      await applyTenantConfig({
        systemTheme: '#0f766e',
        themeConfig: { primaryColor: '#0f766e' },
      }, store)
      expect(resetSpy).not.toHaveBeenCalled()
      expect(seen.every(color => color === '#0f766e')).toBe(true)
      expect(store.primaryColor).toBe('#0f766e')
    }
    finally {
      stop()
    }
  })

  it('preserveAppearance 仅清理会话 UI 状态，不改主色', () => {
    const store = useAppStore()
    store.setThemeConfig({ primaryColor: '#0f766e' })
    store.appearanceOpen = true
    store.selectedTopMenuId = 'menu_1'
    store.resetAccountState({ preserveAppearance: true })
    expect(store.primaryColor).toBe('#0f766e')
    expect(store.appearanceOpen).toBe(false)
    expect(store.selectedTopMenuId).toBeNull()
  })

  it('大于旧列容量的完整主题与中文扩展字段不被截断，加载和恢复保持一致', async () => {
    const store = useAppStore()
    const theme = { ...defaultThemeConfig, extension: { description: '主题配置'.repeat(600) } }
    const themeConfig = JSON.stringify(theme)
    expect(themeConfig.length).toBeGreaterThan(1000)
    const tenant = { systemLayout: 'immersive', themeConfig }
    await applyTenantConfig(tenant, store)
    expect(store.themeConfig.extension).toEqual(theme.extension)
    const initialized = JSON.stringify(store.themeConfig)
    store.restoreTenantAppearance(tenant)
    expect(JSON.stringify(store.themeConfig)).toBe(initialized)
    expect(tenant.themeConfig).toBe(themeConfig)
  })
})
