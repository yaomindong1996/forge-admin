# AI能力数据设计

<cite>
**本文引用的文件**
- [V1.0.87__add_ai_knowledge_rag.sql](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql)
- [V1.0.88__add_agent_engine_event_skill.sql](file://forge-server/db/migration/V1.0.88__add_agent_engine_event_skill.sql)
- [V1.0.124__enhance_ai_chat_message_structure.sql](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql)
- [V1.0.18__add_ai_model_routing_governance.sql](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql)
- [V1.0.21__add_ai_capability_control_plane.sql](file://forge-server/db/migration/V1.0.21__add_ai_capability_control_plane.sql)
- [AiModel.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java)
- [PolicyBasedAiModelRouter.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/PolicyBasedAiModelRouter.java)
- [RouteDecision.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/RouteDecision.java)
- [AiClientImpl.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java)
- [DbChatMemory.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/memory/DbChatMemory.java)
- [DocumentProcessService.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java)
- [MilvusVectorStoreService.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/MilvusVectorStoreService.java)
- [VectorStoreService.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreService.java)
- [VectorStoreFactory.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreFactory.java)
- [AiStoreInstance.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/domain/AiStoreInstance.java)
- [AiPromptTemplateVO.java](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/prompt/vo/AiPromptTemplateVO.java)
- [agent.vue](file://forge-admin-ui/src/views/ai/agent.vue)
- [agent-chat.vue](file://forge-admin-ui/src/views/ai/agent-chat.vue)
</cite>

## 目录
1. [引言](#引言)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能考虑](#性能考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 引言
本文件面向Forge Admin的AI能力中心，聚焦“数据设计”视角，系统化梳理以下领域的数据模型与存储方案：
- AI智能体管理：智能体配置、工具集成、技能定义。
- AI供应商与模型：连接信息、API密钥、模型能力与路由治理。
- 会话管理：对话历史、消息记录、上下文持久化。
- 提示词模板库：分类、变量、版本管理。
- AI知识库：文档存储、向量检索、知识分片。
- 模型路由治理：选择规则、负载均衡、故障转移。
- 性能优化与安全策略：索引、审计、敏感信息保护等。

## 项目结构
仓库中与AI相关的数据设计与实现主要分布在以下位置：
- 数据库迁移脚本：位于 forge-server/db/migration，涵盖Agent、技能、知识库、会话、路由治理等表结构与字典初始化。
- Java实体与服务：位于 forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai，包含模型、路由、会话记忆、知识库处理、向量存储适配等。
- 前端视图：位于 forge-admin-ui/src/views/ai，体现Agent配置、会话交互、提示词模板管理等UI侧字段映射。

```mermaid
graph TB
subgraph "数据库层"
DB["MySQL<br/>迁移脚本"]
end
subgraph "服务层"
Router["模型路由<br/>PolicyBasedAiModelRouter"]
Memory["会话记忆<br/>DbChatMemory"]
DocProc["知识库处理<br/>DocumentProcessService"]
VStore["向量存储抽象<br/>VectorStoreService"]
StoreInst["存储实例<br/>AiStoreInstance"]
end
subgraph "外部系统"
VectorDB["向量数据库<br/>Milvus/PgVector/ES"]
LLM["大模型供应商"]
end
DB --> Router
DB --> Memory
DB --> DocProc
DocProc --> VStore
VStore --> VectorDB
Router --> LLM
Memory --> DB
```

图表来源
- [V1.0.18__add_ai_model_routing_governance.sql:1-192](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L1-L192)
- [V1.0.87__add_ai_knowledge_rag.sql:85-140](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql#L85-L140)
- [V1.0.124__enhance_ai_chat_message_structure.sql:1-116](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L1-L116)

章节来源
- [V1.0.18__add_ai_model_routing_governance.sql:1-192](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L1-L192)
- [V1.0.87__add_ai_knowledge_rag.sql:85-140](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql#L85-L140)
- [V1.0.124__enhance_ai_chat_message_structure.sql:1-116](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L1-L116)

## 核心组件
- 智能体与技能：通过Agent配置绑定工具与技能，支持权限控制与分组激活。
- 供应商与模型：维护模型元数据、能力、价格与上下文窗口；支持路由策略与调用审计。
- 会话与消息：结构化消息记录（含思考过程、用量、附件、状态），工具调用明细独立表。
- 提示词模板：模板元数据、分类、标签、使用统计与推荐标记。
- 知识库与向量检索：文档分块、向量维度校验、集合创建与BM25稀疏向量索引。
- 路由治理：显式候选、能力匹配、健康门控、确定性排序与调用日志。

章节来源
- [V1.0.88__add_agent_engine_event_skill.sql:105-165](file://forge-server/db/migration/V1.0.88__add_agent_engine_event_skill.sql#L105-L165)
- [V1.0.18__add_ai_model_routing_governance.sql:23-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L23-L119)
- [V1.0.124__enhance_ai_chat_message_structure.sql:92-116](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L92-L116)
- [AiPromptTemplateVO.java:1-63](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/prompt/vo/AiPromptTemplateVO.java#L1-L63)
- [V1.0.87__add_ai_knowledge_rag.sql:85-112](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql#L85-L112)

## 架构总览
下图展示AI能力中心在数据层面的关键交互：Agent通过路由策略选择模型，调用过程中记录治理日志；会话消息持久化到数据库；知识库将文档分块并写入向量数据库，同时保留分块元数据于关系型数据库。

```mermaid
sequenceDiagram
participant UI as "前端界面"
participant Agent as "智能体"
participant Router as "模型路由"
participant Model as "模型/供应商"
participant Session as "会话记忆"
participant KB as "知识库处理"
participant VDB as "向量数据库"
UI->>Agent : 发送用户请求
Agent->>Router : 根据策略选择模型
Router-->>Agent : 返回决策(提供商/模型/原因)
Agent->>Model : 发起调用(流式/同步)
Model-->>Agent : 返回结果/错误
Agent->>Session : 持久化消息与用量
Agent->>KB : 可选RAG检索
KB->>VDB : 向量/BM25检索
VDB-->>KB : 返回相似片段
Agent-->>UI : 返回最终响应
```

图表来源
- [PolicyBasedAiModelRouter.java:1-31](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/PolicyBasedAiModelRouter.java#L1-L31)
- [AiClientImpl.java:403-418](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java#L403-L418)
- [DocumentProcessService.java:322-392](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java#L322-L392)
- [MilvusVectorStoreService.java:132-181](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/MilvusVectorStoreService.java#L132-L181)

## 详细组件分析

### 智能体、工具与技能数据模型
- 智能体工具绑定表：记录每个Agent启用的工具来源、标识、分组与启用状态，支持唯一键约束与逻辑删除。
- 智能体工具权限表：对工具访问进行ALLOW/ASK/DENY控制，按Agent与工具键建立唯一约束。
- 技能包与文件：技能主表与文件表分离，支持多文件内容存储与编码；Agent与技能多对多绑定。

```mermaid
erDiagram
AI_AGENT ||--o{ AI_AGENT_TOOL_CONFIG : "绑定"
AI_AGENT ||--o{ AI_AGENT_TOOL_PERMISSION : "权限"
AI_AGENT ||--o{ AI_AGENT_SKILL : "绑定"
AI_SKILL ||--o{ AI_SKILL_FILE : "包含文件"
AI_AGENT {
bigint id PK
bigint tenant_id
varchar agent_code UK
varchar agent_name
text system_prompt
varchar model_selection_mode
bigint route_policy_id
char status
bigint del_flag
}
AI_AGENT_TOOL_CONFIG {
bigint id PK
bigint tenant_id
bigint agent_id FK
varchar tool_source
varchar tool_key
varchar tool_group
char enabled
bigint del_flag
}
AI_AGENT_TOOL_PERMISSION {
bigint id PK
bigint tenant_id
bigint agent_id FK
varchar tool_key
varchar decision
bigint del_flag
}
AI_SKILL {
bigint id PK
bigint tenant_id
varchar skill_code UK
varchar skill_name
varchar version
char status
bigint del_flag
}
AI_SKILL_FILE {
bigint id PK
bigint tenant_id
bigint skill_id FK
varchar file_path
longtext file_content
varchar encoding
bigint del_flag
}
AI_AGENT_SKILL {
bigint id PK
bigint tenant_id
bigint agent_id FK
bigint skill_id FK
bigint del_flag
}
```

图表来源
- [V1.0.88__add_agent_engine_event_skill.sql:105-165](file://forge-server/db/migration/V1.0.88__add_agent_engine_event_skill.sql#L105-L165)

章节来源
- [V1.0.88__add_agent_engine_event_skill.sql:105-165](file://forge-server/db/migration/V1.0.88__add_agent_engine_event_skill.sql#L105-L165)

### 供应商与模型数据模型
- 模型实体：包含供应商ID、模型类型、模型标识、显示名称、最大Token、上下文窗口、输入输出单价、图标、默认标记、状态与排序。
- 模型能力：以能力代码描述模型可路由能力（如流式、推理、工具调用、视觉、结构化输出）。
- 路由策略与目标：策略定义所需能力与显式候选模型优先级；目标表维护策略与模型的关联及顺序。
- 调用审计：记录请求ID、租户/用户/Agent/会话、路由来源与原因、提供商/模型/适配器、结果、错误分类、HTTP状态、耗时、Token用量与价格快照。

```mermaid
classDiagram
class AiModel {
+Long id
+Long providerId
+String modelType
+String modelId
+String modelName
+Integer maxTokens
+Integer contextWindow
+Long inputPricePerMillionCent
+Long outputPricePerMillionCent
+String icon
+String isDefault
+String status
+Integer sortOrder
+Long delFlag
}
class AiModelCapability {
+Long id
+Long tenantId
+Long modelId
+String capabilityCode
+String configJson
+String status
+Long delFlag
}
class AiModelRoutePolicy {
+Long id
+Long tenantId
+String policyCode
+String policyName
+String requiredCapabilities
+String status
+String remark
+Long delFlag
}
class AiModelRouteTarget {
+Long id
+Long tenantId
+Long policyId
+Long modelId
+Integer priority
+String status
+Long delFlag
}
AiModelCapability --> AiModel : "属于"
AiModelRouteTarget --> AiModelRoutePolicy : "属于"
AiModelRouteTarget --> AiModel : "指向"
```

图表来源
- [AiModel.java:1-92](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java#L1-L92)
- [V1.0.18__add_ai_model_routing_governance.sql:23-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L23-L119)

章节来源
- [AiModel.java:1-92](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java#L1-L92)
- [V1.0.18__add_ai_model_routing_governance.sql:23-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L23-L119)

### 会话管理与消息持久化
- 会话记忆：基于数据库的ChatMemory实现，读取最近N条消息构建上下文，支持清空会话。
- 消息增强：为消息表增加思考过程、用量JSON、附件JSON、状态、中断ID、错误信息、更新时间与逻辑删除标志；新增工具调用明细表，按消息行记录每次工具调用的参数与结果。
- 前端交互：会话列表与消息分页加载，支持上滑加载更多、切换会话、删除会话等操作。

```mermaid
flowchart TD
Start(["开始"]) --> LoadMsg["加载最近一页消息"]
LoadMsg --> HasMore{"是否还有更早消息?"}
HasMore -- 否 --> End(["结束"])
HasMore -- 是 --> LoadMore["以最早recordId为游标加载更多"]
LoadMore --> Prepend["头插旧消息并保持滚动锚点"]
Prepend --> End
```

图表来源
- [DbChatMemory.java:1-93](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/memory/DbChatMemory.java#L1-L93)
- [V1.0.124__enhance_ai_chat_message_structure.sql:14-116](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L14-L116)
- [agent-chat.vue:404-569](file://forge-admin-ui/src/views/ai/agent-chat.vue#L404-L569)

章节来源
- [DbChatMemory.java:1-93](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/memory/DbChatMemory.java#L1-L93)
- [V1.0.124__enhance_ai_chat_message_structure.sql:14-116](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L14-L116)
- [agent-chat.vue:404-569](file://forge-admin-ui/src/views/ai/agent-chat.vue#L404-L569)

### 提示词模板库数据模型
- 模板元数据：名称、编码、适用场景、业务分类、领域分类、标签、描述、内容摘要、示例输入、状态、推荐标记、排序、使用/测试/下载次数、备注。
- 前端筛选：支持关键词、适用场景、业务/领域分类、状态、推荐等多维筛选。
- 导出与统计：支持Markdown导出模板内容，统计使用/测试/下载次数。

```mermaid
classDiagram
class AiPromptTemplateVO {
+Long id
+Long tenantId
+String templateName
+String templateCode
+String usageScene
+String businessCategory
+String domainCategory
+String templateTags
+String description
+String contentSummary
+String exampleInput
+String status
+String isRecommended
+Integer sortOrder
+Integer useCount
+Integer testCount
+Integer downloadCount
+String remark
+Long createBy
+String creatorName
+LocalDateTime createTime
+Long updateBy
+String updaterName
+LocalDateTime updateTime
}
```

图表来源
- [AiPromptTemplateVO.java:1-63](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/prompt/vo/AiPromptTemplateVO.java#L1-L63)

章节来源
- [AiPromptTemplateVO.java:1-63](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/prompt/vo/AiPromptTemplateVO.java#L1-L63)

### AI知识库数据模型与向量检索
- 知识库分块：记录租户、知识库ID、文档ID、分块序号、内容、标题、Token数、向量ID、内容哈希、检索次数等；唯一键保证同一文档内分块不重复。
- 向量存储实例：维护向量存储/搜索引擎实例的名称、类别、类型、连接配置JSON、状态与逻辑删除。
- 向量服务抽象：提供集合创建、插入、删除、向量检索与BM25全文检索接口；工厂按类型路由到具体实现（当前Milvus已实现）。
- 处理流程：批量嵌入后按实际维度创建集合，保存分块到数据库并写入向量库，支持强制重建以应对维度不一致。

```mermaid
sequenceDiagram
participant Proc as "文档处理服务"
participant Embed as "Embedding模型"
participant VStore as "向量存储服务"
participant VDB as "向量数据库"
participant DB as "关系数据库"
Proc->>Embed : 批量生成文本向量
Embed-->>Proc : 返回向量与维度
Proc->>VStore : 创建集合(维度/配置/强制重建)
VStore->>VDB : 建集/建索引(稠密+稀疏BM25)
Proc->>DB : 保存分块元数据与向量ID
Proc->>VStore : 插入向量
VStore->>VDB : 写入向量数据
```

图表来源
- [DocumentProcessService.java:322-392](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java#L322-L392)
- [MilvusVectorStoreService.java:132-181](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/MilvusVectorStoreService.java#L132-L181)
- [VectorStoreService.java:1-50](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreService.java#L1-L50)
- [VectorStoreFactory.java:1-44](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreFactory.java#L1-L44)
- [AiStoreInstance.java:1-56](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/domain/AiStoreInstance.java#L1-L56)
- [V1.0.87__add_ai_knowledge_rag.sql:85-140](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql#L85-L140)

章节来源
- [V1.0.87__add_ai_knowledge_rag.sql:85-140](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql#L85-L140)
- [DocumentProcessService.java:322-392](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java#L322-L392)
- [MilvusVectorStoreService.java:132-181](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/MilvusVectorStoreService.java#L132-L181)
- [VectorStoreService.java:1-50](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreService.java#L1-L50)
- [VectorStoreFactory.java:1-44](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreFactory.java#L1-L44)
- [AiStoreInstance.java:1-56](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/domain/AiStoreInstance.java#L1-L56)

### 模型路由治理数据结构
- 路由策略：租户内唯一策略码，声明所需能力与显式候选模型优先级。
- 候选过滤与选择：仅从策略显式候选中选择，要求模型与供应商启用、未逻辑删除、能力满足；排序固定为priority升序、目标ID升序。
- 健康门控：UNKNOWN/HEALTHY/DEGRADED可被选中，OPEN熔断禁止新请求；HALF_OPEN允许一次试探调用。
- 调用审计：记录路由来源、策略、提供商/模型、适配器、结果、错误分类、HTTP状态、耗时、Token与价格快照。

```mermaid
flowchart TD
A["接收路由请求"] --> B["加载策略与候选"]
B --> C{"能力匹配?"}
C -- 否 --> D["排除候选"]
C -- 是 --> E{"健康状态检查"}
E -- OPEN --> F["跳过该候选"]
E -- 其他 --> G["进入候选队列"]
G --> H["按priority/id排序"]
H --> I["选择首个可用候选"]
I --> J["记录调用审计日志"]
```

图表来源
- [V1.0.18__add_ai_model_routing_governance.sql:42-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L42-L119)
- [PolicyBasedAiModelRouter.java:1-31](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/PolicyBasedAiModelRouter.java#L1-L31)
- [RouteDecision.java:1-11](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/RouteDecision.java#L1-L11)

章节来源
- [V1.0.18__add_ai_model_routing_governance.sql:42-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L42-L119)
- [PolicyBasedAiModelRouter.java:1-31](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/PolicyBasedAiModelRouter.java#L1-L31)
- [RouteDecision.java:1-11](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/RouteDecision.java#L1-L11)

### 能力中枢控制面数据模型
- 能力目录与版本：能力元数据、协议工具名、来源、版本、行为、风险等级、可见性、发布状态；不可变版本记录输入/输出Schema与策略快照。
- 机器客户端与授权：客户端代码、密钥ID与前缀、哈希、服务用户、组织、状态、过期时间；授权记录版本策略、固定版本、字段策略与有效期。
- 安全调用日志：记录请求ID、客户端/能力/版本、角色/用户/组织、结果状态、错误码、Schema路径、追踪ID、耗时。

```mermaid
erDiagram
AI_CAPABILITY ||--o{ AI_CAPABILITY_VERSION : "版本"
AI_CAPABILITY_CLIENT ||--o{ AI_CAPABILITY_GRANT : "授权"
AI_CAPABILITY ||--o{ AI_CAPABILITY_INVOCATION_LOG : "调用日志"
AI_CAPABILITY {
bigint id PK
bigint tenant_id
varchar capability_code UK
varchar protocol_tool_name UK
varchar capability_name
varchar description
varchar source_type
varchar source_key
varchar source_version
varchar current_version
varchar behavior
varchar risk_level
varchar visibility
varchar publish_status
tinyint enabled
bigint del_flag
}
AI_CAPABILITY_VERSION {
bigint id PK
bigint tenant_id
bigint capability_id FK
varchar version
json input_schema
json output_schema
varchar behavior
varchar risk_level
varchar visibility
json policy_snapshot
varchar status
bigint del_flag
}
AI_CAPABILITY_CLIENT {
bigint id PK
bigint tenant_id
varchar client_code UK
varchar client_name
varchar key_id UK
varchar key_prefix
varchar key_hash
int credential_version
bigint service_user_id
bigint active_org_id
varchar status
datetime expires_at
datetime last_used_at
bigint del_flag
}
AI_CAPABILITY_GRANT {
bigint id PK
bigint tenant_id
bigint client_id FK
bigint capability_id FK
varchar version_strategy
varchar fixed_version
json field_policy
varchar status
datetime expires_at
bigint del_flag
}
AI_CAPABILITY_INVOCATION_LOG {
bigint id PK
bigint tenant_id
varchar request_id UK
bigint client_id
varchar client_code
bigint capability_id
varchar capability_code
varchar capability_version
varchar actor_type
bigint actor_user_id
bigint service_user_id
bigint active_org_id
varchar result_status
varchar result_code
varchar error_code
varchar schema_path
varchar trace_id
bigint duration_ms
bigint del_flag
}
```

图表来源
- [V1.0.21__add_ai_capability_control_plane.sql:3-145](file://forge-server/db/migration/V1.0.21__add_ai_capability_control_plane.sql#L3-L145)

章节来源
- [V1.0.21__add_ai_capability_control_plane.sql:3-145](file://forge-server/db/migration/V1.0.21__add_ai_capability_control_plane.sql#L3-L145)

## 依赖关系分析
- 路由模块依赖模型、供应商、策略与能力目录，结合健康注册表进行健康门控。
- 会话记忆依赖消息服务，读取最近N条消息构建上下文。
- 知识库处理依赖向量存储抽象与具体实现，按实例类型路由到Milvus等。
- 前端视图依赖后端API，映射Agent配置、会话消息与提示词模板字段。

```mermaid
graph LR
Router["PolicyBasedAiModelRouter"] --> Models["AiModel / AiModelCapability"]
Router --> Policies["AiModelRoutePolicy / Target"]
Router --> Health["健康注册表"]
Memory["DbChatMemory"] --> Records["AiChatRecord / ToolCall"]
DocProc["DocumentProcessService"] --> VStore["VectorStoreService"]
VStore --> Factory["VectorStoreFactory"]
Factory --> Milvus["MilvusVectorStoreService"]
```

图表来源
- [PolicyBasedAiModelRouter.java:1-31](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/PolicyBasedAiModelRouter.java#L1-L31)
- [DbChatMemory.java:1-93](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/memory/DbChatMemory.java#L1-L93)
- [DocumentProcessService.java:322-392](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java#L322-L392)
- [VectorStoreFactory.java:1-44](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreFactory.java#L1-L44)

章节来源
- [PolicyBasedAiModelRouter.java:1-31](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/routing/PolicyBasedAiModelRouter.java#L1-L31)
- [DbChatMemory.java:1-93](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/memory/DbChatMemory.java#L1-L93)
- [DocumentProcessService.java:322-392](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java#L322-L392)
- [VectorStoreFactory.java:1-44](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreFactory.java#L1-L44)

## 性能考虑
- 索引优化：
  - 会话消息：按会话与逻辑删除标志复合索引加速过滤未删除消息。
  - 知识库分块：按文档与分块序号唯一约束，索引文档与知识库ID，内容哈希用于去重。
  - 路由日志：按租户时间、模型时间、Agent时间索引便于查询与清理。
- 向量检索：
  - 稠密向量索引（余弦相似度）与稀疏向量BM25索引组合，提升召回与相关性。
  - 集合维度动态确定，避免硬编码导致的重建成本。
- 路由确定性：
  - 固定排序规则（优先级、目标ID）确保可复跑与可解释性。
- 审计与清理：
  - 调用日志按天清理，默认保留90天，降低长期存储压力。

章节来源
- [V1.0.124__enhance_ai_chat_message_structure.sql:84-90](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L84-L90)
- [V1.0.87__add_ai_knowledge_rag.sql:85-112](file://forge-server/db/migration/V1.0.87__add_ai_knowledge_rag.sql#L85-L112)
- [V1.0.18__add_ai_model_routing_governance.sql:81-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L81-L119)
- [MilvusVectorStoreService.java:132-181](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/MilvusVectorStoreService.java#L132-L181)

## 故障排查指南
- 路由失败：
  - 检查策略是否启用、能力是否满足、候选是否处于OPEN熔断状态。
  - 查看调用审计日志中的route_source、route_reason与error_category定位问题。
- 会话异常：
  - 检查消息状态是否为error或aborted，查看error_msg与usage_json确认用量与错误原因。
  - 工具调用明细表可按record_id与session_id查询具体工具执行状态。
- 知识库处理失败：
  - 核对Embedding模型输出维度与知识库配置维度是否一致，不一致会触发强制重建。
  - 检查向量存储实例配置JSON与类型是否正确，工厂路由不支持的类型会抛出业务异常。
- 提示词模板：
  - 确认模板状态与推荐标记，使用统计可用于评估模板质量。

章节来源
- [V1.0.18__add_ai_model_routing_governance.sql:81-119](file://forge-server/db/migration/V1.0.18__add_ai_model_routing_governance.sql#L81-L119)
- [V1.0.124__enhance_ai_chat_message_structure.sql:44-116](file://forge-server/db/migration/V1.0.124__enhance_ai_chat_message_structure.sql#L44-L116)
- [DocumentProcessService.java:322-392](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/service/DocumentProcessService.java#L322-L392)
- [VectorStoreFactory.java:1-44](file://forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/vectorstore/VectorStoreFactory.java#L1-L44)

## 结论
本数据设计围绕AI能力中心的六大领域展开，形成以关系型数据库为核心、向量数据库为扩展的混合存储架构。通过明确的数据模型、严格的索引与审计机制，以及确定性的路由与健康治理，系统在可扩展性、可观测性与安全性方面具备良好基础。后续可根据业务演进持续完善向量存储类型支持与更细粒度的权限控制。

## 附录
- Agent配置字段参考：模型选择模式、路由策略ID、温度、最大Token、状态、知识ID集合、RAG模式、额外配置等。
- 会话消息分页：支持按recordId游标加载更多，保持视口稳定。
- 提示词模板导出：支持Markdown格式导出模板内容与说明。

章节来源
- [agent.vue:1093-1140](file://forge-admin-ui/src/views/ai/agent.vue#L1093-L1140)
- [agent-chat.vue:404-569](file://forge-admin-ui/src/views/ai/agent-chat.vue#L404-L569)