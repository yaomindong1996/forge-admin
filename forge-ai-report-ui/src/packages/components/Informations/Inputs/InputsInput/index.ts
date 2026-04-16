import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const InputsInputConfig: ConfigType = {
    key: 'InputsInput',
    chartKey: 'VInputsInput',
    conKey: 'VCInputsInput',
    title: '输入框',
    category: ChatCategoryEnum.INPUTS,
    categoryName: ChatCategoryEnumName.INPUTS,
    package: PackagesCategoryEnum.INFORMATIONS,
    chartFrame: ChartFrameEnum.STATIC,
    image: 'inputs_input.png'
}
