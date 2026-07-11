# Forge AI 中枢：产品战略与技术选型方案

> 本文档沉淀 Forge 系统「从低代码平台转向 AI 中枢」的产品战略与技术选型决策，供后续路线规划、方案评审、对外阐述使用。
>
> 核心论断：**Forge 距离「AI 中枢」比想象中近；应让低代码从「前台产品」退居为「后台能力工厂」，其产出的业务对象/元数据自动转化为 AI Agent 可调用的工具。技术上坚持 Spring/JVM 主干，不切 Python。**
>
> 配套工程落地方案：[`Forge-AI中枢落地架构与阶段路线图.md`](./Forge-AI中枢落地架构与阶段路线图.md)

> **2026-07-10 已验证基线**：Forge 保留 Spring AI `1.1.2` 作为统一接口层，并叠加 Spring AI Alibaba/Extensions `1.1.2.3`。当前只接入 DashScope 核心模型模块，通过显式 `adapter_code` 支持 `openai_compatible` 与 `dashscope_native`；这是一项供应商适配前置能力，不代表 MCP、Nacos、Admin 或 Agent Framework 阶段已经完成。

---

## 第一部分：产品战略方案

### 一、战略定位

**Forge = AI Agent 的企业操作系统（Agent-Ready Enterprise Backend）。**

- 不再把「低代码」当作卖点前台，收益边际递减；
- 转而把系统能力（数据 CRUD、流程审批、消息、权限、外部集成）**标准化为对外可编程的 AI 工具**；
- 目标客户不是「点鼠标搭页面的业务人员」，而是**成熟的 AI Agent / Agent 平台**——让它们通过标准协议安全、可控地驱动企业后端。

一句话：**低代码负责「生产能力」，AI 中枢负责「把能力开放给 Agent 消费」。**

### 二、家底盘点（已有地基）

| 能力 | 现有实现 | 作为 AI 中枢的价值 |
|------|----------|-------------------|
| 动态 CRUD | `DynamicCrudController` (`/ai/crud/{configKey}`)，支持 page/tree/getById/create/update/delete/import/export | 天然的「数据操作工具」，可直接包装成 MCP Tool |
| 业务对象元数据 | `AiBusinessObject`（objectCode/objectName/objectType/modelId/configKey/designStatus）+ `LowcodeModelSchema` | AI 理解系统结构的知识来源，可自动生成工具 Schema |
| 流程能力 | `FlowClient`（startProcess/approve/reject/queryTodo）+ `FlowWebhookNotifier`（事件 Webhook 回调，带重试） | 「审批流工具」+ 事件通知出口 |
| 外部系统代理 | `ExternalProxyService`（鉴权/加密/响应转换） | 统一对接外部系统的能力 |
| API 配置中枢 | `SysApiConfig`（apiName/apiCode/reqMethod/urlPath/authFlag/encryptFlag/tenantFlag/limitFlag/sensitiveFields） | 做开放平台 + Tool 注册表的骨架 |
| 多供应商 LLM | `AiClient`/`AiClientImpl`（Spring AI `ChatClient` + 显式 Provider Adapter，支持 OpenAI Compatible 与 DashScope Native） | 统一 LLM 接入层，供应商扩展基础已就绪 |

**结论：核心地基已具备，缺的是「标准化对外出口 + 安全治理 + 元数据目录服务」。**

### 三、关键缺口

1. **MCP 出口缺失**：无 MCP Server，Agent 无法用标准协议发现和调用能力；
2. **Function Calling / Tool 注册表缺失**：能力未标准化为可被 LLM 调用的工具描述；
3. **独立 API Key / OAuth2 Client Credentials 鉴权缺失**：对外服务需要独立于用户登录态的机器鉴权；
4. **Token 计量与配额缺失**：无法按调用方计量、限流、计费；
5. **统一元数据目录服务（Capability Registry）缺失**：业务对象/API/流程分散，缺少统一「能力目录」供 Agent 检索。

### 四、三阶段落地路线图

#### 阶段一：MCP 出口（1–2 个月）
- 新建 `forge-plugin-mcp` 模块；
- 用 Spring AI 原生 MCP starter，把 `DynamicCrudService`、`FlowClient`、消息服务包装成 MCP Tool；
- **从 `LowcodeModelSchema` 自动生成工具的 JSON Schema**，实现「建模即产出工具」；
- 交付一个可演示的：外部 Agent 通过 MCP 查询/操作 Forge 业务数据。

#### 阶段二：开放平台与安全（2–4 个月）
- 独立 API Key / OAuth2 Client Credentials 鉴权（复用并扩展 `SysApiConfig`）；
- Token 计量、配额、限流、审计日志；
- 多租户维度的 MCP 能力隔离与授权；
- 统一能力目录服务（Capability Registry）供 Agent 检索。

#### 阶段三：Agent 编排与增值（4 个月+）
- 面向企业的 Agent 编排（有状态、多智能体流水线）；
- RAG + 企业知识库接入；
- 可视化 Agent 管理、可观测、评估平台。

### 五、商业化与前景

- **产品分档**：社区版（基础 MCP 出口）/ 企业版（安全治理 + 计量 + 多租户）/ 平台版（Agent 编排 + 可观测）；
- **目标客户**：需要让 AI Agent 安全接入企业内部系统的中大型企业、Agent 平台厂商、系统集成商；
- **生态卡位**：抢占「Agent 与企业后端之间的标准适配层」，成为 Agent 生态的企业侧入口；
- **安全风险提示**：对外开放能力必须做好鉴权、租户隔离、敏感字段脱敏、操作审计、限流熔断，涉及资金/状态流转/权限变更的能力需人工审查开关。

---

## 第二部分：技术选型方案

### 一、核心前提：能力都在 JVM 里

- `AiClient` 已基于 **Spring AI**（`ChatClient`、`OpenAiChatOptions`）封装 7 家供应商 + 熔断 + 会话记忆；
- MCP 要暴露的业务能力（CRUD、流程、消息、权限、外部代理）**全部是 Java Service**。

**铁律：「工具（Tool）要离业务能力最近」。**
MCP Server 放在 JVM 内 = 进程内直接方法调用；放到 Python = Python 需 HTTP 回调 Java，凭空多一跳网络 + 双栈运维 + 丢失鉴权/租户/事务上下文。这是选型的核心锚点。

### 二、三条路线的生态状态（2026 年中）

#### 路线 A：Spring AI 原生（当前技术栈）
- MCP 已原生支持：`spring-ai-starter-mcp-server-webmvc/webflux` + client starter；
- 成熟度：1.0 GA 已发布，1.1-M1 强化 agent + MCP，2.0-M6（2026.5）MCP tool-calling agent 已成熟；
- 与现有 `AiClient` 完全同源，零迁移、零学习曲线；
- 短板：重度多智能体编排、Agent 可视化平台较弱（近期用不到）。

#### 路线 B：Spring AI Alibaba（Spring AI 的增强层）
- 当前已验证 Spring AI `1.1.2` 与 Spring AI Alibaba/Extensions `1.1.2.3` 组合，Alibaba 复用 Spring AI 的 `ChatModel`、`ChatClient` 和 Tool Calling 抽象，不是对 Spring AI 的替换；
- 杀手锏：集成 **Nacos MCP Registry**（分布式注册 + 负载均衡）；**MCP Gateway**（把 Nacos 普通服务自动转 MCP）；**Admin 平台**（可视化 Agent 开发、可观测、评估、MCP 管理）；对通义千问原生友好；
- Forge 已先完成 DashScope 核心模型的供应商适配层；Nacos、Admin、MCP Registry 和 Agent Framework 仍需按后续阶段闸门单独评估；
- 代价：后续治理能力会引入 Nacos 等依赖和运维复杂度。

#### 路线 C：AgentScope（阿里通义实验室）
- Python 版：百炼底座，2.0 面向生产，多智能体/Sandbox/Runtime 最完整——但是 Python 栈；
- **AgentScope Java 版**：JVM 原生（JDK 17+、Project Reactor），提供 ReAct + Harness 工程化（工作区/长期记忆/上下文压缩/沙箱）+ 多智能体编排 + MCP/A2A + Nacos 注册 + OpenTelemetry，**可无侵入嵌入 Spring Boot**；
- 定位：重度 Agent 运行时框架，强在有状态、可恢复、多智能体、沙箱隔离——比「暴露工具」重得多。

#### 路线 D：纯 Python 生态（LangGraph 1.0 等）
- 生态最大、最新工具最快（LangGraph/CrewAI/Microsoft Agent Framework 均已 1.0）；
- 但对本项目是异构栈：独立服务、跨语言通信、双份运维、与现有 Sa-Token/多租户/事务体系割裂；
- 作为「对外企业后端服务」，稳定性和体系融合 > 生态新潮。

### 三、决策对比表

| 维度 | Spring AI 原生 | Spring AI Alibaba | AgentScope Java | Python(LangGraph) |
|------|:---:|:---:|:---:|:---:|
| 复用现有 Spring/AiClient | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐ |
| MCP Server 出口成熟度 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 进程内直调业务 Service | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐（需跨网络回调） |
| 分布式 MCP 治理/Admin | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 重度多智能体/有状态编排 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 团队上手 & 运维成本 | 最低 | 低 | 中 | 高（双栈） |

### 四、明确推荐：Java 主干，分层演进，不切 Python

**以 Spring 体系为主干，坚决不把主链路迁到 Python。** 分三层演进，每一步复用前一步投资：

- **① 近期（阶段一 MCP 出口）→ Spring AI 原生 MCP starter**
  在 `forge-plugin-mcp` 里用 `spring-ai-starter-mcp-server`，进程内直调 `DynamicCrudService`、`FlowClient`、消息服务；与现有 `AiClient` 同源，最快出可演示 Demo。

- **② 中期（多租户 MCP 治理、能力目录、分布式）→ 在现有 Spring AI Alibaba 基线上按需启用 Nacos 治理**
  当前已完成的 DashScope Provider Adapter 不自动带入 Nacos；只有出现「分布式部署 + 按租户/Key 治理 + 可视化管理」的真实需求并通过阶段闸门后，才引入 Nacos MCP Registry 和 Admin，无需推翻 Spring AI 代码。

- **③ 未来（重度有状态 Agent / 多智能体流水线）→ JVM 内引入 AgentScope Java**
  无侵入嵌入 Spring Boot，用 Harness（记忆/沙箱/工作区）和 A2A 多智能体，依然不切 Python。

- **Python 仅作旁路微服务**：某些能力只有 Python 生态有（特定 RAG/ML 库、算法团队独立迭代）时，让它作为独立 Agent，**通过 MCP / A2A 接入主系统**——是「接入」而非「主导」，主链路仍在 Java。

### 五、一句话总结

> **别切 Python。** 业务能力、权限、租户、事务全在 JVM，MCP 出口就该贴着它们建。**以 Spring AI 为统一接口，先用已落地的 Spring AI Alibaba Provider Adapter 扩展模型接入 → 近期建设 Spring AI MCP 出口 → 需要治理时再启用 Nacos/Admin → 未来要重度 Agent 时在 JVM 内引入 AgentScope Java。** 这些能力都在 Spring/JVM 体系内分层演进，Python 仅作特定场景旁路，通过 MCP/A2A 接入。

---

## 附录：关键代码资产索引

| 资产 | 路径/标识 |
|------|-----------|
| 统一 LLM 封装 | `forge-plugin-ai` → `AiClient` / `AiClientImpl` |
| 动态 CRUD | `forge-plugin-generator` → `DynamicCrudController` (`/ai/crud/{configKey}`) |
| 业务对象元数据 | `forge-plugin-generator` → `AiBusinessObject` + `LowcodeModelSchema` |
| 流程客户端 | `forge-flow-client` → `FlowClient` |
| 流程事件回调 | `forge-plugin-flow` → `FlowWebhookNotifier` |
| 外部系统代理 | `forge-plugin-external` → `ExternalProxyService` |
| API 配置中枢 | `forge-starter-api-config` → `SysApiConfig` |
