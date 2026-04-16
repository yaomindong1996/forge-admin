import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const ThreeEarth01Config: ConfigType = {
  key: 'ThreeEarth01',
  chartKey: 'VThreeEarth01',
  conKey: 'VCThreeEarth01',
  title: '三维地球',
  category: ChatCategoryEnum.THREE,
  categoryName: ChatCategoryEnumName.THREE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'threeEarth01.png'
}
