<template>
  <n-menu
    ref="menu"
    class="modern-side-menu"
    accordion
    :indent="18"
    :collapsed-icon-size="22"
    :collapsed-width="64"
    :collapsed="appStore.collapsed"
    :options="processedMenus"
    :value="activeKey"
    @update:value="handleMenuSelect"
  />
</template>

<script setup>
import { useMenu } from '@/composables'
import { useAppStore } from '@/store'

const appStore = useAppStore()

const { processedMenus, activeKey, handleMenuSelect } = useMenu()

const menu = ref(null)
const route = useRoute()
watch(route, async () => {
  await nextTick()
  menu.value?.showOption()
})
</script>

<style>
.modern-side-menu {
  padding: 6px 0;
  background: transparent;
}

/* 菜单项 - SnowAdmin 风格 */
.modern-side-menu .n-menu-item-content {
  margin: 1px 6px;
  border-radius: var(--radius-md);
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast);
  font-size: 13px;
  font-weight: 400;
  color: var(--text-secondary);
  min-height: 38px;
  padding: 0 12px !important;
}

.modern-side-menu .n-menu-item-content:hover {
  color: var(--primary-500);
  background: var(--primary-50);
}

/* 选中状态 */
.modern-side-menu .n-menu-item-content--selected {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
  font-weight: 500;
}

.modern-side-menu .n-menu-item-content--selected:hover {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
}

/* 选中项左侧指示条 */
.modern-side-menu .n-menu-item-content--selected::after {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 3px;
  background: var(--primary-500);
  border-radius: 0 2px 2px 0;
}

/* 图标 */
.modern-side-menu .n-menu-item-content__icon {
  font-size: 16px;
  margin-right: 8px !important;
  color: var(--text-tertiary);
  opacity: 0.8;
}

.modern-side-menu .n-menu-item-content:hover .n-menu-item-content__icon {
  color: var(--primary-500);
  opacity: 1;
}

.modern-side-menu .n-menu-item-content--selected .n-menu-item-content__icon {
  color: var(--primary-500) !important;
  opacity: 1;
}

/* 菜单文字 */
.modern-side-menu .n-menu-item-content__label {
  font-size: 13px;
  line-height: 1.4;
}

.modern-side-menu .n-menu-item-content--selected .n-menu-item-content__label {
  color: var(--primary-500) !important;
}

.modern-side-menu .n-menu-item-content:hover .n-menu-item-content__label {
  color: var(--primary-500) !important;
}

/* 子菜单缩进 */
.modern-side-menu .n-submenu-children {
  padding-left: 0;
}

.modern-side-menu .n-submenu-children .n-menu-item-content {
  font-size: 13px;
}

/* 分组标题 */
.modern-side-menu .n-menu-item-group-title {
  padding: 14px 16px 4px !important;
  font-size: 11px !important;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* 折叠状态 */
.modern-side-menu.n-menu--collapsed .n-menu-item-content {
  justify-content: center;
  padding: 0 !important;
  width: 38px;
  height: 38px;
  margin: 2px auto;
  border-radius: var(--radius-md);
}

/* 折叠状态下强制图标居中 - 隐藏文字和箭头占位区域 */
.modern-side-menu.n-menu--collapsed .n-menu-item-content__icon {
  margin-right: 0 !important;
}

.modern-side-menu.n-menu--collapsed .n-menu-item-content-header {
  display: none !important;
}

.modern-side-menu.n-menu--collapsed .n-menu-item-content__arrow {
  display: none !important;
}

.modern-side-menu.n-menu--collapsed .n-menu-item-content--selected::after {
  display: none;
}

/* 滚动条 */
.modern-side-menu::-webkit-scrollbar {
  width: 4px;
}

.modern-side-menu::-webkit-scrollbar-track {
  background: transparent;
}

.modern-side-menu::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: var(--radius-full);
}

.modern-side-menu::-webkit-scrollbar-thumb:hover {
  background: var(--border-default);
}
</style>
