<template>
  <div class="path-field-picker">
    <span class="path-field-picker__label">{{ label }}</span>
    <NSelect
      :value="value || null"
      :options="options"
      :placeholder="placeholderText"
      :loading="false"
      size="small"
      filterable
      tag
      clearable
      :persistent="true"
      @update:value="emit('update:value', $event || '')"
    />
  </div>
</template>

<script setup>
import { NSelect } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  value: { type: String, default: '' },
  label: { type: String, required: true },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const placeholderText = computed(() => {
  if (props.options.length)
    return props.placeholder || '点击选择，或输入自定义路径'
  return '先在「返回字段解析」中定义字段'
})
</script>

<style scoped>
.path-field-picker {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.path-field-picker__label {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}
</style>
