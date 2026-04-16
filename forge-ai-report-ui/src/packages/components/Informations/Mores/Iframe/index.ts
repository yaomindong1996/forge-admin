import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum,ChatCategoryEnumName } from '../../index'

export const IframeConfig: ConfigType = {
  key: 'Iframe',
  chartKey: 'VIframe',
  conKey: 'VCIframe',
  title: '远程网页',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.INFORMATIONS,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'iframe.png'
}
