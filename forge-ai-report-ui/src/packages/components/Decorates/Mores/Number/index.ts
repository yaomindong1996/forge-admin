import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const NumberConfig: ConfigType = {
  key: 'Number',
  chartKey: 'VNumber',
  conKey: 'VCNumber',
  title: '数字计数',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'number.png'
}
