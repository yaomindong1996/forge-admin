import { defaultThemeConfig } from '@/config/theme.config'
import { solidColor } from '@/utils/navigation-theme'

// 保持租户 API 的 JSON 协议，不新增后端字段。
export function readTenantAppearance(data = {}) {
  let source = data.themeConfig || {}
  if (typeof source === 'string') {
    try {
      source = JSON.parse(source)
    }
    catch (error) {
      console.warn('租户主题 JSON 无效，使用默认外观', error)
      source = {}
    }
  }
  if (!source || typeof source !== 'object' || Array.isArray(source)) {
    source = {}
  }
  const theme = { ...defaultThemeConfig, ...source }
  for (const group of ['header', 'topMenu', 'sideMenu', 'headerDark', 'topMenuDark', 'sideMenuDark']) {
    theme[group] = { ...defaultThemeConfig[group], ...source[group] }
  }
  theme.navigationMode = source.navigationMode || (Object.keys(source).length ? 'custom' : 'auto')
  theme.primaryColor = solidColor(data.systemTheme || source.primaryColor, defaultThemeConfig.primaryColor)
  return theme
}

export function writeTenantAppearance(data, theme) {
  data.systemTheme = theme.primaryColor
  data.themeConfig = JSON.stringify(theme)
}

export function prepareTenantAppearance(data) {
  writeTenantAppearance(data, readTenantAppearance(data))
  // 兼容旧表单临时字段，避免把界面辅助状态传给后端。
  Object.keys(data).filter(key => key.startsWith('theme_') || key === 'themePreset').forEach(key => delete data[key])
  return data
}
