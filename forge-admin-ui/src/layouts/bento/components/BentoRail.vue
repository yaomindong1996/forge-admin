<template>
  <div class="bento-rail">
    <!-- Logo -->
    <div class="bento-logo" title="首页">
      <TheLogo />
    </div>

    <!-- 菜单触发按钮 -->
    <button
      class="bento-menu-trigger"
      type="button"
      aria-label="打开菜单"
      title="打开菜单"
      @click="menuDrawerVisible = true"
    >
      <i class="i-ion-menu" />
    </button>

    <!-- 顶部菜单快捷入口 -->
    <div class="bento-quick-links">
      <button
        v-for="item in topMenus"
        :key="item.key"
        class="quick-link"
        :class="{ active: item.key === activeKey }"
        :title="item.label"
        type="button"
        :aria-label="item.label"
        @click="handleMenuSelect(item)"
      >
        <IconRenderer :icon="item.iconClass" :size="20" />
      </button>
    </div>

    <!-- 底部工具栏 -->
    <div class="bento-tools">
      <div class="tool-item" title="切换主题" @click="appStore.isDark = !appStore.isDark">
        <i :class="appStore.isDark ? 'i-material-symbols:light-mode' : 'i-material-symbols:dark-mode-outline'" />
      </div>
      <div class="tool-item" title="全屏" @click="toggleFullscreen">
        <i class="i-material-symbols:fullscreen" />
      </div>
      <div class="tool-item" title="消息">
        <MessageNotification />
      </div>
      <div class="tool-item tool-divider" />
      <div
        class="tool-item user-tool"
        :title="userName || '用户'"
        @click="userDropdownVisible = !userDropdownVisible"
      >
        <div class="user-avatar-small">
          {{ userAvatarText }}
        </div>
      </div>
    </div>

    <!-- 用户下拉菜单 -->
    <n-dropdown
      :show="userDropdownVisible"
      :options="userDropdownOptions"
      placement="right-end"
      @select="handleUserSelect"
      @clickoutside="userDropdownVisible = false"
    >
      <div class="dropdown-anchor" />
    </n-dropdown>

    <!-- 导航菜单抽屉 -->
    <DrawerMenu v-model:show="menuDrawerVisible" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import TheLogo from '@/components/common/TheLogo.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu, useUser } from '@/composables'
import { MessageNotification } from '@/layouts/components'
import { useAppStore, usePermissionStore } from '@/store'
import DrawerMenu from '../../immersive/components/DrawerMenu.vue'

const route = useRoute()
const permissionStore = usePermissionStore()
const appStore = useAppStore()
const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

const menuDrawerVisible = ref(false)

const { userName, userAvatarText, userDropdownOptions, dropdownVisible: userDropdownVisible, handleDropdownSelect } = useUser()

// Extract top shortcut menus (first level)
const topMenus = computed(() => {
  const menus = permissionStore.menus || []
  return menus.slice(0, 8).map((item) => {
    return {
      ...item,
      iconClass: item.icon || (item.children?.length ? 'ionicons5:FolderOutline' : 'ionicons5:DocumentTextOutline'),
    }
  })
})

// Compute active top menu key based on current route
const activeKey = computed(() => {
  const menus = permissionStore.menus || []
  const current = menus.find(item => containsPath(item, route.path))
  return current?.key || current?.id || null
})

function containsPath(item, path) {
  return item.path === path || (item.children || []).some(child => containsPath(child, path))
}

function handleMenuSelect(item) {
  // 一级目录没有页面路由，打开完整菜单，不能让快捷图标点了无响应。
  if (item.children?.length) {
    menuDrawerVisible.value = true
    return
  }
  if (item.path) {
    baseHandleMenuSelect(item.key || item.id, item.path)
  }
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  }
  else {
    document.exitFullscreen()
  }
}

function handleUserSelect(key) {
  handleDropdownSelect(key)
}
</script>

<style scoped>
.bento-rail {
  width: 56px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--side-menu-bg-color);
  border-right: 1px solid var(--side-menu-border-color);
  flex-shrink: 0;
  padding: 6px 0;
}

.bento-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin: 2px auto 8px;
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: background var(--transition-fast);
}

.bento-logo:hover {
  background: var(--bg-secondary);
}

.bento-menu-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin: 0 auto 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 20px;
  color: var(--side-menu-text-color);
  padding: 0;
  border: 0;
  background: transparent;
  transition: all var(--transition-fast);
}

.bento-menu-trigger:hover {
  background: var(--side-menu-bg-color-hover);
  color: var(--side-menu-text-color-hover);
}

/* 快捷菜单 */
.bento-quick-links {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 9px;
  overflow-y: auto;
}

.quick-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin: 0 auto;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 17px;
  color: var(--side-menu-text-color);
  padding: 0;
  border: 0;
  background: transparent;
  transition: all var(--transition-fast);
}

.quick-link:hover {
  background: var(--side-menu-bg-color-hover);
  color: var(--side-menu-text-color-hover);
}

.quick-link.active {
  background: var(--side-menu-bg-color-active);
  color: var(--side-menu-text-color-active);
}

/* 底部工具 */
.bento-tools {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 0;
}

.tool-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 18px;
  color: var(--side-menu-text-color);
  transition: all var(--transition-fast);
  position: relative;
}

.tool-item:hover {
  background: var(--side-menu-bg-color-hover);
  color: var(--side-menu-text-color-hover);
}

.tool-divider {
  height: 1px !important;
  width: 24px;
  margin: 4px auto;
  background: var(--side-menu-border-color);
  border-radius: 0;
  cursor: default;
  color: transparent;
}

.tool-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  color: white;
  background: var(--error-500);
  border-radius: 8px;
}

.user-avatar-small {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary-500);
  color: white;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-tool {
  cursor: pointer;
}

/* 滚动条 */
.bento-quick-links::-webkit-scrollbar {
  width: 2px;
}

.bento-quick-links::-webkit-scrollbar-track {
  background: transparent;
}

.bento-quick-links::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 2px;
}
</style>
