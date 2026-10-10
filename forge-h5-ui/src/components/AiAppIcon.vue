<template>
  <view class="ai-app-icon" :class="`ai-app-icon--${size}`" :style="{ background: bg }">
    <AiIcon :icon="icon" :color="color" :size="size === 'sm' ? 'md' : 'lg'" />
  </view>
</template>

<script setup>
import AiIcon from '@/components/AiIcon.vue'

defineProps({
  icon: { type: String, default: '' },
  color: { type: String, default: '#0066ff' },
  bg: { type: String, default: '#ddf0ff' },
  size: { type: String, default: 'md', validator: value => ['sm', 'md'].includes(value) },
})
</script>

<style lang="scss" scoped>
.ai-app-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: var(--forge-radius-icon, 14px);
}

.ai-app-icon--md {
  width: 48px;
  height: 48px;
}

.ai-app-icon--sm {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}
</style>
