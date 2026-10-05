import { computed, watch } from 'vue'
import { useMenu } from '@/composables/useMenu'
import { usePermissionStore } from '@/store'
import { useBusinessWorkbenchStore } from '@/stores/layout/businessWorkbenchStore'
import { buildMegaSections, buildWorkbenchMenus, findWorkbenchTrail } from './menu-model'

export function useWorkbenchNavigation() {
  const permissionStore = usePermissionStore()
  const store = useBusinessWorkbenchStore()
  const { activeKey, handleMenuSelect } = useMenu()
  const menus = computed(() => buildWorkbenchMenus(permissionStore.menus))
  const trail = computed(() => findWorkbenchTrail(menus.value, activeKey.value))
  const activeRootKey = computed(() => trail.value[0]?.key)
  const highlightedRootKey = computed(() => store.activeMenu || store.pinRoot || activeRootKey.value)
  const expandedMenu = computed(() => menus.value.find(item => item.key === store.activeMenu))
  const sections = computed(() => buildMegaSections(expandedMenu.value))

  watch(activeRootKey, (key) => {
    store.syncPin(key)
  })

  function navigate(item) {
    store.prepareNavigation()
    handleMenuSelect(item.key, item.path)
    store.closePanel()
  }

  function activateMenu(item) {
    if (!item.children.length) {
      navigate(item)
      return
    }
    if (store.activeMenu === item.key)
      store.closeMenus()
    else
      store.openMenu(item.key)
  }

  return {
    menus,
    trail,
    activeKey,
    activeRootKey,
    highlightedRootKey,
    expandedMenu,
    sections,
    navigate,
    activateMenu,
  }
}
