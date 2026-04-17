import { onUnmounted, onMounted } from 'vue'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { EditCanvasTypeEnum } from '@/store/modules/chartEditStore/chartEditStore.d'
import { getSessionStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { fetchRouteParamsLocation } from '@/utils'
import { getProjectDetailApi } from '@/api/project'
import { useSync } from '@/views/chart/hooks/useSync.hook'

const chartEditStore = useChartEditStore()

// 从 sessionStorage 或后端加载项目数据（在 DOM 就绪后调用）
async function loadProjectData() {
  // 已有组件数据（如 AI 生成流程直接跳转），跳过
  if (chartEditStore.getComponentList.length > 0) return

  const { updateComponent } = useSync()

  try {
    const id = fetchRouteParamsLocation()

    // 优先从 sessionStorage 读取
    const storageList = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) as any[]
    if (storageList && id) {
      const found = storageList.find((item: any) => String(item.id) === String(id))
      if (found && found.componentList && found.componentList.length > 0) {
        console.log('[useLayout] 从 SessionStorage 恢复', found.componentList.length, '个组件')
        await updateComponent(found, true)
        return
      }
    }

    // sessionStorage 没有，从后端拉取
    if (id) {
      const res = await getProjectDetailApi(String(id))
      const project = res?.data
      if (project?.componentData) {
        const parsed = JSON.parse(project.componentData)
        if (parsed.componentList && parsed.componentList.length > 0) {
          console.log('[useLayout] 从后端恢复', parsed.componentList.length, '个组件')
          await updateComponent(parsed, true)
        }
      }
    }
  } catch (e) {
    console.warn('[useLayout] 项目数据恢复失败:', e)
  }
}

// 布局处理
export const useLayout = (fn: () => Promise<void>) => {
  let removeScale: Function = () => { }
  onMounted(async () => {
    // 设置 Dom 值(ref 不生效先用 document)
    chartEditStore.setEditCanvas(
      EditCanvasTypeEnum.EDIT_LAYOUT_DOM,
      document.getElementById('go-chart-edit-layout')
    )
    chartEditStore.setEditCanvas(
      EditCanvasTypeEnum.EDIT_CONTENT_DOM,
      document.getElementById('go-chart-edit-content')
    )

    // 先从 sessionStorage / 后端加载项目数据（DOM 已就绪，computedScale 可以正确算缩放）
    await loadProjectData()

    // 获取数据（原有自定义回调，保持兼容）
    await fn()

    // 监听初始化（此时 componentList 已有数据，首次 listenerScale -> computedScale 能正确执行）
    removeScale = chartEditStore.listenerScale()
  })

  onUnmounted(() => {
    chartEditStore.setEditCanvas(EditCanvasTypeEnum.EDIT_LAYOUT_DOM, null)
    chartEditStore.setEditCanvas(EditCanvasTypeEnum.EDIT_CONTENT_DOM, null)
    removeScale()
  })
}
