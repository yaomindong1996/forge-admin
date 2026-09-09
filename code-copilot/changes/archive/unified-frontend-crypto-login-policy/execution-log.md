# 执行日志 — 三端传输加密统一与登录密码策略解耦
> status: complete
> created: 2026-08-04

## 1. 基线

- 当前分支：`main`；根规范仅禁止 `master` 分支，本轮不创建提交、不推送。
- 工作区已有能力开放平台及其它未提交差异，本轮不清理、不重置、不覆盖无关文件。
- 已读取根 `AGENTS.md`、`code-copilot/AGENTS.md`、项目记忆、编码和安全规则，以及 `writing-plans`、`forge-coding-standards` Skill。
- 用户要求自行测试，本轮不启动服务、不执行 Maven/pnpm/Flyway/浏览器测试。

## 2. 研究结论

- Admin UI 已实现 `/crypto/config` 启动加载；H5、报表端仍硬编码 `enabled=true`。
- H5 登录每次无条件获取公钥并 RSA 加密。
- 报表登录无条件 RSA 加密，失败时静默返回明文，并写死 `encrypted=true`。
- `UsernamePasswordAuthStrategy` 读取 `CryptoProperties.enabled`；`UsernamePasswordCaptchaAuthStrategy` 无条件使用另一套 RSA 解密方法。
- `/auth/loginConfig` 已是匿名登录策略接口，适合下发独立密码加密开关。

## 3. 执行记录

| 时间 | 范围 | 结果 | 备注 |
|------|------|------|------|
| 2026-08-04 | 规则、Skill、工作区与现有实现检查 | passed | 未修改用户既有能力平台文件 |
| 2026-08-04 | SDD 文档 | passed | 进入实现阶段 |
| 2026-08-04 | 后端密码策略 | passed（静态审查） | 新增独立开关、统一策略与解码器；两种密码认证方式已收敛 |
| 2026-08-04 | Admin/H5/报表前端 | passed（静态审查） | 三端统一读取后端配置；登录密码按独立配置；移除公共客户端固定 Secret |
| 2026-08-04 | 差异卫生 | passed | `git diff --check`、冲突标记和新增文件尾随空白扫描均无输出 |

## 4. 未执行项

- Maven/pnpm 构建与测试。
- Admin、App、Report 服务启动。
- MySQL/Redis/Flyway 操作。
- 三端真实浏览器登录与网络报文验证。
