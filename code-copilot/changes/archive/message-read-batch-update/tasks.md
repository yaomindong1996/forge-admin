# 任务拆分 — 消息已读批量更新优化
> status: completed
> created: 2026-08-05

## 前置条件

- [x] 已确认前端“全部标记为已读”只调用一次 `/api/message/read/all`。
- [x] 已确认性能问题在 `MessageServiceImpl`：先查未读列表，再循环 `updateById`。
- [x] 已保留用户原有 `.DS_Store` 工作树变更。

## Task 1：建立失败回归

- [x] 增加全部已读只调用一次 Mapper 方法的单测。
- [x] 增加指定 ID 批量已读只调用一次 Mapper 方法的单测。
- [x] 增加空 ID 列表不访问 Mapper 的单测。
- [x] 运行定向测试并记录 Red 证据。

## Task 2：实现单 SQL 批量更新

- [x] `SysMessageReceiverMapper` 新增 `markMessagesReadBatch` 和 `markAllMessagesRead`。
- [x] Mapper XML 新增两条 `UPDATE`，显式限定 `tenant_id/user_id/read_flag`。
- [x] `MessageServiceImpl` 删除查询实体和 `forEach(updateById)`，改为单次 Mapper 调用。

## Task 3：验证与交付

- [x] 执行定向 JUnit、Mapper XML 解析、Message 插件聚合编译和 `git diff --check`。
- [x] 回填 Spec、Test Spec、Execution Log 和可复用踩坑。
- [x] 精确暂存本变更文件并提交，不 push。
