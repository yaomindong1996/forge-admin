<script setup>
import { computed } from 'vue'
import {
  NButton,
  NColorPicker,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
} from 'naive-ui'
import IconSelector from '@/components/IconSelector.vue'
import {
  FREE_LAYOUT_LINK_TYPE_OPTIONS,
  FREE_LAYOUT_TONE_OPTIONS,
  normalizeStatMetric,
} from './free-layout-widget-kit.js'

const props = defineProps({
  block: { type: Object, required: true },
  pageOptions: { type: Array, default: () => [] },
  listBound: { type: Boolean, default: false },
})

const emit = defineEmits(['patch-props'])

const pageSelectOptions = computed(() =>
  (Array.isArray(props.pageOptions) ? props.pageOptions : [])
    .map(item => ({
      label: item.label || item.value,
      value: String(item.value || ''),
    }))
    .filter(item => item.value),
)

const metrics = computed(() => {
  const list = Array.isArray(props.block?.props?.metrics)
    ? props.block.props.metrics
    : []
  return list.map((item, index) => normalizeStatMetric(item, index))
})

function patch(partial) {
  emit('patch-props', partial)
}

function patchMetric(index, partial) {
  const next = metrics.value.map((item, i) => (i === index ? { ...item, ...partial } : { ...item }))
  patch({ metrics: next })
}

function addMetric() {
  const next = [
    ...metrics.value,
    normalizeStatMetric({
      label: `指标${metrics.value.length + 1}`,
      value: '0',
      trend: '',
      icon: 'AnalyticsOutline',
      tone: 'primary',
      linkType: 'none',
    }, metrics.value.length),
  ]
  patch({ metrics: next })
}

function removeMetric(index) {
  patch({ metrics: metrics.value.filter((_, i) => i !== index) })
}

function moveMetric(index, delta) {
  const target = index + delta
  if (target < 0 || target >= metrics.value.length)
    return
  const next = [...metrics.value]
  const [row] = next.splice(index, 1)
  next.splice(target, 0, row)
  patch({ metrics: next })
}
</script>

<template>
  <div class="ss-prop">
    <div class="ss-section">
      <div class="ss-section-title">布局</div>
      <div class="ss-field">
        <label>列数</label>
        <NInputNumber
          :value="Number(block.props?.columns || 4)"
          :min="1"
          :max="6"
          size="small"
          @update:value="(v) => patch({ columns: Number(v) || 4 })"
        />
      </div>
      <div class="ss-field ss-field-row">
        <label>紧凑模式</label>
        <NSwitch
          :value="block.props?.compact === true"
          size="small"
          @update:value="(v) => patch({ compact: v === true })"
        />
      </div>
      <div class="ss-field ss-field-row">
        <label>显示图标</label>
        <NSwitch
          :value="block.props?.showIcon !== false"
          size="small"
          @update:value="(v) => patch({ showIcon: v !== false })"
        />
      </div>
      <div class="ss-field ss-field-row">
        <label>显示趋势</label>
        <NSwitch
          :value="block.props?.showTrend !== false"
          size="small"
          @update:value="(v) => patch({ showTrend: v !== false })"
        />
      </div>
    </div>

    <div class="ss-section">
      <div class="ss-section-head">
        <div class="ss-section-title">指标项</div>
        <NButton size="tiny" quaternary type="primary" @click="addMetric">
          添加
        </NButton>
      </div>
      <div class="ss-help">
        {{ listBound
          ? '当前为「列表循环」：画布按数组条数渲染。这里的指标仅在列表无数据时作占位。'
          : '可配置图标、色调与点击跳转。数值可为静态，也可配合上方数据来源绑定字段。' }}
      </div>

      <div
        v-for="(metric, index) in metrics"
        :key="metric.key"
        class="ss-item"
      >
        <div class="ss-item-head">
          <strong>指标 {{ index + 1 }}</strong>
          <div class="ss-item-actions">
            <NButton size="tiny" quaternary :disabled="index === 0" @click="moveMetric(index, -1)">
              上移
            </NButton>
            <NButton size="tiny" quaternary :disabled="index === metrics.length - 1" @click="moveMetric(index, 1)">
              下移
            </NButton>
            <NButton size="tiny" quaternary type="error" @click="removeMetric(index)">
              删除
            </NButton>
          </div>
        </div>

        <div class="ss-field">
          <label>标签</label>
          <NInput
            :value="metric.label"
            size="small"
            @update:value="(v) => patchMetric(index, { label: v })"
          />
        </div>
        <div class="ss-field">
          <label>数值</label>
          <NInput
            :value="String(metric.value ?? '')"
            size="small"
            @update:value="(v) => patchMetric(index, { value: v })"
          />
        </div>
        <div class="ss-field">
          <label>趋势文案</label>
          <NInput
            :value="metric.trend"
            size="small"
            placeholder="如 +12% 或 较上周"
            @update:value="(v) => patchMetric(index, { trend: v })"
          />
        </div>
        <div class="ss-field">
          <label>图标</label>
          <IconSelector
            :model-value="metric.icon"
            @update:model-value="(v) => patchMetric(index, { icon: v || '' })"
          />
        </div>
        <div class="ss-field">
          <label>色调</label>
          <NSelect
            :value="metric.tone || 'primary'"
            :options="FREE_LAYOUT_TONE_OPTIONS"
            size="small"
            @update:value="(v) => patchMetric(index, { tone: v || 'primary' })"
          />
        </div>
        <div v-if="metric.tone === 'custom'" class="ss-field">
          <label>自定义色</label>
          <NColorPicker
            :value="metric.color || '#2563eb'"
            :modes="['hex']"
            size="small"
            @update:value="(v) => patchMetric(index, { color: v || '' })"
          />
        </div>
        <div class="ss-field">
          <label>跳转类型</label>
          <NSelect
            :value="metric.linkType || 'none'"
            :options="FREE_LAYOUT_LINK_TYPE_OPTIONS"
            size="small"
            @update:value="(v) => patchMetric(index, { linkType: v || 'none' })"
          />
        </div>
        <div v-if="metric.linkType === 'page'" class="ss-field">
          <label>目标页面</label>
          <NSelect
            :value="metric.targetPageId || null"
            :options="pageSelectOptions"
            clearable
            filterable
            size="small"
            @update:value="(v) => patchMetric(index, { targetPageId: v || '', linkType: 'page' })"
          />
        </div>
        <div v-if="metric.linkType === 'url'" class="ss-field">
          <label>外部链接</label>
          <NInput
            :value="metric.href"
            size="small"
            placeholder="https://"
            @update:value="(v) => patchMetric(index, { href: v })"
          />
        </div>
      </div>

      <div v-if="!metrics.length" class="ss-empty">
        暂无指标，可配置图标、色调与点击跳转。
      </div>
    </div>
  </div>
</template>

<style scoped>
.ss-prop {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ss-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ss-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ss-section-title {
  font-size: 12px;
  font-weight: 650;
  color: #334155;
}

.ss-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ss-field > label {
  font-size: 12px;
  color: #64748b;
}

.ss-field-row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.ss-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.ss-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ss-item-head strong {
  font-size: 12px;
  color: #0f172a;
}

.ss-item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.ss-help {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.45;
}

.ss-empty {
  padding: 12px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
}
</style>
