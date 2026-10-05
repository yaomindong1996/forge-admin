import { readTenantAppearance } from '@/utils/tenant-appearance'

export { readTenantAppearance }

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
