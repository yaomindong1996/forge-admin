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
      <view class="pill-select__indicator"><text v-if="isSelected(option)">✓</text></view>
      <view class="pill-select__copy">
        <text class="pill-select__label">{{ option.label }}</text>
        <text v-if="option.description || option.desc" class="pill-select__desc">{{ option.description || option.desc }}</text>
      </view>
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
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16rpx;
  min-height: 88rpx;
}

.pill-select--disabled {
  opacity: 0.55;
}

.pill-select__item {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 160rpx;
  align-items: center;
  justify-content: flex-start;
  gap: 20rpx;
  margin: 0;
  padding: 24rpx 32rpx;
  border: 1rpx solid var(--border-color);
  border-radius: var(--radius-control);
  color: #4e5969;
  font-size: 28rpx;
  font-weight: 400;
  line-height: 1.2;
  background: #fff;
  box-sizing: border-box;
  text-align: left;
  transition: color 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}

.pill-select__item::after {
  border: 0;
}

.pill-select__item--active {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: var(--primary-soft);
}

.pill-select__item--pressed {
  background: var(--surface-muted);
}

.pill-select__indicator {
  display: flex;
  width: 36rpx;
  height: 36rpx;
  flex: 0 0 36rpx;
  align-items: center;
  justify-content: center;
  border: 2rpx solid var(--border-color);
  border-radius: 50%;
  color: #fff;
  font-size: 22rpx;
  background: #fff;
  box-sizing: border-box;
}

.pill-select__item--active .pill-select__indicator {
  border-color: var(--primary-color);
  background: var(--primary-color);
}

.pill-select__copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 8rpx; }
.pill-select__label {
  display: block;
  max-width: 100%;
  overflow-wrap: anywhere;
}

.pill-select__desc { display: block; color: var(--text-muted); font-size: 24rpx; font-weight: 400; line-height: 1.5; }

@media (min-width: 1024px) {
  .pill-select { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
