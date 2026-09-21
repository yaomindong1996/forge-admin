import { defineStore } from 'pinia'
import { printWorkspaceSources } from '@/components/print/management/printWorkspaceSources'

export const usePrintWorkspaceStore = defineStore('printWorkspace', {
  state: () => ({ applicationId: null, sources: [], selectedKey: null }),
  getters: { source: state => state.sources.find(item => item.value === state.selectedKey)?.source || null },
  actions: {
    sync(application, objects) {
      const id = application?.id ? String(application.id) : null
      if (this.applicationId !== id)
        this.selectedKey = null
      this.applicationId = id
      this.sources = printWorkspaceSources(application, objects)
      if (!this.sources.some(item => item.value === this.selectedKey))
        this.selectedKey = this.sources.length === 1 ? this.sources[0].value : null
    },
    clear() { this.$reset() },
  },
})
