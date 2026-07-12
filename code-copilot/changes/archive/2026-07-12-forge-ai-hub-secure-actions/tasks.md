# Forge AI 中枢受控业务动作实施计划

> 变更名：`forge-ai-hub-secure-actions`
> 状态：done
> 原则：发布快照 → 动态授权目录 → 字段交集 → elicitation → 幂等执行 → 双审计

## Task 1：MCP 固定 Tool 扩展点

**文件**：
- Modify `forge-plugin-mcp/.../config/ForgeMcpServerAutoConfiguration.java`
- Create `forge-plugin-mcp/.../spi/McpToolContributor.java`
- Create/Modify MCP contributor 聚合测试

- [x] Red：新增两个 contributor 时工具集合应稳定合并，重复 Tool 名启动失败；
- [x] 实现 `McpToolContributor`，默认 ping 作为 contributor；
- [x] 保持 SYNC/STREAMABLE/stdio=false Guard 不变；
- [x] 运行 MCP 目标测试，16/16 通过。

## Task 2：发布动作快照与幂等加固

**文件**：
- Modify `BusinessObjectDesignVersionMapper.java/xml`
- Modify `BusinessObjectActionService.java`
- Modify `BusinessActionExecutionService.java`
- Modify `AiBusinessActionExecutionLog.java` 与 Mapper XML
- Modify Generator 单元测试

- [x] Red：草稿动作不能替代最近发布版本；相同幂等键不同参数必须失败；无租户上下文必须失败；
- [x] 增加发布版本 XML 查询和 `resolvePublishedAction`；
- [x] 增加 `executePublished`，只使用指定发布版本动作；
- [x] canonical JSON 只落 SHA-256 指纹，不落参数值；
- [x] 幂等命中前比较请求指纹；
- [x] `BusinessActionExecutionServiceTest` 4/4、`BusinessObjectActionServicePublishedTest` 3/3 通过。

## Task 3：Secure Actions 模块与发布 API

**文件**：
- Create `forge-plugin-capability-secure-actions/pom.xml`
- Modify plugin parent、BOM、Admin POM
- Create `.../publish/BusinessActionCapabilityPublisher.java`
- Create `.../publish/BusinessActionCapabilityController.java`
- Create DTO、policy parser、schema builder、step validator

- [x] Red：未发布动作、未知步骤、流程/消息/领域动作、空白名单和非模型字段发布失败；
- [x] 只允许 BUSINESS_ACTION/ACTION/MEDIUM/MCP_ELICITATION；
- [x] 生成 `additionalProperties=false`、Draft 2020-12 的输入/输出 Schema 和不可变 policy snapshot；
- [x] 使用 `@SaCheckPermission("ai:capability:business-action:publish")`、`@ApiDecrypt` 和统一响应；
- [x] 建立模块、BOM、plugin parent、Admin 聚合和可独立关闭的自动配置。

## Task 4：ACTION grant 与动态目录

**文件**：
- Modify `CapabilityGrantService.java`
- Create Secure Actions 授权查询 Mapper/XML
- Create `SecureActionCatalogService.java`

- [x] Red：ACTION 无字段策略、HIGH、跨租户/组织、撤销 grant、无动作权限全部拒绝；
- [x] ACTION grant 只允许 MEDIUM + BUSINESS_ACTION + 非空 allowedFields；
- [x] search/describe 使用显式 XML keyset 分批查询，不加载全库或产生业务 N+1；
- [x] resolvedVersion、发布对象版本和 sourceVersion 精确匹配；
- [x] 字段策略按版本、grant、不可变发布模型字段取交集。

## Task 5：MCP search/describe/invoke 与 R2 确认

**文件**：
- Create `.../mcp/SecureActionMcpToolContributor.java`
- Create `.../mcp/SecureActionMcpHandler.java`
- Create tool schema constants/tests

- [x] Red：SERVICE invoke、scope 不足、无 elicitation、DECLINE/CANCEL、字段越权、幂等冲突均无副作用；
- [x] search/describe 返回结构化授权目录；
- [x] invoke 校验 target schema 后调用 `exchange.createElicitation`；
- [x] 仅 `Action.ACCEPT + confirm=true` 继续执行；
- [x] annotations 固定：invoke destructive=true、idempotent=true、openWorld=false；
- [x] 三个 Tool 均提供 outputSchema、text JSON 与 structuredContent，错误码稳定且不泄露内部信息。

## Task 6：双审计与 Flyway

**文件**：
- Create `V1.0.23__add_secure_business_action_capabilities.sql`
- Modify业务动作日志实体/XML写入
- Integrate secure action invocation audit/tests

- [x] Red：USER A/client/serviceUser/tenant/org/requestId 任一缺失不执行；
- [x] 业务动作日志关联 capability requestId 和双身份；
- [x] Capability 调用日志不保存 arguments，审计异常只安全记录 requestId/能力码/异常类型；
- [x] Flyway 使用 information_schema 防重复、tenant_id=1、显式列名；
- [x] `xmllint`、placeholder、物理 DELETE 和敏感信息扫描通过。

## Task 7：聚合验证与回填

- [x] Secure Actions、Generator 专项、Control Plane、Identity、MCP 测试通过；
- [x] Admin JDK 17 `mvn -pl forge-admin-server -am package -DskipTests` 通过；
- [x] dependency tree 无 Spring Authorization Server、Sa-Token OAuth2；
- [x] 静态扫描确认仅有拒绝 SSE/STATELESS/stdio/ASYNC 的 Guard 与负向测试；
- [x] 更新 execution-log、test-spec 和状态；
- [x] 未启动服务、未调用真实模型、未执行 Flyway、未 commit/push。

## Review 发现（2026-07-12）

- [x] 完成阶段一 Spec Compliance Review；结论 FAIL，按流程未进入 Code Quality Review；
- [x] R1：按 `version policy ∩ grant policy ∩ published model` 重建/裁剪运行时 `arguments` Schema，并在 invoke 再做显式字段交集校验；
- [x] R2：目录查询只使用 resolved `ai_capability_version` 的 source binding、Schema 和 policy，禁止混用能力主表当前 binding；
- [x] R3：发布和调用阶段递归校验所有动作步骤容器，嵌套/未知/禁用步骤失败关闭；
- [x] R4：区分目录、授权、审计和执行基础设施故障，返回稳定 `*_UNAVAILABLE` 错误且安全记录异常类型；
- [x] R5：建立审计预留与可信身份条件更新闭环，审计不可用时不静默返回业务成功；
- [x] R6：elicitation 请求指纹改为规范化请求内容的 SHA-256 摘要，不记录参数值；
- [x] Secure Actions 27、Control Plane 27、Generator 7、MCP 16、Identity 35 和 Admin 40/40 验证通过；
- [x] 再次执行 `/review forge-ai-hub-secure-actions`；Spec Compliance 与 Code Quality 均 PASS。
- [x] 归档到 `code-copilot/changes/archive/2026-07-12-forge-ai-hub-secure-actions/`。
