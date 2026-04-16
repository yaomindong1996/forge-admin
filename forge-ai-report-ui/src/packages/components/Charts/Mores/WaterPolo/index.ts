import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const WaterPoloConfig: ConfigType = {
  key: 'WaterPolo',
  chartKey: 'VWaterPolo',
  conKey: 'VCWaterPolo',
  title: '水球图',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'water_WaterPolo.png'
}
