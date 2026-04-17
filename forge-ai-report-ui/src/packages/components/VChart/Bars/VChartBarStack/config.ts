import { PublicConfigClass } from '@/packages/public'
import { VChartBarStackConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import { vChartOptionPrefixHandle } from '@/packages/public/vChart'
import data from './data.json'
import axisThemeJson from '@/settings/vchartThemes/axis.theme.json'
import { ChatCategoryEnum, IBarOption } from '../../index.d'
import { merge, cloneDeep } from 'lodash'
import { vChartGlobalThemeJson } from '@/settings/vchartThemes'

const barConfig = merge(cloneDeep(vChartGlobalThemeJson.bar), {
  style: {
    width: 15
  }
})

export const includes = ['legends', 'tooltip', 'label']
export const option: IBarOption & { dataset?: any } = {
  // 图表配置
  type: 'bar',
  dataset: data,
  xField: ['type'],
  yField: ['value'],
  seriesField: 'year',
  stack: true,
  // 开启百分比
  percent: false,
  // 业务配置（后续会被转换为图表spec)
  category: VChartBarStackConfig.category as ChatCategoryEnum.BAR,
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
  public key = VChartBarStackConfig.key
  public chartConfig = cloneDeep(VChartBarStackConfig)
  // 图表配置项
  public option = vChartOptionPrefixHandle(option, includes)
}
