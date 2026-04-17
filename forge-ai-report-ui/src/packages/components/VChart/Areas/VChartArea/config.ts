import { PublicConfigClass } from '@/packages/public'
import { VChartAreaConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import { vChartOptionPrefixHandle } from '@/packages/public/vChart'
import data from './data.json'
import cloneDeep from 'lodash/cloneDeep'
import axisThemeJson from '@/settings/vchartThemes/axis.theme.json'
import { IAreaOption } from '../../index.d'

export const includes = ['legends', 'tooltip']
export const option: IAreaOption & { dataset?: any } = {
  // 图表配置
  type: 'area',
  dataset: data,
  xField: 'type',
  yField: 'value',
  seriesField: 'country',
  stack: true,
  // 业务配置（后续会被转换为图表spec)
  category: VChartAreaConfig.category,
  xAxis: {
    name: 'x轴',
    ...axisThemeJson,
    grid: {
      ...axisThemeJson.grid,
      visible: false
    }
  },
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
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = VChartAreaConfig.key
  public chartConfig = cloneDeep(VChartAreaConfig)
  // 图表配置项
  public option = vChartOptionPrefixHandle(option, includes)
}
