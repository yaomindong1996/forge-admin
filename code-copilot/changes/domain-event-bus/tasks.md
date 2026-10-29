# Tasks: domain-event-bus

> 对应 `spec.md`。当前状态：`deferred`（2026-10-28 暂缓，仅文档未编码；重启时从 P0 开放问题确认开始）。

## P0 规格与决策（本轮）

- [x] 写出覆盖 BusinessEvent + FlowEvent 的统一 Spec
- [x] 写入 `decisions.md` 架构决策条目
- [ ] Spec 评审确认：开放问题（双写命名 / Outbox 归属 / Business 是否上 Pub/Sub）

## P1 契约与 Bus 骨架（编码阶段启动）

- [ ] 新增 `forge-starter-event`（或等价包）：Envelope、DomainEventBus、Transport SPI
- [ ] 事件类型映射表：旧常量 ↔ 规范名（第 5 节全量）
- [ ] LocalAsync / AFTER_COMMIT 默认 Transport
- [ ] `BusinessEventPublisher` 适配为 Bus 发布（行为等价）
- [ ] `FlowEventPublisher` 适配为 Bus + RedisPubSubTransport（频道兼容）
- [ ] 单元测试：序列化往返、映射表、租户字段必填校验

## P2 消费侧兼容回归

- [ ] 触发器 `BusinessTriggerExecutor` 仍能匹配旧 `eventType`
- [ ] 业务流程 `START_EVENT` / 终态 `business.flow.*` 路径回归
- [ ] `forge-flow-client` Redis 订阅解析兼容（新旧字段共存）
- [ ] 审批回调 → 业务状态 → 再发 business 事件的顺序符合 decision #74

## P3 可靠投递（默认关闭）

- [ ] Outbox 表设计（Flyway 草案）+ Relay SPI
- [ ] 幂等消费表 / 死信表设计
- [ ] 配置开关：`forge.event.transport=local|redis-pubsub|outbox`
- [ ] 关键路径选 1～2 个事件试点（如 `flow.process.completed`、`business.flow.approved`）

## P4 MQ（独立变更，拆服务后）

- [ ] RocketMQ Transport 设计与 Topic 约定
- [ ] 与 Outbox Relay 对接
- [ ] 运维 Runbook（积压、重放、死信）

## 明确不做（本变更）

- [ ] 不引入 Kafka/Rabbit 依赖
- [ ] 不改造 `forge-starter-message` 为总线
- [ ] 不替换 Flowable
