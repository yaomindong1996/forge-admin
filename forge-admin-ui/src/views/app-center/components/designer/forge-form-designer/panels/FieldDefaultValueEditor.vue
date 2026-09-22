<template>
  <div v-if="editorKind !== 'none'" class="field-default-value-editor" :class="{ 'is-disabled': disabled }">
    <!-- 开关：按组件 checked/unchecked 值选择，禁止手输 -->
    <n-select
      v-if="editorKind === 'switch'"
      :value="normalizedSwitchValue"
      :options="switchOptions"
      :disabled="disabled"
      clearable
      placeholder="请选择默认开关状态"
      @update:value="emitSwitchValue"
    />

    <!-- 数字 -->
    <n-input-number
      v-else-if="editorKind === 'number'"
      :value="normalizedNumberValue"
      :show-button="false"
      :disabled="disabled"
      clearable
      class="full-input"
      placeholder="请输入数字默认值"
      @update:value="emitValue"
    />

    <!-- 选项类：字典 / 静态 options -->
    <n-select
      v-else-if="editorKind === 'option'"
      :value="optionValue"
      :options="optionSelectOptions"
      :multiple="optionMultiple"
      :loading="optionLoading"
      :disabled="disabled"
      filterable
      clearable
      placeholder="请选择默认值"
      @update:value="emitOptionValue"
    />

    <!-- 颜色 -->
    <n-color-picker
      v-else-if="editorKind === 'color'"
      :value="typeof modelValue === 'string' ? modelValue : null"
      :show-alpha="false"
      :modes="['hex']"
      :disabled="disabled"
      @update:value="emitValue"
    />

    <!-- 日期 / 日期时间：预设 + 固定值 -->
    <div v-else-if="editorKind === 'date' || editorKind === 'time'" class="date-default-editor">
      <n-select
        :value="dateMode"
        :options="dateModeOptions"
        :disabled="disabled"
        placeholder="默认值类型"
        @update:value="handleDateModeChange"
      />
      <n-date-picker
        v-if="dateMode === 'fixed' && editorKind === 'date'"
        :formatted-value="fixedDateFormatted"
        :type="datePickerType"
        :value-format="dateValueFormat"
        :disabled="disabled"
        clearable
        class="full-input"
        @update:formatted-value="emitValue"
      />
      <n-time-picker
        v-else-if="dateMode === 'fixed' && editorKind === 'time'"
        :formatted-value="typeof modelValue === 'string' ? modelValue : null"
        value-format="HH:mm:ss"
        :disabled="disabled"
        clearable
        class="full-input"
        @update:formatted-value="emitValue"
      />
    </div>

    <!-- 文本兜底 -->
    <n-input
      v-else
      :value="modelValue == null ? '' : String(modelValue)"
      :disabled="disabled"
      clearable
      placeholder="请输入"
      @update:value="emitValue"
    />
  </div>
  <span v-else class="field-default-value-skip">当前组件不支持静态默认值</span>
</template>

<script setup>
import { computed } from 'vue'
import {
  DATE_DEFAULT_PRESETS,
  isForgeDefaultPreset,
  resolveDefaultValueEditorKind,
  resolveSwitchValuePair,
} from '../field-default-value'

const props = defineProps({
  componentKey: { type: String, default: '' },
  component: { type: Object, default: null },
  fieldAsset: { type: Object, default: null },
  modelValue: { type: [String, Number, Boolean, Array, Object], default: undefined },
  optionSelectOptions: { type: Array, default: () => [] },
  optionMultiple: { type: Boolean, default: false },
  optionLoading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'update:props'])

const editorKind = computed(() => resolveDefaultValueEditorKind(props.componentKey, props.fieldAsset))

const switchPair = computed(() => resolveSwitchValuePair(props.component || {}, props.fieldAsset))

const switchOptions = computed(() => [
  { label: `开启（${formatSwitchLabel(switchPair.value.checkedValue)}）`, value: switchPair.value.checkedValue },
  { label: `关闭（${formatSwitchLabel(switchPair.value.uncheckedValue)}）`, value: switchPair.value.uncheckedValue },
])

const normalizedSwitchValue = computed(() => {
  const value = props.modelValue
  if (value === undefined || value === null || value === '')
    return null
  if (value === switchPair.value.checkedValue || String(value) === String(switchPair.value.checkedValue))
    return switchPair.value.checkedValue
  if (value === switchPair.value.uncheckedValue || String(value) === String(switchPair.value.uncheckedValue))
    return switchPair.value.uncheckedValue
  if (value === true || value === 'true' || value === 1 || value === '1')
    return switchPair.value.checkedValue
  if (value === false || value === 'false' || value === 0 || value === '0')
    return switchPair.value.uncheckedValue
  return value
})

const normalizedNumberValue = computed(() => {
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '')
    return null
  const numeric = Number(props.modelValue)
  return Number.isFinite(numeric) ? numeric : null
})

const optionValue = computed(() => {
  if (!props.optionMultiple)
    return props.modelValue ?? null
  if (Array.isArray(props.modelValue))
    return props.modelValue
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '')
    return []
  if (typeof props.modelValue === 'string')
    return props.modelValue.split(',').map(item => item.trim()).filter(Boolean)
  return [props.modelValue]
})

const datePickerType = computed(() => {
  const key = String(props.componentKey || '').toLowerCase()
  if (key === 'datetime')
    return 'datetime'
  if (key === 'month')
    return 'month'
  if (key === 'year')
    return 'year'
  return 'date'
})

const dateValueFormat = computed(() => {
  const key = String(props.componentKey || '').toLowerCase()
  if (key === 'datetime')
    return 'yyyy-MM-dd HH:mm:ss'
  if (key === 'month')
    return 'yyyy-MM'
  if (key === 'year')
    return 'yyyy'
  return 'yyyy-MM-dd'
})

const dateModeOptions = computed(() => [
  { label: '无默认值', value: 'none' },
  { label: '固定值', value: 'fixed' },
  ...DATE_DEFAULT_PRESETS,
])

const dateMode = computed(() => {
  const value = props.modelValue
  if (value === undefined || value === null || value === '')
    return 'none'
  if (isForgeDefaultPreset(value))
    return value
  return 'fixed'
})

const fixedDateFormatted = computed(() => {
  if (dateMode.value !== 'fixed')
    return null
  return typeof props.modelValue === 'string' ? props.modelValue : null
})

function formatSwitchLabel(value) {
  if (value === true)
    return 'true'
  if (value === false)
    return 'false'
  return String(value)
}

function emitValue(value) {
  if (value === null || value === undefined || value === '')
    emit('update:modelValue', undefined)
  else
    emit('update:modelValue', value)
}

function emitSwitchValue(value) {
  const pair = switchPair.value
  const propsPatch = {}
  if (props.component?.props?.checkedValue === undefined)
    propsPatch.checkedValue = pair.checkedValue
  if (props.component?.props?.uncheckedValue === undefined)
    propsPatch.uncheckedValue = pair.uncheckedValue
  if (Object.keys(propsPatch).length)
    emit('update:props', propsPatch)
  emitValue(value)
}

function emitOptionValue(value) {
  if (props.optionMultiple) {
    const list = Array.isArray(value) ? value : []
    emit('update:modelValue', list.length ? list : undefined)
    return
  }
  emitValue(value)
}

function handleDateModeChange(mode) {
  if (!mode || mode === 'none') {
    emitValue(undefined)
    return
  }
  if (mode === 'fixed') {
    if (isForgeDefaultPreset(props.modelValue) || props.modelValue == null || props.modelValue === '')
      emitValue('')
    return
  }
  emitValue(mode)
}
</script>

<style scoped>
.field-default-value-editor,
.date-default-editor {
  display: grid;
  gap: 6px;
  width: 100%;
}

.full-input {
  width: 100%;
}

.field-default-value-skip {
  color: var(--n-text-color-3);
  font-size: 12px;
}
</style>
