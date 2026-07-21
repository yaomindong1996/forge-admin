# 定时任务 Flowable 编排 Implementation Plan

> **For agentic workers:** 按仓库 code-copilot 工作流在当前会话内联执行；使用复选框逐项追踪，不创建 Git 提交，不回退 V1-V8 工作区成果。

> status: complete
> scope: V9 only

**Goal:** 让定时和手动任务以技术身份幂等启动一个保存时固定的 Flowable processDefinitionId，并把真实 processInstanceId 关联到任务执行日志。

**Architecture:** forge-starter-job 定义中立 SPI；forge-plugin-flow 提供固定定义校验和本地运行实现；forge-flow-client 通过 V8 SecureOutboundClient 提供独立 Flow 服务适配器。Job 插件只按 invokeMode 编排 SINGLE/FLOW，Flow 端独占模型、流程定义和技术身份校验。

**Tech Stack:** Java 17、Spring Boot 3、Quartz、Flowable 7、MyBatis XML、SecureOutboundClient、Flyway、Vue 3、Naive UI、Vitest、JUnit 5、Mockito。

---

## Task 1: Spec 与测试基线

**Files:**
- Modify: `code-copilot/changes/定时任务Flowable编排/spec.md`
- Modify: `code-copilot/changes/定时任务Flowable编排/tasks.md`
- Create: `code-copilot/changes/定时任务Flowable编排/test-spec.md`
- Create: `code-copilot/changes/定时任务Flowable编排/execution-log.md`

- [x] 固化 processDefinitionId、独立 Flow 首验、技术身份和仅启动流程四个门禁。
- [x] 建立 P0/P1 增量测试矩阵并复用 V8 验证基线。
- [x] 完成后回填执行证据、跳过项、父路线图和可复用决策。

## Task 2: Flyway 与固定定义 SPI

**Files:**
- Create: `forge-server/db/migration/V1.0.48__add_job_flow_orchestration.sql`
- Create: `forge-starter-job/.../flow/JobFlowExecutor.java`
- Create: `forge-starter-job/.../flow/JobFlowBindingSnapshot.java`
- Create: `forge-starter-job/.../flow/JobFlowExecutionRequest.java`
- Create: `forge-starter-job/.../flow/JobFlowExecutionResult.java`
- Modify: Job 配置/日志 Entity、DTO、VO、Mapper XML 和 Quartz JobDataMap
- Test: `forge-plugin-job/.../migration/JobFlowMigrationContractTest.java`

- [x] 先写迁移合约测试，覆盖默认 SINGLE、固定定义字段、日志实例索引、字典 tenant_id=1 和防重复保护。
- [x] 新增 V1.0.48 迁移并保持旧任务无损回填为 SINGLE。
- [x] 创建无 Flowable 依赖的 SPI 与不可变请求/响应模型。
- [x] 扩展 Job 持久化协议和调度快照，确保 processDefinitionId 进入 Quartz JobDataMap。

## Task 3: Flow 固定定义校验与幂等启动

**Files:**
- Modify: `forge-plugin-flow` FlowModel Mapper/XML
- Create: `forge-plugin-flow/.../job/JobFlowRuntimeService.java`
- Create: `forge-plugin-flow/.../job/LocalJobFlowExecutor.java`
- Create: `forge-plugin-flow/.../job/JobFlowTechnicalIdentityProperties.java`
- Create: `forge-flow-server/.../controller/JobFlowController.java`
- Test: `forge-plugin-flow/.../job/JobFlowRuntimeServiceTest.java`

- [x] 先写草稿、挂起、版本/部署不匹配、定义不存在和重复 businessKey 测试。
- [x] 用 Mapper XML 查询租户内已发布版本快照，并向 RepositoryService 复核定义状态。
- [x] 使用 processDefinitionId 启动，不调用 latestVersion 或自动部署。
- [x] 以 `job:<jobConfigId>:<executionId>` 幂等写 sys_flow_business，重复请求返回原实例。
- [x] 专用 Controller 只接受绑定快照和 jobInput，身份从 Flow 服务配置解析。

## Task 4: 独立 Flow 安全适配器

**Files:**
- Modify: `forge-flow-client/pom.xml`
- Create: `forge-flow-client/.../job/RemoteJobFlowExecutor.java`
- Create: `forge-flow-client/.../job/JobFlowRemoteProperties.java`
- Modify: Flow Client 自动配置
- Test: `forge-flow-client/.../job/RemoteJobFlowExecutorTest.java`

- [x] 先写 FLOW_API 场景、无 X-Inner-Call、危险身份字段缺失、非 2xx 和恢复查询测试。
- [x] 使用 SecureOutboundClient 调用专用 validate/start/status API，禁止创建 RestTemplate/HttpClient/OkHttpClient。
- [x] Authorization 只来自服务端配置，日志不记录 Token、jobInput 或完整 URL。
- [x] 远程失败关闭，不回退本地实现、最新版本或草稿部署。

## Task 5: Job 保存与执行生命周期

**Files:**
- Modify: `JobConfigValidator.java`
- Modify: `SysJobConfigServiceImpl.java`
- Create: `JobFlowOrchestrationService.java`
- Modify: `QuartzJobExecutor.java`
- Modify: `JobExecutionLifecycleService.java`
- Modify: `SysJobLogMapper.java` / XML
- Test: Job Validator、Orchestration、Quartz 和生命周期测试

- [x] SINGLE 继续校验 BEAN/HANDLER/RPC；FLOW 清空执行器目标并强制由 SPI 返回可信绑定快照。
- [x] 新增/修改 FLOW 配置时固定 modelKey/version/deploymentId/processDefinitionId，不信任前端快照。
- [x] Quartz 先创建 executionId，再用确定性 businessKey 启动流程并原子保存 processInstanceId + SUCCESS。
- [x] 响应丢失时按 businessKey 恢复；启动前失败才记录 FAILED，流程后续状态不回写任务日志。
- [x] 定时、手动和 Open API 触发复用同一 FLOW 路由与并发治理。

## Task 6: 前端流程绑定与历史入口

**Files:**
- Modify: `forge-admin-ui/src/api/system/job.js`
- Modify: `forge-admin-ui/src/views/system/job-config/job-config-form.js`
- Modify: `forge-admin-ui/src/views/system/job-config/components/JobExecutionSection.vue`
- Modify: `forge-admin-ui/src/views/system/job-config/components/JobConfigWorkbench.vue`
- Modify: `forge-admin-ui/src/views/system/job-log-list.vue`
- Test: existing job form/page tests plus V9 binding cases

- [x] 使用字典渲染 SINGLE/FLOW，FLOW 模式只展示已发布模型和版本选择。
- [x] 切换 FLOW 时隐藏 Bean/Handler/RPC 配置，不提供节点、脚本、类名或身份字段。
- [x] 提交只包含 modelKey/version；部署快照由后端回填并在详情中只读展示。
- [x] 日志详情有 processInstanceId 时跳转 `/flow/monitor?processInstanceId=...`。
- [x] 补充 Long ID 字符串、安全切换和 payload 归一化测试。

## Task 7: 聚合验证与回填

- [x] 运行 job、flow、flow-client 目标测试并记录数量和预期故障日志。
- [x] 运行 Flow Server、Admin Server 聚合构建及前端定向测试/生产构建。
- [x] 执行 Flyway placeholder、Mapper XML、直连 HTTP/X-Inner-Call、脚本入口和 `git diff --check` 静态检查。
- [x] 回填 V9 spec/tasks/test-spec/execution-log 和父路线图；真实 Flyway、服务身份、Flow 服务 E2E 标记为用户侧验收。
