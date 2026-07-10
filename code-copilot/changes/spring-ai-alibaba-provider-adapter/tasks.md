# 任务拆分 — Spring AI Alibaba 供应商适配层与 DashScope 原生接入

> 拆分顺序：依赖基线 → 数据模型 → 适配器底层 → 调用编排 → 供应商入口 → 前端 → 验证
> 每个任务为可独立审查的原子变更；执行时遵循 Red/Green TDD，并在每个 Task 后更新 `execution-log.md`。

## 前置条件

- [x] `spec.md` 已完成 HARD-GATE，确认人和时间已回填；
- [x] 当前分支不是受保护的 `master`，且保留用户已有工作区改动；
- [x] Maven 能访问 Spring AI Alibaba `1.1.2.3` release 产物，或开发机已临时安装相同 release；
- [x] 已读取本变更 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` 和 `code-copilot/rules/automated-testing-standard.md`；
- [x] 已记录目标模块现有测试基线，不提交任何真实 API Key。

## Task 1: 统一 Spring AI Alibaba 依赖基线

- **目标**：通过 BOM 固定 Spring AI `1.1.2` 与 Alibaba/Extensions `1.1.2.3`，只引入 DashScope 核心模型模块。
- **涉及文件**：
  - `forge-server/pom.xml` — 更新 Alibaba 版本、增加 Extensions 版本并导入三个 BOM；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/pom.xml` — 移除 Spring AI 依赖显式版本，增加 `spring-ai-alibaba-dashscope`；
  - `code-copilot/changes/spring-ai-alibaba-provider-adapter/execution-log.md` — 记录依赖树证据。
- **关键约束**：
  ```xml
  <spring-ai.version>1.1.2</spring-ai.version>
  <spring-ai-alibaba.version>1.1.2.3</spring-ai-alibaba.version>
  <spring-ai-alibaba-extensions.version>1.1.2.3</spring-ai-alibaba-extensions.version>
  ```
- **实施步骤**：
  - [x] 运行当前 AI 插件 `dependency:tree` 并保存变更前 Spring AI 依赖基线；
  - [x] 在根 `dependencyManagement` 依次导入 `spring-ai-bom`、`spring-ai-alibaba-extensions-bom`、`spring-ai-alibaba-bom`；
  - [x] AI 插件增加 `com.alibaba.cloud.ai:spring-ai-alibaba-dashscope`，确认没有 `spring-ai-alibaba-starter-dashscope`；
  - [x] 运行变更后 `dependency:tree`，确认 `org.springframework.ai:*` 统一为 `1.1.2`、`com.alibaba.cloud.ai:*` 统一为 `1.1.2.3`；
  - [x] 编译 AI 插件，若 Maven 发布仓库不可达，记录阻断证据，不把 vendor 源码加入工程模块。
- **验收标准**：依赖树无 Spring AI 多版本，Boot 保持 `3.5.13`，classpath 不包含 DashScope 自动配置 Starter。

## Task 2: 增加适配器数据契约和字典

- **目标**：用显式字段保存连接协议，历史供应商无行为变化。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.17__add_ai_provider_adapter_code.sql` — 新增字段、回填和字典；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/domain/AiProvider.java` — 新增 `adapterCode`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderAdapterCode.java` — 稳定适配器代码常量/枚举。
- **关键签名**：
  ```java
  public enum AiProviderAdapterCode {
      OPENAI_COMPATIBLE("openai_compatible"),
      DASHSCOPE_NATIVE("dashscope_native");

      public String getCode();
      public static AiProviderAdapterCode require(String code);
  }
  ```
- **实施步骤**：
  - [x] 先编写 `AiProviderAdapterCodeTest`，验证两个合法值以及 null/blank/unknown 失败关闭；
  - [x] 新增 Flyway 字段检查，字段不存在时添加 `varchar(32) NOT NULL DEFAULT 'openai_compatible'`；
  - [x] 字段首次新增时由默认值覆盖历史记录；若字段已存在，只更新 `adapter_code IS NULL OR TRIM(adapter_code)=''`，不得覆盖任何非空值；
  - [x] 使用 `NOT EXISTS` 写入 `ai_provider_adapter_type` 及两条 tenant_id=`1` 字典数据；
  - [x] 实体显式映射 `adapterCode`，默认值只在服务创建链路和数据库中定义，不依赖前端；
  - [x] 运行 Flyway placeholder 静态扫描和 SQL 防重复检查。
- **验收标准**：首次迁移的旧记录均为 Compatible；脚本重跑不覆盖预置的 `dashscope_native`；新字典可由 `useDict` 获取；未知代码抛业务异常。

## Task 3: 建立 Provider Adapter SPI

- **目标**：把具体模型创建封装为可注册、可测试、失败关闭的适配器。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/AiModelRuntimeOptions.java` — 通用 Chat 运行参数；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderAdapter.java` — Adapter SPI；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderAdapterRegistry.java` — 注册表和唯一性校验；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderBaseUrlPolicy.java` — URI 归一化与 Compatible/Native 双向校验；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/OpenAiCompatibleProviderAdapter.java` — 现有模型实现迁移；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/DashScopeNativeProviderAdapter.java` — 原生 DashScope 模型实现。
- **关键签名**：
  ```java
  public record AiModelRuntimeOptions(String model, Double temperature, Integer maxTokens) {
      public String cacheKeyFragment();
  }

  public interface AiProviderAdapter {
      String adapterCode();
      void validate(AiProvider provider, AiModelRuntimeOptions options);
      ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options);
  }

  public final class AiProviderAdapterRegistry {
      public AiProviderAdapterRegistry(List<AiProviderAdapter> adapters);
      public AiProviderAdapter getRequired(String adapterCode);
      public ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options);
  }

  public final class AiProviderBaseUrlPolicy {
      public static String normalizeAndValidate(String adapterCode, String baseUrl);
  }
  ```
- **实施步骤**：
  - [x] 先写 Registry Red 测试：合法路由、未知值、空值、重复 code，以及 `createChatModel` 的选择→校验→创建顺序；
  - [x] `AiProviderAdapterRegistry#createChatModel` 统一执行 `getRequired → validate → adapter.createChatModel`；校验失败时不得调用创建方法；
  - [x] 正式调用、连接测试和 Cache 只依赖 Registry 创建入口，不直接注入具体 Adapter；
  - [x] 实现 Registry，错误使用 `BusinessException` 且不包含 API Key；
  - [x] 先写 Base URL Policy Red 测试，覆盖 scheme/query/fragment/userInfo、尾斜杠、官方 DashScope 双向错配和自定义代理域名；
  - [x] Compatible 在官方 DashScope 域名只接受 `/compatible-mode`；Native 只接受根路径或空值默认；自定义域名只校验不改写；
  - [x] 先写 OpenAI Adapter Red 测试，覆盖模型/温度/最大 Token 和 Base URL 必填；
  - [x] 将当前 `OpenAiApi/OpenAiChatModel` 构造迁入 OpenAI Adapter；
  - [x] 先写 DashScope Adapter Red 测试，覆盖默认 Base URL、原生 URL 校验和 `maxToken` 映射；
  - [x] 使用 `DashScopeApi.builder().apiKey().baseUrl().build()` 与 `DashScopeChatModel.builder()` 实现原生 Adapter；
  - [x] 验证返回模型的默认 Options 为 `DashScopeChatOptions`，且实现 `ToolCallingChatOptions`。
- **验收标准**：新增供应商只需实现 SPI 并注册 Bean；调用层不需要新增 `if/else`；错误适配器不产生网络请求。

## Task 4: 解耦 AiClient 与 ChatClientCache

- **目标**：统一调用链只依赖通用运行参数和 Adapter Registry，保持 `AiClient` 对外协议不变。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java` — 用 `AiModelRuntimeOptions` 替换 `OpenAiChatOptions`；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/ChatClientCache.java` — 委托 Registry 创建 `ChatModel`，强化租户缓存键；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiInvocationResolver.java` — 保留选择优先级，将供应商特定校验下沉 Adapter；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/support/AiProviderCacheEvictionScheduler.java` — 事务提交后缓存失效调度；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/client/AiInvocationResolverTest.java` — 保留并扩展现有回归用例；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/client/ChatClientCacheTest.java` — 新增缓存复用与失效测试。
- **关键签名**：
  ```java
  public ChatClient getOrCreateBase(
      AiProvider provider,
      AiModelRuntimeOptions options
  );

  public void evictByProvider(Long tenantId, Long providerId);

  public void scheduleAfterCommit(AiProvider provider);
  ```
- **实施步骤**：
  - [x] 先写 Cache Red 测试，证明同配置复用、不同租户/Adapter/参数不碰撞、主动失效后重新创建；
  - [x] `getOrCreateBase` 从 `provider.getTenantId()` 构造缓存键，不接受独立 tenantId 参数；缓存键只含 tenantId、providerId、adapterCode、model、temperature、maxTokens，不含明文密钥；
  - [x] 先写 Eviction Scheduler Red 测试：事务提交前不清理、afterCommit 清理、回滚不清理、无活动事务立即清理；
  - [x] 使用 `TransactionSynchronizationManager` 注册 after-commit 回调，Scheduler 从 `AiProvider` 派生 tenantId/providerId；
  - [x] `AiClientImpl.call/stream` 共用同一 `AiModelRuntimeOptions` 构建方法；
  - [x] 删除 `AiClientImpl`、`ChatClientCache` 中所有 `org.springframework.ai.openai.*` import；
  - [x] 保持 `MessageChatMemoryAdvisor`、同步输出、流式输出和会话持久化逻辑不变；
  - [x] 扩展 Resolver 测试，确保显式参数、Agent 参数和供应商默认值优先级未改变。
- **验收标准**：`AiClient` 接口零变更；OpenAI Compatible 回归通过；DashScope 模型可以进入相同 ChatClient/Memory/Stream 链路。

## Task 5: 收口供应商生命周期、连接测试和密钥保护

- **目标**：创建、更新、删除、测试和返回视图统一经过安全服务边界，并正确清理缓存。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/dto/AiProviderSaveDTO.java` — 新增/修改请求；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/dto/AiProviderTestDTO.java` — ID 或未保存完整配置测试请求；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/vo/AiProviderVO.java` — 安全响应对象；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/support/AiProviderSecretMasker.java` — 脱敏和同值判断；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderService.java` — 生命周期、Adapter 测试、密钥保留、缓存失效；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/controller/AiProviderController.java` — DTO/VO 协议和模板调整。
- **关键签名**：
  ```java
  public final class AiProviderSecretMasker {
      public static String mask(String secret);
      public static boolean isUnchangedMask(String submitted, String persisted);
  }

  public void createProvider(AiProviderSaveDTO request);
  public void updateProvider(AiProviderSaveDTO request);
  public AiProviderVO toSafeView(AiProvider provider);
  public String testConnection(AiProviderTestDTO request);
  public void deleteProvider(Long id);
  ```
- **实施步骤**：
  - [x] 先写 SecretMasker Red 测试：长密钥、短密钥、空值、未变化脱敏值和新明文；
  - [x] 先写 Service Red 测试：更新脱敏值保留原密钥、更新明文替换、测试 ID 加载真实密钥、one-of 混合请求拒绝、SDK 敏感异常安全化、持久化成功后调度缓存失效；
  - [x] 页面/详情改为 `AiProviderVO`，禁止实体原样返回；
  - [x] DTO 的 Adapter 缺失/null 时：新增默认为 `openai_compatible`、更新保留持久化值；显式空字符串/纯空白一律拒绝；
  - [x] 更新前加载当前租户实体并执行脱敏回写保护；
  - [x] `AiProviderTestDTO` 执行严格 one-of：有 ID 时拒绝任何配置字段，无 ID 时要求 Adapter 所需完整配置，禁止数据库配置与请求字段合并；
  - [x] 连接测试通过 Registry 创建 `ChatModel`，固定低 Token Prompt；SDK 异常响应使用固定安全文案，日志仅含 providerId、adapterCode、异常类型；
  - [x] 新增、更新和测试接口保持 `@ApiDecrypt/@ApiEncrypt`；
  - [x] 修改、删除持久化成功后调用 `AiProviderCacheEvictionScheduler.scheduleAfterCommit(provider)`；失败或回滚不得清除缓存；
  - [x] `/templates` 为 DashScope Native 返回 `adapterCode=dashscope_native` 和原生 Base URL，同时保留 Compatible 选择能力。
- **验收标准**：浏览器无法获得真实 API Key；保存未改密钥不会覆盖；连接测试不依赖客户端回传已保存密钥；更新后下一次调用使用新配置。

## Task 6: 改造当前供应商管理页面

- **目标**：让管理员明确选择连接协议，并以安全方式测试已保存供应商。
- **涉及文件**：
  - `forge-admin-ui/src/views/ai/provider-model.vue` — 字典、表单、表格、默认值、URL 联动和测试参数；
  - `forge-admin-ui/src/api/ai.js` — 补充测试请求语义注释，接口路径保持不变。
- **关键状态**：
  ```js
  const { dict } = useDict(
    'ai_provider_type',
    'ai_provider_adapter_type',
    'ai_model_type',
    'ai_status',
    'ai_is_default',
  )

  const providerModal = reactive({
    form: {
      adapterCode: 'openai_compatible',
    },
  })
  ```
- **实施步骤**：
  - [x] 在供应商表格和弹窗增加“连接协议”，选项必须来自字典；
  - [x] 新增供应商默认 `openai_compatible`，后端仍提供相同默认兜底；
  - [x] 选择 `dashscope_native` 时，仅在 Base URL 为空或为系统已知 Compatible 默认值时替换为 `https://dashscope.aliyuncs.com`，不得覆盖用户自定义域名；
  - [x] 切回 Compatible 不自动猜测第三方 URL，要求用户明确填写；
  - [x] 已保存供应商的“测试连接”仅提交 `{ id: row.id }`；
  - [x] `providerAdd/providerTest` 使用 `postEncrypt`；`providerUpdate` 使用带 `encrypt: true` 的 PUT 请求，确保敏感配置不会明文发送；
  - [x] API Key 脱敏值可以回显但不能被前端自行还原；后端负责未变化值保留；
  - [x] 不修改未绑定菜单的旧 `provider.vue`，并确认后端“新增 null 则 Compatible、更新 null 则保留原值、blank 拒绝”的规则保证旧请求兼容。
- **验收标准**：活动菜单页面可创建两种 Adapter；切换不误覆盖自定义 URL；测试请求 Network Payload 不含真实或脱敏 API Key。

## Task 7: 完成自动化和装配验证

- **目标**：按最小风险矩阵验证后端、Flyway、前端和可选真实 DashScope 链路。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderAdapterCodeTest.java` — Adapter Code 单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderAdapterRegistryTest.java` — Registry 单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/adapter/AiProviderBaseUrlPolicyTest.java` — URL Policy 单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/adapter/OpenAiCompatibleProviderAdapterTest.java` — Compatible Adapter 单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/adapter/DashScopeNativeProviderAdapterTest.java` — DashScope Adapter 单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/client/ChatClientCacheTest.java` — 缓存单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/client/AiClientImplTest.java` — Native 供应商同步、流式和 reasoningContent 离线回归；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/support/AiProviderSecretMaskerTest.java` — 密钥脱敏单测；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/support/AiProviderCacheEvictionSchedulerTest.java` — after-commit/rollback 缓存失效测试；
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderServiceTest.java` — 生命周期与密钥单测；
  - `code-copilot/changes/spring-ai-alibaba-provider-adapter/test-spec.md` — 回填本轮增量；
  - `code-copilot/changes/spring-ai-alibaba-provider-adapter/execution-log.md` — 记录真实命令和结果。
- **实施步骤**：
  - [ ] 执行 P0 目标测试，保留 Red/Green 证据；
  - [ ] 强制执行 `AiClientImplTest`，离线证明 Native 供应商的同步、流式与 `reasoningContent` 进入统一调用链；不得用“可选公网测试”替代；
  - [ ] 执行 AI 插件 `-Penable-tests test`；
  - [ ] 执行 AI 插件及依赖模块 compile/package；
  - [ ] 执行 `forge-admin-server -am package -DskipTests`，验证主应用装配；
  - [ ] 执行 Flyway placeholder、字段/字典防重复静态检查；有 dev 库时验证 `forge_schema_history` 和回填结果；
  - [ ] 使用 Node `v20.19.0` 执行前端 build；
  - [ ] 可选：人工提供 `AI_DASHSCOPE_API_KEY` 后验证一次同步和流式 `qwen-plus`，未提供时明确记录跳过原因；
  - [ ] 记录所有警告、跳过项、服务 PID 和清理结果。
- **验收标准**：所有必跑项有可复跑证据；没有把网络/密钥缺失写成通过；没有新增明文密钥或未停止的测试服务。

## Task 8: 回填文档与阶段结论

- **目标**：让 AI 中枢路线图与实际依赖决策保持一致。
- **涉及文件**：
  - `docs/Forge-AI中枢落地架构与阶段路线图.md` — 回填阶段 0 的版本和 Adapter 结论；
  - `docs/Forge-AI中枢战略与技术选型方案.md` — 明确 Alibaba 是 Spring AI 增强层而非替换；
  - `code-copilot/changes/spring-ai-alibaba-provider-adapter/spec.md` — 更新执行日志和审查结论；
  - `code-copilot/changes/spring-ai-alibaba-provider-adapter/execution-log.md` — 记录收尾验证；
  - `code-copilot/memory/decisions.md` 或 `pitfalls.md` — 仅沉淀经过实现验证的长期决策/踩坑。
- **实施步骤**：
  - [ ] 根据实际 dependency tree 和测试结果更新路线图，不把计划写成已完成；
  - [ ] 在发布/回退说明中加入 Native 记录检查：存在 `dashscope_native` 时必须先切回 Compatible URL 并通过连接测试，才能回退旧应用；
  - [ ] 记录“核心模型模块而非 Starter”“显式 adapter_code”“历史 Compatible 保持不变”决策；
  - [ ] 若验证发现新的版本/自动配置问题，写入 `pitfalls.md`；
  - [ ] 运行 `git diff --check`、链接/路径检查和 Spec-Task-Test 状态一致性检查；
  - [ ] 进入 `/review`，未通过审查前不归档。
- **验收标准**：代码、Spec、Tasks、Test Spec、执行日志和长期决策不存在状态冲突，文档结论均有实际证据。
