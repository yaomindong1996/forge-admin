<!--
  远程搜索下拉选择组件
  基于 Naive UI 的 n-select，支持远程数据加载
-->

<template>
  <n-select
    :value="value"
    :placeholder="placeholder"
    :options="selectOptions"
    :loading="loading"
    :disabled="disabled"
    :clearable="clearable"
    :filterable="filterable"
    :multiple="multiple"
    :remote="remote"
    :on-search="handleSearch"
    :label-field="labelField"
    @update:value="handleUpdate"
    :value-field="valueField"
    style="width: 100%"
    v-bind="$attrs"
  />
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  // 当前值
  value: {
    type: [String, Number, Array],
    default: null,
  },
  // 占位符
  placeholder: {
    type: String,
    default: '请选择',
  },
  // 接口地址
  api: {
    type: String,
    default: '',
  },
  // 请求方法
  method: {
    type: String,
    default: 'get',
  },
  // 显示字段名
  labelField: {
    type: String,
    default: 'label',
  },
  // 值字段名
  valueField: {
    type: String,
    default: 'value',
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
  // 是否远程搜索
  remote: {
    type: Boolean,
    default: false,
  },
  // 静态选项数据（如果不需要远程加载）
  options: {
    type: Array,
    default: () => [],
  },
  // 请求参数
  params: {
    type: Object,
    default: () => ({}),
  },
  // 数据转换函数
  transform: {
    type: Function,
    default: null,
  },
})

const emit = defineEmits(['update:value', 'change'])

const loading = ref(false)
const selectOptions = ref([])

// 监听静态选项变化
watch(() => props.options, (newOptions) => {
  if (newOptions && newOptions.length > 0) {
    selectOptions.value = newOptions
  }
}, { immediate: true })

// 加载数据
async function loadData(searchQuery = '') {
  if (!props.api) {
    return
  }

  loading.value = true

  try {
    const requestParams = {
      ...props.params,
    }

    // 如果是远程搜索，添加搜索关键词
    if (props.remote && searchQuery) {
      requestParams.keyword = searchQuery
    }

    const response = await request[props.method.toLowerCase()](props.api, requestParams)

    let data = response.data || []

    // 如果提供了数据转换函数，使用它
    if (props.transform && typeof props.transform === 'function') {
      data = props.transform(data)
    }
    else {
      // 默认转换
      data = data.map(item => ({
        label: item[props.labelField],
        value: item[props.valueField],
        ...item,
      }))
    }

    selectOptions.value = data
  }
  catch (error) {
    console.error('加载选项数据失败:', error)
    selectOptions.value = []
  }
  finally {
    loading.value = false
  }
}

// 远程搜索处理
function handleSearch(query) {
  if (props.remote) {
    loadData(query)
  }
}

// 值更新处理
function handleUpdate(newValue) {
  emit('update:value', newValue)

  // 触发 change 事件，传递完整的选中项数据
  if (newValue !== null && newValue !== undefined) {
    if (props.multiple && Array.isArray(newValue)) {
      const selectedItems = selectOptions.value.filter(item =>
        newValue.includes(item[props.valueField]),
      )
      emit('change', newValue, selectedItems)
    }
    else {
      const selectedItem = selectOptions.value.find(item =>
        item[props.valueField] === newValue,
      )
      emit('change', newValue, selectedItem || {})
    }
  }
  else {
    emit('change', newValue, null)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  if (props.api && !props.remote) {
    loadData()
  }
})

// 暴露方法
defineExpose({
  loadData,
  refresh: loadData,
})
</script>
