# 执行日志 — 逻辑删除唯一约束与 SQL 可移植性治理

## 2026-07-22 扫描与设计

- 扫描 `@TableLogic` 实体：81 张表。
- 扫描历史 Flyway：55 张表存在 `logic_delete_active`，64 个唯一索引引用该列。
- 其余 26 张逻辑删除表未使用辅助生成列，确认辅助列不是逻辑删除必需字段。
- 尝试使用 `application-dev.yml` 的 master 配置只读查询 `information_schema`，MySQL 返回 `1045 Access denied`；未执行任何数据库写操作。
- 初始考虑函数唯一索引，用户明确提出跨数据库兼容性要求后改为删除标记模型：未删除为 0，删除后写主键，唯一索引直接包含 `del_flag`。
- 反编译项目实际使用的 MyBatis-Plus 3.5.7，确认数值 `@TableLogic(delval="id")` 会生成未加引号的列引用；String 字段会加引号，因此字符串主键表必须使用专用 Mapper。

## 2026-07-22 实现与增量验证

- 新增 `V1.0.51__replace_logic_delete_generated_columns.sql`：覆盖 55 张表、64 个活跃唯一索引，包含 54 个 BIGINT 删除墓碑和 1 个字符串删除墓碑，不包含 `GENERATED ALWAYS`。
- 更新 54 张数值主键实体的 `delFlag` 类型和 `@TableLogic(value = "0", delval = "主键列")`，同步对齐 `EmployeeDTO`/`EmployeeQuery` 字段类型和目标实体注释；更新 10 处目标表自定义删除 SQL；`sys_flow_node_config` 使用专用 Mapper 原子执行 `SET del_flag = id`。
- 更新代码生成器实体与主子表 Mapper 模板，只有 `Long del_flag + Long 主键` 使用主键墓碑语义；普通 `tinyint/Integer del_flag` 仍保持布尔逻辑删除。
- 聚合安装/编译：

  ```bash
  JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -pl forge-admin-server -am install -DskipTests
  ```

  结果：43 个模块全部 `SUCCESS`。

- 目标契约测试：

  ```bash
  JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -Penable-tests -pl forge-admin-server \
    -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test
  ```

  结果：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。测试实际确认 MP 3.5.7 生成删除片段 `del_flag=id`、查询片段 `del_flag=0`。

- 静态检查：`git diff --check` 通过；V1.0.51 Flyway `${...}` 占位符扫描无输出；未跟踪文件尾随空格扫描通过；新迁移中无生成列表达式。
- 固定删除值扫描仅剩 `CustomQuerySchemeMapper.xml: SET del_flag = '1'`。该表不属于 55 张活跃唯一索引表，继续使用布尔逻辑删除符合本次规则。
- 使用 `-Penable-tests -am` 尝试从聚合层直接执行测试时，既有 `forge-starter-datascope` 测试引擎配置先行失败；先完成聚合安装，再在 `forge-admin-server` 单模块执行目标测试后通过。该问题与本次变更无关。
- 独立 Red 阶段未保留执行报告，最终 Green 契约及构建证据完整，作为非阻断过程警告保留。
- 真实数据库仍因 MySQL 1045 未执行 Flyway、索引 DDL和数据导出/导入验收；未启动 Admin/Flow 服务，未产生需要清理的进程。
- 部署要求：备份目标库、停止目标表写入、检查长事务并安排维护窗口。旧 SQL 若显式包含 `logic_delete_active`，需重新导出或移除该列后再导入。

## 2026-07-23 系统上下文与 Skill 固化

- 用户要求把逻辑删除字段规则加入系统上下文或 Skill，避免未来建表重新引入错误设计。
- 更新 CRUD 主 `SKILL.md`：将逻辑删除字段与活跃唯一索引加入触发描述；任何建表/Flyway 审查前必须读取 SQL 参考和检查清单；不可违反规则明确 `del_flag + @TableLogic`、主键墓碑和禁用方案。
- 更新 `alwaysApply` 的 `code-copilot/rules/project-context.md`：增加“逻辑删除与唯一键（强制）”决策树，区分普通 `0/1`、主键墓碑、跨历史永久唯一和字符串主键例外。
- SQL 参考与检查清单新增反例：禁止用有效行 `deleted_at = NULL` 的唯一索引表达活跃唯一性，因为 MySQL 允许唯一索引存在多个 `NULL`。
- 扩展契约测试，锁定 CRUD 主 Skill、SQL 参考和工程上下文中的关键规则文本。
- Skill 官方 `quick_validate.py` 首次因系统 Python 缺少 `PyYAML` 失败；在一次性临时虚拟环境安装校验依赖后复跑，结果为 `Skill is valid!`。
- 目标测试：`mvn -Penable-tests -pl forge-admin-server -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test`，结果 `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `git diff --check` 和关键规则文本扫描通过；本轮未启动服务、未连接或修改数据库。

## 2026-07-23 真实库旧索引缺失修复

- 用户环境执行 V1.0.51，在 `ai_code_rule` 报错 `Can't DROP 'uk_ai_code_rule_code_active'; check that column/key exists`。MySQL DDL 非事务化，失败前已完成表的变更可能已经提交。
- 根因确认：V1.0.10 创建过 `uk_ai_code_rule_code_active`，但 V1.0.36 后续主动删除该索引并创建/保留 `uk_ai_code_rule_code (tenant_id, rule_code)`；原迁移静态清单没有计算索引的最终历史状态。
- 第一轮测试因契约仍指向已被占用的 V1.0.50 而出现 2 个 `NoSuchFileException`；修正为 V1.0.51 后，新增索引动态发现契约按预期 Red：5 个测试中 3 个失败。
- 修复 V1.0.51：55 张表分别查询 `information_schema.STATISTICS`，用 `GROUP_CONCAT` 动态生成实际存在的 `DROP INDEX` 子句，再在同一个 `ALTER TABLE` 中删除旧索引和生成列、增加墓碑唯一索引。
- `ai_code_rule` 同时把 `uk_ai_code_rule_code_active` 和 `uk_ai_code_rule_code` 作为候选旧索引，避免永久唯一索引继续阻止删除后重建。
- Green 验证：`mvn -Penable-tests -pl forge-admin-server -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test`，结果 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 静态检查：55 个列存在保护、55 个索引发现查询、64 个目标唯一索引和 55 个动态原子替换块均满足契约；Flyway 占位符扫描无输出，`git diff --check` 通过。
- 本轮未直接连接或修改用户数据库。用户数据库需要先对失败的 V1.0.51 执行 Flyway repair，再使用修复后的同版本脚本重跑。

## 2026-07-23 最终复验

- 复跑 `mvn -Penable-tests -pl forge-admin-server -Dtest=LogicDeleteUniqueIndexPortabilityMigrationTest test`，结果 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 最终静态检查通过：`git diff --check` 无输出，V1.0.51 Flyway `${...}` 占位符扫描无输出；迁移包含 55 个索引动态发现块和 64 个目标唯一索引。
- 本轮未启动服务、未连接或修改用户数据库；真实库仍需先执行 Flyway repair，再重跑修复后的 V1.0.51。
