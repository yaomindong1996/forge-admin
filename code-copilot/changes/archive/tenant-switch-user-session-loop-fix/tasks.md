# 任务拆分 — 租户切换用户会话循环修复
> status: complete
> created: 2026-08-04

## 前置条件

- [x] 已读取根 `AGENTS.md`、`code-copilot/AGENTS.md`、项目记忆和适用 Skill。
- [x] 已读取自动化测试标准并确认本轮验证矩阵。
- [x] 已确认仅保留用户工作树中的 `.DS_Store` 变更，不修改数据库。

## Task 1：建立回归测试

- [x] 在 Auth Starter 增加 `AuthControllerTest`，验证用户信息按 Session `userId` 和当前租户刷新。
- [x] 在前端增加权限初始化恢复纯函数测试，验证清理登录态并单次跳转 `/login`。

## Task 2：修复后端用户信息刷新

- [x] 将 `AuthController#getUserInfo` 改为调用 `loadUserByUserId`。
- [x] 保留登录时间、登录 IP、客户端并回写 Token Session。
- [x] 不修改租户校验和权限重建逻辑。

## Task 3：修复前端无限重入

- [x] 提取可测试的账号初始化失败恢复逻辑。
- [x] 用户身份仍为空时清理登录态、设置路由守卫完成并跳转登录页。
- [x] 在身份失败分支立即返回，禁止继续 `replace` 原路由；身份已成功时保留菜单降级逻辑。

## Task 4：增量验证与交付

- [x] 执行前后端定向单测和相关静态检查。
- [x] 执行 Auth 相关后端编译与前端生产构建。
- [x] 回填 `test-spec.md`、`execution-log.md` 和 Spec 状态。
- [x] 精确暂存本任务文件并提交，不推送远端。
