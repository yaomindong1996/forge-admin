<template>
  <wd-radio-group
    v-model="selectedValue"
    :disabled="disabled"
    :inline="inline"
    :shape="button ? 'button' : 'dot'"
    checked-color="var(--forge-color-primary, #4266f7)"
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
  min-height: 88rpx;
  margin: 0;
}

:deep(.wd-radio.is-button) {
  border-radius: var(--radius-control);
}
</style>
