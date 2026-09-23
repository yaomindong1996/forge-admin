<template>
  <view class="ai-select" :class="{ 'ai-select--compact': compact }">
    <wd-picker
      v-model="selectedValue"
      :columns="normalizedOptions"
      :placeholder="placeholder"
      :title="title"
      :disabled="disabled || !normalizedOptions.length"
      :clearable="clearable"
      value-key="value"
      label-key="label"
      root-portal
      @confirm="handleConfirm"
      @clear="handleClear"
    />
    <text v-if="description" class="ai-select-description">{{ description }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择'
  },
  title: {
    type: String,
    default: '请选择'
  },
  description: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  },
  disabled: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedValue = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const normalizedOptions = computed(() => props.options.map(option => typeof option === 'object'
  ? { ...option, label: option.label ?? option.name ?? String(option.value ?? ''), value: option.value ?? option.id }
  : { label: String(option), value: option }))

function handleConfirm(event) {
  emit('change', event?.value ?? selectedValue.value)
}

function handleClear() {
  emit('update:modelValue', '')
  emit('change', '')
}
</script>

<style lang="scss" scoped>
.ai-select {
  min-width: 160rpx;

  &--compact {
    min-width: 172rpx;
  }
}

.ai-select :deep(.wd-picker__cell) {
  display: flex;
  min-height: 88rpx;
  align-items: center;
  padding: 0 24rpx;
  border: 1rpx solid var(--forge-color-border, #c9cdd4);
  border-radius: var(--forge-radius-control, 12rpx);
  background: var(--forge-color-surface, #fff);
  box-sizing: border-box;
}

.ai-select :deep(.wd-cell__wrapper),
.ai-select :deep(.wd-picker__value) {
  display: flex;
  min-height: 86rpx;
  align-items: center;
  line-height: 1.5;
}

.ai-select--compact :deep(.wd-picker__cell) {
  min-height: 88rpx;
  padding: 0 18rpx;
  border-radius: var(--forge-radius-control, 12rpx);
}

.ai-select-description {
  display: block;
  margin-top: 8rpx;
  color: var(--forge-color-text-muted, #86909c);
  font-size: 26rpx;
}

.ai-select :deep(.wd-picker__value) {
  color: var(--forge-color-text, #1d2129);
  font-size: 28rpx;
}

.ai-select :deep(.wd-picker__placeholder) {
  color: var(--forge-color-text-muted, #86909c);
}
</style>
