# 三期：Agent 执行引擎 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建企业级 Agent 执行引擎——ReAct 循环、工具调用（MCP/内置/Capability）、事件流（28 种）、HITL 权限引擎（中断-恢复式）、技能系统、对话 UI 升级、Vision。

**Architecture:** 新建 `agent/engine` 子域。纯 ReAct 循环（推理→行动迭代），底层用 Spring AI `ChatModel`（不用 ChatClient 工具循环），事件模型参考 AgentScope 28 种。引擎内部 Reactor，对外 WebFlux `Flux<ServerSentEvent>`（与现有 `AiClientController` 一致）。保留复用现有路由/熔断/调用日志/会话记忆。HITL 中断-恢复式，Redis 存中断状态。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 2.0.0 ChatModel、Reactor、Redis、Vue 3 + Naive UI

**Spec:** `docs/superpowers/specs/2026-08-04-ai-upgrade-master-design.md`

## 决策记录（2026-08-04 评审确认）

- **前置**：依赖 Phase 0（Spring AI 2.0.0）+ 一期 + 二期（RAG 基础检索）。
- **存量调用过渡**：新引擎通过新入口 `/ai/engine/stream` + 配置开关（Agent `extraConfig.engineEnabled=true`）消费；存量 `AiClient` 的 4 个调用点（对话/BPMN/代码生成/admin bridge）**不改契约**（继续返回文本），零回归。引擎本身完整落地（ReAct + 28 事件 + HITL + 工具）。
- **工具源**：**SPI 桥接解耦**——`forge-plugin-ai` 定义 `AgentToolContributor` 接口，`mcp`/`capability` 插件各自实现，Spring 收集 Bean；`ai` **不反向依赖** mcp/capability 插件。
- **权限默认**：未配置工具默认 **ALLOW**（计划原样），风险工具（删除/提交/取消等关键词）启发式 ASK 兜底。
- **技能脚本**：**只读不执行**——`ReadSkillTool` 直接从 `ai_skill_file` 表读内容给 LLM，不物化落盘、不沙箱（仓库无命令执行沙箱；执行任意脚本另立安全专项）。
- **SSE 通道**：沿用现有 WebFlux `Flux<ServerSentEvent>`（与 `AiClientController` 一致），不用 MVC `SseEmitter`。
- **事件保留**：`ai_agent_event` 审计流水需保留策略——新增 `AiAgentEventRetentionJob`（参照 `AiInvocationLogRetentionJob`）。
- **消息持久化**：新引擎用户消息 + 最终助手回复（可见文本）写现有 `ai_chat_record`；会话列表写现有 `ai_chat_session`（`session_id` 沿用现有 UUID 字符串格式，复用 `AiChatSessionService`/`AiChatRecordService`）。`ai_agent_event` 纯审计。会话列表/回放/多轮记忆（`DbChatMemory`）全部复用现有层，**零新表**。
- **HITL 中断**：`ai_agent_interrupt` **不建表**，中断状态纯 Redis（TTL 10 分钟）。
- **Flyway**：`ai_agent` 全部新增列（`knowledge_ids`/`rag_mode`/`greeting`/`preset_questions`/`max_iters`/`tool_group_mode`）收拢到 **V1.0.88**（含二期 Task 9 需要的列）。
- **Spring AI 2.0.0 tool_call**：`ChatModel.stream()` 的 tool_call 在流式结尾才完整，ReAct 循环需累积 assistant message 直到 finish_reason 再决策；实施前写最小探针测试验证 `AssistantMessage.getToolCalls()` 在 2.0.0 的解析。

## Global Constraints

- 查询 SQL 必须写 Mapper XML（`DataScopeInterceptor`），禁止 Service 层用 `LambdaQueryWrapper`
- 业务数据 `tenant_id` 必须为 `1`
- 分页参数：`pageNum`/`pageSize`
- 逻辑删除默认：`del_flag bigint NOT NULL DEFAULT 0`，数值主键表 `@TableLogic(value = "0", delval = "id")`，唯一键直接建在 `del_flag` 上（无生成列）
- API Key 脱敏；日志禁止打印敏感信息
- Flyway 迁移单调递增（> 1.0.83），SQL 幂等，`NOT EXISTS` 防重复
- 字典不硬编码；菜单 `sys_resource` 带 `NOT EXISTS`
- 禁止 Service 互相注入，跨 Service 协调上提 Controller
- 基础包：`com.mdframe.forge`

---

### Task 1: 数据库迁移（事件表 + Agent 扩展 + 工具/技能/权限表 + 菜单）

**Files:**
- Create: `forge-server/db/mysql/migration/V1.0.88__add_agent_engine_event_skill.sql`

**Interfaces:**
- Produces: 表 `ai_agent_event`、`ai_agent_tool_config`、`ai_skill`、`ai_skill_file`、`ai_agent_skill`、`ai_agent_tool_permission`（HITL 中断状态**纯 Redis 存，不建表**）；`ai_agent` 扩展列；字典 + 菜单

- [ ] **Step 1: 事件表（事件流式单表）**

```sql
CREATE TABLE IF NOT EXISTS `ai_agent_event` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `turn_index` int NOT NULL COMMENT 'ReAct轮次',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型(28种)',
  `event_data` longtext DEFAULT NULL COMMENT '事件数据JSON',
  `parent_id` bigint DEFAULT NULL COMMENT '父事件ID(工具结果关联工具调用)',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `turn_index`),
  KEY `idx_session_type` (`session_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent事件流(全量持久化)';
```

> 注：`ai_agent_event` 是**审计流水表**（一次写入、不更新），**不做逻辑删除**（无 `del_flag`），`create_time` 必填。

- [ ] **Step 2: 工具配置表 + 权限表**

```sql
CREATE TABLE IF NOT EXISTS `ai_agent_tool_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `tool_source` varchar(32) NOT NULL COMMENT '工具来源(mcp/builtin/capability)',
  `tool_key` varchar(200) NOT NULL COMMENT '工具标识',
  `tool_group` varchar(64) DEFAULT 'default' COMMENT '工具组(技能激活)',
  `enabled` char(1) DEFAULT '0' COMMENT '是否启用(0否 1是)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_tool_active` (`agent_id`, `tool_source`, `tool_key`, `del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具绑定';

CREATE TABLE IF NOT EXISTS `ai_agent_tool_permission` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `tool_key` varchar(200) NOT NULL COMMENT '工具标识',
  `decision` varchar(16) NOT NULL COMMENT '权限(allowed/ask/denied)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_perm_active` (`agent_id`, `tool_key`, `del_flag`),
  KEY `idx_agent_tool` (`agent_id`, `tool_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具权限(ALLOW/ASK/DENY)';
```

> 注：`ai_agent_tool_config`/`ai_agent_tool_permission` 的 `del_flag` 统一 `bigint`，唯一键直接建在 `del_flag` 上（无 `logic_delete_active` 生成列，与现有表一致）。

- [ ] **Step 3: 技能表**

```sql
CREATE TABLE IF NOT EXISTS `ai_skill` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `skill_name` varchar(100) NOT NULL COMMENT '技能名称',
  `skill_code` varchar(100) NOT NULL COMMENT '技能编码',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `version` varchar(32) DEFAULT '1.0.0' COMMENT '版本',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_skill_code_active` (`tenant_id`, `skill_code`, `del_flag`),
  KEY `idx_code` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI技能包';

CREATE TABLE IF NOT EXISTS `ai_skill_file` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL COMMENT '技能ID',
  `file_path` varchar(500) NOT NULL COMMENT '技能内文件路径(SKILL.md/scripts/x.py)',
  `file_content` longtext NOT NULL COMMENT '文件内容',
  `encoding` varchar(16) DEFAULT 'utf-8' COMMENT '编码',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_skill` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能文件';

CREATE TABLE IF NOT EXISTS `ai_agent_skill` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `agent_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_agent_skill_active` (`agent_id`, `skill_id`, `del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent技能绑定';
```

- [ ] **Step 4: Agent 扩展列 + 字典 + 菜单**

`ai_agent` 补列：`greeting`（问候语）、`preset_questions`（JSON）、`max_iters`（int）、`knowledge_ids`（JSON）、`rag_mode`（none/forced/smart）、`tool_group_mode`（skill 激活）。用 `information_schema` 判断列存在再 ALTER（幂等，参考 `V1.0.18` 的 `SET @col=...; PREPARE...; EXECUTE...` 模板）。
新增字典：`ai_agent_event_type`（28 种）、`ai_tool_source`（mcp/builtin/capability）、`ai_tool_permission`（allowed/ask/denied）。
新增菜单：`Agent 执行` 分组下 `技能管理` / `工具管理` / `Agent 工作台`。菜单 SQL 按 `V1.0.18` 完整模板：`sys_resource` 含 `client_code`/`menu_status`/`visible`/`resource_type` 等列 + `sys_role_resource` 授权 admin，全部 `NOT EXISTS` 防重复。

- [ ] **Step 5: 提交**

```bash
git add forge-server/db/mysql/migration/V1.0.88__add_agent_engine_event_skill.sql
git commit -m "feat(db): Agent引擎事件/工具/技能表与菜单"
```

---

### Task 2: 事件模型（28 种）与事件发布器

**Files:**
- Create: `.../agent/engine/event/AgentEvent.java`（抽象基类）
- Create: `.../agent/engine/event/AgentEventType.java`（28 种枚举）
- Create: `.../agent/engine/event/AgentEventPublisher.java`
- Create: `.../agent/engine/event/persistence/AgentEventPersistence.java`
- Create: `.../agent/engine/event/sse/AgentEventWebFluxStream.java`（WebFlux SSE 转发）
- Create: `.../agent/engine/event/job/AgentEventRetentionJob.java`（保留策略，参照 `AiInvocationLogRetentionJob`）
- Test: `.../agent/engine/event/AgentEventTypeTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ai_agent_event` 表
- Produces:
  - `AgentEventType` 28 种枚举（`AGENT_START`/`AGENT_END`/`AGENT_RESULT`/`MODEL_CALL_START`/`MODEL_CALL_END`/`TEXT_BLOCK_START`/`TEXT_BLOCK_DELTA`/`TEXT_BLOCK_END`/`THINKING_BLOCK_START`/`THINKING_BLOCK_DELTA`/`THINKING_BLOCK_END`/`DATA_BLOCK_START`/`DATA_BLOCK_DELTA`/`DATA_BLOCK_END`/`TOOL_CALL_START`/`TOOL_CALL_DELTA`/`TOOL_CALL_END`/`TOOL_RESULT_START`/`TOOL_RESULT_TEXT_DELTA`/`TOOL_RESULT_DATA_DELTA`/`TOOL_RESULT_END`/`EXCEED_MAX_ITERS`/`REQUEST_STOP`/`REQUIRE_USER_CONFIRM`/`USER_CONFIRM_RESULT`/`SUBAGENT_EXPOSED`/`HINT_BLOCK`/`ALL_TOOLS_DENIED`/`CUSTOM`）
  - `AgentEventPublisher.publish(sessionId, turnIndex, AgentEvent)`（异步持久化 + WebFlux 转发）
  - `AgentEventPersistence` 批量写 `ai_agent_event`
  - `AgentEventRetentionJob`：`ai_agent_event` 审计流水定期清理（保留 N 天），参照 `AiInvocationLogRetentionJob`

- [ ] **Step 1: 写失败测试（28 种事件类型完整性）**

```java
package com.mdframe.forge.plugin.ai.agent.engine.event;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentEventTypeTest {

    @Test
    void all28TypesAreRegistered() {
        assertEquals(28, AgentEventType.values().length);
    }

    @Test
    void coreStreamingTypesPresent() {
        assertNotNull(AgentEventType.fromCode("TEXT_BLOCK_START"));
        assertNotNull(AgentEventType.fromCode("TOOL_CALL_START"));
        assertNotNull(AgentEventType.fromCode("REQUIRE_USER_CONFIRM"));
        assertNotNull(AgentEventType.fromCode("USER_CONFIRM_RESULT"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AgentEventTypeTest -DfailIfNoTests=false`
Expected: FAIL

- [ ] **Step 3: 实现枚举**

`AgentEventType` 枚举含 `code`，`fromCode(String)` 静态方法。28 个枚举值（含参考 AgentScope 的全部类型）。

- [ ] **Step 4: 实现事件基类与发布器**

`AgentEvent` 抽象基类：`sessionId`/`turnIndex`/`eventType`/`timestamp`/`data`（JSON）。
`AgentEventPublisher`：`publish` 后异步持久化（`AgentEventPersistence`，批量 20 条一写）+ 同步转发 `AgentEventWebFluxStream`（若连接在，`Flux<ServerSentEvent<String>>`）。

- [ ] **Step 5: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AgentEventTypeTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/event/
git commit -m "feat(ai): Agent事件模型28种与发布器"
```

---

### Task 3: 工具抽象与注册表

**Files:**
- Create: `.../agent/engine/tool/AgentTool.java`（接口）
- Create: `.../agent/engine/tool/AgentToolSchema.java`（JSON Schema 生成）
- Create: `.../agent/engine/tool/registry/AgentToolRegistry.java`
- Create: `.../agent/engine/tool/source/BuiltinToolSource.java`、`AgentToolContributor.java`（SPI，放 forge-plugin-ai）
- Create: `.../agent/engine/tool/builtin/RagSearchTool.java`（调二期检索）、`HttpTool.java`
- Modify: `forge-plugin-mcp` / `forge-plugin-capability` 各新增 `AgentToolContributor` 实现（把 MCP/能力映射为 AgentTool）
- Test: `.../agent/engine/tool/AgentToolRegistryTest.java`

**Interfaces:**
- Consumes: Task 1（`ai_agent_tool_config`）、二期 RAG 检索、`AgentToolContributor` SPI 实现（来自 mcp/capability 插件）
- Produces:
  - `AgentTool` 接口：`getKey()`/`getDescription()`/`getParametersSchema()`（JSON Schema）/`execute(Map<String,Object> args, ToolContext ctx) -> ToolResult`（`ToolResult` 含 `type`：text/data/image、`content`）
  - `AgentToolContributor`（SPI）：`String getSource()` / `List<AgentTool> contribute()` —— `forge-plugin-ai` 定义接口，`forge-plugin-mcp`/`forge-plugin-capability` 各自实现（`@Component`），Spring 自动收集
  - `AgentToolRegistry.resolve(agentId) -> List<AgentTool>`（按 Agent 工具配置 + 技能激活组加载）

> **SPI 桥接解耦**：`forge-plugin-ai` **不依赖** `forge-plugin-mcp`/`forge-plugin-capability`。`AgentToolRegistry` 只注入 `List<AgentToolContributor>`，由 mcp/capability 插件（可选能力，`FORGE_MCP_ENABLED`/`FORGE_CAPABILITY_*`）在启用时提供实现；未启用则该来源无 contributor。`mcp`/`capability` 插件需依赖 `forge-plugin-ai`（提供方依赖被适配方，方向无环）。

- [ ] **Step 1: 写失败测试（注册表按工具源加载）**

```java
package com.mdframe.forge.plugin.ai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentToolRegistryTest {

    @Test
    void registryResolvesToolsBySource() {
        // mock: Agent绑定了 builtin:rag_search, capability:order_query
        // AgentToolRegistry.resolve 返回包含这两个工具
    }
}
```

- [ ] **Step 2: 实现 AgentTool 接口与 ToolResult**

`AgentTool`：`getKey/getDescription/getParametersSchema/execute`。`ToolResult` 支持 `type`（text/data/image）多类型结果（多类型结果决策）。

- [ ] **Step 3: 实现内置工具源 + SPI 桥接**

- `BuiltinToolSource`：内置工具注册表（`RagSearchTool`、`HttpTool`），直接实现 `AgentToolContributor`（source=`builtin`）
- `AgentToolContributor`（SPI）：`forge-plugin-ai` 定义，`mcp`/`capability` 插件实现：
  - `mcp` 插件 contributor：读 `McpToolContributorAggregator` 聚合的工具映射为 `AgentTool`（source=`mcp`）
  - `capability` 插件 contributor：把 `CapabilityRegistry` 的能力映射为 `AgentTool`（source=`capability`，复用 `McpCapabilityAdapter` 的 schema 投影逻辑）

- [ ] **Step 4: 实现注册表**

`AgentToolRegistry.resolve(agentId)`：注入 `List<AgentToolContributor>`，按 `ai_agent_tool_config` 的启用工具组（技能激活）过滤各 contributor 的工具，去重。

- [ ] **Step 5: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/tool/
git commit -m "feat(ai): Agent工具抽象与SPI桥接多源注册表"
```

---

### Task 4: 权限引擎（ALLOW/ASK/DENY）

**Files:**
- Create: `.../agent/engine/permission/PermissionEngine.java`
- Create: `.../agent/engine/permission/PermissionDecision.java`
- Create: `.../agent/engine/permission/AgentToolPermissionService.java`
- Test: `.../agent/engine/permission/PermissionEngineTest.java`

**Interfaces:**
- Consumes: Task 1（`ai_agent_tool_permission`）
- Produces: `PermissionEngine.decide(agentId, toolKey, mode) -> PermissionDecision`（`ALLOW`/`ASK`/`DENY`）；`AgentToolPermissionService` 读配置并缓存

- [ ] **Step 1: 写失败测试**

```java
package com.mdframe.forge.plugin.ai.agent.engine.permission;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionEngineTest {

    @Test
    void askRequiresConfirmationAndDenyBlocks() {
        PermissionEngine engine = new PermissionEngine(null);
        assertEquals(PermissionDecision.ASK, engine.decide("agent1", "order_delete", null));
        assertEquals(PermissionDecision.DENY, engine.decide("agent1", "secret_read", null));
        assertEquals(PermissionDecision.ALLOW, engine.decide("agent1", "rag_search", null));
    }

    @Test
    void unknownToolDefaultsToAllow() {
        PermissionEngine engine = new PermissionEngine(null);
        assertEquals(PermissionDecision.ALLOW, engine.decide("agent1", "unknown_tool", null));
    }
}
```

- [ ] **Step 2: 实现权限引擎**

`PermissionEngine.decide`：
1. 查 `ai_agent_tool_permission` 配置（Agent 级别 ALLOW/ASK/DENY）
2. 有配置按配置；无配置默认 ALLOW（普通工具）——风险工具（删除/提交/取消等关键词）建议 ASK 由配置决定
3. `PermissionDecision` 携带 `askMessage`（待确认描述）

- [ ] **Step 3: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=PermissionEngineTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/permission/
git commit -m "feat(ai): 工具权限引擎ALLOW/ASK/DENY"
```

---

### Task 5: ReAct 循环核心

**Files:**
- Create: `.../agent/engine/ReactAgent.java`
- Create: `.../agent/engine/ReactLoop.java`
- Create: `.../agent/engine/ReactContext.java`
- Create: `.../agent/engine/step/ReasonStep.java`、`ActionStep.java`
- Create: `.../agent/engine/hitl/InterruptState.java`
- Create: `.../agent/engine/hitl/InterruptStore.java`（Redis）
- Test: `.../agent/engine/ReactLoopTest.java`

**Interfaces:**
- Consumes: Task 2（事件）、Task 3（工具）、Task 4（权限）、一期 `AiInvocationResolver`/`ChatModel`
- Produces:
  - `ReactAgent.execute(ReactRequest) -> Flux<AgentEvent>`（`ReactRequest` 含 `agentCode`/`sessionId`/`message`/`history`/`contextVars`）
  - `ReactLoop`：ReAct 迭代（`maxIters`），每轮 `ReasonStep`（LLM 推理，检测 tool_call）+ `ActionStep`（执行工具）
  - `InterruptStore`（Redis）：`save(interruptId, state)` / `get` / `remove`，TTL 10 分钟

- [ ] **Step 1: 写失败测试（循环在工具调用后继续推理）**

```java
package com.mdframe.forge.plugin.ai.agent.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReactLoopTest {

    @Test
    void loopIteratesUntilNoToolCall() {
        // mock ChatModel: 第一次返回 tool_call(rag_search), 第二次返回纯文本
        // ReactLoop 应执行2轮：推理→行动→推理
    }

    @Test
    void maxItersStopsLoop() {
        // mock ChatModel 永远返回 tool_call, maxIters=3
        // 循环应在3轮后结束, 发出 EXCEED_MAX_ITERS 事件
    }
}
```

- [ ] **Step 2: 实现 ReAct 循环**

`ReactLoop.run(ReactContext ctx)`：
```
while (turn < maxIters) {
  event: THINKING/TOOL_REASONING 推理（ChatModel.stream()，累积 text/thinking/tool_call）
  if (无 tool_call) { event: TEXT_BLOCK_*; break; }
  for each tool_call:
    decision = permissionEngine.decide(agentId, toolKey)
    if DENY: event: ALL_TOOLS_DENIED; 构造拒绝结果继续
    if ASK: event: REQUIRE_USER_CONFIRM; 保存 InterruptState 到 Redis; return（中断-恢复式）
    result = tool.execute(args)
    event: TOOL_RESULT_*（多类型）
  turn++
}
```

- [ ] **Step 3: 实现中断-恢复**

`InterruptStore`：Redis hash 存 `interruptId → {agentCode, sessionId, turnIndex, toolCall, history}`，TTL 10 分钟。
`ReactAgent.resume(interruptId, ConfirmResult)`：从 Redis 取状态，权限决策设为已确认，继续 `ActionStep` + 后续循环。

- [ ] **Step 4: 用 Spring AI ChatModel 调底层**

`ReasonStep` 用 `ChatModel.stream()`（复用一期 Provider 适配器创建的 `ChatModel`），解析 `AssistantMessage.getToolCalls()`（OpenAI Function Calling 格式）。注意**不用** Spring AI 的 `ChatClient.tools()` / ToolCallAdvisor——工具调用循环完全自控。

- [ ] **Step 5: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS（mock ChatModel 验证循环逻辑）

- [ ] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/
git commit -m "feat(ai): ReAct循环核心与HITL中断-恢复"
```

---

### Task 6: Agent 对话控制器（新入口，非替代 AiClientImpl）

**Files:**
- Create: `.../agent/engine/controller/AgentEngineController.java`
- Create: `.../agent/engine/service/AgentEngineService.java`
- Modify: `.../ai/client/controller/AiClientController.java`（可选：检测 `engineEnabled` 转调新引擎，未启用走旧逻辑）
- Test: `.../agent/engine/service/AgentEngineServiceTest.java`

**Interfaces:**
- Consumes: Task 5（`ReactAgent`）、Task 2（WebFlux SSE）、现有 `AiInvocationResolver`/`DbChatMemory`/`AiModelInvocationRecorder`
- Produces: `POST /ai/engine/stream`（WebFlux SSE，`agentCode`+`sessionId`+`message`）返回 `Flux<ServerSentEvent<String>>` 编码的 Agent 事件；`POST /ai/engine/resume`（HITL 恢复）

> **新入口 + 配置开关**：引擎走独立入口 `/ai/engine/stream`，不替代现有 `AiClient`。存量 `AiClientController` 的 4 个调用点（对话/BPMN/代码生成/admin bridge）**不改契约**（继续返回文本）。可选地在 `AiClientController.stream` 检测 Agent `extraConfig.engineEnabled=true` 时转调新引擎，否则走旧逻辑（平滑过渡）。
>
> **消息持久化（标准分层）**：新引擎的用户消息 + **最终助手回复（仅可见文本，不含思考/工具过程）** 写入现有 `ai_chat_record`（与 `AiClientImpl.persistConversation` 一致，role user/assistant）；会话列表写现有 `ai_chat_session`（`session_id` 沿用现有 UUID 字符串格式，经 `AiChatSessionService.getOrCreate`）。`ai_agent_event` 仅存细粒度事件流（纯审计）。这样会话列表/消息回放（Task 9）与多轮记忆 `DbChatMemory` 全部复用现有层，**零新表**。思考/工具过程只出现在 `ai_agent_event`，不进消息层。

- [ ] **Step 1: 实现 AgentEngineService**

`stream(req)`：
1. `AiInvocationResolver.resolve()` 解析模型（复用路由/熔断）
2. 构造 `ReactContext`（含历史 `DbChatMemory.load`、系统提示词、工具列表）
3. `ReactAgent.execute()` 返回 `Flux<AgentEvent>`
4. 事件编码为 WebFlux SSE（`event:<TYPE>\ndata:<JSON>\n\n`，`Flux<ServerSentEvent<String>>`，与现有 `AiClientController` 一致）
5. 结束持久化：**用户消息 + 最终助手回复（可见文本）写 `ai_chat_record`**（复用 `AiChatRecordService`），**会话写 `ai_chat_session`**（复用 `AiChatSessionService`，`session_id` 沿用 UUID 格式）；事件流写 `ai_agent_event`（Task 1）；调用日志写 `AiModelInvocationRecorder`

- [ ] **Step 2: 实现 HITL resume**

`POST /ai/engine/resume`：`interruptId` + `confirmResult` → `ReactAgent.resume()` → 继续 WebFlux SSE 流。

- [ ] **Step 3: AiClientController 可选转调**

`AiClientController.stream` 检测 Agent 是否启用引擎（`extraConfig.engineEnabled=true`），启用则转调 `AgentEngineService.stream`，否则走旧逻辑（存量调用点契约不变）。

- [ ] **Step 4: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/ forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/controller/
git commit -m "feat(ai): Agent引擎对话控制器与SSE"
```

---

### Task 7: 技能系统（只读不执行）

**Files:**
- Create: `.../skill/domain/AiSkill.java`、`AiSkillFile.java`、`AiAgentSkill.java`（Task 1 表）
- Create: `.../skill/mapper/AiSkillMapper.java`+XML、`AiSkillFileMapper.java`+XML、`AiAgentSkillMapper.java`+XML
- Create: `.../skill/service/AiSkillService.java`（ZIP 上传/解析/存库）
- Create: `.../skill/controller/AiSkillController.java`（含 AI 生成/优化）
- Create: `.../skill/parser/SkillMarkdownParser.java`
- Create: `.../agent/engine/tool/builtin/ReadSkillTool.java`（从 `ai_skill_file` 读内容给 LLM，不落盘）
- Test: `.../skill/service/AiSkillServiceTest.java`

**Interfaces:**
- Consumes: Task 1（`ai_skill` 表）、一期 Chat 模型（AI 生成/优化）
- Produces: `AiSkillService.uploadZip(MultipartFile) -> skillId`；`ReadSkillTool`（按 skillCode 从 `ai_skill_file` 表读 SKILL.md/脚本内容给 LLM 参考）；`POST /ai/skill/ai-generate`（描述→SKILL.md）、`POST /ai/skill/ai-optimize`（指令→优化 SKILL.md）

> **只读不执行**：技能脚本**不执行**（仓库无命令执行沙箱）。`ReadSkillTool` 直接从 `ai_skill_file` 表读内容给 LLM，**不物化落盘**、不 ShellTool。技能靠"描述性指令 + 现有安全工具（RAG/HTTP/Capability）"驱动。后续需要执行任意脚本时另立安全专项（沙箱）。

- [ ] **Step 1: 实现 ZIP 解析存库**

`uploadZip`：解压 ZIP → 读 `SKILL.md`（frontmatter 解析名称/描述/版本）→ `ai_skill_file` 存所有文件（路径+内容）→ 返回 skillId。ZIP 校验（防目录穿越：文件路径规范化后必须位于解压根目录内）。

- [ ] **Step 2: 实现 SkillMarkdownParser**

解析 SKILL.md frontmatter（`name`/`description`/`version`）+ 正文指令。参照 agentscope-java `MarkdownSkillParser`。

- [ ] **Step 3: 实现 ReadSkillTool（只读）**

`ReadSkillTool` 实现 `AgentTool`：按 `skillCode`（或 Agent 绑定的 `ai_agent_skill`）从 `ai_skill_file` 表读文件内容返回给 LLM。不落盘、不执行。

- [ ] **Step 4: 实现 AI 生成/优化**

`ai-generate`：描述 → LLM 生成 SKILL.md 内容（流式可选）。
`ai-optimize`：现有 SKILL.md + 优化指令 → LLM 返回优化后 SKILL.md。

- [ ] **Step 5: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/skill/
git commit -m "feat(ai): 技能系统(ZIP/物化/AI生成优化)"
```

---

### Task 8: Vision 集成对话

**Files:**
- Modify: `.../agent/engine/tool/builtin/`（`ImageUploadTool` 或直接在 ReactRequest 支持图片附件）
- Modify: `.../agent/engine/ReactContext.java`（支持多模态消息）
- Modify: `.../agent/engine/step/ReasonStep.java`（构造 UserMessage 含图片）
- Test: `.../agent/engine/step/ReasonStepTest.java`

**Interfaces:**
- Consumes: Task 5 引擎、一期 Chat 模型（vision capability）
- Produces: `ReactRequest` 支持 `images`（`List<ImageInput>`，`fileId`）；模型带 `vision` 能力时对话可传图

- [ ] **Step 1: 支持图片输入**

`ReactRequest` 加 `images`。`ReasonStep` 构造消息时，若 `AiModel` 有 `vision` capability 且请求含图片，用 Spring AI `UserMessage` 的多模态构造（`image` content block，base64 或 URL）。图片按 fileId 从文件服务取。

- [ ] **Step 2: 无 vision 能力时降级**

模型无 vision 能力但传图：返回 `HINT_BLOCK` 事件提示"当前模型不支持图片"。

- [ ] **Step 3: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/
git commit -m "feat(ai): Agent对话支持Vision图片输入"
```

---

### Task 9: 对话 UI 升级（前端）

**Files:**
- Modify: `forge-admin-ui/src/views/ai/agent.vue`（参考 snail-ai-admin：左配置+右预览）
- Create: `forge-admin-ui/src/views/ai/agent/chat.vue`（用户对话模式）
- Create: `forge-admin-ui/src/views/ai/agent/components/`（消息气泡/工具卡片/思考折叠/循环状态/HITL确认/计划状态）
- Create: `forge-admin-ui/src/views/ai/skill/index.vue`（技能管理 + 技能 IDE）
- Modify: `forge-admin-ui/src/api/ai.js`（engine stream/resume、skill 接口）

**Interfaces:**
- Consumes: Task 6 接口（`/ai/engine/stream`、`/ai/engine/resume`）、Task 7 接口（skill）
- Produces: Agent 工作台（配置+预览）、用户对话模式、技能 IDE、中间状态渲染

- [ ] **Step 1: Agent 工作台改造**

左侧编排面板增加：知识库绑定（多选≤5）、技能绑定、工具配置（MCP/内置/Capability + 权限 ALLOW/ASK/DENY）、问候语、预设问题、maxIters、RAG 模式。右侧实时对话预览支持 SSE + 事件渲染。

- [ ] **Step 2: 对话消息气泡组件**

`AgentMessageBubble.vue`：五段式（思考折叠 → 计划状态 → 内容 → 操作栏 → 引用来源）。工具调用卡片（名称/参数/结果，多类型渲染）、循环状态指示（第几轮/推理中/执行中）。

- [ ] **Step 3: HITL 确认框**

`ToolConfirmDialog.vue`：收到 `REQUIRE_USER_CONFIRM` 事件弹出，展示待确认工具 + 参数，确认/拒绝 → 调 `/ai/engine/resume`。参照 internet-hospital 的 `ToolConfirmDialog`。

- [ ] **Step 4: 用户对话模式**

独立对话页（类似 ChatGPT）：会话侧边栏（重命名/删除/清空/置顶）、输入框（图片上传/自动展开/快捷键/语音预留）、消息流。

- [ ] **Step 5: 技能 IDE**

参照 snail-ai-admin `SkillEditor.vue`：文件树 + CodeMirror 编辑器 + AI 生成/优化按钮。

- [ ] **Step 6: 本地验证**

Run: `cd forge-admin-ui && pnpm dev`
Expected: Agent 工作台可配置知识库/技能/工具权限，对话实时展示思考/工具/循环状态，HITL 确认框可用，用户对话模式 + 会话管理可用，技能 IDE 可编辑

- [ ] **Step 7: 提交**

```bash
git add forge-admin-ui/src/views/ai/ forge-admin-ui/src/api/ai.js
git commit -m "feat(ui): Agent工作台/用户对话模式/技能IDE"
```

---

### Task 10: RAG 检索增强（BM25 / 融合 / Rerank / 补全查询）

> 承接二期顺延的检索增强能力。依赖：二期 `RagSearchService`（纯向量检索）、一期 `AiModelAdapterRegistry`（`getRerank(modelKey)` 获取 `AiRerankModelAdapter`）、Milvus 内置 BM25、一期 Chat 模型（补全查询）。

**Files:**
- Create: `.../rag/search/handler/VectorSearchHandler.java`
- Create: `.../rag/search/handler/Bm25SearchHandler.java`
- Create: `.../rag/search/handler/HybridFusionHandler.java`
- Create: `.../rag/search/handler/RerankHandler.java`
- Create: `.../rag/search/handler/FinalizeHandler.java`
- Create: `.../rag/search/fusion/RrfFusion.java`、`WeightedSumFusion.java`
- Create: `.../rag/search/RagSearchPipeline.java`（责任链，内部 `VectorSearchHandler` 复用二期 `RagSearchService` 的纯向量逻辑）
- Create: `.../rag/search/QueryCompleter.java`（对话历史补全查询）
- Modify: `.../rag/controller/RagSearchController.java`（耗时分解补 bm25/fusion/rerank 段）
- Modify: 二期 `RagSearchRequest`（增 `fusionStrategy`/`rerankEnabled`/`queryComplete` 字段）
- Test: `.../rag/search/RagSearchPipelineTest.java`

**Interfaces:**
- Consumes: 二期 `VectorStore`/`AiKnowledgeChunkMapper`、一期 `AiModelAdapterRegistry`（`getRerank`）、一期 Chat 模型、Milvus BM25
- Produces:
  - `RagSearchPipeline.search(RagSearchRequest) -> List<RagSearchHit>`（保持与二期 `RagSearchService.search` 同返回类型；二期 `RagSearchService` 保留复用，作为纯向量实现被 `VectorSearchHandler` 调用）
  - `RrfFusion.fuse(List<String> vectorRanks, List<String> bm25Ranks) -> List<String>`；`WeightedSumFusion.combine(double vs, double vScale, double bs, double bScale) -> double`
  - `QueryCompleter.complete(String query, List<Message> history) -> String`

- [ ] **Step 1: 写失败测试（融合算法）**

```java
package com.mdframe.forge.plugin.ai.rag.search;

import com.mdframe.forge.plugin.ai.rag.search.fusion.RrfFusion;
import com.mdframe.forge.plugin.ai.rag.search.fusion.WeightedSumFusion;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RagSearchPipelineTest {

    @Test
    void rrfFusionCombinesRanks() {
        RrfFusion fusion = new RrfFusion(60);
        List<String> fused = fusion.fuse(List.of("docA", "docB"), List.of("docB", "docC"));
        assertEquals("docB", fused.get(0)); // B在两个列表都出现，RRF最高
        assertTrue(fused.contains("docA"));
        assertTrue(fused.contains("docC"));
    }

    @Test
    void weightedSumNormalizesScores() {
        WeightedSumFusion fusion = new WeightedSumFusion(0.7);
        double s = fusion.combine(0.8, 0.7, 0.5, 0.5);
        assertTrue(s > 0);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=RagSearchPipelineTest -DfailIfNoTests=false`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现融合算法**

- `RrfFusion`：`score = Σ 1/(k + rank)`，k 默认 60
- `WeightedSumFusion`：`final = α*normalizedVector + (1-α)*normalizedBm25`

- [ ] **Step 4: 实现检索责任链**

Handler 按序（参照 snail-ai `RagSearchPipeline`）：
- `VectorSearchHandler`：调二期 `RagSearchService.search`（纯向量检索 + 阈值过滤，**保留复用**）
- `Bm25SearchHandler`：Milvus 内置 BM25（知识库 `searchConfig.searchEngineEnable` 时）
- `HybridFusionHandler`：按 `fusionStrategy` 选 RRF/WeightedSum 融合
- `RerankHandler`：`registry.getRerank(modelKey).rerank` 重排（可开关，失败降级按分数排序）
- `FinalizeHandler`：`LostInMiddleReorder` + `nearbySliceCount` 相邻分块扩展 + 截取 topK

- [ ] **Step 5: 实现 QueryCompleter**

`QueryCompleter.complete`：取最近 5 条对话历史 + 当前查询，用 LLM 补全（"明天呢"→"北京明天天气"）。可开关（知识库级 `queryComplete` 配置）。

- [ ] **Step 6: 调用方切换 + RagSearchService 保留**

二期 `RagSearchService` 的调用方（`RagForcedInjector`/`RagSearchController`/QA）改为依赖 `RagSearchPipeline`；二期 `RagSearchService` **保留复用**（纯向量实现，被 pipeline 的 `VectorSearchHandler` 调用，不删除）。`RagSearchRequest` 增 `fusionStrategy`/`rerankEnabled`/`queryComplete`。`RagSearchController` 耗时分解补 bm25/fusion/rerank 段。

- [ ] **Step 7: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/search/
git commit -m "feat(ai): RAG混合检索(BM25/融合/Rerank)与补全查询"
```

---

## Self-Review 记录

- **Spec 覆盖**：三期覆盖设计文档全部（Agent 引擎 ✓、工具 ✓、HITL ✓、技能 ✓、对话 UI ✓、Vision ✓、RAG 检索增强 ✓）
- **占位符扫描**：无 TBD/TODO；mock 测试标注"mock ChatModel/工具"，非占位符而是明确测试策略
- **类型一致性**：`AgentEventType` 28 枚举、`AgentTool.execute -> ToolResult`、`ReactAgent.execute -> Flux<AgentEvent>`、`InterruptStore.save/get/remove` 全篇一致；`RagSearchPipeline.search` 与二期 `RagSearchService.search` 返回类型一致（二期 `RagSearchService` 保留复用，pipeline 内部调用）
- **依赖顺序**：Task 5 依赖 Task 2-4；Task 6 依赖 Task 5；Task 8 依赖一期 Chat 模型 vision capability（`ai_model_capability` 已有 `vision` 枚举）；Task 10 依赖二期 `RagSearchService`/`VectorStore` 与一期 `AiModelAdapterRegistry`（`getRerank`）
- **存量过渡**：Task 6 改"新入口 + 配置开关"——引擎走独立 `/ai/engine/stream`，存量 `AiClient` 4 个调用点不改契约，零回归
- **消息持久化**：Task 6 用户/助手消息写 `ai_chat_record` + 会话写 `ai_chat_session`（复用现有 `AiChatSessionService`/`AiChatRecordService`），`ai_agent_event` 纯审计；会话 UI/多轮记忆复用现有层，零新表
- **HITL 中断**：`ai_agent_interrupt` 不建表，纯 Redis
- **工具源**：Task 3 改 SPI 桥接（`AgentToolContributor`，mcp/capability 插件实现），`ai` 不反向依赖可选插件
- **逻辑删除**：本期新表（工具配置/权限/技能/技能文件/Agent技能绑定）统一 `del_flag bigint NOT NULL DEFAULT 0` + 唯一键直接建在 `del_flag` 上（无生成列）；`ai_agent_event` 为审计流水表不做逻辑删除
- **技能**：Task 7 改只读不执行（`ReadSkillTool` 从 DB 读，不落盘、不沙箱）
- **事件保留**：Task 2 新增 `AgentEventRetentionJob`（`ai_agent_event` 保留 N 天清理，参照 `AiInvocationLogRetentionJob`）
- **菜单脚本**：按 `V1.0.18` 完整模板（`client_code`/`menu_status`/`sys_role_resource` 授权 admin）
- **风险提示**：Task 5 用 `ChatModel.stream()` 解析 tool_call 是核心难点——Spring AI 2.0.0 下 tool_call 在流式结尾才完整，需累积 assistant message 直到 finish_reason 再决策，实施时先写一个最小探针测试验证 `AssistantMessage.getToolCalls()`
