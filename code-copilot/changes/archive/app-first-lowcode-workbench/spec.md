# 应用优先的低代码开发工作台
> status: apply
> created: 2026-07-13
> complexity: 🔴复杂
> change: `app-first-lowcode-workbench`

## 1. 背景与目标

Forge 当前“应用总览”以业务对象分组为主，再把访问入口折叠到对象下面。用户需要先理解“业务域、主对象、明细对象、引用对象、入口”的多层关系，才能找到自己要开发或运行的应用。这使页面虽然叫“应用总览”，实际上并未以应用作为主体。

用户同时明确提出第二个核心诉求：低代码开发者进入设计态后，应先看见数据库表、字段、数据源和同步状态；表结构、表单、列表、详情、动作、JS/CSS/服务端增强、发布和历史应围绕同一设计上下文集中管理。

本变更参考 JeeLowCode `/lowdev/tableDesign` 背后的表驱动组织方式，但不照搬其页面样式和高风险在线执行机制。最终目标是建立如下产品层级：

```text
业务域
└── 应用
    ├── 数据对象 / 数据库表
    ├── 页面与访问入口
    ├── 表单 / 列表 / 详情
    ├── 流程与自动化
    ├── 动作与增强
    ├── 权限
    └── 发布版本
```

完成后必须达到以下可验证结果：

- 应用总览的每一条主记录都是一个真实“应用”，而不是业务对象或访问入口。
- 用户从业务域树筛选后，可以直接新建、搜索、进入、启停和查看应用状态。
- 每个应用有稳定的聚合标识，能够关联多个可复用业务对象和多个访问入口。
- 对象设计工作台始终显示数据源、物理表、字段映射和数据库同步状态；新建对象时先确定数据来源和表结构。
- 表结构、表单、列表、详情、流程、动作、增强、发布和历史在同一个应用工作台内可达。
- JS、CSS、服务端扩展具备标准钩子、校验、测试、版本、审计、启停和回滚能力。
- 现有 `ai_business_app` 数据、API 和运行入口保持兼容，不做破坏性重命名。

## 2. 代码现状与研究结论

### 2.1 Forge 当前应用总览不是“应用优先”

- `forge-admin-ui/src/views/app-center/index.vue` 的主区标题为“业务对象分组”，并同时加载业务域、业务对象、访问入口和对象关系。
- `forge-admin-ui/src/views/app-center/components/BusinessObjectTable.vue` 以主对象、明细/引用对象为行，再把访问入口折叠在对象行下。
- `forge-admin-ui/src/views/app-center/index.vue` 的创建菜单是“新建业务域 / 新建业务单元 / 新建访问入口”，没有“新建应用”。
- `forge-admin-ui/src/api/business-app.js` 的 `businessApp*` 方法实际调用 `/ai/business/app` 访问入口 API。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApp.java` 同时包含 `objectCode`、`entryMode`、`entryUrl` 和 `configKey`，实体注释已经明确为“应用入口”。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessAppController.java` 的日志、权限和接口描述也统一称为“访问入口”。

结论：`ai_business_app` 是页面/渠道入口资产，不是真正的应用聚合。直接把它改名为应用会破坏已有运行、代码下载和入口打开语义。

### 2.2 Forge 已有能力应复用

- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` 与 `designer/BusinessObjectDesignerShell.vue` 已提供业务对象设计入口和设计器壳层。
- `designer/BusinessFieldManager.vue` 已维护字段资产；`designer/BusinessAdvancedConfig.vue` 已承载技术配置，但数据库表信息目前过深。
- `forge-form-designer/ForgeFieldShelf.vue`、现有 fcDesigner 适配层和已归档的 `form-first-business-object-designer` 变更已建立表单画布、字段注册表、视图投影和运行态编译链路。
- `components/lowcode-builder/page/CrudHookRulesEditor.vue` 已有部分 CRUD 钩子/规则编辑能力。
- `AiBusinessObjectDesignVersion`、`BusinessObjectDesignVersionMapper` 已提供对象设计版本基础，不能另建一套互不兼容的对象历史。
- 现有 `BusinessAppController`、`BusinessAppService` 和 `BusinessAppMapper.xml` 已稳定服务访问入口，必须保留。

### 2.3 JeeLowCode 可借鉴的设计

本地参考项目：`/Users/yaomindong/Desktop/project/github/JeeLowCode`。该仓库不包含 `/lowdev/tableDesign` 的前端源码，因此本次结论以本地后端、菜单配置和项目截图所体现的产品结构为依据，不假定其前端内部实现。

- `jeelowcode-core/.../params/DbFormAddOrUpdateParam.java` 用一个 `dbformId` 聚合基本信息、数据库字段、字典、导出、外键、页面字段、查询、索引和统计配置。
- `jeelowcode-core/.../controller/DbFormController.java` 提供独立的 `/sync-db/{dbformId}`，把“保存设计”和“同步数据库”拆成两个明确动作。
- `FormFieldEntity.java` 与 `FormFieldWebEntity.java` 分离物理字段属性和页面展示属性，避免把数据库事实与视图配置混为一体。
- `EnhanceJavaEntity.java`、`EnhanceJsEntity.java`、`EnhanceSqlEntity.java` 把增强统一挂到表单设计上下文。
- `jeelowcode-framework/.../EnhanceConstant.java` 固化新增、修改、删除、导入、导出、列表、详情、统计、分页及开始/结束等标准钩子。
- `HistoryController.java` 为表单设计和 JS/SQL/Java 增强提供历史查询。

可借鉴：稳定表/对象锚点、集中配置、保存与同步分离、标准钩子、编辑锁、版本和历史。

不照搬：任意 Java 在线编译、任意 SQL 执行、缺少隔离的脚本运行、密集配置堆叠、把物理表当成唯一业务事实、复制多套不能互相编译的配置模型。

### 2.4 与“表单优先”既有决策的关系

本变更不否定已完成的表单优先设计层，而是补齐开发者视角：

- 应用导航层坚持“应用优先”。
- 对象创建链路先确定数据来源、物理表和字段结构。
- 对象设计页始终显示表映射摘要，并为开发者默认提供“数据结构”入口。
- 业务人员可从“设计表单”深链直接进入画布，但仍可看到紧凑的表映射信息。
- FormDesignerSchema、FieldRegistry、ViewSchema、LinkageSchema 和运行态编译链路继续作为 Forge 事实来源，不退化为页面 JSON 或数据库 DDL 的附属品。

## 3. 术语冻结

| 中文术语 | 代码语义 | 说明 |
|----------|----------|------|
| 业务域 | `BusinessSuite` / `ai_business_suite` | 应用的目录和业务边界，可形成树 |
| 应用 | 新增 `BusinessApplication` / `ai_business_application` | 用户认知中的可交付业务应用聚合 |
| 业务对象 | `BusinessObject` / `ai_business_object` | 可复用的数据模型和设计资产，通常映射数据库表 |
| 页面入口 / 访问入口 | 现有 `BusinessApp` / `ai_business_app` | RUNTIME、ROUTE、IFRAME、H5、API 等打开方式 |
| 应用对象关系 | 新增 `ai_business_application_object` | 应用与可复用业务对象的多对多关系 |
| 扩展 | 新增 `BusinessExtension` | JS、作用域 CSS、服务端能力绑定或可视化规则 |
| 发布版本 | 新增应用版本快照 | 应用级协调发布的不可变快照 |

新代码、页面和文档不得再把 `ai_business_app` 称为“应用”。旧 API 名和旧数据库表名仅作为兼容标识保留。

## 4. 用户角色与主流程

### 4.1 业务用户

- 从业务域找到应用并进入运行页或业务表单。
- 通过“设计表单”入口直接进入画布，不需要理解 DDL。
- 看到业务名称、发布状态、可用入口和流程状态，不直接面对脚本和技术参数。

### 4.2 实施人员 / 低代码开发者

- 新建应用后，优先绑定数据库表、已有对象、模板或 AI 草稿。
- 查看业务字段、字段编码、数据库列、类型、索引和同步差异。
- 在同一工作台配置表单、列表、详情、关系、流程、动作和发布。
- 预览数据库变更后显式确认同步，不允许保存设计时静默改表。

### 4.3 专业开发人员

- 配置受治理的 JS、CSS 和服务端能力绑定。
- 查看版本差异、验证结果、审计、失败策略和回滚记录。
- 不通过平台在线编译任意 Java，不直接执行任意写 SQL。

### 4.4 平台管理员

- 管理应用、扩展、发布和回滚权限。
- 管理服务端扩展白名单、HTTP 适配器、脚本能力开关和安全策略。
- 审计谁在何时修改、测试、启用、发布或回滚了什么内容。

## 5. 目标信息架构与页面行为

### 5.1 应用总览

应用总览保留左侧业务域树，右侧只展示应用列表，不再展示业务对象分组。

```text
┌────────业务域树────────┬────────────────应用列表────────────────┐
│ + 新建业务域           │ 应用总览             [搜索] [新建应用] │
│ 全部应用               │ 状态 / 类型筛选                         │
│ ├─ 客户管理            │ 应用名称 | 状态 | 对象 | 页面 | 流程 | 更新 │
│ ├─ 供应链              │ 客户经营 | 已发布 | 6   | 4    | 2   | ... │
│ └─ 人力资源            │ 采购管理 | 有变更 | 8   | 5    | 1   | ... │
└────────────────────────┴──────────────────────────────────────────┘
```

交互约束：

- 左侧业务域树保持独立滚动，宽度建议 260～300px，顶部保留“新建业务域”。
- 选择父业务域时查询当前域及全部子域的应用。
- 右侧默认使用克制、紧凑的企业控制台表格；可保留卡片视图切换，但不使用统计卡、渐变背景和装饰性大标题堆叠。
- 每行只展示应用名称、业务域、状态、对象/页面/流程数量、最近更新、负责人和操作。
- 第一主操作是“进入应用”；设计工作台默认新开浏览器页签。
- 第一主按钮是“新建应用”。“新建对象”和“新建入口”移动到应用工作台内部。
- 总览只调用一次应用聚合分页接口，不按对象逐个查询关系和入口。

### 5.2 新建应用

新建流程控制在两步：

1. 基本信息：应用名称、应用编码、所属业务域、图标、描述。
2. 初始化方式：空白应用、绑定已有对象、从数据库表开始、从模板开始、从 AI 草稿开始。

保存后立即创建应用草稿并进入工作台；后续对象或表导入失败不得回滚已经创建的应用，而应保留明确的待配置状态和重试入口。

选择“从数据库表开始”时，运行数据源和数据表必须在本向导内直接选择；创建草稿后自动导入业务对象并建立 `PRIMARY` 关联，不得再跳转到数据对象分区要求用户重复打开导入向导。数据对象分区仍保留数据库表导入，供已有应用追加对象使用。

### 5.3 应用工作台

新增路由建议：`/app-center/application/:applicationCode`。

工作台一级分区：

1. 概览：应用状态、就绪度、最近变更、未完成事项。
2. 数据对象：数据库表、字段映射、对象关系和同步状态。
3. 页面入口：表单、列表、详情和访问入口。
4. 流程自动化：Flowable 绑定、触发器、消息和任务。
5. 动作与增强：可视化规则、JS、CSS、服务端扩展绑定。
6. 权限：资源权限、字段/数据权限摘要和入口可见范围。
7. 发布历史：发布检查、版本、差异、回滚和审计。

工作台使用左侧或顶部紧凑分区导航，不把所有配置一次性纵向铺开。每个分区显示完成状态和问题数量。

新建访问入口采用“选择场景 → 配置入口”两步：应用、业务域和应用内主对象由工作台上下文自动带入；入口名称、编码、页面配置和单一默认表单根据场景与对象自动生成。只有存在多个对象、页面或表单时才要求用户选择。普通菜单配置只显示“添加到管理端菜单”开关，父菜单、排序及技术参数放入高级编辑。

### 5.4 数据库表优先的对象设计

新建对象流程：

1. 选择数据源。
2. 选择“已有表 / 新建表 / 暂不建表”。
3. 显示表名、表注释和字段网格；已有表自动读取列，新表输入业务字段并生成列建议。
4. 明确展示“业务字段 ↔ 字段编码 ↔ 数据库列 ↔ 数据类型 ↔ 页面控件”。
5. 保存设计草稿。
6. 通过“预览并同步数据库”查看 DDL 差异，二次确认后同步。
7. 进入表单、列表和详情设计。

对象设计页必须持续显示：数据源、物理表、同步状态、最近同步时间和未同步变更数量。数据库表信息不得只放在“高级配置”。

数据库表导入必须以物理列为基线初始化字段的数据类型、长度、decimal 精度、是否必填和页面控件；后续表单控件归一化不得反向覆盖已经存在的物理字段类型。已有 `NOT NULL` 列导入后继续保持必填，不得因为没有默认值而生成改为 `NULL` 的 DDL。

二级索引采用完全显式配置：系统不得再根据“作为查询条件”、对象关系、租户字段或审计字段自动推导索引。用户在数据结构页明确填写索引名称、类型、字段和用途并保存设计后，索引才进入数据库差异预览；旧版 `auto=true` 索引只保留兼容读取，不参与 DDL 生成。主键约束不受此规则影响。

兼容表单优先的规则：

- 新建对象首次进入默认显示“数据结构”。
- 从数据库表导入的对象默认停留在字段映射网格。
- 从“设计表单”入口进入时允许直接打开表单画布。
- 返回对象设计页时可以恢复用户上次访问分区，但表映射摘要始终可见。
- 普通业务人员不直接编辑 DDL；开发者权限才可执行数据库同步。

## 6. 范围与非目标

### 6.1 本变更范围

- 新增真实应用聚合和应用-对象关联。
- 兼容迁移现有业务对象和访问入口。
- 重构应用总览为应用优先。
- 新增应用工作台并整合现有对象、入口、流程、权限、发布能力。
- 把数据库表和字段映射提升为对象设计的可见锚点。
- 建立受治理的 JS、CSS、服务端扩展中心。
- 建立应用级就绪度、发布快照和协调回滚。

### 6.2 非目标

- 不重写现有动态 CRUD 运行时、AiCrudPage、AiForm 或 Flowable 引擎。
- 不删除或重命名 `ai_business_app`、`/ai/business/app` 及其现有权限。
- 不把业务对象改成应用私有资产；对象仍可被多个应用复用。
- 不让保存表单或字段草稿时自动执行 DDL。
- 不提供任意 Java 源码在线编译或任意 Class 反射执行。
- 不提供任意写 SQL、DDL 脚本或无参数约束 SQL 在线执行。
- 不在本变更中建设完整 IDE、Git 托管、第三方插件市场或多人实时协作编辑。
- 不照搬 JeeLowCode 的视觉样式或高密度配置页。

## 7. 分阶段需求

### Phase 0：语义冻结与兼容边界

#### REQ-P0-01 术语统一

- 新增页面和代码使用“应用、业务对象、访问入口”三个独立术语。
- 现有 `BusinessApp*` 类保持兼容，但新 UI 文案只能称“访问入口”。
- 新应用聚合使用 `BusinessApplication*` 命名，禁止再使用含糊的第二套 `BusinessApp*`。

#### REQ-P0-02 兼容清单

- 固化现有 `/ai/business/app/**` API、权限、运行和代码下载行为。
- 固化 `ai_business_object` 可复用语义。
- 输出存量数据回填规则和失败兜底规则。

阶段门：术语、表结构、回填策略、安全边界经用户确认后，才可进入 Phase 1 编码。

### Phase 1：应用聚合基础

#### REQ-P1-01 应用 CRUD

- 支持应用分页、列表、详情、新增、修改、启停和逻辑删除。
- 应用编码在同一租户下仅对未删除记录唯一。
- 应用必须属于一个有效业务域；业务域停用时应用不能发布，但已发布运行入口按现有策略继续可用，除非管理员显式停用。

#### REQ-P1-02 应用-对象关联

- 一个应用可关联多个对象，一个对象可被多个应用复用。
- 关联角色固定为 `PRIMARY`、`DETAIL`、`REFERENCE`、`SHARED`。
- 一个应用必须且只能有一个 `PRIMARY` 对象才能达到 READY，但草稿应用允许暂时没有主对象。
- 删除关联只删除应用编排关系，不删除业务对象和物理表。

#### REQ-P1-03 访问入口归属

- `ai_business_app` 新增可空 `application_id`。
- 新建访问入口时必须指定应用；兼容同步程序和存量 API 可以暂时为空。
- 删除应用前若存在启用入口，必须阻止删除并提示先迁移或停用入口。

#### REQ-P1-04 能力挂接目标

- `ai_business_binding.target_type` 新增 `APPLICATION`。
- 旧值 `APP` 继续表示访问入口，不改变语义。

#### REQ-P1-05 存量回填

- 按租户和业务域，为主对象组创建默认应用并绑定对象、明细对象和可识别入口。
- 无法唯一识别的入口归入业务域下的“历史入口应用”，不丢弃数据。
- 回填可重复执行，不产生重复应用或重复关联。

阶段门：迁移脚本静态检查、后端聚合 CRUD 测试和存量兼容测试通过后，才能进入 Phase 2。

### Phase 2：应用优先总览

#### REQ-P2-01 聚合分页

- 应用分页结果一次返回应用基本信息、业务域信息、对象数量、入口数量、流程数量、扩展数量、发布状态和最近更新时间。
- 业务域筛选包含子域；关键词匹配应用名称、编码和描述。
- 支持状态筛选、排序和 `pageNum/pageSize` 分页。
- 禁止前端为每条应用逐个请求对象关系或入口数量。

#### REQ-P2-02 总览主区

- 移除 `BusinessObjectTable` 作为应用总览主内容。
- 右侧主记录只有应用；对象和入口仅作为数量和摘要展示。
- 支持新建、编辑、启停、删除、进入应用工作台。
- 空状态提供“新建应用”和“从现有对象整理应用”两个明确动作。

#### REQ-P2-03 兼容入口

- 原有对象设计和访问入口路由继续可访问。
- 对象、入口的维护动作移动到应用工作台后，总览不再重复提供复杂对象分组操作。

阶段门：应用总览不再依赖对象分组、聚合 API 无 N+1、关键筛选和新建流程浏览器验收通过。

### Phase 3：应用工作台与表优先对象设计

#### REQ-P3-01 工作台聚合

- 工作台顶部显示应用名称、业务域、设计/发布状态、就绪度和主操作。
- 分区显示对象、入口、流程、增强、权限和发布状态。
- 聚合接口只返回摘要和首屏必要数据；各分区详情按需加载。

#### REQ-P3-02 数据对象分区

- 显示对象角色、业务名称、对象编码、数据源、物理表、同步状态、设计状态和最近更新时间。
- 支持关联已有对象、从数据库表导入、新建对象、调整角色和移除关联。
- 对象被多个应用使用时，修改结构前必须提示影响应用数量。

#### REQ-P3-03 数据结构首屏

- 新建对象首先进入数据来源和表结构步骤。
- 字段网格展示业务字段名、字段编码、数据库列、数据类型、长度/精度、可空、默认值、索引、页面控件和同步状态。
- 数据库保留字段和审计字段不可被普通编辑删除。
- 导入已有表时不得自动修改数据库；先生成对象草稿和差异。

#### REQ-P3-04 保存与同步分离

- “保存草稿”只保存设计态元数据。
- “预览数据库变更”返回结构化差异和 DDL 预览，不执行数据库变更。
- “确认同步数据库”需要独立权限、二次确认和操作审计。
- 同步失败不得丢失草稿；必须记录失败原因和可重试状态。
- 生产环境可通过配置完全禁用在线同步，仅允许导出迁移脚本。

#### REQ-P3-05 集中设计入口

- 同一对象上下文可切换数据结构、表单、列表、详情、关系、动作、流程、增强、发布和历史。
- 复用现有 FormDesignerSchema、FieldRegistry、ViewSchema、LinkageSchema 和对象版本能力。
- 不复制第二套表单、列表、详情 Schema。

阶段门：数据库映射可见、保存/同步边界验证、现有表单优先对象兼容和至少一个存量对象浏览器回归通过。

### Phase 4：受治理的扩展中心

> implementation-status: `completed-static`（代码与安全自动化用例已实现；真实 Flyway/API/浏览器验证待用户执行）

#### REQ-P4-01 扩展类型

首期支持：

- `VISUAL_RULE`：优先面向业务人员的条件和动作配置。
- `CLIENT_JS`：运行在受限沙箱、只获得白名单上下文 API 的客户端脚本。
- `SCOPED_CSS`：只作用于指定应用/页面/组件根节点的样式。
- `SERVER_BINDING`：绑定已注册的 Spring Bean、Capability 或受控 HTTP 适配器。

`READONLY_SQL` 仅保留模型枚举位置，不在本变更启用；未来启用必须单独立项并通过参数化、只读数据源、SQL AST 校验、行数/耗时限制和审计硬门。

#### REQ-P4-02 标准钩子

对象级标准钩子：

- `BEFORE_CREATE` / `AFTER_CREATE`
- `BEFORE_UPDATE` / `AFTER_UPDATE`
- `BEFORE_DELETE` / `AFTER_DELETE`
- `BEFORE_IMPORT` / `AFTER_IMPORT`
- `BEFORE_EXPORT` / `AFTER_EXPORT`
- `BEFORE_LIST` / `AFTER_LIST`
- `BEFORE_DETAIL` / `AFTER_DETAIL`
- `BEFORE_SUMMARY` / `AFTER_SUMMARY`

页面级钩子：`PAGE_INIT`、`FORM_CHANGE`、`BEFORE_SUBMIT`、`AFTER_SUBMIT`、`ROW_ACTION`。

钩子编码由平台字典维护；执行顺序由 `sort_order` 决定，同顺序按扩展编码稳定排序。

#### REQ-P4-03 生命周期

- 扩展状态为 `DRAFT`、`TESTED`、`ENABLED`、`DISABLED`。
- 内容变更后自动从 `TESTED/ENABLED` 回到 `DRAFT`，已发布运行态继续使用上一已发布版本。
- 只有校验和测试通过的版本才能启用或进入应用发布。
- 每次保存生成版本，支持差异查看和回滚草稿。
- 编辑锁必须有持有人、获得时间和超时释放机制。

#### REQ-P4-04 JS 安全

- 不允许直接访问 `window`、`document`、Cookie、LocalStorage、访问令牌或任意网络请求。
- 通过受限沙箱和结构化上下文执行，只暴露读取字段、设置字段、显示消息、触发白名单动作等 API。
- 配置执行超时、输出大小和失败策略；超限立即终止。
- 禁止通过 `eval` 或 `new Function` 在主页面上下文执行存储脚本。

#### REQ-P4-05 CSS 安全

- 保存时解析 CSS 并自动加应用/页面作用域前缀。
- 拒绝 `@import`、全局 `html/body/:root` 覆盖、外部 URL、表达式和越界选择器。
- 提供预览、影响范围和回滚；扩展 CSS 不得修改 Forge 全局布局。

#### REQ-P4-06 服务端扩展安全

- 只允许绑定管理员注册且标记为低代码可用的处理器。
- 处理器声明输入/输出 Schema、允许钩子、超时、风险级别和所需权限。
- HTTP 适配器只引用安全配置 ID，不在扩展内容中保存 URL 密钥、Token 或 Secret。
- 不允许在线 Java 源码、任意类路径或用户输入 Bean 名直接执行。

#### REQ-P4-07 失败策略和审计

- 失败策略固定为 `BLOCK`、`WARN`、`IGNORE`，高风险前置钩子只能使用 `BLOCK`。
- 记录扩展编码、版本、钩子、耗时、结果、错误摘要、应用、对象、租户和操作者。
- 日志不得记录敏感字段原值、脚本密钥或完整请求体。

阶段门：沙箱逃逸负例、CSS 越界负例、未注册服务绑定负例、版本回滚和审计测试全部通过。

### Phase 5：应用级协调发布

> implementation-status: `completed-static`（发布、恢复、回滚和前端工作台已实现；本轮按用户要求不执行验证）

#### REQ-P5-01 应用就绪度

- 聚合对象设计状态、数据库同步状态、入口状态、流程绑定、扩展测试状态、权限和发布差异。
- 问题按阻断、警告、提示分级，并给出可跳转的修复位置。
- 未通过阻断检查的应用不能发布。

#### REQ-P5-02 不可变版本快照

- 每次发布创建不可变应用版本，记录应用、对象版本、入口配置、流程绑定、扩展版本、权限摘要和校验结果。
- 发布版本号在同一应用内单调递增。
- 快照生成后不允许更新，只能创建新版本。

#### REQ-P5-03 协调发布

- 用户可发布全部变更或选择变更项，但依赖项必须自动补齐。
- 默认发布选择只纳入已测试、已启用或已停用扩展；未测试草稿继续保留并作为提醒跳过，不得因为实验性草稿阻断其它应用资产发布。若未来由用户显式选择未测试扩展，则预检查必须阻断。
- 发布按预检查、快照、对象发布、入口切换、扩展启用、应用状态提交执行。
- 任一步失败必须显示已完成、失败和未执行项，不能只返回笼统失败。
- 不承诺跨数据库和外部系统的全局事务；通过幂等步骤和补偿/重试保证可恢复。

#### REQ-P5-04 回滚

- 回滚目标必须是历史已发布快照。
- 回滚默认只恢复设计/运行配置，不自动回滚破坏性数据库 DDL 或业务数据。
- 如果历史版本依赖当前不存在的字段，必须阻止回滚并给出兼容性说明。

#### REQ-P5-05 旧入口收敛

- 只有在新应用总览采用率、未归属入口数量和关键流程稳定后，才可提出删除旧 UI 的独立变更。
- 本阶段仍不删除旧表和旧 API。

#### REQ-P5-06 发布历史与发布性能

- 进入发布历史只加载版本摘要和运行单摘要，不自动执行完整发布检查，也不读取列表未展示的 `snapshot_json`、`selection_json` 等大型字段。
- 发布检查必须显式触发；直接发布由发布接口执行一次最终权威检查，并在同一请求内复用已解析的应用、对象、入口、扩展、绑定和权限上下文。
- 对象权限摘要、对象最新发布版本和扩展发布版本必须批量查询，禁止按对象或扩展产生 N+1。
- 首次发布可以复用创建运行单前的检查结果；部分失败恢复和回滚兼容性检查必须重新读取当前状态，不能复用过期结论。
- 前端为发布检查和发布执行配置独立超时保护，但延长超时不能替代后端查询收敛。

阶段门：发布失败可恢复、快照不可变、版本回滚边界和兼容入口回归通过后才可标记整体完成。

## 8. 业务规则

### 8.1 应用状态

应用同时维护启停状态和设计状态：

- `status`：`1` 启用、`0` 停用。
- `design_status`：`DRAFT`、`READY`、`PUBLISHED`、`CHANGED`。

状态规则：

- 新建应用为 `DRAFT`。
- 满足一个主对象、至少一个可用入口且无阻断问题后为 `READY`。
- 发布成功为 `PUBLISHED`。
- 已发布应用的任一受管资产变更后为 `CHANGED`，运行态继续使用最近发布版本。
- 停用不覆盖设计状态；恢复启用后仍保持原设计状态。

### 8.2 对象复用

- 对象是业务域资产，不是应用私有子表。
- 修改共享对象必须显示受影响应用；复用影响只作为发布提醒，不要求当前应用同步完成其他应用的发布检查，也不得阻断当前应用发布。
- 应用删除不级联删除对象、物理表、入口历史或版本快照。

### 8.3 数据库同步

- 所有同步必须基于设计版本和当前数据库结构计算差异。
- 添加字段、扩大长度可作为低风险变更；删除字段、缩短长度、改类型、改主键或唯一索引为高风险变更。
- 高风险 DDL 默认不在线执行，只允许导出迁移脚本并人工审核。
- Forge 标准审计字段、租户字段和逻辑删除字段必须符合 `AGENTS.md`。

### 8.4 字典与枚举

- 应用状态、对象角色、扩展类型、扩展状态、钩子和失败策略均写入 `sys_dict_type/sys_dict_data`。
- 前端统一使用 `useDict`、`DictSelect`、`DictTag`，禁止硬编码展示映射。

## 9. 数据模型

### 9.1 `ai_business_application`

| 字段 | 类型建议 | 规则 |
|------|----------|------|
| `id` | bigint | 雪花 ID |
| `tenant_id` | bigint | 必填，默认租户内置数据为 1 |
| `application_code` | varchar(128) | 租户内未删除唯一，创建后不可修改 |
| `application_name` | varchar(128) | 必填 |
| `suite_code` | varchar(128) | 必填，逻辑关联业务域 |
| `icon` | varchar(255) | 可空 |
| `description` | varchar(500) | 可空 |
| `status` | tinyint | 1 启用、0 停用 |
| `design_status` | varchar(32) | DRAFT/READY/PUBLISHED/CHANGED |
| `last_publish_version` | int | 可空 |
| `last_publish_time` | datetime | 可空 |
| `options` | json | 非核心扩展配置，不保存密钥 |
| `del_flag` | char(1) | 0 正常、1 删除，实体显式 `@TableLogic` |
| 审计字段 | 标准字段 | `create_by/create_time/create_dept/update_by/update_time` |

唯一键使用生成列 `logic_delete_active`，仅约束未删除记录：

`UNIQUE (tenant_id, application_code, logic_delete_active)`。

### 9.2 `ai_business_application_object`

| 字段 | 类型建议 | 规则 |
|------|----------|------|
| `id` | bigint | 雪花 ID |
| `tenant_id` | bigint | 必填 |
| `application_id` | bigint | 逻辑关联应用 |
| `object_id` | bigint | 逻辑关联业务对象 |
| `object_role` | varchar(32) | PRIMARY/DETAIL/REFERENCE/SHARED |
| `sort_order` | int | 默认 0 |
| `options` | json | 应用内对象展示配置 |
| `del_flag` | char(1) | 显式逻辑删除 |
| 审计字段 | 标准字段 | 完整审计字段 |

唯一键：`UNIQUE (tenant_id, application_id, object_id, logic_delete_active)`；并通过服务层保证一个应用最多一个未删除 `PRIMARY`。

### 9.3 `ai_business_app` 兼容扩展

新增可空 `application_id bigint` 和索引 `(tenant_id, application_id, status, sort_order)`。实体、DTO、VO、Query 和 Mapper XML 同步补充字段，所有自定义 XML 查询继续显式过滤 `del_flag='0'`。

### 9.4 Phase 4 扩展表

`ai_business_extension` 保存扩展身份、所属应用/对象/入口、类型、钩子、作用域、排序、失败策略、状态、当前草稿版本、当前发布版本、编辑锁和逻辑删除字段。

`ai_business_extension_version` 保存不可覆盖的版本内容、内容摘要、校验结果、测试结果、创建人和审计字段。敏感引用只保存安全配置 ID。

两个表都必须有 `tenant_id`、完整审计字段、`del_flag`、显式 `@TableLogic` 和未删除唯一键语义。

### 9.5 Phase 5 应用版本表

`ai_business_application_version` 至少包含：`application_id`、`version_no`、`snapshot_json`、`snapshot_hash`、`publish_status`、`publish_summary`、`published_by`、`published_time`、标准审计字段和 `del_flag`。

版本记录创建后禁止 update；修复只能生成新版本。唯一键为应用内未删除 `version_no`。

## 10. 数据迁移与回填

### 10.1 Flyway 版本

当前正式迁移目录最新为 `V1.0.26__add_capability_high_risk_approval.sql`。Phase 1 实施时先重新检查版本；若仍可用，建议：

- `V1.0.27__add_business_application_aggregate.sql`
- 后续扩展表和版本表使用紧随其后的单调递增版本。

已经执行过的脚本不得修改；脚本必须通过 `information_schema` 和 `NOT EXISTS` 做防重复保护。

### 10.2 主对象识别

每个租户、业务域内按以下顺序识别默认应用主对象：

1. `object_type` 为 `MASTER` 或 `TRANSACTION`。
2. 未作为其他对象直接 `DETAIL/CHILD` 目标的对象。
3. 仍无法判断时按对象 ID 稳定排序，各自创建独立默认应用，不做猜测性合并。

### 10.3 关联回填

- 主对象关联为 `PRIMARY`。
- 直接明细/子对象关联为 `DETAIL`。
- 引用对象可关联为 `REFERENCE`，允许复用到多个应用。
- 现有入口按 `(tenant_id, suite_code, object_code)` 绑定到对应应用。
- 无对象或多义入口绑定到每个业务域唯一的“历史入口应用”，编码使用确定性规则，保证重复执行结果一致。

### 10.4 回填保护

- 不删除、不改名、不停用任何存量对象和入口。
- 回填前后记录应用、关联、已归属入口、未归属入口数量。
- 回填脚本失败时事务回滚；上线可通过关闭新 UI 回退到旧入口。
- `application_id` 保持可空，旧版本后端仍可忽略新关系。

## 11. 接口设计

### 11.1 Phase 1/2 应用 API

| 方法 | 路径 | 权限建议 | 说明 |
|------|------|----------|------|
| GET | `/ai/business/application/page` | `ai:businessApplication:list` | 聚合分页，参数 `pageNum/pageSize` |
| GET | `/ai/business/application/list` | `ai:businessApplication:list` | 轻量选择列表 |
| GET | `/ai/business/application/{id}` | `ai:businessApplication:list` | 详情 |
| GET | `/ai/business/application/by-code/{applicationCode}` | `ai:businessApplication:list` | 工作台按编码解析 |
| POST | `/ai/business/application` | `ai:businessApplication:add` | 新建 |
| PUT | `/ai/business/application` | `ai:businessApplication:edit` | 修改 |
| PUT | `/ai/business/application/{id}/status` | `ai:businessApplication:status` | 启停 |
| DELETE | `/ai/business/application/{id}` | `ai:businessApplication:delete` | 逻辑删除 |
| GET | `/ai/business/application/{id}/objects` | `ai:businessApplication:list` | 对象关联列表 |
| PUT | `/ai/business/application/{id}/objects` | `ai:businessApplication:edit` | 批量保存关联 |

所有接口使用 `@ApiEncrypt/@ApiDecrypt` 与 `RespInfo`，分页命名严格使用 `pageNum/pageSize`。

### 11.2 Phase 3 工作台 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/business/application/{id}/workspace` | 应用摘要、计数、问题数 |
| GET | `/ai/business/application/{id}/readiness` | 发布就绪度 |
| GET | `/ai/business/object/{id}/table-mapping` | 数据源、物理表、字段映射和同步状态 |
| POST | `/ai/business/object/{id}/database-diff` | 计算结构化差异和 DDL 预览，不执行 |
| POST | `/ai/business/object/{id}/database-sync` | 显式确认同步，独立高权限 |

数据库同步接口必须带设计版本或 ETag；版本不一致返回冲突，不允许覆盖他人新设计。

### 11.3 Phase 4 扩展 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/business/extension/page` | 按应用、对象、类型、钩子和状态分页 |
| POST/PUT/DELETE | `/ai/business/extension` | 扩展草稿 CRUD 和逻辑删除 |
| POST | `/ai/business/extension/{id}/validate` | 静态校验 |
| POST | `/ai/business/extension/{id}/test` | 受限测试，不影响正式运行 |
| PUT | `/ai/business/extension/{id}/status` | 启停，校验状态机 |
| GET | `/ai/business/extension/{id}/versions` | 版本列表和差异 |
| POST | `/ai/business/extension/{id}/rollback/{version}` | 回滚为新草稿版本 |
| POST/DELETE | `/ai/business/extension/{id}/lock` | 获取/释放编辑锁 |

### 11.4 Phase 5 发布 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/business/application/{id}/publish/check` | 发布预检查 |
| POST | `/ai/business/application/{id}/publish` | 协调发布，带幂等键 |
| GET | `/ai/business/application/{id}/versions` | 应用版本列表 |
| GET | `/ai/business/application/{id}/versions/{version}` | 快照和差异摘要 |
| POST | `/ai/business/application/{id}/rollback/{version}` | 兼容检查后回滚 |

## 12. 代码影响范围

### 12.1 后端

建议新增：

- `domain/entity/AiBusinessApplication.java`
- `domain/entity/AiBusinessApplicationObject.java`
- `dto/businessapp/BusinessApplicationDTO.java`
- `dto/businessapp/BusinessApplicationQueryDTO.java`
- `vo/businessapp/BusinessApplicationVO.java`
- `vo/businessapp/BusinessApplicationWorkspaceVO.java`
- `mapper/BusinessApplicationMapper.java` 与 XML
- `mapper/BusinessApplicationObjectMapper.java` 与 XML
- `service/businessapp/BusinessApplicationService.java`
- `service/businessapp/BusinessApplicationWorkspaceService.java`
- `controller/BusinessApplicationController.java`

建议修改：

- `AiBusinessApp.java` 及对应 DTO/VO/Query、Service、Mapper XML，补 `applicationId`。
- `BusinessBinding*` 目标类型校验，新增 `APPLICATION` 且保留 `APP`。
- `BusinessSuite*` 或工作台聚合服务，复用业务域子树解析。

所有复杂查询写在 Mapper XML，显式过滤逻辑删除；Service 不用 `LambdaQueryWrapper` 拼查询 SQL。

### 12.2 前端

建议新增：

- `forge-admin-ui/src/views/app-center/application.[applicationCode].vue`
- `forge-admin-ui/src/views/app-center/components/ApplicationTable.vue`
- `forge-admin-ui/src/views/app-center/components/ApplicationEditorDrawer.vue`
- `forge-admin-ui/src/views/app-center/application-workspace/*`
- `forge-admin-ui/src/api/business-application.js`

建议修改：

- `forge-admin-ui/src/views/app-center/index.vue`：总览只加载应用聚合分页。
- `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue`：筛选语义改为应用。
- `forge-admin-ui/src/router/index.js`：新增应用工作台路由。
- `object-designer.[objectCode].vue` 与设计器壳层：提升表映射摘要和数据结构入口。
- `business-app.js`：保留为访问入口 API，不继续承载新应用聚合 API。

`BusinessObjectTable.vue` 不立即删除，可供旧页面或应用内对象编排临时复用；总览不再引用后再评估清理。

## 13. 权限、安全与审计

- 应用管理、对象关联、数据库同步、扩展编辑、扩展测试、扩展启停、发布和回滚使用独立权限。
- 数据库同步和服务端扩展启用属于高风险管理操作，必须记录操作日志。
- 所有新表和查询受租户隔离；请求体中的 `tenantId` 不得覆盖当前登录租户。
- 脚本、CSS、扩展测试输入和错误日志都要做长度限制与敏感字段脱敏。
- 扩展配置禁止保存数据库密码、API Key、Secret、Token；只能引用安全配置 ID。
- 客户端脚本不得获得鉴权令牌和任意网络能力。
- 应用发布和回滚必须带幂等键，避免重复请求生成多个版本或重复切换。
- 删除用户可见应用、扩展和版本元数据使用逻辑删除；实体显式 `@TableLogic`。
- 物理表 DDL 同步必须拒绝跨租户数据源和未授权数据源。

## 14. 兼容、灰度与回滚

### 14.1 兼容原则

- 先加表和可空字段，再加后端，再加新 UI。
- 旧后端可忽略新表和 `application_id`；新后端可处理未归属旧入口。
- 旧总览可通过路由或配置开关临时保留，直到新总览验证稳定。
- 访问入口运行、预览、代码下载和同步 API 不能因新增应用聚合改变行为。

### 14.2 灰度指标

- 应用聚合分页错误率和耗时。
- 未归属访问入口数量。
- 回填后应用/对象/入口计数一致性。
- 新总览进入应用成功率。
- 应用发布成功、失败和恢复次数。
- 扩展校验失败、超时和运行错误数量。

### 14.3 回退

- Phase 1/2 可关闭新应用总览并恢复旧路由，不删除新数据。
- `application_id` 可空，旧入口继续按原逻辑运行。
- 扩展中心默认功能开关关闭，只有通过安全测试后启用。
- 应用发布失败恢复到最近成功快照；数据库 DDL 不纳入自动回滚。

## 15. 测试策略

- 独立 Test Spec：是，见 `test-spec.md`。
- 文档阶段只执行 `git diff --check`、链接和状态一致性检查，不启动服务、不修改数据库。
- Phase 1 重点覆盖回填幂等、逻辑删除唯一键、一个主对象、入口兼容和租户隔离。
- Phase 2 重点覆盖聚合分页、业务域子树、无 N+1 和总览交互。
- Phase 3 重点覆盖保存/同步分离、DDL 高风险拦截、版本冲突和旧对象兼容。
- Phase 4 重点覆盖脚本沙箱、CSS 作用域、服务白名单、超时、版本和审计负例。
- Phase 5 重点覆盖发布幂等、部分失败恢复、快照不可变和回滚兼容性。
- 前端构建统一使用 Node `v20.19.0`；后端使用 JDK 17。
- 真实 Flyway、服务启动和数据库联调由用户执行或另行明确授权；没有真实证据时不得宣称 E2E 通过。

## 16. 验收标准

### 16.1 产品验收

- [ ] 总览主区只展示应用，不再以对象分组作为主体。
- [ ] 用户能从业务域树筛选、新建并进入应用。
- [ ] 应用内能集中找到对象、页面入口、流程、增强、权限和发布。
- [ ] 新建对象先看数据来源、表名和字段网格。
- [ ] 对象设计页始终可见表映射和同步状态。
- [ ] 保存草稿不会修改数据库；同步数据库必须显式确认。
- [ ] JS/CSS/服务端增强有标准钩子、测试、版本、启停、审计和回滚。
- [ ] 页面保持克制的企业控制台风格，无无业务价值的视觉堆叠。

### 16.2 技术验收

- [ ] `ai_business_application` 和关联表具备完整租户、审计、逻辑删除和未删除唯一键语义。
- [ ] `ai_business_app.application_id` 可空且所有旧 API 回归通过。
- [ ] 存量回填可重复执行，未识别入口有确定性兜底应用。
- [ ] 聚合分页无逐条关系查询，Mapper XML 显式过滤 `del_flag`。
- [ ] 一个应用最多一个主对象，删除应用不删除共享对象。
- [ ] DDL 预览和执行分离，高风险 DDL 默认阻止在线执行。
- [ ] 客户端脚本不在主页面使用 `eval/new Function` 执行。
- [ ] CSS 不能越过应用/页面作用域。
- [ ] 服务端扩展只执行注册白名单能力。
- [ ] 发布快照不可修改，发布和回滚具备幂等与审计。

### 16.3 性能验收

- 应用分页查询不随每页应用数量产生线性额外 SQL。
- 默认 20 条应用的聚合分页目标响应时间在本地开发环境不高于 800ms；实际基线需在 Phase 2 执行日志记录。
- 工作台首屏只加载摘要，非活动分区按需加载。
- 打开发布历史固定为版本摘要和运行单摘要两类请求，不因应用对象、入口或扩展数量增加额外查询。
- 单次发布请求不重复执行完整预检查；权限、最新对象版本和扩展版本查询次数不随资产数量线性增长。
- 发布历史列表不读取快照正文；快照正文只在版本详情、恢复或发布编排确实需要时读取。

### 16.4 Phase 6 工作台整合验收

- 工作台首屏返回对象、入口和扩展的轻量快照，不在普通浏览时执行对象级发布检查和权限全量检查；完整就绪度只在用户执行发布检查或发起发布时执行。
- 分区首次打开后保持组件和数据缓存，来回切换不得重复挂载、重复请求或反复展示整页加载态。
- 左侧数量必须来自与分区列表相同的快照；数量非零时对应分区不能因独立请求口径差异显示空数据。
- 访问入口主视图展示业务名称和中文入口类型，技术编码只作为辅助信息，不把 `RUNTIME` 或全大写下划线编码作为主文案。
- 对象的数据结构、表单、列表、动作、流程和权限设计以内嵌模式进入应用工作台；关闭对象设计后回到原应用分区，不再强制打开第二套看板或新浏览器页签。

## 17. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| “应用”和旧 `BusinessApp` 名称冲突 | 代码和产品继续混乱 | 新聚合统一使用 `BusinessApplication`；旧类只称访问入口 |
| 存量对象关系无法准确合并成应用 | 回填错误 | 保守拆分；歧义入口进入确定性历史应用；提供计数报告 |
| 对象被多个应用复用 | 修改影响扩大 | 显示影响应用、发布检查传播、禁止应用删除级联对象 |
| 表优先与表单优先冲突 | 用户路径割裂 | 应用/创建流程表可见；业务表单深链保留；共享同一 Schema 编译链路 |
| 在线 DDL 造成数据损失 | 生产事故 | 保存/同步分离，高风险 DDL 默认只导出迁移脚本 |
| JS/CSS 扩展越权 | XSS、Token 泄漏、全局样式污染 | 沙箱、受限 API、CSS AST/作用域、功能开关和负例测试 |
| 服务端扩展成为任意执行入口 | RCE/越权 | 注册白名单、声明式 Schema、权限、超时、审计，禁止在线 Java |
| 应用级发布跨度过大 | 部分失败 | 幂等步骤、不可变快照、明确部分结果和补偿，不伪装全局事务 |

## 18. 技术决策

1. 新增真实应用聚合，不复用或重命名 `ai_business_app`。
2. 应用与对象使用多对多关联，不在 `ai_business_object` 增加强制单一 `application_id`。
3. `ai_business_app` 新增可空 `application_id` 作为入口归属，保留全部旧接口。
4. 新增 binding `APPLICATION` 目标类型，旧 `APP` 仍表示访问入口。
5. 总览采用一次聚合分页，不在前端拼对象、关系和入口树。
6. 对象设计继续复用 Forge 的统一 Schema 和运行态编译，不复制 JeeLowCode 配置表模型。
7. 保存设计与数据库同步分离；高风险 DDL 默认只导出脚本。
8. 服务端 Java 增强改为白名单能力绑定，不实现任意在线 Java。
9. 任意 SQL 增强不进入本变更；未来只读 SQL 也必须单独通过安全 Spec。
10. UI 直接依据 Forge 业务结构和企业控制台原则设计，不使用 `ui-ux-pro-max`。

## 19. 待确认项

以下内容已在本 Spec 给出推荐默认值，不阻塞文档评审；进入 `/apply` 前由用户一次性确认：

- [x] 同意新增 `BusinessApplication` 聚合，而不是重命名 `ai_business_app`。
- [x] 同意对象保持可复用，多应用通过关联表共享对象。
- [x] 同意表优先采用“双入口”：新建对象先看表，业务人员可深链进入表单画布。
- [x] 同意首期服务端增强只做白名单能力绑定，不做在线 Java。
- [x] 同意任意 SQL 增强完全排除在本变更之外。
- [x] 同意按 Phase 0→5 分阶段交付，每阶段独立验收后再进入下一阶段。

## 20. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal 文档 | completed | `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | 仅形成方案，未改代码和数据库 |
| Phase 0 兼容冻结 | completed | 访问入口兼容契约测试、四份 SDD 文档 | 用户已确认 HARD-GATE，旧 `/ai/business/app` 语义保持不变 |
| Phase 1 应用聚合基础 | completed | `V1.0.27`、`BusinessApplication*`、入口/Binding 兼容改造及目标测试 | 37 个目标测试、XML/SQL 静态门禁和 Admin 聚合构建通过；未执行真实迁移 |
| Phase 2 应用优先总览 | completed-static | 应用聚合计数/父域子树、前端 API/路由、应用列表、两步新建和失败重试 | 41 个目标测试、定向 ESLint、前端 build、XML/SQL 静态检查和 Admin 聚合构建通过；浏览器/API/性能待真实环境回填 |
| Phase 3 应用工作台与表优先设计 | completed-static | 七分区工作台、对象编排、表映射首屏、数据库差异预览和显式同步 | 56 个 Phase 1～3/兼容测试、定向 ESLint、前端 build、Mapper XML 和 Admin 聚合构建通过；真实数据库/API/浏览器待回填 |
| Phase 4 受治理扩展中心 | completed-static | `V1.0.28/V1.0.32`、扩展实体/版本/锁/状态机、Worker 沙箱、CSS 作用域、Java 服务增强白名单、钩子矩阵、JS/CSS 引导式代码工作台和开发指南 | 历史证据包含后端 75 tests、前端安全 26 tests、Admin 42 模块和前端生产构建；本次钩子矩阵、代码工作台和 Java 契约增量按用户要求未执行验证，真实 Flyway/API/浏览器待用户验证 |
| Phase 5 应用级协调发布 | completed-static | `V1.0.29`、不可变应用版本、发布运行单、就绪度、六步发布、恢复/回滚和发布历史前端 | 代码与自动化契约已完成；按用户要求未运行 Phase 5 测试、构建、Flyway、API 或浏览器验证 |
| Phase 6 工作台体验修复 | completed-static | 轻量快照、分区缓存、入口中文化、应用内嵌对象设计器、应用数据库表初始化合并、访问入口两步向导和 `V1.0.31` 入口类型字典 | 历史前端 ESLint/build、后端生产源码编译及 XML/SQL 静态检查通过；本轮易用性增量按用户要求只做静态检查，真实浏览器/API/Flyway 待用户验收 |
| Phase 7 模板化快速搭建与无入口预览 | completed-static | 三套模板及资产来源选择、统一模板事务服务、模型菜单收口、无入口发布、真实 CRUD 草稿预览和工作台顶部压缩 | 仅完成目标引用、迁移 placeholder/版本和差异空白静态检查；按用户分工未运行构建、测试、Flyway、API 或浏览器 |
| Phase 10 应用级完整代码包 | completed-static | 应用级批量组装、最新 Schema 重编译、三类布局模板、Mapper XML/SQL 补全、复用代码预览面板和 `V1.0.35` 权限 | 完成目标引用、模板指令、Mapper XML、迁移 placeholder 和差异空白静态检查；真实代码生成、ZIP 解压、构建和页面运行待用户验证 |

## 21. 审查结论

- 当前结论：Phase 1～5 已完成代码实施；应用总览以真实应用为唯一主记录，工作台和对象设计器以数据库表为可见锚点，扩展中心采用隔离执行与显式服务注册，应用级发布采用不可变版本和可恢复运行单。Phase 5 状态为 `completed-static`。
- 架构上已落地独立应用聚合、可复用对象多对多编排、访问入口归属、`APPLICATION` 能力挂接、父业务域子树筛选和应用聚合计数。
- 新应用与旧访问入口 API/路由继续分离；创建初始化失败会保留应用草稿并提供重试或转为空白应用的操作路径。
- 已借鉴 JeeLowCode 的集中配置、保存/同步分离和历史治理思想，同时坚持 Forge 统一 Schema；任意 Java/SQL 和高风险在线 DDL 仍明确排除，Phase 4 的 JS/CSS/服务绑定及 Phase 5 应用级发布均已按 Forge 安全边界实施。
- 真实 Flyway、API、SQL 次数/耗时和多租户 E2E 需要用户环境执行后回填，当前不宣称数据库或浏览器联调完成。

## 22. 确认记录（HARD-GATE）

- **确认状态**：已确认
- **确认时间**：2026-07-13
- **确认人**：用户
- **进入 `/apply` 条件**：用户明确回复同意本 Spec，或指出需要修改的条款并完成修订。

## 23. Phase 7：模板化快速搭建、无入口预览与工作台收口（2026-07-14）

### 23.1 方案判断

- “三套主流模板 + 条件式定向引导”方向合理，能够覆盖当前高频的单表 CRUD、左树右表和主子表场景。
- 三套模板不得演变成三套独立模型、页面协议或发布链路。模板只初始化现有 `LowcodeModelSchema + LowcodePageSchema + BusinessObjectRelation`，生成后继续进入同一业务对象设计器修改。
- 模板预览使用前端原生线框结构表达页面布局，不引入图片素材、独立主题或与 Forge 不一致的视觉体系。
- 模板只决定页面结构，不替用户虚构数据资产。每个主对象、树对象和明细对象都必须在定向引导中选择“从数据库表导入”或“复用已有业务对象”；数据库表和已有对象仍保留为非模板初始化起点。
- 初始化必须以应用草稿为边界执行事务：模板初始化失败时保留应用草稿，但不得遗留半套对象、关系或应用对象编排。

### 23.2 三类模板契约

1. **单表 CRUD**：选择一个数据库表或已有业务对象作为主对象，初始化查询、列表、新增/编辑表单和详情页面。
2. **左树右表**：分别选择主对象和树对象的数据来源；来源确定后从真实字段下拉选择树主键、显示字段、父级字段和主表筛选字段；自动建立引用关系并初始化 `tree-crud` 页面。
3. **主子表**：分别选择主对象和一至多个明细对象的数据来源；从真实字段下拉选择主键及每个明细对象的外键；自动建立 `CHILD_LIST` 关系并初始化 `master-detail-crud` 页面。

### 23.3 易用性规则

- 新建应用第二步优先展示三套模板及布局线框；“空白应用、绑定已有对象、从数据库表开始”收进清晰的其他起点区域。
- 数据库表来源在当前向导内直接选择运行数据源和表；已有对象来源在当前业务域内直接选择，不允许再用名称、编码或表名文本框代替资产选择。
- 来源切换后必须加载对应真实字段供关系配置选择，禁止要求用户记忆并手填字段编码。
- “从数据库选择 / 选择已有对象”使用稳定的两列分段切换，在抽屉和窄屏下不得换行、重叠或覆盖后续表单。
- 仅在选中左树右表或主子表后显示对应配置，不展示无关字段。
- 生成结果必须明确对象数量和下一步，并直接进入应用数据对象分区。
- 模板线框预览保持紧凑并使用边框盒模型，不得越过预览行遮挡模板标题；推荐徽标必须保持单行、固定尺寸，不被弹性布局压缩变形。

### 23.4 轻表设计收口

- 应用/对象向导已经承接数据库表直接导入，独立“轻表设计/模型设计”不再作为简单应用搭建的主导航入口。
- 原模型资产能力不物理删除：菜单降级隐藏，旧路由和 API 保留兼容，供已有模型资产、旧应用和高级维护继续使用。
- 禁止复制一套模型数据到新应用；应用模板和数据库导入都必须写入现有业务对象设计协议。

### 23.5 发布和预览

- “不存在已启用页面入口”从发布阻断项降为提醒；无入口应用允许发布对象、扩展和生成代码。
- 默认发布选择只包含可发布入口；停用或缺少运行配置的入口不因默认全选阻断无页面应用。
- 应用工作台头部新增明显的“预览应用”。预览必须直接新开真实 `/ai/crud-page/:configKey` 页面并复用完整 `AiCrudPage` 运行组件，不再进入自建线框/草稿预览页。
- 未发布对象通过显式 `designPreview=1` 读取设计草稿；配置渲染和动态 CRUD 数据请求都必须校验 `ai:businessObject:design`，正常运行入口仍只允许已发布配置。
- 应用没有对象或主对象缺少 `configKey` 时不打开空预览页，留在工作台给出明确提示并切换到数据对象分区。

### 23.6 工作台布局

- `/app-center/application/:applicationCode` 删除重复的页面内命令栏，将返回动作并入应用头部。
- 压缩头部最小高度和内容区上方间距，应用名称、预览、发布和分区导航在首屏直接可见。

### 23.7 验收标准

- [ ] 三套模板入口和布局差异无需阅读长说明即可识别。
- [ ] 模板卡片预览更紧凑，主子表标题和推荐徽标不被遮挡或压缩。
- [ ] 单表模板选择一个数据库表或已有对象后生成可修改的基础表单/列表。
- [ ] 左树右表可分别选择主表/树表来源，并从真实字段选择树关系后生成 `tree-crud` 页面。
- [ ] 主子表可增删子表来源，并从真实字段选择主外键后生成 `master-detail-crud` 页面。
- [ ] 模板初始化失败只保留应用草稿，不遗留半套模板资产。
- [ ] 独立模型设计不再出现在主导航，旧地址仍可兼容访问。
- [ ] 没有页面入口的应用发布检查不出现入口阻断，且工作台直接打开真实 CRUD 页面预览草稿。
- [ ] 应用工作台顶部不再存在重复命令栏和大块空白。

### 23.8 验证边界

- 按用户分工，本轮不执行 Maven、JUnit、前端 Lint/build、API、Flyway、Vite 或浏览器验证。
- 只执行静态引用扫描、迁移版本/路径核对和差异空白检查；真实模板创建、发布、预览和主题适配由用户验收。

## 24. Phase 8：应用草稿图预览与字段配置分层（2026-07-15）

### 24.1 保存、预览和发布语义

- 主子表、左树右表和单表模板初始化完成后必须直接具备草稿预览能力，不得要求用户逐个进入业务对象执行对象发布。
- “保存”只持久化设计草稿；“预览应用”读取应用内最新草稿并重新编译；“发布应用”统一协调发布应用内全部有变更对象。
- 对象级发布保留为高级维护能力，但不是应用预览或应用发布的前置步骤。
- 设计预览禁止复用旧发布阶段留下的 `searchSchema/columnsSchema/editSchema/apiConfig`；只要存在最新 `modelSchema + pageSchema`，必须重新构建草稿运行配置。
- 主对象预览和发布编译前必须根据当前关系表和子对象最新草稿重新水合 `modelRefs`，避免子对象字段变化后主对象仍持有旧字段快照。
- 预览请求继续校验 `ai:businessObject:design`；正常运行请求继续只读取不可变发布版本，不放宽权限或发布门禁。

### 24.2 字段资产与页面用法分层

- 字段资产是字段身份、存储和业务语义的唯一事实来源，负责字段名称/编码、数据库列、物理类型/长度/精度、业务类型、字典、关联、公式、自动编号、数据约束和安全属性。
- 表单用法负责当前表单是否使用、排序、布局、标题覆盖、提示文案、控件展示、只读/隐藏和表单校验；列表用法负责查询/列选择、顺序、列宽、对齐、渲染、固定、排序和点击动作。
- 页面设计器的修改默认只写页面 Schema，不得无条件反写字段资产。需要把页面配置推广为字段默认时，必须由用户执行显式“设为字段默认”。
- 字段资产变化只更新仍处于继承状态的页面用法；已经存在页面覆盖的属性继续保留，并提供“恢复字段默认”。
- 字段编码、数据库映射、字典/关联语义和安全约束等全局属性仍需传播引用；页面标题、宽度、控件表现和显示规则不得反向修改这些全局属性。
- 运行态有效值遵循“字段默认 → 页面覆盖 → 动态规则”的展示优先级，但页面与动态规则都不得放宽数据库/业务硬约束。

### 24.3 字段工作台交互

- 保留“字段与数据库映射”入口，继续满足开发人员先看数据库表和字段结构的需求；不把数据库能力塞入表单或列表设计器。
- 字段页改为紧凑的字段列表和固定右侧属性面板；桌面端不再使用“摘要卡 + 中央大弹窗”，窄屏时属性面板降级为抽屉。
- 属性面板收敛为“业务定义 / 数据库 / 规则与安全”三组；表单/列表显示开关和查询方式从全局字段配置移除或明确降级为“新页面默认建议”。
- 表单和列表属性区显示字段资产只读摘要、继承/覆盖状态、编辑字段定义和恢复默认入口，避免业务人员误以为需要维护多份字段定义。
- 视觉沿用 Forge 当前设计器的紧凑企业工作台、字段行、拖拽手柄、主题变量和右侧属性栏，不引入独立颜色体系或模板化渐变装饰。

### 24.4 验收标准

- [ ] 新建主子表应用无需发布任一对象即可在“预览应用”看到主表和全部明细表单。
- [ ] 复用已发布对象后新增主子关系，草稿预览仍展示最新关系，不读取旧运行 Schema。
- [ ] 修改子对象字段后再次预览，主对象明细字段同步刷新。
- [ ] 发布应用统一发布全部有变更对象，不要求用户按对象逐个发布。
- [ ] 字段数据库列/类型修改在表单和列表中可读取，但不覆盖已有页面布局。
- [ ] 表单隐藏、标题、占位符和控件展示只影响当前表单，不修改列表或全局字段。
- [ ] 列表列宽、渲染和查询组件只影响当前列表，不修改字段资产。
- [ ] 字段工作台不再通过中央大弹窗编辑普通属性，并能清楚区分字段定义与页面用法。

### 24.5 验证边界

- 按用户既有分工，本阶段不执行 Maven、JUnit、前端 Lint/build、API、数据库、Vite 或浏览器验证。
- 补充回归测试源码和静态契约扫描；只执行目标引用检查和差异空白检查，真实主子表预览和交互由用户验收。

## 25. Phase 9：字段属性、关系画布与应用概览密度优化（2026-07-15）

### 25.1 字段属性排版与必填默认值

- “业务定义”按字段身份、默认值与提示、数据约束、关联配置、备注分组，固定右侧栏内不再连续堆叠无层次表单项。
- 字段编码、数据库列和物理类型集中在“数据库与开发”，业务定义不重复展示开发字段。
- 用户把字段切换为必填时，如果当前默认值为空，按字段类型生成确定且安全的默认值：数字/金额/开关为 `0`，日期/日期时间为当前值，复选/文件/图片为 `[]`，文本为 `-`。
- 字典、单选、人员、部门、地区和对象引用没有可推断的合法业务值，不写入伪造 ID 或字典值；界面明确提示用户选择有效默认值。
- 自动默认值只在用户打开必填或主动切换字段类型时生成，不在打开已有字段时静默改写草稿。

### 25.2 可编辑关系 ER 图

- 复用现有 `LowcodeErDiagram`，不引入第二套画布库或修改后端关系协议。
- 关系图成为关系配置的默认入口。对象卡片继续支持拖动布局，字段行新增连接点；从当前对象字段拖到目标对象字段后创建或更新 `DETAIL` 关系。
- 只允许当前对象与同业务域目标对象连线；目标对象之间互连、同对象自连和无字段端点连接必须拒绝并给出明确提示。
- 点击已配置关系连线后定位到关系属性区；关系属性按关系身份、字段端点和页面行为重排，高级选择器、字段映射、审批数量保持折叠。
- “新增关系”表单按“选择目标对象 → 确认字段端点 → 页面展示”分区，保留自动字段推断和可修改能力。
- 字段联动使用可读的“控制字段 → 联动方式 → 目标字段”流式布局，数据源细节和状态策略独立分区。

### 25.3 应用概览密度

- 工作台外层、内容区和概览模块只保留一级必要留白，避免外层 16px、内容 16px、模块 20px 多层叠加。
- 应用头部高度、侧栏宽度、概览标题间距、配置行高度和空状态高度统一压缩，同时保持小屏横向导航与操作可达。
- 颜色、边框和文字继续使用 Forge 主题变量，不新增独立蓝绿背景体系。

### 25.4 验收标准

- [ ] 420～460px 字段属性栏内各分组边界清楚，无横向重叠或连续拥挤表单。
- [ ] 必填数字、金额、开关、日期、日期时间和文本字段能自动获得对应默认值；引用和字典字段不会生成无效值。
- [ ] 用户可在 ER 图从当前对象字段拖到目标对象字段创建关系，并点击连线进入配置。
- [ ] 新增关系与字段联动表单在桌面和窄屏下均有稳定布局。
- [ ] 应用概览首屏比当前版本明显紧凑，无多层 padding 造成的大块留白。

### 25.5 验证边界

- 延续用户自行验证分工，不执行 Maven、JUnit、前端 Lint/build、API、数据库、Vite 或浏览器。
- 仅执行目标引用扫描和差异空白检查；真实拖线、表单默认值、滚动和小屏行为保持 `pending-user`。

### 25.6 应用总览紧凑卡片

- 应用总览以应用为主体，默认使用紧凑卡片网格，不再保留需要六列对齐和 `1180px` 最小宽度的表格。
- 卡片以 268px 为紧凑基准宽度并等分当前行剩余空间，高度控制在约 160～175px；列数必须按容器真实可用宽度形成 4/3/2/1 列，不能因为固定 320px 上限少排一列并在右侧留下大块空白。宽屏少量应用继续保留空轨道，不把单个应用拉伸成整行大卡片。
- 卡片固定包含应用身份、设计状态、业务域、更新时间、对象/入口/流程/扩展计数和底部操作；描述只显示一行。
- 进入、草稿发布和更多操作始终在卡片底部可见；点击卡片主体或按 Enter 进入应用。
- 分页继续位于网格下方；列表区域只允许纵向滚动，不再依赖横向滚动寻找操作列。

## 26. Phase 10：应用级完整代码包（2026-07-15）

### 26.1 能力边界

- 应用管理新增“代码预览与下载”，以 `BusinessApplication` 为生成范围，不再要求用户先创建或定位某个访问入口。
- 继续复用现有 `LowcodeRuntimeConfigBuilder + AiCrudCodegenService + VelocityCodegenStrategy`，不建设第二套生成器、模板目录或对象协议。
- 首期完整代码包覆盖应用内数据对象相关的后端 Java、Mapper XML、前端页面/API、SQL、运行配置和应用清单；流程、扩展和外部集成继续以应用清单声明，不生成未经治理的 Java/SQL 实现。
- 默认选择应用内全部数据对象；用户可以批量选择本次生成范围。主对象被选择时，其主子表明细对象或左树对象作为同一页面聚合依赖生成；明细对象生成独立 Entity、Mapper、DTO、Query、Service 和 ServiceImpl，页面写操作仍由主对象聚合 Controller 统一提交，不重复暴露冲突的独立 Controller。

### 26.2 配置一致性

- 草稿来源在生成前刷新主对象关系图，重新装配最新子对象字段、主外键、树节点字段和页面布局；最新页面 Schema 编译出的 `layoutType` 必须覆盖旧配置，不得沿用旧的 `layoutType/searchSchema/columnsSchema/editSchema/options` 覆盖当前设计。
- `simple-crud` 生成单表 CRUD；`tree-crud` 生成左树右表页面、树查询和树对象 Mapper；`master-detail-crud` 生成主子表提交 DTO、明细独立 Service，以及主 Service 中的明细读取、事务新增、替换更新和级联清理逻辑。
- 主子表代码生成优先读取派生的 `masterDetailConfig.children`，配置缺失或不完整时必须从 `pageSchema.modelRefs` 的对象关系恢复主外键；两处都无法解析时阻断生成，不得静默退化为单表 CRUD。
- Velocity 模板上下文中的关联对象元数据必须可被模板引擎公开访问；所有 Java 产物生成后扫描未解析的 `$...`/`${...}` 引用，发现残留或任一模板渲染异常时整体失败，不得跳过文件后继续返回 ZIP。
- 发布来源只读取各对象已发布配置；任一选中对象未发布时返回具体对象名称，不静默降级为草稿。
- 每个对象使用确定性的独立业务接口前缀；多个对象文件合并到同一可部署目录时必须检测路径冲突，内容不同的同路径文件直接阻断并指出冲突对象。
- 代码包增加应用级 `README.md` 和 `config/application-manifest.json`，记录应用、对象角色、布局类型、接口前缀和本次生成范围。

### 26.3 预览后下载

- 复用现有代码预览工作台和 CodeMirror 文件查看能力；应用模式只增加对象批量选择和应用摘要，不复制文件树、编辑器或下载逻辑。
- 应用模式默认收起代码包设置；用户展开后设置内容在自身区域滚动，文件树和源码预览区始终保留明确的最小高度。
- 打开工作台后默认加载全部对象并生成预览；对象选择、来源或代码包设置变化后，旧预览立即失效。
- 下载按钮只有在当前参数已经成功预览后才可用，确保用户看到的文件集合与下载请求使用同一组参数。
- 应用总览卡片和应用工作台头部都提供代码入口；两处打开同一个应用级代码工作台。

### 26.4 验收标准

- [ ] 单表应用能预览并下载完整前后端、Mapper、SQL 和配置文件。
- [ ] 左树右表应用的预览文件包含树对象实体/Mapper、树接口和 `TreeCrudTemplate` 页面配置。
- [ ] 主子表应用的预览文件包含明细实体/Mapper/DTO/Query/Service/ServiceImpl、主子 DTO，以及主 Service 中的明细查询、事务保存和级联清理，并使用 `MasterDetailCrudTemplate`。
- [ ] 生成的 Java 文件不包含 `${table.className}`、`${child.className}` 等未解析模板变量；模板异常不会返回不完整代码包。
- [ ] 主子派生配置缺失时仍可根据页面模型关系生成子表查询和事务保存；关系本身缺失时返回明确错误，不导出普通单表实现。
- [ ] 应用包含额外共享对象时可以一次选择多个对象并下载为一个 ZIP，不产生同路径静默覆盖。
- [ ] 修改对象选择或代码包设置后必须重新预览，不能直接下载旧结果。
- [ ] 应用总览和应用工作台使用同一预览组件与同一后端生成链路。
- [ ] 应用代码面板默认优先展示文件树和源码区，展开设置后预览区不被压缩至不可用。

### 26.5 验证边界

- 延续用户自行验证分工，不启动服务，不执行数据库、API、Vite 或浏览器联调。
- 本阶段补充契约测试源码、目标引用扫描和差异空白检查；真实生成内容、ZIP 解压和三类页面运行由用户验收。

## 27. Phase 11：低代码协议与下载代码自动适配（2026-07-15）

### 27.1 目标与原则

- 下载代码必须以当前低代码 JSON 协议为唯一事实来源，禁止前端生成模板继续复制在线运行页的字段、表单、动作和布局解释逻辑。
- 在线运行页与下载页面共用同一个低代码运行解释器；以后新增低代码协议字段或页面能力时，只要共享运行解释器已经支持，重新下载的页面自动获得同等能力。
- 下载后端默认使用“协议运行模式”：保留业务专属 Controller URL，但 CRUD、主子事务、树查询、公式、自动编号、唯一约束、加密、数据权限、导入导出和后续运行能力统一委托现有动态 CRUD 运行内核。
- 继续生成 Entity、Mapper、Mapper XML、DTO、Query、Service 和 ServiceImpl 作为可读的数据结构与扩展脚手架；协议运行 Controller 不绕过共享运行内核自行复制业务规则。
- “完全对应”定义为零静默丢失：每份代码包必须携带完整协议快照和覆盖报告；协议缺失、JSON 无法解析或运行契约不完整时整体阻断下载。

### 27.2 协议快照与运行契约

- 每个生成对象输出四类确定性资产：前端 `runtime-config.json`、后端 `META-INF/forge-lowcode/<configKey>.json`、`config/<configKey>-protocol.json` 和 `config/<configKey>-coverage.json`。
- 协议快照必须包含原始 `modelSchema`、`pageSchema`、编译后的 search/columns/edit/api/options，以及 dict/desensitize/encrypt/trans 配置；嵌套 JSON 以对象保存，不得再次降级为不可审查字符串。
- 后端资源保留完整 `AiCrudConfig` 可反序列化结构，并强制使用独立 `generated_*` 运行配置键、`mode=CONFIG`、`buildMode=LOWCODE`、`publishStatus=PUBLISHED`，避免与平台数据库中的设计配置冲突。
- 共享运行配置注册器只加载 classpath 下的 `META-INF/forge-lowcode/*.json`，同一配置键内容冲突时启动失败关闭；动态 CRUD 服务优先解析 `generated_*` 内嵌配置，普通平台配置仍按数据库读取。
- 覆盖报告声明 `shared-runtime + protocol-passthrough` 自动适配策略，并记录模型、页面、表单、视图、联动、运行 Schema、安全配置和业务 API 的承载方式；不得用“忽略未知字段”作为兼容策略。

### 27.3 前端共享解释器

- `crud-page.vue` 增加外部 `runtimeConfig` 输入，在提供内嵌配置时跳过 `/ai/crud-config/.../render` 请求，其余多页面、表单资产、字段渲染、联动、动作、Hook、详情面板、树形和主子表逻辑保持同一执行路径。
- 新增稳定的 `LowcodeRuntimePage` 组件出口，在线路由页和生成页面均复用该出口；生成模板只导入同目录 `runtime-config.json` 并传给共享组件。
- 生成页面不得继续维护 `transformColumns`、`transformFields`、字典预加载、树节点归一化或主子表 props 转换副本；这些能力只允许存在于共享运行解释器。
- 内嵌运行页默认不改写浏览器和 Tab 标题，路由参数、公开查询参数、详情页和表单打开模式继续按生成页面所在真实路由解析。

### 27.4 后端协议运行模式

- 生成 Controller 保持 Forge POST-safe 契约：`GET /page`、`GET /tree`、`POST /getById`、`POST /add`、`POST /edit`、`POST /remove/{id}`、`POST /removeBatch`。
- 业务 Controller 调用 `DynamicCrudService` 和 `DynamicCrudExcelService`，使用内嵌生成配置键执行；新增、修改、删除继续发布既有业务事件，确保触发器、流程和业务动作不因下载代码入口变化而失效。
- 生成配置必须继续使用业务专属 `apiBase`，前端和生成代码中不得出现 `/ai/crud/` 通用接口地址。
- classpath 配置注册只负责提供不可变协议快照，不新增第二套 SQL 执行器、公式引擎、校验器、触发器或权限实现。

### 27.5 验收标准

- [ ] 生成页面模板不再包含在线运行字段/表单转换副本，只包含共享运行组件和本地 JSON 快照引用。
- [ ] 单表、左树右表和主子表生成配置均携带完整 model/page/options，在线运行页支持的配置由同一共享解释器生效。
- [ ] 自动编号、公式、唯一约束、加密、数据权限、主子事务和导入导出经生成业务 Controller 仍进入动态 CRUD 运行内核。
- [ ] 生成资源使用独立 `generated_*` 配置键，不覆盖或误读取数据库同名设计配置。
- [ ] ZIP 包包含 protocol、coverage、前端 runtime config 和后端 classpath config；缺少任一资产时生成失败。
- [ ] 协议 JSON 解析失败、低代码对象缺少 model/page Schema、classpath 同键资源冲突时失败关闭，不返回不完整代码包。
- [ ] 后续在 model/page/options 内新增嵌套字段时，协议快照无需修改模板字段清单即可原样保留，并由升级后的共享运行内核解释。

### 27.6 验证边界

- 延续用户自行执行真实服务、数据库、API、Vite 和浏览器验收的分工。
- 本阶段补充后端单元/契约测试源码、前端静态契约、模板引用和差异空白检查；实际 ZIP 解压、生成模块编译和三类页面运行保持 `pending-user`。

### 27.7 实施记录

- 公共 Velocity 生成入口统一调用 `GeneratedLowcodeRuntimeConfigBuilder`，应用级、访问入口、低代码应用和旧 configKey 下载入口均派生独立 `generated_*` 运行键；源配置键只保留为追溯信息和原输出目录，不再作为 classpath 运行键。
- 业务 API 投影固定包含 page、tree、POST-safe detail/add/edit/remove、导入导出和异步导出任务端点；协议中的当前对象 `/ai/crud/...` 改写为业务 API，`/ai/crud-page/...` 改写为生成前端路由，无法确定归属的其它平台通用接口失败关闭。
- 静态验证已覆盖完整资产路径、未来嵌套字段测试源码、薄前端模板、共享运行内核委托、树/主子配置引用、生成键隔离和差异空白；真实 ZIP、生成代码编译和三类页面运行仍按本阶段边界由用户验收。

## 28. Phase 12：下载后端静态协议编译与可持续二次开发（2026-07-15）

### 28.1 决策修正

- 用户反馈确认 Phase 11 的“下载 Controller 委托 `DynamicCrudService`”不利于专业开发人员修改复杂查询、复杂新增和领域事务，因此该后端交付决策自本阶段起废止。
- 在线低代码预览和平台运行继续使用 `DynamicCrudService`；下载源码不改变在线运行链路，也不删除动态运行能力。
- 下载页面继续使用共享 `LowcodeRuntimePage + runtime-config.json`，保证新增前端协议能力在共享解释器升级并重新下载后自动生效。
- 下载后端改为“静态协议编译”：当前低代码 JSON 经统一代码生成入口编译为普通 Controller、MyBatis-Plus Service、Mapper 和 Mapper XML。所有应用级、访问入口级和旧 configKey 下载入口必须复用该编译入口，禁止再产生第二套后端生成逻辑。

### 28.2 下载后端分层

```text
Generated Controller
    -> I<Object>Service
    -> <Object>ServiceImpl extends ServiceImpl<Mapper, Entity>
    -> Mapper + Mapper XML
```

- Controller 只做协议转换、参数接收和返回值封装，保持 `GET /page`、`GET /tree`、`POST /getById`、`POST /add`、`POST /edit`、`POST /remove/{id}`、`POST /removeBatch` 契约。
- 基础 `selectById/insert/updateById/deleteById/deleteBatchIds` 使用 MyBatis-Plus；分页、列表、树、主子明细和可扩展复杂查询必须落在 Mapper XML，Service 禁止拼装 `LambdaQueryWrapper`。
- 主子新增、修改、删除和批量导入的事务边界位于生成 Service；主 Service 通过 Mapper/Manager 编排明细，不互相注入 Service。
- 导入导出不得通过 `DynamicCrudExcelService` 间接回到动态 CRUD。下载代码改用 `forge-starter-excel` 元数据能力，导出数据源指向生成 Service，导入解析成功后进入生成 Service 的事务新增链路。

### 28.3 二次开发扩展边界

- 每个主生成对象输出 `ServiceExtension` 接口，分页、列表、树、详情、新增、修改、删除、批量删除和导入均提供 around 扩展点；没有扩展 Bean 时执行生成的默认 MyBatis-Plus/Mapper XML 实现。
- 用户实现类由 Spring 组件扫描并可用 `@Order` 排序；扩展可在默认逻辑前后增加校验/编排，也可不调用 `operation.proceed()` 完全替换默认查询或写入逻辑。
- 生成器只覆盖生成接口和默认实现，不向用户实现目录写 Java 文件。ZIP 只携带 `.java.example` 示例和文件所有权清单，用户复制后的正式扩展类属于 `USER_OWNED`，后续重新下载不得要求覆盖。
- 复杂跨域逻辑下沉 Manager；复杂查询新增自定义 Mapper 方法及 XML SQL。不得通过 Service 相互注入绕开循环依赖约束。

### 28.4 后续低代码能力自动适配

- “自动适配”定义为平台能力交付约束：新增低代码协议能力时必须同时补充共享前端解释器或静态后端编译规则及契约测试；所有下载入口复用统一编译器后，重新下载自动获得新实现。
- 完整协议快照继续原样保留未知嵌套字段，但后端覆盖报告必须区分 `FRONTEND_RUNTIME`、`BACKEND_STATIC_COMPILED` 和 `REQUIRES_EXTENSION`，禁止把仅保存 JSON 误报为后端已实现。
- 删除下载后端 classpath 配置注册和 `META-INF/forge-lowcode/*.json` 运行依赖；协议文件只用于审查、追溯和后续重新生成，不在下载应用运行时驱动 CRUD。
- 每个对象输出文件所有权清单：`GENERATED` 可由新包替换，`CREATE_ONCE_SAMPLE` 仅供首次复制，用户自行创建的实现文件为 `USER_OWNED` 且不进入生成文件集合。

### 28.5 验收标准

- [ ] 生成 Controller 只注入 `I<Object>Service` 和标准 Excel 能力，不引用 `DynamicCrudService`、`DynamicCrudExcelService` 或 classpath 低代码配置注册器。
- [ ] 生成 ServiceImpl 继承 MyBatis-Plus `ServiceImpl`，分页/列表/树/主子查询来自 Mapper XML，新增/修改/删除使用 MP 基础方法并保留事务。
- [ ] 单表、左树右表和主子表仍分别生成完整静态代码，主子新增/修改/删除在主 Service 的同一事务内完成。
- [ ] 用户可新增一个不被生成器输出的扩展实现，完全替换分页查询或在新增前后执行复杂逻辑；多个实现按顺序组成调用链。
- [ ] 导入成功数据调用生成 Service，导出元数据的数据源 Bean 和查询方法指向生成 Service，不再间接依赖动态 CRUD。
- [ ] ZIP 保留前端 runtime config、完整 protocol、静态 coverage 和 ownership 四类资产，不再包含后端 classpath runtime config。
- [ ] 在线 `/ai/crud/**` 动态运行保持原行为，删除下载 classpath 注册能力不影响设计态预览和已发布低代码应用。

### 28.6 验证边界

- 延续当前变更既定分工，不自动执行 Maven/JUnit、前端 build、服务、数据库、Vite 或浏览器。
- 本阶段补充模板渲染契约测试源码并执行目标引用、模板指令平衡、XML/JSON 静态解析和差异空白检查；真实 ZIP、生成模块编译、三布局 CRUD、导入导出和扩展 Bean 执行保持 `pending-user`。

## 29. Phase 13：下载包命名与输出策略（2026-07-15）

### 29.1 实体注解零冗余

- 字段未配置脱敏、脱敏类型为空或类型为 `NONE` 时，实体字段不得生成 `@Desensitize`；当全部字段都没有真实脱敏策略时，也不得生成 `Desensitize` 和 `DesensitizeType` import。
- 脱敏类型在进入模板前统一去空白并转为大写；只有真实存在于生成字段中的非 `NONE` 策略才打开实体级 `hasDesensitize` 开关，不能因失效或未知字段产生无用 import。
- 字典、逻辑删除等其它字段注解继续按已有协议条件生成，本阶段不改变其语义。

### 29.2 类名派生协议

- 下载设置新增 `entityPrefix` 和 `stripTablePrefixes`。类名统一按“物理表名 -> 删除第一个匹配的配置表前缀 -> PascalCase -> 追加规范化实体前缀”派生；Entity、DTO、Query、Mapper、Service、Controller、主子关联类型和文件名必须共享同一个最终 `className`。
- `entityPrefix` 允许为空；非空值按 PascalCase 规范化，最终必须是合法 Java 标识符。禁止只在 Entity 模板追加前缀，避免 Mapper、Service 和关联类型引用错位。
- `stripTablePrefixes` 是有序字符串列表，支持用户清空以保留完整表名；未配置时默认 `sys_`、`ai_`、`t_`、`tb_`。只删除首个匹配项，不做中间字符串替换。
- 主表、左树对象和全部明细表使用同一规则。多个对象生成相同最终类名或输出到同一路径但内容不同时继续失败关闭，不允许静默覆盖。

### 29.3 输出范围与目录

- 下载设置新增后端 Java 根目录 `backendBasePath`、Mapper XML 根目录 `mapperXmlBasePath`、前端 API 根目录 `frontendApiBasePath`；现有 `frontendBasePath` 明确表示前端页面根目录。所有路径都归一化为 ZIP 内相对路径，拒绝空路径、`.`、`..` 路径段和绝对路径逃逸。
- 下载范围新增 `includeBackend`、`includeFrontend` 和 `includeExcelSql`；后端关闭时不生成 Java、Mapper XML 和扩展示例，前端关闭时不生成页面、API 和页面侧 runtime config，Excel SQL 可在总 SQL 开关开启时独立关闭。
- 完整协议快照、覆盖报告、ownership 和 README 始终生成，不提供关闭选项；这是低代码配置可审查、可追溯和后续重新生成的最低交付契约。
- 菜单 SQL、字典 SQL 和 Excel SQL 继续受 `includeSql` 总开关约束。字典 SQL 仅在存在真实字典配置时生成。

### 29.4 设置治理与自动适配

- 应用级、访问入口级、低代码应用级和旧 configKey 下载入口必须把新增设置写入同一 `options.codegen` 协议，并由 `BusinessCodegenConfigAssembler` 镜像到公共生成入口。
- 下载设置只开放会影响工程落位和命名的高价值选项；Service/Mapper/Controller 后缀、Lombok、基础实体、REST 方法和框架注解保持 Forge 固定约定，避免生成组合不可编译。
- 以后新增代码生成设置时，必须同时补齐请求 DTO、领域默认值、全部入口持久化/回显、公共生成策略和契约测试；不得在单个入口或单个模板内维护私有设置。

### 29.5 验收标准

- [ ] 无脱敏或 `NONE` 脱敏字段的实体不包含脱敏注解及 import；真实 `PHONE` 等策略仍正确生成。
- [ ] `tf_customer + strip=tf_ + entityPrefix=Biz` 的全部类型和文件统一为 `BizCustomer*`，主子表和左树对象同样生效。
- [ ] 用户可清空表前缀列表保留完整物理表类名；非法实体前缀和非法输出路径在生成前返回明确错误。
- [ ] 后端、前端和三类 SQL 子项可独立选择，关闭项不会残留文件；协议、coverage、ownership 和 README 始终存在。
- [ ] 保存设置后重新打开可完整回显，应用级和访问入口级预览/下载使用相同设置且预览签名会随设置变化失效。

### 29.6 验证边界

- 延续当前变更既定分工，不自动执行 Maven/JUnit、前端 build、服务、数据库、Vite 或浏览器。
- 本阶段补充设置与模板契约测试源码，执行目标引用、模板条件、路径安全和差异空白静态检查；真实 ZIP、生成模块编译和设置回显交互保持 `pending-user`。
