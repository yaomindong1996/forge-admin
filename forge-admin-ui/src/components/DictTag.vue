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
    class="dict-tag"
    :class="`dict-tag--${tagType}`"
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
  <span v-else>{{ resolvedValue }}</span>
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

  // 兼容历史页面误传的 dictValue，推荐新代码统一使用 value
  dictValue: {
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

  // 即使字典 listClass 为 default，也强制显示为标签
  forceTag: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close'])

const dictList = ref([])

const resolvedValue = computed(() => {
  if (props.value !== null && props.value !== undefined && props.value !== '')
    return props.value
  return props.dictValue
})

// 当前字典项
const currentDict = computed(() => {
  const list = props.options || dictList.value
  if (!list || list.length === 0)
    return null

  return list.find(item => String(item.value) === String(resolvedValue.value))
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
  if (props.type || props.forceTag) {
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

<style>
.dict-tag.n-tag {
  --n-border-radius: 3px !important;
  --n-font-weight-strong: 500 !important;
  height: 22px;
  font-weight: 500;
  letter-spacing: 0;
}

.dict-tag.dict-tag--default {
  --n-color: #f7f9fc !important;
  --n-border: 1px solid #dbe3ef !important;
  --n-text-color: #5f6f86 !important;
}

.dict-tag.dict-tag--success {
  --n-color: rgba(30, 174, 117, 0.11) !important;
  --n-border: 1px solid rgba(30, 174, 117, 0.24) !important;
  --n-text-color: #16895a !important;
  --n-close-icon-color: #16895a !important;
  --n-close-icon-color-hover: #0f6f49 !important;
  --n-close-color-hover: rgba(30, 174, 117, 0.13) !important;
}

.dict-tag.dict-tag--info {
  --n-color: rgba(66, 102, 247, 0.09) !important;
  --n-border: 1px solid rgba(66, 102, 247, 0.22) !important;
  --n-text-color: #4266d6 !important;
  --n-close-icon-color: #4266d6 !important;
  --n-close-icon-color-hover: #2944b8 !important;
  --n-close-color-hover: rgba(66, 102, 247, 0.12) !important;
}

.dict-tag.dict-tag--warning {
  --n-color: rgba(245, 158, 11, 0.12) !important;
  --n-border: 1px solid rgba(245, 158, 11, 0.26) !important;
  --n-text-color: #a76508 !important;
  --n-close-icon-color: #a76508 !important;
  --n-close-icon-color-hover: #815006 !important;
  --n-close-color-hover: rgba(245, 158, 11, 0.14) !important;
}

.dict-tag.dict-tag--error {
  --n-color: rgba(239, 82, 82, 0.09) !important;
  --n-border: 1px solid rgba(239, 82, 82, 0.22) !important;
  --n-text-color: #c54747 !important;
  --n-close-icon-color: #c54747 !important;
  --n-close-icon-color-hover: #9f3434 !important;
  --n-close-color-hover: rgba(239, 82, 82, 0.12) !important;
}

.dark .dict-tag.dict-tag--default {
  --n-color: rgba(255, 255, 255, 0.06) !important;
  --n-border: 1px solid rgba(255, 255, 255, 0.14) !important;
  --n-text-color: #cbd5e1 !important;
}

.dark .dict-tag.dict-tag--success {
  --n-color: rgba(52, 211, 153, 0.12) !important;
  --n-border: 1px solid rgba(52, 211, 153, 0.22) !important;
  --n-text-color: #8ddbb8 !important;
}

.dark .dict-tag.dict-tag--info {
  --n-color: rgba(96, 165, 250, 0.12) !important;
  --n-border: 1px solid rgba(96, 165, 250, 0.22) !important;
  --n-text-color: #a9c8f7 !important;
}

.dark .dict-tag.dict-tag--warning {
  --n-color: rgba(245, 158, 11, 0.12) !important;
  --n-border: 1px solid rgba(245, 158, 11, 0.24) !important;
  --n-text-color: #e7c178 !important;
}

.dark .dict-tag.dict-tag--error {
  --n-color: rgba(248, 113, 113, 0.12) !important;
  --n-border: 1px solid rgba(248, 113, 113, 0.22) !important;
  --n-text-color: #eba0a0 !important;
}
</style>
