<template>
  <section class="panel-item form-item-config">
    <div class="compact-config-row">
      <label>编辑打开方式</label>
      <n-select
        :value="schema.layout?.formOpenMode || schema.layout?.modalType || 'modal'"
        :options="formOpenModeOptions"
        size="small"
        @update:value="updateFormOpenModeLayout"
      />
    </div>
    <div class="compact-config-row">
      <label>弹窗宽度</label>
      <n-input
        :value="schema.layout?.modalWidth || '800px'"
        placeholder="800px / 60vw"
        size="small"
        @update:value="updateFormModalWidth"
      />
    </div>
    <div class="compact-config-row">
      <label>抽屉方向</label>
      <n-select
        :value="schema.layout?.drawerPlacement || 'right'"
        :options="drawerPlacementOptions"
        size="small"
        @update:value="updateFormLayout({ drawerPlacement: $event || 'right' })"
      />
    </div>
    <div class="compact-config-row">
      <label>表单列数</label>
      <n-input-number
        class="form-columns-input"
        :value="normalizedFormGridColumns"
        :min="1"
        :max="FORM_LAYOUT_COLUMN_MAX"
        :update-value-on-input="false"
        size="small"
        placeholder="1-12"
        @update:value="commitFormLayoutColumns"
      />
    </div>
    <div class="form-columns-presets-row">
      <span>快捷</span>
      <div class="form-columns-presets">
        <button
          v-for="count in FORM_LAYOUT_COLUMN_PRESETS"
          :key="count"
          type="button"
          :class="{ active: normalizedFormGridColumns === count }"
          @click="commitFormLayoutColumns(count)"
        >
          {{ count }} 列
        </button>
      </div>
    </div>
    <div class="compact-config-row">
      <label>表单大小</label>
      <div class="segmented-mini three form-config-segment">
        <button
          type="button"
          :class="{ active: (schema.layout?.size || 'medium') === 'small' }"
          @click="updateFormLayout({ size: 'small' })"
        >
          小
        </button>
        <button
          type="button"
          :class="{ active: (schema.layout?.size || 'medium') === 'medium' }"
          @click="updateFormLayout({ size: 'medium' })"
        >
          中
        </button>
        <button
          type="button"
          :class="{ active: (schema.layout?.size || 'medium') === 'large' }"
          @click="updateFormLayout({ size: 'large' })"
        >
          大
        </button>
      </div>
    </div>
    <div class="compact-config-row">
      <label>标签位置</label>
      <div class="segmented-mini form-config-segment">
        <button
          type="button"
          :class="{ active: (schema.layout?.labelPlacement || 'left') === 'left' }"
          @click="updateFormLayout({ labelPlacement: 'left' })"
        >
          左侧
        </button>
        <button
          type="button"
          :class="{ active: (schema.layout?.labelPlacement || 'left') === 'top' }"
          @click="updateFormLayout({ labelPlacement: 'top' })"
        >
          顶部
        </button>
      </div>
    </div>
    <div class="compact-config-row">
      <label>标签对齐</label>
      <div class="segmented-mini form-config-segment">
        <button
          type="button"
          :class="{ active: (schema.layout?.labelAlign || 'right') === 'left' }"
          @click="updateFormLayout({ labelAlign: 'left' })"
        >
          左对齐
        </button>
        <button
          type="button"
          :class="{ active: (schema.layout?.labelAlign || 'right') === 'right' }"
          @click="updateFormLayout({ labelAlign: 'right' })"
        >
          右对齐
        </button>
      </div>
    </div>
    <div class="compact-config-row">
      <label>标签宽度</label>
      <div class="label-width-control">
        <n-input
          :value="String(schema.layout?.labelWidth ?? 'auto')"
          placeholder="auto / 100"
          size="small"
          @update:value="updateFormLayout({ labelWidth: normalizeLabelWidthInput($event) })"
        />
        <em>px</em>
      </div>
    </div>
    <div class="compact-config-row">
      <label>行间距</label>
      <n-input-number
        :value="schema.layout?.rowGap || 16"
        :min="0"
        :max="48"
        size="small"
        @update:value="updateFormLayout({ rowGap: $event || 16, yGap: $event || 16 })"
      />
    </div>
    <div class="compact-config-row">
      <label>列间距</label>
      <n-input-number
        :value="schema.layout?.columnGap || 16"
        :min="0"
        :max="48"
        size="small"
        @update:value="updateFormLayout({ columnGap: $event || 16, xGap: $event || 16 })"
      />
    </div>
    <div class="compact-config-row compact-config-row--switch">
      <label>字段过多时折叠</label>
      <n-switch
        size="small"
        :value="!!schema.layout?.enableCollapse"
        @update:value="updateFormLayout({ enableCollapse: $event })"
      />
    </div>
    <div v-if="schema.layout?.enableCollapse" class="compact-config-row">
      <label>默认显示字段数</label>
      <n-input-number
        :value="schema.layout?.maxVisibleFields || 6"
        :min="1"
        :max="50"
        size="small"
        @update:value="updateFormLayout({ maxVisibleFields: $event || 6 })"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useFormDesignerStore } from '@/store'
import {
  FORM_LAYOUT_COLUMN_MAX,
  FORM_LAYOUT_COLUMN_PRESETS,
  normalizeFormLayoutColumns,
  normalizeLabelWidthInput,
} from '../formLayoutConfig'

/**
 * 表单项配置面板（从 ForgePropertyPanel 拆出，Pinia 化改造，AGENTS.md 5.14）
 *
 * 打开方式 / 弹窗宽度 / 栅格列数 / 表单大小 / 标签 / 间距 / 折叠 等表单级 layout 配置。
 */
const designerStore = useFormDesignerStore()

const schema = computed(() => designerStore.schema)

const formOpenModeOptions = [
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
  { label: '平铺', value: 'flat' },
  { label: '多页签', value: 'tabWorkspace' },
]

const drawerPlacementOptions = [
  { label: '右侧', value: 'right' },
  { label: '左侧', value: 'left' },
]

const normalizedFormGridColumns = computed(() => normalizeFormLayoutColumns(schema.value.layout?.gridColumns || 2))

function updateFormLayout(patch = {}) {
  designerStore.updateLayout(patch)
}

/** update-value-on-input=false：输入过程用内部草稿，失焦/回车再提交，避免受控回写顶掉 */
function commitFormLayoutColumns(value) {
  if (value === null || value === undefined || value === '')
    return
  const count = normalizeFormLayoutColumns(value)
  updateFormLayout({ gridColumns: count, gridCols: count })
}

/**
 * 一进设计器：右侧已是「2 列」但字段 span 占满整行时，画布仍像单列，改数字也看不出变化。
 * 检测到根节点普遍占满整行时，按当前列数重跑一次纠正。
 */
onMounted(() => {
  const cols = normalizedFormGridColumns.value
  if (cols <= 1)
    return
  const roots = Array.isArray(schema.value.components) ? schema.value.components : []
  if (!roots.length)
    return
  const allFullRow = roots.every(item => Number(item?.layout?.span || 1) >= cols)
  if (allFullRow)
    commitFormLayoutColumns(cols)
})

function normalizeFormOpenModePatch(value) {
  const formOpenMode = value === 'tabWorkspace' ? 'tabWorkspace' : (['modal', 'drawer', 'flat'].includes(value) ? value : 'modal')
  return {
    formOpenMode,
    modalType: ['modal', 'drawer'].includes(formOpenMode) ? formOpenMode : 'modal',
  }
}

function updateFormOpenModeLayout(value) {
  updateFormLayout(normalizeFormOpenModePatch(value))
}

function updateFormModalWidth(value) {
  const width = value || '800px'
  updateFormLayout({
    modalWidth: width,
    detailModalWidth: width,
  })
}
</script>

<style scoped>
.panel-item {
  display: flex;
  flex-direction: column;
}

.form-item-config {
  display: grid;
  gap: 14px;
}

.compact-config-row {
  display: grid;
  grid-template-columns: minmax(86px, 1fr) 124px;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.compact-config-row--switch > :last-child {
  justify-self: end;
}

.compact-config-row > label {
  color: #646a73;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
}

.form-config-segment {
  width: 124px;
}

.form-config-segment.three,
.form-config-segment:has(button:nth-child(3)) {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.form-columns-input {
  width: 124px;
}

.form-columns-presets-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: -6px;
}

.form-columns-presets-row > span {
  flex: 0 0 auto;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1;
}

.form-columns-presets {
  display: flex;
  flex: 1 1 auto;
  gap: 4px;
  min-width: 0;
}

.form-columns-presets button {
  flex: 1 1 0;
  min-width: 0;
  height: 26px;
  cursor: pointer;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  background: #f7f8fa;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
}

.form-columns-presets button:hover {
  border-color: #c9d4f0;
  color: var(--primary-color, #3370ff);
  background: #f0f5ff;
}

.form-columns-presets button.active {
  border-color: var(--primary-color, #3370ff);
  background: #fff;
  color: var(--primary-color, #3370ff);
}

.label-width-control {
  position: relative;
  min-width: 0;
}

.label-width-control :deep(.n-input__input-el) {
  padding-right: 24px !important;
  text-align: right;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.label-width-control em {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  font-size: 10px;
  font-style: normal;
  pointer-events: none;
}

.segmented-mini {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px;
  width: 100%;
  padding: 2px;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  background: #f2f3f5;
}

.segmented-mini.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.segmented-mini button {
  width: 100%;
  min-width: 0;
  height: 28px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.segmented-mini button:hover {
  color: var(--primary-color, #3370ff);
  background: rgba(255, 255, 255, 0.72);
}

.segmented-mini button.active {
  border-color: transparent;
  background: #fff;
  color: var(--primary-color, #3370ff);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}
</style>
