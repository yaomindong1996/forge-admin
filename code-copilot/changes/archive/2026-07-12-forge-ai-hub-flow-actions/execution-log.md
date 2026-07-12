# 执行日志 — Forge AI 中枢受控流程动作

> change: `forge-ai-hub-flow-actions`
> status: reviewed_passed
> created: 2026-07-12

## 1. 启动记录

| 时间 | 阶段 | 事件 | 结论 |
|---|---|---|---|
| 2026-07-12 | Archive | 归档 `forge-ai-hub-secure-actions` | 修复后 Spec/Code Quality PASS，Secure Actions 27/27 |
| 2026-07-12 | Direction | 用户要求自动开始并执行下一阶段 | 进入阶段 2.2 Flow Actions Proposal/Apply |
| 2026-07-12 | Mapping | 检查 FlowClient、BusinessFlowService、采购审批和 BPMN 所有权 | 复用真实 FLOW binding 与任务访问校验，不建设第二套节点配置 |
| 2026-07-12 | Security | 检查任务办理入口 | 必须移除 DTO userId 覆盖，并把 taskId 降为需多重校验的定位符 |

## 2. 已冻结边界

- 固定 capability 元工具不变；
- FLOW_ACTION/FLOW/MEDIUM，首批 START/APPROVE/REJECT；
- USER A 是唯一发起人/办理人来源；
- businessKey 固定 `<objectCode>:<recordId>`；
- BPMN 节点配置归真实流程设计器；
- 不含 claim/delegate/return/withdraw/terminate、R3、消息、Nacos 和真实模型调用。

## 3. Apply 实现

| 时间 | 范围 | 结果 |
|---|---|---|
| 2026-07-12 | 模块装配 | 新增 flow-actions 模块并接入 plugin parent、BOM、Admin 与配置开关 |
| 2026-07-12 | 发布与目录 | FLOW_ACTION 发布快照、grant allowedOperations 交集、固定元工具目录完成 |
| 2026-07-12 | 执行安全 | START/APPROVE/REJECT、USER A、已签收任务、对象/记录/模型绑定复核完成 |
| 2026-07-12 | 身份修复 | `completeBusinessTask` 不再读取 DTO userId，始终取可信当前用户 |
| 2026-07-12 | 幂等审计 | 新增 `ai_capability_flow_action_log` 与 V1.0.24；预留、摘要、双身份和结果快照完成 |
| 2026-07-12 | 事务修复 | 流程执行与 SUCCESS 更新进入同一本地事务；更新失败转 FAILED，不遗留永久 RUNNING |
| 2026-07-12 | 确认摘要 | FLOW_ACTION 仅显示 operation、recordId、taskId 安全尾号和请求指纹 |

## 4. 增量验证

| 验证 | 结果 | 说明 |
|---|---|---|
| Flow Actions 专项 | PASS，11/11 | 含成功日志更新失败回滚和 20 路并发最多一次副作用回归 |
| Secure Actions 专项 | PASS，29/29 | 含 FLOW_ACTION 分发和安全 elicitation 摘要 |
| Control Plane 专项 | PASS，29/29 | FLOW_ACTION 专用发布与 allowedOperations grant |
| Identity 专项 | PASS，35/35 | 双身份、OAuth Profile、失败关闭 |
| MCP 专项 | PASS，16/16 | 协议 2025-06-18、单 Streamable HTTP 与 transport guard |
| Flow Actions Reactor | PASS，34/34 | JDK 17，`-am install -DskipTests` |
| Admin 聚合 | PASS，41/41 | JDK 17，`package -DskipTests` |
| Generator 编译 | PASS | Reactor 编译 438 个源码文件；仅既有 deprecated/unchecked 警告 |
| Mapper XML | PASS | 两个新增 XML 经 `xmllint --noout` |
| Admin JAR | PASS | 包含 `forge-plugin-capability-flow-actions-1.0.0.jar` |
| 禁止依赖 | PASS | dependency tree 无 Spring Authorization Server、Sa-Token OAuth2 |
| 静态扫描 | PASS | 无 DTO userId 读取、tenant_id=0、物理 DELETE、`${`、旧 transport 配置 |
| `git diff --check` | PASS | 按本变更涉及文件范围执行 |

## 5. 失败与修复记录

1. Secure Actions 首次新增摘要断言误把能力名称中的“同意”当成 comment 泄露；将测试 comment 改为唯一敏感标记后 29/29 通过，生产实现未发生泄露。
2. Flow Actions 单模块首次编译从 `~/.m2` 读取旧 Generator JAR，找不到新 `getActionableTaskFormContext`；按多模块规则先执行 34 模块 Reactor install，再复跑 10/10 通过。
3. 身份/MCP 测试中的 database unavailable 堆栈和 transport guard 启动失败是故障注入断言，测试最终均 PASS，不是环境故障。
4. 新增 20 路并发测试首次用同一可变实体模拟数据库已提交快照，出现 `SUCCESS` 可见但结果 JSON 尚未可见的测试替身竞态；改为原子替换不可变快照模拟数据库提交可见性后，11/11 通过。生产代码未出现该失败。

## 6. 条件跳过与边界

- 未启动 Admin，因此未触发真实 Flyway；V1.0.24 仅完成静态 SQL/XML 检查。
- 无隔离 Flow 服务、测试流程和业务数据，未执行真实 START/APPROVE/REJECT E2E；通过 mock 编排与真实模块编译验证。
- FlowClient 可独立部署，远程副作用无法由本地事务回滚；失败时返回稳定错误并保留对账证据，不宣称分布式原子事务。
- 未调用任何真实大模型，也未接入 Nacos MCP Registry/Admin。
- 未 commit、未 push，未清理工作区中用户已有改动。

## 7. Apply 结论

状态置为 `applied_pending_review`。下一步应执行 `/review forge-ai-hub-flow-actions`，Review 修复并通过后再归档。

## 8. Review 执行记录（2026-07-12）

本轮按两阶段规则只读审查真实代码，不采信 Apply 自述作为合规证据。

| 检查 | 结果 |
|---|---|
| 单 Streamable HTTP、固定元工具、FLOW_ACTION/FLOW/MEDIUM、USER、elicitation、迁移与禁止依赖 | PASS |
| R1 Flow 服务最终 tenant/assignee 授权 | FAIL HARD-GATE；approve/reject 未比较 task.assignee 与 userId，也未显式核对任务租户 |
| R2 顶层伪造参数失败关闭 | FAIL HARD-GATE；handler 投影前未检查未知键，危险顶层字段被静默丢弃 |
| R3 远程副作用幂等与恢复 | FAIL HARD-GATE；远程提交成功、本地事务失败时可形成 orphan/FAILED/pending 组合 |
| R4 businessKey 契约 | FAIL；历史 link 场景实际 Flowable key 会追加 `:R...` |
| R5 发布版本事实 | FAIL；来源 SQL 未锚定 design version 的 PUBLISHED 记录 |
| R6 目录契约与 P0 测试 | FAIL；缺 sourceType/behavior/operation，关键安全路径无测试 |
| Flow Actions 专项 | PASS，11/11 |
| Secure Actions 专项 | PASS，29/29 |
| Mapper XML | PASS，`xmllint --noout` |

执行命令：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-flow-actions/pom.xml test

mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-secure-actions/pom.xml test

xmllint --noout \
  forge-framework/forge-plugin-parent/forge-plugin-capability-flow-actions/src/main/resources/mapper/FlowActionExecutionLogMapper.xml \
  forge-framework/forge-plugin-parent/forge-plugin-capability-flow-actions/src/main/resources/mapper/FlowActionSourceMapper.xml
```

未启动任何服务，无需清理 PID；未执行真实 Flyway、Flowable E2E 或模型调用。

Review 结论：`NEEDS_FIX`。Spec Compliance FAIL，按规则未进入 Code Quality Review；当前状态为 `reviewed_with_findings`。下一步执行 `/fix forge-ai-hub-flow-actions`。

## 9. Fix 执行记录（2026-07-12）

| Finding | 修复 | 验证 |
|---|---|---|
| R1 最终授权 | Flow 服务锁定 `sys_flow_task`，比较 tenant/assignee/status，再比较 Flowable assignee | 7/7 PASS |
| R2 顶层伪造 | invoke 投影前白名单，只允许 capabilityCode/version/recordId/idempotencyKey/arguments | 七类危险字段 PASS |
| R3 远程孤儿 | 任务动作凭证和完成态同事务；FAILED/超时 RUNNING 同摘要恢复；START 固定 key 回填 | 恢复与冲突测试 PASS |
| R4 businessKey | FLOW_ACTION 使用专用稳定 key 入口，普通人工流程保持历史语义 | START adapter 测试 PASS |
| R5 发布事实 | JOIN 精确 PUBLISHED design version，sourceVersion 取发布事实 | Mapper 契约测试 PASS |
| R6 目录/测试 | search/describe 实际输出与 Schema 增加 sourceType/behavior/operation | Handler/Contributor PASS |

新增迁移 `V1.0.25__add_flow_task_action_idempotency.sql`，只增加 `sys_flow_task` 的动作幂等字段与索引；未修改 V1.0.24。

### 9.1 命令与结果

- 首次未显式指定 JDK 17，Maven 使用 JDK 8 报 `无效的目标发行版: 17`；改为项目 JDK 17 后通过，属于环境选择修正；
- 一次共享回归命令中的 `*` 未加引号被 zsh 展开拒绝，命令未执行任何测试或写操作；加引号后正常执行；
- Flow Actions + Flow 最终授权：16/16 + 7/7 PASS；
- Secure Actions：31/31 PASS；Generator 发布动作 3/3、Capability Core 6/6、MCP 16/16 PASS；Control Plane、Identity 选定共享回归 PASS；
- `mvn -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests`：43/43 PASS；
- `xmllint --noout`：三个 Mapper XML PASS；禁止依赖/旧 transport/危险 SQL 扫描与 `git diff --check` PASS。

### 9.2 条件跳过与清理

- 未启动 Admin 或 Flow Server，未创建 PID，无服务需要清理；
- 未执行真实 Flyway；V1.0.25 仅静态验证；
- 未准备隔离流程数据，因此未执行真实 START/APPROVE/REJECT E2E；
- 未调用真实模型，未接入 Nacos MCP Registry/Admin；
- 未 commit、未 push，未清理用户工作区改动。

Fix 结论：R1–R6 已完成，状态置为 `fixed_pending_review`，进入二次 Review。

## 10. 二次 Review、增量修复与归档验收（2026-07-12）

### 10.1 Review 新发现与修复

| Finding | 修复 | 结果 |
|---|---|---|
| R7 SUCCESS 重放被 actionable 预检拦截 | SUCCESS 同摘要直接复用；活动 RUNNING 提前冲突；FAILED/超时 RUNNING 受控恢复 | Flow Actions 19/19 |
| R8 独立 Flow 服务无法识别 `fdu_` | 60 秒内部 Sa-Token 委托桥，Session 绑定 USER A/tenant/org/client/marker，失败禁止静态账号降级 | Client 2/2、Auth 7/7、Controller 3/3 |
| 普通用户 Token 可探测 delegated START | 专用入口要求权限并验证 Token Session 委托标记 | verifier/controller PASS |
| 委托 device 可能受共享/并发策略复用 | 每次签发唯一 device；空 Token、空 Session、非正 clientId 失败关闭 | Auth 7/7 |
| 同租户跨套件重名对象可能误取 FLOW binding | Source SQL 同时要求 `b.target_id=o.id` 和 `target_code` | Mapper 契约与 XML PASS |
| Flowable 完成后任务状态写回结果被忽略 | 状态与幂等凭证必须更新一行，否则抛错回滚 | Flow 编译、授权测试与聚合 PASS |

### 10.2 最终验证证据

- 受影响模块 Reactor install：37/37 PASS；
- Flow Client 2/2、Auth bridge 7/7、Flow Controller 3/3、Flow Actions 19/19、Flow authorization 7/7；
- Secure Actions 31/31、Identity 35/35、Control Plane 29/29、MCP 16/16；
- `mvn -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests`：43/43 PASS；
- `xmllint --noout`：Flow Actions 两个 Mapper 与 FlowTaskMapper PASS；
- V1.0.24/V1.0.25 无 `tenant_id=0`、业务 `${...}`、物理 DELETE；相关文件无尾随空格；
- dependency tree 无 Spring Authorization Server、Sa-Token OAuth2；配置无 SSE/STATELESS/stdio/ASYNC；
- 一次 dependency tree 从仓库根执行因 reactor 路径错误失败；切换到 `forge-server` 后 PASS，未产生代码或环境副作用。

### 10.3 条件跳过与清理

- 未启动 Admin/Flow 服务，未执行真实 Flyway 或真实 Flowable E2E，无 PID 需要清理；
- 未调用真实大模型，未接入 Nacos MCP Registry/Admin；
- 未 commit、未 push、未清理用户工作区其它改动。

二次 Spec Compliance 与 Code Quality 均 PASS，状态置为 `reviewed_passed`，允许归档。
