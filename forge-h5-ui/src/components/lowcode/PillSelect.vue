<template>
  <view class="pill-select" :class="{ 'pill-select--disabled': disabled }">
    <button
      v-for="option in options"
      :key="String(option.value)"
      class="pill-select__item"
      :class="{ 'pill-select__item--active': isSelected(option) }"
      :disabled="disabled"
      :hover-class="disabled ? 'none' : 'pill-select__item--pressed'"
      @click="select(option)"
    >
      <text class="pill-select__label">{{ option.label }}</text>
    </button>
  </view>
</template>

<script setup>
import { watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

watch(
  () => [props.modelValue, props.options, props.clearable, props.disabled],
  () => {
    if (!props.clearable && !props.disabled && isEmpty(props.modelValue) && props.options.length)
      commit(props.options[0].value)
  },
  { immediate: true, deep: true },
)

function isSelected(option) {
  return String(props.modelValue) === String(option.value)
}

function select(option) {
  if (props.disabled)
    return
  if (isSelected(option)) {
    if (props.clearable)
      commit('')
    return
  }
  commit(option.value)
}

function commit(value) {
  emit('update:modelValue', value)
  emit('change', value)
}

function isEmpty(value) {
  return value === undefined || value === null || value === ''
}
</script>

<style lang="scss" scoped>
.pill-select {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  min-height: 72rpx;
  align-items: center;
}

.pill-select--disabled {
  opacity: 0.55;
}

.pill-select__item {
  display: inline-flex;
  min-width: 128rpx;
  min-height: 68rpx;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 10rpx 26rpx;
  border: 1rpx solid #d4dce8;
  border-radius: 999rpx;
  color: #475569;
  font-size: 25rpx;
  font-weight: 700;
  line-height: 1.2;
  background: #fff;
  box-sizing: border-box;
  transition: color 0.16s ease, border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.pill-select__item::after {
  border: 0;
}

.pill-select__item--active {
  border-color: #1f5fbf;
  color: #fff;
  background: #1f5fbf;
}

.pill-select__item--pressed {
  transform: scale(0.97);
}

.pill-select__label {
  max-width: 100%;
  overflow-wrap: anywhere;
}
</style>
