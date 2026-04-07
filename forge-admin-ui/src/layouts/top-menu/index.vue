<template>
  <div class="wh-full flex flex-col">
    <!-- 顶部一级菜单 -->
    <header
      class="layout-header flex h-60 flex-shrink-0 items-center px-20"
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
        <div class="flex flex-shrink-0 items-center px-12 text-18">
          <Fullscreen />
          <MessageNotification class="mr-16" />
          <UserAvatar />
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <article class="w-full flex flex-col flex-1 overflow-hidden">
      <AppCard :bordered="false" :padding="false" class="py-0 px-10" shadow="none" radius="none">
        <AppTab class="w-0 flex-1" />
      </AppCard>
      <div class="flex-1 overflow-auto bg-[#f2f3f5] p-12">
        <slot />
      </div>
    </article>
  </div>
</template>

<script setup>
import { useAppStore } from '@/store'
import { AppCard } from '@/components/common'
import {
  AppTab,
  Fullscreen,
  MenuCollapse,
  MenuSearch,
  MessageNotification,
  UserAvatar,
} from '@/layouts/components'
import TopMenu from './components/TopMenu.vue'
import { TheTitle } from "@/components/index.js";

const appStore = useAppStore()
</script>
