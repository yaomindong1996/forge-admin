import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMenuWorkspaceStore = defineStore('system-menu-workspace', () => {
  const detailVisible = ref(false)
  const filtersVisible = ref(false)
  function closePanels() {
    detailVisible.value = false
    filtersVisible.value = false
  }
  return { detailVisible, filtersVisible, closePanels }
})
