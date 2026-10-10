<template>
  <view v-if="isRange" class="ai-datetime-range">
    <wd-datetime-picker
      v-model="rangeStartValue"
      :type="pickerType"
      :title="`${title}（开始）`"
      :placeholder="startPlaceholder || '开始时间'"
      :disabled="disabled"
      :readonly="readonly"
      :clearable="clearable"
      :min-date="minTimestamp"
      :max-date="rangeEndTimestamp || maxTimestamp"
      :z-index="10010"
      root-portal
      @confirm="event => handleRangeConfirm(0, event)"
      @clear="() => handleRangeClear(0)"
    />
    <text class="ai-datetime-range__separator">至</text>
    <wd-datetime-picker
      v-model="rangeEndValue"
      :type="pickerType"
      :title="`${title}（结束）`"
      :placeholder="endPlaceholder || '结束时间'"
      :disabled="disabled"
      :readonly="readonly"
      :clearable="clearable"
      :min-date="rangeStartTimestamp || minTimestamp"
      :max-date="maxTimestamp"
      :z-index="10010"
      root-portal
      @confirm="event => handleRangeConfirm(1, event)"
      @clear="() => handleRangeClear(1)"
    />
  </view>
  <wd-datetime-picker
    v-else
    v-model="pickerValue"
    :type="pickerType"
    :title="title"
    :placeholder="placeholder"
    :disabled="disabled"
    :readonly="readonly"
    :clearable="clearable"
    :min-date="minTimestamp"
    :max-date="maxTimestamp"
    :z-index="10010"
    root-portal
    @confirm="handleConfirm"
    @clear="handleClear"
  />
</template>

<script setup>
import dayjs from 'dayjs'
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number, Array], default: '' },
  type: { type: String, default: 'date' },
  title: { type: String, default: '选择时间' },
  placeholder: { type: String, default: '请选择' },
  startPlaceholder: { type: String, default: '' },
  endPlaceholder: { type: String, default: '' },
  format: { type: String, default: '' },
  valueFormat: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  clearable: { type: Boolean, default: true },
  min: { type: [String, Number], default: '' },
  max: { type: [String, Number], default: '' },
})

const emit = defineEmits(['update:modelValue', 'change', 'confirm', 'blur'])

const isRange = computed(() => ['daterange', 'datetimerange', 'timerange'].includes(props.type))

const pickerType = computed(() => {
  if (props.type === 'time' || props.type === 'timerange') return 'time'
  if (props.type === 'month') return 'year-month'
  if (props.type === 'date' || props.type === 'daterange' || props.type === 'year') return 'date'
  return 'datetime'
})

const pickerValue = computed({
  get: () => toPickerValue(props.modelValue),
  set: value => emitValue(value),
})
const rangeStartValue = computed({
  get: () => toPickerScalar(Array.isArray(props.modelValue) ? props.modelValue[0] : ''),
  set: value => emitRangeValue(0, value),
})
const rangeEndValue = computed({
  get: () => toPickerScalar(Array.isArray(props.modelValue) ? props.modelValue[1] : ''),
  set: value => emitRangeValue(1, value),
})
const rangeStartTimestamp = computed(() => toBoundary(rangeStartValue.value, 0) || 0)
const rangeEndTimestamp = computed(() => toBoundary(rangeEndValue.value, 0) || 0)

const minTimestamp = computed(() => toBoundary(props.min, new Date(1970, 0, 1).getTime()))
const maxTimestamp = computed(() => toBoundary(props.max, new Date(2100, 11, 31, 23, 59, 59).getTime()))

function toPickerValue(value) {
  if (Array.isArray(value)) return value.map(toPickerScalar)
  return toPickerScalar(value)
}

function toPickerScalar(value) {
  if (value === undefined || value === null || value === '') return ''
  if (pickerType.value === 'time') return String(value).slice(0, 8)
  if (typeof value === 'number') return value
  const parsed = dayjs(String(value))
  return parsed.isValid() ? parsed.valueOf() : ''
}

function emitValue(value) {
  const next = Array.isArray(value) ? value.map(formatValue) : formatValue(value)
  emit('update:modelValue', next)
  emit('change', next)
}

function formatValue(value) {
  if (value === undefined || value === null || value === '') return ''
  if (pickerType.value === 'time') return String(value)
  const parsed = dayjs(Number(value))
  if (!parsed.isValid()) return ''
  return parsed.format(resolveValueFormat())
}

function resolveValueFormat() {
  if (props.valueFormat || props.format) return normalizeDayjsFormat(props.valueFormat || props.format)
  if (props.type === 'year') return 'YYYY'
  if (props.type === 'month') return 'YYYY-MM'
  if (props.type === 'date' || props.type === 'daterange') return 'YYYY-MM-DD'
  if (props.type === 'time' || props.type === 'timerange') return 'HH:mm:ss'
  return 'YYYY-MM-DD HH:mm:ss'
}

function normalizeDayjsFormat(value) {
  return String(value || '')
    .replace(/yyyy/g, 'YYYY')
    .replace(/yy/g, 'YY')
    .replace(/dd/g, 'DD')
}

function emitRangeValue(index, value) {
  const next = Array.isArray(props.modelValue) ? [...props.modelValue] : ['', '']
  next[index] = formatValue(value)
  emit('update:modelValue', next)
  emit('change', next)
  return next
}

function handleRangeConfirm(index, event) {
  const next = emitRangeValue(index, event?.value)
  emit('confirm', next)
  emit('blur')
}

function handleRangeClear(index) {
  const next = Array.isArray(props.modelValue) ? [...props.modelValue] : ['', '']
  next[index] = ''
  emit('update:modelValue', next)
  emit('change', next)
  emit('blur')
}

function toBoundary(value, fallback) {
  if (value === undefined || value === null || value === '') return fallback
  const parsed = typeof value === 'number' ? value : dayjs(String(value)).valueOf()
  return Number.isFinite(parsed) ? parsed : fallback
}

function handleConfirm(event) {
  const next = Array.isArray(event?.value) ? event.value.map(formatValue) : formatValue(event?.value)
  emit('confirm', next)
  emit('blur')
}

function handleClear() {
  const next = Array.isArray(props.modelValue) ? [] : ''
  emit('update:modelValue', next)
  emit('change', next)
  emit('blur')
}
</script>

<style lang="scss" scoped>
.ai-datetime-range { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr); align-items: center; gap: 8rpx; }
.ai-datetime-range__separator { color: var(--forge-color-text-muted, #a2a3a5); font-size: 24rpx; }
:deep(.wd-datetime-picker__cell) {
  display: flex;
  min-height: 44px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--forge-color-border, #f0f1f2);
  border-radius: var(--forge-radius-control);
  background: var(--forge-color-surface, #fff);
  box-sizing: border-box;
}

:deep(.wd-cell__wrapper),
:deep(.wd-datetime-picker__value) {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 42px;
  align-items: center;
  line-height: 1.5;
}

:deep(.wd-cell__body) { display: flex; width: 100%; min-width: 0; align-items: center; padding: 0; }
:deep(.wd-datetime-picker__cell-placeholder) { display: flex; min-height: 42px; align-items: center; color: var(--text-muted); line-height: 1.5; }
</style>
