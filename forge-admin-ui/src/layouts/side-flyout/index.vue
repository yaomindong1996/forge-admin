<template>
  <div class="side-flyout">
    <!-- 顶栏通栏：Logo 和系统名成组，避免和侧栏各用一套底色 -->
    <SideFlyoutHeader :narrow="isNarrow" :collapsed="appStore.collapsed" />

    <div class="side-flyout-body">
      <!-- 左侧只放一级菜单；窄屏改走顶栏抽屉 -->
      <aside
        v-if="!isNarrow"
        class="side-flyout-rail-wrap"
        :class="{
          'is-collapsed': appStore.collapsed,
          'is-flyout-open': Boolean(store.activeMenu),
        }"
        :aria-hidden="appStore.collapsed"
      >
        <SideFlyoutRail />
      </aside>

      <article class="side-flyout-main">
        <div class="side-flyout-tab-bar">
          <AppTab />
        </div>
        <div class="side-flyout-content" :class="{ 'is-flush': isFlowTaskListPage }">
          <div
            v-if="store.activeMenu" class="side-flyout-scrim" aria-hidden="true"
            @click="store.closeMenus()"
          />
          <slot />
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { computed, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AppTab } from '@/layouts/components'
import { useAppStore } from '@/store'
import { useBusinessWorkbenchStore } from '@/stores/layout/businessWorkbenchStore'
import { isFlowTaskListPath } from '@/utils/flow-task-layout'
import SideFlyoutHeader from './components/SideFlyoutHeader.vue'
import SideFlyoutRail from './components/SideFlyoutRail.vue'
import '../business-workbench/workbench.css'
import './side-flyout.css'

const route = useRoute()
const appStore = useAppStore()
const store = useBusinessWorkbenchStore()
const isNarrow = useMediaQuery('(max-width: 768px)')
const isFlowTaskListPage = computed(() => isFlowTaskListPath(route.path))

watch(() => route.fullPath, () => store.onRouteChange())
watch(() => appStore.collapsed, () => store.closeMenus())
watch(isNarrow, (narrow) => {
  if (narrow)
    store.closeMenus()
})
onUnmounted(() => store.closeMenus())
</script>
