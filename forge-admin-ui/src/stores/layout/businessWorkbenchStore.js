import { defineStore } from 'pinia'

export const useBusinessWorkbenchStore = defineStore('business-workbench', {
  state: () => ({
    activeMenu: null,
    pinRoot: null,
    holdPin: false,
  }),
  actions: {
    openMenu(key) {
      this.activeMenu = key
      this.pinRoot = key
      this.holdPin = false
    },
    prepareNavigation() {
      this.pinRoot = this.activeMenu || this.pinRoot
      this.holdPin = true
    },
    closePanel() {
      this.activeMenu = null
    },
    closeMenus() {
      this.activeMenu = null
      this.pinRoot = null
      this.holdPin = false
    },
    onRouteChange() {
      this.activeMenu = null
      if (!this.holdPin)
        this.pinRoot = null
    },
    syncPin(rootKey) {
      if (this.pinRoot && rootKey && String(this.pinRoot) === String(rootKey)) {
        this.pinRoot = null
        this.holdPin = false
      }
    },
  },
})
