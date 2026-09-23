<template>
  <view v-if="control.visible" class="lowcode-field">
    <view class="lowcode-field__label">
      <text>{{ field.label }}</text>
      <text v-if="control.required" class="lowcode-field__required">*</text>
    </view>
    <view class="lowcode-field__control">
      <LowcodeUnsupported
        v-if="descriptor.capability === MOBILE_COMPONENT_CAPABILITY.BLOCKED"
        :component-type="descriptor.sourceType"
        :reason="descriptor.reason"
        :value="modelValue"
      />
      <view v-else-if="readonly || descriptor.capability === MOBILE_COMPONENT_CAPABILITY.READONLY" class="lowcode-field__readonly">
        {{ displayValue }}
      </view>
      <LowcodeArrayField
        v-else-if="descriptor.renderer === 'array'"
        ref="arrayFieldRef"
        :model-value="arrayValue"
        :field="field"
        :readonly="readonly"
        :disabled="disabled"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <view v-else-if="isNumberRangeField" class="lowcode-field__range">
        <AiField
          class="lowcode-field__range-input"
          type="number"
          :model-value="rangeValue[0]"
          :disabled="disabled"
          :placeholder="field.props?.startPlaceholder || '开始值'"
          @update:model-value="updateRangeValue(0, $event)"
          @blur="emit('blur')"
        />
        <text class="lowcode-field__range-separator">至</text>
        <AiField
          class="lowcode-field__range-input"
          type="number"
          :model-value="rangeValue[1]"
          :disabled="disabled"
          :placeholder="field.props?.endPlaceholder || '结束值'"
          @update:model-value="updateRangeValue(1, $event)"
          @blur="emit('blur')"
        />
      </view>
      <view v-else-if="descriptor.renderer === 'barcode-scanner'" class="lowcode-field__barcode">
        <AiField
          :model-value="modelValue"
          :placeholder="field.props?.placeholder || '请输入或扫描条码'"
          :maxlength="field.props?.maxlength || 2048"
          :clearable="field.props?.allowManualInput !== false"
          @update:model-value="updateValue"
          @blur="emit('blur')"
          @confirm="completeManualScan"
        />
        <AiButton
          class="lowcode-field__scan"
          size="sm"
          :loading="scanning"
          :disabled="scanning"
          @click="scan"
        >
          {{ scanning ? '扫描中' : (field.props?.buttonText || '扫码') }}
        </AiButton>
      </view>
      <AiTextarea
        v-else-if="descriptor.renderer === 'textarea'"
        :model-value="modelValue"
        :maxlength="field.props?.maxlength || 2048"
        :disabled="disabled"
        :placeholder="field.props?.placeholder || `请输入${field.label}`"
        :show-word-limit="field.props?.showWordLimit !== false"
        @update:model-value="updateValue"
        @blur="emit('blur')"
      />
      <AiField
        v-else-if="descriptor.renderer === 'text'"
        :model-value="modelValue"
        type="text"
        :placeholder="field.props?.placeholder || `请输入${field.label}`"
        :maxlength="field.props?.maxlength || 2048"
        :clearable="field.props?.clearable !== false"
        :disabled="disabled"
        @update:model-value="updateValue"
        @blur="emit('blur')"
      />
      <AiField
        v-else-if="descriptor.renderer === 'number' || descriptor.renderer === 'money'"
        type="number"
        :model-value="modelValue"
        :disabled="disabled"
        :placeholder="field.props?.placeholder || `请输入${field.label}`"
        @update:model-value="updateValue"
        @blur="emit('blur')"
      />
      <LowcodeEntitySelector
        v-else-if="isRemoteSelectorField"
        :field="field"
        :model-value="modelValue"
        :options="options"
        :form-data="formData"
        :context="context"
        :disabled="disabled"
        :clearable="field.props?.clearable !== false"
        :placeholder="field.props?.placeholder || `请选择${field.label}`"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
        @selection="emit('selection', $event)"
      />
      <AiSelect
        v-else-if="descriptor.renderer === 'picker' && options.length"
        :model-value="modelValue"
        :options="options"
        :placeholder="field.props?.placeholder || `请选择${field.label}`"
        :title="field.label"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <AiRadioGroup
        v-else-if="descriptor.renderer === 'radio' || descriptor.renderer === 'radio-button'"
        :model-value="modelValue"
        :options="options"
        :button="descriptor.renderer === 'radio-button'"
        :inline="field.props?.inline !== false"
        :disabled="disabled"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <AiCheckboxGroup
        v-else-if="descriptor.renderer === 'checkbox' || descriptor.renderer === 'transfer'"
        :model-value="modelValue"
        :options="options"
        :button="field.props?.displayMode === 'button'"
        :inline="field.props?.inline !== false"
        :min="Number(field.props?.min || 0)"
        :max="Number(field.props?.max || 0)"
        :disabled="disabled"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <wd-switch
        v-else-if="descriptor.renderer === 'switch'"
        :model-value="switchValue"
        :disabled="disabled"
        active-color="var(--forge-color-primary, #4266f7)"
        @update:model-value="updateValue"
        @change="emit('change', $event?.value ?? $event)"
      />
      <wd-slider
        v-else-if="descriptor.renderer === 'slider'"
        :model-value="numericValue"
        :min="Number(field.min ?? field.props?.min ?? 0)"
        :max="Number(field.max ?? field.props?.max ?? 100)"
        :step="Number(field.step ?? field.props?.step ?? 1)"
        :disabled="disabled"
        show-value
        @update:model-value="updateValue"
        @change="emit('change', $event?.value ?? $event)"
      />
      <wd-rate
        v-else-if="descriptor.renderer === 'rate'"
        :model-value="numericValue"
        :count="Number(field.props?.count || 5)"
        :disabled="disabled"
        :readonly="readonly"
        :allow-half="field.props?.allowHalf !== false"
        active-color="#ff7d00"
        @update:model-value="updateValue"
        @change="emit('change', $event?.value ?? $event)"
      />
      <AiDateTimePicker
        v-else-if="isDateTimeField"
        :model-value="modelValue"
        :type="fieldType"
        :title="field.label"
        :placeholder="field.props?.placeholder || `请选择${field.label}`"
        :min="field.min ?? field.props?.min ?? field.props?.minDate"
        :max="field.max ?? field.props?.max ?? field.props?.maxDate"
        :disabled="disabled"
        :clearable="field.props?.clearable !== false"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
        @blur="emit('blur')"
      />
      <view v-else-if="descriptor.renderer === 'color'" class="lowcode-field__color">
        <view class="lowcode-field__color-preview" :style="{ backgroundColor: validColorValue }" />
        <AiField
          :model-value="modelValue"
          placeholder="#4266f7"
          :maxlength="32"
          @update:model-value="updateValue"
          @blur="emit('blur')"
        />
      </view>
      <AiFileUpload
        v-else-if="descriptor.renderer === 'file-upload'"
        :model-value="modelValue"
        :business-type="field.businessType || field.props?.businessType || 'lowcode_attachment'"
        :max-count="Number(field.limit || field.props?.limit || field.props?.maxCount || 9)"
        :readonly="readonly"
        @update:model-value="updateValue"
      />
      <AiImageUpload
        v-else-if="descriptor.renderer === 'image-upload'"
        :model-value="modelValue"
        :business-type="field.businessType || field.props?.businessType || 'lowcode_image'"
        :crop="field.props?.crop === true"
        :readonly="readonly || disabled"
        @update:model-value="updateValue"
      />
      <AiSignaturePad
        v-else-if="descriptor.renderer === 'signature'"
        :model-value="String(modelValue || '')"
        :disabled="readonly || disabled"
        @update:model-value="updateValue"
      />
      <LowcodeUnsupported
        v-else-if="isOptionBackedField"
        :component-type="descriptor.sourceType"
        reason="该选择组件没有可用选项，已阻止输入任意值。"
        :value="modelValue"
      />
      <LowcodeUnsupported
        v-else
        :component-type="descriptor.sourceType"
        :reason="descriptor.reason || '该组件已登记，但对应移动端渲染器尚未完成。'"
        :value="modelValue"
        :blocked="false"
      />
    </view>
    <text v-if="error" class="lowcode-field__error">{{ error }}</text>
    <text v-if="scanMessage || hint" class="lowcode-field__hint" :class="{ 'is-error': scanMessage }">
      {{ scanMessage || hint }}
    </text>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import AiButton from '@/components/AiButton.vue'
import AiCheckboxGroup from '@/components/AiCheckboxGroup.vue'
import AiDateTimePicker from '@/components/AiDateTimePicker.vue'
import AiField from '@/components/AiField.vue'
import AiFileUpload from '@/components/AiFileUpload.vue'
import AiImageUpload from '@/components/AiImageUpload.vue'
import AiRadioGroup from '@/components/AiRadioGroup.vue'
import AiSelect from '@/components/AiSelect.vue'
import AiSignaturePad from '@/components/AiSignaturePad.vue'
import AiTextarea from '@/components/AiTextarea.vue'
import LowcodeArrayField from './LowcodeArrayField.vue'
import LowcodeEntitySelector from './LowcodeEntitySelector.vue'
import LowcodeUnsupported from './LowcodeUnsupported.vue'
import { MOBILE_COMPONENT_CAPABILITY, resolveMobileComponent } from './mobile-component-registry'
import { scanBarcode } from '@/utils/barcode-scanner'
import { resolveMobileSelectionLabels } from '@/utils/mobile-selector-runtime'

const props = defineProps({
  field: { type: Object, default: () => ({}) },
  modelValue: { type: [String, Number, Boolean, Array, Object], default: '' },
  options: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  formData: { type: Object, default: () => ({}) },
  context: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue', 'blur', 'change', 'scan', 'selection'])
const scanning = ref(false)
const scanMessage = ref('')
const arrayFieldRef = ref(null)
const platform = resolveRuntimePlatform()
const descriptor = computed(() => resolveMobileComponent(props.field.componentKey || props.field.type, { platform }))
const fieldType = computed(() => descriptor.value.type)
const isNumberRangeField = computed(() => descriptor.value.renderer === 'number-range')
const isDateTimeField = computed(() => ['datetime-picker', 'datetime-range'].includes(descriptor.value.renderer))
const isSingleSelectField = computed(() => ['picker', 'remote-select', 'entity-select', 'tree-select', 'cascader', 'record-selector'].includes(descriptor.value.renderer))
const isRemoteSelectorField = computed(() => ['remote-select', 'entity-select', 'tree-select', 'cascader', 'record-selector'].includes(descriptor.value.renderer))
const isOptionBackedField = computed(() => isSingleSelectField.value || ['radio', 'radio-button', 'checkbox', 'transfer'].includes(descriptor.value.renderer))
const arrayValue = computed(() => Array.isArray(props.modelValue) ? props.modelValue : [])
const numericValue = computed(() => {
  const number = Number(props.modelValue)
  return Number.isFinite(number) ? number : 0
})
const switchValue = computed(() => props.modelValue === true || props.modelValue === 1 || props.modelValue === '1' || props.modelValue === 'true')
const validColorValue = computed(() => /^#[0-9a-f]{3,8}$/i.test(String(props.modelValue || '')) ? String(props.modelValue) : '#c9cdd4')
const rangeValue = computed(() => {
  const value = props.modelValue
  if (Array.isArray(value)) return [value[0] ?? '', value[1] ?? '']
  if (value === undefined || value === null || value === '') return ['', '']
  // Legacy deployments stored the first endpoint as a scalar. Preserve it
  // while exposing the new two-endpoint shape to the submit payload.
  return [value, '']
})
const control = computed(() => props.field.__runtimeControl || { visible: true, required: props.field.required === true })
const readonly = computed(() => props.readonly || props.disabled || props.field.readonly === true || control.value.readonly)
const displayValue = computed(() => {
  const value = props.modelValue
  if (isNumberRangeField.value || descriptor.value.renderer === 'datetime-range') {
    const [start, end] = rangeValue.value
    return start || end ? `${start || '-'} 至 ${end || '-'}` : '-'
  }
  if (isOptionBackedField.value) {
    if (isRemoteSelectorField.value) {
      const labels = resolveMobileSelectionLabels(value, props.options, props.field, props.formData)
      if (labels.length) return labels.join('、')
    }
    const values = Array.isArray(value)
      ? value
      : String(value ?? '').split(',').map(item => item.trim()).filter(Boolean)
    if (!values.length)
      return '-'
    return values
      .map(item => props.options.find(option => String(option.value) === String(item))?.label || item)
      .join('、')
  }
  if (descriptor.value.renderer === 'switch') return switchValue.value ? '是' : '否'
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(value)) return value.length ? value.join('、') : '-'
  if (typeof value === 'object') {
    try { return JSON.stringify(value) }
    catch { return '[复杂数据]' }
  }
  return String(value)
})

function updateValue(value) {
  emit('update:modelValue', value)
}

function updateRangeValue(index, value) {
  const next = [...rangeValue.value]
  next[index] = value
  emit('update:modelValue', next)
}

async function scan() {
  if (scanning.value) return
  scanning.value = true
  scanMessage.value = ''
  try {
    const result = await scanBarcode({ timeoutMs: props.field.props?.timeoutMs })
    updateValue(result.value)
    emit('scan', result)
    emit('blur')
  }
  catch (error) {
    scanMessage.value = resolveScanMessage(error)
  }
  finally {
    scanning.value = false
  }
}

function completeManualScan() {
  const value = String(props.modelValue ?? '').trim()
  if (!value)
    return
  scanMessage.value = ''
  emit('scan', { value, type: 'MANUAL', platform: 'MANUAL' })
}

function resolveScanMessage(error) {
  switch (error?.code) {
    case 'SCAN_CANCELLED':
      return '已取消扫码'
    case 'SCAN_PERMISSION_DENIED':
      return '请允许浏览器使用摄像头，或手工输入条码后按确认键'
    case 'SCAN_UNSUPPORTED':
      return '当前环境不支持摄像头扫码，请手工输入条码后按确认键'
    case 'SCAN_TIMEOUT':
      return '扫码超时，请重试或手工输入条码'
    default:
      return '扫码失败，请重试或手工输入条码'
  }
}

function validate() {
  return arrayFieldRef.value?.validate?.() ?? true
}

function resolveRuntimePlatform() {
  let value = 'mini-program'
  // #ifdef H5
  value = 'h5'
  // #endif
  return value
}

defineExpose({ validate })
</script>

<style lang="scss" scoped>
.lowcode-field { margin-bottom: 32rpx; }
.lowcode-field__label { display: flex; margin-bottom: 12rpx; color: #4e5969; font-size: 28rpx; font-weight: 400; line-height: 1.5; }
.lowcode-field__required { margin-left: 6rpx; color: #f53f3f; }
.lowcode-field__control { min-height: 88rpx; }
.lowcode-field__readonly { min-height: 88rpx; padding: 20rpx 24rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-control); color: #4e5969; background: #f7f8fa; box-sizing: border-box; line-height: 1.5; word-break: break-all; }
.lowcode-field__barcode { display: flex; align-items: center; gap: 12rpx; }
.lowcode-field__barcode :deep(.ai-field) { flex: 1; min-width: 0; }
.lowcode-field__scan { flex: 0 0 auto; }
.lowcode-field__range { display: flex; align-items: center; gap: 10rpx; }
.lowcode-field__range-input { min-width: 0; flex: 1; }
.lowcode-field__range-separator { flex: 0 0 auto; color: #86909c; font-size: 24rpx; }
.lowcode-field__color { display: flex; align-items: center; gap: 12rpx; }
.lowcode-field__color-preview { width: 88rpx; height: 88rpx; flex: 0 0 auto; border: 1rpx solid var(--forge-color-border, #c9cdd4); border-radius: var(--radius-control); }
.lowcode-field__color :deep(.ai-field) { min-width: 0; flex: 1; }
.lowcode-field__error { display: block; margin-top: 8rpx; color: #f53f3f; font-size: 24rpx; }
.lowcode-field__hint { display: block; margin-top: 8rpx; color: #86909c; font-size: 22rpx; }
.lowcode-field__hint.is-error { color: #f53f3f; }
</style>
