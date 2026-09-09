# 任务拆分 — 客户端凭据与公共客户端协议加固
> 拆分顺序：数据协议 → Codec → Service/认证 → UI/验证
> 每个任务完成后同步回填 Spec、Test Spec 和执行日志

## 前置条件

- [x] 已读取根目录规范、记忆文件和自动化测试标准。
- [x] 已确认浏览器环境文件存在非空 `VITE_APP_SECRET`，不输出其值。
- [x] 已确认用户授权按剩余整改方案继续实施。
- [x] 不连接真实数据库，不生成或打印生产客户端 Secret。

## 执行状态

- [x] Task 1：建立客户端认证方式和迁移合同。
- [x] Task 2：实现摘要 Codec、Mapper XML 和 Service 状态机。
- [x] Task 3：接入登录认证与历史机会式升级。
- [x] Task 4：清理浏览器 Secret、更新管理 UI 并完成验证。
- [x] Task 5：修复阶段二复审的 2 Critical + 2 Important 并完成目标验证。
- [ ] Task 6：在部署环境完成 Flyway、权限授权、并发轮换及多租户 E2E 门禁。

## Task 1：建立客户端认证方式和迁移合同

- **目标**：数据库和 DTO 明确 public/confidential 语义，内置客户端不再保存 Secret。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.53__harden_client_credentials.sql`
  - `forge-server/db/全量初始化SQL.sql`
  - `forge-server/forge-admin-server/sql/初始化脚本.sql`
  - `.../entity/SysClient.java`
  - `.../dto/SysClientDTO.java`、`.../vo/SysClientVO.java`
- **关键字段**：`clientAuthMethod`、`hasAppSecret`。
- **验收**：脚本防重复；内置行 `app_secret` 为空；受管 SQL 不含已知弱值。

## Task 2：实现摘要 Codec、Mapper XML 和 Service 状态机

- **目标**：新 Secret 只写 `{bcrypt}` 摘要，查询与条件升级落在 Mapper XML。
- **涉及文件**：
  - `.../security/ClientSecretCodec.java`
  - `.../mapper/SysClientMapper.java`
  - `.../resources/mapper/SysClientMapper.xml`
  - `.../service/IClientService.java`
  - `.../service/impl/ClientServiceImpl.java`
- **关键签名**：
  ```java
  SysClient createClient(SysClientDTO dto);
  boolean updateClient(SysClientDTO dto);
  boolean validateAppSecret(SysClient client, String rawSecret);
  int countLegacyPlaintextSecrets();
  ```
- **验收**：新增机密客户端必须提供 Secret；空更新保留；public 清空；条件升级不覆盖并发轮换。

## Task 3：接入登录认证与 Controller

- **目标**：登录按认证方式校验，Controller 不直接保存实体或构建 Wrapper。
- **涉及文件**：
  - `.../service/impl/SystemAuthServiceImpl.java`
  - `.../controller/SysClientController.java`
  - `.../service/IClientService.java`
  - `.../service/impl/ClientServiceImpl.java`
- **验收**：public 校验 AppId 但不要求 Secret；confidential 缺失/错误 Secret 均拒绝；响应不返回摘要片段。

## Task 4：清理前端并完成增量验证

- **目标**：浏览器构建不再含客户端 Secret，管理 UI 支持认证方式和安全轮换。
- **涉及文件**：
  - `forge-admin-ui/.env.development`、`.env.production`、`.env.example`
  - `forge-admin-ui/src/views/login/index.vue`
  - `forge-admin-ui/src/views/login/callback.vue`
  - `forge-admin-ui/src/views/system/client.vue`
  - 本变更四份 SDD 文档
- **验收**：目标单测、System 模块 package、前端 build、YAML/SQL/敏感扫描、`git diff --check` 通过。

## Task 5：复审修复与收尾

- 登录和密码类请求不进入通用操作日志，动态配置不能取消该硬边界。
- 客户端认证配置不从通用缓存读取，轮换后不存在旧摘要或旧 public 状态回填。
- `.env*` 不声明或暗示浏览器客户端 Secret；Mapper、管理响应和日志边界具备合同测试。
- 空 Secret 元数据更新不得写 `app_secret`；轮换、清空和认证方式切换必须使用旧值 CAS。
- Bearer Token 不得进入应用日志、操作日志或 WebSocket 广播载荷；Mapper XML 保留 IP 白名单 JSON TypeHandler。
- `/auth/online/**` 强制登录并逐接口授权，管理操作先校验租户内会话或用户；删除匿名 `/test`。
- 页面、列表、客户端在线列表和 `userTokens` 兼容路径不返回 Token，分页/列表 SQL 不读取 Token 列。
- 客户端元数据、认证方式和 Secret 使用同一条双旧值 CAS；CAS 失败抛出业务异常。
- 7 个目标测试类 36 条通过；目标 Vue ESLint、Mapper XML、Flyway 和敏感模式扫描通过。

## Task 6：部署门禁

- 在测试/预发数据库执行 `V1.0.53`、`V1.0.56` 并核对资源权限和存量客户端盘点。
- 使用真实事务验证元数据更新、Secret 轮换/清空及并发 CAS 失败回滚。
- 使用两个租户账号验证在线列表、单/批量下线、封禁/解封无法跨租户操作。
- 验证 Redis/Sa-Token 会话下线、WebSocket 通知及旧前端调用兼容行为。
