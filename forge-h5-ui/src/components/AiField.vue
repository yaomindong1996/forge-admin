<template>
  <view class="ai-field" :class="[`ai-field--${layout}`]">
    <text v-if="label" class="ai-field__label">{{ label }}</text>
    <view class="ai-field__content">
      <view class="ai-field__control" :class="{ 'is-error': !!error, 'is-disabled': disabled, 'is-focused': focused }">
        <wd-input
          class="ai-field__input"
          :model-value="modelValue"
          :type="inputType"
          :show-password="type === 'password'"
          :placeholder="placeholder"
          :disabled="disabled"
          :maxlength="Number(maxlength)"
          :clearable="clearable"
          :error="!!error"
          no-border
          @update:model-value="handleModelUpdate"
          @input="handleInput"
          @focus="handleFocus"
          @blur="handleBlur"
          @clear="emit('clear')"
          @confirm="emit('confirm', $event)"
        >
          <template v-if="$slots.leftIcon" #prefix><slot name="leftIcon" /></template>
          <template v-if="$slots.rightIcon" #suffix><slot name="rightIcon" /></template>
        </wd-input>
      </view>
      <text v-if="error" class="ai-field__error">{{ error }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  error: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  type: { type: String, default: 'text' },
  clearable: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  layout: {
    type: String,
    default: 'vertical',
    validator: value => ['horizontal', 'vertical'].includes(value),
  },
  maxlength: { type: [String, Number], default: 140 },
})

const emit = defineEmits(['update:modelValue', 'input', 'clear', 'confirm', 'blur', 'focus'])
const focused = ref(false)
const inputType = computed(() => {
  if (props.type === 'password') return 'text'
  if (props.type === 'email') return 'text'
  return ['text', 'number', 'digit', 'idcard', 'tel', 'nickname'].includes(props.type) ? props.type : 'text'
})

function handleModelUpdate(value) {
  emit('update:modelValue', value)
}

function handleInput(event) {
  emit('input', event?.value ?? event?.detail?.value ?? event)
}

function handleFocus(event) {
  focused.value = true
  emit('focus', event)
}

function handleBlur(event) {
  focused.value = false
  emit('blur', event)
}
</script>

<style lang="scss" scoped>
.ai-field { display: flex; flex-direction: column; gap: 10rpx; }
.ai-field--horizontal { flex-direction: row; align-items: center; gap: 28rpx; }
.ai-field--horizontal .ai-field__label { width: 156rpx; flex: 0 0 auto; }
.ai-field--horizontal .ai-field__content { min-width: 0; flex: 1; }
.ai-field__label { padding-left: 4rpx; color: var(--forge-color-text-secondary, #475569); font-size: 26rpx; font-weight: 700; }
.ai-field__content { display: flex; flex-direction: column; gap: 8rpx; }
.ai-field__control { min-height: 80rpx; padding: 0 8rpx; border: 1rpx solid var(--forge-color-border, #e2e8f0); border-radius: var(--forge-radius-control, 12rpx); background: #fff; transition: border-color .18s ease, box-shadow .18s ease; }
.ai-field__control.is-focused { border-color: var(--forge-color-primary, #2563eb); box-shadow: 0 0 0 3rpx rgba(37, 99, 235, .1); }
.ai-field__control.is-error { border-color: var(--forge-color-danger, #dc2626); }
.ai-field__control.is-disabled { background: var(--forge-color-surface-subtle, #f8fafc); opacity: .72; }
.ai-field__input { width: 100%; }
.ai-field__error { padding: 0 12rpx; color: var(--forge-color-danger, #dc2626); font-size: 23rpx; }
:deep(.wd-input) { min-height: 78rpx; padding: 0 10rpx; background: transparent; }
:deep(.wd-input__inner) { color: var(--forge-color-text, #334155); font-size: 27rpx; }
</style>
