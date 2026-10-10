<template>
  <wd-checkbox-group
    v-model="selectedValues"
    :disabled="disabled"
    :inline="inline"
    :max="max"
    :min="min"
    :shape="button ? 'button' : 'square'"
    checked-color="var(--forge-color-primary, #0066ff)"
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
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0;
  padding: 0 12px;
  box-sizing: border-box;
}

:deep(.wd-checkbox__shape) { flex: 0 0 auto; align-self: center; margin-top: 0 !important; }
:deep(.wd-checkbox__label) { display: flex; min-height: 20px; align-items: center; align-self: center; margin: 0; line-height: 20px; }

:deep(.wd-checkbox.is-button) {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  margin: 0;
  text-align: center;
}

:deep(.wd-checkbox.is-button .wd-checkbox__label),
:deep(.wd-checkbox.is-button-box .wd-checkbox__label) { display: flex; width: 100%; height: 44px; min-height: 44px; align-items: center; justify-content: center; padding: 0 14px; border-radius: var(--radius-control); line-height: 20px; box-sizing: border-box; }
:deep(.wd-checkbox__txt) { display: flex; align-items: center; line-height: 20px; }
</style>
