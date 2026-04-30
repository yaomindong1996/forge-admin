<template>
  <div class="flow-timeline">
    <div
      v-for="(item, index) in items"
      :key="index"
      class="timeline-item"
      :class="{ first: index === 0, last: index === items.length - 1 }"
    >
      <div class="timeline-node" :class="getNodeType(item.action)">
        <i :class="getActionIcon(item.action)" />
      </div>
      <div class="timeline-content">
        <div class="timeline-header">
          <div class="timeline-user">
            <UserAvatar :name="item.assigneeName || '未知'" :size="28" />
            <div class="user-info">
              <span class="user-name">{{ item.assigneeName || '-' }}</span>
              <span class="task-name">{{ item.taskName }}</span>
            </div>
          </div>
          <div class="timeline-meta">
            <span class="action-badge" :class="getActionClass(item.action)">
              {{ getActionText(item.action) }}
            </span>
            <span class="time-text">{{ formatTime(item.completeTime || item.createTime) }}</span>
          </div>
        </div>
        <div v-if="item.comment" class="timeline-comment">
          <i class="i-material-symbols:chat-bubble-outline" />
          <span>{{ item.comment }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import UserAvatar from '@/components/common/UserAvatar.vue'

defineProps({
  items: { type: Array, default: () => [] },
})

function getNodeType(action) {
  if (action === 'approve' || action === 'start')
    return 'success'
  if (action === 'reject')
    return 'error'
  if (action === 'delegate')
    return 'warning'
  return 'default'
}

function getActionIcon(action) {
  const icons = {
    approve: 'i-material-symbols:check-circle',
    reject: 'i-material-symbols:cancel',
    delegate: 'i-material-symbols:arrow-forward',
    claim: 'i-material-symbols:assignment-ind',
    start: 'i-material-symbols:play-circle',
    withdraw: 'i-material-symbols:undo',
    pending: 'i-material-symbols:schedule',
  }
  return icons[action] || 'i-material-symbols:circle'
}

function getActionText(action) {
  const texts = {
    approve: '同意',
    reject: '驳回',
    delegate: '转办',
    claim: '签收',
    start: '发起',
    withdraw: '撤回',
    pending: '待处理',
  }
  return texts[action] || action || '操作'
}

function getActionClass(action) {
  if (action === 'approve' || action === 'start')
    return 'success'
  if (action === 'reject')
    return 'error'
  if (action === 'delegate')
    return 'warning'
  return 'info'
}

function formatTime(time) {
  if (!time)
    return '-'
  if (typeof time === 'string' && time.includes('T')) {
    return time.replace('T', ' ').slice(0, 16)
  }
  return time
}
</script>

<style scoped>
.flow-timeline {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  gap: 12px;
  position: relative;
}

.timeline-item:not(.last)::before {
  content: '';
  position: absolute;
  left: 17px;
  top: 36px;
  width: 2px;
  height: calc(100% - 36px);
  background: #e2e8f0;
}

.timeline-node {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  z-index: 1;
}

.timeline-node.success {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
  color: #fff;
}

.timeline-node.error {
  background: linear-gradient(135deg, #f87171 0%, #ef4444 100%);
  color: #fff;
}

.timeline-node.warning {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: #fff;
}

.timeline-node.default {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
  color: #fff;
}

.timeline-node.info {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: #fff;
}

.timeline-content {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 12px 16px;
  margin-bottom: 12px;
  transition: all 0.2s ease;
}

.timeline-content:hover {
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.timeline-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.task-name {
  font-size: 12px;
  color: #64748b;
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-badge {
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.action-badge.success {
  background: #dcfce7;
  color: #15803d;
}

.action-badge.error {
  background: #fee2e2;
  color: #b91c1c;
}

.action-badge.warning {
  background: #fef3c7;
  color: #b45309;
}

.action-badge.info {
  background: #dbeafe;
  color: #1e40af;
}

.time-text {
  font-size: 12px;
  color: #94a3b8;
}

.timeline-comment {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.timeline-comment i {
  color: #64748b;
  font-size: 16px;
}
</style>
