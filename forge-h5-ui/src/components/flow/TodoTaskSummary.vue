<template>
  <view class="task-summary">
    <text class="task-title">{{ title }}</text>
    <text class="task-node">{{ task.taskName || task.name || '审批节点' }}</text>
    <view class="task-facts">
      <view class="task-fact"><text>申请人</text><text>{{ task.startUserName || task.createByName || '-' }}</text></view>
      <view class="task-fact"><text>发起部门</text><text>{{ task.startDeptName || '-' }}</text></view>
      <view class="task-fact"><text>流程分类</text><text>{{ task.categoryName || task.category || '-' }}</text></view>
      <view class="task-fact"><text>提交时间</text><text>{{ task.createTime || task.startTime || '-' }}</text></view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ task: { type: Object, default: () => ({}) } })
const title = computed(() => props.task.title || props.task.businessTitle || props.task.processName || props.task.processDefinitionName || props.task.taskName || '审批任务')
</script>

<style lang="scss" scoped>
.task-summary { margin: 32rpx 32rpx 0; padding: 32rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.task-title, .task-node, .task-fact text { display: block; }
.task-title { color: var(--text-strong); font-size: 32rpx; font-weight: 500; line-height: 1.4; }
.task-node { margin-top: 8rpx; color: var(--primary-color); font-size: 22rpx; }
.task-facts { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 14rpx 20rpx; margin-top: 18rpx; padding-top: 16rpx; border-top: 1rpx solid var(--border-light); }
.task-fact { min-width: 0; }
.task-fact text:first-child { color: var(--text-muted); font-size: 19rpx; }
.task-fact text:last-child { overflow: hidden; margin-top: 4rpx; color: var(--text-secondary); font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }

@media (min-width: 1024px) {
  .task-summary {
    width: calc(100% - 48px);
    max-width: 1280px;
    margin: 24px auto 0;
    padding: 24px;
    box-sizing: border-box;
  }
}
</style>
