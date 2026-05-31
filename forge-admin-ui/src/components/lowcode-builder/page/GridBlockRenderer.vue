<template>
  <div class="grid-block" :class="[`block-${block.blockType}`, { selected }]">
    <!-- 查询表单 -->
    <template v-if="block.blockType === 'search-form'">
      <div class="block-header">
        <strong>{{ block.label || '查询表单' }}</strong>
        <span class="block-meta">{{ resolvedFields.length }} 个查询字段</span>
      </div>
      <div v-if="resolvedFields.length" class="search-grid">
        <n-form-item
          v-for="field in resolvedFields"
          :key="field.field"
          :label="field.label || field.field"
          :show-feedback="false"
          :style="{ textAlign: fieldAlign(field.field) }"
        >
          <n-tree-select
            v-if="['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(componentType(field))"
            :placeholder="`请选择${field.label || field.field}`"
            disabled
            size="small"
          />
          <n-input
            v-else-if="!field.dictType && !['date', 'datetime', 'dictSelect', 'userSelect'].includes(componentType(field))"
            :placeholder="`请输入${field.label || field.field}`"
            disabled
            size="small"
          />
          <n-date-picker
            v-else-if="['date', 'datetime'].includes(componentType(field))"
            :type="componentType(field) === 'datetime' ? 'datetime' : 'date'"
            disabled
            size="small"
            style="width: 100%"
          />
          <n-select
            v-else
            :placeholder="`请选择${field.label || field.field}`"
            disabled
            size="small"
          />
        </n-form-item>
      </div>
      <div v-else class="block-empty">
        点击右侧"配置字段"按钮添加查询字段
      </div>
      <div class="search-actions">
        <n-button size="small" type="primary" disabled>
          查询
        </n-button>
        <n-button size="small" disabled>
          重置
        </n-button>
        <n-button v-if="block.props?.collapsible" text size="small" type="primary" disabled>
          收起
        </n-button>
      </div>
    </template>

    <!-- 操作工具栏 -->
    <template v-else-if="block.blockType === 'toolbar'">
      <div class="toolbar-actions">
        <n-button v-if="hasAction('add')" size="small" type="primary" disabled>
          + 新增
        </n-button>
        <n-button v-if="hasAction('import')" size="small" disabled>
          导入
        </n-button>
        <n-button v-if="hasAction('export')" size="small" disabled>
          导出
        </n-button>
        <n-button v-if="hasAction('batch-delete')" size="small" disabled>
          批量删除
        </n-button>
        <n-button v-if="hasAction('custom-query')" size="small" text type="primary" disabled>
          自定义查询
        </n-button>
        <n-button
          v-for="action in toolbarCustomActions"
          :key="action.key"
          size="small"
          :type="action.type === 'default' ? undefined : action.type"
          disabled
        >
          {{ action.label }}
        </n-button>
        <span v-if="!(block.props?.actions?.length)" class="block-empty inline">
          未启用任何工具按钮
        </span>
      </div>
    </template>

    <!-- 数据列表 -->
    <template v-else-if="block.blockType === 'data-table'">
      <div class="block-header">
        <strong>{{ block.label || '数据列表' }}</strong>
        <span class="block-meta">{{ resolvedFields.length }} 列</span>
      </div>
      <n-data-table
        v-if="resolvedFields.length"
        :columns="tableColumns"
        :data="sampleRows"
        :bordered="false"
        size="small"
        class="block-table"
      />
      <div v-else class="block-empty">
        点击右侧"配置字段"按钮添加列表列
      </div>
    </template>

    <!-- 左侧导航树 -->
    <template v-else-if="block.blockType === 'tree-panel'">
      <div class="tree-title">
        {{ block.props?.treeTitle || '导航树' }}
      </div>
      <div class="tree-node active">
        全部
      </div>
      <div class="tree-node">
        示例节点一
      </div>
      <div class="tree-node">
        示例节点二
      </div>
      <div class="tree-foot">
        显示字段：{{ block.props?.labelField || '未配置' }}
      </div>
    </template>

    <!-- 指标卡片 -->
    <template v-else-if="block.blockType === 'stats-strip'">
      <div class="stats-grid">
        <div
          v-for="(metric, idx) in (block.props?.metrics || [])"
          :key="idx"
          class="stats-card"
        >
          <div class="stats-label">
            {{ metric.label }}
          </div>
          <div class="stats-value">
            {{ metric.value }}
          </div>
          <div v-if="metric.trend" class="stats-trend" :class="trendClass(metric.trend)">
            {{ metric.trend }}
          </div>
        </div>
        <div v-if="!(block.props?.metrics?.length)" class="block-empty">
          点击右侧添加指标项
        </div>
      </div>
    </template>

    <!-- 说明文本 -->
    <template v-else-if="block.blockType === 'custom-html'">
      <div class="custom-html">
        <div v-if="block.props?.title" class="custom-title">
          {{ block.props.title }}
        </div>
        <div class="custom-body">
          {{ block.props?.content || '在右侧填写说明内容' }}
        </div>
      </div>
    </template>

    <!-- 子表 Tab -->
    <template v-else-if="block.blockType === 'sub-table-tabs'">
      <n-tabs type="line" size="small" :default-value="block.props?.tabs?.[0]?.key" class="sub-tabs">
        <n-tab-pane
          v-for="tab in (block.props?.tabs || [])"
          :key="tab.key"
          :name="tab.key"
          :tab="tab.title"
        >
          <div class="sub-tab-empty">
            子表内容由发布运行时联动加载
          </div>
        </n-tab-pane>
      </n-tabs>
    </template>

    <!-- 分组标题 -->
    <template v-else-if="block.blockType === 'section-divider'">
      <div class="section-divider">
        <span class="bar" />
        <span class="divider-title">{{ block.props?.title || '分组标题' }}</span>
        <span class="bar" />
      </div>
    </template>

    <template v-else>
      <div class="block-empty">
        未知区块类型：{{ block.blockType }}
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  block: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selected: {
    type: Boolean,
    default: false,
  },
})

const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const resolvedFields = computed(() => (props.block.fieldRefs || [])
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))

const tableColumns = computed(() => [
  ...resolvedFields.value.slice(0, 8).map(field => ({
    key: field.field,
    title: field.label || field.field,
    minWidth: 96,
    align: fieldAlign(field.field),
    ellipsis: { tooltip: true },
  })),
  { key: '__actions', title: '操作', width: 96, fixed: 'right' },
])
const toolbarCustomActions = computed(() => (props.block.props?.customActions || [])
  .filter(action => (action.position || 'toolbar') === 'toolbar'))

const sampleRows = computed(() => Array.from({ length: 3 }).map((_, idx) => {
  const row = { __actions: '编辑' }
  resolvedFields.value.slice(0, 8).forEach((field) => {
    row[field.field] = sampleValue(field, idx)
  })
  return row
}))

function componentType(field) {
  return field?.componentType || field?.dataType || 'input'
}

function hasAction(key) {
  return Array.isArray(props.block.props?.actions) && props.block.props.actions.includes(key)
}

function fieldAlign(fieldName) {
  const align = props.block.props?.fieldSettings?.[fieldName]?.align
  return ['left', 'center', 'right'].includes(align) ? align : 'left'
}

function trendClass(trend) {
  if (typeof trend !== 'string')
    return ''
  if (trend.startsWith('+'))
    return 'up'
  if (trend.startsWith('-'))
    return 'down'
  return ''
}

function sampleValue(field, idx) {
  if (!field)
    return '-'
  if (field.dictType)
    return '字典'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return idx % 2 === 0 ? '是' : '否'
  if (['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? `${100 + idx}.00` : String(100 + idx)
  if (['date'].includes(field.dataType))
    return '2026-05-21'
  if (['datetime'].includes(field.dataType))
    return '2026-05-21 09:30:00'
  return field.label ? `${field.label}${idx + 1}` : `示例${idx + 1}`
}
</script>

<style scoped>
.grid-block {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.grid-block.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.block-header strong {
  font-size: 13px;
  color: #0f172a;
}

.block-meta {
  font-size: 11px;
  color: #64748b;
}

.block-empty {
  color: #94a3b8;
  font-size: 12px;
  padding: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  text-align: center;
}

.block-empty.inline {
  display: inline-block;
  padding: 4px 8px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px 12px;
}

.search-actions {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 4px;
  border-top: 1px dashed #e5e7eb;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.block-table {
  flex: 1;
  min-height: 0;
}

.tree-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.tree-node {
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #475569;
}

.tree-node.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.tree-foot {
  margin-top: auto;
  font-size: 11px;
  color: #94a3b8;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 8px;
  flex: 1;
}

.stats-card {
  padding: 10px 12px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.stats-label {
  font-size: 11px;
  color: #64748b;
}

.stats-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-top: 2px;
}

.stats-trend {
  font-size: 11px;
  margin-top: 2px;
}

.stats-trend.up {
  color: #16a34a;
}

.stats-trend.down {
  color: #dc2626;
}

.custom-html {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 6px;
}

.custom-title {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}

.custom-body {
  font-size: 12px;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}

.sub-tabs {
  flex: 1;
}

.sub-tab-empty {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.section-divider .bar {
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}

.divider-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
</style>
