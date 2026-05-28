# AiCrudPage 协议化设计深度分析报告

> 分析对象：`AiCrudPage` 组件及低代码 CRUD 搭建器  
> 分析日期：2026-05-28  
> 文档版本：v1.0

---

## 一、概述

`AiCrudPage` 是 Forge 平台前端低代码体系中的核心运行时组件，其设计目标是：**用一份 JSON 协议驱动一个完整的 CRUD 页面**（含搜索、表格、表单、导入导出等）。

低代码搭建器（`lowcode-builder`）通过可视化拖拽生成两份协议：
- `modelSchema`：描述业务数据模型（表名、字段、类型、字典等）
- `pageSchema`：描述页面布局（搜索区、表格区、表单区、工具栏）

发布时，后端 `LowcodeRuntimeConfigBuilder` 将这两份协议编译为 `AiCrudPage` 的 Props 配置，前端运行时直接渲染出完整业务页面。

---

## 二、协议设计

### 2.1 协议总览

```
用户搭建（可视化）
    │
    ▼
modelSchema + pageSchema（JSON 协议）
    │
    ▼
LowcodeRuntimeConfigBuilder（后端编译）
    │
    ▼
AiCrudPage Props（运行时配置）
    │
    ▼
AiCrudPage.vue（渲染）
```

### 2.2 modelSchema 协议

描述"数据是什么"。

```jsonc
{
  "appType": "SINGLE | TREE | MASTER_DETAIL",
  "tableMode": "CREATE | BIND",
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "treeConfig": {
    // 仅 TREE 类型需要
    "keyField": "id",
    "parentField": "parentId",
    "labelField": "name",
    "childrenField": "children",
    "treeTitle": "树形导航"
  },
  "fields": [
    {
      "field": "contractName",       // 业务对象属性名
      "columnName": "contract_name", // 数据库列名
      "label": "合同名称",
      "dataType": "varchar",
      "length": 128,
      "required": true,
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "input",    // 表单组件类型
      "queryType": "like",          // 查询方式
      "dictType": "contract_type",  // 字典类型（可选）
      "relationDisplay": "name",    // 关联显示字段（可选）
      "valueType": "string | number"
    }
  ]
}
```

**关键设计点**：

| 字段 | 作用 |
|------|------|
| `field` / `columnName` | 业务层与数据库层的双向映射 |
| `componentType` | 决定表单使用 input/select/treeSelect 等组件 |
| `searchable` / `listVisible` / `formVisible` | 字段在三个区域的出现控制 |
| `queryType` | `like` / `eq` / `between` 等，决定搜索条件拼接方式 |
| `dictType` | 非空时，表单和表格自动使用字典组件渲染 |
| `relationDisplay` | 关联模型字段，在列表渲染时自动 JOIN 显示名称 |

### 2.3 pageSchema 协议

描述"页面长什么样"。

```jsonc
{
  "layoutType": "simple-crud | tree-crud | master-detail",
  "zones": [
    {
      "zoneKey": "search",         // 区域标识
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status"]  // 引用 modelSchema.fields
    },
    {
      "zoneKey": "table",
      "componentKey": "data-table",
      "enabled": true,
      "fieldRefs": ["contractName", "status", "createTime"],
      "props": {
        "showImport": true,
        "showExport": true,
        "hideBatchDelete": false
      }
    },
    {
      "zoneKey": "form",
      "componentKey": "edit-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status", "remark"]
    }
  ]
}
```

**关键设计点**：

- `zoneKey` 与 `AiCrudPage` 的 Props 区域一一对应
- `fieldRefs` 是引用而非拷贝，保证字段修改时只需改 `modelSchema`
- `props` 直接映射为 `AiCrudPage` 对应区域的配置 Props

### 2.4 运行时编译输出（AiCrudPage Props）

后端 `LowcodeRuntimeConfigBuilder` 编译后输出的配置结构：

```jsonc
{
  "api": "/ai/crud/contract",
  "apiConfig": {
    "list": "get@/ai/crud/contract/page",
    "add": "post@/ai/crud/contract",
    "update": "put@/ai/crud/contract/:id",
    "delete": "delete@/ai/crud/contract/:id",
    "detail": "get@/ai/crud/contract/:id",
    "export": "post@/ai/crud/contract/export"
  },
  "columns": [ /* 由 modelSchema.fields + pageSchema.zones 编译 */ ],
  "searchSchema": [ /* 由 searchable=true 的字段编译 */ ],
  "editSchema": [ /* 由 formVisible=true 的字段编译 */ ],
  "rowKey": "id",
  "showPagination": true,
  "showImport": true,
  "showExport": true,
  "treeConfig": { /* TREE 类型时生成 */ },
  "transConfig": { /* 字典/组织树/关联字段的值转换配置 */ },
  "joinConfig": [ /* 关联模型 JOIN 配置 */ ]
}
```

---

## 三、AiCrudPage.vue 实现原理

### 3.1 组件架构

```
AiCrudPage.vue
├── AiSearch.vue       // 搜索表单区
├── AiTable.vue        // 数据表格区（支持 table/card 双模式）
├── AiForm.vue         // 新增/编辑/详情表单（Modal/Drawer）
├── AiCustomQuery.vue  // 自定义查询面板
└── ChildTableEditor.vue // 主子表子表编辑
```

### 3.2 核心渲染流程

```
mounted
  └─ loadList()
       ├─ beforeLoadList(props)      // 钩子：修改请求参数
       ├─ parseApiConfig('list', ...) // 解析 API 配置，替换 :id 占位符
       ├─ request(method, url, params)
       ├─ extract list + total
       ├─ beforeRenderList(list)      // 钩子：修改列表数据
       └─ dataSource.value = list     // 驱动 AiTable 重新渲染

用户点击"新增"
  └─ handleAdd()
       ├─ modalStatus = 'add'
       ├─ beforeRenderForm(null)      // 钩子：返回初始值
       ├─ formData.value = { ...defaults, ...hookResult }
       └─ modalVisible = true

用户点击"编辑"
  └─ handleEdit(row)
       ├─ modalStatus = 'edit'
       ├─ loadDetailOnEdit ? 调详情接口 : 直接用 row 数据
       ├─ beforeRenderDetail(row)      // 钩子：修改回显数据
       ├─ normalizeEditData(data)      // 类型归一化
       └─ modalVisible = true

用户点击"确定"
  └─ handleModalConfirm()
       ├─ formRef.validate()
       ├─ beforeSubmit(formData)       // 钩子：修改提交数据
       ├─ buildMasterDetailSubmitData() // 主子表打包
       ├─ parseApiConfig('update'|'add', ...)
       ├─ request(method, url, data)
       └─ loadList()
```

### 3.3 协议驱动的关键机制

#### 3.3.1 搜索表单自动生成

`searchSchema` 是一个 FieldSchema 数组，每个元素描述一个搜索字段：

```js
const searchSchema = [
  { field: 'contractName', label: '合同名称', type: 'input', queryType: 'like' },
  { field: 'status', label: '状态', type: 'select',
    props: { options: [{ label: '生效', value: '1' }, ...] } }
]
```

`AiSearch.vue` 根据 `type` 自动渲染对应组件，提交时根据 `queryType` 拼接查询条件。

#### 3.3.2 表格列渲染增强

`columns` 支持三种渲染方式：

```js
// 方式1：直接指定 render 函数
{ prop: 'status', label: '状态',
  render: (row) => h(DictTag, { dictType: 'contract_status', value: row.status }) }

// 方式2：声明式 render 配置（由 AiCrudPage 自动转换）
{ prop: 'status', label: '状态', render: { type: 'dictTag', dictType: 'contract_status' } }

// 方式3：普通列（自动渲染）
{ prop: 'contractName', label: '合同名称' }
```

方式2 的 `render.type` 支持：`dictTag`、`orgName`、`userName`、`imageUpload`、`fileUpload`。

#### 3.3.3 操作列自动生成

当 `columns` 中某个列的 `prop` 为 `action` 或 `actions` 时，`AiCrudPage` 自动生成操作列：

```js
// 声明操作按钮
{ prop: 'action', label: '操作', width: 200,
  actions: [
    { label: '编辑', key: 'edit', type: 'primary' },
    { label: '删除', key: 'delete', type: 'error' }
  ]
}

// AiCrudPage 内部转换为 render 函数，支持超过2个按钮时自动折叠到"更多"下拉
```

树形表（`treeConfig` 存在时）会自动在操作列注入"添加下级"按钮。

#### 3.3.4 API 占位符替换

`apiConfig` 支持 `:id` 占位符，提交时自动替换为当前行主键值：

```js
// 配置
apiConfig: { update: 'put@/ai/crud/contract/:id' }

// 编辑 id=42 的记录时，实际请求
PUT /ai/crud/contract/42
```

替换逻辑在 `parseApiConfig()` 函数中实现，支持 `:id`、`:rowKey`、`:dictId` 等任意字段占位符。

#### 3.3.5 加密请求支持

当 `apiConfig` 的方法部分为 `postEncrypt` 时，使用加密通道发送请求：

```js
apiConfig: { add: 'postEncrypt@/ai/crud/contract' }
// 实际调用 postEncrypt(url, data)
```

### 3.4 钩子系统

`AiCrudPage` 提供 7 个钩子，覆盖 CRUD 全生命周期：

| 钩子 | 触发时机 | 参数 | 返回值 |
|-------|---------|------|--------|
| `beforeLoadList` | 列表请求发出前 | `params` | 处理后的 params |
| `beforeRenderList` | 列表数据到达后、渲染前 | `list` | 处理后的 list |
| `beforeSearch` | 搜索按钮点击、参数组装后 | `params` | false=中断搜索 |
| `beforeRenderForm` | 新增/编辑弹窗打开、数据组装后 | `row\|null` | 处理后的表单数据 |
| `beforeRenderDetail` | 详情数据加载后、回显前 | `data` | 处理后的详情数据 |
| `beforeSubmit` | 表单提交前 | `formData` | false=中断提交 |
| `beforeDelete` | 删除确认前 | `rows` | false=中断删除 |

钩子可以是同步函数或 `async` 函数，`AiCrudPage` 统一处理 Promise 情况。

---

## 四、与后端接口的自动对接机制

### 4.1 RESTful 约定

`AiCrudPage` 默认遵循 RESTful 约定：

| 操作 | HTTP 方法 | URL | 数据位置 |
|------|-----------|-----|---------|
| 列表 | GET | `/api/resource?pageNum=1&pageSize=10` | Query |
| 详情 | GET | `/api/resource/:id` | Path |
| 新增 | POST | `/api/resource` | Body |
| 编辑 | PUT | `/api/resource/:id` | Path + Body |
| 删除 | DELETE | `/api/resource/:id` | Path |
| 批量删除 | DELETE | `/api/resource` | Body（数组） |
| 导出 | POST | `/api/resource/export` | Body（Blob 响应）|
| 导入 | POST | `/api/resource/import` | FormData |

### 4.2 动态 CRUD 自动对接

Forge 平台提供动态 CRUD 能力，后端根据 `configKey` 自动生成 CRUD 接口，无需手写 Controller：

```
POST /ai/crud/{configKey}/page        // 分页列表
GET  /ai/crud/{configKey}/:id        // 详情
POST /ai/crud/{configKey}             // 新增
PUT  /ai/crud/{configKey}/:id        // 编辑
DEL  /ai/crud/{configKey}/:id        // 删除
POST /ai/crud/{configKey}/export      // 导出
POST /ai/crud/{configKey}/import      // 导入
```

`AiCrudPage` 的 `api` 属性只需配置 `/ai/crud/{configKey}`，`apiConfig` 会自动按上述约定拼接。

### 4.3 字段白名单机制

动态 CRUD 运行时根据 `modelSchema.fields` 生成字段白名单，只读写配置的字段，自动防止越权访问未配置的字段。

### 4.4 字典自动翻译

当字段配置了 `dictType` 时：

- **列表渲染**：`transConfig` 配置告诉前端用 `DictTag` 组件渲染
- **导出**：动态导出自动执行字典值→标签翻译
- **导入**：支持填写字典标签或字典值，后台统一转换

### 4.5 关联字段 JOIN

当字段配置了 `relationDisplay` 时，后端 `joinConfig` 自动生成 LEFT JOIN SQL，前端列表直接显示关联模型的名称字段，而非 ID。

---

## 五、低代码搭建器工作流

### 5.1 搭建四步法

```
Step 1: 数据模型
  配置应用名称、configKey、表名、字段
  └─ 输出：modelSchema.json

Step 2: 页面搭建
  从组件库启用搜索区/表格区/表单区
  选择字段、配置区域属性
  └─ 输出：pageSchema.json

Step 3: 实时预览
  预览草稿，调用后端校验接口
  └─ 读取草稿协议，渲染 AiCrudPage（草稿态）

Step 4: 发布上线
  草稿协议 → 正式配置 + 版本快照 + 菜单注册
  └─ 读取正式协议，渲染 AiCrudPage（运行态）
```

### 5.2 协议校验

发布前调用 `lowcodeValidateModel` 接口校验：

- 字段配置完整性（required 字段是否有默认值等）
- `configKey` 格式合法性（小写字母开头，只允许 `[a-z0-9_]`）
- 表名合法性（不允许 `sys_`、`ai_`、`gen_`、`flow_` 前缀）
- DDL 安全性（只允许 `CREATE TABLE IF NOT EXISTS` 和 `ALTER TABLE ... ADD COLUMN`）

### 5.3 在线 DDL

选择"创建新业务表"模式时：

1. 搭建器生成 DDL 预览
2. 发布时若勾选"在线建表/补字段"，且满足权限和条件，系统自动执行 DDL
3. **安全限制**：绝不执行删除表、删除字段、重命名字段、修改字段类型的 DDL

---

## 六、核心设计亮点

### 6.1 协议与渲染分离

`modelSchema` + `pageSchema` 是"声明式协议"，`AiCrudPage` 是"渲染器"，二者完全解耦：

- 修改字段配置 → 只需更新 `modelSchema` → 重新发布 → `AiCrudPage` 自动适配
- 修改页面布局 → 只需更新 `pageSchema` → 重新发布 → `AiCrudPage` 自动适配
- 无需修改任何 Vue 组件代码

### 6.2 字段的三重复用

同一个字段在 `modelSchema.fields` 中定义一次，自动复用到三个区域：

| 区域 | 来源 | 使用的 Field 属性 |
|------|------|-------------------|
| 搜索区 | `field.searchable === true` | `label`, `componentType`, `queryType` |
| 表格区 | `field.listVisible === true` | `label`, `render`, `width` |
| 表单区 | `field.formVisible === true` | `label`, `componentType`, `required`, `props` |

### 6.3 树形表的协议扩展

树形单表只需：

1. `modelSchema.appType = 'TREE'`
2. 配置 `modelSchema.treeConfig`（父级字段、显示字段）
3. `pageSchema.layoutType = 'tree-crud'`

`AiCrudPage` 自动：
- 左侧渲染树导航（调用 `/ai/crud/{configKey}/tree`）
- 右侧列表按父级字段过滤
- 操作列注入"添加下级"按钮
- 新增/编辑表单自动处理父级字段（隐藏，自动填充）

### 6.4 主子表的协议预留

`childrenConfig` 属性已定义，协议字段已预留：

```js
childrenConfig: [
  {
    key: "contractItems",
    modelCode: "contract_item",
    tableName: "biz_contract_item",
    fields: [ /* 子表字段定义 */ ]
  }
]
```

提交时 `buildMasterDetailSubmitData()` 自动打包为：

```json
{ "main": { /* 主表数据 */ }, "children": { "contractItems": [ /* 子表数据 */ ] } }
```

当前运行时暂未开放，但协议和 `ChildTableEditor.vue` 组件已就绪。

---

## 七、类型系统（TypeScript 接口定义）

### 7.1 AiCrudPageProps（节选）

```ts
interface AiCrudPageProps {
  // 搜索
  searchSchema: SearchFieldSchema[]
  showSearch: boolean
  searchGridCols: number
  searchLabelWidth: string | number
  searchEnableCollapse: boolean

  // 表格
  columns: TableColumnSchema[]
  rowKey: string | ((row: any) => string)
  hideSelection: boolean
  treeConfig?: TreeConfig

  // 表单
  editSchema: FormFieldSchema[]
  childrenConfig?: ChildrenConfig[]
  modalWidth: string
  modalType: 'modal' | 'drawer'
  drawerPlacement: 'left' | 'right' | 'top' | 'bottom'

  // 工具栏
  hideToolbar: boolean
  hideAdd: boolean
  hideBatchDelete: boolean
  showImport: boolean
  showExport: boolean
  toolbarActions: ToolbarAction[]

  // API
  api: string
  apiConfig: {
    list?: string    // 格式: "get@/api/resource/page"
    add?: string     // 格式: "post@/api/resource"
    create?: string  // 备选
    update?: string  // 格式: "put@/api/resource/:id"
    delete?: string  // 格式: "delete@/api/resource/:id"
    detail?: string  // 格式: "get@/api/resource/:id"
    export?: string
    import?: string
    importTemplate?: string
  }
  listMethod: 'get' | 'post'
  listDataField: string   // 默认 'records'
  listTotalField: string  // 默认 'total'

  // 钩子
  beforeLoadList?: (params: any) => any
  beforeRenderList?: (list: any[]) => any[]
  beforeSubmit?: (data: any) => any | false
  beforeDelete?: (rows: any[]) => boolean
  beforeSearch?: (params: any) => any | false
  beforeRenderForm?: (row: any | null) => any
  beforeRenderDetail?: (data: any) => any

  // 其他
  lazy: boolean
  loadDetailOnEdit: boolean
  publicQuery: Record<string, any>
  publicParams: Record<string, any>
}
```

### 7.2 FieldSchema（搜索/表单字段）

```ts
interface FieldSchema {
  field: string           // 字段名
  label: string           // 显示标签
  type: string            // 'input' | 'select' | 'radio' | 'checkbox' | 'datePicker' | 'treeSelect' | 'orgTreeSelect' | 'slot' | ...
  defaultValue?: any
  required?: boolean
  hidden?: boolean
  disabled?: boolean
  readonly?: boolean
  props?: Record<string, any>  // 组件特定属性，如 { options: [...], placeholder: '请选择' }
  rules?: ValidationRule[]    // 表单校验规则
  slotName?: string            // type='slot' 时的插槽名
  valueType?: 'string' | 'number'  // 值类型，影响编辑表单数据归一化
}
```

### 7.3 TableColumnSchema（表格列）

```ts
interface TableColumnSchema {
  prop: string            // 对应 data 中的字段名
  label: string           // 列标题
  width?: number
  minWidth?: number
  fixed?: 'left' | 'right'
  sortable?: boolean
  slot?: string           // 使用插槽自定义渲染
  render?: Function | RenderConfig  // 自定义渲染函数或声明式渲染配置
  actions?: TableAction[] // 操作按钮（prop='action' 时）
  maxActionButtons?: number  // 操作列最大直接显示按钮数，超出折叠到"更多"
}

interface RenderConfig {
  type: 'dictTag' | 'orgName' | 'userName' | 'imageUpload' | 'fileUpload'
  dictType?: string
  targetField?: string
}

interface TableAction {
  label: string
  key: string
  type?: 'primary' | 'error' | 'success' | 'warning' | 'info'
  visible?: boolean | ((row: any) => boolean)
  onClick?: (row: any) => void
  confirmText?: string    // 配置后点击前先确认
  actionType?: 'route' | 'external' | 'refresh' | 'custom'
  routePath?: string     // actionType='route' 时，支持 :id 占位符
  openTarget?: '_self' | '_blank'
  params?: ActionParam[]
}
```

---

## 八、现有不足与优化方向

### 8.1 已识别问题（来自优化设计文档）

| 问题 | 现象 | 根因 |
|------|------|------|
| 组织树字段显示 ID | 列表显示组织 ID 而非名称 | 缺少 `transConfig` 中 `orgTree` 类型的运行时处理 |
| 关联字段显示 ID | 外键字段显示 ID 而非关联名称 | 缺少 `joinConfig` 的前端自动查询和缓存机制 |
| 表单详情设计器无法区分字段来源 | 拖拽时不知道字段来自主模型还是关联模型 | `ComponentPalette.vue` 缺少模型标签 |
| 画布尺寸输入框数字截断 | 宽度/高度输入数字看不见 | CSS `width` 不足 |
| 树形表父节点查询不完整 | 点击父节点只查直接子节点 | 缺少 `ALL` 后缀查询语法支持 |

### 8.2 架构层面可增强点

1. **`transConfig` 前端运行时自动化**：当前 `transConfig` 由后端生成，但前端需要手动在 `columns.render` 中处理。可增强为：`AiCrudPage` 读取 `transConfig` 后自动包装 `columns` 的 render 函数。

2. **`joinConfig` 前端批量查询策略**：当前方案是逐行处理，可优化为：列表加载完成后，收集所有需要 JOIN 显示的 ID，批量查询一次，然后缓存。

3. **协议版本管理**：当前 `modelSchema` 和 `pageSchema` 没有版本号字段，协议变更时难以做兼容处理。建议增加 `schemaVersion` 字段。

4. **`apiConfig` 的灵活性**：当前 `apiConfig` 只支持 `method@url` 格式，不支持动态 URL 函数。可增强为支持函数类型：`list: (params) => ({ method: 'post', url: '/custom/path', data: params })`。

---

## 九、总结

`AiCrudPage` 的协议化设计核心思想是：

> **将"页面应该长什么样"和"数据应该怎么处理"全部声明化，用 JSON 协议描述，由通用渲染器 `AiCrudPage` 统一解析并渲染。**

其技术价值在于：

1. **零代码生成 CRUD 页面**：业务人员通过搭建器配置协议，无需写 Vue 代码
2. **协议与渲染解耦**：协议变更自动反映到页面，无需手动同步代码
3. **字段三重复用**：一次定义，搜索/列表/表单三个区域自动适配
4. **钩子扩展机制**：复杂业务可通过 `beforeXxx` 钩子注入自定义逻辑，不破坏协议化架构
5. **与后端动态 CRUD 深度集成**：`configKey` 机制让前后端协议自动对齐，无需手写 Controller

---

*报告生成时间：2026-05-28*  
*分析基于：forge-project 工程源码及文档*
