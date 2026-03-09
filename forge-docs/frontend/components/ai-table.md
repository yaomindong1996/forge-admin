# AiTable 表格组件

基于 Naive UI 封装的增强型数据表格，支持列设置、密度调整、分页等功能。

## 基础用法

```vue
<template>
  <AiTable
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    @page-change="handlePageChange"
    @refresh="handleRefresh"
  />
</template>

<script setup>
const columns = [
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'age', label: '年龄', width: 80 },
  { prop: 'status', label: '状态', slot: 'status' },
  { prop: 'action', label: '操作', width: 150, fixed: 'right' }
]

const dataSource = ref([])
const loading = ref(false)
const pagination = ref({ page: 1, pageSize: 10, itemCount: 0 })
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| columns | Array | [] | 列配置 |
| dataSource | Array | [] | 数据源 |
| loading | Boolean | false | 加载状态 |
| pagination | Object/Boolean | {} | 分页配置 |
| rowKey | String/Function | 'id' | 行键 |
| hideSelection | Boolean | false | 隐藏多选 |
| striped | Boolean | false | 斑马纹 |
| bordered | Boolean | true | 边框 |
| maxHeight | Number/String | - | 最大高度 |
| scrollX | Number | - | 横向滚动宽度 |
| showToolbar | Boolean | true | 显示工具栏 |
| showRefresh | Boolean | true | 显示刷新按钮 |
| showDensity | Boolean | true | 显示密度调整 |
| showColumnFilter | Boolean | true | 显示列设置 |

## 列配置

| 属性 | 类型 | 说明 |
|------|------|------|
| prop | String | 字段名 |
| label | String | 列标题 |
| width | Number | 列宽 |
| minWidth | Number | 最小宽度 |
| fixed | String | 固定列：left/right |
| align | String | 对齐：left/center/right |
| sortable | Boolean | 是否排序 |
| slot | String | 插槽名 |
| render | Function | 自定义渲染 |
| formatter | Function | 格式化函数 |

## 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| page-change | page | 页码变化 |
| page-size-change | pageSize | 每页条数变化 |
| refresh | - | 刷新 |
| density-change | size | 密度变化 |
| filter-change | columns | 列筛选变化 |

## 方法

```js
// 清除选择
tableRef.value.clearSelection()

// 获取选中行
const rows = tableRef.value.getCheckedRows()

// 设置选中行
tableRef.value.setCheckedKeys([1, 2, 3])
```

## 插槽

```vue
<AiTable :columns="columns">
  <!-- 列插槽 -->
  <template #status="{ row }">
    <n-tag :type="row.status === 1 ? 'success' : 'error'">
      {{ row.status === 1 ? '启用' : '禁用' }}
    </n-tag>
  </template>
  
  <!-- 工具栏左侧 -->
  <template #toolbar-left>
    <n-button type="primary">新增</n-button>
  </template>
</AiTable>
```