import { useAppStore } from '@/store'

export function createPageLoadingGuard(router) {
  router.beforeEach(() => {
    $loadingBar.start()
  })

  router.afterEach(() => {
    setTimeout(() => {
      $loadingBar.finish()
      // 确保路由守卫完成状态被设置
      const appStore = useAppStore()
      if (!appStore.routeGuardCompleted) {
        console.log('在页面加载守卫中设置路由守卫完成状态')
        appStore.setRouteGuardCompleted(true)
      }
    }, 200)
  })

  router.onError(() => {
    $loadingBar.error()
    // 发生错误时也要确保路由守卫完成状态被设置
    const appStore = useAppStore()
    if (!appStore.routeGuardCompleted) {
      console.log('在页面加载错误中设置路由守卫完成状态')
      appStore.setRouteGuardCompleted(true)
    }
  })
}
