import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const CapsuleChartConfig: ConfigType = {
  key: 'CapsuleChart',
  chartKey: 'VCapsuleChart',
  conKey: 'VCCapsuleChart',
  title: '胶囊柱图',
  category: ChatCategoryEnum.BAR,
  categoryName: ChatCategoryEnumName.BAR,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'capsule.png'
}
