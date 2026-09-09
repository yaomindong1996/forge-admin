# 单测 Spec — 客户端凭据与公共客户端协议加固
> status: apply
> created: 2026-07-27

## 0. 测试原则

- Red/Green TDD；先验证当前明文行为，再实现摘要和公共客户端分支。
- 测试和日志不得输出原始 Secret、摘要全文或历史种子值。
- 不连接真实数据库；Mapper XML 使用 Mockito/合同测试，Flyway 仅静态验证。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit | JUnit 5 / AssertJ |
| Mock | Mockito |
| 前端 | Vite production build，Node 20.19.0 |
| 数据库 | SQL 静态合同，不执行本地真实迁移 |

## 2. 覆盖范围

### P0 — ClientSecretCodec

| 场景 | 输入 | 预期 |
|------|------|------|
| 新摘要 | 至少 16 字符且 UTF-8 不超过 72 字节 | 输出 `{bcrypt}`，不包含原文 |
| 摘要匹配 | 正确/错误 Secret | true/false |
| 损坏摘要 | 未知标记、截断 BCrypt | 失败关闭，不降级明文 |
| 遗留明文 | 正确/错误 Secret | 常量时间 true/false，并标记需要升级 |

### P0 — ClientServiceImpl

| 场景 | 预期 |
|------|------|
| 创建 public | `appSecret=null` |
| 创建 confidential 无 Secret | 拒绝且不写库 |
| 更新 confidential 空 Secret | 保留原摘要 |
| 更新 confidential 新 Secret | 写新摘要并清缓存 |
| 切换 public | 清空存储值并清缓存 |
| 并发元数据更新 | 空 Secret 不写存储列，不能恢复轮换前摘要 |
| Secret CAS 失败 | 整次更新失败并回滚，不接受静默覆盖 |
| 遗留明文登录 | 条件更新成功后清缓存；并发失败不覆盖新值 |
| 轮换缓存竞态 | 认证配置绕过通用缓存，旧缓存值和并发回填均不能参与认证 |

### P1 — 认证与数据合同

- `SystemAuthServiceImpl` public 分支不要求 Secret，但仍匹配 AppId；confidential 分支必须调用 Secret 校验。
- confidential 分支统一调用 `IClientService#validateAppSecret`。
- Mapper XML 包含分页、列表、按编码/AppId和 `id + storedSecret` 条件更新。
- Flyway 只清理四个内置 public 客户端，不把未知客户端改成 public。
- `/auth/login` 和密码类入口由操作日志切面硬排除；Mapper/管理 VO 不返回存储值。
- `SysClientMapper.xml` 使用显式 `resultMap`，`ipWhitelist` 绑定 `JacksonTypeHandler`。
- Bearer Token 不进入认证/在线用户日志或广播载荷；单会话和批量下线路径由操作日志硬排除。
- `/auth/online/**` 的动态 API 排除不能绕过登录，所有接口具有独立权限；`/test` 不存在。
- page/list 查询不读取 `token_value`，Controller/VO 和 `userTokens` 兼容路径只暴露会话 ID。
- 会话下线、封禁、解封和会话 ID 查询必须先命中租户过滤后的会话或用户记录；批量下线在副作用前完成全量校验。

### P2 — 前端与静态扫描

- `.env.development/.production` 不存在 `VITE_APP_SECRET`。
- `.env/.env.*` 不存在 `VITE_CLIENT_SECRET`、`VITE_APP_SECRET` 或引导浏览器配置 AppSecret 的说明。
- 登录页和 callback 不再读取或发送 `VITE_APP_SECRET`。
- 客户端管理页从 `sys_client_auth_method` 字典读取选项，Secret 仅在 confidential 时显示。
- 生产 build 通过。

## 3. 执行计划

- [x] 运行 System 现有目标测试基线。
- [x] 新增 Codec/Service Red 测试并确认失败。
- [x] 实现后运行目标 Green 测试。
- [x] 运行 System Reactor package。
- [x] 运行前端 build、SQL/XML 和敏感静态扫描。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-26 | Captcha System 插件 | 4 条目标测试 | 通过 | 复用验证码变更基线 |
| 2026-07-26 | Crypto Admin/Report | 44 模块 package、59 条隔离测试 | 通过 | 当前工作区存在未提交 crypto 改动 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-27 | Client credential | 目标测试、System package、UI build、静态扫描 | 见执行日志 | 通过 | 真实数据库迁移与登录 E2E 跳过 |
| 2026-07-27 | Review fix | legacy-read 迁移幂等、CAS 失败分支 | System 模块 4 个目标测试类 | 20 条通过 | Reactor 测试被无关 Datascope 测试依赖缺口阻断，改用模块定向执行 |
| 2026-07-27 | Final review fix | 日志硬排除、认证配置缓存旁路、环境/Mapper/响应合同 | System 模块 5 个目标测试类、UI build | 24 条通过；8727 modules 构建成功 | 真实 Redis/MySQL 和登录 E2E 仍为部署门禁 |
| 2026-07-27 | Third review fix | Secret CAS、Token 日志边界、Mapper JSON resultMap、`.env.test` 合同 | System 模块 5 个目标测试类、System Reactor、XML/静态扫描 | 26 条通过；25 模块构建成功；扫描无命中 | 首次模块测试受旧 Core 快照阻断，刷新 Reactor 后通过；真实 Redis/MySQL/E2E 未执行 |
| 2026-07-27 | Critical/Important fix | 在线鉴权/租户边界、会话 ID 响应、单 SQL 双字段 CAS、Token 日志 | Core/Excel/System 三模块 Reactor 内 7 个目标测试类；目标 ESLint/XML/静态扫描 | 36 条通过；0 失败/错误/跳过；静态检查通过 | 未执行真实 Redis/MySQL/E2E、全量构建或前端 build |

## 6. 执行证据

- `execution-log.md`：回填命令、测试数量、警告和服务状态。
- 关键接口：本地不启动真实服务；认证分支由单测覆盖。
- 关键数据库检查：仅验证迁移合同，真实迁移由部署环境执行。
- 服务启动与停止：默认不启动服务。
