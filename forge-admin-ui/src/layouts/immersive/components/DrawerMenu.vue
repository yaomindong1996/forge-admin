<template>
  <n-drawer
    v-model:show="drawerVisible" width="min(360px, 100vw)" placement="left"
    class="navigation-drawer" :style="{ background: 'var(--side-menu-bg-color)' }"
  >
    <n-drawer-content closable :body-content-style="{ padding: '0', display: 'flex', flexDirection: 'column' }">
      <template #header>
        <div class="drawer-brand">
          <TheLogo /><TheTitle />
        </div>
      </template>
      <div class="drawer-menu-container">
        <!-- 搜索与当前页面固定，长菜单独立滚动 -->
        <div class="drawer-search">
          <n-input v-model:value="searchKeyword" placeholder="搜索授权菜单" clearable>
            <template #prefix>
              <i class="i-lucide:search" />
            </template>
          </n-input>
          <span class="drawer-current">当前页面：{{ route.meta?.title || '首页' }}</span>
        </div>
        <!-- 共用导航支持完整层级、真实图标和路由选中状态 -->
        <SideMenu
          v-if="filteredMenus.length" class="drawer-menu-list" :options="filteredMenus"
          :collapsed-override="false" :expand-all="Boolean(searchKeyword.trim())" @select="handleSelect"
        />
        <div v-else class="drawer-empty" role="status">
          <i class="i-lucide:search-x" />
          {{ searchKeyword ? '没有匹配的菜单' : '暂无可访问菜单' }}
        </div>
        <div class="drawer-footer">
          <i class="i-lucide:panel-left" /><span>选择功能后自动收起菜单</span>
        </div>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TheLogo from '@/components/common/TheLogo.vue'
import TheTitle from '@/components/common/TheTitle.vue'
import { useMenu } from '@/composables'
import SideMenu from '@/layouts/components/SideMenu.vue'
import { filterNavigationMenus } from '../menu-search'

const emit = defineEmits(['select'])
const route = useRoute()
const { processedMenus } = useMenu()
const drawerVisible = defineModel('show', { type: Boolean, default: false })
const searchKeyword = ref('')
const filteredMenus = computed(() => filterNavigationMenus(processedMenus.value, searchKeyword.value))
function handleSelect() {
  emit('select')
  drawerVisible.value = false
}
watch(drawerVisible, (visible) => {
  if (!visible) {
    searchKeyword.value = ''
  }
})
</script>

<style scoped>
.drawer-brand {
  --brand-title-text-color: var(--side-menu-text-color);
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.drawer-menu-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  color: var(--side-menu-text-color);
}
.drawer-search {
  flex-shrink: 0;
  display: grid;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--side-menu-border-color);
}
.drawer-current {
  font-size: 11px;
  opacity: 0.75;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.drawer-menu-list {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 8px 0;
}
.drawer-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 12px 16px;
  border-top: 1px solid var(--side-menu-border-color);
  font-size: 11px;
  opacity: 0.75;
}
.drawer-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
}
.navigation-drawer :deep(.n-drawer-header),
.navigation-drawer :deep(.n-drawer-header__close) {
  color: var(--side-menu-text-color);
}
.navigation-drawer :deep(.n-drawer-header) {
  border-color: var(--side-menu-border-color);
}
</style>
