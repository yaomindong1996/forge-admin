# 低代码 CRUD 搭建器深度分析报告

> 分析对象：`forge-project` 工程中的低代码 CRUD 可视化搭建器
> 分析时间：2026-05-28
> 参考文档：`lowcode-crud-builder.md`

---

## 一、整体架构概览

低代码 CRUD 搭建器是 Forge 平台面向业务人员提供的零代码单表业务应用搭建能力。其架构遵循"协议驱动、设计态→发布态→运行态"的三层分离设计。

```
┌─────────────────────────────────────────────────────────────┐
│                    设计态（搭建器）                          │
│  LowcodeModelDesigner  →  modelSchema（数据模型协议）      │
│  LowcodePageBuilder    →  pageSchema（页面搭建协议）        │
│  LowcodePreviewPane    →  实时预览草稿效果                  │
└────────────────────────┬────────────────────────────────────┘
                         │ 发布（LowcodeAppService.publish）
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    发布态（协议持久化）                       │
│  ai_crud_config 表：                                      │
│    model_schema  (JSON)  ← 数据模型协议                     │
│    page_schema   (JSON)  ← 页面搭建协议                     │
│    search_schema (JSON)  ← 由 RuntimeConfigBuilder 生成     │
│    columns_schema(JSON)  ← 由 RuntimeConfigBuilder 生成     │
│    edit_schema  (JSON)  ← 由 RuntimeConfigBuilder 生成     │
│    api_config    (JSON)  ← 由 RuntimeConfigBuilder 生成     │
│    options       (JSON)  ← AiCrudPage 运行时 props          │
│  ai_crud_config_version 表：版本快照                        │
└────────────────────────┬────────────────────────────────────┘
                         │ 运行（AiCrudPage 组件读取配置）
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    运行态（AiCrudPage）                      │
│  读取 configKey 对应的已发布配置                            │
│  将 searchSchema / columns / editSchema / apiConfig         │
│  直接映射为 Props，渲染搜索表单、数据表格、编辑表单          │
│  通过动态 CRUD API（/ai/crud/{configKey}/*）完成数据读写   │
└─────────────────────────────────────────────────────────────┘
```

### 关键源码文件索引

| 层级 | 文件路径 | 职责 |
|------|---------|------|
| 设计态-模型 | `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue` | 数据模型字段设计器 |
| 设计态-模型协议 | `forge-admin-ui/src/components/lowcode-builder/model/model-schema.js` | modelSchema 的创建、标准化、字段管理 |
| 设计态-页面 | `forge-admin-ui/src/components/lowcode-builder/page/LowcodePageBuilder.vue` | 页面搭建器主容器 |
| 设计态-页面协议 | `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` | pageSchema 的创建、同步、网格布局 |
| 设计态-预览 | `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue` | 草稿实时预览 |
| 发布态-服务 | `forge/.../service/lowcode/LowcodeAppService.java` | 草稿保存、发布、回滚 |
| 发布态-协议转换 | `forge/.../service/lowcode/LowcodeRuntimeConfigBuilder.java` | modelSchema+pageSchema → AiCrudPage 运行时配置 |
| 发布态-代码生成 | `forge/.../service/lowcode/LowcodeCodegenService.java` | 低代码应用维度代码预览与下载 |
| 运行态-组件 | `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` | 通用 CRUD 页面组件 |
| 运行态-Props | `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js` | AiCrudPage 组件 Props 定义（673 行） |
| 数据库-配置表 | `forge/db/migration/V1.0.4__add_visual_lowcode_crud_builder.sql` | ai_crud_config 增量字段 + ai_crud_config_version 表 |

---

## 二、模型协议（modelSchema）深度分析

### 2.1 协议结构

`modelSchema` 描述业务对象和字段的集合，是搭建器的数据基石。

```json
{
  "schemaVersion": 2,
  "domain": { "id": 1, "code": "crm", "name": "客户关系" },
  "object": { "code": "contract", "name": "合同", "description": "..." },
  "appType": "SINGLE | TREE",
  "tableMode": "CREATE | EXISTING",
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "treeConfig": {
    "keyField": "id",
    "parentField": "parentId",
    "labelField": "name",
    "childrenField": "children",
    "treeTitle": "树形导航",
    "loadMode": "full"
  },
  "fields": [ { "field": "contractName", "columnName": "contract_name", ... } ],
  "relations": [ { "relationType": "MANY_TO_ONE", ... } ],
  "indexes": [ { "indexName": "idx_contract_no", "indexType": "NORMAL", ... } ],
  "policies": { "dataScope": "TENANT", "tenantField": "tenantId", ... }
}
```

### 2.2 字段定义详解

每个 `field` 的完整属性（由 `model-schema.js#createDefaultField` 定义）：

| 属性 | 类型 | 说明 |
|------|------|------|
| `field` | String | 驼峰字段名（如 `contractName`），前端引用标识 |
| `columnName` | String | 下划线列名（如 `contract_name`），数据库列名 |
| `label` | String | 业务中文名（如 `合同名称`） |
| `dataType` | Enum | `varchar/char/text/int/bigint/decimal/date/datetime/time/tinyint` |
| `length` | Integer | 字符串长度（varchar 默认 128） |
| `precision` | Integer | 小数精度（decimal 默认 2） |
| `required` | Boolean | 是否必填（影响表单校验 + 数据库 NOT NULL） |
| `defaultValue` | String | 默认值 |
| `searchable` | Boolean | 是否出现在查询表单中 |
| `listVisible` | Boolean | 是否在数据表格中显示 |
| `formVisible` | Boolean | 是否在编辑/详情表单中显示 |
| `componentType` | Enum | `input/textarea/number/select/radio/checkbox/dictSelect/treeSelect/orgTreeSelect/userSelect/regionTreeSelect/cascader/switch/date/datetime/time/imageUpload/fileUpload` |
| `queryType` | Enum | `eq/like/ge/le/between/in`（查询条件匹配方式） |
| `dictType` | String | 字典类型编码（componentType=dictSelect 时必填） |
| `sensitiveType` | Enum | `NONE/PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS`（脱敏类型） |
| `encryptAlgorithm` | String | 加密算法（如 `AES`），非空时启用字段加密 |
| `sortable` | Boolean | 表格列是否可排序 |
| `primaryKey` | Boolean | 是否主键 |
| `autoIncrement` | Boolean | 是否自增（主键为 bigint 且 extra 含 auto_increment） |
| `systemField` | Boolean | 是否系统字段（审计字段、租户字段） |
| `readonly` | Boolean | 是否只读（系统字段默认只读） |
| `width` | Integer | 表格列宽度（datetime 默认 180，其余默认 160） |
| `remark` | String | 字段备注 |

### 2.3 系统字段自动维护

`model-schema.js#ensureSystemFields` 会自动在业务字段前后插入系统字段：

**前置系统字段（唯一）：**
- `id`（bigint，自增主键，systemField=true，formVisible=false）

**后置系统字段（审计字段，可配置是否启用）：**
- `tenantId`（bigint，租户隔离字段）
- `createBy`（bigint，创建人）
- `createTime`（datetime，创建时间）
- `createDept`（bigint，创建部门）
- `updateBy`（bigint，更新人）
- `updateTime`（datetime，更新时间）
- `delFlag`（char(1)，逻辑删除标志）

这些字段 `formVisible=false`、`systemField=true`、`readonly=true`，业务人员无法在表单中修改，由运行时自动写入。

### 2.4 数据表模式（tableMode）

| 模式 | 说明 | 发布时的 DDL 行为 |
|------|------|-------------------|
| `CREATE`（创建新业务表） | 搭建器生成 DDL 预览，发布时在线建表 | 仅允许 `CREATE TABLE IF NOT EXISTS` 和 `ALTER TABLE ... ADD COLUMN`；需 `ai:lowcode:deploy-ddl` 权限 + 二次确认 |
| `EXISTING`（绑定已有表） | 目标表已存在，字段必须匹配 | 默认不执行 DDL，动态 CRUD 按字段白名单读写 |

**安全约束**：系统不会在线删除表、删除字段、重命名字段或修改字段类型。

### 2.5 树形单表特殊配置

当 `appType=TREE` 时，模型协议需包含 `treeConfig`：

- `parentField`：父级字段，默认 `parentId`（对应数据库 `parent_id`）
- `labelField`：节点显示字段，默认 `name`
- `keyField`：主键字段，默认 `id`

发布后，运行态左侧渲染组织树（调用 `/ai/crud/{configKey}/tree`），右侧列表按父级字段过滤。

### 2.6 关联配置（relations）

`modelSchema.relations` 配置当前模型如何关联其它模型：

```json
{
  "relationType": "MANY_TO_ONE",
  "targetObjectCode": "customer",
  "sourceField": "customerId",
  "targetField": "id",
  "displayField": "customerName"
}
```

- `relationType`：`ONE_TO_MANY`（一对其）、`MANY_TO_ONE`（多对一）、`ONE_TO_ONE`（一对一）
- `sourceField`：本模型的关联字段（如 `customerId`）
- `targetField`：关联对象的主键字段（通常是 `id`）
- `displayField`：页面回显字段（如 `customerName`，列表/表单中显示名称）

### 2.7 策略配置（policies）

`modelSchema.policies` 控制数据访问策略：

| 属性 | 说明 |
|------|------|
| `dataScope` | 数据权限范围：`TENANT`（租户隔离）、`ORG`（部门隔离）、`SELF`（仅创建人可见）、`FOLLOW_SYSTEM`（跟随系统） |
| `userField` | 创建人字段（默认 `createBy`） |
| `orgField` | 部门字段（默认 `createDept`） |
| `tenantField` | 租户字段（默认 `tenantId`） |
| `logicDeleteField` | 逻辑删除字段（默认 `delFlag`） |
| `primaryKeyStrategy` | 主键生成策略（默认 `AUTO_INCREMENT`） |

`model-schema.js#normalizeLowcodePolicies` 会自动从 `fields` 中推断这些字段的实际 field/columnName，避免硬编码。

---

## 三、页面协议（pageSchema）深度分析

### 3.1 协议结构

`pageSchema` 描述页面区域（zone）和组件布局，将 `modelSchema.fields` 映射为可视化的页面元素。

```json
{
  "layoutType": "simple-crud | tree-crud | master-detail-crud",
  "listLayoutMode": "grid | structured",
  "listGridLayout": { "cols": 12, "rowHeight": 32, "gap": 8, "items": [...] },
  "zones": [
    {
      "zoneKey": "search",
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status"],
      "props": { "fieldSettings": {}, "collapsible": true }
    },
    {
      "zoneKey": "table",
      "componentKey": "data-table",
      "enabled": true,
      "fieldRefs": ["contractName", "status", "createTime"],
      "props": {
        "showImport": true,
        "showExport": true,
        "hideBatchDelete": false,
        "enableCustomQuery": true,
        "defaultSortField": "id",
        "defaultSortOrder": "desc",
        "treeConfig": { "enabled": true, "parentField": "parentId", ... }
      }
    },
    {
      "zoneKey": "edit",
      "componentKey": "edit-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status", ...],
      "props": {
        "editGridCols": 1,
        "canvas": { "width": 1040, "height": 460, "snap": 8, "items": [...] }
      }
    }
  ]
}
```

### 3.2 Zone（页面区域）设计

`pageZoneCatalog` 定义了 4 个标准区域：

| zoneKey | componentKey | 说明 |
|---------|-------------|------|
| `search` | `search-form` | 查询表单区域，包含查询集、重置按钮 |
| `table` | `data-table` | 数据列表区域，包含表格列、导入/导出按钮、自定义查询 |
| `edit` | `edit-form` | 编辑表单区域（新增/编辑/详情共用），使用 Canvas 自由布局 |
| `detail` | `detail-panel` | 详情页兼容区（历史协议保留，新配置使用 `edit` zone） |

### 3.3 自由布局（Grid Layout）设计

`listGridLayout` 采用 12 列栅格系统（`LIST_PAGE_GRID_COLS=12`），将页面区域分解为可拖拽的块（Block）：

**标准 Block 类型（`listPageBlockCatalog`）：**

| blockType | 所属 zone | 说明 |
|-----------|-----------|------|
| `search-form` | search | 查询表单块，包含字段集 + 查询/重置按钮 |
| `toolbar` | table | 操作工具栏块，包含新增/导入/导出/自定义查询按钮 |
| `data-table` | table | 数据列表块，配置展示列、排序、宽度 |
| `tree-panel` | table（仅 tree-crud） | 左侧导航树块 |
| `stats-strip` | table | 顶部 KPI 指标卡片（扩展） |
| `custom-html` | table | 说明文本/富文本（扩展） |
| `sub-table-tabs` | table（仅 master-detail-crud） | 子表 Tab（扩展，尚未启用） |
| `section-divider` | table | 分组标题（扩展） |

每个 Block 的网格属性：
- `gridX`（0-11）、`gridY`（垂直位置）、`gridW`（1-12）、`gridH`（最小行高 32px）
- `fieldRefs`：该 Block 关联的字段引用列表
- `props`：Block 级配置（如 `treeConfig`、`fieldSettings`、`actions`）

### 3.4 Canvas 表单设计器（edit zone）

`edit` zone 使用 Canvas 自由布局（`props.canvas.items`），每个表单项是一个可拖拽的 Canvas 组件：

**Canvas 组件目录（`canvasComponentCatalog`）：**

| componentKey | 对应 field.componentType | 说明 |
|-------------|--------------------------|------|
| `field-input` | `input` | 单行输入 |
| `field-textarea` | `textarea` | 多行文本 |
| `field-number` | `number` | 数字输入 |
| `field-select` | `select/radio/checkbox` | 下拉选择（含字典） |
| `field-dict-select` | `dictSelect` | 字典选择器 |
| `field-tree-select` | `treeSelect` | 树形选择 |
| `field-org-tree-select` | `orgTreeSelect` | 组织树选择 |
| `field-user-select` | `userSelect` | 用户选择 |
| `field-region-tree-select` | `regionTreeSelect` | 区划树选择 |
| `field-cascader` | `cascader` | 级联选择 |
| `field-date` | `date` | 日期 |
| `field-datetime` | `datetime` | 日期时间 |
| `field-switch` | `switch` | 开关 |
| `field-upload` | `fileUpload` | 文件上传 |
| `field-image-upload` | `imageUpload` | 图片上传 |

每个 Canvas Item 的属性：
- `id`：唯一标识（格式 `{componentKey}_{随机6位}`）
- `fieldRef`：关联的 `modelSchema.fields[i].field`
- `x/y/w/h`：绝对位置与大小（像素）
- `zIndex`：层叠顺序
- `locked`：是否锁定（锁定后不可拖拽）
- `props`：组件级配置（如 `placeholder`、`fieldRefs`）

`page-schema.js#syncPageSchemaWithModel` 会在模型字段变化时自动同步 Canvas Items（新增的可见字段自动追加，删除的字段自动移除）。

### 3.5 协议同步机制

**核心函数：`syncPageSchemaWithModel(pageSchema, modelSchema)`**

此函数在以下时机被调用：
1. `LowcodePageBuilder` 初始化时
2. `modelSchema` 变化时（watch 监听）
3. `pageSchema` 变化时（watch 监听）

同步逻辑：
1. 根据 `modelSchema.appType` 确定 `layoutType`（`simple-crud` / `tree-crud` / `master-detail-crud`）
2. 过滤每个 zone 的 `fieldRefs`，移除已删除或不可见的字段
3. 对于 `edit`/`detail` zone，同步 Canvas Items（通过 `normalizeZoneCanvas`）
4. 如果 `listLayoutMode=grid`，调用 `syncGridLayoutWithModel` 同步网格布局
5. 如果 `listLayoutMode=structured`，直接使用 zone 配置

**网格布局同步：`syncGridLayoutWithModel(layout, modelSchema, options)`**

1. 过滤 `items`，移除与当前 `layoutType` 不兼容的 Block（如非树形布局的 `tree-panel`）
2. 如果 `layoutType` 发生变化（如从 `simple-crud` 切换到 `tree-crud`），调用 `createDefaultListGridLayout` 重建默认布局
3. 调整每个 Block 的 `gridX/gridY/gridW/gridH`，确保不超出 12 列栅格

---

## 四、发布态配置转换机制

### 4.1 发布流程（LowcodeAppService.publish）

```
草稿（modelSchema + pageSchema）
        ↓
LowcodeRuntimeConfigBuilder.buildRuntimeConfig(configKey, modelSchema, pageSchema)
        ↓
生成 5 个运行时配置（JSON 字符串）：
  - searchSchema  → AiCrudPage.searchSchema
  - columnsSchema  → AiCrudPage.columns
  - editSchema     → AiCrudPage.editSchema
  - apiConfig      → AiCrudPage.apiConfig
  - options        → AiCrudPage 的其他 Props（modalType, showImport 等）
        ↓
写入 ai_crud_config 表（published_version + 1）
        ↓
插入 ai_crud_config_version 表（版本快照）
        ↓
注册/更新菜单（sys_resource）
        ↓
运行态：AiCrudPage 读取已发布配置，渲染页面
```

### 4.2 协议转换器：LowcodeRuntimeConfigBuilder

此类是设计态协议 → 运行态配置的核心转换器。

**入口方法：`buildRuntimeConfig(configKey, modelSchema, pageSchema)`**

#### 4.2.1 构建 searchSchema

`buildSearchSchema(configKey, modelSchema, pageSchema)`：

1. 从 `pageSchema.zones` 中找到 `zoneKey=search` 的 zone
2. 取出 `fieldRefs`，过滤出 `searchable=true` 的字段
3. 对每个字段调用 `buildSearchField(field, fieldSetting, modelSchema, pageSchema)`
4. 如果 `appType=TREE`，追加父级字段作为隐藏查询条件（`appendTreeRuntimeField`）
5. 返回 `List<Map<String, Object>>`，序列化为 JSON 存入 `search_schema`

**searchSchema 每项的典型结构：**
```json
{
  "field": "contractName",
  "label": "合同名称",
  "component": "input",
  "queryType": "like",
  "span": 6
}
```

#### 4.2.2 构建 columnsSchema

`buildColumnsSchema(modelSchema, pageSchema)`：

1. 从 `pageSchema.zones` 中找到 `zoneKey=table` 的 zone
2. 取出 `fieldRefs`，过滤出 `listVisible=true` 的字段
3. 对每个字段调用 `buildTableColumn(field, fieldSetting)`
4. 追加"操作"列（`key=actions`），包含行级操作按钮（编辑/删除/树形表的"添加下级"）
5. 返回 `List<Map<String, Object>>`，序列化为 JSON 存入 `columns_schema`

**columnsSchema 每项的典型结构：**
```json
{
  "key": "contractName",
  "title": "合同名称",
  "dataIndex": "contractName",
  "width": 160,
  "sortable": false,
  "dictType": "",
  "sensitiveType": "NONE"
}
```

#### 4.2.3 构建 editSchema

`buildEditSchema(configKey, modelSchema, pageSchema)`：

1. 从 `pageSchema.zones` 中找到 `zoneKey=edit` 的 zone
2. 读取 `props.canvas.items`，按 `x/y` 排序（保证表单字段顺序）
3. 过滤出 `fieldRef` 非空的 Canvas Items，对每个调用 `buildEditField(field, fieldSetting)`
4. 如果 `appType=TREE`，追加父级字段作为隐藏输入（`appendTreeRuntimeField`）
5. 返回 `List<Map<String, Object>>`，序列化为 JSON 存入 `edit_schema`

**editSchema 每项的典型结构：**
```json
{
  "field": "contractName",
  "label": "合同名称",
  "component": "input",
  "required": true,
  "span": 24,
  "placeholder": "请输入合同名称"
}
```

#### 4.2.4 构建 apiConfig

`buildApiConfig(configKey, modelSchema, pageSchema)`：

生成 RESTful API 映射，运行态 `AiCrudPage` 通过此配置发起请求：

```json
{
  "list": "get@/ai/crud/{configKey}/page",
  "tree": "get@/ai/crud/{configKey}/tree",
  "detail": "get@/ai/crud/{configKey}/:id",
  "create": "post@/ai/crud/{configKey}",
  "update": "put@/ai/crud/{configKey}",
  "delete": "delete@/ai/crud/{configKey}/:id",
  "import": "post@/ai/crud/{configKey}/import",
  "export": "post@/ai/crud/{configKey}/export",
  "importTemplate": "get@/ai/crud/{configKey}/import-template"
}
```

- `list`：分页查询（支持排序、条件过滤、树形过滤）
- `tree`：仅树形表，返回树形结构
- `detail`：详情查询（`:id` 占位符，由 `rowKey` 替换）
- `create/update/delete`：写操作
- `import/export`：Excel 批量导入导出
- `importTemplate`：导入模板下载

#### 4.2.5 构建 options

`buildOptions(modelSchema, pageSchema)`：

生成 `AiCrudPage` 的 Props 配置（对应 `AiCrudPageProps.js` 中定义的 Props）：

| options 属性 | 来源 | 说明 |
|-------------|------|------|
| `modalType` | 固定 `drawer` | 编辑表单弹出方式（drawer 或 modal） |
| `modalWidth` | 固定 `800px`（主子表 `1080px`） | 弹出宽度 |
| `searchGridCols` | 固定 `4` | 搜索表单栅格列数 |
| `editGridCols` | `editZone.props.editGridCols` 或 Canvas 列数推断 | 编辑表单栅格列数 |
| `hideAdd` | `toolbarActions` 是否包含 `add` | 是否隐藏新增按钮 |
| `showImport` | `toolbarActions` 是否包含 `import` | 是否显示导入按钮 |
| `showExport` | `toolbarActions` 是否包含 `export` | 是否显示导出按钮 |
| `hideBatchDelete` | `toolbarActions` 是否包含 `batch-delete` | 是否隐藏批量删除 |
| `enableCustomQuery` | `toolbarActions` 是否包含 `custom-query` | 是否启用自定义查询 |
| `toolbarActions` | `toolbar` block 的 `customActions` | 自定义工具栏按钮 |
| `rowActions` | 一 | 自定义行操作按钮 |
| `defaultSort` | `tableZone.props.defaultSortField/Order` | 默认排序字段和方向 |
| `treeConfig` | `tableZone.props.treeConfig` | 树形配置（仅树形表） |
| `masterDetailConfig` | 主子表配置（尚未启用） | 主子表配置 |

---

## 五、前端低代码组件实现原理

### 5.1 AiCrudPage 组件架构

`AiCrudPage.vue`（2800 行）是一个"万能 CRUD 页面组件"，通过 Props 驱动：

```
AiCrudPage
  ├── AiSearch（搜索表单，由 searchSchema 驱动）
  ├── AiTable（数据表格，由 columns 驱动）
  │     └── 工具栏（新增/导入/导出/自定义查询，由 toolbarActions/showImport/showExport 驱动）
  │     └── 分页（由 showPagination/pageNum/pageSize 驱动）
  │     └── 卡片模式（由 renderMode/cardProps 驱动）
  ├── AiForm（编辑表单，由 editSchema 驱动，弹窗/抽屉展示）
  ├── AiImport（批量导入对话框，由 showImport/importApi 驱动）
  ├── AiExport（导出任务列表，由 showExportTasks/exportTaskConfigKey 驱动）
  └── AiCustomQuery（自定义查询面板，由 enableCustomQuery/customQueryConfigKey 驱动）
```

### 5.2 关键 Props 与功能映射

**搜索相关（对应 searchSchema）：**

| Props | 说明 |
|-------|------|
| `searchSchema` | 搜索表单 Schema 数组，每项定义 `field/label/component/queryType` |
| `searchGridCols` | 搜索表单栅格列数（默认 4） |
| `searchLabelWidth` | 搜索表单标签宽度（默认 `auto`） |
| `searchEnableCollapse` | 是否启用折叠（默认 `true`） |
| `searchMaxVisibleFields` | 折叠前最大显示字段数（默认 3） |

**表格相关（对应 columnsSchema）：**

| Props | 说明 |
|-------|------|
| `columns` | 表格列配置数组，每项定义 `key/title/dataIndex/width/sortable/dictType` |
| `rowKey` | 行键字段名（默认 `id`） |
| `hideSelection` | 是否隐藏多选（默认 `false`） |
| `striped/bordered/tableSize` | 表格样式 |
| `renderMode` | 数据渲染模式（`table` 或 `card`） |
| `showRenderModeSwitch` | 是否显示列表/卡片切换（默认 `true`） |
| `maxHeight/scrollX` | 表格最大高度/横向滚动宽度 |

**编辑表单相关（对应 editSchema）：**

| Props | 说明 |
|-------|------|
| `editSchema` | 编辑表单 Schema 数组，每项定义 `field/label/component/required/span` |
| `editGridCols` | 编辑表单栅格列数（默认 1） |
| `editLabelWidth` | 编辑表单标签宽度（默认 `auto`） |
| `editLabelPlacement` | 标签位置（`left` 或 `top`） |
| `modalType` | 弹出类型（`modal` 或 `drawer`，默认 `drawer`） |
| `modalWidth` | 弹出宽度（默认 `800px`） |
| `hideModalFooter` | 是否隐藏弹窗底部按钮 |

**API 相关：**

| Props | 说明 |
|-------|------|
| `api` | RESTful API 基础路径（简化写法，完整配置用 `apiConfig`） |
| `apiConfig` | API 配置对象（定义 `list/create/update/delete/detail/import/export` 的路径和方法） |
| `isEncrypt` | 是否使用加密请求 |
| `listMethod` | 列表请求方法（`get` 或 `post`，默认 `get`） |
| `listDataField` | 列表数据字段名（默认 `records`） |
| `listTotalField` | 总数字段名（默认 `total`） |

**钩子函数（扩展点）：**

| 钩子 | 调用时机 |
|------|---------|
| `beforeLoadList(params)` | 列表数据加载前，可修改请求参数 |
| `beforeRenderList(list)` | 列表数据渲染前，可修改返回数据 |
| `beforeSubmit(formData)` | 表单提交前，可修改表单数据，返回 `false` 中断提交 |
| `beforeDelete(rows)` | 删除前，返回 `false` 中断删除 |
| `beforeSearch(params)` | 搜索前，可修改搜索参数 |
| `beforeRenderReset()` | 搜索重置前 |
| `beforeRenderDetail(data)` | 详情数据渲染前 |
| `beforeRenderForm(data)` | 表单渲染前 |

### 5.3 动态 CRUD 运行时

`AiCrudPage` 的 API 指向 `/ai/crud/{configKey}/*`，这是由后端动态 CRUD 控制器提供的通用 CRUD 服务：

- **动态 CRUD 控制器**：根据 `configKey` 从 `ai_crud_config` 表读取已发布的 `table_name`、`columns_schema`、`edit_schema`、`search_schema`、`api_config`、`dict_config`、`desensitize_config`、`encrypt_config`、`trans_config` 等配置，动态生成 SQL 并执行。
- **字段白名单**：仅允许读写 `columns_schema`/`edit_schema`/`search_schema` 中出现的字段，防止越权访问。
- **字典翻译**：如果字段配置了 `dictType`，列表查询后会自动进行字典值 → 字典标签的翻译。
- **脱敏处理**：如果字段配置了 `sensitiveType`，导出时会对敏感字段进行脱敏（如手机号显示为 `138****8000`）。
- **加密处理**：如果字段配置了 `encryptAlgorithm`，写入时加密、读取时解密（对业务透明）。
- **数据权限**：根据 `modelSchema.policies.dataScope` 自动追加租户过滤、部门过滤或创建人过滤条件。

---

## 六、与 AiCrudPage 的集成关系

### 6.1 两种构建模式对比

`ai_crud_config` 表新增了 `build_mode` 字段：

| build_mode | 说明 | 适用人群 |
|-----------|------|---------|
| `AI` | AI 智能生成（通过对话生成 CRUD 配置） | 技术人员、业务人员（简单场景） |
| `LOWCODE` | 可视化低代码搭建（本文档分析的模式） | 业务人员、实施顾问 |

无论哪种模式，最终都生成相同的运行时配置（`search_schema/columns_schema/edit_schema/api_config/options`），`AiCrudPage` 不区分构建模式，统一读取配置渲染。

### 6.2 协议版本演进

- `schemaVersion: 1`：早期版本，仅支持 `modelSchema.fields` 基本字段
- `schemaVersion: 2`（当前版本）：引入 `domain/object/relations/indexes/policies`，支持领域管理、关联配置、索引配置、策略配置

`LowcodeAppService` 在读取 `modelSchema` 时会自动进行标准化（`policyService.normalizeModelSchema`），保证向前兼容。

### 6.3 版本管理

`ai_crud_config_version` 表存储每次发布的完整快照：

| 字段 | 说明 |
|------|------|
| `config_id` | 关联的 `ai_crud_config.id` |
| `version_no` | 版本号（每次发布 +1） |
| `version_type` | `publish`（发布）或 `rollback`（回滚） |
| `model_schema` | 发布时的 `modelSchema` 快照 |
| `page_schema` | 发布时的 `pageSchema` 快照 |
| `search_schema/columns_schema/edit_schema/api_config/options` | 发布时的运行时配置快照 |
| `publish_snapshot` | 完整发布快照（长文本，用于一键回滚） |

回滚功能：`LowcodeAppService.rollback(versionId)` 将指定版本的快照恢复到 `ai_crud_config` 的已发布字段。

---

## 七、关键技术亮点

### 7.1 协议驱动设计

- **设计态与运行态彻底解耦**：`modelSchema` 和 `pageSchema` 是设计态协议，由业务人员通过可视化界面配置；`searchSchema/columnsSchema/editSchema/apiConfig/options` 是运行态配置，由 `LowcodeRuntimeConfigBuilder` 自动生成。修改设计态协议不会立即影响运行态，必须经过发布。
- **版本快照**：每次发布都保存完整快照，支持回滚到任意历史版本。
- **草稿隔离**：草稿配置存储在 `model_schema/page_schema` 字段，发布后才写入运行时配置字段，保证草稿不会影响线上运行。

### 7.2 字段映射自动化

- **`field` ↔ `columnName` 自动转换**：`model-schema.js#camelToSnake` 和 `normalizeFieldName` 保证前端字段名（驼峰）与数据库列名（下划线）自动互转，业务人员只需关注中文业务含义。
- **`componentType` → Canvas 组件自动映射**：`page-schema.js#resolveDefaultFieldComponentKey` 根据字段的 `componentType` 和 `dataType` 自动选择合适的 Canvas 组件（如 `dataType=date` → `field-date`）。
- **系统字段自动维护**：`ensureSystemFields` 自动插入 `id/createBy/createTime/...` 等审计字段，业务人员无需手动配置。

### 7.3 安全与权限

- **在线 DDL 权限控制**：`ai:lowcode:deploy-ddl` 权限 + 二次确认，防止误操作。
- **字段白名单**：动态 CRUD 运行时仅允许读写协议中出现的字段，防止 SQL 注入和越权访问。
- **数据权限策略**：`policies.dataScope` 支持租户隔离、部门隔离、创建人隔离，运行时自动追加过滤条件。
- **敏感数据保护**：`sensitiveType` 配置脱敏规则，`encryptAlgorithm` 配置加密算法，导出时自动脱敏，传输时自动加密。

### 7.4 扩展性设计

- **Canvas 自由布局**：`edit` zone 使用 Canvas 拖拽布局，业务人员可以自由调整字段位置、宽度、高度，而不受栅格限制。
- **自定义工具栏按钮**：`toolbarActions` 支持配置自定义按钮，点击后触发钩子函数（`beforeXxx`），技术人员可以编写自定义逻辑。
- **主子表预留**：`appType=MASTER_DETAIL` 的协议已定义，但后端会拦截并提示"尚未启用"，为未来扩展预留空间。
- **代码生成能力**：`LowcodeCodegenService` 支持基于已发布配置生成后端 Java 代码和前端 Vue 代码，满足"低代码 → 代码生成 → 独立部署"的完整生命周期。

---

## 八、典型使用流程

以"合同管理"应用为例：

### 步骤 1：创建低代码应用

1. 进入"低代码应用列表"（/ai/lowcode-apps）
2. 点击"新建应用"，填写：
   - 应用名称：`合同管理`
   - `configKey`：`contract_management`（自动校验格式）
   - 应用类型：`单表应用`
   - 数据表模式：`创建新业务表`
   - 表名：`biz_contract`

### 步骤 2：设计数据模型

1. 进入"数据模型"Tab（LowcodeModelDesigner）
2. 添加字段：
   - `contractName`（合同名称，varchar(128)，必填，可搜索，可列表，可表单）
   - `contractNo`（合同编号，varchar(64)，唯一索引）
   - `status`（状态，varchar(32)，字典选择器，字典类型 `contract_status`）
   - `amount`（金额，decimal(10,2)，数字输入）
   - `signDate`（签订日期，date，日期选择器）
   - `customerId`（客户ID，bigint，关联配置 → 目标对象 `customer`，回显字段 `customerName`）
3. 点击"校验模型"，确保无错误
4. 如果选择"创建新业务表"，可以预览 DDL

### 步骤 3：搭建页面

1. 进入"页面搭建"Tab（LowcodePageBuilder）
2. 选择"列表页面"：
   - 查询表单：勾选 `contractName`（模糊搜索）、`status`（下拉选择）、`signDate`（日期范围）
   - 数据列表：勾选 `contractName`、`contractNo`、`status`、`amount`、`signDate`、`createTime`，设置宽度和排序
   - 工具栏：启用"新增"、"导入"、"导出"、"自定义查询"
3. 选择"表单与详情"：
   - 使用 Canvas 自由布局，拖拽字段调整位置
   - 设置 `contractName` 为必填、`amount` 为数字输入、`signDate` 为日期选择器
4. 点击"实时预览"，查看效果

### 步骤 4：发布上线

1. 点击"发布"按钮
2. 如果选择"创建新业务表"，且当前用户有 `ai:lowcode:deploy-ddl` 权限，会提示"是否在线建表"
3. 勾选"在线建表"，二次确认后，系统执行 DDL（仅 `CREATE TABLE` 和 `ALTER TABLE ADD COLUMN`）
4. 发布成功后，系统自动：
   - 将 `modelSchema` + `pageSchema` 转换为运行时配置（`searchSchema/columnsSchema/editSchema/...`）
   - 写入 `ai_crud_config` 表的已发布字段
   - 插入 `ai_crud_config_version` 表（版本号 +1）
   - 注册菜单（/ai/crud-page/contract_management）
5. 业务用户访问 /ai/crud-page/contract_management 即可使用合同管理功能

### 步骤 5：迭代优化

1. 回到"低代码搭建器"（/ai/lowcode-builder/:id）
2. 修改数据模型或页面配置（草稿状态，不影响线上）
3. 点击"实时预览"确认效果
4. 点击"发布"，系统自动：
   - 生成新版本（版本号 +1）
   - 保存旧版本快照到 `ai_crud_config_version`
   - 更新运行时配置
5. 如果发现发布有误，可以"回滚"到上一版本

---

## 九、与 AiCrudPage 协议化设计（C01）的关联

本文档分析的"低代码 CRUD 搭建器"（C02）是 C01（AiCrudPage 协议化设计）的**可视化搭建层**：

| 维度 | C01（AiCrudPage 协议化设计） | C02（低代码 CRUD 搭建器） |
|------|------------------------------|--------------------------|
| 目标 | 定义 AiCrudPage 的协议化 Props 体系 | 提供可视化界面，让业务人员零代码搭建 CRUD 应用 |
| 核心产出 | `AiCrudPageProps.js`（673 行 Props 定义） | `model-schema.js` + `page-schema.js`（设计态协议） |
| 协议转换 | 无（协议即 Props） | `LowcodeRuntimeConfigBuilder`（设计态协议 → 运行态配置） |
| 适用人群 | 前端开发人员（理解 Props 体系） | 业务人员、实施顾问（可视化操作） |
| 与 AiCrudPage 的关系 | AiCrudPage 本身 | 为 AiCrudPage 生成配置 |
| 代码生成 | 不支持 | 支持（LowcodeCodegenService） |

**协同关系：**

1. **C01 定义标准**：`AiCrudPageProps.js` 定义了 AiCrudPage 能接受的所有 Props，是"协议标准"。
2. **C02 实现搭建**：业务人员通过可视化搭建器配置 `modelSchema` 和 `pageSchema`，`LowcodeRuntimeConfigBuilder` 按照 C01 定义的协议标准，将设计态协议转换为 AiCrudPage 的 Props 配置。
3. **AiCrudPage 统一渲染**：无论通过 C01（技术人员手写 Props）还是 C02（业务人员可视化搭建），最终都由 AiCrudPage 统一渲染，保证用户体验一致。

---

## 十、总结

低代码 CRUD 搭建器是一个**协议驱动、设计态与运行态分离、支持版本管理**的完整低代码解决方案：

1. **设计态**：通过 `LowcodeModelDesigner` 和 `LowcodePageBuilder` 两个可视化设计器，业务人员可以零代码定义数据模型和页面布局，协议存储为 `modelSchema` + `pageSchema`（JSON）。
2. **发布态**：通过 `LowcodeRuntimeConfigBuilder` 将设计态协议自动转换为 AiCrudPage 运行时配置（`searchSchema/columnsSchema/editSchema/apiConfig/options`），支持版本快照和回滚。
3. **运行态**：AiCrudPage 组件读取已发布的配置，动态渲染搜索表单、数据表格、编辑表单，通过动态 CRUD API 完成数据读写。
4. **扩展性**：支持 Canvas 自由布局、自定义工具栏按钮、主子表预留、代码生成，满足从简单到复杂的业务需求。
5. **安全性**：在线 DDL 权限控制、字段白名单、数据权限策略、敏感数据保护等多重安全机制。

**与 C01 的协同**：C02 是 C01 的"上层应用"，C01 定义协议标准，C02 实现可视化搭建，最终都由 AiCrudPage 统一渲染，形成"协议标准 → 可视化搭建 → 零代码运行"的完整闭环。

---

*分析完成。参考源码版本：2026-05-28。*
