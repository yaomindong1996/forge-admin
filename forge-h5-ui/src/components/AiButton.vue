<template>
  <wd-button
    class="ai-button"
    :class="[`ai-button--${variant}`, `ai-button--${size}`]"
    :type="wotType"
    :size="wotSize"
    :plain="plain"
    :round="false"
    :block="block"
    :loading="loading"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <view class="ai-button__content">
      <view v-if="!loading && $slots.leftIcon" class="ai-button__icon"><slot name="leftIcon" /></view>
      <text class="ai-button__text"><slot /></text>
      <view v-if="!loading && $slots.rightIcon" class="ai-button__icon"><slot name="rightIcon" /></view>
    </view>
  </wd-button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: value => ['primary', 'secondary', 'outline', 'ghost', 'danger'].includes(value),
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg'].includes(value),
  },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])
const wotType = computed(() => ({
  primary: 'primary',
  secondary: 'default',
  outline: 'info',
  ghost: 'text',
  danger: 'error',
})[props.variant] || 'primary')
const wotSize = computed(() => ({ sm: 'small', md: 'medium', lg: 'large' })[props.size] || 'medium')
const plain = computed(() => ['secondary', 'outline', 'danger'].includes(props.variant))

function handleClick(event) {
  if (!props.disabled && !props.loading) emit('click', event)
}
</script>

<style lang="scss" scoped>
.ai-button {
  min-width: 0;
  margin: 0;
  border-radius: var(--forge-radius-control, 12rpx) !important;
  font-weight: 700;
  box-shadow: none !important;
}

.ai-button--sm { min-height: 64rpx; }
.ai-button--md { min-height: 80rpx; }
.ai-button--lg { min-height: 96rpx; }
.ai-button--secondary { color: var(--forge-color-text, #334155) !important; border-color: var(--forge-color-border, #e2e8f0) !important; background: #fff !important; }
.ai-button--outline { color: var(--forge-color-text, #334155) !important; border-color: var(--forge-color-border-strong, #cbd5e1) !important; background: #fff !important; }
.ai-button--ghost { color: var(--forge-color-text-secondary, #475569) !important; background: transparent !important; }
.ai-button--danger { color: var(--forge-color-danger, #dc2626) !important; border-color: #fecaca !important; background: #fff !important; }
.ai-button__content { display: flex; min-width: 0; align-items: center; justify-content: center; gap: 12rpx; }
.ai-button__text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-button__icon { display: flex; flex: 0 0 auto; align-items: center; justify-content: center; }
</style>
