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
})
