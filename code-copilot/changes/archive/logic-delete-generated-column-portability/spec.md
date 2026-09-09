# 逻辑删除唯一约束与 SQL 可移植性治理
> status: applied-with-db-retry-pending
> created: 2026-07-22
> complexity: 🔴复杂

## 1. 背景与目标

Forge 部分逻辑删除表使用可见生成列 `logic_delete_active`，通过 `CASE WHEN del_flag = 0 THEN 1 ELSE NULL END` 配合唯一索引，只约束未删除记录。第三方数据库工具导出数据时可能把该生成列及计算值写入 `INSERT`，MySQL 会以“生成列不能显式赋值”拒绝导入。

本变更目标：

- 保留逻辑删除后的业务唯一性与并发数据库约束；
- 移除表结构中可见的 `logic_delete_active` 生成列；
- 使用普通 `del_flag` 删除标记和普通复合唯一索引表达“仅未删除记录唯一”；
- 明确 `del_flag` 是逻辑删除必需字段，活跃唯一表达式仅在业务唯一键需要删除后重建时使用；
- 不修改已经进入 Flyway 历史的旧迁移，通过新迁移升级存量库。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `AGENTS.md#5.11`：当前推荐通过可见生成列 `logic_delete_active` 解决逻辑删除后的唯一键语义。
- `forge-server/db/migration/V1.0.2__fix_existing_logic_delete_contract.sql` 至 `V1.0.47__add_outbound_security_policy.sql`：累计为 55 张表建立可见生成列，并由 64 个唯一索引引用。
- 后端实体扫描：81 个 `@TableName` 实体显式声明 `@TableLogic`；其中 26 张表没有生成列，证明生成列不是逻辑删除的必要组成。
- SQL 脚本直接导入绕过 Controller/Service，应用业务逻辑无法拦截或改写生成列赋值。

### 2.2 现有实现

现有唯一键模型：

```sql
logic_delete_active tinyint
  GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
UNIQUE KEY uk_xxx_active (tenant_id, business_code, logic_delete_active)
```

未删除记录得到索引值 `1`，同一业务键只能存在一条；已删除记录得到 `NULL`，利用 MySQL 唯一索引允许多个 `NULL` 的规则保留多条历史记录。

目标模型只保留一个删除字段：

```sql
del_flag bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
UNIQUE KEY uk_xxx_active (tenant_id, business_code, del_flag)
```

未删除记录统一使用 `0`，因此普通唯一索引只允许一个活跃业务键；删除时把 `del_flag` 更新为该行主键，每条历史记录得到不同标记，可以保留任意多条同业务键历史。该模型不依赖生成列、函数索引、部分索引或 `NULL` 唯一语义。

### 2.3 扫描结论

- `del_flag`/`deleted`：负责逻辑删除状态、ORM 删除改写与查询过滤，是逻辑删除表的必要字段。
- `logic_delete_active`：不负责删除，只服务于部分业务唯一键；不是每张逻辑删除表都需要。
- 55 张带辅助生成列的表均存在引用它的活跃唯一索引，不应直接删除列或降级为普通非唯一索引。
- 26 张未使用辅助列的逻辑删除表无需机械补列；历史版本号、请求号等可能要求跨已删除记录永久唯一，也不应改成“仅活跃唯一”。
- 实际开发库只读 `information_schema` 连接因当前账号来源限制返回 MySQL 1045；仓库 Flyway 与实体映射作为本轮静态权威基线，真实库校验列入部署验收。

完整清单见 `schema-audit.md`。

## 3. 功能点

- [x] 扫描实体逻辑删除声明、Flyway 生成列与引用索引。
- [x] 新增 V1.0.51 迁移，将 55 张表的删除字段转换为唯一墓碑标记，将 64 个唯一索引直接关联 `del_flag` 并删除可见生成列。
- [x] 迁移按表检查 `information_schema.COLUMNS`，已迁移或不存在的表安全跳过。
- [x] 更新实体 `@TableLogic` 与自定义删除 SQL，保证删除时写入主键而不是常量 `1`。
- [x] 更新逻辑删除规范和 CRUD SQL 参考，禁止新建可见辅助生成列。
- [x] 增加迁移契约测试，覆盖表数、索引数、删除标记回填、MyBatis-Plus 删除/查询 SQL 和可重复执行保护。

## 4. 业务规则

1. 所有需要软删除的主数据、配置、设计态元数据和业务单据必须有 `del_flag`、`deleted` 或兼容的历史软删除字段。
2. 没有业务唯一键的逻辑删除表可以继续使用布尔型 `del_flag`，不需要扩展删除标记语义。
3. 业务唯一键要求跨历史记录永久唯一时，使用普通唯一索引，不附加删除状态。
4. 业务唯一键只要求未删除记录唯一、且删除后允许同值重建时，使用删除标记唯一索引：

   ```sql
   del_flag bigint NOT NULL DEFAULT 0,
   UNIQUE KEY uk_xxx_active (tenant_id, business_code, del_flag)
   ```

5. 不允许把唯一性降级为 Service 层“先查后插”；数据库唯一索引仍是并发安全边界。
6. 删除标记不能继续只写 `1`；必须写当前行主键或同等唯一且稳定的删除令牌，否则同一业务键只能保留一条已删除记录。
7. 数值主键表的 `del_flag` 使用 `BIGINT/Long`，实体使用 `@TableLogic(value = "0", delval = "主键数据库列名")`。
8. 字符串主键的 `sys_flow_node_config` 使用字符串删除标记和自定义 Mapper 原子更新 `SET del_flag = id`，禁止调用通用 MP 删除方法。
9. 直接 SQL 数据导出必须显式列名；完成本迁移后业务表不再暴露生成列。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 | 54 张数值主键表 | `del_flag` | 改为 `BIGINT`，存量已删除行回填自身主键 |
| 修改 | `sys_flow_node_config` | `del_flag` | 改为 `varchar(64)`，存量已删除行回填字符串主键 |
| 修改 | 55 张历史表 | 64 个 `*_active` 唯一索引 | 将末尾 `logic_delete_active` 键改为 `del_flag` |
| 删除 | 55 张历史表 | `logic_delete_active` | 删除可见生成列，消除 SQL 导入显式赋值问题 |

迁移只重写已删除行的 `del_flag` 墓碑值，不修改有效业务数据、租户数据或审计字段。

## 6. 接口变更

无 HTTP 路径、字段名或请求协议变更。54 张数值主键实体的 `delFlag` 类型改为 `Long` 并显式配置 MyBatis-Plus 删除值；`EmployeeDTO` 与 `EmployeeQuery` 的同名字段同步改为 `Long`；已有自定义删除 Mapper 改为写入主键；`sys_flow_node_config` 的两个删除入口改为专用 Mapper。

## 7. 影响范围

- `forge-server/db/migration/`：新增版本化迁移。
- `forge-server/forge-admin-server`：新增跨模块迁移契约测试。
- `AGENTS.md`、Forge CRUD Skill 和编码规范：更新未来建表约定。
- 数据库升级期间 55 张表依次执行 `ALTER TABLE`，需评估大表 DDL 时间并在执行前备份。

## 8. 风险与关注点

- `ALTER TABLE` 可能持有元数据锁；生产执行前需检查长事务并安排维护窗口。
- 若真实库手工修改过索引名称或结构，迁移应失败关闭，不能静默删除仍被未知索引引用的列。
- 历史迁移也可能主动删除或替换旧索引；V1.0.51 必须按 `information_schema.STATISTICS` 中实际存在的索引动态生成 `DROP INDEX` 子句，不能假设历史索引名仍存在。
- 该变更不涉及资金、状态流转或权限放开，但属于共享数据库结构变更，必须人工审查迁移清单并做好快照。
- 旧 SQL 数据脚本仍含 `logic_delete_active` 列名时，在新结构上会报未知列；旧脚本需重新导出或清理字段后再导入。

### 回滚方式

若必须回滚数据库结构，应按表先删除 `del_flag` 唯一索引，再恢复生成列和原唯一索引；将非零墓碑压回 `1` 前必须确认没有多条同业务键已删除历史，否则数据会违反旧结构或丢失区分能力。

## 8.5 测试策略

- **P0**：迁移契约测试验证 55 张表、64 个索引全部覆盖，存量删除标记回填主键，唯一索引末位改为 `del_flag`，且每表有 `information_schema` 防重复保护。
- **P0**：MyBatis-Plus 契约验证数值删除字段生成 `del_flag=主键列`，查询条件保持 `del_flag=0`。
- **P1**：静态检查 Flyway 无 `${...}` 占位符、无新增 `logic_delete_active GENERATED ALWAYS` 模式、SQL 中无隐式数据插入。
- **P2**：目标 Maven 模块测试与编译。
- **真实库验收**：用户环境执行 Flyway 后查询 `information_schema.COLUMNS` 应无 `logic_delete_active`；64 个唯一索引末位应为 `del_flag`；验证未删除重复插入失败、删除后 `del_flag=主键`、同值重建成功、再次删除成功以及数据导出再导入成功。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

- [x] 是否所有逻辑删除表都需要辅助列：否，只在“仅未删除业务唯一”场景需要等价索引表达式。
- [x] 是否只保留 `del_flag`：是，但活跃唯一表的删除值必须由常量 `1` 升级为唯一主键墓碑。
- [x] 是否修改旧 Flyway：否，新增 V1.0.51。

## 10. 技术决策

| 决策 | 选择 | 放弃方案 | 原因 |
|------|------|----------|------|
| 活跃唯一表达 | 普通唯一索引末位使用 `del_flag` | 可见生成列、函数索引 | 只使用常规列和索引，数据库迁移兼容性更好 |
| 删除状态 | `0` 表示有效，删除后写主键 | 固定写 `1` | 同一字段同时承担状态和墓碑唯一标记，可保留多条已删除历史 |
| 迁移策略 | 新增 V1.0.51，按真实存在的旧索引原子替换索引和列 | 修改旧迁移、无条件删除历史索引 | 遵守 Flyway 历史不可变规则，并兼容索引重命名、缺失和部分执行状态 |
| ORM 集成 | 数值主键使用 `@TableLogic(delval="主键列")`，字符串主键用专用 Mapper | Service 先查后改 | 保留 MP 查询过滤并确保删除更新原子化 |
| 普适规则 | 按唯一性需求判断 | 所有逻辑删除表机械改成 BIGINT | 避免无业务唯一键的表承担无价值改造 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 扫描与设计 | done | `spec.md`, `schema-audit.md` | 实体 81 张，生成列 55 张，活跃唯一索引 64 个 |
| 迁移与代码对齐 | done | `V1.0.51__replace_logic_delete_generated_columns.sql`、54 张数值主键实体、Flow 节点专用删除 Mapper、10 处自定义删除 SQL、代码生成器模板 | 数值主键删除由 MP 写入主键；字符串主键由 Mapper 写入主键 |
| 测试与编译 | done | `LogicDeleteUniqueIndexPortabilityMigrationTest.java`, `test-spec.md`, `execution-log.md` | 5 个契约测试通过，43 个聚合模块安装成功；真实库因 MySQL 1045 未执行 |
| 规范更新 | done | `AGENTS.md`、CRUD Skill 参考、`conventions.md`、`decisions.md` | 后续禁止新增可见辅助生成列 |
| 上下文与 Skill 防回归 | done | CRUD 主 `SKILL.md`、`project-context.md`、SQL 参考、检查清单、契约测试 | 建表前强制读取规则；禁止生成列、`deleted_at NULL` 唯一语义和墓碑固定值 `1` |
| 真实库索引漂移修复 | done | `V1.0.51__replace_logic_delete_generated_columns.sql`、迁移契约测试、Skill SQL 参考和检查清单 | 动态删除实际存在的旧索引；兼容 `ai_code_rule` 的 `uk_ai_code_rule_code` 历史替代索引 |

## 12. 审查结论

实现与静态审查通过：迁移清单覆盖 55 张表和 64 个唯一索引；54 张数值主键实体使用 MyBatis-Plus 主键墓碑删除，字符串主键表使用专用原子更新；目标契约测试和聚合编译均通过。逻辑删除决策规则已进入根 `AGENTS.md`、`alwaysApply` 工程上下文、CRUD 主 Skill、SQL 参考和生成检查清单，并由契约测试防回归。

用户环境首次执行 V1.0.51 时在 `ai_code_rule` 因历史活跃索引已被 V1.0.36 删除而失败；迁移现已改为发现真实索引后原子替换，并由 5 个契约测试覆盖。数据库仍需先修复 Flyway 失败记录再重跑；执行后按 `test-spec.md` 的验收 SQL 核对列、索引及删除重建链路。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-22
- **确认人**：用户在当前会话明确要求扫描并完整解决该问题。
