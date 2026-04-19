<template>
  <n-config-provider
    class="wh-full"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme="appStore.isDark ? darkTheme : undefined"
    :theme-overrides="appStore.naiveThemeOverrides"
  >
    <!-- 当菜单数据未加载完成时显示加载状态 -->
    <div v-if="showLoading" class="loading-wrapper">
      <n-spin size="large">
        <template #description>
          <div class="loading-text">
            正在加载...
          </div>
        </template>
      </n-spin>
    </div>
    <router-view v-else v-slot="{ Component, route: curRoute }">
      <component :is="LayoutComponent" :key="curRoute.meta?.layout || appStore.layout">
        <!--        <transition name="fade-slide" mode="out-in" appear> -->
        <KeepAlive :include="keepAliveNames">
          <component :is="Component" v-if="!tabStore.reloading" :key="curRoute.fullPath" />
        </KeepAlive>
        <!--        </transition> -->
      </component>

      <LayoutSetting v-if="layoutSettingVisible" class="fixed right-12 top-1/2 z-999" />
    </router-view>

    <!-- 全局水印 -->
    <div v-if="watermarkConfig.enable" class="watermark-layer" :style="watermarkStyle" />
  </n-config-provider>
</template>

<script setup>
import { darkTheme, dateZhCN, zhCN } from 'naive-ui'
import { computed, defineAsyncComponent, markRaw, shallowRef, watch } from 'vue'
// 初始化响应式字体功能
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { LayoutSetting } from '@/components'
import { useWatermark } from '@/composables/useWatermark'
import { useAppStore, usePermissionStore, useTabStore, useUserStore } from '@/store'
import { initResponsiveFont } from '@/utils/responsive-font'

import { layoutSettingVisible } from './settings'

// 使用 shallowRef 确保 Layout 引用稳定
const LayoutComponent = shallowRef(null)

const layouts = new Map()
function getLayout(name) {
  // 利用map将加载过的layout缓存起来，防止重新加载layout导致页面闪烁
  if (layouts.has(name)) {
    return layouts.get(name)
  }
  const layout = markRaw(defineAsyncComponent(() => import(`@/layouts/${name}/index.vue`)))
  layouts.set(name, layout)
  return layout
}

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const userStore = useUserStore()

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
  // 如果路由守卫已完成且菜单数据已加载，则不显示加载状态
  if (appStore.routeGuardCompleted && permissionStore.menuDataLoaded) {
    return false
  }

  // 如果用户已登录但菜单数据未加载完成，则显示加载状态
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

if (appStore.layout === 'default')
  appStore.setLayout('')

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
  initResponsiveFont((scale) => {
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
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.4);
  z-index: 99;
}

.loading-text {
  margin-top: 12px;
  font-size: 16px;
  color: #333;
}

:deep(.n-spin-content) {
  display: flex;
  flex-direction: column;
  align-items: center;
}

:deep(.n-spin-description) {
  margin-top: 12px;
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
