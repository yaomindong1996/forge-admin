<template>
  <view v-if="visible" :class="rootClass" :style="rootStyle">
    <view v-if="fullScreen" class="loading-overlay" :class="{ 'loading-overlay--blur': blur }" />

    <view class="loading-card">
      <view class="loading-content" :class="themeClass">
        <view v-if="type === 'dots'" class="loading-dots">
          <view v-for="index in 3" :key="index" class="loading-dot" :style="{ animationDelay: `${(index - 1) * 0.15}s` }" />
        </view>

        <view v-else-if="type === 'brand'" class="loading-brand">
          <view class="loading-brand-glow" />
          <view class="loading-hexagon" />
        </view>

        <view v-else class="loading-spinner">
          <view class="loading-spinner-track" />
          <view class="loading-spinner-line" />
        </view>

        <text v-if="text" class="loading-text">{{ text }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: true,
  },
  text: {
    type: [String, Number],
    default: '加载中...',
  },
  type: {
    type: String,
    default: 'spinner',
    validator: value => ['spinner', 'dots', 'brand'].includes(value),
  },
  fullScreen: {
    type: Boolean,
    default: false,
  },
  fullscreen: {
    type: Boolean,
    default: undefined,
  },
  blur: {
    type: Boolean,
    default: true,
  },
  theme: {
    type: String,
    default: 'brand',
    validator: value => ['dark', 'light', 'brand'].includes(value),
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg'].includes(value),
  },
  zIndex: {
    type: Number,
    default: 200,
  },
})

const isFullScreen = computed(() => props.fullscreen ?? props.fullScreen)

const rootClass = computed(() => [
  'forge-loading',
  `forge-loading--${props.size}`,
  `forge-loading--${props.theme}`,
  isFullScreen.value ? 'forge-loading--fullscreen' : 'forge-loading--inline',
])

const themeClass = computed(() => `loading-content--${props.theme}`)

const rootStyle = computed(() => {
  if (!isFullScreen.value) {
    return {}
  }
  return {
    zIndex: props.zIndex,
  }
})
</script>

<style lang="scss" scoped>
.forge-loading {
  box-sizing: border-box;
}

.forge-loading--fullscreen {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
}

.forge-loading--inline {
  display: flex;
  width: 100%;
  min-height: 160rpx;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.2);
}

.loading-overlay--blur {
  backdrop-filter: none;
}

.forge-loading--light .loading-overlay {
  background: rgba(15, 23, 42, 0.4);
}

.forge-loading--dark .loading-overlay {
  background: rgba(255, 255, 255, 0.42);
}

.loading-card {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 240rpx;
  min-height: 192rpx;
  align-items: center;
  justify-content: center;
  padding: 48rpx 64rpx;
  border: 1rpx solid var(--border-color);
  border-radius: var(--radius-card);
  background: #fff;
}

.forge-loading--inline .loading-card {
  min-width: 0;
  min-height: 0;
  padding: 24rpx;
  border: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.forge-loading--light .loading-card {
  border-color: rgba(51, 65, 85, 0.26);
  background: rgba(15, 23, 42, 0.82);
}

.forge-loading--dark .loading-card {
  background: #fff;
  box-shadow: none;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  color: #4266f7;
}

.loading-content--light {
  color: #ffffff;
}

.loading-content--dark {
  color: #1d2129;
}

.loading-spinner,
.loading-spinner-track,
.loading-spinner-line {
  position: relative;
  box-sizing: border-box;
  border-radius: 999rpx;
}

.loading-spinner {
  width: 64rpx;
  height: 64rpx;
  animation: spinnerRotate 1s linear infinite;
}

.loading-spinner-track,
.loading-spinner-line {
  position: absolute;
  inset: 0;
  border: 8rpx solid currentColor;
}

.loading-spinner-track {
  opacity: 0.18;
}

.loading-spinner-line {
  border-color: transparent;
  border-top-color: currentColor;
  border-right-color: currentColor;
}

.loading-dots {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 8rpx 0;
}

.loading-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 999rpx;
  background: currentColor;
  animation: dotBounce 0.8s ease-in-out infinite;
}

.loading-brand {
  position: relative;
  display: flex;
  width: 64rpx;
  height: 64rpx;
  align-items: center;
  justify-content: center;
}

.loading-brand-glow {
  position: absolute;
  inset: 0;
  border-radius: 999rpx;
  background: currentColor;
  opacity: 0.24;
  filter: blur(18rpx);
  display: none;
}

.loading-hexagon {
  position: relative;
  width: 54rpx;
  height: 60rpx;
  background: currentColor;
  clip-path: polygon(25% 4%, 75% 4%, 100% 50%, 75% 96%, 25% 96%, 0 50%);
  animation: none;
}

.loading-hexagon::after {
  content: '';
  position: absolute;
  inset: 8rpx;
  background: rgba(255, 255, 255, 0.9);
  clip-path: polygon(25% 4%, 75% 4%, 100% 50%, 75% 96%, 25% 96%, 0 50%);
}

.forge-loading--light .loading-hexagon::after {
  background: rgba(15, 23, 42, 0.82);
}

.loading-text {
  color: currentColor;
  font-weight: 500;
  line-height: 1.35;
  text-align: center;
  letter-spacing: 0;
  animation: none;
}

.forge-loading--sm .loading-card {
  min-width: 180rpx;
  min-height: 144rpx;
  padding: 36rpx 48rpx;
}

.forge-loading--sm .loading-content {
  gap: 18rpx;
}

.forge-loading--sm .loading-spinner,
.forge-loading--sm .loading-brand {
  width: 40rpx;
  height: 40rpx;
}

.forge-loading--sm .loading-spinner-track,
.forge-loading--sm .loading-spinner-line {
  border-width: 6rpx;
}

.forge-loading--sm .loading-dot {
  width: 12rpx;
  height: 12rpx;
}

.forge-loading--sm .loading-hexagon {
  width: 34rpx;
  height: 38rpx;
}

.forge-loading--sm .loading-text {
  font-size: 24rpx;
}

.forge-loading--md .loading-text {
  font-size: 28rpx;
}

.forge-loading--lg .loading-card {
  min-width: 280rpx;
  min-height: 232rpx;
  padding: 56rpx 72rpx;
}

.forge-loading--lg .loading-content {
  gap: 28rpx;
}

.forge-loading--lg .loading-spinner,
.forge-loading--lg .loading-brand {
  width: 96rpx;
  height: 96rpx;
}

.forge-loading--lg .loading-dot {
  width: 24rpx;
  height: 24rpx;
}

.forge-loading--lg .loading-hexagon {
  width: 82rpx;
  height: 90rpx;
}

.forge-loading--lg .loading-text {
  font-size: 32rpx;
}

@keyframes spinnerRotate {
  to {
    transform: rotate(360deg);
  }
}

@keyframes dotBounce {
  0%,
  100% {
    opacity: 0.42;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-50%);
  }
}

@keyframes brandPulse {
  0%,
  100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.34;
    transform: scale(1.2);
  }
}

@keyframes loadingEnter {
  from {
    opacity: 0;
    transform: translateY(20rpx) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes textEnter {
  from {
    opacity: 0;
    transform: translateY(10rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
