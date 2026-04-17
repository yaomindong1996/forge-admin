import merge from 'lodash/merge'
import pick from 'lodash/pick'
import { vChartGlobalThemeJson } from '@/settings/vchartThemes/index'

/**
 * * 合并 color 和全局配置项
 * @param option 配置
 * @param themeSetting 设置
 * @param excludes 排除元素
 * @returns object
 */
export const mergeTheme = <T, U>(option: T, themeSetting: U, includes: string[]) => {
  return (option = merge({}, pick(themeSetting, includes), option))
}

/**
 * * vCharts option 统一前置处理
 * @param option
 * @return option
 */
export const vChartOptionPrefixHandle = (option: any, includes: string[] = []) => {
  option['background'] = 'rgba(0,0,0,0)'
  return mergeTheme(option, vChartGlobalThemeJson, includes)
}
