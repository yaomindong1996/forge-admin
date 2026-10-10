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
          <component :is="LayoutComponent" :key="curRoute.meta?.layout || appStore.layout">
            <Suspense v-if="useRouteSuspense(curRoute)">
              <template #default>
                <component :is="Component" v-if="!tabStore.reloading" :key="resolveRouteViewKey(curRoute)" />
              </template>
              <template #fallback>
                <ApplicationRuntimeSkeleton v-if="isApplicationRuntimeRoute(curRoute)" />
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
        <NoticeDetailModal />
        <SystemAboutModal v-if="versionStore.visible" />
      </n-message-provider>
    </n-dialog-provider>
  </n-config-provider>
</template>

<script setup>
import { darkTheme, dateZhCN, zhCN } from 'naive-ui'
import { computed, defineAsyncComponent, markRaw, onMounted, shallowRef, watch, watchEffect } from 'vue'
// 初始化响应式字体功能
import { useRoute } from 'vue-router'
import { LayoutSetting } from '@/components'
import GlobalLoadingOverlay from '@/components/common/GlobalLoadingOverlay.vue'
import SystemPageLayout from '@/components/common/SystemPageLayout.vue'
import NoticeDetailModal from '@/components/notice/NoticeDetailModal.vue'
import { useWatermark } from '@/composables/useWatermark'
import { isApplicationPortalPath } from '@/router/guards/permission-guard'
import { useAppStore, useAuthStore, usePermissionStore, useTabStore, useUserStore } from '@/store'
import { useSystemVersionStore } from '@/stores/system/versionStore'
import { initResponsiveFont } from '@/utils/responsive-font'
import ApplicationPortalSkeleton from '@/views/app-center/components/portal/ApplicationPortalSkeleton.vue'
import ApplicationRuntimeSkeleton from '@/views/app-center/components/portal/ApplicationRuntimeSkeleton.vue'
import AppPortalLayout from './layouts/app-portal/index.vue'
import EmptyLayout from './layouts/empty/index.vue'
import { canConfigureLayout } from './layouts/layout-settings-visibility'
import { defaultLayout, layoutSettingVisible, normalizeLayout } from './settings'

// 使用 shallowRef 确保 Layout 引用稳定
const LayoutComponent = shallowRef(null)
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
  const path = String(curRoute?.path || '')
  return /^\/app-center\/application\/[^/]+\/runtime(?:\/|$)/.test(path)
}

function useRouteSuspense(curRoute) {
  return curRoute?.meta?.layout === 'app-portal' || isApplicationRuntimeRoute(curRoute)
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
const isSystemRoute = computed(() => route.path.startsWith('/system/'))
const showLayoutSetting = computed(() => {
  return layoutSettingVisible && canConfigureLayout({
    routeLayout: route.meta?.layout,
    authenticated: userStore.userInfo || userStore.staffInfo,
  })
})

// 监听布局变化，及时更新布局组件
watch(() => route.meta?.layout || appStore.layout, (layoutName) => {
  if (layoutName) {
    const layoutComponent = getLayout(layoutName)
    LayoutComponent.value = layoutComponent
  }
  else {
    LayoutComponent.value = null
  }
}, { immediate: true })

// 显示加载状态的条件：
// 1. 用户已登录但路由守卫未完成
// 2. 菜单数据未加载完成
const showLoading = computed(() => {
  // 发布运行页 / 页面管理自己画骨架，不要先盖一层“正在加载...”再白屏。
  if (isApplicationPortalPath(route.path) || isApplicationRuntimeRoute(route)) {
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
    color-mix(in srgb, var(--bg-primary, #fff) 84%, transparent);
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
</style>
