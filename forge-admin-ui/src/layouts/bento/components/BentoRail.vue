<template>
  <div class="bento-rail">
    <!-- Logo -->
    <router-link to="/" class="bento-logo" title="首页">
      <TheLogo />
    </router-link>

    <!-- 菜单触发按钮 -->
    <div
      class="bento-menu-trigger"
      title="打开菜单"
      @click="menuDrawerVisible = true"
    >
      <i class="i-ion-menu" />
    </div>

    <!-- 顶部菜单快捷入口 -->
    <div class="bento-quick-links">
      <div
        v-for="item in topMenus"
        :key="item.key"
        class="quick-link"
        :class="{ active: item.key === activeKey }"
        :title="item.label"
        @click="handleMenuSelect(item)"
      >
        <i :class="item.iconClass" />
      </div>
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
        :title="userStore.userInfo?.realName || '用户'"
        @click="userDropdownVisible = !userDropdownVisible"
      >
        <div class="user-avatar-small">
          {{ userAvatar }}
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
import { computed, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore, useAuthStore, usePermissionStore, useAppStore } from '@/store'
import api from '@/api'
import TheLogo from '@/components/common/TheLogo.vue'
import { MessageNotification } from '@/layouts/components'
import DrawerMenu from '../../immersive/components/DrawerMenu.vue'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const menuDrawerVisible = ref(false)
const userDropdownVisible = ref(false)

const userAvatar = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

// 提取顶部常用菜单（第一级菜单）
const topMenus = computed(() => {
  const menus = permissionStore.menus || []
  return menus.slice(0, 8).map((item) => {
    const iconMap = {
      system: 'i-ai-icon:settings',
      generator: 'i-ai-icon:code',
      flow: 'i-ai-icon:flow',
      message: 'i-ai-icon:bell',
      monitor: 'i-ai-icon:monitor',
      job: 'i-ai-icon:clock',
      file: 'i-ai-icon:file',
    }
    const iconKey = (item.path || '').split('/')[1] || ''
    return {
      ...item,
      iconClass: item.icon || iconMap[iconKey] || 'i-ai-icon:grid',
    }
  })
})

function handleMenuSelect(item) {
  if (item.path) {
    router.push(item.path)
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

const userDropdownOptions = computed(() => [
  { label: '个人信息', key: 'profile', icon: () => h('i', { class: 'i-material-symbols:person-outline' }) },
  { label: '修改密码', key: 'password', icon: () => h('i', { class: 'i-material-symbols:lock-outline' }) },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: () => h('i', { class: 'i-material-symbols:logout' }) },
])

function handleUserSelect(key) {
  userDropdownVisible.value = false
  if (key === 'logout') {
    $dialog.confirm({
      title: '提示',
      type: 'info',
      content: '确认退出？',
      async confirm() {
        try {
          await api.logout()
        }
        catch (error) {
          console.error(error)
        }
        authStore.logout()
        $message.success('已退出登录')
      },
    })
  }
  else if (key === 'profile') {
    router.push('/system/user-profile')
  }
  else if (key === 'password') {
    router.push('/system/password')
  }
}
</script>

<style scoped>
.bento-rail {
  width: 56px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-right: 1px solid var(--border-light);
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
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.bento-menu-trigger:hover {
  background: var(--bg-secondary);
  color: var(--primary-500);
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
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.quick-link:hover {
  background: var(--bg-secondary);
  color: var(--primary-500);
}

.quick-link.active {
  background: var(--primary-50);
  color: var(--primary-500);
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
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  position: relative;
}

.tool-item:hover {
  background: var(--bg-secondary);
  color: var(--primary-500);
}

.tool-divider {
  height: 1px !important;
  width: 24px;
  margin: 4px auto;
  background: var(--border-light);
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
