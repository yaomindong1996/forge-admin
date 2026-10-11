import { forgeSymbols, resolveForgeSymbol } from './forgeSymbols'

// 功能语义固定配色，不按菜单排序/ID 随机取色；深色使用同色相的亮色，避免灰暗或消失。
const palettes = Object.freeze({
  ocean: ['#3976E8', '#149BA9', '#8BB8FF', '#71D5DD'],
  violet: ['#7961D4', '#4384DF', '#BCABFF', '#93BEFF'],
  teal: ['#168D83', '#537DDA', '#70D4C5', '#A0BDFF'],
  amber: ['#CD8726', '#D76657', '#EBC06F', '#F5A49A'],
  iris: ['#6F68D8', '#B8862C', '#B4AEFF', '#EAC579'],
  rose: ['#CB667E', '#8A69CD', '#F0A1B5', '#C8ADFA'],
})

// fill 只填充符号本身的闭合区域，不增加菜单背景块。
export const forgeSymbolColorways = Object.freeze({
  home: { palette: 'ocean', layers: ['primary', 'secondary-fill'] },
  apps: { palette: 'iris', layers: ['primary-fill', 'secondary-fill', 'secondary-fill', 'primary', 'primary'] },
  builder: { palette: 'violet', layers: ['primary-fill', 'secondary', 'secondary', 'primary', 'primary'] },
  workflow: { palette: 'teal', layers: ['primary-fill', 'secondary-fill', 'primary-fill', 'secondary', 'secondary'] },
  analytics: { palette: 'ocean', layers: ['primary', 'secondary', 'primary', 'secondary'] },
  ai: { palette: 'violet', layers: ['primary-fill', 'secondary', 'secondary'] },
  platform: { palette: 'ocean', layers: ['primary-fill', 'secondary-fill', 'secondary', 'primary'] },
  collaboration: { palette: 'amber', layers: ['primary-fill', 'primary', 'secondary', 'secondary'] },
  open: { palette: 'teal', layers: ['primary', 'primary', 'secondary'] },
  plugins: {
    palette: 'iris',
    layers: ['primary-fill'],
    accents: ['M14 4h6v6h1a2 2 0 0 1 0 4h-1v6h-6'],
  },
  monitor: { palette: 'teal', layers: ['primary-fill', 'primary', 'secondary'] },
  message: { palette: 'rose', layers: ['primary-fill', 'secondary'] },
  print: { palette: 'violet', layers: ['secondary', 'primary-fill', 'secondary-fill', 'secondary'] },
  schedule: { palette: 'amber', layers: ['secondary', 'primary', 'primary-fill', 'secondary'] },
  database: { palette: 'ocean', layers: ['secondary-fill', 'primary', 'secondary'] },
  shield: { palette: 'teal', layers: ['primary-fill', 'secondary'] },
  approval: { palette: 'ocean', layers: ['primary-fill', 'secondary-fill', 'secondary'] },
  actions: { palette: 'teal', layers: ['primary-fill', 'secondary', 'primary', 'secondary', 'primary'] },
  layers: { palette: 'violet', layers: ['primary-fill', 'secondary', 'primary'] },
  connection: { palette: 'amber', layers: ['secondary', 'primary-fill', 'secondary'] },
  module: { palette: 'iris', layers: ['primary-fill', 'secondary', 'secondary', 'secondary'] },
})

export function forgeSymbolPaletteStyle(name) {
  const config = forgeSymbolColorways[resolveForgeSymbol(name)]
  const [primary, secondary, primaryDark, secondaryDark] = palettes[config.palette]
  return {
    '--forge-icon-main-light': primary,
    '--forge-icon-accent-light': secondary,
    '--forge-icon-main-dark': primaryDark,
    '--forge-icon-accent-dark': secondaryDark,
  }
}

export function forgeSymbolColoredPaths(name) {
  const symbol = resolveForgeSymbol(name)
  const config = forgeSymbolColorways[symbol]
  const paths = forgeSymbols[symbol].map((d, index) => {
    const role = config.layers[index]
    const tone = role.split('-')[0]
    return {
      d,
      stroke: `var(--forge-icon-${tone})`,
      fill: role.endsWith('-fill') ? `var(--forge-icon-${tone}-fill)` : 'none',
    }
  })
  return [...paths, ...(config.accents || []).map(d => ({
    d,
    stroke: 'var(--forge-icon-secondary)',
    fill: 'none',
  }))]
}
