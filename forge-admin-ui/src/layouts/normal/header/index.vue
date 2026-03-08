<template>
  <AppCard class="layout-header flex items-center px-12" border-b="1px solid light_border dark:dark_border">
    <MenuCollapse />

    <!-- 菜单搜索 -->
    <div class="ml-16">
      <MenuSearch />
    </div>

    <AppTab class="w-0 flex-1 px-12" />

    <span class="mx-6 opacity-20">|</span>

    <div class="flex flex-shrink-0 items-center px-12 text-18">
      <BeginnerGuide />

      <ToggleTheme />

      <Fullscreen />

      <ThemeSetting class="mr-16" />

      <MessageNotification class="mr-16" />

      <UserAvatar />
    </div>
  </AppCard>
</template>

<script setup>
import { ToggleTheme } from '@/components'
import { AppTab, BeginnerGuide, Fullscreen, MenuCollapse, MenuSearch, MessageNotification, UserAvatar } from '@/layouts/components'

function handleLinkClick(link) {
  window.open(link)
}
</script>
