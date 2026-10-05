// 导航只有三个基础色；其余颜色由表面和品牌色推导，避免每个布局分别维护一套配色。
export const navigationPresets = [
  { key: 'clean', name: '清爽白', primary: '#4242F7', header: '#FFFFFF', side: '#FFFFFF' },
  { key: 'forge-blue', name: '品牌蓝', primary: '#4242F7', header: '#4242F7', side: '#FFFFFF' },
  { key: 'mist', name: '雾蓝', primary: '#2F6FED', header: '#EEF4FF', side: '#EEF4FF' },
  { key: 'graphite', name: '石墨', primary: '#334155', header: '#1F2937', side: '#FFFFFF' },
  { key: 'enterprise-red', name: '企业红', primary: '#D12723', header: '#B91C1C', side: '#FFFFFF' },
  { key: 'teal', name: '松石绿', primary: '#0E8F7E', header: '#0F766E', side: '#FFFFFF' },
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
  if (!rgb || rgb.slice(1, 4).some(value => Number(value) > 255)) {
    return null
  }
  return rgb.slice(1, 4).map(Number).concat(Number(rgb[4] ?? 1))
}

export function solidColor(value, fallback = '#FFFFFF') {
  const rgb = channels(value) || channels(fallback)
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

export function resolveNavigationTheme(config, defaults, isDark = false) {
  const source = config || {}
  const suffix = isDark ? 'Dark' : ''
  const groups = ['header', 'topMenu', 'sideMenu']
  const theme = { ...source, primaryColor: solidColor(source.primaryColor, defaults.primaryColor) }
  for (const group of groups) {
    theme[group] = { ...defaults[group + suffix], ...source[group + suffix] }
  }
  if (source.navigationMode === 'auto') {
    const generated = createNavigationTheme({
      primary: theme.primaryColor,
      header: theme.header.backgroundColor,
      side: theme.sideMenu.backgroundColor,
    })
    for (const group of groups) {
      theme[group] = { ...theme[group], ...generated[group] }
    }
  }
  // 旧 JSON 保留配置，只有不可读的前景色在运行时兜底，不改数据库中的原值。
  return protectThemeSurfaces(theme)
}

export function updateNavigationBases(config, patch) {
  const generated = createNavigationTheme({
    primary: patch.primary || config.primaryColor,
    header: patch.header || config.header?.backgroundColor,
    side: patch.side || config.sideMenu?.backgroundColor,
  })
  const next = { ...config, ...generated }
  for (const group of ['header', 'topMenu', 'sideMenu']) {
    next[group] = { ...config[group], ...generated[group] }
  }
  return next
}
