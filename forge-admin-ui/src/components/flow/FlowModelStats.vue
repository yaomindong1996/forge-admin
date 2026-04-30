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
  min-width: 120px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.stat-icon.total {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

.stat-icon.designing {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
}

.stat-icon.deployed {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
}

.stat-icon.suspended {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
}

.stat-icon.disabled {
  background: linear-gradient(135deg, #f87171 0%, #ef4444 100%);
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
</style>
