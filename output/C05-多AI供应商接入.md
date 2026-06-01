# 多 AI 供应商接入：如何让 OpenAI、DeepSeek、Ollama 等模型统一调用

> **Forge 框架系列技术博客 C05**
>
> 在现代企业级应用中，单一的大模型供应商往往难以满足所有场景需求。OpenAI 能力强但成本高，DeepSeek 性价比优异但生态尚浅，Ollama 本地部署数据不出域但算力有限。怎么让它们在一个系统里和谐共处？这篇文章带你拆解 Forge 框架的多供应商接入设计。

---

## 一、问题场景

某制造企业的数据中台接入了 AI 能力，团队很快发现"只接一个模型"行不通：

- **成本控制**：日常问答用便宜的 DeepSeek，复杂推理才上 GPT-4，每月 API 开销能差出 5-10 倍
- **性能与延迟**：高并发场景切到本地 Ollama 部署的 Llama3，RT 从 3s 压到 300ms
- **合规要求**：涉及敏感工单数据的分析必须走内网 Ollama，查询公开技术文档可以走云端
- **容灾降级**：OpenAI 偶发不可用时，需要自动切到备用供应商，而不是直接甩一个 500

同时，这种多供应商策略要能在**管理后台动态配置**——运维人员改数据库就能切换模型，不用找开发改代码重新发布。

这本质上是一个"多实现适配 + 运行时路由"问题。Forge 的答案是：**复用 Spring AI 生态，以 OpenAI 协议为统一标准，三层优先级覆盖**。

---

## 二、解决方案

### 2.1 不造轮子：站在 Spring AI 的肩膀上

Forge 的 `forge-plugin-ai` 模块**没有自建 AI 供应商抽象接口**，而是直接基于 Spring AI 的 `ChatModel` + `OpenAiApi`：

```
调用链路：
Controller
  → AiClient.call() / .stream()
    → AiInvocationResolver（解析参数、选择供应商、确定模型）
      → ChatClientCache（获取/创建 Spring AI ChatClient）
        → OpenAiChatModel（Spring AI 实现）
          → OpenAiApi → 真实 AI API
```

所有兼容 OpenAI Chat Completions 协议的供应商——OpenAI、DeepSeek、智谱 GLM、通义千问、Moonshot、Ollama——全部通过 `OpenAiApi` 这一个类接入。对于调用方（上层业务代码），只需面对两个方法：

```java
public interface AiClient {
    AiClientResponse call(AiClientRequest request);   // 同步调用
    Flux<String> stream(AiClientRequest request);      // SSE 流式调用
}
```

### 2.2 三层优先级：灵活到每次请求都能换模型

供应商和模型的选择遵循三层 fallback：

```
请求级参数（最高）
  ↓ 未指定时
Agent 默认配置（次之）
  ↓ 未指定时
供应商默认配置（最低）
  ↓ 仍未指定
硬编码兜底值（gpt-3.5-turbo）
```

模型名称解析的代码逻辑非常直白：

```java
private String resolveModel(String modelName, AiAgent agent, AiProvider provider) {
    if (StringUtils.hasText(modelName))          // 1. 请求中明确指定
        return modelName;
    if (StringUtils.hasText(agent.getModelName())) // 2. Agent 绑定的默认模型
        return agent.getModelName();
    if (StringUtils.hasText(provider.getDefaultModel())) // 3. 供应商默认模型
        return provider.getDefaultModel();
    return "gpt-3.5-turbo";                      // 4. 硬编码兜底
}
```

temperature、maxTokens 同样遵循此优先级。这意味着你可以：

- **请求级**：某次大屏生成请求指定 `modelName="gpt-4o"`，仅本次生效
- **Agent 级**：SQL 生成 Agent 绑定 DeepSeek，大屏生成 Agent 绑定 GPT-4o
- **全局级**：运维把 `is_default` 切到 Ollama，全站默认走本地模型

---

## 三、数据结构

### 3.1 三张核心表

```
ai_provider（供应商）
  ├── 1:N → ai_model（模型列表）
  └── 1:N → ai_agent（Agent 绑定）

ai_agent（Agent 定义）
  ├── 绑定 provider_id + model_name（默认值）
  └── 独立配置 temperature / maxTokens
```

**ai_provider 表**：存储供应商的连接信息：

```sql
provider_name  VARCHAR(128),   -- 显示名称，如"我的 DeepSeek"
provider_type  VARCHAR(32),    -- 类型标识：openai / deepseek / ollama / custom
api_key        VARCHAR(512),   -- API Key（加密存储）
base_url       VARCHAR(512),   -- API 端点地址
default_model  VARCHAR(128),   -- 默认模型（冗余，与 ai_model 表同步）
models         TEXT,           -- 可用模型列表 JSON（冗余）
is_default     CHAR(1),        -- 是否全局默认供应商
status         CHAR(1)         -- 0 正常 / 1 停用
```

**ai_agent 表**：每个 Agent 定义自己的默认供应商和模型：

```java
private Long providerId;       // 绑定的供应商 ID
private String modelName;      // 绑定的默认模型
private BigDecimal temperature;
private Integer maxTokens;
```

不同业务场景使用不同 Agent（如 "report-gen-agent" 绑定 GPT-4o、"sql-helper-agent" 绑定 DeepSeek），互不干扰。

### 3.2 双写同步机制

`ai_model` 表（模型的独立管理）和 `ai_provider.models`、`ai_provider.default_model` 冗余字段之间存在**双写同步**。每次模型增删改，或供应商信息更新后，系统自动聚合 `ai_model` 表数据并回写 `ai_provider`：

```java
private void syncModelsToProvider(Long providerId) {
    List<String> modelIdList = modelService.getModelIdListByProviderId(providerId);
    String defaultModel = modelService.getDefaultModelId(providerId);
    // UPDATE ai_provider SET models=?, default_model=? WHERE id=?
}
```

这样旧接口仍然能直接从 `ai_provider` 读模型列表，保证向前兼容。

---

## 四、实现链路

### 4.1 同步调用：从请求到响应

`AiClientImpl.call()` 的执行流程：

```
1. AiInvocationResolver.resolve(request)
   ├── 定位 AiAgent（通过 agentCode）
   ├── 选择 AiProvider（请求级 > Agent 绑定 > 全局默认）
   ├── 校验 status、baseUrl、apiKey（缺一抛 BusinessException）
   ├── 确定模型名称、temperature、maxTokens（三层优先级）
   └── 返回 AiInvocation（封装了完整的调用参数）

2. ChatClientCache.getOrCreate(providerId, modelName, temperature, maxTokens)
   ├── 命中缓存 → 直接返回 ChatClient
   └── 未命中 → buildBaseChatClient() 新建并缓存

3. CircuitBreaker.tryAcquire(agentCode)
   ├── 熔断中 → 直接返回 fallback 响应
   └── 正常 → 放行

4. chatClient.prompt().system(...).user(...).call()

5. CircuitBreaker.record(agentCode, success/failure)
   └── 连续失败 3 次 → 熔断 5 分钟

6. 持久化对话记录 → 返回 AiClientResponse
```

### 4.2 ChatClient 缓存

缓存由 Caffeine 实现，Key 为 `providerId:modelName:temperature:maxTokens`：

- 最大 50 个 ChatClient 实例
- TTL 30 分钟（过期自动重建）
- 供应商配置变更时主动驱逐：`evictByProvider(providerId)`

这个设计让相同配置的请求直接复用已建立的 HTTP 连接池，避免每次调用都重建 `OpenAiApi` 实例。

### 4.3 熔断器

熔断按 `agentCode` 粒度，连续失败 3 次即开启熔断，5 分钟后自动进入半开状态：

```java
public class CircuitBreaker {
    private static final int FAILURE_THRESHOLD = 3;
    private static final long RECOVERY_MILLIS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, AtomicInteger> failureCount;
    private final ConcurrentHashMap<String, Long> openTime;
}
```

熔断期间，`call()` 和 `stream()` 不发起真实 API 调用，直接返回 `AiFallbackReason` 标记的错误响应，前端据此展示降级提示。

### 4.4 流式输出

`stream()` 方法返回 `Flux<String>`，通过 SSE 推送给前端：

| event | data | 说明 |
|---|---|---|
| `progress` | `{"stage":"connecting","message":"正在连接..."}` | 连接阶段 |
| `chunk` | `{"content":"文本块"}` | 逐块输出 |
| `complete` | `{"sessionId":"xxx","message":"生成完成"}` | 结束 |
| `error` | `{"message":"错误信息"}` | 异常 |

对于 DeepSeek-R1 等推理模型，流式输出会自动分离思考过程（`reasoning_content`）与正式回复，用分隔符区分：

```
==================== 思考过程 ====================
（模型推理链逐块输出）
==================== 完整回复 ====================
（正式回复逐块输出）
```

---

## 五、设计取舍

### 5.1 为什么复用 Spring AI 而不是自建接口？

**选 Spring AI 的理由**：

- Spring AI 已在 1.0 正式版中稳定了 `ChatModel`、`ChatClient`、`StreamingChatModel` 等核心抽象，自建接口无异于重复造轮子
- `OpenAiApi` 底层基于 `WebClient`，连接池、超时、重试等基础设施 Spring 已打磨成熟
- 社区生态：后续如需接入向量存储（`VectorStore`）、函数调用（`FunctionCallback`）、MCP 协议，Spring AI 均有现成支持

**但也不是无脑拿来用**。Forge 在 Spring AI 之上封装了：

- **三层优先级路由**（Spring AI 本身只关心 `ChatOptions`，不关心从哪来）
- **缓存 + 熔断**（Spring AI 没有现成的 ChatClient 缓存和业务级熔断）
- **供应商管理后台**（Spring AI 是代码级配置，Forge 做到了数据库驱动）

### 5.2 为什么选择 OpenAI 协议作为统一标准？

一句话：**目前兼容 OpenAI Chat Completions API 的供应商最多**。

| 供应商 | 原生协议 | 是否兼容 OpenAI 协议 |
|---|---|---|
| OpenAI | OpenAI | — |
| DeepSeek | OpenAI 兼容 | 是 |
| 智谱 GLM | OpenAI 兼容 | 是 |
| 通义千问 | OpenAI 兼容（/compatible-mode） | 是 |
| Moonshot | OpenAI 兼容 | 是 |
| Ollama | OpenAI 兼容 | 是 |

这意味着一个 `OpenAiApi` 能覆盖 90% 的接入需求。唯一的例外是 Anthropic Claude（原生 Messages API 与 OpenAI 不兼容），若有需求可以通过策略模式扩展，在 `ChatClientCache.buildBaseChatClient()` 中根据 `provider_type` 路由到不同的 `ChatModel` 构建逻辑。

---

## 六、二开指南

### 6.1 添加新供应商

如果新供应商兼容 OpenAI 协议（绝大多数情况），**零代码**：

1. 进入管理后台 → AI 供应商管理 → 新增
2. 选择"自定义"模板
3. 填写 `Base URL` 和 `API Key`
4. 添加模型 → 设为默认 → 启用

如果供应商使用非 OpenAI 协议（如 Anthropic 原生协议），需要：

```java
// 在 ChatClientCache 中增加分支
private ChatClient buildBaseChatClient(AiProvider provider, OpenAiChatOptions options) {
    if ("anthropic".equals(provider.getProviderType())) {
        AnthropicApi api = AnthropicApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        return ChatClient.builder(new AnthropicChatModel(api)).build();
    }
    // 默认走 OpenAI 兼容
    OpenAiApi api = OpenAiApi.builder()...
}
```

### 6.2 自定义流式输出格式

流式输出在 `AiClientImpl.stream()` 中处理，要改变输出格式只需调整 `.concatMap()` 中的映射逻辑：

```java
.concatMap(chatResponse -> {
    // 默认输出：按 chunk 逐个推送
    // 如果要增加 token 计数、耗时统计等元信息：
    return Flux.just(JsonUtils.toJson(Map.of(
        "content", content,
        "tokens", chatResponse.getMetadata().getUsage(),
        "model", chatResponse.getMetadata().getModel()
    )));
})
```

### 6.3 扩展熔断策略

当前按 `agentCode` 粒度的熔断可通过修改 CircuitBreaker 调整：

- **改为按 providerId 熔断**：同一供应商的所有 Agent 共享熔断状态，避免某个 Agent 反复触发而其他 Agent 仍被阻塞
- **增加 Failover**：熔断后自动尝试备用供应商：

```java
public AiProvider failover(Long failedProviderId) {
    return providerService.listByStatus("0").stream()
        .filter(p -> !p.getId().equals(failedProviderId))
        .findFirst()
        .orElseThrow(() -> new BusinessException("无可用供应商"));
}
```

- **接入 Spring Retry**：在 `call()` 方法上添加 `@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))` 实现声明式重试

---

## 七、体验与预告

### 项目入口

| 入口 | 地址 |
|---|---|
| 管理后台 | [http://81.70.22.48:8084/forge/login](http://81.70.22.48:8084/forge/login) |
| 项目文档 | [http://81.70.22.48:8084/forge-docs/](http://81.70.22.48:8084/forge-docs/) |
| 大屏设计器 | [http://81.70.22.48:8084/forge-report/](http://81.70.22.48:8084/forge-report/) |
| Gitee | [https://gitee.com/ForgeLab/forge-admin](https://gitee.com/ForgeLab/forge-admin) |
| GitHub | [https://github.com/yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin) |

### 下一篇预告：C06 — AI 生成数据大屏

供应商到位后，怎么让它真正干活？下一篇将拆解 Forge 的"一句话生成大屏"全链路：从自然语言需求到组件树 JSON，再到主题配色和数据集自动绑定。你将看到 AI 如何理解"做一个本季度部门业绩对比看板"并输出可直接渲染的大屏。

---

*C05 完。下一篇：C06《AI 生成数据大屏：从一句话到一个可视化看板》。*
