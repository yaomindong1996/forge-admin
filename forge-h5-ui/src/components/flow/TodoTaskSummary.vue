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
.task-summary { margin: 24rpx; padding: 28rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.task-title, .task-node, .task-fact text { display: block; }
.task-title { color: var(--text-strong); font-size: 34rpx; font-weight: 600; line-height: 1.4; }
.task-node { margin-top: 12rpx; color: var(--primary-color); font-size: 25rpx; }
.task-facts { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18rpx 24rpx; margin-top: 24rpx; }
.task-fact { min-width: 0; }
.task-fact text:first-child { color: #94a3b8; font-size: 21rpx; }
.task-fact text:last-child { overflow: hidden; margin-top: 5rpx; color: #4e5969; font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
</style>
