# C01 一个 CRUD 页面从 2 天到 10 分钟：AiCrudPage 的协议化设计

> 关键词：低代码 / 协议驱动 / 动态渲染 / 字段三重复用 / 钩子扩展

---

## 一、问题场景：每个 CRUD 页面都是重复劳动

打开你的后台管理系统，数一数有多少个这样的页面：

1. 一个搜索区（几个输入框 + 查询/重置按钮）
2. 一个表格区（列 + 分页 + 操作列"编辑/删除"）
3. 一个弹窗表单（新增/编辑/详情）

这些页面在代码层面长什么样？

```vue
<!-- 搜索区 -->
<template>
  <el-form :model="queryParams" inline>
    <el-form-item label="合同名称">
      <el-input v-model="queryParams.contractName" />
    </el-form-item>
    <el-form-item label="状态">
      <el-select v-model="queryParams.status">
        <el-option label="生效" value="1" />
        <el-option label="作废" value="0" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleQuery">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<!-- 表格区 -->
<el-table :data="list">
  <el-table-column prop="contractName" label="合同名称" />
  <el-table-column prop="status" label="状态" />
  <el-table-column prop="createTime" label="创建时间" />
  <el-table-column label="操作" width="200">
    <template #default="{ row }">
      <el-button size="small" @click="handleEdit(row)">编辑</el-button>
      <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
    </template>
  </el-table-column>
</el-table>

<!-- 表单弹窗 -->
<el-dialog v-model="dialogVisible">
  <el-form :model="formData" :rules="rules">
    <el-form-item label="合同名称" prop="contractName">
      <el-input v-model="formData.contractName" />
    </el-form-item>
    <!-- 还有 10 个字段... -->
  </el-form>
</el-dialog>
```

然后你开始写 JavaScript：`handleQuery`、`resetQuery`、`handleEdit`、`handleDelete`、`handleAdd`、`handleSubmit`、`handleCancel`... 每个页面 200+ 行代码，80% 是重复的。

更糟糕的是，当产品经理说"这个字段要加个字典选择"时，你需要：
1. 前端加 `dictSelect` 组件
2. 后端加 `@DictType` 注解
3. 数据库加字典表记录
4. 联调测试

一个简单的 CRUD 页面，从需求到上线，2 天起步。

Forge Admin 的 `AiCrudPage` 给出的答案是：**用一份 JSON 协议描述整个页面，让组件自动渲染**。开发时间从 2 天压缩到 10 分钟。

---

## 二、解决方案：一份协议，三个区域，全自动渲染

### 2.1 协议总览

`AiCrudPage` 的核心设计是"协议驱动"：

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   modelSchema   │    │   pageSchema    │    │   AiCrudPage    │
│   （数据协议）   │    │   （页面协议）   │    │   （运行时）    │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ - 表名          │    │ - 布局类型      │    │ - 搜索区        │
│ - 字段定义      │────▶│ - 区域配置      │────▶│ - 表格区        │
│ - 字典映射      │    │ - 字段引用      │    │ - 表单区        │
│ - 关联关系      │    │ - 组件属性      │    │ - 操作列        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

后端 `LowcodeRuntimeConfigBuilder` 将两份协议编译为 `AiCrudPage` 的 Props，前端直接渲染。

### 2.2 协议示例：一个合同管理页面

**modelSchema（数据协议）**：

```json
{
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "fields": [
    {
      "field": "contractName",
      "columnName": "contract_name",
      "label": "合同名称",
      "dataType": "varchar",
      "length": 128,
      "required": true,
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "input",
      "queryType": "like"
    },
    {
      "field": "status",
      "columnName": "status",
      "label": "状态",
      "dataType": "tinyint",
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "dictSelect",
      "dictType": "contract_status",
      "queryType": "eq"
    },
    {
      "field": "createTime",
      "columnName": "create_time",
      "label": "创建时间",
      "dataType": "datetime",
      "listVisible": true,
      "formVisible": false,
      "componentType": "datetime",
      "width": 180
    }
  ]
}
```

**pageSchema（页面协议）**：

```json
{
  "layoutType": "simple-crud",
  "zones": [
    {
      "zoneKey": "search",
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status"]
    },
    {
      "zoneKey": "table",
      "componentKey": "data-table",
      "enabled": true,
      "fieldRefs": ["contractName", "status", "createTime"],
      "props": {
        "showImport": true,
        "showExport": true
      }
    },
    {
      "zoneKey": "edit",
      "componentKey": "edit-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status"]
    }
  ]
}
```

**编译后的 AiCrudPage Props**：

```js
{
  api: '/ai/crud/contract',
  apiConfig: {
    list: 'get@/ai/crud/contract/page',
    add: 'post@/ai/crud/contract',
    update: 'put@/ai/crud/contract/:id',
    delete: 'delete@/ai/crud/contract/:id'
  },
  columns: [
    { prop: 'contractName', label: '合同名称' },
    { prop: 'status', label: '状态', 
      render: { type: 'dictTag', dictType: 'contract_status' } },
    { prop: 'createTime', label: '创建时间', width: 180 },
    { prop: 'action', label: '操作', width: 200,
      actions: [
        { label: '编辑', key: 'edit', type: 'primary' },
        { label: '删除', key: 'delete', type: 'error' }
      ]
    }
  ],
  searchSchema: [
    { field: 'contractName', label: '合同名称', type: 'input', queryType: 'like' },
    { field: 'status', label: '状态', type: 'dictSelect', 
      props: { dictType: 'contract_status' }, queryType: 'eq' }
  ],
  editSchema: [
    { field: 'contractName', label: '合同名称', type: 'input', required: true },
    { field: 'status', label: '状态', type: 'dictSelect', 
      props: { dictType: 'contract_status' } }
  ]
}
```

**前端使用**：

```vue
<template>
  <AiCrudPage :config-key="'contract'" />
</template>
```

就一行。搜索、表格、表单、分页、导入导出、操作按钮全部自动生成。

---

## 三、数据结构：字段三重复用与协议分层

### 3.1 字段三重复用：一份定义，三个区域

传统开发中，一个字段需要在三个地方重复定义：

1. **搜索区**：`<el-input v-model="queryParams.contractName" />`
2. **表格区**：`<el-table-column prop="contractName" label="合同名称" />`
3. **表单区**：`<el-form-item label="合同名称"><el-input v-model="formData.contractName" /></el-form-item>`

`modelSchema` 通过三个布尔开关解决这个问题：

```json
{
  "field": "contractName",
  "label": "合同名称",
  "searchable": true,    // 出现在搜索区
  "listVisible": true,   // 出现在表格区
  "formVisible": true    // 出现在表单区
}
```

`pageSchema` 通过 `fieldRefs` 引用这些字段，而不是重新定义。修改字段时（如改 label、加字典），只需改一处，三个区域同步更新。

### 3.2 协议分层：modelSchema 与 pageSchema 分离

为什么分两层？

**modelSchema 回答"数据是什么"**：
- 表名、字段名、数据类型、长度、是否必填
- 字典映射、关联关系、验证规则
- 这是业务模型，与技术无关

**pageSchema 回答"页面长什么样"**：
- 搜索区放哪几个字段
- 表格区显示哪些列，宽度多少
- 表单区用几列布局，字段顺序
- 这是视图配置，与业务解耦

分离的好处：
- 同一份数据模型可以配出多个页面（如"合同列表"和"合同统计"）
- 页面布局可以独立调整，不影响数据模型
- 后端编译时可以做优化：只生成用到的字段配置

### 3.3 组件类型映射：从 dataType 到 componentType

`modelSchema` 的 `dataType`（数据库类型）自动映射为 `componentType`（前端组件）：

| dataType | 默认 componentType | 说明 |
|----------|-------------------|------|
| `varchar` / `char` | `input` | 单行文本输入 |
| `text` | `textarea` | 多行文本 |
| `int` / `bigint` | `number` | 数字输入 |
| `decimal` | `number` | 带小数位的数字 |
| `date` | `date` | 日期选择 |
| `datetime` | `datetime` | 日期时间选择 |
| `time` | `time` | 时间选择 |
| `tinyint` | `switch` | 开关（0/1） |

但你可以覆盖默认映射：

```json
{
  "field": "status",
  "dataType": "tinyint",
  "componentType": "dictSelect",  // 覆盖默认的 switch
  "dictType": "contract_status"
}
```

`dictType` 非空时，组件自动从字典服务获取选项，无需前端硬编码。

---

## 四、实现链路：从协议到页面的完整转换

### 4.1 后端编译：LowcodeRuntimeConfigBuilder

这是协议转换的核心，负责将两份协议编译为 `AiCrudPage` 可消费的 Props：

```java
public class LowcodeRuntimeConfigBuilder {
    public AiCrudConfig buildRuntimeConfig(ModelSchema modelSchema, PageSchema pageSchema) {
        AiCrudConfig config = new AiCrudConfig();
        
        // 1. 生成 API 配置
        config.setApiConfig(buildApiConfig(modelSchema));
        
        // 2. 生成搜索表单配置
        config.setSearchSchema(buildSearchSchema(modelSchema, pageSchema));
        
        // 3. 生成表格列配置
        config.setColumns(buildColumnsSchema(modelSchema, pageSchema));
        
        // 4. 生成编辑表单配置
        config.setEditSchema(buildEditSchema(modelSchema, pageSchema));
        
        // 5. 生成其他选项
        config.setOptions(buildOptions(modelSchema, pageSchema));
        
        return config;
    }
}
```

**关键转换**：

- `searchSchema`：筛选 `searchable=true` 的字段，根据 `queryType` 生成查询条件配置
- `columns`：筛选 `listVisible=true` 的字段，根据 `dictType` 自动添加 `render` 配置
- `editSchema`：筛选 `formVisible=true` 的字段，保持 `modelSchema` 中的验证规则

### 4.2 前端渲染：AiCrudPage.vue 的智能调度

`AiCrudPage` 是一个 2800 行的 Vue 组件，内部拆分为四个子组件：

```
AiCrudPage.vue
├── AiSearch.vue       // 根据 searchSchema 渲染搜索表单
├── AiTable.vue        // 根据 columns 渲染表格（支持 table/card 双模式）
├── AiForm.vue         // 根据 editSchema 渲染表单（Modal/Drawer）
└── AiCustomQuery.vue  // 自定义查询面板
```

**核心渲染流程**：

```js
// 1. 加载配置
const config = await loadConfig(configKey)

// 2. 渲染搜索区
<AiSearch :schema="config.searchSchema" @search="handleSearch" />

// 3. 渲染表格区
<AiTable 
  :columns="config.columns"
  :data="dataSource"
  :pagination="pagination"
  @edit="handleEdit"
  @delete="handleDelete"
/>

// 4. 渲染表单弹窗
<AiForm
  v-model:visible="dialogVisible"
  :schema="config.editSchema"
  :mode="formMode"
  :data="formData"
  @submit="handleSubmit"
/>
```

**智能特性**：

1. **操作列自动折叠**：当操作按钮超过 2 个时，自动折叠到"更多"下拉菜单
2. **字典自动渲染**：`dictType` 字段在表格中显示为标签，在表单中显示为下拉选择
3. **API 占位符替换**：`apiConfig.update` 中的 `:id` 自动替换为当前行主键
4. **主子表支持**：`appType: "MASTER_DETAIL"` 时自动渲染子表 Tab

### 4.3 钩子系统：不修改源码的扩展点

`AiCrudPage` 提供 7 个钩子，覆盖 CRUD 全生命周期：

```js
const hooks = {
  // 列表请求前修改参数
  beforeLoadList: (params) => {
    if (user.isDeptManager) {
      params.deptId = user.deptId  // 自动注入部门过滤
    }
    return params
  },
  
  // 列表数据到达后处理
  beforeRenderList: (list) => {
    return list.map(item => ({
      ...item,
      // 计算字段
      totalAmount: item.quantity * item.unitPrice
    }))
  },
  
  // 表单提交前验证
  beforeSubmit: (formData) => {
    if (formData.amount > 1000000) {
      ElMessage.error('金额超过上限，请审批')
      return false  // 中断提交
    }
    return formData
  }
}
```

钩子通过 `options.hooks` 传入，支持同步和异步函数。这是协议驱动系统的"逃生舱"——当默认行为不满足需求时，无需修改组件源码。

---

## 五、设计取舍：协议化的代价与收益

### 5.1 收益：开发效率的指数级提升

| 传统开发 | 协议驱动 |
|---------|---------|
| 前端：200+ 行 Vue 模板 + 300+ 行 JS | 前端：1 行 `<AiCrudPage />` |
| 后端：Controller + Service + Mapper | 后端：0 行（动态 CRUD 接口） |
| 联调：字段改一处，前后端都要改 | 联调：改 modelSchema，前后端同步生效 |
| 上线：2 天 | 上线：10 分钟 |

实际案例：某客户合同管理系统，12 个 CRUD 页面，传统预估 24 人日，实际用 `AiCrudPage` 2 人日完成。

### 5.2 代价：协议复杂度的上升

协议驱动引入了新的复杂度：

1. **学习成本**：开发者需要理解 `modelSchema` 和 `pageSchema` 的协议格式
2. **调试困难**：页面渲染问题可能源于协议配置错误，需要熟悉协议编译过程
3. **灵活性边界**：协议系统无法覆盖 100% 的定制需求，复杂交互仍需手写代码

我们的应对策略：
- 提供可视化搭建器（下一篇讲），让业务人员拖拽生成协议，无需手写 JSON
- 完善的文档和示例，降低学习曲线
- 钩子系统作为扩展点，平衡"协议覆盖"与"代码自由"

### 5.3 已知限制与改进方向

当前 `AiCrudPage` 的协议设计还有优化空间：

1. **`transConfig` 和 `joinConfig` 的前端自动化不足**：字典/组织树值转换、关联字段 JOIN 需要手动配置 `render`，后续版本计划实现自动推断
2. **协议版本管理**：当前协议缺少 `schemaVersion` 字段，协议变更时的兼容性处理存在风险
3. **性能优化**：大型表格（100+ 列）的协议编译和渲染性能有待优化

---

## 六、二开指南：如何接入你的业务

### 6.1 快速开始

**第一步：定义 modelSchema**

```json
// contract-model.json
{
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "fields": [
    {
      "field": "contractName",
      "columnName": "contract_name",
      "label": "合同名称",
      "dataType": "varchar",
      "length": 128,
      "required": true,
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "input"
    }
    // 更多字段...
  ]
}
```

**第二步：定义 pageSchema**

```json
// contract-page.json
{
  "layoutType": "simple-crud",
  "zones": [
    {
      "zoneKey": "search",
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractName"]
    },
    {
      "zoneKey": "table",
      "componentKey": "data-table",
      "enabled": true,
      "fieldRefs": ["contractName", "createTime"]
    },
    {
      "zoneKey": "edit",
      "componentKey": "edit-form",
      "enabled": true,
      "fieldRefs": ["contractName"]
    }
  ]
}
```

**第三步：发布配置**

```java
// 在 LowcodeAppService 中
lowcodeAppService.publish("contract", modelSchema, pageSchema);
```

**第四步：前端使用**

```vue
<template>
  <AiCrudPage :config-key="'contract'" />
</template>
```

### 6.2 扩展自定义组件

如果内置组件不满足需求，可以注册自定义组件：

```js
// 1. 定义组件
const CustomAmountInput = defineComponent({
  props: ['value', 'fieldConfig'],
  emits: ['update:value'],
  template: `
    <div>
      <el-input-number v-model="localValue" />
      <span>元</span>
    </div>
  `
})

// 2. 注册到 AiCrudPage
AiCrudPage.registerComponent('custom-amount', CustomAmountInput)

// 3. 在 modelSchema 中使用
{
  "field": "amount",
  "componentType": "custom-amount",  // 使用自定义组件
  "props": {
    "min": 0,
    "max": 1000000
  }
}
```

### 6.3 钩子实战：部门数据隔离

```js
// 在业务页面中注入钩子
const deptHook = {
  beforeLoadList: (params) => {
    // 非管理员只能看本部门数据
    if (!user.isAdmin) {
      params.deptId = user.deptId
    }
    return params
  }
}

// 使用
<AiCrudPage 
  :config-key="'contract'"
  :options="{ hooks: deptHook }"
/>
```

---

## 七、体验预告

`AiCrudPage` 的协议化设计将 CRUD 页面开发从"手写每一行代码"升级为"配置一份协议"。对于标准业务场景，开发效率提升 10 倍以上。

但手写 JSON 协议仍然有门槛。下一篇 C02《低代码 CRUD 搭建器如何把"业务字段"变成"可运行页面"？》将介绍可视化搭建器——通过拖拽界面生成协议，让业务人员也能自己搭建页面。

---

> Forge Admin 低代码开发平台：[https://github.com/colin4ge/forge-project](https://github.com/colin4ge/forge-project)
> 系列文章合集：[Forge Admin 技术内幕](https://github.com/colin4ge/forge-project/wiki)
