# 任务拆分 — 低代码应用集成规则引擎

> status: propose
> created: 2026-10-22（Asia/Shanghai）
> 当前仅 D00 文档工作已完成；所有 T 开头任务均未实施。

## 0. 执行规则与路径约定

先读 [spec.md](spec.md)、[test-spec.md](test-spec.md) 和 [execution-log.md](execution-log.md)。用户“生成吧”只授权文档，不是执行下面任务的授权。

- 顺序：T0 兼容门禁 → 数据/协议 → 运行底座 → 管理/适配/UI → T2 发布/分发 → 回归与用户验收。
- 每项应保持可独立审查的小提交，优先 3～5 个主要文件；必要测试与对应代码一起提交。若实际超过范围，先补子任务再编码，不堆进巨型文件。
- 下表为**精确、可展开的路径缩写**，不是现有模块已存在的声明。新增路径在实施时创建；标“已有”的路径须再次核对最新 main。
- 新增类/方法为拟定契约，不允许不创建类就直接调用。签名遵守 DTO 封装、最多 5 个参数，名称调整必须同步 Spec/Test Spec。

| 缩写 | 仓库根目录下的实际路径 |
|---|---|
| CHANGE | `code-copilot/changes/lowcode-rule-engine` |
| STARTER | `forge-server/forge-framework/forge-starter-parent/forge-starter-rule` |
| S | `forge-server/forge-framework/forge-starter-parent/forge-starter-rule/src/main/java/com/mdframe/forge/starter/rule` |
| SR | `forge-server/forge-framework/forge-starter-parent/forge-starter-rule/src/main/resources` |
| ST | `forge-server/forge-framework/forge-starter-parent/forge-starter-rule/src/test/java/com/mdframe/forge/starter/rule` |
| PLUGIN | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule` |
| P | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule/src/main/java/com/mdframe/forge/plugin/rule` |
| PR | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule/src/main/resources` |
| PT | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule/src/test/java/com/mdframe/forge/plugin/rule` |
| BRIDGE | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule-lowcode` |
| L | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule-lowcode/src/main/java/com/mdframe/forge/plugin/rule/lowcode` |
| LT | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule-lowcode/src/test/java/com/mdframe/forge/plugin/rule/lowcode` |
| G | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator` |
| GT | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator` |
| U | `forge-admin-ui/src` |

迁移文件以下按描述名定位，版本号**不预占**；实施前确定当时下一合法版本并把完整文件名回填。

## 1. 状态总览

- [x] D00：代码/官方资料分析，生成 Spec、Tasks、Test Spec 与执行记录；不编码、不升版。
- [ ] G1：用户批准开始 T0 与指定分支。
- [ ] T0-01～T0-02：兼容、装配与协议安全门禁。
- [ ] T1-01～T1-20：单机最小闭环，不作为生产发布完成。
- [ ] T2-01～T2-12：平台发布、分发、查询与生产前验收闭环。
- [ ] UAT-01：用户真实环境验收；未执行不能标通过。

本轮没有启动 T0。各任务关联的 R 编号对应 Spec §3，测试 C 编号对应 Test Spec。

## 2. T0：兼容与范围冻结

### T0-01：验证依赖与 Spring Boot 4 装配

- **状态**：未开始；依赖 G1。覆盖 R19/R21/R24。
- **文件**：`CHANGE/verification/compatibility/pom.xml`、`CHANGE/verification/compatibility/src/test/java/com/mdframe/forge/verification/LiteflowBoot4CompatibilityTest.java`、`CHANGE/compatibility-report.md`。
- **关键测试签名**：`startsOnForgeBoot4Baseline()`、`doesNotPullAgentOrOlderBootRuntime()`、`disabledOrAbsentEngineKeepsHostUsable()`。
- **工作**：锁定实际可解析的 core/Boot4/Rule-DB 一致版本；记录 dependency:tree 和有效 POM，核对 Jackson、日志、表达式、连接池；未授权前不改正式宿主 POM。
- **验收**：C01 通过；未安装、已装但关闭、连接异常均有实际启动证据；不把官网支持当 Forge 集成成功。

### T0-02：验证运行隔离、上下文与发布 API

- **状态**：未开始；依赖 T0-01。覆盖 R04/R09/R10/R12/R14/R24。
- **文件**：`CHANGE/verification/compatibility/src/test/java/com/mdframe/forge/verification/LiteflowIsolationTest.java`、`LiteflowRuleDbContractTest.java`（同目录）、`CHANGE/compatibility-report.md`、`CHANGE/test-spec.md`。
- **关键签名**：`draftCompileCannotReplacePublishedChain()`、`requestContextsDoNotLeak()`、`publishIsCasAndNotSemanticValidation()`。
- **验收**：C02/C03 有证据，明确全局注册表/临时 ID 清理、事务线程边界、第三方 DDL、Publisher DataSource 所有权与超时行为；没有真实数据库时分开标静态/Mock 和待实测项。
- **门禁**：T0 报告与 G3 人工审查后才能进入 T1；若版本或隔离不成立，提出修订，不擅自换引擎或降级 Boot。

## 3. T1：协议、数据与单机最小闭环

### T1-01：创建可选模块骨架和依赖方向

- **依赖**：T0、G3。覆盖 R19/R21。
- **文件**：`STARTER/pom.xml`、`PLUGIN/pom.xml`、`BRIDGE/pom.xml`、`forge-server/forge-framework/forge-dependencies/pom.xml`、`forge-server/forge-framework/forge-starter-parent/pom.xml`。
- **工作**：建立 starter → 基础能力、rule → starter、bridge → rule+generator 的单向依赖；仅在必要聚合/预设登记，插件父 POM 的登记若需第六文件拆子提交。
- **关键契约**：generator 不依赖规则插件；不把规则默认加入所有宿主；缺插件无新增 Bean/接口/启动失败。
- **验收**：C01/C32，原 Admin/Flow 装配不混入规则管理桥接。

### T1-02：冻结 Schema、字段与组件契约

- **文件**：`S/schema/RuleSchema.java`、`RuleNode.java`、`RuleEdge.java`、`RuleField.java`（均在 S/schema）、`S/spi/RuleNodeProvider.java`。
- **关键签名**：`RuleComponentDescriptor descriptor()`；`RuleNodeResult evaluate(RuleNodeInvocation invocation)`。
- **工作**：实现 Spec §5 的类型化结构，效果类型/协议版本、节点实例配置、字段敏感标记与安全限额；固定字段不使用整个 Map。
- **验收**：C04/C05/C06，Spec 的两个 JSON 样例可被明确的 DTO 接收，无隐式精度丢失。

### T1-03：注册器与发布校验器

- **文件**：`S/registry/RuleComponentRegistry.java`、`S/validation/RuleSchemaValidator.java`、`S/validation/RuleGraphValidator.java`、`S/validation/RuleValidationReport.java`。
- **关键签名**：`RuleValidationReport validate(RuleDraftValidationCommand command)`、`RuleComponentDescriptor require(RuleComponentKey key)`。
- **工作**：重复节点/未知类型、结构化图/可达/终端/环、路径/类型/效果白名单与资源限制；报告 nodeId/fieldPath/errorCode。
- **验收**：C04/C07/C08；不能通过删除校验或“默认出口”掩盖未知图。

### T1-04：规范化与 EL 编译

- **文件**：`S/compiler/RuleCanonicalizer.java`、`S/compiler/RuleElCompiler.java`、`S/compiler/RuleCompiledArtifact.java`、`ST/compiler/RuleElCompilerTest.java`。
- **关键签名**：`RuleCompiledArtifact compile(RuleCompilationRequest request)`、`String canonicalJson(RuleSchema schema)`。
- **工作**：稳定排序/hash、类型化 AST/结构化图到受控 EL、节点实例配置索引、引擎/组件清单；不允许提交任意 EL。
- **验收**：C06/C07/C09，同一规范配置稳定摘要，配置变化必改摘要；不支持图拒绝。

### T1-05：纯节点实现

- **文件**：`S/node/ConditionNodeProvider.java`、`DecisionTableNodeProvider.java`、`CalculateNodeProvider.java`、`OutputNodeProvider.java`（均在 S/node）。
- **关键签名**：统一 `RuleNodeResult evaluate(RuleNodeInvocation invocation)`。
- **工作**：布尔/空值/决策表首条命中、默认行、金额分/十进制舍入、溢出、输出 Schema；共享节点实现不存请求可变字段。
- **验收**：C05/C06/C10/C11；每个提供者的边界单测随对应文件提交，可按节点拆成四个原子提交。

### T1-06：运行底座、限流与试算隔离

- **文件**：`S/runtime/RuleRuntime.java`、`S/runtime/LiteflowRuleRuntime.java`、`S/runtime/RuleExecutionBudget.java`、`S/runtime/RuleContextScope.java`、`S/runtime/RulePreviewRegistry.java`。
- **关键签名**：`RuleExecutionResult execute(RuleRuntimeRequest request)`、`RuleTestResult preview(RulePreviewRequest request)`。
- **工作**：按 T0 结论封装 LiteFlow；有界资源/剩余预算、请求上下文清理、临时规则隔离与释放；不承诺中断可强杀任意 Java。
- **验收**：C02/C12/C13/C14；迟到结果不写终态，不覆盖正式规则或其他请求。

### T1-07：规则表、字典与权限迁移

- **文件**：`forge-server/db/migration/V<next>__add_rule_engine_control_tables.sql`、`V<next>__add_rule_engine_dict_and_permissions.sql`（同目录，分别取不同合法编号）、`P/enums/RuleRunStatus.java`、`P/enums/RuleReleaseState.java`。
- **工作**：Spec §6 五张 Forge 表、审计/删除/索引，发布状态和节点/效果/运行状态字典；EnableStatus 复用；SQL 不带真实业务样例。
- **验收**：C15；迁移重号/占位符检查、重复执行、实体列一致、tenant=1 种子。权限资源属于人工审查内容，不默认给全部角色。

### T1-08：定义与版本持久层

- **文件组 A**：`P/entity/RuleDefinition.java`、`P/mapper/RuleDefinitionMapper.java`、`PR/mapper/RuleDefinitionMapper.xml`。
- **签名**：`Page<RuleDefinition> selectPage(RulePageCriteria criteria, Page<?> page)`、`int updateDraftCas(RuleDraftUpdate command)`、`int logicalDelete(RuleDeleteCommand command)`。
- **文件组 B（独立子提交）**：`P/entity/RuleVersion.java`、`P/mapper/RuleVersionMapper.java`、`PR/mapper/RuleVersionMapper.xml`。
- **签名**：`RuleVersion selectExact(RuleVersionQuery query)`、`int insertImmutable(RuleVersion version)`。
- **验收**：C15/C16/C17；显式租户/删除条件，版本正文无更新路径，删除后不复用版本号。

### T1-09：发布记录与运行记录持久层

- **文件组 A**：`P/entity/RuleRelease.java`、`P/mapper/RuleReleaseMapper.java`、`PR/mapper/RuleReleaseMapper.xml`；签名 `int transitionCas(RuleReleaseTransition command)`。
- **文件组 B（独立子提交）**：`P/entity/RuleRun.java`、`P/mapper/RuleRunMapper.java`、`PR/mapper/RuleRunMapper.xml`；签名 `RuleRun reserve(RuleRunReservation command)`。
- **文件组 C（独立子提交）**：`P/entity/RuleNodeRun.java`、`P/mapper/RuleNodeRunMapper.java`、`PR/mapper/RuleNodeRunMapper.xml`；签名 `List<RuleNodeRun> selectTimeline(RuleRunQuery query)`。
- **验收**：C15/C18/C19，CAS 终态/租约、幂等指纹、密文结果过期、日志安全投影均有 Mapper 契约；逻辑唯一键与清理窗口一致。

### T1-10：应用所有权和可信调用端口

- **文件**：`P/spi/RuleApplicationAccessResolver.java`、`P/service/RuleScopePolicy.java`、`P/dto/TrustedRuleInvocation.java`、`L/security/LowcodeRuleApplicationAccessResolver.java`。
- **关键签名**：`RuleApplicationScope requireAccess(RuleApplicationAccessRequest request)`、`RuleInvocationScope resolve(TrustedRuleInvocation invocation)`。
- **工作**：桥接应用存在/状态/租户/访问权限；无桥接时应用规则管理失败关闭；访问规则与访问记录分别校验，不从客户端 DTO 构造信任。
- **验收**：C16/C20/C21；对象共享/同编码不同应用不串归属，无租户身份不默认管理员。

### T1-11：管理写 DTO 与定义服务

- **文件**：`P/dto/RuleDefinitionCreateDTO.java`、`RuleDefinitionUpdateDTO.java`、`RuleAvailabilityDTO.java`（均在 P/dto）、`P/service/RuleDefinitionService.java`、`P/service/impl/RuleDefinitionServiceImpl.java`。
- **关键签名**：`RuleDetailVO create(@Valid RuleDefinitionCreateDTO dto)`、`RuleDetailVO update(@Valid RuleDefinitionUpdateDTO dto)`、`void remove(RuleDeleteCommand command)`。
- **验收**：C16/C17/C20，字段注解+入口 Valid，CAS 冲突 409，唯一键/引用/启用属于业务校验，不把 DTO 非空全留 Service 手写。

### T1-12：管理查询、Controller 与日志投影

- **文件**：`P/controller/RuleDefinitionController.java`、`P/controller/RuleRunController.java`、`P/dto/RulePageQueryDTO.java`、`P/vo/RuleDetailVO.java`、`P/vo/RuleRunDetailVO.java`。
- **关键签名**：`RespInfo<Page<RuleSummaryVO>> page(@Valid RulePageQueryDTO query)`、`RespInfo<RuleRunDetailVO> detail(String runId)`。
- **工作**：Spec §8 接口/权限、pageNum/pageSize、加密、字符串 ID、运行安全字段；摘要 VO 可拆小提交，不把数据库实体直接返回 UI。
- **验收**：C20/C22/C23，不开放 chain/EL 执行接口，日志不返回密文结果/原始请求/凭据。

### T1-13：校验、草稿试算与单机版本服务

- **文件**：`P/controller/RuleDesignController.java`、`P/dto/RuleTestDTO.java`、`P/service/RuleDesignService.java`、`P/service/RuleVersionService.java`、`P/service/LocalRuleArtifactResolver.java`。
- **关键签名**：`RuleValidationReport validate(RuleValidateDTO dto)`、`RuleTestResultVO test(RuleTestDTO dto)`、`RuleVersionVO freeze(RuleVersionFreezeCommand command)`。
- **工作**：T1 模式 LOCAL_PREVIEW、只执行 PURE、版本从不可变控制面恢复，试算不冒充正式发布/所有节点 ready；现有动作 preview 的“仅展示计划”语义不变。
- **验收**：C09/C12/C17/C24；同名草稿不覆盖发布版本，试算不写业务数据。

### T1-14：运行治理、幂等结果与安全追踪

- **文件**：`P/service/RuleInvocationService.java`、`P/service/RuleRunReservationService.java`、`P/service/RuleResultSnapshotService.java`、`P/service/RuleAuditService.java`。
- **关键签名**：`RuleExecutionResult evaluate(TrustedRuleInvocation invocation)`、`RuleRunReservation reserve(RuleRunReservationCommand command)`、`RuleExecutionResult reuse(RuleResultReuseCommand command)`。
- **工作**：现有平台加密能力保护结果快照；明确 retention、幂等窗口、超时迟到、父 trace 和节点实例结果。不把摘要当真实可重放结果。
- **验收**：C18/C19/C21/C23/C25，结果过期不重新计算冒充幂等命中。

### T1-15：新增中性步骤策略 SPI（先拆现有执行领域）

- **文件**：`G/service/businessapp/BusinessActionStepPolicyContributor.java`、`BusinessActionStepPolicyRegistry.java`（同目录）、已有 `G/service/businessapp/BusinessActionExecutionService.java`、已有 `G/service/businessapp/BusinessActionCommandPolicy.java`。
- **关键签名**：`StepPolicyDescriptor describe(BusinessActionStepValidationRequest request)`、`void validateRegisteredSteps(BusinessActionDefinitionValidation request)`。
- **工作**：将新的注册/策略协调放入独立服务，保持既有类型策略不变；纯策略仍无数据库/Session。现有执行类不超过上限，必要拆分另做子任务，不能长到 1000+。
- **验收**：C26/C27；无贡献器的未知步骤发布和运行均拒绝；显式 LOCAL_TRANSACTION 中规则步骤拒绝，不能放宽整个动作白名单。

### T1-16：补齐可信应用调用上下文

- **依赖**：T1-10/T1-15；先于规则步骤适配器。覆盖 R01/R06/R08/R11/R21。
- **文件组 A**：已有 `G/dto/businessapp/BusinessActionExecuteDTO.java`、`G/service/businessapp/BusinessActionExecutionContext.java`、新增 `G/service/businessapp/BusinessActionApplicationScopeResolver.java`、`L/action/RuleActionCallerScopePolicy.java`。
- **关键签名**：`BusinessActionApplicationScope resolve(BusinessActionApplicationScopeRequest request)`；兼容旧 DTO，应用定位字段不直接成为可信权限。
- **文件组 B（独立子提交）**：已有 `U/components/ai-form/business-action-runtime.js`、`U/components/ai-form/__tests__/business-action-runtime.spec.js`（不存在时新增）、已有 `G/service/businessapp/BusinessTriggerExecutor.java`、新增 `L/trigger/RuleTriggerScopeResolver.java`。
- **工作**：页面从应用运行快照传定位、后端查发布态投影验证；事件/定时从固定绑定读取应用版本。若 CRUD/Trigger 巨型文件需增长先拆调用上下文领域，不向超限文件堆逻辑。
- **验收**：C16/C21/C26/C39；共享对象不猜当前应用、无 scope 的规则动作拒绝，无规则旧请求不受影响；绑定缺应用版本必须显式补齐再发布。

### T1-17：规则动作适配器

- **文件**：`L/action/ExecuteRuleStepConfig.java`、`L/action/ExecuteRuleActionStepExecutor.java`、`L/action/ExecuteRuleStepPolicyContributor.java`、`LT/action/ExecuteRuleActionStepExecutorTest.java`。
- **关键签名**：实现既有 `supportType()` / `execute(BusinessActionExecutionContext context, BusinessActionStepDTO step)`，以及 T1-15 新策略 SPI。
- **工作**：固定规则版本、声明输入/结果映射、可信身份、guard、父子幂等、原子合入结果；不直接写业务字段。T1 仅隔离集成试用，正式应用发布加 T2 未就绪阻断。
- **验收**：C21/C26/C28/C29；false 输出不默认为失败；guard 拒绝不执行后续步骤；现有开放平台白名单继续拒绝此新步骤。

### T1-18：前端 API 与领域 Store

- **文件**：`U/api/rule.js`、`U/stores/rule/ruleDesignerStore.js`、`U/views/app-center/rules/composables/useRuleCatalog.js`、`U/views/app-center/rules/utils/ruleSchema.js`。
- **关键签名**：`useRuleDesignerStore()`、`loadRule(id)`、`saveDraft()`、`validateDraft()`、`testDraft(arguments)`。
- **工作**：系统统一请求/加密，Long 字符串、草稿 CAS、脏状态、请求竞态、错误重试；多面板共享状态不多层传 props。
- **验收**：C30/C31/C33，所有页面动作只调用真实后端，空态/错误/无匹配分开。

### T1-19：应用内列表与结构化编辑工作台

- **文件组 A**：`U/views/app-center/rules/RuleCatalog.vue`、`RuleToolbar.vue`、`RuleAssetCard.vue`（同目录）。
- **文件组 B（独立子提交）**：`U/views/app-center/rules/RuleWorkbench.vue`、`panels/RuleSchemaPanel.vue`、`RuleNodePanel.vue`、`RuleTestPanel.vue`（后三者位于同 panels 目录）。
- **工作**：T1 结构化编辑条件/决策表/计算/输出，显式 LOCAL_PREVIEW；组件遵循 DESIGN.md，借用既有公共骨架，不伪造后台操作。
- **验收**：C30/C31/C33；每个 SFC 常规 <1000 行，亮/暗/窄屏、独立滚动、中文可访问标签。

### T1-20：可选 UI 入口和安装资源

- **文件**：已有 `U/views/app-center/application-workspace/ApplicationWorkspaceNav.vue`、`L/config/RuleLowcodeAutoConfiguration.java`、`BRIDGE/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`、插件安装清单（T0/T1-01 核对后的真实路径回填）。
- **工作**：通过可选扩展槽注册应用规则入口，避免硬编码永远显示不存在功能；菜单/API 资源复用 T1-07 正式迁移。无规则插件时 UI 不出现不可用入口。
- **验收**：C01/C30/C32；不新增默认管理端插件安装/授权购买流程，不自动修改所有宿主 POM。

## 4. T2：发布、固定版本与分发闭环

### T2-01：Rule-DB 适配及原生 DDL

- **文件**：`P/config/RuleDbConfiguration.java`、`P/publisher/RuleDbArtifactPublisher.java`、`P/runtime/RuleDbArtifactResolver.java`、`forge-server/db/migration/V<next>__add_rule_engine_runtime_store.sql`。
- **关键签名**：`RuleArtifactWriteResult publish(RuleArtifactWriteCommand command)`、`RuleArtifact resolveExact(RuleArtifactKey key)`。
- **工作**：使用 T0 锁定 API/DDL；平台元数据源、运行读/发布写、auto-init=false；内部键含安装/租户/应用/不可变版本；只用 RulePublisher。
- **验收**：C03/C15/C34，多数据源切换不改变规则权威源，不把 MyBatis 租户插件当作 Rule-DB 防线。

### T2-02：正式发布协调、租约和恢复

- **文件**：`P/service/RulePublishService.java`、`P/service/RulePublishRecoveryService.java`、`P/controller/RulePublishController.java`、`P/dto/RulePublishDTO.java`、`PT/service/RulePublishRecoveryTest.java`。
- **关键签名**：`RulePublishResultVO publish(RulePublishDTO dto)`、`RulePublishResultVO resume(RuleReleaseResumeCommand command)`。
- **工作**：PREPARING→WRITING→VERIFYING→AVAILABLE/FAILED；短事务/事务外发布，CAS、同 hash 幂等、写超时核对、崩溃恢复；不覆盖原版本。
- **验收**：C18/C35/C36，依赖全写入并校验前不能激活引用，失败恢复不读新草稿。

### T2-03：版本闭包和逐节点加载验证

- **文件**：`P/dependency/RuleDependencyResolver.java`、`P/dependency/RuleDependencyValidator.java`、`P/runtime/RuleVersionReadinessService.java`、`PT/dependency/RulePinnedVersionTest.java`。
- **关键签名**：`RuleDependencyManifest resolve(RuleDependencyRequest request)`、`RuleArtifact requireExact(RuleInvocationScope scope)`。
- **工作**：CALL_RULE 版本/环/深度，组件协议/hash，全节点无强一致假设；冷加载有界，缺失/滞后/不兼容明确 RULE_NOT_READY，不回退旧链。
- **验收**：C07/C34/C36/C37；v1/v2 并发、缓存淘汰、实例重启、依赖不同步均不串版。

### T2-04：新增中性应用发布资产贡献器

- **文件**：`G/service/businessapp/BusinessApplicationAssetContributor.java`、`BusinessApplicationAssetContributionRegistry.java`（同目录）、已有 `BusinessApplicationSnapshotService.java`、已有 `BusinessApplicationPublishService.java`（后二者也在同目录）。
- **关键签名**：`ApplicationAssetContribution capture(ApplicationAssetCaptureRequest request)`、`ApplicationAssetValidation validate(ApplicationAssetValidationRequest request)`、`ApplicationAssetPublishResult prepare(ApplicationAssetPublishRequest request)`。
- **工作**：所有入参用中性 DTO；generator 不导入规则插件类；空贡献不改变无规则快照；任务恢复固定原始选中资产，幂等不重复业务写入。
- **验收**：C27/C38；现有 printing/process/object/entry 发布与无规则应用 hash/行为不倒退。

### T2-05：应用规则快照与动作版本投影

- **文件**：`L/publish/RuleApplicationAssetContributor.java`、`L/publish/RuleActionVersionProjection.java`、已有 `G/service/businessapp/BusinessObjectActionService.java`、`LT/publish/RuleApplicationSnapshotTest.java`。
- **关键签名**：`RuleApplicationManifest capture(RuleApplicationCaptureCommand command)`、`RuleActionReference resolvePinned(RuleActionResolveCommand command)`。
- **工作**：正式应用发布必须有完整版本闭包；对象动作引用与应用快照一致；老应用固定 v1，新应用 v2；恢复不重新选最新。
- **验收**：C21/C37/C38/C39；去掉 T1 的正式发布阻断只能在完整校验通过后发生，不使用 latest fallback。

### T2-06：受控只读查询节点与子规则

- **文件**：`L/query/LowcodeRuleQueryProvider.java`、`S/node/QuerySourceNodeProvider.java`、`S/node/CallRuleNodeProvider.java`、`LT/query/LowcodeRuleQueryProviderTest.java`。
- **关键签名**：`RuleQueryResult query(RuleReadQueryRequest request)`；实现节点提供者端口。
- **工作**：使用已发布查询源+字段权限/行级数据范围，读效果由服务端判定；限制 100 行及剩余预算，不支持自由 URL/SQL/EXTERNAL_API 写路径；子规则按 T2-03 固定闭包调用。
- **验收**：C11/C13/C20/C40；无权字段/跨应用/停用源/超量数据失败关闭，不绕过平台查询服务直连业务表。

### T2-07：应用包扩展 SPI 与规则包实现

- **文件组 A**：`G/service/businessapp/BusinessApplicationBundleContributor.java`、已有 `BusinessApplicationBundleExportService.java`、已有 `BusinessApplicationBundleImportService.java`（三者同目录）。
- **文件组 B（独立子提交）**：`L/bundle/RuleApplicationBundleContributor.java`、`L/bundle/RuleBundleValidator.java`、`LT/bundle/RuleApplicationBundleTest.java`。
- **关键签名**：`ApplicationBundleContribution exportAssets(ApplicationBundleRequest request)`、`ApplicationBundleImportPlan validateImport(ApplicationBundleImportRequest request)`、`ApplicationBundleImportResult importAssets(ApplicationBundleImportCommand command)`。
- **验收**：C41/C42；剥离内部 chain/租户/凭据，校验再重映射/编译，缺组件明确阻断，商店与手动包使用同一入口。

### T2-08：静态源码统一编译贡献器

- **文件**：`L/codegen/RuleStaticCodegenContributor.java`、`L/codegen/RuleGeneratedProjectManifest.java`、`BRIDGE/src/main/resources/templates/rule/RuleInvocationSupport.java.vm`、`LT/codegen/RuleStaticCodegenTest.java`。
- **关键签名**：实现已有 `LowcodeStaticCodegenContributor#contributeContext/contributeFiles`，构建依赖若需扩展另补中性依赖贡献端口与子任务，不能伪称现有 add-files 钩子能改宿主 POM。
- **工作**：规则资源、固定依赖、普通 Java 调用、工程落位/构建说明、不覆盖业务扩展；不支持模式有显式错误。
- **验收**：C43/C44；至少一个新目录生成工程实际编译并执行规则输出，不能只断言 ZIP 内有 JSON。

### T2-09：引用保护、停用与生命周期

- **文件**：`P/service/RuleReferenceService.java`、`P/service/RuleLifecycleService.java`、`P/dto/RuleDefaultVersionDTO.java`、`L/reference/LowcodeRuleReferenceResolver.java`、`PT/service/RuleLifecycleTest.java`。
- **关键签名**：`RuleImpactVO inspect(RuleImpactQuery query)`、`void changeAvailability(RuleAvailabilityDTO dto)`、`void selectDefault(RuleDefaultVersionDTO dto)`。
- **工作**：展示引用影响、确认摘要防过期、停用拒绝新调用、活跃引用禁止删/卸载；规则默认指针与应用版本回退分开。
- **验收**：C17/C45/C46；单次停用不删除历史、不自动改已发布应用，旧版本号永不复用。

### T2-10：可视化与发布/运行完整反馈

- **文件组 A**：`U/views/app-center/rules/RuleCanvas.vue`、`panels/RuleDecisionTablePanel.vue`、`U/stores/rule/ruleDesignerStore.js`、`U/views/app-center/rules/utils/ruleGraphAdapter.js`。
- **文件组 B（独立子提交）**：`U/views/app-center/rules/panels/RuleVersionsPanel.vue`、`RuleReleasePanel.vue`、`RuleRunPanel.vue`（同目录）、`U/views/app-center/rules/RuleWorkbench.vue`。
- **工作**：复用 FlowCanvas/系统组件，JSON 唯一编辑源，展示真实依赖/发布状态/实例采集信息；不把 AVAILABLE 描述成“全部节点同步”。
- **验收**：C30/C31/C33/C35，草稿/试算/发布/运行各自状态与错误，可取消交互不写草稿。

### T2-11：宿主选择、健康与观测

- **文件**：`P/config/RulePluginAutoConfiguration.java`、`P/health/RuleEngineHealthContributor.java`、`P/metrics/RuleMetrics.java`、`BRIDGE/src/main/resources/forge-plugin.json`（实际插件描述文件名按 T1-01 已核对规范确定后回填）。
- **工作**：正确控制 LiteFlow 自动装配、读存储失败只阻断规则引用、指标维度低基数；不公开 actuator 不改授权；安装/卸载引用检查与 T2-09 联动。
- **验收**：C01/C14/C32/C34/C47，不能仅加 Forge 开关却仍让第三方无条件初始化。

### T2-12：安全审查、增量回归与交付记录

- **文件**：`CHANGE/spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`（后三者同目录），需要时新增独立 report.md。
- **工作**：复用阶段基线，执行相关 Maven 单测/编译/宿主装配、前端测试/lint/build、Mapper/SQL/MySQL、真实组件 UI；记录失败/跳过/服务 PID 清理。
- **验收**：C01～C47 可追溯，R01～R24 无未声明缺口；无真实验证证据的项目保留 pending。不自动升版、打标签、提交/push或部署。

## 5. 用户环境验收及后续边界

### UAT-01：真实环境验证（单独授权后）

- **前置**：T2-12 完成，用户明确指定环境、库、运行范围与允许的写入。
- **文件**：`CHANGE/test-spec.md`、`execution-log.md`、`uat-report.md`（后三者均为文档）。
- **内容**：MySQL 新/旧库与 Flyway、真实加密/普通权限、至少两执行实例、缓存失效/重启、应用发布回退、生成工程运行、既有审批回归。
- **验收**：结果/日志/SQL/实例与实际版本对应；测试记录只使用合成或已授权测试数据。未授权不自行操作部署或实际业务。

### T3：不自动扩展的后续选项

- 应用业务流程 ACTION/RULE 节点调用：复用同一服务，不先接全画布编译；评估并另修复当前条件默认出口/未实现子流程。
- Flowable Service Task：通过受控服务契约桥接，不让独立 Flow 服务依赖 generator 管理端；审批回调保持已有业务幂等。
- 前置全局保存校验、跨应用共享规则、复杂并行、可控写节点、脚本沙箱、AI 工具开放：均需独立 Spec/安全评审和用户确认。
- **不会**因“继续”或“功能完善”就自动把这些增强加入首版验收。
