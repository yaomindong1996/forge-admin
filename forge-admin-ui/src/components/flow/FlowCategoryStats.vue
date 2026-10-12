<template>
  <div class="flow-category-stats">
    <div class="stats-row">
      <button type="button" class="stat-card" @click="$emit('filter', 'all')">
        <div class="stat-icon total">
          <i class="i-material-symbols:category-outline" />
        </div>
        <div class="stat-content">
          <span class="stat-label">全部分类</span>
          <span class="stat-value">{{ totalCount }}</span>
        </div>
      </button>

      <button type="button" class="stat-card" @click="$emit('filter', 1)">
        <div class="stat-icon enabled">
          <i class="i-material-symbols:check-circle" />
        </div>
        <div class="stat-content">
          <span class="stat-label">已启用</span>
          <span class="stat-value">{{ enabledCount }}</span>
        </div>
      </button>

      <button type="button" class="stat-card" @click="$emit('filter', 0)">
        <div class="stat-icon disabled">
          <i class="i-material-symbols:cancel" />
        </div>
        <div class="stat-content">
          <span class="stat-label">已禁用</span>
          <span class="stat-value">{{ disabledCount }}</span>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  totalCount: { type: Number, default: 0 },
  enabledCount: { type: Number, default: 0 },
  disabledCount: { type: Number, default: 0 },
})

defineEmits(['filter'])
</script>

<style scoped>
.flow-category-stats {
  display: flex;
  flex: 0 0 auto;
  min-width: 0;
}

.stats-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  width: 100%;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 132px;
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 4px;
  background: var(--bg-primary);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 30%, var(--border-light, #e2e8f0));
  background: color-mix(in srgb, var(--primary-color, #2563eb) 3%, var(--bg-primary));
}

.stat-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
}

.stat-icon.total {
  background: #64748b;
}

.stat-icon.enabled {
  background: #16a34a;
}

.stat-icon.disabled {
  background: #dc2626;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.stat-label {
  font-size: 11px;
  color: var(--text-tertiary, #64748b);
  font-weight: 500;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  color: var(--text-primary, #0f172a);
}
</style>
