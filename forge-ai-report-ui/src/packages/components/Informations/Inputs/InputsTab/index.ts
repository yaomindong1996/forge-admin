import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const InputsTabConfig: ConfigType = {
  key: 'InputsTab',
  chartKey: 'VInputsTab',
  conKey: 'VCInputsTab',
  title: '标签选择器',
  category: ChatCategoryEnum.INPUTS,
  categoryName: ChatCategoryEnumName.INPUTS,
  package: PackagesCategoryEnum.INFORMATIONS,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'inputs_tab.png'
}
