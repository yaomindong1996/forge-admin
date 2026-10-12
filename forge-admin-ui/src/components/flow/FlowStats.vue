<template>
  <div class="flow-stats">
    <div class="stats-row">
      <div class="stat-card" :class="{ active: activeTab === 'todo' }" @click="$emit('switch', 'todo')">
        <div class="stat-icon todo">
          <i class="i-material-symbols:pending-actions" />
        </div>
        <div class="stat-content">
          <span class="stat-label">待办任务</span>
          <span class="stat-value">{{ todoCount }}</span>
        </div>
        <div v-if="todoCount > 0" class="stat-badge">
          {{ todoCount > 99 ? '99+' : todoCount }}
        </div>
      </div>

      <div class="stat-card" :class="{ active: activeTab === 'done' }" @click="$emit('switch', 'done')">
        <div class="stat-icon done">
          <i class="i-material-symbols:task-alt" />
        </div>
        <div class="stat-content">
          <span class="stat-label">已办任务</span>
          <span class="stat-value">{{ doneCount }}</span>
        </div>
      </div>

      <div class="stat-card" :class="{ active: activeTab === 'started' }" @click="$emit('switch', 'started')">
        <div class="stat-icon started">
          <i class="i-material-symbols:send" />
        </div>
        <div class="stat-content">
          <span class="stat-label">发起的流程</span>
          <span class="stat-value">{{ startedCount }}</span>
        </div>
      </div>

      <div class="stat-card" :class="{ active: activeTab === 'cc' }" @click="$emit('switch', 'cc')">
        <div class="stat-icon cc">
          <i class="i-material-symbols:content-copy" />
        </div>
        <div class="stat-content">
          <span class="stat-label">抄送我的</span>
          <span class="stat-value">{{ ccCount }}</span>
        </div>
        <div v-if="unreadCc > 0" class="stat-badge">
          {{ unreadCc > 99 ? '99+' : unreadCc }}未读
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  todoCount: { type: Number, default: 0 },
  doneCount: { type: Number, default: 0 },
  startedCount: { type: Number, default: 0 },
  ccCount: { type: Number, default: 0 },
  unreadCc: { type: Number, default: 0 },
  activeTab: { type: String, default: 'todo' },
})

defineEmits(['switch'])
</script>

<style scoped>
.flow-stats {
  margin-bottom: 18px;
  border-bottom: 1px solid #dce8e8;
  background: var(--bg-primary);
}

.stats-row {
  display: flex;
  gap: 2px;
  flex-wrap: wrap;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 44px;
  padding: 0 18px 12px;
  border: 0;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  transition:
    color 160ms ease,
    background-color 160ms ease;
}

.stat-card:hover {
  color: #0f766e;
  background: var(--bg-secondary);
}

.stat-card.active {
  color: #0f766e;
  background: transparent;
}

.stat-card.active::after {
  position: absolute;
  right: 12px;
  bottom: -1px;
  left: 12px;
  height: 2px;
  border-radius: 2px;
  background: #2e9998;
  content: '';
}

.stat-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: currentcolor;
  font-size: 18px;
}

.stat-content {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-label {
  color: currentcolor;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.stat-value {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.stat-badge {
  padding: 1px 7px;
  border: 1px solid #f3b6b6;
  border-radius: 999px;
  background: #fff7f7;
  color: #c24141;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .flow-stats {
    overflow-x: auto;
  }

  .stats-row {
    width: max-content;
    flex-wrap: nowrap;
  }

  .stat-card {
    padding-right: 14px;
    padding-left: 14px;
  }
}
</style>
