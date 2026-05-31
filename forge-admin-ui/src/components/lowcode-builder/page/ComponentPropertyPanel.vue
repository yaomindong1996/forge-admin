<template>
  <div class="property-panel">
    <div class="panel-title">
      参数配置
    </div>
    <n-empty v-if="!zone" description="请选择页面区域" />
    <n-form v-else label-placement="top" size="small" class="zone-form">
      <n-form-item label="当前页面">
        <n-input :value="resolveZoneTitle(zone.zoneKey)" disabled />
      </n-form-item>
      <div class="switch-item">
        <span>启用当前页面</span>
        <n-switch
          :value="zone.enabled !== false"
          size="small"
          @update:value="patchZone({ enabled: $event })"
        />
      </div>

      <template v-if="selectedItem">
        <n-divider>选中组件</n-divider>
        <n-form-item label="组件名称">
          <n-input
            :value="selectedItem.label"
            placeholder="请输入组件名称"
            @update:value="patchItem({ label: $event })"
          />
        </n-form-item>
        <n-form-item label="组件类型">
          <n-select
            :value="selectedItem.componentKey"
            :options="componentOptions"
            filterable
            @update:value="patchItem({ componentKey: $event })"
          />
        </n-form-item>
        <n-form-item v-if="!supportsMultiFields" label="绑定字段">
          <n-select
            :value="selectedItem.fieldRef"
            :options="fieldOptions"
            clearable
            filterable
            placeholder="不绑定字段"
            @update:value="updateItemFieldRef"
          />
        </n-form-item>

        <template v-if="supportsMultiFields">
          <n-form-item label="字段选择">
            <div class="field-selector">
              <button
                v-for="field in fields"
                :key="field.field"
                class="field-toggle"
                :class="{ active: selectedFieldRefs.includes(field.field) }"
                type="button"
                @click="toggleFieldRef(field.field)"
              >
                {{ field.label || field.field }}
              </button>
            </div>
          </n-form-item>
          <n-form-item label="字段顺序">
            <draggable
              :model-value="selectedFieldRows"
              item-key="field"
              handle=".order-handle"
              animation="180"
              class="order-list"
              @update:model-value="updateFieldOrder"
            >
              <template #item="{ element }">
                <div class="order-row">
                  <span class="order-handle">☰</span>
                  <span>{{ element.label }}</span>
                  <button type="button" @click="removeFieldRef(element.field)">
                    移除
                  </button>
                </div>
              </template>
            </draggable>
          </n-form-item>
        </template>

        <n-divider>布局</n-divider>
        <div class="layout-grid">
          <n-form-item label="X">
            <n-input-number
              :value="selectedItem.x"
              :min="0"
              size="small"
              @update:value="patchItem({ x: Number($event || 0) })"
            />
          </n-form-item>
          <n-form-item label="Y">
            <n-input-number
              :value="selectedItem.y"
              :min="0"
              size="small"
              @update:value="patchItem({ y: Number($event || 0) })"
            />
          </n-form-item>
          <n-form-item label="宽度">
            <n-input-number
              :value="selectedItem.w"
              :min="48"
              size="small"
              @update:value="patchItem({ w: Number($event || 48) })"
            />
          </n-form-item>
          <n-form-item label="高度">
            <n-input-number
              :value="selectedItem.h"
              :min="32"
              size="small"
              @update:value="patchItem({ h: Number($event || 32) })"
            />
          </n-form-item>
        </div>

        <n-divider>样式与参数</n-divider>
        <n-form-item label="占位提示">
          <n-input
            :value="selectedItem.props?.placeholder"
            placeholder="请输入占位提示"
            @update:value="patchItemProps({ placeholder: $event })"
          />
        </n-form-item>
        <div class="layout-grid">
          <n-form-item label="标签宽度">
            <n-input-number
              :value="selectedItem.style?.labelWidth || 86"
              :min="40"
              :max="180"
              size="small"
              @update:value="patchItemStyle({ labelWidth: Number($event || 86) })"
            />
          </n-form-item>
          <n-form-item label="内容对齐">
            <n-select
              :value="selectedItem.style?.textAlign || 'left'"
              :options="alignOptions"
              size="small"
              @update:value="patchItemStyle({ textAlign: $event || 'left' })"
            />
          </n-form-item>
        </div>
        <div class="layout-grid">
          <n-form-item label="圆角">
            <n-input-number
              :value="selectedItem.style?.radius || 6"
              :min="0"
              :max="24"
              size="small"
              @update:value="patchItemStyle({ radius: Number($event || 0) })"
            />
          </n-form-item>
        </div>
        <div class="layout-grid">
          <n-form-item label="填充色">
            <n-color-picker
              :value="selectedItem.style?.fill || '#ffffff'"
              :show-alpha="false"
              @update:value="patchItemStyle({ fill: $event })"
            />
          </n-form-item>
          <n-form-item label="边框色">
            <n-color-picker
              :value="selectedItem.style?.stroke || '#cbd5e1'"
              :show-alpha="false"
              @update:value="patchItemStyle({ stroke: $event })"
            />
          </n-form-item>
        </div>
        <div class="switch-stack">
          <div class="switch-item">
            <span>锁定位置</span>
            <n-switch
              :value="!!selectedItem.locked"
              size="small"
              @update:value="patchItem({ locked: $event })"
            />
          </div>
        </div>
        <n-button type="error" secondary block @click="$emit('removeItem', selectedItem.id)">
          删除组件
        </n-button>
      </template>
      <n-empty v-else description="请选择 Canvas 上的组件" class="item-empty" />

      <template v-if="zone.zoneKey === 'table'">
        <n-divider>列表能力</n-divider>
        <div class="switch-stack">
          <div class="switch-item">
            <span>批量导入</span>
            <n-switch
              :value="zone.props?.showImport"
              size="small"
              @update:value="updateZoneProp('showImport', $event)"
            />
          </div>
          <div class="switch-item">
            <span>数据导出</span>
            <n-switch
              :value="zone.props?.showExport"
              size="small"
              @update:value="updateZoneProp('showExport', $event)"
            />
          </div>
          <div class="switch-item">
            <span>隐藏批量删除</span>
            <n-switch
              :value="zone.props?.hideBatchDelete"
              size="small"
              @update:value="updateZoneProp('hideBatchDelete', $event)"
            />
          </div>
          <div class="switch-item">
            <span>自定义查询</span>
            <n-switch
              :value="zone.props?.enableCustomQuery"
              size="small"
              @update:value="updateZoneProp('enableCustomQuery', $event)"
            />
          </div>
        </div>

        <template v-if="layoutType === 'tree-crud'">
          <n-divider>树形导航</n-divider>
          <n-form-item label="树标题">
            <n-input
              :value="treeConfig.treeTitle"
              placeholder="例如：组织架构"
              @update:value="updateTreeConfig('treeTitle', $event)"
            />
          </n-form-item>
          <n-form-item label="父级字段">
            <n-select
              :value="treeConfig.parentField"
              :options="fieldOptions"
              placeholder="请选择父级字段"
              @update:value="updateTreeConfig('parentField', $event)"
            />
          </n-form-item>
          <n-form-item label="显示字段">
            <n-select
              :value="treeConfig.labelField"
              :options="fieldOptions"
              placeholder="请选择树节点显示字段"
              @update:value="updateTreeConfig('labelField', $event)"
            />
          </n-form-item>
          <n-form-item label="加载方式">
            <n-select
              :value="treeConfig.loadMode || 'full'"
              :options="treeLoadModeOptions"
              @update:value="updateTreeConfig('loadMode', $event)"
            />
          </n-form-item>
        </template>
      </template>
    </n-form>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import draggable from 'vuedraggable'
import { canvasComponentCatalog, resolveDefaultFieldComponentKey, resolveZoneTitle } from './page-schema'

const props = defineProps({
  zone: {
    type: Object,
    default: null,
  },
  selectedItem: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
})

const emit = defineEmits(['update:zone', 'updateItem', 'removeItem'])

const treeLoadModeOptions = [
  { label: '全量加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]
const alignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]

const fieldOptions = computed(() => props.fields.map(field => ({
  label: field.label ? `${field.label}（${field.field}）` : field.field,
  value: field.field,
})))

const componentOptions = computed(() => {
  return canvasComponentCatalog
    .filter(item => !item.zones?.length || item.zones.includes(props.zone?.zoneKey))
    .map(item => ({
      label: `${item.title}（${item.componentKey}）`,
      value: item.componentKey,
    }))
})

const supportsMultiFields = computed(() => {
  return ['query-set', 'data-table'].includes(props.selectedItem?.componentKey)
})

const selectedFieldRefs = computed(() => {
  if (!props.selectedItem)
    return []
  return props.selectedItem.fieldRefs?.length
    ? props.selectedItem.fieldRefs
    : props.selectedItem.props?.fieldRefs || []
})

const selectedFieldRows = computed(() => {
  const fieldMap = new Map(props.fields.map(field => [field.field, field]))
  return selectedFieldRefs.value.map((ref) => {
    const field = fieldMap.get(ref)
    return {
      field: ref,
      label: field?.label || ref,
    }
  })
})

const treeConfig = computed(() => {
  return props.zone?.props?.treeConfig || {}
})

watch(
  () => [props.layoutType, props.zone?.zoneKey, props.fields.length],
  () => ensureTreeConfig(),
  { immediate: true },
)

function ensureTreeConfig() {
  if (props.layoutType !== 'tree-crud' || props.zone?.zoneKey !== 'table')
    return
  if (!props.zone.props?.treeConfig) {
    const parentField = props.fields.find(field => field.field === 'parentId')?.field
      || props.fields.find(field => field.field === 'pid')?.field
      || ''
    const labelField = props.fields.find(field => field.field === 'name')?.field
      || props.fields[0]?.field
      || ''
    patchZone({
      props: {
        treeConfig: {
          enabled: true,
          keyField: 'id',
          parentField,
          labelField,
          childrenField: 'children',
          treeTitle: '树形导航',
          loadMode: 'full',
        },
      },
    })
  }
}

function updateItemFieldRef(value) {
  const field = props.fields.find(item => item.field === value)
  patchItem({
    fieldRef: value || '',
    label: field?.label || props.selectedItem?.label || '',
    componentKey: field ? resolveDefaultFieldComponentKey(field, props.zone?.zoneKey) : props.selectedItem?.componentKey,
  })
}

function toggleFieldRef(fieldRef) {
  const refs = selectedFieldRefs.value.includes(fieldRef)
    ? selectedFieldRefs.value.filter(ref => ref !== fieldRef)
    : [...selectedFieldRefs.value, fieldRef]
  updateSelectedFieldRefs(refs)
}

function removeFieldRef(fieldRef) {
  updateSelectedFieldRefs(selectedFieldRefs.value.filter(ref => ref !== fieldRef))
}

function updateFieldOrder(rows) {
  updateSelectedFieldRefs(rows.map(row => row.field))
}

function updateSelectedFieldRefs(refs) {
  patchItem({
    fieldRefs: refs,
    props: {
      fieldRefs: refs,
    },
  })
}

function updateZoneProp(key, value) {
  patchZone({ props: { [key]: value } })
}

function updateTreeConfig(key, value) {
  patchZone({
    props: {
      treeConfig: {
        ...(treeConfig.value || {}),
        enabled: true,
        [key]: value,
      },
    },
  })
}

function patchItemStyle(style) {
  patchItem({ style })
}

function patchItemProps(itemProps) {
  patchItem({ props: itemProps })
}

function patchItem(patch) {
  if (!props.selectedItem)
    return
  emit('updateItem', {
    ...props.selectedItem,
    ...patch,
    style: patch.style ? { ...(props.selectedItem.style || {}), ...patch.style } : props.selectedItem.style || {},
    props: patch.props ? { ...(props.selectedItem.props || {}), ...patch.props } : props.selectedItem.props || {},
  })
}

function patchZone(patch) {
  if (!props.zone)
    return
  emit('update:zone', {
    ...props.zone,
    ...patch,
    props: patch.props ? { ...(props.zone.props || {}), ...patch.props } : props.zone.props || {},
  })
}
</script>

<style scoped>
.property-panel {
  height: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-title {
  height: 42px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.zone-form {
  padding: 14px;
  max-height: calc(100% - 42px);
  overflow: auto;
}

.item-empty {
  margin: 14px 0;
}

.layout-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.field-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.field-toggle {
  max-width: 100%;
  padding: 4px 8px;
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
}

.field-toggle.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.order-list,
.switch-stack {
  display: grid;
  gap: 8px;
}

.order-row {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #475569;
}

.order-row span:nth-child(2) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-row button {
  border: 0;
  background: transparent;
  color: #dc2626;
  cursor: pointer;
  font-size: 12px;
}

.order-handle {
  color: #94a3b8;
  cursor: grab;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #475569;
}
</style>
