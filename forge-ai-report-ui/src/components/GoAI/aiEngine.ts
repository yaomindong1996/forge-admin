import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { createComponent, fetchChartComponent, fetchConfigComponent } from '@/packages/index'
import { ConfigType, CreateComponentType, ChartFrameEnum } from '@/packages/index.d'
import { componentInstall, loadingStart, loadingFinish, loadingError } from '@/utils'
import { findConfigTypeByKey } from './componentRegistry'
import { autoLayout } from './layoutAlgorithm'
import { AIGenerateResponse, AIComponentSchema } from '@/api/ai/ai'
import merge from 'lodash/merge'
import cloneDeep from 'lodash/cloneDeep'

/**
 * 根据 dataset dimensions 数量自动调整 series 数组
 * ECharts 要求 series 数量 = dimensions 列数 - 1（第一列是类目轴）
 */
function adjustSeriesForDataset(option: any, dataset: any) {
  if (!dataset?.dimensions || !Array.isArray(dataset.dimensions)) return
  if (!option.series || !Array.isArray(option.series)) return

  const dataColumns = dataset.dimensions.length - 1 // 减去第一列类目名
  if (dataColumns <= 0) return

  const currentCount = option.series.length
  if (currentCount === dataColumns) return

  if (currentCount < dataColumns) {
    // series 不够，复制最后一个 series 补齐
    const template = cloneDeep(option.series[option.series.length - 1])
    for (let i = currentCount; i < dataColumns; i++) {
      const newSeries = cloneDeep(template)
      option.series.push(newSeries)
    }
  } else if (currentCount > dataColumns) {
    // series 太多，截断
    option.series = option.series.slice(0, dataColumns)
  }
}

/**
 * 将 AI 返回的 option 智能合并到组件实例
 */
function applyOption(instance: CreateComponentType, comp: AIComponentSchema, configType: ConfigType) {
  const aiOption = comp.option
  if (!aiOption) return

  const chartFrame = configType.chartFrame

  if (chartFrame === ChartFrameEnum.ECHARTS) {
    // ECharts 组件：覆盖 dataset，自动调整 series 数量匹配新数据
    if (aiOption.dataset !== undefined) {
      instance.option.dataset = aiOption.dataset
      adjustSeriesForDataset(instance.option, aiOption.dataset)
    }
    // 饼图特有字段
    if (aiOption.type !== undefined) instance.option.type = aiOption.type
    if (aiOption.isCarousel !== undefined) instance.option.isCarousel = aiOption.isCarousel
  } else if (chartFrame === ChartFrameEnum.VCHART) {
    // VChart 组件：覆盖 dataset
    if (aiOption.dataset !== undefined) {
      instance.option.dataset = aiOption.dataset
    }
  } else if (chartFrame === ChartFrameEnum.COMMON) {
    // common 组件（Radar, CapsuleChart, TextCommon, FlipperNumber, TableList 等）
    // 这些组件各自有不同的 option 结构，逐字段覆盖
    Object.keys(aiOption).forEach(key => {
      instance.option[key] = aiOption[key]
    })
  } else {
    // static / naiveUI / undefined — 逐字段覆盖
    Object.keys(aiOption).forEach(key => {
      instance.option[key] = aiOption[key]
    })
  }

  // redirectComponent 类型组件的 dataset 单独覆盖
  if (comp.dataset && configType.redirectComponent) {
    instance.option.dataset = comp.dataset
  }
}

/**
 * 将单个 AIComponentSchema 应用为画布组件
 */
async function createAIComponent(comp: AIComponentSchema): Promise<CreateComponentType | null> {
  const configType = findConfigTypeByKey(comp.key)
  if (!configType) {
    console.warn(`[AI Engine] 未找到组件: "${comp.key}"，跳过`)
    return null
  }

  // 注册 Vue 组件（和 ChartsItemBox 的双击添加逻辑完全一致）
  const chartComp = fetchChartComponent(configType)
  const configComp = fetchConfigComponent(configType)
  if (chartComp) componentInstall(configType.chartKey, chartComp)
  if (configComp) componentInstall(configType.conKey, configComp)

  // 创建组件实例
  try {
    const instance = await createComponent(configType)

    // 应用位置和尺寸
    instance.attr.x = comp.x ?? 50
    instance.attr.y = comp.y ?? 50
    instance.attr.w = comp.w ?? 500
    instance.attr.h = comp.h ?? 300

    // 智能合并 option
    applyOption(instance, comp, configType)

    // redirectComponent 处理（和 ChartsItemBox 一致）
    if (configType.redirectComponent) {
      comp.dataset && (instance.option.dataset = comp.dataset)
      if (comp.title) instance.chartConfig.title = comp.title
      if (comp.chartFrame) instance.chartConfig.chartFrame = comp.chartFrame
    } else if (comp.title) {
      instance.chartConfig.title = comp.title
    }

    console.log(`[AI Engine] 组件创建成功: ${comp.key} (${configType.chartFrame}), id=${instance.id}`)
    return instance
  } catch (error) {
    console.warn(`[AI Engine] 创建组件失败: ${comp.key}`, error)
    return null
  }
}

/**
 * 将 AI 生成结果应用到画布
 */
export async function applyAIToCanvas(
  response: AIGenerateResponse,
  replaceMode: boolean = true
): Promise<void> {
  const chartEditStore = useChartEditStore()

  console.log('[AI Engine] 开始应用, replaceMode=', replaceMode, ', components=', response.components?.length)
  console.log('[AI Engine] 组件列表:', response.components?.map(c => `${c.key}(option=${!!c.option})`))

  loadingStart()

  try {
    // 替换模式：清空现有组件
    if (replaceMode) {
      chartEditStore.componentList.splice(0, chartEditStore.componentList.length)
    }

    // 应用画布配置
    if (response.canvasConfig && replaceMode) {
      if (response.canvasConfig.width) {
        chartEditStore.setEditCanvasConfig('width', response.canvasConfig.width)
      }
      if (response.canvasConfig.height) {
        chartEditStore.setEditCanvasConfig('height', response.canvasConfig.height)
      }
      if (response.canvasConfig.background) {
        chartEditStore.setEditCanvasConfig('background', response.canvasConfig.background)
        chartEditStore.setEditCanvasConfig('selectColor', true)
      }
      if (response.canvasConfig.chartThemeColor) {
        chartEditStore.setEditCanvasConfig('chartThemeColor', response.canvasConfig.chartThemeColor)
      }
    }

    if (response.title && replaceMode) {
      chartEditStore.setEditCanvasConfig('projectName', response.title)
    }

    // 检查是否需要自动布局
    const canvasW = chartEditStore.getEditCanvasConfig.width
    const canvasH = chartEditStore.getEditCanvasConfig.height
    const needsLayout = response.components.some(
      c => c.x === undefined || c.y === undefined || (c.x === 0 && c.y === 0)
    )
    let components = response.components
    if (needsLayout) {
      components = autoLayout(components, canvasW, canvasH)
    }

    // 逐个创建组件并添加到画布
    let successCount = 0
    for (const comp of components) {
      const instance = await createAIComponent(comp)
      if (instance) {
        chartEditStore.addComponentList(instance, false, true)
        successCount++
      }
    }

    console.log('[AI Engine] 完成: ', successCount, '/', components.length, ' 成功, store.componentList.length=', chartEditStore.getComponentList.length)

    // 重新计算缩放（仅在编辑器已挂载时，项目页跳转场景由编辑器自行处理）
    if (chartEditStore.getEditCanvas.editLayoutDom) {
      chartEditStore.computedScale()
    }

    loadingFinish()
  } catch (error) {
    console.error('[AI Engine] 失败:', error)
    loadingError()
    throw error
  }
}
