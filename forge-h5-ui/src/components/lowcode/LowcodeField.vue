<template>
  <view v-if="control.visible" class="lowcode-field" :class="fieldClasses">
    <view v-if="normalizedField.showLabel !== false" class="lowcode-field__label">
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
      <view
        v-else-if="showsReadonlyText"
        class="lowcode-field__readonly"
        :class="readonlyClasses"
      >
        <text class="lowcode-field__readonly-value">{{ displayValue }}</text>
        <button v-if="fieldProps.copyable === true && displayValue !== '-'" class="lowcode-field__copy" @click="copyDisplayValue">复制</button>
      </view>
      <LowcodeArrayField
        v-else-if="descriptor.renderer === 'array'"
        ref="arrayFieldRef"
        :model-value="arrayValue"
        :field="normalizedField"
        :readonly="readonly"
        :disabled="effectiveDisabled"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <view v-else-if="isNumberRangeField" class="lowcode-field__range">
        <AiField
          class="lowcode-field__range-input"
          type="number"
          :model-value="rangeValue[0]"
          :disabled="effectiveDisabled"
          :placeholder="fieldProps.startPlaceholder || '开始值'"
          @update:model-value="updateRangeValue(0, $event)"
          @blur="emit('blur')"
        />
        <text class="lowcode-field__range-separator">至</text>
        <AiField
          class="lowcode-field__range-input"
          type="number"
          :model-value="rangeValue[1]"
          :disabled="effectiveDisabled"
          :placeholder="fieldProps.endPlaceholder || '结束值'"
          @update:model-value="updateRangeValue(1, $event)"
          @blur="emit('blur')"
        />
      </view>
      <view v-else-if="descriptor.renderer === 'barcode-scanner'" class="lowcode-field__barcode">
        <AiField
          :model-value="modelValue"
          :placeholder="fieldProps.placeholder || '请输入或扫描条码'"
          :maxlength="fieldProps.maxLength || 2048"
          :clearable="fieldProps.clearable !== false && fieldProps.allowManualInput !== false"
          :disabled="effectiveDisabled"
          :readonly="fieldProps.allowManualInput === false"
          @update:model-value="updateValue"
          @blur="emit('blur')"
          @confirm="completeManualScan"
        />
        <AiButton
          class="lowcode-field__scan"
          size="sm"
          :loading="scanning"
          :disabled="effectiveDisabled || scanning"
          @click="scan"
        >
          {{ scanning ? '扫描中' : (fieldProps.buttonText || '扫码') }}
        </AiButton>
      </view>
      <AiTextarea
        v-else-if="descriptor.renderer === 'textarea'"
        :model-value="modelValue"
        :maxlength="fieldProps.maxLength || 2048"
        :disabled="effectiveDisabled"
        :readonly="readonly"
        :placeholder="fieldProps.placeholder || `请输入${field.label}`"
        :show-word-limit="fieldProps.showCount === true"
        :auto-height="fieldProps.autosize === true"
        :min-height="textareaMinHeight"
        :clearable="fieldProps.clearable === true"
        @update:model-value="updateValue"
        @blur="emit('blur')"
      />
      <view v-else-if="descriptor.renderer === 'text' && fieldProps.pair === true" class="lowcode-field__range">
        <AiField
          class="lowcode-field__range-input"
          :model-value="pairValue[0]"
          :disabled="effectiveDisabled"
          :readonly="readonly"
          :placeholder="fieldProps.startPlaceholder || fieldProps.placeholder || '请输入开始值'"
          :maxlength="fieldProps.maxLength || 2048"
          @update:model-value="value => updatePairValue(0, value)"
          @blur="emit('blur')"
        />
        <text class="lowcode-field__range-separator">{{ fieldProps.separator || '—' }}</text>
        <AiField
          class="lowcode-field__range-input"
          :model-value="pairValue[1]"
          :disabled="effectiveDisabled"
          :readonly="readonly"
          :placeholder="fieldProps.endPlaceholder || fieldProps.placeholder || '请输入结束值'"
          :maxlength="fieldProps.maxLength || 2048"
          @update:model-value="value => updatePairValue(1, value)"
          @blur="emit('blur')"
        />
      </view>
      <AiField
        v-else-if="descriptor.renderer === 'text'"
        :model-value="modelValue"
        type="text"
        :placeholder="fieldProps.placeholder || `请输入${field.label}`"
        :maxlength="fieldProps.maxLength || 2048"
        :clearable="fieldProps.clearable !== false"
        :disabled="effectiveDisabled"
        :readonly="readonly"
        :autofocus="fieldProps.autofocus === true"
        :show-count="fieldProps.showCount === true"
        :prefix="fieldProps.prefix"
        :suffix="fieldProps.suffix"
        @update:model-value="updateValue"
        @blur="emit('blur')"
      />
      <AiField
        v-else-if="descriptor.renderer === 'number' || descriptor.renderer === 'money'"
        type="number"
        :model-value="modelValue"
        :disabled="effectiveDisabled"
        :readonly="readonly"
        :placeholder="fieldProps.placeholder || `请输入${field.label}`"
        :clearable="fieldProps.clearable === true"
        :prefix="descriptor.renderer === 'money' ? (fieldProps.currencySymbol || '¥') : fieldProps.prefix"
        :suffix="fieldProps.suffix"
        @update:model-value="updateNumericValue"
        @blur="completeNumericInput"
      />
      <LowcodeEntitySelector
        v-else-if="isRemoteSelectorField"
        :field="normalizedField"
        :model-value="modelValue"
        :options="options"
        :form-data="formData"
        :context="context"
        :disabled="effectiveDisabled"
        :clearable="fieldProps.clearable !== false"
        :placeholder="fieldProps.placeholder || `请选择${field.label}`"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
        @selection="emit('selection', $event)"
      />
      <AiSelect
        v-else-if="descriptor.renderer === 'picker' && options.length"
        :model-value="modelValue"
        :options="options"
        :placeholder="fieldProps.placeholder || `请选择${field.label}`"
        :title="field.label"
        :multiple="fieldProps.multiple === true"
        :disabled="effectiveDisabled"
        :clearable="fieldProps.clearable !== false"
        :filterable="fieldProps.filterable === true"
        :min="Number(fieldProps.min || 0)"
        :max="Number(fieldProps.max || 0)"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <AiRadioGroup
        v-else-if="descriptor.renderer === 'radio' || descriptor.renderer === 'radio-button'"
        :model-value="modelValue"
        :options="options"
        :button="descriptor.renderer === 'radio-button'"
        :inline="fieldProps.direction !== 'vertical' && fieldProps.inline !== false"
        :disabled="effectiveDisabled"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <AiCheckboxGroup
        v-else-if="descriptor.renderer === 'checkbox' || descriptor.renderer === 'transfer'"
        :model-value="modelValue"
        :options="options"
        :button="fieldProps.displayMode === 'button'"
        :inline="fieldProps.direction !== 'vertical' && fieldProps.inline !== false"
        :min="Number(fieldProps.min || 0)"
        :max="Number(fieldProps.max || 0)"
        :disabled="effectiveDisabled"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
      />
      <view v-else-if="descriptor.renderer === 'switch'" class="lowcode-field__switch">
        <wd-switch
          :model-value="switchValue"
          :disabled="effectiveDisabled || fieldProps.loading === true"
          active-color="var(--forge-color-primary, #0066ff)"
          @update:model-value="updateSwitchValue"
          @change="handleSwitchChange"
        />
        <text v-if="fieldProps.checkedText || fieldProps.uncheckedText" class="lowcode-field__switch-text">
          {{ switchValue ? fieldProps.checkedText : fieldProps.uncheckedText }}
        </text>
      </view>
      <wd-slider
        v-else-if="descriptor.renderer === 'slider'"
        :model-value="numericValue"
        :min="Number(fieldProps.min ?? 0)"
        :max="Number(fieldProps.max ?? 100)"
        :step="Number(fieldProps.step ?? 1)"
        :disabled="effectiveDisabled"
        :range="fieldProps.range === true"
        :reverse="fieldProps.reverse === true"
        :show-value="fieldProps.showTooltip !== false"
        @update:model-value="updateValue"
        @change="emit('change', $event?.value ?? $event)"
      />
      <wd-rate
        v-else-if="descriptor.renderer === 'rate'"
        :model-value="numericValue"
        :count="Number(fieldProps.count || 5)"
        :disabled="effectiveDisabled"
        :readonly="readonly"
        :allow-half="fieldProps.allowHalf === true"
        :clearable="fieldProps.clearable === true"
        :active-color="fieldProps.color || 'var(--forge-color-primary, #0066ff)'"
        @update:model-value="updateValue"
        @change="emit('change', $event?.value ?? $event)"
      />
      <AiDateTimePicker
        v-else-if="isDateTimeField"
        :model-value="modelValue"
        :type="fieldType"
        :title="field.label"
        :placeholder="fieldProps.placeholder || `请选择${field.label}`"
        :start-placeholder="fieldProps.startPlaceholder"
        :end-placeholder="fieldProps.endPlaceholder"
        :min="fieldProps.min ?? fieldProps.minDate"
        :max="fieldProps.max ?? fieldProps.maxDate"
        :disabled="effectiveDisabled"
        :readonly="readonly"
        :clearable="fieldProps.clearable !== false"
        :format="fieldProps.format"
        :value-format="fieldProps.valueFormat"
        @update:model-value="updateValue"
        @change="emit('change', $event)"
        @blur="emit('blur')"
      />
      <view v-else-if="descriptor.renderer === 'color'" class="lowcode-field__color">
        <view v-if="fieldProps.showPreview !== false" class="lowcode-field__color-preview" :style="{ backgroundColor: validColorValue }" />
        <AiField
          :model-value="modelValue"
          :placeholder="fieldProps.showAlpha ? '#3b82f6FF / rgba(...)' : '#0066ff'"
          :maxlength="32"
          :disabled="effectiveDisabled"
          :readonly="readonly"
          :clearable="fieldProps.clearable !== false"
          @update:model-value="updateValue"
          @blur="emit('blur')"
        />
        <view v-if="colorSwatches.length" class="lowcode-field__swatches">
          <button
            v-for="color in colorSwatches"
            :key="color"
            class="lowcode-field__swatch"
            :style="{ backgroundColor: color }"
            :disabled="effectiveDisabled"
            @click="updateValue(color)"
          />
        </view>
      </view>
      <AiFileUpload
        v-else-if="descriptor.renderer === 'file-upload'"
        :model-value="modelValue"
        :business-type="field.businessType || fieldProps.businessType || 'lowcode_attachment'"
        :max-count="Number(fieldProps.maxCount || 9)"
        :max-size="Number(fieldProps.maxSize || 0)"
        :accept="fieldProps.accept || ''"
        :multiple="fieldProps.multiple === true"
        :show-remove-button="fieldProps.showRemoveButton !== false"
        :readonly="readonly || effectiveDisabled"
        @update:model-value="updateValue"
      />
      <LowcodeImageUpload
        v-else-if="descriptor.renderer === 'image-upload'"
        :model-value="modelValue"
        :business-type="field.businessType || fieldProps.businessType || 'lowcode_image'"
        :crop="fieldProps.crop === true"
        :max-count="Number(fieldProps.maxCount || 1)"
        :max-size="Number(fieldProps.maxSize || 0)"
        :multiple="fieldProps.multiple === true"
        :show-remove-button="fieldProps.showRemoveButton !== false"
        :readonly="readonly || effectiveDisabled"
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
    <text v-if="scanMessage || fieldHint" class="lowcode-field__hint" :class="{ 'is-error': scanMessage }">
      {{ scanMessage || fieldHint }}
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
import AiRadioGroup from '@/components/AiRadioGroup.vue'
import AiSelect from '@/components/AiSelect.vue'
import AiSignaturePad from '@/components/AiSignaturePad.vue'
import AiTextarea from '@/components/AiTextarea.vue'
import LowcodeArrayField from './LowcodeArrayField.vue'
import LowcodeEntitySelector from './LowcodeEntitySelector.vue'
import LowcodeImageUpload from './LowcodeImageUpload.vue'
import LowcodeUnsupported from './LowcodeUnsupported.vue'
import { MOBILE_COMPONENT_CAPABILITY, resolveMobileComponent } from './mobile-component-registry'
import { scanBarcode } from '@/utils/barcode-scanner'
import { resolveMobileSelectionLabels } from '@/utils/mobile-selector-runtime'
import {
  formatMobileFieldValue,
  mobileFieldValueEquals,
  normalizeMobileFieldContract,
} from '@/utils/mobile-field-contract'

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
const normalizedField = computed(() => normalizeMobileFieldContract(props.field))
const fieldProps = computed(() => normalizedField.value.props || {})
const descriptor = computed(() => resolveMobileComponent(normalizedField.value.componentKey || normalizedField.value.type, { platform }))
const fieldType = computed(() => descriptor.value.type)
const isNumberRangeField = computed(() => descriptor.value.renderer === 'number-range')
const isDateTimeField = computed(() => ['datetime-picker', 'datetime-range'].includes(descriptor.value.renderer))
const usesDedicatedReadonlyRenderer = computed(() => ['array', 'file-upload', 'image-upload', 'signature'].includes(descriptor.value.renderer))
const isSingleSelectField = computed(() => ['picker', 'remote-select', 'entity-select', 'tree-select', 'cascader', 'record-selector'].includes(descriptor.value.renderer))
const isRemoteSelectorField = computed(() => ['remote-select', 'entity-select', 'tree-select', 'cascader', 'record-selector'].includes(descriptor.value.renderer))
const isOptionBackedField = computed(() => isSingleSelectField.value || ['radio', 'radio-button', 'checkbox', 'transfer'].includes(descriptor.value.renderer))
const compactRow = computed(() => [
  'text', 'number', 'money', 'picker', 'remote-select', 'entity-select',
  'tree-select', 'cascader', 'record-selector', 'datetime-picker', 'switch',
].includes(descriptor.value.renderer))
const fieldClasses = computed(() => ({
  'lowcode-field--compact-row': compactRow.value,
  'lowcode-field--readonly-row': showsReadonlyText.value,
  'is-round': fieldProps.value.round === true,
  'is-loading': fieldProps.value.loading === true,
  [`is-size-${fieldProps.value.size}`]: Boolean(fieldProps.value.size),
  [`is-status-${fieldProps.value.status}`]: Boolean(fieldProps.value.status),
}))
const readonlyClasses = computed(() => ({
  'is-empty': displayValue.value === '-',
  'is-ellipsis': fieldProps.value.ellipsis === true,
  'is-strong': fieldProps.value.strong === true,
  'is-italic': fieldProps.value.italic === true,
  'is-underline': fieldProps.value.underline === true,
  'is-delete': fieldProps.value.delete === true,
  'is-code': fieldProps.value.code === true,
  [`is-text-${fieldProps.value.type}`]: Boolean(fieldProps.value.type),
}))
const effectiveDisabled = computed(() => props.disabled || normalizedField.value.disabled === true || fieldProps.value.loading === true)
const textareaMinHeight = computed(() => `${Math.max(3, Number(fieldProps.value.rows || 3)) * 42}rpx`)
const arrayValue = computed(() => Array.isArray(props.modelValue) ? props.modelValue : [])
const numericValue = computed(() => {
  const number = Number(props.modelValue)
  return Number.isFinite(number) ? number : 0
})
const switchValue = computed(() => mobileFieldValueEquals(props.modelValue, fieldProps.value.checkedValue))
const validColorValue = computed(() => isSafeColor(props.modelValue) ? String(props.modelValue) : '#c1c3c6')
const colorSwatches = computed(() => (Array.isArray(fieldProps.value.swatches) ? fieldProps.value.swatches : []).filter(isSafeColor))
const fieldHint = computed(() => {
  if (props.hint) return props.hint
  if (descriptor.value.renderer === 'money' && fieldProps.value.showChinese === true)
    return toChineseCurrency(props.modelValue)
  if (descriptor.value.renderer === 'rate' && fieldProps.value.texts && props.modelValue !== '')
    return fieldProps.value.texts?.[props.modelValue] || fieldProps.value.texts?.[String(props.modelValue)] || ''
  return normalizedField.value.description || fieldProps.value.description || ''
})
const rangeValue = computed(() => {
  const value = props.modelValue
  if (Array.isArray(value)) return [value[0] ?? '', value[1] ?? '']
  if (value === undefined || value === null || value === '') return ['', '']
  // Legacy deployments stored the first endpoint as a scalar. Preserve it
  // while exposing the new two-endpoint shape to the submit payload.
  return [value, '']
})
const pairValue = computed(() => {
  if (Array.isArray(props.modelValue)) return [props.modelValue[0] ?? '', props.modelValue[1] ?? '']
  return ['', '']
})
const control = computed(() => props.field.__runtimeControl || { visible: true, required: normalizedField.value.required === true })
const readonly = computed(() => props.readonly || effectiveDisabled.value || normalizedField.value.readonly === true || control.value.readonly)
const showsReadonlyText = computed(() => descriptor.value.capability !== MOBILE_COMPONENT_CAPABILITY.BLOCKED
  && (readonly.value || descriptor.value.capability === MOBILE_COMPONENT_CAPABILITY.READONLY)
  && !usesDedicatedReadonlyRenderer.value)
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
  return formatMobileFieldValue(normalizedField.value, value, props.options)
})

function updateValue(value) {
  emit('update:modelValue', value)
}

function updateNumericValue(value) {
  if (value === '' || value === undefined || value === null) {
    updateValue('')
    return
  }
  const number = Number(value)
  updateValue(Number.isFinite(number) ? number : value)
}

function completeNumericInput() {
  const value = Number(props.modelValue)
  if (Number.isFinite(value)) {
    const min = numberValue(fieldProps.value.min)
    const max = numberValue(fieldProps.value.max)
    const precision = numberValue(fieldProps.value.precision)
    let next = value
    if (min !== undefined) next = Math.max(min, next)
    if (max !== undefined) next = Math.min(max, next)
    if (precision !== undefined) next = Number(next.toFixed(Math.max(0, precision)))
    if (next !== props.modelValue) updateValue(next)
  }
  emit('blur')
}

function updateSwitchValue(checked) {
  updateValue(checked ? fieldProps.value.checkedValue : fieldProps.value.uncheckedValue)
}

function handleSwitchChange(event) {
  const checked = event?.value ?? event
  emit('change', checked ? fieldProps.value.checkedValue : fieldProps.value.uncheckedValue)
}

function updateRangeValue(index, value) {
  const next = [...rangeValue.value]
  next[index] = value
  emit('update:modelValue', next)
}

function updatePairValue(index, value) {
  const next = [...pairValue.value]
  next[index] = value
  updateValue(next)
}

function copyDisplayValue() {
  uni.setClipboardData({ data: String(displayValue.value) })
}

async function scan() {
  if (scanning.value) return
  scanning.value = true
  scanMessage.value = ''
  try {
    const result = await scanBarcode({ timeoutMs: fieldProps.value.timeoutMs })
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

function numberValue(value) {
  if (value === undefined || value === null || value === '') return undefined
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

function toChineseCurrency(value) {
  const number = Number(value)
  if (!Number.isFinite(number) || number < 0 || number > 9999999999999.99) return ''
  if (number === 0) return '人民币零元整'
  const digits = '零壹贰叁肆伍陆柒捌玖'
  const units = ['', '拾', '佰', '仟']
  const groups = ['', '万', '亿', '兆']
  const [integerText, decimalText = ''] = number.toFixed(2).split('.')
  let integer = Number(integerText)
  let result = ''
  let groupIndex = 0
  let needZero = false
  while (integer > 0) {
    const group = integer % 10000
    if (group) {
      let groupResult = ''
      let position = 0
      let current = group
      while (current > 0) {
        const digit = current % 10
        if (digit) {
          groupResult = `${digits[digit]}${units[position]}${needZero ? '零' : ''}${groupResult}`
          needZero = false
        }
        else if (groupResult) needZero = true
        current = Math.floor(current / 10)
        position += 1
      }
      result = `${groupResult.replace(/零+$/g, '')}${groups[groupIndex]}${result}`
    }
    else if (result) needZero = true
    integer = Math.floor(integer / 10000)
    groupIndex += 1
  }
  const jiao = Number(decimalText[0] || 0)
  const fen = Number(decimalText[1] || 0)
  const decimal = `${jiao ? `${digits[jiao]}角` : ''}${fen ? `${digits[fen]}分` : ''}` || '整'
  return `人民币${result.replace(/零+/g, '零')}元${decimal}`
}

function isSafeColor(value) {
  const text = String(value || '').trim()
  return /^#[0-9a-f]{3,8}$/i.test(text)
    || /^rgba?\([\d\s.,%]+\)$/i.test(text)
    || /^hsla?\([\d\s.,%a-z]+\)$/i.test(text)
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
.lowcode-field__label { display: flex; margin-bottom: 12rpx; color: #747677; font-size: 28rpx; font-weight: 400; line-height: 1.5; }
.lowcode-field__required { margin-left: 6rpx; color: #ff5219; }
.lowcode-field__control { min-height: 88rpx; }
/* 只读值按纯文本展示，与带底色的可编辑控件区分开 */
.lowcode-field__readonly { min-height: 88rpx; padding: 12rpx 0; color: var(--forge-text-primary, #171a1d); font-size: 15px; box-sizing: border-box; line-height: 1.5; word-break: break-all; }
.lowcode-field__readonly.is-empty { color: var(--forge-text-tertiary, #a2a3a5); }
.lowcode-field__readonly { display: flex; align-items: center; gap: 12rpx; }
.lowcode-field__readonly-value { min-width: 0; flex: 1; }
.lowcode-field__readonly.is-ellipsis .lowcode-field__readonly-value { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lowcode-field__readonly.is-strong { font-weight: 600; }
.lowcode-field__readonly.is-italic { font-style: italic; }
.lowcode-field__readonly.is-underline { text-decoration: underline; }
.lowcode-field__readonly.is-delete { text-decoration: line-through; }
.lowcode-field__readonly.is-code { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.lowcode-field__readonly.is-text-primary { color: var(--forge-color-primary, #0066ff); }
.lowcode-field__readonly.is-text-success { color: var(--forge-color-success, #16a34a); }
.lowcode-field__readonly.is-text-warning { color: var(--forge-color-warning, #f59e0b); }
.lowcode-field__readonly.is-text-error { color: var(--forge-color-danger, #ff5219); }
.lowcode-field__copy { min-height: 52rpx; margin: 0; padding: 0 12rpx; border: 0; color: var(--forge-color-primary, #0066ff); font-size: 22rpx; line-height: 52rpx; background: transparent; }
.lowcode-field__copy::after { border: 0; }
.lowcode-field__barcode { display: flex; align-items: center; gap: 12rpx; }
.lowcode-field__barcode :deep(.ai-field) { flex: 1; min-width: 0; }
.lowcode-field__scan { flex: 0 0 auto; }
.lowcode-field__range { display: flex; align-items: center; gap: 10rpx; }
.lowcode-field__range-input { min-width: 0; flex: 1; }
.lowcode-field__range-separator { flex: 0 0 auto; color: #a2a3a5; font-size: 24rpx; }
.lowcode-field__color { display: flex; flex-wrap: wrap; align-items: center; gap: 12rpx; }
.lowcode-field__color-preview { width: 88rpx; height: 88rpx; flex: 0 0 auto; border: 1rpx solid var(--forge-color-border, #c1c3c6); border-radius: var(--radius-control); }
.lowcode-field__color :deep(.ai-field) { min-width: 0; flex: 1; }
.lowcode-field__swatches { display: flex; flex: 0 0 100%; flex-wrap: wrap; gap: 10rpx; }
.lowcode-field__swatch { width: 44rpx; height: 44rpx; min-height: 44rpx; margin: 0; padding: 0; border: 2rpx solid #fff; border-radius: 8rpx; box-shadow: 0 0 0 1rpx var(--forge-color-border, #c1c3c6); }
.lowcode-field__swatch::after { border: 0; }
.lowcode-field__error { display: block; margin-top: 8rpx; color: #ff5219; font-size: 24rpx; }
.lowcode-field__hint { display: block; margin-top: 8rpx; color: #a2a3a5; font-size: 22rpx; }
.lowcode-field__hint.is-error { color: #ff5219; }
.lowcode-field__switch { display: flex; min-height: 88rpx; align-items: center; gap: 12rpx; }
.lowcode-field__switch-text { color: #747677; font-size: 22rpx; }
.lowcode-field.is-status-error :deep(.ai-field__control), .lowcode-field.is-status-error :deep(.ai-textarea) { border-color: var(--forge-color-danger, #ff5219); }
.lowcode-field.is-status-warning :deep(.ai-field__control), .lowcode-field.is-status-warning :deep(.ai-textarea) { border-color: var(--forge-color-warning, #f59e0b); }
.lowcode-field.is-round :deep(.ai-field__control), .lowcode-field.is-round :deep(.ai-textarea) { border-radius: 999rpx; }
.lowcode-field.is-loading { opacity: .72; }
</style>
