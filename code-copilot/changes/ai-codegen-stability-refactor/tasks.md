# 任务拆分 — AI 代码生成稳定性重构
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [x] 已确认 Spec 文档（`spec.md`）
- [x] 本阶段不改数据库与接口协议

## Task 1: 建立第一阶段稳定性重构规格
- **目标**: 固化第一阶段改造边界，确保不影响现有功能
- **涉及文件**:
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 新增，记录背景、边界、风险与执行日志
    - `code-copilot/changes/ai-codegen-stability-refactor/tasks.md` — 新增，拆解任务
- **关键签名**:
  ```text
  N/A
  ```

## Task 2: 修复 AI 会话缓存与上下文透传问题
- **目标**: 避免跨会话缓存串线，修复聊天 prompt/userInput 丢失，并统一同步/流式会话创建
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/ChatClientCache.java` — 修改，拆分基础 client 获取与带 memory client 构建
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java` — 修改，统一会话创建与 client 获取流程
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/service/AiChatService.java` — 修改，修复 prompt 覆盖并补齐 userInput
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/service/AiChatSessionService.java` — 修改，补充幂等 ensure/create 语义
- **关键签名**:
  ```java
  public ChatClient getOrCreateBase(Long providerId, String modelName, AiProvider provider, OpenAiChatOptions options) { }
  public ChatClient createSessionClient(ChatClient baseClient, String sessionId, ChatMemory chatMemory) { }
  public AiClientResponse call(AiClientRequest request) { }
  public Flux<String> chatStream(String content, String agentCode, String sessionId, Long userId,
                                 Long providerId, String modelName, Double temperature, Integer maxTokens,
                                 String projectName, String canvasContext) { }
  public AiChatSession getOrCreate(String sessionId, Long userId, Long tenantId, String agentCode, String firstMsg) { }
  ```

## Task 3: 调整动态 CRUD 读取处理顺序并做最小回归验证
- **目标**: 保持接口不变的前提下修正读取顺序，并完成最小编译验证
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 修改，调整处理顺序
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 修改，回填执行日志与审查结论
- **关键签名**:
  ```java
  public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) { }
  public Map<String, Object> selectById(String configKey, Long id) { }
  ```

## Task 4: 抽离 AI 调用解析职责
- **目标**: 将 Agent / Provider / Model / 参数解析从 `AiClientImpl` 抽离为独立解析器，降低主流程复杂度
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiInvocationResolver.java` — 新增，统一解析调用上下文
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java` — 修改，委托解析器
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 修改，补充执行日志
- **关键签名**:
  ```java
  public ResolvedInvocation resolve(String agentCode, Long providerId, String modelName,
                                    Double temperature, Integer maxTokens) { }
  ```

## Task 5: 补齐动态 CRUD 审计字段自动填充
- **目标**: 在不改接口的前提下，为配置驱动 CRUD 的写链路补齐审计字段填充，且不覆盖显式业务输入
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java` — 修改，补充 insert/update 审计字段填充
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 修改，补充执行日志
- **关键签名**:
  ```java
  public int insert(String tableName, Map<String, Object> data) { }
  public int updateById(String tableName, Long id, Map<String, Object> data) { }
  ```

## Task 6: 收敛动态 CRUD 写链路准备逻辑
- **目标**: 在保持现有写入语义不变的前提下，提取 insert/update 共用的写链路准备 helper，降低后续扩展风险
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java` — 修改，抽离 `prepareInsertData` / `prepareUpdateData` 等 helper
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 修改，补充执行日志与审查结论
    - `code-copilot/changes/ai-codegen-stability-refactor/tasks.md` — 修改，登记本次任务
- **关键签名**:
  ```java
  public int insert(String tableName, Map<String, Object> data) { }
  public int updateById(String tableName, Long id, Map<String, Object> data) { }
  private Map<String, Object> prepareInsertData(String tableName, Map<String, Object> data) { }
  private Map<String, Object> prepareUpdateData(String tableName, Map<String, Object> data) { }
  ```

## Task 7: 收敛动态 CRUD 查询/删除公共条件拼装
- **目标**: 在不改变 SQL 语义的前提下，统一租户条件、where 连接与 select SQL 拼装，降低重复逻辑
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java` — 修改，抽离 `buildSelectSql` / `appendTenantCondition` / `appendWhereCondition` 等 helper
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 修改，补充执行日志与审查结论
    - `code-copilot/changes/ai-codegen-stability-refactor/tasks.md` — 修改，登记本次任务
- **关键签名**:
  ```java
  public Page<Map<String, Object>> selectPage(String tableName, int pageNum, int pageSize,
                                              Map<String, Object> searchParams,
                                              Set<String> allowedSearchFields,
                                              Map<String, String> searchTypeMap,
                                              Map<String, String> columnMapping,
                                              String orderBy) { }
  public Map<String, Object> selectById(String tableName, Long id) { }
  public int deleteById(String tableName, Long id, boolean logicDelete) { }
  ```

## Task 8: 补最小回归测试骨架
- **目标**: 为本轮低风险改造补入最小单测骨架，但不改变仓库默认跳过测试的构建策略
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/pom.xml` — 修改，补充测试依赖
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/pom.xml` — 修改，补充测试依赖
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepositoryTest.java` — 新增，覆盖审计字段与不可更新字段规则
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/client/AiInvocationResolverTest.java` — 新增，覆盖解析优先级与默认值逻辑
    - `code-copilot/changes/ai-codegen-stability-refactor/spec.md` — 修改，补充执行日志与验证说明
    - `code-copilot/changes/ai-codegen-stability-refactor/tasks.md` — 修改，登记本次任务
- **关键签名**:
  ```java
  class DynamicCrudRepositoryTest { }
  class AiInvocationResolverTest { }
  ```
