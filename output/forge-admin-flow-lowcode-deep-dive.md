# Forge Admin 深度解析：低代码平台与工作流引擎是怎么跑起来的

## 前言

大多数中后台框架的工作流和低代码是"有这个功能"和"能用"的区别。Forge Admin 把这两个模块做到了"能用"之上——设计态可见、运行态可控、产物可回滚、AI 能介入。

这篇文章拆开这两个模块，看看从"一句需求"到"一个带审批流的业务页面"，中间到底发生了什么。所有内容基于源码，不是概念图。

---

## 一、低代码平台：双层架构，设计态与运行态分离

### 核心概念：两套元数据体系

理解整个平台最关键的一点：存在两套并行的元数据体系，运行时统一收敛到同一套 CRUD 配置上。

**低代码层（负责"造表 + 造页面配置"）**

- **业务领域（AiLowcodeDomain）**：支持父子树形结构，承载表名前缀、configKey 前缀、默认布局类型等领域级规则。比如"CRM"是一个领域，"采购仓储"是另一个。
- **数据模型（AiLowcodeModel）**：归属某个领域，核心是 modelSchema（JSON 协议），描述字段、索引、唯一约束、关系、主键/租户/审计/逻辑删除策略。tableMode 区分两种：CREATE（在线建表）和 EXISTING（绑定已有表）。
- **CRUD 配置（AiCrudConfig）**：运行态的真正契约，同时承载草稿和已发布状态。

**业务应用层（负责"治理、编排、挂接能力"）**

- **业务套件（AiBusinessSuite）**：比如 CRM、采购仓储，支持父子层级。
- **业务对象（AiBusinessObject）**：归属套件，objectType 分 MASTER（主数据）/DETAIL（明细）/LOOKUP（字典）/TRANSACTION（业务单据）。有完整的设计状态机：草稿 → 设计中 → 就绪 → 已发布 → 已变更，带版本号。
- **业务应用（AiBusinessApp）**：访问入口，entryMode 支持运行态渲染 / 路由 / iframe / 外部链接 / H5 / API 六种方式。
- **业务对象关系（AiBusinessObjectRelation）**：REFERENCE（引用）/DETAIL（明细）/CHILD_LIST（子列表）/MANY_TO_MANY（多对多）。
- **能力挂接（AiBusinessBinding）**：把流程、审批、报表、权限、消息、触发器、导入导出、移动端、集成等能力挂到套件、对象或应用上。

两层之间通过 BusinessBootstrapService 幂等映射：领域同步为业务套件，数据模型同步为业务对象，已发布的 CRUD 配置同步为业务应用入口。低代码层负责造东西，业务应用层负责管东西，二者通过 configKey 和 objectCode 在运行时汇合。

### 完整工作流程：六个阶段

**阶段 1：领域与模型设计。** 前端用领域树面板维护业务领域，用模型设计器设计数据模型。字段定义包含数据类型、表单组件类型、字典类型、敏感字段标记、加密算法、公式配置、业务字段类型、索引、唯一约束、对象关系。后端提供 DDL 预览接口，按数据库方言生成建表语句，也支持反向工程——从已有数据源表导入生成模型。

**阶段 2：AI 智能生成（可选）。** LowcodeAiController 提供 SSE 流式生成接口，输入一句自然语言需求（比如"客户合同回款管理系统"），AI 输出领域草稿、模型草稿（带完整字段）、应用草稿、推荐的页面模板及选择理由。生成逻辑有降级机制：先调 AI，失败回落到规则规划，不会因为 AI 不可用就卡死。

**阶段 3：页面搭建。** 可视化搭建器提供两个 Tab：列表页面（网格布局设计器）和表单与详情（三栏拖拽设计：组件面板 + 画布 + 属性面板）。产物是 LowcodePageSchema，包含布局类型、列表网格布局、表单分区、CRUD 钩子规则。同时支持代码预览和下载，基于 Velocity 模板生成 Vue + Java 代码包。

**阶段 4：发布。** 发布不是"保存生效"，而是一个完整的编译流程：

1. 解析 modelSchema + pageSchema
2. 校验表存在且为单字段主键（在线建表模式需专门权限并二次确认）
3. 校验发布态策略
4. 把 modelSchema + pageSchema 编译成运行态协议——产出 searchSchema、columnsSchema、editSchema、apiConfig、options、dictConfig、desensitizeConfig、encryptConfig、transConfig
5. 写回 AiCrudConfig，版本号加一，状态标记已发布
6. 注册菜单资源
7. 同步到业务应用层
8. 保存版本快照，支持回滚

**阶段 5：运行态渲染。** 用户访问运行态页面时，前端三层分发：自定义网格布局优先，其次按布局类型匹配模板组件（simple-crud / tree-crud / master-detail-crud），最后降级到通用 AiCrudPage 组件。模板组件本质上是 AiCrudPage 的薄封装，TreeCrudTemplate 处理左树右表，MasterDetailCrudTemplate 处理主表表单加子表明细。

**阶段 6：运行态数据。** 后端 DynamicCrudController 提供分页、树形、详情、增删改、导入导出的完整接口，所有读写都经过 DynamicCrudService 按 configKey 找配置后动态执行。

### 运行态的心脏：一次新增操作发生了什么

DynamicCrudService.insert 是平台的心脏。一次新增操作的编排顺序：

1. 获取配置，校验必须为已发布状态
2. 切换到模型配置的运行数据源（支持多数据源 + MySQL/Oracle/PostgreSQL 方言）
3. 构建可写字段白名单
4. 执行自动编号（按字段配置的编码规则生成）
5. 生成单据编号（如果配置了单据规则）
6. 执行 STORED 公式（计算结果参与写库）
7. 过滤系统字段（id、tenantId、createBy 等）
8. 唯一约束校验（含联合唯一，空值跳过）
9. 字段加密
10. 真正写库
11. 子表插入后刷新主表聚合公式

查询返回时统一后处理：解密 → 字典翻译 → 脱敏。

这意味着：配置的公式、编码规则、唯一校验、加密、脱敏，不是前端做的花架子，而是后端严格执行的。数据一致性有保障。

### 触发器引擎：事件驱动，不只是通知

触发器不是简单的"发个消息"，而是一个完整的事件驱动模型。

**事件类型**：RECORD_CREATED / RECORD_UPDATED / RECORD_DELETED / STATUS_CHANGED（自动检测 status/state/audit_status/documentStatus 等字段变化）/ FLOW_APPROVED / FLOW_REJECTED / FLOW_CANCELED。

**条件评估**：支持 and/or/rules 嵌套的逻辑树，操作符包括 eq/neq/gt/gte/lt/lte/in/not_in/contains/is_null/not_null，还有变更专用的 changed/changed_to/changed_from（对比修改前后的数据）。

**动作类型**：

- START_FLOW：发起流程，支持表单字段到流程变量的映射
- SEND_MESSAGE：发消息，接收人规则支持发起人/负责人/创建人/指定用户/指定角色/指定部门/全员
- CREATE_RECORD：在目标业务对象创建关联记录，支持字段映射和静态值
- UPDATE_FIELD：更新目标记录字段
- BUSINESS_ACTION：调用通用业务动作，带幂等控制

**执行保障**：每次执行写日志（SKIPPED/SUCCESS/FAILED/TODO），累计执行次数。BUSINESS_ACTION 动作带幂等 key，由触发器ID+事件类型+对象编码+记录ID+动作编码生成 SHA256，防止重复执行。还支持调度触发，由调度服务定时执行，scenarioType 支持到期提醒、指标阈值等场景模板。

### 编码规则与公式引擎

**编码规则**：模板 + 变量 + 流水号机制，模板用 ${token} 语法。支持预览和生成，业务对象的字段可以配置自动编号规则。

**公式引擎**：

- 支持公式试算（预览计算结果）
- 公式依赖分析（DAG 有向无环图、环检测、拓扑排序）
- 条件规则编译（AST 转表达式引擎）
- 内置函数列表 + 函数市场（可安装扩展函数）
- STORED 公式在写库时计算并存储结果，查询时直接返回

公式字段在表单中强制只读，且去掉 required 校验——因为值是算出来的，不是填的。

### 真实可跑的 demo 业务

项目内置了完整的业务菜单作为佐证：

- CRM：线索、客户、商机、合同
- 采购仓储：仓库、物料、供应商、采购单、出库
- 连锁经营：桌台管理
- 资产设备维护：完整资产管理

这些 demo 证明低代码平台能承载真实业务结构，不是只能跑"用户管理"这种玩具示例。

---

## 二、工作流引擎：独立服务 + 注解 SDK

### 整体架构：三个模块，各司其职

工作流引擎物理上拆成三个 Maven 模块：

| 模块 | 角色 | 关键内容 |
|------|------|---------|
| forge-plugin-flow（核心插件） | 流程领域模型与 Service 实现 | 全部实体、Service、Flowable 事件监听器、SpEL 服务、抄送委托 |
| forge-flow-server（独立服务） | 可独立部署的流程中心 | 19 个 Controller、AI 生成 BPMN、低代码桥接 |
| forge-flow-client（客户端 SDK） | 业务系统接入流程 | FlowClient HTTP 客户端 + 三个注解 + AOP 切面 + Redis 事件订阅 |

**关键设计点**：

- 流程中心的所有 Controller 标注 @IgnoreTenant，忽略多租户隔离，但流程实体本身带 tenantId 字段做逻辑隔离。
- 业务系统调流程中心时携带 X-Inner-Call: true 请求头，流程服务端跳过请求体加解密（服务间明文 JSON）。
- 用户 Token 自动透传，流程中心能知道是谁在操作。
- 事件回传方式可配置：Redis Pub/Sub 或 Webhook HTTP 回调，二选一。

### 业务系统怎么接入流程：三个注解

这是工作流引擎最核心的设计。业务系统不用直接调 Flowable API，通过三个注解就能完成接入。

**@FlowBind：声明绑定关系。** 打在业务 Service 类上，声明这个类绑定的流程模型。支持通配 modelKey = "*"，低代码平台用这种方式监听所有流程事件。

**@FlowStart：业务方法成功后自动发起流程。** 打在方法上，AOP 切面在方法正常返回后，用 SpEL 解析属性，自动调流程中心发起流程。SpEL 上下文支持：#参数名、#p0/#p1（按位置）、#result（返回值），还注册了 BeanResolver，可以 @beanName 调用容器里的 Bean。skipOnError 默认为 true——业务方法抛异常时不发起流程，避免业务失败但流程已发起的不一致问题。

**@FlowCallback / @FlowComplete：流程事件回调。** 流程中心通过 Redis 或 Webhook 把事件推回业务系统，FlowEventSubscriber 接收后按 @FlowBind.modelKey 匹配 Bean，按事件类型路由到方法。支持 6 种事件：流程完成、流程驳回、流程取消、任务创建、任务完成、任务分配。@FlowComplete 是 @FlowCallback 的语义化封装，把通过、驳回、取消分发到三个独立方法，比在一个方法里 if-else 更清晰。

### 流程从设计到运行的完整链路

**设计态**：

- 分类：流程分类是树形结构，带 parentId/ancestors/level。
- 表单：表单 Schema 是 JSON，支持三种类型——dynamic（动态表单）、external（外部表单）、builtin（内置表单）。发布时生成版本快照。表单 Schema 会被解析成字段目录，供节点配置绑定用。
- 模型：流程模型存储 BPMN XML + 表单 JSON + 通知方式。前端提供两种设计器——DingFlowDesigner（审批流设计器，钉钉风格）和 FlowModeler（基于 bpmn-js 17.11 的业务流设计器）。节点属性面板分七个 Tab：基础属性、开始配置、审批设置、办理控制、会签配置、流转条件、结束配置。
- 节点配置：存储审批人类型、多人审批策略、超时设置、操作权限。支持动态任意层级审批，支持动态计算审批人接口。
- 部署：部署时校验 BPMN 必须含图形信息 → 替换 process id 为 modelKey → 规范化 XML → 校验连线引用 → 缺图时自动生成 → 调 Flowable 部署 → 更新状态 → 异步插入版本快照。

**运行态**：

- 发起流程：这里有个关键设计——幂等发起。用 ReentrantLock（key 包含租户ID和业务键）加数据库唯一键实现：已存在运行中实例直接复用，已结束的报错。流程定义不存在时自动从模型部署。发起时自动注入大量内置变量：发起人ID/姓名、部门ID/姓名、业务键、业务类型、流程标题、发起人上级领导、行政区划、发起人角色列表、发起人组织列表。
- 待办产生：Flowable 的 TASK_CREATED 事件触发监听器，创建待办记录。如果既没有处理人也没有候选人/候选组，记错误日志（TASK_ASSIGNEE_MISSING），同时发站内消息通知，并发布 TASK_CREATED 事件给业务系统。
- 审批操作：通过（写审批意见、设置 approved 变量、完成任务）、驳回、转办（setOwner + setAssignee）、退回（跳回上一节点）、终结（删除流程实例）、撤回（发起人撤回）。
- 流程完成：读取 approvalResult 变量判断通过还是驳回 → 更新业务记录状态、结束时间、耗时 → 更新表单实例状态 → 发布事件给业务系统 → 通过时自动抄送（从流程变量的 ccRoleKeys 解析角色，按角色查用户发送）。
- 抄送：两种来源——流程完成时按角色自动抄送；BPMN 中配置抄送节点由 JavaDelegate 执行。抄送支持标记已读、批量已读、未读数量统计。

### AI 生成 BPMN：不是套个提示词就完事

接口是 SSE 流式的，输入自然语言描述，输出完整的 BPMN XML。

**生成链路**：生成 sessionId → 用 Flux.concat 拼装 SSE 流（progress 事件 → AI chunk 事件 → complete 事件）→ 默认 temperature=0.2（低温保证稳定），maxTokens=12000。

**提示词是动态拼装的**，分四块注入上下文：

1. 用户需求：用户输入的自然语言描述
2. 当前流程模型信息：modelKey、模型名、分类、流程类型、表单类型
3. 平台流程配置上下文（核心）：把系统真实可用的配置项作为 JSON 注入，让 AI 复用而非编造——审批人类型（user/role/dept/post/leader/deptManager/initiator/expr，每个都带"生成什么 Flowable 属性"的提示）、表单类型、会签类型、完成条件、超时动作、SpEL 模板（从数据库查真实启用的，最多 50 条）、可用表单、当前节点配置
4. 当前 BPMN XML：用于增量修改而非全量重写

**输出约束**：Agent 系统提示词强制输出纯 JSON 对象，包含 modelKey、modelName、description、bpmnXml 等字段。BPMN XML 有 10 条硬约束：definitions 必须含完整命名空间、process id 必须等于 modelKey 且 isExecutable=true、必须含 startEvent/endEvent/BPMNDiagram、审批人优先用 flowable:assignee/candidateUsers/candidateGroups、条件用 ${amount > 5000} 格式、节点 id 禁止中文等。

**前端处理**：用原生 fetch + ReadableStream 读 SSE，支持 progress/chunk/complete/error 四类事件，带 1 次自动重试和 AbortController 中断。AI 输出解析后加载到 BPMN 画布，如果 BPMNDI 坐标无效会自动重新生成图形信息作为兜底。

### 监控与管理：不只是看，能操作

流程监控不是只读的统计面板，管理员可以直接介入：

- 监控统计：运行中实例数、待办数、今日完成数、超时数、任务趋势（7 天）、流程分布。
- 实例管理：查看实例列表（带运行时长、当前节点、当前处理人），管理员可以终止实例、回退节点、转派任务、删除实例、批量清理、挂起/激活。
- 错误日志：流程错误日志分页查询、详情、统计、重试失败节点、标记已解决。比如 TASK_ASSIGNEE_MISSING（找不到处理人），修复配置后可以重试。
- 工作台：聚合统计待办徽标数，和前端的待办提醒联动。

### 版本管理与条件规则

- 版本管理：流程模型每次部署生成版本快照，支持版本列表、版本详情、版本对比（两个版本的 BPMN XML 差异对比）、版本回退、版本下载。
- 条件规则：可视化条件规则，不用手写 SpEL。前端拖拽配置，后端编译成 Flowable 的条件表达式。
- SpEL 表达式模板：维护可复用的 SpEL 表达式模板，节点配置时直接从下拉选择。比如"获取发起人上级领导"这个表达式，做成模板后所有流程都能用。

---

## 三、两个模块的深度集成

工作流引擎和低代码平台不是两个独立系统，它们深度集成：

**能力挂接**：低代码平台的业务对象通过 AiBusinessBinding（bindingType=FLOW）挂接流程能力，配置默认流程 key。

**单据配置**：AiBusinessDocumentConfig 存储单据编号规则、状态字段、发起人字段、默认流程 key、状态映射。业务单据和流程的绑定关系在这里维护。

**触发器发起流程**：低代码平台的触发器引擎支持 START_FLOW 动作，业务数据变更时自动发起流程，支持表单字段到流程变量的映射。

**流程表单复用**：低代码平台设计的表单可以作为流程表单使用，不用重复设计。

**业务待办办理**：低代码运行态支持 formOnly 模式（仅表单），嵌入流程待办页面，审批人在待办里直接填表审批。

**通配监听**：低代码平台用 @FlowBind(modelKey = "*") 监听所有流程事件，实现流程状态回写业务对象。

### 真实可跑的 demo 流程

项目内置了两个完整的流程示例：

- **请假管理**：完整的请假申请流程，包含申请、列表、审批表单。用 @FlowStart 注解在提交请假时自动发起流程，用 @FlowComplete 接收审批结果回写状态。覆盖发起 → 待办 → 审批 → 抄送 → 监控全链路。
- **采购单审批**：采购单审批测试流程，验证流程和低代码业务对象的集成。

这两个 demo 不是玩具，是完整的业务闭环，可以作为真实业务的参考实现。

---

## 四、小结

### 低代码平台的特别之处

1. **设计态和运行态分离**，中间有明确的编译步骤，产物是可审查的运行态协议
2. **运行态逻辑在后端严格执行**，公式、编码、唯一校验、加密、脱敏不是前端花架子
3. **版本管理和回滚**，发布出错可以回退
4. **触发器是真正的事件引擎**，能发起流程、创建记录、更新字段，带幂等控制
5. **AI 生成有降级机制**，AI 不可用时回落到规则规划，不会卡死

### 工作流引擎的特别之处

1. **物理隔离**：流程引擎独立部署，业务系统通过注解 SDK 接入，不耦合 Flowable API
2. **三个注解搞定接入**：@FlowBind 声明绑定、@FlowStart 自动发起、@FlowCallback 接收回调，业务代码干净
3. **幂等发起**：锁 + 唯一键双保险，防止重复发起流程
4. **AI 生成 BPMN 是认真的**：动态注入平台真实配置作为上下文，AI 复用而非编造，输出有 10 条硬约束保证可用
5. **监控能操作**：不只是看统计，管理员能终止、回退、转派、重试失败节点
6. **和低代码深度集成**：触发器发起流程、表单复用、待办嵌入、状态回写，不是两个独立系统

如果你在评估中后台框架的工作流和低代码能力，重点不是看它"有没有"，而是看它的运行态机制经不经得起推敲。Forge Admin 的这两个模块，是经得起推敲的那种。
---

**体验完整的客户管理能力**：

- 后台演示：http://www.dlforgelab.com:8084/forge/login （admin / 123456）
- Gitee：https://gitee.com/ForgeLab/forge-admin
- GitHub：https://github.com/yaomindong1996/forge-admin

> 你做过的 CRM 踩过最大的坑是什么？公海私海、自动回收、还是数据权限？评论区聊聊。
