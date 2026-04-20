<template>
  <div class="immersive-header-bar">
    <!-- 左侧：菜单触发 + Logo + 标题 -->
    <div class="header-left">
      <div
        class="menu-trigger-btn"
        title="展开菜单"
        @click="menuDrawerVisible = !menuDrawerVisible"
      >
        <i class="i-ion-menu" />
      </div>

      <router-link to="/" class="header-logo-link">
        <div class="header-logo-wrapper">
          <TheLogo />
        </div>
        <h2 class="header-system-name">
          {{ systemName }}
        </h2>
      </router-link>
    </div>

    <!-- 中间：面包屑 -->
    <div class="header-center">
      <BreadCrumb />
    </div>

    <!-- 右侧：工具区 -->
    <div class="header-right">
      <MenuSearch />
      <BeginnerGuide />
      <ToggleTheme />
      <Fullscreen />
      <MessageNotification />
      <div class="header-divider" />
      <UserAvatar />
    </div>

    <!-- 抽屉菜单 -->
    <DrawerMenu v-model:show="menuDrawerVisible" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useTenantStore } from '@/store'
import TheLogo from '@/components/common/TheLogo.vue'
import {
  BeginnerGuide,
  BreadCrumb,
  Fullscreen,
  MenuSearch,
  MessageNotification,
  UserAvatar,
} from '@/layouts/components'
import { ToggleTheme } from '@/components'
import DrawerMenu from '../components/DrawerMenu.vue'

const tenantStore = useTenantStore()
const menuDrawerVisible = ref(false)

const systemName = computed(() => {
  return tenantStore.systemName || import.meta.env.VITE_TITLE
})
</script>

<style scoped>
.immersive-header-bar {
  width: 100%;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
  padding: 0 12px;
  flex-shrink: 0;
}

/* 左侧 */
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.menu-trigger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 20px;
  color: var(--text-primary);
  flex-shrink: 0;
}

.menu-trigger-btn:hover {
  background: var(--bg-secondary);
}

.header-logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  overflow: hidden;
}

.header-logo-wrapper {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  background: var(--primary-500);
  box-shadow: 0 2px 6px rgba(22, 93, 255, 0.25);
}

.header-logo-wrapper :deep(img) {
  width: 16px;
  height: 16px;
  object-fit: contain;
  filter: brightness(10);
}

.header-system-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  white-space: nowrap;
  letter-spacing: 0;
}

/* 中间 */
.header-center {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
}

/* 右侧 */
.header-right {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
  padding: 0 4px;
  height: 100%;
}

.header-divider {
  width: 1px;
  height: 14px;
  background: var(--border-light);
  margin: 0 6px;
}
</style>
