# 单测 Spec — 企业协同新增连接允许 client_id/client_secret 为空
> status: apply
> created: 2026-08-31

## 0. 测试原则

- 增量复用 `code-copilot/rules/automated-testing-standard.md`
- 本轮核心是 schema 可空 + 连接 DTO 不透传凭据
- 真实库 Flyway 执行由用户在 `forge-admin-server` 启动时完成，本轮不自动改库

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit 5（spring-boot-starter-test） |
| Mock 框架 | 不需要 |
| 已有测试数量 | collaboration 模块此前无测试目录 |
| 已有测试风格 | starter-social 使用 AssertJ + JUnit 5 |

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）

#### 类名: CollaborationConnectionSaveRequest

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `toEntity` | 新建企业微信连接 | 平台/名称/企业 ID，不填凭据 | 无 | `clientId`、`clientSecret`、`agentId` 均为 null |

### P1 — 数据访问层

Flyway 脚本静态检查：表名、列名、`IS_NULLABLE` 防重复、无 `${}` 占位符。

### P2 — 入口层/服务层

不启动真实服务。用户重启 `forge-admin-server` 后由 Flyway 执行 `V1.0.138`。

### 不测试（明确列出原因）

- 真实 MySQL 插入：本轮不改用户数据库
- 应用保存仍要求 `clientId`：前端应用表单未改，不在本轮范围

## 3. 执行计划

- [x] Step 1: 静态扫描 Flyway 脚本
- [x] Step 2: 运行 collaboration 模块单测
- [x] Step 3: 记录命令与输出到 `execution-log.md`

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 无 | 新变更 | - | - | collaboration 模块此前无测试 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-08-31 | schema + DTO | Flyway 静态扫描 + DTO 单测 | `mvn -pl .../forge-plugin-collaboration -am test -P enable-tests -Dtest=CollaborationConnectionSaveRequestTest` | 1 test, BUILD SUCCESS | 真实库迁移跳过 |

## 6. 执行证据

见 `execution-log.md`。
