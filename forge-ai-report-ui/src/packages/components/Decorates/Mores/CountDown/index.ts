import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const CountDownConfig: ConfigType = {
  key: 'CountDown',
  chartKey: 'VCountDown',
  conKey: 'VCCountDown',
  title: '倒计时',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'countdown.png'
}
