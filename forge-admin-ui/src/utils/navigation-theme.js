// 导航只有三个基础色；其余颜色由表面和品牌色推导，避免每个布局分别维护一套配色。
export const navigationPresets = [
  { key: 'clean', name: '清爽白', primary: '#4242F7', header: '#FFFFFF', side: '#FFFFFF' },
  { key: 'forge-blue', name: '协作蓝', primary: '#4242F7', header: '#4242F7', side: '#FFFFFF' },
  { key: 'mist', name: '雾蓝', primary: '#2F6FED', header: '#EEF4FF', side: '#EEF4FF' },
  { key: 'mist-teal', name: '雾青', primary: '#0E8F7E', header: '#E7F6F3', side: '#E7F6F3' },
  { key: 'mist-violet', name: '雾紫', primary: '#6D5EF6', header: '#F1EEFF', side: '#F1EEFF' },
  { key: 'cloud-blue', name: '云蓝', primary: '#2F6FED', header: '#FFFFFF', side: '#F3F6FC' },
  { key: 'ink-blue', name: '墨蓝', primary: '#3569D4', header: '#1E293B', side: '#182234' },
  { key: 'graphite', name: '石墨', primary: '#334155', header: '#1F2937', side: '#FFFFFF' },
  { key: 'enterprise-red', name: '企业红', primary: '#D12723', header: '#B91C1C', side: '#FFFFFF' },
  { key: 'teal', name: '松石绿', primary: '#0E8F7E', header: '#0F766E', side: '#FFFFFF' },
  // CRM / ERP 常见后台主色：客户蓝、业资深蓝、供应链青绿
  { key: 'crm-sky', name: '客户蓝', primary: '#1890FF', header: '#185ABD', side: '#FFFFFF' },
  { key: 'erp-navy', name: '业资深蓝', primary: '#0B5FFF', header: '#0A2540', side: '#F4F7FA' },
  { key: 'erp-teal', name: '供应链青', primary: '#0F766E', header: '#134E4A', side: '#FFFFFF' },
]

function channels(value) {
  const text = String(value || '').trim()
  const hex = text.match(/^#([\da-f]{3}|[\da-f]{4}|[\da-f]{6}|[\da-f]{8})$/i)?.[1]
  if (hex) {
    const full = hex.length <= 4 ? [...hex].map(char => char + char).join('') : hex
    const rgb = [0, 2, 4].map(offset => Number.parseInt(full.slice(offset, offset + 2), 16))
    return full.length === 8 ? [...rgb, Number.parseInt(full.slice(6), 16) / 255] : rgb
  }
  const rgb = text.match(/^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)(?:\s*,\s*([\d.]+))?\s*\)$/i)
  const values = rgb?.slice(1, 4).map(Number)
  const alpha = Number(rgb?.[4] ?? 1)
  if (!values || values.some(value => !Number.isFinite(value) || value > 255)
    || !Number.isFinite(alpha) || alpha < 0 || alpha > 1) {
    return null
  }
  return values.concat(alpha)
}

export function solidColor(value, fallback = '#FFFFFF') {
  const rgb = channels(value) || channels(fallback) || channels('#FFFFFF')
  return `#${rgb.slice(0, 3).map(value => Math.round(value).toString(16).padStart(2, '0')).join('')}`
}

export function mixColor(foreground, background, weight) {
  const fg = channels(foreground) || channels('#1D2129')
  const bg = channels(background) || channels('#FFFFFF')
  return solidColor(`rgb(${bg.slice(0, 3).map((value, index) => value * (1 - weight) + fg[index] * weight).join(',')})`)
}

function luminance(color) {
  const values = channels(color).slice(0, 3).map((value) => {
    const channel = value / 255
    return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4
  })
  return values[0] * 0.2126 + values[1] * 0.7152 + values[2] * 0.0722
}

export function contrastRatio(foreground, background) {
  const fg = channels(foreground)
  const bg = channels(background)
  if (!fg || !bg) {
    return 0
  }
  const visible = fg.length === 4 ? mixColor(foreground, background, fg[3]) : foreground
  const values = [luminance(visible), luminance(background)].sort((a, b) => b - a)
  return (values[0] + 0.05) / (values[1] + 0.05)
}

export function readableColor(background, preferred) {
  if (contrastRatio(preferred, background) >= 4.5) {
    return preferred
  }
  return contrastRatio('#1D2129', background) >= contrastRatio('#FFFFFF', background) ? '#1D2129' : '#FFFFFF'
}

/** 空横条跟随主色；主色与顶栏太接近时改用可读对比色，避免「协作蓝」等深色顶栏上看不见横条。 */
export function resolveTopMenuActiveBarColor(activeBarColor, primary, headerBackground) {
  const brand = solidColor(primary, '#4242F7')
  const configured = String(activeBarColor || '').trim()
  if (configured) {
    return solidColor(configured, brand)
  }
  const header = solidColor(headerBackground, '#FFFFFF')
  return solidColor(contrastRatio(brand, header) >= 1.8 ? brand : readableColor(header, brand))
}

export function createNavigationTheme({ primary = '#4242F7', header = '#FFFFFF', side = '#FFFFFF' } = {}) {
  const brand = solidColor(primary, '#4242F7')
  const headerBg = solidColor(header)
  const sideBg = solidColor(side)
  const headerText = readableColor(headerBg)
  const headerAccent = readableColor(headerBg, brand)
  const sideText = readableColor(sideBg)
  const sideActive = mixColor(brand, sideBg, 0.12)
  const sideHover = mixColor(sideText, sideBg, 0.06)
  const sideAccent = readableColor(sideActive, brand)
  return {
    primaryColor: brand,
    navigationMode: 'auto',
    header: {
      backgroundColor: headerBg,
      textColor: headerText,
      brandTitleTextColor: headerText,
      borderColor: mixColor(headerText, headerBg, 0.14),
    },
    topMenu: {
      // 不写 activeBarColor：保留调用方/租户已配置的横条，空值表示跟随品牌主色。
      textColor: headerText,
      textColorHover: headerAccent,
      textColorActive: headerAccent,
      textColorActiveHorizontal: headerAccent,
      textColorActiveHover: headerAccent,
      iconColor: headerText,
      iconActiveColor: headerAccent,
      backgroundColor: 'transparent',
      backgroundColorHover: mixColor(headerText, headerBg, 0.08),
      backgroundColorActive: mixColor(headerText, headerBg, 0.1),
      backgroundColorActiveHover: mixColor(headerText, headerBg, 0.14),
    },
    sideMenu: {
      backgroundColor: sideBg,
      textColor: sideText,
      iconColor: sideText,
      textColorHover: readableColor(sideHover, brand),
      backgroundColorHover: sideHover,
      textColorActive: sideAccent,
      iconColorActive: sideAccent,
      backgroundColorActive: sideActive,
      parentTextColorActive: sideAccent,
      parentBackgroundColorActive: mixColor(brand, sideBg, 0.06),
      borderColor: mixColor(sideText, sideBg, 0.14),
    },
  }
}

function protectThemeSurfaces(theme) {
  const header = theme.header
  const top = theme.topMenu
  const side = theme.sideMenu
  header.backgroundColor = solidColor(header.backgroundColor)
  side.backgroundColor = solidColor(side.backgroundColor)
  for (const field of ['textColor', 'brandTitleTextColor']) {
    header[field] = readableColor(header.backgroundColor, header[field])
  }
  const topStates = {
    textColor: 'backgroundColor',
    iconColor: 'backgroundColor',
    textColorHover: 'backgroundColorHover',
    textColorActive: 'backgroundColorActive',
    textColorActiveHover: 'backgroundColorActiveHover',
    textColorActiveHorizontal: 'backgroundColorActive',
    iconActiveColor: 'backgroundColorActive',
  }
  for (const [field, state] of Object.entries(topStates)) {
    const rgb = channels(top[state])
    const background = rgb ? mixColor(top[state], header.backgroundColor, rgb[3] ?? 1) : header.backgroundColor
    top[field] = readableColor(background, top[field])
  }
  for (const field of ['textColor', 'iconColor']) {
    side[field] = readableColor(side.backgroundColor, side[field])
  }
  const states = [
    ['textColorHover', 'backgroundColorHover'],
    ['textColorActive', 'backgroundColorActive'],
    ['iconColorActive', 'backgroundColorActive'],
    ['parentTextColorActive', 'parentBackgroundColorActive'],
  ]
  for (const [text, bg] of states) {
    side[bg] = solidColor(side[bg], side.backgroundColor)
    side[text] = readableColor(side[bg], side[text])
  }
  return theme
}

export function resolveNavigationMode(config, isDark = false) {
  // 旧配置只有一个全局模式；缺少深色模式时沿用它，而不是重置历史定制。
  const legacy = config?.navigationMode === 'auto' ? 'auto' : 'custom'
  const darkMode = config?.navigationModeDark
  return isDark && ['auto', 'custom'].includes(darkMode) ? darkMode : legacy
}

export function updateNavigationMode(config, mode, isDark = false) {
  // 首次编辑先固定另一模式；浅色开关变化不能改变深色的历史回退结果。
  return {
    ...config,
    navigationMode: isDark ? resolveNavigationMode(config) : mode,
    navigationModeDark: isDark ? mode : resolveNavigationMode(config, true),
  }
}

export function resolveNavigationTheme(config, defaults, isDark = false) {
  const source = config || {}
  const suffix = isDark ? 'Dark' : ''
  const groups = ['header', 'topMenu', 'sideMenu']
  const theme = { ...source, primaryColor: solidColor(source.primaryColor, defaults.primaryColor) }
  for (const group of groups) {
    theme[group] = { ...defaults[group + suffix], ...source[group + suffix] }
  }
  if (resolveNavigationMode(source, isDark) === 'auto') {
    const generated = createNavigationTheme({
      primary: theme.primaryColor,
      header: theme.header.backgroundColor,
      side: theme.sideMenu.backgroundColor,
    })
    for (const group of groups) {
      theme[group] = { ...theme[group], ...generated[group] }
    }
  }
  // 自动推导不得冲掉已保存的顶部选中横条（含租户配置）；空字符串仍表示跟随主色。
  const sourceTop = source[`topMenu${suffix}`] || {}
  if (Object.prototype.hasOwnProperty.call(sourceTop, 'activeBarColor')) {
    theme.topMenu.activeBarColor = sourceTop.activeBarColor
  }
  // 旧 JSON 保留配置，只有不可读的前景色在运行时兜底，不改数据库中的原值。
  return protectThemeSurfaces(theme)
}

export function updateNavigationBases(config, patch, isDark = false) {
  const suffix = isDark ? 'Dark' : ''
  const generated = createNavigationTheme({
    primary: patch.primary || config.primaryColor,
    header: patch.header || config[`header${suffix}`]?.backgroundColor,
    side: patch.side || config[`sideMenu${suffix}`]?.backgroundColor,
  })
  // 深色编辑只更新对应分组，不能把深色背景写进浅色配置。
  const next = { ...updateNavigationMode(config, 'auto', isDark), primaryColor: generated.primaryColor }
  for (const group of ['header', 'topMenu', 'sideMenu']) {
    const key = group + suffix
    next[key] = { ...config[key], ...generated[group] }
    // 显式保留横条：扩展运算后若 generated 未带该字段，仍以原配置为准。
    if (group === 'topMenu' && Object.prototype.hasOwnProperty.call(config[key] || {}, 'activeBarColor')) {
      next[key].activeBarColor = config[key].activeBarColor
    }
  }
  return next
}
