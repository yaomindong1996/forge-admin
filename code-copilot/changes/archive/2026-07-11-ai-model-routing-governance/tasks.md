# 任务拆分 — AI 模型路由与调用治理

> 拆分顺序：数据契约 → 模型能力 → 路由策略 → 路由器 → 健康状态 → 调用审计 → 调用链 → 管理端 → 验证
> 每个 Task 都先补 Red 测试，再做最小实现并回填 `execution-log.md`；没有 HARD-GATE 不进入编码。

## 前置条件

- [x] 用户执行 `/apply ai-model-routing-governance`，Spec 状态从 propose 切换为 apply；
- [x] 当前分支不是 master/main，并保留现有工作区中的用户改动；
- [x] 读取根 `AGENTS.md`、当前四份变更文档、长期记忆和自动化测试标准；
- [x] 使用 Java 17 和 Node `v20.19.0`；
- [x] 记录 `forge-plugin-ai` 现有 51 tests 基线，确认测试没有被 Maven skip；
- [x] 自动测试不调用真实付费模型，不写入真实 API Key。

## Apply 结果快照

| Task | 状态 | 结果 |
|------|------|------|
| 1–3 | 完成 | V1.0.18、能力目录、策略/候选 CRUD、批量查询与逻辑删除已实现 |
| 4–5 | 完成 | 确定性 Router、租户校验、模型级 HealthRegistry、Lease 和安全失败分类已实现 |
| 6–7 | 完成 | 调用审计、Token/成本/P95、90 天保留、同步/流式接入且无失败后换模型 |
| 8–9 | 完成 | Agent PINNED/POLICY、模型配置、路由策略与调用记录最小界面已实现 |
| 10 | 完成（条件项除外） | 63 tests、Admin package、ESLint/build、XML/SQL/安全扫描通过；实库与浏览器人工验收待 review |

> 下方粒度更细的未勾选项表示尚无独立自动化或人工验证证据，不等同于对应功能未实现；review 时按风险优先补齐。

## Review Fix 结果

- [x] `AiModelFailureClassifier` 识别 cause chain、内容安全错误码和调用取消，内容安全/取消不计入模型故障；
- [x] Router、调用审计、失败分类、能力 Mapper、健康注册表和策略 Mapper 改为构造器强制装配，Resolver 删除旧模型选择兜底；
- [x] 路由治理页补齐候选启停、供应商/模型/时间筛选、跳过原因、完整分页目录加载和独立权限控制；
- [x] 新增 `AiClientRoutingGovernanceTest`、`AiModelRoutePolicyServiceTest`、`AiAgentServiceTest`、`AiInvocationLogRetentionJobTest`，并扩充 Router、Health、Provider 和失败分类回归。

## Task 1: 建立模型治理数据契约

- **目标**：通过 V1.0.18 一次性建立模型能力、策略、候选、Agent 模式和调用日志结构。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql` — 新建迁移；
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java` — 增加 contextWindow 和价格字段；
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/domain/AiAgent.java` — 增加 modelSelectionMode、routePolicyId；
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/constant/AiModelCapabilityCode.java` — 稳定能力代码；
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/constant/AiModelSelectionMode.java` — PINNED/POLICY。
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/constant/AiModelRouteSource.java` — REQUEST/PINNED/PROVIDER_DEFAULT/POLICY；
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/health/AiModelHealthRegistry.java`、`AiModelHealthLease.java`、`AiModelHealthKey.java`、`AiModelHealthSnapshot.java`、`AiModelHealthStatus.java`、`AiModelFailureCategory.java` — Task 4 可依赖的完整健康 SPI 契约，默认实现与分类器留到 Task 5。
- **关键签名**：
  ```java
  public enum AiModelSelectionMode {
      PINNED, POLICY;
      public static AiModelSelectionMode fromNullable(String value) { }
  }

  public record AiModelHealthKey(Long tenantId, Long providerPk, Long modelPk) { }

  public interface AiModelHealthRegistry {
      AiModelHealthSnapshot snapshot(AiModelHealthKey key);
      Optional<AiModelHealthLease> tryAcquire(AiModelHealthKey key);
      AiModelHealthLease acquireManualProbe(AiModelHealthKey key);
      void reset(AiModelHealthKey key);
      void resetProvider(Long tenantId, Long providerPk);
  }

  public interface AiModelHealthLease extends AutoCloseable {
      AiModelHealthKey key();
      void success();
      void failure(AiModelFailureCategory category);
      void cancel();
      void abort();
      @Override default void close() { abort(); }
  }
  ```
- **实施步骤**：
  - [ ] 编写常量/选择模式失败测试，覆盖 null/blank→PINNED、unknown 非空值失败关闭；
  - [ ] 执行 testCompile，确认缺少新类型而 Red；
  - [ ] 编写 V1.0.18，所有 DDL 使用 information_schema，字典/资源使用 NOT EXISTS，tenantId=1；
  - [ ] 新表补齐标准审计字段；模型能力、策略、候选增加 del_flag、logic_delete_active 与活跃唯一键；
  - [ ] 调用日志明确为技术日志，增加 requestId 唯一键和 tenant/model/agent 时间索引；
  - [ ] 实现枚举/实体字段并运行目标测试 Green；
  - [ ] 执行 Flyway placeholder、tenantId、版本唯一性和防重复静态检查。
- **验收标准**：迁移可重复防护，历史 Agent 默认 PINNED，未修改现有供应商/模型选择结果。

## Task 2: 实现模型能力与价格配置

- **目标**：模型保存时事务化维护能力集合和治理数值，查询时返回完整配置。
- **涉及文件**：
  - `model/capability/domain/AiModelCapability.java` — 新实体；
  - `model/capability/mapper/AiModelCapabilityMapper.java`；
  - `model/capability/mapper/AiModelCapabilityMapper.xml` — 按 modelIds 批量查询、逻辑删除/重建；
  - `model/dto/AiModelSaveDTO.java` — 替代 Controller 直接接实体；
  - `model/vo/AiModelVO.java` — 返回能力、治理字段和只读健康状态；
  - `model/service/AiModelService.java`、`model/controller/AiModelController.java`；
  - `model/service/AiModelServiceTest.java`。
- **关键签名**：
  ```java
  @Transactional(rollbackFor = Exception.class)
  public Long addModel(AiModelSaveDTO dto) { }

  @Transactional(rollbackFor = Exception.class)
  public void updateModel(AiModelSaveDTO dto) { }

  public Map<Long, Set<String>> selectEnabledCapabilityCodes(Collection<Long> modelIds) { }
  ```
- **实施步骤**：
  - [ ] 先写 Red 测试：未知能力拒绝、价格非负、重复能力去重、保存失败整体回滚；
  - [ ] Mapper XML 实现能力批量查询和关系重建，显式过滤 del_flag/status/tenant；
  - [ ] Service 校验 capabilityCode 必须来自稳定集合，contextWindow/单价不得为负；
  - [ ] 模型更新成功后清理对应健康状态和 ChatClient 缓存；
  - [ ] Controller 改接 DTO/VO，不返回内部逻辑删除和审计控制字段；
  - [ ] 运行目标测试、Mapper XML 语法检查和已有模型服务回归。
- **验收标准**：模型和能力事务一致，批量列表无 N+1，现有模型没有能力配置时仍可固定调用。

## Task 3: 实现路由策略持久化与接口

- **目标**：提供可复用策略和显式候选的持久化与 CRUD；预览在 Router 可用后的 Task 4 接入。
- **涉及文件**：
  - `routing/domain/AiModelRoutePolicy.java`、`AiModelRouteTarget.java`；
  - `routing/dto/AiModelRoutePolicySaveDTO.java`；
  - `routing/vo/AiModelRoutePolicyVO.java`；
  - `routing/mapper/AiModelRoutePolicyMapper.java/.xml`；
  - `routing/mapper/AiModelRouteTargetMapper.java/.xml`；
  - `routing/service/AiModelRoutePolicyService.java`；
  - `routing/controller/AiModelRoutingController.java`；
  - `routing/service/AiModelRoutePolicyServiceTest.java`。
- **关键签名**：
  ```java
  @Transactional(rollbackFor = Exception.class)
  public Long createPolicy(AiModelRoutePolicySaveDTO dto) { }

  @Transactional(rollbackFor = Exception.class)
  public void updatePolicy(AiModelRoutePolicySaveDTO dto) { }

  public Page<AiModelRoutePolicyVO> pagePolicy(int pageNum, int pageSize, String keyword, String status) { }
  ```
- **实施步骤**：
  - [ ] 先写 Red 测试：policyCode 在租户内未逻辑删除记录中唯一（status 不影响）、空候选拒绝、重复目标拒绝、跨租户 modelId 拒绝；
  - [ ] XML 实现策略分页、详情聚合、候选批量查询和逻辑删除；
  - [ ] 保存时校验 requiredCapabilities、目标模型/供应商状态与租户归属；
  - [ ] 更新采用“策略主表更新 + 旧候选逻辑删除 + 新候选批量插入”的单事务；
  - [ ] 删除被 POLICY Agent 使用的策略时失败关闭；
  - [ ] 增加加密 CRUD 与权限注解；
  - [ ] 运行服务测试和 XML 语法检查。
- **验收标准**：策略只包含管理员显式候选，跨租户引用和悬空引用均被拒绝。

## Task 4: 实现确定性 AiModelRouter 并接入 Resolver

- **目标**：把模型选择从字符串解析升级为可解释 RouteDecision，同时保持固定模式兼容。
- **涉及文件**：
  - `routing/AiModelRouter.java`；
  - `routing/PolicyBasedAiModelRouter.java`；
  - `routing/RouteRequest.java`、`RouteDecision.java`、`RoutedInvocation.java`、`RouteCandidateSkip.java`；
  - `routing/constant/AiModelRouteReason.java` — REQUEST_EXPLICIT_PAIR/REQUEST_PROVIDER_DEFAULT/REQUEST_MODEL_WITH_RESOLVED_PROVIDER/PINNED_MODEL/PROVIDER_DEFAULT/POLICY_PRIORITY；
  - `routing/dto/AiModelRoutePreviewDTO.java`、`routing/vo/AiModelRoutePreviewVO.java`；
  - `routing/mapper/AiModelRoutingQueryMapper.java/.xml` — 一次查询候选模型、供应商和 Adapter 元数据；
  - `routing/controller/AiModelRoutingController.java` — 增加 preview 入口；
  - `client/AiInvocationResolver.java`；
  - `provider/mapper/AiProviderMapper.java/.xml`、`provider/service/AiProviderService.java` — `requireEnabledDefaultProvider` 恰好一条契约；
  - `routing/PolicyBasedAiModelRouterTest.java`；
  - `client/AiInvocationResolverTest.java`；
  - `provider/service/AiProviderServiceTest.java`。
- **关键签名**：
  ```java
  public interface AiModelRouter {
      RoutedInvocation route(RouteRequest request);
      RouteDecision preview(RouteRequest request);
  }

  public record RouteDecision(
      AiProvider provider,
      AiModel model,
      AiModelRouteSource source,
      AiModelRouteReason reason,
      Long policyId,
      List<RouteCandidateSkip> skippedCandidates) { }

  public record RoutedInvocation(
      RouteDecision decision,
      AiModelHealthLease healthLease) implements AutoCloseable {
      @Override public void close() { healthLease.close(); }
  }
  ```
- **实施步骤**：
  - [ ] 先写 Red 测试覆盖 REQUEST、PINNED、PROVIDER_DEFAULT、POLICY 四种来源；
  - [ ] 按 Spec 决策表覆盖 provider/model 显式字段四种组合，禁止实现自行拼接 Agent model；
  - [ ] Mapper XML 查询当前租户最多两条启用默认供应商；零条、多条、停用/删除、跨租户均按 Spec 失败关闭；
  - [ ] 覆盖能力全包含、停用/删除/跨租户排除、priority/id 稳定排序；
  - [ ] 覆盖 POLICY 无候选时不回退供应商默认模型；
  - [ ] 覆盖 REQUEST/PINNED/PROVIDER_DEFAULT 遇到 OPEN 直接失败，HALF_OPEN 只有 acquire 成功才允许调用；
  - [ ] 覆盖 preview 不调用 HealthRegistry.tryAcquire、不改变 HALF_OPEN 状态；
  - [ ] XML 一次加载策略候选，禁止 Router 循环查询模型/供应商；
  - [ ] Resolver 返回 RoutedInvocation，审计只消费其中纯数据 RouteDecision，不再只返回 model 字符串；
  - [ ] 显式模型校验必须属于最终 provider 且启用；
  - [ ] 接入 preview DTO/VO 和权限，preview 只读取快照且不创建 ChatModel；
  - [ ] 运行 Router/Resolver 测试并复跑 Provider Adapter 相关测试。
- **验收标准**：同一输入和健康快照选择一致，选择原因可审计，不存在隐式全库路由。

## Task 5: 建立模型健康状态机与失败分类

- **目标**：把 agentCode 熔断改为模型目标级健康 SPI，并准确区分健康相关失败。
- **涉及文件**：
  - `health/AiModelHealthRegistry.java`；
  - `health/InMemoryAiModelHealthRegistry.java`；
  - `health/AiModelHealthKey.java`、`AiModelHealthSnapshot.java`、`AiModelHealthStatus.java`；
  - `health/AiModelFailureClassifier.java` — 使用 Task 1 的 AiModelFailureCategory 契约实现安全分类；
  - `client/CircuitBreaker.java` — 删除或变为兼容委托，不保留第二套状态；
  - `provider/service/AiProviderService.java`、`provider/support/AiProviderCacheEvictionScheduler.java`；
  - `health/AiModelConnectionTestService.java` — 服务端加载模型与供应商凭据进行低 Token 测试；
  - `model/controller/AiModelController.java` — 新增 `POST /ai/model/{id}/test`；
  - 对应 `InMemoryAiModelHealthRegistryTest`、`AiModelFailureClassifierTest`、Provider 测试。
- **关键签名**：
  ```java
  public final class InMemoryAiModelHealthRegistry implements AiModelHealthRegistry { }
  ```
- **实施步骤**：
  - [ ] 先写状态机 Red 测试：3 次失败 OPEN、恢复窗后 HALF_OPEN、单试探、成功恢复、失败重开；
  - [ ] 覆盖 HALF_OPEN Lease 在 PREPARATION 失败时 abort 释放试探权且失败数不增加；
  - [ ] 使用 Clock 注入测试时间，不在测试中 sleep；
  - [ ] 失败分类覆盖 timeout/network/429/5xx/auth/model-not-found 与 validation/content/cancel；
  - [ ] 只有模型请求已发出后的健康相关分类和 UNKNOWN 增加失败计数；CANCEL/本地校验只记录结果；
  - [ ] Router 调用 snapshot/tryAcquire 跳过 OPEN 和竞争失败 HALF_OPEN；Lease success/failure/cancel/abort 必须幂等，只允许一种终态；
  - [ ] 供应商手动测试成功和配置 after-commit 更新调用 `resetProvider(tenantId, providerPk)`；模型更新调用单键 reset，避免 Service 互相注入或循环枚举；
  - [ ] 单模型测试只接收 modelPk，浏览器不提交 API Key；通过 acquireManualProbe 获取 Lease，成功/失败更新对应健康状态并复用安全异常诊断；
  - [ ] 运行状态机、分类器、路由和 Provider 回归测试。
- **验收标准**：熔断键为 tenantId/providerPk/modelPk，状态流转线程安全，默认实现不依赖 Nacos/Redis。

## Task 6: 建立安全调用审计、Token 与成本汇总

- **目标**：记录每次模型调用的安全治理元数据，并提供查询与保留清理。
- **涉及文件**：
  - `invocation/domain/AiModelInvocationLog.java`；
  - `invocation/dto/AiInvocationPageQuery.java`；
  - `invocation/vo/AiInvocationLogVO.java`、`AiInvocationSummaryVO.java`；
  - `invocation/mapper/AiModelInvocationLogMapper.java/.xml`；
  - `invocation/service/AiModelInvocationRecorder.java`、`AiModelInvocationQueryService.java`；
  - `invocation/AiInvocationObservation.java`、`AiInvocationPhase.java`、`AiInvocationOutcome.java` — 同步/流式/解析失败共用的不可变记录输入与阶段/结果枚举；
  - `invocation/job/AiInvocationLogRetentionJob.java`；
  - `invocation/controller/AiModelInvocationController.java`；
  - `forge-plugin-ai/pom.xml` — 增加 `forge-starter-job`；
  - 对应 Recorder、Query、Retention 测试。
- **关键签名**：
  ```java
  public void record(AiInvocationObservation observation) { }

  public AiInvocationSummaryVO summarize(AiInvocationPageQuery query) { }

  @ScheduledJob(cron = "0 20 2 * * ?", name = "aiInvocationLogRetention")
  public String cleanExpiredInvocationLogs(String retentionDays) { }
  ```
- **Observation 字段契约**：requestId、tenantId、agentCode、phase、dispatched、outcome、latencyMillis 非空；userId/sessionId/routeSource/routeReason/policyId/providerPk/modelPk/providerModelId/adapterCode/errorCategory/httpStatus/errorCode/Token/价格快照按解析阶段和 Usage 可用性允许 NULL；不得接收 Throwable，不得包含 Prompt、响应、Header、API Key 或 nativeUsage。
- **实施步骤**：
  - [ ] Red 测试确认 requestId 幂等、Usage null 安全、错误码白名单、价格快照和敏感字段不存在；
  - [ ] XML 实现日志插入、分页、汇总、P95 nearest-rank 精确统计和按截止时间批量物理删除；
  - [ ] 汇总成本先累计 DECIMAL(38,0) 分子，再除以 1,000,000 并 HALF_UP 到 long 分；覆盖半分边界和溢出边界；
  - [ ] Usage 或任一价格快照缺失时 costAvailable=false、不按 0 计费，并汇总 costUnavailableCount；
  - [ ] P95 使用 nearest-rank `ceil(0.95*N)`，覆盖空集合、单条和小样本；
  - [ ] Retention 默认 90 天，参数非法失败关闭，不接受任意 SQL/表名；只删除早于截止时间的记录，等于边界的记录保留；
  - [ ] 查询接口按当前租户过滤并增加独立权限；
  - [ ] 运行 Mapper XML、Recorder/Query/Retention 测试。
- **验收标准**：日志不含 Prompt/响应/API Key，分页与汇总租户隔离，保留清理只影响超期技术日志。

## Task 7: 在同步与流式调用链接入路由、健康和计量

- **目标**：让 call/stream 共用一次决策和一次审计，失败后绝不补发第二模型请求。
- **涉及文件**：
  - `client/AiClientImpl.java`；
  - `client/dto/AiClientResponse.java` — 可选增加 requestId，不删除现有字段；
  - `client/AiInvocationResolver.java`；
  - `client/AiClientImplTest.java`；
  - `client/AiClientRoutingGovernanceTest.java`。
- **关键签名**：
  ```java
  private ChatResponse executeCall(ChatClient chatClient, String systemPrompt, String userPrompt) { }

  private AiInvocationObservation buildObservation(
      String requestId, RouteDecision decision, ChatResponse response, long latencyMillis) { }
  ```
- **实施步骤**：
  - [ ] 先写 Fake ChatModel Red 测试，验证同步成功/失败各只调用一次；
  - [ ] 同步从 `.content()` 改用 `.chatResponse()`，统一提取 content 与 Usage；
  - [ ] 流式聚合最后一个有效 Usage，使用 AtomicBoolean 保证 doFinally/doOnError 只记录一次；
  - [ ] ERROR 更新健康失败，正常完成更新成功，CANCEL 不增加失败计数；
  - [ ] 在模型请求发出前维持 phase=PREPARATION；ChatModel/Cache/Adapter 创建失败调用 Lease.abort，发出请求前切换 dispatched=true；
  - [ ] 删除 systemPrompt/userPrompt/assistantContent 的 INFO 日志，改为 requestId、长度和路由元数据；
  - [ ] 验证业务异常发生在模型创建前时不写网络失败、不改变健康；
  - [ ] 运行 AiClient、Cache、Adapter、Session 全部相关测试。
- **验收标准**：同步/流式协议兼容，一次请求最多一次模型网络调用，一次最终审计记录。

## Task 8: 扩展 Agent 固定/策略选择配置

- **目标**：让 Agent 管理端明确选择固定模型或路由策略，避免字段同时生效产生歧义。
- **涉及文件**：
  - `agent/dto/AiAgentSaveDTO.java`、`agent/vo/AiAgentVO.java`；
  - `agent/mapper/AiAgentMapper.java/.xml`；
  - `agent/service/AiAgentService.java`、`agent/controller/AiAgentController.java`；
  - `agent/service/AiAgentServiceTest.java`；
  - `forge-admin-ui/src/views/ai/agent.vue`；
  - `forge-admin-ui/src/api/ai.js`。
- **实施步骤**：
  - [ ] Red 测试覆盖 PINNED provider 必填、POLICY routePolicy 必填、跨租户/停用策略拒绝；
  - [x] Service 归一化 null/blank→PINNED、未知非空值失败；PINNED 保存时强制 routePolicyId=null，POLICY 必须有有效策略；
  - [x] Agent 页面使用字典切换模式，PINNED 显示供应商/模型，POLICY 显示策略；
  - [x] 编辑历史 Agent 回显 PINNED，不自动改写已有 provider/model；
  - [x] POLICY 模式保存时不把 UI 隐藏字段作为运行时选择依据；
  - [ ] 执行目标 ESLint 和 Agent Service 测试。
- **验收标准**：用户能清楚知道 Agent 是固定还是路由，历史 Agent 行为不变。

## Task 9: 建设最小模型治理管理界面

- **目标**：提供必要配置和审计入口，不建设统计大屏。
- **涉及文件**：
  - `forge-admin-ui/src/views/ai/provider-model.vue` — 模型能力、上下文、价格配置；
  - `forge-admin-ui/src/views/ai/model-routing.vue` — “路由策略 / 调用记录”两个页签；
  - `forge-admin-ui/src/api/ai.js` — 策略、预览、日志、汇总 API；
  - `forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql` — 菜单与权限资源。
- **实施步骤**：
  - [x] provider-model 模型弹窗增加字典多选、上下文窗口和分/百万 Token 输入；
  - [x] model-routing 策略页支持名称、编码、所需能力、候选模型、priority 和启停；
  - [x] 提供“预览路由”按钮，只展示选择模型和跳过原因，不发模型请求；
  - [x] 调用记录页只显示安全元数据，支持时间、Agent、模型、结果筛选和分页；
  - [x] 页面采用现有企业配置页克制风格，不添加统计大屏、渐变、英文装饰或无价值动画；
  - [ ] 权限、字典、暗色主题、375/768/1024/1440 响应式和键盘焦点检查；
  - [ ] 使用 Node v20.19.0 执行目标 ESLint、生产构建和浏览器主路径验证。
- **验收标准**：完整配置无需手写 JSON，调用记录不泄露内容，页面无横向溢出。

## Task 10: 全量验证、战略回填与 Review 准备

- **目标**：形成可复跑证据，确保治理层没有破坏 Adapter 和现有 Agent。
- **涉及文件**：
  - 本变更 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`；
  - `docs/Forge-AI中枢战略与技术选型方案.md`；
  - 按实际发现更新 `code-copilot/memory/decisions.md` 或 `pitfalls.md`。
- **实施步骤**：
  - [x] 运行 Router/Health/Invocation/Agent/AiClient 定向测试，确认真实 Tests run；
  - [x] 运行 `forge-plugin-ai -Penable-tests -am test`，归档前基线 51 tests 必须继续通过；
  - [x] 运行 AI 插件 package 和 `forge-admin-server -am package -DskipTests`；
  - [x] 执行 V1.0.18 placeholder、防重复、tenantId、索引静态验证；
  - [ ] 在隔离数据库执行 V1.0.18 和 `forge_schema_history` 验证；
  - [x] 使用 Node v20.19.0 执行目标 ESLint 和前端 build；
  - [ ] 执行浏览器主路径与响应式人工验证；
  - [x] 扫描 Prompt/响应正文 INFO 日志、API Key、固定模型名和跨模型 retry 循环；
  - [x] `git diff --check`，回填实际命令、警告、跳过项和服务 PID；
  - [x] 对照 Spec 做 Compliance Review，再做 Code Quality Review；
  - [x] Review 阻断修复并复验后状态进入 done。
- **验收标准**：Spec、代码、测试与战略文档一致，无自动重试、无敏感内容日志、无 Nacos/MCP 范围漂移。

## Task 11: 归档与知识沉淀

- [x] 复核 Spec、Tasks、Test Spec、Execution Log、自动化测试标准和长期记忆；
- [x] 确认架构决策已沉淀到 `decisions.md` 第 17 条；
- [x] 确认路由 tenantId 与治理强制装配/失败分类踩坑已沉淀到 `pitfalls.md` 第 105、106 条；
- [x] 复用 Review Fix 的 84 tests、Admin 35 模块、前端 8487 modules 和静态检查成功证据；
- [x] 如实保留 Flyway 实库、真实供应商和浏览器人工验证条件项；
- [x] 状态更新为 `done`，归档到 `code-copilot/changes/archive/2026-07-11-ai-model-routing-governance/`。
