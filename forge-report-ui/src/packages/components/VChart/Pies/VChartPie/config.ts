import { PublicConfigClass } from '@/packages/public'
import { VChartPieConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import { vChartOptionPrefixHandle } from '@/packages/public/vChart'
import data from './data.json'
import cloneDeep from 'lodash/cloneDeep'
import type { ChatCategoryEnum, IPieOption } from '../../index.d'

const OUTER_RADIUS = 0.75

export const includes = ['legends', 'tooltip']
export const option: IPieOption & { dataset?: any } = {
  // 图表配置
  type: 'pie',
  dataset: data,
  categoryField: 'year',
  valueField: 'value',
  seriesField: 'year',
  // 中心
  centerX: '50%',
  centerY: '50%',
  innerRadius: 0.68,
  outerRadius: OUTER_RADIUS,
  label: {
    visible: true,
    position: 'outside',
    style: {
      fontSize: 12,
      fill: '#B9B8CE',
      fontFamily: 'SimSun',
      fontWeight: 'normal',
      angle: 0
    },
    line: {
      visible: true
    }
  },
  pie: {
    style: {
      // 圆角
      cornerRadius: 50,
      // 描边宽度
      outerBorder: {
        // 透明度
        strokeOpacity: 1,
        // 外描边距离
        distance: 0,
        // 宽度
        lineWidth: 0,
        // 颜色
        stroke: '#ffffff'
      },
      // 纹理
      texture: ''
    },
    state: {
      hover: {
        outerRadius: 0.85
      },
      selected: {
        outerRadius: 0.85
      }
    }
  },
  // 业务配置（后续会被转换为图表spec)
  category: VChartPieConfig.category as ChatCategoryEnum.PIE,
  extensionMark: [],
  // 动画
  animationNormal: {}
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = VChartPieConfig.key
  public chartConfig = cloneDeep(VChartPieConfig)
  // 图表配置项
  public option = vChartOptionPrefixHandle(option, includes)
}
