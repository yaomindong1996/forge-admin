<template>
  <view class="runtime-flow-timeline">
    <text class="runtime-flow-timeline__title">{{ title }}</text>
    <view v-if="loading" class="runtime-flow-timeline__empty">正在加载审批记录</view>
    <view v-else v-for="(item, index) in items" :key="itemKey(item, index)" class="runtime-flow-timeline__item">
      <view class="runtime-flow-timeline__dot" />
      <view class="runtime-flow-timeline__copy">
        <text>{{ item.activityName || item.taskName || item.name || '流程节点' }}</text>
        <text>{{ item.assigneeName || item.userName || item.operatorName || '-' }} · {{ item.endTime || item.createTime || item.startTime || '-' }}</text>
        <text v-if="item.comment">{{ item.comment }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: { type: String, default: '审批记录' },
  loading: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
})
function itemKey(item, index) { return item.id || item.taskId || `${item.activityName || item.taskName || 'node'}:${index}` }
</script>

<style lang="scss" scoped>
.runtime-flow-timeline { margin-bottom: 32rpx; padding: 32rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.runtime-flow-timeline__title { display: block; margin-bottom: 20rpx; color: #1d2129; font-size: 28rpx; font-weight: 500; }
.runtime-flow-timeline__empty { color: #86909c; font-size: 23rpx; }
.runtime-flow-timeline__item { position: relative; display: flex; gap: 18rpx; padding-bottom: 22rpx; }
.runtime-flow-timeline__item:not(:last-child)::before { position: absolute; top: 16rpx; bottom: 0; left: 7rpx; width: 2rpx; background: #e5e6eb; content: ''; }
.runtime-flow-timeline__dot { position: relative; z-index: 1; width: 16rpx; height: 16rpx; margin-top: 7rpx; border-radius: 50%; background: #4266f7; }
.runtime-flow-timeline__copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 6rpx; color: #4e5969; font-size: 22rpx; line-height: 1.5; }
.runtime-flow-timeline__copy text:first-child { color: #1d2129; font-size: 25rpx; font-weight: 500; }
</style>
