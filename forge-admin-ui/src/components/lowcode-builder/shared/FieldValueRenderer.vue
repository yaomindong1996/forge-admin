<template>
  <component
    :is="linkHref ? 'a' : 'span'"
    class="field-value-renderer"
    :class="[`render-${renderMode}`, { 'is-empty': isEmptyValue, 'is-link': !!linkHref }]"
    :style="valueStyle"
    :href="linkHref || undefined"
    @click="handleClick"
  >
    <DictTag
      v-if="renderMode === 'dictTag'"
      :dict-type="effectiveDictType"
      :options="dictOptions"
      :value="normalizedValue"
      :force-tag="renderConfig.forceTag !== false"
      :type="renderConfig.tagType || renderConfig.type || ''"
      size="small"
    />
    <n-tag
      v-else-if="renderMode === 'tag'"
      :type="renderConfig.tagType || resolveTagType(normalizedValue)"
      size="small"
      :bordered="renderConfig.bordered !== false"
    >
      {{ displayText }}
    </n-tag>
    <span v-else>{{ displayText }}</span>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { getNestedRuntimeValue, matchRuntimeRule } from './runtime-rules'

const props = defineProps({
  value: {
    type: [String, Number, Boolean, Array, Object],
    default: null,
  },
  row: {
    type: Object,
    default: () => ({}),
  },
  field: {
    type: Object,
    default: () => ({}),
  },
  setting: {
    type: Object,
    default: () => ({}),
  },
  context: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['navigate'])

const renderConfig = computed(() => ({
  ...(props.field?.render || {}),
  ...(props.field?.renderConfig || {}),
  ...(props.field?.props?.renderConfig || {}),
  ...(props.setting || {}),
}))
const normalizedValue = computed(() => {
  const valueField = renderConfig.value.valueField || renderConfig.value.targetField
  if (valueField)
    return getNestedRuntimeValue(props.row || {}, valueField) ?? props.value
  return props.value
})
const renderMode = computed(() => {
  const type = renderConfig.value.renderType || renderConfig.value.type || ''
  if (type === 'dictTag' || renderConfig.value.dictType || props.field?.dictType || props.field?.props?.dictType)
    return 'dictTag'
  if (['tag', 'statusTag', 'badge'].includes(type))
    return 'tag'
  if (type === 'link' || renderConfig.value.clickAction === 'navigate')
    return 'link'
  return 'text'
})
const effectiveDictType = computed(() => renderConfig.value.dictType || props.field?.dictType || props.field?.props?.dictType || '')
const dictOptions = computed(() => Array.isArray(renderConfig.value.options) ? renderConfig.value.options : props.field?.options || null)
const isEmptyValue = computed(() => normalizedValue.value === null || normalizedValue.value === undefined || normalizedValue.value === '')
const displayText = computed(() => {
  if (isEmptyValue.value)
    return renderConfig.value.emptyText || '-'
  if (renderConfig.value.textField) {
    const text = getNestedRuntimeValue(props.row || {}, renderConfig.value.textField)
    if (text !== undefined && text !== null && text !== '')
      return String(text)
  }
  const optionLabel = resolveStaticOptionDisplayText(normalizedValue.value)
  if (optionLabel !== null)
    return optionLabel
  if (renderConfig.value.format === 'money')
    return formatMoney(normalizedValue.value, renderConfig.value)
  if (renderConfig.value.prefix || renderConfig.value.suffix)
    return `${renderConfig.value.prefix || ''}${normalizedValue.value}${renderConfig.value.suffix || ''}`
  if (Array.isArray(normalizedValue.value))
    return normalizedValue.value.join(renderConfig.value.separator || '、')
  if (typeof normalizedValue.value === 'object')
    return JSON.stringify(normalizedValue.value)
  return String(normalizedValue.value)
})
const valueStyle = computed(() => ({
  color: resolveRuleColor() || renderConfig.value.textColor || renderConfig.value.color || undefined,
  fontWeight: renderConfig.value.bold === true ? 600 : undefined,
}))
const linkHref = computed(() => {
  if (renderMode.value !== 'link')
    return ''
  return renderConfig.value.href || '#'
})

function resolveRuleColor() {
  const colorRules = Array.isArray(renderConfig.value.colorRules) ? renderConfig.value.colorRules : []
  const context = {
    ...props.context,
    record: props.row,
    row: props.row,
    data: props.row,
  }
  const matched = colorRules.find(rule => rule?.color && matchRuntimeRule(rule, context))
  return matched?.color || matched?.textColor || ''
}

function resolveTagType(value) {
  const typeRules = Array.isArray(renderConfig.value.typeRules) ? renderConfig.value.typeRules : []
  const context = {
    ...props.context,
    record: props.row,
    row: props.row,
    data: props.row,
  }
  const matched = typeRules.find(rule => rule?.tagType && matchRuntimeRule(rule, context))
  if (matched?.tagType)
    return matched.tagType
  const text = String(value ?? '')
  if (['success', 'enabled', 'normal', '1', 'true'].includes(text))
    return 'success'
  if (['warning', 'pending'].includes(text))
    return 'warning'
  if (['error', 'disabled', 'fail', 'failed', '0', 'false'].includes(text))
    return 'error'
  return 'default'
}

function resolveStaticOptionDisplayText(value) {
  const options = Array.isArray(renderConfig.value.options)
    ? renderConfig.value.options
    : (Array.isArray(props.field?.options) && props.field.options.length
        ? props.field.options
        : (Array.isArray(props.field?.props?.options) ? props.field.props.options : null))
  if (!Array.isArray(options) || !options.length)
    return null
  const values = Array.isArray(value) ? value : [value]
  const labels = values.map((item) => {
    const matched = options.find(option => String(option?.value ?? option?.key) === String(item))
    return matched?.label ?? item
  })
  if (!labels.length)
    return null
  return labels.join(renderConfig.value.separator || '、')
}

function formatMoney(value, config = {}) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return String(value ?? '')
  const divisor = Number(config.divisor || 1)
  const precision = Number(config.precision ?? 2)
  return `${config.prefix || ''}${(number / (Number.isFinite(divisor) && divisor ? divisor : 1)).toFixed(precision)}${config.suffix || ''}`
}

function handleClick(event) {
  if (renderMode.value !== 'link')
    return
  event?.preventDefault?.()
  emit('navigate', {
    event,
    row: props.row,
    field: props.field,
    setting: renderConfig.value,
  })
}
</script>

<style scoped>
.field-value-renderer {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  min-width: 0;
  vertical-align: middle;
}

.field-value-renderer > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-value-renderer.is-empty {
  color: #94a3b8;
}

.field-value-renderer.is-link {
  color: #2563eb;
  cursor: pointer;
  text-decoration: none;
}

.field-value-renderer.is-link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}
</style>
