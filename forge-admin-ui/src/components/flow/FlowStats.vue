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
        <div v-if="todoCount > 0" class="stat-badge pulse">
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
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.stats-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e2e8f0;
  flex: 1;
  min-width: 140px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.stat-card.active {
  border-color: #0369a1;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.15);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-icon.todo {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: #fff;
}

.stat-icon.done {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
  color: #fff;
}

.stat-icon.started {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: #fff;
}

.stat-icon.cc {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  color: #fff;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.stat-badge {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.stat-badge.pulse {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>
