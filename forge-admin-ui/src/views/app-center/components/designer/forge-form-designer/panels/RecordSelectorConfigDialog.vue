<template>
  <n-modal
    :show="show"
    preset="card"
    title="选择器设置"
    :style="{ width: 'min(640px, calc(100vw - 32px))' }"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <!-- 选择方式 -->
    <div class="selector-section">
      <label class="section-label">选择方式</label>
      <n-radio-group :value="draftMultiple ? 'multi' : 'single'" size="small" @update:value="draftMultiple = $event === 'multi'">
        <n-radio-button value="single">
          单选
        </n-radio-button>
        <n-radio-button value="multi">
          多选
        </n-radio-button>
      </n-radio-group>
    </div>

    <!-- 展示列 -->
    <div class="selector-section">
      <div class="section-label-row">
        <label class="section-label">展示列</label>
        <n-checkbox
          :checked="allDisplaySelected"
          :indeterminate="someDisplaySelected"
          size="small"
          @update:checked="toggleAllDisplay"
        >
          全选
        </n-checkbox>
      </div>
      <n-spin :show="loading" size="small">
        <n-checkbox-group
          v-if="fieldOptions.length"
          v-model:value="draftDisplayFields"
          class="field-checkbox-grid"
        >
          <n-checkbox
            v-for="field in fieldOptions"
            :key="field.value"
            :value="field.value"
            size="small"
          >
            {{ field.label }}
          </n-checkbox>
        </n-checkbox-group>
        <n-empty
          v-else
          size="small"
          description="未读取到字段"
        />
      </n-spin>
    </div>

    <!-- 搜索字段 -->
    <div class="selector-section">
      <label class="section-label">搜索字段</label>
      <n-select
        v-model:value="draftKeywordFields"
        :options="fieldOptions"
        size="small"
        filterable
        clearable
        multiple
        placeholder="选择关键字模糊搜索的字段"
      />
      <div class="section-hint">
        用户在搜索框输入时，会在这些字段中模糊匹配
      </div>
    </div>

    <!-- 过滤参数 -->
    <div class="selector-section">
      <div class="section-label-row">
        <label class="section-label">过滤参数</label>
        <a v-if="!draftFilters.length" class="filter-add-link" @click="addFilter()">+ 添加</a>
      </div>
      <div v-if="draftFilters.length" class="filter-list">
        <div v-for="(row, index) in draftFilters" :key="index" class="filter-card">
          <div class="filter-card-head">
            <n-select
              :value="row.fieldCode || null"
              :options="fieldOptions"
              size="small"
              filterable
              placeholder="选择筛选字段"
              class="filter-field-select"
              @update:value="updateFilterField(index, $event)"
            />
            <n-button size="tiny" quaternary type="error" @click="removeFilter(index)">
              删除
            </n-button>
          </div>
          <div class="filter-card-default">
            <span class="filter-default-label">默认值</span>
            <n-select
              :value="row.defaultType || 'none'"
              :options="defaultValueTypeOptions"
              size="small"
              class="filter-type-select"
              @update:value="updateFilter(index, { defaultType: $event, defaultValue: '', formField: '' })"
            />
            <n-input
              v-if="row.defaultType === 'fixed' && !getFieldDictType(row.fieldCode)"
              :value="row.defaultValue || ''"
              size="small"
              placeholder="输入固定值"
              class="filter-default-input"
              @update:value="updateFilter(index, { defaultValue: $event })"
            />
            <DictSelect
              v-else-if="row.defaultType === 'fixed' && getFieldDictType(row.fieldCode)"
              :value="row.defaultValue || ''"
              :dict-type="getFieldDictType(row.fieldCode)"
              size="small"
              clearable
              class="filter-default-input"
              @update:value="updateFilter(index, { defaultValue: $event })"
            />
            <n-select
              v-else-if="row.defaultType === 'form'"
              :value="row.formField || null"
              :options="mainFieldOptions"
              size="small"
              filterable
              placeholder="选择当前表单字段"
              class="filter-default-input"
              @update:value="updateFilter(index, { formField: $event })"
            />
          </div>
        </div>
        <a class="filter-add-link" @click="addFilter()">+ 添加过滤参数</a>
      </div>
      <div v-else class="section-hint">
        配置选择器弹窗顶部的筛选条件，可带默认值联动当前表单字段
      </div>
    </div>

    <!-- 字段映射 -->
    <div class="selector-section">
      <div class="section-label-row">
        <label class="section-label">字段映射</label>
        <a v-if="!draftMappings.length" class="filter-add-link" @click="addMapping()">+ 添加</a>
      </div>
      <div v-if="draftMappings.length" class="filter-list">
        <div v-for="(row, index) in draftMappings" :key="index" class="filter-card">
          <div class="filter-card-head">
            <n-select
              :value="row.source || null"
              :options="fieldOptions"
              size="small"
              filterable
              placeholder="源字段（目标对象）"
              class="filter-field-select"
              @update:value="updateMapping(index, { source: $event })"
            />
            <span class="mapping-arrow">→</span>
            <n-select
              :value="row.target || null"
              :options="mainFieldOptions"
              size="small"
              filterable
              placeholder="目标字段（当前表单）"
              class="filter-field-select"
              @update:value="updateMapping(index, { target: $event })"
            />
            <n-button size="tiny" quaternary type="error" @click="removeMapping(index)">
              删除
            </n-button>
          </div>
        </div>
        <a class="filter-add-link" @click="addMapping()">+ 添加字段映射</a>
      </div>
      <div v-else class="section-hint">
        选择记录后，自动将目标对象的字段值填充到当前表单
      </div>
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
import DictSelect from '@/components/DictSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  /** 目标对象字段选项 [{ label, value }] */
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  /** 当前表单字段选项 [{ label, value }] */
  mainFieldOptions: {
    type: Array,
    default: () => [],
  },
  /** 搜索字段配置 [fieldCode, ...] */
  keywordFields: {
    type: Array,
    default: () => [],
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  displayFields: {
    type: Array,
    default: () => [],
  },
  /** 过滤参数配置 [{ fieldCode, defaultType, defaultValue, formField }] */
  filters: {
    type: Array,
    default: () => [],
  },
  /** 字段映射配置 [{ source, target }] */
  mappings: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'confirm'])

const draftKeywordFields = ref([])
const draftMultiple = ref(false)
const draftDisplayFields = ref([])
const draftFilters = ref([])
const draftMappings = ref([])

const defaultValueTypeOptions = [
  { label: '不设默认', value: 'none' },
  { label: '固定值', value: 'fixed' },
  { label: '表单字段', value: 'form' },
]

/** 字段元数据查找表：fieldCode → { dictType, componentType, fieldType } */
const fieldMetaMap = computed(() => {
  const map = {}
  props.fieldOptions.forEach((opt) => {
    if (opt.value && opt.field) {
      map[opt.value] = {
        dictType: opt.field.dictType || '',
        componentType: opt.field.componentType || '',
        fieldType: opt.field.fieldType || '',
      }
    }
  })
  return map
})

function getFieldDictType(fieldCode) {
  return fieldMetaMap.value[fieldCode]?.dictType || ''
}

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      // 构建 fieldCode → label 查找表，补全旧配置中缺失的 fieldLabel
      const labelMap = {}
      props.fieldOptions.forEach((opt) => {
        if (opt.value)
          labelMap[opt.value] = opt.label || opt.value
      })
      draftKeywordFields.value = [...props.keywordFields]
      draftMultiple.value = props.multiple === true
      draftDisplayFields.value = [...props.displayFields]
      draftFilters.value = (props.filters || []).map((f) => {
        const meta = fieldMetaMap.value[f.fieldCode] || {}
        return {
          ...f,
          fieldLabel: f.fieldLabel || labelMap[f.fieldCode] || '',
          // 补全旧配置中缺失的字段元数据
          dictType: f.dictType || meta.dictType || '',
          componentType: f.componentType || meta.componentType || '',
          fieldType: f.fieldType || meta.fieldType || '',
        }
      })
      draftMappings.value = (props.mappings || []).map(m => ({ ...m }))
    }
  },
)

const allDisplaySelected = computed(() =>
  Boolean(props.fieldOptions.length) && draftDisplayFields.value.length === props.fieldOptions.length,
)
const someDisplaySelected = computed(() =>
  draftDisplayFields.value.length > 0 && draftDisplayFields.value.length < props.fieldOptions.length,
)

function toggleAllDisplay(checked) {
  draftDisplayFields.value = checked ? props.fieldOptions.map(o => o.value).filter(Boolean) : []
}

function addFilter() {
  draftFilters.value.push({ fieldCode: '', defaultType: 'none', defaultValue: '', formField: '' })
}

function removeFilter(index) {
  draftFilters.value.splice(index, 1)
}

function updateFilterField(index, fieldCode) {
  const field = props.fieldOptions.find(f => f.value === fieldCode)
  const patch = { fieldCode: fieldCode || '', fieldLabel: field ? field.label : fieldCode }
  // 保存字段元数据，供运行时搜索表单类型感知渲染
  if (field?.field) {
    patch.dictType = field.field.dictType || ''
    patch.componentType = field.field.componentType || ''
    patch.fieldType = field.field.fieldType || ''
  }
  Object.assign(draftFilters.value[index], patch)
}

function updateFilter(index, patch) {
  Object.assign(draftFilters.value[index], patch)
}

function addMapping() {
  draftMappings.value.push({ source: '', target: '' })
}

function removeMapping(index) {
  draftMappings.value.splice(index, 1)
}

function updateMapping(index, patch) {
  Object.assign(draftMappings.value[index], patch)
}

function confirm() {
  // 构建 fieldCode → label 查找表，确保 emit 时 fieldLabel 不为空
  const labelMap = {}
  props.fieldOptions.forEach((opt) => {
    if (opt.value)
      labelMap[opt.value] = opt.label || opt.value
  })
  const filters = draftFilters.value
    .filter(f => f.fieldCode)
    .map((f) => {
      const meta = fieldMetaMap.value[f.fieldCode] || {}
      return {
        ...f,
        fieldLabel: f.fieldLabel || labelMap[f.fieldCode] || '',
        // 保留字段元数据，供运行时渲染对应组件
        dictType: f.dictType || meta.dictType || '',
        componentType: f.componentType || meta.componentType || '',
        fieldType: f.fieldType || meta.fieldType || '',
      }
    })
  emit('confirm', {
    multiple: draftMultiple.value,
    keywordFields: [...draftKeywordFields.value],
    displayFields: [...draftDisplayFields.value],
    filters,
    mappings: draftMappings.value.filter(m => m.source && m.target),
  })
  emit('update:show', false)
}
</script>

<style scoped>
.selector-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.selector-section:last-of-type {
  margin-bottom: 0;
}

.section-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary, #4b5563);
  line-height: 1.4;
}

.section-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-hint {
  font-size: 11px;
  color: var(--text-tertiary, #86909c);
  line-height: 1.5;
}

.field-checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 10px;
  max-height: 220px;
  overflow-y: auto;
  padding: 6px 8px;
  border: 1px solid #ececf0;
  border-radius: 6px;
  background: var(--bg-primary);
}

.filter-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border: 1px solid #ececf0;
  border-radius: 6px;
  background: var(--bg-primary);
}

.filter-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-field-select {
  flex: 1;
  min-width: 0;
}

.mapping-arrow {
  color: var(--text-tertiary);
  font-size: 16px;
  flex-shrink: 0;
}

.filter-card-default {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 2px;
}

.filter-default-label {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-tertiary, #86909c);
}

.filter-type-select {
  width: 96px;
  flex-shrink: 0;
}

.filter-default-input {
  flex: 1;
  min-width: 0;
}

.filter-add-link {
  font-size: 12px;
  color: var(--primary-color, #2080f0);
  cursor: pointer;
  user-select: none;
  line-height: 1.6;
}

.filter-add-link:hover {
  opacity: 0.8;
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
