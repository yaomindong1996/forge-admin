<template>
  <view class="ai-pull-list" :style="{ height }">
    <scroll-view
      class="ai-pull-list__scroll"
      scroll-y
      :show-scrollbar="false"
      :lower-threshold="lowerThreshold"
      :refresher-enabled="refresherEnabled"
      :refresher-triggered="refreshing"
      @refresherrefresh="$emit('refresh')"
      @scrolltolower="handleLoad"
      @scroll="$emit('scroll', $event)"
    >
      <slot name="header" />

      <view v-if="firstLoading" class="ai-pull-list__state">
        <slot name="skeleton">
          <AiSkeleton type="list" :rows="skeletonRows" />
        </slot>
      </view>

      <view v-else-if="error" class="ai-pull-list__state">
        <slot name="error">
          <AiResult
            type="network"
            :title="errorTitle"
            :description="errorDescription"
            primary-text="重试"
            @primary="$emit('retry')"
          />
        </slot>
      </view>

      <view v-else-if="isEmpty" class="ai-pull-list__state">
        <slot name="empty">
          <AiEmpty :title="emptyTitle" :description="emptyDescription" :icon="emptyIcon" />
        </slot>
      </view>

      <view v-else class="ai-pull-list__items">
        <view v-for="(item, index) in list" :key="resolveKey(item, index)" class="ai-pull-list__item">
          <slot :item="item" :index="index" />
        </view>
      </view>

      <view v-if="!firstLoading && !error && list.length > 0" class="ai-pull-list__footer">
        <slot name="footer" :loading="loading" :finished="finished">
          <view v-if="loading" class="ai-pull-list__loading">
            <view class="ai-pull-list__spinner" />
            <text>{{ loadingText }}</text>
          </view>
          <text v-else-if="finished" class="ai-pull-list__finished">{{ finishedText }}</text>
        </slot>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiEmpty from './AiEmpty.vue'
import AiResult from './AiResult.vue'
import AiSkeleton from './AiSkeleton.vue'

const props = defineProps({
  list: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  refreshing: {
    type: Boolean,
    default: false
  },
  finished: {
    type: Boolean,
    default: false
  },
  error: {
    type: [Boolean, Object, String],
    default: false
  },
  height: {
    type: String,
    default: '100%'
  },
  itemKey: {
    type: String,
    default: 'id'
  },
  lowerThreshold: {
    type: Number,
    default: 80
  },
  refresherEnabled: {
    type: Boolean,
    default: true
  },
  skeletonRows: {
    type: Number,
    default: 4
  },
  emptyTitle: {
    type: String,
    default: '暂无数据'
  },
  emptyDescription: {
    type: String,
    default: '当前没有可展示的内容。'
  },
  emptyIcon: {
    type: String,
    default: 'inbox'
  },
  errorTitle: {
    type: String,
    default: '加载失败'
  },
  errorDescription: {
    type: String,
    default: '请检查网络后重试。'
  },
  loadingText: {
    type: String,
    default: '加载中...'
  },
  finishedText: {
    type: String,
    default: '没有更多了'
  }
})

const emit = defineEmits(['refresh', 'load', 'retry', 'scroll'])

const isEmpty = computed(() => !props.loading && props.list.length === 0)
const firstLoading = computed(() => props.loading && props.list.length === 0)

function resolveKey(item, index) {
  return item?.[props.itemKey] ?? index
}

function handleLoad() {
  if (!props.loading && !props.finished && !props.error) {
    emit('load')
  }
}
</script>

<style lang="scss" scoped>
.ai-pull-list {
  min-height: 0;
}

.ai-pull-list__scroll {
  width: 100%;
  height: 100%;
}

.ai-pull-list__items {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.ai-pull-list__state {
  padding: 16rpx 0;
}

.ai-pull-list__footer {
  display: flex;
  min-height: 104rpx;
  align-items: center;
  justify-content: center;
  color: #86909c;
  font-size: 24rpx;
  font-weight: 500;
}

.ai-pull-list__loading {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.ai-pull-list__spinner {
  width: 28rpx;
  height: 28rpx;
  border: 3rpx solid #86909c;
  border-right-color: transparent;
  border-radius: 999rpx;
  animation: ai-pull-list-spin 0.8s linear infinite;
}

.ai-pull-list__finished {
  color: #86909c;
}

@keyframes ai-pull-list-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
