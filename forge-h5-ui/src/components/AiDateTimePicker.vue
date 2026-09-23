<template>
  <wd-datetime-picker
    v-model="pickerValue"
    :type="pickerType"
    :title="title"
    :placeholder="placeholder"
    :disabled="disabled"
    :readonly="readonly"
    :clearable="clearable"
    :min-date="minTimestamp"
    :max-date="maxTimestamp"
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
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  clearable: { type: Boolean, default: true },
  min: { type: [String, Number], default: '' },
  max: { type: [String, Number], default: '' },
})

const emit = defineEmits(['update:modelValue', 'change', 'confirm', 'blur'])

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
  if (props.type === 'year') return parsed.format('YYYY')
  if (props.type === 'month') return parsed.format('YYYY-MM')
  if (props.type === 'date' || props.type === 'daterange') return parsed.format('YYYY-MM-DD')
  return parsed.format('YYYY-MM-DD HH:mm:ss')
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
:deep(.wd-datetime-picker__cell) {
  display: flex;
  min-height: 88rpx;
  align-items: center;
  padding: 0 20rpx;
  border: 1rpx solid var(--forge-color-border, #c9cdd4);
  border-radius: var(--forge-radius-control, 12rpx);
  background: var(--forge-color-surface, #fff);
  box-sizing: border-box;
}

:deep(.wd-cell__wrapper),
:deep(.wd-datetime-picker__value) {
  display: flex;
  min-height: 86rpx;
  align-items: center;
  line-height: 1.5;
}
</style>
