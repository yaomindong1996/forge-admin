# 低代码对象设计器字段同步与交互修复
> status: implemented
> created: 2026-06-13
> scope: `forge-admin-ui`, `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator`
> related: `code-copilot/changes/form-first-business-object-designer/spec.md`

## 1. 背景

低代码应用构建的业务对象设计器已经采用表单优先模式，表单画布负责生成字段资产，字段资产负责高级维护。当前实际使用中存在以下影响设计效率和运行态一致性的问题：

1. 表单设计器新增字段时，“组件字段ID”只能从已有字段中选择，不能直接输入新字段 ID，导致用户无法在拖入组件时明确字段编码。
2. 表单设计与字段资产的双向同步不完整：表单新增字段要沉淀为字段资产，字段资产改名或改字段编码后也应同步回表单设计器的绑定与展示。
3. 切换左侧导航时经常提示未保存，即使用户没有做业务修改，说明初始化、预览补水或内部同步触发了错误的 dirty 状态。
4. 字段配置当前使用右侧抽屉弹层，编辑字段时遮挡上下文，且频繁开关影响效率。
5. 表单设计器新增字段并同步 DDL 后，数据库列已经存在，但运行态新增/编辑数据时该字段值没有入库。
6. 曾经拖入后又删除的自动字段仍残留在模型字段中，发布检查继续提示追加已不在表单中的字段列。
7. 新版画布“显示名称”清空后会立刻被规范化逻辑自动回填，用户不能一次删除完整名称。

## 2. 目标

- 表单设计器中的“组件字段ID”支持选择已有字段，也支持手动输入新字段 ID。
- 手动输入的新字段 ID 在保存/同步设计草稿时自动沉淀到字段资产，字段名称、列名、类型、组件属性沿用现有自动字段资产规则。
- 字段资产保存后同步更新本地模型、表单 Schema、页面 Schema 和视图 Schema 中的字段引用；服务端保存后的重载仍作为最终一致性来源。
- 表单设计器仅在真实 schema/page/field 变更时上报 dirty，初始化、setRule、预览选项补水不触发未保存提示。
- 字段资产页改为列表 + 右侧内嵌属性面板，不再打开 Naive UI Drawer。
- 运行态新增/编辑数据时，设计器刚保存且已建表列的新字段能够被动态 CRUD 正常写入。
- 表单删除自动创建字段后，该字段不再参与模型 Schema、DDL 预览和运行态写入。
- 字段显示名称允许临时清空，保存前不因内部规范化立刻回填。
- 必填字段的数据库非空约束只在配置了可用默认值时同步，避免无默认值必填字段阻断运行态新增。

## 3. 非目标

- 不修改后端字段设计器接口协议。
- 不重写 `fcDesigner`。
- 不重写发布链路和运行态 `AiCrudPage`；动态 CRUD 仅修正新增字段写入白名单。
- 不新增数据库迁移脚本。
- 不处理字段编码变更后的历史数据迁移策略，仍沿用当前后端字段设计服务的处理。

## 4. 设计方案

### 4.1 字段 ID 输入

`forgeBusinessComponents.js` 中的字段绑定属性规则继续使用 `select` 类型，但开启可创建输入能力。用户可以：

- 从已有字段资产中选择字段 ID。
- 直接输入新字段 ID。

`formCreateToForge.js` 已经会从 `rule.props.fieldBinding.fieldCode`、`rule.props.fieldCode`、`rule.field`、`rule.name` 中解析字段编码；本变更保持该转换链路，只补齐属性规则的输入能力。

### 4.2 双向同步

表单到字段资产：

- `BusinessFormDesigner.syncDesignerDraft()` 继续通过 `buildAutoFieldAssets()` 从 `FormDesignerSchema` 自动生成字段资产。
- 手动输入字段 ID 后，组件 `fieldBinding.fieldCode` 作为稳定字段编码，自动生成字段资产。

字段资产到表单：

- 字段资产保存时，如果字段编码发生变化，前端在本地同步替换表单 Schema、页面 Schema、视图 Schema 的引用。
- 字段名称、提示文案、必填、控件类型等仍由现有字段保存和设计器重载兜底同步。

### 4.3 Dirty 状态

`BusinessFormCreateDesigner.flushDesigner()` 增加结构比较：

- 转换出的 Forge Schema 与当前 `modelValue` 等价时，只刷新内部快照，不触发 `update:modelValue` 和 `dirtyChange(true)`。
- 仅用户真实拖拽、删除、排序、改字段绑定或修改属性后才上报 dirty。

`BusinessFormDesigner.syncDesignerDraft()` 增加比较：

- 当前表单 Schema、字段资产和页面 Schema 均未变化时返回 `dirty: false`。
- 左侧导航切换只同步草稿，不制造未保存状态。

### 4.4 字段配置交互

`BusinessFieldManager.vue` 去掉抽屉，改为内嵌工作区：

- 左侧保持字段列表和筛选工具栏。
- 右侧固定展示 `BusinessFieldPropertyPanel`。
- 未选中字段时在右侧展示空状态。
- 小屏幕下列表与属性面板纵向堆叠。

### 4.5 运行态新增字段写入

`DynamicCrudService` 的新增和更新写入白名单不再只依赖 `editSchema`：

- `editSchema` 继续作为运行态表单字段白名单。
- `modelSchema` 额外补充设计器保存后已存在真实数据库列的新字段。
- 仅允许启用、非系统、非主键、非自增、非只读、表单可见且数据库列存在的字段写入。
- 继续过滤 `id`、租户、创建人、更新时间等不可变系统字段，避免请求体任意字段透传。

### 4.6 自动字段清理与默认值策略

表单设计器保存草稿时，`buildAutoFieldAssets()` 只保留当前表单仍绑定的自动创建字段；未绑定的手工字段资产继续保留，避免高级字段资产被误删。

新版画布 schema 规范化时区分“没有 label”和“用户显式清空 label”：字段组件显式传入空字符串时保留空值，不再回退到“字段”。

DDL 同步时，业务字段的 `required` 主要作为运行态表单校验。只有当字段配置了非空默认值，才把数据库列同步为 `NOT NULL DEFAULT ...`；未配置默认值时数据库列保持可空。

## 5. 影响范围

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/fieldReferenceUtils.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/autoFieldRegistry.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java`

## 6. 验收标准

- 在表单设计器拖入输入组件后，可以在属性面板直接输入 `customerLevel` 作为组件字段 ID。
- 保存表单设计后，字段资产中出现 `customerLevel`，字段编码与数据库列名分别为 `customerLevel`、`customer_level`。
- 在字段资产中把字段编码从 `customerLevel` 改为 `customerGrade` 后，表单设计器组件绑定同步为 `customerGrade`，页面/视图引用不遗留旧字段编码。
- 只进入设计器并切换左侧导航，不弹出“未保存变更”提示。
- 字段资产页选择字段后直接在页面右侧编辑属性，不出现右侧抽屉弹层。
- 拖入 form-create 默认输入组件时，`rule.field = input`、`select` 等设计器内部默认值不能沉淀为业务字段编码或数据库列名；新字段应生成 `fieldInput1` 等稳定业务字段编码。
- 组件绑定已有字段资产后，表单组件标题优先回显字段资产名称，例如选择 `customerLevel` 时标题同步为“客户等级”。
- 表单设计器新增字段并同步 DDL 后，运行态新增/编辑记录时该字段值能保存到对应数据库列。
- 表单里删除自动创建字段后，发布检查不再提示追加该字段对应的数据表列。
- 新版画布字段“显示名称”可以一次性清空，输入过程中不自动回填。
- 必填字段未配置默认值时，DDL 不生成 `NOT NULL` 约束；配置默认值后才生成非空默认值约束。
- 前端构建、ESLint 或后端目标模块编译等对应变更范围的校验通过；如因既有问题无法全量通过，需在执行日志记录失败点和本次变更无关性。
