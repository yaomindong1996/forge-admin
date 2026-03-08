/**********************************
 * @FilePath: theme.config.js
 * @Description: 主题配置文件 - 统一管理所有布局的样式配置
 **********************************/

/**
 * 默认主题配置
 */
export const defaultThemeConfig = {
  // 主题色
  primaryColor: '#d12723FF',

  // Header 配置
  header: {
    backgroundColor: '#d12723FF',
    textColor: '#FFFFFF',
    fontSize: 'var(--font-size-base)',
    height: '60px',
    borderColor: '#FFFFFF',
  },

  // 暗色模式 Header 配置
  headerDark: {
    backgroundColor: '#18181c',
    textColor: '#e5e7eb',
    fontSize: 'var(--font-size-base)',
    height: '60px',
    borderColor: '#2d2d30',
  },

  // 顶部菜单配置
  topMenu: {
    textColor: '#FFFFFF',
    textColorHover: '#FFFFFF',
    textColorActive: '#FFFFFF',
    textColorActiveHover: '#FFFFFF',
    textColorActiveHorizontal: '#333333',
    backgroundColor: 'transparent',
    backgroundColorHover: 'rgba(255, 255, 255, 0.1)',
    backgroundColorActive: '#ffffff',
    backgroundColorActiveHover: 'rgba(255, 255, 255, 0.15)',
    fontSize: 'var(--font-size-base)',
    fontWeight: '500',
    iconColor: '#ffffff',
    iconActiveColor: '#333333',
  },

  // 暗色模式顶部菜单配置
  topMenuDark: {
    textColor: '#e5e7eb',
    textColorHover: '#5388ff',
    textColorActive: '#5388ff',
    textColorActiveHover: '#6fa3ff',
    textColorActiveHorizontal: '#5388ff',
    backgroundColor: 'transparent',
    backgroundColorHover: '#2d2d30',
    backgroundColorActive: '#1e3a5f',
    backgroundColorActiveHover: '#2a4a70',
    fontSize: 'var(--font-size-base)',
    fontWeight: '500',
    iconColor: '#9ca3af',
    iconActiveColor: '#5388ff',
  },

  // 侧边菜单配置
  sideMenu: {
    backgroundColor: '#ffffff',
    textColor: '#333333',
    textColorHover: '#316cfa',
    textColorActive: '#316cfa',
    backgroundColorHover: '#f5f5f5',
    backgroundColorActive: '#f6eded',
    borderColor: '#e5e7eb',
    fontSize: 'var(--font-size-base)',
    fontWeight: '400',
    iconColor: '#666666',
    iconColorActive: '#d12723FF',
    collapsedWidth: '64px',
    width: '220px',
  },

  // 暗色模式侧边菜单配置
  sideMenuDark: {
    backgroundColor: '#18181c',
    textColor: '#e5e7eb',
    textColorHover: '#5388ff',
    textColorActive: '#5388ff',
    backgroundColorHover: '#2d2d30',
    backgroundColorActive: '#1e3a5f',
    borderColor: '#2d2d30',
    fontSize: 'var(--font-size-base)',
    fontWeight: '400',
    iconColor: '#9ca3af',
    iconColorActive: '#5388ff',
    collapsedWidth: '64px',
    width: '220px',
  },
}

/**
 * 应用主题配置到 CSS 变量
 * @param {Object} config 主题配置对象
 * @param {boolean} isDark 是否为暗色模式
 */
export function applyThemeConfig(config, isDark = false) {
  const root = document.documentElement

  // 1. 应用字体大小配置
  root.style.setProperty('--font-size-base', '14px')
  root.style.setProperty('--font-size-lg', '16px')
  root.style.setProperty('--font-size-sm', '12px')

  // 2. 应用主题色
  if (config.primaryColor) {
    root.style.setProperty('--primary-color', config.primaryColor)
  }

  // 3. 应用 Header 配置
  const headerConfig = isDark ? (config.headerDark || config.header) : config.header
  if (headerConfig) {
    root.style.setProperty('--layout-header-bg-color', headerConfig.backgroundColor)
    root.style.setProperty('--layout-header-text-color', headerConfig.textColor)
    root.style.setProperty('--layout-header-font-size', headerConfig.fontSize)
    root.style.setProperty('--layout-header-height', headerConfig.height)
    root.style.setProperty('--layout-header-border-color', headerConfig.borderColor)
  }

  // 4. 应用顶部菜单配置
  const topMenuConfig = isDark ? (config.topMenuDark || config.topMenu) : config.topMenu
  if (topMenuConfig) {
    root.style.setProperty('--top-menu-text-color', topMenuConfig.textColor)
    root.style.setProperty('--top-menu-text-color-hover', topMenuConfig.textColorHover)
    root.style.setProperty('--top-menu-text-color-active', topMenuConfig.textColorActive)
    root.style.setProperty('--top-menu-text-color-active-hover', topMenuConfig.textColorActiveHover || topMenuConfig.textColorHover)
    root.style.setProperty('--top-menu-text-color-active-horizontal', topMenuConfig.textColorActiveHorizontal || topMenuConfig.textColorActive)
    root.style.setProperty('--top-menu-bg-color', topMenuConfig.backgroundColor)
    root.style.setProperty('--top-menu-bg-color-hover', topMenuConfig.backgroundColorHover)
    root.style.setProperty('--top-menu-bg-color-active', topMenuConfig.backgroundColorActive)
    root.style.setProperty('--top-menu-bg-color-active-hover', topMenuConfig.backgroundColorActiveHover || topMenuConfig.backgroundColorHover)
    root.style.setProperty('--top-menu-font-size', topMenuConfig.fontSize)
    root.style.setProperty('--top-menu-font-weight', topMenuConfig.fontWeight)
    root.style.setProperty('--top-menu-icon-color', topMenuConfig.iconColor)
    root.style.setProperty('--top-menu-icon-color-active', topMenuConfig.iconActiveColor)
  }

  // 5. 应用侧边菜单配置
  const sideMenuConfig = isDark ? (config.sideMenuDark || config.sideMenu) : config.sideMenu
  if (sideMenuConfig) {
    root.style.setProperty('--side-menu-bg-color', sideMenuConfig.backgroundColor)
    root.style.setProperty('--side-menu-text-color', sideMenuConfig.textColor)
    root.style.setProperty('--side-menu-text-color-hover', sideMenuConfig.textColorHover)
    root.style.setProperty('--side-menu-text-color-active', sideMenuConfig.textColorActive)
    root.style.setProperty('--side-menu-bg-color-hover', sideMenuConfig.backgroundColorHover)
    root.style.setProperty('--side-menu-bg-color-active', sideMenuConfig.backgroundColorActive)
    root.style.setProperty('--side-menu-border-color', sideMenuConfig.borderColor)
    root.style.setProperty('--side-menu-font-size', sideMenuConfig.fontSize)
    root.style.setProperty('--side-menu-font-weight', sideMenuConfig.fontWeight)
    root.style.setProperty('--side-menu-icon-color', sideMenuConfig.iconColor)
    root.style.setProperty('--side-menu-icon-color-active', sideMenuConfig.iconColorActive)
    root.style.setProperty('--side-menu-collapsed-width', sideMenuConfig.collapsedWidth)
    root.style.setProperty('--side-menu-width', sideMenuConfig.width)
  }
}
