<!--
  字典标签组件
  用于显示字典值对应的标签

  使用示例：
  <DictTag :options="dict.case_status" :value="row.status" />
  <DictTag dict-type="case_status" :value="row.status" />
  <DictTag :options="dict.matter_type" :value="row.matterType" type="success" />
-->

<template>
  <span v-if="currentDictItems.length > 1 && shouldShowAsText" class="dict-tag-text">
    {{ currentDictItems.map(item => item.label).join('、') }}
  </span>
  <span v-else-if="currentDictItems.length > 1" class="dict-tag-list">
    <n-tag
      v-for="item in currentDictItems"
      :key="String(item.value)"
      class="dict-tag"
      :class="`dict-tag--${resolveTagType(item)}`"
      :type="resolveTagType(item)"
      :size="size"
      :round="round"
      :bordered="bordered"
    >
      {{ item.label }}
    </n-tag>
  </span>
  <!-- 如果是 default 类型且没有强制指定 type，显示普通文字 -->
  <span v-else-if="currentDict && shouldShowAsText">
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
  <span v-else-if="loading" aria-label="字典加载中">—</span>
  <!-- 没有找到字典项，显示原始值 -->
  <span v-else>{{ displayFallback }}</span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDictData } from '@/composables/useDict'
import { useDictStore } from '@/stores/system/dictStore'

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

  // 字典值，多选时支持逗号分隔或数组
  value: {
    type: [String, Number, Array],
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

const dictStore = useDictStore()
const dictList = computed(() => dictStore.dictCache.get(props.dictType) || [])
const loading = ref(false)

const resolvedValue = computed(() => {
  if (props.value !== null && props.value !== undefined && props.value !== '')
    return props.value
  return props.dictValue
})

const resolvedValueList = computed(() => {
  const raw = resolvedValue.value
  if (Array.isArray(raw))
    return raw.map(item => String(item ?? '').trim()).filter(Boolean)
  if (raw === null || raw === undefined || raw === '')
    return []
  return String(raw).split(/[、,，]/).map(item => item.trim()).filter(Boolean)
})

const displayFallback = computed(() => resolvedValueList.value.join('、') || resolvedValue.value)

/** 空数组不算“已提供 options”，否则字典异步加载前会一直回退成原始编码 */
const hasProvidedOptions = computed(() => Array.isArray(props.options) && props.options.length > 0)

const currentDictItems = computed(() => {
  const list = hasProvidedOptions.value ? props.options : dictList.value
  if (!list || list.length === 0)
    return []
  return resolvedValueList.value
    .map(value => list.find(item => String(item.value) === String(value)
      || String(item.dictValue ?? '') === String(value)))
    .filter(Boolean)
})

const currentDict = computed(() => currentDictItems.value[0] || null)

// 标签类型
const tagType = computed(() => {
  if (props.type) {
    return props.type
  }

  return resolveTagType(currentDict.value)
})

function resolveTagType(dictItem) {
  if (props.type)
    return props.type
  if (!dictItem)
    return 'default'

  // 根据 listClass 映射标签类型
  const listClass = dictItem.listClass || dictItem.raw?.listClass

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
}

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

// 切换类型或从 options 切回自加载时重新请求，旧请求不影响当前加载状态。
watch([() => props.dictType, hasProvidedOptions], async ([dictType, hasOptions], _, onCleanup) => {
  let active = true
  onCleanup(() => {
    active = false
  })
  loading.value = false

  if (hasOptions) {
    return
  }
  if (!dictType) {
    console.warn('DictTag: 未指定 options 或 dictType')
    return
  }

  loading.value = true
  try {
    await getDictData(dictType)
  }
  finally {
    if (active) {
      loading.value = false
    }
  }
}, { immediate: true })

// 关闭事件
function handleClose() {
  emit('close')
}
</script>

<style>
.dict-tag-list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

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
