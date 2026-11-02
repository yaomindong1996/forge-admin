<template>
  <!-- 扩展字段：scheme → AiFormItem，值写入 extValues -->
  <AiFormItem
    :field="aiField"
    :value="modelValue"
    :form-data="formData"
    @update:value="onUpdate"
  />
</template>

<script setup>
import { computed } from 'vue'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import { schemeFieldToAiFormField } from './bizFormFieldTypes'

const props = defineProps({
  schemeField: { type: Object, required: true },
  modelValue: { type: [String, Number, Boolean, Array, Object], default: null },
  formData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue'])

const aiField = computed(() => schemeFieldToAiFormField(props.schemeField || {}))

function onUpdate(value) {
  emit('update:modelValue', value)
}
</script>
