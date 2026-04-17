import themeJson from './global.theme.json'

type ThemeJsonType = typeof themeJson
export type FontType = {
  fontSize: number
  fontFamily: string
  fontWeight: string
  fill: string
  dy?: number
  dx?: number
  angle?: number
  [T: string]: any
}
export interface vChartGlobalThemeJsonType extends Partial<ThemeJsonType> {
  dataset?: any
  [T: string]: any
}

export const vChartGlobalThemeJson = { ...themeJson, dataset: null }
