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
      <TopMenu class="top-menu-wrapper flex-1" />
      <!-- 菜单搜索 -->
      <div class="mx-16">
        <MenuSearch />
      </div>

      <div class="flex items-center">
        <span class="mx-6 opacity-20">|</span>
        <div class="text-18 flex flex-shrink-0 items-center px-12">
          <Fullscreen />
          <MessageNotification class="mr-16" />
          <UserAvatar />
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <article class="w-full flex flex-col flex-1 overflow-hidden">
      <AppCard :bordered="false" :padding="false" class="px-10 py-0" shadow="none" radius="none">
        <AppTab class="w-0 flex-1" />
      </AppCard>
      <div class="flex-1 overflow-auto bg-[#f2f3f5] p-12">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import { AppCard } from '@/components/common'
import DemoBanner from '@/components/DemoBanner.vue'
import { TheTitle } from '@/components/index.js'
import {
  AppTab,
  Fullscreen,
  MenuSearch,
  MessageNotification,
  UserAvatar,
} from '@/layouts/components'
import { useAppStore } from '@/store'
import TopMenu from './components/TopMenu.vue'

const appStore = useAppStore()
</script>
