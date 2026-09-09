# 测试计划 - 在线用户租户上下文修复
> status: completed

## P0

- 在线记录插入期间租户上下文等于记录租户，执行后恢复原上下文。
- Token 事件在线程无租户上下文时，从会话记录恢复租户并只执行一次更新。
- 找不到 Token 所属租户时失败关闭，不执行无租户 SQL。
- 主动登出调用 UPDATE 并写入离线状态，不调用 DELETE。
- 定时清理执行期间 `ignoreTenant=true`，完成后恢复原标记。

## 回归

- 当前租户下的在线会话查询、强制下线和封禁权限行为不变。
- `sys_auth_online_user` 仍未进入忽略表配置。

## 计划命令

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am -DskipTests compile`
- `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system -Dtest=SysOnlineUserServiceSecurityTest,OnlineUserSecurityContractTest test`
- `mvn -Penable-tests -pl forge-admin-server -Dtest=OnlineUserCleanTaskTest test`

## 结果

- 插件定向测试：13 项通过，0 失败、0 错误、0 跳过。
- 定时清理任务测试：1 项通过，0 失败、0 错误、0 跳过。
- 依赖聚合编译：26 个 Reactor 模块全部成功。
- 未启动真实服务、未连接数据库；本轮无 SQL 或配置变更。
