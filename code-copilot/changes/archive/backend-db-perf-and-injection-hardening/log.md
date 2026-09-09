# 变更日志 - 后端数据库性能与注入加固

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-08-05 | scan | 全量扫描后端 2508 Java + 304 XML，发现 88 处问题 | 4 维度并行扫描：N+1、Wrapper 滥用、事务/注入、查询性能 |
| 2026-08-05 | propose | 生成 spec.md + tasks.md，拆分 18 个 Task | 按 P0/P1/P2/P3 优先级分层 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|-----------|------|
| SQL 注入修复 | 参数化 `#{}` + 白名单正则 | 仅白名单校验 | 双重防护，`inSql` 改自定义 Mapper 方法 |
| 事务内远程调用 | `@TransactionalEventListener(AFTER_COMMIT)` | 异步 `@Async` | 保证事务提交后才发送，数据已落库 |
| N+1 修复 | `IN` 批量查询 + 内存分组组装 | `JOIN` 聚合 | 保持分页顺序，避免 JOIN 改变结果集 |
| 循环批量 | `saveBatch` / `updateBatchById` | 自定义 XML 批量 | 复用 MyBatis-Plus 内置，减少改动量 |
| 待办 LIKE | 优先 `FIND_IN_SET`，其次关联表 | 全文索引 | `FIND_IN_SET` 无需表结构变更 |
| 统计查询 | 优先 `LEFT JOIN GROUP BY` | 冗余字段 + 触发器 | 无结构变更，效果不足再加字段 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|---------|--------|
| `inSql` 的第二参数原样拼接 SQL | MyBatis-Plus `inSql` 设计为拼接，非参数化 | 改用自定义 Mapper 方法 + `#{}` 参数化 | 是 |
| 事务内调用 `messageClient.send()` 占 DB 连接 | `@Transactional` 包裹整个 `send` 方法 | 事务只保留落库，发送移到 `AFTER_COMMIT` | 是 |
| `candidate_users` 逗号字符串 + LIKE 无法走索引 | 历史设计用逗号分隔存储多值 | 改 `FIND_IN_SET` 或拆关联表 | 是 |
| `DataScopeInterceptor` 按 mapperMethod 匹配 | LambdaQueryWrapper 生成的 SQL 不在 XML 中 | 查询迁回 XML 才能被数据权限改写 | 是 |

## 知识发现

- [ ] **inSql 拼接风险**：MyBatis-Plus `inSql(field, sql)` 的 sql 参数是原样拼接，不接受参数化，禁止拼入用户输入
- [ ] **事务边界原则**：`@Transactional` 方法内禁止包含远程调用（HTTP/RPC）、文件 IO、Thread.sleep
- [ ] **N+1 识别模式**：`stream().map(x -> mapper.selectById(x.getId()))` 是典型 N+1，改 `selectBatchIds` + 内存组装
- [ ] **DataScope 依赖 XML**：`DataScopeInterceptor` 按 `mapperMethod`（Mapper 接口方法名）精确匹配 XML SQL 改写，Service 层 Wrapper 查询无法被改写
- [ ] **逗号字符串 LIKE**：`LIKE CONCAT(#{v}, ',%')` 三模式匹配是反模式，MySQL 无法走索引

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|----------|---------|---------|

## 代码质量备忘

- P0 Task 1-4 涉及安全/状态流转，必须人工 Review 后才能合并
- P1 Task 5-12 涉及分页列表接口，需验证查询结果顺序一致性
- P3 Task 17 迁移量大（160+ 处），建议按模块分 PR 推进
