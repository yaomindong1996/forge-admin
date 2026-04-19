/**
 * 菜单主题配置工具函数
 */

/**
 * 获取顶部菜单主题覆盖配置
 * @returns {object} 主题覆盖配置对象
 */
export function getTopMenuThemeOverrides() {
  const textColor = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-text-color').trim() || '#333333'
  const textColorHover = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-text-color-hover').trim() || '#316cfa'
  const textColorActive = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-text-color-active').trim() || '#316cfa'
  const textColorActiveHover = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-text-color-active-hover').trim() || textColorHover
  const textColorActiveHorizontal = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-text-color-active-horizontal').trim() || textColorActive
  const bgColorHover = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-bg-color-hover').trim() || '#f5f5f5'
  const bgColorActive = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-bg-color-active').trim() || '#e8f0fe'
  const bgColorActiveHover = getComputedStyle(document.documentElement).getPropertyValue('--top-menu-bg-color-active-hover').trim() || bgColorHover

  return {
    // 通用颜色
    itemTextColor: textColor,
    itemTextColorHover: textColorHover,
    itemTextColorActive: textColorActive,
    itemTextColorChildActive: textColorActive,
    itemTextColorActiveHover: textColorActiveHover,
    itemColorHover: bgColorHover,
    itemColorActive: bgColorActive,
    itemColorActiveHover: bgColorActiveHover,

    // Horizontal 模式专用颜色（完整版）
    itemTextColorHorizontal: textColor, // Horizontal 默认文字色
    itemTextColorHoverHorizontal: textColorHover, // Horizontal 悬停文字色
    itemTextColorActiveHorizontal: textColorActiveHorizontal, // Horizontal 选中文字色（专用） ✅
    itemTextColorChildActiveHorizontal: textColorActiveHorizontal, // Horizontal 子菜单选中文字色
    itemTextColorActiveHoverHorizontal: textColorActiveHover, // Horizontal 选中悬停文字色
    itemColorHoverHorizontal: bgColorHover, // Horizontal 悬停背景色
    itemColorActiveHorizontal: bgColorActive, // Horizontal 选中背景色
    itemColorChildActiveHorizontal: bgColorActive, // Horizontal 子菜单选中背景色
    itemColorActiveHoverHorizontal: bgColorActiveHover, // Horizontal 选中悬停背景色
  }
}
