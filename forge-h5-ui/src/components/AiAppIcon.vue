<template>
  <!-- 彩色图片图标自带底板，直接铺满；线性图标保持浅底 + 着色 -->
  <image v-if="imageSrc" class="ai-app-icon is-image" :class="`ai-app-icon--${size}`" :src="imageSrc" mode="aspectFit" />
  <view v-else class="ai-app-icon" :class="`ai-app-icon--${size}`" :style="{ background: bg }">
    <AiIcon :icon="icon" :color="color" :size="size === 'sm' ? 'md' : 'lg'" />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiIcon from '@/components/AiIcon.vue'
import { resolveStaticUrl } from '@/utils/assets'
import { isImageIcon } from '@/utils/mobile-menu'

const props = defineProps({
  icon: { type: String, default: '' },
  color: { type: String, default: '#0066ff' },
  bg: { type: String, default: '#ddf0ff' },
  size: { type: String, default: 'md', validator: value => ['sm', 'md'].includes(value) },
})

const imageSrc = computed(() => (isImageIcon(props.icon) ? resolveStaticUrl(props.icon) : ''))
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

.ai-app-icon.is-image {
  display: block;
  border-radius: 0;
}
</style>
