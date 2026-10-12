<template>
  <n-form-item label="数值范围">
    <div class="field-number-constraint">
      <div class="constraint-row">
        <n-input-number
          :value="component.props?.min"
          :show-button="false"
          clearable
          placeholder="最小值"
          @update:value="emitProps({ min: $event })"
        />
        <n-input-number
          :value="component.props?.max"
          :show-button="false"
          clearable
          placeholder="最大值"
          @update:value="emitProps({ max: $event })"
        />
      </div>
      <div class="constraint-row">
        <n-input-number
          :value="fieldPrecision"
          :min="0"
          :max="fieldPrecisionMax"
          :show-button="false"
          placeholder="小数位"
          @update:value="emitProps({ precision: $event })"
        />
        <span class="precision-label">小数位</span>
      </div>
      <span class="constraint-help">{{ storageHint }}</span>
    </div>
  </n-form-item>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  component: { type: Object, required: true },
  fieldAsset: { type: Object, default: null },
})

const emit = defineEmits(['update'])

const fieldPrecision = computed(() => {
  const value = props.component.props?.precision ?? props.fieldAsset?.precision ?? 0
  return Math.max(0, Number(value) || 0)
})

const fieldPrecisionMax = computed(() => {
  const dataType = String(props.fieldAsset?.dataType || '').toLowerCase()
  if (dataType !== 'decimal')
    return 0
  return Math.max(0, Number(props.fieldAsset?.length || 18) - 1)
})

const storageHint = computed(() => {
  const dataType = String(props.fieldAsset?.dataType || '').toLowerCase()
  if (dataType === 'decimal')
    return `当前字段存储为 decimal(${props.fieldAsset?.length || 18}, ${props.fieldAsset?.precision ?? 2})；页面范围只能在字段容量内收紧。`
  if (dataType === 'int' || dataType === 'integer')
    return '当前字段存储为 int，数据库范围为 -2147483648 ～ 2147483647。'
  if (dataType === 'bigint')
    return '当前字段存储为 bigint；超出数据库范围时保存接口会直接提示。'
  if (dataType === 'tinyint')
    return '当前字段存储为 tinyint，数据库范围为 -128 ～ 127。'
  return '页面范围会与字段存储容量共同生效。'
})

function emitProps(patch) {
  emit('update', patch)
}
</script>

<style scoped>
.field-number-constraint {
  display: grid;
  gap: 6px;
  width: 100%;
}

.constraint-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px;
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: var(--bg-primary);
  padding: 5px;
}

.precision-label,
.constraint-help {
  border: 1px dashed #d4d4d8;
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.55;
}

.precision-label {
  display: flex;
  align-items: center;
  padding: 5px 8px;
}

.constraint-help {
  padding: 7px 9px;
}
</style>
