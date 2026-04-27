<template>
  <div class="nexus-sidebar">
    <!-- Logo 区域 -->
    <div class="nexus-logo">
      <div class="logo-icon">
        <TheLogo />
      </div>
      <span class="logo-text">Forge Admin</span>
    </div>

    <!-- 菜单区域 -->
    <nav class="nexus-nav">
      <template v-for="item in menuItems" :key="item.key">
        <!-- 有子项：折叠菜单 -->
        <div v-if="item.children && item.children.length" class="nav-collapsible">
          <button
            class="nav-collapsible-btn"
            :class="{ active: isCollapsibleActive(item), expanded: isExpanded(item) }"
            @click="toggleExpand(item)"
          >
            <div class="nav-btn-content">
              <IconRenderer
                v-if="item.icon"
                :icon="item.icon"
                :size="18"
                class="nav-icon"
              />
              <span class="nav-label">{{ item.label }}</span>
            </div>
            <i
              class="i-material-symbols:chevron-right nav-chevron"
              :class="{ expanded: isExpanded(item) }"
            />
          </button>
          <transition name="nav-collapse">
            <div v-if="isExpanded(item)" class="nav-children">
              <div class="nav-children-line" />
              <button
                v-for="child in item.children"
                :key="child.key"
                class="nav-sub-btn"
                :class="{ active: isActive(child) }"
                @click="handleNavClick(child)"
              >
                <span class="nav-sub-dot" :class="{ active: isActive(child) }" />
                <span class="nav-sub-label">{{ child.label }}</span>
              </button>
            </div>
          </transition>
        </div>

        <!-- 无子项：普通菜单项 -->
        <button
          v-else
          class="nav-item"
          :class="{ active: isActive(item) }"
          @click="handleNavClick(item)"
        >
          <div v-if="isActive(item)" class="nav-item-bg" />
          <IconRenderer
            v-if="item.icon"
            :icon="item.icon"
            :size="18"
            class="nav-icon"
          />
          <span class="nav-label">{{ item.label }}</span>
        </button>
      </template>
    </nav>

    <!-- 底部工具区 -->
    <div class="nexus-user">
      <MenuCollapse />
      <n-dropdown
        :show="userDropdownVisible"
        :options="userDropdownOptions"
        placement="top-start"
        @select="handleUserSelect"
        @clickoutside="userDropdownVisible = false"
      >
        <div class="user-info" @click="userDropdownVisible = !userDropdownVisible">
          <img
            v-if="userAvatar"
            :src="userAvatar"
            alt="avatar"
            class="user-avatar-img"
          />
          <div v-else class="user-avatar">
            {{ userAvatarText }}
          </div>
          <div class="user-details">
            <span class="user-name">{{ userName }}</span>
          </div>
        </div>
      </n-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePermissionStore } from '@/store'
import TheLogo from '@/components/common/TheLogo.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import MenuCollapse from '@/layouts/components/MenuCollapse.vue'
import { useUser } from '@/composables'

const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()

const { userName, userAvatarText, userAvatar, userDropdownOptions, dropdownVisible: userDropdownVisible, handleDropdownSelect } = useUser()

const expandedKeys = ref(new Set())

// Process menu data (simple pass-through with key normalization)
const menuItems = computed(() => {
  const menus = permissionStore.menus || []
  return menus.map((item) => ({
    key: item.key || String(item.id),
    name: item.name || item.label || '',
    label: item.label || item.name || item.meta?.title || '',
    path: item.path || '',
    icon: item.icon || '',
    children: (item.children || []).map((child) => ({
      key: child.key || String(child.id),
      name: child.name || child.label || '',
      label: child.label || child.name || child.meta?.title || '',
      path: child.path || '',
      icon: child.icon || '',
    })),
  }))
})

// Check if menu item is active
function isActive(item) {
  if (!item.path)
    return false
  const currentPath = route.path
  if (item.path === currentPath)
    return true
  if (item.path !== '/' && item.path !== currentPath) {
    const normalizedPath = item.path.endsWith('/') ? item.path : item.path + '/'
    const normalizedCurrent = currentPath.endsWith('/') ? currentPath : currentPath + '/'
    if (normalizedCurrent.startsWith(normalizedPath))
      return true
  }
  return false
}

// Check if collapsible menu has active child
function isCollapsibleActive(item) {
  if (!item.children)
    return false
  return item.children.some(child => isActive(child))
}

// Check if menu is expanded
function isExpanded(item) {
  return expandedKeys.value.has(item.key)
}

// Toggle expand/collapse
function toggleExpand(item) {
  if (expandedKeys.value.has(item.key)) {
    expandedKeys.value.delete(item.key)
  }
  else {
    expandedKeys.value.add(item.key)
  }
}

// Handle menu click
function handleNavClick(item) {
  if (item.path) {
    router.push({ path: item.path })
  }
}

// Auto-expand menus with active children
watch(
  () => route.path,
  () => {
    menuItems.value.forEach((item) => {
      if (item.children && isCollapsibleActive(item)) {
        expandedKeys.value.add(item.key)
      }
    })
  },
  { immediate: true },
)

function handleUserSelect(key) {
  handleDropdownSelect(key)
}
</script>

<style scoped>
.nexus-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Logo */
.nexus-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
  justify-content: center;
}

.logo-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.logo-icon :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-text {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
}

/* 导航菜单 */
.nexus-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 12px 8px;
}

/* 普通菜单项 */
.nav-item {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: var(--nexus-hover-bg);
  color: var(--text-primary);
}

.nav-item.active {
  color: var(--nexus-active-text);
}

.nav-item-bg {
  position: absolute;
  inset: 0;
  background: var(--nexus-active-bg);
  border: 1px solid rgba(22, 93, 255, 0.1);
  border-radius: 8px;
}

.nav-item .nav-icon {
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.nav-item.active .nav-icon :deep(svg) {
  color: var(--primary-500);
}

.nav-label {
  flex: 1;
  text-align: left;
  position: relative;
  z-index: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-item.active .nav-label {
  font-weight: 600;
  color: var(--nexus-active-text);
}

/* 可折叠菜单 */
.nav-collapsible {
  margin-bottom: 2px;
}

.nav-collapsible-btn {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.nav-collapsible-btn:hover {
  background: var(--nexus-hover-bg);
  color: var(--text-primary);
}

.nav-collapsible-btn.active {
  color: var(--text-primary);
}

.nav-btn-content {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.nav-chevron {
  font-size: 16px;
  color: var(--text-tertiary);
  transition: transform var(--transition-base);
  flex-shrink: 0;
}

.nav-chevron.expanded {
  transform: rotate(90deg);
}

/* 子菜单 */
.nav-children {
  position: relative;
  padding: 2px 0 2px 28px;
}

.nav-children-line {
  position: absolute;
  left: 14px;
  top: 0;
  bottom: 8px;
  width: 1px;
  background: var(--border-light);
}

.nav-sub-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 5px 8px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.nav-sub-btn:hover {
  background: var(--nexus-hover-bg);
  color: var(--text-secondary);
}

.nav-sub-btn.active {
  background: var(--nexus-active-bg);
  color: var(--nexus-active-text);
}

.nav-sub-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--border-default);
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.nav-sub-dot.active {
  background: var(--primary-500);
  transform: scale(1.3);
}

.nav-sub-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 折叠动画 */
.nav-collapse-enter-active,
.nav-collapse-leave-active {
  transition: all var(--transition-base);
  overflow: hidden;
}

.nav-collapse-enter-from,
.nav-collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

.nav-collapse-enter-to,
.nav-collapse-leave-from {
  opacity: 1;
  max-height: 200px;
}

/* 底部工具区 */
.nexus-user {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 12px;
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: background var(--transition-base);
  flex: 1;
  min-width: 0;
}

.user-info:hover {
  background: var(--nexus-hover-bg);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--primary-500);
  color: white;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(22, 93, 255, 0.2);
  flex-shrink: 0;
}

.user-avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 滚动条 */
.nexus-nav::-webkit-scrollbar {
  width: 4px;
}

.nexus-nav::-webkit-scrollbar-track {
  background: transparent;
}

.nexus-nav::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 2px;
}

.nexus-nav::-webkit-scrollbar-thumb:hover {
  background: var(--border-default);
}

/* ═══════════════════════════════════════
 * 深色模式适配
 * ═══════════════════════════════════════ */
:global(.dark) .nexus-logo {
  border-bottom-color: var(--nexus-border);
}

:global(.dark) .logo-text {
  color: var(--text-primary);
}

:global(.dark) .nexus-user {
  border-top-color: var(--nexus-border);
}

:global(.dark) .nexus-nav {
  background: transparent;
}

:global(.dark) .nav-item {
  color: #e2e8f0;
}

:global(.dark) .nav-item:hover {
  color: #f1f5f9;
  background: var(--nexus-hover-bg);
}

:global(.dark) .nav-item.active .nav-label {
  color: var(--nexus-active-text);
}

:global(.dark) .nav-item.active .nav-icon :deep(svg) {
  color: var(--primary-500);
}

:global(.dark) .nav-item .nav-icon {
  color: #94a3b8;
}

:global(.dark) .nav-item.active .nav-icon {
  color: var(--primary-500);
}

:global(.dark) .nav-collapsible-btn {
  color: #e2e8f0;
}

:global(.dark) .nav-collapsible-btn:hover {
  color: #f1f5f9;
  background: var(--nexus-hover-bg);
}

:global(.dark) .nav-collapsible-btn.active {
  color: #f1f5f9;
}

:global(.dark) .nav-btn-content {
  color: #e2e8f0;
}

:global(.dark) .nav-chevron {
  color: #64748b;
}

:global(.dark) .nav-children-line {
  background: var(--nexus-border);
}

:global(.dark) .nav-sub-btn {
  color: #64748b;
}

:global(.dark) .nav-sub-btn:hover {
  color: #cbd5e1;
  background: var(--nexus-hover-bg);
}

:global(.dark) .nav-sub-btn.active {
  color: var(--nexus-active-text);
  background: var(--nexus-active-bg);
}

:global(.dark) .nav-sub-dot {
  background: #475569;
}

:global(.dark) .nav-sub-dot.active {
  background: var(--primary-500);
}

:global(.dark) .nav-sub-label {
  color: #94a3b8;
}

:global(.dark) .user-name {
  color: var(--text-primary);
}

:global(.dark) .user-settings {
  color: #64748b;
}

:global(.dark) .user-settings:hover {
  color: var(--primary-500);
  background: var(--nexus-hover-bg);
}
</style>
