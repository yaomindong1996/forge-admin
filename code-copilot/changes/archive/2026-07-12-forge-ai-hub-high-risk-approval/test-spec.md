# 测试规格 — Forge AI 中枢高风险动作人工审批

> status: passed_with_environment_limits
> created: 2026-07-12

## 1. P0 安全矩阵

- HIGH invoke 在审批前零 `BusinessActionExecutionService` 调用；
- SERVICE、scope/grant/权限撤销、tenant/org 不一致、MEDIUM/HIGH 路径混用均失败关闭；
- 客户端审批人、候选组、flowModelKey、expire、身份和未知字段全部拒绝；
- 无 KEK/错误 keyId/tag/ciphertext/AAD/digest 时零执行；
- 明文参数、密钥、Token、Header 不出现在表、日志、MCP 输出；
- 审批通过后客户端停用、credentialVersion 变化、grant 撤销、用户停用、权限撤销、能力/版本/policy 漂移、业务状态变化均零执行；
- 重复 APPROVED/REJECTED/CANCELLED 回调与并发恢复最多一次副作用；
- approval.get 跨 client/actor/tenant/org 查询拒绝。

## 2. P0 幂等与状态机

- 20 路相同请求只一条有效 approval 和一个固定 businessKey 流程；
- 同幂等键异摘要冲突；相同摘要返回同 approvalRequestId；
- Flow 启动成功本地回填失败可用 RESERVED 恢复；
- RESERVED/PENDING/EXECUTING 超时不被普通调用抢占重复执行；
- terminal 状态不可回退，EXPIRED/REJECTED/CANCELLED 永不解密执行。

## 3. P1 功能与协议

- HIGH 发布版本、policy、Schema、字段和动作快照正确；
- 默认 BPMN 不覆盖非空设计，候选组只来自 policy；
- 审批表单只读展示安全摘要；
- approval.get 返回稳定结构；
- MCP `2025-06-18`、单 Streamable HTTP，固定工具增加 approval.get；
- MEDIUM 与 FLOW_ACTION 既有行为不回归。

## 4. 计划命令

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-capability-high-risk-approval -am install -DskipTests

mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-high-risk-approval/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-secure-actions/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-identity/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-mcp/pom.xml test
mvn -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests
```

## 5. 条件跳过

- 无隔离 MySQL 时不执行真实 Flyway，只做 SQL/XML 静态检查；
- 无独立 Flow 测试数据和审批人时不冒充真实人工审批 E2E；通过 mock FlowClient、回调和真实模块编译验证；
- 不调用真实大模型；本阶段验证治理和人工审批链路。

## 6. 最终执行结果

| 验证范围 | 结果 |
|---|---|
| High Risk Approval | 24/24 PASS，包含 20 路并发收敛、加密篡改、模型保留、审批归属、回调幂等和只读表单脱敏 |
| Secure Actions | 32/32 PASS，HIGH 路由到审批适配器并审计 `PENDING_APPROVAL`，MEDIUM 保持同步执行 |
| Flow Actions | 19/19 PASS |
| Flow Client / Flow Controller / 委托桥 | 2/2、3/3、7/7 PASS |
| Control Plane / Identity / MCP | 29/29、35/35、16/16 PASS |
| Admin + Flow Server 聚合 | 44 模块 package PASS，包含 High Risk 模块装配 |
| 静态安全检查 | Mapper XML、Flyway placeholder/tenant、禁止依赖、旧 transport 扫描 PASS |

条件限制保持不变：未执行真实 MySQL Flyway、真实 Flowable 人工审批 E2E、真实模型调用；未启动 Admin/Flow 服务，不产生服务 PID。

## 7. 归档前增量验证

- 变更范围：最后的 `expireSeconds @NotNull`、固定 flow-model-key、`EXECUTING` 状态及聚合装配；
- 复用已通过的 High Risk 24/24 和全阶段回归基线，归档前增量复跑 Admin + Flow Server 44 模块 package；
- 结果：`BUILD SUCCESS`；Mapper/Flyway/禁止依赖/旧 transport/空白静态检查 PASS；
- 跳过项：真实 MySQL Flyway 和 Flowable 人工审批 E2E，原因是本轮不启动会更改数据库/流程运行态的服务。
