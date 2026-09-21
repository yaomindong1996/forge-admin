<template>
  <view class="ai-avatar" :class="[`ai-avatar--${shape}`, `ai-avatar--${size}`]">
    <image class="ai-avatar__image" :src="currentSrc" mode="aspectFit" @error="failed = true" />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { DEFAULT_AVATAR_URL } from '@/utils/file'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg', 'xl'].includes(value)
  },
  shape: {
    type: String,
    default: 'circle',
    validator: value => ['circle', 'square'].includes(value)
  }
})

const failed = ref(false)
const currentSrc = computed(() => (!props.src || failed.value) ? DEFAULT_AVATAR_URL : props.src)

watch(() => props.src, () => {
  failed.value = false
})
</script>

<style lang="scss" scoped>
.ai-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-sizing: border-box;
  border: 1rpx solid var(--border-color);
  background: var(--surface-muted);
}

.ai-avatar--circle {
  border-radius: 999rpx;
}

.ai-avatar--square {
  border-radius: var(--radius-control);
}

.ai-avatar--sm {
  width: 72rpx;
  height: 72rpx;
}

.ai-avatar--md {
  width: 96rpx;
  height: 96rpx;
}

.ai-avatar--lg {
  width: 128rpx;
  height: 128rpx;
}

.ai-avatar--xl {
  width: 156rpx;
  height: 156rpx;
}

.ai-avatar__image {
  width: 100%;
  height: 100%;
  padding: 18%;
  box-sizing: border-box;
}
</style>
