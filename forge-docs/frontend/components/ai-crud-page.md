# AiCrudPage CRUD 页面组件

完整的 CRUD 页面解决方案，集成搜索、表格、新增、编辑、删除、导入导出功能。

## 基础用法

```vue
<template>
  <AiCrudPage
    :api="/api/user"
    :columns="columns"
    :search-schema="searchSchema"
    :edit-schema="editSchema"
    :api-config="apiConfig"
  />
</template>

<script setup>
const columns = [
  { prop: 'username', label: '用户名' },
  { prop: 'nickname', label: '昵称' },
  { prop: 'status', label: '状态', slot: 'status' }
]

const searchSchema = [
  { field: 'username', label: '用户名', type: 'input' },
  { field: 'status', label: '状态', type: 'select', options: [...] }
]

const editSchema = [
  { field: 'username', label: '用户名', type: 'input', required: true },
  { field: 'nickname', label: '昵称', type: 'input' },
  { field: 'status', label: '状态', type: 'select', options: [...] }
]

// API 配置（可选，默认使用 RESTful 风格）
const apiConfig = {
  list: 'get@/api/user/list',      // 列表接口
  add: 'post@/api/user',           // 新增接口
  update: 'put@/api/user',         // 更新接口
  delete: 'delete@/api/user'       // 删除接口
}
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| api | String | - | 基础 API 地址 |
| apiConfig | Object | {} | API 配置 |
| columns | Array | [] | 表格列配置 |
| searchSchema | Array | [] | 搜索表单配置 |
| editSchema | Array | [] | 编辑表单配置 |
| rowKey | String | 'id' | 主键字段 |
| showSearch | Boolean | true | 显示搜索 |
| showPagination | Boolean | true | 显示分页 |
| hideAdd | Boolean | false | 隐藏新增按钮 |
| hideToolbar | Boolean | false | 隐藏工具栏 |
| modalType | String | 'modal' | 弹窗类型：modal/drawer |
| modalWidth | String | '600px' | 弹窗宽度 |
| showImport | Boolean | false | 显示导入按钮 |
| showExport | Boolean | false | 显示导出按钮 |

## API 配置格式

```js
const apiConfig = {
  list: 'get@/api/user/list',       // get/post 请求列表
  add: 'post@/api/user',            // 新增
  create: 'post@/api/user/create',  // 新增（备选）
  update: 'put@/api/user',          // 更新
  delete: 'delete@/api/user',       // 删除
  detail: 'get@/api/user/detail'    // 详情
}
```

## 钩子函数

```vue
<AiCrudPage
  :before-search="beforeSearch"
  :before-submit="beforeSubmit"
  :before-render-list="beforeRenderList"
  :before-render-form="beforeRenderForm"
/>
```

| 钩子 | 参数 | 说明 |
|------|------|------|
| beforeSearch | params | 搜索前处理参数 |
| beforeSubmit | data | 提交前处理数据 |
| beforeRenderList | list | 渲染列表前处理数据 |
| beforeRenderForm | row | 渲染表单前处理数据 |
| beforeDelete | rows | 删除前确认 |

## 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| load-list-success | { list, total } | 列表加载成功 |
| submit-success | { data, response } | 提交成功 |
| selection-change | { keys, rows } | 选中变化 |
| modal-open | { status, row } | 弹窗打开 |
| modal-close | - | 弹窗关闭 |

## 插槽

```vue
<AiCrudPage>
  <!-- 工具栏 -->
  <template #toolbar-start>
    <n-button>自定义按钮</n-button>
  </template>
  
  <!-- 表格列插槽 -->
  <template #table-status="{ row }">
    <DictTag dict-type="status" :value="row.status" />
  </template>
  
  <!-- 表单插槽 -->
  <template #form-customField="{ field, value }">
    <custom-input v-model:value="formData.customField" />
  </template>
</AiCrudPage>
```