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
      <n-button
        v-if="clearable && hasValue"
        :disabled="disabled"
        :size="size"
        title="清空"
        @click.stop="handleClear"
      >
        <template #icon>
          <i class="i-material-symbols:close-rounded" />
        </template>
      </n-button>
      <n-button :disabled="disabled" :size="size" title="选择用户" @click="openModal">
        <template #icon>
          <i class="i-material-symbols:person-search-rounded" />
        </template>
      </n-button>
    </n-input-group>

    <UserSelectModal
      v-model:show="modalVisible"
      :title="title"
      :multiple="multiple"
      :selected-users="selectedUsers"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import UserSelectModal from '@/components/bpmn/UserSelectModal.vue'

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
})

const emit = defineEmits(['update:modelValue', 'update:labelValue', 'select', 'clear'])

const modalVisible = ref(false)

const valueList = computed(() => normalizeValueList(props.modelValue, props.multiple))
const labelList = computed(() => normalizeLabelList(props.labelValue))
const hasValue = computed(() => valueList.value.length > 0)
const displayText = computed(() => {
  const labels = labelList.value.filter(Boolean)
  if (labels.length)
    return labels.join('、')
  return valueList.value.map(value => String(value)).join('、')
})

const selectedUsers = computed(() => {
  return valueList.value.map((id, index) => {
    const label = labelList.value[index] || ''
    return {
      id,
      realName: label,
      name: label,
      username: label || String(id),
    }
  })
})

function openModal() {
  if (props.disabled)
    return
  modalVisible.value = true
}

function handleClear() {
  emit('update:modelValue', props.multiple ? [] : null)
  emit('update:labelValue', props.multiple ? [] : '')
  emit('clear')
}

function handleConfirm(value) {
  const users = props.multiple
    ? Array.isArray(value) ? value : value ? [value] : []
    : value ? [value] : []
  const ids = users.map(user => user?.id).filter(isFilledValue)
  const labels = users.map(resolveUserLabel).filter(Boolean)

  if (props.multiple) {
    emit('update:modelValue', ids)
    emit('update:labelValue', labels)
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
  width: 100%;
}

.user-select-picker__input :deep(input) {
  cursor: pointer;
}
</style>
