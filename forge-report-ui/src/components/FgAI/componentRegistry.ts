import { packagesList } from '@/packages'
import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'

// 组件描述信息
export interface ComponentDescriptor {
  key: string
  title: string
  package: PackagesCategoryEnum
  category: string
  categoryName: string
  chartFrame?: ChartFrameEnum
  redirectComponent?: string
  defaultW: number
  defaultH: number
  configType: ConfigType
}

// 各组件真实默认尺寸及用途说明（用于 AI 生成 prompt）
interface AIComponentInfo {
  defaultW: number
  defaultH: number
  description: string
}

const aiComponentInfoMap: Record<string, AIComponentInfo> = {
  // ====== 图表 ECharts ======
  BarCommon: { defaultW: 500, defaultH: 300, description: '柱状图，适合对比分类数据、月度/季度趋势' },
  BarCrossrange: { defaultW: 500, defaultH: 300, description: '横向柱状图，适合排名展示、长标签类目' },
  BarLine: { defaultW: 500, defaultH: 300, description: '柱线混合图，适合同时展示趋势和对比' },
  CapsuleChart: { defaultW: 500, defaultH: 300, description: '胶囊柱图，适合分类排名展示' },
  LineCommon: { defaultW: 500, defaultH: 300, description: '折线图，适合展示时间趋势变化' },
  LineGradientSingle: { defaultW: 500, defaultH: 300, description: '单折线渐变面积图，适合单一指标趋势' },
  LineGradients: { defaultW: 500, defaultH: 300, description: '双折线渐变面积图，适合两组数据对比趋势' },
  LineLinearSingle: { defaultW: 500, defaultH: 300, description: '单折线渐变图' },
  PieCommon: { defaultW: 500, defaultH: 300, description: '饼图，适合展示占比分布' },
  PieCircle: { defaultW: 500, defaultH: 300, description: '环形饼图，适合展示占比分布且有中心文字空间' },
  ScatterCommon: { defaultW: 500, defaultH: 300, description: '散点图，适合展示数据分布和相关性' },
  ScatterLogarithmicRegression: { defaultW: 500, defaultH: 300, description: '对数回归散点图' },
  MapBase: { defaultW: 800, defaultH: 600, description: '地图(可选省份)，适合地理数据展示，建议 w≥750 h≥600' },
  MapAmap: { defaultW: 1000, defaultH: 600, description: '高德地图，适合精确地理坐标展示，建议 w≥1000 h≥600' },
  Dial: { defaultW: 400, defaultH: 300, description: '表盘，适合展示仪表盘类指标' },
  Funnel: { defaultW: 400, defaultH: 350, description: '漏斗图，适合展示转化率、流程递减数据' },
  Graph: { defaultW: 500, defaultH: 400, description: '关系图，适合展示网络拓扑、人物关系' },
  Heatmap: { defaultW: 500, defaultH: 300, description: '热力图，适合展示密度分布' },
  Process: { defaultW: 400, defaultH: 200, description: '进度条，适合展示任务完成进度' },
  Radar: { defaultW: 400, defaultH: 350, description: '雷达图，使用特殊 dataset 格式' },
  Sankey: { defaultW: 500, defaultH: 400, description: '桑基图，适合展示流向和流量' },
  TreeMap: { defaultW: 500, defaultH: 350, description: '树形分布图，适合层级数据展示' },
  WaterPolo: { defaultW: 400, defaultH: 300, description: '水球图，适合展示完成率百分比' },

  // ====== VChart ======
  VChartArea: { defaultW: 500, defaultH: 300, description: 'VChart面积图' },
  VChartPercentArea: { defaultW: 500, defaultH: 300, description: 'VChart百分比面积图' },
  VChartBarCommon: { defaultW: 500, defaultH: 300, description: 'VChart并列柱状图' },
  VChartBarCrossrange: { defaultW: 500, defaultH: 300, description: 'VChart横向柱状图' },
  VChartBarStack: { defaultW: 500, defaultH: 300, description: 'VChart堆叠柱状图' },
  VChartFunnel: { defaultW: 400, defaultH: 350, description: 'VChart漏斗图' },
  VChartLine: { defaultW: 500, defaultH: 300, description: 'VChart折线图' },
  VChartPie: { defaultW: 500, defaultH: 300, description: 'VChart饼图' },
  VChartScatter: { defaultW: 500, defaultH: 300, description: 'VChart散点图' },
  VChartWordCloud: { defaultW: 500, defaultH: 350, description: 'VChart词云图' },

  // ====== 信息组件 ======
  TextCommon: { defaultW: 500, defaultH: 50, description: '文本，适合标题(w=600-1920,h=50-80)、标签、段落' },
  TextGradient: { defaultW: 400, defaultH: 60, description: '渐变文字，适合大标题、关键数字' },
  TextBarrage: { defaultW: 500, defaultH: 70, description: '弹幕文字，适合滚动信息展示' },
  InputsDate: { defaultW: 260, defaultH: 32, description: '时间选择器' },
  InputsInput: { defaultW: 260, defaultH: 32, description: '输入框' },
  InputsPagination: { defaultW: 395, defaultH: 32, description: '分页组件' },
  InputsSelect: { defaultW: 260, defaultH: 32, description: '下拉选择器' },
  InputsTab: { defaultW: 460, defaultH: 32, description: '标签选择器' },
  Iframe: { defaultW: 1200, defaultH: 800, description: '远程网页嵌入' },
  Image: { defaultW: 500, defaultH: 300, description: '图片展示' },
  ImageCarousel: { defaultW: 500, defaultH: 300, description: '轮播图' },
  Video: { defaultW: 500, defaultH: 300, description: '视频播放' },
  WordCloud: { defaultW: 500, defaultH: 350, description: '词云图(ECharts版)' },

  // ====== 表格 ======
  TableList: { defaultW: 400, defaultH: 250, description: '列表/排行榜，适合 Top N 排名展示' },
  TableScrollBoard: { defaultW: 500, defaultH: 250, description: '轮播表格，适合多行数据滚动展示' },
  TablesBasic: { defaultW: 600, defaultH: 300, description: '基础分页表格' },

  // ====== 装饰边框 ======
  Border01: { defaultW: 500, defaultH: 300, description: '装饰边框01，需包裹在图表外层，w/h 应与内部图表一致' },
  Border02: { defaultW: 500, defaultH: 300, description: '装饰边框02，需包裹在图表外层，w/h 应与内部图表一致' },
  Border03: { defaultW: 500, defaultH: 300, description: '装饰边框03，需包裹在图表外层，w/h 应与内部图表一致' },
  Border04: { defaultW: 500, defaultH: 300, description: '装饰边框04，需包裹在图表外层，w/h 应与内部图表一致' },
  Border05: { defaultW: 500, defaultH: 300, description: '装饰边框05，需包裹在图表外层，w/h 应与内部图表一致' },
  Border06: { defaultW: 500, defaultH: 300, description: '装饰边框06，需包裹在图表外层，w/h 应与内部图表一致' },
  Border07: { defaultW: 500, defaultH: 300, description: '装饰边框07，需包裹在图表外层，w/h 应与内部图表一致' },
  Border08: { defaultW: 500, defaultH: 300, description: '装饰边框08，需包裹在图表外层，w/h 应与内部图表一致' },
  Border09: { defaultW: 500, defaultH: 300, description: '装饰边框09，需包裹在图表外层，w/h 应与内部图表一致' },
  Border10: { defaultW: 500, defaultH: 300, description: '装饰边框10，需包裹在图表外层，w/h 应与内部图表一致' },
  Border11: { defaultW: 500, defaultH: 300, description: '装饰边框11，需包裹在图表外层，w/h 应与内部图表一致' },
  Border12: { defaultW: 500, defaultH: 300, description: '装饰边框12，需包裹在图表外层，w/h 应与内部图表一致' },
  Border13: { defaultW: 500, defaultH: 300, description: '装饰边框13，需包裹在图表外层，w/h 应与内部图表一致' },

  // ====== 装饰元素 ======
  Decorates01: { defaultW: 500, defaultH: 300, description: '装饰元素01' },
  Decorates02: { defaultW: 500, defaultH: 300, description: '装饰元素02' },
  Decorates03: { defaultW: 500, defaultH: 70, description: '装饰元素03（横条），适合顶部或分隔线' },
  Decorates04: { defaultW: 500, defaultH: 300, description: '装饰元素04' },
  Decorates05: { defaultW: 500, defaultH: 300, description: '装饰元素05' },
  Decorates06: { defaultW: 500, defaultH: 70, description: '装饰元素06（横条），适合顶部或分隔线' },

  // ====== 小组件 ======
  ScreenTitle: { defaultW: 900, defaultH: 86, description: '组合大屏标题，内置中间标题、左右装饰、背景和边框；主标题优先使用它，不要再用多个装饰组件拼标题' },
  ScreenTitle02: { defaultW: 900, defaultH: 86, description: '第二套组合大屏标题，偏光幕科技风，适合智慧城市、园区、工厂总览标题' },
  ScreenTitle03: { defaultW: 960, defaultH: 90, description: '星环标题，带环形光晕和动态扫光，适合全域态势大屏' },
  ScreenTitle04: { defaultW: 960, defaultH: 90, description: '锋刃标题，黄橙工业科技风，适合工厂生产大屏' },
  ScreenTitle05: { defaultW: 1000, defaultH: 92, description: '双翼标题，左右屏幕边框装饰更明显，适合城市/园区中心标题' },
  ScreenTitle06: { defaultW: 960, defaultH: 90, description: '脉冲标题，动态发光边框，适合能源、监控、告警类大屏' },
  ScreenTitle07: { defaultW: 960, defaultH: 90, description: '晶体标题，粉紫晶体切面风格，适合安全态势类大屏' },
  ScreenTitle08: { defaultW: 1000, defaultH: 92, description: '中枢标题，控制台轨道和发光节点结构，适合数字孪生指挥中心' },
  KpiCard: { defaultW: 320, defaultH: 96, description: '完整指标卡，包含指标名、数值、单位、趋势和装饰边框；顶部核心指标优先使用它' },
  KpiGroup: { defaultW: 900, defaultH: 112, description: '指标组，顶部 4-6 个核心指标时优先使用它，不要拆成多个 KpiCard；dataset 使用 [{title,value,unit,trend}]' },
  MetricCompareCard: { defaultW: 360, defaultH: 150, description: '对比指标卡，适合展示主指标及同比/环比；compareItems 使用 [{label,value,type,unit}]' },
  MiniTrendCard: { defaultW: 320, defaultH: 140, description: '迷你趋势卡，适合指标值加小折线趋势；points 使用数字数组' },
  ProgressRing: { defaultW: 180, defaultH: 180, description: '环形进度卡，适合完成率、达成率、占用率等单指标百分比' },
  StatusBadgeList: { defaultW: 520, defaultH: 78, description: '状态标签组，适合运行/待机/告警等状态数量；dataset 使用 [{label,value,color}]' },
  DataPairList: { defaultW: 420, defaultH: 160, description: '键值信息列表，适合设备详情、项目概况、企业信息；dataset 使用 [{label,value}]' },
  SectionHeader: { defaultW: 460, defaultH: 46, description: '模块标题条，适合图表内部小标题、单位、英文副标题，不需要完整模块框' },
  TimelineList: { defaultW: 440, defaultH: 280, description: '时间线列表，适合实时告警、事件记录、工单进展；dataset 使用 [{time,title,level,status}]' },
  StepFlow: { defaultW: 560, defaultH: 92, description: '步骤流程条，适合生产流程、审批流程、项目阶段；dataset 使用 [{title,status}]' },
  DividerLine: { defaultW: 420, defaultH: 26, description: '发光分割线，适合模块内部或区域之间分隔，可横向或纵向' },
  GlowBackdrop: { defaultW: 720, defaultH: 420, description: 'SVG 风格发光背景，含 reactor/grid/wing/stargate/radar 类型；深色大屏优先使用它增强科技炫酷氛围，zIndex 建议较低且不要套模块框' },
  PanelFrame: { defaultW: 506, defaultH: 306, description: '统一模块框，包含模块标题、角标、背景和边框；放在图表/表格/地图前面作为模块背景' },
  PanelFrame02: { defaultW: 506, defaultH: 306, description: '第二套统一模块框，棱角切边风格；放在图表/表格/地图前面作为模块背景' },
  PanelFrame03: { defaultW: 506, defaultH: 306, description: '扫描模块框，带纵向扫描光效，适合实时监控模块背景' },
  PanelFrame04: { defaultW: 506, defaultH: 306, description: '网格模块框，带科技网格底纹，适合设备、点位、区域分析' },
  PanelFrame05: { defaultW: 506, defaultH: 306, description: '辉光模块框，边缘发光更强，适合核心图表模块' },
  PanelFrame06: { defaultW: 506, defaultH: 306, description: '重角模块框，厚重角标，适合工业和能源大屏' },
  PanelFrame07: { defaultW: 506, defaultH: 306, description: '切角模块框，斜切边框，适合安全态势和指挥舱' },
  PanelFrame08: { defaultW: 506, defaultH: 306, description: '卡片模块框，圆角卡片式科技框，适合指标明细和列表' },
  RankProgressList: { defaultW: 420, defaultH: 280, description: '排行进度列表，适合 Top N、设备排名、区域排行、产线效率排行' },
  CirclePoint: { defaultW: 97, defaultH: 97, description: '圆点光环，适合装饰点缀' },
  Clock: { defaultW: 350, defaultH: 250, description: '时钟，不需要 option，适合顶部信息栏' },
  CountDown: { defaultW: 300, defaultH: 100, description: '倒计时，不需要 option，适合活动倒计时' },
  FlipperNumber: { defaultW: 300, defaultH: 100, description: '数字翻牌器，适合关键指标数字展示' },
  FullScreen: { defaultW: 150, defaultH: 150, description: '全屏按钮' },
  Number: { defaultW: 300, defaultH: 100, description: '数字计数' },
  PipelineH: { defaultW: 500, defaultH: 15, description: '管道-横向，适合分隔线' },
  PipelineV: { defaultW: 15, defaultH: 500, description: '管道-纵向，适合分隔列' },
  TimeCommon: { defaultW: 300, defaultH: 50, description: '通用时间显示' },

  // ====== 其他 ======
  FlowChartLine: { defaultW: 500, defaultH: 300, description: '流程线，适合流程图连接' },
  ThreeEarth01: { defaultW: 800, defaultH: 600, description: '三维地球，适合全球数据展示，建议 w≥800 h≥600' },
  Icon: { defaultW: 64, defaultH: 64, description: '图标' },
}

// 从 packagesList 构建扁平化的 key → descriptor 映射
let registryCache: Map<string, ComponentDescriptor> | null = null

export function getComponentRegistry(): Map<string, ComponentDescriptor> {
  if (registryCache) return registryCache

  registryCache = new Map()
  const packages: PackagesCategoryEnum[] = [
    PackagesCategoryEnum.CHARTS,
    PackagesCategoryEnum.VCHART,
    PackagesCategoryEnum.INFORMATIONS,
    PackagesCategoryEnum.TABLES,
    PackagesCategoryEnum.DECORATES,
    PackagesCategoryEnum.PHOTOS,
    PackagesCategoryEnum.ICONS
  ]

  packages.forEach(pkg => {
    const list = packagesList[pkg]
    if (!list) return
    list.forEach((item: ConfigType) => {
      if (item.disabled) return
      const info = aiComponentInfoMap[item.key]
      registryCache!.set(item.key, {
        key: item.key,
        title: item.title,
        package: item.package,
        category: item.category,
        categoryName: item.categoryName,
        chartFrame: item.chartFrame,
        redirectComponent: item.redirectComponent,
        defaultW: info ? info.defaultW : chartInitConfig.w,
        defaultH: info ? info.defaultH : chartInitConfig.h,
        configType: item
      })
    })
  })

  return registryCache
}

// 根据 key 查找 ConfigType
export function findConfigTypeByKey(key: string): ConfigType | undefined {
  const registry = getComponentRegistry()
  return registry.get(key)?.configType
}

// 获取组件目录文本（可附在 AI 请求中供后端参考）
export function getComponentCatalogText(): string {
  const registry = getComponentRegistry()
  const lines: string[] = []

  const categories: { pkg: PackagesCategoryEnum; label: string }[] = [
    { pkg: PackagesCategoryEnum.CHARTS, label: '图表组件（ECharts）' },
    { pkg: PackagesCategoryEnum.VCHART, label: 'VChart图表' },
    { pkg: PackagesCategoryEnum.INFORMATIONS, label: '信息展示' },
    { pkg: PackagesCategoryEnum.TABLES, label: '表格组件' },
    { pkg: PackagesCategoryEnum.DECORATES, label: '装饰/小组件' },
    { pkg: PackagesCategoryEnum.PHOTOS, label: '图片组件' },
    { pkg: PackagesCategoryEnum.ICONS, label: '图标' },
  ]

  categories.forEach(({ pkg, label }) => {
    const items: ComponentDescriptor[] = []
    registry.forEach(desc => {
      if (desc.package === pkg) items.push(desc)
    })
    if (items.length === 0) return
    lines.push(`\n[${label}]`)
    items.forEach(item => {
      const info = aiComponentInfoMap[item.key]
      const sizeInfo = info ? `${info.defaultW}×${info.defaultH}` : `${item.defaultW}×${item.defaultH}`
      const descPart = info?.description ? ` - ${info.description}` : ''
      lines.push(`  - ${item.key}: ${item.title} (${sizeInfo})${descPart}`)
    })
  })

  return lines.join('\n')
}

// 获取所有可用的组件 key 列表
export function getAvailableComponentKeys(): string[] {
  const registry = getComponentRegistry()
  return Array.from(registry.keys())
}
