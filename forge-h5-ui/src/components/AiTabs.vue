<template>
  <view class="ai-tabs">
    <view class="ai-tabs-header">
      <view
        v-for="(tab, index) in tabs" 
        :key="index"
        class="ai-tabs-tab"
        :class="{ 'ai-tabs-tab--active': activeIndex === index }"
        @click="handleTabClick(index)"
      >
        <text class="ai-tabs-tab-text">{{tab.label || tab}}</text>
      </view>
    </view>
    <view class="ai-tabs-content">
      <slot></slot>
    </view>
  </view>
</template>

<script setup>
import { computed, provide } from 'vue'

const props = defineProps({
  tabs: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const activeIndex = computed(() => props.modelValue)

const handleTabClick = (index) => {
  emit('update:modelValue', index)
  emit('change', index)
}

provide('activeIndex', activeIndex)
</script>

<style lang="scss" scoped>
.ai-tabs {
  &-header {
    gap: 36rpx;
    padding: 0;
    display: flex;
    overflow-x: auto;
    border-bottom: 1rpx solid var(--border-light);
    background: #fff;
    white-space: nowrap;
  }
  
  &-tab {
    position: relative;
    flex: 0 0 auto;
    display: flex;
    min-width: 96rpx;
    min-height: 88rpx;
    align-items: center;
    justify-content: center;
    padding: 0 4rpx;
    
    &--active {
      color: var(--primary-color);
      background: #fff;
    }

    &--active::after {
      position: absolute;
      right: 0;
      bottom: -1rpx;
      left: 0;
      height: 4rpx;
      border-radius: 4rpx 4rpx 0 0;
      background: var(--primary-color);
      content: '';
    }
  }
  
  &-tab-text {
    color: var(--text-secondary);
    font-size: 28rpx;
    
    .ai-tabs-tab--active & {
      color: var(--text-strong);
      font-weight: 500;
    }
  }
}
</style>
