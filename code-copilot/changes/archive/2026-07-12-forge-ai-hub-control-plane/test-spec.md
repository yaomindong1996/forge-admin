# 单测 Spec — Forge AI 中枢控制面

> status: done
> created: 2026-07-11

## 1. P0 覆盖

### 数据与租户

- 五张表字段、逻辑删除和唯一键一致；
- Mapper XML 显式 `tenant_id`、`del_flag=0`；
- 跨租户 ID 查询和关联授权返回不可见/拒绝；
- 删除使用逻辑删除，唯一业务编码删除后可重建。

### 能力版本

- 非 Draft 2020-12、未支持关键字、错误类型组合拒绝发布；
- 相同版本与不同 checksum 拒绝覆盖；
- currentVersion 只能指向同租户同能力的有效快照；
- 停用能力不能授权或决策通过。

### 客户端密钥

- 缺 Pepper 创建和轮换失败；
- 原始 Secret 仅在响应对象存在，实体/VO/日志无该字段；
- 正确 Secret 通过，错误 Secret、错误 clientCode、过期、禁用、吊销全部拒绝；
- HMAC 比较与 dummy HMAC 路径均执行；
- credentialVersion 轮换递增，旧 Secret 立即失效。

### 授权

- grant、client、capability 必须同租户；
- activeOrgId 必须与客户端绑定一致；
- PINNED 精确版本、FOLLOW_MAJOR 同主版本；
- grant/client 过期或禁用、能力停用、风险/行为超范围全部拒绝。

### 审计

- requestId 幂等；
- 成功/失败安全元数据完整；
- 不存在 arguments/data/header/token/secret/apiKey 字段或日志拼接。

### MCP 回归

- 仅 Streamable HTTP `/mcp`；
- SSE/STATELESS/stdio 启动失败；
- `tools/list` 仍只含 `capability.ping`；
- 持久化目录不被静态全量发布。

## 2. P1/P2

- Flyway placeholder、tenant 0、物理 DELETE、无保护 seed 扫描；
- Capability 插件完整测试与 Admin 聚合 package；
- AI 插件 84 tests；
- 不启动真实数据库时明确记录 Flyway 实跑跳过原因；
- 本变更无前端，不运行 pnpm build，复用阶段 0 前端成功基线。

## 3. 执行顺序

- [x] Red：领域服务测试先失败；
- [x] Green：Flyway/Mapper/Service 最小实现；
- [x] Capability + MCP 测试；
- [x] AI 回归；
- [x] Admin package；
- [x] 静态安全扫描与文档回填。

## 4. 基线

| 范围 | 基线 |
|---|---|
| Capability | 13 tests，0 failure/error/skip |
| MCP | 13 tests，0 failure/error/skip |
| AI | 84 tests，0 failure/error/skip |
| Admin | 37/37 modules package SUCCESS |
| 前端 | 阶段 0 目标 ESLint 与生产 build PASS；本变更不修改前端 |

## 5. 最终结果

| 范围 | 结果 |
|---|---|
| Capability | 13 tests，0 failure/error/skip |
| Control Plane | 20 tests，0 failure/error/skip |
| MCP | 13 tests，0 failure/error/skip；Streamable HTTP 协议 `2025-06-18` 实际握手通过 |
| AI | 84 tests，0 failure/error/skip |
| Admin | 38/38 reactor modules `package -DskipTests` SUCCESS |
| 静态检查 | 5 个 Mapper XML、Flyway、逻辑删除、租户、旧 transport、敏感字段和空白检查通过 |

真实 MySQL/Flyway 执行因本轮没有受控测试数据库而跳过；前端未修改，因此不重复运行 `pnpm build`。测试过程未启动常驻服务。

## 6. 2026-07-12 Review 后增量 Fix

- [x] 凭据通过全局不可猜测 `keyId` 定位客户端，认证前不接受 tenantId；
- [x] authenticate 只原子更新 `last_used_at`，并发 rotate/revoke 后影响行数为 0 时失败关闭；
- [x] rotate/revoke 使用 `credential_version` CAS，禁止旧实体覆盖 `status/key_hash`；
- [x] 同版本修改 `sourceType/sourceKey/behavior/riskLevel/visibility` 任一执行语义字段必须拒绝；
- [x] 审计使用稳定状态/错误码并拒绝原始异常、Token、Secret 内容；
- [x] 审计同时保留机器客户端、绑定服务账号和实际 actor，支持后续用户委托；
- [x] Admin Bearer 解析只使用控制面权威身份，忽略伪造的 `X-User-Id`；
- [x] 修复后重跑 Control Plane、MCP、AI 与 Admin 聚合，并补 Flyway/XML 静态检查。

## 7. 增量 Fix 最终结果

| 范围 | 结果 |
|---|---|
| Capability + Control Plane | 13 + 20 tests，0 failure/error/skip |
| MCP | 13 tests，0 failure/error/skip；仍为 Streamable HTTP 协议 `2025-06-18` |
| AI | 84 tests，0 failure/error/skip |
| Admin MCP 身份适配 | 2 tests，0 failure/error/skip |
| Admin 聚合 | 38/38 reactor modules `package -DskipTests` SUCCESS |

## 8. 归档验收

- 归档复用 2026-07-12 最终成功基线，不重复执行无差异全量测试；
- XML、Flyway placeholder、tenant 0、物理 DELETE、尾随空白和敏感日志静态检查通过；
- 真实 MySQL/Flyway 实跑仍因没有受控测试数据库而跳过，未将跳过项误记为通过；
- 本轮未启动服务、数据库、Redis、Nacos，也未调用真实模型。
