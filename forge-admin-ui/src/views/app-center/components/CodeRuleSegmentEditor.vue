<template>
  <section class="segment-editor">
    <header class="segment-editor__header">
      <div>
        <div class="segment-editor__title">
          编码分段
          <n-tag size="small" :bordered="false">
            {{ rows.length }} 段
          </n-tag>
        </div>
        <p>拖动调整输出顺序；稳定分段键不会随排序改变，已有流水计数器可继续使用。</p>
      </div>
      <n-dropdown
        v-if="!disabled"
        trigger="click"
        :options="segmentTypeOptions"
        @select="addSegment"
      >
        <n-button type="primary" size="small">
          <template #icon>
            <i class="i-material-symbols:add-rounded" />
          </template>
          添加段
        </n-button>
      </n-dropdown>
    </header>

    <div class="segment-editor__table">
      <div class="segment-editor__table-head">
        <span>顺序</span>
        <span>段类型</span>
        <span>来源 / 格式 / 值</span>
        <span>长度</span>
        <span>补位</span>
        <span>分组</span>
        <span>列入编码</span>
        <span>操作</span>
      </div>

      <draggable
        v-model="rows"
        item-key="segmentKey"
        handle=".segment-drag-handle"
        :disabled="disabled"
        ghost-class="segment-editor__row--ghost"
        @end="normalizeRows"
      >
        <template #item="{ element: segment, index }">
          <article class="segment-editor__row">
            <div class="segment-editor__row-main">
              <button
                class="segment-drag-handle"
                type="button"
                :disabled="disabled"
                title="拖动排序"
              >
                <i class="i-material-symbols:drag-indicator" />
                <span>{{ index + 1 }}</span>
              </button>

              <n-select
                size="small"
                :value="segment.segmentType"
                :options="segmentTypeOptions"
                :disabled="disabled"
                @update:value="value => changeType(index, value)"
              />

              <div class="segment-editor__value">
                <n-select
                  v-if="segment.segmentType === 'DATE'"
                  size="small"
                  :value="segment.segmentValue"
                  :options="dateFormatOptions"
                  :disabled="disabled"
                  @update:value="value => patchSegment(index, { segmentValue: value, segmentLength: value.length })"
                />
                <n-input
                  v-else-if="segment.segmentType === 'FIXED'"
                  size="small"
                  :value="segment.segmentValue"
                  placeholder="固定前缀、后缀或分隔符"
                  :disabled="disabled"
                  @update:value="value => patchFixedValue(index, value)"
                />
                <div v-else-if="segment.segmentType === 'VARIABLE'" class="segment-editor__variable-value">
                  <n-select
                    size="small"
                    :value="segment.variableSource || 'CUSTOM'"
                    :options="variableSourceOptions"
                    :disabled="disabled"
                    @update:value="value => changeVariableSource(index, value)"
                  />
                  <button
                    v-if="segment.variableSource === 'LOWCODE'"
                    type="button"
                    class="segment-editor__mapping-summary"
                    :class="{ 'is-empty': !segment.segmentValue }"
                    :disabled="disabled"
                    @click="requestLowCodeMapping(segment.segmentKey)"
                  >
                    <i class="i-material-symbols:account-tree-outline" />
                    <span>
                      <strong>{{ lowCodeFieldLabel(segment) }}</strong>
                      <small>{{ lowCodeObjectLabel }}</small>
                    </span>
                    <i class="i-material-symbols:edit-outline-rounded" />
                  </button>
                  <n-input
                    v-else
                    size="small"
                    :value="segment.segmentValue"
                    placeholder="变量名，如 customerType"
                    :disabled="disabled"
                    @update:value="value => patchSegment(index, { segmentValue: value })"
                  />
                </div>
                <n-select
                  v-else-if="segment.segmentType === 'SYS_VAR'"
                  size="small"
                  filterable
                  :value="segment.segmentValue"
                  :options="systemVariableOptions"
                  :disabled="disabled"
                  @update:value="value => patchSegment(index, { segmentValue: value })"
                />
                <n-select
                  v-else
                  size="small"
                  :value="segment.radixType"
                  :options="radixTypeOptions"
                  :disabled="disabled"
                  @update:value="value => patchSequenceRadix(index, value)"
                />
              </div>

              <n-input-number
                size="small"
                :value="segment.segmentLength"
                :min="1"
                :max="segment.segmentType === 'SEQ' ? 32 : 96"
                :show-button="false"
                :disabled="disabled || segment.segmentType === 'DATE'"
                @update:value="value => patchSegment(index, { segmentLength: value })"
              />

              <n-switch
                size="small"
                :value="Number(segment.padEnabled) === 1"
                :disabled="disabled || segment.segmentType === 'SEQ' || segment.segmentType === 'DATE'"
                @update:value="value => patchSegment(index, { padEnabled: value ? 1 : 0 })"
              />

              <n-switch
                size="small"
                :value="Number(segment.groupEnabled) === 1"
                :disabled="disabled || segment.segmentType === 'SEQ'"
                @update:value="value => patchSegment(index, { groupEnabled: value ? 1 : 0 })"
              />

              <n-switch
                size="small"
                :value="Number(segment.includeInCode) === 1"
                :disabled="disabled"
                @update:value="value => patchSegment(index, { includeInCode: value ? 1 : 0 })"
              />

              <div class="segment-editor__actions">
                <n-button
                  quaternary
                  circle
                  size="small"
                  :type="expandedKeys.has(segment.segmentKey) ? 'primary' : 'default'"
                  title="高级配置"
                  @click="toggleExpanded(segment.segmentKey)"
                >
                  <template #icon>
                    <i class="i-material-symbols:tune-rounded" />
                  </template>
                </n-button>
                <n-button
                  v-if="!disabled"
                  quaternary
                  circle
                  size="small"
                  type="error"
                  :disabled="rows.length <= 1"
                  title="删除分段"
                  @click="removeSegment(index)"
                >
                  <template #icon>
                    <i class="i-material-symbols:close-rounded" />
                  </template>
                </n-button>
              </div>
            </div>

            <n-collapse-transition :show="expandedKeys.has(segment.segmentKey)">
              <div class="segment-editor__advanced">
                <div class="advanced-field">
                  <label>稳定分段键</label>
                  <n-input size="small" :value="segment.segmentKey" disabled />
                </div>

                <template v-if="segment.segmentType === 'SEQ'">
                  <div class="advanced-field">
                    <label>起始值</label>
                    <n-input-number
                      size="small"
                      :value="segment.startValue"
                      :min="0"
                      :disabled="disabled"
                      @update:value="value => patchSegment(index, { startValue: value })"
                    />
                  </div>
                  <div class="advanced-field advanced-field--switch">
                    <label>周期重置</label>
                    <n-switch
                      size="small"
                      :value="Number(segment.resetEnabled) === 1"
                      :disabled="disabled"
                      @update:value="value => patchSegment(index, {
                        resetEnabled: value ? 1 : 0,
                        resetPolicy: value ? (segment.resetPolicy === 'NONE' ? 'DAY' : segment.resetPolicy) : 'NONE',
                      })"
                    />
                  </div>
                  <div class="advanced-field">
                    <label>重置周期</label>
                    <n-select
                      size="small"
                      :value="segment.resetPolicy"
                      :options="resetPolicyOptions"
                      :disabled="disabled || Number(segment.resetEnabled) !== 1"
                      @update:value="value => patchSegment(index, { resetPolicy: value })"
                    />
                  </div>
                  <div class="advanced-field advanced-field--switch">
                    <label>排除 I / O / Z</label>
                    <n-switch
                      size="small"
                      :value="Number(segment.excludeAmbiguous) === 1"
                      :disabled="disabled || ['DECIMAL', 'HEX'].includes(segment.radixType)"
                      @update:value="value => patchSegment(index, { excludeAmbiguous: value ? 1 : 0 })"
                    />
                  </div>
                </template>

                <template v-else-if="segment.segmentType !== 'DATE'">
                  <div class="advanced-field">
                    <label>补位字符</label>
                    <n-input
                      size="small"
                      maxlength="1"
                      :value="segment.padChar"
                      :disabled="disabled || Number(segment.padEnabled) !== 1"
                      @update:value="value => patchSegment(index, { padChar: value })"
                    />
                  </div>
                  <div class="advanced-field">
                    <label>补位方向</label>
                    <n-select
                      size="small"
                      :value="segment.padDirection"
                      :options="padDirectionOptions"
                      :disabled="disabled || Number(segment.padEnabled) !== 1"
                      @update:value="value => patchSegment(index, { padDirection: value })"
                    />
                  </div>
                </template>
              </div>
            </n-collapse-transition>
          </article>
        </template>
      </draggable>

      <n-empty v-if="!rows.length" description="暂无编码分段" class="segment-editor__empty" />
    </div>

    <footer class="segment-editor__tips">
      <span><i class="i-material-symbols:info-outline-rounded" /> 流水号固定左补；超过进制容量会失败，不会截断。</span>
      <span>业务变量可映射低代码字段，也可由业务代码通过 fields 传入自定义变量。</span>
      <span>参与分组的段共同决定独立计数器，原始分组值不会写入计数键。</span>
    </footer>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import draggable from 'vuedraggable'
import {
  changeCodeRuleSegmentType,
  changeCodeRuleVariableSource,
  createCodeRuleSegment,
  normalizeCodeRuleSegments,
} from '../code-rule-utils'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  capabilities: {
    type: Object,
    default: () => ({}),
  },
  disabled: Boolean,
  sourceObjectId: {
    type: [Number, String],
    default: null,
  },
  businessObjectOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'requestLowCodeMapping'])
const expandedKeys = ref(new Set())

const rows = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', normalizeCodeRuleSegments(value)),
})

const segmentTypeOptions = computed(() => props.capabilities.segmentTypes || [])
const dateFormatOptions = computed(() => props.capabilities.dateFormats || [])
const radixTypeOptions = computed(() => props.capabilities.radixTypes || [])
const resetPolicyOptions = computed(() => props.capabilities.resetPolicies || [])
const systemVariableOptions = computed(() => props.capabilities.systemVariables || [])
const businessFieldOptions = computed(() => props.capabilities.businessFields || [])
const variableSourceOptions = computed(() => props.capabilities.variableSources?.length
  ? props.capabilities.variableSources
  : [
      { label: '自定义变量', value: 'CUSTOM' },
      { label: '低代码字段', value: 'LOWCODE' },
    ])
const lowCodeObjectLabel = computed(() => {
  if (!props.sourceObjectId)
    return '尚未选择来源对象'
  const option = props.businessObjectOptions.find(item => String(item.value) === String(props.sourceObjectId))
  return option?.label || `业务对象 ${props.sourceObjectId}`
})
const padDirectionOptions = [
  { label: '左侧补位', value: 'LEFT' },
  { label: '右侧补位', value: 'RIGHT' },
]

function emitRows(value) {
  emit('update:modelValue', normalizeCodeRuleSegments(value))
}

function patchSegment(index, patch) {
  const next = rows.value.map((segment, current) => current === index ? { ...segment, ...patch } : segment)
  emitRows(next)
}

function patchFixedValue(index, value) {
  const segment = rows.value[index]
  patchSegment(index, {
    segmentValue: value,
    segmentLength: Number(segment?.padEnabled) === 1 ? segment.segmentLength : Math.max(value.length, 1),
  })
}

function patchSequenceRadix(index, value) {
  const padChar = value === 'ALPHA_UPPER' ? 'A' : value === 'ALPHA_LOWER' ? 'a' : '0'
  patchSegment(index, {
    radixType: value,
    padChar,
    excludeAmbiguous: ['DECIMAL', 'HEX'].includes(value) ? 0 : rows.value[index].excludeAmbiguous,
  })
}

function changeType(index, type) {
  const next = [...rows.value]
  next[index] = changeCodeRuleSegmentType(next[index], type)
  emitRows(next)
}

function changeVariableSource(index, variableSource) {
  if (variableSource === 'LOWCODE') {
    requestLowCodeMapping(rows.value[index]?.segmentKey)
    return
  }
  const next = [...rows.value]
  next[index] = changeCodeRuleVariableSource(next[index], variableSource)
  emitRows(next)
}

function requestLowCodeMapping(segmentKey) {
  if (!segmentKey || props.disabled)
    return
  emit('requestLowCodeMapping', segmentKey)
}

function lowCodeFieldLabel(segment) {
  if (!segment?.segmentValue)
    return '选择对象与字段'
  const option = businessFieldOptions.value.find(item => item.value === segment.segmentValue)
  return option?.label || segment.segmentValue
}

function addSegment(type) {
  emitRows([...rows.value, createCodeRuleSegment(type, rows.value.length + 1)])
}

function removeSegment(index) {
  emitRows(rows.value.filter((_, current) => current !== index))
}

function normalizeRows() {
  emitRows(rows.value)
}

function toggleExpanded(key) {
  const next = new Set(expandedKeys.value)
  if (next.has(key))
    next.delete(key)
  else
    next.add(key)
  expandedKeys.value = next
}
</script>

<style scoped>
.segment-editor {
  color: var(--text-primary, #1d2129);
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 10px;
  background: var(--bg-primary, #fff);
  overflow: hidden;
}

.segment-editor__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.segment-editor__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 650;
  color: var(--text-primary, #1d2129);
}

.segment-editor__header p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.segment-editor__table {
  overflow-x: auto;
}

.segment-editor__table-head,
.segment-editor__row-main {
  display: grid;
  grid-template-columns: 48px 126px minmax(320px, 1fr) 68px 58px 58px 78px 76px;
  gap: 10px;
  align-items: center;
  min-width: 960px;
}

.segment-editor__table-head {
  padding: 9px 16px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  background: var(--bg-secondary, #f7f8fa);
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.segment-editor__table-head span:nth-last-child(-n + 5) {
  text-align: center;
}

.segment-editor__table-head span:last-child {
  position: sticky;
  right: 0;
  z-index: 3;
  display: flex;
  align-self: stretch;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary, #f7f8fa);
  box-shadow: -10px 0 12px -12px var(--text-tertiary, #86909c);
}

.segment-editor__row {
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-primary, #fff);
}

.segment-editor__row:last-child {
  border-bottom: 0;
}

.segment-editor__row-main {
  padding: 12px 16px;
}

.segment-editor__row--ghost {
  opacity: 0.5;
  background: color-mix(in srgb, var(--primary-color, #4242f7) 10%, var(--bg-primary, #fff));
}

.segment-drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 44px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-tertiary, #f2f3f5);
  cursor: grab;
}

.segment-drag-handle:disabled {
  cursor: default;
}

.segment-drag-handle span {
  min-width: 16px;
  font-size: 12px;
  font-weight: 650;
}

.segment-editor__actions {
  position: sticky;
  right: 0;
  z-index: 2;
  display: flex;
  align-self: stretch;
  align-items: center;
  justify-content: flex-end;
  padding-left: 8px;
  background: var(--bg-primary, #fff);
  box-shadow: -10px 0 12px -12px var(--text-tertiary, #86909c);
}

.segment-editor__value {
  min-width: 0;
}

.segment-editor__variable-value {
  display: grid;
  grid-template-columns: 128px minmax(150px, 1fr);
  gap: 8px;
  min-width: 0;
}

.segment-editor__mapping-summary {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) 18px;
  gap: 7px;
  align-items: center;
  width: 100%;
  min-width: 0;
  height: 34px;
  padding: 3px 8px;
  color: var(--text-primary, #1d2129);
  text-align: left;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  cursor: pointer;
}

.segment-editor__mapping-summary:hover:not(:disabled) {
  color: var(--primary-color, #4242f7);
  border-color: var(--primary-color, #4242f7);
  background: color-mix(in srgb, var(--primary-color, #4242f7) 5%, var(--bg-primary, #fff));
}

.segment-editor__mapping-summary:disabled {
  cursor: default;
  opacity: 0.65;
}

.segment-editor__mapping-summary.is-empty {
  border-style: dashed;
}

.segment-editor__mapping-summary > span {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.segment-editor__mapping-summary strong,
.segment-editor__mapping-summary small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.segment-editor__mapping-summary strong {
  font-size: 12px;
  font-weight: 600;
}

.segment-editor__mapping-summary small {
  color: var(--text-tertiary, #86909c);
  font-size: 10px;
}

.segment-editor__advanced {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 14px;
  padding: 13px 18px 16px 74px;
  border-top: 1px dashed var(--border-light, #e5e6eb);
  background: var(--bg-secondary, #f7f8fa);
}

.advanced-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.advanced-field label {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.advanced-field--switch {
  align-items: flex-start;
}

.segment-editor__empty {
  padding: 34px 0;
}

.segment-editor__tips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  padding: 10px 18px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  background: var(--bg-secondary, #f7f8fa);
  border-top: 1px solid var(--border-light, #e5e6eb);
}

.segment-editor__tips span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

@media (max-width: 900px) {
  .segment-editor__advanced {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }
}
</style>
