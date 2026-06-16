# 任务清单：lowcode-app-data-analysis-closure
> status: proposed
> created: 2026-06-15
> 拆分顺序：边界冻结 → 数据模型 → 数据集同步 → 透视查询 → 透视前端 → 指标体系 → 看板闭环 → 告警联动 → 运营治理 → 验证回填
> 原则：每个任务可独立提交；首期只支持单数据集透视；指标首期支持公式；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；Flyway 脚本必须防重复；分析资产暂不纳入代码下载模式。

## 前置条件

- [x] 已确认透视分析器首期只支持单数据集，跨对象关联分析放到 P2/P3 或通过宽表数据集解决。
- [x] 已确认指标首期支持公式表达式，不限于单字段聚合。
- [x] 已确认分析视图公共可见范围支持角色、部门、用户三种授权。
- [x] 已确认 CRM 默认看板由应用发布后生成，不通过初始化脚本写死最终看板配置。
- [x] 已确认告警规则首期只支持手动执行和每日扫描，不开放复杂 cron 配置。
- [x] 已确认分析资产暂不纳入代码下载模式，不生成到业务专属代码包。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 边界冻结 | Task 0 | Spec/Task 硬边界一致，进入实现不再反复改范围 |
| Phase 1 / P0 | 数据模型与基础权限 | Task 1-2 | 分析视图、指标、看板、告警、日志、字段扩展和权限资源 |
| Phase 2 / P0 | 数据集同步与透视后端 | Task 3-5 | 发布后同步数据集、单数据集透视查询、视图保存和钻取协议 |
| Phase 3 / P0 | 数据透视前端最小闭环 | Task 6-8 | 分析工作台、字段面板、透视结果、保存视图、CRM 默认分析视图 |
| Phase 4 / P1 | 指标与看板闭环 | Task 9-12 | 公式指标、指标目录、业务看板、发布后默认 CRM 看板 |
| Phase 5 / P2 | 分析到行动 | Task 13-15 | 告警规则、每日扫描、站内消息/触发器动作、告警日志 |
| Phase 6 / P3 | 治理与运营 | Task 16-18 | 应用健康度、查询日志、统一血缘影响分析、发布前检查 |
| Phase 7 | 验证与文档回填 | Task 19-20 | test-spec、execution-log、编译/前端校验结果 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | Spec 硬边界和任务拆分确认 | pending | P0 |
| Task 1 | Phase 1 | 分析闭环 Flyway 数据模型 | pending | P0 |
| Task 2 | Phase 1 | 后端实体、Mapper、DTO/VO 和权限字典 | pending | P0 |
| Task 3 | Phase 2 | 低代码发布后自动同步分析数据集 | pending | P0 |
| Task 4 | Phase 2 | 单数据集透视查询引擎 | pending | P0 |
| Task 5 | Phase 2 | 分析视图保存、授权和钻取协议 | pending | P0 |
| Task 6 | Phase 3 | 前端分析 API、路由和分析工作台骨架 | pending | P0 |
| Task 7 | Phase 3 | 透视设计器和图表结果面板 | pending | P0 |
| Task 8 | Phase 3 | CRM 默认分析视图和明细钻取 | pending | P0 |
| Task 9 | Phase 4 | 指标目录和公式指标后端能力 | pending | P1 |
| Task 10 | Phase 4 | 指标目录前端和指标预览 | pending | P1 |
| Task 11 | Phase 4 | 业务看板后端与发布后默认看板生成 | pending | P1 |
| Task 12 | Phase 4 | 业务看板前端和应用中心入口集成 | pending | P1 |
| Task 13 | Phase 5 | 指标告警规则和执行日志后端 | pending | P2 |
| Task 14 | Phase 5 | 告警每日扫描任务和动作执行 | pending | P2 |
| Task 15 | Phase 5 | 告警前端配置、日志和 CRM 样板 | pending | P2 |
| Task 16 | Phase 6 | 应用运行监控和健康度 | pending | P3 |
| Task 17 | Phase 6 | 数据血缘与影响分析统一服务 | pending | P3 |
| Task 18 | Phase 6 | 发布前检查和就绪度扩展 | pending | P3 |
| Task 19 | Phase 7 | 自动化测试规格和增量验证 | pending | P0 |
| Task 20 | Phase 7 | 文档回填、审查和确认记录 | pending | P0 |

---

## Phase 0：边界冻结

### Task 0: Spec 硬边界和任务拆分确认

**目标**: 确认实现范围和阶段边界，避免 P0 实现时滑入完整 BI、跨对象分析或代码下载生成。

**涉及文件**:
- `code-copilot/changes/lowcode-app-data-analysis-closure/spec.md` — 保持已澄清决策和技术决策。
- `code-copilot/changes/lowcode-app-data-analysis-closure/tasks.md` — 维护任务状态和执行边界。
- `code-copilot/changes/lowcode-app-data-analysis-closure/test-spec.md` — Task 19 创建，执行验证前补齐。
- `code-copilot/changes/lowcode-app-data-analysis-closure/execution-log.md` — Task 19 创建，记录命令和结果。

**确认事项**:
- P0 只做单数据集透视、分析数据集同步、视图保存、CRM 默认分析视图。
- P1 才做指标目录、公式指标和业务看板。
- P2 才做告警和分析到行动。
- P3 才做运行监控、健康度、统一血缘、发布前分析影响检查。
- 分析资产不进入代码下载模式。

**验收标准**:
- `spec.md` 第 15 章没有未完成待澄清项。
- `tasks.md` 的 P0-P3 与 `spec.md` 第 14 章一致。
- 后续任务的验收标准不包含跨对象关联分析、复杂 cron、代码下载生成分析资产。

---

## Phase 1 / P0：数据模型与基础权限

### Task 1: 分析闭环 Flyway 数据模型

**目标**: 创建分析视图、指标、看板、告警、运行监控和查询日志所需持久化结构，并扩展数据集来源字段。

**涉及文件**:
- 新增 `forge-server/db/migration/V1.0.xx__add_lowcode_analysis_closure_tables.sql` — 分析闭环表结构、索引、字典和权限资源。

**数据结构**:
- 新增 `ai_analysis_view`：保存透视视图配置。
- 新增 `ai_analysis_metric`：保存指标目录。
- 新增 `ai_analysis_metric_version`：保存指标口径版本。
- 新增 `ai_analysis_dashboard`：保存业务看板。
- 新增 `ai_analysis_dashboard_widget`：保存看板组件。
- 新增 `ai_analysis_alert_rule`：保存告警规则。
- 新增 `ai_analysis_alert_log`：保存告警执行日志。
- 新增 `ai_app_usage_log`：保存应用访问和运行事件。
- 新增 `ai_analysis_query_log`：保存分析查询日志。
- 扩展 `data_dataset`：`source_type`, `source_id`, `object_code`, `config_key`, `auto_sync`, `last_sync_time`。
- 扩展 `data_dataset_field`：优先复用现有 `fieldRole`、`defaultAgg`、`sensitiveLevel`、`dictType`、`dataUnit`；缺失时补充 `source_field`, `analysis_role`, `default_time_grain`, `drill_enabled`。

**脚本约束**:
- 使用 `CREATE TABLE IF NOT EXISTS`。
- 新增列和索引前查询 `information_schema`。
- `sys_resource`、`sys_dict_type`、`sys_dict_data` 初始化必须 `NOT EXISTS` 防重复。
- 所有内置数据 `tenant_id=1`。
- 不写真实外部 URL、Token、Secret。

**权限资源建议**:
- `ai:analysis:view`
- `ai:analysis:design`
- `ai:analysis:metric`
- `ai:analysis:dashboard`
- `ai:analysis:alert`
- `ai:analysis:lineage`
- `ai:analysis:health`

**验收标准**:
- 空库和已有库均可执行脚本。
- 所有分析资产表包含 `tenant_id` 和标准审计字段。
- 分析视图、指标、看板、告警编码具备租户维度唯一约束。
- 数据集扩展字段可追踪低代码对象和 `configKey` 来源。

### Task 2: 后端实体、Mapper、DTO/VO 和权限字典

**目标**: 为分析闭环建立基础 Java 模型、XML 查询和前后端协议对象。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisView.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisMetric.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisMetricVersion.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisDashboard.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisDashboardWidget.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisAlertRule.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisAlertLog.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAppUsageLog.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiAnalysisQueryLog.java`
- 新增对应 Mapper：`AnalysisViewMapper.java`, `AnalysisMetricMapper.java`, `AnalysisDashboardMapper.java`, `AnalysisAlertRuleMapper.java`, `AnalysisLineageMapper.java`, `AnalysisHealthMapper.java`
- 新增对应 XML：`AnalysisViewMapper.xml`, `AnalysisMetricMapper.xml`, `AnalysisDashboardMapper.xml`, `AnalysisAlertRuleMapper.xml`, `AnalysisLineageMapper.xml`, `AnalysisHealthMapper.xml`
- 新增 DTO/VO 包：`dto/analysis/*`, `vo/analysis/*`

**关键协议对象**:
- `AnalysisDatasetSyncDTO`：对象或 `configKey` 同步请求。
- `PivotQueryDTO`：数据集、维度、指标、筛选、排序、分页、图表类型。
- `PivotQueryResultVO`：列、行、图表数据、钻取条件。
- `AnalysisViewSaveDTO` / `AnalysisViewVO`：透视视图保存和详情。
- `AnalysisMetricSaveDTO` / `AnalysisMetricVO`：指标保存和详情。
- `AnalysisDashboardSaveDTO` / `AnalysisDashboardVO`：看板保存和详情。
- `AnalysisAlertRuleSaveDTO` / `AnalysisAlertLogVO`：告警规则和日志。
- `AnalysisImpactVO`：统一影响分析结果。
- `AnalysisHealthVO`：应用健康度。

**验收标准**:
- 查询类方法全部在 Mapper XML 中实现。
- DTO 中所有分页字段使用 `pageNum/pageSize`。
- 运行态 VO 不暴露表名、SQL、原始 JSON 给普通业务页面。
- 权限字典和前端按钮权限编码一致。

---

## Phase 2 / P0：数据集同步与透视后端

### Task 3: 低代码发布后自动同步分析数据集

**目标**: 低代码应用发布成功后自动生成或更新分析数据集，实现业务对象到分析数据资产的闭环入口。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/analysis/AnalysisDatasetSyncService.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodePublishService.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataDatasetService.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataDatasetFieldService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AnalysisDatasetController.java`

**关键签名**:
```java
public AnalysisDatasetSyncResultVO syncByConfigKey(String configKey);

public AnalysisDatasetSyncResultVO syncByObjectCode(String objectCode);

public List<DataDatasetField> buildFieldsFromLowcodeModel(AiCrudConfig config,
                                                          LowcodeModelSchema schema,
                                                          boolean preserveManualConfig);
```

**字段识别规则**:
- `dictType` 来自低代码字段配置。
- 金额字段：字段名或列名包含 `amount`、`price`、`fee`、`cost`、`payment`、`total`，默认 `fieldRole=MEASURE`、`defaultAgg=SUM`、`dataUnit=cent`。
- 日期字段：字段类型为时间或命名包含 `date`、`time`，默认 `fieldRole=DATE`。
- 状态、阶段、负责人、地区、客户类型等字段默认 `fieldRole=DIMENSION`。
- `id`、`tenant_id`、`create_by`、`update_by` 默认隐藏但保留钻取条件能力。
- 已人工修改 `fieldRole/defaultAgg/sensitiveLevel/dictType/dataUnit` 的字段不被覆盖。

**接口**:
- `POST /ai/analysis/dataset/sync/object/{objectCode}`
- `POST /ai/analysis/dataset/sync/config/{configKey}`

**验收标准**:
- 发布 `crm_customer`、`crm_opportunity`、`crm_contract`、`crm_payment` 后生成对应 `data_dataset` 和字段。
- 重复发布不会重复创建数据集。
- 人工修改字段角色后再次同步不会被静默覆盖。
- 数据集发布状态与低代码应用发布状态保持一致。

### Task 4: 单数据集透视查询引擎

**目标**: 在 `forge-plugin-data` 运行查询基础上新增受控聚合查询，支持单数据集透视表和图表数据。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataPivotQueryService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/PivotQueryBuilder.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/PivotQueryLimitProperties.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataDatasetRuntimeController.java`
- 新增或复用 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AnalysisPivotController.java`

**关键签名**:
```java
public PivotQueryResultVO query(PivotQueryDTO dto);

public QueryBuildResult buildPivotSql(DataDataset dataset,
                                      DataConnection connection,
                                      List<DataDatasetField> fields,
                                      PivotQueryDTO dto);
```

**查询限制**:
- 最大行维度数：3。
- 最大列维度数：2。
- 最大指标数：5。
- 最大返回行数：1000。
- 默认超时使用数据集 `timeoutSeconds`，最大不超过系统配置。
- 首期只支持单数据集，不接受多个 `datasetId`。

**安全规则**:
- 维度、指标、筛选字段必须存在于数据集字段元数据。
- TABLE 数据集用字段白名单构建聚合 SQL。
- SQL 数据集首期默认只允许明细查询；若开启透视，必须包装为子查询并校验字段白名单。
- 必须复用数据集访问权限、行级权限、字典翻译和敏感字段隐藏/脱敏。
- 查询类 SQL Builder 不允许拼接前端字段原文，必须使用方言 quote 和白名单列名。

**验收标准**:
- 可对单个数据集按维度分组求和、计数、平均。
- 非法字段、未发布数据集、无权限数据集返回明确业务错误。
- 高基数查询超过限制时被拦截。
- 返回结果包含表格列、图表序列和钻取过滤条件。

### Task 5: 分析视图保存、授权和钻取协议

**目标**: 保存用户透视配置，并支持个人/对象/套件公共视图及角色、部门、用户授权。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AnalysisViewController.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/analysis/AnalysisViewService.java`
- 修改 `AnalysisViewMapper.java` / `AnalysisViewMapper.xml`
- 新增 `AnalysisViewAclDTO.java` / `AnalysisViewAclVO.java`
- 修改 `forge-admin-ui/src/api/business-app.js` 或新增 `forge-admin-ui/src/api/analysis.js`

**可见范围**:
- `PRIVATE`：仅创建人。
- `OBJECT_PUBLIC`：对象公共视图，可追加角色、部门、用户授权。
- `SUITE_PUBLIC`：套件公共视图，可追加角色、部门、用户授权。

**钻取协议**:
```json
{
  "targetType": "CRUD_PAGE",
  "configKey": "crm_contract",
  "objectCode": "crm_contract",
  "filters": [
    { "field": "contractStatus", "operator": "eq", "value": "SIGNED" }
  ]
}
```

**接口**:
- `GET /ai/analysis/view/page`
- `GET /ai/analysis/view/{id}`
- `POST /ai/analysis/view`
- `PUT /ai/analysis/view`
- `DELETE /ai/analysis/view/{id}`

**验收标准**:
- 个人视图只有本人可见。
- 公共视图支持角色、部门、用户授权。
- 视图保存前校验数据集、字段、指标、钻取目标均有效。
- 删除数据集或字段前影响分析能发现引用该字段的视图。

---

## Phase 3 / P0：数据透视前端最小闭环

### Task 6: 前端分析 API、路由和分析工作台骨架

**目标**: 在应用中心新增数据分析入口，提供业务对象维度的分析工作台骨架。

**涉及文件**:
- 新增 `forge-admin-ui/src/api/analysis.js` 或 `forge-admin-ui/src/api/analysis.ts`
- 修改 `forge-admin-ui/src/router/index.js` — 注册 `/app-center/analysis` 和可选 `/app-center/object/:objectCode/analysis`。
- 新增 `forge-admin-ui/src/views/app-center/analysis.vue`
- 修改 `forge-admin-ui/src/views/app-center/object.[objectCode].vue`
- 修改 `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue`
- 修改 `forge-admin-ui/src/views/app-center/components/ObjectCard.vue`

**前端 API**:
- `syncAnalysisDatasetByObject(objectCode)`
- `syncAnalysisDatasetByConfig(configKey)`
- `queryPivot(data)`
- `analysisViewPage(params)`
- `analysisViewDetail(id)`
- `saveAnalysisView(data)`
- `deleteAnalysisView(id)`

**页面行为**:
- 从业务对象进入时自动带入 `suiteCode`、`objectCode`、`configKey`。
- 若对象未同步数据集，显示“同步分析数据集”动作。
- 若数据集已同步，默认加载字段元数据和最近视图。
- 普通业务用户不显示表名、SQL、`configKey`。

**验收标准**:
- 应用中心可进入数据分析工作台。
- 对象详情可看到数据分析入口。
- 未同步数据集时可手动触发同步并刷新页面。

### Task 7: 透视设计器和图表结果面板

**目标**: 实现单数据集透视配置、预览和图表展示。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/PivotDesigner.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/FieldRolePanel.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/PivotResultPanel.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/PivotFilterBuilder.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/ChartTypeSelect.vue`
- 修改 `forge-admin-ui/src/views/app-center/analysis.vue`

**交互能力**:
- 字段按维度、指标、日期、明细分组展示。
- 支持选择行维度、列维度、指标、筛选条件、排序、TopN。
- 支持图表类型：指标卡、表格、柱状图、折线图、饼图、面积图、横向条形图。
- 支持聚合方式：计数、去重计数、求和、平均、最大、最小。
- 支持日期粒度：日、周、月、季、年。
- 展示空状态、加载态、错误态、权限不足态。

**验收标准**:
- 选择单个数据集后可以配置并执行透视查询。
- 非法组合在前端配置阶段给出提示，不等到后端报错。
- 图表和表格可以切换。
- 页面在 1366px 桌面和移动端宽度下不重叠。

### Task 8: CRM 默认分析视图和明细钻取

**目标**: 为 CRM 首批对象生成默认分析视图，并实现图表到动态 CRUD 明细页的筛选钻取。

**涉及文件**:
- 修改 `AnalysisDatasetSyncService.java` — 同步 CRM 对象时生成默认分析视图。
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/CrmAnalysisTemplateService.java`
- 修改 `forge-admin-ui/src/views/app-center/components/analysis/PivotResultPanel.vue`
- 修改 `forge-admin-ui/src/views/ai/crud-page.vue` — 支持从分析钻取参数初始化查询条件；若已有能力则复用。
- 修改 `forge-admin-ui/src/api/business-app.js` 或 `forge-admin-ui/src/api/analysis.js`

**默认视图**:
- 客户：客户类型分布、本月新增客户趋势。
- 商机：阶段分布、预计金额按负责人汇总。
- 合同：合同状态分布、签约月份合同金额趋势。
- 回款：回款状态分布、逾期金额按负责人汇总。

**钻取规则**:
- 图表点击生成 `field/operator/value` 筛选条件。
- 跳转 `/ai/crud-page/:configKey`。
- 动态 CRUD 页面按钻取条件初始化搜索区并自动查询。

**验收标准**:
- CRM 四个对象发布后生成默认分析视图。
- 点击默认视图图表可进入对应 CRUD 明细。
- 钻取条件和图表单元格语义一致。

---

## Phase 4 / P1：指标与看板闭环

### Task 9: 指标目录和公式指标后端能力

**目标**: 建立可复用指标目录，支持普通指标和公式指标。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/controller/AnalysisMetricController.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisMetricService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/MetricFormulaValidator.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/MetricCalculationService.java`
- 修改 `AnalysisMetricMapper.java` / `AnalysisMetricMapper.xml`
- 可复用或参考 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/*`

**关键签名**:
```java
public AnalysisMetricVO saveMetric(AnalysisMetricSaveDTO dto);

public MetricPreviewVO previewMetric(Long metricId, MetricPreviewDTO dto);

public FormulaValidationResult validateMetricFormula(AnalysisMetricSaveDTO dto);
```

**公式规则**:
- 公式可引用同一数据集字段或已有指标。
- 支持四则运算、括号和基础函数。
- 发布前做语法校验、字段存在性校验、依赖分析、循环依赖检测。
- 指标变更写入 `ai_analysis_metric_version`。

**验收标准**:
- 可创建普通 SUM/COUNT 指标。
- 可创建回款完成率这类公式指标。
- 循环依赖被拦截。
- 指标预览返回当前值和口径说明。

### Task 10: 指标目录前端和指标预览

**目标**: 提供业务人员和实施人员可维护的指标目录页面。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/MetricCatalogPanel.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/MetricEditorDrawer.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/MetricFormulaEditor.vue`
- 修改 `forge-admin-ui/src/views/app-center/analysis.vue`
- 修改 `forge-admin-ui/src/api/analysis.js`

**交互能力**:
- 按套件、对象、数据集筛选指标。
- 新增普通指标和公式指标。
- 选择字段、聚合方式、时间字段、单位、负责人。
- 编写公式并即时校验。
- 预览指标结果。
- 查看口径说明和版本记录。

**验收标准**:
- 实施人员可创建合同金额、回款完成率等指标。
- 公式错误、字段不存在、循环依赖有明确提示。
- 指标预览结果可用于看板组件选择。

### Task 11: 业务看板后端与发布后默认看板生成

**目标**: 建立业务看板和组件后端能力，并在 CRM 对象发布后生成默认看板。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/controller/AnalysisDashboardController.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisDashboardService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisDashboardTemplateService.java`
- 修改 `AnalysisDashboardMapper.java` / `AnalysisDashboardMapper.xml`
- 修改 `AnalysisDatasetSyncService.java`

**接口**:
- `GET /ai/analysis/dashboard/page`
- `GET /ai/analysis/dashboard/{id}`
- `POST /ai/analysis/dashboard`
- `PUT /ai/analysis/dashboard`
- `DELETE /ai/analysis/dashboard/{id}`

**默认看板生成规则**:
- 发布后按 `suiteCode/objectCode/configKey` 生成或更新默认看板。
- 初始化脚本只维护模板类基础数据，最终看板配置由发布后服务生成。
- 不覆盖用户手动调整过的看板布局，除非用户选择重新生成。

**验收标准**:
- CRM 总览、商机漏斗、合同金额、回款逾期看板可生成。
- 看板组件可绑定指标或透视视图。
- 看板下线前影响分析可识别告警和入口引用。

### Task 12: 业务看板前端和应用中心入口集成

**目标**: 在业务套件和业务对象页面展示看板，并支持看板组件配置。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/DashboardDesigner.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/DashboardWidgetRenderer.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/DashboardWidgetEditor.vue`
- 修改 `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue`
- 修改 `forge-admin-ui/src/views/app-center/object.[objectCode].vue`
- 修改 `forge-admin-ui/src/views/app-center/stats-dashboard.vue` — 可保留旧入口并逐步迁移到新看板。
- 修改 `forge-admin-ui/src/api/analysis.js`

**交互能力**:
- 套件详情展示默认看板。
- 对象详情展示对象级看板。
- 看板组件支持指标卡、图表、透视表。
- 组件可配置数据源、标题、图表类型、钻取目标。
- 组件失败时展示错误态和配置入口。

**验收标准**:
- CRM 套件详情能看到 CRM 总览看板。
- 客户、商机、合同、回款对象详情能看到对象级看板。
- 看板图表可钻取明细。

---

## Phase 5 / P2：分析到行动

### Task 13: 指标告警规则和执行日志后端

**目标**: 建立指标告警规则、执行日志和手动执行能力。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/controller/AnalysisAlertController.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisAlertRuleService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisAlertExecutor.java`
- 修改 `AnalysisAlertRuleMapper.java` / `AnalysisAlertRuleMapper.xml`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/businessapp/BusinessTriggerExecutor.java` — 如复用触发器动作。

**接口**:
- `GET /ai/analysis/alert/rule/page`
- `POST /ai/analysis/alert/rule`
- `PUT /ai/analysis/alert/rule`
- `POST /ai/analysis/alert/rule/{id}/execute`
- `GET /ai/analysis/alert/log/page`

**规则能力**:
- 条件：大于、小于、等于、环比增长、连续 N 天满足条件。
- 目标：指标或透视视图。
- 动作：站内消息、待办、触发器动作、Webhook TODO。
- 去重：同一规则同一周期默认只提醒一次。

**验收标准**:
- 可手动执行告警规则并写入日志。
- 规则失败时记录错误原因。
- 无权限指标不能被告警规则引用。

### Task 14: 告警每日扫描任务和动作执行

**目标**: 接入任务调度中心，按每日扫描方式执行启用的告警规则。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisAlertScanJob.java`
- 新增或修改 Flyway 脚本注册系统任务：`LOWCODE.analysisAlertScanJob`
- 修改 `AnalysisAlertExecutor.java`
- 修改 `AnalysisAlertRuleMapper.xml` — 查询到期启用规则。
- 修改消息发送相关服务调用，复用现有消息中心。

**调度规则**:
- 首期只支持手动执行和每日扫描。
- 不为每条规则创建独立 Job。
- 支持全局扫描上限和单批执行上限。
- 同一规则同一周期防重复。

**验收标准**:
- 任务中心能看到并启停告警扫描任务。
- 扫描任务只执行启用规则。
- 执行站内消息或触发器动作后写入告警日志。
- Webhook 动作返回 TODO 状态，不发真实外部请求。

### Task 15: 告警前端配置、日志和 CRM 样板

**目标**: 提供告警规则配置页面和 CRM 回款逾期、商机停滞、合同金额超限样板。

**涉及文件**:
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/AlertRulePanel.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/AlertRuleEditorDrawer.vue`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/AlertLogDrawer.vue`
- 修改 `forge-admin-ui/src/views/app-center/analysis.vue`
- 修改 `forge-admin-ui/src/api/analysis.js`
- 修改 CRM 默认分析模板服务，发布后生成推荐告警草稿或模板。

**样板规则**:
- 回款逾期：逾期金额大于 0，每日提醒负责人。
- 商机停滞：阶段连续 N 天未变化，生成跟进提醒。
- 合同金额超限：合同金额超过阈值，提示配置流程或触发流程动作。

**验收标准**:
- 用户能创建、启停、手动执行告警规则。
- 告警日志可查看成功、失败、跳过和 TODO。
- CRM 样板对象能生成推荐告警模板。

---

## Phase 6 / P3：治理与运营

### Task 16: 应用运行监控和健康度

**目标**: 记录应用访问、分析查询和异常事件，并输出应用健康度。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AppUsageLogService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisHealthService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/controller/AnalysisHealthController.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/businessapp/BusinessAppOpenService.java`
- 修改 `DataPivotQueryService.java` — 写入 `ai_analysis_query_log`。
- 修改 `AnalysisHealthMapper.java` / `AnalysisHealthMapper.xml`
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/AppHealthPanel.vue`

**健康度指标**:
- 访问量。
- 活跃用户。
- 最近访问时间。
- 打开失败次数。
- 分析查询失败次数。
- 慢查询数。
- 最近异常。
- 告警/触发器执行成功率。

**验收标准**:
- 打开业务应用时写入访问日志。
- 执行透视查询时写入查询日志。
- 应用中心能展示“正常、低使用、异常、待配置”。

### Task 17: 数据血缘与影响分析统一服务

**目标**: 统一查询数据集、字段、指标、透视视图、看板、告警和 AI 大屏组件影响范围。

**涉及文件**:
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/analysis/AnalysisLineageService.java`
- 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/controller/AnalysisLineageController.java`
- 修改 `AnalysisLineageMapper.java` / `AnalysisLineageMapper.xml`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/dashboard/service/AiDashboardGenerateRecordService.java` 或通过适配服务调用已有影响查询。
- 新增 `forge-admin-ui/src/views/app-center/components/analysis/LineageImpactPanel.vue`
- 修改 `forge-admin-ui/src/api/analysis.js`

**影响范围**:
- 数据集影响的指标、视图、看板、告警、AI 大屏组件。
- 字段影响的指标、视图、看板组件、告警条件。
- 指标影响的看板和告警。
- 视图影响的看板和告警。

**验收标准**:
- 数据集下线前可查询影响范围。
- 字段删除或改名前可查询影响范围。
- AI 大屏已有数据集影响结果纳入统一返回。

### Task 18: 发布前检查和就绪度扩展

**目标**: 把分析资产纳入业务对象就绪度和低代码发布前检查。

**涉及文件**:
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/businessapp/BusinessObjectReadinessService.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/lowcode/LowcodePublishService.java`
- 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/service/businessapp/BusinessObjectPublishService.java`
- 修改 `forge-admin-ui/src/views/app-center/components/ReadinessPanel.vue`
- 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue`
- 修改 `forge-admin-ui/src/views/app-center/object.[objectCode].vue`

**检查项**:
- 数据集已同步。
- 字段角色完整。
- 至少一个默认指标。
- 至少一个默认分析视图。
- 看板可打开。
- 告警可选配置。
- 血缘可追踪。
- 字段变更影响分析已确认。

**验收标准**:
- 对象详情显示分析就绪状态。
- 发布前能提示分析资产影响。
- 缺口项可跳转到对应配置页面。

---

## Phase 7：验证与文档回填

### Task 19: 自动化测试规格和增量验证

**目标**: 按项目自动化测试标准生成并执行增量验证。

**涉及文件**:
- 新增 `code-copilot/changes/lowcode-app-data-analysis-closure/test-spec.md`
- 新增 `code-copilot/changes/lowcode-app-data-analysis-closure/execution-log.md`
- 修改 `code-copilot/changes/lowcode-app-data-analysis-closure/tasks.md`

**验证前置**:
- 执行 `/test`、阶段收尾验证、Review 后修复验证或归档前验收时，必须先读取 `code-copilot/rules/automated-testing-standard.md`。
- 复用本变更已有 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`，按本轮差异增量验证。

**建议命令**:
```bash
cd forge-server && mvn -pl forge-admin-server -am compile -DskipTests
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center src/api/analysis.js
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**验收标准**:
- `test-spec.md` 明确 P0/P1/P2/P3 的测试范围和跳过项。
- `execution-log.md` 记录命令、结果、警告、失败项和服务清理情况。
- 阶段完成后对应任务状态和验证结果已回填。

### Task 20: 文档回填、审查和确认记录

**目标**: 保持 Spec、Task、测试日志和审查结论一致，形成可归档交付材料。

**涉及文件**:
- `code-copilot/changes/lowcode-app-data-analysis-closure/spec.md`
- `code-copilot/changes/lowcode-app-data-analysis-closure/tasks.md`
- `code-copilot/changes/lowcode-app-data-analysis-closure/test-spec.md`
- `code-copilot/changes/lowcode-app-data-analysis-closure/execution-log.md`

**回填要求**:
- 每完成一个任务，更新任务状态、实际改动文件和验证结果。
- 若实现中改变边界，必须先回写 `spec.md` 并确认。
- Review 后修复必须写入执行日志。
- 确认记录（HARD-GATE）补齐确认时间和确认人。

**验收标准**:
- `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` 对同一阶段状态描述一致。
- 无未处理待澄清项。
- 可直接进入 `/apply lowcode-app-data-analysis-closure`。
