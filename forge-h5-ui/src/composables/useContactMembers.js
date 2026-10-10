import { ref } from 'vue'
import api from '@/api'
import { CONTACT_PAGE_SIZE, normalizeContactPage } from '@/utils/contacts'

/**
 * 通讯录成员分页加载；buildParams 返回 null 时视为不查询（例如关键字为空）
 */
export function useContactMembers(buildParams) {
  const members = ref([])
  const total = ref(0)
  const loading = ref(false)
  const finished = ref(false)
  const failed = ref(false)
  let pageNum = 1
  let requestSeq = 0

  async function loadPage(reset) {
    const params = buildParams()
    if (!params) {
      members.value = []
      total.value = 0
      finished.value = true
      return
    }
    if (reset) {
      pageNum = 1
      finished.value = false
    }
    if (!reset && (loading.value || finished.value)) return
    // 搜索词快速变化时，只保留最后一次请求的结果
    const seq = ++requestSeq
    loading.value = true
    failed.value = false
    try {
      const res = await api.getContactMembers({ ...params, pageNum, pageSize: CONTACT_PAGE_SIZE })
      if (seq !== requestSeq) return
      const page = normalizeContactPage(res?.data)
      members.value = reset ? page.records : [...members.value, ...page.records]
      total.value = page.total
      finished.value = members.value.length >= page.total || page.records.length < CONTACT_PAGE_SIZE
      pageNum += 1
    }
    catch (error) {
      if (seq !== requestSeq) return
      console.error('加载通讯录成员失败:', error)
      failed.value = true
    }
    finally {
      if (seq === requestSeq) loading.value = false
    }
  }

  return {
    members,
    total,
    loading,
    finished,
    failed,
    reload: () => loadPage(true),
    loadMore: () => loadPage(false),
  }
}
