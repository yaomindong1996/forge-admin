<template>
  <div class="user-select-picker">
    <n-input-group class="user-select-picker__group">
      <n-input
        class="user-select-picker__input"
        :value="displayText"
        :placeholder="placeholder"
        :disabled="disabled"
        :size="size"
        readonly
        @click="openModal"
      />
      <!--
        不能用 n-input 的 clearable：Naive 在 readonly 时不渲染清空按钮。
        人员选择输入框必须 readonly（弹窗选人），清空只能靠独立按钮。
        图标用内联 SVG，避免 UnoCSS 图标类在部分场景不生成导致按钮空白。
      -->
      <n-button
        v-if="clearable && hasValue && !disabled"
        class="user-select-picker__button user-select-picker__clear"
        :size="size"
        title="清空"
        aria-label="清空"
        @click.stop="handleClear"
      >
        <template #icon>
          <!-- material-symbols:close-rounded -->
          <svg class="user-select-picker__svg" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="m12 13.4l-4.9 4.9q-.275.275-.7.275t-.7-.275t-.275-.7t.275-.7l4.9-4.9l-4.9-4.9q-.275-.275-.275-.7t.275-.7t.7-.275t.7.275l4.9 4.9l4.9-4.9q.275-.275.7-.275t.7.275t.275.7t-.275.7L13.4 12l4.9 4.9q.275.275.275.7t-.275.7t-.7.275t-.7-.275z" />
          </svg>
        </template>
      </n-button>
      <n-button
        class="user-select-picker__button user-select-picker__pick"
        :disabled="disabled"
        :size="size"
        title="选择用户"
        aria-label="选择用户"
        @click="openModal"
      >
        <template #icon>
          <!-- material-symbols:person-search-rounded（与历史版本同一图标） -->
          <svg class="user-select-picker__svg" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M11 12q-1.65 0-2.825-1.175T7 8t1.175-2.825T11 4t2.825 1.175T15 8t-1.175 2.825T11 12m10.4 10.8l-2.5-2.5q-.525.3-1.125.5T16.5 21q-1.875 0-3.187-1.312T12 16.5t1.313-3.187T16.5 12t3.188 1.313T21 16.5q0 .675-.2 1.275t-.5 1.125l2.5 2.5q.275.275.275.7t-.275.7t-.7.275t-.7-.275m-3.125-4.525Q19 17.55 19 16.5t-.725-1.775T16.5 14t-1.775.725T14 16.5t.725 1.775T16.5 19t1.775-.725M5 20q-.825 0-1.412-.587T3 18v-.775q0-.85.425-1.575t1.175-1.1q1.125-.575 2.488-.987t2.987-.538q.3-.025.45.25t.025.575q-.275.625-.413 1.288T10 16.475q0 .65.113 1.313t.387 1.262q.15.35-.025.65t-.525.3z" />
          </svg>
        </template>
      </n-button>
    </n-input-group>

    <UserSelectModal
      v-model:show="modalVisible"
      :title="title"
      :multiple="multiple"
      :selected-users="selectedUsers"
      :org-id="orgId"
      :include-children="includeChildren"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { request } from '@/utils/request'
import UserSelectModal from '@/components/common/UserSelectModal.vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Array],
    default: null,
  },
  labelValue: {
    type: [String, Number, Array],
    default: '',
  },
  placeholder: {
    type: String,
    default: '请选择用户',
  },
  title: {
    type: String,
    default: '选择用户',
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  clearable: {
    type: Boolean,
    default: true,
  },
  size: {
    type: String,
    default: undefined,
  },
  // 级联锁定组织：传入后人员选择范围限定在该组织内（可进一步选子组织）
  orgId: {
    type: [String, Number],
    default: null,
  },
  // 是否包含子组织人员：false 时仅查询直属该组织的用户
  includeChildren: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue', 'update:labelValue', 'select', 'clear'])

const modalVisible = ref(false)
const resolvedLabelMap = ref({})

const valueList = computed(() => normalizeValueList(props.modelValue, props.multiple))
const labelList = computed(() => {
  const fromProp = normalizeLabelList(props.labelValue)
  if (fromProp.some(Boolean))
    return fromProp
  return valueList.value.map(id => resolvedLabelMap.value[String(id)] || '')
})
const hasValue = computed(() => valueList.value.length > 0)
const displayText = computed(() => {
  const labels = labelList.value.filter(Boolean)
  if (labels.length)
    return labels.join('、')
  // 远程名尚未回来时先不闪数字 ID
  if (valueList.value.length && Object.keys(resolvedLabelMap.value).length === 0)
    return ''
  return valueList.value.map(value => String(value)).join('、')
})

const selectedUsers = computed(() => {
  return valueList.value.map((id, index) => {
    const label = labelList.value[index] || resolvedLabelMap.value[String(id)] || ''
    return {
      id,
      realName: label,
      name: label,
      username: label || String(id),
    }
  })
})

watch(
  () => [props.modelValue, props.labelValue, props.multiple],
  () => {
    void resolveMissingLabels()
  },
  { immediate: true },
)

async function resolveMissingLabels() {
  const ids = valueList.value.map(id => String(id).trim()).filter(Boolean)
  if (!ids.length)
    return
  const existingLabels = normalizeLabelList(props.labelValue)
  const missing = ids.filter((id, index) => {
    if (existingLabels[index])
      return false
    return !resolvedLabelMap.value[id]
  })
  if (!missing.length)
    return
  try {
    const results = await Promise.all(missing.map(async (id) => {
      try {
        const res = await request.post('/system/user/getById', null, { params: { id } })
        const user = res?.data || res || {}
        return [id, resolveUserLabel(user)]
      }
      catch {
        return [id, '']
      }
    }))
    const next = { ...resolvedLabelMap.value }
    let patchedLabel = ''
    results.forEach(([id, label]) => {
      if (label)
        next[id] = label
    })
    resolvedLabelMap.value = next
    const labels = ids.map((id, index) => existingLabels[index] || next[id] || '').filter(Boolean)
    if (labels.length) {
      patchedLabel = props.multiple ? labels.join(',') : labels[0]
      if (patchedLabel && !isFilledValue(props.labelValue))
        emit('update:labelValue', patchedLabel)
    }
  }
  catch {
    // ignore lookup failures; keep showing whatever we have
  }
}

function openModal() {
  if (props.disabled)
    return
  modalVisible.value = true
}

function handleClear() {
  resolvedLabelMap.value = {}
  emit('update:modelValue', props.multiple ? '' : null)
  emit('update:labelValue', '')
  emit('clear')
}

function handleConfirm(value) {
  const users = props.multiple
    ? Array.isArray(value) ? value : value ? [value] : []
    : value ? [value] : []
  const ids = users.map(user => user?.id).filter(isFilledValue)
  const labels = users.map(resolveUserLabel).filter(Boolean)
  const nextMap = { ...resolvedLabelMap.value }
  ids.forEach((id, index) => {
    if (labels[index])
      nextMap[String(id)] = labels[index]
  })
  resolvedLabelMap.value = nextMap

  if (props.multiple) {
    emit('update:modelValue', ids.map(id => String(id).trim()).join(','))
    emit('update:labelValue', labels.join(','))
    emit('select', users)
    return
  }

  emit('update:modelValue', ids[0] ?? null)
  emit('update:labelValue', labels[0] || '')
  emit('select', users[0] || null)
}

function normalizeValueList(value, multiple) {
  if (Array.isArray(value))
    return value.filter(isFilledValue)
  if (!isFilledValue(value))
    return []
  if (multiple && typeof value === 'string' && value.includes(','))
    return value.split(',').map(item => item.trim()).filter(isFilledValue)
  return [value]
}

function normalizeLabelList(value) {
  if (Array.isArray(value))
    return value.map(item => String(item || '').trim())
  if (!isFilledValue(value))
    return []
  const text = String(value).trim()
  return text.includes(',') ? text.split(',').map(item => item.trim()) : [text]
}

function resolveUserLabel(user) {
  return String(user?.realName || user?.name || user?.nickname || user?.username || '').trim()
}

function isFilledValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}
</script>

<style scoped>
.user-select-picker {
  width: 100%;
}

.user-select-picker__group {
  display: flex;
  align-items: stretch;
  width: 100%;
  min-width: 0;
}

.user-select-picker__input {
  flex: 1 1 auto;
  min-width: 0;
}

.user-select-picker__input :deep(input) {
  cursor: pointer;
}

.user-select-picker__button {
  flex: 0 0 auto;
  flex-shrink: 0;
  align-self: stretch;
  min-width: 40px;
  padding: 0 12px;
}

.user-select-picker__clear,
.user-select-picker__pick {
  color: var(--text-secondary);
}

.user-select-picker__svg {
  display: block;
  width: 20px;
  height: 20px;
}

.user-select-picker__button :deep(.n-button__icon) {
  font-size: 20px;
  margin: 0;
}

.user-select-picker__button :deep(.n-button__content),
.user-select-picker__button :deep(.n-button__icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
