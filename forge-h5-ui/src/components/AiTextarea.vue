<template>
  <view
    class="ai-textarea"
    :class="{ 'is-error': !!error, 'is-disabled': disabled || readonly, 'is-focused': focused }"
    :style="{ minHeight }"
  >
    <wd-textarea
      class="ai-textarea__control"
      :model-value="modelValue"
      :placeholder="placeholder"
      :maxlength="Number(maxlength)"
      :disabled="disabled"
      :readonly="readonly"
      :auto-height="autoHeight"
      :show-word-limit="showWordLimit"
      :clearable="clearable"
      :error="!!error"
      no-border
      @update:model-value="emit('update:modelValue', $event)"
      @input="emit('input', $event?.value ?? $event?.detail?.value ?? $event)"
      @focus="handleFocus"
      @blur="handleBlur"
      @confirm="emit('confirm', $event)"
    />
    <text v-if="error" class="ai-textarea__error">{{ error }}</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  modelValue: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '' },
  maxlength: { type: [String, Number], default: 500 },
  minHeight: { type: String, default: '148rpx' },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  autoHeight: { type: Boolean, default: false },
  showWordLimit: { type: Boolean, default: true },
  clearable: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'input', 'focus', 'blur', 'confirm'])
const focused = ref(false)

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
.ai-textarea {
  width: 100%;
  padding: 8rpx 10rpx;
  border: 1rpx solid var(--forge-color-border, #e2e8f0);
  border-radius: var(--forge-radius-control, 12rpx);
  background: #fff;
  box-sizing: border-box;
  transition: border-color .18s ease, box-shadow .18s ease;
}

.ai-textarea.is-focused {
  border-color: var(--forge-color-primary, #1f5fbf);
  box-shadow: 0 0 0 3rpx rgba(31, 95, 191, .1);
}

.ai-textarea.is-error { border-color: var(--forge-color-danger, #dc2626); }
.ai-textarea.is-disabled { background: var(--forge-color-surface-subtle, #f8fafc); opacity: .76; }
.ai-textarea__control { width: 100%; }
.ai-textarea__error { display: block; padding: 2rpx 12rpx 8rpx; color: var(--forge-color-danger, #dc2626); font-size: 22rpx; }

:deep(.wd-textarea) {
  padding: 10rpx 8rpx;
  background: transparent;
}

:deep(.wd-textarea__inner) {
  min-height: 112rpx;
  color: var(--forge-color-text, #334155);
  font-size: 27rpx;
  line-height: 1.55;
}
</style>
