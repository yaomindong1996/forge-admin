import { useTabStore } from '@/store'

export const EXCLUDE_TAB = ['/404', '/403', '/login']

export function createTabGuard(router) {
  router.afterEach(async (to) => {
    if (EXCLUDE_TAB.includes(to.path))
      return
    const tabStore = useTabStore()
    const { name, fullPath: path } = to
    let title = to.meta?.title
    const icon = to.meta?.icon
    const keepAlive = to.meta?.keepAlive

    // 尝试从组件中读取 title
    if (to.matched.length > 0) {
      const component = to.matched[to.matched.length - 1].components?.default
      if (component) {
        try {
          // 如果组件是异步的，等待加载
          const resolvedComponent = typeof component === 'function' ? await component() : component
          const componentTitle = resolvedComponent?.default?.title || resolvedComponent?.title
          if (componentTitle) {
            title = componentTitle
          }
        } catch (e) {
          console.warn('无法读取组件 title:', e)
        }
      }
    }

    // 检查是否已存在相同path的tab，避免重复添加
    const existingTab = tabStore.tabs.find(item => item.path === path)
    if (!existingTab) {
      // 使用 path 作为 key，确保唯一性
      tabStore.addTab({ name, path, title, icon, keepAlive, key: path })
    }
    tabStore.setActiveTab(path)
  })
}
