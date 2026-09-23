<template>
  <view class="ai-tabbar-host">
    <view class="ai-tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="ai-tabbar__item"
        :class="{ 'is-active': currentKey === tab.key }"
        @click="handleTabClick(tab)"
      >
        <view v-if="currentKey === tab.key" class="ai-tabbar__active" />
        <view class="ai-tabbar__icon" :style="iconMask(tab.icon, currentKey === tab.key ? '#4266f7' : '#86909c')" />
        <text class="ai-tabbar__label">{{ tab.label }}</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { resolveStaticUrl } from '@/utils/assets'

const props = defineProps({
  active: {
    type: String,
    default: '',
  },
})

const tabs = [
  {
    key: 'home',
    label: '首页',
    path: '/pages/index/index',
    icon: '/static/icons/ai-icon/home.svg',
  },
  {
    key: 'todo',
    label: '待办',
    path: '/pages/todo',
    icon: '/static/icons/ai-icon/check-square.svg',
  },
  {
    key: 'mine',
    label: '我的',
    path: '/pages/mine/index',
    icon: '/static/icons/ai-icon/user.svg',
  },
]

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
})

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
  justify-content: stretch;
  padding: 0;
  pointer-events: none;
}

.ai-tabbar {
  display: flex;
  width: 100%;
  min-height: calc(104rpx + env(safe-area-inset-bottom));
  padding: 8rpx 24rpx env(safe-area-inset-bottom);
  border-top: 1rpx solid var(--border-color);
  background: #fff;
  pointer-events: auto;
}

.ai-tabbar__item {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  line-height: 1;
  transition: color 0.15s ease, background 0.15s ease;
}

.ai-tabbar__item::after {
  display: none;
}

.ai-tabbar__item:active {
  background: var(--surface-muted);
}

.ai-tabbar__active {
  position: absolute;
  top: -9rpx;
  left: 50%;
  width: 44rpx;
  height: 4rpx;
  background: var(--primary-color);
  transform: translateX(-50%);
}

.ai-tabbar__icon {
  position: relative;
  z-index: 1;
  width: 38rpx;
  height: 38rpx;
  transition: background-color 0.15s ease;
}

.ai-tabbar__label {
  position: relative;
  z-index: 1;
  margin-top: 8rpx;
  color: var(--text-muted);
  font-size: 20rpx;
  font-weight: 500;
}

.ai-tabbar__item.is-active .ai-tabbar__label {
  color: var(--primary-color);
  font-weight: 500;
}
</style>
