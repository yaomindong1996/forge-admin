import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const ScatterCommonConfig: ConfigType = {
  key: 'ScatterCommon',
  chartKey: 'VScatterCommon',
  conKey: 'VCScatterCommon',
  title: '散点图',
  category: ChatCategoryEnum.SCATTER,
  categoryName: ChatCategoryEnumName.SCATTER,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.ECHARTS,
  image: 'scatter-multi.png'
}
