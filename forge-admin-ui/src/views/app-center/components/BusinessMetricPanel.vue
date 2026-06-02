<template>
  <div class="business-metric-panel">
    <section v-if="valueMetrics.length" class="metric-cards">
      <div v-for="metric in valueMetrics" :key="metric.metricCode" class="metric-card">
        <span>{{ metric.metricName }}</span>
        <strong>{{ formatMetricValue(metric) }}</strong>
      </div>
    </section>

    <section v-for="metric in groupMetrics" :key="metric.metricCode" class="metric-section">
      <div class="metric-section-head">
        <h3>{{ metric.metricName || '分布统计' }}</h3>
        <span v-if="metric.field">{{ metric.field }}</span>
      </div>
      <n-data-table
        v-if="metric.items?.length"
        :columns="groupColumns"
        :data="normalizeItems(metric.items)"
        :pagination="false"
        size="small"
      />
      <n-empty v-else description="暂无数据" />
    </section>

    <section v-for="metric in trendMetrics" :key="metric.metricCode" class="metric-section">
      <div class="metric-section-head">
        <h3>{{ metric.metricName || '新增趋势' }}</h3>
      </div>
      <div v-if="metric.items?.length" class="trend-list">
        <div v-for="item in normalizeItems(metric.items)" :key="item.label" class="trend-row">
          <span>{{ item.label }}</span>
          <div class="trend-track">
            <i :style="{ width: `${barWidth(item.value, metric.items)}%` }" />
          </div>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <n-empty v-else description="暂无趋势数据" />
    </section>

    <n-empty v-if="!metrics.length" description="暂无可统计字段" />
  </div>
</template>

<script setup>
import { computed, h } from 'vue'

const props = defineProps({
  metrics: {
    type: Array,
    default: () => [],
  },
})

const valueMetrics = computed(() => props.metrics.filter(metric => ['COUNT', 'SUM'].includes(metric.metricType)))
const groupMetrics = computed(() => props.metrics.filter(metric => metric.metricType === 'GROUP'))
const trendMetrics = computed(() => props.metrics.filter(metric => metric.metricType === 'TREND'))

const groupColumns = [
  {
    title: '分组',
    key: 'label',
    render: row => h('span', row.label || '(空)'),
  },
  {
    title: '数量',
    key: 'value',
    width: 120,
  },
]

function normalizeItems(items = []) {
  return items.map((item, index) => ({
    id: `${item.label ?? 'empty'}_${index}`,
    label: item.label ?? item.name ?? '(空)',
    value: Number(item.value ?? item.count ?? 0),
  }))
}

function formatMetricValue(metric = {}) {
  if (metric.unit === '分')
    return formatYuan(metric.value)
  const value = Number(metric.value ?? 0)
  return `${value.toLocaleString('zh-CN')}${metric.unit || ''}`
}

function formatYuan(value) {
  const amount = Number(value || 0) / 100
  return amount.toLocaleString('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
  })
}

function barWidth(value, items = []) {
  const list = normalizeItems(items)
  const max = Math.max(...list.map(item => Number(item.value || 0)), 1)
  return Math.max(2, Math.round((Number(value || 0) / max) * 100))
}
</script>

<style scoped>
.business-metric-panel {
  display: grid;
  gap: 16px;
}

.metric-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.metric-card,
.metric-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.metric-card span {
  color: #64748b;
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.metric-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.metric-section-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.metric-section-head span {
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  padding: 2px 8px;
}

.trend-list {
  display: grid;
  gap: 8px;
}

.trend-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 64px;
  gap: 10px;
  align-items: center;
}

.trend-row span,
.trend-row strong {
  color: #475569;
  font-size: 12px;
}

.trend-row span {
  text-align: right;
}

.trend-row strong {
  text-align: right;
}

.trend-track {
  height: 18px;
  overflow: hidden;
  border-radius: 4px;
  background: #e2e8f0;
}

.trend-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
}
</style>
