<template>
  <view class="ai-select" :class="{ 'ai-select--compact': compact }">
    <wd-select-picker
      v-model="selectedValue"
      :columns="normalizedOptions"
      :type="multiple ? 'checkbox' : 'radio'"
      :placeholder="placeholder"
      :title="title"
      :disabled="disabled || !normalizedOptions.length"
      :clearable="clearable"
      :filterable="filterable"
      :filter-placeholder="filterPlaceholder"
      :show-confirm="multiple || showConfirm"
      :min="min"
      :max="max"
      :safe-area-inset-bottom="true"
      value-key="value"
      label-key="label"
      :z-index="10010"
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
    type: [String, Number, Array],
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
  multiple: { type: Boolean, default: false },
  min: { type: Number, default: 0 },
  max: { type: Number, default: 0 },
  clearable: { type: Boolean, default: false },
  filterable: { type: Boolean, default: false },
  filterPlaceholder: { type: String, default: '搜索选项' },
  showConfirm: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedValue = computed({
  get: () => props.multiple
    ? (Array.isArray(props.modelValue) ? props.modelValue : String(props.modelValue || '').split(',').map(item => item.trim()).filter(Boolean))
    : props.modelValue,
  set: value => emit('update:modelValue', value),
})

const normalizedOptions = computed(() => props.options.map(option => typeof option === 'object'
  ? { ...option, label: option.label ?? option.name ?? String(option.value ?? ''), value: option.value ?? option.id }
  : { label: String(option), value: option }))

function handleConfirm(event) {
  emit('change', event?.value ?? selectedValue.value)
}

function handleClear() {
  const value = props.multiple ? [] : ''
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style lang="scss" scoped>
.ai-select {
  min-width: 80px;

  &--compact {
    min-width: 86px;
  }
}

.ai-select :deep(.wd-select-picker__cell) {
  display: flex;
  min-height: 44px;
  align-items: center;
  padding: 0 12px;
  border: 1px solid var(--forge-color-border, #f0f1f2);
  border-radius: var(--forge-radius-control);
  background: var(--forge-color-surface, #fff);
  box-sizing: border-box;
}

.ai-select :deep(.wd-cell__wrapper),
.ai-select :deep(.wd-cell__body),
.ai-select :deep(.wd-cell__value) {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  line-height: 1.5;
}

.ai-select :deep(.wd-cell__body) { padding: 0; }
.ai-select :deep(.wd-cell__value) { justify-content: flex-start; }

.ai-select--compact :deep(.wd-select-picker__cell) {
  min-height: 44px;
  padding: 0 9px;
  border-radius: var(--forge-radius-control);
}

.ai-select-description {
  display: block;
  margin-top: 4px;
  color: var(--forge-color-text-muted, #a2a3a5);
  font-size: 13px;
}

.ai-select :deep(.wd-cell__value) {
  color: var(--forge-color-text, #171a1d);
  font-size: 14px;
}

.ai-select :deep(.wd-select-picker__cell--placeholder .wd-cell__value) {
  color: var(--forge-color-text-muted, #a2a3a5);
}
</style>
