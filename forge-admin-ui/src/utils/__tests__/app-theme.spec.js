import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { defaultThemeConfig } from '@/config/theme.config'
import { useAppStore } from '@/store/modules/app'
import { solidColor } from '@/utils/navigation-theme'

describe('外观会话状态', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('租户历史非法主色不会打断主题加载', () => {
    const store = useAppStore()
    expect(() => store.setThemeConfig({ primaryColor: 'light' })).not.toThrow()
    expect(store.primaryColor).toBe(defaultThemeConfig.primaryColor.toLowerCase())
    expect(store.themeConfig.primaryColor).toBe(store.primaryColor)
  })

  it('账号重置关闭跨布局设置面板，恢复默认而非残留上一租户主题', () => {
    const store = useAppStore()
    store.appearanceOpen = true
    store.setThemeConfig({ primaryColor: '#b91c1c' })
    store.resetAccountState()
    expect(store.appearanceOpen).toBe(false)
    expect(store.themeConfig).toEqual(defaultThemeConfig)
  })

  it('兼容旧租户 8 位和短写透明色主色，传调色板前规范为实色', () => {
    expect(solidColor('#4266F7FF')).toBe('#4266f7')
    expect(solidColor('#fff8')).toBe('#ffffff')
  })

  it('恢复租户外观同时恢复布局，保留原配置及身份相关会话状态', () => {
    const store = useAppStore()
    const config = {
      systemLayout: 'immersive',
      systemTheme: '#0f766e',
      themeConfig: JSON.stringify({
        headerDark: { backgroundColor: '#061917' },
        extension: { density: 'compact' },
      }),
    }
    const original = JSON.stringify(config)
    store.appearanceOpen = true
    store.selectedTopMenuId = 'business'
    store.isDark = true
    store.restoreTenantAppearance(config)
    expect(store.layout).toBe('immersive')
    expect(store.primaryColor).toBe('#0f766e')
    expect(store.themeConfig.headerDark.backgroundColor).toBe('#061917')
    expect(store.themeConfig.extension).toEqual({ density: 'compact' })
    expect(store.selectedTopMenuId).toBe('business')
    expect(store.appearanceOpen).toBe(true)
    expect(store.isDark).toBe(true)
    expect(JSON.stringify(config)).toBe(original)
  })

  it('恢复系统配色只清除外观覆盖，不改布局或深浅模式', () => {
    const store = useAppStore()
    store.layout = 'nexus'
    store.isDark = true
    store.setThemeConfig({ primaryColor: '#0f766e', extension: { density: 'compact' } })
    store.restoreSystemTheme()
    expect(store.themeConfig).toEqual({
      ...defaultThemeConfig,
      primaryColor: defaultThemeConfig.primaryColor.toLowerCase(),
    })
    expect(store.layout).toBe('nexus')
    expect(store.isDark).toBe(true)
    expect(store.themeConfig).not.toHaveProperty('extension')
  })

  it('无租户配置时可安全恢复系统基线', () => {
    const store = useAppStore()
    expect(() => store.restoreTenantAppearance(null)).not.toThrow()
    expect(store.primaryColor).toBe(defaultThemeConfig.primaryColor.toLowerCase())
  })

  it('非法 RGB 数字或透明度降级，不生成 NaN CSS 颜色', () => {
    for (const color of ['rgb(.,0,0)', 'rgb(999,0,0)', 'rgba(0,0,0,2)']) {
      expect(solidColor(color, '#2f6fed')).toBe('#2f6fed')
    }
    expect(solidColor('invalid', 'invalid')).toBe('#ffffff')
  })
})
