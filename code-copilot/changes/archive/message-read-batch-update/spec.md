# 消息已读批量更新优化
> status: completed
> created: 2026-08-05
> complexity: 低

## 1. 背景与问题

消息通知的“全部标记为已读”前端已经只调用一次 `POST /api/message/read/all`，但后端 `MessageServiceImpl#markAllRead` 会先查询当前用户的所有未读接收记录，再逐条调用 `updateById`。“批量标记已读”接口也存在同样的逐条更新。未读消息较多时会产生 1 次查询 + N 次更新。

## 2. 目标

- “全部标记为已读”改为单条 `UPDATE` 更新当前租户、当前用户的全部未读记录。
- 既有“批量标记已读”同步改为单条带 `message_id IN (...)` 的 `UPDATE`。
- 保持现有前端交互、API 路径、请求体和返回协议不变。

## 3. 功能范围

- `SysMessageReceiverMapper` 新增按消息 ID 集合批量已读和当前用户全部已读方法。
- Mapper XML 显式限定 `tenant_id`、`user_id`、`read_flag = 0`，统一设置 `read_flag = 1` 和同一个 `read_time`。
- Service 只调用一次 Mapper 更新，不再查询实体或循环 `updateById`。
- 空批量 ID 保持直接返回，不生成空 `IN` SQL。

## 4. 边界与安全

1. 用户 ID 继续只来自 `SessionHelper.getUserId()`，客户端不能指定他人。
2. 租户 ID 由服务端可信上下文解析，SQL 必须显式限定租户和用户。
3. 只更新 `read_flag = 0` 的记录，已读记录的原 `read_time` 不被覆盖。
4. 不修改消息内容、投递状态、接收人归属或数据库结构。

## 5. 数据与接口影响

- 数据库结构：无变更，无 Flyway。
- `POST /api/message/read/batch`：协议不变，底层由逐条更新改为单 SQL 批量更新。
- `POST /api/message/read/all`：协议不变，底层由查询 + N 次更新改为单 SQL 更新。
- 前端已经是单次调用，本轮不修改 Vue/API 代码。

## 6. 验收标准

- [x] 全部已读只调用一次 Mapper 批量更新。
- [x] 指定 ID 批量已读只调用一次 Mapper 批量更新。
- [x] 空 ID 列表不调用 Mapper。
- [x] SQL 显式包含租户、用户、未读条件以及受控 `IN` 列表。
- [x] 消息插件定向测试、聚合编译、XML 解析和差异检查通过。

## 7. 确认记录（HARD-GATE）

- 确认时间：2026-08-05
- 确认人：用户
- 确认内容：用户要求将消息通知“全部标记为已读”从逐条更新优化为批量操作。

## 8. 实施结论

- `markAllRead` 已由“查询未读记录 + N 次 `updateById`”改为一次 `markAllMessagesRead`。
- `markReadBatch` 已由“按 ID 查询 + N 次 `updateById`”改为一次 `markMessagesReadBatch`。
- API 与前端调用保持不变；未启动服务、未修改数据库。
