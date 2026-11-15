# 密码策略后端生效 Spec

> 变更名：`password-policy-enforcement`
> 状态：`review`
> 创建日期：2026-11-15

## 1. 需求

配置中心「安全配置 → 密码策略」可以配置最小长度、大小写/数字/特殊字符、过期天数、历史记录数，
但后端没有任何代码读取 `SecurityConfig.passwordPolicy`，配置保存后不生效。

本次让策略真正生效，已与用户确认的口径：

1. **复杂度**（长度、大小写、数字、特殊字符）：立即生效，覆盖所有「人设定密码」的入口。
2. **密码历史**（不得与当前密码及最近 N 个旧密码相同）：立即生效，覆盖用户自助改密与找回密码。
3. **密码过期**：默认关闭，管理员在配置页主动开启后才生效。
4. 作为独立小改动重做（2026-09-28 的旧实现混在安全整改大合并里被整体回滚，回滚原因未留记录），并补足测试。

## 2. 现状

- 表结构已具备，不需要新增 Flyway：`sys_user.password_version`、`sys_user.password_changed_time`（`V1.0.188`）、
  `sys_user_password_history`（`V1.0.190`），MySQL / PostgreSQL 全量 SQL 中均已存在。
- 写密码的入口共 5 处，全部只做 `PasswordUtil.encrypt`，没有任何校验：
  新增用户、管理员重置、用户自助改密、找回密码、注册。
- 「强制改密」链路已存在：`LoginUser.forcePasswordChange` 为 true 时
  `ApiPermissionInterceptor.enforcePasswordChange` 只放行 `/auth/userInfo`、`/auth/changePassword`、`/auth/logout`、`/crypto/*`，
  管理端前端会跳到个人中心弹出改密框。过期复用这条链路。

## 3. 方案

### F1 策略组件 `PasswordPolicySupport`

位置 `forge-plugin-system/.../plugin/system/auth/`，`@Component`（不是 Service，避免 Service 之间互相注入）。
依赖 `ConfigManagerService` 与新的 `SysUserCredentialMapper`。

| 方法 | 作用 |
|------|------|
| `validateComplexity(rawPassword)` | 按配置校验长度与字符类别；长度上限固定 72（BCrypt 只使用前 72 字节） |
| `validateNotReused(userId, rawPassword)` | 不得与当前密码相同；不得与最近 N 个历史密码相同 |
| `changeCredential(userId, encodedPassword, forcePasswordChange)` | 同一事务内更新 `sys_user` 凭证列并把被替换的旧哈希写入历史、裁剪到 N 条 |
| `applyExpiration(loginUser)` | 过期开启且已超期时，给本次登录会话打上 `passwordExpired` 与 `forcePasswordChange` |

配置取值规则：

- `minLength` 为空或小于 1 时按 8；`historyCount` 为空或小于等于 0 视为关闭历史校验，上限 20（与配置页上限一致）。
- 读不到配置时使用 `PasswordPolicyConfig` 默认值（与现有 `ConfigManagerService` 兜底一致）。

### F2 新 Mapper `SysUserCredentialMapper`（+ XML）

凭证相关 SQL 集中在一个 Mapper，全部写在 XML，MySQL / PostgreSQL 通用：

- `selectCredential(userId)`：取 `tenant_id`、`password`、`password_changed_time`
- `updateCredential(...)`：`password`、`password_version = COALESCE(password_version, 0) + 1`、`password_changed_time`、
  `force_password_change`（字面量 0/1）、`update_time`
- `selectRecentPasswordHashes` / `insertPasswordHistory` / `selectHistoryIds` / `deleteHistoryByIds`

裁剪历史采用「查 id 列表后按 id 删除」，不使用带 `LIMIT` 的子查询删除，避免方言差异。
调用统一包在 `TenantContextHolder.executeIgnore` 内并显式带 `tenant_id`（凭证归属租户取 `sys_user.tenant_id`）。

### F3 接入点

| 入口 | 复杂度 | 历史 | 写入方式 |
|------|--------|------|----------|
| `SysUserServiceImpl.insertUser`（含 Excel 导入） | 是 | 否（新用户无历史） | 不变 |
| `SysUserServiceImpl.resetPassword`（管理员重置） | 是 | **否** | `changeCredential`（记录历史） |
| `SystemAuthServiceImpl.changePassword`（自助改密） | 是 | 是 | `changeCredential` |
| `SystemAuthServiceImpl.resetPassword`（找回密码） | 是（解密后、验证码校验前） | 是（验证码校验后） | `changeCredential` |
| `SystemAuthServiceImpl.register` | 是 | 否 | 不变 |

说明：

- 管理员重置不做「与当前/历史密码相同」比对：否则有重置权限的人可以用报错信息验证某个用户当前或曾经的密码。
  旧密码仍会进入历史，用户之后不能改回去。
- 找回密码先校验复杂度再消耗验证码，避免弱密码白白用掉验证码；历史比对放在验证码通过之后，不给匿名调用方提供比对结果。
- 三方登录自动建号（`SocialAuthStrategyImpl`）与协同通讯录同步建号（`ForgeDirectoryWriterImpl`）使用系统随机密码，不经过策略校验，保持不变。
- 管理员重置后 `force_password_change = 0`、新增用户为 1，沿用现有行为。
- Excel 导入的逐行错误信息受 `SysUserImportSecurityContractTest` 约束必须保持固定文案，
  因此弱密码行仍显示「用户数据写入失败」，只在处理建议里补充「密码是否符合密码策略」。

### F4 密码过期（默认关闭）

- `SecurityConfig.PasswordPolicyConfig` 新增 `expireEnabled`（默认 `false`）。已保存的旧配置 JSON 没有该字段，反序列化后为关闭。
- 仅在**用户名密码登录**（`UsernamePasswordAuthStrategy`、`UsernamePasswordCaptchaAuthStrategy`）认证成功后判定：
  `password_changed_time + expireDays <= now` 即过期。短信、三方、SSO 登录不判定（这些用户可能根本不知道本地密码，强制改密会把人锁死）。
- 过期后在 `LoginUser` 上设置 `passwordExpired = true`、`forcePasswordChange = true`，后续由现有拦截器限制只能改密。
- `/auth/userInfo` 每次都会从数据库重建 `LoginUser`，会把会话态的过期标记冲掉，
  因此 `AuthController.getUserInfo` 在重建后把 `passwordExpired` 从旧会话带到新对象。
  被标记的会话只能访问放行名单内的接口，其它重建会话的入口（切换租户/组织等）本身已被拦截，不需要改。
- 改密成功后清除两个标记。
- 拦截器对过期会话返回「当前账号密码已过期，必须先修改密码」，初始密码场景的提示语不变；
  前端登录页、个人中心的强制改密文案改为同时适用于初始密码与过期两种情况。
- 管理员在会话被标记后再关闭过期开关，已标记的会话仍需改密或重新登录，不做实时撤销。

### F5 配置页

`config-center.vue` 密码策略区新增「启用密码过期」开关，「密码过期天数」仅在开启时可编辑，并在开关旁提示影响范围。

## 4. 影响范围

- 后端：`forge-starter-config`（配置类）、`forge-starter-core`（`LoginUser` 新增字段）、`forge-starter-auth`（控制器、拦截器文案）、
  `forge-plugin-system`（组件、Mapper、5 个接入点、2 个登录策略）。
- 前端：`forge-admin-ui` 配置中心、个人中心与登录页的强制改密文案。
- 数据库：无新增迁移脚本。
- 行为变化（升级后立刻可见）：
  - 新设定的密码必须满足当前配置（种子配置为 8 位 + 大小写 + 数字）。之前能设 `123456` 的入口现在会被拒绝，Excel 导入中的弱密码行会失败。
  - 已有用户的现有密码不受影响，只在下次设定密码时校验。
  - 改密/找回密码不能使用当前密码和最近 5 个旧密码（历史从本次上线后开始累积）。
  - 过期不会自动生效。

## 5. 不在本次范围

- 改密后按 `password_version` 吊销其它会话（旧实现 `c67602db1` 的内容）。
- MFA、企业 IdP 等（归企业版）。
- H5 端强制改密引导页：H5 目前没有改密引导，开启过期后 H5 密码登录的过期用户会被拦截且无页面可改密，配置页已提示，需在 PC 端改密。
- 前端按策略做实时提示（个人中心新密码仍是前端 `min: 6` 规则，不满足策略时由后端返回具体原因）。
- 用户列表展示「密码已过期」、批量强制改密等管理功能。

## 6. 风险与回滚

- 种子管理员 `password_changed_time` 为 2026-07-13，早于 90 天。因此过期必须默认关闭；管理员开启时自己下次密码登录就会被要求改密，属于预期。
- 回滚：还原本次代码即可，`sys_user_password_history` 中已写入的数据可保留，不影响旧代码运行。
- 会话兼容：`LoginUser` 新增字段随会话写入 Redis。Admin、Flow、App 等服务共享会话，
  已用测试确认会话 JSON 反序列化会忽略未知字段，新旧版本混跑时旧服务不会因为多出的字段读不出会话。
- 企业版仓库 ForgePro 有一份改了包名的同名类（登录策略、`SystemAuthServiceImpl` 及其测试），
  本次只改社区仓，同步到企业版时需按相同方式调整构造参数。
