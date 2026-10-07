import { ref } from 'vue'
import { getRuntimePlugin } from '@/api/system/plugin'
import { useLatestPluginRequest } from './useLatestPluginRequest'

async function fetchDetail(id) {
  const response = await getRuntimePlugin(id)
  if (response?.code !== 200 || !response.data?.id)
    throw new Error(response?.message || '插件详情响应无效')
  return response.data
}

export function usePluginDetail() {
  const request = useLatestPluginRequest(fetchDetail, true)
  const detailVisible = ref(false)
  let selectedId = ''

  function openDetail(id = selectedId) {
    selectedId = id
    detailVisible.value = true
    return request.run(id)
  }

  function closeDetail() {
    request.cancel()
    detailVisible.value = false
  }

  const { data: detail, loading: detailLoading, error: detailError } = request
  return { detail, detailVisible, detailLoading, detailError, openDetail, closeDetail }
}
