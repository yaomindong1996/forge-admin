<template>
  <view class="history-panel">
    <AiListSkeleton v-if="loading" :rows="4" compact />
    <view v-else-if="items.length" :class="mode === 'history' ? 'timeline' : 'process-nodes'">
      <view
        v-for="(item, index) in items"
        :key="itemKey(item, index)"
        :class="mode === 'history' ? 'timeline-item' : ['process-node', `is-${item.status || 'pending'}`]"
      >
        <view :class="mode === 'history' ? 'timeline-dot' : 'process-node__mark'" />
        <view :class="mode === 'history' ? 'timeline-copy' : 'process-node__copy'">
          <text :class="mode === 'history' ? 'timeline-title' : ''">{{ itemTitle(item) }}</text>
          <text :class="mode === 'history' ? 'timeline-meta' : ''">{{ itemMeta(item) }}</text>
          <text v-if="mode === 'history' && item.comment" class="timeline-comment">{{ item.comment }}</text>
        </view>
      </view>
    </view>
    <view v-else class="page-hint">{{ mode === 'history' ? '暂无审批记录' : '暂无可展示的流程节点' }}</view>
  </view>
</template>

<script setup>
import AiListSkeleton from '@/components/AiListSkeleton.vue'

defineProps({
  mode: { type: String, default: 'history' },
  loading: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
})

function itemKey(item, index) { return item.id || item.taskId || item.nodeId || `${item.activityName || item.taskName || 'node'}:${index}` }
function itemTitle(item) { return item.activityName || item.taskName || item.nodeName || item.name || '流程节点' }
function itemMeta(item) {
  if (Array.isArray(item.assigneeNames) && item.assigneeNames.length) return item.assigneeNames.join('、')
  const actor = item.assigneeName || item.userName || item.operatorName
  const time = item.endTime || item.createTime || item.startTime
  if (actor || time) return `${actor || '-'} · ${time || '-'}`
  return item.comment || item.statusText || item.status || '等待处理'
}
</script>

<style lang="scss" scoped>
.history-panel { margin: 24rpx; padding: 28rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.page-hint { padding: 80rpx 32rpx; color: var(--text-muted); font-size: 26rpx; text-align: center; }
.timeline, .process-nodes { padding: 4rpx 0; }
.timeline-item, .process-node { position: relative; display: flex; gap: 20rpx; padding-bottom: 28rpx; }
.timeline-item:not(:last-child)::before, .process-node:not(:last-child)::after { position: absolute; top: 20rpx; bottom: 0; left: 8rpx; width: 2rpx; background: #e5e7eb; content: ''; }
.timeline-dot { position: relative; z-index: 1; width: 18rpx; height: 18rpx; margin-top: 8rpx; border-radius: 50%; background: var(--primary-color); }
.timeline-copy, .process-node__copy { min-width: 0; flex: 1; }
.timeline-title, .timeline-meta, .timeline-comment, .process-node__copy text { display: block; }
.timeline-title, .process-node__copy text:first-child { color: var(--text-strong); font-size: 27rpx; font-weight: 600; }
.timeline-meta { margin-top: 8rpx; color: var(--text-muted); font-size: 23rpx; line-height: 1.5; }
.timeline-comment { margin-top: 12rpx; color: #4e5969; font-size: 24rpx; line-height: 1.5; }
.process-node { gap: 16rpx; padding-bottom: 24rpx; }
.process-node__mark { position: relative; z-index: 1; width: 18rpx; height: 18rpx; margin-top: 6rpx; border: 4rpx solid #cbd5e1; border-radius: 50%; background: #fff; box-sizing: border-box; }
.process-node.is-running .process-node__mark { border-color: #2563eb; background: #2563eb; box-shadow: 0 0 0 6rpx #dbeafe; }
.process-node.is-completed .process-node__mark { border-color: #16a34a; background: #16a34a; }
.process-node__copy text:last-child { overflow: hidden; margin-top: 6rpx; color: #64748b; font-size: 22rpx; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
</style>
