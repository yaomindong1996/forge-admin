import { defineStore } from 'pinia'
import { nextTick } from 'vue'
import { useRouterStore } from '@/store'
import { getSessionStorage, setSessionStorage } from '@/utils'

const TABS_KEY = `${import.meta.env.VITE_TENANT || 'default'}_tabs`

export const useTabStore = defineStore('tab', {
  state: () => ({
    tabs: getSessionStorage(TABS_KEY) || [],
    activeTab: '',
    reloading: false, // 添加reloading状态用于页面刷新
    // 添加缓存视图列表
    cacheViews: [],
  }),
  getters: {
    activeTabPath() {
      return this.tabs.find(item => item.key === this.activeTab)?.path || '/'
    },
  },
  actions: {
    setTabs(tabs) {
      this.tabs = tabs
      setSessionStorage(TABS_KEY, tabs)
    },
    removeTabCache(tab) {
      if (!tab?.path)
        return
      const cacheName = tab.path.substring(1).replace(/\//g, '-').replace(/\?.*/, '')
      const cacheIndex = this.cacheViews.indexOf(cacheName)
      if (cacheIndex > -1) {
        this.cacheViews.splice(cacheIndex, 1)
      }
    },
    // 修改 addTab 方法，添加缓存视图处理
    addTab(tab) {
      if (this.tabs.some(item => item.key === tab.key))
        return
      this.tabs.push(tab)
      setSessionStorage(TABS_KEY, this.tabs)

      // 添加缓存视图
      if (tab.path) {
        // 转换路径为缓存名称格式，例如 /system/user -> system-user
        const cacheName = tab.path.substring(1).replace(/\//g, '-').replace(/\?.*/, '')
        if (!this.cacheViews.includes(cacheName)) {
          this.cacheViews.push(cacheName)
        }
      }
    },
    updateTabTitle(path, title) {
      if (!path || !title)
        return
      const tab = this.tabs.find(item => item.path === path || item.key === path)
      if (!tab || tab.title === title)
        return
      tab.title = title
      setSessionStorage(TABS_KEY, this.tabs)
    },
    updateTabMeta(path, patch = {}) {
      if (!path || !patch || !Object.keys(patch).length)
        return
      const tab = this.tabs.find(item => item.path === path || item.key === path)
      if (!tab)
        return
      Object.assign(tab, patch)
      setSessionStorage(TABS_KEY, this.tabs)
    },
    // 修改 removeTab 方法，删除对应的缓存视图
    removeTab(key) {
      const index = this.tabs.findIndex(item => item.key === key)
      if (index === -1)
        return
      const isLast = index === this.tabs.length - 1

      // 删除对应的缓存视图
      const tab = this.tabs[index]
      this.removeTabCache(tab)

      this.tabs.splice(index, 1)
      setSessionStorage(TABS_KEY, this.tabs)
      if (key !== this.activeTab)
        return
      const newTab = this.tabs[index] || this.tabs[index - 1] || { path: '/' }
      useRouterStore().router?.push(newTab.path)
      this.setActiveTab(isLast ? (newTab.key || newTab.path) : this.tabs[index]?.key || newTab.key || newTab.path)
    },
    removeTabSilently(keyOrPath) {
      if (!keyOrPath)
        return
      const removedActive = this.tabs.some(item => (item.key === keyOrPath || item.path === keyOrPath) && item.key === this.activeTab)
      const nextTabs = []
      this.tabs.forEach((tab) => {
        if (tab.key === keyOrPath || tab.path === keyOrPath) {
          this.removeTabCache(tab)
          return
        }
        nextTabs.push(tab)
      })
      if (nextTabs.length === this.tabs.length)
        return
      this.setTabs(nextTabs)
      if (removedActive)
        this.activeTab = nextTabs[nextTabs.length - 1]?.key || ''
    },
    setActiveTab(key) {
      this.activeTab = key
    },
    removeOther(curPath) {
      // 删除其他标签时，也需要更新缓存视图列表
      const filterTabs = this.tabs.filter(item => item.path === curPath)

      // 更新缓存视图列表
      const newCacheViews = []
      filterTabs.forEach((tab) => {
        if (tab.path) {
          const cacheName = tab.path.substring(1).replace(/\//g, '-').replace(/\?.*/, '')
          if (!newCacheViews.includes(cacheName)) {
            newCacheViews.push(cacheName)
          }
        }
      })
      this.cacheViews = newCacheViews

      this.setTabs(filterTabs)
      if (!filterTabs.find(item => item.path === this.activeTab)) {
        useRouterStore().router?.push(filterTabs[filterTabs.length - 1].path)
      }
    },
    removeLeft(curPath) {
      const curIndex = this.tabs.findIndex(item => item.path === curPath)
      const filterTabs = this.tabs.filter((item, index) => index >= curIndex)

      // 更新缓存视图列表
      const newCacheViews = []
      filterTabs.forEach((tab) => {
        if (tab.path) {
          const cacheName = tab.path.substring(1).replace(/\//g, '-').replace(/\?.*/, '')
          if (!newCacheViews.includes(cacheName)) {
            newCacheViews.push(cacheName)
          }
        }
      })
      this.cacheViews = newCacheViews

      this.setTabs(filterTabs)
      if (!filterTabs.find(item => item.path === this.activeTab)) {
        useRouterStore().router?.push(filterTabs[filterTabs.length - 1].path)
      }
    },
    removeRight(curPath) {
      const curIndex = this.tabs.findIndex(item => item.path === curPath)
      const filterTabs = this.tabs.filter((item, index) => index <= curIndex)

      // 更新缓存视图列表
      const newCacheViews = []
      filterTabs.forEach((tab) => {
        if (tab.path) {
          const cacheName = tab.path.substring(1).replace(/\//g, '-').replace(/\?.*/, '')
          if (!newCacheViews.includes(cacheName)) {
            newCacheViews.push(cacheName)
          }
        }
      })
      this.cacheViews = newCacheViews

      this.setTabs(filterTabs)
      if (!filterTabs.find(item => item.path === this.activeTab.value)) {
        useRouterStore().router?.push(filterTabs[filterTabs.length - 1].path)
      }
    },
    removeAll() {
      // 清空所有标签
      this.setTabs([])
      // 清空缓存视图列表
      this.cacheViews = []
      // 跳转到首页
      useRouterStore().router?.push('/')
    },
    // 添加reloadTab方法
    async reloadTab(path, keepAlive) {
      // 设置reloading状态为true
      this.reloading = true

      // 如果是keepAlive页面，先移除再添加
      if (keepAlive) {
        const tab = this.tabs.find(item => item.path === path)
        if (tab) {
          // 临时移除keepAlive属性
          tab.keepAlive = false
          // 触发重新渲染
          await nextTick()
          // 恢复keepAlive属性
          tab.keepAlive = true
        }
      }

      // 触发重新渲染
      await nextTick()

      // 重置reloading状态
      this.reloading = false
    },
    resetTabs() {
      this.$reset()
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_tab`,
    pick: ['tabs'],
    storage: sessionStorage,
  },
})
