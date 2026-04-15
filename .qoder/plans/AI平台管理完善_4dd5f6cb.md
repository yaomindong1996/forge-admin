# AI 平台管理完善

## 背景与现状分析

### 已有基础
- `ai_provider` 表 + CRUD Controller：供应商基础管理已完成，缺少"测试连接"接口和预设模板
- `ai_agent` 表 + CRUD Controller：Agent 管理已完成
- `AiChatService`：基于 OpenAI 兼容接口动态构建，已支持多供应商切换
- `ai_chat_record` 表：表结构已建，**但没有后端写入逻辑和查询接口**
- `AiChatController`：有 `/chat/stream` 接口，**没有携带 sessionId 和上下文**
- `forge-report-ui`：有 AI 对话面板，没有供应商设置入口

### 待补充的 Gap
1. 供应商管理缺：测试连接、设为默认接口、预设模板数据
2. 会话记录：缺后端写入和查询 API
3. 多轮上下文：缺 sessionId 传递、`ChatMemory` 持久化实现
4. forge-report-ui：缺供应商设置 UI

---

## 数据库变更

### 1. `ai_provider` 表新增字段
文件：`forge/forge-report/src/main/sql/alter.sql`（新建）
```sql
-- 供应商预设类型枚举（存放在代码层即可，表结构不变）
-- 新增: default_model 字段（供应商默认模型名）
ALTER TABLE ai_provider ADD COLUMN `default_model` VARCHAR(100) DEFAULT 'gpt-3.5-turbo' COMMENT '默认模型名' AFTER `models`;
```

### 2. `ai_chat_session` 表（新建）
```sql
CREATE TABLE `ai_chat_session` (
  `id`          VARCHAR(64) NOT NULL COMMENT '会话ID（UUID）',
  `tenant_id`   BIGINT NOT NULL DEFAULT 0,
  `user_id`     BIGINT NOT NULL COMMENT '用户ID',
  `agent_code`  VARCHAR(50) DEFAULT NULL,
  `session_name` VARCHAR(200) DEFAULT NULL COMMENT '会话标题（首条消息截取）',
  `status`      CHAR(1) NOT NULL DEFAULT '0' COMMENT '0正常 1已删除',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';
```

### 3. `ai_chat_record` 表补充索引
```sql
-- 表已存在，补充 tenant_id 索引
ALTER TABLE ai_chat_record ADD KEY `idx_tenant_id` (`tenant_id`);
```

---

## Task 1：后端 - 供应商管理增强

文件：`forge/forge-report/src/main/java/com/mdframe/forge/report/ai/provider/`

**1.1 `AiProvider` 实体新增字段**
- 新增 `defaultModel` 字段

**1.2 `AiProviderController` 新增接口**
```java
// 测试连接（发送一条简单消息，验证 API Key + Base URL 可用）
POST /goview/ai/provider/test

// 设为默认供应商
PUT /goview/ai/provider/{id}/default
```

**1.3 `AiProviderService` 新增方法**
- `testConnection(AiProvider provider)` - 构建临时 OpenAiChatModel，发 "hi" 测试，超时 5s
- `setDefault(Long id)` - 清除其他 isDefault=1，设当前为默认

**1.4 预设供应商模板接口**
```java
// 返回内置模板列表（不涉及数据库，纯代码枚举）
GET /goview/ai/provider/templates
```
返回结构（代码中硬编码列表）：
```json
[
  {"templateKey":"alibaba","name":"阿里百炼","baseUrl":"https://dashscope.aliyuncs.com/compatible-mode","defaultModel":"qwen-plus"},
  {"templateKey":"openai","name":"OpenAI","baseUrl":"https://api.openai.com","defaultModel":"gpt-4o-mini"},
  {"templateKey":"zhipu","name":"智谱 AI","baseUrl":"https://open.bigmodel.cn/api/paas/v4","defaultModel":"glm-4"},
  {"templateKey":"moonshot","name":"Moonshot","baseUrl":"https://api.moonshot.cn/v1","defaultModel":"moonshot-v1-8k"},
  {"templateKey":"ollama","name":"Ollama（本地）","baseUrl":"http://localhost:11434","defaultModel":"llama3"},
  {"templateKey":"custom","name":"自定义","baseUrl":"","defaultModel":""}
]
```

---

## Task 2：后端 - AI 会话与多轮上下文

**2.1 新建 `ai/session/` 包**
- `AiChatSession` 实体（对应 `ai_chat_session` 表）
- `AiChatSessionMapper`
- `AiChatSessionService`
  - `getOrCreate(String sessionId, Long userId, String agentCode)` - 不存在则创建
  - `listByUser(Long userId)` - 用户历史会话列表
  - `listMessages(String sessionId)` - 某会话所有消息（分页）

**2.2 `AiChatRecord` 实体完善**
文件：新建 `ai/chat/domain/AiChatRecord.java`（表已有，缺实体类）
```java
@TableName("ai_chat_record")
public class AiChatRecord extends BaseEntity {
    private Long userId;
    private String agentCode;
    private String sessionId;
    private String role;       // user/assistant/system
    private String content;
    private Integer tokenUsage;
}
```

**2.3 持久化 ChatMemory 实现**
新建 `ai/chat/memory/DbChatMemory.java`，实现 Spring AI 的 `ChatMemory` 接口：
```java
public class DbChatMemory implements ChatMemory {
    // add() -> 写入 ai_chat_record
    // get() -> 按 sessionId 查最近 N 条消息
    // clear() -> 物理删除 sessionId 下所有记录
}
```

**2.4 修改 `AiChatService`**
- 构造函数注入 `DbChatMemory`
- `chatStream` 方法新增 `sessionId`、`userId` 参数
- 使用 `ChatClient.prompt().advisors(new MessageChatMemoryAdvisor(chatMemory, sessionId))` 携带历史上下文
- 流式输出完成后（`doOnComplete`）将本轮 user/assistant 消息写入 `AiChatRecord`

**2.5 修改 `AiChatController`**
```java
// ChatRequest 新增字段
String sessionId;  // 前端生成 UUID，同一会话始终传同一个

// 新增查询接口
GET  /goview/ai/session/list              // 当前用户会话列表
GET  /goview/ai/session/{sessionId}/messages  // 会话消息记录
DELETE /goview/ai/session/{sessionId}    // 删除会话
```

---

## Task 3：forge-report-ui - 供应商设置界面

位置：新建 `src/views/project/settings/AiSettings.vue`（在项目列表页头部工具栏增加"AI设置"入口）

**3.1 供应商设置弹窗**
- 打开时调 `GET /goview-api/goview/ai/provider/page` 加载已配置列表
- 调 `GET /goview-api/goview/ai/provider/templates` 加载预设模板列表
- 选择模板时自动填充 Name / BaseURL / DefaultModel
- 用户只需填入 API Key
- 支持"测试连接"按钮（调 `POST /goview-api/goview/ai/provider/test`）
- 支持"设为默认"操作

**3.2 新增 API 文件**
文件：`src/api/ai/index.ts`
```typescript
export const getProviderTemplatesApi = () => get('/goview-api/goview/ai/provider/templates')
export const getProviderPageApi = (params?) => get('/goview-api/goview/ai/provider/page', params)
export const createProviderApi = (data) => post('/goview-api/goview/ai/provider', data)
export const updateProviderApi = (data) => put('/goview-api/goview/ai/provider', data)
export const deleteProviderApi = (id) => del(`/goview-api/goview/ai/provider/${id}`)
export const testProviderApi = (data) => post('/goview-api/goview/ai/provider/test', data)
export const setDefaultProviderApi = (id) => put(`/goview-api/goview/ai/provider/${id}/default`)
```

**3.3 项目列表页增加入口**
文件：`src/views/project/layout/components/ProjectLayoutCreate/index.vue`（或头部工具栏区域）
- 在右上角增加"AI 设置"图标按钮，点击打开 `AiSettings` 弹窗

---

## Task 4：SQL 脚本整合

将所有 DDL 变更写入：
- `forge/forge-report/src/main/sql/alter.sql`（新建，幂等性 ALTER 语句）
- `forge/forge-report/src/main/sql/init.sql`（补充 `ai_chat_session` 建表和 `ai_chat_record` 表索引）

---

## 接口汇总

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 供应商 | GET | `/goview/ai/provider/templates` | 预设模板 |
| 供应商 | POST | `/goview/ai/provider/test` | 测试连接 |
| 供应商 | PUT | `/goview/ai/provider/{id}/default` | 设为默认 |
| 会话 | GET | `/goview/ai/session/list` | 当前用户会话列表 |
| 会话 | GET | `/goview/ai/session/{sessionId}/messages` | 消息明细 |
| 会话 | DELETE | `/goview/ai/session/{sessionId}` | 删除会话 |
| 对话 | POST (SSE) | `/goview/ai/chat/stream` | 新增 sessionId 字段 |

---

## 风险与边界

- `MessageChatMemoryAdvisor` 需确认 spring-ai 版本（1.0.0.2）中是否可用，否则手动拼装历史消息
- `ai_chat_record` 的 content 字段是 LONGTEXT，流式输出需等流完成后统一写入，不逐 chunk 写
- forge-report-ui 不做"会话管理"页面，仅做供应商设置
