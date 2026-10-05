<template>
  <div class="nexus-header layout-chrome-header">
    <!-- 左侧：面包屑 -->
    <div class="header-left">
      <BreadCrumb />
    </div>

    <!-- 右侧：工具栏 -->
    <HeaderTools class="header-right" />
  </div>
</template>

<script setup>
import HeaderTools from '@/layouts/components/HeaderTools.vue'
import BreadCrumb from '../BreadCrumb.vue'
</script>

<style scoped>
.nexus-header {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  color: var(--layout-header-text-color);
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
  display: flex;
  align-items: center;
}

.header-left :deep(.n-breadcrumb ul) {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
}

.header-left :deep(.n-breadcrumb-item) {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

.header-left :deep(.n-breadcrumb-item__separator) {
  color: var(--layout-header-text-color);
  font-weight: 400;
  margin: 0 6px;
  display: inline-flex;
  align-items: center;
  height: 24px;
  line-height: 24px;
}

.header-left :deep(.n-breadcrumb-item__link) {
  color: var(--layout-header-text-color);
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0;
  line-height: 24px;
}

.header-left :deep(.nexus-breadcrumb-entry) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 24px;
  min-width: 0;
  line-height: 1;
}

.header-left :deep(.nexus-breadcrumb-label) {
  display: inline-flex;
  align-items: center;
  height: 24px;
  line-height: 24px;
}

.header-left :deep(.icon-renderer) {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 16px;
  line-height: 1;
}

.header-left :deep(.icon-renderer .n-icon),
.header-left :deep(.icon-renderer i),
.header-left :deep(.icon-renderer .icon-image) {
  display: block;
  width: 16px;
  height: 16px;
  line-height: 16px;
}

.header-left :deep(.n-breadcrumb-item:last-child .n-breadcrumb-item__link) {
  color: var(--layout-header-text-color);
  font-weight: 600;
}

.header-left :deep(.n-breadcrumb-item__link:hover) {
  color: var(--top-menu-text-color-hover);
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
  padding: 7px 12px;
  height: 34px;
  box-sizing: border-box;
  transition: all var(--transition-base);
  gap: 8px;
}

.header-center :deep(.search-trigger:hover) {
  background: var(--layout-header-hover-color);
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
  margin: 0 6px;
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
