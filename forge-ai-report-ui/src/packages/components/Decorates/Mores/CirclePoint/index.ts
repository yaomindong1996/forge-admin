import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum,ChatCategoryEnumName } from '../../index'

export const CirclePointConfig: ConfigType = {
    key: 'CirclePoint',
    chartKey: 'VCirclePoint',
    conKey: 'VCCirclePoint',
    title: '圆点光环',
    category: ChatCategoryEnum.MORE,
    categoryName: ChatCategoryEnumName.MORE,
    package: PackagesCategoryEnum.DECORATES,
    chartFrame: ChartFrameEnum.STATIC,
    image: 'flow-circle.png'
}
