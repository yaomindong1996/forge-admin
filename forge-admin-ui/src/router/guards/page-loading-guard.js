import { finishGlobalLoading, startGlobalLoading } from '@/composables/useGlobalLoading'
import { pendingShellLayout, resolvePendingShellLayout } from '@/router/shell-layout'
import { useAppStore } from '@/store'

function isAppPortalRoute(route) {
  return route?.meta?.layout === 'app-portal'
}

function isApplicationRuntimeRoute(route) {
  if (route?.name === 'BusinessApplicationRuntime')
    return true
  const path = String(route?.path || '')
  return /^\/app-center\/application\/[^/]+\/runtime(?:\/|$)/.test(path)
}

function shouldSkipRouteChrome(route) {
  // 这些页自己画骨架，全局进度条/蒙层只会叠出「白屏感」
  return isAppPortalRoute(route) || isApplicationRuntimeRoute(route) || route?.meta?.layout === 'empty'
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

  router.beforeEach((to, from) => {
    // 进入设计态/空壳页时立刻锁布局，避免系统侧栏空内容闪一帧
    pendingShellLayout.value = resolvePendingShellLayout(to)

    if (shouldSkipRouteChrome(to)) {
      finishRouteChrome()
      return
    }
    // 同页仅改 query（筛选/分页/排序）不走全局蒙层与顶栏进度条，避免和局部骨架屏叠两层
    if (from.matched.length && to.path === from.path && to.name === from.name) {
      finishRouteLoading()
      return
    }
    startRouteLoading()
    $loadingBar.start()
  })

  router.afterEach((to, from) => {
    pendingShellLayout.value = resolvePendingShellLayout(to)

    if (shouldSkipRouteChrome(to)) {
      finishRouteChrome()
      return
    }
    if (from.matched.length && to.path === from.path && to.name === from.name) {
      finishRouteLoading()
      return
    }
    // 回到系统壳后清除 pending，避免污染后续普通页
    pendingShellLayout.value = null
    setTimeout(() => {
      finishRouteChrome()
    }, 200)
  })

  router.onError(() => {
    pendingShellLayout.value = null
    finishRouteLoading()
    $loadingBar.error()
    const appStore = useAppStore()
    if (!appStore.routeGuardCompleted)
      appStore.setRouteGuardCompleted(true)
  })
}
