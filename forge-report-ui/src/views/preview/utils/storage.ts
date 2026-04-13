import { getSessionStorage, getLocalStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { ChartEditStorage } from '@/store/modules/chartEditStore/chartEditStore.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'

const chartEditStore = useChartEditStore()

export interface ChartEditStorageType extends ChartEditStorage {
  id: string
}

// 根据路由 id 获取存储数据的信息
// 优先从 sessionStorage 读取，如果没有，尝试从 localStorage 读取（支持发布分享）
export const getSessionStorageInfo = () => {
  const urlHash = document.location.hash
  const toPathArray = urlHash.split('/')
  const id = toPathArray && toPathArray[toPathArray.length - 1]

  // 先尝试 sessionStorage
  const sessionList: ChartEditStorageType[] = getSessionStorage(
    StorageEnum.GO_CHART_STORAGE_LIST
  )
  if (sessionList) {
    for (let i = 0; i < sessionList.length; i++) {
      if (id.toString() === sessionList[i]['id']) {
        const { editCanvasConfig, requestGlobalConfig, componentList } = sessionList[i]
        chartEditStore.editCanvasConfig = editCanvasConfig
        chartEditStore.requestGlobalConfig = requestGlobalConfig
        chartEditStore.componentList = componentList
        return sessionList[i]
      }
    }
  }

  // 如果 sessionStorage 没有，尝试从 localStorage 读取（发布功能保存的数据）
  const localList: ChartEditStorageType[] = getLocalStorage(
    StorageEnum.GO_CHART_STORAGE_LIST
  )
  if (localList) {
    for (let i = 0; i < localList.length; i++) {
      if (id.toString() === localList[i]['id']) {
        const { editCanvasConfig, requestGlobalConfig, componentList } = localList[i]
        chartEditStore.editCanvasConfig = editCanvasConfig
        chartEditStore.requestGlobalConfig = requestGlobalConfig
        chartEditStore.componentList = componentList
        return localList[i]
      }
    }
  }
}