# 客户端凭据与公共客户端协议加固
> status: apply
> created: 2026-07-27
> complexity: 🔴复杂

## 1. 背景与目标

`sys_client.app_secret` 当前以明文保存并直接比较，四个内置客户端的密钥还被写入初始化 SQL；管理端登录构建通过 `VITE_APP_SECRET` 把同一值打进浏览器产物。浏览器、H5 和分发式 App 无法安全保管客户端 Secret，因此继续把它们建模为机密客户端没有安全收益。

本变更完成以下结果：

1. 客户端显式区分 `none` 公共客户端与 `client_secret` 机密客户端。
2. 内置 PC、报表、App、H5 客户端迁移为公共客户端，数据库和前端构建不再保存或发送 Secret。
3. 机密客户端仅保存带算法标记的 BCrypt 摘要，接口永不返回摘要或原文。
4. 历史自定义客户端支持明文双读；首次成功验证后以条件更新方式升级为 BCrypt，兼容期可盘点且不得吞掉格式错误。
5. 客户端查询下沉 Mapper XML，避免本变更继续扩大 Controller/Service Wrapper 技术债。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- 登录入口：`SystemAuthServiceImpl#validateAndLoadClient` 读取 `userClient/appId/appSecret`，当前通过 `SysClient#getAppSecret().equals` 比较。
- 通用校验：`ClientServiceImpl#validateAppSecret` 同样直接明文比较。
- 管理入口：`SysClientController#create/update/getAppSecret` 直接保存 DTO 中的 Secret，并对存储值做前四后四脱敏。
- 浏览器入口：`login/index.vue`、`login/callback.vue` 从 `VITE_APP_SECRET` 读取并随登录请求发送；`.env.development` 和 `.env.production` 均配置了非空值。
- 数据入口：两份初始化 SQL 的 `sys_client.app_secret` 为 `NOT NULL`，四个内置客户端均保存明文。

### 2.2 现有实现

- `forge-starter-auth` 已提供 `PasswordUtil.encrypt/matches` BCrypt 封装，System 插件已依赖该 Starter。
- `sys_client.app_secret` 为 `varchar(128)`，可容纳 `{bcrypt}` 标记和 BCrypt 摘要。
- `SysClientMapper` 当前只继承 `BaseMapper`，没有 Mapper XML。
- `SysClientVO` 不返回 `appSecret`，但 `getMaskedAppSecret` 会泄露摘要前后片段。

### 2.3 发现与风险

- `VITE_*` 会被 Vite 静态注入浏览器产物，不能作为机密凭据来源。
- 只把数据库值改成 BCrypt 而继续让浏览器发送固定 Secret，仍允许任何前端用户提取并复用该凭据。
- 直接把全部存量客户端切成公共客户端会削弱自定义服务端客户端，必须只清理明确的内置公共客户端。
- 无条件机会式升级可能覆盖管理员并发轮换的新 Secret，必须以 `id + 原存储值` 条件更新。

## 3. 功能点

- [x] 新增客户端认证方式字段和 `sys_client_auth_method` 字典。
- [x] 内置公共客户端清空 `app_secret`，初始化 SQL 不再包含客户端 Secret。
- [x] 新增 `{bcrypt}` 客户端 Secret 编解码器，使用 BCrypt 校验和常量时间遗留明文比较。
- [x] 创建/修改机密客户端时哈希新 Secret；修改时空值保留，切换公共客户端时清空。
- [x] 历史明文成功校验后条件升级摘要并清理缓存。
- [x] 登录认证按客户端认证方式决定是否要求 Secret；公共客户端仍必须匹配 `clientCode + appId`。
- [x] 前端删除 `VITE_APP_SECRET` 读取和环境值，管理页按认证方式显示轮换输入。
- [x] 客户端分页、列表、按编码和 AppId 查询进入 Mapper XML。
- [x] 登录及密码类入口从通用操作日志硬排除，认证配置读取绕过一小时通用缓存，消除 Secret 日志和旧凭据回填窗口。
- [x] 空 Secret 元数据更新不写 `app_secret`；轮换、清空和认证方式切换使用旧存储值 CAS，禁止并发恢复旧摘要。
- [x] Bearer Token 不进入认证/在线用户日志或 WebSocket 广播载荷，单会话和批量下线路径从通用操作日志硬排除。
- [x] 客户端 Mapper XML 使用显式 `resultMap` 和 `JacksonTypeHandler` 恢复 IP 白名单 JSON 映射。
- [x] `/auth/online/**` 强制登录并逐接口校验权限；移除测试广播入口，所有管理操作通过租户内会话/用户记录定位。
- [x] 在线用户分页、列表及 `userTokens` 兼容路径只返回会话 ID 和展示字段，列表 SQL 不读取 `token_value`。

## 4. 业务规则

1. `none` 只能表示无法安全持有 Secret 的公共客户端；仍必须校验启用状态、客户端编码和 AppId。
2. `client_secret` 创建时必须提供 Secret；更新时空值表示不轮换，非空表示原子替换摘要。
3. 新 Secret 至少 16 个字符且 UTF-8 编码不超过 72 字节，避免 BCrypt 输入截断；不记录日志，不进入 VO、缓存日志或错误信息。
4. 摘要格式固定为 `{bcrypt}<60-char-hash>`；未知标记或损坏摘要失败关闭。
5. 遗留无标记值只在兼容期开启双读；匹配使用 UTF-8 字节常量时间比较，成功后立即条件升级。
6. 管理端只返回 `hasAppSecret`/固定掩码，不返回摘要片段。
7. Bearer Token 可用于会话定位和下线执行，但不得写入应用日志、操作日志或广播消息载荷。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `sys_client` | `client_auth_method varchar(32)` | 默认 `client_secret`，保护未知存量自定义客户端 |
| 修改 | `sys_client` | `app_secret varchar(128) NULL` | 公共客户端允许为空 |
| 更新 | `sys_client` | 内置客户端 | `pc/forge_report/app/h5` 设置 `none` 并清空 Secret |
| 新增 | `sys_dict_type/data` | `sys_client_auth_method` | `none`、`client_secret`，`tenant_id=1` |

迁移脚本使用 `V1.0.53__harden_client_credentials.sql`，所有结构和字典写入具备 `information_schema`/`NOT EXISTS` 防重复保护。回滚代码前必须先确认没有 `{bcrypt}` 摘要且公共客户端已恢复旧协议；不提供恢复历史明文的 SQL。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 收紧 | `/auth/login` | POST | 公共客户端不再接收固定 Secret；机密客户端校验 BCrypt/兼容明文 |
| 扩展 | `/system/client` | POST/PUT | 新增 `clientAuthMethod`；Secret 采用新增必填、更新可选轮换语义 |
| 收紧 | `/system/client/secret/{id}` | GET | 仅返回固定掩码或空值，不泄露摘要片段 |
| 收紧 | `/auth/online/page`、`/list`、`/userTokens` | GET | 需要显式查询权限；不返回 Bearer Token，`userTokens` 兼容路径返回租户内会话记录 ID |
| 收紧 | `/auth/online/kickout`、`/batchKickout` | POST | 需要独立操作权限；请求参数从原始 Token 改为租户内会话记录 ID |
| 删除 | `/auth/online/test` | GET | 删除匿名 WebSocket 测试广播入口 |

## 7. 影响范围

- `forge-plugin-system`：实体、DTO/VO、Mapper XML、凭据编解码、Service、认证和测试。
- `forge-admin-ui`：登录请求、环境模板、客户端管理页。
- `forge-server/db/migration` 和两份初始化 SQL：字段、字典和内置公共客户端数据。

## 8. 风险与关注点

- **认证安全变更**：公共/机密客户端边界错误会导致登录拒绝或客户端认证降级，必须按内置编码白名单迁移。
- **兼容迁移**：历史自定义客户端没有可恢复原文副本；兼容读不能在未盘点前直接关闭。
- **缓存一致性**：客户端认证方式和 Secret 属于安全配置，不再从通用缓存读取；更新、删除和历史升级继续清理遗留 `client:config:*` 键。
- **浏览器约束**：删除 `VITE_APP_SECRET` 是协议修正，不能通过混淆或改名重新引入。

## 8.5 测试策略

- **测试范围**：Codec 格式、公共/机密创建更新、历史升级和轮换并发保护、登录分支、Mapper JSON 映射、Token 日志边界、前端 build 和敏感值扫描。
- **覆盖率目标**：Codec 和 Secret 状态分支 100%；不设置仓库总覆盖率阈值。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

- 无。采用保守规则：仅四个仓库内置分发式客户端迁移为 public；未知存量客户端保持 `client_secret` 并走兼容升级。

## 10. 技术决策

1. 使用显式 `client_auth_method`，不通过 Secret 是否为空猜测客户端类型。
2. 复用 `PasswordUtil` BCrypt，不引入新的哈希依赖。
3. 使用 `{bcrypt}` 标记区分新旧格式，避免把普通 `$2...` 明文误判为摘要。
4. 使用 Mapper XML 条件更新完成机会式升级，不在 Service 使用 Wrapper 拼查询。
5. 公共客户端删除 Secret，而不是给浏览器换一个新的固定值。
6. 管理端新建客户端默认选择 `client_secret`，仅在管理员明确选择时创建公共客户端。
7. 客户端认证配置每次从数据库读取，不使用缺少版本/CAS 合同的一小时通用缓存。
8. `/auth/login` 及密码类入口由日志切面硬排除，动态日志配置不能重新开启敏感请求体记录。
9. 元数据、认证方式和 Secret 在同一条 Mapper XML `UPDATE` 中写入，并同时校验旧认证方式与旧 Secret；空 Secret 元数据更新不写 `app_secret`，CAS 失败抛错并回滚。
10. 在线会话管理接口仅接收会话记录 ID，Service 在租户过滤后的数据库记录中解析 Token；Token 不进入响应、日志或广播载荷。
11. 动态 API 权限排除只影响数据库权限拦截器，不能绕过全局登录校验；在线管理方法另有不可动态移除的 `@SaCheckPermission`。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal | 完成 | 本变更四份 SDD 文档 | 当前用户消息确认按审计建议继续 |
| Task 1-4 | 完成 | Flyway/初始化 SQL、System Entity/DTO/VO/Mapper/Service/Auth/Controller、Admin UI/环境文件 | 未连接真实数据库 |
| Task 5 | 代码修复完成 | 在线鉴权/租户边界、会话 ID 协议、双字段原子 CAS、Token 日志清理、36 条目标测试 | 未执行真实 DB/Redis/E2E 或全量构建，保留部署门禁 |

## 12. 审查结论

- 阶段一 Spec Compliance：PASS。
- 阶段二首次复审：FAIL，发现 legacy-read 迁移覆盖关闭状态和 CAS 失败测试缺口；两项均已修复。
- 阶段二终审：FAIL，1 个 Critical、1 个 Important、2 个 Minor；发现登录 Secret 可进入通用操作日志、轮换后旧认证配置可被缓存回填，以及环境模板/合同覆盖缺口。
- 最新修复：日志切面硬排除登录和密码类入口并同步默认/初始化配置；认证配置改为数据库直读且只清理遗留缓存；删除全部浏览器 Secret 环境声明；新增日志、缓存、Mapper/响应和环境合同。
- 第三次 Code Quality Review：FAIL，2 个 Critical、2 个 Important、2 个 Minor；发现并发元数据更新可恢复旧摘要、Bearer Token 日志/操作日志暴露、Mapper JSON TypeHandler 丢失和合同缺口。
- 第三次修复：空 Secret 更新零写，轮换/清空改为旧值 CAS；移除 Token 日志和广播载荷，硬排除在线下线路径；Mapper 增加显式 `resultMap`/`JacksonTypeHandler`，环境合同覆盖 `.env.test`。
- 修复验证：System 5 个目标测试类 26/26，System 25 模块 Reactor package 通过，SysClient Mapper XML 和 Token 静态扫描通过；前端最新生产构建仍为 8727 modules。等待新的阶段二复审。
- 本轮 Critical/Important 修复：删除 `/auth/online/test`，登录排除与动态权限排除解耦，在线接口增加显式权限和租户内记录校验；列表 SQL 不读取 Token；客户端元数据、认证方式、Secret 合并为同一条双旧值 CAS。
- 本轮验证：7 个目标测试类 36/36；目标 Vue ESLint、两份 Mapper XML、Flyway 版本/placeholder、环境 Secret、Token 日志/响应和格式扫描通过。按用户约束未执行真实 DB/Redis/E2E 或全量构建。
- 最新 Code Quality 复审：FAIL，1 个 Critical；超级管理员会跳过租户 SQL 拦截，在线会话管理仍需显式传入当前 `tenantId`。
- 处置状态：用户于 2026-07-27 明确要求暂不处理该项；本阶段保持未通过，不将其记为部署门禁已完成。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-27
- **确认人**：用户
- **确认内容**：用户在收到剩余项、风险和执行顺序后明确回复“按照你的思路 继续进行”，授权按该分阶段方案继续实施。
