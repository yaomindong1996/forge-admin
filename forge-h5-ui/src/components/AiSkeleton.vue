<template>
  <view class="ai-skeleton" :class="[`ai-skeleton--${type}`, { 'ai-skeleton--animated': animated }]">
    <template v-if="type === 'profile'">
      <view class="ai-skeleton__avatar" />
      <view class="ai-skeleton__profile-main">
        <view class="ai-skeleton__line ai-skeleton__line--title" />
        <view class="ai-skeleton__line ai-skeleton__line--short" />
      </view>
    </template>

    <template v-else-if="type === 'card'">
      <view class="ai-skeleton__media" />
      <view class="ai-skeleton__line ai-skeleton__line--title" />
      <view
        v-for="item in rows"
        :key="item"
        class="ai-skeleton__line"
        :class="{ 'ai-skeleton__line--short': item === rows }"
      />
    </template>

    <template v-else>
      <view v-for="item in rows" :key="item" class="ai-skeleton__list-row">
        <view v-if="avatar" class="ai-skeleton__avatar ai-skeleton__avatar--sm" />
        <view class="ai-skeleton__list-main">
          <view class="ai-skeleton__line ai-skeleton__line--title" />
          <view class="ai-skeleton__line ai-skeleton__line--short" />
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'list',
    validator: value => ['list', 'card', 'profile'].includes(value)
  },
  rows: {
    type: Number,
    default: 3
  },
  avatar: {
    type: Boolean,
    default: true
  },
  animated: {
    type: Boolean,
    default: true
  }
})
</script>

<style lang="scss" scoped>
.ai-skeleton {
  width: 100%;
}

.ai-skeleton--card {
  padding: 24rpx;
  border: 1rpx solid var(--forge-border, #c9cdd4);
  border-radius: var(--forge-radius-card, 12rpx);
  background: var(--forge-surface, #ffffff);
  box-sizing: border-box;
}

.ai-skeleton--profile,
.ai-skeleton__list-row {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.ai-skeleton__list-row {
  min-height: 112rpx;
  padding: 22rpx 0;
}

.ai-skeleton__list-row + .ai-skeleton__list-row {
  border-top: 1rpx solid var(--forge-border-light, #e5e6eb);
}

.ai-skeleton__avatar,
.ai-skeleton__line,
.ai-skeleton__media {
  overflow: hidden;
  border-radius: 6rpx;
  background: #e5e6eb;
}

.ai-skeleton--animated .ai-skeleton__avatar,
.ai-skeleton--animated .ai-skeleton__line,
.ai-skeleton--animated .ai-skeleton__media {
  animation: ai-skeleton-pulse 1.1s ease-in-out infinite alternate;
}

.ai-skeleton__avatar {
  width: 96rpx;
  height: 96rpx;
  flex: 0 0 96rpx;
  border-radius: 8rpx;
}

.ai-skeleton__avatar--sm {
  width: 76rpx;
  height: 76rpx;
  flex-basis: 76rpx;
  border-radius: 8rpx;
}

.ai-skeleton__profile-main,
.ai-skeleton__list-main {
  min-width: 0;
  flex: 1;
}

.ai-skeleton__media {
  height: 180rpx;
  margin-bottom: 26rpx;
  border-radius: 8rpx;
}

.ai-skeleton__line {
  height: 24rpx;
  margin-top: 18rpx;
}

.ai-skeleton__line--title {
  width: 64%;
  height: 30rpx;
  margin-top: 0;
}

.ai-skeleton__line--short {
  width: 42%;
}

@keyframes ai-skeleton-pulse {
  0% {
    opacity: 0.65;
  }
  100% {
    opacity: 1;
  }
}
</style>
