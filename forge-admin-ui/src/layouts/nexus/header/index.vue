<template>
  <div class="nexus-header">
    <!-- 左侧：面包屑 -->
    <div class="header-left">
      <BreadCrumb />
    </div>

    <!-- 中间：搜索 -->
    <div class="header-center">
      <MenuSearch />
    </div>

    <!-- 右侧：工具栏 -->
    <div class="header-right">
      <ToggleTheme />
      <Fullscreen />
      <MessageNotification />
      <div class="header-divider" />
      <UserAvatar />
    </div>
  </div>
</template>

<script setup>
import ToggleTheme from '@/components/common/ToggleTheme.vue'
import Fullscreen from '@/layouts/components/Fullscreen.vue'
import MenuSearch from '@/layouts/components/MenuSearch.vue'
import MessageNotification from '@/layouts/components/MessageNotification.vue'
import UserAvatar from '@/layouts/components/UserAvatar.vue'
import BreadCrumb from '../BreadCrumb.vue'
</script>

<style scoped>
.nexus-header {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 16px;
}

/* 左侧面包屑 */
.header-left {
  display: flex;
  align-items: center;
  flex: 0 1 auto;
  min-width: 0;
}

/* 面包屑样式 */
.header-left :deep(.n-breadcrumb) {
  font-size: 14px;
  font-weight: 500;
}

.header-left :deep(.n-breadcrumb-separator) {
  color: var(--text-tertiary);
  font-weight: 400;
  margin: 0 6px;
}

.header-left :deep(.n-breadcrumb-item__link) {
  color: var(--text-secondary);
  font-size: 14px;
  padding: 4px 0;
}

.header-left :deep(.n-breadcrumb-item:last-child .n-breadcrumb-item__link) {
  color: var(--text-primary);
  font-weight: 600;
}

.header-left :deep(.n-breadcrumb-item__link:hover) {
  color: var(--primary-500) !important;
}

/* 中间搜索框 */
.header-center {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 560px;
  margin: 0 auto;
}

.header-center :deep(.search-trigger) {
  width: 100%;
  max-width: 560px;
  justify-content: flex-start;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 8px 14px;
  height: 36px;
  box-sizing: border-box;
  transition: all var(--transition-base);
  gap: 10px;
}

.header-center :deep(.search-trigger:hover) {
  background: var(--nexus-hover-bg);
  border-color: var(--border-default);
}

.header-center :deep(.search-text) {
  font-size: 14px;
  color: var(--text-tertiary);
}

.header-center :deep(.search-shortcut) {
  padding: 2px 8px;
  font-size: 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 4px;
}

/* 右侧工具栏 */
.header-right {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  margin-left: auto;
}

.header-divider {
  width: 1px;
  height: 24px;
  background: var(--border-light);
  margin: 0 8px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .header-center {
    max-width: 360px;
  }
}

@media (max-width: 768px) {
  .header-center {
    display: none;
  }
}
</style>
