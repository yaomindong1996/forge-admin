import { CreateComponentType, CreateComponentGroupType, FilterEnum } from '@/packages/index.d'
import { HistoryActionTypeEnum } from '@/store/modules/chartHistoryStore/chartHistoryStore.d'
import {
  RequestHttpEnum,
  RequestContentTypeEnum,
  RequestDataTypeEnum,
  RequestHttpIntervalEnum,
  RequestParams,
  RequestBodyEnum,
  RequestParamsObjType
} from '@/enums/httpEnum'
import { BaseEvent } from '@/enums/eventEnum'
import { PreviewScaleEnum } from '@/enums/styleEnum'
import type { ChartColorsNameType, CustomColorsType, GlobalThemeJsonType } from '@/settings/chartThemes/index'

// 编辑画布属性
export enum EditCanvasTypeEnum {
  EDIT_LAYOUT_DOM = 'editLayoutDom',
  EDIT_CONTENT_DOM = 'editContentDom',
  OFFSET = 'offset',
  SCALE = 'scale',
  USER_SCALE = 'userScale',
  LOCK_SCALE = 'lockScale',
  IS_CREATE = 'isCreate',
  IS_DRAG = 'isDrag',
  IS_SELECT = 'isSelect',
  IS_CODE_EDIT="isCodeEdit"
}

// 编辑区域
export type EditCanvasType = {
  // 编辑区域 DOM
  [EditCanvasTypeEnum.EDIT_LAYOUT_DOM]: HTMLElement | null
  [EditCanvasTypeEnum.EDIT_CONTENT_DOM]: HTMLElement | null
  // 偏移大小
  [EditCanvasTypeEnum.OFFSET]: number
  // 缩放
  [EditCanvasTypeEnum.SCALE]: number
  // 缩放
  [EditCanvasTypeEnum.USER_SCALE]: number
  // 锁定缩放
  [EditCanvasTypeEnum.LOCK_SCALE]: boolean
  // 初始化创建
  [EditCanvasTypeEnum.IS_CREATE]: boolean
  // 拖拽中
  [EditCanvasTypeEnum.IS_DRAG]: boolean
  // 框选中
  [EditCanvasTypeEnum.IS_SELECT]: boolean
  // 代码编辑中
  [EditCanvasTypeEnum.IS_CODE_EDIT]: boolean
}

// 滤镜/背景色/宽高主题等
export enum EditCanvasConfigEnum {
  PROJECT_NAME = 'projectName',
  WIDTH = 'width',
  HEIGHT = 'height',
  CHART_THEME_COLOR = 'chartThemeColor',
  CHART_CUSTOM_THEME_COLOR_INFO = 'chartCustomThemeColorInfo',
  CHART_THEME_SETTING = 'chartThemeSetting',
  VCHART_THEME_NAME = 'vChartThemeName',
  BACKGROUND = 'background',
  BACKGROUND_IMAGE = 'backgroundImage',
  SELECT_COLOR = 'selectColor',
  PREVIEW_SCALE_TYPE = 'previewScaleType'
}

export interface EditCanvasConfigType {
  // 滤镜-启用
  [FilterEnum.FILTERS_SHOW]: boolean
  // 滤镜-色相
  [FilterEnum.HUE_ROTATE]: number
  // 滤镜-饱和度
  [FilterEnum.SATURATE]: number
  // 滤镜-亮度
  [FilterEnum.BRIGHTNESS]: number
  // 滤镜-对比度
  [FilterEnum.CONTRAST]: number
  // 滤镜-不透明度
  [FilterEnum.OPACITY]: number
  // 变换（暂不使用）
  [FilterEnum.ROTATE_Z]: number
  [FilterEnum.ROTATE_X]: number
  [FilterEnum.ROTATE_Y]: number
  [FilterEnum.SKEW_X]: number
  [FilterEnum.SKEW_Y]: number
  [FilterEnum.BLEND_MODE]: string
  // 大屏名称
  [EditCanvasConfigEnum.PROJECT_NAME]?: string
  // 大屏宽度
  [EditCanvasConfigEnum.WIDTH]: number
  // 大屏高度
  [EditCanvasConfigEnum.HEIGHT]: number
  // 背景色
  [EditCanvasConfigEnum.BACKGROUND]?: string
  // 背景图片
  [EditCanvasConfigEnum.BACKGROUND_IMAGE]?: string | null
  // 图表主题颜色
  [EditCanvasConfigEnum.CHART_THEME_COLOR]: ChartColorsNameType
  // 自定义图表主题颜色
  [EditCanvasConfigEnum.CHART_CUSTOM_THEME_COLOR_INFO]?: CustomColorsType[] 
  // 图表全局配置
  [EditCanvasConfigEnum.CHART_THEME_SETTING]: GlobalThemeJsonType
  // 图表主题颜色
  [EditCanvasConfigEnum.SELECT_COLOR]: boolean,
  // vChart 主题
  [EditCanvasConfigEnum.VCHART_THEME_NAME]: string
  // 预览展示方式
  [EditCanvasConfigEnum.PREVIEW_SCALE_TYPE]: PreviewScaleEnum
}

// 坐标轴信息
// eslint-disable-next-line no-redeclare
export enum EditCanvasTypeEnum {
  START_X = 'startX',
  START_Y = 'startY',
  X = 'x',
  Y = 'y'
}

// 鼠标位置
export type MousePositionType = {
  // 开始 X
  [EditCanvasTypeEnum.START_X]: number
  // 开始 Y
  [EditCanvasTypeEnum.START_Y]: number
  // X
  [EditCanvasTypeEnum.X]: number
  // y
  [EditCanvasTypeEnum.Y]: number
}

// 操作目标
export type TargetChartType = {
  hoverId?: string
  selectId: string[]
}

// 数据记录
export type RecordChartType = {
  charts: CreateComponentType | CreateComponentGroupType | Array<CreateComponentType | CreateComponentGroupType>
  type: HistoryActionTypeEnum.CUT | HistoryActionTypeEnum.COPY
}

// Store 枚举
export enum ChartEditStoreEnum {
  EDIT_RANGE = 'editRange',
  EDIT_CANVAS = 'editCanvas',
  RIGHT_MENU_SHOW = 'rightMenuShow',
  MOUSE_POSITION = 'mousePosition',
  TARGET_CHART = 'targetChart',
  RECORD_CHART = 'recordChart',
  // 以下需要存储
  EDIT_CANVAS_CONFIG = 'editCanvasConfig',
  REQUEST_GLOBAL_CONFIG = 'requestGlobalConfig',
  COMPONENT_LIST = 'componentList'
}

// 请求公共类型
type RequestPublicConfigType = {
  // 时间单位（时分秒）
  requestIntervalUnit: RequestHttpIntervalEnum
  // 请求内容
  requestParams: RequestParams
}

// 数据池项类型
export type RequestDataPondItemType = {
  dataPondId: string,
  dataPondName: string,
  dataPondRequestConfig: RequestConfigType
}

export type DynamicRequestParamTarget = 'Params' | 'Header' | 'Body'

export type DynamicRequestParamSource = 'context' | 'component' | 'custom' | 'preset'

export interface DynamicRequestParamBinding {
  id: string
  enabled: boolean
  target: DynamicRequestParamTarget
  targetKey: string
  source: DynamicRequestParamSource
  sourceKey?: string
  componentId?: string
  componentField?: string
  customValue?: string | number | boolean | null
  presetType?: 'tn-day-start' | 'tn-day-end'
  offsetDays?: number
  dateFormat?: string
  fallbackValue?: string | number | boolean | null
}

export interface DatasetMappingConfig {
  // 组件数据适配模式，auto 会按组件类型和现有 dataset 结构自动转换
  mode?: 'auto' | 'echartsDataset' | 'arrayRows' | 'objectRows' | 'nameValue' | 'singleValue'
  // 组件字段到数据集字段的映射，例如 TableList: { name: 'dept_name', value: 'amount' }
  fieldMap?: Record<string, string>
  // 组件实际输出字段顺序，默认使用 datasetFields
  outputFields?: string[]
  // 表格类组件是否用数据集字段标签同步表头
  syncHeader?: boolean
}

// 全局的图表请求配置
export interface RequestGlobalConfigType extends RequestPublicConfigType {
  // 组件定制轮询时间
  requestInterval: number
  // 请求源地址
  requestOriginUrl?: string
  // 公共数据池
  requestDataPond: RequestDataPondItemType[]
}

// 单个图表请求配置
export interface RequestConfigType extends RequestPublicConfigType {
  // 所选全局数据池的对应 id
  requestDataPondId?: string
  // 组件定制轮询时间
  requestInterval?: number
  // 获取数据的方式
  requestDataType: RequestDataTypeEnum
  // 请求方式 get/post/del/put/patch
  requestHttpType: RequestHttpEnum
  // 源后续的 url
  requestUrl?: string
  // 请求内容主体方式 普通/sql
  requestContentType: RequestContentTypeEnum
  // 请求体类型
  requestParamsBodyType: RequestBodyEnum
  // SQL 请求对象
  requestSQLContent: {
    sql: string
  }
  // 接口来源（内部/外部）
  requestSource?: 'internal' | 'external'
  // 外部系统ID（当 requestSource 为 external 时）
  externalSystemId?: number | null
  // 外部接口ID（当 requestSource 为 external 时）
  externalApiId?: number | null
  // 外部请求参数配置
  externalRequestParams?: Record<string, any>
  // 动态请求参数配置
  dynamicRequestParams?: DynamicRequestParamBinding[]
  // 数据集ID（当 requestDataType 为 DATASET 时）
  datasetId?: number | null
  // 数据连接ID（仅用于 report-ui 配置联动）
  datasetConnectionId?: number | null
  // 数据集名称
  datasetName?: string
  // 数据集输出字段
  datasetFields?: string[]
  // 数据集查询参数
  datasetParams?: Record<string, any>
  // 数据集页码
  datasetPageNum?: number
  // 数据集每页条数
  datasetPageSize?: number
  // 数据集最大行数
  datasetMaxRows?: number
  // 数据集输出模式
  datasetOutputMode?: string
  // 数据集到组件数据结构的字段映射
  datasetMapping?: DatasetMappingConfig
}

// Store 类型
export interface ChartEditStoreType {
  [ChartEditStoreEnum.EDIT_CANVAS]: EditCanvasType
  [ChartEditStoreEnum.EDIT_CANVAS_CONFIG]: EditCanvasConfigType
  [ChartEditStoreEnum.RIGHT_MENU_SHOW]: boolean
  [ChartEditStoreEnum.MOUSE_POSITION]: MousePositionType
  [ChartEditStoreEnum.TARGET_CHART]: TargetChartType
  [ChartEditStoreEnum.RECORD_CHART]?: RecordChartType
  [ChartEditStoreEnum.REQUEST_GLOBAL_CONFIG]: RequestGlobalConfigType
  [ChartEditStoreEnum.COMPONENT_LIST]: Array<CreateComponentType | CreateComponentGroupType>
  projectPages: ReportCanvasPage[]
  activePageId: string
  homePageId: string
  pageTransition: ReportPageTransition
  runtimePageTransition: ReportPageTransition | ''
  runtimePageContext: Record<string, any>
  sharedRequestGlobalConfig: Partial<RequestGlobalConfigType>
}

// 存储数据类型
export interface ChartEditStorage {
  [ChartEditStoreEnum.EDIT_CANVAS_CONFIG]: EditCanvasConfigType
  [ChartEditStoreEnum.REQUEST_GLOBAL_CONFIG]: RequestGlobalConfigType
  [ChartEditStoreEnum.COMPONENT_LIST]: Array<CreateComponentType | CreateComponentGroupType>
}

export type ReportPageTransition = 'none' | 'fade' | 'slide-left' | 'slide-right' | 'zoom'

export type ComponentActionTrigger = BaseEvent.ON_CLICK | BaseEvent.ON_DBL_CLICK

export type ComponentActionType = 'goPage'

export type DrillParamSource = 'static' | 'componentField' | 'pageContext' | 'userContext'

export interface DrillParamBinding {
  id: string
  targetKey: string
  source: DrillParamSource
  sourceKey?: string
  value?: string | number | boolean | null
  fallbackValue?: string | number | boolean | null
}

export interface ComponentAction {
  id: string
  trigger: ComponentActionTrigger
  type: ComponentActionType
  targetPageId: string
  transition?: ReportPageTransition
  params?: DrillParamBinding[]
}

export interface ReportCanvasPage extends ChartEditStorage {
  id: string
  name: string
  sort: number
}

export interface ReportMultiPageStorage {
  version: 2
  homePageId: string
  activePageId: string
  pageTransition?: ReportPageTransition
  pages: ReportCanvasPage[]
  sharedRequestGlobalConfig?: Partial<RequestGlobalConfigType>
}

export type ReportProjectStorage = ChartEditStorage | ReportMultiPageStorage
