# C02 低代码 CRUD 搭建器如何把"业务字段"变成"可运行页面"？

> 关键词：可视化搭建 / 模型协议 / 页面协议 / 发布态转换 / 设计态→运行态

---

## 一、问题场景：从 Excel 需求文档到可运行系统有多远？

产品经理给你发来一份 Excel：

| 字段名 | 类型 | 长度 | 必填 | 搜索 | 列表 | 表单 | 组件 | 字典 |
|--------|------|------|------|------|------|------|------|------|
| 合同编号 | varchar | 32 | ✓ | ✓ | ✓ | ✓ | input | - |
| 合同名称 | varchar | 128 | ✓ | ✓ | ✓ | ✓ | input | - |
| 合同状态 | tinyint | - | ✓ | ✓ | ✓ | ✓ | select | contract_status |
| 客户名称 | varchar | 64 | ✓ | ✓ | ✓ | ✓ | input | - |
| 合同金额 | decimal | 10,2 | ✓ | - | ✓ | ✓ | number | - |
| 签订日期 | date | - | ✓ | ✓ | ✓ | ✓ | date | - |
| 创建时间 | datetime | - | - | - | ✓ | - | datetime | - |

你的任务是：把这个 Excel 变成一个可运行的"合同管理"页面。

传统路径：
1. 建表：`CREATE TABLE biz_contract (...)`
2. 后端：Controller + Service + Mapper + DTO + VO
3. 前端：搜索区 + 表格区 + 表单弹窗 + JS 逻辑
4. 联调：字段映射、字典对接、验证规则
5. 测试：功能测试 + 权限测试

至少 2 天。如果产品经理改需求（"客户名称改成下拉选择，从客户表选"），再来一轮。

Forge Admin 的低代码 CRUD 搭建器给出的答案是：**在可视化界面拖拽，10 分钟生成可运行页面**。核心原理是三层协议转换：

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   设计态         │     │   发布态         │     │   运行态         │
│   （可视化搭建）  │────▶│   （协议持久化）  │────▶│   （AiCrudPage） │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ 1. 拖拽字段      │     │ 1. modelSchema  │     │ 1. 搜索表单      │
│ 2. 配置属性      │────▶│ 2. pageSchema   │────▶│ 2. 数据表格      │
│ 3. 布局页面      │     │ 3. 编译为运行时  │     │ 3. 编辑表单      │
│ 4. 实时预览      │     │   配置          │     │ 4. 完整交互      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 二、解决方案：三层协议，可视化转换

### 2.1 第一层：模型协议（modelSchema）—— 数据是什么

在可视化搭建器的"模型设计"标签页，你通过表单配置每个字段：

```
┌─────────────────────────────────────────────────────────┐
│                   模型设计器                             │
├─────────────────────────────────────────────────────────┤
│ 字段名: [合同编号]   数据库列名: [contract_no]           │
│ 标签: [合同编号]     数据类型: [varchar] ┌─────────────┐│
│ 长度: [32]          必填: ☑             │ 组件类型:   ││
│ 搜索: ☑             列表: ☑             │ ▼ input     ││
│ 表单: ☑             组件: [input]       │   textarea  ││
│ 字典: [─]           查询类型: [like]    │   number    ││
│ 关联显示: [─]       值类型: [string]    │   select    ││
│ 备注: [唯一业务标识]                    │   dictSelect││
└─────────────────────────────────────────────────────────┘
```

背后的 `modelSchema` 协议：

```json
{
  "schemaVersion": 2,
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "appType": "SINGLE",
  "tableMode": "CREATE",
  "fields": [
    {
      "field": "contractNo",
      "columnName": "contract_no",
      "label": "合同编号",
      "dataType": "varchar",
      "length": 32,
      "required": true,
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "input",
      "queryType": "like",
      "valueType": "string",
      "remark": "唯一业务标识"
    },
    // 更多字段...
  ],
  "policies": {
    "dataScope": "TENANT",
    "tenantField": "tenantId",
    "logicDeleteField": "delFlag"
  }
}
```

**关键设计**：系统字段自动维护。你只需要配置业务字段，系统会自动插入：

- 前置：`id`（bigint，自增主键）
- 后置：`tenantId`、`createBy`、`createTime`、`updateBy`、`updateTime`、`delFlag`

这些系统字段 `formVisible=false`、`readonly=true`，运行时自动写入，业务人员不可见。

### 2.2 第二层：页面协议（pageSchema）—— 页面长什么样

在"页面搭建"标签页，你通过拖拽布局页面：

```
┌─────────────────────────────────────────────────────────┐
│                   页面搭建器                             │
├─────────────┬───────────────────────────────────────────┤
│ 组件库       │ 画布                                      │
│             │ ┌─────────────────────────────────────┐  │
│ ▼ 搜索区    │ │ 合同编号: [________]                │  │
│   ├─ 查询表单│ │ 合同状态: [▼ 请选择]                │  │
│ ▼ 表格区    │ │ [查询] [重置]                       │  │
│   ├─ 工具栏  │ └─────────────────────────────────────┘  │
│   ├─ 数据表  │                                          │
│   ├─ 树形面板│ ┌─────────────────────────────────────┐  │
│ ▼ 表单区    │ │ 合同编号  合同状态  签订日期         │  │
│   ├─ 编辑表单│ │ CT2025001 生效     2025-01-15       │  │
│             │ │ CT2025002 草稿     2025-01-16       │  │
│             │ └─────────────────────────────────────┘  │
└─────────────┴───────────────────────────────────────────┘
```

背后的 `pageSchema` 协议：

```json
{
  "layoutType": "simple-crud",
  "listLayoutMode": "grid",
  "listGridLayout": {
    "cols": 12,
    "rowHeight": 32,
    "gap": 8,
    "items": [
      {
        "blockType": "search-form",
        "gridX": 0, "gridY": 0, "gridW": 12, "gridH": 1,
        "fieldRefs": ["contractNo", "status"],
        "props": { "collapsible": true }
      },
      {
        "blockType": "toolbar",
        "gridX": 0, "gridY": 1, "gridW": 12, "gridH": 1,
        "props": { 
          "showAdd": true,
          "showImport": true,
          "showExport": true
        }
      },
      {
        "blockType": "data-table",
        "gridX": 0, "gridY": 2, "gridW": 12, "gridH": 8,
        "fieldRefs": ["contractNo", "status", "signDate", "createTime"]
      }
    ]
  },
  "zones": [
    {
      "zoneKey": "search",
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractNo", "status"]
    },
    // 更多区域...
  ]
}
```

**关键设计**：12 列栅格自由布局。每个 Block（搜索表单、工具栏、数据表）都是一个可拖拽的网格项，支持任意位置、任意大小。

### 2.3 第三层：发布态转换 —— 从设计态到运行态

点击"发布"按钮时，后端 `LowcodeRuntimeConfigBuilder` 执行转换：

```java
public AiCrudConfig buildRuntimeConfig(ModelSchema modelSchema, PageSchema pageSchema) {
    // 1. 生成 API 配置
    ApiConfig apiConfig = buildApiConfig(modelSchema);
    
    // 2. 生成搜索表单配置
    List<FieldSchema> searchSchema = buildSearchSchema(modelSchema, pageSchema);
    
    // 3. 生成表格列配置
    List<ColumnSchema> columns = buildColumnsSchema(modelSchema, pageSchema);
    
    // 4. 生成编辑表单配置
    List<FieldSchema> editSchema = buildEditSchema(modelSchema, pageSchema);
    
    // 5. 打包为 AiCrudPage 的 Props
    return AiCrudConfig.builder()
        .apiConfig(apiConfig)
        .searchSchema(searchSchema)
        .columns(columns)
        .editSchema(editSchema)
        .options(buildOptions(modelSchema, pageSchema))
        .build();
}
```

转换后的运行配置存入 `ai_crud_config` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| `config_key` | varchar | 配置标识，如 "contract" |
| `model_schema` | json | 原始 modelSchema |
| `page_schema` | json | 原始 pageSchema |
| `search_schema` | json | 编译后的搜索配置 |
| `columns_schema` | json | 编译后的表格列配置 |
| `edit_schema` | json | 编译后的编辑表单配置 |
| `api_config` | json | 编译后的 API 配置 |
| `options` | json | AiCrudPage 的其他 Props |

**为什么存编译后的配置？** 性能优化。运行时直接读取编译结果，避免每次请求重新编译。

### 2.4 运行态：AiCrudPage 消费配置

前端页面只需一行：

```vue
<template>
  <AiCrudPage :config-key="'contract'" />
</template>
```

`AiCrudPage` 组件：
1. 根据 `config-key` 从 `/api/ai/crud/config/contract` 加载配置
2. 将 `searchSchema` 传给 `AiSearch` 渲染搜索区
3. 将 `columns` 传给 `AiTable` 渲染表格区  
4. 将 `editSchema` 传给 `AiForm` 渲染表单弹窗
5. 根据 `apiConfig` 自动处理 CRUD 请求

用户看到的完全是一个功能完整的业务系统页面，不知道背后是协议驱动的。

---

## 三、数据结构：协议衔接的细节设计

### 3.1 模型协议深度：20+ 字段属性

`modelSchema.fields` 的每个字段有 20+ 个属性，覆盖从数据库到前端的完整链路：

| 属性组 | 关键属性 | 作用 |
|--------|---------|------|
| **基础标识** | `field`、`columnName`、`label` | 业务名、列名、显示名 |
| **数据类型** | `dataType`、`length`、`precision` | 建表 DDL、表单验证 |
| **区域控制** | `searchable`、`listVisible`、`formVisible` | 三重复用开关 |
| **组件渲染** | `componentType`、`dictType`、`queryType` | 前端组件、字典、查询方式 |
| **业务规则** | `required`、`defaultValue`、`sensitiveType` | 必填、默认值、脱敏 |
| **系统属性** | `systemField`、`readonly`、`primaryKey` | 系统字段、只读、主键 |

**特殊字段类型**：

1. **字典字段**：`componentType: "dictSelect"` + `dictType: "contract_status"`
   - 表格：自动渲染为 `<DictTag />`
   - 表单：自动渲染为 `<DictSelect />`
   - 搜索：自动渲染为下拉选择

2. **关联字段**：配置 `relations` 数组
   ```json
   {
     "relationType": "MANY_TO_ONE",
     "targetObjectCode": "customer",
     "sourceField": "customerId",
     "targetField": "id",
     "displayField": "customerName"
   }
   ```
   - 表格：自动 JOIN 显示 `customerName`
   - 表单：自动渲染为客户选择器

3. **文件字段**：`componentType: "fileUpload"` 或 `"imageUpload"`
   - 自动集成 `forge-starter-file` 模块
   - 支持预览、下载、鉴权访问

### 3.2 页面协议深度：自由布局网格系统

`pageSchema` 采用 12 列栅格（`LIST_PAGE_GRID_COLS=12`），支持 8 种标准 Block：

| blockType | 所属 zone | 说明 |
|-----------|-----------|------|
| `search-form` | search | 查询表单 + 按钮 |
| `toolbar` | table | 新增/导入/导出/自定义查询按钮 |
| `data-table` | table | 数据表格 |
| `tree-panel` | table（仅 tree-crud） | 左侧导航树 |
| `stats-strip` | table | 顶部 KPI 指标卡片 |
| `custom-html` | table | 说明文本/富文本 |
| `sub-table-tabs` | table（仅 master-detail-crud） | 子表 Tab |
| `section-divider` | table | 分组标题 |

每个 Block 的网格属性：
- `gridX`、`gridY`：位置（0-based）
- `gridW`、`gridH`：大小（1-12 列，行高 32px 倍数）
- `fieldRefs`：引用的字段列表
- `props`：Block 级配置

**布局同步机制**：当你在模型设计器修改字段时，页面搭建器自动同步：

```js
// 在 model-schema.js 中
function syncPageSchemaWithModel(pageSchema, modelSchema) {
  // 1. 移除已删除字段的引用
  // 2. 为新字段创建默认布局
  // 3. 保持用户自定义的布局调整
}
```

### 3.3 发布态转换：5 个核心编译方法

`LowcodeRuntimeConfigBuilder` 的转换逻辑：

**1. buildSearchSchema()**：筛选 `searchable=true` 的字段
```java
List<FieldSchema> searchSchema = modelSchema.getFields().stream()
    .filter(field -> Boolean.TRUE.equals(field.getSearchable()))
    .map(field -> convertToSearchField(field))
    .collect(Collectors.toList());
```

**2. buildColumnsSchema()**：筛选 `listVisible=true` 的字段
- 字典字段：自动添加 `render: { type: "dictTag", dictType: "..." }`
- 关联字段：自动添加 `render` 显示关联对象名称
- 文件字段：自动添加预览链接

**3. buildEditSchema()**：筛选 `formVisible=true` 的字段
- 保留 `required`、`defaultValue` 等验证规则
- 系统字段自动设置为 `readonly: true`
- 根据 `componentType` 生成对应的组件 Props

**4. buildApiConfig()**：生成 CRUD 接口配置
```json
{
  "list": "get@/ai/crud/contract/page",
  "add": "post@/ai/crud/contract", 
  "update": "put@/ai/crud/contract/:id",
  "delete": "delete@/ai/crud/contract/:id",
  "detail": "get@/ai/crud/contract/:id",
  "export": "post@/ai/crud/contract/export",
  "import": "post@/ai/crud/contract/import"
}
```

**5. buildOptions()**：生成其他运行时选项
- `rowKey: "id"`（主键字段）
- `showPagination: true`
- `treeConfig`（如果是树形表）
- `transConfig`（字典/组织树转换配置）
- `joinConfig`（关联表 JOIN 配置）

---

## 四、实现链路：从拖拽到发布的完整流程

### 4.1 设计态：可视化搭建器组件架构

```
LowcodePageBuilder.vue（主容器）
├── LowcodeModelDesigner.vue（模型设计器）
│   ├── ModelFieldList.vue（字段列表）
│   ├── ModelFieldEditor.vue（字段属性编辑器）
│   └── ModelRelationConfig.vue（关联配置）
├── LowcodePageDesigner.vue（页面设计器）
│   ├── ComponentLibrary.vue（组件库）
│   ├── CanvasGrid.vue（画布网格）
│   ├── BlockItem.vue（可拖拽块）
│   └── BlockPropertyPanel.vue（块属性面板）
└── LowcodePreviewPane.vue（实时预览）
    └── AiCrudPage.vue（嵌入预览）
```

**关键交互**：

1. **字段拖拽**：从组件库拖拽字段到画布，自动创建对应的 Block
2. **属性联动**：修改字段的 `componentType`，画布中的组件实时更新
3. **实时预览**：右侧预览区使用真实的 `AiCrudPage`，数据 Mock 自本地
4. **协议同步**：修改模型时，页面协议自动同步字段引用

### 4.2 发布态：服务端处理流程

```
POST /api/lowcode/app/publish
  ├─ 1. 参数校验：modelSchema + pageSchema 格式校验
  ├─ 2. 表模式判断：
  │     tableMode=CREATE → 生成 DDL 预览（需二次确认）
  │     tableMode=EXISTING → 校验字段是否匹配已有表
  ├─ 3. 协议编译：LowcodeRuntimeConfigBuilder.buildRuntimeConfig()
  ├─ 4. 版本管理：
  │     - 当前配置存入 ai_crud_config_version（历史快照）
  │     - 编译结果存入 ai_crud_config
  ├─ 5. DDL 执行（如果 tableMode=CREATE 且用户确认）：
  │     - 仅允许 CREATE TABLE 和 ADD COLUMN
  │     - 禁止删除表、删除字段、修改字段类型
  └─ 6. 返回结果：configKey + 发布成功信息
```

**安全约束**：
- DDL 操作需要 `ai:lowcode:deploy-ddl` 权限
- 禁止在线删除表/字段，防止数据丢失
- 所有 DDL 操作记录审计日志

### 4.3 运行态：动态 CRUD 接口

发布后自动生成一组 RESTful 接口：

```
GET    /ai/crud/{configKey}/page      # 分页列表
GET    /ai/crud/{configKey}/tree      # 树形数据（如果 appType=TREE）
GET    /ai/crud/{configKey}/:id       # 详情
POST   /ai/crud/{configKey}           # 新增
PUT    /ai/crud/{configKey}/:id       # 更新  
DELETE /ai/crud/{configKey}/:id       # 删除
POST   /ai/crud/{configKey}/export    # 导出
POST   /ai/crud/{configKey}/import    # 导入
```

这些接口由 `AiCrudController` 动态处理，基于 `modelSchema` 自动：
- 参数验证（必填、长度、类型）
- 数据权限过滤（租户、部门、个人）
- 字段加密/脱敏处理
- 关联字段 JOIN 查询

### 4.4 版本管理与回滚

每次发布生成一个版本快照：

```sql
-- ai_crud_config_version 表
CREATE TABLE ai_crud_config_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(64) NOT NULL,
  version INT NOT NULL,
  model_schema JSON,
  page_schema JSON,
  publish_time DATETIME,
  publish_by BIGINT
);
```

支持功能：
- **版本对比**：比较任意两个版本的协议差异
- **一键回滚**：回退到指定版本
- **协议导出**：导出 JSON 文件，用于迁移/备份

---

## 五、设计取舍：可视化 vs 协议化

### 5.1 为什么不是纯可视化（无代码）？

纯可视化搭建器（如宜搭、简道云）的局限：
- 复杂业务逻辑难以表达
- 性能优化空间小
- 系统集成能力弱
-  vendor lock-in（绑定特定平台）

我们的选择：**可视化生成协议，协议驱动运行**。

优势：
1. **协议可读可调**：高级用户可以直接编辑 JSON，实现复杂配置
2. **协议可版本化**：Git 管理、Code Review、CI/CD
3. **协议可移植**：导出 JSON，在其他环境导入
4. **逃生舱机制**：复杂场景可用钩子注入自定义代码

### 5.2 协议版本演进策略

当前协议设计存在一个已知问题：缺少 `schemaVersion` 字段。

假设 v1 协议：
```json
{
  "fields": [
    { "field": "name", "label": "姓名" }
  ]
}
```

v2 协议新增属性：
```json
{
  "fields": [
    { "field": "name", "label": "姓名", "sensitiveType": "NAME" }
  ]
}
```

v1 配置在 v2 系统运行时会缺失 `sensitiveType` 属性。解决方案：

1. **向前兼容**：新属性都有默认值，缺失时使用默认
2. **协议升级**：提供升级工具，将旧协议转换为新格式
3. **版本标注**：为协议添加 `schemaVersion`，运行时根据版本号适配

计划在下一版本实现完整的协议版本管理。

### 5.3 性能优化：编译缓存与懒加载

**编译缓存**：发布时编译一次，运行时直接读取编译结果，避免重复编译。

**懒加载策略**：
- 页面首次加载：只加载 `searchSchema` 和 `columns`（首屏必需）
- 点击"新增"时：异步加载 `editSchema`
- 点击"导入"时：异步加载导入模板配置

**大型表优化**：字段超过 50 个时：
- 搜索区：默认只显示前 5 个 `searchable` 字段，其余折叠
- 表格区：默认只显示前 10 个 `listVisible` 字段，支持列显示控制
- 表单区：使用 Tab 分组，避免表单过长

---

## 六、二开指南：扩展你的搭建器

### 6.1 添加自定义组件类型

**第一步：注册组件类型**

```js
// 在 model-schema.js 中
const COMPONENT_TYPE_CATALOG = {
  // 内置类型
  input: { label: '单行文本', icon: 'el-icon-edit' },
  select: { label: '下拉选择', icon: 'el-icon-arrow-down' },
  // 添加自定义类型
  'custom-map': { 
    label: '地图选择', 
    icon: 'el-icon-location',
    // 组件实现
    component: defineAsyncComponent(() => import('@/components/custom/MapPicker.vue'))
  }
}
```

**第二步：配置字段使用**

在模型设计器中选择组件类型"地图选择"，字段自动获得对应的属性和验证规则。

**第三步：运行时渲染**

`AiCrudPage` 通过动态组件加载机制自动渲染自定义组件。

### 6.2 扩展 Block 类型

**第一步：定义 Block 配置**

```js
// 在 page-schema.js 中
const LIST_PAGE_BLOCK_CATALOG = {
  // 内置 Block
  'search-form': { label: '查询表单', zone: 'search' },
  'data-table': { label: '数据表格', zone: 'table' },
  // 添加自定义 Block
  'custom-chart': {
    label: '统计图表',
    zone: 'table',
    component: defineAsyncComponent(() => import('@/components/custom/ChartBlock.vue')),
    defaultProps: { chartType: 'bar', height: 300 }
  }
}
```

**第二步：拖拽使用**

在页面搭建器的组件库中会出现"统计图表" Block，拖拽到画布即可。

### 6.3 集成外部数据源

**场景**：合同管理需要从外部 CRM 系统同步客户数据。

**解决方案**：通过钩子注入外部数据

```js
// 1. 定义外部数据源钩子
const externalCustomerHook = {
  beforeLoadList: async (params) => {
    // 调用外部 API 获取客户数据
    const customers = await fetchExternalCustomers();
    // 注入到查询参数
    params.externalCustomerIds = customers.map(c => c.id);
    return params;
  }
}

// 2. 在搭建器中配置
{
  "field": "customerId",
  "componentType": "select",
  "props": {
    "options": [],  // 空数组，由钩子动态填充
    "hook": "externalCustomerHook"  // 指定钩子名称
  }
}
```

**优势**：无需修改搭建器核心代码，通过配置即可集成任意外部系统。

---

## 七、体验预告

低代码 CRUD 搭建器将"业务字段 Excel"到"可运行系统"的路径从 2 天压缩到 10 分钟。通过三层协议（模型→页面→运行）的可视化转换，让业务人员也能参与系统搭建。

但这只是 Forge Admin 低代码能力的冰山一角。后续我们将推出：
- **流程引擎**：可视化配置审批流、工作流
- **报表设计器**：拖拽生成复杂报表和仪表盘
- **规则引擎**：配置业务规则，实现动态决策

所有这些能力都建立在同一套协议体系上，实现"一次建模，多处使用"。

---

> Forge Admin 低代码开发平台：[https://github.com/colin4ge/forge-project](https://github.com/colin4ge/forge-project)
> 系列文章合集：[Forge Admin 技术内幕](https://github.com/colin4ge/forge-project/wiki)
