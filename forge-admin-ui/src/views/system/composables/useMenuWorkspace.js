import { computed, onBeforeUnmount, watch } from 'vue'
import { useMenuWorkspaceStore } from '@/stores/system/menuWorkspaceStore'
import { getResourceBreadcrumbs } from '../menu-interaction-utils'

export function useMenuWorkspace(api) {
  const workspace = useMenuWorkspaceStore()
  const contextBreadcrumbs = computed(() => getResourceBreadcrumbs(api.flatResources.value, api.currentNode.value))
  const activeFilterCount = computed(() => [api.resourceTypeFilter.value, api.visibleFilter.value]
    .filter(value => value !== null && value !== undefined)
    .length)
  const hasSearch = computed(() => !!api.resourceKeyword.value.trim() || activeFilterCount.value > 0)

  function openResourceDetail(row) {
    api.selectedRow.value = row
    workspace.detailVisible = true
  }
  function enterResource(row) {
    api.handleNavigationSelect([row?.id ?? 0])
    api.expandResourcePath(api.currentNode.value)
    workspace.closePanels()
  }
  function handleResourceName(row) {
    if ([1, 2].includes(Number(row.resourceType))) {
      enterResource(row)
      return
    }
    openResourceDetail(row)
  }
  function resetResourceFilters() {
    api.resourceTypeFilter.value = null
    api.visibleFilter.value = null
    api.resourceKeyword.value = ''
  }
  function editResourceFromDetail(row) {
    workspace.detailVisible = false
    api.handleEdit(row)
  }
  function addResourceFromDetail(row) {
    workspace.detailVisible = false
    api.handleAdd(row)
  }

  // 客户端和选中资源变化后不能留下指向旧资源的浮层。
  watch(api.currentClientCode, workspace.closePanels)
  watch(api.activeResource, (row) => {
    if (!row) {
      workspace.detailVisible = false
    }
  })
  onBeforeUnmount(workspace.closePanels)
  return {
    workspace,
    contextBreadcrumbs,
    activeFilterCount,
    hasSearch,
    openResourceDetail,
    enterResource,
    handleResourceName,
    resetResourceFilters,
    editResourceFromDetail,
    addResourceFromDetail,
  }
}
