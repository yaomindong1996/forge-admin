# 任务拆分 — AI 代码生成器基础增强
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [ ] forge-plugin-ai 模块已可编译运行（AI 供应商/模型/Agent/对话功能正常）
- [ ] forge-plugin-generator 模块已可编译运行（导入表/字段配置/代码生成功能正常）
- [ ] forge-admin 模块同时依赖 ai 和 generator 插件

## Task 1: 数据模型扩展 ✅
- **目标**: 新增 ai_context_config 表和 GenTableColumn 字段，为后续功能提供数据基础
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/sql/ai_context_config.sql` — 新增，DDL 初始化脚本
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/sql/gen_table_column_ai_fields.sql` — 新增，DDL ALTER 脚本
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTableColumn.java` — 修改，新增 validateRule/aiRecommended 字段
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/domain/AiContextConfig.java` — 新增，ai_context_config 实体
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/mapper/AiContextConfigMapper.java` — 新增，Mapper
- **关键签名**:
  ```java
  // GenTableColumn 新增字段
  private String validateRule;
  private Integer aiRecommended;
  
  // AiContextConfig 实体
  public class AiContextConfig extends TenantEntity {
      private Long id;
      private String agentCode;
      private String configName;
      private String configContent;
      private String configType;  // SPEC/SAMPLE/RULE
      private Integer sort;
      private String status;
  }
  ```

## Task 2: AiClient 接口与通用 DTO 定义 ✅
- **目标**: 定义 AiClient 接口和通用请求/响应 DTO，为所有 AI 调用提供统一协议
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClient.java` — 新增，接口定义
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/dto/AiClientRequest.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/dto/AiClientResponse.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/dto/AiFallbackReason.java` — 新增，降级原因枚举
- **关键签名**:
  ```java
  public interface AiClient {
      AiClientResponse call(AiClientRequest request);
      Flux<String> stream(AiClientRequest request);
  }
  
  public class AiClientRequest {
      private String agentCode;
      private String message;
      private Long providerId;
      private String modelName;
      private Double temperature;
      private Integer maxTokens;
      private Map<String, String> contextVars;
      private String sessionId;
  }
  
  public class AiClientResponse {
      private String content;
      private boolean fallback;
      private String fallbackReason;
      private String sessionId;
  }
  
  public enum AiFallbackReason {
      PROVIDER_NOT_CONFIGURED, PROVIDER_DISABLED, API_ERROR, TIMEOUT
  }
  ```

## Task 3: AiClientImpl 实现（含缓存与降级） ✅
- **目标**: 实现 AiClient 接口，从 AiChatService 提取通用逻辑，加入 ChatClient 缓存、上下文注入、降级/熔断机制
- **依赖**: Task 1, Task 2
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java` — 新增，核心实现
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/ChatClientCache.java` — 新增，Caffeine 缓存管理
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/CircuitBreaker.java` — 新增，熔断器
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/ContextInjector.java` — 新增，上下文注入（读 code-style 文件 + 读 DB SPEC 上下文）
- **关键签名**:
  ```java
  @Service
  public class AiClientImpl implements AiClient {
      public AiClientResponse call(AiClientRequest request) { }
      public Flux<String> stream(AiClientRequest request) { }
  }
  
  @Component
  public class ChatClientCache {
      public ChatClient getOrCreate(Long providerId, String modelName, AiProvider provider, OpenAiChatOptions options) { }
      public void evict(Long providerId, String modelName) { }
      public void evictByProvider(Long providerId) { }
  }
  
  @Component
  public class CircuitBreaker {
      public boolean isOpen(String key);
      public void recordFailure(String key);
      public void recordSuccess(String key);
  }
  
  @Component
  public class ContextInjector {
      public String injectContext(String systemPrompt, String agentCode) { }
  }
  ```

## Task 4: AiPromptTemplateRenderer 泛化 ✅
- **目标**: 将 AiPromptTemplateRenderer 从大屏专用扩展为通用模板渲染，支持任意变量替换
- **依赖**: 无强依赖，可与 Task 3 并行
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/service/AiPromptTemplateRenderer.java` — 修改，新增通用 render 方法
- **关键签名**:
  ```java
  // 新增通用方法
  public static String render(String template, Map<String, String> variables) { }
  // 原有 renderDashboardSystemPrompt 保持不变（调用通用 render）
  ```

## Task 5: AiChatService 重构（委托 AiClient）
- **目标**: AiChatService 的 generateDashboard*/chatStream 方法改为委托 AiClient，仅保留大屏 prompt 构建逻辑
- **依赖**: Task 3
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/service/AiChatService.java` — 修改，注入 AiClient，删除 buildChatClient/resolveProvider/resolveModel/resolveTemperature/resolveMaxTokens/executeStreamChat/persistChatConversation，保留 buildDashboardPrompt/buildDashboardSystemPrompt/buildChatUserPrompt
- **关键签名**:
  ```java
  @Service
  public class AiChatService {
      private final AiClient aiClient; // 替换原有的直接构建逻辑
      
      public String generateDashboard(AIGenerateRequest request) {
          // 委托 aiClient.call() 替代原有 buildChatClient().prompt().call()
      }
      public Flux<String> generateDashboardStream(AIGenerateRequest request) {
          // 委托 aiClient.stream() 替代原有 buildChatClient().prompt().stream()
      }
      public Flux<String> chatStream(...) {
          // 委托 aiClient.stream() 替代原有 executeStreamChat()
      }
  }
  ```

## Task 6: AiClientController + AiContextConfig CRUD
- **目标**: 新增通用 AI 调用 Controller 和上下文配置管理 CRUD
- **依赖**: Task 3, Task 5
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/controller/AiClientController.java` — 新增，/ai/client/call 和 /ai/client/stream
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/controller/AiContextConfigController.java` — 新增，CRUD 接口
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/context/service/AiContextConfigService.java` — 新增
- **关键签名**:
  ```java
  @RestController
  @RequestMapping("/ai/client")
  public class AiClientController {
      @PostMapping("/call")
      public RespInfo<AiClientResponse> call(@RequestBody AiClientRequest request) { }
      
      @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
      public Flux<ServerSentEvent<String>> stream(@RequestBody AiClientRequest request) { }
  }
  
  @RestController
  @RequestMapping("/ai/context")
  public class AiContextConfigController {
      @GetMapping("/list")
      public RespInfo<List<AiContextConfig>> list(@RequestParam String agentCode) { }
      @PostMapping("/add")
      public RespInfo<Void> add(@RequestBody AiContextConfig config) { }
      @PutMapping("/update")
      public RespInfo<Void> update(@RequestBody AiContextConfig config) { }
      @DeleteMapping("/{id}")
      public RespInfo<Void> delete(@PathVariable Long id) { }
  }
  ```

## Task 7: code-style 文件 + Agent 初始数据
- **目标**: 创建 code-style.md 配置文件，新增 codegen_column_advisor 和 codegen_schema_builder Agent 初始数据
- **依赖**: Task 1（DDL 脚本）
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/ai-codegen/code-style.md` — 新增，项目编码规范
    - `forge/forge-admin/src/main/resources/sql/` — 修改，追加 Agent 初始数据 INSERT 语句
- **关键签名**:
  ```sql
  -- codegen_column_advisor Agent
  INSERT INTO ai_agent (id, tenant_id, agent_name, agent_code, description, system_prompt, temperature, max_tokens, status)
  VALUES (?, 1, '代码生成字段顾问', 'codegen_column_advisor', '根据数据库字段信息推荐Java类型、表单组件、字典类型、验证规则', '...', 0.3, 4096, '0');
  
  -- codegen_schema_builder Agent
  INSERT INTO ai_agent (id, tenant_id, agent_name, agent_code, description, system_prompt, temperature, max_tokens, status)
  VALUES (?, 1, '代码生成Schema构建器', 'codegen_schema_builder', '根据自然语言描述推断数据模型Schema', '...', 0.3, 4096, '0');
  ```

## Task 8: AI 字段推荐后端接口
- **目标**: 在 generator 模块新增 AI 字段推荐接口，注入 AiClient + 字典列表 + 降级逻辑
- **依赖**: Task 3, Task 7
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/GenController.java` — 修改，新增 recommendColumns 接口
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiRecommendService.java` — 新增，AI 推荐服务（注入 AiClient、字典列表查询、推荐结果校验、降级逻辑）
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/RecommendColumnsRequest.java` — 新增
- **关键签名**:
  ```java
  @Service
  public class AiRecommendService {
      private final AiClient aiClient; // Admin 桥接注入
      
      public List<GenTableColumn> recommendColumns(Long tableId, List<GenTableColumn> columns) { }
      
      // 内部方法
      private String buildColumnRecommendPrompt(GenTable table, List<GenTableColumn> columns, String dictList) { }
      private List<GenTableColumn> validateAndMerge(List<GenTableColumn> original, List<GenTableColumn> recommended) { }
      private List<GenTableColumn> fallbackToRuleBased(List<GenTableColumn> columns) { }
  }
  ```

## Task 9: NL→Schema 后端接口
- **目标**: 在 generator 模块新增自然语言到 Schema 推断接口（单轮 + 追问优化）
- **依赖**: Task 3, Task 7
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/GenController.java` — 修改，新增 nlToSchema/nlToSchemaRefine 接口
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiSchemaService.java` — 新增，Schema 推断服务
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/NlToSchemaRequest.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/NlToSchemaRefineRequest.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/NlToSchemaResult.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/SchemaColumn.java` — 新增
- **关键签名**:
  ```java
  @Service
  public class AiSchemaService {
      private final AiClient aiClient;
      
      public NlToSchemaResult nlToSchema(String description) { }
      public NlToSchemaResult nlToSchemaRefine(String currentSchemaJson, String message, String sessionId) { }
      
      private NlToSchemaResult parseSchemaResponse(String aiResponse) { }
      private void appendBaseColumns(List<SchemaColumn> columns) { }
  }
  ```

## Task 10: 前端 AI 推荐按钮 + Diff 标记
- **目标**: ColumnConfigModal 中新增 AI 推荐按钮，调用推荐接口回填结果并标记 Diff
- **依赖**: Task 8
- **涉及文件**:
    - `forge-admin-ui/src/views/generator/components/ColumnConfigModal.vue` — 修改，新增 AI 推荐按钮、调用接口、结果回填、Diff 标记样式
    - `forge-admin-ui/src/api/ai.js` — 修改，新增 AiClient 通用调用接口
    - `forge-admin-ui/src/api/generator.js` — 新增或修改，新增 AI 字段推荐接口
- **关键变更**:
  ```vue
  <!-- ColumnConfigModal 新增按钮 -->
  <n-button type="primary" :loading="aiLoading" @click="handleAiRecommend">
    AI 推荐
  </n-button>
  
  <!-- Diff 标记样式 -->
  <td :class="{ 'ai-recommended': row.aiRecommended && isDiff(row, 'javaType') }">
  ```

## Task 11: 前端 AiSchemaModal（NL→Schema + 追问优化）
- **目标**: 新增 AiSchemaModal 组件，支持自然语言输入、Schema 预览、追问优化、确认导入
- **依赖**: Task 9
- **涉及文件**:
    - `forge-admin-ui/src/views/generator/components/AiSchemaModal.vue` — 新增
    - `forge-admin-ui/src/views/generator/table.vue` — 修改，新增"AI 建表"操作入口
    - `forge-admin-ui/src/api/generator.js` — 修改，新增 NL→Schema 接口
- **关键变更**:
  ```vue
  <!-- table.vue toolbar 新增 -->
  <n-button type="primary" size="small" @click="showAiSchemaModal = true">
    AI 建表
  </n-button>
  
  <!-- AiSchemaModal 流程 -->
  输入描述 → 调用 nlToSchema → 展示 Schema 预览表 → "追问优化"→ 输入追问 → 调用 nlToSchemaRefine → 更新预览 → "确认导入"
  ```

## Task 12: 前端 AI 上下文配置管理页面
- **目标**: 新增 AI 上下文配置管理页面，管理 SPEC 上下文片段
- **依赖**: Task 6
- **涉及文件**:
    - `forge-admin-ui/src/views/ai/context-config.vue` — 新增，使用 AiCrudPage 组件
    - `forge-admin-ui/src/api/ai.js` — 修改，新增上下文配置 CRUD 接口
    - 路由配置 — 修改，新增菜单/路由
- **关键变更**:
  ```vue
  <!-- 使用 AiCrudPage CRUD 页面 -->
  <AiCrudPage
    :api-config="{
      list: 'get@/ai/context/list?agentCode=xxx',
      create: 'post@/ai/context/add',
      update: 'put@/ai/context/update',
      delete: 'delete@/ai/context/{id}'
    }"
    :edit-schema="contextEditSchema"
    :columns="contextColumns"
  />
  ```

## Task 13: 大屏生成回归测试
- **目标**: 验证 AiChatService 重构后大屏生成功能不受影响
- **依赖**: Task 5
- **涉及文件**:
    - 测试文件（手动或自动化）
- **验证项**:
  - `POST /ai/generate` 非流式大屏生成正常
  - `POST /ai/generate/stream` SSE 流式大屏生成正常
  - `POST /ai/chat/stream` 多轮对话上下文延续正常
  - 对话记录正常持久化到 ai_chat_record
  - 对比重构前后输出内容一致性
