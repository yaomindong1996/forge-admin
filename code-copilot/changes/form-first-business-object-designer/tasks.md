# 任务清单：form-first-business-object-designer
> status: proposed
> created: 2026-05-31
> spec: `code-copilot/changes/form-first-business-object-designer/spec.md`
> 拆分顺序：产品决策冻结 → 表单优先协议 → fcDesigner 适配层 → Forge 业务组件 → 对象设计器体验 → 视图投影 → 级联联动 → 发布迁移 → 应用中心体验收尾 → 验证归档
> 原则：表单设计是普通用户第一入口；优先复用 `fcDesigner`；不自研完整表单画布；form-create rule/options 不是 Forge 运行时唯一事实来源；字段编码和数据库列稳定；普通模式不展示 DDL、表名、Schema、configKey。

## 前置条件

- [x] 已确认首期以 `fcDesigner` 作为表单设计画布，不推进完整自研画布。
- [x] 已确认 `FormCreateDesignerAdapter.vue` 是低代码表单设计适配基础。
- [x] 已确认 form-create rule/options 只作为设计器可编辑表示，保存和发布时必须转换为 Forge Schema。
- [x] 已确认 `LowcodeModelSchema.fields` 首期继续作为字段注册表事实来源。
- [x] 已确认 `ai_business_object.designer_options` 优先承载表单优先扩展配置；确实不足时再新增 JSON 列。
- [x] 已确认普通用户默认进入“表单设计”，字段资产下沉为高级维护面板。
- [x] 已确认表单组件删除默认只移出表单，不直接删除字段。
- [x] 已确认发布运行态继续使用 `AiCrudPage`、`AiForm`、`DynamicCrudController` 和 `LowcodeRuntimeConfigBuilder`。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 决策冻结 | Task 0-1 | 明确 fcDesigner 复用边界、字段事实来源、Schema 存储策略 |
| Phase 1 | 协议与 Schema | Task 2-5 | FormDesignerSchema、FieldRegistry、ViewSchema、LinkageSchema 协议 |
| Phase 2 | fcDesigner 适配层 | Task 6-10 | form-create rule/options 与 Forge Schema 双向转换 |
| Phase 3 | Forge 业务组件 | Task 11-16 | 字典、地区、组织、用户、文件、图片、引用对象设计态/运行态组件 |
| Phase 4 | 表单优先对象设计器 | Task 17-21 | 默认表单设计入口、字段自动绑定、字段资产降级、高级属性隔离 |
| Phase 5 | 视图投影 | Task 22-25 | 查询、列表、详情从表单和字段派生，并支持视图层覆盖 |
| Phase 6 | 级联联动 | Task 26-29 | 统一级联规则，支持字典父子、关联字典、远程参数、组织/对象过滤 |
| Phase 7 | 发布与兼容 | Task 30-33 | 发布检查、旧对象迁移、运行配置生成、版本回滚 |
| Phase 8 | 应用中心体验收尾与验证归档 | Task 34-40 | 应用总览、CRM 交付验收、构建、接口、页面、CRM 端到端验收和文档回填 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 产品和技术决策冻结 | completed | P0 |
| Task 1 | Phase 0 | 现有 fcDesigner 集成能力盘点 | completed | P0 |
| Task 2 | Phase 1 | FormDesignerSchema 协议定义 | completed | P0 |
| Task 3 | Phase 1 | 字段注册表和字段绑定协议 | completed | P0 |
| Task 4 | Phase 1 | ViewSchema 视图投影协议 | completed | P0 |
| Task 5 | Phase 1 | LinkageSchema 级联联动协议 | completed | P0 |
| Task 6 | Phase 2 | form-create 到 Forge Schema 转换器 | completed | P0 |
| Task 7 | Phase 2 | Forge Schema 到 form-create 转换器 | completed | P0 |
| Task 8 | Phase 2 | 字段引用提取和脏引用修复 | completed | P0 |
| Task 9 | Phase 2 | fcDesigner 设计器适配组件重构 | completed | P0 |
| Task 10 | Phase 2 | 设计器预览与保存事件标准化 | completed | P1 |
| Task 11 | Phase 3 | 字典组件设计态和运行态适配 | completed | P0 |
| Task 12 | Phase 3 | 地区、组织、用户组件适配 | completed | P0 |
| Task 13 | Phase 3 | 文件、图片上传组件适配 | completed | P1 |
| Task 14 | Phase 3 | 引用对象选择组件适配 | completed | P1 |
| Task 15 | Phase 3 | 子表/明细表组件适配 | completed | P1 |
| Task 16 | Phase 3 | 组件属性面板业务化 | completed | P0 |
| Task 17 | Phase 4 | 对象设计器默认入口切换为表单设计 | completed | P0 |
| Task 18 | Phase 4 | 表单拖入组件自动创建字段 | completed | P0 |
| Task 19 | Phase 4 | 字段资产面板重构 | completed | P0 |
| Task 20 | Phase 4 | 普通属性和高级属性隔离 | completed | P0 |
| Task 21 | Phase 4 | 表单设计保存和未保存变更提示 | completed | P0 |
| Task 22 | Phase 5 | 查询条件视图投影 | completed | P0 |
| Task 23 | Phase 5 | 数据列表视图投影 | completed | P0 |
| Task 24 | Phase 5 | 详情视图投影 | completed | P1 |
| Task 25 | Phase 5 | 视图层对齐、宽度、排序覆盖 | completed | P0 |
| Task 26 | Phase 6 | 级联规则编辑模型 | completed | P0 |
| Task 27 | Phase 6 | 字典父子和关联字典运行态过滤 | completed | P0 |
| Task 28 | Phase 6 | 远程参数、组织和对象引用过滤 | completed | P1 |
| Task 29 | Phase 6 | 级联发布检查和脏规则修复 | completed | P0 |
| Task 30 | Phase 7 | 发布检查支持表单优先 Schema | completed | P0 |
| Task 31 | Phase 7 | 发布编译链路接入 FormDesignerSchema | completed | P0 |
| Task 32 | Phase 7 | 旧字段优先对象迁移 | completed | P0 |
| Task 33 | Phase 7 | 设计版本和回滚兼容 | completed | P1 |
| Task 34 | Phase 8 | 应用总览和应用列表布局收敛 | completed | P0 |
| Task 35 | Phase 8 | CRM 套件交付验收区域收敛 | completed | P0 |
| Task 36 | Phase 8 | 前端构建和 lint 验证 | completed | P0 |
| Task 37 | Phase 8 | 后端编译和接口验证 | completed | P0 |
| Task 38 | Phase 8 | Playwright 页面冒烟验证 | completed | P0 |
| Task 39 | Phase 8 | CRM 客户对象端到端验收 | pending | P0 |
| Task 40 | Phase 8 | Spec、任务、执行日志回填 | completed | P1 |
| Task 41 | Phase 8 | 设计器交互回归修复 | completed | P0 |
| Task 42 | Phase 8 | 动态运行态表单组件与布局回归修复 | completed | P0 |
| Task 43 | Phase 8 | 组织数据源、运行态样式和字段 DDL 同步修复 | completed | P0 |
| Task 44 | Phase 8 | 表单布局组件与保存入口收敛 | completed | P0 |
| Task 45 | Phase 8 | 应用入口、设计器弹层和多列表单回归修复 | completed | P0 |
| Task 46 | Phase 8 | fcDesigner 画布列数和布局组件中文化修复 | completed | P0 |

---

## Phase 0：决策冻结

### Task 0: 产品和技术决策冻结

**目标**: 在编码前冻结表单优先主链路，避免继续在固定字段配置和自研画布上消耗成本。

**涉及文件**:
- `code-copilot/changes/form-first-business-object-designer/spec.md`
- `code-copilot/changes/form-first-business-object-designer/tasks.md`
- `.opencode/memory/decisions.md`

**执行要点**:
- 确认 `fcDesigner` 是首期表单画布基础。
- 确认不从零自研完整表单设计器。
- 确认 form-create rule/options 必须经 Forge Adapter 转换。
- 确认普通用户默认进入“表单设计”。
- 确认字段资产不再作为普通用户第一入口。
- 确认表单组件删除不直接删除字段。

**验收标准**:
- `spec.md` 第 24 章待确认问题中与首期路线相关的问题已给出结论。
- `tasks.md` 前置条件全部有明确确认。
- `.opencode/memory/decisions.md` 记录“fcDesigner 优先，不自研完整画布”的项目决策。

### Task 1: 现有 fcDesigner 集成能力盘点

**目标**: 明确当前 `fcDesigner` 能力边界，避免重复实现已有功能。

**涉及文件**:
- `forge-admin-ui/package.json`
- `forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue`
- `forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue`
- `forge-admin-ui/src/components/form-create/formCreateBridge.js`
- `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`
- `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`

**执行要点**:
- 梳理 `@form-create/designer`、`@form-create/element-ui` 版本和使用位置。
- 梳理 `FormCreateDesignerAdapter.vue` 当前已经支持的字段映射、保存、预览能力。
- 梳理 `page-schema.js` 已有 `formCreateRule/formCreateOptions` 字段引用解析逻辑。
- 输出当前可复用能力和缺口清单。

**验收标准**:
- 形成 `code-copilot/changes/form-first-business-object-designer/research.md`。
- `research.md` 明确哪些能力复用、哪些能力补齐、哪些能力不做。

---

## Phase 1：协议与 Schema

### Task 2: FormDesignerSchema 协议定义

**目标**: 定义 Forge 自有表单设计 Schema，作为表单优先设计器的产品层事实来源。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- 新增 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/FormDesignerSchemaDTO.java`
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectDesignerDTO.java`
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessObjectDesignerVO.java`

**协议字段**:
- `schemaVersion`
- `formKey`
- `formName`
- `layout`
- `components`
- `component.id`
- `component.componentKey`
- `component.label`
- `component.fieldBinding`
- `component.props`
- `component.layout`
- `component.validation`
- `component.visibility`

**执行要点**:
- 前端提供默认 schema、normalize、validate、clone 工具。
- 后端 DTO 使用 `Map<String, Object>` 承载可扩展 JSON，避免频繁改 Java 类型。
- `BusinessObjectDesignerVO` 返回当前表单设计草稿。
- `BusinessObjectDesignerDTO` 支持保存当前表单设计草稿。

**验收标准**:
- 空对象能生成合法默认 `FormDesignerSchema`。
- 已有对象没有表单 Schema 时能根据字段生成默认 Schema。
- Schema 校验能识别缺少 `id`、缺少 `componentKey`、字段组件未绑定字段等问题。

### Task 3: 字段注册表和字段绑定协议

**目标**: 建立表单组件与业务字段的稳定绑定规则。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFieldDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFieldVO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFieldSchemaService.java`

**协议字段**:
- `fieldBinding.mode`: `field`、`virtual`
- `fieldBinding.fieldCode`
- `fieldBinding.createIfMissing`
- `fieldBinding.source`: `designer`、`field_asset`、`migration`
- `fieldBinding.locked`

**执行要点**:
- 拖入字段组件时自动创建 `fieldBinding`。
- label 改名只改字段展示名称，不改已发布字段编码。
- 虚拟组件不进入字段注册表。
- 系统字段和已发布字段支持只读绑定。

**验收标准**:
- 新拖入“单行文本”并命名“客户名称”时自动生成 `customerName/customer_name`。
- 已发布字段重命名不会变更 `fieldCode` 和 `columnName`。
- 删除画布组件不会删除字段注册表字段。

### Task 4: ViewSchema 视图投影协议

**目标**: 统一查询条件、数据列表、详情视图的派生配置和覆盖配置。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessDetailDesigner.vue`
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessLayoutDTO.java`
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessLayoutVO.java`

**协议字段**:
- `search.fields[]`
- `list.columns[]`
- `detail.sections[]`
- `align`
- `width`
- `fixed`
- `sortable`
- `visible`
- `order`
- `formatter`

**执行要点**:
- 视图默认从字段注册表和表单 Schema 派生。
- 用户调整查询/列表/详情时只保存覆盖项。
- 对齐、列宽、固定列、排序属于视图层配置。

**验收标准**:
- 不配置视图时能自动生成查询、列表、详情。
- 用户设置列表列居中后，发布运行态表格列居中。
- 用户设置查询条件右对齐后，运行态搜索表单字段右对齐。

### Task 5: LinkageSchema 级联联动协议

**目标**: 统一描述字段联动，不把级联能力散落到单个组件属性中。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/designer/form-first/linkageSchema.js`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue`
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectDesignerDTO.java`
- 修改 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessObjectDesignerVO.java`

**协议字段**:
- `ruleId`
- `type`
- `sourceField`
- `targetField`
- `dataSourceType`
- `matchMode`
- `dictConfig`
- `remoteConfig`
- `emptyStrategy`
- `clearOnSourceChange`

**执行要点**:
- 支持 `parentDictCode`、`linkedDict`、`remoteParam`、`objectReference`、`orgScope`。
- 表单、查询条件、运行态表单共用规则。
- 发布检查能定位失效规则。

**验收标准**:
- 能定义“客户等级 → 客户状态”的关联字典级联。
- 能定义“省份/组织 → 下级选项”的远程参数过滤。
- 规则引用不存在字段时发布检查阻断。

---

## Phase 2：fcDesigner 适配层

### Task 6: form-create 到 Forge Schema 转换器

**目标**: 将 `fcDesigner` 输出的 form-create rule/options 转换为 Forge `FormDesignerSchema`。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js`
- 修改 `forge-admin-ui/src/components/form-create/formCreateBridge.js`
- 修改 `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`

**执行要点**:
- 解析 rule 的 `field/title/type/props/validate/value/children`。
- 生成稳定 `component.id`。
- 映射 `type` 到 `componentKey`。
- 从 rule 提取字段绑定。
- 将 form-create 校验规则转换为 Forge validation。
- 将 form-create options 转换为 Forge layout。

**验收标准**:
- input、textarea、inputNumber、select、radio、checkbox、datePicker、timePicker、switch、upload 可转换。
- 嵌套 `children` 可转换。
- 转换后不丢失字段引用和基础校验。

### Task 7: Forge Schema 到 form-create 转换器

**目标**: 将 Forge `FormDesignerSchema` 反向生成 form-create rule/options，使 `fcDesigner` 可编辑。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js`
- 修改 `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`

**执行要点**:
- 根据组件类型生成 form-create `type`。
- 根据字段注册表补齐 `field/title/name`。
- 根据 Forge props 生成 form-create props。
- 根据 validation 生成 form-create validate。
- 根据 layout 生成 form-create options。
- 保留 Forge 扩展属性到 rule `_forge` 字段，避免编辑后丢失业务信息。

**验收标准**:
- Forge Schema 进入 `fcDesigner` 后可正常显示、编辑、保存。
- 编辑后再次转换回 Forge Schema 不丢失 `fieldBinding`。
- `fcDesigner` 不显示 `undefined` 或空字段标题。

### Task 8: 字段引用提取和脏引用修复

**目标**: 防止表单引用已删除字段或重复字段。

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java`

**执行要点**:
- 提取 form-create rule 中的所有字段引用。
- 提取 Forge FormDesignerSchema 中的所有字段引用。
- 删除重复字段引用。
- 标记不存在字段引用。
- 提供修复动作：移除组件、重新绑定字段、创建新字段。

**验收标准**:
- 表单引用不存在字段时，保存可提示，发布必须阻断。
- 重复绑定同一字段时提示用户确认是否允许复用。
- 自动修复不会删除字段资产中的真实字段。

### Task 9: fcDesigner 设计器适配组件重构

**目标**: 将现有低代码 `FormCreateDesignerAdapter.vue` 改造为业务对象表单设计器核心画布。

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**执行要点**:
- 保留 `FcDesigner` 作为画布。
- 增加业务对象上下文：objectId、objectCode、suiteCode、fields。
- 顶部操作改为保存、预览、按字段生成、清理失效字段。
- 输出 Forge FormDesignerSchema，而不是只输出 formCreateRule。
- 支持从已有字段注册表生成默认表单。

**验收标准**:
- 对象设计器表单页能加载 `fcDesigner`。
- 保存后后端能收到 Forge FormDesignerSchema。
- 重新进入页面可以还原上次设计结果。

### Task 10: 设计器预览与保存事件标准化

**目标**: 统一表单设计器保存、预览、应用配置、重置等动作。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue`
- `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`

**执行要点**:
- `save` 保存 Forge Schema。
- `preview` 展示设计态表单预览。
- `resetFromFields` 根据字段注册表重建表单。
- `repairRefs` 修复失效字段引用。
- `applyDesignerConfig` 只做本地应用，不自动发布。

**验收标准**:
- 用户点击预览可看到真实表单控件。
- 用户点击保存后页面离开不再提示未保存。
- 重置前必须二次确认。

---

## Phase 3：Forge 业务组件

### Task 11: 字典组件设计态和运行态适配

**目标**: 在 `fcDesigner` 中支持 Forge 字典选择，并映射到运行态 `DictSelect`。

**涉及文件**:
- `forge-admin-ui/src/components/DictSelect.vue`
- `forge-admin-ui/src/composables/useDict.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`

**执行要点**:
- 设计态支持选择 `dictType`。
- 设计态展示字典占位项。
- 运行态使用 `DictSelect` 和 `useDict`。
- 保留 `parentDictCode`、`linkedDictType`、`linkedDictValue`。

**验收标准**:
- 设计器中可拖入“字典选择”。
- 属性面板可选择字典类型。
- 发布后运行态下拉显示真实字典项。

### Task 12: 地区、组织、用户组件适配

**目标**: 支持 Forge 常用组织类组件在 `fcDesigner` 中设计和运行。

**涉及文件**:
- `forge-admin-ui/src/components/RegionTreeSelect.vue`
- `forge-admin-ui/src/components/common/UserSelectPicker.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`

**执行要点**:
- 地区组件映射到 `RegionTreeSelect`。
- 用户组件映射到 `UserSelectPicker`。
- 部门/组织组件映射到现有组织树 optionSource。
- 搜索态保留行政区划 `ALL` 查询语义。

**验收标准**:
- 设计器可拖入地区、组织、用户组件。
- 运行态表单和查询条件能正常选择。
- 行政区划查询继续符合 AGENTS 附录 A 规则。

### Task 13: 文件、图片上传组件适配

**目标**: 支持文件和图片上传组件设计态、运行态和鉴权展示。

**涉及文件**:
- `forge-admin-ui/src/components/file-upload/index.vue`
- `forge-admin-ui/src/components/image-upload/index.vue`
- `forge-admin-ui/src/components/common/AuthImage.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`

**执行要点**:
- 图片上传存储 fileId，不存 URL。
- 运行态图片回显使用 `AuthImage`。
- 文件字段支持数量、大小、类型限制。
- 表单设计态显示上传占位，不实际上传。

**验收标准**:
- 图片字段发布后表单可上传 fileId。
- 列表图片回显不直接使用 URL。
- 文件字段配置不会暴露敏感下载地址。

### Task 14: 引用对象选择组件适配

**目标**: 支持业务对象引用字段，例如联系人引用客户。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFieldSchemaService.java`

**执行要点**:
- 设计态可选择目标对象、值字段、显示字段。
- 运行态通过远程 optionSource 加载对象选项。
- 支持保存 ID 和回显名称。
- 支持作为级联源字段或目标字段。

**验收标准**:
- 可配置“联系人所属客户”引用字段。
- 编辑表单能回显已保存客户名称。
- 查询条件可按引用对象筛选。

### Task 15: 子表/明细表组件适配

**目标**: 支持主子表/明细字段设计和运行。

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- 设计态用“明细表”组件表达子表。
- 明细表绑定目标对象和关系。
- 子表字段不误生成到主对象字段注册表。
- 发布检查目标对象和关系配置。

**验收标准**:
- CRM 客户详情可配置联系人/跟进记录明细区域。
- 主表发布不会把子表字段同步进主表物理表。

**执行状态**: completed
- `forgeBusinessComponents` 已提供“明细表”业务组件，字段绑定为 virtual，避免误生成主对象字段。
- `ChildTableEditor` 新增行后立即提交到父表单，提交时可进入主子表 payload。
- 子表复杂字段复用 `AiFormItem` 运行态组件，支持字典、组织、地区、引用对象、上传等真实控件。
- `LowcodeRuntimeConfigBuilder` 在编辑区缺少子表 fieldRef 时兜底输出子表可编辑字段，避免 form-first 保存后子表配置为空。

### Task 16: 组件属性面板业务化

**目标**: 让业务用户在属性面板里只看到高频业务配置，高级技术项默认折叠。

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/page/ComponentPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`

**执行要点**:
- 普通属性：标题、提示、默认值、必填、只读、隐藏、宽度、对齐。
- 控件属性：字典类型、多选、搜索、清空、日期格式。
- 联动属性：级联源字段、匹配方式、清空策略。
- 高级属性：字段编码、列名、数据类型、长度、小数位、脱敏、加密。

**验收标准**:
- 普通模式默认不展示数据库列名、DDL、Schema。
- 开发者模式才展示高级属性。
- 属性改动能保存并恢复。

---

## Phase 4：表单优先对象设计器

### Task 17: 对象设计器默认入口切换为表单设计

**目标**: 业务用户进入对象设计器时默认看到表单画布。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`
- `forge-admin-ui/src/router/index.js`

**执行要点**:
- 默认 panel 从 `fields` 改为 `form`。
- 左侧导航把“表单设计”放第一位。
- 字段资产放到“字段资产”导航项。
- URL query `panel=fields` 仍兼容。

**验收标准**:
- 点击“设计对象”默认进入表单设计。
- 老链接 `panel=fields` 仍能打开字段资产。
- 顶部状态和发布按钮不丢失。

**执行状态**: completed
- 应用中心、套件页、对象详情页和对象创建向导默认打开 `panel=form`。
- 对象设计器页面在没有 query panel 时默认进入表单页签，`panel=fields` 老链接仍保留字段资产入口。
- 设计器壳层已调整为全屏浮层式工作台，表单画布拥有完整视口空间。

### Task 18: 表单拖入组件自动创建字段

**目标**: 用户拖入字段组件后无需先去字段管理创建字段。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `forge-admin-ui/src/api/business-app.js`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFieldDesignService.java`

**执行要点**:
- 拖入组件生成临时字段绑定。
- 用户填写标题后生成字段编码建议。
- 保存时批量创建缺失字段。
- 字段创建失败时定位到组件。

**验收标准**:
- 拖入“单行文本”并输入“客户名称”后保存，会创建 `customerName` 字段。
- 字段编码冲突时提示并给出可选编码。
- 保存失败不会丢失画布草稿。

**执行状态**: completed
- `formCreateToForge` 已将 fcDesigner 临时 `field_...` 转为稳定字段编码，冲突时自动追加数字后缀。
- `BusinessFormDesigner.saveLayout` 保存表单前会根据 FormDesignerSchema 补齐缺失字段资产，并先保存字段注册表再保存布局。
- 设计器草稿字段允许先保存未完成的字典/引用配置，后续发布检查再阻断必填业务配置。

### Task 19: 字段资产面板重构

**目标**: 字段资产服务于查看和高级维护，不再是主工作台。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldList.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**执行要点**:
- 字段列表显示“已放入表单”“未放入表单”“系统字段”“已发布”状态。
- 支持将未使用字段拖回表单。
- 删除字段前展示引用位置。
- 已发布字段默认停用或隐藏。

**验收标准**:
- 未放入表单字段可一键添加到表单。
- 字段被查询/列表/详情/级联引用时删除会阻断。
- 字段状态变化同步到表单和视图。

**执行状态**: completed
- 字段资产页已增加统计、搜索、状态筛选、来源筛选和“表单生成/已入表单/未入表单/查询/列表”标记。
- 未入表单字段支持从字段资产页一键切回表单画布并追加组件。
- 字段资产定位调整为查看与高级维护，不再作为对象设计默认主工作台。

### Task 20: 普通属性和高级属性隔离

**目标**: 降低业务用户认知负担。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessAdvancedConfig.vue`
- `forge-admin-ui/src/stores` 或现有权限/用户 store

**执行要点**:
- 普通模式只展示业务字段属性。
- 高级模式展示字段编码、列名、数据类型、Schema。
- 高级模式需要权限控制。
- 危险操作二次确认。

**验收标准**:
- 普通用户看不到 DDL、表名、Schema、configKey。
- 有高级权限用户可查看和维护技术项。
- 字段编码修改会提示已发布风险。

**执行状态**: completed
- 字段属性面板普通模式隐藏字段编码、列名、数据类型、字段长度等技术项。
- 高级模式继续通过权限控制入口展示字段编码、列名、数据类型、查询方式、脱敏和加密配置。

### Task 21: 表单设计保存和未保存变更提示

**目标**: 保证设计器编辑体验可靠。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/api/business-app.js`

**执行要点**:
- 表单 schema 变化设置 dirty 状态。
- 路由离开和关闭抽屉前提示。
- 保存成功更新 baseline。
- 保存失败保留用户编辑。

**验收标准**:
- 未保存离开页面会提示。
- 保存成功后不再提示。
- 保存失败时画布内容不丢失。

**执行状态**: completed
- 顶部状态增加“未保存”提示，面板 dirty 状态保留边框提示。
- 切换设计面板时如存在未保存变更会二次确认，路由离开和浏览器关闭继续保留提示。
- 表单保存失败不清理本地 FormDesignerSchema，用户画布内容继续保留。

---

## Phase 5：视图投影

### Task 22: 查询条件视图投影

**目标**: 从表单/字段派生查询条件，并允许用户覆盖。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- 默认把 `searchable=true` 字段生成查询条件。
- 支持排序、折叠、默认值、对齐。
- 支持级联规则注入查询条件组件。

**验收标准**:
- 新增字段勾选“作为查询条件”后自动出现在查询设计。
- 查询条件对齐设置发布后生效。
- 删除字段后查询条件引用被提示修复。

**执行状态**: completed
- `viewSchema.js` 已支持从 pageSchema 的 search zone 投影查询字段、查询方式、组件类型、默认值和对齐方式。
- `BusinessListDesigner` 在查询配置变化时同步更新 `draft.viewSchema`，保存列表时同步持久化 ViewSchema。

### Task 23: 数据列表视图投影

**目标**: 从字段派生列表列，并支持表格视图配置。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- `forge-admin-ui/src/components/ai-form/AiTable.vue`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- 默认把 `listVisible=true` 字段生成列。
- 支持列宽、最小宽、固定列、对齐、排序。
- 字典字段使用 `DictTag`。
- 图片字段使用 `AuthImage`。

**验收标准**:
- 列表列配置发布后运行态生效。
- 图片列不直接用 URL。
- 字典列显示标签而不是裸值。

**执行状态**: completed
- `viewSchema.js` 已支持从 table zone 投影列表列顺序、渲染方式、对齐、列宽、固定列和排序配置。
- `StructuredListPageDesigner` 补齐列宽和固定列配置，运行态继续复用已有字典、组织、用户、地区、文件和图片渲染逻辑。

### Task 24: 详情视图投影

**目标**: 从表单分组和字段生成详情视图。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessDetailDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- 默认复用表单分组生成详情分组。
- 支持详情字段隐藏、只读、格式化。
- 支持关联数据页签。

**验收标准**:
- 客户详情能显示基础信息分组。
- 详情字段顺序与设计一致。
- 关联页签不影响主表字段发布。

**执行状态**: completed
- `BusinessDetailDesigner` 保存详情时同步生成 `ViewSchema.detail.sections`。
- 详情主信息继续按表单字段顺序只读展示，关联页签从关系配置投影到 detail zone。

### Task 25: 视图层对齐、宽度、排序覆盖

**目标**: 把对齐、宽度、排序明确放在视图层，而不是污染模型字段。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge-admin-ui/src/components/ai-form/AiTable.vue`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- 表单字段支持内容对齐。
- 查询条件支持内容对齐。
- 列表列支持内容对齐、宽度、固定列。
- `LowcodeRuntimeConfigBuilder` 输出 `align/width/fixed/sortable`。

**验收标准**:
- 表单、查询、列表可分别配置同一字段的不同对齐方式。
- 发布后运行态分别生效。

**执行状态**: completed
- 表单对齐继续来自 FormDesignerSchema / form-create `_forge.layout.align`。
- 查询和列表对齐、列表列宽、固定列、排序保存到 pageSchema `fieldSettings` 并投影到 ViewSchema。
- `LowcodeRuntimeConfigBuilder` 已输出表格列 `align/width/fixed/sorter`，视图层配置优先于模型字段默认宽度。

---

## Phase 6：级联联动

### Task 26: 级联规则编辑模型

**目标**: 提供统一的级联规则配置入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/linkageSchema.js`

**执行要点**:
- 字段属性中提供快捷级联配置。
- “关系与级联”面板提供全量规则列表。
- 支持 sourceField、targetField、matchMode、emptyStrategy、clearOnSourceChange。

**验收标准**:
- 用户能配置字典 A 控制字典 B。
- 用户能查看当前对象全部级联规则。
- 删除字段时能看到关联级联规则。

**执行状态**: completed
- `linkageSchema.js` 已支持从字段 `basicProps.cascade` 汇总规则、规范化 LinkageSchema、校验规则和将规则回写目标字段。
- `BusinessRelationDesigner` 已扩展为“对象关系 / 级联规则”统一页签，支持新增、编辑、停用、删除级联规则。
- 保存关系页时会同步保存 `linkageSchema`，并把规则回写到字段运行属性，避免级联配置散落且不可见。

### Task 27: 字典父子和关联字典运行态过滤

**目标**: 支持用户提出的 `sys_dict_data` 三字段级联。

**涉及文件**:
- `forge-admin-ui/src/composables/useDict.js`
- `forge-admin-ui/src/components/DictSelect.vue`
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- `useDict` 保留 `parentDictCode`、`linkedDictType`、`linkedDictValue`。
- `DictSelect` 支持 `parentDictCode` 过滤。
- `DictSelect` 支持 `linkedDictType/linkedDictValue` 过滤。
- 上级变化时按规则清空下级。

**验收标准**:
- 选择字典 A 的值后，字典 B 按 `parent_dict_code` 过滤。
- 选择字典 A 的值后，字典 B 按 `linked_dict_type/value` 过滤。
- 查询条件和编辑表单都能生效。

**执行状态**: completed
- `useDict` 保留 `parentDictCode`、`linkedDictType`、`linkedDictValue` 元数据。
- `DictSelect` 和 `AiFormItem` 已按 `parentDictCode`、`linkedDict` 两种模式过滤字典选项，并支持上级变化清空下级。
- LinkageSchema 保存后会写入目标字段 `basicProps.cascade`，发布运行态继续复用现有字典组件过滤能力。

### Task 28: 远程参数、组织和对象引用过滤

**目标**: 支持非字典级联，例如组织关联某类业务数据。

**涉及文件**:
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/linkageSchema.js`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- `remoteParam` 将上级字段值作为 API 参数。
- `orgScope` 将组织/部门/地区值作为过滤参数。
- `objectReference` 支持业务对象引用级联。
- 上级为空时支持 empty/all/disabled。

**验收标准**:
- 选择组织后，下级远程选择只展示该组织相关数据。
- 上级为空时下级按配置显示空、全部或禁用。

**执行状态**: completed
- 级联规则支持 `remoteParam`、`orgScope`、`objectReference` 类型，并统一生成运行态 `remoteParam` cascade 配置。
- `AiFormItem` 已在远程 optionSource 中注入上级字段参数。
- `emptyStrategy=disabled` 时运行态目标控件会在上级为空时禁用。

### Task 29: 级联发布检查和脏规则修复

**目标**: 防止发布后出现失效联动。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessPublishCheckVO.java`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue`

**执行要点**:
- 检查 sourceField 是否存在。
- 检查 targetField 是否存在。
- 检查字典类型是否存在。
- 检查远程参数是否配置完整。
- 提供修复入口。

**验收标准**:
- 级联规则引用删除字段时发布阻断。
- 缺少目标字典类型时发布阻断。
- 警告和阻断项能跳转到对应配置面板。

**执行状态**: completed
- `BusinessObjectPublishService` 新增 Linkage 发布检查，覆盖 LinkageSchema 和字段快捷级联配置。
- 发布检查会阻断上级字段缺失、目标字段缺失、目标字典类型缺失、关联字典类型缺失、远程参数缺失、远程接口缺失和引用目标对象缺失。
- 发布检查中的 `LINKAGE` 类问题会跳转到“关系与级联”面板修复。

---

## Phase 7：发布与兼容

### Task 30: 发布检查支持表单优先 Schema

**目标**: 发布检查覆盖表单 Schema、字段注册表、视图投影和级联规则。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessPublishCheckVO.java`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue`

**执行要点**:
- 字段检查。
- 表单组件检查。
- 视图引用检查。
- 级联规则检查。
- 数据表检查。
- 运行配置生成检查。

**验收标准**:
- 发布检查能显示通过、警告、阻断。
- 阻断项都有修复入口。
- 无业务字段、无表单组件、字段引用失效都能被拦截。

**执行状态**: completed
- `BusinessObjectPublishService` 已将发布检查扩展到 FormDesignerSchema、ViewSchema 和 LinkageSchema。
- 表单检查会阻断空表单、字段组件未绑定、表单组件引用不存在字段和组件 ID 重复。
- 视图检查会阻断查询、列表、详情投影引用不存在字段；阻断项能跳转到表单、列表、详情或关系与级联面板。

### Task 31: 发布编译链路接入 FormDesignerSchema

**目标**: 将表单优先 Schema 编译到现有运行态配置。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`

**执行要点**:
- FormDesignerSchema 生成 editSchema。
- ViewSchema 生成 searchSchema、columnsSchema、detailSchema。
- LinkageSchema 注入 props/cascade/optionSource。
- 字段注册表生成 LowcodeModelSchema。

**验收标准**:
- 发布后 `AiCrudPage` 能正常新增、编辑、查询、列表展示。
- 表单设计中的字段顺序和校验发布后生效。
- 视图层配置发布后生效。

**执行状态**: completed
- `BusinessObjectDesignerService.saveDraft` 已在保存草稿和发布前编译表单优先 Schema，输出统一的 `LowcodeModelSchema` 与 `LowcodePageSchema`。
- `FormDesignerSchema` 会同步字段 label、必填、只读、默认值、控件类型、表单顺序和 `edit` 区域字段设置。
- `ViewSchema` 会同步查询、列表、详情区域的字段顺序、对齐、宽度、固定列、排序和分组设置。
- `LinkageSchema` 会写回目标字段 `basicProps.cascade`，运行态组件可继续使用统一级联配置。
- `BusinessObjectPublishService.publishCheck` 会基于编译后的页面和字段配置执行运行配置生成检查。

### Task 32: 旧字段优先对象迁移

**目标**: 兼容已有业务对象，不要求用户重建表单。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessLayoutDesignService.java`

**执行要点**:
- 有 formCreateRule 时转换为 FormDesignerSchema。
- 有 LowcodePageSchema 时转换为 FormDesignerSchema。
- 只有字段时生成默认表单。
- 未使用字段进入字段资产。

**验收标准**:
- 现有 CRM 客户对象进入新设计器能看到表单。
- 旧对象未发布草稿不影响当前运行态。
- 迁移后可保存、发布、回滚。

**执行状态**: completed
- `BusinessObjectDesignerService` 在没有保存 `FormDesignerSchema` 时，会优先从旧 `edit` 区 `formCreateRule` 迁移出表单优先 Schema。
- 没有 `formCreateRule` 但存在旧 `LowcodePageSchema.edit.fieldRefs` 时，会按旧表单字段顺序和 `fieldSettings` 生成可编辑表单。
- 只有字段注册表的对象仍按字段生成默认表单，未进入表单的字段保留在字段资产中。
- 迁移结果只作为进入设计器的兜底回显，用户保存后才写入 `designer_options`，不直接改动旧运行态草稿。

### Task 33: 设计版本和回滚兼容

**目标**: 让设计版本能保存表单优先相关快照。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObjectDesignVersion.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignVersionService.java`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessVersionPanel.vue`

**执行要点**:
- 版本快照包含 FormDesignerSchema、FieldRegistry、ViewSchema、LinkageSchema。
- 回滚时恢复设计草稿，不直接覆盖运行态，除非用户重新发布。
- 版本列表展示发布时间、发布人、状态、说明。

**验收标准**:
- 发布生成设计版本。
- 回滚后设计器画布恢复。
- 重新发布后运行态更新。

**执行状态**: completed
- 新增 `ai_business_object_design_version.designer_options_snapshot` JSON 快照列，用于保存 `FormDesignerSchema/ViewSchema/LinkageSchema` 等设计器扩展配置。
- 设计版本 DTO/VO、实体和 Mapper 已补齐 `designerOptionsSnapshot`。
- 发布生成版本、回滚生成版本时都会写入当前 `designer_options` 快照。
- 回滚设计版本时会恢复模型、页面、关系和设计器扩展快照；旧版本没有快照时会清空扩展配置并回到迁移/默认兜底逻辑。

---

## Phase 8：应用中心体验收尾与验证归档

### Task 34: 应用总览和应用列表布局收敛

**目标**: 解决 `/app-center` 应用总览和应用列表信息密度混乱的问题，让业务对象、应用入口、套件导航的层级更清楚。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/index.vue`
- `forge-admin-ui/src/views/app-center/shared-center.css`
- `forge-admin-ui/src/views/app-center/components/AppCard.vue`
- `forge-admin-ui/src/views/app-center/components/ObjectCard.vue`
- `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue`

**执行要点**:
- 总览页保留左侧套件导航，但减少重复统计和装饰性信息。
- 主区域优先展示当前套件、对象数量、应用入口数量、发布状态和下一步动作。
- 对象卡片和应用入口卡片统一高度、按钮位置、状态标签和空态。
- 应用入口列表避免与业务对象列表视觉权重混在一起，使用 tabs 或清晰分区。
- 移动端和窄屏下导航、过滤器、卡片列表不能互相挤压。

**验收标准**:
- `/app-center` 首屏能清楚区分套件导航、业务对象、应用入口。
- 业务对象卡片和应用入口卡片不出现按钮错位、标题换行遮挡、统计重复堆叠。
- 1024px、1440px、移动端宽度下布局不乱。
- Playwright 截图用于对比应用总览改造结果。

**执行状态**: completed
- `/app-center` 改为左侧套件导航 + 主工作区结构，主工作区用指标条、筛选条和对象/入口 tabs 区分信息层级。
- 业务对象卡片和应用入口卡片统一为固定操作区，按钮和更多操作收敛到卡片底部，标题、描述、标签均做截断和稳定高度处理。
- 修复 Naive UI `n-spin-content` flex 导致卡片网格在桌面收缩成单列的问题，桌面恢复 3 列、1024px 恢复 2 列、移动端单列。
- 顶部 `top-menu` / `top-side-menu` 在窄屏隐藏长标题、搜索入口和次要工具按钮，避免应用中心移动端横向溢出。
- Playwright mock 数据截图验证通过：`/private/tmp/forge_app_center_visual/app-center_desktop.png`、`/private/tmp/forge_app_center_visual/app-center_tablet.png`、`/private/tmp/forge_app_center_visual/app-center_mobile.png`。

### Task 35: CRM 套件交付验收区域收敛

**目标**: 解决 `/app-center/suite/CRM` 交付验收区域信息堆叠和视觉拥挤的问题，让验收状态、阻断项和下一步动作更容易判断。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue`
- `forge-admin-ui/src/views/app-center/components/SuiteAcceptancePanel.vue`
- `forge-admin-ui/src/views/app-center/shared-center.css`

**执行要点**:
- 套件详情页主栏优先展示业务对象和应用入口，交付验收保持侧栏摘要，不抢主工作区。
- `SuiteAcceptancePanel` 区分完整模式和 compact 模式，CRM 套件侧栏默认使用 compact。
- compact 模式只展示验收结论、关键计数、阻断项摘要和下一步主按钮。
- 详细对象、引擎、渠道检查可折叠或进入完整验收详情，不在侧栏长列表堆叠。
- 状态颜色、标签和按钮语义统一，避免多层卡片嵌套。

**验收标准**:
- `/app-center/suite/CRM` 右侧交付验收区域不超过一屏主要高度。
- 验收结论、阻断项、下一步动作在 3 秒内可识别。
- 侧栏没有明显文字重叠、卡片嵌套、按钮挤压。
- Playwright 截图用于对比 CRM 套件页改造结果。

**执行状态**: completed
- `/app-center/suite/CRM` 保留主栏业务对象和场景入口，交付验收固定为侧栏摘要，不再挤占主工作区。
- `SuiteAcceptancePanel` compact 模式强化结论卡、关键步骤、核心对象、引擎能力和渠道接入的视觉层级。
- 使用容器查询优化 compact 面板：在窄侧栏保持单列摘要，在 1024px 单列页面时自动恢复多列网格，避免大屏单列拉伸。
- 修复验收面板内 `n-spin-content` 宽度收缩问题，侧栏与平板宽容器均按实际容器宽度排布。
- Playwright mock 数据截图验证通过：`/private/tmp/forge_app_center_visual/app-center_suite_CRM_desktop.png`、`/private/tmp/forge_app_center_visual/app-center_suite_CRM_tablet.png`、`/private/tmp/forge_app_center_visual/app-center_suite_CRM_mobile.png`。

### Task 36: 前端构建和 lint 验证

**目标**: 确保前端改造可构建。

**命令**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

**验收标准**:
- 构建成功。
- 若出现已有 UnoCSS icon 警告，需要记录为既有警告，不作为本变更失败。
- 无 Vue 模板编译错误。

**执行状态**: completed
- 针对本阶段触达文件的 ESLint 已通过。
- 全量前端构建已通过：`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。
- 构建中仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告，未出现 Vue 模板编译错误。

### Task 37: 后端编译和接口验证

**目标**: 确保后端 DTO、Service、发布链路编译通过。

**命令**:
```bash
cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

**接口验证**:
- `GET /ai/business/object/{objectId}/designer`
- `PUT /ai/business/object/{objectId}/designer`
- `PUT /ai/business/object/{objectId}/layout/form`
- `GET /ai/business/object/{objectId}/publish/check`
- `POST /ai/business/object/{objectId}/publish`

**验收标准**:
- Maven 编译成功。
- 接口返回统一 `RespInfo`。
- 保存表单草稿不影响当前运行配置。

**执行状态**: completed
- 后端编译已通过：`mvn -pl forge-admin-server -am compile -DskipTests`。
- 本地后端健康检查通过，CRM 客户对象读取型接口返回 HTTP 200：`designer`、`layout/form`、`fields`、`publish-check`。
- 当前本地后端连接共享数据库，未直接执行 `PUT /designer` 和 `POST /publish`，避免改写真实设计草稿或发布版本。

### Task 38: Playwright 页面冒烟验证

**目标**: 验证对象设计器基础交互无运行时错误。

**涉及页面**:
- `/app-center`
- `/app-center/suite/CRM`
- `/app-center/object/:objectCode/designer`

**验证点**:
- 登录后能进入应用中心。
- 点击 CRM 客户对象“设计对象”进入表单设计。
- `fcDesigner` 画布正常渲染。
- 拖入字段组件不报错。
- 保存草稿不报错。
- 控制台无新增 runtime error。

**验收标准**:
- 至少保存 2 张截图：应用中心、客户对象表单设计器。
- 截图中不出现明显重叠、遮挡、空白画布。

**执行状态**: completed
- Playwright mock 数据冒烟已覆盖 `/app-center`、`/app-center/suite/CRM`、`/app-center/object/CRM_CUSTOMER/designer?suiteCode=CRM&panel=form`。
- 应用中心和 CRM 套件页在 1440px、1024px、390px 视口均无 body 横向溢出。
- 表单设计器已修复 1024px 下 `fcDesigner` 中间画布被压缩导致字段竖排的问题；窄屏下由画布内部横向滚动承载完整设计器。
- 截图路径：`/private/tmp/forge_app_center_visual/app-center_desktop.png`、`/private/tmp/forge_app_center_visual/app-center_suite_CRM_desktop.png`、`/private/tmp/forge_app_center_visual/app-center_object_CRM_CUSTOMER_designer?suiteCode=CRM&panel=form_desktop.png`、对应 tablet/mobile 截图同目录。

### Task 39: CRM 客户对象端到端验收

**目标**: 以 CRM 客户对象验证完整业务闭环。

**验收脚本**:
- 打开客户对象设计器。
- 默认进入表单设计。
- 拖入客户名称、客户等级、客户状态字段。
- 配置客户等级控制客户状态级联。
- 设置客户名称必填。
- 设置列表客户等级列居中。
- 保存草稿。
- 执行发布检查。
- 发布对象。
- 打开运行应用。
- 新增一条客户数据。
- 查询列表确认字段展示和级联生效。

**验收标准**:
- 全链路可完成。
- 发布前阻断项为 0。
- 运行态新增、查询、列表展示正常。

**执行状态**: pending
- 尚未执行真实保存、发布和新增客户数据的端到端流程。
- 原因：当前本地后端连接共享数据库，直接发布或新增数据会影响共享环境。
- 下一步建议使用独立本地库或测试租户执行本任务，覆盖保存草稿、发布检查、发布对象、运行态新增与查询。

### Task 40: Spec、任务、执行日志回填

**目标**: 让变更记录可追溯。

**涉及文件**:
- `code-copilot/changes/form-first-business-object-designer/spec.md`
- `code-copilot/changes/form-first-business-object-designer/tasks.md`
- 新增 `code-copilot/changes/form-first-business-object-designer/execution-log.md`
- `.opencode/memory/pitfalls.md`
- `.opencode/memory/decisions.md`

**执行要点**:
- 回填实际完成范围。
- 标记未完成项和后续计划。
- 记录 fcDesigner 适配踩坑。
- 记录最终验证命令和结果。

**验收标准**:
- `tasks.md` 状态与实际一致。
- `execution-log.md` 记录关键命令、结果、截图路径。
- 有价值的踩坑和决策进入记忆文件。

**执行状态**: completed
- 已新增 `execution-log.md`，记录实现范围、验证命令、截图路径和剩余风险。
- 已回填 Task 36-40 状态，其中真实 CRM 端到端验收继续保持 pending。
- 已补充 `fcDesigner` 窄屏布局踩坑到 `.opencode/memory/pitfalls.md`。

### Task 41: 设计器交互回归修复

**目标**: 修复表单优先对象设计器在真实使用中的交互回归。

**问题清单**:
- 左侧菜单切换后 `BusinessFormCreateDesigner` 异步回写已销毁的 `fcDesigner` 实例，导致 `setRule` 空引用并卡住。
- 列表设计底部/右侧汇总信息占用空间。
- `fcDesigner` 内置预览弹层被对象设计器全屏层级遮挡。
- 关系与级联面板把 `linkageSchema` prop 变更重新 emit 给父组件，触发递归更新。
- 部分保存请求没传 `relations` 时，后端 DTO 默认空列表导致已有关系被误清空。

**执行状态**: completed
- `BusinessFormCreateDesigner` 增加销毁态和加载序号保护，所有异步 hydrate 完成后都会重新确认设计器实例存在。
- `BusinessListDesigner` 移除查询条件、表格列、工具栏、行操作汇总侧栏，列表设计区域改为单列工作区。
- `BusinessObjectDesignerShell` 降低全屏层级，让 form-create / Element Plus 预览弹层可以正常浮在设计器之上。
- `BusinessRelationDesigner` 拆分关系脏状态和级联同步，外部 schema 重置不再反向 emit，保存级联时不会携带未加载或未修改的空关系数组。
- `object-designer.[objectCode].vue` 的通用草稿保存不再携带 `relations`，关系只由关系面板专门保存。
- `BusinessObjectDesignerDTO` 的 `fields`、`relations`、`designerOptions` 改为可空，后端可以区分“未传字段”和“明确传空数组”。
- 验证通过：目标文件 ESLint、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 42: 动态运行态表单组件与布局回归修复

**目标**: 修复运行态动态页面中组织选择、用户选择回显以及编辑表单布局与设计态不一致的问题。

**问题清单**:
- 运行态表单项只识别严格的 `orgTreeSelect`、`userSelect` 类型，历史别名或设计器中间类型会退化为普通输入框。
- 用户选择后只回填单一 `targetField`，遇到 `ownerUserIdName`、`ownerUserName`、`ownerName` 等不同翻译字段时展示不稳定。
- 组织树当前值缺少名称兜底选项，编辑详情中值能回填但显示名称可能丢失。
- 表单设计保存后只写回 `formCreateRule/formCreateOptions`，运行态直接消费的 `fieldSettings` 没同步，导致顺序、span、labelWidth、align、label 不一致。
- 后端运行态编辑字段 label 优先取模型字段，未优先使用设计器覆盖 label。

**执行状态**: completed
- `AiFormItem` 增加组织/用户组件别名识别，组织树默认读取 `/system/org/tree`，并在选择或编辑回填时通过候选名称字段补齐展示文本。
- `AiFormItem` 用户选择回填支持 `labelValueField`、`targetField`、`${field}Name`、`*UserName`、`*Name` 等候选字段，选择和清空都会同步显示字段。
- `AiForm` 必填校验把组织/用户别名按选择类组件处理。
- `BusinessFormDesigner` 保存表单设计时同步编译 `fieldSettings`、`fieldRefs`、`editGridCols`、`labelPlacement`、`labelWidth`，并保留关系字段配置。
- `LowcodeRuntimeConfigBuilder` 运行态编辑字段优先使用设计器 label/defaultValue，规范组织/用户历史别名，并为组织/用户字段补齐默认 `labelValueField/targetField`。
- 验证通过：目标文件 ESLint、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 43: 组织数据源、运行态样式和字段 DDL 同步修复

**目标**: 继续修复真实使用中暴露的组织列表不渲染、编辑页样式与设计器不一致、表单新增字段未同步数据库列的问题。

**问题清单**:
- 组织树字段如果保存了空 `optionSource`，运行态会跳过默认 `/system/org/tree` 数据源。
- 远程选项只读取 `res.data`，对 `RespInfo`、分页 records/list/rows、嵌套 data 等响应形态不够健壮。
- 编辑表单运行态缺少设计器行距、列距和标签右对齐参数，且 2 列表单默认弹窗宽度偏窄。
- 表单设计器拖入新字段后只保存字段注册表，未通过低代码受控 DDL 同步物理表缺失列。

**执行状态**: completed
- `AiFormItem` 对空/无效 `optionSource` 增加回退逻辑，组织选择默认走 `/system/org/tree`，并增强远程响应解包和 tree option 字段兜底。
- `AiForm` / `AiCrudPage` / 动态 CRUD 页面支持编辑表单 `labelAlign`、`editXGap`、`editYGap`，运行态默认标签右对齐、行列间距 16。
- `FormDesignerSchema`、form-create 双向转换、前后端编译链路保存 `rowGap/columnGap`，后端运行配置输出更接近设计器布局；2 列编辑表单默认弹窗宽度提升到 `1040px`。
- `BusinessObjectDesignerDTO` 新增 `syncDdl/confirmSyncDdl`，`BusinessObjectDesignerService.saveDesigner` 在表单保存后使用 `LowcodeDdlService` 预览并执行缺失表结构同步，继续校验 `ai:lowcode:deploy-ddl` 权限。
- `BusinessFormDesigner` 保存表单时请求同步 DDL，新增字段保存后会同步缺失数据库列。
- 验证通过：目标文件 ESLint、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 44: 表单布局组件与保存入口收敛

**目标**: 修复 fcDesigner 布局组件保存/回显异常、运行态编辑表单未按设计布局渲染、表单设计页保存按钮过多的问题。

**问题清单**:
- fcDesigner 的 `fcRow/col/elCard/elTabs/elCollapse/elDivider` 等布局节点会被转换成普通字段，重新进入设计器后布局退化。
- 编辑页运行态只消费扁平 `editSchema`，卡片、标签页、折叠面板、栅格行列等设计布局无法还原。
- 表单设计页同时存在顶部全局“保存”、表单面板“保存表单”、设计器工具栏“应用表单配置”，保存入口重复。
- 表单优先编译后仍可能受旧 canvas 排序影响，导致字段顺序和设计器顺序不一致。

**执行状态**: completed
- `FormDesignerSchema` 将布局/辅助组件归一为虚拟节点，form-create 双向转换保留原始类型、children、style/native/wrap 等元数据，不再生成伪字段。
- `BusinessFormDesigner` 和后端编译链路生成 `formLayout` 树，并把 row/col 继承 span 编译到 `fieldSettings`；表单优先编译会清理旧 `canvas`。
- `LowcodeRuntimeConfigBuilder` 下发 `options.editFormLayout`，动态 CRUD 页面把布局树与字段 schema 合并。
- `AiForm` 新增递归布局渲染，支持 row/col/card/tabs/collapse/divider，同时继续复用原有 `AiFormItem` 字段渲染和校验。
- 移除表单面板“保存表单”和设计器工具栏“应用表单配置”，只保留对象设计器顶部全局“保存”。
- 验证通过：目标文件 ESLint、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 45: 应用入口、设计器弹层和多列表单回归修复

**目标**: 修复应用入口挂载菜单后的重复 tab、对象设计器新开页面污染 tab、动态搜索区按钮错位，并补齐表单两列/三列配置到运行态适配。

**问题清单**:
- `/app-center/app/:appId` 作为应用入口桥接页会先登记“应用入口”tab，再 `replace` 到实际动态页面，形成重复 tab。
- 动态 CRUD 页面作为唯一 tab 时无法关闭，用户从菜单打开客户页后只能关闭桥接入口 tab。
- 对象设计器从应用总览、套件页、对象详情页进入时走路由跳转，顶部会堆积多个设计器 tab。
- `AiForm` 搜索操作区独立开 grid 后落到第一列，导致“搜索/重置/展开”按钮靠左。
- 表单设计缺少显式的一列/两列/三列控制，三列表单运行态弹窗宽度也偏窄。

**执行状态**: completed
- `BusinessAppEntry` 和 `BusinessObjectDesigner` 路由增加 `skipTab`，tab guard 会跳过桥接/设计器路由并清理遗留桥接 tab。
- 动态 CRUD tab 增加 `forceClosable`，顶部 tab 组件改为按 tab 计算关闭能力，客户动态页即使是唯一业务 tab 也可以关闭。
- 应用总览、套件详情、对象详情的“设计对象”改为直接挂载 `object-designer.[objectCode].vue` 全屏弹层，不再通过路由新开 tab；设计器直接 URL 访问仍保持兼容。
- `AiForm` 搜索操作区跨整行右对齐，折叠按钮判断改为基于可见字段。
- `BusinessFormDesigner` 增加“表单列数”控制，保存到 `FormDesignerSchema.layout.gridColumns`，发布运行态继续映射到 `editGridCols/formLayout`；三列表单默认弹窗宽度提升到 `1180px`。
- 验证通过：目标文件 ESLint、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 46: fcDesigner 画布列数和布局组件中文化修复

**目标**: 修复表单设计器中“单列/两列/三列”只影响运行态、不影响 fcDesigner 画布的问题，并让布局组件拖入后显示中文业务化名称。

**问题清单**:
- `FormDesignerSchema.layout.gridColumns` 只保存了业务列数，但 fcDesigner 画布实际按 form-create rule 的 `col.span` 渲染，导致画布一直看起来像单列。
- 拖入 `fcRow/col/elCard/elTabs/elCollapse/elDivider` 等布局组件后，部分节点标题或回显名称保留英文/原始组件名。
- `col.props.span=12` 属于 form-create 24 栅格，转换成 Forge schema 时不能直接当成业务列跨度，否则两列布局回显会被撑成整行。
- 空栅格布局保存时，后端递归编译运行态字段会把 `null inheritedSpan` 误拆箱，导致 `PUT /ai/business/object/{objectId}/designer` 空指针。

**执行状态**: completed
- `BusinessFormDesigner` 切换表单列数时，立即把当前 schema 组件重算为对应的 form-create `col.span`：单列 24、两列 12、三列 8。
- `BusinessFormCreateDesigner` 重置字段和追加字段时保留当前列数，并向 fcDesigner 显式传入中文 locale。
- `FormDesignerSchema` 增加布局组件中文默认名称和英文/原始名称归一化，保存、回显和拖入后的布局节点统一显示中文。
- `formCreateToForge` 读回 fcDesigner 规则时也会按当前列数重算布局，避免 `col.props.span` 被误解释成业务列跨度。
- `BusinessObjectDesignerService` 修复 `collectRuntimeFormFields` 中 `inheritedSpan` 三元表达式拆箱问题，空布局容器不再触发 NPE。
- 验证通过：目标文件 ESLint、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 47: 删除字典组件后残留字段校验修复

**目标**: 修复 fcDesigner 删除字典/引用组件后，保存仍提示“字典字段必须配置字典类型”或引用字段配置不完整的问题。

**问题清单**:
- fcDesigner 删除画布组件只移除 `FormDesignerSchema.components`，不会自动删除字段资产注册表中的历史字段。
- 表单保存时会把全部主表业务字段提交给后端，旧的 `DICT/SELECT/RADIO/CHECKBOX/CASCADER/REFERENCE` 字段如果已经不在当前画布或已改成普通输入，仍可能触发字段类型必填校验。
- 仅靠前端归一化不够，旧页面缓存、其它保存入口或历史数据仍可能绕过前端处理。

**执行状态**: completed
- `BusinessFormDesigner` 保存前按当前 `FormDesignerSchema` 建立字段到画布组件的映射；字段已移出画布且未配置字典/引用，或字段在画布上已切回普通组件时，自动降级为普通文本字段。
- 前端降级时同步清理 `dictType`、`referenceObjectCode`、`referenceDisplayField`，并把 `componentType/queryType` 调整为 `input/like`。
- `BusinessObjectDesignerService.saveDesigner` 在重建模型字段前增加后端兜底，对提交字段和当前表单 schema 做同样归一化，避免残留字段资产再次触发后端校验。
- 验证通过：`BusinessFormDesigner.vue` ESLint、`git diff --check`、`mvn -pl forge-admin-server -am compile -DskipTests`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。

### Task 48: fcDesigner 栅格布局临时 ref 展示修复

**目标**: 修复栅格布局、栅格列等布局组件前显示 `ref_Fs5x...` 这类 fcDesigner 临时引用串的问题。

**问题清单**:
- fcDesigner 会给布局 rule 生成 `ref_...` 临时 `id/name/title`。
- 布局组件不是业务字段，但 form-create 转 Forge schema 时可能把临时 ref 当成布局标题或组件 id 保存。
- Forge schema 回写到 fcDesigner 时，非字段布局组件继续写入 `name=component.id`，导致画布上再次显示临时 ref。

**执行状态**: completed
- `FormDesignerSchema` 增加 `ref_...` 临时引用识别，布局标题归一化时会剥离临时 ref，旧 schema 中保存的临时布局 id 也会被替换为稳定 `cmp_<componentKey>_<index>`。
- `formCreateToForge` 转换布局组件时跳过临时 `ref_...` 作为标题/id，字段编码生成也不再把 `ref_...` 当作有效字段名。
- `forgeToFormCreate` 回写布局 rule 时不再给非字段组件写 `name`，避免 fcDesigner 把布局 id 当作展示前缀。
- 验证通过：目标文件 ESLint、`git diff --check`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。
