import { defaultThemeConfig } from '@/config/theme.config'
import { solidColor } from '@/utils/navigation-theme'

// 租户编辑与会话外观恢复共用解析规则，不写入租户或修改原配置。
export function readTenantAppearance(data = {}) {
  let source = data?.themeConfig || {}
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
  const fallback = solidColor(source.primaryColor, defaultThemeConfig.primaryColor)
  theme.primaryColor = solidColor(data?.systemTheme, fallback)
  return theme
}
