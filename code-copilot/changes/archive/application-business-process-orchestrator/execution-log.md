# 执行日志 — 应用级业务流程编排器

> change: `application-business-process-orchestrator`
> started: 2026-08-03

## 2026-08-03 Task 0：HARD-GATE 与实施基线

- 变更范围：仅当前变更的 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 用户授权：当前会话明确要求开始开发，按 Spec 第 9 章六项推荐默认值完成 HARD-GATE。
- 规则基线：已读取根 `AGENTS.md`、`code-copilot/AGENTS.md`、三份 memory、自动化测试标准、流程业务 Skill 共享参考、Forge 编码规范与前端设计 Skill。
- Git 基线：当前分支为 `main`；工作区已有能力开放平台相关修改和 `.DS_Store` 变化，均不属于本变更，实施中禁止覆盖或提交。
- Flyway 基线：当前最新脚本为并行变更中的 `V1.0.82__improve_capability_client_workbench.sql`；本变更分配 `V1.0.83/V1.0.84`，不修改 `V1.0.82`。
- 协议基线：冻结 `businessProcessJson 1.0`、三类完整样例、可信身份矩阵、Process/Node/Approval 状态机与 CAS 条件。
- 安全结论：DAG、单活动审批、受限定时普通用户、同应用子流程深度 5、分阶段旧入口停写、禁止自由 Webhook；任一身份/权限/版本/关联不可信时失败关闭。
- 已启动服务：无。

- 数据库/运行态变更：无。
- 文档检查：四份变更文档通过 `git diff --no-index --check`，无空白错误。
- 协议检查：Ruby `JSON.parse` 成功解析 `test-spec.md` 中 3 个 JSON 协议样例；Ruby 输出一条系统目录权限 warning，不影响解析结论。
- 状态检查：Spec 为 `apply`、HARD-GATE 为 `completed`、Tasks 为 `Apply/M1`；未发现仍要求“仅允许 Proposal”或“待用户确认”的门禁文本。
- 已知非阻断：Spec Research 与任务前置中出现的 `TODO` 是对旧 `BusinessTriggerExecutor` 未实现 Webhook 的现状描述，不是本变更占位实现；新节点必须把该能力标为不可用。
- 提交证据：`57fc3acb [application-business-process-orchestrator] 冻结编排协议与验证基线`；提交统计上传因本机 DNS/网络失败，Git 提交本身成功，未执行 push。

## 2026-08-03 Task 1：流程数据库结构与资源

- 新增 `V1.0.83__add_application_business_process.sql`：创建流程定义、不可变版本、运行实例和节点运行四张表；定义/版本使用 `BIGINT del_flag` 主键墓碑，运行表不提供删除标记。
- 新增 `V1.0.84__add_application_business_process_resources.sql`：写入设计/运行/节点/触发字典、应用发布 `PROCESSES` 步骤、隐藏设计器路由和管理/运行/迁移权限；权限只继承既有应用查看、编辑和发布角色，不扩大无应用权限角色。
- 幂等与租户：建表使用 `CREATE TABLE IF NOT EXISTS`；字典、资源和角色资源均使用 `NOT EXISTS`；内置数据统一 `tenant_id=1`。
- 静态验证：`git diff --check` 通过；两个新迁移的 Flyway placeholder 扫描无输出；`tenant_id DEFAULT 0/=0/,0` 扫描无输出；迁移版本重复扫描无输出。
- 轻量结构检查：`V1.0.83` 单引号 202、左右括号 60/60；`V1.0.84` 单引号 496、左右括号 31/31。
- 合同核对：Flowable 使用 `BusinessFlowService/FlowClient`；消息与企业协同使用 `BusinessActionStepExecutor + MessageService/CollaborationMessageChannel`；统一能力平台仅确认 `CapabilityRegistry`，generator 尚无受控桥接，Task 9B 前按不可用处理。
- 安全发现：旧 `SendMessageActionStepExecutor#resolveUserId` 在无 Session 时回退 `1L`，违反本变更“无合法普通用户失败关闭”；列入 Task 9B 修复，业务流程运行时不得复用该回退。
- 跳过项：未连接 MySQL，未执行新库/存量库/重复 Flyway 和 `forge_schema_history` 检查；原因是本轮不自动修改真实数据库，留待 Task 19 目标环境验收。
- 已启动服务：无。

## 2026-08-03 Task 2：流程定义持久层

- 新增 `AiBusinessProcess`：覆盖流程定义全部字段，`delFlag` 显式使用 `@TableLogic(value = "0", delval = "id")`。
- 新增 `BusinessProcessMapper/BusinessProcessMapper.xml`：分页和按 ID/编码查询同时限定 `tenant_id`、有效应用、有效应用对象关联、启用业务对象和 `del_flag=0`，共享对象不能绕过应用关联。
- 并发与删除：草稿保存要求当前 `draft_schema_hash` 命中客户端基线后才更新；逻辑删除原子写入当前行 `id` 并记录更新人。
- 新增 `BusinessProcessMapperContractTest` 3 项：覆盖主键墓碑、租户/应用/对象失败关闭和草稿 hash CAS。
- 首次命令：默认 Java 8 执行 Maven 失败，错误为 `无效的目标发行版: 17`，未进入源码编译；确认本机已有 Homebrew JDK 17 后仅对验证命令临时切换。
- 成功命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessMapperContractTest test`。
- 结果：主代码编译成功；测试 `3/3` 通过。现有 `BusinessFlowService` deprecation 与 `BusinessObjectDesignerService` unchecked 编译提示未新增失败。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-03 Task 3：流程版本持久层

- 新增 `AiBusinessProcessVersion`：完整承载应用版本、流程版本、规范化协议、依赖快照与发布审计，`delFlag` 使用主键墓碑逻辑删除。
- 新增 `BusinessProcessVersionMapper/BusinessProcessVersionMapper.xml`：提供固定版本、版本 ID、版本列表、应用选定流程集合和最大版本号查询；全部显式限定租户和未删除记录，正式版本读取额外限定 `status=1`。
- 不可变合同：只新增 `insertImmutable`，XML 不存在 `<update>` 或 `UPDATE ai_business_process_version`；空 `processIds` 集合使用 `AND 1 = 0` 失败关闭，避免误查全应用版本。
- 成功命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessMapperContractTest test`。
- 结果：主代码编译成功；累计 Mapper 契约测试 `4/4` 通过。仅保留 Task 2 已记录的既有 deprecation/unchecked 编译提示。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-03 Task 4：流程运行与节点运行持久层

- 新增 `AiBusinessProcessRun/AiBusinessProcessNodeRun`：运行记录固定应用、流程版本、业务对象/记录、可信 actor 与组织；节点记录按 `runId + nodeId + attemptNo` 新增尝试，不声明普通删除字段。
- 新增 `BusinessProcessRunMapper/BusinessProcessRunMapper.xml`：提供运行 ID、幂等键、Flowable 实例等待关联和租户内恢复扫描；恢复范围区分 PENDING、超时 RUNNING/WAITING 和到期 FAILED。
- 流程强 CAS：更新同时匹配 `tenantId + runId + expectedStatus + expectedCurrentNodeId + expectedProcessInstanceId`；终态记录结束时间，失败重试仅允许 `FAILED -> PENDING` 且原子增加次数。
- 新增 `BusinessProcessNodeRunMapper/BusinessProcessNodeRunMapper.xml`：插入尝试强制 PENDING，认领只允许 PENDING，完成/等待/回调消费同时匹配旧状态和 correlation；失败尝试不提供复活 SQL。
- XML 检查：`xmllint --noout BusinessProcessRunMapper.xml BusinessProcessNodeRunMapper.xml` 通过；目标文件 `git diff --check` 通过。
- 成功命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessMapperContractTest test`。
- 结果：主代码编译成功；累计 Mapper 契约测试 `6/6` 通过。仅保留已记录的既有 deprecation/unchecked 编译提示。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-03 Task 5：运行查询与安全摘要

- 新增 `BusinessProcessRunQueryDTO`：支持应用、流程、业务对象、记录、状态、触发来源和创建时间区间过滤。
- 新增 `BusinessProcessRunVO/BusinessProcessRunDetailVO`：流程、版本、actor、组织和节点运行 ID 均声明为字符串；详情时间线只暴露 correlation、安全输入/输出摘要、错误码和截断错误摘要。
- 运行分页 SQL：显式限定 `r.tenant_id`，使用 `CAST(... AS CHAR)` 返回所有长整型 ID，不读取 `context_snapshot/source_event_id/idempotency_key`。
- 节点查询：时间线使用不含幂等键的 `Timeline_Columns`；最后尝试保留内部幂等恢复字段；可重试与审批 correlation 查询同时限定租户和 run，查询顺序稳定。
- XML 检查：`xmllint --noout BusinessProcessRunMapper.xml BusinessProcessNodeRunMapper.xml` 通过；目标文件 `git diff --check` 通过。
- 成功命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessMapperContractTest test`。
- 结果：主代码编译成功；累计 Mapper 契约测试 `8/8` 通过，新增验证安全列和字符串 ID。仅保留已记录的既有编译提示。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-03 Task 6：businessProcessJson 协议与发布校验

- 新增强类型协议：`BusinessProcessSchema/BusinessProcessNode/BusinessProcessEdge` 分离根协议、主对象、节点、连线、策略、依赖和迁移元数据，不复用 BPMN/flowJson。
- 新增 `BusinessProcessSchemaValidator`：严格拒绝重复键、未知根字段和数字 ID；按节点/边/端口及依赖排序生成 canonical JSON 和 SHA-256；保留条件分支与重试退避等有序语义。
- 图门禁：单开始、节点注册表、固定/条件/审批出口、悬空边、自环、重复出口、DAG、开始可达、结束可达、节点/边数量和子流程深度全部失败关闭。
- 节点与依赖门禁：校验事件、定时普通用户引用、审批固定版本与四结果出口、记录动作、消息、业务动作、能力桥接、同应用已发布子流程、直接/间接递归、对象与字段有效性。
- 安全门禁：大小写及嵌套路径扫描 URL/Webhook/Secret/Token/Password/PrivateKey/Authorization/Cookie/JavaClass/SQL/Script/SpEL；自由 URL/JDBC 地址和画布 actor userId 覆盖失败关闭，问题响应不回显配置值。
- 冻结样例修正：定时提醒样例原有未连线 `end_failed`，与不可达节点门禁冲突，已从 `test-spec.md` 和测试资源中移除；新增手动审批、事件审批、定时提醒三份 classpath 回归资源。
- 新增 `BusinessProcessValidationVO/BusinessProcessValidationContext` 与 `BusinessProcessSchemaValidatorTest` 10 项，覆盖稳定 hash、三份冻结样例、重复键/数字 ID、多开始、环、悬空边、未知节点、无结束路径、失效字段、敏感键、自由 URL、递归子流程和能力桥接未就绪。
- 中间失败 1：新增“未知节点/无结束节点”用例首次用字符串替换构造 fixture，未实际移除结束节点，导致 1 项断言失败；改为解析后按节点/边 ID 构造无结束图，重跑通过，生产代码无回退。
- 中间失败 2：仅关闭 Jackson scalar coercion 仍会把数字 objectId 转为字符串，数字 ID 拒绝用例失败；增加原始 JsonNode 递归 ID 类型检查，并保留校验阶段二次保护，重跑通过。
- 成功命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessSchemaValidatorTest,BusinessProcessMapperContractTest test`。
- 结果：主代码编译成功；本轮 `18/18` 测试通过（Schema 10、Mapper 8）。仅保留已记录的既有 deprecation/unchecked 编译提示。
- 知识沉淀：新增 `pitfalls.md #160`，明确画布样例必须通过真实图校验，不能以 JSON 语法解析代替合法性验证。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-03 Task 7：流程定义控制面 Service 与 API

- 新增 `BusinessProcessDTO/BusinessProcessSchemaDTO/BusinessProcessVO`、`BusinessProcessService` 和 `BusinessProcessController`：提供应用内分页、详情、创建、同应用复制、基础信息更新、草稿 hash CAS、校验、启停和逻辑删除；Controller 使用独立 `/ai/business/process` 命名空间、加解密与细粒度权限。
- 草稿语义：新流程初始化为规范化“手动开始 → 成功结束”；所有 JSON/前端雪花 ID 保持字符串；流程编码创建后不可修改；结构不完整草稿可保存并保持 `DRAFT`，跨应用对象、编码不一致、Secret/自由 URL 等高风险错误禁止保存。
- 复制与删除：副本生成新编码并重建全部节点/边 ID，清空发布版本、运行状态和旧来源；存在任意 run 或有效发布版本时拒绝逻辑删除。
- 校验目录：使用当前应用对象/字段、不可变对象发布快照中的动作、表单/消息、同应用已发布子流程和真实 `sys_resource` 权限目录；Flowable 模型必须同时属于当前应用对象绑定且 `status=1/deploymentId` 有效，流程服务不可用时失败关闭。
- 权限补丁：新增 `V1.0.85__add_business_process_start_permission.sql`，不修改已提交 `V1.0.84`；注册 `ai:businessProcess:start`，仅从既有 `ai:businessApplication:runtime` 角色继承通用 API 门禁，正式运行仍需发布快照动作权限、可见条件、记录状态和数据权限二次校验。
- Mapper 扩展：基础信息/状态/设计状态更新、Schema CAS 同步主对象、run 引用计数、有效发布引用计数和当前已发布子流程查询全部写在 XML；流程列表不返回完整草稿正文。
- 定向测试命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessSchemaValidatorTest,BusinessProcessMapperContractTest,BusinessProcessServiceTest,BusinessProcessControllerTest,BusinessProcessValidationContextResolverTest test`。
- 定向测试结果：`31/31` 通过（Schema 10、Mapper 9、Service 8、Controller 3、Context Resolver 1），Failures/Errors/Skipped 均为 0。
- 静态检查：`xmllint --noout` 校验三份变更 Mapper XML 通过；`V1.0.85` Flyway placeholder 和 `tenant_id=0` 扫描无输出，`tenant_id=1/NOT EXISTS/ai:businessProcess:start` 命中预期；目标文件 `git diff --check` 通过。
- 聚合编译命令：`JAVA_HOME=<JDK17> PATH=<JDK17/bin:...> mvn -pl forge-admin-server -am compile -DskipTests`。
- 聚合编译结果：47/47 模块 `BUILD SUCCESS`，generator 与 admin 装配链路通过；仅有既有 deprecation、unchecked 和 Lombok `@Builder` warning，无新增阻断。
- 跳过项：未执行真实 MySQL/Flyway、权限继承数据查询、加密 HTTP API、Flowable 已发布/未发布模型联调和浏览器验证；原因是本轮遵循用户偏好不启动真实服务、不改数据库或 Flowable 运行态，留待 Task 19 环境门禁。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-03 Task 14：前端独立协议与画布基础

- TDD 红灯：首次执行 `pnpm exec vitest run src/components/business-process-designer/__tests__/business-process-designer.spec.js`，测试套件因 `BusinessProcessCanvas.vue` 等四个目标模块不存在而按预期失败，未进入用例执行。
- 协议与注册表：新增 `business-process-schema.js/business-process-node-types.js`，提供默认草稿、严格字符串 ID、BPMN 输入拒绝、节点/边/端口/依赖排序、稳定 hash 输入、八类业务节点和审批四结果出口。
- 设计器状态：新增 `useBusinessProcessDesigner.js`，复用 `useFlowHistory`，支持线性节点插入/复制/删除、边 CRUD、条件双分支、选择、撤销重做、dirty 基线和深克隆导出；业务 DAG 中不生成伪审批节点。
- 画布复用：新增 `BusinessProcessCanvas.vue`，用临时只读布局适配把业务类型映射给 `layoutFlow`，持久化协议不增加 `nodeType/bpmnElementId`；直接复用既有 `FlowCanvas/EdgeLayer` 插槽，无需修改共享画布。
- BPMN 隔离：`convertJsonToBpmn` 在转换前显式识别并拒绝 `businessProcessJson`，审批 `flowJson` 的原转换路径保持不变。
- 定向测试：业务画布测试 `10/10` 通过；组合执行业务画布、BPMN roundtrip、JSON→BPMN、FlowCanvas、layout-engine 共 5 个测试文件，结果 `48/48` 通过。
- 中间操作纠正：首次组合回归误在仓库根目录执行，pnpm 报 `Command "vitest" not found`，未执行任何测试或产生代码影响；切换到 `forge-admin-ui` 后按同一命令通过。
- 静态检查：`pnpm exec eslint src/components/business-process-designer src/components/flow-designer/converter/json-to-bpmn.js` 通过；目标文件 `git diff --check` 通过。
- 生产构建：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，Vite 转换 `8825` 个模块并 `BUILD SUCCESS`（`built in 1m 50s`）。保留仓库既有的组件命名冲突、动态/静态 import、CSS `//` 注释警告，未新增阻断。
- 跳过项：未启动 Vite/浏览器，原因是 Task 14 仅交付协议和画布基础，尚无应用路由与完整节点配置；浏览器交互留待 Task 15-16。未启动 Admin/Flow 服务，未执行 Flyway、数据库或 Flowable 运行态变更。
- 已启动服务：无。

## 2026-08-03 Task 15：节点配置、真实审批设计器与草稿交互

- TDD 红灯：新增 `business-process-designer-workbench.spec.js` 后首次运行因五个目标组件不存在而在模块解析阶段失败，符合先冻结工作台交互合同再实现的预期。
- 工作台：新增 `BusinessProcessDesigner.vue`，提供紧凑工具栏、节点面板、共享画布、问题列表、节点复制/删除、检查、自动/显式保存状态、服务端冲突提示与刷新入口；dirty 草稿注册 `beforeunload` 保护。
- 节点配置：新增 `BusinessProcessNodeRenderer/BusinessProcessNodeConfigDrawer/StartNodeConfig/ActionAndApprovalNodeConfig`，覆盖触发方式、事件、定时服务账号、结构化条件、记录动作、消息、业务动作、能力、审批、子流程和结束结果，不暴露高级文本协议或自由外部目标。
- 审批衔接：审批节点只选择已发布/已部署 Flowable 模型，并在全屏弹层异步复用真实 `flow/design.vue`；会签、驳回、退回、审批人和字段权限继续由 BPMN 所有，关闭/保存/部署后发出模型刷新事件。
- 协议联动：新增 `synchronizeBusinessProcessDependencies` 与开始类型切换合同；节点引用变更自动重建七类受治理依赖，手动/事件/定时切换同步 `recordIdSource`，动作新增默认绑定当前主对象。
- 定向测试：两份业务设计器测试共 `17/17` 通过（协议/历史 11、工作台交互 6）；工作台测试覆盖审批四出口、真实设计器入口、结构化触发切换、面板路由、依赖同步、自动保存、hash 冲突和离开保护。
- 组合回归：业务设计器、BPMN roundtrip、JSON→BPMN、FlowCanvas 和 layout-engine 共 6 个测试文件，结果 `55/55` 通过；未发布/未部署审批模型不会进入可选目录。
- 静态检查：`pnpm exec eslint src/components/business-process-designer` 通过；目标目录 `git diff --check` 通过。
- 生产构建：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，Vite 转换 `8825` 个模块并成功构建（`built in 1m 46s`）；真实 `flow/design.vue` 异步依赖、Naive UI 抽屉和弹窗均通过装配。
- 既有警告：仍为组件命名冲突、动态/静态 import 和 CSS `//` 注释等仓库存量构建警告，本任务未新增阻断。
- 跳过项：未启动 Vite/浏览器，原因是 Task 15 独立组件尚未接入可访问路由；浏览器与路由离开验收将在 Task 16 完成应用工作台/全屏设计页后执行。未启动 Admin/Flow 服务，未执行 Flyway、数据库或 Flowable 运行态变更。
- 已启动服务：无。

## 2026-08-03 Task 16：应用工作台业务流程核心面板

- TDD 红灯：新增 Task 16 工作台测试后首次运行因 `ApplicationProcessPanel.vue`、`business-process.[processId].vue` 和 `api/business-process.js` 不存在而在模块解析阶段失败，符合先冻结接口、列表和设计页交互合同再实现的预期。
- 控制面 API：新增 `api/business-process.js`，分页、详情、创建、复制、更新、设计草稿、Schema CAS、校验、启停和逻辑删除均显式使用加密请求；保存 payload 使用服务端返回的 64 位 `draftSchemaHash`，不把前端 dirty-check hash 当并发基线。
- 应用工作台：新增克制的流程列表，支持搜索、状态筛选、分页、新建、复制、设计、启停、逻辑删除和进入应用发布；新建流程只选择当前应用对象，流程编码由服务端生成。旧“业务流程/触发器/动作”三按钮组件不再作为应用工作台主入口。
- 路由与返回：`automation` 分区文案改为“业务流程 / 触发、审批与自动化”，新增 `/app-center/business-process/:processId` 全屏路由；筛选同步到 route query，`returnTo` 返回原应用和筛选状态，且只接受本地路径。
- 全屏设计页：加载真实 designer 草稿和对象字段、对象动作、Flowable 模型、表单、消息模板、同应用已发布子流程目录；所有 ID 归一为字符串。关闭内嵌 Flowable 设计器后刷新模型和表单目录；受治理能力与服务账号目录未交付时保持空目录并失败关闭。
- 草稿可靠性：保存期间继续编辑会排队再次 CAS 保存；脏草稿执行检查前先保存；HTTP 409 显示冲突并禁止覆盖；浏览器刷新和路由离开分别由 `beforeunload` 与 `onBeforeRouteLeave` 保护。
- 未交付边界：运行记录和迁移预览在面板中显示“待接入”禁用态，未创建 Task 13/17 尚不存在的 API。
- 定向与回归测试：Node `v20.19.0` 下执行 Task 16 API/工作台测试与业务流程设计器、BPMN roundtrip、JSON→BPMN、FlowCanvas、layout-engine 共 8 个测试文件，结果 `62/62` 通过。
- 静态检查：Task 16 API、页面、路由、测试及 `src/components/business-process-designer` 目标 ESLint 通过；目标文件空白检查通过。
- 浏览器验证：用 `webapp-testing` Playwright 脚本临时启动 Vite `127.0.0.1:3017`，通过 34 次受控请求装配真实应用工作台和全屏设计路由；验证旧三入口消失、筛选保留、节点新增、CAS 保存使用 `aaaaaaaa...` 服务端 hash、服务端校验和未保存离开取消。workspace/designer 截图保存于 `/tmp/forge-task16-workspace.png`、`/tmp/forge-task16-designer.png`，console error 与 page error 均为 0；脚本结束后 Vite 已停止。
- 生产构建：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，Vite 转换 `8845` 个模块并成功构建（`built in 1m 58s`）；保留仓库既有组件命名冲突、动态/静态 import 和 CSS `//` 注释警告，无新增阻断。
- 跳过项：未启动 Admin/Flow，未执行真实加密 HTTP、权限资源查询、MySQL/Flyway、Flowable 模型保存/部署或运行态变更；这些仍由 Task 19 目标环境验收。
- 已启动服务：浏览器验证仅临时启动 Vite，已由脚本停止；未遗留 `3017` 监听进程。

## 2026-08-04 Task 12：应用发布流程版本和依赖快照

- TDD 红灯：先新增 `BusinessProcessPublishServiceTest` 和应用发布步骤断言；首次执行 `mvn -Penable-tests ... -Dtest=BusinessProcessPublishServiceTest,BusinessApplicationPhaseFiveSecurityTest` 在测试编译阶段因 `BusinessProcessPublishService` 尚不存在而失败，符合先冻结版本合同再实现的预期。
- 发布选择与候选冻结：`BusinessApplicationPublishDTO/AssetSelectionVO` 增加流程 ID；空选择默认包含应用内全部启用流程，`includeAutomation=false` 时明确为空。候选快照保存流程白名单信息、完整结构化协议和 `draftSchemaHash`，恢复不得读取后来修改的新草稿。
- 不可变版本：新增 `BusinessProcessPublishService/BusinessProcessPublishResult/BusinessProcessSnapshot`；发布前锁定流程定义，以 `(tenant_id, process_id, application_version, del_flag)` 幂等复用，hash 不同返回 409；新版本只执行 `insertImmutable`，无版本 UPDATE SQL。
- 依赖固定：流程版本依赖快照固定对象设计版本；Flowable 必须同时具备 `status=1`、正版本号、`processDefinitionId` 和 `deploymentId`，并保存模型 ID、版本与部署标识；表单、业务动作、消息模板、能力和子流程只保存稳定白名单引用。
- 应用发布与恢复：固定步骤变为 `PRECHECK → SNAPSHOT → PROCESSES → OBJECTS → ENTRIES → PAGE_MENUS → EXTENSIONS → COMMIT`；`PROCESSES` 被计入部分成功副作用，步骤成功后把结构化 `publishedProcessVersions` 写回运行单快照。`runtimeActions` 仅冻结空字段，Task 13 再编译手动动作。
- 回滚边界：来源快照中的历史 `processVersionId` 只恢复流程定义的 `published_version/design_status` 投影，并清理未选择投影；不读取或更新运行表，因此已开始实例继续固定自己的流程版本。
- 就绪检查：正式流程来源切换为 `businessProcessJson`；复用 Schema/图/依赖校验阻断对象版本、字段、审批部署版本、结束路径、子流程递归、单活动审批策略和手动权限问题。旧 binding 仅作为兼容快照保留，不再决定是否存在应用级流程。
- 定向验证命令：`JAVA_HOME=<JDK17> mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessPublishServiceTest,BusinessApplicationPhaseFiveSecurityTest,BusinessApplicationAssetSelectionServiceTest,BusinessApplicationReadinessServiceTest,BusinessProcessMapperContractTest,BusinessProcessSchemaValidatorTest,BusinessProcessValidationContextResolverTest,BusinessProcessServiceTest,BusinessApplicationPublishRunServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- 边界复核红灯：补充“候选缺少流程 hash”和“Flowable 版本号为 0”回归后，相关两类测试共 `7` 项首次运行出现 `3` 个预期失败，确认协调发布仍会回退当前草稿且零版本会被校验/发布接受。
- 边界修正：协调发布存在 `publishRunId` 时强制要求每个所选流程都有候选 hash；Flowable 模型在校验目录和依赖固定阶段均要求版本号 `> 0`。相关两类测试重跑 `7/7` 通过。
- 定向验证结果：Task 12 九类测试最终 `46/46` 通过，Failures/Errors/Skipped 均为 0；覆盖幂等重试、hash 冲突、候选 hash 缺失失败关闭、Flowable 非正版本拒绝、对象/Flowable 依赖固定、历史投影恢复、默认流程选择、快照字段和就绪阻断。
- Mapper 静态检查：`xmllint --noout BusinessProcessMapper.xml BusinessProcessVersionMapper.xml` 通过；目标差异 `git diff --check` 通过。
- 聚合编译：边界修正后重跑 `JAVA_HOME=<JDK17> mvn -pl forge-admin-server -am -DskipTests compile`，47/47 模块 `BUILD SUCCESS`；仅保留仓库既有 deprecation、unchecked 和 Lombok `@Builder` warning。
- generator 全量基线（边界修正前）：执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator test`，共 561 项，555 项通过，2 failures + 4 errors。失败为未被 Task 12 修改的 `FormulaExecutionEngineLookupTest`、`FormulaValueMaskerTest`、`BusinessBindingApplicationTargetTest`、`BusinessExtensionVersionServiceTest`（2 项）和 `LowcodeRuntimeConfigBuilderTest`；Task 12 定向类全部通过，本轮未越界修改这些存量失败。边界修正后未重复消耗时间执行同一已知失败全量，改以覆盖修改类的 `46/46` 定向测试和 47 模块聚合编译闭环。
- 跳过项：未启动 Admin/Flow，未执行 Flyway、真实数据库发布/回滚、加密 HTTP 或 Flowable 部署联调；本任务没有新增数据库脚本，真实新旧实例并存验收留待 Task 19。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-07 业务画布审批结果连线错乱修复

- 问题复现：默认草稿插入审批节点后，四条审批结果边都指向原 `end_success`。共享布局器把同一个结束节点当成四个独立分支头，首次回归得到结束节点 `x=-338`、主轴 `x=220`，与用户截图中的左移结束节点和长水平折线一致。
- TDD 红灯：新增“共享审批后继保持居中且结果路由可区分”用例；首次执行业务画布测试为 `13 passed / 1 failed`，失败断言为 `expected -338 to be 220`，确认测试覆盖真实根因。
- 布局修复：`BusinessProcessCanvas` 只读适配层按入度为业务 DAG 节点补充 `mergeNode` 布局提示，不写回持久化协议；多条同源同目标结果边按来源端口顺序使用独立起止锚点和插入位置。`BusinessProcessNodeRenderer` 将可见端口改为等宽栅格，使标签、连线和添加按钮保持同序对齐。
- 定向测试：Node `v20.19.0` 下单文件回归 `14/14` 通过；组合执行业务设计器、工作台、BPMN roundtrip、JSON→BPMN、layout-algorithm、FlowCanvas 和 layout-engine 共 7 个测试文件，结果 `68/68` 通过，Failures/Errors/Skipped 均为 0。
- 静态检查：三份目标文件 ESLint 通过；目标差异 `git diff --check` 通过。
- 浏览器证据：按 `webapp-testing` 流程临时启动受控 Vite `127.0.0.1:3018` 并拦截控制面 API，真实路由渲染 3 个节点和 5 条边；开始/审批/结束屏幕 x 坐标均为 `1121.34375`，四个结果插入目标 x 分别为 `1162.9/1220.5/1278.1/1335.7`，四条结果 SVG 路径唯一，页面脚本错误为 0；截图保存于 `/tmp/forge-business-process-canvas-fixed.png`。
- 浏览器环境告警：首轮通过时有 1 条未定位资源的 `net::ERR_CONNECTION_REFUSED` 控制台提示；后续复跑遇到开发态 UnoCSS 工具类未及时生成，等候 `position:absolute` 超时。该告警未出现在组件测试或生产构建，未将复跑标记为通过；所有临时 `3018` Vite/preview 进程均已由脚本停止。
- 生产构建：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，Vite 转换 `8874` 个模块并成功构建（`built in 1m 54s`）。保留仓库既有组件命名冲突、动态/静态 import 和 CSS `//` 注释警告，无新增阻断。
- 跳过项：未启动 Admin/Flow，未调用真实加密 HTTP、未读取或保存用户真实草稿，未执行 MySQL/Flyway 或改变 Flowable 运行态。
- 已启动服务：仅浏览器验证临时启动 Vite/preview，均已停止；数据库/Flowable 运行态变更：无。

## 2026-08-07 Flowable BPMN XML 解析兼容性修复

- 根因：`BpmnXmlUtils` 直接向当前 JAXP `DocumentBuilderFactory`/`TransformerFactory` 设置 `ACCESS_EXTERNAL_*` 属性；旧 parser/provider 不识别 `http://javax.xml.XMLConstants/property/accessExternalDTD`，导致部署在 XML 归一化前失败。
- 修复：将外部访问属性改为兼容性设置；保留 `disallow-doctype-decl`、外部实体禁用和不展开实体的强制安全配置。`process id` 已匹配模型 Key 时降为 debug 日志。
- 回归测试：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Dtest=BpmnXmlUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- 结果：`3/3` 通过（重复连线归一化、条件连线保留、DOCTYPE 拒绝）；Flow 模块生产编译/打包成功。测试输出包含 XML parser 对预期 DOCTYPE 拒绝的标准 Fatal Error 日志。
- 环境限制：未启动 Admin/Flow，未访问真实数据库或 Flowable 运行态；上游 reactor 测试编译仍存在既有 `MessageServiceImplTest` 构造器参数不匹配，未修改无关测试。
- 已启动服务：无。
- 发布校验回归：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessValidationContextResolverTest,BusinessProcessSchemaValidatorTest,BusinessApplicationReadinessServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- 结果：`15/15` 通过，确认 Flowable 模型仍需“当前应用对象绑定 + 已发布部署版本”；本轮未删除归属门禁。

## 2026-08-07 Task 20：业务流程设计器可用性修复

- 审批目录与发布门禁：新增 `GET /ai/business/process/{id}/flow-models`，只返回当前业务流程所属应用内、启用对象存在有效 `FLOW` Binding 且 Flowable `status=1`、`version>0`、`processDefinitionId/deploymentId` 完整的模型；前端选择目录与 `BusinessProcessValidationContextResolver` 发布校验复用同一目录判定，未放宽跨应用、未发布或失效引用门禁。
- 条件配置：从原 Flowable `ConditionConfig` 抽取共享 `condition-expression.js`，业务流程条件抽屉复用 AND/OR、比较、区间、包含、空值、表达式生成和旧表达式反解析；普通用户只编辑中文业务字段、判断关系和值，不展示 SpEL 或技术端口。
- 图协议修复：条件 branches、节点 ports 和 outgoing edges 在一次更新中原子同步；新增、改名、删除、默认分支都会同步真实 edge。协议规范化保留节点 port 的业务顺序，避免按字母排序后把“条件 2”排到“条件 1”之前。
- 布局与删除：多入边后继按汇合点布局，同源同目标的条件/审批结果边使用独立锚点、路径和插入按钮；条件和审批多出口均指向唯一公共后继时允许从卡片右上角红色按钮删除并恢复 DAG，尚未汇合时失败关闭并给中文原因。
- 中文化：普通页面统一显示“审批通过、审批驳回、审批取消、执行失败、条件 N、其他情况”，不显示 `APPROVED/MATCHED/OTHERWISE/BRANCH_*`；问题列表也改为中文节点位置。
- 前端定向回归：Node `v20.19.0` 下组合执行 `business-process-designer.spec.js`、`business-process-designer-workbench.spec.js`、`business-process-workspace.spec.js`、`ConditionConfig.spec.js` 和 `bpmn-to-json-condition.spec.js`，结果 `5 files / 50 tests passed`，Failures/Errors/Skipped 均为 0。
- 前端静态与构建：目标 ESLint 无输出通过；最终执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，Vite 转换 `8877` 个模块并成功构建（`built in 1m 39s`）。保留仓库既有 `UserSelectModal` 命名冲突、动态/静态 import 和 CSS `//` 注释告警，无新增构建阻断。
- 后端定向回归：JDK 17 下执行 `BusinessProcessControllerTest,BusinessProcessSchemaValidatorTest,BusinessProcessValidationContextResolverTest,BusinessProcessServiceTest,BusinessProcessPublishServiceTest,BusinessApplicationReadinessServiceTest`，结果 `34/34` 通过，Failures/Errors/Skipped 均为 0，`BUILD SUCCESS`。
- Flowable XML 回归：执行 `BpmnXmlUtilsTest`，结果 `3/3` 通过；输出中的 DOCTYPE Fatal Error 是拒绝恶意 DOCTYPE 用例的预期日志。Generator 独立 `compile` 同步通过。
- 静态校验：`xmllint --noout BusinessBindingMapper.xml` 和 `git diff --check` 均无输出通过。
- 浏览器验证：先执行 `with_server.py --help`，再用受控 API 临时启动 Vite `127.0.0.1:4173` 装配真实全屏路由。验证当前应用“请假审批”模型可回显；新增条件、添加第三分支并配置“请假天数”两条规则后，页面显示“三天及以上 / 三天以内 / 其他情况”；8 条真实 SVG 连线全部唯一，8 个插入按钮最小中心距 `57.6px`；卡片删除后恢复为 5 条边；条件配置和节点删除分别触发草稿保存。console error、page error、request failure 均为 0。
- 浏览器截图：`artifacts/business-process-condition-configured.png`、`artifacts/business-process-after-card-delete.png`；人工检查确认节点保持主轴、分支线无折返/重叠、中文标签与锚点同序、删除按钮清晰可用。
- 已知无关基线：此前带 `-am` 的聚合测试在 `forge-plugin-message/src/test/.../MessageServiceImplTest.java` 测试编译失败，原因是既有测试构造器缺少新增 `ApplicationEventPublisher` 参数；本轮未越界修改消息模块，以 Generator 独立 34 项定向测试和编译闭环。
- 环境门禁：未启动真实 Admin/Flow，未访问或修改 MySQL/Flyway/Flowable 运行态，也未写入用户真实流程草稿；真实应用发布和审批实例运行仍由 Task 19 目标环境联调，不能用受控浏览器结果替代。
- 已启动服务：浏览器验证仅临时启动 Vite `4173`，已由 `with_server.py` 停止；无遗留服务进程。

## 2026-08-07 Task 20 审批目录与跨层连线最终纠偏

- 历史口径说明：上一条 Task 20 记录保留当时错误的对象 `FLOW Binding` 实现和测试证据，不能覆盖为已正确；本条记录其后续纠偏结果，当前实现和 Spec 第 14 节以本条为准。
- 审批目录：应用业务流程仍先按 `processId -> applicationId` 校验当前应用访问边界，但可选审批模型改为当前可信租户的 Flowable 已发布资产，不再要求业务对象配置旧 `FLOW Binding`。目录与发布校验统一要求 `status=1`、`version>0`、`processDefinitionId/deploymentId` 完整，并排除业务编排模型。
- 租户边界：Flow 模型列表改为 Mapper XML 显式接收 `SessionHelper.getTenantId()`；缺少可信租户上下文时返回空目录且不访问 Mapper。未使用默认租户或跨租户忽略查询。
- 条件发布门禁：只有默认分支的条件节点不再被当作有效流程；服务端要求至少一个判断分支和一个唯一默认分支，默认分支不得配置判断规则，普通页面继续只显示结构化中文条件。
- 布局最终修复：业务画布改用独立 `business-process-layout.js`。除共享后继和分支顺序外，路由会检测跨层边是否穿越中间卡片，命中时使用卡片外侧独立通道；绕行线与其它分支汇合线不共用线段，条件分支下游卡片顺序与配置顺序一致。
- Generator 定向测试：执行 `JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessControllerTest,BusinessProcessSchemaValidatorTest,BusinessProcessValidationContextResolverTest,BusinessProcessServiceTest,BusinessProcessPublishServiceTest,BusinessApplicationReadinessServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`，结果 `36/36` 通过，Failures/Errors/Skipped 均为 0。
- Flow 定向测试：执行 `JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Dtest=BpmnXmlUtilsTest,FlowModelServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`，结果 `5/5` 通过；DOCTYPE Fatal Error 仍是安全拒绝用例的预期输出。
- Flow Client 编译：执行 `JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn -pl forge-flow/forge-flow-client -am -DskipTests compile`，响应泛型修改编译成功。
- 前端组合回归：Node `v20.19.0` 下组合执行 `business-process-designer.spec.js`、`business-process-designer-workbench.spec.js`、`business-process-workspace.spec.js`、`ConditionConfig.spec.js` 和 `bpmn-to-json-condition.spec.js`，结果 `5 files / 54 tests passed`，Failures/Errors/Skipped 均为 0。
- 静态检查：目标 ESLint 无输出通过；`xmllint --noout` 校验 `FlowModelMapper.xml` 和业务绑定 Mapper XML 通过；`git diff --check` 通过。
- 生产构建：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，Vite 转换 `8878` 个模块并成功构建（`built in 2m 8s`）。仅保留仓库既有组件命名冲突、动态/静态 import 和 CSS `//` 注释告警。
- 环境门禁：未启动真实 Admin/Flow，未执行 Flyway、真实加密 HTTP、应用发布或审批实例运行，也未改变 MySQL/Flowable 运行态；真实“选择审批模型 -> 保存 -> 发布 -> 发起审批”仍需 Task 19 目标环境联调。
- 已启动服务：无；数据库/Flowable 运行态变更：无。

## 2026-08-23 审批终态恢复 rollback-only 修复

- 根因：`BusinessProcessApprovalResultListener` 原为同步事件监听器，审批终态恢复加入 `BusinessFlowService.handleFlowEngineEvent` 当前事务；动作节点的动态 CRUD 异常虽被编排器转换成失败结果，但共享事务已被下游拦截器标记 rollback-only，最终提交只抛 `UnexpectedRollbackException`，节点失败状态也无法落库。
- 修复：流程引擎回调建立本地事务；审批结果监听改为 `AFTER_COMMIT + fallbackExecution`；业务流程恢复和动作执行分别使用 `REQUIRES_NEW`。动作失败只回滚动作写入，外层恢复事务仍可提交 `FAILED/errorSummary`，不再被下游事务标记为 rollback-only。
- 容错：审批终态已经提交后，恢复异常记录包含租户、流程实例、结果和完整原始堆栈，不再反向抛给 FlowCallback；监听前后租户上下文正确恢复。
- TDD 红灯：新增监听器事务合同与异常隔离测试，首次执行 2 项均失败，分别确认原监听仍为同步 `@EventListener` 且异常会向外传播。
- 定向验证：JDK 17 执行 `mvn -o -Penable-tests -Dtest=BusinessProcessApprovalResultListenerTest,BusinessProcessOrchestratorTest,BusinessProcessActionExecutorTest test -q`，3 个测试类共 14 项通过，Failures/Errors/Skipped 均为 0；预期异常隔离用例输出一条完整 ERROR 堆栈作为日志合同证据。
- 静态检查：本轮相关 Java 文件 `git diff --check` 通过。
- 跳过项：按用户偏好未执行全量 Maven/Vite 构建，未启动 Admin/Flow、未连接 MySQL/Redis，也未执行真实审批通过后的动态字段更新；本轮未启动任何服务。
