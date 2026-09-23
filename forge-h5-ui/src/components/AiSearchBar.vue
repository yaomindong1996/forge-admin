<template>
  <view
    class="ai-search-bar"
    :class="{ 'ai-search-bar--focused': focused, 'ai-search-bar--not-clearable': !clearable }"
  >
    <wd-search
      class="ai-search-bar__control"
      :model-value="modelValue"
      :placeholder="placeholder"
      :cancel-txt="cancelText"
      :hide-cancel="!(showCancel || focused)"
      :disabled="disabled"
      :focus="autoFocus"
      placeholder-left
      light
      focus-when-clear
      @update:model-value="handleModelUpdate"
      @change="handleInput"
      @search="handleSearch"
      @clear="handleClear"
      @cancel="handleCancel"
      @focus="handleFocus"
      @blur="handleBlur"
    />
  </view>
</template>

<script setup>
import { ref } from 'vue'

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

function handleModelUpdate(value) {
  emit('update:modelValue', value)
}

function handleInput(event) {
  const value = event?.value ?? event?.detail?.value ?? event
  emit('input', value)
}

function handleSearch(event) {
  emit('search', event?.value ?? props.modelValue)
}

function handleClear() {
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
  width: 100%;
}

:deep(.wd-search) {
  padding: 0;
  background: transparent;
}

:deep(.wd-search__block) {
  display: flex;
  min-height: 88rpx;
  align-items: center;
  padding: 0 24rpx;
  border: 1rpx solid var(--border-color, #c9cdd4);
  border-radius: var(--radius-control, 12rpx);
  background: #fff;
  transition: border-color .16s ease, background-color .16s ease;
}

.ai-search-bar--focused :deep(.wd-search__block) {
  border-color: var(--primary-color, #4266f7);
}

:deep(.wd-search__field) {
  display: flex;
  height: 86rpx;
  align-items: center;
  background: transparent;
}

:deep(.wd-search__input) {
  height: 86rpx;
  padding: 0;
  color: #1d2129;
  font-size: 28rpx;
  font-weight: 400;
  line-height: 86rpx;
  box-sizing: border-box;
}

:deep(.wd-search__placeholder-txt) {
  color: #86909c;
  font-size: 28rpx;
  font-weight: 400;
  line-height: 86rpx;
}

:deep(.wd-search__search-icon),
:deep(.wd-search__clear) { display: flex; align-items: center; align-self: stretch; }

:deep(.wd-search__cancel) {
  color: var(--primary-color, #4266f7);
  font-size: 26rpx;
  font-weight: 500;
}

.ai-search-bar--not-clearable :deep(.wd-search__clear) {
  display: none;
}
</style>
