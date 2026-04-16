import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const TablesBasicConfig: ConfigType = {
  key: 'TablesBasic',
  chartKey: 'VTablesBasic',
  conKey: 'VCTablesBasic',
  title: '基础分页表格',
  category: ChatCategoryEnum.TABLE,
  categoryName: ChatCategoryEnumName.TABLE,
  package: PackagesCategoryEnum.TABLES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'tables_basic.png'
}
