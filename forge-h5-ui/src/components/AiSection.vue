<template>
  <view
    class="ai-section"
    :class="[
      `ai-section--${variant}`,
      {
        'ai-section--no-padding': noPadding,
        'ai-section--compact': compact
      }
    ]"
  >
    <view v-if="title || desc || $slots.extra" class="ai-section__head">
      <view class="ai-section__copy">
        <text v-if="title" class="ai-section__title">{{ title }}</text>
        <text v-if="desc" class="ai-section__desc">{{ desc }}</text>
      </view>
      <view v-if="$slots.extra" class="ai-section__extra">
        <slot name="extra" />
      </view>
    </view>
    <slot />
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  desc: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'glass',
    validator: value => ['glass', 'plain', 'solid'].includes(value)
  },
  noPadding: {
    type: Boolean,
    default: false
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.ai-section {
  overflow: hidden;
  padding: 32rpx;
  border: 1rpx solid var(--border-color);
  border-radius: var(--radius-card);
  background: #fff;
}

.ai-section--glass {
  background: #fff;
}

.ai-section--solid {
  background: #ffffff;
}

.ai-section--plain {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.ai-section--no-padding {
  padding: 0;
}

.ai-section--compact {
  padding: 24rpx;
}

.ai-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.ai-section__copy {
  min-width: 0;
  flex: 1;
}

.ai-section__title,
.ai-section__desc {
  display: block;
  min-width: 0;
}

.ai-section__title {
  overflow: hidden;
  color: var(--text-strong);
  font-size: 32rpx;
  font-weight: 500;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-section__desc {
  margin-top: 8rpx;
  color: var(--text-muted);
  font-size: 26rpx;
  font-weight: 400;
  line-height: 1.45;
}

.ai-section__extra {
  flex-shrink: 0;
}
</style>
