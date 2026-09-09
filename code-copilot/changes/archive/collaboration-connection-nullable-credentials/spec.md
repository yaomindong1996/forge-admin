# 企业协同新增连接允许 client_id/client_secret 为空
> status: apply
> created: 2026-08-31
> complexity: 🟢简单

## 1. 背景与目标

企业协同升级后，`sys_social_config` 只表示一个外部企业连接根，应用凭据改由连接下的物理应用（`sys_social_app_config`）保存。管理端「新建连接」只填平台、企业 ID、身份策略等连接字段，不再填写 `client_id`/`client_secret`。

当前表结构仍把这两列定义为 `NOT NULL`。MySQL 严格模式下插入会失败，表现为企业协同新增连接报错。

完成后应达到：新增连接不填 `client_id`/`client_secret` 也能落库；存量已填写的值保持不变；应用管理仍要求填写应用凭据。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- 管理端新建连接走 `POST /system/collaboration/connections`，请求体是 `CollaborationConnectionSaveRequest`。出处：`forge-plugin-collaboration/.../controller/CollaborationConnectionController.java`，`create`。
- `CollaborationConnectionSaveRequest#toEntity()` 明确不透传凭据字段。出处：同模块 `dto/CollaborationConnectionSaveRequest.java`。
- 前端连接表单 `editSchema` 没有 `clientId`/`clientSecret`；配置指南也要求先建连接，再在「应用管理」填应用 ID/Secret。出处：`forge-admin-ui/src/views/system/collaboration/connections.vue`。

### 2.2 现有实现

- `sys_social_config.client_id`、`client_secret` 在初始 DDL 中为 `varchar(255) NOT NULL`。出处：`forge-starter-social/sql/sys_social_config.sql`。
- V1.0.57 把该表升级为连接根时只增可空字段和新表，没有放开这两列。出处：`forge-server/db/migration/V1.0.57__add_collaboration_connection_foundation.sql`。
- 应用表 `sys_social_app_config.client_id` 本来就可空，Secret 走密文列。出处：同上脚本第 131 行附近。

### 2.3 发现与风险

- MyBatis-Plus 默认不插入 null 字段；列又没有默认值，严格模式下会报 `Field 'client_id' doesn't have a default value`。
- 登录/Token 仍按「应用优先、连接回退」解析凭据，连接列变空不影响已配置应用的连接。出处：`SocialAuthRequestFactory#buildAuthConfig`。

## 3. 功能点

- [x] `sys_social_config.client_id`、`client_secret` 改为可空，存量数据不改写。
- [x] 模板 SQL 与实体注释同步为「连接根可空、凭据在应用表」。
- [x] 用单测锁定新增连接 DTO 不回写旧凭据字段。

## 4. 业务规则

1. 新建/修改连接不要求、不接收 `client_id`/`client_secret`。
2. 应用凭据仍在应用管理中配置；应用表 `clientId` 对登录应用仍必填（前端应用表单保持必填）。
3. 旧连接上已有的 `client_id`/`client_secret` 继续作为兼容回退，不做清空迁移。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改列可空 | `sys_social_config` | `client_id`、`client_secret` | `varchar(255) DEFAULT NULL`；仅当当前为 `NOT NULL` 时执行 |

## 6. 接口变更

无协议变更。现有 `POST /system/collaboration/connections` 仍不接收凭据字段。

## 7. 影响范围

- 企业协同连接新增/修改落库
- 旧三方登录配置表兼容字段
- 登录解析对连接列的回退路径（空值时必须命中应用凭据）

## 8. 风险与关注点

- 不涉及资金、权限放开或状态机。
- 已执行 Flyway 脚本不可改，必须新增 `V1.0.138`。

## 8.5 测试策略

- **测试范围**：DTO 不透传凭据；Flyway 脚本防重复与占位符扫描；协作插件单测。
- **覆盖率目标**：本轮新增 DTO 契约测试。
- **独立 Test Spec**：是

## 9. 待澄清

无。用户已确认连接级 `client_id`/`client_secret` 应可空。

## 10. 技术决策

连接根保留旧列做兼容回退，不删除列，只放开 NOT NULL。凭据权威来源仍是 `sys_social_app_config`。

## 11. 执行日志

见 `execution-log.md`。

## 12. 审查结论

DTO 单测通过。真实库需重启 `forge-admin-server` 执行 `V1.0.138` 后验收新增连接。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-08-31
- **确认人**：用户直接给出原因与方案后进入 apply
