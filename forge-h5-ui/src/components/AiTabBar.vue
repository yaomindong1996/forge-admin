<template>
  <view class="ai-tabbar-host">
    <!-- 悬浮胶囊底栏：选中项为灰色底块 + 深色图标 -->
    <view class="ai-tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="ai-tabbar__item"
        :class="{ 'is-active': currentKey === tab.key }"
        @click="handleTabClick(tab)"
      >
        <view class="ai-tabbar__icon-wrap">
          <view class="ai-tabbar__icon" :style="iconMask(tab.icon, currentKey === tab.key ? ACTIVE_COLOR : IDLE_COLOR)" />
          <text v-if="badgeText(tab)" class="ai-tabbar__badge">{{ badgeText(tab) }}</text>
        </view>
        <text class="ai-tabbar__label">{{ tab.label }}</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useBadgeStore } from '@/store'
import { resolveStaticUrl } from '@/utils/assets'

const props = defineProps({
  active: {
    type: String,
    default: '',
  },
})

const ACTIVE_COLOR = '#171a1d'
const IDLE_COLOR = '#747677'

const tabs = [
  { key: 'message', label: '消息', path: '/pages/message/index', icon: '/static/icons/ai-icon/message-circle.svg', badge: 'messageTabText' },
  { key: 'todo', label: '待办', path: '/pages/todo', icon: '/static/icons/ai-icon/check-square.svg', badge: 'todoText' },
  { key: 'home', label: '工作台', path: '/pages/index/index', icon: '/static/icons/ai-icon/grid.svg' },
  { key: 'contacts', label: '通讯录', path: '/pages/contacts/index', icon: '/static/icons/ai-icon/users.svg' },
  { key: 'mine', label: '我的', path: '/pages/mine/index', icon: '/static/icons/ai-icon/user.svg' },
]

const badgeStore = useBadgeStore()

const currentKey = computed(() => {
  if (props.active) {
    return props.active
  }
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route || ''
  const matched = tabs.find(tab => route === tab.path.replace(/^\//, ''))
  return matched?.key || 'home'
})

onMounted(() => {
  hideNativeTabBar()
})

onShow(() => {
  hideNativeTabBar()
  badgeStore.refresh()
})

function badgeText(tab) {
  return tab.badge ? badgeStore[tab.badge] : ''
}

function hideNativeTabBar() {
  if (typeof uni === 'undefined' || typeof uni.hideTabBar !== 'function') {
    return
  }
  uni.hideTabBar({
    animation: false,
    fail: () => {},
  })
}

function iconMask(icon, color) {
  const url = resolveStaticUrl(icon)
  return {
    backgroundColor: color,
    WebkitMask: `url(${url}) center / contain no-repeat`,
    mask: `url(${url}) center / contain no-repeat`,
  }
}

function handleTabClick(tab) {
  if (tab.key === currentKey.value) {
    return
  }
  uni.switchTab({ url: tab.path })
}
</script>

<style lang="scss" scoped>
.ai-tabbar-host {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 80;
  display: flex;
  justify-content: center;
  padding: 0 12px calc(8px + env(safe-area-inset-bottom));
  pointer-events: none;
}

.ai-tabbar {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 520px;
  height: 58px;
  padding: 0 4px;
  border-radius: 29px;
  background: var(--forge-surface);
  box-shadow: var(--forge-shadow-float);
  pointer-events: auto;
}

.ai-tabbar__item {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50px;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 25px;
  background: transparent;
  line-height: 1;
  transition: background 0.15s ease;
}

.ai-tabbar__item::after {
  display: none;
}

.ai-tabbar__item.is-active {
  background: var(--forge-surface-muted);
}

.ai-tabbar__icon-wrap {
  position: relative;
  width: 22px;
  height: 22px;
}

.ai-tabbar__icon {
  width: 22px;
  height: 22px;
  transition: background-color 0.15s ease;
}

.ai-tabbar__badge {
  position: absolute;
  top: -6px;
  left: 14px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border: 1.5px solid var(--forge-surface);
  border-radius: 9px;
  background: var(--forge-color-danger);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  box-sizing: content-box;
}

.ai-tabbar__label {
  margin-top: 4px;
  color: var(--forge-text-secondary);
  font-size: 10px;
}

.ai-tabbar__item.is-active .ai-tabbar__label {
  color: var(--forge-text-primary);
  font-weight: 600;
}
</style>
