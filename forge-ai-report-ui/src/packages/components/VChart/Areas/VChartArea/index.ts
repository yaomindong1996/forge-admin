import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const VChartAreaConfig: ConfigType = {
  key: 'VChartArea',
  chartKey: 'VVChartArea',
  conKey: 'VCVChartArea',
  title: 'VChart面积图',
  category: ChatCategoryEnum.AREA,
  categoryName: ChatCategoryEnumName.AREA,
  package: PackagesCategoryEnum.VCHART,
  chartFrame: ChartFrameEnum.VCHART,
  image: 'vchart_area.png'
}
