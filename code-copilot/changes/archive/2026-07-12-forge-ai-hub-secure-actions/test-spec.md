# 测试规格 — Forge AI 中枢受控业务动作

> status: done
> created: 2026-07-12

## 1. P0 安全矩阵

- 只有合法 fdu USER Token 可 invoke；SERVICE Token 返回 `USER_DELEGATION_REQUIRED`；
- tenant/client/actor/serviceUser/activeOrg 任一不一致拒绝；
- grant、client、用户权限或组织撤销后下一请求拒绝；
- 未发布、已停用、HIGH、非 BUSINESS_ACTION、非 ACTION 能力拒绝；
- 发布快照不存在或 sourceVersion 不匹配拒绝；
- START_FLOW、SEND_MESSAGE、DOMAIN_ACTION、未知/嵌套不安全步骤拒绝；
- 字段不在版本 policy、grant policy 或发布模型字段交集时拒绝；
- MCP 客户端不支持 elicitation、DECLINE、CANCEL 或异常时不产生写入；
- 只有 ACCEPT 进入一次执行；
- 相同幂等键相同请求返回原结果，不同请求返回冲突；
- 正常和所有异常路径无身份、租户、MDC 泄漏。

## 2. P0 协议矩阵

- initialize 协议版本 `2025-06-18`；
- tools/list 恰好包含 ping/search/describe/invoke；
- search/describe 注解为只读，invoke 为 destructive + idempotent；
- 三个元工具返回 text JSON + structuredContent；
- 未认证仍为 HTTP 401，Origin 为 403，身份基础设施为 503；
- 不出现 `/sse`、STATELESS、stdio、ASYNC 或第二 MCP endpoint。

## 3. P1 功能矩阵

- 发布 API 从发布动作生成稳定 sourceKey、Schema 和 policy snapshot；
- search 支持 query、limit 1～50、hasMore，不加载全库；
- describe 返回 resolvedVersion、Schema、allowedFields 和 confirmationRequired；
- invoke 正确映射 recordId、arguments、idempotencyKey，tenant/user/org 不从参数读取；
- BusinessActionExecutionLog 与 CapabilityInvocationLog 可通过 requestId 关联；
- create_by/update_by/create_dept 使用 A/activeOrg。

## 4. 静态与构建验证

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-capability-secure-actions -am install -DskipTests

mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-secure-actions/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-generator/pom.xml \
  -Dtest=BusinessActionExecutionServiceTest,BusinessObjectActionServicePublishedTest test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-control-plane/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-mcp/pom.xml test
mvn -Penable-tests -f forge-framework/forge-plugin-parent/forge-plugin-capability-identity/pom.xml test
mvn -pl forge-admin-server -am package -DskipTests
```

## 5. Apply 验证结果（2026-07-12）

- Secure Actions：18/18；
- Generator 本轮专项：7/7；
- Control Plane：24/24；
- MCP：16/16；
- Identity：35/35；
- Admin 聚合：40/40 reactor modules SUCCESS；
- Mapper XML、Flyway placeholder/tenant/物理删除、禁用依赖与传输 Guard 静态检查通过；
- Generator 全量基线另有 289 项中的 2 failures + 1 error，分别位于公式/低代码既有测试，本轮相关专项全部通过，详见 execution-log。

## 6. 条件跳过

- 未提供受控 MySQL 时，不执行 V1.0.23 实库迁移；必须保留静态检查证据；
- 未提供真实支持 elicitation 的 MCP 客户端时，使用 SDK exchange mock/协议集成测试，不冒充真实客户端 UI；
- 不调用真实大模型；本变更验证 Tool 与业务执行链，不验证模型自主决策质量；
- 不验证 Flowable、消息或 R3，因为这些能力必须被拒绝且不在范围内。

## 7. Review 缺口矩阵（2026-07-12）

现有测试通过不能覆盖或推翻以下失败项；Fix 后必须先补 Red、再给 Green：

| Review 项 | 当前缺口 | Fix 验收 |
|---|---|---|
| R1 字段交集 | 只断言 `descriptor.allowedFields`，未证明 grant 缩小后原版本 Schema 中的额外字段会被拒绝 | grant 仅允许 `status` 时，版本 Schema 中的 `remark` 必须在 elicitation 前拒绝且零副作用 |
| R2 版本 binding | 未覆盖 capability 当前版本与 PINNED/FOLLOW_MAJOR resolved version 的 source binding 不同 | 目录必须返回 resolved version 自身的 sourceKey/sourceVersion/policy；重绑定不得串动作 |
| R3 递归步骤 | 只覆盖顶层禁用步骤 | `stepList`、嵌套 `steps`、`childSteps` 和未知步骤均在发布与 invoke 阶段失败关闭 |
| R4 不可用错误 | 未注入目录 Mapper、策略解析和审计存储故障 | 分别返回稳定 `CATALOG_UNAVAILABLE`、`AUTHORIZATION_UNAVAILABLE`、`AUDIT_UNAVAILABLE`，不得映射为参数错误 |
| R5 双审计 | 只验证正常审计调用 | 审计写失败不能形成“业务成功但无 Capability 审计”的静默缺口 |
| R6 请求指纹 | 未校验 elicitation 指纹与请求体的绑定关系 | capability/version/recordId/arguments 任一变化时规范化摘要必须变化，键顺序变化时摘要保持一致 |

Review 结论：Spec Compliance FAIL，状态 `reviewed_with_findings`；阶段一未通过，因此未执行 Code Quality Review。

## 8. Fix 增量验证（2026-07-12）

| Review 项 | Red/Green 证据 | 结果 |
|---|---|---|
| R1 字段交集 | Catalog 测试验证版本 `status,remark` + grant `status` 后有效 Schema 只剩 `status`；invoke 另有显式字段交集检查 | PASS |
| R2 版本 binding | Mapper XML 测试要求 `v.source_key/v.source_version` 且禁止 `c.source_key/c.source_version`，查询同时约束版本行为、风险和可见性 | PASS |
| R3 递归步骤 | 顶层 `stepList`、嵌套 START_FLOW、嵌套 CREATE_RECORD 均失败关闭；invoke 在审计和执行前拒绝 | PASS |
| R4 不可用错误 | 授权 Mapper 故障稳定返回 `AUTHORIZATION_UNAVAILABLE`；目录数据返回 `CATALOG_UNAVAILABLE`；审计故障返回 `AUDIT_UNAVAILABLE` | PASS |
| R5 双审计 | 审计预留失败时执行次数为 0；`recordOrUpdate` 覆盖首次插入、可信身份更新、身份冲突和 JDBC 未报告 changed rows | PASS |
| R6 请求指纹 | 参数 Map 键顺序变化摘要相同，recordId/请求内容变化摘要不同 | PASS |

回归结果：Secure Actions 27/27、Control Plane 27/27、Generator 专项 7/7、MCP 16/16、Identity 35/35、Admin 40/40。V1.0.23 真实数据库迁移、真实外部 MCP 客户端和真实模型调用继续按原条件跳过，未冒充 PASS。

## 9. 修复后复审增量验证（2026-07-12）

- 复跑 `mvn -Penable-tests test`：Secure Actions 27/27 PASS；
- `xmllint --noout SecureActionCatalogMapper.xml`：PASS；
- V1.0.23 Flyway `${...}` placeholder 扫描：无输出，PASS；
- 相关文件 `git diff --check`：PASS；
- 本轮仅复审与文档归档，无产品代码差异，复用 Fix 阶段其余模块和 Admin 聚合成功证据。
