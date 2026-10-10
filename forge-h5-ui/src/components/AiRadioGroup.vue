<template>
  <wd-radio-group
    v-model="selectedValue"
    :disabled="disabled"
    :inline="inline"
    :shape="button ? 'button' : 'dot'"
    checked-color="var(--forge-color-primary, #0066ff)"
    @change="handleChange"
  >
    <wd-radio
      v-for="option in options"
      :key="String(option.value)"
      :value="option.value"
      :disabled="option.disabled === true"
    >
      {{ option.label }}
    </wd-radio>
  </wd-radio-group>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  options: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: [String, Number, Boolean],
    default: ''
  },
  disabled: { type: Boolean, default: false },
  inline: { type: Boolean, default: false },
  button: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedValue = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const handleChange = (event) => {
  emit('change', event?.value ?? selectedValue.value)
}
</script>

<style lang="scss" scoped>
:deep(.wd-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx 22rpx;
}

:deep(.wd-radio) {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0;
  padding: 0 12px;
  box-sizing: border-box;
}

:deep(.wd-radio__shape) { flex: 0 0 auto; align-self: center; margin-top: 0 !important; }
:deep(.wd-radio__label) { display: flex; min-height: 20px; align-items: center; align-self: center; margin: 0; line-height: 20px; }
:deep(.wd-radio.is-button) { display: inline-flex; min-height: 44px; align-items: center; justify-content: center; margin: 0; text-align: center; }
:deep(.wd-radio.is-button .wd-radio__label),
:deep(.wd-radio.is-button-radio .wd-radio__label) { display: flex; width: 100%; height: 44px; min-height: 44px; align-items: center; justify-content: center; padding: 0 14px; border-radius: var(--radius-control); line-height: 20px; box-sizing: border-box; }
</style>
