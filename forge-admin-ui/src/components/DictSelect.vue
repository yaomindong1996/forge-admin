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

  // 当前表单数据，用于字典级联过滤
  formData: {
    type: Object,
    default: () => ({}),
  },

  // { enabled, sourceField, mode: parentDictCode | linkedDict }
  cascade: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:value'])

const dictList = ref([])
const loading = ref(false)

// 字典选项
const dictOptions = computed(() => {
  return filterDictList(dictList.value).map(item => ({
    label: item.label,
    value: item.value,
    disabled: item.status === 0, // 状态为 0 时禁用
    raw: item.raw || item,
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

function filterDictList(list = []) {
  const cascade = props.cascade || {}
  if (!cascade.enabled || !cascade.sourceField)
    return list
  const sourceValue = props.formData?.[cascade.sourceField]
  if (sourceValue === null || sourceValue === undefined || sourceValue === '')
    return []
  const mode = cascade.mode || cascade.matchMode || 'linkedDict'
  return list.filter((item) => {
    const raw = item.raw || item
    if (mode === 'parentDictCode') {
      return isSameValue(raw.parentDictCode ?? item.parentDictCode, sourceValue)
        || isSameValue(raw.parentDictCode ?? item.parentDictCode, resolveSourceDictCode(sourceValue))
    }
    if (mode === 'linkedDict') {
      const linkedType = cascade.linkedDictType || cascade.sourceDictType
      const typeMatched = !linkedType || isSameValue(raw.linkedDictType ?? item.linkedDictType, linkedType)
      return typeMatched && isSameValue(raw.linkedDictValue ?? item.linkedDictValue, sourceValue)
    }
    return true
  })
}

function resolveSourceDictCode(sourceValue) {
  const sourceOptions = props.cascade?.sourceOptions || []
  const matched = sourceOptions.find(item => isSameValue(item.value, sourceValue))
  return matched?.dictCode ?? matched?.raw?.dictCode ?? sourceValue
}

function isSameValue(left, right) {
  if (left === right)
    return true
  if (left === null || left === undefined || right === null || right === undefined)
    return false
  return String(left) === String(right)
}
</script>
