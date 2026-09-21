<template>
  <view class="ai-dropdown-menu">
    <view class="dropdown-trigger" @click="toggleDropdown">
      <slot name="trigger">
        <view class="default-trigger">
          <text>{{ selectedLabel || placeholder }}</text>
        </view>
      </slot>
    </view>
    
    <view class="dropdown-overlay" v-if="visible" @click="close">
      <view class="dropdown-popup" @click.stop>
        <view 
          v-for="(option, index) in options" 
          :key="index"
          class="dropdown-option"
          :class="{ 'dropdown-option--selected': modelValue === option.value }"
          @click="selectOption(option)"
        >
          <text class="option-label">{{option.label}}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const visible = ref(false)

const selectedLabel = computed(() => {
  const selected = props.options.find(o => o.value === props.modelValue)
  return selected ? selected.label : ''
})

const toggleDropdown = () => {
  visible.value = !visible.value
}

const close = () => {
  visible.value = false
}

const selectOption = (option) => {
  emit('update:modelValue', option.value)
  emit('change', option.value)
  close()
}
</script>

<style lang="scss" scoped>
.ai-dropdown-menu {
  position: relative;
}

.dropdown-trigger {
  cursor: pointer;
}

.dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.dropdown-popup {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 12px 12px 0 0;
  max-height: 50vh;
  overflow-y: auto;
}

.dropdown-option {
  padding: 16px;
  border-bottom: 1px solid #eee;
  
  &--selected {
    color: var(--primary-color, #1f5fbf);
    background: #f5f5f5;
  }
}

.option-label {
  font-size: 14px;
}
</style>
