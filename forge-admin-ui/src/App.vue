<template>
  <n-config-provider
    class="wh-full"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme="appStore.isDark ? darkTheme : undefined"
    :theme-overrides="appStore.naiveThemeOverrides"
  >
    <n-dialog-provider>
      <n-message-provider>
        <!-- 当菜单数据未加载完成时显示加载状态 -->
        <div v-if="showLoading" class="loading-wrapper">
          <div class="app-loader" aria-hidden="true" />
          <div class="loading-text">
            正在加载...
          </div>
        </div>
        <router-view v-else v-slot="{ Component, route: curRoute }">
          <component :is="LayoutComponent" :key="activeShellLayout">
            <Suspense v-if="useRouteSuspense(curRoute)">
              <template #default>
                <component :is="Component" v-if="!tabStore.reloading" :key="resolveRouteViewKey(curRoute)" />
              </template>
              <template #fallback>
                <ApplicationRuntimeSkeleton v-if="isApplicationRuntimeRoute(curRoute) || isApplicationRuntimePath(readBrowserPathname())" />
                <ApplicationPortalSkeleton v-else />
              </template>
            </Suspense>
            <SystemPageLayout v-else-if="isSystemRoute">
              <KeepAlive :include="keepAliveNames">
                <component :is="Component" v-if="!tabStore.reloading" :key="resolveRouteViewKey(curRoute)" />
              </KeepAlive>
            </SystemPageLayout>
            <KeepAlive v-else :include="keepAliveNames">
              <component :is="Component" v-if="!tabStore.reloading" :key="resolveRouteViewKey(curRoute)" />
            </KeepAlive>
          </component>

          <LayoutSetting
            v-if="showLayoutSetting" :show-trigger="!['business-workbench', 'side-flyout'].includes(appStore.layout)"
            class="fixed right-12 top-1/2 z-999"
          />
        </router-view>

        <!-- 全局水印 -->
        <div v-if="watermarkConfig.enable" class="watermark-layer" :style="watermarkStyle" />
        <GlobalLoadingOverlay />
        <!-- 切入设计态瞬间盖住旧系统壳内容，避免侧栏卸掉后列表空闪一帧 -->
        <div
          v-if="showDesignShellTransition"
          class="design-shell-transition"
          aria-busy="true"
          aria-label="正在进入设计态"
        >
          <ApplicationRuntimeSkeleton />
        </div>
        <!-- 无侧栏停靠点时左下角兜底：始终图标态，避免白底长按钮挡内容 -->
        <RecentDownloads v-if="showRecentDownloads && !downloadsDockAvailable" compact />
        <NoticeDetailModal />
        <SystemAboutModal v-if="versionStore.visible" />
      </n-message-provider>
    </n-dialog-provider>
  </n-config-provider>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { darkTheme, dateZhCN, zhCN } from 'naive-ui'
import { computed, defineAsyncComponent, markRaw, onMounted, watch, watchEffect } from 'vue'
// 初始化响应式字体功能
import { useRoute } from 'vue-router'
import { LayoutSetting } from '@/components'
import GlobalLoadingOverlay from '@/components/common/GlobalLoadingOverlay.vue'
import SystemPageLayout from '@/components/common/SystemPageLayout.vue'
import NoticeDetailModal from '@/components/notice/NoticeDetailModal.vue'
import { useWatermark } from '@/composables/useWatermark'
import { useSidebarVisibility } from '@/layouts/composables/useSidebarVisibility'
import {
  isApplicationPortalPath,
  isApplicationRuntimePath,
  isEmptyShellPath,
  pendingShellLayout,
  readBrowserPathname,
  resolveShellLayoutName,
} from '@/router/shell-layout'
import { useAppStore, useAuthStore, usePermissionStore, useTabStore, useUserStore } from '@/store'
import { useRecentDownloadsStore } from '@/stores/system/recentDownloadsStore'
import { hasDownloadDock } from '@/stores/system/recentDownloadsUtils'
import { useSystemVersionStore } from '@/stores/system/versionStore'
import { initResponsiveFont } from '@/utils/responsive-font'
import ApplicationPortalSkeleton from '@/views/app-center/components/portal/ApplicationPortalSkeleton.vue'
import ApplicationRuntimeSkeleton from '@/views/app-center/components/portal/ApplicationRuntimeSkeleton.vue'
import AppPortalLayout from './layouts/app-portal/index.vue'
import RecentDownloads from './layouts/components/RecentDownloads.vue'
import EmptyLayout from './layouts/empty/index.vue'
import { canConfigureLayout } from './layouts/layout-settings-visibility'
import { defaultLayout, layoutSettingVisible, normalizeLayout } from './settings'

const SystemAboutModal = defineAsyncComponent(() => import('@/components/common/SystemAboutModal.vue'))
const versionStore = useSystemVersionStore()
const versionAuth = useAuthStore()
watch(() => versionAuth.accessToken, () => versionStore.close())

const layouts = new Map()
const layoutModules = import.meta.glob([
  './layouts/*/index.vue',
  '!./layouts/app-portal/index.vue',
  '!./layouts/empty/index.vue',
])

function resolveRouteViewKey(curRoute) {
  return curRoute.meta?.preserveOnQuery ? curRoute.path : curRoute.fullPath
}

function isApplicationRuntimeRoute(curRoute) {
  if (curRoute?.name === 'BusinessApplicationRuntime')
    return true
  return isApplicationRuntimePath(curRoute?.path)
}

function useRouteSuspense(curRoute) {
  // 仅应用门户 / 设计运行页用 Suspense 骨架；登录等其它空壳页保持原同步挂载
  return curRoute?.meta?.layout === 'app-portal'
    || isApplicationRuntimeRoute(curRoute)
    || isApplicationPortalPath(curRoute?.path)
    || isApplicationRuntimePath(readBrowserPathname())
}

/** 同步加载的布局（不在 glob 里，但必须按名称认作合法，否则会回退到默认工作台） */
const SYNC_LAYOUTS = new Set(['empty', 'app-portal'])

function normalizeLayoutName(name) {
  const layoutName = normalizeLayout(name)
  if (SYNC_LAYOUTS.has(layoutName))
    return layoutName
  return layoutModules[`./layouts/${layoutName}/index.vue`] ? layoutName : defaultLayout
}

function getLayout(name) {
  const layoutName = normalizeLayoutName(name)
  // 利用map将加载过的layout缓存起来，防止重新加载layout导致页面闪烁
  if (layouts.has(layoutName)) {
    return layouts.get(layoutName)
  }
  // app-portal / empty 同步加载：避免再套一层异步 layout 白屏
  const layout = layoutName === 'app-portal'
    ? markRaw(AppPortalLayout)
    : layoutName === 'empty'
      ? markRaw(EmptyLayout)
      : markRaw(defineAsyncComponent(layoutModules[`./layouts/${layoutName}/index.vue`]))
  layouts.set(layoutName, layout)
  return layout
}

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const userStore = useUserStore()
const downloadsStore = useRecentDownloadsStore()
const downloadsNarrow = useMediaQuery('(max-width: 768px)')
const { showSidebar: downloadsSidebarVisible } = useSidebarVisibility()

// 用 pending + URL 推断，避免进入设计态时仍渲染系统侧栏一帧
const activeShellLayout = computed(() => normalizeLayoutName(resolveShellLayoutName({
  routeMetaLayout: route.meta?.layout,
  routePath: route.path,
  locationPath: readBrowserPathname(),
  pending: pendingShellLayout.value,
  appLayout: appStore.layout,
})))
const LayoutComponent = computed(() => getLayout(activeShellLayout.value))
const downloadsLayout = computed(() => activeShellLayout.value)
const downloadsDockAvailable = computed(() => hasDownloadDock(downloadsLayout.value, {
  narrow: downloadsNarrow.value,
  collapsed: appStore.collapsed,
  showSidebar: downloadsSidebarVisible.value,
}))
watch([() => route.fullPath, downloadsLayout, () => appStore.collapsed, downloadsNarrow], () => {
  downloadsStore.open = false
})
const isSystemRoute = computed(() => route.path.startsWith('/system/'))
const showRecentDownloads = computed(() => {
  return !!versionAuth.accessToken
    && activeShellLayout.value !== 'empty'
    && activeShellLayout.value !== 'app-portal'
    && route.path !== '/login'
})
const showLayoutSetting = computed(() => {
  const shell = activeShellLayout.value
  return layoutSettingVisible && canConfigureLayout({
    // 设计态/门户空壳禁止打开布局设置；系统壳才允许
    routeLayout: (shell === 'empty' || shell === 'app-portal') ? shell : undefined,
    authenticated: userStore.userInfo || userStore.staffInfo,
  })
})

const showDesignShellTransition = computed(() => {
  if (pendingShellLayout.value !== 'empty')
    return false
  // 目标已是空壳且当前路由也已落在设计态时，交给页面内骨架，不必再盖一层
  if (isApplicationRuntimeRoute(route) || isEmptyShellPath(route.path))
    return false
  return true
})

// 显示加载状态的条件：
// 1. 用户已登录但路由守卫未完成
// 2. 菜单数据未加载完成
const showLoading = computed(() => {
  const browserPath = readBrowserPathname()
  // 发布运行页 / 设计态自己画骨架，不要先盖一层“正在加载...”再闪系统菜单。
  if (
    isApplicationPortalPath(route.path)
    || isApplicationPortalPath(browserPath)
    || isApplicationRuntimeRoute(route)
    || isEmptyShellPath(route.path)
    || isEmptyShellPath(browserPath)
    || activeShellLayout.value === 'empty'
    || activeShellLayout.value === 'app-portal'
  ) {
    return false
  }

  // 路由守卫完成后不再阻塞页面渲染，避免菜单接口异常时永久 loading。
  if (appStore.routeGuardCompleted) {
    return false
  }

  // 已登录但守卫还在补拉菜单时显示加载状态。
  if (userStore.userInfo && !permissionStore.menuDataLoaded) {
    return true
  }

  // 在路由守卫完成之前，如果用户已登录，显示加载状态
  if (userStore.userInfo && appStore.routeGuardCompleted === false) {
    return true
  }

  // 其他情况根据路由守卫状态决定
  return !appStore.routeGuardCompleted
})

const tabStore = useTabStore()
// 修改缓存逻辑，根据tabStore中的cacheViews来决定是否缓存
const keepAliveNames = computed(() => {
  return tabStore.cacheViews
})

// 使用水印功能
const { watermarkConfig, getWatermarkStyle } = useWatermark()

// 水印样式
const watermarkStyle = computed(() => getWatermarkStyle())
onMounted(() => {
  initResponsiveFont(() => {
    // 更新Naive UI主题配置，使组件库字体也响应式变化
    appStore.updateNaiveThemeOverrides({
      fontSize: `calc(14px * var(--font-scale, 1))`,
    })
  })
})

// 监听主题变化
watchEffect(() => {
  appStore.setThemeColor(appStore.primaryColor, appStore.isDark)
  // 应用主题配置（响应暗色模式变化）
  appStore.applyCurrentTheme()
})
</script>

<style scoped>
.loading-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 18px;
  background:
    radial-gradient(
      circle at center,
      color-mix(in srgb, var(--primary-color, #165dff) 8%, transparent),
      transparent 34%
    ),
    color-mix(in srgb, var(--bg-primary) 84%, transparent);
  backdrop-filter: blur(2px);
  z-index: 99;
}

.app-loader {
  --size: 1px;
  --loader-color: var(--primary-color, #165dff);

  width: calc(48 * var(--size));
  height: calc(48 * var(--size));
  background: var(--loader-color);
  border-radius: 10%;
  box-shadow: 0 10px 30px color-mix(in srgb, var(--loader-color) 32%, transparent);
  animation: app-loader-rotate 1s linear infinite;
}

.loading-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary, #4e5969);
  letter-spacing: 0;
}

.dark .loading-wrapper {
  background:
    radial-gradient(
      circle at center,
      color-mix(in srgb, var(--primary-color, #4080ff) 18%, transparent),
      transparent 36%
    ),
    color-mix(in srgb, var(--bg-primary, #0f172a) 88%, transparent);
}

.dark .app-loader {
  --loader-color: var(--primary-color, #4080ff);
}

@keyframes app-loader-rotate {
  0% {
    transform: rotate(0deg) scale(0.2);
    border-radius: 10%;
  }

  50% {
    transform: rotate(180deg) scale(1.5);
    border-radius: 50%;
  }

  100% {
    transform: rotate(360deg) scale(0.2);
    border-radius: 10%;
  }
}

/* 水印层样式 */
.watermark-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1000;
}

.design-shell-transition {
  position: fixed;
  inset: 0;
  z-index: 98;
  background: var(--bg-secondary, #f5f6f8);
}
</style>
