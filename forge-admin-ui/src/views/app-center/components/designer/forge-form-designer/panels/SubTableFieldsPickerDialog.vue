<template>
  <n-modal
    :show="show"
    preset="card"
    :title="title"
    :style="{ width: 'min(520px, calc(100vw - 32px))' }"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <div v-if="requiredCodes.length" class="picker-hint">
      带「必填」标记的字段必须显示，取消勾选会被自动保留，否则子表保存会报错。
    </div>
    <div class="picker-toolbar">
      <n-checkbox
        :checked="allSelected"
        :indeterminate="someSelected"
        size="small"
        @update:checked="toggleAll"
      >
        全选
      </n-checkbox>
      <span class="picker-count">
        已选 {{ draft.length }} / {{ options.length }} 项
        <template v-if="requiredCodes.length">
          · 必填 {{ requiredCodes.length }}
        </template>
      </span>
    </div>
    <n-spin :show="loading" size="small">
      <n-checkbox-group
        v-if="options.length"
        :value="draft"
        class="field-checkbox-grid"
        @update:value="onDraftUpdate"
      >
        <n-checkbox
          v-for="field in options"
          :key="field.value"
          :value="field.value"
          :disabled="field.required === true"
          size="small"
        >
          <span class="field-checkbox-copy">
            <strong>
              {{ field.label }}
              <span v-if="field.required" class="field-required-tag">必填</span>
            </strong>
            <small>{{ field.value }}</small>
          </span>
        </n-checkbox>
      </n-checkbox-group>
      <n-empty
        v-else
        size="small"
        description="未读取到字段"
      />
    </n-spin>
    <div v-if="draft.length" class="field-order-section">
      <div class="field-order-title">
        <strong>展示顺序</strong>
        <span>拖动字段调整运行时渲染顺序</span>
      </div>
      <draggable
        v-model="draft"
        :item-key="item => item"
        handle=".field-drag-handle"
        class="field-order-list"
        :animation="150"
      >
        <template #item="{ element, index }">
          <div class="field-order-row">
            <span class="field-drag-handle" title="拖动排序">⋮⋮</span>
            <span class="field-order-index">{{ index + 1 }}</span>
            <span class="field-order-label">{{ resolveOptionLabel(element) }}</span>
            <n-button text size="tiny" :disabled="index === 0" aria-label="上移" @click="moveDraft(index, -1)">
              ↑
            </n-button>
            <n-button text size="tiny" :disabled="index === draft.length - 1" aria-label="下移" @click="moveDraft(index, 1)">
              ↓
            </n-button>
            <span v-if="requiredCodes.includes(element)" class="field-required-tag">必填</span>
          </div>
        </template>
      </draggable>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <span class="footer-spacer" />
        <n-button size="small" @click="emit('update:show', false)">
          取消
        </n-button>
        <n-button size="small" type="primary" @click="confirm">
          确定
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import {
  collectRequiredFieldCodes,
  ensureRequiredDisplayFieldCodes,
} from './sub-table-display-fields'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '选择字段',
  },
  /** 可选字段列表 [{ label, value, required? }] */
  options: {
    type: Array,
    default: () => [],
  },
  /** 已选字段编码 */
  modelValue: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'confirm'])

const draft = ref([])

const requiredCodes = computed(() => collectRequiredFieldCodes(props.options))

watch(() => props.show, (visible) => {
  if (visible)
    draft.value = ensureRequiredDisplayFieldCodes(props.modelValue, props.options)
})

watch(() => props.options, (options) => {
  if (!props.show)
    return
  draft.value = ensureRequiredDisplayFieldCodes(draft.value, options)
})

const allSelected = computed(() =>
  Boolean(props.options.length) && draft.value.length === props.options.length,
)
const someSelected = computed(() =>
  draft.value.length > 0 && draft.value.length < props.options.length,
)

function onDraftUpdate(next) {
  draft.value = ensureRequiredDisplayFieldCodes(next, props.options)
}

function toggleAll(checked) {
  if (checked) {
    draft.value = props.options.map(o => o.value).filter(Boolean)
    return
  }
  // 取消全选时仍保留必填
  draft.value = [...requiredCodes.value]
}

function confirm() {
  emit('confirm', ensureRequiredDisplayFieldCodes(draft.value, props.options))
  emit('update:show', false)
}

function resolveOptionLabel(value) {
  return props.options.find(option => option.value === value)?.label || value
}

function moveDraft(index, offset) {
  const target = index + offset
  if (target < 0 || target >= draft.value.length)
    return
  const next = [...draft.value]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  draft.value = next
}
</script>

<style scoped>
.picker-hint {
  margin-bottom: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fff7e8;
  color: #ad6800;
  font-size: 12px;
  line-height: 1.5;
}

.picker-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.picker-count {
  font-size: 12px;
  color: var(--text-tertiary, #86909c);
}

.field-checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 10px;
  max-height: 320px;
  overflow-y: auto;
  padding: 6px 8px;
  border: 1px solid #ececf0;
  border-radius: 6px;
  background: var(--bg-primary);
}

.field-order-section {
  margin-top: 12px;
}

.field-order-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--text-primary, #1f2937);
  font-size: 12px;
}

.field-order-title span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.field-order-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 220px;
  overflow-y: auto;
  padding: 6px 8px;
  border: 1px solid #ececf0;
  border-radius: 6px;
  background: #fafafc;
}

.field-order-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 4px 6px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
  background: var(--bg-primary);
}

.field-drag-handle {
  cursor: grab;
  color: var(--text-tertiary);
  letter-spacing: -2px;
}

.field-order-index {
  width: 18px;
  color: var(--text-tertiary);
  font-size: 11px;
  text-align: center;
}

.field-order-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary, #1f2937);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-checkbox-copy {
  display: inline-flex;
  flex-direction: column;
  gap: 1px;
  margin-left: 2px;
  min-width: 0;
}

.field-checkbox-copy strong {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-primary, #1f2937);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.field-required-tag {
  flex-shrink: 0;
  padding: 0 4px;
  border-radius: 3px;
  background: #fff1f0;
  color: #cf1322;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
}

.field-checkbox-copy small {
  color: var(--text-tertiary, #86909c);
  font-size: 10px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-spacer {
  flex: 1;
}

@media (max-width: 600px) {
  .field-checkbox-grid {
    grid-template-columns: 1fr;
  }
}
</style>
