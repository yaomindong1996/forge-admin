import { PublicConfigClass } from '@/packages/public'
import { VChartBarCrossrangeConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import { vChartOptionPrefixHandle } from '@/packages/public/vChart'
import data from './data.json'
import axisThemeJson from '@/settings/vchartThemes/axis.theme.json'
import { ChatCategoryEnum, IBarOption } from '../../index.d'
import { merge, cloneDeep } from 'lodash'
import { vChartGlobalThemeJson } from '@/settings/vchartThemes'

const barConfig = merge(cloneDeep(vChartGlobalThemeJson.bar), {
  style: {
    height: 10
  }
})
delete (barConfig.style as { width?: any }).width

export const includes = ['legends', 'tooltip', 'label']
export const option: IBarOption & { dataset?: any } = {
  // 图表配置
  type: 'bar',
  dataset: data,
  stack: true,
  xField: ['value'],
  yField: ['year', 'type'],
  seriesField: 'type',
  // 业务配置（后续会被转换为图表spec)
  category: VChartBarCrossrangeConfig.category as ChatCategoryEnum.BAR,
  direction: 'horizontal',
  xAxis: {
    name: 'x轴',
    ...(merge(cloneDeep(axisThemeJson), {
      unit: {
        style: {
          dx: 10,
          dy: 0
        }
      }
    }) as any),
    grid: {
      ...axisThemeJson.grid,
      visible: false
    }
  },
  yAxis: {
    name: 'y轴',
    ...(merge(cloneDeep(axisThemeJson), {
      unit: {
        style: {
          dx: 0,
          dy: -10
        }
      }
    }) as any),
    grid: {
      ...axisThemeJson.grid,
      style: {
        ...axisThemeJson.grid.style
      }
    }
  },
  bar: {
    ...barConfig
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = VChartBarCrossrangeConfig.key
  public chartConfig = cloneDeep(VChartBarCrossrangeConfig)
  // 图表配置项
  public option = vChartOptionPrefixHandle(option, includes)
}
