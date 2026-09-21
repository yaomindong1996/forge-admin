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
    gap: 0;
    padding: 0;
    display: flex;
    border: 1rpx solid #e5e7eb;
    border-radius: var(--radius-control);
    background: #fff;
    overflow: hidden;
  }
  
  &-tab {
    flex: 1;
    display: flex;
    height: 64rpx;
    align-items: center;
    justify-content: center;
    border-right: 1rpx solid var(--border-color);

    &:last-child {
      border-right: 0;
    }
    
    &--active {
      color: var(--primary-color);
      background: var(--primary-soft, #edf4ff);
    }
  }
  
  &-tab-text {
    color: var(--text-secondary);
    font-size: 23rpx;
    
    .ai-tabs-tab--active & {
      color: var(--primary-color);
      font-weight: 650;
    }
  }
}
</style>
