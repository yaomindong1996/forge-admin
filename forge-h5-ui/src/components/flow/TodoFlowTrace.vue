<template>
  <view class="trace-panel">
    <!-- 同一流程记录页中的分区标题 -->
    <view class="trace-heading">
      <text class="trace-heading__title">{{ mode === 'history' ? '办理记录' : '节点进度' }}</text>
      <text v-if="!loading && items.length" class="trace-heading__count">{{ items.length }} 项</text>
    </view>
    <AiListSkeleton v-if="loading" :rows="4" compact />
    <!-- 时间线：办理人头像 + 状态角标，竖线串联节点 -->
    <view v-else-if="items.length" class="trace-list">
      <view
        v-for="(item, index) in items"
        :key="itemKey(item, index)"
        class="trace-item"
        :class="`is-${itemTone(item)}`"
      >
        <view class="trace-avatar">
          <text>{{ avatarText(item) }}</text>
          <view class="trace-avatar__badge" />
        </view>
        <view class="trace-copy">
          <view class="trace-title-row">
            <text class="trace-title">{{ itemTitle(item) }}</text>
            <text v-if="itemTime(item)" class="trace-time">{{ itemTime(item) }}</text>
          </view>
          <view class="trace-sub-row">
            <text v-if="itemActor(item)" class="trace-actor">{{ itemActor(item) }}</text>
            <text class="trace-state" :class="itemTone(item)">
              {{ mode === 'history' ? formatFlowAction(item.action || item.status) : formatFlowStatus(item.status || item.statusText) }}
            </text>
          </view>
          <text v-if="mode === 'history' && item.comment" class="trace-comment">{{ item.comment }}</text>
        </view>
      </view>
    </view>
    <view v-else class="trace-empty">{{ mode === 'history' ? '暂无办理记录' : '暂无可展示的流程节点' }}</view>
  </view>
</template>

<script setup>
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import { contactInitials } from '@/utils/contacts'
import { flowStatusTone, formatFlowAction, formatFlowDateTime, formatFlowStatus } from '@/utils/flow-display'

const props = defineProps({
  mode: { type: String, default: 'history' },
  loading: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
})

function itemKey(item, index) { return item.id || item.taskId || item.nodeId || `${item.activityName || item.taskName || 'node'}:${index}` }
function itemTitle(item) { return item.activityName || item.taskName || item.nodeName || item.name || '流程节点' }
function actionTone(action) {
  const value = String(action || '').toLowerCase()
  if (['approve', 'approved', 'pass', 'passed', 'start'].includes(value)) return 'completed'
  if (['reject', 'rejected', 'terminate', 'terminated'].includes(value)) return 'exception'
  return 'pending'
}
function itemTone(item) {
  return props.mode === 'process' ? flowStatusTone(item.status) : actionTone(item.action || item.status)
}
function itemActor(item) {
  return Array.isArray(item.assigneeNames) && item.assigneeNames.length
    ? item.assigneeNames.join('、')
    : item.assigneeName || item.userName || item.operatorName || ''
}
function itemTime(item) {
  const time = item.completeTime || item.endTime || item.createTime || item.startTime
  return time ? formatFlowDateTime(time) : ''
}
// 有办理人显示姓名缩写；未分配的节点用节点名首字
function avatarText(item) {
  const actor = Array.isArray(item.assigneeNames) && item.assigneeNames.length > 1 ? '' : itemActor(item)
  return actor ? contactInitials(actor) : itemTitle(item).slice(0, 1)
}
</script>

<style lang="scss" scoped>
.trace-panel { margin-top: 12px; padding: 16px; border-radius: var(--forge-radius-card); background: var(--forge-surface); }
.trace-heading, .trace-title-row, .trace-sub-row { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: 8px; }
.trace-heading { margin-bottom: 14px; }
.trace-heading__title { color: var(--forge-text-primary); font-size: 15px; font-weight: 600; }
.trace-heading__count { color: var(--forge-text-tertiary); font-size: 12px; }
.trace-list { padding: 0; }
.trace-item { position: relative; display: flex; gap: 12px; padding-bottom: 20px; }
.trace-item:last-child { padding-bottom: 0; }
.trace-item:not(:last-child)::before { position: absolute; top: 40px; bottom: 4px; left: 17px; width: 2px; border-radius: 1px; background: var(--forge-border); content: ''; }
.trace-avatar { position: relative; display: flex; width: 36px; height: 36px; flex: 0 0 36px; align-items: center; justify-content: center; border-radius: 50%; color: #fff; font-size: 13px; font-weight: 500; background: var(--forge-arrow); }
.trace-avatar__badge { position: absolute; right: -2px; bottom: -2px; width: 12px; height: 12px; border: 2px solid var(--forge-surface); border-radius: 50%; background: var(--forge-arrow); box-sizing: border-box; }
.trace-item.is-running .trace-avatar { background: var(--forge-color-primary); }
.trace-item.is-running .trace-avatar__badge { background: var(--forge-color-warning); }
.trace-item.is-completed .trace-avatar { background: var(--forge-color-primary); }
.trace-item.is-completed .trace-avatar__badge { background: var(--forge-color-success); }
.trace-item.is-exception .trace-avatar { background: var(--forge-color-primary); }
.trace-item.is-exception .trace-avatar__badge { background: var(--forge-color-danger); }
.trace-copy { min-width: 0; flex: 1; padding-top: 1px; }
.trace-title { min-width: 0; flex: 1; overflow: hidden; color: var(--forge-text-primary); font-size: 15px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.trace-time { flex: 0 0 auto; color: var(--forge-text-tertiary); font-size: 12px; }
.trace-sub-row { justify-content: flex-start; margin-top: 2px; }
.trace-actor { min-width: 0; overflow: hidden; color: var(--forge-text-secondary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.trace-state { flex: 0 0 auto; color: var(--forge-text-secondary); font-size: 13px; }
.trace-state.running { color: var(--forge-color-warning); }
.trace-state.completed { color: var(--forge-color-success); }
.trace-state.exception { color: var(--forge-color-danger); }
.trace-comment { display: block; margin-top: 8px; padding: 8px 12px; border-radius: 10px; color: var(--forge-text-primary); font-size: 14px; line-height: 1.5; overflow-wrap: anywhere; background: var(--forge-page-bg); }
.trace-empty { padding: 24px 12px; color: var(--forge-text-tertiary); font-size: 13px; text-align: center; }
</style>
