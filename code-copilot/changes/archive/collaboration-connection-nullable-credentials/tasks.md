# 任务拆分 — 企业协同新增连接允许 client_id/client_secret 为空

## 前置条件

- [x] 已确认新增连接不填写连接级应用凭据
- [x] 当前最高 Flyway 版本为 `V1.0.137`

## Task 1: 放开连接表凭据列 NOT NULL

- **目标**: 存量库和新库模板都允许 `client_id`/`client_secret` 为空
- **涉及文件**:
    - `forge-server/db/migration/V1.0.138__allow_nullable_social_config_credentials.sql` — 新增，按 `IS_NULLABLE = 'NO'` 防重复 `MODIFY COLUMN`
    - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/sql/sys_social_config.sql` — 模板 DDL 同步可空
    - `forge-server/forge-framework/forge-starter-parent/forge-starter-social/src/main/java/com/mdframe/forge/starter/social/domain/entity/SysSocialConfig.java` — 注释标明连接根可空
- **验证点**:
    - 脚本不含 Flyway `${}` 占位符
    - 已可空时重跑脚本为 `SELECT 1`

## Task 2: 锁定新增连接不回写旧凭据

- **目标**: 防止后续又把应用凭据塞回连接根
- **涉及文件**:
    - `forge-plugin-collaboration/src/test/java/com/mdframe/forge/plugin/collaboration/dto/CollaborationConnectionSaveRequestTest.java`
    - `forge-plugin-collaboration/pom.xml` — 增加 `spring-boot-starter-test`
- **关键签名**:
  ```java
  void toEntityLeavesLegacyCredentialsUnset()
  ```
