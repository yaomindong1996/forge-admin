# 任务拆分 — 租户工作区 UX

## Task 1: 超级管理员按当前工作区隔离
- `TenantInterceptor`：超管设置 `tenantId`，不再 `setIgnore(true)`
- `SysUserServiceImpl` / `SysOrgServiceImpl` / `SysRoleServiceImpl` / `SysPostServiceImpl`：查询和写入租户固定为 Session 当前租户

## Task 2: 登录页与顶栏
- `forge-admin-ui/src/views/login/index.vue`：启用租户数 ≤ 1 不展示选择器
- `TenantSwitcher.vue`：可切换数 ≤ 1 不渲染

## Task 3: 系统管理页去掉租户选择
- `user.vue` / `role.vue` / `org.vue` / `post.vue`：去掉搜索、表格列、新增表单中的所属租户；树按当前租户加载
- 保留用户高级关系「租户关系」和超管「加入租户」

## Task 4: 测试
- 拦截器/查询租户归一化单测
- 前端源码契约测试

## Task 5: 登录按账号工作区消歧
- `UserLoadServiceImpl#authenticateByUsernamePassword`
- 管理端 / H5 登录页默认不拉全集租户，4091 后再展示工作区选择
