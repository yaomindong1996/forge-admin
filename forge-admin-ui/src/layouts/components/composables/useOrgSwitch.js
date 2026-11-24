import { computed, h, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { useAuthStore, useUserStore } from '@/store'

function flattenOrgOptions(list = [], parentNames = []) {
  return (list || []).flatMap((item) => {
    const pathNames = [...parentNames, item.orgName || item.name || String(item.id)]
    const current = {
      ...item,
      pathLabel: pathNames.join(' / '),
    }
    const children = flattenOrgOptions(item.children || [], pathNames)
    return [current, ...children]
  })
}

/**
 * 组织切换：顶栏不再常驻展示，账户菜单 / 紧凑面板复用同一套逻辑。
 */
export function useOrgSwitch() {
  const router = useRouter()
  const userStore = useUserStore()
  const authStore = useAuthStore()

  const orgOptions = ref([])
  const switchingOrg = ref(false)

  const currentOrgId = computed(() => userStore.activeOrgId || userStore.userInfo?.activeOrgId)
  const flatOrgOptions = computed(() => flattenOrgOptions(orgOptions.value))
  const displayOrgOptions = computed(() => {
    if (flatOrgOptions.value.length > 0)
      return flatOrgOptions.value
    if (!currentOrgId.value)
      return []
    return [{
      id: currentOrgId.value,
      orgName: userStore.activeOrgName || userStore.userInfo?.activeOrgName || '当前组织',
    }]
  })
  const switchableOrgCount = computed(() => displayOrgOptions.value.length)
  const canSwitchOrg = computed(() => switchableOrgCount.value > 1)
  const currentOrgName = computed(() => {
    const current = displayOrgOptions.value.find(item => Number(item.id) === Number(currentOrgId.value))
    return current?.orgName || userStore.activeOrgName || userStore.userInfo?.activeOrgName || '未选择组织'
  })

  const orgMenuChildren = computed(() => displayOrgOptions.value.map(item => ({
    label: item.pathLabel || item.orgName || `组织 ${item.id}`,
    key: `org:${item.id}`,
    disabled: switchingOrg.value || Number(item.id) === Number(currentOrgId.value),
    icon: () => h('i', {
      class: Number(item.id) === Number(currentOrgId.value)
        ? 'i-material-symbols:check-circle-rounded text-14 text-success'
        : 'i-material-symbols:account-tree-rounded text-14',
    }),
  })))

  /** 供 NDropdown 作为二级菜单（级联）挂载 */
  const orgSwitchMenuOption = computed(() => {
    if (!canSwitchOrg.value)
      return null
    return {
      label: `切换组织（${currentOrgName.value}）`,
      key: 'org-switch',
      icon: () => h('i', { class: 'i-material-symbols:account-tree-rounded text-14' }),
      children: orgMenuChildren.value,
    }
  })

  async function loadOrgOptions() {
    if (!userStore.userInfo)
      return
    try {
      const res = await api.getCurrentOrgOptions()
      if (res.code === 200)
        orgOptions.value = res.data || []
    }
    catch (error) {
      console.error('load org options error', error)
    }
  }

  async function switchOrg(orgId) {
    if (!orgId || Number(orgId) === Number(currentOrgId.value))
      return false
    try {
      switchingOrg.value = true
      const res = await api.switchOrg(orgId)
      if (res.code === 200) {
        authStore.resetLoginState({ resetAuth: false })
        window.$message?.success('当前组织已切换')
        await router.replace({ path: '/', query: { orgSwitch: Date.now() } })
        return true
      }
    }
    catch {
      window.$message?.error('切换组织失败')
    }
    finally {
      switchingOrg.value = false
    }
    return false
  }

  function handleOrgMenuSelect(key) {
    if (!key?.startsWith('org:'))
      return false
    void switchOrg(Number(key.replace('org:', '')))
    return true
  }

  watch(() => [userStore.userInfo?.tenantId, userStore.userInfo?.activeOrgId], () => {
    loadOrgOptions()
  }, { immediate: true })

  return {
    switchingOrg,
    canSwitchOrg,
    currentOrgId,
    currentOrgName,
    displayOrgOptions,
    orgMenuChildren,
    orgSwitchMenuOption,
    loadOrgOptions,
    switchOrg,
    handleOrgMenuSelect,
  }
}
