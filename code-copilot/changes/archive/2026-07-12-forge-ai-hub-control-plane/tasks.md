# 任务拆分 — Forge AI 中枢控制面

> 变更名：`forge-ai-hub-control-plane`
> 执行方式：用户已预授权 Proposal 后自动 Apply；TDD；禁止 commit/push

## 前置条件

- [x] 阶段 0 已归档且 Capability/MCP/AI/Admin 验证通过；
- [x] 读取根 AGENTS、长期记忆、自动化测试标准和相关 Skills；
- [x] 检查阶段 1 路线图、当前 Capability/MCP 实现和 SDK 动态目录限制；
- [x] 确认下一个 Flyway 版本为 `V1.0.21`；
- [x] 不覆盖工作区已有供应商、模型路由、组织权限和 vendor 源码改动。

## Task 1：Flyway 控制面结构

- [x] 新建五张表，全部包含标准审计字段和 `del_flag tinyint`；
- [x] 为能力编码、工具名、版本、客户端编码和 grant 增加仅约束有效记录的生成列唯一键；
- [x] 添加 capability/client/grant 状态与可见性字典，内置数据 `tenant_id=1`；
- [x] 添加控制面 API 权限资源，全部 `NOT EXISTS`；
- [x] 静态检查 `${...}`、tenant 0、无列名 INSERT 和物理 DELETE。

## Task 2：实体、Mapper 与 XML

- [x] 新建五个 `TenantEntity` 实体并显式 `@TableLogic`；
- [x] 新建 Mapper，分页、详情、唯一性和授权决策查询全部写 XML；
- [x] XML 显式列清单、`tenant_id` 和 `del_flag=0`；
- [x] 调用日志 Mapper 只接受安全元数据模型。

## Task 3：能力目录与不可变版本

- [x] Red：重复 code/toolName、无效 Schema、相同版本修改和跨租户详情必须失败；
- [x] 实现发布事务：校验 Schema、计算 checksum、插入不可变 version、更新 current pointer；
- [x] 只允许 `READ_ONLY`、`LOW/MEDIUM` 进入本阶段 grant；
- [x] 实现分页、详情和停用。

## Task 4：机器客户端凭据

- [x] Red：缺 Pepper、Secret 落库、错误 Secret、过期/吊销客户端、非正数 user/org 必须失败；
- [x] 使用 `SecureRandom` 生成 256 bit Secret，HMAC-SHA256 + 常量时间比较；
- [x] 创建/轮换只返回一次原始 Secret，持久化只保存 prefix/hash/version；
- [x] 实现 revoke 与安全分页，不返回 keyHash。

## Task 5：显式授权交集

- [x] Red：跨租户、组织不一致、客户端/能力/grant 禁用或过期、版本不匹配全部拒绝；
- [x] 实现 PINNED/FOLLOW_MAJOR 版本解析；
- [x] ACTION/FLOW/MESSAGE/EXTERNAL 能力拒绝授权；
- [x] 授权分页、创建和撤销使用逻辑删除。

## Task 6：调用审计

- [x] Red：安全事件写入后不包含 arguments/data/Header/Token/Secret；
- [x] 实现幂等 requestId 写入和日志分页；
- [x] 失败日志只存稳定 errorCode/schemaPath，不存 Throwable message；
- [x] 保留后续留存任务接口，不在本变更物理删除日志。

## Task 7：管理 API 与安全边界

- [x] Controller 使用 `RespInfo`、`pageNum/pageSize`、POST-safe 写接口；
- [x] 加入 `@SaCheckPermission`、`@OperationLog`、`@ApiDecrypt/@ApiEncrypt`；
- [x] DTO 不包含 tenantId、keyHash、delFlag 等受控字段；
- [x] MCP 自动配置不注入持久化能力，现有 ping 测试保持全绿。

## Task 8：验证与文档

- [x] Capability/MCP 测试真实执行且无 skip；
- [x] 控制面领域测试覆盖 P0 安全分支；
- [x] AI 84 tests 回归；
- [x] Admin 38 个预期聚合模块 package（以实际 reactor 数为准）；
- [x] Flyway、XML、SSE/Nacos/高风险工具/敏感日志/空白扫描；
- [x] 回填四份文档，执行两阶段 Review；
- [x] Review 通过前未归档；当前保持 `review-ready` 等待用户明确归档。

## Review 结果

- [x] Spec Compliance Review 通过，无前端、低代码执行器、动态业务 Tool、Nacos 或旧 SSE 范围漂移；
- [x] Code Quality Review 完成并修复 6 类安全/一致性问题；
- [x] 最终修复后 Capability 13、Control Plane 20、MCP 13、AI 84、Admin 身份适配 2 项、Admin 38/38 全部通过；
- [x] 状态保持 `review-ready`，等待用户后续明确归档。

## 2026-07-12 增量 Fix

- [x] Secret 改为全局不可猜测 keyId 格式，认证前不再要求调用方提供 tenantId；
- [x] authenticate/rotate/revoke 使用目标字段原子更新和 `credential_version` CAS，旧快照不能恢复已吊销凭据；
- [x] 版本 checksum 与不可变校验覆盖 source、behavior、riskLevel、visibility 全部执行语义；
- [x] 审计增加 actor/service 双身份字段并限制稳定码格式，拒绝原始异常、Token、Secret 内容；
- [x] Admin 组合根接入 Bearer 机器凭据解析器，只信任数据库绑定的 tenant/serviceUser/activeOrg；
- [x] 认证前凭据定位和 `last_used_at` CAS 更新均显式绕过自动租户追加，同时保留 SQL 内权威 tenant 条件；
- [x] 完成 Control Plane 20、MCP 13、AI 84、Admin 身份适配 2 项与 38/38 聚合回归；
- [x] 明确具体人员委托令牌为下一独立变更，本轮禁止信任调用方自报 userId Header。

## Archive

- [x] 用户于 2026-07-12 明确要求归档上一个阶段需求并开始阶段 2；
- [x] 核对四份文档、长期决策、踩坑记录、最终验证证据和跳过项；
- [x] 状态更新为 `done`，归档到 `code-copilot/changes/archive/2026-07-12-forge-ai-hub-control-plane/`；
- [x] 下一变更固定为 `mcp-user-delegation-identity`，不在归档动作中追加业务代码。
