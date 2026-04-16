# AI 插件（forge-plugin-ai）

AI 能力插件，集成 Spring AI，支持多 AI 提供商（通义千问、OpenAI、智谱、月之暗面等），提供 Agent 管理、Chat 会话和流式 SSE 响应。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-plugin-ai</artifactId>
</dependency>
```

### 2. 配置 AI 提供商

在 `ai_provider` 表中添加提供商配置：

| 字段 | 说明 |
|------|------|
| `provider_name` | 提供商名称 |
| `provider_type` | 类型（openai / azure / dashscope / ollama） |
| `api_key` | API Key |
| `base_url` | API 基础地址 |
| `models` | 支持的模型列表（JSON） |
| `default_model` | 默认模型 |
| `is_default` | 是否为默认提供商 |

## 支持的平台

| 提供商 | `provider_type` | 说明 |
|--------|-----------------|------|
| 通义千问（DashScope） | `dashscope` | 阿里大模型 |
| OpenAI | `openai` | GPT-4/GPT-3.5 |
| 智谱 AI | `openai` | 兼容 OpenAI 接口 |
| 月之暗面 | `openai` | 兼容 OpenAI 接口 |
| Ollama | `ollama` | 本地部署模型 |
| Azure OpenAI | `azure` | Azure 托管 OpenAI |

## 核心功能

### Agent 管理

Agent 是预配置了系统提示词和模型的 AI 助手：

**数据库表**：`ai_agent`

| 字段 | 说明 |
|------|------|
| `agent_name` | Agent 名称 |
| `agent_code` | Agent 编码（唯一标识） |
| `description` | 描述 |
| `system_prompt` | 系统提示词 |
| `provider_id` | 关联的提供商 ID |
| `model_name` | 使用的模型 |
| `temperature` | 温度参数（0~1） |
| `max_tokens` | 最大输出 Token 数 |
| `extra_config` | 额外配置（JSON） |
| `status` | 状态 |

### Chat 会话

#### 流式对话

```java
@Autowired
private AiChatService aiChatService;

// 流式 Chat（返回 Flux<String>）
Flux<String> stream = aiChatService.chatStream(
    "你好，请帮我分析一下这个数据",
    "data-analyst",    // agentCode
    sessionId,         // 可为空，为空时自动创建
    userId,
    null,              // providerId（null 时使用 Agent 默认配置）
    null,              // modelName
    null,              // temperature
    null,              // maxTokens
    null,              // projectName
    null               // canvasContext
);
```

#### 参数解析优先级

所有 Chat 参数都遵循以下回退链：

```
显式传入 > Agent 配置 > 提供商配置 > 默认值
```

### 多轮上下文

`DbChatMemory` 实现了 Spring AI 的 `ChatMemory` 接口，基于数据库 `ai_chat_record` 表存储对话历史，默认保留最近 20 条消息作为上下文。

### Dashboard 生成

```java
// 非流式
String dashboard = aiChatService.generateDashboard(AIGenerateRequest);

// 流式
Flux<String> stream = aiChatService.generateDashboardStream(AIGenerateRequest);
```

## REST API

### Chat 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat/stream` | 流式对话（SSE） |
| POST | `/api/ai/dashboard` | 生成 Dashboard |
| POST | `/api/ai/dashboard/stream` | 流式生成 Dashboard |

### Agent 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/agent/page` | 分页查询 Agent |
| GET | `/api/ai/agent/{id}` | 获取 Agent 详情 |
| POST | `/api/ai/agent` | 创建 Agent |
| PUT | `/api/ai/agent` | 更新 Agent |
| DELETE | `/api/ai/agent/{id}` | 删除 Agent |

### Provider 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/provider/page` | 分页查询 Provider |
| GET | `/api/ai/provider/{id}` | 获取 Provider 详情 |
| POST | `/api/ai/provider` | 创建 Provider |
| PUT | `/api/ai/provider` | 更新 Provider |
| DELETE | `/api/ai/provider/{id}` | 删除 Provider |

## 数据库表

| 表名 | 说明 |
|------|------|
| `ai_provider` | AI 提供商配置 |
| `ai_agent` | AI Agent 配置 |
| `ai_chat_session` | Chat 会话 |
| `ai_chat_record` | Chat 消息记录 |

## 核心组件

| 组件 | 说明 |
|------|------|
| `AiChatService` | 核心 Chat 服务，支持流式/非流式、Dashboard 生成 |
| `AiChatRecordService` | Chat 记录持久化 |
| `AiChatSessionService` | 会话管理（getOrCreate） |
| `DbChatMemory` | 基于数据库的 Chat Memory（实现 Spring AI `ChatMemory`） |
| `AiPromptTemplateRenderer` | Dashboard 系统提示词渲染 |
| `AiAgentService` | Agent 管理服务 |
| `AiProviderService` | Provider 管理服务 |

## DTO

### `ChatRequest`

| 字段 | 说明 |
|------|------|
| `content` | 用户输入内容 |
| `agentCode` | Agent 编码 |
| `sessionId` | 会话 ID |
| `providerId` | 指定提供商 ID（覆盖 Agent 默认值） |
| `modelName` | 指定模型（覆盖默认值） |
| `temperature` | 温度参数 |
| `maxTokens` | 最大 Token |
| `projectName` | 项目名称 |
| `canvasContext` | Canvas 上下文 |

### `AIGenerateRequest`

| 字段 | 说明 |
|------|------|
| `sessionId` | 会话 ID |
| `prompt` | 提示词 |
| `style` | 样式风格 |
| `canvasWidth` | Canvas 宽度 |
| `canvasHeight` | Canvas 高度 |
| `componentCatalog` | 组件目录 |
| `projectName` | 项目名称 |
| `canvasContext` | Canvas 上下文 |
