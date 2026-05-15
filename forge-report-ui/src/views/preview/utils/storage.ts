import { getSessionStorage, getLocalStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { getProjectDetailApi } from '@/api/project'
import { normalizeProjectStorage, resolveInitialPreviewPage } from '@/utils/reportPages'
import type { ChartEditStorage, ReportPageTransition, ReportProjectStorage } from '@/store/modules/chartEditStore/chartEditStore.d'

const chartEditStore = useChartEditStore()

export interface ChartEditStorageType extends ChartEditStorage {
  id: string
}

const getHashInfo = () => {
  const [path, query = ''] = document.location.hash.split('?')
  const toPathArray = path.split('/')
  return {
    id: (toPathArray && toPathArray[toPathArray.length - 1]) || '',
    pageId: new URLSearchParams(query).get('pageId') || undefined
  }
}

const applyStorage = (storage: ReportProjectStorage, id: string, pageId?: string) => {
  const project = normalizeProjectStorage(storage)
  const initialPageId = resolveInitialPreviewPage(project, pageId)
  const pageStorage = chartEditStore.loadProjectStorage(project, initialPageId)
  return { ...pageStorage, id }
}

export const switchPreviewPage = async (
  pageId: string,
  context: Record<string, any> = {},
  transition?: ReportPageTransition
) => {
  chartEditStore.setRuntimePageTransition(transition || '')
  chartEditStore.setRuntimePageContext(context)
  const nextStorage = chartEditStore.switchPage(pageId)

  if (!nextStorage && chartEditStore.getHomePageId) {
    chartEditStore.switchPage(chartEditStore.getHomePageId)
  }
}

// 根据路由 id 获取存储数据的信息
// 优先从 sessionStorage 读取，如果没有，尝试从 localStorage 读取，最后从后端读取
export const getSessionStorageInfo = async () => {
  const { id, pageId } = getHashInfo()

  const sessionList: Array<ChartEditStorageType | (ReportProjectStorage & { id: string })> = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST)
  if (sessionList) {
    for (let i = 0; i < sessionList.length; i++) {
      if (id.toString() === String(sessionList[i].id)) {
        return applyStorage(sessionList[i], String(id), pageId)
      }
    }
  }

  const localList: Array<ChartEditStorageType | (ReportProjectStorage & { id: string })> = getLocalStorage(StorageEnum.GO_CHART_STORAGE_LIST)
  if (localList) {
    for (let i = 0; i < localList.length; i++) {
      if (id.toString() === String(localList[i].id)) {
        return applyStorage(localList[i], String(id), pageId)
      }
    }
  }

  const res = await getProjectDetailApi(id)
  const project = res?.data
  if (project?.componentData) {
    const parsed = JSON.parse(project.componentData)
    return applyStorage(parsed, String(id), pageId)
  }

  return null
}
