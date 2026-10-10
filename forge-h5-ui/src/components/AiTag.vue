<template>
  <view
    class="ai-tag"
    :class="[
      `ai-tag--${normalizedType}`,
      `ai-tag--${variant}`,
      `ai-tag--${normalizedSize}`,
      { 'ai-tag--round': round }
    ]"
    :style="customStyle"
  >
    <text class="ai-tag__text"><slot /></text>
    <button v-if="closable" class="ai-tag__close" type="button" @click.stop="emit('close')">
      <text>×</text>
    </button>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default'
  },
  variant: {
    type: String,
    default: 'soft',
    validator: value => ['solid', 'soft', 'outline'].includes(value)
  },
  size: {
    type: String,
    default: 'md'
  },
  color: {
    type: String,
    default: ''
  },
  closable: {
    type: Boolean,
    default: false
  },
  round: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const normalizedType = computed(() => {
  if (props.type === 'error') return 'danger'
  if (['primary', 'success', 'warning', 'danger', 'default'].includes(props.type)) return props.type
  return 'default'
})

const normalizedSize = computed(() => {
  const sizeMap = {
    small: 'sm',
    medium: 'md',
    large: 'lg'
  }
  return sizeMap[props.size] || props.size
})

const customStyle = computed(() => {
  if (!props.color) return {}
  return props.variant === 'solid'
    ? {
        backgroundColor: props.color,
        borderColor: props.color,
        color: '#fff'
      }
    : {
        borderColor: props.color,
        color: props.color
      }
})
</script>

<style lang="scss" scoped>
.ai-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  max-width: 100%;
  border: 1px solid transparent;
  border-radius: 4px;
  font-weight: 400;
  letter-spacing: 0;
  line-height: 1;
  transition: opacity 0.2s ease, transform 0.2s ease;

  &--round {
    border-radius: 999px;
  }

  &--sm {
    gap: 4px;
    min-height: 18px;
    padding: 2px 6px;
    font-size: 11px;
  }

  &--md {
    gap: 4px;
    min-height: 22px;
    padding: 3px 8px;
    font-size: 12px;
  }

  &--lg {
    gap: 6px;
    min-height: 26px;
    padding: 5px 10px;
    font-size: 14px;
  }

  &--primary {
    &.ai-tag--solid {
      color: #fff;
      background: var(--primary-color);
    }
    &.ai-tag--soft {
      color: var(--primary-color);
      border-color: transparent;
      background: var(--primary-soft);
    }
    &.ai-tag--outline {
      color: var(--primary-color);
      border-color: var(--forge-color-primary-border);
      background: transparent;
    }
  }

  &--success {
    &.ai-tag--solid {
      color: #fff;
      background: #12b76a;
    }
    &.ai-tag--soft {
      color: #12b76a;
      border-color: transparent;
      background: var(--forge-tone-green-bg);
    }
    &.ai-tag--outline {
      color: #12b76a;
      border-color: rgba(18, 183, 106, 0.4);
      background: transparent;
    }
  }

  &--warning {
    &.ai-tag--solid {
      color: #fff;
      background: #fd8838;
    }
    &.ai-tag--soft {
      color: #fd8838;
      border-color: transparent;
      background: var(--forge-tone-orange-bg);
    }
    &.ai-tag--outline {
      color: #fd8838;
      border-color: rgba(253, 136, 56, 0.4);
      background: transparent;
    }
  }

  &--danger {
    &.ai-tag--solid {
      color: #fff;
      background: #ff5219;
    }
    &.ai-tag--soft {
      color: #ff5219;
      border-color: transparent;
      background: var(--forge-tone-red-bg);
    }
    &.ai-tag--outline {
      color: #ff5219;
      border-color: rgba(255, 82, 25, 0.4);
      background: transparent;
    }
  }

  &--default {
    &.ai-tag--solid {
      color: #fff;
      background: #171a1d;
    }
    &.ai-tag--soft {
      color: #747677;
      border-color: transparent;
      background: var(--forge-surface-subtle);
    }
    &.ai-tag--outline {
      color: #747677;
      border-color: var(--forge-border-strong);
      background: transparent;
    }
  }
}

.ai-tag__text {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-tag__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28rpx;
  height: 28rpx;
  margin: 0 -4rpx 0 0;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  background: transparent;
  color: currentColor;
  font-size: 26rpx;
  font-weight: 500;
  line-height: 1;

  &::after {
    border: 0;
  }
}
</style>
