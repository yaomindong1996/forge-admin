# 单测 Spec — 租户工作区 UX
> status: apply
> created: 2026-08-31

## P0

| 场景 | 预期 |
|------|------|
| 超管登录态进入拦截器 | 设置当前 tenantId，不 setIgnore |
| 超管查询用户/组织且请求未带 tenantId | 后端补当前 Session 租户 |
| 登录页租户选项 ≤ 1 | 源码以 `length > 1` 才展示 |
| 顶栏 TenantSwitcher | `switchableTenantCount > 1` 才渲染 |
| 用户/角色/组织/岗位搜索 schema | 不再出现「所属租户」筛选 |

## 不测试

真实多租户库切换与浏览器点选，本轮不启动服务。
