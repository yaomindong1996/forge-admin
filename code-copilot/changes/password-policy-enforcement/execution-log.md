# 密码策略后端生效 Execution Log

> 变更名：`password-policy-enforcement`
> 执行时间：2026-11-15
> 环境：macOS，JDK 17.0.13（Homebrew），Maven 3.9.3 离线模式，Node v20.19.0；本地 Docker 中的 MySQL 8.0.46、PostgreSQL 16.15

## 0. 改动前基线

```bash
cd forge-server && mvn -o -B -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests \
  -Dtest='SystemAuthServiceImplChangePasswordTest,SystemAuthServiceImplPasswordRecoveryTest,UserLoadServiceImplPasswordLoginTest,SysUserMapperSqlContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
# Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

## 1. 改动明细

### 后端

| 文件 | 改动 |
|------|------|
| `forge-starter-config/.../config/SecurityConfig.java` | `PasswordPolicyConfig` 新增 `expireEnabled`，默认 `false` |
| `forge-starter-core/.../session/LoginUser.java` | 新增会话态字段 `passwordExpired` |
| `forge-starter-auth/.../controller/AuthController.java` | `/auth/userInfo` 重建 `LoginUser` 后延续旧会话的 `passwordExpired`，并置 `forcePasswordChange` |
| `forge-starter-auth/.../interceptor/ApiPermissionInterceptor.java` | 过期会话被拦截时提示「当前账号密码已过期，必须先修改密码」，初始密码提示不变 |
| `forge-plugin-system/.../auth/PasswordPolicySupport.java` | 新增。复杂度校验、历史比对、凭证写入（含历史记录与裁剪）、过期判定 |
| `forge-plugin-system/.../auth/UserPasswordCredential.java` | 新增。凭证快照（用户、归属租户、密码哈希、变更时间） |
| `forge-plugin-system/.../mapper/SysUserCredentialMapper.java` + `resources/mapper/SysUserCredentialMapper.xml` | 新增。6 条 SQL，MySQL / PostgreSQL 通用 |
| `forge-plugin-system/.../service/impl/SysUserServiceImpl.java` | `insertUser` 校验复杂度；`resetPassword` 校验复杂度并改走 `changeCredential` |
| `forge-plugin-system/.../service/impl/SystemAuthServiceImpl.java` | `changePassword`、`resetPassword`、`register` 接入策略；删除 `updateUserPassword`；改密成功清除 `passwordExpired` |
| `forge-plugin-system/.../strategy/AbstractAuthStrategy.java` | 注入策略组件，提供 `applyPasswordExpiration` |
| `forge-plugin-system/.../strategy/UsernamePassword{,Captcha}AuthStrategy.java` | 认证成功后调用 `applyPasswordExpiration` |
| `forge-plugin-system/.../controller/SysUserController.java` | 导入失败行的处理建议补充「密码是否符合密码策略」（错误文案本身受契约测试约束未改） |

### 前端

| 文件 | 改动 |
|------|------|
| `forge-admin-ui/src/views/system/config-center.vue` | 新增「启用密码过期」开关与说明；「密码过期天数」在未启用时禁用；默认值补 `expireEnabled: false` |
| `forge-admin-ui/src/views/system/profile.vue` | 强制改密弹窗标题、提示、消息改为同时适用于初始密码与过期 |
| `forge-admin-ui/src/views/login/composables/useLoginPage.part1.js` | 登录后强制改密提示改为「当前账号必须先修改密码」 |

### 测试

| 文件 | 说明 |
|------|------|
| `PasswordPolicySupportTest`（新增，22 例） | 复杂度、历史、凭证写入、过期及边界 |
| `SysUserCredentialMapperXmlContractTest`（新增，5 例） | XML 语句齐全、更新时递增版本并写变更时间、读取跳过已删除用户、历史语句带租户与用户条件、SQL 可移植 |
| `SysUserCredentialMapperH2Test`（新增，6 个场景 × MySQL/PostgreSQL 模式 = 12 例） | 真实 XML 经 MyBatis 执行 |
| `SysUserServiceImplPasswordPolicyTest`（新增，5 例） | 新增用户、管理员重置 |
| `PasswordLoginExpirationTest`（新增，3 例） | 密码登录策略的过期判定；短信/三方策略不得调用 |
| `LoginUserSessionCompatTest`（新增，3 例） | 新字段随会话 JSON 还原；旧会话可读；未知字段可读 |
| `SystemAuthServiceImplChangePasswordTest`（2 → 6 例） | 弱密码、复用、成功写入并清标记、写入失败保留标记 |
| `SystemAuthServiceImplPasswordRecoveryTest`（5 → 10 例） | 弱密码不消耗验证码、验证码未通过不比对历史、复用、成功、注册弱密码 |
| `AuthControllerTest`（2 → 3 例） | `/auth/userInfo` 延续过期标记 |
| `SystemAuthServiceImplClientCredentialTest` / `SsoClientAccessTest` | 仅适配构造参数 |

无新增 Flyway 脚本。

## 2. 验证记录

### 2.1 后端目标单测

```bash
cd forge-server && mvn -o -B -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests \
  -Dtest='PasswordPolicySupportTest,SysUserCredentialMapperXmlContractTest,SysUserCredentialMapperH2Test,SystemAuthServiceImpl*Test,SysUserServiceImplPasswordPolicyTest,PasswordLoginExpirationTest,AuthControllerTest,LoginUserSessionCompatTest,SaSessionForJacksonCustomizedCompatTest,UserLoadServiceImplPasswordLoginTest,SysUserMapperSqlContractTest,SysUserImportSecurityContractTest,SysUserAccessPolicyTest,LoginPasswordDecoderTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
# forge-starter-auth: Tests run: 7, Failures: 0, Errors: 0
# forge-plugin-system: Tests run: 87, Failures: 0, Errors: 0 — BUILD SUCCESS
```

注意必须带 `-am`：本次改了 `forge-starter-core` / `forge-starter-config`，不带 `-am` 时 Maven 会用 `~/.m2` 里的旧构件，测试编译直接报找不到 `setPasswordExpired`（实际遇到过一次）。

### 2.2 受影响模块完整测试（与干净 HEAD 对比）

```bash
mvn -o -B -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dmaven.test.failure.ignore=true
```

| | 用例数 | 失败 | 错误 |
|---|---|---|---|
| 干净 HEAD（`8390d9ac1`，独立 worktree） | 888 | 14 | 5 |
| 本次工作区 | 945 | 14 | 5 |

- 多出的 57 例即本次新增/扩展的用例，全部通过。
- 失败的测试方法两边逐条比对完全一致，共涉及 10 个类，均为 HEAD 既有问题，与本次改动无关：
  `PluginRegistryTest`、`PluginAutoConfigurationTest`、`SocialAppCredentialServiceTest`、
  `SysCacheDiagnosticsRemovalContractTest`、`AiImageGeneratingDictMigrationTest`、`SysPluginTaskMapperTest`、
  `OnlineUserSecurityContractTest`、`SysPluginContextTest`、`SysPluginServiceTest`、`SysUserRelationCoordinatorTest`。
- 其中 `forge-starter-plugin` 模块的 10 个失败是测试里写死了核心版本 1.2.0（当前 1.2.1）；其余几个类的失败原因本轮没有逐个排查。
- 不加 `-Dmaven.test.failure.ignore=true` 时构建会在 `forge-starter-plugin` 就中断，跑不到后面的模块。

### 2.3 下游编译

```bash
mvn -o -B -pl forge-admin-server,forge-app-server -am compile -DskipTests
# BUILD SUCCESS
```

### 2.4 双栈 SQL（真实数据库）

在本地容器（MySQL 8.0.46、PostgreSQL 16.15）各建一个临时库 `forge_pwd_policy_tmp`，
用仓库全量 SQL 里 `sys_user`、`sys_user_password_history` 的建表语句建表，
把 `SysUserCredentialMapper.xml` 的 6 条语句换成字面量参数逐条执行。
两个库都执行成功，查询结果一致。执行后临时库已删除。

- 逐条的输出没有留存，这里只记录结论。
- 这一步验证的是 SQL 方言；参数是手工换成字面量的，MyBatis 的参数绑定与结果映射由 2.1 的 H2 双模式测试覆盖。

### 2.5 前端

对 `config-center.vue`、`profile.vue`、`useLoginPage.part1.js` 跑 ESLint（Node v20.19.0，`pnpm --ignore-workspace`）。
三个文件原本就有 ESLint 问题；用 HEAD 版本跑同样的检查，问题数与改动后完全相同
（`profile.vue` 25、`config-center.vue` 8 条警告、`useLoginPage.part1.js` 98），本次没有新增。

### 2.6 仓库门禁

```bash
pnpm quality:check     # 退出码 1
git diff --check       # 通过
```

`quality:check` 结果：新增 0 条，2 条 worsened，3 条 stale。

- 2 条 worsened 和 3 条 stale 都不在本次改动的文件里。
- 在干净 HEAD 的 worktree 上跑同样的命令，输出逐行相同，说明门禁在当前 `main` 上本来就不通过。
- 本次没有修改 `.quality-baseline.json`，也没有执行 `quality:prune`。
- 本机原先没装 `lizard`：缺它时复杂度检查没有任何输出，门禁会把基线里 800 条复杂度记录全部报成 stale 并提示去 prune，
  这是假象，照做会清掉基线。本轮在临时 venv 里装了 `lizard` 后重跑才得到上面的有效结果（venv 已删除）。

## 3. 跳过项

- **真实服务联调未执行**。没有启动 Admin 服务与前端，以下内容只有单元/集成层面的证据，没有在运行中的系统上观察过：
  - Spring 容器能否正常装配新组件与新 Mapper。已确认编译通过；四个服务（admin / app / flow / report）的
    `mybatis-plus.mapperPackage` 都是 `com.mdframe.forge.**.mapper`，新 Mapper 与同包既有 Mapper 走同一条扫描路径；
  - 登录页、个人中心在密码过期时的实际跳转与提示；
  - 配置页开关的保存与回显。
- 前端没有跑生产构建，没有新增前端自动化用例。
- 没有覆盖 `UsernamePasswordCaptchaAuthStrategy` 的运行时路径（依赖验证码装配较多），只用源码契约断言它调用了过期判定；
  运行时行为由共用的 `applyPasswordExpiration` 与 `UsernamePasswordAuthStrategy` 的用例间接覆盖。

## 4. 备注

- 升级后立刻生效的行为：新设定的密码必须满足策略；改密/找回密码不得复用当前及最近 N 个密码。已有密码不受影响。
- 密码历史从上线后开始累积，上线前用过的旧密码不在历史表里。
- 过期按 `sys_user.password_changed_time` 判定。该列由 `V1.0.190` 回填为
  `COALESCE(password_changed_time, update_time, create_time, NOW())`，对老用户是「用户记录最后更新时间」而不是真实改密时间。
  开启过期前建议先看一眼会有多少账号立刻过期：
  `SELECT COUNT(*) FROM sys_user WHERE del_flag = 0 AND password_changed_time < NOW() - INTERVAL 90 DAY;`（MySQL 写法）。
- 插件模块改动需 `mvn install` 后再从 admin-server 启动，避免跑旧代码。
