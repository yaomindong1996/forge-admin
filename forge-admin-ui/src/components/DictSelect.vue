<!--
  字典选择器组件
  用于在表单中选择字典项

  使用示例：
  <DictSelect v-model:value="formData.status" dict-type="case_status" />
  <DictSelect v-model:value="formData.types" dict-type="matter_type" multiple />
-->

<template>
  <n-select
    :value="value"
    :options="dictOptions"
    :placeholder="placeholder"
    :disabled="disabled"
    :clearable="clearable"
    :filterable="filterable"
    :multiple="multiple"
    :loading="loading"
    v-bind="$attrs"
    @update:value="handleUpdate"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDictData } from '@/composables/useDict'

const props = defineProps({
  // v-model 绑定值
  value: {
    type: [String, Number, Array],
    default: null,
  },

  // 字典类型
  dictType: {
    type: String,
    required: true,
  },

  // 占位符
  placeholder: {
    type: String,
    default: '请选择',
  },

  // 是否禁用
  disabled: {
    type: Boolean,
    default: false,
  },

  // 是否可清空
  clearable: {
    type: Boolean,
    default: true,
  },

  // 是否可搜索
  filterable: {
    type: Boolean,
    default: true,
  },

  // 是否多选
  multiple: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:value'])

const dictList = ref([])
const loading = ref(false)

// 字典选项
const dictOptions = computed(() => {
  return dictList.value.map(item => ({
    label: item.label,
    value: item.value,
    disabled: item.status === 0, // 状态为 0 时禁用
  }))
})

// 加载字典数据
async function loadDict() {
  if (!props.dictType) {
    console.warn('DictSelect: 未指定 dictType')
    return
  }

  loading.value = true
  try {
    dictList.value = await getDictData(props.dictType)
  }
  finally {
    loading.value = false
  }
}

// 监听 dictType 变化
watch(() => props.dictType, () => {
  loadDict()
}, { immediate: true })

// 更新值
function handleUpdate(val) {
  emit('update:value', val)
}
</script>
