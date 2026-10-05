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
})
