import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const VChartLineConfig: ConfigType = {
  key: 'VChartLine',
  chartKey: 'VVChartLine',
  conKey: 'VCVChartLine',
  title: '折线图-VChart',
  category: ChatCategoryEnum.LINE,
  categoryName: ChatCategoryEnumName.LINE,
  package: PackagesCategoryEnum.VCHART,
  chartFrame: ChartFrameEnum.VCHART,
  image: 'vchart_line.png'
}
