<template>
  <div class="layout-header flex items-center px-12" border-b="1px solid light_border dark:dark_border">
    <MenuCollapse />

    <!-- 菜单搜索 -->
    <div class="ml-16">
      <MenuSearch />
    </div>

    <div class="header-breadcrumb-wrap">
      <BreadCrumb />
    </div>

    <div class="header-tools-wrap">
      <BeginnerGuide />

      <ToggleTheme />

      <Fullscreen />

      <!--      <ThemeSetting /> -->

      <MessageNotification />

      <UserAvatar />
    </div>
  </div>
</template>

<script setup>
import { ToggleTheme } from '@/components'
import { BeginnerGuide, BreadCrumb, Fullscreen, MenuCollapse, MenuSearch, MessageNotification, UserAvatar } from '@/layouts/components'

function handleLinkClick(link) {
  window.open(link)
}
</script>

<style scoped>
.layout-header {
  width: 100%;
  height: 50px;
  flex-shrink: 0;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
}

.header-breadcrumb-wrap {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  margin-left: 12px;
}

.header-tools-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding-left: 12px;
}
</style>
