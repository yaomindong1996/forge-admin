<!--
  搜索表单组件
  基于 AiForm 封装，专门用于列表页搜索场景
-->

<template>
  <div class="ai-search-box">
    <AiForm
      ref="formRef"
      v-model:value="formData"
      :schema="schema"
      :grid-cols="gridCols"
      :label-placement="labelPlacement"
      :label-width="labelWidth"
      :size="size"
      :enable-collapse="enableCollapse"
      :max-visible-fields="maxVisibleFields"
      :show-submit="false"
      :show-reset="false"
      :context="formContext"
      :show-feedback="false"
      :y-gap="yGap"
    >
      <!-- 透传所有插槽 -->
      <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
        <slot :name="slotName" v-bind="slotProps" />
      </template>

      <!-- 搜索操作按钮 -->
      <template #formAction="{ formData: data }">
        <n-space>
          <n-button
            type="primary"
            size="small"
            :loading="searchLoading"
            :disabled="searchLoading"
            @click="handleSearch"
          >
            <template #icon>
              <n-icon><SearchOutline /></n-icon>
            </template>
            {{ searchText }}
          </n-button>
          <n-button
            size="small"
            strong
            secondary
            :loading="resetLoading"
            :disabled="searchLoading || resetLoading"
            @click="handleReset"
          >
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            {{ resetText }}
          </n-button>
          <!-- 额外操作按钮插槽 -->
          <slot name="extra-actions" :form-data="data" />
        </n-space>
      </template>
    </AiForm>
  </div>
</template>

<script setup>
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import AiForm from './AiForm.vue'

const props = defineProps({
  // 表单配置（兼容 options 命名）
  schema: {
    type: Array,
    default: () => [],
  },
  options: {
    type: Array,
    default: () => [],
  },
  // 初始值
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  // 栅格列数
  gridCols: {
    type: Number,
    default: 4,
  },
  // 标签位置
  labelPlacement: {
    type: String,
    default: 'left',
  },
  // 标签宽度
  labelWidth: {
    type: [String, Number],
    default: 'auto',
  },
  // 尺寸
  size: {
    type: String,
    default: 'medium',
  },
  // 是否启用折叠
  enableCollapse: {
    type: Boolean,
    default: true,
  },
  // 最大显示字段数
  maxVisibleFields: {
    type: Number,
    default: 3,
  },
  // 搜索按钮文本
  searchText: {
    type: String,
    default: '搜索',
  },
  // 重置按钮文本
  resetText: {
    type: String,
    default: '重置',
  },
  // 上下文对象
  context: {
    type: Object,
    default: () => ({}),
  },
  // 重置前的钩子函数
  beforeReset: {
    type: Function,
    default: null,
  },
  // 表单项间距
  yGap: {
    type: Number,
    default: 16,
  },
})

const emit = defineEmits(['search', 'reset', 'update:modelValue'])

const formRef = ref(null)
const formData = ref({ ...props.modelValue })
const searchLoading = ref(false)
const resetLoading = ref(false)

// 外部 setSearchParams / filterBlock 会改 v-model，需同步到内部表单，否则重置清不掉
watch(
  () => props.modelValue,
  (value) => {
    const next = { ...(value || {}) }
    const current = formData.value || {}
    const nextKeys = Object.keys(next)
    const currKeys = Object.keys(current)
    if (nextKeys.length === currKeys.length && nextKeys.every(key => next[key] === current[key]))
      return
    formData.value = next
  },
  { deep: true },
)

// 兼容 options 和 schema 两种命名
const schema = computed(() => props.schema.length > 0 ? props.schema : props.options)
const formContext = computed(() => ({
  ...props.context,
  isSearch: true,
}))

/** 只保留查询 schema 内字段及树筛选附属键，避免交互 setValue 污染搜索参数 */
function pickSearchSchemaData(data = {}) {
  const allowed = new Set()
  for (const field of schema.value || []) {
    const name = String(field?.field || field?.prop || '').trim()
    if (!name)
      continue
    allowed.add(name)
    allowed.add(`${name}_includeChildren`)
    allowed.add(`${name}__treeExpanded`)
  }
  const next = {}
  Object.entries(data || {}).forEach(([key, value]) => {
    if (allowed.has(key) || key === '_searchTypes')
      next[key] = value
  })
  return next
}

/**
 * 与左树筛选一致：补齐 includeChildren；展开值存在 __treeExpanded，提交时由列表请求改写。
 */
function withSearchTreeIncludeChildren(data = {}) {
  const next = pickSearchSchemaData(data)
  for (const field of schema.value || []) {
    const type = String(field?.type || field?.componentType || '').trim()
    if (!['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(type))
      continue
    const name = String(field?.field || '').trim()
    if (!name)
      continue
    const allow = field.includeChildren !== false && field.props?.includeChildren !== false
    const flagKey = `${name}_includeChildren`
    const expandedKey = `${name}__treeExpanded`
    const value = next[name]
    const filled = Array.isArray(value)
      ? value.length > 0
      : value !== null && value !== undefined && String(value).trim() !== ''
    if (!allow || !filled) {
      delete next[flagKey]
      delete next[expandedKey]
      continue
    }
    next[flagKey] = true
  }
  return next
}

/**
 * 搜索
 */
async function handleSearch() {
  // 防止重复点击
  if (searchLoading.value || resetLoading.value) {
    return
  }

  try {
    searchLoading.value = true
    await formRef.value?.validate()
    const payload = withSearchTreeIncludeChildren(formData.value)
    formData.value = payload
    emit('search', { ...payload })
    emit('update:modelValue', { ...payload })
  }
  catch (error) {
    console.warn('表单验证失败:', error)
  }
  finally {
    // 延迟 300ms 再关闭 loading,防止连续点击
    setTimeout(() => {
      searchLoading.value = false
    }, 300)
  }
}

/**
 * 重置
 */
async function handleReset() {
  // 防止重复点击
  if (searchLoading.value || resetLoading.value) {
    return
  }

  try {
    resetLoading.value = true

    // 执行重置前的钩子
    if (props.beforeReset && typeof props.beforeReset === 'function') {
      const result = props.beforeReset(formData.value)

      // 如果是 Promise，等待执行完成
      if (result instanceof Promise) {
        await result
      }
    }

    // 重置表单
    formRef.value?.reset()
    formData.value = {}

    emit('reset')
    emit('update:modelValue', {})

    // 重置后自动搜索
    await handleSearch()
  }
  catch (error) {
    console.error('重置失败:', error)
  }
  finally {
    setTimeout(() => {
      resetLoading.value = false
    }, 300)
  }
}

/**
 * 更新单个字段值
 */
function updateField(name, value) {
  formData.value[name] = value
}

/**
 * 获取表单数据
 */
function getFormData() {
  return pickSearchSchemaData(formData.value)
}

/**
 * 设置表单数据
 */
function setFormData(data) {
  formData.value = pickSearchSchemaData(data)
}

// 暴露方法
defineExpose({
  handleSearch,
  handleReset,
  updateField,
  getFormData,
  setFormData,
  validate: () => formRef.value?.validate(),
  reset: () => formRef.value?.reset(),
})
</script>

<style scoped>
.ai-search-box {
  padding: 8px 12px 0;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
}
</style>
