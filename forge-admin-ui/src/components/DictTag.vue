<!--
  字典标签组件
  用于显示字典值对应的标签

  使用示例：
  <DictTag :options="dict.case_status" :value="row.status" />
  <DictTag dict-type="case_status" :value="row.status" />
  <DictTag :options="dict.matter_type" :value="row.matterType" type="success" />
-->

<template>
  <!-- 如果是 default 类型且没有强制指定 type，显示普通文字 -->
  <span v-if="currentDict && shouldShowAsText">
    {{ currentDict.label }}
  </span>
  <!-- 否则显示标签 -->
  <n-tag
    v-else-if="currentDict"
    :type="tagType"
    :size="size"
    :round="round"
    :bordered="bordered"
    :closable="closable"
    @close="handleClose"
  >
    {{ currentDict.label }}
  </n-tag>
  <!-- 没有找到字典项，显示原始值 -->
  <span v-else>{{ value }}</span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDictData } from '@/composables/useDict'

const props = defineProps({
  // 字典选项列表（优先使用）
  options: {
    type: Array,
    default: null,
  },

  // 字典类型（当 options 为空时使用）
  dictType: {
    type: String,
    default: '',
  },

  // 字典值
  value: {
    type: [String, Number],
    default: '',
  },

  // 标签类型（可选：default, success, warning, error, info）
  // 如果不指定，会根据字典项的 listClass 自动判断
  type: {
    type: String,
    default: '',
  },

  // 标签尺寸
  size: {
    type: String,
    default: 'small',
  },

  // 是否圆角
  round: {
    type: Boolean,
    default: false,
  },

  // 是否显示边框
  bordered: {
    type: Boolean,
    default: true,
  },

  // 是否可关闭
  closable: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close'])

const dictList = ref([])

// 当前字典项
const currentDict = computed(() => {
  const list = props.options || dictList.value
  if (!list || list.length === 0)
    return null

  return list.find(item => String(item.value) === String(props.value))
})

// 标签类型
const tagType = computed(() => {
  if (props.type) {
    return props.type
  }

  if (!currentDict.value) {
    return 'default'
  }

  // 根据 listClass 映射标签类型
  const listClass = currentDict.value.listClass || currentDict.value.raw?.listClass

  // 如果没有 listClass，返回默认类型
  if (!listClass) {
    return 'default'
  }

  // 标签类型映射（兼容新旧命名）
  const typeMap = {
    default: 'default',
    success: 'success',
    info: 'info',
    warning: 'warning',
    error: 'error',
    // 兼容旧的命名
    primary: 'info',
    danger: 'error',
  }

  return typeMap[listClass] || 'default'
})

// 是否显示为普通文字（当 listClass 为 default 且没有强制指定 type 时）
const shouldShowAsText = computed(() => {
  // 如果强制指定了 type，则显示标签
  if (props.type) {
    return false
  }

  if (!currentDict.value) {
    return true
  }

  // 获取 listClass
  const listClass = currentDict.value.listClass || currentDict.value.raw?.listClass

  // 如果 listClass 为 default 或空，显示为普通文字
  return !listClass || listClass === 'default'
})

// 加载字典数据
async function loadDict() {
  if (props.options) {
    // 如果传入了 options，直接使用
    return
  }

  if (!props.dictType) {
    console.warn('DictTag: 未指定 options 或 dictType')
    return
  }

  dictList.value = await getDictData(props.dictType)
}

// 监听 dictType 变化
watch(() => props.dictType, () => {
  if (!props.options) {
    loadDict()
  }
}, { immediate: true })

// 关闭事件
function handleClose() {
  emit('close')
}
</script>
