<template>
  <view class="mobile-watermark" :style="containerStyle">
    <text v-for="index in 15" :key="index" class="mobile-watermark__item" :style="itemStyle">{{ content }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  content: { type: String, default: '内部资料' },
  options: { type: Object, default: () => ({}) },
})
const containerStyle = computed(() => ({
  gridTemplateColumns: `repeat(3, minmax(${Math.max(72, Number(props.options.width || 112))}rpx, 1fr))`,
  minHeight: `${Math.max(120, Number(props.options.height || 180))}rpx`,
}))
const itemStyle = computed(() => ({
  color: props.options.fontColor || 'rgba(100, 116, 139, .26)',
  fontSize: `${Math.max(10, Number(props.options.fontSize || 14))}px`,
  transform: `rotate(${Number(props.options.rotate ?? -22)}deg)`,
}))
</script>

<style lang="scss" scoped>
.mobile-watermark { display: grid; overflow: hidden; align-content: space-around; gap: 28rpx 16rpx; padding: 24rpx 10rpx; border: 1rpx dashed #e2e8f0; border-radius: 14rpx; background: #fbfdff; }
.mobile-watermark__item { overflow: hidden; text-align: center; text-overflow: ellipsis; white-space: nowrap; user-select: none; }
</style>
