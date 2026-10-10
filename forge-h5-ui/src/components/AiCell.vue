<template>
  <view
    class="ai-cell"
    :class="{
      'ai-cell--clickable': clickable,
      'ai-cell--border': border
    }"
    :hover-class="clickable ? 'ai-cell--hover' : 'none'"
    @click="handleClick"
  >
    <view
      v-if="$slots.icon || icon"
      class="ai-cell__icon"
      :style="{ background: iconBg }"
    >
      <slot name="icon">
        <image v-if="icon" class="ai-cell__icon-image" :src="resolvedIcon" mode="aspectFit" />
      </slot>
    </view>

    <view class="ai-cell__main">
      <text class="ai-cell__title">{{ title }}</text>
      <text v-if="label" class="ai-cell__label">{{ label }}</text>
      <slot name="label" />
    </view>

    <view class="ai-cell__right">
      <text v-if="value" class="ai-cell__value">{{ value }}</text>
      <slot name="value" />
      <text v-if="isLink" class="ai-cell__arrow">›</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { resolveStaticUrl } from '@/utils/assets'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  iconBg: {
    type: String,
    default: 'var(--forge-tone-blue-bg)'
  },
  isLink: {
    type: Boolean,
    default: false
  },
  border: {
    type: Boolean,
    default: true
  },
  clickable: {
    type: Boolean,
    default: false
  }
})

const resolvedIcon = computed(() => resolveStaticUrl(props.icon))

const emit = defineEmits(['click'])

const handleClick = (event) => {
  if (props.clickable || props.isLink) {
    emit('click', event)
  }
}
</script>

<style lang="scss" scoped>
.ai-cell {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 52px;
  padding: 10px 16px;
  box-sizing: border-box;
  transition: background 0.15s ease;

  /* 分隔线从文字起始处开始；有图标时让出图标宽度 */
  &--border::after {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 16px;
    height: 1px;
    background: var(--forge-border);
    content: '';
    transform: scaleY(0.5);
  }

  &--border:has(.ai-cell__icon)::after {
    left: 60px;
  }

  &:last-child::after {
    display: none;
  }

  &--clickable {
    cursor: pointer;
  }

  &--hover {
    background: var(--surface-muted);
  }
}

.ai-cell__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  margin-right: 12px;
  border-radius: 10px;
}

.ai-cell__icon-image {
  width: 20px;
  height: 20px;
}

.ai-cell__main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.ai-cell__title {
  overflow: hidden;
  color: var(--forge-text-primary);
  font-size: 16px;
  font-weight: 400;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-cell__label {
  overflow: hidden;
  color: var(--forge-text-secondary);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-cell__right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 4px;
  max-width: 45%;
  margin-left: 12px;
}

.ai-cell__value {
  overflow: hidden;
  color: var(--forge-text-tertiary);
  font-size: 14px;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-cell__arrow {
  color: var(--forge-arrow);
  font-size: 22px;
  font-weight: 300;
  line-height: 1;
}
</style>
