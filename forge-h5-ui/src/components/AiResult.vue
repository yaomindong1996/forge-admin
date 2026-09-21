<template>
  <view class="ai-result" :class="[`ai-result--${currentType}`]">
    <view class="ai-result__icon-wrap" :style="{ color: currentColor }">
      <AiIcon :name="currentIcon" :color="currentColor" size="xl" />
    </view>
    <text class="ai-result__title">{{ resolvedTitle }}</text>
    <text v-if="resolvedDescription" class="ai-result__desc">{{ resolvedDescription }}</text>

    <view v-if="$slots.default" class="ai-result__extra">
      <slot />
    </view>

    <view v-if="primaryText || secondaryText || $slots.actions" class="ai-result__actions">
      <slot name="actions">
        <AiButton v-if="secondaryText" block variant="secondary" @click="$emit('secondary')">
          {{ secondaryText }}
        </AiButton>
        <AiButton v-if="primaryText" block @click="$emit('primary')">
          {{ primaryText }}
        </AiButton>
      </slot>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiButton from './AiButton.vue'
import AiIcon from './AiIcon.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'info',
    validator: value => ['success', 'error', 'warning', 'info', 'empty', 'forbidden', 'notFound', 'network'].includes(value)
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  primaryText: {
    type: String,
    default: ''
  },
  secondaryText: {
    type: String,
    default: ''
  }
})

defineEmits(['primary', 'secondary'])

const preset = {
  success: {
    icon: 'check-circle',
    color: '#10b981',
    title: '操作成功',
    description: '当前操作已完成。'
  },
  error: {
    icon: 'x-circle',
    color: '#ef4444',
    title: '操作失败',
    description: '请稍后重试或检查当前信息。'
  },
  warning: {
    icon: 'alert-triangle',
    color: '#d97706',
    title: '请注意',
    description: '当前操作需要确认后继续。'
  },
  info: {
    icon: 'info',
    color: '#1f5fbf',
    title: '提示信息',
    description: '这里展示当前页面的提示内容。'
  },
  empty: {
    icon: 'inbox',
    color: '#64748b',
    title: '暂无内容',
    description: '当前没有可展示的数据。'
  },
  forbidden: {
    icon: 'shield-off',
    color: '#526071',
    title: '暂无权限',
    description: '你没有访问当前内容的权限。'
  },
  notFound: {
    icon: 'compass',
    color: '#64748b',
    title: '页面不存在',
    description: '当前页面可能已移动或被删除。'
  },
  network: {
    icon: 'wifi-off',
    color: '#1f5fbf',
    title: '网络异常',
    description: '请检查网络连接后重试。'
  }
}

const currentType = computed(() => props.type || 'info')
const currentPreset = computed(() => preset[currentType.value] || preset.info)
const currentIcon = computed(() => props.icon || currentPreset.value.icon)
const currentColor = computed(() => currentPreset.value.color)
const resolvedTitle = computed(() => props.title || currentPreset.value.title)
const resolvedDescription = computed(() => props.description || currentPreset.value.description)
</script>

<style lang="scss" scoped>
.ai-result {
  display: flex;
  min-height: 520rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56rpx 36rpx;
  box-sizing: border-box;
  text-align: center;
}

.ai-result__icon-wrap {
  display: flex;
  width: 88rpx;
  height: 88rpx;
  align-items: center;
  justify-content: center;
  border: 1rpx solid var(--forge-border, #dfe4ea);
  border-radius: 10rpx;
  background: var(--forge-color-primary-soft, #edf4ff);
}

.ai-result__title {
  display: block;
  max-width: 620rpx;
  margin-top: 24rpx;
  color: var(--forge-text-primary, #161b22);
  font-size: 31rpx;
  font-weight: 650;
  line-height: 1.35;
}

.ai-result__desc {
  display: block;
  max-width: 620rpx;
  margin-top: 10rpx;
  color: var(--forge-text-secondary, #4b5563);
  font-size: 23rpx;
  font-weight: 400;
  line-height: 1.6;
}

.ai-result__extra {
  width: 100%;
  margin-top: 28rpx;
}

.ai-result__actions {
  display: flex;
  width: 100%;
  max-width: 560rpx;
  gap: 12rpx;
  margin-top: 32rpx;
}
</style>
