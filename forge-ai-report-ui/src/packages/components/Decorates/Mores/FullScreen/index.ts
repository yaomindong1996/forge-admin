import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const FullScreenConfig: ConfigType = {
  key: 'FullScreen',
  chartKey: 'VFullScreen',
  conKey: 'VCFullScreen',
  title: '全屏按钮',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'fullScreen.png'
}
