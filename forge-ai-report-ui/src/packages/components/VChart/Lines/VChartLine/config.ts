import { PublicConfigClass } from '@/packages/public'
import { VChartLineConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import { vChartOptionPrefixHandle } from '@/packages/public/vChart'
import data from './data.json'
import cloneDeep from 'lodash/cloneDeep'
import axisThemeJson from '@/settings/vchartThemes/axis.theme.json'
import { ChatCategoryEnum, ILineOption } from '../../index.d'

export const includes = ['legends', 'tooltip', 'label', 'line', 'point']
export const option: ILineOption & { dataset?: any } = {
  // 图表配置
  type: 'line',
  dataset: data,
  xField: 'type',
  yField: 'value',
  seriesField: 'country',
  stack: true,
  // 开启百分比
  percent: false,
  // 业务配置（后续会被转换为图表spec)
  category: VChartLineConfig.category as ChatCategoryEnum.LINE,
  xAxis: {
    name: 'x轴',
    ...axisThemeJson,
    grid: {
      ...axisThemeJson.grid,
      visible: false
    }
  } as any,
  yAxis: {
    name: 'y轴',
    ...axisThemeJson,
    grid: {
      ...axisThemeJson.grid,
      style: {
        ...axisThemeJson.grid.style,
        lineDash: [3, 3]
      }
    }
  } as any
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = VChartLineConfig.key
  public chartConfig = cloneDeep(VChartLineConfig)
  // 图表配置项
  public option = vChartOptionPrefixHandle(option, includes)
}
