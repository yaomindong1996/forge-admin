import { onMounted, ref } from 'vue'
import { goDialog } from '@/utils'
import { DialogEnum } from '@/enums/pluginEnum'
import { ChartList } from '../../..'
import { deleteProjectApi, getProjectPageApi } from '@/api/project'

// 数据初始化
export const useDataListInit = () => {
  const list = ref<ChartList>([])
  const loading = ref(false)

  const loadList = async () => {
    try {
      loading.value = true
      const res = await getProjectPageApi({ pageNum: 1, pageSize: 100 })
      const records = res?.data?.records || []
      list.value = records.map(item => ({
        id: item.id,
        title: item.projectName,
        release: item.publishStatus === '1',
        label: item.remark || '项目',
        publishUrl: item.publishUrl,
        createTime: item.createTime,
        indexImg: item.indexImg,
      })) as ChartList
    } catch (error: any) {
      window.$message.error(error?.message || '获取项目列表失败')
    } finally {
      loading.value = false
    }
  }

  onMounted(loadList)

  // 删除
  const deleteHandle = (cardData: any, _index: number) => {
    goDialog({
      type: DialogEnum.DELETE,
      promise: true,
      onPositiveCallback: () => deleteProjectApi(cardData.id),
      promiseResCallback: async () => {
        window.$message.success('删除成功')
        await loadList()
      }
    })
  }

  return {
    list,
    loading,
    loadList,
    deleteHandle
  }
}
