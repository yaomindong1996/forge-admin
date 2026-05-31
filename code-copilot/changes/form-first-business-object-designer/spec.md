# 表单优先的低代码业务对象设计器
> status: proposed
> created: 2026-05-31
> complexity: 🔴复杂
> related: `code-copilot/changes/lowcode-business-object-designer/spec.md`

## 1. 背景

当前 `lowcode-business-object-designer` 已经把 Forge 低代码从“模型管理/CRUD 配置”推进到“业务对象设计器”，但主体验仍然偏字段和模型：

- 用户进入对象设计后，核心动作仍是先维护业务字段，再把字段同步到表单、列表、详情。
- 表单设计器更像字段配置的附属面板，不像真正的可视化表单搭建器。
- 字段、表单、查询条件、列表列、详情展示之间存在多处配置入口，容易产生重复配置和视图不一致。
- 业务用户并不理解模型字段、数据库列、数据类型、Schema、DDL。他们更关心最终表单长什么样、字段怎么排版、哪些字段必填、哪些字段联动。
- 级联、对齐、查询条件、列表列等问题，本质上不是单点组件缺陷，而是“视图层能力被塞进字段层”的结构问题。

更符合业务低代码平台的主链路应当是：

`创建业务对象 → 设计动态表单 → 自动生成/维护字段注册表 → 派生查询/列表/详情 → 配置关系/权限/流程 → 发布运行应用`

本变更提出“表单优先”的业务对象设计器：表单是业务用户的第一入口，字段模型是平台自动维护的技术投影，运行时 CRUD 继续复用现有 Forge 低代码能力。

## 2. 核心决策

### 2.1 产品决策

普通用户默认从“表单设计”开始设计业务对象，而不是从“字段管理”开始。

- 表单画布是对象设计器默认首屏。
- 用户拖入一个“客户名称”输入框时，系统自动创建或绑定一个业务字段。
- 用户在画布上配置必填、默认值、字典、级联、显示条件、对齐方式等表单属性。
- 系统根据表单组件生成字段注册表、模型 Schema、页面 Schema 和运行时配置。
- 字段管理仍然存在，但定位为“字段资产和高级维护”，不是主入口。

### 2.2 技术决策

不推翻现有低代码底座，新增“表单优先设计层”作为业务对象设计器的产品层。

当前继续复用：

- `ai_business_object` 作为业务对象入口。
- `ai_business_object.design_status/config_key/designer_options` 作为对象设计状态和扩展配置。
- `ai_business_object_design_version` 作为设计版本快照。
- `ai_lowcode_model.model_schema` 作为低代码模型事实来源。
- `ai_crud_config` 作为运行时 CRUD 配置事实来源。
- `LowcodeRuntimeConfigBuilder` 作为运行配置生成器。
- `DynamicCrudController` 和 `AiCrudPage` 作为运行态主能力。
- `/ai/business/object/{objectId}/designer`、`/fields`、`/layout/form|list|detail`、`/publish` 等现有业务对象设计器接口。

当前系统已经集成 `fcDesigner` / `form-create`：

- 依赖：`@form-create/designer`、`@form-create/element-ui`。
- 流程表单已使用：`forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue`。
- 低代码页面已存在适配器：`forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`。
- 已有桥接工具：`forge-admin-ui/src/components/form-create/formCreateBridge.js`。

因此本阶段不建议自研完整表单设计器。更合理的技术路线是：以 `fcDesigner` 作为可视化画布和交互基础，Forge 自己建设业务组件适配、字段绑定、Schema 编排、级联规则和发布转换。

新增或强化：

- 表单设计器 Schema：记录画布组件、容器、布局、字段绑定、校验、联动、显示规则。
- 字段注册表：由表单组件自动生成和维护，承载稳定字段编码、数据库列、类型、发布状态。
- 视图投影器：从表单 Schema 和字段注册表派生查询条件、列表列、详情布局。
- 级联规则模型：统一描述字典、组织、业务对象、远程接口之间的联动。
- `fcDesigner` 适配层：把 form-create rule/options 转换为统一 Forge FormDesignerSchema，并反向生成可编辑的 form-create 规则。

## 3. 目标

- 让业务用户先看到“最终表单”，不用先理解数据模型。
- 表单设计器达到常见开源低代码表单设计器的基本交互水准：组件面板、画布、属性面板、表单设置、预览、历史、快速布局。
- 字段从表单组件自动生成，字段编码和数据库列由系统稳定维护。
- 查询条件、数据列表、详情布局从字段和表单派生，并允许视图层单独调整。
- 对齐方式、显示宽度、表格列宽、固定列、排序等归属视图配置，不污染字段模型。
- 级联能力统一建模，支持字典父子、关联字典、组织过滤、业务对象引用、远程参数过滤。
- 发布链路继续复用现有低代码运行时，不重写动态 CRUD。
- 兼容已有字段优先对象，允许平滑迁移到表单优先设计器。

## 4. 非目标

- 不重写 `DynamicCrudController`。
- 不重写 `AiCrudPage`。
- 不移除 `ai_lowcode_model` 和 `ai_crud_config`。
- 不从零自研完整表单设计器画布。
- 不把 `fcDesigner` / form-create JSON 直接作为 Forge 运行时唯一事实来源。
- 不让普通用户直接维护数据库表名、列名、DDL、JSON Schema。
- 不在本阶段实现完整流程设计器、报表设计器或复杂规则引擎。
- 不在本阶段做复杂多人实时协同编辑。

## 5. 用户角色

### 5.1 业务用户

关注最终页面：

- 我要设计一张客户录入表。
- 我要调整字段顺序和分组。
- 我要让客户等级选择后，客户状态或子分类自动过滤。
- 我要预览新增、编辑和详情效果。
- 我要发布后直接进入客户管理页面使用。

### 5.2 实施人员

关注快速交付：

- 从模板、AI、Excel、数据库表或空白对象开始。
- 设计表单、列表、查询、详情和关联数据。
- 配置字段级联、对象关联、权限、导入导出。
- 发布前看阻断项和风险项。

### 5.3 开发人员

关注底层可控：

- 查看字段编码、数据库列、数据类型、Schema、DDL、运行配置。
- 处理已发布字段变更、数据迁移、索引、脱敏、加密。
- 排查运行时配置和动态 CRUD 问题。

## 6. 产品信息架构

业务对象设计器路由继续使用：

`/app-center/object/:objectCode/designer`

设计器布局：

- 顶部：对象名称、所属套件、设计状态、发布状态、保存、预览、发布、更多。
- 左侧：设计导航。
- 中间：当前工作区画布或配置区。
- 右侧：组件属性、字段属性、表单属性、视图属性。

左侧导航建议：

1. `表单设计`：默认入口，设计新增/编辑表单。
2. `列表视图`：配置表格列、行操作、工具栏。
3. `查询条件`：配置搜索字段、默认值、折叠、对齐方式。
4. `详情视图`：配置详情分组、只读字段、关联页签。
5. `字段资产`：查看表单生成的字段，维护高级字段属性。
6. `关系与级联`：统一维护对象关系和字段联动规则。
7. `权限流程`：配置权限摘要、审批绑定、自动化摘要。
8. `发布检查`：检查字段、表单、表结构、运行配置、权限和关系。
9. `高级配置`：开发者模式，查看模型、DDL、Schema、configKey。

## 7. 表单设计器体验要求

### 7.1 左侧组件面板

组件分类：

- 基础字段：单行文本、多行文本、数字、整数、金额、日期、日期时间、时间、开关。
- 选择字段：下拉、单选、多选、级联选择、字典选择。
- 组织字段：人员、部门、组织树、地区。
- 业务字段：引用对象、关联对象、子表、明细表。
- 文件字段：附件、图片。
- 布局组件：分组、栅格、标签页、说明文本、分割线。

组件拖入画布后默认创建字段绑定。布局组件不创建字段。

### 7.2 中间画布

画布能力：

- 拖拽排序。
- 拖入字段。
- 拖入分组和栅格容器。
- 组件复制、删除、隐藏。
- 单列、双列、三列快速布局。
- 选中组件高亮。
- 空画布引导从字段模板或组件库开始。
- 支持预览新增表单和编辑表单。
- 支持历史撤销/恢复，至少保留当前会话内历史。

画布不应出现技术字段名、`undefined`、空 label、空组件。

### 7.3 右侧属性面板

选中字段组件时展示：

- 基础属性：标题、提示文案、默认值、是否必填、是否只读、是否隐藏。
- 布局属性：宽度、栅格占比、内容对齐、标签显示、标签宽度。
- 控件属性：字典类型、是否多选、是否可搜索、是否可清空、日期格式。
- 校验规则：必填、长度、数值范围、正则、自定义提示。
- 联动规则：显示条件、禁用条件、级联过滤、上级字段变化后是否清空。
- 字段绑定：自动创建字段、绑定已有字段、仅展示虚拟字段。
- 高级属性：字段编码、数据库列名、数据类型、长度、小数位、脱敏、加密。

选中表单空白处时展示：

- 表单名称。
- 标签位置。
- 标签宽度。
- 默认列数。
- 提交按钮配置。
- 表单说明。
- 运行端布局模式。

## 8. 表单 Schema 模型

表单 Schema 是产品层事实来源，用于表达业务用户看到和配置的表单。

示例结构：

```json
{
  "schemaVersion": "form-first-v1",
  "formKey": "customer_default_form",
  "formName": "客户表单",
  "layout": {
    "labelPlacement": "left",
    "labelWidth": 100,
    "gridColumns": 2
  },
  "components": [
    {
      "id": "cmp_customer_name",
      "componentKey": "input",
      "label": "客户名称",
      "fieldBinding": {
        "mode": "field",
        "fieldCode": "customerName",
        "createIfMissing": true
      },
      "props": {
        "placeholder": "请输入客户名称",
        "clearable": true
      },
      "layout": {
        "span": 2,
        "align": "left"
      },
      "validation": {
        "required": true,
        "requiredMessage": "请输入客户名称"
      },
      "visibility": {
        "hidden": false,
        "readonly": false
      }
    }
  ]
}
```

规则：

- 每个组件必须有稳定 `id`。
- 字段组件必须有 `fieldBinding`。
- `fieldBinding.mode=field` 表示绑定真实业务字段。
- `fieldBinding.mode=virtual` 表示仅展示或辅助输入，不入库。
- `fieldCode` 一旦发布，不因 label 改名自动变化。
- 布局属性只影响视图，不改变数据库字段。

## 9. 字段注册表模型

字段注册表是技术层事实来源，用于稳定承载字段编码、数据库列、数据类型和发布状态。

首期不新增独立字段事实表，优先复用 `LowcodeModelSchema.fields` 和现有业务字段 DTO/VO；后续如需要可再拆表。

字段注册表示例：

```json
{
  "fieldCode": "customerName",
  "fieldName": "客户名称",
  "columnName": "customer_name",
  "fieldType": "TEXT",
  "dataType": "varchar",
  "length": 128,
  "precision": 0,
  "componentType": "input",
  "required": true,
  "formVisible": true,
  "listVisible": true,
  "searchable": true,
  "fieldStatus": "ENABLED",
  "published": true,
  "basicProps": {
    "placeholder": "请输入客户名称"
  },
  "advancedProps": {}
}
```

规则：

- 字段注册表可以从表单组件自动生成。
- 字段注册表也可以在高级字段资产中维护。
- 表单删除组件不等于删除字段；字段删除必须进入字段资产或发布检查确认。
- 已发布字段默认只能隐藏/停用，不能直接物理删除。
- 字段改名只改 `fieldName`，不默认改 `fieldCode` 和 `columnName`。

## 10. 视图投影模型

查询条件、数据列表、详情页都从字段注册表和表单 Schema 派生，但允许视图层覆盖。

### 10.1 查询条件

查询条件配置项：

- 字段。
- 控件类型。
- 默认值。
- 占位文案。
- 排序。
- 是否默认展开。
- 标签宽度。
- 内容对齐：左对齐、居中、右对齐。
- 级联过滤。

### 10.2 数据列表

列表列配置项：

- 字段。
- 列标题。
- 宽度。
- 最小宽度。
- 固定列。
- 内容对齐：左对齐、居中、右对齐。
- 是否可排序。
- 字典标签。
- 日期格式。
- 金额格式。
- 是否隐藏。

### 10.3 详情视图

详情配置项：

- 分组。
- 字段。
- 展示标题。
- 展示格式。
- 内容对齐。
- 关联页签。
- 只读和隐藏规则。

## 11. 级联与联动模型

级联不再作为某个字典组件的特殊属性，而是统一字段联动规则。

联动规则结构：

```json
{
  "ruleId": "cascade_customer_level_status",
  "type": "cascade",
  "sourceField": "customerLevel",
  "targetField": "customerStatus",
  "dataSourceType": "dict",
  "matchMode": "linkedDict",
  "dictConfig": {
    "targetDictType": "crm_customer_status",
    "linkedDictType": "crm_customer_level"
  },
  "remoteConfig": {
    "api": "",
    "method": "get",
    "paramName": "parentValue"
  },
  "emptyStrategy": "empty",
  "clearOnSourceChange": true
}
```

支持模式：

- `parentDictCode`：使用 `sys_dict_data.parent_dict_code` 匹配上级字典项编码。
- `linkedDict`：使用 `sys_dict_data.linked_dict_type` + `linked_dict_value` 匹配关联字典。
- `remoteParam`：把上级字段值作为接口参数重新加载下级选项。
- `objectReference`：根据上级业务对象字段过滤下级业务对象数据。
- `orgScope`：根据组织、部门、区域字段过滤下级数据。

空值策略：

- `empty`：上级为空时下级选项为空。
- `all`：上级为空时显示全部。
- `disabled`：上级为空时禁用下级。

运行规则：

- 上级字段变化时，默认清空下级字段。
- 如果下级已有值仍在新选项中，可以配置保留。
- 级联规则必须在表单、查询条件和运行态表单中一致生效。

## 12. 编译与发布链路

表单优先设计器的编译链路：

```text
FormDesignerSchema
  -> FieldRegistry
  -> LowcodeModelSchema
  -> LowcodePageSchema
  -> LowcodeRuntimeConfigBuilder
  -> AiCrudConfig
  -> DynamicCrudController + AiCrudPage
```

保存草稿时：

- 保存表单 Schema 到对象设计扩展配置或设计器聚合 DTO。
- 同步生成字段注册表。
- 同步修复页面 Schema 中的字段引用。
- 标记对象 `design_status=CHANGED` 或 `DESIGNING`。

发布前：

- 校验字段注册表。
- 校验表单 Schema。
- 校验查询/列表/详情投影。
- 校验级联规则。
- 校验关系配置。
- 校验数据库表结构。
- 校验运行时配置能生成。

发布时：

- 写入或更新 `ai_lowcode_model.model_schema`。
- 写入或更新 `ai_crud_config`。
- 写入 `ai_business_object_design_version`。
- 更新 `ai_business_object.config_key/last_publish_time/last_publish_version/design_status`。
- 同步业务应用入口。

## 13. fcDesigner 集成策略

本阶段明确优先复用系统已集成的 `fcDesigner`，不重新实现完整表单设计器。

### 13.1 复用边界

`fcDesigner` 负责：

- 组件拖拽。
- 画布布局。
- 组件排序、复制、删除。
- 表单预览。
- form-create rule/options 编辑。
- 基础表单属性配置。

Forge 负责：

- 业务对象上下文。
- 业务字段自动创建和稳定绑定。
- 字典、组织、人员、地区、文件、图片等 Forge 业务组件注册。
- 字段注册表、模型 Schema、页面 Schema、运行配置编译。
- 查询条件、列表列、详情视图投影。
- 级联和联动规则统一建模。
- 发布检查、版本、权限、租户、安全控制。

### 13.2 适配层

需要建设 `fcDesigner` 适配层：

```text
Designer Adapter
  输入：form-create rule/options
  输出：Forge FormDesignerSchema
```

反向转换：

```text
Forge FormDesignerSchema + FieldRegistry
  -> form-create rule/options
  -> fcDesigner 可编辑画布
```

适配层职责：

- 从 form-create rule 中提取字段引用。
- 为新拖入组件自动创建字段绑定。
- 为 Forge 字段生成 form-create rule。
- 把 form-create 基础控件映射到 Forge 字段类型。
- 把 Forge 业务组件映射成可在 `fcDesigner` 中编辑的自定义组件。
- 保存时输出 Forge FormDesignerSchema，而不是只保存原始 form-create rule。
- 运行时仍编译到 `AiCrudPage` / `AiForm` 可消费的 schema。

### 13.3 自定义组件要求

`fcDesigner` 中需要注册或包装以下 Forge 业务组件：

- `DictSelect`：字典选择。
- 级联字典选择：支持 `parent_dict_code` 和 `linked_dict_type/value`。
- `RegionTreeSelect`：行政区划。
- 组织/部门树。
- 用户选择。
- `FileUpload`。
- `ImageUpload`。
- 引用对象选择。
- 子表/明细表。

自定义组件必须支持：

- 设计态占位展示。
- 属性面板配置。
- 运行态真实渲染。
- 与字段注册表双向绑定。

### 13.4 不直接使用 form-create 作为最终运行时事实来源

form-create rule 可以作为设计器输入输出，但不能直接替代 Forge 运行时配置，原因：

- Forge 运行态依赖 `AiCrudPage` 的搜索、列表、编辑、详情、权限、导入导出。
- Forge 字典、组织、租户、加解密、文件鉴权需要项目内统一组件和请求封装。
- 发布检查、版本回滚、字段稳定性需要 Forge Schema 控制。
- 查询条件、列表列、详情页不完全等同于一张编辑表单。

因此最终事实来源应是：

```text
Forge FormDesignerSchema + FieldRegistry + ViewSchema + LinkageSchema
```

form-create rule/options 是设计器适配层的可编辑表示。

## 14. 自研表单设计器成本判断

如果从零自研一个达到 `fcDesigner` 可用程度的表单设计器，需要覆盖：

- 拖拽引擎。
- 组件库面板。
- 画布容器、栅格、分组、嵌套布局。
- 选中态、复制、删除、排序、撤销恢复。
- 属性面板。
- 表单预览。
- JSON 导入导出。
- 设计态和运行态组件映射。
- 校验规则编辑。
- 复杂组件扩展机制。
- 与后端 Schema 的双向转换。
- 大量浏览器交互细节和回归测试。

粗略成本判断：

| 方案 | 首版可用成本 | 达到较好体验成本 | 风险 |
|------|--------------|------------------|------|
| 复用 `fcDesigner` + Forge 适配层 | 2-4 周 | 4-8 周 | 中等，主要风险在业务组件适配和 schema 转换 |
| 完全自研表单设计器 | 8-12 周 | 3-6 个月 | 高，交互细节多，容易长期维护成独立产品 |
| 当前固定表单配置继续增强 | 1-2 周 | 很难达到好体验 | 中高，短期快但用户体验改善有限 |

结论：

- 不建议完全自研表单设计器。
- 不建议继续在固定表单配置上小修小补。
- 建议以 `fcDesigner` 为画布基础，把研发资源投入到业务组件适配、字段自动绑定、级联规则、Schema 编译、发布检查和运行态一致性。

## 15. 设计器选型约束

虽然本阶段优先使用 `fcDesigner`，但设计器适配层仍要保持边界清晰，避免未来被单一设计器锁死。

设计器能力必须满足：

- 支持 Vue 3。
- 支持组件扩展。
- 支持自定义属性面板。
- 支持拖拽布局、分组、栅格。
- 能输出可控 JSON Schema。
- 许可证允许在当前项目中使用。
- 不强制替换 Forge 运行时组件。
- 能映射到 Naive UI 或 Forge 自有组件。
- 能承载字典、组织、用户、地区、文件、图片等 Forge 业务组件。

短期策略：

- 以 `FormCreateDesignerAdapter.vue` 为基础改造成业务对象表单设计器。
- 补齐 Forge 业务组件在 `fcDesigner` 中的设计态和运行态映射。
- 保留 `Designer Adapter` 边界，避免 form-create rule 渗透到所有业务层。

中期策略：

- 评估是否继续加深 `fcDesigner` 定制。
- 如果 `fcDesigner` 无法满足长期体验，再基于适配层替换画布，而不是推翻业务 Schema。

## 16. 前端改造范围

重点文件：

- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessDetailDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/CanvasFormDesigner.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/ComponentPropertyPanel.vue`
- `forge-admin-ui/src/components/form-create/formCreateBridge.js`
- `forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue`
- `forge-admin-ui/src/components/ai-form/AiForm.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge-admin-ui/src/components/DictSelect.vue`

前端要求：

- 对象设计器默认打开表单设计。
- 字段资产从主入口降级到左侧导航中的一个面板。
- 表单设计优先复用 `fcDesigner`，提供组件库、画布、属性面板和预览。
- 字段拖入表单时自动创建字段绑定。
- 属性面板区分“业务属性”和“高级属性”。
- 查询条件、列表列、详情字段提供独立对齐配置。
- 级联配置从单字段属性扩展为统一联动规则入口。
- 预览必须渲染真实 Forge 组件，而不是只显示占位卡片。

## 17. 后端改造范围

重点文件：

- `BusinessObjectDesignerController.java`
- `BusinessObjectDesignerService.java`
- `BusinessFieldDesignService.java`
- `BusinessFieldSchemaService.java`
- `BusinessLayoutDesignService.java`
- `BusinessObjectPublishService.java`
- `LowcodeRuntimeConfigBuilder.java`
- `LowcodeFieldSchema.java`
- `BusinessObjectDesignerDTO.java`
- `BusinessLayoutDTO.java`
- `BusinessFieldDTO.java`
- `BusinessObjectDesignerVO.java`
- `BusinessFieldVO.java`
- `BusinessLayoutVO.java`

后端要求：

- `getDesigner` 返回表单优先设计器所需聚合数据。
- `saveDesigner` 能接收完整表单设计草稿。
- 保存时从表单 Schema 推导字段注册表。
- 保存时保护已发布字段编码和列名。
- 布局保存接口能保存视图层对齐、列宽、排序、级联等配置。
- 发布检查能识别表单组件引用不存在字段、字段未绑定、级联规则目标缺失。
- `LowcodeRuntimeConfigBuilder` 能把视图层配置转换为 `searchSchema`、`columnsSchema`、`editSchema`。

## 18. 数据库与迁移

优先复用现有表：

- `ai_business_object.designer_options`
- `ai_business_object_design_version`
- `ai_lowcode_model.model_schema`
- `ai_crud_config`
- `ai_business_object_relation`
- `ai_business_field_template`

如现有 `designer_options` 无法承载表单优先 Schema，可新增 Flyway 脚本扩展：

- `form_schema json`：当前草稿表单 Schema。
- `field_registry_schema json`：当前字段注册表快照。
- `view_schema json`：查询、列表、详情视图配置。
- `linkage_schema json`：级联和联动规则配置。

脚本要求：

- 新增列必须通过 `information_schema` 防重复。
- JSON 字段默认允许为空。
- `tenant_id` 相关内置数据必须为 `1`。
- 不修改已执行历史迁移脚本。

## 19. 兼容策略

已有字段优先对象进入表单优先设计器时：

- 如果存在表单布局，转换为 `FormDesignerSchema`。
- 如果不存在表单布局，根据字段注册表生成默认表单。
- 如果字段存在但未在表单中使用，在右侧字段资产中标记“未放入表单”。
- 如果表单引用了不存在字段，发布检查阻断并提供删除引用或重新绑定字段。
- 旧的 `layout/form|list|detail` 接口继续可用。

已有运行应用不受影响：

- 未发布的新草稿不会影响当前 `ai_crud_config`。
- 发布后才更新运行配置。
- 支持通过设计版本回滚。

## 20. 权限与安全

- 普通用户需要 `ai:businessObject:design` 才能进入设计器。
- 发布需要 `ai:businessObject:publish`。
- 高级配置需要单独权限，例如 `ai:businessObject:advanced`。
- 在线建表、字段物理删除、字段类型变更、DDL 预览必须进入高级权限。
- 敏感字段必须支持脱敏和加密配置。
- API Key、Secret、数据库连接信息不得出现在表单 Schema 中。
- 日志不得打印完整手机号、身份证、银行卡等敏感值。

## 21. 验收标准

### 19.1 产品验收

- 进入客户对象设计器时，默认看到表单画布，而不是字段表格。
- 用户可以拖入“单行文本”并命名为“客户名称”，系统自动生成字段。
- 用户可以拖入“下拉选择”并绑定字典。
- 用户可以配置字典级联：点击字典 A 的值后，字典 B 根据 `parent_dict_code` 或 `linked_dict_type/value` 过滤。
- 用户可以在查询条件、列表列、表单字段中分别配置对齐方式。
- 用户可以预览新增表单、列表和详情。
- 用户可以发布对象，发布后运行应用可打开。
- 普通模式不出现 DDL、表名、Schema、configKey 作为主信息。

### 19.2 技术验收

- 前端 `pnpm --dir forge-admin-ui build` 通过。
- 后端 `mvn -pl forge-admin-server -am compile -DskipTests` 通过。
- `getDesigner` 能返回表单 Schema、字段注册表、视图配置、级联规则。
- `saveDesigner` 能保存表单草稿并同步字段注册表。
- `publish` 能从表单优先 Schema 生成运行时配置。
- `AiCrudPage` 运行态表单能正确处理字段对齐和级联。
- 已有 CRM 客户对象可迁移并正常发布。

## 22. 分阶段实施建议

### Phase 1：产品骨架

- 对象设计器默认入口切换为表单设计。
- 基于 `fcDesigner` 重构前端布局为组件库 + 画布 + 属性面板。
- 字段资产降级为导航项。
- 表单组件支持自动创建字段。

### Phase 2：Schema 编排

- 定义 `FormDesignerSchema`。
- 实现 form-create rule/options 与 Forge FormDesignerSchema 的双向转换。
- 实现表单 Schema 到字段注册表转换。
- 实现字段注册表到模型 Schema 转换。
- 实现表单 Schema 到编辑表单运行 Schema 转换。

### Phase 3：视图投影

- 查询条件从字段/表单派生。
- 列表列从字段/表单派生。
- 详情视图从字段/表单派生。
- 视图层支持对齐、宽度、排序、显示格式。

### Phase 4：级联联动

- 抽象统一联动规则。
- 支持字典父子和关联字典。
- 支持组织、对象引用、远程参数过滤。
- 表单、查询条件和运行态统一生效。

### Phase 5：发布与迁移

- 发布检查支持表单优先 Schema。
- 兼容旧对象自动生成表单 Schema。
- CRM 客户对象端到端迁移验证。
- 固化 `fcDesigner` 适配层边界，为后续替换或深度定制保留空间。

## 23. 风险

- 如果直接把 form-create rule 当作最终事实来源，可能导致 Forge 组件、字典、权限、运行时 Schema 难以统一。
- 如果过度定制 `fcDesigner` 内部实现，后续升级和维护成本会升高。
- 如果字段注册表完全由表单组件即时生成，已发布字段的稳定性可能受影响。
- 如果继续字段优先，产品体验会越来越像代码生成器，而不是业务低代码。
- 如果表单 Schema、字段注册表、页面 Schema 三者没有清晰边界，会继续出现重复配置和引用脏数据。
- 如果不限制高级配置权限，普通用户可能误改字段编码、列名和数据类型。

## 24. 已确认决策

- 首期以 `fcDesigner` 作为表单画布，不推进完整自研画布。
- 表单优先 Schema 优先存入 `ai_business_object.designer_options`；确实不足时再新增独立 JSON 列。
- 字段资产首期继续使用 `LowcodeModelSchema.fields` 作为事实来源，不新增独立字段表。
- 新建对象后默认进入空白表单画布，字段资产作为高级维护入口。
- 表单组件删除默认只是移出表单，不直接删除或停用字段；需要处理字段资产时在字段资产面板单独操作。
- 级联规则同时支持字段属性面板快捷维护和“关系与级联”面板统一维护，最终以 LinkageSchema 作为事实来源。
- 普通用户默认看不到字段编码、列名、DDL、Schema、configKey；开发者模式按权限展示。
- `fcDesigner` 设计态可以沿用 Element Plus / form-create 外观，发布运行态必须转换为 Naive UI / Forge 运行组件。
- 允许保留 form-create renderer 作为设计预览态，发布运行态仍编译到 `AiCrudPage`。
