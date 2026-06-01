# forge-plugin-ai 模块深度分析：多 AI 供应商接入设计

> 分析时间：2026-06-01  
> 分析范围：`forge-plugin-ai` 模块全部源码

---

## 一、模块整体架构

```
forge-plugin-ai/
├── client/               ← AI 调用核心层（本文重点）
│   ├── AiClient.java          接口定义
│   ├── AiClientImpl.java      实现（call + stream）
│   ├── AiInvocationResolver.java  参数解析与供应商选择
│   ├── ChatClientCache.java       Spring AI ChatClient 缓存
│   ├── CircuitBreaker.java       熔断器
│   └── ContextInjector.java      上下文注入
├── provider/             ← 供应商管理（CRUD + 测试）
│   ├── domain/AiProvider.java
│   ├── service/AiProviderService.java
│   └── controller/AiProviderController.java
├── model/                ← 模型管理（CRUD，依附于供应商）
│   ├── domain/AiModel.java
│   └── service/AiModelService.java
├── agent/                ← Agent 定义（绑定默认供应商/模型）
│   ├── domain/AiAgent.java
│   └── service/AiAgentService.java
├── chat/                 ← 对话服务层
│   ├── service/AiChatService.java
│   └── service/AiPromptTemplateRenderer.java
└── context/              ← 上下文配置（RULE / SPEC 类型）
```

---

## 二、AI 供应商抽象层设计

### 2.1 核心设计思想

本模块**没有**定义自己的 `AiProvider` 接口，而是**直接复用 Spring AI 的 `ChatModel` 抽象**，通过 `OpenAiApi` 兼容所有兼容 OpenAI 协议的供应商（OpenAI / DeepSeek / 智谱 / 通义千问 / Ollama 等）。

```
调用链：
Controller → AiClient.call/stream → AiInvocationResolver（解析参数+选供应商）
→ ChatClientCache（获取/创建 Spring AI ChatClient）→ Spring AI ChatModel → 真实 AI API
```

### 2.2 AiClient 接口（统一调用接口）

```java
// 文件：client/AiClient.java
public interface AiClient {
    AiClientResponse call(AiClientRequest request);   // 同步调用
    Flux<String> stream(AiClientRequest request);      // 流式调用（SSE）
}
```

**设计亮点**：
- 接口极简，只有 `call` 和 `stream` 两个方法
- 请求参数统一封装在 `AiClientRequest` 中，屏蔽了底层差异
- 返回类型统一：`AiClientResponse`（同步）/ `Flux<String>`（流式）

---

## 三、支持的 AI 供应商

### 3.1 供应商类型枚举

在 `AiProviderController.templates()` 中硬编码了 7 种预设模板：

| templateKey | 供应商名称 | 默认 Base URL | 默认模型 |
|---|---|---|---|
| `alibaba` | 阿里百炼 | `https://dashscope.aliyuncs.com/compatible-mode` | `qwen-plus` |
| `openai` | OpenAI | `https://api.openai.com` | `gpt-4o-mini` |
| `zhipu` | 智谱 AI | `https://open.bigmodel.cn/api/paas/v4` | `glm-4` |
| `moonshot` | Moonshot | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` |
| `deepseek` | DeepSeek | `https://api.deepseek.com` | `deepseek-chat` |
| `ollama` | Ollama（本地） | `http://localhost:11434` | `llama3` |
| `custom` | 自定义 | 空（用户填写） | 空（用户填写） |

**关键设计**：所有供应商均通过 `OpenAiApi` 适配，因为它们都兼容 OpenAI API 协议。`provider_type` 字段目前主要用于**前端展示和字典区分**，不影响后端调用逻辑。

### 3.2 数据库中的供应商配置

```sql
-- ai_provider 表核心字段
CREATE TABLE ai_provider (
    id            BIGINT PRIMARY KEY,
    provider_name  VARCHAR(128),   -- 显示名称
    provider_type  VARCHAR(32),    -- 类型标识（openai/deepseek/ollama/custom...）
    api_key       VARCHAR(512),    -- API Key（加密存储）
    base_url      VARCHAR(512),    -- API Base URL
    default_model VARCHAR(128),    -- 默认模型（冗余字段，与 ai_model 表同步）
    models        TEXT,            -- 可用模型列表 JSON（冗余字段）
    is_default    CHAR(1),        -- 是否默认供应商（0/1）
    status        CHAR(1)          -- 状态（0正常/1停用）
);
```

---

## 四、配置管理

### 4.1 三层配置优先级（参数解析核心）

`AiInvocationResolver.resolve()` 方法实现了**三层优先级覆盖**设计：

```
请求级参数（最高）> Agent 默认配置（次之）> 供应商默认配置（最低）
```

**模型名称解析逻辑**（`resolveModel` 方法）：

```java
private String resolveModel(String modelName, AiAgent agent, AiProvider provider) {
    if (StringUtils.hasText(modelName))     // 1. 请求中明确指定
        return modelName;
    if (StringUtils.hasText(agent.getModelName()))  // 2. Agent 绑定的默认模型
        return agent.getModelName();
    if (StringUtils.hasText(provider.getDefaultModel())) // 3. 供应商默认模型
        return provider.getDefaultModel();
    return "gpt-3.5-turbo";  // 4. 硬编码兜底
}
```

**温度和 maxTokens 同理**，均遵循此优先级。

### 4.2 AiClientRequest 请求参数

```java
// 文件：client/dto/AiClientRequest.java
public class AiClientRequest {
    private String agentCode;        // Agent 编码（必填，用于定位系统提示和默认配置）
    private String message;           // 用户提示词
    private String userInput;         // 用户输入（与 message 二选一，getUserInputOrMessage() 统一获取）
    private Long providerId;          // 供应商 ID（可选，覆盖 Agent 的默认供应商）
    private String modelName;         // 模型名称（可选，覆盖 Agent/供应商 默认值）
    private Double temperature;       // 温度（可选）
    private Integer maxTokens;        // 最大 Token（可选）
    private Map<String, String> contextVars;  // 模板变量（用于渲染系统提示词中的占位符）
    private String sessionId;         // 会话 ID（可选，用于多轮对话）
}
```

### 4.3 供应商配置管理接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `GET /ai/provider/templates` | 获取预设模板列表 | 前端新增供应商时的下拉选项 |
| `GET /ai/provider/page` | 分页查询 | 支持按名称/类型/状态筛选 |
| `POST /ai/provider` | 新增供应商 | |
| `PUT /ai/provider` | 更新供应商 | 更新后自动同步 `ai_model` 数据到 `ai_provider.models` 字段 |
| `DELETE /ai/provider/{id}` | 删除供应商 | 有模型关联时拒绝删除 |
| `POST /ai/provider/test` | 测试连接 | 用配置的实际 `baseUrl` + `apiKey` 发一条测试消息 |
| `PUT /ai/provider/{id}/default` | 设为默认 | 清除其他默认标记，设当前为默认 |

---

## 五、统一调用接口与供应商差异屏蔽

### 5.1 ChatClientCache：Spring AI ChatClient 的构建与缓存

```java
// 文件：client/ChatClientCache.java
private ChatClient buildBaseChatClient(AiProvider provider, OpenAiChatOptions options) {
    OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(provider.getBaseUrl())   // ← 不同供应商的差异点
            .apiKey(provider.getApiKey())     // ← 不同供应商的差异点
            .build();
    ChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();
    return ChatClient.builder(chatModel).build();
}
```

**屏蔽差异的关键**：所有供应商都通过 `OpenAiApi` 构建，差异仅在于 `baseUrl` 和 `apiKey` 两个配置值。这意味着：
- 新增供应商只需在数据库配置正确的 `baseUrl`，无需写任何 Java 代码
- DeepSeek、智谱、通义千问等兼容 OpenAI 协议的供应商均可直接接入

**缓存策略**：
- 缓存 Key：`providerId + ":" + modelName + ":" + temperature + ":" + maxTokens`
- 最大缓存数：50
- TTL：30 分钟
- 供应商配置变更时，主动调用 `evictByProvider(providerId)` 清除缓存

### 5.2 流式输出支持

`AiClientImpl.stream()` 方法完整支持 SSE 流式返回：

```java
// 流式调用核心代码
return chatClient.prompt()
    .system(systemPrompt)
    .user(request.getMessage())
    .stream()
    .chatResponse()
    .concatMap(chatResponse -> { /* 逐块处理并 Flux 输出 */ })
    .doFinally(signal -> { /* 持久化完整对话记录 */ });
```

**流式响应格式**（SSE 事件流）：

| event 类型 | data 格式 | 说明 |
|---|---|---|
| `progress` | `{"stage":"connecting","message":"正在连接模型..."}` | 连接阶段提示 |
| `chunk` | `{"content":"<文本块>"}` | 流式文本块（思考过程或正式回复） |
| `complete` | `{"sessionId":"...","message":"生成完成"}` | 流式结束 |
| `error` | `{"message":"错误信息"}` | 错误 |

**思考过程分离**（支持 DeepSeek-R1 等推理模型）：

```java
// 从 AssistantMessage 的 metadata 中提取 reasoning_content
private String extractReasoningContent(AssistantMessage message) {
    Map<String, Object> metadata = message.getMetadata();
    // 尝试多种可能的 key（不同模型返回格式略有差异）
    Object reasoning = metadata.get("reasoningContent");
    if (reasoning instanceof String) return (String) reasoning;
    reasoning = metadata.get("reasoning_content");
    if (reasoning instanceof String) return (String) reasoning;
    reasoning = metadata.get("reasoning");
    if (reasoning instanceof String) return (String) reasoning;
    return null;
}
```

流式输出时，思考过程和正式回复用分隔符分开：
```
==================== 思考过程 ====================
（reasoning content 逐块输出）
==================== 完整回复 ====================
（正式回复逐块输出）
```

---

## 六、供应商切换机制

### 6.1 配置级切换（主推方式）

在数据库层面通过 `is_default` 字段标记默认供应商：

```java
// AiProviderService.getDefaultProvider()
public AiProvider getDefaultProvider() {
    return getOne(new LambdaQueryWrapper<AiProvider>()
            .eq(AiProvider::getIsDefault, "1")
            .eq(AiProvider::getStatus, "0")
            .last("LIMIT 1"));
}
```

**切换步骤**（通过管理后台）：
1. 进入"供应商管理"页面
2. 找到目标供应商，点击"设为默认"
3. 后端执行 `UPDATE ai_provider SET is_default='1' WHERE id=?`（先清除其他记录的默认标记）
4. 下次 AI 调用时，`AiInvocationResolver` 在 `resolveProvider()` 中按优先级自动选取

### 6.2 请求级覆盖（临时切换）

每次请求可以通过 `AiClientRequest.providerId` 临时指定供应商，不影响全局默认配置：

```java
// AiInvocationResolver.resolveProvider() 优先级
private AiProvider resolveProvider(Long providerId, AiAgent agent) {
    AiProvider provider = null;
    if (providerId != null) {
        provider = providerService.getById(providerId);  // 1. 请求级指定
    }
    if (provider == null && agent.getProviderId() != null) {
        provider = providerService.getById(agent.getProviderId());  // 2. Agent 绑定
    }
    if (provider == null) {
        provider = providerService.getDefaultProvider();  // 3. 全局默认
    }
    // 校验 status 和配置完整性...
    return provider;
}
```

### 6.3 Agent 级绑定

每个 `AiAgent` 记录可以绑定默认供应商和模型：

```java
// AiAgent 实体
private Long providerId;    // 绑定的供应商 ID
private String modelName;    // 绑定的默认模型
private BigDecimal temperature;
private Integer maxTokens;
```

这样不同业务场景（"数据大屏生成 Agent"、"SQL 助手 Agent"）可以使用不同的供应商/模型，互不干扰。

---

## 七、错误处理与重试机制

### 7.1 熔断器（CircuitBreaker）

```java
// 文件：client/CircuitBreaker.java
@Component
public class CircuitBreaker {
    private static final int FAILURE_THRESHOLD = 3;       // 连续失败 3 次触发熔断
    private static final long RECOVERY_MILLIS = 5 * 60 * 1000L;  // 5 分钟后自动恢复

    private final ConcurrentHashMap<String, AtomicInteger> failureCount;  // key → 失败次数
    private final ConcurrentHashMap<String, Long> openTime;              // key → 熔断开启时间
}
```

**熔断 Key**：以 `agentCode` 为粒度（同一 Agent 的多次调用共享熔断状态）。

**熔断行为**：
- 熔断开启时，`call()` 和 `stream()` 直接返回 fallback 响应，不发起真实 API 调用
- 5 分钟后自动进入半开状态，下次调用尝试恢复

### 7.2 业务异常分类

```java
// AiClientImpl.handleBusinessException()
private AiClientResponse handleBusinessException(BusinessException e, AiClientRequest request) {
    AiFallbackReason reason;
    String msg = e.getMessage();
    if (msg.contains("未配置"))
        reason = AiFallbackReason.PROVIDER_NOT_CONFIGURED;
    else if (msg.contains("已停用"))
        reason = AiFallbackReason.PROVIDER_DISABLED;
    else
        reason = AiFallbackReason.API_ERROR;
    return AiClientResponse.fallback(null, reason, request.getSessionId());
}
```

`AiFallbackReason` 枚举：
- `PROVIDER_NOT_CONFIGURED`：供应商配置不完整（缺 `baseUrl` 或 `apiKey`）
- `PROVIDER_DISABLED`：供应商已停用（`status=1`）
- `API_ERROR`：API 调用失败（网络超时、认证失败等）
- `TIMEOUT`：超时（预留，当前未显式使用）

### 7.3 重试机制

**当前代码中没有显式的重试机制**（没有 Spring Retry 或手动重试循环）。

依赖的容错手段：
1. **熔断器**：快速失败，避免持续请求不可用供应商
2. **Spring AI 内置重试**：`OpenAiChatModel` 底层使用的 `WebClient` 可以配置超时，但没有配置重试
3. ** fallback 响应**：调用失败时返回结构化错误信息，前端可以提示用户

> **注意**：这是一个可以改进的点。生产环境建议增加：
> - Spring Retry 声明式重试（`@Retryable`）或 Spring AI 的 `RetryTemplate`
> - 供应商级别的失败转移（Failover）：主供应商失败时自动切换到备用供应商

---

## 八、ai_model 表与 ai_provider 表的双写同步

### 8.1 设计背景

初始设计只有 `ai_provider` 表，`models` 字段用 JSON 数组存储该供应商支持的模型列表。后来拆分为独立的 `ai_model` 表，但为了**向前兼容**旧接口，保留了 `ai_provider.models` 和 `ai_provider.default_model` 两个冗余字段。

### 8.2 同步机制

```java
// AiProviderController 中，更新供应商后主动同步
@PutMapping
public RespInfo<Void> update(@RequestBody AiProvider provider) {
    providerService.updateById(provider);
    syncModelsToProvider(provider.getId());  // 双写同步
    return RespInfo.success();
}

// 从 ai_model 表聚合写回 ai_provider
private void syncModelsToProvider(Long providerId) {
    List<String> modelIdList = modelService.getModelIdListByProviderId(providerId);
    String defaultModel = modelService.getDefaultModelId(providerId);
    // UPDATE ai_provider SET models=?, default_model=? WHERE id=?
}
```

**同步触发时机**：
- `AiModelService.addModel()` — 新增模型后
- `AiModelService.updateModel()` — 更新模型后
- `AiModelService.deleteModel()` — 删除模型后
- `AiProviderController.update()` — 供应商信息更新后

---

## 九、上下文注入机制（ContextInjector）

除了供应商和模型配置，`forge-plugin-ai` 还支持为 Agent 注入**项目级上下文**：

```java
// ContextInjector.injectContext()
public String injectContext(String systemPrompt, String agentCode) {
    // 1. 注入编码规范（config_type = 'RULE'）
    String codeStyle = readCodeStyle(agentCode);
    // 2. 注入 SPEC 上下文（config_type = 'SPEC'）
    String specContext = readSpecContext(agentCode);
    // 3. 拼接后追加到系统提示词末尾
    return systemPrompt + contextBuilder.toString();
}
```

上下文内容存储在 `ai_context_config` 表中，按 `agentCode` 关联，支持多个配置片段按 `sort` 排序后拼接。

---

## 十、总结与设计评价

### 优点

| 方面 | 评价 |
|---|---|
| **扩展性** | 新增供应商无需改 Java 代码，数据库配置即可 |
| **统一性** | 所有供应商通过 `OpenAiApi` 统一适配，接口极简 |
| **灵活性** | 三层优先级（请求 > Agent > 供应商）覆盖大多数场景 |
| **流式支持** | 完整支持 SSE 流式输出，且能分离思考过程 |
| **多租户** | 所有实体继承 `TenantEntity`，天然支持多租户隔离 |

### 不足与改进建议

| 方面 | 现状 | 建议 |
|---|---|---|
| **重试机制** | 无显式重试 | 增加 Spring Retry 或 Failover 切换 |
| **供应商类型硬编码** | 前端模板列表硬编码在 Controller 中 | 改为数据库字典表驱动 |
| **非 OpenAI 协议供应商** | 无法直接支持（如 Anthropic Claude 原生协议） | 引入策略模式，`AiProviderService` 根据 `provider_type` 构建不同的 `ChatModel` |
| **API Key 加密** | 依赖 `@ApiDecrypt` / `@ApiEncrypt` 注解在传输层加解密 | 存储层也应加密（数据库字段加密） |
| **熔断粒度** | 按 `agentCode` 熔断 | 建议按 `providerId` 熔断，同一供应商的多个 Agent 共享熔断状态 |

---

## 附录：核心文件索引

| 文件 | 路径 | 核心职责 |
|---|---|---|
| AiClient.java | `client/AiClient.java` | 统一调用接口定义 |
| AiClientImpl.java | `client/AiClientImpl.java` | 同步/流式调用实现 |
| AiInvocationResolver.java | `client/AiInvocationResolver.java` | 参数解析与供应商选择 |
| ChatClientCache.java | `client/ChatClientCache.java` | Spring AI ChatClient 缓存管理 |
| CircuitBreaker.java | `client/CircuitBreaker.java` | 熔断器实现 |
| ContextInjector.java | `client/ContextInjector.java` | 系统提示词上下文注入 |
| AiProvider.java | `provider/domain/AiProvider.java` | 供应商实体 |
| AiProviderService.java | `provider/service/AiProviderService.java` | 供应商服务（含连接测试） |
| AiProviderController.java | `provider/controller/AiProviderController.java` | 供应商管理 REST API |
| AiModel.java | `model/domain/AiModel.java` | 模型实体 |
| AiModelService.java | `model/service/AiModelService.java` | 模型服务（含双写同步） |
| AiAgent.java | `agent/domain/AiAgent.java` | Agent 实体（绑定供应商/模型） |
| AiChatService.java | `chat/service/AiChatService.java` | 对话服务（大屏生成入口） |
| AiClientController.java | `client/controller/AiClientController.java` | AI 调用 REST API（SSE 流式） |
