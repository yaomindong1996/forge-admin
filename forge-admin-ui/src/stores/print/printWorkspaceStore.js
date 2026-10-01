import { defineStore } from 'pinia'
import { printWorkspaceSources } from '@/components/print/management/printWorkspaceSources'

export const usePrintWorkspaceStore = defineStore('printWorkspace', {
  state: () => ({ applicationId: null, sources: [], selectedKey: null }),
  getters: { source: state => state.sources.find(item => item.value === state.selectedKey)?.source || null },
  actions: {
    sync(application, objects, pageId) {
      const id = application?.id ? String(application.id) : null
      const scopedPageId = String(pageId || '').trim()
      const firstLoad = this.applicationId == null
      if (this.applicationId !== id)
        this.selectedKey = null
      this.applicationId = id
      this.sources = printWorkspaceSources(application, objects)
        .filter(item => !scopedPageId || item.source.pageId === scopedPageId)
      if (!this.sources.some(item => item.value === this.selectedKey)) {
        // 首次进入可默认第一项；来源删除或切换应用后不静默改选其它业务表单。
        this.selectedKey = firstLoad ? this.sources[0]?.value || null : null
      }
    },
    clear() { this.$reset() },
  },
})
