# 全表检索查询收敛
> status: apply
> created: 2026-09-02
> complexity: 🟡中等

## 1. 背景与目标

用户列表分页每次用非相关 `GROUP_CONCAT` 子查询扫完整关联表。流程候选任务先把 Flowable 候选全量 `.list()` 再本地分页。超时检查把全部活动任务装进内存。

做完后：用户翻页只按当前行查组织/岗位/租户名；候选任务走 Mapper 分页；超时扫描按批 `listPage`，不再一次 `.list()`。

## 2. 代码现状

- `SysUserMapper.xml` `selectUserPage` / `selectExportList` 三个 derived table 对 `sys_user_tenant` / `sys_user_org` / `sys_user_post` 全表 `GROUP BY`。
- `FlowTaskServiceImpl.candidateTasks` 调用 `taskService.createTaskQuery().list()`。
- `FlowTimeoutServiceImpl.checkAndHandleTimeoutTasks` / `getUpcomingTimeoutTasks` 调用 `.active().list()`。定时器目前注释掉。

## 3. 功能点

- [x] 用户列表/导出的组织、岗位、租户名改为相关子查询（`uo.user_id = u.id`）。
- [x] 候选任务改为 `sys_flow_task` Mapper 分页，语义保持：未签收 + 候选人/候选组。
- [x] 超时扫描按批 `listPage`，单批 100。

## 4. 非目标

- 不改区划树全量加载（已有 24h 缓存）。
- 不改数据权限冷启动全表缓存。
- 不把候选人逗号字段改成关系表。
- 不启动真实 Admin/Flow 服务。
