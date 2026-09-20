<template>
  <wd-checkbox-group
    v-model="selectedValues"
    :disabled="disabled"
    :inline="inline"
    :max="max"
    :min="min"
    :shape="button ? 'button' : 'square'"
    checked-color="var(--forge-color-primary, #2563eb)"
    @change="handleChange"
  >
    <wd-checkbox
      v-for="option in options"
      :key="String(option.value)"
      :model-value="option.value"
      :disabled="option.disabled === true"
    >
      {{ option.label }}
    </wd-checkbox>
  </wd-checkbox-group>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [Array, String], default: () => [] },
  options: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  inline: { type: Boolean, default: false },
  button: { type: Boolean, default: false },
  min: { type: Number, default: 0 },
  max: { type: Number, default: 0 },
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedValues = computed({
  get: () => Array.isArray(props.modelValue)
    ? props.modelValue
    : String(props.modelValue || '').split(',').map(item => item.trim()).filter(Boolean),
  set: value => emit('update:modelValue', value),
})

function handleChange(event) {
  const value = event?.value || selectedValues.value
  emit('change', value)
}
</script>

<style lang="scss" scoped>
:deep(.wd-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx 22rpx;
}

:deep(.wd-checkbox) {
  margin: 0;
}
</style>
