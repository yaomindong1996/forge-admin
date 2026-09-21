<script setup>
import { NInput } from 'naive-ui'
import { computed, ref } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { addField, startItemDrag } from './elementCatalog'
import { groupPrintFields } from './fieldGroups'

const store = usePrintDesignerStore()
const query = ref('')
const groups = computed(() => groupPrintFields(store.catalog, query.value))
const total = computed(() => groups.value.reduce((sum, group) => sum + group.fields.length, 0))

function kindLabel(kind) {
  if (kind === 'main')
    return '主表'
  if (kind === 'collection')
    return '子表'
  if (kind === 'flow')
    return '流程'
  return '字段'
}

function onAdd(field) {
  addField(store, field.path)
}
</script>

<template>
  <section class="designer-group field-tree">
    <h3>数据源</h3>
    <p class="muted tip">
      主表字段拖到画布或空白表格单元格；带「明细表」标记的子表/数组请整表拖入，会生成可循环的明细表格。当前共 {{ total }} 项。
    </p>
    <NInput v-model:value="query" size="small" placeholder="搜索字段 / 路径" clearable />
    <p v-if="!groups.length" class="muted empty">
      暂无可用字段，请确认模板已绑定业务对象且含主表/子表目录。
    </p>
    <div v-for="group in groups" :key="group.key" class="field-group" :data-kind="group.kind">
      <div class="group-head">
        <span class="group-badge">{{ kindLabel(group.kind) }}</span>
        <strong>{{ group.title }}</strong>
        <small>{{ group.fields.length }}</small>
      </div>
      <button
        v-for="field in group.fields"
        :key="field.path"
        type="button"
        class="field-row"
        :class="{ collection: field.type === 'COLLECTION' }"
        draggable="true"
        :title="field.type === 'COLLECTION' ? `拖入生成「${field.label || field.path}」明细表` : `拖入或点击添加 ${field.label || field.path}`"
        @dragstart="startItemDrag($event, { field: field.path })"
        @click="onAdd(field)"
      >
        <span class="field-main">
          <span class="field-mark" aria-hidden="true">{{ field.type === 'COLLECTION' ? '▤' : '·' }}</span>
          <span class="field-label">{{ field.label || field.path.split('.').at(-1) }}</span>
          <span v-if="field.type === 'COLLECTION'" class="field-tag">明细表</span>
        </span>
        <small class="field-path">{{ field.path }}</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
.tip,
.empty {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.45;
}
.field-group {
  margin-top: 10px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px;
  overflow: hidden;
  background: var(--bg-primary, #fff);
}
.field-group[data-kind='collection'] {
  border-color: color-mix(in srgb, var(--primary-color) 28%, #e2e8f0);
}
.group-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  background: var(--gray-100, #f6f8fb);
  border-bottom: 1px solid var(--border-light, #e2e8f0);
}
.group-badge {
  flex: none;
  padding: 0 5px;
  border-radius: 3px;
  background: #e2e8f0;
  color: #334155;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
}
.field-group[data-kind='main'] .group-badge {
  background: #dbeafe;
  color: #1d4ed8;
}
.field-group[data-kind='collection'] .group-badge {
  background: color-mix(in srgb, var(--primary-color) 16%, #fff);
  color: var(--primary-color);
}
.field-group[data-kind='flow'] .group-badge {
  background: #fef3c7;
  color: #b45309;
}
.group-head strong {
  flex: 1;
  min-width: 0;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
}
.group-head small {
  color: var(--text-tertiary, #94a3b8);
  font-size: 10px;
}
.field-row {
  display: flex;
  flex-direction: column;
  gap: 1px;
  width: 100%;
  padding: 6px 8px;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--border-light, #eef2f7);
  color: inherit;
  text-align: left;
  cursor: grab;
}
.field-row:last-child {
  border-bottom: 0;
}
.field-row:hover {
  background: color-mix(in srgb, var(--primary-color) 6%, #fff);
}
.field-row.collection {
  background: color-mix(in srgb, var(--primary-color) 4%, #fff);
}
.field-main {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.field-mark {
  flex: none;
  width: 12px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 11px;
}
.field-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
}
.field-tag {
  flex: none;
  margin-left: auto;
  padding: 0 4px;
  border-radius: 3px;
  background: color-mix(in srgb, var(--primary-color) 12%, #fff);
  color: var(--primary-color);
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
}
.field-path {
  padding-left: 16px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 10px;
  overflow-wrap: anywhere;
}
</style>
