<template>
  <div class="business-workbench">
    <!-- 顶栏：品牌 + Mega 菜单 + 工具 -->
    <WorkbenchHeader />
    <!-- Tab 多页签（替代原面包屑） -->
    <div class="business-tab-bar">
      <AppTab />
    </div>
    <!-- 页面内容 -->
    <main class="business-layout-content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AppTab } from '@/layouts/components'
import { useBusinessWorkbenchStore } from '@/stores/layout/businessWorkbenchStore'
import WorkbenchHeader from './components/WorkbenchHeader.vue'
import './workbench.css'

const route = useRoute()
const store = useBusinessWorkbenchStore()
watch(() => route.fullPath, () => store.closeMenus())
onUnmounted(() => store.closeMenus())
</script>
