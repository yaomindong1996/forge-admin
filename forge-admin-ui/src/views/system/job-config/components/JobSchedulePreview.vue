<template>
  <div class="schedule-preview" :class="{ invalid: Boolean(error) }">
    <div class="preview-summary">
      <div class="preview-icon">
        <i :class="previewIcon" />
      </div>
      <div>
        <span>{{ previewLabel }} · {{ timezone || '未选择时区' }}</span>
        <strong>{{ error || description || '正在校验执行计划' }}</strong>
      </div>
      <NSpin v-if="loading" size="small" />
    </div>

    <div v-if="!error && nextFireTimes.length" class="future-times">
      <span v-for="(time, index) in nextFireTimes" :key="time">
        <b v-if="scheduleType === 'CRON'">{{ index + 1 }}</b>
        {{ formatTime(time) }}
      </span>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { NSpin } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  scheduleType: {
    type: String,
    default: 'CRON',
  },
  timezone: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  nextFireTimes: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
})

const previewLabel = computed(() => props.scheduleType === 'ONCE' ? '一次性计划' : '周期计划')
const previewIcon = computed(() => {
  if (props.error)
    return 'i-material-symbols:error-outline-rounded'
  return props.scheduleType === 'ONCE'
    ? 'i-material-symbols:event-outline-rounded'
    : 'i-material-symbols:event-repeat-rounded'
})

function formatTime(value) {
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style scoped>
.schedule-preview {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 24%, var(--border-color));
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color) 4%, var(--card-color));
}

.schedule-preview.invalid {
  border-color: color-mix(in srgb, var(--error-color) 35%, var(--border-color));
  background: color-mix(in srgb, var(--error-color) 4%, var(--card-color));
}

.preview-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview-summary > div:nth-child(2) {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preview-icon {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  color: var(--primary-color);
  font-size: 18px;
}

.invalid .preview-icon,
.invalid strong {
  color: var(--error-color);
}

.preview-summary span,
.future-times span {
  color: var(--text-color-3);
  font-size: 12px;
}

.preview-summary strong {
  overflow-wrap: anywhere;
  color: var(--text-color-1);
  font-size: 13px;
}

.future-times {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px 14px;
  padding-top: 10px;
  border-top: 1px solid var(--divider-color);
}

.future-times span {
  display: flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.future-times b {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  color: var(--text-color-2);
  background: var(--action-color);
  border-radius: 50%;
  font-size: 10px;
  font-weight: 600;
}

@media (max-width: 560px) {
  .future-times {
    grid-template-columns: 1fr;
  }
}
</style>
