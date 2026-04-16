import { getSessionStorage, getLocalStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { ChartEditStorage } from '@/store/modules/chartEditStore/chartEditStore'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { getProjectDetailApi } from '@/api/project'

const chartEditStore = useChartEditStore()

export interface ChartEditStorageType extends ChartEditStorage {
  id: string
}

const applyStorage = (storage: ChartEditStorageType) => {
  const { editCanvasConfig, requestGlobalConfig, componentList } = storage
  chartEditStore.editCanvasConfig = editCanvasConfig
  chartEditStore.requestGlobalConfig = requestGlobalConfig
  chartEditStore.componentList = componentList
  return storage
}

// 根据路由 id 获取存储数据的信息
// 优先从 sessionStorage 读取，如果没有，尝试从 localStorage 读取，最后从后端读取
export const getSessionStorageInfo = async () => {
  const urlHash = document.location.hash
  const toPathArray = urlHash.split('/')
  const id = toPathArray && toPathArray[toPathArray.length - 1]

  const sessionList: ChartEditStorageType[] = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST)
  if (sessionList) {
    for (let i = 0; i < sessionList.length; i++) {
      if (id.toString() === String(sessionList[i].id)) {
        return applyStorage(sessionList[i])
      }
    }
  }

  const localList: ChartEditStorageType[] = getLocalStorage(StorageEnum.GO_CHART_STORAGE_LIST)
  if (localList) {
    for (let i = 0; i < localList.length; i++) {
      if (id.toString() === String(localList[i].id)) {
        return applyStorage(localList[i])
      }
    }
  }

  const res = await getProjectDetailApi(id)
  const project = res?.data
  if (project?.componentData) {
    const parsed = JSON.parse(project.componentData)
    return applyStorage({ ...parsed, id: String(id) })
  }

  return null
}
