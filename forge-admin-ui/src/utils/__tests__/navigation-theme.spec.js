import { describe, expect, it } from 'vitest'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import { filterNavigationMenus } from '@/layouts/immersive/menu-search'
import { canConfigureLayout } from '@/layouts/layout-settings-visibility'
import {
  contrastRatio,
  createNavigationTheme,
  navigationPresets,
  resolveNavigationMode,
  resolveNavigationTheme,
  updateNavigationBases,
  updateNavigationMode,
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

  it('雾色方案顶栏和侧栏同色，形成浅罩三色条', () => {
    const washes = navigationPresets.filter(item => item.key.startsWith('mist'))
    expect(washes.map(item => item.name)).toEqual(['雾蓝', '雾青', '雾紫'])
    for (const preset of washes) {
      expect(preset.header).toBe(preset.side)
      expect(preset.header.toLowerCase()).not.toBe(preset.primary.toLowerCase())
    }
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

describe('顶部菜单选中横条', () => {
  it.each([false, true])('模式 %s 未设置时跟随品牌主色，与可读文字色独立', (dark) => {
    const source = updateNavigationBases(defaultThemeConfig, { primary: '#2F6FED', header: '#2F6FED' }, dark)
    const rendered = resolveNavigationTheme(source, defaultThemeConfig, dark)
    expect(rendered.topMenu.textColorActive).not.toBe(rendered.primaryColor)
    applyThemeConfig(source, dark)
    expect(document.documentElement.style.getPropertyValue('--top-menu-active-bar-color')).toBe('#2f6fed')
    applyThemeConfig({ ...source, primaryColor: '#B91C1C' }, dark)
    expect(document.documentElement.style.getPropertyValue('--top-menu-active-bar-color')).toBe('#b91c1c')
  })

  it.each(['auto', 'custom'])('%s 配色保留独立浅/深横条，基础色与预设更新不覆盖它', (mode) => {
    const source = {
      ...defaultThemeConfig,
      navigationMode: mode,
      topMenu: { ...defaultThemeConfig.topMenu, activeBarColor: '#FF7D00' },
      topMenuDark: { ...defaultThemeConfig.topMenuDark, activeBarColor: '#7DB7FF' },
    }
    const original = JSON.stringify(source)
    const next = updateNavigationBases(source, navigationPresets[1])
    expect(next.topMenu.activeBarColor).toBe('#FF7D00')
    expect(next.topMenuDark.activeBarColor).toBe('#7DB7FF')
    applyThemeConfig(next)
    expect(document.documentElement.style.getPropertyValue('--top-menu-active-bar-color')).toBe('#ff7d00')
    applyThemeConfig(next, true)
    expect(document.documentElement.style.getPropertyValue('--top-menu-active-bar-color')).toBe('#7db7ff')
    expect(JSON.stringify(source)).toBe(original)
  })

  it.each([undefined, null, '', 'invalid', 'rgb(999,0,0)'])('非法或空横条 %s 安全回退品牌色', (value) => {
    applyThemeConfig({ primaryColor: '#0E8F7E', topMenu: { activeBarColor: value } })
    expect(document.documentElement.style.getPropertyValue('--top-menu-active-bar-color')).toBe('#0e8f7e')
  })

  it('租户保存/重新编辑保留两套横条与未知字段', () => {
    const data = { themeConfig: JSON.stringify({
      primaryColor: '#2f6fed',
      navigationMode: 'auto',
      topMenu: { activeBarColor: '#FF7D00' },
      topMenuDark: { activeBarColor: '#7DB7FF' },
      extension: { version: 3 },
    }) }
    prepareTenantAppearance(data)
    const loaded = readTenantAppearance(data)
    expect(loaded.topMenu.activeBarColor).toBe('#FF7D00')
    expect(loaded.topMenuDark.activeBarColor).toBe('#7DB7FF')
    expect(loaded.extension).toEqual({ version: 3 })
  })
})

describe('深浅导航模式隔离', () => {
  it.each(['auto', 'custom'])('旧全局 %s 同时适用于浅色和深色', (mode) => {
    expect(resolveNavigationMode({ navigationMode: mode })).toBe(mode)
    expect(resolveNavigationMode({ navigationMode: mode }, true)).toBe(mode)
    expect(resolveNavigationMode({ navigationMode: mode, navigationModeDark: 'invalid' }, true)).toBe(mode)
  })

  it.each([false, true])('修改模式 %s 基础色不改变另一套手动渲染效果', (dark) => {
    const source = {
      ...defaultThemeConfig,
      navigationMode: 'custom',
      header: { ...defaultThemeConfig.header, textColor: '#334155' },
      headerDark: { ...defaultThemeConfig.headerDark, textColor: '#b8cde0' },
      extension: { density: 'compact' },
    }
    const original = JSON.stringify(source)
    const other = resolveNavigationTheme(source, defaultThemeConfig, !dark)
    const next = updateNavigationBases(source, { header: '#061917', side: '#08201d' }, dark)
    expect(resolveNavigationMode(next, dark)).toBe('auto')
    expect(resolveNavigationMode(next, !dark)).toBe('custom')
    const rendered = resolveNavigationTheme(next, defaultThemeConfig, !dark)
    for (const group of ['header', 'topMenu', 'sideMenu']) {
      expect(rendered[group]).toEqual(other[group])
    }
    expect(next.extension).toEqual(source.extension)
    expect(JSON.stringify(source)).toBe(original)
  })

  it.each([false, true])('手动开关 %s 不改变另一套自动配色', (dark) => {
    const source = { ...defaultThemeConfig }
    const next = updateNavigationMode(source, 'custom', dark)
    expect(resolveNavigationMode(next, dark)).toBe('custom')
    expect(resolveNavigationMode(next, !dark)).toBe('auto')
    expect(source).not.toHaveProperty('navigationModeDark')
  })

  it('租户保存、解析和重新编辑保留独立模式及扩展字段', () => {
    const data = { themeConfig: JSON.stringify({
      navigationMode: 'custom',
      navigationModeDark: 'auto',
      header: { textColor: '#334155' },
      extension: { version: 2 },
    }) }
    prepareTenantAppearance(data)
    const loaded = readTenantAppearance(data)
    expect(loaded.navigationMode).toBe('custom')
    expect(loaded.navigationModeDark).toBe('auto')
    expect(loaded.header.textColor).toBe('#334155')
    expect(loaded.extension).toEqual({ version: 2 })
    const next = updateNavigationMode(loaded, 'custom', true)
    expect(next.navigationMode).toBe('custom')
    expect(next.navigationModeDark).toBe('custom')
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
