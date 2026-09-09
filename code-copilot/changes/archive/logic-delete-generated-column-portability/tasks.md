# 任务拆分 — 逻辑删除唯一约束与 SQL 可移植性治理

## 前置条件

- [x] 已读取 `AGENTS.md`、项目记忆、编码规范与自动化测试标准。
- [x] 已确认 `V1.0.50` 已被业务应用运行路由迁移占用，逻辑删除脚本使用 `V1.0.51`。
- [x] 已完成仓库静态清单；真实开发库只读连接因 MySQL 1045 无法使用。

## Task 1: 建立迁移契约测试

- **目标**：先以失败测试固定 55 张表、64 个普通唯一索引、删除标记回填和无可见生成列的目标契约。
- **涉及文件**：
  - `forge-server/forge-admin-server/src/test/java/com/mdframe/forge/admin/migration/LogicDeleteUniqueIndexPortabilityMigrationTest.java` — 新增迁移静态契约测试。
- **验证命令**：
  - `cd forge-server && mvn -pl forge-admin-server -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test`
- [x] 已建立覆盖目标迁移和 MyBatis-Plus SQL 的契约测试；最终 Green 证据已记录。独立 Red 执行结果未保留，作为过程警告记录，不影响最终契约结果。

## Task 2: 新增数据库迁移

- **目标**：把删除字段升级为唯一墓碑标记，按表原子替换 64 个唯一索引并删除 55 个可见生成列。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.51__replace_logic_delete_generated_columns.sql` — 新增 Flyway 迁移。
- **实现约束**：
  - 每张表先查询 `information_schema.COLUMNS`；列不存在时安全跳过。
  - 数值主键表先把 `del_flag` 改为 BIGINT，并把存量非零值回填为主键；字符串主键表使用 varchar 删除标记。
  - 同一个 `ALTER TABLE` 内删除旧索引、增加末位为 `del_flag` 的普通唯一索引、删除生成列。
  - 保留原表名、索引名、业务键顺序和租户维度。
- [x] 迁移契约测试转绿，55 张表、64 个索引、54 个 BIGINT 标记和 1 个字符串标记全部满足契约。

## Task 3: 对齐 MyBatis-Plus 删除行为

- **目标**：保证新增数据为 `del_flag=0`，删除时原子写入当前行主键。
- **涉及文件**：
  - 54 张数值主键实体 — `delFlag` 改为 `Long` 并配置 `@TableLogic(value="0", delval="主键列")`。
  - 已有自定义删除 Mapper XML — `SET del_flag = 1` 改为 `SET del_flag = 主键列`。
  - `FlowNodeConfigMapper.xml` / `FlowNodeConfigServiceImpl.java` — 字符串主键表使用专用逻辑删除 SQL。
- [x] MyBatis-Plus 生成 SQL 与自定义删除 SQL 契约测试转绿：删除条件为 `del_flag=id`，查询条件为 `del_flag=0`。

## Task 4: 更新项目规范

- **目标**：后续建表不再产生可见 `logic_delete_active` 生成列。
- **涉及文件**：
  - `AGENTS.md` — 修正逻辑删除唯一键规则。
  - `.agents/skills/forge-codegen-crud/references/sql-seeds.md` — 补充删除标记唯一索引模板。
  - `.agents/skills/forge-codegen-crud/references/validation-checklist.md` — 增加辅助生成列禁用检查。
  - `forge-docs/guide/conventions.md` — 补充面向开发者的逻辑删除索引说明。
  - `code-copilot/memory/decisions.md` — 沉淀架构决策。
- [x] 规范明确 `del_flag` 与唯一索引的职责边界，并同步代码生成器模板。

## Task 5: 增量验证与记录

- **目标**：完成静态、测试和编译验证并记录真实限制。
- **涉及文件**：
  - `code-copilot/changes/logic-delete-generated-column-portability/test-spec.md`
  - `code-copilot/changes/logic-delete-generated-column-portability/execution-log.md`
  - `code-copilot/changes/logic-delete-generated-column-portability/spec.md`
  - `code-copilot/changes/logic-delete-generated-column-portability/tasks.md`
- **验证命令**：
  - `git diff --check`
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`
  - `cd forge-server && mvn -pl forge-admin-server -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test`
  - `cd forge-server && mvn -pl forge-admin-server -am compile -DskipTests`
- [x] 已记录所有通过、警告、跳过项和真实数据库验收 SQL；真实 Flyway 留待用户数据库维护窗口执行。

## Task 6: 固化系统上下文与 CRUD Skill

- **目标**：确保后续 Agent 在建表、Flyway 或 CRUD 生成时必然读取逻辑删除唯一键决策规则。
- **涉及文件**：
  - `.agents/skills/forge-codegen-crud/SKILL.md` — 将逻辑删除列和活跃唯一索引加入触发描述、工作流和不可违反规则。
  - `.agents/skills/forge-codegen-crud/references/sql-seeds.md` — 补充 `deleted_at = NULL` 唯一索引反例。
  - `.agents/skills/forge-codegen-crud/references/validation-checklist.md` — 增加逻辑删除唯一性检查项。
  - `code-copilot/rules/project-context.md` — 在 `alwaysApply` 工程上下文增加强制决策规则。
  - `LogicDeleteUniqueIndexPortabilityMigrationTest.java` — 锁定主 Skill、SQL 参考和工程上下文规则。
- [x] Skill 官方结构校验通过，目标契约测试最终 5/5 通过，`git diff --check` 通过。

## Task 7: 修复真实库旧索引缺失导致的 Flyway 失败

- **目标**：V1.0.51 不再无条件删除静态清单中的索引，兼容历史迁移改名、删除和本次迁移部分执行后的真实状态。
- **根因**：`V1.0.36__add_structured_code_rule_segments.sql` 已删除 `uk_ai_code_rule_code_active`，并使用永久唯一索引 `uk_ai_code_rule_code`；V1.0.51 的静态 `DROP INDEX` 因此失败。
- **实现**：
  - 55 张表逐表查询 `information_schema.STATISTICS`。
  - 动态拼接实际存在且引用 `logic_delete_active` 的索引，以及同名目标索引。
  - `ai_code_rule` 额外识别并删除历史替代索引 `uk_ai_code_rule_code`。
  - 在同一个 `ALTER TABLE` 中删除实际旧索引、删除生成列并建立墓碑唯一索引。
- [x] 新增索引漂移失败契约，确认 Red 后完成修复；目标测试 5/5 通过。
