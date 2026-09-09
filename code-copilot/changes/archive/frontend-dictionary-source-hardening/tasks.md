# 任务拆分 — 前端字典来源与设计器枚举整改

## 前置条件

- [x] 已读取根规范、项目记忆、编码规范和自动化测试标准。
- [x] 已核对现有字典和目标页面持久化值。
- [x] 已取得用户对剩余阶段持续 `/apply` 的授权。
- [x] 不修改已执行的历史 Flyway，不覆盖 `system/client.vue`、`system/config-center.vue` 的既有安全整改。
- [ ] 真实 MySQL/Flyway 和浏览器 E2E 由部署环境执行。

## 执行状态

- [x] Task 1：新增运行时字典迁移和前端转换基础设施。
- [x] Task 2：整改 System 与 Message 字典来源。
- [x] Task 3：整改 External 与 Generator 字典来源。
- [x] Task 4：整改 Data 与 Flow 字典来源。
- [x] Task 5：整改 App Center 字典来源。
- [x] Task 6：收敛 UI 强类型常量并执行聚合验证和两阶段审查。

## Task 1：迁移与转换基础设施

- 新增 `forge-server/db/migration/V1.0.54__add_frontend_runtime_dicts.sql`。
- 新增 `forge-admin-ui/src/utils/dict-options.js`，提供数字、布尔和值映射纯函数。
- 新增 `forge-admin-ui/src/utils/__tests__/dict-options.spec.js`。
- 验收：脚本中的类型和值均 `tenant_id=1`、显式列、`NOT EXISTS`；Vitest Red/Green 通过。

## Task 2：System 与 Message

- 修改 `system/menu.vue`：复用资源、显示、是/否、请求方法和打开方式字典。
- 修改 `system/client.vue`：协作保留客户端凭据安全改动，补齐认证/验证码/状态字典。
- 修改 `system/config-center.vue`：协作保留密钥字段移除，补齐算法和同账号策略字典。
- 修改 `message/biz-type.vue`、`message/message-list.vue`：字典搜索和 `DictTag` 回显。
- 验收：相关 Schema 为 computed，字段提交类型和默认值不变。

## Task 3：External 与 Generator

- 修改 `external/manage.vue`：认证、位置、状态、方法、内容类型、调用状态/类型全部来自字典。
- 修改 `generator/datasource.vue`：数据库类型、用途、风险和布尔/状态字典化。
- 修改 `generator/template.vue`、`generator/table.vue`：模板类型/引擎、生成方式和状态字典化。
- 验收：`data_db_type` 映射后仍提交生成器兼容值；`PATCH` 可选且不暴露 `ALL`。

## Task 4：Data 与 Flow

- 修改 `data/dataset.vue`：7 组运行时元数据选项从新增字典读取。
- 修改 `flow/design.vue`、`flow/model.vue`：设计器类型、表单类型和自动审批模式字典化。
- 验收：`business` 表单值可加载；业务绑定场景排序和默认值保持。

## Task 5：App Center

- 修改 `app-center/trigger.vue`：接收规则、触发类型、事件、动作和执行状态字典化。
- 修改 `app-center/integration.vue`：平台筛选和统计标题使用统一平台字典。
- 修改 `AppEditorDrawer.vue`、`AppEntryWizard.vue`：挂载、入口、移动场景、可见范围、平台和运行打开方式字典化。
- 验收：保存到业务表/JSON 的值、大小写和默认场景不变。

## Task 6：常量、验证与审查

- 新增 `forge-admin-ui/src/constants/dict-options.js`。
- 修改 `system/dictData.vue`、`ai/components/DictConfigPanel.vue`、`DocumentStatusMappingTable.vue` 复用标签类型常量。
- 执行 Vitest、目标 ESLint、生产构建、SQL/字面量 options/尾随空白/EOF 检查。
- 回填 `spec.md`、`test-spec.md`、`execution-log.md` 和总整改清单。
- 先执行 Spec Compliance Review，通过后再执行 Code Quality Review；修复所有 Critical/Important。
- 数据集数字发布状态、未知值兼容回显、分类状态字典标签和详情 hydration 已纳入第五轮回归合同。
- 数据集列表类型标签已改为消费 `data_dataset_type` 字典元数据，并纳入第六轮回归合同。
