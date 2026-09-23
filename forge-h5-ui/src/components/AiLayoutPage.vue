<template>
  <AiPageShell :grid="grid" :safe-bottom="safeBottom">
    <view class="ai-layout-page" :class="{ 'ai-layout-page--fixed': fixed }">
      <view v-if="showNav" class="ai-layout-page__nav" :class="{ 'ai-layout-page__nav--glass': navGlass }">
        <slot name="nav">
          <button v-if="showBack" class="ai-layout-page__back" @click="handleBack">
            <AiIcon name="chevron-left" color="#4E5969" size="md" />
          </button>
          <view class="ai-layout-page__title-block">
            <text v-if="title" class="ai-layout-page__title">{{ title }}</text>
            <text v-if="subtitle" class="ai-layout-page__subtitle">{{ subtitle }}</text>
          </view>
          <view class="ai-layout-page__nav-extra">
            <slot name="navRight" />
          </view>
        </slot>
      </view>

      <scroll-view
        class="ai-layout-page__body"
        :class="{ 'ai-layout-page__body--scroll': scroll }"
        :scroll-y="scroll"
        :show-scrollbar="false"
        @scrolltolower="$emit('scrolltolower')"
        @refresherrefresh="$emit('refresh')"
      >
        <view class="ai-layout-page__inner" :class="{ 'ai-layout-page__inner--padded': padded }">
          <slot />
        </view>
      </scroll-view>

      <view v-if="$slots.footer" class="ai-layout-page__footer" :class="{ 'ai-layout-page__footer--glass': footerGlass }">
        <slot name="footer" />
      </view>
    </view>
  </AiPageShell>
</template>

<script setup>
import AiIcon from './AiIcon.vue'
import AiPageShell from './AiPageShell.vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  showNav: {
    type: Boolean,
    default: true
  },
  showBack: {
    type: Boolean,
    default: true
  },
  backUrl: {
    type: String,
    default: ''
  },
  navGlass: {
    type: Boolean,
    default: true
  },
  footerGlass: {
    type: Boolean,
    default: true
  },
  padded: {
    type: Boolean,
    default: true
  },
  scroll: {
    type: Boolean,
    default: true
  },
  fixed: {
    type: Boolean,
    default: true
  },
  grid: {
    type: Boolean,
    default: true
  },
  safeBottom: {
    type: Boolean,
    default: false
  }
})

defineEmits(['back', 'scrolltolower', 'refresh'])

function handleBack() {
  const pages = getCurrentPages()
  if (props.backUrl) {
    uni.navigateTo({ url: props.backUrl })
    return
  }
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({
    url: '/pages/index/index',
    fail: () => uni.reLaunch({ url: '/pages/index/index' }),
  })
}
</script>

<style lang="scss" scoped>
.ai-layout-page {
  position: relative;
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.ai-layout-page--fixed {
  height: 100vh;
  overflow: hidden;
}

.ai-layout-page__nav {
  position: relative;
  z-index: 3;
  display: flex;
  min-height: 92rpx;
  align-items: center;
  gap: 16rpx;
  padding: calc(14rpx + env(safe-area-inset-top)) 32rpx 14rpx;
  box-sizing: border-box;
}

.ai-layout-page__nav--glass {
  border-bottom: 1rpx solid var(--border-color);
  background: #fff;
}

.ai-layout-page__back {
  display: flex;
  width: 88rpx;
  height: 88rpx;
  flex: 0 0 88rpx;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 1rpx solid var(--border-color);
  border-radius: var(--radius-control);
  background: #fff;
  box-shadow: none;
}

.ai-layout-page__back::after {
  border: 0;
}

.ai-layout-page__title-block {
  min-width: 0;
  flex: 1;
}

.ai-layout-page__title,
.ai-layout-page__subtitle {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-layout-page__title {
  color: var(--text-strong);
  font-size: 36rpx;
  font-weight: 500;
  line-height: 1.25;
}

.ai-layout-page__subtitle {
  margin-top: 6rpx;
  color: var(--text-muted);
  font-size: 26rpx;
  font-weight: 400;
}

.ai-layout-page__nav-extra {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.ai-layout-page__body {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
}

.ai-layout-page__body--scroll {
  height: 0;
}

.ai-layout-page__inner {
  width: 100%;
  max-width: var(--forge-page-max-width, 1280px);
  margin: 0 auto;
  box-sizing: border-box;
}

.ai-layout-page__inner--padded {
  padding: 32rpx;
}

.ai-layout-page__footer {
  position: relative;
  z-index: 3;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.ai-layout-page__footer--glass {
  border-top: 1rpx solid var(--border-color);
  background: #fff;
  box-shadow: none;
}

@media (min-width: 1024px) {
  .ai-layout-page__nav {
    padding-right: max(24px, calc((100vw - 1280px) / 2 + 24px));
    padding-left: max(24px, calc((100vw - 1280px) / 2 + 24px));
  }

  .ai-layout-page__inner--padded {
    padding: 24px;
  }

  .ai-layout-page__footer {
    padding-right: max(24px, calc((100vw - 1280px) / 2 + 24px));
    padding-left: max(24px, calc((100vw - 1280px) / 2 + 24px));
  }
}
</style>
