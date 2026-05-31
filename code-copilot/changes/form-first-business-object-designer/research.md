# fcDesigner 集成能力盘点
> change: `form-first-business-object-designer`
> task: Task 1
> date: 2026-05-31

## 1. 结论

当前项目已经具备可复用的 `fcDesigner` / form-create 基础，不需要首期自研完整表单画布。

推荐路线：

`fcDesigner 可视化编辑 -> form-create rule/options -> Forge Adapter -> FormDesignerSchema + FieldRegistry + ViewSchema + LinkageSchema -> LowcodeRuntimeConfigBuilder -> AiCrudPage`

关键边界：

- `fcDesigner` 负责拖拽、排序、复制、删除、基础属性编辑、JSON 预览和设计预览。
- Forge 负责业务对象上下文、字段绑定、业务组件、级联联动、视图投影、发布检查和运行态编译。
- form-create rule/options 可以作为设计态可编辑表示，但不能作为 Forge 运行时唯一事实来源。

## 2. 依赖版本

来源：

- `forge-admin-ui/package.json`
- `forge-admin-ui/pnpm-lock.yaml`

当前依赖：

- `@form-create/designer`: package 声明 `^3`，锁定版本 `3.4.0`
- `@form-create/element-ui`: package 声明 `^3`，锁定版本 `3.3.0`
- 设计态依赖 Element Plus 外观，运行态 Forge 仍以 Vue3 + Naive UI + AiCrudPage 为主。

## 3. 已有可复用能力

### 3.1 流程动态表单

相关文件：

- `forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue`
- `forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue`
- `forge-admin-ui/src/components/form-create/formCreateBridge.js`
- `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`

已具备能力：

- 安装 Element Plus 和 `@form-create/element-ui` 插件。
- 使用 `FcDesigner` 渲染表单设计器。
- 支持 `setRule`、`getRule`、`setOption`、保存、清空。
- 支持 form-create renderer 做预览和流程运行态动态表单提交。
- `formCreateBridge.js` 已提供 rule/options normalize、默认 options、clone、旧字段协议兼容。

可复用点：

- 插件安装逻辑应沉淀为统一 bridge，避免不同组件重复安装 Element Plus / form-create。
- normalize 逻辑可扩展为 Forge Adapter 的输入清洗层。
- renderer 可继续作为设计预览态，不替代发布运行态。

### 3.2 低代码页面表单适配器

相关文件：

- `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`
- `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`

已具备能力：

- 在低代码页面 `edit` zone 中嵌入 `FcDesigner`。
- 根据模型字段生成 form-create rule。
- 支持字段类型到基础 form-create 控件映射：`input`、`textarea`、`inputNumber`、`select`、`radio`、`checkbox`、`datePicker`、`timePicker`、`switch`、`upload`。
- 支持保存 `formCreateRule` 和 `formCreateOptions` 到 zone props。
- 支持根据 rule 中的 `field` 抽取字段引用。
- 支持清理不存在字段、重复字段引用。
- 支持主子表布局下主表字段和子表字段分离展示。
- `LowcodePreviewPane.vue` 已能优先读取 canvas/form-create 字段引用做草稿预览。

可复用点：

- `FormCreateDesignerAdapter.vue` 可以作为业务对象表单设计器的画布基础，但需要抽出转换器，避免只绑定 `zone.props.formCreateRule`。
- `page-schema.js` 的 `resolveFieldRefsFromFormCreateRules` 可迁移或复用到 `formDesignerSchema.js`。
- `buildRuleFromField`、`resolveRuleType`、`buildRuleProps` 可作为 Forge Schema -> form-create 的第一版映射基础。

## 4. 主要缺口

### 4.1 缺少 Forge FormDesignerSchema

现在保存的是 `formCreateRule/formCreateOptions` 或页面 zone canvas，不是面向业务对象的稳定表单协议。

需要补齐：

- `FormDesignerSchema`
- 组件稳定 `id`
- `fieldBinding`
- 表单 layout
- validation
- visibility
- `_forge` 扩展属性保留

### 4.2 缺少字段绑定和自动建字段协议

当前低代码适配器假设字段已经存在于模型字段列表。表单优先需要支持：

- 拖入组件后生成临时字段绑定。
- 保存时批量创建缺失字段。
- 已发布字段重命名不改 `fieldCode` / `columnName`。
- 删除表单组件不删除字段资产。

### 4.3 Forge 业务组件仍不完整

需要为 `fcDesigner` 设计态和 Forge 运行态建立映射：

- `DictSelect`
- 级联字典选择
- `RegionTreeSelect`
- 组织树 / 部门选择
- 用户选择
- 文件上传 / 图片上传
- 业务对象引用
- 子表 / 明细表

### 4.4 级联能力没有统一模型

当前字典组件和 optionSource 缺少统一 LinkageSchema。

需要补齐：

- `parentDictCode`
- `linkedDictType`
- `linkedDictValue`
- `remoteParam`
- `orgScope`
- `objectReference`
- `clearOnSourceChange`
- `emptyStrategy`

### 4.5 视图投影与运行态配置未统一

当前表单、查询、列表、详情分散在页面 Schema、canvas、zone props 中。表单优先需要：

- 从表单和字段注册表派生查询、列表、详情。
- 只保存视图覆盖项。
- 支持表单、查询、列表分别配置同一字段的不同对齐方式。
- 发布时统一输出到 `LowcodeRuntimeConfigBuilder` 使用的运行态配置。

## 5. 首期不做

- 不自研完整拖拽画布。
- 不把 form-create rule/options 当作最终运行时事实来源。
- 不把普通用户带到 DDL、表名、JSON Schema、configKey 配置。
- 不替换 `DynamicCrudController`。
- 不替换 `AiCrudPage`。
- 不在本阶段实现多人实时协同。

## 6. 后续实施建议

1. 先定义 `FormDesignerSchema`、`ViewSchema`、`LinkageSchema` 的前端工具和后端 DTO。
2. 再建设 form-create rule/options 与 Forge Schema 的双向转换器。
3. 然后把 `FormCreateDesignerAdapter.vue` 包装成业务对象表单设计器，输出 Forge Schema。
4. 最后接入发布检查和 `LowcodeRuntimeConfigBuilder`，确保运行态仍复用现有动态 CRUD。
