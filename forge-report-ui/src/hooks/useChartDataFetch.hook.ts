import { ref, toRefs, toRaw, watch } from 'vue'
import type VChart from 'vue-echarts'
import { customizeHttp } from '@/api/http'
import { useChartDataPondFetch } from '@/hooks/'
import { CreateComponentType, ChartFrameEnum } from '@/packages/index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { RequestDataTypeEnum } from '@/enums/httpEnum'
import { isPreview, newFunctionHandle, intervalUnitHandle, normalizeDatasetForChart } from '@/utils'
import { setOption } from '@/packages/public/chart'
import { isNil } from 'lodash'

// 获取类型
type ChartEditStoreType = typeof useChartEditStore

/**
 * setdata 数据监听与更改
 * @param targetComponent
 * @param useChartEditStore 若直接引会报错，只能动态传递
 * @param updateCallback 自定义更新函数
 */
export const useChartDataFetch = (
  targetComponent: CreateComponentType,
  useChartEditStore: ChartEditStoreType,
  updateCallback?: (...args: any) => any
) => {
  const vChartRef = ref<typeof VChart | null>(null)
  let fetchInterval: any = 0

  // 数据池
  const { addGlobalDataInterface } = useChartDataPondFetch()

  // 组件类型
  const { chartFrame } = targetComponent.chartConfig

  // eCharts 组件配合 vChart 库更新方式
  const echartsUpdateHandle = (dataset: any) => {
    if (chartFrame === ChartFrameEnum.ECHARTS) {
      if (vChartRef.value) {
        setOption(vChartRef.value, { dataset: dataset }, false)
      }
    }
  }

  const requestIntervalFn = () => {
    const chartEditStore = useChartEditStore()

    // 全局数据
    const {
      requestOriginUrl,
      requestIntervalUnit: globalUnit,
      requestInterval: globalRequestInterval
    } = toRefs(chartEditStore.getRequestGlobalConfig)

    // 目标组件
    const {
      requestDataType,
      requestUrl,
      requestSource,
      externalApiId,
      requestIntervalUnit: targetUnit,
      requestInterval: targetInterval
    } = toRefs(targetComponent.request)

    // 非请求类型
    if (requestDataType.value !== RequestDataTypeEnum.AJAX) return

    try {
      const isExternalRequest = requestSource?.value === 'external'
      const canFetchInternal = !isExternalRequest && requestUrl?.value
      const canFetchExternal = isExternalRequest && externalApiId?.value

      if (canFetchInternal || canFetchExternal) {
        // requestOriginUrl 允许为空
        const completePath = requestOriginUrl && requestOriginUrl.value + requestUrl.value
        if (canFetchInternal && !completePath) return

        clearInterval(fetchInterval)

        const fetchFn = async () => {
          const res = await customizeHttp(toRaw(targetComponent.request), toRaw(chartEditStore.getRequestGlobalConfig))
          if (res) {
            try {
              const filter = targetComponent.filter
              const { data } = res
              const nextDataset = normalizeDatasetForChart(newFunctionHandle(data, res, filter), chartFrame)
              echartsUpdateHandle(nextDataset)
              // 更新回调函数
              if (updateCallback) {
                updateCallback(nextDataset)
              }
            } catch (error) {
              console.error(error)
            }
          }
        }

        // 普通初始化与组件交互处理监听
        watch(
          () => [
            targetComponent.request.requestParams,
            targetComponent.request.externalApiId,
            targetComponent.request.externalRequestParams
          ],
          () => {
            fetchFn()
          },
          {
            immediate: true,
            deep: true
          }
        )

        // 定时时间
        const time = targetInterval && !isNil(targetInterval.value) ? targetInterval.value : globalRequestInterval.value
        // 单位
        const unit = targetInterval && !isNil(targetInterval.value) ? targetUnit.value : globalUnit.value
        // 开启轮询
        if (time) {
          fetchInterval = setInterval(fetchFn, intervalUnitHandle(time, unit))
        }
      }
      // eslint-disable-next-line no-empty
    } catch (error) {
      console.log(error)
    }
  }

  if (isPreview()) {
    targetComponent.request.requestDataType === RequestDataTypeEnum.Pond
      ? addGlobalDataInterface(targetComponent, useChartEditStore, (newData: any) => {
          echartsUpdateHandle(newData)
          if (updateCallback) updateCallback(newData)
        })
      : requestIntervalFn()
  } else {
    requestIntervalFn()
  }
  return { vChartRef }
}
