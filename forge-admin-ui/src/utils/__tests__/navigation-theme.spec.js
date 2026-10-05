import { describe, expect, it } from 'vitest'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import { filterNavigationMenus } from '@/layouts/immersive/menu-search'
import { canConfigureLayout } from '@/layouts/layout-settings-visibility'
import {
  contrastRatio,
  createNavigationTheme,
  navigationPresets,
  resolveNavigationTheme,
  updateNavigationBases,
} from '@/utils/navigation-theme'
import { prepareTenantAppearance, readTenantAppearance } from '@/views/system/components/tenant/tenant-appearance'

describe('统一导航主题', () => {
  it.each(navigationPresets)('$name 自动生成可读顶栏和导航状态', (preset) => {
    const config = updateNavigationBases(defaultThemeConfig, preset)
    const theme = resolveNavigationTheme(config, defaultThemeConfig)
    expect(theme.header.backgroundColor.toLowerCase()).toBe(preset.header.toLowerCase())
    expect(contrastRatio(theme.header.textColor, theme.header.backgroundColor)).toBeGreaterThanOrEqual(4.5)
    expect(contrastRatio(theme.sideMenu.textColor, theme.sideMenu.backgroundColor)).toBeGreaterThanOrEqual(4.5)
    const activeContrast = contrastRatio(theme.sideMenu.textColorActive, theme.sideMenu.backgroundColorActive)
    expect(activeContrast).toBeGreaterThanOrEqual(4.5)
    expect(theme.sideMenu.iconColorActive).toBe(theme.sideMenu.textColorActive)
  })

  it('低对比历史文字只在运行时保护，不修改原始 JSON', () => {
    const old = {
      primaryColor: '#2f6fed',
      header: { backgroundColor: '#fff', textColor: '#fff' },
      topMenu: { textColor: 'rgba(255,255,255,0.7)' },
      sideMenu: { backgroundColor: '#111', textColor: '#111' },
    }
    const theme = resolveNavigationTheme(old, defaultThemeConfig)
    expect(theme.header.textColor).toBe('#1D2129')
    expect(theme.topMenu.textColor).toBe('#1D2129')
    expect(theme.sideMenu.textColor).toBe('#FFFFFF')
    expect(old.header.textColor).toBe('#fff')
  })

  it('已可读的自定义颜色保留，并保护菜单独立选中表面', () => {
    const theme = resolveNavigationTheme({
      header: { backgroundColor: '#fff', textColor: '#334155' },
      topMenu: { textColorActive: '#111111', backgroundColorActive: '#111111' },
    }, defaultThemeConfig)
    expect(theme.header.textColor).toBe('#334155')
    expect(theme.topMenu.textColorActive).toBe('#FFFFFF')
  })

  it('非法主色不触发调色板错误，暗色有独立表面', () => {
    const theme = resolveNavigationTheme({ ...defaultThemeConfig, primaryColor: 'light' }, defaultThemeConfig, true)
    expect(theme.primaryColor).toBe(defaultThemeConfig.primaryColor.toLowerCase())
    expect(theme.header.backgroundColor).toBe('#18181c')
    expect(contrastRatio(theme.header.textColor, theme.header.backgroundColor)).toBeGreaterThanOrEqual(4.5)
  })

  it('深色基础色只写深色分组，保留浅色、尺寸与未知扩展', () => {
    const source = { ...defaultThemeConfig, extension: { density: 'compact' } }
    const original = JSON.stringify(source)
    const next = updateNavigationBases(source, { header: '#061917', side: '#08201d' }, true)
    expect(next.navigationMode).toBe('auto')
    expect(next.header).toEqual(source.header)
    expect(next.topMenu).toEqual(source.topMenu)
    expect(next.sideMenu).toEqual(source.sideMenu)
    expect(next.headerDark.backgroundColor).toBe('#061917')
    expect(next.sideMenuDark.backgroundColor).toBe('#08201d')
    expect(next.sideMenuDark.width).toBe(source.sideMenuDark.width)
    expect(next.extension).toEqual(source.extension)
    const rendered = resolveNavigationTheme(next, defaultThemeConfig, true)
    expect(contrastRatio(rendered.header.textColor, rendered.header.backgroundColor)).toBeGreaterThanOrEqual(4.5)
    expect(JSON.stringify(source)).toBe(original)
  })

  it('cSS 变量同时覆盖 Header、菜单、工具及 Logo 文字', () => {
    const config = createNavigationTheme({ header: '#171717', side: '#eef4ff' })
    applyThemeConfig(config)
    const style = document.documentElement.style
    expect(style.getPropertyValue('--layout-header-bg-color')).toBe('#171717')
    expect(style.getPropertyValue('--layout-header-text-color')).toBe('#FFFFFF')
    expect(style.getPropertyValue('--brand-title-text-color')).toBe('#FFFFFF')
    expect(style.getPropertyValue('--side-menu-bg-color')).toBe('#eef4ff')
    expect(style.getPropertyValue('--top-menu-text-color')).toBe('#FFFFFF')
  })
})

describe('租户外观保存兼容', () => {
  it('保留暗色主题、尺寸和未知扩展字段，仅更新基础色相关字段', () => {
    const source = {
      primaryColor: '#123456',
      customVersion: 7,
      headerDark: { backgroundColor: '#030303' },
      sideMenu: { width: '248px' },
      extension: { density: 'compact' },
    }
    const data = { systemTheme: '#2f6fed', themeConfig: JSON.stringify(source), themePreset: 'old' }
    const theme = updateNavigationBases(readTenantAppearance(data), { header: '#f0f0f0' })
    data.themeConfig = JSON.stringify(theme)
    prepareTenantAppearance(data)
    const saved = JSON.parse(data.themeConfig)
    expect(saved.navigationMode).toBe('auto')
    expect(saved.customVersion).toBe(7)
    expect(saved.headerDark.backgroundColor).toBe('#030303')
    expect(saved.sideMenu.width).toBe('248px')
    expect(saved.extension).toEqual(source.extension)
    expect(data.systemTheme).toBe('#2f6fed')
    expect(data).not.toHaveProperty('themePreset')
  })
  it('未调整的旧主题按手动模式回显，主色优先来自租户 systemTheme', () => {
    const theme = readTenantAppearance({ systemTheme: '#b91c1c', themeConfig: '{"primaryColor":"#4242F7"}' })
    expect(theme.navigationMode).toBe('custom')
    expect(theme.primaryColor).toBe('#b91c1c')
    expect(readTenantAppearance().navigationMode).toBe('auto')
  })
})

describe('布局恢复和沉浸式菜单', () => {
  it('用户选择空白仍能恢复，路由强制空白 / 门户和未登录隐藏入口', () => {
    expect(canConfigureLayout({ authenticated: true })).toBe(true)
    expect(canConfigureLayout({ authenticated: true, routeLayout: 'empty' })).toBe(false)
    expect(canConfigureLayout({ authenticated: true, routeLayout: 'app-portal' })).toBe(false)
    expect(canConfigureLayout({ authenticated: false })).toBe(false)
  })
  const tree = [{ key: 'system', label: '系统管理', children: [
    { key: 'users', label: '用户管理', path: '/system/user' },
    { key: 'roles', label: '角色管理', path: '/system/role' },
  ] }]
  it('搜索命中父目录保留完整子树，点击不变成无路由的死入口', () => {
    expect(filterNavigationMenus(tree, '系统')).toEqual(tree)
  })
  it('搜索叶子保留路径和祖先，不改原树', () => {
    const result = filterNavigationMenus(tree, ' 用户 ')
    expect(result[0].children).toEqual([tree[0].children[0]])
    expect(tree[0].children).toHaveLength(2)
    expect(filterNavigationMenus(tree, '没有')).toEqual([])
  })
})
