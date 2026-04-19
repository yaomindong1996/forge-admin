<template>
  <div class="wh-full flex flex-col">
    <!-- 演示环境提示条 -->
    <DemoBanner />

    <!-- 顶部一级菜单 -->
    <header
      class="layout-header h-60 flex flex-shrink-0 items-center px-20"
      border-b="1px solid light_border dark:dark_border"
    >
      <TheLogo class="mr-20" />
      <TheTitle />
      <MenuCollapse class="mr-10" />
      <TopMenu class="top-menu-wrapper flex-1" />

      <!-- 菜单搜索 -->
      <div class="mx-16">
        <MenuSearch />
      </div>

      <div class="flex items-center">
        <span class="mx-6 opacity-20">|</span>
        <div class="text-18 flex flex-shrink-0 items-center px-12">
          <!--          <ThemeConfigButton class="mr-16" /> -->
          <ToggleTheme />
          <Fullscreen />
          <!--          <ThemeSetting class="mr-16" /> -->
          <MessageNotification class="mr-16" />
          <UserAvatar />
        </div>
      </div>
    </header>

    <div class="w-full flex flex-1 overflow-hidden">
      <!-- 左侧二级及以下菜单 - 只在有子菜单时显示 -->
      <aside
        v-if="showSidebar"
        class="side-menu-wrapper flex flex-col flex-shrink-0 transition-width-300"
        :class="appStore.collapsed ? 'w-64 collapsed' : 'w-200'"
        border-r="1px solid #999999"
      >
        <SideMenu />
      </aside>

      <!-- 主内容区域 -->
      <article class="w-0 flex flex-col flex-1">
        <AppCard :bordered="false" :padding="false" class="px-10 py-3" shadow="none" radius="none">
          <AppTab class="w-0 flex-1" />
        </AppCard>
        <div class="flex-1 overflow-auto bg-[#f2f3f5] p-12">
          <slot />
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { AppCard } from '@/components/common'
import DemoBanner from '@/components/DemoBanner.vue'
import { TheTitle } from '@/components/index.js'
import {
  AppTab,
  Fullscreen,
  MenuCollapse,
  MenuSearch,
  MessageNotification,
  UserAvatar,
} from '@/layouts/components'
import { useAppStore, usePermissionStore } from '@/store'
import SideMenu from './components/SideMenu.vue'
import TopMenu from './components/TopMenu.vue'

const appStore = useAppStore()
const permissionStore = usePermissionStore()
const route = useRoute()

// 判断是否应该显示侧边栏
const showSidebar = computed(() => {
  const menus = permissionStore.menus || []

  // 如果菜单数据还没加载，不显示侧边栏
  if (!menus.length || !permissionStore.menuDataLoaded) {
    return false
  }

  // 递归查找包含当前路由的顶级菜单
  const findTopMenuByPath = (currentPath) => {
    const findInMenu = (menuItems, topMenu) => {
      for (const item of menuItems) {
        if (item.path === currentPath) {
          return topMenu
        }
        if (item.children && item.children.length > 0) {
          const found = findInMenu(item.children, topMenu)
          if (found)
            return found
        }
      }
      return null
    }

    // 在每个顶级菜单中查找
    for (const topMenu of menus) {
      // 如果顶级菜单本身匹配
      if (topMenu.path === currentPath) {
        return topMenu
      }
      // 在顶级菜单的子菜单中查找
      if (topMenu.children && topMenu.children.length > 0) {
        const found = findInMenu(topMenu.children, topMenu)
        if (found)
          return found
      }
    }
    return null
  }

  // 查找当前路由对应的顶级菜单
  const activeTopMenu = findTopMenuByPath(route.path)

  // 如果找到了顶级菜单，检查它是否有子菜单
  if (activeTopMenu) {
    return activeTopMenu.children && activeTopMenu.children.length > 0
  }

  // 如果没有找到匹配的路由，但有选中的顶部菜单ID，使用它
  if (appStore.selectedTopMenuId) {
    const selectedMenu = menus.find(item => item.id === appStore.selectedTopMenuId)
    if (selectedMenu) {
      return selectedMenu.children && selectedMenu.children.length > 0
    }
  }

  // 默认不显示侧边栏
  return false
})

function handleLinkClick(link) {
  window.open(link)
}
</script>
