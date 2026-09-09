<template>
  <!-- Layout mode: delegate to LowcodeLayoutNodes when the schema has layout containers -->
  <LowcodeLayoutNodes
    v-if="hasLayoutNodes"
    ref="layoutRef"
    :nodes="nodes"
    :data="data"
    :dict-options="dictOptions"
    :readonly="readonly"
    :context="context"
    :field-linkages="fieldLinkages"
    @update:data="(v) => emit('update:data', v)"
    @field-event="(payload) => emit('field-event', payload)"
  />

  <!-- Flat mode: original field-only rendering -->
  <view v-else class="lowcode-form" :class="{ 'lowcode-form--inline-grid': layout === 'inline_grid' }">
    <LowcodeField
      v-for="field in renderedFields"
      :key="field.field"
      class="lowcode-form__field"
      :field="field"
      :model-value="data[field.field]"
      :options="fieldOptions(field)"
      :readonly="readonly"
      :error="errors[field.field]"
      @update:model-value="updateField(field, $event)"
      @blur="emit('field-event', { trigger: 'BLUR', field, data })"
      @change="emit('field-event', { trigger: 'CHANGE', field, data })"
      @scan="emit('field-event', { trigger: 'SCAN_COMPLETE', field, data, scan: $event })"
    />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import LowcodeField from './LowcodeField.vue'
import LowcodeLayoutNodes from './LowcodeLayoutNodes.vue'
import { applyFieldLinkageChange, filterFieldOptionsByLinkage, hasDesignerLayoutNodes, resolveFieldControl, resolveFieldLinkageContext } from '@/utils/lowcode-runtime'

const props = defineProps({
  fields: { type: Array, default: () => [] },
  /** Renderable node tree from normalizeDesignerComponents; enables layout mode when contains layout nodes. */
  nodes: { type: Array, default: () => [] },
  data: { type: Object, default: () => ({}) },
  dictOptions: { type: Object, default: () => ({}) },
  currentChildren: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
  context: { type: Object, default: () => ({}) },
  layout: { type: String, default: 'stack' },
  fieldLinkages: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:data', 'field-event'])
const errors = reactive({})
const layoutRef = ref(null)
const hasLayoutNodes = computed(() => hasDesignerLayoutNodes(props.nodes))
const renderedFields = computed(() => props.fields.map(field => ({
  ...field,
  props: {
    ...(field.props || {}),
    linkageContext: resolveFieldLinkageContext(props.fieldLinkages, field.field, props.data),
  },
  __runtimeControl: resolveFieldControl(field, {
    record: props.data,
    formData: props.data,
    row: props.data,
    route: { query: props.context.routeQuery || {} },
    user: props.context.user || {},
  }),
})).filter(field => field.__runtimeControl.visible))

function fieldOptions(field) {
  if (field.type === 'dictSelect' || field.type === 'pillSelect') {
    const options = props.dictOptions[field.dictType || field.props?.dictType] || []
    return filterFieldOptionsByLinkage(options, field.props?.linkageContext)
  }
  const source = field.props?.options || field.options
  if (Array.isArray(source)) {
    const options = source.map(item => typeof item === 'object' ? item : ({ label: String(item), value: item }))
    return filterFieldOptionsByLinkage(options, field.props?.linkageContext)
  }
  if (field.props?.optionSource?.type === 'CURRENT_CHILDREN') {
    const sourceRows = props.currentChildren[field.props.optionSource.relationKey] || []
    return sourceRows.filter(row => props.currentChildren[field.props.optionSource.relationKey]).map(row => ({
      label: row[field.props.optionSource.labelField] ?? row.id,
      value: row[field.props.optionSource.valueField] ?? row.id,
    }))
  }
  return []
}

function updateField(field, value) {
  props.data[field.field] = value
  applyFieldLinkageChange(props.fieldLinkages, field.field, props.data)
  delete errors[field.field]
  emit('update:data', props.data)
}

function validate() {
  // Delegate to layout tree when in layout mode
  if (hasLayoutNodes.value && layoutRef.value)
    return layoutRef.value.validate()
  Object.keys(errors).forEach(key => delete errors[key])
  for (const field of props.fields) {
    const control = resolveFieldControl(field, { record: props.data, formData: props.data, row: props.data })
    if (!control.visible || !control.required || props.readonly || control.readonly) continue
    const value = props.data[field.field]
    if (value === undefined || value === null || value === '' || (Array.isArray(value) && !value.length)) {
      errors[field.field] = field.requiredMessage || `请输入${field.label}`
    }
  }
  return Object.keys(errors).length === 0
}

defineExpose({ validate })
</script>

<style lang="scss" scoped>
.lowcode-form { display: flex; flex-direction: column; }
.lowcode-form--inline-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr) minmax(132rpx, 0.65fr);
  gap: 14rpx;
  align-items: end;
}
.lowcode-form--inline-grid :deep(.lowcode-field) { min-width: 0; margin-bottom: 0; }
.lowcode-form--inline-grid :deep(.lowcode-field__label) { min-height: 34rpx; font-size: 22rpx; }
.lowcode-form--inline-grid :deep(.lowcode-field__control) { min-width: 0; }
.lowcode-form--inline-grid :deep(.lowcode-field__readonly) { padding: 16rpx; font-size: 24rpx; }
@media (max-width: 360px) {
  .lowcode-form--inline-grid { grid-template-columns: minmax(0, 1fr) minmax(132rpx, 0.72fr); }
}
</style>
