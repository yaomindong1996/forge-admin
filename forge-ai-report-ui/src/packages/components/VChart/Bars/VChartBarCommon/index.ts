import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const VChartBarCommonConfig: ConfigType = {
  key: 'VChartBarCommon',
  chartKey: 'VVChartBarCommon',
  conKey: 'VCVChartBarCommon',
  title: '并列柱状图-VChart',
  category: ChatCategoryEnum.BAR,
  categoryName: ChatCategoryEnumName.BAR,
  package: PackagesCategoryEnum.VCHART,
  chartFrame: ChartFrameEnum.VCHART,
  image: 'vchart_bar_x.png'
}
