import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const LineCommonConfig: ConfigType = {
  key: 'LineCommon',
  chartKey: 'VLineCommon',
  conKey: 'VCLineCommon',
  title: '折线图',
  category: ChatCategoryEnum.LINE,
  categoryName: ChatCategoryEnumName.LINE,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.ECHARTS,
  image: 'line.png'
}
