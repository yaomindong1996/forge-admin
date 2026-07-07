# 数据表逻辑删除分批改造
> status: applied-with-warning
> created: 2026-07-07
> complexity: 🔴复杂

## 1. 背景与目标

当前项目中部分用户可见的主数据、配置数据和设计态元数据仍通过 MyBatis-Plus `deleteById/deleteBatchIds/removeById/remove` 或 Mapper XML `DELETE FROM` 做物理删除。目标是在不影响框架运行表、日志清理和授权关系重建语义的前提下，分批将可恢复、可审计的数据改为逻辑删除。

可验证结果：

- 已有删除字段但未生效的表，删除调用改为 `UPDATE ... SET del_flag/deleted = 1`。
- 需要软删的项目内部主表新增 `del_flag` 或补齐实体 `@TableLogic`。
- 自定义 Mapper XML 查询显式过滤逻辑删除记录。
- 定时任务框架自带表和 Flowable 引擎自带表不进入改造范围。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- MyBatis-Plus 全局配置未配置全局逻辑删除字段，仅设置 `idType`，见 `forge-server/forge-admin-server/src/main/resources/application.yml` 的 `mybatis-plus.global-config.dbConfig`。
- 低代码动态 CRUD 删除已按运行时表是否存在 `del_flag` 自动选择软删或物理删，见 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` `deleteById(String configKey, Object id)` 和 `DynamicCrudRepository.deleteById(...)`。
- Flyway 当前 `V1.0.0__baseline.sql` 是历史基线占位，新增结构变更必须新增版本脚本，见 `forge-server/db/migration/V1.0.0__baseline.sql`。

### 2.2 现有实现

- `ai_agent` 表已有 `del_flag`，但 `AiAgent` 实体无 `@TableLogic`，删除入口在 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/controller/AiAgentController.java` `delete(Long id)`。
- `ai_provider` 表已有 `del_flag`，但 `AiProvider` 实体无 `@TableLogic`，删除入口在 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderService.java` `delete(Long id)`。
- `ai_report_project` 表已有 `del_flag`，但 `ReportProject` 实体无 `@TableLogic`，删除入口在 `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/controller/ReportProjectController.java` `delete(Long id)`；`ReportProjectMapper.xml` 自定义查询未过滤 `del_flag`。
- `ai_report_template` 表已有 `del_flag`，`ReportTemplateMapper.xml` 查询已经过滤 `t.del_flag = '0'`，但实体无 `@TableLogic`，删除入口在 `forge-server/forge-report-server/src/main/java/com/mdframe/forge/report/project/template/service/ReportTemplateService.java` `deleteTemplate(Long id, Long userId)`。
- `sys_employee` 表已有 `del_flag`，`Employee` 实体只有普通 `delFlag` 字段，删除入口在 `forge-server/forge-admin-server/src/main/java/com/mdframe/forge/employee/service/impl/EmployeeServiceImpl.java` `deleteById(Long id)` 和 `deleteBatch(Long[] ids)`。
- `sys_flow_node_config` 表已有 `del_flag`，但 `FlowNodeConfig` 实体无逻辑删除字段；删除入口在 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowNodeConfigServiceImpl.java` `deleteConfig(String id)` 和 `deleteByModelId(String modelId)`。
- `sys_flow_approval_level` 实体已有 `@TableLogic private Integer deleted`，但全量初始化 SQL 中该表缺 `deleted` 字段，入口在 `FlowNodeConfigServiceImpl` 删除节点配置时清理层级配置。
- `sys_file_metadata` 使用 `status=0` 作为自定义软删除，见 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemFileMetadataPersistence.java` `delete(String fileId)`，不强制迁移为 `del_flag`。

### 2.3 发现与风险

- 若只给实体加 `@TableLogic`，但表缺字段，会导致删除 SQL 访问不存在列而失败。
- 若只给表加字段，不改 XML 查询，自定义列表仍会查出已删除数据。
- 授权关系表如果改为软删，可能产生脏授权或唯一索引冲突，必须单独评审。
- 日志、会话、清理任务和框架运行表保留物理删除更合理。
- 远程当前库 `information_schema` 查询在 `/apply` 阶段已尝试，沙箱内连接失败，提权审批服务返回 503，无法完成远程实库盘点；本批使用仓库全量初始化 SQL + 幂等 Flyway 脚本兜底。

## 3. 功能点

- [ ] 功能 1：对已有删除字段但仍物理删除的表补齐实体 `@TableLogic` 和 XML 查询过滤。
- [x] 功能 2：对项目内部主数据/配置表分批新增 `del_flag` 并接入逻辑删除。（Batch 2.1/2.2 已完成，Batch 2.3a 组织/岗位已完成，Batch 2.3b-1 租户已完成，Batch 2.3b-2 角色已完成，Batch 2.3b-3 菜单资源已完成，Batch 2.3b-4 用户已完成）
- [x] 功能 3：对低代码、数据资产、报表、应用中心设计态元数据分批新增 `del_flag` 并接入逻辑删除。（Batch 3.1 低代码/应用中心主设计态表已完成，Batch 3.2 数据资产/报表目录已完成，Batch 3.3 采购样例业务表已完成）
- [x] 功能 4：对 Mapper XML `DELETE FROM` 逐项分类，保留框架/日志/关系重建类物理删除，转换用户可见数据的物理删除。
- [x] 功能 5：记录所有明确排除的表和原因。

## 4. 业务规则

- 定时任务框架自带表排除：`qrtz_*`、SnailJob/Quartz 内部表。
- Flowable 引擎自带表排除：`act_*`、`flw_*`、Flowable/Liquibase changelog 表。
- `sys_job_config`、`sys_job_log` 属于 Forge 内部表，不按定时任务框架自带表排除，纳入盘点和改造评审。
- Forge 自研流程配置表不属于 Flowable 引擎表，例如 `sys_flow_model`、`sys_flow_template`、`sys_flow_node_config`、`sys_flow_form`、`sys_flow_entry`，按平台配置数据处理。
- 关系表默认保持物理删除，除非明确需要恢复子配置。
- 低代码运行表删除逻辑已存在，优先确保运行表包含 `del_flag`。
- 新增内置字段迁移必须通过 `forge-server/db/migration/` Flyway 脚本，脚本必须防重复。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增字段 | `sys_flow_approval_level` | `deleted tinyint NOT NULL DEFAULT 0` | 修复实体已有 `@TableLogic` 但表缺字段 |
| 可能新增字段 | `sys_flow_node_config` | `del_flag tinyint NOT NULL DEFAULT 0` | 若当前库缺字段则补齐 |
| 新增生成列/索引 | `ai_agent`, `sys_employee`, `sys_flow_node_config` | `logic_delete_active` + active 唯一索引 | 避免逻辑删除后原唯一键阻塞重建同编码/同节点的有效数据 |
| 新增字段 | Batch 2.1 平台内部表 | `del_flag` | `ai_context_config`、`ai_model`、`ai_page_template`、`ai_crud_config`、`sys_message_template`、`sys_message_biz_type`、`sys_job_config`、`sys_job_log` |
| 新增生成列/索引 | Batch 2.1 有业务唯一键表 | `logic_delete_active` + active 唯一索引 | 避免逻辑删除后原唯一键阻塞重建同编码/同配置的有效数据 |
| 保留物理清理 | `sys_job_log` | `cleanLog(days)` 专用 XML `DELETE` | 行级删除改为逻辑删除；日志留存清理仍按策略物理释放历史数据 |
| 新增字段 | Batch 2.2 系统配置表 | `del_flag` | `sys_config`、`sys_dict_type`、`sys_dict_data`、`sys_notice`、`sys_api_config`、`sys_data_scope_config`、`sys_file_storage_config` |
| 新增生成列/索引 | Batch 2.2 有业务唯一键表 | `logic_delete_active` + active 唯一索引 | 配置键、字典类型/字典值、API路径方法、数据权限 Mapper 方法只约束未删除记录唯一 |
| 新增字段 | Batch 2.3a 组织/岗位表 | `del_flag` | `sys_org`、`sys_post` |
| 新增生成列/索引 | Batch 2.3a 组织/岗位唯一键 | `logic_delete_active` + active 唯一索引 | 组织名称、岗位编码、组织内岗位名称只约束未删除记录唯一 |
| 新增字段 | Batch 2.3b-1 租户表 | `del_flag` | `sys_tenant` |
| 新增生成列/索引 | Batch 2.3b-1 租户唯一键 | `logic_delete_active` + active 唯一索引 | 租户名称只约束未删除记录唯一 |
| 新增字段 | Batch 2.3b-2 角色表 | `del_flag` | `sys_role` |
| 新增生成列/索引 | Batch 2.3b-2 角色唯一键 | `logic_delete_active` + active 唯一索引 | 租户内角色名称、角色标识只约束未删除记录唯一 |
| 新增字段 | Batch 2.3b-3 菜单/权限资源表 | `del_flag` | `sys_resource` |
| 新增生成列/索引 | Batch 2.3b-3 资源唯一键 | `logic_delete_active` + active 唯一索引 | 租户内资源权限标识只约束未删除记录唯一 |
| 新增字段 | Batch 2.3b-4 用户表 | `del_flag` | `sys_user` |
| 新增生成列/索引 | Batch 2.3b-4 用户唯一键 | `logic_delete_active` + active 唯一索引 | 默认租户内用户名只约束未删除记录唯一 |
| 新增字段 | Batch 3.1 低代码/应用中心主设计态表 | `del_flag` | `ai_business_suite`、`ai_business_app`、`ai_business_object`、`ai_business_trigger`、`ai_code_rule`、`ai_lowcode_domain`、`ai_lowcode_model` |
| 新增生成列/索引 | Batch 3.1 有业务唯一键表 | `logic_delete_active` + active 唯一索引 | 套件、应用、业务对象、编码规则、低代码领域、低代码模型只约束未删除记录唯一 |
| 新增字段 | Batch 3.2 数据资产/报表目录元数据表 | `del_flag` | `ai_report_data_connection`、`ai_report_data_dataset`、`ai_report_data_dataset_category`、`ai_report_data_business_definition`、`ai_report_data_dimension`、`ai_report_directory` |
| 新增生成列/索引 | Batch 3.2 数据资产有业务唯一键表 | `logic_delete_active` + active 唯一索引 | 数据连接、数据集、数据集分类、业务定义、维度编码只约束未删除记录唯一 |
| 新增字段 | Batch 3.3 采购样例业务表 | `del_flag` | `sample_purchase_order` |
| 新增生成列/索引 | Batch 3.3 采购样例业务唯一键 | `logic_delete_active` + active 唯一索引 | 采购单号、流程业务 Key 只约束未删除记录唯一 |
| 新增字段 | Task 4 用户配置主表 | `del_flag` | `ai_prompt_template`、`ai_custom_query_scheme` |
| 新增生成列/索引 | Task 4 提示词模板唯一键 | `logic_delete_active` + active 唯一索引 | 提示词模板编码只约束未删除记录唯一 |
| 不变更 | `qrtz_*`, `act_*`, `flw_*` | 无 | 框架自带表排除 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 不变更 | 现有删除接口 | POST/DELETE | 接口协议不变，底层由物理删除变为逻辑删除 |
| 不变更 | 现有列表/详情接口 | GET/POST | 返回结果过滤已逻辑删除记录 |

## 7. 影响范围

- 后端实体：`forge-plugin-system`、`forge-plugin-ai`、`forge-plugin-generator`、`forge-plugin-data`、`forge-plugin-flow`、`forge-report-server`、`forge-business-core`。
- Mapper XML：系统、AI、数据资产、低代码、报表、流程配置相关 XML。
- SQL 迁移：`forge-server/db/migration/` 新增 Flyway 脚本。
- 前端：接口协议不变，正常不需要改。

## 8. 风险与关注点

- 权限表属于高风险范围：`sys_role_resource`、`sys_user_role`、`sys_user_org_role` 等默认不改为软删。
- 数据恢复需求若覆盖子配置表，需要同步处理唯一索引和恢复冲突。
- `@TableLogic` 字段类型必须匹配数据库字段类型，`char(1)` 用 `String`，`tinyint` 用 `Integer`。
- Mapper XML 必须显式过滤逻辑删除，不能依赖 MP 自动处理。
- Flyway 版本号必须确认当前库 `forge_schema_history` 后再落最终编号。
- 逻辑删除会改变唯一键语义；Batch 1 已对 `ai_agent.agent_code`、`sys_employee.emp_no`、`sys_flow_node_config(model_id,node_id)` 增加“仅未删除记录唯一”的兼容处理。

## 8.5 测试策略

- **测试范围**：编译验证、迁移 SQL 幂等验证、重点删除接口/API smoke、自定义 XML 列表过滤验证。
- **覆盖率目标**：覆盖 Batch 1 全部表；Batch 2/3 每类至少一个代表表做删除后列表不可见验证。
- **独立 Test Spec**：是。

## 9. 待澄清

- [x] 定时任务框架自带表是否排除：只排除 `qrtz_*`、SnailJob/Quartz 内部表；`sys_job_config/sys_job_log` 纳入。
- [x] Flowable 自带表是否排除：排除 `act_*`、`flw_*`；Forge 自研 `sys_flow_*` 配置表纳入。
- [x] 是否允许先执行 Batch 1 小范围修复，再继续扩大到 Batch 2/3：用户执行 `/apply logic-delete-batch-refactor` 后，本轮只落 Batch 1。
- [x] 当前库 `information_schema` 盘点是否允许执行只读查询：已尝试执行，只读连接被沙箱/审批服务阻断；本轮不绕过审批，使用静态盘点和幂等迁移兜底。

## 10. 技术决策

- 使用 MyBatis-Plus `@TableLogic` 作为实体级逻辑删除机制，不引入自定义删除拦截器。
- `del_flag` 作为新表/新增字段默认逻辑删除字段；历史流程表若已使用 `deleted`，保持现状。
- 分批迁移，避免一次性修改所有删除链路导致权限、日志和关系表语义混乱。
- 关系表改造不默认执行，先做分类审查。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 0 | completed-with-warning | `execution-log.md` | 远程实库只读查询未完成，已记录阻断原因和静态盘点依据 |
| Task 1 | completed | Batch 1 Java/XML/Flyway 文件 | 已补 `@TableLogic`、XML 过滤、字段迁移、唯一键兼容 |
| Task 2 | completed | Batch 2.1/2.2/2.3a/2.3b-1/2.3b-2/2.3b-3/2.3b-4 Java/XML/Flyway 文件 | AI/消息/任务/低代码配置表、系统配置/字典/通知/API/数据权限/文件存储配置、组织/岗位、租户、角色、菜单资源、用户已完成 |
| Task 3 | completed | Batch 3.1/3.2/3.3 Java/XML/Flyway 文件 | 低代码/应用中心 7 张主设计态表、数据资产/报表目录 6 张主元数据表、采购样例业务表已完成 |
| Task 4 | completed | Task 4 Java/XML/Flyway 文件 | XML 物理删除已评审；`ai_prompt_template`、`ai_custom_query_scheme` 转逻辑删除，其余日志/会话/关系/重建型子配置保留物理删除 |
| Task 5 | completed-with-warning | `test-spec.md`、`execution-log.md` | admin/report/flow 编译和静态检查通过；本地 MySQL 3407 不可用，SQL 实跑和接口 smoke 跳过 |

## 12. 审查结论

待 `/review logic-delete-batch-refactor` 执行。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-07
- **确认人**：用户
- **确认范围**：执行 `/apply logic-delete-batch-refactor`；已按用户“继续”推进到 Batch 3.3 采购样例业务表，核心权限关系表、低代码关系/日志表、数据资产字段/ACL/行权限/维度项/绑定表仍按默认策略保持物理删除，Flowable/Quartz/SnailJob 框架表仍排除。
