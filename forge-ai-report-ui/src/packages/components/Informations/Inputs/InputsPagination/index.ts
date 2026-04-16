import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const InputsPaginationConfig: ConfigType = {
    key: 'InputsPagination',
    chartKey: 'VInputsPagination',
    conKey: 'VCInputsPagination',
    title: '分页',
    category: ChatCategoryEnum.INPUTS,
    categoryName: ChatCategoryEnumName.INPUTS,
    package: PackagesCategoryEnum.INFORMATIONS,
    chartFrame: ChartFrameEnum.STATIC,
    image: 'inputs_pagination.png'
}
