<template>
  <view class="ai-field" :class="[`ai-field--${layout}`]">
    <text v-if="label" class="ai-field__label">
      <text v-if="required" class="ai-field__required">*</text>{{ label }}
    </text>
    <view class="ai-field__content">
      <view class="ai-field__control" :class="{ 'is-error': !!error, 'is-disabled': disabled, 'is-focused': focused }">
        <wd-input
          class="ai-field__input"
          :model-value="modelValue"
          :type="inputType"
          :show-password="type === 'password'"
          :placeholder="placeholder"
          :disabled="disabled"
          :readonly="readonly"
          :focus="autofocus"
          :maxlength="Number(maxlength)"
          :clearable="clearable"
          :show-word-limit="showCount"
          :error="!!error"
          no-border
          @update:model-value="handleModelUpdate"
          @input="handleInput"
          @focus="handleFocus"
          @blur="handleBlur"
          @clear="emit('clear')"
          @confirm="emit('confirm', $event)"
        >
          <template v-if="prefix || $slots.leftIcon" #prefix>
            <slot name="leftIcon"><text class="ai-field__affix">{{ prefix }}</text></slot>
          </template>
          <template v-if="suffix || $slots.rightIcon" #suffix>
            <slot name="rightIcon"><text class="ai-field__affix">{{ suffix }}</text></slot>
          </template>
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
  readonly: { type: Boolean, default: false },
  autofocus: { type: Boolean, default: false },
  showCount: { type: Boolean, default: false },
  prefix: { type: [String, Number], default: '' },
  suffix: { type: [String, Number], default: '' },
  required: { type: Boolean, default: false },
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
.ai-field { display: flex; flex-direction: column; gap: 5px; }
.ai-field--horizontal { flex-direction: row; align-items: center; gap: 14px; }
.ai-field--horizontal .ai-field__label { width: 78px; flex: 0 0 auto; }
.ai-field--horizontal .ai-field__content { min-width: 0; flex: 1; }
.ai-field__label { color: var(--forge-color-text-secondary, #747677); font-size: 14px; font-weight: 600; line-height: 1.5; }
.ai-field__required { margin-right: 3px; color: var(--forge-color-danger, #ff5219); }
.ai-field__content { display: flex; flex-direction: column; gap: 4px; }
.ai-field__control { display: flex; height: 44px; align-items: center; padding: 0 12px; border: 1px solid var(--forge-color-border-subtle, #f5f6f7); border-radius: var(--forge-radius-control); background: #f7f8fa; box-sizing: border-box; transition: border-color .16s ease, background-color .16s ease, box-shadow .16s ease; }
.ai-field__control.is-focused { border-color: var(--forge-color-primary, #0066ff); background: #fff; box-shadow: 0 0 0 3px rgba(0, 102, 255, .1); }
.ai-field__control.is-error { border-color: var(--forge-color-danger, #ff5219); }
.ai-field__control.is-disabled { background: var(--forge-color-surface-subtle, #f7f8fa); opacity: .72; }
.ai-field__input { width: 100%; min-width: 0; }
.ai-field__error { color: var(--forge-color-danger, #ff5219); font-size: 12px; line-height: 1.5; }
.ai-field__affix { color: var(--forge-color-text-secondary, #747677); font-size: 14px; line-height: 1; }
:deep(.wd-input), :deep(.wd-input__body), :deep(.wd-input__value) { display: flex; width: 100%; min-width: 0; height: 42px; align-items: center; padding: 0; background: transparent; box-sizing: border-box; }
:deep(.wd-input__prefix),
:deep(.wd-input__suffix) { display: flex; align-items: center; align-self: stretch; }
:deep(.wd-input__inner) { display: flex; height: 42px; min-height: 42px; align-items: center; padding: 0; color: var(--forge-color-text, #171a1d); font-size: 14px; font-weight: 400; line-height: normal; box-sizing: border-box; }
:deep(.uni-input-wrapper), :deep(.uni-input-form), :deep(.uni-input-input), :deep(.uni-input-placeholder) { height: 42px; min-height: 42px; line-height: 42px; box-sizing: border-box; }
</style>
