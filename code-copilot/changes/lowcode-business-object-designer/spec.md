# 低代码业务对象设计器主链路重构
> status: apply
> created: 2026-05-29
> complexity: 🔴复杂

## 1. 背景与目标

当前 Forge 低代码能力已经具备数据模型、页面搭建、发布、动态 CRUD、代码预览、业务应用中心等底层能力，但产品主链路仍明显偏“技术模型/代码生成器”：

- 普通业务用户一上来看到“业务域与数据模型”“数据模型设计”“模型编码 / 数据表名”“DDL 预览”“保存并同步表结构”等概念。
- 用户需要理解模型、表名、Schema、configKey、发布配置，才能把一个业务对象变成可运行页面。
- 业务对象、字段、表单、列表、详情、关系、操作这些低代码用户真正关心的概念，没有成为第一入口。
- 业务应用中心已经有“业务套件/业务对象”的产品语言，但点击对象后仍会回到“配置模型/配置布局/发布应用”的技术路径。

正常低代码平台的主体验应当是：

`创建业务对象 → 设计业务字段 → 设计表单/列表/详情 → 配置关系/操作/权限/流程 → 发布应用`

用户不需要先理解“数据模型”。用户只需要知道“客户有哪些字段”“客户表单怎么排版”“客户详情里能看到联系人和跟进记录”“谁可以看和改”。平台内部再把这些业务配置转换为低代码模型、物理表、动态 CRUD 配置和运行时页面。

本变更目标是把 Forge 低代码从“模型驱动的代码生成器体验”调整为“字段驱动的业务对象搭建器体验”：

- 面向业务和实施人员，新增业务对象设计器作为主链路。
- 保留现有 `ai_lowcode_model`、`ai_crud_config`、`DynamicCrudController`、`LowcodeRuntimeConfigBuilder` 等底层能力。
- 将“数据模型、表名、DDL、Schema、configKey”收敛到高级配置或开发者模式。
- 字段管理成为核心入口，字段变更自动驱动模型 Schema、表单 Schema、列表 Schema、运行配置和发布检查。
- 业务对象设计器采用“左侧对象配置导航 + 中间表单/列表设计画布 + 右侧字段列表/属性面板”的工作台形态，参考传统低代码对象设计体验。

完成后，业务人员应能在“应用中心 → CRM → 客户 → 设计”中完成以下闭环：

- 新增客户字段，例如客户名称、客户等级、联系电话、所属地区、负责人、跟进状态。
- 将字段拖入表单分组，调整单列/双列布局和字段顺序。
- 配置列表查询条件、表格列、行操作和工具栏按钮。
- 配置客户与联系人、商机、跟进记录的业务关系。
- 一键发布客户管理应用，平台自动生成或更新运行配置。
- 普通模式下不出现 `model_schema`、`page_schema`、`configKey`、表名、DDL 等技术信息。

## 2. 代码现状（Research Findings）

### 2.1 现有低代码入口偏数据模型

- `forge-admin-ui/src/views/ai/lowcode-models.vue` 当前页面标题和导航使用“业务域与数据模型”“数据模型设计”“领域数据模型”等概念。
- `lowcode-models.vue` 顶部操作包括“新建模型”“导入数据表”“AI 生成模型”“DDL 预览”“保存并同步表结构”，说明当前主路径仍围绕数据模型和表结构。
- `lowcode-models.vue` 在左侧树中使用“域”“模型”作为资产类型，普通业务用户需要先理解业务域与模型层级。

### 2.2 已有字段、关系和页面设计能力，但产品语言仍技术化

- `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue` 已有“字段设计”“关联配置”“校验规则”等能力，可复用为业务对象设计器的核心模块。
- `LowcodeModelDesigner.vue` 的“模型基础信息”中直接展示“模型编码 / 数据表名”“应用类型”等技术项，适合作为高级配置，不适合作为普通用户首屏。
- `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldTable.vue` 已支持字段名称、字段编码、字段说明、数据类型、表单组件、长度、小数位、必填、默认值、关联配置等字段属性。
- `ModelFieldTable.vue` 当前以表格方式编辑字段，字段编码、数据类型、长度、小数位等技术属性过于靠前；需要改成业务字段列表 + 右侧属性面板，高级属性折叠。
- `LowcodeModelDesigner.vue` 已有“关联配置”页签，支持关系类型、关联对象、本模型字段、关联对象字段、回显字段，但需要用业务对象语言重新包装，并与应用中心对象关系配置统一。

### 2.3 已有页面搭建与运行配置生成能力

- `forge-admin-ui/src/components/lowcode-builder/page/LowcodePageBuilder.vue` 已支持“列表页面”“表单与详情”两个设计区域。
- `LowcodePageBuilder.vue` 支持自由布局、结构化模式、标准单表、左树右表、主子表等布局类型。
- `LowcodePageBuilder.vue` 通过 `syncPageSchemaWithModel` 将页面 Schema 与模型字段同步，说明字段驱动页面配置已经具备基础能力。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java` 已能将 `LowcodeModelSchema` + `LowcodePageSchema` 转成 `AiCrudPage` 运行时配置。
- `LowcodeRuntimeConfigBuilder` 已生成 `searchSchema`、`columnsSchema`、`editSchema`、`apiConfig`、`options`、字典、脱敏、加密、转换等配置，可作为业务对象发布的内部转换器继续复用。

### 2.4 后端接口仍以低代码模型为中心

- `LowcodeModelController` 暴露 `/ai/lowcode/model/page`、`/list`、`/{id}`、`/validate`、`/ddl/preview`、`/preview-db-table`、`/import-db-table` 等接口。
- `LowcodeModelController` 的语义是“低代码数据模型接口”，适合开发者模式和内部资产管理，不应作为业务用户主入口。
- `LowcodeModelController` 已具备模型保存、状态启停、校验、DDL 预览、数据库表导入能力，可被业务对象设计器服务层复用。

### 2.5 发布链路可复用，但入口需要前移到业务对象

- `LowcodePublishService` 已负责发布、版本、回滚和菜单注册。
- `LowcodePublishService.publish` 已从模型 Schema 和页面 Schema 生成运行时配置，更新 `ai_crud_config` 并同步业务运行入口。
- `LowcodePublishService.ensureTableReady` 当前要求表存在或在线建表权限，适合作为业务对象发布检查的一部分，但普通用户不应直接看到 DDL 细节。
- `LowcodePublishService.syncBusinessRuntimeEntry` 已能把低代码发布结果同步到业务应用入口，说明“业务对象 → 运行入口”的后半段链路已经存在。

### 2.6 业务应用中心已经具备承载主链路的壳

- `forge-admin-ui/src/views/app-center/index.vue` 已提供应用中心、业务套件、业务对象、应用入口等产品入口。
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue` 已有对象详情、运行态提示、配置模型、配置布局、发布应用、关系、能力页签。
- 当前对象详情仍把用户引导到“配置模型/配置布局/发布应用”，需要收敛为一个“设计对象”入口，并在对象设计器里完成字段、表单、列表、关系和发布。

## 3. 产品定位

### 3.1 业务用户视角

业务用户不理解也不需要理解数据模型、物理表、Schema、DDL。业务用户关心：

- 这个系统有哪些业务对象，例如客户、合同、回款、跟进记录。
- 每个对象有哪些字段。
- 新增/编辑表单如何排版。
- 列表如何查询、展示、排序、导入和导出。
- 详情页能看到哪些关联数据。
- 哪些按钮和操作可用。
- 谁能看、谁能改、是否需要审批。

### 3.2 实施人员视角

实施人员需要快速把业务语言落成可运行应用：

- 从模板、AI 描述、Excel/数据库表导入或空白对象开始。
- 维护字段、布局、关系、操作、权限、流程。
- 发布前看到缺口检查，例如字段编码冲突、必填字段缺少表单组件、关系目标未发布、数据表未同步。
- 必要时进入高级配置查看表名、字段编码、Schema 和发布配置。

### 3.3 开发人员视角

开发人员继续使用底层低代码和代码生成能力：

- `ai_lowcode_model` 仍作为模型事实来源。
- `ai_crud_config` 仍作为动态 CRUD 运行配置事实来源。
- `DynamicCrudController`、`AiCrudPage`、`LowcodeRuntimeConfigBuilder`、`LowcodePublishService` 继续复用。
- 代码生成、DDL 预览、JSON 导入导出、Schema 调试进入高级配置或开发者菜单。

## 4. 范围

### 4.1 本阶段必须完成

- 新增“业务对象设计器”主入口，替代普通用户直接进入“数据模型设计”的路径。
- 对象设计器至少包含：基本信息、字段管理、表单设计、列表设计、详情设计、关系配置、自定义操作、权限/流程、发布检查、高级配置。
- 字段管理成为核心工作区，支持业务字段新增、编辑、复制、排序、删除、隐藏、启停。
- 表单设计采用类截图的搭建器形态：左侧对象配置导航，中间表单布局画布，右侧字段列表和添加字段入口。
- 字段属性分为普通属性和高级属性。普通属性面向业务用户，高级属性才展示字段编码、数据库列名、长度、小数位、索引等。
- 表单、列表、详情设计必须自动跟随字段变化同步，不允许出现 `undefined` 字段、空 label 或脏字段引用。
- 关系配置必须使用业务语言，例如“客户有多个联系人”“合同属于客户”，同时映射到底层关系字段。
- 发布流程必须从业务对象设计器发起，内部复用现有低代码发布能力。
- 应用中心对象详情页把“配置模型/配置布局/发布应用”收敛为“设计对象/发布对象/运行应用”。
- 原“数据模型管理”保留，但定位为开发者高级入口，不作为业务搭建主入口。
- 从模板、数据库导入、AI 生成、空白创建四种入口都必须生成业务对象草稿，并进入业务对象设计器继续编辑。

### 4.2 本阶段不做

- 不重写 `DynamicCrudController`。
- 不重写 `AiCrudPage`。
- 不推翻 `ai_lowcode_model`、`ai_crud_config` 事实来源。
- 不一次性做完整移动端搭建器。
- 不一次性做完整流程设计器、报表设计器或复杂规则引擎。
- 不把字段设计拆成独立物理字段表作为唯一事实来源，首期仍优先复用 JSON Schema。
- 不让普通业务用户直接维护数据库索引、SQL、DDL、JSON Schema。

## 5. 功能点

### 5.1 信息架构与入口重构

- [ ] 应用中心业务对象卡片新增主按钮“设计对象”，进入 `/app-center/object/:objectCode/designer` 或等价路由。
- [ ] 对象详情页保留“运行应用”“查看就绪度”“配置能力”等入口，但设计链路统一进入业务对象设计器。
- [ ] 低代码模型管理从业务用户菜单中下沉到“开发者工具/高级配置/模型资产”。
- [ ] 普通模式下页面标题使用“业务对象设计”“字段管理”“表单设计”，不使用“数据模型设计”作为主标题。
- [ ] 开发者模式可以显示“模型编码、表名、Schema、configKey、DDL 预览、同步表结构”。

### 5.2 业务对象设计器框架

- [ ] 设计器左侧为对象配置导航：基本信息、字段管理、表单设计、列表设计、详情设计、关系配置、自定义操作、权限流程、发布检查、高级配置。
- [ ] 设计器顶部显示对象名称、所属套件、设计状态、发布状态、最后保存时间、最后发布时间。
- [ ] 设计器顶部提供保存、预览、发布、更多操作。
- [ ] 设计器主体根据左侧导航切换，不使用大量纵向堆叠。
- [ ] 未保存变更离开页面时必须提示。
- [ ] 保存失败必须定位到具体字段、布局项或关系配置。

### 5.3 基本信息

- [ ] 普通用户维护对象名称、对象说明、所属套件、对象图标、对象分类、启停状态。
- [ ] 对象编码默认根据对象名称自动生成，普通模式只在“高级”中展示。
- [ ] 支持对象显示字段选择，例如客户对象显示“客户名称”。
- [ ] 支持对象默认排序字段选择，例如创建时间倒序。
- [ ] 支持对象模板标记，用于后续复用。

### 5.4 字段管理

- [ ] 支持新增字段，字段类型包括文本、多行文本、数字、金额、日期、日期时间、下拉、单选、多选、开关、附件、图片、人员、部门、地区、引用对象。
- [ ] 字段新增时只要求填写字段名称和字段类型，字段编码、数据库列名自动生成。
- [ ] 字段属性普通区包括：字段名称、字段类型、是否必填、默认值、提示文案、是否显示在表单、是否显示在列表、是否作为查询条件、是否导入导出。
- [ ] 字段属性高级区包括：字段编码、数据库列名、数据类型、长度、小数位、唯一约束、索引、脱敏、加密、字典类型、转换配置。
- [ ] 系统字段固定展示但不可删除，例如创建人、创建时间、修改人、修改时间、所属用户、所属部门。
- [ ] 字段支持拖拽排序，排序会影响默认表单和列表布局。
- [ ] 字段删除前必须判断是否已发布、是否已有数据、是否被表单/列表/详情/关系/流程/触发器引用。
- [ ] 已发布字段默认不物理删除，优先进入隐藏或停用状态；需要物理删除时进入高级危险操作。
- [ ] 字段重命名只改展示名称，不默认改字段编码和数据库列。
- [ ] 字段类型变更必须提示数据迁移风险，已发布且有数据时需要二次确认或禁止直接变更。

### 5.5 表单设计

- [ ] 表单设计采用画布模式，支持分组、两列/三列布局、字段拖拽、字段隐藏、字段只读、字段校验。
- [ ] 右侧字段列表展示未使用字段、系统字段、已使用字段状态，并支持“添加字段”。
- [ ] 支持从字段列表拖拽到表单画布。
- [ ] 支持基础信息、营销数据、联系人信息等分组标题。
- [ ] 支持字段组件属性配置，例如占位文案、宽度、默认值、联动显示、必填校验。
- [ ] 支持保存为新增/编辑表单布局。
- [ ] 支持手机端表单自动适配预览，但首期不做完整移动端设计器。
- [ ] 表单设计保存时必须校验字段引用存在，缺失字段自动提示修复。

### 5.6 列表设计

- [ ] 支持配置查询条件：字段、控件类型、默认值、排序、是否折叠。
- [ ] 支持配置表格列：字段、列名、宽度、固定列、排序、字典标签、金额展示、日期格式。
- [ ] 支持配置工具栏按钮：新增、导入、导出、自定义查询、批量删除。
- [ ] 支持配置行操作：查看、编辑、删除、自定义操作、发起审批。
- [ ] 支持紧凑模式和标准模式。
- [ ] 支持从字段管理自动生成默认列表，但用户可继续调整。

### 5.7 详情设计

- [ ] 支持详情基础字段分组展示。
- [ ] 支持详情页签：基本信息、关联数据、操作日志、审批记录。
- [ ] 支持在详情中嵌入关联列表，例如客户详情显示联系人、商机、跟进记录。
- [ ] 支持详情字段只读、隐藏、排序、分组。
- [ ] 支持详情页关联入口跳转到目标对象运行页并带入筛选条件。

### 5.8 关系配置

- [ ] 支持业务化关系类型：属于、拥有多个、引用、明细。
- [ ] 配置文案使用“客户有多个联系人”“合同属于客户”，不直接以外键和 Join 作为主文案。
- [ ] 关系配置需要保存目标对象、当前对象字段、目标对象字段、回显字段、详情页签名称、默认筛选条件。
- [ ] 关系配置与 `LowcodeModelSchema.relations` 和 `ai_business_object_relation` 保持同步或明确映射。
- [ ] 模型关系和业务对象关系冲突时必须提示差异，提供修复动作。

### 5.9 自定义操作

- [ ] 支持配置工具栏操作、行操作和详情操作。
- [ ] 操作类型包括打开页面、调用接口、发起审批、执行触发器、打开外部链接。
- [ ] 操作必须支持权限标识、二次确认、成功提示、失败提示。
- [ ] 普通用户不直接填写接口 JSON；高级模式可配置请求参数映射。

### 5.10 权限、流程与自动化

- [ ] 对象设计器提供权限摘要：谁可见、谁可新增、谁可编辑、谁可删除、谁可导入导出。
- [ ] 流程配置以“是否需要审批”“绑定哪个流程”“哪些状态可发起”为业务语言展示。
- [ ] 自动化配置首期只接入已有触发器/消息能力，不新增复杂规则引擎。
- [ ] 权限、流程、自动化状态进入发布检查和就绪度。

### 5.11 发布检查与发布

- [ ] 发布前检查字段、表单、列表、详情、关系、数据表、运行配置、权限。
- [ ] 检查项必须区分通过、警告、阻断。
- [ ] 阻断项必须提供修复入口，例如“字段缺少名称”“表单引用了已删除字段”“数据表未同步”。
- [ ] 发布动作内部调用现有低代码发布链路，生成或更新 `ai_crud_config`。
- [ ] 发布成功后自动创建或更新业务应用入口。
- [ ] 发布成功后可直接“打开应用”。
- [ ] 支持版本记录和回滚，复用现有版本能力。

### 5.12 高级配置与开发者模式

- [ ] 高级配置展示模型编码、表名、字段编码、数据库列名、Schema 预览、DDL 预览、configKey、API 配置。
- [ ] 高级配置需要权限控制，普通业务用户不可见。
- [ ] JSON 导入导出、数据库表导入、在线建表、代码预览、ZIP 下载全部放入高级或开发者入口。
- [ ] 高级配置的变更必须回写业务对象设计器，不能形成两套互不生效的配置。

### 5.13 模板、AI 和数据库导入统一输出

- [ ] 空白创建输出业务对象草稿。
- [ ] 模板创建输出业务对象、字段、表单、列表、关系的草稿。
- [ ] AI 描述生成输出业务对象草稿，而不是直接把用户带到数据模型管理。
- [ ] 数据库表导入面向实施/开发者，导入后也进入业务对象设计器，用业务字段语言继续编辑。
- [ ] 所有入口最终都进入同一个对象设计器和发布链路。

## 6. 业务规则

- 普通模式禁止展示 `model_schema`、`page_schema`、`configKey`、DDL、表名作为主信息。
- 字段名称不能为空，字段编码必须唯一且稳定。
- 字段编码和数据库列名自动生成后，已发布对象默认不随字段名称变更自动改名。
- 页面渲染不允许出现 `undefined`、`null`、空字段名、空列标题。
- 系统字段由平台维护，普通用户不可删除。
- 字典类字段必须绑定字典，不在前端硬编码选项。
- 金额字段统一按分存储，展示层可以按元格式化。
- 人员、部门、地区字段必须复用现有人员、部门、行政区划能力。
- 引用对象字段必须有明确目标对象和回显字段。
- 删除或变更已发布字段必须经过引用检查和数据风险提示。
- 业务对象发布后才能成为业务应用入口的正常可打开状态。
- 开发者高级配置可以暴露技术细节，但所有技术变更必须回流到业务对象设计器。
- 查询 SQL 仍按项目规范写在 Mapper XML 中，不能在 Service 中拼复杂查询。
- 所有内置数据 `tenant_id=1`。

## 7. 数据变更

首期优先复用现有事实来源，避免新增一套字段事实表：

- `ai_business_object`：业务对象主资产，承载用户入口和业务语义。
- `ai_lowcode_model`：字段、关系、策略等模型 Schema 的事实来源。
- `ai_crud_config`：发布后的运行配置事实来源。
- `ai_crud_config_version`：发布版本和回滚事实来源。
- `ai_business_object_relation`：业务应用中心关系展示和运行入口事实来源。

建议补充以下结构：

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 扩展 | `ai_business_object` | `design_status` | 设计状态：DRAFT/READY/PUBLISHED/ERROR |
| 扩展 | `ai_business_object` | `config_key` | 当前对象默认运行配置 key，便于从对象进入运行页 |
| 扩展 | `ai_business_object` | `last_publish_time` | 最近发布时间 |
| 扩展 | `ai_business_object` | `last_publish_version` | 最近发布版本号 |
| 扩展 | `ai_business_object` | `designer_options` JSON | 设计器偏好和高级配置开关，不保存敏感信息 |
| 新增 | `ai_business_object_design_version` | `object_id`, `version_no`, `model_schema_snapshot`, `page_schema_snapshot`, `relation_schema_snapshot`, `publish_status` | 业务对象维度设计版本，可与 `ai_crud_config_version` 互相追溯 |
| 新增 | `ai_business_field_template` | `template_code`, `template_name`, `field_schema`, `suite_code`, `status` | 可选字段模板库，例如客户电话、负责人、地区 |

数据脚本要求：

- Flyway 脚本放在 `forge/db/migration/`。
- 新增列前检查 `information_schema`。
- 新表使用 `CREATE TABLE IF NOT EXISTS`。
- 内置数据使用 `INSERT ... SELECT ... WHERE NOT EXISTS`。
- 所有业务内置数据 `tenant_id=1`。
- 不修改已经执行过的历史迁移脚本。

## 8. 接口变更

### 8.1 业务对象设计器

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/object/{objectId}/designer` | GET | 获取业务对象设计器完整草稿 |
| 新增 | `/ai/business/object/{objectId}/designer` | PUT | 保存业务对象设计器完整草稿 |
| 新增 | `/ai/business/object/{objectId}/designer/basic` | PUT | 保存对象基础信息 |
| 新增 | `/ai/business/object/{objectId}/designer/dirty-check` | GET | 查询是否有未发布变更 |

### 8.2 字段管理

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/object/{objectId}/fields` | GET | 查询业务字段 |
| 新增 | `/ai/business/object/{objectId}/fields` | POST | 新增业务字段 |
| 新增 | `/ai/business/object/{objectId}/fields/{fieldCode}` | PUT | 修改业务字段 |
| 新增 | `/ai/business/object/{objectId}/fields/{fieldCode}` | DELETE | 删除或停用业务字段 |
| 新增 | `/ai/business/object/{objectId}/fields/reorder` | POST | 字段排序 |
| 新增 | `/ai/business/field-template/list` | GET | 查询字段模板 |

### 8.3 页面设计

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/object/{objectId}/layout/form` | PUT | 保存表单布局 |
| 新增 | `/ai/business/object/{objectId}/layout/list` | PUT | 保存列表布局 |
| 新增 | `/ai/business/object/{objectId}/layout/detail` | PUT | 保存详情布局 |
| 新增 | `/ai/business/object/{objectId}/layout/preview` | POST | 预览页面运行配置 |

### 8.4 关系、操作、权限和发布

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 增强 | `/ai/business/object/{objectId}/relations` | GET/POST | 关系配置与模型关系同步 |
| 新增 | `/ai/business/object/{objectId}/actions` | GET/POST/PUT/DELETE | 自定义操作配置 |
| 新增 | `/ai/business/object/{objectId}/permission-summary` | GET | 查询对象权限摘要 |
| 新增 | `/ai/business/object/{objectId}/publish-check` | GET | 发布检查 |
| 新增 | `/ai/business/object/{objectId}/publish` | POST | 从业务对象设计器发布运行配置 |
| 新增 | `/ai/business/object/{objectId}/versions` | GET | 查询业务对象设计版本 |
| 新增 | `/ai/business/object/{objectId}/versions/{versionId}/rollback` | POST | 回滚业务对象设计版本 |

### 8.5 复用现有低代码接口

以下接口保留，供业务对象设计器服务层和开发者模式复用：

| 接口 | 说明 |
|------|------|
| `/ai/lowcode/model/*` | 模型资产管理、校验、DDL 预览、数据库表导入 |
| `/ai/lowcode/app/*` | 低代码应用草稿、预览、发布、版本 |
| `/ai/crud/{configKey}/*` | 动态 CRUD 运行时 |

## 9. 前端变更

| 页面/组件 | 变更 |
|----------|------|
| `src/views/app-center/object.[objectCode].vue` | 将配置模型、配置布局、发布应用收敛为“设计对象”主入口 |
| 新增 `src/views/app-center/object-designer.[objectCode].vue` | 业务对象设计器主页面 |
| 新增 `BusinessObjectDesignerShell.vue` | 设计器外壳：左侧导航、顶部工具栏、主体区域 |
| 新增 `BusinessFieldManager.vue` | 业务字段管理，普通属性优先，高级属性折叠 |
| 新增 `BusinessFormDesigner.vue` | 表单画布，复用/改造 `CanvasFormDesigner` |
| 新增 `BusinessListDesigner.vue` | 列表查询、表格列、工具栏和行操作配置 |
| 新增 `BusinessDetailDesigner.vue` | 详情分组、详情页签和关联列表配置 |
| 新增 `BusinessRelationDesigner.vue` | 业务语言关系配置，与模型关系同步 |
| 新增 `BusinessActionDesigner.vue` | 自定义按钮和操作配置 |
| 新增 `BusinessPublishChecklist.vue` | 发布检查和修复入口 |
| 新增 `BusinessAdvancedConfig.vue` | 开发者高级配置 |
| `LowcodeModelDesigner.vue` | 拆分可复用字段、关系、规则能力；普通入口不直接使用“数据模型设计”标题 |
| `ModelFieldTable.vue` | 从技术表格编辑逐步改为业务字段列表 + 属性面板；字段编码、列名、长度进入高级区 |
| `LowcodePageBuilder.vue` | 作为内部页面设计能力复用，业务入口包装为表单/列表/详情设计 |
| `BusinessObjectWizardDrawer.vue` | 四种创建方式统一跳转业务对象设计器 |
| `business-app.js` | 增加对象设计器、字段、布局、发布检查等 API |

## 10. 后端变更

推荐继续放在 `forge-plugin-generator` 的 `businessapp` 和 `lowcode` 包内，保持边界清晰。

| 类/服务 | 说明 |
|---------|------|
| `BusinessObjectDesignerController` | 业务对象设计器聚合接口 |
| `BusinessObjectDesignerService` | 组装业务对象、模型 Schema、页面 Schema、关系、发布状态 |
| `BusinessFieldDesignService` | 字段新增、编辑、删除、排序、引用检查 |
| `BusinessLayoutDesignService` | 表单、列表、详情布局保存和字段引用校验 |
| `BusinessObjectPublishService` | 面向业务对象的发布检查和发布门面 |
| `BusinessObjectDesignVersionService` | 业务对象设计版本记录和回滚 |
| `BusinessFieldTemplateService` | 字段模板管理 |
| `BusinessObjectRelationService` | 增强为业务关系和模型关系同步 |
| `LowcodeSchemaValidator` | 增强字段引用、空 label、重复 field、页面脏引用校验 |
| `LowcodeRuntimeConfigBuilder` | 继续作为业务对象发布的运行配置转换器 |
| `LowcodePublishService` | 继续负责最终发布、版本、菜单和业务入口同步 |

## 11. 菜单与权限

新增或调整权限：

| 权限标识 | 说明 |
|----------|------|
| `ai:businessObject:design` | 进入业务对象设计器 |
| `ai:businessObject:field` | 维护业务字段 |
| `ai:businessObject:layout` | 维护表单、列表、详情布局 |
| `ai:businessObject:relation` | 维护对象关系 |
| `ai:businessObject:action` | 维护自定义操作 |
| `ai:businessObject:publish` | 发布业务对象 |
| `ai:businessObject:advanced` | 查看和维护高级技术配置 |
| `ai:businessFieldTemplate:list` | 查看字段模板 |
| `ai:businessFieldTemplate:edit` | 维护字段模板 |

菜单策略：

- 普通业务菜单显示“应用中心”“业务套件”“业务对象设计”。
- “低代码模型管理”“CRUD 配置”“代码生成”迁移到开发者菜单或高级入口。
- 已有路由保留，避免破坏老链接，但普通用户菜单不再优先展示。

## 12. 迁移策略

### 12.1 第一批：入口和语言调整

- 应用中心对象详情新增“设计对象”入口。
- 对象设计器先复用现有低代码模型和页面设计组件。
- 普通模式隐藏模型编码、表名、DDL、configKey。
- 现有“配置模型/配置布局/发布应用”按钮逐步降级为高级入口。

### 12.2 第二批：字段驱动闭环

- 实现字段管理 API 和前端字段管理区。
- 字段变更自动同步模型 Schema 和页面 Schema。
- 增强字段引用校验，解决 `undefined` 字段和脏引用问题。
- 完成表单、列表、详情设计器的业务化包装。

### 12.3 第三批：发布和版本闭环

- 实现业务对象发布检查。
- 从业务对象设计器调用发布并同步业务应用入口。
- 增加业务对象设计版本记录。
- 支持业务对象维度回滚。

### 12.4 第四批：高级能力收敛

- 关系配置与模型关系、应用中心关系统一。
- 自定义操作、权限、流程、自动化纳入对象设计器。
- 字段模板、对象模板、AI 生成、数据库导入统一进入对象设计器。

## 13. 风险与关注点

- 产品定位风险：如果继续把数据模型放在首屏，平台仍会被用户理解成技术配置台。
- 元数据一致性风险：业务对象、低代码模型、页面 Schema、运行配置、业务应用入口之间必须保持同步。
- 字段变更风险：已发布字段删除、改类型、改编码可能造成数据丢失或运行异常。
- 双入口风险：如果模型管理和对象设计器都能独立改同一份 Schema，必须定义冲突检测和同步规则。
- 发布风险：业务对象发布涉及在线建表、字段变更和运行配置生成，需要明确权限和回滚。
- 权限风险：高级配置中包含表名、API、DDL、Schema，必须权限隔离。
- 体验风险：字段管理如果仍像数据库表设计，会继续劝退业务用户。
- 范围风险：本阶段聚焦业务对象设计器，不扩展完整移动端、报表、流程和复杂自动化设计器。

## 14. 测试策略

- **测试范围**：业务对象设计器入口、字段管理、表单设计、列表设计、详情设计、关系配置、发布检查、发布运行、版本回滚、高级权限。
- **后端验证**：`mvn -pl forge-admin-server -am compile -DskipTests`。
- **前端验证**：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui build`。
- **接口验证**：对象设计器详情、字段新增/修改/删除、布局保存、发布检查、发布接口。
- **页面验证**：应用中心对象详情、业务对象设计器、运行页、低代码模型开发者入口。
- **业务验收**：从 CRM 客户对象进入设计器，新增字段、拖入表单、配置列表列、发布、打开客户管理运行页。
- **脏数据验证**：删除字段后，表单、列表、详情和运行页不出现 `undefined`。
- **权限验证**：无高级权限用户看不到表名、DDL、Schema、configKey。
- **独立 Test Spec**：建议进入 `/test` 阶段补充端到端验收清单。

## 15. 验收标准

- 普通用户从应用中心进入业务对象后，第一主路径是“设计对象”，不是“配置模型”。
- 业务对象设计器可以完成字段新增、字段排序、字段属性配置。
- 用户可以在表单设计画布中把字段拖入分组并保存布局。
- 用户可以配置列表查询条件、表格列、工具栏按钮和行操作。
- 用户可以配置详情分组和关联列表入口。
- 用户可以用业务语言配置对象关系，并同步到底层模型关系和应用中心关系。
- 发布前检查能发现字段缺失、页面脏引用、关系目标缺失、数据表未同步等问题。
- 发布成功后自动生成或更新 `ai_crud_config` 和业务应用入口。
- 运行页不出现 `undefined`、空 label、空字段名。
- 普通用户界面不出现 `model_schema`、`page_schema`、`configKey`、DDL、表名作为主信息。
- 开发者用户仍可进入高级配置查看模型、Schema、DDL、代码生成和运行配置。
- 现有低代码模型、页面搭建、发布和动态 CRUD 能力继续可用。

## 16. 待澄清

- [x] 业务对象设计器使用新路由 `/app-center/object/:objectCode/designer`，对象详情页只保留“设计对象”主入口。
- [x] 普通用户隐藏“数据模型管理/CRUD 配置/代码生成/DDL 预览”，相关能力保留在高级/开发者入口。
- [x] 已发布字段删除默认隐藏/停用，不直接物理删除；物理删除进入高级危险操作并另走迁移审查。
- [x] 字段模板首期入库维护，使用 `ai_business_field_template` 承载通用模板，方便后续按套件扩展。
- [x] 表单设计首期复用 `CanvasFormDesigner` 和现有低代码页面搭建能力，不引入第二套主设计器。
- [x] 在线建表和字段变更默认需要高级/开发者权限，普通业务用户只看到业务化发布检查和修复入口。
- [x] 对象设计版本独立建表 `ai_business_object_design_version`，并保留 `ai_crud_config_version` 追溯字段。

## 17. 技术决策

- 业务对象设计器是普通用户主入口；低代码模型管理是高级/开发者入口。
- 不新增第二套运行时，继续复用 `AiCrudPage` 和 `DynamicCrudController`。
- 不新增第二套发布引擎，继续复用 `LowcodePublishService`。
- 不新增第二套模型事实来源，首期继续以 `ai_lowcode_model.model_schema` 和 `ai_crud_config.page_schema` 承载字段和页面配置。
- `ai_business_object` 作为业务资产聚合入口，不直接替代低代码模型表。
- 字段优先用业务语言编辑，技术字段放入高级属性。
- 关系配置以业务语言展示，以 `LowcodeRelationSchema` 和 `ai_business_object_relation` 做双向映射。
- 发布检查必须先于发布执行，字段和页面 Schema 校验失败不得发布。

## 18. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 0 | completed | `spec.md`, `tasks.md` | 固化新路由、入口隔离、字段删除、模板、画布、建表权限和版本表结论 |
| Task 1 | completed | `forge/db/migration/V1.0.40__add_business_object_designer.sql`, `AiBusinessObject.java`, `AiBusinessObjectDesignVersion.java`, `AiBusinessFieldTemplate.java` | 扩展业务对象设计字段，新增设计版本和字段模板结构与内置模板 |
| Task 2 | completed | `dto/businessapp/*`, `vo/businessapp/*`, `constant/BusinessObjectDesignStatus.java`, `constant/BusinessPublishCheckLevel.java` | 补齐设计器、字段、布局、发布和版本协议 |
| Task 3 | completed | `BusinessFieldSchemaService.java`, `BusinessFieldTemplateService.java`, `BusinessFieldTemplateMapper.java`, `BusinessFieldTemplateMapper.xml`, `LowcodeFieldSchema.java` | 支持字段默认 Schema、模板查询和字段业务扩展属性 |
| Task 4 | completed | `BusinessPublishCheckItemVO.java`, `BusinessObjectDesignVersionDTO.java`, `BusinessObjectDesignVersionService.java`, `BusinessObjectDesignVersionMapper.java`, `BusinessObjectDesignVersionMapper.xml` | 定义发布检查项和对象设计版本快照协议 |
| Task 5 | completed | `BusinessObjectDesignerController.java`, `BusinessObjectDesignerService.java` | 提供对象设计器加载和保存聚合接口，保存草稿时同步业务对象、模型 Schema 和页面 Schema |
| Task 6 | completed | `BusinessFieldDesignService.java`, `BusinessObjectDesignerController.java` | 实现字段列表、新增、修改、删除/停用、排序和页面引用检查 |
| Task 7 | completed | `BusinessLayoutDesignService.java`, `BusinessObjectDesignerController.java` | 实现表单、列表、详情布局保存和运行配置预览 |
| Task 8 | completed | `BusinessObjectRelationController.java`, `BusinessObjectDesignerService.java` | 关系保存/删除后同步 `LowcodeModelSchema.relations` |
| Task 9 | completed | `BusinessObjectActionService.java`, `BusinessObjectActionVO.java`, `BusinessObjectActionDTO.java`, `BusinessObjectDesignerController.java` | 自定义操作保存到 `designer_options`，权限摘要复用能力挂接 |
| Task 10 | completed | `BusinessObjectPublishService.java`, `BusinessObjectDesignerController.java` | 发布检查区分 PASS/WARN/BLOCK，发布门面复用低代码发布链路并回写业务对象发布状态 |
| Task 11 | completed | `BusinessObjectPublishService.java`, `BusinessObjectDesignVersionService.java`, `BusinessObjectDesignerService.java` | 发布时记录业务对象设计版本，支持按设计版本回滚草稿和低代码版本 |
| Task 17 | completed | `BusinessDetailDesigner.vue`, `object-designer.[objectCode].vue` | 接入详情字段分组、排序、隐藏/只读、详情页签和关联入口保存 |
| Task 18 | completed | `BusinessRelationDesigner.vue`, `BusinessActionDesigner.vue`, `BusinessPermissionFlowPanel.vue`, `object-designer.[objectCode].vue` | 接入关系配置、自定义操作、权限摘要和能力挂接摘要 |
| Task 19 | completed | `BusinessPublishChecklist.vue`, `object-designer.[objectCode].vue`, `BusinessObjectDesignerShell.vue` | 发布检查按通过/警告/阻断分组，支持修复跳转、发布、打开应用、版本回滚 |
| Task 20 | completed | `BusinessAdvancedConfig.vue`, `object-designer.[objectCode].vue`, `BusinessObjectDesignerShell.vue` | 高级配置按 `ai:businessObject:advanced` 权限展示，普通模式不显示表名、Schema、DDL、configKey |
| Task 21 | completed | `object.[objectCode].vue`, `ObjectCard.vue`, `ReadinessPanel.vue`, `SuiteAcceptancePanel.vue` | 应用中心对象详情改为设计对象主入口，普通用户先进入设计器，运行应用入口保留 |
| Task 22 | completed | `BusinessObjectWizardDrawer.vue`, `index.vue`, `suite.[suiteCode].vue` | 四种创建方式统一跳转业务对象设计器，不再落到低代码模型管理 |
| Task 23 | completed | `V1.0.41__add_business_object_designer_menu_permissions.sql`, `object.[objectCode].vue`, `object-designer.[objectCode].vue` | 补齐设计/字段/布局/操作/发布/高级权限与菜单隔离，普通用户隐藏技术细节 |
| Task 24 | completed | `BusinessObjectPublishService.java`, `V1.0.42__fix_crm_business_relation_field_direction.sql` | 后端编译通过；发布检查补充目标对象字段校验，CRM 样板关系字段方向修正为当前对象 `id` → 目标对象外键 |
| Task 25 | completed | `BusinessFieldPropertyPanel.vue`, `object-designer.[objectCode].vue` | 修复未选字段时属性面板空指针；对象设计器保留 19 位 `objectId` 字符串，页面验证无白屏和 `fieldName/null` 错误 |
| Task 26 | completed | `BusinessPublishChecklist.vue`, `object-designer.[objectCode].vue` | CRM 客户对象发布检查 PASS，发布成功并打开 `/ai/crud-page/crm_customer`，运行页无 `undefined` |
| Task 27 | completed | `spec.md`, `tasks.md` | 回填 Phase 5 验证结果和已修复问题，任务状态与实际完成情况一致 |

**Phase 2 验证**：2026-05-29 使用 JDK 17 执行 `mvn -pl forge-admin-server -am compile -DskipTests`，结果 `BUILD SUCCESS`。

**Phase 3 Task 17-20 验证**：2026-05-29 使用 Node v20.19.0 执行 `pnpm --dir forge-admin-ui exec eslint ...`，新增和改动前端文件检查通过；使用 `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`，结果构建成功。默认堆内存构建在 chunk 渲染阶段 OOM，增大 Node 堆后通过；构建期间存在项目既有 UnoCSS 图标缺失和 CSS `//` 注释警告。

**Phase 4 Task 21-23 验证**：2026-05-29 使用 Node v20.19.0 执行 `pnpm --dir forge-admin-ui exec eslint src/views/app-center/index.vue src/views/app-center/suite.[suiteCode].vue src/views/app-center/object.[objectCode].vue src/views/app-center/object-designer.[objectCode].vue src/views/app-center/components/ObjectCard.vue src/views/app-center/components/BusinessObjectWizardDrawer.vue src/views/app-center/components/ReadinessPanel.vue src/views/app-center/components/SuiteAcceptancePanel.vue`，检查通过。

**Phase 5 Task 24-27 验证**：2026-05-29 使用 JDK 17 执行 `mvn -pl forge-admin-server -am compile -DskipTests`，结果 `BUILD SUCCESS`；使用 Node v20.19.0 执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`，结果构建成功。构建期间仍存在项目既有 UnoCSS 图标缺失、CSS `//` 注释和大 chunk 警告；默认 Node 堆内存构建在 chunk 渲染阶段可能 OOM，增大堆内存后通过。

**问题修复记录**：2026-05-29 修复 `BusinessFieldPropertyPanel.vue` 在 `field=null` 时 `createFieldForm` 读取 `fieldName` 的空指针；修复 `object-designer.[objectCode].vue` 将 19 位 `route.query.objectId` 转为 `Number` 导致精度丢失的问题；补充 `BusinessObjectPublishService.java` 对关系目标对象字段的发布校验；新增 `V1.0.42__fix_crm_business_relation_field_direction.sql` 修正 CRM 样板关系字段方向。

**CRM 端到端验收**：2026-05-29 使用 Playwright 复用前端登录、密钥交换和加密请求链路，打开 `/app-center/object/CUSTOMER/designer?objectId=1910000000000000101&panel=fields`，字段列表 18 条，客户等级字段存在，属性面板正常渲染，无页面错误和 `fieldName/null` 控制台错误。发布检查 `overallStatus=PASS`、`blockCount=0`、`warnCount=0`；发布客户对象成功，生成设计发布版本 `2060296120277417985`；打开 `/ai/crud-page/crm_customer` 成功，搜索区和表格列无 `undefined`。

## 19. 审查结论

待 `/review lowcode-business-object-designer` 执行。

## 20. 确认记录（HARD-GATE）

- **确认时间**：2026-05-29
- **确认人**：用户指令“完成0-1阶段”
- **确认结论**：按第 16 章已勾选结论执行 Phase 0-1，后续进入 Phase 2 时不得重新把普通用户主链路导回低代码模型管理。
