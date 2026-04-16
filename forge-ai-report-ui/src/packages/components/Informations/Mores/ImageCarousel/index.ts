import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const ImageCarouselConfig: ConfigType = {
  key: 'ImageCarousel',
  chartKey: 'VImageCarousel',
  conKey: 'VCImageCarousel',
  title: '轮播图',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.INFORMATIONS,
  chartFrame: ChartFrameEnum.NAIVE_UI,
  image: 'photo_carousel.png'
}
