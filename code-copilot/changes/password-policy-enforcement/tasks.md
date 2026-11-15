# 密码策略后端生效任务

- [x] `SecurityConfig.PasswordPolicyConfig` 新增 `expireEnabled`（默认关闭）。
- [x] `LoginUser` 新增 `passwordExpired`。
- [x] 新增 `SysUserCredentialMapper` + XML（凭证读取/更新、历史读取/写入/裁剪）。
- [x] 新增 `PasswordPolicySupport`（复杂度、历史比对、凭证写入、过期判定）。
- [x] 接入 `SysUserServiceImpl.insertUser` / `resetPassword`。
- [x] 接入 `SystemAuthServiceImpl.changePassword` / `resetPassword` / `register`，移除旧的 `updateUserPassword`。
- [x] 两个用户名密码登录策略接入过期判定。
- [x] `AuthController.getUserInfo` 保留会话过期标记；拦截器对过期会话给出专门提示。
- [x] 配置页新增「启用密码过期」开关；个人中心、登录页强制改密文案调整。
- [x] 单测：策略组件、Mapper XML 契约、H2 双模式 Mapper 执行、5 个接入点、登录策略、过期标记保留、会话 JSON 兼容。
- [x] 在本地 MySQL 8.0 / PostgreSQL 16 临时库上执行新 SQL，确认双栈可用（临时库已删除）。
- [x] 后端目标单测、受影响模块完整测试（与干净 HEAD 对比）、Admin/App 服务编译、前端 ESLint、`pnpm quality:check`、`git diff --check`。
- [x] 回填 execution-log 与 CHANGELOG。
- [ ] 真实服务联调（P1，未执行，见 execution-log 跳过项）。
- [ ] 同步到企业版仓库 ForgePro（不在本次范围，见 spec 第 6 节）。
