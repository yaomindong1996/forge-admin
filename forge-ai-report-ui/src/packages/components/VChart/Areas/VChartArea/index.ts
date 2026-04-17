import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

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
