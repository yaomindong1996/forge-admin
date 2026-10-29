# Forge 领域事件总线 Spec（BusinessEvent + FlowEvent）

> 变更名：`domain-event-bus`  
> 状态：`deferred`（2026-10-28 确认暂缓，不进入编码）  
> 创建日期：2026-10-28  
> 实施方式：先 Spec / 后分阶段编码；**本期文档不写实现代码**  
> 范围：统一低代码 `BusinessEvent` 与流程 `FlowEvent` 的事件契约与投递抽象；一期不引入 Kafka/Rabbit/RocketMQ

## 1. 背景

Forge 当前是单体为主、Admin 与 Flow 可选分部署。异步能力分散在：

| 通道 | 用途 | 可靠性 |
|------|------|--------|
| Spring `ApplicationEvent` + `@EventListener` | 配置刷新、数据权限、部分内部协作 | 进程内，无持久 |
| `@Async` | 触发器、导出、发布后处理 | 进程内，崩溃可丢 |
| `BusinessEventPublisher` | 低代码 CRUD / 状态 / 流程结果 → 触发器与业务流程编排 | 同 JVM 异步，无 Outbox |
| `FlowEventPublisher` → Redis Pub/Sub | 流程生命周期跨进程广播 | **不持久、不 ack、不重试** |
| Quartz / Job | 定时扫描与补偿 | 适合周期任务，不适合主事件通道 |
| `forge-starter-message` | 站内信 / 邮件 / 短信 | 通知通道，不是领域总线 |

问题不是「没有异步」，而是：

1. **两套事件模型**：`BusinessEvent`（generator）与 `FlowEventMessage`（core/flow）字段、类型命名、租户/幂等语义不一致。  
2. **无统一发布 API**：业务代码有的直接调触发器，有的 Redis，有的 Spring Event。  
3. **可靠投递缺口**：事务已提交后的副作用失败只能打日志；Pub/Sub 丢消息无补偿。  
4. **拆服务未到**：现在上 Kafka/Rabbit 运维成本高于收益。

本变更目标：**先收口契约与总线抽象，BusinessEvent 与 FlowEvent 同时纳入；可靠投递与 MQ 分阶段，默认可插拔。**

## 2. 目标与非目标

### 2.1 目标

1. 定义统一 **Domain Event 信封（Envelope）**，Business / Flow 两类 payload 都挂在同一信封下。  
2. 提供 **`DomainEventBus` 抽象**（概念上落在未来 `forge-starter-event`），业务只 `publish`，不感知 Transport。  
3. **一期**：Local（同步提交后 / 异步）为默认；Flow 现有 Redis Pub/Sub 降级为可选 Transport，行为兼容。  
4. **二期**：Transactional Outbox +（Redis Stream 或 DB 扫表）做可靠投递；消费幂等与死信约定明确。  
5. **三期**：拆服务后再接 RocketMQ（默认候选）等 MQ，只换 Transport。  
6. 明确事件清单、命名规范、租户上下文传播、与现有触发器 / 流程回调 / 通知的边界。

### 2.2 非目标

- 本期不引入 Kafka / RabbitMQ / RocketMQ 依赖与部署。  
- 不替换 Flowable；流程状态机仍由 Flow 引擎负责。  
- 不把 `forge-starter-message` 改造成总线；通知只作为 Handler。  
- 不一次性重写全部触发器 / 业务流程编排实现；一期以适配器兼容为主。  
- 不做前端 i18n 或与事件无关的改造。

## 3. 架构分层

```
业务插件 (generator / flow / system / …)
        │  publish(envelope) 或 兼容适配器
        ▼
DomainEventBus（starter）
  · 填充 eventId / occurredAt / tenant 快照
  · 默认 AFTER_COMMIT；显式 SYNC 少用
        │
        ├─ LocalSyncTransport / LocalAsyncTransport   （一期默认）
        ├─ RedisPubSubTransport                       （兼容 Flow 广播）
        └─ OutboxTransport → Relay → Stream/MQ        （二期 / 三期）
        ▼
Handlers（按 eventType 订阅）
  · BusinessTriggerHandler
  · BusinessProcessStartHandler
  · FlowCallback / 状态修复 Handler
  · Notification / Webhook / Audit（可选）
```

**原则**：写接口与领域服务只发事件；副作用走 Handler。Handler 必须可幂等，异步线程使用信封内显式 `tenantId` / `operatorId`，禁止隐式 ThreadLocal 碰运气。

## 4. 统一事件信封（契约）

所有跨模块事件必须可序列化为下列逻辑结构（字段名实现时可微调，语义不可缺）：

| 字段 | 必填 | 说明 |
|------|------|------|
| `eventId` | 是 | 全局唯一（雪花 / UUID），消费幂等主键之一 |
| `eventType` | 是 | 稳定字符串，见第 5 节命名 |
| `eventVersion` | 是 | 信封版本，从 `1` 起 |
| `occurredAt` | 是 | 事件发生时间（业务时间，非投递时间） |
| `tenantId` | 是 | 租户；缺省不得静默变 0 |
| `source` | 是 | 产生方：`generator` / `flow` / `system` / … |
| `aggregateType` | 建议 | 如 `business_object` / `flow_instance` |
| `aggregateId` | 建议 | `objectCode:recordId` 或 `processInstanceId` |
| `idempotencyKey` | 建议 | 业务幂等键；缺省可用 `eventId` |
| `traceId` | 可选 | 与日志追踪对齐 |
| `operatorId` / `operatorName` | 可选 | 操作人快照 |
| `payload` | 是 | 类型相关体：BusinessPayload 或 FlowPayload |
| `headers` | 可选 | 扩展键值，禁止塞大对象明文敏感信息 |

### 4.1 BusinessPayload（由现有 `BusinessEvent` 演进）

保留并规范化：

- `suiteCode`, `objectCode`, `configKey`, `recordId`
- `recordData`, `previousData`（大字段策略：一期可内联；二期可改为引用 + 按需加载，Spec 预留）
- 字段读取继续兼容平铺 / `{main, children}`（现有 `readRecordValue` 语义不得破坏）

### 4.2 FlowPayload（由现有 `FlowEventMessage` 演进）

保留：

- `processInstanceId`, `processDefKey`, `processDefId`
- `businessKey`, `businessType`, `title`
- `applyUserId` / 姓名、任务相关字段（taskId、assignee、nodeKey 等现有字段）
- `variables` 仅允许非敏感摘要或显式白名单；禁止默认全量落 Outbox

### 4.3 兼容策略

- 一期：`BusinessEventPublisher` / `FlowEventPublisher` 改为调用 Bus（或薄适配），对外类型名可暂留，避免大爆炸重构。  
- Redis 频道名一期保持 `flow:event:{processDefKey}` 与 `flow:event:all`，避免已部署订阅方断裂。  
- JSON 增加信封字段时：**旧字段继续写出**，新订阅方读信封，旧订阅方忽略未知字段。

## 5. 事件清单与命名

### 5.1 命名规范

- 格式：`{Domain}.{Action}` 或继续兼容现有大写下划线（一期 **双写别名表**）。  
- 推荐长期：`business.record.created`、`flow.process.completed`。  
- 一期兼容常量：现有 `RECORD_CREATED`、`PROCESS_COMPLETED` 等仍有效，Bus 内维护映射表。

### 5.2 Business 事件（纳入 Bus）

| 现有常量 | 建议规范名 | 生产者 | 典型消费者 |
|----------|------------|--------|------------|
| `RECORD_CREATED` | `business.record.created` | Dynamic CRUD / 单据运行时 | 触发器、START_EVENT 流程 |
| `RECORD_UPDATED` | `business.record.updated` | 同上 | 触发器 |
| `RECORD_DELETED` | `business.record.deleted` | 同上 | 触发器 |
| `STATUS_CHANGED` | `business.status.changed` | Publisher 检测状态字段 | 触发器 |
| `FIELD_CHANGED` | `business.field.changed` | （若启用） | 触发器 |
| `FORM_SUBMITTED` | `business.form.submitted` | 与 create/update 伴随 | 触发器 |
| `FLOW_APPROVED` | `business.flow.approved` | 流程回调协调器 | 触发器、业务状态 |
| `FLOW_REJECTED` | `business.flow.rejected` | 同上 | 同上 |
| `FLOW_CANCELED` | `business.flow.canceled` | 同上 | 同上 |
| `SCHEDULED_DUE` | `business.schedule.due` | 调度触发 | 触发器 |

### 5.3 Flow 事件（纳入 Bus）

| 现有常量 | 建议规范名 | 生产者 | 典型消费者 |
|----------|------------|--------|------------|
| `PROCESS_STARTED` | `flow.process.started` | Flow 服务 | Admin 订阅、业务回调 |
| `PROCESS_COMPLETED` | `flow.process.completed` | Flow 服务 | 业务终态、通知 |
| `PROCESS_REJECTED` | `flow.process.rejected` | Flow 服务 | 业务驳回态 |
| `PROCESS_CANCELED` | `flow.process.canceled` | Flow 服务 | 撤回态 |
| `TASK_CREATED` | `flow.task.created` | Flow 服务 | 待办、通知、状态 heal |
| `TASK_COMPLETED` | `flow.task.completed` | Flow 服务 | 状态推进 |
| `TASK_ASSIGNED` | `flow.task.assigned` | Flow 服务 | 待办刷新 |

### 5.4 Business ↔ Flow 关系（禁止混用语义）

- **Flow 引擎事实**：以 `flow.*` 为准（任务创建/完成、流程终态）。  
- **业务单据语义**：以 `business.*` 为准（对象记录、单据状态、触发器）。  
- 审批通过后：Flow 发 `flow.process.completed`；Admin/generator 在 Handler 中更新单据并再发 `business.flow.approved`（或保持现有 Callback → Publisher 路径，但都走 Bus）。  
- **禁止**用 Redis Flow 事件直接当触发器输入而不经业务层规范化（避免 Admin/Flow 双源状态打架）。

## 6. 投递语义分阶段

### 6.1 一期（默认，不引入 MQ）

| 项 | 约定 |
|----|------|
| 默认 Transport | LocalAsync（事务 **AFTER_COMMIT** 后投递） |
| Flow 跨进程 | RedisPubSubTransport，频道与 JSON **兼容现网** |
| 失败 | 记错误日志；关键路径可依赖现有 Job/人工补偿 |
| 成功标准 | 契约统一、双发布点收口到 Bus、行为与现网等价可回归 |

### 6.2 二期（可靠异步）

触发条件（满足任一再开做）：外部 Webhook/ERP 不可丢、多实例抢占消费、需要统一重试与死信。

| 项 | 约定 |
|----|------|
| 写入 | 业务事务内写 `event_outbox`（或等价表） |
| 中继 | 异步 Relay 投递 Redis Stream / 或 Job 扫 Outbox |
| 消费 | 消费者组；成功写 `event_consumed(tenant_id, event_id)` |
| 失败 | 有限重试 → `event_dead_letter` + 告警钩子 |
| Pub/Sub | 可保留作「即时 UI/提示」；**可靠副作用只认 Outbox** |

### 6.3 三期（拆服务）

- `MqTransport`（默认候选 RocketMQ）；Topic 建议：`forge.business.{objectCode}`、`forge.flow.{processDefKey}` 或按 `eventType` 聚合。  
- Bus API 不变；仅配置切换 Transport。

## 7. 模块与归属（实现阶段指引）

| 模块 | 职责 |
|------|------|
| 未来 `forge-starter-event` | Envelope、Bus、Transport SPI、Outbox 抽象、幂等工具 |
| `forge-plugin-generator` | BusinessPayload 适配、`BusinessEventPublisher` 委托 Bus、触发器 Handler |
| `forge-plugin-flow` / `forge-flow-server` | FlowPayload 适配、原 `FlowEventPublisher` 委托 Bus |
| `forge-flow-client` | 订阅侧改为 Bus 订阅或继续听兼容 Redis 频道 |
| `forge-starter-message` | 仅作 Notification Handler，不拥有总线 |
| `forge-starter-job` | Outbox Relay / 死信扫描（二期） |

依赖方向：plugin → starter-event → core；**禁止** starter-event 依赖 generator。

## 8. 安全与多租户

1. `tenantId` 必填；消费前恢复租户上下文（显式传参或 Scoped 包装），禁止依赖错误 ThreadLocal。  
2. Payload / Outbox **禁止**落库密码、Token、完整证件号等；需审计时只留摘要。  
3. 跨租户订阅禁止；管理端诊断接口不得枚举他租户事件正文。  
4. 幂等键在租户内唯一：`(tenant_id, event_id)` 或 `(tenant_id, idempotency_key)`。

## 9. 验收标准（编码阶段）

### 9.1 一期

- [ ] 存在统一 Envelope 与类型映射表（Business + Flow 全覆盖第 5 节清单）。  
- [ ] `BusinessEventPublisher` 与 `FlowEventPublisher` 均经 Bus 发布（或经官方适配器）。  
- [ ] 低代码：创建/更新/删除仍能触发既有触发器与 START_EVENT（回归用例）。  
- [ ] 流程：Redis `flow:event:all` 仍能被现有 Client 订阅解析（兼容字段保留）。  
- [ ] 文档：`decisions.md` 已记录本决策；本 Spec 为实施依据。  

### 9.2 二期（另开任务启用时）

- [ ] Outbox 与业务同事务；进程崩溃后 Relay 可补投。  
- [ ] 重复投递不导致重复副作用（幂等表或业务幂等）。  
- [ ] 死信可查询、可手工重放。  

## 10. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 大 payload 撑爆 Redis/Outbox | 二期改为引用；一期限制 variables/recordData 体积告警 |
| 双写映射遗漏导致触发器不匹配 | 映射表单测 + 兼容别名 |
| AFTER_COMMIT 与现有同步假设冲突 | 默认 AFTER_COMMIT；个别 Handler 可声明 SYNC 并文档化 |
| 与现有「审批回调 AFTER_COMMIT」决策冲突 | 对齐 decision #74：终态本地事务先提交，再发 Bus 事件 |

## 11. 开放问题（实现前确认）

1. 规范名 `business.*` / `flow.*` 是一期双写还是仅内部映射、对外仍只暴露旧常量？（**建议：一期双写，对外旧常量为主**）  
2. Outbox 表放公共库还是按服务分表？（**建议：Admin 侧业务 Outbox；Flow 侧流程 Outbox，二期再定统一中继**）  
3. `recordData` 是否在一期就禁止入 Redis Pub/Sub？（**建议：Flow Pub/Sub 继续现状；Business 一期不走 Pub/Sub**）

## 12. 实施分期与 tasks 索引

详见同目录 `tasks.md`。顺序固定：

1. P0 Spec 评审通过（本文）  
2. P1 契约 + Bus 骨架 + 双适配器（行为等价）  
3. P2 触发器 / 流程订阅回归  
4. P3 Outbox 设计与表结构（可只出设计，开关默认关）  
5. P4 拆服务后 MQ Transport（独立变更）
