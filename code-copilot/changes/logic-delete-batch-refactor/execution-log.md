# 变更日志 — 数据表逻辑删除分批改造

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-07 | propose | 完成静态审计和分批规划 | 原计划从 `docs/superpowers/plans` 移入 SDD 目录 |
| 2026-07-07 | propose | 明确定时任务/Flowable 排除边界 | 只排除 `qrtz_*`、`act_*`、`flw_*` 等框架自带表；`sys_job_config/sys_job_log` 纳入内部表 |
| 2026-07-07 | apply | 用户确认执行 `/apply logic-delete-batch-refactor` | 切换到 `feature/logic-delete-batch-refactor` 分支 |
| 2026-07-07 | apply | 当前库只读查询未完成 | 沙箱内远程 MySQL 连接失败；提权审批服务返回 503，未绕过审批 |
| 2026-07-07 | apply | 完成 Batch 1 代码和 Flyway 改造 | 新增 `V1.0.2__fix_existing_logic_delete_contract.sql`，补实体 `@TableLogic` 和 XML 过滤 |
| 2026-07-07 | verify | 完成 Batch 1 编译验证 | admin/report/flow 三个 Maven 编译均通过 |
| 2026-07-07 | apply | Git 提交未完成 | `.git/index.lock` 写入被沙箱拒绝；提权审批服务 503，未绕过审批 |
| 2026-07-07 | apply | 完成 Batch 2.1 代码和 Flyway 改造 | AI/消息/任务/低代码配置表新增逻辑删除契约 |
| 2026-07-07 | verify | 完成 Batch 2.1 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过 |
| 2026-07-07 | apply | 完成 Batch 2.2 代码和 Flyway 改造 | 系统配置/字典/通知/API/数据权限/文件存储配置表新增逻辑删除契约 |
| 2026-07-07 | verify | 完成 Batch 2.2 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过 |
| 2026-07-07 | apply | 完成 Batch 2.3a 组织/岗位代码和 Flyway 改造 | `sys_org`、`sys_post` 新增逻辑删除契约，用户/角色/租户/菜单资源继续拆分 |
| 2026-07-07 | verify | 完成 Batch 2.3a 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 2.3b-1 租户代码和 Flyway 改造 | `sys_tenant` 新增逻辑删除契约，用户/角色/菜单资源继续拆分 |
| 2026-07-07 | verify | 完成 Batch 2.3b-1 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 2.3b-2 角色代码和 Flyway 改造 | `sys_role` 新增逻辑删除契约，用户/菜单资源继续拆分 |
| 2026-07-07 | verify | 完成 Batch 2.3b-2 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 2.3b-3 菜单/权限资源代码和 Flyway 改造 | `sys_resource` 新增逻辑删除契约，用户表继续拆分 |
| 2026-07-07 | verify | 完成 Batch 2.3b-3 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 2.3b-4 用户代码和 Flyway 改造 | `sys_user` 新增逻辑删除契约；核心身份/权限主表批次完成 |
| 2026-07-07 | verify | 完成 Batch 2.3b-4 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 3.1 低代码/应用中心代码和 Flyway 改造 | `ai_business_suite/app/object/trigger`、`ai_code_rule`、`ai_lowcode_domain/model` 新增逻辑删除契约 |
| 2026-07-07 | verify | 完成 Batch 3.1 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 3.2 数据资产/报表目录代码和 Flyway 改造 | `ai_report_data_connection/dataset/dataset_category/business_definition/dimension`、`ai_report_directory` 新增逻辑删除契约 |
| 2026-07-07 | verify | 完成 Batch 3.2 增量验证 | admin/report 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Batch 3.3 采购样例业务表代码和 Flyway 改造 | `sample_purchase_order` 新增逻辑删除契约 |
| 2026-07-07 | verify | 完成 Batch 3.3 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | apply | 完成 Task 4 XML 物理删除评审和必要改造 | `ai_prompt_template`、`ai_custom_query_scheme` 转逻辑删除；剩余 `DELETE FROM` 分类保留 |
| 2026-07-07 | verify | 完成 Task 4 增量验证 | admin 聚合编译、XML 语法、Flyway 占位符和空白检查通过；本地 MySQL 不可用，SQL 实跑跳过 |
| 2026-07-07 | verify | 完成 Task 5 最终收尾验证 | admin/report/flow 聚合编译和静态检查通过；本地 MySQL 不可用，SQL 实跑和接口 smoke 跳过 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 变更管理 | 使用 `code-copilot/changes/logic-delete-batch-refactor/` | `docs/superpowers/plans/` | 项目采用 SDD，所有变更产物必须在 code-copilot changes 下 |
| 逻辑删除机制 | MyBatis-Plus `@TableLogic` + Mapper XML 显式过滤 | 自定义全局删除拦截器 | 符合现有技术栈，风险更小 |
| 表范围 | 分批改造 | 一次性全表改造 | 避免关系表、日志表、框架表语义混乱 |
| 框架表排除 | 排除 `qrtz_*`, `act_*`, `flw_*` | 排除所有 `sys_job_*` / `sys_flow_*` | `sys_job_*` 和 `sys_flow_*` 多数是 Forge 内部业务表 |
| 远程库盘点受阻 | 不绕过审批，使用静态 DDL + 幂等迁移兜底 | 非授权方式连接远程库 | 符合工具权限规则，脚本通过 `information_schema` 判断表/列/索引存在性 |
| 逻辑删除唯一约束 | 生成列 `logic_delete_active` + active 唯一索引 | 直接把唯一键改成 `(业务键, del_flag)` | 多次删除同一业务键时，`del_flag=1` 会继续冲突；生成列对已删除记录返回 `NULL`，只约束未删除记录 |
| 任务日志留存清理 | `sys_job_log` 行级删除逻辑删除，`cleanLog(days)` 保持物理删除 | 所有 `sys_job_log` 删除都改为逻辑删除 | 用户要求 `sys_job_log` 纳入内部表；但留存清理的语义是释放历史日志，需要通过专用 Mapper XML 明确保留物理删除 |
| Batch 2.3 拆分 | 先做 `sys_org/sys_post`，延后 `sys_user/sys_role/sys_tenant/sys_resource` | 身份/权限/菜单资源一次性改造 | 用户、角色、租户、资源影响登录、授权、菜单、关系表重建和低代码菜单注册，风险明显高于组织/岗位 |
| Batch 2.3b 继续拆分 | 先做 `sys_tenant`、`sys_role`、`sys_resource`，最后单独处理 `sys_user` | 用户、角色、租户、资源一次性改造 | 租户、角色、资源主表改造面相对可控；用户表影响登录、会话、租户成员和组织授权，需要作为独立小批次收尾 |
| Batch 2.3b-2 角色边界 | 只软删 `sys_role` 主表，角色关系表仍物理清理 | 同时软删 `sys_role_resource/sys_role_org/sys_user_org_role` | 关系表是授权重建数据，默认物理删除可避免脏授权和恢复冲突；角色主表通过 active 唯一索引支持重建 |
| Batch 2.3b-3 资源边界 | 只软删 `sys_resource` 主表，`sys_role_resource` 仍物理清理 | 同时软删角色资源关系表 | 资源删除入口清理授权关系，关系表是授权重建数据；资源主表软删后通过 active 唯一索引支持重建同权限标识 |
| Batch 2.3b-4 用户边界 | 只软删 `sys_user` 主表，用户租户/组织/岗位/角色关系表仍物理清理 | 同时软删用户关系表 | 用户关系表是授权和成员关系重建数据；现有删除入口已清理关系，主表软删保留审计和重建用户名能力 |
| Batch 3.1 低代码关系边界 | 只软删低代码/应用中心主设计态表，`ai_business_binding`、`ai_business_object_relation`、`ai_business_trigger_log` 仍物理删除 | 同时软删低代码关系/日志表 | 关系表属于配置重建数据，日志表属于执行审计/留存数据；本批只在 join 展示时过滤已删除主对象/套件 |
| Batch 3.2 数据资产关系边界 | 只软删数据资产/报表目录主元数据表，数据集字段、ACL、行权限、维度项和业务数据集绑定表仍物理删除 | 同时软删所有数据资产子配置表 | 字段、ACL、行权限、维度项和绑定属于重建型子配置/关系数据；本批只在 join 和摘要统计时过滤已删除主数据 |
| Batch 3.3 流程样例边界 | 只软删 `sample_purchase_order` 主业务表，Flowable/Forge 流程任务表仍按原语义处理 | 同时改造流程任务/实例表 | 流程运行表不属于采购样例业务主表，且用户已明确 Flowable 自带表排除；业务表查询和回调按 businessKey 过滤已删除记录 |
| Task 4 用户配置主表 | `ai_prompt_template`、`ai_custom_query_scheme` 转逻辑删除 | 继续保留 XML 物理删除 | 两者是用户可维护配置主数据，删除后应支持审计和重建业务键 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 实体已有 `@TableLogic` 但表缺字段 | DDL 和 Java 演进不同步 | Batch 1 先补字段并做幂等迁移 | 待归档确认 |
| 表已有 `del_flag` 但删除仍物理删 | 实体缺 `@TableLogic` | 给实体补注解/字段，XML 查询补过滤 | 待归档确认 |
| XML 查询绕过 MP 自动逻辑删除 | 自定义 SQL 不受 BaseMapper 过滤影响 | 所有 XML select 手工加 `del_flag/deleted` 条件 | 待归档确认 |
| 逻辑删除后主键复用冲突 | `ai_report_template.id = source_project_id`，软删后重新从同一项目创建会撞主键 | 增加包含已删除记录的查询与恢复方法，重建时先恢复再更新 | 待归档确认 |

## 知识发现

- [ ] **逻辑删除范围边界**: 框架自带表和项目内部表不能只按表名前缀粗暴划分，`sys_job_config/sys_job_log` 属于 Forge 内部表，需要纳入盘点。
- [ ] **逻辑删除唯一约束模式**: MySQL 逻辑删除表如需保留业务唯一性，优先使用“生成列返回 active 标识 + 唯一索引”，不要直接用 `(业务键, del_flag)`。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 远程实库盘点 | `/apply` 前完成当前库 `information_schema` 盘点 | 沙箱连接失败且提权审批服务 503 | 记录为 warning，使用静态 DDL 盘点和幂等迁移兜底；后续可在有数据库权限时补跑 |

## 验证记录

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-07 | 工作区 | `git status --short` | 有无关 `.DS_Store` 和 `code-copilot/changes/spec(2).md` 变更 | 本轮不处理这些无关变更 |
| 2026-07-07 | 远程库只读盘点 | `MYSQL_PWD=*** mysql -h 120.48.96.178 -P 3306 -u rdsroot -D forge_admin_new -N -e ...` | 失败 | 沙箱内 `Can't connect to MySQL server`; 提权审批服务 503 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.2__fix_existing_logic_delete_contract.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译基线 | `mvn -q -pl forge-admin-server -am -DskipTests compile` | 首次失败 | 当前 shell 使用 JDK 8，报 `无效的目标发行版: 17` |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 使用 OpenJDK 17 |
| 2026-07-07 | report 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-report-server -am -DskipTests compile` | 通过 | 覆盖报表实体/XML/Mapper 改动 |
| 2026-07-07 | flow 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-flow/forge-flow-server -am -DskipTests compile` | 通过 | 覆盖流程节点配置实体/XML 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 SQL 幂等实跑 |
| 2026-07-07 | Git 提交 | `git add <本次变更路径>` | 失败 | 沙箱无 `.git` 写权限；提权审批服务 503，因此未生成 SDD commit |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.3__add_logic_delete_to_platform_internal_tables.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' code-copilot/changes/logic-delete-batch-refactor forge-server/db/migration/V1.0.3__add_logic_delete_to_platform_internal_tables.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobLogMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeDomainMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/resources/mapper/SysJobLogMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 AI、generator、message、job 模块改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.3 SQL 幂等实跑 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.4__add_logic_delete_to_system_config_tables.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' code-copilot/changes/logic-delete-batch-refactor forge-server/db/migration/V1.0.4__add_logic_delete_to_system_config_tables.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysNoticeReadRecordMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/resources/mapper/SysApiConfigMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysDataScopeConfigMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudExportTaskMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysNoticeReadRecordMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-api-config/src/main/resources/mapper/SysApiConfigMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysDataScopeConfigMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudExportTaskMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 system、api-config、datascope、generator XML 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.4 SQL 幂等实跑 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.5__add_logic_delete_to_org_post_tables.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.5__add_logic_delete_to_org_post_tables.sql ... SysOrgMapper.xml SysPostMapper.xml SysUserMapper.xml SysUserOrgMapper.xml DataScopeOrgMapper.xml SysMessageReceiverMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysPostMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysNoticeReadRecordMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/DataScopeOrgMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 org/post 实体、system XML、message XML、datascope mapper 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.5 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | 自动化测试标准 | `sed -n '1,260p' code-copilot/rules/automated-testing-standard.md` | 已读取 | 按增量验证范围复用当前 test-spec/execution-log，不从零规划 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.6__add_logic_delete_to_tenant_table.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.6__add_logic_delete_to_tenant_table.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysTenant.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysTenantMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserTenantMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysPostMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysTenantMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserTenantMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysPostMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 tenant 实体和 system XML 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.6 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.7__add_logic_delete_to_role_table.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.7__add_logic_delete_to_role_table.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysRole.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/mapper/DataScopeRoleMapper.java forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/DataScopeRoleMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysRoleDataScopeMapper.xml` | 通过 | 首次发现 `DataScopeRoleMapper.java` 两处尾随空白，修复后通过 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/DataScopeRoleMapper.xml forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysRoleDataScopeMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 role 实体、system XML 和 datascope mapper/XML 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.7 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.8__add_logic_delete_to_resource_table.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.8__add_logic_delete_to_resource_table.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysResource.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessPermissionMapper.xml` | 通过 | 首次发现 `SysResourceMapper.xml` 两处尾随空白，修复后通过 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessPermissionMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 resource 实体、system XML 和 generator XML 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.8 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.9__add_logic_delete_to_user_table.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.9__add_logic_delete_to_user_table.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUser.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml ... FlowModelVersionMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysTenantMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysNoticeReadRecordMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessMessageChannelMapper.xml forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiDashboardGenerateRecordMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiChatSessionMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiPromptTemplateMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowModelVersionMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 user 实体、登录/用户/消息/AI/流程/业务样例 XML 改动 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.9 SQL 幂等实跑和登录/接口 smoke |
| 2026-07-07 | 自动化测试标准 | `sed -n '1,260p' code-copilot/rules/automated-testing-standard.md` | 已读取 | 按增量验证范围复用当前 test-spec/execution-log，不从零规划 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.10__add_logic_delete_to_lowcode_design_metadata_tables.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件/相关文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.10__add_logic_delete_to_lowcode_design_metadata_tables.sql ... BusinessSuiteMapper.xml BusinessAppMapper.xml BusinessObjectMapper.xml BusinessTriggerMapper.xml CodeRuleMapper.xml AiLowcodeDomainMapper.xml AiLowcodeModelMapper.xml BusinessObjectRelationMapper.xml BusinessBindingMapper.xml AiCrudConfigMapper.xml` | 通过 | 首次整文件扫描发现 Batch 2.3b 角色相关文件历史尾随空白，已做机械去尾空白后通过 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessSuiteMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessTriggerMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/CodeRuleMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeDomainMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeModelMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectRelationMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessBindingMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 generator 低代码/应用中心实体、Mapper XML、system 角色空白清理 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.10 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | 自动化测试标准 | `sed -n '1,260p' code-copilot/rules/automated-testing-standard.md` | 已读取 | 按增量验证范围复用当前 test-spec/execution-log，不从零规划 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.11__add_logic_delete_to_report_data_metadata_tables.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件/相关文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.11__add_logic_delete_to_report_data_metadata_tables.sql ... DataConnectionMapper.xml DataDatasetMapper.xml DataDatasetCategoryMapper.xml DataBusinessDefinitionMapper.xml DataDimensionMapper.xml DataBusinessDatasetMapper.xml ReportDirectoryMapper.xml` | 通过 | 首次发现 `DataConnectionMapper.xml` 一处尾随空白，修复后通过 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataConnectionMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetCategoryMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataBusinessDefinitionMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDimensionMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataBusinessDatasetMapper.xml forge-server/forge-report-server/src/main/resources/mapper/project/directory/ReportDirectoryMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖 forge-plugin-data 实体、Mapper XML 和 V1.0.11 迁移脚本 |
| 2026-07-07 | report 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-report-server -am -DskipTests compile` | 通过 | 覆盖报表目录实体和 Mapper XML |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.11 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.12__add_logic_delete_to_sample_purchase_order.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件/相关文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.12__add_logic_delete_to_sample_purchase_order.sql forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/domain/SamplePurchaseOrder.java forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖采购样例实体、Mapper XML 和 V1.0.12 迁移脚本 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.12 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | XML 物理删除扫描 | `rg -n -i "delete\\s+from" forge-server -g '*Mapper.xml'` | 已分类 | 初始 15 处；`ai_prompt_template`、`ai_custom_query_scheme` 转逻辑删除后剩余 13 处均保留物理删除 |
| 2026-07-07 | Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql` | 通过 | 无输出，未发现 Flyway 占位符 |
| 2026-07-07 | 新文件/相关文件尾随空白检查 | `rg -n '[ \t]$' forge-server/db/migration/V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/prompt/domain/AiPromptTemplate.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiPromptTemplateMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/CustomQueryScheme.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/CustomQuerySchemeMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | XML 语法检查 | `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiPromptTemplateMapper.xml forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/CustomQuerySchemeMapper.xml` | 通过 | 无输出 |
| 2026-07-07 | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | admin 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-admin-server -am -DskipTests compile` | 通过 | 覆盖提示词模板、用户查询方案实体/XML 和 V1.0.13 迁移脚本 |
| 2026-07-07 | 本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，未执行 V1.0.13 SQL 幂等实跑和接口 smoke |
| 2026-07-07 | 最终 Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.2__fix_existing_logic_delete_contract.sql ... forge-server/db/migration/V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql` | 通过 | 无输出，新增迁移未发现 Flyway 占位符 |
| 2026-07-07 | 最终尾随空白检查 | `rg -n '[ \t]$' code-copilot/changes/logic-delete-batch-refactor forge-server/db/migration/V1.0.2__fix_existing_logic_delete_contract.sql ... forge-server/db/migration/V1.0.13__add_logic_delete_to_prompt_and_query_scheme_tables.sql` | 通过 | 无输出 |
| 2026-07-07 | 最终空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-07 | 最终 XML 物理删除扫描 | `rg -n -i "delete\\s+from" forge-server -g '*Mapper.xml'` | 通过 | 剩余 13 处与 Task 4 保留物理删除清单一致 |
| 2026-07-07 | report 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-report-server -am -DskipTests compile` | 通过 | 最终补跑报表服务聚合编译 |
| 2026-07-07 | flow 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-flow/forge-flow-server -am -DskipTests compile` | 通过 | 最终补跑流程服务聚合编译 |
| 2026-07-07 | 最终本地 MySQL | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping` | 失败 | 本地 3407 无 MySQL 服务，最终未执行 SQL 幂等实跑和接口 smoke |

## 代码质量备忘

- 新增 Flyway 脚本必须使用 `information_schema` 防重复。
- 禁止修改已经执行过的历史迁移脚本。
- Mapper XML 查询条件必须考虑表别名。
- 不要把授权关系表默认改为逻辑删除。
