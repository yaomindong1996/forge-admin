// AI 生成大屏 - 请求类型
export interface AIGenerateRequest {
  // 用户需求描述
  prompt: string
  // 主题风格
  style?: 'dark' | 'light'
  // 画布宽度
  canvasWidth?: number
  // 画布高度
  canvasHeight?: number
  // 可用组件目录文本，供后端填充提示词模板
  componentCatalog?: string
  // 当前项目名称
  projectName?: string
  // 当前画布上下文，供 AI 在原有基础上增量修改
  canvasContext?: string
  // 指定供应商
  providerId?: number | string
  // 指定模型
  modelName?: string
  // 温度参数
  temperature?: number
  // 最大 token 数
  maxTokens?: number
}

// AI 生成大屏 - 组件 Schema
export interface AIComponentSchema {
  // 组件 key，如 "BarCommon"、"TextCommon"
  key: string
  // 左上角 x
  x: number
  // 左上角 y
  y: number
  // 宽度
  w: number
  // 高度
  h: number
  // 图表配置/数据（ECharts option 或组件 option）
  option?: Record<string, any>
  // 数据集（用于 redirectComponent 类组件）
  dataset?: any
  // 自定义标题（覆盖默认）
  title?: string
}

// AI 生成大屏 - 响应类型
export interface AIGenerateResponse {
  // 大屏标题
  title: string
  // 画布配置（项目页入口时使用）
  canvasConfig?: {
    width: number
    height: number
    background?: string
    chartThemeColor?: string
  }
  // 组件列表
  components: AIComponentSchema[]
}
