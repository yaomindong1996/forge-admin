<template>
  <div class="flow-management-stats">
    <div class="stats-row">
      <div class="stat-card" @click="$emit('filter', 'all')">
        <div class="stat-icon total">
          <i class="i-material-symbols:device-hub" />
        </div>
        <div class="stat-content">
          <span class="stat-label">全部模型</span>
          <span class="stat-value">{{ totalCount }}</span>
        </div>
      </div>

      <div class="stat-card designing" @click="$emit('filter', 0)">
        <div class="stat-icon designing">
          <i class="i-material-symbols:edit-note" />
        </div>
        <div class="stat-content">
          <span class="stat-label">设计中</span>
          <span class="stat-value">{{ designingCount }}</span>
        </div>
      </div>

      <div class="stat-card deployed" @click="$emit('filter', 1)">
        <div class="stat-icon deployed">
          <i class="i-material-symbols:rocket-launch" />
        </div>
        <div class="stat-content">
          <span class="stat-label">已部署</span>
          <span class="stat-value">{{ deployedCount }}</span>
        </div>
      </div>

      <div class="stat-card suspended" @click="$emit('filter', 2)">
        <div class="stat-icon suspended">
          <i class="i-material-symbols:pause-circle" />
        </div>
        <div class="stat-content">
          <span class="stat-label">已挂起</span>
          <span class="stat-value">{{ suspendedCount }}</span>
        </div>
      </div>

      <div class="stat-card disabled" @click="$emit('filter', 3)">
        <div class="stat-icon disabled">
          <i class="i-material-symbols:block" />
        </div>
        <div class="stat-content">
          <span class="stat-label">已禁用</span>
          <span class="stat-value">{{ disabledCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  totalCount: { type: Number, default: 0 },
  designingCount: { type: Number, default: 0 },
  deployedCount: { type: Number, default: 0 },
  suspendedCount: { type: Number, default: 0 },
  disabledCount: { type: Number, default: 0 },
})

defineEmits(['filter'])
</script>

<style scoped>
.flow-management-stats {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  flex-shrink: 0;
  overflow-x: auto;
}

.stats-row {
  display: flex;
  gap: 8px;
  min-width: 760px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border-radius: 7px;
  padding: 10px 12px;
  cursor: pointer;
  transition:
    background 160ms ease,
    border-color 160ms ease;
  border: 1px solid #e2e8f0;
  flex: 1;
  min-width: 140px;
}

.stat-card:hover {
  background: #fff;
  border-color: #cbd5e1;
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  border: 1px solid transparent;
  flex-shrink: 0;
}

.stat-icon.total {
  color: #475569;
  background: #f1f5f9;
  border-color: #e2e8f0;
}

.stat-icon.designing {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.stat-icon.deployed {
  color: #047857;
  background: #ecfdf5;
  border-color: #bbf7d0;
}

.stat-icon.suspended {
  color: #475569;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.stat-icon.disabled {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.stat-content {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  width: 100%;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  white-space: nowrap;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  line-height: 1;
}

@media (max-width: 720px) {
  .stats-row {
    min-width: 700px;
  }
}
</style>
