import { finishGlobalLoading, startGlobalLoading } from '@/composables/useGlobalLoading'
import { useAppStore } from '@/store'

function isAppPortalRoute(route) {
  return route?.meta?.layout === 'app-portal'
}

export function createPageLoadingGuard(router) {
  let routeLoadingToken = null

  function startRouteLoading() {
    if (routeLoadingToken)
      return

    const appStore = useAppStore()
    if (appStore.routeGuardCompleted !== true)
      return

    routeLoadingToken = startGlobalLoading({
      globalLoadingType: 'route',
      globalLoadingText: '页面加载中，请稍候...',
      globalLoadingDelay: 120,
    })
  }

  function finishRouteLoading() {
    finishGlobalLoading(routeLoadingToken)
    routeLoadingToken = null
  }

  function finishRouteChrome() {
    finishRouteLoading()
    $loadingBar.finish()
    const appStore = useAppStore()
    if (!appStore.routeGuardCompleted)
      appStore.setRouteGuardCompleted(true)
  }

  router.beforeEach((to) => {
    if (isAppPortalRoute(to)) {
      finishRouteChrome()
      return
    }
    startRouteLoading()
    $loadingBar.start()
  })

  router.afterEach((to) => {
    if (isAppPortalRoute(to)) {
      finishRouteChrome()
      return
    }
    setTimeout(() => {
      finishRouteChrome()
    }, 200)
  })

  router.onError(() => {
    finishRouteLoading()
    $loadingBar.error()
    const appStore = useAppStore()
    if (!appStore.routeGuardCompleted)
      appStore.setRouteGuardCompleted(true)
  })
}
