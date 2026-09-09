# 单测 Spec — 逻辑删除唯一约束与 SQL 可移植性治理
> status: applied-with-db-retry-pending
> created: 2026-07-22

## 0. 测试原则

- 先建立迁移静态契约并确认 Red，再新增 V1.0.51 使其 Green；真实库暴露索引漂移后继续补充失败契约并完成第二轮 Red/Green。
- 不连接或修改用户数据库；真实库只执行用户环境中的只读结构验收和 Flyway 升级。
- 复用 `code-copilot/rules/automated-testing-standard.md`，按共享 Flyway 变更升级验证范围。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit 5（`spring-boot-starter-test`） |
| Mock 框架 | 不需要 |
| 测试风格 | 读取 Flyway SQL 的迁移契约测试 |

## 2. 覆盖范围

### P0 — 迁移完整性

| 场景 | 预期结果 |
|------|----------|
| 迁移版本 | `V1.0.51__replace_logic_delete_generated_columns.sql` 存在 |
| 表覆盖 | 55 张生成列表均出现一次列迁移保护 |
| 索引覆盖 | 64 个原活跃唯一索引全部转换且保留名称和业务键顺序 |
| 逻辑语义 | 未删除标记为 0，已删除标记回填当前行主键 |
| SQL 可移植性 | 迁移后删除可见 `logic_delete_active` 列，不创建普通替代字段 |
| 幂等保护 | 每张表使用 `information_schema.COLUMNS` 判断后再执行 |
| 索引漂移 | 每张表使用 `information_schema.STATISTICS` 动态删除实际存在的旧索引；索引缺失或已改名时不报错 |
| 编码规则历史索引 | 删除实际存在的 `uk_ai_code_rule_code_active` 或 `uk_ai_code_rule_code`，最终只建立墓碑索引 |

### P1 — 规范回归

- `AGENTS.md` 不再推荐可见辅助生成列。
- CRUD 主 `SKILL.md` 必须把逻辑删除字段和活跃唯一索引列入触发范围及不可违反规则，并强制建表前读取 SQL 参考和检查清单。
- `alwaysApply` 的 `project-context.md` 必须包含逻辑删除唯一键决策规则。
- CRUD SQL 参考明确并非所有逻辑删除表都需要删除标记唯一索引。
- CRUD SQL 参考和检查清单必须禁止使用有效行 `deleted_at = NULL` 的唯一索引表达活跃唯一性。
- 新增业务 DDL 不得使用 `logic_delete_active GENERATED ALWAYS`。
- 数值主键实体必须使用 `Long delFlag` 与 `@TableLogic(value="0", delval="主键列")`。
- 字符串主键例外必须有专用原子删除 SQL，禁止退回 `del_flag=1`。

### P2 — 构建

- `forge-admin-server` 目标测试通过。
- `forge-admin-server -am compile -DskipTests` 通过。

### 不测试

- 不自动启动 Admin/Flow 服务，不执行真实 Flyway；当前开发库账号从本机访问返回 MySQL 1045，且共享库结构变更需用户维护窗口执行。
- 不修改或重写旧 SQL 数据导出文件；旧文件需要重新导出或删除该列后使用。

## 3. 执行计划

- [x] Step 1: 新增迁移与 MyBatis-Plus 契约测试；独立 Red 执行证据未保留，已在执行日志记录该过程警告。
- [x] Step 2: 新增 V1.0.51 并确认 Green。
- [x] Step 3: 执行静态检查、目标测试和聚合模块安装/编译。
- [x] Step 4: 记录真实库验收查询和跳过原因。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-22 | 开发库结构扫描 | `information_schema` 只读查询 | 阻塞 | MySQL 1045，未修改数据库 |
| 2026-07-22 | 后端聚合安装/编译 | `mvn -pl forge-admin-server -am install -DskipTests` | 通过 | 43 个模块全部 SUCCESS |
| 2026-07-22 | 迁移与 MP 契约测试 | `mvn -Penable-tests -pl forge-admin-server -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test` | 通过 | 4 个测试，0 失败、0 错误、0 跳过 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-22 | Flyway + 实体 + Mapper + 生成器 + 规范 | 静态检查、目标测试、编译 | 见 `execution-log.md` | 通过 | 真实库 Flyway 由用户执行；本机账号返回 MySQL 1045 |
| 2026-07-23 | CRUD Skill + alwaysApply 工程上下文 + 规范契约 | Skill 结构校验、目标测试、差异检查 | 见 `execution-log.md` | 通过 | 官方校验器首次缺少 PyYAML，使用一次性临时环境复跑通过 |
| 2026-07-23 | 真实库缺失旧索引兼容 | 迁移失败复盘、索引历史扫描、Red/Green 契约、静态检查 | 见 `execution-log.md` | 代码通过（5/5） | 用户数据库需 Flyway repair 后重跑 V1.0.51 |

## 6. 执行证据

- `execution-log.md`：已记录命令、结果、警告和跳过项。
- 关键数据库检查：执行 Flyway 后运行以下只读验收 SQL；第一条应返回 0 行，第二条应返回 64 个末位为 `del_flag` 的唯一索引。

```sql
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME = 'logic_delete_active';

SELECT TABLE_NAME,
       INDEX_NAME,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND NON_UNIQUE = 0
GROUP BY TABLE_NAME, INDEX_NAME
HAVING SUBSTRING_INDEX(index_columns, ',', -1) = 'del_flag';
```

- 任选一张目标业务表验证：重复新增有效业务键失败；逻辑删除后 `del_flag=主键`；相同业务键重新新增成功；再次删除成功。
- 服务启动与停止：本轮不启动服务。
