# Tenant / DataScope Pitfalls Index

Source: `code-copilot/memory/pitfalls/security.md` (and related backend notes). Open matching titles only.

- 多租户拦截器会破坏 LIMIT 1 FOR UPDATE 顺序
- 登录前、Token 事件和定时任务访问租户表必须显式建立上下文
- 租户拦截 SQL 不要使用 MySQL NULL 安全等号
- 异步日志补全用户信息必须忽略租户条件
- 租户业务数据源切换后数据权限不能访问业务库里的平台表
- 超级管理员区划树被登录用户 regionCode 误裁剪
- 超级管理员不能全局忽略租户隔离
- 公开 OAuth/OpenAPI 入口跳过登录租户后必须自行建立可信租户上下文（capability pitfalls）
- AI/MCP 机器调用的数据权限必须按执行器 fail-closed（capability pitfalls）
