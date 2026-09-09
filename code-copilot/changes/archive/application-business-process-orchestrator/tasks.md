# 任务拆分 — 应用级业务流程编排器
> change: `application-business-process-orchestrator`
> 当前阶段：Apply/M1，先实施控制面与画布，不接管正式触发
> 拆分顺序：协议冻结 → 数据模型 → 控制面 → 持久化运行 → 节点执行 → Flowable 恢复 → 发布 → 前端 → 迁移收口
> 每个编码任务必须独立提交、同步文档并按增量差异验证

## 前置条件

- [x] 用户明确确认 `spec.md` 第 9 章全部默认决策并完成 HARD-GATE 记录。
- [x] 进入 `/apply` 前读取根 `AGENTS.md`、当前 `spec.md/tasks.md`、`code-copilot/memory/*` 和相关 Skill。
- [x] 创建 `test-spec.md` 与 `execution-log.md`，并读取 `code-copilot/rules/automated-testing-standard.md` 形成增量验证基线。
- [x] 实施前重新扫描 `forge-server/db/migration`；当前最新为并行变更中的 `V1.0.82`，本变更使用连续未占用的 `V1.0.83/V1.0.84`，Task 7 以新增 `V1.0.85` 补齐运行时开始权限，不修改已提交脚本或并行 `V1.0.82`。
- [x] 已确认节点调用合同：审批复用 `BusinessFlowService/FlowClient`，消息与企业协同复用 `BusinessActionStepExecutor + MessageService/CollaborationMessageChannel`；统一能力平台提供 `CapabilityRegistry`，但 generator 尚无受控桥接，完成 Task 9B 前节点必须标为不可用；不提供自由 URL。
- [ ] 确认采购审批样例和目标测试应用的当前发布版本、旧触发器、FLOW Binding、业务动作及运行中 Flowable 实例基线。
- [x] 所有 19 位 ID 在前端和 JSON 中按字符串处理；所有业务数据继续使用 `businessKey=<objectCode>:<recordId>`。
- [x] 已在 `test-spec.md` 完成状态、审批权限、定时身份、外部调用、旧配置迁移和回调幂等专项审查；真实环境验收门禁继续保留。

## 里程碑

| 里程碑 | 范围 | 完成标志 |
|---|---|---|
| M1 控制面与画布 | Task 0-7、Task 14-16 | 应用可创建、复制、保存、校验业务流程草稿；节点配置和应用工作台入口完整，尚不接管正式触发 |
| M2 运行、审批与发布 | Task 8A-8B、Task 9A-9B、Task 10-13 | 事件/定时/手动触发可持久化执行，审批节点可等待和恢复，应用发布固定流程版本 |
| M3 迁移与收口 | Task 17-18 | 旧配置可预览/迁移，样例切换，旧普通入口停止写入且历史运行不受影响 |
| M4 验证与审查 | Task 19 | Spec 合规、自动化、构建、真实 Flowable E2E 和迁移演练完成 |

## 功能追踪

| Spec 功能 | 主要实现 Task | 验证 Task |
|---|---|---|
| 功能 1-4：应用流程资产、主对象、单开始节点与审批并发 | Task 1-4、Task 7、Task 11、Task 16 | Task 19 |
| 功能 5-8：独立协议、校验器与画布基础 | Task 0、Task 6、Task 14-15 | Task 19 |
| 功能 9-12：事件、业务语义、定时与手动开始 | Task 10、Task 13、Task 15 | Task 19 |
| 功能 13-18：条件、动作、消息、受治理能力、结束与子流程 | Task 6、Task 8A-9B、Task 10、Task 15 | Task 19 |
| 功能 19-23：Flowable 审批子流程、等待、恢复与状态修复 | Task 11、Task 15 | Task 19 |
| 功能 24-30：持久化运行、幂等、重试、身份与数据权限 | Task 1、Task 4-5、Task 8A-9B、Task 10-11、Task 13 | Task 19 |
| 功能 31-35：应用发布、版本、回滚、运行治理与就绪检查 | Task 3、Task 5、Task 12-13、Task 16 | Task 19 |
| 功能 36-40：迁移、旧入口收口与采购审批样例 | Task 17-18 | Task 19 |

建议实施顺序为 `Task 0 → Task 1-7 → Task 14-16 → Task 12 → Task 8A-8B → Task 9A-9B → Task 10-11 → Task 13 → Task 17-19`。其中 Task 12 必须先冻结应用发布快照和已发布版本解析合同，Task 10 才能把正式事件和定时触发路由到该快照；Task 编号表达领域拆分，不代表可以忽略这项依赖。

## 执行状态

- [x] Research：完成应用、对象、触发器、业务动作、Flowable、DingFlowDesigner 和应用发布现状调查。
- [x] Proposal：生成 `spec.md` 和本 `tasks.md`，未修改生产代码或数据库。
- [x] HARD-GATE：用户确认首版协议、审批并发、定时发起人、旧入口退出、子流程和 Webhook 边界。
- [ ] Apply/M1：控制面与画布（进行中）。
- [ ] Apply/M2：运行、审批和发布。
- [ ] Apply/M3：迁移与旧入口收口。
- [ ] Review/Test/UAT：自动化、构建、浏览器、数据库、Flowable 和迁移演练。

## Task 0：冻结协议、文件清单与测试基线

**状态：completed（2026-08-03）**。协议样例、身份矩阵、状态机、安全审查与验证矩阵冻结在 `test-spec.md`；基线证据记录在 `execution-log.md`。

- **目标**：在修改生产代码前冻结首版节点范围、Schema 示例、状态机、身份矩阵、迁移样例和测试命令。
- **涉及文件**：
  - `code-copilot/changes/application-business-process-orchestrator/spec.md` — 回填第 9、13 章确认记录。
  - `code-copilot/changes/application-business-process-orchestrator/tasks.md` — 回填任务状态和最终文件清单。
  - `code-copilot/changes/application-business-process-orchestrator/test-spec.md` — 新增协议、服务、迁移、前端和 E2E 测试矩阵。
  - `code-copilot/changes/application-business-process-orchestrator/execution-log.md` — 新增环境、基线、命令、结果和清理记录。
- **关键产物**：
  - `businessProcessJson` 完整示例：手动提交审批、记录新增自动化、定时分层提醒各一份。
  - 身份矩阵：`MANUAL/EVENT/SCHEDULE/PROCESS_CALLBACK/EXTERNAL` 对应 actor、tenant、activeOrg 和失败策略。
  - 状态机：process run、node run、approval wait 的合法状态转换和 CAS 条件。
- **验收**：不存在未决节点类型、自由 URL Webhook 或“实现时再定”的协议字段；测试 Spec 可追溯到 Spec 功能 1-40。

## Task 1：新增流程定义与版本数据库结构

**状态：implemented（2026-08-03）**。`V1.0.83/V1.0.84` 已实现并通过静态检查；MySQL 8 新库、存量库和重复 Flyway 实跑留待 Task 19 真实环境门禁。

- **目标**：创建应用级流程定义、不可变版本、运行实例、节点运行和权限资源表结构。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.83__add_application_business_process.sql` — 新增四张表、业务唯一索引、运行索引和审计字段。
  - `forge-server/db/migration/V1.0.84__add_application_business_process_resources.sql` — 新增字典、菜单/API 权限和 `PROCESSES` 发布步骤相关资源。
- **关键约束**：
  - `ai_business_process.del_flag`、`ai_business_process_version.del_flag` 使用 `BIGINT NOT NULL DEFAULT 0`，逻辑删除写当前主键。
  - 流程编码唯一索引覆盖 `tenant_id + application_id + process_code + del_flag`；非空简单旧来源增加 `tenant_id + legacy_source_type + legacy_source_id + del_flag` 唯一索引，合并来源由迁移服务锁定并校验全部来源。
  - `ai_business_process_run.subject_record_id` 使用字符串列，兼容不同业务主键且不损失雪花 ID 精度。
  - JSON 字段使用 `LONGTEXT` 并在服务端解析；索引只覆盖租户、应用、流程、状态、业务键、幂等键和时间等稳定列。
  - Flyway 使用 `information_schema`、`CREATE TABLE IF NOT EXISTS` 和 `INSERT ... WHERE NOT EXISTS` 防重复，内置数据 `tenant_id=1`。
- **验证**：MySQL 8 新库、存量库、重复执行和 `forge_schema_history` 检查；禁止在 SQL 中转换旧画布 JSON。

## Task 2：流程定义持久层

**状态：completed（2026-08-03）**。已实现租户/应用/主对象失败关闭查询、草稿 hash CAS 和主键墓碑逻辑删除，并通过 3 项 Mapper 契约测试。

- **目标**：建立流程定义实体、Mapper XML 和活动记录查询，所有查询显式限定租户与逻辑删除。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessProcess.java` — 新增流程定义实体和显式 `@TableLogic(value="0", delval="id")`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessProcessMapper.java` — 新增定义查询和 CAS 更新签名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessProcessMapper.xml` — 新增应用分页、编码查询、发布状态和逻辑删除 SQL。
- **关键签名**：
  ```java
  Page<AiBusinessProcess> selectPage(Long tenantId, Long applicationId,
                                        String keyword, String status, Page<?> page);
  AiBusinessProcess selectActiveByCode(Long tenantId, Long applicationId, String processCode);
  int updateDraftSchema(Long tenantId, Long id, String schemaJson, String schemaHash,
                        String designStatus, Long updateBy);
  int logicalDelete(Long tenantId, Long id, Long updateBy);
  ```
- **验收**：同应用有效编码唯一；跨租户、已删除、停用应用和共享对象误匹配用例失败关闭。

## Task 3：流程版本持久层

**状态：completed（2026-08-03）**。已实现不可变版本实体、固定版本/应用版本读取和显式插入合同；Mapper XML 不提供任何更新版本正文的路径。

- **目标**：建立不可变流程版本和依赖快照存储，禁止已发布版本原地修改。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessProcessVersion.java` — 新增版本实体并显式声明 `@TableLogic(value="0", delval="id")`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessProcessVersionMapper.java` — 新增版本插入、列表和固定版本读取签名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessProcessVersionMapper.xml` — 新增显式租户/逻辑删除过滤和版本唯一查询。
- **关键签名**：
  ```java
  AiBusinessProcessVersion selectPublishedVersion(Long tenantId, Long processId, Integer versionNo);
  List<AiBusinessProcessVersion> selectPublishedByApplication(Long tenantId, Long applicationId,
                                                               Collection<Long> processIds);
  int insertImmutable(AiBusinessProcessVersion version);
  ```
- **验收**：相同 `processId + versionNo` 重复发布幂等命中相同 hash，不同 hash 冲突；任何 update 版本正文的路径均不存在。

## Task 4：流程运行与节点运行持久层

**状态：completed（2026-08-03）**。已实现 run/node 实体、幂等读取、租户内恢复扫描、流程与节点强 CAS、失败重试计数和不可复活节点尝试。

- **目标**：为异步执行、审批等待、回调恢复、重试和时间线提供持久化状态与 CAS。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessProcessRun.java` — 新增流程运行实体。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessProcessNodeRun.java` — 新增节点运行实体。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessProcessRunMapper.java` — 新增运行创建、锁定、状态 CAS、恢复和分页签名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessProcessRunMapper.xml` — 实现运行查询、审批关联、幂等命中和恢复扫描。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessProcessNodeRunMapper.java` — 新增节点尝试和结果更新签名；仅 `insert/selectById/updateById` 等 MyBatis-Plus 内置单表方法可直接复用，CAS 和全部自定义查询必须写在 Mapper XML。
- **关键签名**：
  ```java
  AiBusinessProcessRun selectByIdempotencyKey(Long tenantId, Long processVersionId, String idempotencyKey);
  int compareAndSetStatus(Long tenantId, Long runId, String expectedStatus,
                          String nextStatus, String currentNodeId, LocalDateTime nextRetryTime);
  AiBusinessProcessRun selectWaitingByProcessInstanceId(Long tenantId, String processInstanceId);
  List<AiBusinessProcessRun> selectRecoverableRuns(LocalDateTime before, int limit);
  int insertAttempt(AiBusinessProcessNodeRun nodeRun);
  int completeAttempt(Long tenantId, Long id, String expectedStatus, String nextStatus,
                      String outputSummary, String errorCode, String errorSummary);
  ```
- **验收**：并发创建相同幂等键只返回一个 run；重复回调、重复重试和状态越级更新不能推进两次。

## Task 5：节点运行 XML 与安全日志查询

**状态：completed（2026-08-03）**。已实现运行分页、节点时间线/最后尝试/可重试/审批关联查询和安全摘要 VO；对外 ID 全部为字符串。

- **目标**：补齐节点时间线、可重试节点、审批关联和安全摘要查询，不让 Controller 或 Service 拼装 SQL。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessProcessNodeRunMapper.xml` — 新增时间线、最后尝试、可重试和关联 ID 查询。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessprocess/BusinessProcessRunQueryDTO.java` — 新增应用、流程、对象、记录、状态和时间过滤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessprocess/BusinessProcessRunVO.java` — 新增运行列表响应，ID 均序列化为字符串。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessprocess/BusinessProcessRunDetailVO.java` — 新增运行详情和节点时间线响应。
- **关键签名**：
  ```java
  List<AiBusinessProcessNodeRun> selectTimeline(Long tenantId, Long runId);
  AiBusinessProcessNodeRun selectLatestAttempt(Long tenantId, Long runId, String nodeId);
  Page<BusinessProcessRunVO> selectRunPage(Long tenantId, BusinessProcessRunQueryDTO query, Page<?> page);
  ```
- **验收**：日志只返回安全摘要和错误码；输入/输出正文、Authorization、Token、Secret 和数据库连接异常不进入响应或日志。

## Task 6：冻结 businessProcessJson 领域协议与校验器

**状态：completed（2026-08-03）**。已实现强类型 1.0 协议、稳定规范化/hash、图与依赖失败关闭校验、敏感配置拦截，并用三份冻结样例和 10 项校验器测试验证。

- **目标**：定义独立于 BPMN 的强类型业务编排协议和服务端发布校验。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/businessprocess/schema/BusinessProcessSchema.java` — 定义根协议、subject、policies 和 dependencies。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/businessprocess/schema/BusinessProcessNode.java` — 定义节点 ID、类型、名称、配置和标准出口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/businessprocess/schema/BusinessProcessEdge.java` — 定义 source/target/sourcePort/condition/default。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/businessprocess/validation/BusinessProcessSchemaValidator.java` — 实现结构、DAG、可达性、依赖和权限校验。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessprocess/BusinessProcessValidationVO.java` — 返回 error/warning、nodeId、fieldPath 和修复建议。
- **关键签名**：
  ```java
  public BusinessProcessValidationVO validate(BusinessProcessSchema schema,
                                               BusinessProcessValidationContext context);
  public BusinessProcessSchema normalize(String schemaJson);
  public String canonicalJson(BusinessProcessSchema schema);
  public String schemaHash(BusinessProcessSchema schema);
  ```
- **验收**：拒绝多个开始节点、环、悬空边、不可达节点、无结束路径、未知节点、无效出口、递归子流程、失效对象/字段和自由 URL/Secret。

## Task 7：流程定义控制面 Service 与 API

**状态：completed（2026-08-03）**。已实现应用级 CRUD、草稿 hash CAS、同应用复制、校验上下文、启停与删除引用门禁；定向测试 31/31、Mapper XML、权限迁移静态检查和 Admin 47 模块聚合编译通过。

- **目标**：提供应用级流程 CRUD、草稿保存、校验、启停和逻辑删除，普通调用不得读取其它应用资产。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessprocess/BusinessProcessDTO.java` — 新增创建/修改 DTO。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessprocess/BusinessProcessSchemaDTO.java` — 新增草稿协议和客户端基线 hash。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessprocess/BusinessProcessVO.java` — 新增列表和设计详情响应。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessService.java` — 新增控制面和乐观并发服务。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessProcessController.java` — 新增 `/ai/business/process` 管理接口和权限注解。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/businessprocess/validation/BusinessProcessValidationContextResolver.java` — 从应用对象、发布快照、权限目录和已发布 Flowable 模型解析失败关闭的校验上下文。
  - `forge-server/db/migration/V1.0.85__add_business_process_start_permission.sql` — 注册默认手动开始权限并仅继承既有应用运行权限角色。
- **关键签名**：
  ```java
  public BusinessProcessVO create(BusinessProcessDTO dto);
  public BusinessProcessVO copy(Long sourceProcessId, BusinessProcessDTO dto);
  public BusinessProcessVO getDesigner(Long processId);
  public BusinessProcessVO saveSchema(Long processId, BusinessProcessSchemaDTO dto);
  public BusinessProcessValidationVO validate(Long processId);
  public void updateStatus(Long processId, Integer status);
  public void logicalDelete(Long processId);
  ```
- **验收**：应用/主对象必须同租户且已关联；复制流程必须生成新 `processCode`，只复制草稿协议并重建节点 ID，不复制发布版本、运行状态或旧来源标识；删除有运行实例或有效发布引用的流程被阻断；草稿 hash 冲突返回明确版本冲突。

## Task 8A：持久化业务流程核心状态机

- **目标**：创建流程运行实例、按图调度节点、持久化检查点并支持恢复和人工重试。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessOrchestrator.java` — 新增 start/execute/resume/retry/cancel 主状态机。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessRunService.java` — 新增运行创建、CAS、上下文清洗和时间线服务。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessNodeExecutor.java` — 定义节点执行器 SPI。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessExecutionContext.java` — 定义不可变运行上下文和可信身份。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessprocess/BusinessProcessNodeResult.java` — 定义 COMPLETED/WAITING/FAILED、输出端口和关联 ID。
- **关键签名**：
  ```java
  public BusinessProcessRunVO start(BusinessProcessStartCommand command);
  public void execute(Long runId);
  public void resume(BusinessProcessResumeCommand command);
  public BusinessProcessRunVO retry(Long runId);
  public BusinessProcessRunVO cancel(Long runId);

  interface BusinessProcessNodeExecutor {
      String supportType();
      BusinessProcessNodeResult execute(BusinessProcessExecutionContext context,
                                        BusinessProcessNode node);
  }
  ```
- **验收**：先落 run 再异步执行；服务中断后可以从最后检查点恢复；节点调度严格服从已发布 DAG 和 CAS；未知节点失败关闭。

## Task 8B：实现条件、结束与子流程节点

- **目标**：补齐不依赖外部动作引擎的内置节点，并冻结分支、终态和子流程调用边界。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessConditionEvaluator.java` — 统一执行开始过滤和条件节点使用的结构化 AND/OR、字段及上下文规则。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/ConditionProcessNodeExecutor.java` — 执行结构化条件并选择唯一命中出口或默认出口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/EndProcessNodeExecutor.java` — 只归并流程终态，不执行隐藏字段更新或外部副作用。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/SubProcessNodeExecutor.java` — 启动同应用固定版本子流程，校验静态递归、运行深度、租户和可信身份继承。
- **关键签名**：
  ```java
  public BusinessProcessConditionResult evaluate(BusinessProcessExecutionContext context,
                                                   BusinessProcessCondition condition);
  public BusinessProcessNodeResult execute(BusinessProcessExecutionContext context,
                                           BusinessProcessNode node);
  ```
- **验收**：多分支只选择明确命中或唯一默认出口；结束节点无隐藏副作用；递归或超过深度 5 的子流程失败关闭；子流程固定版本、租户和执行身份不可由画布覆盖。

## Task 9A：抽取公共业务动作步骤运行合同

- **目标**：让旧业务动作、新业务流程动作节点和旧触发器共享一个步骤执行实现，消除动作类型分支复制。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionStepRuntime.java` — 新增步骤执行器注册、单步/多步执行和结果归一化。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionService.java` — 改为委托 `BusinessActionStepRuntime`，保持现有 API 合同。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/ActionProcessNodeExecutor.java` — 将流程动作节点转换为受控 `BusinessActionStepDTO` 并执行。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java` — 旧触发器进入兼容适配，不再维护第二套动作执行细节。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionStepExecutor.java` — 补充幂等、可重试和敏感摘要合同说明，不改变现有实现类型。
- **关键签名**：
  ```java
  public BusinessActionStepResultVO executeStep(BusinessActionExecutionContext context,
                                                 BusinessActionStepDTO step,
                                                 String idempotencyKey);
  public List<BusinessActionStepResultVO> executeSteps(BusinessActionExecutionContext context,
                                                        List<BusinessActionStepDTO> steps,
                                                        String idempotencyPrefix);
  ```
- **验收**：更新记录、创建记录和已发布业务动作均通过公共合同执行；相同 process run/node 重试不重复产生记录或审批；不支持动作明确失败；现有业务动作合同测试不回归。

## Task 9B：接入消息与受治理能力动作

- **目标**：让消息模块、企业集成和统一能力开放平台通过公共动作合同接入流程，不向画布开放 Secret 或自由 URL。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/GovernedCapabilityActionStepExecutor.java` — 仅按已发布能力/企业集成连接引用调用受治理外部能力，不接受自由 URL 或客户端凭据。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/SendMessageActionStepExecutor.java` — 补齐消息模板、通道、接收人规则和稳定消息幂等键，不在流程上下文保存通道 Secret。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/GovernedActionStepExecutorTest.java` — 覆盖消息幂等、能力白名单、失效引用、自由 URL 和敏感配置拒绝。
- **关键签名**：
  ```java
  public String supportType();
  public BusinessActionStepResultVO execute(BusinessActionExecutionContext context,
                                            BusinessActionStepDTO step,
                                            String idempotencyKey);
  ```
- **验收**：消息和受治理能力引用通过公共合同执行；重复调用不重复发送消息或制造外部副作用；自由 URL、客户端凭据、失效能力和不可用通道明确失败。

## Task 10：事件、条件与定时开始节点

- **目标**：将业务事件和统一扫描任务路由到已发布业务流程，不再要求新配置写入 `ai_business_trigger`。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessTriggerDispatcher.java` — 按应用发布快照匹配开始节点并同步创建 run。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEvent.java` — 增加稳定 eventId、`FORM_SUBMITTED/ACTION_EXECUTED` 和可信身份摘要。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEventPublisher.java` — 事务完成后优先分发新流程，旧触发器保留兼容。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerSchedulerService.java` — 保留 Job/锁/候选记录扫描，配置来源切换为已发布定时开始节点。
- **关键签名**：
  ```java
  public List<BusinessProcessRunVO> dispatch(BusinessEvent event);
  public boolean matches(BusinessProcessSchema schema, BusinessProcessNode startNode,
                         BusinessEvent event);
  public String scanScheduledProcesses();
  ```
- **验收**：事件重复投递只创建一个流程 run；草稿保存不触发；定时扫描不读取未发布流程；旧触发器兼容结果保持。

## Task 11：审批子流程启动、等待与回调恢复

- **目标**：把 Flowable 作为可等待审批节点接入业务编排器，同时保留任务表单和状态修复权威链路。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/ApprovalProcessNodeExecutor.java` — 启动固定版本审批并返回 WAITING。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessApprovalResumeListener.java` — 监听通过/驳回/取消结果并恢复唯一节点。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/event/BusinessProcessFlowResultEvent.java` — 定义 tenant、businessKey、processInstanceId、result 和安全变量摘要。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` — 支持显式审批节点上下文并在事务提交后发布恢复事件。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowVariableResolver.java` — 校验业务流程版本固定的字段/变量映射。
- **关键签名**：
  ```java
  public BusinessProcessNodeResult execute(BusinessProcessExecutionContext context,
                                           BusinessProcessNode approvalNode);
  public void onFlowResult(BusinessProcessFlowResultEvent event);
  public BusinessFlowRuntimeVO startFromBusinessProcess(BusinessProcessApprovalStartCommand command);
  ```
- **验收**：启动失败不进入 WAITING；回调按 `processInstanceId + businessKey` 唯一恢复；重复/乱序/跨租户回调不重复执行后续动作。

## Task 12：应用发布流程版本和依赖快照

**状态：completed（2026-08-04）**。应用协调发布已新增可恢复 `PROCESSES` 步骤，默认选择应用内全部启用流程，并以候选快照中的草稿 hash 固定恢复边界；候选缺少所选流程摘要时失败关闭，不回退当前草稿。每个应用版本生成或幂等复用不可变流程版本，依赖快照固定业务对象设计版本及 Flowable `model/正版本号/processDefinitionId/deploymentId`。应用快照已增加 `processes/publishedProcessVersions/runtimeActions` 白名单字段，回滚只恢复流程发布投影、不修改历史运行实例。`runtimeActions` 本任务仅冻结稳定空字段，实际 `START_PROCESS` 编译仍由 Task 13 交付。

- **目标**：把业务流程纳入应用就绪检查、不可变快照、可恢复发布和回滚。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/constant/BusinessApplicationPublishStep.java` — 在 `SNAPSHOT` 后增加 `PROCESSES`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessPublishService.java` — 校验草稿、生成版本和固定依赖。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationSnapshotService.java` — 增加 process/version/runtimeActions 白名单快照。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishService.java` — 执行和恢复 `PROCESSES` 步骤。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationReadinessService.java` — 增加流程图、对象、审批版本、按钮权限和并发冲突检查。
- **关键签名**：
  ```java
  public BusinessProcessPublishResult publishForApplication(Long applicationId,
                                                             Integer applicationVersion,
                                                             Collection<Long> processIds);
  public List<BusinessProcessSnapshot> resolvePublishedSnapshots(Long applicationId,
                                                                  Collection<Long> processIds);
  ```
- **验收**：草稿修改不影响正式触发；相同发布运行重试不重复生成不同版本；应用回滚恢复流程投影但不改变运行中实例。

## Task 13：手动开始节点编译为页面动作

- **目标**：将已发布手动开始节点投影为 `START_PROCESS` 列表/详情/表单动作，前端只提交流程编码和记录 ID。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessRuntimeActionCompiler.java` — 编译位置、权限、可见条件、确认文案和稳定动作 key。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationRuntimeService.java` — 从发布快照返回流程动作投影。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessProcessRuntimeController.java` — 新增手动 start、run 详情、retry/cancel 接口。
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 支持 `START_PROCESS` 加载态、确认、成功刷新和错误展示。
  - `forge-admin-ui/src/views/ai/crud-page.vue` — 归一化运行时流程动作并避免与内置新增/编辑按钮冲突。
- **关键签名**：
  ```java
  public List<Map<String, Object>> compileActions(BusinessProcessSnapshot snapshot);
  public RespInfo<BusinessProcessRunVO> start(String applicationCode, String processCode,
                                               BusinessProcessManualStartDTO dto);
  ```
- **验收**：客户端篡改对象、流程、actor、状态或字段数据不能绕过服务端；同一按钮重复提交幂等；无权限动作不展示且直接调用也拒绝。

## Task 14：前端独立业务流程协议与画布基础

**状态：completed（2026-08-03）**。已冻结前端 `businessProcessJson 1.0` 创建/归一化/稳定 hash 合同，新增八类业务节点注册表、DAG 图操作与历史状态，并以适配层复用现有画布、连线和布局；定向与既有回归共 48 项、ESLint 和生产构建通过。

- **目标**：复用图形基础建立独立业务画布，不改变 DingFlowDesigner 的审批协议和 BPMN 转换。
- **涉及文件**：
  - `forge-admin-ui/src/components/business-process-designer/business-process-schema.js` — 定义创建、归一化、克隆、hash 输入和协议迁移。
  - `forge-admin-ui/src/components/business-process-designer/business-process-node-types.js` — 定义开始、条件、动作、审批、子流程和结束节点注册表。
  - `forge-admin-ui/src/components/business-process-designer/useBusinessProcessDesigner.js` — 提供节点/边 CRUD、DAG 分支、撤销重做和选择状态。
  - `forge-admin-ui/src/components/business-process-designer/BusinessProcessCanvas.vue` — 复用或包装 `FlowCanvas`、EdgeLayer 和布局能力。
  - `forge-admin-ui/src/components/flow-designer/canvas/FlowCanvas.vue` — 已确认现有 `edges/nodes/toolbar` 插槽足够，本任务无需修改，保持 DingFlowDesigner 默认行为和测试。
- **关键签名**：
  ```javascript
  export function createBusinessProcessSchema({ processCode, objectRef, startType })
  export function normalizeBusinessProcessSchema(input)
  export function validateBusinessProcessGraph(schema, context)
  export function useBusinessProcessDesigner(initialSchema)
  ```
- **验收**：业务协议不能被 `convertJsonToBpmn` 消费；现有 DingFlowDesigner roundtrip 测试通过；多个开始节点、环和悬空边在保存前提示。

## Task 15：前端节点配置与审批设计器衔接

**状态：completed（2026-08-03）**。已完成克制的业务编排工作台、独立节点渲染器、结构化触发/条件/动作/审批/子流程/结束配置、依赖自动同步、草稿自动/显式保存状态、hash 冲突提示、问题定位与离开保护；审批配置复用真实 `flow/design.vue`，不在业务画布复制审批内部能力。

- **目标**：为首版节点提供业务化右侧配置，审批节点打开真实流程设计器。
- **涉及文件**：
  - `forge-admin-ui/src/components/business-process-designer/BusinessProcessDesigner.vue` — 组合工具栏、画布、节点面板、问题列表和保存状态。
  - `forge-admin-ui/src/components/business-process-designer/BusinessProcessNodeRenderer.vue` — 按节点注册表渲染稳定卡片和出口。
  - `forge-admin-ui/src/components/business-process-designer/BusinessProcessNodeConfigDrawer.vue` — 路由到对应结构化配置组件。
  - `forge-admin-ui/src/components/business-process-designer/StartNodeConfig.vue` — 配置事件、定时、手动位置、条件和身份提示。
  - `forge-admin-ui/src/components/business-process-designer/ActionAndApprovalNodeConfig.vue` — 配置动作、字段映射、消息/能力引用、审批模型和结果出口，并内嵌 `flow/design.vue`。
- **关键事件**：
  ```javascript
  defineEmits(['save', 'validate', 'open-flow-designer', 'dirty-change', 'locate-issue'])
  ```
- **验收**：普通模式无 JSON、SpEL、Java、SQL 和自由 URL；审批节点内部配置只在真实流程设计器保存；关闭后刷新模型版本和节点摘要；草稿具备防抖自动保存状态、显式保存和 hash 冲突提示，离开未保存页面必须确认。

## Task 16：应用工作台业务流程核心面板

**状态：completed（2026-08-03）**。已用应用级流程列表替换旧三入口聚合，接通创建、复制、设计、启停、逻辑删除、筛选状态回传和应用发布入口；新增全屏设计路由，完成服务端 hash CAS、保存后校验、409 冲突、目录刷新和离开确认。运行记录与迁移服务尚未交付，当前以明确预留态展示且未虚构接口。

- **目标**：用流程列表、画布、运行记录和迁移问题替换当前三个跳转按钮。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/application-workspace/ApplicationProcessPanel.vue` — 新增流程列表、创建、复制、设计、启停、运行和迁移入口。
  - `forge-admin-ui/src/views/app-center/application.[applicationCode].vue` — 将 `automation` 组件切换为 `ApplicationProcessPanel` 并传入应用发布上下文。
  - `forge-admin-ui/src/views/app-center/application-workspace/ApplicationWorkspaceNav.vue` — 文案统一为“业务流程 / 触发、审批与自动化”。
  - `forge-admin-ui/src/views/app-center/business-process.[processId].vue` — 新增全屏业务流程设计路由页面。
  - `forge-admin-ui/src/api/business-process.js` — 新增流程、Schema、校验、运行和迁移 API 封装。
- **关键交互**：
  ```javascript
  function createProcess()
  function openDesigner(processId)
  function openRunDetail(runId)
  function previewMigration()
  function requestApplicationPublish()
  ```
- **验收**：用户从应用进入后无需先选择对象设计器面板；新流程在当前应用选择主对象；设计器新页签返回后保持原应用和筛选状态。

## Task 17：迁移预览、幂等执行与问题报告

- **目标**：把旧触发器、对象 FLOW Binding 和审批结果动作转换为新流程草稿，不用 Flyway 猜测 JSON 语义。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessMigrationService.java` — 实现 preview/apply、来源合并、幂等和问题分类。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessprocess/BusinessProcessMigrationDTO.java` — 定义 applicationId、sourceIds、previewHash 和执行策略。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessprocess/BusinessProcessMigrationPreviewVO.java` — 返回生成图、旧来源、警告和阻塞项。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessProcessMigrationController.java` — 新增 preview/apply/issues 权限接口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessBindingMapper.xml` — 补充按应用/对象批量读取旧 FLOW/TRIGGER/ACTION 来源的 XML 查询，不修改旧记录。
- **关键签名**：
  ```java
  public BusinessProcessMigrationPreviewVO preview(BusinessProcessMigrationDTO dto);
  public BusinessProcessMigrationResultVO apply(BusinessProcessMigrationDTO dto);
  public List<BusinessProcessMigrationIssueVO> listIssues(Long applicationId);
  ```
- **验收**：预览不写数据；apply 要求 previewHash 一致；重复执行返回相同流程；无法转换项阻塞而不是静默跳过；旧配置不物理删除。

## Task 18：旧入口停写与采购审批样例迁移

- **目标**：在迁移验证完成后收口普通入口，并将采购审批样例切换为可验证的新画布流程。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` — 隐藏旧 `flow-app/triggers/actions` 普通入口，保留迁移问题和高级兼容诊断。
  - `forge-admin-ui/src/views/app-center/application-workspace/ApplicationAutomationPanel.vue` — 删除工作台引用后保留兼容组件或标记待后续删除，不再作为主入口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessTriggerController.java` — 迁移完成应用的写接口返回只读迁移提示，查询/日志兼容保留。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessFlowController.java` — 旧 binding 写接口增加迁移状态保护，任务表单和运行接口不变。
  - `forge-server/db/seed/demo/application_business_process_purchase_sample.sql` — 新增或调整演示 seed，以应用流程协议引用现有采购 BPMN，不覆盖已编辑 BPMN XML。
- **验收**：普通用户只有一个可写业务流程入口；旧运行中审批继续办理；旧日志可查；采购“提交审批 → 审批 → 状态 → 消息”在一张业务画布可见。

## Task 19：自动化验证、两阶段审查与部署门禁

- **目标**：按自动化测试标准验证协议、迁移、运行、审批、权限、发布和前端体验，完成 Spec 合规和代码质量审查。
- **新增/修改测试文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/businessprocess/BusinessProcessSchemaValidatorTest.java` — 图、字段、依赖、循环和敏感配置。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessOrchestratorTest.java` — 状态机、幂等、恢复、分支和重试。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessprocess/ApprovalProcessNodeExecutorTest.java` — 启动、等待、回调、重复和乱序。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessMigrationServiceTest.java` — 预览、转换、问题和重复执行。
  - `forge-admin-ui/src/components/business-process-designer/__tests__/business-process-designer.spec.js` — 协议、画布、节点配置和审批设计器入口。
- **必须执行的增量命令**：
  ```bash
  cd forge-server
  mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
    -Dtest=BusinessProcessSchemaValidatorTest,BusinessProcessOrchestratorTest,ApprovalProcessNodeExecutorTest,BusinessProcessMigrationServiceTest,GovernedActionStepExecutorTest test

  source ~/.nvm/nvm.sh && nvm use v20.19.0
  cd forge-admin-ui
  pnpm exec vitest run src/components/business-process-designer/__tests__/business-process-designer.spec.js
  pnpm exec eslint src/components/business-process-designer src/views/app-center src/api/business-process.js
  pnpm build
  ```
- **数据库验证**：新库、存量库、重复 Flyway；唯一索引、逻辑删除墓碑、跨租户查询和迁移幂等。
- **真实 E2E**：手动提交审批、记录新增自动化、定时分层提醒、审批通过/驳回/取消、服务重启恢复、节点失败重试、应用回滚和旧实例继续办理。
- **审查顺序**：先逐功能 1-40 做 Spec 合规审查，再做租户、权限、状态机、幂等、并发、敏感日志、SQL 和前端质量审查。
- **文档回填**：每个命令、结果、警告、跳过项、真实环境门禁和服务清理追加到 `execution-log.md`；未执行项不得表述为通过。

## 完成门禁

- [ ] Spec 功能 1-40 均能对应至少一个完成 Task 和验证证据。
- [ ] 应用工作台不存在面向普通用户的第二套流程/触发器/审批后动作写入口。
- [ ] `DingFlowDesigner.flowJson` 与 BPMN 双向转换未被业务协议污染，现有审批设计器回归通过。
- [ ] 新流程只读取应用发布快照，草稿不能在正式运行触发。
- [ ] 同一记录重复提交、重复事件和重复回调不会创建重复审批或重复副作用。
- [ ] 旧配置和运行中实例在迁移、停写和回滚阶段均可追溯，不物理删除。
- [ ] 状态、审批权限、定时身份、外部调用和迁移完成专项人工审查。
- [ ] `spec.md/tasks.md/test-spec.md/execution-log.md` 与实际实现和验证结果一致。

## Task 20：业务流程设计器可用性修复

**状态：completed（2026-08-07）**。上一轮只覆盖构造图，并错误要求审批模型必须存在对象 `FLOW Binding`；本轮已按 20A/20B/20C 完成纠偏。真实 Admin/Flow 发布和审批实例联调仍属于 Task 19，不用受控前端回归代替。

- **目标**：修复应用级业务流程设计器中审批模型目录、条件分支、画布连线、中文语义和节点删除的产品断层，使新增、配置、删除、保存和发布形成一致闭环。
- **涉及文件**：
  - `forge-server/forge-flow/forge-flow-client/src/main/java/com/mdframe/forge/flow/client/FlowClient.java` — 修正模型列表响应类型，供应用级目录读取租户已发布审批资产。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessProcessController.java` — 提供当前流程可引用的租户已发布审批模型目录，不要求对象 `FLOW Binding`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessService.java` — 保持流程所属应用权限校验，但不把对象绑定作为模型目录门禁。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/businessprocess/validation/BusinessProcessValidationContextResolver.java` — 目录和发布检查统一验证模型状态、版本、部署和权限可见性。
  - `forge-admin-ui/src/api/business-process.js` 与 `forge-admin-ui/src/views/app-center/business-process.[processId].vue` — 改用当前流程审批模型目录，并把不可用原因以中文业务提示展示。
  - `forge-admin-ui/src/components/flow-designer/panel/condition-expression.js` — 抽取原审批设计器已有的结构化规则归一化、表达式生成和反解析能力。
  - `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` — 消费共享表达式工具，保持既有 BPMN 条件配置兼容。
  - `forge-admin-ui/src/components/business-process-designer/BusinessProcessConditionConfig.vue` — 提供中文字段、关系、取值、AND/OR 和默认分支配置，不向普通用户暴露技术端口或表达式。
  - `forge-admin-ui/src/components/business-process-designer/useBusinessProcessDesigner.js` — 原子同步条件 branches、ports 与 outgoing edges；支持安全删除多出口节点并恢复 DAG。
  - `forge-admin-ui/src/components/business-process-designer/business-process-layout.js` — 新增业务 DAG 分层布局和端口/汇合锚点路由，不再把业务卡片当作 BPMN 网关。
  - `forge-admin-ui/src/components/business-process-designer/BusinessProcessCanvas.vue`、`BusinessProcessNodeRenderer.vue`、`BusinessProcessDesigner.vue` — 接入独立布局，统一中文结果标签，并把删除按钮直接放在可删除节点卡片上。
- **测试先行**：
  - 条件节点插入后保持开始、条件、公共后继主轴居中，二至多分支连线和插入按钮互不重叠。
  - 新增、删除、重命名条件分支时，节点端口与真实出边一一同步且图校验通过。
  - 删除条件/审批多出口节点时，所有入边安全接回唯一公共后继，不残留悬空边或重复边。
  - 节点卡片只显示“审批通过、审批驳回、审批取消、执行失败、条件满足、其他情况”等中文业务语义，删除按钮可单独点击且不触发节点选择。
  - 审批模型目录只返回当前租户有权限且已发布/已部署的模型；不要求对象 `FLOW Binding`，失效模型以中文问题提示阻断发布。
- **验收**：新建流程后可直接选择租户已发布审批模型；条件分支可用结构化规则完成配置；不同下游、汇合、共享后继和旧草稿读入均保持稳定；普通用户界面不出现 `APPROVED/MATCHED/OTHERWISE` 等技术枚举。

### Task 20A：审批目录解除对象绑定门禁

- [x] 修改 `FlowClient.getModelList` 返回 `FlowResult<List<Map<String,Object>>>`，通过 Flow Client 编译及 Generator 目录映射回归验证响应合同。
- [x] `BusinessProcessValidationContextResolver` 从模型列表构造目录，只保留已启用且具有正版本、部署 ID 和流程定义 ID 的模型；删除按对象代码查询 `FLOW Binding` 的目录门禁。
- [x] 目录与发布校验共用同一模型有效性判定，覆盖“无对象 FLOW Binding 仍可选择/发布”“未部署模型不可选择”“失效版本阻断发布”和可信租户隔离。

### Task 20B：业务画布独立 DAG 布局

- [x] 新增纯函数 `business-process-layout.js`，使用 `dagre` 多边图从节点入度/出度分层，固定业务卡片尺寸，按端口顺序分配源端锚点和汇合节点入端锚点，保证每条边有唯一路径。
- [x] `BusinessProcessCanvas.vue` 改用该布局；插入按钮使用真实路径最长线段中点；不修改 BPMN `layout-engine.js` 和 DingFlowDesigner 行为。
- [x] 覆盖条件多分支分别进入下游卡片再汇合、审批多结果共享后继、跨层直达边绕过中间卡片、绕行线与汇合线不重叠；孤立、重复或悬空边继续由图校验失败关闭，不以 JSON 可解析替代合法性。

### Task 20C：旧草稿归一化与用户语义

- [x] 读入旧草稿时恢复审批固定端口顺序和条件分支顺序；重复、孤立或悬空连线无法安全推断时保留并生成阻断问题，不静默把错误分支接到其它节点。
- [x] 条件分支继续复用 `condition-expression.js`，默认分支不生成条件表达式；结果标签和问题提示全部使用中文。
- [x] 卡片删除、分支增删、下游插入后执行客户端图校验并保留撤销记录；受控浏览器回归覆盖真实 Schema 装配、结构化条件和卡片删除。

## Task 21：低代码应用/对象信息架构收口

- [ ] `BusinessObjectDesignerShell.vue` 与 `object-designer.[objectCode].vue`：对象普通导航只保留基本信息、数据结构、关联关系、表单资产和对象级检查；旧流程、触发器、动作和应用数据权限入口迁移到高级兼容区并标明只读/迁移状态。
- [ ] `application-workspace/ApplicationWorkspaceNav.vue`、`ApplicationProcessPanel.vue` 和页面设计入口：应用工作台收口页面/列表视图、业务流程/自动化、角色数据权限和发布；列表设计从对象入口迁移为应用页面配置，支持继承对象默认列表预设。
- [ ] 增加产品文案和发布就绪检查映射：对象缺字段/关系/表单是对象问题，流程模型/触发器/角色范围是应用问题，不再出现跨层复合错误。
- [ ] 为导航唯一写入口、列表视图归属、关系保留在对象层和数据权限两层合同增加前端路由/组件测试；迁移前旧配置只读可查，不物理删除。
