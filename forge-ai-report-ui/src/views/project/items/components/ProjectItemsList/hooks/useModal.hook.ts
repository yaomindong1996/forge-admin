import { ref } from 'vue'
import { ChartEnum } from '@/enums/pageEnum'
import { fetchPathByName, getSessionStorage, routerTurnByPath, setSessionStorage } from '@/utils'
import { Chartype } from '../../../index'
import { getProjectDetailApi } from '@/api/project'
import { StorageEnum } from '@/enums/storageEnum'

export const useModalDataInit = (reloadList?: () => Promise<void>) => {
  const modalShow = ref<boolean>(false)
  const modalData = ref<Chartype | null>(null)

  // 关闭 modal
  const closeModal = () => {
    modalShow.value = false
    modalData.value = null
  }

  // 打开 modal
  const resizeHandle = (cardData: Chartype) => {
    if (!cardData) return
    modalShow.value = true
    modalData.value = cardData
  }

  // 打开编辑器并从后端加载项目数据到 sessionStorage
  const editHandle = async (cardData: Chartype) => {
    if (!cardData) return
    try {
      const res = await getProjectDetailApi(cardData.id)
      const project = res?.data
      if (!project?.componentData) {
        window.$message.warning('该项目暂无可编辑内容')
        return
      }

      const parsed = JSON.parse(project.componentData)
      const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []
      const repeatIndex = sessionStorageInfo.findIndex((e: { id: string | number }) => String(e.id) === String(cardData.id))
      const saveData = { ...parsed, id: String(cardData.id) }

      if (repeatIndex !== -1) {
        sessionStorageInfo.splice(repeatIndex, 1, saveData)
      } else {
        sessionStorageInfo.push(saveData)
      }
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)

      const path = fetchPathByName(ChartEnum.CHART_HOME_NAME, 'href')
      routerTurnByPath(path, [String(cardData.id)], undefined, true)
      closeModal()
      if (reloadList) await reloadList()
    } catch (error: any) {
      window.$message.error(error?.message || '加载项目详情失败')
    }
  }

  return {
    modalData,
    modalShow,
    closeModal,
    resizeHandle,
    editHandle
  }
}
