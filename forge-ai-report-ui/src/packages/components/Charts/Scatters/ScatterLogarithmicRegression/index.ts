import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index'

export const ScatterLogarithmicRegressionConfig: ConfigType = {
  key: 'ScatterLogarithmicRegression',
  chartKey: 'VScatterLogarithmicRegression',
  conKey: 'VCScatterLogarithmicRegression',
  title: '对数回归散点图',
  category: ChatCategoryEnum.SCATTER,
  categoryName: ChatCategoryEnumName.SCATTER,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.ECHARTS,
  image: 'scatter-logarithmic-regression.png'
}
