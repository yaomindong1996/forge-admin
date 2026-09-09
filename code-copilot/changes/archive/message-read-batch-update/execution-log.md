# 执行日志 — 消息已读批量更新优化
> status: completed
> created: 2026-08-05

## 1. 基线

- 当前分支：`main`。
- 基线提交：`348eb011 [business-domain-delete-orphan-entry-fix] 修复业务域孤立入口删除阻断`。
- 用户既有工作树变更：`M .DS_Store`、`D forge/.DS_Store`，本轮保持不动。

## 2. 根因

| 范围 | 现状 | 结论 |
|------|------|------|
| 顶部消息通知 | `MessageNotification#handleMarkAllRead` 只调用一次 `markAllMessagesRead` | 前端不是逐条请求 |
| 消息列表 | `handleMarkAllRead` 只调用一次 `/api/message/read/all` | 前端不是逐条请求 |
| 后端全部已读 | 先 `selectList`，再 `receivers.forEach(receiverMapper::updateById)` | 真实 N+1 根因 |
| 后端批量已读 | 先按 ID 查询，再逐条 `updateById` | 同源性能问题，一并收敛 |

## 3. 已执行记录

| 时间 | 范围 | 动作 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-08-05 | 基线 | `git status --short` | passed | 仅用户既有 `.DS_Store` 变更 |
| 2026-08-05 | 根因定位 | 检查前端全部已读入口、Message API、Controller、Service 和 Mapper XML | passed | 确认请求已批量，但后端落库仍逐条 |
| 2026-08-05 10:41 | Red 回归 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-message -Dtest=MessageServiceImplTest test` | expected failure | 测试编译失败：两个批量 Mapper 方法尚不存在 |
| 2026-08-05 10:43 | Green 回归 | 同上 | passed | 5 tests，0 failures，0 errors，0 skipped |
| 2026-08-05 10:43 | XML 解析 | `xmllint --noout .../SysMessageReceiverMapper.xml` | passed | Mapper XML 语法正常 |
| 2026-08-05 10:43 | 聚合编译 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-message -am compile -DskipTests` | passed | 27/27 reactor modules `SUCCESS`；仅有既有弃用、unchecked 和 Lombok Builder 警告 |
| 2026-08-05 10:44 | 差异检查 | `git diff --check` | passed | 无空白错误 |

## 4. 服务与数据库

- 本轮启动服务：无。
- 数据库变更：无。
- 遗留 PID：无。

## 5. 结论与跳过项

- “全部已读”和“选中批量已读”均已从 1 次查询 + N 次更新收敛为 1 次 SQL 更新。
- SQL 显式限定 `tenant_id`、`user_id`、`read_flag = 0`；指定 ID 场景使用参数化 `IN`，空集合由 Service 短路且 Mapper 兜底为 `AND 1 = 0`。
- 未启动 Admin、MySQL 或 Redis，未执行运行态接口和真实落库验证；本轮无前端改动。
