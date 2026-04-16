import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const VChartScatterConfig: ConfigType = {
  key: 'VChartScatter',
  chartKey: 'VVChartScatter',
  conKey: 'VCVChartScatter',
  title: '散点图-VChart',
  category: ChatCategoryEnum.SCATTER,
  categoryName: ChatCategoryEnumName.SCATTER,
  package: PackagesCategoryEnum.VCHART,
  chartFrame: ChartFrameEnum.VCHART,
  image: 'vchart_scatter.png'
}
