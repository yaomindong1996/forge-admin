import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum,ChatCategoryEnumName } from '../../index'

export const TimeCommonConfig: ConfigType = {
  key: 'TimeCommon',
  chartKey: 'VTimeCommon',
  conKey: 'VCTimeCommon',
  title: '通用时间',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'time.png'
}
