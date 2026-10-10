import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { useAuthStore, useUserStore } from '@/store'
import { usePluginCenter } from '@/views/system/plugin/usePluginCenter'

/** 首页、左侧切换列表和详情标签共享状态；不持久化当前实例的插件信息。 */
export const usePluginCenterStore = defineStore('plugin-client-center', () => {
  const user = useUserStore()
  const auth = useAuthStore()
  const center = usePluginCenter()
  const selectedId = ref('')
  const detailTab = ref('introduction')
  const homeTab = ref('installed')
  const active = ref(false)
  const permitted = code => !!user.isAdmin && user.permissions.some(
    grant => ['**', '*:*:*', code].includes(grant),
  )
  const canList = computed(() => permitted('system:plugin:list'))
  const canDetail = computed(() => permitted('system:plugin:detail'))

  function clear() {
    center.clearList()
    center.closeDetail()
    selectedId.value = ''
    detailTab.value = 'introduction'
    homeTab.value = 'installed'
  }

  function open(id, tab = 'introduction') {
    if (!canDetail.value || !id)
      return
    selectedId.value = id
    detailTab.value = tab
    return center.openDetail(id)
  }

  function back() {
    center.closeDetail()
    selectedId.value = ''
  }

  function activate() {
    if (active.value)
      return
    active.value = true
    if (canList.value)
      return center.load()
  }

  function deactivate() {
    active.value = false
    clear()
  }

  // 注销、切租户及撤回权限时同步丢弃旧响应，不能沿用上一个身份的清单。
  watch(() => [user.userId, user.tenantId, auth.accessToken, canList.value, canDetail.value], () => {
    clear()
    center.query.keyword = ''
    center.query.origin = null
    center.query.pageNum = 1
    if (active.value && canList.value)
      center.load()
  }, { flush: 'sync' })

  return { ...center, selectedId, detailTab, homeTab, canList, canDetail, open, back, activate, deactivate }
})
