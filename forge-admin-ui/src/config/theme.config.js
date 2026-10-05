/**********************************
 * @FilePath: theme.config.js
 * @Description: 主题配置文件 - 统一管理所有布局的样式配置
 **********************************/

/**
 * 默认主题配置
 */
import { resolveNavigationTheme, solidColor } from '@/utils/navigation-theme'

export const defaultThemeConfig = {
  navigationMode: 'auto',
  // 主题色
  primaryColor: '#4242F7',

  // Header 配置
  header: {
    backgroundColor: '#FFFFFF',
    textColor: '#1D2129',
    brandTitleTextColor: '#1D2129',
    fontSize: 'var(--font-size-base)',
    height: '60px',
    borderColor: '#E5E7EB',
  },

  // 暗色模式 Header 配置
  headerDark: {
    backgroundColor: '#18181c',
    textColor: '#e5e7eb',
    brandTitleTextColor: '#e5e7eb',
    fontSize: 'var(--font-size-base)',
    height: '60px',
    borderColor: '#2d2d30',
  },

  // 顶部菜单配置
  topMenu: {
    activeBarColor: '', // 空值跟随品牌主色，与选中文字颜色独立。
    textColor: 'rgba(255, 255, 255, 0.75)',
    textColorHover: '#FFFFFF',
    textColorActive: '#FFFFFF',
    textColorActiveHover: '#FFFFFF',
    textColorActiveHorizontal: '#FFFFFF',
    backgroundColor: 'transparent',
    backgroundColorHover: 'transparent',
    backgroundColorActive: 'transparent',
    backgroundColorActiveHover: 'transparent',
    fontSize: 'var(--font-size-base)',
    fontWeight: '500',
    iconColor: 'rgba(255, 255, 255, 0.75)',
    iconActiveColor: '#FFFFFF',
  },

  // 暗色模式顶部菜单配置
  topMenuDark: {
    activeBarColor: '',
    textColor: '#e5e7eb',
    textColorHover: '#5388ff',
    textColorActive: '#5388ff',
    textColorActiveHover: '#6fa3ff',
    textColorActiveHorizontal: '#5388ff',
    backgroundColor: 'transparent',
    backgroundColorHover: 'transparent',
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
    parentTextColorActive: '#1d4ed8',
    backgroundColorHover: '#f5f5f5',
    backgroundColorActive: '#f6eded',
    parentBackgroundColorActive: '#eef4ff',
    borderColor: '#e5e7eb',
    fontSize: 'var(--font-size-base)',
    fontWeight: '400',
    iconColor: '#666666',
    iconColorActive: '#4242F7',
    collapsedWidth: '64px',
    width: '220px',
  },

  // 暗色模式侧边菜单配置
  sideMenuDark: {
    backgroundColor: '#18181c',
    textColor: '#e5e7eb',
    textColorHover: '#5388ff',
    textColorActive: '#5388ff',
    parentTextColorActive: '#93c5fd',
    backgroundColorHover: '#2d2d30',
    backgroundColorActive: '#1e3a5f',
    parentBackgroundColorActive: '#1e293b',
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
 * 调整颜色亮度
 * @param {string} hex - 十六进制颜色值
 * @param {number} amount - 调整量，正数变亮，负数变暗
 * @returns {string} 调整后的十六进制颜色
 */
function adjustColorBrightness(hex, amount) {
  const num = Number.parseInt(hex.replace('#', ''), 16)
  let r = (num >> 16) + amount
  let g = ((num >> 8) & 0x00FF) + amount
  let b = (num & 0x0000FF) + amount

  r = Math.min(255, Math.max(0, r))
  g = Math.min(255, Math.max(0, g))
  b = Math.min(255, Math.max(0, b))

  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`
}

function hexToRgb(hex) {
  const normalized = hex.replace('#', '')
  const value = normalized.length === 3
    ? normalized.split('').map(char => char + char).join('')
    : normalized

  const num = Number.parseInt(value, 16)

  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  }
}

function applyMenuVariables(root, prefix, properties) {
  for (const [name, value] of Object.entries(properties)) {
    root.style.setProperty(`--${prefix}-${name}`, value)
  }
}

function applyTopMenuConfig(root, menu, primary) {
  applyMenuVariables(root, 'top-menu', {
    'active-bar-color': solidColor(menu.activeBarColor, primary),
    'text-color': menu.textColor,
    'text-color-hover': menu.textColorHover || menu.textColorActive || menu.textColor,
    'text-color-active': menu.textColorActive,
    'text-color-active-hover': menu.textColorActiveHover || menu.textColorHover,
    'text-color-active-horizontal': menu.textColorActiveHorizontal || menu.textColorActive,
    'bg-color': menu.backgroundColor,
    'bg-color-hover': menu.backgroundColorHover,
    'bg-color-active': menu.backgroundColorActive,
    'bg-color-active-hover': menu.backgroundColorActiveHover || menu.backgroundColorHover,
    'font-size': menu.fontSize,
    'font-weight': menu.fontWeight,
    'icon-color': menu.iconColor,
    'icon-color-active': menu.iconActiveColor,
  })
}

function applySideMenuConfig(root, menu) {
  applyMenuVariables(root, 'side-menu', {
    'bg-color': menu.backgroundColor,
    'text-color': menu.textColor,
    'text-color-hover': menu.textColorHover,
    'text-color-active': menu.textColorActive,
    'parent-text-color-active': menu.parentTextColorActive || menu.textColorActive,
    'bg-color-hover': menu.backgroundColorHover,
    'bg-color-active': menu.backgroundColorActive,
    'parent-bg-color-active': menu.parentBackgroundColorActive || 'transparent',
    'border-color': menu.borderColor,
    'font-size': menu.fontSize,
    'font-weight': menu.fontWeight,
    'icon-color': menu.iconColor,
    'icon-color-active': menu.iconColorActive,
    'collapsed-width': menu.collapsedWidth,
    'width': menu.width,
  })
}

/**
 * 应用主题配置到 CSS 变量
 * @param {object} config 主题配置对象
 * @param {boolean} isDark 是否为暗色模式
 */
export function applyThemeConfig(config, isDark = false) {
  config = resolveNavigationTheme(config, defaultThemeConfig, isDark)
  const root = document.documentElement

  // 1. 应用字体大小配置
  root.style.setProperty('--font-size-base', '14px')
  root.style.setProperty('--font-size-lg', '16px')
  root.style.setProperty('--font-size-sm', '12px')

  // 2. 应用主题色（按钮扁平纯色，不用渐变和悬浮投影）
  if (config.primaryColor) {
    const primary = config.primaryColor
    const primaryHover = adjustColorBrightness(primary, -8)
    const primaryActive = adjustColorBrightness(primary, -14)
    const { r, g, b } = hexToRgb(primary)
    const primaryLight = adjustColorBrightness(primary, 26)
    const primaryDark = adjustColorBrightness(primary, -22)

    root.style.setProperty('--primary-color', primary)
    root.style.setProperty('--primary-gradient', `linear-gradient(135deg, ${primaryLight} 0%, ${primary} 100%)`)
    root.style.setProperty('--primary-gradient-hover', `linear-gradient(135deg, ${primary} 0%, ${primaryDark} 100%)`)
    root.style.setProperty('--button-primary-bg', primary)
    root.style.setProperty('--button-primary-bg-hover', primaryHover)
    root.style.setProperty('--button-primary-bg-active', primaryActive)
    root.style.setProperty('--button-primary-border', primary)
    root.style.setProperty('--button-primary-shadow', 'none')
    root.style.setProperty('--button-primary-shadow-hover', 'none')
    root.style.setProperty('--button-primary-shadow-active', 'none')
    root.style.setProperty('--button-primary-focus-ring', `0 0 0 2px rgba(${r}, ${g}, ${b}, 0.18)`)
    root.style.setProperty('--button-primary-ambient', 'transparent')
    root.style.setProperty('--button-primary-ambient-hover', 'transparent')
  }

  // 3. 应用 Header 配置
  const headerConfig = config.header
  if (headerConfig) {
    root.style.setProperty('--layout-header-bg-color', headerConfig.backgroundColor)
    root.style.setProperty('--layout-header-text-color', headerConfig.textColor)
    root.style.setProperty('--brand-title-text-color', headerConfig.brandTitleTextColor || headerConfig.textColor || '#FFFFFF')
    root.style.setProperty('--layout-header-font-size', headerConfig.fontSize)
    root.style.setProperty('--layout-header-height', headerConfig.height)
    root.style.setProperty('--layout-header-border-color', headerConfig.borderColor)
    root.style.setProperty('--layout-header-hover-color', `color-mix(in srgb, ${headerConfig.textColor} 8%, transparent)`)
  }

  // 4. 应用顶部菜单配置
  const topMenuConfig = config.topMenu
  if (topMenuConfig) {
    applyTopMenuConfig(root, topMenuConfig, config.primaryColor)
  }

  // 5. 应用侧边菜单配置
  const sideMenuConfig = config.sideMenu
  if (sideMenuConfig) {
    applySideMenuConfig(root, sideMenuConfig)
  }
}
