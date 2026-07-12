# 测试规格 — Forge AI 中枢受控流程动作

> status: reviewed_passed
> created: 2026-07-12

## 1. P0 安全矩阵

- SERVICE invoke、scope 不足、grant 撤销、用户权限撤销、tenant/org 不一致均零 FlowClient 调用；
- 未发布/停用对象、无 FLOW binding、binding/model/version 漂移拒绝；
- 客户端 userId、flowModelKey、variables、processInstanceId 和未知字段被 Schema 拒绝；
- START 使用 `<objectCode>:<recordId>`，同记录运行中流程不重复启动；
- APPROVE/REJECT 只允许当前 A 已签收任务，候选未签收、他人任务、已办任务拒绝；
- task businessKey/objectCode/recordId/processDefKey 任一不匹配拒绝；
- 无 elicitation、DECLINE/CANCEL/confirm=false 均零副作用；
- 相同幂等键同请求返回已有结果，不同请求拒绝；
- 审计或幂等预留失败时零副作用；
- 正常和异常路径无身份、租户、MDC 泄漏。

## 2. P0 协议矩阵

- initialize 协议仍为 `2025-06-18`；
- tools/list 仍只有固定 ping/search/describe/invoke；
- 单 `/mcp` Streamable HTTP，不出现旧 SSE/STATELESS/stdio/ASYNC；
- FLOW_ACTION search/describe 返回结构化 Schema，invoke 返回统一结果；
- 未认证/Origin/身份基础设施错误仍由 HTTP 401/403/503 处理。

## 3. P1 功能矩阵

- 发布 API 为每个 operation 生成稳定 sourceKey、Schema、policy snapshot；
- START 从发布业务记录和 FLOW binding 构建流程；
- APPROVE/REJECT 固定使用 A，comment 和 action 正确映射；
- CapabilityInvocationLog 与 FlowActionLog requestId 可关联；
- actorUserId/clientId/serviceUserId/tenant/activeOrg 完整记录，不保存 comment 正文。

## 4. 计划命令

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-capability-flow-actions -am install -DskipTests

mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-flow-actions/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-secure-actions/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-control-plane/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-mcp/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-identity/pom.xml test
mvn -pl forge-admin-server -am package -DskipTests
```

## 5. 条件跳过

- 无受控 MySQL 时不执行 V1.0.24 真实迁移，只做静态检查；
- 无独立 Flow 服务与测试流程数据时不冒充真实 START/APPROVE/REJECT E2E，通过 mock FlowClient/BusinessFlowService 验证安全编排；
- 不调用真实大模型；本阶段验证能力与流程执行链，不验证模型自主决策。

## 6. Apply 验证结论

- Flow Actions 11/11、Secure Actions 29/29、Control Plane 29/29、Identity 35/35、MCP 16/16 全部通过；
- Flow Actions Reactor 34/34、Admin 聚合 41/41 成功；Generator 由 Reactor 编译 438 个源码文件通过；
- Mapper XML、迁移静态规则、JAR 装配、禁止依赖和 Streamable HTTP 配置扫描通过；
- 未执行的真实 Flyway、真实 Flowable E2E 和真实模型调用仍按条件跳过，等待 Review 后由人工环境验收。

## 7. Review 增量验证（2026-07-12）

本轮只读复跑：

- Flow Actions：11/11 PASS；
- Secure Actions：29/29 PASS；
- 两个 Flow Actions Mapper XML：`xmllint --noout` PASS；
- 未启动服务、未执行 Flyway、未调用真实 Flowable 或模型。

Review 发现测试矩阵缺口：

- 缺少顶层 `userId/tenantId/activeOrgId/flowModelKey/variables/processInstanceId/unknown` 失败关闭测试；
- 缺少任务确认后转签、跨租户任务和 Flow 服务最终 assignee 校验测试；
- 缺少 START 成功、历史实例重发 businessKey、binding/version 漂移测试；
- 缺少远程 Flow 成功、本地 link/SUCCESS 日志提交失败后的幂等恢复测试；
- 缺少 search/describe 返回 sourceType/behavior/operation 的契约测试。

结论：现有测试均绿，但 Spec Compliance FAIL，状态为 `reviewed_with_findings`。Fix 后必须先补上述 Red/Green，再复跑共享回归。

## 8. Fix 增量验证（2026-07-12）

- Flow Actions：16/16 PASS，新增 START 专用固定 key 入口、FAILED/超时 RUNNING 恢复、发布版本事实锚定；
- Flow 最终授权：7/7 PASS，覆盖跨租户、确认后转签、非办理人幂等探测、同请求完成态和普通 UI 自动分配兼容；
- Secure Actions：31/31 PASS，覆盖七类顶层危险字段失败关闭、目录元数据真实输出和 Schema；
- Generator 发布动作：3/3 PASS；Capability Core：6/6 PASS；MCP：16/16 PASS；Control Plane 与 Identity 共享回归 PASS；
- Admin + Flow Server 聚合：43/43 PASS；Generator 438 源码、Flow plugin 124 源码、Flow Server 25 源码编译通过；
- 三个 Mapper XML、V1.0.25 静态规则、禁止依赖、旧 transport 和 `git diff --check` PASS。

本轮仍不执行真实 Flyway、真实 Flowable E2E 或真实模型调用。状态进入 `fixed_pending_review`，待二次两阶段 Review。

## 9. 二次 Review 与身份桥增量验证（2026-07-12）

- Flow Client delegated identity：2/2 PASS，确认请求体不传 userId/userName/deptId/deptName，委托签发失败不降级静态 Token；
- Sa-Token 委托签发与 Session verifier：7/7 PASS，覆盖 60 秒 TTL、唯一 device、空 Token、SERVICE 身份、普通 Session 无标记和非正 clientId；
- Flow delegated controller：3/3 PASS，确认 START 只取可信 Session，任务身份/租户不能由 DTO 覆盖；
- Flow Actions：19/19 PASS；Flow 最终任务授权：7/7 PASS；
- Secure Actions 31/31、Identity 35/35、Control Plane 29/29、MCP 16/16 PASS；MCP 协议仍为 `2025-06-18` 且仅 Streamable HTTP；
- Admin + Flow Server 聚合 package：43/43 PASS；三个 Mapper XML、迁移静态规则、禁止依赖和尾随空格检查 PASS。

一次从仓库根目录运行 Maven dependency tree 因 reactor 路径错误失败，切换到 `forge-server` 后同命令 PASS；这是命令工作目录问题，不是代码或依赖失败。

未启动服务，无 PID 需要清理；真实 Flyway、真实 Flowable E2E 和真实模型调用继续按条件跳过。二次 Review 结论为 PASS。
