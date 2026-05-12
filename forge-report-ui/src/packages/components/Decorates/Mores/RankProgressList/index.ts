import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const RankProgressListConfig: ConfigType = {
  key: 'RankProgressList',
  chartKey: 'VRankProgressList',
  conKey: 'VCRankProgressList',
  title: '排行进度',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'jindu.png'
}
