# 单测 Spec — 数据表逻辑删除分批改造
> status: propose
> created: 2026-07-07

## 0. 测试原则

- **Red/Green TDD**：涉及新增逻辑的单测必须先 Red 再 Green。
- **First Run the Tests**：开始前先跑相关模块已有测试或编译，记录基线。
- **展示工作**：必须把实际命令、结果、警告、跳过项写入 `execution-log.md`。
- **增量复用**：执行 `/test` 或阶段验收前，先读取 `code-copilot/rules/automated-testing-standard.md` 和当前变更四件套。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | 以项目 Maven 依赖为准 |
| Mock 框架 | 以项目 Maven 依赖为准 |
| 已有测试数量 | `/test` 阶段统计 |
| 已有测试风格 | 优先复用现有 service/mapper 测试风格 |

## 2. 覆盖范围

### P0 — Batch 1 核心逻辑（必须覆盖）

| 类名 | 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|------|----------|
| `EmployeeServiceImpl` | `deleteById(Long id)` | 删除员工 | 存在的员工 ID | 数据库记录保留，`del_flag=1` |
| `ReportTemplateService` | `deleteTemplate(Long id, Long userId)` | 删除模板 | 当前用户拥有的模板 ID | 数据库记录保留，`del_flag=1` |
| `FlowNodeConfigServiceImpl` | `deleteConfig(String id)` | 删除节点配置 | 存在配置 ID | 主配置逻辑删除，层级/操作配置按分类处理 |

### P1 — Mapper XML 查询过滤

| Mapper | 方法 | 场景 | 预期结果 |
|--------|------|------|----------|
| `ReportProjectMapper.xml` | `selectProjectPage` | 存在 `del_flag=1` 项目 | 列表不返回 |
| `ReportProjectMapper.xml` | `countByDirectoryIds` | 目录下只有已删除项目 | 计数为 0 |
| `FlowNodeConfigMapper.xml` | `selectByModelKeyAndNode` | 同节点存在已删除配置 | 返回未删除配置 |

### P2 — SQL 迁移

| 脚本 | 场景 | 预期结果 |
|------|------|----------|
| Batch 1 Flyway | 目标列不存在 | 自动新增列 |
| Batch 1 Flyway | 目标列已存在 | 不报错 |
| Batch 2/3 Flyway | 表不存在 | 不报错，跳过 |

### 不测试（明确列出原因）

- `qrtz_*`、`act_*`、`flw_*`：框架自带表，明确排除。
- 授权关系表：默认保留物理删除，防止脏授权。
- 纯日志清理表：保留物理删除，按留存策略清理。

## 3. 执行计划

- [ ] Step 1: 读取 `code-copilot/rules/automated-testing-standard.md`。
- [ ] Step 2: 运行 Batch 1 相关模块编译，确认基线。
- [ ] Step 3: 为 Batch 1 代表服务补充最小单测或集成 smoke。
- [ ] Step 4: 执行 Flyway SQL 幂等验证。
- [ ] Step 5: 扩展 Batch 2/3 后按模块补充 smoke。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-07 | 静态审计 | `rg` + SQL/实体扫描 | 已完成 | 远程库只读查询未完成 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-07 | Batch 1 | 编译 + 静态 SQL 检查 | `git diff --check`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.2__fix_existing_logic_delete_contract.sql`; `mvn -q -pl forge-admin-server -am -DskipTests compile`; `mvn -q -pl forge-report-server -am -DskipTests compile`; `mvn -q -pl forge-flow/forge-flow-server -am -DskipTests compile` | 通过 | 首次 Maven 使用 JDK 8 失败，改用 OpenJDK 17 后通过；远程/本地 MySQL 均不可用，未做 SQL 实跑和接口 smoke |
| 2026-07-07 | Batch 2.1 | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.3__add_logic_delete_to_platform_internal_tables.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.3 SQL 实跑和接口 smoke |
| 2026-07-07 | Batch 2.2 | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.4__add_logic_delete_to_system_config_tables.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.4 SQL 实跑和接口 smoke |
| 2026-07-07 | Batch 2.3a | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.5__add_logic_delete_to_org_post_tables.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.5 SQL 实跑和接口 smoke；用户/角色/租户/菜单资源未进入本批 |
| 2026-07-07 | Batch 2.3b-1 | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.6__add_logic_delete_to_tenant_table.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.6 SQL 实跑和接口 smoke；用户/角色/菜单资源未进入本批 |
| 2026-07-07 | Batch 2.3b-2 | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.7__add_logic_delete_to_role_table.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.7 SQL 实跑和接口 smoke；用户/菜单资源未进入本批 |
| 2026-07-07 | Batch 2.3b-3 | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.8__add_logic_delete_to_resource_table.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.8 SQL 实跑和接口 smoke；用户表未进入本批 |
| 2026-07-07 | Batch 2.3b-4 | 编译 + XML/SQL 静态检查 | `git diff --check`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.9__add_logic_delete_to_user_table.sql`; `xmllint --noout ...`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.9 SQL 实跑和登录/接口 smoke |
| 2026-07-07 | Batch 3.1 | 编译 + XML/SQL 静态检查 | `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.10__add_logic_delete_to_lowcode_design_metadata_tables.sql`; `xmllint --noout ...`; `git diff --check`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.10 SQL 实跑和接口 smoke；数据资产/报表目录/业务样例表未进入本批 |
| 2026-07-07 | Batch 3.2 | 编译 + XML/SQL 静态检查 | `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.11__add_logic_delete_to_report_data_metadata_tables.sql`; `xmllint --noout ...`; `git diff --check`; `mvn -q -pl forge-admin-server -am -DskipTests compile`; `mvn -q -pl forge-report-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.11 SQL 实跑和接口 smoke；采购样例业务表未进入本批 |
| 2026-07-07 | Batch 3.3 | 编译 + XML/SQL 静态检查 | `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.12__add_logic_delete_to_sample_purchase_order.sql`; `xmllint --noout ...`; `git diff --check`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.12 SQL 实跑和接口 smoke |
| 2026-07-07 | Task 4 | 编译 + XML/SQL 静态检查 | `rg -n -i 'delete\\s+from' forge-server -g '*Mapper.xml'`; `rg -n '[ \t]$' ...`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql`; `xmllint --noout ...`; `git diff --check`; `mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，未做 V1.0.13 SQL 实跑和接口 smoke；剩余 13 处 `DELETE FROM` 均已分类为保留物理删除 |
| 2026-07-07 | Task 5 | 最终收尾验证 | `rg -n '\$\{[^}]+\}' V1.0.2..V1.0.13`; `rg -n '[ \t]$' ...`; `git diff --check`; `rg -n -i 'delete\\s+from' forge-server -g '*Mapper.xml'`; `mvn -q -pl forge-admin-server -am -DskipTests compile`; `mvn -q -pl forge-report-server -am -DskipTests compile`; `mvn -q -pl forge-flow/forge-flow-server -am -DskipTests compile` | 通过 | 本地 MySQL 3407 不可用，最终 SQL 实跑和接口 smoke 跳过 |

## 6. 执行证据

- `execution-log.md`：记录所有命令和结果。
- 关键接口：员工删除、报表项目/模板删除、流程节点配置删除。
- 关键数据库检查：删除前后记录是否保留，删除字段是否置位。
- 服务启动与停止：如需 API smoke，记录进程和清理情况。
