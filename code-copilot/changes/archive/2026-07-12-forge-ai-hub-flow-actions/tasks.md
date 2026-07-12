# Forge AI 中枢受控流程动作实施计划

> 变更名：`forge-ai-hub-flow-actions`
> 状态：fixed_pending_review
> 原则：发布 FLOW 绑定 → 实时授权 → 任务归属 → elicitation → 幂等流程副作用 → 双审计

## Task 1：固定元工具受控执行 SPI

- [x] 为阶段 2.1 网关增加 `GovernedCapabilityExecutionAdapter`，按 sourceType 解析、确认和执行；
- [x] 保持现有 BUSINESS_ACTION 执行路径，并通过适配 SPI 分发 FLOW_ACTION；
- [x] 目录 SQL 精确允许 BUSINESS_ACTION/ACTION 与 FLOW_ACTION/FLOW 两种 MEDIUM 组合；
- [x] search/describe 输出行为、来源和 operation，不泄露未授权能力。

## Task 2：FLOW_ACTION 发布契约

- [x] 新建 flow-actions 模块、POM、BOM、plugin parent、Admin 聚合与自动配置；
- [x] 新建发布 DTO/Controller/Publisher/Mapper XML；
- [x] 只从启用且已发布对象与启用 FLOW binding 生成 START/APPROVE/REJECT；
- [x] Schema 使用 Draft 2020-12、additionalProperties=false、Long ID 字符串；
- [x] Control Plane 禁止通用 publish 绕过 FLOW_ACTION 专用发布入口。

## Task 3：grant 与运行时策略

- [x] FLOW_ACTION grant 必须是 FLOW/MEDIUM 且配置非空 allowedOperations；
- [x] grant 只能缩小版本 operation，撤销/过期下一请求实时失效；
- [x] 调用时复核 resolved version 的 bindingId/modelKey/objectVersion/operation；
- [x] 基础设施故障使用稳定 `CATALOG/AUTHORIZATION/FLOW_*_UNAVAILABLE`。

## Task 4：START/APPROVE/REJECT 执行器

- [x] START 只调用 `startDocumentFlow`，不传 flowModelKey、variables 或客户端身份；
- [x] APPROVE/REJECT 先验证 A 是已签收办理人，再验证 objectCode/recordId/processDefKey；
- [x] 移除 `completeBusinessTask` 对 DTO userId 的信任；
- [x] REJECT comment 必填，所有 comment 不进入普通日志；
- [x] 不实现 claim/delegate/return/withdraw/terminate。

## Task 5：幂等、确认与双审计

- [x] 创建流程动作日志实体/Mapper/XML/Service，先预留再副作用；
- [x] canonical SHA-256 digest 绑定版本、recordId、taskId、comment；
- [x] 唯一键预留和并发冲突路径保证相同键最多一次进入副作用；
- [x] elicitation 仅 ACCEPT + confirm=true 继续；
- [x] Capability 审计、流程动作日志、流程实例/任务用 requestId 串联。

## Task 6：Flyway 与权限

- [x] 新增 V1.0.24 日志表、逻辑删除唯一键和 publish/invoke 权限资源；
- [x] CREATE IF NOT EXISTS/NOT EXISTS、tenant_id=1、显式列名；
- [x] placeholder、物理 DELETE、敏感数据、旧 SSE/STATELESS/stdio/ASYNC 扫描通过。

## Task 7：增量验证与回填

- [x] Flow Actions、Secure Actions、Generator 编译、Control Plane、Identity、MCP 专项验证通过；
- [x] Admin JDK 17 聚合 package 通过；
- [x] dependency tree 无 Spring Authorization Server、Sa-Token OAuth2；
- [x] 未执行真实 Flyway、真实 Flowable E2E 或真实模型时明确记录条件跳过；
- [x] 更新 spec/test-spec/execution-log，状态置为 `applied_pending_review`。

## Task 8：Review Findings

- [x] 完成阶段一 Spec Compliance Review；结论 FAIL，按规则未进入 Code Quality Review；
- [x] R1：Flow 服务最终办理入口按可信 tenant + assignee + task status 重新授权，消除转签 TOCTOU 和内部接口绕过；
- [x] R2：invoke 在任何字段投影前拒绝顶层未知字段和身份/流程控制字段；
- [x] R3：建立远程流程副作用的幂等与可恢复回填协议；
- [x] R4：FLOW_ACTION START 固定业务 key，普通人工流程历史重发保持兼容；
- [x] R5：来源查询锚定实际 `PUBLISHED` 的业务对象设计版本记录；
- [x] R6：search/describe 补齐 sourceType/behavior/operation，并补全 P0 Red/Green 测试；
- [x] R7：成功幂等重放绕过任务可办理预检，活动 RUNNING 在查询任务前失败关闭；
- [x] R8：增加 60 秒 Sa-Token 用户委托桥、专用 delegated START 与 Session 委托标记验证；
- [x] 二次 Spec Compliance 与 Code Quality Review 通过，允许归档。
