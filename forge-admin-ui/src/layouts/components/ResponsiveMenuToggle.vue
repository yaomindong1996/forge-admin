<template>
  <!-- 桌面只改变收起状态；窄屏用带遮罩的授权菜单，不改变桌面偏好 -->
  <MenuCollapse v-if="!isNarrow" />
  <button
    v-else class="chrome-icon-button" type="button" title="打开菜单" aria-label="打开菜单"
    :aria-expanded="drawerVisible" aria-haspopup="dialog" @click="drawerVisible = true"
  >
    <i class="i-lucide:menu" />
  </button>
  <DrawerMenu v-model:show="drawerVisible" />
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { ref, watch } from 'vue'
import DrawerMenu from '@/layouts/immersive/components/DrawerMenu.vue'
import MenuCollapse from './MenuCollapse.vue'

const isNarrow = useMediaQuery('(max-width: 768px)')
const drawerVisible = ref(false)
// 窗口放大后回到桌面导航，不能残留遮罩挡住内容。
watch(isNarrow, () => {
  drawerVisible.value = false
})
</script>
