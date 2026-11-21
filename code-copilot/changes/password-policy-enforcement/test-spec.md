# 本轮增量测试计划：密码策略后端生效

## P0

- `PasswordPolicySupportTest`（新增）
  - 复杂度：空密码、过短、超过 72 位、缺大写/小写/数字/特殊字符分别给出对应提示；满足策略时通过；关闭某项要求后不再校验该项。
  - 配置缺失或 `passwordPolicy` 为空时按默认策略校验。
  - 历史：与当前密码相同被拒；命中最近 N 个历史哈希被拒；`historyCount` 为 0 时不查历史；新密码通过。
  - 凭证写入：更新成功后写入被替换的旧哈希并裁剪超出 N 的记录；`historyCount` 为 0 时不写历史；用户不存在时返回 false 且不写历史。
  - 过期：关闭时不查库、不打标记；开启且超期时同时设置 `passwordExpired` 与 `forcePasswordChange`；未超期不打标记；边界（刚好满 N 天）判定为过期。
- `SysUserCredentialMapperXmlContractTest`（新增）
  - 更新语句必须递增 `password_version`、写 `password_changed_time`，`force_password_change` 使用字面量。
  - 历史相关语句必须同时带 `tenant_id` 与 `user_id` 条件。
  - XML 中不出现 MySQL 专有写法（反引号、`NOW()`、`IFNULL`、`LIMIT x, y`）。
- `SysUserCredentialMapperH2Test`（新增）
  - 用真实 Mapper XML 经 MyBatis 在 H2 的 MySQL 与 PostgreSQL 兼容模式下各跑一遍：
    凭证读取与已删除用户过滤、凭证更新（版本递增、时间、强制改密字面量）、历史写入/倒序读取/窗口裁剪、
    租户与用户隔离、基于已落库历史的复用校验。
- `PasswordLoginExpirationTest`（新增）
  - 用户名密码登录策略在过期开启且超期时给会话打标记；关闭时不查库；
    短信、三方登录策略源码中不得调用过期判定。
- `LoginUserSessionCompatTest`（新增）
  - `passwordExpired` 经会话 JSON 序列化后可还原；旧会话 JSON 没有该字段时为 null；
    会话 JSON 中出现当前类不认识的字段时仍可读出（新旧版本混跑）。
- `SystemAuthServiceImplChangePasswordTest`（扩展）
  - 弱密码被拒且不写库；与历史重复被拒且不写库；成功时经 `changeCredential` 写入并清除会话的两个标记。
- `SystemAuthServiceImplPasswordRecoveryTest`（扩展）
  - 弱密码在消耗验证码之前被拒；验证码通过后走历史比对与 `changeCredential`；注册弱密码被拒。
- `SysUserServiceImplPasswordPolicyTest`（新增）
  - 新增用户弱密码被拒且不落库；管理员重置弱密码被拒；管理员重置成功走 `changeCredential` 且不做历史比对。
- `AuthControllerTest`（扩展）
  - 会话带 `passwordExpired` 时，`/auth/userInfo` 重建后的对象保留该标记并置 `forcePasswordChange`；未带标记时不改变重建结果。
- 既有回归：`UserLoadServiceImplPasswordLoginTest`、`SysUserMapperSqlContractTest`、`SysUserImportSecurityContractTest`、
  `SystemAuthServiceImplSsoClientAccessTest`、`SystemAuthServiceImplClientCredentialTest`。
- 双栈 SQL：在本地 MySQL 与 PostgreSQL 的临时库上，用当前全量 SQL 中的表结构执行 `SysUserCredentialMapper.xml` 的全部语句。
- 前端：ESLint 检查改动文件。
- 仓库级：`pnpm quality:check`、`git diff --check`。

## P1

- 真实服务联调：启动 Admin 后在配置页修改策略，分别验证新增用户、管理员重置、自助改密、找回密码、注册、过期登录。

## 跳过项

- P1 真实服务联调未执行：按仓库约定，启动真实服务并对真实库做端到端验证需要用户授权，本轮未启动 Admin 与前端开发服务。
  因此以下内容没有在运行中的系统上验证过：Spring 容器装配、真实登录后的强制改密跳转、配置页开关的保存与回显。
- 前端未新增自动化用例：改动是一个开关、一个禁用绑定和四处文案，仅做了 ESLint 对比；未跑前端构建。
- `pnpm quality:check` 与受影响模块完整测试在 HEAD 上本来就不通过，本轮只确认「没有因本次改动新增问题」，详见 execution-log。
