<template>
  <view class="ai-search-bar" :class="{ 'ai-search-bar--focused': focused }">
    <view class="ai-search-bar__box">
      <AiIcon name="search" color="#94a3b8" size="sm" />
      <input
        class="ai-search-bar__input"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :focus="autoFocus"
        confirm-type="search"
        placeholder-class="ai-search-bar__placeholder"
        @input="handleInput"
        @confirm="handleSearch"
        @focus="handleFocus"
        @blur="handleBlur"
      />
      <button
        v-if="clearable && hasValue && !disabled"
        class="ai-search-bar__clear"
        type="button"
        @click="handleClear"
      >
        <text>×</text>
      </button>
    </view>
    <button v-if="showCancel || focused" class="ai-search-bar__cancel" type="button" @click="handleCancel">
      {{ cancelText }}
    </button>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import AiIcon from './AiIcon.vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '搜索'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  showCancel: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  autoFocus: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'input', 'search', 'clear', 'cancel', 'focus', 'blur'])
const focused = ref(false)
const hasValue = computed(() => String(props.modelValue || '').length > 0)

function handleInput(event) {
  const value = event.detail.value
  emit('update:modelValue', value)
  emit('input', value)
}

function handleSearch() {
  emit('search', props.modelValue)
}

function handleClear() {
  emit('update:modelValue', '')
  emit('clear')
}

function handleCancel() {
  emit('update:modelValue', '')
  emit('cancel')
}

function handleFocus(event) {
  focused.value = true
  emit('focus', event)
}

function handleBlur(event) {
  focused.value = false
  emit('blur', event)
}
</script>

<style lang="scss" scoped>
.ai-search-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.ai-search-bar__box {
  display: flex;
  min-width: 0;
  height: 68rpx;
  flex: 1;
  align-items: center;
  gap: 12rpx;
  padding: 0 18rpx;
  border: 1rpx solid var(--border-color);
  border-radius: 12rpx;
  background: #fdfefe;
  box-shadow: none;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.ai-search-bar--focused .ai-search-bar__box {
  border-color: var(--primary-color);
  background: #fff;
  box-shadow: 0 0 0 3rpx rgba(31, 95, 191, 0.1);
}

.ai-search-bar__input {
  min-width: 0;
  height: 66rpx;
  flex: 1;
  color: #334155;
  font-size: 26rpx;
  font-weight: 500;
}

:deep(.ai-search-bar__placeholder) {
  color: #94a3b8;
  font-weight: 500;
}

.ai-search-bar__clear,
.ai-search-bar__cancel {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
}

.ai-search-bar__clear::after,
.ai-search-bar__cancel::after {
  border: 0;
}

.ai-search-bar__clear {
  width: 42rpx;
  height: 42rpx;
  border-radius: 999rpx;
  color: #ffffff;
  font-size: 34rpx;
  line-height: 1;
  background: rgba(148, 163, 184, 0.68);
}

.ai-search-bar__cancel {
  flex-shrink: 0;
  height: 64rpx;
  color: #1f5fbf;
  font-size: 25rpx;
  font-weight: 650;
  background: transparent;
}
</style>
